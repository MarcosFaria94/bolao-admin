package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.domain.CalculoRateio
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.LeituraComprovante
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.domain.Volante
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun novoId(): String = UUID.randomUUID().toString()

val Aposta.palpite: Palpite get() = Palpite(numeros, trevos, extra, palpitesLoteca)

/**
 * Todas as regras de contabilidade do bolão, como funções puras sobre o [Estado].
 * Cada operação devolve um novo Estado; quem chama é que grava (Repositorio.atualizar).
 *
 * O registro só cresce: nenhum lançamento, aposta registrada, apuração ou comprovante
 * é alterado ou apagado depois de criado. Cada registro gera um [Comprovante] único.
 */
object Operacoes {

    // ---------- consultas ----------

    fun saldo(e: Estado): Long = e.lancamentos.sumOf { it.valor }

    fun apostaFeitaEm(e: Estado, data: LocalDate): Boolean = e.apostas.any { it.registrada && it.dataAposta == data }

    fun rascunhos(e: Estado): List<Aposta> = e.apostas.filter { !it.registrada }.sortedByDescending { it.criadaEm }

    fun apostasDoMes(e: Estado, mes: YearMonth): List<Aposta> =
        e.apostas.filter { it.registrada && YearMonth.from(it.dataAposta) == mes }

    /** Número que a próxima aposta registrada em [hoje] vai receber (Nº1 = primeira do mês). */
    fun proximoNumero(e: Estado, hoje: LocalDate): Int = apostasDoMes(e, YearMonth.from(hoje)).size + 1

    /** Número da aposta: o registrado, ou o que um rascunho receberia se fosse registrado em [hoje]. */
    fun numeroDe(e: Estado, a: Aposta, hoje: LocalDate): Int = if (a.registrada) a.numero else proximoNumero(e, hoje)

    /** Número do próximo aporte com data em [data] (Nº1 = primeiro aporte do mês; recomeça a cada mês). */
    fun proximoNumeroAporte(e: Estado, data: LocalDate): Int =
        e.lancamentos.count { it.tipo == TipoLancamento.APORTE && YearMonth.from(it.data) == YearMonth.from(data) } + 1

    /** Número do próximo rateio registrado em [hoje] (Nº1 = primeiro rateio do mês; recomeça a cada mês). */
    fun proximoNumeroRateio(e: Estado, hoje: LocalDate): Int =
        e.lancamentos.count { it.tipo == TipoLancamento.RATEIO && YearMonth.from(it.data) == YearMonth.from(hoje) } + 1

    /**
     * Nº do lançamento no mês, como nos comprovantes: o da aposta (aposta e prêmio), o do aporte e o
     * do rateio, na ordem em que foram registrados no mês. null nos outros lançamentos.
     */
    fun numeroNoMes(e: Estado, l: Lancamento): Int? {
        fun posicao(tipo: TipoLancamento): Int? {
            val doMes = e.lancamentos.filter { it.tipo == tipo && YearMonth.from(it.data) == YearMonth.from(l.data) }
            return doMes.indexOfFirst { it.id == l.id }.takeIf { it >= 0 }?.plus(1)
        }
        return when (l.tipo) {
            TipoLancamento.APOSTA, TipoLancamento.PREMIO ->
                l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }?.numero?.takeIf { it > 0 }
            TipoLancamento.APORTE -> posicao(TipoLancamento.APORTE)
            TipoLancamento.RATEIO ->
                e.rateios.firstOrNull { it.numero == l.rateioNumero }?.numeroNoMes?.takeIf { it > 0 } ?: posicao(TipoLancamento.RATEIO)
            else -> null
        }
    }

    fun aportesDoMes(e: Estado, mes: YearMonth): Long =
        e.lancamentos.filter { it.tipo == TipoLancamento.APORTE && YearMonth.from(it.data) == mes }.sumOf { it.valor }

    fun aporteMensalRegistrado(e: Estado, mes: YearMonth): Boolean =
        e.lancamentos.any { it.tipo == TipoLancamento.APORTE && !it.eventual && YearMonth.from(it.data) == mes }

    /**
     * O aporte mensal de [mes] é cobrado? A partir do mês escolhido no cadastro; sem essa
     * escolha, no mês do cadastro só se ele foi feito no dia 1º.
     */
    fun aporteMensalDevido(e: Estado, mes: YearMonth): Boolean {
        e.inicioAportes?.let { inicio -> runCatching { YearMonth.parse(inicio) }.getOrNull()?.let { return !mes.isBefore(it) } }
        val cadastro = e.cadastradoEm ?: return true
        val mesCadastro = YearMonth.from(cadastro)
        return mes.isAfter(mesCadastro) || (mes == mesCadastro && cadastro.dayOfMonth == 1)
    }

    fun aporteMensalPendente(e: Estado, hoje: LocalDate): Boolean {
        val mes = YearMonth.from(hoje)
        return e.cadastroConcluido && aporteMensalDevido(e, mes) && !aporteMensalRegistrado(e, mes)
    }

    /** Aporte mensal do grupo (o valor de Ajustes × integrantes). */
    fun valorAporteMensal(e: Estado): Long = e.config.aporteMensal * e.pessoas.size.coerceAtLeast(1)

    /** Saldo no fim do mês anterior a [mes]. O saldo inicial do cadastro conta como "anterior". */
    fun saldoAnterior(e: Estado, mes: YearMonth): Long = e.lancamentos.filter {
        it.data.isBefore(mes.atDay(1)) || (it.tipo == TipoLancamento.SALDO_INICIAL && YearMonth.from(it.data) == mes)
    }.sumOf { it.valor }

    fun rendimentoDoMes(e: Estado, mes: YearMonth): Long =
        e.lancamentos.filter { it.tipo == TipoLancamento.RENDIMENTO && YearMonth.from(it.data) == mes }.sumOf { it.valor }

    private val TIPOS_DO_EXTRATO = setOf(
        TipoLancamento.APORTE, TipoLancamento.APOSTA, TipoLancamento.PREMIO, TipoLancamento.RATEIO,
        TipoLancamento.COMPLEMENTO, TipoLancamento.DIVISAO_FINAL,
    )

    fun ordenados(lista: List<Lancamento>): List<Lancamento> = lista.sortedWith(compareBy<Lancamento>({ it.data }, { it.criadoEm }))

    /** Movimentações do mês, na ordem em que aconteceram, no formato do EXTRATO. */
    fun movimentosDoMes(e: Estado, mes: YearMonth): List<Mensagens.Movimento> =
        ordenados(e.lancamentos.filter { YearMonth.from(it.data) == mes && it.tipo in TIPOS_DO_EXTRATO })
            .map { l ->
                val aposta = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
                when (l.tipo) {
                    TipoLancamento.APORTE -> Mensagens.Movimento(l.data, Mensagens.Categoria.APORTE, Mensagens.descricaoAporte(), l.valor)
                    TipoLancamento.COMPLEMENTO -> Mensagens.Movimento(l.data, Mensagens.Categoria.APORTE, "COMPLEMENTO ADM.", l.valor)
                    TipoLancamento.APOSTA -> Mensagens.Movimento(
                        l.data, Mensagens.Categoria.APOSTA,
                        aposta?.let { Mensagens.descricaoAposta(it.numero, it.modalidade) } ?: "APOSTA", l.valor,
                    )
                    TipoLancamento.PREMIO -> Mensagens.Movimento(
                        l.data, Mensagens.Categoria.PREMIO,
                        aposta?.let { Mensagens.descricaoPremio(it.numero, it.modalidade) } ?: "PRÊMIO", l.valor,
                    )
                    TipoLancamento.DIVISAO_FINAL -> Mensagens.Movimento(l.data, Mensagens.Categoria.RATEIO, "DIVISÃO FINAL", l.valor)
                    else -> Mensagens.Movimento(l.data, Mensagens.Categoria.RATEIO, Mensagens.descricaoRateio(), l.valor)
                }
            }

    fun textoExtrato(e: Estado, mes: YearMonth): String =
        Mensagens.extrato(mes, saldoAnterior(e, mes), rendimentoDoMes(e, mes), movimentosDoMes(e, mes))

    /** Descrição curta de um lançamento, para as listas do app ("APOSTA Nº05 LM"). */
    fun descricao(e: Estado, l: Lancamento): String {
        val aposta = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
        fun n(v: Int) = v.toString().padStart(2, '0')
        return when (l.tipo) {
            TipoLancamento.SALDO_INICIAL -> "SALDO INICIAL"
            TipoLancamento.APORTE -> if (l.eventual) "APORTE ADICIONAL" else "APORTE"
            TipoLancamento.APOSTA -> aposta?.let { "APOSTA Nº${n(it.numero)} ${it.modalidade.sigla}" } ?: "APOSTA"
            TipoLancamento.PREMIO -> aposta?.let { "PRÊMIO Nº${n(it.numero)} ${it.modalidade.sigla}" } ?: "PRÊMIO"
            TipoLancamento.RATEIO -> "RATEIO"
            TipoLancamento.RENDIMENTO -> "RENDIMENTO"
            TipoLancamento.COMPLEMENTO -> "COMPLEMENTO DO ADM."
            TipoLancamento.DIVISAO_FINAL -> "DIVISÃO FINAL"
        }
    }

    /** O que foi apostado, para as linhas "NÚMEROS: ..." do bloco da aposta. */
    fun jogoDe(a: Aposta): Mensagens.Jogo = Mensagens.Jogo(
        numeros = a.numeros,
        trevos = a.trevos,
        extra = a.extra,
        palpitesLoteca = a.palpitesLoteca,
        bilheteFederal = a.bilheteFederal,
        fracoesFederal = a.fracoesFederal,
    )

    /** Bloco APOSTA do termo, com o saldo de antes da aposta. */
    private fun blocoAposta(e: Estado, a: Aposta, hoje: LocalDate, saldoAtual: Long): String = Mensagens.aposta(
        numero = numeroDe(e, a, hoje),
        dataAposta = if (a.registrada) a.dataAposta else hoje,
        modalidade = a.modalidade,
        concurso = a.concurso,
        premioEstimado = a.premioEstimado,
        dataSorteio = a.dataSorteio,
        jogo = jogoDe(a),
        saldoAtual = saldoAtual,
        valor = a.valor,
    )

    /** Saldo logo antes do lançamento de [tipo] ligado à aposta [apostaId]. */
    fun saldoAntesDe(e: Estado, apostaId: String, tipo: TipoLancamento): Long {
        val l = e.lancamentos.firstOrNull { it.apostaId == apostaId && it.tipo == tipo } ?: return saldo(e)
        return e.lancamentos.filter { it.criadoEm < l.criadoEm }.sumOf { it.valor }
    }

    // ---------- provisionado e extra ----------

    data class Provisao(
        val apostasRestantes: Int,
        val valorPorAposta: Long,
        val aportesDoMes: Long,
        /** Valor ainda necessário para as apostas que faltam no mês, nunca acima dos aportes do mês. */
        val provisionado: Long,
    )

    fun provisao(e: Estado, hoje: LocalDate): Provisao {
        val restantes = Calendario.apostasRestantesNoMes(hoje, apostaFeitaEm(e, hoje))
        val porAposta = CalculoRateio.valorPorAposta(e.config.aporteMensal, e.pessoas.size.coerceAtLeast(1))
        val aportes = aportesDoMes(e, YearMonth.from(hoje))
        return Provisao(restantes, porAposta, aportes, CalculoRateio.provisionado(restantes, porAposta, aportes))
    }

    /** Extra = Saldo Líquido − Provisionado (o que pode ser rateado). Pode ser negativo. */
    fun extra(e: Estado, hoje: LocalDate): Long = saldo(e) - provisao(e, hoje).provisionado

    // ---------- comprovantes ----------

    fun nomeImagem(tipo: TipoComprovante, id: String): String = "recibo-${tipo.name.lowercase()}-${id.take(8)}.png"

    /** Partes do registro que entram no código de autenticação. */
    fun partesAutenticacao(c: Comprovante): List<String> =
        listOf(c.tipo.name, c.titulo, c.texto, c.data.toString(), c.criadoEm.toString()) +
            c.anexos.map { "${it.nome}:${it.sha256.orEmpty()}" }

    private fun novoComprovante(
        e: Estado,
        tipo: TipoComprovante,
        titulo: String,
        texto: String,
        data: LocalDate,
        agora: Long,
        anexos: List<Arquivo> = emptyList(),
        envioDispensado: Boolean = false,
        apostaId: String? = null,
        rateioNumero: Int? = null,
        mes: String? = null,
    ): Comprovante {
        val id = novoId()
        val rascunho = Comprovante(
            id = id, tipo = tipo, titulo = titulo, texto = texto, data = data, criadoEm = agora,
            imagem = Arquivo(nomeImagem(tipo, id), nomeImagem(tipo, id), "image/png", agora),
            anexos = anexos, autenticacao = "", envioDispensado = envioDispensado,
            apostaId = apostaId, rateioNumero = rateioNumero, mes = mes,
        )
        return rascunho.copy(autenticacao = Autenticacao.calcular(e.comprovantes.lastOrNull()?.autenticacao, partesAutenticacao(rascunho)))
    }

    /** Índice do primeiro comprovante cuja autenticação não confere (null = corrente íntegra). */
    fun primeiraAutenticacaoQuebrada(e: Estado): Int? {
        var anterior: String? = null
        e.comprovantes.forEachIndexed { i, c ->
            if (Autenticacao.calcular(anterior, partesAutenticacao(c)) != c.autenticacao) return i
            anterior = c.autenticacao
        }
        return null
    }

    fun comprovantesPendentesDeEnvio(e: Estado): List<Comprovante> =
        e.comprovantes.filter { !it.envioDispensado && !e.enviado(it) }

    fun registrarEnvio(e: Estado, comprovanteId: String, agora: Long): Estado {
        require(e.comprovantes.any { it.id == comprovanteId }) { "Comprovante não encontrado." }
        return e.copy(envios = e.envios + Envio(comprovanteId, agora))
    }

    /** Comprovante que registrou um lançamento (o do extrato, para o rendimento do mês). */
    fun comprovanteDoLancamento(e: Estado, l: Lancamento): Comprovante? = when (l.tipo) {
        TipoLancamento.APORTE -> e.comprovantes.firstOrNull { it.tipo == TipoComprovante.APORTE && it.criadoEm == l.criadoEm }
        TipoLancamento.APOSTA -> e.comprovantes.lastOrNull { it.tipo == TipoComprovante.APOSTA && it.apostaId == l.apostaId }
        TipoLancamento.PREMIO -> e.comprovantes.lastOrNull { it.tipo == TipoComprovante.APURACAO && it.apostaId == l.apostaId }
        TipoLancamento.RATEIO -> e.comprovantes.lastOrNull { it.tipo == TipoComprovante.RATEIO && it.rateioNumero == l.rateioNumero }
        TipoLancamento.RENDIMENTO -> e.comprovantes.lastOrNull { it.tipo == TipoComprovante.EXTRATO && it.mes == YearMonth.from(l.data).toString() }
        TipoLancamento.COMPLEMENTO, TipoLancamento.DIVISAO_FINAL -> e.comprovantes.lastOrNull { it.tipo == TipoComprovante.EXTINCAO }
        TipoLancamento.SALDO_INICIAL -> null
    }

    fun comprovanteDaAposta(e: Estado, a: Aposta): Comprovante? =
        e.comprovantes.lastOrNull { it.tipo == TipoComprovante.APOSTA && it.apostaId == a.id }

    fun comprovanteDaApuracao(e: Estado, a: Aposta): Comprovante? =
        e.comprovantes.lastOrNull { it.tipo == TipoComprovante.APURACAO && it.apostaId == a.id }

    // ---------- aporte ----------

    /** Prévia do bloco APORTE (o saldo atual é o de agora; o número é o que o aporte vai receber). */
    fun textoAporte(e: Estado, data: LocalDate, valor: Long, adicional: Boolean): String =
        Mensagens.aporte(proximoNumeroAporte(e, data), data, saldo(e), valor, adicional)

    /** Título do próximo aporte com data em [data] ("APORTE Nº 1 - 01/06/2026"). */
    fun tituloAporte(e: Estado, data: LocalDate): String = Mensagens.tituloAporte(proximoNumeroAporte(e, data), data)

    /** Aporte mensal: valor de Ajustes, no dia 1º do mês, só registrado (sem mensagem). */
    fun registrarAporteMensal(e: Estado, hoje: LocalDate, agora: Long): Estado {
        val mes = YearMonth.from(hoje)
        require(!aporteMensalRegistrado(e, mes)) { "O aporte mensal de ${Datas.mesAno(mes)} já foi registrado." }
        return registrarAporte(e, mes.atDay(1), valorAporteMensal(e), eventual = false, agora = agora)
    }

    /** Aporte adicional (com a concordância dos 3, registrada no grupo): data e valor livres. */
    fun registrarAporteAdicional(e: Estado, data: LocalDate, valor: Long, agora: Long): Estado =
        registrarAporte(e, data, valor, eventual = true, agora = agora)

    private fun registrarAporte(e: Estado, data: LocalDate, valor: Long, eventual: Boolean, agora: Long): Estado {
        require(valor > 0) { "Informe o valor do aporte." }
        require(!mesFechado(e, YearMonth.from(data))) { "O extrato de ${Datas.mesAno(YearMonth.from(data))} já foi fechado." }
        // Nº do aporte no mês (recomeça a cada mês), como no termo.
        val numero = proximoNumeroAporte(e, data)
        val texto = Mensagens.aporte(numero, data, saldo(e), valor, adicional = eventual)
        val lanc = Lancamento(id = novoId(), data = data, criadoEm = agora, tipo = TipoLancamento.APORTE, valor = valor, eventual = eventual)
        val c = novoComprovante(
            e, TipoComprovante.APORTE, Mensagens.tituloAporte(numero, data), texto, data, agora,
            envioDispensado = !eventual,
        )
        return e.copy(lancamentos = e.lancamentos + lanc, comprovantes = e.comprovantes + c)
    }

    // ---------- aposta ----------

    fun salvarRascunho(e: Estado, a: Aposta): Estado {
        require(!a.registrada) { "A aposta já foi registrada." }
        val atual = e.apostas.firstOrNull { it.id == a.id }
        require(atual == null || !atual.registrada) { "A aposta já foi registrada." }
        return e.copy(apostas = if (atual != null) e.apostas.map { if (it.id == a.id) a else it } else e.apostas + a)
    }

    fun excluirRascunho(e: Estado, id: String): Estado =
        e.copy(apostas = e.apostas.filterNot { it.id == id && !it.registrada })

    /** Bloco APOSTA. Para aposta já registrada, vale o comprovante (saldo de antes dela). */
    fun textoAposta(e: Estado, a: Aposta, hoje: LocalDate): String =
        comprovanteDaAposta(e, a)?.texto ?: run {
            val saldoAntes = if (a.registrada) saldoAntesDe(e, a.id, TipoLancamento.APOSTA) else saldo(e)
            blocoAposta(e, a, hoje, saldoAntes)
        }

    fun tituloAposta(e: Estado, a: Aposta, hoje: LocalDate): String =
        Mensagens.tituloAposta(numeroDe(e, a, hoje), if (a.registrada) a.dataAposta else hoje)

    /** Uma diferença entre a aposta do app e o comprovante da Caixa. */
    data class Divergencia(val campo: String, val noApp: String, val naCaixa: String)

    /**
     * Compara a aposta com o que foi lido no comprovante da Caixa. Só entra o que a leitura
     * conseguiu com segurança (o resto da leitura fica null e não conta).
     */
    fun divergencias(a: Aposta, l: LeituraComprovante.Lida): List<Divergencia> = buildList {
        val m = a.modalidade
        fun nums(v: List<Int>) = if (m.formato == Formato.COLUNAS) v.joinToString(" ") else v.sorted().joinToString(" ") { m.rotulo(it) }
        l.concurso?.takeIf { it != a.concurso }?.let { add(Divergencia("Concurso", a.concurso.toString(), it.toString())) }
        l.numeros?.let { lidos ->
            val iguais = if (m.formato == Formato.COLUNAS) lidos == a.numeros else lidos.sorted() == a.numeros.sorted()
            if (!iguais) add(Divergencia("Números", nums(a.numeros), nums(lidos)))
        }
        l.trevos?.takeIf { it.sorted() != a.trevos.sorted() }?.let { add(Divergencia("Trevos", a.trevos.sorted().joinToString(" "), it.joinToString(" "))) }
        l.extra?.takeIf { !Conferencia.mesmoNome(it, a.extra) }?.let {
            add(Divergencia(if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte" else "Time do Coração", a.extra ?: "—", it))
        }
        l.palpitesLoteca?.takeIf { it != a.palpitesLoteca }?.let { add(Divergencia("Palpites", a.palpitesLoteca.joinToString(" "), it.joinToString(" "))) }
        l.bilhete?.takeIf { it.filter(Char::isDigit) != a.bilheteFederal.orEmpty().filter(Char::isDigit) }?.let {
            add(Divergencia("Bilhete", a.bilheteFederal.orEmpty(), it))
        }
    }

    /** A aposta como está no comprovante da Caixa (o que foi lido com segurança). */
    fun ajustarPeloComprovante(a: Aposta, l: LeituraComprovante.Lida): Aposta = a.copy(
        concurso = l.concurso ?: a.concurso,
        numeros = l.numeros ?: a.numeros,
        trevos = l.trevos ?: a.trevos,
        extra = l.extra ?: a.extra,
        palpitesLoteca = l.palpitesLoteca ?: a.palpitesLoteca,
        bilheteFederal = l.bilhete ?: a.bilheteFederal,
    )

    /** Dá o número do mês, marca como registrada, desconta do saldo e cria o comprovante. */
    fun registrarAposta(e: Estado, id: String, hoje: LocalDate, agora: Long): Estado {
        val a = e.apostas.first { it.id == id }
        require(!a.registrada) { "A aposta já foi registrada." }
        require(a.valor > 0) { "Informe o valor da aposta." }
        require(a.concurso > 0) { "Informe o número do concurso." }
        val anexo = requireNotNull(a.comprovante) { "Anexe o comprovante da Caixa." }
        val numero = proximoNumero(e, hoje)
        val registrada = a.copy(numero = numero, dataAposta = hoje, eventual = false, registrada = true)
        val texto = blocoAposta(e, registrada, hoje, saldo(e))
        val lanc = Lancamento(
            id = novoId(), data = hoje, criadoEm = agora, tipo = TipoLancamento.APOSTA, valor = -a.valor, apostaId = id,
        )
        val c = novoComprovante(
            e, TipoComprovante.APOSTA, Mensagens.tituloAposta(numero, hoje), texto, hoje, agora,
            anexos = listOf(anexo), apostaId = id,
        )
        return e.copy(
            apostas = e.apostas.map { if (it.id == id) registrada else it },
            lancamentos = e.lancamentos + lanc,
            comprovantes = e.comprovantes + c,
        )
    }

    // ---------- apuração ----------

    /** Registro da apuração a partir do resultado da Caixa e da conferência. */
    fun apuracaoDe(c: Concurso, r: Conferencia.Resultado, premio: Long, agora: Long) = Apuracao(
        acertos = r.acertos,
        premio = premio,
        dezenasSorteadas = c.dezenas,
        segundoSorteio = c.segundoSorteio,
        trevosSorteados = c.trevos,
        extraSorteado = c.timeOuMes,
        resultadosLoteca = (0 until Volante.JOGOS_LOTECA).map { c.jogosLoteca.getOrNull(it)?.resultado.orEmpty() },
        premiosFederal = c.bilhetesFederal,
        faixa = r.faixas.joinToString(" + ") { it.descricao }.ifBlank { null },
        faixasPremiadas = r.faixas.map { it.faixa },
        rateioOficial = c.faixas,
        acumulou = if (c.temResultado) c.acumulado else null,
        precisaConfirmar = r.precisaConfirmarValor || r.observacoes.isNotEmpty(),
        conferidaEm = agora,
    )

    /** Guarda a conferência automática (ainda sem confirmar): só um aviso, não é registro. */
    fun guardarConferencia(e: Estado, id: String, apuracao: Apuracao): Estado =
        e.copy(apostas = e.apostas.map { if (it.id == id && it.registrada && it.apuracao?.confirmadaEm == null) it.copy(apuracao = apuracao) else it })

    fun mesFechado(e: Estado, mes: YearMonth): Boolean = e.extratos.any { it.mes == mes.toString() }

    /** O prêmio entra na data do sorteio; se aquele mês já foi fechado no extrato, entra hoje. */
    fun dataDoPremio(e: Estado, a: Aposta, hoje: LocalDate): LocalDate {
        val s = a.dataSorteio ?: return hoje
        return if (s.isAfter(hoje) || mesFechado(e, YearMonth.from(s))) hoje else s
    }

    /** Bloco APURAÇÃO da aposta [a], apurada em [hoje], com o [saldoAtual] de antes do prêmio. */
    private fun blocoApuracao(a: Aposta, hoje: LocalDate, saldoAtual: Long, premio: Long, resultado: String): String =
        Mensagens.apuracao(
            numero = a.numero,
            dataAposta = a.dataAposta,
            modalidade = a.modalidade,
            concurso = a.concurso,
            premioEstimado = a.premioEstimado,
            dataApuracao = hoje,
            saldoAtual = saldoAtual,
            premio = premio,
            resultado = resultado,
        )

    /**
     * Prévia do bloco APURAÇÃO (o saldo atual é o de agora, antes do prêmio; o resultado vai
     * na imagem anexada). Depois de registrada, vale o comprovante.
     */
    fun textoApuracao(e: Estado, a: Aposta, premio: Long, hoje: LocalDate): String =
        comprovanteDaApuracao(e, a)?.texto ?: blocoApuracao(a, hoje, saldo(e), premio, Mensagens.VIDE_ANEXO)

    /** Confirma a apuração: soma o prêmio ao saldo e cria o comprovante (com a imagem dos acertos). */
    fun confirmarApuracao(e: Estado, id: String, apuracao: Apuracao, hoje: LocalDate, agora: Long, imagem: Arquivo? = null): Estado {
        val a = e.apostas.first { it.id == id }
        require(a.registrada) { "Registre a aposta antes de apurar." }
        require(a.apuracao?.confirmadaEm == null) { "Esta aposta já foi apurada." }
        require(apuracao.premio >= 0) { "Prêmio inválido." }
        val confirmada = apuracao.copy(confirmadaEm = agora)
        // O resultado vai na imagem dos acertos; sem ela, fica a contagem no próprio bloco.
        val resultado = if (imagem != null) Mensagens.VIDE_ANEXO else apuracao.acertos
        val texto = blocoApuracao(a, hoje, saldo(e), apuracao.premio, resultado)
        val premio = if (apuracao.premio > 0) {
            listOf(
                Lancamento(
                    id = novoId(), data = dataDoPremio(e, a, hoje), criadoEm = agora,
                    tipo = TipoLancamento.PREMIO, valor = apuracao.premio, apostaId = id,
                ),
            )
        } else {
            emptyList()
        }
        val c = novoComprovante(
            e, TipoComprovante.APURACAO, Mensagens.tituloApuracao(a.numero, hoje), texto, hoje, agora,
            anexos = listOfNotNull(imagem), apostaId = id,
        )
        return e.copy(
            apostas = e.apostas.map { if (it.id == id) it.copy(apuracao = confirmada) else it },
            lancamentos = e.lancamentos + premio,
            comprovantes = e.comprovantes + c,
        )
    }

    // ---------- rateio ----------

    data class CalculoDoRateio(
        val extra: Long,
        val pedido: Long,
        val cota: Long,
        /** Soma das cotas (o que sai do saldo). */
        val total: Long,
        /** Centavos que sobram do arredondamento para baixo e ficam no saldo. */
        val sobra: Long,
        val saldoFinal: Long,
    )

    /** Rateio de [pedido] (no máximo o Extra): cota = pedido ÷ integrantes, arredondada para baixo. */
    fun calcularRateio(e: Estado, hoje: LocalDate, pedido: Long): CalculoDoRateio {
        val n = e.pessoas.size.coerceAtLeast(1)
        val cota = pedido.coerceAtLeast(0) / n
        val total = cota * n
        return CalculoDoRateio(extra(e, hoje), pedido, cota, total, pedido - total, saldo(e) - total)
    }

    /** Prévia do bloco RATEIO de [total] em [hoje] (o número é o que o rateio vai receber no mês). */
    fun textoRateio(e: Estado, hoje: LocalDate, total: Long): String =
        Mensagens.rateio(proximoNumeroRateio(e, hoje), hoje, saldo(e), total)

    /** Título do próximo rateio em [hoje] ("RATEIO Nº1 - 14/06/26"). */
    fun tituloRateio(e: Estado, hoje: LocalDate): String = Mensagens.tituloRateio(proximoNumeroRateio(e, hoje), hoje)

    fun registrarRateio(e: Estado, hoje: LocalDate, pedido: Long, comprovantes: List<Arquivo>, agora: Long): Estado {
        val extra = extra(e, hoje)
        require(extra > 0) { "Não há valor a ser rateado." }
        require(pedido <= extra) { "Valor acima de ${Dinheiro.rs(extra)} não pode ser rateado." }
        val calc = calcularRateio(e, hoje, pedido)
        require(calc.cota > 0) { "O valor é pequeno demais para dividir entre ${e.pessoas.size}." }
        require(comprovantes.size == e.pessoas.size) { "Anexe os comprovantes dos ${e.pessoas.size} Pix." }
        // [numero] é só a chave interna (corrida); no comprovante vai o número do rateio no mês.
        val numero = e.proximoRateio
        val noMes = proximoNumeroRateio(e, hoje)
        val saldoAntes = saldo(e)
        val rateio = Rateio(numero, hoje, saldoAntes, provisao(e, hoje).provisionado, calc.cota, comprovantes, agora, numeroNoMes = noMes)
        val lanc = Lancamento(
            id = novoId(), data = hoje, criadoEm = agora, tipo = TipoLancamento.RATEIO,
            valor = -calc.total, rateioNumero = numero,
        )
        val c = novoComprovante(
            e, TipoComprovante.RATEIO, Mensagens.tituloRateio(noMes, hoje), Mensagens.rateio(noMes, hoje, saldoAntes, calc.total), hoje, agora,
            anexos = comprovantes, rateioNumero = numero,
        )
        return e.copy(
            rateios = e.rateios + rateio,
            lancamentos = e.lancamentos + lanc,
            comprovantes = e.comprovantes + c,
            proximoRateio = numero + 1,
        )
    }

    // ---------- extrato ----------

    data class Conferido(
        /** Saldo que o app calcula agora, sem o rendimento já lançado para o mês. */
        val calculado: Long,
        val informado: Long,
        val rendimento: Long,
        val alerta: String?,
    )

    /**
     * O Administrador digita o saldo atual da caixinha; o rendimento é a diferença
     * para o saldo calculado pelos lançamentos.
     */
    fun conferirSaldo(e: Estado, mes: YearMonth, informado: Long): Conferido {
        val calculado = saldo(e) - rendimentoDoMes(e, mes)
        val rendimento = informado - calculado
        val limite = maxOf(100L, calculado * 2 / 100)
        val alerta = when {
            rendimento < 0 -> "O saldo informado é ${Dinheiro.rs(-rendimento)} menor " +
                "que o calculado. Confira se falta registrar alguma aposta ou rateio."
            rendimento > limite -> "Um rendimento de ${Dinheiro.rs(rendimento)} " +
                "é alto para um mês. Confira se falta registrar algum aporte ou prêmio."
            else -> null
        }
        return Conferido(calculado, informado, rendimento, alerta)
    }

    /** null = pode registrar; senão, o motivo. */
    fun impedimentoDoExtrato(e: Estado, mes: YearMonth, hoje: LocalDate): String? = when {
        mesFechado(e, mes) -> "O extrato de ${Datas.mesAno(mes)} já foi registrado."
        hoje.isBefore(mes.atEndOfMonth()) ->
            if (hoje.plusDays(1) == mes.atEndOfMonth()) "O extrato só pode ser registrado a partir de amanhã, último dia do mês."
            else "O extrato só pode ser registrado a partir do último dia do mês (${Datas.longa(mes.atEndOfMonth())})."
        else -> null
    }

    private fun comRendimento(e: Estado, mes: YearMonth, informado: Long, hoje: LocalDate, agora: Long): Pair<Estado, Conferido> {
        val c = conferirSaldo(e, mes, informado)
        val data = if (YearMonth.from(hoje) == mes) hoje else mes.atEndOfMonth()
        val rendimento = if (c.rendimento != 0L) {
            listOf(
                Lancamento(
                    id = novoId(), data = data, criadoEm = agora, tipo = TipoLancamento.RENDIMENTO,
                    valor = c.rendimento, obs = "Rendimento de $mes",
                ),
            )
        } else {
            emptyList()
        }
        return e.copy(lancamentos = e.lancamentos + rendimento) to c
    }

    /** Como o bloco EXTRATO vai ficar com o saldo [informado]. */
    fun previaExtrato(e: Estado, mes: YearMonth, informado: Long, hoje: LocalDate): String =
        textoExtrato(comRendimento(e, mes, informado, hoje, Long.MAX_VALUE).first, mes)

    /** Lança o rendimento do mês, fecha o mês e cria o comprovante. Um extrato por mês, sem refazer. */
    fun registrarExtrato(e: Estado, mes: YearMonth, informado: Long, hoje: LocalDate, agora: Long): Estado {
        impedimentoDoExtrato(e, mes, hoje)?.let { throw IllegalArgumentException(it) }
        val (novo, c) = comRendimento(e, mes, informado, hoje, agora)
        val texto = textoExtrato(novo, mes)
        val comprovante = novoComprovante(
            e, TipoComprovante.EXTRATO, Mensagens.tituloExtrato(hoje), texto, hoje, agora, mes = mes.toString(),
        )
        return novo.copy(
            extratos = e.extratos + Extrato(mes.toString(), informado, c.rendimento, agora),
            comprovantes = e.comprovantes + comprovante,
        )
    }

    /** Meses sem extrato: os já terminados e o atual a partir do último dia. */
    fun extratosPendentes(e: Estado, hoje: LocalDate): List<YearMonth> {
        if (!e.cadastroConcluido) return emptyList()
        val inicio = listOfNotNull(e.cadastradoEm, e.lancamentos.minOfOrNull { it.data }).minOrNull() ?: return emptyList()
        val atual = YearMonth.from(hoje)
        var m = YearMonth.from(inicio)
        val lista = mutableListOf<YearMonth>()
        while (!m.isAfter(atual)) {
            val terminou = m.isBefore(atual) || Calendario.ehUltimoDiaDoMes(hoje)
            if (terminou && !mesFechado(e, m)) lista += m
            m = m.plusMonths(1)
        }
        return lista
    }

    // ---------- extinção (Parte 5) ----------

    data class DivisaoFinal(
        val saldo: Long,
        /** Saldo ÷ integrantes, arredondado para cima na segunda casa decimal. */
        val cota: Long,
        val total: Long,
        /** Diferença do arredondamento, coberta pelo Administrador. */
        val complemento: Long,
    )

    fun divisaoFinal(e: Estado): DivisaoFinal {
        val s = saldo(e)
        val n = e.pessoas.size.coerceAtLeast(1)
        val cota = if (s <= 0) 0 else (s + n - 1) / n
        return DivisaoFinal(s, cota, cota * n, cota * n - s)
    }

    /** O que ainda impede a extinção (vazio = pode extinguir). */
    fun impedimentosDaExtincao(e: Estado): List<String> = buildList {
        val naoApuradas = e.apostas.count { it.registrada && it.apuracao?.confirmadaEm == null }
        if (naoApuradas > 0) add("$naoApuradas aposta(s) ainda sem apuração.")
        val rascunhos = rascunhos(e).size
        if (rascunhos > 0) add("$rascunhos aposta(s) em andamento: registre ou descarte.")
        val naoEnviados = comprovantesPendentesDeEnvio(e).size
        if (naoEnviados > 0) add("$naoEnviados comprovante(s) ainda não enviados ao grupo.")
        if (saldo(e) <= 0) add("Não há saldo remanescente para dividir.")
    }

    /**
     * Começa a extinção (Parte 5 do termo), depois de resolvidas todas as pendências.
     * A extinção forçada não passa por aqui: é a formatação do app (Repositorio.formatar).
     */
    fun iniciarExtincao(e: Estado, agora: Long): Estado {
        require(e.extincao == null) { "A extinção já começou." }
        impedimentosDaExtincao(e).firstOrNull()?.let { throw IllegalArgumentException(it) }
        return e.copy(extincao = Extincao(iniciadaEm = agora))
    }

    fun anexarNaExtincao(e: Estado, pessoaId: String, arquivo: Arquivo): Estado {
        val x = requireNotNull(e.extincao) { "A extinção não começou." }
        require(x.registradaEm == null) { "A divisão final já foi registrada." }
        return e.copy(extincao = x.copy(comprovantes = x.comprovantes + (pessoaId to arquivo)))
    }

    fun textoExtincao(e: Estado): String {
        val d = divisaoFinal(e)
        return Mensagens.extincao(d.saldo, d.complemento, d.total)
    }

    /** Registra a divisão final: complemento do Administrador, saída das cotas e o comprovante. */
    fun registrarExtincao(e: Estado, hoje: LocalDate, agora: Long): Estado {
        val x = requireNotNull(e.extincao) { "A extinção não começou." }
        require(x.registradaEm == null) { "A divisão final já foi registrada." }
        val anexos = e.pessoas.map { p -> requireNotNull(x.comprovantes[p.id]) { "Falta o comprovante do Pix de ${p.apelido}." } }
        val d = divisaoFinal(e)
        require(d.saldo > 0) { "Não há saldo para dividir." }
        val lancs = buildList {
            if (d.complemento > 0) {
                add(
                    Lancamento(
                        id = novoId(), data = hoje, criadoEm = agora, tipo = TipoLancamento.COMPLEMENTO,
                        valor = d.complemento, pessoaId = e.administrador?.id, obs = "Arredondamento coberto pelo Administrador",
                    ),
                )
            }
            add(Lancamento(id = novoId(), data = hoje, criadoEm = agora + 1, tipo = TipoLancamento.DIVISAO_FINAL, valor = -d.total))
        }
        val c = novoComprovante(
            e, TipoComprovante.EXTINCAO, Mensagens.tituloExtincao(hoje), textoExtincao(e), hoje, agora,
            anexos = anexos, envioDispensado = true,
        )
        return e.copy(
            lancamentos = e.lancamentos + lancs,
            comprovantes = e.comprovantes + c,
            extincao = x.copy(registradaEm = agora),
        )
    }
}

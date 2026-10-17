package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.CalculoRateio
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.domain.Palpite
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun novoId(): String = UUID.randomUUID().toString()

val Aposta.palpite: Palpite get() = Palpite(numeros, trevos, extra, palpitesLoteca)

/**
 * Todas as regras de contabilidade do bolão, como funções puras sobre o [Estado].
 * Cada operação devolve um novo Estado; quem chama é que grava (Repositorio.atualizar).
 */
object Operacoes {

    // ---------- consultas ----------

    fun saldo(e: Estado): Long = e.lancamentos.sumOf { it.valor }

    fun apostaRegularFeitaEm(e: Estado, data: LocalDate, excetoId: String? = null): Boolean =
        e.apostas.any { it.registrada && !it.eventual && it.dataAposta == data && it.id != excetoId }

    /** APOSTA EVENTUAL: fora dos dias programados, ou a segunda em diante no mesmo dia. */
    fun ehEventual(e: Estado, data: LocalDate, excetoId: String? = null): Boolean =
        !Calendario.ehDiaProgramado(data) || apostaRegularFeitaEm(e, data, excetoId)

    /** APORTE EVENTUAL: fora do dia 1º. */
    fun aporteEhEventual(data: LocalDate): Boolean = data.dayOfMonth != 1

    fun rascunhos(e: Estado): List<Aposta> = e.apostas.filter { !it.registrada }.sortedByDescending { it.criadaEm }

    fun aportesDoMes(e: Estado, mes: YearMonth): Long =
        e.lancamentos.filter { it.tipo == TipoLancamento.APORTE && YearMonth.from(it.data) == mes }.sumOf { it.valor }

    /** Saldo no fim do mês anterior a [mes]. O saldo inicial do cadastro conta como "anterior". */
    fun saldoAnterior(e: Estado, mes: YearMonth): Long = e.lancamentos.filter {
        it.data.isBefore(mes.atDay(1)) || (it.tipo == TipoLancamento.SALDO_INICIAL && YearMonth.from(it.data) == mes)
    }.sumOf { it.valor }

    fun rendimentoDoMes(e: Estado, mes: YearMonth): Long =
        e.lancamentos.filter { it.tipo == TipoLancamento.RENDIMENTO && YearMonth.from(it.data) == mes }.sumOf { it.valor }

    /** Movimentações do mês, na ordem em que aconteceram, no formato do EXTRATO. */
    fun movimentosDoMes(e: Estado, mes: YearMonth): List<Mensagens.Movimento> =
        e.lancamentos
            .filter { YearMonth.from(it.data) == mes }
            .filter { it.tipo in setOf(TipoLancamento.APORTE, TipoLancamento.APOSTA, TipoLancamento.PREMIO, TipoLancamento.RATEIO) }
            .sortedWith(compareBy<Lancamento>({ it.data }, { it.criadoEm }))
            .map { l ->
                val apelido = e.pessoa(l.pessoaId)?.apelido ?: "?"
                val aposta = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
                when (l.tipo) {
                    TipoLancamento.APORTE -> Mensagens.Movimento(l.data, Mensagens.Categoria.APORTE, Mensagens.descricaoAporte(apelido), l.valor)
                    TipoLancamento.APOSTA -> Mensagens.Movimento(
                        l.data, Mensagens.Categoria.APOSTA,
                        aposta?.let { Mensagens.descricaoAposta(it.numero, it.modalidade) } ?: "APOSTA ", l.valor,
                    )
                    TipoLancamento.PREMIO -> Mensagens.Movimento(
                        l.data, Mensagens.Categoria.PREMIO,
                        aposta?.let { Mensagens.descricaoPremio(it.numero, it.modalidade) } ?: "PRÊMIO ", l.valor,
                    )
                    else -> Mensagens.Movimento(
                        l.data, Mensagens.Categoria.RATEIO,
                        Mensagens.descricaoRateio(l.rateioNumero ?: 0, apelido), l.valor,
                    )
                }
            }

    fun textoExtrato(e: Estado, mes: YearMonth): String =
        Mensagens.extrato(mes, saldoAnterior(e, mes), rendimentoDoMes(e, mes), movimentosDoMes(e, mes))

    /** Identificação da aposta (#132 010626 LF-3060 2M 030626). Rascunhos usam o próximo número. */
    fun identificacao(e: Estado, a: Aposta): String = Mensagens.identificacao(
        numeroJogo = if (a.registrada) a.numero else e.proximoJogo,
        dataAposta = a.dataAposta,
        modalidade = a.modalidade,
        concurso = a.concurso,
        premioEstimado = a.premioEstimado,
        dataSorteio = a.dataSorteio,
    )

    /** Mensagem APOSTA. Para aposta já registrada, o saldo é o de antes dela. */
    fun textoAposta(e: Estado, a: Aposta): String {
        val saldoAntes = if (a.registrada) saldoAntesDe(e, a.id, TipoLancamento.APOSTA) else saldo(e)
        val eventual = if (a.registrada) a.eventual else ehEventual(e, a.dataAposta, a.id)
        return Mensagens.aposta(identificacao(e, a), eventual, saldoAntes, a.valor)
    }

    /** Saldo logo antes do lançamento de [tipo] ligado à aposta [apostaId]. */
    fun saldoAntesDe(e: Estado, apostaId: String, tipo: TipoLancamento): Long {
        val l = e.lancamentos.firstOrNull { it.apostaId == apostaId && it.tipo == tipo } ?: return saldo(e)
        return e.lancamentos.filter { it.criadoEm < l.criadoEm }.sumOf { it.valor }
    }

    // ---------- aporte ----------

    /** Registra um aporte por integrante (o EXTRATO lista um por um). */
    fun registrarAportes(
        e: Estado,
        data: LocalDate,
        valores: List<Pair<String, Long>>,
        eventual: Boolean,
        agora: Long,
    ): Estado {
        val novos = valores.filter { it.second > 0 }.mapIndexed { i, (pessoaId, valor) ->
            Lancamento(
                id = novoId(), data = data, criadoEm = agora + i, tipo = TipoLancamento.APORTE,
                valor = valor, pessoaId = pessoaId, eventual = eventual,
            )
        }
        return e.copy(lancamentos = e.lancamentos + novos)
    }

    fun excluirLancamento(e: Estado, id: String): Estado {
        val l = e.lancamentos.firstOrNull { it.id == id } ?: return e
        require(l.tipo == TipoLancamento.APORTE || l.tipo == TipoLancamento.SALDO_INICIAL) {
            "Este lançamento é desfeito pela tela do jogo, rateio ou extrato."
        }
        return e.copy(lancamentos = e.lancamentos.filterNot { it.id == id })
    }

    // ---------- aposta ----------

    fun salvarRascunho(e: Estado, a: Aposta): Estado {
        require(!a.registrada) { "A aposta já foi registrada." }
        val existe = e.apostas.any { it.id == a.id }
        return e.copy(apostas = if (existe) e.apostas.map { if (it.id == a.id) a else it } else e.apostas + a)
    }

    fun excluirRascunho(e: Estado, id: String): Estado =
        e.copy(apostas = e.apostas.filterNot { it.id == id && !it.registrada })

    /** Dá o número do jogo, marca como registrada e desconta do saldo. */
    fun registrarAposta(e: Estado, id: String, agora: Long): Estado {
        val a = e.apostas.first { it.id == id }
        require(!a.registrada) { "A aposta já foi registrada." }
        require(a.valor > 0) { "Informe o valor da aposta." }
        val eventual = ehEventual(e, a.dataAposta, id)
        val numero = e.proximoJogo
        val registrada = a.copy(numero = numero, eventual = eventual, registrada = true)
        val lanc = Lancamento(
            id = novoId(), data = a.dataAposta, criadoEm = agora, tipo = TipoLancamento.APOSTA,
            valor = -a.valor, apostaId = id, eventual = eventual,
        )
        return e.copy(
            apostas = e.apostas.map { if (it.id == id) registrada else it },
            lancamentos = e.lancamentos + lanc,
            proximoJogo = numero + 1,
        )
    }

    /** Desfaz o registro (devolve o valor ao saldo). O número só volta se for o último usado. */
    fun desfazerAposta(e: Estado, id: String): Estado {
        val a = e.apostas.first { it.id == id }
        require(a.registrada) { "A aposta não está registrada." }
        require(a.apuracao?.confirmadaEm == null) { "Desfaça a apuração antes." }
        val proximo = if (a.numero == e.proximoJogo - 1) a.numero else e.proximoJogo
        return e.copy(
            apostas = e.apostas.map { if (it.id == id) it.copy(registrada = false, numero = 0, apuracao = null) else it },
            lancamentos = e.lancamentos.filterNot { it.apostaId == id && it.tipo == TipoLancamento.APOSTA },
            proximoJogo = proximo,
        )
    }

    // ---------- apuração ----------

    /** Guarda a conferência automática (ainda sem confirmar). */
    fun guardarConferencia(e: Estado, id: String, apuracao: Apuracao): Estado =
        e.copy(apostas = e.apostas.map { if (it.id == id && it.apuracao?.confirmadaEm == null) it.copy(apuracao = apuracao) else it })

    /** Confirma a apuração e soma o prêmio ao saldo (lançado na data do sorteio). */
    fun confirmarApuracao(e: Estado, id: String, apuracao: Apuracao, hoje: LocalDate, agora: Long): Estado {
        val a = e.apostas.first { it.id == id }
        require(a.registrada) { "Registre a aposta antes de apurar." }
        require(a.apuracao?.confirmadaEm == null) { "Esta aposta já foi apurada." }
        val confirmada = apuracao.copy(confirmadaEm = agora)
        val premio = if (apuracao.premio > 0) {
            listOf(
                Lancamento(
                    id = novoId(), data = a.dataSorteio ?: hoje, criadoEm = agora,
                    tipo = TipoLancamento.PREMIO, valor = apuracao.premio, apostaId = id,
                ),
            )
        } else {
            emptyList()
        }
        return e.copy(
            apostas = e.apostas.map { if (it.id == id) it.copy(apuracao = confirmada) else it },
            lancamentos = e.lancamentos + premio,
        )
    }

    fun desfazerApuracao(e: Estado, id: String): Estado = e.copy(
        apostas = e.apostas.map { if (it.id == id) it.copy(apuracao = it.apuracao?.copy(confirmadaEm = null)) else it },
        lancamentos = e.lancamentos.filterNot { it.apostaId == id && it.tipo == TipoLancamento.PREMIO },
    )

    /** Saldo para a mensagem APURAÇÃO: o de antes do prêmio, se já confirmada. */
    fun saldoParaApuracao(e: Estado, a: Aposta): Long =
        if (a.apuracao?.confirmadaEm != null) saldoAntesDe(e, a.id, TipoLancamento.PREMIO) else saldo(e)

    // ---------- rateio ----------

    data class SugestaoRateio(
        val apostasRestantes: Int,
        val valorPorAposta: Long,
        val aportesDoMes: Long,
        val provisionado: Long,
    )

    fun sugestaoProvisionado(e: Estado, hoje: LocalDate): SugestaoRateio {
        val restantes = Calendario.apostasRestantesNoMes(hoje, apostaRegularFeitaEm(e, hoje))
        val porAposta = CalculoRateio.valorPorAposta(e.config.aporteMensal, e.pessoas.size.coerceAtLeast(1))
        val aportes = aportesDoMes(e, YearMonth.from(hoje))
        return SugestaoRateio(restantes, porAposta, aportes, CalculoRateio.provisionado(restantes, porAposta, aportes))
    }

    fun registrarRateio(
        e: Estado,
        data: LocalDate,
        provisionado: Long,
        cota: Long,
        comprovantes: List<Arquivo>,
        agora: Long,
    ): Estado {
        require(cota > 0) { "Não há valor para ratear." }
        require(cota * e.pessoas.size <= saldo(e)) { "O rateio passa do saldo." }
        val numero = e.proximoRateio
        val rateio = Rateio(numero, data, saldo(e), provisionado, cota, comprovantes, agora)
        val lancs = e.pessoas.mapIndexed { i, p ->
            Lancamento(
                id = novoId(), data = data, criadoEm = agora + i, tipo = TipoLancamento.RATEIO,
                valor = -cota, pessoaId = p.id, rateioNumero = numero,
            )
        }
        return e.copy(
            rateios = e.rateios + rateio,
            lancamentos = e.lancamentos + lancs,
            proximoRateio = numero + 1,
        )
    }

    fun desfazerRateio(e: Estado, numero: Int): Estado = e.copy(
        rateios = e.rateios.filterNot { it.numero == numero },
        lancamentos = e.lancamentos.filterNot { it.tipo == TipoLancamento.RATEIO && it.rateioNumero == numero },
        proximoRateio = if (numero == e.proximoRateio - 1) numero else e.proximoRateio,
    )

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
            rendimento < 0 -> "O saldo informado é ${io.github.marcosfaria94.bolao.domain.Dinheiro.rs(-rendimento)} menor " +
                "que o calculado. Confira se falta registrar alguma aposta ou rateio."
            rendimento > limite -> "Um rendimento de ${io.github.marcosfaria94.bolao.domain.Dinheiro.rs(rendimento)} " +
                "é alto para um mês. Confira se falta registrar algum aporte ou prêmio."
            else -> null
        }
        return Conferido(calculado, informado, rendimento, alerta)
    }

    /** Lança o rendimento do mês (substitui um extrato anterior do mesmo mês). */
    fun registrarExtrato(e: Estado, mes: YearMonth, informado: Long, hoje: LocalDate, agora: Long): Estado {
        val c = conferirSaldo(e, mes, informado)
        val data = if (YearMonth.from(hoje) == mes) hoje else mes.atEndOfMonth()
        val semAnterior = e.lancamentos.filterNot { it.tipo == TipoLancamento.RENDIMENTO && YearMonth.from(it.data) == mes }
        val rendimento = if (c.rendimento != 0L) {
            listOf(
                Lancamento(
                    id = novoId(), data = data, criadoEm = agora, tipo = TipoLancamento.RENDIMENTO,
                    valor = c.rendimento, obs = "Rendimento de ${mes}",
                ),
            )
        } else {
            emptyList()
        }
        return e.copy(
            lancamentos = semAnterior + rendimento,
            extratos = e.extratos.filterNot { it.mes == mes.toString() } + Extrato(mes.toString(), informado, c.rendimento, agora),
        )
    }

    fun desfazerExtrato(e: Estado, mes: YearMonth): Estado = e.copy(
        lancamentos = e.lancamentos.filterNot { it.tipo == TipoLancamento.RENDIMENTO && YearMonth.from(it.data) == mes },
        extratos = e.extratos.filterNot { it.mes == mes.toString() },
    )
}

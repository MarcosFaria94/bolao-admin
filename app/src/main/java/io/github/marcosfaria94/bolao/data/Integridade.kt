package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.domain.Datas
import java.time.LocalDate

/**
 * Verificador de integridade: confere a corrente de autenticação dos comprovantes, a
 * impressão digital (SHA-256) de cada anexo guardado e se todo registro de dinheiro tem o
 * seu comprovante. Também acha um comprovante pelo código de autenticação impresso nele.
 */
object Integridade {

    data class Anexo(val comprovante: Comprovante, val arquivo: Arquivo)

    data class Relatorio(
        val comprovantes: Int,
        /** Índice do primeiro comprovante cuja autenticação não confere (null = corrente íntegra). */
        val quebraNaCorrente: Int?,
        val anexos: Int,
        val anexosQueConferem: Int,
        val anexosAlterados: List<Anexo>,
        val anexosFaltando: List<Anexo>,
        /** Anexos sem impressão digital guardada (não dá para conferir o conteúdo). */
        val anexosSemImpressao: List<Anexo>,
        /** Registros de dinheiro sem o comprovante correspondente. */
        val semComprovante: List<String>,
        /** Registros de versões antigas do app (até a 0.2.6), que não tinham comprovante. */
        val legados: Int = 0,
    ) {
        val integro: Boolean
            get() = quebraNaCorrente == null && anexosAlterados.isEmpty() && anexosFaltando.isEmpty() && semComprovante.isEmpty()
    }

    /** [sha256] calcula a impressão digital do arquivo guardado (null = arquivo não existe). */
    fun verificar(e: Estado, hoje: LocalDate = LocalDate.now(), sha256: (Arquivo) -> String?): Relatorio {
        val alterados = mutableListOf<Anexo>()
        val faltando = mutableListOf<Anexo>()
        val semImpressao = mutableListOf<Anexo>()
        var conferem = 0
        var total = 0
        for (c in e.comprovantes) for (a in c.anexos) {
            total++
            val atual = sha256(a)
            when {
                atual == null -> faltando += Anexo(c, a)
                a.sha256 == null -> semImpressao += Anexo(c, a)
                atual.equals(a.sha256, ignoreCase = true) -> conferem++
                else -> alterados += Anexo(c, a)
            }
        }
        return Relatorio(
            comprovantes = e.comprovantes.size,
            quebraNaCorrente = Operacoes.primeiraAutenticacaoQuebrada(e),
            anexos = total,
            anexosQueConferem = conferem,
            anexosAlterados = alterados,
            anexosFaltando = faltando,
            anexosSemImpressao = semImpressao,
            semComprovante = registrosSemComprovante(e, hoje),
            legados = legados(e),
        )
    }

    private fun antigo(e: Estado, instante: Long?): Boolean {
        val limite = e.legadoAte ?: return false
        return instante != null && instante <= limite
    }

    private fun legados(e: Estado): Int {
        if (e.legadoAte == null) return 0
        return e.lancamentos.count { it.tipo != TipoLancamento.SALDO_INICIAL && antigo(e, it.criadoEm) } +
            e.extratos.count { antigo(e, it.criadoEm) }
    }

    /** Instante do registro da aposta (o lançamento do valor). */
    private fun registroDaAposta(e: Estado, a: Aposta): Long? =
        e.lancamentos.firstOrNull { it.tipo == TipoLancamento.APOSTA && it.apostaId == a.id }?.criadoEm

    /** Cada aporte, aposta, apuração, rateio, extrato e divisão final precisa do seu comprovante. */
    fun registrosSemComprovante(e: Estado, hoje: LocalDate = LocalDate.now()): List<String> = buildList {
        fun quando(d: LocalDate) = Datas.quando(d, hoje)
        val porTipo = e.comprovantes.groupBy { it.tipo }
        val aportes = porTipo[TipoComprovante.APORTE].orEmpty().map { it.criadoEm }.toMutableList()
        e.lancamentos.filter { it.tipo == TipoLancamento.APORTE && !antigo(e, it.criadoEm) }.forEach { l ->
            if (!aportes.remove(l.criadoEm)) add("Aporte ${quando(l.data)}")
        }
        e.apostas.filter { it.registrada }.forEach { a ->
            if (!antigo(e, registroDaAposta(e, a)) && porTipo[TipoComprovante.APOSTA].orEmpty().none { it.apostaId == a.id }) {
                add("Aposta Nº${a.numero} feita ${quando(a.dataAposta)}")
            }
            val apurada = a.apuracao?.confirmadaEm
            if (apurada != null && !antigo(e, apurada) && porTipo[TipoComprovante.APURACAO].orEmpty().none { it.apostaId == a.id }) {
                add("Apuração da aposta Nº${a.numero} feita ${quando(a.dataAposta)}")
            }
        }
        e.lancamentos.filter { it.tipo == TipoLancamento.RATEIO && !antigo(e, it.criadoEm) }.forEach { l ->
            if (porTipo[TipoComprovante.RATEIO].orEmpty().none { it.rateioNumero == l.rateioNumero }) {
                add("Rateio ${quando(l.data)}")
            }
        }
        e.extratos.filter { !antigo(e, it.criadoEm) }.forEach { x ->
            if (porTipo[TipoComprovante.EXTRATO].orEmpty().none { it.mes == x.mes }) add("Extrato de ${x.mes}")
        }
        if (e.lancamentos.any { it.tipo == TipoLancamento.DIVISAO_FINAL } && porTipo[TipoComprovante.EXTINCAO].isNullOrEmpty()) {
            add("Divisão final do saldo")
        }
    }

    /** Só os algarismos e as letras de A a F do código (sem traços nem espaços), em maiúsculas. */
    fun normalizar(codigo: String): String = codigo.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }

    /** Já dá para procurar o código digitado (pelo menos os 8 algarismos)? */
    fun podeBuscar(codigo: String): Boolean = normalizar(codigo).length >= Autenticacao.DIGITOS

    /**
     * Comprovantes com o código digitado: os 8 algarismos impressos no comprovante (ex.: "4829-1736")
     * ou, nos comprovantes das versões até a 0.4, o começo do SHA-256 (pelo menos 8 caracteres).
     */
    fun buscar(e: Estado, codigo: String): List<Comprovante> {
        val alvo = normalizar(codigo)
        if (alvo.length < Autenticacao.DIGITOS) return emptyList()
        val curto = alvo.takeIf { it.length == Autenticacao.DIGITOS && it.all(Char::isDigit) }
        return e.comprovantes.filter { c ->
            (curto != null && Autenticacao.curto(c.autenticacao) == curto) || c.autenticacao.uppercase().startsWith(alvo)
        }
    }

    /** A autenticação deste comprovante confere com o registro e com os anteriores? */
    fun conferePosicao(e: Estado, c: Comprovante): Boolean {
        val i = e.comprovantes.indexOfFirst { it.id == c.id }
        if (i < 0) return false
        val quebra = Operacoes.primeiraAutenticacaoQuebrada(e)
        return quebra == null || quebra > i
    }
}

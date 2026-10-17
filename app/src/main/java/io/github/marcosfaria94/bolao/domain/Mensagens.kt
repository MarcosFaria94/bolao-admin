package io.github.marcosfaria94.bolao.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Modelos de mensagem do termo (Parte 4), reproduzidos caractere por caractere.
 * Os valores são alinhados à direita pela largura do maior valor do bloco,
 * exatamente como nos exemplos do termo.
 */
object Mensagens {

    private fun alinhar(vararg valores: Long): List<String> {
        val textos = valores.map { Dinheiro.formatar(it) }
        val largura = textos.maxOf { it.length }
        return textos.map { it.padStart(largura) }
    }

    /** Envolve o texto em bloco monoespaçado do WhatsApp. */
    fun monoespacado(texto: String): String = "```\n$texto\n```"

    /**
     * Identificação do jogo, ex.: "#132 010626 LF-3060 2M 030626".
     * Prêmio e data do sorteio ficam de fora se não forem conhecidos.
     */
    fun identificacao(
        numeroJogo: Int,
        dataAposta: LocalDate,
        modalidade: Modalidade,
        concurso: Int,
        premioEstimado: Long?,
        dataSorteio: LocalDate?,
    ): String = listOfNotNull(
        "#$numeroJogo",
        Datas.compacta(dataAposta),
        "${modalidade.sigla}-$concurso",
        premioEstimado?.takeIf { it > 0 }?.let { Dinheiro.abreviar(it) },
        dataSorteio?.let { Datas.compacta(it) },
    ).joinToString(" ")

    /** 0. Registro de aporte. */
    fun aporte(data: LocalDate, saldoLiquido: Long, aportes: Long, eventual: Boolean): String {
        val (a, b, c) = alinhar(saldoLiquido, aportes, saldoLiquido + aportes)
        val titulo = if (eventual) "APORTE EVENTUAL" else "APORTE"
        return "- $titulo ${Datas.compacta(data)} -\n" +
            "Saldo Líquido:     R$ $a\n" +
            "Aportes......:   + R$ $b\n" +
            "Saldo Final    =   R$ $c"
    }

    /** 1. Registro do Jogo (dias programados ou eventuais). */
    fun aposta(identificacao: String, eventual: Boolean, saldoLiquido: Long, valor: Long): String {
        val (a, b, c) = alinhar(saldoLiquido, valor, saldoLiquido - valor)
        val titulo = if (eventual) "APOSTA EVENTUAL" else "APOSTA"
        return "- $titulo $identificacao -\n" +
            "Saldo Líquido:   R$ $a\n" +
            "Aposta.......: - R$ $b\n" +
            "Saldo Final    = R$ $c"
    }

    /** 2. Apuração (dia seguinte ao sorteio). */
    fun apuracao(identificacao: String, acertos: String, saldoLiquido: Long, premio: Long): String {
        val (a, b, c) = alinhar(saldoLiquido, premio, saldoLiquido + premio)
        return "- APURAÇÃO $identificacao -\n" +
            "Contagem de acertos: $acertos\n" +
            "Saldo Líquido:   R$ $a\n" +
            "Prêmio.......: + R$ $b\n" +
            "Saldo Final    = R$ $c"
    }

    /** 3. Rateio. [total] = soma das 3 cotas. */
    fun rateio(numero: Int, data: LocalDate, saldoLiquido: Long, total: Long): String {
        val (a, b, c) = alinhar(saldoLiquido, total, saldoLiquido - total)
        return "- RATEIO Nº$numero ${Datas.compacta(data)} -\n" +
            "\n" +
            "Saldo Líquido:     R$ $a\n" +
            "Rateio.......:   - R$ $b\n" +
            "Saldo Final    =   R$ $c"
    }

    enum class Categoria { APORTE, PREMIO, APOSTA, RATEIO }

    /**
     * Uma linha de MOVIMENTAÇÕES do extrato.
     * [valor] positivo para entradas (aporte, prêmio) e negativo para saídas (aposta, rateio).
     * [descricao] já no formato do termo, ex.: "APORTE (Marcos)", "APOSTA #132 LF ", "RATEIO Nº1 (Caio)".
     */
    data class Movimento(
        val data: LocalDate,
        val categoria: Categoria,
        val descricao: String,
        val valor: Long,
    )

    fun descricaoAporte(apelido: String) = "APORTE ($apelido)"
    fun descricaoAposta(numero: Int, m: Modalidade) = "APOSTA #$numero ${m.sigla} "
    fun descricaoPremio(numero: Int, m: Modalidade) = "PRÊMIO #$numero ${m.sigla} "
    fun descricaoRateio(numero: Int, apelido: String) = "RATEIO Nº$numero ($apelido)"

    data class Resumo(
        val saldoAnterior: Long,
        val rendimento: Long,
        val aportes: Long,
        val premiacoes: Long,
        val apostas: Long,
        val rateio: Long,
    ) {
        val saldoFinal: Long get() = saldoAnterior + rendimento + aportes + premiacoes - apostas - rateio
    }

    fun resumo(saldoAnterior: Long, rendimento: Long, movimentos: List<Movimento>): Resumo = Resumo(
        saldoAnterior = saldoAnterior,
        rendimento = rendimento,
        aportes = movimentos.filter { it.categoria == Categoria.APORTE }.sumOf { it.valor },
        premiacoes = movimentos.filter { it.categoria == Categoria.PREMIO }.sumOf { it.valor },
        apostas = -movimentos.filter { it.categoria == Categoria.APOSTA }.sumOf { it.valor },
        rateio = -movimentos.filter { it.categoria == Categoria.RATEIO }.sumOf { it.valor },
    )

    /** 4. Prestação de Contas (último dia do mês). */
    fun extrato(mes: YearMonth, saldoAnterior: Long, rendimento: Long, movimentos: List<Movimento>): String {
        val r = resumo(saldoAnterior, rendimento, movimentos)

        // RESUMO: rótulo com pontos até 20 colunas, sinal na coluna 22, "R$" colado no valor.
        val valoresResumo = alinhar(
            r.saldoAnterior, kotlin.math.abs(r.rendimento), r.aportes, r.premiacoes, r.apostas, r.rateio, r.saldoFinal,
        )
        fun linhaResumo(rotulo: String, sinal: Char, valor: String) = rotulo.padEnd(20, '.') + ": " + sinal + " R$" + valor
        val resumo = listOf(
            linhaResumo("Saldo Líq. Anterior", ' ', valoresResumo[0]),
            linhaResumo("Rendimento Líquido", if (r.rendimento < 0) '-' else '+', valoresResumo[1]),
            linhaResumo("Aportes", '+', valoresResumo[2]),
            linhaResumo("Premiações", '+', valoresResumo[3]),
            linhaResumo("Apostas", '-', valoresResumo[4]),
            linhaResumo("Rateio", '-', valoresResumo[5]),
            "Saldo Final".padEnd(22) + "= R$" + valoresResumo[6],
        )

        // MOVIMENTAÇÕES: a linha 00 junta saldo anterior + rendimento no 1º dia do mês.
        data class Linha(val data: LocalDate, val descricao: String, val sinal: Char, val valor: Long)
        val linhas = mutableListOf(Linha(mes.atDay(1), "SALDO + RENDIMENTO", ' ', saldoAnterior + rendimento))
        for (m in movimentos) {
            val sinal = if (m.valor < 0) '-' else '+'
            linhas += Linha(m.data, m.descricao, sinal, kotlin.math.abs(m.valor))
        }
        val larguraSeq = maxOf(2, (linhas.size - 1).toString().length)
        val larguraDescricao = maxOf(21, linhas.maxOf { it.descricao.length })
        val valores = alinhar(*linhas.map { it.valor }.toLongArray())
        var dataAnterior: LocalDate? = null
        val movs = linhas.mapIndexed { i, l ->
            val colunaData = if (l.data == dataAnterior) " ".repeat(8) else Datas.curta(l.data)
            dataAnterior = l.data
            i.toString().padStart(larguraSeq, '0') + "  " + colunaData + " " +
                l.descricao.padEnd(larguraDescricao, '.') + ": " + l.sinal + " R$" + valores[i]
        }

        return "- EXTRATO ${Datas.mesAno(mes)} -\n" +
            "\n" +
            "RESUMO\n" +
            resumo.joinToString("\n") + "\n" +
            "\n" +
            "MOVIMENTAÇÕES\n" +
            movs.joinToString("\n")
    }
}

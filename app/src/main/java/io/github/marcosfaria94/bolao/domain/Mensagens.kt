package io.github.marcosfaria94.bolao.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Modelos do termo (Parte 4), reproduzidos caractere por caractere.
 *
 * Cada registro tem um título (o texto que vai no WhatsApp, ex.: "APURAÇÃO Nº 1 - 04/06/26")
 * e um bloco (o conteúdo do comprovante, que vira imagem). Os valores do bloco são alinhados
 * à direita pela largura do maior valor, exatamente como nos exemplos do termo.
 */
object Mensagens {

    private fun alinhar(vararg valores: Long): List<String> {
        val textos = valores.map { Dinheiro.formatar(it) }
        val largura = textos.maxOf { it.length }
        return textos.map { it.padStart(largura) }
    }

    // ---------- títulos (o texto enviado no WhatsApp) ----------

    /** "APORTE - 01/06/2026". */
    fun tituloAporte(data: LocalDate): String = "APORTE - ${Datas.longa(data)}"

    /** "APOSTA Nº 1 -  01/06/2026" (com dois espaços antes da data, como no termo). */
    fun tituloAposta(numero: Int, dataAposta: LocalDate): String = "APOSTA Nº $numero -  ${Datas.longa(dataAposta)}"

    /** "APURAÇÃO Nº 1 - 04/06/26": número da aposta e data da apuração. */
    fun tituloApuracao(numero: Int, dataApuracao: LocalDate): String = "APURAÇÃO Nº $numero - ${Datas.curta(dataApuracao)}"

    /** "RATEIO - 14/06/26". */
    fun tituloRateio(data: LocalDate): String = "RATEIO - ${Datas.curta(data)}"

    /** "EXTRATO - 30/06/26". */
    fun tituloExtrato(data: LocalDate): String = "EXTRATO - ${Datas.curta(data)}"

    /** Divisão final do saldo, na extinção do bolão (Parte 5). */
    fun tituloExtincao(data: LocalDate): String = "EXTINÇÃO - ${Datas.curta(data)}"

    // ---------- blocos ----------

    /**
     * Identificação da aposta, ex.: "Nº1 010626 LF-3060 2M 030626".
     * Prêmio e data do sorteio ficam de fora se não forem conhecidos.
     */
    fun identificacao(
        numero: Int,
        dataAposta: LocalDate,
        modalidade: Modalidade,
        concurso: Int,
        premioEstimado: Long?,
        dataSorteio: LocalDate?,
    ): String = listOfNotNull(
        "Nº$numero",
        Datas.compacta(dataAposta),
        "${modalidade.sigla}-$concurso",
        premioEstimado?.takeIf { it > 0 }?.let { Dinheiro.abreviar(it) },
        dataSorteio?.let { Datas.compacta(it) },
    ).joinToString(" ")

    /** 1. Registro de aporte. */
    fun aporte(data: LocalDate, saldoInicial: Long, aportes: Long): String {
        val (a, b, c) = alinhar(saldoInicial, aportes, saldoInicial + aportes)
        return "- APORTE ${Datas.compacta(data)} -\n" +
            "\n" +
            "Saldo Inicial:   R$ $a\n" +
            "Aportes......: + R$ $b\n" +
            "Saldo Final..: = R$ $c"
    }

    /** 2. Registro do Jogo. */
    fun aposta(identificacao: String, saldoInicial: Long, valor: Long): String {
        val (a, b, c) = alinhar(saldoInicial, valor, saldoInicial - valor)
        return "- APOSTA $identificacao -\n" +
            "\n" +
            "Saldo Inicial:   R$ $a\n" +
            "Aposta.......: - R$ $b\n" +
            "Saldo Final..: = R$ $c"
    }

    /** "(Aposta Premiada!)", como no termo, ou "(Aposta não premiada)". */
    fun situacaoApuracao(premiada: Boolean): String = if (premiada) "(Aposta Premiada!)" else "(Aposta não premiada)"

    /** 3. Apuração (dia seguinte ao sorteio). */
    fun apuracao(identificacao: String, acertos: String, premiada: Boolean, saldoInicial: Long, premio: Long): String {
        val (a, b, c) = alinhar(saldoInicial, premio, saldoInicial + premio)
        return "- APURAÇÃO $identificacao -\n" +
            "\n" +
            "Acertos: $acertos ${situacaoApuracao(premiada)}\n" +
            "Saldo Inicial:   R$ $a\n" +
            "Prêmio.......: + R$ $b\n" +
            "Saldo Final..: = R$ $c"
    }

    /** 4. Rateio. [total] = soma das 3 cotas. */
    fun rateio(data: LocalDate, saldoLiquido: Long, total: Long): String {
        val (a, b, c) = alinhar(saldoLiquido, total, saldoLiquido - total)
        return "- RATEIO ${Datas.compacta(data)} -\n" +
            "\n" +
            "Saldo Líquido:   R$ $a\n" +
            "Rateio.......: - R$ $b\n" +
            "Saldo Final    = R$ $c"
    }

    /**
     * Divisão final do saldo na extinção (Parte 5): cada um recebe o saldo ÷ 3 arredondado
     * para cima, e a diferença é coberta pelo Administrador.
     */
    fun extincao(data: LocalDate, saldoLiquido: Long, complemento: Long, divisao: Long): String {
        val (a, b, c, d) = alinhar(saldoLiquido, complemento, divisao, saldoLiquido + complemento - divisao)
        return "- EXTINÇÃO ${Datas.compacta(data)} -\n" +
            "\n" +
            "Saldo Líquido:   R$ $a\n" +
            "Complemento..: + R$ $b\n" +
            "Divisão......: - R$ $c\n" +
            "Saldo Final..: = R$ $d"
    }

    /** Título do documento de saldo (não é registro): "SALDO - 19/10/26". */
    fun tituloSaldo(data: LocalDate): String = "SALDO - ${Datas.curta(data)}"

    /**
     * Consulta de saldo, no estilo dos blocos do termo: Extra = Saldo Líquido − Provisionado.
     * Não é um registro (não entra no app nem na autenticação).
     */
    fun saldo(data: LocalDate, saldoLiquido: Long, provisionado: Long): String {
        val (a, b, c) = alinhar(saldoLiquido, provisionado, saldoLiquido - provisionado)
        return "- SALDO ${Datas.compacta(data)} -\n" +
            "\n" +
            "Saldo Líquido:   R$ $a\n" +
            "Provisionado.: - R$ $b\n" +
            "Extra........: = R$ $c"
    }

    // ---------- 5. Prestação de Contas (extrato) ----------

    enum class Categoria { APORTE, PREMIO, APOSTA, RATEIO }

    /**
     * Uma linha de MOVIMENTAÇÕES do extrato.
     * [valor] positivo para entradas (aporte, prêmio) e negativo para saídas (aposta, rateio).
     * [descricao] já no formato do termo, ex.: "APORTE ", "APOSTA Nº01 LF ", "RATEIO ".
     */
    data class Movimento(
        val data: LocalDate,
        val categoria: Categoria,
        val descricao: String,
        val valor: Long,
    )

    private fun dois(n: Int) = n.toString().padStart(2, '0')

    fun descricaoAporte() = "APORTE "
    fun descricaoAposta(numero: Int, m: Modalidade) = "APOSTA Nº${dois(numero)} ${m.sigla} "
    fun descricaoPremio(numero: Int, m: Modalidade) = "PRÊMIO Nº${dois(numero)} ${m.sigla} "
    fun descricaoRateio() = "RATEIO "

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

    /** 5. Prestação de Contas (último dia do mês). */
    fun extrato(mes: YearMonth, saldoAnterior: Long, rendimento: Long, movimentos: List<Movimento>): String {
        val r = resumo(saldoAnterior, rendimento, movimentos)

        // RESUMO: rótulo com pontos até 20 colunas, ": ", sinal, " R$" colado no valor.
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
            linhaResumo("Saldo Final", '=', valoresResumo[6]),
        )

        // MOVIMENTAÇÕES: a linha 00 traz o saldo líquido (anterior + rendimento) no 1º dia do mês.
        data class Linha(val data: LocalDate, val descricao: String, val sinal: Char, val valor: Long)
        val linhas = mutableListOf(Linha(mes.atDay(1), "SALDO LÍQUIDO", ' ', saldoAnterior + rendimento))
        for (m in movimentos) {
            val sinal = if (m.valor < 0) '-' else '+'
            linhas += Linha(m.data, m.descricao, sinal, kotlin.math.abs(m.valor))
        }
        val larguraSeq = maxOf(2, (linhas.size - 1).toString().length)
        val larguraDescricao = maxOf(22, linhas.maxOf { it.descricao.length })
        val valores = alinhar(*linhas.map { it.valor }.toLongArray())
        val movs = linhas.mapIndexed { i, l ->
            i.toString().padStart(larguraSeq, '0') + "  " + Datas.curta(l.data) + " " +
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

    // ---------- lembrete do aporte ----------

    /** Mensagem do botão "Enviar lembrete" (dia do aporte mensal). */
    fun lembreteAporte(grupo: String, valor: Long, chavePix: String): String =
        "🏆 $grupo - Lembrete Automático\n" +
            "💸 Valor: ${Dinheiro.rs(valor)}\n" +
            "🔑 Chave Pix: $chavePix\n" +
            "ℹ️ Obs.: Não é necessário enviar o comprovante."
}

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

    /** Colunas do comprovante, para quebrar as linhas longas (números da Lotomania...). */
    const val COLUNAS = 48

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

    // ---------- partes dos blocos ----------

    /**
     * "CONTABILIDADE DO GRUPO" do termo: saldo atual, a operação e o saldo final,
     * com o rótulo completado com pontos até 11 colunas ("APORTES....", "APOSTA.....").
     */
    private fun contabilidade(rotulo: String, sinal: Char, saldoAtual: Long, valor: Long, saldoFinal: Long): String {
        val (a, b, c) = alinhar(saldoAtual, valor, saldoFinal)
        return "SALDO ATUAL:   R$ $a\n" +
            rotulo.padEnd(11, '.') + ": $sinal R$ $b\n" +
            "SALDO FINAL: = R$ $c"
    }

    /** "Nº1/JUN": número da aposta no mês e o mês da aposta. */
    fun numeroNoMes(numero: Int, dataAposta: LocalDate): String = "Nº$numero/${Datas.mesCurto(dataAposta)}"

    /** "2 MILHÕES", "1,7 MILHÃO", "450 MIL" (prêmio principal do bloco da aposta). */
    fun premioPrincipal(centavos: Long?): String = when {
        centavos == null || centavos <= 0 -> "A CONFIRMAR"
        centavos < 100_000 -> Dinheiro.rs(centavos)
        else -> Dinheiro.porExtenso(centavos, comRs = false).uppercase()
    }

    /**
     * "RÓTULO: itens" quebrando em várias linhas de até [COLUNAS] colunas, com as linhas
     * seguintes alinhadas embaixo do primeiro item (os 50 números da Lotomania, por exemplo).
     */
    fun linhaQuebrada(rotulo: String, itens: List<String>): String {
        val inicio = "$rotulo: "
        val recuo = " ".repeat(inicio.length)
        val linhas = mutableListOf<StringBuilder>()
        var atual = StringBuilder(inicio)
        var vazia = true
        for (item in itens) {
            if (!vazia && atual.length + 1 + item.length > COLUNAS) {
                linhas += atual
                atual = StringBuilder(recuo)
                vazia = true
            }
            if (!vazia) atual.append(' ')
            atual.append(item)
            vazia = false
        }
        linhas += atual
        return linhas.joinToString("\n") { it.toString().trimEnd() }
    }

    /** O que foi apostado, no formato de cada modalidade (linhas "NÚMEROS: ..." do bloco). */
    data class Jogo(
        val numeros: List<Int> = emptyList(),
        val trevos: List<Int> = emptyList(),
        /** Time do Coração ou Mês de Sorte. */
        val extra: String? = null,
        val palpitesLoteca: List<String> = emptyList(),
        val bilheteFederal: String? = null,
        val fracoesFederal: Int? = null,
    )

    fun linhasDoJogo(m: Modalidade, j: Jogo): String = when (m.formato) {
        Formato.NUMEROS -> buildList {
            add(linhaQuebrada("NÚMEROS", j.numeros.sorted().map { m.rotulo(it) }.ifEmpty { listOf("...") }))
            if (m.trevos > 0) add(linhaQuebrada("TREVOS", j.trevos.sorted().map { it.toString() }.ifEmpty { listOf("...") }))
            when (m.extra) {
                Extra.TIME_DO_CORACAO -> add("TIME DO CORAÇÃO: ${j.extra?.uppercase() ?: "..."}")
                Extra.MES_DE_SORTE -> add("MÊS DE SORTE: ${j.extra?.uppercase() ?: "..."}")
                Extra.NENHUM -> Unit
            }
        }.joinToString("\n")
        // Super Sete: um número por coluna, da 1ª à 7ª.
        Formato.COLUNAS -> linhaQuebrada("NÚMEROS", j.numeros.map { if (it in 0..9) it.toString() else "-" }.ifEmpty { listOf("...") })
        Formato.LOTECA -> linhaQuebrada("PALPITES", j.palpitesLoteca.map { it.ifBlank { "-" } }.ifEmpty { listOf("...") })
        Formato.FEDERAL -> "BILHETE: ${j.bilheteFederal?.ifBlank { null } ?: "..."}\n" +
            "FRAÇÕES: ${j.fracoesFederal ?: 10} DE 10"
    }

    // ---------- blocos ----------

    /** 1. Registro de aporte. [adicional] = aporte fora do mensal (com a concordância dos 3). */
    fun aporte(data: LocalDate, saldoAtual: Long, aportes: Long, adicional: Boolean = false): String =
        "TIPO DE APORTE: ${if (adicional) "ADICIONAL" else "OBRIGATÓRIO"}\n" +
            "DATA: ${Datas.longa(data)}\n" +
            "\n" +
            "CONTABILIDADE DO GRUPO\n" +
            contabilidade("APORTES", '+', saldoAtual, aportes, saldoAtual + aportes)

    /** 2. Registro do Jogo (dias programados ou eventuais). */
    fun aposta(
        numero: Int,
        dataAposta: LocalDate,
        modalidade: Modalidade,
        concurso: Int,
        premioEstimado: Long?,
        dataSorteio: LocalDate?,
        jogo: Jogo,
        saldoAtual: Long,
        valor: Long,
    ): String =
        "APOSTA: ${numeroNoMes(numero, dataAposta)}\n" +
            "DATA: ${Datas.longa(dataAposta)}\n" +
            "MODALIDADE: ${modalidade.nome.uppercase()}\n" +
            "CONCURSO: ${concurso.takeIf { it > 0 } ?: "A CONFIRMAR"}\n" +
            "PRÊMIO PRINCIPAL: ${premioPrincipal(premioEstimado)}\n" +
            "SORTEIO: ${dataSorteio?.let { Datas.longa(it) } ?: "A CONFIRMAR"}\n" +
            linhasDoJogo(modalidade, jogo) + "\n" +
            "\n" +
            "CONTABILIDADE DO GRUPO\n" +
            contabilidade("APOSTA", '-', saldoAtual, valor, saldoAtual - valor)

    /** 3. Apuração (dia seguinte ao sorteio): identifica a aposta e soma o prêmio. */
    fun apuracao(numero: Int, dataAposta: LocalDate, saldoAtual: Long, premio: Long): String =
        "APOSTA: ${numeroNoMes(numero, dataAposta)}\n" +
            "DATA: ${Datas.longa(dataAposta)}\n" +
            "\n" +
            "CONTABILIDADE DO GRUPO\n" +
            contabilidade("PRÊMIO", '+', saldoAtual, premio, saldoAtual + premio)

    /** 4. Rateio. [total] = soma das 3 cotas. */
    fun rateio(data: LocalDate, saldoAtual: Long, total: Long): String =
        "- RATEIO ${Datas.compacta(data)} -\n" +
            "\n" +
            contabilidade("RATEIO", '-', saldoAtual, total, saldoAtual - total)

    /**
     * Divisão final do saldo na extinção (Parte 5): cada um recebe o saldo ÷ 3 arredondado
     * para cima, e a diferença é coberta pelo Administrador.
     */
    fun extincao(data: LocalDate, saldoAtual: Long, complemento: Long, divisao: Long): String {
        val (a, b, c, d) = alinhar(saldoAtual, complemento, divisao, saldoAtual + complemento - divisao)
        return "- EXTINÇÃO ${Datas.compacta(data)} -\n" +
            "\n" +
            "SALDO ATUAL:   R$ $a\n" +
            "COMPLEMENTO: + R$ $b\n" +
            "DIVISÃO....: - R$ $c\n" +
            "SALDO FINAL: = R$ $d"
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
            "SALDO LÍQUIDO:   R$ $a\n" +
            "PROVISIONADO.: - R$ $b\n" +
            "EXTRA........: = R$ $c"
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
            linhaResumo("SALDO LÍQ. ANTERIOR", ' ', valoresResumo[0]),
            linhaResumo("RENDIMENTO LÍQUIDO", if (r.rendimento < 0) '-' else '+', valoresResumo[1]),
            linhaResumo("APORTES", '+', valoresResumo[2]),
            linhaResumo("PREMIAÇÕES", '+', valoresResumo[3]),
            linhaResumo("APOSTAS", '-', valoresResumo[4]),
            linhaResumo("RATEIO", '-', valoresResumo[5]),
            linhaResumo("SALDO FINAL", '=', valoresResumo[6]),
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

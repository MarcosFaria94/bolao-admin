package io.github.marcosfaria94.bolao.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Chance de acertar cada faixa com a aposta simples. */
object Probabilidades {

    /** [umEm] = "1 em X" (arredondado). */
    data class Faixa(val nome: String, val umEm: Long, val observacao: String? = null)

    private fun c(n: Int, k: Int): BigInteger {
        if (k < 0 || k > n) return BigInteger.ZERO
        var r = BigInteger.ONE
        for (i in 0 until k) r = r.multiply(BigInteger.valueOf((n - i).toLong())).divide(BigInteger.valueOf((i + 1).toLong()))
        return r
    }

    private fun umEm(casos: BigInteger, total: BigInteger): Long =
        BigDecimal(total).divide(BigDecimal(casos), 0, RoundingMode.HALF_UP).toLong()

    /** Casos favoráveis de acertar exatamente [k] (de [d] sorteados) apostando [a] de [n] números. */
    private fun hiper(n: Int, a: Int, d: Int, k: Int): BigInteger = c(a, k).multiply(c(n - a, d - k))

    private fun faixaNumeros(nome: String, n: Int, a: Int, d: Int, k: Int) = Faixa(nome, umEm(hiper(n, a, d, k), c(n, d)))

    fun faixas(m: Modalidade): List<Faixa> = when (m) {
        Modalidade.MEGA_SENA -> listOf(
            faixaNumeros("Sena (6 acertos)", 60, 6, 6, 6),
            faixaNumeros("Quina (5 acertos)", 60, 6, 6, 5),
            faixaNumeros("Quadra (4 acertos)", 60, 6, 6, 4),
        )
        Modalidade.LOTOFACIL -> (15 downTo 11).map { faixaNumeros("$it acertos", 25, 15, 15, it) }
        Modalidade.QUINA -> listOf(
            faixaNumeros("Quina (5 acertos)", 80, 5, 5, 5),
            faixaNumeros("Quadra (4 acertos)", 80, 5, 5, 4),
            faixaNumeros("Terno (3 acertos)", 80, 5, 5, 3),
            faixaNumeros("Duque (2 acertos)", 80, 5, 5, 2),
        )
        Modalidade.LOTOMANIA -> (20 downTo 15).map { faixaNumeros("$it acertos", 100, 50, 20, it) } +
            faixaNumeros("0 acertos", 100, 50, 20, 0)
        Modalidade.TIMEMANIA -> (7 downTo 3).map { faixaNumeros("$it acertos", 80, 10, 7, it) } +
            Faixa("Time do Coração", 80)
        Modalidade.DUPLA_SENA -> listOf(
            faixaNumeros("Sena (6 acertos)", 50, 6, 6, 6),
            faixaNumeros("Quina (5 acertos)", 50, 6, 6, 5),
            faixaNumeros("Quadra (4 acertos)", 50, 6, 6, 4),
            faixaNumeros("Terno (3 acertos)", 50, 6, 6, 3),
        ).map { it.copy(observacao = "em cada um dos 2 sorteios") }
        Modalidade.DIA_DE_SORTE -> (7 downTo 4).map { faixaNumeros("$it acertos", 31, 7, 7, it) } +
            Faixa("Mês de Sorte", 12)
        Modalidade.SUPER_SETE -> (7 downTo 3).map { k ->
            // Cada coluna é independente: 1 chance em 10.
            val casos = c(7, k).multiply(BigInteger.valueOf(9).pow(7 - k))
            Faixa("$k colunas", umEm(casos, BigInteger.TEN.pow(7)))
        }
        Modalidade.MAIS_MILIONARIA -> {
            val totalNumeros = c(50, 6)
            val totalTrevos = c(6, 2)
            fun casos(n: Int, trevos: List<Int>): BigInteger {
                val nums = hiper(50, 6, 6, n)
                val trv = trevos.fold(BigInteger.ZERO) { s, t -> s.add(hiper(6, 2, 2, t)) }
                return nums.multiply(trv)
            }
            val total = totalNumeros.multiply(totalTrevos)
            listOf(
                Faixa("6 números + 2 trevos", umEm(casos(6, listOf(2)), total)),
                Faixa("6 números + 1 trevo ou nenhum", umEm(casos(6, listOf(1, 0)), total)),
                Faixa("5 números + 2 trevos", umEm(casos(5, listOf(2)), total)),
                Faixa("5 números + 1 trevo ou nenhum", umEm(casos(5, listOf(1, 0)), total)),
                Faixa("4 números + 2 trevos", umEm(casos(4, listOf(2)), total)),
                Faixa("4 números + 1 trevo ou nenhum", umEm(casos(4, listOf(1, 0)), total)),
                Faixa("3 números + 2 trevos", umEm(casos(3, listOf(2)), total)),
                Faixa("3 números + 1 trevo", umEm(casos(3, listOf(1)), total)),
                Faixa("2 números + 2 trevos", umEm(casos(2, listOf(2)), total)),
                Faixa("2 números + 1 trevo", umEm(casos(2, listOf(1)), total)),
            )
        }
        Modalidade.LOTECA -> {
            // Supondo os 3 resultados igualmente prováveis em cada jogo.
            val total = BigInteger.valueOf(3).pow(14)
            listOf(
                Faixa("14 acertos", umEm(BigInteger.ONE, total)),
                Faixa("13 acertos", umEm(BigInteger.valueOf(14L * 2), total)),
            ).map { it.copy(observacao = "supondo os 3 resultados igualmente prováveis") }
        }
        Modalidade.FEDERAL -> listOf(
            Faixa("1º prêmio (bilhete inteiro)", 100_000),
            Faixa("Qualquer um dos 5 prêmios", 20_000),
        )
    }

    // ---------- tabelas dos Detalhes (desde a 0.27) ----------

    /**
     * Uma linha da tabela de probabilidades, como a do site da Caixa: quantos números (e trevos) foram
     * jogados, quantas apostas simples cabem neles (o valor da aposta é o preço da simples vezes isso) e a
     * chance de cada faixa ("1 em X"; null = não dá para acertar essa faixa com essa aposta).
     */
    data class LinhaDaTabela(val numeros: Int, val trevos: Int, val apostasSimples: Long, val umEm: List<Long?>)

    /** Os nomes das colunas das faixas (na ordem de [tabela]). */
    fun colunas(m: Modalidade): List<String> = when (m) {
        Modalidade.MEGA_SENA -> listOf("Sena", "Quina", "Quadra")
        Modalidade.LOTOFACIL -> (15 downTo 11).map { "$it acertos" }
        Modalidade.QUINA -> listOf("Quina", "Quadra", "Terno", "Duque")
        Modalidade.LOTOMANIA -> (20 downTo 15).map { "$it acertos" } + "0 acertos"
        Modalidade.TIMEMANIA -> (7 downTo 3).map { "$it acertos" } + "Time do Coração"
        Modalidade.DUPLA_SENA -> listOf("Sena", "Quina", "Quadra", "Terno")
        Modalidade.DIA_DE_SORTE -> (7 downTo 4).map { "$it acertos" } + "Mês de Sorte"
        Modalidade.SUPER_SETE -> (7 downTo 3).map { "$it colunas" }
        // Desde a 0.32, "6 + 0/1" (até a 0.31, "6 + 1 ou 0"): números acertados + trevos acertados.
        Modalidade.MAIS_MILIONARIA -> FAIXAS_DA_MAIS_MILIONARIA
        Modalidade.LOTECA -> listOf("14 acertos", "13 acertos")
        Modalidade.FEDERAL -> listOf("1º prêmio", "Qualquer prêmio")
    }

    /** Quantos acertos cada coluna das modalidades de números pede (sem as colunas do time e do mês). */
    private fun acertosDasColunas(m: Modalidade): List<Int> = when (m) {
        Modalidade.MEGA_SENA -> listOf(6, 5, 4)
        Modalidade.LOTOFACIL -> (15 downTo 11).toList()
        Modalidade.QUINA -> listOf(5, 4, 3, 2)
        Modalidade.LOTOMANIA -> (20 downTo 15).toList() + 0
        Modalidade.TIMEMANIA -> (7 downTo 3).toList()
        Modalidade.DUPLA_SENA -> listOf(6, 5, 4, 3)
        Modalidade.DIA_DE_SORTE -> (7 downTo 4).toList()
        else -> emptyList()
    }

    /** "1 em X" de acertar exatamente [k] dos números sorteados jogando [a] números ([casos] = 0: não dá). */
    private fun umEmOuNada(casos: BigInteger, total: BigInteger): Long? = if (casos.signum() == 0) null else umEm(casos, total)

    /**
     * A tabela de probabilidades da modalidade (desde a 0.27, nos Detalhes, como a da Caixa). Com as apostas
     * com mais números ([mais], o parâmetro), uma linha para cada quantidade de números que o volante aceita
     * (na +Milionária, também de trevos); sem ele, só a da aposta simples.
     */
    fun tabela(m: Modalidade, mais: Boolean): List<LinhaDaTabela> {
        val comMais = mais && Volante.aceitaMais(m)
        val numeros = if (comMais) m.qtdAposta..Volante.maximo(m) else m.qtdAposta..m.qtdAposta
        val trevos = if (comMais && m.trevos > 0) m.trevos..Volante.maximoTrevos(m) else m.trevos..m.trevos
        return when {
            m == Modalidade.MAIS_MILIONARIA -> numeros.flatMap { a -> trevos.map { b -> linhaMaisMilionaria(a, b) } }
            m.formato == Formato.NUMEROS -> numeros.map { a ->
                val n = m.maior - m.menor + 1
                val total = c(n, m.qtdSorteada)
                val dosNumeros = acertosDasColunas(m).map { k -> umEmOuNada(hiper(n, a, m.qtdSorteada, k), total) }
                val extra = when (m.extra) {
                    Extra.TIME_DO_CORACAO -> listOf<Long?>(80)
                    Extra.MES_DE_SORTE -> listOf<Long?>(12)
                    Extra.NENHUM -> emptyList()
                }
                LinhaDaTabela(a, m.trevos, Volante.apostasSimples(m, a, m.trevos), dosNumeros + extra)
            }
            // Super Sete, Loteca e Federal: só a aposta simples.
            else -> listOf(LinhaDaTabela(m.qtdAposta, 0, 1, faixas(m).map { it.umEm }))
        }
    }

    /** A +Milionária com [a] números e [b] trevos: cada faixa é números acertados e trevos acertados. */
    private fun linhaMaisMilionaria(a: Int, b: Int): LinhaDaTabela {
        val total = c(50, 6).multiply(c(6, 2))
        fun casos(n: Int, ts: List<Int>): BigInteger = hiper(50, a, 6, n).multiply(ts.fold(BigInteger.ZERO) { s, t -> s.add(hiper(6, b, 2, t)) })
        val faixas = listOf(
            6 to listOf(2), 6 to listOf(1, 0), 5 to listOf(2), 5 to listOf(1, 0), 4 to listOf(2), 4 to listOf(1, 0),
            3 to listOf(2), 3 to listOf(1), 2 to listOf(2), 2 to listOf(1),
        )
        return LinhaDaTabela(a, b, Volante.apostasSimples(Modalidade.MAIS_MILIONARIA, a, b), faixas.map { (n, ts) -> umEmOuNada(casos(n, ts), total) })
    }

    /**
     * Os nomes das faixas da +Milionária (desde a 0.32, pedido do usuário): os números acertados + os trevos
     * acertados, com "0/1" para "1 trevo ou nenhum" ("6 + 2", "6 + 0/1", "4 + 2"...). Na ordem das faixas da Caixa.
     */
    val FAIXAS_DA_MAIS_MILIONARIA: List<String> =
        listOf("6 + 2", "6 + 0/1", "5 + 2", "5 + 0/1", "4 + 2", "4 + 0/1", "3 + 2", "3 + 1", "2 + 2", "2 + 1")

    /** A legenda da nomenclatura da +Milionária (no fim da premiação e das tabelas, desde a 0.32). */
    const val LEGENDA_DA_MAIS_MILIONARIA =
        "6 + 2 = 6 números e 2 trevos acertados; 6 + 0/1 = 6 números e 1 trevo ou nenhum; 3 + 1 = 3 números e 1 trevo."

    /**
     * A legenda do traço das tabelas de probabilidades (desde a 0.32): a aposta não fica naquela faixa. Na
     * +Milionária, com os 6 trevos marcados os 2 trevos sorteados estão sempre na aposta.
     */
    fun legendaDoTraco(m: Modalidade): String =
        if (m == Modalidade.MAIS_MILIONARIA) {
            "— = essa aposta não fica nessa faixa: com os 6 trevos marcados, os 2 trevos sorteados estão sempre entre os dela " +
                "(ela acerta os 2 trevos), então as faixas com 1 trevo ou nenhum não acontecem."
        } else {
            "— = essa aposta não fica nessa faixa (não dá para acertar só esse tanto de números com ela)."
        }

    /** As observações da tabela de probabilidades (embaixo dela). Desde a 0.32 a legenda da +Milionária é a nova. */
    fun observacoesDaTabela(m: Modalidade): List<String> = faixas(m).mapNotNull { it.observacao }.distinct() +
        if (m == Modalidade.MAIS_MILIONARIA) listOf(LEGENDA_DA_MAIS_MILIONARIA) else emptyList()

    /** A tabela tem algum traço (alguma faixa que a aposta não alcança): a legenda do traço vai embaixo. */
    fun temTraco(linhas: List<LinhaDaTabela>): Boolean = linhas.any { l -> l.umEm.any { it == null } }

    /**
     * Um grupo da tabela de quantidade de prêmios: acertando [acertando] dos números sorteados, quantos prêmios
     * de cada faixa ([faixas], em acertos) a aposta recebe.
     */
    data class GrupoDePremios(val acertando: Int, val faixas: List<Int>)

    /** A menor faixa de cada modalidade que tem a tabela de quantidade de prêmios (as de números, com mais números). */
    private fun menorFaixa(m: Modalidade): Int? = when (m) {
        Modalidade.MEGA_SENA -> 4
        Modalidade.LOTOFACIL -> 11
        Modalidade.QUINA -> 2
        Modalidade.DUPLA_SENA -> 3
        Modalidade.DIA_DE_SORTE -> 4
        else -> null
    }

    /**
     * Os grupos da tabela "Quantidade de prêmios a receber acertando" (como a da Caixa; desde a 0.27): só nas
     * modalidades que aceitam mais números (sem a +Milionária, em que os trevos entram na conta). null = não há.
     */
    fun gruposDePremios(m: Modalidade): List<GrupoDePremios>? {
        val menor = menorFaixa(m) ?: return null
        if (!Volante.aceitaMais(m)) return null
        return (m.qtdSorteada downTo menor).map { h -> GrupoDePremios(h, (h downTo menor).toList()) }
    }

    /** O nome de cada faixa na tabela de quantidade de prêmios: "Sena", "Quina"... ou "11 acertos". */
    fun nomeDaFaixa(m: Modalidade, acertos: Int): String = when {
        m == Modalidade.MEGA_SENA || m == Modalidade.DUPLA_SENA -> mapOf(6 to "Sena", 5 to "Quina", 4 to "Quadra", 3 to "Terno")[acertos]
        m == Modalidade.QUINA -> mapOf(5 to "Quina", 4 to "Quadra", 3 to "Terno", 2 to "Duque")[acertos]
        else -> null
    } ?: "$acertos acertos"

    /**
     * Quantos prêmios de cada faixa uma aposta com [numeros] números recebe, em cada grupo de [gruposDePremios]:
     * acertando h números, as apostas simples com t acertos são C(h, t) × C(números − h, simples − t).
     */
    fun quantidadeDePremios(m: Modalidade, numeros: Int): List<List<Long>> =
        gruposDePremios(m).orEmpty().map { g ->
            g.faixas.map { t -> Volante.combinacoes(g.acertando, t) * Volante.combinacoes(numeros - g.acertando, m.qtdAposta - t) }
        }

    /** "50.063.860": o número com o ponto dos milhares (as tabelas já dizem "1 em"). */
    fun milhares(v: Long): String {
        val s = v.toString()
        val sb = StringBuilder()
        for ((i, ch) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(ch)
        }
        return sb.toString()
    }

    /** "1 em 50.063.860". */
    fun texto(umEm: Long): String {
        val s = umEm.toString()
        val sb = StringBuilder()
        for ((i, ch) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(ch)
        }
        return "1 em $sb"
    }
}

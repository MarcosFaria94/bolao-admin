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
                Faixa("6 números + 1 ou nenhum trevo", umEm(casos(6, listOf(1, 0)), total)),
                Faixa("5 números + 2 trevos", umEm(casos(5, listOf(2)), total)),
                Faixa("5 números + 1 ou nenhum trevo", umEm(casos(5, listOf(1, 0)), total)),
                Faixa("4 números + 2 trevos", umEm(casos(4, listOf(2)), total)),
                Faixa("4 números + 1 ou nenhum trevo", umEm(casos(4, listOf(1, 0)), total)),
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

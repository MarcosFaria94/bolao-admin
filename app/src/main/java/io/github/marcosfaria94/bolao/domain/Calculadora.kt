package io.github.marcosfaria94.bolao.domain

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.expm1
import kotlin.math.ln1p
import kotlin.math.roundToLong

/**
 * A calculadora de probabilidades (desde a 0.38, pedido do usuário; o ícone ao lado das Estatísticas, nos Detalhes): um
 * controle deslizante para as apostas simples e um para cada tipo de aposta com mais números que a pessoa adiciona; o
 * painel mostra a chance de cada faixa (de acertar pelo menos uma vez com todas essas apostas no mesmo concurso) e a de
 * ganhar algum prêmio. Cada modalidade tem a sua configuração ([ConfigDaCalculadora]), guardada sozinha.
 *
 * A conta considera apostas diferentes umas das outras, como se escolhidas ao acaso: a chance de uma faixa com todas
 * elas é 1 − Π (1 − p)ⁿ (p = a chance de uma aposta daquele tipo, n = quantas). Na faixa principal, com apostas
 * diferentes, isso é praticamente a soma das chances. Na Dupla Sena, cada aposta concorre nos 2 sorteios.
 */
object Calculadora {

    /** As amplitudes que a engrenagem oferece (o máximo dos controles deslizantes). */
    val AMPLITUDES = listOf(10, 20, 50, 100, 200, 500, 1000)

    /** A amplitude de quem nunca mexeu na engrenagem. */
    const val AMPLITUDE_PADRAO = 20

    /** O índice da linha "Qualquer prêmio" na escolha das faixas do painel ([ConfigDaCalculadora.faixas]). */
    const val QUALQUER_PREMIO = -1

    const val NOME_QUALQUER = "Qualquer prêmio"

    /** A calculadora existe em todas as modalidades menos a Federal (bilhetes, sem volante). */
    fun tem(m: Modalidade): Boolean = m != Modalidade.FEDERAL

    /** Os tipos de aposta da modalidade (números e trevos), da tabela de probabilidades com mais números. */
    fun linhas(m: Modalidade): List<Probabilidades.LinhaDaTabela> = Probabilidades.tabela(m, mais = true)

    /** A linha da aposta simples. */
    fun simples(m: Modalidade): Probabilidades.LinhaDaTabela = linhas(m).first()

    /** Os tipos de aposta com mais números (o que o "Adicionar" oferece); vazio = a modalidade só tem a simples. */
    fun comMaisNumeros(m: Modalidade): List<Probabilidades.LinhaDaTabela> = linhas(m).drop(1)

    /** A linha da tabela com [numeros] e [trevos]. */
    fun linha(m: Modalidade, numeros: Int, trevos: Int): Probabilidades.LinhaDaTabela? =
        linhas(m).firstOrNull { it.numeros == numeros && it.trevos == trevos }

    /** Os nomes das faixas do painel (as colunas da tabela de probabilidades). */
    fun faixas(m: Modalidade): List<String> = Probabilidades.colunas(m)

    /** A coluna do time ou do mês (independente dos números) é a última. */
    private fun temExtra(m: Modalidade): Boolean = m.extra != Extra.NENHUM

    /** Uma linha do painel: a faixa ([indice]; [QUALQUER_PREMIO] = algum prêmio), o nome e a chance (0 a 1). */
    data class Chance(val indice: Int, val nome: String, val p: Double)

    /** Os tipos de aposta da configuração, com quantas apostas de cada (as que têm zero ficam de fora). */
    fun apostas(m: Modalidade, c: ConfigDaCalculadora): List<Pair<Probabilidades.LinhaDaTabela, Int>> {
        val lista = mutableListOf(simples(m) to c.simples)
        c.extras.forEach { e -> linha(m, e.numeros, e.trevos)?.let { lista += it to e.apostas } }
        return lista.filter { it.second > 0 }
    }

    /** As chances do painel (todas as faixas e, no fim, "Qualquer prêmio"). */
    fun chances(m: Modalidade, c: ConfigDaCalculadora): List<Chance> {
        val nomes = faixas(m)
        val grupo = apostas(m, c)
        val sorteios = if (m == Modalidade.DUPLA_SENA) 2 else 1
        // Soma dos log(1 − p) de cada faixa e do "algum prêmio".
        val logs = DoubleArray(nomes.size)
        var logQualquer = 0.0
        for ((l, n) in grupo) {
            val ps = nomes.indices.map { j -> l.chances.getOrNull(j) ?: 0.0 }
            for (j in nomes.indices) logs[j] += n * sorteios * ln1p(-ps[j].coerceIn(0.0, 1.0))
            // Os números: as faixas são de acertos exatos (uma exclui a outra); o time e o mês, à parte.
            val numeros = if (temExtra(m)) ps.dropLast(1) else ps
            val extra = if (temExtra(m)) ps.last() else 0.0
            val semNumeros = ln1p(-numeros.sum().coerceIn(0.0, 1.0)) * sorteios
            logQualquer += n * (semNumeros + ln1p(-extra.coerceIn(0.0, 1.0)))
        }
        return nomes.mapIndexed { j, nome -> Chance(j, nome, -expm1(logs[j])) } + Chance(QUALQUER_PREMIO, NOME_QUALQUER, -expm1(logQualquer))
    }

    /** As linhas do painel que a configuração mostra (todas, ou as escolhidas na engrenagem). */
    fun doPainel(m: Modalidade, c: ConfigDaCalculadora): List<Chance> {
        val todas = chances(m, c)
        val escolhidas = c.faixas ?: return todas
        return todas.filter { it.indice in escolhidas }
    }

    /** Quantas apostas simples tudo isso vale (o preço é esse tanto vezes o preço da simples). */
    fun apostasSimples(m: Modalidade, c: ConfigDaCalculadora): Long = apostas(m, c).sumOf { (l, n) -> l.apostasSimples * n }

    /** Quantas apostas ao todo. */
    fun total(m: Modalidade, c: ConfigDaCalculadora): Int = apostas(m, c).sumOf { it.second }

    /** "1 em 50.063.860" (a chance [p] arredondada); "Certeza" com 100%; "—" sem chance. */
    fun umEm(p: Double): String = when {
        p <= 0.0 -> "—"
        p >= 0.999_999_5 -> "Certeza"
        else -> Probabilidades.texto((1.0 / p).roundToLong().coerceAtLeast(1))
    }

    /** "12,3%", "0,0021%", "< 0,0001%" (a porcentagem com 2 algarismos que contam). */
    fun porcentagem(p: Double): String {
        val v = p * 100
        if (v <= 0.0) return "0%"
        if (v < 0.0001) return "< 0,0001%"
        if (v >= 99.995) return "100%"
        val casas = when {
            v >= 10 -> 1
            v >= 1 -> 2
            else -> (2 - kotlin.math.floor(kotlin.math.log10(v)).toInt()).coerceAtMost(6)
        }
        return BigDecimal(v).setScale(casas, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',') + "%"
    }

    /** O nome de um tipo de aposta: "Apostas simples (6 números)", "Apostas de 8 números", "Apostas de 7 números e 3 trevos". */
    fun nomeDaLinha(m: Modalidade, numeros: Int, trevos: Int, simples: Boolean = false): String {
        val nums = when (m.formato) {
            Formato.COLUNAS -> "7 colunas"
            Formato.LOTECA -> "14 jogos"
            else -> "$numeros números"
        }
        val trv = if (m.trevos > 0) " e $trevos trevos" else ""
        return if (simples) "Apostas simples ($nums$trv)" else "Apostas de $nums$trv"
    }

    /** "1 aposta", "3 apostas". */
    fun quantas(n: Int): String = if (n == 1) "1 aposta" else "$n apostas"

    /** A configuração com mais [apostas] do tipo [numeros]/[trevos] (somando ao que já havia; a simples vai no controle dela). */
    fun adicionar(m: Modalidade, c: ConfigDaCalculadora, numeros: Int, trevos: Int, apostas: Int): ConfigDaCalculadora {
        val s = simples(m)
        if (numeros == s.numeros && trevos == s.trevos) return c.copy(simples = c.simples + apostas)
        val existente = c.extras.indexOfFirst { it.numeros == numeros && it.trevos == trevos }
        val extras = if (existente >= 0) {
            c.extras.mapIndexed { i, e -> if (i == existente) e.copy(apostas = e.apostas + apostas) else e }
        } else {
            (c.extras + LinhaDaCalculadora(numeros, trevos, apostas)).sortedWith(compareBy({ it.numeros }, { it.trevos }))
        }
        // A amplitude cresce, se for preciso, para o controle caber a quantidade.
        val maior = (extras.map { it.apostas } + c.simples).max()
        val amplitude = AMPLITUDES.firstOrNull { it >= maior }?.coerceAtLeast(c.maximo) ?: AMPLITUDES.last()
        return c.copy(extras = extras, maximo = amplitude)
    }
}

/** Um controle deslizante a mais da calculadora (desde a 0.38): apostas com [numeros] números (e [trevos]), [apostas] delas. */
@Serializable
data class LinhaDaCalculadora(val numeros: Int, val trevos: Int = 0, val apostas: Int = 1)

/**
 * A configuração da calculadora de uma modalidade (desde a 0.38): quantas apostas simples, os controles das apostas com
 * mais números, a amplitude dos controles ([maximo]) e as faixas do painel ([faixas]; null = todas, com o "Qualquer
 * prêmio" = [Calculadora.QUALQUER_PREMIO]). O "Redefinir" volta a esta configuração padrão.
 */
@Serializable
data class ConfigDaCalculadora(
    val simples: Int = 1,
    val extras: List<LinhaDaCalculadora> = emptyList(),
    val maximo: Int = Calculadora.AMPLITUDE_PADRAO,
    val faixas: List<Int>? = null,
)

package io.github.marcosfaria94.bolao.domain

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.expm1
import kotlin.math.ln1p
import kotlin.math.roundToLong

/**
 * A calculadora de probabilidades (desde a 0.38, pedido do usuário; o ícone ao lado das Estatísticas, nos Detalhes): um
 * controle deslizante para as apostas simples e (desde a 0.40, todos na tela, começando em 0) um para cada tipo de aposta
 * com mais números; o painel mostra a chance de cada faixa (de acertar pelo menos uma vez com todas essas apostas no mesmo
 * concurso) e, à parte, a de ganhar algum prêmio. Cada modalidade tem a sua configuração ([ConfigDaCalculadora]), guardada
 * sozinha.
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

    /**
     * "1 em 50.063.860" (a chance [p] arredondada); "—" sem chance. Desde a 0.39 sem o "Certeza": as apostas são contadas
     * como diferentes e feitas ao acaso, e assim a chance nunca chega a 100% (perto disso, "1 em 1").
     */
    fun umEm(p: Double): String = when {
        p <= 0.0 -> "—"
        else -> Probabilidades.texto((1.0 / p).roundToLong().coerceAtLeast(1))
    }

    /**
     * "~12,3%", "~0,0021%", "< 0,0001%" (a porcentagem com 2 algarismos que contam). Desde a 0.39 (pedido do usuário) com o
     * "~" na frente: é um valor arredondado, e "100%" sem ele parecia certeza ("~100%" é quase certo, não certo).
     */
    fun porcentagem(p: Double): String {
        val v = p * 100
        if (v <= 0.0) return "0%"
        if (v < 0.0001) return "< 0,0001%"
        if (v >= 99.995) return "${APROXIMADO}100%"
        val casas = when {
            v >= 10 -> 1
            v >= 1 -> 2
            else -> (2 - kotlin.math.floor(kotlin.math.log10(v)).toInt()).coerceAtMost(6)
        }
        return APROXIMADO + BigDecimal(v).setScale(casas, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',') + "%"
    }

    /** O sinal das porcentagens arredondadas (desde a 0.39). */
    const val APROXIMADO = "~"

    /**
     * O nome de um tipo de aposta: "6 números", "8 números", "7 números e 3 trevos", "7 colunas" (desde a 0.40, pedido do
     * usuário; era "Apostas simples (6 números)" e "Apostas de 8 números").
     */
    fun nomeDaLinha(m: Modalidade, numeros: Int, trevos: Int): String {
        val nums = when (m.formato) {
            Formato.COLUNAS -> "7 colunas"
            Formato.LOTECA -> "14 jogos"
            else -> "$numeros números"
        }
        val trv = if (m.trevos > 0) " e $trevos trevos" else ""
        return "$nums$trv"
    }

    /** O título dos controles das apostas com mais números (desde a 0.40, todos na tela). */
    fun tituloDosMaisNumeros(m: Modalidade): String = if (m.trevos > 0) "Com mais números ou trevos" else "Com mais números"

    /** "1 aposta", "3 apostas". */
    fun quantas(n: Int): String = if (n == 1) "1 aposta" else "$n apostas"

    /** Quantas apostas do tipo [numeros]/[trevos] a configuração tem (a simples no controle dela; as outras, 0 se não houver). */
    fun apostasDe(m: Modalidade, c: ConfigDaCalculadora, numeros: Int, trevos: Int): Int {
        val s = simples(m)
        if (numeros == s.numeros && trevos == s.trevos) return c.simples
        return c.extras.filter { it.numeros == numeros && it.trevos == trevos }.sumOf { it.apostas }
    }

    /**
     * A configuração com [apostas] do tipo [numeros]/[trevos] (desde a 0.40 cada tipo tem o seu controle na tela, a simples
     * começando em 1 e os outros em 0; os que ficam em 0 saem da lista guardada). Nunca abaixo de 0.
     */
    fun comApostas(m: Modalidade, c: ConfigDaCalculadora, numeros: Int, trevos: Int, apostas: Int): ConfigDaCalculadora {
        val q = apostas.coerceAtLeast(0)
        val s = simples(m)
        if (numeros == s.numeros && trevos == s.trevos) return c.copy(simples = q)
        val outras = c.extras.filterNot { it.numeros == numeros && it.trevos == trevos }
        val extras = if (q == 0) outras else (outras + LinhaDaCalculadora(numeros, trevos, q)).sortedWith(compareBy({ it.numeros }, { it.trevos }))
        return c.copy(extras = extras)
    }
}

/**
 * Um tipo de aposta com mais números na calculadora (desde a 0.38): apostas com [numeros] números (e [trevos]), [apostas]
 * delas. Desde a 0.40 só os que não estão em 0 ficam guardados (o padrão 1 continua, para ler o que já estava guardado).
 */
@Serializable
data class LinhaDaCalculadora(val numeros: Int, val trevos: Int = 0, val apostas: Int = 1)

/**
 * A configuração da calculadora de uma modalidade (desde a 0.38): quantas apostas simples (começa em 1), as apostas com
 * mais números (as que não estão em 0), a amplitude dos controles ([maximo]) e as faixas do painel ([faixas]; null =
 * todas, com o "Qualquer prêmio" = [Calculadora.QUALQUER_PREMIO]). O "Redefinir" volta a esta configuração padrão.
 */
@Serializable
data class ConfigDaCalculadora(
    val simples: Int = 1,
    val extras: List<LinhaDaCalculadora> = emptyList(),
    val maximo: Int = Calculadora.AMPLITUDE_PADRAO,
    val faixas: List<Int>? = null,
)

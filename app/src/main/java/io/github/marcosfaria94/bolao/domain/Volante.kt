package io.github.marcosfaria94.bolao.domain

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** Palpite de uma aposta simples. */
data class Palpite(
    /** Números marcados (Super Sete: um dígito por coluna, na ordem das colunas). */
    val numeros: List<Int> = emptyList(),
    /** +Milionária: trevos de 1 a 6. */
    val trevos: List<Int> = emptyList(),
    /** Time do Coração (Timemania) ou Mês de Sorte (Dia de Sorte). */
    val extra: String? = null,
    /** Loteca: "1", "X" ou "2" para cada jogo. */
    val loteca: List<String> = emptyList(),
)

/** Gerador de números aleatórios e regras do volante da aposta simples. */
object Volante {
    val TREVOS = 1..6
    val OPCOES_LOTECA = listOf("1", "X", "2")
    const val JOGOS_LOTECA = 14
    const val COLUNAS_SUPER_SETE = 7

    private val aleatorio: Random = SecureRandom().asKotlinRandom()

    fun universo(m: Modalidade): IntRange = m.menor..m.maior

    /** Só a Lotomania tem a aposta espelho. */
    fun temEspelho(m: Modalidade): Boolean = m == Modalidade.LOTOMANIA

    /**
     * A aposta espelho da Lotomania (desde a 0.20): os números que ficaram de fora dos [numeros]
     * (com 50 marcados, os outros 50), em ordem.
     */
    fun espelho(m: Modalidade, numeros: List<Int>): List<Int> = (universo(m) - numeros.toSet()).sorted()

    // ---------- apostas com mais números (desde a 0.22, parâmetro "Apostas com mais números") ----------

    /**
     * O máximo de números de uma aposta: nas modalidades em que a Caixa aceita mais números que a
     * aposta simples (cada número a mais multiplica as combinações e o preço), o limite dela; nas
     * outras, a quantidade da aposta simples. O Super Sete e a Loteca ficam na aposta simples.
     */
    fun maximo(m: Modalidade): Int = when (m) {
        Modalidade.MEGA_SENA -> 20
        Modalidade.LOTOFACIL -> 20
        Modalidade.QUINA -> 15
        Modalidade.DUPLA_SENA -> 15
        Modalidade.DIA_DE_SORTE -> 15
        Modalidade.MAIS_MILIONARIA -> 12
        else -> m.qtdAposta
    }

    /** +Milionária: de 2 a 6 trevos. */
    fun maximoTrevos(m: Modalidade): Int = if (m == Modalidade.MAIS_MILIONARIA) 6 else m.trevos

    /** A modalidade aceita mais números (ou trevos) que a aposta simples. */
    fun aceitaMais(m: Modalidade): Boolean = m.formato == Formato.NUMEROS && (maximo(m) > m.qtdAposta || maximoTrevos(m) > m.trevos)

    /** Combinações de [n] tomados [k] a [k] (0 quando não dá). */
    fun combinacoes(n: Int, k: Int): Long {
        if (k < 0 || n < k) return 0
        val j = minOf(k, n - k)
        var r = 1L
        for (i in 1..j) r = r * (n - j + i) / i
        return r
    }

    /**
     * Quantas apostas simples cabem no palpite: 1 na aposta simples; com mais números, as combinações
     * (7 números na Mega-Sena = 7 apostas; 8 = 28). Na +Milionária, vezes as combinações dos trevos.
     */
    fun apostasSimples(m: Modalidade, numeros: Int, trevos: Int = m.trevos): Long {
        if (m.formato != Formato.NUMEROS) return 1
        val dosNumeros = combinacoes(numeros.coerceAtLeast(m.qtdAposta), m.qtdAposta)
        val dosTrevos = if (m.trevos > 0) combinacoes(trevos.coerceAtLeast(m.trevos), m.trevos) else 1
        return (dosNumeros * dosTrevos).coerceAtLeast(1)
    }

    /** O preço do palpite: o da aposta simples vezes as apostas simples que cabem nele. */
    fun preco(m: Modalidade, p: Palpite, precoSimples: Long): Long = precoSimples * apostasSimples(m, p.numeros.size, p.trevos.size)

    /** O palpite tem mais números (ou trevos) que a aposta simples. */
    fun temMais(m: Modalidade, p: Palpite): Boolean = m.formato == Formato.NUMEROS && (p.numeros.size > m.qtdAposta || p.trevos.size > m.trevos)

    // ---------- sorteio ----------

    /**
     * O que já saiu nas outras apostas do mesmo concurso (desde a 0.22): o "Completar" evita repetir o
     * Mês de Sorte e o Time do Coração dentro do conjunto (com 12 meses, repetir por acaso é comum: em 5
     * apostas, 6 em cada 10 vezes) e não devolve um jogo igual a outro.
     */
    data class JaApostado(val extras: List<String> = emptyList(), val jogos: List<List<Int>> = emptyList())

    /**
     * Mês de Sorte ou Time do Coração sorteado: entre os que ainda não saíram nas outras apostas do
     * conjunto ([usados]); quando todos já saíram, entre os que saíram menos vezes.
     */
    fun extraAleatorio(m: Modalidade, r: Random = aleatorio, usados: List<String> = emptyList()): String? {
        val opcoes = when (m.extra) {
            Extra.MES_DE_SORTE -> MESES_DE_SORTE
            Extra.TIME_DO_CORACAO -> TIMES_DO_CORACAO
            Extra.NENHUM -> return null
        }
        val vezes = usados.groupingBy { Conferencia.normalizar(it) }.eachCount()
        val menor = opcoes.minOf { vezes[Conferencia.normalizar(it)] ?: 0 }
        return opcoes.filter { (vezes[Conferencia.normalizar(it)] ?: 0) == menor }.random(r)
    }

    /** Um palpite completo, sorteado. */
    fun sortear(m: Modalidade, r: Random = aleatorio): Palpite = when (m.formato) {
        Formato.NUMEROS -> Palpite(
            numeros = universo(m).shuffled(r).take(m.qtdAposta).sorted(),
            trevos = if (m.trevos > 0) TREVOS.shuffled(r).take(m.trevos).sorted() else emptyList(),
            extra = extraAleatorio(m, r),
        )
        Formato.COLUNAS -> Palpite(numeros = List(COLUNAS_SUPER_SETE) { r.nextInt(0, 10) })
        Formato.LOTECA -> Palpite(loteca = List(JOGOS_LOTECA) { OPCOES_LOTECA.random(r) })
        Formato.FEDERAL -> Palpite()
    }

    /**
     * Mantém o que foi marcado e completa o resto com números aleatórios (até a aposta simples; com mais
     * números já marcados, eles ficam). [ja]: o que as outras apostas do mesmo concurso já têm.
     */
    fun completar(m: Modalidade, atual: Palpite, r: Random = aleatorio, ja: JaApostado = JaApostado()): Palpite = when (m.formato) {
        Formato.NUMEROS -> {
            val marcados = atual.numeros.distinct().filter { it in universo(m) }.take(maximo(m))
            val faltam = (m.qtdAposta - marcados.size).coerceAtLeast(0)
            val trevos = atual.trevos.distinct().take(maximoTrevos(m))
            val faltamTrevos = (m.trevos - trevos.size).coerceAtLeast(0)
            val novosTrevos = (TREVOS - trevos.toSet()).shuffled(r).take(faltamTrevos)
            // Não devolve um jogo igual a outro do conjunto (quase impossível por acaso; sorteia de novo).
            var numeros = marcados.sorted()
            for (tentativa in 1..(if (faltam > 0) 20 else 1)) {
                numeros = (marcados + (universo(m) - marcados.toSet()).shuffled(r).take(faltam)).sorted()
                if (faltam == 0 || ja.jogos.none { it.sorted() == numeros }) break
            }
            atual.copy(
                numeros = numeros,
                trevos = (trevos + novosTrevos).sorted(),
                extra = atual.extra?.takeIf { it.isNotBlank() } ?: extraAleatorio(m, r, ja.extras),
            )
        }
        Formato.COLUNAS -> {
            val cols = (0 until COLUNAS_SUPER_SETE).map { i -> atual.numeros.getOrNull(i)?.takeIf { it in 0..9 } ?: r.nextInt(0, 10) }
            atual.copy(numeros = cols)
        }
        Formato.LOTECA -> {
            val jogos = (0 until JOGOS_LOTECA).map { i -> atual.loteca.getOrNull(i)?.takeIf { it in OPCOES_LOTECA } ?: OPCOES_LOTECA.random(r) }
            atual.copy(loteca = jogos)
        }
        Formato.FEDERAL -> atual
    }

    /**
     * O "+1" do volante (desde a 0.27, com as apostas com mais números): marca mais um número sorteado entre
     * os que ainda não estão marcados, até o máximo da modalidade (no máximo, o palpite volta igual). Os
     * trevos, o time e o mês não mudam.
     */
    fun maisUm(m: Modalidade, atual: Palpite, r: Random = aleatorio): Palpite {
        if (m.formato != Formato.NUMEROS || atual.numeros.size >= maximo(m)) return atual
        val livres = (universo(m) - atual.numeros.toSet()).toList()
        if (livres.isEmpty()) return atual
        return atual.copy(numeros = (atual.numeros + livres.random(r)).sorted())
    }

    /** Ainda cabe mais um número no palpite (o "+1" fica ligado). */
    fun cabeMaisUm(m: Modalidade, p: Palpite): Boolean = m.formato == Formato.NUMEROS && p.numeros.size < maximo(m)

    /**
     * Marca ou desmarca um número, sem passar da quantidade da aposta simples (ou, com [mais] números
     * permitidos, do máximo da modalidade).
     */
    fun alternar(m: Modalidade, atual: Palpite, numero: Int, mais: Boolean = false): Palpite {
        val tem = numero in atual.numeros
        val limite = if (mais) maximo(m) else m.qtdAposta
        return when {
            tem -> atual.copy(numeros = atual.numeros - numero)
            atual.numeros.size >= limite -> atual
            else -> atual.copy(numeros = (atual.numeros + numero).sorted())
        }
    }

    fun alternarTrevo(m: Modalidade, atual: Palpite, trevo: Int, mais: Boolean = false): Palpite {
        val tem = trevo in atual.trevos
        val limite = if (mais) maximoTrevos(m) else m.trevos
        return when {
            tem -> atual.copy(trevos = atual.trevos - trevo)
            atual.trevos.size >= limite -> atual
            else -> atual.copy(trevos = (atual.trevos + trevo).sorted())
        }
    }

    /**
     * Quantos números ainda faltam marcar para a aposta simples (0 = já dá para apostar): os números
     * nas modalidades de números, as colunas sem número no Super Sete, os jogos sem palpite na Loteca.
     */
    fun faltam(m: Modalidade, p: Palpite): Int = when (m.formato) {
        Formato.NUMEROS -> (m.qtdAposta - p.numeros.distinct().count { it in universo(m) }).coerceAtLeast(0)
        Formato.COLUNAS -> (0 until COLUNAS_SUPER_SETE).count { p.numeros.getOrNull(it) !in 0..9 }
        Formato.LOTECA -> (0 until JOGOS_LOTECA).count { p.loteca.getOrNull(it) !in OPCOES_LOTECA }
        Formato.FEDERAL -> 0
    }

    /**
     * O que ainda falta para o palpite ficar completo (vazio = pronto). Uma aposta com mais números que
     * a simples (até o máximo da modalidade) também está completa: quem limita é o volante ([alternar]).
     */
    fun pendencias(m: Modalidade, p: Palpite): List<String> = buildList {
        when (m.formato) {
            Formato.NUMEROS -> {
                if (p.numeros.size !in m.qtdAposta..maximo(m) || p.numeros.distinct().size != p.numeros.size ||
                    p.numeros.any { it !in universo(m) }
                ) {
                    add("Marque ${m.qtdAposta} números (marcados: ${p.numeros.size}).")
                }
                if (m.trevos > 0 && p.trevos.size !in m.trevos..maximoTrevos(m)) add("Marque ${m.trevos} trevos (marcados: ${p.trevos.size}).")
                if (m.extra == Extra.TIME_DO_CORACAO && p.extra.isNullOrBlank()) add("Informe o Time do Coração.")
                if (m.extra == Extra.MES_DE_SORTE && p.extra.isNullOrBlank()) add("Escolha o Mês de Sorte.")
            }
            Formato.COLUNAS -> if (p.numeros.size != COLUNAS_SUPER_SETE || p.numeros.any { it !in 0..9 }) {
                add("Marque um número em cada uma das 7 colunas.")
            }
            Formato.LOTECA -> if (p.loteca.size != JOGOS_LOTECA || p.loteca.any { it !in OPCOES_LOTECA }) {
                add("Marque um palpite em cada um dos 14 jogos.")
            }
            Formato.FEDERAL -> Unit
        }
    }

    /** Texto curto do palpite para a lista de jogos e mensagens. */
    fun resumo(m: Modalidade, p: Palpite): String = when (m.formato) {
        Formato.NUMEROS -> buildString {
            append(p.numeros.joinToString(" ") { m.rotulo(it) })
            if (p.trevos.isNotEmpty()) append(" · trevos ${p.trevos.joinToString(" ")}")
            p.extra?.let { append(" · $it") }
        }
        Formato.COLUNAS -> p.numeros.joinToString(" ")
        Formato.LOTECA -> p.loteca.joinToString(" ")
        Formato.FEDERAL -> ""
    }
}

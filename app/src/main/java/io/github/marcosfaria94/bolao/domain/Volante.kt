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

    /** Um palpite completo, sorteado. */
    fun sortear(m: Modalidade, r: Random = aleatorio): Palpite = when (m.formato) {
        Formato.NUMEROS -> Palpite(
            numeros = universo(m).shuffled(r).take(m.qtdAposta).sorted(),
            trevos = if (m.trevos > 0) TREVOS.shuffled(r).take(m.trevos).sorted() else emptyList(),
            extra = if (m.extra == Extra.MES_DE_SORTE) MESES_DE_SORTE.random(r) else null,
        )
        Formato.COLUNAS -> Palpite(numeros = List(COLUNAS_SUPER_SETE) { r.nextInt(0, 10) })
        Formato.LOTECA -> Palpite(loteca = List(JOGOS_LOTECA) { OPCOES_LOTECA.random(r) })
        Formato.FEDERAL -> Palpite()
    }

    /** Mantém o que foi marcado e completa o resto com números aleatórios. */
    fun completar(m: Modalidade, atual: Palpite, r: Random = aleatorio): Palpite = when (m.formato) {
        Formato.NUMEROS -> {
            val faltam = (m.qtdAposta - atual.numeros.size).coerceAtLeast(0)
            val novos = (universo(m) - atual.numeros.toSet()).shuffled(r).take(faltam)
            val faltamTrevos = (m.trevos - atual.trevos.size).coerceAtLeast(0)
            val novosTrevos = (TREVOS - atual.trevos.toSet()).shuffled(r).take(faltamTrevos)
            atual.copy(
                numeros = (atual.numeros.take(m.qtdAposta) + novos).sorted(),
                trevos = (atual.trevos.take(m.trevos) + novosTrevos).sorted(),
                extra = atual.extra ?: if (m.extra == Extra.MES_DE_SORTE) MESES_DE_SORTE.random(r) else null,
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

    /** Marca ou desmarca um número, sem passar da quantidade da aposta simples. */
    fun alternar(m: Modalidade, atual: Palpite, numero: Int): Palpite {
        val tem = numero in atual.numeros
        return when {
            tem -> atual.copy(numeros = atual.numeros - numero)
            atual.numeros.size >= m.qtdAposta -> atual
            else -> atual.copy(numeros = (atual.numeros + numero).sorted())
        }
    }

    fun alternarTrevo(m: Modalidade, atual: Palpite, trevo: Int): Palpite {
        val tem = trevo in atual.trevos
        return when {
            tem -> atual.copy(trevos = atual.trevos - trevo)
            atual.trevos.size >= m.trevos -> atual
            else -> atual.copy(trevos = (atual.trevos + trevo).sorted())
        }
    }

    /** O que ainda falta para o palpite ficar completo (vazio = pronto). */
    fun pendencias(m: Modalidade, p: Palpite): List<String> = buildList {
        when (m.formato) {
            Formato.NUMEROS -> {
                if (p.numeros.size != m.qtdAposta || p.numeros.distinct().size != p.numeros.size ||
                    p.numeros.any { it !in universo(m) }
                ) {
                    add("Marque ${m.qtdAposta} números (marcados: ${p.numeros.size}).")
                }
                if (m.trevos > 0 && p.trevos.size != m.trevos) add("Marque ${m.trevos} trevos (marcados: ${p.trevos.size}).")
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

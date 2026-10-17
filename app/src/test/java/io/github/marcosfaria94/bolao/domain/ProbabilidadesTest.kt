package io.github.marcosfaria94.bolao.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * "1 em X" da aposta simples, arredondado para o inteiro mais próximo.
 * As tabelas da Caixa às vezes truncam (ex.: Lotofácil 14 acertos = 21.791,7 aparece como 21.791),
 * então podem diferir em 1 da conta exata.
 */
class ProbabilidadesTest {

    private fun umEm(m: Modalidade) = Probabilidades.faixas(m).map { it.umEm }

    @Test
    fun megaSena() = assertEquals(listOf(50_063_860L, 154_518L, 2_332L), umEm(Modalidade.MEGA_SENA))

    @Test
    fun lotofacil() = assertEquals(listOf(3_268_760L, 21_792L, 692L, 60L, 11L), umEm(Modalidade.LOTOFACIL))

    @Test
    fun quina() = assertEquals(listOf(24_040_016L, 64_107L, 866L, 36L), umEm(Modalidade.QUINA))

    @Test
    fun lotomania() = assertEquals(
        listOf(11_372_636L, 352_552L, 24_236L, 2_777L, 473L, 112L, 11_372_636L),
        umEm(Modalidade.LOTOMANIA),
    )

    @Test
    fun timemania() = assertEquals(listOf(26_472_637L, 216_103L, 5_220L, 276L, 29L, 80L), umEm(Modalidade.TIMEMANIA))

    @Test
    fun duplaSena() = assertEquals(listOf(15_890_700L, 60_192L, 1_120L, 60L), umEm(Modalidade.DUPLA_SENA))

    @Test
    fun diaDeSorte() = assertEquals(listOf(2_629_575L, 15_652L, 454L, 37L, 12L), umEm(Modalidade.DIA_DE_SORTE))

    @Test
    fun superSete() = assertEquals(listOf(10_000_000L, 158_730L, 5_879L, 392L, 44L), umEm(Modalidade.SUPER_SETE))

    @Test
    fun maisMilionaria() = assertEquals(238_360_500L, umEm(Modalidade.MAIS_MILIONARIA).first())

    @Test
    fun loteca() = assertEquals(listOf(4_782_969L, 170_820L), umEm(Modalidade.LOTECA))

    @Test
    fun texto() = assertEquals("1 em 50.063.860", Probabilidades.texto(50_063_860))
}

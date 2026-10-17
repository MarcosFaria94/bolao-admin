package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Modalidade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class LembretesTest {

    private val fuso = ZoneOffset.ofHours(-3)
    private val pessoas = listOf(Pessoa("p1", "A", "A", "x", true), Pessoa("p2", "B", "B", "y"), Pessoa("p3", "C", "C", "z"))
    private val base = Estado(cadastroConcluido = true, pessoas = pessoas, proximoJogo = 1)

    private fun chaves(e: Estado, agora: LocalDateTime, avisados: Set<String> = emptySet()) =
        Lembretes.pendentes(e, agora, avisados, fuso).map { it.chave }

    @Test
    fun aporteNoDia1() {
        val dia1 = LocalDateTime.of(2026, 11, 1, 9, 30)
        assertTrue("aporte-2026-11" in chaves(base, dia1))
        assertTrue("aporte-2026-11" !in chaves(base, dia1.withHour(8)))
        val comAporte = Operacoes.registrarAportes(base, dia1.toLocalDate(), listOf("p1" to 1000L), false, 1)
        assertTrue("aporte-2026-11" !in chaves(comAporte, dia1))
        // Já avisado não repete.
        assertTrue("aporte-2026-11" !in chaves(base, dia1, setOf("aporte-2026-11")))
    }

    @Test
    fun diaDeJogo() {
        val dia19 = LocalDateTime.of(2026, 10, 19, 10, 5)
        assertTrue("jogo-2026-10-19" in chaves(base, dia19))
        assertTrue(chaves(base, LocalDateTime.of(2026, 10, 18, 11, 0)).none { it.startsWith("jogo-") })
        val a = Aposta("a", 0, Modalidade.QUINA, 1, LocalDate.of(2026, 10, 19), valor = 300, criadaEm = 0, comprovante = null)
        val e = Operacoes.registrarAposta(Operacoes.salvarRascunho(base, a), "a", 1)
        assertTrue("jogo-2026-10-19" !in chaves(e, dia19))
    }

    @Test
    fun comprovantePendente() {
        val criada = LocalDateTime.of(2026, 10, 19, 10, 0).atZone(fuso).toInstant().toEpochMilli()
        val r = Aposta("r", 0, Modalidade.MEGA_SENA, 1, LocalDate.of(2026, 10, 19), valor = 600, criadaEm = criada)
        val e = Operacoes.salvarRascunho(base, r)
        assertTrue("comprovante-r" !in chaves(e, LocalDateTime.of(2026, 10, 19, 10, 30)))
        assertTrue("comprovante-r" in chaves(e, LocalDateTime.of(2026, 10, 19, 11, 5)))
    }

    @Test
    fun extratoNoUltimoDia() {
        assertTrue("extrato-2026-10" in chaves(base, LocalDateTime.of(2026, 10, 31, 18, 0)))
        assertTrue("extrato-2026-10" !in chaves(base, LocalDateTime.of(2026, 10, 30, 18, 0)))
    }

    @Test
    fun especiais() {
        assertEquals(LocalDate.of(2026, 4, 5), Especiais.pascoa(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Especiais.pascoa(2027))
        assertTrue("especial-MEGA_SENA-2026-12-31" in chaves(base, LocalDateTime.of(2026, 12, 22, 12, 0)))
        assertTrue(chaves(base, LocalDateTime.of(2026, 12, 10, 12, 0)).none { it.startsWith("especial-MEGA") })
    }

    @Test
    fun resultadoSoDepoisDoSorteio() {
        val quarta = LocalDate.of(2026, 10, 21)
        val a = Aposta("a", 0, Modalidade.QUINA, 1, LocalDate.of(2026, 10, 19), dataSorteio = quarta, valor = 300, criadaEm = 0)
        val e = Operacoes.registrarAposta(Operacoes.salvarRascunho(base, a), "a", 1)
        assertEquals(0, Lembretes.aguardandoResultado(e, quarta.atTime(21, 30)).size)
        assertEquals(1, Lembretes.aguardandoResultado(e, quarta.atTime(22, 10)).size)
        val domingo = LocalDate.of(2026, 10, 25)
        val b = a.copy(id = "b", dataSorteio = domingo)
        val e2 = Operacoes.registrarAposta(Operacoes.salvarRascunho(base, b), "b", 1)
        assertEquals(1, Lembretes.aguardandoResultado(e2, domingo.atTime(12, 30)).size)
    }
}

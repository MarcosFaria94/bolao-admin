package io.github.marcosfaria94.bolao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DinheiroECalendarioTest {

    @Test
    fun formatar() {
        assertEquals("0,00", Dinheiro.formatar(0))
        assertEquals("0,02", Dinheiro.formatar(2))
        assertEquals("3,50", Dinheiro.formatar(350))
        assertEquals("1.234,56", Dinheiro.formatar(123456))
        assertEquals("1.000.000,00", Dinheiro.formatar(100_000_000))
        assertEquals("-3,50", Dinheiro.formatar(-350))
        assertEquals("R$ 40,02", Dinheiro.rs(4002))
    }

    @Test
    fun ler() {
        assertEquals(4002L, Dinheiro.ler("40,02"))
        assertEquals(4002L, Dinheiro.ler("R$ 40,02"))
        assertEquals(123456L, Dinheiro.ler("1.234,56"))
        assertEquals(1000L, Dinheiro.ler("10"))
        assertEquals(350L, Dinheiro.ler("3.5"))
        assertEquals(350L, Dinheiro.ler("3,5"))
        assertEquals(100000L, Dinheiro.ler("1.000"))
        assertEquals(-200L, Dinheiro.ler("-2,00"))
        assertNull(Dinheiro.ler(""))
        assertNull(Dinheiro.ler("abc"))
        assertNull(Dinheiro.ler("1,234"))
    }

    @Test
    fun abreviarPremio() {
        assertEquals("2M", Dinheiro.abreviar(200_000_000))
        assertEquals("1,7M", Dinheiro.abreviar(170_000_000))
        assertEquals("45M", Dinheiro.abreviar(4_500_000_000))
        assertEquals("120M", Dinheiro.abreviar(12_000_000_000))
        assertEquals("600K", Dinheiro.abreviar(60_000_000))
        assertEquals("500K", Dinheiro.abreviar(50_000_000))
        assertEquals("1B", Dinheiro.abreviar(100_000_000_000))
        assertEquals("1,1B", Dinheiro.abreviar(110_000_000_000))
        assertEquals("850", Dinheiro.abreviar(85_000))
    }

    @Test
    fun datas() {
        val d = LocalDate.of(2026, 6, 1)
        assertEquals("010626", Datas.compacta(d))
        assertEquals("01/06/26", Datas.curta(d))
        assertEquals(LocalDate.of(2026, 10, 17), Datas.lerBr("17/10/2026"))
        assertNull(Datas.lerBr("2026-10-17"))
    }

    @Test
    fun calendario() {
        assertTrue(Calendario.ehDiaProgramado(LocalDate.of(2026, 10, 19)))
        assertFalse(Calendario.ehDiaProgramado(LocalDate.of(2026, 10, 17)))
        assertEquals(LocalDate.of(2026, 10, 19), Calendario.proximoDiaProgramado(LocalDate.of(2026, 10, 17)))
        assertEquals(LocalDate.of(2026, 10, 19), Calendario.proximoDiaProgramado(LocalDate.of(2026, 10, 19)))
        assertEquals(LocalDate.of(2026, 10, 22), Calendario.diaProgramadoDepoisDe(LocalDate.of(2026, 10, 19)))
        assertEquals(LocalDate.of(2026, 11, 1), Calendario.proximoDiaProgramado(LocalDate.of(2026, 10, 29)))
        assertEquals(LocalDate.of(2027, 1, 1), Calendario.proximoDiaProgramado(LocalDate.of(2026, 12, 31)))
        assertEquals(10, Calendario.diasProgramados(java.time.YearMonth.of(2027, 2)).size)
        assertEquals(10, Calendario.apostasRestantesNoMes(LocalDate.of(2026, 10, 1), apostaDeHojeFeita = false))
        assertEquals(9, Calendario.apostasRestantesNoMes(LocalDate.of(2026, 10, 1), apostaDeHojeFeita = true))
        assertEquals(0, Calendario.apostasRestantesNoMes(LocalDate.of(2026, 10, 30), apostaDeHojeFeita = false))
        assertTrue(Calendario.ehUltimoDiaDoMes(LocalDate.of(2026, 2, 28).withYear(2027)))
    }

    @Test
    fun rateioSemSaldoSuficiente() {
        assertNull(CalculoRateio.calcular(saldoLiquido = 1500, provisionado = 1500))
        assertNull(CalculoRateio.calcular(saldoLiquido = 1502, provisionado = 1500))
        val r = CalculoRateio.calcular(saldoLiquido = 1503, provisionado = 1500)!!
        assertEquals(1, r.cota)
        // O provisionado nunca passa dos aportes do mês.
        assertEquals(2000, CalculoRateio.provisionado(10, 300, aportesDoMes = 2000))
        assertEquals(0, CalculoRateio.provisionado(5, 300, aportesDoMes = 0))
    }
}

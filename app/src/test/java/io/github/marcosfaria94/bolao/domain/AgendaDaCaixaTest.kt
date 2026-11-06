package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa.Estado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** A agenda dos sorteios (desde a 0.20): quando consultar a Caixa e se os dados guardados são certos. */
class AgendaDaCaixaTest {
    // Quarta-feira, 21/10/2026 (sorteio da Quina às 21h; da Federal, às 20h).
    private val quarta = LocalDate.of(2026, 10, 21)
    private val domingo = LocalDate.of(2026, 10, 25)

    private fun em(t: LocalDateTime): Instant = t.atZone(AgendaDaCaixa.FUSO).toInstant()

    private val rateio = listOf(FaixaPremio(1, "5 acertos", 0, 0), FaixaPremio(2, "4 acertos", 54, 812_345), FaixaPremio(3, "3 acertos", 4_210, 9_876))

    private fun quina(numero: Int, proxima: LocalDate?, faixas: List<FaixaPremio> = rateio) = Concurso(
        modalidade = Modalidade.QUINA, numero = numero, data = quarta.minusDays(1), dezenas = listOf(5, 12, 33, 47, 71),
        faixas = faixas, proximoNumero = numero + 1, proximaData = proxima, premioEstimadoProximo = 700_000_000,
    )

    @Test
    fun horarioDosSorteios() {
        assertEquals("21h", AgendaDaCaixa.hora(AgendaDaCaixa.horaDoSorteio(Modalidade.QUINA, quarta)))
        assertEquals("20h", AgendaDaCaixa.hora(AgendaDaCaixa.horaDoSorteio(Modalidade.FEDERAL, quarta)))
        assertEquals("11h", AgendaDaCaixa.hora(AgendaDaCaixa.horaDoSorteio(Modalidade.LOTOFACIL, domingo)))
        // A consulta é 1 hora depois do sorteio, na hora de Brasília.
        assertEquals(em(quarta.atTime(22, 0)), AgendaDaCaixa.resultadoSai(Modalidade.QUINA, quarta))
        assertEquals(em(quarta.atTime(21, 0)), AgendaDaCaixa.resultadoSai(Modalidade.FEDERAL, quarta))
        assertEquals(em(domingo.atTime(12, 0)), AgendaDaCaixa.resultadoSai(Modalidade.QUINA, domingo))
    }

    @Test
    fun dadosSeguradosAteOProximoSorteio() {
        val u = quina(7148, proxima = quarta)
        // De manhã, com o resultado de ontem: em dia (certos) até o sorteio das 21h, sem consultar a Caixa.
        val manha = AgendaDaCaixa.situacao(Modalidade.QUINA, u, em(quarta.atTime(9, 0)), tentativa = em(quarta.atTime(8, 0)))
        assertEquals(Estado.EM_DIA, manha.estado)
        assertTrue(manha.certa)
        assertFalse(manha.precisaConsultar(em(quarta.atTime(9, 0))))
        assertEquals(em(quarta.atTime(22, 0)), manha.consultarEm)
        // Durante o sorteio (e a primeira hora depois): o concurso já foi sorteado, mas ainda não consulta.
        val sorteando = AgendaDaCaixa.situacao(Modalidade.QUINA, u, em(quarta.atTime(21, 30)), tentativa = null)
        assertEquals(Estado.SORTEANDO, sorteando.estado)
        assertTrue(sorteando.sorteado)
        assertFalse(sorteando.precisaConsultar(em(quarta.atTime(21, 30))))
        // 1 hora depois: consulta (uma vez).
        val depois = AgendaDaCaixa.situacao(Modalidade.QUINA, u, em(quarta.atTime(22, 0)), tentativa = em(quarta.atTime(8, 0)))
        assertEquals(Estado.AGUARDANDO, depois.estado)
        assertTrue(depois.precisaConsultar(em(quarta.atTime(22, 0))))
        // Consultou às 22h e o resultado não veio: tenta de novo 15 minutos depois.
        val semResultado = AgendaDaCaixa.situacao(Modalidade.QUINA, u, em(quarta.atTime(22, 5)), tentativa = em(quarta.atTime(22, 0)))
        assertEquals(Estado.AGUARDANDO, semResultado.estado)
        assertEquals(em(quarta.atTime(22, 15)), semResultado.consultarEm)
        assertTrue(AgendaDaCaixa.situacao(Modalidade.QUINA, u, em(quarta.atTime(22, 15)), em(quarta.atTime(22, 0))).precisaConsultar(em(quarta.atTime(22, 15))))
        // Mais de 2 horas esperando (ex.: a Loteca, que depende dos jogos): de hora em hora.
        val demorado = AgendaDaCaixa.situacao(Modalidade.QUINA, u, em(quarta.plusDays(1).atTime(0, 30)), tentativa = em(quarta.plusDays(1).atTime(0, 20)))
        assertEquals(Estado.AGUARDANDO, demorado.estado)
        assertEquals(em(quarta.plusDays(1).atTime(1, 20)), demorado.consultarEm)
        // Chegou o resultado novo (próximo sorteio na sexta): em dia de novo.
        val novo = quina(7149, proxima = quarta.plusDays(2))
        assertEquals(Estado.EM_DIA, AgendaDaCaixa.situacao(Modalidade.QUINA, novo, em(quarta.atTime(22, 20)), em(quarta.atTime(22, 15))).estado)
    }

    @Test
    fun semRateioSemDataESemDados() {
        // O resultado chegou sem o rateio (ganhadores sem o valor): tenta de novo a cada 15 minutos.
        val semRateio = quina(7149, quarta.plusDays(2), listOf(FaixaPremio(2, "4 acertos", 54, 0), FaixaPremio(3, "3 acertos", 4_210, 0))).copy(data = quarta)
        assertTrue(AgendaDaCaixa.semRateio(semRateio))
        val s = AgendaDaCaixa.situacao(Modalidade.QUINA, semRateio, em(quarta.atTime(22, 5)), em(quarta.atTime(22, 0)))
        assertEquals(Estado.SEM_RATEIO, s.estado)
        assertEquals(em(quarta.atTime(22, 15)), s.consultarEm)
        // De madrugada (mais de 2 horas depois), de hora em hora.
        assertEquals(em(quarta.plusDays(1).atTime(1, 0)), AgendaDaCaixa.situacao(Modalidade.QUINA, semRateio, em(quarta.plusDays(1).atTime(0, 10)), em(quarta.plusDays(1).atTime(0, 0))).consultarEm)
        // Ninguém ganhou em nenhuma faixa (ainda não apurado) também é sem rateio; na Loteca, não.
        assertTrue(AgendaDaCaixa.semRateio(quina(7149, null, listOf(FaixaPremio(1, "5 acertos", 0, 0), FaixaPremio(2, "4 acertos", 0, 0)))))
        // Sem a data do próximo sorteio: pergunta de novo a cada 2 horas.
        val semData = AgendaDaCaixa.situacao(Modalidade.QUINA, quina(7149, proxima = null), em(quarta.atTime(23, 0)), em(quarta.atTime(22, 30)))
        assertEquals(Estado.SEM_DATA, semData.estado)
        assertNull(semData.sorteio)
        assertEquals(em(quarta.plusDays(1).atTime(0, 30)), semData.consultarEm)
        // Nada guardado: consulta já.
        val nada = AgendaDaCaixa.situacao(Modalidade.QUINA, null, em(quarta.atTime(10, 0)), null)
        assertEquals(Estado.SEM_DADOS, nada.estado)
        assertTrue(nada.precisaConsultar(em(quarta.atTime(10, 0))))
    }

    @Test
    fun nuncaVoltaParaUmConcursoMaisVelho() {
        val guardado = quina(7149, quarta.plusDays(2))
        assertTrue(AgendaDaCaixa.aceitar(null, guardado))
        // A Caixa respondeu com o concurso anterior (servidor atrasado): fica o guardado.
        assertFalse(AgendaDaCaixa.aceitar(guardado, quina(7148, quarta)))
        assertTrue(AgendaDaCaixa.aceitar(guardado, quina(7150, quarta.plusDays(3))))
        // O mesmo concurso: não troca o completo pelo sem rateio nem perde a data do próximo.
        assertFalse(AgendaDaCaixa.aceitar(guardado, guardado.copy(faixas = listOf(FaixaPremio(2, "4 acertos", 54, 0)))))
        assertFalse(AgendaDaCaixa.aceitar(guardado, guardado.copy(proximaData = null)))
        assertTrue(AgendaDaCaixa.aceitar(guardado, guardado.copy(premioEstimadoProximo = 800_000_000)))
    }

    @Test
    fun quandoNasFrases() {
        val agora = em(quarta.atTime(10, 0))
        assertEquals("hoje às 21h", AgendaDaCaixa.quando(em(quarta.atTime(21, 0)), agora))
        assertEquals("amanhã às 21h", AgendaDaCaixa.quando(em(quarta.plusDays(1).atTime(21, 0)), agora))
        assertEquals("em 4 dias às 11h", AgendaDaCaixa.quando(em(domingo.atTime(11, 0)), agora))
        assertEquals("em 30/10 às 21h30", AgendaDaCaixa.quando(em(LocalDate.of(2026, 10, 30).atTime(21, 30)), agora))
    }

    @Test
    fun sorteioNasTelas() {
        assertEquals("Hoje", Datas.sorteio(quarta, quarta))
        assertEquals("Amanhã", Datas.sorteio(quarta.plusDays(1), quarta))
        assertEquals("Em 2 dias", Datas.sorteio(quarta.plusDays(2), quarta))
        assertEquals("Em 7 dias", Datas.sorteio(quarta.plusDays(7), quarta))
        assertEquals("29/10/2026", Datas.sorteio(quarta.plusDays(8), quarta))
        assertEquals("Ontem", Datas.sorteio(quarta.minusDays(1), quarta))
    }
}

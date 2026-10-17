package io.github.marcosfaria94.bolao.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Confere, caractere por caractere, as mensagens geradas contra os exemplos do termo. */
class MensagensTest {

    private val d = { dia: Int -> LocalDate.of(2026, 6, dia) }

    @Test
    fun aporteIgualAoTermo() {
        val msg = Mensagens.aporte(d(1), saldoLiquido = 1002, aportes = 3000, eventual = false)
        assertEquals(ModelosDoTermo.APORTE, msg)
    }

    @Test
    fun apostaIgualAoTermo() {
        val id = Mensagens.identificacao(132, d(1), Modalidade.LOTOFACIL, 3060, 200_000_000, d(3))
        assertEquals("#132 010626 LF-3060 2M 030626", id)
        val msg = Mensagens.aposta(id, eventual = false, saldoLiquido = 4002, valor = 350)
        assertEquals(ModelosDoTermo.APOSTA, msg)
    }

    @Test
    fun apuracaoIgualAoTermo() {
        val id = Mensagens.identificacao(132, d(1), Modalidade.LOTOFACIL, 3060, 200_000_000, d(3))
        val msg = Mensagens.apuracao(id, "12/15", saldoLiquido = 3652, premio = 1400)
        assertEquals(ModelosDoTermo.APURACAO, msg)
    }

    @Test
    fun rateioIgualAoTermo() {
        // 14/06: restam 16, 19, 22, 25 e 28 -> 5 × R$ 3,00 = R$ 15,00 provisionados.
        val restantes = Calendario.apostasRestantesNoMes(d(14), apostaDeHojeFeita = false)
        assertEquals(5, restantes)
        val porAposta = CalculoRateio.valorPorAposta(aporteMensal = 1000)
        assertEquals(300, porAposta)
        val prov = CalculoRateio.provisionado(restantes, porAposta, aportesDoMes = 3000)
        assertEquals(1500, prov)
        val r = CalculoRateio.calcular(saldoLiquido = 3902, provisionado = prov)!!
        assertEquals(800, r.cota)
        assertEquals(2400, r.total)
        assertEquals(2, r.sobra)
        assertEquals(1502, r.saldoFinal)
        val msg = Mensagens.rateio(1, d(14), r.saldoLiquido, r.total)
        assertEquals(ModelosDoTermo.RATEIO, msg)
    }

    @Test
    fun extratoIgualAoTermo() {
        val ap = Mensagens.Categoria.APOSTA
        val movs = listOf(
            Mensagens.Movimento(d(1), Mensagens.Categoria.APORTE, Mensagens.descricaoAporte("Fulano"), 1000),
            Mensagens.Movimento(d(1), Mensagens.Categoria.APORTE, Mensagens.descricaoAporte("Beto"), 1000),
            Mensagens.Movimento(d(1), Mensagens.Categoria.APORTE, Mensagens.descricaoAporte("Beltrano"), 1000),
            Mensagens.Movimento(d(1), ap, Mensagens.descricaoAposta(132, Modalidade.LOTOFACIL), -350),
            Mensagens.Movimento(d(3), Mensagens.Categoria.PREMIO, Mensagens.descricaoPremio(132, Modalidade.LOTOFACIL), 1400),
            Mensagens.Movimento(d(4), ap, Mensagens.descricaoAposta(133, Modalidade.QUINA), -300),
            Mensagens.Movimento(d(7), ap, Mensagens.descricaoAposta(134, Modalidade.QUINA), -300),
            Mensagens.Movimento(d(10), ap, Mensagens.descricaoAposta(135, Modalidade.SUPER_SETE), -250),
            Mensagens.Movimento(d(13), ap, Mensagens.descricaoAposta(136, Modalidade.LOTOMANIA), -300),
            Mensagens.Movimento(d(14), Mensagens.Categoria.RATEIO, Mensagens.descricaoRateio(1, "Fulano"), -800),
            Mensagens.Movimento(d(14), Mensagens.Categoria.RATEIO, Mensagens.descricaoRateio(1, "Beto"), -800),
            Mensagens.Movimento(d(14), Mensagens.Categoria.RATEIO, Mensagens.descricaoRateio(1, "Beltrano"), -800),
            Mensagens.Movimento(d(16), ap, Mensagens.descricaoAposta(137, Modalidade.LOTOMANIA), -300),
            Mensagens.Movimento(d(19), ap, Mensagens.descricaoAposta(138, Modalidade.QUINA), -300),
            Mensagens.Movimento(d(22), ap, Mensagens.descricaoAposta(139, Modalidade.SUPER_SETE), -250),
            Mensagens.Movimento(d(25), ap, Mensagens.descricaoAposta(140, Modalidade.LOTOMANIA), -300),
            Mensagens.Movimento(d(28), ap, Mensagens.descricaoAposta(141, Modalidade.TIMEMANIA), -350),
        )
        val msg = Mensagens.extrato(YearMonth.of(2026, 6), saldoAnterior = 1000, rendimento = 2, movimentos = movs)
        assertEquals(ModelosDoTermo.EXTRATO, msg)
    }

    @Test
    fun valoresMaioresContinuamAlinhados() {
        val msg = Mensagens.aposta("#7 040726 MS-2900 45M 050726", false, saldoLiquido = 123456, valor = 600)
        val linhas = msg.lines()
        assertEquals("Saldo Líquido:   R$ 1.234,56", linhas[1])
        assertEquals("Aposta.......: - R$     6,00", linhas[2])
        assertEquals("Saldo Final    = R$ 1.228,56", linhas[3])
    }

    @Test
    fun titulosEventuais() {
        val aporte = Mensagens.aporte(LocalDate.of(2026, 6, 15), 1000, 500, eventual = true)
        assertEquals("- APORTE EVENTUAL 150626 -", aporte.lines()[0])
        val aposta = Mensagens.aposta("#9 150626 MS-2901 3M 160626", eventual = true, saldoLiquido = 1000, valor = 600)
        assertEquals("- APOSTA EVENTUAL #9 150626 MS-2901 3M 160626 -", aposta.lines()[0])
    }
}

package io.github.marcosfaria94.bolao.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Confere, caractere por caractere, os títulos e os blocos gerados contra os exemplos do termo. */
class MensagensTest {

    private val d = { dia: Int -> LocalDate.of(2026, 6, dia) }

    @Test
    fun aporteIgualAoTermo() {
        assertEquals(ModelosDoTermo.TITULO_APORTE, Mensagens.tituloAporte(d(1)))
        assertEquals(ModelosDoTermo.APORTE, Mensagens.aporte(d(1), saldoInicial = 1002, aportes = 3000))
    }

    @Test
    fun apostaIgualAoTermo() {
        val id = Mensagens.identificacao(1, d(1), Modalidade.LOTOFACIL, 3060, 200_000_000, d(3))
        assertEquals("Nº1 010626 LF-3060 2M 030626", id)
        assertEquals(ModelosDoTermo.TITULO_APOSTA, Mensagens.tituloAposta(1, d(1)))
        assertEquals(ModelosDoTermo.APOSTA, Mensagens.aposta(id, saldoInicial = 4002, valor = 350))
    }

    @Test
    fun apuracaoIgualAoTermo() {
        val id = Mensagens.identificacao(1, d(1), Modalidade.LOTOFACIL, 3060, 200_000_000, d(3))
        assertEquals(ModelosDoTermo.TITULO_APURACAO, Mensagens.tituloApuracao(1, d(4)))
        assertEquals(ModelosDoTermo.APURACAO, Mensagens.apuracao(id, "12/15", premiada = true, saldoInicial = 3652, premio = 1400))
        val semPremio = Mensagens.apuracao(id, "9/15", premiada = false, saldoInicial = 3652, premio = 0)
        assertEquals("Acertos: 9/15 (Aposta não premiada)", semPremio.lines()[2])
        assertEquals("Prêmio.......: + R$  0,00", semPremio.lines()[4])
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
        assertEquals(ModelosDoTermo.TITULO_RATEIO, Mensagens.tituloRateio(d(14)))
        assertEquals(ModelosDoTermo.RATEIO, Mensagens.rateio(d(14), r.saldoLiquido, r.total))
    }

    @Test
    fun extratoIgualAoTermo() {
        val ap = Mensagens.Categoria.APOSTA
        val movs = listOf(
            Mensagens.Movimento(d(1), Mensagens.Categoria.APORTE, Mensagens.descricaoAporte(), 3000),
            Mensagens.Movimento(d(1), ap, Mensagens.descricaoAposta(1, Modalidade.LOTOFACIL), -350),
            Mensagens.Movimento(d(3), Mensagens.Categoria.PREMIO, Mensagens.descricaoPremio(1, Modalidade.LOTOFACIL), 1400),
            Mensagens.Movimento(d(4), ap, Mensagens.descricaoAposta(2, Modalidade.QUINA), -300),
            Mensagens.Movimento(d(7), ap, Mensagens.descricaoAposta(3, Modalidade.QUINA), -300),
            Mensagens.Movimento(d(10), ap, Mensagens.descricaoAposta(4, Modalidade.SUPER_SETE), -250),
            Mensagens.Movimento(d(13), ap, Mensagens.descricaoAposta(5, Modalidade.LOTOMANIA), -300),
            Mensagens.Movimento(d(14), Mensagens.Categoria.RATEIO, Mensagens.descricaoRateio(), -2400),
            Mensagens.Movimento(d(16), ap, Mensagens.descricaoAposta(6, Modalidade.LOTOMANIA), -300),
            Mensagens.Movimento(d(19), ap, Mensagens.descricaoAposta(7, Modalidade.QUINA), -300),
            Mensagens.Movimento(d(22), ap, Mensagens.descricaoAposta(8, Modalidade.SUPER_SETE), -250),
            Mensagens.Movimento(d(25), ap, Mensagens.descricaoAposta(9, Modalidade.LOTOMANIA), -300),
            Mensagens.Movimento(d(28), ap, Mensagens.descricaoAposta(10, Modalidade.TIMEMANIA), -350),
        )
        assertEquals(ModelosDoTermo.TITULO_EXTRATO, Mensagens.tituloExtrato(d(30)))
        val msg = Mensagens.extrato(YearMonth.of(2026, 6), saldoAnterior = 1000, rendimento = 2, movimentos = movs)
        assertEquals(ModelosDoTermo.EXTRATO, msg)
    }

    @Test
    fun valoresMaioresContinuamAlinhados() {
        val msg = Mensagens.aposta("Nº7 040726 MS-2900 45M 050726", saldoInicial = 123456, valor = 600)
        val linhas = msg.lines()
        assertEquals("Saldo Inicial:   R$ 1.234,56", linhas[2])
        assertEquals("Aposta.......: - R$     6,00", linhas[3])
        assertEquals("Saldo Final..: = R$ 1.228,56", linhas[4])
    }

    @Test
    fun extincaoArredondaParaCima() {
        val msg = Mensagens.extincao(LocalDate.of(2026, 10, 19), saldoLiquido = 5, complemento = 1, divisao = 6)
        assertEquals(
            "- EXTINÇÃO 191026 -\n\nSaldo Líquido:   R$ 0,05\nComplemento..: + R$ 0,01\nDivisão......: - R$ 0,06\nSaldo Final..: = R$ 0,00",
            msg,
        )
        assertEquals("EXTINÇÃO - 19/10/26", Mensagens.tituloExtincao(LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun lembreteDoAporte() {
        assertEquals(
            "🏆 Bolão dos Amigos - Lembrete Automático\n" +
                "💸 Valor: R$ 10,00\n" +
                "🔑 Chave Pix: fulano@exemplo.com\n" +
                "ℹ️ Obs.: Não é necessário enviar o comprovante.",
            Mensagens.lembreteAporte("Bolão dos Amigos", 1000, "fulano@exemplo.com"),
        )
    }

    @Test
    fun premioPorExtenso() {
        assertEquals("R$ 170 Milhões", Dinheiro.porExtenso(17_000_000_000))
        assertEquals("R$ 1,7 Milhão", Dinheiro.porExtenso(170_000_000))
        assertEquals("R$ 2 Milhões", Dinheiro.porExtenso(200_000_000))
        assertEquals("R$ 11,6 Milhões", Dinheiro.porExtenso(1_160_000_000))
        assertEquals("R$ 450 Mil", Dinheiro.porExtenso(45_000_000))
        assertEquals("R$ 1 Bilhão", Dinheiro.porExtenso(100_000_000_000))
        assertEquals("10 milhões", Dinheiro.porExtenso(1_000_000_000, comRs = false, maiuscula = false))
        assertEquals("R$ 850,00", Dinheiro.porExtenso(85_000))
    }
}

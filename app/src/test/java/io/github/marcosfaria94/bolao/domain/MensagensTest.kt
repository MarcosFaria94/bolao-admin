package io.github.marcosfaria94.bolao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Confere, caractere por caractere, os títulos e os blocos gerados contra os exemplos do termo. */
class MensagensTest {

    private val d = { dia: Int -> LocalDate.of(2026, 6, dia) }

    @Test
    fun saldoAtualIgualAoTermo() {
        assertEquals(ModelosDoTermo.SALDO, Mensagens.saldo(1002, "Fulano de Tal"))
        assertEquals("CAIXA DO BOLÃO: R$ 1.234,56", Mensagens.saldo(123456, "Fulano de Tal").lines()[0])
    }

    @Test
    fun aporteIgualAoTermo() {
        assertEquals(ModelosDoTermo.TITULO_APORTE, Mensagens.tituloAporte(d(1)))
        assertEquals(ModelosDoTermo.APORTE, Mensagens.aporte(d(1), saldoAtual = 1002, aportes = 3000))
        val adicional = Mensagens.aporte(d(15), saldoAtual = 1002, aportes = 1500, adicional = true)
        assertEquals("TIPO DE APORTE: ADICIONAL", adicional.lines()[0])
        assertEquals("DATA: 15/06/2026", adicional.lines()[1])
    }

    @Test
    fun apostaIgualAoTermo() {
        assertEquals(ModelosDoTermo.TITULO_APOSTA, Mensagens.tituloAposta(1, d(1)))
        // No termo, "NÚMEROS: ..." está no lugar dos números.
        val bloco = Mensagens.aposta(
            1, d(1), Modalidade.LOTOFACIL, 3060, 200_000_000, d(3), Mensagens.Jogo(), saldoAtual = 4002, valor = 350,
        )
        assertEquals(ModelosDoTermo.APOSTA, bloco)
        val comNumeros = Mensagens.aposta(
            1, d(1), Modalidade.LOTOFACIL, 3060, 200_000_000, d(3),
            Mensagens.Jogo(numeros = listOf(25, 1, 2, 3, 5, 8, 10, 11, 13, 14, 16, 18, 20, 21, 23)), saldoAtual = 4002, valor = 350,
        )
        assertEquals(
            listOf("NÚMEROS: 01 02 03 05 08 10 11 13 14 16 18 20 21", "         23 25"),
            comNumeros.lines().subList(6, 8),
        )
        assertTrue(comNumeros.lines().all { it.length <= Mensagens.COLUNAS })
    }

    @Test
    fun linhasDeCadaModalidade() {
        fun linhas(m: Modalidade, j: Mensagens.Jogo) = Mensagens.linhasDoJogo(m, j)
        assertEquals("NÚMEROS: 05 12 23 34 45 56", linhas(Modalidade.MEGA_SENA, Mensagens.Jogo(listOf(56, 5, 12, 23, 34, 45))))
        assertEquals(
            "NÚMEROS: 03 15 22 31 40 49\nTREVOS: 2 5",
            linhas(Modalidade.MAIS_MILIONARIA, Mensagens.Jogo(listOf(3, 15, 22, 31, 40, 49), trevos = listOf(5, 2))),
        )
        assertEquals(
            "NÚMEROS: 02 09 14 27 33 41 52 60 71 80\nTIME DO CORAÇÃO: FLAMENGO/RJ",
            linhas(Modalidade.TIMEMANIA, Mensagens.Jogo(listOf(2, 9, 14, 27, 33, 41, 52, 60, 71, 80), extra = "Flamengo/RJ")),
        )
        assertEquals(
            "NÚMEROS: 01 05 09 13 20 27 31\nMÊS DE SORTE: MARÇO",
            linhas(Modalidade.DIA_DE_SORTE, Mensagens.Jogo(listOf(1, 5, 9, 13, 20, 27, 31), extra = "Março")),
        )
        assertEquals("NÚMEROS: 3 0 7 1 9 4 2", linhas(Modalidade.SUPER_SETE, Mensagens.Jogo(listOf(3, 0, 7, 1, 9, 4, 2))))
        assertEquals(
            "PALPITES: 1 X 2 1 1 X 2 2 1 X 1 1 2 X",
            linhas(Modalidade.LOTECA, Mensagens.Jogo(palpitesLoteca = "1X211X221X112X".map { it.toString() })),
        )
        assertEquals("BILHETE: 012345\nFRAÇÕES: 4 DE 10", linhas(Modalidade.FEDERAL, Mensagens.Jogo(bilheteFederal = "012345", fracoesFederal = 4)))
        // Lotomania: 50 números em 4 linhas, alinhados embaixo do primeiro.
        val mania = linhas(Modalidade.LOTOMANIA, Mensagens.Jogo((0..98 step 2).toList())).lines()
        assertEquals(4, mania.size)
        assertTrue(mania[0].startsWith("NÚMEROS: 00 02 04"))
        assertTrue(mania.drop(1).all { it.startsWith("         ") && it.length <= Mensagens.COLUNAS })
    }

    @Test
    fun apuracaoIgualAoTermo() {
        assertEquals(ModelosDoTermo.TITULO_APURACAO, Mensagens.tituloApuracao(1, d(4)))
        assertEquals(
            ModelosDoTermo.APURACAO,
            Mensagens.apuracao(1, d(1), Modalidade.LOTOFACIL, 3060, 200_000_000, d(4), saldoAtual = 3652, premio = 1400),
        )
        val semPremio = Mensagens.apuracao(1, d(1), Modalidade.QUINA, 7146, null, d(4), saldoAtual = 3652, premio = 0, resultado = "1/5")
        assertTrue(semPremio.lines().contains("PRÊMIO PRINCIPAL: A CONFIRMAR"))
        assertTrue(semPremio.lines().contains("RESULTADO: 1/5"))
        assertEquals("PRÊMIO.....: + R$  0,00", semPremio.lines().last { it.startsWith("PRÊMIO.") })
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
        assertEquals(ModelosDoTermo.RATEIO, Mensagens.rateio(r.saldoLiquido, r.total))
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
    fun extratoSoComAporteAlinhaPeloMaiorRotulo() {
        val movs = listOf(Mensagens.Movimento(d(1), Mensagens.Categoria.APORTE, Mensagens.descricaoAporte(), 3000))
        val linhas = Mensagens.extrato(YearMonth.of(2026, 6), 0, 0, movs).lines()
        assertEquals("00  01/06/26 SALDO LÍQUIDO:   R$ 0,00", linhas[linhas.size - 2])
        assertEquals("01  01/06/26 APORTE.......: + R$30,00", linhas.last())
    }

    @Test
    fun valoresMaioresContinuamAlinhados() {
        val msg = Mensagens.aporte(d(1), saldoAtual = 123456, aportes = 600)
        val linhas = msg.lines()
        assertEquals("FINANCEIRO -", linhas[3])
        assertEquals("SALDO ATUAL:   R$ 1.234,56", linhas[4])
        assertEquals("APORTES....: + R$     6,00", linhas[5])
        assertEquals("SALDO FINAL: = R$ 1.240,56", linhas[6])
    }

    @Test
    fun premioPrincipal() {
        assertEquals("2 MILHÕES", Mensagens.premioPrincipal(200_000_000))
        assertEquals("1,7 MILHÃO", Mensagens.premioPrincipal(170_000_000))
        assertEquals("450 MIL", Mensagens.premioPrincipal(45_000_000))
        assertEquals("A CONFIRMAR", Mensagens.premioPrincipal(null))
        assertEquals("Nº12 - OUT/2026", Mensagens.numeroNoMes(12, LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun extincaoArredondaParaCima() {
        val msg = Mensagens.extincao(saldoAtual = 5, complemento = 1, divisao = 6)
        assertEquals(
            "FINANCEIRO -\nSALDO ATUAL:   R$ 0,05\nCOMPLEMENTO: + R$ 0,01\nDIVISÃO....: - R$ 0,06\nSALDO FINAL: = R$ 0,00",
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

    @Test
    fun premioDaTarja() {
        // Tarja das apostas do mês: o valor inteiro, no formato "R$ 999.999.999,99".
        assertEquals("R$ 52.000.000,00", Dinheiro.rs(5_200_000_000))
        assertEquals("R$ 185.000.000,00", Dinheiro.rs(18_500_000_000))
        assertEquals("R$ 450.000,00", Dinheiro.rs(45_000_000))
    }

    @Test
    fun datasDasTelas() {
        val hoje = LocalDate.of(2026, 10, 20)
        assertEquals("Ontem", Datas.relativa(hoje.minusDays(1), hoje))
        assertEquals("Hoje", Datas.relativa(hoje, hoje))
        assertEquals("Amanhã", Datas.relativa(hoje.plusDays(1), hoje))
        assertEquals("22/10/2026", Datas.relativa(hoje.plusDays(2), hoje))
        assertEquals("18/10/2026", Datas.relativa(hoje.minusDays(2), hoje))
        assertEquals("hoje", Datas.quando(hoje, hoje))
        assertEquals("em 01/11/2026", Datas.quando(LocalDate.of(2026, 11, 1), hoje))
        assertEquals("Sorteio hoje", Datas.sorteioEm(hoje, hoje))
        assertEquals("Sorteio amanhã", Datas.sorteioEm(hoje.plusDays(1), hoje))
        assertEquals("Sorteio em 3 dias", Datas.sorteioEm(hoje.plusDays(3), hoje))
        // Virada do ano.
        assertEquals("Amanhã", Datas.relativa(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun codigoDeAutenticacaoCurto() {
        val codigo = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
        // Os 15 primeiros dígitos hexadecimais (0x0123456789ABCDE) e os 8 últimos algarismos deles.
        val esperado = (0x0123456789ABCDEL % 100_000_000L).toString().padStart(8, '0')
        assertEquals(esperado, Autenticacao.curto(codigo))
        assertEquals(esperado.take(4) + "-" + esperado.drop(4), Autenticacao.formatar(codigo))
        assertEquals(9, Autenticacao.formatar(Autenticacao.sha256("qualquer coisa")).length)
        assertTrue(Autenticacao.formatar(Autenticacao.sha256("x")).matches(Regex("\\d{4}-\\d{4}")))
    }
}

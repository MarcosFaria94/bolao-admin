package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Extrato
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.data.Rateio
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.ui.aposta.Saida
import io.github.marcosfaria94.bolao.ui.aposta.ficaEmAndamento
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.aposta.rascunhoDa
import io.github.marcosfaria94.bolao.ui.aposta.saidaDaTela
import io.github.marcosfaria94.bolao.ui.aposta.situacaoCurta
import io.github.marcosfaria94.bolao.ui.aposta.situacaoDe
import io.github.marcosfaria94.bolao.ui.aposta.tituloDaAposta
import io.github.marcosfaria94.bolao.ui.comum.FiltroHistorico
import io.github.marcosfaria94.bolao.ui.comum.destinoDoMovimento
import io.github.marcosfaria94.bolao.ui.comum.douradoNoHistorico
import io.github.marcosfaria94.bolao.ui.comum.movimentosDoHistorico
import io.github.marcosfaria94.bolao.ui.comum.nomeDoMovimento
import io.github.marcosfaria94.bolao.ui.comum.tarjaDoMovimento
import io.github.marcosfaria94.bolao.ui.comum.valorDoMovimento
import io.github.marcosfaria94.bolao.ui.jogos.indicesDoCartao
import io.github.marcosfaria94.bolao.ui.jogos.linhaIndices
import io.github.marcosfaria94.bolao.ui.jogos.precoDoCartao
import io.github.marcosfaria94.bolao.ui.jogos.premioDoCartao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Textos das telas que não dependem do Android (títulos, legendas dos índices, aposta nova). */
class TelasTest {

    private val hoje = LocalDate.of(2026, 10, 21)

    private fun aposta(m: Modalidade, numero: Int, concurso: Int, registrada: Boolean = true, premio: Long? = null) = Aposta(
        id = "a$numero", numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje,
        valor = 350, registrada = registrada, criadaEm = 0, premioEstimado = premio,
    )

    @Test
    fun tituloESituacaoDaAposta() {
        assertEquals("Aposta Nº8", tituloDaAposta(aposta(Modalidade.MEGA_SENA, 8, 3073)))
        val a = aposta(Modalidade.QUINA, 1, 7148)
        fun curta(x: Aposta) = situacaoCurta(x, situacaoDe(x, hoje), hoje)
        // Antes do sorteio: quando ele acontece.
        assertEquals("Sorteio hoje", curta(a.copy(dataSorteio = hoje)))
        assertEquals("Sorteio amanhã", curta(a.copy(dataSorteio = hoje.plusDays(1))))
        assertEquals("Sorteio daqui 3 dias", curta(a.copy(dataSorteio = hoje.plusDays(3))))
        assertEquals("Aguardando sorteio", curta(a))
        // Depois: o resultado (sem o valor do prêmio).
        assertEquals("Em apuração", curta(a.copy(dataSorteio = hoje.minusDays(1))))
        val apuracao = Apuracao(acertos = "2/5", premio = 0, conferidaEm = 1, confirmadaEm = 2)
        assertEquals("Não premiada", curta(a.copy(dataSorteio = hoje.minusDays(1), apuracao = apuracao)))
        assertEquals("Premiada", curta(a.copy(dataSorteio = hoje.minusDays(1), apuracao = apuracao.copy(premio = 700))))
        assertEquals("Premiada", curta(a.copy(dataSorteio = hoje.minusDays(1), apuracao = apuracao.copy(premio = 700, confirmadaEm = null))))
    }

    /** Um mês com tudo: saldo inicial, os dois aportes, uma aposta premiada com o prêmio, um rateio e o extrato. */
    private fun mesCompleto(): Estado {
        val apurada = Apuracao(acertos = "3/5", premio = 700, conferidaEm = 1, confirmadaEm = 2)
        val a = aposta(Modalidade.QUINA, 1, 7148, premio = 5_500_000_000).copy(dataSorteio = hoje.withDayOfMonth(1), apuracao = apurada)
        val b = aposta(Modalidade.LOTOFACIL, 2, 3500).copy(id = "b", dataSorteio = hoje.plusDays(1))
        val pessoas = listOf(Pessoa("p1", "A", "A", "x", true), Pessoa("p2", "B", "B", "y"), Pessoa("p3", "C", "C", "z"))
        val mes = YearMonth.from(hoje).minusMonths(1)
        val img = Arquivo("x.png", "x.png", "image/png", 0)
        return Estado(
            pessoas = pessoas,
            apostas = listOf(a, b),
            lancamentos = listOf(
                Lancamento("l0", mes.atDay(1), 0, TipoLancamento.SALDO_INICIAL, 1250),
                Lancamento("r0", mes.atEndOfMonth(), 9, TipoLancamento.RENDIMENTO, 3),
                Lancamento("l1", hoje.withDayOfMonth(1), 10, TipoLancamento.APORTE, 15000),
                Lancamento("l2", hoje, 20, TipoLancamento.APORTE, 900, eventual = true),
                Lancamento("l3", hoje.withDayOfMonth(1), 11, TipoLancamento.APOSTA, -300, apostaId = a.id),
                Lancamento("l4", hoje.withDayOfMonth(1), 12, TipoLancamento.PREMIO, 700, apostaId = a.id),
                Lancamento("l5", hoje, 30, TipoLancamento.RATEIO, -15048, rateioNumero = 7),
                Lancamento("l6", hoje, 40, TipoLancamento.APOSTA, -350, apostaId = b.id),
            ),
            rateios = listOf(Rateio(7, hoje, 0, 0, 5016, criadoEm = 30, numeroNoMes = 1)),
            extratos = listOf(Extrato(mes.toString(), 1253, 3, criadoEm = 9)),
            comprovantes = listOf(
                Comprovante("cx", TipoComprovante.EXTRATO, "EXTRATO", "", mes.atEndOfMonth(), 9, img, autenticacao = "", mes = mes.toString()),
            ),
        )
    }

    @Test
    fun historicoDaHome() {
        val e = mesCompleto()
        val mes = YearMonth.from(hoje)
        // Tudo, no mês atual: o mais recente primeiro (no mesmo dia, o último registrado em cima).
        val tudo = movimentosDoHistorico(e, FiltroHistorico.TUDO, mes)
        assertEquals(
            listOf(
                "Aposta Nº2 · Sorteio amanhã", "Rateio Nº1", "Aporte Nº2 · Adicional",
                "Prêmio Nº1", "Aposta Nº1 · Premiada", "Aporte Nº1 · Mensal",
            ),
            tudo.map { tarjaDoMovimento(e, it, hoje) },
        )
        assertEquals(
            listOf("- R$ 3,50", "- R$ 150,48", "+ R$ 9,00", "+ R$ 7,00", "- R$ 3,00", "+ R$ 150,00"),
            tudo.map { valorDoMovimento(it) },
        )
        // Só a tarja da aposta premiada fica dourada (o prêmio, não); o nome da modalidade vai embaixo da bola.
        assertEquals(listOf(false, false, false, false, true, false), tudo.map { douradoNoHistorico(e, it, hoje) })
        assertEquals(listOf("Lotofácil", null, null, null, "Quina", null), tudo.map { nomeDoMovimento(e, it) })
        assertEquals("aposta/a1", destinoDoMovimento(e, tudo[4]))
        assertEquals("aposta/a1", destinoDoMovimento(e, tudo[3]))

        // O tipo e o mês não mexem um no outro.
        assertEquals(listOf("Aposta Nº2 · Sorteio amanhã", "Aposta Nº1 · Premiada"), movimentosDoHistorico(e, FiltroHistorico.APOSTAS, mes).map { tarjaDoMovimento(e, it, hoje) })
        assertEquals(2, movimentosDoHistorico(e, FiltroHistorico.APORTES, mes).size)
        assertEquals(1, movimentosDoHistorico(e, FiltroHistorico.RATEIOS, mes).size)
        assertEquals(1, movimentosDoHistorico(e, FiltroHistorico.PREMIOS, mes).size)
        assertTrue(movimentosDoHistorico(e, FiltroHistorico.EXTRATO, mes).isEmpty())

        // Extrato do mês anterior: um item só, com o rendimento (o lançamento do rendimento não se repete).
        val extratos = movimentosDoHistorico(e, FiltroHistorico.EXTRATO, null)
        assertEquals(listOf("Extrato · Set/2026"), extratos.map { tarjaDoMovimento(e, it, hoje) })
        assertEquals("+ R$ 0,03", valorDoMovimento(extratos.single()))
        assertEquals("comprovante/cx", destinoDoMovimento(e, extratos.single()))
        // Mês "Tudo": os dois meses, o saldo inicial sem sinal (só aparece em "Tudo").
        val todos = movimentosDoHistorico(e, FiltroHistorico.TUDO, null)
        assertEquals(8, todos.size)
        assertEquals("Saldo inicial", tarjaDoMovimento(e, todos.last(), hoje))
        assertEquals("R$ 12,50", valorDoMovimento(todos.last()))
    }

    @Test
    fun linhaDosIndices() {
        val k = ResultadoK(
            numero = 7148, especial = false, acumuladoPrincipal = 0.0, reserva = 0, herancaFaixas = emptyMap(),
            nAnterior = 0.0, k1 = 0.6894, k2 = 0.0912,
        )
        val quina = Indices.Item(Modalidade.QUINA, null, k, 34.2)
        assertEquals(listOf("k1" to "0,689", "k2" to "0,091", "odds" to "1:34"), indicesDoCartao(quina))
        assertEquals("k1  0,689    k2  0,091    odds  1:34", linhaIndices(quina))
        assertEquals("k1  —    k2  —    odds  1:34", linhaIndices(Indices.Item(Modalidade.QUINA, null, null, 34.2)))
        assertNull(indicesDoCartao(Indices.Item(Modalidade.LOTECA, null, null, null)))
        assertEquals("Sem referência", linhaIndices(Indices.Item(Modalidade.LOTECA, null, null, null)))
        assertEquals("Sem referência", linhaIndices(Indices.Item(Modalidade.FEDERAL, null, null, null)))
    }

    @Test
    fun premioDoCartaoDosJogos() {
        val ultimo = Concurso(
            modalidade = Modalidade.QUINA, numero = 7147, data = hoje.minusDays(1), dezenas = emptyList(),
            proximoNumero = 7148, proximaData = hoje, premioEstimadoProximo = 5_850_000_000,
        )
        // Sem "Prêmio:" nem "R$": só o valor por extenso.
        assertEquals("58,5 Milhões", premioDoCartao(Indices.Item(Modalidade.QUINA, ultimo, null, 34.2)))
        assertEquals("230 Milhões", premioDoCartao(Indices.Item(Modalidade.MEGA_SENA, ultimo.copy(premioEstimadoProximo = 23_000_000_000), null, 2298.0)))
        assertEquals("500 Mil", premioDoCartao(Indices.Item(Modalidade.FEDERAL, ultimo.copy(premioEstimadoProximo = 50_000_000), null, null)))
        assertEquals("Prêmio a confirmar", premioDoCartao(Indices.Item(Modalidade.QUINA, null, null, 34.2)))
        // Preço de Ajustes diferente do dos parâmetros: aviso.
        assertTrue(Indices.Item(Modalidade.MEGA_SENA, null, null, null, precoAjustes = 700, precoParametros = 600).precoDiferente)
        assertTrue(!Indices.Item(Modalidade.MEGA_SENA, null, null, null, precoAjustes = 600, precoParametros = 600).precoDiferente)
        assertTrue(!Indices.Item(Modalidade.FEDERAL, null, null, null, precoAjustes = null, precoParametros = null).precoDiferente)
    }

    @Test
    fun apostaNovaComOVolanteVazio() {
        val e = Estado(config = Config(precos = mapOf(Modalidade.QUINA.name to 320)))
        val ultimo = Concurso(
            modalidade = Modalidade.QUINA, numero = 7147, data = hoje.minusDays(1), dezenas = emptyList(),
            proximoNumero = 7148, proximaData = hoje, premioEstimadoProximo = 60_000_000,
        )
        val a = novaAposta(e, Modalidade.QUINA, ultimo, hoje, agora = 5)
        assertTrue(a.numeros.isEmpty())
        assertTrue(a.trevos.isEmpty() && a.extra == null)
        assertEquals(7148, a.concurso)
        assertEquals(hoje, a.dataSorteio)
        assertEquals(60_000_000L, a.premioEstimado)
        assertEquals(320L, a.valor)
        assertEquals(false, a.registrada)

        // Super Sete: as 7 colunas sem número; concurso já sorteado: a confirmar.
        val s = novaAposta(e, Modalidade.SUPER_SETE, ultimo.copy(modalidade = Modalidade.SUPER_SETE, proximaData = hoje.minusDays(1)), hoje, 5)
        assertEquals(List(7) { -1 }, s.numeros)
        assertEquals(0, s.concurso)
        assertEquals(10, novaAposta(e, Modalidade.FEDERAL, null, hoje, 5).fracoesFederal)
    }

    @Test
    fun volanteAbreOAndamentoOuUmaNova() {
        val rascunho = aposta(Modalidade.QUINA, 0, 7148, registrada = false)
        val e = Estado(apostas = listOf(rascunho, aposta(Modalidade.MEGA_SENA, 1, 3000)))
        assertEquals(rascunho, rascunhoDa(e, Modalidade.QUINA))
        // A registrada não volta ao volante: a página do jogo começa uma aposta nova.
        assertNull(rascunhoDa(e, Modalidade.MEGA_SENA))
    }

    @Test
    fun precoEmbaixoDoNome() {
        val e = Estado(config = Config(precos = mapOf(Modalidade.QUINA.name to 320)))
        assertEquals("R$ 3,20", precoDoCartao(Modalidade.QUINA, e))
        assertEquals("R$ 6,00", precoDoCartao(Modalidade.MEGA_SENA, e))
        assertEquals("Variável", precoDoCartao(Modalidade.FEDERAL, e))
    }

    @Test
    fun rascunhoSoFicaComOVolanteCompletoOuOComprovante() {
        val comprovante = Arquivo("c.pdf", "c.pdf", "application/pdf", 0)
        val quina = aposta(Modalidade.QUINA, 0, 7148, registrada = false)
        assertTrue(ficaEmAndamento(quina.copy(numeros = listOf(1, 2, 3, 4, 5))))
        assertFalse(ficaEmAndamento(quina.copy(numeros = listOf(1, 2, 3))))
        assertFalse(ficaEmAndamento(quina))
        // Com o comprovante da Caixa, fica mesmo faltando dezena.
        assertTrue(ficaEmAndamento(quina.copy(numeros = listOf(1, 2), comprovante = comprovante)))
        // Timemania: as 10 dezenas e o Time do Coração.
        val time = aposta(Modalidade.TIMEMANIA, 0, 2300, registrada = false).copy(numeros = (1..10).toList())
        assertFalse(ficaEmAndamento(time))
        assertTrue(ficaEmAndamento(time.copy(extra = "FLAMENGO/RJ")))
        // Super Sete: um número em cada uma das 7 colunas.
        val sete = aposta(Modalidade.SUPER_SETE, 0, 750, registrada = false)
        assertFalse(ficaEmAndamento(sete.copy(numeros = List(7) { -1 })))
        assertTrue(ficaEmAndamento(sete.copy(numeros = listOf(1, 2, 3, 4, 5, 6, 7))))
        // Federal: o bilhete e as frações.
        val federal = aposta(Modalidade.FEDERAL, 0, 6000, registrada = false).copy(fracoesFederal = 10)
        assertFalse(ficaEmAndamento(federal))
        assertTrue(ficaEmAndamento(federal.copy(bilheteFederal = "12345")))
    }

    @Test
    fun saidaDaTelaDaAposta() {
        val comprovante = Arquivo("c.pdf", "c.pdf", "application/pdf", 0)
        val vazia = aposta(Modalidade.QUINA, 0, 7148, registrada = false)
        val completa = vazia.copy(numeros = listOf(1, 2, 3, 4, 5))
        val metade = vazia.copy(numeros = listOf(1, 2))
        val descartada = Saida.Descartada("Aposta descartada: o volante ficou incompleto e sem o comprovante da Caixa.")

        // Aposta nova.
        assertEquals(Saida.EmAndamento("Aposta em andamento: falta anexar o comprovante da Caixa."), saidaDaTela(completa, null, nova = true))
        assertEquals(Saida.EmAndamento("Aposta em andamento: pronta para registrar."), saidaDaTela(completa.copy(comprovante = comprovante), completa, nova = true))
        assertEquals(Saida.EmAndamento("Aposta em andamento: falta completar o volante."), saidaDaTela(metade.copy(comprovante = comprovante), null, nova = true))
        assertEquals(descartada, saidaDaTela(metade, null, nova = true))
        // Nada escolhido: descarta sem aviso.
        assertEquals(Saida.Descartada(null), saidaDaTela(vazia, null, nova = true))

        // Aposta que já estava em andamento.
        assertEquals(Saida.EmAndamento(null), saidaDaTela(completa, completa, nova = false))
        assertEquals(descartada, saidaDaTela(metade, completa, nova = false))
        assertEquals(descartada, saidaDaTela(vazia, completa, nova = false))

        // Registrada: nada a fazer.
        assertEquals(Saida.Nada, saidaDaTela(metade, completa.copy(registrada = true), nova = true))

        // Federal fala do bilhete.
        val federal = aposta(Modalidade.FEDERAL, 0, 6000, registrada = false).copy(bilheteFederal = "123", fracoesFederal = null)
        assertEquals(
            Saida.Descartada("Aposta descartada: o bilhete ficou incompleto e sem o comprovante da Caixa."),
            saidaDaTela(federal, null, nova = true),
        )
    }
}

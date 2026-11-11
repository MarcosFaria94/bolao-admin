package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.IndicesDaAposta
import io.github.marcosfaria94.bolao.data.Sessao
import io.github.marcosfaria94.bolao.data.Sessoes
import io.github.marcosfaria94.bolao.ui.sessoes.resumoDaSessao
import io.github.marcosfaria94.bolao.ui.sessoes.tipoDaSessao
import io.github.marcosfaria94.bolao.ui.sessoes.linhasDaSessao
import io.github.marcosfaria94.bolao.ui.sessoes.sessoesCriadas
import io.github.marcosfaria94.bolao.data.Operacoes
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
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.comum.FiltroHome
import io.github.marcosfaria94.bolao.ui.comum.tiposDoFiltro
import io.github.marcosfaria94.bolao.ui.home.jogosEmAndamento
import io.github.marcosfaria94.bolao.ui.home.quantosEmAndamento
import io.github.marcosfaria94.bolao.ui.home.pendenciaDoJogo
import io.github.marcosfaria94.bolao.ui.home.resumoDoJogo
import io.github.marcosfaria94.bolao.data.Envio
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.ui.comum.destinoDoMovimento
import io.github.marcosfaria94.bolao.ui.comum.douradoNoHistorico
import io.github.marcosfaria94.bolao.ui.comum.movimentosDoHistorico
import io.github.marcosfaria94.bolao.ui.comum.Etiqueta
import io.github.marcosfaria94.bolao.ui.comum.TomEtiqueta
import io.github.marcosfaria94.bolao.ui.comum.etiquetaDoMovimento
import io.github.marcosfaria94.bolao.ui.comum.horaDoMovimento
import io.github.marcosfaria94.bolao.ui.comum.tituloDoMovimento
import io.github.marcosfaria94.bolao.ui.comum.valorDoMovimento
import io.github.marcosfaria94.bolao.ui.jogos.indicesDoCartao
import io.github.marcosfaria94.bolao.ui.jogos.linhaIndices
import io.github.marcosfaria94.bolao.ui.jogos.precoDoCartao
import io.github.marcosfaria94.bolao.ui.jogos.premioDoCartao
import io.github.marcosfaria94.bolao.domain.TIMES
import io.github.marcosfaria94.bolao.domain.TIMES_DO_CORACAO
import io.github.marcosfaria94.bolao.ui.ajustes.SecaoAjuste
import io.github.marcosfaria94.bolao.ui.aposta.OrdemDosTimes
import io.github.marcosfaria94.bolao.ui.aposta.nomeDoTime
import io.github.marcosfaria94.bolao.ui.aposta.timesEmOrdem
import io.github.marcosfaria94.bolao.ui.aposta.timeAleatorio
import io.github.marcosfaria94.bolao.ui.aposta.mesAleatorio
import io.github.marcosfaria94.bolao.ui.aposta.mesDaLista
import io.github.marcosfaria94.bolao.ui.aposta.siglaDoMes
import io.github.marcosfaria94.bolao.ui.aposta.valorSemQuebra
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.imagem.ImagemDosJogos
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.ui.home.emOrdemDeSorteioEK1
import io.github.marcosfaria94.bolao.ui.painel.TipoDoResumo
import io.github.marcosfaria94.bolao.ui.painel.indicesDoConcurso
import io.github.marcosfaria94.bolao.ui.painel.quantasPendencias
import io.github.marcosfaria94.bolao.ui.painel.resumoDasPendencias
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.ui.ajustes.resumoDosParametros
import io.github.marcosfaria94.bolao.ui.ajustes.textoDoCalendario
import io.github.marcosfaria94.bolao.ui.aposta.mensagemDoRegistro
import io.github.marcosfaria94.bolao.ui.aposta.textoDoVolante
import io.github.marcosfaria94.bolao.ui.aposta.textoRegistrar
import io.github.marcosfaria94.bolao.ui.aposta.proximaAposta
import io.github.marcosfaria94.bolao.ui.aposta.textoFaltaComprovante
import io.github.marcosfaria94.bolao.ui.comprovantes.textoDescartarTudo
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.home.textoDaProximaAposta
import io.github.marcosfaria94.bolao.ui.jogos.bloqueioDaAposta
import kotlin.random.Random
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
                "Aposta Nº2 · Lotofácil", "Rateio Nº1", "Aporte Nº2",
                "Prêmio Nº1 · Quina", "Aposta Nº1 · Quina", "Aporte Nº1",
            ),
            tudo.map { tituloDoMovimento(e, it) },
        )
        // A etiqueta da situação (embaixo do título, com a hora do registro).
        assertEquals(
            listOf(
                Etiqueta("Sorteio amanhã", TomEtiqueta.AZUL), null, Etiqueta("Adicional", TomEtiqueta.NEUTRO),
                // O prêmio diz "Resgatado" (sem o ouro); a aposta premiada, "Premiada" em ouro.
                Etiqueta("Resgatado", TomEtiqueta.NEUTRO), Etiqueta("Premiada", TomEtiqueta.OURO), Etiqueta("Mensal", TomEtiqueta.NEUTRO),
            ),
            tudo.map { etiquetaDoMovimento(e, it, hoje) },
        )
        assertEquals("00:00", horaDoMovimento(tudo[0], java.time.ZoneOffset.UTC))
        // Aposta esperando o comprovante da Caixa e rateio esperando os Pix.
        val esperando = e.copy(
            apostas = e.apostas.map { if (it.id == "b") it.copy(pendenteDeAnexo = true) else it },
            rateios = e.rateios.map { it.copy(pendenteDeAnexo = true) },
        )
        val tudoEsperando = movimentosDoHistorico(esperando, FiltroHistorico.TUDO, mes)
        assertEquals(Etiqueta("Sem comprovante", TomEtiqueta.VERMELHO), etiquetaDoMovimento(esperando, tudoEsperando[0], hoje))
        assertEquals(Etiqueta("Esperando os Pix", TomEtiqueta.VERMELHO), etiquetaDoMovimento(esperando, tudoEsperando[1], hoje))
        assertEquals(
            listOf("- R$ 3,50", "- R$ 150,48", "+ R$ 9,00", "+ R$ 7,00", "- R$ 3,00", "+ R$ 150,00"),
            tudo.map { valorDoMovimento(it) },
        )
        // Só o prêmio fica dourado (a aposta premiada, não).
        assertEquals(listOf(false, false, false, true, false, false), tudo.map { douradoNoHistorico(e, it, hoje) })
        assertEquals("aposta/a1", destinoDoMovimento(e, tudo[4]))
        assertEquals("aposta/a1", destinoDoMovimento(e, tudo[3]))

        // O tipo e o mês não mexem um no outro.
        assertEquals(listOf("Aposta Nº2 · Lotofácil", "Aposta Nº1 · Quina"), movimentosDoHistorico(e, FiltroHistorico.APOSTAS, mes).map { tituloDoMovimento(e, it) })
        assertEquals(2, movimentosDoHistorico(e, FiltroHistorico.APORTES, mes).size)
        assertEquals(1, movimentosDoHistorico(e, FiltroHistorico.RATEIOS, mes).size)
        assertEquals(1, movimentosDoHistorico(e, FiltroHistorico.PREMIOS, mes).size)
        assertTrue(movimentosDoHistorico(e, FiltroHistorico.EXTRATO, mes).isEmpty())

        // Extrato do mês anterior: um item só, com o rendimento (o lançamento do rendimento não se repete).
        val extratos = movimentosDoHistorico(e, FiltroHistorico.EXTRATO, null)
        assertEquals(listOf("Extrato · Set/2026"), extratos.map { tituloDoMovimento(e, it) })
        assertEquals("+ R$ 0,03", valorDoMovimento(extratos.single()))
        assertEquals("comprovante/cx", destinoDoMovimento(e, extratos.single()))
        // Mês "Tudo": os dois meses, o saldo inicial sem sinal (só aparece em "Tudo").
        val todos = movimentosDoHistorico(e, FiltroHistorico.TUDO, null)
        assertEquals(8, todos.size)
        assertEquals("Saldo inicial", tituloDoMovimento(e, todos.last()))
        assertEquals("R$ 12,50", valorDoMovimento(todos.last()))
    }

    @Test
    fun filtroDoHistorico() {
        val e = mesCompleto()
        val mes = YearMonth.from(hoje)
        val anterior = mes.minusMonths(1)
        // Padrão: todos os tipos no mês atual.
        val padrao = FiltroHome.padrao(hoje)
        assertTrue(padrao.ehPadrao(hoje))
        assertEquals("${Datas.mesAnoCurto(mes)} · Tudo", padrao.resumo())
        assertEquals(6, movimentosDoHistorico(e, padrao).size)
        // Vários tipos e vários meses de uma vez.
        val tipos = tiposDoFiltro(individual = false)
        val apostasEPremios = padrao.comTipo(FiltroHistorico.APOSTAS, tipos).comTipo(FiltroHistorico.PREMIOS, tipos)
        assertEquals(
            listOf("Aposta Nº2 · Lotofácil", "Prêmio Nº1 · Quina", "Aposta Nº1 · Quina"),
            movimentosDoHistorico(e, apostasEPremios).map { tituloDoMovimento(e, it) },
        )
        assertEquals("${Datas.mesAnoCurto(mes)} · Apostas, Prêmios", apostasEPremios.resumo())
        val doisMeses = padrao.comMes(anterior).comTipo(FiltroHistorico.EXTRATO, tipos)
        assertEquals(listOf("Extrato · ${Datas.mesAnoCurto(anterior)}"), movimentosDoHistorico(e, doisMeses).map { tituloDoMovimento(e, it) })
        // Desmarcar o último mês = todos os meses; marcar todos os tipos = "Tudo".
        assertEquals(8, movimentosDoHistorico(e, padrao.comMes(mes)).size)
        val todos = tipos.fold(padrao) { f, t -> f.comTipo(t, tipos) }
        assertTrue(todos.tipos.isEmpty())
        // O modo individual só tem apostas e prêmios.
        assertEquals(listOf(FiltroHistorico.APOSTAS, FiltroHistorico.PREMIOS), tiposDoFiltro(individual = true))
    }

    @Test
    fun gruposDosJogosEmAndamento() {
        val q1 = aposta(Modalidade.QUINA, 1, 7150).copy(id = "q1", dataSorteio = hoje.plusDays(2), numeros = listOf(5, 1, 33, 47, 71))
        val q2 = q1.copy(id = "q2", numero = 2, criadaEm = 5)
        val m = aposta(Modalidade.MEGA_SENA, 3, 2950).copy(id = "m", dataSorteio = hoje)
        val l = aposta(Modalidade.LOTOFACIL, 4, 3600).copy(id = "l", dataSorteio = hoje.plusDays(2))
        val sorteada = aposta(Modalidade.QUINA, 5, 7140).copy(id = "s", dataSorteio = hoje.minusDays(1))
        val semComprovante = aposta(Modalidade.DIA_DE_SORTE, 6, 1130).copy(id = "d", dataSorteio = hoje.plusDays(1), pendenteDeAnexo = true)
        val rascunho = aposta(Modalidade.MEGA_SENA, 0, 2950).copy(id = "r", registrada = false, dataSorteio = hoje)
        val e = Estado(apostas = listOf(q1, q2, m, l, sorteada, semComprovante, rascunho))
        val grupos = jogosEmAndamento(e, hoje)
        // Agrupados por modalidade/concurso/data, o sorteio mais próximo primeiro (sem as sorteadas).
        assertEquals(
            listOf("Mega-Sena 2950", "Dia de Sorte 1130", "Lotofácil 3600", "Quina 7150"),
            grupos.map { "${it.modalidade.nome} ${it.concurso}" },
        )
        // A que falta registrar (em andamento) entra no grupo do concurso dela, depois das registradas.
        assertEquals(listOf("m", "r"), grupos.first().apostas.map { it.id })
        assertEquals(listOf("q1", "q2"), grupos.last().apostas.map { it.id })
        assertEquals(hoje.plusDays(2), grupos.last().sorteio)
        assertEquals(6, quantosEmAndamento(e, hoje))
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
        assertEquals("R$ 58,5 Milhões", premioDoCartao(Indices.Item(Modalidade.QUINA, ultimo, null, 34.2)))
        assertEquals("Não divulgado", premioDoCartao(Indices.Item(Modalidade.QUINA, null, null, 34.2)))
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
        // Logo depois do sorteio, sem a data do próximo (não divulgada): o próximo número já vale, sem a data.
        val semData = novaAposta(e, Modalidade.QUINA, ultimo.copy(proximaData = null, premioEstimadoProximo = null), hoje, 5)
        assertEquals(7148, semData.concurso)
        assertNull(semData.dataSorteio)
        assertNull(semData.premioEstimado)
        // Dados velhos (o último sorteio foi há dias) e sem a data: a confirmar.
        assertEquals(0, novaAposta(e, Modalidade.QUINA, ultimo.copy(data = hoje.minusDays(5), proximaData = null), hoje, 5).concurso)
        assertEquals(10, novaAposta(e, Modalidade.FEDERAL, null, hoje, 5).fracoesFederal)
    }

    @Test
    fun timesDoCoracaoEmLista() {
        assertEquals(80, TIMES.size)
        assertEquals(80, TIMES.map { it.caixa }.toSet().size)
        assertEquals(TIMES_DO_CORACAO, TIMES.map { it.caixa })
        assertTrue(TIMES.all { it.wiki.startsWith("pt:") || it.wiki.startsWith("en:") })
        // O nome da tela: "Vasco da Gama / RJ"; o digitado à mão fica como está.
        assertEquals("Vasco da Gama / RJ", nomeDoTime("VASCO DA GAMA/RJ"))
        assertEquals("Inter de Limeira / SP", nomeDoTime("inter limeira/sp"))
        assertEquals("OUTRO/XX", nomeDoTime("OUTRO/XX"))
        assertNull(nomeDoTime(null))
        // Ordem alfabética sem diferença de acento: Águia entre ABC e Altos; América/MG antes de América/RN.
        val porNome = timesEmOrdem(TIMES, OrdemDosTimes.NOME, crescente = true).map { "${it.nome}/${it.uf}" }
        assertEquals(listOf("ABC/RN", "Águia de Marabá/PA", "Altos/PI", "Amazonas/AM", "América/MG", "América/RN"), porNome.take(6))
        assertEquals("Ypiranga/RS", porNome.last())
        assertEquals("Ypiranga/RS", timesEmOrdem(TIMES, OrdemDosTimes.NOME, crescente = false).first().let { "${it.nome}/${it.uf}" })
        // Por UF: AL primeiro (ASA, CRB, CSA), e o nome dentro da UF.
        val porUf = timesEmOrdem(TIMES, OrdemDosTimes.UF, crescente = true).map { "${it.nome}/${it.uf}" }
        assertEquals(listOf("ASA/AL", "CRB/AL", "CSA/AL", "Amazonas/AM", "Manaus/AM"), porUf.take(5))
        assertEquals("Tocantinópolis/TO", porUf.last())
        assertEquals("TO", timesEmOrdem(TIMES, OrdemDosTimes.UF, crescente = false).first().uf)
    }

    @Test
    fun ajustesNotificacoes() {
        // A apuração automática e o toque do prêmio viraram uma tela só (os endereços antigos abrem ela).
        assertEquals(SecaoAjuste.NOTIFICACOES, SecaoAjuste.de("apuracao"))
        assertEquals(SecaoAjuste.NOTIFICACOES, SecaoAjuste.de("toque"))
        assertEquals("Notificações", SecaoAjuste.NOTIFICACOES.titulo(false))
        // Desde a 0.21 o Modo de teste saiu dos Ajustes (é uma sessão à parte, na tela das sessões).
        // Desde a 0.24 o Backup também fica no Celular.
        assertEquals(listOf(SecaoAjuste.NOTIFICACOES, SecaoAjuste.BACKUP), SecaoAjuste.grupos(false).first { it.first == "CELULAR" }.second)
        assertTrue(SecaoAjuste.grupos(true).none { SecaoAjuste.TESTE in it.second })
    }

    /** As sessões (desde a 0.21): o nome genérico, a contagem e o resumo de cada uma. */
    @Test
    fun sessoes() {
        assertEquals("Bolão 1", Sessoes.nomeGenerico(emptyList()))
        assertEquals("Bolão 2", Sessoes.nomeGenerico(listOf("Bolão 1", "Individual")))
        // O menor número livre (o "Bolão 1" foi apagado ou renomeado).
        assertEquals("Bolão 1", Sessoes.nomeGenerico(listOf("Bolão 2", "Amigos do trabalho")))
        assertEquals("Bolão 3", Sessoes.nomeGenerico(listOf("bolão 1", "BOLÃO 2")))
        assertEquals("1 sessão criada", sessoesCriadas(1))
        assertEquals("3 sessões criadas", sessoesCriadas(3))
        val bolao = Sessao("a", "Bolão 1", 0, Modo.BOLAO)
        val pessoas = listOf(Pessoa("p1", "A", "A", "x", true), Pessoa("p2", "B", "B", "y"), Pessoa("p3", "C", "C", "z"))
        val e = Estado(cadastroConcluido = true, pessoas = pessoas, lancamentos = listOf(Lancamento("l", hoje, 0, TipoLancamento.SALDO_INICIAL, 1_230)))
        assertEquals("3 integrantes · saldo R$\u00A012,30", resumoDaSessao(bolao, e))
        assertEquals("Cadastro não concluído", resumoDaSessao(Sessao("b"), Estado()))
        assertEquals("Carregando...", resumoDaSessao(bolao, null))
        val individual = Sessao("i", "Individual", 0, Modo.INDIVIDUAL)
        assertEquals("Individual · saldo R$\u00A012,30", resumoDaSessao(individual, e.copy(modo = Modo.INDIVIDUAL)))
        assertTrue(individual.individual && !bolao.individual)
        // O cartão da tela das sessões (desde a 0.24): uma informação por linha, com o tipo do bolão.
        assertEquals("Bolão formal", tipoDaSessao(bolao, e))
        assertEquals("Bolão informal", tipoDaSessao(bolao, e.copy(informal = true)))
        assertEquals("Modo individual", tipoDaSessao(individual, e.copy(modo = Modo.INDIVIDUAL)))
        assertNull(tipoDaSessao(Sessao("b"), Estado()))
        assertEquals(listOf("Bolão formal", "3 integrantes", "Saldo: R$\u00A012,30"), linhasDaSessao(bolao, e).map { it.texto })
        assertTrue(linhasDaSessao(bolao, e).first().principal)
        assertEquals(listOf("Modo individual", "Saldo: R$\u00A012,30"), linhasDaSessao(individual, e.copy(modo = Modo.INDIVIDUAL)).map { it.texto })
        assertEquals(listOf("Cadastro não concluído"), linhasDaSessao(Sessao("b"), Estado()).map { it.texto })
        assertEquals(listOf("Carregando..."), linhasDaSessao(bolao, null).map { it.texto })
    }

    @Test
    fun ajustesComExtincaoNosIntegrantesEBackupNoCelular() {
        // Desde a 0.24: a extinção do bolão fica no fim dos Integrantes abertos (fora dos grupos); o Backup,
        // no Celular. Desde a 0.23: o aporte e os preços nos Parâmetros; o termo na Segurança. Sem o grupo
        // "Sobre" (os Créditos são o rodapé da lista).
        assertEquals(listOf(SecaoAjuste.INTEGRANTES, SecaoAjuste.PARAMETROS), SecaoAjuste.grupos(false).first { it.first == "BOLÃO" }.second)
        assertEquals(listOf(SecaoAjuste.TERMO, SecaoAjuste.INTEGRIDADE, SecaoAjuste.AUDITORIA), SecaoAjuste.grupos(false).first { it.first == "SEGURANÇA" }.second)
        assertTrue(SecaoAjuste.grupos(false).none { SecaoAjuste.EXTINCAO in it.second })
        assertTrue(SecaoAjuste.grupos(false, informal = true).none { SecaoAjuste.EXTINCAO in it.second })
        // O bolão informal (desde a 0.22) não tem o termo.
        assertEquals(listOf(SecaoAjuste.INTEGRANTES, SecaoAjuste.PARAMETROS), SecaoAjuste.grupos(false, informal = true).first { it.first == "BOLÃO" }.second)
        assertEquals(listOf(SecaoAjuste.INTEGRIDADE, SecaoAjuste.AUDITORIA), SecaoAjuste.grupos(false, informal = true).first { it.first == "SEGURANÇA" }.second)
        // O individual: o "Apagar tudo" no fim do "Apostas"; sem o grupo Segurança.
        assertEquals(listOf(SecaoAjuste.MODO, SecaoAjuste.PARAMETROS, SecaoAjuste.EXTINCAO), SecaoAjuste.grupos(true).first { it.first == "APOSTAS" }.second)
        assertEquals(listOf("APOSTAS", "CELULAR"), SecaoAjuste.grupos(true).map { it.first })
        assertEquals(listOf(SecaoAjuste.NOTIFICACOES, SecaoAjuste.BACKUP), SecaoAjuste.grupos(true).first { it.first == "CELULAR" }.second)
        assertEquals(listOf("BOLÃO", "SEGURANÇA", "CELULAR"), SecaoAjuste.grupos(false).map { it.first })
        // O endereço antigo do "Aporte e preços" (avisos de antes da 0.23) abre os Parâmetros.
        assertEquals(SecaoAjuste.PARAMETROS, SecaoAjuste.de("valores"))
        assertEquals(SecaoAjuste.PARAMETROS, SecaoAjuste.de("parametros"))
        // A atualização e o modo de teste agem ali mesmo, na lista: não têm tela (os endereços antigos não abrem nada).
        assertNull(SecaoAjuste.de("teste"))
        assertNull(SecaoAjuste.de("atualizacao"))
        assertNull(SecaoAjuste.de("creditos"))
        assertFalse(SecaoAjuste.TESTE.temTela)
        assertEquals(SecaoAjuste.EXTINCAO, SecaoAjuste.de("extincao"))
    }

    @Test
    fun painelOkOuPendente() {
        val q = aposta(Modalidade.QUINA, 1, 7148).copy(numeros = listOf(71, 5, 12, 33, 47))
        val e = Estado(apostas = listOf(q))
        // Registrada e sem comprovante do bolão para enviar: OK.
        assertNull(pendenciaDoJogo(e, q))
        assertEquals("Falta registrar", pendenciaDoJogo(e, q.copy(registrada = false)))
        assertEquals("Sem comprovante", pendenciaDoJogo(e, q.copy(pendenteDeAnexo = true)))
        val pdf = Arquivo("c.pdf", "c.pdf", "application/pdf", 0)
        assertEquals("A conferir", pendenciaDoJogo(e, q.copy(pendenteDeAnexo = true, comprovante = pdf)))
        // O comprovante do bolão ainda não enviado ao grupo: pendente; enviado (ou no individual), OK.
        val c = Comprovante("c1", TipoComprovante.APOSTA, "APOSTA Nº 1", "", hoje, 1, pdf, autenticacao = "", apostaId = q.id)
        val comComprovante = e.copy(comprovantes = listOf(c))
        assertEquals("Enviar ao grupo", pendenciaDoJogo(comComprovante, q))
        assertNull(pendenciaDoJogo(comComprovante.copy(envios = listOf(Envio("c1", 2))), q))
        assertNull(pendenciaDoJogo(comComprovante.copy(modo = Modo.INDIVIDUAL), q))
        // O jogo numa linha.
        assertEquals("05 12 33 47 71", resumoDoJogo(q))
        assertEquals("50 números", resumoDoJogo(aposta(Modalidade.LOTOMANIA, 2, 2790).copy(numeros = (0 until 100 step 2).toList())))
        assertEquals("01 02 03 04 05 06 · Trevos 2 5", resumoDoJogo(aposta(Modalidade.MAIS_MILIONARIA, 3, 300).copy(numeros = (1..6).toList(), trevos = listOf(5, 2))))
        assertEquals(
            "01 02 03 04 05 06 07 08 09 10 · Vasco da Gama / RJ",
            resumoDoJogo(aposta(Modalidade.TIMEMANIA, 4, 2290).copy(numeros = (1..10).toList(), extra = "VASCO DA GAMA/RJ")),
        )
        assertEquals("03 09 14 20 25 28 31 · Março", resumoDoJogo(aposta(Modalidade.DIA_DE_SORTE, 5, 1125).copy(numeros = listOf(3, 9, 14, 20, 25, 28, 31), extra = "Março")))
    }

    @Test
    fun resumoDasPendenciasNoPainel() {
        val pessoas = listOf(Pessoa("p1", "A", "A", "x", true), Pessoa("p2", "B", "B", "y"), Pessoa("p3", "C", "C", "z"))
        val pdf = Arquivo("c.pdf", "c.pdf", "application/pdf", 0)
        val amanha = hoje.plusDays(1)
        // Uma a registrar, uma sem o comprovante, uma a conferir e uma com o comprovante para enviar; e o aporte do mês.
        val r = aposta(Modalidade.QUINA, 0, 7149, registrada = false).copy(id = "r", numeros = listOf(1, 2, 3, 4, 5), dataSorteio = amanha)
        val sem = aposta(Modalidade.MEGA_SENA, 1, 2950).copy(id = "s", pendenteDeAnexo = true, dataSorteio = amanha)
        val conferir = aposta(Modalidade.LOTOFACIL, 2, 3500).copy(id = "c", pendenteDeAnexo = true, comprovante = pdf, dataSorteio = amanha)
        val enviar = aposta(Modalidade.QUINA, 3, 7149).copy(id = "o", dataSorteio = amanha)
        val c = Comprovante("c1", TipoComprovante.APOSTA, "APOSTA Nº 3", "", hoje, 1, pdf, autenticacao = "", apostaId = "o")
        val e = Estado(
            cadastroConcluido = true, cadastradoEm = hoje.withDayOfMonth(1), pessoas = pessoas,
            apostas = listOf(r, sem, conferir, enviar), comprovantes = listOf(c),
        )
        val resumo = resumoDasPendencias(e, Pendencias.de(e, hoje.atTime(12, 0)))
        assertEquals(
            listOf(TipoDoResumo.REGISTRAR to 1, TipoDoResumo.SEM_COMPROVANTE to 1, TipoDoResumo.CONFERIR to 1, TipoDoResumo.ENVIAR to 1, TipoDoResumo.APORTE to 1),
            resumo.map { it.tipo to it.quantidade },
        )
        // Cada quadrinho abre a tela que resolve: a aposta, os Comprovantes, o comprovante (um só), o aporte.
        assertEquals(listOf("aposta/r", "comprovantes", "comprovantes", "comprovante/c1", "aporte"), resumo.map { it.destino })
        assertEquals(5, quantasPendencias(resumo))
        // Dois para enviar: os Comprovantes (que têm o "Enviar ao grupo").
        val c2 = c.copy(id = "c2", apostaId = "x")
        val dois = e.copy(comprovantes = listOf(c, c2))
        assertEquals("comprovantes", resumoDasPendencias(dois, Pendencias.de(dois, hoje.atTime(12, 0))).single { it.tipo == TipoDoResumo.ENVIAR }.destino)
        // Sem nada: o resumo vazio ("Tudo em dia").
        val emDia = Estado(cadastroConcluido = true, cadastradoEm = hoje.withDayOfMonth(1), pessoas = pessoas, modo = Modo.INDIVIDUAL)
        assertTrue(resumoDasPendencias(emDia, Pendencias.de(emDia, hoje.atTime(12, 0))).isEmpty())
    }

    @Test
    fun painelPeloSorteioEPeloK1() {
        val q = aposta(Modalidade.QUINA, 1, 7149).copy(id = "q", dataSorteio = hoje.plusDays(1))
        val m = aposta(Modalidade.MEGA_SENA, 2, 2950).copy(id = "m", dataSorteio = hoje.plusDays(1))
        val l = aposta(Modalidade.LOTOFACIL, 3, 3500).copy(id = "l", dataSorteio = hoje)
        val d = aposta(Modalidade.DIA_DE_SORTE, 4, 1125).copy(id = "d", dataSorteio = null)
        val grupos = jogosEmAndamento(Estado(apostas = listOf(q, m, l, d)), hoje)
        // O sorteio mais próximo primeiro; no mesmo dia, o maior k1; sem data, no fim.
        val k = mapOf(Modalidade.QUINA to 0.70, Modalidade.MEGA_SENA to 0.76)
        assertEquals(
            listOf("Lotofácil", "Mega-Sena", "Quina", "Dia de Sorte"),
            emOrdemDeSorteioEK1(grupos) { k[it.modalidade] }.map { it.modalidade.nome },
        )
        // Sem o k1 (a Caixa não respondeu): depois dos que têm, no mesmo dia.
        assertEquals(
            listOf("Lotofácil", "Quina", "Mega-Sena", "Dia de Sorte"),
            emOrdemDeSorteioEK1(grupos) { if (it.modalidade == Modalidade.QUINA) 0.5 else null }.map { it.modalidade.nome },
        )
        // Sem os índices guardados na aposta: o k1 (e o k2) só vale para o concurso dos índices de agora.
        val kq = ResultadoK(numero = 7149, especial = false, acumuladoPrincipal = 0.0, reserva = 0, herancaFaixas = emptyMap(), nAnterior = 0.0, k1 = 0.7, k2 = 0.1)
        val item = Indices.Item(Modalidade.QUINA, null, kq, 34.0)
        fun grupo(a: Aposta) = jogosEmAndamento(Estado(apostas = listOf(a)), hoje).single()
        val gq = grupo(q)
        assertEquals(0.7, indicesDoConcurso(item, gq)!!.k1!!, 1e-9)
        assertNull(indicesDoConcurso(item, grupo(q.copy(concurso = 7150)))!!.k1)
        assertEquals(34.0, indicesDoConcurso(item, grupo(q.copy(concurso = 7150)))!!.odds!!, 1e-9)
        assertNull(indicesDoConcurso(null, gq))
        // Desde a 0.21, os guardados na aposta (de quando foi registrada) valem mesmo com os Jogos em outro concurso.
        val guardada = q.copy(concurso = 7150, indices = IndicesDaAposta(k1 = 0.81, k2 = 0.12, odds = 34.0))
        val gg = grupo(guardada)
        assertEquals(0.81, indicesDoConcurso(item, gg)!!.k1!!, 1e-9)
        assertEquals(0.12, indicesDoConcurso(item, gg)!!.k2!!, 1e-9)
        assertEquals(0.81, indicesDoConcurso(null, gg)!!.k1!!, 1e-9)
        assertEquals(34.0, indicesDoConcurso(null, gg)!!.odds!!, 1e-9)
    }

    /** Desde a 0.21: o k1, o k2 e as odds do concurso ficam guardados na aposta registrada. */
    @Test
    fun indicesGuardadosNaAposta() {
        val kq = ResultadoK(numero = 7149, especial = false, acumuladoPrincipal = 0.0, reserva = 0, herancaFaixas = emptyMap(), nAnterior = 0.0, k1 = 0.7, k2 = 0.1)
        val itens = mapOf(Modalidade.QUINA to Indices.Item(Modalidade.QUINA, null, kq, 34.0))
        val registrada = aposta(Modalidade.QUINA, 1, 7149).copy(id = "r", registrada = true)
        val outroConcurso = registrada.copy(id = "o", concurso = 7150)
        val rascunho = registrada.copy(id = "x", registrada = false)
        val e = Estado(apostas = listOf(registrada, outroConcurso, rascunho))
        val g = Operacoes.guardarIndices(e, itens)
        assertEquals(IndicesDaAposta(k1 = 0.7, k2 = 0.1, odds = 34.0), g.apostas[0].indices)
        assertNull(g.apostas[1].indices)
        assertNull(g.apostas[2].indices)
        // Já guardados: não mudam (nem com os índices de agora diferentes); sem nada a guardar, o mesmo estado.
        val outros = mapOf(Modalidade.QUINA to Indices.Item(Modalidade.QUINA, null, kq.copy(k1 = 0.5), 34.0))
        assertEquals(g.apostas[0].indices, Operacoes.guardarIndices(g, outros).apostas[0].indices)
        assertTrue(Operacoes.guardarIndices(g, outros) === g)
    }

    @Test
    fun sortearOTimeDoCoracao() {
        val sorteio = Random(7)
        repeat(30) { assertTrue(timeAleatorio(TIMES, null, sorteio) in TIMES) }
        // Não repete o time de agora; com a busca, só entre os times da busca.
        val vasco = TIMES.first { it.nome == "Vasco da Gama" }
        repeat(30) { assertTrue(timeAleatorio(TIMES, vasco.caixa, sorteio) != vasco) }
        val dois = TIMES.filter { it.uf == "RJ" }.take(2)
        repeat(10) { assertEquals(dois[1], timeAleatorio(dois, dois[0].caixa, sorteio)) }
        // Só ele na busca: fica ele mesmo; a busca sem nenhum time: todos.
        assertEquals(vasco, timeAleatorio(listOf(vasco), vasco.caixa, sorteio))
        assertTrue(timeAleatorio(emptyList(), null, sorteio) in TIMES)
    }

    @Test
    fun sortearOMesDeSorte() {
        val sorteio = Random(7)
        repeat(30) { assertTrue(mesAleatorio(null, sorteio) in MESES_DE_SORTE) }
        // Não repete o mês de agora (sem diferença de acento ou de maiúscula).
        repeat(30) { assertTrue(mesAleatorio("Março", sorteio) != "Março") }
        repeat(30) { assertTrue(mesAleatorio("MARCO", sorteio) != "Março") }
        // Em 60 sorteios aparecem vários meses.
        assertTrue((1..60).map { mesAleatorio(null, sorteio) }.toSet().size > 6)
        assertEquals("Março", mesDaLista("MARÇO"))
        assertEquals(null, mesDaLista("Marte"))
        assertEquals(listOf("JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"), MESES_DE_SORTE.map { siglaDoMes(it) })
    }

    @Test
    fun cabecalhoDaImagemDosJogosComOSorteio() {
        val q = aposta(Modalidade.QUINA, 1, 7149)
        assertEquals("QUINA · CONCURSO 7149 · SORTEIO HOJE", ImagemDosJogos.cabecalhoDe(q.copy(dataSorteio = hoje), hoje))
        assertEquals("QUINA · CONCURSO 7149 · SORTEIO AMANHÃ", ImagemDosJogos.cabecalhoDe(q.copy(dataSorteio = hoje.plusDays(1)), hoje))
        // Desde a 0.20: "EM N DIAS" até 7 dias; depois, a data.
        assertEquals("QUINA · CONCURSO 7149 · SORTEIO EM 5 DIAS", ImagemDosJogos.cabecalhoDe(q.copy(dataSorteio = hoje.plusDays(5)), hoje))
        val longe = hoje.plusDays(8)
        assertEquals("QUINA · CONCURSO 7149 · SORTEIO ${Datas.longa(longe)}", ImagemDosJogos.cabecalhoDe(q.copy(dataSorteio = longe), hoje))
        // Sem a data divulgada: só a modalidade e o concurso.
        assertEquals("QUINA · CONCURSO 7149", ImagemDosJogos.cabecalhoDe(q.copy(dataSorteio = null), hoje))
    }

    @Test
    fun valorEmDinheiroNaoSeParte() {
        // "(R$ 118,69)" vai inteiro para a linha de baixo: o espaço depois do R$ não quebra.
        assertEquals("Aposta premiada (R\$\u00A0118,69)", valorSemQuebra("Aposta premiada (R\$ 118,69)"))
        assertEquals("Ontem", valorSemQuebra("Ontem"))
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
    fun precoNoCantoDoCartao() {
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
        val descartada = Saida.Descartada("Aposta descartada: o volante ficou incompleto.")

        // Aposta nova.
        assertEquals(Saida.EmAndamento("Aposta em andamento: falta registrar."), saidaDaTela(completa, null, nova = true))
        assertEquals(Saida.EmAndamento("Aposta em andamento: falta registrar."), saidaDaTela(completa.copy(comprovante = comprovante), completa, nova = true))
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
            Saida.Descartada("Aposta descartada: o bilhete ficou incompleto."),
            saidaDaTela(federal, null, nova = true),
        )
    }

    // ---------- 0.22 ----------

    @Test
    fun textoEmbaixoDoTituloDoVolante() {
        val quina = Modalidade.QUINA
        // Parâmetro desligado: "Marque N números" (o mínimo menos os já marcados) e, pronta, "Aposta completa".
        assertEquals("Marque 5 números", textoDoVolante(quina, Palpite(), mais = false))
        assertEquals("Marque 2 números", textoDoVolante(quina, Palpite(numeros = listOf(1, 2, 3)), mais = false))
        assertEquals("Marque 1 número", textoDoVolante(quina, Palpite(numeros = listOf(1, 2, 3, 4)), mais = false))
        assertEquals("Aposta completa", textoDoVolante(quina, Palpite(numeros = listOf(1, 2, 3, 4, 5)), mais = false))
        // Ligado: "Marque no mínimo N números" e, pronta, "Aposta com n números".
        assertEquals("Marque no mínimo 5 números", textoDoVolante(quina, Palpite(), mais = true))
        assertEquals("Marque no mínimo 1 número", textoDoVolante(quina, Palpite(numeros = listOf(1, 2, 3, 4)), mais = true))
        assertEquals("Aposta com 5 números", textoDoVolante(quina, Palpite(numeros = listOf(1, 2, 3, 4, 5)), mais = true))
        assertEquals("Aposta com 7 números", textoDoVolante(quina, Palpite(numeros = (1..7).toList()), mais = true))
        // Depois dos números: os trevos, o time e o mês.
        val mi = Modalidade.MAIS_MILIONARIA
        assertEquals("Marque 2 trevos", textoDoVolante(mi, Palpite(numeros = (1..6).toList()), mais = false))
        assertEquals("Marque no mínimo 1 trevo", textoDoVolante(mi, Palpite(numeros = (1..6).toList(), trevos = listOf(1)), mais = true))
        assertEquals("Aposta com 7 números e 3 trevos", textoDoVolante(mi, Palpite(numeros = (1..7).toList(), trevos = listOf(1, 2, 3)), mais = true))
        assertEquals("Escolha o Mês de Sorte", textoDoVolante(Modalidade.DIA_DE_SORTE, Palpite(numeros = (1..7).toList()), mais = false))
        assertEquals("Escolha o Time do Coração", textoDoVolante(Modalidade.TIMEMANIA, Palpite(numeros = (1..10).toList()), mais = false))
        // Super Sete (as colunas sem número), Loteca (os palpites) e Federal (sem volante).
        assertEquals("Marque 7 números", textoDoVolante(Modalidade.SUPER_SETE, Palpite(numeros = List(7) { -1 }), mais = false))
        assertEquals("Aposta completa", textoDoVolante(Modalidade.SUPER_SETE, Palpite(numeros = List(7) { 3 }), mais = true))
        assertEquals("Marque 14 palpites", textoDoVolante(Modalidade.LOTECA, Palpite(), mais = false))
        assertEquals("", textoDoVolante(Modalidade.FEDERAL, Palpite(), mais = false))
    }

    @Test
    fun botaoEAvisosDoRegistro() {
        assertEquals("Registrar aposta", textoRegistrar(1))
        assertEquals("Registrar 4 apostas", textoRegistrar(4))
        fun a(n: Int, teimosinha: Int = 0) = Aposta(
            id = "a$n", numero = n, modalidade = Modalidade.QUINA, concurso = 7000 + n, dataAposta = LocalDate.of(2026, 11, 10),
            valor = 300, criadaEm = 0, registrada = true, teimosinha = teimosinha, teimosinhaOrdem = if (teimosinha > 0) n else 0,
            teimosinhaDe = if (teimosinha > 0) "a1" else null,
        )
        assertEquals("Aposta Nº3 registrada. Anexe o comprovante da Caixa na aba Comprovantes.", mensagemDoRegistro(listOf(a(3)), individual = false))
        assertEquals("Aposta Nº3 registrada.", mensagemDoRegistro(listOf(a(3)), individual = true))
        assertEquals(
            "Apostas Nº1 e Nº2 (espelho) registradas. Anexe os comprovantes da Caixa na aba Comprovantes.",
            mensagemDoRegistro(listOf(a(1), a(2)), individual = false),
        )
        assertEquals(
            "Apostas Nº1 a Nº4 registradas (teimosinha de 4 concursos). Anexe o comprovante da Caixa na aba Comprovantes.",
            mensagemDoRegistro((1..4).map { a(it, 4) }, individual = false),
        )
        assertTrue(bloqueioDaAposta(7149).startsWith("Aposta bloqueada: o concurso 7149 já foi sorteado"))
        assertTrue(bloqueioDaAposta(null).startsWith("Aposta bloqueada: o concurso já foi sorteado"))
    }

    @Test
    fun proximaApostaEParametrosNaTela() {
        val dia10 = LocalDate.of(2026, 11, 10)
        val termo = Calendario.DIAS_PROGRAMADOS
        assertEquals("Hoje", textoDaProximaAposta(dia10, apostaDeHojeFeita = false, dias = termo))
        assertEquals(Datas.relativa(LocalDate.of(2026, 11, 13), dia10), textoDaProximaAposta(dia10, apostaDeHojeFeita = true, dias = termo))
        assertEquals("Amanhã", textoDaProximaAposta(dia10, apostaDeHojeFeita = false, dias = listOf(11)))
        assertEquals("Sem dia definido", textoDaProximaAposta(dia10, apostaDeHojeFeita = false, dias = emptyList()))
        assertEquals("10 dias por mês", resumoDosParametros(Parametros()))
        assertEquals("1 dia por mês", resumoDosParametros(Parametros(diasDeAposta = listOf(15))))
        assertEquals("Sem dia definido", resumoDosParametros(Parametros(diasDeAposta = emptyList())))
        assertEquals("3 apostas por mês: dias 1, 15 e 30.", textoDoCalendario(listOf(1, 15, 30)))
        assertEquals("1 aposta por mês: dia 5.", textoDoCalendario(listOf(5)))
        assertEquals("Parâmetros", SecaoAjuste.PARAMETROS.titulo(individual = false))
        assertEquals(SecaoAjuste.PARAMETROS, SecaoAjuste.de("parametros"))
    }

    @Test
    fun mesSorteadoPrefereOsQueOConjuntoNaoTem() {
        val sorteio = Random(5)
        // As outras apostas do concurso já têm todos menos Julho e Agosto: sai um dos dois.
        val usados = MESES_DE_SORTE - "Julho" - "Agosto"
        repeat(30) { assertTrue(mesAleatorio(null, sorteio, usados) in listOf("Julho", "Agosto")) }
        // Sem repetir o de agora: com Julho no botão, sai Agosto.
        repeat(10) { assertEquals("Agosto", mesAleatorio("Julho", sorteio, usados)) }
        // Todos usados uma vez e Março duas: qualquer um, menos Março.
        repeat(30) { assertTrue(mesAleatorio(null, sorteio, MESES_DE_SORTE + "MARÇO") != "Março") }
    }

    @Test
    fun registrarContinuaNoVolante() {
        // Desde a 0.25 o volante continua na página depois do registro: a aposta seguinte, vazia, no mesmo concurso.
        val e = Estado(cadastroConcluido = true)
        val hoje = LocalDate.of(2026, 11, 10)
        val feita = novaAposta(e, Modalidade.QUINA, null, hoje, 1L).copy(
            concurso = 7150, dataSorteio = hoje, premioEstimado = 900_000_00, numeros = listOf(5, 12, 33, 47, 71), teimosinha = 3,
        )
        val proxima = proximaAposta(e, feita, hoje, 2L)
        assertTrue(proxima.id != feita.id)
        assertEquals(7150, proxima.concurso)
        assertEquals(hoje, proxima.dataSorteio)
        assertEquals(900_000_00L, proxima.premioEstimado)
        assertTrue(proxima.numeros.isEmpty())
        assertEquals(0, proxima.teimosinha)
        assertEquals(e.config.preco(Modalidade.QUINA), proxima.valor)
        // O aviso do comprovante que falta, no alto do volante.
        assertEquals("Falta o comprovante da Caixa da aposta Nº3. Anexe em Comprovantes para ela valer no bolão.", textoFaltaComprovante(listOf(3)))
        assertEquals(
            "Falta o comprovante da Caixa das apostas Nº3, Nº4 e Nº5. Anexe em Comprovantes para elas valerem no bolão.",
            textoFaltaComprovante(listOf(5, 3, 4)),
        )
        // A confirmação do "Descartar tudo" dos Comprovantes (só os jogos, desde a 0.26).
        assertEquals(
            "3 apostas ainda sem comprovante saem do app, e R$ 9,00 volta ao saldo. As apostas com o comprovante anexado, " +
                "esperando a conferência, e os rateios ficam.",
            textoDescartarTudo(3, 900),
        )
        assertTrue(textoDescartarTudo(1, 300).startsWith("1 aposta ainda sem comprovante sai do app, e R$ 3,00 volta ao saldo."))
    }

    @Test
    fun envioEmLoteDeApostasEApuracoes() {
        fun c(id: String, tipo: TipoComprovante, em: Long, dispensado: Boolean = false) = Comprovante(
            id = id, tipo = tipo, titulo = "TÍTULO $id", texto = "", data = LocalDate.of(2026, 11, 10), criadoEm = em,
            imagem = Arquivo("$id.png", "$id.png", "image/png", em), autenticacao = "x", envioDispensado = dispensado,
        )
        val e = Estado(
            cadastroConcluido = true,
            comprovantes = listOf(
                c("apuracao", TipoComprovante.APURACAO, 30), c("aposta", TipoComprovante.APOSTA, 10), c("rateio", TipoComprovante.RATEIO, 20),
                c("enviada", TipoComprovante.APOSTA, 5), c("mensal", TipoComprovante.APORTE, 1, dispensado = true),
            ),
            envios = listOf(Envio("enviada", 6)),
        )
        // Só as apostas e as apurações que ainda não foram ao grupo, da mais antiga para a mais nova.
        val lote = Compartilhar.paraOLote(e)
        assertEquals(listOf("aposta", "apuracao"), lote.map { it.id })
        assertEquals("TÍTULO aposta\nTÍTULO apuracao", Compartilhar.textoDoLote(lote))
        // Os anexos vão de trás para a frente (desde a 0.25): o WhatsApp manda do último para o primeiro.
        assertEquals(listOf("pdf 2", "imagem 2", "pdf 1", "imagem 1"), Compartilhar.naOrdemDoWhatsApp(listOf("imagem 1", "pdf 1", "imagem 2", "pdf 2")))
        // No bolão informal nada espera envio: não há lote.
        assertTrue(Compartilhar.paraOLote(e.copy(comprovantes = e.comprovantes.map { it.copy(envioDispensado = true) })).isEmpty())
    }
}

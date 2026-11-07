package io.github.marcosfaria94.bolao.telas

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import io.github.marcosfaria94.bolao.BolaoApp
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.DadosDeExemplo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Sessoes
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.AppRaiz
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.comum.Animacoes
import io.github.marcosfaria94.bolao.ui.tema.BolaoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * TEMPORÁRIO (branch temp-telas): toques nas telas (abas girando, Painel de Jogos pela bola ou puxando
 * para baixo, o resumo das pendências, voltar puxando para baixo, telas que fecham deslizando, filtro,
 * Detalhes, segurar a modalidade, lista dos Ajustes com os Créditos e o modo de teste, Times do
 * Coração com o "Sortear", apuração sem resultado, registro sem comprovante, conferência).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w380dp-h2400dp-notnight-xhdpi")
class InteracoesTest {
    @get:Rule
    val regra = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as BolaoApp
    private val hoje = LocalDate.now()
    private val padrao = "${Datas.mesAnoCurto(YearMonth.from(LocalDate.now()))} · Tudo"

    @Before
    fun antes() {
        Animacoes.ligadas = false
    }

    private fun esperar() {
        repeat(6) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            regra.waitForIdle()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun abrir(modo: Modo = Modo.BOLAO, mudar: (Estado) -> Estado = { it }) {
        runBlocking { app.repo.atualizar { mudar(DadosDeExemplo.gerar(hoje, System.currentTimeMillis(), ZoneId.systemDefault(), semente = 7, modo = modo)) } }
        val c = Concurso(
            modalidade = Modalidade.QUINA, numero = 7147, data = hoje.minusDays(1), dezenas = listOf(5, 12, 33, 47, 71),
            proximoNumero = 7148, proximaData = hoje, premioEstimadoProximo = 5_850_000_000,
        )
        val f = Indices::class.java.getDeclaredField("_itens").apply { isAccessible = true }
        (f.get(app.indices) as MutableStateFlow<Map<Modalidade, Indices.Item>>).value =
            mapOf(Modalidade.QUINA to Indices.Item(Modalidade.QUINA, c, null, 34.0))
        regra.setContent { BolaoTema { AppRaiz() } }
        esperar()
    }

    private fun voltar() {
        regra.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        esperar()
    }

    /** Nenhum nó com o [texto] aparece na tela (pode existir numa página vizinha, fora da tela). */
    private fun naoAparece(texto: String) {
        val nos = regra.onAllNodesWithText(texto)
        repeat(nos.fetchSemanticsNodes().size) { nos[it].assertIsNotDisplayed() }
    }

    /** Rola a lista (vertical) que tem o [texto] até ele: a de dentro é a última. */
    private fun rolarAte(texto: String) {
        regra.onAllNodes(hasScrollAction() and hasAnyDescendant(hasText(texto, substring = true))).onLast()
            .performScrollToNode(hasText(texto, substring = true))
        esperar()
    }

    /** Puxa a tela para baixo a partir do meio de cima (abaixo da barra do título). */
    private fun puxarParaBaixo() {
        regra.onRoot().performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.75f) }
        esperar()
    }

    /** Empurra a tela para cima (no fim da lista). */
    private fun empurrarParaCima() {
        regra.onRoot().performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.3f) }
        esperar()
    }

    /** Espera (até [segundos]) a [condicao] valer. */
    private fun esperarAte(segundos: Int = 30, condicao: () -> Boolean) {
        val fim = System.currentTimeMillis() + segundos * 1000L
        while (!condicao() && System.currentTimeMillis() < fim) {
            Thread.sleep(250)
            esperar()
        }
        assertTrue("Esperou $segundos s e não aconteceu.", condicao())
    }

    private fun existe(texto: String, substring: Boolean = false): Boolean =
        regra.onAllNodesWithText(texto, substring = substring).fetchSemanticsNodes().isNotEmpty()

    /** Foto da tela inteira (a janela da atividade). */
    private fun fotoDaTela(nome: String) {
        var v: View? = null
        regra.activityRule.scenario.onActivity { v = it.window.decorView }
        salvar(v!!, nome)
    }

    private fun salvar(v: View, nome: String) {
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        val pasta = File("build/telas").apply { mkdirs() }
        File(pasta, "$nome-claro.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Só as apostas conferidas (sem as que esperam o comprovante). */
    private fun soConferidas(e: Estado) = e.copy(apostas = e.apostas.filter { it.registrada && !it.pendenteDeAnexo }, rateios = e.rateios.filter { !it.pendenteDeAnexo }, lancamentos = e.lancamentos.filter { l -> l.apostaId == null || e.apostas.any { it.id == l.apostaId && !it.pendenteDeAnexo } })

    /** Uma aposta da Quina registrada para amanhã (para o Painel de Jogos). */
    private fun comJogoASortear(e: Estado): Estado {
        val r = novaAposta(e, Modalidade.QUINA, null, hoje, System.currentTimeMillis() - 3_600_000)
            .copy(concurso = 7149, dataSorteio = hoje.plusDays(1), numeros = listOf(5, 12, 33, 47, 71), comprovante = Arquivo("q.pdf", "q.pdf", "application/pdf", 0))
        return Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(e, r), r.id, hoje, System.currentTimeMillis() - 3_000_000)
    }

    /** O Painel de Jogos na tela: o resumo e o jogo da Quina de amanhã (a bola com o concurso 7149). */
    private fun noPainel() {
        regra.onNodeWithText("Pendências").assertIsDisplayed()
        regra.onNodeWithText("7149").assertIsDisplayed()
    }

    @Test
    fun filtroDoHistorico() {
        abrir { comJogoASortear(it) }
        // Começa no padrão: todos os tipos no mês atual.
        regra.onNodeWithText(padrao).assertExists()
        regra.onNodeWithText("Filtrar").performClick()
        esperar()
        // Na tela "Filtrar": quadrinhos grandes, vários tipos e meses de uma vez.
        regra.onNodeWithText("TIPO").assertIsDisplayed()
        regra.onNodeWithText("MÊS").assertIsDisplayed()
        regra.onNodeWithText("Apostas").performClick()
        regra.onNodeWithText("Prêmios").performClick()
        esperar()
        regra.onNodeWithText("Todos os meses").performClick()
        esperar()
        fotoDaTela("34-filtrar-marcado")
        regra.onNodeWithText("Limpar filtro").assertExists()
        // Sai deslizando o dedo para o lado (o filtro fica).
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        val filtrado = "Todos os meses · Apostas, Prêmios"
        regra.onNodeWithText(filtrado).assertExists()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()

        // O Painel de Jogos (puxando para baixo ou pela bola) não limpam o filtro.
        puxarParaBaixo()
        noPainel()
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText(filtrado).assertExists()
        regra.onNodeWithContentDescription("Painel de Jogos").performClick()
        esperar()
        noPainel()
        voltar()
        regra.onNodeWithText(filtrado).assertExists()

        // "Limpar filtro" volta ao padrão; "Ver N itens" volta à Home.
        regra.onNodeWithText("Filtrar").performClick()
        esperar()
        regra.onNodeWithText("Limpar filtro").performClick()
        esperar()
        regra.onNodeWithText("Ver", substring = true).performClick()
        esperar()
        regra.onNodeWithText(padrao).assertExists()

        // Puxar o "Filtrar" para baixo também volta (com o filtro).
        regra.onNodeWithText("Filtrar").performClick()
        esperar()
        regra.onNodeWithText("Apostas").performClick()
        esperar()
        puxarParaBaixo()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        regra.onNodeWithText("${Datas.mesAnoCurto(YearMonth.from(hoje))} · Apostas").assertExists()

        // Abrir outra tela (uma aposta) limpa o filtro sozinho.
        rolarAte("Aposta Nº")
        regra.onAllNodesWithText("Aposta Nº", substring = true)[0].performClick()
        esperar()
        regra.onNodeWithText("Detalhes").assertExists()
        voltar()
        regra.onNodeWithText(padrao).assertExists()

        // Ir para outra aba também.
        regra.onNodeWithText("Filtrar").performClick()
        esperar()
        regra.onNodeWithText("Apostas").performClick()
        voltar()
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        regra.onNodeWithText("Home").performClick()
        esperar()
        regra.onNodeWithText(padrao).assertExists()
    }

    @Test
    fun puxarParaBaixoAbreOPainel() {
        abrir { comJogoASortear(soConferidas(it)) }
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // Sem o sino (as Notificações saíram): só a bola do Painel de Jogos na barra de cima.
        assertTrue(regra.onAllNodesWithContentDescription("Notificações").fetchSemanticsNodes().isEmpty())
        // Na Home, puxar para baixo (do alto da lista): o Painel de Jogos, com o resumo e o jogo (o sorteio, o prêmio e o OK/Pendente).
        puxarParaBaixo()
        noPainel()
        regra.onNodeWithText("Prêmio estimado").assertIsDisplayed()
        regra.onAllNodesWithText("Amanhã")[0].assertIsDisplayed()
        regra.onAllNodesWithText("Pendente")[0].assertIsDisplayed()
        // As dezenas em bolas (uma por bola) e o resumo com a pendência e a quantidade.
        for (n in listOf("05", "12", "33", "47", "71")) regra.onNodeWithText(n).assertIsDisplayed()
        regra.onNodeWithText("Enviar ao grupo").assertIsDisplayed()
        // Os jogos separados das pendências pelo título "MEUS JOGOS", com o "Compartilhar" (desde a 0.19);
        // sem o contador de jogos e OK no resumo; o título do painel com o ícone.
        regra.onNodeWithText("MEUS JOGOS").assertIsDisplayed()
        regra.onNodeWithText("Compartilhar").assertIsDisplayed()
        assertTrue(!existe("JOGOS EM ANDAMENTO") && !existe(" OK", substring = true) && !existe("1 jogo"))
        regra.onNodeWithText("Painel de Jogos").assertIsDisplayed()
        fotoDaTela("36-painel-de-jogos")
        naoAparece("Próxima aposta")
        // Sem a seta na barra de cima.
        assertTrue(regra.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isEmpty())
        // Empurrar para cima, nada; puxar para baixo volta para a Home.
        empurrarParaCima()
        noPainel()
        puxarParaBaixo()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        naoAparece("Pendências")
        // Deslizar para o lado também volta.
        puxarParaBaixo()
        noPainel()
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // Nos Jogos também; o voltar do celular volta para os Jogos.
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        puxarParaBaixo()
        noPainel()
        voltar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
    }

    @Test
    fun iconeAbreOPainel() {
        abrir { e ->
            var x = comJogoASortear(soConferidas(e))
            // Uma da Mega-Sena que falta registrar.
            val m = novaAposta(x, Modalidade.MEGA_SENA, null, hoje, System.currentTimeMillis() - 1_000_000)
                .copy(concurso = 2950, dataSorteio = hoje.plusDays(2), numeros = listOf(4, 18, 22, 35, 41, 59))
            x = Operacoes.salvarRascunho(x, m)
            x
        }
        // O ícone do painel na barra de cima, com o contador vermelho das pendências (a que falta registrar e o envio).
        regra.onNodeWithContentDescription("Painel de Jogos").assertIsDisplayed()
        regra.onNodeWithContentDescription("Painel de Jogos").performClick()
        esperar()
        // Um cartão por modalidade e concurso (a bola com a sigla e o concurso).
        regra.onNodeWithText("7149").assertIsDisplayed()
        regra.onNodeWithText("2950").assertIsDisplayed()
        // No resumo, a pendência com a quantidade; no jogo, só o OK/Pendente (sem a descrição).
        regra.onNodeWithText("Falta registrar").assertIsDisplayed()
        regra.onNodeWithText("Nova aposta").assertIsDisplayed()
        // Tocar na aposta abre a página dela; puxar para baixo volta para o Painel de Jogos.
        regra.onNodeWithText("71").performClick()
        esperar()
        regra.onNodeWithText("Detalhes").assertIsDisplayed()
        naoAparece("Pendências")
        puxarParaBaixo()
        noPainel()
        // O quadrinho "Falta registrar" abre a aposta que falta registrar.
        regra.onNodeWithText("Falta registrar").performClick()
        esperar()
        regra.onNodeWithText("Volante completo").assertExists()
    }

    /** Puxar para baixo volta nas telas por cima das abas: o aviso diz sempre "Sair" (desde a 0.19). */
    @Test
    fun puxarParaBaixoVoltaNasTelas() {
        abrir { soConferidas(it) }
        // No Aporte (aberto pela Home), puxando devagar: "Sair" desde o começo e, pronto, "Sair" de novo
        // (até a 0.18, "Voltar" e o nome da tela para onde volta).
        regra.onAllNodesWithText("Aporte")[0].performClick()
        esperar()
        naoAparece("Próxima aposta")
        assertTrue(!existe("Sair") && !existe("Home"))
        regra.onRoot().performTouchInput {
            down(Offset(width / 2f, height * 0.25f))
            repeat(6) { moveBy(Offset(0f, 20f)) }
        }
        esperar()
        assertTrue(existe("Sair"))
        assertTrue(!existe("Voltar"))
        regra.onRoot().performTouchInput { repeat(20) { moveBy(Offset(0f, 20f)) } }
        esperar()
        fotoDaTela("33-puxando-para-voltar")
        assertTrue(existe("Sair") && !existe("Home"))
        regra.onRoot().performTouchInput { up() }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // A página do jogo (aberta pelos Jogos) volta para os Jogos.
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onNodeWithText("Completar").assertIsDisplayed()
        puxarParaBaixo()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        // Os Detalhes também.
        app.destinoPendente.value = "detalhes/QUINA"
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        puxarParaBaixo()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        // Uma tela dos Ajustes.
        regra.onNodeWithText("Ajustes").performClick()
        esperar()
        regra.onNodeWithText("Aporte e preços").performClick()
        esperar()
        regra.onNodeWithText("Preço da aposta simples").assertIsDisplayed()
        puxarParaBaixo()
        regra.onNodeWithText("Atualização").assertIsDisplayed()
        // Um comprovante (do Histórico).
        val c = runBlocking { app.repo.atual() }.comprovantes.last { !it.envioDispensado }
        app.destinoPendente.value = "comprovante/${c.id}"
        esperar()
        regra.onNodeWithText(c.titulo).assertIsDisplayed()
        puxarParaBaixo()
        regra.onNodeWithText("Atualização").assertIsDisplayed()
    }

    /** Um puxão curto (como ao rolar a lista até o começo) não abre o Painel de Jogos. */
    @Test
    fun puxaoCurtoNaoAbre() {
        abrir()
        regra.onRoot().performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.25f + 150f, durationMillis = 120) }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        naoAparece("Pendências")
    }

    @Test
    fun comprovantesPeloResumo() {
        abrir { e ->
            val x = soConferidas(e)
            val r = novaAposta(x, Modalidade.QUINA, null, hoje, System.currentTimeMillis() - 7_200_000)
                .copy(concurso = 7148, dataSorteio = hoje, numeros = listOf(5, 12, 33, 47, 71))
            Operacoes.registrarAposta(Operacoes.salvarRascunho(x, r), r.id, hoje, System.currentTimeMillis() - 7_000_000)
        }
        // Só 3 abas embaixo e, em cima, a bola do Painel de Jogos (sem o botão dos Comprovantes nem o sino).
        naoAparece("Comprovantes")
        assertTrue(regra.onAllNodesWithContentDescription("Comprovantes").fetchSemanticsNodes().isEmpty())
        regra.onNodeWithText("Ajustes").assertIsDisplayed()
        // No resumo do Painel de Jogos: "Sem comprovante" com a quantidade; no jogo, o "Pendente".
        regra.onNodeWithContentDescription("Painel de Jogos").performClick()
        esperar()
        regra.onNodeWithText("Pendências").assertIsDisplayed()
        regra.onNodeWithText("Sem comprovante").assertIsDisplayed()
        regra.onAllNodesWithText("Pendente").onLast().assertIsDisplayed()
        // O quadrinho abre os Comprovantes; puxar para baixo volta para o Painel de Jogos.
        regra.onNodeWithText("Sem comprovante").performClick()
        esperar()
        regra.onNodeWithText("Anexar comprovantes").assertIsDisplayed()
        puxarParaBaixo()
        regra.onNodeWithText("Pendências").assertIsDisplayed()
        // E do Painel de Jogos, para a Home.
        puxarParaBaixo()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
    }

    @Test
    fun abasDeslizandoODedo() {
        abrir()
        regra.onNodeWithText("Próxima aposta").assertExists()
        // Para a direita, a partir da Home: os Ajustes, entrando como as outras abas (as abas giram).
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Integrantes").assertIsDisplayed()
        naoAparece("Próxima aposta")
        naoAparece("Pendências")
        // Nos Ajustes, para a esquerda: a Home.
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // Para a esquerda: Jogos e Ajustes; nos Ajustes, para a esquerda de novo, a Home.
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Integrantes").assertIsDisplayed()
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        naoAparece("Integrantes")
        // Para a direita duas vezes: Ajustes e Jogos.
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        // Arrastar só um terço da tela, devagar, já passa de aba.
        regra.onRoot().performTouchInput { swipeRight(startX = width * 0.3f, endX = width * 0.65f, durationMillis = 900) }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // Tocar na barra de baixo troca de aba (pelo caminho mais curto).
        regra.onNodeWithText("Ajustes").performClick()
        esperar()
        regra.onNodeWithText("Integrantes").assertIsDisplayed()
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // O voltar do celular, numa aba que não é a Home, volta para a Home.
        regra.onNodeWithText("Ajustes").performClick()
        esperar()
        voltar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // Do Painel de Jogos (pela bola) também.
        regra.onNodeWithContentDescription("Painel de Jogos").performClick()
        esperar()
        regra.onNodeWithText("Pendências").assertIsDisplayed()
        voltar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
    }

    @Test
    fun pendenciaDoResumoAbreATela() {
        abrir { e ->
            val r = novaAposta(e, Modalidade.QUINA, null, hoje, System.currentTimeMillis() - 7_200_000)
                .copy(concurso = 7148, dataSorteio = hoje, numeros = listOf(5, 12, 33, 47, 71))
            Operacoes.salvarRascunho(e, r)
        }
        // A bola fica na barra de cima de todas as abas (aqui, nos Ajustes).
        regra.onNodeWithText("Ajustes").performClick()
        esperar()
        regra.onNodeWithContentDescription("Painel de Jogos").performClick()
        esperar()
        // A aposta em andamento: no resumo ("Falta registrar") e no jogo ("Nova aposta", Pendente).
        regra.onNodeWithText("Falta registrar").assertIsDisplayed()
        regra.onNodeWithText("Nova aposta").assertIsDisplayed()
        // Deslizar para o lado volta para onde estava (os Ajustes).
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Integrantes").assertIsDisplayed()
        // Tocar na pendência abre a página do jogo com a aposta em andamento.
        regra.onNodeWithContentDescription("Painel de Jogos").performClick()
        esperar()
        regra.onNodeWithText("Falta registrar").performClick()
        esperar()
        regra.onNodeWithText("Volante completo").assertExists()
    }

    @Test
    fun listaDosAjustes() {
        abrir()
        regra.onNodeWithText("Ajustes").performClick()
        esperar()
        // A lista em grupos: a extinção no grupo do bolão; sem o grupo "Sobre".
        for (grupo in listOf("BOLÃO", "SEGURANÇA", "CELULAR")) regra.onNodeWithText(grupo).assertIsDisplayed()
        naoAparece("SOBRE")
        for (item in listOf("Integrantes", "Aporte e preços", "Termo", "Extinção do Bolão", "Integridade", "Auditoria", "Backup", "Notificações", "Atualização")) {
            regra.onNodeWithText(item).assertIsDisplayed()
        }
        // Desde a 0.21 o Modo de teste saiu dos Ajustes (é uma sessão à parte, na tela das sessões).
        assertTrue(!existe("Modo de teste"))
        assertTrue(regra.onAllNodesWithContentDescription("Modo de teste").fetchSemanticsNodes().isEmpty())
        // O "Apoie" antes dos Créditos: o texto (desde a 0.21, o do lema), o nome e a chave Pix; o botão copia a chave.
        regra.onNodeWithText("Gratuito e sem anúncios, esse é o nosso lema", substring = true).assertIsDisplayed()
        regra.onNodeWithText("considere pagar um café para nós", substring = true).assertIsDisplayed()
        // O nome curto (desde a 0.20).
        regra.onNodeWithText("Marcos Faria").assertIsDisplayed()
        regra.onNodeWithText("pixfacil@aol.com").assertIsDisplayed()
        regra.onNodeWithText("Apoie").performClick()
        esperar()
        regra.onNodeWithText("Pix copiado").assertIsDisplayed()
        val area = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("pixfacil@aol.com", area.primaryClip?.getItemAt(0)?.text?.toString())
        fotoDaTela("37-ajustes-apoie")
        // Sem título desde a 0.21 (na 0.20, "Aceita um café?"; na 0.19, "Doação").
        naoAparece("Aceita um café?")
        naoAparece("Doação")
        // Os Créditos no rodapé: "Criado com" e "Claude" (em duas linhas), a versão e o "Tecnologia" (que mostra a lista ali mesmo).
        regra.onNodeWithText("Criado com").assertIsDisplayed()
        regra.onNodeWithText("Claude").assertIsDisplayed()
        regra.onNodeWithText("versão", substring = true).assertIsDisplayed()
        naoAparece("Kotlin 2.2")
        regra.onNodeWithText("Tecnologia").performClick()
        esperar()
        regra.onNodeWithText("Kotlin 2.2").assertIsDisplayed()
        regra.onNodeWithText("ML Kit").assertIsDisplayed()
        fotoDaTela("32-ajustes-tecnologia")
        regra.onNodeWithText("Tecnologia").performClick()
        esperar()
        naoAparece("Kotlin 2.2")
        // A Atualização verifica ali mesmo (não abre tela).
        regra.onNodeWithText("Atualização").performClick()
        esperar()
        regra.onNodeWithText("Notificações").assertIsDisplayed()
        // Notificações: a apuração automática e o toque do prêmio juntos, sem a seta no título; puxar para baixo volta.
        regra.onNodeWithText("Notificações").performClick()
        esperar()
        regra.onNodeWithText("Apuração automática").assertIsDisplayed()
        regra.onNodeWithText("Toque do prêmio").assertExists()
        assertTrue(regra.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isEmpty())
        puxarParaBaixo()
        regra.onNodeWithText("Atualização").assertIsDisplayed()
        // A extinção abre a tela dela; deslizar para o lado volta.
        regra.onNodeWithText("Extinção do Bolão").performClick()
        esperar()
        naoAparece("Atualização")
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Aporte e preços").performClick()
        esperar()
        regra.onNodeWithText("Preço da aposta simples").assertIsDisplayed()
        voltar()
        regra.onNodeWithText("Integrantes").assertIsDisplayed()
    }

    /**
     * As sessões (desde a 0.21): o "Alternar sessão" ao lado do Painel de Jogos abre a tela das sessões
     * (quantas, cada uma, o modo de teste e o "Criar sessão"); sair puxando para baixo ou deslizando;
     * trocar de sessão; o "Excluir" é o atalho para a extinção; a sessão nova com o "Cancelar".
     */
    @Test
    fun sessoes() {
        abrir()
        // Uma sessão só: sem o nome embaixo do título; o botão ao lado do Painel de Jogos.
        regra.onNodeWithContentDescription("Alternar sessão").assertIsDisplayed()
        regra.onNodeWithContentDescription("Painel de Jogos").assertIsDisplayed()
        regra.onNodeWithContentDescription("Alternar sessão").performClick()
        esperar()
        regra.onNodeWithText("Sessões").assertIsDisplayed()
        regra.onNodeWithText("1 sessão criada").assertIsDisplayed()
        // A sessão das versões anteriores (com o cadastro) ganha o nome genérico.
        esperarAte { existe("Bolão 1") }
        regra.onNodeWithText("Em uso").assertIsDisplayed()
        regra.onNodeWithText("3 integrantes · saldo", substring = true).assertIsDisplayed()
        regra.onNodeWithText("Modo de teste").assertIsDisplayed()
        regra.onNodeWithText("Criar sessão").assertExists()
        assertTrue(regra.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isEmpty())
        // Puxar para baixo sai (o aviso "Sair"); deslizar para o lado também.
        puxarParaBaixo()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        regra.onNodeWithContentDescription("Alternar sessão").performClick()
        esperar()
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()

        // A sessão nova: o cadastro com o "Cancelar"; o Bolão pede o nome (opcional, com o genérico).
        regra.onNodeWithContentDescription("Alternar sessão").performClick()
        esperar()
        rolarAte("Criar sessão")
        regra.onNodeWithText("Criar sessão").performClick()
        esperarAte { existe("Como vai usar o app?") }
        regra.onNodeWithText("Nova sessão").assertIsDisplayed()
        regra.onNodeWithText("Cancelar").assertIsDisplayed()
        regra.onNodeWithText("Continuar").performClick()
        esperar()
        regra.onNodeWithText("Nome do bolão (opcional)").assertExists()
        regra.onNodeWithText("Vazio: \"Bolão 2\".", substring = true).assertExists()
        fotoDaTela("45-cadastro-nome-do-bolao")
        // Cancelar: a sessão nova é apagada e o app volta ao Bolão 1.
        regra.onNodeWithText("Cancelar").performClick()
        esperar()
        regra.onNodeWithText("Cancelar a sessão").performClick()
        esperarAte { existe("Próxima aposta") }
        assertEquals(1, app.sessoes.lista.value.size)

        // Duas sessões (a segunda feita aqui): o nome da sessão em uso embaixo do título.
        val outra = app.sessoes.criar()
        runBlocking {
            app.sessoes.repo(outra).atualizar { DadosDeExemplo.gerar(hoje, System.currentTimeMillis(), ZoneId.systemDefault(), semente = 3, modo = Modo.BOLAO) }
        }
        app.sessoes.nomear(outra, "Amigos do trabalho", Modo.BOLAO)
        esperarAte { existe("Próxima aposta") }
        regra.onNodeWithText("Amigos do trabalho").assertIsDisplayed()
        regra.onNodeWithContentDescription("Alternar sessão").performClick()
        esperar()
        regra.onNodeWithText("2 sessões criadas").assertIsDisplayed()
        regra.onNodeWithText("Bolão 1").assertIsDisplayed()
        fotoDaTela("44-sessoes")
        // Tocar no Bolão 1: passa a usá-lo (a tela volta para as abas).
        regra.onNodeWithText("Bolão 1").performClick()
        esperarAte { app.sessoes.ativa.value == Sessoes.PRINCIPAL && existe("Próxima aposta") }
        regra.onNodeWithText("Bolão 1").assertIsDisplayed()
        fotoDaTela("44b-home-com-o-nome-da-sessao")

        // Renomear a outra pelo menu.
        regra.onNodeWithContentDescription("Alternar sessão").performClick()
        esperar()
        regra.onNodeWithContentDescription("Opções de Amigos do trabalho").performClick()
        esperar()
        regra.onNodeWithText("Renomear").performClick()
        // O que foi escolhido no menu abre depois que ele fecha (um instante).
        regra.mainClock.advanceTimeBy(500)
        esperarAte { existe("Salvar") }
        regra.onAllNodes(hasSetTextAction())[0].performTextClearance()
        regra.onAllNodes(hasSetTextAction())[0].performTextInput("Trabalho")
        regra.onNodeWithText("Salvar").performClick()
        esperar()
        regra.onNodeWithText("Trabalho").assertIsDisplayed()

        // O "Excluir" de outra sessão: passa a usá-la e abre a extinção dela.
        regra.onNodeWithContentDescription("Opções de Trabalho").performClick()
        esperar()
        regra.onNodeWithText("Excluir").performClick()
        regra.mainClock.advanceTimeBy(500)
        esperarAte { app.sessoes.ativa.value == outra && existe("Encerrar o bolão") }
        regra.onAllNodesWithText("Extinção do Bolão")[0].assertIsDisplayed()
    }

    /** O Modo de teste é uma sessão à parte (desde a 0.21): entra pela tela das sessões e, ao sair, volta à sessão de antes. */
    @Test
    fun modoDeTesteNaTelaDasSessoes() {
        abrir()
        regra.onNodeWithContentDescription("Alternar sessão").performClick()
        esperar()
        regra.onNodeWithText("Modo de teste").performClick()
        esperarAte(60) { existe("Modo de teste · dados de exemplo") }
        esperar()
        assertEquals(Sessoes.TESTE, app.sessoes.ativa.value)
        // O teste não conta como sessão.
        regra.onNodeWithContentDescription("Alternar sessão").performClick()
        esperar()
        regra.onNodeWithText("1 sessão criada").assertIsDisplayed()
        regra.onNodeWithText("Dados de exemplo. Trocar de sessão apaga o exemplo.").assertIsDisplayed()
        fotoDaTela("46-sessoes-no-teste")
        puxarParaBaixo()
        // Sair pela faixa vermelha: o exemplo é apagado e o app volta ao Bolão 1.
        regra.onNodeWithText("Sair").performClick()
        esperar()
        regra.onNodeWithText("Sair e apagar").performClick()
        esperarAte { !existe("Modo de teste · dados de exemplo") && existe("Próxima aposta") }
        assertEquals(Sessoes.PRINCIPAL, app.sessoes.ativa.value)
        assertTrue(!app.sessoes.temTeste)
    }

    /** Só pode haver uma sessão Individual (desde a 0.21): no cadastro de outra, a opção fica desligada. */
    @Test
    fun umIndividualSo() {
        abrir(Modo.INDIVIDUAL)
        esperarAte { app.sessoes.sessao(Sessoes.PRINCIPAL)?.nome == Sessoes.NOME_INDIVIDUAL }
        app.sessoes.criar()
        esperarAte { existe("Como vai usar o app?") }
        regra.onNodeWithText("Já existe a sessão Individual", substring = true).assertIsDisplayed()
        fotoDaTela("47-cadastro-individual-ocupado")
        // O Individual não muda de nome: no menu dele não há "Renomear".
        assertTrue(app.sessoes.sessao(Sessoes.PRINCIPAL)!!.individual)
    }

    @Test
    fun timesDoCoracaoEmLista() {
        abrir { soConferidas(it) }
        app.destinoPendente.value = "modalidade/TIMEMANIA"
        esperar()
        rolarAte("Escolher o time")
        regra.onNodeWithText("Escolher o time").performClick()
        esperar()
        // A lista: um time por linha, em ordem alfabética, com o cabeçalho Nome | UF e o "Sortear" no alto.
        regra.onNodeWithText("Nome").assertIsDisplayed()
        regra.onNodeWithText("UF").assertIsDisplayed()
        regra.onNodeWithText("Sortear").assertIsDisplayed()
        regra.onNodeWithText("ABC").assertIsDisplayed()
        regra.onNodeWithText("Águia de Marabá").assertIsDisplayed()
        // Sem o campo "Outro time" no fim da lista.
        assertTrue(!existe("Outro time", substring = true))
        // Os brasões vêm da Wikipédia (no CI há internet): espera um pouco e tira a foto.
        repeat(24) {
            Thread.sleep(500)
            esperar()
        }
        salvar(ShadowDialog.getLatestDialog().window!!.decorView, "30-times")
        // Por UF: Alagoas primeiro; o ABC (RN) sai do alto.
        regra.onNodeWithText("UF").performClick()
        esperar()
        regra.onNodeWithText("ASA").assertIsDisplayed()
        regra.onNodeWithText("CRB").assertIsDisplayed()
        naoAparece("ABC")
        // De novo: de Z a A (Tocantins primeiro).
        regra.onNodeWithText("UF").performClick()
        esperar()
        regra.onNodeWithText("Tocantinópolis").assertIsDisplayed()
        // Por nome de Z a A: Ypiranga primeiro.
        regra.onNodeWithText("Nome").performClick()
        esperar()
        regra.onNodeWithText("Nome").performClick()
        esperar()
        regra.onNodeWithText("Ypiranga").assertIsDisplayed()
        // A busca continua; tocar escolhe e fecha.
        regra.onAllNodes(hasSetTextAction())[0].performTextInput("vasco")
        esperar()
        regra.onNodeWithText("Vasco da Gama").performClick()
        esperar()
        regra.onNodeWithText("Vasco da Gama / RJ").assertIsDisplayed()
        // O brasão do botão é o do time escolhido (desde a 0.20; antes ficava o do time anterior).
        esperarOsBrasoes()
        rolarAte("Vasco da Gama / RJ")
        fotoDaTela("43a-time-escolhido")
        // Sem o X ao lado do título.
        regra.onNodeWithText("Vasco da Gama / RJ").performClick()
        esperar()
        assertTrue(regra.onAllNodesWithContentDescription("Fechar").fetchSemanticsNodes().isEmpty())
        // Deslizar para o lado fecha a lista (sem a tela acompanhar o dedo) e o time continua o mesmo.
        regra.onAllNodes(isRoot()).onLast().performTouchInput { swipeLeft() }
        esperar()
        assertTrue(!existe("Sortear"))
        regra.onNodeWithText("Vasco da Gama / RJ").assertIsDisplayed()
        // O "Sortear" escolhe outro time, fecha a lista e avisa qual saiu.
        regra.onNodeWithText("Vasco da Gama / RJ").performClick()
        esperar()
        regra.onNodeWithText("Sortear").performClick()
        esperar()
        assertTrue(!existe("Sortear"))
        assertTrue(!existe("Vasco da Gama / RJ"))
        val primeiro = avisoDoSorteio()
        assertTrue(primeiro != null && "Vasco" !in primeiro)
        // O brasão acompanha o time sorteado (até a 0.19 ficava o do Vasco).
        esperarOsBrasoes()
        fotoDaTela("43b-time-sorteado")
        // Puxar a lista para baixo (do alto) também sorteia (outro time), fecha e avisa.
        // O botão do time (o aviso do sorteio também tem " / ").
        regra.onNode(hasText("Trocar") and hasText(" / ", substring = true)).performClick()
        esperar()
        regra.onNodeWithText("Sortear").assertIsDisplayed()
        regra.onAllNodes(isRoot()).onLast().performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.8f) }
        esperar()
        assertTrue(!existe("Sortear"))
        esperarAte { avisoDoSorteio().let { it != null && it != primeiro } }
    }

    /** O Mês de Sorte (desde a 0.19) funciona como os times: lista, Sortear, deslizar para fechar e puxar para sortear. */
    @Test
    fun mesesDoDiaDeSorteEmLista() {
        abrir { soConferidas(it) }
        app.destinoPendente.value = "modalidade/DIA_DE_SORTE"
        esperar()
        rolarAte("Escolher o mês")
        regra.onNodeWithText("Escolher o mês").performClick()
        esperar()
        // A lista: os 12 meses, com o "Sortear" no alto (sem a busca e sem o X).
        // O título da lista (o botão de baixo também diz "Mês de Sorte": a lista é a última raiz).
        regra.onAllNodesWithText("Mês de Sorte").onLast().assertIsDisplayed()
        regra.onNodeWithText("Sortear").assertIsDisplayed()
        for (mes in listOf("Janeiro", "Fevereiro", "Março", "Junho", "Dezembro")) regra.onNodeWithText(mes).assertExists()
        regra.onNodeWithText("JAN").assertIsDisplayed()
        assertTrue(regra.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        assertTrue(regra.onAllNodesWithContentDescription("Fechar").fetchSemanticsNodes().isEmpty())
        salvar(ShadowDialog.getLatestDialog().window!!.decorView, "42-meses")
        // Tocar escolhe e fecha.
        regra.onNodeWithText("Março").performClick()
        esperar()
        assertTrue(!existe("Sortear"))
        regra.onNodeWithText("Março").assertIsDisplayed()
        regra.onNodeWithText("MAR").assertIsDisplayed()
        // Deslizar para o lado fecha a lista e o mês continua o mesmo.
        regra.onNodeWithText("Março").performClick()
        esperar()
        regra.onNodeWithText("Sortear").assertIsDisplayed()
        regra.onAllNodes(isRoot()).onLast().performTouchInput { swipeLeft() }
        esperar()
        assertTrue(!existe("Sortear"))
        regra.onNodeWithText("Março").assertIsDisplayed()
        // O "Sortear" escolhe outro mês, fecha a lista e avisa qual saiu.
        regra.onNodeWithText("Março").performClick()
        esperar()
        regra.onNodeWithText("Sortear").performClick()
        esperar()
        assertTrue(!existe("Sortear"))
        val primeiro = avisoDoMes()
        assertTrue(primeiro != null && "Março" !in primeiro)
        // Puxar a lista para baixo (do alto) também sorteia, fecha e avisa.
        regra.onNode(hasText("Trocar") and hasText("Mês de Sorte")).performClick()
        esperar()
        regra.onNodeWithText("Sortear").assertIsDisplayed()
        regra.onAllNodes(isRoot()).onLast().performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.8f) }
        esperar()
        assertTrue(!existe("Sortear"))
        esperarAte { avisoDoMes().let { it != null && it != primeiro } }
    }

    /** A mensagem rápida do sorteio do mês ("Mês sorteado: Março"), se estiver na tela. */
    private fun avisoDoMes(): String? = regra.onAllNodesWithText("Mês sorteado:", substring = true).fetchSemanticsNodes()
        .flatMap { it.config.getOrElse(SemanticsProperties.Text) { emptyList() } }
        .map { it.text }
        .firstOrNull { it.startsWith("Mês sorteado:") }

    /** Tocar na Legenda dos Jogos abre a página do cálculo do k1 e do k2 (desde a 0.19). */
    @Test
    fun legendaAbreOCalculo() {
        abrir()
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        regra.onNodeWithText("Ver cálculo").assertIsDisplayed()
        regra.onNodeWithText("Legenda").performClick()
        esperar()
        regra.onNodeWithText("k1 e k2").assertIsDisplayed()
        regra.onNodeWithText("Retorno médio").assertIsDisplayed()
        regra.onNodeWithText("Retorno médio frequente").assertIsDisplayed()
        rolarAte("A RECEITA DO k1 EM 5 PASSOS")
        regra.onNodeWithText("Descontar o IR").assertExists()
        naoAparece("Ver cálculo")
        // Sem a seta na barra de cima; deslizar para o lado volta para os Jogos.
        assertTrue(regra.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isEmpty())
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Ver cálculo").assertIsDisplayed()
        // De novo, e puxando para baixo também volta.
        regra.onNodeWithText("Legenda").performClick()
        esperar()
        regra.onNodeWithText("Retorno médio").assertIsDisplayed()
        puxarParaBaixo()
        regra.onNodeWithText("Ver cálculo").assertIsDisplayed()
    }

    /** Espera os brasões chegarem da Wikipédia (no CI há internet). */
    private fun esperarOsBrasoes() {
        repeat(12) {
            Thread.sleep(500)
            esperar()
        }
    }

    /** A aposta espelho da Lotomania (desde a 0.20): a opção no volante registra duas apostas. */
    @Test
    @Suppress("UNCHECKED_CAST")
    fun apostaEspelhoDaLotomania() {
        abrir { soConferidas(it) }
        // O próximo concurso da Lotomania (o volante já abre com ele).
        val c = Concurso(
            modalidade = Modalidade.LOTOMANIA, numero = 2789, data = hoje.minusDays(1), dezenas = (1..20).toList(),
            proximoNumero = 2790, proximaData = hoje.plusDays(2), premioEstimadoProximo = 380_000_000,
        )
        val f = Indices::class.java.getDeclaredField("_itens").apply { isAccessible = true }
        val itens = f.get(app.indices) as MutableStateFlow<Map<Modalidade, Indices.Item>>
        itens.value = itens.value + (Modalidade.LOTOMANIA to Indices.Item(Modalidade.LOTOMANIA, c, null, 7.0))
        esperar()
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        app.destinoPendente.value = "modalidade/LOTOMANIA"
        esperar()
        regra.onNodeWithText("Completar").performClick()
        esperar()
        // A opção, desligada no começo; tocar liga.
        rolarAte("Aposta espelho")
        regra.onNodeWithText("Aposta espelho").assertIsOff()
        regra.onNodeWithText("Faz outra aposta com os 50 números", substring = true).assertExists()
        regra.onNodeWithText("Aposta espelho").performClick()
        esperar()
        regra.onNodeWithText("Aposta espelho").assertIsOn()
        // O botão passa a falar das duas apostas.
        rolarAte("Registrar apostas")
        regra.onNodeWithText("Registrar apostas").performClick()
        esperar()
        // A pergunta fala das duas apostas.
        regra.onNodeWithText("Deseja registrar as apostas?").assertExists()
        regra.onNodeWithText("são duas apostas", substring = true).assertExists()
        regra.onNodeWithText("Sim").performClick()
        esperar()
        esperar()
        val e = runBlocking { app.repo.atual() }
        val duas = Operacoes.apostasSemComprovante(e).filter { it.modalidade == Modalidade.LOTOMANIA }.sortedBy { it.numero }
        assertEquals(2, duas.size)
        assertEquals(50, duas[0].numeros.size)
        assertEquals((0..99).toList(), (duas[0].numeros + duas[1].numeros).sorted())
        assertEquals(duas[0].concurso, duas[1].concurso)
        assertEquals(duas[0].numero + 1, duas[1].numero)
    }

    /** A mensagem rápida do sorteio do time ("Time sorteado: ABC / RN"), se estiver na tela. */
    private fun avisoDoSorteio(): String? = regra.onAllNodesWithText("Time sorteado:", substring = true).fetchSemanticsNodes()
        .flatMap { it.config.getOrElse(SemanticsProperties.Text) { emptyList() } }
        .map { it.text }
        .firstOrNull { it.startsWith("Time sorteado:") }

    @Test
    fun apuracaoSemResultado() {
        // Duas apostas conferidas, sem o resultado da Caixa (no teste a Caixa não responde): uma com o
        // sorteio hoje e outra com o sorteio ontem.
        var hojeId = ""
        var ontemId = ""
        abrir { e ->
            var x = soConferidas(e)
            for ((dias, guardar) in listOf<Pair<Long, (String) -> Unit>>(0L to { hojeId = it }, 1L to { ontemId = it })) {
                val sorteio = hoje.minusDays(dias)
                val r = novaAposta(x, Modalidade.QUINA, null, sorteio.minusDays(1), System.currentTimeMillis() - 90_000_000)
                    .copy(concurso = 7148 - dias.toInt(), dataSorteio = sorteio, numeros = listOf(5, 12, 33, 47, 71), comprovante = Arquivo("q.pdf", "q.pdf", "application/pdf", 0))
                guardar(r.id)
                x = Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(x, r), r.id, sorteio.minusDays(1), System.currentTimeMillis() - 90_000_000)
            }
            x
        }
        // No dia do sorteio: só "Resultado ainda não apurado" (sem os botões) e sem o comprovante da apuração.
        app.destinoPendente.value = "aposta/$hojeId"
        esperar()
        esperarAte { existe("Resultado ainda não apurado") }
        assertTrue(!existe("Tentar de novo") && !existe("Informar à mão"))
        assertTrue(!existe("Comprovante da apuração"))
        assertTrue(!existe("Não foi possível consultar", substring = true))
        regra.onNodeWithText("Comprovante da aposta").assertExists()
        voltar()
        // No dia seguinte ao sorteio, ainda sem o resultado: o erro, com os dois botões.
        app.destinoPendente.value = "aposta/$ontemId"
        esperar()
        esperarAte { existe("Tentar de novo") }
        regra.onNodeWithText("Resultado ainda não apurado").assertExists()
        regra.onNodeWithText("Informar à mão").assertExists()
        assertTrue(!existe("Comprovante da apuração"))
        // "Informar à mão": o resultado digitado e o "Apurar e enviar" (fora da linha dos comprovantes).
        rolarAte("Informar à mão")
        regra.onNodeWithText("Informar à mão").performClick()
        esperar()
        regra.onNodeWithText("Resultado do sorteio").assertExists()
        regra.onNodeWithText("Apurar e enviar").assertExists()
        assertTrue(!existe("Comprovante da apuração"))
    }

    @Test
    fun registrarSemOComprovanteEDescartar() {
        abrir { soConferidas(it) }
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onNodeWithText("Completar").performClick()
        esperar()
        rolarAte("Registrar aposta")
        regra.onNodeWithText("Registrar aposta").performClick()
        esperar()
        // Sem a Caixa (no teste), o app avisa e pergunta.
        regra.onNodeWithText("Deseja registrar aposta?").assertExists()
        regra.onNodeWithText("Sim").performClick()
        esperar()
        val e = runBlocking { app.repo.atual() }
        val a = Operacoes.apostasSemComprovante(e).single()
        assertEquals(Modalidade.QUINA, a.modalidade)
        // A página mostra a aposta esperando o comprovante, com o descarte.
        regra.onNodeWithText("Esperando o comprovante da Caixa").assertExists()
        rolarAte("Descartar aposta")
        regra.onNodeWithText("Descartar aposta").performClick()
        esperar()
        regra.onNodeWithText("Descartar").performClick()
        esperar()
        esperar()
        val depois = runBlocking { app.repo.atual() }
        assertTrue(Operacoes.apostasSemComprovante(depois).isEmpty())
        // Ao sair da página, a aposta descartada não volta como rascunho.
        assertTrue(Operacoes.rascunhos(depois).isEmpty())
        assertEquals(Operacoes.saldo(e) + a.valor, Operacoes.saldo(depois))
    }

    @Test
    fun conferenciaNaAbaComprovantes() {
        val arq = Arquivo("caixa.pdf", "caixa.pdf", "application/pdf", System.currentTimeMillis(), sha256 = "AB")
        var id = ""
        abrir { e ->
            var x = soConferidas(e)
            val r = novaAposta(x, Modalidade.QUINA, null, hoje, System.currentTimeMillis() - 3_600_000)
                .copy(concurso = 7148, dataSorteio = hoje, numeros = listOf(5, 12, 33, 47, 71))
            id = r.id
            x = Operacoes.registrarAposta(Operacoes.salvarRascunho(x, r), r.id, hoje, System.currentTimeMillis() - 3_000_000)
            Operacoes.anexarNaAposta(x, r.id, arq)
        }
        app.destinoPendente.value = "comprovantes"
        esperar()
        regra.onNodeWithText("Conferência").assertExists()
        regra.onNodeWithText("Aprovar todos (1)").performClick()
        esperar()
        regra.onNodeWithText("Aprovar").performClick()
        esperar()
        // A aprovação gera o comprovante (imagem): espera terminar, em vez de um tempo fixo.
        esperarAte { Operacoes.apostasParaConferir(runBlocking { app.repo.atual() }).isEmpty() }
        esperar()
        val e = runBlocking { app.repo.atual() }
        val c = Operacoes.comprovanteDaAposta(e, e.apostas.first { it.id == id })!!
        assertEquals(listOf("caixa.pdf"), c.anexos.map { it.nome })
        // Pronto para enviar ao grupo, ali mesmo.
        regra.onNodeWithText("Enviar ao grupo").assertExists()
        regra.onNodeWithText(c.titulo).assertExists()
    }

    @Test
    fun detalhesNaBarraEDeslizando() {
        abrir { soConferidas(it) }
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onNodeWithText("Completar").performClick()
        esperar()
        regra.onNodeWithText("Volante completo").assertIsDisplayed()
        // Na página do jogo: "← Voltar" e "Detalhes →".
        regra.onNodeWithText("Voltar").assertIsDisplayed()
        regra.onNodeWithText("Detalhes").performClick()
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onNodeWithText("Probabilidades").assertIsDisplayed()
        naoAparece("Volante completo")
        // Nos Detalhes, a barra mostra "← Aposta" e "Voltar →".
        regra.onNodeWithText("Aposta").assertIsDisplayed()
        regra.onNodeWithText("Voltar").assertIsDisplayed()
        // A seta do "← Aposta" volta à página do jogo.
        regra.onNodeWithContentDescription("Voltar").performClick()
        esperar()
        regra.onNodeWithText("Volante completo").assertIsDisplayed()
        regra.onNodeWithText("Detalhes").performClick()
        esperar()
        // O voltar do celular também volta à página do jogo, como ela estava.
        voltar()
        naoAparece("Regras")
        regra.onNodeWithText("Volante completo").assertIsDisplayed()
        // Deslizando o dedo: para a esquerda abre; para a direita volta.
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Volante completo").assertIsDisplayed()
        // Para a direita na página do jogo: sai dela (volta para os Jogos) e a aposta completa fica em andamento.
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        val rascunhos = runBlocking { Operacoes.rascunhos(app.repo.atual()) }
        assertEquals(listOf(Modalidade.QUINA), rascunhos.map { it.modalidade })
        assertEquals(5, rascunhos.single().numeros.size)
        // Nos Detalhes, para a esquerda: volta para onde a página foi aberta (os Jogos).
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onRoot().performTouchInput { swipeLeft() }
        esperar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        naoAparece("Próxima aposta")
        // Aberta pela Home: o "Voltar →" dos Detalhes volta para a Home.
        regra.onNodeWithText("Home").performClick()
        esperar()
        app.destinoPendente.value = "detalhes/QUINA"
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onNodeWithText("Voltar").performClick()
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        // E o "← Voltar" (o título) da página do jogo também.
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onNodeWithText("Volante completo").assertIsDisplayed()
        regra.onNodeWithText("Voltar").performClick()
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
    }

    @Test
    fun segurarAModalidadeAbreOsDetalhes() {
        abrir { soConferidas(it) }
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        // Segurar o cartão da Quina: os Detalhes, direto.
        regra.onAllNodesWithText("Quina")[0].performTouchInput { longClick() }
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onNodeWithText("Aposta").assertIsDisplayed()
        // O voltar do celular sai da página (volta para a lista dos Jogos).
        voltar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        // Segurar pouco (meio segundo) não abre: só tocar abre a página do jogo.
        regra.onAllNodesWithText("Quina")[0].performTouchInput {
            down(center)
            advanceEventTime(500)
            up()
        }
        esperar()
        naoAparece("Regras")
        regra.onNodeWithText("Completar").assertIsDisplayed()
        voltar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        // A seta do "← Aposta" vai para o volante.
        regra.onAllNodesWithText("Quina")[0].performTouchInput { longClick() }
        esperar()
        regra.onNodeWithContentDescription("Voltar").performClick()
        esperar()
        regra.onNodeWithText("Completar").assertIsDisplayed()
        voltar()
        regra.onNodeWithText("Legenda").assertIsDisplayed()
        // Tocar (sem segurar) abre a página do jogo, com o volante.
        regra.onAllNodesWithText("Quina")[0].performClick()
        esperar()
        regra.onNodeWithText("Completar").assertIsDisplayed()
    }

    @Test
    fun mesmaApostaEmDuasTelas() {
        abrir { soConferidas(it) }
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onNodeWithText("Completar").performClick()
        esperar()
        val primeira = runBlocking { Operacoes.rascunhos(app.repo.atual()) }.single()

        // A mesma aposta aberta por cima (como pelo aviso do celular) e mudada lá.
        app.destinoPendente.value = "aposta/${primeira.id}"
        esperar()
        regra.onNodeWithText("Limpar").performClick()
        esperar()
        regra.onNodeWithText("Completar").performClick()
        esperar()
        voltar()
        val mudada = runBlocking { Operacoes.rascunhos(app.repo.atual()) }.single()
        assertTrue(mudada.numeros != primeira.numeros)
        // A página de baixo passa a mostrar a versão nova: ao sair dela, a mudança fica.
        voltar()
        val final = runBlocking { Operacoes.rascunhos(app.repo.atual()) }.single()
        assertEquals(mudada.numeros, final.numeros)

        // Descartada numa tela por cima: a página de baixo fecha sozinha e não traz a aposta de volta.
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onNodeWithText("Volante completo").assertExists()
        app.destinoPendente.value = "aposta/${final.id}"
        esperar()
        rolarAte("Descartar rascunho")
        regra.onNodeWithText("Descartar rascunho").performClick()
        esperar()
        regra.onNodeWithText("Confirmar").performClick()
        esperar()
        regra.onNodeWithText("Legenda").assertExists()
        assertTrue(runBlocking { Operacoes.rascunhos(app.repo.atual()) }.isEmpty())
    }

    @Test
    fun aporteRateioExtratoESaldoFechamDeslizandoOuPuxando() {
        abrir()
        for (puxando in listOf(false, true)) {
            for (atalho in listOf("Aporte", "Rateio", "Extrato", "Saldo líquido")) {
                regra.onAllNodesWithText(atalho)[0].performClick()
                esperar()
                naoAparece("Próxima aposta")
                // Sem a seta na barra de cima.
                assertTrue(atalho, regra.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isEmpty())
                if (puxando) {
                    puxarParaBaixo()
                } else {
                    regra.onRoot().performTouchInput { if (atalho == "Rateio" || atalho == "Saldo líquido") swipeLeft() else swipeRight() }
                    esperar()
                }
                regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
            }
        }
    }

    /** Tocar na aba em que já se está: a lista volta ao começo. */
    @Test
    @Config(qualifiers = "w380dp-h800dp-notnight-xhdpi")
    fun tocarNaAbaAtualVoltaAoComeco() {
        abrir()
        // Todos os meses no Histórico, para a lista ficar comprida.
        regra.onNodeWithText("Filtrar").performClick()
        esperar()
        regra.onNodeWithText("Todos os meses").performClick()
        esperar()
        regra.onRoot().performTouchInput { swipeRight() }
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
        repeat(4) { empurrarParaCima() }
        naoAparece("Próxima aposta")
        // "Home" no título e na barra de baixo: o da barra é o último.
        regra.onAllNodesWithText("Home").onLast().performClick()
        esperar()
        regra.onNodeWithText("Próxima aposta").assertIsDisplayed()
    }

    @Test
    fun apagarTudoVoltaAoComoVaiUsar() {
        abrir(Modo.INDIVIDUAL)
        regra.onNodeWithText("Gasto em apostas").assertExists()
        // No individual: os Ajustes com a lista do modo (o "Apagar tudo" no grupo das apostas).
        regra.onNodeWithText("Ajustes").performClick()
        esperar()
        regra.onNodeWithText("APOSTAS").assertIsDisplayed()
        regra.onNodeWithText("Apagar tudo").assertIsDisplayed()
        naoAparece("Integrantes")
        // Desde a 0.21, apagar tudo apaga a sessão; sem outra, o app vai para uma sessão nova (o cadastro).
        runBlocking { app.sessoes.excluir(app.repo.sessao) }
        esperar()
        // O cadastro recomeça no "Como vai usar o app?" (e não no último passo do cadastro anterior).
        regra.onNodeWithText("Como vai usar o app?").assertIsDisplayed()
        regra.onNodeWithText("Passo 1 de 5").assertExists()
    }
}

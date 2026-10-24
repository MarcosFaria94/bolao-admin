package io.github.marcosfaria94.bolao.telas

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import io.github.marcosfaria94.bolao.BolaoApp
import io.github.marcosfaria94.bolao.data.DadosDeExemplo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Concurso
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
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** TEMPORÁRIO (branch temp-telas): toques nas telas novas (parâmetros do Histórico, sino, Detalhes). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w380dp-h2400dp-notnight-xhdpi")
class InteracoesTest {
    @get:Rule
    val regra = createAndroidComposeRule<ComponentActivity>()

    private val app get() = RuntimeEnvironment.getApplication() as BolaoApp
    private val hoje = LocalDate.now()

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
    private fun abrir(mudar: (Estado) -> Estado = { it }) {
        runBlocking { app.repo.atualizar { mudar(DadosDeExemplo.gerar(hoje, System.currentTimeMillis(), ZoneId.systemDefault())) } }
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

    @Test
    fun parametrosDoHistorico() {
        abrir()
        // Começa em "Tudo" e no mês atual.
        regra.onNodeWithContentDescription("Escolher o tipo").assertExists()
        regra.onNodeWithText("Tudo").performClick()
        esperar()
        regra.onNodeWithText("Apostas").performClick()
        esperar()
        // Mudar o mês não mexe no tipo.
        regra.onNodeWithContentDescription("Escolher o mês").performClick()
        esperar()
        regra.onAllNodesWithText("Tudo")[0].performClick()
        esperar()
        assertEquals(1, regra.onAllNodesWithText("Apostas").fetchSemanticsNodes().size)
        assertEquals(1, regra.onAllNodesWithText("Tudo").fetchSemanticsNodes().size)

        // Abrir uma aposta e voltar: os parâmetros ficam como estavam.
        regra.onNode(hasScrollAction()).performScrollToNode(hasText("Aposta Nº2", substring = true))
        esperar()
        regra.onAllNodesWithText("Aposta Nº2", substring = true)[0].performClick()
        esperar()
        regra.onNodeWithText("Detalhes").assertExists()
        voltar()
        regra.onNodeWithText("Apostas").assertExists()

        // Ir para outra aba e voltar: os dois recomeçam (Tudo e o mês atual).
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        regra.onNodeWithText("Legenda").assertExists()
        regra.onNodeWithText("Home").performClick()
        esperar()
        regra.onNodeWithText("Próxima aposta").assertExists()
        regra.onNodeWithText("Tudo").assertExists()
        assertTrue(regra.onAllNodesWithText("Apostas").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun sinoAbreAsNotificacoes() {
        abrir { e ->
            val r = novaAposta(e, Modalidade.QUINA, null, hoje, System.currentTimeMillis() - 7_200_000)
                .copy(concurso = 7148, dataSorteio = hoje, numeros = listOf(5, 12, 33, 47, 71))
            Operacoes.salvarRascunho(e, r)
        }
        regra.onNodeWithContentDescription("Notificações").performClick()
        esperar()
        regra.onNodeWithText("Anexar o comprovante da Caixa").assertIsDisplayed()
        // Tocar na pendência abre a página do jogo com a aposta em andamento.
        regra.onNodeWithText("Anexar o comprovante da Caixa").performClick()
        esperar()
        regra.onNodeWithText("Volante completo").assertExists()
    }

    @Test
    fun detalhesNaoDescartamAAposta() {
        abrir { it.copy(apostas = it.apostas.filter { a -> a.registrada }) }
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        regra.onNodeWithText("Completar").performClick()
        esperar()
        regra.onNodeWithText("Volante completo").assertExists()
        // O botão "Detalhes" puxa os Detalhes por cima da página; a barra de cima passa a dizer "Detalhes".
        regra.onNodeWithText("Detalhes").performClick()
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onNodeWithText("Probabilidades").assertExists()
        assertEquals(2, regra.onAllNodesWithText("Detalhes").fetchSemanticsNodes().size)
        // O voltar do celular fecha só os Detalhes: a página volta como estava, com o volante.
        voltar()
        assertTrue(regra.onAllNodesWithText("Regras").fetchSemanticsNodes().isEmpty())
        assertEquals(1, regra.onAllNodesWithText("Detalhes").fetchSemanticsNodes().size)
        regra.onNodeWithText("Volante completo").assertExists()
        // Arrastar o dedo para a esquerda abre; para a direita, fecha.
        regra.onRoot().performTouchInput { swipeLeft(startX = width * 0.9f, endX = width * 0.1f) }
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onRoot().performTouchInput { swipeRight(startX = width * 0.1f, endX = width * 0.9f) }
        esperar()
        assertTrue(regra.onAllNodesWithText("Regras").fetchSemanticsNodes().isEmpty())
        // Arrastar só um pouquinho, devagar: volta a fechar sozinho.
        regra.onRoot().performTouchInput { swipeLeft(startX = width * 0.9f, endX = width * 0.75f, durationMillis = 1500) }
        esperar()
        assertTrue(regra.onAllNodesWithText("Regras").fetchSemanticsNodes().isEmpty())
        // A alça da borda abre com um toque; a seta da barra de cima fecha só os Detalhes.
        regra.onNodeWithContentDescription("Abrir os Detalhes").performClick()
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onNodeWithContentDescription("Voltar").performClick()
        esperar()
        assertTrue(regra.onAllNodesWithText("Regras").fetchSemanticsNodes().isEmpty())
        regra.onNodeWithText("Volante completo").assertExists()
        // Sair da página: a aposta completa fica em andamento.
        voltar()
        val rascunhos = runBlocking { Operacoes.rascunhos(app.repo.atual()) }
        assertEquals(listOf(Modalidade.QUINA), rascunhos.map { it.modalidade })
        assertEquals(5, rascunhos.single().numeros.size)
    }

    @Test
    fun alcasDosDetalhes() {
        abrir { it.copy(apostas = it.apostas.filter { a -> a.registrada }) }
        regra.onNodeWithText("Jogos").performClick()
        esperar()
        app.destinoPendente.value = "modalidade/QUINA"
        esperar()
        // Aberta pelo botão, fechada com um toque na alça dos Detalhes (na borda esquerda deles).
        regra.onNodeWithText("Detalhes").performClick()
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        regra.onNodeWithContentDescription("Fechar os Detalhes").performClick()
        esperar()
        assertTrue(regra.onAllNodesWithText("Regras").fetchSemanticsNodes().isEmpty())
        // Um toque na alça da página (na borda direita) abre.
        regra.onNodeWithContentDescription("Abrir os Detalhes").performClick()
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
        // Arrastar a alça dos Detalhes para a direita fecha; arrastar a alça da página para a esquerda abre.
        regra.onNodeWithContentDescription("Fechar os Detalhes").performTouchInput { swipeRight(startX = centerX, endX = centerX + 600f) }
        esperar()
        assertTrue(regra.onAllNodesWithText("Regras").fetchSemanticsNodes().isEmpty())
        regra.onNodeWithContentDescription("Abrir os Detalhes").performTouchInput { swipeLeft(startX = centerX, endX = centerX - 600f) }
        esperar()
        regra.onNodeWithText("Regras").assertIsDisplayed()
    }

    @Test
    fun mesmaApostaEmDuasTelas() {
        abrir { it.copy(apostas = it.apostas.filter { a -> a.registrada }) }
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
        regra.onNode(hasScrollAction()).performScrollToNode(hasText("Descartar rascunho"))
        regra.onNodeWithText("Descartar rascunho").performClick()
        esperar()
        regra.onNodeWithText("Confirmar").performClick()
        esperar()
        regra.onNodeWithText("Legenda").assertExists()
        assertTrue(runBlocking { Operacoes.rascunhos(app.repo.atual()) }.isEmpty())
    }
}

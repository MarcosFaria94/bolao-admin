package io.github.marcosfaria94.bolao.telas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.github.marcosfaria94.bolao.BolaoApp
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.DadosDeExemplo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.ui.AppRaiz
import io.github.marcosfaria94.bolao.ui.aposta.ApostaEmEdicao
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.comum.Animacoes
import io.github.marcosfaria94.bolao.ui.tema.BolaoTema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** TEMPORÁRIO (branch temp-telas): fotos das telas no computador, nos dois temas. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w380dp-h1900dp-notnight-xhdpi")
class FotosDasTelas {
    private val pasta = File("build/telas").apply { mkdirs() }
    private val app get() = RuntimeEnvironment.getApplication() as BolaoApp
    private val hoje = LocalDate.now()

    @Before
    fun antes() {
        Animacoes.ligadas = false
    }

    @After
    fun depois() {
        System.clearProperty("fotos.detalhes")
        System.clearProperty("fotos.filtro")
        System.clearProperty("fotos.tudo")
    }

    private fun esperar(vezes: Int = 8) {
        repeat(vezes) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
            Thread.sleep(40)
        }
    }

    private fun dados(mudar: (Estado) -> Estado = { it }) {
        runBlocking {
            app.repo.atualizar { mudar(DadosDeExemplo.gerar(hoje, System.currentTimeMillis(), ZoneId.systemDefault())) }
        }
    }

    private fun item(
        m: Modalidade, numero: Int, k1: Double?, k2: Double?, odds: Double?, premio: Long, sorteio: LocalDate,
        dezenas: List<Int> = emptyList(), faixas: List<FaixaPremio> = emptyList(), acumulado: Boolean = false,
    ): Indices.Item {
        val c = Concurso(
            modalidade = m, numero = numero - 1, data = hoje.minusDays(1), dezenas = dezenas, faixas = faixas,
            acumulado = acumulado, proximoNumero = numero, proximaData = sorteio, premioEstimadoProximo = premio,
        )
        val k = if (k1 != null) {
            ResultadoK(numero = numero, especial = false, acumuladoPrincipal = 0.0, reserva = 0, herancaFaixas = emptyMap(), nAnterior = 0.0, k1 = k1, k2 = k2)
        } else {
            null
        }
        return Indices.Item(m, c, k, odds)
    }

    @Suppress("UNCHECKED_CAST")
    private fun indices(avisos: Boolean = false) {
        val mapa = listOf(
            item(
                Modalidade.QUINA, 7148, 0.762, 0.091, 34.0, 5_850_000_000, hoje, listOf(5, 12, 33, 47, 71),
                faixas = listOf(FaixaPremio(1, "5 acertos", 0, 0), FaixaPremio(2, "4 acertos", 54, 812_345), FaixaPremio(3, "3 acertos", 4_210, 9_876)),
                acumulado = true,
            ),
            item(Modalidade.MEGA_SENA, 2950, 0.701, 0.052, 2332.0, 12_000_000_000, hoje.plusDays(1), listOf(4, 18, 22, 35, 41, 59)),
            item(Modalidade.LOTOFACIL, 3512, 0.655, 0.312, 11.0, 170_000_000, hoje, (1..15).toList()),
            item(Modalidade.DIA_DE_SORTE, 1125, 0.612, 0.120, 6.0, 30_000_000, hoje.plusDays(2)),
            item(Modalidade.LOTOMANIA, 2780, 0.598, 0.200, 7.0, 380_000_000, hoje.plusDays(1)),
            item(Modalidade.TIMEMANIA, 2290, 0.560, 0.110, 26.0, 1_200_000_000, hoje.plusDays(2)),
            item(Modalidade.SUPER_SETE, 750, 0.520, 0.050, 44.0, 280_000_000, hoje.plusDays(1)),
            item(Modalidade.DUPLA_SENA, 2860, 0.510, 0.070, 1000.0, 400_000_000, hoje.plusDays(3)),
            item(Modalidade.MAIS_MILIONARIA, 300, 0.480, 0.030, 1000.0, 19_000_000_000, hoje.plusDays(4)),
            item(Modalidade.LOTECA, 1215, null, null, null, 150_000_000, hoje.plusDays(3)),
            item(Modalidade.FEDERAL, 6012, null, null, null, 50_000_000, hoje.plusDays(2)),
        ).associateBy { it.modalidade }
        val f = Indices::class.java.getDeclaredField("_itens").apply { isAccessible = true }
        (f.get(app.indices) as MutableStateFlow<Map<Modalidade, Indices.Item>>).value = mapa
        if (avisos) {
            val g = Indices::class.java.getDeclaredField("_avisos").apply { isAccessible = true }
            (g.get(app.indices) as MutableStateFlow<List<Indices.Aviso>>).value = listOf(
                Indices.Aviso(
                    "indices-regras-QUINA", Modalidade.QUINA, "Regras da Quina mudaram: os índices podem estar errados",
                    "3 concursos seguidos com o rateio fora das regras da Caixa.", System.currentTimeMillis() - 3_600_000, "modalidade/QUINA",
                ),
            )
        }
    }

    private fun foto(a: ComponentActivity, nome: String) {
        val v = a.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        File(pasta, "$nome.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Abre o app, vai para [destinos] (um depois do outro) e tira a foto, nos dois temas. */
    private fun tela(nome: String, vararg destinos: String, altura: Int = 1900, arrastar: Float? = null) {
        for (tema in listOf("claro", "escuro")) {
            RuntimeEnvironment.setQualifiers("w380dp-h${altura}dp-${if (tema == "escuro") "night" else "notnight"}-xhdpi")
            val c = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            c.get().setContent { BolaoTema { AppRaiz() } }
            esperar()
            for (d in destinos) {
                app.destinoPendente.value = d
                esperar()
            }
            val v = c.get().window.decorView
            val y = v.height * 0.5f
            val inicio = SystemClock.uptimeMillis()
            fun tocar(acao: Int, x: Float, ms: Long) {
                val ev = MotionEvent.obtain(inicio, inicio + ms, acao, x, y, 0)
                v.dispatchTouchEvent(ev)
                ev.recycle()
            }
            // [arrastar]: o dedo vai da direita para a esquerda essa fração da largura (e fica parado ali).
            if (arrastar != null) {
                tocar(MotionEvent.ACTION_DOWN, v.width * 0.9f, 0)
                for (i in 1..12) {
                    tocar(MotionEvent.ACTION_MOVE, v.width * (0.9f - arrastar * i / 12), 16L * i)
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16))
                }
                esperar(2)
            }
            foto(c.get(), "$nome-$tema")
            if (arrastar != null) {
                tocar(MotionEvent.ACTION_UP, v.width * (0.9f - arrastar), 5000)
                esperar(2)
            }
            c.pause().stop().destroy()
            esperar(2)
        }
    }

    @Test
    fun home() {
        dados(); indices()
        tela("01-home", altura = 2600)
    }

    @Test
    fun homeApostasDeTodosOsMeses() {
        dados(); indices()
        System.setProperty("fotos.filtro", "APOSTAS")
        System.setProperty("fotos.tudo", "1")
        tela("02-home-apostas", altura = 2600)
    }

    @Test
    fun jogos() {
        dados(); indices(avisos = true)
        tela("03-jogos", "jogos", altura = 2300)
    }

    @Test
    fun paginaDoJogo() {
        dados { it.copy(apostas = it.apostas.filter { a -> a.registrada }) }
        indices()
        tela("04-jogo-quina", "jogos", "modalidade/QUINA", altura = 2000)
    }

    @Test
    fun arrastandoOsDetalhes() {
        dados { it.copy(apostas = it.apostas.filter { a -> a.registrada }) }
        indices()
        tela("04b-arrastando", "jogos", "modalidade/QUINA", altura = 1000, arrastar = 0.45f)
    }

    @Test
    fun detalhesDoJogo() {
        dados(); indices()
        System.setProperty("fotos.detalhes", "1")
        tela("05-detalhes", "jogos", "modalidade/QUINA")
    }

    @Test
    fun apostaEmAndamento() {
        var id = ""
        dados { e ->
            val r = novaAposta(e, Modalidade.MEGA_SENA, null, hoje, System.currentTimeMillis())
                .copy(concurso = 2950, dataSorteio = hoje.plusDays(1), premioEstimado = 12_000_000_000, numeros = listOf(4, 18, 22, 35, 41, 59))
            id = r.id
            Operacoes.salvarRascunho(e.copy(apostas = e.apostas.filter { it.registrada }), r)
        }
        indices()
        tela("06-aposta-andamento", "aposta/$id", altura = 2000)
    }

    @Test
    fun apostaRegistrada() {
        var id = ""
        dados { e ->
            id = e.apostas.first { it.registrada && (it.apuracao?.premio ?: 0) > 0 }.id
            e
        }
        indices()
        tela("07-aposta-registrada", "aposta/$id", altura = 2000)
    }

    @Test
    fun notificacoes() {
        dados { e ->
            val r = novaAposta(e, Modalidade.QUINA, null, hoje, System.currentTimeMillis() - 7_200_000)
                .copy(concurso = 7148, dataSorteio = hoje, numeros = listOf(5, 12, 33, 47, 71))
            Operacoes.salvarRascunho(e, r)
        }
        indices(avisos = true)
        tela("08-notificacoes", "notificacoes")
        tela("08b-home-sino", altura = 900)
    }

    @Test
    fun notificacoesVazia() {
        dados { e -> e.copy(apostas = emptyList(), lancamentos = emptyList(), comprovantes = emptyList(), rateios = emptyList(), cadastradoEm = hoje) }
        indices()
        tela("09-notificacoes-vazia", "notificacoes")
    }

    @Test
    fun ajustes() {
        dados(); indices()
        tela("10-ajustes", "ajustes", altura = 3000)
    }

    // ---------- regra do rascunho (o modelo da página, ao sair) ----------

    private fun sair(vm: ApostaEmEdicao) {
        val loja = ViewModelStore()
        @Suppress("UNCHECKED_CAST")
        val fabrica = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = vm as T
        }
        ViewModelProvider(loja, fabrica)[ApostaEmEdicao::class.java]
        loja.clear()
        esperar(4)
        runBlocking { app.repo.atual() }
        esperar(2)
    }

    private fun rascunhos(): List<Aposta> = runBlocking { Operacoes.rascunhos(app.repo.atual()) }

    @Test
    fun regraDoRascunho() {
        dados { it.copy(apostas = it.apostas.filter { a -> a.registrada }) }
        val e = runBlocking { app.repo.atual() }
        val comprovante = Arquivo("c.pdf", "c.pdf", "application/pdf", 0)

        // Nova, sem nada escolhido (só olhou a página do jogo): descartada sem aviso.
        val vazia = novaAposta(e, Modalidade.QUINA, null, hoje, 1).copy(concurso = 7148)
        app.repo.limparAviso()
        sair(ApostaEmEdicao(app.repo, app.escopo, vazia, nova = true))
        assertTrue(rascunhos().isEmpty())
        assertNull(app.repo.aviso.value)

        // Nova, faltando dezena: descartada, com aviso.
        val metade = ApostaEmEdicao(app.repo, app.escopo, vazia.copy(id = "metade"), nova = true)
        metade.mudar(metade.aposta.copy(numeros = listOf(1, 2, 3)))
        sair(metade)
        assertTrue(rascunhos().isEmpty())
        assertEquals("Aposta descartada: o volante ficou incompleto e sem o comprovante da Caixa.", app.repo.aviso.value)

        // Nova, completa: fica em andamento.
        app.repo.limparAviso()
        val completa = ApostaEmEdicao(app.repo, app.escopo, vazia.copy(id = "completa"), nova = true)
        completa.mudar(completa.aposta.copy(numeros = listOf(1, 2, 3, 4, 5)))
        sair(completa)
        assertEquals(listOf("completa"), rascunhos().map { it.id })
        assertEquals("Aposta em andamento: falta anexar o comprovante da Caixa.", app.repo.aviso.value)

        // Em andamento, o volante limpo (sem comprovante): sai de "Em andamento".
        app.repo.limparAviso()
        val gravada = rascunhos().single()
        val limpa = ApostaEmEdicao(app.repo, app.escopo, gravada, nova = false)
        limpa.mudar(gravada.copy(numeros = emptyList()))
        // Enquanto a página está aberta, continua gravada como estava (completa).
        assertEquals(listOf(1, 2, 3, 4, 5), rascunhos().single().numeros)
        sair(limpa)
        assertTrue(rascunhos().isEmpty())
        assertEquals("Aposta descartada: o volante ficou incompleto e sem o comprovante da Caixa.", app.repo.aviso.value)

        // Nova, faltando dezena mas com o comprovante: fica.
        app.repo.limparAviso()
        val comComprovante = ApostaEmEdicao(app.repo, app.escopo, vazia.copy(id = "comprovante"), nova = true)
        comComprovante.mudar(comComprovante.aposta.copy(numeros = listOf(7), comprovante = comprovante))
        sair(comComprovante)
        assertEquals(listOf("comprovante"), rascunhos().map { it.id })
        assertEquals("Aposta em andamento: falta completar o volante.", app.repo.aviso.value)

        // "Descartar rascunho": sai mesmo com o comprovante.
        val descartar = ApostaEmEdicao(app.repo, app.escopo, rascunhos().single(), nova = false)
        descartar.descartar()
        sair(descartar)
        assertTrue(rascunhos().isEmpty())
    }
}

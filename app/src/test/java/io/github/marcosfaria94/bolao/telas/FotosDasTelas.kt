package io.github.marcosfaria94.bolao.telas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
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
import io.github.marcosfaria94.bolao.data.IndicesDaAposta
import io.github.marcosfaria94.bolao.data.Sessoes
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
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

    /** Altura da tela do celular do Administrador (380dp de largura, tamanho de exibição maior). */
    private val CELULAR = 845

    @Before
    fun antes() {
        Animacoes.ligadas = false
    }

    @After
    fun depois() {
        listOf("fotos.detalhes", "fotos.filtro", "fotos.tudo", "fotos.passo", "fotos.individual").forEach { System.clearProperty(it) }
    }

    private fun esperar(vezes: Int = 8) {
        repeat(vezes) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
            Thread.sleep(40)
        }
    }

    /** Exemplo fixo (semente 7): com uma aposta e um rateio esperando comprovante quando possível. */
    private fun exemplo(modo: Modo = Modo.BOLAO, semente: Long = 7): Estado =
        DadosDeExemplo.gerar(hoje, System.currentTimeMillis(), ZoneId.systemDefault(), semente = semente, modo = modo)

    private fun dados(modo: Modo = Modo.BOLAO, mudar: (Estado) -> Estado = { it }) {
        runBlocking { app.repo.atualizar { mudar(exemplo(modo)) } }
    }

    /** Três apostas registradas que ainda esperam o sorteio (duas no mesmo concurso) e uma que falta registrar. */
    private fun comJogosASortear(e: Estado): Estado {
        var x = e
        val agora = System.currentTimeMillis()
        val q = novaAposta(x, Modalidade.QUINA, null, hoje, agora - 9_000_000)
            .copy(concurso = 7149, dataSorteio = hoje.plusDays(1), numeros = listOf(5, 12, 33, 47, 71), comprovante = Arquivo("q.pdf", "q.pdf", "application/pdf", 0))
        x = Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(x, q), q.id, hoje, agora - 9_000_000)
        val q2 = novaAposta(x, Modalidade.QUINA, null, hoje, agora - 8_000_000)
            .copy(concurso = 7149, dataSorteio = hoje.plusDays(1), numeros = listOf(3, 18, 26, 54, 80))
        x = Operacoes.registrarAposta(Operacoes.salvarRascunho(x, q2), q2.id, hoje, agora - 8_000_000)
        val q3 = novaAposta(x, Modalidade.QUINA, null, hoje, agora - 7_500_000)
            .copy(concurso = 7149, dataSorteio = hoje.plusDays(1), numeros = listOf(1, 9, 27, 44, 63))
        x = Operacoes.salvarRascunho(x, q3)
        val m = novaAposta(x, Modalidade.MEGA_SENA, null, hoje, agora - 7_000_000)
            .copy(concurso = 2950, dataSorteio = hoje.plusDays(2), numeros = listOf(4, 18, 22, 35, 41, 59), comprovante = Arquivo("m.pdf", "m.pdf", "application/pdf", 0))
        x = Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(x, m), m.id, hoje, agora - 7_000_000)
        // A da Mega-Sena já foi enviada ao grupo: fica OK no Painel de Jogos.
        Operacoes.comprovanteDaAposta(x, x.apostas.first { it.id == m.id })?.let { x = Operacoes.registrarEnvio(x, it.id, agora - 6_500_000) }
        val l = novaAposta(x, Modalidade.LOTOMANIA, null, hoje, agora - 6_000_000)
            .copy(concurso = 2790, dataSorteio = hoje.plusDays(2), numeros = (0 until 100 step 2).toList(), comprovante = Arquivo("l.pdf", "l.pdf", "application/pdf", 0))
        return Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(x, l), l.id, hoje, agora - 6_000_000)
    }

    /** Um rateio já publicado (com ganhadores e valores), para os dados ficarem "em dia". */
    private val RATEIO = listOf(FaixaPremio(1, "Principal", 0, 0), FaixaPremio(2, "Secundária", 120, 5_000))

    private fun item(
        m: Modalidade, numero: Int, k1: Double?, k2: Double?, odds: Double?, premio: Long?, sorteio: LocalDate?,
        dezenas: List<Int> = emptyList(), faixas: List<FaixaPremio> = RATEIO, acumulado: Boolean = false,
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

    /**
     * Os dados dos Jogos. Desde a 0.20 (a agenda dos sorteios): os sorteios são de amanhã em diante, para
     * os dados ficarem "em dia" a qualquer hora do CI. [aguardando] = a Quina com o sorteio de ontem (o
     * resultado não chegou, com o erro da Caixa) e a Mega-Sena com o resultado sem o rateio.
     */
    @Suppress("UNCHECKED_CAST")
    private fun indices(avisos: Boolean = false, aguardando: Boolean = false) {
        val mapa = listOf(
            item(
                Modalidade.QUINA, 7148, 0.762, 0.091, 34.0, 5_850_000_000, if (aguardando) hoje.minusDays(1) else hoje.plusDays(1), listOf(5, 12, 33, 47, 71),
                faixas = listOf(FaixaPremio(1, "5 acertos", 0, 0), FaixaPremio(2, "4 acertos", 54, 812_345), FaixaPremio(3, "3 acertos", 4_210, 9_876)),
                acumulado = true,
            ),
            item(
                Modalidade.MEGA_SENA, 2950, 0.701, 0.052, 2332.0, 12_000_000_000, hoje.plusDays(2), listOf(4, 18, 22, 35, 41, 59),
                faixas = if (aguardando) listOf(FaixaPremio(5, "Quina", 41, 0), FaixaPremio(6, "Quadra", 3_200, 0)) else RATEIO,
            ),
            item(Modalidade.LOTOFACIL, 3512, 0.655, 0.312, 11.0, 170_000_000, hoje.plusDays(1), (1..15).toList()),
            // A Caixa ainda não divulgou o prêmio nem a data do próximo concurso: "Não divulgado".
            item(Modalidade.DIA_DE_SORTE, 1125, 0.612, 0.120, 6.0, null, null),
            item(Modalidade.LOTOMANIA, 2780, 0.598, 0.200, 7.0, 380_000_000, hoje.plusDays(3)),
            item(Modalidade.TIMEMANIA, 2290, 0.560, 0.110, 26.0, 1_200_000_000, hoje.plusDays(2)),
            item(Modalidade.SUPER_SETE, 750, 0.520, 0.050, 44.0, 280_000_000, hoje.plusDays(1)),
            item(Modalidade.DUPLA_SENA, 2860, 0.510, 0.070, 1000.0, 400_000_000, hoje.plusDays(3)),
            item(Modalidade.MAIS_MILIONARIA, 300, 0.480, 0.030, 1000.0, 19_000_000_000, hoje.plusDays(9)),
            item(Modalidade.LOTECA, 1215, null, null, null, 150_000_000, hoje.plusDays(3)),
            item(Modalidade.FEDERAL, 6012, null, null, null, 50_000_000, hoje.plusDays(2)),
        ).associateBy { it.modalidade }
        val f = Indices::class.java.getDeclaredField("_itens").apply { isAccessible = true }
        (f.get(app.indices) as MutableStateFlow<Map<Modalidade, Indices.Item>>).value = mapa
        // A última consulta: a da Quina deu erro (só aparece quando ela espera o resultado).
        val c = Indices::class.java.getDeclaredField("_consultas").apply { isAccessible = true }
        val agora = System.currentTimeMillis()
        (c.get(app.indices) as MutableStateFlow<Map<Modalidade, Indices.Consulta>>).value = mapOf(
            Modalidade.QUINA to Indices.Consulta(tentativa = agora - 5 * 60_000, sucesso = agora - 86_400_000, erro = "A Caixa respondeu com erro 403."),
            Modalidade.MEGA_SENA to Indices.Consulta(tentativa = agora - 5 * 60_000, sucesso = agora - 5 * 60_000),
        )
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

    /** Abre o app, vai para [destinos] (um depois do outro) e tira a foto, nos dois temas ([fonte] = tamanho da letra do sistema). */
    private fun tela(nome: String, vararg destinos: String, altura: Int = 1900, fonte: Float = 1f) {
        for (tema in listOf("claro", "escuro")) {
            RuntimeEnvironment.setQualifiers("w380dp-h${altura}dp-${if (tema == "escuro") "night" else "notnight"}-xhdpi")
            RuntimeEnvironment.setFontScale(fonte)
            val c = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            c.get().setContent { BolaoTema { AppRaiz() } }
            esperar()
            for (d in destinos) {
                app.destinoPendente.value = d
                esperar()
            }
            foto(c.get(), "$nome-$tema")
            RuntimeEnvironment.setFontScale(1f)
            c.pause().stop().destroy()
            esperar(2)
        }
    }

    @Test
    fun home() {
        dados { comJogosASortear(it) }; indices()
        tela("01-home", altura = 2600)
        tela("01b-home-celular", altura = CELULAR)
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

    /** Desde a 0.20: a Quina esperando o resultado (com o erro da Caixa) e a Mega-Sena sem o rateio. */
    @Test
    fun jogosAguardandoResultado() {
        dados(); indices(aguardando = true)
        tela("03b-jogos-aguardando", "jogos", altura = 1500)
        tela("03c-jogos-aguardando-celular", "jogos", altura = CELULAR, fonte = 1.3f)
        tela("04c-jogo-aguardando", "jogos", "modalidade/QUINA", altura = 1200)
    }

    /** Desde a 0.20: o volante da Lotomania com a opção "Aposta espelho" ligada. */
    @Test
    fun lotomaniaComEspelho() {
        var id = ""
        dados { e ->
            val r = novaAposta(e, Modalidade.LOTOMANIA, null, hoje, System.currentTimeMillis())
                .copy(concurso = 2790, dataSorteio = hoje.plusDays(2), premioEstimado = 380_000_000, numeros = (0 until 100 step 2).toList(), espelho = true)
            id = r.id
            Operacoes.salvarRascunho(e.copy(apostas = e.apostas.filter { it.registrada }), r)
        }
        indices()
        tela("06b-lotomania-espelho", "aposta/$id", altura = 2600)
    }

    @Test
    fun paginaDoJogo() {
        dados { it.copy(apostas = it.apostas.filter { a -> a.registrada && !a.pendenteDeAnexo }) }
        indices()
        tela("04-jogo-quina", "jogos", "modalidade/QUINA", altura = 2000)
        // Sem o prêmio e a data do próximo concurso (a Caixa ainda não divulgou).
        tela("04b-jogo-nao-divulgado", "jogos", "modalidade/DIA_DE_SORTE", altura = 1100)
    }

    @Test
    fun detalhesDoJogo() {
        dados(); indices()
        System.setProperty("fotos.detalhes", "1")
        tela("05-detalhes", "jogos", "modalidade/QUINA", altura = 2200)
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
        tela("07-aposta-registrada", "aposta/$id", altura = 2200)
        // Com a letra grande do sistema: o "(R$ 124,14)" da situação vai inteiro para a linha de baixo.
        tela("07b-aposta-registrada-letra-grande", "aposta/$id", altura = 1200, fonte = 1.3f)
    }

    @Test
    fun apostaEsperandoComprovante() {
        var id = ""
        dados { e ->
            val r = novaAposta(e, Modalidade.QUINA, null, hoje.minusDays(1), System.currentTimeMillis() - 3_600_000)
                .copy(concurso = 7148, dataSorteio = hoje, numeros = listOf(5, 12, 33, 47, 71))
            id = r.id
            Operacoes.registrarAposta(Operacoes.salvarRascunho(e, r), r.id, hoje, System.currentTimeMillis() - 3_000_000)
        }
        indices()
        tela("08-aposta-esperando", "aposta/$id", altura = 2000)
    }

    /** Uma aposta conferida, com o sorteio no dia [sorteio] e sem o resultado da Caixa (no teste, a Caixa não responde). */
    private fun apostaSemResultado(sorteio: LocalDate): String {
        var id = ""
        dados { e ->
            val r = novaAposta(e, Modalidade.QUINA, null, sorteio.minusDays(1), System.currentTimeMillis() - 90_000_000)
                .copy(concurso = 7148, dataSorteio = sorteio, numeros = listOf(5, 12, 33, 47, 71), comprovante = Arquivo("q.pdf", "q.pdf", "application/pdf", 0))
            id = r.id
            Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(e, r), r.id, sorteio.minusDays(1), System.currentTimeMillis() - 90_000_000)
        }
        indices()
        return id
    }

    @Test
    fun apostaSemResultadoAinda() {
        // No dia do sorteio: "Resultado ainda não apurado", sem tarja e sem o comprovante da apuração.
        val id = apostaSemResultado(hoje)
        tela("08b-aposta-sem-resultado", "aposta/$id", altura = 1900)
    }

    @Test
    fun apostaSemResultadoNoDiaSeguinte() {
        // No dia seguinte ao sorteio, sem o resultado: o erro, com "Tentar de novo" e "Informar à mão".
        val id = apostaSemResultado(hoje.minusDays(1))
        tela("08c-aposta-atrasada", "aposta/$id", altura = 1900)
    }

    @Test
    fun comprovantes() {
        val arq = Arquivo("caixa.pdf", "comprovante-caixa-QN-7148.pdf", "application/pdf", System.currentTimeMillis())
        dados { e ->
            var x = e
            val r = novaAposta(x, Modalidade.QUINA, null, hoje, System.currentTimeMillis() - 3_600_000)
                .copy(concurso = 7148, dataSorteio = hoje, numeros = listOf(5, 12, 33, 47, 71))
            x = Operacoes.registrarAposta(Operacoes.salvarRascunho(x, r), r.id, hoje, System.currentTimeMillis() - 3_000_000)
            val ajustada = x.apostas.first { it.id == r.id }.copy(numeros = listOf(5, 12, 33, 47, 80))
            x = Operacoes.anexarNaAposta(x, r.id, arq, ajustada, listOf("Números: no app 05 12 33 47 71; no comprovante 05 12 33 47 80."))
            val m = novaAposta(x, Modalidade.MEGA_SENA, null, hoje, System.currentTimeMillis() - 2_000_000)
                .copy(concurso = 2950, dataSorteio = hoje.plusDays(1), numeros = listOf(4, 18, 22, 35, 41, 59))
            Operacoes.registrarAposta(Operacoes.salvarRascunho(x, m), m.id, hoje, System.currentTimeMillis() - 1_000_000)
        }
        indices()
        tela("09-comprovantes", "comprovantes", altura = 2400)
    }

    @Test
    fun meusJogos() {
        dados { e ->
            // Desde a 0.21: o k1, o k2 e as odds guardados nas apostas da Quina (de quando foram registradas).
            val x = comJogosASortear(e).let { y ->
                y.copy(apostas = y.apostas.map { a -> if (a.modalidade == Modalidade.QUINA && a.registrada) a.copy(indices = IndicesDaAposta(0.812, 0.094, 34.0)) else a })
            }
            val r = novaAposta(x, Modalidade.TIMEMANIA, null, hoje, System.currentTimeMillis() - 7_200_000)
                .copy(concurso = 2290, dataSorteio = hoje.plusDays(2), numeros = listOf(3, 9, 14, 20, 25, 28, 31, 44, 51, 77), extra = "VASCO DA GAMA/RJ")
            Operacoes.salvarRascunho(x, r)
        }
        indices(avisos = true)
        tela("27-meus-jogos", "meus-jogos", altura = 2900)
        tela("27b-meus-jogos-celular", "meus-jogos", altura = CELULAR)
        // A barra de cima das abas: o ícone do Painel de Jogos (com as pendências no canto).
        tela("10b-home-barra", altura = 900)
        tela("10c-ajustes-barra", "ajustes", altura = 900)
    }

    /** As sessões (desde a 0.21): duas sessões, o modo de teste e o "Criar sessão"; a barra com o nome da sessão. */
    @Test
    fun sessoes() {
        dados(); indices()
        val outra = app.sessoes.criar()
        runBlocking { app.sessoes.repo(outra).atualizar { exemplo(semente = 3) } }
        app.sessoes.nomear(Sessoes.PRINCIPAL, "", Modo.BOLAO)
        app.sessoes.nomear(outra, "Amigos do trabalho", Modo.BOLAO)
        tela("48-sessoes", "sessoes", altura = CELULAR)
        tela("48b-barra-com-a-sessao", altura = 900)
        tela("48c-sessoes-letra-grande", "sessoes", altura = CELULAR, fonte = 1.3f)
    }

    @Test
    fun meusJogosVazio() {
        dados { it.copy(apostas = it.apostas.filter { a -> a.registrada && a.apuracao != null }) }
        indices()
        tela("27c-meus-jogos-vazio", "meus-jogos", altura = 1100)
    }

    @Test
    fun meusJogosEmDia() {
        // Modo individual, sem nada a fazer: o resumo diz "Tudo em dia".
        dados(Modo.INDIVIDUAL) { it.copy(apostas = it.apostas.filter { a -> a.registrada && a.apuracao?.confirmadaEm != null }) }
        indices()
        tela("27d-meus-jogos-em-dia", "meus-jogos", altura = 1100)
    }

    /** A imagem do "Compartilhar": uma aposta de cada formato (e a Lotomania, com 50 números), com o dia do sorteio. */
    @Test
    fun imagemDosJogos() {
        dados()
        val e = runBlocking { app.repo.atual() }
        val agora = System.currentTimeMillis()
        fun aposta(m: Modalidade, concurso: Int, dias: Long = 1, mudar: (Aposta) -> Aposta) =
            mudar(novaAposta(e, m, null, hoje, agora).copy(concurso = concurso, dataSorteio = hoje.plusDays(dias)))
        val apostas = listOf(
            aposta(Modalidade.TIMEMANIA, 2464) { it.copy(numeros = listOf(12, 14, 23, 26, 32, 35, 43, 48, 56, 64), extra = "AMERICA/RN") },
            aposta(Modalidade.QUINA, 7149, dias = 0) { it.copy(numeros = listOf(5, 12, 33, 47, 71)) },
            aposta(Modalidade.MAIS_MILIONARIA, 301, dias = 5) { it.copy(numeros = listOf(3, 11, 19, 27, 38, 49), trevos = listOf(2, 5)) },
            aposta(Modalidade.DIA_DE_SORTE, 1126) { it.copy(numeros = listOf(2, 7, 11, 18, 22, 25, 30), extra = "MARÇO") },
            aposta(Modalidade.SUPER_SETE, 751) { it.copy(numeros = listOf(3, 0, 7, 1, 9, 4, 6)) },
            aposta(Modalidade.LOTOMANIA, 2790) { it.copy(numeros = (0 until 100 step 2).toList()) },
            aposta(Modalidade.LOTECA, 1216) { it.copy(palpitesLoteca = listOf("1", "X", "2", "1", "1", "X", "2", "2", "1", "X", "1", "2", "X", "1")) },
            aposta(Modalidade.FEDERAL, 6013) { it.copy(bilheteFederal = "012345") },
        )
        val arquivo = io.github.marcosfaria94.bolao.imagem.ImagemDosJogos.salvar(app, apostas, hoje, teste = false, File(pasta, "40-imagem-jogos.png"))
        io.github.marcosfaria94.bolao.imagem.ImagemDosJogos.salvar(app, apostas.take(2), hoje, teste = true, File(pasta, "40b-imagem-jogos-teste.png"))
        // Leve: bem menos de 200 KB, mesmo com 8 apostas.
        assertTrue("A imagem tem ${arquivo.length()} bytes.", arquivo.length() < 200_000)
    }

    /** A página do cálculo do k1 e do k2 (tocar na Legenda dos Jogos), com o exemplo do jogo em destaque. */
    @Test
    fun calculoDosIndices() {
        dados(); indices()
        tela("41-indices", "jogos", "indices", altura = 2900)
        tela("41b-indices-celular", "jogos", "indices", altura = CELULAR)
    }

    @Test
    fun ajustes() {
        dados(); indices()
        tela("11-ajustes", "ajustes", altura = CELULAR)
        // A lista inteira, com os Créditos no rodapé.
        tela("11b-ajustes-rodape", "ajustes", altura = 2200)
    }

    @Test
    fun modoIndividual() {
        dados(Modo.INDIVIDUAL); indices()
        tela("12-individual-home", altura = 2400)
        tela("13-individual-ajustes", "ajustes", altura = 1300)
    }

    @Test
    fun cadastro() {
        runBlocking { app.repo.atualizar { Estado() } }
        System.setProperty("fotos.passo", "1")
        tela("14-cadastro-modo", altura = 1500)
        System.setProperty("fotos.passo", "2")
        tela("15-cadastro-integrantes", altura = 2400)
    }

    @Test
    fun filtrar() {
        dados(); indices()
        tela("16-filtrar", "filtro", altura = 1500)
        tela("16b-filtrar-celular", "filtro", altura = CELULAR)
        System.setProperty("fotos.filtro", "APOSTAS")
        tela("16c-filtrar-apostas", "filtro", altura = 1500)
    }

    @Test
    fun comprovanteDaApuracao() {
        var id = ""
        dados { e ->
            id = e.comprovantes.last { it.tipo == TipoComprovante.APURACAO }.id
            e
        }
        indices()
        tela("18-comprovante-apuracao", "comprovante/$id", altura = 2600)
    }

    @Test
    fun detalhesDireto() {
        dados(); indices()
        tela("19-detalhes-direto", "jogos", "detalhes/QUINA", altura = 1200)
    }

    @Test
    fun saldo() {
        dados(); indices()
        tela("26-saldo", "saldo", altura = 1400)
    }

    @Test
    fun telasDosAjustes() {
        dados(); indices()
        tela("20-ajustes-notificacoes", "ajustes", "ajustes/notificacoes", altura = 2300)
        tela("22-ajustes-valores", "ajustes", "ajustes/valores", altura = 1100)
        tela("24-ajustes-integrantes", "ajustes", "ajustes/integrantes", altura = 900)
        tela("25-ajustes-extincao", "ajustes", "ajustes/extincao", altura = 1100)
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
        assertEquals("Aposta descartada: o volante ficou incompleto.", app.repo.aviso.value)

        // Nova, completa: fica em andamento.
        app.repo.limparAviso()
        val completa = ApostaEmEdicao(app.repo, app.escopo, vazia.copy(id = "completa"), nova = true)
        completa.mudar(completa.aposta.copy(numeros = listOf(1, 2, 3, 4, 5)))
        sair(completa)
        assertEquals(listOf("completa"), rascunhos().map { it.id })
        assertEquals("Aposta em andamento: falta registrar.", app.repo.aviso.value)

        // Em andamento, o volante limpo: sai de "Em andamento".
        app.repo.limparAviso()
        val gravada = rascunhos().single()
        val limpa = ApostaEmEdicao(app.repo, app.escopo, gravada, nova = false)
        limpa.mudar(gravada.copy(numeros = emptyList()))
        // Enquanto a página está aberta, continua gravada como estava (completa).
        assertEquals(listOf(1, 2, 3, 4, 5), rascunhos().single().numeros)
        sair(limpa)
        assertTrue(rascunhos().isEmpty())
        assertEquals("Aposta descartada: o volante ficou incompleto.", app.repo.aviso.value)

        // Versões antigas: faltando dezena mas com o comprovante, fica.
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

    /** O volante acompanha a Caixa: o concurso que já foi sorteado passa para o próximo. */
    @Test
    fun volanteAcompanhaOConcurso() {
        dados { it.copy(apostas = it.apostas.filter { a -> a.registrada }) }
        val e = runBlocking { app.repo.atual() }
        val ultimo = Concurso(
            modalidade = Modalidade.QUINA, numero = 7148, data = hoje, dezenas = listOf(1, 2, 3, 4, 5),
            proximoNumero = 7149, proximaData = hoje.plusDays(1), premioEstimadoProximo = 700_000_000,
        )
        // Aberta com o concurso 7148 (já sorteado): passa para o 7149, com o sorteio e o prêmio.
        val vm = ApostaEmEdicao(app.repo, app.escopo, novaAposta(e, Modalidade.QUINA, null, hoje, 1).copy(concurso = 7148), nova = true)
        val (antes, depois) = vm.acompanharConcurso(ultimo, 7149, hoje)!!
        assertEquals(7148, antes.concurso)
        assertEquals(7149, depois.concurso)
        assertEquals(hoje.plusDays(1), vm.aposta.dataSorteio)
        assertEquals(700_000_000L, vm.aposta.premioEstimado)
        // Já no concurso certo: nada muda.
        assertNull(vm.acompanharConcurso(ultimo, 7149, hoje))
        // Sem concurso nenhum (dados velhos ao abrir): recebe o próximo.
        val semConcurso = ApostaEmEdicao(app.repo, app.escopo, novaAposta(e, Modalidade.QUINA, null, hoje, 1).copy(id = "sem", concurso = 0), nova = true)
        assertEquals(7149, semConcurso.acompanharConcurso(ultimo, 7149, hoje)!!.second.concurso)
        // A Loteca não muda sozinha (os 14 jogos são de cada concurso).
        val loteca = ApostaEmEdicao(app.repo, app.escopo, novaAposta(e, Modalidade.LOTECA, null, hoje, 1).copy(id = "lt", concurso = 1200), nova = true)
        assertNull(loteca.acompanharConcurso(ultimo.copy(modalidade = Modalidade.LOTECA), 1201, hoje))
        // No concurso certo, sem a data (não divulgada): quando a Caixa divulga, a data entra sozinha, sem aviso.
        val semData = ApostaEmEdicao(app.repo, app.escopo, novaAposta(e, Modalidade.QUINA, null, hoje, 1).copy(id = "sd", concurso = 7149), nova = true)
        assertNull(semData.aposta.dataSorteio)
        assertNull(semData.acompanharConcurso(ultimo, 7149, hoje))
        assertEquals(hoje.plusDays(1), semData.aposta.dataSorteio)
        assertEquals(700_000_000L, semData.aposta.premioEstimado)
        sair(vm); sair(semConcurso); sair(loteca); sair(semData)
    }
}

package io.github.marcosfaria94.bolao.ui.caixa

import androidx.compose.material3.Surface
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.BoxScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlin.math.abs
import kotlin.math.hypot
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.view.ViewGroup
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.marcosfaria94.bolao.data.Downloads
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Efetivacao
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Transcricao
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.IndicadorDoPuxao
import io.github.marcosfaria94.bolao.ui.comum.PuxaoAlemDaBorda
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.resume

/**
 * A tela da Caixa (desde a 0.31): o Loterias Online da Caixa dentro do app, pelo botão do trevo na barra de baixo.
 * Desde a 0.32 fica em terceiro na barra (Home, Jogos, Caixa, Ajustes); "caixa" é o endereço dela (abrir("caixa") vai
 * para ela). Na 0.36 e na 0.37 ela abria só pelo botão; desde a 0.38 (pedido do usuário) ela volta para a roda das abas
 * e tem o cadeado ([BotaoDoCadeado]; desde a 0.39 com o tamanho, a cor e a transparência do ícone dos cookies do site,
 * logo acima dele ou espelhado do outro lado): aberto, deslizar o dedo para os lados na página passa para a próxima
 * tela; fechado, o dedo fica todo com o site. Desde a 0.39 as apostas vão para o carrinho só pelo "Transcrever em lote".
 */
const val ROTA_CAIXA = "caixa"

/** O título da tela da Caixa (desde a 0.44 "Loteria", pedido do usuário: "troque o nome Caixa por Loteria"; até a 0.43, "Loterias Caixa"). */
const val TITULO_CAIXA = "Loteria"

/** O nome do botão da barra de baixo (desde a 0.44; até a 0.43, "Caixa"). */
const val ROTULO_CAIXA = "Loteria"

/** O botão de recarregar da barra de cima (de volta desde a 0.44; saiu na 0.42). */
const val RECARREGAR_A_PAGINA = "Recarregar a página"


/** Quantas vezes o app pergunta ao script se ele terminou (de 300 em 300 ms: 45 segundos). */
private const val PERGUNTAS = 150

/** As fases do deslize para os lados na página, com o cadeado aberto (desde a 0.38): o valor é o arrasto (px) ou, no fim, a velocidade (px/s). */
enum class FaseDoDeslize { COMECO, ARRASTO, FIM }

/**
 * Onde ficou o ícone dos cookies do site (desde a 0.38), em frações da página: o meio dele, o alto, a largura e (desde a
 * 0.39) a altura (0 = igual à largura), se ficou à [direita], a [cor] de fundo dele (ARGB; null = não lida) e a
 * [opacidade] dele. [naPagina] (desde a 0.41): o script pôs o cadeado na própria página, copiado do ícone dos cookies
 * (o app não desenha o dele).
 */
data class PosicaoDoCookie(
    val x: Float,
    val topo: Float,
    val largura: Float,
    val altura: Float = 0f,
    val direita: Boolean = x >= 0.5f,
    val cor: Int? = null,
    val opacidade: Float = 1f,
    val naPagina: Boolean = false,
)

/** O que o app lê para saber se o script terminou: o JSON da resposta, ou null. */
private const val LER_RESPOSTA =
    "(function(){var e=window.__bolaoTranscricao;return e&&e.fim?JSON.stringify(e.fim):null})()"

/**
 * O navegador da tela da Caixa: a página (um WebView só, guardado enquanto o app está aberto nesta sessão, para
 * voltar à tela e encontrar a página onde estava, com a conta aberta) e o que a tela mostra dela. As apostas que o
 * "Transcrever" já pôs no carrinho ficam em [transcritas] (nesta abertura do app), para não irem de novo.
 *
 * Desde a 0.33 a página fica "congelada": o WebView não mora mais dentro da aba (que sai da tela e volta a cada
 * passagem, e a página se redesenhava e recarregava toda vez que se passava por ela a caminho dos Ajustes), mas numa
 * camada fixa por cima das abas ([PaginaFixaDaCaixa]), presa à janela enquanto a sessão está aberta; a aba da Caixa só
 * reserva o lugar ([medida]) e a camada anda junto com as abas.
 */
class NavegadorDaCaixa(private val contexto: Context) {

    init {
        ultimo = java.lang.ref.WeakReference(this)
    }

    internal companion object {
        /** O navegador de agora (só para os testes das telas mostrarem as situações da área de efetivar). */
        @Volatile
        internal var ultimo: java.lang.ref.WeakReference<NavegadorDaCaixa>? = null
    }

    /** O lugar da página na aba da Caixa (o alto e o tamanho, px na raiz), medido pela aba; null = a aba nunca abriu. */
    var medida by mutableStateOf<MedidaDaPagina?>(null)

    /** O endereço da página de agora (o site muda só a parte depois do "#"; o WebViewClient atualiza). */
    var endereco by mutableStateOf<String?>(null)

    /** O carregamento da página (0 a 100). */
    var progresso by mutableIntStateOf(100)
        private set

    /** A página não abriu (sem internet, site fora do ar). */
    var erro by mutableStateOf<String?>(null)
        private set

    /** O "Transcrever" está trabalhando na página. */
    var transcrevendo by mutableStateOf(false)
        private set

    /** Um aviso curto para a tela mostrar (download começado...). */
    var aviso by mutableStateOf<String?>(null)

    /** As apostas que já foram para o carrinho do site (pelos ids). */
    val transcritas = mutableStateListOf<String>()

    private val prefs = contexto.getSharedPreferences("app", Context.MODE_PRIVATE)

    /**
     * O cadeado (desde a 0.38): aberto (o padrão), deslizar o dedo para os lados na página passa para a próxima tela;
     * fechado, rola a página (o dedo fica todo com o site, como na 0.36 e na 0.37). Fica guardado.
     */
    var destravada by mutableStateOf(!prefs.getBoolean(CADEADO_FECHADO, false))
        private set

    fun alternarCadeado() {
        destravada = !destravada
        prefs.edit().putBoolean(CADEADO_FECHADO, !destravada).apply()
        aviso = if (destravada) AVISO_CADEADO_ABERTO else AVISO_CADEADO_FECHADO
        // O cadeado da página (desde a 0.41) troca o desenho também.
        web?.evaluateJavascript("window.__bolaoCadeadoEstado&&window.__bolaoCadeadoEstado($destravada)", null)
    }

    /**
     * Abre o cadeado sem aviso (desde a 0.44, pedido do usuário: "permita que o cadeado se abra ao trocar de tela clicando
     * no botão de outra tela"): sair da Caixa pela barra de baixo deixa o cadeado aberto para a volta.
     */
    fun abrirCadeado() {
        if (destravada) return
        destravada = true
        prefs.edit().putBoolean(CADEADO_FECHADO, false).apply()
        web?.evaluateJavascript("window.__bolaoCadeadoEstado&&window.__bolaoCadeadoEstado(true)", null)
    }

    /**
     * A ponte do cadeado que fica dentro da página (desde a 0.41; assets/caixa/cookies.js): o toque nele chama
     * [alternar] (na linha da ponte; a troca vai para a linha das telas) e o desenho pergunta se está [aberto]. A página só
     * consegue abrir e fechar o cadeado (nada mais do app passa por aqui).
     */
    private inner class PonteDoCadeado {
        @android.webkit.JavascriptInterface
        fun aberto(): Boolean = destravada

        @android.webkit.JavascriptInterface
        fun alternar(): Boolean {
            val novo = !destravada
            principal.post { if (destravada != novo) alternarCadeado() }
            return novo
        }

        /** O botão "Efetivar apostas" da página (desde a 0.42) foi tocado: a tela abre a área. */
        @android.webkit.JavascriptInterface
        fun efetivar() {
            principal.post { pedirEfetivar = true }
        }

        /** Quantas apostas esperam (o número no botão da página). */
        @android.webkit.JavascriptInterface
        fun pendentes(): Int = pendentesNaPagina

        /**
         * O dedo encostou na página (desde a 0.44): [noTopo] = a página (e a parte rolável em que o dedo está) já está no
         * alto. Só assim puxar para baixo abre o Painel de Jogos (no meio da página, puxar para baixo rola o site).
         */
        @android.webkit.JavascriptInterface
        fun toque(noTopo: Boolean) {
            toqueNoTopo = noTopo
            toqueEm = android.os.SystemClock.uptimeMillis()
        }
    }

    @Volatile
    private var toqueNoTopo = true

    @Volatile
    private var toqueEm = 0L

    /** Quem acompanha o puxão para baixo no alto da página (a raiz, que abre o Painel de Jogos; desde a 0.44); null = ninguém. */
    var aoPuxar: ((FaseDoDeslize, Float) -> Unit)? = null

    /** A página passa o puxão para baixo (no alto dela) para a raiz. */
    fun puxar(fase: FaseDoDeslize, valor: Float) {
        aoPuxar?.invoke(fase, valor)
    }

    /**
     * Puxar para baixo pode abrir o Painel de Jogos no toque que começou em [tocouEm]: alguém acompanha e a página disse que
     * está no alto (sem resposta da página para este toque, vale o que o WebView sabe da rolagem).
     */
    fun podePuxar(tocouEm: Long): Boolean = aoPuxar != null && (toqueEm < tocouEm - 50 || toqueNoTopo)

    private val principal = android.os.Handler(android.os.Looper.getMainLooper())

    /** Quem acompanha o deslize para os lados (a raiz, que move as abas); null = ninguém. */
    var aoDeslizar: ((FaseDoDeslize, Float) -> Unit)? = null

    /** A página passa o deslize para os lados para as abas (cadeado aberto). */
    fun deslizar(fase: FaseDoDeslize, valor: Float) {
        aoDeslizar?.invoke(fase, valor)
    }

    /** Onde ficou o ícone dos cookies do site (o cadeado vai logo acima); null = não achado (o cadeado fica no canto). */
    var cookie by mutableStateOf<PosicaoDoCookie?>(null)
        private set

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cookies: Job? = null

    /** O script que leva o ícone dos cookies para a direita (assets/caixa/cookies.js). */
    private val scriptDosCookies: String by lazy { contexto.assets.open("caixa/cookies.js").bufferedReader().use { it.readText() } }

    /** Leva o ícone dos cookies para a direita e lê onde ele ficou, algumas vezes (o site monta o botão depois de abrir). */
    private fun cuidarDosCookies(w: WebView) {
        cookies?.cancel()
        cookies = escopo.launch {
            for (espera in listOf(300L, 1_200L, 2_500L, 5_000L)) {
                delay(espera)
                runCatching { withTimeoutOrNull(2_000) { avaliar(w, scriptDosCookies) } }
                val lido = runCatching { withTimeoutOrNull(2_000) { avaliar(w, "window.__bolaoCookie||null") } }.getOrNull()
                // Desde a 0.44 o que a página diz vale sempre: sem os botões na página (null), o app desenha os dele. Até a
                // 0.43 ficava a leitura de antes, e os botões sumiam quando o site redesenhava a página (nem os da página
                // nem os do app apareciam).
                cookie = Transcricao.textoDoJavascript(lido)?.let(::lerCookie)
            }
        }
    }

    private var web: WebView? = null

    /** O endereço que não abriu (para o "Tentar de novo" ir nele, e não recarregar a página de antes). */
    private var falhou: String? = null

    /** Quantas vezes seguidas um endereço http:// virou https:// (desde a 0.36; para não ficar indo e voltando). */
    private var trocasParaHttps = 0

    /** O script do "Transcrever" (assets/caixa/transcrever.js). */
    private val script: String by lazy { contexto.assets.open("caixa/transcrever.js").bufferedReader().use { it.readText() } }

    /** O script que lê se a conta está aberta no site (assets/caixa/sessao.js; desde a 0.42). */
    private val scriptDaSessao: String by lazy { contexto.assets.open("caixa/sessao.js").bufferedReader().use { it.readText() } }

    /** O WebView da tela (criado na primeira vez, com a tela de agora; nas outras, o mesmo, já com a página). */
    fun webView(tela: Context): WebView {
        val w = web ?: criar(tela).also { web = it }
        (w.parent as? ViewGroup)?.removeView(w)
        return w
    }

    /** Fecha a página de vez (ao trocar de sessão ou fechar o app). */
    fun destruir() {
        escopo.cancel()
        web?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        web = null
    }

    /** O voltar do celular: a página de antes do site. False quando não há página antes (a tela fecha). */
    fun voltar(): Boolean {
        val w = web ?: return false
        if (!w.canGoBack()) return false
        w.goBack()
        return true
    }

    /**
     * O "Recarregar a página" da barra de cima e o "Tentar de novo" do aviso de erro. Desde a 0.36, depois de um erro,
     * vai de novo no endereço que não abriu (com https:// no lugar de http://); até a 0.35 recarregava a página, que
     * podia ser o próprio endereço bloqueado (net::ERR_CLEARTEXT_NOT_PERMITTED), e o erro só saía reiniciando o app.
     */
    fun recarregar() {
        erro = null
        trocasParaHttps = 0
        val w = web ?: return
        val alvo = falhou ?: w.url
        falhou = null
        when {
            alvo.isNullOrBlank() || !alvo.startsWith("http", ignoreCase = true) -> w.loadUrl(Transcricao.ENDERECO)
            enderecoSeguro(alvo) != null -> w.loadUrl(enderecoSeguro(alvo)!!)
            alvo == w.url -> w.reload()
            else -> w.loadUrl(alvo)
        }
    }

    /** A página inicial do site (o botão do aviso de erro, desde a 0.36): sai de qualquer página que não abre. */
    fun paginaInicial() {
        erro = null
        falhou = null
        trocasParaHttps = 0
        web?.loadUrl(Transcricao.ENDERECO)
    }

    /** Tocar no botão da Caixa já na tela da Caixa: a página volta ao começo (como as listas das abas). */
    fun paraOTopo() {
        web?.pageUp(true)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun criar(tela: Context): WebView = PaginaDaCaixa(tela, this).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportMultipleWindows(false)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // O site como no Chrome do celular (sem a marca "wv" do WebView, que alguns sites recusam).
        runCatching {
            settings.userAgentString = WebSettings.getDefaultUserAgent(tela).replace("; wv", "").replace(" Version/4.0", "")
        }
        runCatching {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
        addJavascriptInterface(PonteDoCadeado(), PONTE_DO_CADEADO)
        webViewClient = Cliente()
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progresso = newProgress
            }

            // O site não precisa da localização.
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                callback?.invoke(origin, false, false)
            }
        }
        setDownloadListener { url, agente, disposicao, tipo, _ -> baixar(url, agente, disposicao, tipo) }
        loadUrl(endereco?.takeIf { it.startsWith("http") } ?: Transcricao.ENDERECO)
    }

    private inner class Cliente : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            return when (uri.scheme?.lowercase()) {
                "https" -> false
                // Desde a 0.36: um link (ou redirecionamento) para http:// vai pelo https:// (o Android não abre
                // endereço sem cadeado: dava net::ERR_CLEARTEXT_NOT_PERMITTED).
                "http" -> {
                    val seguro = enderecoSeguro(uri.toString())
                    if (seguro == null || trocasParaHttps >= LIMITE_DE_TROCAS) return false
                    trocasParaHttps++
                    view.post { view.loadUrl(seguro) }
                    true
                }
                // Pagamento (Pix, app do banco), telefone, e-mail...: o app de fora.
                else -> {
                    abrirFora(view, uri.toString())
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            erro = null
            endereco = url
            // Página nova (não só o "#"): os botões da página saem junto; até o script pôr os novos, o app mostra os dele.
            cookie = null
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            endereco = url
            // O site muda de página sem recarregar (só a parte depois do "#"): o ícone dos cookies de novo.
            cuidarDosCookies(view)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            endereco = url
            if (erro == null && url?.startsWith("https://") == true) trocasParaHttps = 0
            runCatching { CookieManager.getInstance().flush() }
            cuidarDosCookies(view)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (!request.isForMainFrame) return
            val url = request.url.toString()
            // Uma página http:// que escapou (ex.: um formulário): vai de novo, pelo https:// (desde a 0.36).
            val seguro = enderecoSeguro(url)
            if (seguro != null && trocasParaHttps < LIMITE_DE_TROCAS) {
                trocasParaHttps++
                view.post { view.loadUrl(seguro) }
                return
            }
            falhou = url
            erro = "O site da Caixa não abriu (${error.description})."
        }
    }

    private fun abrirFora(view: WebView, url: String) {
        val intent = runCatching {
            if (url.startsWith("intent:")) {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
        }.getOrNull() ?: return
        try {
            view.context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val reserva = intent.getStringExtra("browser_fallback_url")
            if (reserva != null && (reserva.startsWith("https://") || reserva.startsWith("http://"))) view.loadUrl(reserva)
            else aviso = "Não há app no celular para abrir isso."
        } catch (_: SecurityException) {
            aviso = "Não há app no celular para abrir isso."
        }
    }

    /** Os arquivos do site (o comprovante da aposta, por exemplo) vão para Downloads/Bolao. */
    private fun baixar(url: String, agente: String?, disposicao: String?, tipo: String?) {
        val nome = URLUtil.guessFileName(url, disposicao, tipo)
        when {
            url.startsWith("http://") || url.startsWith("https://") -> runCatching {
                val pedido = DownloadManager.Request(Uri.parse(url))
                    .addRequestHeader("User-Agent", agente.orEmpty())
                    .setMimeType(tipo)
                    .setTitle(nome)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Downloads.PASTA + "/" + nome)
                CookieManager.getInstance().getCookie(url)?.let { pedido.addRequestHeader("Cookie", it) }
                (contexto.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(pedido)
                aviso = "Baixando $nome em Downloads/${Downloads.PASTA}."
            }.onFailure { aviso = "Não deu para baixar o arquivo." }
            // Arquivo feito pela própria página (blob:): a página lê e entrega em base64.
            url.startsWith("blob:") -> web?.let { w -> CoroutineScope(Dispatchers.Main).launch { baixarDaPagina(w, url, nome, tipo) } }
            else -> aviso = "Não deu para baixar o arquivo."
        }
    }

    private suspend fun baixarDaPagina(w: WebView, url: String, nome: String, tipo: String?) {
        val js = "(function(){window.__bolaoArquivo=null;fetch(" + Transcricao.textoParaJavascript(url) + ").then(function(r){return r.blob()})" +
            ".then(function(b){var f=new FileReader();f.onload=function(){window.__bolaoArquivo=String(f.result)};f.readAsDataURL(b)})" +
            ".catch(function(){window.__bolaoArquivo='erro'});return 'ok'})()"
        avaliar(w, js)
        repeat(60) {
            delay(250)
            val dado = Transcricao.textoDoJavascript(avaliar(w, "window.__bolaoArquivo")) ?: return@repeat
            if (dado == "erro" || !dado.contains(",")) {
                aviso = "Não deu para baixar o arquivo."
                return
            }
            val bytes = Base64.decode(dado.substringAfter(','), Base64.DEFAULT)
            runCatching {
                Downloads.salvar(contexto, nome, tipo ?: "application/octet-stream") { it.write(bytes) }
                aviso = "$nome salvo em Downloads/${Downloads.PASTA}."
            }.onFailure { aviso = "Não deu para salvar o arquivo." }
            return
        }
        aviso = "Não deu para baixar o arquivo."
    }

    private suspend fun avaliar(w: WebView, js: String): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { c -> w.evaluateJavascript(js) { if (c.isActive) c.resume(it) } }
    }

    /**
     * O botão "Efetivar apostas" (desde a 0.42, no lugar do "Transcrever em lote" da barra de cima; na página, junto do
     * cadeado) foi tocado: a tela da Caixa abre a área.
     */
    var pedirEfetivar by mutableStateOf(false)

    /** O lote está rodando (a área não fecha: o site está sendo usado). */
    var efetivando by mutableStateOf(false)
        private set

    /** Como está cada aposta da área (pela chave da linha), no lote de agora e nos de antes (nesta abertura do app). */
    val situacoes = mutableStateMapOf<String, Efetivacao.Situacao>()

    /** A sessão do site, lida na última vez (ao abrir a área, antes do lote e a cada página). */
    var sessao by mutableStateOf(Efetivacao.Sessao.VERIFICANDO)
        internal set

    /** Quantas apostas esperam (o número que o botão da página mostra; a ponte lê daqui). */
    @Volatile
    private var pendentesNaPagina = 0

    /** Quantas apostas esperam, para o botão do app (quando o da página não aparece). */
    var pendentes by mutableIntStateOf(0)
        private set

    /** Avisa a página de quantas apostas esperam (o botão chama atenção com uma ou mais). */
    fun mostrarPendentes(n: Int) {
        pendentesNaPagina = n
        pendentes = n
        web?.evaluateJavascript("window.__bolaoEfetivarEstado&&window.__bolaoEfetivarEstado($n)", null)
    }

    /** Lê se a conta está aberta no site (até 3 s; sem resposta, desconhecida). */
    suspend fun verificarSessao(): Efetivacao.Sessao {
        // Sem a página (nos testes das telas), fica a que estava.
        val w = web ?: return sessao.takeIf { it != Efetivacao.Sessao.VERIFICANDO } ?: Efetivacao.Sessao.DESCONHECIDA.also { sessao = it }
        sessao = Efetivacao.Sessao.VERIFICANDO
        val lido = runCatching { withTimeoutOrNull(3_000) { avaliar(w, scriptDaSessao) } }.getOrNull()
        return Efetivacao.sessao(Transcricao.textoDoJavascript(lido)).also { sessao = it }
    }

    /**
     * Efetiva as [linhas] (desde a 0.42; o "Transcrever em lote" da 0.38): antes de tudo, confere a sessão do site (com a
     * conta fechada, nada vai); depois, página por página (a sessão de novo a cada página), marca cada aposta no volante e
     * põe no carrinho. Uma aposta só conta como no carrinho quando o contador do site sobe; se a sessão cair no meio, o
     * lote para e as que faltam ficam "não enviadas" (nada vai para o limbo). O login e o pagamento continuam no site.
     */
    suspend fun efetivar(linhas: List<Efetivacao.Linha>) {
        if (efetivando || linhas.isEmpty()) return
        val w = web ?: return
        efetivando = true
        try {
            linhas.forEach { situacoes[it.chave] = Efetivacao.Situacao.NaFila }
            if (verificarSessao() == Efetivacao.Sessao.DESLOGADA) {
                linhas.forEach { situacoes[it.chave] = Efetivacao.Situacao.NaoEnviada }
                return
            }
            var parou = false
            for ((pagina, daPagina) in linhas.groupBy { it.pagina }) {
                if (parou) break
                val chegada = irParaAPagina(w, pagina)
                val chegou = chegada != Chegada.NAO
                if (chegou && verificarSessao() == Efetivacao.Sessao.DESLOGADA) {
                    parou = true
                    break
                }
                for (l in daPagina) {
                    situacoes[l.chave] = Efetivacao.Situacao.Indo
                    // Mandada de novo (estava no carrinho): só volta ao carrinho se o site aceitar agora.
                    transcritas.removeAll(l.item.ids)
                    val r = if (!chegou) {
                        Transcricao.Resposta(mensagem = mensagemDaPaginaQueNaoAbriu(nomeDaPagina(pagina), ondeFicou))
                    } else {
                        // Na outra página da modalidade (a normal no lugar da do especial, ou o contrário), o script confere o concurso.
                        transcrever(l.item, conferirConcurso = chegada == Chegada.OUTRA)
                    }
                    transcritas.addAll(Transcricao.transcritas(l.item, r).filter { it !in transcritas })
                    situacoes[l.chave] = if (r.noCarrinho) Efetivacao.Situacao.NoCarrinho else Efetivacao.Situacao.Falhou(r.mensagem, r.sessao, r.parcial)
                    if (r.sessao) {
                        sessao = Efetivacao.Sessao.DESLOGADA
                        parou = true
                        break
                    }
                }
            }
            if (parou) {
                linhas.filter { situacoes[it.chave] == Efetivacao.Situacao.NaFila }.forEach { situacoes[it.chave] = Efetivacao.Situacao.NaoEnviada }
            }
        } finally {
            efetivando = false
        }
    }

    /** Como o lote chegou à página de uma modalidade: não chegou, a própria página ou a outra da mesma modalidade. */
    private enum class Chegada { NAO, EXATA, OUTRA }

    /** Onde o site ficou na última página que não abriu (a parte do "#"), para a mensagem; null = não se sabe. */
    private var ondeFicou: String? = null

    /**
     * Muda a página do site para a [pagina] (desde a 0.44, com mais caminhos): primeiro o endereço dela e, se o site não
     * abrir, o da outra página da mesma modalidade (na Quina Friday, 2026, a Caixa vendeu a Quina só para o concurso
     * especial e o "#/quina" não abria); por último, o link da modalidade na própria página. Vale chegar a qualquer página
     * da modalidade (o site pode levar de uma para a outra sozinho): fora da página certa, o script confere o concurso.
     */
    private suspend fun irParaAPagina(w: WebView, pagina: Transcricao.Pagina): Chegada {
        ondeFicou = null
        if (Transcricao.pagina(endereco) == pagina) return Chegada.EXATA
        for (fragmento in Transcricao.fragmentos(pagina)) {
            avaliar(w, "(function(){location.hash=" + Transcricao.textoParaJavascript(fragmento) + ";return 'ok'})()")
            esperarAPagina(pagina, vezes = 24)?.let { return it }
        }
        val tocou = runCatching { Transcricao.textoDoJavascript(avaliar(w, Transcricao.scriptDoLink(pagina))) }.getOrNull()
        if (tocou == "ok") esperarAPagina(pagina, vezes = 32)?.let { return it }
        ondeFicou = endereco?.substringAfter('#', "")?.substringBefore('?')?.takeIf { it.isNotBlank() }?.let { "#$it" }
        return Chegada.NAO
    }

    /** Espera (de 250 em 250 ms, [vezes] vezes) o endereço chegar a uma página da modalidade da [pagina] e o volante aparecer. */
    private suspend fun esperarAPagina(pagina: Transcricao.Pagina, vezes: Int): Chegada? {
        repeat(vezes) {
            delay(250)
            val agora = Transcricao.pagina(endereco)
            if (agora?.modalidade == pagina.modalidade) {
                // O volante da página nova (o script ainda espera por ele, se demorar).
                delay(1_200)
                return if (agora == pagina) Chegada.EXATA else Chegada.OUTRA
            }
        }
        return null
    }

    /**
     * Transcreve o [item] na página de agora: o script marca o volante do site, confere e põe no carrinho. O app
     * pergunta de tempos em tempos se terminou.
     */
    suspend fun transcrever(item: Transcricao.Item, conferirConcurso: Boolean = false): Transcricao.Resposta {
        val w = web ?: return Transcricao.Resposta(mensagem = "A página da Caixa não está aberta.")
        transcrevendo = true
        try {
            val plano = if (conferirConcurso) item.plano.copy(conferirConcurso = true) else item.plano
            val inicio = Transcricao.textoDoJavascript(avaliar(w, "(" + script + ")(" + Transcricao.json(plano) + ")"))
            if (inicio == "ocupado") return Transcricao.Resposta(mensagem = "O app ainda está marcando outra aposta na página. Espere um pouco.")
            if (inicio != "iniciado") return Transcricao.Resposta(mensagem = "A página da Caixa não deixou o app marcar o volante. Espere a página abrir e efetive de novo.")
            repeat(PERGUNTAS) {
                delay(300)
                Transcricao.resposta(Transcricao.textoDoJavascript(avaliar(w, LER_RESPOSTA)))?.let { return it }
            }
            return Transcricao.Resposta(mensagem = "O site demorou demais para responder. Confira o volante e o carrinho.")
        } finally {
            transcrevendo = false
        }
    }
}

/**
 * A página do site. Com o cadeado fechado, o arrasto para os lados fica com o site (como na 0.36 e na 0.37): rolar, tocar e
 * ampliar são do site. Com o cadeado aberto (desde a 0.38, o padrão), o arrasto para os lados ([ehParaOsLados]) deixa de ser
 * do site e vai para as abas ([NavegadorDaCaixa.deslizar]): a tela passa para a vizinha. Desde a 0.44 (pedido do usuário:
 * "faça abrir o Painel de Jogos ao empurrar a tela da Caixa pra baixo"), puxar para baixo com a página no alto
 * ([ehParaBaixo]) vai para a raiz ([NavegadorDaCaixa.puxar]), que abre o Painel de Jogos como nas outras abas; no meio da
 * página, puxar para baixo continua rolando o site. O toque e os dois dedos continuam do site.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
private class PaginaDaCaixa(contexto: Context, private val navegador: NavegadorDaCaixa) : WebView(contexto) {
    private val folga = ViewConfiguration.get(contexto).scaledTouchSlop
    private var x0 = 0f
    private var y0 = 0f
    private var ultimoX = 0f
    private var ultimoY = 0f
    private var decidido = false
    private var trocando = false
    private var puxando = false
    private var noTopoAoTocar = false
    private var tocouEm = 0L
    private var rastreador: VelocityTracker? = null

    /** O site para de acompanhar este dedo (o arrasto passou para o app). */
    private fun soltarDoSite(e: MotionEvent) {
        val cancelar = MotionEvent.obtain(e)
        cancelar.action = MotionEvent.ACTION_CANCEL
        super.onTouchEvent(cancelar)
        cancelar.recycle()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                x0 = e.rawX
                y0 = e.rawY
                ultimoX = e.rawX
                ultimoY = e.rawY
                decidido = false
                trocando = false
                puxando = false
                noTopoAoTocar = !runCatching { canScrollVertically(-1) }.getOrDefault(true)
                tocouEm = e.eventTime
                rastreador?.recycle()
                rastreador = VelocityTracker.obtain()
            }
            // Dois dedos (ampliar): do site.
            MotionEvent.ACTION_POINTER_DOWN -> if (!trocando && !puxando) decidido = true
        }
        rastreador?.let { r ->
            val bruto = MotionEvent.obtain(e)
            bruto.setLocation(e.rawX, e.rawY)
            r.addMovement(bruto)
            bruto.recycle()
        }
        if (trocando) {
            when (e.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    navegador.deslizar(FaseDoDeslize.ARRASTO, e.rawX - ultimoX)
                    ultimoX = e.rawX
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val v = if (e.actionMasked == MotionEvent.ACTION_UP) rastreador?.let { it.computeCurrentVelocity(1000); it.xVelocity } ?: 0f else 0f
                    trocando = false
                    navegador.deslizar(FaseDoDeslize.FIM, v)
                }
            }
            return true
        }
        if (puxando) {
            when (e.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    navegador.puxar(FaseDoDeslize.ARRASTO, e.rawY - ultimoY)
                    ultimoY = e.rawY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val v = if (e.actionMasked == MotionEvent.ACTION_UP) rastreador?.let { it.computeCurrentVelocity(1000); it.yVelocity } ?: 0f else 0f
                    puxando = false
                    navegador.puxar(FaseDoDeslize.FIM, v)
                }
            }
            return true
        }
        if (!decidido && e.actionMasked == MotionEvent.ACTION_MOVE) {
            val dx = e.rawX - x0
            val dy = e.rawY - y0
            if (hypot(dx, dy) > folga) {
                decidido = true
                if (navegador.destravada && ehParaOsLados(dx, dy)) {
                    trocando = true
                    soltarDoSite(e)
                    ultimoX = e.rawX
                    navegador.deslizar(FaseDoDeslize.COMECO, 0f)
                    navegador.deslizar(FaseDoDeslize.ARRASTO, dx)
                    return true
                }
                val noTopo = noTopoAoTocar && !runCatching { canScrollVertically(-1) }.getOrDefault(true)
                if (ehParaBaixo(dx, dy) && noTopo && navegador.podePuxar(tocouEm)) {
                    puxando = true
                    soltarDoSite(e)
                    ultimoY = e.rawY
                    navegador.puxar(FaseDoDeslize.COMECO, 0f)
                    navegador.puxar(FaseDoDeslize.ARRASTO, dy)
                    return true
                }
            }
        }
        val usado = super.onTouchEvent(e)
        return usado || e.actionMasked == MotionEvent.ACTION_DOWN
    }
}

/** O arrasto ([dx], [dy]) é para baixo: bem mais para baixo que para os lados (desde a 0.44, o puxão do Painel de Jogos). */
fun ehParaBaixo(dx: Float, dy: Float): Boolean = dy > 0f && dy > 1.5f * abs(dx)

/**
 * A mensagem da aposta cuja página não abriu no site (desde a 0.44 diz onde o site ficou, para dar para saber o porquê):
 * "Não consegui abrir a página da Quina no site (ele ficou em #/home). Tente de novo."
 */
fun mensagemDaPaginaQueNaoAbriu(nome: String, ondeFicou: String?): String =
    "Não consegui abrir a página da $nome no site" + (ondeFicou?.let { " (ele ficou em $it)" } ?: "") + ". Tente de novo."

/** O arrasto ([dx], [dy]) é para os lados: bem mais para os lados que para cima ou para baixo (desde a 0.38). */
fun ehParaOsLados(dx: Float, dy: Float): Boolean = abs(dx) > 1.5f * abs(dy)

/**
 * O JSON que o script dos cookies deixa na página ({x, topo, largura} e, desde a 0.39, altura, direita, cor e opacidade);
 * null se não der.
 */
fun lerCookie(json: String): PosicaoDoCookie? = runCatching {
    val o = kotlinx.serialization.json.Json.parseToJsonElement(json) as kotlinx.serialization.json.JsonObject
    fun texto(k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
    fun f(k: String) = texto(k)!!.toFloat()
    val x = f("x")
    PosicaoDoCookie(
        x = x,
        topo = f("topo"),
        largura = f("largura"),
        altura = texto("altura")?.toFloatOrNull() ?: 0f,
        direita = texto("direita")?.toBooleanStrictOrNull() ?: (x >= 0.5f),
        cor = texto("cor")?.let(::corDoCss),
        opacidade = texto("opacidade")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f,
        naPagina = texto("cadeado")?.toBooleanStrictOrNull() ?: false,
    ).takeIf { it.x in 0f..1f && it.topo in 0f..1f }
}.getOrNull()

/** "rgb(0, 86, 167)" ou "rgba(21, 89, 163, 0.5)" (a cor do CSS) em ARGB; null se não for uma dessas. */
fun corDoCss(texto: String): Int? {
    val m = Regex("""rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)""").matchEntire(texto.trim()) ?: return null
    fun c(i: Int) = m.groupValues[i].toFloat().roundToInt().coerceIn(0, 255)
    val a = m.groupValues[4].takeIf { it.isNotEmpty() }?.toFloat()?.let { (it * 255).roundToInt().coerceIn(0, 255) } ?: 255
    return (a shl 24) or (c(1) shl 16) or (c(2) shl 8) or c(3)
}

/** O lugar do cadeado na página (px): o [tamanho] (o diâmetro), a distância da borda [direita] e a de [baixo]. */
data class LugarDoCadeado(val tamanho: Float, val direita: Float, val baixo: Float)

/**
 * Onde fica o cadeado (desde a 0.39, pedido do usuário: "se não der para os dois ficarem do lado direito, espelha eles,
 * com o mesmo alinhamento, tamanho e transparência"). Desde a 0.44 (pedido do usuário: "jogue o cookie pra esquerda... e
 * alinhe ele na mesma altura") o ícone dos cookies fica onde o site põe (à esquerda) e o cadeado fica sempre na mesma
 * altura dele, com o mesmo tamanho: com o ícone à esquerda, espelhado (a mesma distância da borda, do lado direito); com
 * o ícone à direita (outro site), logo à esquerda dele. Sem achar o ícone, o espelho do que se viu no site da Caixa (47dp,
 * a 27dp da borda e de baixo). [largura] e [altura]: o tamanho da página (px); [dp]: px por dp.
 */
fun lugarDoCadeado(c: PosicaoDoCookie?, largura: Float, altura: Float, dp: Float): LugarDoCadeado {
    if (c == null || c.largura <= 0f) return LugarDoCadeado(TAMANHO_DO_COOKIE * dp, MARGEM_DO_COOKIE * dp, MARGEM_DO_COOKIE * dp)
    val larguraDoCookie = c.largura * largura
    val alturaDoCookie = if (c.altura > 0f) c.altura * altura else larguraDoCookie
    val tamanho = larguraDoCookie.coerceIn(32 * dp, 64 * dp)
    val baixoDoCookie = altura - (c.topo * altura + alturaDoCookie)
    val baixo = (baixoDoCookie + (alturaDoCookie - tamanho) / 2).coerceAtLeast(4 * dp)
    return if (c.direita) {
        val esquerdaDoCookie = c.x * largura - larguraDoCookie / 2
        LugarDoCadeado(tamanho, (largura - esquerdaDoCookie + 8 * dp).coerceAtLeast(4 * dp), baixo)
    } else {
        val esquerdaDoCookie = c.x * largura - larguraDoCookie / 2
        LugarDoCadeado(tamanho, (esquerdaDoCookie + (larguraDoCookie - tamanho) / 2).coerceAtLeast(4 * dp), baixo)
    }
}

/** O ícone dos cookies como apareceu no site da Caixa (foto do usuário, 0.38): 47dp, a 27dp da borda e de baixo. */
const val TAMANHO_DO_COOKIE = 47f
const val MARGEM_DO_COOKIE = 27f

/** A cor do ícone dos cookies como apareceu (azul a 50%: sobre o branco dá o azul-claro da foto, #8AACD1). */
const val COR_DO_COOKIE = 0x801559A3.toInt()

/** O nome da ponte do cadeado da página no JavaScript (desde a 0.41): window.BolaoCadeado. */
const val PONTE_DO_CADEADO = "BolaoCadeado"

/** A chave do cadeado nas preferências (desde a 0.38; true = fechado). */
private const val CADEADO_FECHADO = "caixa_cadeado_fechado"

/** O cadeado para quem não vê a tela (e os testes) e o aviso ao tocar nele (desde a 0.38). */
const val CADEADO_ABERTO = "Cadeado aberto: deslizar para os lados passa para a próxima tela"
const val CADEADO_FECHADO_DESCRICAO = "Cadeado fechado: deslizar para os lados rola a página"
const val AVISO_CADEADO_ABERTO = "Cadeado aberto: deslize para os lados para passar de tela."
const val AVISO_CADEADO_FECHADO = "Cadeado fechado: deslizar para os lados rola a página do site."


/** O endereço http:// com https:// (o mesmo, com cadeado; desde a 0.36); outro endereço, null. */
fun enderecoSeguro(url: String?): String? =
    url?.takeIf { it.startsWith("http://", ignoreCase = true) }?.let { "https://" + it.substring("http://".length) }

/** Quantas vezes seguidas o app troca http:// por https:// antes de mostrar o erro (um site que volta sempre ao http). */
private const val LIMITE_DE_TROCAS = 5

/**
 * A tela da Caixa: a página do site e a barra de carregamento em cima. Desde a 0.39 as apostas vão para o carrinho só em
 * lote (desde a 0.42, o "Efetivar apostas"). Desde a 0.44 a faixa do carrinho, com o "Confirmar", saiu (pedido do usuário:
 * "extinga o botão Confirmar aposta, ele só passa a aparecer se a efetivação falhar"): a aposta que entra no carrinho do
 * site já é confirmada pela área de efetivar. Com o teclado aberto (a conta, a busca do time), a página sobe junto.
 */
@Composable
fun CaixaTela(estado: Estado, navegador: NavegadorDaCaixa) {
    val avisos = LocalAvisos.current

    LaunchedEffect(navegador.aviso) {
        navegador.aviso?.let {
            navegador.aviso = null
            avisos.showSnackbar(it)
        }
    }

    // As apostas que esperam o site (de todas as modalidades) e, delas, as que já foram para o carrinho.
    val pendentes = remember(estado) { Transcricao.emLote(estado, LocalDate.now(), emptyList()).flatMap { it.second } }
    val transcritas: List<String> = navegador.transcritas
    val noCarrinho = pendentes.filter { i -> i.ids.isNotEmpty() && i.ids.all { it in transcritas } }
    // O botão "Efetivar apostas" (desde a 0.42) chama atenção com uma aposta ou mais por efetivar: a página fica sabendo.
    val porEfetivar = pendentes.size - noCarrinho.size
    LaunchedEffect(porEfetivar) { navegador.mostrarPendentes(porEfetivar) }
    if (navegador.pedirEfetivar) {
        AreaDeEfetivar(estado, navegador, aoFechar = { navegador.pedirEfetivar = false })
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        // O lugar da página (desde a 0.33 a página mesmo fica na camada fixa por cima das abas, PaginaFixaDaCaixa).
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { c ->
                    val nova = MedidaDaPagina(c.positionInRoot().y, c.size.width, c.size.height)
                    if (navegador.medida != nova) navegador.medida = nova
                },
        )
    }
}

/** O alto (px, na raiz) e o tamanho do lugar da página na aba da Caixa (desde a 0.33). */
data class MedidaDaPagina(val topo: Float, val largura: Int, val altura: Int)

/** O nome da página do site para a acessibilidade (e os testes). */
const val DESCRICAO_DA_PAGINA = "Página das Loterias Caixa"

/**
 * A página da Caixa congelada (desde a 0.33): o WebView numa camada por cima das abas, preso à janela desde a primeira
 * vez que a tela da Caixa abre até a sessão fechar, com a barra de carregamento e o aviso de erro. [posicao] diz onde
 * a tela da Caixa está agora (0 = na tela; null = fora dela ou com outra tela por cima), lido na hora de posicionar.
 * Fora da tela a página continua presa e do mesmo tamanho, só que de lado: não se redesenha nem recarrega ao voltar.
 */
@Composable
fun PaginaFixaDaCaixa(navegador: NavegadorDaCaixa, puxao: PuxaoAlemDaBorda? = null, posicao: () -> Float?) {
    val medida = navegador.medida ?: return
    val densidade = LocalDensity.current
    var origem by remember { mutableStateOf(Offset.Zero) }
    var largura by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origem = it.positionInRoot(); largura = it.size.width }) {
        Box(
            Modifier
                .offset {
                    val d = posicao()
                    val x = if (d == null) 2 * maxOf(largura, medida.largura) + 100 else (d * medida.largura).roundToInt()
                    // Puxando para baixo no alto da página (desde a 0.44), a página desce junto com o dedo.
                    val desce = if (d == null) 0f else puxao?.deslocamento?.coerceAtLeast(0f) ?: 0f
                    IntOffset(x, (medida.topo - origem.y + desce).roundToInt())
                }
                .size(with(densidade) { medida.largura.toDp() }, with(densidade) { medida.altura.toDp() }),
        ) {
            AndroidView(
                factory = { navegador.webView(it) },
                modifier = Modifier.fillMaxSize().semantics { contentDescription = DESCRICAO_DA_PAGINA },
                onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
            )
            if (navegador.progresso in 1..99) {
                LinearProgressIndicator(
                    progress = { navegador.progresso / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                )
            }
            navegador.erro?.let { ErroDaPagina(it, aoTentar = { navegador.recarregar() }, aoIrAoInicio = { navegador.paginaInicial() }) }
            if (navegador.erro == null) {
                BotaoDoCadeado(navegador, medida)
                BotaoDeEfetivar(navegador, medida, navegador.pendentes)
            }
        }
        // O aviso do puxão (desde a 0.44): por cima da página, que fica numa camada acima das abas.
        if (puxao != null) {
            IndicadorDoPuxao(
                puxao, puxando = "Painel de Jogos", pronto = "Painel de Jogos", icone = Icones.Painel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (medida.topo - origem.y).roundToInt()) }
                    .padding(top = 14.dp),
            )
        }
    }
}

/**
 * O cadeado da página da Caixa (desde a 0.38, pedido do usuário). Desde a 0.39 ele imita o ícone dos cookies do site: o
 * mesmo tamanho, a mesma cor e a mesma transparência; fica logo acima do ícone, quando o script conseguiu passá-lo para
 * a direita, ou espelhado do outro lado, na mesma altura ([lugarDoCadeado]). Tocar abre e fecha.
 *
 * Desde a 0.41 (pedido do usuário: "equivalentes em tamanho e transparência"; na foto o cadeado saía 1,3 vez maior e com
 * a metade da cor) o cadeado vai para dentro da própria página, desenhado pelo script com o tamanho, a cor, a sombra e a
 * transparência que o navegador dá ao ícone dos cookies ([PosicaoDoCookie.naPagina]). Este aqui fica só para quando o
 * ícone dos cookies não aparece.
 */
@Composable
private fun BoxScope.BotaoDoCadeado(navegador: NavegadorDaCaixa, medida: MedidaDaPagina) {
    val densidade = LocalDensity.current
    val c = navegador.cookie
    if (c?.naPagina == true) return
    val lugar = lugarDoCadeado(c, medida.largura.toFloat(), medida.altura.toFloat(), densidade.density)
    val fundo = corDoCadeado(c)
    val aberto = navegador.destravada
    with(densidade) {
        Surface(
            onClick = { navegador.alternarCadeado() },
            shape = CircleShape,
            color = fundo,
            contentColor = corDoIconeDoCadeado(fundo),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = lugar.direita.toDp(), bottom = lugar.baixo.toDp())
                .size(lugar.tamanho.toDp())
                .semantics { contentDescription = if (aberto) CADEADO_ABERTO else CADEADO_FECHADO_DESCRICAO },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (aberto) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size((lugar.tamanho * 0.5f).toDp()),
                )
            }
        }
    }
}

/** A cor do cadeado: a do ícone dos cookies (com a opacidade dele); sem a cor lida, a da foto ([COR_DO_COOKIE]). */
fun corDoCadeado(c: PosicaoDoCookie?): Color {
    val base = Color(c?.cor ?: COR_DO_COOKIE)
    return base.copy(alpha = (base.alpha * (c?.opacidade ?: 1f)).coerceIn(0.15f, 1f))
}

/** O ícone branco, como o dos cookies; num fundo claro (o que aparece sobre o branco da página), azul-noite. */
fun corDoIconeDoCadeado(fundo: Color): Color =
    if (fundo.compositeOver(Color.White).luminance() > 0.75f) Color(0xFF13294B) else Color.White

/**
 * O teclado está aberto (pela altura dele: zero com ele fechado ou enquanto o celular não informou; o "isImeVisible"
 * do Compose começa verdadeiro até o celular informar, e esconderia a barra de baixo e a faixa).
 */
@Composable
fun tecladoAberto(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

/** A página não abriu: o aviso, o "Tentar de novo" e (desde a 0.36) a "Página inicial" do site. */
@Composable
private fun ErroDaPagina(mensagem: String, aoTentar: () -> Unit, aoIrAoInicio: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(mensagem, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = aoTentar) { Text(TENTAR_DE_NOVO) }
        TextButton(onClick = aoIrAoInicio) { Text(PAGINA_INICIAL) }
    }
}

/** Os botões do aviso de erro da página (desde a 0.36, a página inicial também). */
const val TENTAR_DE_NOVO = "Tentar de novo"
const val PAGINA_INICIAL = "Página inicial"

/** O nome do concurso da página: o especial ("Mega da Virada") ou a modalidade. */
fun nomeDaPagina(p: Transcricao.Pagina): String =
    if (p.especial) Especiais.CONHECIDOS.firstOrNull { it.modalidade == p.modalidade }?.nome ?: p.modalidade.nome else p.modalidade.nome

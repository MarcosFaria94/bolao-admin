package io.github.marcosfaria94.bolao.ui.caixa

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AlertDialog
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
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Transcricao
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.platform.LocalContext
import io.github.marcosfaria94.bolao.ui.comum.CONFIRMAR
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.confirmarApostas
import io.github.marcosfaria94.bolao.ui.comum.mensagemDaConfirmacao
import io.github.marcosfaria94.bolao.ui.comum.textoDaConfirmacao
import io.github.marcosfaria94.bolao.ui.comum.tituloDaConfirmacao
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
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
 * e tem o cadeado ([BotaoDoCadeado], logo acima do ícone dos cookies do site, que passou para a direita): aberto,
 * deslizar o dedo para os lados na página passa para a próxima tela; fechado, o dedo fica todo com o site.
 */
const val ROTA_CAIXA = "caixa"

/** O título da tela da Caixa. */
const val TITULO_CAIXA = "Loterias Caixa"

/** O nome do botão da barra de baixo. */
const val ROTULO_CAIXA = "Caixa"

/** O botão que transcreve a aposta no volante do site. */
const val TRANSCREVER = "Transcrever"

/** Quantas vezes o app pergunta ao script se ele terminou (de 300 em 300 ms: 45 segundos). */
private const val PERGUNTAS = 150

/** As fases do deslize para os lados na página, com o cadeado aberto (desde a 0.38): o valor é o arrasto (px) ou, no fim, a velocidade (px/s). */
enum class FaseDoDeslize { COMECO, ARRASTO, FIM }

/** Onde ficou o ícone dos cookies do site (desde a 0.38), em frações da página: o meio dele, o alto e a largura. */
data class PosicaoDoCookie(val x: Float, val topo: Float, val largura: Float)

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
    }

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
                cookie = Transcricao.textoDoJavascript(lido)?.let(::lerCookie) ?: cookie
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

    /** O "Transcrever em lote" da barra de cima foi tocado (desde a 0.38): a tela da Caixa abre a janela. */
    var pedirLote by mutableStateOf(false)

    /** O lote em andamento: o que está sendo transcrito agora ("2 de 5 · Quina Nº4"); null = nenhum. */
    var andamentoDoLote by mutableStateOf<String?>(null)
        private set

    /** Uma aposta do lote e como foi: no carrinho ou o porquê de não ter ido. */
    data class FeitoNoLote(val pagina: Transcricao.Pagina, val item: Transcricao.Item, val resposta: Transcricao.Resposta)

    /**
     * O "Transcrever em lote" (desde a 0.38, pedido do usuário): para cada página com apostas ([grupos]), o app muda a
     * página do site (só a parte depois do "#": a conta continua aberta), espera o volante e transcreve uma aposta depois
     * da outra, como o "Transcrever" da faixa. O login e o pagamento continuam no site. Devolve como foi cada aposta.
     */
    suspend fun transcreverEmLote(grupos: List<Pair<Transcricao.Pagina, List<Transcricao.Item>>>): List<FeitoNoLote> {
        val w = web ?: return emptyList()
        val feitos = mutableListOf<FeitoNoLote>()
        val total = grupos.sumOf { it.second.size }
        var n = 0
        try {
            for ((pagina, itens) in grupos) {
                val chegou = irParaAPagina(w, pagina)
                for (item in itens) {
                    n++
                    andamentoDoLote = "$n de $total · ${nomeDaPagina(pagina)} · ${item.titulo}"
                    val r = if (!chegou) {
                        Transcricao.Resposta(mensagem = "Não consegui abrir a página da ${nomeDaPagina(pagina)} no site: abra a página dela e use o $TRANSCREVER.")
                    } else {
                        transcrever(item)
                    }
                    transcritas.addAll(Transcricao.transcritas(item, r).filter { it !in transcritas })
                    feitos += FeitoNoLote(pagina, item, r)
                }
            }
        } finally {
            andamentoDoLote = null
        }
        return feitos
    }

    /** Muda a página do site para a [pagina] e espera o endereço chegar (até 10 s) e o volante aparecer. */
    private suspend fun irParaAPagina(w: WebView, pagina: Transcricao.Pagina): Boolean {
        if (Transcricao.pagina(endereco) == pagina) return true
        val fragmento = Transcricao.fragmento(pagina) ?: return false
        avaliar(w, "(function(){location.hash=" + Transcricao.textoParaJavascript(fragmento) + ";return 'ok'})()")
        repeat(40) {
            delay(250)
            if (Transcricao.pagina(endereco) == pagina) {
                // O volante da página nova (o script ainda espera por ele, se demorar).
                delay(1_200)
                return true
            }
        }
        return false
    }

    /**
     * Transcreve o [item] na página de agora: o script marca o volante do site, confere e põe no carrinho. O app
     * pergunta de tempos em tempos se terminou.
     */
    suspend fun transcrever(item: Transcricao.Item): Transcricao.Resposta {
        val w = web ?: return Transcricao.Resposta(mensagem = "A página da Caixa não está aberta.")
        transcrevendo = true
        try {
            val inicio = Transcricao.textoDoJavascript(avaliar(w, "(" + script + ")(" + Transcricao.json(item.plano) + ")"))
            if (inicio == "ocupado") return Transcricao.Resposta(mensagem = "O $TRANSCREVER ainda está trabalhando na página. Espere um pouco.")
            if (inicio != "iniciado") return Transcricao.Resposta(mensagem = "A página da Caixa não deixou o $TRANSCREVER rodar. Recarregue e tente de novo.")
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
 * A página do site. Com o cadeado fechado, ela fica com o dedo todo (como na 0.36 e na 0.37): rolar, tocar e ampliar são
 * do site. Com o cadeado aberto (desde a 0.38, o padrão), o arrasto para os lados ([ehParaOsLados]) deixa de ser do site
 * e vai para as abas ([NavegadorDaCaixa.deslizar]): a tela passa para a vizinha; o arrasto para cima e para baixo, o
 * toque e os dois dedos continuam do site.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
private class PaginaDaCaixa(contexto: Context, private val navegador: NavegadorDaCaixa) : WebView(contexto) {
    private val folga = ViewConfiguration.get(contexto).scaledTouchSlop
    private var x0 = 0f
    private var y0 = 0f
    private var ultimoX = 0f
    private var decidido = false
    private var trocando = false
    private var rastreador: VelocityTracker? = null

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                x0 = e.rawX
                y0 = e.rawY
                ultimoX = e.rawX
                decidido = false
                trocando = false
                rastreador?.recycle()
                rastreador = VelocityTracker.obtain()
            }
            // Dois dedos (ampliar): do site.
            MotionEvent.ACTION_POINTER_DOWN -> if (!trocando) decidido = true
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
        if (!decidido && e.actionMasked == MotionEvent.ACTION_MOVE && navegador.destravada) {
            val dx = e.rawX - x0
            val dy = e.rawY - y0
            if (hypot(dx, dy) > folga) {
                decidido = true
                if (ehParaOsLados(dx, dy)) {
                    trocando = true
                    // O site para de acompanhar este dedo.
                    val cancelar = MotionEvent.obtain(e)
                    cancelar.action = MotionEvent.ACTION_CANCEL
                    super.onTouchEvent(cancelar)
                    cancelar.recycle()
                    ultimoX = e.rawX
                    navegador.deslizar(FaseDoDeslize.COMECO, 0f)
                    navegador.deslizar(FaseDoDeslize.ARRASTO, dx)
                    return true
                }
            }
        }
        val usado = super.onTouchEvent(e)
        return usado || e.actionMasked == MotionEvent.ACTION_DOWN
    }
}

/** O arrasto ([dx], [dy]) é para os lados: bem mais para os lados que para cima ou para baixo (desde a 0.38). */
fun ehParaOsLados(dx: Float, dy: Float): Boolean = abs(dx) > 1.5f * abs(dy)

/** O JSON que o script dos cookies deixa na página ({x, topo, largura}); null se não der. */
fun lerCookie(json: String): PosicaoDoCookie? = runCatching {
    val o = kotlinx.serialization.json.Json.parseToJsonElement(json) as kotlinx.serialization.json.JsonObject
    fun f(k: String) = (o[k] as kotlinx.serialization.json.JsonPrimitive).content.toFloat()
    PosicaoDoCookie(f("x"), f("topo"), f("largura")).takeIf { it.x in 0f..1f && it.topo in 0f..1f }
}.getOrNull()

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
 * A tela da Caixa: a página do site, a barra de carregamento em cima e, numa página de modalidade, a faixa do
 * "Transcrever" embaixo (a aposta que vai para o carrinho e o botão). Com o teclado aberto (a conta, a busca do
 * time), a faixa some e a página sobe junto.
 */
@Composable
fun CaixaTela(estado: Estado, navegador: NavegadorDaCaixa) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var resposta by remember { mutableStateOf<Transcricao.Resposta?>(null) }
    // As apostas do carrinho esperando a confirmação na janela (desde a 0.36); null = fechada.
    var aConfirmar by remember { mutableStateOf<List<Transcricao.Item>?>(null) }
    var confirmando by remember { mutableStateOf(false) }
    val teclado = tecladoAberto()

    LaunchedEffect(navegador.aviso) {
        navegador.aviso?.let {
            navegador.aviso = null
            avisos.showSnackbar(it)
        }
    }

    val pagina = Transcricao.pagina(navegador.endereco)
    val itens = remember(estado, pagina) { pagina?.let { Transcricao.itens(estado, it, LocalDate.now()) }.orEmpty() }
    // O "Transcrever em lote" (desde a 0.38): a janela com o que vai, o andamento e, no fim, como foi cada aposta.
    var resultadoDoLote by remember { mutableStateOf<List<NavegadorDaCaixa.FeitoNoLote>?>(null) }
    if (navegador.pedirLote) {
        val lote = remember(estado, navegador.transcritas.size) { Transcricao.emLote(estado, LocalDate.now(), navegador.transcritas) }
        JanelaDoLote(
            lote,
            aoTranscrever = {
                navegador.pedirLote = false
                escopo.launch { resultadoDoLote = navegador.transcreverEmLote(lote) }
            },
            aoFechar = { navegador.pedirLote = false },
        )
    }
    navegador.andamentoDoLote?.let { AndamentoDoLote(it) }
    resultadoDoLote?.let { feitos -> ResultadoDoLote(feitos, aoFechar = { resultadoDoLote = null }) }

    fun transcrever(item: Transcricao.Item) {
        escopo.launch {
            val r = navegador.transcrever(item)
            navegador.transcritas.addAll(Transcricao.transcritas(item, r).filter { it !in navegador.transcritas })
            if (r.ok && r.noCarrinho && !r.semEspelho) avisos.showSnackbar(r.mensagem) else resposta = r
        }
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
        if (pagina != null && !teclado) {
            FaixaDoTranscrever(
                pagina = pagina,
                itens = itens,
                transcritas = navegador.transcritas,
                transcrevendo = navegador.transcrevendo,
                aoTranscrever = ::transcrever,
                aoRecomecar = { feitos ->
                    navegador.transcritas.removeAll(feitos.flatMap { it.ids })
                    feitos.firstOrNull()?.let(::transcrever)
                },
                // No bolão (desde a 0.36), as do carrinho se confirmam aqui depois de pagas; no Individual não há o que confirmar.
                aoConfirmar = if (estado.individual) null else { feitos -> aConfirmar = feitos },
                confirmando = confirmando,
            )
        }
    }

    resposta?.let { r ->
        AlertDialog(
            onDismissRequest = { resposta = null },
            title = { Text(tituloDaResposta(r)) },
            text = { Text(r.mensagem) },
            confirmButton = { TextButton(onClick = { resposta = null }) { Text("OK") } },
        )
    }

    aConfirmar?.let { feitos ->
        val ids = feitos.flatMap { it.ids }
        val numeros = estado.apostas.filter { it.id in ids }.map { it.numero }.sorted()
        DialogoConfirmar(
            titulo = tituloDaConfirmacao(numeros),
            texto = textoDaConfirmacao(numeros.size, estado),
            confirmar = CONFIRMAR,
            aoConfirmar = {
                aConfirmar = null
                confirmando = true
                escopo.launch {
                    try {
                        val feitas = confirmarApostas(ctx, ids)
                        navegador.transcritas.removeAll(ids)
                        avisos.showSnackbar(mensagemDaConfirmacao(feitas, estado))
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível confirmar: ${t.message}")
                    } finally {
                        confirmando = false
                    }
                }
            },
            aoFechar = { aConfirmar = null },
        )
    }
}

/** O nome do botão da barra de cima da Caixa (desde a 0.38) e o título da janela. */
const val TRANSCREVER_EM_LOTE = "Transcrever em lote"

/** "5 apostas de 3 modalidades: Mega-Sena (2), Quina (2) e Lotofácil (1)." */
fun textoDoLote(lote: List<Pair<Transcricao.Pagina, List<Transcricao.Item>>>): String {
    val total = lote.sumOf { it.second.size }
    if (total == 0) return "Nenhuma aposta para transcrever agora."
    val partes = lote.map { (p, i) -> "${nomeDaPagina(p)} (${i.size})" }
    val lista = if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " e " + partes.last()
    val apostas = if (total == 1) "1 aposta" else "$total apostas"
    return "$apostas: $lista. O app abre a página de cada modalidade no site e põe as apostas no carrinho, uma depois da outra. " +
        "Entre na sua conta antes; o pagamento continua no site."
}

/** A janela do "Transcrever em lote": o que vai e o botão. */
@Composable
private fun JanelaDoLote(lote: List<Pair<Transcricao.Pagina, List<Transcricao.Item>>>, aoTranscrever: () -> Unit, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(TRANSCREVER_EM_LOTE) },
        text = { Text(textoDoLote(lote)) },
        confirmButton = { TextButton(onClick = aoTranscrever, enabled = lote.isNotEmpty()) { Text(TRANSCREVER) } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

/** O andamento do lote (não fecha: o site está sendo usado). */
@Composable
private fun AndamentoDoLote(texto: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(TRANSCREVER_EM_LOTE) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(texto)
            }
        },
        confirmButton = {},
    )
}

/** Como foi o lote: quantas foram para o carrinho e o porquê das que não foram. */
@Composable
private fun ResultadoDoLote(feitos: List<NavegadorDaCaixa.FeitoNoLote>, aoFechar: () -> Unit) {
    val ok = feitos.count { it.resposta.ok && it.resposta.noCarrinho }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (ok == feitos.size) "No carrinho" else "No carrinho: $ok de ${feitos.size}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ok > 0) "Pague no site e depois confirme as apostas aqui (a faixa de cada modalidade ou os Comprovantes)." else "Nenhuma foi para o carrinho.")
                feitos.filter { !(it.resposta.ok && it.resposta.noCarrinho) }.forEach { f ->
                    Text("${nomeDaPagina(f.pagina)} · ${f.item.titulo}: ${f.resposta.mensagem}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("OK") } },
    )
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
fun PaginaFixaDaCaixa(navegador: NavegadorDaCaixa, posicao: () -> Float?) {
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
                    IntOffset(x, (medida.topo - origem.y).roundToInt())
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
            if (navegador.erro == null) BotaoDoCadeado(navegador, medida)
        }
    }
}

/**
 * O cadeado da página da Caixa (desde a 0.38, pedido do usuário): logo acima do ícone dos cookies do site (que o
 * script levou para a direita); sem achar o ícone, no canto de baixo à direita. Tocar abre e fecha.
 */
@Composable
private fun BoxScope.BotaoDoCadeado(navegador: NavegadorDaCaixa, medida: MedidaDaPagina) {
    val densidade = LocalDensity.current
    val c = navegador.cookie
    val tamanho = 40.dp
    val (direita, baixo) = with(densidade) {
        if (c == null) {
            12.dp to 84.dp
        } else {
            val meio = (1f - c.x) * medida.largura
            val acima = (1f - c.topo) * medida.altura
            (meio.toDp() - tamanho / 2).coerceAtLeast(4.dp) to (acima.toDp() + 8.dp)
        }
    }
    val aberto = navegador.destravada
    Surface(
        onClick = { navegador.alternarCadeado() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
        contentColor = if (aberto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 4.dp,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = direita, bottom = baixo)
            .size(tamanho)
            .semantics { contentDescription = if (aberto) CADEADO_ABERTO else CADEADO_FECHADO_DESCRICAO },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(if (aberto) Icons.Outlined.LockOpen else Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * O teclado está aberto (pela altura dele: zero com ele fechado ou enquanto o celular não informou; o "isImeVisible"
 * do Compose começa verdadeiro até o celular informar, e esconderia a barra de baixo e a faixa).
 */
@Composable
fun tecladoAberto(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0

/** O título da janela da resposta do "Transcrever". */
fun tituloDaResposta(r: Transcricao.Resposta): String = when {
    r.noCarrinho -> "No carrinho"
    r.parcial -> "Confira o volante"
    else -> "Não deu para transcrever"
}

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

/** O texto da faixa quando não há aposta da modalidade para transcrever (até a 0.35, "esperando comprovante"). */
fun semApostaParaTranscrever(p: Transcricao.Pagina): String = "Nenhuma aposta da ${nomeDaPagina(p)} para transcrever."

/**
 * O texto da faixa com todas as apostas da página no carrinho: no bolão (desde a 0.36), o lembrete de pagar e confirmar.
 */
fun noCarrinho(apostas: Int, confirmar: Boolean): String = when {
    confirmar && apostas == 1 -> "No carrinho: pague no site e confirme."
    confirmar -> "As $apostas no carrinho: pague no site e confirme."
    apostas == 1 -> "A aposta já foi para o carrinho."
    else -> "As $apostas apostas já foram para o carrinho."
}

/** O botão da faixa que confirma as do carrinho quando ainda há outras para transcrever: "Confirmar as do carrinho (2)". */
fun confirmarAsDoCarrinho(apostas: Int): String = "Confirmar as do carrinho ($apostas)"

/** O botão da faixa que transcreve de novo as que já foram (quando uma saiu do carrinho). */
const val TRANSCREVER_DE_NOVO = "Transcrever de novo"

/**
 * A faixa do "Transcrever" (numa página de modalidade do site): o selo da modalidade, a próxima aposta para transcrever
 * (o número dela e o jogo, e "1 de 3" quando há mais de uma) e o botão. Todas já no carrinho: o botão vira "De novo"
 * (para quando uma saiu do carrinho). No bolão (desde a 0.36, com [aoConfirmar]), as do carrinho se confirmam aqui
 * depois de pagas: com todas no carrinho o botão vira "Confirmar" (e "Transcrever de novo" fica embaixo); com outras
 * ainda por transcrever, "Confirmar as do carrinho (N)" fica embaixo.
 */
@Composable
fun FaixaDoTranscrever(
    pagina: Transcricao.Pagina,
    itens: List<Transcricao.Item>,
    transcritas: List<String>,
    transcrevendo: Boolean,
    aoTranscrever: (Transcricao.Item) -> Unit,
    aoRecomecar: (List<Transcricao.Item>) -> Unit,
    aoConfirmar: ((List<Transcricao.Item>) -> Unit)? = null,
    confirmando: Boolean = false,
) {
    val feitos = itens.filter { i -> i.ids.isNotEmpty() && i.ids.all { it in transcritas } }
    val proximo = itens.firstOrNull { it !in feitos }
    val confirmar = aoConfirmar != null
    val ocupado = transcrevendo || confirmando
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        ) {
            SeloModalidade(pagina.modalidade, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val (titulo, detalhe) = when {
                    proximo != null -> {
                        val posicao = if (itens.size > 1) " · ${itens.indexOf(proximo) + 1} de ${itens.size}" else ""
                        proximo.titulo + posicao to proximo.detalhe
                    }
                    itens.isEmpty() -> nomeDaPagina(pagina) to semApostaParaTranscrever(pagina)
                    else -> nomeDaPagina(pagina) to noCarrinho(itens.size, confirmar)
                }
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    detalhe,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // O lembrete do carrinho (pagar e confirmar) inteiro, também com a letra grande.
                    maxLines = if (proximo == null) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            val todosNoCarrinho = proximo == null && feitos.isNotEmpty()
            when {
                // Todas no carrinho, no bolão: pagar no site e confirmar.
                todosNoCarrinho && confirmar -> Button(onClick = { aoConfirmar?.invoke(feitos) }, enabled = !ocupado) {
                    if (confirmando) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(CONFIRMAR, maxLines = 1)
                }
                else -> Button(
                    onClick = { if (todosNoCarrinho) aoRecomecar(feitos) else proximo?.let(aoTranscrever) },
                    enabled = !ocupado && (proximo != null || todosNoCarrinho),
                ) {
                    if (transcrevendo) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (todosNoCarrinho) "De novo" else TRANSCREVER, maxLines = 1)
                }
            }
        }
        // No bolão, a outra ação embaixo: transcrever de novo (todas no carrinho) ou confirmar as que já foram.
        if (confirmar && feitos.isNotEmpty()) {
            TextButton(
                onClick = { if (proximo == null) aoRecomecar(feitos) else aoConfirmar?.invoke(feitos) },
                enabled = !ocupado,
                modifier = Modifier.align(Alignment.End).padding(end = 8.dp, bottom = 4.dp),
            ) {
                Text(if (proximo == null) TRANSCREVER_DE_NOVO else confirmarAsDoCarrinho(feitos.sumOf { it.ids.size }), maxLines = 1)
            }
        }
    }
}

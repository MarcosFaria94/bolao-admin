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
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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

/**
 * Onde ficou o ícone dos cookies do site (desde a 0.38), em frações da página: o meio dele, o alto, a largura e (desde a
 * 0.39) a altura (0 = igual à largura), se ficou à [direita], a [cor] de fundo dele (ARGB; null = não lida) e a
 * [opacidade] dele.
 */
data class PosicaoDoCookie(
    val x: Float,
    val topo: Float,
    val largura: Float,
    val altura: Float = 0f,
    val direita: Boolean = x >= 0.5f,
    val cor: Int? = null,
    val opacidade: Float = 1f,
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
     * da outra (desde a 0.39 é o único jeito de pôr apostas no carrinho). O login e o pagamento continuam no site. Devolve como foi cada aposta.
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
                        Transcricao.Resposta(mensagem = "Não consegui abrir a página da ${nomeDaPagina(pagina)} no site. Tente o $TRANSCREVER_EM_LOTE de novo.")
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
 * com o mesmo alinhamento, tamanho e transparência"). Com o ícone dos cookies [c] à direita (o script conseguiu passá-lo),
 * o cadeado fica logo acima dele, no mesmo eixo; com o ícone à esquerda, espelhado: a mesma distância da borda (a direita
 * no lugar da esquerda), a mesma altura e o mesmo tamanho. Sem achar o ícone, o espelho do que se viu no site da Caixa
 * (47dp, a 27dp da borda e de baixo). [largura] e [altura]: o tamanho da página (px); [dp]: px por dp.
 */
fun lugarDoCadeado(c: PosicaoDoCookie?, largura: Float, altura: Float, dp: Float): LugarDoCadeado {
    if (c == null) return LugarDoCadeado(TAMANHO_DO_COOKIE * dp, MARGEM_DO_COOKIE * dp, MARGEM_DO_COOKIE * dp)
    val larguraDoCookie = c.largura * largura
    val alturaDoCookie = if (c.altura > 0f) c.altura * altura else larguraDoCookie
    val tamanho = larguraDoCookie.coerceIn(32 * dp, 64 * dp)
    val baixoDoCookie = altura - (c.topo * altura + alturaDoCookie)
    return if (c.direita) {
        val meio = (1f - c.x) * largura
        LugarDoCadeado(tamanho, (meio - tamanho / 2).coerceAtLeast(4 * dp), baixoDoCookie + alturaDoCookie + 8 * dp)
    } else {
        val esquerdaDoCookie = c.x * largura - larguraDoCookie / 2
        LugarDoCadeado(
            tamanho,
            (esquerdaDoCookie + (larguraDoCookie - tamanho) / 2).coerceAtLeast(4 * dp),
            (baixoDoCookie + (alturaDoCookie - tamanho) / 2).coerceAtLeast(4 * dp),
        )
    }
}

/** O ícone dos cookies como apareceu no site da Caixa (foto do usuário, 0.38): 47dp, a 27dp da borda e de baixo. */
const val TAMANHO_DO_COOKIE = 47f
const val MARGEM_DO_COOKIE = 27f

/** A cor do ícone dos cookies como apareceu (azul a 50%: sobre o branco dá o azul-claro da foto, #8AACD1). */
const val COR_DO_COOKIE = 0x801559A3.toInt()

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
 * A tela da Caixa: a página do site, a barra de carregamento em cima e, no bolão, com apostas no carrinho do site, a
 * faixa do carrinho embaixo ([FaixaDoCarrinho]: pagar no site e confirmar aqui). Desde a 0.39 (pedido do usuário: "permita
 * apenas transcrever em lote") as apostas vão para o carrinho só pelo "Transcrever em lote" da barra de cima: a faixa
 * de cada página de modalidade, com o "Transcrever" de uma aposta de cada vez (de 0.31 a 0.38), saiu. Com o teclado
 * aberto (a conta, a busca do time), a faixa some e a página sobe junto.
 */
@Composable
fun CaixaTela(estado: Estado, navegador: NavegadorDaCaixa) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
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

    // As apostas que esperam o site (de todas as modalidades) e, delas, as que já foram para o carrinho.
    val pendentes = remember(estado) { Transcricao.emLote(estado, LocalDate.now(), emptyList()).flatMap { it.second } }
    val transcritas: List<String> = navegador.transcritas
    val noCarrinho = pendentes.filter { i -> i.ids.isNotEmpty() && i.ids.all { it in transcritas } }
    // O "Transcrever em lote" (desde a 0.38): a janela com o que vai, o andamento e, no fim, como foi cada aposta.
    var resultadoDoLote by remember { mutableStateOf<List<NavegadorDaCaixa.FeitoNoLote>?>(null) }
    if (navegador.pedirLote) {
        val lote = remember(estado, navegador.transcritas.size) { Transcricao.emLote(estado, LocalDate.now(), navegador.transcritas) }
        JanelaDoLote(
            lote,
            jaNoCarrinho = noCarrinho.sumOf { it.ids.size },
            aoTranscrever = {
                navegador.pedirLote = false
                escopo.launch { resultadoDoLote = navegador.transcreverEmLote(lote) }
            },
            // Desde a 0.39: as que já foram (uma saiu do carrinho do site) vão de novo, todas.
            aoTranscreverDeNovo = {
                navegador.pedirLote = false
                navegador.transcritas.removeAll(noCarrinho.flatMap { it.ids })
                val todas = Transcricao.emLote(estado, LocalDate.now(), emptyList())
                escopo.launch { resultadoDoLote = navegador.transcreverEmLote(todas) }
            },
            aoFechar = { navegador.pedirLote = false },
        )
    }
    navegador.andamentoDoLote?.let { AndamentoDoLote(it) }
    resultadoDoLote?.let { feitos -> ResultadoDoLote(feitos, confirmar = !estado.individual, aoFechar = { resultadoDoLote = null }) }

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
        // No bolão (desde a 0.36), as do carrinho se confirmam aqui depois de pagas; no Individual não há o que confirmar.
        if (!estado.individual && noCarrinho.isNotEmpty() && !teclado) {
            FaixaDoCarrinho(
                apostas = noCarrinho.sumOf { it.ids.size },
                confirmando = confirmando,
                ocupado = navegador.transcrevendo,
                aoConfirmar = { aConfirmar = noCarrinho },
            )
        }
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

/**
 * "5 apostas: Mega-Sena (2), Quina (2) e Lotofácil (1). ..." Sem nenhuma por transcrever: "Nenhuma aposta para transcrever
 * agora." ou, com [jaNoCarrinho] (desde a 0.39), que elas já estão no carrinho do site.
 */
fun textoDoLote(lote: List<Pair<Transcricao.Pagina, List<Transcricao.Item>>>, jaNoCarrinho: Int = 0): String {
    val total = lote.sumOf { it.second.size }
    if (total == 0 && jaNoCarrinho > 0) {
        val quantas = if (jaNoCarrinho == 1) "A aposta já está" else "As $jaNoCarrinho apostas já estão"
        return "$quantas no carrinho do site. Se alguma saiu de lá, transcreva de novo."
    }
    if (total == 0) return "Nenhuma aposta para transcrever agora."
    val partes = lote.map { (p, i) -> "${nomeDaPagina(p)} (${i.size})" }
    val lista = if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " e " + partes.last()
    val apostas = if (total == 1) "1 aposta" else "$total apostas"
    return "$apostas: $lista. O app abre a página de cada modalidade no site e põe as apostas no carrinho, uma depois da outra. " +
        "Entre na sua conta antes; o pagamento continua no site."
}

/**
 * A janela do "Transcrever em lote": o que vai e o botão. Sem nenhuma por transcrever, mas com apostas já no carrinho
 * (desde a 0.39), o botão "Transcrever de novo" manda todas outra vez (para quando uma saiu do carrinho do site).
 */
@Composable
private fun JanelaDoLote(
    lote: List<Pair<Transcricao.Pagina, List<Transcricao.Item>>>,
    jaNoCarrinho: Int,
    aoTranscrever: () -> Unit,
    aoTranscreverDeNovo: () -> Unit,
    aoFechar: () -> Unit,
) {
    val deNovo = lote.isEmpty() && jaNoCarrinho > 0
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(TRANSCREVER_EM_LOTE) },
        text = { Text(textoDoLote(lote, jaNoCarrinho)) },
        confirmButton = {
            if (deNovo) {
                TextButton(onClick = aoTranscreverDeNovo) { Text(TRANSCREVER_DE_NOVO) }
            } else {
                TextButton(onClick = aoTranscrever, enabled = lote.isNotEmpty()) { Text(TRANSCREVER) }
            }
        },
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
private fun ResultadoDoLote(feitos: List<NavegadorDaCaixa.FeitoNoLote>, confirmar: Boolean, aoFechar: () -> Unit) {
    val ok = feitos.count { it.resposta.ok && it.resposta.noCarrinho }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (ok == feitos.size) "No carrinho" else "No carrinho: $ok de ${feitos.size}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        ok == 0 -> "Nenhuma foi para o carrinho."
                        confirmar -> "Pague no site e depois confirme as apostas aqui (a faixa embaixo da página ou os Comprovantes)."
                        else -> "Pague no site."
                    },
                )
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
 * O cadeado da página da Caixa (desde a 0.38, pedido do usuário). Desde a 0.39 ele imita o ícone dos cookies do site: o
 * mesmo tamanho, a mesma cor e a mesma transparência; fica logo acima do ícone, quando o script conseguiu passá-lo para
 * a direita, ou espelhado do outro lado, na mesma altura ([lugarDoCadeado]). Tocar abre e fecha.
 */
@Composable
private fun BoxScope.BotaoDoCadeado(navegador: NavegadorDaCaixa, medida: MedidaDaPagina) {
    val densidade = LocalDensity.current
    val c = navegador.cookie
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

/** O título da faixa do carrinho (desde a 0.39): "1 aposta no carrinho", "3 apostas no carrinho". */
fun tituloDoCarrinho(apostas: Int): String = if (apostas == 1) "1 aposta no carrinho" else "$apostas apostas no carrinho"

/** O lembrete da faixa do carrinho (desde a 0.39). */
const val LEMBRETE_DO_CARRINHO = "Pague no site da Caixa e confirme aqui."

/** O botão da janela do lote que manda de novo as que já foram para o carrinho (quando uma saiu de lá). */
const val TRANSCREVER_DE_NOVO = "Transcrever de novo"

/**
 * A faixa do carrinho (desde a 0.39, no lugar da faixa do "Transcrever" de cada página): no bolão, com apostas que o
 * "Transcrever em lote" já pôs no carrinho do site, o ícone do carrinho, quantas são, o lembrete de pagar e o botão
 * "Confirmar" (todas de uma vez, de todas as modalidades). Fica em qualquer página do site.
 */
@Composable
fun FaixaDoCarrinho(apostas: Int, confirmando: Boolean, ocupado: Boolean, aoConfirmar: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tituloDoCarrinho(apostas), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    LEMBRETE_DO_CARRINHO,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(onClick = aoConfirmar, enabled = !confirmando && !ocupado) {
                if (confirmando) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(CONFIRMAR, maxLines = 1)
            }
        }
    }
}

package io.github.marcosfaria94.bolao.ui.caixa

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
import androidx.compose.ui.platform.LocalDensity
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
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.coroutines.resume

/** A tela da Caixa (desde a 0.31): o Loterias Online da Caixa dentro do app, pelo botão do trevo na barra de baixo. */
const val ROTA_CAIXA = "caixa"

/** O título da tela da Caixa. */
const val TITULO_CAIXA = "Loterias Caixa"

/** O nome do botão da barra de baixo. */
const val ROTULO_CAIXA = "Caixa"

/** O botão que transcreve a aposta no volante do site. */
const val TRANSCREVER = "Transcrever"

/** Quantas vezes o app pergunta ao script se ele terminou (de 300 em 300 ms: 45 segundos). */
private const val PERGUNTAS = 150

/** O que o app lê para saber se o script terminou: o JSON da resposta, ou null. */
private const val LER_RESPOSTA =
    "(function(){var e=window.__bolaoTranscricao;return e&&e.fim?JSON.stringify(e.fim):null})()"

/**
 * O navegador da tela da Caixa: a página (um WebView só, guardado enquanto o app está aberto nesta sessão, para
 * voltar à tela e encontrar a página onde estava, com a conta aberta) e o que a tela mostra dela. As apostas que o
 * "Transcrever" já pôs no carrinho ficam em [transcritas] (nesta abertura do app), para não irem de novo.
 */
class NavegadorDaCaixa(private val contexto: Context) {

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

    private var web: WebView? = null

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

    fun recarregar() {
        erro = null
        val w = web ?: return
        if (w.url.isNullOrBlank()) w.loadUrl(Transcricao.ENDERECO) else w.reload()
    }

    /** Tocar no botão da Caixa já na tela da Caixa: a página volta ao começo (como as listas das abas). */
    fun paraOTopo() {
        web?.pageUp(true)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun criar(tela: Context): WebView = WebView(tela).apply {
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
                "http", "https" -> false
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
        }

        override fun onPageFinished(view: WebView, url: String?) {
            endereco = url
            runCatching { CookieManager.getInstance().flush() }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) erro = "O site da Caixa não abriu (${error.description})."
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
 * A tela da Caixa: a página do site, a barra de carregamento em cima e, numa página de modalidade, a faixa do
 * "Transcrever" embaixo (a aposta que vai para o carrinho e o botão). Com o teclado aberto (a conta, a busca do
 * time), a faixa some e a página sobe junto.
 */
@Composable
fun CaixaTela(estado: Estado, navegador: NavegadorDaCaixa) {
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var resposta by remember { mutableStateOf<Transcricao.Resposta?>(null) }
    val teclado = tecladoAberto()

    LaunchedEffect(navegador.aviso) {
        navegador.aviso?.let {
            navegador.aviso = null
            avisos.showSnackbar(it)
        }
    }

    val pagina = Transcricao.pagina(navegador.endereco)
    val itens = remember(estado, pagina) { pagina?.let { Transcricao.itens(estado, it, LocalDate.now()) }.orEmpty() }

    fun transcrever(item: Transcricao.Item) {
        escopo.launch {
            val r = navegador.transcrever(item)
            navegador.transcritas.addAll(Transcricao.transcritas(item, r).filter { it !in navegador.transcritas })
            if (r.ok && r.noCarrinho && !r.semEspelho) avisos.showSnackbar(r.mensagem) else resposta = r
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { navegador.webView(it) },
                modifier = Modifier.fillMaxSize().semantics { contentDescription = "Página das Loterias Caixa" },
                onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
            )
            if (navegador.progresso in 1..99) {
                LinearProgressIndicator(
                    progress = { navegador.progresso / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                )
            }
            navegador.erro?.let { ErroDaPagina(it) { navegador.recarregar() } }
        }
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

/** A página não abriu: o aviso e o "Tentar de novo". */
@Composable
private fun ErroDaPagina(mensagem: String, aoTentar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(mensagem, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = aoTentar) { Text("Tentar de novo") }
    }
}

/** O nome do concurso da página: o especial ("Mega da Virada") ou a modalidade. */
fun nomeDaPagina(p: Transcricao.Pagina): String =
    if (p.especial) Especiais.CONHECIDOS.firstOrNull { it.modalidade == p.modalidade }?.nome ?: p.modalidade.nome else p.modalidade.nome

/** O texto da faixa quando não há aposta da modalidade esperando o comprovante. */
fun semApostaParaTranscrever(p: Transcricao.Pagina): String = "Nenhuma aposta da ${nomeDaPagina(p)} esperando comprovante."

/**
 * A faixa do "Transcrever" (numa página de modalidade do site): o selo da modalidade, a próxima aposta que espera o
 * comprovante (o número dela e o jogo, e "1 de 3" quando há mais de uma) e o botão. Todas já no carrinho: o botão
 * vira "De novo" (para quando uma saiu do carrinho).
 */
@Composable
fun FaixaDoTranscrever(
    pagina: Transcricao.Pagina,
    itens: List<Transcricao.Item>,
    transcritas: List<String>,
    transcrevendo: Boolean,
    aoTranscrever: (Transcricao.Item) -> Unit,
    aoRecomecar: (List<Transcricao.Item>) -> Unit,
) {
    val feitos = itens.filter { i -> i.ids.isNotEmpty() && i.ids.all { it in transcritas } }
    val proximo = itens.firstOrNull { it !in feitos }
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
                    else -> nomeDaPagina(pagina) to
                        (if (itens.size == 1) "A aposta já foi para o carrinho." else "As ${itens.size} apostas já foram para o carrinho.")
                }
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    detalhe,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            val deNovo = proximo == null && feitos.isNotEmpty()
            Button(
                onClick = { if (deNovo) aoRecomecar(feitos) else proximo?.let(aoTranscrever) },
                enabled = !transcrevendo && (proximo != null || deNovo),
            ) {
                if (transcrevendo) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (deNovo) "De novo" else TRANSCREVER, maxLines = 1)
            }
        }
    }
}

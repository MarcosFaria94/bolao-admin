package io.github.marcosfaria94.bolao.net

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.marcosfaria94.bolao.domain.TIMES
import io.github.marcosfaria94.bolao.domain.TimeDoCoracao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Brasões dos Times do Coração: a imagem principal do artigo de cada time na Wikipédia (ver
 * [TimeDoCoracao.wiki]), buscada na primeira vez que a lista aparece e guardada no celular (pasta
 * "brasoes"). As imagens não vêm no app nem no repositório. Sem internet (ou sem imagem), a tela
 * mostra um escudo genérico no lugar.
 */
object Brasoes {
    private const val PASTA = "brasoes"
    private const val ENDERECOS = "enderecos.txt"

    /** Largura da miniatura pedida (um dos tamanhos padrão da Wikipédia). */
    private const val LARGURA = 120

    /** A Wikipédia pede um User-Agent que diga quem é o programa. */
    private const val AGENTE = "BolaoDosAmigos (https://github.com/MarcosFaria94/bolao-admin)"

    /** Depois de uma falha na consulta dos endereços, espera isto antes de tentar de novo. */
    private const val ESPERA_DEPOIS_DE_FALHAR_MS = 60_000L

    private val memoria = ConcurrentHashMap<String, ImageBitmap>()
    private val travaEnderecos = Mutex()
    private val downloads = Semaphore(4)

    @Volatile
    private var enderecos: Map<String, String>? = null

    @Volatile
    private var falhouEm = 0L

    /** O brasão já carregado (sem esperar). */
    fun naMemoria(t: TimeDoCoracao): ImageBitmap? = memoria[t.wiki]

    /** O brasão do time: da memória, do celular ou da Wikipédia (null = sem internet ou sem imagem). */
    suspend fun carregar(ctx: Context, t: TimeDoCoracao): ImageBitmap? = withContext(Dispatchers.IO) {
        memoria[t.wiki]?.let { return@withContext it }
        val arquivo = File(pasta(ctx), nomeDoArquivo(t.wiki))
        if (arquivo.isFile) {
            decodificar(arquivo.readBytes())?.let {
                memoria[t.wiki] = it
                return@withContext it
            }
        }
        val url = endereco(ctx, t.wiki) ?: return@withContext null
        downloads.withPermit {
            memoria[t.wiki] ?: runCatching {
                val bytes = baixar(url)
                val img = decodificar(bytes) ?: return@runCatching null
                runCatching { arquivo.writeBytes(bytes) }
                memoria[t.wiki] = img
                img
            }.getOrNull()
        }
    }

    private fun pasta(ctx: Context): File = File(ctx.filesDir, PASTA).apply { mkdirs() }

    private fun nomeDoArquivo(wiki: String): String =
        MessageDigest.getInstance("SHA-1").digest(wiki.toByteArray()).joinToString("") { "%02x".format(it) }.take(20) + ".img"

    private fun decodificar(bytes: ByteArray): ImageBitmap? =
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()

    /** O endereço da miniatura do brasão: do celular ou da Wikipédia (uma consulta para todos os times). */
    private suspend fun endereco(ctx: Context, wiki: String): String? = travaEnderecos.withLock {
        val guardados = enderecos ?: lerEnderecos(ctx).also { enderecos = it }
        guardados[wiki]?.let { return@withLock it }
        if (System.currentTimeMillis() - falhouEm < ESPERA_DEPOIS_DE_FALHAR_MS) return@withLock null
        val novos = runCatching { consultar(TIMES.map { it.wiki }.filter { it !in guardados }) }
            .onFailure { falhouEm = System.currentTimeMillis() }
            .getOrNull() ?: return@withLock null
        val todos = guardados + novos
        enderecos = todos
        runCatching { File(pasta(ctx), ENDERECOS).writeText(todos.entries.joinToString("\n") { "${it.key}\t${it.value}" }) }
        // Sem imagem no artigo: não pergunta de novo nesta abertura do app.
        if (wiki !in novos) falhouEm = System.currentTimeMillis()
        todos[wiki]
    }

    private fun lerEnderecos(ctx: Context): Map<String, String> = runCatching {
        File(pasta(ctx), ENDERECOS).readLines().mapNotNull { linha ->
            val i = linha.indexOf('\t')
            if (i <= 0) null else linha.substring(0, i) to linha.substring(i + 1)
        }.toMap()
    }.getOrDefault(emptyMap())

    /** Consulta a Wikipédia ("pt:" ou "en:" + título): devolve as miniaturas encontradas. */
    private fun consultar(artigos: List<String>): Map<String, String> {
        val res = mutableMapOf<String, String>()
        artigos.groupBy { it.substringBefore(':') }.forEach { (lingua, lista) ->
            lista.chunked(50).forEach { lote ->
                val titulos = lote.map { it.substringAfter(':') }
                val url = "https://$lingua.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
                    .addQueryParameter("action", "query")
                    .addQueryParameter("format", "json")
                    .addQueryParameter("formatversion", "2")
                    .addQueryParameter("redirects", "1")
                    .addQueryParameter("prop", "pageimages")
                    .addQueryParameter("piprop", "thumbnail")
                    .addQueryParameter("pithumbsize", LARGURA.toString())
                    .addQueryParameter("pilimit", "50")
                    .addQueryParameter("pilicense", "any")
                    .addQueryParameter("titles", titulos.joinToString("|"))
                    .build()
                val texto = Http.cliente.newCall(Request.Builder().url(url).header("User-Agent", AGENTE).build()).execute().use { resp ->
                    check(resp.isSuccessful) { "Wikipédia: HTTP ${resp.code}" }
                    resp.body?.string().orEmpty()
                }
                miniaturas(texto, titulos).forEach { (titulo, endereco) -> res["$lingua:$titulo"] = endereco }
            }
        }
        return res
    }

    /** Lê a resposta da Wikipédia: título pedido -> endereço da miniatura (seguindo as normalizações e redirecionamentos). */
    fun miniaturas(json: String, titulos: List<String>): Map<String, String> {
        val q = Json.parseToJsonElement(json).jsonObject["query"] as? JsonObject ?: return emptyMap()
        fun trocas(chave: String): Map<String, String> = (q[chave] as? JsonArray).orEmpty().mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            val de = (o["from"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val para = (o["to"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            de to para
        }.toMap()
        val normalizados = trocas("normalized")
        val redirecionados = trocas("redirects")
        val paginas = (q["pages"] as? JsonArray).orEmpty().mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            val titulo = (o["title"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val miniatura = ((o["thumbnail"] as? JsonObject)?.get("source") as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            titulo to miniatura
        }.toMap()
        return titulos.mapNotNull { t ->
            val n = normalizados[t] ?: t
            val final = redirecionados[n] ?: n
            paginas[final]?.let { t to it }
        }.toMap()
    }

    private fun baixar(url: String): ByteArray =
        Http.cliente.newCall(Request.Builder().url(url).header("User-Agent", AGENTE).build()).execute().use { resp ->
            check(resp.isSuccessful) { "Brasão: HTTP ${resp.code}" }
            resp.body?.bytes() ?: ByteArray(0)
        }
}

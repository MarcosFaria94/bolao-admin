package io.github.marcosfaria94.bolao.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.util.AtomicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Guarda todos os dados do bolão num único arquivo JSON dentro do celular,
 * e os anexos (comprovantes, termo) na pasta "arquivos". Nada sai do aparelho
 * a não ser pelo backup.
 */
class Repositorio(private val context: Context) {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val pastaDados = File(context.filesDir, "dados").apply { mkdirs() }
    private val arquivoEstado = AtomicFile(File(pastaDados, "estado.json"))
    val pastaArquivos: File = File(context.filesDir, "arquivos").apply { mkdirs() }

    private val _estado = MutableStateFlow<Estado?>(null)
    /** null enquanto carrega. */
    val estado: StateFlow<Estado?> = _estado.asStateFlow()

    /** Aviso para mostrar na tela se o arquivo de dados estava ilegível. */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    private val mutex = Mutex()
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        escopo.launch { mutex.withLock { _estado.value = lerDoDisco() } }
    }

    private fun lerDoDisco(): Estado {
        val bytes = try {
            arquivoEstado.readFully()
        } catch (e: java.io.FileNotFoundException) {
            return Estado()
        }
        return try {
            json.decodeFromString(Estado.serializer(), bytes.decodeToString())
        } catch (t: Throwable) {
            // Não sobrescreve: guarda uma cópia do arquivo ilegível e começa vazio.
            val copia = File(pastaDados, "estado-ilegivel-${System.currentTimeMillis()}.json")
            runCatching { copia.writeBytes(bytes) }
            _aviso.value = "O arquivo de dados não pôde ser lido. Uma cópia foi guardada e é possível restaurar um backup."
            Estado()
        }
    }

    private fun gravar(e: Estado) {
        val saida = arquivoEstado.startWrite()
        try {
            saida.write(json.encodeToString(Estado.serializer(), e).encodeToByteArray())
            arquivoEstado.finishWrite(saida)
        } catch (t: Throwable) {
            arquivoEstado.failWrite(saida)
            throw t
        }
    }

    /** Espera o carregamento inicial e devolve o estado atual. */
    suspend fun atual(): Estado = mutex.withLock { _estado.value ?: lerDoDisco().also { _estado.value = it } }

    /** Aplica uma mudança e grava no disco antes de devolver. */
    suspend fun atualizar(mudanca: (Estado) -> Estado): Estado = withContext(Dispatchers.IO) {
        mutex.withLock {
            val atual = _estado.value ?: lerDoDisco()
            val novo = mudanca(atual)
            if (novo != atual) gravar(novo)
            _estado.value = novo
            novo
        }
    }

    /** Troca tudo (usado pela restauração do backup). */
    suspend fun substituir(novo: Estado) = withContext(Dispatchers.IO) {
        mutex.withLock {
            gravar(novo)
            _estado.value = novo
            _aviso.value = null
        }
    }

    fun limparAviso() {
        _aviso.value = null
    }

    fun arquivo(a: Arquivo): File = File(pastaArquivos, a.nome)

    /** Copia um arquivo escolhido pelo usuário para a pasta do app. */
    suspend fun guardarArquivo(uri: Uri, prefixo: String): Arquivo = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        var nomeOriginal = "arquivo"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { nomeOriginal = it }
        }
        val extensao = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: nomeOriginal.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 5 }
            ?: "bin"
        val nome = "$prefixo-${UUID.randomUUID().toString().take(8)}.$extensao"
        val destino = File(pastaArquivos, nome)
        resolver.openInputStream(uri).use { entrada ->
            requireNotNull(entrada) { "Não foi possível abrir o arquivo escolhido." }
            destino.outputStream().use { entrada.copyTo(it) }
        }
        Arquivo(nome = nome, nomeOriginal = nomeOriginal, mime = mime, guardadoEm = System.currentTimeMillis())
    }

    /** Guarda bytes gerados pelo app (ex.: imagem da apuração). */
    suspend fun guardarBytes(bytes: ByteArray, prefixo: String, extensao: String, mime: String): Arquivo =
        withContext(Dispatchers.IO) {
            val nome = "$prefixo-${UUID.randomUUID().toString().take(8)}.$extensao"
            File(pastaArquivos, nome).writeBytes(bytes)
            Arquivo(nome = nome, nomeOriginal = nome, mime = mime, guardadoEm = System.currentTimeMillis())
        }

    /** Apaga anexos que nenhum registro usa mais. */
    suspend fun limparArquivosSoltos() = withContext(Dispatchers.IO) {
        val e = atual()
        val usados = buildSet {
            e.termo?.let { add(it.nome) }
            e.apostas.forEach { a -> a.comprovante?.let { add(it.nome) } }
            e.rateios.forEach { r -> r.comprovantes.forEach { add(it.nome) } }
        }
        pastaArquivos.listFiles()?.filter { it.isFile && it.name !in usados }?.forEach { it.delete() }
    }

    companion object {
        fun novoId(): String = UUID.randomUUID().toString()
    }
}

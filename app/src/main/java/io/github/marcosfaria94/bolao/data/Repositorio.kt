package io.github.marcosfaria94.bolao.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.util.AtomicFile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID

/**
 * Guarda todos os dados do bolão num único arquivo JSON dentro do celular,
 * e os anexos (comprovantes, termo) na pasta "arquivos". Nada sai do aparelho
 * a não ser pelo backup.
 *
 * O modo de teste usa um espaço separado (pasta "teste"), com dados de exemplo:
 * enquanto ele está ligado, os dados reais não são lidos nem alterados.
 *
 * Todas as mudanças passam por uma fila única, na ordem em que foram pedidas,
 * e são gravadas no disco antes de aparecerem na tela.
 */
class Repositorio(private val context: Context) {

    enum class Espaco { REAL, TESTE }

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    /** Espaço em uso. Só muda dentro da fila. */
    @Volatile
    private var espaco: Espaco = if (prefs.getBoolean(CHAVE_MODO_TESTE, false)) Espaco.TESTE else Espaco.REAL

    private val _modoTeste = MutableStateFlow(espaco == Espaco.TESTE)
    /** true enquanto o app mostra os dados de exemplo do modo de teste. */
    val modoTeste: StateFlow<Boolean> = _modoTeste.asStateFlow()
    val emTeste: Boolean get() = espaco == Espaco.TESTE

    private fun raiz(e: Espaco): File = if (e == Espaco.TESTE) File(context.filesDir, PASTA_TESTE) else context.filesDir
    private fun pastaDados(e: Espaco): File = File(raiz(e), "dados").apply { mkdirs() }
    private fun arquivoEstado(e: Espaco) = AtomicFile(File(pastaDados(e), "estado.json"))
    private fun pastaDeArquivos(e: Espaco): File = File(raiz(e), "arquivos").apply { mkdirs() }

    /** Pasta dos anexos do espaço em uso. */
    val pastaArquivos: File get() = pastaDeArquivos(espaco)

    private val _estado = MutableStateFlow<Estado?>(null)
    /** null enquanto carrega. */
    val estado: StateFlow<Estado?> = _estado.asStateFlow()

    /** Aviso para mostrar na tela (arquivo de dados ilegível, modo de teste...). */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    private sealed interface Pedido {
        /** Mudança nos dados do espaço que estava em uso quando foi pedida. */
        class Mudanca(val espaco: Espaco, val mudanca: (Estado) -> Estado, val resposta: CompletableDeferred<Estado>?) : Pedido

        /** Tarefa que mexe com os espaços ou com os anexos, na mesma ordem das mudanças. */
        class Tarefa(val acao: suspend () -> Any?, val resposta: CompletableDeferred<Any?>) : Pedido
    }

    private val fila = Channel<Pedido>(Channel.UNLIMITED)
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        escopo.launch {
            _estado.value = lerDoDisco(espaco)
            for (p in fila) {
                when (p) {
                    is Pedido.Mudanca -> try {
                        // Uma mudança pedida antes de trocar entre teste e dados reais não vale para o outro espaço.
                        check(p.espaco == espaco) { "O app trocou entre o modo de teste e os dados reais. Tente de novo." }
                        val atual = _estado.value ?: lerDoDisco(espaco)
                        val novo = p.mudanca(atual)
                        if (novo != atual) gravar(espaco, novo)
                        _estado.value = novo
                        p.resposta?.complete(novo)
                    } catch (t: Throwable) {
                        p.resposta?.completeExceptionally(t)
                    }

                    is Pedido.Tarefa -> try {
                        p.resposta.complete(p.acao())
                    } catch (t: Throwable) {
                        p.resposta.completeExceptionally(t)
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> naFila(acao: suspend () -> T): T {
        val resposta = CompletableDeferred<Any?>()
        fila.send(Pedido.Tarefa(acao, resposta))
        return resposta.await() as T
    }

    private fun lerDoDisco(e: Espaco): Estado {
        val bytes = try {
            arquivoEstado(e).readFully()
        } catch (x: java.io.FileNotFoundException) {
            return Estado()
        }
        return try {
            val lido = json.decodeFromString(Estado.serializer(), bytes.decodeToString())
            if (e == Espaco.TESTE && lido.versao < Migracao.VERSAO) {
                _aviso.value = "Os dados de exemplo são de uma versão antiga do app. Em Ajustes, toque em \"Recomeçar com um exemplo novo\"."
            }
            Migracao.migrar(lido, java.time.LocalDate.now())
        } catch (t: Throwable) {
            // Não sobrescreve: guarda uma cópia do arquivo ilegível e começa vazio.
            val copia = File(pastaDados(e), "estado-ilegivel-${System.currentTimeMillis()}.json")
            runCatching { copia.writeBytes(bytes) }
            _aviso.value = "O arquivo de dados não pôde ser lido. Uma cópia foi guardada e é possível restaurar um backup."
            Estado()
        }
    }

    private fun gravar(e: Espaco, estado: Estado) {
        val arquivo = arquivoEstado(e)
        val saida = arquivo.startWrite()
        try {
            saida.write(json.encodeToString(Estado.serializer(), estado).encodeToByteArray())
            arquivo.finishWrite(saida)
        } catch (t: Throwable) {
            arquivo.failWrite(saida)
            throw t
        }
    }

    /** Aplica uma mudança (na ordem da fila), grava no disco e devolve o novo estado. */
    suspend fun atualizar(mudanca: (Estado) -> Estado): Estado {
        val resposta = CompletableDeferred<Estado>()
        fila.send(Pedido.Mudanca(espaco, mudanca, resposta))
        return resposta.await()
    }

    /** Mesma fila, sem esperar (para gravar o rascunho a cada toque). Erros são ignorados. */
    fun enfileirar(mudanca: (Estado) -> Estado) {
        fila.trySend(Pedido.Mudanca(espaco, mudanca, null))
    }

    /** Estado atual, depois de tudo o que já estava na fila. */
    suspend fun atual(): Estado = naFila { _estado.value ?: lerDoDisco(espaco) }

    fun avisar(texto: String) {
        _aviso.value = texto
    }

    fun limparAviso() {
        _aviso.value = null
    }

    fun arquivo(a: Arquivo): File = File(pastaArquivos, a.nome)

    /**
     * Copia um arquivo escolhido pelo usuário para a pasta do app, sem mudar nada nele,
     * e guarda a impressão digital (SHA-256) do conteúdo.
     */
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
        val digest = MessageDigest.getInstance("SHA-256")
        resolver.openInputStream(uri).use { entrada ->
            requireNotNull(entrada) { "Não foi possível abrir o arquivo escolhido." }
            DigestInputStream(entrada, digest).use { d -> destino.outputStream().use { d.copyTo(it) } }
        }
        Arquivo(
            nome = nome, nomeOriginal = nomeOriginal, mime = mime, guardadoEm = System.currentTimeMillis(),
            sha256 = hex(digest.digest()),
        )
    }

    /** Guarda um anexo criado pelo próprio app (ex.: imagem dos acertos), com a impressão digital. */
    suspend fun guardarGerado(prefixo: String, nomeOriginal: String, mime: String, escrever: (File) -> Unit): Arquivo =
        withContext(Dispatchers.IO) {
            val extensao = nomeOriginal.substringAfterLast('.', "bin")
            val nome = "$prefixo-${UUID.randomUUID().toString().take(8)}.$extensao"
            val destino = File(pastaArquivos, nome)
            escrever(destino)
            Arquivo(
                nome = nome, nomeOriginal = nomeOriginal, mime = mime, guardadoEm = System.currentTimeMillis(),
                sha256 = sha256(destino),
            )
        }

    /** SHA-256 do arquivo guardado (null se ele não existir). */
    fun sha256(a: Arquivo): String? = arquivo(a).takeIf { it.isFile }?.let { sha256(it) }

    private fun sha256(f: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { entrada ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = entrada.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return hex(digest.digest())
    }

    private fun hex(b: ByteArray): String = b.joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    /** Nomes de todos os arquivos que algum registro usa. */
    private fun usados(e: Estado): Set<String> = buildSet {
        e.termo?.let { add(it.nome) }
        e.termoSemAssinatura?.let { add(it.nome) }
        e.apostas.forEach { a -> a.comprovante?.let { add(it.nome) } }
        e.rateios.forEach { r -> r.comprovantes.forEach { add(it.nome) } }
        e.comprovantes.forEach { c ->
            add(c.imagem.nome)
            c.anexos.forEach { add(it.nome) }
        }
        e.extincao?.let { x ->
            x.comprovantes.values.forEach { add(it.nome) }
            x.auditoria?.let { add(it.nome) }
        }
    }

    /** Apaga anexos que nenhum registro usa mais (só no espaço em uso). */
    suspend fun limparArquivosSoltos() {
        naFila {
            val e = _estado.value ?: lerDoDisco(espaco)
            val usados = usados(e)
            // Anexos recentes podem ser de um registro ainda em andamento: só apaga os antigos.
            val limite = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            pastaDeArquivos(espaco).listFiles()
                ?.filter { it.isFile && it.name !in usados && it.lastModified() < limite }
                ?.forEach { it.delete() }
        }
    }

    /**
     * Apaga um anexo que nenhum registro usa: o comprovante da Caixa de uma aposta descartada
     * ou um anexo trocado. Roda na fila, depois das mudanças já pedidas (se a aposta foi
     * gravada ou registrada antes, o arquivo continua).
     */
    fun apagarSeSolto(a: Arquivo) {
        escopo.launch {
            runCatching {
                naFila {
                    val e = _estado.value ?: lerDoDisco(espaco)
                    if (a.nome !in usados(e)) File(pastaArquivos, a.nome).delete()
                }
            }
        }
    }

    /**
     * Fim da extinção: apaga todos os dados e anexos do bolão (o app volta ao cadastro).
     * No modo de teste, apaga só os dados de exemplo e volta aos dados reais.
     */
    suspend fun apagarTudo() {
        naFila {
            if (espaco == Espaco.TESTE) {
                mudarPara(Espaco.REAL, lerDoDisco(Espaco.REAL))
                apagarTeste()
            } else {
                File(context.filesDir, "dados").deleteRecursively()
                File(context.filesDir, "arquivos").deleteRecursively()
                _estado.value = Estado()
                _aviso.value = null
            }
        }
    }

    /** Dados e pasta de anexos do mesmo espaço, lidos juntos (para o backup). */
    class Retrato(val estado: Estado, val pastaArquivos: File, val teste: Boolean)

    suspend fun retrato(): Retrato = naFila { Retrato(_estado.value ?: lerDoDisco(espaco), pastaDeArquivos(espaco), espaco == Espaco.TESTE) }

    /** Troca todos os dados do espaço em uso pelos de um backup; [anexos] é a pasta com os anexos extraídos. */
    suspend fun restaurar(novo: Estado, anexos: File) {
        naFila {
            require(espaco == Espaco.TESTE || !novo.exemplo) {
                "Este backup é do modo de teste (dados de exemplo). Para não misturar com os dados reais, restaure-o com o modo de teste ligado."
            }
            val destino = pastaDeArquivos(espaco)
            anexos.listFiles()?.filter { it.isFile }?.forEach { it.copyTo(File(destino, it.name), overwrite = true) }
            gravar(espaco, novo)
            _estado.value = novo
            _aviso.value = null
        }
    }

    // ---------- modo de teste ----------

    /** Dados reais, mesmo com o modo de teste ligado (só leitura). */
    suspend fun estadoReal(): Estado = naFila {
        if (espaco == Espaco.REAL) _estado.value ?: lerDoDisco(Espaco.REAL) else lerDoDisco(Espaco.REAL)
    }

    /**
     * Liga o modo de teste com o [exemplo] (um teste anterior é apagado antes).
     * [prepararAnexos] grava os anexos fictícios na pasta recebida, antes da troca, e
     * devolve o estado final (com as impressões digitais dos anexos).
     */
    suspend fun entrarNoTeste(exemplo: Estado, prepararAnexos: (File, Estado) -> Estado) {
        require(exemplo.exemplo) { "Só dados de exemplo podem ir para o modo de teste." }
        naFila {
            apagarTeste()
            val final = prepararAnexos(pastaDeArquivos(Espaco.TESTE), exemplo)
            require(final.exemplo) { "Só dados de exemplo podem ir para o modo de teste." }
            gravar(Espaco.TESTE, final)
            mudarPara(Espaco.TESTE, final)
        }
    }

    /** Desliga o modo de teste: volta aos dados reais e apaga os dados de exemplo. */
    suspend fun sairDoTeste() {
        naFila {
            mudarPara(Espaco.REAL, lerDoDisco(Espaco.REAL))
            apagarTeste()
        }
    }

    private fun mudarPara(e: Espaco, estado: Estado) {
        prefs.edit().putBoolean(CHAVE_MODO_TESTE, e == Espaco.TESTE).commit()
        espaco = e
        _estado.value = estado
        _modoTeste.value = e == Espaco.TESTE
    }

    private fun apagarTeste() {
        File(context.filesDir, PASTA_TESTE).deleteRecursively()
    }

    companion object {
        const val PASTA_TESTE = "teste"
        private const val CHAVE_MODO_TESTE = "modo_teste"

        fun novoId(): String = io.github.marcosfaria94.bolao.data.novoId()
    }
}

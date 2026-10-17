package io.github.marcosfaria94.bolao.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup completo num .zip: estado.json + todos os anexos (comprovantes, termo).
 * O arquivo vai para Downloads/Bolao e pode ser enviado para o Drive, e-mail etc.
 */
class Backup(private val context: Context, private val repo: Repositorio) {

    private companion object {
        const val ESTADO = "estado.json"
        const val PASTA = "arquivos/"
    }

    suspend fun gerar(saida: OutputStream) = withContext(Dispatchers.IO) {
        val e = repo.atual()
        ZipOutputStream(BufferedOutputStream(saida)).use { zip ->
            zip.putNextEntry(ZipEntry(ESTADO))
            zip.write(repo.json.encodeToString(Estado.serializer(), e).encodeToByteArray())
            zip.closeEntry()
            repo.pastaArquivos.listFiles()?.filter { it.isFile }?.forEach { f ->
                zip.putNextEntry(ZipEntry(PASTA + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("LEIA-ME.txt"))
            zip.write(
                ("Backup do app Bolão Admin.\n" +
                    "Para restaurar: abra o app > Ajustes > Restaurar backup e escolha este arquivo.\n")
                    .encodeToByteArray(),
            )
            zip.closeEntry()
        }
    }

    /** Grava o backup em Downloads/Bolao e devolve o endereço do arquivo. */
    suspend fun salvarEmDownloads(): Uri = withContext(Dispatchers.IO) {
        val agora = LocalDateTime.now()
        val nome = "bolao-backup-%04d-%02d-%02d-%02d%02d.zip".format(
            agora.year, agora.monthValue, agora.dayOfMonth, agora.hour, agora.minute,
        )
        val valores = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nome)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Bolao")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
            ?: error("Não foi possível criar o arquivo em Downloads.")
        try {
            val saida = resolver.openOutputStream(uri) ?: error("Não foi possível gravar em Downloads.")
            saida.use { gerar(it) }
            valores.clear()
            valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, valores, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        uri
    }

    data class Restaurado(val estado: Estado, val anexos: Int)

    /** Lê um backup e substitui todos os dados do app por ele. */
    suspend fun restaurar(entrada: InputStream): Restaurado = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "restauracao-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var estado: Estado? = null
            var anexos = 0
            ZipInputStream(BufferedInputStream(entrada)).use { zip ->
                while (true) {
                    val item = zip.nextEntry ?: break
                    val nome = item.name
                    if (item.isDirectory) continue
                    if (nome == ESTADO) {
                        estado = repo.json.decodeFromString(Estado.serializer(), zip.readBytes().decodeToString())
                    } else if (nome.startsWith(PASTA)) {
                        val simples = nome.removePrefix(PASTA)
                        if (simples.isBlank() || simples.contains('/') || simples.contains('\\') || simples.contains("..")) continue
                        File(temp, simples).outputStream().use { zip.copyTo(it) }
                        anexos++
                    }
                }
            }
            val e = estado ?: throw IllegalArgumentException("Este arquivo não é um backup do Bolão.")
            temp.listFiles()?.forEach { f -> f.copyTo(File(repo.pastaArquivos, f.name), overwrite = true) }
            repo.substituir(e)
            repo.limparArquivosSoltos()
            Restaurado(e, anexos)
        } finally {
            temp.deleteRecursively()
        }
    }
}

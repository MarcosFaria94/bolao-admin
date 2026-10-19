package io.github.marcosfaria94.bolao.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/** Arquivos que o app entrega ao usuário (backup, termo, auditoria) vão para Downloads/Bolao. */
object Downloads {

    const val PASTA = "Bolao"

    /** Cria o arquivo em Downloads/Bolao, escreve com [escrever] e devolve o endereço. */
    suspend fun salvar(context: Context, nome: String, mime: String, escrever: suspend (OutputStream) -> Unit): Uri =
        withContext(Dispatchers.IO) {
            val valores = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nome)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + PASTA)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                ?: error("Não foi possível criar o arquivo em Downloads.")
            try {
                val saida = resolver.openOutputStream(uri) ?: error("Não foi possível gravar em Downloads.")
                saida.use { escrever(it) }
                valores.clear()
                valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, valores, null, null)
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }
            uri
        }
}

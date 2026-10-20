package io.github.marcosfaria94.bolao.imagem

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Lê o texto do comprovante da Caixa (imagem ou PDF) com o reconhecimento de texto do
 * Google (ML Kit, no próprio celular: nada é enviado para fora). O texto vai para a
 * LeituraComprovante, que acha o concurso e os números.
 */
object LeitorComprovante {
    private const val LARGURA = 1600
    private const val PAGINAS = 2
    private const val TEMPO_MS = 20_000L

    suspend fun texto(arquivo: File, mime: String): String {
        val paginas = withContext(Dispatchers.IO) { Anexos.paginas(arquivo, mime, LARGURA).take(PAGINAS) }
        if (paginas.isEmpty()) throw IOException("formato de arquivo que o app não consegue ler")
        val reconhecedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val partes = mutableListOf<String>()
            for (b in paginas) partes += withTimeout(TEMPO_MS) { reconhecer(reconhecedor, b) }
            return partes.joinToString("\n")
        } finally {
            reconhecedor.close()
            paginas.forEach { it.recycle() }
        }
    }

    private suspend fun reconhecer(r: TextRecognizer, b: Bitmap): String = suspendCancellableCoroutine { cont ->
        r.process(InputImage.fromBitmap(b, 0))
            .addOnSuccessListener { t -> if (cont.isActive) cont.resume(t.text) }
            .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }
}

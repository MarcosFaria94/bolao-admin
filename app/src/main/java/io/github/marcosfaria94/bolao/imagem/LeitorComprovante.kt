package io.github.marcosfaria94.bolao.imagem

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
 *
 * Desde a 0.22: a leitura que falha (o reconhecedor ainda carregando, na primeira vez) é repetida
 * uma vez; e, quando a primeira leitura não bate com nenhuma aposta, há as [outrasLeituras] do mesmo
 * arquivo (o texto que o próprio PDF traz e a leitura com a página maior), para um número mal
 * reconhecido não trocar o jogo de uma aposta.
 */
object LeitorComprovante {
    private const val LARGURA = 1600

    /** A segunda leitura: a página maior (os números pequenos do comprovante saem mais nítidos). */
    private const val LARGURA_MAIOR = 2400
    private const val PAGINAS = 2
    private const val TEMPO_MS = 20_000L

    /** Espera antes de repetir uma leitura que falhou ou veio vazia. */
    private const val ESPERA_DA_REPETICAO_MS = 1_200L

    suspend fun texto(arquivo: File, mime: String): String {
        val primeira = runCatching { reconhecer(arquivo, mime, LARGURA) }
        primeira.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        // Falhou ou não leu nada: uma segunda tentativa (na primeira leitura depois de abrir o app o
        // reconhecedor pode ainda estar carregando).
        delay(ESPERA_DA_REPETICAO_MS)
        return runCatching { reconhecer(arquivo, mime, LARGURA) }.getOrElse { primeira.getOrThrow() }
    }

    /**
     * Outras leituras do mesmo arquivo, da mais confiável para a menos: o texto que o próprio PDF traz
     * (os comprovantes do app da Caixa são PDFs de texto; o Android lê esse texto a partir da versão 15)
     * e o reconhecimento com a página maior. Vazias ou com erro ficam de fora.
     */
    suspend fun outrasLeituras(arquivo: File, mime: String): List<String> = buildList {
        runCatching { textoDoPdf(arquivo, mime) }.getOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }
        runCatching { reconhecer(arquivo, mime, LARGURA_MAIOR) }.getOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }
    }

    /** O texto guardado no PDF (sem reconhecimento): só em PDF e do Android 15 em diante; senão, vazio. */
    suspend fun textoDoPdf(arquivo: File, mime: String): String = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 35 || !Anexos.ehPdf(mime, arquivo) || !arquivo.isFile) return@withContext ""
        val partes = mutableListOf<String>()
        ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                for (i in 0 until minOf(r.pageCount, PAGINAS)) {
                    r.openPage(i).use { pg -> pg.textContents.forEach { partes += it.text } }
                }
            }
        }
        partes.joinToString("\n")
    }

    private suspend fun reconhecer(arquivo: File, mime: String, largura: Int): String {
        val paginas = withContext(Dispatchers.IO) { Anexos.paginas(arquivo, mime, largura).take(PAGINAS) }
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

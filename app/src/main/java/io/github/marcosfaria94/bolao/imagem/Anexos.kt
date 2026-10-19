package io.github.marcosfaria94.bolao.imagem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.ParcelFileDescriptor
import io.github.marcosfaria94.bolao.pdf.Jpeg
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Leitura dos anexos para mostrar na tela e para o PDF da auditoria: fotos (com a rotação
 * da câmera), capturas de tela e PDFs (cada página vira uma imagem). O arquivo guardado
 * nunca é alterado.
 */
object Anexos {

    const val MAX_PAGINAS = 12

    fun ehPdf(mime: String, arquivo: File): Boolean =
        mime == "application/pdf" || arquivo.name.endsWith(".pdf", ignoreCase = true)

    fun ehImagem(mime: String): Boolean = mime.startsWith("image/")

    private fun rotacao(arquivo: File): Int = runCatching {
        when (ExifInterface(arquivo.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    /** Foto ou imagem reduzida para no máximo [larguraMax] pontos de largura, já na posição certa. */
    fun imagem(arquivo: File, larguraMax: Int): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(arquivo.path, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
        val graus = rotacao(arquivo)
        val larguraReal = if (graus == 90 || graus == 270) limites.outHeight else limites.outWidth
        var amostra = 1
        while (larguraReal / (amostra * 2) >= larguraMax) amostra *= 2
        val bmp = BitmapFactory.decodeFile(arquivo.path, BitmapFactory.Options().apply { inSampleSize = amostra }) ?: return null
        if (graus == 0) return bmp
        val girada = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(graus.toFloat()) }, true)
        if (girada !== bmp) bmp.recycle()
        return girada
    }

    /** Páginas de um PDF como imagens (fundo branco), até [maxPaginas]. */
    fun paginasPdf(arquivo: File, larguraMax: Int, maxPaginas: Int = MAX_PAGINAS): List<Bitmap> {
        val lista = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                for (i in 0 until minOf(r.pageCount, maxPaginas)) {
                    r.openPage(i).use { pg ->
                        val escala = larguraMax.toFloat() / pg.width
                        val w = larguraMax
                        val h = (pg.height * escala).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        pg.render(bmp, null, Matrix().apply { setScale(escala, escala) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        lista += bmp
                    }
                }
            }
        }
        return lista
    }

    /** Todas as "páginas" de um anexo como imagens (foto = 1 página). */
    fun paginas(arquivo: File, mime: String, larguraMax: Int): List<Bitmap> = runCatching {
        when {
            !arquivo.isFile -> emptyList()
            ehPdf(mime, arquivo) -> paginasPdf(arquivo, larguraMax)
            else -> listOfNotNull(imagem(arquivo, larguraMax))
        }
    }.getOrDefault(emptyList())

    private fun jpeg(bmp: Bitmap, qualidade: Int = 82): ByteArray {
        // JPEG não tem transparência: fundo branco.
        val base = if (bmp.hasAlpha()) {
            Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888).also {
                val c = Canvas(it)
                c.drawColor(Color.WHITE)
                c.drawBitmap(bmp, 0f, 0f, null)
            }
        } else {
            bmp
        }
        val saida = ByteArrayOutputStream()
        base.compress(Bitmap.CompressFormat.JPEG, qualidade, saida)
        if (base !== bmp) base.recycle()
        return saida.toByteArray()
    }

    /**
     * Páginas de um anexo em JPEG, para o PDF da auditoria. Uma foto JPEG leve e sem rotação
     * entra como está (os mesmos bytes do original); as outras são convertidas, reduzidas a
     * no máximo [larguraMax] pontos.
     */
    fun jpegsParaPdf(arquivo: File, mime: String, larguraMax: Int = 1400): List<ByteArray> = runCatching {
        if (!arquivo.isFile) return@runCatching emptyList()
        if (mime == "image/jpeg" && arquivo.length() <= 1_500_000 && rotacao(arquivo) == 0) {
            val bytes = arquivo.readBytes()
            val d = Jpeg.dimensoes(bytes)
            if (d != null && d.componentes in listOf(1, 3) && d.largura <= 2400 && d.altura <= 3200) return@runCatching listOf(bytes)
        }
        paginas(arquivo, mime, larguraMax).map { bmp ->
            try {
                jpeg(bmp)
            } finally {
                bmp.recycle()
            }
        }
    }.getOrDefault(emptyList())
}

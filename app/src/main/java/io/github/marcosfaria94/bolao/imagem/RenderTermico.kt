package io.github.marcosfaria94.bolao.imagem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import io.github.marcosfaria94.bolao.imagem.LayoutTermico.Estilo
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Desenha o comprovante no estilo do papel térmico dos bancos: amarelo-canário, letra de
 * impressora, bordas serrilhadas. Sem suavização (cada ponto é tinta ou papel), então a
 * imagem sai com só 4 cores e vira um PNG de poucos KB.
 */
object RenderTermico {
    private const val TAMANHO = 60f
    private const val LINHA = 63
    private const val ALTURA_LOGO = 170
    private const val SERRA = 20
    private const val BORDA = 12
    private const val DESLOCA_SOMBRA = 6

    // Cores (0xRRGGBB). A ordem é a da paleta do PNG.
    private const val BRANCO = 0xFFFFFF
    private const val PAPEL = 0xFBF0A6
    private const val TINTA = 0x2C2924
    private const val SOMBRA = 0xE2DCC0
    private val PALETA = intArrayOf(BRANCO, PAPEL, TINTA, SOMBRA)

    private fun argb(rgb: Int) = rgb or (0xFF shl 24)

    private fun pincel(context: Context, tamanho: Float, cor: Int) = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
        isSubpixelText = false
        isLinearText = false
        typeface = Fontes.termica(context)
        textSize = tamanho
        color = argb(cor)
    }

    private fun altura(e: Estilo): Int = when (e) {
        Estilo.GRANDE -> LINHA * 2
        Estilo.LOGO -> ALTURA_LOGO
        Estilo.ESPACO -> LINHA / 2
        Estilo.PEQUENO -> LINHA * 2 / 3
        else -> LINHA
    }

    /** Papel com as bordas de cima e de baixo serrilhadas (como uma bobina rasgada). */
    private fun papel(x0: Float, y0: Float, largura: Float, alturaPapel: Float): Path {
        val x1 = x0 + largura
        val y1 = y0 + alturaPapel
        val passo = SERRA * 2f
        val p = Path()
        p.moveTo(x0, y0 + SERRA)
        var x = x0
        while (x < x1) {
            p.lineTo(min(x + passo / 2, x1), y0)
            p.lineTo(min(x + passo, x1), y0 + SERRA)
            x += passo
        }
        p.lineTo(x1, y1 - SERRA)
        x = x1
        while (x > x0) {
            p.lineTo(max(x - passo / 2, x0), y1)
            p.lineTo(max(x - passo, x0), y1 - SERRA)
            x -= passo
        }
        p.close()
        return p
    }

    private fun logo(c: Canvas, cx: Float, cy: Float) {
        val s = 0.78f
        val tinta = Paint().apply { isAntiAlias = false; color = argb(TINTA); style = Paint.Style.FILL }
        val traco = Paint().apply {
            isAntiAlias = false; color = argb(TINTA); style = Paint.Style.STROKE
            strokeWidth = 10f * s; strokeCap = Paint.Cap.ROUND
        }
        val contorno = Paint().apply { isAntiAlias = false; color = argb(PAPEL); style = Paint.Style.STROKE; strokeWidth = 3f }
        val nervura = Paint().apply { isAntiAlias = false; color = argb(PAPEL); style = Paint.Style.STROKE; strokeWidth = 2.5f }
        c.drawPath(Fontes.caule(cx, cy, s * 0.9f), traco)
        for (g in Fontes.ANGULOS_FOLHAS) {
            val f = Fontes.folha(cx, cy, s, g)
            c.drawPath(f, tinta)
            c.drawPath(f, contorno)
        }
        for (g in Fontes.ANGULOS_FOLHAS) {
            val r = Math.toRadians((g - 90).toDouble())
            val cos = Math.cos(r).toFloat()
            val sin = Math.sin(r).toFloat()
            c.drawLine(cx + cos * 22 * s, cy + sin * 22 * s, cx + cos * 58 * s, cy + sin * 58 * s, nervura)
        }
        c.drawCircle(cx, cy, 9f * s, Paint().apply { isAntiAlias = false; color = argb(PAPEL) })
        c.drawCircle(cx, cy, 4f * s, tinta)
    }

    fun desenhar(context: Context, linhas: List<LayoutTermico.Linha>): Bitmap {
        val normal = pincel(context, TAMANHO, TINTA)
        val grande = pincel(context, TAMANHO * 2, TINTA)
        val avanco = normal.measureText("M")
        val colunas = LayoutTermico.colunas(linhas)
        val margem = avanco * 2
        val larguraPapel = margem * 2 + colunas * avanco
        val alturaPapel = linhas.sumOf { altura(it.estilo) } + margem * 2 + SERRA * 2
        val w = (larguraPapel + BORDA * 2 + DESLOCA_SOMBRA).toInt()
        val h = (alturaPapel + BORDA * 2 + DESLOCA_SOMBRA).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(argb(BRANCO))
        val chapado = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
        chapado.color = argb(SOMBRA)
        c.drawPath(papel(BORDA + DESLOCA_SOMBRA.toFloat(), BORDA + DESLOCA_SOMBRA.toFloat(), larguraPapel, alturaPapel), chapado)
        chapado.color = argb(PAPEL)
        c.drawPath(papel(BORDA.toFloat(), BORDA.toFloat(), larguraPapel, alturaPapel), chapado)

        val subida = -normal.fontMetrics.ascent
        val subidaGrande = -grande.fontMetrics.ascent
        val x0 = BORDA + margem
        val xCentro = BORDA + larguraPapel / 2
        var y = BORDA + SERRA + margem
        val invertido = pincel(context, TAMANHO, PAPEL)
        val pequeno = pincel(context, TAMANHO * 2 / 3, TINTA)
        val subidaPequena = -pequeno.fontMetrics.ascent
        for (l in linhas) {
            when (l.estilo) {
                Estilo.NORMAL -> c.drawText(l.texto, x0, y + subida, normal)
                Estilo.CENTRO -> c.drawText(l.texto, xCentro - normal.measureText(l.texto) / 2, y + subida, normal)
                Estilo.GRANDE -> c.drawText(l.texto, xCentro - grande.measureText(l.texto) / 2, y + subidaGrande, grande)
                Estilo.INVERTIDO, Estilo.FAIXA -> {
                    chapado.color = argb(TINTA)
                    c.drawRect(x0 - avanco / 2, y + 4, x0 + colunas * avanco + avanco / 2, y + LINHA - 2, chapado)
                    val x = if (l.estilo == Estilo.FAIXA) xCentro - invertido.measureText(l.texto) / 2 else x0
                    c.drawText(l.texto, x, y + subida, invertido)
                }
                Estilo.LOGO -> logo(c, xCentro, y + 72f)
                Estilo.ESPACO -> Unit
                Estilo.PEQUENO -> c.drawText(l.texto, x0, y + subidaPequena, pequeno)
            }
            y += altura(l.estilo)
        }
        return bmp
    }

    /** Converte cada ponto para o índice da paleta (a cor mais próxima, se o sistema suavizar algo). */
    private fun indices(bmp: Bitmap): ByteArray {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val saida = ByteArray(px.size)
        for (i in px.indices) {
            val rgb = px[i] and 0xFFFFFF
            var melhor = 0
            var dist = Int.MAX_VALUE
            for (k in PALETA.indices) {
                val p = PALETA[k]
                if (p == rgb) {
                    melhor = k
                    dist = 0
                    break
                }
                val d = abs((p shr 16 and 0xFF) - (rgb shr 16 and 0xFF)) + abs((p shr 8 and 0xFF) - (rgb shr 8 and 0xFF)) +
                    abs((p and 0xFF) - (rgb and 0xFF))
                if (d < dist) {
                    dist = d
                    melhor = k
                }
            }
            saida[i] = melhor.toByte()
        }
        return saida
    }

    /** PNG com paleta de 4 cores (2 bits por ponto). */
    fun png(context: Context, linhas: List<LayoutTermico.Linha>): ByteArray {
        val bmp = desenhar(context, linhas)
        try {
            return PngIndexado.codificar(PngIndexado.Imagem(bmp.width, bmp.height, indices(bmp), PALETA))
        } finally {
            bmp.recycle()
        }
    }

    fun salvar(context: Context, linhas: List<LayoutTermico.Linha>, destino: File): File {
        destino.parentFile?.mkdirs()
        val temp = File(destino.path + ".tmp")
        temp.writeBytes(png(context, linhas))
        if (!temp.renameTo(destino)) {
            destino.delete()
            temp.renameTo(destino)
        }
        return destino
    }
}

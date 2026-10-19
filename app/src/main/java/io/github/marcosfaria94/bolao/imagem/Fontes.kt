package io.github.marcosfaria94.bolao.imagem

import android.content.Context
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Typeface

/** Fontes embutidas (assets/fontes, licença OFL) e o desenho do trevo usado nas imagens. */
object Fontes {
    @Volatile
    private var termica: Typeface? = null

    @Volatile
    private var jostRegular: Typeface? = null

    @Volatile
    private var jostNegrito: Typeface? = null

    /** VT323: parecida com a letra das impressoras térmicas dos comprovantes de banco. */
    fun termica(context: Context): Typeface =
        termica ?: runCatching { Typeface.createFromAsset(context.assets, "fontes/vt323.ttf") }
            .getOrElse { Typeface.MONOSPACE }.also { termica = it }

    /** Jost: geométrica, no estilo da letra do site das Loterias Caixa. */
    fun jost(context: Context, negrito: Boolean): Typeface = if (negrito) {
        jostNegrito ?: runCatching { Typeface.createFromAsset(context.assets, "fontes/jost-bold.ttf") }
            .getOrElse { Typeface.DEFAULT_BOLD }.also { jostNegrito = it }
    } else {
        jostRegular ?: runCatching { Typeface.createFromAsset(context.assets, "fontes/jost-regular.ttf") }
            .getOrElse { Typeface.DEFAULT }.also { jostRegular = it }
    }

    /**
     * Uma folha do trevo (a mesma do ícone do app), com a ponta em (0, 0) e a folha para cima,
     * já girada [graus] e ampliada [escala] vezes em torno de (cx, cy).
     */
    fun folha(cx: Float, cy: Float, escala: Float, graus: Float): Path {
        val p = Path()
        p.moveTo(0f, 0f)
        p.cubicTo(-10f, -10f, -40f, -30f, -40f, -55f)
        p.cubicTo(-40f, -75f, -20f, -85f, 0f, -68f)
        p.cubicTo(20f, -85f, 40f, -75f, 40f, -55f)
        p.cubicTo(40f, -30f, 10f, -10f, 0f, 0f)
        p.close()
        val m = Matrix()
        m.postScale(escala, escala)
        m.postRotate(graus)
        m.postTranslate(cx, cy)
        p.transform(m)
        return p
    }

    /** Caule do trevo, saindo de (cx, cy). */
    fun caule(cx: Float, cy: Float, escala: Float): Path {
        val p = Path()
        p.moveTo(0f, 0f)
        p.cubicTo(4f, 40f, 14f, 75f, 34f, 112f)
        val m = Matrix()
        m.postScale(escala, escala)
        m.postTranslate(cx, cy)
        p.transform(m)
        return p
    }

    val ANGULOS_FOLHAS = floatArrayOf(45f, 135f, 225f, 315f)
}

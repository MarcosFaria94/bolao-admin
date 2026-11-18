package io.github.marcosfaria94.bolao.notificacoes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Tarjas

/**
 * O desenho grande dos avisos do celular (desde a 0.41, pedido do usuário: "reformular os pushs"): um círculo com o
 * trevo de corações das tarjas. Na cor da modalidade (o fundo e as folhas da tarja dela) quando o aviso é de uma
 * modalidade; no azul-noite com o trevo dourado (as cores do app) quando é do bolão (pendências) ou de várias.
 */
object Emblema {

    /** O tamanho do desenho (px): o Android reduz para o lugar do ícone grande. */
    private const val TAMANHO = 192

    private const val AZUL_NOITE = 0xFF13294B.toInt()
    private const val DOURADO = 0xFFF2B630.toInt()
    private const val DOURADO_CLARO = 0xFFF7D98A.toInt()

    /** As cores (fundo, folha cheia, folha clara) do emblema de [m] (null = o do app). */
    fun cores(m: Modalidade?): Triple<Int, Int, Int> {
        if (m == null) return Triple(AZUL_NOITE, DOURADO, DOURADO_CLARO)
        val c = Tarjas.cores(m)
        return Triple(c.fundo.toInt(), c.folhaCheia.toInt(), c.folhaClara.toInt())
    }

    fun de(m: Modalidade?): Bitmap {
        val (fundo, cheia, clara) = cores(m)
        val b = Bitmap.createBitmap(TAMANHO, TAMANHO, Bitmap.Config.ARGB_8888)
        val tela = Canvas(b)
        val tinta = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val meio = TAMANHO / 2f
        tinta.color = fundo
        tela.drawCircle(meio, meio, meio, tinta)
        // O trevo: as 4 folhas (duas cheias e duas claras), como nas tarjas.
        for ((angulo, ehCheia) in Tarjas.FOLHAS) {
            val (inicio, curvas) = Tarjas.folha(angulo, meio, meio, TAMANHO * 0.62f)
            val caminho = Path().apply {
                moveTo(inicio[0], inicio[1])
                for (k in curvas) cubicTo(k[0], k[1], k[2], k[3], k[4], k[5])
                close()
            }
            tinta.color = if (ehCheia) cheia else clara
            tela.drawPath(caminho, tinta)
        }
        return b
    }
}

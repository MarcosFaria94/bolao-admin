package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Ícones próprios do app: o trevo do ícone, para a aba Apostas e as listas. */
object Icones {
    // As quatro folhas do trevo do ícone (a mesma folha das imagens), centradas em (12; 10,4).
    private const val FOLHAS =
        "M12.00,10.40C12.00,9.01 11.31,5.55 13.04,3.82C14.43,2.43 16.50,3.12 16.71,5.69C19.28,5.90 19.97,7.97 18.58,9.36" +
            "C16.85,11.09 13.39,10.40 12.00,10.40ZM12.00,10.40C13.39,10.40 16.85,9.71 18.58,11.44C19.97,12.83 19.28,14.90 16.71,15.11" +
            "C16.50,17.68 14.43,18.37 13.04,16.98C11.31,15.25 12.00,11.79 12.00,10.40ZM12.00,10.40C12.00,11.79 12.69,15.25 10.96,16.98" +
            "C9.57,18.37 7.50,17.68 7.29,15.11C4.72,14.90 4.03,12.83 5.42,11.44C7.15,9.71 10.61,10.40 12.00,10.40ZM12.00,10.40" +
            "C10.61,10.40 7.15,11.09 5.42,9.36C4.03,7.97 4.72,5.90 7.29,5.69C7.50,3.12 9.57,2.43 10.96,3.82C12.69,5.55 12.00,9.01 12.00,10.40Z"
    private const val CAULE = "M12.00,10.40C12.50,14.60 13.60,18.40 15.60,22.00"

    private fun trevo(nome: String, cheio: Boolean): ImageVector =
        ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = addPathNodes(FOLHAS),
                fill = if (cheio) SolidColor(Color.Black) else null,
                stroke = if (cheio) null else SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                pathData = addPathNodes(CAULE),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
            )
        }.build()

    /** Trevo de contorno (aba não escolhida, listas). */
    val Trevo: ImageVector by lazy { trevo("Trevo", cheio = false) }

    /** Trevo cheio (aba escolhida). */
    val TrevoCheio: ImageVector by lazy { trevo("TrevoCheio", cheio = true) }
}

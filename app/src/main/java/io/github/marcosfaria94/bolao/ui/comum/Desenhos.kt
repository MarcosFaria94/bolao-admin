package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Desenhos coloridos dos emblemas (círculos do Histórico e dos atalhos da Home): cofrinho do
 * aporte, as 3 pilhas de moedas do rateio, o extrato, o troféu do prêmio, o rendimento e o
 * complemento do Administrador. Cada um tem as próprias cores (vão sem tint) numa grade de 48.
 */
object Desenhos {

    private fun desenho(nome: String, bloco: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(nome, 48.dp, 48.dp, 48f, 48f).apply(bloco).build()

    private fun ImageVector.Builder.forma(
        d: String,
        cor: Long? = null,
        traco: Long? = null,
        espessura: Float = 0f,
        redondo: Boolean = false,
    ) {
        addPath(
            pathData = addPathNodes(d),
            fill = cor?.let { SolidColor(Color(it)) },
            stroke = traco?.let { SolidColor(Color(it)) },
            strokeLineWidth = espessura,
            strokeLineCap = if (redondo) StrokeCap.Round else StrokeCap.Butt,
            strokeLineJoin = if (redondo) StrokeJoin.Round else StrokeJoin.Miter,
        )
    }

    val Cofrinho: ImageVector by lazy {
        desenho("Cofrinho") {
            forma("M9.5,27.5 C6.5,27.5 5.2,25 6.6,23.4", traco = 0xFFE86E9C, espessura = 1.8f, redondo = true)
            forma("M15.4,33H16.6A2.4,2.4 0 0,1 19,35.4V38.6A2.4,2.4 0 0,1 16.6,41H15.4A2.4,2.4 0 0,1 13,38.6V35.4A2.4,2.4 0 0,1 15.4,33Z", cor = 0xFFE86E9C)
            forma("M29.4,33H30.6A2.4,2.4 0 0,1 33,35.4V38.6A2.4,2.4 0 0,1 30.6,41H29.4A2.4,2.4 0 0,1 27,38.6V35.4A2.4,2.4 0 0,1 29.4,33Z", cor = 0xFFE86E9C)
            forma("M8,27.5A15,11.5 0 1,0 38,27.5A15,11.5 0 1,0 8,27.5Z", cor = 0xFFF7A1C0)
            forma("M25.5,24.5A8.5,8.5 0 1,0 42.5,24.5A8.5,8.5 0 1,0 25.5,24.5Z", cor = 0xFFF7A1C0)
            forma("M30.5,17.2 L33.2,10.8 L37.6,17.6 Z", cor = 0xFFE86E9C)
            forma("M38.4,26.5A3.6,4.4 0 1,0 45.6,26.5A3.6,4.4 0 1,0 38.4,26.5Z", cor = 0xFFEE7FA9)
            forma("M40.3,25.2A0.9,0.9 0 1,0 42.1,25.2A0.9,0.9 0 1,0 40.3,25.2Z", cor = 0xFFA92C5E)
            forma("M40.5,27.9A0.9,0.9 0 1,0 42.3,27.9A0.9,0.9 0 1,0 40.5,27.9Z", cor = 0xFFA92C5E)
            forma("M34.9,21.8A1.5,1.5 0 1,0 37.9,21.8A1.5,1.5 0 1,0 34.9,21.8Z", cor = 0xFF2B1B24)
            forma("M10.5,23.5A6.5,3.2 0 1,0 23.5,23.5A6.5,3.2 0 1,0 10.5,23.5Z", cor = 0xFFFCC8DA)
            forma("M19.5,17.2 L27,17.2", traco = 0xFFA92C5E, espessura = 1.8f, redondo = true)
            forma("M17.2,9.6A6,6 0 1,0 29.2,9.6A6,6 0 1,0 17.2,9.6Z", cor = 0xFFF2B630, traco = 0xFFC98A12, espessura = 1.2f)
            forma("M19.6,9.6A3.6,3.6 0 1,0 26.8,9.6A3.6,3.6 0 1,0 19.6,9.6Z", traco = 0xFFFFE08A, espessura = 1.1f)
        }
    }

    val Moedas: ImageVector by lazy {
        desenho("Moedas") {
            forma("M6.6,37H14.4A1.6,1.6 0 0,1 16,38.6V39.4A1.6,1.6 0 0,1 14.4,41H6.6A1.6,1.6 0 0,1 5,39.4V38.6A1.6,1.6 0 0,1 6.6,37Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M6.75,37.9H14.25A0.55,0.55 0 0,1 14.8,38.45V38.45A0.55,0.55 0 0,1 14.25,39H6.75A0.55,0.55 0 0,1 6.2,38.45V38.45A0.55,0.55 0 0,1 6.75,37.9Z", cor = 0xFFFFD66B)
            forma("M6.6,33H14.4A1.6,1.6 0 0,1 16,34.6V35.4A1.6,1.6 0 0,1 14.4,37H6.6A1.6,1.6 0 0,1 5,35.4V34.6A1.6,1.6 0 0,1 6.6,33Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M6.75,33.9H14.25A0.55,0.55 0 0,1 14.8,34.45V34.45A0.55,0.55 0 0,1 14.25,35H6.75A0.55,0.55 0 0,1 6.2,34.45V34.45A0.55,0.55 0 0,1 6.75,33.9Z", cor = 0xFFFFD66B)
            forma("M6.6,29H14.4A1.6,1.6 0 0,1 16,30.6V31.4A1.6,1.6 0 0,1 14.4,33H6.6A1.6,1.6 0 0,1 5,31.4V30.6A1.6,1.6 0 0,1 6.6,29Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M6.75,29.9H14.25A0.55,0.55 0 0,1 14.8,30.45V30.45A0.55,0.55 0 0,1 14.25,31H6.75A0.55,0.55 0 0,1 6.2,30.45V30.45A0.55,0.55 0 0,1 6.75,29.9Z", cor = 0xFFFFD66B)
            forma("M5,29A5.5,2 0 1,0 16,29A5.5,2 0 1,0 5,29Z", cor = 0xFFFFE08A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M33.6,37H41.4A1.6,1.6 0 0,1 43,38.6V39.4A1.6,1.6 0 0,1 41.4,41H33.6A1.6,1.6 0 0,1 32,39.4V38.6A1.6,1.6 0 0,1 33.6,37Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M33.75,37.9H41.25A0.55,0.55 0 0,1 41.8,38.45V38.45A0.55,0.55 0 0,1 41.25,39H33.75A0.55,0.55 0 0,1 33.2,38.45V38.45A0.55,0.55 0 0,1 33.75,37.9Z", cor = 0xFFFFD66B)
            forma("M33.6,33H41.4A1.6,1.6 0 0,1 43,34.6V35.4A1.6,1.6 0 0,1 41.4,37H33.6A1.6,1.6 0 0,1 32,35.4V34.6A1.6,1.6 0 0,1 33.6,33Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M33.75,33.9H41.25A0.55,0.55 0 0,1 41.8,34.45V34.45A0.55,0.55 0 0,1 41.25,35H33.75A0.55,0.55 0 0,1 33.2,34.45V34.45A0.55,0.55 0 0,1 33.75,33.9Z", cor = 0xFFFFD66B)
            forma("M33.6,29H41.4A1.6,1.6 0 0,1 43,30.6V31.4A1.6,1.6 0 0,1 41.4,33H33.6A1.6,1.6 0 0,1 32,31.4V30.6A1.6,1.6 0 0,1 33.6,29Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M33.75,29.9H41.25A0.55,0.55 0 0,1 41.8,30.45V30.45A0.55,0.55 0 0,1 41.25,31H33.75A0.55,0.55 0 0,1 33.2,30.45V30.45A0.55,0.55 0 0,1 33.75,29.9Z", cor = 0xFFFFD66B)
            forma("M33.6,25H41.4A1.6,1.6 0 0,1 43,26.6V27.4A1.6,1.6 0 0,1 41.4,29H33.6A1.6,1.6 0 0,1 32,27.4V26.6A1.6,1.6 0 0,1 33.6,25Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M33.75,25.9H41.25A0.55,0.55 0 0,1 41.8,26.45V26.45A0.55,0.55 0 0,1 41.25,27H33.75A0.55,0.55 0 0,1 33.2,26.45V26.45A0.55,0.55 0 0,1 33.75,25.9Z", cor = 0xFFFFD66B)
            forma("M32,25A5.5,2 0 1,0 43,25A5.5,2 0 1,0 32,25Z", cor = 0xFFFFE08A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M20.1,37H27.9A1.6,1.6 0 0,1 29.5,38.6V39.4A1.6,1.6 0 0,1 27.9,41H20.1A1.6,1.6 0 0,1 18.5,39.4V38.6A1.6,1.6 0 0,1 20.1,37Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M20.25,37.9H27.75A0.55,0.55 0 0,1 28.3,38.45V38.45A0.55,0.55 0 0,1 27.75,39H20.25A0.55,0.55 0 0,1 19.7,38.45V38.45A0.55,0.55 0 0,1 20.25,37.9Z", cor = 0xFFFFD66B)
            forma("M20.1,33H27.9A1.6,1.6 0 0,1 29.5,34.6V35.4A1.6,1.6 0 0,1 27.9,37H20.1A1.6,1.6 0 0,1 18.5,35.4V34.6A1.6,1.6 0 0,1 20.1,33Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M20.25,33.9H27.75A0.55,0.55 0 0,1 28.3,34.45V34.45A0.55,0.55 0 0,1 27.75,35H20.25A0.55,0.55 0 0,1 19.7,34.45V34.45A0.55,0.55 0 0,1 20.25,33.9Z", cor = 0xFFFFD66B)
            forma("M20.1,29H27.9A1.6,1.6 0 0,1 29.5,30.6V31.4A1.6,1.6 0 0,1 27.9,33H20.1A1.6,1.6 0 0,1 18.5,31.4V30.6A1.6,1.6 0 0,1 20.1,29Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M20.25,29.9H27.75A0.55,0.55 0 0,1 28.3,30.45V30.45A0.55,0.55 0 0,1 27.75,31H20.25A0.55,0.55 0 0,1 19.7,30.45V30.45A0.55,0.55 0 0,1 20.25,29.9Z", cor = 0xFFFFD66B)
            forma("M20.1,25H27.9A1.6,1.6 0 0,1 29.5,26.6V27.4A1.6,1.6 0 0,1 27.9,29H20.1A1.6,1.6 0 0,1 18.5,27.4V26.6A1.6,1.6 0 0,1 20.1,25Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M20.25,25.9H27.75A0.55,0.55 0 0,1 28.3,26.45V26.45A0.55,0.55 0 0,1 27.75,27H20.25A0.55,0.55 0 0,1 19.7,26.45V26.45A0.55,0.55 0 0,1 20.25,25.9Z", cor = 0xFFFFD66B)
            forma("M20.1,21H27.9A1.6,1.6 0 0,1 29.5,22.6V23.4A1.6,1.6 0 0,1 27.9,25H20.1A1.6,1.6 0 0,1 18.5,23.4V22.6A1.6,1.6 0 0,1 20.1,21Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M20.25,21.9H27.75A0.55,0.55 0 0,1 28.3,22.45V22.45A0.55,0.55 0 0,1 27.75,23H20.25A0.55,0.55 0 0,1 19.7,22.45V22.45A0.55,0.55 0 0,1 20.25,21.9Z", cor = 0xFFFFD66B)
            forma("M20.1,17H27.9A1.6,1.6 0 0,1 29.5,18.6V19.4A1.6,1.6 0 0,1 27.9,21H20.1A1.6,1.6 0 0,1 18.5,19.4V18.6A1.6,1.6 0 0,1 20.1,17Z", cor = 0xFFE9A92A, traco = 0xFFA8740C, espessura = 0.7f)
            forma("M20.25,17.9H27.75A0.55,0.55 0 0,1 28.3,18.45V18.45A0.55,0.55 0 0,1 27.75,19H20.25A0.55,0.55 0 0,1 19.7,18.45V18.45A0.55,0.55 0 0,1 20.25,17.9Z", cor = 0xFFFFD66B)
            forma("M18.5,17A5.5,2 0 1,0 29.5,17A5.5,2 0 1,0 18.5,17Z", cor = 0xFFFFE08A, traco = 0xFFA8740C, espessura = 0.7f)
        }
    }

    val Extrato: ImageVector by lazy {
        desenho("Extrato") {
            forma("M11,6 L37,6 L37,40 L34,37.5 L31,40 L28,37.5 L25,40 L22,37.5 L19,40 L16,37.5 L13,40 L11,38.3 Z", cor = 0xFFFFFFFF, traco = 0xFFCBD5E1, espessura = 0.8f, redondo = true)
            forma("M11,6H37V12H11Z", cor = 0xFF2563EB)
            forma("M15.5,17.5 L32.5,17.5 M15.5,22.5 L29,22.5 M15.5,27.5 L31,27.5", traco = 0xFF94A3B8, espessura = 2.2f, redondo = true)
            forma("M27.5,35A7,7 0 1,0 41.5,35A7,7 0 1,0 27.5,35Z", cor = 0xFF22C55E, traco = 0xFF15803D, espessura = 1f)
            forma("M31.2,35.2 L33.6,37.6 L37.8,32.6", traco = 0xFFFFFFFF, espessura = 2f, redondo = true)
        }
    }

    val Trofeu: ImageVector by lazy {
        desenho("Trofeu") {
            forma("M14,10 C7,10 7,20 15.5,21.5", traco = 0xFFD9971C, espessura = 2.6f, redondo = true)
            forma("M34,10 C41,10 41,20 32.5,21.5", traco = 0xFFD9971C, espessura = 2.6f, redondo = true)
            forma("M13,7 L35,7 L35,12 C35,21 30,27 24,27 C18,27 13,21 13,12 Z", cor = 0xFFF2B630)
            forma("M16,9.5 L19,9.5 L19,15 C19,19 20.5,22 22,23.5 C18.5,22.5 16,18.5 16,13 Z", cor = 0xFFFFE08A)
            forma("M21.5,26.5H26.5V32.5H21.5Z", cor = 0xFFD9971C)
            forma("M17.5,32H30.5A1.5,1.5 0 0,1 32,33.5V35A1.5,1.5 0 0,1 30.5,36.5H17.5A1.5,1.5 0 0,1 16,35V33.5A1.5,1.5 0 0,1 17.5,32Z", cor = 0xFFF2B630)
            forma("M15,36H33A1.5,1.5 0 0,1 34.5,37.5V39.5A1.5,1.5 0 0,1 33,41H15A1.5,1.5 0 0,1 13.5,39.5V37.5A1.5,1.5 0 0,1 15,36Z", cor = 0xFF8A5A12)
            forma("M24,11.5 L25.3,14.4 L28.4,14.6 L26,16.6 L26.8,19.6 L24,18 L21.2,19.6 L22,16.6 L19.6,14.6 L22.7,14.4 Z", cor = 0xFFFFF6D6)
        }
    }

    val Rendimento: ImageVector by lazy {
        desenho("Rendimento") {
            forma("M10.5,30H14.5A1.5,1.5 0 0,1 16,31.5V38.5A1.5,1.5 0 0,1 14.5,40H10.5A1.5,1.5 0 0,1 9,38.5V31.5A1.5,1.5 0 0,1 10.5,30Z", cor = 0xFFBBF7D0)
            forma("M22,24H26A1.5,1.5 0 0,1 27.5,25.5V38.5A1.5,1.5 0 0,1 26,40H22A1.5,1.5 0 0,1 20.5,38.5V25.5A1.5,1.5 0 0,1 22,24Z", cor = 0xFF86EFAC)
            forma("M33.5,17H37.5A1.5,1.5 0 0,1 39,18.5V38.5A1.5,1.5 0 0,1 37.5,40H33.5A1.5,1.5 0 0,1 32,38.5V18.5A1.5,1.5 0 0,1 33.5,17Z", cor = 0xFF4ADE80)
            forma("M8,25 L18,16 L25,21 L37,9", traco = 0xFFFFFFFF, espessura = 2.8f, redondo = true)
            forma("M30.5,8.5 L38,8 L37.5,15.5", traco = 0xFFFFFFFF, espessura = 2.8f, redondo = true)
        }
    }

    val Complemento: ImageVector by lazy {
        desenho("Complemento") {
            forma("M9,24A15,15 0 1,0 39,24A15,15 0 1,0 9,24Z", cor = 0xFFF2B630, traco = 0xFFC98A12, espessura = 1.6f)
            forma("M13,24A11,11 0 1,0 35,24A11,11 0 1,0 13,24Z", traco = 0xFFFFE08A, espessura = 1.4f)
            forma("M24,17 L24,31 M17,24 L31,24", traco = 0xFF8A5A12, espessura = 3.4f, redondo = true)
        }
    }
}

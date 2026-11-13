package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Ícones próprios do app: o trevo do ícone (listas e avisos; até a 0.30, a aba Jogos), as cartas da aba Jogos, o
 * trevo das Loterias Caixa (o botão da tela da Caixa) e o painel do Painel de Jogos.
 */
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

    /** Trevo de contorno (listas e avisos; até a 0.30, a aba Jogos não escolhida). */
    val Trevo: ImageVector by lazy { trevo("Trevo", cheio = false) }

    /** Trevo cheio (o filtro do Histórico; até a 0.30, a aba Jogos escolhida). */
    val TrevoCheio: ImageVector by lazy { trevo("TrevoCheio", cheio = true) }

    /**
     * As cartas da aba Jogos (desde a 0.31; até a 0.30, o trevo, que passou para o botão da Caixa): o "cards" dos
     * Tabler Icons (MIT, (c) Paweł Kuna), em traço de 2 numa grade de 24 (aba não escolhida).
     */
    val Cartas: ImageVector by lazy {
        ImageVector.Builder("Cartas", 24.dp, 24.dp, 24f, 24f).apply {
            listOf(
                "M3.604,7.197l7.138,-3.109a0.96,0.96 0 0,1 1.27,0.527l4.924,11.902a1,1 0 0,1 -0.514,1.304l-7.137,3.109" +
                    "a0.96,0.96 0 0,1 -1.271,-0.527l-4.924,-11.903a1,1 0 0,1 0.514,-1.304z",
                "M15,4h1a1,1 0 0,1 1,1v3.5",
                "M20,6c0.264,0.112 0.52,0.217 0.768,0.315a1,1 0 0,1 0.53,1.311l-2.298,5.374",
            ).forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }

    /** As cartas cheias (aba Jogos escolhida): o "cards" cheio dos Tabler Icons. */
    val CartasCheias: ImageVector by lazy {
        ImageVector.Builder("CartasCheias", 24.dp, 24.dp, 24f, 24f).apply {
            listOf(
                "M10.348,3.169l-7.15,3.113a2,2 0 0,0 -1.03,2.608l4.92,11.895a1.96,1.96 0 0,0 2.59,1.063l7.142,-3.11" +
                    "a2.002,2.002 0 0,0 1.036,-2.611l-4.92,-11.894a1.96,1.96 0 0,0 -2.588,-1.064z",
                "M16,3a2,2 0 0,1 1.995,1.85l0.005,0.15v3.5a1,1 0 0,1 -1.993,0.117l-0.007,-0.117v-3.5h-1" +
                    "a1,1 0 0,1 -0.117,-1.993l0.117,-0.007h1z",
                "M19.08,5.61a1,1 0 0,1 1.31,-0.53c0.257,0.108 0.505,0.21 0.769,0.314a2,2 0 0,1 1.114,2.479l-0.056,0.146" +
                    "l-2.298,5.374a1,1 0 0,1 -1.878,-0.676l0.04,-0.11l2.296,-5.371l-0.366,-0.148l-0.402,-0.167a1,1 0 0,1 -0.53,-1.312z",
            ).forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) }
        }.build()
    }

    /** O lado do quadrado de cada pétala do trevo das Loterias Caixa (grade de 24; desde a 0.32). */
    private const val LADO_DA_PETALA = 6.53f

    /** O vão reto entre as pétalas (a cruz do meio, como na marca das Loterias Caixa; desde a 0.32). */
    private const val VAO_DA_CAIXA = 1.4f

    /** A largura do contorno das pétalas vazias (por dentro da pétala, para o vão continuar aberto). */
    private const val TRACO_DA_CAIXA = 1.5f

    /**
     * Uma pétala do trevo das Loterias Caixa (desde a 0.32): o coração clássico, um quadrado com dois semicírculos
     * em dois lados, com a ponta (o canto do quadrado) no centro do trevo e os lados retos ao longo do vão. [quarto]:
     * 0 = em cima à esquerda, 1 = em cima à direita, 2 = embaixo à direita, 3 = embaixo à esquerda (girando 90° no
     * sentido do relógio). [recuo]: o desenho encolhido por dentro (para o contorno não fechar o vão).
     */
    private fun petalaDaCaixa(quarto: Int, recuo: Float): List<PathNode> {
        val s = LADO_DA_PETALA
        val centro = 12f
        val cx = centro - VAO_DA_CAIXA / 2
        val cy = centro - VAO_DA_CAIXA / 2
        val r = s / 2 - recuo
        // Os centros dos dois semicírculos e o encontro deles (a reentrância do coração).
        val ax = cx - s / 2
        val ay = cy - s
        val bx = cx - s
        val by = cy - s / 2
        val meia = hypot(ax - bx, ay - by) / 2
        val h = sqrt(max(r * r - meia * meia, 0f))
        val nx = (ax + bx) / 2 - h / sqrt(2f)
        val ny = (ay + by) / 2 - h / sqrt(2f)
        fun girar(x: Float, y: Float): Pair<Float, Float> {
            var dx = x - centro
            var dy = y - centro
            repeat(quarto) {
                val t = dx
                dx = -dy
                dy = t
            }
            return dx + centro to dy + centro
        }
        return PathBuilder().apply {
            val (x0, y0) = girar(cx - recuo, cy - recuo)
            moveTo(x0, y0)
            val (x1, y1) = girar(cx - recuo, cy - s)
            lineTo(x1, y1)
            val (x2, y2) = girar(nx, ny)
            arcTo(r, r, 0f, recuo > 0f, false, x2, y2)
            val (x3, y3) = girar(cx - s, cy - recuo)
            arcTo(r, r, 0f, false, false, x3, y3)
            close()
        }.nodes
    }

    /**
     * O trevo das Loterias Caixa (o botão da Caixa na barra de baixo, desde a 0.31): quatro pétalas em coração com as
     * pontas no centro e, desde a 0.32, o vão reto entre elas (a cruz da marca; até a 0.31 eram as folhas das
     * tarjas, quase encostadas). As duas de uma diagonal são cheias (a de cima à esquerda e a de baixo à direita,
     * como na marca). Não escolhido, as outras duas só no contorno; escolhido, as outras duas claras.
     */
    private fun trevoDaCaixa(nome: String, escolhido: Boolean): ImageVector =
        ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f).apply {
            for (quarto in 0 until 4) {
                val cheia = quarto % 2 == 0
                when {
                    cheia -> addPath(pathData = petalaDaCaixa(quarto, 0f), fill = SolidColor(Color.Black))
                    escolhido -> addPath(pathData = petalaDaCaixa(quarto, 0f), fill = SolidColor(Color.Black), fillAlpha = 0.45f)
                    else -> addPath(
                        pathData = petalaDaCaixa(quarto, TRACO_DA_CAIXA / 2),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = TRACO_DA_CAIXA,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
        }.build()

    /** O trevo das Loterias Caixa, botão não escolhido. */
    val TrevoDaCaixa: ImageVector by lazy { trevoDaCaixa("TrevoDaCaixa", escolhido = false) }

    /** O trevo das Loterias Caixa, botão escolhido (na tela da Caixa). */
    val TrevoDaCaixaEscolhido: ImageVector by lazy { trevoDaCaixa("TrevoDaCaixaEscolhido", escolhido = true) }

    /**
     * O painel (os 4 blocos) do Painel de Jogos, desde a 0.18 (até a 0.17, a bola): o "layout-dashboard"
     * dos Tabler Icons (MIT, (c) Paweł Kuna), em traço de 2 numa grade de 24.
     */
    val Painel: ImageVector by lazy {
        ImageVector.Builder("Painel", 24.dp, 24.dp, 24f, 24f).apply {
            listOf(
                "M5,4h4a1,1 0 0,1 1,1v6a1,1 0 0,1 -1,1h-4a1,1 0 0,1 -1,-1v-6a1,1 0 0,1 1,-1",
                "M5,16h4a1,1 0 0,1 1,1v2a1,1 0 0,1 -1,1h-4a1,1 0 0,1 -1,-1v-2a1,1 0 0,1 1,-1",
                "M15,12h4a1,1 0 0,1 1,1v6a1,1 0 0,1 -1,1h-4a1,1 0 0,1 -1,-1v-6a1,1 0 0,1 1,-1",
                "M15,4h4a1,1 0 0,1 1,1v2a1,1 0 0,1 -1,1h-4a1,1 0 0,1 -1,-1v-2a1,1 0 0,1 1,-1",
            ).forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }

    /**
     * As setas das sessões (o botão "Alternar sessão", desde a 0.21): o "switch-horizontal" dos Tabler
     * Icons (MIT, (c) Paweł Kuna), em traço de 2 numa grade de 24.
     */
    val Sessoes: ImageVector by lazy {
        ImageVector.Builder("Sessoes", 24.dp, 24.dp, 24f, 24f).apply {
            listOf("M16,3l4,4l-4,4", "M10,7l10,0", "M8,13l-4,4l4,4", "M4,17l9,0").forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }
}

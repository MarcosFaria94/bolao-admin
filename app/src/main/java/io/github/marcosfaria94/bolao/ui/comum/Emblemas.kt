package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.tema.corModalidade

/** Cores dos círculos dos emblemas dos lançamentos (iguais nos dois temas). */
object CoresEmblema {
    val APORTE = Color(0xFF1F8F5F)
    val RATEIO = Color(0xFF2557D6)
    val EXTRATO = Color(0xFF7C3AED)
    val SALDO_INICIAL = Color(0xFF0E9F8A)
    val RENDIMENTO = Color(0xFF15803D)
    val COMPLEMENTO = Color(0xFF9333EA)
    val PREMIO = Color(0xFFB7791F)
}

private fun Color.clarear(f: Float): Color = lerp(this, Color.White, f)
private fun Color.escurecer(f: Float): Color = lerp(this, Color.Black, f)

/** Fundo dos círculos: a cor com um leve degradê de cima para baixo. */
private fun fundoCirculo(cor: Color): Brush = Brush.verticalGradient(listOf(cor.clarear(0.12f), cor.escurecer(0.10f)))

/**
 * Bola 3D, como as das loterias: a luz vem do alto, à esquerda (degradê radial da cor clara para
 * a escura, com a borda mais escura), um brilho pequeno perto da luz, um reflexo fraco na borda de
 * baixo e uma sombra suave embaixo, fora do círculo.
 */
fun Modifier.bola3D(cor: Color): Modifier = drawBehind {
    val r = size.minDimension / 2f
    val c = center
    // Sombra suave embaixo (aparece só fora da bola, mais para baixo).
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Black.copy(alpha = 0.26f),
            0.80f to Color.Black.copy(alpha = 0.12f),
            1f to Color.Transparent,
            center = Offset(c.x, c.y + r * 0.14f),
            radius = r * 1.10f,
        ),
        radius = r * 1.10f,
        center = Offset(c.x, c.y + r * 0.14f),
    )
    // Corpo da bola.
    drawCircle(
        brush = Brush.radialGradient(
            0f to cor.clarear(0.30f),
            0.45f to cor,
            0.85f to cor.escurecer(0.30f),
            1f to cor.escurecer(0.45f),
            center = Offset(c.x - r * 0.28f, c.y - r * 0.34f),
            radius = r * 1.45f,
        ),
        radius = r,
        center = c,
    )
    // Reflexo fraco na borda de baixo (dá volume).
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            0.72f to Color.Transparent,
            1f to Color.White.copy(alpha = 0.12f),
            center = Offset(c.x, c.y - r * 0.18f),
            radius = r,
        ),
        radius = r,
        center = c,
    )
    // Brilho perto da luz.
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.72f),
            0.35f to Color.White.copy(alpha = 0.32f),
            1f to Color.Transparent,
            center = Offset(c.x - r * 0.36f, c.y - r * 0.50f),
            radius = r * 0.46f,
        ),
        radius = r * 0.46f,
        center = Offset(c.x - r * 0.36f, c.y - r * 0.50f),
    )
}

/**
 * Tamanho de letra preso ao tamanho do círculo: não cresce com a letra maior do sistema
 * (o texto tem de caber dentro do emblema).
 */
@Composable
private fun Dp.emSp(): TextUnit = with(LocalDensity.current) { this@emSp.toSp() }

/** Letra branca com uma sombra leve embaixo, para ler bem sobre a bola 3D. */
@Composable
fun estiloNaBola(tamanho: Dp): TextStyle {
    val d = LocalDensity.current
    return TextStyle(
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.40f),
            offset = Offset(0f, with(d) { (tamanho * 0.025f).toPx() }),
            blurRadius = with(d) { (tamanho * 0.05f).toPx() },
        ),
    )
}

/**
 * Emblema de uma modalidade: bola 3D na cor dela com a sigla (ou um [desenho], como o troféu do
 * prêmio) e, embaixo, o número do concurso. Sem concurso, só a sigla, um pouco maior.
 */
@Composable
fun EmblemaModalidade(
    m: Modalidade,
    concurso: Int?,
    tamanho: Dp = 56.dp,
    modifier: Modifier = Modifier,
    desenho: ImageVector? = null,
) {
    val numero = concurso?.takeIf { it > 0 }?.toString()
    val letraSigla = (tamanho * if (numero != null) 0.37f else 0.46f).emSp()
    val letraNumero = (tamanho * 0.225f).emSp()
    val estilo = estiloNaBola(tamanho)
    Box(
        modifier.size(tamanho).bola3D(corModalidade(m.cor)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (desenho != null) {
                Image(desenho, contentDescription = null, modifier = Modifier.size(tamanho * if (numero != null) 0.50f else 0.66f))
            } else {
                Text(
                    m.sigla,
                    style = estilo,
                    fontWeight = FontWeight.Bold,
                    fontSize = letraSigla,
                    lineHeight = letraSigla * 1.05f,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
            if (numero != null) {
                Text(
                    numero,
                    style = estilo,
                    fontWeight = FontWeight.Bold,
                    fontSize = letraNumero,
                    lineHeight = letraNumero * 1.1f,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Emblema de um tipo de lançamento: círculo colorido com o desenho (cofrinho, moedas...). */
@Composable
fun EmblemaDesenho(desenho: ImageVector, cor: Color, tamanho: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(tamanho).background(fundoCirculo(cor), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(desenho, contentDescription = null, modifier = Modifier.size(tamanho * 0.75f))
    }
}

/** Emblema com um ícone (branco, ou na [corIcone]) num círculo colorido (Ajustes, Notificações). */
@Composable
fun EmblemaIcone(
    icone: ImageVector,
    cor: Color,
    tamanho: Dp = 44.dp,
    modifier: Modifier = Modifier,
    corIcone: Color = Color.White,
) {
    Box(
        modifier.size(tamanho).background(fundoCirculo(cor), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(tamanho * 0.52f))
    }
}

/** Emblema com as iniciais de um nome num círculo colorido (integrantes, em Ajustes). */
@Composable
fun EmblemaIniciais(nome: String, cor: Color, tamanho: Dp = 48.dp, modifier: Modifier = Modifier) {
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val iniciais = when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes[0].take(1)
        else -> partes.first().take(1) + partes.last().take(1)
    }.uppercase()
    val letra = (tamanho * 0.38f).emSp()
    Box(
        modifier.size(tamanho).background(fundoCirculo(cor), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciais, color = Color.White, fontWeight = FontWeight.Bold, fontSize = letra, lineHeight = letra, maxLines = 1)
    }
}

/** Emblema do saldo inicial: o cifrão. */
@Composable
fun EmblemaCifrao(tamanho: Dp = 56.dp, modifier: Modifier = Modifier) {
    val letra = (tamanho * 0.52f).emSp()
    Box(
        modifier.size(tamanho).background(fundoCirculo(CoresEmblema.SALDO_INICIAL), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$", color = Color.White, fontWeight = FontWeight.Black, fontSize = letra, lineHeight = letra, maxLines = 1)
    }
}

/**
 * Emblema de um lançamento no Histórico do Financeiro: a modalidade (sigla e concurso) nas
 * apostas, o troféu com o concurso nos prêmios, e um desenho colorido nos outros.
 */
@Composable
fun EmblemaLancamento(e: Estado, l: Lancamento, tamanho: Dp = 56.dp) {
    val a = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
    when (l.tipo) {
        TipoLancamento.APOSTA ->
            if (a != null) EmblemaModalidade(a.modalidade, a.concurso, tamanho)
            else EmblemaDesenho(Desenhos.Moedas, CoresEmblema.RATEIO, tamanho)
        TipoLancamento.PREMIO ->
            if (a != null) EmblemaModalidade(a.modalidade, a.concurso, tamanho, desenho = Desenhos.Trofeu)
            else EmblemaDesenho(Desenhos.Trofeu, CoresEmblema.PREMIO, tamanho)
        TipoLancamento.APORTE -> EmblemaDesenho(Desenhos.Cofrinho, CoresEmblema.APORTE, tamanho)
        TipoLancamento.RATEIO, TipoLancamento.DIVISAO_FINAL -> EmblemaDesenho(Desenhos.Moedas, CoresEmblema.RATEIO, tamanho)
        TipoLancamento.SALDO_INICIAL -> EmblemaCifrao(tamanho)
        TipoLancamento.RENDIMENTO -> EmblemaDesenho(Desenhos.Rendimento, CoresEmblema.RENDIMENTO, tamanho)
        TipoLancamento.COMPLEMENTO -> EmblemaDesenho(Desenhos.Complemento, CoresEmblema.COMPLEMENTO, tamanho)
    }
}

package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.clickable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.Tarja
import io.github.marcosfaria94.bolao.ui.tema.corDestaque

/**
 * Animações que não param (o "Completar" pulsando, o brilho do sorteio de hoje). Desligadas só
 * nas fotos das telas feitas no computador, onde cada quadro da animação deixaria tudo lento.
 */
object Animacoes {
    @Volatile
    var ligadas: Boolean = true
}

/**
 * Botão pulsando para chamar a atenção ([ligado]): ele respira e solta um anel da cor [cor] em
 * volta, como o "Completar" do volante (o "Apostar" da Home no dia de aposta).
 */
fun Modifier.pulsando(ligado: Boolean, cor: Color): Modifier = composed {
    if (!ligado || !Animacoes.ligadas) return@composed this
    val pulso = rememberInfiniteTransition(label = "pulso")
    val escala by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escala",
    )
    val anel by pulso.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "anel",
    )
    this
        .graphicsLayer {
            scaleX = escala
            scaleY = escala
        }
        .drawBehind {
            val folga = 10.dp.toPx() * anel
            drawRoundRect(
                color = cor.copy(alpha = 0.5f * (1f - anel)),
                topLeft = Offset(-folga, -folga),
                size = Size(size.width + 2 * folga, size.height + 2 * folga),
                cornerRadius = CornerRadius(size.height / 2 + folga),
            )
        }
}

/**
 * Barra de título das telas: título maior e um pouco mais abaixo que o padrão, com a seta de
 * voltar nas telas que não são abas e, à direita, as [acoes] (nas abas, o ícone do Painel de Jogos).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraTitulo(
    titulo: String,
    aoVoltar: (() -> Unit)? = null,
    subtitulo: String? = null,
    acoes: @Composable RowScope.() -> Unit = {},
    iconeVoltar: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    descricaoVoltar: String = "Voltar",
    /** O título também volta (ex.: "← Voltar", "← Aposta" na página do jogo). */
    tituloVolta: Boolean = false,
    /** A cor do título (o Painel de Jogos usa a cor de destaque). */
    corTitulo: Color = Color.Unspecified,
    /** Um desenho antes do título (o ícone do Painel de Jogos). */
    antesDoTitulo: (@Composable () -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (antesDoTitulo != null) {
                        antesDoTitulo()
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        titulo,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTitulo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (tituloVolta && aoVoltar != null) Modifier.clickable(onClick = aoVoltar) else Modifier,
                    )
                }
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            if (aoVoltar != null) {
                IconButton(onClick = aoVoltar) { Icon(iconeVoltar, contentDescription = descricaoVoltar) }
            }
        },
        actions = acoes,
        expandedHeight = 68.dp,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Tarja pequena e discreta com o título do item ("Aposta Nº1", "Rateio Nº1"), alinhada à esquerda. */
@Composable
fun TarjaTitulo(texto: String, modifier: Modifier = Modifier, fundo: Color = Tarja.fundo(), cor: Color = Tarja.texto()) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
        fontWeight = FontWeight.SemiBold,
        color = cor,
        maxLines = 1,
        modifier = modifier
            .background(fundo, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * Texto com um brilho dourado passando por cima e uma estrelinha piscando ao lado
 * ("Sorteio: Hoje" do concurso em destaque, só quando o sorteio é hoje).
 */
@Composable
fun TextoBrilhante(texto: String, estilo: TextStyle, cor: Color, modifier: Modifier = Modifier) {
    if (!Animacoes.ligadas) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(texto, style = estilo, color = cor)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Dourado, modifier = Modifier.size(18.dp))
        }
        return
    }
    val brilho = rememberInfiniteTransition(label = "brilho")
    val passo by brilho.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "passo",
    )
    val pisca by brilho.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pisca",
    )
    // Faixa clara (dourado quase branco) que atravessa o texto da esquerda para a direita.
    val largura = 260f
    val x = passo * largura
    val pincel = Brush.linearGradient(
        colors = listOf(cor, Color(0xFFFFF1C2), cor),
        start = Offset(x - 70f, 0f),
        end = Offset(x + 70f, 0f),
        tileMode = TileMode.Clamp,
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = estilo.copy(brush = pincel))
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Dourado,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    alpha = pisca
                    scaleX = 0.85f + 0.15f * pisca
                    scaleY = 0.85f + 0.15f * pisca
                },
        )
    }
}

/** O título da marca do concurso em destaque (o de maior k1). */
const val CONCURSO_EM_DESTAQUE = "CONCURSO EM DESTAQUE"

/** O título da marca quando o destaque é um concurso especial (desde a 0.28: o especial sempre é o destaque). */
const val CONCURSO_ESPECIAL = "CONCURSO ESPECIAL"

/**
 * Marca do concurso em destaque, no alto do cartão (Jogos e a página do jogo): o ícone de brilho e
 * "CONCURSO EM DESTAQUE" ou, num concurso especial ([especial]), "CONCURSO ESPECIAL". Desde a 0.28 o
 * ícone e o título brilham (uma faixa clara passa pelo texto e o ícone pisca), sem o contorno dourado no
 * cartão (na Timemania, amarela, as cores se confundiam); a estrelinha saiu, porque agora ela marca os jogos
 * favoritos. Com as animações desligadas (fotos), fica parado.
 */
@Composable
fun TituloDestaque(modifier: Modifier = Modifier, especial: Boolean = false, cor: Color = corDestaque()) {
    val texto = if (especial) CONCURSO_ESPECIAL else CONCURSO_EM_DESTAQUE
    val estilo = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    if (!Animacoes.ligadas) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Dourado, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, style = estilo, color = cor, maxLines = 1)
        }
        return
    }
    val brilho = rememberInfiniteTransition(label = "destaque")
    val passo by brilho.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "passo",
    )
    val pisca by brilho.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pisca",
    )
    // A faixa clara que atravessa o título (no tema claro, um dourado vivo; no escuro, quase branco).
    val claro = if (isSystemInDarkTheme()) Color(0xFFFFF1C2) else Color(0xFFE3A21A)
    var largura by remember { mutableIntStateOf(0) }
    val x = passo * largura
    val meia = largura * 0.16f + 1f
    val pincel = Brush.linearGradient(
        colors = listOf(cor, claro, cor),
        start = Offset(x - meia, 0f),
        end = Offset(x + meia, 0f),
        tileMode = TileMode.Clamp,
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Dourado,
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer {
                    alpha = pisca
                    scaleX = 0.82f + 0.18f * pisca
                    scaleY = 0.82f + 0.18f * pisca
                    rotationZ = (1f - pisca) * 14f
                },
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, style = estilo.copy(brush = pincel), maxLines = 1, modifier = Modifier.onSizeChanged { largura = it.width })
    }
}

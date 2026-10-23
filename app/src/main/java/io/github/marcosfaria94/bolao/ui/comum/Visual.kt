package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
 * Barra de título das telas: título maior e um pouco mais abaixo que o padrão, com a seta de
 * voltar nas telas que não são abas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraTitulo(titulo: String, aoVoltar: (() -> Unit)? = null, subtitulo: String? = null) {
    TopAppBar(
        title = {
            Column {
                Text(
                    titulo,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            if (aoVoltar != null) {
                IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
            }
        },
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
 * Um item dos Históricos (Financeiro e Apostas): o emblema redondo e, ao lado, a tarja com o
 * título, a descrição ("Quina · 55M", "3 pessoas") e a última linha (o valor ou a situação).
 */
@Composable
fun ItemHistorico(
    emblema: @Composable () -> Unit,
    tarja: String,
    descricao: String?,
    linha: String,
    aoTocar: (() -> Unit)?,
    corLinha: Color = MaterialTheme.colorScheme.onSurface,
    pesoLinha: FontWeight = FontWeight.Normal,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = aoTocar != null) { aoTocar?.invoke() }
            .padding(vertical = 10.dp),
    ) {
        emblema()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            TarjaTitulo(tarja)
            if (descricao != null) {
                Text(
                    descricao,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                linha,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = pesoLinha,
                color = corLinha,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (aoTocar != null) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

/**
 * Marca do concurso em destaque (o de maior k1), no alto do cartão: a estrelinha dourada e
 * "CONCURSO EM DESTAQUE" (Apostas, Jogos, Nova aposta e a página do jogo).
 */
@Composable
fun TituloDestaque(modifier: Modifier = Modifier, cor: Color = corDestaque()) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Dourado, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "CONCURSO EM DESTAQUE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = cor,
            maxLines = 1,
        )
    }
}

package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Deslizar o dedo para os lados fecha a tela: ela acompanha o dedo e sai para o lado em que foi
 * puxada. Puxar pouco (e devagar) a devolve ao lugar. A rolagem para cima e para baixo continua.
 */
fun Modifier.deslizarParaFechar(aoFechar: () -> Unit): Modifier = composed {
    val deslocamento = remember { Animatable(0f) }
    val escopo = rememberCoroutineScope()
    val fechar by rememberUpdatedState(aoFechar)
    var largura by remember { mutableFloatStateOf(1f) }
    var fechando by remember { mutableStateOf(false) }
    val rastreador = remember { VelocityTracker() }
    this
        .onSizeChanged { largura = it.width.toFloat().coerceAtLeast(1f) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { rastreador.resetTracking() },
                onDragEnd = {
                    val velocidade = rastreador.calculateVelocity().x
                    val x = deslocamento.value
                    escopo.launch {
                        val longe = abs(x) > largura * FRACAO_PARA_FECHAR
                        val rapido = abs(velocidade) > VELOCIDADE_PARA_FECHAR && sign(velocidade) == sign(x)
                        if ((longe || rapido) && x != 0f && !fechando) {
                            fechando = true
                            deslocamento.animateTo(sign(x) * largura, tween(140))
                            fechar()
                        } else {
                            deslocamento.animateTo(0f, spring())
                        }
                    }
                },
                onDragCancel = { escopo.launch { deslocamento.animateTo(0f, spring()) } },
            ) { mudanca, dx ->
                mudanca.consume()
                rastreador.addPosition(mudanca.uptimeMillis, mudanca.position)
                escopo.launch { deslocamento.snapTo(deslocamento.value + dx) }
            }
        }
        .graphicsLayer {
            translationX = deslocamento.value
            alpha = 1f - (abs(deslocamento.value) / largura).coerceIn(0f, 1f) * 0.6f
        }
}

/** Puxar a tela mais do que isto da largura fecha (ou um puxão rápido). */
private const val FRACAO_PARA_FECHAR = 0.3f

/** Pixels por segundo. */
private const val VELOCIDADE_PARA_FECHAR = 1200f

/**
 * O que a tela aberta põe na barra de cima (a do título): um botão à direita ("Detalhes →"), um
 * título diferente e o que o voltar da barra faz. [dono] é a entrada da navegação da tela (a barra
 * só usa a ação da tela que está em cima).
 */
class AcaoDaBarra(
    val dono: Any?,
    val titulo: String? = null,
    val botao: String? = null,
    val aoTocarBotao: () -> Unit = {},
    val aoVoltar: (() -> Unit)? = null,
)

/** Guarda a ação da barra da tela de cima (a raiz do app desenha a barra). */
class BarraDaTela {
    var acao by mutableStateOf<AcaoDaBarra?>(null)
}

val LocalBarraDaTela = staticCompositionLocalOf { BarraDaTela() }

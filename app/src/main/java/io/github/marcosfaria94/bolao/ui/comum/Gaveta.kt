package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Gaveta: uma tela guardada à direita da página (os Detalhes do jogo), que entra por cima dela
 * seguindo o dedo. [abertura] vai de 0 (fechada, fora da tela) a 1 (aberta, cobrindo a página);
 * [aberta] é onde ela vai parar (é o que fica guardado ao girar o celular).
 */
@Stable
class EstadoGaveta(abertaInicial: Boolean) {
    var abertura by mutableFloatStateOf(if (abertaInicial) 1f else 0f)
        private set
    var aberta by mutableStateOf(abertaInicial)
        private set

    /** Largura da página, em pixels (a gaveta anda essa distância). */
    internal var largura by mutableFloatStateOf(0f)

    /** O dedo para a esquerda puxa a gaveta para dentro; para a direita, empurra para fora. */
    internal val arrasto = DraggableState { dx ->
        if (largura > 0f) abertura = (abertura - dx / largura).coerceIn(0f, 1f)
    }

    suspend fun abrir() = assentar(true)

    suspend fun fechar() = assentar(false)

    /**
     * O dedo soltou: um empurrão rápido decide o lado ([velocidade] em pixels por segundo, positiva
     * para a direita); devagar, a gaveta vai para o lado mais perto.
     */
    internal suspend fun soltar(velocidade: Float, limite: Float) {
        val abrir = when {
            velocidade < -limite -> true
            velocidade > limite -> false
            else -> abertura >= 0.5f
        }
        assentar(abrir, if (largura > 0f) -velocidade / largura else 0f)
    }

    private suspend fun assentar(abrir: Boolean, velocidade: Float = 0f) {
        aberta = abrir
        val alvo = if (abrir) 1f else 0f
        // Pelo arrasto, para o dedo poder pegar a gaveta no meio do caminho (o toque cancela a animação).
        println("DIAG assentar abrir=$abrir abertura=$abertura")
        try { arrasto.drag(MutatePriority.Default) {
            if (!Animacoes.ligadas) {
                abertura = alvo
            } else {
                animate(abertura, alvo, velocidade, spring(stiffness = 700f, visibilityThreshold = 0.001f)) { v, _ ->
                    abertura = v.coerceIn(0f, 1f)
                }
            }
        } } catch (c: kotlin.coroutines.cancellation.CancellationException) { println("DIAG assentar cancelado: ${c.message}"); throw c }
        println("DIAG assentado abertura=$abertura")
    }

    companion object {
        val Saver: Saver<EstadoGaveta, Boolean> = Saver(save = { it.aberta }, restore = { EstadoGaveta(it) })
    }
}

@Composable
fun rememberEstadoGaveta(abertaInicial: Boolean = false): EstadoGaveta =
    rememberSaveable(saver = EstadoGaveta.Saver) { EstadoGaveta(abertaInicial) }

/** Velocidade do dedo que já conta como empurrão (abre ou fecha a gaveta mesmo sem chegar à metade). */
private val EMPURRAO = 400.dp

/**
 * A página com a [gaveta] guardada à direita: arrastar o dedo para a esquerda (em qualquer ponto
 * da página) puxa a gaveta por cima, seguindo o dedo; para a direita, ela volta. Para mostrar
 * isso, uma alça na borda direita da página, com as setas duplas para a esquerda (e, na gaveta
 * aberta, uma na borda esquerda, com as setas para a direita); um toque na alça também abre ou
 * fecha. [nome] é o da gaveta, para a leitura de tela ("Abrir os Detalhes").
 */
@Composable
fun PaginaComGaveta(
    estado: EstadoGaveta,
    nome: String,
    gaveta: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    pagina: @Composable () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    val limite = with(LocalDensity.current) { EMPURRAO.toPx() }
    // O mesmo arrasto na página, na alça e na gaveta (lembrado: recompor no meio não solta o dedo).
    val arrastar = remember(estado, limite) {
        Modifier.draggable(
            estado.arrasto,
            Orientation.Horizontal,
            onDragStopped = { v -> estado.soltar(v, limite) },
        )
    }
    // Composta enquanto aparece ou vai abrir (no meio do arrasto, o dedo continua com ela).
    val visivel by remember(estado) { derivedStateOf { estado.aberta || estado.abertura > 0f } }

    Box(modifier.fillMaxSize().onSizeChanged { estado.largura = it.width.toFloat() }.diag("raiz")) {
        Box(Modifier.fillMaxSize().then(arrastar).diag("pagina")) { pagina() }
        Alca(
            Icons.Filled.KeyboardDoubleArrowLeft,
            "Abrir os $nome",
            naDireita = true,
            chamarAtencao = true,
            aoTocar = { escopo.launch { estado.abrir() } },
            modifier = Modifier.align(Alignment.CenterEnd).then(arrastar),
        )
        if (visivel) {
            // A página escurece conforme a gaveta entra.
            Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black, alpha = 0.32f * estado.abertura) })
            // A gaveta, com a sombra à esquerda desenhada à mão (sem camada por cima: o toque chega
            // direto na alça e no conteúdo) e a beirada de 1dp (no tema escuro a sombra quase não aparece).
            val fundo = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(((1f - estado.abertura) * estado.largura).roundToInt(), 0) }
                    .drawBehind {
                        val sombra = 12.dp.toPx()
                        drawRect(
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)), startX = -sombra, endX = 0f),
                            topLeft = Offset(-sombra, 0f),
                            size = Size(sombra, size.height),
                        )
                    }
                    .background(fundo)
                    // O arrasto também faz a gaveta segurar os toques (não passam para a página embaixo).
                    .then(arrastar)
                    .diag("gaveta")
                    .semantics { isTraversalGroup = true },
            ) {
                gaveta()
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Alca(
                    Icons.Filled.KeyboardDoubleArrowRight,
                    "Fechar os $nome",
                    naDireita = false,
                    aoTocar = { escopo.launch { estado.fechar() } },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
    }
}

/**
 * Alça na borda (uma aba estreita, da cor dos botões, com as setas duplas para o lado de arrastar).
 * Fica de fora do gesto de voltar do celular, que também usa as bordas: arrastar a partir dela
 * mexe na gaveta. [chamarAtencao]: ao abrir a página, as setas dão dois empurrõezinhos.
 */
@Composable
private fun Alca(
    icone: ImageVector,
    descricao: String,
    naDireita: Boolean,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    chamarAtencao: Boolean = false,
) {
    val forma = if (naDireita) {
        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
    } else {
        RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
    }
    val margem = with(LocalDensity.current) { 16.dp.toPx() }
    val empurrao = remember { Animatable(0f) }
    if (chamarAtencao) {
        LaunchedEffect(Unit) {
            if (!Animacoes.ligadas) return@LaunchedEffect
            delay(700)
            repeat(2) {
                empurrao.animateTo(1f, tween(220))
                empurrao.animateTo(0f, tween(320))
            }
        }
    }
    Box(
        modifier
            // Um pouco acima e abaixo da aba também fica de fora do gesto de voltar.
            .systemGestureExclusion { c -> Rect(0f, -margem, c.size.width.toFloat(), c.size.height + margem) }
            .size(width = 14.dp, height = 72.dp)
            // Só o fundo arredondado, sem recortar (sem camada: o toque chega na alça assim que ela aparece).
            .background(MaterialTheme.colorScheme.primary, forma)
            .diag("alca $descricao")
            .clickable(role = Role.Button, onClick = { println("DIAG clique $descricao"); aoTocar() }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icone,
            contentDescription = descricao,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .requiredSize(16.dp)
                .graphicsLayer { translationX = (if (naDireita) -3f else 3f) * density * empurrao.value },
        )
    }
}


private fun Modifier.diag(nome: String): Modifier = this
    .pointerInput(nome) {
        awaitPointerEventScope {
            while (true) {
                val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                println("DIAG toque $nome ${e.type} " + e.changes.joinToString { "${it.position} consumido=${it.isConsumed}" })
            }
        }
    }
    .then(Modifier.onGloballyPositioned { println("DIAG pos $nome ${it.positionInRoot()} ${it.size}") })

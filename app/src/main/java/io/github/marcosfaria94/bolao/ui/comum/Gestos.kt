package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
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

/** Puxar a tela mais do que isto da largura fecha (ou um puxão rápido). Até a 0.15, 30%. */
private const val FRACAO_PARA_FECHAR = 0.2f

/** Pixels por segundo. Até a 0.15, 1200. */
private const val VELOCIDADE_PARA_FECHAR = 900f

/**
 * Nos pagers (as abas e a página do jogo com os Detalhes), arrastar devagar mais do que isto da
 * largura já passa de página ao soltar (o padrão do Compose é metade).
 */
const val LIMIAR_DA_PAGINA = 0.3f

/** O jeito de soltar dos pagers do app: passa de página com menos esforço ([LIMIAR_DA_PAGINA]). */
@Composable
fun soltarDasPaginas(estado: PagerState): TargetedFlingBehavior =
    PagerDefaults.flingBehavior(estado, snapPositionalThreshold = LIMIAR_DA_PAGINA)

/**
 * Puxar além da borda dispara uma ação: a borda é o começo ou o fim de uma lista ([vertical]) ou a
 * primeira ou a última página de um pager. A tela acompanha o dedo (com resistência) e, solta depois
 * do [limite] (ou num puxão rápido), a ação acontece; antes disso, ela volta ao lugar.
 * [noInicio]: puxar para baixo (ou para a direita) antes do começo; [noFim]: para cima (ou para a
 * esquerda) depois do fim. null = nada acontece naquela borda.
 *
 * Usar com Modifier.nestedScroll(puxao) em volta da lista (ou do pager) e desenhar o conteúdo
 * deslocado de [deslocamento] (ex.: graphicsLayer { translationY = puxao.deslocamento }).
 */
class PuxaoAlemDaBorda(
    val vertical: Boolean,
    val limite: Float,
    /** Num puxão rápido, quanto do [limite] a tela já precisa ter andado. */
    private val fracaoDoPuxaoRapido: Float = 0.25f,
    /** Pixels por segundo de um puxão rápido. */
    private val velocidadeDoPuxaoRapido: Float = VELOCIDADE_PARA_FECHAR,
) : NestedScrollConnection {
    var noInicio: (() -> Unit)? = null
    var noFim: (() -> Unit)? = null

    /** Quanto a tela andou com o dedo, em pixels (positivo = para baixo ou para a direita). */
    var deslocamento by mutableFloatStateOf(0f)
        private set

    /** De 0 (parada) a 1 (já dispara ao soltar). */
    val progresso: Float get() = (abs(deslocamento) / limite).coerceIn(0f, 1f)

    private fun eixo(o: Offset) = if (vertical) o.y else o.x
    private fun eixo(v: Velocity) = if (vertical) v.y else v.x
    private fun offset(d: Float) = if (vertical) Offset(0f, d) else Offset(d, 0f)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val antes = deslocamento
        if (source != NestedScrollSource.UserInput || antes == 0f) return Offset.Zero
        val d = eixo(available)
        // O dedo voltando (contra o puxão): primeiro desfaz o puxão, só depois a lista rola.
        if (!(antes > 0f && d < 0f) && !(antes < 0f && d > 0f)) return Offset.Zero
        val novo = if (antes > 0f) (antes + d * RESISTENCIA).coerceAtLeast(0f) else (antes + d * RESISTENCIA).coerceAtMost(0f)
        deslocamento = novo
        return offset((novo - antes) / RESISTENCIA)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val d = eixo(available)
        val puxa = (d > 0f && deslocamento >= 0f && noInicio != null) || (d < 0f && deslocamento <= 0f && noFim != null)
        if (!puxa) return Offset.Zero
        deslocamento += d * RESISTENCIA
        return offset(d)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val x = deslocamento
        if (x == 0f) return Velocity.Zero
        val v = eixo(available)
        val acao = if (x > 0f) noInicio else noFim
        val rapido = abs(x) > limite * fracaoDoPuxaoRapido && abs(v) > velocidadeDoPuxaoRapido && sign(v) == sign(x)
        if (acao != null && (abs(x) >= limite || rapido)) {
            deslocamento = 0f
            acao()
        } else {
            animate(x, 0f, animationSpec = spring()) { valor, _ -> deslocamento = valor }
        }
        // A lista (ou o pager) não sai rolando com o resto do puxão.
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // Segurança: um gesto cancelado no meio não deixa a tela fora do lugar.
        if (deslocamento != 0f) animate(deslocamento, 0f, animationSpec = spring()) { valor, _ -> deslocamento = valor }
        return Velocity.Zero
    }

    private companion object {
        /** A tela anda metade do que o dedo anda. */
        const val RESISTENCIA = 0.5f
    }
}

/** Quanto a tela precisa andar (já com a resistência) para o puxão para os lados disparar a ação. Até a 0.15, 72dp. */
val LIMITE_DO_PUXAO = 56.dp

/**
 * Para cima e para baixo (abrir os Meus jogos, voltar puxando para baixo): a 0.15
 * deixou mais difícil (96dp, puxão rápido com 60% do caminho a 2600 px/s) e ficou pesado; a 0.16
 * alivia, mas o puxão rápido ainda pede boa parte do caminho, para não abrir sem querer ao rolar a lista.
 */
val LIMITE_DO_PUXAO_VERTICAL = 72.dp
private const val FRACAO_DO_PUXAO_RAPIDO_VERTICAL = 0.4f
private const val VELOCIDADE_DO_PUXAO_RAPIDO_VERTICAL = 1800f

/** [PuxaoAlemDaBorda] guardado na tela, com as ações de agora. */
@Composable
fun rememberPuxaoAlemDaBorda(vertical: Boolean, noInicio: (() -> Unit)? = null, noFim: (() -> Unit)? = null): PuxaoAlemDaBorda {
    val densidade = LocalDensity.current
    val puxao = remember(vertical, densidade) {
        if (vertical) {
            PuxaoAlemDaBorda(
                vertical, with(densidade) { LIMITE_DO_PUXAO_VERTICAL.toPx() },
                FRACAO_DO_PUXAO_RAPIDO_VERTICAL, VELOCIDADE_DO_PUXAO_RAPIDO_VERTICAL,
            )
        } else {
            PuxaoAlemDaBorda(vertical, with(densidade) { LIMITE_DO_PUXAO.toPx() })
        }
    }
    SideEffect {
        puxao.noInicio = noInicio
        puxao.noFim = noFim
    }
    return puxao
}

/**
 * O aviso do puxão: aparece aos poucos enquanto se puxa ([puxando]) e diz [pronto] (o nome da tela
 * que vai abrir, ou para onde volta), em destaque, quando soltar já dispara a ação.
 * [noFim]: o aviso do puxão além do fim (empurrando para cima); senão, o do começo (puxando para baixo).
 */
@Composable
fun IndicadorDoPuxao(
    puxao: PuxaoAlemDaBorda,
    puxando: String,
    pronto: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
    noFim: Boolean = false,
) {
    val p = puxao.progresso
    if (p <= 0f || (noFim && puxao.deslocamento > 0f) || (!noFim && puxao.deslocamento < 0f)) return
    val cheio = p >= 1f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer {
                alpha = p
                val escala = 0.85f + 0.15f * p
                scaleX = escala
                scaleY = escala
            }
            .background(
                if (cheio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        val cor = if (cheio) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (cheio) pronto else puxando, style = MaterialTheme.typography.labelLarge, color = cor, maxLines = 1)
    }
}

/**
 * Puxar a tela para baixo (do alto da lista) volta para a tela de antes ([aoVoltar]). O aviso diz
 * "Voltar" enquanto se puxa e, quando soltar já volta, o nome da tela de antes ([voltaPara]).
 */
@Composable
fun PuxarParaVoltar(voltaPara: String, aoVoltar: () -> Unit, modifier: Modifier = Modifier, conteudo: @Composable () -> Unit) {
    val puxao = rememberPuxaoAlemDaBorda(vertical = true, noInicio = aoVoltar)
    Box(modifier.fillMaxSize().nestedScroll(puxao)) {
        Box(Modifier.fillMaxSize().acompanharPuxao(puxao)) { conteudo() }
        IndicadorDoPuxao(
            puxao, puxando = "Voltar", pronto = voltaPara, icone = Icons.Outlined.KeyboardArrowDown,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
        )
    }
}

/** A tela acompanhando o puxão: anda junto com o dedo e, para os lados, vai sumindo. */
fun Modifier.acompanharPuxao(puxao: PuxaoAlemDaBorda): Modifier = graphicsLayer {
    val x = puxao.deslocamento
    if (puxao.vertical) {
        translationY = x
    } else {
        translationX = x
        alpha = 1f - puxao.progresso * 0.35f
    }
}

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

/** Tocar na aba em que já se está: a [lista] volta ao começo. [toques] sobe a cada toque (a raiz conta). */
@Composable
fun VoltarAoTopo(lista: LazyListState, toques: Int) {
    var visto by remember { mutableIntStateOf(toques) }
    LaunchedEffect(toques) {
        if (toques != visto) {
            visto = toques
            lista.animateScrollToItem(0)
        }
    }
}

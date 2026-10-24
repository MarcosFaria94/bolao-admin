package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Troca feita pela tela aberta na barra de cima: o [titulo] e o que a seta de voltar faz. */
class AjusteDaBarra(val titulo: String, val voltar: () -> Unit)

/**
 * A barra de cima do app, que a tela aberta pode ajustar: com os Detalhes puxados por cima da
 * página do jogo, ela diz "Detalhes" e a seta de voltar fecha só os Detalhes.
 */
@Stable
class BarraDaTela {
    var ajuste by mutableStateOf<AjusteDaBarra?>(null)
        internal set
}

val LocalBarraDaTela = staticCompositionLocalOf { BarraDaTela() }

/** Enquanto [ativo] (e a tela estiver aberta), a barra de cima mostra [titulo] e a seta de voltar chama [voltar]. */
@Composable
fun AjustarBarra(ativo: Boolean, titulo: String, voltar: () -> Unit) {
    val barra = LocalBarraDaTela.current
    val voltarAgora by rememberUpdatedState(voltar)
    DisposableEffect(barra, ativo, titulo) {
        val meu = if (ativo) AjusteDaBarra(titulo) { voltarAgora() } else null
        if (meu != null) barra.ajuste = meu
        // Só desfaz o próprio ajuste (a tela seguinte pode já ter posto o dela).
        onDispose { if (meu != null && barra.ajuste === meu) barra.ajuste = null }
    }
}

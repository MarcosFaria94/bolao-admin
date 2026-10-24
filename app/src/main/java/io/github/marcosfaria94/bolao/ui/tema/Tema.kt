package io.github.marcosfaria94.bolao.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Cores do app: as do ícone (azul-noite e o dourado do trevo) sobre fundos neutros,
 * no estilo limpo dos apps de banco. Verde só para dinheiro que entra.
 */

/** Azul-noite do fundo do ícone. */
val AzulNoite = Color(0xFF0B1626)

/** Azul-noite um pouco mais claro (barra no tema claro, cartão em destaque). */
val AzulMarinho = Color(0xFF13294B)

/** Dourado do trevo. */
val Dourado = Color(0xFFF2B630)

private val Claro = lightColorScheme(
    primary = AzulMarinho,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5F5),
    onPrimaryContainer = Color(0xFF0B1A33),
    secondary = Color(0xFFA87400),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBEFCB),
    onSecondaryContainer = Color(0xFF3D2A00),
    tertiary = Color(0xFF1E6FD9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEBFF),
    onTertiaryContainer = Color(0xFF0A2A55),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF8C1D18),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF131820),
    surface = Color(0xFFF4F6F9),
    onSurface = Color(0xFF131820),
    surfaceVariant = Color(0xFFE3E7ED),
    onSurfaceVariant = Color(0xFF5B6472),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF1F5),
    surfaceContainerHigh = Color(0xFFE8ECF1),
    surfaceContainerHighest = Color(0xFFE1E6EC),
    outline = Color(0xFF8A94A3),
    outlineVariant = Color(0xFFDDE2E8),
)

private val Escuro = darkColorScheme(
    primary = Dourado,
    onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFF3A2F12),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFF9DB4DA),
    onSecondary = Color(0xFF0B1E3D),
    secondaryContainer = Color(0xFF1C2C47),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFF7CC4FF),
    onTertiary = Color(0xFF002B4D),
    tertiaryContainer = Color(0xFF0F3355),
    onTertiaryContainer = Color(0xFFCFE6FF),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF4A0E0B),
    errorContainer = Color(0xFF3B1E21),
    onErrorContainer = Color(0xFFFFB4AB),
    background = Color(0xFF0E1218),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF0E1218),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF26303C),
    onSurfaceVariant = Color(0xFF9AA5B5),
    surfaceContainerLowest = Color(0xFF0A0D12),
    surfaceContainerLow = Color(0xFF141A22),
    surfaceContainer = Color(0xFF18202A),
    surfaceContainerHigh = Color(0xFF1E2732),
    surfaceContainerHighest = Color(0xFF26303C),
    outline = Color(0xFF6B778A),
    outlineVariant = Color(0xFF26303C),
)

private val base = Typography()

private val Tipografia = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

/** Estilo dos blocos monoespaçados (igual ao do WhatsApp). */
val EstiloMensagem = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, lineHeight = 17.sp)

@Composable
fun BolaoTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Escuro else Claro,
        typography = Tipografia,
        content = conteudo,
    )
}

/** Cor de cada modalidade, para chips e volantes. */
fun corModalidade(argb: Long): Color = Color(argb.toInt())

/** Dourado da aposta premiada. */
@Composable
fun corPremiada(): Color = if (isSystemInDarkTheme()) Color(0xFFFFC83D) else Color(0xFFA87400)

/** Verde do dinheiro que entra (aporte, prêmio, rendimento). */
@Composable
fun corEntrada(): Color = if (isSystemInDarkTheme()) Color(0xFF3DD68C) else Color(0xFF15803D)

/** Azul-claro de "Aguardando sorteio". */
@Composable
fun corAguardando(): Color = if (isSystemInDarkTheme()) Color(0xFF7CC4FF) else Color(0xFF1E6FD9)

/** Âmbar de "Aguardando apuração". */
@Composable
fun corApuracao(): Color = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFB26A00)

/** Amarelo-ouro do valor das apostas nas movimentações. */
@Composable
fun corAposta(): Color = if (isSystemInDarkTheme()) Dourado else Color(0xFF9A6A00)

/** Azul do valor dos rateios nas movimentações. */
@Composable
fun corRateio(): Color = if (isSystemInDarkTheme()) Color(0xFF6CB6FF) else Color(0xFF1E63C6)

/** Dourado de destaque sobre o fundo normal das telas ("CONCURSO EM DESTAQUE" nos Jogos e na página do jogo). */
@Composable
fun corDestaque(): Color = if (isSystemInDarkTheme()) Dourado else Color(0xFF8A5A00)

/** Tarja do título dos itens dos Históricos ("Aposta Nº1"): tom discreto, diferente da cor da modalidade. */
object Tarja {
    @Composable
    fun fundo(): Color = if (isSystemInDarkTheme()) Color(0xFF222C3A) else Color(0xFFE6EBF2)

    @Composable
    fun texto(): Color = if (isSystemInDarkTheme()) Color(0xFFE4E9F1) else Color(0xFF13294B)
}

/** Ouro da tarja da aposta premiada no Histórico da Home ("Aposta Nº1 · Premiada"): degradê, com a letra escura. */
object Ouro {
    @Composable
    fun degrade(): List<Color> =
        if (isSystemInDarkTheme()) listOf(Color(0xFFFFD873), Color(0xFFE9A92A)) else listOf(Color(0xFFF9DE8A), Color(0xFFEFB23A))

    val textoTarja: Color = Color(0xFF3D2A00)
}

/** Barra de baixo: azul-noite do ícone, aba escolhida em dourado. */
object Barra {
    @Composable
    fun fundo(): Color = if (isSystemInDarkTheme()) AzulNoite else AzulMarinho

    val escolhida: Color = Dourado

    @Composable
    fun outras(): Color = if (isSystemInDarkTheme()) Color(0xFF7D8AA3) else Color(0xFFA9B7CF)

    /** Contador de pendências do sino (estilo Facebook). */
    val contador: Color = Color(0xFFE53935)
}

/** Sino das notificações, no canto da Home: todo amarelo. */
val AmareloSino = Color(0xFFFFC21A)

/** Avisos de alerta (falta anexar, extrato só no último dia...): vermelho suave, em negrito. */
object CoresAlerta {
    @Composable
    fun fundo(): Color = if (isSystemInDarkTheme()) Color(0xFF3B1E21) else Color(0xFFFDECEC)

    @Composable
    fun texto(): Color = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else Color(0xFFB3261E)
}

/** Faixa do modo de teste: vermelha, com letra branca. */
val VermelhoTeste = Color(0xFFC62828)

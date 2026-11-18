package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.LastPage
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.domain.Trilha
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A navegação dos concursos nos Detalhes. Desde a 0.37 é um botão flutuante, discreto, no rodapé e no centro, só com o
 * "^" (mostrar); desde a 0.38 (pedido do usuário), aberto, ele vira o seletor pequeno: Retroceder e Avançar (só as setas),
 * o campo "Nº Concurso" e o Último (só o ícone, à direita do Avançar). Sem o "Ir" e sem o "v": tocar fora do seletor
 * fecha (indo ao concurso digitado, se houver); o Concluir do teclado e os caracteres que não são algarismos ("," "."
 * espaço "-", as teclas que sobram no teclado de números) também. Segurar Retroceder ou Avançar passa os concursos num
 * ritmo que cresce devagar até [CONCURSOS_POR_SEGUNDO] por segundo. Tocar no campo limpa o número para digitar outro.
 * Os concursos seguem a [trilha] (no especial, só os especiais dele; na modalidade, só os regulares).
 *
 * [numero]: o concurso na tela; [aoIr]: vai a um concurso da trilha. Fica num Box que ocupa a página: aberto, uma camada
 * transparente por baixo do seletor pega o toque de fora.
 */
@Composable
fun BoxScope.NavegacaoFlutuante(
    numero: Int,
    trilha: Trilha,
    aberta: Boolean,
    aoAbrir: (Boolean) -> Unit,
    aoIr: (Int) -> Unit,
) {
    val foco = LocalFocusManager.current
    var texto by remember(numero) { mutableStateOf(numero.toString()) }
    var digitando by remember { mutableStateOf(false) }
    val numeroAgora by rememberUpdatedState(numero)
    val irAgora by rememberUpdatedState(aoIr)

    /** Fecha o seletor; com um número digitado (diferente do da tela), vai ao concurso da trilha mais perto dele. */
    fun fechar(digitado: String = texto) {
        val n = digitado.toIntOrNull()?.takeIf { it > 0 }
        val destino = n?.let { trilha.maisPerto(it) }
        digitando = false
        foco.clearFocus()
        texto = numeroAgora.toString()
        aoAbrir(false)
        if (destino != null && destino != numeroAgora) irAgora(destino)
    }

    if (aberta) {
        // O toque fora do seletor (na página) fecha.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            if (e.changes.any { it.pressed }) fechar()
                        }
                    }
                },
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .imePadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
    ) {
        if (aberta) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    val anterior = trilha.anterior(numero)
                    val proximo = trilha.proximo(numero)
                    BotaoQueRepete(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, RETROCEDER, anterior != null) {
                        trilha.anterior(numeroAgora)?.let(irAgora)
                    }
                    CampoDoConcurso(
                        texto = texto,
                        aoMudar = { novo ->
                            // As teclas que sobram no teclado de números ("," "." espaço "-") fecham o seletor.
                            if (novo.any { it in TECLAS_QUE_FECHAM }) {
                                fechar(novo.takeWhile { it.isDigit() })
                            } else {
                                texto = novo.filter(Char::isDigit).take(6)
                            }
                        },
                        aoConcluir = { fechar() },
                        aoFocar = { focado ->
                            if (focado && !digitando) {
                                // Ao tocar no campo: limpa o número para digitar outro.
                                digitando = true
                                texto = ""
                            }
                        },
                    )
                    BotaoQueRepete(Icons.AutoMirrored.Outlined.KeyboardArrowRight, AVANCAR, proximo != null) {
                        trilha.proximo(numeroAgora)?.let(irAgora)
                    }
                    BotaoQueRepete(Icons.AutoMirrored.Outlined.LastPage, ULTIMO_CONCURSO, numero != trilha.ultimo, repete = false) {
                        foco.clearFocus()
                        irAgora(trilha.ultimo)
                    }
                }
            }
        } else {
            // O botão discreto: "^" mostra o seletor.
            Surface(
                onClick = { aoAbrir(true) },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 2.dp,
            ) {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    contentDescription = MOSTRAR_A_NAVEGACAO,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp).size(26.dp),
                )
            }
        }
    }
}

/** O campo "Nº Concurso" do seletor (desde a 0.38 pequeno, sem a borda alta do OutlinedTextField). */
@Composable
private fun CampoDoConcurso(texto: String, aoMudar: (String) -> Unit, aoConcluir: () -> Unit, aoFocar: (Boolean) -> Unit) {
    val cor = MaterialTheme.colorScheme.onSurface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp),
    ) {
        Text(ROTULO_DO_CONCURSO, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        BasicTextField(
            value = texto,
            onValueChange = aoMudar,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = cor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { aoConcluir() }, onGo = { aoConcluir() }),
            modifier = Modifier
                .width(88.dp)
                .onFocusChanged { aoFocar(it.isFocused) }
                .semantics { contentDescription = CAMPO_DO_CONCURSO },
        )
    }
}

/**
 * Um botão redondo e pequeno do seletor, só com o ícone ([nome] para quem não vê a tela). Tocar dá um passo; com
 * [repete], segurar repete o passo cada vez mais rápido, até [CONCURSOS_POR_SEGUNDO] por segundo.
 */
@Composable
private fun BotaoQueRepete(icone: ImageVector, nome: String, ligado: Boolean, repete: Boolean = true, passo: () -> Unit) {
    val passoAgora by rememberUpdatedState(passo)
    val ligadoAgora by rememberUpdatedState(ligado)
    val fundo = if (ligado) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val frente = if (ligado) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Surface(
        shape = CircleShape,
        color = fundo,
        contentColor = frente,
        modifier = Modifier
            .size(TAMANHO_DO_BOTAO)
            .semantics {
                contentDescription = nome
                role = Role.Button
                if (!ligado) disabled()
                onClick { if (ligadoAgora) passoAgora(); ligadoAgora }
            }
            .pointerInput(repete) {
                coroutineScope {
                    val escopo = this
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!ligadoAgora) return@awaitEachGesture
                        passoAgora()
                        val repeticao = if (repete) {
                            escopo.launch {
                                delay(ESPERA_PARA_REPETIR)
                                var intervalo = PRIMEIRO_INTERVALO
                                while (ligadoAgora) {
                                    passoAgora()
                                    delay(intervalo)
                                    intervalo = proximoIntervalo(intervalo)
                                }
                            }
                        } else {
                            null
                        }
                        waitForUpOrCancellation()
                        repeticao?.cancel()
                    }
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

/** O ritmo de segurar o botão (desde a 0.38): começa devagar e acelera até 4 concursos por segundo. */
const val CONCURSOS_POR_SEGUNDO = 4
private const val ESPERA_PARA_REPETIR = 450L
private const val PRIMEIRO_INTERVALO = 650L
private const val MENOR_INTERVALO = 1000L / CONCURSOS_POR_SEGUNDO

/** O próximo intervalo da repetição: 12% mais curto, até o limite de [CONCURSOS_POR_SEGUNDO] por segundo. */
fun proximoIntervalo(atual: Long): Long = maxOf(MENOR_INTERVALO, (atual * 0.88).toLong())

/** As teclas que fecham o seletor ao digitar no campo (desde a 0.38). */
val TECLAS_QUE_FECHAM = setOf(',', '.', ' ', '-')

private val TAMANHO_DO_BOTAO = 44.dp

/** Os nomes do seletor (desde a 0.38 os botões não têm texto: são para quem não vê a tela e os testes). */
const val RETROCEDER = "Retroceder"
const val AVANCAR = "Avançar"
const val ULTIMO_CONCURSO = "Último"
const val MOSTRAR_A_NAVEGACAO = "Mostrar a navegação dos concursos"

/** O nome do campo do concurso nos Detalhes (desde a 0.37, "Nº Concurso"; de 0.33 a 0.36, "Concurso"). */
const val ROTULO_DO_CONCURSO = "Nº Concurso"

/** Para quem não vê a tela: o campo do número (desde a 0.28). */
const val CAMPO_DO_CONCURSO = "Número do concurso"

/** Quanto a página dos Detalhes deixa de espaço no fim, para o botão flutuante (fechado e aberto) não cobrir nada. */
val ESPACO_DA_NAVEGACAO_FECHADA = 64.dp
val ESPACO_DA_NAVEGACAO_ABERTA = 96.dp

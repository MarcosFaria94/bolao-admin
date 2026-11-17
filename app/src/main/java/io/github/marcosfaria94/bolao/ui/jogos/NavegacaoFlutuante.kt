package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.LastPage
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.domain.Trilha
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe

/**
 * A navegação dos concursos nos Detalhes (desde a 0.37, pedido do usuário; de 0.28 a 0.36 eram os botões e o campo no
 * alto da página): um botão flutuante, discreto, no rodapé e no centro, só com o "^" (mostrar). Tocar nele abre a caixa
 * com Retroceder, Avançar, "Nº Concurso", Ir e Último, e o botão vira "v" (ocultar). Tocar em "Nº Concurso" abre o
 * teclado numérico e limpa o número que estava; tocar fora da caixa (ou fechar o teclado com o Ir) volta o número e
 * fecha a digitação. Os concursos seguem a [trilha] (no especial, só os especiais dele; na modalidade, só os regulares).
 *
 * [numero]: o concurso na tela; [aoIr]: vai a um concurso da trilha. Fica num Box que ocupa a página: com a digitação
 * aberta, uma camada transparente por baixo da caixa pega o toque de fora.
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
    val digitado = texto.toIntOrNull()?.takeIf { it > 0 }
    val destino = digitado?.let { trilha.maisPerto(it) }
    fun parar() {
        foco.clearFocus()
    }
    fun ir() {
        val d = destino
        digitando = false
        if (d != null && d != numero) aoIr(d) else texto = numero.toString()
        parar()
    }
    if (digitando) {
        // O toque fora da caixa (na página) fecha a digitação e volta o número que estava.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val e = awaitPointerEvent()
                            if (e.changes.any { it.pressed }) parar()
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
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val anterior = trilha.anterior(numero)
                        val proximo = trilha.proximo(numero)
                        BotaoDaNavegacao(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, RETROCEDER, anterior != null) {
                            parar()
                            anterior?.let(aoIr)
                        }
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { novo -> texto = novo.filter(Char::isDigit).take(6) },
                            singleLine = true,
                            label = { Text(ROTULO_DO_CONCURSO, maxLines = 1) },
                            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { ir() }, onDone = { ir() }),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { f ->
                                    if (f.isFocused && !digitando) {
                                        // Ao tocar no campo: limpa o número para digitar outro.
                                        digitando = true
                                        texto = ""
                                    } else if (!f.isFocused && digitando) {
                                        // Ao sair dele sem ir: o número que estava.
                                        digitando = false
                                        texto = numero.toString()
                                    }
                                }
                                .semantics { contentDescription = CAMPO_DO_CONCURSO },
                        )
                        BotaoDaNavegacao(Icons.AutoMirrored.Outlined.KeyboardArrowRight, AVANCAR, proximo != null) {
                            parar()
                            proximo?.let(aoIr)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { ir() },
                            enabled = destino != null && destino != numero,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        ) {
                            Text(IR_AO_CONCURSO, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                parar()
                                aoIr(trilha.ultimo)
                            },
                            enabled = numero != trilha.ultimo,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.LastPage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ULTIMO_CONCURSO, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        // O botão discreto: "^" mostra a caixa; "v" oculta.
        Surface(
            onClick = {
                parar()
                aoAbrir(!aberta)
            },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation = 2.dp,
        ) {
            Icon(
                if (aberta) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                contentDescription = if (aberta) OCULTAR_A_NAVEGACAO else MOSTRAR_A_NAVEGACAO,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp).size(26.dp),
            )
        }
    }
}

/** Retroceder e Avançar: a seta e o nome embaixo, num botão só. */
@Composable
private fun BotaoDaNavegacao(icone: ImageVector, nome: String, ligado: Boolean, aoTocar: () -> Unit) {
    FilledTonalButton(
        onClick = aoTocar,
        enabled = ligado,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        modifier = Modifier.width(84.dp).heightIn(min = 56.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(24.dp))
            TextoQueCabe(nome, MaterialTheme.typography.labelMedium, menor = 8.sp)
        }
    }
}

/** Os nomes da caixa da navegação (desde a 0.37). */
const val RETROCEDER = "Retroceder"
const val AVANCAR = "Avançar"
const val ULTIMO_CONCURSO = "Último"
const val MOSTRAR_A_NAVEGACAO = "Mostrar a navegação dos concursos"
const val OCULTAR_A_NAVEGACAO = "Ocultar a navegação dos concursos"

/** O nome do campo do concurso nos Detalhes (desde a 0.37, "Nº Concurso"; de 0.33 a 0.36, "Concurso"). */
const val ROTULO_DO_CONCURSO = "Nº Concurso"

/** O botão que vai ao concurso digitado (desde a 0.33). */
const val IR_AO_CONCURSO = "Ir"

/** Para quem não vê a tela: o campo do número (desde a 0.28). */
const val CAMPO_DO_CONCURSO = "Número do concurso"

/** Quanto a página dos Detalhes deixa de espaço no fim, para o botão flutuante (fechado e aberto) não cobrir nada. */
val ESPACO_DA_NAVEGACAO_FECHADA = 64.dp
val ESPACO_DA_NAVEGACAO_ABERTA = 220.dp

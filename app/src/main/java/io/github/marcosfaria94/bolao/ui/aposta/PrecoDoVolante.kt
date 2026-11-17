package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro

/**
 * O preço do volante (desde a 0.37, pedido do usuário): tocar no preço, ao lado do "Aposta Nº1", abre a janela do preço
 * da aposta simples da modalidade, o mesmo de Ajustes > Parâmetros (é um atalho dele). Salvar muda o preço da modalidade
 * e o do volante: as apostas com mais números são múltiplos da aposta simples ([precoComOSimples]). A Federal não tem
 * (o valor dela é digitado no volante).
 */
fun mudaOPrecoNoVolante(m: Modalidade): Boolean = m.formato != Formato.FEDERAL

/** O preço do volante [r] com a aposta simples a [novo] centavos: vezes as apostas simples que cabem no palpite. */
fun precoComOSimples(r: Aposta, novo: Long): Long =
    novo * Volante.apostasSimples(r.modalidade, r.numeros.size, r.trevos.size)

/** O título da janela do preço (e o nome do campo). */
const val TITULO_DO_PRECO = "Preço da aposta simples"

/** O que o toque no preço do volante faz (para quem não vê a tela). */
const val MUDAR_O_PRECO = "Mudar o preço da aposta simples"

/** A explicação da janela: vale para as próximas apostas e, com mais números, os múltiplos. */
fun explicacaoDoPreco(m: Modalidade): String =
    "Vale para esta e para as próximas apostas da ${m.nome} (é o mesmo preço de Ajustes > Parâmetros)." +
        if (Volante.aceitaMais(m)) " As apostas com mais números custam múltiplos dele." else ""

/** O aviso depois de salvar. */
fun avisoDoPreco(m: Modalidade, centavos: Long): String = "Aposta simples da ${m.nome}: ${Dinheiro.rs(centavos)}."

/**
 * A janela do preço da aposta simples (desde a 0.37). Feita sobre um Dialog que ocupa a tela (com a janela no meio, como
 * as outras), e não sobre o AlertDialog: com um campo de texto, o AlertDialog não deixa os testes de tela terminarem.
 * Tocar fora dela fecha sem salvar.
 */
@Composable
fun JanelaDoPreco(m: Modalidade, inicial: Long, aoSalvar: (Long) -> Unit, aoFechar: () -> Unit) {
    var valor by remember { mutableLongStateOf(inicial) }
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    val salvar = { if (valor > 0) aoSalvar(valor) }
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = aoFechar)
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    // O toque dentro da janela não fecha.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(TITULO_DO_PRECO, style = MaterialTheme.typography.headlineSmall)
                    Text(m.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(explicacaoDoPreco(m), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CampoDinheiro(
                        centavos = valor,
                        aoMudar = { valor = it },
                        rotulo = TITULO_DO_PRECO,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.fillMaxWidth().focusRequester(foco),
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = aoFechar) { Text("Cancelar") }
                        TextButton(onClick = salvar, enabled = valor > 0) { Text("Salvar") }
                    }
                }
            }
        }
    }
}

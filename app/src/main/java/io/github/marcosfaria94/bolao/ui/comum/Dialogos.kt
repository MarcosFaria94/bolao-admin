package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Diálogo para editar um valor em dinheiro. */
@Composable
fun DialogoDinheiro(
    titulo: String,
    inicial: Long,
    rotulo: String,
    aoConfirmar: (Long) -> Unit,
    aoFechar: () -> Unit,
    explicacao: String? = null,
) {
    var valor by remember { mutableLongStateOf(inicial) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = {
            Column {
                explicacao?.let { Text(it) }
                CampoDinheiro(
                    centavos = valor,
                    aoMudar = { valor = it },
                    rotulo = rotulo,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { aoConfirmar(valor) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

/** Confirmação simples (sim/não). */
@Composable
fun DialogoConfirmar(
    titulo: String,
    texto: String,
    confirmar: String,
    aoConfirmar: () -> Unit,
    aoFechar: () -> Unit,
    cancelar: String = "Cancelar",
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = aoConfirmar) { Text(confirmar) } },
        dismissButton = { TextButton(onClick = aoFechar) { Text(cancelar) } },
    )
}

/**
 * Pergunta minimalista com "Sim" e "Não" bem grandes (ex.: "Deseja registrar aposta?").
 * Tocar fora ou voltar vale como "Não".
 */
@Composable
fun DialogoSimNao(titulo: String, texto: String, aoSim: () -> Unit, aoNao: () -> Unit) {
    Dialog(onDismissRequest = aoNao) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    OutlinedButton(onClick = aoNao, modifier = Modifier.weight(1f).height(60.dp)) {
                        Text("Não", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = aoSim, modifier = Modifier.weight(1f).height(60.dp)) {
                        Text("Sim", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Arquivo salvo em Downloads/Bolao: abrir ou enviar. */
@Composable
fun DialogoArquivoPronto(titulo: String, texto: String, uri: android.net.Uri, aoFechar: () -> Unit, mime: String = "application/pdf") {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = {
                aoFechar()
                Compartilhar.enviarArquivo(ctx, uri, mime, "Enviar")
            }) { Text("Enviar") }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = aoFechar) { Text("Fechar") }
                TextButton(onClick = {
                    aoFechar()
                    Compartilhar.abrir(ctx, uri, mime)
                }) { Text("Abrir") }
            }
        },
    )
}

/**
 * Uma janela com campos (desde a 0.38): feita sobre um Dialog que ocupa a tela, com a janela no meio, como a do preço do
 * volante ([io.github.marcosfaria94.bolao.ui.aposta.JanelaDoPreco]); o AlertDialog com campo de texto não deixa os testes
 * de tela terminarem. Tocar fora fecha. [botoes]: os botões do pé (à direita).
 */
@Composable
fun JanelaComCampos(
    titulo: String,
    aoFechar: () -> Unit,
    botoes: @Composable RowScope.() -> Unit,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
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
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(titulo, style = MaterialTheme.typography.headlineSmall)
                    conteudo()
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth(), content = botoes)
                }
            }
        }
    }
}

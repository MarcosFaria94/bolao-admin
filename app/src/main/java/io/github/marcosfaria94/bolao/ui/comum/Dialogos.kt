package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

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
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = aoConfirmar) { Text(confirmar) } },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

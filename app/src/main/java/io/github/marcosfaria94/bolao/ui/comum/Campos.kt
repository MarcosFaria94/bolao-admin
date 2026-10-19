package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.tema.EstiloMensagem
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Campo de data que abre o calendário ao tocar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(
    data: LocalDate?,
    aoMudar: (LocalDate) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    ajuda: String? = null,
    /** Só a data (sem o dia da semana), para campos estreitos. */
    curto: Boolean = false,
    habilitado: Boolean = true,
) {
    var aberto by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = data?.let { if (curto) Datas.longa(it) else "${Datas.diaSemana(it)}, ${Datas.longa(it)}" } ?: "",
        enabled = habilitado,
        onValueChange = {},
        readOnly = true,
        label = { Text(rotulo) },
        supportingText = ajuda?.let { { Text(it) } },
        trailingIcon = {
            IconButton(onClick = { aberto = true }, enabled = habilitado) { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Escolher data") }
        },
        singleLine = true,
        modifier = modifier,
    )
    if (aberto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (data ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        aoMudar(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}

/** O texto que vai no WhatsApp: só o título, como nos modelos do termo. */
@Composable
fun TextoDoWhatsapp(titulo: String, modifier: Modifier = Modifier, rodape: String? = null) {
    val ctx = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text("Texto no WhatsApp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(titulo, style = EstiloMensagem, softWrap = false, modifier = Modifier.horizontalScroll(rememberScrollState()))
                rodape?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            IconButton(onClick = { copiar(ctx, "Mensagem", titulo) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar texto")
            }
        }
    }
}

/** Linha de um anexo: anexar, trocar e ver. */
@Composable
fun LinhaAnexo(
    titulo: String,
    nomeArquivo: String?,
    aoAnexar: () -> Unit,
    arquivo: File? = null,
    mime: String? = null,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            if (nomeArquivo != null) Icons.Outlined.CheckCircle else Icons.Outlined.AttachFile,
            contentDescription = null,
            tint = if (nomeArquivo != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                nomeArquivo ?: "Nenhum arquivo anexado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (arquivo != null && mime != null) {
            IconButton(onClick = { Compartilhar.abrir(ctx, arquivo, mime) }) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Ver anexo")
            }
        }
        OutlinedButton(onClick = aoAnexar) { Text(if (nomeArquivo == null) "Anexar" else "Trocar") }
    }
}

@Composable
fun EspacoH(largura: Int = 8) = Spacer(Modifier.width(largura.dp))

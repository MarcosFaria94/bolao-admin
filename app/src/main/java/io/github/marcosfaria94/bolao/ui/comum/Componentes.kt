package io.github.marcosfaria94.bolao.ui.comum

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.tema.corModalidade

/** Avisos rápidos (snackbar) disponíveis em qualquer tela. */
val LocalAvisos = staticCompositionLocalOf<SnackbarHostState> { error("SnackbarHostState não fornecido") }

/**
 * Campo de dinheiro no estilo dos apps de banco: os dígitos entram pela direita
 * (0,00 -> 0,05 -> 0,50 -> 5,00) e nunca há dúvida entre vírgula e ponto.
 */
@Composable
fun CampoDinheiro(
    centavos: Long,
    aoMudar: (Long) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    ajuda: String? = null,
    erro: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    val texto = Dinheiro.formatar(centavos)
    OutlinedTextField(
        value = TextFieldValue(texto, selection = TextRange(texto.length)),
        onValueChange = { novo ->
            val digitos = novo.text.filter { it.isDigit() }.trimStart('0').take(12)
            aoMudar(digitos.toLongOrNull() ?: 0L)
        },
        label = { Text(rotulo) },
        prefix = { Text("R$ ") },
        supportingText = ajuda?.let { { Text(it) } },
        isError = erro,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = modifier,
    )
}

/** Campo para números inteiros positivos (número do jogo, concurso...). */
@Composable
fun CampoInteiro(
    valor: Int?,
    aoMudar: (Int?) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    ajuda: String? = null,
    erro: Boolean = false,
    prefixo: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val texto = valor?.toString().orEmpty()
    OutlinedTextField(
        value = TextFieldValue(texto, selection = TextRange(texto.length)),
        onValueChange = { novo ->
            val digitos = novo.text.filter { it.isDigit() }.take(7)
            aoMudar(digitos.toIntOrNull())
        },
        label = { Text(rotulo) },
        prefix = prefixo?.let { { Text(it) } },
        supportingText = ajuda?.let { { Text(it) } },
        isError = erro,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = modifier,
    )
}

/** Título de seção dentro das listas. */
@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Texto discreto para listas vazias. */
@Composable
fun TextoVazio(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

/** Selo com a sigla e a cor da modalidade (LF, MS...). */
@Composable
fun SeloModalidade(m: Modalidade, modifier: Modifier = Modifier, tamanho: Int = 40) {
    Box(
        modifier = modifier
            .size(tamanho.dp)
            .background(corModalidade(m.cor), RoundedCornerShape((tamanho / 4).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            m.sigla,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (tamanho * 0.36f).sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Uma linha "rótulo ......... valor". */
@Composable
fun LinhaValor(rotulo: String, valor: String, modifier: Modifier = Modifier, destaque: Boolean = false) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Caixa de aviso (informação ou alerta). */
@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier, alerta: Boolean = false) {
    Surface(
        color = if (alerta) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (alerta) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

val PaddingTela = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

fun copiar(context: Context, rotulo: String, texto: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(rotulo, texto))
    // A partir do Android 13 o sistema já mostra a confirmação da cópia.
    if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
}

@Composable
fun ColunaCentral(modifier: Modifier = Modifier, conteudo: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { conteudo() }
}

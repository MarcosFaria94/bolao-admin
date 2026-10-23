package io.github.marcosfaria94.bolao.ui.comum

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.tema.CoresAlerta
import java.time.LocalDate
import java.time.YearMonth

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

/**
 * Texto de uma linha só que diminui a letra (até [menor]) para caber na largura disponível,
 * sem quebrar nem cortar: prêmios grandes ao lado do botão, nomes longos de modalidade.
 */
@Composable
fun TextoQueCabe(
    texto: String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    cor: Color = Color.Unspecified,
    peso: FontWeight? = null,
    menor: TextUnit = 14.sp,
) {
    val medidor = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val largura = constraints.maxWidth
        val base = if (peso != null) estilo.copy(fontWeight = peso) else estilo
        val tamanho = remember(texto, base, largura) {
            var t = base.fontSize.value
            if (largura != Constraints.Infinity && !base.fontSize.isUnspecified) {
                while (t > menor.value) {
                    val r = medidor.measure(texto, base.copy(fontSize = t.sp), maxLines = 1, softWrap = false)
                    if (r.size.width <= largura) break
                    t -= 1f
                }
            }
            t
        }
        val final = if (base.fontSize.isUnspecified || tamanho == base.fontSize.value) {
            base
        } else {
            val escala = tamanho / base.fontSize.value
            base.copy(
                fontSize = tamanho.sp,
                lineHeight = if (base.lineHeight.isSp) (base.lineHeight.value * escala).sp else base.lineHeight,
            )
        }
        Text(texto, style = final, color = cor, maxLines = 1, softWrap = false)
    }
}

/** Título de seção dentro das listas. */
@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
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

/**
 * Caixa de aviso. Informação: fundo neutro. Alerta (falta anexar, extrato só no último dia...):
 * vermelho suave e em negrito, o mesmo padrão em todas as telas.
 */
@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier, alerta: Boolean = false) {
    val fundo = if (alerta) CoresAlerta.fundo() else MaterialTheme.colorScheme.surfaceContainerHigh
    val cor = if (alerta) CoresAlerta.texto() else MaterialTheme.colorScheme.onSurface
    Surface(color = fundo, contentColor = cor, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (alerta) Icons.Outlined.ErrorOutline else Icons.Outlined.Info,
                contentDescription = null,
                tint = if (alerta) cor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (alerta) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/**
 * Parâmetro de data ("Out/2026"): botão com a lista dos meses possíveis (os que têm dados, até o
 * atual), o mais recente primeiro. Começa no mês atual.
 */
@Composable
fun SeletorMes(
    meses: List<YearMonth>,
    escolhido: YearMonth,
    aoEscolher: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    /** Texto extra de cada mês na lista (ex.: "registrado"). */
    detalhe: (YearMonth) -> String? = { null },
) {
    var aberto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { aberto = true },
            contentPadding = PaddingValues(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text(Datas.mesAnoCurto(escolhido), style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Escolher o mês")
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            meses.forEach { m ->
                DropdownMenuItem(
                    text = {
                        Text(
                            Datas.mesAnoCurto(m) + (detalhe(m)?.let { " · $it" } ?: ""),
                            fontWeight = if (m == escolhido) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        aoEscolher(m)
                        aberto = false
                    },
                )
            }
        }
    }
}

/** Meses de [datas] (do primeiro com dados até o atual), o mais recente primeiro. */
fun mesesComDados(datas: List<LocalDate>, hoje: LocalDate): List<YearMonth> {
    val atual = YearMonth.from(hoje)
    var m = datas.minOrNull()?.let { YearMonth.from(it) }?.takeIf { it.isBefore(atual) } ?: atual
    val lista = mutableListOf<YearMonth>()
    while (!m.isAfter(atual)) {
        lista += m
        m = m.plusMonths(1)
    }
    return lista.reversed()
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

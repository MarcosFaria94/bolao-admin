package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.LeituraComprovante
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.TIMES_DO_CORACAO
import io.github.marcosfaria94.bolao.domain.partesDoTime
import io.github.marcosfaria94.bolao.ui.tema.corModalidade

/** Botão do Time do Coração (Timemania): abre a tela com os times. */
@Composable
fun BotaoTimeDoCoracao(atual: String?, aoEscolher: (String) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { aberto = true },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Time do Coração", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(atual?.ifBlank { null } ?: "Escolher o time", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("Trocar", style = MaterialTheme.typography.labelLarge)
    }
    if (aberto) {
        DialogoTimes(
            atual = atual,
            aoEscolher = {
                aoEscolher(it)
                aberto = false
            },
            aoFechar = { aberto = false },
        )
    }
}

/** Tela com os times em retângulos lado a lado: tocar escolhe. Dá para buscar e digitar outro. */
@Composable
fun DialogoTimes(atual: String?, aoEscolher: (String) -> Unit, aoFechar: () -> Unit) {
    var busca by remember { mutableStateOf("") }
    var outro by remember { mutableStateOf("") }
    val cor = corModalidade(Modalidade.TIMEMANIA.cor)
    val filtro = LeituraComprovante.simples(busca).filter { it.isLetterOrDigit() }
    val lista = TIMES_DO_CORACAO.filter { filtro.isEmpty() || filtro in LeituraComprovante.simples(it).filter { c -> c.isLetterOrDigit() } }

    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 12.dp)) {
                    IconButton(onClick = aoFechar) { Icon(Icons.Outlined.Close, contentDescription = "Fechar") }
                    Text("Time do Coração", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it.take(30) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Buscar time") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                HorizontalDivider(Modifier.padding(top = 8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(lista, key = { it }) { time ->
                        val escolhido = atual != null && Conferencia.normalizar(time) == Conferencia.normalizar(atual)
                        val (nome, uf) = partesDoTime(time)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (escolhido) cor else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (escolhido) Color.White else MaterialTheme.colorScheme.onSurface,
                            ),
                            border = if (escolhido) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 72.dp).clickable { aoEscolher(time) },
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(nome, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                                Text(uf, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    // A lista muda a cada dois anos: se faltar um time, dá para digitar.
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Outro time (como está no app da Caixa)", style = MaterialTheme.typography.labelLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = outro,
                                    onValueChange = { outro = it.take(40) },
                                    placeholder = { Text("NOME/UF") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(enabled = outro.isNotBlank(), onClick = { aoEscolher(outro.trim().uppercase()) }) { Text("Usar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

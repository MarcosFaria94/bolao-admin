package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade

/** Casas decimais dos índices na tela (parametros_k.json). */
const val CASAS_INDICE = 3

fun textoIndice(v: Double?): String = v?.let { IndiceK.exibir(it, CASAS_INDICE) } ?: "—"

/**
 * Uma modalidade na lista ordenada pelo k1 (Jogos e Nova aposta):
 * "Mega-Sena · 3072 · R$ 6,00   #1", o prêmio estimado e os índices k1 e k2.
 */
@Composable
fun CartaoIndice(posicao: Int, item: Indices.Item, estado: Estado, aoTocar: () -> Unit) {
    val m = item.modalidade
    val u = item.ultimo
    val preco = if (m.precoVariavel) "preço variável" else Dinheiro.rs(estado.config.preco(m))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SeloModalidade(m, tamanho = 52)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listOfNotNull(m.nome, item.proximoNumero?.toString(), preco).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("#$posicao", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    u?.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row {
                    when {
                        item.especial -> Text("Concurso especial: sem k1 e k2", style = MaterialTheme.typography.labelMedium)
                        item.k1 != null -> {
                            Text("k1 = ${textoIndice(item.k1)}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text("k2 = ${textoIndice(item.k2)}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        }
                        else -> Text("k1 = —    k2 = —", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

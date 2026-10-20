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

/** "k1 = 0,762  k2 = 0,091  odds = 1:34" (odds sempre inteiras). */
fun linhaIndices(item: Indices.Item): String {
    val odds = item.odds?.let { "  odds = ${IndiceK.exibirOddsInteiras(it)}" }.orEmpty()
    return if (item.especial) "Concurso especial: sem k1 e k2$odds"
    else "k1 = ${textoIndice(item.k1)}  k2 = ${textoIndice(item.k2)}$odds"
}

/**
 * Uma modalidade na lista ordenada pelo k1 (Jogos e Nova aposta):
 * "Quina · 7146 · Preço: R$ 3,00", o prêmio estimado e "k1 = ...  k2 = ...  odds = 1:34".
 */
@Composable
fun CartaoIndice(item: Indices.Item, estado: Estado, aoTocar: () -> Unit) {
    val m = item.modalidade
    val u = item.ultimo
    val preco = "Preço: " + if (m.precoVariavel) "variável" else Dinheiro.rs(estado.config.preco(m))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SeloModalidade(m, tamanho = 52)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    listOfNotNull(m.nome, item.proximoNumero?.toString(), preco).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    u?.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(linhaIndices(item), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

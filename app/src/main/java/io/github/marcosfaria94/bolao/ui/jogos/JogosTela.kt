package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.tema.Dourado

/** Jogos: as modalidades em ordem de k1 (melhor retorno matemático primeiro). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JogosTela(estado: Estado, abrirModalidade: (Modalidade) -> Unit) {
    val ctx = LocalContext.current
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    val falhou by indices.falhou.collectAsStateWithLifecycle()
    val avisos by indices.avisos.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }
    val destaque = indices.destaque(itens)?.modalidade

    PullToRefreshBox(
        isRefreshing = atualizando,
        onRefresh = { indices.atualizar(forcar = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingTela,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Legenda() }
            // Preço de Ajustes diferente do dos parâmetros ou regras da Caixa que mudaram.
            items(avisos, key = { it.chave }) { a -> Aviso(a.titulo, alerta = true) }
            if (falhou && itens.values.all { it.ultimo == null }) {
                item {
                    Text(
                        "Não foi possível consultar a Caixa agora. Puxe a lista para baixo para tentar de novo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            items(indices.ordenados(itens), key = { it.modalidade.name }) { item ->
                CartaoIndice(item, estado, destaque = item.modalidade == destaque, aoTocar = { abrirModalidade(item.modalidade) })
            }
        }
    }
}

/** O que querem dizer os índices dos cartões (a lista vem em ordem de k1). */
private val LEGENDA = listOf(
    "k1" to "Retorno médio",
    "k2" to "Retorno médio frequente",
    "odds" to "Chance de prêmio",
)

/**
 * Legenda dos índices num quadro, com os dois-pontos alinhados:
 * · k1   : Retorno médio
 * · k2   : Retorno médio frequente
 * · odds : Chance de prêmio
 */
@Composable
private fun Legenda() {
    val estilo = MaterialTheme.typography.bodyLarge
    val cor = MaterialTheme.colorScheme.onSurface
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Dourado, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Legenda", style = MaterialTheme.typography.titleMedium, color = cor)
            }
            LEGENDA.forEach { (sigla, texto) ->
                Row {
                    Text("·", style = estilo, color = suave, modifier = Modifier.width(16.dp))
                    Text(sigla, style = estilo, fontWeight = FontWeight.SemiBold, color = cor, modifier = Modifier.width(46.dp))
                    Text(": $texto", style = estilo, color = suave)
                }
            }
        }
    }
}

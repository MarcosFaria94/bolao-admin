package io.github.marcosfaria94.bolao.ui.jogos

import io.github.marcosfaria94.bolao.ui.comum.VoltarAoTopo
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.tema.Dourado

/**
 * Jogos: as modalidades em ordem de k1 (melhor retorno matemático primeiro). A Caixa é consultada
 * de novo a cada vez que se entra aqui (a raiz do app cuida disso); puxar a lista para baixo abre os
 * Painel de Jogos. Tocar numa modalidade abre a página do jogo; segurar, os Detalhes dela. Tocar em
 * "Jogos" na barra de baixo, já nos Jogos ([paraOTopo]), volta a lista ao começo. Tocar na Legenda
 * abre a página que explica o cálculo do k1 e do k2 ([abrirIndices], desde a 0.19).
 */
@Composable
fun JogosTela(
    estado: Estado,
    abrirModalidade: (Modalidade) -> Unit,
    abrirDetalhes: (Modalidade) -> Unit,
    paraOTopo: Int = 0,
    abrirIndices: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    val falhou by indices.falhou.collectAsStateWithLifecycle()
    val avisos by indices.avisos.collectAsStateWithLifecycle()
    val destaque = indices.destaque(itens)?.modalidade
    val lista = rememberLazyListState()
    VoltarAoTopo(lista, paraOTopo)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lista,
            contentPadding = PaddingTela,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Legenda(abrirIndices) }
            // Preço de Ajustes diferente do dos parâmetros ou regras da Caixa que mudaram.
            items(avisos, key = { it.chave }) { a -> Aviso(a.titulo, alerta = true) }
            if (falhou && !atualizando) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Não foi possível consultar a Caixa agora.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { indices.atualizar(forcar = true) }) { Text("Tentar de novo") }
                    }
                }
            }
            items(indices.ordenados(itens), key = { it.modalidade.name }) { item ->
                CartaoIndice(
                    item, estado, destaque = item.modalidade == destaque,
                    aoTocar = { abrirModalidade(item.modalidade) },
                    aoSegurar = { abrirDetalhes(item.modalidade) },
                )
            }
        }
        // Consultando a Caixa: uma linha fininha no alto da lista.
        if (atualizando) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
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
 * O quadro inteiro é um botão (desde a 0.19): abre a página do cálculo ("Ver cálculo ›").
 */
@Composable
private fun Legenda(aoTocar: () -> Unit) {
    val estilo = MaterialTheme.typography.bodyLarge
    val cor = MaterialTheme.colorScheme.onSurface
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = aoTocar,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Dourado, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Legenda", style = MaterialTheme.typography.titleMedium, color = cor, modifier = Modifier.weight(1f))
                Text("Ver cálculo", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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

package io.github.marcosfaria94.bolao.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.comum.FiltroHome
import io.github.marcosfaria94.bolao.ui.comum.mesesComDados
import io.github.marcosfaria94.bolao.ui.comum.movimentosDoHistorico
import io.github.marcosfaria94.bolao.ui.comum.tiposDoFiltro
import java.time.LocalDate

/**
 * "Filtrar" o Histórico da Home: os tipos e os meses, vários de cada vez (sem nada marcado vale
 * "Tudo"). Cada toque já vale; "Limpar filtro" volta ao padrão (todos os tipos no mês atual) e
 * "Ver N itens" volta à Home.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltroTela(estado: Estado, filtro: FiltroHome, mudar: (FiltroHome) -> Unit, aoFechar: () -> Unit) {
    val hoje = LocalDate.now()
    val tipos = tiposDoFiltro(estado.individual)
    val meses = mesesComDados(estado.lancamentos.map { it.data }, hoje)
    val quantos = movimentosDoHistorico(estado, filtro).size

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Tipo", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Opcao("Tudo", filtro.tipos.isEmpty()) { mudar(filtro.copy(tipos = emptySet())) }
                tipos.forEach { t -> Opcao(t.rotulo, t in filtro.tipos) { mudar(filtro.comTipo(t, tipos)) } }
            }
            Spacer(Modifier.height(6.dp))
            Text("Mês", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Opcao("Tudo", filtro.meses.isEmpty()) { mudar(filtro.copy(meses = emptySet())) }
                meses.forEach { m -> Opcao(Datas.mesAnoCurto(m), m in filtro.meses) { mudar(filtro.comMes(m)) } }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Marque quantos quiser. O filtro volta ao padrão (todos os tipos no mês atual) ao trocar de tela.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { mudar(FiltroHome.padrao(hoje)) },
                enabled = !filtro.ehPadrao(hoje),
                modifier = Modifier.weight(1f),
            ) { Text("Limpar filtro") }
            Button(onClick = aoFechar, modifier = Modifier.weight(1f)) {
                Text(if (quantos == 1) "Ver 1 item" else "Ver $quantos itens", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Uma opção do filtro (marcada ou não), com o tique quando marcada. */
@Composable
private fun Opcao(rotulo: String, marcada: Boolean, aoTocar: () -> Unit) {
    FilterChip(
        selected = marcada,
        onClick = aoTocar,
        label = { Text(rotulo, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (marcada) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
    )
}

package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import java.time.LocalDate
import java.time.YearMonth

/**
 * Histórico de um tipo de lançamento (aportes, rateios), no estilo das movimentações do mês.
 * Abre no último trimestre; o período pode ser mudado. Tocar num item mostra o comprovante.
 */
@Composable
fun Historico(
    titulo: String,
    estado: Estado,
    filtro: (Lancamento) -> Boolean,
    comprovanteDe: (Lancamento) -> Comprovante?,
    modifier: Modifier = Modifier,
) {
    val hoje = LocalDate.now()
    var de by remember { mutableStateOf(YearMonth.from(hoje).minusMonths(2).atDay(1)) }
    var ate by remember { mutableStateOf(hoje) }
    var vendo by remember { mutableStateOf<Comprovante?>(null) }
    val lista = Operacoes.ordenados(estado.lancamentos.filter { filtro(it) && !it.data.isBefore(de) && !it.data.isAfter(ate) }).reversed()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoData(data = de, aoMudar = { de = it }, rotulo = "De", modifier = Modifier.weight(1f), curto = true)
                CampoData(data = ate, aoMudar = { ate = it }, rotulo = "Até", modifier = Modifier.weight(1f), curto = true)
            }
            HorizontalDivider()
            if (lista.isEmpty()) {
                TextoVazio("Nada registrado neste período.")
            } else {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    var dia: LocalDate? = null
                    lista.forEach { l ->
                        if (l.data != dia) {
                            dia = l.data
                            Text(
                                Datas.curta(l.data),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        val c = comprovanteDe(l)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = c != null) { vendo = c }
                                .padding(vertical = 2.dp),
                        ) {
                            Text(
                                Operacoes.descricao(estado, l),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                (if (l.valor < 0) "- " else "+ ") + Dinheiro.rs(kotlin.math.abs(l.valor)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                Text(
                    "Toque num item para ver o comprovante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    vendo?.let { c ->
        VisualizadorComprovante(estado, c.titulo, aoFechar = { vendo = null }, comprovante = c)
    }
}

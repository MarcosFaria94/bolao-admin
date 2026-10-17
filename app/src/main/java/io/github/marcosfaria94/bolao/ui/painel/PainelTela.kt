package io.github.marcosfaria94.bolao.ui.painel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.comum.TituloSecao
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun PainelTela(estado: Estado, abrir: (String) -> Unit) {
    val hoje = LocalDate.now()
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val emBreve: () -> Unit = {
        escopo.launch { avisos.showSnackbar("Esta parte chega numa próxima atualização.") }
    }

    val registradas = estado.apostas.filter { it.registrada }
    val naoSorteados = registradas
        .filter { it.apuracao == null && (it.dataSorteio == null || !it.dataSorteio.isBefore(hoje)) }
        .sortedBy { it.dataSorteio ?: hoje }
    val sorteados = registradas
        .filter { it.dataSorteio != null && it.dataSorteio.isBefore(hoje) && it.apuracao?.confirmadaEm == null }
        .sortedBy { it.dataSorteio }
    val apostaDeHojeFeita = registradas.any { it.dataAposta == hoje && !it.eventual }

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item { CartaoSaldo(estado, aoAportar = emBreve, aoRatear = emBreve, aoExtrato = emBreve) }
        item { CartaoProximaAposta(hoje, apostaDeHojeFeita, aoApostar = emBreve) }

        item { TituloSecao("Jogos não sorteados") }
        if (naoSorteados.isEmpty()) {
            item { TextoVazio("Nenhum jogo aguardando sorteio.") }
        } else {
            items(naoSorteados, key = { it.id }) { ItemAposta(it, hoje) }
        }

        item { TituloSecao("Jogos sorteados") }
        if (sorteados.isEmpty()) {
            item { TextoVazio("Nenhum jogo esperando apuração.") }
        } else {
            items(sorteados, key = { it.id }) { ItemAposta(it, hoje) }
        }

        val ultimos = estado.lancamentos.sortedByDescending { it.criadoEm }.take(6)
        if (ultimos.isNotEmpty()) {
            item { TituloSecao("Últimas movimentações") }
            items(ultimos, key = { it.id }) { ItemLancamento(it, estado) }
        }
    }
}

@Composable
private fun CartaoSaldo(estado: Estado, aoAportar: () -> Unit, aoRatear: () -> Unit, aoExtrato: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Saldo líquido", style = MaterialTheme.typography.labelLarge)
            Text(Dinheiro.rs(estado.saldo), style = MaterialTheme.typography.displaySmall)
            Text(
                "1/3 de cada integrante: ${Dinheiro.rs(estado.saldo / 3)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = aoAportar, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Aporte", maxLines = 1)
                }
                FilledTonalButton(onClick = aoRatear, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rateio", maxLines = 1)
                }
                FilledTonalButton(onClick = aoExtrato, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Extrato", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CartaoProximaAposta(hoje: LocalDate, apostaDeHojeFeita: Boolean, aoApostar: () -> Unit) {
    val ehHoje = Calendario.ehDiaProgramado(hoje) && !apostaDeHojeFeita
    val dia = if (ehHoje) hoje else Calendario.diaProgramadoDepoisDe(hoje)
    val faltam = ChronoUnit.DAYS.between(hoje, dia)
    val quando = when {
        ehHoje -> "Hoje, ${Datas.diaMes(dia)}"
        faltam == 1L -> "Amanhã, ${Datas.diaMes(dia)}"
        else -> "${Datas.diaSemana(dia).replaceFirstChar { it.uppercase() }}, ${Datas.diaMes(dia)} · em $faltam dias"
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ehHoje) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Event, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Próxima aposta", style = MaterialTheme.typography.labelLarge)
                Text(quando, style = MaterialTheme.typography.titleMedium)
                if (apostaDeHojeFeita) {
                    Text("A aposta de hoje já foi feita.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = aoApostar) { Text("Apostar") }
        }
    }
}

@Composable
private fun ItemAposta(a: Aposta, hoje: LocalDate) {
    val sorteio = a.dataSorteio
    val quando = when {
        sorteio == null -> "Data do sorteio a confirmar"
        sorteio == hoje -> "Sorteio hoje"
        sorteio.isBefore(hoje) -> "Sorteado em ${Datas.diaMes(sorteio)} · aguardando apuração"
        else -> "Sorteio ${Datas.diaSemana(sorteio)}, ${Datas.diaMes(sorteio)}"
    }
    ListItem(
        leadingContent = { SeloModalidade(a.modalidade) },
        headlineContent = {
            Text("#${a.numero} · ${a.modalidade.nome} ${a.concurso}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = { Text(quando) },
        trailingContent = { Text(Dinheiro.rs(a.valor), style = MaterialTheme.typography.bodyMedium) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ItemLancamento(l: Lancamento, estado: Estado) {
    val pessoa = estado.pessoa(l.pessoaId)?.apelido
    val aposta = l.apostaId?.let { id -> estado.apostas.firstOrNull { it.id == id } }
    val descricao = when (l.tipo) {
        TipoLancamento.SALDO_INICIAL -> "Saldo inicial"
        TipoLancamento.APORTE -> if (l.eventual) "Aporte eventual ($pessoa)" else "Aporte ($pessoa)"
        TipoLancamento.APOSTA -> aposta?.let { "Aposta #${it.numero} ${it.modalidade.sigla}" } ?: "Aposta"
        TipoLancamento.PREMIO -> aposta?.let { "Prêmio #${it.numero} ${it.modalidade.sigla}" } ?: "Prêmio"
        TipoLancamento.RATEIO -> "Rateio Nº${l.rateioNumero} ($pessoa)"
        TipoLancamento.RENDIMENTO -> "Rendimento"
    }
    ListItem(
        headlineContent = { Text(descricao) },
        supportingContent = { Text(Datas.longa(l.data)) },
        trailingContent = {
            Text(
                (if (l.valor >= 0) "+ " else "− ") + Dinheiro.rs(kotlin.math.abs(l.valor)),
                color = if (l.valor >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

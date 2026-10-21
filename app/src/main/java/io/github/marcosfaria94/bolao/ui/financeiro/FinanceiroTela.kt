package io.github.marcosfaria94.bolao.ui.financeiro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.ListaLancamentos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.comum.TituloComMes
import io.github.marcosfaria94.bolao.ui.comum.mesesComDados
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FinanceiroTela(estado: Estado, abrir: (String) -> Unit) {
    val hoje = LocalDate.now()
    val provisao = Operacoes.provisao(estado, hoje)
    val saldo = Operacoes.saldo(estado)
    val meses = mesesComDados(estado.lancamentos.map { it.data }, hoje)
    var mes by remember { mutableStateOf(YearMonth.from(hoje)) }
    val movimentos = Operacoes.ordenados(estado.lancamentos.filter { YearMonth.from(it.data) == mes }).reversed()

    fun abrirLancamento(l: Lancamento) {
        when {
            l.tipo == TipoLancamento.APOSTA && l.apostaId != null -> abrir("aposta/${l.apostaId}")
            l.tipo == TipoLancamento.PREMIO && l.apostaId != null -> abrir("aposta/${l.apostaId}")
            else -> Operacoes.comprovanteDoLancamento(estado, l)?.let { abrir("comprovante/${it.id}") }
        }
    }

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Saldo(
                data = hoje,
                saldo = saldo,
                provisionado = provisao.provisionado,
                extra = saldo - provisao.provisionado,
                aoTocar = { abrir("saldo") },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Bloco("Aporte", Icons.Outlined.Savings, Modifier.weight(1f)) { abrir("aporte") }
                Bloco("Rateio", Icons.Outlined.Groups, Modifier.weight(1f)) { abrir("rateio") }
                Bloco("Extrato", Icons.AutoMirrored.Outlined.ReceiptLong, Modifier.weight(1f)) { abrir("extrato") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TituloComMes("Movimentações", meses, mes) { mes = it }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        if (movimentos.isEmpty()) {
                            TextoVazio("Nenhuma movimentação em ${Datas.mesAnoExtenso(mes)}.", Modifier.padding(vertical = 10.dp))
                        } else {
                            ListaLancamentos(estado, movimentos, aoTocar = ::abrirLancamento)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Saldo no estilo dos apps de banco: rótulo discreto, o valor grande e, embaixo,
 * o provisionado e o extra. Tocar gera o documento do saldo para o grupo.
 */
@Composable
private fun Saldo(data: LocalDate, saldo: Long, provisionado: Long, extra: Long, aoTocar: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Saldo líquido · ${Datas.relativa(data, LocalDate.now())}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = aoTocar) {
                Icon(Icons.Outlined.Share, contentDescription = "Enviar o saldo ao grupo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(Dinheiro.rs(saldo), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Indicador("Provisionado", Dinheiro.rs(provisionado), Modifier.weight(1f))
            Indicador("Extra", (if (extra < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(extra)), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Indicador(rotulo: String, valor: String, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Bloco(rotulo: String, icone: ImageVector, modifier: Modifier, aoTocar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.aspectRatio(1.15f).clickable(onClick = aoTocar),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icone, contentDescription = null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(8.dp))
                Text(rotulo, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

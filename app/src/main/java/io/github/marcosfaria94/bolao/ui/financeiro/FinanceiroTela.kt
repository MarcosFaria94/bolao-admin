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
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Selos
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.teste.RoteiroDeTeste
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FinanceiroTela(estado: Estado, selos: Selos, abrir: (String) -> Unit, teste: Boolean = false) {
    val hoje = LocalDate.now()
    val provisao = Operacoes.provisao(estado, hoje)
    val saldo = Operacoes.saldo(estado)
    val mes = YearMonth.from(hoje)
    val movimentos = Operacoes.ordenados(estado.lancamentos.filter { YearMonth.from(it.data) == mes }).reversed()
    val naoEnviados = Operacoes.comprovantesPendentesDeEnvio(estado)

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (teste) item { RoteiroDeTeste() }
        item {
            CartaoSaldo(
                data = hoje,
                saldo = saldo,
                provisionado = provisao.provisionado,
                extra = saldo - provisao.provisionado,
                aoTocar = { abrir("saldo") },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Bloco("Aporte", Icons.Outlined.Savings, if (selos.aportePendente) 1 else 0, Modifier.weight(1f)) { abrir("aporte") }
                Bloco("Rateio", Icons.Outlined.Groups, 0, Modifier.weight(1f)) { abrir("rateio") }
                Bloco("Extrato", Icons.AutoMirrored.Outlined.ReceiptLong, selos.extratosPendentes.size, Modifier.weight(1f)) { abrir("extrato") }
            }
        }
        if (naoEnviados.isNotEmpty()) {
            item { NaoEnviados(estado, naoEnviados) }
        }
        item { Movimentacoes(estado, movimentos) }
    }
}

@Composable
private fun CartaoSaldo(data: LocalDate, saldo: Long, provisionado: Long, extra: Long, aoTocar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Saldo Líquido · ${Datas.longa(data)}", style = MaterialTheme.typography.titleSmall)
                Text(Dinheiro.rs(saldo), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Provisionado: ${Dinheiro.rs(provisionado)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Extra: " + (if (extra < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(extra)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Outlined.Share, contentDescription = "Enviar o saldo ao grupo", modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun Bloco(rotulo: String, icone: ImageVector, selo: Int, modifier: Modifier, aoTocar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.aspectRatio(1.15f).clickable(onClick = aoTocar),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BadgedBox(badge = { if (selo > 0) Badge { Text(selo.toString()) } }) {
                    Icon(icone, contentDescription = null, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(rotulo, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Comprovantes registrados que ainda não foram abertos no WhatsApp. */
@Composable
private fun NaoEnviados(estado: Estado, lista: List<Comprovante>) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var vendo by remember { mutableStateOf<Comprovante?>(null) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Comprovantes ainda não enviados ao grupo", style = MaterialTheme.typography.titleSmall)
            lista.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable { vendo = c },
                    )
                    OutlinedButton(onClick = { escopo.launch { Compartilhar.comprovante(ctx, estado, c) } }) { Text("Enviar") }
                }
            }
        }
    }
    vendo?.let { c ->
        VisualizadorComprovante(estado, c.titulo, aoFechar = { vendo = null }, comprovante = c)
    }
}

@Composable
private fun Movimentacoes(estado: Estado, lista: List<Lancamento>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Movimentações do mês", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (lista.isEmpty()) {
                TextoVazio("Nenhuma movimentação neste mês.")
            } else {
                ListaMovimentos(estado, lista)
            }
        }
    }
}

/** Movimentações agrupadas por dia (o mais recente primeiro), no estilo do extrato. */
@Composable
fun ListaMovimentos(estado: Estado, lista: List<Lancamento>) {
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
        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
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
                color = if (l.valor < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

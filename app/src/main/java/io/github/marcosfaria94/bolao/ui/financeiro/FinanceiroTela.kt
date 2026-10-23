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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.comum.CoresEmblema
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.ListaLancamentos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.QuadroSaldo
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
            // Tocar no quadro gera o "Saldo atual".
            QuadroSaldo(saldo, provisao.provisionado, Modifier.padding(top = 8.dp), aoTocar = { abrir("saldo") })
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Bloco("Aporte", Desenhos.Cofrinho, CoresEmblema.APORTE, Modifier.weight(1f)) { abrir("aporte") }
                Bloco("Rateio", Desenhos.Moedas, CoresEmblema.RATEIO, Modifier.weight(1f)) { abrir("rateio") }
                Bloco("Extrato", Desenhos.Extrato, CoresEmblema.EXTRATO, Modifier.weight(1f)) { abrir("extrato") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TituloComMes("Histórico", meses, mes) { mes = it }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        if (movimentos.isEmpty()) {
                            TextoVazio("Nada registrado em ${Datas.mesAnoCurto(mes)}.", Modifier.padding(vertical = 10.dp))
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

/** Atalho do Financeiro (Aporte, Rateio, Extrato): o desenho colorido num círculo e o nome. */
@Composable
private fun Bloco(rotulo: String, desenho: ImageVector, cor: Color, modifier: Modifier, aoTocar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.aspectRatio(1.05f).clickable(onClick = aoTocar),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmblemaDesenho(desenho, cor, tamanho = 54.dp)
                Spacer(Modifier.height(8.dp))
                Text(rotulo, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

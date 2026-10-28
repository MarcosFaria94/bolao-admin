package io.github.marcosfaria94.bolao.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.FiltroHistorico
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.Indicador
import io.github.marcosfaria94.bolao.ui.comum.Quadro
import io.github.marcosfaria94.bolao.ui.comum.QuadroSaldo
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.valorComSinal
import io.github.marcosfaria94.bolao.ui.tema.AmareloSino
import io.github.marcosfaria94.bolao.ui.tema.Barra
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import io.github.marcosfaria94.bolao.ui.comum.SeletorOpcoes
import io.github.marcosfaria94.bolao.ui.comum.itensDoHistorico
import io.github.marcosfaria94.bolao.ui.comum.mesesComDados
import io.github.marcosfaria94.bolao.ui.comum.movimentosDoHistorico
import java.time.LocalDate
import java.time.YearMonth

/** Espaço entre os blocos da Home (o Histórico, embaixo, é uma lista contínua). */
private val ESPACO = 14.dp

/**
 * Home: o saldo (com o provisionado e o extra à direita), os atalhos de Aporte, Rateio e Extrato,
 * a próxima aposta e o Histórico de tudo o que entrou e saiu, com o sino das notificações e os
 * parâmetros de tipo e de mês. No modo individual: só o saldo (que começa em zero e cai a cada
 * aposta) e o Histórico das apostas e dos prêmios.
 *
 * [visita] muda a cada vez que se vai para outra aba: aí os parâmetros do Histórico (e a rolagem)
 * recomeçam. Ao voltar de uma tela aberta daqui (uma aposta, um comprovante), ficam como estavam.
 */
@Composable
fun HomeTela(estado: Estado, visita: Int, pendencias: Int, abrir: (String) -> Unit, apostar: () -> Unit) {
    val hoje = LocalDate.now()
    val provisao = Operacoes.provisao(estado, hoje)
    val saldo = Operacoes.saldo(estado)
    val individual = estado.individual
    // Os dois parâmetros do Histórico: começam em "Tudo" e no mês atual; mudar um não mexe no outro.
    // Com a [visita] na chave e nas entradas: numa visita nova (depois de outra aba), nada do que foi
    // guardado volta, esteja a Home ainda na tela ou não.
    var filtro by rememberSaveable(visita, key = "filtro-$visita") { mutableStateOf(FiltroHistorico.TUDO) }
    var mes by rememberSaveable(visita, key = "mes-$visita") { mutableStateOf<YearMonth?>(YearMonth.from(hoje)) }
    val rolagem = rememberSaveable(visita, saver = LazyListState.Saver, key = "rolagem-$visita") { LazyListState() }
    val meses: List<YearMonth?> = listOf(null) + mesesComDados(estado.lancamentos.map { it.data }, hoje)
    val movimentos = movimentosDoHistorico(estado, filtro, mes)
    val tipos = if (individual) listOf(FiltroHistorico.TUDO, FiltroHistorico.APOSTAS, FiltroHistorico.PREMIOS) else FiltroHistorico.entries

    LazyColumn(state = rolagem, contentPadding = PaddingTela, modifier = Modifier.fillMaxSize()) {
        if (individual) {
            item(key = "saldo") { QuadroSaldoIndividual(estado, Modifier.padding(top = 8.dp, bottom = ESPACO)) }
        } else {
            item(key = "saldo") {
                // Tocar no quadro gera o "Saldo atual".
                QuadroSaldo(saldo, provisao.provisionado, Modifier.padding(top = 8.dp, bottom = ESPACO), aoTocar = { abrir("saldo") })
            }
            item(key = "atalhos") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = ESPACO)) {
                    Bloco("Aporte", Desenhos.Cofrinho, Modifier.weight(1f)) { abrir("aporte") }
                    Bloco("Rateio", Desenhos.Moedas, Modifier.weight(1f)) { abrir("rateio") }
                    Bloco("Extrato", Desenhos.Extrato, Modifier.weight(1f)) { abrir("extrato") }
                }
            }
            item(key = "proxima") {
                CartaoProximaAposta(hoje, Operacoes.apostaFeitaEm(estado, hoje), Modifier.padding(bottom = ESPACO), aoApostar = apostar)
            }
        }
        item(key = "titulo-historico") {
            // "Histórico", o sino das notificações, o tipo e o mês (com "Tudo" nos dois).
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Histórico", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Sino(pendencias) { abrir("notificacoes") }
                Spacer(Modifier.width(4.dp))
                SeletorOpcoes(tipos, filtro, { it.rotulo }, { filtro = it }, descricao = "Escolher o tipo")
                Spacer(Modifier.width(6.dp))
                SeletorOpcoes(meses, mes, { m -> m?.let { Datas.mesAnoCurto(it) } ?: "Tudo" }, { mes = it }, descricao = "Escolher o mês")
            }
        }
        itensDoHistorico(
            estado,
            movimentos,
            hoje,
            vazio = "Nada registrado" + (mes?.let { " em ${Datas.mesAnoCurto(it)}" } ?: "") + ".",
            abrir = abrir,
        )
    }
}

/** Sino das notificações (no Histórico, entre o título e os parâmetros): todo amarelo, com o contador vermelho de pendências. */
@Composable
private fun Sino(pendencias: Int, aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar, modifier = Modifier.size(44.dp)) {
        BadgedBox(badge = {
            if (pendencias > 0) {
                Badge(containerColor = Barra.contador, contentColor = Color.White) {
                    Text(if (pendencias > 99) "99+" else pendencias.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notificações", tint = AmareloSino, modifier = Modifier.size(26.dp))
        }
    }
}

/**
 * Saldo do modo individual: começa em zero, cai a cada aposta e sobe com os prêmios. Negativo (em
 * vermelho) quer dizer que se gastou mais do que se ganhou; embaixo, o gasto e os prêmios.
 */
@Composable
private fun QuadroSaldoIndividual(estado: Estado, modifier: Modifier) {
    val saldo = Operacoes.saldo(estado)
    val gasto = -estado.lancamentos.filter { it.tipo == TipoLancamento.APOSTA }.sumOf { it.valor }
    val premios = estado.lancamentos.filter { it.tipo == TipoLancamento.PREMIO }.sumOf { it.valor }
    val cor = when {
        saldo < 0 -> MaterialTheme.colorScheme.error
        saldo > 0 -> corEntrada()
        else -> MaterialTheme.colorScheme.onSurface
    }
    Quadro(modifier) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("Saldo", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextoQueCabe(
                valorComSinal(saldo).removePrefix("+ ").let { if (saldo == 0L) Dinheiro.rs(0) else it },
                MaterialTheme.typography.displayMedium,
                cor = cor,
                peso = FontWeight.Bold,
                menor = 22.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Indicador("Gasto em apostas", Dinheiro.rs(gasto), Modifier.weight(1f), alinharNoFim = false)
                Indicador("Prêmios", Dinheiro.rs(premios))
            }
        }
    }
}

/** Atalho da Home (Aporte, Rateio, Extrato): o desenho no círculo neutro e o nome. */
@Composable
private fun Bloco(rotulo: String, desenho: ImageVector, modifier: Modifier, aoTocar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.aspectRatio(1.05f).clickable(onClick = aoTocar),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmblemaDesenho(desenho, tamanho = 54.dp)
                Spacer(Modifier.height(8.dp))
                Text(rotulo, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** "Próxima aposta: Hoje" e o "Apostar", que abre os Jogos (lá fica o volante de cada modalidade). */
@Composable
private fun CartaoProximaAposta(hoje: LocalDate, apostaDeHojeFeita: Boolean, modifier: Modifier, aoApostar: () -> Unit) {
    val ehHoje = Calendario.ehDiaProgramado(hoje) && !apostaDeHojeFeita
    val dia = if (ehHoje) hoje else Calendario.diaProgramadoDepoisDe(hoje)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Próxima aposta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(Datas.relativa(dia, hoje), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = aoApostar,
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp),
            ) { Text("Apostar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

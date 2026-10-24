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
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.FiltroHistorico
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.QuadroSaldo
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
 * a próxima aposta e o Histórico de tudo o que entrou e saiu, com os parâmetros de tipo e de mês.
 * O sino das notificações fica na barra de título (AppRaiz).
 *
 * [visita] muda a cada vez que se vai para outra aba: aí os parâmetros do Histórico (e a rolagem)
 * recomeçam. Ao voltar de uma tela aberta daqui (uma aposta, um comprovante), ficam como estavam.
 */
@Composable
fun HomeTela(estado: Estado, visita: Int, abrir: (String) -> Unit, apostar: () -> Unit) {
    val hoje = LocalDate.now()
    val provisao = Operacoes.provisao(estado, hoje)
    val saldo = Operacoes.saldo(estado)
    // Os dois parâmetros do Histórico: começam em "Tudo" e no mês atual; mudar um não mexe no outro.
    // Com a [visita] na chave e nas entradas: numa visita nova (depois de outra aba), nada do que foi
    // guardado volta, esteja a Home ainda na tela ou não.
    var filtro by rememberSaveable(visita, key = "filtro-$visita") { mutableStateOf(System.getProperty("fotos.filtro")?.let { FiltroHistorico.valueOf(it) } ?: FiltroHistorico.TUDO) }
    var mes by rememberSaveable(visita, key = "mes-$visita") { mutableStateOf<YearMonth?>(if (System.getProperty("fotos.tudo") == "1") null else YearMonth.from(hoje)) }
    val rolagem = rememberSaveable(visita, saver = LazyListState.Saver, key = "rolagem-$visita") { LazyListState() }
    val meses: List<YearMonth?> = listOf(null) + mesesComDados(estado.lancamentos.map { it.data }, hoje)
    val movimentos = movimentosDoHistorico(estado, filtro, mes)

    LazyColumn(state = rolagem, contentPadding = PaddingTela, modifier = Modifier.fillMaxSize()) {
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
        item(key = "titulo-historico") {
            // "Histórico", o tipo e o mês (com "Tudo" nos dois).
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Histórico", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                SeletorOpcoes(FiltroHistorico.entries, filtro, { it.rotulo }, { filtro = it }, descricao = "Escolher o tipo")
                Spacer(Modifier.width(8.dp))
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

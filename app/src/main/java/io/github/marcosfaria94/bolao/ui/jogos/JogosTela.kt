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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Jogos: as modalidades em ordem de k1 (melhor retorno matemático primeiro). Desde a 0.20 os dados
 * ficam guardados no celular e valem até o próximo sorteio: a Caixa só é consultada 1 hora depois de
 * cada sorteio ([AgendaDaCaixa]); o quadro do alto diz se os dados estão em dia ou esperando o
 * resultado (e quais jogos). Puxar a lista para baixo abre o Painel de Jogos. Tocar numa modalidade abre a página do jogo; segurar, os Detalhes dela. Tocar em
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
    val consultas by indices.consultas.collectAsStateWithLifecycle()
    val avisos by indices.avisos.collectAsStateWithLifecycle()
    // O relógio da agenda: a cada meio minuto a situação é refeita e, se um sorteio passou da hora de
    // consultar, a Caixa é consultada (só para os jogos que precisam).
    val agora by produceState(Instant.now()) {
        while (true) {
            delay(30_000)
            value = Instant.now()
        }
    }
    LaunchedEffect(agora) { indices.atualizar() }
    val situacoes = Modalidade.entries.associateWith { indices.situacao(it, agora, itens, consultas) }
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
            // Os dados estão em dia (até o próximo sorteio) ou esperando o resultado?
            item(key = "situacao") {
                SituacaoDosDados(situacoes, consultas, agora, atualizando, aoTentar = { indices.atualizar(forcar = true) })
            }
            // Preço de Ajustes diferente do dos parâmetros ou regras da Caixa que mudaram.
            items(avisos, key = { it.chave }) { a -> Aviso(a.titulo, alerta = true) }
            items(indices.ordenados(itens), key = { it.modalidade.name }) { item ->
                CartaoIndice(
                    item, estado, destaque = item.modalidade == destaque,
                    aoTocar = { abrirModalidade(item.modalidade) },
                    aoSegurar = { abrirDetalhes(item.modalidade) },
                    situacao = situacoes[item.modalidade],
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
    val larguraDaSigla = with(LocalDensity.current) { 52.sp.toDp() }
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
                    // A largura acompanha o tamanho da letra do celular (com a letra grande, "odds" quebrava).
                    Text(sigla, style = estilo, fontWeight = FontWeight.SemiBold, color = cor, maxLines = 1, softWrap = false, modifier = Modifier.width(larguraDaSigla))
                    Text(": $texto", style = estilo, color = suave)
                }
            }
        }
    }
}

/** Os jogos que esperam o resultado (o sorteio passou, falta o rateio ou não há dados). */
fun esperandoResultado(situacoes: Map<Modalidade, AgendaDaCaixa.Situacao>): List<Modalidade> =
    situacoes.filterValues { !it.certa && it.estado != AgendaDaCaixa.Estado.SEM_DATA }.keys.toList()

/** "Quina", "Quina e Lotofácil", "Quina, Lotofácil e Mega-Sena". */
fun nomesDosJogos(lista: List<Modalidade>): String {
    val nomes = lista.map { it.nome }
    return if (nomes.size <= 1) nomes.joinToString() else nomes.dropLast(1).joinToString(", ") + " e " + nomes.last()
}

/**
 * O quadro da situação dos dados (desde a 0.20), abaixo da Legenda. Em dia: "Dados em dia", até quando
 * valem (o próximo sorteio) e que o app consulta a Caixa 1 hora depois de cada sorteio. Esperando: os
 * jogos que esperam o resultado e quando é a próxima consulta; se a Caixa não respondeu, o erro e o
 * "Tentar agora".
 */
@Composable
private fun SituacaoDosDados(
    situacoes: Map<Modalidade, AgendaDaCaixa.Situacao>,
    consultas: Map<Modalidade, Indices.Consulta>,
    agora: Instant,
    atualizando: Boolean,
    aoTentar: () -> Unit,
) {
    val esperando = esperandoResultado(situacoes)
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    val erro = esperando.firstNotNullOfOrNull { consultas[it]?.erro }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val emDia = esperando.isEmpty()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (emDia) Icons.Outlined.CheckCircle else Icons.Outlined.HourglassTop,
                    contentDescription = null,
                    tint = if (emDia) corEntrada() else corDestaque(),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (emDia) TITULO_EM_DIA else TITULO_ESPERANDO,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (emDia) {
                val proximo = situacoes.values.mapNotNull { s -> s.sorteio?.takeIf { s.certa } }.minOrNull()
                Text(
                    (proximo?.let { "Valem até o próximo sorteio, ${AgendaDaCaixa.quando(it, agora)}. " } ?: "") +
                        "O app consulta a Caixa 1 hora depois de cada sorteio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = suave,
                )
            } else {
                Text(nomesDosJogos(esperando), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val consulta = esperando.mapNotNull { situacoes[it]?.consultarEm }.minOrNull()
                /** "Quina: " quando a frase não vale para todos os jogos que esperam. */
                fun quais(lista: List<Modalidade>) = if (lista.size < esperando.size) nomesDosJogos(lista) + ": " else ""
                val texto = buildList {
                    val sorteados = esperando.filter { situacoes[it]?.sorteado == true }
                    if (sorteados.isNotEmpty()) {
                        val sorteio = sorteados.mapNotNull { situacoes[it]?.sorteio }.minOrNull()
                        val frase = (sorteio?.let { "o sorteio foi ${AgendaDaCaixa.quando(it, agora)}" } ?: "o sorteio já foi") +
                            "; até o resultado chegar, os dados são do concurso já sorteado."
                        add(quais(sorteados) + if (quais(sorteados).isEmpty()) frase.replaceFirstChar { it.uppercase() } else frase)
                    }
                    val semRateio = esperando.filter { situacoes[it]?.estado == AgendaDaCaixa.Estado.SEM_RATEIO }
                    if (semRateio.isNotEmpty()) add(quais(semRateio) + (if (quais(semRateio).isEmpty()) "O" else "o") + " resultado chegou sem o rateio.")
                    val semDados = esperando.filter { situacoes[it]?.estado == AgendaDaCaixa.Estado.SEM_DADOS }
                    if (semDados.isNotEmpty()) add(quais(semDados) + (if (quais(semDados).isEmpty()) "A" else "a") + "inda sem os dados da Caixa.")
                    add(
                        when {
                            atualizando -> "Consultando a Caixa..."
                            consulta == null || !consulta.isAfter(agora) -> "O app consulta a Caixa em instantes."
                            else -> "Próxima consulta à Caixa ${AgendaDaCaixa.quando(consulta, agora)}."
                        },
                    )
                }
                Text(texto.joinToString(" "), style = MaterialTheme.typography.bodyMedium, color = suave)
                if (erro != null && !atualizando) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            erro,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = aoTentar) { Text("Tentar agora") }
                    }
                }
            }
        }
    }
}

const val TITULO_EM_DIA = "Dados em dia"
const val TITULO_ESPERANDO = "Aguardando resultado"

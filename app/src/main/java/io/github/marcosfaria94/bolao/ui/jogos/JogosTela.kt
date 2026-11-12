package io.github.marcosfaria94.bolao.ui.jogos

import io.github.marcosfaria94.bolao.ui.comum.VoltarAoTopo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Jogos: as modalidades em ordem de k1 (melhor retorno matemático primeiro). Desde a 0.20 os dados
 * ficam guardados no celular e valem até o próximo sorteio: a Caixa só é consultada depois de cada
 * sorteio (desde a 0.25, 5 minutos depois e de 5 em 5 minutos até o resultado chegar; [AgendaDaCaixa]); o quadro do alto diz se os dados estão em dia ou esperando o
 * resultado (e quais jogos). Puxar a lista para baixo abre o Painel de Jogos. Tocar numa modalidade abre a página do jogo; segurar, os Detalhes dela. Tocar em
 * "Jogos" na barra de baixo, já nos Jogos ([paraOTopo]), volta a lista ao começo. Desde a 0.22: o título
 * grande "Escolha um jogo" e, embaixo, dois botões lado a lado: "Indicadores" (abre a página que explica
 * o k1 e o k2, [abrirIndices]) e "Dados em dia" (ou "Aguardando resultado"), que ao tocar mostra o texto.
 * Desde a 0.27: cada jogo com a tarja da modalidade (como as da Caixa), sem a Loteria Federal, e os
 * concursos especiais com apostas abertas (Mega da Virada...) como opções à parte, logo antes dos outros
 * jogos ([Especiais.aParte]; tocar abre a página do especial, [abrirEspecial], com os Detalhes se segurar).
 * Desde a 0.28, havendo um concurso especial com apostas abertas, ele é o concurso em destaque (sempre o
 * primeiro da lista), com o título "CONCURSO ESPECIAL" brilhando.
 */
@Composable
fun JogosTela(
    estado: Estado,
    abrirModalidade: (Modalidade) -> Unit,
    abrirDetalhes: (Modalidade) -> Unit,
    paraOTopo: Int = 0,
    abrirIndices: () -> Unit = {},
    abrirEspecial: (ConcursoEspecial, detalhes: Boolean) -> Unit = { _, _ -> },
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
    val situacoes = Modalidade.ATIVAS.associateWith { indices.situacao(it, agora, itens, consultas) }
    val especiais = Especiais.aParte(agora, { itens[it]?.proximoNumero })
    // O concurso em destaque: desde a 0.28, sempre o concurso especial quando há um (senão, o de maior k1).
    val destaque = indices.destaque(itens, agora)
    // O especial em destaque que já é o próximo concurso da modalidade: o cartão dela vai para o alto da lista.
    val especialNoCartao = destaque?.especial?.takeIf { e -> itens[e.modalidade]?.proximoNumero == e.concurso }
    val lista = rememberLazyListState()
    VoltarAoTopo(lista, paraOTopo)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lista,
            contentPadding = PaddingTela,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // O título bem grande (desde a 0.22).
            item(key = "titulo") {
                Text(
                    TITULO_DOS_JOGOS,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            }
            // Os dois botões, lado a lado: os Indicadores (abre a página do k1 e do k2) e a situação dos dados
            // (em dia ou esperando o resultado), que ao tocar mostra o texto embaixo.
            item(key = "situacao") {
                var aberto by rememberSaveable { mutableStateOf(false) }
                val esperando = esperandoResultado(situacoes)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        BotaoDoQuadro(
                            INDICADORES, Icons.Outlined.Info, Dourado, Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            Modifier.weight(1f).fillMaxHeight(), aoTocar = abrirIndices,
                        )
                        BotaoDoQuadro(
                            if (esperando.isEmpty()) TITULO_EM_DIA else TITULO_ESPERANDO,
                            if (esperando.isEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.HourglassTop,
                            if (esperando.isEmpty()) corEntrada() else corDestaque(),
                            if (aberto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            Modifier.weight(1f).fillMaxHeight(),
                            descricaoDoSinal = if (aberto) "Fechar" else "Ver mais",
                        ) { aberto = !aberto }
                    }
                    if (aberto) SituacaoDosDados(situacoes, consultas, agora, atualizando, aoTentar = { indices.atualizar(forcar = true) })
                }
            }
            // Preço de Ajustes diferente do dos parâmetros ou regras da Caixa que mudaram.
            items(avisos, key = { it.chave }) { a -> Aviso(a.titulo, alerta = true) }
            // Os concursos especiais com apostas abertas (desde a 0.27), antes dos outros jogos.
            items(especiais, key = { "especial-${it.modalidade.name}-${it.concurso}" }) { e ->
                CartaoEspecial(
                    e, itens[e.modalidade], estado,
                    aoTocar = { abrirEspecial(e, false) },
                    aoSegurar = { abrirEspecial(e, true) },
                    destaque = destaque?.especial == e,
                )
            }
            val ordem = indices.ordenados(itens).let { l -> if (especialNoCartao == null) l else l.sortedByDescending { it.modalidade == especialNoCartao.modalidade } }
            items(ordem, key = { it.modalidade.name }) { item ->
                CartaoIndice(
                    item, estado,
                    destaque = destaque != null && item.modalidade == destaque.modalidade &&
                        (destaque.especial == null || destaque.especial == especialNoCartao),
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

/** O título grande da tela Jogos (desde a 0.22). */
const val TITULO_DOS_JOGOS = "Escolha um jogo"

/** O botão que abre a página do k1, do k2 e das odds (até a 0.21, o quadro "Legenda", com o texto de cada índice). */
const val INDICADORES = "Indicadores"

/**
 * Um dos dois botões do alto dos Jogos (desde a 0.22): o ícone, o nome e, no fim, o sinal de que é um
 * botão (a seta para a página, ou a de abrir e fechar), com o contorno na cor do app.
 */
@Composable
private fun BotaoDoQuadro(
    texto: String,
    icone: ImageVector,
    corDoIcone: Color,
    sinal: ImageVector,
    modifier: Modifier = Modifier,
    descricaoDoSinal: String? = null,
    aoTocar: () -> Unit,
) {
    val primaria = MaterialTheme.colorScheme.primary
    // A letra não cresce com a letra grande do celular: os dois botões dividem a largura da tela, e com a
    // letra grande "Indicadores" quebrava no meio da palavra.
    val (letra, linha) = with(LocalDensity.current) { 16.dp.toSp() to 19.dp.toSp() }
    Card(
        onClick = aoTocar,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.5.dp, primaria.copy(alpha = 0.55f)),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight().padding(start = 10.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(icone, contentDescription = null, tint = corDoIcone, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                texto,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = letra, lineHeight = linha),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Icon(sinal, contentDescription = descricaoDoSinal, tint = primaria, modifier = Modifier.size(22.dp))
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
 * O texto da situação dos dados (desde a 0.20; desde a 0.22 só aparece ao tocar no botão "Dados em dia"
 * ou "Aguardando resultado"). Em dia: até quando os dados valem (o próximo sorteio) e que o app consulta
 * a Caixa 5 minutos depois de cada sorteio. Esperando: os jogos que esperam o resultado e quando é a
 * próxima consulta; se a Caixa não respondeu, o erro e o "Tentar agora".
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
            if (emDia) {
                val proximo = situacoes.values.mapNotNull { s -> s.sorteio?.takeIf { s.certa } }.minOrNull()
                Text(
                    (proximo?.let { "Valem até o próximo sorteio, ${AgendaDaCaixa.quando(it, agora)}. " } ?: "") +
                        "O app consulta a Caixa 5 minutos depois de cada sorteio, de 5 em 5 minutos até o resultado chegar.",
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

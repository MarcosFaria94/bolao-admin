package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.ui.aposta.rotaDaAposta
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.TituloDestaque
import io.github.marcosfaria94.bolao.ui.comum.bola3D
import io.github.marcosfaria94.bolao.ui.comum.estiloNaBola
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import java.time.LocalDate

/** Jogos: as modalidades em ordem de k1 (melhor retorno matemático primeiro). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JogosTela(estado: Estado, abrirModalidade: (Modalidade) -> Unit) {
    val ctx = LocalContext.current
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    val falhou by indices.falhou.collectAsStateWithLifecycle()
    val avisos by indices.avisos.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }
    val destaque = indices.destaque(itens)?.modalidade

    PullToRefreshBox(
        isRefreshing = atualizando,
        onRefresh = { indices.atualizar(forcar = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingTela,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Legenda() }
            // Preço de Ajustes diferente do dos parâmetros ou regras da Caixa que mudaram.
            items(avisos, key = { it.chave }) { a -> Aviso(a.titulo, alerta = true) }
            if (falhou && itens.values.all { it.ultimo == null }) {
                item {
                    Text(
                        "Não foi possível consultar a Caixa agora. Puxe a lista para baixo para tentar de novo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            items(indices.ordenados(itens), key = { it.modalidade.name }) { item ->
                CartaoIndice(item, estado, destaque = item.modalidade == destaque, aoTocar = { abrirModalidade(item.modalidade) })
            }
        }
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
 */
@Composable
private fun Legenda() {
    val estilo = MaterialTheme.typography.bodyLarge
    val cor = MaterialTheme.colorScheme.onSurface
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Dourado, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Legenda", style = MaterialTheme.typography.titleMedium, color = cor)
            }
            LEGENDA.forEach { (sigla, texto) ->
                Row {
                    Text("·", style = estilo, color = suave, modifier = Modifier.width(16.dp))
                    Text(sigla, style = estilo, fontWeight = FontWeight.SemiBold, color = cor, modifier = Modifier.width(46.dp))
                    Text(": $texto", style = estilo, color = suave)
                }
            }
        }
    }
}

/** Como funciona a aposta simples (regras resumidas). */
private fun regras(m: Modalidade, estado: Estado): List<Pair<String, String>> {
    val preco = "Preço" to if (m.precoVariavel) "variável" else Dinheiro.rs(estado.config.preco(m))
    return when (m.formato) {
        Formato.NUMEROS -> listOf(
            "Volante" to "${m.rotulo(m.menor)} a ${m.rotulo(m.maior)}" + if (m.trevos > 0) " + trevos de 1 a 6" else "",
            "Aposta simples" to "${m.qtdAposta} dezenas" + when {
                m.trevos > 0 -> " + ${m.trevos} trevos"
                m.extra == Extra.TIME_DO_CORACAO -> " + Time do Coração"
                m.extra == Extra.MES_DE_SORTE -> " + Mês de Sorte"
                else -> ""
            },
            "Sorteio" to if (m.sorteios > 1) "${m.sorteios} sorteios de ${m.qtdSorteada} dezenas" else "${m.qtdSorteada} dezenas",
            preco,
            "Premiação" to premiacao(m),
        )
        Formato.COLUNAS -> listOf("Volante" to "7 colunas de 0 a 9", "Aposta simples" to "1 número por coluna", preco, "Premiação" to premiacao(m))
        Formato.LOTECA -> listOf("Volante" to "14 jogos de futebol", "Aposta simples" to "1 palpite por jogo (1, X ou 2)", preco, "Premiação" to premiacao(m))
        Formato.FEDERAL -> listOf("Bilhete" to "5 algarismos, inteiro ou em frações", preco, "Premiação" to premiacao(m))
    }
}

private fun premiacao(m: Modalidade): String = when (m) {
    Modalidade.MEGA_SENA -> "Acertar 4, 5 ou 6"
    Modalidade.LOTOFACIL -> "Acertar 11 a 15"
    Modalidade.QUINA -> "Acertar 2 a 5"
    Modalidade.LOTOMANIA -> "Acertar 15 a 20, ou nenhum"
    Modalidade.TIMEMANIA -> "Acertar 3 a 7, ou o Time do Coração"
    Modalidade.DUPLA_SENA -> "Acertar 3 a 6 em qualquer sorteio"
    Modalidade.DIA_DE_SORTE -> "Acertar 4 a 7, ou o Mês de Sorte"
    Modalidade.SUPER_SETE -> "Acertar 3 a 7 colunas"
    Modalidade.MAIS_MILIONARIA -> "A partir de 2 números + 1 trevo"
    Modalidade.LOTECA -> "Acertar 13 ou 14 jogos"
    Modalidade.FEDERAL -> "5 prêmios, aproximações e finais"
}

/**
 * Página do jogo: o emblema (sigla e próximo concurso), o nome e a chance do prêmio principal com
 * o botão "Apostar" ao lado; o prêmio do próximo concurso e o dia do sorteio num quadro (com a
 * estrelinha do concurso em destaque); depois o último resultado, a premiação, as regras e as
 * probabilidades.
 */
@Composable
fun ModalidadeTela(estado: Estado, m: Modalidade, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }
    val item: Indices.Item? = itens[m]
    val u = item?.ultimo
    val cor = corModalidade(m.cor)
    val hoje = LocalDate.now()
    val emDestaque = indices.destaque(itens)?.modalidade == m

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Emblema, nome e chance do prêmio principal, com o "Apostar" ao lado do nome.
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemaModalidade(m, item?.proximoNumero, tamanho = 68.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TextoQueCabe(
                    m.nome,
                    MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 40.sp),
                    peso = FontWeight.Bold,
                    menor = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Probabilidades.faixas(m).firstOrNull()?.let {
                    Text(Probabilidades.texto(it.umEm), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(10.dp))
            Button(
                // A aposta em andamento da modalidade, ou uma nova (com o volante vazio).
                onClick = { abrir(rotaDaAposta(estado, m)) },
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            ) { Text("Apostar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }

        // O prêmio do próximo concurso (de ponta a ponta) e o dia do sorteio, juntos num quadro.
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = if (emDestaque) BorderStroke(2.dp, Dourado) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (emDestaque) {
                    TituloDestaque()
                    Spacer(Modifier.height(4.dp))
                }
                if (u == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp)) {
                        if (atualizando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            if (atualizando) "  Consultando a Caixa..." else "Sem dados da Caixa agora.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    TextoQueCabe(
                        u.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                        MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 70.sp),
                        peso = FontWeight.Bold,
                        menor = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    u.proximaData?.let { RotuloValor("Sorteio", Datas.relativa(it, hoje)) }
                }
            }
        }

        // Último resultado.
        if (u != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val principal = u.faixas.minByOrNull { it.faixa }
                    when {
                        u.acumulado -> Text("Acumulou!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        principal != null && principal.ganhadores > 0 -> Text("Saiu o prêmio principal!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        else -> Unit
                    }
                    Text(
                        "Concurso ${u.numero}" + (u.data?.let { " · ${Datas.relativa(it, hoje)}" } ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Resultado(m, u, cor)
                    if (u.faixas.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Premiação", style = MaterialTheme.typography.titleMedium)
                        u.faixas.sortedBy { it.faixa }.forEach { f ->
                            Column {
                                Text(
                                    f.descricao.ifBlank { "${f.faixa}ª faixa" } + if (f.ganhadores > 0 && f.valor > 0) " · ${Dinheiro.rs(f.valor)}" else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    when (f.ganhadores) {
                                        0 -> "Não houve ganhadores"
                                        1 -> "1 aposta ganhadora"
                                        else -> "${Dinheiro.formatar(f.ganhadores * 100L).substringBefore(",")} apostas ganhadoras"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp)) {
                Text("Regras", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                regras(m, estado).forEach { (r, v) -> LinhaValor(r, v) }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp)) {
                Text("Probabilidades", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Probabilidades.faixas(m).forEach { f -> LinhaValor(f.nome, Probabilidades.texto(f.umEm)) }
                Probabilidades.faixas(m).mapNotNull { it.observacao }.distinct().forEach { obs ->
                    Text("* $obs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** "Sorteio: Hoje": o rótulo e o valor na mesma cor (o valor em negrito). */
@Composable
private fun RotuloValor(rotulo: String, valor: String) {
    Text(
        buildAnnotatedString {
            append("$rotulo: ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(valor) }
        },
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bolas(numeros: List<String>, cor: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        numeros.forEach { n ->
            // As bolas sorteadas no mesmo estilo 3D dos emblemas.
            Box(
                Modifier.size(38.dp).bola3D(cor),
                contentAlignment = Alignment.Center,
            ) {
                Text(n, style = estiloNaBola(38.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Números sorteados do último concurso, no formato de cada modalidade. */
@Composable
private fun Resultado(m: Modalidade, c: Concurso, cor: Color) {
    when (m.formato) {
        Formato.NUMEROS -> {
            if (m == Modalidade.DUPLA_SENA && c.segundoSorteio.isNotEmpty()) {
                Text("1º sorteio", style = MaterialTheme.typography.labelLarge)
                Bolas(c.dezenas.map(m::rotulo), cor)
                Text("2º sorteio", style = MaterialTheme.typography.labelLarge)
                Bolas(c.segundoSorteio.map(m::rotulo), cor)
            } else {
                Bolas(c.dezenas.map(m::rotulo), cor)
            }
            if (c.trevos.isNotEmpty()) {
                Text("Trevos", style = MaterialTheme.typography.labelLarge)
                Bolas(c.trevos.map { it.toString() }, cor)
            }
            c.timeOuMes?.let {
                Text((if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte: " else "Time do Coração: ") + it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Formato.COLUNAS -> Bolas(c.dezenas.map { it.toString() }, cor)
        Formato.LOTECA -> c.jogosLoteca.forEach { j ->
            Text(
                "${j.ordem}. ${j.time1} ${j.gols1 ?: "-"} x ${j.gols2 ?: "-"} ${j.time2}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Formato.FEDERAL -> c.bilhetesFederal.forEachIndexed { i, b ->
            Text("${i + 1}º prêmio: $b", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

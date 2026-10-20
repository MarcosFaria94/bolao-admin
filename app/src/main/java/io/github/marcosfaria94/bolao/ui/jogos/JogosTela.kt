package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import io.github.marcosfaria94.bolao.ui.aposta.abrirOuCriarAposta
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import kotlinx.coroutines.launch

/** Jogos: as modalidades em ordem de k1 (melhor retorno matemático primeiro). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JogosTela(estado: Estado, abrirModalidade: (Modalidade) -> Unit) {
    val ctx = LocalContext.current
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    val falhou by indices.falhou.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }

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
            item {
                Text(
                    "Em ordem de k1: o prêmio líquido esperado de uma aposta simples no próximo concurso, dividido pelo preço. " +
                        "k2 é a parte do k1 que vem dos prêmios frequentes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (falhou && itens.values.all { it.ultimo == null }) {
                    Text(
                        "Não foi possível consultar a Caixa agora. Puxe a lista para baixo para tentar de novo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            items(indices.ordenados(itens), key = { it.modalidade.name }) { item ->
                CartaoIndice(item, estado, aoTocar = { abrirModalidade(item.modalidade) })
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

/** Detalhe de uma modalidade: odds, último resultado, premiação, regras e probabilidades. */
@Composable
fun ModalidadeTela(estado: Estado, m: Modalidade, abrirAposta: (String) -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }
    val item: Indices.Item? = itens[m]
    val u = item?.ultimo
    val cor = corModalidade(m.cor)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Nome (sem a cor da modalidade no texto: o selo leva a cor) e a chance do prêmio principal.
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeloModalidade(m, tamanho = 56)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(m.nome, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Probabilidades.faixas(m).firstOrNull()?.let {
                    Text(Probabilidades.texto(it.umEm), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Próximo concurso, com o botão de apostar de frente.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Próximo concurso", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (u == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        if (atualizando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            if (atualizando) "  Consultando a Caixa..." else "Sem dados da Caixa agora.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    Text(
                        u.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        listOfNotNull(item?.proximoNumero?.toString(), u.proximaData?.let { Datas.longa(it) }).joinToString(" · "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    escopo.launch {
                        try {
                            abrirAposta(abrirOuCriarAposta(ctx, estado, m))
                        } catch (t: Throwable) {
                            avisos.showSnackbar("Não foi possível começar a aposta: ${t.message}")
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 20.dp),
            ) { Text("APOSTAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
        if (item != null && (item.k != null || item.especial || item.odds != null)) {
            Text(linhaIndices(item), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "Concurso ${u.numero}" + (u.data?.let { " · ${Datas.longa(it)}" } ?: ""),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bolas(numeros: List<String>, cor: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        numeros.forEach { n ->
            Box(
                Modifier.size(38.dp).background(cor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
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

package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * As partes de uma tabela larga (desde a 0.28, os Detalhes dividem as tabelas em outras menores, que cabem na
 * tela): [total] colunas em grupos de no máximo [maximo], do mesmo tamanho ou quase (4 = 2 + 2; 5 = 3 + 2;
 * 7 = 3 + 2 + 2). Cada parte é a faixa de índices das colunas.
 */
fun partesDaTabela(total: Int, maximo: Int = COLUNAS_POR_TABELA): List<IntRange> =
    if (total <= 0) emptyList() else partesIguais(total, (total + maximo - 1) / maximo)

/** [total] colunas em [quantas] partes do mesmo tamanho ou quase (as primeiras com uma a mais). */
private fun partesIguais(total: Int, quantas: Int): List<IntRange> {
    val base = total / quantas
    val resto = total % quantas
    var inicio = 0
    return (0 until quantas).map { i ->
        val tamanho = base + if (i < resto) 1 else 0
        (inicio until inicio + tamanho).also { inicio += tamanho }
    }
}

/**
 * As partes de uma tabela que cabem na largura [disponivel] (desde a 0.28; com a letra grande do celular, as
 * tabelas se dividem mais): o menor número de tabelas, com as colunas divididas por igual ([partesDaTabela]),
 * em que cada uma cabe: a primeira coluna ([primeira]), as colunas da parte ([larguras]) e, por cima delas, o
 * título do grupo ([titulo]; 0 = sem). No máximo [maximo] colunas por tabela; uma coluna que sozinha não cabe
 * fica numa tabela só dela (que rola para os lados). As medidas em qualquer unidade (a mesma para todas).
 */
fun partesQueCabem(larguras: List<Float>, primeira: Float, titulo: Float, disponivel: Float, maximo: Int = COLUNAS_POR_TABELA): List<IntRange> {
    val n = larguras.size
    if (n == 0) return emptyList()
    fun cabe(p: IntRange): Boolean = primeira + maxOf(p.sumOf { larguras[it].toDouble() }.toFloat(), titulo) <= disponivel + 0.5f
    for (k in (n + maximo - 1) / maximo..n) {
        val partes = partesIguais(n, k)
        if (partes.all(::cabe)) return partes
    }
    return partesIguais(n, n)
}

/**
 * Uma tabela dividida em tabelas menores que cabem na largura da tela ([partesQueCabem]; desde a 0.28), uma
 * embaixo da outra, todas com a primeira coluna ([primeira]); com [grupo], o título das colunas ("Probabilidade
 * de acerto (1 em)") fica por cima, uma vez só (pode quebrar a linha: não alarga as tabelas). A letra das
 * tabelas cresce com a do celular só até [ESCALA_MAXIMA_DA_TABELA], para as colunas caberem na tela.
 */
@Composable
fun TabelasDivididas(
    primeira: String,
    grupo: String?,
    colunas: List<String>,
    linhas: List<Pair<String, List<String>>>,
    modifier: Modifier = Modifier,
) {
    LetraDaTabela {
        val medidor = rememberTextMeasurer()
        val densidade = LocalDensity.current
        val celula = MaterialTheme.typography.bodyMedium
        val cabecalho = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        val folga = with(densidade) { (FOLGA_DA_CELULA * 2).toPx() }
        fun largura(texto: String, negrito: Boolean): Float =
            medidor.measure(texto, if (negrito) cabecalho else celula, maxLines = 1, softWrap = false).size.width.toFloat()
        val medidas = remember(primeira, colunas, linhas, celula, cabecalho, densidade) {
            val l0 = maxOf(largura(primeira, true), linhas.maxOfOrNull { largura(it.first, true) } ?: 0f) + folga
            val ls = colunas.mapIndexed { j, c -> maxOf(largura(c, true), linhas.maxOfOrNull { largura(it.second.getOrElse(j) { "" }, false) } ?: 0f) + folga }
            l0 to ls
        }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (grupo != null) {
                Text(grupo, style = cabecalho, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BoxWithConstraints {
                val disponivel = with(densidade) { maxWidth.toPx() }
                val partes = partesQueCabem(medidas.second, medidas.first, 0f, disponivel)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    partes.forEach { parte ->
                        TabelaRolavel(
                            primeira = primeira,
                            grupos = emptyList(),
                            colunas = colunas.slice(parte),
                            linhas = linhas.map { (rotulo, valores) -> rotulo to parte.map { valores.getOrElse(it) { "" } } },
                        )
                    }
                }
            }
        }
    }
}

/** Até quanto a letra das tabelas dos Detalhes cresce com a letra grande do celular (desde a 0.28). */
const val ESCALA_MAXIMA_DA_TABELA = 1.1f

/** A letra das tabelas: a do celular, até [ESCALA_MAXIMA_DA_TABELA]. */
@Composable
private fun LetraDaTabela(conteudo: @Composable () -> Unit) {
    val d = LocalDensity.current
    if (d.fontScale <= ESCALA_MAXIMA_DA_TABELA) {
        conteudo()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(d.density, ESCALA_MAXIMA_DA_TABELA), content = conteudo)
    }
}

/** O espaço de cada lado do texto nas células das tabelas. */
private val FOLGA_DA_CELULA = 8.dp

/** Quantas colunas de valores (além da primeira) cada tabela dos Detalhes tem, no máximo (desde a 0.28). */
const val COLUNAS_POR_TABELA = 3

/**
 * A rolagem para os lados da tabela não passa adiante (desde a 0.28): no fim da tabela, o resto do arrasto
 * não vai para a página, que antes saía dos Detalhes (ou voltava para o volante) quando o dedo seguia
 * arrastando a tabela.
 */
private val SemPassarAdiante = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
}

/**
 * Uma tabela como as do site da Caixa (desde a 0.27, nos Detalhes do jogo): a primeira coluna ([primeira]:
 * quantos números foram jogados) e as outras, com um cabeçalho em dois andares quando há [grupos] (título e
 * quantas colunas ele cobre; "" = sem título) e o nome de cada uma das [colunas]. Cada linha: o rótulo da
 * primeira coluna e os valores das outras. A largura de cada coluna é a do texto mais comprido dela; as
 * linhas são listradas para ler melhor.
 *
 * Desde a 0.28 os Detalhes dividem as tabelas em outras menores ([partesDaTabela]): a tabela ocupa a largura
 * toda (as colunas crescem por igual) e só rola para os lados quando não cabe (letra muito grande), com a
 * primeira coluna parada e sem levar a página junto ([SemPassarAdiante]).
 */
@Composable
fun TabelaRolavel(
    primeira: String,
    grupos: List<Pair<String, Int>>,
    colunas: List<String>,
    linhas: List<Pair<String, List<String>>>,
    modifier: Modifier = Modifier,
) {
    val medidor = rememberTextMeasurer()
    val densidade = LocalDensity.current
    val celula = MaterialTheme.typography.bodyMedium
    val cabecalho = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    val folga = FOLGA_DA_CELULA
    fun largura(texto: String, negrito: Boolean): Dp = with(densidade) {
        medidor.measure(texto, if (negrito) cabecalho else celula, maxLines = 1, softWrap = false).size.width.toDp()
    }
    // As medidas: a largura de cada coluna e a altura das linhas (acompanham a letra do celular).
    val (naturalPrimeira, naturais) = remember(primeira, grupos, colunas, linhas, celula, cabecalho) {
        val l0 = maxOf(largura(primeira, true), linhas.maxOfOrNull { largura(it.first, true) } ?: 0.dp) + folga * 2
        val ls = colunas.mapIndexed { j, c ->
            maxOf(largura(c, true), linhas.maxOfOrNull { largura(it.second.getOrElse(j) { "" }, false) } ?: 0.dp) + folga * 2
        }.toMutableList()
        // O título de um grupo mais largo que as colunas dele: as colunas do grupo crescem por igual.
        var inicio = 0
        grupos.forEach { (titulo, quantas) ->
            val fim = minOf(inicio + quantas, ls.size)
            if (titulo.isNotBlank() && fim > inicio) {
                val soma = ls.subList(inicio, fim).fold(0.dp) { a, x -> a + x }
                val precisa = largura(titulo, true) + folga * 2
                if (precisa > soma) {
                    val extra = (precisa - soma) / (fim - inicio)
                    for (k in inicio until fim) ls[k] = ls[k] + extra
                }
            }
            inicio = fim
        }
        l0 to ls.toList()
    }
    val alturaLinha = remember(celula) { with(densidade) { medidor.measure("0", celula).size.height.toDp() } + 14.dp }
    val alturaCabecalho = remember(cabecalho) { with(densidade) { medidor.measure("A", cabecalho).size.height.toDp() } + 14.dp }
    val comGrupos = grupos.any { it.first.isNotBlank() }
    val fundoCabecalho = MaterialTheme.colorScheme.surfaceContainerHighest
    val listra = MaterialTheme.colorScheme.surfaceContainerHigh
    fun fundoDa(i: Int): Color = if (i % 2 == 1) listra else Color.Transparent
    val linha = MaterialTheme.colorScheme.outlineVariant

    BoxWithConstraints(modifier) {
        // Cabendo, a tabela ocupa a largura toda: a sobra vai para todas as colunas por igual.
        val natural = naturais.fold(naturalPrimeira) { s, x -> s + x }
        val cabe = natural <= maxWidth
        val sobra = if (cabe) (maxWidth - natural) / (naturais.size + 1) else 0.dp
        val larguraPrimeira = naturalPrimeira + sobra
        val larguras = naturais.map { it + sobra }
        Row(Modifier.clip(RoundedCornerShape(10.dp))) {
            // A primeira coluna, parada.
            Column(Modifier.width(larguraPrimeira)) {
                if (comGrupos) Box(Modifier.width(larguraPrimeira).height(alturaCabecalho).background(fundoCabecalho))
                Box(Modifier.width(larguraPrimeira).height(alturaCabecalho).background(fundoCabecalho), contentAlignment = Alignment.Center) {
                    Text(primeira, style = cabecalho, maxLines = 1, softWrap = false)
                }
                HorizontalDivider(color = linha)
                linhas.forEachIndexed { i, (rotulo, _) ->
                    Box(Modifier.width(larguraPrimeira).height(alturaLinha).background(fundoDa(i)), contentAlignment = Alignment.Center) {
                        Text(rotulo, style = celula, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
            // As outras (rolando para os lados só quando a tabela não cabe).
            val rolagem = if (cabe) Modifier else Modifier.nestedScroll(SemPassarAdiante).horizontalScroll(rememberScrollState())
            Box(Modifier.weight(1f).then(rolagem)) {
                Column {
                    if (comGrupos) {
                        Row {
                            var j = 0
                            grupos.forEach { (titulo, quantas) ->
                                val w = larguras.subList(j, minOf(j + quantas, larguras.size)).fold(0.dp) { s, x -> s + x }
                                j += quantas
                                Box(Modifier.width(w).height(alturaCabecalho).background(fundoCabecalho), contentAlignment = Alignment.CenterStart) {
                                    Text(
                                        titulo,
                                        style = cabecalho,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = folga),
                                    )
                                }
                            }
                        }
                    }
                    Row {
                        colunas.forEachIndexed { j, c ->
                            Box(Modifier.width(larguras[j]).height(alturaCabecalho).background(fundoCabecalho), contentAlignment = Alignment.Center) {
                                Text(c, style = cabecalho, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.width(larguras.fold(0.dp) { s, x -> s + x }), color = linha)
                    linhas.forEachIndexed { i, (_, valores) ->
                        Row(Modifier.background(fundoDa(i))) {
                            colunas.indices.forEach { j ->
                                Box(Modifier.width(larguras[j]).height(alturaLinha), contentAlignment = Alignment.Center) {
                                    Text(valores.getOrElse(j) { "" }, style = celula, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

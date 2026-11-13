package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

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

/**
 * Uma forma de desenhar a tabela (desde a 0.30): a [escala] da letra (1 = a das tabelas), a [folga] de cada lado do
 * texto nas células e se os títulos das colunas podem [quebrar] em duas linhas.
 */
data class Tentativa(val escala: Float, val folga: Dp, val quebrar: Boolean)

/**
 * As formas de caber na largura, na ordem (desde a 0.30; na 0.29 a tabela rolava para os lados por pouco, com
 * meia coluna escondida): como sempre; os títulos das colunas em duas linhas ("Qtd de / jogos"); menos espaço nas
 * células; a letra um pouco menor, até 85%. A primeira que cabe é a usada.
 */
val TENTATIVAS_DA_TABELA: List<Tentativa> = listOf(
    Tentativa(1f, 8.dp, quebrar = false),
    Tentativa(1f, 8.dp, quebrar = true),
    Tentativa(1f, 5.dp, quebrar = true),
    Tentativa(0.95f, 5.dp, quebrar = true),
    Tentativa(0.9f, 4.dp, quebrar = true),
    Tentativa(0.85f, 4.dp, quebrar = true),
)

/** Sem caber de nenhum jeito: a tabela rola para os lados, com a letra de sempre e os títulos em duas linhas. */
val TABELA_ROLANDO = Tentativa(1f, 8.dp, quebrar = true)

/** A primeira das [tentativas] em que a tabela cabe nos [disponivel] px ([total]: a largura dela, em px), ou null. */
fun primeiraQueCabe(tentativas: List<Tentativa>, disponivel: Int, total: (Tentativa) -> Int): Tentativa? =
    tentativas.firstOrNull { total(it) <= disponivel }

/**
 * A largura de um título em até duas linhas ([largura] mede um texto numa linha): quebrando num dos espaços, a
 * divisão em que a linha mais comprida fica mais curta; sem espaço (ou se não ajudar), a linha inteira.
 */
fun larguraEmDuasLinhas(texto: String, largura: (String) -> Int): Int {
    val inteira = largura(texto)
    val espacos = texto.indices.filter { texto[it] == ' ' }
    if (espacos.isEmpty()) return inteira
    // Um pixel a mais: a quebra do texto na tela não pode ficar sem lugar por arredondamento.
    val melhor = espacos.minOf { i -> maxOf(largura(texto.substring(0, i)), largura(texto.substring(i + 1))) } + 1
    return minOf(inteira, melhor)
}

/**
 * As larguras (px) das colunas de uma tabela que cabe, ocupando os [disponivel] px: a sobra vai para todas por
 * igual e o resto da divisão, para a última (a soma dá exatamente a largura: nada sobra para rolar, que na 0.29
 * deixava a borda direita esmaecida como se a tabela continuasse).
 */
fun distribuirSobra(larguras: List<Int>, disponivel: Int): List<Int> {
    val sobra = disponivel - larguras.sum()
    if (sobra <= 0 || larguras.isEmpty()) return larguras
    val cada = sobra / larguras.size
    return larguras.mapIndexed { i, l -> l + cada + if (i == larguras.lastIndex) sobra - cada * larguras.size else 0 }
}

/**
 * A tabela pronta para desenhar: as letras ([celula], [cabecalho]), a [folga] das células, as larguras em px (a
 * [primeira] coluna e as [larguras] das outras), as alturas em px (o andar dos grupos, o cabeçalho e cada linha) e
 * se ela [cabe] na largura (senão, rola).
 */
private class PlanoDaTabela(
    val celula: TextStyle,
    val cabecalho: TextStyle,
    val folga: Dp,
    val primeira: Int,
    val larguras: List<Int>,
    val alturaGrupos: Int,
    val alturaCabecalho: Int,
    val alturaLinha: Int,
    val cabe: Boolean,
)

/** A letra [base] na [escala] (o tamanho e a altura da linha). */
private fun TextStyle.naEscala(escala: Float): TextStyle =
    if (escala == 1f) this else copy(fontSize = fontSize * escala, lineHeight = if (lineHeight.isSpecified) lineHeight * escala else lineHeight)

/**
 * Mede a tabela e escolhe como desenhar ([TENTATIVAS_DA_TABELA]; sem caber, [TABELA_ROLANDO]): a largura de cada
 * coluna é a do texto mais comprido dela (o título, em uma ou duas linhas) mais a folga dos dois lados; o título de
 * um grupo mais largo que as colunas dele faz elas crescerem por igual.
 */
private fun planejar(
    medidor: TextMeasurer,
    d: Density,
    celulaBase: TextStyle,
    cabecalhoBase: TextStyle,
    primeira: String,
    grupos: List<Pair<String, Int>>,
    colunas: List<String>,
    linhas: List<Pair<String, List<String>>>,
    disponivel: Int,
    ordenaveis: Set<Int> = emptySet(),
    extraDoTitulo: Int = 0,
): PlanoDaTabela {
    fun larguraDe(t: String, e: TextStyle): Int = medidor.measure(t, e, maxLines = 1, softWrap = false).size.width
    class Medidas(val t: Tentativa, val celula: TextStyle, val cabecalho: TextStyle, val folga: Int, val daPrimeira: Int, val larguras: List<Int>) {
        val total: Int get() = daPrimeira + larguras.sum()
    }
    val cache = HashMap<Tentativa, Medidas>()
    fun medir(t: Tentativa): Medidas = cache.getOrPut(t) {
        val cel = celulaBase.naEscala(t.escala)
        val cab = cabecalhoBase.naEscala(t.escala)
        val negrito = cel.copy(fontWeight = FontWeight.Bold)
        val f = with(d) { t.folga.roundToPx() } * 2
        fun titulo(x: String): Int = if (t.quebrar) larguraEmDuasLinhas(x) { larguraDe(it, cab) } else larguraDe(x, cab)
        val l0 = maxOf(titulo(primeira), linhas.maxOfOrNull { larguraDe(it.first, negrito) } ?: 0) + f
        // O título das colunas que se ordenam tem a setinha ao lado ([extraDoTitulo]).
        fun extra(j: Int): Int = if (j in ordenaveis) extraDoTitulo else 0
        val ls = colunas.mapIndexed { j, c ->
            maxOf(titulo(c) + extra(j), linhas.maxOfOrNull { larguraDe(it.second.getOrElse(j) { "" }, cel) } ?: 0) + f
        }.toMutableList()
        var inicio = 0
        grupos.forEach { (g, quantas) ->
            val fim = minOf(inicio + quantas, ls.size)
            if (g.isNotBlank() && fim > inicio) {
                val falta = titulo(g) + f - ls.subList(inicio, fim).sum()
                if (falta > 0) {
                    val n = fim - inicio
                    for (k in inicio until fim) ls[k] += (falta + n - 1) / n
                }
            }
            inicio = fim
        }
        Medidas(t, cel, cab, f, l0, ls)
    }
    val escolhida = primeiraQueCabe(TENTATIVAS_DA_TABELA, disponivel) { medir(it).total }
    val m = medir(escolhida ?: TABELA_ROLANDO)
    val cabe = escolhida != null
    // Cabendo, a tabela ocupa a largura toda (a sobra vai para as colunas por igual).
    val todas = if (cabe) distribuirSobra(listOf(m.daPrimeira) + m.larguras, disponivel) else listOf(m.daPrimeira) + m.larguras
    val margem = with(d) { 14.dp.roundToPx() }
    fun alturaDe(t: String, largura: Int): Int =
        medidor.measure(t, m.cabecalho, maxLines = 2, constraints = Constraints(maxWidth = maxOf(1, largura - m.folga))).size.height
    val alturaCabecalho = maxOf(
        alturaDe(primeira, todas[0]),
        colunas.indices.maxOfOrNull { j -> alturaDe(colunas[j], todas[j + 1] - if (j in ordenaveis) extraDoTitulo else 0) } ?: 0,
    ) + margem
    var alturaGrupos = 0
    if (grupos.any { it.first.isNotBlank() }) {
        var j = 0
        grupos.forEach { (g, quantas) ->
            val w = todas.subList(1 + j, minOf(1 + j + quantas, todas.size)).sum()
            j += quantas
            if (g.isNotBlank()) alturaGrupos = maxOf(alturaGrupos, alturaDe(g, w))
        }
        alturaGrupos += margem
    }
    val alturaLinha = medidor.measure("0", m.celula).size.height + margem
    return PlanoDaTabela(m.celula, m.cabecalho, m.t.folga, todas[0], todas.drop(1), alturaGrupos, alturaCabecalho, alturaLinha, cabe)
}

/**
 * A rolagem para os lados da tabela não passa adiante (desde a 0.28): no fim da tabela, o resto do arrasto
 * (e do arremesso) não vai para a página, que antes saía dos Detalhes (ou voltava para o volante) quando o
 * dedo seguia arrastando a tabela.
 */
private val SemPassarAdiante = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = Offset(available.x, 0f)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity(available.x, 0f)
}

/**
 * O arrasto para os lados em cima de uma tabela que rola fica com ela (desde a 0.29): mesmo onde ela não rola (a
 * primeira coluna), deslizar o dedo para os lados não leva a página junto. Desde a 0.30, só nas tabelas que rolam:
 * as que cabem inteiras deixam o dedo passar a página, como no resto da tela. O arrasto para cima e para baixo
 * continua rolando a página.
 */
private fun Modifier.prenderArrastoLateral(): Modifier = pointerInput(Unit) {
    detectHorizontalDragGestures { mudanca, _ -> mudanca.consume() }
}

/**
 * Uma tabela como as do site da Caixa (desde a 0.27, nos Detalhes do jogo): a primeira coluna ([primeira]:
 * quantos números foram jogados) e as outras, com um cabeçalho em dois andares quando há [grupos] (título e
 * quantas colunas ele cobre; "" = sem título) e o nome de cada uma das [colunas]. Cada linha: o rótulo da
 * primeira coluna e os valores das outras. A largura de cada coluna é a do texto mais comprido dela; as
 * linhas são listradas para ler melhor.
 *
 * A tabela ocupa a largura toda. Desde a 0.30, antes de rolar ela tenta caber ([TENTATIVAS_DA_TABELA]: os títulos
 * em duas linhas, menos espaço nas células, a letra até 85%); cabendo, fica parada, sem a borda esmaecida e sem
 * prender o arrasto para os lados (o dedo passa a página). Só quando não cabe de jeito nenhum ela rola para os
 * lados, com a primeira coluna parada, a borda direita esmaecida enquanto há colunas escondidas e o arrasto preso
 * nela ([prenderArrastoLateral], [SemPassarAdiante]). A letra cresce com a do celular só até
 * [ESCALA_MAXIMA_DA_TABELA].
 *
 * Desde a 0.32, com [ordem] (o valor de cada célula para ordenar; null = "—", sempre no fim), tocar no título de
 * uma coluna ordena as linhas por ela (do menor para o maior; tocar de novo, ao contrário), com a setinha no
 * título; tocar no título da primeira coluna volta à ordem de sempre ([OrdemDaTabela]).
 */
@Composable
fun TabelaRolavel(
    primeira: String,
    grupos: List<Pair<String, Int>>,
    colunas: List<String>,
    linhas: List<Pair<String, List<String>>>,
    modifier: Modifier = Modifier,
    ordem: List<List<Long?>>? = null,
) = LetraDaTabela {
    val medidor = rememberTextMeasurer()
    val densidade = LocalDensity.current
    val celulaBase = MaterialTheme.typography.bodyMedium
    val cabecalhoBase = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    val comGrupos = grupos.any { it.first.isNotBlank() }
    val fundoCabecalho = MaterialTheme.colorScheme.surfaceContainerHighest
    val listra = MaterialTheme.colorScheme.surfaceContainerHigh
    fun fundoDa(i: Int): Color = if (i % 2 == 1) listra else Color.Transparent
    val linha = MaterialTheme.colorScheme.outlineVariant

    // A ordenação (desde a 0.32): a coluna escolhida (-1 = a ordem de sempre) e o sentido.
    val ordenaveis = if (ordem != null) colunas.indices.toSet() else emptySet()
    var ordenacao by rememberSaveable(colunas) { mutableStateOf(OrdemDaTabela()) }
    val vistas = remember(linhas, ordem, ordenacao) { ordenacao.linhas(linhas.size, ordem) }
    val extraDoTitulo = with(densidade) { (TAMANHO_DA_SETINHA + 2.dp).roundToPx() }

    BoxWithConstraints(modifier) {
        val disponivel = constraints.maxWidth
        val p = remember(primeira, grupos, colunas, linhas, celulaBase, cabecalhoBase, densidade, disponivel, ordenaveis) {
            planejar(medidor, densidade, celulaBase, cabecalhoBase, primeira, grupos, colunas, linhas, disponivel, ordenaveis, extraDoTitulo)
        }
        fun Int.emDp(): Dp = with(densidade) { this@emDp.toDp() }
        val larguraPrimeira = p.primeira.emDp()
        val larguras = p.larguras.map { it.emDp() }
        val alturaLinha = p.alturaLinha.emDp()
        val alturaCabecalho = p.alturaCabecalho.emDp()
        val alturaGrupos = p.alturaGrupos.emDp()
        val folga = p.folga
        val rolagem = rememberScrollState()
        Row(Modifier.clip(RoundedCornerShape(10.dp)).then(if (p.cabe) Modifier else Modifier.prenderArrastoLateral())) {
            // A primeira coluna, parada.
            Column(Modifier.width(larguraPrimeira)) {
                if (comGrupos) Box(Modifier.width(larguraPrimeira).height(alturaGrupos).background(fundoCabecalho))
                Box(
                    Modifier
                        .width(larguraPrimeira)
                        .height(alturaCabecalho)
                        .background(fundoCabecalho)
                        .then(
                            if (ordem != null) {
                                Modifier.clickable(onClickLabel = ORDEM_DE_SEMPRE) { ordenacao = OrdemDaTabela() }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(primeira, style = p.cabecalho, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = folga))
                }
                HorizontalDivider(color = linha)
                vistas.forEachIndexed { i, original ->
                    val rotulo = linhas[original].first
                    Box(Modifier.width(larguraPrimeira).height(alturaLinha).background(fundoDa(i)), contentAlignment = Alignment.Center) {
                        Text(rotulo, style = p.celula, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
            // As outras: só rolam para os lados quando a tabela não cabe (sem levar a página junto, nem no fim); aí,
            // enquanto há colunas escondidas à direita, a borda direita esmaece (mostra que a tabela continua).
            val fundo = MaterialTheme.colorScheme.surfaceContainerLow
            val esmaecer = with(densidade) { 28.dp.toPx() }
            Box(
                Modifier
                    .weight(1f)
                    .then(
                        if (p.cabe) {
                            Modifier
                        } else {
                            Modifier
                                .drawWithContent {
                                    drawContent()
                                    if (rolagem.canScrollForward) {
                                        drawRect(
                                            Brush.horizontalGradient(listOf(fundo.copy(alpha = 0f), fundo), startX = size.width - esmaecer, endX = size.width),
                                            topLeft = Offset(size.width - esmaecer, 0f),
                                        )
                                    }
                                }
                                .nestedScroll(SemPassarAdiante)
                                .horizontalScroll(rolagem)
                        },
                    ),
            ) {
                Column {
                    if (comGrupos) {
                        Row {
                            var j = 0
                            grupos.forEach { (titulo, quantas) ->
                                val w = larguras.subList(j, minOf(j + quantas, larguras.size)).fold(0.dp) { s, x -> s + x }
                                j += quantas
                                Box(Modifier.width(w).height(alturaGrupos).background(fundoCabecalho), contentAlignment = Alignment.CenterStart) {
                                    Text(
                                        titulo,
                                        style = p.cabecalho,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = folga),
                                    )
                                }
                            }
                        }
                    }
                    Row {
                        colunas.forEachIndexed { j, c ->
                            Box(
                                Modifier
                                    .width(larguras[j])
                                    .height(alturaCabecalho)
                                    .background(fundoCabecalho)
                                    .then(
                                        if (j in ordenaveis) {
                                            Modifier.clickable(onClickLabel = ordenarPor(c)) { ordenacao = ordenacao.tocar(j) }
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (j in ordenaveis) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = folga)) {
                                        Text(c, style = p.cabecalho, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                                        Spacer(Modifier.width(2.dp))
                                        Icon(
                                            when {
                                                ordenacao.coluna != j -> Icons.Outlined.UnfoldMore
                                                ordenacao.crescente -> Icons.Outlined.ArrowUpward
                                                else -> Icons.Outlined.ArrowDownward
                                            },
                                            contentDescription = null,
                                            tint = if (ordenacao.coluna == j) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(TAMANHO_DA_SETINHA),
                                        )
                                    }
                                } else {
                                    Text(c, style = p.cabecalho, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = folga))
                                }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.width(larguras.fold(0.dp) { s, x -> s + x }), color = linha)
                    vistas.forEachIndexed { i, original ->
                        val valores = linhas[original].second
                        Row(Modifier.background(fundoDa(i))) {
                            colunas.indices.forEach { j ->
                                Box(Modifier.width(larguras[j]).height(alturaLinha), contentAlignment = Alignment.Center) {
                                    Text(valores.getOrElse(j) { "" }, style = p.celula, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** O tamanho da setinha da ordenação no título da coluna (desde a 0.32). */
private val TAMANHO_DA_SETINHA = 14.dp

/** O que tocar no título da primeira coluna faz (com a tabela ordenada por outra coluna). */
const val ORDEM_DE_SEMPRE = "Voltar à ordem de sempre"

/** "Ordenar por Preço": o que tocar no título de uma coluna faz. */
fun ordenarPor(coluna: String): String = "Ordenar por $coluna"

/**
 * A ordem das linhas de uma tabela que se ordena (desde a 0.32): a [coluna] escolhida (-1 = a ordem de sempre) e se
 * é do menor para o maior ([crescente]).
 */
data class OrdemDaTabela(val coluna: Int = -1, val crescente: Boolean = true) : java.io.Serializable {
    /** Tocar no título da coluna [j]: ordena por ela (do menor para o maior); de novo, inverte. */
    fun tocar(j: Int): OrdemDaTabela = if (coluna == j) copy(crescente = !crescente) else OrdemDaTabela(j, true)

    /**
     * Os índices das [quantas] linhas na ordem da tabela: a de sempre ou pelos valores da coluna em [ordem] (os
     * empates ficam na ordem de sempre; o "—", null, sempre no fim).
     */
    fun linhas(quantas: Int, ordem: List<List<Long?>>?): List<Int> {
        val todas = (0 until quantas).toList()
        if (coluna < 0 || ordem == null) return todas
        val com = todas.filter { ordem.getOrNull(it)?.getOrNull(coluna) != null }
        val sem = todas - com.toSet()
        val porValor = compareBy<Int> { ordem[it][coluna]!! }
        return com.sortedWith(if (crescente) porValor else porValor.reversed()) + sem
    }
}

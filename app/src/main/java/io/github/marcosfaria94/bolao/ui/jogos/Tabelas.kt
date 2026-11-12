package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Uma tabela como as do site da Caixa (desde a 0.27, nos Detalhes do jogo): a primeira coluna ([primeira]:
 * quantos números foram jogados) fica parada e as outras rolam para os lados, com um cabeçalho em dois
 * andares: os [grupos] (título e quantas colunas ele cobre; "" = sem título) e o nome de cada uma das
 * [colunas]. Cada linha: o rótulo da primeira coluna e os valores das outras. A largura de cada coluna é a
 * do texto mais comprido dela; as linhas são listradas para ler melhor.
 */
@Composable
fun TabelaRolavel(
    primeira: String,
    grupos: List<Pair<String, Int>>,
    colunas: List<String>,
    linhas: List<Pair<String, List<String>>>,
) {
    val medidor = rememberTextMeasurer()
    val densidade = LocalDensity.current
    val celula = MaterialTheme.typography.bodyMedium
    val cabecalho = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
    val folga = 10.dp
    fun largura(texto: String, negrito: Boolean): Dp = with(densidade) {
        medidor.measure(texto, if (negrito) cabecalho else celula, maxLines = 1, softWrap = false).size.width.toDp()
    }
    // As medidas: a largura de cada coluna e a altura das linhas (acompanham a letra do celular).
    val (larguraPrimeira, larguras) = remember(primeira, grupos, colunas, linhas, celula, cabecalho) {
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
        // As outras, rolando para os lados.
        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
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

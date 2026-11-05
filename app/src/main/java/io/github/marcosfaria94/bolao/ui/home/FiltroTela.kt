package io.github.marcosfaria94.bolao.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.FiltroHistorico
import io.github.marcosfaria94.bolao.ui.comum.FiltroHome
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.corCirculo
import io.github.marcosfaria94.bolao.ui.comum.deslizarParaFechar
import io.github.marcosfaria94.bolao.ui.comum.mesesComDados
import io.github.marcosfaria94.bolao.ui.comum.movimentosDoHistorico
import io.github.marcosfaria94.bolao.ui.comum.tiposDoFiltro
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import java.time.LocalDate
import java.time.YearMonth

/** Colunas da grade dos tipos e da grade dos meses. */
private const val COLUNAS_TIPOS = 3
private const val COLUNAS_MESES = 3

/** Nome curto do mês, no quadrinho ("Nov"): o ano fica no título de cada ano. */
private val MESES = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

/**
 * "Filtrar" o Histórico da Home: os tipos e os meses, vários de cada vez (sem nada marcado vale
 * "Tudo"), em quadrinhos grandes que se marcam tocando (até a 0.15 eram botões pequenos). Os tipos
 * numa grade de 3 colunas, cada um com o desenho do Histórico; os meses agrupados por ano, também
 * em 3 colunas, com "Todos os meses" em cima. Cada toque já vale; "Limpar filtro" volta ao padrão
 * (todos os tipos no mês atual) e "Ver N itens" (ou deslizar o dedo para o lado, ou puxar para
 * baixo) volta à Home.
 */
@Composable
fun FiltroTela(estado: Estado, filtro: FiltroHome, mudar: (FiltroHome) -> Unit, aoFechar: () -> Unit) {
    val hoje = LocalDate.now()
    val tipos = tiposDoFiltro(estado.individual)
    val meses = mesesComDados(estado.lancamentos.map { it.data }, hoje)
    val quantos = movimentosDoHistorico(estado, filtro).size

    Column(Modifier.fillMaxSize().deslizarParaFechar(aoFechar)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TituloDoFiltro("TIPO", primeiro = true)
            val opcoesDeTipo: List<Pair<FiltroHistorico?, Boolean>> = listOf(null to filtro.tipos.isEmpty()) + tipos.map { it to (it in filtro.tipos) }
            Grade(opcoesDeTipo, COLUNAS_TIPOS) { (t, marcado) ->
                Quadrinho(
                    marcado = marcado,
                    altura = 92.dp,
                    aoTocar = { mudar(if (t == null) filtro.copy(tipos = emptySet()) else filtro.comTipo(t, tipos)) },
                ) {
                    DesenhoDoTipo(t)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t?.rotulo ?: "Tudo",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 19.sp),
                        fontWeight = if (marcado) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            TituloDoFiltro("MÊS")
            Quadrinho(
                marcado = filtro.meses.isEmpty(),
                altura = 52.dp,
                modifier = Modifier.fillMaxWidth(),
                aoTocar = { mudar(filtro.copy(meses = emptySet())) },
            ) {
                Text(
                    "Todos os meses",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    fontWeight = if (filtro.meses.isEmpty()) FontWeight.Bold else FontWeight.SemiBold,
                )
            }
            meses.groupBy { it.year }.forEach { (ano, doAno) ->
                Text(
                    ano.toString(),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp),
                )
                Grade(doAno, COLUNAS_MESES) { m ->
                    val marcado = m in filtro.meses
                    Quadrinho(marcado = marcado, altura = 56.dp, descricao = Datas.mesAnoCurto(m), aoTocar = { mudar(filtro.comMes(m)) }) {
                        Text(
                            MESES[m.monthValue - 1],
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                            fontWeight = if (marcado) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        if (m == YearMonth.from(hoje)) {
                            Text("Este mês", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Toque para marcar quantos quiser. O filtro volta ao padrão (todos os tipos no mês atual) ao trocar de tela.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { mudar(FiltroHome.padrao(hoje)) },
                enabled = !filtro.ehPadrao(hoje),
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("Limpar filtro", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp)) }
            Button(onClick = aoFechar, modifier = Modifier.weight(1f).height(52.dp)) {
                Text(
                    if (quantos == 1) "Ver 1 item" else "Ver $quantos itens",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** "TIPO", "MÊS": como os títulos dos grupos dos Ajustes. */
@Composable
private fun TituloDoFiltro(nome: String, primeiro: Boolean = false) {
    Text(
        nome,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 1.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = if (primeiro) 6.dp else 24.dp, bottom = 10.dp),
    )
}

/** Os [itens] numa grade de [colunas] colunas (a última linha completa com espaços vazios). */
@Composable
private fun <T> Grade(itens: List<T>, colunas: Int, conteudo: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itens.chunked(colunas).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                linha.forEach { Box(Modifier.weight(1f)) { conteudo(it) } }
                repeat(colunas - linha.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Um quadrinho do filtro: marcado, fica com o fundo e o contorno da cor de destaque e o tique no
 * canto; desmarcado, só o contorno fino. Tocar marca ou desmarca.
 */
@Composable
private fun Quadrinho(
    marcado: Boolean,
    altura: Dp,
    aoTocar: () -> Unit,
    modifier: Modifier = Modifier,
    descricao: String? = null,
    conteudo: @Composable () -> Unit,
) {
    val forma = RoundedCornerShape(16.dp)
    val destaque = MaterialTheme.colorScheme.primary
    val borda = if (marcado) BorderStroke(2.dp, destaque) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Box(
        modifier
            .fillMaxWidth()
            .height(altura)
            .clip(forma)
            .background(if (marcado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .border(borda, forma)
            .semantics { selected = marcado }
            .clickable(role = Role.Checkbox, onClickLabel = descricao, onClick = aoTocar),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        ) { conteudo() }
        if (marcado) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(18.dp)
                    .background(destaque, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
            }
        }
    }
}

/** O desenho de cada tipo, o mesmo do Histórico (o trevo nas apostas; "Tudo", o infinito). */
@Composable
private fun DesenhoDoTipo(t: FiltroHistorico?) {
    val tamanho = 38.dp
    when (t) {
        null, FiltroHistorico.TUDO -> EmblemaIcone(Icons.Outlined.AllInclusive, corCirculo(), tamanho, corIcone = MaterialTheme.colorScheme.onSurfaceVariant)
        FiltroHistorico.APORTES -> EmblemaDesenho(Desenhos.Cofrinho, tamanho)
        FiltroHistorico.APOSTAS -> EmblemaIcone(Icones.TrevoCheio, corCirculo(), tamanho, corIcone = Dourado)
        FiltroHistorico.RATEIOS -> EmblemaDesenho(Desenhos.Moedas, tamanho)
        FiltroHistorico.PREMIOS -> EmblemaDesenho(Desenhos.Trofeu, tamanho)
        FiltroHistorico.EXTRATO -> EmblemaDesenho(Desenhos.Extrato, tamanho)
    }
}

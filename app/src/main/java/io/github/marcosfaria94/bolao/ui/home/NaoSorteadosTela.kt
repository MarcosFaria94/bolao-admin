package io.github.marcosfaria94.bolao.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.aposta.Situacao
import io.github.marcosfaria94.bolao.ui.aposta.situacaoDe
import io.github.marcosfaria94.bolao.ui.aposta.tituloDaAposta
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.Etiqueta
import io.github.marcosfaria94.bolao.ui.comum.EtiquetaSituacao
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.comum.TomEtiqueta
import io.github.marcosfaria94.bolao.ui.comum.bola3D
import io.github.marcosfaria94.bolao.ui.comum.deslizarParaFechar
import io.github.marcosfaria94.bolao.ui.comum.estiloNaBola
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import java.time.LocalDate

/** Os jogos de um mesmo concurso que ainda esperam o sorteio. */
data class GrupoNaoSorteado(val modalidade: Modalidade, val concurso: Int, val sorteio: LocalDate?, val apostas: List<Aposta>)

/**
 * As apostas registradas que ainda não foram sorteadas (inclusive as que esperam o comprovante),
 * agrupadas por modalidade, concurso e data do sorteio, o sorteio mais próximo primeiro (sem data
 * no fim). Dentro do grupo, na ordem de registro.
 */
fun jogosNaoSorteados(e: Estado, hoje: LocalDate): List<GrupoNaoSorteado> =
    e.apostas
        .filter { it.registrada && situacaoDe(it, hoje) == Situacao.AGUARDANDO }
        .groupBy { Triple(it.modalidade, it.concurso, it.dataSorteio) }
        .map { (k, lista) ->
            GrupoNaoSorteado(k.first, k.second, k.third, lista.sortedWith(compareBy<Aposta>({ it.dataAposta }, { it.criadaEm }, { it.numero })))
        }
        .sortedWith(
            compareBy<GrupoNaoSorteado> { it.sorteio == null }
                .thenBy { it.sorteio }
                .thenBy { it.modalidade.ordinal }
                .thenBy { it.concurso },
        )

/** Quantas apostas esperam o sorteio (o número no botão da Home). */
fun quantosNaoSorteados(e: Estado, hoje: LocalDate): Int = jogosNaoSorteados(e, hoje).sumOf { it.apostas.size }

/**
 * "Jogos não sorteados" (o botão ao lado do Histórico): cada concurso num cartão, com a modalidade,
 * o concurso, a data do sorteio e as dezenas escolhidas em cada aposta. Fecha pelo "X" ou
 * deslizando o dedo para qualquer lado (e volta para a Home, com o filtro do Histórico como estava).
 */
@Composable
fun NaoSorteadosTela(estado: Estado, abrir: (String) -> Unit, aoFechar: () -> Unit) {
    val hoje = LocalDate.now()
    val grupos = jogosNaoSorteados(estado, hoje)
    Box(Modifier.fillMaxSize().deslizarParaFechar(aoFechar)) {
        LazyColumn(contentPadding = PaddingTela, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            if (grupos.isEmpty()) {
                item(key = "vazio") {
                    TextoVazio("Nenhum jogo esperando o sorteio.", Modifier.padding(horizontal = 4.dp, vertical = 12.dp))
                }
            }
            items(grupos, key = { "${it.modalidade}-${it.concurso}-${it.sorteio}" }) { g ->
                CartaoDoConcurso(estado, g, hoje, abrir)
            }
        }
    }
}

/** Um concurso: a bola da modalidade, o nome, o concurso e o sorteio; embaixo, cada aposta com as dezenas. */
@Composable
private fun CartaoDoConcurso(estado: Estado, g: GrupoNaoSorteado, hoje: LocalDate, abrir: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmblemaModalidade(g.modalidade, g.concurso, tamanho = 50.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        g.modalidade.nome,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 24.sp),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (g.concurso > 0) "Concurso ${g.concurso}" else "Concurso a confirmar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        buildAnnotatedString {
                            append("Sorteio: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(dataDoSorteio(g.sorteio, hoje)) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            g.apostas.forEach { a ->
                HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { abrir("aposta/${a.id}") }
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tituloDaAposta(a), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold)
                        etiquetaDoAnexo(a)?.let { EtiquetaSituacao(it) }
                    }
                    NumerosEscolhidos(a)
                }
            }
        }
    }
}

/** "Hoje · 29/10/2026", "Amanhã · 30/10/2026", "02/11/2026" ou "a confirmar". */
private fun dataDoSorteio(d: LocalDate?, hoje: LocalDate): String {
    if (d == null) return "a confirmar"
    val r = Datas.relativa(d, hoje)
    val data = Datas.longa(d)
    return if (r == data) data else "$r · $data"
}

/** A aposta que ainda espera o comprovante da Caixa (ou a conferência dele). */
private fun etiquetaDoAnexo(a: Aposta): Etiqueta? = when {
    Operacoes.aguardandoComprovante(a) -> Etiqueta("Sem comprovante", TomEtiqueta.VERMELHO)
    Operacoes.aguardandoConferencia(a) -> Etiqueta("A conferir", TomEtiqueta.ROXO)
    else -> null
}

private val BOLA = 30.dp

/** As escolhas da aposta, no formato da modalidade: as dezenas (e os trevos, o time ou o mês), as colunas, os palpites ou o bilhete. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumerosEscolhidos(a: Aposta) {
    val m = a.modalidade
    val cor = corModalidade(m.cor)
    when (m.formato) {
        Formato.NUMEROS -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Bolas(a.numeros.sorted().map(m::rotulo), cor)
            if (a.trevos.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trevos  ", style = MaterialTheme.typography.labelLarge)
                    Bolas(a.trevos.sorted().map { it.toString() }, cor)
                }
            }
            a.extra?.takeIf { it.isNotBlank() }?.let {
                Text(
                    (if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte: " else "Time do Coração: ") + it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Formato.COLUNAS -> Bolas(a.numeros.map { it.toString() }, cor)
        Formato.LOTECA -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            a.palpitesLoteca.forEachIndexed { i, p ->
                Text(
                    "${i + 1}: $p",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        Formato.FEDERAL -> Text(
            "Bilhete ${a.bilheteFederal ?: "—"}" + (a.fracoesFederal?.let { if (it == 1) " · 1 fração" else " · $it frações" } ?: ""),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bolas(numeros: List<String>, cor: Color, tamanho: Dp = BOLA) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        numeros.forEach { n ->
            Box(Modifier.size(tamanho).bola3D(cor), contentAlignment = Alignment.Center) {
                Text(n, style = estiloNaBola(tamanho), fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

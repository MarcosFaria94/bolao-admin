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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import io.github.marcosfaria94.bolao.ui.comum.TomEtiqueta
import io.github.marcosfaria94.bolao.ui.comum.bola3D
import io.github.marcosfaria94.bolao.ui.comum.estiloNaBola
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import java.time.LocalDate

/** Os jogos de um mesmo concurso que ainda estão em andamento (antes do sorteio). */
data class GrupoEmAndamento(val modalidade: Modalidade, val concurso: Int, val sorteio: LocalDate?, val apostas: List<Aposta>)

/**
 * Os jogos em andamento: as apostas registradas que ainda não foram sorteadas (inclusive as que
 * esperam o comprovante) e as que ainda falta registrar (volante pronto), agrupadas por modalidade,
 * concurso e data do sorteio, o sorteio mais próximo primeiro (sem data no fim). Dentro do grupo, as
 * registradas na ordem de registro e, depois, as que falta registrar.
 */
fun jogosEmAndamento(e: Estado, hoje: LocalDate): List<GrupoEmAndamento> =
    e.apostas
        .filter { !it.registrada || situacaoDe(it, hoje) == Situacao.AGUARDANDO }
        .groupBy { Triple(it.modalidade, it.concurso, it.dataSorteio) }
        .map { (k, lista) ->
            GrupoEmAndamento(
                k.first, k.second, k.third,
                lista.sortedWith(compareBy<Aposta>({ !it.registrada }, { it.dataAposta }, { it.criadaEm }, { it.numero })),
            )
        }
        .sortedWith(
            compareBy<GrupoEmAndamento> { it.sorteio == null }
                .thenBy { it.sorteio }
                .thenBy { it.modalidade.ordinal }
                .thenBy { it.concurso },
        )

/** Quantos jogos estão em andamento. */
fun quantosEmAndamento(e: Estado, hoje: LocalDate): Int = jogosEmAndamento(e, hoje).sumOf { it.apostas.size }

/**
 * Um concurso dos jogos em andamento (a seção "Meus jogos" das Notificações): a bola da modalidade,
 * o nome, o concurso e o sorteio; embaixo, cada aposta com as dezenas (tocar abre a aposta).
 */
@Composable
fun CartaoDoConcurso(estado: Estado, g: GrupoEmAndamento, hoje: LocalDate, abrir: (String) -> Unit) {
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
                        Text(
                            if (a.registrada) tituloDaAposta(a) else "Nova aposta",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.SemiBold,
                        )
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

/** A aposta que ainda falta registrar, ou que espera o comprovante da Caixa (ou a conferência dele). */
private fun etiquetaDoAnexo(a: Aposta): Etiqueta? = when {
    !a.registrada -> Etiqueta("Falta registrar", TomEtiqueta.LARANJA)
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

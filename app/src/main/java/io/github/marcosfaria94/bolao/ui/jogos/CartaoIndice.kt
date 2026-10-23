package io.github.marcosfaria94.bolao.ui.jogos

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.FitaDestaque
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.Tarja
import java.time.LocalDate

/** Casas decimais dos índices na tela (parametros_k.json). */
const val CASAS_INDICE = 3

fun textoIndice(v: Double?): String = v?.let { IndiceK.exibir(it, CASAS_INDICE) } ?: "—"

/** Loteca e Federal não têm os parâmetros dos índices: não há k1 nem k2 para elas. */
fun semReferencia(m: Modalidade): Boolean = m == Modalidade.LOTECA || m == Modalidade.FEDERAL

/**
 * Os índices do cartão, cada um com o nome e o valor: [("k1", "0,836"), ("k2", "0,091"),
 * ("odds", "1:34")]. null = texto no lugar ("Sem referência", "Concurso especial...").
 */
fun indicesDoCartao(item: Indices.Item): List<Pair<String, String>>? = when {
    semReferencia(item.modalidade) || item.especial -> null
    else -> listOfNotNull(
        "k1" to textoIndice(item.k1),
        "k2" to textoIndice(item.k2),
        item.odds?.let { "odds" to IndiceK.exibirOddsInteiras(it) },
    )
}

/** "k1  0,836    k2  0,091    odds  1:34" (odds sempre inteiras); Loteca e Federal: "Sem referência". */
fun linhaIndices(item: Indices.Item): String {
    val odds = item.odds?.let { "    odds  ${IndiceK.exibirOddsInteiras(it)}" }.orEmpty()
    return when {
        semReferencia(item.modalidade) -> "Sem referência$odds"
        item.especial -> "Concurso especial: sem k1 e k2$odds"
        else -> indicesDoCartao(item)!!.joinToString("    ") { (nome, valor) -> "$nome  $valor" }
    }
}

/** "Quina · 57M": a modalidade e o prêmio estimado do próximo concurso, em milhões. */
fun tituloDoCartao(item: Indices.Item): String =
    item.modalidade.nome + (item.ultimo?.premioEstimadoProximo?.takeIf { it > 0 }?.let { " · ${Dinheiro.milhoes(it)}" } ?: "")

/**
 * Uma modalidade na lista ordenada pelo k1 (Jogos e Nova aposta): o emblema (sigla e próximo
 * concurso), "Quina · 57M" bem grande, "Sorteio: Hoje" e os índices com espaço entre eles; o preço
 * fica no canto de baixo, à direita, numa etiqueta. O concurso em destaque tem contorno dourado e
 * a fita "DESTAQUE" no canto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartaoIndice(item: Indices.Item, estado: Estado, aoTocar: () -> Unit, destaque: Boolean = false) {
    val m = item.modalidade
    val u = item.ultimo
    val hoje = LocalDate.now()
    val preco = if (m.precoVariavel) "Preço variável" else Dinheiro.rs(estado.config.preco(m))
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = if (destaque) BorderStroke(2.dp, Dourado) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                EmblemaModalidade(m, item.proximoNumero, tamanho = 60.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TextoQueCabe(
                        tituloDoCartao(item),
                        MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp, lineHeight = 28.sp),
                        cor = MaterialTheme.colorScheme.onSurface,
                        peso = FontWeight.Bold,
                        menor = 15.sp,
                        // Com a fita, o título não passa por baixo dela.
                        modifier = Modifier.fillMaxWidth().padding(end = if (destaque) 34.dp else 0.dp),
                    )
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = suave)) { append("Sorteio: ") }
                            append(u?.proximaData?.let { Datas.relativa(it, hoje) } ?: "a confirmar")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val lista = indicesDoCartao(item)
                    if (lista == null) {
                        Text(linhaIndices(item), style = MaterialTheme.typography.bodyMedium, color = suave)
                    } else {
                        // Respiro entre os índices; numa tela estreita, quebram para a linha de baixo.
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            lista.forEach { (nome, valor) ->
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(nome) }
                                        append(" ")
                                        append(valor)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = suave,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    // Preço no canto de baixo, à direita, com fundo próprio.
                    Text(
                        preco,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Tarja.texto(),
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp)
                            .background(Tarja.fundo(), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
            if (destaque) FitaDestaque()
        }
    }
}

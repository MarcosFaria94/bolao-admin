package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
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
import io.github.marcosfaria94.bolao.ui.comum.TituloDestaque
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

/** Prêmio estimado do próximo concurso, por extenso ("R$ 58,5 Milhões"); sem estimativa, "a confirmar". */
fun premioDoCartao(item: Indices.Item): String =
    item.ultimo?.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "a confirmar"

/** Preço da aposta simples, no canto do cartão: "R$ 3,00"; na Federal, "Variável". */
fun precoDoCartao(m: Modalidade, estado: Estado): String =
    if (m.precoVariavel) "Variável" else Dinheiro.rs(estado.config.preco(m))

/** Largura da coluna da bola e do nome (igual em todos os cartões, para as bolas ficarem alinhadas). */
private val LARGURA_BOLA = 72.dp

/**
 * Uma modalidade na lista ordenada pelo k1 (Jogos): a bola (sigla e próximo concurso) com o nome
 * da modalidade embaixo; ao lado, o preço no canto de cima, "Prêmio: R$ 58,5 Milhões",
 * "Sorteio: Hoje" e os índices. O concurso em destaque tem contorno dourado e, no alto, a
 * estrelinha com "CONCURSO EM DESTAQUE".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CartaoIndice(item: Indices.Item, estado: Estado, aoTocar: () -> Unit, destaque: Boolean = false) {
    val m = item.modalidade
    val u = item.ultimo
    val hoje = LocalDate.now()
    val cor = MaterialTheme.colorScheme.onSurface
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = aoTocar,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = if (destaque) BorderStroke(2.dp, Dourado) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            if (destaque) {
                TituloDestaque(Modifier.padding(start = 6.dp, bottom = 10.dp))
            }
            Row(verticalAlignment = Alignment.Top) {
                // A bola e, embaixo dela (fora dela), o nome da modalidade.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.width(LARGURA_BOLA),
                ) {
                    EmblemaModalidade(m, item.proximoNumero, tamanho = 58.dp)
                    Text(
                        m.nome,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 16.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = cor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // O preço no canto de cima, à direita.
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        EtiquetaPreco(precoDoCartao(m, estado))
                    }
                    TextoQueCabe(
                        buildAnnotatedString {
                            append("Prêmio: ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(premioDoCartao(item)) }
                        },
                        MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal),
                        cor = cor,
                        menor = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // "Sorteio:" e a data na mesma cor.
                    Text(
                        "Sorteio: " + (u?.proximaData?.let { Datas.relativa(it, hoje) } ?: "a confirmar"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = cor,
                        maxLines = 1,
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
                }
            }
        }
    }
}

/** Etiqueta do preço, no canto de cima do cartão: letra grande, com fundo próprio. */
@Composable
private fun EtiquetaPreco(preco: String) {
    Box(
        Modifier
            .background(Tarja.fundo(), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            preco,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
            color = Tarja.texto(),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

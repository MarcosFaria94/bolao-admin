package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.Icon
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
import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.ui.comum.EtiquetaDePreco
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.ui.comum.TarjaDoDestaque
import io.github.marcosfaria94.bolao.ui.comum.TituloDestaque
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import java.time.LocalDate

/** Casas decimais dos índices na tela (parametros_k.json). */
const val CASAS_INDICE = 3

fun textoIndice(v: Double?): String = v?.let { IndiceK.exibir(it, CASAS_INDICE) } ?: "—"

/** Loteca (e a Federal, das versões antigas) não têm os parâmetros dos índices: não há k1 nem k2 para elas. */
fun semReferencia(m: Modalidade): Boolean = m == Modalidade.LOTECA || m == Modalidade.FEDERAL

/**
 * Os índices do cartão, cada um com o nome e o valor: [("k1", "0,836"), ("k2", "0,091"),
 * ("odds", "1:34")]. null = texto no lugar ("Sem referência", "Concurso especial..."). Desde a 0.33 o concurso
 * especial tem o k1 e o k2 da média dos últimos especiais (sem histórico, o texto).
 */
fun indicesDoCartao(item: Indices.Item): List<Pair<String, String>>? = when {
    semReferencia(item.modalidade) || (item.especial && item.k1 == null) -> null
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
        item.especial && item.k1 == null -> "$SEM_INDICES_NO_ESPECIAL$odds"
        else -> indicesDoCartao(item)!!.joinToString("    ") { (nome, valor) -> "$nome  $valor" }
    }
}

/** Prêmio estimado do próximo concurso, por extenso ("R$ 58,5 Milhões"); sem estimativa da Caixa, "Não divulgado". */
fun premioDoCartao(item: Indices.Item): String =
    item.ultimo?.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: NAO_DIVULGADO

/** O que aparece quando a Caixa ainda não divulgou a informação (o prêmio estimado ou a data do sorteio). */
const val NAO_DIVULGADO = "Não divulgado"

/** Preço da aposta simples, no canto do cartão: "R$ 3,00"; na Federal, "Variável". */
fun precoDoCartao(m: Modalidade, estado: Estado): String =
    if (m.precoVariavel) "Variável" else Dinheiro.rs(estado.config.preco(m))

/**
 * O aviso do cartão quando os dados não são certos (desde a 0.20): "Aguardando resultado" (o sorteio
 * já passou e o resultado não chegou) ou "Aguardando rateio" (o resultado chegou sem o rateio). Em
 * dia (ou sem a data do próximo sorteio), nada.
 */
fun avisoDoCartao(s: AgendaDaCaixa.Situacao?): String? = when (s?.estado) {
    AgendaDaCaixa.Estado.SORTEANDO, AgendaDaCaixa.Estado.AGUARDANDO -> "Aguardando resultado"
    AgendaDaCaixa.Estado.SEM_RATEIO -> "Aguardando rateio"
    else -> null
}

/**
 * Uma modalidade na lista ordenada pelo k1 (Jogos). Desde a 0.27, no alto do cartão, de ponta a ponta, a
 * tarja da modalidade como as da Caixa (até a 0.26, a bola com a sigla e o concurso); embaixo, o concurso,
 * o preço no canto, "Prêmio: R$ 58,5 Milhões", "Sorteio: Hoje" ("Em 3 dias" até 7 dias) e os índices.
 * Desde a 0.34 o concurso em destaque (o de maior k1, especial ou não) tem a tarja dourada [TarjaDoDestaque] logo
 * abaixo da tarja da modalidade, e todo concurso especial tem, no alto, o título que brilha "CONCURSO ESPECIAL"
 * ([TituloDestaque]; até a 0.33 o especial era sempre o destaque e só o destaque tinha o título). Quando os dados não são certos ([situacao]), o aviso ao lado do concurso ([avisoDoCartao]).
 * Quando o próximo concurso é um concurso especial ([Especiais]), a tarja é a dele ("mega da virada").
 */
@Composable
fun CartaoIndice(
    item: Indices.Item,
    estado: Estado,
    aoTocar: () -> Unit,
    destaque: Boolean = false,
    aoSegurar: (() -> Unit)? = null,
    situacao: AgendaDaCaixa.Situacao? = null,
    aoInformar: (() -> Unit)? = null,
) {
    val m = item.modalidade
    val hoje = LocalDate.now()
    CartaoDoJogo(
        m = m,
        especial = Especiais.doConcurso(m, item.proximoNumero),
        concurso = item.proximoNumero,
        preco = precoDoCartao(m, estado),
        premio = premioDoCartao(item),
        sorteio = item.ultimo?.proximaData?.let { Datas.sorteio(it, hoje) } ?: NAO_DIVULGADO,
        aviso = avisoDoCartao(situacao),
        destaque = destaque,
        aoTocar = aoTocar,
        aoSegurar = aoSegurar,
        aoInformar = aoInformar,
    ) { IndicesNaLinha(item) }
}

/**
 * Um concurso especial à parte nos Jogos (desde a 0.27): o mesmo cartão, com a tarja do especial ("mega da
 * virada"), o concurso, o prêmio e o dia e a hora do sorteio anunciados pela Caixa, e os índices: desde a 0.33, o
 * k1 e o k2 da média ponderada dos últimos especiais ([item] = [Indices.doEspecial]) e as odds da modalidade (sem
 * o histórico dos especiais, só as odds). Tocar abre a página do especial; segurar, os Detalhes.
 */
@Composable
fun CartaoEspecial(
    e: ConcursoEspecial,
    item: Indices.Item?,
    estado: Estado,
    aoTocar: () -> Unit,
    aoSegurar: (() -> Unit)? = null,
    destaque: Boolean = false,
    aoInformar: (() -> Unit)? = null,
) {
    val hoje = LocalDate.now()
    CartaoDoJogo(
        m = e.modalidade,
        especial = e,
        concurso = e.concurso,
        preco = precoDoCartao(e.modalidade, estado),
        premio = e.premioEstimado?.let { Dinheiro.porExtenso(it) } ?: NAO_DIVULGADO,
        sorteio = Datas.sorteio(e.dia, hoje) + " às " + AgendaDaCaixa.hora(e.hora),
        aviso = null,
        destaque = destaque,
        aoTocar = aoTocar,
        aoSegurar = aoSegurar,
        aoInformar = aoInformar,
    ) {
        if (item != null && item.k1 != null) {
            IndicesNaLinha(item)
        } else {
            val odds = item?.odds?.let { "    odds  ${IndiceK.exibirOddsInteiras(it)}" }.orEmpty()
            Text(SEM_INDICES_NO_ESPECIAL + odds, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** O que os cartões dos concursos especiais dizem no lugar do k1 e do k2 (desde a 0.33, só sem o histórico dos especiais). */
const val SEM_INDICES_NO_ESPECIAL = "Concurso especial: sem k1 e k2"

/** O cartão de um jogo nos Jogos: a tarja de ponta a ponta e, embaixo, o concurso, o preço, o prêmio, o sorteio e os índices. */
@Composable
private fun CartaoDoJogo(
    m: Modalidade,
    especial: ConcursoEspecial?,
    concurso: Int?,
    preco: String,
    premio: String,
    sorteio: String,
    aviso: String?,
    destaque: Boolean,
    aoTocar: () -> Unit,
    aoSegurar: (() -> Unit)?,
    aoInformar: (() -> Unit)? = null,
    indices: @Composable () -> Unit,
) {
    val cor = MaterialTheme.colorScheme.onSurface
    val vibrar = LocalHapticFeedback.current
    // Segurar abre os Detalhes só depois de um tempo maior que o padrão do celular (não abre sem querer).
    val base = LocalViewConfiguration.current
    val segurarMais = remember(base) {
        object : ViewConfiguration by base {
            override val longPressTimeoutMillis: Long get() = SEGURAR_PARA_DETALHES_MS
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides segurarMais) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            // Tocar abre a página do jogo; segurar um pouco, os Detalhes (com uma vibração curta).
            .combinedClickable(
                onClick = aoTocar,
                onLongClick = aoSegurar?.let { segurar ->
                    {
                        vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
                        segurar()
                    }
                },
                onLongClickLabel = if (aoSegurar != null) "Ver os detalhes" else null,
            ),
    ) {
        // A tarja de ponta a ponta, no alto do cartão (o cartão arredonda os cantos de cima).
        TarjaModalidade(m, Modifier.fillMaxWidth(), altura = 46.dp, especial = especial, cantos = 0.dp)
        // Desde a 0.34: o destaque (o de maior k1) tem a tarja dourada logo abaixo; o especial, a marca dele, sempre.
        if (destaque) TarjaDoDestaque(Modifier.fillMaxWidth())
        Column(Modifier.padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (especial != null) {
                TituloDestaque(Modifier.padding(bottom = 4.dp), especial = true)
            }
            // O concurso e, à direita, o preço; quando os dados não são certos, o aviso no lugar do concurso.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (aviso != null) {
                    Icon(Icons.Outlined.HourglassTop, contentDescription = null, tint = corDestaque(), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    TextoQueCabe(
                        aviso,
                        MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        cor = corDestaque(),
                        peso = FontWeight.SemiBold,
                        menor = 9.sp,
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                    )
                } else {
                    Text(
                        concurso?.let { "Concurso $it" } ?: "Concurso",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp),
                        fontWeight = FontWeight.Bold,
                        color = cor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(end = 6.dp),
                    )
                }
                EtiquetaDePreco(preco)
            }
            TextoQueCabe(
                buildAnnotatedString {
                    append("Prêmio: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(premio) }
                },
                MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Normal),
                cor = cor,
                menor = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            // "Sorteio:" e a data na mesma cor.
            Text("Sorteio: $sorteio", style = MaterialTheme.typography.bodyLarge, color = cor, maxLines = 1)
            // Desde a 0.32: o "i" num círculo antes do k1, do k2 e das odds (tocar abre os Indicadores, que explicam).
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconeDeInformacao(aoInformar)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) { indices() }
            }
        }
    }
    }
}

/** O que o "i" dos cartões dos Jogos diz (abre os Indicadores). */
const val SOBRE_OS_INDICES = "O que são o k1, o k2 e as odds"

/**
 * O "i" num círculo antes dos índices dos cartões dos Jogos (desde a 0.32, pedido do usuário). Tocar nele abre os
 * Indicadores (a explicação do k1, do k2 e das odds); o toque no resto do cartão continua abrindo o jogo.
 */
@Composable
private fun IconeDeInformacao(aoTocar: (() -> Unit)?) {
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(if (aoTocar != null) Modifier.clickable(onClickLabel = SOBRE_OS_INDICES, onClick = aoTocar) else Modifier)
            .semantics { contentDescription = SOBRE_OS_INDICES },
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = suave, modifier = Modifier.size(20.dp))
    }
}

/**
 * Os índices numa linha ("k1 0,836   k2 0,091   odds 1:34", o nome em negrito), com respiro entre
 * eles (numa tela estreita, quebram para a linha de baixo); Loteca, Federal e concurso especial, o texto.
 * Nos cartões dos Jogos e do Painel de Jogos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IndicesNaLinha(item: Indices.Item, modifier: Modifier = Modifier) {
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    val lista = indicesDoCartao(item)
    if (lista == null) {
        Text(linhaIndices(item), style = MaterialTheme.typography.bodyMedium, color = suave, modifier = modifier)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier) {
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

/** Quanto segurar o cartão para abrir os Detalhes (o padrão do Android é 400 ms). */
const val SEGURAR_PARA_DETALHES_MS = 700L

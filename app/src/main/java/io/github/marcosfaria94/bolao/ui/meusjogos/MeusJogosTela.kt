package io.github.marcosfaria94.bolao.ui.meusjogos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.timeDoCoracao
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.ui.aposta.Brasao
import io.github.marcosfaria94.bolao.ui.aposta.nomeDoTime
import io.github.marcosfaria94.bolao.ui.aposta.tituloDaAposta
import io.github.marcosfaria94.bolao.ui.comum.CoresEmblema
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.bola3D
import io.github.marcosfaria94.bolao.ui.home.GrupoEmAndamento
import io.github.marcosfaria94.bolao.ui.home.emOrdemDeSorteioEK1
import io.github.marcosfaria94.bolao.ui.home.jogosEmAndamento
import io.github.marcosfaria94.bolao.ui.home.pendenciaDoJogo
import io.github.marcosfaria94.bolao.ui.jogos.IndicesNaLinha
import io.github.marcosfaria94.bolao.ui.jogos.NAO_DIVULGADO
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Barra
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corAposta
import io.github.marcosfaria94.bolao.ui.tema.corApuracao
import io.github.marcosfaria94.bolao.ui.tema.corConferir
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import io.github.marcosfaria94.bolao.ui.tema.corRateio
import java.time.LocalDate

/** Endereço da tela Meus jogos (o botão da bola na barra de cima, ou puxar qualquer aba para baixo). */
const val ROTA_MEUS_JOGOS = "meus-jogos"

/**
 * Meus jogos (desde a 0.17, também o lugar das pendências: as Notificações saíram). No alto, o resumo:
 * cada pendência com a quantidade, num quadrinho que abre a tela que resolve ([resumo]), e quantos
 * jogos estão em andamento e quantos OK. Depois, os jogos em andamento (registrados esperando o sorteio
 * e os que falta registrar), um cartão por modalidade e concurso: o sorteio mais próximo primeiro e,
 * no mesmo dia, o maior k1. No cartão, a bola (sigla e concurso, como nas outras telas), o nome, o k1,
 * o k2 e as odds do concurso, o sorteio e o prêmio estimado; e cada aposta com as dezenas em bolas
 * coloridas (como no volante) e, à direita, OK ou Pendente. Tocar numa aposta abre a página dela; sem
 * jogos, o "Apostar" vai para os Jogos ([apostar]). A raiz cuida de voltar (puxando para baixo ou
 * deslizando para o lado).
 */
@Composable
fun MeusJogosTela(estado: Estado, resumo: List<ItemDoResumo>, abrir: (String) -> Unit, apostar: () -> Unit) {
    val ctx = LocalContext.current
    // Com o resumo na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(Unit) { Notificador.limparAcoes(ctx) }
    val itens by ctx.bolao.indices.itens.collectAsStateWithLifecycle()
    val hoje = LocalDate.now()
    val grupos = emOrdemDeSorteioEK1(jogosEmAndamento(estado, hoje)) { k1DoConcurso(itens[it.modalidade], it.concurso) }
    val todas = grupos.flatMap { it.apostas }
    val ok = todas.count { pendenciaDoJogo(estado, it) == null }

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "resumo") { CartaoDoResumo(resumo, todas.size, ok, abrir) }
        if (grupos.isEmpty()) {
            item(key = "vazio") { Vazio(apostar) }
        } else {
            items(grupos, key = { "${it.modalidade.name}-${it.concurso}-${it.sorteio}" }) { g ->
                CartaoDoConcurso(estado, g, itens[g.modalidade], hoje, abrir)
            }
        }
    }
}

/** O k1 do concurso [concurso] (só se os índices de agora são dele). */
fun k1DoConcurso(item: Indices.Item?, concurso: Int): Double? = item?.k?.takeIf { it.numero == concurso }?.k1

/** Os índices do concurso [concurso]: os de agora, se são dele; senão, só as odds (o k1 e o k2 com "—"). */
private fun indicesDoConcurso(item: Indices.Item, concurso: Int): Indices.Item =
    if (item.k?.numero == concurso) item else item.copy(k = null)

/** Verde do OK e âmbar do Pendente. */
@Composable
private fun corOk(): Color = corEntrada()

@Composable
private fun corPendente(): Color = corApuracao()

/** O ícone e a cor de cada tipo de pendência do resumo. */
@Composable
private fun visualDo(tipo: TipoDoResumo): Pair<ImageVector, Color> = when (tipo) {
    TipoDoResumo.REGISTRAR -> Icons.Outlined.EditNote to Color(0xFFE07B1F)
    TipoDoResumo.SEM_COMPROVANTE -> Icons.AutoMirrored.Outlined.ReceiptLong to Barra.contador
    TipoDoResumo.CONFERIR -> Icons.Outlined.TaskAlt to corConferir()
    TipoDoResumo.ENVIAR -> Icons.AutoMirrored.Outlined.Send to corRateio()
    TipoDoResumo.APURAR -> Icons.AutoMirrored.Outlined.FactCheck to corApuracao()
    TipoDoResumo.APOSTA_DO_DIA -> Icones.Trevo to corAposta()
    TipoDoResumo.APORTE -> Icons.Outlined.Payments to corEntrada()
    TipoDoResumo.EXTRATO -> Icons.Outlined.Description to CoresEmblema.EXTRATO
    TipoDoResumo.INDICES -> Icons.Outlined.WarningAmber to Color(0xFFD97706)
}

/**
 * O resumo: "Resumo" e, à direita, quantos jogos e quantos OK; embaixo, cada pendência num quadrinho
 * (o ícone, a quantidade e o nome), dois por linha. Sem pendência, "Tudo em dia".
 */
@Composable
private fun CartaoDoResumo(resumo: List<ItemDoResumo>, jogos: Int, ok: Int, abrir: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Resumo",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    (if (jogos == 1) "1 jogo" else "$jogos jogos") + if (jogos > 0) " · $ok OK" else "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (resumo.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = corOk(), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Tudo em dia: nenhuma pendência.", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    resumo.chunked(2).forEach { linha ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            linha.forEach { item -> Quadrinho(item) { abrir(item.destino) } }
                            if (linha.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** Uma pendência do resumo: o ícone num círculo e a quantidade (grande) em cima; o nome embaixo. */
@Composable
private fun RowScope.Quadrinho(item: ItemDoResumo, aoTocar: () -> Unit) {
    val (icone, cor) = visualDo(item.tipo)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClickLabel = "Resolver: ${item.tipo.rotulo}", onClick = aoTocar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(cor.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (item.quantidade > 99) "99+" else item.quantidade.toString(),
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = cor,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.tipo.rotulo,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * O prêmio estimado do concurso: o da Caixa agora (se a Caixa está recebendo apostas para ele) ou o
 * que estava valendo quando a aposta foi feita; sem nenhum, "Não divulgado".
 */
private fun premioDoConcurso(g: GrupoEmAndamento, item: Indices.Item?): String {
    val daCaixa = item?.ultimo?.takeIf { it.proximoNumero == g.concurso }?.premioEstimadoProximo
    val valor = daCaixa?.takeIf { it > 0 } ?: g.apostas.mapNotNull { it.premioEstimado }.filter { it > 0 }.maxOrNull()
    return valor?.let { Dinheiro.porExtenso(it) } ?: NAO_DIVULGADO
}

/**
 * Um concurso: a bola (sigla e concurso), o nome e os índices; o sorteio e o prêmio; e as apostas,
 * cada uma com as dezenas em bolas e OK ou Pendente.
 */
@Composable
private fun CartaoDoConcurso(estado: Estado, g: GrupoEmAndamento, item: Indices.Item?, hoje: LocalDate, abrir: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
            EmblemaModalidade(g.modalidade, g.concurso.takeIf { it > 0 }, tamanho = 58.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    g.modalidade.nome,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item != null) IndicesNaLinha(indicesDoConcurso(item, g.concurso))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Informacao(Icons.Outlined.Event, "Sorteio", g.sorteio?.let { Datas.relativa(it, hoje) } ?: NAO_DIVULGADO)
            Informacao(Icons.Outlined.EmojiEvents, "Prêmio estimado", premioDoConcurso(g, item))
        }
        g.apostas.forEach { a ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LinhaDaAposta(a, ok = pendenciaDoJogo(estado, a) == null) { abrir("aposta/${a.id}") }
        }
    }
}

/** O sorteio ou o prêmio: o ícone, o rótulo pequeno e o valor, num quadrinho. */
@Composable
private fun RowScope.Informacao(icone: ImageVector, rotulo: String, valor: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(rotulo, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            // O valor diminui um pouco para caber inteiro ("R$ 120 Milhões", "07/11/2026").
            TextoQueCabe(
                valor,
                MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 19.sp),
                peso = FontWeight.SemiBold,
                menor = 11.sp,
            )
        }
    }
}

/** Uma aposta do concurso: o número e, à direita, OK ou Pendente; embaixo, as dezenas em bolas. */
@Composable
private fun LinhaDaAposta(a: Aposta, ok: Boolean, aoTocar: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (a.registrada) tituloDaAposta(a) else "Nova aposta",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IndicadorOk(ok)
        }
        DezenasEmBolas(a)
    }
}

/** Tamanho de letra preso ao tamanho da bola (o número tem de caber nela, com qualquer letra do sistema). */
@Composable
private fun Dp.emSp(): TextUnit = with(LocalDensity.current) { this@emSp.toSp() }

/** Uma dezena como no volante: a bola na cor da modalidade com o número em branco. */
@Composable
private fun Bolinha(texto: String, cor: Color, tamanho: Dp, forma: Shape = CircleShape) {
    Box(Modifier.size(tamanho).background(cor, forma), contentAlignment = Alignment.Center) {
        val letra = (tamanho * 0.42f).emSp()
        Text(texto, color = Color.White, fontSize = letra, lineHeight = letra, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** Uma etiqueta ao lado das bolas: o time do coração (com o brasão), o mês de sorte, o bilhete da Federal. */
@Composable
private fun Etiqueta(texto: String, inicio: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(BOLA)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
            .padding(start = 6.dp, end = 12.dp),
    ) {
        inicio()
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Tamanho das bolas das dezenas (e menores nos jogos com muitas, como a Lotomania). */
private val BOLA = 34.dp
private val BOLA_PEQUENA = 30.dp

/**
 * As dezenas do jogo em bolas coloridas, como no volante: os números (em ordem), os trevos (+Milionária),
 * o time do coração e o mês de sorte; as 7 colunas da Super Sete; os 14 palpites da Loteca; o bilhete
 * da Federal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DezenasEmBolas(a: Aposta) {
    val m = a.modalidade
    val cor = corModalidade(m.cor)
    val quadrada = RoundedCornerShape(30)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (m.formato) {
            Formato.NUMEROS -> {
                val tamanho = if (a.numeros.size > 20) BOLA_PEQUENA else BOLA
                a.numeros.sorted().forEach { Bolinha(m.rotulo(it), cor, tamanho) }
                a.trevos.sorted().forEach { Bolinha(it.toString(), cor, BOLA, quadrada) }
                when (m.extra) {
                    Extra.TIME_DO_CORACAO -> a.extra?.let { caixa ->
                        val time = timeDoCoracao(caixa)
                        Etiqueta(nomeDoTime(caixa) ?: caixa) { if (time != null) Brasao(time, 26.dp) }
                    }
                    Extra.MES_DE_SORTE -> a.extra?.let { mes ->
                        Etiqueta(mes.lowercase().replaceFirstChar { it.uppercase() }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Extra.NENHUM -> Unit
                }
            }
            Formato.COLUNAS -> a.numeros.forEach { Bolinha(if (it in 0..9) it.toString() else "-", cor, BOLA, quadrada) }
            Formato.LOTECA -> a.palpitesLoteca.forEach { Bolinha(it.ifBlank { "-" }, cor, BOLA_PEQUENA, quadrada) }
            Formato.FEDERAL -> Etiqueta(a.bilheteFederal?.let { "Bilhete $it" } ?: "Bilhete") {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            }
        }
        if (m.formato != Formato.FEDERAL && a.numeros.isEmpty() && a.palpitesLoteca.isEmpty()) {
            Text("Sem números", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "OK" (verde, com o certo) ou "Pendente" (âmbar, com o relógio). */
@Composable
private fun IndicadorOk(ok: Boolean) {
    val cor = if (ok) corOk() else corPendente()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(cor.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(start = 8.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
    ) {
        Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule, contentDescription = null, tint = cor, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            if (ok) "OK" else "Pendente",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
            fontWeight = FontWeight.Bold,
            color = cor,
            maxLines = 1,
        )
    }
}

/** Sem jogos em andamento: o trevo, o texto e o "Apostar". */
@Composable
private fun Vazio(apostar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icones.Trevo, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Nenhum jogo em andamento.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Os jogos registrados aparecem aqui até o sorteio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = apostar) { Text("Apostar") }
        }
    }
}

/**
 * O ícone dos Meus jogos (na barra de cima das abas; até a 0.16 era um bilhete): uma bola 3D dourada,
 * como as das loterias, com o número de jogos em andamento no círculo branco do meio.
 */
@Composable
fun BolaDosMeusJogos(jogos: Int, tamanho: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(tamanho).bola3D(Dourado), contentAlignment = Alignment.Center) {
        Box(Modifier.size(tamanho * 0.58f).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            val letra = (tamanho * if (jogos > 9) 0.29f else 0.36f).emSp()
            Text(
                if (jogos > 99) "99" else jogos.toString(),
                color = AzulMarinho,
                fontSize = letra,
                lineHeight = letra,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

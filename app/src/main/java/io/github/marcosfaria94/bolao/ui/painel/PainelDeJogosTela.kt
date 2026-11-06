package io.github.marcosfaria94.bolao.ui.painel

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.rememberCoroutineScope
import io.github.marcosfaria94.bolao.imagem.ImagemDosJogos
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Endereço da tela Painel de Jogos (o botão da bola na barra de cima, ou puxar qualquer aba para baixo). */
const val ROTA_PAINEL = "meus-jogos"

/**
 * Painel de Jogos (até a 0.17, "Meus jogos"; desde a 0.17, também o lugar das pendências: as
 * Notificações saíram). Desde a 0.19 abre com as cores próprias (azul-marinho e dourado, como o ícone:
 * a raiz troca o esquema de cores, ver coresDoPainel). No alto, as pendências (desde a 0.18, uma lista
 * compacta): cada tipo numa linha com o ícone, o nome e a quantidade, que abre a tela que resolve
 * ([resumo]). Depois, o título "MEUS JOGOS" (com o "Compartilhar", que manda uma imagem leve com as
 * apostas registradas) e os jogos num bloco só (registrados esperando o sorteio e os que falta
 * registrar): o sorteio mais próximo primeiro e, no mesmo dia, o maior k1. Cada concurso tem uma faixa
 * na cor da modalidade (a bola com a sigla e o concurso, o nome, o k1, o k2 e as odds, o sorteio e o
 * prêmio estimado) e um filete da mesma cor à esquerda que acompanha as apostas dele; cada aposta, no
 * fundo neutro, com as dezenas em bolas coloridas (como no volante) e, à direita, OK ou Pendente.
 * Tocar numa aposta abre a página dela; sem jogos, o "Apostar" vai para os Jogos ([apostar]). A raiz
 * cuida de voltar (puxando para baixo ou deslizando para o lado).
 */
@Composable
fun PainelDeJogosTela(estado: Estado, resumo: List<ItemDoResumo>, abrir: (String) -> Unit, apostar: () -> Unit) {
    val ctx = LocalContext.current
    // Com as pendências na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(Unit) { Notificador.limparAcoes(ctx) }
    val itens by ctx.bolao.indices.itens.collectAsStateWithLifecycle()
    val hoje = LocalDate.now()
    val grupos = emOrdemDeSorteioEK1(jogosEmAndamento(estado, hoje)) { k1DoConcurso(itens[it.modalidade], it.concurso) }
    val todas = grupos.flatMap { it.apostas }
    // O "Compartilhar" manda só as apostas registradas (as que falta registrar ainda não valem).
    val registradas = todas.filter { it.registrada }
    val escopo = rememberCoroutineScope()

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "resumo") { CartaoDoResumo(resumo, abrir) }
        item(key = "titulo-jogos") {
            TituloDosJogos(compartilhar = if (registradas.isEmpty()) null else {
                { escopo.launch { compartilharJogos(ctx, registradas) } }
            })
        }
        if (grupos.isEmpty()) {
            item(key = "vazio") { Vazio(apostar) }
        } else {
            item(key = "jogos") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    grupos.forEachIndexed { i, g ->
                        // Uma linha entre um concurso e outro (a faixa colorida já marca o começo de cada um).
                        if (i > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        BlocoDoConcurso(estado, g, itens[g.modalidade], hoje, abrir)
                    }
                }
            }
        }
    }
}

/**
 * Gera a imagem dos jogos (leve, em PNG) e abre o compartilhamento do Android (WhatsApp, Telegram...).
 * No modo de teste a imagem diz que é um teste.
 */
private suspend fun compartilharJogos(ctx: Context, apostas: List<Aposta>) {
    val arquivo = withContext(Dispatchers.Default) {
        ImagemDosJogos.salvar(ctx, apostas, LocalDate.now(), ctx.bolao.repo.emTeste, File(Compartilhar.pastaTemporaria(ctx), "jogos.png"))
    }
    Compartilhar.enviarArquivo(ctx, Compartilhar.uri(ctx, arquivo), "image/png", "Compartilhar")
}

/**
 * O título dos jogos ("MEUS JOGOS", como os grupos dos Ajustes, na cor de destaque do painel; até a
 * 0.18, "JOGOS EM ANDAMENTO"), separando-os das pendências; à direita, o "Compartilhar" (só com alguma
 * aposta registrada; até a 0.18, "Compartilhar jogos").
 */
@Composable
private fun TituloDosJogos(compartilhar: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 40.dp),
    ) {
        Text(
            TITULO_DOS_JOGOS,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, letterSpacing = 0.9.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp, end = 8.dp),
        )
        if (compartilhar != null) {
            FilledTonalButton(
                onClick = compartilhar,
                contentPadding = PaddingValues(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Compartilhar", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), maxLines = 1, softWrap = false)
            }
        }
    }
}

/** O título dos jogos no Painel de Jogos. */
const val TITULO_DOS_JOGOS = "MEUS JOGOS"

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
 * As pendências (desde a 0.18, uma lista compacta; na 0.17 eram quadrinhos): "Pendências" e, embaixo,
 * uma linha por tipo (o ícone, o nome, a quantidade e a seta), que abre a tela que resolve. Sem
 * pendência, "Tudo em dia". (Até a 0.18, à direita do título, quantos jogos e quantos OK.)
 */
@Composable
private fun CartaoDoResumo(resumo: List<ItemDoResumo>, abrir: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
            Text(
                "Pendências",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        if (resumo.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = corOk(), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Tudo em dia: nenhuma pendência.", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
            }
        } else {
            resumo.forEachIndexed { i, item ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 62.dp), color = MaterialTheme.colorScheme.outlineVariant)
                LinhaDoResumo(item) { abrir(item.destino) }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** Uma pendência: o ícone num círculo colorido, o nome, a quantidade (na cor) e a seta. */
@Composable
private fun LinhaDoResumo(item: ItemDoResumo, aoTocar: () -> Unit) {
    val (icone, cor) = visualDo(item.tipo)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Resolver: ${item.tipo.rotulo}", onClick = aoTocar)
            .heightIn(min = 52.dp)
            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(Modifier.size(32.dp).background(cor.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            item.tipo.rotulo,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (item.quantidade > 99) "99+" else item.quantidade.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 21.sp),
            fontWeight = FontWeight.Bold,
            color = cor,
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
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

/** Largura do filete na cor da modalidade, à esquerda de cada concurso. */
private val FILETE = 5.dp

/**
 * A cor da modalidade para a faixa e o filete do concurso: no tema escuro, um pouco mais clara (a
 * Quina, a +Milionária e a Federal são azuis escuros, que sumiriam no fundo azul-marinho).
 */
@Composable
private fun corDoConcurso(g: GrupoEmAndamento): Color {
    val cor = corModalidade(g.modalidade.cor)
    return if (isSystemInDarkTheme()) lerp(cor, Color.White, 0.22f) else cor
}

/**
 * Um concurso (dentro do bloco dos jogos; desde a 0.19, com a modalidade separada das apostas): a
 * faixa na cor da modalidade com a bola (sigla e concurso), o nome, os índices, o sorteio e o prêmio;
 * embaixo, no fundo neutro, as apostas, cada uma com as dezenas em bolas e OK ou Pendente. Um filete
 * da cor da modalidade, à esquerda, acompanha o concurso inteiro.
 */
@Composable
private fun BlocoDoConcurso(estado: Estado, g: GrupoEmAndamento, item: Indices.Item?, hoje: LocalDate, abrir: (String) -> Unit) {
    val cor = corDoConcurso(g)
    val faixa = cor.copy(alpha = if (isSystemInDarkTheme()) 0.26f else 0.12f)
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawRect(cor, size = Size(FILETE.toPx(), size.height)) }
            .padding(start = FILETE),
    ) {
        Column(Modifier.fillMaxWidth().background(faixa)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 14.dp)) {
                EmblemaModalidade(g.modalidade, g.concurso.takeIf { it > 0 }, tamanho = 58.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        g.modalidade.nome,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 24.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item != null) IndicesNaLinha(indicesDoConcurso(item, g.concurso))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            ) {
                Informacao(Icons.Outlined.Event, "Sorteio", g.sorteio?.let { Datas.sorteio(it, hoje) } ?: NAO_DIVULGADO)
                Informacao(Icons.Outlined.EmojiEvents, "Prêmio estimado", premioDoConcurso(g, item))
            }
        }
        g.apostas.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = 12.dp, end = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            LinhaDaAposta(a, ok = pendenciaDoJogo(estado, a) == null) { abrir("aposta/${a.id}") }
        }
    }
}

/** O sorteio ou o prêmio: o ícone, o rótulo pequeno e o valor, num quadrinho (sobre a faixa da modalidade). */
@Composable
private fun RowScope.Informacao(icone: ImageVector, rotulo: String, valor: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
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
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
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
 * O ícone do Painel de Jogos (na barra de cima das abas; desde a 0.18, no lugar da bola dourada): um
 * quadrado azul-marinho de cantos arredondados com o painel (os 4 blocos) em dourado.
 */
@Composable
fun IconeDoPainel(tamanho: Dp = 38.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(tamanho).background(AzulMarinho, RoundedCornerShape(tamanho * 0.29f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icones.Painel, contentDescription = null, tint = Dourado, modifier = Modifier.size(tamanho * 0.72f))
    }
}

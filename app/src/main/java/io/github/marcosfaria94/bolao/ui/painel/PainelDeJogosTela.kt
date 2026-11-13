package io.github.marcosfaria94.bolao.ui.painel

import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import androidx.compose.material.icons.outlined.AutoAwesome
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import androidx.compose.foundation.background
import io.github.marcosfaria94.bolao.ui.comum.trazerAoAbrir
import io.github.marcosfaria94.bolao.ui.aposta.coresDaBola
import io.github.marcosfaria94.bolao.ui.aposta.CoresDaBola
import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Tarjas
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.pdf.JogosPdf
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.home.GrupoEmAndamento
import io.github.marcosfaria94.bolao.ui.home.jogosEmAndamento
import io.github.marcosfaria94.bolao.ui.home.pendenciaDoJogo
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.state.ToggleableState
import io.github.marcosfaria94.bolao.data.Favorito
import io.github.marcosfaria94.bolao.data.Favoritos
import io.github.marcosfaria94.bolao.ui.aposta.mensagemDoRegistro
import io.github.marcosfaria94.bolao.ui.aposta.valorSemQuebra
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.rememberCoroutineScope
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
 * ([resumo]). Depois, o título "MEUS JOGOS" (com o "Compartilhar", que manda um PDF leve com as
 * apostas registradas) e os jogos num bloco só (registrados esperando o sorteio e os que falta
 * registrar): o sorteio mais próximo primeiro e, no mesmo dia, o maior k1. Cada concurso tem a tarja da
 * modalidade, a faixa com o concurso, os índices, o sorteio e o prêmio estimado, e um filete da cor da
 * modalidade à esquerda que acompanha as apostas dele; cada aposta, no fundo neutro, com as dezenas em
 * bolas coloridas (como no volante) e, à direita, OK ou Pendente. Tocar numa aposta abre a página dela;
 * sem jogos, o "Apostar" vai para os Jogos ([apostar]). A raiz cuida de voltar (puxando para baixo ou
 * deslizando para o lado).
 *
 * Desde a 0.28:
 * - cada concurso abre e fecha tocando no alto dele (drill-down; começa aberto);
 * - sem as lixeiras: a dica "Segure uma aposta..." e, segurando, as caixas de seleção (também no concurso,
 *   que marca todas as apostas dele). Os botões "N selecionadas" (cancela) e "Excluir" ficam flutuando no
 *   rodapé da tela, no meio, acompanhando a rolagem. Só saem do app as apostas que ainda podem ser
 *   descartadas ([Operacoes.podeDescartar]); as já conferidas não têm a caixa;
 * - a estrelinha de cada aposta guarda o jogo nos favoritos ([Favoritos]) e a seção "JOGOS FAVORITOS", no
 *   fim, lista os favoritos por modalidade: "Apostar" registra o jogo no próximo concurso; segurando, dá para
 *   escolher vários (ou todos de uma modalidade) e apostar de uma vez.
 *
 * Desde a 0.30: os jogos por modalidade ([porModalidade]: a tarja uma vez só e os concursos embaixo, o especial
 * dentro da modalidade dele); cada concurso começa fechado, com o selo de quantas apostas logo depois do número.
 */
@Composable
fun PainelDeJogosTela(estado: Estado, resumo: List<ItemDoResumo>, abrir: (String) -> Unit, apostar: () -> Unit) {
    val ctx = LocalContext.current
    // Com as pendências na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(Unit) { Notificador.limparAcoes(ctx, ctx.bolao.repo.sessao) }
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val hoje = LocalDate.now()
    // Desde a 0.30, por modalidade (o concurso especial, como a Mega da Virada, fica dentro da modalidade dele): as
    // modalidades pelo sorteio mais próximo e, no mesmo dia, pelo maior k1; os concursos de cada uma, o mais próximo primeiro.
    val modalidades = porModalidade(jogosEmAndamento(estado, hoje)) { indicesDoConcurso(itens[it.modalidade], it)?.k1 }
    val grupos = modalidades.flatMap { it.concursos }
    val todas = grupos.flatMap { it.apostas }
    // O "Compartilhar" manda só as apostas registradas (as que falta registrar ainda não valem).
    val registradas = todas.filter { it.registrada }
    val escopo = rememberCoroutineScope()
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    // A seleção (segurar uma aposta): null = sem seleção. Só as apostas que ainda podem sair do app.
    var selApostas by remember { mutableStateOf<Set<String>?>(null) }
    val podem = todas.filter { Operacoes.podeDescartar(estado, it) }.map { it.id }.toSet()
    val apostasMarcadas = selApostas?.intersect(podem)
    val selecionando = apostasMarcadas != null
    fun cancelar() {
        selApostas = null
    }
    BackHandler(enabled = selecionando) { cancelar() }
    var excluir by remember { mutableStateOf<Exclusao?>(null) }
    fun pedirExclusao(apostas: List<Aposta>, titulo: String) {
        val lista = apostas.filter { it.id in podem }
        if (lista.isNotEmpty()) excluir = Exclusao(lista, titulo, textoDaExclusao(lista))
    }
    fun alternarFavorito(a: Aposta) {
        val antes = Favoritos.doJogo(estado, a)
        escopo.launch {
            repo.atualizar { Favoritos.alternar(it, a, System.currentTimeMillis()) }
            if (antes == null) {
                avisos.showSnackbar(FAVORITO_GUARDADO)
            } else {
                tirouFavorito(avisos, antes) { f -> repo.atualizar { Favoritos.devolver(it, f) } }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            // Com a seleção, um espaço a mais no fim: os botões flutuantes não cobrem o último jogo.
            contentPadding = if (selecionando) PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp) else PaddingTela,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "resumo") { CartaoDoResumo(resumo, abrir) }
            item(key = "titulo-jogos") {
                Column {
                    TituloDosJogos(compartilhar = if (registradas.isEmpty()) null else {
                        { escopo.launch { compartilharJogos(ctx, grupos, itens) } }
                    })
                    // Os jogos favoritos (desde a 0.29, um botão que abre a tela deles; na 0.28, uma seção no fim).
                    if (!selecionando) BotaoDosFavoritos(estado.favoritos.size) { abrir(ROTA_FAVORITOS) }
                    // A dica (desde a 0.28, no lugar das lixeiras): segurar uma aposta abre a seleção.
                    if (podem.isNotEmpty() && !selecionando) Dica(DICA_DAS_APOSTAS)
                }
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
                        modalidades.forEachIndexed { i, md ->
                            // Uma linha entre uma modalidade e outra (a tarja já marca o começo de cada uma).
                            if (i > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            key(md.modalidade) {
                                // A tarja da modalidade uma vez só e, embaixo, os concursos dela (desde a 0.30).
                                TarjaModalidade(md.modalidade, Modifier.fillMaxWidth(), altura = 42.dp, cantos = 0.dp)
                                md.concursos.forEachIndexed { j, g ->
                                    if (j > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                    key(g.modalidade, g.concurso, g.sorteio) {
                                        BlocoDoConcurso(
                                            estado, g, itens[g.modalidade], hoje, abrir,
                                            podem = podem,
                                            selecao = apostasMarcadas,
                                            aoSegurar = { a -> selApostas = setOf(a.id) },
                                            aoMarcar = { ids, marcar ->
                                                selApostas = apostasMarcadas.orEmpty().let { if (marcar) it + ids else it - ids.toSet() }
                                            },
                                            aoFavoritar = ::alternarFavorito,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Os botões da seleção, flutuando no rodapé da tela, no meio (desde a 0.28; na 0.27 ficavam no lugar do título).
        if (apostasMarcadas != null) {
            BarraFlutuante(
                apostasMarcadas.size, EXCLUIR, Icons.Outlined.Delete, perigo = true,
                aoCancelar = ::cancelar,
                aoConfirmar = { pedirExclusao(todas.filter { it.id in apostasMarcadas }, if (apostasMarcadas.size == 1) "Excluir a aposta?" else "Excluir as apostas?") },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            )
        }
    }

    // A confirmação da exclusão: uma janela só para todas as apostas escolhidas.
    excluir?.let { x ->
        DialogoConfirmar(
            titulo = x.titulo,
            texto = x.texto,
            confirmar = EXCLUIR,
            aoConfirmar = {
                excluir = null
                cancelar()
                escopo.launch {
                    try {
                        val anexos = x.apostas.mapNotNull { it.comprovante }
                        repo.atualizar { Operacoes.descartarApostas(it, x.apostas.map { a -> a.id }) }
                        anexos.forEach { repo.apagarSeSolto(it) }
                        avisos.showSnackbar(avisoDaExclusao(x.apostas))
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível excluir: ${t.message}")
                    }
                }
            },
            aoFechar = { excluir = null },
        )
    }
}

const val FAVORITO_GUARDADO = "Jogo guardado nos favoritos."
const val FAVORITO_TIRADO = "Jogo tirado dos favoritos."
const val DESFAZER = "Desfazer"
const val APOSTAR = "Apostar"

/** As dicas do Painel (desde a 0.28): segurar uma aposta abre a seleção; nos jogos favoritos, como apostar. */
const val DICA_DAS_APOSTAS = "Segure uma aposta para selecionar e excluir."
const val DICA_DOS_FAVORITOS = "Apostar registra o jogo no próximo concurso. Segure um jogo para escolher vários."

/**
 * O botão dos jogos favoritos (desde a 0.29, embaixo de "MEUS JOGOS"; na 0.28 os favoritos eram uma seção no fim
 * do Painel): a estrela, "Jogos favoritos", quantos são e a seta; abre a tela deles ([FavoritosTela]).
 */
@Composable
private fun BotaoDosFavoritos(quantos: Int, aoTocar: () -> Unit) {
    Card(
        onClick = aoTocar,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 52.dp).padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box(Modifier.size(32.dp).background(Dourado.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Dourado, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                TITULO_FAVORITOS,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                quantos.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

/** Uma modalidade no Painel (desde a 0.30): os concursos dela com jogos em andamento, o sorteio mais próximo primeiro. */
data class ModalidadeDoPainel(val modalidade: Modalidade, val concursos: List<GrupoEmAndamento>)

/**
 * A ordem do Painel (desde a 0.30, pedido do usuário; na 0.29 cada concurso era um bloco e a Mega da Virada ficava
 * separada da Mega-Sena, como outra modalidade): os concursos agrupados por modalidade (o concurso especial fica
 * dentro da modalidade dele, como um concurso a mais); as modalidades pelo sorteio mais próximo (o do concurso
 * mais próximo dela; sem data no fim) e, no mesmo dia, pelo maior k1 desse concurso ([k1]; sem k1, depois); dentro
 * de cada modalidade, os concursos pelo sorteio mais próximo.
 */
fun porModalidade(grupos: List<GrupoEmAndamento>, k1: (GrupoEmAndamento) -> Double?): List<ModalidadeDoPainel> =
    grupos
        .groupBy { it.modalidade }
        .map { (m, lista) ->
            ModalidadeDoPainel(m, lista.sortedWith(compareBy<GrupoEmAndamento> { it.sorteio == null }.thenBy { it.sorteio }.thenBy { it.concurso }))
        }
        .sortedWith(
            compareBy<ModalidadeDoPainel> { it.concursos.first().sorteio == null }
                .thenBy { it.concursos.first().sorteio }
                .thenBy { -(k1(it.concursos.first()) ?: Double.NEGATIVE_INFINITY) }
                .thenBy { it.modalidade.ordinal },
        )

/** Um favorito pronto para apostar: a aposta no próximo concurso e por que ainda não dá ([impedimento], null = dá). */
data class PlanoDoFavorito(val favorito: Favorito, val aposta: Aposta, val impedimento: String?)

/**
 * Por que o favorito [f] não pode ser apostado agora (null = pode): o jogo com mais números sem o parâmetro,
 * o próximo concurso que o app ainda não sabe ([concurso] 0) ou que já foi sorteado sem o resultado chegar.
 */
fun impedimentoDoFavorito(e: Estado, f: Favorito, concurso: Int, sorteado: Boolean): String? = when {
    !Favoritos.cabe(e, f) -> "Precisa das apostas com mais números (Ajustes > Parâmetros)."
    e.config.preco(f.modalidade) <= 0 -> "Informe o preço da aposta simples em Ajustes > Parâmetros."
    concurso <= 0 -> "Sem os dados do próximo concurso da ${f.modalidade.nome}: abra os Jogos para consultar a Caixa."
    sorteado -> "O concurso $concurso da ${f.modalidade.nome} já foi sorteado: aguarde o resultado."
    else -> null
}

/**
 * O texto da confirmação de apostar os favoritos: para qual concurso vai cada aposta, quanto sai do saldo e,
 * no bolão, o lembrete de fazer os jogos na Caixa e anexar os comprovantes.
 */
fun textoDosFavoritos(apostas: List<Aposta>, individual: Boolean): String {
    val valor = apostas.sumOf { it.valor }
    val destino = if (apostas.size == 1) {
        val a = apostas[0]
        "A aposta da ${a.modalidade.nome} vai para o concurso ${a.concurso}"
    } else {
        val concursos = apostas.groupBy { it.modalidade to it.concurso }.keys.map { (m, c) -> "${m.nome} $c" }
        val lista = if (concursos.size == 1) concursos[0] else concursos.dropLast(1).joinToString(", ") + " e " + concursos.last()
        "As ${apostas.size} apostas vão para os próximos concursos ($lista)"
    }
    val lembrete = if (individual) "" else " Depois, faça os jogos no app Loterias CAIXA e anexe os comprovantes na aba Comprovantes."
    return "$destino, e ${Dinheiro.rs(valor)} sai do saldo.$lembrete"
}

/** O aviso depois de apostar os favoritos: "Aposta Nº3 registrada." ou "3 apostas registradas (Nº3 a Nº5)." */
fun mensagemDosFavoritos(feitas: List<Aposta>, individual: Boolean): String {
    if (feitas.size <= 1) return mensagemDoRegistro(feitas, individual)
    val texto = "${feitas.size} apostas registradas (Nº${feitas.minOf { it.numero }} a Nº${feitas.maxOf { it.numero }})."
    return texto + if (individual) "" else " Anexe os comprovantes da Caixa na aba Comprovantes."
}

/** Uma exclusão esperando a confirmação: as apostas, o título e o texto da janela. */
private data class Exclusao(val apostas: List<Aposta>, val titulo: String, val texto: String)

/**
 * O texto da confirmação das lixeiras do Painel de Jogos (desde a 0.27): quais apostas saem do app, quanto
 * volta ao saldo (as registradas) e, quando há, quantas apostas já conferidas ficam ([ficam]).
 */
fun textoDaExclusao(apostas: List<Aposta>, ficam: Int = 0): String {
    val valor = apostas.filter { it.registrada }.sumOf { it.valor }
    val quais = if (apostas.size == 1) {
        val a = apostas[0]
        (if (a.registrada) "A aposta Nº${a.numero}" else "A nova aposta (ainda não registrada)") +
            " da ${a.modalidade.nome}" + (if (a.concurso > 0) " (concurso ${a.concurso})" else "") + " sai do app"
    } else {
        "As ${apostas.size} apostas saem do app"
    }
    val dinheiro = if (valor > 0) " e ${Dinheiro.rs(valor)} volta" + (if (apostas.size == 1) "" else "m") + " ao saldo" else ""
    val resto = when (ficam) {
        0 -> ""
        1 -> " A aposta já conferida fica: ela não pode mais sair do app."
        else -> " As $ficam apostas já conferidas ficam: elas não podem mais sair do app."
    }
    return "$quais$dinheiro.$resto"
}

/** O aviso depois da exclusão: "Aposta excluída: R$ 6,00 voltou ao saldo." ou "3 apostas excluídas." */
fun avisoDaExclusao(apostas: List<Aposta>): String {
    val valor = apostas.filter { it.registrada }.sumOf { it.valor }
    val quais = if (apostas.size == 1) "Aposta excluída" else "${apostas.size} apostas excluídas"
    return quais + (if (valor > 0) ": ${Dinheiro.rs(valor)} voltou ao saldo." else ".")
}

/**
 * Gera o PDF dos jogos (desde a 0.27; até a 0.26, uma imagem PNG): leve, com páginas do tamanho da tela do
 * celular, agrupado por modalidade e concurso como o Painel ([JogosPdf]); e abre o compartilhamento do
 * Android (WhatsApp, Telegram...). Só as apostas registradas. No modo de teste o PDF diz que é um teste.
 */
private suspend fun compartilharJogos(ctx: Context, grupos: List<GrupoEmAndamento>, itens: Map<Modalidade, Indices.Item>) {
    val hoje = LocalDate.now()
    val doPdf = grupos.mapNotNull { g ->
        val registradas = g.apostas.filter { it.registrada }
        if (registradas.isEmpty()) null else JogosPdf.Grupo(g.modalidade, g.concurso, g.sorteio, premioDoConcurso(g, itens[g.modalidade]), registradas)
    }
    val arquivo = withContext(Dispatchers.Default) {
        val bytes = JogosPdf.gerar(doPdf, hoje, ctx.bolao.repo.emTeste) { nomeDoTime(it) }
        File(Compartilhar.pastaTemporaria(ctx), "meus-jogos-${Datas.compacta(hoje)}.pdf").apply { writeBytes(bytes) }
    }
    Compartilhar.enviarArquivo(ctx, Compartilhar.uri(ctx, arquivo), "application/pdf", "Compartilhar")
}

/**
 * Os botões da seleção (desde a 0.28, flutuando no rodapé da tela, no meio; na 0.27, uma barra no lugar do
 * título): "✕ N selecionadas" (cancela a seleção) e a ação ([acao]: "Excluir", em vermelho, ou "Apostar").
 */
@Composable
internal fun BarraFlutuante(
    quantas: Int,
    acao: String,
    icone: ImageVector,
    perigo: Boolean,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            OutlinedButton(
                onClick = aoCancelar,
                contentPadding = PaddingValues(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                modifier = Modifier.semantics { contentDescription = CANCELAR_SELECAO },
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (quantas == 1) "1 selecionada" else "$quantas selecionadas", maxLines = 1, softWrap = false)
            }
            Button(
                onClick = aoConfirmar,
                enabled = quantas > 0,
                colors = if (perigo) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                } else {
                    ButtonDefaults.buttonColors()
                },
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(acao, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Uma dica pequena embaixo do título da seção (o ícone do toque e o texto). */
@Composable
internal fun Dica(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)) {
        Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

const val EXCLUIR = "Excluir"
const val CANCELAR_SELECAO = "Cancelar a seleção"

/**
 * O título dos jogos ("MEUS JOGOS", como os grupos dos Ajustes, na cor de destaque do painel; até a
 * 0.18, "JOGOS EM ANDAMENTO"), separando-os das pendências; à direita, o "Compartilhar" (só com alguma
 * aposta registrada; até a 0.18, "Compartilhar jogos").
 */
@Composable
private fun TituloDosJogos(compartilhar: (() -> Unit)?) = TituloDaSecao(TITULO_DOS_JOGOS, compartilhar)

/** O título de uma seção do Painel ("MEUS JOGOS", "JOGOS FAVORITOS"), com o "Compartilhar" quando há. */
@Composable
private fun TituloDaSecao(titulo: String, compartilhar: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 40.dp),
    ) {
        Text(
            titulo,
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

/**
 * Os índices do concurso do grupo [g]: os guardados nas apostas quando foram registradas (desde a 0.21);
 * sem eles, os de agora, se são do concurso; senão, só as odds (o k1 e o k2 com "—").
 */
fun indicesDoConcurso(item: Indices.Item?, g: GrupoEmAndamento): Indices.Item? {
    val guardados = g.apostas.firstNotNullOfOrNull { it.indices }
    if (guardados != null) {
        val k = ResultadoK(
            numero = g.concurso, especial = guardados.especial, acumuladoPrincipal = 0.0, reserva = 0,
            herancaFaixas = emptyMap(), nAnterior = 0.0, k1 = guardados.k1, k2 = guardados.k2,
        )
        return (item ?: Indices.Item(g.modalidade, null, null, null)).copy(k = k, odds = guardados.odds ?: item?.odds)
    }
    if (item == null) return null
    return if (item.k?.numero == g.concurso) item else item.copy(k = null)
}

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
        // Com a letra grande, um nome comprido ("Apostas não registradas", desde a 0.30) passa para a segunda linha.
        Text(
            item.tipo.rotulo,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
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
 * Quina, a +Milionária e a Federal são azuis escuros, que sumiriam no fundo azul-marinho). Desde a 0.29,
 * revisada: a Timemania é amarela (a cor da tarja dela; [Tarjas.destaque]) e o concurso especial (Mega da
 * Virada...) é dourado, para não se confundir com o concurso normal da modalidade.
 */
@Composable
private fun corDoConcurso(g: GrupoEmAndamento): Color {
    if (Especiais.doConcurso(g.modalidade, g.concurso) != null) return Dourado
    val cor = corModalidade(Tarjas.destaque(g.modalidade))
    return if (isSystemInDarkTheme() && g.modalidade != Modalidade.TIMEMANIA) lerp(cor, Color.White, 0.22f) else cor
}

/** A faixa clarinha do alto do concurso, na cor dele (no amarelo e no dourado, um pouco mais forte, para aparecer). */
@Composable
private fun faixaDoConcurso(g: GrupoEmAndamento, cor: Color): Color {
    val claro = g.modalidade == Modalidade.TIMEMANIA || Especiais.doConcurso(g.modalidade, g.concurso) != null
    return cor.copy(alpha = if (isSystemInDarkTheme()) (if (claro) 0.13f else 0.26f) else (if (claro) 0.22f else 0.12f))
}

/**
 * Um concurso (dentro do bloco dos jogos; desde a 0.19, com a modalidade separada das apostas). Desde a 0.30 a tarja
 * da modalidade fica uma vez só, em cima dos concursos dela ([ModalidadeDoPainel]); cada concurso começa pela faixa
 * clarinha com o concurso, quantas apostas (em destaque, logo depois do número), o sorteio e o prêmio; depois, no
 * fundo neutro, as apostas, cada uma com as dezenas em bolas e OK ou Pendente. Um filete da cor da modalidade (no
 * concurso especial, dourado, com o nome dele em cima do número) acompanha o concurso inteiro, à esquerda.
 *
 * Tocar na faixa abre e fecha o concurso (drill-down; desde a 0.30 começa fechado; a seta diz qual). Sem as
 * lixeiras: segurar uma aposta abre a seleção ([aoSegurar]); na seleção ([selecao] não nulo), a caixa do concurso
 * marca ou desmarca todas as apostas dele que podem sair do app ([aoMarcar]). A estrelinha de cada aposta guarda o
 * jogo nos favoritos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlocoDoConcurso(
    estado: Estado,
    g: GrupoEmAndamento,
    item: Indices.Item?,
    hoje: LocalDate,
    abrir: (String) -> Unit,
    podem: Set<String>,
    selecao: Set<String>?,
    aoSegurar: (Aposta) -> Unit,
    aoMarcar: (List<String>, marcar: Boolean) -> Unit,
    aoFavoritar: (Aposta) -> Unit,
) {
    val cor = corDoConcurso(g)
    val faixa = faixaDoConcurso(g, cor)
    val especial = Especiais.doConcurso(g.modalidade, g.concurso)
    val doConcurso = g.apostas.filter { it.id in podem }.map { it.id }
    var aberto by rememberSaveable { mutableStateOf(false) }
    val titulo = if (g.concurso > 0) "Concurso ${g.concurso}" else "Concurso"
    Column(
        Modifier
            .fillMaxWidth()
            // Ao abrir, a lista rola para mostrar o máximo do concurso a partir do alto dele (desde a 0.32).
            .trazerAoAbrir(aberto)
            .drawBehind { drawRect(cor, size = Size(FILETE.toPx(), size.height)) }
            .padding(start = FILETE),
    ) {
        // O alto do concurso: tocar abre e fecha.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = if (aberto) "Fechar" else "Abrir") { aberto = !aberto }
                .background(faixa)
                .padding(start = if (selecao != null && doConcurso.isNotEmpty()) 0.dp else 12.dp, end = 4.dp, top = 10.dp, bottom = 12.dp),
        ) {
            // Na seleção, a caixa do concurso: marca (ou desmarca) todas as apostas dele que podem sair.
            if (selecao != null && doConcurso.isNotEmpty()) {
                val marcadas = doConcurso.count { it in selecao }
                TriStateCheckbox(
                    state = when (marcadas) {
                        0 -> ToggleableState.Off
                        doConcurso.size -> ToggleableState.On
                        else -> ToggleableState.Indeterminate
                    },
                    onClick = { aoMarcar(doConcurso, marcadas < doConcurso.size) },
                    modifier = Modifier.semantics { contentDescription = "Selecionar: $titulo" },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // O concurso especial dentro da modalidade (desde a 0.30): o nome dele, em dourado, em cima do número.
                if (especial != null) NomeDoEspecial(especial.nome)
                // Com a letra grande (ou a caixa da seleção), o selo desce para a linha de baixo.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    SeloDasApostas(g.apostas.size, Modifier.align(Alignment.CenterVertically))
                }
                // Desde a 0.29: sem o k1, o k2 e as odds; o sorteio e o prêmio numa linha, com os ícones (sem os quadrinhos).
                DadosDoConcurso(sorteioDoConcurso(g, hoje), premioDoConcurso(g, item))
            }
            Icon(
                if (aberto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = (if (aberto) "Fechar o " else "Abrir o ") + titulo.lowercase(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp).size(26.dp),
            )
        }
        if (!aberto) return@Column
        g.apostas.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = 12.dp, end = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            val pode = a.id in podem
            LinhaDaAposta(
                a,
                ok = pendenciaDoJogo(estado, a) == null,
                podeSelecionar = pode,
                selecionada = selecao?.let { a.id in it },
                favorita = if (Favoritos.podeFavoritar(a)) Favoritos.doJogo(estado, a) != null else null,
                aoTocar = {
                    when {
                        selecao == null -> abrir("aposta/${a.id}")
                        pode -> aoMarcar(listOf(a.id), a.id !in selecao)
                    }
                },
                aoSegurar = if (pode && selecao == null) ({ aoSegurar(a) }) else null,
                aoFavoritar = { aoFavoritar(a) },
            )
        }
    }
}

/** "1 aposta" ou "3 apostas": quantas apostas o concurso tem (desde a 0.30, o selo ao lado do número do concurso). */
fun textoDasApostas(n: Int): String = if (n == 1) "1 aposta" else "$n apostas"

/**
 * O selo de quantas apostas o concurso tem (desde a 0.30, logo depois do número, em destaque: a cor de destaque
 * do painel, dourado no escuro e azul-marinho no claro; até a 0.29, um texto cinza embaixo, só com o concurso
 * fechado).
 */
@Composable
private fun SeloDasApostas(n: Int, modifier: Modifier = Modifier) {
    Text(
        textoDasApostas(n),
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp, lineHeight = 17.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** O nome do concurso especial em cima do número (desde a 0.30): o brilho e "MEGA DA VIRADA", em dourado. */
@Composable
private fun NomeDoEspecial(nome: String) {
    val cor = corDestaque()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            nome.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 0.8.sp),
            fontWeight = FontWeight.Bold,
            color = cor,
            maxLines = 1,
        )
    }
}

/** "Hoje às 21h", "Amanhã às 20h", "14/11/2026 às 21h" (o dia como nos Jogos e a hora da agenda); sem o dia, "Não divulgado". */
private fun sorteioDoConcurso(g: GrupoEmAndamento, hoje: LocalDate): String =
    g.sorteio?.let { dia -> Datas.sorteio(dia, hoje) + " às " + AgendaDaCaixa.hora(AgendaDaCaixa.horaDoSorteio(g.modalidade, dia)) } ?: NAO_DIVULGADO

/**
 * O sorteio e o prêmio estimado do concurso (desde a 0.29, no lugar dos dois quadrinhos "Sorteio" e "Prêmio
 * estimado"): numa linha, cada um com o ícone (o calendário e o troféu) e o valor em negrito; com a letra grande,
 * o prêmio desce para a linha de baixo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DadosDoConcurso(sorteio: String, premio: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        DadoDoConcurso(Icons.Outlined.Event, "Sorteio", sorteio)
        DadoDoConcurso(Icons.Outlined.EmojiEvents, "Prêmio estimado", premio)
    }
}

@Composable
private fun DadoDoConcurso(icone: ImageVector, rotulo: String, valor: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$rotulo: $valor" },
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            valorSemQuebra(valor),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Uma aposta do concurso: o número e, à direita, OK ou Pendente e (desde a 0.28, no lugar da lixeira) a
 * estrelinha que guarda o jogo nos favoritos ([favorita]: null = o jogo não pode ser favorito); embaixo, as
 * dezenas em bolas. Segurar abre a seleção ([aoSegurar]); na seleção ([selecionada] não nulo), a caixa de
 * seleção fica à esquerda (só nas que podem sair do app) e tocar marca ou desmarca.
 */
@Composable
private fun LinhaDaAposta(
    a: Aposta,
    ok: Boolean,
    podeSelecionar: Boolean,
    selecionada: Boolean?,
    favorita: Boolean?,
    aoTocar: () -> Unit,
    aoSegurar: (() -> Unit)?,
    aoFavoritar: () -> Unit,
) {
    val vibrar = LocalHapticFeedback.current
    val titulo = if (a.registrada) tituloDaAposta(a) else "Nova aposta"
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = aoTocar,
                onLongClick = aoSegurar?.let { s ->
                    {
                        vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
                        s()
                    }
                },
                onLongClickLabel = if (aoSegurar != null) "Selecionar" else null,
            )
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
            if (selecionada != null) {
                // Na seleção, só as que podem sair do app têm a caixa (as outras ficam com o lugar dela vazio).
                if (podeSelecionar) {
                    Checkbox(
                        checked = selecionada,
                        onCheckedChange = { aoTocar() },
                        modifier = Modifier.semantics { contentDescription = "Selecionar: $titulo" },
                    )
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IndicadorOk(ok)
            if (selecionada == null && favorita != null) {
                BotaoEstrela(favorita, titulo, aoFavoritar)
            } else {
                Spacer(Modifier.width(10.dp))
            }
        }
        Box(Modifier.padding(end = 10.dp)) { DezenasEmBolas(a) }
    }
}

/** A estrelinha dos favoritos: cheia e dourada no jogo favorito; vazia nos outros. */
@Composable
internal fun BotaoEstrela(favorita: Boolean, titulo: String, aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar) {
        Icon(
            if (favorita) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = (if (favorita) "Tirar dos favoritos: " else "Favoritar: ") + titulo,
            tint = if (favorita) Dourado else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp),
        )
    }
}

/** Tamanho de letra preso ao tamanho da bola (o número tem de caber nela, com qualquer letra do sistema). */
@Composable
private fun Dp.emSp(): TextUnit = with(LocalDensity.current) { this@emSp.toSp() }

/**
 * Uma dezena como no volante: a bola na cor da modalidade com o número em branco (desde a 0.29, as cores
 * oficiais, [coresDaBola]: na Timemania, amarela com o número e o contorno verdes).
 */
@Composable
private fun Bolinha(texto: String, cor: CoresDaBola, tamanho: Dp, forma: Shape = CircleShape) {
    var mod = Modifier.size(tamanho).background(cor.fundo, forma)
    if (cor.comContorno) mod = mod.border(1.5.dp, cor.contorno, forma)
    Box(mod, contentAlignment = Alignment.Center) {
        val letra = (tamanho * 0.42f).emSp()
        Text(texto, color = cor.numero, fontSize = letra, lineHeight = letra, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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

/** As dezenas da aposta em bolas coloridas ([DezenasDoJogo]). */
@Composable
private fun DezenasEmBolas(a: Aposta) =
    DezenasDoJogo(a.modalidade, a.numeros, a.trevos, a.extra, a.palpitesLoteca, a.bilheteFederal, vazio = true)

/**
 * As dezenas do jogo em bolas coloridas, como no volante: os números (em ordem), os trevos (+Milionária),
 * o time do coração e o mês de sorte; as 7 colunas da Super Sete; os 14 palpites da Loteca; o bilhete
 * da Federal. Nas apostas do Painel, nos jogos favoritos e na escolha do favorito no volante (desde a 0.28).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DezenasDoJogo(
    m: Modalidade,
    numeros: List<Int>,
    trevos: List<Int> = emptyList(),
    extra: String? = null,
    palpitesLoteca: List<String> = emptyList(),
    bilheteFederal: String? = null,
    vazio: Boolean = false,
) {
    val cor = coresDaBola(m)
    val corDoTrevo = coresDaBola(Tarjas.trevo(m))
    val quadrada = RoundedCornerShape(30)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (m.formato) {
            Formato.NUMEROS -> {
                val tamanho = if (numeros.size > 20) BOLA_PEQUENA else BOLA
                numeros.sorted().forEach { Bolinha(m.rotulo(it), cor, tamanho) }
                trevos.sorted().forEach { Bolinha(it.toString(), corDoTrevo, BOLA, quadrada) }
                when (m.extra) {
                    Extra.TIME_DO_CORACAO -> extra?.let { caixa ->
                        val time = timeDoCoracao(caixa)
                        Etiqueta(nomeDoTime(caixa) ?: caixa) { if (time != null) Brasao(time, 26.dp) }
                    }
                    Extra.MES_DE_SORTE -> extra?.let { mes ->
                        Etiqueta(mes.lowercase().replaceFirstChar { it.uppercase() }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = cor.contorno, modifier = Modifier.size(20.dp))
                        }
                    }
                    Extra.NENHUM -> Unit
                }
            }
            Formato.COLUNAS -> numeros.forEach { Bolinha(if (it in 0..9) it.toString() else "-", cor, BOLA, quadrada) }
            Formato.LOTECA -> palpitesLoteca.forEach { Bolinha(it.ifBlank { "-" }, cor, BOLA_PEQUENA, quadrada) }
            Formato.FEDERAL -> Etiqueta(bilheteFederal?.let { "Bilhete $it" } ?: "Bilhete") {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = cor.contorno, modifier = Modifier.size(20.dp))
            }
        }
        if (vazio && m.formato != Formato.FEDERAL && numeros.isEmpty() && palpitesLoteca.isEmpty()) {
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

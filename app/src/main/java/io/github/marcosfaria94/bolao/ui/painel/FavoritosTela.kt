package io.github.marcosfaria94.bolao.ui.painel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Favorito
import io.github.marcosfaria94.bolao.data.Favoritos
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.novoId
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.aposta.valorSemQuebra
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** Endereço da tela dos jogos favoritos (desde a 0.29, o botão "Jogos favoritos" do Painel de Jogos). */
const val ROTA_FAVORITOS = "favoritos"

/** O título da tela e do botão dos jogos favoritos. */
const val TITULO_FAVORITOS = "Jogos favoritos"

/**
 * Os jogos favoritos (desde a 0.29 numa tela própria, que abre pelo botão "Jogos favoritos" embaixo de "MEUS
 * JOGOS", no Painel de Jogos; na 0.28 eram uma seção no fim do Painel), com a mesma disposição do Painel: por
 * modalidade, a tarja e cada jogo com quantos números e o preço, a estrelinha, o "Apostar" e as dezenas em
 * bolas. A estrelinha (tirar dos favoritos) e o "Apostar" pedem confirmação (sempre, mesmo com o parâmetro
 * "Registrar sem confirmação"); apostar um jogo que já foi apostado no mesmo concurso avisa que a aposta é
 * repetida ([avisoDeRepeticao]). Segurar um jogo abre a seleção, para apostar vários de uma vez (ou todos de uma
 * modalidade), com o "Apostar" flutuando no rodapé.
 */
@Composable
fun FavoritosTela(estado: Estado) {
    val ctx = LocalContext.current
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val consultas by indices.consultas.collectAsStateWithLifecycle()
    val hoje = LocalDate.now()
    val escopo = rememberCoroutineScope()
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    // A seleção (segurar um jogo): null = sem seleção.
    var selecao by remember { mutableStateOf<Set<String>?>(null) }
    val marcados = selecao?.intersect(estado.favoritos.map { it.id }.toSet())
    BackHandler(enabled = marcados != null) { selecao = null }
    // A aposta de cada favorito no próximo concurso (ou por que ainda não dá).
    val agora = Instant.now()
    fun planoDo(f: Favorito): PlanoDoFavorito {
        val m = f.modalidade
        val base = novaAposta(estado, m, itens[m]?.ultimo, hoje, System.currentTimeMillis())
        val sorteado = indices.situacao(m, agora, itens, consultas).sorteado
        return PlanoDoFavorito(f, Favoritos.aposta(estado, f, base), impedimentoDoFavorito(estado, f, base.concurso, sorteado))
    }
    var apostar by remember { mutableStateOf<List<PlanoDoFavorito>?>(null) }
    var excluir by remember { mutableStateOf<Favorito?>(null) }
    fun registrar(planos: List<PlanoDoFavorito>) {
        selecao = null
        escopo.launch {
            try {
                var feitas: List<Aposta> = emptyList()
                val agoraMs = System.currentTimeMillis()
                repo.atualizar { e ->
                    // Cada aposta com o seu próprio id (o mesmo favorito pode ser apostado mais de uma vez).
                    val apostas = planos.map { it.aposta.copy(id = novoId(), criadaEm = agoraMs) }
                    val (x, lista) = Favoritos.consolidar(e, apostas, LocalDate.now(), agoraMs)
                    feitas = lista
                    Operacoes.guardarIndices(x, itens, indices::resultadoDoEspecial)
                }
                avisos.showSnackbar(mensagemDosFavoritos(feitas, estado.individual))
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível apostar: ${t.message}")
            }
        }
    }
    // Sempre com a confirmação (desde a 0.29), que avisa das apostas repetidas.
    fun pedir(planos: List<PlanoDoFavorito>) {
        val livres = planos.filter { it.impedimento == null }
        if (livres.isEmpty()) {
            escopo.launch { avisos.showSnackbar(planos.firstNotNullOfOrNull { it.impedimento } ?: "Nada para apostar.") }
        } else {
            apostar = livres
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            // Com a seleção, um espaço a mais no fim: os botões flutuantes não cobrem o último jogo.
            contentPadding = if (marcados != null) PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp) else PaddingTela,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "dica") {
                if (estado.favoritos.isNotEmpty() && marcados == null) Dica(DICA_DOS_FAVORITOS)
            }
            item(key = "favoritos") {
                if (estado.favoritos.isEmpty()) {
                    SemFavoritos()
                } else {
                    CartaoDosFavoritos(
                        estado,
                        planoDo = ::planoDo,
                        selecao = marcados,
                        aoSegurar = { f -> selecao = setOf(f.id) },
                        aoMarcar = { ids, marcar -> selecao = marcados.orEmpty().let { if (marcar) it + ids else it - ids.toSet() } },
                        aoApostar = { p -> pedir(listOf(p)) },
                        aoTirar = { f -> excluir = f },
                    )
                }
            }
        }
        if (marcados != null) {
            BarraFlutuante(
                marcados.size, APOSTAR, Icones.Trevo, perigo = false,
                aoCancelar = { selecao = null },
                aoConfirmar = { pedir(estado.favoritos.filter { it.id in marcados }.map(::planoDo)) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            )
        }
    }

    // Apostar: a confirmação, com o aviso das apostas repetidas (o mesmo jogo já registrado no concurso).
    apostar?.let { planos ->
        val repetidas = planos.map { it.aposta to Favoritos.repetidas(estado, it.aposta) }
        val aviso = avisoDeRepeticao(repetidas)
        DialogoConfirmar(
            titulo = when {
                aviso != null -> if (repetidas.count { it.second.isNotEmpty() } == 1) "Aposta repetida" else "Apostas repetidas"
                planos.size == 1 -> "Apostar o jogo favorito?"
                else -> "Apostar os ${planos.size} jogos favoritos?"
            },
            texto = valorSemQuebra(listOfNotNull(aviso, textoDosFavoritos(planos.map { it.aposta }, estado.individual)).joinToString("\n\n")),
            confirmar = if (aviso != null) "Apostar assim mesmo" else APOSTAR,
            aoConfirmar = {
                apostar = null
                registrar(planos)
            },
            aoFechar = { apostar = null },
        )
    }
    // A estrelinha: tirar o jogo dos favoritos, com a confirmação (e o "Desfazer" no aviso).
    excluir?.let { f ->
        DialogoConfirmar(
            titulo = "Excluir o jogo favorito?",
            texto = textoDaExclusaoDoFavorito(f),
            confirmar = EXCLUIR,
            aoConfirmar = {
                excluir = null
                escopo.launch {
                    repo.atualizar { Favoritos.remover(it, f.id) }
                    tirouFavorito(avisos, f) { x -> repo.atualizar { Favoritos.devolver(it, x) } }
                }
            },
            aoFechar = { excluir = null },
        )
    }
}

/** "O jogo de 5 números da Quina sai dos favoritos. As apostas já feitas com ele continuam." */
fun textoDaExclusaoDoFavorito(f: Favorito): String =
    "O jogo de ${Favoritos.descricao(f)} da ${f.modalidade.nome} sai dos favoritos. As apostas já feitas com ele continuam."

/**
 * O aviso da aposta repetida (desde a 0.29): para cada aposta que vai ser feita, as apostas já registradas no
 * mesmo concurso com o mesmo jogo ([Favoritos.repetidas]). null = nenhuma repetida.
 */
fun avisoDeRepeticao(pares: List<Pair<Aposta, List<Aposta>>>): String? {
    val com = pares.filter { it.second.isNotEmpty() }
    if (com.isEmpty()) return null
    fun numeros(l: List<Aposta>) = l.sortedBy { it.numero }.joinToString(", ") { "Nº${it.numero}" }
    return if (com.size == 1) {
        val (a, l) = com[0]
        "Esse jogo já foi apostado no concurso ${a.concurso} da ${a.modalidade.nome} (" + (if (l.size == 1) "aposta " else "apostas ") + numeros(l) + ")."
    } else {
        "${com.size} desses jogos já foram apostados nos mesmos concursos (" +
            com.joinToString("; ") { (a, l) -> "${a.modalidade.nome} ${a.concurso}: ${numeros(l)}" } + ")."
    }
}

/** O favorito tirado: o aviso com o "Desfazer", que devolve o jogo aos favoritos. */
suspend fun tirouFavorito(avisos: SnackbarHostState, f: Favorito, devolver: suspend (Favorito) -> Unit) {
    val r = avisos.showSnackbar(FAVORITO_TIRADO, actionLabel = DESFAZER, duration = SnackbarDuration.Short)
    if (r == SnackbarResult.ActionPerformed) devolver(f)
}

/**
 * Os jogos favoritos num cartão (como os concursos do Painel): por modalidade, a tarja e cada jogo com quantos
 * números e o preço, a estrelinha (tira dos favoritos), o "Apostar" (registra no próximo concurso) e as dezenas
 * em bolas. Segurar um jogo abre a seleção ([aoSegurar]); na seleção, a caixa de cada jogo e a de "Todos os
 * jogos" da modalidade.
 */
@Composable
private fun CartaoDosFavoritos(
    estado: Estado,
    planoDo: (Favorito) -> PlanoDoFavorito,
    selecao: Set<String>?,
    aoSegurar: (Favorito) -> Unit,
    aoMarcar: (List<String>, marcar: Boolean) -> Unit,
    aoApostar: (PlanoDoFavorito) -> Unit,
    aoTirar: (Favorito) -> Unit,
) {
    val porModalidade = estado.favoritos.groupBy { it.modalidade }.toSortedMap(compareBy { it.ordinal })
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        porModalidade.entries.forEachIndexed { i, (m, lista) ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            TarjaModalidade(m, Modifier.fillMaxWidth(), altura = 42.dp, cantos = 0.dp)
            if (selecao != null) {
                val ids = lista.map { it.id }
                val quantos = ids.count { it in selecao }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 12.dp)) {
                    TriStateCheckbox(
                        state = when (quantos) {
                            0 -> ToggleableState.Off
                            ids.size -> ToggleableState.On
                            else -> ToggleableState.Indeterminate
                        },
                        onClick = { aoMarcar(ids, quantos < ids.size) },
                        modifier = Modifier.semantics { contentDescription = "Selecionar todos os jogos da ${m.nome}" },
                    )
                    Text("Todos os jogos", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(Modifier.padding(start = 12.dp, end = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            lista.forEachIndexed { j, f ->
                if (j > 0) HorizontalDivider(Modifier.padding(start = 12.dp, end = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                val plano = planoDo(f)
                LinhaDoFavorito(
                    estado, plano,
                    selecionado = selecao?.let { f.id in it },
                    aoTocar = { if (selecao != null) aoMarcar(listOf(f.id), f.id !in selecao) },
                    aoSegurar = { aoSegurar(f) },
                    aoApostar = { aoApostar(plano) },
                    aoTirar = { aoTirar(f) },
                )
            }
        }
    }
}

/**
 * Um jogo favorito: quantos números e o preço; à direita, a estrelinha cheia (tira dos favoritos) e o "Apostar"
 * (desligado quando ainda não dá, com o motivo embaixo); as dezenas em bolas. Na seleção, a caixa à esquerda.
 */
@Composable
private fun LinhaDoFavorito(
    estado: Estado,
    plano: PlanoDoFavorito,
    selecionado: Boolean?,
    aoTocar: () -> Unit,
    aoSegurar: () -> Unit,
    aoApostar: () -> Unit,
    aoTirar: () -> Unit,
) {
    val f = plano.favorito
    val vibrar = LocalHapticFeedback.current
    val titulo = Favoritos.descricao(f) + " · " + valorSemQuebra(Dinheiro.rs(Favoritos.preco(estado, f)))
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = aoTocar,
                onLongClick = if (selecionado == null) {
                    {
                        vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
                        aoSegurar()
                    }
                } else {
                    null
                },
                onLongClickLabel = if (selecionado == null) "Selecionar" else null,
            )
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
            if (selecionado != null) {
                Checkbox(
                    checked = selecionado,
                    onCheckedChange = { aoTocar() },
                    modifier = Modifier.semantics { contentDescription = "Selecionar: $titulo" },
                )
            }
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (selecionado == null) {
                BotaoEstrela(favorita = true, titulo = titulo, aoTocar = aoTirar)
                FilledTonalButton(
                    onClick = aoApostar,
                    enabled = plano.impedimento == null,
                    contentPadding = PaddingValues(start = 12.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                    modifier = Modifier.heightIn(min = 38.dp).semantics { contentDescription = "Apostar: $titulo" },
                ) {
                    Text(APOSTAR, maxLines = 1, softWrap = false)
                }
            }
        }
        Box(Modifier.padding(end = 6.dp)) { DezenasDoJogo(f.modalidade, f.numeros, f.trevos, f.extra) }
        plano.impedimento?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Sem jogos favoritos: a estrela e como guardar um. */
@Composable
private fun SemFavoritos() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = Dourado, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(SEM_FAVORITOS, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

const val SEM_FAVORITOS = "Nenhum jogo favorito. Toque na estrela de uma aposta, em Meus jogos (ou na do volante), para guardar os números dela aqui."

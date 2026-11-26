package io.github.marcosfaria94.bolao.ui.painel

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Efetivacao
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Limites
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.aposta.volanteCompleto
import io.github.marcosfaria94.bolao.ui.comum.DialogoSimNao
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** O endereço da tela dos rascunhos (desde a 0.44; a linha "Rascunhos" do Painel de Jogos). */
const val ROTA_RASCUNHOS = "rascunhos"

/** O título da tela (desde a 0.44, pedido do usuário: "transforme o título Apostas não registradas por Rascunhos"). */
const val TITULO_RASCUNHOS = "Rascunhos"

const val MARCAR_TODOS_OS_RASCUNHOS = "Todos"
const val MARCAR_NENHUM_RASCUNHO = "Nenhum"
const val ABRIR_O_RASCUNHO = "Abrir o rascunho"

/** O botão do lote: "Registrar 3 apostas em lote", "Registrar 1 aposta". */
fun textoDoLoteDeRascunhos(apostas: Int): String = if (apostas == 1) "Registrar 1 aposta" else "Registrar $apostas apostas em lote"

/** "2 de 3 marcados · R$ 9,00". */
fun resumoDosRascunhos(total: Int, marcados: Int, valor: Long): String =
    "$marcados de $total marcado${if (total == 1) "" else "s"} · ${Dinheiro.rs(valor)}"

/** O nome da caixa de marcar de cada rascunho: "Marcar o rascunho da Quina 7173 (05 12 33 47 71)". */
fun nomeDaMarcaDoRascunho(a: Aposta): String =
    "Marcar o rascunho da ${a.modalidade.nome} ${a.concurso} (${io.github.marcosfaria94.bolao.domain.Transcricao.detalhe(a)})"

/** Por que o rascunho [a] não pode ser registrado agora ([sorteado]: o concurso dele já foi sorteado e o resultado não chegou); null = pode. */
fun impedimentoDoRascunho(a: Aposta, sorteado: Boolean): String? = when {
    !volanteCompleto(a) -> "Falta completar o volante."
    a.concurso <= 0 -> "Falta o número do concurso: abra o rascunho para o app consultar a Caixa."
    a.valor <= 0 -> "Falta o preço da aposta simples (Ajustes > Parâmetros)."
    sorteado -> "O concurso ${a.concurso} já foi sorteado e o resultado ainda não chegou."
    else -> null
}

/** O texto da janela do lote: quanto sai do saldo e o que fazer depois. */
fun textoDaJanelaDoLote(apostas: Int, total: Long, individual: Boolean): String =
    "Desconta ${Dinheiro.rs(total)} do saldo." + if (individual) {
        ""
    } else if (apostas == 1) {
        " Depois, efetive a aposta na aba Loteria (botão \"Efetivar apostas\")."
    } else {
        " Depois, efetive as apostas na aba Loteria (botão \"Efetivar apostas\")."
    }

/** O aviso depois do lote: "Apostas Nº3 a Nº6 registradas: efetive na aba Loteria." */
fun mensagemDoLoteDeRascunhos(feitas: List<Aposta>, individual: Boolean): String {
    if (feitas.isEmpty()) return "Nenhuma aposta registrada."
    val numeros = feitas.map { it.numero }
    val texto = if (feitas.size == 1) "Aposta Nº${numeros[0]} registrada" else "Apostas Nº${numeros.min()} a Nº${numeros.max()} registradas"
    return texto + if (individual) "." else ": efetive na aba Loteria."
}

/**
 * Os rascunhos (desde a 0.44, pedido do usuário: "transforme o título Apostas não registradas por Rascunhos... permita
 * que a mesma modalidade possa fazer vários rascunhos... crie botão para Registrar apostas em lote na parte dos
 * Rascunhos"). A lista separa por modalidade e concurso; cada rascunho tem a caixa de marcar (os prontos vêm marcados), o
 * jogo em bolas, o preço e a seta que abre o rascunho no volante. Embaixo, o "Registrar N apostas em lote" registra os
 * marcados de uma vez, como o "Registrar aposta" do volante (com a janela de confirmação, a não ser com o parâmetro
 * "Registrar sem confirmação"; respeitando o stop loss e o "Não jogar sem saldo"). Sem rascunho, um aviso.
 */
@Composable
fun RascunhosTela(estado: Estado, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val consultas by indices.consultas.collectAsStateWithLifecycle()
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val rascunhos = Operacoes.rascunhos(estado)
    fun sorteado(m: Modalidade, concurso: Int): Boolean =
        concurso == itens[m]?.proximoNumero && indices.situacao(m, Instant.now(), itens, consultas).sorteado
    val impedimentos = rascunhos.associate { it.id to impedimentoDoRascunho(it, sorteado(it.modalidade, it.concurso)) }
    val prontos = rascunhos.filter { impedimentos[it.id] == null }
    // Os prontos vêm marcados; o que se desmarca fica guardado (um rascunho novo chega marcado).
    var desmarcados by remember { mutableStateOf(emptySet<String>()) }
    val marcados = prontos.filter { it.id !in desmarcados }
    fun registraveis(lista: List<Aposta>) = lista.map { Operacoes.comoVaiSerRegistrado(estado, it) }
    val apostas = registraveis(marcados).sumOf { Operacoes.apostasDoRegistro(it) }
    val total = registraveis(marcados).sumOf { it.valor * Operacoes.apostasDoRegistro(it) }
    var perguntar by remember { mutableStateOf(false) }
    var registrando by remember { mutableStateOf(false) }

    fun registrar(ids: List<String>) {
        registrando = true
        escopo.launch {
            try {
                var feitas: List<Aposta> = emptyList()
                repo.atualizar { e ->
                    val (x, lista) = Operacoes.registrarEmLote(e, ids, LocalDate.now(), System.currentTimeMillis())
                    feitas = lista
                    // O k1, o k2 e as odds do concurso ficam guardados na aposta (desde a 0.21).
                    Operacoes.guardarIndices(x, indices.itens.value, indices::resultadoDoEspecial)
                }
                avisos.showSnackbar(mensagemDoLoteDeRascunhos(feitas, estado.individual))
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível registrar: ${t.message}")
            } finally {
                registrando = false
            }
        }
    }
    fun pedirRegistro() {
        Limites.bloqueio(estado, apostas, total, hoje)?.let { escopo.launch { avisos.showSnackbar(it) }; return }
        if (estado.parametros.semConfirmacao) registrar(marcados.map { it.id }) else perguntar = true
    }

    Column(Modifier.fillMaxSize()) {
        if (rascunhos.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                    Text(SEM_RASCUNHOS, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = desmarcados.none { id -> prontos.any { it.id == id } } && prontos.isNotEmpty(),
                onClick = { desmarcados = emptySet() },
                enabled = !registrando,
                label = { Text(MARCAR_TODOS_OS_RASCUNHOS) },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = marcados.isEmpty(),
                onClick = { desmarcados = rascunhos.map { it.id }.toSet() },
                enabled = !registrando,
                label = { Text(MARCAR_NENHUM_RASCUNHO) },
            )
            Spacer(Modifier.weight(1f))
            Text(
                resumoDosRascunhos(rascunhos.size, marcados.size, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            gruposDosRascunhos(rascunhos).forEach { g ->
                item(key = "g:${g.modalidade}:${g.concurso}") { CabecalhoDosRascunhos(g) }
                g.rascunhos.forEach { a ->
                    item(key = a.id) {
                        LinhaDoRascunho(
                            estado, a,
                            impedimento = impedimentos[a.id],
                            marcado = marcados.any { it.id == a.id },
                            habilitado = !registrando,
                            aoMarcar = { desmarcados = if (a.id in desmarcados) desmarcados - a.id else desmarcados + a.id },
                            aoAbrir = { abrir("aposta/${a.id}") },
                        )
                    }
                }
            }
        }
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Button(
                onClick = { pedirRegistro() },
                enabled = marcados.isNotEmpty() && !registrando,
                colors = ButtonDefaults.buttonColors(containerColor = AzulMarinho, contentColor = Dourado),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            ) {
                if (registrando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Dourado)
                } else {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (marcados.isEmpty()) "Marque os rascunhos" else textoDoLoteDeRascunhos(apostas) + " · " + Dinheiro.rs(total),
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                }
            }
        }
    }

    if (perguntar) {
        DialogoSimNao(
            titulo = if (apostas == 1) "Deseja registrar a aposta?" else "Deseja registrar as $apostas apostas?",
            texto = textoDaJanelaDoLote(apostas, total, estado.individual),
            aoNao = { perguntar = false },
            aoSim = {
                perguntar = false
                registrar(marcados.map { it.id })
            },
        )
    }
}

/** A tela dos rascunhos sem nenhum. */
const val SEM_RASCUNHOS = "Nenhum rascunho. Os volantes completos que você não registra ficam aqui."

/** Um grupo da lista dos rascunhos: a modalidade, o concurso, o dia do sorteio e os rascunhos dele. */
data class GrupoDeRascunhos(val modalidade: Modalidade, val concurso: Int, val sorteio: LocalDate?, val rascunhos: List<Aposta>)

/** Os rascunhos por modalidade (na ordem do app) e, dentro dela, por concurso (o sorteio mais próximo primeiro); os mais novos primeiro. */
fun gruposDosRascunhos(rascunhos: List<Aposta>): List<GrupoDeRascunhos> =
    rascunhos.groupBy { it.modalidade }.toList().sortedBy { it.first.ordinal }.flatMap { (m, lista) ->
        lista.groupBy { it.concurso }.toList()
            .sortedWith(compareBy({ it.second.firstNotNullOfOrNull { a -> a.dataSorteio } ?: LocalDate.MAX }, { it.first }))
            .map { (c, ls) -> GrupoDeRascunhos(m, c, ls.firstNotNullOfOrNull { it.dataSorteio }, ls.sortedByDescending { it.criadaEm }) }
    }

@Composable
private fun CabecalhoDosRascunhos(g: GrupoDeRascunhos) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        SeloModalidade(g.modalidade, tamanho = 24.dp)
        Spacer(Modifier.width(8.dp))
        Text(g.modalidade.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        val especial = Especiais.doConcurso(g.modalidade, g.concurso)
        Text(
            listOfNotNull(
                if (g.concurso > 0) " · Concurso ${g.concurso}" else " · Sem concurso",
                especial?.let { " (${it.nome})" },
                Efetivacao.dia(g.sorteio)?.let { " · $it" },
            ).joinToString(""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Um rascunho: a caixa de marcar, o jogo em bolas, o preço (e quantas apostas, com a teimosinha ou a espelho) e a seta. */
@Composable
private fun LinhaDoRascunho(
    estado: Estado,
    a: Aposta,
    impedimento: String?,
    marcado: Boolean,
    habilitado: Boolean,
    aoMarcar: () -> Unit,
    aoAbrir: () -> Unit,
) {
    val pronto = impedimento == null
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (marcado) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(
            Modifier
                .clickable(enabled = habilitado && pronto, role = Role.Checkbox, onClick = aoMarcar)
                .padding(start = 4.dp, end = 2.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = marcado,
                onCheckedChange = { aoMarcar() },
                enabled = habilitado && pronto,
                modifier = Modifier.semantics { contentDescription = nomeDaMarcaDoRascunho(a) },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DezenasDoJogo(a.modalidade, a.numeros, a.trevos, a.extra, a.palpitesLoteca, a.bilheteFederal, vazio = true)
                val final = Operacoes.comoVaiSerRegistrado(estado, a)
                val n = Operacoes.apostasDoRegistro(final)
                Text(
                    Dinheiro.rs(final.valor * n) + if (n > 1) " · $n apostas" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (impedimento != null) {
                    Text(impedimento, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(onClick = aoAbrir) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = ABRIR_O_RASCUNHO)
            }
        }
    }
}

package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.ConcursosNotaveis
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.EspeciaisDoHistorico
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.PremiosTipicos
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.domain.ResumoDoConcurso
import io.github.marcosfaria94.bolao.domain.Trilha
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.aposta.valorSemQuebra
import io.github.marcosfaria94.bolao.ui.comum.CartaoQueAbre
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe

/**
 * Os concursos da página do jogo (desde a 0.37): os resumos do histórico da modalidade e a [trilha] que os Detalhes
 * percorrem (no especial, só os especiais da época dele; na modalidade, só os regulares). [lendo]: o histórico ainda
 * está sendo lido (na primeira vez que se abre a modalidade; depois fica na memória).
 */
class ConcursosDaPagina(val trilha: Trilha?, val resumos: List<ResumoDoConcurso>?, val especiais: EspeciaisDoHistorico.Classificacao?) {
    val lendo: Boolean get() = resumos == null

    /** Os resumos que entram na página: os especiais da época (no especial) ou os regulares (na modalidade). */
    fun daPagina(especial: Boolean): List<ResumoDoConcurso> {
        val r = resumos.orEmpty()
        val e = especiais ?: return if (especial) emptyList() else r
        return if (especial) e.daEpoca.toSet().let { s -> r.filter { it.numero in s } } else r.filter { it.numero !in e.todos }
    }
}

/**
 * Lê (uma vez por modalidade) os resumos do histórico e monta a trilha: [u] é o último concurso que a Caixa mandou (pode
 * ainda não estar no histórico: entra pela marca de especial dele); [especial], a página de um concurso especial.
 */
@Composable
fun rememberConcursosDaPagina(m: Modalidade, u: Concurso?, especial: Boolean): ConcursosDaPagina {
    val historico = LocalContext.current.bolao.historico
    val versao by historico.acrescimos.collectAsStateWithLifecycle()
    val resumos by produceState<List<ResumoDoConcurso>?>(null, m, versao) {
        value = runCatching { historico.resumos(m) }.getOrNull() ?: emptyList()
    }
    val epoca = Especiais.epoca(m)
    val especiais = remember(resumos, epoca) { resumos?.let { EspeciaisDoHistorico.classificar(it, epoca) } }
    val trilha = remember(especiais, u?.numero, especial) {
        val e = especiais ?: return@remember null
        val ultimo = u?.numero ?: 0
        val uEspecial = u != null && u.registro?.indicadorConcursoEspecial == 2
        val uDaEpoca = uEspecial && epoca != null && u!!.data?.let { EspeciaisDoHistorico.naEpoca(it, epoca) } == true
        if (especial) {
            Trilha.especiais(e.daEpoca + listOfNotNull(u?.numero?.takeIf { uDaEpoca }))
        } else {
            // O último é o que a Caixa mandou (o mesmo dos Jogos); sem ele, o mais novo do histórico.
            val maior = ultimo.takeIf { it > 0 } ?: resumos?.lastOrNull()?.numero ?: 0
            if (maior <= 0) null else Trilha.regulares(maior, e.todos + listOfNotNull(u?.numero?.takeIf { uEspecial }))
        }
    }
    return ConcursosDaPagina(trilha, resumos, especiais)
}

/** O endereço da tela das Estatísticas (desde a 0.37): a modalidade, o especial (0 = nenhum) e o concurso dos Detalhes. */
const val ROTA_ESTATISTICAS = "estatisticas/{nome}?especial={especial}&concurso={concurso}"
const val TITULO_ESTATISTICAS = "Estatísticas"
const val TITULO_CONCURSOS_NOTAVEIS = "Concursos notáveis"

fun rotaDasEstatisticas(m: Modalidade, especial: Int?, concurso: Int?): String =
    "estatisticas/${m.name}?especial=${especial ?: 0}&concurso=${concurso ?: 0}"

/** As Estatísticas existem em todas as modalidades menos a Federal (que paga o 1º prêmio em todo concurso). */
fun temEstatisticas(m: Modalidade): Boolean = m != Modalidade.FEDERAL

/**
 * O botão das Estatísticas nos Detalhes (desde a 0.37, no fim, no lugar dos Prêmios típicos): um cartão como os que abrem,
 * com o gráfico e a seta para a direita.
 */
@Composable
fun BotaoDasEstatisticas(aoTocar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Abrir as estatísticas", onClick = aoTocar)
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(TITULO_ESTATISTICAS, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A tela das Estatísticas (desde a 0.37, pelo botão dos Detalhes): no alto a tarja da modalidade (ou a do especial) e,
 * cada um num cartão que começa fechado, abre trazendo-se para a tela e fecha com um toque (como os dos Detalhes):
 * os Concursos notáveis ([ConcursosNotaveis]: os que soltaram o prêmio principal) e os Prêmios típicos (que até a 0.36
 * ficavam nos Detalhes). Na página de um especial, só os especiais da época dele; na da modalidade, só os regulares.
 * [concurso]: o concurso que estava nos Detalhes (a coluna dos Prêmios típicos; 0 = o último).
 */
@Composable
fun EstatisticasTela(m: Modalidade, especial: Int?, concurso: Int?) {
    val app = LocalContext.current.bolao
    val itens by app.indices.itens.collectAsStateWithLifecycle()
    val u = itens[m]?.ultimo
    val esp: ConcursoEspecial? = especial?.takeIf { it > 0 }?.let { Especiais.doConcurso(m, it) }
    val ehEspecial = especial != null && especial > 0
    val pagina = rememberConcursosDaPagina(m, u, ehEspecial)
    val alvo = concurso?.takeIf { it > 0 } ?: pagina.trilha?.ultimo ?: u?.numero
    // O concurso da coluna dos Prêmios típicos (o que estava nos Detalhes; sem ele, o último da página).
    val naTela by produceState<Concurso?>(u?.takeIf { it.numero == alvo }, m, alvo) {
        val n = alvo ?: return@produceState
        if (value?.numero == n) return@produceState
        value = runCatching { app.historico.concurso(m, n) }.getOrNull()
            ?: CaixaApi.guardado(m, n)
            ?: runCatching { CaixaApi.concurso(m, n) }.getOrNull()
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TarjaModalidade(m, Modifier.fillMaxWidth(), altura = 50.dp, especial = esp)
        Text(
            if (ehEspecial) "Só os concursos especiais${esp?.let { " (${it.nome})" } ?: ""}." else "Só os concursos regulares (sem os especiais).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CartaoQueAbre(TITULO_CONCURSOS_NOTAVEIS) { TabelaDosNotaveis(m, pagina, ehEspecial) }
        CartaoQueAbre(TITULO_PREMIOS_TIPICOS) {
            TabelaDePremiosTipicos(m, u, naTela ?: u, ultimoDaPagina = pagina.trilha?.ultimo ?: u?.numero, semEstimativa = ehEspecial)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Os Concursos notáveis (desde a 0.37): os botões "Maiores prêmios" e "Mais recentes", quantas vezes o prêmio principal
 * saiu e a tabela (Concurso | Prêmio | Ganhadores, que se ordena tocando no título das colunas).
 */
@Composable
private fun TabelaDosNotaveis(m: Modalidade, pagina: ConcursosDaPagina, especial: Boolean) {
    if (pagina.lendo) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("  Lendo o histórico...", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val daPagina = remember(pagina.resumos, pagina.especiais, especial) { pagina.daPagina(especial) }
    var jeito by rememberSaveable(m, especial) { mutableIntStateOf(0) }
    val lista = remember(daPagina, jeito) { if (jeito == 0) ConcursosNotaveis.maiores(daPagina) else ConcursosNotaveis.recentes(daPagina) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SeletorDosNotaveis(listOf(ConcursosNotaveis.MAIORES, ConcursosNotaveis.RECENTES), jeito) { jeito = it }
        Text(ConcursosNotaveis.resumo(daPagina), style = MaterialTheme.typography.bodyMedium)
        if (lista.isEmpty()) return@Column
        TabelaRolavel(
            primeira = ConcursosNotaveis.COLUNA_CONCURSO,
            grupos = emptyList(),
            colunas = listOf(ConcursosNotaveis.COLUNA_PREMIO, ConcursosNotaveis.COLUNA_GANHADORES),
            linhas = lista.map { r ->
                (r.numero.toString() + (r.dia?.let { " (${it.year})" } ?: "")) to
                    listOf(valorSemQuebra(PremiosTipicos.valor(r.total)), ConcursosNotaveis.milhar(r.ganhadores))
            },
            ordem = lista.map { r -> listOf(r.total, r.ganhadores.toLong()) },
        )
        val notas = listOfNotNull(
            ConcursosNotaveis.NOTA_DO_PREMIO,
            Probabilidades.LEGENDA_DA_MAIS_MILIONARIA.takeIf { m == Modalidade.MAIS_MILIONARIA },
        )
        notas.forEach { n ->
            Text("* $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Os botões dos Concursos notáveis (como os dos acertos dos Detalhes): o escolhido destacado. */
@Composable
private fun SeletorDosNotaveis(opcoes: List<String>, escolhido: Int, aoEscolher: (Int) -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        opcoes.forEachIndexed { i, nome ->
            val sim = i == escolhido
            val fundo = if (sim) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
            val texto = if (sim) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(forma)
                    .background(fundo)
                    .clickable { aoEscolher(i) }
                    .clearAndSetSemantics {
                        contentDescription = nome
                        role = Role.Tab
                        selected = sim
                        onClick { aoEscolher(i); true }
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            ) {
                TextoQueCabe(nome, MaterialTheme.typography.labelLarge, cor = texto, peso = FontWeight.Bold, menor = 9.sp)
            }
        }
    }
}

/**
 * Os prêmios típicos (desde a 0.32; desde a 0.37 na tela das Estatísticas, antes nos Detalhes): uma linha por faixa de
 * premiação, com a estimativa do que cada aposta ganhadora recebe no próximo concurso ([PremiosTipicos.estimar], o
 * mesmo cálculo do k1, sem o IR) e o que o concurso pagou; embaixo, a nota que explica que é só uma estimativa e como
 * ela é calculada. Sem a estimativa (Loteca, concurso especial: [semEstimativa]), só o concurso. A coluna é a do
 * concurso [naTela] (o que estava nos Detalhes; "Último concurso" quando é o [ultimoDaPagina]).
 */
@Composable
fun TabelaDePremiosTipicos(m: Modalidade, u: Concurso?, naTela: Concurso? = u, ultimoDaPagina: Int? = u?.numero, semEstimativa: Boolean = false) {
    val indices = LocalContext.current.bolao.indices
    val itens by indices.itens.collectAsStateWithLifecycle()
    val estimativa = remember(itens[m], semEstimativa) { if (semEstimativa) null else indices.premiosTipicos(m) }
    val concurso = naTela ?: u
    if (concurso == null && estimativa == null) {
        Text("Sem dados da Caixa agora.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val linhas = PremiosTipicos.linhas(m, estimativa, concurso)
    val coluna = concurso?.let { colunaDoConcurso(it.numero, ultimoDaPagina) } ?: COLUNA_ULTIMO_CONCURSO
    fun texto(v: Long?): String = v?.let { valorSemQuebra(PremiosTipicos.valor(it)) } ?: "—"
    TabelaRolavel(
        primeira = COLUNA_FAIXA,
        grupos = emptyList(),
        colunas = if (estimativa != null) listOf(COLUNA_ESTIMATIVA, coluna) else listOf(coluna),
        linhas = linhas.map { l -> l.nome to (if (estimativa != null) listOf(texto(l.estimativa), texto(l.ultimo)) else listOf(texto(l.ultimo))) },
    )
    val notas = listOfNotNull(
        if (estimativa != null) PremiosTipicos.NOTA_DA_ESTIMATIVA else PremiosTipicos.SEM_ESTIMATIVA,
        concurso?.let { PremiosTipicos.notaDoConcurso(it.numero, ultimo = it.numero == ultimoDaPagina) },
        Probabilidades.LEGENDA_DA_MAIS_MILIONARIA.takeIf { m == Modalidade.MAIS_MILIONARIA },
    )
    notas.forEach { n ->
        Text("* $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

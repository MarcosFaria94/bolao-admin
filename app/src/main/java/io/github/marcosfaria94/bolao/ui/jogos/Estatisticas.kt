package io.github.marcosfaria94.bolao.ui.jogos

import kotlinx.coroutines.withContext

import kotlinx.coroutines.Dispatchers

import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt
import io.github.marcosfaria94.bolao.ui.aposta.nomeDoTimeSorteado
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.NumerosNotaveis
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
 * A tela das Estatísticas (desde a 0.37; desde a 0.38 pelo ícone no canto direito da barra dos Detalhes): no alto a tarja
 * da modalidade (ou a do especial) e, cada um num cartão que começa fechado, abre trazendo-se para a tela e fecha com um
 * toque (como os dos Detalhes): os Concursos notáveis ([ConcursosNotaveis]: os que soltaram o prêmio principal), desde a
 * 0.38 os Números notáveis ([NumerosNotaveis], com os Times, Meses, Trevos ou Resultados notáveis) e os Prêmios típicos. Na página de um especial, só os especiais da época dele; na da modalidade, só os regulares.
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
        // Desde a 0.38: os Números notáveis (e os Times, Meses, Trevos ou Resultados notáveis), um cartão para cada.
        // Desde a 0.39 a conta (milhares de concursos) roda fora da linha da tela: a Timemania travava ao abrir.
        val grupos by produceState<List<NumerosNotaveis.Grupo>?>(null, pagina.resumos, pagina.especiais, ehEspecial) {
            value = null
            if (pagina.lendo) return@produceState
            val daPagina = pagina.daPagina(ehEspecial)
            value = withContext(Dispatchers.Default) { NumerosNotaveis.grupos(m, daPagina) }
        }
        val prontos = grupos
        // Os cartões aparecem na hora (os títulos não dependem da conta); aberto antes da conta chegar, fica aberto.
        NumerosNotaveis.titulos(m).forEach { t ->
            key(t) {
                CartaoQueAbre(t) {
                    val g = prontos?.firstOrNull { it.titulo == t }
                    when {
                        g != null -> TabelaDosNumeros(m, g, ehEspecial)
                        prontos == null -> LendoOHistorico()
                        else -> Text("Nenhum concurso no histórico.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        CartaoQueAbre(TITULO_PREMIOS_TIPICOS) {
            TabelaDePremiosTipicos(m, u, naTela ?: u, ultimoDaPagina = pagina.trilha?.ultimo ?: u?.numero, semEstimativa = ehEspecial)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Os Concursos notáveis (desde a 0.37): quantas vezes o prêmio principal saiu e a tabela (Concurso | Prêmio | Ganhadores).
 * Desde a 0.38 (pedido do usuário) sem os botões "Maiores prêmios"/"Mais recentes": tocar no título de qualquer coluna,
 * também o Concurso, ordena a lista toda (de novo, ao contrário) e o controle deslizante diz quantos concursos ver (os
 * primeiros dessa ordem). Começa nos maiores prêmios, 20 concursos.
 */
@Composable
private fun TabelaDosNotaveis(m: Modalidade, pagina: ConcursosDaPagina, especial: Boolean) {
    if (pagina.lendo) {
        LendoOHistorico()
        return
    }
    val daPagina = remember(pagina.resumos, pagina.especiais, especial) { pagina.daPagina(especial) }
    var ordem by rememberSaveable(m, especial) { mutableStateOf(ORDEM_DOS_NOTAVEIS) }
    val todos = remember(daPagina, ordem) { ConcursosNotaveis.ordenar(daPagina, ordem.coluna, ordem.crescente) }
    var quantos by rememberSaveable(m, especial) { mutableIntStateOf(ConcursosNotaveis.QUANTOS) }
    val vistos = quantos.coerceIn(1, todos.size.coerceAtLeast(1))
    val lista = todos.take(vistos)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(ConcursosNotaveis.resumo(daPagina), style = MaterialTheme.typography.bodyMedium)
        if (todos.isEmpty()) return@Column
        if (todos.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ConcursosNotaveis.QUANTOS_VER, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(textoDosVistos(vistos, todos.size), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = vistos.toFloat(),
                onValueChange = { quantos = it.roundToInt().coerceIn(1, todos.size) },
                valueRange = 1f..todos.size.toFloat(),
                modifier = Modifier.semantics { contentDescription = ConcursosNotaveis.QUANTOS_VER },
            )
        }
        TabelaRolavel(
            primeira = ConcursosNotaveis.COLUNA_CONCURSO,
            grupos = emptyList(),
            colunas = listOf(ConcursosNotaveis.COLUNA_PREMIO, ConcursosNotaveis.COLUNA_GANHADORES),
            linhas = lista.map { r ->
                (r.numero.toString() + (r.dia?.let { " (${it.year})" } ?: "")) to
                    listOf(valorSemQuebra(PremiosTipicos.valor(r.total)), ConcursosNotaveis.milhar(r.ganhadores))
            },
            deFora = OrdenacaoDeFora(ordem) { j -> ordem = proximaOrdem(ordem, j) },
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

/** A ordem de começo dos Concursos notáveis: os maiores prêmios primeiro (desde a 0.38). */
val ORDEM_DOS_NOTAVEIS = OrdemDaTabela(coluna = 0, crescente = false)

/** A ordem de começo dos Números notáveis: os que mais saíram primeiro (desde a 0.38). */
val ORDEM_DOS_NUMEROS = OrdemDaTabela(coluna = 0, crescente = false)

/**
 * Tocar no título da coluna [j] (-1 = a primeira) das tabelas que se ordenam por fora (desde a 0.38): a mesma coluna
 * inverte; outra começa do maior para o menor (o concurso mais novo, o prêmio maior, o número que mais saiu).
 */
fun proximaOrdem(atual: OrdemDaTabela, j: Int): OrdemDaTabela =
    if (atual.coluna == j) atual.copy(crescente = !atual.crescente) else OrdemDaTabela(j, crescente = false)

/** "20 de 812" (quantos concursos a tabela mostra). */
fun textoDosVistos(vistos: Int, total: Int): String = "$vistos de ${ConcursosNotaveis.milhar(total)}"

@Composable
private fun LendoOHistorico() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text("  Lendo o histórico...", style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Uma tabela dos Números notáveis (desde a 0.38; e os Times, Meses, Trevos e Resultados notáveis): todos os itens, dos
 * que mais saíram aos que menos, com as vezes, a % dos concursos e o atraso. Tocar no título de uma coluna ordena.
 */
@Composable
private fun TabelaDosNumeros(m: Modalidade, grupo: NumerosNotaveis.Grupo, especial: Boolean) {
    var ordem by rememberSaveable(m, especial, grupo.titulo) { mutableStateOf(ORDEM_DOS_NUMEROS) }
    val loteca = m.formato == Formato.LOTECA
    // Na Loteca a % é dos jogos (14 por concurso); nas outras, dos concursos.
    val total = if (loteca) grupo.itens.sumOf { it.vezes } else grupo.concursos
    val comAtraso = grupo.itens.any { it.atraso != null }
    val itens = remember(grupo, ordem) {
        val chave: (NumerosNotaveis.Item) -> Long = when (ordem.coluna) {
            -1 -> { i -> i.ordem }
            2 -> { i -> (i.atraso ?: Int.MAX_VALUE).toLong() }
            else -> { i -> i.vezes.toLong() }
        }
        grupo.itens.sortedWith((if (ordem.crescente) compareBy(chave) else compareByDescending(chave)).thenBy { it.ordem })
    }
    // As linhas prontas (desde a 0.39 lembradas: o nome de cada time era procurado de novo a cada desenho).
    val linhas = remember(itens, total, comAtraso) {
        itens.map { i ->
            rotuloDoNotavel(grupo, i) to listOfNotNull(
                ConcursosNotaveis.milhar(i.vezes),
                NumerosNotaveis.porcentagem(i.vezes, total),
                (i.atraso?.let { ConcursosNotaveis.milhar(it) } ?: "—").takeIf { comAtraso },
            )
        }
    }
    val colunas = remember(comAtraso) {
        listOfNotNull(NumerosNotaveis.COLUNA_VEZES, NumerosNotaveis.COLUNA_PORCENTAGEM, NumerosNotaveis.COLUNA_ATRASO.takeIf { comAtraso })
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(textoDosConcursos(grupo, loteca), style = MaterialTheme.typography.bodyMedium)
        if (itens.isEmpty()) return@Column
        TabelaRolavel(
            primeira = grupo.coluna,
            grupos = emptyList(),
            colunas = colunas,
            linhas = linhas,
            // A coluna da % ordena como as vezes.
            deFora = OrdenacaoDeFora(ordem) { j -> ordem = proximaOrdem(ordem, if (j == 1) 0 else j) },
        )
        listOfNotNull(NumerosNotaveis.NOTA_DA_PORCENTAGEM, NumerosNotaveis.NOTA_DO_ATRASO.takeIf { comAtraso }).forEach { n ->
            Text("* $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** O rótulo da primeira coluna: o número; na Timemania, o nome do time como na lista ("Retrô / PE"). */
private fun rotuloDoNotavel(grupo: NumerosNotaveis.Grupo, i: NumerosNotaveis.Item): String =
    if (grupo.titulo == NumerosNotaveis.TITULO_TIMES) nomeDoTimeSorteado(i.rotulo) else i.rotulo

/** "Nos 3.067 concursos da página." / "Nos 17.934 jogos dos 1.281 concursos." */
fun textoDosConcursos(g: NumerosNotaveis.Grupo, loteca: Boolean): String = when {
    g.concursos == 0 -> "Nenhum concurso no histórico."
    loteca -> "Nos ${ConcursosNotaveis.milhar(g.itens.sumOf { it.vezes })} jogos dos ${ConcursosNotaveis.milhar(g.concursos)} concursos."
    g.concursos == 1 -> "No único concurso da página."
    else -> "Nos ${ConcursosNotaveis.milhar(g.concursos)} concursos da página."
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

package io.github.marcosfaria94.bolao.ui.jogos

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Etapa
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Formato
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Garantia
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Modo
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Pedido
import io.github.marcosfaria94.bolao.domain.ConfigDaCalculadora
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** O endereço da calculadora de probabilidades (desde a 0.38): "calculadora/MEGA_SENA". */
const val ROTA_CALCULADORA = "calculadora/{nome}"
const val TITULO_CALCULADORA = "Calculadora"

fun rotaDaCalculadora(m: Modalidade): String = "calculadora/${m.name}"

/** A calculadora existe em todas as modalidades menos a Federal. */
fun temCalculadora(m: Modalidade): Boolean = Calculadora.tem(m)

/** A legenda do "Qualquer prêmio" (desde a 0.40). */
const val LEGENDA_QUALQUER = "Todas as faixas juntas"

/** Os textos e nomes da calculadora em etapas (os testes procuram por eles; desde a 0.42). */
const val ETAPA_COMO = "Cálculo"
const val ETAPA_RESULTADO = "Resultado"
const val CAMPO_DA_ETAPA = "Valor da etapa"
const val AJUDA_DA_CALCULADORA = "Como usar a calculadora"
const val NOVO_CALCULO = "Novo cálculo"
const val CONTINUAR = "Continuar"
const val VER_RESULTADO = "Ver resultado"

/** O nome de cada passo da barra de cima (para quem não vê a tela e os testes): "Etapa 2: Faixa". */
fun nomeDoPasso(numero: Int, titulo: String): String = "Etapa $numero: $titulo"

/** O nome do botão de cada modalidade no seletor de cima: "Calcular na Quina". */
fun nomeDaModalidadeNaCalculadora(m: Modalidade): String = "Calcular na ${m.nome}"

/**
 * A configuração da calculadora da 0.38 à 0.41 (a dos controles e a do teclado), guardada no celular: a calculadora em
 * etapas (0.42) não usa mais; fica para ler o que já estava guardado (e os testes antigos).
 */
object GuardaDaCalculadora {
    private const val PREFS = "calculadora"
    private val json = Json { ignoreUnknownKeys = true }

    fun ler(ctx: Context, m: Modalidade): ConfigDaCalculadora =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(m.name, null)
            ?.let { runCatching { json.decodeFromString(ConfigDaCalculadora.serializer(), it) }.getOrNull() }
            ?: ConfigDaCalculadora()

    fun gravar(ctx: Context, m: Modalidade, c: ConfigDaCalculadora) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(m.name, json.encodeToString(ConfigDaCalculadora.serializer(), c)).apply()
    }
}

/** A porcentagem na tela (desde a 0.41, pedido do usuário): o "~" um pouco afastado do número ("~ 3,31%", espaço fino). */
fun porcentagemNaTela(p: Double): String = Calculadora.porcentagem(p).replace(Calculadora.APROXIMADO, Calculadora.APROXIMADO + "\u2009")

/** As notas do fim da calculadora (como a conta é feita). */
fun notasDaCalculadora(m: Modalidade): List<String> = listOfNotNull(
    "A chance de acertar cada faixa pelo menos uma vez, com todas essas apostas no mesmo concurso e diferentes umas das outras. " +
        "Na faixa principal a conta é exata (as chances se somam); nas outras, as apostas contam como feitas ao acaso.",
    "Na Dupla Sena cada aposta concorre nos 2 sorteios: a chance é a de ganhar em pelo menos um deles.".takeIf { m == Modalidade.DUPLA_SENA },
    "Na Loteca, supondo os 3 resultados igualmente prováveis em cada jogo.".takeIf { m == Modalidade.LOTECA },
    Probabilidades.LEGENDA_DA_MAIS_MILIONARIA.takeIf { m == Modalidade.MAIS_MILIONARIA },
)

/** O pedido guardado na rotação da tela (e ao voltar do fundo). */
private val GuardaDoPedido = listSaver<Pedido, String>(
    save = { p ->
        listOf(
            p.modo?.name.orEmpty(), p.faixa?.toString().orEmpty(), p.formato.name, p.valor,
            p.numeros?.toString().orEmpty(), p.trevos?.toString().orEmpty(),
            p.garantia?.acertos?.toString().orEmpty(), p.garantia?.sorteados?.toString().orEmpty(),
        )
    },
    restore = { l ->
        Pedido(
            modo = Modo.entries.firstOrNull { it.name == l[0] },
            faixa = l[1].toIntOrNull(),
            formato = Formato.entries.firstOrNull { it.name == l[2] } ?: Formato.UM_EM,
            valor = l[3],
            numeros = l[4].toIntOrNull(),
            trevos = l[5].toIntOrNull(),
            garantia = l[6].toIntOrNull()?.let { a -> l[7].toIntOrNull()?.let { s -> Garantia(a, s) } },
        )
    },
)

/**
 * A calculadora de probabilidades, refeita na 0.42 em etapas que ocupam a tela inteira (pedido do usuário: "faz ser
 * possível trocar a modalidade a qualquer momento; trate como fluxo de etapas de cálculo (1, 2, 3) que ocupa a tela
 * inteira e dê sempre para retroceder/mudar a escolha anterior; o teclado de calculadora é uma ideia ruim"):
 * - **em cima**, as modalidades (trocar a qualquer momento: as escolhas que valem na outra ficam) e a barra das etapas
 *   (cada etapa feita mostra a escolha e, tocada, volta a ela para mudar; o "?" ensina a usar);
 * - **no meio**, a etapa: como calcular (P, J, Q, $, F, D), a faixa, o número (com o teclado do celular e sugestões), o
 *   tipo de jogo ou a garantia do fechamento; no fim, o resultado (o preço, os jogos, as chances de cada faixa e, quando
 *   faz sentido, os prêmios do mínimo ao máximo e os jogos do fechamento ou do desdobramento);
 * - **embaixo**, voltar e continuar.
 * As contas ficam em [CalculoEmEtapas].
 */
@Composable
fun CalculadoraTela(estado: Estado, inicial: Modalidade) {
    var nome by rememberSaveable { mutableStateOf(inicial.name) }
    val m = Modalidade.entries.firstOrNull { it.name == nome } ?: inicial
    var pedido by rememberSaveable(stateSaver = GuardaDoPedido) { mutableStateOf(Pedido()) }
    var passo by rememberSaveable { mutableIntStateOf(0) }
    var ajuda by rememberSaveable { mutableStateOf(false) }
    val preco = estado.config.preco(m)
    val etapas = pedido.modo?.let { CalculoEmEtapas.etapas(it) }.orEmpty()
    val ultimo = etapas.size + 1
    // Nunca além da primeira etapa sem escolha.
    val agora = passo.coerceAtMost(CalculoEmEtapas.etapaAtual(m, pedido))

    fun irPara(i: Int) {
        passo = i.coerceIn(0, CalculoEmEtapas.etapaAtual(m, pedido))
    }
    fun escolher(novo: Pedido, naEtapa: Int) {
        pedido = novo
        passo = CalculoEmEtapas.depoisDe(m, novo, naEtapa)
    }
    fun trocarModalidade(nova: Modalidade) {
        if (nova == m) return
        val p = CalculoEmEtapas.naModalidade(pedido, m, nova)
        nome = nova.name
        pedido = p
        passo = passo.coerceAtMost(CalculoEmEtapas.etapaAtual(nova, p))
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        SeletorDeModalidade(m, ::trocarModalidade)
        BarraDeEtapas(m, pedido, agora, aoTocar = ::irPara, aoAjudar = { ajuda = true })
        HorizontalDivider()
        AnimatedContent(
            targetState = agora,
            transitionSpec = {
                val ida = targetState > initialState
                (slideInHorizontally { if (ida) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (ida) -it / 4 else it / 4 } + fadeOut())
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "etapa",
        ) { i ->
            val etapa = etapas.getOrNull(i - 1)
            when {
                i == 0 -> EtapaComoCalcular(m, pedido.modo) { escolher(CalculoEmEtapas.comModo(pedido, it), 0) }
                i >= ultimo -> EtapaDoResultado(m, pedido, preco)
                etapa == Etapa.FAIXA -> EtapaDaFaixa(m, pedido) { escolher(CalculoEmEtapas.comFaixa(m, pedido, it), i) }
                etapa == Etapa.TIPO -> EtapaDoTipo(m, pedido, preco) { n, t -> escolher(pedido.copy(numeros = n, trevos = t), i) }
                etapa == Etapa.GARANTIA -> EtapaDaGarantia(m, pedido, preco) { escolher(pedido.copy(garantia = it), i) }
                etapa != null -> EtapaDoNumero(
                    m, pedido, etapa,
                    aoMudar = { pedido = it },
                    aoContinuar = { if (CalculoEmEtapas.completa(m, pedido, etapa)) passo = CalculoEmEtapas.depoisDe(m, pedido, i) },
                )
            }
        }
        RodapeDaCalculadora(
            m, pedido, agora, ultimo,
            aoVoltar = { irPara(agora - 1) },
            aoRecomecar = { pedido = Pedido(formato = pedido.formato); passo = 0 },
            aoContinuar = {
                val e = etapas.getOrNull(agora - 1)
                if (e != null && CalculoEmEtapas.completa(m, pedido, e)) passo = CalculoEmEtapas.depoisDe(m, pedido, agora)
            },
        )
    }
    if (ajuda) AjudaDaCalculadora(m) { ajuda = false }
}

// ---------------------------------------------------------------- em cima

/** As modalidades, uma ao lado da outra (rola de lado): a escolhida com a cor dela; tocar troca a qualquer momento. */
@Composable
private fun SeletorDeModalidade(m: Modalidade, aoTrocar: (Modalidade) -> Unit) {
    val lista = remember { Modalidade.ATIVAS.filter { Calculadora.tem(it) } }
    val rolagem = rememberLazyListState(initialFirstVisibleItemIndex = (lista.indexOf(m) - 1).coerceAtLeast(0))
    LazyRow(
        state = rolagem,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lista, key = { it.name }) { x ->
            val escolhida = x == m
            val cor = Color(x.cor)
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (escolhida) cor else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(role = Role.Tab) { aoTrocar(x) }
                    .semantics { contentDescription = nomeDaModalidadeNaCalculadora(x); selected = escolhida }
                    .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeloModalidade(x, tamanho = 26.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    x.nome,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (escolhida) FontWeight.Bold else FontWeight.Medium,
                    color = if (escolhida) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** O que fica escrito embaixo de cada etapa feita (a escolha dela). */
private fun escolhaDaEtapa(m: Modalidade, p: Pedido, etapa: Etapa): String? = when (etapa) {
    Etapa.FAIXA -> p.faixa?.let { f -> if (f == Calculadora.QUALQUER_PREMIO) "Qualquer" else Calculadora.faixas(m).getOrNull(f) }
    Etapa.TIPO -> p.numeros?.let { n -> if (p.trevos != null && m.trevos > 0) "$n + ${p.trevos}" else "$n números" }
    Etapa.GARANTIA -> p.garantia?.let { g -> CalculoEmEtapas.nomeDaGarantia(m, g).replace(" · se sair ", " · ") }
    else -> CalculoEmEtapas.numero(p.valor)?.takeIf { CalculoEmEtapas.erroDoValor(m, p) == null }?.let { v ->
        when (p.modo) {
            Modo.PROBABILIDADE -> if (p.formato == Formato.PORCENTO) Calculadora.porcentagem(v / 100) else "1 em " + curto(v)
            Modo.ORCAMENTO -> "R$ " + curto(v)
            Modo.FECHAMENTO, Modo.DESDOBRAMENTO -> "${v.toInt()} números"
            else -> curto(v)
        }
    }
}

/** "1 mi", "250 mil", "1.500": o número curto, para caber embaixo da etapa. */
private fun curto(v: Double): String = when {
    v >= 1e9 -> Calculadora.numeroDoUmEm(v / 1e9).removeSuffix(",0") + " bi"
    v >= 1e6 -> Calculadora.numeroDoUmEm(v / 1e6).removeSuffix(",0") + " mi"
    v >= 1e4 -> Calculadora.numeroDoUmEm(v / 1e3).removeSuffix(",0") + " mil"
    v == Math.floor(v) -> Probabilidades.milhares(v.toLong())
    else -> Calculadora.numeroDoUmEm(v)
}

/**
 * A barra das etapas: um círculo numerado por etapa (a feita com o visto, a de agora em dourado), o nome e, embaixo, a
 * escolha. Tocar numa etapa feita volta a ela (para mudar a escolha); as de depois esperam. O "?" no fim.
 */
@Composable
private fun BarraDeEtapas(m: Modalidade, p: Pedido, agora: Int, aoTocar: (Int) -> Unit, aoAjudar: () -> Unit) {
    val etapas = p.modo?.let { CalculoEmEtapas.etapas(it) }
    val titulos = listOf(ETAPA_COMO) + (etapas?.map { it.titulo } ?: listOf("Detalhes")) + ETAPA_RESULTADO
    val alcance = CalculoEmEtapas.etapaAtual(m, p)
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 0.dp, bottom = 6.dp), verticalAlignment = Alignment.Top) {
        titulos.forEachIndexed { i, titulo ->
            val feita = i < alcance && i != agora
            val atual = i == agora
            val pode = i <= alcance && !atual && (etapas != null || i == 0)
            val escolha = when {
                i == 0 -> p.modo?.titulo
                etapas != null && i in 1..etapas.size -> escolhaDaEtapa(m, p, etapas[i - 1])
                else -> null
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = pode, role = Role.Tab) { aoTocar(i) }
                    .semantics {
                        contentDescription = nomeDoPasso(i + 1, titulo) + (escolha?.let { ": $it" } ?: "")
                        selected = atual
                        if (!pode && !atual) disabled()
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fundo = when {
                    atual -> Dourado
                    feita -> AzulMarinho
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
                Box(Modifier.size(28.dp).clip(CircleShape).background(fundo), contentAlignment = Alignment.Center) {
                    if (feita && i < titulos.lastIndex) {
                        Icon(Icons.Outlined.Check, null, tint = Dourado, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            "${i + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = when {
                                atual -> AzulMarinho
                                feita -> Color.White
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (atual) FontWeight.Bold else FontWeight.Normal,
                    color = if (atual || feita) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    escolha ?: " ",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (atual) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = aoAjudar, modifier = Modifier.padding(top = 2.dp)) {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, AJUDA_DA_CALCULADORA)
        }
    }
}

// ---------------------------------------------------------------- as etapas

/** O título de cada etapa (a pergunta) e a explicação embaixo. */
@Composable
private fun Pergunta(texto: String, explicacao: String? = null) {
    Text(texto, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (explicacao != null) {
        Text(explicacao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
    Spacer(Modifier.height(12.dp))
}

/** Uma opção grande de uma etapa (cartão tocável): a escolhida (a de antes, ao voltar) com a borda dourada. */
@Composable
private fun Opcao(
    escolhida: Boolean,
    habilitada: Boolean = true,
    aoTocar: () -> Unit,
    descricao: String? = null,
    conteudo: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (escolhida) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (escolhida) BorderStroke(2.dp, Dourado) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = habilitada, role = Role.RadioButton) { aoTocar() }
            .semantics {
                selected = escolhida
                if (descricao != null) contentDescription = descricao
                if (!habilitada) disabled()
            },
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { conteudo() }
    }
}

/** A coluna que rola de cada etapa. */
@Composable
private fun ColunaDaEtapa(conteudo: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) { conteudo() }
}

/** Etapa 1: como calcular. Os 6 jeitos, com a letra, o que faz e um exemplo; os que não valem na modalidade, apagados. */
@Composable
private fun EtapaComoCalcular(m: Modalidade, atual: Modo?, aoEscolher: (Modo) -> Unit) {
    ColunaDaEtapa {
        Pergunta("Como você quer calcular?", "Escolha o ponto de partida. Dá para voltar e trocar a qualquer momento.")
        Modo.entries.forEach { modo ->
            val motivo = CalculoEmEtapas.motivoSemModo(m, modo)
            Opcao(escolhida = modo == atual, habilitada = motivo == null, aoTocar = { aoEscolher(modo) }, descricao = "${modo.letra} ${modo.titulo}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (motivo == null) AzulMarinho else MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(modo.letra, color = if (motivo == null) Dourado else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(modo.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (motivo == null) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(motivo ?: modo.descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (motivo == null) {
                            Text("Ex.: ${modo.exemplo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

/** A etapa da faixa: as faixas, com a chance de 1 aposta simples ao lado (na probabilidade) ou o que dá (nos prêmios). */
@Composable
private fun EtapaDaFaixa(m: Modalidade, p: Pedido, aoEscolher: (Int) -> Unit) {
    val modo = p.modo ?: return
    val simples = remember(m) { Calculadora.chances(m, ConfigDaCalculadora()) }
    ColunaDaEtapa {
        if (modo == Modo.PREMIOS) {
            Pergunta("Em que faixa?", "A quantidade de prêmios que você quer receber nela, se acertar.")
        } else {
            Pergunta("Em que faixa?", "A chance que você vai pedir é a de acertar esta faixa pelo menos uma vez.")
        }
        CalculoEmEtapas.faixasDoModo(m, modo).forEach { (i, nome) ->
            val qualquer = i == Calculadora.QUALQUER_PREMIO
            Opcao(escolhida = p.faixa == i, aoTocar = { aoEscolher(i) }, descricao = "Faixa $nome") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val sub = when {
                            qualquer -> LEGENDA_QUALQUER
                            modo == Modo.PREMIOS -> CalculoEmEtapas.premiosDaFaixa(m, m.qtdAposta + 1, i)?.let {
                                "Com ${m.qtdAposta + 1} números: ${it.first} se acertar só a ${nome.lowercase()}"
                            }
                            else -> null
                        }
                        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (modo == Modo.PROBABILIDADE) {
                        val c = simples.firstOrNull { it.indice == i }?.p ?: 0.0
                        Column(horizontalAlignment = Alignment.End) {
                            Text("1 aposta simples", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Calculadora.umEm(c), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/** A pergunta e o jeito de cada etapa de número. */
private data class JeitoDoNumero(val pergunta: String, val explicacao: String?, val prefixo: String?, val sufixo: String?, val decimal: Boolean)

private fun jeitoDoNumero(m: Modalidade, p: Pedido): JeitoDoNumero = when (p.modo) {
    Modo.PROBABILIDADE -> {
        val nome = if (p.faixa == Calculadora.QUALQUER_PREMIO) "algum prêmio" else Calculadora.faixas(m).getOrNull(p.faixa ?: 0)?.lowercase() ?: ""
        JeitoDoNumero(
            "Qual chance você quer na $nome?",
            "A calculadora monta a combinação de jogos mais barata que chega lá.",
            if (p.formato == Formato.UM_EM) "1 em" else null,
            if (p.formato == Formato.PORCENTO) "%" else null,
            true,
        )
    }
    Modo.JOGOS -> JeitoDoNumero("Quantos jogos?", "Na etapa seguinte, o tipo de jogo (6 números, 7 números...).", null, "jogos", false)
    Modo.PREMIOS -> JeitoDoNumero(
        "Quantos ${CalculoEmEtapas.plural(m, p.faixa ?: 0)}?",
        "Se acertar a ${Calculadora.faixas(m).getOrNull(p.faixa ?: 0)?.lowercase()}, a aposta tem que dar pelo menos esse tanto.",
        null, null, false,
    )
    Modo.ORCAMENTO -> JeitoDoNumero("Quanto você quer gastar?", "Na etapa seguinte, o tipo de jogo: a calculadora mostra quantos cabem e o troco.", "R$", null, true)
    Modo.FECHAMENTO -> JeitoDoNumero(
        "Com quantos números?",
        "Os números que você vai escolher (de ${CalculoEmEtapas.faixaDeNumeros(m, Modo.FECHAMENTO)?.first} a ${CalculoEmEtapas.faixaDeNumeros(m, Modo.FECHAMENTO)?.last}). Os jogos do fechamento saem deles.",
        null, "números", false,
    )
    Modo.DESDOBRAMENTO -> JeitoDoNumero(
        "Com quantos números?",
        "Todos os jogos de ${m.qtdAposta} números com eles (de ${CalculoEmEtapas.faixaDeNumeros(m, Modo.DESDOBRAMENTO)?.first} a ${CalculoEmEtapas.faixaDeNumeros(m, Modo.DESDOBRAMENTO)?.last}).",
        null, "números", false,
    )
    null -> JeitoDoNumero("", null, null, null, false)
}

/**
 * A etapa do número (a chance, os jogos, os prêmios, o dinheiro ou os números): o campo com o teclado do celular (não o
 * da calculadora), o "1 em X" ou "%" na chance, sugestões de toque e o erro embaixo. Continuar no teclado ou embaixo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtapaDoNumero(m: Modalidade, p: Pedido, etapa: Etapa, aoMudar: (Pedido) -> Unit, aoContinuar: () -> Unit) {
    val jeito = jeitoDoNumero(m, p)
    val erro = CalculoEmEtapas.erroDoValor(m, p)
    val foco = remember { FocusRequester() }
    LaunchedEffect(etapa) { runCatching { foco.requestFocus() } }
    ColunaDaEtapa {
        Pergunta(jeito.pergunta, jeito.explicacao)
        if (p.modo == Modo.PROBABILIDADE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                FilterChip(selected = p.formato == Formato.UM_EM, onClick = { aoMudar(p.copy(formato = Formato.UM_EM, valor = "")) }, label = { Text("1 em X") })
                FilterChip(selected = p.formato == Formato.PORCENTO, onClick = { aoMudar(p.copy(formato = Formato.PORCENTO, valor = "")) }, label = { Text("Porcentagem") })
            }
        }
        OutlinedTextField(
            value = p.valor,
            onValueChange = { novo -> aoMudar(p.copy(valor = novo.filter { it.isDigit() || (jeito.decimal && (it == ',' || it == '.')) }.take(14))) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            prefix = jeito.prefixo?.let { { Text("$it ", style = MaterialTheme.typography.headlineSmall) } },
            suffix = jeito.sufixo?.let { { Text(" $it", style = MaterialTheme.typography.titleMedium) } },
            isError = erro != null,
            supportingText = {
                val v = CalculoEmEtapas.numero(p.valor)
                Text(
                    erro ?: when {
                        v == null -> dicaDoCampo(m, p)
                        p.modo == Modo.PROBABILIDADE && p.formato == Formato.UM_EM -> "1 em " + Calculadora.numeroDoUmEm(v)
                        p.modo == Modo.ORCAMENTO -> Dinheiro.rs(Math.round(v * 100))
                        else -> dicaDoCampo(m, p)
                    },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = if (jeito.decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { aoContinuar() }),
            modifier = Modifier.fillMaxWidth().focusRequester(foco).semantics { contentDescription = CAMPO_DA_ETAPA },
        )
        Spacer(Modifier.height(8.dp))
        Text("Sugestões", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            CalculoEmEtapas.sugestoes(m, p).forEach { (valor, rotulo) ->
                FilterChip(selected = p.valor == valor, onClick = { aoMudar(p.copy(valor = valor)) }, label = { Text(rotulo) })
            }
        }
    }
}

/** A dica embaixo do campo, sem nada digitado. */
private fun dicaDoCampo(m: Modalidade, p: Pedido): String = when (p.modo) {
    Modo.PROBABILIDADE -> if (p.formato == Formato.UM_EM) "Ex.: 1000000 para 1 em 1 milhão" else "Ex.: 5 para 5%"
    Modo.JOGOS -> "De 1 em diante"
    Modo.PREMIOS -> "Ex.: 10"
    Modo.ORCAMENTO -> "Aposta simples: ${m.nome} a partir de ${Dinheiro.rs(m.precoPadrao)}"
    else -> CalculoEmEtapas.faixaDeNumeros(m, p.modo!!)?.let { "De ${it.first} a ${it.last}" } ?: ""
}

/** A etapa do tipo de jogo: cada tipo com o preço de uma aposta e o total (nos jogos) ou quantos cabem (no orçamento). */
@Composable
private fun EtapaDoTipo(m: Modalidade, p: Pedido, preco: Long, aoEscolher: (Int, Int) -> Unit) {
    val opcoes = remember(m, p.modo, p.valor, preco) { CalculoEmEtapas.opcoesDeTipo(m, p, preco) }
    ColunaDaEtapa {
        Pergunta("Que tipo de jogo?", if (p.modo == Modo.ORCAMENTO) "Quantos cabem no orçamento, de cada tipo." else "O preço de tudo, com cada tipo.")
        opcoes.forEachIndexed { i, o ->
            if (i == 1) {
                Text(Calculadora.tituloDosMaisNumeros(m), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            }
            val nome = Calculadora.nomeDaLinha(m, o.linha.numeros, o.linha.trevos)
            val cabe = o.jogos > 0
            Opcao(
                escolhida = p.numeros == o.linha.numeros && (p.trevos ?: o.linha.trevos) == o.linha.trevos,
                habilitada = cabe,
                aoTocar = { aoEscolher(o.linha.numeros, o.linha.trevos) },
                descricao = "Tipo $nome",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${Dinheiro.rs(o.linha.apostasSimples * preco)} cada", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (p.modo == Modo.ORCAMENTO) {
                            Text(if (cabe) Calculadora.quantas(o.jogos) else "Não cabe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (cabe) Text(if (o.sobra > 0) "sobra ${Dinheiro.rs(o.sobra)}" else "sem sobra", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(Dinheiro.rs(o.custo), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(Calculadora.quantas(o.jogos), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Quantos jogos cada garantia pede (a conta é feita fora da linha da tela; null enquanto calcula, -1 = grande demais). */
@Composable
private fun jogosDasGarantias(m: Modalidade, numeros: Int): Map<Garantia, Int>? {
    val r by produceState<Map<Garantia, Int>?>(null, m, numeros) {
        value = withContext(Dispatchers.Default) {
            CalculoEmEtapas.garantias(m, numeros).flatMap { it.second }.associateWith { g ->
                CalculoEmEtapas.fechamento(numeros, m.qtdAposta, g)?.size ?: -1
            }
        }
    }
    return r
}

/** A etapa da garantia do fechamento: por faixa, de quantos sorteados, com quantos jogos e a chance da condição. */
@Composable
private fun EtapaDaGarantia(m: Modalidade, p: Pedido, preco: Long, aoEscolher: (Garantia) -> Unit) {
    val n = CalculoEmEtapas.numero(p.valor)?.toInt() ?: return
    val jogos = jogosDasGarantias(m, n)
    ColunaDaEtapa {
        Pergunta("Que garantia?", "Um prêmio garantido se parte dos números sorteados estiver entre os seus $n. Quanto mais forte a garantia, mais jogos.")
        CalculoEmEtapas.garantias(m, n).forEach { (acertos, lista) ->
            val faixa = CalculoEmEtapas.indiceDosAcertos(m, acertos)?.let { Calculadora.faixas(m)[it] } ?: "$acertos acertos"
            Text(faixa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            lista.forEach { g ->
                val j = jogos?.get(g)
                val condicao = CalculoEmEtapas.chanceDaCondicao(m, n, g.sorteados)
                Opcao(
                    escolhida = p.garantia == g,
                    habilitada = j != null && j > 0,
                    aoTocar = { aoEscolher(g) },
                    descricao = "Garantia ${CalculoEmEtapas.nomeDaGarantia(m, g)}",
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Se sair ${g.sorteados} dos ${m.qtdSorteada}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Isso acontece ${Calculadora.umEm(condicao)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            when {
                                j == null -> Text("calculando…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                j < 0 -> Text("conta grande demais", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> {
                                    Text(Calculadora.quantas(j).replace("aposta", "jogo"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(Dinheiro.rs(j * preco), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            "O fechamento é montado pela calculadora (escolhendo, a cada passo, o jogo que cobre mais casos). Nem sempre é o menor que existe, mas a garantia vale.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---------------------------------------------------------------- o resultado

/** Os jogos do fechamento do pedido (fora da linha da tela). */
@Composable
private fun jogosDoFechamento(m: Modalidade, p: Pedido): List<List<Int>>? {
    val n = CalculoEmEtapas.numero(p.valor)?.toInt()
    val g = p.garantia
    val r by produceState<List<List<Int>>?>(null, m, n, g) {
        value = if (p.modo != Modo.FECHAMENTO || n == null || g == null) null else withContext(Dispatchers.Default) { CalculoEmEtapas.fechamento(n, m.qtdAposta, g) }
    }
    return r
}

/**
 * O resultado: o cartão de cima no azul com o dourado (o preço grande, quantos jogos e de que tipo, o pedido), as chances
 * de cada faixa (a do pedido em destaque), os prêmios de cada faixa (do mínimo ao máximo) quando é uma aposta só, e os
 * jogos do fechamento ou do desdobramento (pela posição dos seus números: 01 = o 1º que você escolher).
 */
@Composable
private fun EtapaDoResultado(m: Modalidade, p: Pedido, preco: Long) {
    val fechamento = jogosDoFechamento(m, p)
    val r = remember(m, p, preco, fechamento?.size) { CalculoEmEtapas.resultado(m, p, preco, fechamento?.size) }
    ColunaDaEtapa {
        if (r == null) {
            Text(
                if (p.modo == Modo.FECHAMENTO && fechamento == null) "Montando o fechamento…" else "Não dá para chegar nesse pedido: volte e mude a escolha.",
                style = MaterialTheme.typography.bodyLarge,
            )
            return@ColunaDaEtapa
        }
        CartaoDoResultado(m, r)
        Spacer(Modifier.height(14.dp))
        Text("Chance em cada faixa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        r.chances.forEach { c ->
            val destaque = c.indice == r.destaque
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (destaque) Dourado.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow)
                    .then(if (destaque) Modifier.border(1.5.dp, Dourado, RoundedCornerShape(10.dp)) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = if (destaque) FontWeight.Bold else FontWeight.Medium)
                    r.premios?.get(c.indice)?.let { pr ->
                        Text(textoDosPremios(m, c.indice, pr), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Calculadora.umEm(c.p), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(porcentagemNaTela(c.p), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (r.garantia != null && r.numerosEscolhidos != null) {
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Garantia", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(CalculoEmEtapas.textoDaGarantia(m, r.garantia, r.numerosEscolhidos), style = MaterialTheme.typography.bodyMedium)
                    r.chanceDaCondicao?.let {
                        Text("A condição acontece ${Calculadora.umEm(it)} (${porcentagemNaTela(it)}).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        val lista = when (p.modo) {
            Modo.FECHAMENTO -> fechamento
            Modo.DESDOBRAMENTO -> r.numerosEscolhidos?.let { n -> CalculoEmEtapas.desdobramento((0 until n).toList(), m.qtdAposta, LIMITE_DE_JOGOS_NA_TELA) }
            else -> null
        }
        if (lista != null) ListaDeJogos(m, lista, r.jogos)
        Spacer(Modifier.height(10.dp))
        notasDaCalculadora(m).forEach { n ->
            Text("* $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** "Acertando só a quadra: 3 · no máximo 5". */
private fun textoDosPremios(m: Modalidade, faixa: Int, pr: LongRange): String {
    val nome = Calculadora.faixas(m)[faixa].lowercase()
    val plural = CalculoEmEtapas.plural(m, faixa)
    val quantos = if (pr.first == 1L) "1 " + (if (plural.startsWith("prêmios")) plural.replaceFirst("prêmios", "prêmio") else nome) else "${Probabilidades.milhares(pr.first)} $plural"
    return if (pr.first == pr.last) "$quantos se acertar a $nome"
    else "Só a $nome: ${Probabilidades.milhares(pr.first)} · no máximo ${Probabilidades.milhares(pr.last)}"
}

private const val LIMITE_DE_JOGOS_NA_TELA = 60

/** O cartão de cima do resultado (azul com o dourado, como o visor da 0.41). */
@Composable
private fun CartaoDoResultado(m: Modalidade, r: CalculoEmEtapas.Resultado) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AzulMarinho)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeloModalidade(m, tamanho = 22.dp)
            Spacer(Modifier.width(8.dp))
            Text(m.nome, color = Color(0xFFB9C6DA), style = MaterialTheme.typography.labelLarge)
        }
        Text(Dinheiro.rs(r.custo), color = Dourado, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp))
        Text(r.tipo, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(r.linha, color = Color(0xFFB9C6DA), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Os jogos do fechamento ou do desdobramento, pela posição dos números escolhidos. */
@Composable
private fun ListaDeJogos(m: Modalidade, jogos: List<List<Int>>, total: Long) {
    Spacer(Modifier.height(12.dp))
    Text("Os jogos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(
        "Pela posição dos seus números: 01 é o 1º que você escolher, 02 o 2º...",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    jogos.take(LIMITE_DE_JOGOS_NA_TELA).forEachIndexed { i, j ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}".padStart(3), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp))
            Text(j.sorted().joinToString("  ") { m.rotulo(it + 1) }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
    if (total > LIMITE_DE_JOGOS_NA_TELA) {
        Text("… e mais ${Probabilidades.milhares(total - LIMITE_DE_JOGOS_NA_TELA)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- embaixo

/** Embaixo: voltar (uma etapa) e continuar (nas etapas de número) ou novo cálculo (no resultado). */
@Composable
private fun RodapeDaCalculadora(
    m: Modalidade,
    p: Pedido,
    agora: Int,
    ultimo: Int,
    aoVoltar: () -> Unit,
    aoRecomecar: () -> Unit,
    aoContinuar: () -> Unit,
) {
    val etapa = p.modo?.let { CalculoEmEtapas.etapas(it) }?.getOrNull(agora - 1)
    val deNumero = etapa != null && etapa !in setOf(Etapa.FAIXA, Etapa.TIPO, Etapa.GARANTIA)
    if (agora == 0 && p.modo == null) return
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (agora > 0) {
            TextButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Voltar")
            }
        }
        Spacer(Modifier.weight(1f))
        when {
            agora >= ultimo -> OutlinedButton(onClick = aoRecomecar) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(NOVO_CALCULO)
            }
            deNumero -> {
                val ok = CalculoEmEtapas.completa(m, p, etapa!!)
                val vaiAoFim = CalculoEmEtapas.depoisDe(m, p, agora) >= ultimo
                Button(
                    onClick = aoContinuar,
                    enabled = ok,
                    colors = ButtonDefaults.buttonColors(containerColor = AzulMarinho, contentColor = Dourado),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) { Text(if (vaiAoFim) VER_RESULTADO else CONTINUAR, fontWeight = FontWeight.Bold) }
            }
            else -> Text("Toque numa opção", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** O "?": como usar a calculadora (as etapas e cada jeito de calcular). */
@Composable
private fun AjudaDaCalculadora(m: Modalidade, aoFechar: () -> Unit) {
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 620.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(AJUDA_DA_CALCULADORA, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A conta é feita em etapas, de cima para baixo: 1, escolha como calcular; 2 e 3, responda as perguntas; no fim, o resultado. " +
                        "As etapas feitas ficam na barra de cima com a sua escolha: toque numa delas para voltar e mudar. " +
                        "A modalidade também troca a qualquer momento, na fileira de cima (a ${m.nome} é a de agora).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Modo.entries.forEach { modo ->
                    Row(Modifier.padding(bottom = 10.dp)) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(AzulMarinho), contentAlignment = Alignment.Center) {
                            Text(modo.letra, color = Dourado, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(modo.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(modo.descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(
                    "O resultado mostra o preço, quantos jogos e de que tipo, a chance de cada faixa e, com uma aposta só, quantos prêmios de cada faixa ela dá " +
                        "(acertando só aquela faixa e no máximo). Os preços são os dos Parâmetros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = aoFechar, modifier = Modifier.align(Alignment.End)) { Text("Entendi") }
            }
        }
    }
}

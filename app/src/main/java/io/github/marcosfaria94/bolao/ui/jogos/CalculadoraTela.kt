package io.github.marcosfaria94.bolao.ui.jogos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import io.github.marcosfaria94.bolao.ui.comum.PuxarParaVoltar
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import androidx.compose.ui.text.TextStyle
import io.github.marcosfaria94.bolao.domain.Tarjas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * O pedido para abrir a calculadora numa modalidade ("calculadora/MEGA_SENA"; nas notificações e nos testes). Até a 0.42 era
 * uma tela; desde a 0.43 ela abre por cima de tudo (o pop-up), sem rota.
 */
const val PREFIXO_DA_CALCULADORA = "calculadora/"
const val TITULO_CALCULADORA = "Calculadora"

fun rotaDaCalculadora(m: Modalidade): String = PREFIXO_DA_CALCULADORA + m.name

/** A modalidade do pedido "calculadora/MEGA_SENA" (null = não é a calculadora, ou sem modalidade: "calculadora/"). */
fun modalidadeDoPedido(r: String): Modalidade? =
    r.removePrefix(PREFIXO_DA_CALCULADORA).takeIf { r.startsWith(PREFIXO_DA_CALCULADORA) }?.let { n -> Modalidade.entries.firstOrNull { it.name == n } }

/** A calculadora existe em todas as modalidades menos a Federal. */
fun temCalculadora(m: Modalidade): Boolean = Calculadora.tem(m)

/** A legenda do "Qualquer prêmio" (desde a 0.40). */
const val LEGENDA_QUALQUER = "Todas as faixas juntas"

/** Os textos e nomes da calculadora (os testes procuram por eles; desde a 0.42, as etapas; desde a 0.43, o pop-up). */
const val ETAPA_MODALIDADE = "Modalidade"
const val ETAPA_COMO = "Cálculo"
const val ETAPA_RESULTADO = "Resultado"
const val PERGUNTA_DA_MODALIDADE = "Em que modalidade?"
const val PERGUNTA_DO_CALCULO = "O que você quer calcular?"
const val CAMPO_DA_ETAPA = "Valor da etapa"
const val AJUDA_DA_CALCULADORA = "Como usar a calculadora"
const val NOVO_CALCULO = "Novo cálculo"
const val CONTINUAR = "Continuar"
const val VER_RESULTADO = "Ver resultado"
const val FECHAR_A_DICA = "Fechar a dica"

/**
 * O botão da calculadora (desde a 0.43, ao lado do "Alternar sessão", nas abas; até a 0.42, um ícone nos Detalhes): o
 * quadrado azul-marinho com a calculadora dourada, do tamanho e no jeito do das sessões e do Painel.
 */
@Composable
fun BotaoDaCalculadora(aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar, modifier = Modifier.size(58.dp).semantics(mergeDescendants = true) { contentDescription = TITULO_CALCULADORA }) {
        Box(Modifier.size(38.dp).background(AzulMarinho, RoundedCornerShape(38.dp * 0.29f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Calculate, contentDescription = null, tint = Dourado, modifier = Modifier.size(26.dp))
        }
    }
}

/** O nome de cada passo da barra das etapas (para quem não vê a tela e os testes): "Etapa 2: Cálculo". */
fun nomeDoPasso(numero: Int, titulo: String): String = "Etapa $numero: $titulo"

/** O nome de cada modalidade da lista da etapa 1: "Calcular na Quina". */
fun nomeDaModalidadeNaCalculadora(m: Modalidade): String = "Calcular na ${m.nome}"

/** O nome do botão de dica de cada cálculo: "O que é Fechamento". */
fun nomeDaDica(modo: Modo): String = "O que é ${modo.titulo}"

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
            p.base?.name.orEmpty(),
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
            base = CalculoEmEtapas.Base.entries.firstOrNull { it.name == l.getOrNull(8) },
        )
    },
)

/**
 * A calculadora de probabilidades. Na 0.42, etapas que ocupavam a tela; desde a 0.43 (pedido do usuário) um **pop-up** por
 * cima da tela (o fundo escurecido; tocar fora fecha):
 * - **em cima**, o título e o "?" grande no canto direito (como usar);
 * - **no meio**, a etapa: 1, a **modalidade** (a lista com as tarjas, como na tela Jogos); 2, **o que calcular** (6 botões
 *   com a palavra inteira, perto da mão, cada um com a dica do que é); depois as perguntas de cada cálculo; no fim, o
 *   **resultado** (só os números: o preço, quantos jogos e de que tipo, as chances e, quando faz sentido, os prêmios de
 *   cada faixa, a garantia ou os limites do empacotamento; os jogos em si, não);
 * - **embaixo**, o botão de continuar (nas etapas de número) ou o novo cálculo, e a **barra das etapas** (tocar numa feita
 *   volta a ela).
 * Sem o "Voltar": deslizar o dedo para a direita volta uma etapa (para a esquerda, avança para uma já feita) e puxar para
 * baixo fecha. O voltar do celular volta uma etapa (na primeira, fecha). As contas ficam em [CalculoEmEtapas].
 * [inicial]: a modalidade em que abre (já na etapa 2); null = começa escolhendo a modalidade.
 */
@Composable
fun CalculadoraPopup(estado: Estado, inicial: Modalidade?, aoFechar: () -> Unit) {
    var nome by rememberSaveable { mutableStateOf(inicial?.name) }
    val m = nome?.let { n -> Modalidade.entries.firstOrNull { it.name == n } }
    var pedido by rememberSaveable(stateSaver = GuardaDoPedido) { mutableStateOf(Pedido()) }
    var passo by rememberSaveable { mutableIntStateOf(if (inicial != null) 1 else 0) }
    var ajuda by rememberSaveable { mutableStateOf(false) }
    val etapas = CalculoEmEtapas.etapas(pedido)
    // Os passos da barra: 0 = a modalidade, 1 = o que calcular, 2... = as etapas do cálculo, [ultimo] = o resultado.
    val ultimo = etapas.size + 2
    val alcance = if (m == null) 0 else 1 + CalculoEmEtapas.etapaAtual(m, pedido)
    val agora = passo.coerceAtMost(alcance)
    val fechar by rememberUpdatedState(aoFechar)

    fun irPara(i: Int) {
        passo = i.coerceIn(0, alcance)
    }
    // [naEtapa]: o passo do cálculo (1 = o que calcular) em que a escolha foi feita.
    fun escolher(novo: Pedido, naEtapa: Int) {
        val x = m ?: return
        pedido = novo
        passo = 1 + CalculoEmEtapas.depoisDe(x, novo, naEtapa)
    }
    fun escolherModalidade(nova: Modalidade) {
        val p = if (m == null) pedido else CalculoEmEtapas.naModalidade(pedido, m, nova)
        nome = nova.name
        pedido = p
        passo = 1 + CalculoEmEtapas.etapaAtual(nova, p)
    }
    BackHandler { if (agora > 0) irPara(agora - 1) else fechar() }

    Box(Modifier.fillMaxSize()) {
        // O fundo escurecido: tocar fora fecha.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) { detectTapGestures { fechar() } }
                .semantics { contentDescription = FORA_DA_CALCULADORA },
        )
        PuxarParaVoltar(aoVoltar = { fechar() }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(FRACAO_DA_ALTURA)) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).imePadding()) {
                    CabecalhoDaCalculadora(aoAjudar = { ajuda = true })
                    AnimatedContent(
                        targetState = agora,
                        transitionSpec = {
                            val ida = targetState > initialState
                            (slideInHorizontally { if (ida) it / 4 else -it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { if (ida) -it / 4 else it / 4 } + fadeOut())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .deslizarEntreEtapas(
                                aoVoltar = { if (agora > 0) irPara(agora - 1) },
                                aoAvancar = { if (agora < alcance) irPara(agora + 1) },
                            ),
                        label = "etapa",
                    ) { i ->
                        val etapa = etapas.getOrNull(i - 2)
                        val x = m
                        when {
                            i == 0 || x == null -> EtapaDaModalidade(m, ::escolherModalidade)
                            i == 1 -> EtapaOQueCalcular(x, pedido.modo) { escolher(CalculoEmEtapas.comModo(pedido, it), 0) }
                            i >= ultimo -> EtapaDoResultado(x, pedido, estado.config.preco(x))
                            etapa == Etapa.BASE -> EtapaDaBase(pedido.base) { escolher(CalculoEmEtapas.comBase(pedido, it), i - 1) }
                            etapa == Etapa.FAIXA -> EtapaDaFaixa(x, pedido) { escolher(CalculoEmEtapas.comFaixa(x, pedido, it), i - 1) }
                            etapa == Etapa.TIPO -> EtapaDoTipo(x, pedido, estado.config.preco(x)) { n, t -> escolher(pedido.copy(numeros = n, trevos = t), i - 1) }
                            etapa == Etapa.GARANTIA -> EtapaDaGarantia(x, pedido, estado.config.preco(x)) { escolher(pedido.copy(garantia = it), i - 1) }
                            etapa != null -> EtapaDoNumero(
                                x, pedido, etapa,
                                aoMudar = { pedido = it },
                                aoContinuar = { if (CalculoEmEtapas.completa(x, pedido, etapa)) passo = 1 + CalculoEmEtapas.depoisDe(x, pedido, i - 1) },
                            )
                        }
                    }
                    RodapeDaCalculadora(
                        m, pedido, agora, ultimo,
                        aoRecomecar = { pedido = Pedido(formato = pedido.formato); passo = 1 },
                        aoContinuar = {
                            val e = etapas.getOrNull(agora - 2)
                            if (m != null && e != null && CalculoEmEtapas.completa(m, pedido, e)) passo = 1 + CalculoEmEtapas.depoisDe(m, pedido, agora - 1)
                        },
                    )
                    BarraDeEtapas(m, pedido, agora, alcance, aoTocar = ::irPara)
                }
            }
        }
    }
    if (ajuda) AjudaDaCalculadora { ajuda = false }
}

/** Quanto da altura da tela o pop-up ocupa. */
private const val FRACAO_DA_ALTURA = 0.92f

/** O nome do fundo escurecido (tocar fecha). */
const val FORA_DA_CALCULADORA = "Fechar a calculadora"

/**
 * Deslizar o dedo para os lados entre as etapas (desde a 0.43, no lugar do "Voltar"): para a direita volta uma etapa, para
 * a esquerda avança (só para uma etapa já feita). Dispara ao passar de um quarto da largura, ou num deslize rápido; uma vez
 * por gesto. A rolagem para cima e para baixo continua (e o puxão para baixo, que fecha).
 */
private fun Modifier.deslizarEntreEtapas(aoVoltar: () -> Unit, aoAvancar: () -> Unit): Modifier = composed {
    val voltar by rememberUpdatedState(aoVoltar)
    val avancar by rememberUpdatedState(aoAvancar)
    var largura by remember { mutableFloatStateOf(1f) }
    val rastreador = remember { VelocityTracker() }
    this
        .onSizeChanged { largura = it.width.toFloat().coerceAtLeast(1f) }
        .pointerInput(Unit) {
            var andado = 0f
            var feito = false
            fun disparar() {
                feito = true
                if (andado > 0f) voltar() else avancar()
            }
            detectHorizontalDragGestures(
                onDragStart = {
                    rastreador.resetTracking()
                    andado = 0f
                    feito = false
                },
                onDragEnd = {
                    val v = rastreador.calculateVelocity().x
                    if (!feito && andado != 0f && kotlin.math.abs(v) > VELOCIDADE_DO_DESLIZE && kotlin.math.sign(v) == kotlin.math.sign(andado)) disparar()
                },
            ) { mudanca, dx ->
                mudanca.consume()
                rastreador.addPosition(mudanca.uptimeMillis, mudanca.position)
                andado += dx
                if (!feito && kotlin.math.abs(andado) > largura * FRACAO_DO_DESLIZE) disparar()
            }
        }
}

private const val FRACAO_DO_DESLIZE = 0.25f
private const val VELOCIDADE_DO_DESLIZE = 1200f

/**
 * O alto do pop-up: a alça (puxar para baixo fecha), o título e o "?" grande no canto direito. O alto não rola, mas passa
 * o arrasto para baixo ao puxão que fecha.
 */
@Composable
private fun CabecalhoDaCalculadora(aoAjudar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .scrollable(rememberScrollableState { 0f }, Orientation.Vertical),
    ) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(TITULO_CALCULADORA, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = aoAjudar, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, AJUDA_DA_CALCULADORA, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** A etapa 1: a modalidade, na lista com as tarjas (como na tela Jogos); a escolhida com o contorno dourado. */
@Composable
private fun EtapaDaModalidade(atual: Modalidade?, aoEscolher: (Modalidade) -> Unit) {
    val lista = remember { Modalidade.ATIVAS.filter { Calculadora.tem(it) } }
    ColunaDaEtapa {
        Pergunta(PERGUNTA_DA_MODALIDADE)
        lista.forEach { x ->
            val escolhida = x == atual
            TarjaModalidade(
                x,
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .then(if (escolhida) Modifier.border(3.dp, Dourado, RoundedCornerShape(TAMANHO_DA_TARJA * Tarjas.CANTO + 3.dp)).padding(3.dp) else Modifier)
                    .clip(RoundedCornerShape(TAMANHO_DA_TARJA * Tarjas.CANTO))
                    .clickable(role = Role.RadioButton) { aoEscolher(x) }
                    .semantics { contentDescription = nomeDaModalidadeNaCalculadora(x); selected = escolhida },
                altura = TAMANHO_DA_TARJA,
            )
        }
    }
}

private val TAMANHO_DA_TARJA = 52.dp

/** O que fica escrito embaixo de cada etapa feita (a escolha dela). */
private fun escolhaDaEtapa(m: Modalidade, p: Pedido, etapa: Etapa): String? = when (etapa) {
    Etapa.FAIXA -> p.faixa?.let { f -> if (f == Calculadora.QUALQUER_PREMIO) "Qualquer" else Calculadora.faixas(m).getOrNull(f) }
    Etapa.TIPO -> p.numeros?.let { n -> if (p.trevos != null && m.trevos > 0) "$n + ${p.trevos}" else "$n números" }
    Etapa.GARANTIA -> p.garantia?.let { g -> CalculoEmEtapas.nomeDaGarantia(m, g).replace(" · se sair ", " · ") }
    Etapa.BASE -> p.base?.curto
    else -> CalculoEmEtapas.numero(p.valor)?.takeIf { CalculoEmEtapas.erroDoValor(m, p) == null }?.let { v ->
        when (p.modo) {
            Modo.PROBABILIDADE -> if (p.formato == Formato.PORCENTO) Calculadora.porcentagem(v / 100) else "1 em " + curto(v)
            Modo.ORCAMENTO -> "R$ " + curto(v)
            Modo.FECHAMENTO, Modo.COBERTURA -> "${v.toInt()} números"
            Modo.DESDOBRAMENTO -> if (CalculoEmEtapas.porPremios(p)) "${v.toInt()} ${CalculoEmEtapas.plural(m, p.faixa ?: 0)}" else "${v.toInt()} números"
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
 * A barra das etapas, embaixo (desde a 0.43; na 0.42, em cima): um círculo por etapa (a modalidade com o selo dela; as
 * outras feitas com o visto; a de agora em dourado), o nome e, embaixo, a escolha. Tocar numa etapa feita volta a ela
 * (para mudar a escolha); as de depois esperam. [alcance]: a primeira etapa ainda sem escolha.
 */
@Composable
private fun BarraDeEtapas(m: Modalidade?, p: Pedido, agora: Int, alcance: Int, aoTocar: (Int) -> Unit) {
    val etapas = p.modo?.let { CalculoEmEtapas.etapas(p) }
    val titulos = listOf(ETAPA_MODALIDADE, ETAPA_COMO) + (etapas?.map { it.titulo } ?: listOf("Detalhes")) + ETAPA_RESULTADO
    val muitas = titulos.size > 5
    HorizontalDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        titulos.forEachIndexed { i, titulo ->
            val feita = i < alcance && i != agora
            val atual = i == agora
            val pode = i <= alcance && !atual && (etapas != null || i <= 1)
            val escolha = when {
                i == 0 -> m?.nome
                i == 1 -> p.modo?.titulo
                m != null && etapas != null && i in 2..etapas.size + 1 -> escolhaDaEtapa(m, p, etapas[i - 2])
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
                    .padding(vertical = 4.dp, horizontal = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fundo = when {
                    atual -> Dourado
                    feita -> AzulMarinho
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
                if (i == 0 && m != null && !atual) {
                    SeloModalidade(m, tamanho = 30.dp)
                } else {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(fundo), contentAlignment = Alignment.Center) {
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
                }
                Text(
                    titulo,
                    style = if (muitas) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
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

/**
 * A etapa 2: o que calcular (desde a 0.43, pedido do usuário). A pergunta em cima e os 6 botões embaixo, perto da mão (2
 * por linha), com a palavra inteira: APOSTAS, CHANCE, VALOR, FECHAMENTO, DESDOBRAMENTO, EMPACOTAMENTO. Cada botão tem a
 * dica (o "i"), que mostra o que ele calcula, logo acima dos botões; os que não valem na modalidade ficam apagados (a dica
 * diz o porquê). O escolhido antes (ao voltar) fica dourado.
 */
@Composable
private fun EtapaOQueCalcular(m: Modalidade, atual: Modo?, aoEscolher: (Modo) -> Unit) {
    var dica by remember { mutableStateOf<Modo?>(null) }
    val rolagem = rememberScrollState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rolagem)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Pergunta(PERGUNTA_DO_CALCULO)
            Column {
                dica?.let { d -> CartaoDaDica(m, d) { dica = null } }
                Modo.entries.chunked(2).forEach { linha ->
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        linha.forEach { modo ->
                            BotaoDoCalculo(
                                modo,
                                escolhido = modo == atual,
                                habilitado = CalculoEmEtapas.motivoSemModo(m, modo) == null,
                                dicaAberta = dica == modo,
                                aoTocar = { aoEscolher(modo) },
                                aoPedirDica = { dica = if (dica == modo) null else modo },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Um botão do "O que você quer calcular?": a palavra inteira e, à direita, o "i" da dica. */
@Composable
private fun BotaoDoCalculo(
    modo: Modo,
    escolhido: Boolean,
    habilitado: Boolean,
    dicaAberta: Boolean,
    aoTocar: () -> Unit,
    aoPedirDica: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fundo = when {
        !habilitado -> MaterialTheme.colorScheme.surfaceContainerHighest
        escolhido -> Dourado
        else -> AzulMarinho
    }
    val frente = when {
        !habilitado -> MaterialTheme.colorScheme.onSurfaceVariant
        escolhido -> AzulMarinho
        else -> Dourado
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = fundo,
        border = if (dicaAberta) BorderStroke(2.dp, if (escolhido) AzulMarinho else Dourado) else null,
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = habilitado, role = Role.Button, onClick = aoTocar)
            .semantics {
                contentDescription = modo.titulo
                selected = escolhido
                if (!habilitado) disabled()
            },
    ) {
        Row(Modifier.padding(start = 12.dp, end = 0.dp), verticalAlignment = Alignment.CenterVertically) {
            // A palavra inteira sempre (diminui a letra até caber: DESDOBRAMENTO e EMPACOTAMENTO são compridas).
            TextoQueCabe(
                modo.titulo.uppercase(),
                TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.3.sp),
                cor = frente,
                menor = 10.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = aoPedirDica, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Info, nomeDaDica(modo), tint = frente.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** A dica de um cálculo, logo acima dos botões: o nome, o que calcula e um exemplo (ou por que não vale na modalidade). */
@Composable
private fun CartaoDaDica(m: Modalidade, modo: Modo, aoFechar: () -> Unit) {
    val motivo = CalculoEmEtapas.motivoSemModo(m, modo)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, Dourado),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 12.dp, end = 2.dp)) {
            Column(Modifier.weight(1f)) {
                Text(modo.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(modo.descricao, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                Text(
                    motivo ?: "Ex.: ${exemploDoModo(m, modo)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (motivo != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = aoFechar, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Close, FECHAR_A_DICA, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** A etapa da faixa: as faixas, com a chance de 1 aposta simples ao lado (na probabilidade) ou o que dá (nos prêmios). */
@Composable
private fun EtapaDaFaixa(m: Modalidade, p: Pedido, aoEscolher: (Int) -> Unit) {
    val modo = p.modo ?: return
    val premios = CalculoEmEtapas.porPremios(p)
    val simples = remember(m) { Calculadora.chances(m, ConfigDaCalculadora()) }
    ColunaDaEtapa {
        if (premios) {
            Pergunta("Em que faixa?", "A quantidade de prêmios que você quer receber nela, se acertar.")
        } else {
            Pergunta("Em que faixa?", "A chance que você vai pedir é a de acertar esta faixa pelo menos uma vez.")
        }
        CalculoEmEtapas.faixasDoModo(m, p).forEach { (i, nome) ->
            val qualquer = i == Calculadora.QUALQUER_PREMIO
            Opcao(escolhida = p.faixa == i, aoTocar = { aoEscolher(i) }, descricao = "Faixa $nome") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val sub = when {
                            qualquer -> LEGENDA_QUALQUER
                            premios -> CalculoEmEtapas.premiosDaFaixa(m, m.qtdAposta + 1, i)?.let {
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
    Modo.ORCAMENTO -> JeitoDoNumero("Quanto você quer gastar?", "Na etapa seguinte, o tipo de jogo: a calculadora mostra quantos cabem e o troco.", "R$", null, true)
    Modo.FECHAMENTO -> JeitoDoNumero(
        "Com quantos números?",
        "Os números que você vai escolher (de ${CalculoEmEtapas.faixaDeNumeros(m, Modo.FECHAMENTO)?.first} a ${CalculoEmEtapas.faixaDeNumeros(m, Modo.FECHAMENTO)?.last}). Os jogos do fechamento saem deles.",
        null, "números", false,
    )
    Modo.DESDOBRAMENTO -> if (CalculoEmEtapas.porPremios(p)) {
        JeitoDoNumero(
            "Quantos ${CalculoEmEtapas.plural(m, p.faixa ?: 0)}?",
            "Se acertar a ${Calculadora.faixas(m).getOrNull(p.faixa ?: 0)?.lowercase()}, o desdobramento tem que dar pelo menos esse tanto.",
            null, null, false,
        )
    } else JeitoDoNumero(
        "Com quantos números?",
        "Todos os jogos de ${m.qtdAposta} números com eles (de ${CalculoEmEtapas.faixaDeNumeros(m, Modo.DESDOBRAMENTO)?.first} a ${CalculoEmEtapas.faixaDeNumeros(m, Modo.DESDOBRAMENTO)?.last}).",
        null, "números", false,
    )
    Modo.COBERTURA -> JeitoDoNumero(
        "Com quantos números?",
        "Os jogos saem desses números, sem repetir ${CalculoEmEtapas.nenhumGrupo(m)}. Todos = os ${CalculoEmEtapas.faixaDeNumeros(m, Modo.COBERTURA)?.last} da ${m.nome}." +
            when (val x = CalculoEmEtapas.maximoMontavel(m)) {
                null -> ""
                CalculoEmEtapas.faixaDeNumeros(m, Modo.COBERTURA)?.last -> " A calculadora também conta quantos jogos consegue montar (pode levar alguns segundos)."
                else -> " Até $x números a calculadora também conta quantos jogos consegue montar."
            },
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
    Modo.ORCAMENTO -> "Aposta simples: ${m.nome} a partir de ${Dinheiro.rs(m.precoPadrao)}"
    Modo.DESDOBRAMENTO -> if (CalculoEmEtapas.porPremios(p)) "Ex.: 10" else CalculoEmEtapas.faixaDeNumeros(m, Modo.DESDOBRAMENTO)?.let { "De ${it.first} a ${it.last}" } ?: ""
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

/** Os jogos da cobertura do pedido (fora da linha da tela; null enquanto monta ou quando a conta é grande demais). */
@Composable
private fun jogosDaCobertura(m: Modalidade, p: Pedido): List<IntArray>? {
    val n = CalculoEmEtapas.numero(p.valor)?.toInt()
    val r by produceState<List<IntArray>?>(null, m, n, p.modo) {
        value = if (p.modo != Modo.COBERTURA || n == null || !CalculoEmEtapas.podeMontarCobertura(m, n)) {
            null
        } else {
            val t = CalculoEmEtapas.acertosDaCobertura(m) ?: return@produceState
            withContext(Dispatchers.Default) {
                val trabalho = coroutineContext
                CalculoEmEtapas.cobertura(n, m.qtdAposta, t) { trabalho.isActive }
            }
        }
    }
    return r
}

/** O exemplo de cada jeito de calcular na etapa 1 (o da cobertura com o limite da modalidade). */
private fun exemploDoModo(m: Modalidade, modo: Modo): String = when (modo) {
    Modo.COBERTURA -> CalculoEmEtapas.limitesDaCobertura(m)?.let { r ->
        val t = CalculoEmEtapas.acertosDaCobertura(m) ?: return modo.exemplo
        "${r.last} números, até ${Probabilidades.milhares(CalculoEmEtapas.limiteDeJohnson(r.last, m.qtdAposta, t))} jogos"
    } ?: modo.exemplo
    else -> modo.exemplo
}

/** A etapa da base do desdobramento: pelos números ou pelos prêmios. */
@Composable
private fun EtapaDaBase(atual: CalculoEmEtapas.Base?, aoEscolher: (CalculoEmEtapas.Base) -> Unit) {
    ColunaDaEtapa {
        Pergunta("Desdobrar a partir de quê?", "Os dois dão todos os jogos simples de um grupo de números (o mesmo preço e os mesmos prêmios de uma aposta com eles).")
        CalculoEmEtapas.Base.entries.forEach { b ->
            Opcao(escolhida = b == atual, aoTocar = { aoEscolher(b) }, descricao = "Base ${b.titulo}") {
                Column {
                    Text(b.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(b.descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ex.: ${b.exemplo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

/** O cartão da cobertura no resultado: os limites e quantos jogos a calculadora montou. */
@Composable
private fun CartaoDaCobertura(m: Modalidade, c: CalculoEmEtapas.InfoDaCobertura) {
    Spacer(Modifier.height(8.dp))
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Empacotamento", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "Jogos de ${c.tamanho} números saídos dos seus ${c.numeros}, sem repetir ${CalculoEmEtapas.nenhumGrupo(m)}: " +
                    "cada grupo de ${c.acertos} números aparece em um jogo só.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            LinhaDaCobertura("Limite de Johnson (o máximo nunca passa disso)", "${Probabilidades.milhares(c.johnson)} jogos")
            LinhaDaCobertura(
                "Contando só os grupos: C(${c.numeros}, ${c.acertos}) ÷ C(${c.tamanho}, ${c.acertos})",
                "${Probabilidades.milhares(c.grupos)} jogos",
            )
            if (c.montados != null) {
                LinhaDaCobertura("Montados pela calculadora", "${Probabilidades.milhares(c.montados.toLong())} jogos")
            } else {
                Text(
                    "Com ${c.numeros} números a conta de montar os jogos é grande demais para o celular" +
                        (CalculoEmEtapas.maximoMontavel(m)?.let { " (ela monta até $it números)" } ?: "") +
                        ". O preço e as chances são os do limite de Johnson.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "O máximo exato (o número de empacotamento D(${c.numeros}, ${c.tamanho}, ${c.acertos})) só é conhecido em casos pequenos.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun LinhaDaCobertura(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * O resultado: o cartão de cima no azul com o dourado (o preço grande, quantos jogos e de que tipo, o pedido), as chances
 * de cada faixa (a do pedido em destaque), os prêmios de cada faixa (do mínimo ao máximo) quando é uma aposta só, a
 * garantia do fechamento ou os limites do empacotamento. Desde a 0.43 só os números (pedido do usuário): os jogos do
 * fechamento, do desdobramento e do empacotamento não aparecem.
 */
@Composable
private fun EtapaDoResultado(m: Modalidade, p: Pedido, preco: Long) {
    val fechamento = jogosDoFechamento(m, p)
    val cobertura = jogosDaCobertura(m, p)
    val r = remember(m, p, preco, fechamento?.size, cobertura?.size) { CalculoEmEtapas.resultado(m, p, preco, fechamento?.size, cobertura?.size) }
    ColunaDaEtapa {
        if (r == null) {
            Text(
                when {
                    p.modo == Modo.FECHAMENTO && fechamento == null -> "Montando o fechamento…"
                    p.modo == Modo.COBERTURA && cobertura == null -> "Contando os jogos do empacotamento…"
                    else -> "Não dá para chegar nesse pedido: volte e mude a escolha."
                },
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
        r.cobertura?.let { CartaoDaCobertura(m, it) }
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

/**
 * Em cima da barra das etapas: o continuar (nas etapas de número) ou o novo cálculo (no resultado). Sem o "Voltar" desde a
 * 0.43 (deslizar para a direita volta). Nas etapas de escolher, nada (tocar na opção já avança).
 */
@Composable
private fun RodapeDaCalculadora(
    m: Modalidade?,
    p: Pedido,
    agora: Int,
    ultimo: Int,
    aoRecomecar: () -> Unit,
    aoContinuar: () -> Unit,
) {
    if (m == null || agora < 2) return
    val etapa = CalculoEmEtapas.etapas(p).getOrNull(agora - 2)
    val deNumero = etapa != null && etapa !in setOf(Etapa.FAIXA, Etapa.TIPO, Etapa.GARANTIA, Etapa.BASE)
    if (agora < ultimo && !deNumero) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (agora >= ultimo) {
            OutlinedButton(onClick = aoRecomecar, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(NOVO_CALCULO)
            }
        } else {
            val ok = CalculoEmEtapas.completa(m, p, etapa!!)
            val vaiAoFim = CalculoEmEtapas.depoisDe(m, p, agora - 1) + 1 >= ultimo
            Button(
                onClick = aoContinuar,
                enabled = ok,
                colors = ButtonDefaults.buttonColors(containerColor = AzulMarinho, contentColor = Dourado),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(if (vaiAoFim) VER_RESULTADO else CONTINUAR, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

/** O "?": como usar a calculadora (as etapas e cada jeito de calcular). */
@Composable
private fun AjudaDaCalculadora(aoFechar: () -> Unit) {
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 620.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(AJUDA_DA_CALCULADORA, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A conta é feita em etapas, mostradas na barra de baixo: 1, a modalidade; 2, o que você quer calcular; depois, as " +
                        "perguntas de cada cálculo; no fim, o resultado (só os números).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Para voltar uma etapa, deslize o dedo para a direita, ou toque numa etapa da barra de baixo para mudar a escolha dela. " +
                        "Para fechar a calculadora, puxe para baixo (ou toque fora dela).",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.height(12.dp))
                Modo.entries.forEach { modo ->
                    Column(Modifier.padding(bottom = 10.dp)) {
                        Text(modo.titulo.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text(modo.descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

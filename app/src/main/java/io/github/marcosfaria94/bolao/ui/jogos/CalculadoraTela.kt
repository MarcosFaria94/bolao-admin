package io.github.marcosfaria94.bolao.ui.jogos

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.Calculadora.Registro
import io.github.marcosfaria94.bolao.domain.ConfigDaCalculadora
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.domain.Teclado
import io.github.marcosfaria94.bolao.ui.comum.Animacoes
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.math.abs

/** O endereço da calculadora de probabilidades (desde a 0.38): "calculadora/MEGA_SENA". */
const val ROTA_CALCULADORA = "calculadora/{nome}"
const val TITULO_CALCULADORA = "Calculadora"

fun rotaDaCalculadora(m: Modalidade): String = "calculadora/${m.name}"

/** A calculadora existe em todas as modalidades menos a Federal. */
fun temCalculadora(m: Modalidade): Boolean = Calculadora.tem(m)

/** Os textos e nomes da calculadora (os testes procuram por eles). */
const val TITULO_DO_PAINEL = "Probabilidades"
const val GRUPO_CHANCES = "CHANCES"
const val GRUPO_APOSTAS = "APOSTAS"
const val DICA_CHANCES = "Toque e digite a chance"
const val DICA_APOSTAS = "Toque e digite quantas"
const val PRECO_NO_VISOR = "Preço"

/** A legenda do "Qualquer prêmio" (desde a 0.40). */
const val LEGENDA_QUALQUER = "Todas as faixas juntas"

/** O nome de cada tecla para quem não vê a tela (e os testes): "Tecla 7", "Tecla igual"... */
fun nomeDaTecla(t: String): String = "Tecla $t"
const val TECLA_IGUAL = "igual"
const val TECLA_APAGAR = "apagar"
const val TECLA_LIMPAR = "limpar"
const val TECLA_ZERAR = "zerar tudo"

/**
 * A configuração de cada calculadora (desde a 0.38): guardada no celular, uma por modalidade, a cada mudança e ao sair
 * da tela (vale para o app todo, não é da sessão: é uma simulação).
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
fun porcentagemNaTela(p: Double): String = Calculadora.porcentagem(p).replace(Calculadora.APROXIMADO, Calculadora.APROXIMADO + " ")

/**
 * A calculadora de probabilidades da modalidade, refeita na 0.41 (pedido do usuário: "use lógica de calculadora mesmo:
 * botões visíveis para a conta, tela de saída e o '=' chamando o resultado"; até a 0.40 eram controles deslizantes).
 * - **O visor** (em cima, sempre na tela): o preço de tudo, grande, com a diferença da última mudança ("▲ R$ 6,00"),
 *   quantas apostas, o que está sendo mudado e o que foi digitado.
 * - **As listas** (no meio): as chances de cada faixa e do "Qualquer prêmio" e os tipos de aposta. Tocar numa linha a
 *   escolhe para o visor.
 * - **O teclado** (embaixo): os algarismos, a vírgula, + − × ÷, o %, o apagar, o C/AC (o AC volta ao começo: 1 aposta
 *   simples) e o "=". Com um tipo de aposta escolhido, o "=" põe aquele tanto de apostas; com uma faixa, a chance
 *   digitada ("1 em X", ou "X%" com o %) monta a combinação mais barata que chega lá ([Calculadora.paraChance]), e as
 *   outras faixas mudam junto. Uma conta pode começar pelo valor do visor: "÷ 2 =" dobra a chance da faixa escolhida.
 * A configuração fica guardada sozinha ([GuardaDaCalculadora]).
 */
@Composable
fun CalculadoraTela(estado: Estado, m: Modalidade) {
    val ctx = LocalContext.current
    var config by remember(m) { mutableStateOf(GuardaDaCalculadora.ler(ctx, m)) }
    var chaveDoRegistro by rememberSaveable(m) { mutableStateOf(Calculadora.chave(Calculadora.registroDaSimples(m))) }
    val registro = remember(m, chaveDoRegistro) { Calculadora.deChave(m, chaveDoRegistro) }
    var linha by rememberSaveable(m) { mutableStateOf("") }
    var mensagem by remember(m) { mutableStateOf<Calculadora.Resposta?>(null) }
    // Guarda a cada mudança e ao sair da tela.
    LaunchedEffect(config) { GuardaDaCalculadora.gravar(ctx, m, config) }
    val ultima by rememberUpdatedState(config)
    DisposableEffect(m) { onDispose { GuardaDaCalculadora.gravar(ctx, m, ultima) } }

    val preco = estado.config.preco(m)
    val chances = remember(m, config) { Calculadora.chances(m, config) }
    val custo = Calculadora.custo(m, config, preco)
    val total = Calculadora.total(m, config)

    fun escolher(r: Registro) {
        chaveDoRegistro = Calculadora.chave(r)
        linha = ""
        mensagem = null
    }
    fun tecla(t: String) {
        mensagem = null
        linha = when (t) {
            TECLA_APAGAR -> Teclado.apagar(linha)
            TECLA_LIMPAR -> ""
            else -> Teclado.digitar(linha, t.first())
        }
    }
    fun zerar() {
        config = ConfigDaCalculadora()
        linha = ""
        mensagem = Calculadora.Resposta(null, "De volta ao começo: ${Calculadora.quantas(1)} de ${Calculadora.nomeDaLinha(m, Calculadora.simples(m).numeros, Calculadora.simples(m).trevos)}.")
    }
    fun igual() {
        if (linha.isEmpty()) return
        // O registro lido agora (o teclado pode estar com esta função de uma composição anterior: a referência a uma
        // função local é igual entre composições, e o Compose não troca a tecla).
        val r = Calculadora.aplicar(m, config, Calculadora.deChave(m, chaveDoRegistro), linha)
        r.config?.let { config = it }
        if (!r.erro) linha = ""
        mensagem = r
    }

    Column(Modifier.fillMaxSize()) {
        Visor(m, registro, config, linha, mensagem, custo, total, Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp))
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TituloDoGrupo(GRUPO_CHANCES, DICA_CHANCES)
            chances.filter { it.indice != Calculadora.QUALQUER_PREMIO }.forEach { c ->
                val r = Registro.Faixa(c.indice)
                LinhaDaChance(
                    nome = c.nome,
                    p = c.p,
                    porSorteio = if (m == Modalidade.DUPLA_SENA) Calculadora.chancePorSorteio(m, config, c.indice) else null,
                    escolhida = registro == r,
                    aoTocar = { escolher(r) },
                )
            }
            chances.firstOrNull { it.indice == Calculadora.QUALQUER_PREMIO }?.let { q ->
                val r = Registro.Faixa(Calculadora.QUALQUER_PREMIO)
                LinhaDoQualquer(q.p, registro == r) { escolher(r) }
            }
            Spacer(Modifier.height(6.dp))
            TituloDoGrupo(GRUPO_APOSTAS, DICA_APOSTAS)
            val tipos = remember(m) { Calculadora.linhas(m) }
            tipos.forEachIndexed { i, l ->
                if (i == 1) {
                    Text(
                        Calculadora.tituloDosMaisNumeros(m),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
                val r = Registro.Apostas(l.numeros, l.trevos)
                val n = Calculadora.apostasDe(m, config, l.numeros, l.trevos)
                LinhaDoTipo(
                    nome = Calculadora.nomeDaLinha(m, l.numeros, l.trevos),
                    apostas = n,
                    valor = l.apostasSimples * n * preco,
                    precoDeUma = l.apostasSimples * preco,
                    escolhida = registro == r,
                    aoTocar = { escolher(r) },
                )
            }
            notasDaCalculadora(m).forEach { n ->
                Text("* $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(4.dp))
        }
        TecladoDaCalculadora(
            comPorcento = registro is Registro.Faixa,
            temLinha = linha.isNotEmpty(),
            aoTocar = ::tecla,
            aoZerar = ::zerar,
            aoIgual = ::igual,
        )
    }
}

/** As notas do fim da calculadora (como a conta é feita). */
fun notasDaCalculadora(m: Modalidade): List<String> = listOfNotNull(
    "A chance de acertar cada faixa pelo menos uma vez, com todas essas apostas no mesmo concurso e diferentes umas das outras. " +
        "Na faixa principal a conta é exata (as chances se somam); nas outras, as apostas contam como feitas ao acaso.",
    "Pedindo a chance de uma faixa, a calculadora monta a combinação de apostas mais barata que chega lá (com o mesmo preço, a que dá mais chance nas outras faixas).",
    "Na Dupla Sena cada aposta concorre nos 2 sorteios: a chance é a de ganhar em pelo menos um deles. A tabela da Caixa mostra a de cada sorteio (\"por sorteio\", embaixo do nome da faixa).".takeIf { m == Modalidade.DUPLA_SENA },
    "Na Loteca, supondo os 3 resultados igualmente prováveis em cada jogo.".takeIf { m == Modalidade.LOTECA },
    Probabilidades.LEGENDA_DA_MAIS_MILIONARIA.takeIf { m == Modalidade.MAIS_MILIONARIA },
)

private val AzulNoite = Color(0xFF13294B)
private val AzulNoiteFundo = Color(0xFF0B1626)
private val Suave = Color(0xFFB9C6DA)
private val Subiu = Color(0xFFFF9E80)
private val Desceu = Color(0xFF3DD68C)
private val Erro = Color(0xFFFF8A80)

/**
 * O visor (desde a 0.41): no azul-noite com o dourado, nos dois temas, como o de uma calculadora. Em cima, o preço de tudo
 * (grande; troca de cima para baixo quando sobe e de baixo para cima quando desce) e, por 3 segundos, a diferença; embaixo,
 * o que está sendo mudado ([registro]) e a [linha] digitada (sem nada, o valor de agora, apagado) e a [mensagem] do "=".
 */
@Composable
private fun Visor(
    m: Modalidade,
    registro: Registro,
    config: ConfigDaCalculadora,
    linha: String,
    mensagem: Calculadora.Resposta?,
    custo: Long,
    total: Long,
    modifier: Modifier = Modifier,
) {
    // A diferença do preço (some depois de 3 s).
    var anterior by remember(m) { mutableLongStateOf(custo) }
    var diferenca by remember(m) { mutableLongStateOf(0L) }
    LaunchedEffect(custo) {
        if (custo != anterior) {
            diferenca = custo - anterior
            anterior = custo
            delay(3_000)
            diferenca = 0L
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(AzulNoite, AzulNoiteFundo)))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                m.nome.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                fontWeight = FontWeight.Bold,
                color = Dourado,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(Calculadora.quantas(total), style = MaterialTheme.typography.bodyMedium, color = Suave, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Column(Modifier.weight(1f)) {
                Text(PRECO_NO_VISOR, style = MaterialTheme.typography.bodySmall, color = Suave)
                AnimatedVisibility(
                    visible = diferenca != 0L,
                    enter = if (Animacoes.ligadas) fadeIn() else EnterTransition.None,
                    exit = if (Animacoes.ligadas) fadeOut() else ExitTransition.None,
                ) {
                    val subiu = diferenca > 0
                    Text(
                        (if (subiu) "▲ " else "▼ ") + Dinheiro.rs(abs(diferenca)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (subiu) Subiu else Desceu,
                        maxLines = 1,
                    )
                }
            }
            AnimatedContent(
                targetState = custo,
                transitionSpec = {
                    if (!Animacoes.ligadas) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else if (targetState > initialState) {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    } else {
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    }
                },
                label = "preço",
            ) { valor ->
                Text(
                    Dinheiro.rs(valor),
                    style = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.semantics { contentDescription = "$PRECO_NO_VISOR: ${Dinheiro.rs(valor)}" },
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Color.White.copy(alpha = 0.12f))
        Text(
            Calculadora.nomeDoRegistro(m, registro) + " · " + Calculadora.pedidoDoRegistro(registro),
            style = MaterialTheme.typography.bodySmall,
            color = Suave,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val vazio = linha.isEmpty()
        Text(
            if (vazio) Calculadora.textoAtual(m, config, registro) else Teclado.visor(linha),
            style = TextStyle(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = if (vazio) Suave.copy(alpha = 0.7f) else Color.White,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        mensagem?.let { r ->
            Text(
                r.mensagem,
                style = MaterialTheme.typography.bodySmall,
                color = if (r.erro) Erro else Dourado,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** O título de um grupo das listas ("CHANCES") com a dica à direita. */
@Composable
private fun TituloDoGrupo(titulo: String, dica: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 1.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            dica,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Uma linha que se escolhe para o visor (contorno dourado quando [escolhida]). */
@Composable
private fun LinhaEscolhivel(escolhida: Boolean, aoTocar: () -> Unit, fundo: Color = MaterialTheme.colorScheme.surfaceContainerLow, conteudo: @Composable RowScope.() -> Unit) {
    val cores = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (escolhida) cores.primary.copy(alpha = 0.12f) else fundo)
            .then(if (escolhida) Modifier.border(BorderStroke(1.5.dp, cores.primary), RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = aoTocar)
            .semantics { selected = escolhida }
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = conteudo,
    )
}

/** O "1 em X" e, embaixo (desde a 0.41 maior, com o "~" afastado), a porcentagem, alinhados à direita. */
@Composable
private fun ChanceAoLado(p: Double, cor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(horizontalAlignment = Alignment.End) {
        Text(Calculadora.umEm(p), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(porcentagemNaTela(p), style = MaterialTheme.typography.bodyMedium, color = cor, maxLines = 1)
    }
}

/** Uma faixa: o nome (na Dupla Sena com o "por sorteio" embaixo) e a chance. */
@Composable
private fun LinhaDaChance(nome: String, p: Double, porSorteio: Double?, escolhida: Boolean, aoTocar: () -> Unit) {
    LinhaEscolhivel(escolhida, aoTocar) {
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.bodyLarge, fontWeight = if (escolhida) FontWeight.Bold else FontWeight.Normal)
            if (porSorteio != null) {
                Text(
                    "por sorteio " + Calculadora.umEm(porSorteio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        ChanceAoLado(p)
    }
}

/** O "Qualquer prêmio" (à parte desde a 0.40): o Σ, o nome, a legenda e a chance, num fundo de outra cor. */
@Composable
private fun LinhaDoQualquer(p: Double, escolhida: Boolean, aoTocar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val suave = cores.onSecondaryContainer.copy(alpha = 0.8f)
    LinhaEscolhivel(escolhida, aoTocar, fundo = cores.secondaryContainer) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(cores.primary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text("Σ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cores.primary)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(Calculadora.NOME_QUALQUER, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = cores.onSecondaryContainer)
            Text(LEGENDA_QUALQUER, style = MaterialTheme.typography.bodySmall, color = suave)
        }
        Spacer(Modifier.width(8.dp))
        ChanceAoLado(p, suave)
    }
}

/** Um tipo de aposta: o nome e o preço de uma; à direita, quantas e o valor delas. */
@Composable
private fun LinhaDoTipo(nome: String, apostas: Int, valor: Long, precoDeUma: Long, escolhida: Boolean, aoTocar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    val usada = apostas > 0
    LinhaEscolhivel(escolhida, aoTocar) {
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.bodyLarge, fontWeight = if (escolhida || usada) FontWeight.SemiBold else FontWeight.Normal)
            Text("${Dinheiro.rs(precoDeUma)} cada", style = MaterialTheme.typography.bodySmall, color = cores.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Calculadora.quantas(apostas),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (usada) FontWeight.Bold else FontWeight.Normal,
                color = if (usada) cores.onSurface else cores.onSurfaceVariant,
                maxLines = 1,
            )
            if (usada) Text(Dinheiro.rs(valor), style = MaterialTheme.typography.bodyMedium, color = cores.onSurfaceVariant, maxLines = 1)
        }
    }
}

/**
 * O teclado (desde a 0.41): 5 linhas de 4 teclas, como o de uma calculadora de celular. O C apaga o que foi digitado; sem
 * nada digitado ele vira AC, que volta a calculadora ao começo. O % só vale para a chance de uma faixa ([comPorcento]).
 */
@Composable
private fun TecladoDaCalculadora(comPorcento: Boolean, temLinha: Boolean, aoTocar: (String) -> Unit, aoZerar: () -> Unit, aoIgual: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Surface(color = cores.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (temLinha) {
                    Tecla("C", nomeDaTecla(TECLA_LIMPAR), TipoDeTecla.LIMPAR) { aoTocar(TECLA_LIMPAR) }
                } else {
                    Tecla("AC", nomeDaTecla(TECLA_ZERAR), TipoDeTecla.LIMPAR) { aoZerar() }
                }
                Tecla("⌫", nomeDaTecla(TECLA_APAGAR), TipoDeTecla.SINAL, ligada = temLinha, icone = true) { aoTocar(TECLA_APAGAR) }
                Tecla("%", nomeDaTecla("%"), TipoDeTecla.SINAL, ligada = comPorcento) { aoTocar("%") }
                Tecla(Teclado.DIVIDIDO.toString(), nomeDaTecla("dividido"), TipoDeTecla.SINAL) { aoTocar(Teclado.DIVIDIDO.toString()) }
            }
            for ((a, b, c, sinal) in listOf(
                listOf("7", "8", "9", Teclado.VEZES.toString()),
                listOf("4", "5", "6", Teclado.MENOS.toString()),
                listOf("1", "2", "3", Teclado.MAIS.toString()),
            )) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Tecla(a, nomeDaTecla(a), TipoDeTecla.NUMERO) { aoTocar(a) }
                    Tecla(b, nomeDaTecla(b), TipoDeTecla.NUMERO) { aoTocar(b) }
                    Tecla(c, nomeDaTecla(c), TipoDeTecla.NUMERO) { aoTocar(c) }
                    val nome = when (sinal) {
                        Teclado.VEZES.toString() -> "vezes"
                        Teclado.MENOS.toString() -> "menos"
                        else -> "mais"
                    }
                    Tecla(sinal, nomeDaTecla(nome), TipoDeTecla.SINAL) { aoTocar(sinal) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Tecla("0", nomeDaTecla("0"), TipoDeTecla.NUMERO, peso = 2f) { aoTocar("0") }
                Tecla(",", nomeDaTecla("vírgula"), TipoDeTecla.NUMERO) { aoTocar(",") }
                Tecla("=", nomeDaTecla(TECLA_IGUAL), TipoDeTecla.IGUAL, ligada = temLinha) { aoIgual() }
            }
        }
    }
}

private enum class TipoDeTecla { NUMERO, SINAL, LIMPAR, IGUAL }

/** Uma tecla: [rotulo] na tela, [nome] para quem não vê (e os testes). */
@Composable
private fun RowScope.Tecla(
    rotulo: String,
    nome: String,
    tipo: TipoDeTecla,
    ligada: Boolean = true,
    peso: Float = 1f,
    icone: Boolean = false,
    aoTocar: () -> Unit,
) {
    val cores = MaterialTheme.colorScheme
    val (fundo, frente) = when (tipo) {
        TipoDeTecla.NUMERO -> cores.surfaceContainerHighest to cores.onSurface
        TipoDeTecla.SINAL -> cores.secondaryContainer to cores.onSecondaryContainer
        TipoDeTecla.LIMPAR -> cores.errorContainer to cores.onErrorContainer
        TipoDeTecla.IGUAL -> Dourado to AzulNoite
    }
    Surface(
        onClick = aoTocar,
        enabled = ligada,
        shape = RoundedCornerShape(16.dp),
        color = if (ligada) fundo else fundo.copy(alpha = 0.45f),
        contentColor = if (ligada) frente else frente.copy(alpha = 0.4f),
        modifier = Modifier
            .weight(peso)
            .height(ALTURA_DA_TECLA)
            .clearAndSetSemantics {
                contentDescription = nome
                role = Role.Button
                if (ligada) onClick { aoTocar(); true } else disabled()
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icone) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Text(rotulo, style = TextStyle(fontSize = 22.sp, fontWeight = if (tipo == TipoDeTecla.NUMERO) FontWeight.Medium else FontWeight.Bold), maxLines = 1)
            }
        }
    }
}

private val ALTURA_DA_TECLA = 46.dp

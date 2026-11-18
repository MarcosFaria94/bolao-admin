package io.github.marcosfaria94.bolao.ui.jogos

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.ConfigDaCalculadora
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json

/** O endereço da calculadora de probabilidades (desde a 0.38): "calculadora/MEGA_SENA". */
const val ROTA_CALCULADORA = "calculadora/{nome}"
const val TITULO_CALCULADORA = "Calculadora"

fun rotaDaCalculadora(m: Modalidade): String = "calculadora/${m.name}"

/** A calculadora existe em todas as modalidades menos a Federal. */
fun temCalculadora(m: Modalidade): Boolean = Calculadora.tem(m)

/** Os textos e nomes da calculadora (os testes procuram por eles). */
const val CONFIGURAR_A_CALCULADORA = "Configurar a calculadora"
const val REDEFINIR = "Redefinir"
const val TITULO_DO_PAINEL = "Probabilidades"

/** A explicação do "Qualquer prêmio", à parte no painel (desde a 0.40). */
const val LEGENDA_QUALQUER = "Todas as faixas juntas"
const val AMPLITUDE = "Amplitude dos controles"
const val FAIXAS_NO_PAINEL = "Faixas no painel"
const val TODAS_AS_FAIXAS = "Todas"

/** Os botões do ajuste fino de cada controle (desde a 0.40): "Menos uma: 8 números", "Mais uma: 8 números". */
fun menosUma(nome: String): String = "Menos uma: $nome"
fun maisUma(nome: String): String = "Mais uma: $nome"

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

/**
 * A calculadora de probabilidades da modalidade (desde a 0.38, pedido do usuário; o ícone da calculadora nos Detalhes, ao
 * lado das Estatísticas): o painel com a chance de cada faixa e, à parte (desde a 0.40), a de algum prêmio, com as
 * apostas escolhidas e o custo; o controle deslizante das apostas simples (começa em 1) e, desde a 0.40 (pedido do
 * usuário), um para cada tipo de aposta com mais números, todos na tela e começando em 0 (antes era preciso adicioná-los).
 * Cada controle é pequeno e tem − e + para o ajuste fino. A engrenagem muda a amplitude dos controles e as faixas do
 * painel; "Redefinir" volta ao padrão. A configuração fica guardada sozinha ([GuardaDaCalculadora]).
 */
@Composable
fun CalculadoraTela(estado: Estado, m: Modalidade) {
    val ctx = LocalContext.current
    var config by remember(m) { mutableStateOf(GuardaDaCalculadora.ler(ctx, m)) }
    var configurando by remember { mutableStateOf(false) }
    // Guarda a cada mudança e ao sair da tela.
    LaunchedEffect(config) { GuardaDaCalculadora.gravar(ctx, m, config) }
    val ultima by rememberUpdatedState(config)
    DisposableEffect(m) { onDispose { GuardaDaCalculadora.gravar(ctx, m, ultima) } }

    val preco = estado.config.preco(m)
    val simples = remember(m) { Calculadora.simples(m) }
    val maisNumeros = remember(m) { Calculadora.comMaisNumeros(m) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TarjaModalidade(m, Modifier.fillMaxWidth(), altura = 50.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Simule as apostas de um concurso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { config = ConfigDaCalculadora() }) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(REDEFINIR)
            }
            IconButton(onClick = { configurando = true }) {
                Icon(Icons.Outlined.Settings, contentDescription = CONFIGURAR_A_CALCULADORA)
            }
        }
        PainelDasChances(m, config, preco)
        // Cada tipo de aposta com o seu controle: a simples num cartão; as com mais números, todas, noutro.
        CartaoDosControles(null) {
            ControleDeApostas(
                nome = Calculadora.nomeDaLinha(m, simples.numeros, simples.trevos),
                valor = config.simples,
                maximo = config.maximo,
                aoMudar = { config = config.copy(simples = it) },
            )
        }
        if (maisNumeros.isNotEmpty()) {
            CartaoDosControles(Calculadora.tituloDosMaisNumeros(m)) {
                maisNumeros.forEachIndexed { i, l ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ControleDeApostas(
                        nome = Calculadora.nomeDaLinha(m, l.numeros, l.trevos),
                        valor = Calculadora.apostasDe(m, config, l.numeros, l.trevos),
                        maximo = config.maximo,
                        aoMudar = { v -> config = Calculadora.comApostas(m, config, l.numeros, l.trevos, v) },
                    )
                }
            }
        }
        notasDaCalculadora(m).forEach { n ->
            Text("* $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (configurando) {
        JanelaDaEngrenagem(m, config, aoMudar = { config = it }, aoFechar = { configurando = false })
    }
}

/** As notas do fim da calculadora (como a conta é feita). */
fun notasDaCalculadora(m: Modalidade): List<String> = listOfNotNull(
    "A chance de acertar cada faixa pelo menos uma vez, com todas essas apostas no mesmo concurso, considerando apostas diferentes umas das outras.",
    "Na Dupla Sena, cada aposta concorre nos 2 sorteios.".takeIf { m == Modalidade.DUPLA_SENA },
    "Na Loteca, supondo os 3 resultados igualmente prováveis em cada jogo.".takeIf { m == Modalidade.LOTECA },
    Probabilidades.LEGENDA_DA_MAIS_MILIONARIA.takeIf { m == Modalidade.MAIS_MILIONARIA },
)

/**
 * O painel: o total de apostas e o custo e, embaixo, cada faixa com o "1 em X" e a porcentagem. Desde a 0.40 (pedido do
 * usuário) o "Qualquer prêmio" fica à parte, num quadro de outra cor com o Σ e a [LEGENDA_QUALQUER]: é a junção de todas
 * as faixas, não uma faixa.
 */
@Composable
private fun PainelDasChances(m: Modalidade, config: ConfigDaCalculadora, preco: Long) {
    val chances = remember(m, config) { Calculadora.doPainel(m, config) }
    val faixas = chances.filter { it.indice != Calculadora.QUALQUER_PREMIO }
    val qualquer = chances.firstOrNull { it.indice == Calculadora.QUALQUER_PREMIO }
    val total = Calculadora.total(m, config)
    val custo = Calculadora.apostasSimples(m, config) * preco
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(TITULO_DO_PAINEL, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${Calculadora.quantas(total)} · ${Dinheiro.rs(custo)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (chances.isEmpty()) {
                Text("Nenhuma faixa escolhida na engrenagem.", style = MaterialTheme.typography.bodyMedium)
            }
            faixas.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(c.nome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    ChanceAoLado(c.p)
                }
            }
            if (qualquer != null) {
                if (faixas.isNotEmpty()) Spacer(Modifier.height(4.dp))
                QuadroDoQualquer(qualquer.p)
            }
        }
    }
}

/** O "1 em X" e, embaixo, a porcentagem, alinhados à direita. */
@Composable
private fun ChanceAoLado(p: Double, cor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(horizontalAlignment = Alignment.End) {
        Text(Calculadora.umEm(p), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(Calculadora.porcentagem(p), style = MaterialTheme.typography.bodySmall, color = cor, maxLines = 1)
    }
}

/**
 * O "Qualquer prêmio" à parte (desde a 0.40), num quadro de outra cor: em cima o Σ e o nome (a linha toda, para caber com a
 * letra grande); embaixo a [LEGENDA_QUALQUER] e a chance.
 */
@Composable
private fun QuadroDoQualquer(p: Double) {
    val cores = MaterialTheme.colorScheme
    val suave = cores.onSecondaryContainer.copy(alpha = 0.8f)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cores.secondaryContainer,
        contentColor = cores.onSecondaryContainer,
        border = BorderStroke(1.dp, cores.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(TAMANHO_DO_SIGMA).clip(CircleShape).background(cores.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Σ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cores.primary)
                }
                Spacer(Modifier.width(ESPACO_DO_SIGMA))
                Text(Calculadora.NOME_QUALQUER, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    LEGENDA_QUALQUER,
                    style = MaterialTheme.typography.bodySmall,
                    color = suave,
                    modifier = Modifier.weight(1f).padding(start = TAMANHO_DO_SIGMA + ESPACO_DO_SIGMA, end = 8.dp),
                )
                ChanceAoLado(p, suave)
            }
        }
    }
}

private val TAMANHO_DO_SIGMA = 28.dp
private val ESPACO_DO_SIGMA = 10.dp

/** Um cartão com os controles (o [titulo] em cima, se houver). */
@Composable
private fun CartaoDosControles(titulo: String?, conteudo: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            if (titulo != null) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
                )
            }
            conteudo()
        }
    }
}

/**
 * O controle de um tipo de aposta: o nome e quantas em cima; embaixo, o − , o controle deslizante (pequeno, desde a 0.40)
 * e o + (de uma em uma, para o ajuste fino, de 0 ao máximo da amplitude).
 */
@Composable
private fun ControleDeApostas(nome: String, valor: Int, maximo: Int, aoMudar: (Int) -> Unit) {
    val v = valor.coerceIn(0, maximo)
    val cores = MaterialTheme.colorScheme
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(
                Calculadora.quantas(valor),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (valor > 0) FontWeight.Bold else FontWeight.Normal,
                color = if (valor > 0) cores.onSurface else cores.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotaoDoAjuste(Icons.Outlined.Remove, menosUma(nome), v > 0) { aoMudar(v - 1) }
            ControleDeslizantePequeno(v, maximo, nome, aoMudar, Modifier.weight(1f).padding(horizontal = 6.dp))
            BotaoDoAjuste(Icons.Outlined.Add, maisUma(nome), v < maximo) { aoMudar(v + 1) }
        }
    }
}

/** O − ou o + de um controle (desde a 0.40): redondo, pequeno. */
@Composable
private fun BotaoDoAjuste(icone: ImageVector, nome: String, ligado: Boolean, aoTocar: () -> Unit) {
    val cores = MaterialTheme.colorScheme
    Surface(
        onClick = aoTocar,
        enabled = ligado,
        shape = CircleShape,
        color = cores.surfaceContainerHighest,
        contentColor = if (ligado) cores.onSurface else cores.onSurface.copy(alpha = 0.38f),
        modifier = Modifier.size(TAMANHO_DO_AJUSTE).semantics { contentDescription = nome },
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp)) }
    }
}

/**
 * O controle deslizante pequeno (desde a 0.40, pedido do usuário; o padrão do Material tem um pino de 44dp e um trilho de
 * 16dp): um pino redondo de [TAMANHO_DO_PINO] (cinza em 0) e um trilho de [ALTURA_DO_TRILHO], numa faixa de
 * [ALTURA_DO_CONTROLE].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControleDeslizantePequeno(valor: Int, maximo: Int, nome: String, aoMudar: (Int) -> Unit, modifier: Modifier) {
    val cores = MaterialTheme.colorScheme
    val ativo = cores.primary
    val inativo = cores.surfaceContainerHighest
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Slider(
            value = valor.toFloat(),
            onValueChange = { aoMudar(it.roundToInt()) },
            modifier = modifier.height(ALTURA_DO_CONTROLE).semantics { contentDescription = nome },
            valueRange = 0f..maximo.toFloat(),
            // O pino cinza em 0: os tipos que estão sendo usados se destacam na lista.
            thumb = { Box(Modifier.size(TAMANHO_DO_PINO).shadow(1.dp, CircleShape).background(if (valor > 0) ativo else cores.outline, CircleShape)) },
            track = { estado ->
                Canvas(Modifier.fillMaxWidth().height(ALTURA_DO_TRILHO)) {
                    val r = estado.valueRange
                    val f = if (r.endInclusive > r.start) ((estado.value - r.start) / (r.endInclusive - r.start)).coerceIn(0f, 1f) else 0f
                    val canto = CornerRadius(size.height / 2, size.height / 2)
                    drawRoundRect(inativo, cornerRadius = canto)
                    if (f > 0f) drawRoundRect(ativo, size = Size(size.width * f, size.height), cornerRadius = canto)
                }
            },
        )
    }
}

private val TAMANHO_DO_AJUSTE = 30.dp
private val TAMANHO_DO_PINO = 18.dp
private val ALTURA_DO_TRILHO = 6.dp
private val ALTURA_DO_CONTROLE = 28.dp

/** A engrenagem: a amplitude dos controles e as faixas do painel. */
@Composable
private fun JanelaDaEngrenagem(m: Modalidade, config: ConfigDaCalculadora, aoMudar: (ConfigDaCalculadora) -> Unit, aoFechar: () -> Unit) {
    val nomes = Calculadora.faixas(m)
    val indices = nomes.indices.toList() + Calculadora.QUALQUER_PREMIO
    val escolhidas = config.faixas ?: indices
    fun com(f: List<Int>) = aoMudar(config.copy(faixas = f.takeIf { it.toSet() != indices.toSet() }))
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(CONFIGURAR_A_CALCULADORA) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(AMPLITUDE, style = MaterialTheme.typography.titleSmall)
                Text("O máximo de cada controle deslizante.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Calculadora.AMPLITUDES.chunked(4).forEach { linha ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        linha.forEach { a ->
                            val sim = a == config.maximo
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sim) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .clickable {
                                        aoMudar(
                                            config.copy(
                                                maximo = a,
                                                simples = config.simples.coerceAtMost(a),
                                                extras = config.extras.map { it.copy(apostas = it.apostas.coerceAtMost(a)) },
                                            ),
                                        )
                                    }
                                    .semantics { contentDescription = "Amplitude $a" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    a.toString(),
                                    color = if (sim) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (sim) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        repeat(4 - linha.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(FAIXAS_NO_PAINEL, style = MaterialTheme.typography.titleSmall)
                LinhaDeEscolha(TODAS_AS_FAIXAS, config.faixas == null) { v -> com(if (v) indices else emptyList()) }
                indices.forEach { i ->
                    // O "Qualquer prêmio" separado das faixas (desde a 0.40: é a junção delas).
                    if (i == Calculadora.QUALQUER_PREMIO) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val nome = if (i == Calculadora.QUALQUER_PREMIO) Calculadora.NOME_QUALQUER else nomes[i]
                    LinhaDeEscolha(nome, i in escolhidas) { v -> com(if (v) escolhidas + i else escolhidas - i) }
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
    )
}

@Composable
private fun LinhaDeEscolha(nome: String, marcada: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .toggleable(value = marcada, role = Role.Checkbox, onValueChange = aoMudar),
    ) {
        Checkbox(checked = marcada, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text(nome, style = MaterialTheme.typography.bodyLarge)
    }
}

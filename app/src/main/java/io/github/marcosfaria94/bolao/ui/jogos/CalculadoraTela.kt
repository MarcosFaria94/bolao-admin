package io.github.marcosfaria94.bolao.ui.jogos

import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.ConfigDaCalculadora
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.ui.comum.JanelaComCampos
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/** O endereço da calculadora de probabilidades (desde a 0.38): "calculadora/MEGA_SENA". */
const val ROTA_CALCULADORA = "calculadora/{nome}"
const val TITULO_CALCULADORA = "Calculadora"

fun rotaDaCalculadora(m: Modalidade): String = "calculadora/${m.name}"

/** A calculadora existe em todas as modalidades menos a Federal. */
fun temCalculadora(m: Modalidade): Boolean = Calculadora.tem(m)

/** Os textos e nomes da calculadora (os testes procuram por eles). */
const val CONFIGURAR_A_CALCULADORA = "Configurar a calculadora"
const val REDEFINIR = "Redefinir"
const val ADICIONAR_APOSTAS = "Adicionar apostas com mais números"
const val TITULO_DO_PAINEL = "Probabilidades"
const val AMPLITUDE = "Amplitude dos controles"
const val FAIXAS_NO_PAINEL = "Faixas no painel"
const val TODAS_AS_FAIXAS = "Todas"

/** "Tirar as apostas de 8 números" (o X de cada controle a mais). */
fun tirarAsApostas(nome: String): String = "Tirar: $nome"

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
 * lado das Estatísticas): o painel com a chance de cada faixa (e de algum prêmio) com as apostas escolhidas e o custo; o
 * controle deslizante das apostas simples e um para cada tipo de aposta com mais números adicionado ("Adicionar apostas
 * com mais números": quantos números e quantas apostas). A engrenagem muda a amplitude dos controles e as faixas do
 * painel; "Redefinir" volta ao padrão. A configuração fica guardada sozinha ([GuardaDaCalculadora]).
 */
@Composable
fun CalculadoraTela(estado: Estado, m: Modalidade) {
    val ctx = LocalContext.current
    var config by remember(m) { mutableStateOf(GuardaDaCalculadora.ler(ctx, m)) }
    var configurando by remember { mutableStateOf(false) }
    var adicionando by remember { mutableStateOf(false) }
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
        ControleDeApostas(
            nome = Calculadora.nomeDaLinha(m, simples.numeros, simples.trevos, simples = true),
            valor = config.simples,
            maximo = config.maximo,
            aoMudar = { config = config.copy(simples = it) },
        )
        config.extras.forEachIndexed { i, e ->
            ControleDeApostas(
                nome = Calculadora.nomeDaLinha(m, e.numeros, e.trevos),
                valor = e.apostas,
                maximo = config.maximo,
                aoMudar = { v -> config = config.copy(extras = config.extras.mapIndexed { j, x -> if (j == i) x.copy(apostas = v) else x }) },
                aoTirar = { config = config.copy(extras = config.extras.filterIndexed { j, _ -> j != i }) },
            )
        }
        if (maisNumeros.isNotEmpty()) {
            OutlinedButton(onClick = { adicionando = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(ADICIONAR_APOSTAS, maxLines = 1)
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
    if (adicionando) {
        JanelaDeAdicionar(
            m, config.maximo,
            aoAdicionar = { n, t, q ->
                adicionando = false
                config = Calculadora.adicionar(m, config, n, t, q)
            },
            aoFechar = { adicionando = false },
        )
    }
}

/** As notas do fim da calculadora (como a conta é feita). */
fun notasDaCalculadora(m: Modalidade): List<String> = listOfNotNull(
    "A chance de acertar cada faixa pelo menos uma vez, com todas essas apostas no mesmo concurso, considerando apostas diferentes umas das outras.",
    "Na Dupla Sena, cada aposta concorre nos 2 sorteios.".takeIf { m == Modalidade.DUPLA_SENA },
    "Na Loteca, supondo os 3 resultados igualmente prováveis em cada jogo.".takeIf { m == Modalidade.LOTECA },
    Probabilidades.LEGENDA_DA_MAIS_MILIONARIA.takeIf { m == Modalidade.MAIS_MILIONARIA },
)

/** O painel: o total de apostas e o custo e, embaixo, cada faixa com o "1 em X" e a porcentagem. */
@Composable
private fun PainelDasChances(m: Modalidade, config: ConfigDaCalculadora, preco: Long) {
    val chances = remember(m, config) { Calculadora.doPainel(m, config) }
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
            chances.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val qualquer = c.indice == Calculadora.QUALQUER_PREMIO
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        c.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (qualquer) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Calculadora.umEm(c.p), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            Calculadora.porcentagem(c.p),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Um controle deslizante de um tipo de aposta: o nome, quantas e o X (nos adicionados). */
@Composable
private fun ControleDeApostas(nome: String, valor: Int, maximo: Int, aoMudar: (Int) -> Unit, aoTirar: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
                Text(nome, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(Calculadora.quantas(valor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (aoTirar != null) {
                    IconButton(onClick = aoTirar) { Icon(Icons.Outlined.Close, contentDescription = tirarAsApostas(nome)) }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
            }
            Slider(
                value = valor.coerceIn(0, maximo).toFloat(),
                onValueChange = { aoMudar(it.roundToInt()) },
                valueRange = 0f..maximo.toFloat(),
                modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = nome },
            )
        }
    }
}

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

/** "Adicionar apostas com mais números": quantos números (e trevos, na +Milionária) e quantas apostas. */
@Composable
private fun JanelaDeAdicionar(m: Modalidade, maximo: Int, aoAdicionar: (Int, Int, Int) -> Unit, aoFechar: () -> Unit) {
    val opcoes = remember(m) { Calculadora.comMaisNumeros(m) }
    val numeros = opcoes.map { it.numeros }.distinct().sorted()
    val trevos = opcoes.map { it.trevos }.distinct().sorted()
    var n by remember { mutableIntStateOf(numeros.firstOrNull { it > m.qtdAposta } ?: numeros.first()) }
    var t by remember { mutableIntStateOf(m.trevos) }
    var quantas by remember { mutableStateOf("1") }
    val q = quantas.toIntOrNull()?.takeIf { it in 1..maximo.coerceAtLeast(Calculadora.AMPLITUDES.last()) }
    val existe = Calculadora.linha(m, n, t) != null
    JanelaComCampos(
        titulo = "Adicionar apostas",
        aoFechar = aoFechar,
        botoes = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
            TextButton(onClick = { if (q != null) aoAdicionar(n, t, q) }, enabled = q != null && existe) { Text("Adicionar") }
        },
    ) {
        Escolha("Números jogados", n, (listOf(m.qtdAposta) + numeros).distinct().sorted()) { n = it }
        if (m.trevos > 0) Escolha("Trevos", t, (listOf(m.trevos) + trevos).distinct().sorted()) { t = it }
        OutlinedTextField(
            value = quantas,
            onValueChange = { novo -> quantas = novo.filter(Char::isDigit).take(4) },
            label = { Text("Número de apostas") },
            singleLine = true,
            isError = q == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Calculadora.linha(m, n, t)?.let { l ->
            Text(
                "Cada uma vale ${l.apostasSimples} ${if (l.apostasSimples == 1L) "aposta simples" else "apostas simples"}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Um valor com − e + (os números jogados, os trevos). */
@Composable
private fun Escolha(nome: String, valor: Int, opcoes: List<Int>, aoMudar: (Int) -> Unit) {
    val i = opcoes.indexOf(valor)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(nome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (i > 0) aoMudar(opcoes[i - 1]) }, enabled = i > 0) {
            Icon(Icons.Outlined.Remove, contentDescription = "Menos: $nome")
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Text(valor.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = { if (i < opcoes.lastIndex) aoMudar(opcoes[i + 1]) }, enabled = i in 0 until opcoes.lastIndex) {
            Icon(Icons.Outlined.Add, contentDescription = "Mais: $nome")
        }
    }
}

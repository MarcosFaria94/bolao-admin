package io.github.marcosfaria94.bolao.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.LimiteMensal
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.data.TipoDeLimite
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Limites
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.DialogoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.JanelaComCampos
import io.github.marcosfaria94.bolao.ui.comum.Secao
import io.github.marcosfaria94.bolao.ui.comum.trazerAoAbrir
import io.github.marcosfaria94.bolao.ui.home.SEM_DIA_DEFINIDO
import kotlinx.coroutines.launch

/** Os nomes dos parâmetros (as chaves da tela; os testes procuram por eles). */
const val PARAMETRO_SEM_CONFIRMACAO = "Registrar sem confirmação"
const val PARAMETRO_MAIS_NUMEROS = "Apostas com mais números"
const val PARAMETRO_TEIMOSINHA = "Teimosinha"
const val PARAMETRO_STOP_LOSS = "Stop loss"
const val PARAMETRO_SALDO = "Não jogar sem saldo"
const val PARAMETRO_APORTE = "Aporte mensal"
const val PARAMETRO_PRECO = "Preço da aposta simples"
const val PARAMETRO_CALENDARIO = "Calendário de apostas"
const val PARAMETRO_ACUMULOU = "Concurso acumulou"

/**
 * Os avisos com valor (desde a 0.38): o título e, ao lado, o valor num botão pequeno. Desde a 0.41, nas Notificações,
 * "k1 acima de [0,80]" e "Prêmio acima de [R$ 100 mi]" (eram "Avisar se k1 >" e "Avisar se algum prêmio >").
 */
const val PARAMETRO_K1 = "k1 acima de"
const val PARAMETRO_PREMIO = "Prêmio acima de"

/** O valor curto da linha do aviso do prêmio: "R$ 100 mi", "R$ 1,5 bi", "R$ 500 mil". */
fun valorCurto(centavos: Long): String {
    val reais = centavos / 100.0
    fun num(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.ROOT, "%.1f", v).replace('.', ',')
    return when {
        reais >= 1e9 -> "R$ ${num(reais / 1e9)} bi"
        reais >= 1e6 -> "R$ ${num(reais / 1e6)} mi"
        reais >= 1e3 -> "R$ ${num(reais / 1e3)} mil"
        else -> Dinheiro.rs(centavos)
    }
}

/** As explicações do "?" de cada parâmetro (desde a 0.38 a tela mostra só os títulos; a explicação abre no "?"). */
object AjudaDosParametros {
    const val APORTE = "Quanto cada integrante deposita no dia 1º de cada mês. Mudanças no aporte precisam da concordância de todos, registrada no grupo."
    const val PRECO = "O preço da aposta simples de cada modalidade (toque na modalidade para mudar). As apostas com mais números custam múltiplos dele. Também dá para mudar tocando no preço, no volante."
    const val SEM_CONFIRMACAO = "Registra a aposta direto, sem a mensagem \"Deseja registrar aposta?\"."
    const val MAIS_NUMEROS = "Deixa marcar mais números que a aposta simples. O preço acompanha a quantidade."
    const val TEIMOSINHA = "Mostra no volante a opção de repetir a mesma aposta em vários concursos seguidos (menos nos concursos especiais)."
    const val STOP_LOSS = "Um limite por mês: em número de apostas ou em dinheiro gasto. Passado o limite, o app não registra mais apostas até o mês virar. Sem limite é o padrão."
    const val SALDO = "Não deixa registrar a aposta que custa mais que o saldo do bolão. Desligado, a aposta é registrada mesmo com o saldo baixo (o padrão)."
    const val CALENDARIO = "Os dias do mês com aposta: a Home mostra a próxima aposta e, no dia, o \"Apostar\" pulsa. Sem dias marcados, fica \"Sem dia definido\"."
    const val CALENDARIO_BOLAO = " O provisionado do rateio acompanha o calendário: o aporte do mês dividido pelas apostas do mês."
    const val ACUMULOU = "Avisa no celular quando sai o resultado de um concurso que acumulou (ninguém acertou o prêmio principal)."
    const val K1 = "Avisa no celular quando o k1 do próximo concurso de uma modalidade passa do valor escolhido (toque no valor para mudar). O k1 é quanto volta, em média, de cada R\$ 1,00 apostado."
    const val PREMIO = "Avisa no celular quando o prêmio estimado do próximo concurso de alguma modalidade (ou de um concurso especial) passa do valor escolhido (toque no valor para mudar)."
}

/**
 * O calendário em poucas palavras ("10 dias por mês"). Até a 0.40 era o que a linha "Parâmetros" da lista dos Ajustes
 * mostrava à direita; desde a 0.41 o calendário fica nas Notificações (a linha mostra [RESUMO_DOS_PARAMETROS]).
 */
fun resumoDosParametros(p: Parametros): String = when {
    p.semDiaDefinido -> SEM_DIA_DEFINIDO
    p.dias.size == 1 -> "1 dia por mês"
    else -> "${p.dias.size} dias por mês"
}

/** O que a linha "Parâmetros" da lista dos Ajustes mostra à direita (desde a 0.41). */
const val RESUMO_DOS_PARAMETROS = "Preços"

/** O valor da linha do calendário nos Parâmetros (curto, para o título caber com a letra grande): "10 dias", "Nenhum". */
fun resumoCurtoDoCalendario(p: Parametros): String = when {
    p.semDiaDefinido -> "Nenhum"
    p.dias.size == 1 -> "1 dia"
    else -> "${p.dias.size} dias"
}

/** "10 apostas por mês: dias 1, 4, 7... e 28." */
fun textoDoCalendario(dias: List<Int>): String = when (dias.size) {
    0 -> "Nenhum dia marcado."
    1 -> "1 aposta por mês: dia ${dias[0]}."
    else -> "${dias.size} apostas por mês: dias ${Calendario.texto(dias)}."
}

/** O k1 digitado ("0,85" ou "0.85"); null se não for um número entre 0 e 10. */
fun lerK1(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it < 10 }

/**
 * Ajustes > Parâmetros (desde a 0.22; refeita na 0.38, pedido do usuário: "vai caber muitos outros parâmetros"). Cada
 * opção é uma linha só com o título, o "?" (que abre a explicação) e, no fim, a chave ou o valor. Grupos:
 * - Valores: o aporte mensal (só no bolão) e o preço da aposta simples, que abre e fecha embaixo da linha (começa fechado);
 * - Apostas: registrar sem confirmação, mais números, teimosinha, o stop loss (sem limite por padrão) e o "Não jogar sem
 *   saldo" (desligado por padrão; só no bolão);
 * - Avisos (até a 0.37, "Calendário de apostas"; desde a 0.41 em Ajustes > Notificações, pedido do usuário: "colocar
 *   Avisos em Notificações"; [AvisosDosJogosDaSessao]): o calendário, avisar se o concurso acumular, se o k1 passar de x
 *   e se algum prêmio passar de n. Continuam sendo da sessão (guardados nos parâmetros dela).
 * Cada toque já vale.
 */
@Composable
internal fun ParametrosDaSessao(estado: Estado) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val escopo = rememberCoroutineScope()
    val p = estado.parametros
    fun mudar(f: (Parametros) -> Parametros) {
        escopo.launch { runCatching { repo.atualizar { it.copy(config = it.config.copy(parametros = f(it.config.parametros))) } } }
    }
    var editandoAporte by remember { mutableStateOf(false) }
    var editandoPreco by remember { mutableStateOf<io.github.marcosfaria94.bolao.domain.Modalidade?>(null) }
    var precosAbertos by rememberSaveable { mutableStateOf(false) }
    var editandoStop by remember { mutableStateOf(false) }

    Secao("Valores") {
        if (!estado.individual) {
            LinhaDoParametro(PARAMETRO_APORTE, AjudaDosParametros.APORTE, aoTocar = { editandoAporte = true }) {
                ValorDoParametro(Dinheiro.rs(estado.config.aporteMensal))
            }
            Divisor()
        }
        Column(Modifier.trazerAoAbrir(precosAbertos)) {
            LinhaDoParametro(
                PARAMETRO_PRECO, AjudaDosParametros.PRECO,
                aoTocar = { precosAbertos = !precosAbertos },
                descricaoDoToque = if (precosAbertos) "Fechar os preços" else "Abrir os preços",
            ) {
                Icon(
                    if (precosAbertos) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (precosAbertos) GradePrecos(estado) { editandoPreco = it }
        }
    }

    Secao("Apostas") {
        ChaveDoParametro(PARAMETRO_SEM_CONFIRMACAO, AjudaDosParametros.SEM_CONFIRMACAO, p.semConfirmacao) { v -> mudar { it.copy(semConfirmacao = v) } }
        Divisor()
        ChaveDoParametro(PARAMETRO_MAIS_NUMEROS, AjudaDosParametros.MAIS_NUMEROS, p.maisNumeros) { v -> mudar { it.copy(maisNumeros = v) } }
        Divisor()
        ChaveDoParametro(PARAMETRO_TEIMOSINHA, AjudaDosParametros.TEIMOSINHA, p.teimosinha) { v -> mudar { it.copy(teimosinha = v) } }
        Divisor()
        LinhaDoParametro(PARAMETRO_STOP_LOSS, AjudaDosParametros.STOP_LOSS, aoTocar = { editandoStop = true }) {
            ValorDoParametro(Limites.resumo(p.limiteMensal), comSeta = true)
        }
        if (!estado.individual) {
            Divisor()
            ChaveDoParametro(PARAMETRO_SALDO, AjudaDosParametros.SALDO, p.exigirSaldo) { v -> mudar { it.copy(exigirSaldo = v) } }
        }
    }

    if (editandoAporte) {
        DialogoDinheiro(
            titulo = PARAMETRO_APORTE,
            inicial = estado.config.aporteMensal,
            rotulo = "Valor por integrante",
            explicacao = "Total do mês: ${Dinheiro.rs(estado.config.aporteMensal * estado.pessoas.size.coerceAtLeast(1))}. " +
                "Mudanças no aporte precisam da concordância dos ${estado.pessoas.size} integrantes, registrada no grupo.",
            aoConfirmar = { v ->
                editandoAporte = false
                if (v > 0) escopo.launch { repo.atualizar { it.copy(config = it.config.copy(aporteMensal = v)) } }
            },
            aoFechar = { editandoAporte = false },
        )
    }
    editandoPreco?.let { m ->
        DialogoDinheiro(
            titulo = m.nome,
            inicial = estado.config.preco(m),
            rotulo = PARAMETRO_PRECO,
            aoConfirmar = { v ->
                editandoPreco = null
                if (v > 0) escopo.launch {
                    repo.atualizar { it.copy(config = it.config.copy(precos = it.config.precos + (m.name to v))) }
                }
            },
            aoFechar = { editandoPreco = null },
        )
    }
    if (editandoStop) {
        JanelaDoStopLoss(p.limiteMensal, aoSalvar = { l -> editandoStop = false; mudar { it.copy(limiteMensal = l) } }, aoFechar = { editandoStop = false })
    }
}

@Composable
private fun Divisor() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

/**
 * Uma linha de parâmetro (desde a 0.38): o título, o "?" (abre a explicação numa janela) e, no fim, o [final] (a chave,
 * o valor, a seta). Com [aoTocar], a linha inteira é um botão.
 */
@Composable
private fun LinhaDoParametro(
    titulo: String,
    ajuda: String,
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
    descricaoDoToque: String? = null,
    final: @Composable () -> Unit,
) {
    var ajudando by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (aoTocar != null) Modifier.clickable(onClickLabel = descricaoDoToque, onClick = aoTocar) else Modifier)
            .padding(vertical = 4.dp),
    ) {
        // O título e o "?" logo depois dele; o resto da linha fica para o [final], no canto direito.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            BotaoDeAjuda(titulo) { ajudando = true }
        }
        Spacer(Modifier.width(4.dp))
        final()
    }
    if (ajudando) {
        AlertDialog(
            onDismissRequest = { ajudando = false },
            title = { Text(titulo) },
            text = { Text(ajuda) },
            confirmButton = { TextButton(onClick = { ajudando = false }) { Text("OK") } },
        )
    }
}

/** O "?" ao lado do título (desde a 0.38). */
@Composable
private fun BotaoDeAjuda(titulo: String, aoTocar: () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(onClick = aoTocar, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = descricaoDaAjuda(titulo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** "O que é: Stop loss" (o "?" para quem não vê a tela, e os testes). */
fun descricaoDaAjuda(titulo: String): String = "O que é: $titulo"

/** O valor à direita da linha (com a seta, quando tocar abre uma janela). */
@Composable
private fun ValorDoParametro(texto: String, comSeta: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (comSeta) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Uma chave de ligar e desligar (desde a 0.38 só o título e o "?"; a linha inteira liga e desliga). Com [valor], um botão
 * pequeno com o valor fica antes da chave (tocar muda o valor).
 */
@Composable
private fun ChaveDoParametro(
    titulo: String,
    ajuda: String,
    ligada: Boolean,
    valor: String? = null,
    aoTocarNoValor: (() -> Unit)? = null,
    aoMudar: (Boolean) -> Unit,
) {
    LinhaDoParametro(
        titulo, ajuda,
        modifier = Modifier.toggleable(value = ligada, role = Role.Switch, onValueChange = aoMudar),
    ) {
        if (valor != null && aoTocarNoValor != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = "Mudar o valor", onClick = aoTocarNoValor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(valor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
        }
        Switch(checked = ligada, onCheckedChange = null)
    }
}

/** A janela do stop loss (desde a 0.38): sem limite, por número de apostas ou por dinheiro gasto no mês. */
@Composable
private fun JanelaDoStopLoss(atual: LimiteMensal?, aoSalvar: (LimiteMensal?) -> Unit, aoFechar: () -> Unit) {
    var tipo by remember { mutableStateOf(atual?.tipo) }
    var apostas by remember { mutableStateOf(atual?.takeIf { it.tipo == TipoDeLimite.APOSTAS }?.valor?.toString() ?: "20") }
    var dinheiro by remember { mutableLongStateOf(atual?.takeIf { it.tipo == TipoDeLimite.DINHEIRO }?.valor ?: 100_00L) }
    val limite: LimiteMensal? = when (tipo) {
        null -> null
        TipoDeLimite.APOSTAS -> apostas.toLongOrNull()?.takeIf { it > 0 }?.let { LimiteMensal(TipoDeLimite.APOSTAS, it) }
        TipoDeLimite.DINHEIRO -> dinheiro.takeIf { it > 0 }?.let { LimiteMensal(TipoDeLimite.DINHEIRO, it) }
    }
    val valido = tipo == null || limite != null
    JanelaComCampos(
        titulo = PARAMETRO_STOP_LOSS,
        aoFechar = aoFechar,
        botoes = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
            TextButton(onClick = { aoSalvar(limite) }, enabled = valido) { Text("Salvar") }
        },
    ) {
        Text("O limite de cada mês.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OpcaoDoStop(Limites.SEM_LIMITE, tipo == null) { tipo = null }
        OpcaoDoStop("Número de apostas", tipo == TipoDeLimite.APOSTAS) { tipo = TipoDeLimite.APOSTAS }
        if (tipo == TipoDeLimite.APOSTAS) {
            OutlinedTextField(
                value = apostas,
                onValueChange = { novo -> apostas = novo.filter(Char::isDigit).take(5) },
                label = { Text("Apostas por mês") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OpcaoDoStop("Dinheiro gasto", tipo == TipoDeLimite.DINHEIRO) { tipo = TipoDeLimite.DINHEIRO }
        if (tipo == TipoDeLimite.DINHEIRO) {
            CampoDinheiro(centavos = dinheiro, aoMudar = { dinheiro = it }, rotulo = "Gasto máximo por mês", imeAction = ImeAction.Done, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OpcaoDoStop(texto: String, escolhida: Boolean, aoEscolher: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .selectable(selected = escolhida, role = Role.RadioButton, onClick = aoEscolher)
            .padding(vertical = 2.dp),
    ) {
        RadioButton(selected = escolhida, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A janela do k1 do aviso (desde a 0.38). */
@Composable
internal fun JanelaDoK1(atual: Double, aoSalvar: (Double) -> Unit, aoFechar: () -> Unit) {
    var texto by remember { mutableStateOf(IndiceK.exibir(atual, 2)) }
    val lido = lerK1(texto)
    JanelaComCampos(
        titulo = PARAMETRO_K1,
        aoFechar = aoFechar,
        botoes = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
            TextButton(onClick = { lido?.let(aoSalvar) }, enabled = lido != null) { Text("Salvar") }
        },
    ) {
        Text(AjudaDosParametros.K1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = texto,
            onValueChange = { novo -> texto = novo.filter { it.isDigit() || it == ',' || it == '.' }.take(5) },
            label = { Text("k1") },
            singleLine = true,
            isError = lido == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A janela do prêmio do aviso (desde a 0.38). */
@Composable
internal fun JanelaDoPremio(atual: Long, aoSalvar: (Long) -> Unit, aoFechar: () -> Unit) {
    var v by remember { mutableLongStateOf(atual) }
    JanelaComCampos(
        titulo = PARAMETRO_PREMIO,
        aoFechar = aoFechar,
        botoes = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
            TextButton(onClick = { aoSalvar(v) }, enabled = v > 0) { Text("Salvar") }
        },
    ) {
        Text(AjudaDosParametros.PREMIO, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        CampoDinheiro(centavos = v, aoMudar = { v = it }, rotulo = "Prêmio estimado", imeAction = ImeAction.Done, modifier = Modifier.fillMaxWidth())
    }
}

/** A janela do calendário de apostas (desde a 0.38; até a 0.37, os dias ficavam direto na tela): os dias do mês. */
@Composable
internal fun JanelaDoCalendario(estado: Estado, p: Parametros, mudar: ((Parametros) -> Parametros) -> Unit, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(PARAMETRO_CALENDARIO) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = p.semDiaDefinido, role = Role.Switch) { v -> mudar { it.copy(diasDeAposta = if (v) emptyList() else null) } },
                ) {
                    Text(SEM_DIA_DEFINIDO, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = p.semDiaDefinido, onCheckedChange = null)
                }
                if (!p.semDiaDefinido) {
                    Text("Toque nos dias do mês com aposta:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GradeDeDias(p.dias) { dia ->
                        mudar { atual ->
                            val novos = if (dia in atual.dias) atual.dias - dia else (atual.dias + dia).sorted()
                            // Os mesmos dias do termo voltam a ser "os do termo" (null).
                            atual.copy(diasDeAposta = novos.takeIf { it != Calendario.DIAS_PROGRAMADOS })
                        }
                    }
                    Text(textoDoCalendario(p.dias), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (p.dias.any { it > 28 }) {
                        Text(
                            "Num mês que não tem o dia marcado (29, 30 ou 31), esse dia fica sem aposta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
        dismissButton = if (p.diasDeAposta != null) {
            { TextButton(onClick = { mudar { it.copy(diasDeAposta = null) } }) { Text(if (estado.enviaAoGrupo) "Dias do termo" else "Dias padrão") } }
        } else {
            null
        },
    )
}

/** Os dias do mês (1 a 31) em 7 colunas: os marcados na cor do app; tocar marca e desmarca. */
@Composable
private fun GradeDeDias(marcados: List<Int>, aoTocar: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        (1..31).chunked(7).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                linha.forEach { dia ->
                    val marcado = dia in marcados
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (marcado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { aoTocar(dia) }
                            .semantics {
                                contentDescription = "Dia $dia"
                                selected = marcado
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            dia.toString(),
                            fontSize = 14.sp,
                            fontWeight = if (marcado) FontWeight.Bold else FontWeight.Normal,
                            color = if (marcado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(7 - linha.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

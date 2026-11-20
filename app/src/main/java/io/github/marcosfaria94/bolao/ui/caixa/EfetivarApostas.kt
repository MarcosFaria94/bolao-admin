package io.github.marcosfaria94.bolao.ui.caixa

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Efetivacao
import io.github.marcosfaria94.bolao.domain.Efetivacao.Situacao
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import kotlinx.coroutines.launch
import java.time.LocalDate

/** O nome do botão flutuante e o título da área (desde a 0.42; até a 0.41, o "Transcrever em lote" da barra de cima). */
const val EFETIVAR_APOSTAS = "Efetivar apostas"

/** O nome do botão flutuante para quem não vê a tela (e os testes): "Efetivar apostas: 3 para efetivar". */
fun nomeDoBotaoDeEfetivar(pendentes: Int): String = when (pendentes) {
    0 -> "$EFETIVAR_APOSTAS: nenhuma para efetivar"
    1 -> "$EFETIVAR_APOSTAS: 1 para efetivar"
    else -> "$EFETIVAR_APOSTAS: $pendentes para efetivar"
}

const val FECHAR_EFETIVAR = "Fechar a área de efetivar"
const val VERIFICAR_SESSAO = "Verificar a sessão de novo"
const val ENTRAR_NO_SITE = "Entrar no site"
const val MARCAR_TUDO = "Tudo"
const val MARCAR_NENHUMA = "Nenhuma"

/** O nome da caixa de marcar de cada aposta: "Marcar Aposta Nº3 (Mega-Sena 2950)". */
fun nomeDaMarca(l: Efetivacao.Linha): String = "Marcar ${l.item.titulo} (${l.modalidade.nome} ${l.concurso})"

/**
 * O botão "Efetivar apostas" do app (desde a 0.42), para quando o script não pôs o da própria página (o ícone dos cookies não
 * apareceu): do tamanho, da cor e da transparência do cadeado ([corDoCadeado]), logo acima dele. Com uma aposta ou mais por
 * efetivar ele chama atenção: o número num selo vermelho e um anel dourado que pulsa em volta.
 */
@Composable
fun BoxScope.BotaoDeEfetivar(navegador: NavegadorDaCaixa, medida: MedidaDaPagina, pendentes: Int) {
    val densidade = LocalDensity.current
    val c = navegador.cookie
    if (c?.naPagina == true) return
    val lugar = lugarDoCadeado(c, medida.largura.toFloat(), medida.altura.toFloat(), densidade.density)
    val fundo = corDoCadeado(c)
    val chama = pendentes > 0
    val pulso = rememberInfiniteTransition(label = "efetivar")
    val escala by pulso.animateFloat(1f, 1.35f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "anel")
    val alfa by pulso.animateFloat(0.85f, 0f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "alfa")
    with(densidade) {
        val tamanho = lugar.tamanho.toDp()
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = lugar.direita.toDp(), bottom = (lugar.baixo + lugar.tamanho).toDp() + VAO_DOS_BOTOES)
                .size(tamanho),
        ) {
            if (chama) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .scale(escala)
                        .border(2.dp, Dourado.copy(alpha = alfa), CircleShape),
                )
            }
            Surface(
                onClick = { navegador.pedirEfetivar = true },
                shape = CircleShape,
                color = fundo,
                contentColor = corDoIconeDoCadeado(fundo),
                border = if (chama) BorderStroke(2.dp, Dourado) else null,
                modifier = Modifier.fillMaxSize().semantics { contentDescription = nomeDoBotaoDeEfetivar(pendentes) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(tamanho * 0.5f))
                }
            }
            if (chama) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .heightIn(min = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE53935))
                        .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (pendentes > 99) "99+" else "$pendentes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** O espaço entre o cadeado e o botão de efetivar (o mesmo do cadeado e o ícone dos cookies, no script). */
private val VAO_DOS_BOTOES = 8.dp

/**
 * A área "Efetivar apostas" (desde a 0.42): a tela inteira por cima da Caixa.
 * - **Em cima**, a sessão do site (verificada ao abrir; com a conta fechada, o "Entrar no site" fecha a área para entrar);
 * - **o meio**, as apostas que esperam o site, separadas por modalidade e concurso, cada uma com a caixa de marcar e a
 *   situação (não efetivada, na fila, indo, no carrinho, não foi). As não efetivadas vêm marcadas ("Tudo"); as do carrinho
 *   podem ser marcadas de novo (para quando saíram do carrinho do site);
 * - **embaixo**, o "Efetivar N apostas" (antes de tudo o app confere a sessão de novo).
 * Durante o lote a área não fecha (o site está sendo usado) e a situação de cada aposta muda na hora.
 */
@Composable
fun AreaDeEfetivar(estado: Estado, navegador: NavegadorDaCaixa, aoFechar: () -> Unit) {
    val escopo = rememberCoroutineScope()
    val lista = remember(estado) { Efetivacao.lista(estado, LocalDate.now()) }
    val grupos = remember(lista) { Efetivacao.grupos(lista) }
    var escolhidas by remember { mutableStateOf(Efetivacao.preSelecionadas(lista, navegador.transcritas, navegador.situacoes)) }
    var rodou by remember { mutableStateOf(false) }
    val rodando = navegador.efetivando
    LaunchedEffect(Unit) { navegador.verificarSessao() }
    // No fim do lote: as que foram para o carrinho saem da marcação (as que não foram continuam marcadas).
    LaunchedEffect(rodando) {
        if (!rodando && rodou) escolhidas = escolhidas.filter { navegador.situacoes[it] != Situacao.NoCarrinho }.toSet()
    }
    fun situacao(l: Efetivacao.Linha) = Efetivacao.situacao(l, navegador.transcritas, navegador.situacoes)
    val naoNoCarrinho = lista.filter { situacao(it) != Situacao.NoCarrinho }.map { it.chave }.toSet()

    Dialog(
        onDismissRequest = { if (!rodando) aoFechar() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = aoFechar, enabled = !rodando) { Icon(Icons.Outlined.Close, FECHAR_EFETIVAR) }
                    Text(EFETIVAR_APOSTAS, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                FaixaDaSessao(
                    navegador.sessao,
                    rodando,
                    aoVerificar = { escopo.launch { navegador.verificarSessao() } },
                    aoEntrar = aoFechar,
                )
                if (lista.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhuma aposta esperando o site.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = escolhidas == naoNoCarrinho && naoNoCarrinho.isNotEmpty(),
                            onClick = { escolhidas = naoNoCarrinho },
                            enabled = !rodando,
                            label = { Text(MARCAR_TUDO) },
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = escolhidas.isEmpty(), onClick = { escolhidas = emptySet() }, enabled = !rodando, label = { Text(MARCAR_NENHUMA) })
                        Spacer(Modifier.weight(1f))
                        Text(Efetivacao.resumo(lista, escolhidas), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (rodou && !rodando) ResumoDoLote(lista.map { situacao(it) })
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                        grupos.forEach { g ->
                            item(key = "g:${g.modalidade}:${g.concurso}:${g.especial}") { CabecalhoDoGrupo(g) }
                            g.linhas.forEach { l ->
                                item(key = l.chave) {
                                    LinhaDeEfetivar(
                                        l,
                                        situacao(l),
                                        marcada = l.chave in escolhidas,
                                        habilitada = !rodando,
                                        aoMarcar = { escolhidas = if (l.chave in escolhidas) escolhidas - l.chave else escolhidas + l.chave },
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    if (!estado.individual) {
                        Text(
                            "O pagamento continua no site. Depois de pagar, confirme aqui na faixa do carrinho.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    val marcadas = lista.filter { it.chave in escolhidas }
                    val fechada = navegador.sessao == Efetivacao.Sessao.DESLOGADA
                    Button(
                        onClick = {
                            rodou = true
                            escopo.launch { navegador.efetivar(marcadas) }
                        },
                        enabled = marcadas.isNotEmpty() && !rodando && !fechada && navegador.sessao != Efetivacao.Sessao.VERIFICANDO,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulMarinho, contentColor = Dourado),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) {
                        if (rodando) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Dourado)
                            Spacer(Modifier.width(10.dp))
                            val feitas = lista.count { navegador.situacoes[it.chave].let { s -> s != null && s != Situacao.NaFila && s != Situacao.Indo } && it.chave in escolhidas }
                            Text("Efetivando ${feitas + 1} de ${marcadas.size}…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Outlined.AddShoppingCart, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    fechada -> "Entre na sua conta para efetivar"
                                    marcadas.isEmpty() -> "Marque as apostas"
                                    else -> Efetivacao.textoDoBotao(marcadas.size) + " · " + Dinheiro.rs(marcadas.sumOf { it.valor })
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A faixa da sessão do site, no alto da área: verde (aberta), vermelha (expirada), cinza (verificando ou sem saber). */
@Composable
private fun FaixaDaSessao(s: Efetivacao.Sessao, rodando: Boolean, aoVerificar: () -> Unit, aoEntrar: () -> Unit) {
    val (fundo, frente) = when (s) {
        Efetivacao.Sessao.LOGADA -> Color(0xFF1B5E20).copy(alpha = 0.14f) to Color(0xFF2E7D32)
        Efetivacao.Sessao.DESLOGADA -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(fundo)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (s) {
            Efetivacao.Sessao.VERIFICANDO -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Efetivacao.Sessao.LOGADA -> Icon(Icons.Outlined.CheckCircle, null, tint = frente, modifier = Modifier.size(20.dp))
            Efetivacao.Sessao.DESLOGADA -> Icon(Icons.Outlined.ErrorOutline, null, tint = frente, modifier = Modifier.size(20.dp))
            Efetivacao.Sessao.DESCONHECIDA -> Icon(Icons.AutoMirrored.Outlined.HelpOutline, null, tint = frente, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(Efetivacao.textoDaSessao(s), style = MaterialTheme.typography.bodyMedium, color = if (s == Efetivacao.Sessao.DESLOGADA) frente else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (s == Efetivacao.Sessao.DESLOGADA) {
            TextButton(onClick = aoEntrar) { Text(ENTRAR_NO_SITE, fontWeight = FontWeight.Bold) }
        }
        // Verificar de novo (também com a sessão expirada: depois de entrar, ou se a página enganou a verificação).
        if (s != Efetivacao.Sessao.VERIFICANDO && !rodando) {
            IconButton(onClick = aoVerificar) { Icon(Icons.Outlined.Refresh, VERIFICAR_SESSAO, modifier = Modifier.size(20.dp)) }
        }
    }
}

/** No fim do lote: quantas foram para o carrinho e quantas não. */
@Composable
private fun ResumoDoLote(situacoes: List<Situacao>) {
    val ok = situacoes.count { it == Situacao.NoCarrinho }
    val falhas = situacoes.count { it is Situacao.Falhou || it == Situacao.NaoEnviada }
    if (ok == 0 && falhas == 0) return
    Text(
        listOfNotNull(
            "$ok no carrinho".takeIf { ok > 0 },
            (if (falhas == 1) "1 não foi" else "$falhas não foram").takeIf { falhas > 0 },
        ).joinToString(" · ") + if (falhas > 0) ": continuam marcadas para tentar de novo." else ".",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/** O alto de cada grupo: o selo, a modalidade, o concurso e o dia do sorteio. */
@Composable
private fun CabecalhoDoGrupo(g: Efetivacao.Grupo) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        SeloModalidade(g.modalidade, tamanho = 24.dp)
        Spacer(Modifier.width(8.dp))
        Text(g.modalidade.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            listOfNotNull(" · Concurso ${g.concurso}", " (especial)".takeIf { g.especial }, Efetivacao.dia(g.sorteio)?.let { " · $it" }).joinToString(""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Uma aposta da área: a caixa de marcar, o título, o jogo e o preço, e a situação à direita (o porquê embaixo, se não foi). */
@Composable
private fun LinhaDeEfetivar(l: Efetivacao.Linha, s: Situacao, marcada: Boolean, habilitada: Boolean, aoMarcar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (marcada) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Column(
            Modifier
                .clickable(enabled = habilitada, role = Role.Checkbox, onClick = aoMarcar)
                .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = marcada,
                    onCheckedChange = { aoMarcar() },
                    enabled = habilitada,
                    modifier = Modifier.semantics { contentDescription = nomeDaMarca(l) },
                )
                Column(Modifier.weight(1f)) {
                    Text(l.item.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(l.item.detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Text(Dinheiro.rs(l.valor), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                EtiquetaDaSituacao(s)
            }
            if (s is Situacao.Falhou) {
                Text(
                    s.mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 48.dp, top = 2.dp),
                )
            } else if (s == Situacao.NoCarrinho) {
                Text(
                    "Saiu do carrinho do site? Marque para mandar de novo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, top = 2.dp),
                )
            }
        }
    }
}

/** A etiqueta colorida da situação. */
@Composable
private fun EtiquetaDaSituacao(s: Situacao) {
    val verde = Color(0xFF2E7D32)
    val (cor, icone) = when (s) {
        Situacao.NaoEfetivada -> Color(0xFFB26A00) to Icons.Outlined.Schedule
        Situacao.NaFila -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Outlined.Schedule
        Situacao.Indo -> MaterialTheme.colorScheme.primary to null
        Situacao.NoCarrinho -> verde to Icons.Outlined.CheckCircle
        is Situacao.Falhou -> MaterialTheme.colorScheme.error to Icons.Outlined.ErrorOutline
        Situacao.NaoEnviada -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Outlined.ErrorOutline
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, cor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .background(cor.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icone != null) Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp))
        else CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = cor)
        Text(Efetivacao.etiqueta(s), style = MaterialTheme.typography.labelSmall, color = cor, fontWeight = FontWeight.SemiBold)
    }
}

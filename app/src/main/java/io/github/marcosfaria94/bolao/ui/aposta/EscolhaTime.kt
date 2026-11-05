package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.LeituraComprovante
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.TIMES
import io.github.marcosfaria94.bolao.domain.TimeDoCoracao
import io.github.marcosfaria94.bolao.domain.timeDoCoracao
import io.github.marcosfaria94.bolao.net.Brasoes
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import java.text.Collator
import java.util.Locale
import kotlin.random.Random

/** "Vasco da Gama / RJ" (o time da lista) ou o texto digitado à mão. */
fun nomeDoTime(caixa: String?): String? {
    if (caixa.isNullOrBlank()) return null
    val t = timeDoCoracao(caixa) ?: return caixa
    return "${t.nome} / ${t.uf}"
}

/** Botão do Time do Coração (Timemania): o brasão e o nome do time; abre a lista dos times. */
@Composable
fun BotaoTimeDoCoracao(atual: String?, aoEscolher: (String) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    val time = timeDoCoracao(atual)
    OutlinedButton(
        onClick = { aberto = true },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (time != null) {
            Brasao(time, 34.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Time do Coração", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(nomeDoTime(atual) ?: "Escolher o time", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("Trocar", style = MaterialTheme.typography.labelLarge)
    }
    if (aberto) {
        DialogoTimes(
            atual = atual,
            aoEscolher = {
                aoEscolher(it)
                aberto = false
            },
            aoFechar = { aberto = false },
        )
    }
}

/** O brasão do time (da Wikipédia, guardado no celular); enquanto não chega (ou sem internet), um escudo genérico. */
@Composable
fun Brasao(time: TimeDoCoracao, tamanho: Dp = 36.dp) {
    val ctx = LocalContext.current.applicationContext
    val imagem by produceState(Brasoes.naMemoria(time), time) {
        if (value == null) value = Brasoes.carregar(ctx, time)
    }
    Box(Modifier.size(tamanho), contentAlignment = Alignment.Center) {
        val i = imagem
        if (i != null) {
            Image(i, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(tamanho * 0.8f),
            )
        }
    }
}

/** Por onde a lista dos times está classificada. */
enum class OrdemDosTimes { NOME, UF }

/**
 * Os times na ordem pedida: por nome (e a UF, nos nomes iguais) ou por UF (e o nome dentro da
 * UF), em ordem alfabética sem diferença de acento; [crescente] = de A a Z.
 */
fun timesEmOrdem(lista: List<TimeDoCoracao>, ordem: OrdemDosTimes, crescente: Boolean): List<TimeDoCoracao> {
    val alfabeto = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply { strength = Collator.PRIMARY }
    val porNome = Comparator<TimeDoCoracao> { a, b -> alfabeto.compare(a.nome, b.nome) }
    val porUf = Comparator<TimeDoCoracao> { a, b -> alfabeto.compare(a.uf, b.uf) }
    val principal = if (ordem == OrdemDosTimes.NOME) porNome else porUf
    val desempate = if (ordem == OrdemDosTimes.NOME) porUf else porNome
    return lista.sortedWith((if (crescente) principal else principal.reversed()).then(desempate))
}

/** Largura da coluna da UF (a lista e o cabeçalho). */
private val COLUNA_UF = 64.dp

/** Tamanho do brasão na lista. */
private val BRASAO = 36.dp

/**
 * Um time sorteado para o "Sortear" do Time do Coração: da [lista] (a da busca, se tiver algum time;
 * senão, todos), sem repetir o [atual] quando dá.
 */
fun timeAleatorio(lista: List<TimeDoCoracao>, atual: String?, sorteio: Random = Random.Default): TimeDoCoracao {
    val base = lista.ifEmpty { TIMES }
    val escolhido = atual?.let { Conferencia.normalizar(it) }
    val outros = base.filter { Conferencia.normalizar(it.caixa) != escolhido }.ifEmpty { base }
    return outros.random(sorteio)
}

/**
 * A lista dos times: um por linha (o brasão, o nome e a UF), em ordem alfabética. Tocar em "Nome"
 * ou em "UF" (no alto da lista) classifica por aquela coluna; tocar de novo inverte. Dá para buscar
 * e, no alto, o "Sortear" escolhe um time ao acaso (da busca, se houver). Desde a 0.17 não há mais
 * o campo "Outro time": só os times da lista da Caixa.
 */
@Composable
fun DialogoTimes(atual: String?, aoEscolher: (String) -> Unit, aoFechar: () -> Unit) {
    var busca by remember { mutableStateOf("") }
    var ordem by remember { mutableStateOf(OrdemDosTimes.NOME) }
    var crescente by remember { mutableStateOf(true) }
    val cor = corModalidade(Modalidade.TIMEMANIA.cor)
    val filtro = LeituraComprovante.simples(busca).filter { it.isLetterOrDigit() }
    val lista = timesEmOrdem(
        TIMES.filter { t -> filtro.isEmpty() || filtro in LeituraComprovante.simples(t.nome + t.caixa).filter { it.isLetterOrDigit() } },
        ordem,
        crescente,
    )
    val escolhido = atual?.let { Conferencia.normalizar(it) }
    // Ao classificar (ou buscar), a lista volta ao começo: senão ela ficaria presa no time que estava no alto.
    val rolagem = rememberLazyListState()
    LaunchedEffect(ordem, crescente, filtro) { rolagem.scrollToItem(0) }
    fun classificar(por: OrdemDosTimes) {
        if (ordem == por) crescente = !crescente else {
            ordem = por
            crescente = true
        }
    }

    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 12.dp)) {
                    IconButton(onClick = aoFechar) { Icon(Icons.Outlined.Close, contentDescription = "Fechar") }
                    Text("Time do Coração", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    // Um time ao acaso (da busca, se houver): escolhe e fecha, como tocar no time.
                    FilledTonalButton(
                        onClick = { aoEscolher(timeAleatorio(lista, atual).caixa) },
                        contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Casino, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sortear", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
                    }
                }
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it.take(30) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Buscar time") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                // O cabeçalho da lista: tocar em "Nome" ou "UF" classifica por aquela coluna.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp),
                ) {
                    Spacer(Modifier.width(BRASAO + 14.dp))
                    Cabecalho("Nome", ordem == OrdemDosTimes.NOME, crescente, Modifier.weight(1f)) { classificar(OrdemDosTimes.NOME) }
                    Cabecalho("UF", ordem == OrdemDosTimes.UF, crescente, Modifier.width(COLUNA_UF), centro = true) { classificar(OrdemDosTimes.UF) }
                }
                HorizontalDivider()
                LazyColumn(Modifier.weight(1f), state = rolagem, contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(lista, key = { it.caixa }) { t ->
                        val este = escolhido != null && Conferencia.normalizar(t.caixa) == escolhido
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (este) cor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
                                .clickable(role = Role.Button) { aoEscolher(t.caixa) }
                                .heightIn(min = 58.dp)
                                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        ) {
                            Brasao(t, BRASAO)
                            Spacer(Modifier.width(14.dp))
                            Text(
                                t.nome,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                fontWeight = if (este) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (este) Icon(Icons.Outlined.Check, contentDescription = "Escolhido", tint = cor, modifier = Modifier.size(20.dp))
                            Text(
                                t.uf,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(COLUNA_UF),
                            )
                        }
                        HorizontalDivider(Modifier.padding(start = 16.dp + BRASAO + 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (lista.isEmpty()) {
                        item(key = "nenhum") {
                            Text(
                                "Nenhum time com \"$busca\".",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Uma coluna do cabeçalho da lista dos times: o nome e, na coluna da classificação, a seta (para cima = A a Z). */
@Composable
private fun Cabecalho(titulo: String, ativa: Boolean, crescente: Boolean, modifier: Modifier, centro: Boolean = false, aoTocar: () -> Unit) {
    val cor = if (ativa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centro) Arrangement.Center else Arrangement.Start,
        modifier = modifier
            .clickable(role = Role.Button, onClickLabel = "Classificar por $titulo", onClick = aoTocar)
            .padding(vertical = 10.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), fontWeight = FontWeight.Bold, color = cor)
        if (ativa) {
            Spacer(Modifier.width(4.dp))
            Icon(
                if (crescente) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                contentDescription = if (crescente) "de A a Z" else "de Z a A",
                tint = cor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

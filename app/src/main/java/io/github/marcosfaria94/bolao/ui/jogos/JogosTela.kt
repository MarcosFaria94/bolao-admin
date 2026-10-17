package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.net.CaixaApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade

/** Como funciona a aposta simples de cada modalidade. */
fun regraDaAposta(m: Modalidade): String = when (m.formato) {
    Formato.NUMEROS -> buildString {
        append("${m.qtdAposta} números de ${m.rotulo(m.menor)} a ${m.rotulo(m.maior)}")
        if (m.trevos > 0) append(" + ${m.trevos} trevos de 1 a 6")
        when (m.extra) {
            io.github.marcosfaria94.bolao.domain.Extra.TIME_DO_CORACAO -> append(" + Time do Coração")
            io.github.marcosfaria94.bolao.domain.Extra.MES_DE_SORTE -> append(" + Mês de Sorte")
            else -> Unit
        }
        if (m.sorteios > 1) append(" · ${m.sorteios} sorteios de ${m.qtdSorteada}") else append(" · sorteia ${m.qtdSorteada}")
    }
    Formato.COLUNAS -> "1 número (0 a 9) em cada uma das 7 colunas"
    Formato.LOTECA -> "1 palpite (coluna 1, empate ou coluna 2) em cada um dos 14 jogos"
    Formato.FEDERAL -> "Bilhete de 5 algarismos, inteiro ou em frações · valor varia por extração"
}

@Composable
fun JogosTela(estado: Estado) {
    // Próximo concurso, data e prêmio de cada modalidade, consultados na Caixa em paralelo.
    val infos = remember { mutableStateMapOf<Modalidade, Concurso>() }
    var falhou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        coroutineScope {
            Modalidade.entries.forEach { m ->
                launch {
                    runCatching { CaixaApi.ultimo(m) }
                        .onSuccess { infos[m] = it }
                        .onFailure { falhou = true }
                }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Column {
                Text(
                    "Preço da aposta simples, próximo concurso e chance de acerto em cada faixa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (falhou && infos.isEmpty()) {
                    Text(
                        "Não foi possível consultar a Caixa agora.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        items(Modalidade.entries, key = { it.name }) { m -> CartaoModalidade(m, estado, infos[m]) }
    }
}

@Composable
private fun CartaoModalidade(m: Modalidade, estado: Estado, info: Concurso?) {
    var aberto by rememberSaveable(m.name) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.clickable { aberto = !aberto }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeloModalidade(m)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.nome, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (m.precoVariavel) "Preço variável" else Dinheiro.rs(estado.config.preco(m)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (aberto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (aberto) "Recolher" else "Ver chances",
                )
            }
            if (info != null) {
                Spacer(Modifier.height(8.dp))
                val proximo = info.proximoNumero?.let { "Concurso $it" } ?: "Próximo concurso"
                val data = info.proximaData?.let { " · ${Datas.diaSemana(it)}, ${Datas.diaMes(it)}" }.orEmpty()
                Text("$proximo$data", style = MaterialTheme.typography.bodyMedium)
                info.premioEstimadoProximo?.let {
                    Text(
                        "Prêmio estimado: ${Dinheiro.rs(it)}" + if (info.acumulado) " (acumulado)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (aberto) {
                Spacer(Modifier.height(12.dp))
                Text(regraDaAposta(m), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Chance de acerto (aposta simples)", style = MaterialTheme.typography.labelLarge)
                Probabilidades.faixas(m).forEach { f ->
                    LinhaValor(f.nome, Probabilidades.texto(f.umEm))
                }
                Probabilidades.faixas(m).mapNotNull { it.observacao }.distinct().forEach { obs ->
                    Text("* $obs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                info?.faixas?.filter { it.valor > 0 }?.takeIf { it.isNotEmpty() }?.let { faixas ->
                    Spacer(Modifier.height(8.dp))
                    Text("Prêmios pagos no concurso ${info.numero}", style = MaterialTheme.typography.labelLarge)
                    faixas.forEach { f -> LinhaValor(f.descricao.ifBlank { "${f.faixa}ª faixa" }, Dinheiro.rs(f.valor)) }
                }
            }
        }
    }
}

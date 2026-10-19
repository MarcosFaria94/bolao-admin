package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.tema.corPremiada
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Situação de uma aposta registrada, como na lista "Apostas do mês". */
enum class Situacao(val texto: String) {
    AGUARDANDO("Aguardando sorteio"),
    NAO_APURADA("Concurso não apurado"),
    NAO_PREMIADA("Aposta não premiada"),
    PREMIADA("Aposta premiada"),
}

fun situacaoDe(a: Aposta, hoje: LocalDate): Situacao {
    val ap = a.apuracao
    return when {
        ap?.confirmadaEm != null -> if (ap.premio > 0) Situacao.PREMIADA else Situacao.NAO_PREMIADA
        ap != null -> Situacao.NAO_APURADA
        a.dataSorteio != null && a.dataSorteio.isBefore(hoje) -> Situacao.NAO_APURADA
        else -> Situacao.AGUARDANDO
    }
}

@Composable
fun ApostasTela(estado: Estado, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }

    val destaque = indices.destaque(itens, Operacoes.saldo(estado)) { estado.config.preco(it) }
    val mes = YearMonth.from(hoje)
    // As do mês e as de antes que ainda esperam apuração.
    val lista = estado.apostas
        .filter { it.registrada && (YearMonth.from(it.dataAposta) == mes || it.apuracao?.confirmadaEm == null) }
        .sortedWith(compareByDescending<Aposta> { it.dataAposta }.thenByDescending { it.numero })
    val rascunhos = Operacoes.rascunhos(estado)

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            CartaoDestaque(destaque) {
                val m = destaque?.modalidade ?: return@CartaoDestaque
                escopo.launch {
                    try {
                        abrir("aposta/" + abrirOuCriarAposta(ctx, estado, m))
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível começar a aposta: ${t.message}")
                    }
                }
            }
        }
        item { CartaoProximaAposta(hoje, Operacoes.apostaFeitaEm(estado, hoje), aoJogar = { abrir("apostar") }) }
        if (rascunhos.isNotEmpty()) {
            items(rascunhos, key = { "r-" + it.id }) { r ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().clickable { abrir("aposta/${r.id}") },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SeloModalidade(r.modalidade, tamanho = 36)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Aposta em andamento · ${r.modalidade.nome}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (r.comprovante == null) "Falta anexar o comprovante da Caixa" else "Pronta para registrar",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text("Continuar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Apostas do mês", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    if (lista.isEmpty()) {
                        TextoVazio("Nenhuma aposta registrada neste mês.")
                    } else {
                        var dia: LocalDate? = null
                        lista.forEach { a ->
                            if (a.dataAposta != dia) {
                                dia = a.dataAposta
                                Text(
                                    Datas.curta(a.dataAposta),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                            ItemAposta(a, situacaoDe(a, hoje)) {
                                val s = situacaoDe(a, hoje)
                                abrir(if (s == Situacao.NAO_APURADA) "apuracao/${a.id}" else "aposta/${a.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoDestaque(item: Indices.Item?, aoTocar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().clickable(enabled = item != null, onClick = aoTocar),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Concurso em destaque", style = MaterialTheme.typography.labelLarge)
                if (item == null) {
                    Text("Consultando a Caixa...", style = MaterialTheme.typography.titleMedium)
                } else {
                    val u = item.ultimo
                    Text(
                        item.modalidade.nome + (item.proximoNumero?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        u?.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    u?.proximaData?.let { Text("Sorteio: ${Datas.longa(it)}", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (item != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Apostar", modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun CartaoProximaAposta(hoje: LocalDate, apostaDeHojeFeita: Boolean, aoJogar: () -> Unit) {
    val ehHoje = Calendario.ehDiaProgramado(hoje) && !apostaDeHojeFeita
    val dia = if (ehHoje) hoje else Calendario.diaProgramadoDepoisDe(hoje)
    val faltam = ChronoUnit.DAYS.between(hoje, dia)
    val quando = when {
        ehHoje -> "Hoje, ${Datas.longa(dia)}"
        faltam == 1L -> "Amanhã, ${Datas.longa(dia)}"
        else -> "${Datas.diaSemana(dia).replaceFirstChar { it.uppercase() }}, ${Datas.longa(dia)}"
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ehHoje) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Próxima aposta", style = MaterialTheme.typography.labelLarge)
                Text(quando, style = MaterialTheme.typography.titleMedium)
                if (apostaDeHojeFeita) Text("A aposta de hoje já foi feita.", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = aoJogar) { Text("Jogar") }
        }
    }
}

@Composable
private fun ItemAposta(a: Aposta, s: Situacao, aoTocar: () -> Unit) {
    val premio = a.premioEstimado?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it, comRs = false, maiuscula = false) }
    val cor = when (s) {
        Situacao.PREMIADA -> corPremiada()
        else -> Color.Unspecified
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(vertical = 4.dp),
    ) {
        SeloModalidade(a.modalidade, tamanho = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Nº${a.numero} ${a.modalidade.nome}" + (premio?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                s.texto + if (s == Situacao.PREMIADA) " · ${Dinheiro.rs(a.apuracao?.premio ?: 0)}" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = cor,
            )
        }
    }
}

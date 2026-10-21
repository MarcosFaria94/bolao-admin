package io.github.marcosfaria94.bolao.ui.aposta

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.CabecalhoDia
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.comum.TituloComMes
import io.github.marcosfaria94.bolao.ui.comum.mesesComDados
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corAguardando
import io.github.marcosfaria94.bolao.ui.tema.corApuracao
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import io.github.marcosfaria94.bolao.ui.tema.corPremiada
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Situação de uma aposta registrada, como na lista "Apostas do mês". */
enum class Situacao(val texto: String) {
    AGUARDANDO("Aguardando sorteio"),
    APURACAO("Aguardando apuração"),
    PREMIADA_A_APURAR("Aposta premiada · aguardando apuração"),
    NAO_PREMIADA("Aposta não premiada"),
    PREMIADA("Aposta premiada"),
}

fun situacaoDe(a: Aposta, hoje: LocalDate): Situacao {
    val ap = a.apuracao
    return when {
        ap?.confirmadaEm != null -> if (ap.premio > 0) Situacao.PREMIADA else Situacao.NAO_PREMIADA
        ap != null && (ap.premio > 0 || ap.faixasPremiadas.isNotEmpty()) -> Situacao.PREMIADA_A_APURAR
        ap != null -> Situacao.APURACAO
        a.dataSorteio != null && a.dataSorteio.isBefore(hoje) -> Situacao.APURACAO
        else -> Situacao.AGUARDANDO
    }
}

/**
 * Texto da situação: antes do sorteio, quando ele acontece ("Sorteio hoje", "Sorteio amanhã",
 * "Sorteio em 3 dias"); depois, o resultado. O valor do prêmio não aparece aqui (é surpresa
 * na tela da aposta).
 */
fun textoSituacao(a: Aposta, s: Situacao, hoje: LocalDate): String =
    if (s == Situacao.AGUARDANDO && a.dataSorteio != null) Datas.sorteioEm(a.dataSorteio, hoje) else s.texto

@Composable
fun corDaSituacao(s: Situacao): Color = when (s) {
    Situacao.AGUARDANDO -> corAguardando()
    Situacao.APURACAO -> corApuracao()
    Situacao.PREMIADA, Situacao.PREMIADA_A_APURAR -> corPremiada()
    Situacao.NAO_PREMIADA -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** "Nº8 | Mega-Sena · 3072". */
fun tituloDaAposta(a: Aposta, numero: Int = a.numero): String =
    "Nº$numero | ${a.modalidade.nome}" + if (a.concurso > 0) " · ${a.concurso}" else ""

@Composable
fun ApostasTela(estado: Estado, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }

    val destaque = indices.destaque(itens)
    val registradas = estado.apostas.filter { it.registrada }
    val meses = mesesComDados(registradas.map { it.dataAposta }, hoje)
    var mes by remember { mutableStateOf(YearMonth.from(hoje)) }
    val lista = registradas
        .filter { YearMonth.from(it.dataAposta) == mes }
        .sortedWith(compareByDescending<Aposta> { it.dataAposta }.thenByDescending { it.numero })

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            CartaoDestaque(destaque, hoje) {
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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TituloComMes("Apostas do mês", meses, mes) { mes = it }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        if (lista.isEmpty()) {
                            TextoVazio("Nenhuma aposta registrada em ${Datas.mesAnoExtenso(mes)}.", Modifier.padding(vertical = 10.dp))
                        } else {
                            var dia: LocalDate? = null
                            lista.forEach { a ->
                                if (a.dataAposta != dia) {
                                    // Divisória inteira entre um dia e outro.
                                    if (dia != null) HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    dia = a.dataAposta
                                    CabecalhoDia(a.dataAposta, hoje)
                                } else {
                                    HorizontalDivider(Modifier.padding(start = 66.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                ItemAposta(a, situacaoDe(a, hoje), hoje) { abrir("aposta/${a.id}") }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Concurso em destaque (o de maior k1): azul-noite com dourado. Selo com "Quina · 7147",
 * o prêmio grande, "Sorteio: Hoje" e o botão "Apostar agora".
 */
@Composable
private fun CartaoDestaque(item: Indices.Item?, hoje: LocalDate, aoApostar: () -> Unit) {
    val suave = Color(0xFFC9D3E6)
    Card(
        colors = CardDefaults.cardColors(containerColor = AzulMarinho, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Dourado, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "CONCURSO EM DESTAQUE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Dourado,
                )
            }
            Spacer(Modifier.height(14.dp))
            if (item == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Dourado)
                    Spacer(Modifier.width(10.dp))
                    Text("Consultando a Caixa...", style = MaterialTheme.typography.titleMedium)
                }
                return@Column
            }
            val u = item.ultimo
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeloModalidade(item.modalidade, tamanho = 48)
                Spacer(Modifier.width(14.dp))
                TextoQueCabe(
                    item.modalidade.nome + (item.proximoNumero?.let { " · $it" } ?: ""),
                    MaterialTheme.typography.headlineMedium,
                    cor = suave,
                    peso = FontWeight.SemiBold,
                    menor = 18.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            TextoQueCabe(
                u?.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 48.sp),
                peso = FontWeight.Bold,
                menor = 24.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            u?.proximaData?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Sorteio: ${Datas.relativa(it, hoje)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = suave,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = aoApostar,
                colors = ButtonDefaults.buttonColors(containerColor = Dourado, contentColor = Color(0xFF241A00)),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Apostar agora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartaoProximaAposta(hoje: LocalDate, apostaDeHojeFeita: Boolean, aoJogar: () -> Unit) {
    val ehHoje = Calendario.ehDiaProgramado(hoje) && !apostaDeHojeFeita
    val dia = if (ehHoje) hoje else Calendario.diaProgramadoDepoisDe(hoje)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Próxima aposta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(Datas.relativa(dia, hoje), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = aoJogar,
                contentPadding = PaddingValues(horizontal = 30.dp, vertical = 14.dp),
            ) { Text("Jogar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Tarja com o prêmio principal do concurso, no formato "R$ 52.000.000,00", na cor da modalidade. */
@Composable
private fun TarjaPremio(a: Aposta, centavos: Long) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(corModalidade(a.modalidade.cor), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Dinheiro.rs(centavos),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Uma aposta: o selo, a tarja com o prêmio, "Nº8 | Mega-Sena · 3072" e a situação. */
@Composable
private fun ItemAposta(a: Aposta, s: Situacao, hoje: LocalDate, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(vertical = 12.dp),
    ) {
        SeloModalidade(a.modalidade, tamanho = 52)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            a.premioEstimado?.takeIf { it > 0 }?.let { TarjaPremio(a, it) }
            Text(
                tituloDaAposta(a),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                textoSituacao(a, s, hoje),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = corDaSituacao(s),
            )
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

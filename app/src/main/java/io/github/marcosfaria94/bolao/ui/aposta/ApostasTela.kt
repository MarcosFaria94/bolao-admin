package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.comum.CabecalhoDia
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.ItemHistorico
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TextoBrilhante
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.TituloDestaque
import io.github.marcosfaria94.bolao.ui.comum.descricaoDaAposta
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.comum.TituloComMes
import io.github.marcosfaria94.bolao.ui.comum.mesesComDados
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corAguardando
import io.github.marcosfaria94.bolao.ui.tema.corApuracao
import io.github.marcosfaria94.bolao.ui.tema.corPremiada
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

/** "Aposta Nº8": o título da aposta (na tarja). O Nº recomeça a cada mês. */
fun tituloDaAposta(a: Aposta): String = "Aposta Nº${a.numero}"

@Composable
fun ApostasTela(estado: Estado, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
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
        // Tocar no concurso em destaque abre a página do jogo (lá fica o botão de apostar).
        item { CartaoDestaque(destaque, hoje) { m -> abrir("modalidade/${m.name}") } }
        item { CartaoProximaAposta(hoje, Operacoes.apostaFeitaEm(estado, hoje), aoApostar = { abrir("apostar") }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TituloComMes("Histórico", meses, mes) { mes = it }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        if (lista.isEmpty()) {
                            TextoVazio("Nenhuma aposta registrada em ${Datas.mesAnoCurto(mes)}.", Modifier.padding(vertical = 10.dp))
                        } else {
                            var dia: LocalDate? = null
                            lista.forEach { a ->
                                if (a.dataAposta != dia) {
                                    // Divisória inteira entre um dia e outro.
                                    if (dia != null) HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    dia = a.dataAposta
                                    CabecalhoDia(a.dataAposta, hoje)
                                } else {
                                    HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
 * Concurso em destaque (o de maior k1): azul-noite com dourado. O emblema da modalidade (sigla e
 * concurso), o nome, o prêmio grande e "Sorteio: Hoje" (com brilho, só quando é hoje mesmo).
 * O cartão inteiro abre a página do jogo.
 */
@Composable
private fun CartaoDestaque(item: Indices.Item?, hoje: LocalDate, aoTocar: (Modalidade) -> Unit) {
    val suave = Color(0xFFC9D3E6)
    val cores = CardDefaults.cardColors(
        containerColor = AzulMarinho,
        contentColor = Color.White,
        disabledContainerColor = AzulMarinho,
        disabledContentColor = Color.White,
    )
    Card(
        onClick = { item?.let { aoTocar(it.modalidade) } },
        enabled = item != null,
        colors = cores,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloDestaque(Modifier.weight(1f), cor = Dourado)
                if (item != null) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Abrir o jogo", tint = suave)
                }
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
                EmblemaModalidade(item.modalidade, item.proximoNumero, tamanho = 68.dp)
                Spacer(Modifier.width(16.dp))
                TextoQueCabe(
                    item.modalidade.nome,
                    MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
                    cor = Color.White,
                    peso = FontWeight.Bold,
                    menor = 20.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            TextoQueCabe(
                u?.premioEstimadoProximo?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 48.sp),
                peso = FontWeight.Bold,
                menor = 24.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            u?.proximaData?.let { data ->
                Spacer(Modifier.height(2.dp))
                val estilo = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                val texto = "Sorteio: ${Datas.relativa(data, hoje)}"
                // O brilho só quando o sorteio é hoje mesmo.
                if (data == hoje) TextoBrilhante(texto, estilo, cor = Color(0xFFFFE08A))
                else Text(texto, style = estilo, color = suave)
            }
        }
    }
}

@Composable
private fun CartaoProximaAposta(hoje: LocalDate, apostaDeHojeFeita: Boolean, aoApostar: () -> Unit) {
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
                onClick = aoApostar,
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp),
            ) { Text("Apostar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Uma aposta do Histórico: o emblema da modalidade (sigla e concurso), a tarja "Aposta Nº1",
 * "Quina · 55M" e a situação, na cor dela.
 */
@Composable
private fun ItemAposta(a: Aposta, s: Situacao, hoje: LocalDate, aoTocar: () -> Unit) {
    ItemHistorico(
        emblema = { EmblemaModalidade(a.modalidade, a.concurso, tamanho = 56.dp) },
        tarja = tituloDaAposta(a),
        descricao = descricaoDaAposta(a),
        linha = textoSituacao(a, s, hoje),
        corLinha = corDaSituacao(s),
        pesoLinha = FontWeight.SemiBold,
        aoTocar = aoTocar,
    )
}

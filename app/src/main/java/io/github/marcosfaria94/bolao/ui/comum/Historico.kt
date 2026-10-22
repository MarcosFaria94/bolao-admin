package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.tema.corAposta
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import io.github.marcosfaria94.bolao.ui.tema.corRateio
import java.time.LocalDate
import java.time.YearMonth

/**
 * Nome de um lançamento nas listas: "Aposta · Lotomania · 2850", "Prêmio · Lotofácil · 3809",
 * "Aporte mensal"... O Nº da aposta vai na etiqueta embaixo do ícone ([numeroLancamento]).
 */
fun rotuloLancamento(e: Estado, l: Lancamento): String {
    val a = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
    fun jogo(a: Aposta) = a.modalidade.nome + if (a.concurso > 0) " · ${a.concurso}" else ""
    return when (l.tipo) {
        TipoLancamento.SALDO_INICIAL -> "Saldo inicial"
        TipoLancamento.APORTE -> if (l.eventual) "Aporte adicional" else "Aporte mensal"
        TipoLancamento.APOSTA -> a?.let { "Aposta · ${jogo(it)}" } ?: "Aposta"
        TipoLancamento.PREMIO -> a?.let { "Prêmio · ${jogo(it)}" } ?: "Prêmio"
        TipoLancamento.RATEIO -> "Rateio entre os integrantes"
        TipoLancamento.RENDIMENTO -> "Rendimento da caixinha"
        TipoLancamento.COMPLEMENTO -> "Complemento do Administrador"
        TipoLancamento.DIVISAO_FINAL -> "Divisão final do saldo"
    }
}

/** Nº da aposta de uma aposta ou de um prêmio (null nos outros lançamentos). */
fun numeroLancamento(e: Estado, l: Lancamento): Int? =
    if (l.tipo == TipoLancamento.APOSTA || l.tipo == TipoLancamento.PREMIO) {
        l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }?.numero?.takeIf { it > 0 }
    } else {
        null
    }

/** Cor do valor: amarelo-ouro nas apostas, azul nos rateios e verde no dinheiro que entra. */
@Composable
fun corDoValor(t: TipoLancamento, valor: Long): Color = when (t) {
    TipoLancamento.APOSTA -> corAposta()
    TipoLancamento.RATEIO, TipoLancamento.DIVISAO_FINAL -> corRateio()
    else -> if (valor >= 0) corEntrada() else MaterialTheme.colorScheme.onSurface
}

fun iconeLancamento(t: TipoLancamento): ImageVector = when (t) {
    TipoLancamento.SALDO_INICIAL -> Icons.Outlined.AccountBalance
    TipoLancamento.APORTE -> Icons.Outlined.Savings
    TipoLancamento.APOSTA -> Icones.Trevo
    TipoLancamento.PREMIO -> Icons.Outlined.EmojiEvents
    TipoLancamento.RATEIO -> Icons.Outlined.Groups
    TipoLancamento.RENDIMENTO -> Icons.AutoMirrored.Outlined.TrendingUp
    TipoLancamento.COMPLEMENTO -> Icons.Outlined.VolunteerActivism
    TipoLancamento.DIVISAO_FINAL -> Icons.AutoMirrored.Outlined.CallSplit
}

/** "+ R$ 30,00" ou "- R$ 3,00". */
fun valorComSinal(centavos: Long): String = (if (centavos < 0) "- " else "+ ") + Dinheiro.rs(kotlin.math.abs(centavos))

/** Data que separa os grupos da lista: "Hoje", "Ontem" ou "13/10/2026". */
@Composable
fun CabecalhoDia(data: LocalDate, hoje: LocalDate = LocalDate.now(), modifier: Modifier = Modifier) {
    Text(
        Datas.relativa(data, hoje),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

/**
 * Uma movimentação, como nos apps de banco: ícone redondo (com a etiqueta "Nº7" embaixo, nas
 * apostas e nos prêmios), o nome em negrito e o valor logo embaixo, na cor do tipo.
 */
@Composable
fun ItemMovimento(
    icone: ImageVector,
    titulo: String,
    valor: Long,
    corValor: Color,
    numero: Int? = null,
    aoTocar: (() -> Unit)? = null,
) {
    val fundoIcone = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = aoTocar != null) { aoTocar?.invoke() }
            .padding(vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(42.dp).background(fundoIcone, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            if (numero != null) {
                Spacer(Modifier.height(4.dp))
                EtiquetaNumero("Nº$numero", fundo = fundoIcone, cor = MaterialTheme.colorScheme.onSurfaceVariant, largura = 42.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                valorComSinal(valor),
                style = MaterialTheme.typography.bodyLarge,
                color = corValor,
            )
        }
        if (aoTocar != null) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Lançamentos agrupados por dia (o mais recente primeiro), com uma linha entre um e outro. */
@Composable
fun ListaLancamentos(estado: Estado, lista: List<Lancamento>, aoTocar: ((Lancamento) -> Unit)?) {
    val hoje = LocalDate.now()
    var dia: LocalDate? = null
    lista.forEach { l ->
        if (l.data != dia) {
            // Divisória inteira entre um dia e outro; dentro do mesmo dia, uma mais curta.
            if (dia != null) HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            dia = l.data
            CabecalhoDia(l.data, hoje)
        } else {
            HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        ItemMovimento(
            icone = iconeLancamento(l.tipo),
            titulo = rotuloLancamento(estado, l),
            valor = l.valor,
            corValor = corDoValor(l.tipo, l.valor),
            numero = numeroLancamento(estado, l),
            aoTocar = aoTocar?.let { { it(l) } },
        )
    }
}

/** Título de uma lista com o parâmetro de data (mês e ano) ao lado. */
@Composable
fun TituloComMes(titulo: String, meses: List<YearMonth>, mes: YearMonth, aoEscolher: (YearMonth) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        SeletorMes(meses, mes, aoEscolher)
    }
}

/**
 * Histórico de um tipo de lançamento (aportes, rateios), do mês escolhido (começa no atual).
 * Tocar num item abre o comprovante.
 */
@Composable
fun Historico(
    titulo: String,
    estado: Estado,
    filtro: (Lancamento) -> Boolean,
    abrir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hoje = LocalDate.now()
    val todos = estado.lancamentos.filter(filtro)
    val meses = mesesComDados(todos.map { it.data }, hoje)
    var mes by remember { mutableStateOf(YearMonth.from(hoje)) }
    val lista = Operacoes.ordenados(todos.filter { YearMonth.from(it.data) == mes }).reversed()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloComMes(titulo, meses, mes) { mes = it }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                if (lista.isEmpty()) {
                    TextoVazio("Nada registrado em ${Datas.mesAnoExtenso(mes)}.", Modifier.padding(vertical = 10.dp))
                } else {
                    ListaLancamentos(estado, lista) { l ->
                        Operacoes.comprovanteDoLancamento(estado, l)?.let { abrir("comprovante/${it.id}") }
                    }
                    Spacer(Modifier.size(4.dp))
                }
            }
        }
    }
}

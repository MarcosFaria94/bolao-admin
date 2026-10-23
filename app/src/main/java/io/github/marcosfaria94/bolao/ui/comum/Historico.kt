package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import java.time.LocalDate
import java.time.YearMonth

/** "Quina · 55M": a modalidade e o prêmio principal do concurso (em milhões, quando a Caixa informou). */
fun descricaoDaAposta(a: Aposta): String =
    a.modalidade.nome + (a.premioEstimado?.takeIf { it > 0 }?.let { " · ${Dinheiro.milhoes(it)}" } ?: "")

/**
 * Título de um lançamento, na tarja do Histórico: "Aposta Nº1", "Prêmio Nº1", "Aporte Nº1",
 * "Rateio Nº1" (o Nº recomeça a cada mês, como no termo), "Saldo inicial"...
 */
fun tarjaLancamento(e: Estado, l: Lancamento): String {
    val n = Operacoes.numeroNoMes(e, l)
    fun comNumero(nome: String) = if (n != null) "$nome Nº$n" else nome
    return when (l.tipo) {
        TipoLancamento.SALDO_INICIAL -> "Saldo inicial"
        TipoLancamento.APORTE -> comNumero("Aporte")
        TipoLancamento.APOSTA -> comNumero("Aposta")
        TipoLancamento.PREMIO -> comNumero("Prêmio")
        TipoLancamento.RATEIO -> comNumero("Rateio")
        TipoLancamento.RENDIMENTO -> "Rendimento"
        TipoLancamento.COMPLEMENTO -> "Complemento"
        TipoLancamento.DIVISAO_FINAL -> "Divisão final"
    }
}

/** Descrição de um lançamento: "Mensal", "Quina · 55M", "3 pessoas"... (o saldo inicial não tem). */
fun descricaoLancamento(e: Estado, l: Lancamento): String? {
    val a = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
    val pessoas = e.pessoas.size.coerceAtLeast(1)
    return when (l.tipo) {
        TipoLancamento.SALDO_INICIAL -> null
        TipoLancamento.APORTE -> if (l.eventual) "Adicional" else "Mensal"
        TipoLancamento.APOSTA, TipoLancamento.PREMIO -> a?.let(::descricaoDaAposta)
        TipoLancamento.RATEIO, TipoLancamento.DIVISAO_FINAL -> if (pessoas == 1) "1 pessoa" else "$pessoas pessoas"
        TipoLancamento.RENDIMENTO -> "Caixinha Nubank"
        TipoLancamento.COMPLEMENTO -> "Administrador"
    }
}

/** "+ R$ 30,00", "- R$ 3,00"; o saldo inicial, que não é entrada nem saída do mês: "· R$ 12,50". */
fun valorDoLancamento(l: Lancamento): String =
    if (l.tipo == TipoLancamento.SALDO_INICIAL) "· " + Dinheiro.rs(l.valor) else valorComSinal(l.valor)

/** "+ R$ 30,00" ou "- R$ 3,00". */
fun valorComSinal(centavos: Long): String = (if (centavos < 0) "- " else "+ ") + Dinheiro.rs(kotlin.math.abs(centavos))

/** Data que separa os grupos da lista: "Hoje", "Ontem" ou "13/10/2026", em negrito, na cor do texto. */
@Composable
fun CabecalhoDia(data: LocalDate, hoje: LocalDate = LocalDate.now(), modifier: Modifier = Modifier) {
    Text(
        Datas.relativa(data, hoje),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 14.dp, bottom = 2.dp),
    )
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
            HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        ItemHistorico(
            emblema = { EmblemaLancamento(estado, l) },
            tarja = tarjaLancamento(estado, l),
            descricao = descricaoLancamento(estado, l),
            linha = valorDoLancamento(l),
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
                    TextoVazio("Nada registrado em ${Datas.mesAnoCurto(mes)}.", Modifier.padding(vertical = 10.dp))
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

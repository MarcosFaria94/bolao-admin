package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.aposta.premiada
import io.github.marcosfaria94.bolao.ui.aposta.situacaoCurta
import io.github.marcosfaria94.bolao.ui.aposta.situacaoDe
import io.github.marcosfaria94.bolao.ui.tema.Ouro
import io.github.marcosfaria94.bolao.ui.tema.Tarja
import io.github.marcosfaria94.bolao.ui.tema.corPremiada
import java.time.LocalDate
import java.time.YearMonth

/** O parâmetro de tipo do Histórico da Home (começa em "Tudo"). */
enum class FiltroHistorico(val rotulo: String) {
    TUDO("Tudo"),
    APORTES("Aportes"),
    APOSTAS("Apostas"),
    RATEIOS("Rateios"),
    PREMIOS("Prêmios"),
    EXTRATO("Extrato"),
}

/** Um item do Histórico: um lançamento ou o extrato de um mês. */
sealed interface Movimento {
    val data: LocalDate

    /** Ordem dentro do dia (o registro mais recente primeiro). */
    val ordem: Long

    /** Chave única do item na lista. */
    val chave: String

    data class DeLancamento(val l: Lancamento) : Movimento {
        override val data: LocalDate get() = l.data
        override val ordem: Long get() = l.criadoEm
        override val chave: String get() = l.id
    }

    /**
     * O extrato do mês [mes], com o rendimento lançado nele. Fica no dia do registro (se foi no
     * próprio mês) ou no último dia do mês, como o rendimento.
     */
    data class DeExtrato(
        val mes: YearMonth,
        val rendimento: Long,
        override val data: LocalDate,
        override val ordem: Long,
        val comprovante: Comprovante?,
    ) : Movimento {
        override val chave: String get() = "extrato-$mes"
    }
}

/** Em que tipo do parâmetro o item entra (null = só em "Tudo": saldo inicial, complemento, divisão final). */
fun filtroDe(mv: Movimento): FiltroHistorico? = when (mv) {
    is Movimento.DeExtrato -> FiltroHistorico.EXTRATO
    is Movimento.DeLancamento -> when (mv.l.tipo) {
        TipoLancamento.APORTE -> FiltroHistorico.APORTES
        TipoLancamento.APOSTA -> FiltroHistorico.APOSTAS
        TipoLancamento.RATEIO -> FiltroHistorico.RATEIOS
        TipoLancamento.PREMIO -> FiltroHistorico.PREMIOS
        TipoLancamento.RENDIMENTO -> FiltroHistorico.EXTRATO
        TipoLancamento.SALDO_INICIAL, TipoLancamento.COMPLEMENTO, TipoLancamento.DIVISAO_FINAL -> null
    }
}

/**
 * Itens do Histórico do [filtro] e do [mes] (null = todos os meses), o mais recente primeiro.
 * O rendimento entra no item do extrato do mês (sem extrato registrado, ele aparece sozinho).
 */
fun movimentosDoHistorico(e: Estado, filtro: FiltroHistorico, mes: YearMonth?): List<Movimento> {
    val extratos = e.extratos.mapNotNull { x ->
        val m = runCatching { YearMonth.parse(x.mes) }.getOrNull() ?: return@mapNotNull null
        val c = e.comprovantes.lastOrNull { it.tipo == TipoComprovante.EXTRATO && it.mes == x.mes }
        val dia = c?.data?.takeIf { YearMonth.from(it) == m } ?: m.atEndOfMonth()
        Movimento.DeExtrato(m, x.rendimento, dia, x.criadoEm, c)
    }
    val comExtrato = extratos.map { it.mes }.toSet()
    val lancamentos = e.lancamentos
        .filterNot { it.tipo == TipoLancamento.RENDIMENTO && YearMonth.from(it.data) in comExtrato }
        .map { Movimento.DeLancamento(it) }
    return (lancamentos + extratos)
        .filter { filtro == FiltroHistorico.TUDO || filtroDe(it) == filtro }
        .filter { mes == null || YearMonth.from(it.data) == mes }
        .sortedWith(compareByDescending<Movimento> { it.data }.thenByDescending { it.ordem })
}

/**
 * Tarja do item: "Aposta Nº8 · Sorteio hoje", "Aporte Nº2 · Mensal", "Rateio Nº1", "Prêmio Nº1",
 * "Extrato · Out/2026"... O Nº recomeça a cada mês, como nos comprovantes.
 */
fun tarjaDoMovimento(e: Estado, mv: Movimento, hoje: LocalDate): String {
    if (mv is Movimento.DeExtrato) return "Extrato · ${Datas.mesAnoCurto(mv.mes)}"
    val l = (mv as Movimento.DeLancamento).l
    val n = Operacoes.numeroNoMes(e, l)
    fun comNumero(nome: String) = if (n != null) "$nome Nº$n" else nome
    return when (l.tipo) {
        TipoLancamento.SALDO_INICIAL -> "Saldo inicial"
        TipoLancamento.APORTE -> comNumero("Aporte") + if (l.eventual) " · Adicional" else " · Mensal"
        TipoLancamento.APOSTA -> {
            val a = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
            comNumero("Aposta") + (a?.let { " · " + situacaoCurta(it, situacaoDe(it, hoje), hoje) } ?: "")
        }
        TipoLancamento.PREMIO -> comNumero("Prêmio")
        TipoLancamento.RATEIO -> comNumero("Rateio")
        TipoLancamento.RENDIMENTO -> "Rendimento"
        TipoLancamento.COMPLEMENTO -> "Complemento"
        TipoLancamento.DIVISAO_FINAL -> "Divisão final"
    }
}

/** "- R$ 3,50", "+ R$ 30,00"; o extrato, com o rendimento; o saldo inicial, sem sinal ("R$ 12,50"). */
fun valorDoMovimento(mv: Movimento): String = when (mv) {
    is Movimento.DeExtrato -> valorComSinal(mv.rendimento)
    is Movimento.DeLancamento ->
        if (mv.l.tipo == TipoLancamento.SALDO_INICIAL) Dinheiro.rs(mv.l.valor) else valorComSinal(mv.l.valor)
}

/** Aposta premiada (já apurada ou esperando a apuração) e o prêmio dela: o item fica todo dourado. */
fun douradoNoHistorico(e: Estado, mv: Movimento, hoje: LocalDate): Boolean {
    val l = (mv as? Movimento.DeLancamento)?.l ?: return false
    return when (l.tipo) {
        TipoLancamento.PREMIO -> true
        TipoLancamento.APOSTA -> l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }?.let { premiada(situacaoDe(it, hoje)) } == true
        else -> false
    }
}

/** Nome da modalidade, embaixo da bola (só nas apostas). */
fun nomeDoMovimento(e: Estado, mv: Movimento): String? {
    val l = (mv as? Movimento.DeLancamento)?.l?.takeIf { it.tipo == TipoLancamento.APOSTA } ?: return null
    return l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }?.modalidade?.nome
}

/** Tela que o item abre: a aposta (na página da modalidade), o extrato ou o comprovante. null = nenhuma. */
fun destinoDoMovimento(e: Estado, mv: Movimento): String? = when (mv) {
    is Movimento.DeExtrato -> mv.comprovante?.let { "comprovante/${it.id}" } ?: "extrato/${mv.mes}"
    is Movimento.DeLancamento -> when {
        (mv.l.tipo == TipoLancamento.APOSTA || mv.l.tipo == TipoLancamento.PREMIO) && mv.l.apostaId != null -> "aposta/${mv.l.apostaId}"
        else -> Operacoes.comprovanteDoLancamento(e, mv.l)?.let { "comprovante/${it.id}" }
    }
}

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

/**
 * Parâmetro de lista suspensa ("Tudo", "Out/2026"...): o botão com o escolhido e, ao tocar, a
 * lista das [opcoes].
 */
@Composable
fun <T> SeletorOpcoes(
    opcoes: List<T>,
    escolhido: T,
    rotulo: (T) -> String,
    aoEscolher: (T) -> Unit,
    descricao: String,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { aberto = true },
            contentPadding = PaddingValues(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text(rotulo(escolhido), style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = descricao)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { o ->
                DropdownMenuItem(
                    text = { Text(rotulo(o), fontWeight = if (o == escolhido) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        aoEscolher(o)
                        aberto = false
                    },
                )
            }
        }
    }
}

/** Raio dos cantos do cartão do Histórico. */
private val CANTO = 12.dp

/** Um pedaço do cartão do Histórico (cada item da lista é um pedaço; o primeiro e o último arredondam os cantos). */
@Composable
private fun Modifier.trechoDoCartao(primeiro: Boolean, ultimo: Boolean): Modifier {
    val forma = RoundedCornerShape(
        topStart = if (primeiro) CANTO else 0.dp,
        topEnd = if (primeiro) CANTO else 0.dp,
        bottomStart = if (ultimo) CANTO else 0.dp,
        bottomEnd = if (ultimo) CANTO else 0.dp,
    )
    return fillMaxWidth().clip(forma).background(MaterialTheme.colorScheme.surfaceContainerLow)
}

/**
 * O Histórico num cartão, separado por dia (o mais recente primeiro), com uma linha só entre um
 * dia e outro. Cada dia e cada item são itens da lista (ela só desenha o que aparece na tela).
 */
fun LazyListScope.itensDoHistorico(
    estado: Estado,
    movimentos: List<Movimento>,
    hoje: LocalDate,
    vazio: String,
    abrir: (String) -> Unit,
) {
    if (movimentos.isEmpty()) {
        item(key = "historico-vazio") {
            Box(Modifier.trechoDoCartao(primeiro = true, ultimo = true).padding(horizontal = 16.dp, vertical = 12.dp)) {
                TextoVazio(vazio)
            }
        }
        return
    }
    val dias = movimentos.groupBy { it.data }.entries.toList()
    dias.forEachIndexed { i, (dia, lista) ->
        item(key = "dia-$dia") {
            Column(Modifier.trechoDoCartao(primeiro = i == 0, ultimo = false).padding(horizontal = 12.dp)) {
                if (i > 0) HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                else Spacer(Modifier.height(2.dp))
                CabecalhoDia(dia, hoje)
            }
        }
        lista.forEachIndexed { j, mv ->
            val ultimo = i == dias.lastIndex && j == lista.lastIndex
            item(key = mv.chave) {
                Box(
                    Modifier
                        .trechoDoCartao(primeiro = false, ultimo = ultimo)
                        .padding(start = 12.dp, end = 12.dp, bottom = if (ultimo) 8.dp else 0.dp),
                ) {
                    val destino = destinoDoMovimento(estado, mv)
                    ItemDoHistorico(
                        emblema = { EmblemaMovimento(estado, mv) },
                        nome = nomeDoMovimento(estado, mv),
                        tarja = tarjaDoMovimento(estado, mv, hoje),
                        valor = valorDoMovimento(mv),
                        dourado = douradoNoHistorico(estado, mv, hoje),
                        aoTocar = destino?.let { { abrir(it) } },
                    )
                }
            }
        }
    }
}

/** Largura da coluna do emblema (com o nome da modalidade embaixo da bola): igual em todos os itens. */
private val LARGURA_EMBLEMA = 72.dp

/**
 * Um item do Histórico: o emblema (nas apostas, a bola com o nome da modalidade embaixo) e, ao
 * lado, a tarja que vai até o fim ("Aposta Nº8 · Sorteio hoje") e o valor bem grande. [dourado]
 * (aposta premiada e prêmio): tudo na cor de ouro.
 */
@Composable
fun ItemDoHistorico(
    emblema: @Composable () -> Unit,
    nome: String?,
    tarja: String,
    valor: String,
    dourado: Boolean,
    aoTocar: (() -> Unit)?,
) {
    val ouro = corPremiada()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = aoTocar != null) { aoTocar?.invoke() }
            .padding(vertical = 10.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.width(LARGURA_EMBLEMA),
        ) {
            emblema()
            if (nome != null) {
                Text(
                    nome,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, lineHeight = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = if (dourado) ouro else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TarjaLarga(tarja, dourado)
            TextoQueCabe(
                valor,
                MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp),
                cor = if (dourado) ouro else MaterialTheme.colorScheme.onSurface,
                peso = FontWeight.Bold,
                menor = 18.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Tarja que vai até o fim da linha, em retângulo ("Aposta Nº8 · Sorteio hoje"); [dourado]: na cor de ouro. */
@Composable
fun TarjaLarga(texto: String, dourado: Boolean = false, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(8.dp)
    val fundo = if (dourado) Modifier.background(Brush.horizontalGradient(Ouro.degrade()), forma) else Modifier.background(Tarja.fundo(), forma)
    Box(
        modifier
            .fillMaxWidth()
            .then(fundo)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        TextoQueCabe(
            texto,
            MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 21.sp),
            cor = if (dourado) Ouro.textoTarja else Tarja.texto(),
            peso = FontWeight.SemiBold,
            menor = 11.sp,
        )
    }
}

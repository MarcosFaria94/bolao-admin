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
import androidx.compose.ui.text.style.TextOverflow
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
import io.github.marcosfaria94.bolao.ui.aposta.Situacao
import io.github.marcosfaria94.bolao.ui.aposta.premiada
import io.github.marcosfaria94.bolao.ui.aposta.situacaoCurta
import io.github.marcosfaria94.bolao.ui.aposta.situacaoDe
import io.github.marcosfaria94.bolao.ui.tema.Ouro
import io.github.marcosfaria94.bolao.ui.tema.Tarja
import io.github.marcosfaria94.bolao.ui.tema.corAguardando
import io.github.marcosfaria94.bolao.ui.tema.corApuracao
import io.github.marcosfaria94.bolao.ui.tema.corConferir
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import io.github.marcosfaria94.bolao.ui.tema.corPremiada
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
 * Título do item: "Aposta Nº8 · Mega-Sena", "Aporte Nº2", "Rateio Nº1", "Prêmio Nº1 · Lotofácil",
 * "Extrato · Out/2026"... O Nº recomeça a cada mês, como nos comprovantes.
 */
fun tituloDoMovimento(e: Estado, mv: Movimento): String {
    if (mv is Movimento.DeExtrato) return "Extrato · ${Datas.mesAnoCurto(mv.mes)}"
    val l = (mv as Movimento.DeLancamento).l
    val n = Operacoes.numeroNoMes(e, l)
    fun comNumero(nome: String) = if (n != null) "$nome Nº$n" else nome
    val a = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
    return when (l.tipo) {
        TipoLancamento.SALDO_INICIAL -> "Saldo inicial"
        TipoLancamento.APORTE -> comNumero("Aporte")
        TipoLancamento.APOSTA -> comNumero("Aposta") + (a?.let { " · ${it.modalidade.nome}" } ?: "")
        TipoLancamento.PREMIO -> comNumero("Prêmio") + (a?.let { " · ${it.modalidade.nome}" } ?: "")
        TipoLancamento.RATEIO -> comNumero("Rateio")
        TipoLancamento.RENDIMENTO -> "Rendimento"
        TipoLancamento.COMPLEMENTO -> "Complemento"
        TipoLancamento.DIVISAO_FINAL -> "Divisão final"
    }
}

/** Cor da etiqueta da situação. */
enum class TomEtiqueta { NEUTRO, AZUL, LARANJA, OURO, VERMELHO, ROXO }

/** A etiqueta da situação de um item ("Sorteio hoje", "Não premiada", "Mensal", "Sem comprovante"...). */
data class Etiqueta(val texto: String, val tom: TomEtiqueta)

/**
 * Situação do item, na etiqueta colorida: antes do sorteio, quando ele acontece ("Sorteio hoje");
 * depois, "Premiada" ou "Não premiada" ("Em apuração" enquanto o resultado não chega). A aposta e o
 * rateio que esperam o comprovante (ou a conferência dele) dizem isso. O aporte diz se é mensal ou
 * adicional. null = sem etiqueta.
 */
fun etiquetaDoMovimento(e: Estado, mv: Movimento, hoje: LocalDate): Etiqueta? {
    val l = (mv as? Movimento.DeLancamento)?.l ?: return null
    return when (l.tipo) {
        TipoLancamento.APORTE -> Etiqueta(if (l.eventual) "Adicional" else "Mensal", TomEtiqueta.NEUTRO)
        TipoLancamento.APOSTA -> {
            val a = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } } ?: return null
            when {
                Operacoes.aguardandoComprovante(a) -> Etiqueta("Sem comprovante", TomEtiqueta.VERMELHO)
                Operacoes.aguardandoConferencia(a) -> Etiqueta("A conferir", TomEtiqueta.ROXO)
                else -> {
                    val s = situacaoDe(a, hoje)
                    val tom = when (s) {
                        Situacao.AGUARDANDO -> TomEtiqueta.AZUL
                        Situacao.APURACAO -> TomEtiqueta.LARANJA
                        Situacao.PREMIADA, Situacao.PREMIADA_A_APURAR -> TomEtiqueta.OURO
                        Situacao.NAO_PREMIADA -> TomEtiqueta.NEUTRO
                    }
                    Etiqueta(situacaoCurta(a, s, hoje), tom)
                }
            }
        }
        TipoLancamento.RATEIO -> e.rateios.firstOrNull { it.numero == l.rateioNumero }?.takeIf { it.pendenteDeAnexo }?.let { r ->
            if (Operacoes.pixCompletos(e, r)) Etiqueta("A conferir", TomEtiqueta.ROXO) else Etiqueta("Esperando os Pix", TomEtiqueta.VERMELHO)
        }
        TipoLancamento.PREMIO -> Etiqueta("Premiada", TomEtiqueta.OURO)
        else -> null
    }
}

/** Hora do registro ("21:14"): quando o item foi registrado no app. */
fun horaDoMovimento(mv: Movimento, fuso: ZoneId = ZoneId.systemDefault()): String {
    val ms = when (mv) {
        is Movimento.DeLancamento -> mv.l.criadoEm
        is Movimento.DeExtrato -> mv.ordem
    }
    return Instant.ofEpochMilli(ms).atZone(fuso).toLocalTime().format(FORMATO_HORA)
}

private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Valor do item em centavos (o extrato, com o rendimento do mês). */
fun centavosDoMovimento(mv: Movimento): Long = when (mv) {
    is Movimento.DeExtrato -> mv.rendimento
    is Movimento.DeLancamento -> mv.l.valor
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
fun CabecalhoDia(data: LocalDate, hoje: LocalDate = LocalDate.now(), modifier: Modifier = Modifier, total: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)) {
        Text(
            Datas.relativa(data, hoje),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (total != null) {
            Text(
                total,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
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

/** Raio dos cantos do cartão de cada dia do Histórico. */
private val CANTO = 14.dp

/** Um pedaço do cartão do dia (cada item da lista é um pedaço; o primeiro e o último arredondam os cantos). */
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

/** Tamanho do emblema dos itens do Histórico. */
private val TAMANHO_EMBLEMA = 44.dp

/** Onde começa o texto dos itens (a linha entre dois itens começa aqui). */
private val RECUO_TEXTO = 12.dp + TAMANHO_EMBLEMA + 12.dp

/**
 * O Histórico separado por dia (o mais recente primeiro): cada dia num cartão, com a data e o
 * total do dia em cima e uma linha fina entre um item e outro. Cada dia e cada item são itens da
 * lista (ela só desenha o que aparece na tela).
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
            Box(Modifier.trechoDoCartao(primeiro = true, ultimo = true).padding(horizontal = 16.dp, vertical = 14.dp)) {
                TextoVazio(vazio)
            }
        }
        return
    }
    val dias = movimentos.groupBy { it.data }.entries.toList()
    dias.forEach { (dia, lista) ->
        item(key = "dia-$dia") {
            CabecalhoDia(dia, hoje, total = valorComSinal(lista.sumOf { centavosDoMovimento(it) }))
        }
        lista.forEachIndexed { j, mv ->
            item(key = mv.chave) {
                Column(Modifier.trechoDoCartao(primeiro = j == 0, ultimo = j == lista.lastIndex)) {
                    if (j > 0) HorizontalDivider(Modifier.padding(start = RECUO_TEXTO), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    val destino = destinoDoMovimento(estado, mv)
                    ItemDoHistorico(
                        emblema = { EmblemaMovimento(estado, mv, TAMANHO_EMBLEMA) },
                        titulo = tituloDoMovimento(estado, mv),
                        etiqueta = etiquetaDoMovimento(estado, mv, hoje),
                        hora = horaDoMovimento(mv),
                        valor = valorDoMovimento(mv),
                        positivo = centavosDoMovimento(mv) > 0,
                        dourado = douradoNoHistorico(estado, mv, hoje),
                        aoTocar = destino?.let { { abrir(it) } },
                    )
                }
            }
        }
    }
}

/**
 * Um item do Histórico: o emblema (nas apostas, a bola da modalidade com o concurso); ao lado, o
 * título ("Aposta Nº8 · Mega-Sena") e, embaixo, a etiqueta da situação e a hora do registro; no
 * fim, o valor (as entradas em verde). [dourado] (aposta premiada e prêmio): o valor em ouro.
 */
@Composable
fun ItemDoHistorico(
    emblema: @Composable () -> Unit,
    titulo: String,
    etiqueta: Etiqueta?,
    hora: String?,
    valor: String,
    positivo: Boolean,
    dourado: Boolean,
    aoTocar: (() -> Unit)?,
) {
    val corValor = when {
        dourado -> corPremiada()
        positivo -> corEntrada()
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = aoTocar != null) { aoTocar?.invoke() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        emblema()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // A hora vem primeiro e nunca é cortada; a etiqueta usa o espaço que sobrar.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hora != null) {
                    Text(
                        hora,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                if (etiqueta != null) EtiquetaSituacao(etiqueta, Modifier.weight(1f, fill = false))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 21.sp),
            fontWeight = FontWeight.Bold,
            color = corValor,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Etiqueta pequena e colorida com a situação ("Sorteio hoje", "Premiada" em ouro...). */
@Composable
fun EtiquetaSituacao(e: Etiqueta, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(6.dp)
    val cor = when (e.tom) {
        TomEtiqueta.NEUTRO -> Tarja.texto()
        TomEtiqueta.AZUL -> corAguardando()
        TomEtiqueta.LARANJA -> corApuracao()
        TomEtiqueta.OURO -> Ouro.textoTarja
        TomEtiqueta.VERMELHO -> MaterialTheme.colorScheme.error
        TomEtiqueta.ROXO -> corConferir()
    }
    val fundo = when (e.tom) {
        TomEtiqueta.OURO -> Modifier.background(Brush.horizontalGradient(Ouro.degrade()), forma)
        TomEtiqueta.NEUTRO -> Modifier.background(Tarja.fundo(), forma)
        else -> Modifier.background(cor.copy(alpha = 0.14f), forma)
    }
    Text(
        e.texto,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
        fontWeight = FontWeight.SemiBold,
        color = cor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.then(fundo).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

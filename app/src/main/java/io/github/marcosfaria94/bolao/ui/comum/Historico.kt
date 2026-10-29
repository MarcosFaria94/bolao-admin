package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.Saver
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

/** Os tipos do Histórico da Home ("Tudo" = todos). */
enum class FiltroHistorico(val rotulo: String) {
    TUDO("Tudo"),
    APORTES("Aportes"),
    APOSTAS("Apostas"),
    RATEIOS("Rateios"),
    PREMIOS("Prêmios"),
    EXTRATO("Extrato"),
}

/** Os tipos que o filtro oferece: no modo individual, só apostas e prêmios. */
fun tiposDoFiltro(individual: Boolean): List<FiltroHistorico> =
    if (individual) listOf(FiltroHistorico.APOSTAS, FiltroHistorico.PREMIOS) else FiltroHistorico.entries - FiltroHistorico.TUDO

/**
 * O filtro do Histórico da Home (tela "Filtrar"): os tipos e os meses escolhidos, vários de cada
 * vez; vazio = todos. O padrão é todos os tipos no mês atual ("Limpar filtro" volta a ele, e ele
 * volta sozinho ao trocar de tela).
 */
data class FiltroHome(val tipos: Set<FiltroHistorico> = emptySet(), val meses: Set<YearMonth> = emptySet()) {
    fun ehPadrao(hoje: LocalDate): Boolean = this == padrao(hoje)

    /** Marca ou desmarca o tipo [t] (todos marcados = "Tudo"). */
    fun comTipo(t: FiltroHistorico, todos: List<FiltroHistorico>): FiltroHome {
        val novos = if (t in tipos) tipos - t else tipos + t
        return copy(tipos = if (novos.containsAll(todos)) emptySet() else novos)
    }

    /** Marca ou desmarca o mês [m]. */
    fun comMes(m: YearMonth): FiltroHome = copy(meses = if (m in meses) meses - m else meses + m)

    /** "Out/2026 · Tudo", "Set/2026, Out/2026 · Apostas, Prêmios", "Todos os meses · Tudo". */
    fun resumo(): String {
        val m = if (meses.isEmpty()) "Todos os meses" else meses.sorted().joinToString(", ") { Datas.mesAnoCurto(it) }
        val t = if (tipos.isEmpty()) "Tudo" else tipos.sortedBy { it.ordinal }.joinToString(", ") { it.rotulo }
        return "$m · $t"
    }

    companion object {
        fun padrao(hoje: LocalDate) = FiltroHome(meses = setOf(YearMonth.from(hoje)))

        /** Para guardar o filtro na tela (girar o celular, ir e voltar do "Filtrar"). */
        val Saver: Saver<FiltroHome, Any> = listSaver(
            save = { f -> f.tipos.map { "t:${it.name}" } + f.meses.map { "m:$it" } },
            restore = { lista ->
                FiltroHome(
                    tipos = lista.filter { it.startsWith("t:") }.mapNotNull { runCatching { FiltroHistorico.valueOf(it.drop(2)) }.getOrNull() }.toSet(),
                    meses = lista.filter { it.startsWith("m:") }.mapNotNull { runCatching { YearMonth.parse(it.drop(2)) }.getOrNull() }.toSet(),
                )
            },
        )
    }
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
fun movimentosDoHistorico(e: Estado, filtro: FiltroHistorico, mes: YearMonth?): List<Movimento> =
    movimentosDoHistorico(
        e,
        FiltroHome(tipos = if (filtro == FiltroHistorico.TUDO) emptySet() else setOf(filtro), meses = setOfNotNull(mes)),
    )

/**
 * Itens do Histórico com o [filtro] (os tipos e os meses escolhidos), o mais recente primeiro.
 * O rendimento entra no item do extrato do mês (sem extrato registrado, ele aparece sozinho).
 */
fun movimentosDoHistorico(e: Estado, filtro: FiltroHome): List<Movimento> {
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
        .filter { filtro.tipos.isEmpty() || filtroDe(it) in filtro.tipos }
        .filter { filtro.meses.isEmpty() || YearMonth.from(it.data) in filtro.meses }
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
                        // O dourado fica só no item do prêmio; a aposta premiada diz "Premiada" sem ele.
                        Situacao.PREMIADA, Situacao.PREMIADA_A_APURAR, Situacao.NAO_PREMIADA -> TomEtiqueta.NEUTRO
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

/** Só o prêmio fica dourado (o valor e a etiqueta); a aposta premiada, não. */
@Suppress("UNUSED_PARAMETER")
fun douradoNoHistorico(e: Estado, mv: Movimento, hoje: LocalDate): Boolean =
    (mv as? Movimento.DeLancamento)?.l?.tipo == TipoLancamento.PREMIO

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

/**
 * Data que separa os dias da lista: "Hoje", "Ontem" ou "13/10/2026" em negrito numa etiqueta, com
 * uma linha até o fim (sem o total do dia).
 */
@Composable
fun CabecalhoDia(data: LocalDate, hoje: LocalDate = LocalDate.now(), modifier: Modifier = Modifier, primeiro: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(top = if (primeiro) 10.dp else 22.dp, bottom = 10.dp),
    ) {
        Text(
            Datas.relativa(data, hoje),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 18.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    }
}

/** Cartão de cada item do Histórico. */
@Composable
private fun Modifier.cartaoDoItem(): Modifier {
    val forma = RoundedCornerShape(14.dp)
    return fillMaxWidth()
        .clip(forma)
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
}

/** Tamanho do emblema dos itens do Histórico. */
private val TAMANHO_EMBLEMA = 44.dp

/** Espaço entre um item e outro do mesmo dia. */
private val ESPACO_ITENS = 8.dp

/**
 * O Histórico separado por dia (o mais recente primeiro): a data numa etiqueta com uma linha até o
 * fim e, embaixo, cada item no seu cartão, com espaço entre eles. Cada dia e cada item são itens da
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
            Box(Modifier.padding(top = 10.dp).cartaoDoItem().padding(horizontal = 16.dp, vertical = 14.dp)) {
                TextoVazio(vazio)
            }
        }
        return
    }
    val dias = movimentos.groupBy { it.data }.entries.toList()
    dias.forEachIndexed { i, (dia, lista) ->
        item(key = "dia-$dia") { CabecalhoDia(dia, hoje, primeiro = i == 0) }
        lista.forEachIndexed { j, mv ->
            item(key = mv.chave) {
                val destino = destinoDoMovimento(estado, mv)
                Box(Modifier.padding(bottom = if (j < lista.lastIndex) ESPACO_ITENS else 0.dp).cartaoDoItem()) {
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
 * fim, o valor (as entradas em verde). [dourado] (o prêmio): o valor em ouro.
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

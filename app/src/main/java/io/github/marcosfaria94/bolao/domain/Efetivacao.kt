package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.data.Estado
import java.time.LocalDate

/**
 * O "Efetivar apostas" da tela da Caixa (desde a 0.42, no lugar do "Transcrever em lote"; pedido do usuário: "mostra a
 * lista de apostas não efetivadas pré-selecionadas (Tudo); se a pessoa quiser desmarcar algo ou remarcar um jogo já
 * efetivado, ela tem a possibilidade; essa área separa por modalidade, concurso e mostra o status de cada jogo; faz a
 * verificação se a sessão não expirou antes, para os jogos não irem para o limbo").
 *
 * Cada linha é um item da [Transcricao] (uma aposta, uma teimosinha inteira ou a aposta com a espelho), com a página do
 * site em que ela vai. A [Situacao] de cada uma: não efetivada, na fila, indo, no carrinho ou o porquê de não ter ido.
 */
object Efetivacao {

    /** Como está cada aposta da lista. */
    sealed interface Situacao {
        /** Ainda não foi para o carrinho do site (nesta abertura do app). */
        data object NaoEfetivada : Situacao
        /** Escolhida, esperando a vez no lote. */
        data object NaFila : Situacao
        /** O app está marcando o volante dela agora. */
        data object Indo : Situacao
        /** Está no carrinho do site (o contador do site subiu). */
        data object NoCarrinho : Situacao
        /** Não foi: o porquê ([sessao] = a sessão do site expirou; [parcial] = o volante ficou marcado para conferir). */
        data class Falhou(val mensagem: String, val sessao: Boolean = false, val parcial: Boolean = false) : Situacao
        /** O lote parou antes dela (a sessão expirou numa aposta de antes). */
        data object NaoEnviada : Situacao
    }

    /** O nome curto da situação (a etiqueta na lista). */
    fun etiqueta(s: Situacao): String = when (s) {
        Situacao.NaoEfetivada -> "Não efetivada"
        Situacao.NaFila -> "Na fila"
        Situacao.Indo -> "Indo para o carrinho…"
        Situacao.NoCarrinho -> "No carrinho"
        is Situacao.Falhou -> if (s.sessao) "Sessão expirada" else if (s.parcial) "Conferir no site" else "Não foi"
        Situacao.NaoEnviada -> "Não enviada"
    }

    /** Uma aposta da lista: o [item] e a [pagina] do site em que ele vai; [chave] identifica a linha. */
    data class Linha(val pagina: Transcricao.Pagina, val item: Transcricao.Item) {
        val chave: String get() = item.ids.joinToString(",").ifEmpty { item.aposta.id }
        val modalidade: Modalidade get() = pagina.modalidade
        val concurso: Int get() = item.aposta.concurso
        val sorteio: LocalDate? get() = item.aposta.dataSorteio
        val valor: Long get() = item.plano.valor
    }

    /** Todas as apostas que esperam o site (as que já foram para o carrinho também: dá para mandar de novo). */
    fun lista(e: Estado, hoje: LocalDate): List<Linha> =
        Transcricao.emLote(e, hoje, emptyList()).flatMap { (p, itens) -> itens.map { Linha(p, it) } }

    /** A situação de uma linha: a do lote de agora ([situacoes]) ou, sem ela, no carrinho (pelas [transcritas]) ou não efetivada. */
    fun situacao(l: Linha, transcritas: Collection<String>, situacoes: Map<String, Situacao>): Situacao =
        situacoes[l.chave] ?: if (l.item.ids.isNotEmpty() && l.item.ids.all { it in transcritas }) Situacao.NoCarrinho else Situacao.NaoEfetivada

    /** As que já vêm marcadas: todas as que não estão no carrinho (o "Tudo"). */
    fun preSelecionadas(lista: List<Linha>, transcritas: Collection<String>, situacoes: Map<String, Situacao>): Set<String> =
        lista.filter { situacao(it, transcritas, situacoes) != Situacao.NoCarrinho }.map { it.chave }.toSet()

    /** Um grupo da lista: a modalidade, o concurso, a data do sorteio e as linhas dele. */
    data class Grupo(val modalidade: Modalidade, val concurso: Int, val especial: Boolean, val sorteio: LocalDate?, val linhas: List<Linha>)

    /** A lista separada por modalidade (na ordem do app) e, dentro dela, por concurso (o sorteio mais próximo primeiro). */
    fun grupos(lista: List<Linha>): List<Grupo> =
        lista.groupBy { it.modalidade }.toList().sortedBy { it.first.ordinal }.flatMap { (m, linhas) ->
            linhas.groupBy { it.concurso to it.pagina.especial }.toList()
                .sortedWith(compareBy({ it.second.first().sorteio ?: LocalDate.MAX }, { it.first.first }))
                .map { (k, ls) -> Grupo(m, k.first, k.second, ls.first().sorteio, ls) }
        }

    /** O lote das [escolhidas], página por página, na ordem da lista (o jeito que o navegador da Caixa transcreve). */
    fun lote(lista: List<Linha>, escolhidas: Set<String>): List<Pair<Transcricao.Pagina, List<Transcricao.Item>>> =
        lista.filter { it.chave in escolhidas }.groupBy { it.pagina }.map { (p, ls) -> p to ls.map { it.item } }

    /** "3 de 5 marcadas · R$ 27,00". */
    fun resumo(lista: List<Linha>, escolhidas: Set<String>): String {
        val marcadas = lista.filter { it.chave in escolhidas }
        return "${marcadas.size} de ${lista.size} marcada${if (lista.size == 1) "" else "s"} · ${Dinheiro.rs(marcadas.sumOf { it.valor })}"
    }

    /** O texto do botão: "Efetivar 3 apostas", "Efetivar 1 aposta". */
    fun textoDoBotao(n: Int): String = if (n == 1) "Efetivar 1 aposta" else "Efetivar $n apostas"

    /** Quantas apostas esperam (o número no botão flutuante): as que não estão no carrinho. */
    fun pendentes(lista: List<Linha>, transcritas: Collection<String>): Int =
        lista.count { l -> l.item.ids.isEmpty() || !l.item.ids.all { it in transcritas } }

    /** A sessão do site, lida pela página (assets/caixa/sessao.js). */
    enum class Sessao { VERIFICANDO, LOGADA, DESLOGADA, DESCONHECIDA }

    fun sessao(texto: String?): Sessao = when (texto) {
        "logado" -> Sessao.LOGADA
        "deslogado" -> Sessao.DESLOGADA
        else -> Sessao.DESCONHECIDA
    }

    /** O que a linha da sessão diz no alto da área. */
    fun textoDaSessao(s: Sessao): String = when (s) {
        Sessao.VERIFICANDO -> "Verificando a sessão no site…"
        Sessao.LOGADA -> "Sessão aberta no site"
        Sessao.DESLOGADA -> "Sessão expirada: entre na sua conta no site antes de efetivar"
        Sessao.DESCONHECIDA -> "Não deu para ver a sessão: cada aposta só conta quando o carrinho do site muda"
    }

    /** "Sáb 22/11": o dia do sorteio do grupo. */
    fun dia(d: LocalDate?): String? = d?.let {
        val dias = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
        "%s %02d/%02d".format(dias[it.dayOfWeek.value - 1], it.dayOfMonth, it.monthValue)
    }
}

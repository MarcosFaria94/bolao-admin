package io.github.marcosfaria94.bolao.domain

/**
 * Concursos com o preço da aposta diferente do de sempre (desde a 0.44). A Caixa às vezes baixa o preço de um concurso
 * só: a Quina Friday (concurso 7173, sorteio em 29/11/2026) vendeu a aposta simples por R$ 2,50 em vez de R$ 3,00, e as
 * apostas com mais números na mesma proporção (6 números R$ 15,00, 7 números R$ 52,50...). Com o preço de sempre no
 * app, o "Efetivar apostas" parava a aposta antes do carrinho (o site mostrava R$ 2,50 e o app R$ 3,00) e o saldo do bolão
 * ficava com R$ 0,50 a menos por aposta.
 *
 * A API da Caixa não informa o preço de um concurso, então a lista é escrita aqui (como a dos concursos especiais, em
 * [Especiais]). O preço vale para a aposta simples; as apostas com mais números são múltiplos dele.
 */
object PrecosDosConcursos {

    /** O preço [simples] (centavos) da aposta simples no [concurso] da [modalidade]. */
    data class PrecoDoConcurso(val modalidade: Modalidade, val concurso: Int, val simples: Long, val nome: String)

    val LISTA: List<PrecoDoConcurso> = listOf(
        // CAIXA Notícias, 19/11/2026: "No concurso 7173, a aposta simples, de cinco números, custará R$ 2,50".
        PrecoDoConcurso(Modalidade.QUINA, 7173, 250, "Quina Friday"),
    )

    /** O preço da aposta simples no concurso [concurso] da [m]; null = o de sempre (o de Ajustes). */
    fun simples(m: Modalidade, concurso: Int?, lista: List<PrecoDoConcurso> = LISTA): Long? =
        concurso?.takeIf { it > 0 }?.let { n -> lista.firstOrNull { it.modalidade == m && it.concurso == n }?.simples }

    /** Os preços diferentes da modalidade [m] (para reconhecer o rascunho que saiu de um desses concursos). */
    fun daModalidade(m: Modalidade, lista: List<PrecoDoConcurso> = LISTA): List<PrecoDoConcurso> = lista.filter { it.modalidade == m }
}

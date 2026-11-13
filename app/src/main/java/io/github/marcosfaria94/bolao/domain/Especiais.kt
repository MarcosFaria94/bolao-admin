package io.github.marcosfaria94.bolao.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Um concurso especial (Mega da Virada, Quina de São João...): a Caixa abre as apostas semanas antes do
 * sorteio, enquanto os concursos normais da modalidade continuam.
 */
data class ConcursoEspecial(
    val modalidade: Modalidade,
    /** "Mega da Virada". */
    val nome: String,
    /** O nome na tarja, em minúsculas, como a Caixa escreve: "mega da virada". */
    val tarja: String,
    val concurso: Int,
    /** Dia e hora do sorteio (de Brasília). */
    val sorteio: LocalDateTime,
    /** Prêmio estimado divulgado pela Caixa, em centavos (null = não divulgado). */
    val premioEstimado: Long?,
) {
    val dia: LocalDate get() = sorteio.toLocalDate()
    val hora: LocalTime get() = sorteio.toLocalTime()

    /** O instante do sorteio. */
    fun momento(): Instant = sorteio.atZone(AgendaDaCaixa.FUSO).toInstant()
}

/**
 * Os concursos especiais que aparecem entre as opções dos Jogos (desde a 0.27). A API da Caixa não diz
 * o número nem a data de um concurso especial antes de ele ser o próximo da modalidade (só o valor
 * acumulado para ele), então a lista é escrita aqui, com o que a Caixa anuncia: a cada concurso
 * especial novo, acrescentar uma linha em [CONHECIDOS] (o app sai numa versão nova).
 *
 * Enquanto o sorteio não passa e o concurso ainda não é o próximo da modalidade, ele aparece como
 * uma opção à parte nos Jogos, com a tarja dele ([aParte]); quando vira o próximo concurso, é o
 * próprio cartão da modalidade que ganha a tarja do especial.
 */
object Especiais {

    val CONHECIDOS: List<ConcursoEspecial> = listOf(
        // Anunciada pela Caixa em 03/11/2026: concurso 3100, sorteio na sexta-feira, 01/01/2027, às 11h.
        ConcursoEspecial(
            Modalidade.MEGA_SENA, "Mega da Virada", "mega da virada", 3100,
            LocalDateTime.of(2027, 1, 1, 11, 0), premioEstimado = 100_000_000_000L,
        ),
    )

    /** O concurso especial de número [numero] da modalidade [m] (null = concurso normal). */
    fun doConcurso(m: Modalidade, numero: Int?, lista: List<ConcursoEspecial> = CONHECIDOS): ConcursoEspecial? =
        numero?.takeIf { it > 0 }?.let { n -> lista.firstOrNull { it.modalidade == m && it.concurso == n } }

    /** A hora do sorteio especial da modalidade [m] no dia [dia] (null = não há especial nesse dia). */
    fun hora(m: Modalidade, dia: LocalDate, lista: List<ConcursoEspecial> = CONHECIDOS): LocalTime? =
        lista.firstOrNull { it.modalidade == m && it.dia == dia }?.hora

    /**
     * Os especiais que aparecem à parte nos Jogos: o sorteio ainda não passou e o concurso é depois do
     * próximo da modalidade ([proximo]: o que a Caixa está recebendo; sem ele, aparece também).
     */
    fun aParte(agora: Instant, proximo: (Modalidade) -> Int?, lista: List<ConcursoEspecial> = CONHECIDOS): List<ConcursoEspecial> =
        lista.filter { e -> e.momento().isAfter(agora) && (proximo(e.modalidade)?.let { e.concurso > it } ?: true) }
            .sortedBy { it.sorteio }

    /**
     * O concurso especial em destaque (desde a 0.28): sempre que há um concurso especial com apostas abertas, à
     * parte nos Jogos ou já como o próximo concurso da modalidade ([proximo]), ele é o concurso em destaque; com
     * mais de um, o de sorteio mais próximo. null = nenhum (o destaque é o de maior k1).
     */
    fun emDestaque(agora: Instant, proximo: (Modalidade) -> Int?, lista: List<ConcursoEspecial> = CONHECIDOS): ConcursoEspecial? =
        lista.filter { e -> e.momento().isAfter(agora) && (proximo(e.modalidade)?.let { e.concurso >= it } ?: true) }
            .minByOrNull { it.sorteio }

    /**
     * A época do ano do concurso especial de cada modalidade (desde a 0.33): o k1 e o k2 do próximo especial saem só dos
     * especiais dessa época ([IndiceEspecial][io.github.marcosfaria94.bolao.domain.indices.IndiceEspecial]), para a Mega
     * da Virada não se misturar com outro especial da Mega-Sena (como o de maio de 2026).
     */
    fun epoca(m: Modalidade): java.time.MonthDay? = when (m) {
        Modalidade.MEGA_SENA -> java.time.MonthDay.of(12, 31)
        Modalidade.LOTOFACIL -> java.time.MonthDay.of(9, 9)
        Modalidade.QUINA -> java.time.MonthDay.of(6, 24)
        // A Páscoa muda de data (de 22 de março a 25 de abril).
        Modalidade.DUPLA_SENA -> java.time.MonthDay.of(4, 8)
        else -> null
    }

    /** O nome do concurso especial de cada modalidade (o do valor acumulado para ele). */
    fun nomeDoEspecial(m: Modalidade): String? = when (m) {
        Modalidade.MEGA_SENA -> "Mega da Virada"
        Modalidade.LOTOFACIL -> "Lotofácil da Independência"
        Modalidade.QUINA -> "Quina de São João"
        Modalidade.DUPLA_SENA -> "Dupla de Páscoa"
        else -> null
    }
}

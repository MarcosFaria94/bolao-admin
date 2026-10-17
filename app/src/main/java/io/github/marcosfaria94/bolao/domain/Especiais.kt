package io.github.marcosfaria94.bolao.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Concursos especiais do ano (datas aproximadas pelo calendário da Caixa).
 * Servem para avisar com antecedência; a data exata vem da Caixa na hora de apostar.
 */
object Especiais {

    data class Especial(val nome: String, val modalidade: Modalidade, val data: LocalDate)

    /** Domingo de Páscoa (algoritmo de Meeus/Jones/Butcher). */
    fun pascoa(ano: Int): LocalDate {
        val a = ano % 19
        val b = ano / 100
        val c = ano % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val mes = (h + l - 7 * m + 114) / 31
        val dia = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(ano, mes, dia)
    }

    fun doAno(ano: Int): List<Especial> = listOf(
        // Dupla de Páscoa: sábado antes do domingo de Páscoa.
        Especial("Dupla de Páscoa", Modalidade.DUPLA_SENA, pascoa(ano).minusDays(1)),
        Especial("Quina de São João", Modalidade.QUINA, LocalDate.of(ano, 6, 24)),
        // Lotofácil da Independência: por volta de 7 de setembro (sábado mais próximo).
        Especial(
            "Lotofácil da Independência",
            Modalidade.LOTOFACIL,
            LocalDate.of(ano, 9, 7).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).let {
                if (it.dayOfMonth > 10) it.minusWeeks(1) else it
            },
        ),
        Especial("Mega da Virada", Modalidade.MEGA_SENA, LocalDate.of(ano, 12, 31)),
    )

    /** Especiais que acontecem nos próximos [dias] dias a partir de [hoje] (inclusive). */
    fun proximos(hoje: LocalDate, dias: Long = 10): List<Especial> =
        (doAno(hoje.year) + doAno(hoje.year + 1)).filter { !it.data.isBefore(hoje) && !it.data.isAfter(hoje.plusDays(dias)) }
}

package io.github.marcosfaria94.bolao.domain

import java.time.LocalDate
import java.time.YearMonth

/** Dias de aposta definidos no termo (Parte 3). */
object Calendario {
    val DIAS_PROGRAMADOS = listOf(1, 4, 7, 10, 13, 16, 19, 22, 25, 28)

    fun ehDiaProgramado(d: LocalDate): Boolean = d.dayOfMonth in DIAS_PROGRAMADOS

    /** Primeiro dia programado igual ou posterior a [d]. */
    fun proximoDiaProgramado(d: LocalDate): LocalDate {
        val dia = DIAS_PROGRAMADOS.firstOrNull { it >= d.dayOfMonth }
        return if (dia != null) d.withDayOfMonth(dia) else d.plusMonths(1).withDayOfMonth(DIAS_PROGRAMADOS.first())
    }

    /** Primeiro dia programado estritamente depois de [d]. */
    fun diaProgramadoDepoisDe(d: LocalDate): LocalDate = proximoDiaProgramado(d.plusDays(1))

    fun diasProgramados(mes: YearMonth): List<LocalDate> =
        DIAS_PROGRAMADOS.filter { it <= mes.lengthOfMonth() }.map { mes.atDay(it) }

    /**
     * Quantas apostas programadas ainda faltam no mês de [hoje].
     * O dia de hoje conta se for dia programado e a aposta do dia ainda não foi feita.
     */
    fun apostasRestantesNoMes(hoje: LocalDate, apostaDeHojeFeita: Boolean): Int {
        val depois = DIAS_PROGRAMADOS.count { it > hoje.dayOfMonth && it <= hoje.lengthOfMonth() }
        val hojeConta = ehDiaProgramado(hoje) && !apostaDeHojeFeita
        return depois + if (hojeConta) 1 else 0
    }

    fun ehUltimoDiaDoMes(d: LocalDate): Boolean = d.dayOfMonth == d.lengthOfMonth()
}

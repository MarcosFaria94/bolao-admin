package io.github.marcosfaria94.bolao.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Dias de aposta. Os do termo (Parte 3) são os [DIAS_PROGRAMADOS]; desde a 0.22 cada sessão escolhe os
 * seus (Ajustes > Parâmetros > Calendário de apostas), ou nenhum ("sem dia definido"): por isso as
 * funções recebem os [dias] (o padrão são os do termo).
 */
object Calendario {
    val DIAS_PROGRAMADOS = listOf(1, 4, 7, 10, 13, 16, 19, 22, 25, 28)

    fun ehDiaProgramado(d: LocalDate, dias: List<Int> = DIAS_PROGRAMADOS): Boolean = d.dayOfMonth in dias

    /** Primeiro dia programado igual ou posterior a [d]. null = sem dia definido. */
    fun proximoDia(d: LocalDate, dias: List<Int>): LocalDate? {
        if (dias.none { it in 1..31 }) return null
        var mes = YearMonth.from(d)
        // Um dia que o mês não tem (o 31 em abril) é pulado; em até 12 meses sempre há um.
        repeat(13) {
            val inicio = if (mes == YearMonth.from(d)) d.dayOfMonth else 1
            dias.sorted().firstOrNull { it >= inicio && it <= mes.lengthOfMonth() }?.let { return mes.atDay(it) }
            mes = mes.plusMonths(1)
        }
        return null
    }

    /** Primeiro dia programado igual ou posterior a [d] (com os dias do termo). */
    fun proximoDiaProgramado(d: LocalDate): LocalDate = proximoDia(d, DIAS_PROGRAMADOS)!!

    /** Primeiro dia programado estritamente depois de [d]. */
    fun diaProgramadoDepoisDe(d: LocalDate): LocalDate = proximoDiaProgramado(d.plusDays(1))

    /** Primeiro dia de aposta estritamente depois de [d]. null = sem dia definido. */
    fun diaDepoisDe(d: LocalDate, dias: List<Int>): LocalDate? = proximoDia(d.plusDays(1), dias)

    fun diasProgramados(mes: YearMonth, dias: List<Int> = DIAS_PROGRAMADOS): List<LocalDate> =
        dias.sorted().filter { it in 1..mes.lengthOfMonth() }.map { mes.atDay(it) }

    /**
     * Quantas apostas programadas ainda faltam no mês de [hoje].
     * O dia de hoje conta se for dia programado e a aposta do dia ainda não foi feita.
     */
    fun apostasRestantesNoMes(hoje: LocalDate, apostaDeHojeFeita: Boolean, dias: List<Int> = DIAS_PROGRAMADOS): Int {
        val depois = dias.distinct().count { it > hoje.dayOfMonth && it <= hoje.lengthOfMonth() }
        val hojeConta = ehDiaProgramado(hoje, dias) && !apostaDeHojeFeita
        return depois + if (hojeConta) 1 else 0
    }

    fun ehUltimoDiaDoMes(d: LocalDate): Boolean = d.dayOfMonth == d.lengthOfMonth()

    /** "1, 4, 7 e 10". */
    fun texto(dias: List<Int>): String = when (dias.size) {
        0 -> ""
        1 -> dias[0].toString()
        else -> dias.dropLast(1).joinToString(", ") + " e " + dias.last()
    }
}

package io.github.marcosfaria94.bolao.domain

import java.time.LocalDate
import java.time.YearMonth

object Datas {
    private val MESES = listOf("JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ")
    private val MESES_EXTENSO = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private val DIAS_SEMANA = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")

    private fun dois(n: Int) = n.toString().padStart(2, '0')

    /** 2026-06-01 -> "010626" (como no termo). */
    fun compacta(d: LocalDate): String = dois(d.dayOfMonth) + dois(d.monthValue) + dois(d.year % 100)

    /** 2026-06-01 -> "01/06/26". */
    fun curta(d: LocalDate): String = "${dois(d.dayOfMonth)}/${dois(d.monthValue)}/${dois(d.year % 100)}"

    /** 2026-06-01 -> "01/06/2026". */
    fun longa(d: LocalDate): String = "${dois(d.dayOfMonth)}/${dois(d.monthValue)}/${d.year}"

    /** 2026-06-01 -> "01/06". */
    fun diaMes(d: LocalDate): String = "${dois(d.dayOfMonth)}/${dois(d.monthValue)}"

    /** 2026-06 -> "JUN/2026". */
    fun mesAno(m: YearMonth): String = "${MESES[m.monthValue - 1]}/${m.year}"

    /** 2026-06 -> "junho de 2026". */
    fun mesAnoExtenso(m: YearMonth): String = "${MESES_EXTENSO[m.monthValue - 1]} de ${m.year}"

    /** "seg", "ter"... */
    fun diaSemana(d: LocalDate): String = DIAS_SEMANA[d.dayOfWeek.value - 1]

    /** "dd/MM/yyyy" -> LocalDate (formato da API da Caixa). */
    fun lerBr(texto: String?): LocalDate? {
        if (texto == null) return null
        val p = texto.trim().split("/")
        if (p.size != 3) return null
        val d = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        var a = p[2].toIntOrNull() ?: return null
        if (a < 100) a += 2000
        return runCatching { LocalDate.of(a, m, d) }.getOrNull()
    }
}

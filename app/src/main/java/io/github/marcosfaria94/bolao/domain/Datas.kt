package io.github.marcosfaria94.bolao.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object Datas {
    private val MESES = listOf("JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ")
    private val MESES_EXTENSO = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private fun dois(n: Int) = n.toString().padStart(2, '0')

    /** 2026-06-01 -> "010626". */
    fun compacta(d: LocalDate): String = dois(d.dayOfMonth) + dois(d.monthValue) + dois(d.year % 100)

    /** 2026-06-01 -> "01/06/26". */
    fun curta(d: LocalDate): String = "${dois(d.dayOfMonth)}/${dois(d.monthValue)}/${dois(d.year % 100)}"

    /** 2026-06-01 -> "01/06/2026". */
    fun longa(d: LocalDate): String = "${dois(d.dayOfMonth)}/${dois(d.monthValue)}/${d.year}"

    /** 2026-06-01 -> "01/06". */
    fun diaMes(d: LocalDate): String = "${dois(d.dayOfMonth)}/${dois(d.monthValue)}"

    /** 2026-06-01 -> "JUN". */
    fun mesCurto(d: LocalDate): String = MESES[d.monthValue - 1]

    /** 2026-06 -> "JUN/2026". */
    fun mesAno(m: YearMonth): String = "${MESES[m.monthValue - 1]}/${m.year}"

    /** 2026-06 -> "junho de 2026". */
    fun mesAnoExtenso(m: YearMonth): String = "${MESES_EXTENSO[m.monthValue - 1]} de ${m.year}"

    /** 2026-06 -> "Junho de 2026" (seletor de mês das telas). */
    fun mesAnoTitulo(m: YearMonth): String = mesAnoExtenso(m).replaceFirstChar { it.uppercase() }

    /** 17/10/2026 10:32:15 (hora do celular). */
    fun dataHora(instante: Long, fuso: ZoneId = ZoneId.systemDefault()): String {
        val t = Instant.ofEpochMilli(instante).atZone(fuso).toLocalDateTime()
        return longa(t.toLocalDate()) + " " + dois(t.hour) + ":" + dois(t.minute) + ":" + dois(t.second)
    }

    // ---------- datas das telas: ontem, hoje e amanhã por extenso ----------
    // Os comprovantes, as mensagens do WhatsApp e o PDF da auditoria usam sempre a data.

    private fun dias(d: LocalDate, hoje: LocalDate): Long = ChronoUnit.DAYS.between(hoje, d)

    /** "Ontem", "Hoje", "Amanhã" ou "22/10/2026" (sozinha ou depois de "Rótulo: "). */
    fun relativa(d: LocalDate, hoje: LocalDate): String = when (dias(d, hoje)) {
        -1L -> "Ontem"
        0L -> "Hoje"
        1L -> "Amanhã"
        else -> longa(d)
    }

    /** No meio da frase: "ontem", "hoje", "amanhã" ou "em 22/10/2026". */
    fun quando(d: LocalDate, hoje: LocalDate): String = when (dias(d, hoje)) {
        -1L -> "ontem"
        0L -> "hoje"
        1L -> "amanhã"
        else -> "em ${longa(d)}"
    }

    /** No meio da frase, com a hora: "hoje às 21:11:05" ou "em 17/10/2026 às 10:32:15". */
    fun quandoHora(instante: Long, hoje: LocalDate, fuso: ZoneId = ZoneId.systemDefault()): String {
        val t = Instant.ofEpochMilli(instante).atZone(fuso).toLocalDateTime()
        return quando(t.toLocalDate(), hoje) + " às " + dois(t.hour) + ":" + dois(t.minute) + ":" + dois(t.second)
    }

    /** Dia do sorteio na lista de apostas: "Sorteio hoje", "Sorteio amanhã", "Sorteio em 3 dias". */
    fun sorteioEm(sorteio: LocalDate, hoje: LocalDate): String = when (val n = dias(sorteio, hoje)) {
        0L -> "Sorteio hoje"
        1L -> "Sorteio amanhã"
        else -> "Sorteio em $n dias"
    }

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

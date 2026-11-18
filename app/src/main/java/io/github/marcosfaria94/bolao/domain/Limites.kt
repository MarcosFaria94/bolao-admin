package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.LimiteMensal
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoDeLimite
import java.time.LocalDate
import java.time.YearMonth

/**
 * Os limites do registro (desde a 0.38, Ajustes > Parâmetros, pedidos do usuário): o stop loss do mês (em apostas ou em
 * dinheiro gasto; sem limite por padrão) e o "não jogar com saldo insuficiente" (desligado por padrão; não vale no
 * Individual, em que o saldo começa em zero e só mostra o que se gastou ou ganhou). Passado um deles, o volante e os
 * favoritos não registram e dizem por quê.
 */
object Limites {

    /** As apostas registradas no mês [mes] e quanto elas custaram (centavos). */
    fun doMes(e: Estado, mes: YearMonth): Pair<Int, Long> = Operacoes.apostasDoMes(e, mes).let { it.size to it.sumOf { a -> a.valor } }

    /** Por que o registro de [apostas] apostas que custam [total] (centavos) não pode acontecer hoje; null = pode. */
    fun bloqueio(e: Estado, apostas: Int, total: Long, hoje: LocalDate): String? {
        val p = e.parametros
        p.limiteMensal?.let { l ->
            val (feitas, gasto) = doMes(e, YearMonth.from(hoje))
            when (l.tipo) {
                TipoDeLimite.APOSTAS -> if (feitas + apostas > l.valor) {
                    return "Stop loss: o limite é de ${textoDoLimite(l)} e este mês já tem ${apostas(feitas)}" +
                        (if (apostas > 1) " (o registro faz mais $apostas)." else ".")
                }
                TipoDeLimite.DINHEIRO -> if (gasto + total > l.valor) {
                    return "Stop loss: o limite é de ${textoDoLimite(l)}; este mês já foram ${Dinheiro.rs(gasto)} e esta aposta custa ${Dinheiro.rs(total)}."
                }
            }
        }
        if (p.exigirSaldo && !e.individual) {
            val saldo = Operacoes.saldo(e)
            if (saldo < total) return "Saldo insuficiente: o saldo é ${Dinheiro.rs(saldo)} e a aposta custa ${Dinheiro.rs(total)}."
        }
        return null
    }

    private fun apostas(n: Int): String = if (n == 1) "1 aposta" else "$n apostas"

    /** "20 apostas por mês", "R$ 100,00 por mês". */
    fun textoDoLimite(l: LimiteMensal): String = when (l.tipo) {
        TipoDeLimite.APOSTAS -> "${apostas(l.valor.toInt())} por mês"
        TipoDeLimite.DINHEIRO -> "${Dinheiro.rs(l.valor)} por mês"
    }

    /** O que a linha do stop loss mostra: "Sem limite" ou o limite. */
    fun resumo(l: LimiteMensal?): String = l?.let(::textoDoLimite) ?: SEM_LIMITE

    const val SEM_LIMITE = "Sem limite"
}

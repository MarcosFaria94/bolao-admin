package io.github.marcosfaria94.bolao.domain

/**
 * Rateio = Saldo Líquido − Provisionado; Cota = Rateio ÷ 3, arredondada para baixo
 * na segunda casa decimal. A sobra do arredondamento fica no saldo do grupo.
 */
object CalculoRateio {

    data class Resultado(
        val saldoLiquido: Long,
        val provisionado: Long,
        /** Saldo Líquido − Provisionado, antes do arredondamento. */
        val disponivel: Long,
        val cota: Long,
        /** Soma das cotas (o que sai do saldo). */
        val total: Long,
        /** Centavos que sobram do arredondamento e ficam no saldo. */
        val sobra: Long,
    ) {
        val saldoFinal: Long get() = saldoLiquido - total
    }

    /** Devolve null quando não há rateio possível (saldo não supera o provisionado). */
    fun calcular(saldoLiquido: Long, provisionado: Long, integrantes: Int = 3): Resultado? {
        require(integrantes > 0)
        val disponivel = saldoLiquido - provisionado
        if (disponivel <= 0) return null
        val cota = disponivel / integrantes
        if (cota <= 0) return null
        val total = cota * integrantes
        return Resultado(saldoLiquido, provisionado, disponivel, cota, total, disponivel - total)
    }

    /**
     * Valor de referência de cada aposta para o provisionado: os aportes do mês divididos
     * pelas 10 apostas programadas (R$ 10,00 × 3 ÷ 10 = R$ 3,00).
     */
    fun valorPorAposta(aporteMensal: Long, integrantes: Int = 3): Long =
        aporteMensal * integrantes / Calendario.DIAS_PROGRAMADOS.size

    /** Provisionado = apostas restantes do mês × valor por aposta, nunca acima dos aportes do mês. */
    fun provisionado(apostasRestantes: Int, valorPorAposta: Long, aportesDoMes: Long): Long =
        minOf(apostasRestantes * valorPorAposta, maxOf(aportesDoMes, 0))
}

package io.github.marcosfaria94.bolao.domain.indices

import java.time.LocalDate
import java.time.MonthDay
import kotlin.math.abs

/**
 * O k1 e o k2 dos concursos especiais (desde a 0.33: Mega da Virada, Quina de São João, Lotofácil da
 * Independência, Dupla de Páscoa), que o modelo dos concursos normais não calcula ([IndiceK.calcular] devolve
 * null: as vendas de um especial são de outra escala e o prêmio não acumula).
 *
 * No concurso especial o prêmio não acumula: tudo o que vai para os prêmios (com o acumulado do ano todo para o
 * especial) sai no próprio sorteio, descendo de faixa quando a de cima não tem ganhador. Então o retorno de cada
 * R$ 1,00 apostado num especial já sorteado é exatamente o que ele pagou ÷ o que arrecadou ([retorno]: a soma de
 * ganhadores × prêmio de cada faixa, já sem o IR, como a Caixa informa, dividida pela arrecadação); o k2 é a mesma
 * conta só com as faixas de chance de pelo menos 1 em 1.000 (o mesmo corte do k2 dos concursos normais).
 *
 * O k1 e o k2 do próximo especial são a mediana (o percentil 50) desses retornos nos últimos [QUANTOS] especiais do mesmo
 * tipo (da mesma época do ano, [epoca]: a Mega da Virada só com as outras Mega da Virada). Desde a 0.38 (pedido do
 * usuário: "percentil 50 ou média simples"); de 0.33 a 0.37 era a média ponderada (o mais novo pesava 5, até 1). A
 * mediana não se deixa levar por um ano fora do comum (um rateio esquisito, um ano de venda muito baixa). O retorno é
 * uma razão (o que pagou ÷ o que arrecadou, no mesmo concurso): a inflação não mexe nele.
 */
object IndiceEspecial {

    /** Quantos especiais entram na média (os mais recentes). */
    const val QUANTOS = 5

    /** Retornos fora deste intervalo são dados quebrados da Caixa (arrecadação zerada, rateio pela metade). */
    private const val RETORNO_MINIMO = 0.05
    private const val RETORNO_MAXIMO = 3.0

    /** Até quantos dias da época do especial um concurso especial conta como do mesmo tipo. */
    const val DIAS_DA_EPOCA = 45

    /** Um especial já sorteado na conta: o concurso, o dia do sorteio ("dd/mm/aaaa"), o k1 e o k2 dele (o retorno); [peso]: 1 (desde a 0.38, a mediana). */
    data class Amostra(val numero: Int, val k1: Double, val k2: Double, val peso: Double, val data: String? = null)

    /** O k1 e o k2 estimados para o próximo especial, com os especiais que entraram na conta (o mais novo primeiro). */
    data class Estimativa(val k1: Double, val k2: Double, val amostras: List<Amostra>)

    /**
     * O retorno do especial já sorteado [r]: (k1, k2) = o que pagou ÷ o que arrecadou, em todas as faixas e só nas
     * frequentes. null sem a arrecadação ou com o rateio incompleto.
     */
    fun retorno(m: ModalidadeK, k2PMinimo: Double, r: RegistroK): Pair<Double, Double>? {
        if (r.valorArrecadado <= 0) return null
        if (m.faixas.any { it.faixa !in r.ganhadores }) return null
        var total = 0.0
        var frequente = 0.0
        for (f in m.faixas) {
            val g = r.ganhadores[f.faixa] ?: 0
            if (g <= 0) continue
            val v = r.premios[f.faixa] ?: return null
            if (v <= 0) return null
            total += g * v
            if (f.p >= k2PMinimo) frequente += g * v
        }
        val k1 = total / r.valorArrecadado
        if (k1 < RETORNO_MINIMO || k1 > RETORNO_MAXIMO) return null
        return k1 to frequente / r.valorArrecadado
    }

    /** O dia do sorteio do registro ("dd/mm/aaaa"); null sem a data. */
    private fun dia(r: RegistroK): LocalDate? = r.dataApuracao?.trim()?.split("/")?.takeIf { it.size == 3 }?.let { (d, m, a) ->
        runCatching { LocalDate.of(a.toInt(), m.toInt(), d.toInt()) }.getOrNull()
    }

    /** O dia [d] fica a até [DIAS_DA_EPOCA] dias da [epoca] (no ano dele, no anterior ou no seguinte). */
    fun daEpoca(d: LocalDate, epoca: MonthDay): Boolean {
        val distancia = (-1..1).minOf { k ->
            val alvo = epoca.atYear(d.year + k)
            abs(d.toEpochDay() - alvo.toEpochDay())
        }
        return distancia <= DIAS_DA_EPOCA
    }

    /**
     * A estimativa do próximo especial da modalidade [chave] a partir dos especiais já sorteados [especiais] (em
     * qualquer ordem; só os marcados como especiais pela Caixa contam e, com a [epoca], só os dessa época do ano).
     * null = nenhum com os dados completos.
     */
    fun estimar(p: ParametrosK, chave: String, especiais: List<RegistroK>, quantos: Int = QUANTOS, epoca: MonthDay? = null): Estimativa? {
        val m = p.modalidades[chave] ?: return null
        val validos = especiais
            .filter { it.indicadorConcursoEspecial == 2 }
            .filter { r -> epoca == null || dia(r)?.let { daEpoca(it, epoca) } ?: false }
            .distinctBy { it.numero }
            .sortedByDescending { it.numero }
            .mapNotNull { r -> retorno(m, p.k2PMinimo, r)?.let { r to it } }
            .take(quantos)
        if (validos.isEmpty()) return null
        val amostras = validos.map { (r, k) -> Amostra(r.numero, k.first, k.second, 1.0, r.dataApuracao) }
        return Estimativa(k1 = mediana(amostras.map { it.k1 }), k2 = mediana(amostras.map { it.k2 }), amostras = amostras)
    }

    /** A mediana (o percentil 50): o do meio; com uma quantidade par, a média dos dois do meio. */
    fun mediana(v: List<Double>): Double {
        val o = v.sorted()
        val n = o.size
        return if (n % 2 == 1) o[n / 2] else (o[n / 2 - 1] + o[n / 2]) / 2
    }
}

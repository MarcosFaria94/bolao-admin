package io.github.marcosfaria94.bolao.domain.indices

import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Campos de um concurso apurado, como a API da Caixa devolve, usados nos índices
 * (último concurso apurado "u" e o anterior a ele "a"). Valores em reais.
 */
data class RegistroK(
    val numero: Int,
    val numeroConcursoProximo: Int? = null,
    /** "dd/mm/aaaa". */
    val dataProximoConcurso: String? = null,
    val valorArrecadado: Double = 0.0,
    val valorEstimadoProximoConcurso: Double = 0.0,
    val valorAcumuladoProximoConcurso: Double = 0.0,
    val valorAcumuladoConcurso05: Double = 0.0,
    val numeroConcursoFinal05: Int = 0,
    val valorAcumuladoConcursoEspecial: Double = 0.0,
    val acumulado: Boolean = false,
    val indicadorConcursoEspecial: Int = 1,
    /** Ganhadores por faixa (listaRateioPremio). */
    val ganhadores: Map<Int, Int> = emptyMap(),
) {
    companion object {
        /** Lê os campos do JSON da API (ou dos casos de teste, no mesmo formato). */
        fun deJson(o: JsonObject): RegistroK = RegistroK(
            numero = o.inteiro("numero") ?: throw IllegalArgumentException("Registro sem número de concurso."),
            numeroConcursoProximo = o.inteiro("numeroConcursoProximo")?.takeIf { it != 0 },
            dataProximoConcurso = o.texto("dataProximoConcurso"),
            valorArrecadado = o.numOuNull("valorArrecadado") ?: 0.0,
            valorEstimadoProximoConcurso = o.numOuNull("valorEstimadoProximoConcurso") ?: 0.0,
            valorAcumuladoProximoConcurso = o.numOuNull("valorAcumuladoProximoConcurso") ?: 0.0,
            valorAcumuladoConcurso05 = o.numOuNull("valorAcumuladoConcurso_0_5") ?: 0.0,
            numeroConcursoFinal05 = o.inteiro("numeroConcursoFinal_0_5") ?: 0,
            valorAcumuladoConcursoEspecial = o.numOuNull("valorAcumuladoConcursoEspecial") ?: 0.0,
            acumulado = o.booleano("acumulado") ?: false,
            indicadorConcursoEspecial = o.inteiro("indicadorConcursoEspecial")?.takeIf { it != 0 } ?: 1,
            ganhadores = o.lista("listaRateioPremio").mapNotNull { e ->
                val f = e as? JsonObject ?: return@mapNotNull null
                val faixa = f.inteiro("faixa") ?: return@mapNotNull null
                faixa to (f.inteiro("numeroDeGanhadores") ?: 0)
            }.toMap(),
        )
    }
}

/** Resultado do cálculo para o próximo concurso de uma modalidade. */
data class ResultadoK(
    val numero: Int,
    val especial: Boolean,
    val acumuladoPrincipal: Double,
    val reserva: Int,
    val herancaFaixas: Map<Int, Double>,
    val nAnterior: Double,
    val nPrevisto: Double? = null,
    val sigmaLog: Double? = null,
    /** null = especial ou dados incompletos. */
    val k1: Double? = null,
    val k2: Double? = null,
    val k1PorFaixa: Map<Int, Double> = emptyMap(),
)

/**
 * Índices k1 (retorno matemático geral) e k2 (curto prazo) e odds das Loterias Caixa.
 * Reprodução fiel da implementação de referência (k_referencia.py, parâmetros v1.2).
 */
object IndiceK {

    // ---------- 1. Distribuição dos OUTROS ganhadores de uma faixa ----------

    /**
     * Devolve (xmin, probabilidades de X = xmin, xmin+1, ...). Poisson (r = null) ou binomial
     * negativa (média lam, forma r). Recorrência a partir da moda, em log, normalizada no fim.
     */
    fun distribuicao(lam: Double, r: Double?): Pair<Int, DoubleArray> {
        if (lam <= 0) return 0 to doubleArrayOf(1.0)
        val moda: Int
        val logRazao: (Int) -> Double
        if (r == null) {
            moda = floor(lam).toInt()
            logRazao = { x -> ln(lam / (x + 1)) }
        } else {
            val q = lam / (r + lam)
            moda = if (r > 1) floor((r - 1) * lam / r).toInt() else 0
            logRazao = { x -> ln((x + r) / (x + 1) * q) }
        }
        val corte = ln(1e-16)
        val acima = ArrayList<Double>()
        var lw = 0.0
        var x = moda
        while (true) {
            lw += logRazao(x)
            x += 1
            if (lw < corte) break
            acima.add(lw)
        }
        val abaixo = ArrayList<Double>()
        lw = 0.0
        x = moda
        while (x > 0) {
            lw -= logRazao(x - 1)
            x -= 1
            if (lw < corte) break
            abaixo.add(lw)
        }
        val logs = DoubleArray(abaixo.size + 1 + acima.size)
        var i = 0
        for (j in abaixo.indices.reversed()) logs[i++] = abaixo[j]
        logs[i++] = 0.0
        for (v in acima) logs[i++] = v
        val pesos = DoubleArray(logs.size) { exp(logs[it]) }
        val total = somar(pesos)
        for (k in pesos.indices) pesos[k] = pesos[k] / total
        return (moda - abaixo.size) to pesos
    }

    /** Soma compensada (como o sum() do Python 3.12+). */
    private fun somar(v: DoubleArray): Double {
        var s = 0.0
        var c = 0.0
        for (x in v) {
            val t = s + x
            c += if (abs(s) >= abs(x)) (s - t) + x else (x - t) + s
            s = t
        }
        return s + c
    }

    /** P(nenhum outro ganhador). */
    fun pZero(lam: Double, r: Double?): Double = if (r == null) exp(-lam) else (r / (r + lam)).pow(r)

    /** IR sobre o prêmio inteiro quando ele passa do limite de isenção. */
    fun liquido(v: Double, limite: Double, ir: Double): Double = if (v > limite) v * (1 - ir) else v

    /** E[ líquido(pote / (1 + X)) ], com o pote podendo assumir valores com probabilidades (cascata). */
    fun esperancaPremio(opcoes: List<Pair<Double, Double>>, lam: Double, r: Double?, limite: Double, ir: Double): Double {
        val (xmin, probs) = distribuicao(lam, r)
        var total = 0.0
        for ((pote, q) in opcoes) {
            if (q <= 0 || pote <= 0) continue
            var s = 0.0
            for (i in probs.indices) s += probs[i] * liquido(pote / (1 + xmin + i), limite, ir)
            total += q * s
        }
        return total
    }

    // ---------- 2. k1 para um número de apostas N conhecido ----------

    fun kParaN(
        m: ModalidadeK,
        numero: Int,
        n: Double,
        acumulado: Double,
        heranca: Map<Int, Double>,
        limite: Double,
        ir: Double,
    ): Pair<Double, Map<Int, Double>> {
        val faixas = m.faixas.associateBy { it.faixa }
        val preco = m.preco
        val bruto = preco * m.fracPremio * (1 - m.fracRgp)
        var fixo = 0.0
        for (f in m.faixas) if (f.fixo) fixo += f.p * f.valor
        val base = (bruto - fixo) * n
        val potes = HashMap<Int, Double>()
        for (f in m.faixas) {
            if (!f.rateio) continue
            var fracao = f.fracao
            if (f.fracaoFinalZero != null && numero % 10 == 0) fracao = f.fracaoFinalZero
            var pote = fracao * base
            if (f.principal) pote += acumulado / (1 - ir)
            pote += heranca[f.faixa] ?: 0.0
            potes[f.faixa] = pote
        }
        val cascata = m.ordemCascata
        val ev = LinkedHashMap<Int, Double>()
        for (f in m.faixas) {
            if (f.fixo) {
                ev[f.faixa] = f.p * liquido(f.valor, limite, ir)
                continue
            }
            val lam = n * f.p
            val r = f.r
            var opcoes = listOf(potes.getValue(f.faixa) to 1.0)
            val idx = cascata.indexOf(f.faixa)
            if (idx >= 0) {
                val acima = cascata.subList(0, idx)
                val z = acima.map { a -> pZero(n * faixas.getValue(a).p, faixas.getValue(a).r) }
                val lista = ArrayList<Pair<Double, Double>>()
                lista += potes.getValue(f.faixa) to (if (z.isNotEmpty()) 1 - z.last() else 1.0)
                for (kk in acima.indices.reversed()) {
                    var somaAcima = 0.0
                    for (a in acima.subList(kk, acima.size)) somaAcima += potes.getValue(a)
                    val valor = potes.getValue(f.faixa) + somaAcima
                    var prob = 1.0
                    for (zz in z.subList(kk, z.size)) prob *= zz
                    prob *= if (kk > 0) 1 - z[kk - 1] else 1.0
                    lista += valor to prob
                }
                opcoes = lista
            }
            ev[f.faixa] = f.p * esperancaPremio(opcoes, lam, r, limite, ir)
        }
        var soma = 0.0
        for (v in ev.values) soma += v
        return soma / preco to ev.mapValues { it.value / preco }
    }

    // ---------- 3. Entradas a partir dos registros da API ----------

    fun acumuladoPrincipal(u: RegistroK, numero: Int): Double {
        val ap = u.valorAcumuladoProximoConcurso
        val a05 = u.valorAcumuladoConcurso05
        val nf = u.numeroConcursoFinal05
        if (nf != 0 && nf == numero) return if (ap > 0) ap else a05
        if (ap > 0 && !u.acumulado && abs(ap - a05) < 0.01) return 0.0
        return ap
    }

    fun herancaFaixas(m: ModalidadeK, u: RegistroK): Map<Int, Double> {
        val gan = u.ganhadores
        if (m.faixas.none { it.herdaMesmaFaixa } || gan.size < m.faixas.size) return emptyMap()
        val r = u.valorArrecadado
        var fixos = 0.0
        for (f in m.faixas) if (f.fixo) fixos += (gan[f.faixa] ?: 0) * f.valor
        val base = r * m.fracPremio * (1 - m.fracRgp) - fixos
        return m.faixas.filter { it.herdaMesmaFaixa && (gan[it.faixa] ?: 0) == 0 }.associate { it.faixa to it.fracao * base }
    }

    fun ehEspecial(e: Double, eAnt: Double, acumEspecial: Double, acumProx: Double): Boolean {
        if (e == 0.0 || acumEspecial == 0.0) return false
        if (abs(acumProx - acumEspecial) >= 0.01) return false
        return e >= 2.5 * eAnt && e >= acumEspecial
    }

    fun preverN(v: VendasK, e: Double, nAnt: Double, dia: Int, reserva: Int): Pair<Double, Double> {
        val efeito = v.efeitoDiaSemana[dia.toString()] ?: v.efeitoDiaAusente
        val logN = v.intercepto + efeito + v.bLogEstimativa * ln(e) + v.bLogNAnterior * ln(nAnt) + v.bReserva * reserva
        return exp(logN) to v.sigmaLog
    }

    /** "20/10/2026" -> 0 (segunda) ... 6 (domingo). */
    private fun diaDaSemana(texto: String?): Int? {
        val p = texto?.trim()?.split("/") ?: return null
        if (p.size != 3) return null
        val d = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        val a = p[2].toIntOrNull() ?: return null
        return runCatching { LocalDate.of(a, m, d).dayOfWeek.value - 1 }.getOrNull()
    }

    /** k1, k2 e as entradas do próximo concurso, a partir do último concurso apurado [u] e do anterior [a]. */
    fun calcular(p: ParametrosK, chave: String, u: RegistroK, a: RegistroK): ResultadoK {
        val m = p.modalidades[chave] ?: throw IllegalArgumentException("Modalidade sem parâmetros: $chave")
        val ir = p.irAliquota
        val limite = p.limiteIsencao
        val numero = u.numeroConcursoProximo ?: (u.numero + 1)
        val e = u.valorEstimadoProximoConcurso
        val eAnt = a.valorEstimadoProximoConcurso
        val acumulado = acumuladoPrincipal(u, numero)
        val reserva = if (u.numeroConcursoFinal05 == numero) 1 else 0
        val heranca = herancaFaixas(m, u)
        val especial = ehEspecial(e, eAnt, u.valorAcumuladoConcursoEspecial, u.valorAcumuladoProximoConcurso)
        // Vendas do último concurso REGULAR.
        val ref = if (u.indicadorConcursoEspecial == 2) a else u
        val nAnterior = ref.valorArrecadado / m.preco
        val base = ResultadoK(numero, especial, acumulado, reserva, heranca, nAnterior)
        if (especial || e <= 0 || nAnterior <= 0) return base
        val dia = diaDaSemana(u.dataProximoConcurso) ?: return base
        val (nMed, sigma) = preverN(m.vendas, e, nAnterior, dia, reserva)
        var k1 = 0.0
        val porFaixa = LinkedHashMap<Int, Double>()
        for (i in p.ghNos.indices) {
            val x = p.ghNos[i]
            val w = p.ghPesos[i]
            val n = nMed * exp(sqrt(2.0) * sigma * x)
            val (kn, det) = kParaN(m, numero, n, acumulado, heranca, limite, ir)
            val peso = w / sqrt(Math.PI)
            k1 += peso * kn
            for ((fx, v) in det) porFaixa[fx] = (porFaixa[fx] ?: 0.0) + peso * v
        }
        // k2 (curto prazo): só as faixas com chance de pelo menos k2_p_minimo.
        val pFaixa = m.faixas.associate { it.faixa to it.p }
        var k2 = 0.0
        for ((fx, v) in porFaixa) if ((pFaixa[fx] ?: 0.0) >= p.k2PMinimo) k2 += v
        return base.copy(nPrevisto = nMed, sigmaLog = sigma, k1 = k1, k2 = k2, k1PorFaixa = porFaixa)
    }

    /** Texto da tela: [casas] casas decimais, com vírgula (ex.: 0,647). */
    fun exibir(v: Double, casas: Int): String =
        BigDecimal(v).setScale(casas, RoundingMode.HALF_EVEN).toPlainString().replace(".", ",")

    // ---------- 4. Odds ----------

    /** Chance de uma aposta simples ganhar qualquer prêmio: (P, X), com X = 1 ÷ P. */
    fun odds(m: ModalidadeK): Pair<Double, Double> {
        val soma = LinkedHashMap<String, Double>()
        for (f in m.faixas) soma[f.grupo] = (soma[f.grupo] ?: 0.0) + f.p
        var prod = 1.0
        for (s in soma.values) prod *= (1.0 - s)
        val pQualquer = 1.0 - prod
        return pQualquer to 1.0 / pQualquer
    }

    /** "1:2298" quando X >= 100; "1:9,4" quando X < 100. */
    fun exibirOdds(x: Double): String =
        if (x >= 100) "1:" + BigDecimal(x).setScale(0, RoundingMode.HALF_EVEN).toPlainString()
        else "1:" + BigDecimal(x).setScale(1, RoundingMode.HALF_EVEN).toPlainString().replace(".", ",")
}

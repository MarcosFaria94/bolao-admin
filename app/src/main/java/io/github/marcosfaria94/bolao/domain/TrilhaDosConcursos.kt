package io.github.marcosfaria94.bolao.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.time.LocalDate
import java.time.MonthDay
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * O resumo de um concurso do histórico (desde a 0.37): o número, o dia, quanto a Caixa arrecadou, se ela marcou o
 * concurso como especial e a faixa principal (quantos ganharam e quanto cada um recebeu, em centavos). É o que os
 * Detalhes usam para separar os especiais dos regulares ([EspeciaisDoHistorico]) e a tela das Estatísticas, para os
 * Concursos notáveis ([ConcursosNotaveis]).
 */
data class ResumoDoConcurso(
    val numero: Int,
    val dia: LocalDate?,
    val arrecadado: Double = 0.0,
    val marcado: Boolean = false,
    val ganhadores: Int = 0,
    val porGanhador: Long = 0,
) {
    /** O total pago na faixa principal (ganhadores × o prêmio de cada um), em centavos. */
    val total: Long get() = ganhadores * porGanhador

    /** O prêmio principal saiu (alguém acertou a faixa 1). */
    val saiu: Boolean get() = ganhadores > 0

    companion object {
        /** Lê uma linha do histórico (o JSON da Caixa, reduzido); null se ela não tiver o número. */
        fun daLinha(linha: String): ResumoDoConcurso? = runCatching {
            val o = Json.parseToJsonElement(linha) as JsonObject
            val numero = (o["numero"] as? JsonPrimitive)?.intOrNull ?: return null
            val dia = (o["dataApuracao"] as? JsonPrimitive)?.content?.let(::lerDia)
            val principal = (o["listaRateioPremio"] as? JsonArray).orEmpty()
                .mapNotNull { it as? JsonObject }
                .filter { (it["faixa"] as? JsonPrimitive)?.intOrNull != null }
                .minByOrNull { (it["faixa"] as JsonPrimitive).intOrNull!! }
            val ganhadores = (principal?.get("numeroDeGanhadores") as? JsonPrimitive)?.intOrNull ?: 0
            val valor = (principal?.get("valorPremio") as? JsonPrimitive)?.doubleOrNull ?: 0.0
            ResumoDoConcurso(
                numero = numero,
                dia = dia,
                arrecadado = (o["valorArrecadado"] as? JsonPrimitive)?.doubleOrNull ?: 0.0,
                marcado = (o["indicadorConcursoEspecial"] as? JsonPrimitive)?.intOrNull == 2,
                ganhadores = ganhadores,
                porGanhador = (valor * 100).roundToLong(),
            )
        }.getOrNull()

        /** "dd/mm/aaaa" → a data (null se não der). */
        fun lerDia(texto: String): LocalDate? = texto.trim().split("/").takeIf { it.size == 3 }?.let { (d, m, a) ->
            runCatching { LocalDate.of(a.toInt(), m.toInt(), d.toInt()) }.getOrNull()
        }
    }
}

/**
 * Os concursos especiais do histórico (desde a 0.37, para os Detalhes: o especial mostra só os especiais dele e a
 * modalidade, só os regulares; um conjunto não tem nada do outro). A Caixa marca os especiais (indicadorConcursoEspecial
 * = 2) só desde 2017/2018; os mais velhos (a Mega da Virada de 2009 a 2016, a Quina de São João de 2011 a 2017...) se
 * reconhecem pela época do ano ([Especiais.epoca]) e pela arrecadação, muitas vezes maior que a dos concursos vizinhos.
 */
object EspeciaisDoHistorico {

    /** Quantas vezes a mediana da arrecadação dos vizinhos faz de um concurso não marcado, na época, um especial. */
    const val VEZES_A_MEDIANA = 4.0

    /** Quantos concursos de cada lado entram na mediana. */
    const val VIZINHOS = 6

    /** A distância (em dias) da época do especial: a Mega da Virada é no dia 31/12, a Dupla de Páscoa muda de data... */
    const val DIAS_DA_EPOCA = 25

    /**
     * [todos]: todos os especiais (de qualquer época: os regulares não têm nenhum); [daEpoca]: os da época do especial
     * da modalidade (a Mega da Virada, sem o especial de maio de 2026), do mais velho ao mais novo.
     */
    data class Classificacao(val todos: Set<Int>, val daEpoca: List<Int>)

    /** O dia [d] fica a até [DIAS_DA_EPOCA] dias da [epoca] (no ano dele, no anterior ou no seguinte). */
    fun naEpoca(d: LocalDate, epoca: MonthDay): Boolean =
        (-1..1).minOf { k -> abs(d.toEpochDay() - epoca.atYear(d.year + k).toEpochDay()) } <= DIAS_DA_EPOCA

    /** Separa os especiais dos concursos [resumos] (em qualquer ordem) da modalidade com a época [epoca]. */
    fun classificar(resumos: List<ResumoDoConcurso>, epoca: MonthDay?): Classificacao {
        val lista = resumos.sortedBy { it.numero }
        val todos = LinkedHashSet<Int>()
        lista.forEachIndexed { i, r ->
            if (r.marcado) {
                todos += r.numero
                return@forEachIndexed
            }
            if (epoca == null || r.dia == null || r.arrecadado <= 0 || !naEpoca(r.dia, epoca)) return@forEachIndexed
            val vizinhos = (maxOf(0, i - VIZINHOS) until minOf(lista.size, i + VIZINHOS + 1))
                .filter { it != i }
                .map { lista[it].arrecadado }
                .filter { it > 0 }
                .sorted()
            if (vizinhos.isEmpty()) return@forEachIndexed
            val mediana = if (vizinhos.size % 2 == 1) vizinhos[vizinhos.size / 2] else (vizinhos[vizinhos.size / 2 - 1] + vizinhos[vizinhos.size / 2]) / 2
            if (r.arrecadado >= VEZES_A_MEDIANA * mediana) todos += r.numero
        }
        val porNumero = lista.associateBy { it.numero }
        val daEpoca = if (epoca == null) emptyList() else todos.filter { n -> porNumero[n]?.dia?.let { naEpoca(it, epoca) } == true }.sorted()
        return Classificacao(todos, daEpoca)
    }
}

/**
 * Os concursos que os Detalhes percorrem (desde a 0.37, nos botões Retroceder, Avançar, Nº Concurso, Ir e Último): na
 * página de um concurso especial, só os especiais da época dele ([especiais]: a Mega da Virada mostra só as Mega da
 * Virada); na da modalidade, todos menos os especiais ([regulares]). [ultimo]: o mais novo da trilha.
 */
class Trilha private constructor(private val lista: List<Int>?, private val pular: Set<Int>, val ultimo: Int) {

    /** O mais velho da trilha. */
    val primeiro: Int get() = lista?.first() ?: (1..ultimo).firstOrNull { it !in pular } ?: ultimo

    /** Os especiais (só os da [lista]). */
    val soEspeciais: Boolean get() = lista != null

    /** O concurso [n] faz parte da trilha. */
    fun contem(n: Int): Boolean = if (lista != null) n in lista else n in 1..ultimo && n !in pular

    /** O concurso da trilha antes do [n] (null = não há). */
    fun anterior(n: Int): Int? {
        if (lista != null) return lista.lastOrNull { it < n }
        var k = minOf(n, ultimo + 1) - 1
        while (k >= 1 && k in pular) k--
        return k.takeIf { it >= 1 }
    }

    /** O concurso da trilha depois do [n] (null = não há). */
    fun proximo(n: Int): Int? {
        if (lista != null) return lista.firstOrNull { it > n }
        var k = maxOf(n, 0) + 1
        while (k <= ultimo && k in pular) k++
        return k.takeIf { it <= ultimo }
    }

    /** O concurso da trilha mais perto do [n] (o digitado em "Nº Concurso"): ele mesmo, se fizer parte; no empate, o mais velho. */
    fun maisPerto(n: Int): Int {
        if (contem(n)) return n
        if (lista != null) return lista.minWith(compareBy<Int> { abs(it - n) }.thenBy { it })
        if (n > ultimo) return ultimo
        if (n < 1) return primeiro
        val antes = anterior(n)
        val depois = proximo(n)
        return when {
            antes == null -> depois ?: ultimo
            depois == null -> antes
            n - antes <= depois - n -> antes
            else -> depois
        }
    }

    companion object {
        /** Só os especiais [numeros] (null se não houver nenhum). */
        fun especiais(numeros: Collection<Int>): Trilha? =
            numeros.distinct().sorted().takeIf { it.isNotEmpty() }?.let { Trilha(it, emptySet(), it.last()) }

        /** Todos os concursos até o [ultimoDaModalidade], menos os [especiais] (o último da trilha é o último regular). */
        fun regulares(ultimoDaModalidade: Int, especiais: Set<Int>): Trilha {
            var u = ultimoDaModalidade
            while (u > 1 && u in especiais) u--
            return Trilha(null, especiais, u.coerceAtLeast(1))
        }
    }
}

/**
 * Os Concursos notáveis (desde a 0.37, na tela das Estatísticas): os concursos que soltaram o prêmio principal, com o
 * número, o prêmio (o total pago na faixa principal) e quantas apostas ganharam. Dois jeitos de ver: os maiores prêmios
 * e os mais recentes, [QUANTOS] de cada vez.
 */
object ConcursosNotaveis {

    /** Quantos concursos aparecem na tabela. */
    const val QUANTOS = 20

    const val MAIORES = "Maiores prêmios"
    const val RECENTES = "Mais recentes"

    const val COLUNA_CONCURSO = "Concurso"
    const val COLUNA_PREMIO = "Prêmio"
    const val COLUNA_GANHADORES = "Ganhadores"

    const val NOTA_DO_PREMIO = "Prêmio: o total pago na faixa principal do concurso, dividido entre as apostas ganhadoras."

    /** Os concursos [resumos] que soltaram o prêmio principal. */
    fun notaveis(resumos: List<ResumoDoConcurso>): List<ResumoDoConcurso> = resumos.filter { it.saiu && it.porGanhador > 0 }

    /** Os [quantos] maiores prêmios (no empate, o mais novo primeiro). */
    fun maiores(resumos: List<ResumoDoConcurso>, quantos: Int = QUANTOS): List<ResumoDoConcurso> =
        notaveis(resumos).sortedWith(compareByDescending<ResumoDoConcurso> { it.total }.thenByDescending { it.numero }).take(quantos)

    /** Os [quantos] mais recentes. */
    fun recentes(resumos: List<ResumoDoConcurso>, quantos: Int = QUANTOS): List<ResumoDoConcurso> =
        notaveis(resumos).sortedByDescending { it.numero }.take(quantos)

    /** "O prêmio principal saiu em 812 dos 3.085 concursos." */
    fun resumo(resumos: List<ResumoDoConcurso>): String {
        val saiu = notaveis(resumos).size
        val total = resumos.size
        return when {
            total == 0 -> "Nenhum concurso no histórico."
            saiu == 0 -> "O prêmio principal não saiu em nenhum dos ${milhar(total)} concursos."
            saiu == 1 -> "O prêmio principal saiu em 1 dos ${milhar(total)} concursos."
            else -> "O prêmio principal saiu em ${milhar(saiu)} dos ${milhar(total)} concursos."
        }
    }

    /** "1.234" (o ponto dos milhares). */
    fun milhar(n: Int): String = Dinheiro.formatar(n * 100L).substringBefore(",")
}

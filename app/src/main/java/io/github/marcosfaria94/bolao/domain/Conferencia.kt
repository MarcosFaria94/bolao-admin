package io.github.marcosfaria94.bolao.domain

import java.text.Normalizer

/**
 * Confere uma aposta simples contra o resultado de um concurso.
 * O prêmio calculado é uma sugestão: na apuração o Administrador confirma (ou corrige) o valor.
 */
object Conferencia {

    data class Resultado(
        /** Texto da linha "Contagem de acertos" da mensagem APURAÇÃO. */
        val acertos: String,
        /** Faixas premiadas encontradas (pode haver mais de uma: Dupla Sena, Time do Coração...). */
        val faixas: List<FaixaPremio>,
        /** Soma dos prêmios desta aposta, em centavos. */
        val premio: Long,
        /** Números do palpite que bateram (para a imagem). */
        val acertados: Set<Int> = emptySet(),
        /** Dupla Sena: números que bateram no 2º sorteio. */
        val acertadosSegundo: Set<Int> = emptySet(),
        /** Super Sete: colunas acertadas (índices 0 a 6). Loteca: jogos acertados (0 a 13). */
        val posicoesAcertadas: Set<Int> = emptySet(),
        val trevosAcertados: Set<Int> = emptySet(),
        val extraAcertado: Boolean = false,
        /** Quando o app não tem como saber o valor sozinho (ex.: finais da Federal). */
        val precisaConfirmarValor: Boolean = false,
        /** Explicação curta para a tela de apuração. */
        val observacoes: List<String> = emptyList(),
    ) {
        val premiada: Boolean get() = faixas.isNotEmpty() || premio > 0
    }

    private val FAIXAS_POR_ACERTOS: Map<Modalidade, Map<Int, Int>> = mapOf(
        Modalidade.MEGA_SENA to mapOf(6 to 1, 5 to 2, 4 to 3),
        Modalidade.LOTOFACIL to mapOf(15 to 1, 14 to 2, 13 to 3, 12 to 4, 11 to 5),
        Modalidade.QUINA to mapOf(5 to 1, 4 to 2, 3 to 3, 2 to 4),
        Modalidade.LOTOMANIA to mapOf(20 to 1, 19 to 2, 18 to 3, 17 to 4, 16 to 5, 15 to 6, 0 to 7),
        Modalidade.TIMEMANIA to mapOf(7 to 1, 6 to 2, 5 to 3, 4 to 4, 3 to 5),
        Modalidade.DIA_DE_SORTE to mapOf(7 to 1, 6 to 2, 5 to 3, 4 to 4),
        Modalidade.DUPLA_SENA to mapOf(6 to 1, 5 to 2, 4 to 3, 3 to 4),
        Modalidade.SUPER_SETE to mapOf(7 to 1, 6 to 2, 5 to 3, 4 to 4, 3 to 5),
        Modalidade.LOTECA to mapOf(14 to 1, 13 to 2),
    )

    private fun Concurso.faixa(n: Int): FaixaPremio? = faixas.firstOrNull { it.faixa == n }

    /** Faixa encontrada; se a Caixa não listou a faixa, cria uma com valor 0 para o Administrador completar. */
    private fun Concurso.faixaOuVazia(n: Int, descricao: String): FaixaPremio =
        faixa(n) ?: FaixaPremio(n, descricao, 0, 0)

    fun conferir(
        m: Modalidade,
        palpite: Palpite,
        concurso: Concurso,
        bilheteFederal: String? = null,
        fracoesFederal: Int? = null,
    ): Resultado = when (m.formato) {
        Formato.NUMEROS -> when (m) {
            Modalidade.DUPLA_SENA -> duplaSena(palpite, concurso)
            Modalidade.MAIS_MILIONARIA -> maisMilionaria(palpite, concurso)
            else -> numeros(m, palpite, concurso)
        }
        Formato.COLUNAS -> superSete(palpite, concurso)
        Formato.LOTECA -> loteca(palpite, concurso)
        Formato.FEDERAL -> federal(bilheteFederal.orEmpty(), fracoesFederal ?: 10, concurso)
    }

    // ---------- apostas com mais números (desde a 0.22) ----------

    /** O aviso da apuração de uma aposta com mais números premiada: o Administrador confere o valor. */
    const val AVISO_MAIS_NUMEROS = "Aposta com mais números: o prêmio soma as apostas simples premiadas dentro dela. Confira o valor no app da Caixa."

    /**
     * Numa aposta com [marcados] números, dos quais [acertados] saíram: quantas das apostas simples (de
     * [k] números) que cabem nela acertam exatamente [t]. Ex.: 7 números na Mega-Sena com os 6
     * sorteados = 1 sena e 6 quinas.
     */
    fun simplesCom(acertados: Int, marcados: Int, k: Int, t: Int): Long =
        Volante.combinacoes(acertados, t) * Volante.combinacoes(marcados - acertados, k - t)

    /** As faixas premiadas com a quantidade de apostas simples em cada uma, da maior para a menor. */
    private fun comQuantidade(linhas: List<Pair<FaixaPremio, Long>>): List<FaixaPremio> =
        linhas.sortedBy { it.first.faixa }.map { (f, q) -> if (q > 1) f.copy(descricao = "${f.descricao} (${q}x)") else f }

    private fun resultadoComMais(
        acertos: String,
        linhas: List<Pair<FaixaPremio, Long>>,
        base: Resultado,
        faltaDado: Boolean = false,
        observacoes: List<String> = emptyList(),
    ): Resultado = base.copy(
        acertos = acertos,
        faixas = comQuantidade(linhas),
        premio = linhas.sumOf { (f, q) -> f.valor * q },
        // Premiada com mais números: o valor é uma soma de várias apostas simples; o Administrador confirma.
        precisaConfirmarValor = faltaDado || linhas.isNotEmpty(),
        observacoes = observacoes + if (linhas.isNotEmpty()) listOf(AVISO_MAIS_NUMEROS) else emptyList(),
    )

    private fun numerosComMais(m: Modalidade, p: Palpite, c: Concurso): Resultado {
        val acertados = p.numeros.toSet() intersect c.dezenas.toSet()
        val h = acertados.size
        val linhas = mutableListOf<Pair<FaixaPremio, Long>>()
        FAIXAS_POR_ACERTOS[m].orEmpty().forEach { (t, faixa) ->
            val q = if (t > 0) simplesCom(h, p.numeros.size, m.qtdAposta, t) else 0
            if (q > 0) linhas += c.faixaOuVazia(faixa, "$t acertos") to q
        }
        var texto = "$h/${m.qtdSorteada}"
        var extraAcertado = false
        if (m.extra == Extra.MES_DE_SORTE) {
            extraAcertado = mesmoNome(p.extra, c.timeOuMes)
            if (extraAcertado) {
                texto += " + Mês de Sorte"
                // O Mês de Sorte vale para cada aposta simples dentro da aposta.
                linhas += c.faixaOuVazia(5, "Mês de Sorte") to Volante.combinacoes(p.numeros.size, m.qtdAposta)
            }
        }
        val semExtra = m.extra != Extra.NENHUM && c.timeOuMes.isNullOrBlank()
        return resultadoComMais(
            texto, linhas, Resultado("", emptyList(), 0, acertados = acertados, extraAcertado = extraAcertado),
            faltaDado = semExtra || linhas.any { it.first.valor == 0L },
            observacoes = if (semExtra) listOf("A Caixa não informou o Time do Coração/Mês de Sorte: informe o resultado à mão.") else emptyList(),
        )
    }

    private fun duplaSenaComMais(p: Palpite, c: Concurso): Resultado {
        val primeiro = p.numeros.toSet() intersect c.dezenas.toSet()
        val segundo = p.numeros.toSet() intersect c.segundoSorteio.toSet()
        val linhas = mutableListOf<Pair<FaixaPremio, Long>>()
        FAIXAS_POR_ACERTOS.getValue(Modalidade.DUPLA_SENA).forEach { (t, faixa) ->
            simplesCom(primeiro.size, p.numeros.size, 6, t).takeIf { it > 0 }?.let { linhas += c.faixaOuVazia(faixa, "$t acertos (1º sorteio)") to it }
            simplesCom(segundo.size, p.numeros.size, 6, t).takeIf { it > 0 }?.let { linhas += c.faixaOuVazia(faixa + 4, "$t acertos (2º sorteio)") to it }
        }
        return resultadoComMais(
            "${primeiro.size}/6 (1º sorteio) e ${segundo.size}/6 (2º sorteio)", linhas,
            Resultado("", emptyList(), 0, acertados = primeiro, acertadosSegundo = segundo),
            faltaDado = linhas.any { it.first.valor == 0L },
        )
    }

    /** +Milionária: as faixas pelos números e trevos acertados (6+2, 6+1 ou 0, 5+2...). */
    private val FAIXAS_MAIS_MILIONARIA: List<Triple<Int, Int, IntRange>> = listOf(
        Triple(1, 6, 2..2), Triple(2, 6, 0..1), Triple(3, 5, 2..2), Triple(4, 5, 0..1), Triple(5, 4, 2..2), Triple(6, 4, 0..1),
        Triple(7, 3, 2..2), Triple(8, 3, 1..1), Triple(9, 2, 2..2), Triple(10, 2, 1..1),
    )

    private fun maisMilionariaComMais(p: Palpite, c: Concurso): Resultado {
        val acertados = p.numeros.toSet() intersect c.dezenas.toSet()
        val trevos = p.trevos.toSet() intersect c.trevos.toSet()
        val linhas = mutableListOf<Pair<FaixaPremio, Long>>()
        FAIXAS_MAIS_MILIONARIA.forEach { (faixa, n, ts) ->
            val q = simplesCom(acertados.size, p.numeros.size, 6, n) * ts.sumOf { t -> simplesCom(trevos.size, p.trevos.size, 2, t) }
            if (q > 0) linhas += c.faixaOuVazia(faixa, "$n números + ${if (ts.first == ts.last) "${ts.first}" else "1 ou nenhum"} trevo(s)") to q
        }
        val textoTrevos = when (trevos.size) {
            0 -> ""
            1 -> " + 1 trevo"
            else -> " + ${trevos.size} trevos"
        }
        val semTrevos = c.trevos.isEmpty()
        return resultadoComMais(
            "${acertados.size}/6$textoTrevos", linhas,
            Resultado("", emptyList(), 0, acertados = acertados, trevosAcertados = trevos),
            faltaDado = semTrevos || linhas.any { it.first.valor == 0L },
            observacoes = if (semTrevos) listOf("A Caixa não informou os trevos: informe o resultado à mão.") else emptyList(),
        )
    }

    private fun numeros(m: Modalidade, p: Palpite, c: Concurso): Resultado {
        if (Volante.temMais(m, p)) return numerosComMais(m, p, c)
        val acertados = p.numeros.toSet() intersect c.dezenas.toSet()
        val n = acertados.size
        val faixas = mutableListOf<FaixaPremio>()
        FAIXAS_POR_ACERTOS[m]?.get(n)?.let { faixas += c.faixaOuVazia(it, "$n acertos") }
        var texto = "$n/${m.qtdSorteada}"
        var extraAcertado = false
        when (m.extra) {
            Extra.TIME_DO_CORACAO -> {
                extraAcertado = mesmoNome(p.extra, c.timeOuMes)
                if (extraAcertado) {
                    texto += " + Time do Coração"
                    faixas += c.faixaOuVazia(6, "Time do Coração")
                }
            }
            Extra.MES_DE_SORTE -> {
                extraAcertado = mesmoNome(p.extra, c.timeOuMes)
                if (extraAcertado) {
                    texto += " + Mês de Sorte"
                    faixas += c.faixaOuVazia(5, "Mês de Sorte")
                }
            }
            Extra.NENHUM -> Unit
        }
        val semExtra = m.extra != Extra.NENHUM && c.timeOuMes.isNullOrBlank()
        return Resultado(
            acertos = texto,
            faixas = faixas,
            premio = faixas.sumOf { it.valor },
            acertados = acertados,
            extraAcertado = extraAcertado,
            precisaConfirmarValor = semExtra || faixas.any { it.valor == 0L },
            observacoes = if (semExtra) listOf("A Caixa não informou o Time do Coração/Mês de Sorte: informe o resultado à mão.") else emptyList(),
        )
    }

    private fun duplaSena(p: Palpite, c: Concurso): Resultado {
        if (Volante.temMais(Modalidade.DUPLA_SENA, p)) return duplaSenaComMais(p, c)
        val primeiro = p.numeros.toSet() intersect c.dezenas.toSet()
        val segundo = p.numeros.toSet() intersect c.segundoSorteio.toSet()
        val mapa = FAIXAS_POR_ACERTOS.getValue(Modalidade.DUPLA_SENA)
        val faixas = buildList {
            mapa[primeiro.size]?.let { add(c.faixaOuVazia(it, "${primeiro.size} acertos (1º sorteio)")) }
            // No 2º sorteio as faixas da Caixa continuam a numeração: 5 (sena) a 8 (terno).
            mapa[segundo.size]?.let { add(c.faixaOuVazia(it + 4, "${segundo.size} acertos (2º sorteio)")) }
        }
        return Resultado(
            acertos = "${primeiro.size}/6 (1º sorteio) e ${segundo.size}/6 (2º sorteio)",
            faixas = faixas,
            premio = faixas.sumOf { it.valor },
            acertados = primeiro,
            acertadosSegundo = segundo,
            precisaConfirmarValor = faixas.any { it.valor == 0L },
        )
    }

    private fun maisMilionaria(p: Palpite, c: Concurso): Resultado {
        if (Volante.temMais(Modalidade.MAIS_MILIONARIA, p)) return maisMilionariaComMais(p, c)
        val acertados = p.numeros.toSet() intersect c.dezenas.toSet()
        val trevos = p.trevos.toSet() intersect c.trevos.toSet()
        val n = acertados.size
        val t = trevos.size
        val faixa = when {
            n == 6 && t == 2 -> 1
            n == 6 -> 2
            n == 5 && t == 2 -> 3
            n == 5 -> 4
            n == 4 && t == 2 -> 5
            n == 4 -> 6
            n == 3 && t == 2 -> 7
            n == 3 && t == 1 -> 8
            n == 2 && t == 2 -> 9
            n == 2 && t == 1 -> 10
            else -> null
        }
        val faixas = listOfNotNull(faixa?.let { c.faixaOuVazia(it, "$n números + $t trevo(s)") })
        val textoTrevos = when (t) {
            0 -> ""
            1 -> " + 1 trevo"
            else -> " + $t trevos"
        }
        val semTrevos = c.trevos.isEmpty()
        return Resultado(
            acertos = "$n/6$textoTrevos",
            faixas = faixas,
            premio = faixas.sumOf { it.valor },
            acertados = acertados,
            trevosAcertados = trevos,
            precisaConfirmarValor = semTrevos || faixas.any { it.valor == 0L },
            observacoes = if (semTrevos) listOf("A Caixa não informou os trevos: informe o resultado à mão.") else emptyList(),
        )
    }

    private fun superSete(p: Palpite, c: Concurso): Resultado {
        val colunas = p.numeros.indices.filter { i -> c.dezenas.getOrNull(i) == p.numeros[i] }.toSet()
        val n = colunas.size
        val faixas = listOfNotNull(
            FAIXAS_POR_ACERTOS.getValue(Modalidade.SUPER_SETE)[n]?.let { c.faixaOuVazia(it, "$n colunas") },
        )
        return Resultado(
            acertos = "$n/7",
            faixas = faixas,
            premio = faixas.sumOf { it.valor },
            posicoesAcertadas = colunas,
            precisaConfirmarValor = faixas.any { it.valor == 0L },
        )
    }

    private fun loteca(p: Palpite, c: Concurso): Resultado {
        val resultados = c.jogosLoteca.map { it.resultado }
        val acertos = p.loteca.indices.filter { i -> resultados.getOrNull(i) != null && resultados[i] == p.loteca[i] }.toSet()
        val semPlacar = (0 until Volante.JOGOS_LOTECA).count { resultados.getOrNull(it) == null }
        val n = acertos.size
        val faixas = listOfNotNull(
            FAIXAS_POR_ACERTOS.getValue(Modalidade.LOTECA)[n]?.let { c.faixaOuVazia(it, "$n acertos") },
        )
        return Resultado(
            acertos = "$n/14",
            faixas = faixas,
            premio = faixas.sumOf { it.valor },
            posicoesAcertadas = acertos,
            precisaConfirmarValor = semPlacar > 0 || faixas.any { it.valor == 0L },
            observacoes = if (semPlacar > 0) listOf("$semPlacar jogo(s) sem placar na Caixa: marque o resultado manualmente.") else emptyList(),
        )
    }

    /**
     * Federal: confere os 5 prêmios principais, a aproximação do 1º prêmio e os finais
     * (milhar, centena, dezena e unidade do 1º prêmio). O valor dos finais varia por extração,
     * então o app sinaliza para o Administrador confirmar.
     */
    private fun federal(bilhete: String, fracoes: Int, c: Concurso): Resultado {
        if (bilhete.none(Char::isDigit) || c.bilhetesFederal.isEmpty()) {
            return Resultado("sem resultado", emptyList(), 0, precisaConfirmarValor = true)
        }
        fun seis(t: String) = t.filter(Char::isDigit).padStart(6, '0').takeLast(6)
        val b = seis(bilhete)
        val premiados = c.bilhetesFederal.map(::seis)
        val achados = mutableListOf<FaixaPremio>()
        val obs = mutableListOf<String>()
        premiados.forEachIndexed { i, p ->
            if (p == b) achados += c.faixaOuVazia(i + 1, "").copy(descricao = "${i + 1}º prêmio")
        }
        val primeiro = premiados.first()
        val numBilhete = b.toIntOrNull()
        val numPrimeiro = primeiro.toIntOrNull()
        if (achados.isEmpty() && numBilhete != null && numPrimeiro != null && kotlin.math.abs(numBilhete - numPrimeiro) == 1) {
            achados += faixaPorNome(c, "aproxima") ?: FaixaPremio(0, "Aproximação do 1º prêmio", 0, 0)
        }
        if (achados.isEmpty()) {
            val final = when {
                b.takeLast(4) == primeiro.takeLast(4) -> "milhar" to "Milhar do 1º prêmio"
                b.takeLast(3) == primeiro.takeLast(3) -> "centena" to "Centena do 1º prêmio"
                b.takeLast(2) == primeiro.takeLast(2) -> "dezena" to "Dezena do 1º prêmio"
                b.takeLast(1) == primeiro.takeLast(1) -> "unidade" to "Unidade do 1º prêmio"
                else -> null
            }
            final?.let { (chave, nome) ->
                achados += faixaPorNome(c, chave)?.copy(descricao = nome) ?: FaixaPremio(0, nome, 0, 0)
                obs += "O valor dos prêmios pelos finais muda a cada extração: confira no comprovante da Caixa."
            }
        }
        // Valor de bilhete inteiro dividido pelas frações compradas (10 frações = bilhete inteiro).
        val premio = achados.sumOf { it.valor } * fracoes / 10
        val texto = if (achados.isEmpty()) "nenhum prêmio (bilhete $b)" else achados.joinToString(" + ") { it.descricao }
        if (achados.isNotEmpty() && fracoes < 10) obs += "Valor proporcional a $fracoes de 10 frações."
        return Resultado(
            acertos = texto,
            faixas = achados,
            premio = premio,
            precisaConfirmarValor = achados.any { it.valor == 0L },
            observacoes = obs,
        )
    }

    private fun faixaPorNome(c: Concurso, trecho: String): FaixaPremio? =
        c.faixas.firstOrNull { normalizar(it.descricao).contains(normalizar(trecho)) }

    /** Compara nomes ignorando acentos, caixa e pontuação ("Atlético/MG" ~ "ATLETICO MG"). */
    fun mesmoNome(apostado: String?, sorteado: String?): Boolean {
        val a = normalizar(apostado ?: return false)
        val s = normalizar(sorteado ?: return false)
        if (a.isEmpty() || s.isEmpty()) return false
        return a == s || s.startsWith(a) || a.startsWith(s)
    }

    fun normalizar(t: String): String =
        Normalizer.normalize(t, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .uppercase()
            .filter { it.isLetterOrDigit() }
}

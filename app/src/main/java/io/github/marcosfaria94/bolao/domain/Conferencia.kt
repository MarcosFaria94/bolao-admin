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

    private fun numeros(m: Modalidade, p: Palpite, c: Concurso): Resultado {
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
            observacoes = if (semExtra) listOf("A Caixa não informou o Time do Coração/Mês de Sorte: use \"Corrigir o resultado\".") else emptyList(),
        )
    }

    private fun duplaSena(p: Palpite, c: Concurso): Resultado {
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
            observacoes = if (semTrevos) listOf("A Caixa não informou os trevos: use \"Corrigir o resultado\".") else emptyList(),
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

package io.github.marcosfaria94.bolao.domain

import java.text.Normalizer

/**
 * Leitura do comprovante da Caixa (o texto reconhecido na imagem ou no PDF): o concurso e o que
 * foi apostado. Serve para conferir, no registro, se a aposta do app é a mesma da Caixa.
 * Tolerante: o que não der para ler com segurança fica null e não muda nada na aposta.
 */
object LeituraComprovante {

    data class Lida(
        val concurso: Int? = null,
        val numeros: List<Int>? = null,
        val trevos: List<Int>? = null,
        /** Time do Coração (Timemania) ou Mês de Sorte (Dia de Sorte). */
        val extra: String? = null,
        val palpitesLoteca: List<String>? = null,
        val bilhete: String? = null,
    ) {
        val vazia: Boolean
            get() = concurso == null && numeros == null && trevos == null && extra == null &&
                palpitesLoteca == null && bilhete == null
    }

    /** Sem acentos e em maiúsculas. */
    fun simples(t: String): String = Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").uppercase()

    /** Só letras e algarismos (para achar nomes de times sem ligar para espaços e barras). */
    private fun compacto(t: String): String = simples(t).filter { it in 'A'..'Z' || it in '0'..'9' }

    private val DATA = Regex("\\b\\d{1,2}/\\d{1,2}(/\\d{2,4})?\\b")
    private val HORA = Regex("\\b\\d{1,2}[:h]\\d{2}(:\\d{2})?\\b")
    private val DINHEIRO = Regex("R\\$\\s*[\\d.,]+|\\b\\d{1,3}(\\.\\d{3})*,\\d{2}\\b")
    private val LONGO = Regex("\\b\\d{3,}\\b")
    private val NUMERO = Regex("\\b\\d{1,2}\\b")
    private val SEPARADORES = Regex("[\\s\\-–—|·•,.;:()\\[\\]/]+")

    /** A linha sem datas, horas, valores e números longos (códigos, concurso...). */
    private fun limpa(linha: String): String =
        linha.replace(DINHEIRO, " ").replace(DATA, " ").replace(HORA, " ").replace(LONGO, " ")

    private fun tokens(linha: String): List<String> = limpa(linha).replace(SEPARADORES, " ").trim().split(" ").filter { it.isNotEmpty() }

    /** A linha só tem números de 1 ou 2 algarismos (e separadores)? */
    private fun soNumeros(linha: String): Boolean {
        val t = tokens(linha)
        return t.isNotEmpty() && t.all { it.matches(Regex("\\d{1,2}")) }
    }

    private fun numerosDe(trecho: String): List<Int> = NUMERO.findAll(limpa(trecho)).map { it.value.toInt() }.toList()

    fun ler(m: Modalidade, texto: String, times: List<String> = TIMES_DO_CORACAO): Lida {
        val linhas = texto.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return when (m.formato) {
            Formato.NUMEROS -> Lida(
                concurso = concurso(m, texto),
                numeros = numeros(m, linhas),
                trevos = trevos(m, linhas),
                extra = when (m.extra) {
                    Extra.TIME_DO_CORACAO -> time(linhas, times)
                    Extra.MES_DE_SORTE -> mes(linhas)
                    Extra.NENHUM -> null
                },
            )
            Formato.COLUNAS -> Lida(concurso = concurso(m, texto), numeros = colunas(linhas))
            Formato.LOTECA -> Lida(concurso = concurso(m, texto), palpitesLoteca = loteca(linhas))
            Formato.FEDERAL -> Lida(concurso = concurso(m, texto), bilhete = bilhete(linhas))
        }
    }

    /** "Concurso 3072", "CONCURSO Nº 3072" ou o nome da modalidade seguido do número ("Mega-Sena 3072"). */
    fun concurso(m: Modalidade, texto: String): Int? {
        val t = simples(texto)
        Regex("CONCURSO\\s*(?:N[O°º.]*\\s*)?[:\\-]?\\s*(\\d{3,5})\\b").find(t)?.let { return it.groupValues[1].toInt() }
        val palavras = simples(m.nome).replace("+", " ").split(Regex("[\\s\\-]+")).filter { it.isNotEmpty() }
        val nome = palavras.joinToString("[\\s\\-]*") { Regex.escape(it) }
        return Regex("$nome\\s*[:\\-]?\\s*(\\d{3,5})\\b").find(t)?.groupValues?.get(1)?.toInt()
    }

    /** Números marcados: depois do rótulo ("Números:", "Dezenas") ou um bloco só de números com a quantidade certa. */
    private fun numeros(m: Modalidade, linhas: List<String>): List<Int>? {
        val alvo = m.qtdAposta
        val faixa = m.menor..m.maior
        fun valida(l: List<Int>) = l.size == alvo && l.distinct().size == alvo && l.all { it in faixa }

        for ((i, linha) in linhas.withIndex()) {
            val s = simples(linha).substringBefore("TREVO")
            val rotulo = Regex("\\b(NUMEROS|DEZENAS|APOSTADOS)\\b").find(s) ?: continue
            val juntos = numerosDe(s.substring(rotulo.range.last + 1)).toMutableList()
            var k = i + 1
            while (juntos.size < alvo && k < linhas.size && soNumeros(linhas[k])) {
                juntos += numerosDe(linhas[k])
                k++
            }
            if (valida(juntos)) return juntos.sorted()
        }

        val candidatos = mutableListOf<List<Int>>()
        var bloco = mutableListOf<Int>()
        for (linha in linhas + "") {
            if (soNumeros(linha)) {
                bloco += numerosDe(linha)
            } else {
                if (valida(bloco)) candidatos += bloco.sorted()
                bloco = mutableListOf()
            }
        }
        return candidatos.distinct().singleOrNull()
    }

    /** +Milionária: os trevos (1 a 6) depois da palavra "Trevos". */
    private fun trevos(m: Modalidade, linhas: List<String>): List<Int>? {
        if (m.trevos == 0) return null
        for ((i, linha) in linhas.withIndex()) {
            val s = simples(linha)
            val pos = s.indexOf("TREVO")
            if (pos < 0) continue
            var nums = numerosDe(s.substring(pos)).filter { it in 1..6 }
            if (nums.isEmpty() && i + 1 < linhas.size && soNumeros(linhas[i + 1])) nums = numerosDe(linhas[i + 1]).filter { it in 1..6 }
            if (nums.size == m.trevos && nums.distinct().size == m.trevos) return nums.sorted()
        }
        return null
    }

    /** Linha (e a seguinte) depois do rótulo que contém todas as [palavras]; null se não houver. */
    private fun depoisDoRotulo(linhas: List<String>, vararg palavras: String): String? {
        val i = linhas.indexOfFirst { l -> val s = simples(l); palavras.all { it in s } }
        if (i < 0) return null
        val s = simples(linhas[i])
        val fim = palavras.maxOf { p -> s.indexOf(p) + p.length }
        return s.substring(fim) + " " + linhas.getOrElse(i + 1) { "" }
    }

    /**
     * Timemania: o Time do Coração. Perto do rótulo vale o nome com ou sem o estado (se for de um
     * time só); sem o rótulo, só o nome com o estado ("SANTOS/SP").
     */
    private fun time(linhas: List<String>, times: List<String>): String? {
        val zona = depoisDoRotulo(linhas, "TIME", "CORAC")
        val alvo = compacto(zona ?: linhas.joinToString(" "))
        val exatos = times.filter { compacto(it) in alvo }
        if (exatos.isNotEmpty()) return exatos.maxBy { compacto(it).length }
        if (zona == null) return null
        val porNome = times.groupBy { compacto(partesDoTime(it).first) }.filterKeys { it.length >= 3 && it in alvo }
        return porNome.maxByOrNull { it.key.length }?.value?.singleOrNull()
    }

    /** Dia de Sorte: o Mês de Sorte, perto do rótulo. */
    private fun mes(linhas: List<String>): String? {
        val zona = compacto(depoisDoRotulo(linhas, "MES", "SORTE") ?: return null)
        return MESES_DE_SORTE.filter { compacto(it) in zona }.singleOrNull()
    }

    /** Super Sete: um algarismo por coluna, da 1ª à 7ª. */
    private fun colunas(linhas: List<String>): List<Int>? {
        fun digitos(trecho: String) = Regex("\\b\\d\\b").findAll(limpa(trecho)).map { it.value.toInt() }.toList()
        for (linha in linhas) {
            val s = simples(linha)
            val rotulo = Regex("\\b(COLUNAS|NUMEROS)\\b").find(s) ?: continue
            val d = digitos(s.substring(rotulo.range.last + 1))
            if (d.size == 7) return d
        }
        val blocos = linhas.filter { l -> val t = tokens(l); t.size == 7 && t.all { it.matches(Regex("\\d")) } }
        return blocos.singleOrNull()?.let { digitos(it) }
    }

    /** Loteca: 14 palpites (1, X ou 2) depois de "Palpites". */
    private fun loteca(linhas: List<String>): List<String>? {
        for ((i, linha) in linhas.withIndex()) {
            val s = simples(linha)
            val pos = s.indexOf("PALPITE")
            if (pos < 0) continue
            val juntos = mutableListOf<String>()
            var trecho = s.substring(pos + "PALPITE".length)
            var k = i
            while (true) {
                juntos += Regex("(?<![A-Z0-9])[12X](?![A-Z0-9])").findAll(trecho).map { it.value }
                k++
                if (juntos.size >= 14 || k >= linhas.size) break
                trecho = simples(linhas[k])
            }
            if (juntos.size == 14) return juntos
        }
        return null
    }

    /** Federal: o número do bilhete (5 ou 6 algarismos) depois de "Bilhete". */
    private fun bilhete(linhas: List<String>): String? {
        val zona = depoisDoRotulo(linhas, "BILHETE") ?: return null
        return Regex("\\b(\\d{5,6})\\b").find(zona)?.groupValues?.get(1)
    }
}

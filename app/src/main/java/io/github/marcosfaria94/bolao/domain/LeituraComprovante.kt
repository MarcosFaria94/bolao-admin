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

    // ---------- vários comprovantes de uma vez (aba Comprovantes, desde a 0.10) ----------

    /**
     * Nome de cada modalidade como aparece nos comprovantes (sem acento, em maiúsculas), na ordem
     * de procura: a Quina por último, para não confundir com as faixas de prêmio de outros jogos.
     */
    private val NOMES: List<Pair<Modalidade, Regex>> = listOf(
        Modalidade.MAIS_MILIONARIA to Regex("(\\+|MAIS)\\s*-?\\s*MILIONARIA"),
        Modalidade.MEGA_SENA to Regex("MEGA\\s*-?\\s*SENA"),
        Modalidade.DUPLA_SENA to Regex("DUPLA\\s*-?\\s*SENA"),
        Modalidade.LOTOFACIL to Regex("LOTO\\s*-?\\s*FACIL"),
        Modalidade.LOTOMANIA to Regex("LOTO\\s*-?\\s*MANIA"),
        Modalidade.TIMEMANIA to Regex("TIME\\s*-?\\s*MANIA"),
        Modalidade.DIA_DE_SORTE to Regex("DIA\\s+DE\\s+SORTE"),
        Modalidade.SUPER_SETE to Regex("SUPER\\s*-?\\s*(SETE|7)\\b"),
        Modalidade.LOTECA to Regex("\\bLOTECA\\b"),
        // Só "Loteria Federal": "Caixa Econômica Federal" está em todo comprovante da Caixa.
        Modalidade.FEDERAL to Regex("LOTERIA\\s+FEDERAL"),
        Modalidade.QUINA to Regex("\\bQUINA\\b"),
    )

    /** A modalidade escrita na linha (null = nenhuma). */
    fun modalidadeDaLinha(linha: String): Modalidade? {
        val s = simples(linha)
        return NOMES.firstOrNull { it.second.containsMatchIn(s) }?.first
    }

    /** Modalidades citadas no texto, na ordem em que aparecem. */
    fun modalidades(texto: String): List<Modalidade> = texto.lines().mapNotNull { modalidadeDaLinha(it) }.distinct()

    /**
     * O texto dividido por modalidade: cada parte começa na linha com o nome da modalidade e vai até
     * o nome de outra (um comprovante de várias apostas de jogos diferentes). O que vem antes do
     * primeiro nome (o cabeçalho da Caixa) vai junto da primeira parte. Sem nenhum nome: vazio.
     */
    fun secoes(texto: String): List<Pair<Modalidade, String>> {
        val linhas = texto.lines()
        val partes = mutableListOf<Pair<Modalidade, MutableList<String>>>()
        val antes = mutableListOf<String>()
        for (l in linhas) {
            val m = modalidadeDaLinha(l)
            when {
                m != null && (partes.isEmpty() || partes.last().first != m) -> {
                    partes += m to mutableListOf<String>().also { if (partes.isEmpty()) it += antes }
                    partes.last().second += l
                }
                partes.isEmpty() -> antes += l
                else -> partes.last().second += l
            }
        }
        return partes.map { (m, l) -> m to l.joinToString("\n") }
    }

    /**
     * Todas as apostas de uma modalidade no texto (um comprovante pode ter mais de um jogo): uma
     * leitura para cada conjunto de números com a quantidade certa. Com um jogo só, é a [ler].
     */
    fun lerApostas(m: Modalidade, texto: String): List<Lida> {
        val base = ler(m, texto)
        if (m.formato != Formato.NUMEROS) return listOf(base)
        val linhas = texto.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val varios = todosOsNumeros(m, linhas)
        if (varios.size <= 1) return listOf(if (base.numeros != null) base else base.copy(numeros = varios.firstOrNull()))
        // Com vários jogos, trevos e time/mês não dá para saber de qual é: ficam de fora.
        return varios.map { Lida(concurso = base.concurso, numeros = it) }
    }

    /** Os conjuntos de números válidos do texto (depois dos rótulos ou em blocos só de números). */
    private fun todosOsNumeros(m: Modalidade, linhas: List<String>): List<List<Int>> {
        val alvo = m.qtdAposta
        val faixa = m.menor..m.maior
        fun valida(l: List<Int>) = l.size == alvo && l.distinct().size == alvo && l.all { it in faixa }
        val rotulados = mutableListOf<List<Int>>()
        var i = 0
        while (i < linhas.size) {
            val s = simples(linhas[i]).substringBefore("TREVO")
            val rotulo = Regex("\\b(NUMEROS|DEZENAS|APOSTADOS)\\b").find(s)
            if (rotulo == null) {
                i++
                continue
            }
            val juntos = numerosDe(s.substring(rotulo.range.last + 1)).toMutableList()
            var k = i + 1
            while (juntos.size < alvo && k < linhas.size && soNumeros(linhas[k])) {
                juntos += numerosDe(linhas[k])
                k++
            }
            if (valida(juntos)) rotulados += juntos.sorted()
            i = maxOf(k, i + 1)
        }
        if (rotulados.isNotEmpty()) return rotulados
        val blocos = mutableListOf<List<Int>>()
        var bloco = mutableListOf<Int>()
        for (linha in linhas + "") {
            if (soNumeros(linha)) {
                bloco += numerosDe(linha)
                if (bloco.size == alvo && valida(bloco)) {
                    blocos += bloco.sorted()
                    bloco = mutableListOf()
                }
            } else {
                if (valida(bloco)) blocos += bloco.sorted()
                bloco = mutableListOf()
            }
        }
        return blocos
    }

    /** O que se lê de um comprovante de Pix: o valor, o trecho do destino (quem recebeu) e o código da transação. */
    data class PixLido(val valor: Long?, val destino: String, val codigo: String?)

    private val VALOR = Regex("R\\$\\s*(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+,\\d{2})")

    /** O texto é de um comprovante de Pix (transferência), e não de uma aposta? */
    fun ehPix(texto: String): Boolean {
        val s = simples(texto)
        return "PIX" in s && VALOR.containsMatchIn(s) &&
            listOf("COMPROVANTE", "TRANSFERENCIA", "TRANSFERIDO", "ENVIADO", "PAGAMENTO").any { it in s }
    }

    /**
     * Lê um comprovante de Pix: o valor (na linha do "Valor", ou na seguinte; senão, o primeiro valor
     * em reais), o trecho de quem recebeu (de "Destino", "Para", "Recebedor" ou "Favorecido" até
     * "Origem"/"De"/"Pagador") e o código da transação (E2E), quando aparece.
     */
    fun lerPix(texto: String): PixLido {
        val linhas = texto.lines().map { simples(it).trim() }.filter { it.isNotEmpty() }
        var valor: Long? = null
        for ((i, l) in linhas.withIndex()) {
            if (!Regex("\\bVALOR\\b").containsMatchIn(l) || "TARIFA" in l) continue
            val achado = VALOR.find(l) ?: linhas.getOrNull(i + 1)?.let { VALOR.find(it) }
            if (achado != null) {
                valor = Dinheiro.ler(achado.groupValues[1])
                break
            }
        }
        if (valor == null) valor = VALOR.find(linhas.joinToString("\n"))?.let { Dinheiro.ler(it.groupValues[1]) }
        val inicio = linhas.indexOfFirst { l -> listOf("DESTINO", "RECEBEDOR", "FAVORECIDO", "QUEM RECEBEU").any { it in l } || l.startsWith("PARA") }
        val fim = if (inicio < 0) -1 else (inicio + 1 until linhas.size).firstOrNull { k ->
            val l = linhas[k]
            listOf("ORIGEM", "PAGADOR", "QUEM PAGOU").any { it in l } || l == "DE" || l.startsWith("DE ")
        } ?: linhas.size
        val destino = if (inicio < 0) linhas.joinToString(" ") else linhas.subList(inicio, fim).joinToString(" ")
        val codigo = Regex("\\bE\\d{8}[A-Z0-9]{20,}\\b").find(linhas.joinToString(" "))?.value
        return PixLido(valor, destino, codigo)
    }

    /** O [nome] (ou um pedaço dele com 3 letras ou mais) e a [chave] aparecem no [trecho]? Quanto maior, mais certo. */
    fun pontosDoNome(trecho: String, nome: String, apelido: String, chave: String): Int {
        val alvo = compacto(trecho)
        var pontos = 0
        val chaveCompacta = compacto(chave)
        if (chaveCompacta.length >= 5 && chaveCompacta in alvo) pontos += 5
        if (compacto(nome).length >= 3 && compacto(nome) in alvo) pontos += 4
        simples(nome).split(Regex("\\s+")).filter { it.length >= 3 && it !in setOf("DOS", "DAS", "DE", "DA", "DO") }
            .forEach { if (compacto(it) in alvo) pontos += 1 }
        if (compacto(apelido).length >= 3 && compacto(apelido) in alvo) pontos += 1
        return pontos
    }

    /** Federal: o número do bilhete (5 ou 6 algarismos) depois de "Bilhete". */
    private fun bilhete(linhas: List<String>): String? {
        val zona = depoisDoRotulo(linhas, "BILHETE") ?: return null
        return Regex("\\b(\\d{5,6})\\b").find(zona)?.groupValues?.get(1)
    }
}

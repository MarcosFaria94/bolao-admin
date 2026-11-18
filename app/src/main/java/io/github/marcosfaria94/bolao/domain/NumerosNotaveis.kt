package io.github.marcosfaria94.bolao.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Os Números notáveis (desde a 0.38, pedido do usuário, na tela das Estatísticas): todos os números da modalidade, dos
 * que mais saíram para os que menos saíram, nos concursos da página (os regulares ou só os especiais da época). Com a
 * mesma lógica, cada modalidade tem também o que ela sorteia além dos números: os Times notáveis (Timemania), os Meses
 * notáveis (Dia de Sorte), os Trevos notáveis (+Milionária) e, na Loteca, os Resultados notáveis (coluna 1, do meio e 2).
 * Na Super Sete, cada número em cada coluna. Cada item: quantas vezes saiu, em quantos % dos concursos e há quantos
 * concursos não sai (o atraso).
 */
object NumerosNotaveis {

    /** Um item da tabela: o [rotulo] (o número, o time...), quantas [vezes] saiu, o [atraso] e a [ordem] natural. */
    data class Item(val rotulo: String, val vezes: Int, val atraso: Int?, val ordem: Long, val chave: String = rotulo)

    /** Uma tabela: o título do cartão, o nome da primeira coluna, os itens (dos que mais saíram aos que menos) e quantos concursos entraram. */
    data class Grupo(val titulo: String, val coluna: String, val itens: List<Item>, val concursos: Int)

    const val TITULO_NUMEROS = "Números notáveis"
    const val TITULO_TIMES = "Times notáveis"
    const val TITULO_MESES = "Meses notáveis"
    const val TITULO_TREVOS = "Trevos notáveis"
    const val TITULO_RESULTADOS = "Resultados notáveis"

    const val COLUNA_VEZES = "Vezes"
    const val COLUNA_PORCENTAGEM = "%"
    const val COLUNA_ATRASO = "Atraso"

    const val NOTA_DO_ATRASO = "Atraso: há quantos concursos ele não sai (0 = saiu no último concurso; 3 = saiu pela última vez 3 concursos antes do último; — = nunca saiu)."
    const val NOTA_DA_PORCENTAGEM = "%: em quantos dos concursos ele saiu."

    private val MESES = listOf("Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro")

    /** As tabelas da modalidade [m] com os concursos [resumos] (em qualquer ordem). */
    fun grupos(m: Modalidade, resumos: List<ResumoDoConcurso>): List<Grupo> {
        val lista = resumos.sortedBy { it.numero }
        val n = lista.size
        return when (m.formato) {
            Formato.NUMEROS -> listOfNotNull(
                Grupo(TITULO_NUMEROS, "Número", contar(lista, (m.menor..m.maior).map { it.toLong() }) { r -> (r.numeros + r.segundo).map { it.toLong() } }
                    .map { it.copy(rotulo = numero(m, it.ordem.toInt())) }, n),
                if (m.trevos > 0) Grupo(TITULO_TREVOS, "Trevo", contar(lista, (1L..6L).toList()) { r -> r.trevos.map { it.toLong() } }, n) else null,
                when (m.extra) {
                    Extra.TIME_DO_CORACAO -> Grupo(
                        TITULO_TIMES, "Time",
                        contarTextos(lista, rotuloDe = { it.extra?.let(::chaveDoTime) }) { it.extra?.let(::chaveCompactaDoTime) },
                        lista.count { it.extra != null },
                    )
                    Extra.MES_DE_SORTE -> Grupo(TITULO_MESES, "Mês", contarMeses(lista), lista.count { it.extra != null })
                    Extra.NENHUM -> null
                },
            )
            Formato.COLUNAS -> listOf(Grupo(TITULO_NUMEROS, "Número", colunas(lista), n))
            Formato.LOTECA -> listOf(Grupo(TITULO_RESULTADOS, "Resultado", resultados(lista), lista.count { it.loteca.isNotEmpty() }))
            Formato.FEDERAL -> emptyList()
        }
    }

    /**
     * Os títulos dos cartões da modalidade [m], na ordem de [grupos], sem fazer a conta (desde a 0.39: os cartões aparecem
     * na hora e a conta chega depois, sem fechar o cartão que já foi aberto).
     */
    fun titulos(m: Modalidade): List<String> = when (m.formato) {
        Formato.NUMEROS -> listOfNotNull(
            TITULO_NUMEROS,
            TITULO_TREVOS.takeIf { m.trevos > 0 },
            when (m.extra) {
                Extra.TIME_DO_CORACAO -> TITULO_TIMES
                Extra.MES_DE_SORTE -> TITULO_MESES
                Extra.NENHUM -> null
            },
        )
        Formato.COLUNAS -> listOf(TITULO_NUMEROS)
        Formato.LOTECA -> listOf(TITULO_RESULTADOS)
        Formato.FEDERAL -> emptyList()
    }

    /** "05" (com dois algarismos, como nas bolas; na Lotomania, "00" é o 100). */
    private fun numero(m: Modalidade, n: Int): String = if (m == Modalidade.LOTOMANIA && n == 100) "00" else n.toString().padStart(2, '0')

    /** Conta os [possiveis] nos concursos (cada um lido por [doConcurso]); em ordem: mais vezes primeiro, no empate a ordem natural. */
    private fun contar(lista: List<ResumoDoConcurso>, possiveis: List<Long>, doConcurso: (ResumoDoConcurso) -> List<Long>): List<Item> {
        val vezes = HashMap<Long, Int>()
        val ultimo = HashMap<Long, Int>()
        lista.forEachIndexed { i, r -> doConcurso(r).toSet().forEach { v -> vezes[v] = (vezes[v] ?: 0) + 1; ultimo[v] = i } }
        return possiveis.map { v ->
            Item(v.toString(), vezes[v] ?: 0, ultimo[v]?.let { lista.lastIndex - it }, v)
        }.sortedWith(compareByDescending<Item> { it.vezes }.thenBy { it.ordem })
    }

    /**
     * Conta os textos (os times): só os que saíram ao menos uma vez. [doConcurso] dá a chave que junta as grafias do mesmo
     * time; [rotuloDe], o texto da tabela (o da grafia mais recente).
     */
    private fun contarTextos(
        lista: List<ResumoDoConcurso>,
        rotuloDe: (ResumoDoConcurso) -> String? = { null },
        doConcurso: (ResumoDoConcurso) -> String?,
    ): List<Item> {
        val vezes = LinkedHashMap<String, Int>()
        val ultimo = HashMap<String, Int>()
        val rotulos = HashMap<String, String>()
        val comExtra = lista.mapNotNull { r -> doConcurso(r)?.let { k -> k to (rotuloDe(r) ?: k) } }
        comExtra.forEachIndexed { i, (k, rotulo) -> vezes[k] = (vezes[k] ?: 0) + 1; ultimo[k] = i; rotulos[k] = rotulo }
        val ordem = vezes.keys.sortedBy { rotulos.getValue(it) }.withIndex().associate { (i, k) -> k to i.toLong() }
        return vezes.map { (k, v) -> Item(rotulos.getValue(k), v, ultimo[k]?.let { comExtra.lastIndex - it }, ordem.getValue(k), chave = k) }
            .sortedWith(compareByDescending<Item> { it.vezes }.thenBy { it.ordem })
    }

    /** Os 12 meses (os que nunca saíram também). */
    private fun contarMeses(lista: List<ResumoDoConcurso>): List<Item> {
        val comMes = lista.filter { mes(it.extra) != null }
        val vezes = IntArray(12)
        val ultimo = IntArray(12) { -1 }
        comMes.forEachIndexed { i, r -> val k = mes(r.extra)!!; vezes[k]++; ultimo[k] = i }
        return (0 until 12).map { k -> Item(MESES[k], vezes[k], ultimo[k].takeIf { it >= 0 }?.let { comMes.lastIndex - it }, k.toLong()) }
            .sortedWith(compareByDescending<Item> { it.vezes }.thenBy { it.ordem })
    }

    /** O mês (0 a 11) do texto da Caixa ("Novembro", "MARÇO", "marco"); null se não for um mês. */
    fun mes(texto: String?): Int? {
        val t = semAcento(texto ?: return null).lowercase(Locale.ROOT).trim()
        return MESES.indexOfFirst { semAcento(it).lowercase(Locale.ROOT) == t }.takeIf { it >= 0 }
    }

    /** O time como a Caixa escreve, sem acentos e com a UF depois da barra, para juntar o mesmo time de concursos diferentes. */
    fun chaveDoTime(texto: String): String =
        semAcento(texto).uppercase(Locale.ROOT).replace(BARRA, "/").replace(ESPACOS, " ").trim()

    /**
     * A chave que junta as grafias do mesmo time (desde a 0.39): só as letras e os algarismos do nome e da UF ("A B C/RN" e
     * "ABC/RN" eram duas linhas na tabela).
     */
    fun chaveCompactaDoTime(texto: String): String {
        val t = chaveDoTime(texto)
        val i = t.lastIndexOf('/')
        val nome = (if (i < 0) t else t.substring(0, i)).filter(Char::isLetterOrDigit)
        val uf = if (i < 0) "" else t.substring(i + 1).filter(Char::isLetterOrDigit)
        return if (uf.isEmpty()) nome else "$nome/$uf"
    }

    private fun semAcento(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD).replace(ACENTOS, "")

    // Compilados uma vez (desde a 0.39: a Timemania passa milhares de concursos por aqui).
    private val BARRA = Regex("\\s*/\\s*")
    private val ESPACOS = Regex("\\s+")
    private val ACENTOS = Regex("\\p{M}+")

    /** A Super Sete: cada número (0 a 9) em cada uma das 7 colunas, "5 na coluna 1". */
    private fun colunas(lista: List<ResumoDoConcurso>): List<Item> {
        val itens = mutableListOf<Item>()
        for (c in 0 until 7) {
            val doGrupo = contar(lista, (0L..9L).toList()) { r -> listOfNotNull(r.numeros.getOrNull(c)?.toLong()) }
            itens += doGrupo.map { it.copy(rotulo = "${it.ordem} · col. ${c + 1}", ordem = c * 10L + it.ordem, chave = "c${c + 1}:${it.ordem}") }
        }
        return itens.sortedWith(compareByDescending<Item> { it.vezes }.thenBy { it.ordem })
    }

    /** A Loteca: quantos jogos terminaram com a vitória do time 1, empate ou vitória do time 2 (os 14 de cada concurso). */
    private fun resultados(lista: List<ResumoDoConcurso>): List<Item> {
        val nomes = listOf(1 to "Coluna 1", 0 to "Coluna do meio", 2 to "Coluna 2")
        val vezes = IntArray(3)
        lista.forEach { r -> r.loteca.forEach { v -> vezes[v]++ } }
        return nomes.mapIndexed { i, (v, nome) -> Item(nome, vezes[v], null, i.toLong()) }
            .sortedWith(compareByDescending<Item> { it.vezes }.thenBy { it.ordem })
    }

    /** A % de um item: das vezes sobre os [concursos] (na Loteca, sobre os jogos). */
    fun porcentagem(vezes: Int, total: Int): String =
        if (total <= 0) "—" else String.format(Locale.ROOT, "%.1f", vezes * 100.0 / total).replace('.', ',') + "%"
}

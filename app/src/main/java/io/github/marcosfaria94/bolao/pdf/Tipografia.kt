package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.pdf.EscritorPdf.Fonte

/**
 * Medidas das fontes padrão do PDF (larguras em milésimos do tamanho, codificação WinAnsi,
 * dos códigos 32 a 255) e a quebra de parágrafos em linhas, com trechos em negrito.
 */
object Tipografia {

    private val HELVETICA = intArrayOf(
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556, 556, 556,
        556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556, 278, 556,
        556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584, 761,
        556, 0, 222, 556, 333, 1000, 556, 556, 333, 1000, 667, 333, 1000, 0, 611, 0, 0, 222, 222, 333, 333, 350, 556, 1000,
        333, 1000, 500, 333, 944, 0, 500, 667, 278, 333, 556, 556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 556, 537, 278, 333, 333, 365, 556, 834, 834, 834, 611, 667, 667, 667, 667, 667, 667, 1000, 722,
        667, 667, 667, 667, 278, 278, 278, 278, 722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611,
        556, 556, 556, 556, 556, 556, 889, 500, 556, 556, 556, 556, 278, 278, 278, 278, 556, 556, 556, 556, 556, 556, 556, 584,
        611, 556, 556, 556, 556, 500, 556, 500,
    )

    private val HELVETICA_NEGRITO = intArrayOf(
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556, 556, 556,
        556, 556, 333, 333, 584, 584, 584, 611, 975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556, 333, 556, 611, 556, 611, 556, 333, 611,
        611, 278, 278, 556, 278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584, 761,
        556, 0, 278, 556, 500, 1000, 556, 556, 333, 1000, 667, 333, 1000, 0, 611, 0, 0, 278, 278, 500, 500, 350, 556, 1000,
        333, 1000, 556, 333, 944, 0, 500, 667, 278, 333, 556, 556, 556, 556, 280, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 611, 556, 278, 333, 333, 365, 556, 834, 834, 834, 611, 722, 722, 722, 722, 722, 722, 1000, 722,
        667, 667, 667, 667, 278, 278, 278, 278, 722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611,
        556, 556, 556, 556, 556, 556, 889, 556, 556, 556, 556, 556, 278, 278, 278, 278, 611, 611, 611, 611, 611, 611, 611, 584,
        611, 611, 611, 611, 611, 556, 611, 556,
    )

    private val WIN_ANSI = java.nio.charset.Charset.forName("windows-1252")

    /** Largura do texto em pontos. */
    fun largura(texto: String, fonte: Fonte, tamanho: Float): Float {
        val bytes = EscritorPdf.normalizar(texto).toByteArray(WIN_ANSI)
        if (fonte == Fonte.COURIER || fonte == Fonte.COURIER_NEGRITO) return bytes.size * 600 * tamanho / 1000f
        val tabela = if (fonte == Fonte.HELVETICA_NEGRITO) HELVETICA_NEGRITO else HELVETICA
        var soma = 0
        for (b in bytes) {
            val v = (b.toInt() and 0xFF) - 32
            soma += if (v in tabela.indices) tabela[v] else 556
        }
        return soma * tamanho / 1000f
    }

    /** Um pedaço de texto com a sua fonte (para parágrafos com partes em negrito). */
    data class Trecho(val texto: String, val fonte: Fonte)

    /**
     * Lê "**negrito**" de um texto simples (como no termo) e devolve os trechos.
     * Asteriscos soltos (sem par) ficam como texto.
     */
    fun trechos(texto: String, normal: Fonte = Fonte.HELVETICA, negrito: Fonte = Fonte.HELVETICA_NEGRITO): List<Trecho> {
        val partes = texto.split("**")
        if (partes.size % 2 == 0) return listOf(Trecho(texto, normal))
        return partes.mapIndexedNotNull { i, p -> if (p.isEmpty()) null else Trecho(p, if (i % 2 == 1) negrito else normal) }
    }

    /** Quebra os trechos em linhas de no máximo [larguraMax] pontos, sem cortar palavras. */
    fun quebrar(trechos: List<Trecho>, tamanho: Float, larguraMax: Float): List<List<Trecho>> {
        // Palavras com o espaço que vem depois, cada uma na sua fonte.
        data class Palavra(val texto: String, val fonte: Fonte, val espacoDepois: Boolean)
        val palavras = mutableListOf<Palavra>()
        for (t in trechos) {
            var resto = t.texto
            while (resto.isNotEmpty()) {
                val i = resto.indexOf(' ')
                if (i < 0) {
                    palavras += Palavra(resto, t.fonte, false)
                    resto = ""
                } else {
                    if (i > 0) palavras += Palavra(resto.substring(0, i), t.fonte, true)
                    else if (palavras.isNotEmpty()) palavras[palavras.lastIndex] = palavras.last().copy(espacoDepois = true)
                    resto = resto.substring(i + 1)
                }
            }
        }
        val linhas = mutableListOf<List<Trecho>>()
        var atual = mutableListOf<Palavra>()
        var larguraAtual = 0f
        fun fechar() {
            if (atual.isEmpty()) return
            // Junta palavras vizinhas da mesma fonte num trecho só.
            val l = mutableListOf<Trecho>()
            atual.forEachIndexed { k, p ->
                val texto = p.texto + if (p.espacoDepois && k < atual.lastIndex) " " else ""
                if (l.isNotEmpty() && l.last().fonte == p.fonte) l[l.lastIndex] = Trecho(l.last().texto + texto, p.fonte)
                else l += Trecho(texto, p.fonte)
            }
            linhas += l
            atual = mutableListOf()
            larguraAtual = 0f
        }
        for (p in palavras) {
            val w = largura(p.texto, p.fonte, tamanho)
            val espaco = if (atual.isNotEmpty() && atual.last().espacoDepois) largura(" ", atual.last().fonte, tamanho) else 0f
            if (atual.isNotEmpty() && larguraAtual + espaco + w > larguraMax) fechar()
            larguraAtual += (if (atual.isEmpty()) 0f else espaco) + w
            atual += p
        }
        fechar()
        return linhas
    }

    fun largura(linha: List<Trecho>, tamanho: Float): Float = linha.sumOf { largura(it.texto, it.fonte, tamanho).toDouble() }.toFloat()
}

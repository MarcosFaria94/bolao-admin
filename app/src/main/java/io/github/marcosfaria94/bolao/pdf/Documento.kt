package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.pdf.EscritorPdf.Fonte

/**
 * Montagem de documentos em fluxo (de cima para baixo, com quebra de página automática)
 * sobre o [EscritorPdf]: títulos, parágrafos com negrito, listas, blocos monoespaçados,
 * tabelas e imagens. Rodapé com "página X de N".
 */
class Documento(
    val pdf: EscritorPdf = EscritorPdf(),
    private val margem: Float = 56f,
    private val rodape: String = "",
) {
    private val larguraPagina = EscritorPdf.A4_LARGURA
    private val alturaPagina = EscritorPdf.A4_ALTURA
    val largura: Float get() = larguraPagina - 2 * margem
    private val limite: Float get() = alturaPagina - margem - 18f

    private var pagina: EscritorPdf.Pagina? = null
    var y: Float = 0f
        private set

    val paginaAtual: EscritorPdf.Pagina get() = pagina ?: novaPagina()

    fun novaPagina(): EscritorPdf.Pagina {
        val p = pdf.pagina()
        pagina = p
        y = margem
        return p
    }

    /** Garante [altura] pontos livres na página (senão, vai para a próxima). */
    fun reservar(altura: Float) {
        if (pagina == null || y + altura > limite) novaPagina()
    }

    fun espaco(pontos: Float) {
        y += pontos
        if (y > limite) novaPagina()
    }

    fun titulo(texto: String, tamanho: Float = 20f, cor: Int = 0x111111) {
        val linhas = Tipografia.quebrar(listOf(Tipografia.Trecho(texto, Fonte.HELVETICA_NEGRITO)), tamanho, largura)
        reservar(linhas.size * tamanho * 1.25f + 6f)
        for (l in linhas) {
            y += tamanho
            desenharLinha(l, margem, tamanho, cor)
            y += tamanho * 0.25f
        }
        y += 6f
    }

    fun secao(texto: String, tamanho: Float = 14f) {
        espaco(8f)
        reservar(tamanho * 1.4f + 40f)
        titulo(texto, tamanho, 0x1B4D3E)
    }

    fun subsecao(texto: String) {
        espaco(2f)
        reservar(40f)
        titulo(texto, 11.5f, 0x222222)
    }

    /**
     * Parágrafo; "**assim**" fica em negrito. Com [marcacao] = false o texto vai como está
     * (para dados do cadastro: o CPF mascarado "***.099.906-**" tem asteriscos).
     */
    fun paragrafo(
        texto: String,
        tamanho: Float = 10.5f,
        cor: Int = 0x222222,
        recuo: Float = 0f,
        centralizado: Boolean = false,
        fonte: Fonte = Fonte.HELVETICA,
        depois: Float = 6f,
        marcacao: Boolean = true,
    ) {
        val negrito = if (fonte == Fonte.HELVETICA) Fonte.HELVETICA_NEGRITO else fonte
        val trechos = if (marcacao) Tipografia.trechos(texto, fonte, negrito) else listOf(Tipografia.Trecho(texto, fonte))
        val linhas = Tipografia.quebrar(trechos, tamanho, largura - recuo)
        val entrelinha = tamanho * 1.38f
        for (l in linhas) {
            reservar(entrelinha)
            y += tamanho
            val x = if (centralizado) margem + (largura - Tipografia.largura(l, tamanho)) / 2 else margem + recuo
            desenharLinha(l, x, tamanho, cor)
            y += entrelinha - tamanho
        }
        y += depois
    }

    private fun desenharLinha(l: List<Tipografia.Trecho>, xInicial: Float, tamanho: Float, cor: Int) {
        var x = xInicial
        for (t in l) {
            paginaAtual.texto(x, y, tamanho, t.fonte, t.texto, cor)
            x += Tipografia.largura(t.texto, t.fonte, tamanho)
        }
    }

    /** Lista com marcadores ("•") ou numerada. */
    fun lista(itens: List<String>, numerada: Boolean = false, tamanho: Float = 10.5f, marcacao: Boolean = true) {
        itens.forEachIndexed { i, item ->
            val marcador = if (numerada) "${i + 1}." else "•"
            reservar(tamanho * 1.4f)
            paginaAtual.texto(margem + 6f, y + tamanho, tamanho, Fonte.HELVETICA, marcador, 0x444444)
            paragrafo(item, tamanho, recuo = 22f, depois = 2f, marcacao = marcacao)
        }
        y += 4f
    }

    /** Bloco monoespaçado com fundo cinza (como os modelos de mensagem do termo). Fica inteiro numa página quando cabe. */
    fun codigo(texto: String, tamanho: Float = 8.6f) {
        val linhas = texto.split("\n")
        val entrelinha = tamanho * 1.32f
        val maisLarga = linhas.maxOfOrNull { it.length } ?: 0
        val tam = if (maisLarga * 0.6f * tamanho > largura - 16f) (largura - 16f) / (maisLarga * 0.6f) else tamanho
        val alturaTotal = linhas.size * entrelinha + 12f
        if (alturaTotal <= limite - margem) reservar(alturaTotal) else reservar(entrelinha + 14f)
        var i = 0
        while (i < linhas.size) {
            val cabem = ((limite - y - 12f) / entrelinha).toInt().coerceAtLeast(1)
            val n = minOf(cabem, linhas.size - i)
            val h = n * entrelinha + 12f
            val pg = paginaAtual
            pg.retangulo(margem, y, largura, h, preenchimento = 0xF1F1EC, contorno = 0xDDDDD5, espessura = 0.5f)
            var yy = y + 6f
            repeat(n) {
                yy += entrelinha
                pg.texto(margem + 8f, yy - entrelinha * 0.25f, tam, Fonte.COURIER, linhas[i], 0x1A1A1A)
                i++
            }
            y += h
            if (i < linhas.size) novaPagina()
        }
        y += 8f
    }

    /**
     * Tabela simples: [colunas] com largura relativa, a primeira linha é o cabeçalho.
     * [alinharDireita] = colunas com números.
     */
    fun tabela(
        linhas: List<List<String>>,
        larguras: List<Float>,
        alinharDireita: Set<Int> = emptySet(),
        tamanho: Float = 9f,
        destaque: Set<Int> = emptySet(),
    ) {
        if (linhas.isEmpty()) return
        val total = larguras.sum()
        val ws = larguras.map { it / total * largura }
        val altura = tamanho * 1.8f
        linhas.forEachIndexed { i, cols ->
            reservar(altura)
            val cab = i == 0
            if (cab) paginaAtual.retangulo(margem, y, largura, altura, preenchimento = 0x1B4D3E)
            else if (i in destaque) paginaAtual.retangulo(margem, y, largura, altura, preenchimento = 0xFBF0A6)
            else if (i % 2 == 0) paginaAtual.retangulo(margem, y, largura, altura, preenchimento = 0xF6F6F2)
            var x = margem
            cols.forEachIndexed { c, t ->
                val fonte = if (cab) Fonte.HELVETICA_NEGRITO else Fonte.HELVETICA
                val cor = if (cab) 0xFFFFFF else 0x222222
                var texto = t
                while (texto.length > 1 && Tipografia.largura(texto, fonte, tamanho) > ws[c] - 8f) texto = texto.dropLast(1)
                val w = Tipografia.largura(texto, fonte, tamanho)
                val tx = if (c in alinharDireita) x + ws[c] - 4f - w else x + 4f
                paginaAtual.texto(tx, y + altura * 0.66f, tamanho, fonte, texto, cor)
                x += ws[c]
            }
            y += altura
        }
        y += 8f
    }

    /** Imagem centralizada, com largura máxima [larguraMax] e sem passar da página. */
    fun imagem(img: EscritorPdf.Imagem, larguraMax: Float = largura, legenda: String? = null) {
        val alturaUtil = limite - margem - (if (legenda != null) 16f else 0f)
        var w = minOf(larguraMax, largura)
        var h = w * img.altura / img.largura
        if (h > alturaUtil) {
            h = alturaUtil
            w = h * img.largura / img.altura
        }
        reservar(h + (if (legenda != null) 16f else 0f))
        val x = margem + (largura - w) / 2
        paginaAtual.imagem(img, x, y, w, h)
        paginaAtual.retangulo(x, y, w, h, contorno = 0xCCCCCC, espessura = 0.4f)
        y += h + 4f
        legenda?.let {
            paginaAtual.texto(
                margem + (largura - Tipografia.largura(it, Fonte.HELVETICA_ITALICO, 8f)) / 2, y + 8f, 8f,
                Fonte.HELVETICA_ITALICO, it, 0x666666,
            )
            y += 14f
        }
        y += 6f
    }

    fun linhaHorizontal(cor: Int = 0xCCCCCC) {
        reservar(6f)
        paginaAtual.linha(margem, y, margem + largura, y, cor, 0.6f)
        y += 8f
    }

    /** Linhas de assinatura, com espaço para o carimbo da assinatura digital. */
    fun assinaturas(nomes: List<String>) {
        for (n in nomes) {
            reservar(78f)
            y += 46f
            paginaAtual.linha(margem + largura * 0.15f, y, margem + largura * 0.85f, y, 0x333333, 0.7f)
            y += 14f
            val w = Tipografia.largura(n, Fonte.HELVETICA, 10.5f)
            paginaAtual.texto(margem + (largura - w) / 2, y, 10.5f, Fonte.HELVETICA, n, 0x222222)
            y += 12f
        }
    }

    /** Escreve o rodapé em todas as páginas e grava. */
    fun gravar(saida: java.io.OutputStream) {
        val paginas = pdf.todasAsPaginas
        paginas.forEachIndexed { i, p ->
            val texto = listOf(rodape, "página ${i + 1} de ${paginas.size}").filter { it.isNotBlank() }.joinToString(" · ")
            val w = Tipografia.largura(texto, Fonte.HELVETICA, 8f)
            p.texto(margem + (largura - w) / 2, alturaPagina - margem / 2, 8f, Fonte.HELVETICA, texto, 0x888888)
        }
        pdf.gravar(saida)
    }
}

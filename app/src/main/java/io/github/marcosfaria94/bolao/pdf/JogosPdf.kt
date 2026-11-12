package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.NOME_DO_APP
import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Tarjas
import io.github.marcosfaria94.bolao.pdf.EscritorPdf.Fonte
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * O PDF do "Compartilhar" do Painel de Jogos (desde a 0.27, no lugar da imagem): leve (só texto e formas,
 * nas fontes padrão do PDF, sem imagens) e com páginas do tamanho de uma tela de celular (360 × 640 pontos),
 * para ler bem no WhatsApp sem ampliar. Como no Painel, os jogos ficam agrupados por modalidade e concurso:
 * a tarja da modalidade (como as da Caixa), o concurso, o dia e a hora do sorteio e o prêmio estimado; e
 * cada aposta com o número e as dezenas em bolinhas na cor da modalidade (os trevos, o time, o mês, as
 * colunas da Super Sete, os palpites da Loteca). No modo de teste, o aviso de que é um teste.
 */
object JogosPdf {
    const val LARGURA = 360f
    const val ALTURA = 640f
    private const val MARGEM = 16f
    private const val ALTURA_DA_TARJA = 34f
    private const val BOLA = 20f
    private const val ENTRE_BOLAS = 5f
    private const val FOLGA = 9f

    private const val TEXTO = 0x1B2430
    private const val SUAVE = 0x5B6675
    private const val FUNDO_DA_APOSTA = 0xF2F4F7
    private const val LARANJA = 0xC26A00

    /** Um concurso no PDF: a modalidade, o número, o dia do sorteio, o prêmio estimado (já escrito) e as apostas. */
    data class Grupo(
        val modalidade: Modalidade,
        val concurso: Int,
        val sorteio: LocalDate?,
        val premio: String,
        val apostas: List<Aposta>,
    )

    private val DIAS = mapOf(
        DayOfWeek.MONDAY to "segunda-feira", DayOfWeek.TUESDAY to "terça-feira", DayOfWeek.WEDNESDAY to "quarta-feira",
        DayOfWeek.THURSDAY to "quinta-feira", DayOfWeek.FRIDAY to "sexta-feira", DayOfWeek.SATURDAY to "sábado", DayOfWeek.SUNDAY to "domingo",
    )

    /** "Sexta-feira, 13/11/2026 às 21h" (a hora da agenda da Caixa; nos concursos especiais, a deles). */
    fun quando(m: Modalidade, dia: LocalDate): String =
        DIAS.getValue(dia.dayOfWeek).replaceFirstChar { it.uppercase() } + ", " + Datas.longa(dia) + " às " + AgendaDaCaixa.hora(AgendaDaCaixa.horaDoSorteio(m, dia))

    private fun rgb(argb: Long): Int = (argb and 0xFFFFFF).toInt()

    /** O que vai em bolinhas (ou quadradinhos) e o texto embaixo delas, numa aposta. */
    private class Conteudo(val bolas: List<String>, val quadrados: List<String>, val extra: String?)

    private fun conteudo(a: Aposta, nomeDoTime: (String) -> String?): Conteudo {
        val m = a.modalidade
        return when (m.formato) {
            Formato.NUMEROS -> Conteudo(
                a.numeros.sorted().map { m.rotulo(it) },
                a.trevos.sorted().map { "$it" },
                a.extra?.takeIf { it.isNotBlank() }?.let { x ->
                    when (m.extra) {
                        Extra.TIME_DO_CORACAO -> "Time do Coração: " + (nomeDoTime(x) ?: x)
                        Extra.MES_DE_SORTE -> "Mês de Sorte: " + x.lowercase().replaceFirstChar { it.uppercase() }
                        Extra.NENHUM -> null
                    }
                },
            )
            Formato.COLUNAS -> Conteudo(emptyList(), a.numeros.map { if (it in 0..9) "$it" else "-" }, null)
            Formato.LOTECA -> Conteudo(emptyList(), a.palpitesLoteca.map { it.ifBlank { "-" } }, null)
            Formato.FEDERAL -> Conteudo(emptyList(), emptyList(), "Bilhete: " + (a.bilheteFederal ?: "—"))
        }
    }

    private val porLinha: Int = ((LARGURA - 2 * MARGEM - 2 * FOLGA + ENTRE_BOLAS) / (BOLA + ENTRE_BOLAS)).toInt()

    private fun linhasDeBolas(c: Conteudo): Int = (c.bolas.size + c.quadrados.size + porLinha - 1) / porLinha

    private fun alturaDaAposta(c: Conteudo): Float {
        val linhas = linhasDeBolas(c)
        var h = FOLGA + 13f
        if (linhas > 0) h += 6f + linhas * BOLA + (linhas - 1) * ENTRE_BOLAS
        if (c.extra != null) h += 15f
        return h + FOLGA
    }

    /**
     * Gera o PDF dos [grupos]. [hoje]: a data no alto; [teste]: dados de exemplo (o aviso no alto);
     * [nomeDoTime]: o nome do time do coração como o app mostra ("Flamengo/RJ").
     */
    fun gerar(grupos: List<Grupo>, hoje: LocalDate, teste: Boolean, nomeDoTime: (String) -> String? = { null }): ByteArray {
        val pdf = EscritorPdf()
        pdf.titulo = "Meus jogos - " + Datas.longa(hoje)
        pdf.assunto = NOME_DO_APP
        var pg = pdf.pagina(LARGURA, ALTURA)
        var y = MARGEM

        // O alto da primeira página: o nome do app, "Meus jogos" e a data (e, no teste, o aviso).
        pg.texto(MARGEM, y + 9f, 9f, Fonte.HELVETICA, NOME_DO_APP, SUAVE)
        pg.texto(MARGEM, y + 31f, 20f, Fonte.HELVETICA_NEGRITO, "Meus jogos", TEXTO)
        val data = Datas.longa(hoje)
        pg.texto(LARGURA - MARGEM - Tipografia.largura(data, Fonte.HELVETICA, 10f), y + 31f, 10f, Fonte.HELVETICA, data, SUAVE)
        y += 42f
        if (teste) {
            pg.texto(MARGEM, y + 9f, 9.5f, Fonte.HELVETICA_NEGRITO, "TESTE do app (dados de exemplo) – pode ignorar", LARANJA)
            y += 16f
        }
        y += 6f

        fun novaPagina() {
            pg = pdf.pagina(LARGURA, ALTURA)
            y = MARGEM
        }
        fun cabe(h: Float): Boolean = y + h <= ALTURA - MARGEM

        for (g in grupos) {
            val conteudos = g.apostas.map { it to conteudo(it, nomeDoTime) }
            val cabecalho = ALTURA_DA_TARJA + 8f + 54f
            // O cabeçalho do concurso não fica sozinho no fim da página: vai junto com a primeira aposta.
            if (!cabe(cabecalho + (conteudos.firstOrNull()?.let { alturaDaAposta(it.second) } ?: 0f))) novaPagina()
            tarja(pg, g.modalidade, Tarjas.nome(g.modalidade, Especiais.doConcurso(g.modalidade, g.concurso)), MARGEM, y, LARGURA - 2 * MARGEM)
            y += ALTURA_DA_TARJA + 8f
            pg.texto(MARGEM, y + 12f, 12.5f, Fonte.HELVETICA_NEGRITO, "Concurso ${g.concurso}", TEXTO)
            pg.texto(MARGEM, y + 29f, 10.5f, Fonte.HELVETICA, "Sorteio: " + (g.sorteio?.let { quando(g.modalidade, it) } ?: "Não divulgado"), TEXTO)
            pg.texto(MARGEM, y + 45f, 10.5f, Fonte.HELVETICA, "Prêmio estimado: ${g.premio}", TEXTO)
            y += 54f
            for ((a, c) in conteudos) {
                val h = alturaDaAposta(c)
                if (!cabe(h)) novaPagina()
                aposta(pg, a, c, y, h)
                y += h + 6f
            }
            y += 12f
        }
        val saida = ByteArrayOutputStream()
        pdf.gravar(saida)
        return saida.toByteArray()
    }

    /** A tarja: a cor, o canto da esquerda em diagonal, o trevo e o nome em minúsculas, à direita. */
    private fun tarja(pg: EscritorPdf.Pagina, m: Modalidade, nome: String, x: Float, y: Float, w: Float) {
        val h = ALTURA_DA_TARJA
        val c = Tarjas.cores(m)
        val r = h * Tarjas.CANTO
        pg.retanguloArredondado(x, y, w, h, r, rgb(c.fundo))
        // O canto da esquerda (com os cantos arredondados do lado de fora).
        val k = r * 0.5523f
        pg.forma(
            floatArrayOf(x + r, y),
            listOf(
                floatArrayOf(x + Tarjas.FAIXA_EM_CIMA * h, y, x + Tarjas.FAIXA_EM_CIMA * h, y, x + Tarjas.FAIXA_EM_CIMA * h, y),
                floatArrayOf(x + Tarjas.FAIXA_EMBAIXO * h, y + h, x + Tarjas.FAIXA_EMBAIXO * h, y + h, x + Tarjas.FAIXA_EMBAIXO * h, y + h),
                floatArrayOf(x + r, y + h, x + r, y + h, x + r, y + h),
                floatArrayOf(x + r - k, y + h, x, y + h - r + k, x, y + h - r),
                floatArrayOf(x, y + r, x, y + r, x, y + r),
                floatArrayOf(x, y + r - k, x + r - k, y, x + r, y),
            ),
            rgb(c.faixa),
        )
        for ((angulo, cheia) in Tarjas.FOLHAS) {
            val (inicio, curvas) = Tarjas.folha(angulo, x + Tarjas.CENTRO_DO_TREVO * h, y + h / 2f, Tarjas.TAMANHO_DO_TREVO * h)
            pg.forma(inicio, curvas, rgb(if (cheia) c.folhaCheia else c.folhaClara))
        }
        var tamanho = h * 0.5f
        val livre = w - h * (Tarjas.INICIO_DO_NOME + Tarjas.MARGEM_DO_NOME)
        while (tamanho > 8f && Tipografia.largura(nome, Fonte.HELVETICA_NEGRITO, tamanho) > livre) tamanho -= 0.5f
        val largura = Tipografia.largura(nome, Fonte.HELVETICA_NEGRITO, tamanho)
        pg.texto(x + w - h * Tarjas.MARGEM_DO_NOME - largura, y + h / 2f + tamanho * 0.36f, tamanho, Fonte.HELVETICA_NEGRITO, nome, rgb(c.texto))
    }

    /** Uma aposta: o fundo claro, "Aposta Nº3" e as dezenas em bolinhas (e o time, o mês...). */
    private fun aposta(pg: EscritorPdf.Pagina, a: Aposta, c: Conteudo, y: Float, h: Float) {
        val x = MARGEM
        pg.retanguloArredondado(x, y, LARGURA - 2 * MARGEM, h, 8f, FUNDO_DA_APOSTA)
        var yy = y + FOLGA
        pg.texto(x + FOLGA, yy + 10f, 11f, Fonte.HELVETICA_NEGRITO, "Aposta Nº${a.numero}", TEXTO)
        yy += 13f
        val cor = rgb(a.modalidade.cor)
        val itens = c.bolas.map { it to false } + c.quadrados.map { it to true }
        if (itens.isNotEmpty()) {
            yy += 6f
            itens.forEachIndexed { i, (texto, quadrado) ->
                val linha = i / porLinha
                val coluna = i % porLinha
                val bx = x + FOLGA + coluna * (BOLA + ENTRE_BOLAS)
                val by = yy + linha * (BOLA + ENTRE_BOLAS)
                if (quadrado) pg.retanguloArredondado(bx, by, BOLA, BOLA, 5f, cor) else pg.circulo(bx + BOLA / 2f, by + BOLA / 2f, BOLA / 2f, cor)
                val tamanho = if (texto.length > 2) 7.5f else 9f
                val w = Tipografia.largura(texto, Fonte.HELVETICA_NEGRITO, tamanho)
                pg.texto(bx + (BOLA - w) / 2f, by + BOLA / 2f + tamanho * 0.36f, tamanho, Fonte.HELVETICA_NEGRITO, texto, 0xFFFFFF)
            }
            val linhas = linhasDeBolas(c)
            yy += linhas * BOLA + (linhas - 1) * ENTRE_BOLAS
        }
        c.extra?.let { pg.texto(x + FOLGA, yy + 12f, 10f, Fonte.HELVETICA, it, TEXTO) }
    }
}

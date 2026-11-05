package io.github.marcosfaria94.bolao.imagem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import java.io.File
import java.time.LocalDate
import kotlin.math.max

/**
 * A imagem do "Compartilhar jogos" (Painel de Jogos, desde a 0.18): uma imagem só, compacta e leve
 * (PNG de 720 px de largura), com um cartão por aposta no estilo da conferência do site da Caixa: o
 * fundo na cor da modalidade, em cima "QUINA · CONCURSO 7149" e as dezenas em círculos com o contorno
 * branco (os trevos em quadradinhos, o time do coração, o mês de sorte, as colunas da Super Sete, os
 * palpites da Loteca, o bilhete da Federal). No alto, "Jogos em andamento" e a data de hoje; no modo
 * de teste, o aviso de que é um teste.
 */
object ImagemDosJogos {
    const val LARGURA = 720
    private const val MARGEM = 20f
    private const val ESPACO = 14f
    private const val PREENCHIMENTO = 22f
    private const val ALTURA_MAXIMA = 8000

    /** Diâmetro das bolas: menores nos jogos com muitas dezenas (como a Lotomania). */
    private fun diametro(n: Int): Float = if (n > 20) 48f else 56f

    private const val ENTRE_BOLAS = 8f

    private class Pinceis(context: Context) {
        val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, true); textSize = 30f; color = Color.WHITE }
        val data = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, false); textSize = 24f; color = Color.argb(170, 255, 255, 255) }
        val teste = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, true); textSize = 22f; color = Color.rgb(0xF2, 0xB6, 0x30) }
        val cabecalho = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, false); textSize = 23f; letterSpacing = 0.04f }
        val bola = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, true); color = Color.WHITE }
        val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, false); textSize = 25f; color = Color.WHITE }
        val textoNegrito = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, true); textSize = 25f; color = Color.WHITE }
        val contorno = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }
        val fundo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    }

    /** O que vai em círculos (ou quadradinhos) no cartão da aposta [a], e o texto da linha de baixo. */
    private class Conteudo(val bolas: List<String>, val quadradas: List<String>, val rotulo: String?, val valor: String?)

    private fun conteudoDe(a: Aposta): Conteudo {
        val m = a.modalidade
        return when (m.formato) {
            Formato.NUMEROS -> {
                val (rotulo, valor) = when (m.extra) {
                    Extra.TIME_DO_CORACAO -> "Meu time:" to a.extra
                    Extra.MES_DE_SORTE -> "Meu mês:" to a.extra
                    Extra.NENHUM -> null to null
                }
                Conteudo(a.numeros.sorted().map { m.rotulo(it) }, a.trevos.sorted().map { "$it" }, rotulo, valor?.uppercase()?.takeIf { it.isNotBlank() })
            }
            // Super Sete: um número por coluna, em quadradinhos (como no volante).
            Formato.COLUNAS -> Conteudo(emptyList(), a.numeros.map { if (it in 0..9) "$it" else "-" }, null, null)
            Formato.LOTECA -> Conteudo(emptyList(), a.palpitesLoteca.map { it.ifBlank { "-" } }, null, null)
            Formato.FEDERAL -> Conteudo(emptyList(), emptyList(), "Bilhete:", a.bilheteFederal ?: "—")
        }
    }

    /** Quantas bolas cabem numa linha do cartão. */
    private fun porLinha(d: Float): Int = max(1, ((LARGURA - 2 * MARGEM - 2 * PREENCHIMENTO + ENTRE_BOLAS) / (d + ENTRE_BOLAS)).toInt())

    /** Altura das bolas e quadradinhos (em linhas de [porLinha]). */
    private fun alturaDasBolas(c: Conteudo): Float {
        val total = c.bolas.size + c.quadradas.size
        if (total == 0) return 0f
        val d = diametro(total)
        val linhas = (total + porLinha(d) - 1) / porLinha(d)
        return linhas * d + (linhas - 1) * ENTRE_BOLAS
    }

    private fun alturaDoCartao(c: Conteudo): Float {
        var h = PREENCHIMENTO + 26f + 14f
        val bolas = alturaDasBolas(c)
        if (bolas > 0f) h += bolas
        if (c.valor != null) h += (if (bolas > 0f) 14f else 0f) + 32f
        return h + PREENCHIMENTO
    }

    fun gerar(context: Context, apostas: List<Aposta>, hoje: LocalDate, teste: Boolean): Bitmap {
        val p = Pinceis(context)
        val conteudos = apostas.map { it to conteudoDe(it) }
        val topo = MARGEM + 36f + (if (teste) 30f else 0f) + 12f
        val altura = (topo + conteudos.sumOf { (_, c) -> (alturaDoCartao(c) + ESPACO).toDouble() }.toFloat() - ESPACO + MARGEM)
            .toInt().coerceIn(120, ALTURA_MAXIMA)
        val bmp = Bitmap.createBitmap(LARGURA, altura, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(0x0B, 0x16, 0x26))

        // O alto: "Jogos em andamento" e a data de hoje (e, no teste, o aviso).
        var y = MARGEM
        c.drawText("Jogos em andamento", MARGEM + 4f, y + 30f, p.titulo)
        val d = Datas.longa(hoje)
        c.drawText(d, LARGURA - MARGEM - 4f - p.data.measureText(d), y + 30f, p.data)
        y += 36f
        if (teste) {
            c.drawText("TESTE do app (dados de exemplo) – pode ignorar", MARGEM + 4f, y + 24f, p.teste)
            y += 30f
        }
        y += 12f

        for ((a, conteudo) in conteudos) {
            val m = a.modalidade
            val cor = (m.cor and 0xFFFFFFFF).toInt()
            val h = alturaDoCartao(conteudo)
            p.fundo.color = cor
            c.drawRoundRect(RectF(MARGEM, y, LARGURA - MARGEM, y + h), 26f, 26f, p.fundo)
            val x0 = MARGEM + PREENCHIMENTO
            var yy = y + PREENCHIMENTO
            // "QUINA · CONCURSO 7149" (branco translúcido, como no site da Caixa).
            p.cabecalho.color = Color.argb(200, 255, 255, 255)
            val cabecalho = listOf(m.nome.uppercase(), "CONCURSO ${a.concurso}").joinToString(" · ")
            c.drawText(cabecalho, x0, yy + 22f, p.cabecalho)
            yy += 26f + 14f

            val total = conteudo.bolas.size + conteudo.quadradas.size
            if (total > 0) {
                val diam = diametro(total)
                val n = porLinha(diam)
                p.bola.textSize = diam * 0.46f
                val fm = p.bola.fontMetrics
                (conteudo.bolas.map { it to false } + conteudo.quadradas.map { it to true }).forEachIndexed { i, (t, quadrada) ->
                    val cx = x0 + (i % n) * (diam + ENTRE_BOLAS) + diam / 2
                    val cy = yy + (i / n) * (diam + ENTRE_BOLAS) + diam / 2
                    val r = diam / 2 - 1.5f
                    if (quadrada) {
                        c.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), diam * 0.24f, diam * 0.24f, p.contorno)
                    } else {
                        c.drawCircle(cx, cy, r, p.contorno)
                    }
                    c.drawText(t, cx - p.bola.measureText(t) / 2, cy - (fm.ascent + fm.descent) / 2, p.bola)
                }
                yy += alturaDasBolas(conteudo)
            }
            if (conteudo.valor != null) {
                if (total > 0) yy += 14f
                var x = x0
                conteudo.rotulo?.let {
                    c.drawText(it, x, yy + 25f, p.texto)
                    x += p.texto.measureText(it) + 8f
                }
                c.drawText(conteudo.valor, x, yy + 25f, p.textoNegrito)
            }
            y += h + ESPACO
        }
        return bmp
    }

    /** Gera e grava a imagem em [destino] (PNG). */
    fun salvar(context: Context, apostas: List<Aposta>, hoje: LocalDate, teste: Boolean, destino: File): File {
        val bmp = gerar(context, apostas, hoje, teste)
        try {
            destino.parentFile?.mkdirs()
            destino.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bmp.recycle()
        }
        return destino
    }
}

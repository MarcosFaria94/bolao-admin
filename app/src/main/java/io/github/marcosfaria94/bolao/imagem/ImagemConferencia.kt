package io.github.marcosfaria94.bolao.imagem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import java.io.File
import kotlin.math.ceil
import kotlin.math.min

/**
 * Imagem dos acertos no estilo da conferência do site da Caixa: fundo na cor da modalidade,
 * "Números sorteados", o divisor com o trevo e "Meus números" em círculos (cheio = acerto).
 * Vai anexada ao comprovante da APURAÇÃO.
 */
object ImagemConferencia {
    private const val LARGURA = 1080
    private const val MARGEM = 72f
    private const val ALTURA_MAXIMA = 4200

    private fun clara(cor: Int, f: Float): Int = Color.rgb(
        (Color.red(cor) + (255 - Color.red(cor)) * f).toInt(),
        (Color.green(cor) + (255 - Color.green(cor)) * f).toInt(),
        (Color.blue(cor) + (255 - Color.blue(cor)) * f).toInt(),
    )

    private class Pinceis(context: Context, val cor: Int) {
        val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, false); textSize = 60f; color = Color.WHITE }
        val sorteado = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, true); textSize = 60f; color = Color.WHITE }
        val bola = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, true); textSize = 54f }
        val pequeno = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, false); textSize = 32f; color = clara(cor, 0.6f) }
        val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, false); textSize = 40f; color = Color.WHITE }
        val textoNegrito = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fontes.jost(context, true); textSize = 40f; color = Color.WHITE }
        val branco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
        val contorno = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3.5f }
        val linha = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = 3f }
    }

    /** Colunas da grade: até 7 numa linha só; mais que isso, linhas de 5 (ou 8 quando são muitos). */
    private fun colunas(n: Int): Int = when {
        n <= 7 -> n.coerceAtLeast(1)
        n % 5 == 0 && n <= 25 -> 5
        else -> 8
    }

    fun gerar(context: Context, a: Aposta, ap: Apuracao): Bitmap {
        val m = a.modalidade
        val cor = (m.cor and 0xFFFFFFFF).toInt()
        val p = Pinceis(context, cor)
        val bmp = Bitmap.createBitmap(LARGURA, ALTURA_MAXIMA, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(cor)
        val util = LARGURA - 2 * MARGEM
        var y = 56f

        fun cabecalho() {
            val partes = listOfNotNull(m.nome.uppercase(), "CONCURSO ${a.concurso}", a.dataSorteio?.let { Datas.longa(it) })
            c.drawText(partes.joinToString(" · "), MARGEM, y + 32f, p.pequeno)
            y += 62f
        }

        fun titulo(t: String) {
            c.drawText(t, MARGEM, y + 58f, p.titulo)
            y += 96f
        }

        fun subtitulo(t: String) {
            c.drawText(t, MARGEM, y + 40f, p.texto)
            y += 62f
        }

        /** Números sorteados: em negrito, sem círculo. */
        fun sorteados(lista: List<String>) {
            if (lista.isEmpty()) {
                c.drawText("—", MARGEM, y + 58f, p.sorteado)
                y += 110f
                return
            }
            val cols = colunas(lista.size)
            val cel = min(140f, util / cols)
            lista.forEachIndexed { i, t ->
                val cx = MARGEM + (i % cols) * cel + cel / 2
                val base = y + (i / cols) * 110f + 64f
                c.drawText(t, cx - p.sorteado.measureText(t) / 2, base, p.sorteado)
            }
            y += ceil(lista.size / cols.toDouble()).toFloat() * 110f + 10f
        }

        /** Meus números: círculo branco cheio quando acertou, só o contorno quando não. */
        fun meus(lista: List<String>, acerto: (Int) -> Boolean) {
            val cols = colunas(lista.size)
            val cel = min(140f, util / cols)
            val raio = min(56f, cel / 2 - 8f)
            p.bola.textSize = raio * 0.96f
            lista.forEachIndexed { i, t ->
                val cx = MARGEM + (i % cols) * cel + cel / 2
                val cy = y + (i / cols) * cel + raio + 4f
                val ok = acerto(i)
                if (ok) c.drawCircle(cx, cy, raio, p.branco) else c.drawCircle(cx, cy, raio - 1.5f, p.contorno)
                p.bola.color = if (ok) cor else Color.WHITE
                val fm = p.bola.fontMetrics
                c.drawText(t, cx - p.bola.measureText(t) / 2, cy - (fm.ascent + fm.descent) / 2, p.bola)
            }
            y += ceil(lista.size / cols.toDouble()).toFloat() * cel + 16f
        }

        fun divisor() {
            val cy = y + 34f
            c.drawLine(LARGURA * 0.22f, cy, LARGURA * 0.78f, cy, p.linha)
            c.drawCircle(LARGURA / 2f, cy, 34f, p.branco)
            val escura = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor }
            val clarinha = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = clara(cor, 0.45f) }
            Fontes.ANGULOS_FOLHAS.forEachIndexed { k, g ->
                c.drawPath(Fontes.folha(LARGURA / 2f, cy, 0.32f, g), if (k % 2 == 0) escura else clarinha)
            }
            y += 96f
        }

        fun linhaTexto(rotulo: String, valor: String, destaque: Boolean = false) {
            c.drawText(rotulo, MARGEM, y + 42f, p.texto)
            val x = MARGEM + p.texto.measureText(rotulo) + 12f
            if (destaque) {
                val w = p.textoNegrito.measureText(valor)
                c.drawRoundRect(RectF(x - 14f, y - 2f, x + w + 14f, y + 58f), 30f, 30f, p.branco)
                p.textoNegrito.color = cor
                c.drawText(valor, x, y + 42f, p.textoNegrito)
                p.textoNegrito.color = Color.WHITE
            } else {
                c.drawText(valor, x, y + 42f, p.textoNegrito)
            }
            y += 70f
        }

        cabecalho()
        when (m.formato) {
            Formato.NUMEROS -> {
                val apostados = a.numeros.sorted()
                titulo("Números sorteados")
                if (m == Modalidade.DUPLA_SENA) {
                    subtitulo("1º sorteio")
                    sorteados(ap.dezenasSorteadas.map { m.rotulo(it) })
                    subtitulo("2º sorteio")
                    sorteados(ap.segundoSorteio.map { m.rotulo(it) })
                } else {
                    sorteados(ap.dezenasSorteadas.map { m.rotulo(it) })
                }
                if (m.trevos > 0) {
                    subtitulo("Trevos")
                    sorteados(ap.trevosSorteados.map { "$it" })
                }
                when (m.extra) {
                    Extra.TIME_DO_CORACAO -> linhaTexto("Time do Coração:", ap.extraSorteado ?: "—")
                    Extra.MES_DE_SORTE -> linhaTexto("Mês de Sorte:", ap.extraSorteado ?: "—")
                    Extra.NENHUM -> Unit
                }
                divisor()
                titulo("Meus números")
                if (m == Modalidade.DUPLA_SENA) {
                    subtitulo("no 1º sorteio")
                    meus(apostados.map { m.rotulo(it) }) { apostados[it] in ap.dezenasSorteadas }
                    subtitulo("no 2º sorteio")
                    meus(apostados.map { m.rotulo(it) }) { apostados[it] in ap.segundoSorteio }
                } else {
                    meus(apostados.map { m.rotulo(it) }) { apostados[it] in ap.dezenasSorteadas }
                }
                if (m.trevos > 0) {
                    subtitulo("Meus trevos")
                    val trevos = a.trevos.sorted()
                    meus(trevos.map { "$it" }) { trevos[it] in ap.trevosSorteados }
                }
                if (m.extra != Extra.NENHUM && !a.extra.isNullOrBlank()) {
                    val acertou = io.github.marcosfaria94.bolao.domain.Conferencia.mesmoNome(a.extra, ap.extraSorteado)
                    linhaTexto(if (m.extra == Extra.MES_DE_SORTE) "Meu mês:" else "Meu time:", a.extra, destaque = acertou)
                }
            }
            Formato.COLUNAS -> {
                titulo("Números sorteados")
                sorteados(ap.dezenasSorteadas.map { "$it" })
                divisor()
                titulo("Meus números")
                meus(a.numeros.map { "$it" }) { i -> ap.dezenasSorteadas.getOrNull(i) == a.numeros[i] }
                subtitulo("Um número por coluna, da 1ª à 7ª.")
            }
            Formato.LOTECA -> {
                titulo("Resultados")
                for (j in 0 until 14) {
                    val rotulo = a.jogosLoteca.getOrNull(j)?.takeIf { it.isNotBlank() } ?: "Jogo ${j + 1}"
                    val res = ap.resultadosLoteca.getOrNull(j).orEmpty()
                    val palpite = a.palpitesLoteca.getOrNull(j).orEmpty()
                    val ok = res.isNotEmpty() && res == palpite
                    c.drawText("${j + 1}. ${rotulo.take(30)}", MARGEM, y + 40f, p.texto)
                    val cx = LARGURA - MARGEM - 170f
                    if (ok) c.drawCircle(cx, y + 28f, 30f, p.branco) else c.drawCircle(cx, y + 28f, 28f, p.contorno)
                    p.textoNegrito.color = if (ok) cor else Color.WHITE
                    c.drawText(palpite, cx - p.textoNegrito.measureText(palpite) / 2, y + 42f, p.textoNegrito)
                    p.textoNegrito.color = Color.WHITE
                    val r = res.ifEmpty { "?" }
                    c.drawText(r, LARGURA - MARGEM - 40f - p.textoNegrito.measureText(r) / 2, y + 42f, p.textoNegrito)
                    y += 78f
                }
                c.drawText("meu palpite", LARGURA - MARGEM - 170f - p.pequeno.measureText("meu palpite") / 2, y + 30f, p.pequeno)
                c.drawText("resultado", LARGURA - MARGEM - 40f - p.pequeno.measureText("resultado") / 2, y + 30f, p.pequeno)
                y += 70f
            }
            Formato.FEDERAL -> {
                titulo("Bilhetes premiados")
                val meu = a.bilheteFederal.orEmpty().filter(Char::isDigit).padStart(6, '0').takeLast(6)
                ap.premiosFederal.forEachIndexed { i, b ->
                    linhaTexto("${i + 1}º prêmio:", b, destaque = b.takeLast(6) == meu)
                }
                divisor()
                titulo("Meu bilhete")
                linhaTexto("Bilhete:", a.bilheteFederal.orEmpty())
                linhaTexto("Frações:", "${a.fracoesFederal ?: 10} de 10")
            }
        }
        y += 40f
        val altura = y.toInt().coerceIn(200, ALTURA_MAXIMA)
        val final = Bitmap.createBitmap(bmp, 0, 0, LARGURA, altura)
        if (final !== bmp) bmp.recycle()
        return final
    }

    fun salvar(context: Context, a: Aposta, ap: Apuracao, destino: File): File {
        val bmp = gerar(context, a, ap)
        try {
            destino.parentFile?.mkdirs()
            destino.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bmp.recycle()
        }
        return destino
    }
}

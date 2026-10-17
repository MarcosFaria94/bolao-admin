package io.github.marcosfaria94.bolao.ui.apuracao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import java.io.File

/**
 * Imagem da apuração: números sorteados e números apostados, com os acertos marcados.
 * É a imagem que vai anexada à mensagem APURAÇÃO.
 */
object ImagemApuracao {
    private const val LARGURA = 1080
    private const val MARGEM = 56f
    private const val ESPACO = 14f

    private val PRETO = Color.rgb(27, 29, 28)
    private val CINZA = Color.rgb(112, 121, 115)
    private val CINZA_CLARO = Color.rgb(232, 236, 234)

    private fun pincel(tamanho: Float, cor: Int, negrito: Boolean = false, centro: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamanho
        color = cor
        typeface = if (negrito) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        if (centro) textAlign = Paint.Align.CENTER
    }

    fun gerar(a: Aposta, ap: Apuracao): Bitmap {
        val m = a.modalidade
        val cor = m.cor.toInt()
        val bmp = Bitmap.createBitmap(LARGURA, 3200, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        // Cabeçalho na cor da modalidade.
        c.drawRect(0f, 0f, LARGURA.toFloat(), 170f, Paint().apply { color = cor })
        c.drawText("#${a.numero} · ${m.nome} ${a.concurso}", MARGEM, 82f, pincel(52f, Color.WHITE, negrito = true))
        val datas = buildString {
            a.dataSorteio?.let { append("Sorteio ${Datas.curta(it)}") }
            append(if (isEmpty()) "" else " · ")
            append("aposta ${Datas.curta(a.dataAposta)}")
        }
        c.drawText(datas, MARGEM, 138f, pincel(34f, Color.argb(220, 255, 255, 255)))
        var y = 230f

        fun titulo(texto: String) {
            c.drawText(texto, MARGEM, y, pincel(34f, CINZA, negrito = true))
            y += 26f
        }

        /** Bolas em linhas de 10. [cheia] = preenchida na cor; [contorno] = só contorno colorido. */
        fun bolas(itens: List<String>, cheia: (Int) -> Boolean, contorno: (Int) -> Boolean, colunas: Int = 10) {
            val d = (LARGURA - 2 * MARGEM - (colunas - 1) * ESPACO) / colunas
            val texto = pincel(d * 0.42f, PRETO, negrito = true, centro = true)
            itens.chunked(colunas).forEachIndexed { linha, grupo ->
                grupo.forEachIndexed { i, t ->
                    val idx = linha * colunas + i
                    val cx = MARGEM + i * (d + ESPACO) + d / 2
                    val cy = y + d / 2
                    val raio = d / 2
                    when {
                        cheia(idx) -> {
                            c.drawCircle(cx, cy, raio, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cor })
                            texto.color = Color.WHITE
                        }
                        contorno(idx) -> {
                            c.drawCircle(cx, cy, raio, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
                            c.drawCircle(cx, cy, raio - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = cor; style = Paint.Style.STROKE; strokeWidth = 6f
                            })
                            texto.color = cor
                        }
                        else -> {
                            c.drawCircle(cx, cy, raio, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CINZA_CLARO })
                            texto.color = PRETO
                        }
                    }
                    c.drawText(t, cx, cy - (texto.descent() + texto.ascent()) / 2, texto)
                }
                y += d + ESPACO
            }
            y += 30f
        }

        fun linhaTexto(texto: String, tamanho: Float = 36f, cor: Int = PRETO, negrito: Boolean = false) {
            y += tamanho
            c.drawText(texto, MARGEM, y, pincel(tamanho, cor, negrito))
            y += tamanho * 0.5f
        }

        val sorteados = ap.dezenasSorteadas
        when (m.formato) {
            Formato.NUMEROS -> {
                val apostados = a.numeros.sorted()
                if (m == Modalidade.DUPLA_SENA) {
                    titulo("1º sorteio")
                    bolas(sorteados.map { m.rotulo(it) }, cheia = { sorteados[it] in apostados }, contorno = { true })
                    titulo("2º sorteio")
                    bolas(ap.segundoSorteio.map { m.rotulo(it) }, cheia = { ap.segundoSorteio[it] in apostados }, contorno = { true })
                } else {
                    titulo("Sorteados")
                    bolas(sorteados.map { m.rotulo(it) }, cheia = { sorteados[it] in apostados }, contorno = { true })
                }
                titulo("Apostados")
                val todosSorteados = (sorteados + ap.segundoSorteio).toSet()
                bolas(apostados.map { m.rotulo(it) }, cheia = { apostados[it] in todosSorteados }, contorno = { false })
                if (m.trevos > 0) {
                    titulo("Trevos sorteados e apostados")
                    val trevos = ap.trevosSorteados
                    val lista = trevos.map { "$it" } + a.trevos.map { "$it" }
                    bolas(
                        lista,
                        cheia = { i -> if (i < trevos.size) trevos[i] in a.trevos else a.trevos[i - trevos.size] in trevos },
                        contorno = { i -> i < trevos.size },
                        colunas = 6,
                    )
                }
                if (m.extra != Extra.NENHUM) {
                    val nome = if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte" else "Time do Coração"
                    linhaTexto("$nome sorteado: ${ap.extraSorteado ?: "-"}", negrito = true)
                    linhaTexto("Apostado: ${a.extra ?: "-"}", cor = CINZA)
                    y += 20f
                }
            }
            Formato.COLUNAS -> {
                titulo("Sorteado em cada coluna")
                bolas(sorteados.map { "$it" }, cheia = { i -> a.numeros.getOrNull(i) == sorteados[i] }, contorno = { true }, colunas = 7)
                titulo("Apostado")
                bolas(a.numeros.map { "$it" }, cheia = { i -> sorteados.getOrNull(i) == a.numeros[i] }, contorno = { false }, colunas = 7)
            }
            Formato.LOTECA -> {
                titulo("Palpite × resultado")
                y += 10f
                val nomes = pincel(30f, PRETO)
                for (j in 0 until 14) {
                    val rotulo = a.jogosLoteca.getOrNull(j)?.takeIf { it.isNotBlank() } ?: "Jogo ${j + 1}"
                    val res = ap.resultadosLoteca.getOrNull(j).orEmpty()
                    val palpite = a.palpitesLoteca.getOrNull(j).orEmpty()
                    val acerto = res.isNotEmpty() && res == palpite
                    c.drawText("${j + 1}. ${rotulo.take(34)}", MARGEM, y + 34f, nomes)
                    val caixa = RectF(LARGURA - MARGEM - 220f, y, LARGURA - MARGEM - 120f, y + 50f)
                    c.drawRoundRect(caixa, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (acerto) cor else CINZA_CLARO })
                    c.drawText(palpite, caixa.centerX(), y + 36f, pincel(30f, if (acerto) Color.WHITE else PRETO, true, true))
                    c.drawText(res.ifEmpty { "?" }, LARGURA - MARGEM - 50f, y + 36f, pincel(30f, CINZA, true, true))
                    y += 62f
                }
                c.drawText("palpite", LARGURA - MARGEM - 170f, y + 20f, pincel(24f, CINZA, centro = true))
                c.drawText("resultado", LARGURA - MARGEM - 50f, y + 20f, pincel(24f, CINZA, centro = true))
                y += 60f
            }
            Formato.FEDERAL -> {
                linhaTexto("Bilhete ${a.bilheteFederal.orEmpty()} · ${a.fracoesFederal ?: 10} de 10 frações", 40f, PRETO, true)
                y += 20f
                ap.premiosFederal.forEachIndexed { i, b ->
                    val igual = b.takeLast(6) == a.bilheteFederal.orEmpty().padStart(6, '0')
                    val caixa = RectF(MARGEM, y, LARGURA - MARGEM, y + 70f)
                    c.drawRoundRect(caixa, 16f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (igual) cor else CINZA_CLARO })
                    c.drawText("${i + 1}º prêmio", MARGEM + 24f, y + 46f, pincel(34f, if (igual) Color.WHITE else PRETO))
                    c.drawText(b, LARGURA - MARGEM - 24f, y + 48f, pincel(38f, if (igual) Color.WHITE else PRETO, true).apply { textAlign = Paint.Align.RIGHT })
                    y += 86f
                }
                y += 20f
            }
        }

        // Resultado final.
        c.drawLine(MARGEM, y, LARGURA - MARGEM, y, Paint().apply { color = CINZA_CLARO; strokeWidth = 3f })
        y += 20f
        linhaTexto("Acertos: ${ap.acertos}", 42f, PRETO, true)
        linhaTexto(
            if (ap.premio > 0) "Prêmio: ${Dinheiro.rs(ap.premio)}" else "Sem prêmio neste concurso",
            46f,
            if (ap.premio > 0) cor else CINZA,
            true,
        )
        y += 30f
        return Bitmap.createBitmap(bmp, 0, 0, LARGURA, y.toInt().coerceAtMost(bmp.height))
    }

    fun salvar(context: Context, a: Aposta, bmp: Bitmap): File {
        val arq = File(Compartilhar.pastaTemporaria(context), "apuracao-${a.numero}-${a.modalidade.sigla}-${a.concurso}.png")
        arq.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return arq
    }
}

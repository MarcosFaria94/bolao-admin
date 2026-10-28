package io.github.marcosfaria94.bolao.ui.teste

import android.graphics.Bitmap
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.DadosDeExemplo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.imagem.ImagemConferencia
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import java.io.File
import java.time.LocalDate

/**
 * Imagens dos anexos fictícios do modo de teste (comprovantes de aposta e de Pix).
 * Todas trazem "EXEMPLO – não é um comprovante real" bem visível.
 */
object ImagensDeExemplo {
    private const val LARGURA = 900
    private const val ALTURA = 1200
    private const val MARGEM = 56f

    private val PRETO = Color.rgb(27, 29, 28)
    private val CINZA = Color.rgb(112, 121, 115)
    private val VERDE_PIX = Color.rgb(11, 110, 79)

    private fun pincel(tamanho: Float, cor: Int, negrito: Boolean = false, centro: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamanho
        color = cor
        typeface = if (negrito) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        if (centro) textAlign = Paint.Align.CENTER
    }

    /** Desenha todos os anexos fictícios que o estado de exemplo usa. */
    fun desenharTodos(context: Context, pasta: File, e: Estado) {
        e.apostas.forEach { a ->
            val c = a.comprovante ?: return@forEach
            if (c.nome.startsWith(DadosDeExemplo.PREFIXO)) comprovanteAposta(File(pasta, c.nome), a)
        }
        e.rateios.forEach { r ->
            r.comprovantes.forEachIndexed { i, arq ->
                if (arq.nome.startsWith(DadosDeExemplo.PREFIXO)) {
                    val p = e.pessoas.getOrNull(i)
                    pix(File(pasta, arq.nome), p?.apelido ?: "Integrante ${i + 1}", p?.chavePix.orEmpty(), r.cota, r.data)
                }
            }
            // Pix já anexados de um rateio esperando a conferência.
            r.pix.forEach { (id, arq) ->
                if (arq.nome.startsWith(DadosDeExemplo.PREFIXO)) {
                    val p = e.pessoa(id)
                    pix(File(pasta, arq.nome), p?.apelido ?: "Integrante", p?.chavePix.orEmpty(), r.cota, r.data)
                }
            }
        }
        // Imagens dos acertos das apurações (as mesmas que o app gera ao apurar).
        e.comprovantes.filter { it.tipo == TipoComprovante.APURACAO }.forEach { c ->
            val a = e.apostas.firstOrNull { it.id == c.apostaId } ?: return@forEach
            val ap = a.apuracao ?: return@forEach
            c.anexos.filter { it.nome.startsWith(DadosDeExemplo.PREFIXO) }.forEach { arq ->
                ImagemConferencia.salvar(context, a, ap, File(pasta, arq.nome))
            }
        }
    }

    private fun linhasDaAposta(a: Aposta): List<String> = buildList {
        add("Aposta em ${Datas.longa(a.dataAposta)}")
        a.dataSorteio?.let { add("Sorteio: ${Datas.longa(it)}") }
        add("Valor: ${Dinheiro.rs(a.valor)}")
        add("")
        addAll(linhasDoPalpite(a))
    }

    /** Desenha o comprovante fictício da aposta [a] e devolve o texto dele (o que o reconhecimento de texto leria). */
    fun comprovanteAposta(destino: File, a: Aposta): String {
        val m = a.modalidade
        val linhas = linhasDaAposta(a)
        desenhar(destino, m.cor.toInt(), "${m.nome} ${a.concurso}", "Comprovante de aposta", linhas)
        return texto("${m.nome} ${a.concurso}", "Comprovante de aposta", linhas)
    }

    private fun linhasDoPix(apelido: String, chave: String, valor: Long, data: LocalDate) = listOf(
        "Valor: ${Dinheiro.rs(valor)}",
        "Para: $apelido",
        "Chave: ${chave.ifBlank { "-" }}",
        "Data: ${Datas.longa(data)}",
    )

    /** Desenha o comprovante fictício de um Pix e devolve o texto dele. */
    fun pix(destino: File, apelido: String, chave: String, valor: Long, data: LocalDate): String {
        val linhas = linhasDoPix(apelido, chave, valor, data)
        desenhar(destino, VERDE_PIX, "Pix de ${Dinheiro.rs(valor)}", "Comprovante de Pix", linhas)
        return texto("Pix de ${Dinheiro.rs(valor)}", "Comprovante de Pix", linhas)
    }

    /** O texto do desenho, como o reconhecimento de texto leria (o modo de teste não precisa ler as próprias imagens). */
    private fun texto(titulo: String, subtitulo: String, linhas: List<String>): String =
        (listOf("EXEMPLO", "Não é um comprovante real", titulo, subtitulo) + linhas.filter { it.isNotEmpty() } +
            "Criado pelo modo de teste do Bolão dos Amigos").joinToString("\n")

    private fun linhasDoPalpite(a: Aposta): List<String> {
        val m = a.modalidade
        return when (m.formato) {
            Formato.NUMEROS -> buildList {
                a.numeros.chunked(10).forEachIndexed { i, grupo ->
                    add((if (i == 0) "Números: " else "") + grupo.joinToString(" ") { m.rotulo(it) })
                }
                if (a.trevos.isNotEmpty()) add("Trevos: ${a.trevos.joinToString(" ")}")
                a.extra?.let { add((if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte: " else "Time do Coração: ") + it) }
            }
            Formato.COLUNAS -> listOf("Colunas: ${a.numeros.joinToString(" ")}")
            Formato.LOTECA -> a.palpitesLoteca.chunked(7).mapIndexed { i, g -> (if (i == 0) "Palpites: " else "") + g.joinToString(" ") }
            Formato.FEDERAL -> listOf("Bilhete: ${a.bilheteFederal.orEmpty()}", "Frações: ${a.fracoesFederal ?: 10} de 10")
        }
    }

    private fun desenhar(destino: File, cor: Int, titulo: String, subtitulo: String, linhas: List<String>) {
        val bmp = Bitmap.createBitmap(LARGURA, ALTURA, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        // Marca d'água em diagonal.
        c.save()
        c.rotate(-30f, LARGURA / 2f, ALTURA / 2f)
        c.drawText("EXEMPLO", LARGURA / 2f, ALTURA / 2f + 60f, pincel(190f, Color.argb(26, 0, 0, 0), negrito = true, centro = true))
        c.restore()

        // Faixa de aviso no topo.
        c.drawRect(0f, 0f, LARGURA.toFloat(), 190f, Paint().apply { color = cor })
        c.drawText("EXEMPLO", MARGEM, 100f, pincel(70f, Color.WHITE, negrito = true))
        c.drawText("Não é um comprovante real", MARGEM, 154f, pincel(34f, Color.argb(230, 255, 255, 255)))

        var y = 290f
        c.drawText(titulo, MARGEM, y, pincel(50f, PRETO, negrito = true))
        y += 56f
        c.drawText(subtitulo, MARGEM, y, pincel(32f, CINZA))
        y += 84f
        val texto = pincel(34f, PRETO)
        for (linha in linhas) {
            if (linha.isNotEmpty()) c.drawText(linha, MARGEM, y, texto)
            y += 52f
        }

        c.drawText("Criado pelo modo de teste do Bolão dos Amigos", MARGEM, ALTURA - 56f, pincel(28f, CINZA))
        destino.parentFile?.mkdirs()
        destino.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }
}

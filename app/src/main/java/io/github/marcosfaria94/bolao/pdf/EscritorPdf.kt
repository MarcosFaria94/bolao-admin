package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.imagem.PngIndexado
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Locale
import java.util.zip.Deflater

/**
 * Escritor de PDF simples e próprio (sem bibliotecas): páginas com texto nas fontes padrão
 * do PDF (Helvetica e Courier, com acentos), retângulos, linhas e imagens. As imagens JPEG
 * entram como estão e os comprovantes do app (PNG com paleta) entram sem decodificar,
 * então o arquivo fica leve. Coordenadas em pontos (1/72 pol.), com a origem no canto de
 * cima à esquerda e o y crescendo para baixo.
 */
class EscritorPdf {

    enum class Fonte(val nomeBase: String) {
        HELVETICA("Helvetica"),
        HELVETICA_NEGRITO("Helvetica-Bold"),
        HELVETICA_ITALICO("Helvetica-Oblique"),
        COURIER("Courier"),
        COURIER_NEGRITO("Courier-Bold"),
    }

    class Imagem internal constructor(internal val numero: Int, val largura: Int, val altura: Int)

    private class Obj(val numero: Int, var dados: ByteArray = ByteArray(0))

    private val objetos = mutableListOf<Obj>()
    private val paginas = mutableListOf<Pagina>()
    private val fontes = LinkedHashMap<Fonte, Int>()
    var titulo: String = ""
    var assunto: String = ""

    private fun novoObjeto(): Obj = Obj(objetos.size + 3).also { objetos += it }

    inner class Pagina internal constructor(val largura: Float, val altura: Float) {
        internal val conteudo = StringBuilder()
        internal val imagens = LinkedHashSet<Int>()
        internal val fontesUsadas = LinkedHashSet<Fonte>()

        private fun n(v: Float): String = String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
        private fun cor(rgb: Int): String =
            "${n((rgb shr 16 and 0xFF) / 255f)} ${n((rgb shr 8 and 0xFF) / 255f)} ${n((rgb and 0xFF) / 255f)}"

        /** Texto com a linha de base em [y] (medido de cima). */
        fun texto(x: Float, y: Float, tamanho: Float, fonte: Fonte, texto: String, corRgb: Int = 0x000000) {
            if (texto.isEmpty()) return
            fontesUsadas += fonte
            val id = fontes.getOrPut(fonte) { fontes.size + 1 }
            conteudo.append("BT ${cor(corRgb)} rg /F$id ${n(tamanho)} Tf 1 0 0 1 ${n(x)} ${n(altura - y)} Tm ")
                .append(literal(texto)).append(" Tj ET\n")
        }

        fun retangulo(x: Float, y: Float, w: Float, h: Float, preenchimento: Int? = null, contorno: Int? = null, espessura: Float = 0.75f) {
            val r = "${n(x)} ${n(altura - y - h)} ${n(w)} ${n(h)} re"
            when {
                preenchimento != null && contorno != null ->
                    conteudo.append("${cor(preenchimento)} rg ${cor(contorno)} RG ${n(espessura)} w $r B\n")
                preenchimento != null -> conteudo.append("${cor(preenchimento)} rg $r f\n")
                contorno != null -> conteudo.append("${cor(contorno)} RG ${n(espessura)} w $r S\n")
            }
        }

        fun linha(x1: Float, y1: Float, x2: Float, y2: Float, corRgb: Int = 0x000000, espessura: Float = 0.75f) {
            conteudo.append("${cor(corRgb)} RG ${n(espessura)} w ${n(x1)} ${n(altura - y1)} m ${n(x2)} ${n(altura - y2)} l S\n")
        }

        /** Imagem ocupando o retângulo (x, y, w, h). */
        fun imagem(img: Imagem, x: Float, y: Float, w: Float, h: Float) {
            imagens += img.numero
            conteudo.append("q ${n(w)} 0 0 ${n(h)} ${n(x)} ${n(altura - y - h)} cm /Im${img.numero} Do Q\n")
        }
    }

    /** A4 por padrão. */
    fun pagina(largura: Float = A4_LARGURA, altura: Float = A4_ALTURA): Pagina = Pagina(largura, altura).also { paginas += it }

    val quantidadePaginas: Int get() = paginas.size

    val todasAsPaginas: List<Pagina> get() = paginas.toList()

    /** JPEG embutido como está (DCTDecode). */
    fun jpeg(bytes: ByteArray): Imagem {
        val d = Jpeg.dimensoes(bytes) ?: throw IllegalArgumentException("JPEG inválido.")
        val o = novoObjeto()
        val espaco = if (d.componentes == 1) "/DeviceGray" else if (d.componentes == 4) "/DeviceCMYK" else "/DeviceRGB"
        o.dados = stream(
            "<< /Type /XObject /Subtype /Image /Width ${d.largura} /Height ${d.altura} /ColorSpace $espaco " +
                "/BitsPerComponent 8 /Filter /DCTDecode",
            bytes,
        )
        return Imagem(o.numero, d.largura, d.altura)
    }

    /** PNG com paleta (os comprovantes do app), sem decodificar: FlateDecode com preditor PNG. */
    fun pngIndexado(p: PngIndexado.ParaPdf): Imagem {
        val o = novoObjeto()
        val cores = p.paletaRgb.size / 3
        val paleta = p.paletaRgb.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
        o.dados = stream(
            "<< /Type /XObject /Subtype /Image /Width ${p.largura} /Height ${p.altura} " +
                "/ColorSpace [/Indexed /DeviceRGB ${cores - 1} <$paleta>] /BitsPerComponent ${p.bits} " +
                "/Filter /FlateDecode /DecodeParms << /Predictor 15 /Colors 1 /BitsPerComponent ${p.bits} /Columns ${p.largura} >>",
            p.idat,
        )
        return Imagem(o.numero, p.largura, p.altura)
    }

    private fun stream(dicionarioSemFechar: String, dados: ByteArray): ByteArray {
        val b = ByteArrayOutputStream()
        b.write("$dicionarioSemFechar /Length ${dados.size} >>\nstream\n".toByteArray(Charsets.ISO_8859_1))
        b.write(dados)
        b.write("\nendstream".toByteArray(Charsets.ISO_8859_1))
        return b.toByteArray()
    }

    private fun comprimir(dados: ByteArray): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION)
        d.setInput(dados)
        d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(32 * 1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return out.toByteArray()
    }

    fun gravar(saida: OutputStream) {
        // Objetos fixos: 1 = catálogo, 2 = árvore de páginas. Fontes, páginas e conteúdos vêm depois.
        val numerosFontes = fontes.keys.associateWith { f ->
            novoObjeto().also { it.dados = "<< /Type /Font /Subtype /Type1 /BaseFont /${f.nomeBase} /Encoding /WinAnsiEncoding >>".toByteArray(Charsets.ISO_8859_1) }.numero
        }
        val numerosPaginas = paginas.map { pg ->
            val conteudo = novoObjeto()
            conteudo.dados = stream("<< /Filter /FlateDecode", comprimir(pg.conteudo.toString().toByteArray(Charsets.ISO_8859_1)))
            val pag = novoObjeto()
            val fontesRef = pg.fontesUsadas.joinToString(" ") { "/F${fontes.getValue(it)} ${numerosFontes.getValue(it)} 0 R" }
            val imagensRef = pg.imagens.joinToString(" ") { "/Im$it $it 0 R" }
            pag.dados = (
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${fmt(pg.largura)} ${fmt(pg.altura)}] " +
                    "/Resources << /Font << $fontesRef >> /XObject << $imagensRef >> >> /Contents ${conteudo.numero} 0 R >>"
                ).toByteArray(Charsets.ISO_8859_1)
            pag.numero
        }
        val info = novoObjeto()
        info.dados = ("<< /Title ${textoUnicode(titulo)} /Subject ${textoUnicode(assunto)} /Producer ${textoUnicode("Bolão dos Amigos")} >>")
            .toByteArray(Charsets.ISO_8859_1)

        val todos = listOf(
            Obj(1, "<< /Type /Catalog /Pages 2 0 R >>".toByteArray(Charsets.ISO_8859_1)),
            Obj(2, "<< /Type /Pages /Kids [${numerosPaginas.joinToString(" ") { "$it 0 R" }}] /Count ${numerosPaginas.size} >>".toByteArray(Charsets.ISO_8859_1)),
        ) + objetos
        val porNumero = todos.sortedBy { it.numero }
        val b = ByteArrayOutputStream()
        b.write("%PDF-1.4\n%".toByteArray(Charsets.ISO_8859_1))
        b.write(byteArrayOf(0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))
        val posicoes = IntArray(porNumero.size + 1)
        for (o in porNumero) {
            posicoes[o.numero] = b.size()
            b.write("${o.numero} 0 obj\n".toByteArray(Charsets.ISO_8859_1))
            b.write(o.dados)
            b.write("\nendobj\n".toByteArray(Charsets.ISO_8859_1))
        }
        val xref = b.size()
        val sb = StringBuilder()
        sb.append("xref\n0 ${porNumero.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        for (i in 1..porNumero.size) sb.append(String.format(Locale.US, "%010d 00000 n \n", posicoes[i]))
        sb.append("trailer\n<< /Size ${porNumero.size + 1} /Root 1 0 R /Info ${info.numero} 0 R >>\nstartxref\n$xref\n%%EOF\n")
        b.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        saida.write(b.toByteArray())
        saida.flush()
    }

    private fun fmt(v: Float): String = String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')

    companion object {
        const val A4_LARGURA = 595.28f
        const val A4_ALTURA = 841.89f

        private val WIN_ANSI: Charset = Charset.forName("windows-1252")

        /** Troca o que não existe no WinAnsi (fontes padrão do PDF) por equivalentes. */
        fun normalizar(t: String): String = buildString {
            for (ch in t) {
                append(
                    when (ch) {
                        '−' -> '-'
                        ' ', ' ', ' ' -> ' '
                        '→' -> '>'
                        '✓', '✔' -> 'v'
                        else -> ch
                    },
                )
            }
        }.let { s ->
            val codificador = WIN_ANSI.newEncoder()
            s.filter { c -> c == '\n' || codificador.canEncode(c) }
        }

        /** String literal do PDF em WinAnsi, com escapes. */
        fun literal(t: String): String {
            val bytes = normalizar(t).toByteArray(WIN_ANSI)
            val sb = StringBuilder("(")
            for (b in bytes) {
                val v = b.toInt() and 0xFF
                when {
                    v == '('.code || v == ')'.code || v == '\\'.code -> sb.append('\\').append(v.toChar())
                    v < 32 || v > 126 -> sb.append('\\').append(Integer.toOctalString(v).padStart(3, '0'))
                    else -> sb.append(v.toChar())
                }
            }
            return sb.append(")").toString()
        }

        /** Texto do dicionário de informações em UTF-16 (aceita qualquer caractere). */
        fun textoUnicode(t: String): String =
            "<FEFF" + t.toByteArray(Charsets.UTF_16BE).joinToString("") { "%02X".format(it.toInt() and 0xFF) } + ">"
    }
}

/** Dimensões de um JPEG lidas do cabeçalho (marcador SOF). */
object Jpeg {
    data class Dimensoes(val largura: Int, val altura: Int, val componentes: Int)

    fun dimensoes(b: ByteArray): Dimensoes? {
        if (b.size < 4 || b[0] != 0xFF.toByte() || b[1] != 0xD8.toByte()) return null
        var i = 2
        while (i + 9 < b.size) {
            if (b[i] != 0xFF.toByte()) {
                i++
                continue
            }
            val marcador = b[i + 1].toInt() and 0xFF
            if (marcador == 0xD8 || marcador == 0x01 || marcador in 0xD0..0xD7 || marcador == 0xFF) {
                i++
                continue
            }
            val tamanho = ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)
            if (marcador in 0xC0..0xCF && marcador != 0xC4 && marcador != 0xC8 && marcador != 0xCC) {
                val altura = ((b[i + 5].toInt() and 0xFF) shl 8) or (b[i + 6].toInt() and 0xFF)
                val largura = ((b[i + 7].toInt() and 0xFF) shl 8) or (b[i + 8].toInt() and 0xFF)
                val comp = b[i + 9].toInt() and 0xFF
                return Dimensoes(largura, altura, comp)
            }
            i += 2 + tamanho
        }
        return null
    }
}

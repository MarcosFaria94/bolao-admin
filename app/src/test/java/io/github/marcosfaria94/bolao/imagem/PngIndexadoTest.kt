package io.github.marcosfaria94.bolao.imagem

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decodificador de PNG com paleta só para o teste (independente do codificador): confere o
 * CRC de cada bloco, descomprime, desfaz os filtros e devolve as cores dos pontos.
 */
private fun decodificar(png: ByteArray): Triple<Int, Int, IntArray> {
    val assinatura = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    assertArrayEquals(assinatura, png.copyOfRange(0, 8))
    fun int(i: Int) = ((png[i].toInt() and 0xFF) shl 24) or ((png[i + 1].toInt() and 0xFF) shl 16) or
        ((png[i + 2].toInt() and 0xFF) shl 8) or (png[i + 3].toInt() and 0xFF)
    var pos = 8
    var largura = 0
    var altura = 0
    var bits = 0
    var paleta = IntArray(0)
    val idat = java.io.ByteArrayOutputStream()
    while (pos < png.size) {
        val n = int(pos)
        val tipo = String(png, pos + 4, 4, Charsets.US_ASCII)
        val crc = java.util.zip.CRC32().apply { update(png, pos + 4, 4 + n) }
        assertEquals("CRC do bloco $tipo", crc.value.toInt(), int(pos + 8 + n))
        val ini = pos + 8
        when (tipo) {
            "IHDR" -> {
                largura = int(ini)
                altura = int(ini + 4)
                bits = png[ini + 8].toInt()
                assertEquals(3, png[ini + 9].toInt())
                assertEquals(0, png[ini + 12].toInt())
            }
            "PLTE" -> paleta = IntArray(n / 3) { k ->
                ((png[ini + k * 3].toInt() and 0xFF) shl 16) or ((png[ini + k * 3 + 1].toInt() and 0xFF) shl 8) or (png[ini + k * 3 + 2].toInt() and 0xFF)
            }
            "IDAT" -> idat.write(png, ini, n)
        }
        pos = ini + n + 4
        if (tipo == "IEND") break
    }
    val porLinha = (largura * bits + 7) / 8
    val dados = ByteArray((porLinha + 1) * altura)
    val inf = java.util.zip.Inflater().apply { setInput(idat.toByteArray()) }
    var lidos = 0
    while (lidos < dados.size && !inf.finished()) lidos += inf.inflate(dados, lidos, dados.size - lidos)
    assertEquals(dados.size, lidos)
    val pixels = IntArray(largura * altura)
    var anterior = IntArray(porLinha)
    for (y in 0 until altura) {
        val filtro = dados[y * (porLinha + 1)].toInt()
        val linha = IntArray(porLinha)
        for (i in 0 until porLinha) {
            val bruto = dados[y * (porLinha + 1) + 1 + i].toInt() and 0xFF
            val a = if (i > 0) linha[i - 1] else 0
            val b = anterior[i]
            val c = if (i > 0) anterior[i - 1] else 0
            linha[i] = when (filtro) {
                0 -> bruto
                1 -> bruto + a
                2 -> bruto + b
                3 -> bruto + (a + b) / 2
                4 -> {
                    val p = a + b - c
                    val pa = kotlin.math.abs(p - a)
                    val pb = kotlin.math.abs(p - b)
                    val pc = kotlin.math.abs(p - c)
                    bruto + if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
                }
                else -> error("Filtro $filtro")
            } and 0xFF
        }
        for (x in 0 until largura) {
            val bit = x * bits
            val desloc = 8 - bits - (bit % 8)
            val indice = (linha[bit / 8] shr desloc) and ((1 shl bits) - 1)
            pixels[y * largura + x] = paleta[indice]
        }
        anterior = linha
    }
    return Triple(largura, altura, pixels)
}

class PngIndexadoTest {

    private val paleta = intArrayOf(0xFFFFFF, 0xFBF0A6, 0x2C2924, 0xE2DCC0)

    private fun xadrez(l: Int, a: Int, cores: Int): PngIndexado.Imagem {
        val idx = ByteArray(l * a) { i -> ((i % l / 3 + i / l / 2) % cores).toByte() }
        return PngIndexado.Imagem(l, a, idx, paleta.copyOf(cores).map { it }.toIntArray())
    }

    @Test
    fun profundidadePelaQuantidadeDeCores() {
        assertEquals(1, PngIndexado.bitsPorPixel(2))
        assertEquals(2, PngIndexado.bitsPorPixel(3))
        assertEquals(2, PngIndexado.bitsPorPixel(4))
        assertEquals(4, PngIndexado.bitsPorPixel(5))
        assertEquals(8, PngIndexado.bitsPorPixel(17))
    }

    @Test
    fun codificaEDecodificaPixelAPixel() {
        // Larguras que não fecham o byte (2 bits × 37 = 74 bits) e linhas repetidas (filtro "up").
        for ((l, cores) in listOf(37 to 4, 64 to 2, 13 to 3)) {
            val img = xadrez(l, 23, cores)
            val png = PngIndexado.codificar(img)
            val (largura, altura, pixels) = decodificar(png)
            assertEquals(l, largura)
            assertEquals(23, altura)
            for (y in 0 until 23) for (x in 0 until l) {
                val esperado = img.paleta[img.indices[y * l + x].toInt()]
                assertEquals("pixel $x,$y", esperado, pixels[y * l + x])
            }
        }
    }

    @Test
    fun ampliadaRepeteOsPixels() {
        val img = PngIndexado.Imagem(2, 1, byteArrayOf(0, 2), paleta)
        val a = img.ampliada(3)
        assertEquals(6, a.largura)
        assertEquals(3, a.altura)
        assertArrayEquals(ByteArray(18) { i -> if (i % 6 < 3) 0 else 2 }, a.indices)
    }

    @Test
    fun comprovanteTipicoFicaLevissimo() {
        // Papel de 1152 × 2600 com texto esparso: o PNG de 2 bits precisa ficar bem abaixo de 100 KB.
        val l = 1152
        val a = 2600
        val idx = ByteArray(l * a) { 1 }
        for (y in 0 until a) for (x in 0 until l) {
            if (x < 12 || x >= l - 12) idx[y * l + x] = 0
            else if ((y / 63) % 2 == 0 && (y % 63) in 10..50 && (x / 30) % 3 != 0 && ((x * 7 + y * 13) % 11) < 6) idx[y * l + x] = 2
        }
        val png = PngIndexado.codificar(PngIndexado.Imagem(l, a, idx, paleta))
        assertTrue("PNG com ${png.size} bytes", png.size < 100_000)
    }

    @Test
    fun partesParaOPdf() {
        val img = xadrez(37, 5, 4)
        val p = PngIndexado.lerParaPdf(PngIndexado.codificar(img))!!
        assertEquals(37, p.largura)
        assertEquals(5, p.altura)
        assertEquals(2, p.bits)
        assertEquals(12, p.paletaRgb.size)
        assertEquals(0xFB.toByte(), p.paletaRgb[3])
        // Os dados descomprimidos têm 1 byte de filtro + 10 bytes por linha.
        val inf = java.util.zip.Inflater().apply { setInput(p.idat) }
        val buf = ByteArray(1000)
        val n = inf.inflate(buf)
        assertEquals(5 * 11, n)
        assertNull(PngIndexado.lerParaPdf(byteArrayOf(1, 2, 3)))
    }
}

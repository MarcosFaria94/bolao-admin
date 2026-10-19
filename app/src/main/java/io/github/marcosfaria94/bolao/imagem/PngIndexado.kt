package io.github.marcosfaria94.bolao.imagem

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Codificador de PNG com paleta (poucas cores), para os comprovantes do app ficarem levíssimos:
 * um comprovante térmico tem 4 cores, então cada pixel ocupa 2 bits e as linhas repetidas
 * comprimem quase a zero. Sem dependência do Android (dá para testar no computador).
 */
object PngIndexado {

    /** Imagem com paleta: [indices] tem um byte por pixel (índice na [paleta], cores 0xRRGGBB). */
    class Imagem(val largura: Int, val altura: Int, val indices: ByteArray, val paleta: IntArray) {
        init {
            require(largura > 0 && altura > 0) { "Imagem vazia." }
            require(indices.size == largura * altura) { "Tamanho dos índices não confere." }
            require(paleta.size in 1..256) { "Paleta precisa ter de 1 a 256 cores." }
        }

        /** Cópia ampliada [fator] vezes, pixel a pixel (sem suavizar: fica com cara de impressão). */
        fun ampliada(fator: Int): Imagem {
            if (fator == 1) return this
            val l = largura * fator
            val novo = ByteArray(l * altura * fator)
            for (y in 0 until altura) {
                val linha = ByteArray(l)
                for (x in 0 until largura) {
                    val v = indices[y * largura + x]
                    for (k in 0 until fator) linha[x * fator + k] = v
                }
                for (k in 0 until fator) System.arraycopy(linha, 0, novo, (y * fator + k) * l, l)
            }
            return Imagem(l, altura * fator, novo, paleta)
        }
    }

    fun bitsPorPixel(cores: Int): Int = when {
        cores <= 2 -> 1
        cores <= 4 -> 2
        cores <= 16 -> 4
        else -> 8
    }

    /** Linhas já com o byte de filtro na frente, empacotadas na profundidade de [bits]. */
    private fun linhasFiltradas(img: Imagem, bits: Int): ByteArray {
        val porLinha = (img.largura * bits + 7) / 8
        val saida = ByteArrayOutputStream(img.altura * (porLinha + 1))
        var anterior = ByteArray(porLinha)
        val atual = ByteArray(porLinha)
        val sub = ByteArray(porLinha)
        for (y in 0 until img.altura) {
            atual.fill(0)
            var bit = 0
            for (x in 0 until img.largura) {
                val v = img.indices[y * img.largura + x].toInt() and 0xFF
                val byte = bit / 8
                val desloc = 8 - bits - (bit % 8)
                atual[byte] = (atual[byte].toInt() or (v shl desloc)).toByte()
                bit += bits
            }
            // Filtro "up" (1 linha igual à anterior vira só zeros) ou nenhum, o que somar menos.
            var somaNada = 0L
            var somaUp = 0L
            for (i in 0 until porLinha) {
                somaNada += (atual[i].toInt() and 0xFF).let { if (it > 127) 256 - it else it }
                val d = (atual[i] - anterior[i]).toByte()
                sub[i] = d
                somaUp += (d.toInt() and 0xFF).let { if (it > 127) 256 - it else it }
            }
            if (somaUp < somaNada) {
                saida.write(2)
                saida.write(sub, 0, porLinha)
            } else {
                saida.write(0)
                saida.write(atual, 0, porLinha)
            }
            anterior = atual.copyOf()
        }
        return saida.toByteArray()
    }

    private fun comprimir(dados: ByteArray): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION)
        d.setInput(dados)
        d.finish()
        val saida = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        while (!d.finished()) {
            val n = d.deflate(buf)
            saida.write(buf, 0, n)
        }
        d.end()
        return saida.toByteArray()
    }

    private fun bloco(saida: DataOutputStream, tipo: String, dados: ByteArray) {
        val t = tipo.encodeToByteArray()
        saida.writeInt(dados.size)
        saida.write(t)
        saida.write(dados)
        val crc = CRC32()
        crc.update(t)
        crc.update(dados)
        saida.writeInt(crc.value.toInt())
    }

    fun codificar(img: Imagem): ByteArray {
        val bits = bitsPorPixel(img.paleta.size)
        val bytes = ByteArrayOutputStream()
        val saida = DataOutputStream(bytes)
        saida.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        val ihdr = ByteArrayOutputStream().also {
            val o = DataOutputStream(it)
            o.writeInt(img.largura)
            o.writeInt(img.altura)
            o.writeByte(bits)
            o.writeByte(3) // cor com paleta
            o.writeByte(0)
            o.writeByte(0)
            o.writeByte(0)
        }.toByteArray()
        bloco(saida, "IHDR", ihdr)
        val plte = ByteArray(img.paleta.size * 3)
        img.paleta.forEachIndexed { i, c ->
            plte[i * 3] = (c shr 16 and 0xFF).toByte()
            plte[i * 3 + 1] = (c shr 8 and 0xFF).toByte()
            plte[i * 3 + 2] = (c and 0xFF).toByte()
        }
        bloco(saida, "PLTE", plte)
        bloco(saida, "IDAT", comprimir(linhasFiltradas(img, bits)))
        bloco(saida, "IEND", ByteArray(0))
        saida.flush()
        return bytes.toByteArray()
    }

    /** Partes de um PNG com paleta, para embutir no PDF sem decodificar (FlateDecode + preditor PNG). */
    class ParaPdf(val largura: Int, val altura: Int, val bits: Int, val paletaRgb: ByteArray, val idat: ByteArray)

    /** Lê um PNG com paleta gerado por [codificar] (ou outro PNG tipo 3, sem entrelaçamento). null = outro tipo. */
    fun lerParaPdf(png: ByteArray): ParaPdf? {
        if (png.size < 8 || png[1] != 0x50.toByte() || png[2] != 0x4E.toByte()) return null
        var pos = 8
        var largura = 0
        var altura = 0
        var bits = 0
        var paleta: ByteArray? = null
        val idat = ByteArrayOutputStream()
        fun int(i: Int) = ((png[i].toInt() and 0xFF) shl 24) or ((png[i + 1].toInt() and 0xFF) shl 16) or
            ((png[i + 2].toInt() and 0xFF) shl 8) or (png[i + 3].toInt() and 0xFF)
        while (pos + 8 <= png.size) {
            val tamanho = int(pos)
            val tipo = String(png, pos + 4, 4, Charsets.US_ASCII)
            val ini = pos + 8
            if (ini + tamanho > png.size) return null
            when (tipo) {
                "IHDR" -> {
                    largura = int(ini)
                    altura = int(ini + 4)
                    bits = png[ini + 8].toInt()
                    val cor = png[ini + 9].toInt()
                    val entrelacado = png[ini + 12].toInt()
                    if (cor != 3 || entrelacado != 0) return null
                }
                "PLTE" -> paleta = png.copyOfRange(ini, ini + tamanho)
                "IDAT" -> idat.write(png, ini, tamanho)
                "IEND" -> break
            }
            pos = ini + tamanho + 4
        }
        val p = paleta ?: return null
        if (largura <= 0 || altura <= 0) return null
        return ParaPdf(largura, altura, bits, p, idat.toByteArray())
    }
}

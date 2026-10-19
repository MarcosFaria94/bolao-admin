package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.imagem.PngIndexado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

/** Saída opcional para olhar os PDFs gerados: BOLAO_SAIDA=/pasta. */
internal fun salvarParaOlhar(nome: String, bytes: ByteArray) {
    val pasta = System.getenv("BOLAO_SAIDA") ?: return
    File(pasta).mkdirs()
    File(pasta, nome).writeBytes(bytes)
}

/** Foto JPEG de 48 × 72 (xadrez), para os testes: no Android não há ImageIO para criar uma. */
private val JPEG_TESTE = (
    "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAoHBwgHBgoICAgLCgoLDhgQDg0NDh0VFhEYIx8lJCIfIiEmKzcvJik0KSEiMEExNDk7" +
    "Pj4+JS5ESUM8SDc9Pjv/2wBDAQoLCw4NDhwQEBw7KCIoOzs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7" +
    "Ozs7Ozs7Ozv/wAARCABIADADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUF" +
    "BAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVW" +
    "V1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi" +
    "4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAEC" +
    "AxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVm" +
    "Z2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq" +
    "8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDna+iKK+d6WxyfwPO4V9EUV870bB/A87hX0RRXzvRsH8DzuFfRFFfO9GwfwPO4V9EUV870" +
    "bB/A87hX0RRXzvRsH8DzuFfRFFfO9GwfwPO4V9EUV870bB/A87hX0RRXzvRsH8DzuFfRFFfO9GwfwPO4V9EUV870bB/A87hX0RRX" +
    "zvRsH8DzuFfRFFFCDDdT53r6IoooQYbqfO9fRFFFCDDdT//Z"
    )

internal fun jpegDeTeste(): ByteArray = java.util.Base64.getDecoder().decode(JPEG_TESTE)

class EscritorPdfTest {

    /** Confere a tabela xref: cada entrada aponta para "N 0 obj". */
    private fun conferirXref(pdf: ByteArray) {
        val texto = String(pdf, Charsets.ISO_8859_1)
        val inicio = texto.substringAfterLast("startxref\n").substringBefore("\n").trim().toInt()
        assertTrue(texto.startsWith("xref", inicio))
        val linhas = texto.substring(inicio).split("\n")
        val total = linhas[1].split(" ")[1].toInt()
        for (n in 1 until total) {
            val pos = linhas[2 + n].substring(0, 10).toInt()
            assertTrue("objeto $n", texto.startsWith("$n 0 obj", pos))
        }
    }

    @Test
    fun textoComAcentosEEscapes() {
        assertEquals("(Bol\\343o \\(teste\\) \\\\ \\247)", EscritorPdf.literal("Bolão (teste) \\ §"))
        assertEquals("Saldo - R$ 1 > 2 v", EscritorPdf.normalizar("Saldo − R$ 1 → 2 ✓"))
        assertEquals("Trevo  ok", EscritorPdf.normalizar("Trevo 🍀 ok"))
        assertEquals("<FEFF0042006F006C00E3006F>", EscritorPdf.textoUnicode("Bolão"))
    }

    @Test
    fun larguraDasFontes() {
        // "Bolão" em Helvetica 10: B 667 + o 556 + l 222 + ã 556 + o 556 = 2557 → 25,57 pt.
        assertEquals(25.57f, Tipografia.largura("Bolão", EscritorPdf.Fonte.HELVETICA, 10f), 0.001f)
        assertEquals(30f, Tipografia.largura("Bolão", EscritorPdf.Fonte.COURIER, 10f), 0.001f)
        // "um dois três" = 13,89 + 2,78 + 20,56 + 2,78 + 18,34 = 58,35 pt: cabe em 60; "quatro" vai para a linha de baixo.
        val l = Tipografia.quebrar(Tipografia.trechos("um **dois três** quatro cinco"), 10f, 60f)
        assertEquals(2, l.size)
        assertEquals(listOf("um ", "dois três"), l[0].map { it.texto })
        assertEquals(listOf(EscritorPdf.Fonte.HELVETICA, EscritorPdf.Fonte.HELVETICA_NEGRITO), l[0].map { it.fonte })
        assertEquals(listOf("quatro cinco"), l[1].map { it.texto })
        // Asterisco sem par fica como texto.
        assertEquals(listOf(Tipografia.Trecho("5 ** 2", EscritorPdf.Fonte.HELVETICA)), Tipografia.trechos("5 ** 2"))
    }

    @Test
    fun pdfComTextoImagensEVariasPaginas() {
        val doc = Documento(rodape = "Teste")
        doc.pdf.titulo = "Teste do escritor"
        doc.titulo("Título do documento")
        repeat(40) { doc.paragrafo("Parágrafo $it com **negrito** e acentuação: ação, é, ô, ü, ç. ".repeat(3)) }
        doc.codigo((1..30).joinToString("\n") { "Linha $it  = R$ ${it},00" })
        doc.tabela(listOf(listOf("A", "B"), listOf("1", "R$ 2,00"), listOf("3", "R$ 4,00")), listOf(2f, 1f), alinharDireita = setOf(1), destaque = setOf(2))
        doc.imagem(doc.pdf.jpeg(jpegDeTeste()), legenda = "Uma foto")
        val idx = ByteArray(90 * 40) { (it % 4).toByte() }
        val png = PngIndexado.codificar(PngIndexado.Imagem(90, 40, idx, intArrayOf(0xFFFFFF, 0xFBF0A6, 0x2C2924, 0xE2DCC0)))
        doc.imagem(doc.pdf.pngIndexado(PngIndexado.lerParaPdf(png)!!), larguraMax = 200f)
        doc.assinaturas(listOf("Fulano de Tal", "Beto da Silva"))
        val saida = ByteArrayOutputStream()
        doc.gravar(saida)
        val pdf = saida.toByteArray()
        salvarParaOlhar("escritor.pdf", pdf)
        assertEquals(Jpeg.Dimensoes(48, 72, 3), Jpeg.dimensoes(jpegDeTeste()))
        assertTrue(String(pdf, 0, 8, Charsets.ISO_8859_1).startsWith("%PDF-1.4"))
        assertTrue(doc.pdf.quantidadePaginas >= 3)
        conferirXref(pdf)
    }
}

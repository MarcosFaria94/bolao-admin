package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.DadosDeExemplo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.imagem.PngIndexado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.Inflater

class TermoEAuditoriaPdfTest {

    private val fuso = ZoneOffset.ofHours(-3)
    private val hoje = LocalDate.of(2026, 10, 17)
    private val exemplo: Estado by lazy {
        val e = DadosDeExemplo.gerar(hoje, hoje.atTime(9, 0).atZone(fuso).toInstant().toEpochMilli(), fuso, emptyMap())
        DadosDeExemplo.comImpressoesDigitais(e) { "H-" + it.nome }
            .copy(pessoas = e.pessoas.mapIndexed { i, p -> p.copy(cpf = listOf("***.111.222-**", "***.333.444-**", "")[i]) })
    }

    /** Texto de todas as páginas (descomprime os conteúdos). */
    private fun textoDoPdf(pdf: ByteArray): String {
        val s = String(pdf, Charsets.ISO_8859_1)
        val sb = StringBuilder()
        var i = 0
        while (true) {
            val k = s.indexOf("/FlateDecode /Length ", i)
            if (k < 0) break
            val ini = s.indexOf("stream\n", k) + 7
            val tam = s.substring(k + 21).substringBefore(" ").toInt()
            val antes = s.substring(maxOf(0, k - 300), k)
            if (!antes.contains("/Subtype /Image")) {
                val inf = Inflater().apply { setInput(pdf, ini, tam) }
                val buf = ByteArray(2_000_000)
                val n = inf.inflate(buf)
                sb.append(String(buf, 0, n, Charsets.ISO_8859_1))
            }
            i = ini + tam
        }
        return sb.toString()
    }

    @Test
    fun termoCruComOsDadosDoCadastro() {
        val saida = ByteArrayOutputStream()
        TermoPdf.gerar(exemplo, saida)
        val pdf = saida.toByteArray()
        salvarParaOlhar("termo.pdf", pdf)
        val texto = textoDoPdf(pdf)
        assertTrue(texto.contains("(Fulano de Tal \\226 ***.111.222-** \\(Administrador\\))"))
        assertTrue(texto.contains("(Beltrano Souza)"))
        assertTrue(texto.contains("fulano@exemplo.com"))
        assertTrue(texto.contains("Parte 4 \\226 Presta\\347\\343o de Contas"))
        assertTrue(texto.contains("(APURA\\307\\303O N\\272 1 - 04/06/26)"))
    }

    private class Fonte(val faltando: Set<String> = emptySet(), val alterados: Set<String> = emptySet()) : AuditoriaPdf.FonteDeImagens {
        var recibos = 0
        override fun recibo(c: Comprovante): ByteArray {
            recibos++
            val l = 576
            val a = 300 + c.texto.lines().size * 32
            val idx = ByteArray(l * a) { i -> if (i % l < 8 || i % l >= l - 8) 0 else if ((i / l) % 32 in 8..20 && (i % l) % 12 < 7) 2 else 1 }
            return PngIndexado.codificar(PngIndexado.Imagem(l, a, idx, intArrayOf(0xFFFFFF, 0xFBF0A6, 0x2C2924, 0xE2DCC0)))
        }
        override fun paginas(a: Arquivo): List<ByteArray> =
            if (a.nome in faltando) emptyList() else listOf(jpegDeTeste())
        override fun sha256(a: Arquivo): String? = when (a.nome) {
            in faltando -> null
            in alterados -> "0".repeat(64)
            else -> a.sha256
        }
    }

    @Test
    fun auditoriaDeTodoOPeriodo() {
        val e = exemplo
        val fonte = Fonte()
        val saida = ByteArrayOutputStream()
        val de = e.lancamentos.minOf { it.data }
        val r = AuditoriaPdf.gerar(e, de, hoje, fonte, saida, geradoEm = hoje.atTime(10, 0).atZone(fuso).toInstant().toEpochMilli())
        val pdf = saida.toByteArray()
        salvarParaOlhar("auditoria.pdf", pdf)
        assertEquals(e.comprovantes.size, r.comprovantes)
        assertEquals(e.comprovantes.size, fonte.recibos)
        assertEquals(e.comprovantes.sumOf { it.anexos.size }, r.anexos)
        assertTrue(r.problemas.toString(), r.problemas.isEmpty())
        // Capa + resumo, uma página por comprovante (os anexos cabem junto ou na seguinte) e os lançamentos.
        assertTrue(r.paginas >= e.comprovantes.size + 2)
        val texto = textoDoPdf(pdf)
        assertTrue(texto.contains("ntegra"))
        assertTrue(texto.contains("MODO DE TESTE"))
        assertTrue(texto.contains("(Lan\\347amentos do per\\355odo)"))
    }

    @Test
    fun auditoriaApontaAnexoSumidoOuAlterado() {
        val e = exemplo
        val anexos = e.comprovantes.flatMap { it.anexos }
        val sumido = anexos.first().nome
        val alterado = anexos.last().nome
        val r = AuditoriaPdf.gerar(e, hoje.withDayOfMonth(1).minusMonths(1), hoje, Fonte(setOf(sumido), setOf(alterado)), ByteArrayOutputStream())
        assertTrue(r.problemas.toString(), r.problemas.any { it.contains("modificado") })
        assertTrue(r.problemas.toString(), r.problemas.any { it.contains("não encontrado") })
        assertEquals(2, r.problemas.size)
        // Uma corrente adulterada também aparece.
        val adulterado = e.copy(comprovantes = e.comprovantes.mapIndexed { i, c -> if (i == 2) c.copy(texto = c.texto + " ") else c })
        val r2 = AuditoriaPdf.gerar(adulterado, hoje.minusYears(1), hoje, Fonte(), ByteArrayOutputStream())
        assertTrue(r2.problemas.first(), r2.problemas.first().contains("não confere"))
        assertFalse(r2.problemas.any { it.contains("modificado") })
    }
}

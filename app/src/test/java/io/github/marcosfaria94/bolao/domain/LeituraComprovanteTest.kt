package io.github.marcosfaria94.bolao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Leitura do texto do comprovante da Caixa (como o reconhecimento de texto devolve). */
class LeituraComprovanteTest {

    @Test
    fun comprovanteDoModoDeTeste() {
        // Texto do comprovante fictício do modo de teste (ImagensDeExemplo).
        val texto = """
            EXEMPLO
            Não é um comprovante real
            Lotofácil 3505
            Comprovante de aposta
            Aposta em 19/10/2026
            Sorteio: 20/10/2026
            Valor: R$ 3,50
            Números: 01 02 03 05 08 10 11 13 14 16
            18 20 21 23 25
            Criado pelo modo de teste do Bolão dos Amigos
        """.trimIndent()
        val l = LeituraComprovante.ler(Modalidade.LOTOFACIL, texto)
        assertEquals(3505, l.concurso)
        assertEquals(listOf(1, 2, 3, 5, 8, 10, 11, 13, 14, 16, 18, 20, 21, 23, 25), l.numeros)
    }

    @Test
    fun comprovanteComRotulosDaCaixa() {
        val texto = """
            LOTERIAS CAIXA
            COMPROVANTE DE APOSTA
            MEGA-SENA
            CONCURSO: 3072
            Data do sorteio: 22/10/2026 - 20:00
            Aposta simples
            05 - 12 - 23 - 34 - 45 - 56
            Valor da aposta R$ 6,00
            Código: 0123 4567 8901 2345
        """.trimIndent()
        val l = LeituraComprovante.ler(Modalidade.MEGA_SENA, texto)
        assertEquals(3072, l.concurso)
        assertEquals(listOf(5, 12, 23, 34, 45, 56), l.numeros)
    }

    @Test
    fun numerosEmBolasUmPorLinha() {
        val texto = "Quina\nConcurso 7146\n05\n17\n33\n61\n79\nTotal R$ 3,00"
        assertEquals(listOf(5, 17, 33, 61, 79), LeituraComprovante.ler(Modalidade.QUINA, texto).numeros)
    }

    @Test
    fun naoInventaQuandoNaoDaParaLer() {
        // Quantidade errada (faltou um número): não lê.
        assertNull(LeituraComprovante.ler(Modalidade.MEGA_SENA, "Números: 05 12 23 34 45").numeros)
        // Dois blocos possíveis: ambíguo, não lê.
        assertNull(LeituraComprovante.ler(Modalidade.QUINA, "01 02 03 04 05\nxx\n06 07 08 09 10").numeros)
        // Fora do volante (Quina vai até 80).
        assertNull(LeituraComprovante.ler(Modalidade.QUINA, "Números: 05 17 33 61 99").numeros)
        assertTrue(LeituraComprovante.ler(Modalidade.QUINA, "sem nada de útil").vazia)
    }

    @Test
    fun extras() {
        val tm = LeituraComprovante.ler(
            Modalidade.TIMEMANIA,
            "Timemania 2290\nNúmeros: 02 09 14 27 33 41 52 60 71 80\nTime do Coração: SANTOS/SP",
        )
        assertEquals(2290, tm.concurso)
        assertEquals(10, tm.numeros?.size)
        assertEquals("SANTOS/SP", tm.extra)
        // Perto do rótulo, o nome sem o estado vale quando é de um time só.
        assertEquals("VASCO DA GAMA/RJ", LeituraComprovante.ler(Modalidade.TIMEMANIA, "Time do Coração\nVasco da Gama").extra)
        assertNull(LeituraComprovante.ler(Modalidade.TIMEMANIA, "Time do Coração: Botafogo").extra)

        val ds = LeituraComprovante.ler(Modalidade.DIA_DE_SORTE, "Números: 01 05 09 13 20 27 31\nMês de Sorte: Março")
        assertEquals("Março", ds.extra)
        val mi = LeituraComprovante.ler(Modalidade.MAIS_MILIONARIA, "Números: 03 15 22 31 40 49 Trevos: 2 5")
        assertEquals(listOf(3, 15, 22, 31, 40, 49), mi.numeros)
        assertEquals(listOf(2, 5), mi.trevos)
    }

    @Test
    fun outrosFormatos() {
        assertEquals(listOf(3, 0, 7, 1, 9, 4, 2), LeituraComprovante.ler(Modalidade.SUPER_SETE, "Colunas: 3 0 7 1 9 4 2").numeros)
        assertEquals(
            "1X211X221X112X".map { it.toString() },
            LeituraComprovante.ler(Modalidade.LOTECA, "Palpites: 1 X 2 1 1 X 2\n2 1 X 1 1 2 X").palpitesLoteca,
        )
        assertEquals("012345", LeituraComprovante.ler(Modalidade.FEDERAL, "Bilhete: 012345\nFrações: 10 de 10").bilhete)
    }
}

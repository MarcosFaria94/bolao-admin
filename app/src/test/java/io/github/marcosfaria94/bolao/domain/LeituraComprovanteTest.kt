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

    /** O comprovante do Dia de Sorte (PDF da Caixa de 07/11/2026), com o rodapé dos telefones. */
    private val rodape = """
        0800 726 0101                           4004 0104                          0800 104 0104     0800 726 0207     0800 725 7474
                                                                                   Alô CAIXA:(Demais Atendimento CAIXA
        SAC(Sugestões, reclamações e elogios)   Alô CAIXA:(Regiões Metropolitanas) Regiões)                            Ouvidoria
    """.trimIndent()

    @Test
    fun telefoneDoRodapeNaoEOConcurso() {
        // Como o PDF sai em linhas (o rótulo e o valor na mesma linha).
        val emLinhas = """
            Loterias
            Comprovante de Aposta Dia de Sorte
            Dados da Aposta
            Nome:            MARCOS DE FARIA VALADARES                CPF:   119.099.906-47
            Data da aposta: 07/11/2026                                Subcanal da compra:  ANDROID
            Valor da aposta: R$ 2,50                                  Hora da compra:      10:40:59
            Modalidade:      Dia de Sorte                             Concurso:            1342
            Teimosinha:      Não                                      Data do sorteio:     08/11/2026
            Seus Números:
            06 | 07 | 11 | 15 | 17 | 24 | 26
            Mês de Sorte: Junho
            Validade do prêmio: 90 dias
            A4112AC3BF37EDE6465F7A45658595589C24
            $rodape
        """.trimIndent()
        val l = LeituraComprovante.ler(Modalidade.DIA_DE_SORTE, emLinhas)
        assertEquals(1342, l.concurso)
        assertEquals(listOf(6, 7, 11, 15, 17, 24, 26), l.numeros)
        assertEquals("Junho", l.extra)
        assertTrue(4004 !in l.concursos)

        // Como o reconhecimento de texto pode devolver: a coluna dos rótulos separada da dos valores,
        // e o nome da modalidade logo antes do telefone (até a 0.20 o concurso virava 4004).
        val emColunas = """
            Comprovante de Aposta Dia de Sorte
            Modalidade:
            Teimosinha:
            Concurso:
            Data do sorteio:
            Código E. Lotérico:
            Dia de Sorte
            1342
            08/11/2026
            04.025304-0
            Seus Números:
            06 | 07 | 11 | 15 | 17 | 24 | 26
            Mês de Sorte: Junho
            Dia de Sorte
            4004 0104
            0800 726 0101
        """.trimIndent()
        val c = LeituraComprovante.ler(Modalidade.DIA_DE_SORTE, emColunas)
        assertEquals(1342, c.concurso)
        assertTrue(4004 !in c.concursos)
        // Pelas seções (aba Comprovantes), também.
        val porSecao = LeituraComprovante.secoes(emColunas).flatMap { (m, t) -> LeituraComprovante.lerApostas(m, t) }
        assertEquals(listOf(1342), porSecao.map { it.concurso })
    }

    @Test
    fun concursoQueBateComAAposta() {
        val l = LeituraComprovante.Lida(concurso = 4004, concursos = listOf(4004, 1342), numeros = listOf(1, 2, 3, 4, 5, 6, 7))
        // Um dos candidatos é o da aposta: ele ganha.
        assertEquals(1342, LeituraComprovante.escolherConcurso(l, listOf(1342)).lida.concurso)
        // Perto do da aposta (o comprovante é de outro concurso): vale o lido.
        val perto = LeituraComprovante.escolherConcurso(l.copy(concursos = listOf(1345)), listOf(1342))
        assertEquals(1345, perto.lida.concurso)
        assertNull(perto.duvidoso)
        // Longe de todas: a leitura fica sem o concurso e o número vai para a nota.
        val longe = LeituraComprovante.escolherConcurso(l.copy(concursos = listOf(4004)), listOf(1342))
        assertNull(longe.lida.concurso)
        assertEquals(4004, longe.duvidoso)
        // Sem apostas para comparar: o mais provável.
        assertEquals(4004, LeituraComprovante.escolherConcurso(l, emptyList()).lida.concurso)
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

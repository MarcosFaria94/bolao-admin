package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.net.CaixaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConferenciaTest {

    private fun faixas(vararg valores: Long) = valores.mapIndexed { i, v -> FaixaPremio(i + 1, "${i + 1}ª faixa", 1, v) }

    @Test
    fun lotofacil12Acertos() {
        val c = Concurso(
            Modalidade.LOTOFACIL, 3060, LocalDate.of(2026, 6, 3),
            dezenas = (1..15).toList(),
            faixas = faixas(200_000_000, 150_000, 3_000, 1_400, 700),
        )
        val p = Palpite(numeros = (1..12).toList() + listOf(20, 21, 22))
        val r = Conferencia.conferir(Modalidade.LOTOFACIL, p, c)
        assertEquals("12/15", r.acertos)
        assertEquals(1400L, r.premio)
        assertEquals(4, r.faixas.single().faixa)
        assertFalse(r.precisaConfirmarValor)
    }

    @Test
    fun megaSemPremio() {
        val c = Concurso(Modalidade.MEGA_SENA, 2900, null, dezenas = listOf(1, 2, 3, 4, 5, 6), faixas = faixas(1, 2, 3))
        val r = Conferencia.conferir(Modalidade.MEGA_SENA, Palpite(numeros = listOf(1, 2, 3, 40, 50, 60)), c)
        assertEquals("3/6", r.acertos)
        assertEquals(0L, r.premio)
        assertFalse(r.premiada)
    }

    @Test
    fun duplaSenaDoisSorteios() {
        val c = Concurso(
            Modalidade.DUPLA_SENA, 2700, null,
            dezenas = listOf(1, 2, 3, 4, 5, 6), segundoSorteio = listOf(7, 8, 9, 10, 11, 12),
            faixas = faixas(1, 2, 3, 400, 5, 6, 700, 8),
        )
        val r = Conferencia.conferir(Modalidade.DUPLA_SENA, Palpite(numeros = listOf(1, 2, 3, 7, 8, 9)), c)
        assertEquals("3/6 (1º sorteio) e 3/6 (2º sorteio)", r.acertos)
        assertEquals(listOf(4, 8), r.faixas.map { it.faixa })
        assertEquals(408L, r.premio)
    }

    @Test
    fun maisMilionariaComTrevos() {
        val c = Concurso(
            Modalidade.MAIS_MILIONARIA, 300, null, dezenas = listOf(1, 2, 3, 4, 5, 6), trevos = listOf(2, 5),
            faixas = faixas(1, 2, 3, 4, 5, 6, 7, 800, 9, 10),
        )
        val r = Conferencia.conferir(Modalidade.MAIS_MILIONARIA, Palpite(numeros = listOf(1, 2, 3, 40, 41, 42), trevos = listOf(1, 5)), c)
        assertEquals("3/6 + 1 trevo", r.acertos)
        assertEquals(8, r.faixas.single().faixa)
        assertEquals(800L, r.premio)
    }

    @Test
    fun superSeteColunas() {
        val c = Concurso(Modalidade.SUPER_SETE, 700, null, dezenas = listOf(1, 2, 3, 4, 5, 6, 7), faixas = faixas(1, 2, 3, 4, 500))
        val r = Conferencia.conferir(Modalidade.SUPER_SETE, Palpite(numeros = listOf(1, 2, 3, 0, 0, 0, 0)), c)
        assertEquals("3/7", r.acertos)
        assertEquals(setOf(0, 1, 2), r.posicoesAcertadas)
        assertEquals(500L, r.premio)
    }

    @Test
    fun timemaniaComTimeDoCoracao() {
        val c = Concurso(
            Modalidade.TIMEMANIA, 2200, null, dezenas = listOf(1, 2, 3, 4, 5, 6, 7), timeOuMes = "ATLÉTICO/MG",
            faixas = faixas(1, 2, 3, 4, 300, 750),
        )
        val r = Conferencia.conferir(Modalidade.TIMEMANIA, Palpite(numeros = (1..3).toList() + (70..76).toList(), extra = "Atletico MG"), c)
        assertEquals("3/7 + Time do Coração", r.acertos)
        assertEquals(1050L, r.premio)
        assertTrue(r.extraAcertado)
    }

    @Test
    fun lotecaTreze() {
        val jogos = (1..14).map { JogoLoteca(it, "A$it", "B$it", 1, 0) } // todos "1"
        val c = Concurso(Modalidade.LOTECA, 1150, null, dezenas = emptyList(), jogosLoteca = jogos, faixas = faixas(100_000, 5_000))
        val r = Conferencia.conferir(Modalidade.LOTECA, Palpite(loteca = List(13) { "1" } + "X"), c)
        assertEquals("13/14", r.acertos)
        assertEquals(5000L, r.premio)
    }

    @Test
    fun federal() {
        val c = Concurso(
            Modalidade.FEDERAL, 6088, null, dezenas = emptyList(),
            bilhetesFederal = listOf("043770", "084465", "003867", "023192", "086301"),
            faixas = faixas(50_000_000, 3_500_000, 3_000_000, 2_500_000, 2_050_300),
        )
        val segundo = Conferencia.conferir(Modalidade.FEDERAL, Palpite(), c, bilheteFederal = "84465", fracoesFederal = 2)
        assertEquals("2º prêmio", segundo.acertos)
        assertEquals(700_000L, segundo.premio)
        val aproximacao = Conferencia.conferir(Modalidade.FEDERAL, Palpite(), c, bilheteFederal = "043771", fracoesFederal = 10)
        assertTrue(aproximacao.precisaConfirmarValor)
        assertEquals("Aproximação do 1º prêmio", aproximacao.acertos)
        val dezena = Conferencia.conferir(Modalidade.FEDERAL, Palpite(), c, bilheteFederal = "011170", fracoesFederal = 10)
        assertEquals("Dezena do 1º prêmio", dezena.acertos)
        val nada = Conferencia.conferir(Modalidade.FEDERAL, Palpite(), c, bilheteFederal = "011111", fracoesFederal = 10)
        assertFalse(nada.premiada)
    }

    @Test
    fun volanteSorteiaCompleto() {
        // O Time do Coração e o Mês de Sorte também são sorteados, como os números.
        for (m in Modalidade.entries) {
            val p = Volante.sortear(m)
            assertEquals(m.name, emptyList<String>(), Volante.pendencias(m, p))
        }
        val time = Volante.sortear(Modalidade.TIMEMANIA).extra
        assertTrue(time in TIMES_DO_CORACAO)
        assertEquals(listOf("Informe o Time do Coração."), Volante.pendencias(Modalidade.TIMEMANIA, Volante.sortear(Modalidade.TIMEMANIA).copy(extra = null)))
        val parcial = Palpite(numeros = listOf(10, 20))
        val completo = Volante.completar(Modalidade.MEGA_SENA, parcial)
        assertEquals(6, completo.numeros.size)
        assertTrue(completo.numeros.containsAll(listOf(10, 20)))
        // Não deixa marcar além da quantidade da aposta simples.
        assertEquals(completo, Volante.alternar(Modalidade.MEGA_SENA, completo, 59.takeIf { it !in completo.numeros } ?: 58))
    }

    @Test
    fun leJsonDaCaixa() {
        val json = """
        {"acumulado":false,"dataApuracao":"03/06/2026","dataProximoConcurso":"04/06/2026",
         "dezenasSorteadasOrdemSorteio":["05","01","03"],"listaDezenas":["01","03","05"],
         "listaDezenasSegundoSorteio":null,"listaRateioPremio":[
           {"descricaoFaixa":"15 acertos","faixa":1,"numeroDeGanhadores":0,"valorPremio":0.0},
           {"descricaoFaixa":"12 acertos","faixa":4,"numeroDeGanhadores":9000,"valorPremio":14.0}],
         "nomeTimeCoracaoMesSorte":"\u0000\u0000\u0000","numero":3060,"numeroConcursoProximo":3061,
         "valorEstimadoProximoConcurso":2000000.0,"trevosSorteados":null,"listaResultadoEquipeEsportiva":null}
        """.trimIndent()
        val c = CaixaParser.ler(Modalidade.LOTOFACIL, json)
        assertEquals(3060, c.numero)
        assertEquals(listOf(1, 3, 5), c.dezenas)
        assertEquals(LocalDate.of(2026, 6, 3), c.data)
        assertEquals(1400L, c.faixas.first { it.faixa == 4 }.valor)
        assertEquals(3061, c.proximoNumero)
        assertEquals(LocalDate.of(2026, 6, 4), c.proximaData)
        assertEquals(200_000_000L, c.premioEstimadoProximo)
        assertEquals(null, c.timeOuMes)
        assertTrue(c.temResultado)
    }
}

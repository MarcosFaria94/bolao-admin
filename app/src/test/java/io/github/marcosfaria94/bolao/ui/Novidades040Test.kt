package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.ConfigDaCalculadora
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.LinhaDaCalculadora
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.NumerosNotaveis
import io.github.marcosfaria94.bolao.domain.ResumoDoConcurso
import io.github.marcosfaria94.bolao.domain.TIMES
import io.github.marcosfaria94.bolao.domain.mesmoTime
import io.github.marcosfaria94.bolao.domain.timeDoCoracao
import io.github.marcosfaria94.bolao.ui.aposta.nomeDoTimeSorteado
import io.github.marcosfaria94.bolao.ui.jogos.ALFA_DA_LUPA
import io.github.marcosfaria94.bolao.ui.jogos.LEGENDA_QUALQUER
import io.github.marcosfaria94.bolao.ui.jogos.maisUma
import io.github.marcosfaria94.bolao.ui.jogos.menosUma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/** Os pedidos da 0.40: o time com outra grafia da Caixa, a lupa com destaque e a calculadora com todos os controles. */
class Novidades040Test {

    private fun arquivo(vararg caminhos: String): File = caminhos.map { File(it) }.first { it.exists() }

    @Test
    fun timeComOutraGrafia() {
        // As grafias da Caixa que fugiam da lista (o brasão não aparecia).
        assertEquals("Confiança", timeDoCoracao("CONFIANCA SAF    /SE")?.nome)
        assertEquals("Confiança / SE", nomeDoTimeSorteado("CONFIANCA SAF    /SE"))
        assertEquals("Barra / SC", nomeDoTimeSorteado("BARRA FC         /SC"))
        assertEquals("Sampaio Corrêa / MA", nomeDoTimeSorteado("SAMP CORREA      /MA"))
        assertTrue(mesmoTime("SAORAIMUNDO/RR", "SÃO RAIMUNDO/RR"))
        // Times diferentes continuam diferentes.
        assertFalse(mesmoTime("ATLÉTICO/MG", "ATLETICO/GO"))
        assertFalse(mesmoTime("AMERICA/RJ", "AMERICANO/RJ"))
        assertFalse(mesmoTime("SÃO PAULO/SP", "SÃO BERNARDO/SP"))
        assertFalse(mesmoTime("SANTOS/SP", "SANTO ANDRE/SP"))
        assertFalse(mesmoTime("BARRA", "BARRA FC"))
        assertNull(timeDoCoracao("CAMPINENSE       /PB"))
        for ((i, a) in TIMES.withIndex()) for (b in TIMES.drop(i + 1)) assertFalse("${a.caixa} x ${b.caixa}", mesmoTime(a.caixa, b.caixa))
        // A conferência: apostou no Confiança e saiu "CONFIANCA SAF/SE" = acertou o Time do Coração.
        assertTrue(Conferencia.mesmoNome("CONFIANÇA/SE", "CONFIANCA SAF    /SE"))
        assertFalse(Conferencia.mesmoNome("ITABAIANA/SE", "CONFIANCA SAF    /SE"))
        // Os meses do Dia de Sorte como antes.
        assertTrue(Conferencia.mesmoNome("Março", "MARCO"))
        assertFalse(Conferencia.mesmoNome("Junho", "Julho"))
        // Os Times notáveis juntam as grafias do mesmo time numa linha só.
        assertEquals(NumerosNotaveis.chaveDoGrupoDoTime("CONFIANÇA/SE"), NumerosNotaveis.chaveDoGrupoDoTime("CONFIANCA SAF    /SE"))
        val pasta = arquivo("src/main/assets/historico", "app/src/main/assets/historico")
        val linhas = GZIPInputStream(File(pasta, "${Modalidade.TIMEMANIA.slugApi}.jsonl.gzip").inputStream()).bufferedReader().readLines()
        val times = NumerosNotaveis.grupos(Modalidade.TIMEMANIA, linhas.mapNotNull { ResumoDoConcurso.daLinha(it) })
            .first { it.titulo == NumerosNotaveis.TITULO_TIMES }
        val nomes = times.itens.map { nomeDoTimeSorteado(it.rotulo) }
        assertEquals(1, nomes.count { it == "Confiança / SE" })
        assertEquals(times.itens.size, nomes.toSet().size)
    }

    @Test
    fun calculadoraComTodosOsControles() {
        val m = Modalidade.MEGA_SENA
        // O título: "6 números" (era "Apostas simples (6 números)"); os outros, "8 números".
        assertEquals("6 números", Calculadora.nomeDaLinha(m, 6, 0))
        assertEquals("8 números", Calculadora.nomeDaLinha(m, 8, 0))
        assertEquals("7 números e 3 trevos", Calculadora.nomeDaLinha(Modalidade.MAIS_MILIONARIA, 7, 3))
        assertEquals("Com mais números", Calculadora.tituloDosMaisNumeros(m))
        // A simples começa em 1; todos os outros (de 7 a 20 números), em 0.
        val padrao = ConfigDaCalculadora()
        assertEquals(1, padrao.simples)
        assertEquals((7..20).toList(), Calculadora.comMaisNumeros(m).map { it.numeros })
        assertTrue(Calculadora.comMaisNumeros(m).all { Calculadora.apostasDe(m, padrao, it.numeros, it.trevos) == 0 })
        assertEquals(1, Calculadora.apostasDe(m, padrao, 6, 0))
        // O + e o − mudam de uma em uma; em 0 o tipo sai da lista guardada; nunca abaixo de 0.
        val com8 = Calculadora.comApostas(m, padrao, 8, 0, 1)
        assertEquals(listOf(LinhaDaCalculadora(8, 0, 1)), com8.extras)
        assertEquals(2, Calculadora.apostasDe(m, Calculadora.comApostas(m, com8, 8, 0, 2), 8, 0))
        assertTrue(Calculadora.comApostas(m, com8, 8, 0, 0).extras.isEmpty())
        assertEquals(0, Calculadora.comApostas(m, padrao, 6, 0, -1).simples)
        // Os tipos ficam em ordem (números e trevos), seja qual for a ordem em que foram mexidos.
        val varios = Calculadora.comApostas(m, Calculadora.comApostas(m, padrao, 10, 0, 2), 7, 0, 3)
        assertEquals(listOf(7, 10), varios.extras.map { it.numeros })
        // A +Milionária: todos os números (6 a 12) com todos os trevos (2 a 6), menos a simples.
        assertEquals(7 * 5 - 1, Calculadora.comMaisNumeros(Modalidade.MAIS_MILIONARIA).size)
        assertEquals("Com mais números ou trevos", Calculadora.tituloDosMaisNumeros(Modalidade.MAIS_MILIONARIA))
        // Sem mais números (Timemania, Lotomania, Super Sete, Loteca): só o controle da simples.
        for (s in listOf(Modalidade.TIMEMANIA, Modalidade.LOTOMANIA, Modalidade.SUPER_SETE, Modalidade.LOTECA)) {
            assertTrue(s.nome, Calculadora.comMaisNumeros(s).isEmpty())
        }
        // Os nomes dos botões do ajuste fino e o "Qualquer prêmio" à parte.
        assertEquals("Menos uma: 8 números", menosUma("8 números"))
        assertEquals("Mais uma: 8 números", maisUma("8 números"))
        assertTrue(LEGENDA_QUALQUER.startsWith("Todas as faixas juntas"))
        // A lupa quase opaca (era 60%).
        assertTrue(ALFA_DA_LUPA > 0.6f)
    }
}

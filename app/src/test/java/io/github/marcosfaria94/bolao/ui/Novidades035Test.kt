package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.PremioDoConcurso
import io.github.marcosfaria94.bolao.net.CaixaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/** As novidades da 0.35 que dão para testar sem as telas: o prêmio de cada concurso no alto do resultado. */
class Novidades035Test {

    private fun concurso(m: Modalidade, n: Int, faixa1: FaixaPremio? = null, acumulado: Boolean = true, estimadoProximo: Long? = null) =
        Concurso(m, n, null, emptyList(), faixas = listOfNotNull(faixa1), acumulado = acumulado, premioEstimadoProximo = estimadoProximo)

    @Test
    fun premioDoConcurso() {
        val antes = concurso(Modalidade.TIMEMANIA, 2466, estimadoProximo = 2_750_000_000L)
        val acumulou = concurso(Modalidade.TIMEMANIA, 2467, FaixaPremio(1, "7 acertos", 0, 0))
        // O prêmio estimado que a Caixa divulgou para o concurso (veio com o anterior).
        assertEquals(PremioDoConcurso.Premio(PremioDoConcurso.ROTULO_ESTIMADO, 2_750_000_000L), PremioDoConcurso.de(acumulou, antes))
        assertEquals("R$ 27,5 Milhões", Dinheiro.porExtenso(PremioDoConcurso.de(acumulou, antes)!!.valor))
        // O anterior tem de ser o concurso de antes, da mesma modalidade (um velho, ainda na tela, não serve).
        assertNull(PremioDoConcurso.de(acumulou, concurso(Modalidade.TIMEMANIA, 2400, estimadoProximo = 1L)))
        assertNull(PremioDoConcurso.de(acumulou, concurso(Modalidade.QUINA, 2466, estimadoProximo = 1L)))
        // Sem a estimativa: se saiu, o que a faixa principal pagou (todos os ganhadores); se acumulou, nada.
        val saiu = concurso(Modalidade.MEGA_SENA, 500, FaixaPremio(1, "6 acertos", 2, 1_000_000_000L), acumulado = false)
        assertEquals(PremioDoConcurso.Premio(PremioDoConcurso.ROTULO_PAGO, 2_000_000_000L), PremioDoConcurso.de(saiu, null))
        assertNull(PremioDoConcurso.de(acumulou, null))
        assertNull(PremioDoConcurso.de(acumulou, concurso(Modalidade.TIMEMANIA, 2466, estimadoProximo = 0L)))
        // Na Federal, o 1º prêmio (a Federal não tem estimativa).
        val federal = concurso(Modalidade.FEDERAL, 6000, FaixaPremio(1, "1º prêmio", 1, 50_000_000L), acumulado = false)
        assertEquals(PremioDoConcurso.Premio(PremioDoConcurso.ROTULO_DA_FEDERAL, 50_000_000L), PremioDoConcurso.de(federal, null))
    }

    /** Com o histórico embutido: o prêmio dos concursos de verdade (o último da Timemania, as Mega da Virada). */
    @Test
    fun premioDosConcursosDoHistorico() {
        val pasta = listOf("src/main/assets/historico", "app/src/main/assets/historico").map { File(it) }.first { it.exists() }
        fun linhas(m: Modalidade) = GZIPInputStream(File(pasta, "${m.slugApi}.jsonl.gzip").inputStream()).bufferedReader().readLines()
            .map { CaixaParser.ler(m, it) }.associateBy { it.numero }
        val mega = linhas(Modalidade.MEGA_SENA)
        // A Mega da Virada de 2025: o R$ 1 Bilhão anunciado (e não o que cada um dos 6 ganhadores levou).
        val virada = PremioDoConcurso.de(mega.getValue(2955), mega[2954])!!
        assertEquals(PremioDoConcurso.ROTULO_ESTIMADO, virada.rotulo)
        assertEquals("R$ 1 Bilhão", Dinheiro.porExtenso(virada.valor))
        assertEquals("R$ 600 Milhões", Dinheiro.porExtenso(PremioDoConcurso.de(mega.getValue(2810), mega[2809])!!.valor))
        // Todo concurso da Timemania (que tem a estimativa desde o 1º) tem o prêmio, acumulando ou não.
        val time = linhas(Modalidade.TIMEMANIA)
        val sem = time.values.filter { it.numero > 1 && time.containsKey(it.numero - 1) && PremioDoConcurso.de(it, time[it.numero - 1]) == null }
        assertTrue("sem prêmio: ${sem.take(5).map { it.numero }}", sem.isEmpty())
        // Na Federal, o 1º prêmio de cada concurso.
        val federal = linhas(Modalidade.FEDERAL)
        val c = federal.values.last()
        assertEquals(PremioDoConcurso.ROTULO_DA_FEDERAL, PremioDoConcurso.de(c, null)!!.rotulo)
    }
}

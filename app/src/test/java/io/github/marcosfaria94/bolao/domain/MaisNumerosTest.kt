package io.github.marcosfaria94.bolao.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Apostas com mais números (parâmetro da 0.22): o volante, o preço, a conferência e a leitura do comprovante. */
class MaisNumerosTest {

    private fun faixas(vararg valores: Long) = valores.mapIndexed { i, v -> FaixaPremio(i + 1, "${i + 1}ª faixa", 1, v) }

    @Test
    fun combinacoesEPreco() {
        assertEquals(1L, Volante.combinacoes(6, 6))
        assertEquals(7L, Volante.combinacoes(7, 6))
        assertEquals(28L, Volante.combinacoes(8, 6))
        assertEquals(38_760L, Volante.combinacoes(20, 6))
        assertEquals(0L, Volante.combinacoes(5, 6))
        // Mega-Sena: 7 números = 7 apostas (R$ 42,00); 20 números = R$ 232.560,00.
        assertEquals(4_200L, Volante.preco(Modalidade.MEGA_SENA, Palpite(numeros = (1..7).toList()), 600))
        assertEquals(23_256_000L, Volante.preco(Modalidade.MEGA_SENA, Palpite(numeros = (1..20).toList()), 600))
        // Lotofácil com 16 = 16 apostas; Quina com 6 = 6; Dia de Sorte com 8 = 8.
        assertEquals(16L, Volante.apostasSimples(Modalidade.LOTOFACIL, 16))
        assertEquals(6L, Volante.apostasSimples(Modalidade.QUINA, 6))
        assertEquals(8L, Volante.apostasSimples(Modalidade.DIA_DE_SORTE, 8))
        // +Milionária: 6 números e 3 trevos = 3 apostas; 7 números e 2 trevos = 7.
        assertEquals(3L, Volante.apostasSimples(Modalidade.MAIS_MILIONARIA, 6, 3))
        assertEquals(7L, Volante.apostasSimples(Modalidade.MAIS_MILIONARIA, 7, 2))
        // Aposta simples (ou incompleta): 1.
        assertEquals(1L, Volante.apostasSimples(Modalidade.MEGA_SENA, 3))
        assertEquals(1L, Volante.apostasSimples(Modalidade.SUPER_SETE, 7))
        assertTrue(Volante.aceitaMais(Modalidade.MEGA_SENA) && Volante.aceitaMais(Modalidade.MAIS_MILIONARIA))
        assertFalse(Volante.aceitaMais(Modalidade.LOTOMANIA) || Volante.aceitaMais(Modalidade.TIMEMANIA) || Volante.aceitaMais(Modalidade.SUPER_SETE))
    }

    @Test
    fun volanteAceitaAteOMaximo() {
        val m = Modalidade.QUINA
        var p = Palpite(numeros = listOf(1, 2, 3, 4, 5))
        // Sem o parâmetro, o sexto número não entra; com ele, entra até o 15º.
        assertEquals(5, Volante.alternar(m, p, 6).numeros.size)
        (6..20).forEach { p = Volante.alternar(m, p, it, mais = true) }
        assertEquals(15, p.numeros.size)
        assertTrue(Volante.pendencias(m, p).isEmpty())
        assertTrue(Volante.temMais(m, p))
        assertEquals(0, Volante.faltam(m, p))
        assertEquals(3, Volante.faltam(m, Palpite(numeros = listOf(1, 2))))
        // O "Completar" completa até a aposta simples e não tira o que já tem a mais.
        assertEquals(5, Volante.completar(m, Palpite(numeros = listOf(7)), Random(1)).numeros.size)
        assertEquals(p.numeros, Volante.completar(m, p, Random(1)).numeros)
        // Trevos da +Milionária: até 6 com o parâmetro.
        var t = Palpite(trevos = listOf(1, 2))
        assertEquals(2, Volante.alternarTrevo(Modalidade.MAIS_MILIONARIA, t, 3).trevos.size)
        (3..6).forEach { t = Volante.alternarTrevo(Modalidade.MAIS_MILIONARIA, t, it, mais = true) }
        assertEquals(6, t.trevos.size)
    }

    /** "Senti que repetiu mês demais": dentro do conjunto de apostas do concurso, o mês só repete depois dos 12. */
    @Test
    fun completarNaoRepeteOMesNoConjunto() {
        val r = Random(7)
        val usados = mutableListOf<String>()
        repeat(12) { usados += Volante.extraAleatorio(Modalidade.DIA_DE_SORTE, r, usados)!! }
        assertEquals(MESES_DE_SORTE.toSet(), usados.toSet())
        // Com os 12 usados uma vez e Março duas, o próximo não é Março.
        repeat(20) { assertFalse(Volante.extraAleatorio(Modalidade.DIA_DE_SORTE, r, usados + "Março") == "Março") }
        // O "Completar" usa o que as outras apostas do concurso já têm (e mantém o mês escolhido à mão).
        val ja = Volante.JaApostado(extras = MESES_DE_SORTE - "Julho")
        repeat(10) { assertEquals("Julho", Volante.completar(Modalidade.DIA_DE_SORTE, Palpite(), r, ja).extra) }
        assertEquals("Maio", Volante.completar(Modalidade.DIA_DE_SORTE, Palpite(extra = "Maio"), r, ja).extra)
        // E não devolve um jogo igual a outro do conjunto.
        val outro = Volante.completar(Modalidade.DIA_DE_SORTE, Palpite(), Random(3)).numeros
        assertFalse(Volante.completar(Modalidade.DIA_DE_SORTE, Palpite(), Random(3), Volante.JaApostado(jogos = listOf(outro))).numeros == outro)
        assertNull(Volante.extraAleatorio(Modalidade.QUINA, r))
    }

    @Test
    fun megaComSeteNumeros() {
        val c = Concurso(Modalidade.MEGA_SENA, 3080, null, dezenas = listOf(1, 2, 3, 4, 5, 6), faixas = faixas(50_000_000_00, 40_000_00, 1_000_00))
        // Os 6 sorteados entre os 7 marcados: 1 sena e 6 quinas.
        val sena = Conferencia.conferir(Modalidade.MEGA_SENA, Palpite(numeros = listOf(1, 2, 3, 4, 5, 6, 30)), c)
        assertEquals("6/6", sena.acertos)
        assertEquals(listOf(1, 2), sena.faixas.map { it.faixa })
        assertEquals(50_000_000_00L + 6 * 40_000_00L, sena.premio)
        assertTrue(sena.precisaConfirmarValor && Conferencia.AVISO_MAIS_NUMEROS in sena.observacoes)
        // 4 acertos em 7 números: 3 quadras (C(4,4) x C(3,2)).
        val quadras = Conferencia.conferir(Modalidade.MEGA_SENA, Palpite(numeros = listOf(1, 2, 3, 4, 30, 40, 50)), c)
        assertEquals("4/6", quadras.acertos)
        assertEquals(3 * 1_000_00L, quadras.premio)
        assertTrue(quadras.faixas.single().descricao.endsWith("(3x)"))
        // Sem prêmio: nada a confirmar (a apuração sai sozinha).
        val nada = Conferencia.conferir(Modalidade.MEGA_SENA, Palpite(numeros = listOf(1, 2, 3, 30, 40, 50, 60)), c)
        assertEquals("3/6", nada.acertos)
        assertFalse(nada.premiada || nada.precisaConfirmarValor)
        assertTrue(nada.observacoes.isEmpty())
        assertEquals(1L, Conferencia.simplesCom(6, 7, 6, 6))
        assertEquals(6L, Conferencia.simplesCom(6, 7, 6, 5))
    }

    @Test
    fun diaDeSorteEDuplaSenaComMaisNumeros() {
        // Dia de Sorte com 8 números, 4 acertos e o mês: C(4,4) x C(4,3) = 4 apostas com 4 acertos; o mês vale nas 8.
        val ds = Concurso(
            Modalidade.DIA_DE_SORTE, 1340, null, dezenas = listOf(1, 2, 3, 4, 5, 6, 7), timeOuMes = "Março",
            faixas = faixas(1_000_000_00, 2_000_00, 25_00, 5_00, 2_50),
        )
        val r = Conferencia.conferir(Modalidade.DIA_DE_SORTE, Palpite(numeros = listOf(1, 2, 3, 4, 20, 21, 22, 23), extra = "Março"), ds)
        assertEquals("4/7 + Mês de Sorte", r.acertos)
        assertEquals(4 * 5_00L + 8 * 2_50L, r.premio)
        assertTrue(r.extraAcertado)
        // Dupla Sena com 7 números: 3 acertos no 1º sorteio (C(3,3) x C(4,3) = 4 ternos) e nada no 2º.
        val dp = Concurso(
            Modalidade.DUPLA_SENA, 2800, null, dezenas = listOf(1, 2, 3, 4, 5, 6), segundoSorteio = listOf(40, 41, 42, 43, 44, 45),
            faixas = faixas(1, 2, 3, 400, 5, 6, 7, 800),
        )
        val d = Conferencia.conferir(Modalidade.DUPLA_SENA, Palpite(numeros = listOf(1, 2, 3, 20, 21, 22, 23)), dp)
        assertEquals("3/6 (1º sorteio) e 0/6 (2º sorteio)", d.acertos)
        assertEquals(4 * 400L, d.premio)
        assertEquals(listOf(4), d.faixas.map { it.faixa })
    }

    @Test
    fun maisMilionariaComMaisTrevos() {
        val c = Concurso(
            Modalidade.MAIS_MILIONARIA, 400, null, dezenas = listOf(1, 2, 3, 4, 5, 6), trevos = listOf(1, 2),
            faixas = (1..10).map { FaixaPremio(it, "", 1, (11 - it) * 100L) },
        )
        // 6 números (4 acertos) e 3 trevos (os 2 sorteados entre eles): 3 apostas; 1 com os 2 trevos (5ª faixa)
        // e 2 com 1 trevo (6ª faixa: 4 números + 1 ou nenhum).
        val r = Conferencia.conferir(Modalidade.MAIS_MILIONARIA, Palpite(numeros = listOf(1, 2, 3, 4, 30, 40), trevos = listOf(1, 2, 3)), c)
        assertEquals("4/6 + 2 trevos", r.acertos)
        assertEquals(listOf(5, 6), r.faixas.map { it.faixa })
        assertEquals(600L + 2 * 500L, r.premio)
    }

    @Test
    fun leituraDeApostaComMaisNumeros() {
        val oito = listOf(3, 9, 17, 24, 38, 41, 52, 60)
        val texto = "MEGA-SENA\nCONCURSO: 3080\nNúmeros: 03 09 17 24 38 41\n52 60\nValor R$ 168,00"
        // Sem saber da aposta de 8 números, a leitura fica na aposta simples (os 6 primeiros)...
        assertEquals(listOf(3, 9, 17, 24, 38, 41), LeituraComprovante.ler(Modalidade.MEGA_SENA, texto).numeros)
        // ...e, com a aposta esperada, lê os 8.
        assertEquals(oito, LeituraComprovante.ler(Modalidade.MEGA_SENA, texto, esperados = listOf(oito)).numeros)
        assertEquals(listOf(oito), LeituraComprovante.lerApostas(Modalidade.MEGA_SENA, texto, listOf(oito)).map { it.numeros })
        // Sem rótulo: a sequência inteira igual à aposta esperada é um jogo só; senão, dois jogos de 6.
        val doze = (1..12).toList()
        val semRotulo = "MEGA-SENA 3080\n01 02 03 04 05 06\n07 08 09 10 11 12\nBoa sorte"
        assertEquals(listOf(doze), LeituraComprovante.lerApostas(Modalidade.MEGA_SENA, semRotulo, listOf(doze)).map { it.numeros })
        assertEquals(2, LeituraComprovante.lerApostas(Modalidade.MEGA_SENA, semRotulo).size)
    }
}

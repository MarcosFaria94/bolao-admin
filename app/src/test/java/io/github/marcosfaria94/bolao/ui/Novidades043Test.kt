package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Base
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Etapa
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Modo
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Pedido
import io.github.marcosfaria94.bolao.domain.Modalidade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** As novidades da 0.43 que dão para testar sem as telas: o desdobramento com a base e a cobertura. */
class Novidades043Test {

    private val mega = Modalidade.MEGA_SENA

    @Test
    fun desdobramentoPelosNumerosOuPelosPremios() {
        assertEquals(listOf("P", "J", "D", "$", "F", "C"), Modo.entries.map { it.letra })
        var p = CalculoEmEtapas.comModo(Pedido(), Modo.DESDOBRAMENTO)
        assertEquals(listOf(Etapa.BASE, Etapa.NUMEROS), CalculoEmEtapas.etapas(p))
        assertEquals(1, CalculoEmEtapas.etapaAtual(mega, p))
        // Pelos prêmios: a faixa e quantos.
        p = CalculoEmEtapas.comBase(p, Base.PREMIOS)
        assertEquals(listOf(Etapa.BASE, Etapa.FAIXA, Etapa.PREMIOS), CalculoEmEtapas.etapas(p))
        assertEquals(2, CalculoEmEtapas.depoisDe(mega, p, 1))
        assertEquals(listOf("Quina", "Quadra"), CalculoEmEtapas.faixasDoModo(mega, p).map { it.second })
        p = CalculoEmEtapas.comFaixa(mega, p, 2).copy(valor = "10")
        assertEquals(4, CalculoEmEtapas.etapaAtual(mega, p))
        val r = CalculoEmEtapas.resultado(mega, p, 600)!!
        assertEquals(9, r.numerosEscolhidos)
        assertEquals(84L, r.jogos)
        assertEquals(2, r.destaque)
        // Trocar de base começa a etapa seguinte do zero; a mesma base mantém.
        assertEquals(p, CalculoEmEtapas.comBase(p, Base.PREMIOS))
        val n = CalculoEmEtapas.comBase(p, Base.NUMEROS)
        assertNull(n.faixa)
        assertEquals("", n.valor)
        assertEquals(2, CalculoEmEtapas.etapaAtual(mega, n))
        val pelosNumeros = CalculoEmEtapas.resultado(mega, n.copy(valor = "10"), 600)!!
        assertEquals(210L, pelosNumeros.jogos)
        assertNull(pelosNumeros.destaque)
        // A base vai junto para a outra modalidade.
        assertEquals(Base.PREMIOS, CalculoEmEtapas.naModalidade(p, mega, Modalidade.QUINA).base)
    }

    @Test
    fun limitesDaCobertura() {
        // O exemplo do usuário: D(60, 6, 4) ≤ 32.509 contando as quadras; o limite de Johnson aperta para 32.450.
        assertEquals(32_509L, CalculoEmEtapas.limiteDosGrupos(60, 6, 4))
        assertEquals(32_450L, CalculoEmEtapas.limiteDeJohnson(60, 6, 4))
        assertEquals(304L, CalculoEmEtapas.limiteDeJohnson(80, 5, 2))
        assertEquals(975L, CalculoEmEtapas.limiteDeJohnson(50, 6, 3))
        assertEquals(3_110L, CalculoEmEtapas.limiteDeJohnson(25, 15, 11))
        assertEquals(7L, CalculoEmEtapas.limiteDeJohnson(7, 3, 2))
        assertEquals(4, CalculoEmEtapas.acertosDaCobertura(mega))
        assertEquals(2, CalculoEmEtapas.acertosDaCobertura(Modalidade.QUINA))
        assertEquals(11, CalculoEmEtapas.acertosDaCobertura(Modalidade.LOTOFACIL))
        assertEquals(7..60, CalculoEmEtapas.limitesDaCobertura(mega))
        assertNull(CalculoEmEtapas.limitesDaCobertura(Modalidade.LOTOMANIA))
        assertNull(CalculoEmEtapas.limitesDaCobertura(Modalidade.SUPER_SETE))
        assertNotNull(CalculoEmEtapas.motivoSemModo(Modalidade.LOTOMANIA, Modo.COBERTURA))
        assertNull(CalculoEmEtapas.motivoSemModo(mega, Modo.COBERTURA))
        assertEquals("nenhuma quadra", CalculoEmEtapas.nenhumGrupo(mega))
        assertEquals("nenhum duque", CalculoEmEtapas.nenhumGrupo(Modalidade.QUINA))
        assertEquals("nenhum terno", CalculoEmEtapas.nenhumGrupo(Modalidade.DUPLA_SENA))
        assertEquals("nenhum grupo de 11 números", CalculoEmEtapas.nenhumGrupo(Modalidade.LOTOFACIL))
        // A calculadora monta os jogos com todos os números da Mega, da Quina, da Dupla e do Dia de Sorte.
        assertEquals(60, CalculoEmEtapas.maximoMontavel(mega))
        assertEquals(80, CalculoEmEtapas.maximoMontavel(Modalidade.QUINA))
        assertEquals(50, CalculoEmEtapas.maximoMontavel(Modalidade.DUPLA_SENA))
        assertEquals(31, CalculoEmEtapas.maximoMontavel(Modalidade.DIA_DE_SORTE))
        assertEquals(23, CalculoEmEtapas.maximoMontavel(Modalidade.LOTOFACIL))
        assertEquals(26, CalculoEmEtapas.maximoMontavel(Modalidade.TIMEMANIA))
        assertFalse(CalculoEmEtapas.podeMontarCobertura(Modalidade.TIMEMANIA, 80))
    }

    private fun confere(jogos: List<IntArray>, v: Int, k: Int, t: Int) {
        val vistos = HashSet<List<Int>>()
        fun grupos(a: List<Int>, ini: Int, falta: Int, atual: List<Int>, saida: MutableList<List<Int>>) {
            if (falta == 0) { saida += atual; return }
            for (i in ini..a.size - falta) grupos(a, i + 1, falta - 1, atual + a[i], saida)
        }
        for (j in jogos) {
            assertEquals(k, j.size)
            assertEquals(k, j.toSet().size)
            assertTrue(j.all { it in 0 until v })
            val g = mutableListOf<List<Int>>()
            grupos(j.sorted(), 0, t, emptyList(), g)
            for (x in g) assertTrue("grupo repetido $x", vistos.add(x))
        }
    }

    @Test
    fun coberturaMontada() {
        // O plano de Fano: 7 jogos de 3 em 7 números, sem repetir par.
        val fano = CalculoEmEtapas.cobertura(7, 3, 2)
        assertEquals(7, fano.size)
        confere(fano, 7, 3, 2)
        val nove = CalculoEmEtapas.cobertura(9, 3, 2)
        confere(nove, 9, 3, 2)
        assertTrue("${nove.size}", nove.size in 8..12)
        // Mega com 12 e com 20 números: nunca repete quadra, pelo menos o da ordem do dicionário (10 e 154).
        val doze = CalculoEmEtapas.cobertura(12, 6, 4)
        confere(doze, 12, 6, 4)
        assertTrue("${doze.size}", doze.size in 10..30)
        val vinte = CalculoEmEtapas.cobertura(20, 6, 4)
        confere(vinte, 20, 6, 4)
        assertTrue("${vinte.size}", vinte.size in 154..276)
        // Os jogos voltam em ordem.
        assertTrue(vinte.zipWithNext().all { (a, b) -> a.toList().zip(b.toList()).first { it.first != it.second }.let { it.first < it.second } })
        // Sempre o mesmo resultado.
        assertEquals(doze.map { it.toList() }, CalculoEmEtapas.cobertura(12, 6, 4).map { it.toList() })
    }

    @Test
    fun resultadoDaCobertura() {
        // Na Timemania com os 80 números a conta de montar é grande demais: o preço e as chances do limite de Johnson.
        val tm = Modalidade.TIMEMANIA
        val todos = CalculoEmEtapas.resultado(tm, Pedido(modo = Modo.COBERTURA, valor = "80"), 350)!!
        assertEquals(632L, todos.jogos)
        assertEquals(632L * 350, todos.custo)
        assertEquals("até 632 jogos de 10 números", todos.tipo)
        assertEquals(684L, todos.cobertura!!.grupos)
        assertNull(todos.cobertura!!.montados)
        assertEquals(4, todos.destaque)
        // Na Mega com os 60, os montados (o máximo fica entre eles e 32.450).
        assertNull(CalculoEmEtapas.resultado(mega, Pedido(modo = Modo.COBERTURA, valor = "60"), 600))
        val mega60 = CalculoEmEtapas.resultado(mega, Pedido(modo = Modo.COBERTURA, valor = "60"), 600, montadosDaCobertura = 20_425)!!
        assertEquals(20_425L * 600, mega60.custo)
        assertEquals(32_450L, mega60.cobertura!!.johnson)
        assertEquals(32_509L, mega60.cobertura!!.grupos)
        assertEquals(2, mega60.destaque)
        // Com 20 números a tela monta antes (sem os montados, ainda não há resultado).
        assertNull(CalculoEmEtapas.resultado(mega, Pedido(modo = Modo.COBERTURA, valor = "20"), 600))
        val vinte = CalculoEmEtapas.resultado(mega, Pedido(modo = Modo.COBERTURA, valor = "20"), 600, montadosDaCobertura = 160)!!
        assertEquals(160L, vinte.jogos)
        assertEquals("160 jogos de 6 números", vinte.tipo)
        assertEquals(276L, vinte.cobertura!!.johnson)
        assertNotNull(CalculoEmEtapas.erroDoValor(mega, Pedido(modo = Modo.COBERTURA, valor = "61")))
        assertNull(CalculoEmEtapas.erroDoValor(mega, Pedido(modo = Modo.COBERTURA, valor = "60")))
        assertEquals("60" to "Todos (60)", CalculoEmEtapas.sugestoes(mega, Pedido(modo = Modo.COBERTURA)).last())
    }
}

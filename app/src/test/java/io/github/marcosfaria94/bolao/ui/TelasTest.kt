package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.aposta.rotaDaAposta
import io.github.marcosfaria94.bolao.ui.aposta.tituloDaAposta
import io.github.marcosfaria94.bolao.ui.comum.numeroLancamento
import io.github.marcosfaria94.bolao.ui.comum.rotuloLancamento
import io.github.marcosfaria94.bolao.ui.jogos.linhaIndices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Textos das telas que não dependem do Android (títulos, legendas dos índices, aposta nova). */
class TelasTest {

    private val hoje = LocalDate.of(2026, 10, 21)

    private fun aposta(m: Modalidade, numero: Int, concurso: Int, registrada: Boolean = true) = Aposta(
        id = "a$numero", numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje,
        valor = 350, registrada = registrada, criadaEm = 0,
    )

    @Test
    fun tituloDaApostaSemONumero() {
        assertEquals("Mega-Sena · 3073", tituloDaAposta(aposta(Modalidade.MEGA_SENA, 8, 3073)))
        assertEquals("Quina", tituloDaAposta(aposta(Modalidade.QUINA, 1, 0)))
    }

    @Test
    fun movimentacoesComOJogoNoTitulo() {
        val a = aposta(Modalidade.LOTOFACIL, 7, 3809)
        val e = Estado(apostas = listOf(a))
        val premio = Lancamento("l1", hoje, 1, TipoLancamento.PREMIO, 700, apostaId = a.id)
        val jogo = Lancamento("l2", hoje, 2, TipoLancamento.APOSTA, -350, apostaId = a.id)
        val aporte = Lancamento("l3", hoje, 3, TipoLancamento.APORTE, 3000)
        assertEquals("Prêmio · Lotofácil · 3809", rotuloLancamento(e, premio))
        assertEquals("Aposta · Lotofácil · 3809", rotuloLancamento(e, jogo))
        assertEquals("Aporte mensal", rotuloLancamento(e, aporte))
        assertEquals(7, numeroLancamento(e, premio))
        assertEquals(7, numeroLancamento(e, jogo))
        assertNull(numeroLancamento(e, aporte))
    }

    @Test
    fun linhaDosIndices() {
        val k = ResultadoK(
            numero = 7148, especial = false, acumuladoPrincipal = 0.0, reserva = 0, herancaFaixas = emptyMap(),
            nAnterior = 0.0, k1 = 0.6894, k2 = 0.0912,
        )
        assertEquals("k1 0,689  k2 0,091  odds 1:34", linhaIndices(Indices.Item(Modalidade.QUINA, null, k, 34.2)))
        assertEquals("k1 —  k2 —  odds 1:34", linhaIndices(Indices.Item(Modalidade.QUINA, null, null, 34.2)))
        assertEquals("Sem referência", linhaIndices(Indices.Item(Modalidade.LOTECA, null, null, null)))
        assertEquals("Sem referência", linhaIndices(Indices.Item(Modalidade.FEDERAL, null, null, null)))
    }

    @Test
    fun apostaNovaComOVolanteVazio() {
        val e = Estado(config = Config(precos = mapOf(Modalidade.QUINA.name to 320)))
        val ultimo = Concurso(
            modalidade = Modalidade.QUINA, numero = 7147, data = hoje.minusDays(1), dezenas = emptyList(),
            proximoNumero = 7148, proximaData = hoje, premioEstimadoProximo = 60_000_000,
        )
        val a = novaAposta(e, Modalidade.QUINA, ultimo, hoje, agora = 5)
        assertTrue(a.numeros.isEmpty())
        assertTrue(a.trevos.isEmpty() && a.extra == null)
        assertEquals(7148, a.concurso)
        assertEquals(hoje, a.dataSorteio)
        assertEquals(60_000_000L, a.premioEstimado)
        assertEquals(320L, a.valor)
        assertEquals(false, a.registrada)

        // Super Sete: as 7 colunas sem número; concurso já sorteado: a confirmar.
        val s = novaAposta(e, Modalidade.SUPER_SETE, ultimo.copy(modalidade = Modalidade.SUPER_SETE, proximaData = hoje.minusDays(1)), hoje, 5)
        assertEquals(List(7) { -1 }, s.numeros)
        assertEquals(0, s.concurso)
        assertEquals(10, novaAposta(e, Modalidade.FEDERAL, null, hoje, 5).fracoesFederal)
    }

    @Test
    fun apostarAbreOAndamentoOuUmaNova() {
        val rascunho = aposta(Modalidade.QUINA, 0, 7148, registrada = false)
        val e = Estado(apostas = listOf(rascunho))
        assertEquals("aposta/${rascunho.id}", rotaDaAposta(e, Modalidade.QUINA))
        assertEquals("nova/MEGA_SENA", rotaDaAposta(e, Modalidade.MEGA_SENA))
    }
}

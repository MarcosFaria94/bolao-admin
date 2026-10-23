package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.data.Rateio
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.aposta.rotaDaAposta
import io.github.marcosfaria94.bolao.ui.aposta.tituloDaAposta
import io.github.marcosfaria94.bolao.ui.comum.descricaoDaAposta
import io.github.marcosfaria94.bolao.ui.comum.descricaoLancamento
import io.github.marcosfaria94.bolao.ui.comum.tarjaLancamento
import io.github.marcosfaria94.bolao.ui.comum.valorDoLancamento
import io.github.marcosfaria94.bolao.ui.jogos.indicesDoCartao
import io.github.marcosfaria94.bolao.ui.jogos.linhaIndices
import io.github.marcosfaria94.bolao.ui.jogos.tituloDoCartao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Textos das telas que não dependem do Android (títulos, legendas dos índices, aposta nova). */
class TelasTest {

    private val hoje = LocalDate.of(2026, 10, 21)

    private fun aposta(m: Modalidade, numero: Int, concurso: Int, registrada: Boolean = true, premio: Long? = null) = Aposta(
        id = "a$numero", numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje,
        valor = 350, registrada = registrada, criadaEm = 0, premioEstimado = premio,
    )

    @Test
    fun tituloEDescricaoDaAposta() {
        // Tarja "Aposta Nº8"; descrição com a modalidade e o prêmio principal em milhões.
        assertEquals("Aposta Nº8", tituloDaAposta(aposta(Modalidade.MEGA_SENA, 8, 3073)))
        assertEquals("Quina · 55M", descricaoDaAposta(aposta(Modalidade.QUINA, 1, 7148, premio = 5_500_000_000)))
        assertEquals("Lotofácil · 1,7M", descricaoDaAposta(aposta(Modalidade.LOTOFACIL, 2, 3809, premio = 170_000_000)))
        assertEquals("Quina", descricaoDaAposta(aposta(Modalidade.QUINA, 1, 0)))
    }

    @Test
    fun historicoDoFinanceiro() {
        val a = aposta(Modalidade.QUINA, 1, 7148, premio = 5_500_000_000)
        val pessoas = listOf(Pessoa("p1", "A", "A", "x", true), Pessoa("p2", "B", "B", "y"), Pessoa("p3", "C", "C", "z"))
        val inicial = Lancamento("l0", hoje.withDayOfMonth(1), 0, TipoLancamento.SALDO_INICIAL, 1250)
        val mensal = Lancamento("l1", hoje.withDayOfMonth(1), 1, TipoLancamento.APORTE, 15000)
        val adicional = Lancamento("l2", hoje, 2, TipoLancamento.APORTE, 900, eventual = true)
        val jogo = Lancamento("l3", hoje, 3, TipoLancamento.APOSTA, -300, apostaId = a.id)
        val premio = Lancamento("l4", hoje, 4, TipoLancamento.PREMIO, 700, apostaId = a.id)
        val rateio = Lancamento("l5", hoje, 5, TipoLancamento.RATEIO, -15048, rateioNumero = 7)
        val e = Estado(
            pessoas = pessoas,
            apostas = listOf(a),
            lancamentos = listOf(inicial, mensal, adicional, jogo, premio, rateio),
            rateios = listOf(Rateio(7, hoje, 0, 0, 5016, criadoEm = 5, numeroNoMes = 1)),
        )
        assertEquals(listOf("Saldo inicial", "Aporte Nº1", "Aporte Nº2", "Aposta Nº1", "Prêmio Nº1", "Rateio Nº1"), e.lancamentos.map { tarjaLancamento(e, it) })
        assertEquals(listOf(null, "Mensal", "Adicional", "Quina · 55M", "Quina · 55M", "3 pessoas"), e.lancamentos.map { descricaoLancamento(e, it) })
        assertEquals(
            listOf("· R$ 12,50", "+ R$ 150,00", "+ R$ 9,00", "- R$ 3,00", "+ R$ 7,00", "- R$ 150,48"),
            e.lancamentos.map { valorDoLancamento(it) },
        )
    }

    @Test
    fun linhaDosIndices() {
        val k = ResultadoK(
            numero = 7148, especial = false, acumuladoPrincipal = 0.0, reserva = 0, herancaFaixas = emptyMap(),
            nAnterior = 0.0, k1 = 0.6894, k2 = 0.0912,
        )
        val quina = Indices.Item(Modalidade.QUINA, null, k, 34.2)
        assertEquals(listOf("k1" to "0,689", "k2" to "0,091", "odds" to "1:34"), indicesDoCartao(quina))
        assertEquals("k1  0,689    k2  0,091    odds  1:34", linhaIndices(quina))
        assertEquals("k1  —    k2  —    odds  1:34", linhaIndices(Indices.Item(Modalidade.QUINA, null, null, 34.2)))
        assertNull(indicesDoCartao(Indices.Item(Modalidade.LOTECA, null, null, null)))
        assertEquals("Sem referência", linhaIndices(Indices.Item(Modalidade.LOTECA, null, null, null)))
        assertEquals("Sem referência", linhaIndices(Indices.Item(Modalidade.FEDERAL, null, null, null)))
    }

    @Test
    fun tituloDoCartaoDosJogos() {
        val ultimo = Concurso(
            modalidade = Modalidade.QUINA, numero = 7147, data = hoje.minusDays(1), dezenas = emptyList(),
            proximoNumero = 7148, proximaData = hoje, premioEstimadoProximo = 5_700_000_000,
        )
        assertEquals("Quina · 57M", tituloDoCartao(Indices.Item(Modalidade.QUINA, ultimo, null, 34.2)))
        assertEquals("Quina", tituloDoCartao(Indices.Item(Modalidade.QUINA, null, null, 34.2)))
        // Preço de Ajustes diferente do dos parâmetros: aviso.
        assertTrue(Indices.Item(Modalidade.MEGA_SENA, null, null, null, precoAjustes = 700, precoParametros = 600).precoDiferente)
        assertTrue(!Indices.Item(Modalidade.MEGA_SENA, null, null, null, precoAjustes = 600, precoParametros = 600).precoDiferente)
        assertTrue(!Indices.Item(Modalidade.FEDERAL, null, null, null, precoAjustes = null, precoParametros = null).precoDiferente)
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

package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.ModelosDoTermo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Simula o mês de junho do exemplo do termo, do aporte ao extrato. */
class OperacoesTest {

    private val junho = YearMonth.of(2026, 6)
    private fun d(dia: Int) = LocalDate.of(2026, 6, dia)
    private var relogio = 1_000L
    private fun agora() = relogio++.also { relogio += 10 }

    private val pessoas = listOf(
        Pessoa("p1", "Fulano de Tal", "Fulano", "chave1", administrador = true),
        Pessoa("p2", "Beto de Tal", "Beto", "chave2"),
        Pessoa("p3", "Beltrano de Tal", "Beltrano", "chave3"),
    )

    private fun inicial() = Estado(
        cadastroConcluido = true,
        pessoas = pessoas,
        proximoJogo = 132,
        proximoRateio = 1,
        lancamentos = listOf(
            Lancamento("ini", LocalDate.of(2026, 5, 31), 0, TipoLancamento.SALDO_INICIAL, 1000),
        ),
    )

    private fun aposta(e: Estado, dia: Int, m: Modalidade, valor: Long, sorteio: Int = dia + 1): Pair<Estado, Aposta> {
        val a = Aposta(
            id = novoId(), numero = 0, modalidade = m, concurso = 3000 + dia, dataAposta = d(dia),
            dataSorteio = d(sorteio), valor = valor, criadaEm = agora(),
        )
        val comRascunho = Operacoes.salvarRascunho(e, a)
        val registrado = Operacoes.registrarAposta(comRascunho, a.id, agora())
        return registrado to registrado.apostas.first { it.id == a.id }
    }

    @Test
    fun mesDeJunhoDoTermo() {
        var e = inicial()
        e = Operacoes.registrarAportes(e, d(1), pessoas.map { it.id to 1000L }, eventual = false, agora = agora())
        assertEquals(4000L, Operacoes.saldo(e))

        val (e1, a132) = aposta(e, 1, Modalidade.LOTOFACIL, 350, sorteio = 3); e = e1
        assertEquals(132, a132.numero)
        assertFalse(a132.eventual)
        assertEquals(133, e.proximoJogo)

        e = Operacoes.confirmarApuracao(e, a132.id, Apuracao("12/15", 1400, conferidaEm = agora()), d(4), agora())
        // O prêmio entra na data do sorteio (03/06), como no extrato do termo.
        assertEquals(d(3), e.lancamentos.last().data)

        e = aposta(e, 4, Modalidade.QUINA, 300).first
        e = aposta(e, 7, Modalidade.QUINA, 300).first
        e = aposta(e, 10, Modalidade.SUPER_SETE, 250).first
        e = aposta(e, 13, Modalidade.LOTOMANIA, 300).first

        // Rateio em 14/06: faltam 16, 19, 22, 25 e 28 -> 5 × R$ 3,00.
        val s = Operacoes.sugestaoProvisionado(e, d(14))
        assertEquals(5, s.apostasRestantes)
        assertEquals(1500L, s.provisionado)
        assertEquals(3900L, Operacoes.saldo(e))
        e = Operacoes.registrarRateio(e, d(14), s.provisionado, cota = 800, comprovantes = emptyList(), agora = agora())
        assertEquals(2, e.proximoRateio)
        assertEquals(1500L, Operacoes.saldo(e))

        e = aposta(e, 16, Modalidade.LOTOMANIA, 300).first
        e = aposta(e, 19, Modalidade.QUINA, 300).first
        e = aposta(e, 22, Modalidade.SUPER_SETE, 250).first
        e = aposta(e, 25, Modalidade.LOTOMANIA, 300).first
        e = aposta(e, 28, Modalidade.TIMEMANIA, 350).first
        assertEquals(0L, Operacoes.saldo(e))

        // Fim do mês: a caixinha mostra R$ 0,02 -> rendimento de 2 centavos.
        val c = Operacoes.conferirSaldo(e, junho, informado = 2)
        assertEquals(2L, c.rendimento)
        assertNull(c.alerta)
        e = Operacoes.registrarExtrato(e, junho, informado = 2, hoje = d(30), agora = agora())

        assertEquals(ModelosDoTermo.EXTRATO, Operacoes.textoExtrato(e, junho))

        // Refazer o extrato do mesmo mês substitui o rendimento, sem duplicar.
        e = Operacoes.registrarExtrato(e, junho, informado = 3, hoje = d(30), agora = agora())
        assertEquals(3L, Operacoes.rendimentoDoMes(e, junho))
        assertEquals(1, e.extratos.size)
    }

    @Test
    fun apostaEventual() {
        var e = inicial()
        // 17/10 não é dia programado.
        assertTrue(Operacoes.ehEventual(e, LocalDate.of(2026, 10, 17)))
        assertFalse(Operacoes.ehEventual(e, d(19)))
        e = aposta(e, 19, Modalidade.QUINA, 300).first
        // A segunda aposta no mesmo dia programado é eventual.
        assertTrue(Operacoes.ehEventual(e, d(19)))
        val (e2, segunda) = aposta(e, 19, Modalidade.MEGA_SENA, 600)
        assertTrue(segunda.eventual)
        assertTrue(Operacoes.textoAposta(e2, segunda).startsWith("- APOSTA EVENTUAL #133 "))
    }

    @Test
    fun desfazerApostaDevolveSaldoENumero() {
        var e = inicial()
        val (e1, a) = aposta(e, 4, Modalidade.QUINA, 300); e = e1
        assertEquals(700L, Operacoes.saldo(e))
        e = Operacoes.desfazerAposta(e, a.id)
        assertEquals(1000L, Operacoes.saldo(e))
        assertEquals(132, e.proximoJogo)
        assertFalse(e.apostas.first().registrada)
    }

    @Test
    fun mensagemApostaUsaSaldoDeAntes() {
        var e = inicial().copy(lancamentos = listOf(Lancamento("ini", d(1), 0, TipoLancamento.SALDO_INICIAL, 4002)))
        val rascunho = Aposta(
            id = "a1", numero = 0, modalidade = Modalidade.LOTOFACIL, concurso = 3060, dataAposta = d(1),
            dataSorteio = d(3), premioEstimado = 200_000_000, valor = 350, criadaEm = 1,
        )
        e = Operacoes.salvarRascunho(e, rascunho)
        // Prévia do rascunho já mostra o próximo número e o saldo atual.
        assertEquals(ModelosDoTermo.APOSTA, Operacoes.textoAposta(e, rascunho))
        e = Operacoes.registrarAposta(e, "a1", agora())
        // Depois de registrada, a mensagem continua igual (saldo de antes da aposta).
        assertEquals(ModelosDoTermo.APOSTA, Operacoes.textoAposta(e, e.apostas.first()))
    }

    @Test
    fun alertasDoExtrato() {
        val e = inicial()
        assertNotNull(Operacoes.conferirSaldo(e, junho, informado = 900).alerta)
        assertNotNull(Operacoes.conferirSaldo(e, junho, informado = 2000).alerta)
        assertNull(Operacoes.conferirSaldo(e, junho, informado = 1009).alerta)
    }

    @Test
    fun desfazerRateioDevolveNumero() {
        var e = inicial()
        e = Operacoes.registrarRateio(e, d(14), 0, 300, emptyList(), agora())
        assertEquals(100L, Operacoes.saldo(e))
        e = Operacoes.desfazerRateio(e, 1)
        assertEquals(1000L, Operacoes.saldo(e))
        assertEquals(1, e.proximoRateio)
    }
}

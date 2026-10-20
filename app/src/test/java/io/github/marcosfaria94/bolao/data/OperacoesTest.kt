package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.ModelosDoTermo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Simula o mês de junho do exemplo do termo, do aporte ao extrato, e as regras de registro. */
class OperacoesTest {

    private val junho = YearMonth.of(2026, 6)
    private fun d(dia: Int) = LocalDate.of(2026, 6, dia)
    private var relogio = 1_000L
    private fun agora() = relogio.also { relogio += 10 }

    private val pessoas = listOf(
        Pessoa("p1", "Fulano de Tal", "Fulano", "chave1", administrador = true),
        Pessoa("p2", "Beto de Tal", "Beto", "chave2"),
        Pessoa("p3", "Beltrano de Tal", "Beltrano", "chave3"),
    )

    private fun inicial() = Estado(
        cadastroConcluido = true,
        cadastradoEm = d(1),
        pessoas = pessoas,
        proximoRateio = 1,
        lancamentos = listOf(
            Lancamento("ini", d(1), 0, TipoLancamento.SALDO_INICIAL, 1000),
        ),
    )

    private fun anexo(nome: String) = Arquivo(nome, nome, "application/pdf", 0, sha256 = "AB")

    private fun aposta(e: Estado, dia: Int, m: Modalidade, valor: Long, sorteio: Int = dia + 1, premio: Long? = null): Pair<Estado, Aposta> {
        val a = Aposta(
            id = novoId(), numero = 0, modalidade = m, concurso = 3000 + dia, dataAposta = d(dia),
            dataSorteio = d(sorteio), premioEstimado = premio, valor = valor, criadaEm = agora(), comprovante = anexo("caixa-$dia.pdf"),
        )
        val comRascunho = Operacoes.salvarRascunho(e, a)
        val registrado = Operacoes.registrarAposta(comRascunho, a.id, d(dia), agora())
        return registrado to registrado.apostas.first { it.id == a.id }
    }

    @Test
    fun mesDeJunhoDoTermo() {
        var e = inicial()
        assertTrue(Operacoes.aporteMensalPendente(e, d(1)))
        // Aporte mensal (R$ 10,00 de Ajustes × 3, no dia 1º).
        e = Operacoes.registrarAporteMensal(e, d(1), agora())
        assertEquals(4000L, Operacoes.saldo(e))
        assertFalse(Operacoes.aporteMensalPendente(e, d(1)))
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarAporteMensal(e, d(2), agora()) }
        val cAporte = e.comprovantes.single()
        assertEquals("APORTE - 01/06/2026", cAporte.titulo)
        assertTrue(cAporte.envioDispensado)

        val (e1, a1) = aposta(e, 1, Modalidade.LOTOFACIL, 350, sorteio = 3, premio = 200_000_000); e = e1
        assertEquals(1, a1.numero)
        val cAposta = e.comprovantes.last()
        assertEquals(ModelosDoTermo.TITULO_APOSTA, cAposta.titulo)
        assertEquals(
            "APOSTA: Nº1/JUN\nDATA: 01/06/2026\nMODALIDADE: LOTOFÁCIL\nCONCURSO: 3001\nPRÊMIO PRINCIPAL: 2 MILHÕES\n" +
                "SORTEIO: 03/06/2026\nNÚMEROS: ...\n\nCONTABILIDADE DO GRUPO\n" +
                "SALDO ATUAL:   R$ 40,00\nAPOSTA.....: - R$  3,50\nSALDO FINAL: = R$ 36,50",
            cAposta.texto,
        )
        assertEquals(listOf("caixa-1.pdf"), cAposta.anexos.map { it.nome })
        // Uma aposta registrada não muda mais.
        assertThrows(IllegalArgumentException::class.java) { Operacoes.salvarRascunho(e, a1.copy(registrada = false, valor = 1)) }

        e = Operacoes.confirmarApuracao(e, a1.id, Apuracao("12/15", 1400, conferidaEm = agora()), d(4), agora())
        // O prêmio entra na data do sorteio (03/06), como no extrato do termo.
        assertEquals(d(3), e.lancamentos.last().data)
        assertEquals("APURAÇÃO Nº 1 - 04/06/26", e.comprovantes.last().titulo)
        assertEquals(
            "APOSTA: Nº1/JUN\nDATA: 01/06/2026\n\nCONTABILIDADE DO GRUPO\n" +
                "SALDO ATUAL:   R$ 36,50\nPRÊMIO.....: + R$ 14,00\nSALDO FINAL: = R$ 50,50",
            e.comprovantes.last().texto,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Operacoes.confirmarApuracao(e, a1.id, Apuracao("12/15", 1400, conferidaEm = 0), d(4), agora())
        }

        e = aposta(e, 4, Modalidade.QUINA, 300).first
        e = aposta(e, 7, Modalidade.QUINA, 300).first
        e = aposta(e, 10, Modalidade.SUPER_SETE, 250).first
        e = aposta(e, 13, Modalidade.LOTOMANIA, 300).first
        assertEquals(listOf(1, 2, 3, 4, 5), e.apostas.map { it.numero })

        // Rateio em 14/06: faltam 16, 19, 22, 25 e 28 -> 5 × R$ 3,00; Extra = 39,00 − 15,00.
        val p = Operacoes.provisao(e, d(14))
        assertEquals(5, p.apostasRestantes)
        assertEquals(1500L, p.provisionado)
        assertEquals(3900L, Operacoes.saldo(e))
        assertEquals(2400L, Operacoes.extra(e, d(14)))
        val pix = pessoas.map { anexo("pix-${it.apelido}.png") }
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarRateio(e, d(14), 2401, pix, agora()) }
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarRateio(e, d(14), 2400, pix.take(2), agora()) }
        e = Operacoes.registrarRateio(e, d(14), 2400, pix, agora())
        assertEquals(1500L, Operacoes.saldo(e))
        assertEquals(800L, e.rateios.single().cota)
        assertEquals("RATEIO - 14/06/26", e.comprovantes.last().titulo)
        // Sem extra, não há o que ratear.
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarRateio(e, d(14), 1, pix, agora()) }

        e = aposta(e, 16, Modalidade.LOTOMANIA, 300).first
        e = aposta(e, 19, Modalidade.QUINA, 300).first
        e = aposta(e, 22, Modalidade.SUPER_SETE, 250).first
        e = aposta(e, 25, Modalidade.LOTOMANIA, 300).first
        e = aposta(e, 28, Modalidade.TIMEMANIA, 350).first
        assertEquals(0L, Operacoes.saldo(e))

        // O extrato só sai a partir do último dia do mês.
        assertNotNull(Operacoes.impedimentoDoExtrato(e, junho, d(29)))
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarExtrato(e, junho, 2, d(29), agora()) }
        assertNull(Operacoes.impedimentoDoExtrato(e, junho, d(30)))
        assertEquals(listOf(junho), Operacoes.extratosPendentes(e, d(30)))
        assertEquals(emptyList<YearMonth>(), Operacoes.extratosPendentes(e, d(29)))

        // Fim do mês: a caixinha mostra R$ 0,02 -> rendimento de 2 centavos.
        val c = Operacoes.conferirSaldo(e, junho, informado = 2)
        assertEquals(2L, c.rendimento)
        assertNull(c.alerta)
        e = Operacoes.registrarExtrato(e, junho, informado = 2, hoje = d(30), agora = agora())
        val extrato = e.comprovantes.last()
        assertEquals(ModelosDoTermo.TITULO_EXTRATO, extrato.titulo)
        assertEquals(ModelosDoTermo.EXTRATO, extrato.texto)
        assertEquals(ModelosDoTermo.EXTRATO, Operacoes.textoExtrato(e, junho))
        assertTrue(Operacoes.extratosPendentes(e, d(30)).isEmpty())

        // Um extrato por mês: não dá para refazer.
        assertNotNull(Operacoes.impedimentoDoExtrato(e, junho, d(30)))
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarExtrato(e, junho, 3, d(30), agora()) }

        // Corrente de autenticação íntegra; mudar um registro antigo quebra no lugar certo.
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(e))
        val adulterado = e.copy(comprovantes = e.comprovantes.mapIndexed { i, x -> if (i == 3) x.copy(texto = x.texto + " ") else x })
        assertEquals(3, Operacoes.primeiraAutenticacaoQuebrada(adulterado))
    }

    @Test
    fun numeracaoRecomecaNoMes() {
        var e = inicial().copy(lancamentos = listOf(Lancamento("ini", d(1), 0, TipoLancamento.SALDO_INICIAL, 10_000)))
        e = aposta(e, 28, Modalidade.QUINA, 300).first
        e = aposta(e, 28, Modalidade.MEGA_SENA, 600).first
        val julho = Aposta(
            id = "j", numero = 0, modalidade = Modalidade.QUINA, concurso = 1, dataAposta = d(30),
            valor = 300, criadaEm = agora(), comprovante = anexo("j.pdf"),
        )
        // O rascunho criado em junho e registrado em 01/07 vira a Nº1 de julho, com a data do registro.
        e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, julho), "j", LocalDate.of(2026, 7, 1), agora())
        val j = e.apostas.first { it.id == "j" }
        assertEquals(1, j.numero)
        assertEquals(LocalDate.of(2026, 7, 1), j.dataAposta)
        assertEquals(listOf(1, 2, 1), e.apostas.map { it.numero })
        assertEquals("APOSTA Nº 1 -  01/07/2026", e.comprovantes.last().titulo)
    }

    @Test
    fun apostaSemComprovanteNaoRegistra() {
        val a = Aposta(id = "a", numero = 0, modalidade = Modalidade.QUINA, concurso = 1, dataAposta = d(1), valor = 300, criadaEm = 0)
        val e = Operacoes.salvarRascunho(inicial(), a)
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarAposta(e, "a", d(1), 1) }
    }

    @Test
    fun aporteDoMesDoCadastro() {
        // Cadastro no meio do mês: o aporte daquele mês não é cobrado (já está no saldo inicial).
        val e = inicial().copy(cadastradoEm = d(19))
        assertFalse(Operacoes.aporteMensalPendente(e, d(19)))
        assertTrue(Operacoes.aporteMensalPendente(e, LocalDate.of(2026, 7, 1)))
        // Cadastro no dia 1º: cobra.
        assertTrue(Operacoes.aporteMensalPendente(inicial().copy(cadastradoEm = d(1)), d(1)))
    }

    @Test
    fun aporteAdicionalVaiAoGrupo() {
        val e = Operacoes.registrarAporteAdicional(inicial(), d(15), 1500, agora())
        val c = e.comprovantes.single()
        assertFalse(c.envioDispensado)
        assertEquals("APORTE - 15/06/2026", c.titulo)
        assertEquals(1, Operacoes.comprovantesPendentesDeEnvio(e).size)
        val enviado = Operacoes.registrarEnvio(e, c.id, agora())
        assertTrue(Operacoes.comprovantesPendentesDeEnvio(enviado).isEmpty())
    }

    @Test
    fun premioDeMesFechadoEntraHoje() {
        var e = inicial().copy(lancamentos = listOf(Lancamento("ini", d(1), 0, TipoLancamento.SALDO_INICIAL, 1000)))
        val (e1, a) = aposta(e, 28, Modalidade.QUINA, 300, sorteio = 30); e = e1
        e = Operacoes.registrarExtrato(e, junho, informado = 700, hoje = d(30), agora = agora())
        e = Operacoes.confirmarApuracao(e, a.id, Apuracao("2/5", 425, conferidaEm = 0), LocalDate.of(2026, 7, 1), agora())
        assertEquals(LocalDate.of(2026, 7, 1), e.lancamentos.last().data)
    }

    @Test
    fun alertasDoExtrato() {
        val e = inicial()
        assertNotNull(Operacoes.conferirSaldo(e, junho, informado = 900).alerta)
        assertNotNull(Operacoes.conferirSaldo(e, junho, informado = 2000).alerta)
        assertNull(Operacoes.conferirSaldo(e, junho, informado = 1009).alerta)
    }

    @Test
    fun extincao() {
        var e = inicial().copy(lancamentos = listOf(Lancamento("ini", d(1), 0, TipoLancamento.SALDO_INICIAL, 5)))
        // R$ 0,05 ÷ 3 = 0,0166... -> 0,02 para cada um; o Administrador cobre R$ 0,01.
        val div = Operacoes.divisaoFinal(e)
        assertEquals(2L, div.cota)
        assertEquals(6L, div.total)
        assertEquals(1L, div.complemento)

        val (e1, a) = aposta(e.copy(lancamentos = e.lancamentos + Lancamento("x", d(1), 1, TipoLancamento.APORTE, 300)), 4, Modalidade.QUINA, 300)
        e = e1
        assertTrue(Operacoes.impedimentosDaExtincao(e).any { it.contains("sem apuração") })
        assertTrue(Operacoes.impedimentosDaExtincao(e).any { it.contains("não enviados") })
        e = Operacoes.confirmarApuracao(e, a.id, Apuracao("0/5", 0, conferidaEm = 0), d(6), agora())
        e.comprovantes.filter { !it.envioDispensado }.forEach { e = Operacoes.registrarEnvio(e, it.id, agora()) }
        assertEquals(emptyList<String>(), Operacoes.impedimentosDaExtincao(e))

        e = Operacoes.iniciarExtincao(e, agora())
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarExtincao(e, d(7), agora()) }
        pessoas.forEach { e = Operacoes.anexarNaExtincao(e, it.id, anexo("final-${it.apelido}.png")) }
        e = Operacoes.registrarExtincao(e, d(7), agora())
        assertEquals(0L, Operacoes.saldo(e))
        val c = e.comprovantes.last()
        assertEquals(TipoComprovante.EXTINCAO, c.tipo)
        assertEquals("EXTINÇÃO - 07/06/26", c.titulo)
        assertEquals(3, c.anexos.size)
        assertNotNull(e.extincao!!.registradaEm)
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(e))
    }
}

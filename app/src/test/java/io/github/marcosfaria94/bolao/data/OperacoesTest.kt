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
        val registrado = Operacoes.registrarApostaComComprovante(comRascunho, a.id, d(dia), agora())
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
        assertEquals(ModelosDoTermo.TITULO_APORTE, cAporte.titulo)
        assertEquals(
            "APORTE Nº1 - JUN/2026\nDATA: 01/06/2026\nTIPO DE APORTE: OBRIGATÓRIO\n\nFINANCEIRO -\n" +
                "SALDO ATUAL:   R$ 10,00\nAPORTES....: + R$ 30,00\nSALDO FINAL: = R$ 40,00",
            cAporte.texto,
        )
        assertTrue(cAporte.envioDispensado)

        val (e1, a1) = aposta(e, 1, Modalidade.LOTOFACIL, 350, sorteio = 3, premio = 200_000_000); e = e1
        assertEquals(1, a1.numero)
        val cAposta = e.comprovantes.last()
        assertEquals(ModelosDoTermo.TITULO_APOSTA, cAposta.titulo)
        assertEquals(
            "APOSTA: Nº1 - JUN/2026\nDATA: 01/06/2026\nMODALIDADE: LOTOFÁCIL\nCONCURSO: 3001\nPRÊMIO PRINCIPAL: 2 MILHÕES\n" +
                "SORTEIO: 03/06/2026\nNÚMEROS: ...\n\nFINANCEIRO -\n" +
                "SALDO ATUAL:   R$ 40,00\nAPOSTA.....: - R$  3,50\nSALDO FINAL: = R$ 36,50",
            cAposta.texto,
        )
        assertEquals(listOf("caixa-1.pdf"), cAposta.anexos.map { it.nome })
        // Uma aposta registrada não muda mais.
        assertThrows(IllegalArgumentException::class.java) { Operacoes.salvarRascunho(e, a1.copy(registrada = false, valor = 1)) }

        val acertos = anexo("acertos-1.png")
        e = Operacoes.confirmarApuracao(e, a1.id, Apuracao("12/15", 1400, conferidaEm = agora()), d(4), agora(), acertos)
        // O prêmio entra na data do sorteio (03/06), como no extrato do termo.
        assertEquals(d(3), e.lancamentos.last().data)
        assertEquals("APURAÇÃO Nº 1 - 04/06/26", e.comprovantes.last().titulo)
        assertEquals(
            "APOSTA: Nº1 - JUN/2026\nDATA: 01/06/2026\nMODALIDADE: LOTOFÁCIL\nCONCURSO: 3001\nPRÊMIO PRINCIPAL: 2 MILHÕES\n" +
                "APURAÇÃO: 04/06/2026\nRESULTADO: (VIDE ANEXO)\n\nFINANCEIRO -\n" +
                "SALDO ATUAL:   R$ 36,50\nPRÊMIO.....: + R$ 14,00\nSALDO FINAL: = R$ 50,50",
            e.comprovantes.last().texto,
        )
        assertEquals(listOf(acertos), e.comprovantes.last().anexos)
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
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarRateioComPix(e, d(14), 2401, pix, agora()) }
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarRateioComPix(e, d(14), 2400, pix.take(2), agora()) }
        e = Operacoes.registrarRateioComPix(e, d(14), 2400, pix, agora())
        assertEquals(1500L, Operacoes.saldo(e))
        assertEquals(800L, e.rateios.single().cota)
        assertEquals(ModelosDoTermo.TITULO_RATEIO, e.comprovantes.last().titulo)
        assertEquals(
            "RATEIO Nº1 - JUN/2026\nDATA: 14/06/2026\n\nFINANCEIRO -\n" +
                "SALDO ATUAL:   R$ 39,00\nRATEIO.....: - R$ 24,00\nSALDO FINAL: = R$ 15,00",
            e.comprovantes.last().texto,
        )
        assertEquals(1, e.rateios.single().numeroNoMes)
        // Sem extra, não há o que ratear.
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarRateioComPix(e, d(14), 1, pix, agora()) }

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
        e = Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(e, julho), "j", LocalDate.of(2026, 7, 1), agora())
        val j = e.apostas.first { it.id == "j" }
        assertEquals(1, j.numero)
        assertEquals(LocalDate.of(2026, 7, 1), j.dataAposta)
        assertEquals(listOf(1, 2, 1), e.apostas.map { it.numero })
        assertEquals("APOSTA Nº 1 -  01/07/2026", e.comprovantes.last().titulo)
    }

    private fun semComprovante(id: String, dia: Int, m: Modalidade = Modalidade.QUINA, concurso: Int = 7000) = Aposta(
        id = id, numero = 0, modalidade = m, concurso = concurso, dataAposta = d(dia), dataSorteio = d(dia + 1),
        valor = 300, numeros = listOf(1, 2, 3, 4, 5), criadaEm = agora(),
    )

    @Test
    fun apostaRegistradaEsperaOComprovanteEPodeSerDescartada() {
        var e = Operacoes.registrarAporteMensal(inicial(), d(1), agora())
        listOf("a", "b", "c").forEach { id ->
            e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, semComprovante(id, 4)), id, d(4), agora())
        }
        // Registradas sem o comprovante: o valor já saiu do saldo, mas não há comprovante do bolão.
        assertEquals(listOf(1, 2, 3), e.apostas.map { it.numero })
        assertEquals(4000L - 900, Operacoes.saldo(e))
        assertTrue(e.apostas.all { Operacoes.aguardandoComprovante(it) })
        assertEquals(1, e.comprovantes.size)
        assertEquals(listOf("a", "b", "c"), Operacoes.apostasSemComprovante(e).map { it.id })
        // Sem a conferência não dá para apurar nem fechar o mês.
        assertThrows(IllegalArgumentException::class.java) {
            Operacoes.confirmarApuracao(e, "a", Apuracao("0/5", 0, conferidaEm = 0), d(6), agora())
        }
        assertNotNull(Operacoes.impedimentoDoExtrato(e, junho, d(30)))
        assertTrue(Operacoes.impedimentosDaExtincao(e).any { it.contains("esperando") })
        assertThrows(IllegalArgumentException::class.java) { Operacoes.aprovarAposta(e, "a", agora()) }

        // Descartar a do meio: o valor volta e a de depois sobe um número.
        e = Operacoes.descartarAposta(e, "b")
        assertEquals(listOf("a" to 1, "c" to 2), e.apostas.map { it.id to it.numero })
        assertEquals(4000L - 600, Operacoes.saldo(e))
        assertTrue(e.lancamentos.none { it.apostaId == "b" })

        // Comprovante anexado (com os números corrigidos) e conferência: sai o comprovante do bolão.
        val caixa = anexo("caixa-a.pdf")
        val ajustada = e.apostas.first { it.id == "a" }.copy(numeros = listOf(6, 7, 8, 9, 10))
        e = Operacoes.anexarNaAposta(e, "a", caixa, ajustada, listOf("Números: no app 01 02 03 04 05; no comprovante 06 07 08 09 10."))
        assertTrue(Operacoes.aguardandoConferencia(e.apostas.first { it.id == "a" }))
        assertEquals(listOf("a"), Operacoes.apostasParaConferir(e).map { it.id })
        e = Operacoes.desanexarDaAposta(e, "a")
        assertTrue(Operacoes.aguardandoComprovante(e.apostas.first { it.id == "a" }))
        e = Operacoes.anexarNaAposta(e, "a", caixa, ajustada)
        e = Operacoes.aprovarAposta(e, "a", agora())
        val a = e.apostas.first { it.id == "a" }
        assertFalse(a.pendenteDeAnexo)
        assertEquals(listOf(6, 7, 8, 9, 10), a.numeros)
        val c = e.comprovantes.last()
        assertEquals(TipoComprovante.APOSTA, c.tipo)
        assertEquals("APOSTA Nº 1 -  04/06/2026", c.titulo)
        assertTrue(c.texto.contains("SALDO ATUAL:   R$ 40,00"))
        assertEquals(listOf(caixa), c.anexos)
        // Conferida, não pode mais ser descartada; a "c" ainda pode (e a "a" não muda de número).
        assertThrows(IllegalArgumentException::class.java) { Operacoes.descartarAposta(e, "a") }
        e = Operacoes.descartarAposta(e, "c")
        assertEquals(listOf("a" to 1), e.apostas.map { it.id to it.numero })
        // Com o buraco depois de uma conferida, o próximo número não repete nenhum.
        assertEquals(2, Operacoes.proximoNumero(e, d(7)))
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(e))
        assertTrue(Integridade.registrosSemComprovante(e).isEmpty())
    }

    @Test
    fun descarteNaoRenumeraDepoisDeUmaConferida() {
        var e = Operacoes.registrarAporteMensal(inicial(), d(1), agora())
        listOf("a", "b").forEach { id -> e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, semComprovante(id, 4)), id, d(4), agora()) }
        e = Operacoes.aprovarAposta(Operacoes.anexarNaAposta(e, "b", anexo("b.pdf")), "b", agora())
        e = Operacoes.descartarAposta(e, "a")
        // A Nº2 já tem comprovante com o número dela: fica o buraco, e a próxima é a Nº3.
        assertEquals(listOf("b" to 2), e.apostas.map { it.id to it.numero })
        assertEquals(3, Operacoes.proximoNumero(e, d(7)))
    }

    @Test
    fun rateioEsperaOsPixEAConferencia() {
        var e = Operacoes.registrarAporteMensal(inicial().copy(lancamentos = listOf(Lancamento("ini", d(1), 0, TipoLancamento.SALDO_INICIAL, 5000))), d(1), agora())
        e = Operacoes.registrarRateio(e, d(14), 2400, agora())
        val r = e.rateios.single()
        assertTrue(r.pendenteDeAnexo)
        assertEquals(800L, r.cota)
        assertEquals(8000L - 2400, Operacoes.saldo(e))
        assertEquals(listOf(r.numero), Operacoes.rateiosSemPix(e).map { it.numero })
        // Um Pix por integrante; com todos, vai para a conferência.
        pessoas.take(2).forEach { p -> e = Operacoes.anexarPix(e, r.numero, p.id, anexo("pix-${p.apelido}.png")) }
        assertThrows(IllegalArgumentException::class.java) { Operacoes.aprovarRateio(e, r.numero, agora()) }
        e = Operacoes.anexarPix(e, r.numero, "p3", anexo("pix-Beltrano.png"))
        assertEquals(listOf(r.numero), Operacoes.rateiosParaConferir(e).map { it.numero })
        // Descartar antes da conferência: o valor volta.
        val descartado = Operacoes.descartarRateio(e, r.numero)
        assertEquals(8000L, Operacoes.saldo(descartado))
        assertTrue(descartado.rateios.isEmpty())
        // Aprovado: o comprovante do bolão com os 3 Pix na ordem dos integrantes.
        e = Operacoes.aprovarRateio(e, r.numero, agora())
        val c = e.comprovantes.last()
        assertEquals(TipoComprovante.RATEIO, c.tipo)
        assertEquals("RATEIO Nº1 - 14/06/26", c.titulo)
        assertEquals(listOf("pix-Fulano.png", "pix-Beto.png", "pix-Beltrano.png"), c.anexos.map { it.nome })
        assertTrue(c.texto.contains("SALDO ATUAL:   R$ 80,00"))
        assertFalse(e.rateios.single().pendenteDeAnexo)
        assertThrows(IllegalArgumentException::class.java) { Operacoes.descartarRateio(e, r.numero) }
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(e))
    }

    @Test
    fun concursoQueJaPassouAcompanhaACaixa() {
        var e = Operacoes.registrarAporteMensal(inicial(), d(1), agora())
        // Em andamento, no concurso 3075; registrada depois do sorteio do 3075 (o app estava desatualizado).
        val rascunho = semComprovante("r", 25, Modalidade.MEGA_SENA, 3075)
        val atrasada = semComprovante("x", 25, Modalidade.MEGA_SENA, 3075).copy(dataSorteio = d(24))
        e = Operacoes.salvarRascunho(e, rascunho)
        e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, atrasada), "x", d(25), agora())
        // Registrada para o sorteio de amanhã: continua (o comprovante da Caixa diz o concurso).
        val emDia = semComprovante("y", 25, Modalidade.MEGA_SENA, 3076).copy(dataSorteio = d(26))
        e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, emDia), "y", d(25), agora())
        val proximo = Operacoes.ProximoConcurso(3076, d(27), 3_000_000_00)
        val (novo, mudadas) = Operacoes.atualizarConcursos(e, mapOf(Modalidade.MEGA_SENA to proximo))
        assertEquals(setOf("r", "x"), mudadas.map { it.first.id }.toSet())
        assertEquals(3076, novo.apostas.first { it.id == "r" }.concurso)
        assertEquals(d(27), novo.apostas.first { it.id == "x" }.dataSorteio)
        assertEquals(3076, novo.apostas.first { it.id == "y" }.concurso)
        assertEquals(d(26), novo.apostas.first { it.id == "y" }.dataSorteio)
        // A Loteca não muda sozinha (os jogos são outros).
        assertFalse(Operacoes.concursoDesatualizado(semComprovante("l", 25, Modalidade.LOTECA, 1200), 1201))
        // Sem nada a mudar, o estado é o mesmo.
        assertTrue(Operacoes.atualizarConcursos(novo, mapOf(Modalidade.MEGA_SENA to proximo)).second.isEmpty())
    }

    @Test
    fun modoIndividual() {
        var e = Estado(
            cadastroConcluido = true, cadastradoEm = d(1), modo = Modo.INDIVIDUAL,
            pessoas = listOf(Pessoa("eu", "Fulano", "Fulano", "", administrador = true)),
        )
        e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, semComprovante("a", 3)), "a", d(3), agora())
        e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, semComprovante("b", 5)), "b", d(5), agora())
        // Sem comprovante e já valendo: o saldo começa em zero e fica negativo.
        assertEquals(-600L, Operacoes.saldo(e))
        assertTrue(e.apostas.none { it.pendenteDeAnexo })
        assertTrue(e.comprovantes.isEmpty())
        e = Operacoes.confirmarApuracao(e, "a", Apuracao("3/5", 1100, conferidaEm = 0), d(5), agora())
        assertEquals(500L, Operacoes.saldo(e))
        assertTrue(e.comprovantes.isEmpty())
        // Tudo pode ser excluído (a aposta e o prêmio dela).
        e = Operacoes.descartarAposta(e, "a")
        assertEquals(-300L, Operacoes.saldo(e))
        assertEquals(listOf("b" to 1), e.apostas.map { it.id to it.numero })
        assertTrue(Operacoes.impedimentosDaExtincao(e).isEmpty())
        assertTrue(Integridade.registrosSemComprovante(e).isEmpty())
        assertThrows(IllegalArgumentException::class.java) { Operacoes.registrarRateio(e, d(14), 100, agora()) }
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
        assertEquals("APORTE Nº 1 - 15/06/2026", c.titulo)
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

    @Test
    fun numeracaoDeAporteERateioRecomecaNoMes() {
        var e = inicial().copy(lancamentos = listOf(Lancamento("ini", d(1), 0, TipoLancamento.SALDO_INICIAL, 10_000)))
        val pix = pessoas.map { anexo("pix-${it.apelido}.png") }
        // Junho: aporte mensal (Nº1) e um adicional (Nº2); dois rateios (Nº1 e Nº2).
        e = Operacoes.registrarAporteMensal(e, d(1), agora())
        e = Operacoes.registrarAporteAdicional(e, d(10), 900, agora())
        assertEquals("APORTE Nº 2 - 10/06/2026", e.comprovantes.last().titulo)
        assertEquals("APORTE Nº2 - JUN/2026", e.comprovantes.last().texto.lines().first())
        assertEquals("TIPO DE APORTE: ADICIONAL", e.comprovantes.last().texto.lines()[2])
        e = Operacoes.registrarRateioComPix(e, d(20), 300, pix, agora())
        e = Operacoes.registrarRateioComPix(e, d(20), 300, pix, agora())
        assertEquals("RATEIO Nº2 - 20/06/26", e.comprovantes.last().titulo)
        assertEquals("RATEIO Nº2 - JUN/2026", e.comprovantes.last().texto.lines().first())
        // Julho: tudo volta ao Nº1 (a chave interna do rateio continua corrida).
        val julho = LocalDate.of(2026, 7, 1)
        assertEquals("APORTE Nº 1 - 01/07/2026", Operacoes.tituloAporte(e, julho))
        assertEquals("RATEIO Nº1 - 01/07/26", Operacoes.tituloRateio(e, julho))
        e = Operacoes.registrarAporteMensal(e, julho, agora())
        assertEquals("APORTE Nº 1 - 01/07/2026", e.comprovantes.last().titulo)
        e = Operacoes.registrarRateioComPix(e, julho, 300, pix, agora())
        assertEquals("RATEIO Nº1 - 01/07/26", e.comprovantes.last().titulo)
        assertEquals(listOf(1, 2, 3), e.rateios.map { it.numero })
        assertEquals(listOf(1, 2, 1), e.rateios.map { it.numeroNoMes })
        // Nº de cada lançamento nas listas do app.
        val aportes = e.lancamentos.filter { it.tipo == TipoLancamento.APORTE }
        assertEquals(listOf(1, 2, 1), aportes.map { Operacoes.numeroNoMes(e, it) })
        val rateios = e.lancamentos.filter { it.tipo == TipoLancamento.RATEIO }
        assertEquals(listOf(1, 2, 1), rateios.map { Operacoes.numeroNoMes(e, it) })
        assertNull(Operacoes.numeroNoMes(e, e.lancamentos.first()))
        // Rateios de antes da 0.7 (sem o número do mês) também ganham o Nº pela ordem do mês.
        val antigo = e.copy(rateios = e.rateios.map { it.copy(numeroNoMes = 0) })
        assertEquals(listOf(1, 2, 1), rateios.map { Operacoes.numeroNoMes(antigo, it) })
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(e))
    }
}

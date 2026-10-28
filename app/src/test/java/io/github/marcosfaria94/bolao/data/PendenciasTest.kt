package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Modalidade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class PendenciasTest {

    private val fuso = ZoneOffset.ofHours(-3)
    private val pessoas = listOf(Pessoa("p1", "A", "A", "x", true), Pessoa("p2", "B", "B", "y"), Pessoa("p3", "C", "C", "z"))
    private val base = Estado(cadastroConcluido = true, cadastradoEm = LocalDate.of(2026, 10, 1), pessoas = pessoas)
    private val anexo = Arquivo("c.pdf", "c.pdf", "application/pdf", 0)
    private val rateio = listOf(FaixaPremio(1, "5 acertos", 0, 0), FaixaPremio(4, "2 acertos", 100, 425))

    private fun ms(t: LocalDateTime) = t.atZone(fuso).toInstant().toEpochMilli()
    private fun chaves(e: Estado, agora: LocalDateTime) = Pendencias.de(e, agora, fuso).map { it.chave }

    private fun registrar(e: Estado, a: Aposta, hoje: LocalDate, agora: Long = 1) =
        Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(e, a), a.id, hoje, agora)

    @Test
    fun aporteDoMes() {
        val dia1 = LocalDateTime.of(2026, 11, 1, 7, 0)
        val p = Pendencias.de(base, dia1, fuso).single { it.chave == "aporte-2026-11" }
        assertEquals("aporte", p.destino)
        // Na lista desde a meia-noite; o aviso no celular só a partir das 9h.
        assertTrue(Pendencias.paraAvisar(listOf(p), dia1.withHour(8), emptySet()).isEmpty())
        assertEquals(listOf(p), Pendencias.paraAvisar(listOf(p), dia1.withHour(9), emptySet()))
        // Já avisado não repete; registrado, some.
        assertTrue(Pendencias.paraAvisar(listOf(p), dia1.withHour(9), setOf(p.chave)).isEmpty())
        val comAporte = Operacoes.registrarAporteMensal(base, dia1.toLocalDate(), 1)
        assertTrue("aporte-2026-11" !in chaves(comAporte, dia1))
    }

    @Test
    fun diaDeAposta() {
        val dia19 = LocalDateTime.of(2026, 10, 19, 10, 5)
        val comAporte = Operacoes.registrarAporteMensal(base, LocalDate.of(2026, 10, 1), 0)
        assertTrue("aposta-2026-10-19" in chaves(comAporte, dia19))
        assertTrue(chaves(comAporte, LocalDateTime.of(2026, 10, 18, 11, 0)).none { it.startsWith("aposta-") })
        val a = Aposta("a", 0, Modalidade.QUINA, 1, LocalDate.of(2026, 10, 19), valor = 300, criadaEm = 0, comprovante = anexo)
        val e = registrar(comAporte, a, LocalDate.of(2026, 10, 19))
        assertTrue("aposta-2026-10-19" !in chaves(e, dia19))
    }

    @Test
    fun rascunhoEComprovanteNaoEnviado() {
        val criada = LocalDateTime.of(2026, 10, 19, 10, 0)
        val r = Aposta("r", 0, Modalidade.MEGA_SENA, 1, LocalDate.of(2026, 10, 19), valor = 600, criadaEm = ms(criada))
        var e = Operacoes.salvarRascunho(Operacoes.registrarAporteMensal(base, LocalDate.of(2026, 10, 1), 0), r)
        val p = Pendencias.de(e, criada.plusMinutes(5), fuso).single { it.chave == "rascunho-r" }
        assertEquals("Registrar a aposta", p.titulo)
        assertEquals("aposta/r", p.destino)
        // A lista mostra a bola da modalidade (sigla e concurso).
        assertEquals(Modalidade.MEGA_SENA, p.modalidade)
        assertEquals(1, p.concurso)
        // O rascunho já é a pendência do dia: não aparece "Fazer a aposta de hoje".
        assertTrue(chaves(e, criada.plusMinutes(5)).none { it.startsWith("aposta-") })
        // O aviso no celular espera uma hora.
        assertTrue(Pendencias.paraAvisar(listOf(p), criada.plusMinutes(30), emptySet()).isEmpty())
        assertEquals(1, Pendencias.paraAvisar(listOf(p), criada.plusMinutes(61), emptySet()).size)

        // Registrada sem o comprovante da Caixa: sai o rascunho e entra "Anexar o comprovante" (aba Comprovantes).
        e = Operacoes.registrarAposta(e, "r", LocalDate.of(2026, 10, 19), ms(criada.plusMinutes(20)))
        assertTrue(chaves(e, criada.plusMinutes(25)).none { it.startsWith("rascunho-") })
        val anexar = Pendencias.de(e, criada.plusMinutes(25), fuso).single { it.tipo == Pendencia.Tipo.ANEXO }
        assertEquals("anexo-r", anexar.chave)
        assertEquals(Pendencias.ABA_COMPROVANTES, anexar.destino)
        assertTrue(Pendencias.paraAvisar(listOf(anexar), criada.plusMinutes(70), emptySet()).isEmpty())
        assertEquals(1, Pendencias.paraAvisar(listOf(anexar), criada.plusMinutes(81), emptySet()).size)
        // Esperando o comprovante, não entra na apuração automática.
        assertTrue(Pendencias.aguardandoResultado(e.copy(apostas = e.apostas.map { it.copy(dataSorteio = LocalDate.of(2026, 10, 1)) }), criada).isEmpty())
        // Anexado: uma pendência só, a da conferência.
        e = Operacoes.anexarNaAposta(e, "r", anexo.copy(guardadoEm = ms(criada.plusMinutes(21))))
        val conferir = Pendencias.de(e, criada.plusMinutes(25), fuso).single { it.tipo == Pendencia.Tipo.CONFERENCIA }
        assertEquals(Pendencias.ABA_COMPROVANTES, conferir.destino)
        assertTrue(Pendencias.de(e, criada.plusMinutes(25), fuso).none { it.tipo == Pendencia.Tipo.ANEXO })
        // Aprovada: entra o envio do comprovante (até ser aberto no WhatsApp).
        e = Operacoes.aprovarAposta(e, "r", ms(criada.plusMinutes(22)))
        assertTrue(Pendencias.de(e, criada.plusMinutes(25), fuso).none { it.tipo == Pendencia.Tipo.CONFERENCIA })
        val envio = Pendencias.de(e, criada.plusMinutes(25), fuso).single { it.tipo == Pendencia.Tipo.ENVIO }
        val c = e.comprovantes.last()
        assertEquals("comprovante/${c.id}", envio.destino)
        assertEquals("APOSTA Nº 1 -  19/10/2026", envio.texto)
        assertEquals(TipoComprovante.APOSTA, envio.comprovante)
        assertEquals(Modalidade.MEGA_SENA, envio.modalidade)
        e = Operacoes.registrarEnvio(e, c.id, 0)
        assertTrue(Pendencias.de(e, criada.plusMinutes(25), fuso).none { it.tipo == Pendencia.Tipo.ENVIO })
    }

    @Test
    fun extratoNoUltimoDiaEDepois() {
        assertTrue("extrato-2026-10" in chaves(base, LocalDateTime.of(2026, 10, 31, 8, 0)))
        assertTrue("extrato-2026-10" !in chaves(base, LocalDateTime.of(2026, 10, 30, 18, 0)))
        // Esquecido: continua pendente no mês seguinte.
        val p = Pendencias.de(base, LocalDateTime.of(2026, 11, 2, 8, 0), fuso).single { it.chave == "extrato-2026-10" }
        assertEquals("extrato/2026-10", p.destino)
    }

    @Test
    fun ordemDaMaisRecente() {
        val lista = Pendencias.de(base, LocalDateTime.of(2026, 11, 1, 10, 0), fuso)
        // Aporte de novembro (01/11) antes do extrato de outubro (31/10).
        assertEquals(listOf("aporte-2026-11", "aposta-2026-11-01", "extrato-2026-10"), lista.map { it.chave })
        assertTrue(lista.zipWithNext().all { (a, b) -> !a.desde.isBefore(b.desde) })
    }

    @Test
    fun apuracaoAutomatica() {
        val sorteio = LocalDate.of(2026, 10, 21)
        val a = Aposta("a", 0, Modalidade.QUINA, 1, LocalDate.of(2026, 10, 19), dataSorteio = sorteio, valor = 300, criadaEm = 0, comprovante = anexo)
        var e = registrar(base, a, LocalDate.of(2026, 10, 19))
        // O resultado sai à noite (domingo, às 11h).
        assertEquals(0, Pendencias.aguardandoResultado(e, sorteio.atTime(21, 30)).size)
        assertEquals(1, Pendencias.aguardandoResultado(e, sorteio.atTime(22, 10)).size)

        // Conferida sem o rateio oficial: confere de novo na próxima vez e ainda não apura.
        e = Operacoes.guardarConferencia(e, "a", Apuracao("1/5", 0, conferidaEm = ms(sorteio.atTime(22, 10))))
        assertEquals(1, Pendencias.aguardandoResultado(e, sorteio.atTime(23, 10)).size)
        assertTrue(Pendencias.prontasParaApurar(e, sorteio.plusDays(1).atTime(9, 0)).isEmpty())

        // Com o rateio publicado: apura sozinha a partir das 8h do dia seguinte.
        e = Operacoes.guardarConferencia(e, "a", Apuracao("1/5", 0, rateioOficial = rateio, conferidaEm = ms(sorteio.atTime(23, 10))))
        assertEquals(0, Pendencias.aguardandoResultado(e, sorteio.atTime(23, 30)).size)
        assertTrue(Pendencias.prontasParaApurar(e, sorteio.plusDays(1).atTime(7, 59)).isEmpty())
        assertEquals(listOf("a"), Pendencias.prontasParaApurar(e, sorteio.plusDays(1).atTime(8, 0)).map { it.id })
        // Enquanto a apuração automática não acontece, não há pendência para o Administrador.
        assertTrue(chaves(e, sorteio.plusDays(1).atTime(8, 0)).none { it.startsWith("apuracao-") })

        val domingo = LocalDate.of(2026, 10, 25)
        val b = a.copy(id = "b", dataSorteio = domingo)
        val e2 = registrar(base, b, LocalDate.of(2026, 10, 19))
        assertEquals(1, Pendencias.aguardandoResultado(e2, domingo.atTime(12, 30)).size)
    }

    @Test
    fun apuracaoQuePrecisaDoAdministrador() {
        val sorteio = LocalDate.of(2026, 10, 21)
        val a = Aposta("a", 0, Modalidade.TIMEMANIA, 1, LocalDate.of(2026, 10, 19), dataSorteio = sorteio, valor = 350, criadaEm = 0, comprovante = anexo)
        var e = registrar(base, a, LocalDate.of(2026, 10, 19))
        // Sem resultado da Caixa por dois dias: pendência para apurar à mão.
        assertTrue(chaves(e, sorteio.plusDays(1).atTime(12, 0)).none { it.startsWith("apuracao-") })
        assertTrue("apuracao-a" in chaves(e, sorteio.plusDays(2).atTime(0, 1)))

        // Conferida, mas com algo a confirmar: pendência na hora e nada de apuração automática.
        e = Operacoes.guardarConferencia(
            e, "a", Apuracao("3/7", 0, faixasPremiadas = listOf(5), rateioOficial = rateio, precisaConfirmar = true, conferidaEm = ms(sorteio.atTime(22, 30))),
        )
        val p = Pendencias.de(e, sorteio.atTime(22, 40), fuso).single { it.chave == "apuracao-a" }
        assertEquals("aposta/a", p.destino)
        assertTrue(Pendencias.prontasParaApurar(e, sorteio.plusDays(1).atTime(9, 0)).isEmpty())
        // E continua sendo conferida (a Caixa pode completar o valor).
        assertEquals(1, Pendencias.aguardandoResultado(e, sorteio.atTime(23, 0)).size)
    }

    @Test
    fun avisoDeApostaPremiada() {
        val sorteio = LocalDate.of(2026, 10, 21)
        val a = Aposta("a", 0, Modalidade.QUINA, 7146, LocalDate.of(2026, 10, 19), dataSorteio = sorteio, valor = 300, criadaEm = 0, comprovante = anexo)
        var e = registrar(base, a, LocalDate.of(2026, 10, 19))
        val agora = sorteio.atTime(22, 30)
        e = Operacoes.guardarConferencia(e, "a", Apuracao("2/5", 425, faixasPremiadas = listOf(4), rateioOficial = rateio, conferidaEm = ms(agora)))
        val p = Pendencias.premiadas(e, agora, emptySet(), fuso).single()
        assertEquals("Aposta premiada!", p.titulo)
        // Sem o valor do prêmio: é surpresa ao abrir o app.
        assertEquals("Nº1 | Quina · 7146 · Toque para ver o prêmio.", p.texto)
        assertTrue(p.texto, !p.texto.contains("R$"))
        assertEquals("aposta/a", p.destino)
        // Avisa uma vez só, e só conferências recentes.
        assertTrue(Pendencias.premiadas(e, agora, setOf(p.chave), fuso).isEmpty())
        assertTrue(Pendencias.premiadas(e, agora.plusDays(4), emptySet(), fuso).isEmpty())
    }

    @Test
    fun avisosSoDurante() {
        val p = Pendencias.de(base, LocalDateTime.of(2026, 11, 1, 23, 0), fuso)
        assertTrue(p.isNotEmpty())
        // Às 23h nada vai para o celular; às 8h do dia seguinte, vai.
        assertTrue(Pendencias.paraAvisar(p, LocalDateTime.of(2026, 11, 1, 23, 0), emptySet()).isEmpty())
        assertTrue(Pendencias.paraAvisar(p, LocalDateTime.of(2026, 11, 2, 8, 0), emptySet()).isNotEmpty())
    }

    @Test
    fun rateioEsperandoOsPix() {
        var e = Operacoes.registrarAporteMensal(base.copy(lancamentos = listOf(Lancamento("i", LocalDate.of(2026, 10, 1), 0, TipoLancamento.SALDO_INICIAL, 9000))), LocalDate.of(2026, 10, 1), 0)
        val quando = LocalDateTime.of(2026, 10, 14, 12, 0)
        e = Operacoes.registrarRateio(e, quando.toLocalDate(), 3000, ms(quando))
        val p = Pendencias.de(e, quando.plusMinutes(5), fuso).single { it.tipo == Pendencia.Tipo.ANEXO }
        assertEquals("Anexar os Pix do rateio Nº1", p.titulo)
        assertEquals("Faltam 3 comprovantes de Pix (R$ 10,00 para cada um).", p.texto)
        pessoas.forEach { e = Operacoes.anexarPix(e, 1, it.id, anexo) }
        assertTrue(Pendencias.de(e, quando.plusMinutes(5), fuso).any { it.tipo == Pendencia.Tipo.CONFERENCIA })
    }

    @Test
    fun modoIndividualSoTemAsApuracoes() {
        val individual = base.copy(modo = Modo.INDIVIDUAL, pessoas = pessoas.take(1))
        // Sem aporte, sem dia de aposta, sem extrato.
        assertTrue(Pendencias.de(individual, LocalDateTime.of(2026, 10, 31, 12, 0), fuso).isEmpty())
        assertTrue(Pendencias.de(individual, LocalDateTime.of(2026, 11, 1, 12, 0), fuso).isEmpty())
    }

    @Test
    fun nadaDuranteAExtincao() {
        val e = base.copy(extincao = Extincao(iniciadaEm = 0))
        assertTrue(chaves(e, LocalDateTime.of(2026, 11, 1, 10, 0)).isEmpty())
    }
}

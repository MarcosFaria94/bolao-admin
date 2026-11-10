package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Modalidade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** As novidades da 0.22: os Parâmetros (calendário, teimosinha), o bolão informal e o comprovante que desfaz. */
class ParametrosTest {

    private val hoje = LocalDate.of(2026, 11, 10)
    private var relogio = 1_000L
    private fun agora() = relogio.also { relogio += 10 }

    private val pessoas = listOf(
        Pessoa("p1", "Fulano de Tal", "Fulano", "chave1", administrador = true),
        Pessoa("p2", "Beltrano de Tal", "Beltrano", "chave2"),
        Pessoa("p3", "Ciclano de Tal", "Ciclano", "chave3"),
    )

    private fun inicial(parametros: Parametros = Parametros()) = Estado(
        cadastroConcluido = true, cadastradoEm = hoje.withDayOfMonth(1), pessoas = pessoas,
        config = Config(parametros = parametros),
        lancamentos = listOf(Lancamento("ini", hoje.withDayOfMonth(1), 0, TipoLancamento.SALDO_INICIAL, 20_000)),
    )

    private fun rascunho(id: String, m: Modalidade = Modalidade.QUINA, concurso: Int = 7000, numeros: List<Int> = listOf(1, 2, 3, 4, 5)) = Aposta(
        id = id, numero = 0, modalidade = m, concurso = concurso, dataAposta = hoje, dataSorteio = hoje.plusDays(1),
        premioEstimado = 1_000_000, valor = m.precoPadrao, numeros = numeros, criadaEm = agora(),
    )

    private fun arquivo(nome: String) = Arquivo(nome, nome, "application/pdf", agora(), sha256 = nome)

    private fun caixa(m: String, concurso: Int, numeros: String) = """
        LOTERIAS CAIXA
        COMPROVANTE DE APOSTA
        $m
        CONCURSO: $concurso
        Números: $numeros
        Valor da aposta R$ 3,00
    """.trimIndent()

    @Test
    fun calendarioDosParametros() {
        // Sem nada escolhido: os dias do termo.
        assertEquals(Calendario.DIAS_PROGRAMADOS, Parametros().dias)
        assertFalse(Parametros().semDiaDefinido)
        // Os dias escolhidos, em ordem e sem repetir; fora de 1 a 31 não conta.
        assertEquals(listOf(5, 20, 31), Parametros(diasDeAposta = listOf(31, 5, 20, 5, 40)).dias)
        assertTrue(Parametros(diasDeAposta = emptyList()).semDiaDefinido)
        val dias = listOf(5, 20, 31)
        assertEquals(LocalDate.of(2026, 11, 20), Calendario.proximoDia(hoje, dias))
        assertEquals(LocalDate.of(2026, 11, 20), Calendario.diaDepoisDe(LocalDate.of(2026, 11, 5), dias))
        // Novembro não tem dia 31: depois do dia 20, o próximo é 5 de dezembro.
        assertEquals(LocalDate.of(2026, 12, 5), Calendario.diaDepoisDe(LocalDate.of(2026, 11, 20), dias))
        assertEquals(2, Calendario.diasProgramados(YearMonth.of(2026, 11), dias).size)
        assertEquals(3, Calendario.diasProgramados(YearMonth.of(2026, 12), dias).size)
        // Só o dia 31: pula os meses sem ele.
        assertEquals(LocalDate.of(2027, 3, 31), Calendario.proximoDia(LocalDate.of(2027, 2, 1), listOf(31)))
        // Sem dia definido.
        assertNull(Calendario.proximoDia(hoje, emptyList()))
        assertNull(Calendario.diaDepoisDe(hoje, emptyList()))
        assertFalse(Calendario.ehDiaProgramado(hoje, emptyList()))
        assertEquals(0, Calendario.apostasRestantesNoMes(hoje, false, emptyList()))
        assertEquals("1, 4 e 7", Calendario.texto(listOf(1, 4, 7)))
    }

    @Test
    fun provisionadoAcompanhaOCalendario() {
        // Aporte de R$ 30,00 (3 x R$ 10,00) no dia 1º.
        val comAporte = Operacoes.registrarAporteMensal(inicial(), hoje.withDayOfMonth(1), agora())
        // Dias do termo: dia 10 é dia de aposta, faltam 7 (10, 13... 28) de R$ 3,00.
        assertEquals(2_100L, Operacoes.provisao(comAporte, hoje).provisionado)
        // Só dois dias no mês (15 e 25): R$ 15,00 por aposta, faltam as duas.
        val doisDias = comAporte.copy(config = comAporte.config.copy(parametros = Parametros(diasDeAposta = listOf(15, 25))))
        val p = Operacoes.provisao(doisDias, hoje)
        assertEquals(2, p.apostasRestantes)
        assertEquals(1_500L, p.valorPorAposta)
        assertEquals(3_000L, p.provisionado)
        // Sem dia definido: fica reservado o que sobra do aporte do mês depois das apostas do mês.
        var semDia = comAporte.copy(config = comAporte.config.copy(parametros = Parametros(diasDeAposta = emptyList())))
        assertEquals(3_000L, Operacoes.provisao(semDia, hoje).provisionado)
        semDia = Operacoes.registrarAposta(Operacoes.salvarRascunho(semDia, rascunho("a")), "a", hoje, agora())
        assertEquals(2_700L, Operacoes.provisao(semDia, hoje).provisionado)
        assertEquals(Operacoes.saldo(semDia) - 2_700L, Operacoes.extra(semDia, hoje))
    }

    @Test
    fun teimosinhaViraUmaApostaPorConcurso() {
        val e = Operacoes.salvarRascunho(inicial(), rascunho("t").copy(teimosinha = 4))
        val saldo = Operacoes.saldo(e)
        val (depois, feitas) = Operacoes.registrarTudo(e, "t", hoje, agora())
        // Quatro apostas, Nº1 a Nº4, nos concursos 7000 a 7003, com os mesmos números e o mesmo valor.
        assertEquals(listOf(1, 2, 3, 4), feitas.map { it.numero })
        assertEquals(listOf(7000, 7001, 7002, 7003), feitas.map { it.concurso })
        assertEquals(listOf(1, 2, 3, 4), feitas.map { it.teimosinhaOrdem })
        assertTrue(feitas.all { it.ehTeimosinha && it.teimosinha == 4 && it.teimosinhaDe == "t" && it.numeros == listOf(1, 2, 3, 4, 5) })
        assertTrue(feitas.all { it.registrada && it.pendenteDeAnexo })
        assertEquals(saldo - 4 * 300, Operacoes.saldo(depois))
        // Só a do primeiro concurso tem o dia do sorteio e o prêmio estimado.
        assertNotNull(feitas[0].dataSorteio)
        assertTrue(feitas.drop(1).all { it.dataSorteio == null && it.premioEstimado == null })
        assertEquals(4, Operacoes.daMesmaTeimosinha(depois, feitas[2]).size)
        // A teimosinha registrada fica nos concursos dela (não "acompanha a Caixa" como as outras).
        assertFalse(Operacoes.concursoDesatualizado(feitas[1], 7005))
        // O dia do sorteio chega quando a Caixa divulga o concurso (mesmo com a aposta já conferida)...
        val proximo = Operacoes.ProximoConcurso(7001, hoje.plusDays(2), 2_000_000)
        val comData = Operacoes.completarDatas(depois, mapOf(Modalidade.QUINA to proximo))
        assertEquals(hoje.plusDays(2), comData.apostas.first { it.concurso == 7001 }.dataSorteio)
        assertEquals(2_000_000L, comData.apostas.first { it.concurso == 7001 }.premioEstimado)
        // ...ou, se o concurso já foi sorteado sem o app ver, pelo dia do concurso.
        val atrasadas = Operacoes.semDataJaSorteadas(comData, mapOf(Modalidade.QUINA to 7002))
        assertEquals(listOf(7002), atrasadas.map { it.concurso })
        val gravado = Operacoes.comDataDoSorteio(comData, mapOf(atrasadas.single().id to hoje.plusDays(3)))
        assertEquals(hoje.plusDays(3), gravado.apostas.first { it.concurso == 7002 }.dataSorteio)
        // Sem teimosinha (0 ou 1), na Loteca ou na Federal: uma aposta só.
        val (uma, lista) = Operacoes.registrarTudo(Operacoes.salvarRascunho(inicial(), rascunho("u").copy(teimosinha = 1)), "u", hoje, agora())
        assertEquals(1, lista.size)
        assertFalse(uma.apostas.single().ehTeimosinha)
        assertEquals(1, Operacoes.concursosDaTeimosinha(rascunho("l", Modalidade.LOTECA).copy(teimosinha = 5)))
        assertEquals(Operacoes.MAXIMO_DA_TEIMOSINHA, Operacoes.concursosDaTeimosinha(rascunho("x").copy(teimosinha = 90)))
    }

    @Test
    fun teimosinhaComEspelhoEUmComprovanteSo() {
        // Lotomania com a espelho, em 2 concursos: 4 apostas.
        val pares = (0 until 100 step 2).toList()
        val lm = rascunho("lm", Modalidade.LOTOMANIA, 2790, pares).copy(espelho = true, teimosinha = 2)
        assertEquals(4, Operacoes.apostasDoRegistro(lm))
        val (e, feitas) = Operacoes.registrarTudo(Operacoes.salvarRascunho(inicial(), lm), "lm", hoje, agora())
        assertEquals(listOf(2790, 2790, 2791, 2791), feitas.map { it.concurso })
        assertEquals(listOf(1, 2, 3, 4), feitas.map { it.numero })
        // Uma teimosinha de Quina: o comprovante da Caixa é um só e vai para todos os concursos dela.
        val (q, quinas) = Operacoes.registrarTudo(Operacoes.salvarRascunho(e, rascunho("q").copy(teimosinha = 3)), "q", hoje, agora())
        val r = Associacao.associar(q, listOf(Associacao.Entrada(arquivo("quina.pdf"), caixa("QUINA", 7000, "01 02 03 04 05"))))
        assertTrue(quinas.all { a -> r.estado.apostas.first { it.id == a.id }.comprovante?.nome == "quina.pdf" })
        assertEquals(listOf(7000, 7001, 7002), quinas.map { a -> r.estado.apostas.first { it.id == a.id }.concurso })
        assertTrue(r.avisos.any { it.texto.startsWith("Teimosinha: o mesmo comprovante") })
        assertTrue(r.descartados.isEmpty())
        // As da Lotomania continuam esperando.
        assertEquals(4, Operacoes.apostasSemComprovante(r.estado).size)
        // Tirar o comprovante de uma tira de todas as da teimosinha.
        val semAnexo = Operacoes.desanexarDaAposta(r.estado, quinas[1].id)
        assertTrue(quinas.all { a -> semAnexo.apostas.first { it.id == a.id }.comprovante == null })
    }

    @Test
    fun bolaoInformalNaoEnviaNada() {
        val informal = inicial().copy(informal = true)
        assertFalse(informal.enviaAoGrupo)
        assertTrue(inicial().enviaAoGrupo)
        // O aporte adicional (que no formal vai ao grupo) fica só no app.
        val e = Operacoes.registrarAporteAdicional(informal, hoje, 3_000, agora())
        assertTrue(e.comprovantes.single().envioDispensado)
        assertTrue(Operacoes.comprovantesPendentesDeEnvio(e).isEmpty())
        // A aposta continua esperando o comprovante da Caixa, como no formal.
        var comAposta = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, rascunho("a")), "a", hoje, agora())
        assertTrue(Operacoes.aguardandoComprovante(comAposta.apostas.single()))
        comAposta = Operacoes.aprovarAposta(Operacoes.anexarNaAposta(comAposta, "a", arquivo("caixa.pdf")), "a", agora())
        assertTrue(Operacoes.comprovantesPendentesDeEnvio(comAposta).isEmpty())
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(comAposta))
        // No formal, os mesmos registros ficam para enviar.
        val formal = Operacoes.registrarAporteAdicional(inicial(), hoje, 3_000, agora())
        assertEquals(1, Operacoes.comprovantesPendentesDeEnvio(formal).size)
    }

    /** Até a 0.21, tirar o comprovante deixava a aposta com o jogo trocado: anexar de novo "dava certo". */
    @Test
    fun tirarOComprovanteDesfazOQueEleMudou() {
        var e = Operacoes.registrarAposta(Operacoes.salvarRascunho(inicial(), rascunho("a")), "a", hoje, agora())
        val antes = e.apostas.single()
        // O comprovante lido com outros números e outro concurso: a aposta fica como no comprovante...
        val r = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("c.pdf"), caixa("QUINA", 7001, "01 02 03 04 55"))))
        val alterada = r.estado.apostas.single()
        assertEquals(listOf(1, 2, 3, 4, 55), alterada.numeros)
        assertEquals(7001, alterada.concurso)
        assertEquals(Operacoes.jogoDaAposta(antes), alterada.antesDoAnexo)
        // ...e tirar o comprovante devolve a aposta ao que era.
        e = Operacoes.desanexarDaAposta(r.estado, "a")
        assertEquals(antes, e.apostas.single())
        // Com o comprovante igual ao jogo, não há o que desfazer.
        val igual = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("d.pdf"), caixa("QUINA", 7000, "01 02 03 04 05"))))
        assertNull(igual.estado.apostas.single().antesDoAnexo)
        // A aprovação apaga o retrato de antes.
        val aprovada = Operacoes.aprovarAposta(r.estado, "a", agora()).apostas.single()
        assertNull(aprovada.antesDoAnexo)
        assertEquals(listOf(1, 2, 3, 4, 55), aprovada.numeros)
    }

    /**
     * Até a 0.21 o comprovante que não batia em cheio ia para a primeira aposta da fila: um número mal
     * lido trocava o jogo de outra aposta e as seguintes iam trocando em cadeia.
     */
    @Test
    fun comprovanteMalLidoVaiParaAApostaMaisParecida() {
        var e = inicial()
        val jogos = mapOf("a" to listOf(1, 2, 3, 4, 5), "b" to listOf(11, 12, 13, 14, 15), "c" to listOf(21, 22, 23, 24, 25))
        jogos.forEach { (id, n) -> e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, rascunho(id, numeros = n)), id, hoje, agora()) }
        // Os comprovantes fora de ordem, e o da aposta "c" com um número mal lido (26 no lugar do 25).
        val r = Associacao.associar(
            e,
            listOf(
                Associacao.Entrada(arquivo("c.pdf"), caixa("QUINA", 7000, "21 22 23 24 26")),
                Associacao.Entrada(arquivo("b.pdf"), caixa("QUINA", 7000, "11 12 13 14 15")),
                Associacao.Entrada(arquivo("a.pdf"), caixa("QUINA", 7000, "01 02 03 04 05")),
            ),
        )
        fun aposta(id: String) = r.estado.apostas.first { it.id == id }
        // Cada arquivo na sua aposta; "a" e "b" intactas; só a "c" ficou como no comprovante.
        assertEquals(listOf("a.pdf", "b.pdf", "c.pdf"), listOf("a", "b", "c").map { aposta(it).comprovante?.nome })
        assertEquals(jogos.getValue("a"), aposta("a").numeros)
        assertEquals(jogos.getValue("b"), aposta("b").numeros)
        assertEquals(listOf(21, 22, 23, 24, 26), aposta("c").numeros)
        assertEquals(1, r.avisos.count { it.tipo == Associacao.Tipo.ALTERADA })
        assertTrue(r.descartados.isEmpty())
    }

    @Test
    fun segundaLeituraDoMesmoArquivoResolveONumeroMalLido() {
        val e = Operacoes.registrarAposta(Operacoes.salvarRascunho(inicial(), rascunho("a")), "a", hoje, agora())
        val errada = caixa("QUINA", 7000, "01 02 03 04 06")
        val certa = caixa("QUINA", 7000, "01 02 03 04 05")
        assertFalse(Associacao.confereEmCheio(e, errada))
        assertTrue(Associacao.confereEmCheio(e, certa))
        // A leitura principal errou um número; a alternativa (o texto do PDF, a página maior) bate com a aposta: vale ela.
        val r = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("a.pdf"), errada, listOf(certa))))
        assertEquals(listOf(1, 2, 3, 4, 5), r.estado.apostas.single().numeros)
        assertEquals(Associacao.Tipo.ANEXADO, r.avisos.first().tipo)
        assertNull(r.estado.apostas.single().antesDoAnexo)
        // Sem alternativa que bata, continua valendo o comprovante (com a nota da diferença).
        val semAlternativa = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("a.pdf"), errada, listOf(errada))))
        assertEquals(Associacao.Tipo.ALTERADA, semAlternativa.avisos.first().tipo)
    }

    @Test
    fun comprovanteDeApostaComMaisNumeros() {
        // Mega-Sena com 8 números (28 apostas simples), em duas linhas no comprovante.
        val oito = listOf(3, 9, 17, 24, 38, 41, 52, 60)
        val a = rascunho("ms", Modalidade.MEGA_SENA, 3080, oito).copy(valor = 600L * 28)
        val e = Operacoes.registrarAposta(Operacoes.salvarRascunho(inicial(), a), "ms", hoje, agora())
        val texto = "LOTERIAS CAIXA\nMEGA-SENA\nCONCURSO: 3080\nNúmeros: 03 09 17 24 38 41\n52 60\nValor R$ 168,00"
        val r = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("mega.pdf"), texto)))
        assertEquals(Associacao.Tipo.ANEXADO, r.avisos.first().tipo)
        assertEquals(oito, r.estado.apostas.single().numeros)
    }
}

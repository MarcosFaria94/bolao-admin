package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class DadosDeExemploTest {

    private val fuso = ZoneOffset.ofHours(-3)
    private fun instante(d: LocalDate, hora: Int = 1, minuto: Int = 4) = d.atTime(hora, minuto).atZone(fuso).toInstant().toEpochMilli()
    private fun gerar(hoje: LocalDate, reais: Map<Modalidade, Concurso> = emptyMap(), hora: Int = 1) =
        DadosDeExemplo.gerar(hoje, instante(hoje, hora), fuso, reais)

    @Test
    fun impressoesDigitaisDosAnexosRefazemACorrente() {
        val e = gerar(LocalDate.of(2026, 10, 17))
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(e))
        val com = DadosDeExemplo.comImpressoesDigitais(e) { "H-" + it.nome }
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(com))
        assertTrue(com.comprovantes.flatMap { it.anexos }.all { it.sha256 == "H-" + it.nome })
        assertTrue(com.apostas.mapNotNull { it.comprovante }.all { it.sha256 != null })
        assertEquals(e.comprovantes.map { it.texto }, com.comprovantes.map { it.texto })
        assertFalse(e.comprovantes.map { it.autenticacao } == com.comprovantes.map { it.autenticacao })
    }

    /** As apostas dos dias programados (as que esperam o comprovante ficam de fora). */
    private fun programadas(e: Estado) = e.apostas.filter { !it.pendenteDeAnexo }

    @Test
    fun exemploDe17DeOutubro() {
        val hoje = LocalDate.of(2026, 10, 17)
        val e = gerar(hoje)
        assertTrue(e.exemplo)
        assertTrue(e.cadastroConcluido)
        // De 2 a 5 integrantes fictícios, o Administrador primeiro.
        assertTrue(e.pessoas.size in 2..5)
        assertEquals(DadosDeExemplo.NOMES.take(e.pessoas.size), e.pessoas)

        // 10 apostas em setembro e 6 em outubro (dias 1 a 16); a de hoje fica para quem testa.
        val p = programadas(e)
        assertEquals(16, p.size)
        assertEquals((1..10).toList() + (1..6).toList(), p.map { it.numero })
        val pendentes = p.filter { it.apuracao == null }
        assertEquals(1, pendentes.size)
        assertEquals(LocalDate.of(2026, 10, 16), pendentes.single().dataAposta)
        assertTrue(pendentes.single().dataSorteio!!.isBefore(hoje))

        // Uma ou duas premiadas em setembro.
        val setembro = YearMonth.of(2026, 9)
        val premiadasSetembro = p.filter { YearMonth.from(it.dataAposta) == setembro && (it.apuracao?.premio ?: 0) > 0 }
        assertTrue(premiadasSetembro.size in 1..2)
        assertEquals(listOf("2026-09"), e.extratos.map { it.mes })
        assertTrue(Operacoes.rendimentoDoMes(e, setembro) in 1..DadosDeExemplo.RENDIMENTO_MAXIMO)
        conferirCoerencia(hoje, e, instante(hoje))
    }

    @Test
    fun cadaExemploSaiDiferente() {
        val hoje = LocalDate.of(2026, 10, 17)
        val exemplos = (1L..12L).map { DadosDeExemplo.gerar(hoje, instante(hoje, 12), fuso, semente = it) }
        // Modalidades, integrantes e valores variam de um exemplo para outro.
        assertTrue(exemplos.map { e -> e.apostas.map { it.modalidade } }.distinct().size > 6)
        assertTrue(exemplos.map { it.pessoas.size }.distinct().size > 1)
        assertTrue(exemplos.map { Operacoes.saldo(it) }.distinct().size > 6)
        // Às vezes há o que anexar na aba Comprovantes (aposta sem comprovante ou rateio sem Pix).
        assertTrue(exemplos.any { Operacoes.apostasSemComprovante(it).isNotEmpty() })
        assertTrue(exemplos.any { Operacoes.rateiosSemPix(it).isNotEmpty() || Operacoes.rateiosParaConferir(it).isNotEmpty() })
        exemplos.forEach { conferirCoerencia(hoje, it, instante(hoje, 12)) }
    }

    @Test
    fun exemploDoModoIndividual() {
        var d = LocalDate.of(2026, 1, 1)
        while (d.isBefore(LocalDate.of(2027, 1, 1))) {
            val e = DadosDeExemplo.gerar(d, instante(d, 12), fuso, semente = d.toEpochDay(), modo = Modo.INDIVIDUAL)
            val msg = "dia $d"
            assertTrue(msg, e.individual && e.exemplo)
            assertEquals(msg, 1, e.pessoas.size)
            assertTrue(msg, e.comprovantes.isEmpty())
            assertTrue(msg, e.rateios.isEmpty() && e.extratos.isEmpty())
            assertTrue(msg, e.lancamentos.all { it.tipo == TipoLancamento.APOSTA || it.tipo == TipoLancamento.PREMIO })
            assertTrue(msg, e.apostas.all { it.registrada && !it.pendenteDeAnexo && it.comprovante == null && it.dataAposta.isBefore(d) })
            // O saldo é o gasto menos os prêmios (começa em zero).
            assertEquals(msg, e.lancamentos.sumOf { it.valor }, Operacoes.saldo(e))
            val esperando = e.apostas.single { it.apuracao == null }
            assertEquals(msg, e.apostas.maxOf { it.dataAposta }, esperando.dataAposta)
            assertTrue(msg, e.apostas.any { (it.apuracao?.premio ?: 0) > 0 })
            e.apostas.groupBy { YearMonth.from(it.dataAposta) }.values.forEach { doMes ->
                assertEquals(msg, (1..doMes.size).toList(), doMes.sortedBy { it.dataAposta }.map { it.numero })
            }
            d = d.plusDays(7)
        }
    }

    @Test
    fun todosOsDiasDoAnoGeramDadosCoerentes() {
        var d = LocalDate.of(2026, 1, 1)
        while (d.isBefore(LocalDate.of(2028, 1, 1))) {
            val reais = mapOf(Modalidade.LOTOFACIL to concursoReal(Modalidade.LOTOFACIL, 3600, d.minusDays(1)))
            conferirCoerencia(d, gerar(d), instante(d))
            conferirCoerencia(d, gerar(d, reais, hora = 23), instante(d, 23))
            d = d.plusDays(1)
        }
    }

    private fun conferirCoerencia(hoje: LocalDate, e: Estado, agora: Long) {
        val msg = "dia $hoje"
        // Nada no futuro.
        assertTrue(msg, e.lancamentos.all { !it.data.isAfter(hoje) && it.criadoEm < agora })
        assertTrue(msg, e.apostas.all { it.dataAposta.isBefore(hoje) && it.criadaEm < agora })
        assertTrue(msg, e.apostas.all { a -> a.apuracao?.confirmadaEm?.let { it < agora } ?: true })
        // O saldo nunca fica negativo, em nenhum momento.
        var saldo = 0L
        e.lancamentos.sortedBy { it.criadoEm }.forEach {
            saldo += it.valor
            assertTrue("$msg: saldo negativo em ${it.data}", saldo >= 0)
        }
        // Todas registradas; as conferidas com o comprovante; numeração do mês em sequência.
        assertTrue(msg, e.apostas.all { it.registrada })
        assertTrue(msg, e.apostas.filter { !it.pendenteDeAnexo }.all { it.comprovante != null })
        e.apostas.groupBy { YearMonth.from(it.dataAposta) }.values.forEach { doMes ->
            assertEquals(msg, (1..doMes.size).toList(), doMes.map { it.numero })
        }
        assertNull(msg, Operacoes.primeiraAutenticacaoQuebrada(e))
        assertTrue(msg, Operacoes.comprovantesPendentesDeEnvio(e).isEmpty())
        assertTrue(msg, Integridade.registrosSemComprovante(e).isEmpty())
        // Só a última aposta programada espera a apuração, já sorteada; a que espera o comprovante, o sorteio.
        val p = programadas(e)
        val pendente = p.single { it.apuracao == null }
        assertEquals(msg, p.maxOf { it.dataAposta }, pendente.dataAposta)
        assertTrue(msg, pendente.dataSorteio!!.isBefore(hoje))
        assertFalse(msg, pendente.dataSorteio!!.isBefore(pendente.dataAposta))
        e.apostas.filter { it.pendenteDeAnexo }.forEach { a ->
            assertNull(msg, a.apuracao)
            assertNull(msg, a.comprovante)
            assertFalse(msg, a.dataSorteio!!.isBefore(hoje))
            assertEquals(msg, hoje.minusDays(1), a.dataAposta)
        }
        assertTrue(msg, e.apostas.count { it.pendenteDeAnexo } <= 1)
        // As outras foram conferidas com o próprio volante.
        e.apostas.filter { it.apuracao != null }.forEach { a ->
            assertNotNull(msg, a.apuracao!!.confirmadaEm)
            assertTrue(msg, a.apuracao!!.dezenasSorteadas.isNotEmpty())
            assertTrue("$msg: palpite incompleto", Volante.pendencias(a.modalidade, a.palpite).isEmpty())
        }
        assertTrue(msg, e.apostas.all { Volante.pendencias(it.modalidade, it.palpite).isEmpty() })
        // Rateio: no máximo um conferido no mês anterior e um esperando os Pix ontem; extrato do mês anterior.
        val anterior = YearMonth.from(hoje).minusMonths(1)
        assertTrue(msg, e.rateios.count { !it.pendenteDeAnexo } <= 1)
        assertTrue(msg, e.rateios.filter { !it.pendenteDeAnexo }.all { YearMonth.from(it.data) == anterior && it.cota > 0 })
        assertTrue(msg, e.rateios.filter { it.pendenteDeAnexo }.all { it.data == hoje.minusDays(1) && it.pix.size < e.pessoas.size })
        assertEquals(msg, listOf(anterior.toString()), e.extratos.map { it.mes })
        // Anexos com nomes únicos: comprovante de cada aposta conferida, imagem de cada apuração e os Pix.
        val anexos = DadosDeExemplo.anexos(e).map { it.nome }
        assertEquals(msg, anexos.size, anexos.distinct().size)
        val conferidas = e.apostas.count { !it.pendenteDeAnexo }
        val pix = e.rateios.sumOf { if (it.pendenteDeAnexo) it.pix.size else it.comprovantes.size }
        assertEquals(msg, conferidas + (conferidas - 1) + pix, anexos.size)
        val imagens = e.comprovantes.map { it.imagem.nome }
        assertEquals(msg, imagens.size, imagens.distinct().size)
        // As mensagens saem sem erro e as apostas registradas nunca passaram do saldo.
        e.apostas.forEach { a ->
            val antes = Operacoes.saldoAntesDe(e, a.id, TipoLancamento.APOSTA)
            assertTrue("$msg: aposta Nº${a.numero} sem saldo", antes >= a.valor)
            Operacoes.textoAposta(e, a, hoje)
        }
        Operacoes.textoExtrato(e, YearMonth.from(hoje))
        Operacoes.textoExtrato(e, anterior)
    }

    @Test
    fun ultimoJogoUsaConcursoRealQuandoHa() {
        val hoje = LocalDate.of(2026, 10, 17)
        val lf = concursoReal(Modalidade.LOTOFACIL, 3600, LocalDate.of(2026, 10, 16))
        val e = gerar(hoje, mapOf(Modalidade.LOTOFACIL to lf))
        val pendente = programadas(e).single { it.apuracao == null }
        assertEquals(Modalidade.LOTOFACIL, pendente.modalidade)
        assertEquals(3600, pendente.concurso)
        assertEquals(LocalDate.of(2026, 10, 16), pendente.dataSorteio)
        // Os outros jogos da Lotofácil ganham números anteriores, estimados a partir do real.
        val outras = programadas(e).filter { it.modalidade == Modalidade.LOTOFACIL && it.id != pendente.id }
        assertTrue(outras.all { it.concurso < 3600 })
        assertEquals(outras.size, outras.map { it.concurso }.distinct().size)
    }

    @Test
    fun resultadoDeHojeNaoServeParaOExemplo() {
        // Sorteio de hoje ainda aparece como "sorteio hoje": o exemplo usa um sorteio já passado.
        val hoje = LocalDate.of(2026, 10, 18)
        val lf = concursoReal(Modalidade.LOTOFACIL, 3600, hoje)
        val qn = concursoReal(Modalidade.QUINA, 6900, LocalDate.of(2026, 10, 17))
        val pendente = programadas(gerar(hoje, mapOf(Modalidade.LOTOFACIL to lf, Modalidade.QUINA to qn))).single { it.apuracao == null }
        assertEquals(Modalidade.QUINA, pendente.modalidade)
        assertEquals(6900, pendente.concurso)
    }

    @Test
    fun usaOsIntegrantesDoCadastroReal() {
        val reais = listOf(
            Pessoa("a", "Ana Maria", "Ana", "ana@x", administrador = true),
            Pessoa("b", "Bia Souza", "Bia", "bia@x"),
            Pessoa("c", "Cid Lima", "Cid", "cid@x"),
        )
        val e = DadosDeExemplo.gerar(LocalDate.of(2026, 10, 17), instante(LocalDate.of(2026, 10, 17)), fuso, pessoas = reais)
        assertEquals(reais, e.pessoas)
        assertEquals("ana@x", e.administrador!!.chavePix)
    }

    @Test
    fun primeiroDiaDoMesDeixaOAporteParaQuemTesta() {
        val hoje = LocalDate.of(2026, 11, 1)
        val e = gerar(hoje)
        assertEquals(0L, Operacoes.aportesDoMes(e, YearMonth.from(hoje)))
        // O último jogo é o do dia 28 do mês anterior.
        assertEquals(LocalDate.of(2026, 10, 28), programadas(e).single { it.apuracao == null }.dataAposta)
    }

    @Test
    fun estadoDeExemploVaiEVoltaDoJson() {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val e = gerar(LocalDate.of(2026, 10, 17))
        val texto = json.encodeToString(Estado.serializer(), e)
        assertEquals(e, json.decodeFromString(Estado.serializer(), texto))
        // Dados antigos (sem o campo novo) continuam sendo lidos como dados reais.
        assertFalse(json.decodeFromString(Estado.serializer(), "{\"cadastroConcluido\":true}").exemplo)
        assertNull(json.decodeFromString(Estado.serializer(), "{}").termo)
    }

    private fun concursoReal(m: Modalidade, numero: Int, data: LocalDate) = Concurso(
        modalidade = m,
        numero = numero,
        data = data,
        dezenas = Volante.universo(m).toList().take(m.qtdSorteada),
    )
}

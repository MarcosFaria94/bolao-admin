package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.CalculoRateio
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
    fun exemploDe17DeOutubro() {
        val hoje = LocalDate.of(2026, 10, 17)
        val e = gerar(hoje)
        assertTrue(e.exemplo)
        assertTrue(e.cadastroConcluido)
        assertEquals(DadosDeExemplo.PESSOAS, e.pessoas)

        // 10 apostas em setembro e 6 em outubro (dias 1 a 16); a de hoje fica para quem testa.
        assertEquals(16, e.apostas.size)
        assertEquals(DadosDeExemplo.PRIMEIRO_JOGO + 16, e.proximoJogo)
        val pendentes = e.apostas.filter { it.apuracao == null }
        assertEquals(1, pendentes.size)
        assertEquals(LocalDate.of(2026, 10, 16), pendentes.single().dataAposta)
        assertTrue(pendentes.single().dataSorteio!!.isBefore(hoje))

        // Setembro: 10,02 + 30,00 + 14,00 − 30,00 − 24,00 + 0,03 = 0,05.
        val setembro = YearMonth.of(2026, 9)
        val ateSetembro = e.lancamentos.filter { !YearMonth.from(it.data).isAfter(setembro) }
        assertEquals(5L, ateSetembro.sumOf { it.valor })
        // Outubro até o dia 16: 0,05 + 30,00 + 7,00 − 19,00 = 18,05.
        assertEquals(1805L, Operacoes.saldo(e))

        assertEquals(1, e.rateios.size)
        assertEquals(800L, e.rateios.single().cota)
        assertEquals(listOf("2026-09"), e.extratos.map { it.mes })
        assertEquals(DadosDeExemplo.RENDIMENTO_ANTERIOR, Operacoes.rendimentoDoMes(e, setembro))
        assertEquals(EXTRATO_SETEMBRO, Operacoes.textoExtrato(e, setembro))

        // Rateio hoje: faltam 19, 22, 25 e 28 (4 × R$ 3,00); cota = (18,05 − 12,00) ÷ 3 = 2,01.
        val s = Operacoes.sugestaoProvisionado(e, hoje)
        assertEquals(1200L, s.provisionado)
        assertEquals(201L, CalculoRateio.calcular(Operacoes.saldo(e), s.provisionado)!!.cota)

        // A primeira aposta de outubro teve prêmio (11 acertos na Lotofácil).
        val premiadas = e.apostas.filter { (it.apuracao?.premio ?: 0) > 0 }
        assertEquals(listOf("12/15" to 1400L, "11/15" to 700L), premiadas.map { it.apuracao!!.acertos to it.apuracao!!.premio })
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
        // Todas registradas, com comprovante e numeração em sequência.
        assertTrue(msg, e.apostas.all { it.registrada && it.comprovante != null })
        assertEquals(msg, (DadosDeExemplo.PRIMEIRO_JOGO until e.proximoJogo).toList(), e.apostas.map { it.numero }.sorted())
        // Só a última fica esperando a apuração, já sorteada.
        val pendente = e.apostas.single { it.apuracao == null }
        assertEquals(msg, e.apostas.maxOf { it.dataAposta }, pendente.dataAposta)
        assertTrue(msg, pendente.dataSorteio!!.isBefore(hoje))
        assertFalse(msg, pendente.dataSorteio!!.isBefore(pendente.dataAposta))
        // As outras foram conferidas com o próprio volante.
        e.apostas.filter { it.apuracao != null }.forEach { a ->
            assertNotNull(msg, a.apuracao!!.confirmadaEm)
            assertTrue(msg, a.apuracao!!.dezenasSorteadas.isNotEmpty())
            assertTrue("$msg: palpite incompleto", Volante.pendencias(a.modalidade, a.palpite).isEmpty())
        }
        assertTrue(msg, Volante.pendencias(pendente.modalidade, pendente.palpite).isEmpty())
        // Rateio do mês anterior e extrato do mês anterior.
        assertEquals(msg, 800L, e.rateios.single().cota)
        assertEquals(msg, listOf(YearMonth.from(hoje).minusMonths(1).toString()), e.extratos.map { it.mes })
        // Anexos com nomes únicos.
        val anexos = DadosDeExemplo.anexos(e).map { it.nome }
        assertEquals(msg, anexos.size, anexos.distinct().size)
        assertEquals(msg, e.apostas.size + 3, anexos.size)
        // As mensagens saem sem erro e as apostas registradas nunca passaram do saldo.
        e.apostas.forEach { a ->
            val antes = Operacoes.saldoAntesDe(e, a.id, TipoLancamento.APOSTA)
            assertTrue("$msg: aposta #${a.numero} sem saldo", antes >= a.valor)
            Operacoes.textoAposta(e, a)
        }
        Operacoes.textoExtrato(e, YearMonth.from(hoje))
        Operacoes.textoExtrato(e, YearMonth.from(hoje).minusMonths(1))
    }

    @Test
    fun ultimoJogoUsaConcursoRealQuandoHa() {
        val hoje = LocalDate.of(2026, 10, 17)
        val lf = concursoReal(Modalidade.LOTOFACIL, 3600, LocalDate.of(2026, 10, 16))
        val e = gerar(hoje, mapOf(Modalidade.LOTOFACIL to lf))
        val pendente = e.apostas.single { it.apuracao == null }
        assertEquals(Modalidade.LOTOFACIL, pendente.modalidade)
        assertEquals(3600, pendente.concurso)
        assertEquals(LocalDate.of(2026, 10, 16), pendente.dataSorteio)
        // Os outros jogos da Lotofácil ganham números anteriores, estimados a partir do real.
        val outras = e.apostas.filter { it.modalidade == Modalidade.LOTOFACIL && it.id != pendente.id }
        assertTrue(outras.all { it.concurso < 3600 })
        assertEquals(outras.size, outras.map { it.concurso }.distinct().size)
    }

    @Test
    fun resultadoDeHojeNaoServeParaOExemplo() {
        // Sorteio de hoje ainda aparece como "sorteio hoje": o exemplo usa um sorteio já passado.
        val hoje = LocalDate.of(2026, 10, 18)
        val lf = concursoReal(Modalidade.LOTOFACIL, 3600, hoje)
        val qn = concursoReal(Modalidade.QUINA, 6900, LocalDate.of(2026, 10, 17))
        val pendente = gerar(hoje, mapOf(Modalidade.LOTOFACIL to lf, Modalidade.QUINA to qn)).apostas.single { it.apuracao == null }
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
        assertTrue(Operacoes.textoExtrato(e, YearMonth.of(2026, 9)).contains("APORTE (Ana)"))
    }

    @Test
    fun primeiroDiaDoMesDeixaOAporteParaQuemTesta() {
        val hoje = LocalDate.of(2026, 11, 1)
        val e = gerar(hoje)
        assertEquals(0L, Operacoes.aportesDoMes(e, YearMonth.from(hoje)))
        // O último jogo é o do dia 28 do mês anterior.
        assertEquals(LocalDate.of(2026, 10, 28), e.apostas.single { it.apuracao == null }.dataAposta)
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

    private companion object {
        const val EXTRATO_SETEMBRO = "- EXTRATO SET/2026 -\n\nRESUMO\nSaldo Líq. Anterior.:   R\$10,02\nRendimento Líquido..: + R\$ 0,03\nAportes.............: + R\$30,00\nPremiações..........: + R\$14,00\nApostas.............: - R\$30,00\nRateio..............: - R\$24,00\nSaldo Final           = R\$ 0,05\n\nMOVIMENTAÇÕES\n00  01/09/26 SALDO + RENDIMENTO...:   R\$10,05\n01           APORTE (Fulano)......: + R\$10,00\n02           APORTE (Beto)........: + R\$10,00\n03           APORTE (Beltrano)....: + R\$10,00\n04           APOSTA #121 LF ......: - R\$ 3,50\n05  02/09/26 PRÊMIO #121 LF ......: + R\$14,00\n06  04/09/26 APOSTA #122 QN ......: - R\$ 3,00\n07  07/09/26 APOSTA #123 DS ......: - R\$ 2,50\n08  10/09/26 APOSTA #124 LM ......: - R\$ 3,00\n09  13/09/26 APOSTA #125 SS ......: - R\$ 3,00\n10  14/09/26 RATEIO Nº1 (Fulano)..: - R\$ 8,00\n11           RATEIO Nº1 (Beto)....: - R\$ 8,00\n12           RATEIO Nº1 (Beltrano): - R\$ 8,00\n13  16/09/26 APOSTA #126 DP ......: - R\$ 3,00\n14  19/09/26 APOSTA #127 QN ......: - R\$ 3,00\n15  22/09/26 APOSTA #128 DS ......: - R\$ 2,50\n16  25/09/26 APOSTA #129 LM ......: - R\$ 3,00\n17  28/09/26 APOSTA #130 LF ......: - R\$ 3,50"
    }
}

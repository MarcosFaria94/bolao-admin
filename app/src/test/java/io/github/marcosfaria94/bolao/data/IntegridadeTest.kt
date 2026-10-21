package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Autenticacao
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class IntegridadeTest {

    private val fuso = ZoneOffset.ofHours(-3)
    private val hoje = LocalDate.of(2026, 10, 17)
    private val exemplo: Estado by lazy {
        val e = DadosDeExemplo.gerar(hoje, hoje.atTime(9, 0).atZone(fuso).toInstant().toEpochMilli(), fuso, emptyMap())
        DadosDeExemplo.comImpressoesDigitais(e) { "H-" + it.nome }
    }

    @Test
    fun tudoConfere() {
        val r = Integridade.verificar(exemplo) { it.sha256 }
        assertTrue(r.toString(), r.integro)
        assertEquals(exemplo.comprovantes.size, r.comprovantes)
        assertEquals(exemplo.comprovantes.sumOf { it.anexos.size }, r.anexosQueConferem)
        assertNull(r.quebraNaCorrente)
        assertEquals(0, r.legados)
    }

    @Test
    fun anexoAlteradoOuSumido() {
        val anexos = exemplo.comprovantes.flatMap { it.anexos }
        val r = Integridade.verificar(exemplo) { a ->
            when (a.nome) {
                anexos[0].nome -> null
                anexos[1].nome -> "0".repeat(64)
                else -> a.sha256
            }
        }
        assertFalse(r.integro)
        assertEquals(listOf(anexos[0].nome), r.anexosFaltando.map { it.arquivo.nome })
        assertEquals(listOf(anexos[1].nome), r.anexosAlterados.map { it.arquivo.nome })
        assertEquals(anexos.size - 2, r.anexosQueConferem)
    }

    @Test
    fun comprovanteApagadoAparece() {
        // Apagar o último comprovante não quebra a corrente, mas o registro fica sem comprovante.
        val ultimo = exemplo.comprovantes.last()
        val e = exemplo.copy(comprovantes = exemplo.comprovantes.dropLast(1))
        val r = Integridade.verificar(e) { it.sha256 }
        assertNull(r.quebraNaCorrente)
        assertEquals(1, r.semComprovante.size)
        assertTrue(r.semComprovante.single(), r.semComprovante.single().startsWith(if (ultimo.tipo == TipoComprovante.APOSTA) "Aposta" else "Apura"))
        assertFalse(r.integro)
        // Apagar um do meio quebra a corrente.
        val meio = exemplo.copy(comprovantes = exemplo.comprovantes.filterIndexed { i, _ -> i != 3 })
        assertEquals(3, Integridade.verificar(meio) { it.sha256 }.quebraNaCorrente)
    }

    @Test
    fun buscaPeloCodigoImpresso() {
        val c = exemplo.comprovantes[5]
        // O comprovante traz 8 algarismos: "4829-1736".
        val impresso = Autenticacao.formatar(c.autenticacao)
        assertTrue(impresso, impresso.matches(Regex("\\d{4}-\\d{4}")))
        assertTrue(c in Integridade.buscar(exemplo, impresso))
        assertTrue(c in Integridade.buscar(exemplo, impresso.replace("-", " ")))
        assertTrue(Integridade.buscar(exemplo, impresso).all { Autenticacao.curto(it.autenticacao) == Autenticacao.curto(c.autenticacao) })
        assertFalse(Integridade.podeBuscar(impresso.take(6)))
        assertEquals(emptyList<Comprovante>(), Integridade.buscar(exemplo, impresso.take(6)))
        // Comprovantes das versões até a 0.4 traziam o começo do SHA-256 (24 caracteres).
        val antigo = c.autenticacao.take(24).chunked(4).joinToString("-").lowercase()
        assertEquals(listOf(c), Integridade.buscar(exemplo, antigo))
        assertTrue(Integridade.conferePosicao(exemplo, c))
        val adulterado = exemplo.copy(comprovantes = exemplo.comprovantes.mapIndexed { i, x -> if (i == 2) x.copy(texto = x.texto + " ") else x })
        assertFalse(Integridade.conferePosicao(adulterado, adulterado.comprovantes[5]))
        assertTrue(Integridade.conferePosicao(adulterado, adulterado.comprovantes[1]))
    }

    @Test
    fun registrosDaVersaoAntigaNaoSaoCobrados() {
        val antigo = """
            {"versao":1,"cadastroConcluido":true,"proximoJogo":2,
             "pessoas":[{"id":"p1","nome":"Fulano de Tal","apelido":"Fulano","chavePix":"f@exemplo.com","administrador":true}],
             "lancamentos":[
               {"id":"l1","data":"2026-10-01","criadoEm":100,"tipo":"APORTE","valor":3000},
               {"id":"l2","data":"2026-10-01","criadoEm":200,"tipo":"APOSTA","valor":-350,"apostaId":"a"}],
             "apostas":[{"id":"a","numero":1,"modalidade":"LOTOFACIL","concurso":3500,"dataAposta":"2026-10-01","valor":350,"registrada":true,"criadaEm":150}]}
        """.trimIndent()
        val json = Json { ignoreUnknownKeys = true }
        val e = Migracao.migrar(json.decodeFromString(Estado.serializer(), antigo), hoje)
        assertEquals(200L, e.legadoAte)
        val r = Integridade.verificar(e) { it.sha256 }
        assertTrue(r.semComprovante.toString(), r.semComprovante.isEmpty())
        assertEquals(2, r.legados)
        assertTrue(r.integro)
        // Um aporte novo, depois da migração, precisa de comprovante.
        val novo = e.copy(lancamentos = e.lancamentos + Lancamento("l3", hoje, 300, TipoLancamento.APORTE, 1000, eventual = true))
        assertEquals(1, Integridade.verificar(novo) { it.sha256 }.semComprovante.size)
    }

    @Test
    fun inicioDosAportesEscolhidoNoCadastro() {
        val e = Estado(cadastroConcluido = true, cadastradoEm = LocalDate.of(2026, 10, 1), inicioAportes = "2026-11")
        assertFalse(Operacoes.aporteMensalDevido(e, java.time.YearMonth.of(2026, 10)))
        assertTrue(Operacoes.aporteMensalDevido(e, java.time.YearMonth.of(2026, 11)))
        // Sem a escolha: cadastro no dia 1º cobra o mês do cadastro.
        assertTrue(Operacoes.aporteMensalDevido(e.copy(inicioAportes = null), java.time.YearMonth.of(2026, 10)))
    }
}

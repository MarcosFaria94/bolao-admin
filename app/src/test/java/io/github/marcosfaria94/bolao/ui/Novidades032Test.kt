package io.github.marcosfaria94.bolao.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.PremiosTipicos
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.domain.indices.RegistroK
import io.github.marcosfaria94.bolao.net.Contato
import io.github.marcosfaria94.bolao.notificacoes.Toque
import io.github.marcosfaria94.bolao.ui.ajustes.GRUPO_APOIO
import io.github.marcosfaria94.bolao.ui.ajustes.SecaoAjuste
import io.github.marcosfaria94.bolao.ui.ajustes.alternarLista
import io.github.marcosfaria94.bolao.ui.ajustes.envolver
import io.github.marcosfaria94.bolao.ui.caixa.GestoDaCaixa
import io.github.marcosfaria94.bolao.ui.comum.trocaPorDeslize
import io.github.marcosfaria94.bolao.ui.jogos.OrdemDaTabela
import io.github.marcosfaria94.bolao.ui.jogos.nomeNaPremiacao
import io.github.marcosfaria94.bolao.ui.jogos.ordenaAsTabelas
import io.github.marcosfaria94.bolao.ui.jogos.textoDosGanhadores
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** As novidades da 0.32 que dão para testar sem as telas. */
class Novidades032Test {

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    private val parametros = ParametrosK.ler(arquivo("src/main/assets/parametros_k.json", "app/src/main/assets/parametros_k.json").readText())
    private val casos: List<JsonObject> = Json.parseToJsonElement(
        arquivo("src/test/resources/casos_de_teste.json", "app/src/test/resources/casos_de_teste.json").readText(),
    ).jsonObject["casos"]!!.jsonArray.map { it.jsonObject }

    private fun caso(id: String) = casos.first { it["id"]!!.jsonPrimitive.content == id }

    @Test
    fun premiosTipicosPeloCalculoDoK1() {
        for (c in casos) {
            val id = c["id"]!!.jsonPrimitive.content
            val chave = c["chave"]!!.jsonPrimitive.content
            val u = RegistroK.deJson(c["ultimo"]!!.jsonObject)
            val a = RegistroK.deJson(c["anterior"]!!.jsonObject)
            val est = PremiosTipicos.estimar(parametros, chave, u, a)
            val semIr = IndiceK.calcular(parametros.copy(irAliquota = 0.0), chave, u, a)
            if (semIr.k1 == null) {
                // O concurso especial não tem o k1: nem a estimativa.
                assertNull(id, est)
                continue
            }
            assertNotNull(id, est)
            val m = parametros.modalidades.getValue(chave)
            assertEquals(id, m.faixas.map { it.faixa }.toSet(), est!!.keys)
            for (f in m.faixas) {
                val v = est.getValue(f.faixa)
                assertTrue("$id ${f.nome}: $v", v > 0)
                // As faixas de valor fixo pagam o valor da Caixa.
                if (f.fixo) assertEquals("$id ${f.nome}", Math.round(f.valor * 100), v)
            }
            // A conta volta ao k1 (sem o IR): a soma de chance × prêmio ÷ preço (com os centavos arredondados).
            val k1 = m.faixas.sumOf { f -> f.p * est.getValue(f.faixa) / 100.0 } / m.preco
            assertTrue("$id: $k1 x ${semIr.k1}", abs(k1 - semIr.k1!!) < 2e-4)
            // A faixa principal vale mais que as outras.
            val principal = m.faixas.first { it.principal }.faixa
            assertTrue(id, m.faixas.filter { !it.principal }.all { est.getValue(it.faixa) < est.getValue(principal) })
        }
    }

    @Test
    fun premiosTipicosNaTabela() {
        assertEquals(listOf("Sena", "Quina", "Quadra"), PremiosTipicos.nomes(Modalidade.MEGA_SENA))
        assertEquals(8, PremiosTipicos.nomes(Modalidade.DUPLA_SENA).size)
        assertEquals("Sena (2º)", PremiosTipicos.nomes(Modalidade.DUPLA_SENA)[4])
        assertEquals("6 + 0/1", PremiosTipicos.nomes(Modalidade.MAIS_MILIONARIA)[1])
        assertEquals(listOf("14 acertos", "13 acertos"), PremiosTipicos.nomes(Modalidade.LOTECA))
        val ultimo = Concurso(
            Modalidade.MEGA_SENA, 3000, null, listOf(1, 2, 3, 4, 5, 6),
            faixas = listOf(FaixaPremio(1, "6 acertos", 0, 0), FaixaPremio(2, "5 acertos", 40, 4_512_345), FaixaPremio(3, "4 acertos", 3000, 98_765)),
        )
        val linhas = PremiosTipicos.linhas(Modalidade.MEGA_SENA, mapOf(1 to 5_000_000_000L, 2 to 4_000_000L, 3 to 100_000L), ultimo)
        assertEquals(listOf(null, 4_512_345L, 98_765L), linhas.map { it.ultimo })
        assertEquals(5_000_000_000L, linhas[0].estimativa)
        // Sem a estimativa (Loteca, especial): só o último concurso.
        assertTrue(PremiosTipicos.linhas(Modalidade.MEGA_SENA, null, ultimo).all { it.estimativa == null })
        assertEquals("R$ 987,65", PremiosTipicos.valor(98_765))
        assertEquals("R$ 45.123,45", PremiosTipicos.valor(4_512_345))
        assertEquals("R$ 150 Mil", PremiosTipicos.valor(15_000_000))
        assertEquals("R$ 50 Milhões", PremiosTipicos.valor(5_000_000_000))
    }

    @Test
    fun maisMilionariaComANomenclaturaNova() {
        assertEquals(
            listOf("6 + 2", "6 + 0/1", "5 + 2", "5 + 0/1", "4 + 2", "4 + 0/1", "3 + 2", "3 + 1", "2 + 2", "2 + 1"),
            Probabilidades.colunas(Modalidade.MAIS_MILIONARIA),
        )
        assertTrue(Probabilidades.observacoesDaTabela(Modalidade.MAIS_MILIONARIA).any { "6 + 0/1" in it })
        // O traço: só com os 6 trevos marcados (as faixas de 1 trevo ou nenhum não acontecem).
        val tabela = Probabilidades.tabela(Modalidade.MAIS_MILIONARIA, true)
        assertTrue(Probabilidades.temTraco(tabela))
        assertTrue(tabela.filter { l -> l.umEm.any { it == null } }.all { it.trevos == 6 })
        assertFalse(Probabilidades.temTraco(Probabilidades.tabela(Modalidade.MEGA_SENA, true)))
        assertTrue(Probabilidades.legendaDoTraco(Modalidade.MAIS_MILIONARIA).contains("6 trevos"))
        // Na premiação, o nome curto pela posição da faixa; nas outras, o da Caixa.
        assertEquals("6 + 0/1", nomeNaPremiacao(Modalidade.MAIS_MILIONARIA, FaixaPremio(2, "6 acertos + 1 ou nenhum trevo", 0, 0)))
        assertEquals("5 acertos", nomeNaPremiacao(Modalidade.QUINA, FaixaPremio(1, "5 acertos", 0, 0)))
        assertEquals("3ª faixa", nomeNaPremiacao(Modalidade.QUINA, FaixaPremio(3, "", 0, 0)))
        assertEquals("1.234 apostas ganhadoras", textoDosGanhadores(1234))
        // As tabelas da +Milionária se ordenam (com mais de uma linha).
        assertTrue(ordenaAsTabelas(Modalidade.MAIS_MILIONARIA, tabela))
        assertFalse(ordenaAsTabelas(Modalidade.MAIS_MILIONARIA, Probabilidades.tabela(Modalidade.MAIS_MILIONARIA, false)))
        assertFalse(ordenaAsTabelas(Modalidade.MEGA_SENA, Probabilidades.tabela(Modalidade.MEGA_SENA, true)))
    }

    @Test
    fun ordemDaTabela() {
        val valores: List<List<Long?>> = listOf(listOf(30L), listOf(10L), listOf(null), listOf(20L), listOf(10L))
        assertEquals(listOf(0, 1, 2, 3, 4), OrdemDaTabela().linhas(5, valores))
        val crescente = OrdemDaTabela().tocar(0)
        // Empates na ordem de sempre; o "—" no fim.
        assertEquals(listOf(1, 4, 3, 0, 2), crescente.linhas(5, valores))
        val decrescente = crescente.tocar(0)
        assertFalse(decrescente.crescente)
        assertEquals(listOf(0, 3, 1, 4, 2), decrescente.linhas(5, valores))
        // Outra coluna começa do menor para o maior.
        assertEquals(OrdemDaTabela(1, true), decrescente.tocar(1))
        assertEquals(listOf(0, 1, 2), OrdemDaTabela(0, true).linhas(3, null))
    }

    @Test
    fun deslizarEntreOsBotoes() {
        // Para a esquerda, o próximo; para a direita, o anterior.
        assertEquals(1, trocaPorDeslize(0, 5, dx = -40f, dy = 5f))
        assertEquals(2, trocaPorDeslize(3, 5, dx = 40f, dy = -5f))
        // Nos extremos, o gesto fica para a página (sai dos Detalhes).
        assertNull(trocaPorDeslize(0, 5, dx = 40f, dy = 0f))
        assertNull(trocaPorDeslize(4, 5, dx = -40f, dy = 0f))
        // Mais para cima ou para baixo: rola a página.
        assertNull(trocaPorDeslize(2, 5, dx = -20f, dy = 30f))
    }

    @Test
    fun gestoNaPaginaDaCaixa() {
        val g = GestoDaCaixa(limite = 10f)
        // Para os lados, com a página sem ter para onde rolar: troca de aba, e o arrasto vai para as abas.
        g.inicio(100f, 300f)
        assertEquals(0f, g.mover(105f, 302f) { false })
        assertEquals(GestoDaCaixa.Modo.INDEFINIDO, g.modo)
        assertEquals(0f, g.mover(80f, 303f) { false })
        assertEquals(GestoDaCaixa.Modo.TROCA, g.modo)
        assertEquals(-30f, g.mover(50f, 310f) { false })
        g.fim()
        // A página ampliada ainda rola para aquele lado: fica com a página.
        g.inicio(100f, 300f)
        g.mover(70f, 300f) { lado -> lado == 1 }
        assertEquals(GestoDaCaixa.Modo.PAGINA, g.modo)
        assertEquals(0f, g.mover(10f, 300f) { true })
        // Para cima ou para baixo: a página rola.
        g.inicio(100f, 300f)
        g.mover(110f, 260f) { false }
        assertEquals(GestoDaCaixa.Modo.PAGINA, g.modo)
        // Dois dedos (ampliar): a página.
        g.inicio(100f, 300f)
        g.outroDedo()
        g.mover(40f, 300f) { false }
        assertEquals(GestoDaCaixa.Modo.PAGINA, g.modo)
    }

    @Test
    fun contatoComOLimiteDeCincoMinutos() {
        val agora = 1_000_000_000L
        assertEquals(0L, Contato.espera(0L, agora))
        assertEquals(Contato.INTERVALO_MS, Contato.espera(agora, agora))
        assertEquals(60_000L, Contato.espera(agora - 4 * 60_000L, agora))
        assertEquals(0L, Contato.espera(agora - 5 * 60_000L, agora))
        // O relógio do celular voltou: no máximo os 5 minutos.
        assertEquals(Contato.INTERVALO_MS, Contato.espera(agora + 3_600_000L, agora))
        assertEquals("4 min", Contato.textoDaEspera(3 * 60_000L + 1))
        assertEquals("menos de 1 min", Contato.textoDaEspera(30_000L))
        assertEquals("Bolão dos Amigos · Reportar bug", Contato.assunto(Contato.Motivo.BUG))
        assertTrue(Contato.emailValido(""))
        assertTrue(Contato.emailValido("alguem@exemplo.com"))
        assertFalse(Contato.emailValido("alguem@"))
        val json = Json.parseToJsonElement(Contato.json(Contato.Motivo.SUGESTAO, " Oi ", "a@b.co", null)).jsonObject
        assertEquals("Bolão dos Amigos · Sugestão", json["_subject"]!!.jsonPrimitive.content)
        assertEquals("Oi", json["Mensagem"]!!.jsonPrimitive.content)
        assertEquals("a@b.co", json["_replyto"]!!.jsonPrimitive.content)
        assertNull(json["Aparelho"])
        assertNull(Json.parseToJsonElement(Contato.json(Contato.Motivo.MENSAGEM, "x", "", null)).jsonObject["email"])
        assertEquals(Contato.Resultado.Enviada, Contato.interpretar(200, """{"success":"true","message":"The form was submitted successfully."}"""))
        assertEquals(Contato.Resultado.Enviada, Contato.interpretar(200, """{"success":true}"""))
        assertEquals(
            Contato.Resultado.Guardada,
            Contato.interpretar(200, """{"success":"false","message":"This form needs Activation. We've sent you an email containing an 'Activate Form' button."}"""),
        )
        assertTrue(Contato.interpretar(500, "erro") is Contato.Resultado.Erro)
        assertTrue(Contato.interpretar(200, """{"success":"false","message":"Bad"}""") is Contato.Resultado.Erro)
        assertTrue(Contato.corpo(Contato.Motivo.DUVIDA, "Pergunta", "a@b.co", "App 0.32").contains("Responder para: a@b.co"))
    }

    @Test
    fun edicaoDaMensagem() {
        // Negrito no trecho escolhido.
        val v = envolver(TextFieldValue("um dois tres", TextRange(3, 7)), "*")
        assertEquals("um *dois* tres", v.text)
        assertEquals(TextRange(4, 8), v.selection)
        // Sem trecho escolhido: as marcas no cursor, com ele no meio.
        val c = envolver(TextFieldValue("ab", TextRange(1)), "_")
        assertEquals("a__b", c.text)
        assertEquals(TextRange(2), c.selection)
        // A linha do cursor vira item de lista; de novo, deixa de ser.
        val l = alternarLista(TextFieldValue("primeira\nsegunda", TextRange(12)))
        assertEquals("primeira\n• segunda", l.text)
        assertEquals("primeira\nsegunda", alternarLista(l).text)
        // Várias linhas escolhidas.
        val varias = alternarLista(TextFieldValue("a\nb\nc", TextRange(0, 3)))
        assertEquals("• a\n• b\nc", varias.text)
        assertEquals("• x", alternarLista(TextFieldValue("x", TextRange(0))).text)
    }

    @Test
    fun ajustesEParametros() {
        // Sessões novas: com mais números e teimosinha.
        assertTrue(Parametros().maisNumeros)
        assertTrue(Parametros().teimosinha)
        assertFalse(Parametros().semConfirmacao)
        // O grupo Apoio, com a Doação e o Contato, no fim (nos dois modos).
        for (individual in listOf(false, true)) {
            val grupos = SecaoAjuste.grupos(individual)
            assertEquals(GRUPO_APOIO, grupos.last().first)
            assertEquals(listOf(SecaoAjuste.DOACAO, SecaoAjuste.CONTATO), grupos.last().second)
        }
        assertEquals(SecaoAjuste.DOACAO, SecaoAjuste.de("doacao"))
        assertEquals(SecaoAjuste.CONTATO, SecaoAjuste.de("contato"))
        assertEquals("ajustes/contato", SecaoAjuste.CONTATO.rota)
        // O som do prêmio secundário é outro (começa desligado).
        assertEquals(Toque.Qual.SECUNDARIO, Toque.Origem.SECUNDARIO.qual)
        assertEquals(Toque.Qual.PRINCIPAL, Toque.Origem.PREMIO.qual)
        assertFalse(Toque.Qual.SECUNDARIO.ligadoPadrao)
        assertTrue(Toque.Qual.PRINCIPAL.ligadoPadrao)
        assertEquals(Toque.SOM_DE_NOTIFICACAO, Toque.nomeDoToque(Toque.Config(true, null, null), Toque.Qual.SECUNDARIO))
        assertEquals(Toque.TOQUE_DO_CELULAR, Toque.nomeDoToque(Toque.Config(true, null, null)))
        // A Federal (fora do app) e a especial: sem o caso de lá, a estimativa da virada é nula.
        val virada = caso("megasena_especial_virada")
        assertNull(
            PremiosTipicos.estimar(
                parametros, "megasena",
                RegistroK.deJson(virada["ultimo"]!!.jsonObject), RegistroK.deJson(virada["anterior"]!!.jsonObject),
            ),
        )
        assertNull(PremiosTipicos.estimar(parametros, "loteca", RegistroK(numero = 2), RegistroK(numero = 1)))
    }
}

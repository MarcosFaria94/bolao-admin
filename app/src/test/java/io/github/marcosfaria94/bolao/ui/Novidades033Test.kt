package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Historico
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.IndiceEspecial
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.domain.indices.RegistroK
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.net.CaixaParser
import io.github.marcosfaria94.bolao.ui.ajustes.GRUPO_FEEDBACK
import io.github.marcosfaria94.bolao.ui.ajustes.SecaoAjuste
import io.github.marcosfaria94.bolao.ui.caixa.posicaoDaAba
import io.github.marcosfaria94.bolao.ui.jogos.IR_AO_CONCURSO
import io.github.marcosfaria94.bolao.ui.jogos.ROTULO_DO_CONCURSO
import io.github.marcosfaria94.bolao.ui.jogos.SEM_INDICES_NO_ESPECIAL
import io.github.marcosfaria94.bolao.ui.jogos.indicesDoCartao
import io.github.marcosfaria94.bolao.ui.jogos.linhaIndices
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/** As novidades da 0.33 que dão para testar sem as telas. */
class Novidades033Test {

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    private val parametros = ParametrosK.ler(arquivo("src/main/assets/parametros_k.json", "app/src/main/assets/parametros_k.json").readText())

    /** Um especial da Mega-Sena já sorteado: arrecadou [arrecadado] e pagou [pagos] (faixa → ganhadores e prêmio). */
    private fun especial(numero: Int, arrecadado: Double, pagos: Map<Int, Pair<Int, Double>>, indicador: Int = 2) = RegistroK(
        numero = numero,
        valorArrecadado = arrecadado,
        indicadorConcursoEspecial = indicador,
        ganhadores = (1..3).associateWith { pagos[it]?.first ?: 0 },
        premios = (1..3).associateWith { pagos[it]?.second ?: 0.0 },
    )

    @Test
    fun retornoDeUmEspecial() {
        val m = parametros.modalidades.getValue("megasena")
        // Arrecadou R$ 1 bilhão e pagou R$ 300 milhões na sena, R$ 50 milhões na quina e R$ 30 milhões na quadra.
        val r = especial(2955, 1_000_000_000.0, mapOf(1 to (3 to 100_000_000.0), 2 to (1000 to 50_000.0), 3 to (30_000 to 1_000.0)))
        val (k1, k2) = IndiceEspecial.retorno(m, parametros.k2PMinimo, r)!!
        assertEquals(0.38, k1, 1e-9)
        // Nenhuma faixa da Mega tem chance de 1 em 1.000 (a quadra é 1 em 2.332): o k2 é zero, como nos concursos normais.
        assertEquals(0.0, k2, 1e-12)
        // Sem a arrecadação ou com uma faixa com ganhador e sem o valor: não dá para saber.
        assertNull(IndiceEspecial.retorno(m, parametros.k2PMinimo, r.copy(valorArrecadado = 0.0)))
        assertNull(IndiceEspecial.retorno(m, parametros.k2PMinimo, r.copy(premios = r.premios + (3 to 0.0))))
        // Um retorno absurdo (dado quebrado da Caixa) fica de fora.
        assertNull(IndiceEspecial.retorno(m, parametros.k2PMinimo, r.copy(valorArrecadado = 1_000.0)))
    }

    @Test
    fun mediaPonderadaDosUltimosEspeciais() {
        // Sete especiais (k1 0,30 a 0,36, do mais velho ao mais novo) e um concurso normal no meio: entram os 5 mais novos,
        // com pesos 5 (o mais novo) a 1.
        val lista = (0 until 7).map { i ->
            val k1 = 0.30 + i * 0.01
            especial(1000 + i * 365, 1_000_000.0, mapOf(1 to (1 to k1 * 1_000_000.0)))
        } + especial(5000, 1_000_000.0, mapOf(1 to (1 to 900_000.0)), indicador = 1)
        val e = IndiceEspecial.estimar(parametros, "megasena", lista.shuffled())!!
        assertEquals(listOf(3190, 2825, 2460, 2095, 1730), e.amostras.map { it.numero })
        assertEquals(listOf(5.0, 4.0, 3.0, 2.0, 1.0), e.amostras.map { it.peso })
        val esperado = (5 * 0.36 + 4 * 0.35 + 3 * 0.34 + 2 * 0.33 + 1 * 0.32) / 15
        assertEquals(esperado, e.k1, 1e-9)
        // Com um especial só, ele mesmo; sem nenhum, nada.
        assertEquals(0.31, IndiceEspecial.estimar(parametros, "megasena", lista.take(2).drop(1))!!.k1, 1e-9)
        assertNull(IndiceEspecial.estimar(parametros, "megasena", emptyList()))
        assertNull(IndiceEspecial.estimar(parametros, "loteca", lista))
    }

    @Test
    fun soOsEspeciaisDaMesmaEpoca() {
        fun r(numero: Int, data: String, k1: Double) = especial(numero, 1_000_000.0, mapOf(1 to (1 to k1 * 1_000_000.0))).copy(dataApuracao = data)
        val lista = listOf(
            r(2810, "31/12/2024", 0.40), r(2955, "01/01/2026", 0.39), r(3010, "24/05/2026", 0.67),
        )
        val epoca = Especiais.epoca(Modalidade.MEGA_SENA)!!
        val e = IndiceEspecial.estimar(parametros, "megasena", lista, epoca = epoca)!!
        assertEquals(listOf(2955, 2810), e.amostras.map { it.numero })
        assertEquals((2 * 0.39 + 1 * 0.40) / 3, e.k1, 1e-9)
        // Sem a época, entram todos os especiais (o de maio, o mais novo, pesa mais).
        assertEquals(3010, IndiceEspecial.estimar(parametros, "megasena", lista)!!.amostras.first().numero)
        // A virada do ano: 1º de janeiro é da época de 31 de dezembro; 15 de fevereiro já não é.
        assertTrue(IndiceEspecial.daEpoca(java.time.LocalDate.of(2027, 1, 1), epoca))
        assertFalse(IndiceEspecial.daEpoca(java.time.LocalDate.of(2027, 2, 20), epoca))
        // A Páscoa muda de data: de 22 de março a 25 de abril, tudo é da época da Dupla de Páscoa.
        val pascoa = Especiais.epoca(Modalidade.DUPLA_SENA)!!
        assertTrue(IndiceEspecial.daEpoca(java.time.LocalDate.of(2024, 3, 23), pascoa))
        assertTrue(IndiceEspecial.daEpoca(java.time.LocalDate.of(2025, 4, 19), pascoa))
    }

    @Test
    fun cartoesDoEspecialComK1() {
        val k = ResultadoK(3100, true, 0.0, 0, emptyMap(), 0.0, k1 = 0.412, k2 = 0.0)
        val item = Indices.Item(Modalidade.MEGA_SENA, null, k, odds = 2332.0)
        assertEquals(listOf("k1" to "0,412", "k2" to "0,000", "odds" to "1:2332"), indicesDoCartao(item))
        // Sem o histórico dos especiais: o texto de sempre.
        val sem = item.copy(k = k.copy(k1 = null, k2 = null))
        assertNull(indicesDoCartao(sem))
        assertTrue(linhaIndices(sem).startsWith(SEM_INDICES_NO_ESPECIAL))
    }

    @Test
    fun indicesGuardadosNaApostaDoEspecial() {
        val k = ResultadoK(3095, false, 0.0, 0, emptyMap(), 0.0, k1 = 0.3, k2 = 0.0)
        val itens = mapOf(Modalidade.MEGA_SENA to Indices.Item(Modalidade.MEGA_SENA, null, k, odds = 2332.0))
        val doEspecial = ResultadoK(3100, true, 0.0, 0, emptyMap(), 0.0, k1 = 0.45, k2 = 0.0)
        val e0 = io.github.marcosfaria94.bolao.data.DadosDeExemplo.gerar(
            java.time.LocalDate.of(2026, 11, 13), 1_700_000_000_000L, java.time.ZoneId.of("America/Sao_Paulo"), semente = 7,
        )
        val modelo = e0.apostas.first { it.registrada }
        val aposta = modelo.copy(id = "virada", modalidade = Modalidade.MEGA_SENA, concurso = 3100, indices = null, apuracao = null)
        val e = e0.copy(apostas = e0.apostas + aposta)
        val guardado = io.github.marcosfaria94.bolao.data.Operacoes.guardarIndices(e, itens) { m, n ->
            doEspecial.takeIf { m == Modalidade.MEGA_SENA && n == 3100 }
        }
        val a: Aposta = guardado.apostas.first { it.id == "virada" }
        assertEquals(0.45, a.indices!!.k1!!, 1e-12)
        assertTrue(a.indices!!.especial)
        // Sem a estimativa do especial, a aposta fica sem os índices (como antes).
        val nada = io.github.marcosfaria94.bolao.data.Operacoes.guardarIndices(e, itens)
        assertNull(nada.apostas.first { it.id == "virada" }.indices)
    }

    @Test
    fun linhaDoHistorico() {
        val texto = """{"numero":2955,"tipoJogo":"MEGA_SENA","dataApuracao":"31/12/2025","listaDezenas":["09","13","21","32","33","59"],
            |"listaMunicipioUFGanhadores":[{"municipio":"X"}],"acumulado":false,"indicadorConcursoEspecial":2,"valorArrecadado":2500000000.5,
            |"nomeTimeCoracaoMesSorte":"","trevosSorteados":[],"listaRateioPremio":[{"descricaoFaixa":"6 acertos","faixa":1,
            |"numeroDeGanhadores":6,"valorPremio":181000000.0,"extra":1},{"descricaoFaixa":"5 acertos","faixa":2,"numeroDeGanhadores":3000,
            |"valorPremio":40000.0},{"descricaoFaixa":"4 acertos","faixa":3,"numeroDeGanhadores":200000,"valorPremio":900.0}]}""".trimMargin().replace("\n", "")
        val linha = Historico.reduzir(texto)
        // Só os campos que o app lê, numa linha; o número primeiro e a marca do especial sem espaços.
        assertTrue(linha.startsWith("{\"numero\":2955,"))
        assertFalse(linha.contains("listaMunicipioUFGanhadores") || linha.contains("tipoJogo") || linha.contains("extra"))
        assertFalse(linha.contains("nomeTimeCoracaoMesSorte") || linha.contains("trevosSorteados"))
        assertTrue(linha.contains(Historico.MARCA_DO_ESPECIAL))
        assertEquals(2955, Historico.numeroDa(linha))
        // Lida de volta, é o mesmo concurso (e o registro do k1 dos especiais sai dela).
        val c = CaixaParser.ler(Modalidade.MEGA_SENA, linha)
        assertEquals(listOf(9, 13, 21, 32, 33, 59), c.dezenas)
        assertEquals(3, c.faixas.size)
        assertTrue(Historico.completo(c))
        assertEquals(2, c.registro!!.indicadorConcursoEspecial)
        assertEquals(2_500_000_000.5, c.registro!!.valorArrecadado, 1e-6)
        // Sem o rateio completo (ganhador sem o valor), não entra no histórico.
        assertFalse(Historico.completo(c.copy(faixas = c.faixas.map { it.copy(valor = 0) })))
        assertFalse(Historico.completo(Concurso(Modalidade.MEGA_SENA, 3000, null, emptyList(), faixas = listOf(FaixaPremio(1, "6", 0, 0)))))
    }

    /** O histórico embutido: um arquivo por modalidade, os concursos em ordem, e o índice confere com eles. */
    @Test
    fun historicoEmbutido() {
        val pasta = arquivo("src/main/assets/historico", "app/src/main/assets/historico")
        val indice = Json.parseToJsonElement(File(pasta, "indice.json").readText()).jsonObject
        for (m in Modalidade.entries) {
            val f = File(pasta, "${m.slugApi}.jsonl.gzip")
            assertTrue("falta o histórico da ${m.nome}", f.isFile)
            val numeros = GZIPInputStream(f.inputStream()).bufferedReader().readLines().map { Historico.numeroDa(it)!! }
            assertEquals("${m.nome} fora de ordem", numeros.sorted().distinct(), numeros)
            val ultimo = indice[m.slugApi]!!.jsonObject["ultimo"]!!.jsonPrimitive.intOrNull!!
            assertEquals(ultimo, numeros.last())
            // O índice diz quais faltam (os que a Caixa não entregou: vão à Caixa quando pedidos).
            val faltam = indice[m.slugApi]!!.jsonObject["faltam"]!!.jsonArray.map { it.jsonPrimitive.intOrNull!! }
            assertEquals(ultimo, numeros.size + faltam.size)
            assertTrue(faltam.none { it in numeros })
            assertTrue("${m.nome}: só ${numeros.size} de $ultimo", numeros.size >= ultimo * 0.85)
            // Toda linha se lê como a resposta da Caixa (o mesmo concurso, com o resultado e o rateio).
            val ruins = GZIPInputStream(f.inputStream()).bufferedReader().readLines().filter { l ->
                val c = runCatching { CaixaParser.ler(m, l) }.getOrNull()
                c == null || c.numero != Historico.numeroDa(l) || !Historico.completo(c)
            }
            assertTrue("${m.nome}: ${ruins.size} linhas que não se leem (${ruins.firstOrNull()?.take(200)})", ruins.isEmpty())
        }
        // A Mega da Virada de 2025 (concurso 2955) e os especiais que dão o k1 do próximo.
        val linhas = GZIPInputStream(File(pasta, "megasena.jsonl.gzip").inputStream()).bufferedReader().readLines()
        val especiais = linhas.filter { it.contains(Historico.MARCA_DO_ESPECIAL) }
            .map { RegistroK.deJson(Json.parseToJsonElement(it) as JsonObject) }
        assertTrue(especiais.any { it.numero == 2955 })
        // Só as Mega da Virada (o especial de maio de 2026, concurso 3010, é de outro tipo e fica de fora).
        val e = IndiceEspecial.estimar(parametros, "megasena", especiais, epoca = Especiais.epoca(Modalidade.MEGA_SENA))!!
        assertEquals(IndiceEspecial.QUANTOS, e.amostras.size)
        assertEquals(listOf(2955, 2810, 2670, 2550, 2440), e.amostras.map { it.numero })
        assertEquals("01/01/2026", e.amostras.first().data)
        assertTrue("k1 da Mega da Virada: ${e.k1}", e.k1 in 0.3..0.5)
        assertEquals(0.0, e.k2, 1e-12)
        for (chave in listOf("quina", "lotofacil", "duplasena")) {
            val m = Modalidade.entries.first { it.slugApi == chave }
            val l = GZIPInputStream(File(pasta, "${m.slugApi}.jsonl.gzip").inputStream()).bufferedReader().readLines()
                .filter { it.contains(Historico.MARCA_DO_ESPECIAL) }.map { RegistroK.deJson(Json.parseToJsonElement(it) as JsonObject) }
            val est = IndiceEspecial.estimar(parametros, chave, l, epoca = Especiais.epoca(m))
            assertNotNull("sem estimativa para $chave", est)
            assertTrue("k1 de $chave: ${est!!.k1}", est.k1 in 0.2..1.2)
        }
    }

    @Test
    fun telasDa033() {
        // Os Detalhes: o campo é só "Concurso" e o botão "Ir" fica embaixo.
        assertEquals("Concurso", ROTULO_DO_CONCURSO)
        assertEquals("Ir", IR_AO_CONCURSO)
        // O último grupo dos Ajustes se chama FEEDBACK (era APOIO).
        assertEquals("FEEDBACK", GRUPO_FEEDBACK)
        for (individual in listOf(false, true)) assertEquals(GRUPO_FEEDBACK, SecaoAjuste.grupos(individual).last().first)
        // A página da Caixa congelada anda com as abas (Home, Jogos, Caixa, Ajustes, girando): a aba da Caixa está nas
        // páginas 2, 6, 10...
        val caixa = { k: Int -> k.mod(4) == 2 }
        assertEquals(0f, posicaoDaAba(6f, 4, caixa)!!, 1e-6f)
        // A caminho dos Ajustes (da Caixa para a direita): saindo pela esquerda.
        assertEquals(-0.25f, posicaoDaAba(6.25f, 4, caixa)!!, 1e-6f)
        // Dos Jogos para a Caixa: entrando pela direita.
        assertEquals(0.4f, posicaoDaAba(5.6f, 4, caixa)!!, 1e-6f)
        // Na Home, nos Jogos e nos Ajustes assentados: fora da tela.
        assertNull(posicaoDaAba(4f, 4, caixa))
        assertNull(posicaoDaAba(5f, 4, caixa))
        assertNull(posicaoDaAba(7f, 4, caixa))
        // Dos Ajustes para a Home, a Caixa da volta seguinte (página 10) continua longe.
        assertNull(posicaoDaAba(7.9f, 4, caixa))
    }
}

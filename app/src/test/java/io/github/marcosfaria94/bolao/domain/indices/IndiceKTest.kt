package io.github.marcosfaria94.bolao.domain.indices

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/**
 * Confere o motor contra os 15 casos e as odds de casos_de_teste.json (Apêndice B do manual),
 * com a mesma tolerância da implementação de referência: diferença menor que 1e-9.
 */
class IndiceKTest {

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    private val parametros = ParametrosK.ler(arquivo("src/main/assets/parametros_k.json", "app/src/main/assets/parametros_k.json").readText())
    private val testes: JsonObject = Json.parseToJsonElement(
        (javaClass.classLoader?.getResource("casos_de_teste.json")?.readText()
            ?: arquivo("src/test/resources/casos_de_teste.json", "app/src/test/resources/casos_de_teste.json").readText()),
    ).jsonObject

    private fun relativo(a: Double, b: Double) = if (b == 0.0) abs(a) else abs(a / b - 1)

    @Test
    fun quinzeCasosConferem() {
        val casas = parametros.casasExibicao
        var pior = 0.0
        val resultados = mutableMapOf<String, ResultadoK>()
        for (caso in testes["casos"]!!.jsonArray.map { it.jsonObject }) {
            val id = caso["id"]!!.jsonPrimitive.content
            val chave = caso["chave"]!!.jsonPrimitive.content
            val res = IndiceK.calcular(
                parametros, chave,
                RegistroK.deJson(caso["ultimo"]!!.jsonObject),
                RegistroK.deJson(caso["anterior"]!!.jsonObject),
            )
            val esp = caso["esperado"]!!.jsonObject
            assertEquals(id, esp["numero"]!!.jsonPrimitive.int, res.numero)
            assertEquals(id, esp["especial"]!!.jsonPrimitive.boolean, res.especial)
            assertEquals(id, esp["reserva"]!!.jsonPrimitive.int, res.reserva)
            assertTrue(id, abs(res.acumuladoPrincipal - esp["acumulado_principal"]!!.jsonPrimitive.double) < 1e-6)
            val heranca = esp["heranca_faixas"]!!.jsonObject
            assertEquals(id, heranca.keys, res.herancaFaixas.keys.map { it.toString() }.toSet())
            heranca.forEach { (k, v) -> assertTrue(id, relativo(res.herancaFaixas.getValue(k.toInt()), v.jsonPrimitive.double) < 1e-9) }
            assertTrue(id, abs(res.nAnterior - esp["N_anterior"]!!.jsonPrimitive.double) < 1e-6)
            if (res.especial) {
                assertNull(id, res.k1)
                continue
            }
            assertNotNull(id, res.k1)
            val k1 = res.k1!!
            val k2 = res.k2!!
            val difK = max(abs(k1 - esp["k1"]!!.jsonPrimitive.double), abs(k2 - esp["k2"]!!.jsonPrimitive.double))
            var difF = 0.0
            esp["k1_por_faixa"]!!.jsonObject.forEach { (fx, v) ->
                difF = max(difF, abs(res.k1PorFaixa.getValue(fx.toInt()) - v.jsonPrimitive.double))
            }
            val difN = relativo(res.nPrevisto!!, esp["N_previsto"]!!.jsonPrimitive.double)
            assertTrue("$id sigma", abs(res.sigmaLog!! - esp["sigma_log"]!!.jsonPrimitive.double) < 1e-12)
            pior = maxOf(pior, difK, difF, difN)
            val exib = esp["exibicao"]!!.jsonObject
            assertEquals(id, exib["k1"]!!.jsonPrimitive.content, IndiceK.exibir(k1, casas))
            assertEquals(id, exib["k2"]!!.jsonPrimitive.content, IndiceK.exibir(k2, casas))
            resultados[id] = res
        }
        assertTrue("maior diferença $pior", pior < 1e-9)

        val k1Lotofacil = resultados.getValue("ao_vivo_lotofacil").k1!!
        for (caso in testes["casos"]!!.jsonArray.map { it.jsonObject }) {
            val esp = caso["esperado"]!!.jsonObject
            val rel = esp["k1_rel_lotofacil"] ?: continue
            val id = caso["id"]!!.jsonPrimitive.content
            val calc = resultados.getValue(id).k1!! / k1Lotofacil
            assertTrue(id, relativo(calc, rel.jsonPrimitive.double) < 1e-9)
            assertEquals(id, esp["exibicao"]!!.jsonObject["k1_rel_lotofacil"]!!.jsonPrimitive.content, IndiceK.exibir(calc, casas))
        }
    }

    @Test
    fun oddsConferem() {
        for (o in testes["odds"]!!.jsonArray.map { it.jsonObject }) {
            val chave = o["chave"]!!.jsonPrimitive.content
            val (p, x) = IndiceK.odds(parametros.modalidades.getValue(chave))
            assertTrue(chave, relativo(p, o["p_qualquer_premio"]!!.jsonPrimitive.double) < 1e-12)
            assertTrue(chave, relativo(x, o["um_em"]!!.jsonPrimitive.double) < 1e-12)
            assertEquals(chave, o["exibicao"]!!.jsonPrimitive.content, IndiceK.exibirOdds(x))
        }
    }

    @Test
    fun parametrosLidos() {
        assertEquals("1.2", parametros.versao)
        assertEquals(9, parametros.modalidades.size)
        assertEquals(9, parametros.ghNos.size)
        assertEquals(listOf(2, 3, 4, 5, 6), parametros.modalidades.getValue("maismilionaria").ordemCascata)
        assertEquals(0.72, parametros.modalidades.getValue("lotofacil").faixas.first().fracaoFinalZero!!, 0.0)
        assertNull(parametros.modalidades.getValue("lotofacil").faixas[2].r)
        assertEquals("https://loterias.caixa.gov.br/Style%20Library/json/params.txt", parametros.paramsUrl)
        assertEquals(2, parametros.basesReserva.size)
    }

    @Test
    fun exibicaoArredondaComoOPython() {
        assertEquals("0,647", IndiceK.exibir(0.6472949166944352, 3))
        assertEquals("0,000", IndiceK.exibir(0.0, 3))
        assertEquals("1,000", IndiceK.exibir(1.0, 3))
        assertEquals("1:2298", IndiceK.exibirOdds(2297.5612666360616))
        assertEquals("1:9,4", IndiceK.exibirOdds(9.443844149240453))
        assertEquals("1:100", IndiceK.exibirOdds(100.0))
    }

    @Test
    fun dadosIncompletosFicamSemK1() {
        val u = RegistroK(numero = 10, numeroConcursoProximo = 11, dataProximoConcurso = "20/10/2026")
        val r = IndiceK.calcular(parametros, "megasena", u, RegistroK(numero = 9))
        assertNull(r.k1)
        assertNull(r.k2)
        // Sem data do próximo concurso também não calcula (em vez de quebrar).
        val semData = u.copy(dataProximoConcurso = null, valorArrecadado = 1e8, valorEstimadoProximoConcurso = 1e8)
        assertNull(IndiceK.calcular(parametros, "megasena", semData, RegistroK(numero = 9)).k1)
    }
}

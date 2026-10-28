package io.github.marcosfaria94.bolao.domain.indices

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
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

/**
 * A auditoria das regras contra rateios de verdade pagos pela Caixa (casos_auditoria.json):
 * com as regras do parametros_k.json, todas as faixas batem (a diferença é de centavos). Se a
 * Caixa mudar uma fração ou um prêmio fixo, a faixa deixa de bater.
 */
class AuditoriaRegrasTest {

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    private val parametros = ParametrosK.ler(arquivo("src/main/assets/parametros_k.json", "app/src/main/assets/parametros_k.json").readText())
    private val casos: List<JsonObject> = Json.parseToJsonElement(
        javaClass.classLoader?.getResource("casos_auditoria.json")?.readText()
            ?: arquivo("src/test/resources/casos_auditoria.json", "app/src/test/resources/casos_auditoria.json").readText(),
    ).jsonObject["casos"]!!.jsonArray.map { it.jsonObject }

    private fun registro(o: JsonObject, chave: String): RegistroK? = (o[chave] as? JsonObject)?.let { RegistroK.deJson(it) }

    private fun caso(id: String) = casos.first { it["id"]!!.jsonPrimitive.content == id }

    /** O registro do caso com o prêmio de uma faixa trocado (como se a Caixa tivesse pago outro valor). */
    private fun comPremio(o: JsonObject, faixa: Int, valor: Double?, ganhadores: Int? = null): RegistroK {
        val ultimo = o["ultimo"]!!.jsonObject
        val lista = ultimo["listaRateioPremio"]!!.jsonArray.map { e ->
            val f = e.jsonObject
            if (f["faixa"]!!.jsonPrimitive.int != faixa) {
                f
            } else {
                JsonObject(
                    f + mapOf(
                        "valorPremio" to (valor?.let { JsonPrimitive(it) } ?: JsonNull),
                        "numeroDeGanhadores" to JsonPrimitive(ganhadores ?: f["numeroDeGanhadores"]!!.jsonPrimitive.int),
                    ),
                )
            }
        }
        return RegistroK.deJson(JsonObject(ultimo + ("listaRateioPremio" to JsonArray(lista))))
    }

    @Test
    fun rateiosDeVerdadeBatemComAsRegras() {
        assertEquals(9, casos.size)
        for (c in casos) {
            val id = c["id"]!!.jsonPrimitive.content
            val chave = c["chave"]!!.jsonPrimitive.content
            val r = AuditoriaRegras.auditar(parametros, chave, registro(c, "ultimo")!!, registro(c, "anterior"))
            assertNotNull(id, r)
            r!!
            val esperadas = c["faixas_conferidas"]!!.jsonArray.map { it.jsonPrimitive.int }
            assertEquals(id, esperadas, r.faixas.map { it.faixa })
            r.faixas.forEach { f ->
                assertTrue("$id faixa ${f.faixa}: esperado ${f.esperado}, pago ${f.pago}", f.confere)
                // Na prática, a diferença é de no máximo 3 centavos.
                assertTrue("$id faixa ${f.faixa}", kotlin.math.abs(f.pago - f.esperado) < 0.03)
            }
            assertTrue(id, r.confere)
        }
    }

    @Test
    fun herancaECascataEntramNaConta() {
        // Lotomania 2818: o 2817 não teve ganhador de 19 acertos e o pote dele veio para o 2818.
        val c = caso("lotomania_2818_heranca")
        val semAnterior = AuditoriaRegras.auditar(parametros, "lotomania", registro(c, "ultimo")!!, null)!!
        assertFalse(semAnterior.faixas.first { it.faixa == 2 }.confere)
        // Sem o anterior, o prêmio principal (que leva o acumulado) não é conferido.
        assertTrue(semAnterior.faixas.none { it.faixa == 1 })
        val comAnterior = AuditoriaRegras.auditar(parametros, "lotomania", registro(c, "ultimo")!!, registro(c, "anterior"))!!
        assertTrue(comAnterior.faixas.first { it.faixa == 2 }.confere)
        // +Milionária 400: sem ganhador na faixa 2, o pote dela desceu para a 3.
        val m = caso("maismilionaria_400_cascata")
        val r = AuditoriaRegras.auditar(parametros, "maismilionaria", registro(m, "ultimo")!!, registro(m, "anterior"))!!
        assertTrue(r.faixas.first { it.faixa == 3 }.confere)
    }

    @Test
    fun mudancaDeRegraNaoPassa() {
        // Quina: a faixa de 4 acertos passa a receber 20% a mais.
        val q = caso("quina_7148")
        val maior = AuditoriaRegras.auditar(parametros, "quina", comPremio(q, 2, 14430.2 * 1.2), null)!!
        assertFalse(maior.confere)
        assertEquals(listOf(2), maior.divergentes.map { it.faixa })
        // Lotofácil: o prêmio fixo de 11 acertos vai de R$ 7,00 para R$ 7,50.
        val l = caso("lotofacil_3809")
        val fixo = AuditoriaRegras.auditar(parametros, "lotofacil", comPremio(l, 5, 7.5), null)!!
        assertEquals(listOf(5), fixo.divergentes.map { it.faixa })
        assertEquals(7.0, fixo.divergentes.single().esperado, 0.0)
    }

    @Test
    fun megaSenaComQuinaEQuadraDe10PorCento() {
        // Até o 3072 valiam 13% (quina) e 15% (quadra); a partir do 3073, 10% e 10% (regra atual da Caixa).
        val c = caso("megasena_3072")
        val antigo = registro(c, "ultimo")!!
        assertTrue(AuditoriaRegras.auditar(parametros, "megasena", antigo, registro(c, "anterior"))!!.confere)
        val m = parametros.modalidades.getValue("megasena")
        assertEquals(0.13, m.faixas[1].fracaoNo(3072), 0.0)
        assertEquals(0.10, m.faixas[1].fracaoNo(3073), 0.0)
        assertEquals(0.15, m.faixas[2].fracaoNo(3072), 0.0)
        assertEquals(0.10, m.faixas[2].fracaoNo(3075), 0.0)

        // O mesmo rateio num concurso novo (3075): com 10%, cada ganhador recebe 10/13 e 10/15 do antigo.
        fun liquido(v: Double) = if (v > parametros.limiteIsencao) v * (1 - parametros.irAliquota) else v
        val base = antigo.valorArrecadado * m.fracPremio
        val quina = liquido(0.10 * base / antigo.ganhadores.getValue(2))
        val quadra = liquido(0.10 * base / antigo.ganhadores.getValue(3))
        val novo = antigo.copy(numero = 3075, premios = antigo.premios + mapOf(2 to quina, 3 to quadra))
        val r = AuditoriaRegras.auditar(parametros, "megasena", novo, null)!!
        assertTrue(r.faixas.toString(), r.confere)
        // Pago pela regra antiga num concurso novo: não passa (era o aviso "Regras da Mega-Sena mudaram").
        val pelaRegraAntiga = antigo.copy(numero = 3075)
        assertFalse(AuditoriaRegras.auditar(parametros, "megasena", pelaRegraAntiga, null)!!.confere)
    }

    @Test
    fun concursoQueNaoDaParaConferir() {
        val q = caso("quina_7148")
        // A Caixa já publicou os ganhadores, mas ainda não os valores: espera completar.
        assertNull(AuditoriaRegras.auditar(parametros, "quina", comPremio(q, 3, 0.0), null))
        assertNull(AuditoriaRegras.auditar(parametros, "quina", comPremio(q, 3, null), null))
        // Concurso especial (Quina de São João, Mega da Virada...): outras regras.
        val especial = registro(q, "ultimo")!!.copy(indicadorConcursoEspecial = 2)
        assertNull(AuditoriaRegras.auditar(parametros, "quina", especial, null))
        // Sem o rateio (lista vazia) ou sem arrecadação.
        assertNull(AuditoriaRegras.auditar(parametros, "quina", registro(q, "ultimo")!!.copy(ganhadores = emptyMap(), premios = emptyMap()), null))
        assertNull(AuditoriaRegras.auditar(parametros, "quina", registro(q, "ultimo")!!.copy(valorArrecadado = 0.0), null))
        // Modalidade sem parâmetros.
        assertNull(AuditoriaRegras.auditar(parametros, "loteca", registro(q, "ultimo")!!, null))
    }

    @Test
    fun toleranciaDoArredondamento() {
        assertEquals(0.05, AuditoriaRegras.tolerancia(10.0), 0.0)
        assertEquals(50.0, AuditoriaRegras.tolerancia(100_000.0), 1e-9)
        assertTrue(AuditoriaRegras.Faixa(2, "x", 1, 4.648, 4.64).confere)
        assertFalse(AuditoriaRegras.Faixa(2, "x", 1, 4.648, 4.70).confere)
    }
}

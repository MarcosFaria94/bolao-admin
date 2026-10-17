package io.github.marcosfaria94.bolao.net

import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.JogoLoteca
import io.github.marcosfaria94.bolao.domain.Modalidade
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.math.roundToLong

/**
 * Lê o JSON da API (não oficial) do portal de loterias da Caixa.
 * Tudo é lido de forma tolerante: campo ausente ou em outro formato vira null/vazio.
 */
object CaixaParser {

    fun ler(modalidade: Modalidade, texto: String): Concurso {
        val o = Json.parseToJsonElement(texto) as? JsonObject
            ?: throw IllegalArgumentException("Resposta da Caixa em formato inesperado.")
        val numero = o.int("numero") ?: throw IllegalArgumentException("Resposta da Caixa sem número de concurso.")

        val dezenasTexto = when (modalidade.formato) {
            // No Super Sete a ordem importa (coluna 1 a 7).
            Formato.COLUNAS -> o.lista("dezenasSorteadasOrdemSorteio").ifEmpty { o.lista("listaDezenas") }
            else -> o.lista("listaDezenas")
        }
        val federal = modalidade == Modalidade.FEDERAL
        val dezenas = if (federal) emptyList() else dezenasTexto.mapNotNull { it.trim().toIntOrNull() }.let {
            if (modalidade.formato == Formato.COLUNAS) it else it.sorted()
        }
        val bilhetes = if (federal) dezenasTexto.map { it.filter(Char::isDigit).padStart(6, '0') } else emptyList()

        val faixas = (o["listaRateioPremio"] as? JsonArray).orEmpty().mapNotNull { e ->
            val f = e as? JsonObject ?: return@mapNotNull null
            FaixaPremio(
                faixa = f.int("faixa") ?: return@mapNotNull null,
                descricao = f.str("descricaoFaixa").orEmpty().trim(),
                ganhadores = f.int("numeroDeGanhadores") ?: 0,
                valor = centavos(f.dbl("valorPremio")) ?: 0L,
            )
        }.sortedBy { it.faixa }

        val jogos = (o["listaResultadoEquipeEsportiva"] as? JsonArray).orEmpty().mapIndexedNotNull { i, e ->
            val j = e as? JsonObject ?: return@mapIndexedNotNull null
            JogoLoteca(
                ordem = j.int("nuSequencial") ?: (i + 1),
                time1 = j.str("nomeEquipeUm")?.trim().orEmpty(),
                time2 = j.str("nomeEquipeDois")?.trim().orEmpty(),
                gols1 = j.int("nuGolEquipeUm"),
                gols2 = j.int("nuGolEquipeDois"),
            )
        }.sortedBy { it.ordem }

        return Concurso(
            modalidade = modalidade,
            numero = numero,
            data = Datas.lerBr(o.str("dataApuracao")),
            dezenas = dezenas,
            segundoSorteio = o.lista("listaDezenasSegundoSorteio").mapNotNull { it.trim().toIntOrNull() }.sorted(),
            trevos = o.lista("trevosSorteados").mapNotNull { it.trim().toIntOrNull() }.sorted(),
            timeOuMes = o.str("nomeTimeCoracaoMesSorte")?.replace("\u0000", "")?.trim()?.takeIf { it.isNotEmpty() },
            faixas = faixas,
            acumulado = o.bool("acumulado") ?: false,
            jogosLoteca = jogos,
            bilhetesFederal = bilhetes,
            proximoNumero = o.int("numeroConcursoProximo"),
            proximaData = Datas.lerBr(o.str("dataProximoConcurso")),
            premioEstimadoProximo = centavos(o.dbl("valorEstimadoProximoConcurso"))?.takeIf { it > 0 },
            numeroConcursoEspecial = o.int("numeroConcursoFinal_0_5")?.takeIf { it > 0 },
            acumuladoEspecial = centavos(o.dbl("valorAcumuladoConcursoEspecial"))?.takeIf { it > 0 },
        )
    }

    private fun centavos(reais: Double?): Long? = reais?.let { (it * 100).roundToLong() }

    private fun JsonObject.prim(k: String): JsonPrimitive? = this[k] as? JsonPrimitive
    private fun JsonObject.str(k: String): String? = prim(k)?.contentOrNull
    private fun JsonObject.int(k: String): Int? = prim(k)?.let { it.intOrNull ?: it.contentOrNull?.trim()?.toIntOrNull() }
    private fun JsonObject.dbl(k: String): Double? = prim(k)?.let { it.doubleOrNull ?: it.contentOrNull?.replace(",", ".")?.toDoubleOrNull() }
    private fun JsonObject.bool(k: String): Boolean? = prim(k)?.booleanOrNull
    private fun JsonObject.lista(k: String): List<String> =
        (this[k] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

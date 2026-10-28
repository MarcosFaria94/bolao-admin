package io.github.marcosfaria94.bolao.domain.indices

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Parâmetros dos índices k1 e k2 e das odds (arquivo parametros_k.json, embutido no app).
 * Todas as regras de cada modalidade vêm daqui; o código do cálculo não repete nenhum número.
 */
data class FaixaK(
    val faixa: Int,
    val nome: String,
    /** Probabilidade de uma aposta simples cair nesta faixa. */
    val p: Double,
    val umEm: Double,
    val grupo: String,
    val principal: Boolean,
    /** "rateio" ou "fixo". */
    val tipo: String,
    val fracao: Double = 0.0,
    /** Lotofácil: fração usada quando o número do concurso termina em 0. */
    val fracaoFinalZero: Double? = null,
    /** Dispersão da popularidade (binomial negativa); null = Poisson. */
    val r: Double? = null,
    val herdaMesmaFaixa: Boolean = false,
    val cascata: Boolean = false,
    /** Prêmio fixo, em reais. */
    val valor: Double = 0.0,
    /**
     * Frações que valeram antes da regra atual: (até o concurso, fração). A Caixa muda a divisão
     * entre as faixas de vez em quando (Mega-Sena: quina e quadra de 13% e 15% para 10% e 10% a
     * partir do concurso 3073); assim os concursos antigos continuam conferindo com a regra deles.
     */
    val fracoesAnteriores: List<Pair<Int, Double>> = emptyList(),
) {
    val fixo: Boolean get() = tipo == "fixo"
    val rateio: Boolean get() = tipo == "rateio"

    /** Fração da faixa no concurso [numero]: a regra que valia nele (e a dos concursos de final 0). */
    fun fracaoNo(numero: Int): Double {
        fracoesAnteriores.sortedBy { it.first }.firstOrNull { numero <= it.first }?.let { return it.second }
        if (fracaoFinalZero != null && numero % 10 == 0) return fracaoFinalZero
        return fracao
    }
}

data class VendasK(
    val intercepto: Double,
    val efeitoDiaSemana: Map<String, Double>,
    val efeitoDiaAusente: Double,
    val bLogEstimativa: Double,
    val bLogNAnterior: Double,
    val bReserva: Double,
    val sigmaLog: Double,
)

data class ModalidadeK(
    /** Chave da API da Caixa (megasena, lotofacil...). */
    val chave: String,
    val nome: String,
    val preco: Double,
    val fracPremio: Double,
    val fracRgp: Double,
    val ordemCascata: List<Int>,
    val faixas: List<FaixaK>,
    val vendas: VendasK,
)

data class ParametrosK(
    val versao: String,
    val irAliquota: Double,
    val limiteIsencao: Double,
    val k2PMinimo: Double,
    val casasExibicao: Int,
    val ghNos: List<Double>,
    val ghPesos: List<Double>,
    val paramsUrl: String,
    val campoBase: String,
    val basesReserva: List<String>,
    val modalidades: Map<String, ModalidadeK>,
) {
    companion object {
        fun ler(texto: String): ParametrosK {
            val o = Json.parseToJsonElement(texto).objeto()
            val gh = o.obj("gauss_hermite_9")
            val api = o.obj("api")
            val modalidades = o.obj("modalidades").mapValues { (chave, v) -> lerModalidade(chave, v.objeto()) }
            return ParametrosK(
                versao = o.texto("versao").orEmpty(),
                irAliquota = o.num("ir_aliquota"),
                limiteIsencao = o.num("limite_isencao"),
                k2PMinimo = o.num("k2_p_minimo"),
                casasExibicao = o.inteiro("casas_exibicao") ?: 3,
                ghNos = gh.lista("nos").map { it.numero() },
                ghPesos = gh.lista("pesos").map { it.numero() },
                paramsUrl = api.texto("params_url").orEmpty(),
                campoBase = api.texto("campo_base") ?: "urlapiloterias",
                basesReserva = api.lista("bases_reserva").mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
                modalidades = modalidades,
            )
        }

        private fun lerModalidade(chave: String, m: JsonObject): ModalidadeK {
            val v = m.obj("vendas")
            return ModalidadeK(
                chave = chave,
                nome = m.texto("nome").orEmpty(),
                preco = m.num("preco"),
                fracPremio = m.num("frac_premio"),
                fracRgp = m.numOuNull("frac_rgp") ?: 0.0,
                ordemCascata = m.lista("ordem_cascata").map { it.numero().toInt() },
                faixas = m.lista("faixas").map { e ->
                    val f = e.objeto()
                    FaixaK(
                        faixa = f.inteiro("faixa") ?: 0,
                        nome = f.texto("nome").orEmpty(),
                        p = f.num("p"),
                        umEm = f.numOuNull("um_em") ?: 0.0,
                        grupo = f.texto("grupo").orEmpty(),
                        principal = f.booleano("principal") ?: false,
                        tipo = f.texto("tipo").orEmpty(),
                        fracao = f.numOuNull("fracao") ?: 0.0,
                        fracaoFinalZero = f.numOuNull("fracao_final_zero"),
                        r = f.numOuNull("r"),
                        herdaMesmaFaixa = f.booleano("herda_mesma_faixa") ?: false,
                        cascata = f.booleano("cascata") ?: false,
                        valor = f.numOuNull("valor") ?: 0.0,
                        fracoesAnteriores = f.lista("fracoes_anteriores").mapNotNull { e ->
                            val h = e as? JsonObject ?: return@mapNotNull null
                            val ate = h.inteiro("ate_concurso") ?: return@mapNotNull null
                            val fr = h.numOuNull("fracao") ?: return@mapNotNull null
                            ate to fr
                        },
                    )
                },
                vendas = VendasK(
                    intercepto = v.num("intercepto"),
                    efeitoDiaSemana = v.obj("efeito_dia_semana").mapValues { it.value.numero() },
                    efeitoDiaAusente = v.num("efeito_dia_ausente"),
                    bLogEstimativa = v.num("b_log_estimativa"),
                    bLogNAnterior = v.num("b_log_N_anterior"),
                    bReserva = v.num("b_reserva"),
                    sigmaLog = v.num("sigma_log"),
                ),
            )
        }
    }
}

// ---------- leitura tolerante do JSON ----------

internal fun JsonElement.objeto(): JsonObject = this as? JsonObject ?: throw IllegalArgumentException("Objeto JSON esperado.")
internal fun JsonElement.numero(): Double =
    (this as? JsonPrimitive)?.doubleOrNull ?: throw IllegalArgumentException("Número JSON esperado.")

internal fun JsonObject.obj(k: String): JsonObject = this[k]?.objeto() ?: throw IllegalArgumentException("Falta \"$k\".")
internal fun JsonObject.lista(k: String): List<JsonElement> = (this[k] as? JsonArray).orEmpty()
internal fun JsonObject.prim(k: String): JsonPrimitive? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }
internal fun JsonObject.texto(k: String): String? = prim(k)?.contentOrNull
internal fun JsonObject.numOuNull(k: String): Double? =
    prim(k)?.let { it.doubleOrNull ?: it.contentOrNull?.trim()?.replace(",", ".")?.toDoubleOrNull() }
internal fun JsonObject.num(k: String): Double = numOuNull(k) ?: throw IllegalArgumentException("Falta o número \"$k\".")
internal fun JsonObject.inteiro(k: String): Int? =
    prim(k)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() ?: it.contentOrNull?.trim()?.toIntOrNull() }
internal fun JsonObject.booleano(k: String): Boolean? = prim(k)?.booleanOrNull

package io.github.marcosfaria94.bolao.data

import android.content.Context
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.RegistroK
import io.github.marcosfaria94.bolao.net.CaixaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

/**
 * O histórico de todos os concursos já sorteados (desde a 0.33): o app traz embutidos os resultados de cada
 * modalidade até a data da versão (assets/historico/<modalidade>.jsonl.gzip: compactado em gzip, mas sem a terminação
 * ".gz", que o Android trata de um jeito especial nos assets; uma linha por concurso, só com os campos
 * que o app lê, no formato da API da Caixa), e cada concurso completo que chega da Caixa depois (o último dos Jogos, o
 * anterior do k1, um consultado nos Detalhes) é acrescentado no celular (filesDir/historico/<modalidade>.jsonl). Assim
 * os Detalhes mostram qualquer concurso sem ir à Caixa (e sem esperar o tempo esgotado dela) e o k1 dos concursos
 * especiais sai dos especiais já sorteados ([especiais]).
 *
 * As linhas de uma modalidade só são lidas na primeira vez que alguém pede um concurso dela (e ficam na memória
 * enquanto o app está aberto); os acréscimos valem por cima das embutidas.
 */
class Historico(private val context: Context) {

    private val pasta: File get() = File(context.filesDir, "historico").apply { mkdirs() }
    private fun acrescimos(m: Modalidade) = File(pasta, "${m.slugApi}.jsonl")

    /** As linhas já lidas de cada modalidade (número do concurso → linha). */
    private val linhas = ConcurrentHashMap<Modalidade, ConcurrentHashMap<Int, String>>()
    private val travas = ConcurrentHashMap<Modalidade, Mutex>()

    /** Os acréscimos de cada modalidade (para não gravar duas vezes o mesmo concurso). */
    private val acrescidos = ConcurrentHashMap<Modalidade, MutableMap<Int, String>>()

    /** Os concursos especiais já sorteados de cada modalidade (lidos uma vez). */
    private val especiaisLidos = ConcurrentHashMap<Modalidade, List<RegistroK>>()

    private val _novosEspeciais = MutableStateFlow(0)

    /** Muda quando um concurso especial novo entra no histórico (o k1 dos especiais é refeito). */
    val novosEspeciais: StateFlow<Int> = _novosEspeciais.asStateFlow()

    /** O resumo do que vem embutido: o último concurso de cada modalidade e os que faltam (a Caixa não tinha). */
    private val embutido: Map<String, Pair<Int, Set<Int>>> by lazy {
        runCatching {
            val o = context.assets.open(INDICE).use { Json.parseToJsonElement(it.readBytes().decodeToString()) } as JsonObject
            o.mapValues { (_, v) ->
                val x = v as JsonObject
                val ultimo = (x["ultimo"] as? JsonPrimitive)?.intOrNull ?: 0
                val faltam = (x["faltam"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.intOrNull }.toSet()
                ultimo to faltam
            }
        }.getOrDefault(emptyMap())
    }

    /** O último concurso embutido no app da modalidade [m] (0 = nenhum). */
    fun ultimoEmbutido(m: Modalidade): Int = embutido[m.slugApi]?.first ?: 0

    /** O concurso [numero] vem embutido no app. */
    fun embutido(m: Modalidade, numero: Int): Boolean =
        embutido[m.slugApi]?.let { (ultimo, faltam) -> numero in 1..ultimo && numero !in faltam } ?: false

    /** O concurso [numero] da modalidade [m], do histórico (null = não está nele: perguntar à Caixa). */
    suspend fun concurso(m: Modalidade, numero: Int): Concurso? = withContext(Dispatchers.IO) {
        val linha = carregar(m)[numero] ?: return@withContext null
        runCatching { CaixaParser.ler(m, linha) }.getOrNull()?.takeIf { it.numero == numero }
    }

    /** Quantos concursos da modalidade [m] o histórico tem. */
    suspend fun quantos(m: Modalidade): Int = withContext(Dispatchers.IO) { carregar(m).size }

    private suspend fun carregar(m: Modalidade): Map<Int, String> {
        linhas[m]?.let { return it }
        return travas.getOrPut(m) { Mutex() }.withLock {
            linhas[m] ?: ConcurrentHashMap<Int, String>().also { mapa ->
                percorrer(m) { n, l -> mapa[n] = l }
                linhas[m] = mapa
            }
        }
    }

    /** Percorre as linhas da modalidade [m]: as embutidas e, depois, os acréscimos (a última de cada concurso vale). */
    private fun percorrer(m: Modalidade, f: (Int, String) -> Unit) {
        runCatching {
            context.assets.open("historico/${m.slugApi}.jsonl.gzip").use { e ->
                GZIPInputStream(e).bufferedReader(Charsets.UTF_8).forEachLine { l -> numeroDa(l)?.let { f(it, l) } }
            }
        }
        lerAcrescimos(m).forEach { (n, l) -> f(n, l) }
    }

    private fun lerAcrescimos(m: Modalidade): MutableMap<Int, String> = acrescidos.getOrPut(m) {
        val mapa = LinkedHashMap<Int, String>()
        runCatching {
            acrescimos(m).takeIf { it.isFile }?.bufferedReader(Charsets.UTF_8)?.use { r ->
                r.forEachLine { l -> numeroDa(l)?.let { mapa[it] = l } }
            }
        }
        java.util.Collections.synchronizedMap(mapa)
    }

    /**
     * Acrescenta ao histórico do celular o concurso [c] que a Caixa mandou ([texto] = a resposta dela), se ele está
     * completo (com o rateio) e ainda não está no histórico do jeito que veio.
     */
    fun acrescentar(m: Modalidade, c: Concurso, texto: String) {
        if (!completo(c)) return
        val linha = runCatching { reduzir(texto) }.getOrNull() ?: return
        if (numeroDa(linha) != c.numero) return
        synchronized(this) {
            val ja = lerAcrescimos(m)
            if (ja[c.numero] == linha) return
            if (ja[c.numero] == null && embutido(m, c.numero)) return
            runCatching { acrescimos(m).appendText(linha + "\n", Charsets.UTF_8) }.onFailure { return }
            ja[c.numero] = linha
            linhas[m]?.put(c.numero, linha)
            if (linha.contains(MARCA_DO_ESPECIAL)) {
                especiaisLidos.remove(m)
                _novosEspeciais.value += 1
            }
        }
    }

    /** Os concursos especiais já sorteados da modalidade [m] (Mega da Virada, Quina de São João...), do mais novo ao mais velho. */
    suspend fun especiais(m: Modalidade): List<RegistroK> = withContext(Dispatchers.IO) {
        especiaisLidos[m]?.let { return@withContext it }
        val mapa = HashMap<Int, RegistroK>()
        val ja = linhas[m]
        val visitar: (Int, String) -> Unit = { n, l ->
            if (l.contains(MARCA_DO_ESPECIAL)) {
                runCatching { RegistroK.deJson(Json.parseToJsonElement(l) as JsonObject) }.getOrNull()?.let { mapa[n] = it }
            } else {
                mapa.remove(n)
            }
        }
        if (ja != null) ja.forEach { (n, l) -> visitar(n, l) } else percorrer(m, visitar)
        mapa.values.sortedByDescending { it.numero }.also { especiaisLidos[m] = it }
    }

    companion object {
        /** O resumo do histórico embutido ({"megasena": {"ultimo": 2990, "faltam": [...]}, ...}). */
        const val INDICE = "historico/indice.json"

        /** Como a linha de um concurso especial começa a se reconhecer sem ler o JSON todo. */
        const val MARCA_DO_ESPECIAL = "\"indicadorConcursoEspecial\":2"

        /** Os campos guardados de cada concurso (os que o [CaixaParser] e o [RegistroK] leem), nesta ordem. */
        val CAMPOS = listOf(
            "numero", "dataApuracao", "listaDezenas", "dezenasSorteadasOrdemSorteio", "listaDezenasSegundoSorteio",
            "trevosSorteados", "nomeTimeCoracaoMesSorte", "acumulado", "numeroConcursoProximo", "dataProximoConcurso",
            "valorEstimadoProximoConcurso", "numeroConcursoFinal_0_5", "valorAcumuladoConcursoEspecial",
            "valorAcumuladoProximoConcurso", "valorAcumuladoConcurso_0_5", "valorArrecadado", "indicadorConcursoEspecial",
        )
        private val CAMPOS_DA_FAIXA = listOf("faixa", "descricaoFaixa", "numeroDeGanhadores", "valorPremio")
        private val CAMPOS_DO_JOGO = listOf("nuSequencial", "nomeEquipeUm", "nomeEquipeDois", "nuGolEquipeUm", "nuGolEquipeDois")

        private val NUMERO = Regex("\"numero\":(\\d+)")

        /** O número do concurso de uma linha do histórico. */
        fun numeroDa(linha: String): Int? = NUMERO.find(linha)?.groupValues?.get(1)?.toIntOrNull()

        /** O concurso tem o resultado e o rateio completos (o que entra no histórico). */
        fun completo(c: Concurso): Boolean =
            c.temResultado && c.faixas.isNotEmpty() && c.faixas.none { it.ganhadores > 0 && it.valor <= 0 }

        /** Campo sem valor (nulo, texto vazio ou lista vazia): não vai para o histórico. */
        private fun vazio(e: JsonElement?): Boolean = when (e) {
            null, JsonNull -> true
            is JsonPrimitive -> e.isString && e.content.isEmpty()
            is JsonArray -> e.isEmpty()
            else -> false
        }

        /** A resposta da Caixa [texto] só com os campos guardados, numa linha (o mesmo formato do histórico embutido). */
        fun reduzir(texto: String): String {
            val o = Json.parseToJsonElement(texto) as JsonObject
            val r = buildJsonObject {
                for (k in CAMPOS) o[k]?.takeIf { !vazio(it) }?.let { put(k, it) }
                put("listaRateioPremio", buildJsonArray {
                    (o["listaRateioPremio"] as? JsonArray).orEmpty().forEach { f ->
                        val x = f as? JsonObject ?: return@forEach
                        add(buildJsonObject { CAMPOS_DA_FAIXA.forEach { k -> x[k]?.let { put(k, it) } } })
                    }
                })
                (o["listaResultadoEquipeEsportiva"] as? JsonArray)?.takeIf { it.isNotEmpty() }?.let { jogos ->
                    put("listaResultadoEquipeEsportiva", buildJsonArray {
                        jogos.forEach { j ->
                            val x = j as? JsonObject ?: return@forEach
                            add(buildJsonObject { CAMPOS_DO_JOGO.forEach { k -> x[k]?.let { put(k, it) } } })
                        }
                    })
                }
            }
            return r.toString()
        }
    }
}

package io.github.marcosfaria94.bolao.net

import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLException

/**
 * API não oficial do portal de loterias da Caixa. Pode mudar ou falhar:
 * toda tela que usa estes dados também aceita digitação manual.
 *
 * O endereço do servidor é lido do arquivo de configuração do próprio site da Caixa
 * (params.txt); se ele falhar, vão os endereços de reserva, na ordem.
 */
object CaixaApi {
    private const val VALIDADE_ULTIMO_MS = 10 * 60 * 1000L
    private const val VALIDADE_BASE_MS = 6 * 60 * 60 * 1000L

    @Volatile
    private var paramsUrl = "https://loterias.caixa.gov.br/Style%20Library/json/params.txt"

    @Volatile
    private var reservas = listOf(
        "https://servicebus3.caixa.gov.br/portaldeloterias",
        "https://servicebus2.caixa.gov.br/portaldeloterias",
    )

    @Volatile
    private var baseDescoberta: Pair<Long, String>? = null
    private val travaBase = Mutex()

    private val cacheUltimo = ConcurrentHashMap<Modalidade, Pair<Long, Concurso>>()
    private val cacheConcurso = ConcurrentHashMap<String, Concurso>()

    /** Endereços vindos do parametros_k.json (seção "api"). */
    fun configurar(params: String, bases: List<String>) {
        if (params.isNotBlank()) paramsUrl = params
        if (bases.isNotEmpty()) reservas = bases
    }

    /** Último concurso sorteado (traz também o número, a data e o prêmio do próximo). */
    suspend fun ultimo(m: Modalidade, usarCache: Boolean = true): Concurso {
        val guardado = cacheUltimo[m]
        if (usarCache && guardado != null && System.currentTimeMillis() - guardado.first < VALIDADE_ULTIMO_MS) {
            return guardado.second
        }
        val c = buscar(m, "/api/${m.slugApi}").first
        cacheUltimo[m] = System.currentTimeMillis() to c
        return c
    }

    /** O mesmo, com o texto original da resposta (para guardar no celular). */
    suspend fun ultimoComTexto(m: Modalidade): Pair<Concurso, String> {
        val r = buscar(m, "/api/${m.slugApi}")
        cacheUltimo[m] = System.currentTimeMillis() to r.first
        return r
    }

    /** Um concurso específico. Resultados já sorteados ficam guardados durante o uso do app. */
    suspend fun concurso(m: Modalidade, numero: Int): Concurso =
        cacheConcurso["${m.name}-$numero"] ?: concursoComTexto(m, numero).first

    suspend fun concursoComTexto(m: Modalidade, numero: Int): Pair<Concurso, String> {
        val r = buscar(m, "/api/${m.slugApi}/$numero")
        val c = r.first
        // Nunca apurar com o resultado de outro concurso.
        if (c.numero != numero) throw IOException("A Caixa devolveu o concurso ${c.numero} em vez do $numero.")
        // Só guarda o resultado completo (com o rateio oficial): antes disso, a conferência pergunta de novo.
        val completo = c.temResultado && c.faixas.isNotEmpty() && c.faixas.none { it.ganhadores > 0 && it.valor <= 0 }
        if (completo) cacheConcurso["${m.name}-$numero"] = c
        return r
    }

    /** O último já guardado na memória, sem ir à internet (para preencher telas rapidamente). */
    fun ultimoGuardado(m: Modalidade): Concurso? = cacheUltimo[m]?.second

    fun guardado(m: Modalidade, numero: Int): Concurso? = cacheConcurso["${m.name}-$numero"]

    private fun pedido(url: String): Request = Request.Builder()
        .url(url)
        .header("Accept", "application/json, text/plain, */*")
        .header("Origin", "https://loterias.caixa.gov.br")
        .header("Referer", "https://loterias.caixa.gov.br/")
        .header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36",
        )
        .build()

    /** Servidor da API indicado pelo site da Caixa (guardado por algumas horas). */
    private suspend fun baseAtual(): String? = travaBase.withLock {
        baseDescoberta?.takeIf { System.currentTimeMillis() - it.first < VALIDADE_BASE_MS }?.let { return@withLock it.second }
        val achada = withContext(Dispatchers.IO) {
            runCatching {
                Http.cliente.newCall(pedido(paramsUrl)).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    val texto = resp.body?.string().orEmpty()
                    Regex("\"urlapiloterias\"\\s*:\\s*\"([^\"]+)\"").find(texto)?.groupValues?.get(1)?.trimEnd('/')
                }
            }.getOrNull()
        }
        if (achada != null) baseDescoberta = System.currentTimeMillis() to achada
        achada
    }

    private suspend fun buscar(m: Modalidade, caminho: String): Pair<Concurso, String> {
        val bases = (listOfNotNull(baseAtual()) + reservas).distinct()
        var erro: IOException? = null
        for (base in bases) {
            try {
                return buscarEm(m, base + caminho)
            } catch (e: IOException) {
                erro = e
            }
        }
        throw erro ?: IOException("Não foi possível consultar a Caixa.")
    }

    private suspend fun buscarEm(m: Modalidade, url: String): Pair<Concurso, String> = withContext(Dispatchers.IO) {
        try {
            Http.cliente.newCall(pedido(url)).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("A Caixa respondeu com erro ${resp.code}.")
                val corpo = resp.body?.string().orEmpty()
                if (corpo.isBlank()) throw IOException("A Caixa respondeu sem dados.")
                CaixaParser.ler(m, corpo) to corpo
            }
        } catch (e: UnknownHostException) {
            throw IOException("Sem internet no momento.")
        } catch (e: SocketTimeoutException) {
            throw IOException("A Caixa demorou para responder.")
        } catch (e: SSLException) {
            throw IOException("Falha na conexão segura com a Caixa.")
        } catch (e: IllegalArgumentException) {
            throw IOException(e.message ?: "Resposta da Caixa em formato inesperado.")
        }
    }
}

package io.github.marcosfaria94.bolao.net

import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import kotlinx.coroutines.Dispatchers
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
 */
object CaixaApi {
    private const val BASE = "https://servicebus2.caixa.gov.br/portaldeloterias/api/"
    private const val VALIDADE_ULTIMO_MS = 10 * 60 * 1000L

    private val cacheUltimo = ConcurrentHashMap<Modalidade, Pair<Long, Concurso>>()
    private val cacheConcurso = ConcurrentHashMap<String, Concurso>()

    /** Último concurso sorteado (traz também o número, a data e o prêmio do próximo). */
    suspend fun ultimo(m: Modalidade, usarCache: Boolean = true): Concurso {
        val guardado = cacheUltimo[m]
        if (usarCache && guardado != null && System.currentTimeMillis() - guardado.first < VALIDADE_ULTIMO_MS) {
            return guardado.second
        }
        val c = buscar(m, BASE + m.slugApi)
        cacheUltimo[m] = System.currentTimeMillis() to c
        return c
    }

    /** Um concurso específico. Resultados já sorteados ficam guardados durante o uso do app. */
    suspend fun concurso(m: Modalidade, numero: Int): Concurso {
        val chave = "${m.name}-$numero"
        cacheConcurso[chave]?.let { return it }
        val c = buscar(m, BASE + m.slugApi + "/" + numero)
        // Nunca apurar com o resultado de outro concurso.
        if (c.numero != numero) throw IOException("A Caixa devolveu o concurso ${c.numero} em vez do $numero.")
        if (c.temResultado) cacheConcurso[chave] = c
        return c
    }

    /** O último já guardado na memória, sem ir à internet (para preencher telas rapidamente). */
    fun ultimoGuardado(m: Modalidade): Concurso? = cacheUltimo[m]?.second

    private suspend fun buscar(m: Modalidade, url: String): Concurso = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json, text/plain, */*")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36",
            )
            .build()
        try {
            Http.cliente.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("A Caixa respondeu com erro ${resp.code}.")
                val corpo = resp.body?.string().orEmpty()
                if (corpo.isBlank()) throw IOException("A Caixa respondeu sem dados.")
                CaixaParser.ler(m, corpo)
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

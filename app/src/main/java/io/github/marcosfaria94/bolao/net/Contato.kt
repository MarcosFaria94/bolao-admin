package io.github.marcosfaria94.bolao.net

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.marcosfaria94.bolao.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * O Contato dos Ajustes (desde a 0.32): a mensagem vai direto para o e-mail de quem faz o app ([EMAIL]), sem abrir
 * o app de e-mail, pelo FormSubmit (formsubmit.co, um serviço gratuito que entrega no e-mail o que recebe; o primeiro
 * envio pede, uma vez, a confirmação no e-mail, e as mensagens de antes dela ficam guardadas e chegam depois).
 * Uma mensagem a cada [INTERVALO_MS] (5 minutos), contada no celular. Não dando certo, a tela oferece o app de
 * e-mail do celular ([intentDoEmail]).
 */
object Contato {
    /** O e-mail que recebe as mensagens (autorizado pelo usuário, como a chave Pix do Apoie). */
    const val EMAIL = "pix.plz@pm.me"

    /** O endereço do FormSubmit que recebe o envio (a versão que responde em JSON). */
    const val ENDERECO = "https://formsubmit.co/ajax/$EMAIL"

    /** Uma mensagem a cada 5 minutos (pedido do usuário). */
    const val INTERVALO_MS = 5 * 60 * 1000L

    /** O maior tamanho da mensagem. */
    const val TAMANHO_MAXIMO = 2000

    private const val PREFS = "contato"
    private const val ULTIMO = "ultimo_envio"

    /** O motivo da mensagem (vai no assunto do e-mail). */
    enum class Motivo(val rotulo: String) {
        MENSAGEM("Mensagem"),
        DUVIDA("Dúvida"),
        SUGESTAO("Sugestão"),
        BUG("Reportar bug"),
    }

    /** O que aconteceu com o envio. */
    sealed interface Resultado {
        /** Entregue. */
        data object Enviada : Resultado

        /** Recebida, esperando a confirmação do e-mail (primeiro envio): chega depois dela. */
        data object Guardada : Resultado

        /** Não foi: o porquê. */
        data class Erro(val mensagem: String) : Resultado
    }

    /** Quando a última mensagem foi mandada (ms; 0 = nunca). */
    fun ultimoEnvio(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(ULTIMO, 0L)

    /** Guarda a hora do envio (conta para os 5 minutos). */
    fun marcarEnvio(context: Context, agora: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(ULTIMO, agora).apply()
    }

    /**
     * Quanto falta (ms) para poder mandar outra mensagem: 0 = já pode. Com o relógio do celular voltado para trás
     * (a última mensagem "no futuro"), no máximo os 5 minutos.
     */
    fun espera(ultimo: Long, agora: Long): Long {
        if (ultimo <= 0L) return 0L
        return (ultimo + INTERVALO_MS - agora).coerceIn(0L, INTERVALO_MS)
    }

    /** "4 min" / "1 min" / "menos de 1 min": quanto falta, para a tela. */
    fun textoDaEspera(ms: Long): String {
        val minutos = (ms + 59_999L) / 60_000L
        return if (ms < 60_000L) "menos de 1 min" else "$minutos min"
    }

    /** O assunto do e-mail: "Bolão dos Amigos · Sugestão". */
    fun assunto(motivo: Motivo): String = "Bolão dos Amigos · ${motivo.rotulo}"

    /** A versão do app e o celular (vai junto quando se reporta um bug, se a pessoa deixar). */
    fun aparelho(): String =
        "App ${BuildConfig.VERSION_NAME} · Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

    /** O texto do e-mail (para o app de e-mail): o motivo, a mensagem, o e-mail para a resposta e o aparelho. */
    fun corpo(motivo: Motivo, mensagem: String, resposta: String, aparelho: String?): String = buildString {
        append("Motivo: ").append(motivo.rotulo).append("\n\n")
        append(mensagem.trim()).append('\n')
        if (resposta.isNotBlank()) append("\nResponder para: ").append(resposta.trim()).append('\n')
        if (aparelho != null) append('\n').append(aparelho).append('\n')
    }

    /** O e-mail parece um e-mail (o campo da resposta é opcional: vazio também vale). */
    fun emailValido(texto: String): Boolean {
        val t = texto.trim()
        return t.isEmpty() || Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(t)
    }

    /** O que vai para o FormSubmit: os campos aparecem numa tabela no e-mail. */
    fun json(motivo: Motivo, mensagem: String, resposta: String, aparelho: String?): String = buildJsonObject {
        put("_subject", assunto(motivo))
        put("_template", "table")
        put("_captcha", "false")
        put("Motivo", motivo.rotulo)
        put("Mensagem", mensagem.trim())
        if (resposta.isNotBlank()) {
            put("email", resposta.trim())
            put("_replyto", resposta.trim())
        }
        if (aparelho != null) put("Aparelho", aparelho)
    }.toString()

    /** Lê a resposta do FormSubmit ([codigo] HTTP e o [texto] em JSON). */
    fun interpretar(codigo: Int, texto: String): Resultado {
        val o = runCatching { Json.parseToJsonElement(texto).jsonObject }.getOrNull()
        val sucesso = (o?.get("success") as? JsonPrimitive)?.let { it.booleanOrNull ?: (it.contentOrNull == "true") } == true
        val mensagem = (o?.get("message") as? JsonPrimitive)?.contentOrNull.orEmpty()
        return when {
            codigo in 200..299 && sucesso -> Resultado.Enviada
            // O primeiro envio: o FormSubmit manda o pedido de confirmação para o e-mail e guarda a mensagem.
            mensagem.contains("activat", ignoreCase = true) -> Resultado.Guardada
            mensagem.isNotBlank() -> Resultado.Erro("A mensagem não foi enviada (o serviço respondeu: $mensagem).")
            else -> Resultado.Erro("A mensagem não foi enviada (resposta $codigo do serviço).")
        }
    }

    /** Manda a mensagem. Sem internet ou com o serviço fora do ar: [Resultado.Erro]. */
    suspend fun enviar(motivo: Motivo, mensagem: String, resposta: String, aparelho: String?): Resultado = withContext(Dispatchers.IO) {
        val corpo = json(motivo, mensagem, resposta, aparelho).toRequestBody("application/json; charset=utf-8".toMediaType())
        val pedido = Request.Builder()
            .url(ENDERECO)
            .post(corpo)
            .header("Accept", "application/json")
            // O FormSubmit pede a página de onde a mensagem vem: a do app no GitHub.
            .header("Referer", "https://github.com/${BuildConfig.REPO_GITHUB}")
            .build()
        try {
            Http.cliente.newCall(pedido).execute().use { r -> interpretar(r.code, r.body?.string().orEmpty()) }
        } catch (e: IOException) {
            Resultado.Erro("A mensagem não foi enviada: sem conexão com a internet ou o serviço não respondeu.")
        }
    }

    /** O app de e-mail do celular, com o destinatário, o assunto e o texto prontos (o jeito de reserva). */
    fun intentDoEmail(motivo: Motivo, mensagem: String, resposta: String, aparelho: String?): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, assunto(motivo))
            putExtra(Intent.EXTRA_TEXT, corpo(motivo, mensagem, resposta, aparelho))
        }
}

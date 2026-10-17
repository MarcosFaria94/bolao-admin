package io.github.marcosfaria94.bolao.net

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import io.github.marcosfaria94.bolao.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Atualização com um toque: confere o último Release do GitHub, baixa o APK
 * e abre o instalador do Android.
 */
object Atualizador {

    data class Versao(
        val codigo: Int,
        val nome: String,
        val notas: String,
        val urlApk: String,
        val tamanho: Long,
    )

    val versaoInstalada: Int get() = BuildConfig.VERSION_CODE
    val nomeInstalado: String get() = BuildConfig.VERSION_NAME

    /** Devolve a versão publicada mais recente (pode ser igual à instalada). */
    suspend fun ultimaPublicada(): Versao = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.REPO_GITHUB}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "bolao-admin/${BuildConfig.VERSION_NAME}")
            .build()
        Http.cliente.newCall(req).execute().use { resp ->
            if (resp.code == 404) throw IOException("Ainda não há versão publicada.")
            if (!resp.isSuccessful) throw IOException("O GitHub respondeu ${resp.code}.")
            val corpo = Json.parseToJsonElement(resp.body!!.string()).jsonObject
            val tag = corpo["tag_name"]?.jsonPrimitive?.contentOrNull ?: throw IOException("Versão sem nome.")
            // Tag "v0.1.23": o último número é o versionCode (contagem de commits).
            val codigo = tag.substringAfterLast('.').toIntOrNull() ?: throw IOException("Versão $tag em formato inesperado.")
            val apk = corpo["assets"]?.jsonArray
                ?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }
                ?: throw IOException("A versão $tag não tem APK.")
            Versao(
                codigo = codigo,
                nome = tag.removePrefix("v"),
                notas = corpo["body"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
                urlApk = apk["browser_download_url"]!!.jsonPrimitive.content,
                tamanho = apk["size"]?.jsonPrimitive?.longOrNull ?: -1L,
            )
        }
    }

    /** null se já estiver na última versão. */
    suspend fun verificar(): Versao? = ultimaPublicada().takeIf { it.codigo > versaoInstalada }

    private fun arquivoApk(context: Context) = File(File(context.cacheDir, "atualizacao").apply { mkdirs() }, "bolao-admin.apk")

    /** Baixa o APK informando o progresso de 0 a 1. */
    suspend fun baixar(context: Context, v: Versao, progresso: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val destino = arquivoApk(context)
        val parcial = File(destino.path + ".parcial")
        val req = Request.Builder()
            .url(v.urlApk)
            .header("User-Agent", "bolao-admin/${BuildConfig.VERSION_NAME}")
            .build()
        Http.cliente.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Falha ao baixar (${resp.code}).")
            val corpo = resp.body ?: throw IOException("Download vazio.")
            val total = if (v.tamanho > 0) v.tamanho else corpo.contentLength()
            corpo.byteStream().use { entrada ->
                parcial.outputStream().use { saida ->
                    val buf = ByteArray(64 * 1024)
                    var lidos = 0L
                    while (true) {
                        val n = entrada.read(buf)
                        if (n < 0) break
                        saida.write(buf, 0, n)
                        lidos += n
                        if (total > 0) progresso((lidos.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            if (v.tamanho > 0 && parcial.length() != v.tamanho) {
                parcial.delete()
                throw IOException("Download incompleto. Tente de novo.")
            }
        }
        destino.delete()
        if (!parcial.renameTo(destino)) throw IOException("Não foi possível preparar o arquivo.")
        progresso(1f)
        destino
    }

    /** O Android exige liberar "Instalar apps desconhecidos" para este app uma vez. */
    fun podeInstalar(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun abrirPermissaoInstalar(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Abre o instalador do sistema (no HyperOS o caminho pelo instalador padrão é o mais estável). */
    fun instalar(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.arquivos", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

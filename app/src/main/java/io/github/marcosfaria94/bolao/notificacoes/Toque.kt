package io.github.marcosfaria94.bolao.notificacoes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * O toque do prêmio principal (Ajustes > Toque do prêmio): quando uma aposta acerta o prêmio
 * principal, o celular toca por [DURACAO_MS] (o tempo de uma chamada), vibrando junto, como uma
 * ligação. Toca no volume do alarme, para tocar mesmo com o celular no silencioso. Para pelo aviso
 * do celular ("Parar o toque"), ao abrir o app ou sozinho, no fim do tempo.
 *
 * A música é escolhida em Ajustes: um arquivo de áudio do celular, um dos toques do sistema ou o
 * toque de chamada do celular (o padrão). Fica guardada no próprio celular (não vai no backup:
 * o endereço do arquivo só vale neste aparelho).
 */
object Toque {
    /** Quanto tempo toca: o de uma chamada que ninguém atende. */
    const val DURACAO_MS = 30_000L

    /** Quem pediu: o "Testar" dos Ajustes ou um prêmio de verdade. */
    enum class Origem { TESTE, PREMIO }

    /** O que está tocando agora (null = nada). */
    private val _tocando = MutableStateFlow<Origem?>(null)
    val tocando: StateFlow<Origem?> = _tocando.asStateFlow()

    /** A escolha dos Ajustes. [uri] null = o toque de chamada do celular. */
    data class Config(val ligado: Boolean, val uri: Uri?, val nome: String?)

    private const val PREFS = "toque"
    private const val LIGADO = "ligado"
    private const val URI = "uri"
    private const val NOME = "nome"

    /** Nome do toque padrão (o de chamada do celular). */
    const val TOQUE_DO_CELULAR = "Toque de chamada do celular"

    private var tocador: MediaPlayer? = null
    private var vibrador: Vibrator? = null
    private val mao by lazy { Handler(Looper.getMainLooper()) }
    private val pararNoFim = Runnable { parar() }

    fun config(context: Context): Config {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(
            ligado = p.getBoolean(LIGADO, true),
            uri = p.getString(URI, null)?.let(Uri::parse),
            nome = p.getString(NOME, null),
        )
    }

    fun ligar(context: Context, ligado: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(LIGADO, ligado).apply()
    }

    /** Guarda a música escolhida ([uri] null = volta ao toque de chamada do celular). */
    fun escolher(context: Context, uri: Uri?, nome: String?) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // O arquivo escolhido antes não precisa mais da permissão de leitura.
        p.getString(URI, null)?.let(Uri::parse)?.takeIf { it != uri }?.let { antigo ->
            runCatching { context.contentResolver.releasePersistableUriPermission(antigo, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        p.edit().apply {
            if (uri == null) remove(URI) else putString(URI, uri.toString())
            if (nome == null) remove(NOME) else putString(NOME, nome)
        }.apply()
    }

    /** Uma música escolhida no seletor de arquivos: guarda a permissão de ler o arquivo depois (mesmo com o app fechado). */
    fun escolherArquivo(context: Context, uri: Uri) {
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val nome = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()?.substringBeforeLast('.')
        escolher(context, uri, nome ?: "Música escolhida")
    }

    /** Um toque do sistema escolhido na lista do Android. */
    fun escolherToqueDoSistema(context: Context, uri: Uri?) {
        if (uri == null) return
        val nome = runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
        escolher(context, uri, nome ?: "Toque do celular")
    }

    /** Nome para mostrar nos Ajustes. */
    fun nomeDoToque(c: Config): String = if (c.uri == null) TOQUE_DO_CELULAR else c.nome ?: "Música escolhida"

    /**
     * Começa a tocar (o que estiver tocando para antes). Tenta a música escolhida e, se ela não abrir
     * (arquivo apagado...), o toque de chamada, o do alarme e o da notificação do celular.
     * Devolve false se nada tocou.
     */
    @Synchronized
    fun tocar(context: Context, origem: Origem): Boolean {
        parar()
        val ctx = context.applicationContext
        val opcoes = listOfNotNull(
            config(ctx).uri,
            Settings.System.DEFAULT_RINGTONE_URI,
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            Settings.System.DEFAULT_NOTIFICATION_URI,
        )
        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        for (u in opcoes) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(atributos)
                mp.setDataSource(ctx, u)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                tocador = mp
                break
            } catch (t: Throwable) {
                runCatching { mp.release() }
            }
        }
        if (tocador == null) return false
        vibrar(ctx, atributos)
        _tocando.value = origem
        mao.postDelayed(pararNoFim, DURACAO_MS)
        return true
    }

    /** Para o toque (e a vibração). */
    @Synchronized
    fun parar() {
        mao.removeCallbacks(pararNoFim)
        tocador?.let { mp ->
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        tocador = null
        vibrador?.let { runCatching { it.cancel() } }
        vibrador = null
        _tocando.value = null
    }

    /**
     * Toca e espera até o fim (o tempo da chamada ou alguém parar): o Vigia fica rodando enquanto
     * isso, para o celular não fechar o app no meio do toque.
     */
    suspend fun tocarEEsperar(context: Context, origem: Origem) {
        if (!tocar(context, origem)) return
        withTimeoutOrNull(DURACAO_MS + 2_000L) { tocando.first { it == null } }
    }

    @Suppress("DEPRECATION")
    private fun vibrar(ctx: Context, atributos: AudioAttributes) {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            ctx.getSystemService(Vibrator::class.java)
        } ?: return
        if (!v.hasVibrator()) return
        // Como uma chamada: vibra 1 s, para 1 s, de novo... até o toque parar.
        runCatching { v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1_000, 1_000), 0), atributos) }
        vibrador = v
    }
}

/** "Parar o toque" (no aviso do prêmio) e o aviso arrastado para fora: para o toque. */
class PararToque : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Toque.parar()
    }
}

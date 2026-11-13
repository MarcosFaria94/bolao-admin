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
 *
 * Desde a 0.32, também o som do prêmio secundário ([Qual.SECUNDARIO]; as outras faixas): desligado, o aviso toca o
 * som de notificação de sempre; ligado, o aviso fica quieto e toca o som escolhido, uma vez (até
 * [DURACAO_SECUNDARIO_MS]), no volume das notificações (no silencioso, não toca).
 */
object Toque {
    /** Quanto tempo toca: o de uma chamada que ninguém atende. */
    const val DURACAO_MS = 30_000L

    /** O som do prêmio secundário toca uma vez, no máximo por este tempo. */
    const val DURACAO_SECUNDARIO_MS = 12_000L

    /** Quem pediu: o "Testar" dos Ajustes ou um prêmio de verdade (desde a 0.32, também os do prêmio secundário). */
    enum class Origem(val qual: Qual) {
        TESTE(Qual.PRINCIPAL),
        PREMIO(Qual.PRINCIPAL),
        TESTE_SECUNDARIO(Qual.SECUNDARIO),
        SECUNDARIO(Qual.SECUNDARIO),
    }

    /**
     * Qual dos sons (desde a 0.32): o do prêmio principal (como uma chamada) ou o do prêmio secundário. As escolhas
     * ficam com nomes próprios nas preferências ([prefixo]; o principal com os de sempre).
     */
    enum class Qual(val prefixo: String, val ligadoPadrao: Boolean) {
        PRINCIPAL("", true),
        SECUNDARIO("sec_", false),
    }

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

    fun config(context: Context, qual: Qual = Qual.PRINCIPAL): Config {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(
            ligado = p.getBoolean(qual.prefixo + LIGADO, qual.ligadoPadrao),
            uri = p.getString(qual.prefixo + URI, null)?.let(Uri::parse),
            nome = p.getString(qual.prefixo + NOME, null),
        )
    }

    fun ligar(context: Context, ligado: Boolean, qual: Qual = Qual.PRINCIPAL) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(qual.prefixo + LIGADO, ligado).apply()
    }

    /** Guarda a música escolhida ([uri] null = volta ao toque de chamada do celular; no secundário, ao som de notificação). */
    fun escolher(context: Context, uri: Uri?, nome: String?, qual: Qual = Qual.PRINCIPAL) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // O arquivo escolhido antes não precisa mais da permissão de leitura (se o outro som não usa o mesmo).
        val outro = Qual.entries.filter { it != qual }.mapNotNull { p.getString(it.prefixo + URI, null)?.let(Uri::parse) }
        p.getString(qual.prefixo + URI, null)?.let(Uri::parse)?.takeIf { it != uri && it !in outro }?.let { antigo ->
            runCatching { context.contentResolver.releasePersistableUriPermission(antigo, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        p.edit().apply {
            if (uri == null) remove(qual.prefixo + URI) else putString(qual.prefixo + URI, uri.toString())
            if (nome == null) remove(qual.prefixo + NOME) else putString(qual.prefixo + NOME, nome)
        }.apply()
    }

    /** Uma música escolhida no seletor de arquivos: guarda a permissão de ler o arquivo depois (mesmo com o app fechado). */
    fun escolherArquivo(context: Context, uri: Uri, qual: Qual = Qual.PRINCIPAL) {
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val nome = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()?.substringBeforeLast('.')
        escolher(context, uri, nome ?: "Música escolhida", qual)
    }

    /** Um toque do sistema escolhido na lista do Android. */
    fun escolherToqueDoSistema(context: Context, uri: Uri?, qual: Qual = Qual.PRINCIPAL) {
        if (uri == null) return
        val nome = runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull()
        escolher(context, uri, nome ?: "Toque do celular", qual)
    }

    /** Nome do som padrão do prêmio secundário (o de notificação do celular). */
    const val SOM_DE_NOTIFICACAO = "Som de notificação do celular"

    /** Nome para mostrar nos Ajustes. */
    fun nomeDoToque(c: Config, qual: Qual = Qual.PRINCIPAL): String = when {
        c.uri != null -> c.nome ?: "Música escolhida"
        qual == Qual.SECUNDARIO -> SOM_DE_NOTIFICACAO
        else -> TOQUE_DO_CELULAR
    }

    /**
     * Começa a tocar (o que estiver tocando para antes). Tenta a música escolhida e, se ela não abrir
     * (arquivo apagado...), o toque de chamada, o do alarme e o da notificação do celular.
     * Devolve false se nada tocou.
     */
    @Synchronized
    fun tocar(context: Context, origem: Origem): Boolean {
        parar()
        val ctx = context.applicationContext
        val secundario = origem.qual == Qual.SECUNDARIO
        // O principal tenta a música escolhida e, se ela não abrir, os toques do celular; o secundário, o som de
        // notificação.
        val opcoes = if (secundario) {
            listOfNotNull(config(ctx, Qual.SECUNDARIO).uri, Settings.System.DEFAULT_NOTIFICATION_URI)
        } else {
            listOfNotNull(
                config(ctx).uri,
                Settings.System.DEFAULT_RINGTONE_URI,
                Settings.System.DEFAULT_ALARM_ALERT_URI,
                Settings.System.DEFAULT_NOTIFICATION_URI,
            )
        }
        // O principal no volume do alarme (toca no silencioso); o secundário no das notificações.
        val atributos = AudioAttributes.Builder()
            .setUsage(if (secundario) AudioAttributes.USAGE_NOTIFICATION else AudioAttributes.USAGE_ALARM)
            .setContentType(if (secundario) AudioAttributes.CONTENT_TYPE_SONIFICATION else AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        for (u in opcoes) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(atributos)
                mp.setDataSource(ctx, u)
                mp.isLooping = !secundario
                // O secundário toca uma vez: parou sozinho, acabou.
                if (secundario) mp.setOnCompletionListener { mao.post { if (tocador === it) parar() } }
                mp.prepare()
                mp.start()
                tocador = mp
                break
            } catch (t: Throwable) {
                runCatching { mp.release() }
            }
        }
        if (tocador == null) return false
        if (!secundario) vibrar(ctx, atributos)
        _tocando.value = origem
        mao.postDelayed(pararNoFim, if (secundario) DURACAO_SECUNDARIO_MS else DURACAO_MS)
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
        val tempo = if (origem.qual == Qual.SECUNDARIO) DURACAO_SECUNDARIO_MS else DURACAO_MS
        withTimeoutOrNull(tempo + 2_000L) { tocando.first { it == null } }
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

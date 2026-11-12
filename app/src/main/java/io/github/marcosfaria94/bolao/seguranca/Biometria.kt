package io.github.marcosfaria94.bolao.seguranca

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Digital (ou rosto), com o PIN/padrão do celular como alternativa. */
object Biometria {

    private const val AUTENTICADORES = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** O celular tem biometria ou bloqueio de tela configurado? */
    fun disponivel(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * O bloqueio do app (desde a 0.30, em Ajustes > Biometria, no grupo Celular: vale para o app todo, não para a
     * sessão): [pedir] a biometria ao abrir o app e de novo depois de [minutos] fora dele. Até a 0.29 era sempre
     * ligado, com 3 minutos.
     */
    data class Bloqueio(val pedir: Boolean, val minutos: Int)

    private const val PREFS = "biometria"
    private const val PEDIR = "pedir"
    private const val MINUTOS = "minutos"

    /** Os tempos fora do app que dá para escolher (em minutos) até a biometria ser pedida de novo. */
    val TEMPOS: List<Int> = listOf(1, 3, 10, 30)

    /** O tempo de sempre (o das versões até a 0.29): ir ao app da Caixa ou ao WhatsApp e voltar logo não pede de novo. */
    const val MINUTOS_PADRAO = 3

    fun bloqueio(context: Context): Bloqueio {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Bloqueio(p.getBoolean(PEDIR, true), p.getInt(MINUTOS, MINUTOS_PADRAO).takeIf { it in TEMPOS } ?: MINUTOS_PADRAO)
    }

    fun pedir(context: Context, sim: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PEDIR, sim).apply()
    }

    fun minutos(context: Context, minutos: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(MINUTOS, minutos).apply()
    }

    /** O app fica bloqueado até a biometria? Ligada nos Ajustes e o celular com biometria ou bloqueio de tela. */
    fun bloqueia(context: Context): Boolean = bloqueio(context).pedir && disponivel(context)

    /** Pede a autorização. true = autorizado; false = cancelado, erro ou sem biometria. */
    suspend fun autorizar(activity: FragmentActivity, titulo: String, subtitulo: String? = null): Boolean {
        if (!disponivel(activity)) return false
        return suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (cont.isActive) cont.resume(false)
                    }
                    // onAuthenticationFailed: digital não reconhecida; a janela continua aberta para tentar de novo.
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(titulo)
                .apply { subtitulo?.let { setSubtitle(it) } }
                .setAllowedAuthenticators(AUTENTICADORES)
                .build()
            prompt.authenticate(info)
            cont.invokeOnCancellation { runCatching { prompt.cancelAuthentication() } }
        }
    }
}

/** A atividade por trás de um Context do Compose. */
fun Context.atividade(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.atividade()
    else -> null
}

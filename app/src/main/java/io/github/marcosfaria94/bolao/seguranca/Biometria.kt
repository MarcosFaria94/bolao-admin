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

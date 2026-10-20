package io.github.marcosfaria94.bolao

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.seguranca.Biometria
import io.github.marcosfaria94.bolao.seguranca.TelaBloqueio
import io.github.marcosfaria94.bolao.ui.AppRaiz
import io.github.marcosfaria94.bolao.ui.tema.BolaoTema
import kotlinx.coroutines.launch

/**
 * Tela única do app. Abre bloqueada pela biometria (digital, rosto ou o bloqueio do celular)
 * e volta a bloquear depois de alguns minutos fora do app.
 */
class MainActivity : FragmentActivity() {

    private val bloqueado = mutableStateOf(false)
    private var saiuEm = 0L
    private var pedindo = false
    /** Pede a digital sozinho uma vez ao abrir ou ao voltar; depois, só pelo botão. */
    private var pedirAoVoltar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) lerDestino(intent)
        if (Biometria.disponivel(this)) {
            bloqueado.value = true
            pedirAoVoltar = true
        }
        setContent {
            BolaoTema {
                Box(Modifier.fillMaxSize()) {
                    AppRaiz()
                    if (bloqueado.value) TelaBloqueio(aoDesbloquear = ::desbloquear)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bolao.visivel = true
        val longe = saiuEm > 0 && SystemClock.elapsedRealtime() - saiuEm > TEMPO_SEM_BLOQUEIO_MS
        if (longe && !bloqueado.value && Biometria.disponivel(this)) {
            bloqueado.value = true
            pedirAoVoltar = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (bloqueado.value && pedirAoVoltar) {
            pedirAoVoltar = false
            desbloquear()
        }
    }

    override fun onStop() {
        super.onStop()
        bolao.visivel = false
        if (!isChangingConfigurations) saiuEm = SystemClock.elapsedRealtime()
    }

    private fun desbloquear() {
        if (pedindo) return
        if (!Biometria.disponivel(this)) {
            bloqueado.value = false
            return
        }
        pedindo = true
        lifecycleScope.launch {
            val ok = Biometria.autorizar(this@MainActivity, getString(R.string.app_name), "Desbloqueie para abrir o bolão")
            pedindo = false
            if (ok) bloqueado.value = false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lerDestino(intent)
    }

    private fun lerDestino(intent: Intent?) {
        intent?.getStringExtra(Notificador.EXTRA_DESTINO)?.let { bolao.destinoPendente.value = it }
    }

    private companion object {
        /** Ir ao app da Caixa ou ao WhatsApp e voltar logo não pede a digital de novo. */
        const val TEMPO_SEM_BLOQUEIO_MS = 3 * 60 * 1000L
    }
}

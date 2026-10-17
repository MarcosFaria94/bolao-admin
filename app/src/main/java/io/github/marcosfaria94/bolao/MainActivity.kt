package io.github.marcosfaria94.bolao

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.ui.AppRaiz
import io.github.marcosfaria94.bolao.ui.tema.BolaoTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) lerDestino(intent)
        setContent {
            BolaoTema {
                AppRaiz()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lerDestino(intent)
    }

    private fun lerDestino(intent: Intent?) {
        intent?.getStringExtra(Notificador.EXTRA_DESTINO)?.let { bolao.destinoPendente.value = it }
    }
}

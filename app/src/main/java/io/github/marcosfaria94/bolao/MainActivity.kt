package io.github.marcosfaria94.bolao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.marcosfaria94.bolao.ui.AppRaiz
import io.github.marcosfaria94.bolao.ui.tema.BolaoTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BolaoTema {
                AppRaiz()
            }
        }
    }
}

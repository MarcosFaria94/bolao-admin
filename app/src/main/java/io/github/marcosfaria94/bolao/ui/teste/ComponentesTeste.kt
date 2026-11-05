package io.github.marcosfaria94.bolao.ui.teste

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.tema.VermelhoTeste

/** Faixa vermelha (letra branca) embaixo do título em todas as telas enquanto o modo de teste estiver ligado. */
@Composable
fun FaixaModoTeste(aoSair: () -> Unit) {
    Surface(
        color = VermelhoTeste,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Modo de teste · dados de exemplo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = aoSair, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text("Sair", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Confirmação para sair do modo de teste (apaga os dados de exemplo). */
@Composable
fun DialogoSairDoTeste(aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    DialogoConfirmar(
        titulo = "Sair do modo de teste?",
        texto = "Os dados de exemplo e os comprovantes fictícios serão apagados, e o app volta para os seus dados reais, como estavam.",
        confirmar = "Sair e apagar",
        aoConfirmar = {
            aoFechar()
            ModoTeste.sair(ctx)
        },
        aoFechar = aoFechar,
    )
}

@Composable
fun BotaoExperimentar(preparando: Boolean, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = aoTocar, enabled = !preparando, modifier = modifier) {
        Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (preparando) "Preparando o exemplo..." else "Experimentar com dados de exemplo")
    }
}

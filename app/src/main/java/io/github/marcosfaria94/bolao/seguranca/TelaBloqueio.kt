package io.github.marcosfaria94.bolao.seguranca

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.R

/** Cobre o app enquanto ele estiver bloqueado (não deixa tocar no que está embaixo). */
@Composable
fun TelaBloqueio(aoDesbloquear: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(32.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_frente),
                contentDescription = null,
                modifier = Modifier.size(140.dp).clip(RoundedCornerShape(32.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text("Bolão dos Amigos", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = aoDesbloquear) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Desbloquear")
            }
        }
    }
}

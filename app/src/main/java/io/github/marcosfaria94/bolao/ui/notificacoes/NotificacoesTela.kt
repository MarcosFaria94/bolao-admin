package io.github.marcosfaria94.bolao.ui.notificacoes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Pendencia
import io.github.marcosfaria94.bolao.notificacoes.Notificador

/**
 * Notificações (o sino da Home, ou deslizar o dedo para a direita a partir da Home), bem simples: o
 * que falta fazer em texto corrido, um item embaixo do outro com espaço entre eles (o mais recente
 * primeiro). Tocar num item abre a tela que resolve. [ativa]: a página está na tela (ao lado da
 * Home ela também é desenhada enquanto o dedo desliza).
 */
@Composable
fun NotificacoesTela(pendencias: List<Pendencia>, abrir: (String) -> Unit, ativa: Boolean = true) {
    val ctx = LocalContext.current
    // Com a lista na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(ativa) { if (ativa) Notificador.limparAcoes(ctx) }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (pendencias.isEmpty()) {
            item {
                Text(
                    "Tudo em dia. Quando houver algo a fazer no bolão, aparece aqui.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            }
        }
        items(pendencias, key = { it.chave }) { p ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { abrir(p.destino) }
                    .padding(horizontal = 8.dp, vertical = 14.dp),
            ) {
                Text(
                    p.titulo,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    p.texto,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

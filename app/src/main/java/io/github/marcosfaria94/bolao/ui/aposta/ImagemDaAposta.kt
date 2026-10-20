package io.github.marcosfaria94.bolao.ui.aposta

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.imagem.ImagemConferencia
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Os números em bolas, no estilo da conferência do site da Caixa: só os apostados (aposta
 * feita) ou os sorteados e os apostados com os acertos (apuração). É a mesma imagem que vai
 * anexada ao comprovante da apuração.
 */
@Composable
fun ImagemDaAposta(a: Aposta, ap: Apuracao?, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val imagem by produceState<Bitmap?>(null, a, ap) {
        value = withContext(Dispatchers.Default) { runCatching { ImagemConferencia.gerar(ctx, a, ap) }.getOrNull() }
    }
    val forma = RoundedCornerShape(16.dp)
    val bmp = imagem
    if (bmp == null) {
        Box(
            modifier.fillMaxWidth().height(180.dp).clip(forma).background(corModalidade(a.modalidade.cor)),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = Color.White) }
    } else {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = if (ap == null) "Números apostados" else "Números sorteados e apostados",
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth().clip(forma),
        )
    }
}

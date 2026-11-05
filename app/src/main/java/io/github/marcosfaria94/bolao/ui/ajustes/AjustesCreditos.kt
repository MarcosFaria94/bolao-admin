package io.github.marcosfaria94.bolao.ui.ajustes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.github.marcosfaria94.bolao.data.NOME_DO_APP
import io.github.marcosfaria94.bolao.net.Atualizador
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.Secao

/** O app do Claude na Google Play. */
private const val PACOTE_CLAUDE = "com.anthropic.claude"

/** Cor dos emblemas da tecnologia (discreta, a mesma para todos). */
private val CorTecnologia = Color(0xFF475569)

/**
 * Créditos: o app foi criado usando o Claude (o nome e o ícone do app do Claude, quando ele está
 * instalado no celular; tocar abre a página do Claude na Google Play para baixar) e a tecnologia usada.
 */
@Composable
internal fun Creditos() {
    val ctx = LocalContext.current
    val icone = remember { iconeDoClaude(ctx) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { abrirClaudeNaLoja(ctx) },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp),
        ) {
            Text(
                "Criado usando o",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (icone != null) {
                Image(icone, contentDescription = "Claude", modifier = Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)))
            } else {
                EmblemaIcone(Icons.Outlined.AutoAwesome, Color(0xFFC15F3C), 92.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Claude",
                fontFamily = FontFamily.Serif,
                fontSize = 46.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "a IA da Anthropic",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "O código e as telas deste app foram escritos em conversa com o Claude.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            FilledTonalButton(onClick = { abrirClaudeNaLoja(ctx) }) {
                Icon(Icons.Outlined.Shop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Baixar o Claude na Google Play")
            }
        }
    }
    Secao("Tecnologia") {
        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            LinhaTecnologia(Icons.Outlined.Code, "Kotlin 2.2", "A linguagem em que o app foi escrito")
            Divisoria()
            LinhaTecnologia(Icons.Outlined.Palette, "Jetpack Compose · Material 3", "As telas, os gestos e as animações")
            Divisoria()
            LinhaTecnologia(Icons.Outlined.PhoneAndroid, "Android 10 ou mais novo", "Feito para o Android, direto no celular")
            Divisoria()
            LinhaTecnologia(Icons.Outlined.DocumentScanner, "ML Kit", "A leitura dos comprovantes da Caixa e dos Pix")
            Divisoria()
            LinhaTecnologia(Icons.Outlined.Schedule, "WorkManager", "A apuração e os avisos com o app fechado")
            Divisoria()
            LinhaTecnologia(Icons.Outlined.Cloud, "Loterias Caixa", "Os resultados e os prêmios oficiais")
        }
    }
    Text(
        "$NOME_DO_APP · versão ${Atualizador.nomeInstalado}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LinhaTecnologia(icone: ImageVector, titulo: String, texto: String) {
    LinhaAjuste(
        emblema = { EmblemaIcone(icone, CorTecnologia, 38.dp) },
        titulo = titulo,
        subtitulo = texto,
    )
}

/** O ícone do app do Claude, quando ele está instalado no celular (null se não estiver). */
private fun iconeDoClaude(ctx: Context): ImageBitmap? = runCatching {
    ctx.packageManager.getApplicationIcon(PACOTE_CLAUDE).toBitmap(256, 256).asImageBitmap()
}.getOrNull()

/** Abre a página do Claude na Google Play (direto no app da Play Store; senão, no navegador). */
fun abrirClaudeNaLoja(ctx: Context) {
    val market = Uri.parse("market://details?id=$PACOTE_CLAUDE")
    val tentativas = listOf(
        Intent(Intent.ACTION_VIEW, market).setPackage("com.android.vending"),
        Intent(Intent.ACTION_VIEW, market),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACOTE_CLAUDE")),
    )
    for (i in tentativas) {
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (t: ActivityNotFoundException) {
            // Tenta o próximo jeito.
        }
    }
}

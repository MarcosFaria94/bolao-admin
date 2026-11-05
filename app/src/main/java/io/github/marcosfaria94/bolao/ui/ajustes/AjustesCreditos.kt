package io.github.marcosfaria94.bolao.ui.ajustes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.comum.copiar
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Check
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Button

/** O app do Claude na Google Play. */
private const val PACOTE_CLAUDE = "com.anthropic.claude"

/** A cor do Claude (o contorno da pílula dos créditos e o ícone genérico, sem o app instalado). */
private val LARANJA_CLAUDE = Color(0xFFC15F3C)

/** A tecnologia usada no app (o "Tecnologia" do rodapé dos Ajustes): o nome e para que serve. */
private val TECNOLOGIA = listOf(
    "Kotlin 2.2" to "A linguagem em que o app foi escrito",
    "Jetpack Compose · Material 3" to "As telas, os gestos e as animações",
    "Android 10 ou mais novo" to "Feito para o Android, direto no celular",
    "ML Kit" to "A leitura dos comprovantes da Caixa e dos Pix",
    "WorkManager" to "A apuração e os avisos com o app fechado",
    "Loterias Caixa" to "Os resultados e os prêmios oficiais",
)

/**
 * O "Apoie" do rodapé dos Ajustes (desde a 0.18): o texto, o nome e a chave Pix do Administrador; o
 * botão copia a chave.
 */
internal const val APOIE_TEXTO =
    "Gratuito e sem anúncios, sempre. Fique à vontade para retribuir com qualquer valor. Toda doação é bem-vinda."
internal const val APOIE_NOME = "Marcos de Faria Valadares"
internal const val APOIE_PIX = "pixfacil@aol.com"

/** Quanto tempo o botão "Apoie" mostra "Pix copiado" depois do toque. */
private const val COPIADO_MS = 2500L

/**
 * Os Créditos, no rodapé da lista dos Ajustes (até a 0.15 eram uma tela). Primeiro o "Apoie" (desde a
 * 0.18): a xícara de café sorrindo, o texto, o nome e a chave Pix, e o botão que copia a chave. Depois,
 * "Criado com" e, embaixo, "Claude", em destaque e no centro (numa pílula com o contorno na cor do
 * Claude; com o ícone do app do Claude, quando ele está instalado; tocar abre a página dele na Google
 * Play), o nome do app com a versão instalada e o botão discreto "Tecnologia", que mostra (ou esconde)
 * a tecnologia ali mesmo.
 */
@Composable
internal fun CreditosNoRodape() {
    val ctx = LocalContext.current
    val icone = remember { iconeDoClaude(ctx) }
    var tecnologia by rememberSaveable { mutableStateOf(false) }
    val discreta = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
    ) {
        Apoie()
        Spacer(Modifier.height(22.dp))
        val pilula = RoundedCornerShape(28.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(pilula)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.5.dp, LARANJA_CLAUDE.copy(alpha = 0.55f), pilula)
                .clickable(onClickLabel = "Abrir o Claude na Google Play") { abrirClaudeNaLoja(ctx) }
                .padding(start = 30.dp, end = 32.dp, top = 12.dp, bottom = 14.dp),
        ) {
            // Duas linhas: "Criado com" em cima e o Claude (com o ícone) embaixo.
            Text("Criado com", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 24.sp), color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icone != null) {
                    Image(icone, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    EmblemaIcone(Icons.Outlined.AutoAwesome, LARANJA_CLAUDE, 32.dp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Claude",
                    fontFamily = FontFamily.Serif,
                    fontSize = 27.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "$NOME_DO_APP · versão ${Atualizador.nomeInstalado}",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = discreta,
            textAlign = TextAlign.Center,
        )
        TextButton(
            onClick = { tecnologia = !tecnologia },
            colors = ButtonDefaults.textButtonColors(contentColor = discreta),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text("Tecnologia", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
            Spacer(Modifier.width(2.dp))
            Icon(if (tecnologia) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        if (tecnologia) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                TECNOLOGIA.forEachIndexed { i, (nome, texto) ->
                    if (i > 0) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(nome, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold)
                        Text(texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp), color = discreta)
                    }
                }
            }
        }
    }
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

/**
 * O "Apoie": a xícara de café sorrindo, o texto, o nome e a chave Pix, e o botão dourado que copia a
 * chave (a partir do Android 13 o sistema confirma a cópia; o botão também mostra "Pix copiado").
 */
@Composable
private fun Apoie() {
    val ctx = LocalContext.current
    var copiado by remember { mutableStateOf(false) }
    LaunchedEffect(copiado) {
        if (copiado) {
            delay(COPIADO_MS)
            copiado = false
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Image(Desenhos.Xicara, contentDescription = "Xícara de café sorrindo", modifier = Modifier.size(84.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                APOIE_TEXTO,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(APOIE_NOME, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(APOIE_PIX, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    copiar(ctx, "Chave Pix", APOIE_PIX)
                    copiado = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Dourado, contentColor = AzulMarinho),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                modifier = Modifier.height(52.dp),
            ) {
                Icon(
                    if (copiado) Icons.Outlined.Check else Icons.Outlined.VolunteerActivism,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(if (copiado) "Pix copiado" else "Apoie", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


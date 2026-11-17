package io.github.marcosfaria94.bolao.ui.ajustes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import io.github.marcosfaria94.bolao.domain.Pix
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.copiar
import io.github.marcosfaria94.bolao.ui.comum.deslizarParaOsLados
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import kotlinx.coroutines.delay

/**
 * O "Apoie" (Ajustes > Feedback > Doação). Desde a 0.37 (pedido do usuário): o texto novo, a janela que salta por cima
 * dos Ajustes (um pop-up, [JanelaDoApoie]; até a 0.36 era uma tela) e o botão "Apoie", que abre a tela clássica do Pix
 * ([TelaDoPix]): o QR Code e o botão que copia a chave. O título e a xícara continuam.
 */
const val APOIE_TITULO = "Que tal um café quentinho?"
const val APOIE_TEXTO =
    "Este aplicativo é mantido por paixão. Não vendemos seus dados, não temos anúncios e não cobramos mensalidade. " +
        "Se você gosta disso, considere deixar umas moedas para o café."
const val APOIE_PIX = "pixfacil@aol.com"

/** O nome de quem recebe no QR Code (o mesmo autorizado no repositório) e a cidade do código (o Banco Central pede uma). */
const val APOIE_NOME = "Marcos Faria"
const val APOIE_CIDADE = "BRASIL"

/** O código do Pix ("Pix Copia e Cola") que vai no QR Code. */
val PIX_DO_APOIE: String by lazy { Pix.copiaECola(APOIE_PIX, APOIE_NOME, APOIE_CIDADE) }

const val TITULO_DO_PIX = "Pix"
const val COPIAR_CHAVE_PIX = "Copiar chave Pix"
const val PIX_COPIADO = "Pix copiado"
const val QR_CODE_DO_PIX = "QR Code do Pix"
const val INSTRUCAO_DO_PIX = "Abra o app do seu banco, escolha pagar com Pix e leia o QR Code, ou copie a chave. O valor é você quem escolhe."

/** Quanto tempo o botão mostra "Pix copiado" depois do toque. */
private const val COPIADO_MS = 2500L

/**
 * O que a janela do Apoie mostra: o título, a xícara de café sorrindo, o texto (justificado) e o botão dourado "Apoie",
 * que abre a tela do Pix ([aoApoiar]).
 */
@Composable
internal fun ConteudoDoApoie(aoApoiar: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Text(
            APOIE_TITULO,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Image(Desenhos.Xicara, contentDescription = "Xícara de café sorrindo", modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            APOIE_TEXTO,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = aoApoiar,
            colors = ButtonDefaults.buttonColors(containerColor = Dourado, contentColor = AzulMarinho),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
            modifier = Modifier.height(52.dp),
        ) {
            Icon(Icons.Outlined.VolunteerActivism, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Apoie", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * A janela do Apoie (desde a 0.37: salta por cima dos Ajustes, ao tocar em "Doação"). "Apoie" troca a janela pela tela do
 * Pix; fechar a tela do Pix (ou tocar fora da janela) volta aos Ajustes.
 */
@Composable
fun JanelaDoApoie(aoFechar: () -> Unit) {
    var pix by rememberSaveable { mutableStateOf(false) }
    if (pix) {
        TelaDoPix(aoFechar = aoFechar)
        return
    }
    Dialog(onDismissRequest = aoFechar) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 8.dp)) {
                ConteudoDoApoie(aoApoiar = { pix = true })
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = aoFechar) { Text("Agora não") }
            }
        }
    }
}

/**
 * O "Apoie" numa tela (o endereço antigo "ajustes/doacao", que até a 0.36 era o jeito de abrir): o mesmo conteúdo da
 * janela, num cartão; o botão abre a tela do Pix.
 */
@Composable
internal fun Apoie() {
    var pix by rememberSaveable { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ConteudoDoApoie(aoApoiar = { pix = true }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp))
    }
    if (pix) TelaDoPix(aoFechar = { pix = false })
}

/**
 * A tela clássica do Pix (desde a 0.37, pelo "Apoie"): o QR Code (preto no branco também no tema escuro, para os bancos
 * lerem), a chave e o botão "Copiar chave Pix" (a partir do Android 13 o sistema confirma a cópia; o botão também mostra
 * "Pix copiado"). Fecha no X, deslizando o dedo para os lados ou pelo voltar do celular.
 */
@Composable
fun TelaDoPix(aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    var copiado by remember { mutableStateOf(false) }
    LaunchedEffect(copiado) {
        if (copiado) {
            delay(COPIADO_MS)
            copiado = false
        }
    }
    Dialog(onDismissRequest = aoFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize().deslizarParaOsLados(aoFechar)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp)) {
                    Text(TITULO_DO_PIX, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = aoFechar) { Icon(Icons.Outlined.Close, contentDescription = "Fechar") }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(INSTRUCAO_DO_PIX, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Box(
                        Modifier
                            .widthIn(max = 300.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(26.dp),
                    ) {
                        QrCode(PIX_DO_APOIE, Modifier.fillMaxSize())
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Chave Pix (e-mail)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(APOIE_PIX, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            copiar(ctx, "Chave Pix", APOIE_PIX)
                            copiado = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Dourado, contentColor = AzulMarinho),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Icon(if (copiado) Icons.Outlined.Check else Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(if (copiado) PIX_COPIADO else COPIAR_CHAVE_PIX, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Os módulos (quadradinhos) do QR Code do [texto]: [linhas][coluna] = escuro. Correção de erro média (M). */
fun modulosDoQrCode(texto: String): List<BooleanArray> {
    val m = Encoder.encode(texto, ErrorCorrectionLevel.M).matrix
    return (0 until m.height).map { y -> BooleanArray(m.width) { x -> m.get(x, y).toInt() == 1 } }
}

/** O QR Code do [texto], preto, desenhado quadradinho por quadradinho no espaço que tiver (o fundo branco vem de fora). */
@Composable
fun QrCode(texto: String, modifier: Modifier = Modifier) {
    val modulos = remember(texto) { modulosDoQrCode(texto) }
    Canvas(modifier.semantics { contentDescription = QR_CODE_DO_PIX }) {
        val n = modulos.size
        if (n == 0) return@Canvas
        val lado = minOf(size.width, size.height) / n
        val inicio = Offset((size.width - lado * n) / 2f, (size.height - lado * n) / 2f)
        modulos.forEachIndexed { y, linha ->
            linha.forEachIndexed { x, escuro ->
                if (escuro) drawRect(Color.Black, Offset(inicio.x + x * lado, inicio.y + y * lado), Size(lado + 0.5f, lado + 0.5f))
            }
        }
    }
}

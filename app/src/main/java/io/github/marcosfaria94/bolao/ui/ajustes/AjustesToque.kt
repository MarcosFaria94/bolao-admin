package io.github.marcosfaria94.bolao.ui.ajustes

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.notificacoes.Toque
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import kotlinx.coroutines.launch

/** Rosa do toque (o mesmo do menu dos Ajustes). */
private val RosaToque = Color(0xFFDB2777)

/**
 * Toque do prêmio: quando uma aposta acerta o prêmio principal, o celular toca como uma chamada
 * ([Toque.DURACAO_MS], vibrando, no volume do alarme). Liga e desliga, escolhe a música (um arquivo
 * de áudio ou um toque do celular) e testa.
 */
@Composable
internal fun ToqueDoPremio() {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var versao by remember { mutableIntStateOf(0) }
    val c = remember(versao) { Toque.config(ctx) }
    val tocando by Toque.tocando.collectAsStateWithLifecycle()
    val testando = tocando == Toque.Origem.TESTE
    // Saiu da tela no meio do teste: para.
    DisposableEffect(Unit) { onDispose { if (Toque.tocando.value == Toque.Origem.TESTE) Toque.parar() } }

    val escolherMusica = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Toque.escolherArquivo(ctx, uri)
            versao++
        }
    }
    val escolherToque = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (r.resultCode == Activity.RESULT_OK && uri != null) {
            if (uri == Settings.System.DEFAULT_RINGTONE_URI) Toque.escolher(ctx, null, null) else Toque.escolherToqueDoSistema(ctx, uri)
            versao++
        }
    }

    ExplicacaoAjuste(
        "Quando uma aposta acertar o prêmio principal, o celular toca como uma chamada: " +
            "${Toque.DURACAO_MS / 1000} segundos, vibrando, no volume do alarme (toca mesmo no silencioso). " +
            "Para pelo aviso do celular (\"Parar o toque\") ou ao abrir o app.",
    )
    QuadroAjuste {
        LinhaAjuste(
            emblema = { EmblemaIcone(Icons.Outlined.RingVolume, RosaToque, TAMANHO_EMBLEMA) },
            titulo = "Tocar no prêmio principal",
            subtitulo = if (c.ligado) "Ligado" else "Desligado: só o aviso de sempre",
            aoTocar = {
                Toque.ligar(ctx, !c.ligado)
                versao++
            },
            final = {
                Switch(checked = c.ligado, onCheckedChange = {
                    Toque.ligar(ctx, it)
                    versao++
                })
            },
        )
        Divisoria()
        LinhaAjuste(
            emblema = { EmblemaIcone(Icons.Outlined.MusicNote, Color(0xFF7C3AED), TAMANHO_EMBLEMA) },
            titulo = "Música",
            subtitulo = Toque.nomeDoToque(c),
        )
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { escolherMusica.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Escolher uma música")
            }
            OutlinedButton(
                onClick = {
                    val pedido = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Toque do prêmio principal")
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_RINGTONE_URI)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, c.uri ?: Settings.System.DEFAULT_RINGTONE_URI)
                    try {
                        escolherToque.launch(pedido)
                    } catch (t: ActivityNotFoundException) {
                        escopo.launch { avisos.showSnackbar("Este celular não abriu a lista de toques: escolha uma música.") }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Escolher um toque do celular")
            }
            if (c.uri != null) {
                TextButton(
                    onClick = {
                        Toque.escolher(ctx, null, null)
                        versao++
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Voltar ao toque de chamada do celular") }
            }
        }
        Divisoria()
        LinhaAjuste(
            emblema = { EmblemaIcone(if (testando) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, Color(0xFF1F8F5F), TAMANHO_EMBLEMA) },
            titulo = "Testar o toque",
            subtitulo = if (testando) "Tocando como no prêmio..." else "Toca do jeito que vai tocar no prêmio",
            final = {
                if (testando) {
                    Button(onClick = { Toque.parar() }) { Text("Parar") }
                } else {
                    OutlinedButton(onClick = {
                        if (!Toque.tocar(ctx, Toque.Origem.TESTE)) {
                            escopo.launch { avisos.showSnackbar("Não foi possível tocar a música: escolha outra.") }
                        }
                    }) { Text("Testar") }
                }
            },
        )
    }
}

/** O título da seção do som do prêmio secundário (Ajustes > Notificações, desde a 0.32). */
const val TITULO_DO_SOM_SECUNDARIO = "Som do prêmio secundário"

/** A chave do som próprio do prêmio secundário. */
const val SOM_PROPRIO_NO_SECUNDARIO = "Som próprio no prêmio secundário"

/**
 * O som do prêmio secundário (desde a 0.32, pedido do usuário): as outras faixas, que não são o prêmio principal.
 * Desligado, o aviso "Aposta premiada" toca o som de notificação de sempre; ligado, o aviso fica quieto e o app toca
 * o som escolhido (uma música, um som do celular ou o de notificação), uma vez, no volume das notificações (no
 * silencioso, não toca). Testar toca do jeito que vai tocar.
 */
@Composable
internal fun SomDoPremioSecundario() {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val qual = Toque.Qual.SECUNDARIO
    var versao by remember { mutableIntStateOf(0) }
    val c = remember(versao) { Toque.config(ctx, qual) }
    val tocando by Toque.tocando.collectAsStateWithLifecycle()
    val testando = tocando == Toque.Origem.TESTE_SECUNDARIO
    DisposableEffect(Unit) { onDispose { if (Toque.tocando.value == Toque.Origem.TESTE_SECUNDARIO) Toque.parar() } }

    val escolherMusica = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Toque.escolherArquivo(ctx, uri, qual)
            versao++
        }
    }
    val escolherSom = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (r.resultCode == Activity.RESULT_OK && uri != null) {
            if (uri == Settings.System.DEFAULT_NOTIFICATION_URI) Toque.escolher(ctx, null, null, qual) else Toque.escolherToqueDoSistema(ctx, uri, qual)
            versao++
        }
    }

    ExplicacaoAjuste(
        "Nas outras faixas (o prêmio secundário), o aviso \"Aposta premiada\" toca o som de notificação de sempre. " +
            "Ligue para escolher um som só para ele: toca uma vez, no volume das notificações (no silencioso, não toca).",
    )
    QuadroAjuste {
        LinhaAjuste(
            emblema = { EmblemaIcone(Icons.Outlined.NotificationsActive, Color(0xFF0E7490), TAMANHO_EMBLEMA) },
            titulo = SOM_PROPRIO_NO_SECUNDARIO,
            subtitulo = if (c.ligado) "Ligado" else "Desligado: o som de notificação de sempre",
            aoTocar = {
                Toque.ligar(ctx, !c.ligado, qual)
                versao++
            },
            final = {
                Switch(checked = c.ligado, onCheckedChange = {
                    Toque.ligar(ctx, it, qual)
                    versao++
                })
            },
        )
        if (c.ligado) {
            Divisoria()
            LinhaAjuste(
                emblema = { EmblemaIcone(Icons.Outlined.MusicNote, Color(0xFF7C3AED), TAMANHO_EMBLEMA) },
                titulo = "Som",
                subtitulo = Toque.nomeDoToque(c, qual),
            )
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { escolherMusica.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Escolher uma música")
                }
                OutlinedButton(
                    onClick = {
                        val pedido = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, TITULO_DO_SOM_SECUNDARIO)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, c.uri ?: Settings.System.DEFAULT_NOTIFICATION_URI)
                        try {
                            escolherSom.launch(pedido)
                        } catch (t: ActivityNotFoundException) {
                            escopo.launch { avisos.showSnackbar("Este celular não abriu a lista de sons: escolha uma música.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Escolher um som do celular")
                }
                if (c.uri != null) {
                    TextButton(
                        onClick = {
                            Toque.escolher(ctx, null, null, qual)
                            versao++
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Voltar ao som de notificação do celular") }
                }
            }
            Divisoria()
            LinhaAjuste(
                emblema = { EmblemaIcone(if (testando) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, Color(0xFF1F8F5F), TAMANHO_EMBLEMA) },
                titulo = "Testar o som",
                subtitulo = if (testando) "Tocando como no prêmio..." else "Toca do jeito que vai tocar no prêmio secundário",
                final = {
                    if (testando) {
                        Button(onClick = { Toque.parar() }) { Text("Parar") }
                    } else {
                        OutlinedButton(onClick = {
                            if (!Toque.tocar(ctx, Toque.Origem.TESTE_SECUNDARIO)) {
                                escopo.launch { avisos.showSnackbar("Não foi possível tocar o som: escolha outro.") }
                            }
                        }) { Text("Testar") }
                    }
                },
            )
        }
    }
}

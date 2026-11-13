package io.github.marcosfaria94.bolao.ui.ajustes

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.notificacoes.AvisoDeEspeciais
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.notificacoes.SegundoPlano
import io.github.marcosfaria94.bolao.notificacoes.Toque
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import kotlinx.coroutines.launch

/*
 * A tela das Notificações (Ajustes > Notificações), refeita na 0.34 (pedido do usuário: "tá muito feio"). Até a 0.33
 * eram três seções com parágrafos de explicação, linhas apertadas e botões empilhados. Agora:
 * 1. o quadro do alto, azul-noite com o sino dourado, diz se está tudo pronto para os avisos chegarem com o app
 *    fechado, com as três liberações do celular (permissão, bateria e inicialização automática) em linhas curtas;
 * 2. "O QUE AVISAR": só chaves, uma por linha (o prêmio principal, o prêmio secundário e, desde a 0.34, os concursos
 *    especiais, ligado por padrão);
 * 3. "SONS": o toque de cada prêmio, com o "Testar" no fim da linha; tocar na linha abre a janela de escolha
 *    (uma música, um toque do celular ou o de sempre).
 */

/** Os nomes das linhas da tela (também usados nos testes). */
const val GRUPO_O_QUE_AVISAR = "O QUE AVISAR"
const val GRUPO_SONS = "SONS"
const val AVISO_PREMIO_PRINCIPAL = "Prêmio principal"
const val AVISO_PREMIO_SECUNDARIO = "Prêmio secundário"
const val AVISO_CONCURSOS_ESPECIAIS = "Concursos especiais"
const val SOM_DO_PRINCIPAL = "Toque do prêmio principal"
const val TESTAR_O_TOQUE = "Testar o toque"
const val TESTAR_O_SOM = "Testar o som"
const val TUDO_PRONTO = "Avisos prontos"
const val FALTA_LIBERAR = "Falta liberar"

private val RosaToque = Color(0xFFDB2777)
private val Turquesa = Color(0xFF0E7490)
private val Roxo = Color(0xFF7C3AED)
private val OuroEscuro = Color(0xFFB7791F)
private val Verde = Color(0xFF22C55E)
private val Ambar = Color(0xFFF59E0B)
private val DouradoSino = Color(0xFFF2B630)

/** A tela inteira das Notificações. */
@Composable
internal fun NotificacoesDoApp() {
    // Uma mudança numa chave (ligar o prêmio secundário) relê os sons também.
    var versao by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuadroDosAvisos()
        TituloDoGrupo(GRUPO_O_QUE_AVISAR)
        OQueAvisar(versao) { versao++ }
        TituloDoGrupo(GRUPO_SONS)
        Sons(versao) { versao++ }
        Text(
            "Os avisos de \"Ação necessária\" (alguma pendência no bolão) chegam sempre que há algo a fazer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

/** O título de um grupo, como os dos Ajustes ("O QUE AVISAR"). */
@Composable
private fun TituloDoGrupo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 1.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 2.dp),
    )
}

/** Um quadro arredondado dos grupos. */
@Composable
private fun Quadro(conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = conteudo)
    }
}

@Composable
private fun Linha() {
    HorizontalDivider(Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * O quadro do alto: o sino dourado no azul-noite, "Avisos prontos" (ou "Falta liberar") e as três liberações do
 * celular, cada uma com o sinal (verde = ok; âmbar = falta) e o botão curto. A situação é lida de novo ao voltar dos
 * ajustes do Android.
 */
@Composable
private fun QuadroDosAvisos() {
    val ctx = LocalContext.current
    var conferido by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        conferido++
        onPauseOrDispose {}
    }
    val permitidos = remember(conferido) { Notificador.permitido(ctx) }
    val bateriaLivre = remember(conferido) { SegundoPlano.bateriaLivre(ctx) }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) Notificador.teste(ctx)
        conferido++
    }
    val pronto = permitidos && bateriaLivre
    val branco = Color.White
    val suave = Color(0xFFB9C6DA)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF13294B), Color(0xFF0B1626))))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(DouradoSino.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = DouradoSino, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (pronto) TUDO_PRONTO else FALTA_LIBERAR,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 26.sp),
                    fontWeight = FontWeight.Bold,
                    color = branco,
                )
                Text(
                    "O app avisa mesmo fechado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = suave,
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Liberacao(
                "Avisos", permitidos,
                acao = if (permitidos) "Testar" else "Permitir",
                detalhe = if (permitidos) "Permitidos" else "Desligados: o aviso do prêmio não chega",
            ) {
                when {
                    permitidos -> Notificador.teste(ctx)
                    android.os.Build.VERSION.SDK_INT >= 33 -> pedirPermissao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    else -> SegundoPlano.abrirAjustesDoApp(ctx)
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            Liberacao(
                "Bateria", bateriaLivre,
                acao = if (bateriaLivre) null else "Liberar",
                detalhe = if (bateriaLivre) "Sem restrição" else "Com restrição: a apuração pode atrasar",
            ) { SegundoPlano.pedirBateriaLivre(ctx) }
            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            // O app não tem como saber se a inicialização automática está ligada (é do HyperOS): sem o sinal.
            Liberacao("Inicialização automática", null, acao = "Abrir", detalhe = "No HyperOS, deixe ligada") { SegundoPlano.abrirInicioAutomatico(ctx) }
        }
    }
}

/** Uma liberação do celular no quadro do alto: o sinal ([ok]: verde, âmbar ou nenhum), o nome e o botão curto. */
@Composable
private fun Liberacao(nome: String, ok: Boolean?, acao: String?, detalhe: String? = null, aoTocar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        when (ok) {
            true -> Icon(Icons.Filled.CheckCircle, contentDescription = "Ok", tint = Verde, modifier = Modifier.size(20.dp))
            false -> Icon(Icons.Filled.WarningAmber, contentDescription = "Falta", tint = Ambar, modifier = Modifier.size(20.dp))
            null -> Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Color(0xFFB9C6DA), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            if (detalhe != null) Text(detalhe, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB9C6DA))
        }
        if (acao != null) {
            val destaque = ok == false
            Text(
                acao,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (destaque) Color(0xFF13294B) else DouradoSino,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (destaque) DouradoSino else Color.Transparent)
                    .clickable(onClick = aoTocar)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

/** Uma linha com chave: a linha toda liga e desliga (a chave acompanha). */
@Composable
private fun LinhaComChave(icone: ImageVector, cor: Color, titulo: String, subtitulo: String, ligado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = ligado, role = Role.Switch, onValueChange = aoMudar)
            .padding(vertical = 12.dp),
    ) {
        EmblemaIcone(icone, cor, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = ligado, onCheckedChange = null)
    }
}

/** "O que avisar": o prêmio principal (o toque como chamada), o secundário (o som próprio) e os concursos especiais. */
@Composable
private fun OQueAvisar(versao: Int, mudou: () -> Unit) {
    val ctx = LocalContext.current
    val principal = remember(versao) { Toque.config(ctx) }
    val secundario = remember(versao) { Toque.config(ctx, Toque.Qual.SECUNDARIO) }
    val especiais = remember(versao) { AvisoDeEspeciais.ligado(ctx) }
    Quadro {
        LinhaComChave(
            Icons.Outlined.RingVolume, RosaToque, AVISO_PREMIO_PRINCIPAL,
            if (principal.ligado) "Toca como chamada, ${Toque.DURACAO_MS / 1000} s, até no silencioso" else "Só o aviso, sem tocar",
            principal.ligado,
        ) {
            Toque.ligar(ctx, it)
            mudou()
        }
        Linha()
        LinhaComChave(
            Icons.Outlined.EmojiEvents, Turquesa, AVISO_PREMIO_SECUNDARIO,
            if (secundario.ligado) "Som próprio, uma vez" else "Som de notificação de sempre",
            secundario.ligado,
        ) {
            Toque.ligar(ctx, it, Toque.Qual.SECUNDARIO)
            mudou()
        }
        Linha()
        LinhaComChave(
            Icons.Filled.AutoAwesome, OuroEscuro, AVISO_CONCURSOS_ESPECIAIS,
            if (especiais) "Quando as apostas abrem e na véspera" else "Sem avisos dos especiais",
            especiais,
        ) {
            AvisoDeEspeciais.ligar(ctx, it)
            mudou()
        }
    }
}

/** Os sons: o toque do prêmio principal e o som do prêmio secundário, cada um com o "Testar" no fim da linha. */
@Composable
private fun Sons(versao: Int, mudou: () -> Unit) {
    val ctx = LocalContext.current
    val tocando by Toque.tocando.collectAsStateWithLifecycle()
    var escolhendo by remember { mutableStateOf<Toque.Qual?>(null) }
    // Saiu da tela no meio de um teste: para.
    DisposableEffect(Unit) {
        onDispose { if (Toque.tocando.value == Toque.Origem.TESTE || Toque.tocando.value == Toque.Origem.TESTE_SECUNDARIO) Toque.parar() }
    }
    val principal = remember(versao, tocando) { Toque.config(ctx) }
    val secundario = remember(versao, tocando) { Toque.config(ctx, Toque.Qual.SECUNDARIO) }
    Quadro {
        LinhaDeSom(
            titulo = SOM_DO_PRINCIPAL,
            nome = Toque.nomeDoToque(principal),
            ativo = true,
            testando = tocando == Toque.Origem.TESTE,
            descricaoDoTeste = TESTAR_O_TOQUE,
            origem = Toque.Origem.TESTE,
            aoEscolher = { escolhendo = Toque.Qual.PRINCIPAL },
        )
        Linha()
        LinhaDeSom(
            titulo = TITULO_DO_SOM_SECUNDARIO,
            nome = if (secundario.ligado) Toque.nomeDoToque(secundario, Toque.Qual.SECUNDARIO) else "Ligue o prêmio secundário para escolher",
            ativo = secundario.ligado,
            testando = tocando == Toque.Origem.TESTE_SECUNDARIO,
            descricaoDoTeste = TESTAR_O_SOM,
            origem = Toque.Origem.TESTE_SECUNDARIO,
            aoEscolher = { escolhendo = Toque.Qual.SECUNDARIO },
        )
    }
    escolhendo?.let { qual ->
        EscolherOSom(qual, aoFechar = {
            escolhendo = null
            mudou()
        })
    }
}

/** Uma linha de som: a nota, o nome do som escolhido e o botão redondo de testar (tocar de novo para). */
@Composable
private fun LinhaDeSom(
    titulo: String,
    nome: String,
    ativo: Boolean,
    testando: Boolean,
    descricaoDoTeste: String,
    origem: Toque.Origem,
    aoEscolher: () -> Unit,
) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (ativo) Modifier.clickable(onClick = aoEscolher) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        EmblemaIcone(Icons.Outlined.MusicNote, if (ativo) Roxo else MaterialTheme.colorScheme.outline, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = if (ativo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(nome, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = {
                if (testando) {
                    Toque.parar()
                } else if (!Toque.tocar(ctx, origem)) {
                    escopo.launch { avisos.showSnackbar("Não foi possível tocar: escolha outro som.") }
                }
            },
            enabled = ativo,
            modifier = Modifier.size(44.dp).semantics { contentDescription = if (testando) "Parar" else descricaoDoTeste },
        ) {
            Icon(if (testando) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, contentDescription = null)
        }
    }
}

/**
 * A janela de escolha do som ([qual]: o toque do prêmio principal ou o som do secundário): uma música do celular
 * (um arquivo de áudio), um toque (ou som) do celular, ou voltar ao de sempre.
 */
@Composable
private fun EscolherOSom(qual: Toque.Qual, aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val principal = qual == Toque.Qual.PRINCIPAL
    val c = remember { Toque.config(ctx, qual) }
    val padrao = if (principal) Settings.System.DEFAULT_RINGTONE_URI else Settings.System.DEFAULT_NOTIFICATION_URI
    val escolherMusica = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) Toque.escolherArquivo(ctx, uri, qual)
        aoFechar()
    }
    val escolherDoCelular = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.let { IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) }
        if (r.resultCode == Activity.RESULT_OK && uri != null) {
            if (uri == padrao) Toque.escolher(ctx, null, null, qual) else Toque.escolherToqueDoSistema(ctx, uri, qual)
        }
        aoFechar()
    }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (principal) SOM_DO_PRINCIPAL else TITULO_DO_SOM_SECUNDARIO) },
        text = {
            Column {
                Text(
                    "Agora: ${Toque.nomeDoToque(c, qual)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OpcaoDeSom(Icons.Outlined.LibraryMusic, "Uma música do celular") { escolherMusica.launch(arrayOf("audio/*")) }
                OpcaoDeSom(Icons.Outlined.NotificationsActive, if (principal) "Um toque do celular" else "Um som do celular") {
                    val pedido = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, if (principal) RingtoneManager.TYPE_ALL else RingtoneManager.TYPE_NOTIFICATION)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, if (principal) SOM_DO_PRINCIPAL else TITULO_DO_SOM_SECUNDARIO)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, padrao)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, c.uri ?: padrao)
                    try {
                        escolherDoCelular.launch(pedido)
                    } catch (t: ActivityNotFoundException) {
                        escopo.launch { avisos.showSnackbar("Este celular não abriu a lista de sons: escolha uma música.") }
                    }
                }
                if (c.uri != null) {
                    OpcaoDeSom(Icons.Outlined.Restore, if (principal) "Voltar ao toque de chamada" else "Voltar ao som de notificação") {
                        Toque.escolher(ctx, null, null, qual)
                        aoFechar()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
    )
}

@Composable
private fun OpcaoDeSom(icone: ImageVector, texto: String, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = aoTocar).padding(vertical = 12.dp, horizontal = 4.dp),
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(2.dp))
}

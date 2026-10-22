package io.github.marcosfaria94.bolao.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.net.Atualizador
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.ui.atualizacao.AtualizacaoViewModel
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.DialogoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.TituloSecao
import io.github.marcosfaria94.bolao.ui.comum.copiar
import io.github.marcosfaria94.bolao.ui.teste.CartaoModoTeste
import io.github.marcosfaria94.bolao.ui.teste.DialogoSairDoTeste
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@Composable
fun AjustesTela(estado: Estado, atualizacao: AtualizacaoViewModel, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val backup = remember { ctx.bolao.backup }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val ultima by atualizacao.ultima.collectAsStateWithLifecycle()
    val teste by repo.modoTeste.collectAsStateWithLifecycle()
    var sairDoTeste by remember { mutableStateOf(false) }

    var editandoAporte by remember { mutableStateOf(false) }
    var editandoPreco by remember { mutableStateOf<Modalidade?>(null) }
    var confirmandoRestauracao by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }
    var pedindoExtincao by remember { mutableStateOf(false) }
    // Extinção forçada: destravada pelos toques no quadro da extinção (vale enquanto a tela está aberta).
    var forcadaLiberada by remember { mutableStateOf(false) }

    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) Notificador.teste(ctx)
    }

    // Os dois anexos do termo: o sem assinaturas e o assinado pelos 3.
    val escolherTermoAssinado = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            try {
                val arq = repo.guardarArquivo(uri, "termo-assinado")
                repo.atualizar { it.copy(termo = arq) }
                repo.limparArquivosSoltos()
                avisos.showSnackbar("Termo assinado guardado.")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível guardar o arquivo: ${t.message}")
            }
        }
    }
    val escolherTermoSemAssinatura = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            try {
                val arq = repo.guardarArquivo(uri, "termo-sem-assinatura")
                repo.atualizar { it.copy(termoSemAssinatura = arq) }
                repo.limparArquivosSoltos()
                avisos.showSnackbar("Termo sem assinaturas guardado.")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível guardar o arquivo: ${t.message}")
            }
        }
    }

    val escolherBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            trabalhando = true
            try {
                val entrada = ctx.contentResolver.openInputStream(uri) ?: error("Não foi possível abrir o arquivo.")
                val r = entrada.use { backup.restaurar(it) }
                avisos.showSnackbar("Backup restaurado: ${r.estado.apostas.size} jogos e ${r.anexos} anexos.")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível restaurar: ${t.message}")
            } finally {
                trabalhando = false
            }
        }
    }

    LazyColumn(contentPadding = PaddingTela, modifier = Modifier.fillMaxWidth()) {
        item { TituloSecao("Integrantes") }
        estado.pessoas.forEach { p ->
            item(key = "pessoa-${p.id}") {
                ListItem(
                    headlineContent = { Text(if (p.administrador) "${p.apelido} (Administrador)" else p.apelido) },
                    supportingContent = { Text("${p.nome}\nPix: ${p.chavePix}") },
                    trailingContent = {
                        IconButton(onClick = { copiar(ctx, "Chave Pix", p.chavePix) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar chave Pix")
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Valores") }
        item {
            ListItem(
                headlineContent = { Text("Aporte mensal por integrante") },
                supportingContent = { Text("Total do mês: ${Dinheiro.rs(estado.config.aporteMensal * estado.pessoas.size.coerceAtLeast(1))}") },
                trailingContent = { Text(Dinheiro.rs(estado.config.aporteMensal), style = MaterialTheme.typography.titleMedium) },
                modifier = Modifier.clickable { editandoAporte = true },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        item {
            Text(
                "Preço da aposta simples (toque para mudar)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            )
        }
        Modalidade.entries.filter { !it.precoVariavel }.forEach { m ->
            item(key = "preco-${m.name}") {
                ListItem(
                    leadingContent = { SeloModalidade(m, tamanho = 32) },
                    headlineContent = { Text(m.nome) },
                    trailingContent = { Text(Dinheiro.rs(estado.config.preco(m))) },
                    modifier = Modifier.clickable { editandoPreco = m },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Termo") }
        item {
            AnexoDoTermo(
                titulo = "Termo sem assinaturas",
                arquivo = estado.termoSemAssinatura,
                aoAnexar = { escolherTermoSemAssinatura.launch(arrayOf("application/pdf", "image/*")) },
            )
        }
        item {
            AnexoDoTermo(
                titulo = "Termo assinado",
                arquivo = estado.termo,
                aoAnexar = { escolherTermoAssinado.launch(arrayOf("application/pdf", "image/*")) },
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Integridade") }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
                headlineContent = { Text("Verificar integridade") },
                supportingContent = { Text("Confere a autenticação de todos os comprovantes, os anexos originais e um código de comprovante.") },
                modifier = Modifier.clickable { abrir("integridade") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Backup") }
        item {
            Text(
                "O backup leva tudo: jogos, lançamentos, comprovantes e o termo. " +
                    "Fica em Downloads/Bolao e dá para enviar ao Drive ou por e-mail.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                OutlinedButton(
                    enabled = !trabalhando,
                    onClick = {
                        escopo.launch {
                            trabalhando = true
                            try {
                                val uri = backup.salvarEmDownloads()
                                val r = avisos.showSnackbar("Backup salvo em Downloads/Bolao.", actionLabel = "Enviar")
                                if (r == SnackbarResult.ActionPerformed) {
                                    val enviar = Intent(Intent.ACTION_SEND)
                                        .setType("application/zip")
                                        .putExtra(Intent.EXTRA_STREAM, uri)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    ctx.startActivity(Intent.createChooser(enviar, "Enviar backup"))
                                }
                            } catch (t: Throwable) {
                                avisos.showSnackbar("Não foi possível fazer o backup: ${t.message}")
                            } finally {
                                trabalhando = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Backup, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Fazer backup")
                }
                OutlinedButton(
                    enabled = !trabalhando,
                    onClick = { confirmandoRestauracao = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restaurar")
                }
            }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Atualização") }
        item {
            val nova = ultima?.takeIf { it.codigo > Atualizador.versaoInstalada }
            ListItem(
                leadingContent = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                headlineContent = { Text("Versão ${Atualizador.nomeInstalado}") },
                supportingContent = { Text(if (nova != null) "Versão ${nova.nome} disponível" else "Atualiza pelo GitHub com um toque") },
                trailingContent = {
                    OutlinedButton(onClick = {
                        if (nova != null) atualizacao.atualizar(nova) else atualizacao.verificar(manual = true)
                    }) { Text(if (nova != null) "Atualizar" else "Verificar") }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Notificações no HyperOS") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "No Poco, os avisos (ação necessária e aposta premiada) só chegam na hora certa se o app puder iniciar sozinho " +
                                "e não for limitado pela economia de bateria.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Em Ajustes do app: ative \"Inicialização automática\" e, em \"Economia de bateria\", escolha \"Sem restrições\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }) { Text("Ajustes do app") }
                        OutlinedButton(onClick = {
                            if (Notificador.permitido(ctx)) {
                                Notificador.teste(ctx)
                            } else if (android.os.Build.VERSION.SDK_INT >= 33) {
                                pedirPermissao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) { Text(if (Notificador.permitido(ctx)) "Testar aviso" else "Permitir avisos") }
                    }
                }
            }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Modo de teste") }
        item { CartaoModoTeste(emTeste = teste, aoPedirSaida = { sairDoTeste = true }) }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { TituloSecao("Extinção do Bolão") }
        item {
            CartaoExtincao(
                estado,
                forcadaLiberada = forcadaLiberada,
                aoLiberarForcada = { forcadaLiberada = true },
                aoPedir = { pedindoExtincao = true },
            )
        }
    }

    if (pedindoExtincao) {
        DialogoExtincao(
            estado,
            forcada = forcadaLiberada && Operacoes.impedimentosDaExtincao(estado).isNotEmpty(),
            aoFechar = { pedindoExtincao = false },
        )
    }

    if (sairDoTeste) DialogoSairDoTeste(aoFechar = { sairDoTeste = false })

    if (editandoAporte) {
        DialogoDinheiro(
            titulo = "Aporte mensal",
            inicial = estado.config.aporteMensal,
            rotulo = "Valor por integrante",
            explicacao = "Mudanças no aporte precisam da concordância dos 3 integrantes, registrada no grupo.",
            aoConfirmar = { v ->
                editandoAporte = false
                if (v > 0) escopo.launch { repo.atualizar { it.copy(config = it.config.copy(aporteMensal = v)) } }
            },
            aoFechar = { editandoAporte = false },
        )
    }

    editandoPreco?.let { m ->
        DialogoDinheiro(
            titulo = m.nome,
            inicial = estado.config.preco(m),
            rotulo = "Preço da aposta simples",
            aoConfirmar = { v ->
                editandoPreco = null
                if (v > 0) escopo.launch {
                    repo.atualizar { it.copy(config = it.config.copy(precos = it.config.precos + (m.name to v))) }
                }
            },
            aoFechar = { editandoPreco = null },
        )
    }

    if (confirmandoRestauracao) {
        DialogoConfirmar(
            titulo = "Restaurar backup?",
            texto = "Todos os dados atuais serão trocados pelos do backup escolhido. Se quiser guardar os atuais, faça um backup antes.",
            confirmar = "Escolher arquivo",
            aoConfirmar = {
                confirmandoRestauracao = false
                escolherBackup.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            },
            aoFechar = { confirmandoRestauracao = false },
        )
    }
}

/** Um dos dois anexos do termo: ver e anexar (ou trocar). */
@Composable
private fun AnexoDoTermo(titulo: String, arquivo: Arquivo?, aoAnexar: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    ListItem(
        leadingContent = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) },
        headlineContent = { Text(titulo) },
        supportingContent = {
            Text(
                arquivo?.let {
                    "${it.nomeOriginal} · anexado " +
                        Datas.quando(Instant.ofEpochMilli(it.guardadoEm).atZone(ZoneId.systemDefault()).toLocalDate(), java.time.LocalDate.now())
                } ?: "Nenhum arquivo anexado.",
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (arquivo != null) {
                    IconButton(onClick = { Compartilhar.abrir(ctx, repo.arquivo(arquivo), arquivo.mime) }) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Ver $titulo")
                    }
                }
                OutlinedButton(onClick = aoAnexar) { Text(if (arquivo == null) "Anexar" else "Trocar") }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

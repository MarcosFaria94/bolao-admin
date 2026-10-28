package io.github.marcosfaria94.bolao.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.net.Atualizador
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.pdf.AuditoriaPdf
import io.github.marcosfaria94.bolao.pdf.PdfsDoApp
import io.github.marcosfaria94.bolao.ui.atualizacao.AtualizacaoViewModel
import io.github.marcosfaria94.bolao.ui.comum.CampoData
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.CoresEmblema
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.DialogoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.Secao
import io.github.marcosfaria94.bolao.ui.comum.TarjaTitulo
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.copiar
import io.github.marcosfaria94.bolao.ui.tema.CoresAlerta
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import io.github.marcosfaria94.bolao.ui.teste.CartaoModoTeste
import io.github.marcosfaria94.bolao.ui.teste.DialogoSairDoTeste
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (estado.individual) {
            item {
                Secao("Modo", Modifier.padding(top = 8.dp)) {
                    LinhaAjuste(
                        emblema = { EmblemaIcone(Icons.Outlined.Person, CoresEmblema.APORTE, TAMANHO_EMBLEMA) },
                        titulo = "Individual",
                        subtitulo = "Só as apostas e os prêmios, sem comprovantes. O saldo começa em zero e mostra quanto se gastou ou ganhou.",
                    )
                }
            }
        }
        if (!estado.individual) item {
            Secao("Integrantes", Modifier.padding(top = 8.dp)) {
                estado.pessoas.forEachIndexed { i, p ->
                    // Sem as bolas das iniciais: a linha entre um e outro vai de ponta a ponta.
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ItemIntegrante(p) { copiar(ctx, "Chave Pix", p.chavePix) }
                }
            }
        }

        if (!estado.individual) item {
            Secao("Aporte mensal") {
                LinhaAjuste(
                    emblema = { EmblemaDesenho(Desenhos.Cofrinho, TAMANHO_EMBLEMA) },
                    titulo = "Por integrante",
                    subtitulo = "Total do mês: ${Dinheiro.rs(estado.config.aporteMensal * estado.pessoas.size.coerceAtLeast(1))}",
                    aoTocar = { editandoAporte = true },
                    final = {
                        Text(Dinheiro.rs(estado.config.aporteMensal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    },
                )
            }
        }

        item {
            Secao(
                "Preço da aposta simples",
                acao = { Text("Toque para mudar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            ) {
                GradePrecos(estado) { editandoPreco = it }
            }
        }

        if (!estado.individual) item {
            Secao("Termo") {
                AnexoDoTermo(
                    titulo = "Termo sem assinaturas",
                    icone = Icons.Outlined.Description,
                    cor = CoresEmblema.RATEIO,
                    arquivo = estado.termoSemAssinatura,
                    aoAnexar = { escolherTermoSemAssinatura.launch(arrayOf("application/pdf", "image/*")) },
                )
                Divisoria()
                AnexoDoTermo(
                    titulo = "Termo assinado",
                    icone = Icons.Outlined.HistoryEdu,
                    cor = CoresEmblema.EXTRATO,
                    arquivo = estado.termo,
                    aoAnexar = { escolherTermoAssinado.launch(arrayOf("application/pdf", "image/*")) },
                )
            }
        }

        if (!estado.individual) item {
            Secao("Integridade") {
                LinhaAjuste(
                    emblema = { EmblemaIcone(Icons.Outlined.VerifiedUser, CoresEmblema.APORTE, TAMANHO_EMBLEMA) },
                    titulo = "Verificar integridade",
                    subtitulo = "Confere a autenticação de todos os comprovantes, os anexos originais e um código de comprovante.",
                    aoTocar = { abrir("integridade") },
                    final = { Seta() },
                )
            }
        }

        if (!estado.individual) item {
            Secao("Auditoria") { Auditoria(estado) }
        }

        item {
            Secao("Backup") {
                LinhaAjuste(
                    emblema = { EmblemaIcone(Icons.Outlined.Backup, CoresEmblema.RATEIO, TAMANHO_EMBLEMA) },
                    titulo = "Cópia de tudo",
                    subtitulo = "Jogos, lançamentos, comprovantes e o termo. Fica em Downloads/Bolao e dá para enviar ao Drive ou por e-mail.",
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Fazer backup", maxLines = 1)
                    }
                    OutlinedButton(
                        enabled = !trabalhando,
                        onClick = { confirmandoRestauracao = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restaurar", maxLines = 1)
                    }
                }
            }
        }

        item {
            val nova = ultima?.takeIf { it.codigo > Atualizador.versaoInstalada }
            Secao("Atualização") {
                LinhaAjuste(
                    emblema = { EmblemaIcone(Icons.Outlined.SystemUpdate, AzulMarinhoEmblema, TAMANHO_EMBLEMA) },
                    titulo = "Versão ${Atualizador.nomeInstalado}",
                    subtitulo = if (nova != null) "Versão ${nova.nome} disponível" else "Atualiza pelo GitHub com um toque",
                    final = {
                        if (nova != null) {
                            Button(onClick = { atualizacao.atualizar(nova) }) { Text("Atualizar") }
                        } else {
                            OutlinedButton(onClick = { atualizacao.verificar(manual = true) }) { Text("Verificar") }
                        }
                    },
                )
            }
        }

        item {
            Secao("Notificações no HyperOS") {
                LinhaAjuste(
                    emblema = { EmblemaIcone(Icons.Outlined.NotificationsActive, CoresEmblema.PREMIO, TAMANHO_EMBLEMA) },
                    titulo = "Avisos na hora certa",
                    subtitulo = "Os avisos (ação necessária e aposta premiada) só chegam na hora se o app puder iniciar sozinho " +
                        "e não for limitado pela economia de bateria.",
                )
                Text(
                    "Em Ajustes do app: ative \"Inicialização automática\" e, em \"Economia de bateria\", escolha \"Sem restrições\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = RECUO_TEXTO),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) { TextoQueCabe("Ajustes do app", MaterialTheme.typography.labelLarge, menor = 11.sp) }
                    OutlinedButton(
                        onClick = {
                            if (Notificador.permitido(ctx)) {
                                Notificador.teste(ctx)
                            } else if (android.os.Build.VERSION.SDK_INT >= 33) {
                                pedirPermissao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        TextoQueCabe(
                            if (Notificador.permitido(ctx)) "Testar aviso" else "Permitir avisos",
                            MaterialTheme.typography.labelLarge,
                            menor = 11.sp,
                        )
                    }
                }
            }
        }

        item {
            Secao("Modo de teste") {
                CartaoModoTeste(emTeste = teste, aoPedirSaida = { sairDoTeste = true })
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (estado.individual) "Apagar tudo" else "Extinção do Bolão", style = MaterialTheme.typography.titleMedium)
                CartaoExtincao(
                    estado,
                    forcadaLiberada = forcadaLiberada || estado.individual,
                    aoLiberarForcada = { forcadaLiberada = true },
                    aoPedir = { pedindoExtincao = true },
                )
            }
        }
    }

    if (pedindoExtincao) {
        DialogoExtincao(
            estado,
            forcada = forcadaLiberada || estado.individual,
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

/** Tamanho dos emblemas das linhas dos Ajustes. */
private val TAMANHO_EMBLEMA = 44.dp

/** Onde começa o texto das linhas (depois do emblema): botões e dicas ficam alinhados com ele. */
private val RECUO_TEXTO = 58.dp

/** Azul do emblema da atualização (o mesmo nos dois temas, legível no escuro). */
private val AzulMarinhoEmblema = Color(0xFF2F4A7A)

@Composable
private fun Divisoria() {
    HorizontalDivider(Modifier.padding(start = RECUO_TEXTO), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun Seta() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Uma linha dos Ajustes: o emblema colorido, o título e o texto de apoio e, no fim, o valor ou o
 * botão ([final]). Com [aoTocar], a linha inteira é um botão.
 */
@Composable
private fun LinhaAjuste(
    emblema: @Composable () -> Unit,
    titulo: String,
    subtitulo: String? = null,
    aoTocar: (() -> Unit)? = null,
    final: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        emblema()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (final != null) {
            Spacer(Modifier.width(8.dp))
            final()
        }
    }
}

/** Um integrante: o apelido (com a marca do Administrador), o nome e a chave Pix, com o copiar no fim. */
@Composable
private fun ItemIntegrante(p: Pessoa, aoCopiar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.apelido,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (p.administrador) {
                    Spacer(Modifier.width(8.dp))
                    TarjaTitulo("Administrador", fundo = corDestaque().copy(alpha = 0.16f), cor = corDestaque())
                }
            }
            Text(
                p.nome,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Pix: ${p.chavePix}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = aoCopiar) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar chave Pix", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Auditoria (antes ficava no Extrato): o PDF com todos os comprovantes do período e os anexos
 * originais, em ordem cronológica, salvo em Downloads/Bolao.
 */
@Composable
private fun Auditoria(estado: Estado) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var de by remember { mutableStateOf(PdfsDoApp.inicio(estado)) }
    var ate by remember { mutableStateOf(LocalDate.now()) }
    var gerando by remember { mutableStateOf(false) }
    var progresso by remember { mutableFloatStateOf(0f) }
    var pronto by remember { mutableStateOf<Pair<Uri, AuditoriaPdf.Resultado>?>(null) }

    LinhaAjuste(
        emblema = { EmblemaIcone(Icons.Outlined.PictureAsPdf, CoresEmblema.EXTRATO, TAMANHO_EMBLEMA) },
        titulo = "PDF da auditoria",
        subtitulo = "Todos os comprovantes (aporte, aposta, apuração, rateio e extrato) e os anexos originais, em ordem cronológica.",
    )
    Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Um embaixo do outro: lado a lado, a data inteira não cabe no cartão.
        CampoData(data = de, aoMudar = { de = it }, rotulo = "De", modifier = Modifier.fillMaxWidth())
        CampoData(data = ate, aoMudar = { ate = it }, rotulo = "Até", modifier = Modifier.fillMaxWidth())
        if (gerando) LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth())
        Button(
            enabled = !gerando && !de.isAfter(ate),
            onClick = {
                gerando = true
                progresso = 0f
                escopo.launch {
                    try {
                        pronto = PdfsDoApp.auditoriaEmDownloads(ctx, estado, de, ate) { feitos, total ->
                            progresso = if (total == 0) 1f else feitos / total.toFloat()
                        }
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível gerar o PDF: ${t.message}")
                    } finally {
                        gerando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (gerando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Gerar PDF")
        }
    }

    pronto?.let { (uri, r) ->
        AlertDialog(
            onDismissRequest = { pronto = null },
            title = { Text("Auditoria pronta") },
            text = {
                Text(
                    "Salva em Downloads/Bolao: ${r.comprovantes} comprovante(s), ${r.anexos} anexo(s), ${r.paginas} página(s)." +
                        if (r.problemas.isEmpty()) "\n\nTudo confere." else "\n\nAtenção:\n" + r.problemas.joinToString("\n") { "• $it" },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pronto = null
                    Compartilhar.enviarArquivo(ctx, uri, "application/pdf", "Enviar a auditoria")
                }) { Text("Enviar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pronto = null }) { Text("Fechar") }
                    TextButton(onClick = {
                        pronto = null
                        Compartilhar.abrir(ctx, uri, "application/pdf")
                    }) { Text("Abrir") }
                }
            },
        )
    }
}

/** Colunas da tabela de preços (10 modalidades = 4 + 4 + 2, a última linha no meio). */
private const val COLUNAS_PRECOS = 4

/** Tabela de preços: a bola de cada modalidade, o nome e o preço embaixo; tocar muda o preço. */
@Composable
private fun GradePrecos(estado: Estado, aoEscolher: (Modalidade) -> Unit) {
    val lista = Modalidade.entries.filter { !it.precoVariavel }
    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lista.chunked(COLUNAS_PRECOS).forEach { linha ->
            Row(Modifier.fillMaxWidth()) {
                // Linha incompleta: as bolas ficam no meio, alinhadas com as colunas de cima.
                val sobra = (COLUNAS_PRECOS - linha.size) / 2f
                if (sobra > 0f) Spacer(Modifier.weight(sobra))
                linha.forEach { m ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { aoEscolher(m) }
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                    ) {
                        EmblemaModalidade(m, concurso = null, tamanho = 48.dp)
                        Spacer(Modifier.height(6.dp))
                        TextoQueCabe(
                            m.nome,
                            MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                            cor = MaterialTheme.colorScheme.onSurfaceVariant,
                            menor = 9.sp,
                        )
                        TextoQueCabe(
                            Dinheiro.rs(estado.config.preco(m)),
                            MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            peso = FontWeight.Bold,
                            menor = 11.sp,
                        )
                    }
                }
                if (sobra > 0f) Spacer(Modifier.weight(sobra))
            }
        }
    }
}

/** Um dos dois anexos do termo: o arquivo anexado (ou a falta dele) e, embaixo, ver e anexar (ou trocar). */
@Composable
private fun AnexoDoTermo(titulo: String, icone: ImageVector, cor: Color, arquivo: Arquivo?, aoAnexar: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemaIcone(icone, cor, TAMANHO_EMBLEMA)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                if (arquivo != null) {
                    Text(
                        "${arquivo.nomeOriginal} · anexado " +
                            Datas.quando(Instant.ofEpochMilli(arquivo.guardadoEm).atZone(ZoneId.systemDefault()).toLocalDate(), java.time.LocalDate.now()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        "Nenhum arquivo anexado.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CoresAlerta.texto(),
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = RECUO_TEXTO, top = 8.dp),
        ) {
            if (arquivo != null) {
                OutlinedButton(
                    onClick = { Compartilhar.abrir(ctx, repo.arquivo(arquivo), arquivo.mime) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver")
                }
            }
            OutlinedButton(onClick = aoAnexar, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (arquivo == null) "Anexar" else "Trocar")
            }
        }
    }
}

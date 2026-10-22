package io.github.marcosfaria94.bolao.ui.extincao

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Downloads
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.pdf.PdfsDoApp
import io.github.marcosfaria94.bolao.seguranca.Biometria
import io.github.marcosfaria94.bolao.seguranca.atividade
import io.github.marcosfaria94.bolao.ui.comum.AVISO_DEFINITIVO
import io.github.marcosfaria94.bolao.ui.comum.Anexo
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BotoesDeRegistro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LinhaAnexo
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.copiar
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Extinção do bolão (Parte 5 do termo). Tela que não deixa sair: anexar os Pix da divisão
 * final, registrar, gerar o PDF da auditoria completa e enviá-lo ao grupo. Depois disso,
 * tudo é apagado e o app volta ao cadastro de um novo grupo.
 */
@Composable
fun ExtincaoTela(estado: Estado) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val x = estado.extincao ?: return
    val hoje = LocalDate.now()
    val divisao = Operacoes.divisaoFinal(estado)
    // Só na extinção forçada: sem saldo, não há divisão nem Pix (vai direto para o PDF).
    // Depois de registrada, a divisão deixa o saldo zerado: aí vale se houve o comprovante dela.
    val semDivisao = if (x.registradaEm == null) divisao.saldo <= 0 else estado.comprovantes.none { it.tipo == TipoComprovante.EXTINCAO }

    var paraQuem by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }
    var perguntarSeEnviou by remember { mutableStateOf(false) }
    var confirmarApagar by remember { mutableStateOf(false) }
    var confirmarSemDivisao by remember { mutableStateOf(false) }

    // Não há como voltar: a extinção vai até o fim.
    BackHandler(enabled = true) {}

    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pessoa = paraQuem
        if (uri != null && pessoa != null) escopo.launch {
            try {
                val arq = repo.guardarArquivo(uri, "pix-final")
                repo.atualizar { Operacoes.anexarNaExtincao(it, pessoa, arq) }
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível anexar: ${t.message}")
            }
        }
    }

    // Depois de registrar a divisão: o PDF da auditoria completa é gerado na hora.
    LaunchedEffect(x.registradaEm, x.auditoria) {
        if (x.registradaEm != null && x.auditoria == null && !trabalhando) {
            trabalhando = true
            try {
                val e = repo.atual()
                val inicio = PdfsDoApp.inicio(e)
                val nome = PdfsDoApp.nomeAuditoria(e, inicio, hoje).replace("auditoria-", "auditoria-final-")
                val destino = File(repo.pastaArquivos, nome)
                PdfsDoApp.auditoriaEmArquivo(ctx, e, inicio, hoje, destino)
                // Uma cópia fica em Downloads/Bolao: o app vai apagar tudo no fim.
                runCatching { Downloads.salvar(ctx, nome, "application/pdf") { saida -> destino.inputStream().use { it.copyTo(saida) } } }
                val arq = Arquivo(nome, nome, "application/pdf", System.currentTimeMillis())
                repo.atualizar { it.copy(extincao = it.extincao?.copy(auditoria = arq)) }
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível gerar o PDF: ${t.message}")
            } finally {
                trabalhando = false
            }
        }
    }

    // Ao voltar do WhatsApp, pergunta se o PDF foi enviado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (x.auditoriaEnviadaEm != null) perguntarSeEnviou = true
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Extinção do Bolão", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Text(
                "Iniciada ${Datas.quandoHora(x.iniciadaEm, hoje)}. Esta tela só fecha quando a divisão final for registrada e o PDF da auditoria for enviado ao grupo.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (x.forcada) {
                Aviso("Extinção forçada: as pendências do bolão ficaram como estavam.")
            }
            if (!semDivisao) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Divisão final do saldo", style = MaterialTheme.typography.titleMedium)
                    LinhaValor("Saldo líquido", Dinheiro.rs(divisao.saldo))
                    LinhaValor("Cota de cada integrante (÷ ${estado.pessoas.size}, para cima)", Dinheiro.rs(divisao.cota), destaque = true)
                    if (divisao.complemento > 0) LinhaValor("Complemento do Administrador", Dinheiro.rs(divisao.complemento))
                    LinhaValor("Total transferido", Dinheiro.rs(divisao.total))
                }
            }

            if (x.registradaEm == null && semDivisao) {
                Aviso("Não há saldo para dividir: não há Pix a fazer. A extinção segue direto para o PDF da auditoria.")
                Button(
                    onClick = { confirmarSemDivisao = true },
                    enabled = !trabalhando,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Gerar o PDF da auditoria") }
            } else if (x.registradaEm == null) {
                if (estado.exemplo) {
                    TextButton(onClick = {
                        escopo.launch {
                            try {
                                estado.pessoas.forEach { p ->
                                    val arq = ModoTeste.pix(ctx, p, divisao.cota)
                                    repo.atualizar { Operacoes.anexarNaExtincao(it, p.id, arq) }
                                }
                            } catch (t: Throwable) {
                                avisos.showSnackbar("Não foi possível criar os comprovantes de exemplo: ${t.message}")
                            }
                        }
                    }) { Text("Usar comprovantes de Pix de exemplo") }
                }
                estado.pessoas.forEachIndexed { i, p ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Pix ${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${p.apelido} · ${Dinheiro.rs(divisao.cota)}", style = MaterialTheme.typography.titleMedium)
                                    Text("Chave: ${p.chavePix}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { copiar(ctx, "Chave Pix", p.chavePix) }) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar chave Pix")
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            val arq = x.comprovantes[p.id]
                            LinhaAnexo(
                                titulo = "Comprovante do Pix",
                                nomeArquivo = arq?.nomeOriginal,
                                aoAnexar = {
                                    paraQuem = p.id
                                    escolher.launch(arrayOf("application/pdf", "image/*"))
                                },
                                arquivo = arq?.let { repo.arquivo(it) },
                                mime = arq?.mime,
                            )
                        }
                    }
                }
                val faltam = estado.pessoas.count { x.comprovantes[it.id] == null }
                if (faltam > 0) Aviso("Falta anexar $faltam comprovante(s) do Pix da divisão final.", alerta = true)
                BotoesDeRegistro(
                    habilitado = faltam == 0,
                    trabalhando = trabalhando,
                    aoVisualizar = { vendo = true },
                    aoRegistrar = { confirmar = true },
                    textoRegistrar = "Registrar a divisão final",
                )
            } else {
                val quando = Instant.ofEpochMilli(x.registradaEm).atZone(ZoneId.systemDefault()).toLocalDate()
                Aviso(if (semDivisao) "Extinção registrada ${Datas.quando(quando, hoje)}, sem saldo para dividir." else "Divisão final registrada ${Datas.quando(quando, hoje)}.")
                val pdf = x.auditoria
                if (pdf == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("  Gerando o PDF da auditoria completa...")
                    }
                } else {
                    Text(
                        "O PDF da auditoria completa está pronto (uma cópia ficou em Downloads/Bolao). Envie ao grupo para encerrar.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = { Compartilhar.abrir(ctx, repo.arquivo(pdf), pdf.mime) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Abrir o PDF")
                    }
                    Button(
                        onClick = {
                            val titulo = estado.comprovantes.lastOrNull { it.tipo == TipoComprovante.EXTINCAO }?.titulo ?: Mensagens.tituloExtincao(hoje)
                            if (Compartilhar.whatsapp(ctx, titulo, listOf(Anexo(repo.arquivo(pdf), pdf.mime)))) {
                                escopo.launch { repo.atualizar { it.copy(extincao = it.extincao?.copy(auditoriaEnviadaEm = System.currentTimeMillis())) } }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Enviar o PDF ao grupo") }
                    if (x.auditoriaEnviadaEm != null) {
                        Button(
                            onClick = { confirmarApagar = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Concluir: apagar tudo e recomeçar") }
                    }
                }
            }
            SnackbarHost(avisos)
        }
    }

    if (vendo) {
        VisualizadorComprovante(
            estado,
            Mensagens.tituloExtincao(hoje),
            aoFechar = { vendo = false },
            previa = Recibos.previa(estado, TipoComprovante.EXTINCAO, Operacoes.textoExtincao(estado), x.comprovantes.size),
            anexos = estado.pessoas.mapNotNull { x.comprovantes[it.id] },
        )
    }

    if (confirmar) {
        DialogoConfirmar(
            titulo = "Registrar a divisão final?",
            texto = "Sai ${Dinheiro.rs(divisao.total)} do saldo (${Dinheiro.rs(divisao.cota)} para cada um)" +
                (if (divisao.complemento > 0) ", com ${Dinheiro.rs(divisao.complemento)} de complemento do Administrador" else "") +
                ". Depois o app gera o PDF da auditoria completa.\n\n$AVISO_DEFINITIVO",
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmar = false
                trabalhando = true
                escopo.launch {
                    try {
                        val agora = System.currentTimeMillis()
                        registrarComprovante(ctx, { Operacoes.registrarExtincao(it, hoje, agora) }, enviar = false)
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível registrar: ${t.message}")
                    } finally {
                        trabalhando = false
                    }
                }
            },
            aoFechar = { confirmar = false },
        )
    }

    if (confirmarSemDivisao) {
        DialogoConfirmar(
            titulo = "Encerrar sem divisão?",
            texto = "Não há saldo para dividir. O app gera agora o PDF da auditoria completa, para enviar ao grupo.\n\n$AVISO_DEFINITIVO",
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmarSemDivisao = false
                escopo.launch {
                    try {
                        repo.atualizar { Operacoes.encerrarSemDivisao(it, System.currentTimeMillis()) }
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível encerrar: ${t.message}")
                    }
                }
            },
            aoFechar = { confirmarSemDivisao = false },
        )
    }

    if (perguntarSeEnviou && !confirmarApagar) {
        DialogoConfirmar(
            titulo = "O PDF foi enviado ao grupo?",
            texto = "Se sim, o app apaga todos os dados deste bolão e volta ao cadastro de um novo grupo. " +
                "O PDF da auditoria continua em Downloads/Bolao.",
            confirmar = "Confirmar",
            aoConfirmar = {
                perguntarSeEnviou = false
                confirmarApagar = true
            },
            aoFechar = { perguntarSeEnviou = false },
        )
    }

    if (confirmarApagar) {
        DialogoConfirmar(
            titulo = "Apagar tudo e recomeçar?",
            texto = "Todos os registros, comprovantes e anexos deste bolão serão apagados do celular. Não há como desfazer.",
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmarApagar = false
                escopo.launch {
                    // Apagar tudo também pede a biometria (se o celular deixou de ter bloqueio, vale a confirmação).
                    val atividade = ctx.atividade()
                    val precisa = Biometria.disponivel(ctx)
                    if (precisa && (atividade == null || !Biometria.autorizar(atividade, "Extinção do Bolão", "Confirme para apagar os dados"))) {
                        avisos.showSnackbar("Os dados não foram apagados: a biometria não foi confirmada.")
                        return@launch
                    }
                    val teste = estado.exemplo
                    repo.apagarTudo()
                    repo.avisar(
                        if (teste) "Extinção de teste concluída: os dados de exemplo foram apagados."
                        else "Bolão extinto. O PDF da auditoria está em Downloads/Bolao. Cadastre o novo grupo.",
                    )
                }
            },
            aoFechar = { confirmarApagar = false },
        )
    }
}

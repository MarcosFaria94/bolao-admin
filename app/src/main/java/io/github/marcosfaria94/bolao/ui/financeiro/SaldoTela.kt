package io.github.marcosfaria94.bolao.ui.financeiro

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.imagem.LayoutTermico
import io.github.marcosfaria94.bolao.imagem.RenderTermico
import io.github.marcosfaria94.bolao.ui.comum.Anexo
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.TextoDoWhatsapp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Documento do saldo atual, no mesmo estilo dos comprovantes, para mandar ao grupo.
 * Não é registro: a imagem é feita na hora, numa pasta temporária, e apagada depois.
 */
@Composable
fun SaldoTela(estado: Estado) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val geradoEm = remember { System.currentTimeMillis() }
    val saldo = Operacoes.saldo(estado)
    val provisionado = Operacoes.provisao(estado, hoje).provisionado
    val titulo = Mensagens.tituloSaldo(hoje)
    val linhas = remember(saldo, provisionado) {
        LayoutTermico.consultaSaldo(
            estado.nomeExibicao,
            Mensagens.saldo(hoje, saldo, provisionado),
            geradoEm,
            estado.comprovantes.lastOrNull()?.autenticacao,
            teste = estado.exemplo,
        )
    }
    val imagem by produceState<Bitmap?>(null, linhas) {
        value = withContext(Dispatchers.Default) { runCatching { RenderTermico.desenhar(ctx, linhas) }.getOrNull() }
    }
    var enviando by remember { mutableStateOf(false) }

    // Nada fica guardado: ao sair da tela, os arquivos temporários do saldo são apagados.
    DisposableEffect(Unit) { onDispose { apagarTemporarios(ctx, idadeMinimaMs = 2 * 60_000) } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Documento com o saldo de agora, para mandar ao grupo. Ele não é registrado nem fica guardado no app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val bmp = imagem
        if (bmp == null) {
            Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Documento do saldo",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextoDoWhatsapp(titulo, rodape = "A imagem do saldo vai junto.")
        Button(
            enabled = !enviando,
            onClick = {
                enviando = true
                escopo.launch {
                    try {
                        val arquivo = withContext(Dispatchers.Default) {
                            File(Compartilhar.pastaTemporaria(ctx), "saldo-${System.currentTimeMillis()}.png").also {
                                RenderTermico.salvar(ctx, linhas, it)
                            }
                        }
                        Compartilhar.whatsapp(ctx, titulo, listOf(Anexo(arquivo, "image/png")))
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível gerar o documento: ${t.message}")
                    } finally {
                        enviando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (enviando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Enviar no WhatsApp")
        }
    }
}

/** Apaga os documentos de saldo temporários com mais de [idadeMinimaMs] (o WhatsApp já os leu). */
fun apagarTemporarios(context: Context, idadeMinimaMs: Long) {
    val limite = System.currentTimeMillis() - idadeMinimaMs
    Compartilhar.pastaTemporaria(context).listFiles()
        ?.filter { it.name.startsWith("saldo-") && it.lastModified() < limite }
        ?.forEach { it.delete() }
}

package io.github.marcosfaria94.bolao.ui.comum

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.imagem.Anexos
import io.github.marcosfaria94.bolao.imagem.LayoutTermico
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.imagem.RenderTermico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A imagem do comprovante (registrado [comprovante] ou a [previa] de antes do registro) e os
 * anexos originais, um embaixo do outro.
 */
@Composable
fun ConteudoComprovante(
    estado: Estado,
    modifier: Modifier = Modifier,
    comprovante: Comprovante? = null,
    previa: List<LayoutTermico.Linha>? = null,
    anexos: List<Arquivo> = comprovante?.anexos.orEmpty(),
    /** Anexos que ainda não foram guardados (prévia), já como imagem: (legenda, imagem). */
    imagensExtras: List<Pair<String, Bitmap>> = emptyList(),
) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val recibo by produceState<Bitmap?>(null, comprovante?.id, previa) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                when {
                    comprovante != null -> BitmapFactory.decodeFile(Recibos.arquivoAgora(ctx, estado, comprovante).path)
                    previa != null -> RenderTermico.desenhar(ctx, previa)
                    else -> null
                }
            }.getOrNull()
        }
    }
    val paginas by produceState<List<Pair<String, Bitmap>>?>(null, anexos) {
        value = withContext(Dispatchers.IO) {
            anexos.flatMapIndexed { i, a ->
                val pgs = Anexos.paginas(repo.arquivo(a), a.mime, 1080).take(4)
                if (pgs.isEmpty()) {
                    emptyList()
                } else {
                    pgs.mapIndexed { k, b ->
                        val sufixo = if (pgs.size > 1) " · página ${k + 1}" else ""
                        "Anexo ${i + 1}: ${a.nomeOriginal}$sufixo" to b
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            paginas?.forEach { it.second.recycle() }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        item {
            val r = recibo
            if (r == null) {
                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Image(
                    bitmap = r.asImageBitmap(),
                    contentDescription = "Comprovante",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(imagensExtras) { (legenda, b) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(legenda, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = legenda,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        val lista = paginas
        if (anexos.isNotEmpty() && lista == null) {
            item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        if (lista != null) {
            items(lista) { (legenda, b) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(legenda, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Image(
                        bitmap = b.asImageBitmap(),
                        contentDescription = legenda,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().background(Color.White),
                    )
                }
            }
        }
        // Anexos que não dão para mostrar aqui (ou PDFs com mais páginas): abrir no celular.
        items(anexos) { a ->
            TextButton(onClick = { Compartilhar.abrir(ctx, repo.arquivo(a), a.mime) }) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Abrir ${a.nomeOriginal}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * "Visualizar comprovante": a imagem gerada pelo app e os anexos originais, em tela cheia.
 * Serve para o registrado ([comprovante]) e para a prévia de antes do registro ([previa]).
 */
@Composable
fun VisualizadorComprovante(
    estado: Estado,
    titulo: String,
    aoFechar: () -> Unit,
    comprovante: Comprovante? = null,
    previa: List<LayoutTermico.Linha>? = null,
    anexos: List<Arquivo> = comprovante?.anexos.orEmpty(),
    imagensExtras: List<Pair<String, Bitmap>> = emptyList(),
    acoes: (@Composable RowScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = aoFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 12.dp)) {
                    IconButton(onClick = aoFechar) { Icon(Icons.Outlined.Close, contentDescription = "Fechar") }
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider()
                ConteudoComprovante(
                    estado,
                    modifier = Modifier.weight(1f),
                    comprovante = comprovante,
                    previa = previa,
                    anexos = anexos,
                    imagensExtras = imagensExtras,
                )
                if (acoes != null) {
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        content = acoes,
                    )
                }
            }
        }
    }
}

/**
 * Tela de um comprovante registrado (aberta pela pendência de envio ou pelas listas):
 * a imagem, os anexos e o botão para enviar ao grupo.
 */
@Composable
fun ComprovanteTela(estado: Estado, id: String) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    val c = estado.comprovante(id)
    if (c == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Este comprovante não existe mais.") }
        return
    }
    val enviado = estado.enviado(c)
    Column(Modifier.fillMaxSize()) {
        Text(
            c.titulo,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ConteudoComprovante(estado, modifier = Modifier.weight(1f).fillMaxWidth(), comprovante = c)
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (c.envioDispensado) {
                Text(
                    "Este registro não vai ao grupo (o aporte mensal é só registrado).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(
                    onClick = { escopo.launch { Compartilhar.comprovante(ctx, estado, c) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(if (enviado) "Enviar de novo" else "Enviar ao grupo", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

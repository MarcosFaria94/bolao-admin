package io.github.marcosfaria94.bolao.ui.comum

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.imagem.Recibos

/** Aviso que vai em toda confirmação de registro (o registro só cresce). */
const val AVISO_DEFINITIVO = "Depois de registrado, nada pode ser alterado nem desfeito."

/** Pé das telas de registro: "Visualizar comprovante" e "Registrar e enviar". */
@Composable
fun BotoesDeRegistro(
    habilitado: Boolean,
    trabalhando: Boolean,
    aoVisualizar: () -> Unit,
    aoRegistrar: () -> Unit,
    modifier: Modifier = Modifier,
    textoVisualizar: String = "Visualizar comprovante",
    textoRegistrar: String = "Registrar e enviar",
    visualizarHabilitado: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = aoVisualizar,
            enabled = visualizarHabilitado && !trabalhando,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(textoVisualizar) }
        Button(
            onClick = aoRegistrar,
            enabled = habilitado && !trabalhando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (trabalhando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(textoRegistrar)
        }
    }
}

/**
 * Grava um registro que cria um comprovante, desenha a imagem dele e (se [enviar])
 * abre o WhatsApp com o título, a imagem e os anexos. Devolve o comprovante criado.
 */
suspend fun registrarComprovante(
    context: Context,
    mudanca: (Estado) -> Estado,
    enviar: Boolean = true,
): Comprovante {
    val repo = context.bolao.repo
    val antes = repo.atual().comprovantes.size
    val novo = repo.atualizar(mudanca)
    check(novo.comprovantes.size > antes) { "O registro não criou o comprovante." }
    val c = novo.comprovantes.last()
    Recibos.arquivo(context, novo, c)
    if (enviar) Compartilhar.comprovante(context, novo, c)
    return c
}

package io.github.marcosfaria94.bolao.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Integridade
import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.tema.EstiloMensagem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Verificador de integridade: refaz a corrente de autenticação, confere a impressão digital
 * de cada anexo e se todo registro tem comprovante. Também confere um código de autenticação
 * impresso num comprovante (por exemplo, um que alguém do grupo recebeu).
 */
@Composable
fun IntegridadeTela(estado: Estado) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    var rodada by remember { mutableIntStateOf(0) }
    var relatorio by remember { mutableStateOf<Integridade.Relatorio?>(null) }
    var codigo by remember { mutableStateOf("") }
    var achados by remember { mutableStateOf<List<Comprovante>?>(null) }
    var vendo by remember { mutableStateOf<Comprovante?>(null) }

    LaunchedEffect(rodada) {
        relatorio = null
        relatorio = withContext(Dispatchers.IO) { Integridade.verificar(estado) { repo.sha256(it) } }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val r = relatorio
        if (r == null) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Conferindo os comprovantes e os anexos...")
                }
            }
        } else {
            val cor = if (r.integro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (r.integro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (r.integro) Icons.Outlined.VerifiedUser else Icons.Outlined.ErrorOutline, contentDescription = null, tint = cor, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (r.integro) "Tudo íntegro" else "Atenção: algo não confere",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Conferido em ${Datas.dataHora(System.currentTimeMillis())}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Item(
                        "Corrente de autenticação",
                        r.quebraNaCorrente?.let { i ->
                            "NÃO confere a partir de \"${estado.comprovantes.getOrNull(i)?.titulo}\": o registro foi alterado depois de criado."
                        } ?: "Íntegra: os ${r.comprovantes} comprovantes conferem com os registros, em ordem.",
                        ok = r.quebraNaCorrente == null,
                    )
                    HorizontalDivider()
                    Item(
                        "Anexos originais",
                        buildString {
                            append("${r.anexosQueConferem} de ${r.anexos} conferem com a impressão digital (SHA-256).")
                            r.anexosAlterados.forEach { append("\n• Alterado: ${it.arquivo.nomeOriginal} (${it.comprovante.titulo})") }
                            r.anexosFaltando.forEach { append("\n• Não encontrado: ${it.arquivo.nomeOriginal} (${it.comprovante.titulo})") }
                            if (r.anexosSemImpressao.isNotEmpty()) {
                                append("\n${r.anexosSemImpressao.size} sem impressão digital guardada (não dá para conferir o conteúdo).")
                            }
                        },
                        ok = r.anexosAlterados.isEmpty() && r.anexosFaltando.isEmpty(),
                    )
                    HorizontalDivider()
                    Item(
                        "Registros com comprovante",
                        if (r.semComprovante.isEmpty()) {
                            "Todo aporte, aposta, apuração, rateio e extrato tem o seu comprovante."
                        } else {
                            "Sem comprovante:" + r.semComprovante.joinToString("") { "\n• $it" }
                        } + if (r.legados > 0) "\n${r.legados} registro(s) de antes da versão 0.3, que ainda não tinha comprovante." else "",
                        ok = r.semComprovante.isEmpty(),
                    )
                }
            }
            OutlinedButton(onClick = { rodada++ }, modifier = Modifier.fillMaxWidth()) { Text("Verificar de novo") }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conferir um comprovante", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Digite o código de autenticação impresso no comprovante (ex.: 3F2A-9C1E-5B7D-0A41-77C2-8B90).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = {
                        codigo = it.take(40)
                        achados = null
                    },
                    label = { Text("Código de autenticação") },
                    textStyle = EstiloMensagem,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = Integridade.normalizar(codigo).length >= 8,
                    onClick = { achados = Integridade.buscar(estado, codigo) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Verificar código") }
                achados?.let { lista ->
                    if (lista.isEmpty()) {
                        Text(
                            "Nenhum comprovante deste app tem esse código. O comprovante não é deste bolão ou foi alterado.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    lista.forEach { c ->
                        val confere = Integridade.conferePosicao(estado, c)
                        Column {
                            Text(
                                if (confere) "Autêntico: ${c.titulo}" else "Encontrado, mas NÃO confere: ${c.titulo}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (confere) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            Text(
                                "Registrado em ${Datas.dataHora(c.criadoEm)} · ${Autenticacao.formatar(c.autenticacao)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { vendo = c }) { Text("Ver o comprovante") }
                        }
                    }
                }
            }
        }
    }

    vendo?.let { c ->
        VisualizadorComprovante(estado, c.titulo, aoFechar = { vendo = null }, comprovante = c)
    }
}

@Composable
private fun Item(titulo: String, texto: String, ok: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            (if (ok) "✓ " else "✗ ") + titulo,
            style = MaterialTheme.typography.titleSmall,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

package io.github.marcosfaria94.bolao.ui.aporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoData
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PreviaMensagem
import io.github.marcosfaria94.bolao.ui.tema.EstiloMensagem
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AporteTela(estado: Estado, aoConcluir: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()

    var data by remember { mutableStateOf(LocalDate.now()) }
    var eventual by remember { mutableStateOf(Operacoes.aporteEhEventual(LocalDate.now())) }
    val recebido = remember { mutableStateMapOf<String, Boolean>().apply { estado.pessoas.forEach { put(it.id, true) } } }
    val valores = remember { mutableStateMapOf<String, Long>().apply { estado.pessoas.forEach { put(it.id, estado.config.aporteMensal) } } }
    var editando by remember { mutableStateOf(false) }
    var textoEditado by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }

    val itens = estado.pessoas.filter { recebido[it.id] == true }.map { it.id to (valores[it.id] ?: 0L) }.filter { it.second > 0 }
    val total = itens.sumOf { it.second }
    val textoGerado = Mensagens.aporte(data, Operacoes.saldo(estado), total, eventual)
    val texto = if (editando) textoEditado else textoGerado

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Marque quem já fez o Pix e confira os valores. O app registra um aporte por integrante, como no extrato.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoData(
            data = data,
            aoMudar = {
                data = it
                eventual = Operacoes.aporteEhEventual(it)
            },
            rotulo = "Data do aporte",
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Aporte eventual", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Aporte fora do dia 1º (adicional, com a concordância dos 3).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = eventual, onCheckedChange = { eventual = it })
        }

        estado.pessoas.forEach { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = recebido[p.id] == true, onCheckedChange = { recebido[p.id] = it })
                    Text(p.apelido, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    CampoDinheiro(
                        centavos = valores[p.id] ?: 0L,
                        aoMudar = { valores[p.id] = it },
                        rotulo = "Valor",
                        modifier = Modifier.weight(1.2f),
                    )
                }
            }
        }
        Text("Total: ${Dinheiro.rs(total)}", style = MaterialTheme.typography.titleMedium)

        if (editando) {
            OutlinedTextField(
                value = textoEditado,
                onValueChange = { textoEditado = it },
                label = { Text("Mensagem (editável)") },
                textStyle = EstiloMensagem,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { editando = false }) { Text("Voltar à mensagem automática") }
        } else {
            PreviaMensagem(textoGerado)
            OutlinedButton(onClick = {
                textoEditado = textoGerado
                editando = true
            }) { Text("Editar mensagem") }
        }

        if (itens.isEmpty()) Aviso("Marque pelo menos um integrante com valor.", alerta = true)

        Button(
            enabled = itens.isNotEmpty() && !salvando,
            onClick = {
                salvando = true
                escopo.launch {
                    try {
                        repo.atualizar { Operacoes.registrarAportes(it, data, itens, eventual, System.currentTimeMillis()) }
                        Compartilhar.whatsapp(ctx, texto)
                        aoConcluir()
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível registrar: ${t.message}")
                    } finally {
                        salvando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Registrar e enviar no WhatsApp") }
    }
}

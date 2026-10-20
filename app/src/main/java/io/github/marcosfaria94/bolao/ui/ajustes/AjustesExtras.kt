package io.github.marcosfaria94.bolao.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.seguranca.Biometria
import io.github.marcosfaria94.bolao.seguranca.atividade
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import kotlinx.coroutines.launch

/** Seção "Extinção do Bolão" dos Ajustes. */
@Composable
fun CartaoExtincao(estado: Estado, aoPedir: () -> Unit) {
    val impedimentos = Operacoes.impedimentosDaExtincao(estado)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Encerra o bolão (Parte 5 do termo): divide o saldo entre os integrantes, gera o PDF da auditoria " +
                    "completa e, depois de enviado ao grupo, apaga tudo para cadastrar um novo grupo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (impedimentos.isNotEmpty()) {
                Text("Antes, é preciso resolver:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                impedimentos.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) }
            }
            Button(
                onClick = aoPedir,
                enabled = impedimentos.isEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Extinção do Bolão") }
        }
    }
}

/**
 * Confirmação séria da extinção: digitar EXTINGUIR e autorizar com a biometria.
 * Depois disso o app só mostra a tela da extinção, até o fim.
 */
@Composable
fun DialogoExtincao(estado: Estado, aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }
    val divisao = Operacoes.divisaoFinal(estado)
    val pode = texto.trim().equals("EXTINGUIR", ignoreCase = false)
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Extinguir o bolão?", color = MaterialTheme.colorScheme.error) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Isto encerra o bolão de vez. Nenhuma aposta mais será feita e o saldo de ${Dinheiro.rs(divisao.saldo)} " +
                        "será dividido: ${Dinheiro.rs(divisao.cota)} para cada um" +
                        (if (divisao.complemento > 0) " (o Administrador completa ${Dinheiro.rs(divisao.complemento)})" else "") + ".",
                )
                Text(
                    "No fim, todos os dados deste bolão serão apagados do celular. Não há como voltar atrás.",
                    fontWeight = FontWeight.Bold,
                )
                Text("Para continuar, digite EXTINGUIR e confirme com a biometria.")
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(20) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pode,
                onClick = {
                    val atividade = ctx.atividade()
                    escopo.launch {
                        if (atividade == null || !Biometria.disponivel(ctx)) {
                            avisos.showSnackbar("Para extinguir o bolão é preciso ter biometria ou bloqueio de tela configurado no celular.")
                            return@launch
                        }
                        val ok = Biometria.autorizar(atividade, "Extinção do Bolão", "Confirme com a biometria")
                        if (!ok) {
                            avisos.showSnackbar("Extinção cancelada: a biometria não foi confirmada.")
                            return@launch
                        }
                        try {
                            ctx.bolao.repo.atualizar { Operacoes.iniciarExtincao(it, System.currentTimeMillis()) }
                            aoFechar()
                        } catch (t: Throwable) {
                            avisos.showSnackbar("Não foi possível iniciar a extinção: ${t.message}")
                        }
                    }
                },
            ) { Text("Confirmar", color = if (pode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

package io.github.marcosfaria94.bolao.ui.ajustes

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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

/** Toques seguidos para destravar a extinção forçada: 5 até a dica aparecer, mais 5 para destravar. */
private const val TOQUES_DICA = 5
private const val TOQUES_DESTRAVAR = 10

/** Intervalo máximo entre dois toques "seguidos" (e quanto a dica espera pelos 5 toques finais). */
private const val INTERVALO_TOQUES_MS = 800L
private const val ESPERA_DEPOIS_DA_DICA_MS = 4_000L

/**
 * Seção "Extinção do Bolão" dos Ajustes. O quadro tem um botão invisível: 5 toques seguidos
 * mostram a dica "Toque mais 5 vezes para desbloquear opção" e, com mais 5, o botão vira
 * "Extinção forçada" (ignora as pendências da lista).
 */
@Composable
fun CartaoExtincao(estado: Estado, forcadaLiberada: Boolean, aoLiberarForcada: () -> Unit, aoPedir: () -> Unit) {
    val ctx = LocalContext.current
    val impedimentos = Operacoes.impedimentosDaExtincao(estado)
    var toques by remember { mutableIntStateOf(0) }
    var ultimoToque by remember { mutableLongStateOf(0L) }
    var dica by remember { mutableStateOf<Toast?>(null) }
    fun mostrar(texto: String) {
        dica?.cancel()
        dica = Toast.makeText(ctx, texto, Toast.LENGTH_SHORT).also { it.show() }
    }
    val tocar: () -> Unit = tocar@{
        if (forcadaLiberada) return@tocar
        val agora = SystemClock.elapsedRealtime()
        val limite = if (toques >= TOQUES_DICA) ESPERA_DEPOIS_DA_DICA_MS else INTERVALO_TOQUES_MS
        if (agora - ultimoToque > limite) toques = 0
        ultimoToque = agora
        toques += 1
        when {
            toques >= TOQUES_DESTRAVAR -> {
                toques = 0
                mostrar("Opção desbloqueada: extinção forçada.")
                aoLiberarForcada()
            }
            toques >= TOQUES_DICA -> {
                val faltam = TOQUES_DESTRAVAR - toques
                mostrar(if (faltam == 1) "Toque mais 1 vez para desbloquear opção" else "Toque mais $faltam vezes para desbloquear opção")
            }
        }
    }
    val forcar = forcadaLiberada && impedimentos.isNotEmpty()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            // Botão invisível: sem efeito de toque, só conta os toques.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = tocar),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Encerra o bolão (Parte 5 do termo): divide o saldo entre os integrantes, gera o PDF da auditoria " +
                    "completa e, depois de enviado ao grupo, apaga tudo para cadastrar um novo grupo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (impedimentos.isNotEmpty()) {
                Text(
                    if (forcar) "A extinção forçada ignora:" else "Antes, é preciso resolver:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                impedimentos.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) }
            }
            Button(
                onClick = aoPedir,
                enabled = impedimentos.isEmpty() || forcar,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (forcar) "Extinção forçada" else "Extinção do Bolão") }
        }
    }
}

/**
 * Confirmação séria da extinção: digitar EXTINGUIR e autorizar com a biometria.
 * Depois disso o app só mostra a tela da extinção, até o fim. Na [forcada], as pendências
 * ficam como estão (e, sem saldo, não há divisão: vai direto para o PDF da auditoria).
 */
@Composable
fun DialogoExtincao(estado: Estado, forcada: Boolean, aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }
    val divisao = Operacoes.divisaoFinal(estado)
    val pode = texto.trim().equals("EXTINGUIR", ignoreCase = false)
    val pendencias = if (forcada) Operacoes.impedimentosDaExtincao(estado) else emptyList()
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(if (forcada) "Extinção forçada?" else "Extinguir o bolão?", color = MaterialTheme.colorScheme.error) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (divisao.saldo > 0) {
                        "Isto encerra o bolão de vez. Nenhuma aposta mais será feita e o saldo de ${Dinheiro.rs(divisao.saldo)} " +
                            "será dividido: ${Dinheiro.rs(divisao.cota)} para cada um" +
                            (if (divisao.complemento > 0) " (o Administrador completa ${Dinheiro.rs(divisao.complemento)})" else "") + "."
                    } else {
                        "Isto encerra o bolão de vez. Nenhuma aposta mais será feita. Não há saldo para dividir: " +
                            "a extinção vai direto para o PDF da auditoria."
                    },
                )
                if (pendencias.isNotEmpty()) {
                    Text("As pendências abaixo ficam como estão:", fontWeight = FontWeight.Bold)
                    pendencias.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
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
                            ctx.bolao.repo.atualizar { Operacoes.iniciarExtincao(it, System.currentTimeMillis(), forcada = forcada) }
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

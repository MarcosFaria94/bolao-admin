package io.github.marcosfaria94.bolao.ui.ajustes

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.net.Contato
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** O que a tela diz depois do envio (o aviso no alto). */
const val MENSAGEM_ENVIADA = "Mensagem enviada. Obrigado!"
const val MENSAGEM_GUARDADA = "Mensagem enviada: ela chega assim que o e-mail de destino confirmar o serviço. Obrigado!"

/** Os nomes dos botões de edição da mensagem (o que o leitor de tela diz). */
const val NEGRITO = "Negrito"
const val ITALICO = "Itálico"
const val LISTA = "Lista"
const val DESFAZER = "Desfazer"
const val LIMPAR_A_MENSAGEM = "Limpar a mensagem"
const val ENVIAR = "Enviar"
const val CAMPO_DA_MENSAGEM = "Mensagem"
const val CAMPO_DO_EMAIL = "Seu e-mail (opcional)"

/** A explicação no alto da tela do Contato. */
const val EXPLICACAO_DO_CONTATO =
    "Fale com quem faz o app: uma dúvida, uma sugestão ou um problema que você encontrou. A mensagem vai direto para o " +
        "e-mail ${Contato.EMAIL} (pelo serviço gratuito FormSubmit), sem abrir o app de e-mail. Uma mensagem a cada 5 minutos."

/** "Você pode mandar outra mensagem em 4 min." */
fun textoDaEspera(ms: Long): String = "Você pode mandar outra mensagem em ${Contato.textoDaEspera(ms)}."

/**
 * O trecho escolhido da mensagem entre as [marca]s (negrito "*", itálico "_", como no WhatsApp); sem nada escolhido,
 * as duas marcas no lugar do cursor, com ele no meio. Desde a 0.32 (Contato).
 */
fun envolver(v: TextFieldValue, marca: String): TextFieldValue {
    val ini = minOf(v.selection.start, v.selection.end).coerceIn(0, v.text.length)
    val fim = maxOf(v.selection.start, v.selection.end).coerceIn(0, v.text.length)
    val texto = v.text.substring(0, ini) + marca + v.text.substring(ini, fim) + marca + v.text.substring(fim)
    val sel = if (ini == fim) TextRange(ini + marca.length) else TextRange(ini + marca.length, fim + marca.length)
    return TextFieldValue(texto, sel)
}

/** O marcador da lista. */
const val MARCADOR = "• "

/**
 * As linhas em que está o cursor (ou o trecho escolhido) viram itens de lista ("• "); se todas já são, deixam de
 * ser. Desde a 0.32 (Contato).
 */
fun alternarLista(v: TextFieldValue): TextFieldValue {
    val t = v.text
    val ini = minOf(v.selection.start, v.selection.end).coerceIn(0, t.length)
    val fim = maxOf(v.selection.start, v.selection.end).coerceIn(0, t.length)
    val comeco = t.lastIndexOf('\n', (ini - 1).coerceAtLeast(-1)).let { if (ini == 0) 0 else it + 1 }
    val final = t.indexOf('\n', fim).let { if (it < 0) t.length else it }
    val linhas = t.substring(comeco, final).split('\n')
    val tirar = linhas.all { it.startsWith(MARCADOR) }
    val novas = linhas.map { if (tirar) it.removePrefix(MARCADOR) else MARCADOR + it }
    val trecho = novas.joinToString("\n")
    val diferenca = trecho.length - (final - comeco)
    val primeira = if (tirar) -MARCADOR.length else MARCADOR.length
    val texto = t.substring(0, comeco) + trecho + t.substring(final)
    val novoIni = (ini + primeira).coerceIn(comeco, comeco + trecho.length)
    val novoFim = (fim + diferenca).coerceIn(novoIni, comeco + trecho.length)
    return TextFieldValue(texto, if (ini == fim) TextRange(novoIni) else TextRange(novoIni, novoFim))
}

/**
 * O Contato (desde a 0.32, Ajustes > Apoio > Contato): o motivo (Mensagem, Dúvida, Sugestão ou Reportar bug), o
 * e-mail para a resposta (opcional), a mensagem com os botões de edição (negrito, itálico, lista, desfazer e limpar)
 * e o "Enviar", que manda direto para o e-mail de quem faz o app ([Contato]). Uma mensagem a cada 5 minutos: depois
 * de mandar, o botão fica desligado e a tela diz quanto falta. Não dando certo, o "Enviar pelo app de e-mail".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContatoDoApp() {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var motivo by rememberSaveable { mutableStateOf(Contato.Motivo.MENSAGEM) }
    var email by rememberSaveable { mutableStateOf("") }
    var mensagem by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val historico = remember { mutableStateListOf<TextFieldValue>() }
    var comAparelho by rememberSaveable { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ultimo by remember { mutableStateOf(Contato.ultimoEnvio(ctx)) }
    // Quanto falta para a próxima mensagem (refeito a cada segundo enquanto falta).
    val espera by produceState(Contato.espera(ultimo, System.currentTimeMillis()), ultimo) {
        while (true) {
            value = Contato.espera(ultimo, System.currentTimeMillis())
            if (value == 0L) break
            delay(1_000)
        }
    }

    fun mudar(novo: TextFieldValue) {
        val limitado = if (novo.text.length > Contato.TAMANHO_MAXIMO) novo.copy(text = novo.text.take(Contato.TAMANHO_MAXIMO)) else novo
        if (limitado.text != mensagem.text) {
            historico.add(mensagem)
            if (historico.size > 60) historico.removeAt(0)
        }
        mensagem = limitado
    }
    val aparelho = if (motivo == Contato.Motivo.BUG && comAparelho) Contato.aparelho() else null
    val emailOk = Contato.emailValido(email)
    val pode = mensagem.text.isNotBlank() && emailOk && espera == 0L && !enviando

    fun depoisDoEnvio(texto: String) {
        val agora = System.currentTimeMillis()
        Contato.marcarEnvio(ctx, agora)
        ultimo = agora
        mensagem = TextFieldValue("")
        historico.clear()
        erro = null
        escopo.launch { avisos.showSnackbar(texto) }
    }

    ExplicacaoAjuste(EXPLICACAO_DO_CONTATO)
    QuadroAjuste {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Motivo", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Contato.Motivo.entries.forEach { mo ->
                    FilterChip(
                        selected = motivo == mo,
                        onClick = { motivo = mo },
                        label = { Text(mo.rotulo) },
                        leadingIcon = if (motivo == mo) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else {
                            null
                        },
                    )
                }
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.take(120) },
                singleLine = true,
                label = { Text(CAMPO_DO_EMAIL) },
                supportingText = { Text(if (emailOk) "Para receber a resposta." else "Confira o e-mail.") },
                isError = !emailOk,
                leadingIcon = { Icon(Icons.Outlined.MailOutline, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            // Os botões de edição, em cima da mensagem.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BotaoDeEdicao(Icons.Outlined.FormatBold, NEGRITO) { mudar(envolver(mensagem, "*")) }
                BotaoDeEdicao(Icons.Outlined.FormatItalic, ITALICO) { mudar(envolver(mensagem, "_")) }
                BotaoDeEdicao(Icons.AutoMirrored.Outlined.FormatListBulleted, LISTA) { mudar(alternarLista(mensagem)) }
                VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                BotaoDeEdicao(Icons.AutoMirrored.Outlined.Undo, DESFAZER, habilitado = historico.isNotEmpty()) {
                    mensagem = historico.removeAt(historico.lastIndex)
                }
                BotaoDeEdicao(Icons.Outlined.ClearAll, LIMPAR_A_MENSAGEM, habilitado = mensagem.text.isNotEmpty()) { mudar(TextFieldValue("")) }
                Spacer(Modifier.weight(1f))
                Text(
                    "${mensagem.text.length}/${Contato.TAMANHO_MAXIMO}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = mensagem,
                onValueChange = ::mudar,
                label = { Text(CAMPO_DA_MENSAGEM) },
                placeholder = { Text("Escreva aqui. *negrito*, _itálico_ e • lista, como no WhatsApp.") },
                minLines = 6,
                maxLines = 14,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = CAMPO_DA_MENSAGEM },
            )
            if (motivo == Contato.Motivo.BUG) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = comAparelho, onCheckedChange = { comAparelho = it })
                    Text("Mandar junto a versão do app e o modelo do celular", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = {
                    if (!pode) return@Button
                    enviando = true
                    erro = null
                    escopo.launch {
                        when (val r = Contato.enviar(motivo, mensagem.text, email, aparelho)) {
                            Contato.Resultado.Enviada -> depoisDoEnvio(MENSAGEM_ENVIADA)
                            Contato.Resultado.Guardada -> depoisDoEnvio(MENSAGEM_GUARDADA)
                            is Contato.Resultado.Erro -> erro = r.mensagem
                        }
                        enviando = false
                    }
                },
                enabled = pode,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (enviando) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (enviando) "Enviando..." else ENVIAR)
            }
            if (espera > 0L) {
                Text(textoDaEspera(espera), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            erro?.let { e ->
                Text(e, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                // O jeito de reserva: o app de e-mail do celular, com tudo preenchido (também conta para os 5 minutos).
                OutlinedButton(
                    onClick = {
                        try {
                            ctx.startActivity(Contato.intentDoEmail(motivo, mensagem.text, email, aparelho))
                            val agora = System.currentTimeMillis()
                            Contato.marcarEnvio(ctx, agora)
                            ultimo = agora
                            erro = null
                        } catch (_: ActivityNotFoundException) {
                            erro = "Não há app de e-mail no celular. Escreva para ${Contato.EMAIL}."
                        }
                    },
                    enabled = espera == 0L && mensagem.text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Enviar pelo app de e-mail") }
            }
        }
    }
}

/** Um botão de edição da mensagem (só o ícone; o nome é o que o leitor de tela diz). */
@Composable
private fun BotaoDeEdicao(icone: androidx.compose.ui.graphics.vector.ImageVector, nome: String, habilitado: Boolean = true, aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar, enabled = habilitado, modifier = Modifier.size(40.dp)) {
        Icon(icone, contentDescription = nome, modifier = Modifier.size(22.dp))
    }
}

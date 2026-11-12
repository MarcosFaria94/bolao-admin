package io.github.marcosfaria94.bolao.ui.ajustes

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.marcosfaria94.bolao.seguranca.Biometria
import io.github.marcosfaria94.bolao.seguranca.atividade
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import kotlinx.coroutines.launch

/** A chave do bloqueio do app (o nome da linha e da chave). */
const val PEDIR_A_BIOMETRIA = "Pedir a biometria"

/** Na lista dos Ajustes, quando o celular não tem biometria nem bloqueio de tela. */
const val SEM_BLOQUEIO_NO_CELULAR = "Sem bloqueio"

/** O aviso quando a biometria não confirma o desligamento. */
const val BIOMETRIA_NAO_DESLIGADA = "A biometria não foi confirmada: o bloqueio continua ligado."

/** "1 min", "30 min": cada tempo da escolha. */
fun textoDoTempo(minutos: Int): String = "$minutos min"

/**
 * Ajustes > Biometria (desde a 0.30, no grupo Celular; até a 0.29 o app sempre pedia a biometria, sem ajuste):
 * a chave "Pedir a biometria" (desligar pede a biometria antes, para quem pegar o celular desbloqueado não tirar o
 * bloqueio do app) e, ligada, depois de quanto tempo fora do app ela é pedida de novo (1, 3, 10 ou 30 minutos;
 * 3 é o de sempre). Sem biometria nem bloqueio de tela no celular, o aviso e o botão que abre a segurança do
 * Android. Vale para o app todo (todas as sessões). A extinção e o "Apagar tudo" continuam pedindo a biometria.
 */
@Composable
internal fun BiometriaDoCelular() {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var versao by remember { mutableIntStateOf(0) }
    // Lido de novo ao voltar (dos ajustes de segurança do Android, onde dá para criar o bloqueio de tela).
    LifecycleResumeEffect(Unit) {
        versao++
        onPauseOrDispose {}
    }
    val b = remember(versao) { Biometria.bloqueio(ctx) }
    val disponivel = remember(versao) { Biometria.disponivel(ctx) }

    fun mudar(pedir: Boolean) {
        if (pedir) {
            Biometria.pedir(ctx, true)
            versao++
            return
        }
        val atividade = ctx.atividade()
        escopo.launch {
            if (disponivel && (atividade == null || !Biometria.autorizar(atividade, "Biometria", "Confirme para desligar o bloqueio do app"))) {
                avisos.showSnackbar(BIOMETRIA_NAO_DESLIGADA)
                return@launch
            }
            Biometria.pedir(ctx, false)
            versao++
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ExplicacaoAjuste(
            "O app pede a digital (ou o rosto, ou o bloqueio de tela do celular) ao abrir e ao voltar depois de um tempo fora. " +
                "Vale para todas as sessões. A extinção do bolão e o apagar tudo pedem a biometria sempre.",
        )
        if (!disponivel) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Aviso("O celular não tem biometria nem bloqueio de tela: o app abre sem pedir nada.", alerta = true)
                OutlinedButton(
                    onClick = {
                        try {
                            ctx.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (t: ActivityNotFoundException) {
                            escopo.launch { avisos.showSnackbar("Abra os ajustes do celular e crie um bloqueio de tela.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Configurar o bloqueio no celular")
                }
            }
        }
        QuadroAjuste {
            LinhaAjuste(
                emblema = { EmblemaIcone(Icons.Outlined.Fingerprint, Color(0xFF2F6FDE), TAMANHO_EMBLEMA) },
                titulo = PEDIR_A_BIOMETRIA,
                subtitulo = if (b.pedir) "Ao abrir o app" else "Desligada: o app abre sem pedir",
                aoTocar = { mudar(!b.pedir) },
                final = {
                    Switch(
                        checked = b.pedir,
                        onCheckedChange = { mudar(it) },
                        modifier = Modifier.semantics { contentDescription = PEDIR_A_BIOMETRIA },
                    )
                },
            )
            if (b.pedir) {
                Divisoria()
                LinhaAjuste(
                    emblema = { EmblemaIcone(Icons.Outlined.Schedule, AzulMarinhoEmblema, TAMANHO_EMBLEMA) },
                    titulo = "Pedir de novo depois de",
                    subtitulo = "Tempo fora do app: ir ao WhatsApp ou ao app da Caixa e voltar antes disso não pede de novo.",
                )
                EscolhaDoTempo(b.minutos) { t ->
                    Biometria.minutos(ctx, t)
                    versao++
                }
            }
        }
    }
}

/**
 * Os tempos lado a lado, com a largura dividida por igual (cabem com a letra grande do celular), o escolhido
 * destacado, como os botões dos acertos dos Detalhes.
 */
@Composable
private fun EscolhaDoTempo(escolhido: Int, aoEscolher: (Int) -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
    ) {
        Biometria.TEMPOS.forEach { t ->
            val sim = t == escolhido
            val fundo = if (sim) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
            val texto = if (sim) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(forma)
                    .background(fundo)
                    .selectable(selected = sim, role = Role.RadioButton) { aoEscolher(t) }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
            ) {
                Text(
                    textoDoTempo(t),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (sim) FontWeight.Bold else FontWeight.Medium,
                    color = texto,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

package io.github.marcosfaria94.bolao.ui.teste

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.tema.VermelhoTeste
import kotlinx.coroutines.launch

/** Faixa vermelha (letra branca) embaixo do título em todas as telas enquanto o modo de teste estiver ligado. */
@Composable
fun FaixaModoTeste(aoSair: () -> Unit) {
    Surface(
        color = VermelhoTeste,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Modo de teste · dados de exemplo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = aoSair, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text("Sair", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Confirmação para sair do modo de teste (apaga os dados de exemplo). */
@Composable
fun DialogoSairDoTeste(aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    DialogoConfirmar(
        titulo = "Sair do modo de teste?",
        texto = "Os dados de exemplo e os comprovantes fictícios serão apagados, e o app volta para os seus dados reais, como estavam.",
        confirmar = "Sair e apagar",
        aoConfirmar = {
            aoFechar()
            ModoTeste.sair(ctx)
        },
        aoFechar = aoFechar,
    )
}

/** Seção "Modo de teste" dos Ajustes. */
@Composable
fun CartaoModoTeste(emTeste: Boolean, aoPedirSaida: () -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val preparando by ModoTeste.trabalhando.collectAsStateWithLifecycle()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (emTeste) {
                Text(
                    "Você está vendo dados de exemplo. Aportes, apostas, apurações, rateios e extratos feitos aqui não mexem nos dados reais.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = {
                        Vigia.rodarAgora(ctx)
                        escopo.launch { avisos.showSnackbar("Conferindo: se algum jogo já tiver resultado, o aviso chega em instantes.") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Conferir os sorteios agora") }
                OutlinedButton(
                    onClick = { ModoTeste.entrar(ctx) },
                    enabled = !preparando,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (preparando) "Preparando..." else "Recomeçar com um exemplo novo") }
                TextButton(onClick = aoPedirSaida, enabled = !preparando, modifier = Modifier.fillMaxWidth()) {
                    Text("Sair do modo de teste", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    "Para experimentar sem mexer nos dados reais: o app abre um mês de exemplo, separado, e apaga tudo ao sair.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                BotaoExperimentar(preparando = preparando, aoTocar = { ModoTeste.entrar(ctx) })
            }
        }
    }
}

@Composable
fun BotaoExperimentar(preparando: Boolean, aoTocar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = aoTocar, enabled = !preparando, modifier = modifier) {
        Icon(Icons.Outlined.Science, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (preparando) "Preparando o exemplo..." else "Experimentar com dados de exemplo")
    }
}

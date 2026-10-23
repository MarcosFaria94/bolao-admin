package io.github.marcosfaria94.bolao.ui.notificacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Pendencia
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.tema.Barra
import io.github.marcosfaria94.bolao.ui.tema.corEntrada

private fun icone(t: Pendencia.Tipo): ImageVector = when (t) {
    Pendencia.Tipo.APORTE -> Icons.Outlined.Savings
    Pendencia.Tipo.APOSTA_DO_DIA -> Icones.Trevo
    Pendencia.Tipo.RASCUNHO -> Icons.Outlined.AttachFile
    Pendencia.Tipo.ENVIO -> Icons.AutoMirrored.Outlined.Send
    Pendencia.Tipo.APURACAO -> Icons.AutoMirrored.Outlined.FactCheck
    Pendencia.Tipo.EXTRATO -> Icons.AutoMirrored.Outlined.ReceiptLong
    Pendencia.Tipo.INDICES -> Icons.Outlined.QueryStats
}

/**
 * Notificações: todas as pendências do bolão (ações para deixar o app em dia), a mais recente
 * primeiro. Tocar numa pendência abre a tela que resolve.
 */
@Composable
fun NotificacoesTela(pendencias: List<Pendencia>, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    // Com a lista na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(Unit) { Notificador.limparAcoes(ctx) }

    if (pendencias.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = corEntrada(), modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("Tudo em dia", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Quando houver algo a fazer no bolão (aporte, aposta, envio de comprovante, extrato), aparece aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(contentPadding = PaddingTela, modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                if (pendencias.size == 1) "1 pendência" else "${pendencias.size} pendências",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    pendencias.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ItemPendencia(p) { abrir(p.destino) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemPendencia(p: Pendencia, aoTocar: () -> Unit) {
    val hoje = java.time.LocalDate.now()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = aoTocar).padding(vertical = 12.dp),
    ) {
        Box(Modifier.size(42.dp).background(Barra.contador.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icone(p.tipo), contentDescription = null, tint = Barra.contador, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        // A data fica em cima, pequena: o título usa a largura toda.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                Datas.relativa(p.desde.toLocalDate(), hoje),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(p.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(p.texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

package io.github.marcosfaria94.bolao.ui.notificacoes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Pendencia
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.ui.comum.CabecalhoDia
import io.github.marcosfaria94.bolao.ui.comum.CoresEmblema
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.Indicador
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.Quadro
import io.github.marcosfaria94.bolao.ui.comum.Secao
import io.github.marcosfaria94.bolao.ui.comum.TarjaTitulo
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import java.time.LocalDate

/** Nome do tipo da pendência, na tarja do item ("Aporte", "Apuração"...). */
fun tarjaPendencia(p: Pendencia): String = when (p.tipo) {
    Pendencia.Tipo.APORTE -> "Aporte"
    Pendencia.Tipo.APOSTA_DO_DIA -> "Aposta do dia"
    Pendencia.Tipo.RASCUNHO -> "Aposta em andamento"
    Pendencia.Tipo.ENVIO -> "Comprovante"
    Pendencia.Tipo.APURACAO -> "Apuração"
    Pendencia.Tipo.EXTRATO -> "Extrato"
    Pendencia.Tipo.INDICES -> "Índices"
}

/** "3 pendências", "1 pendência". */
fun textoQuantas(n: Int): String = if (n == 1) "1 pendência" else "$n pendências"

/**
 * Notificações, no estilo do Financeiro: um quadro com o total de pendências (as de hoje e as
 * anteriores) e, embaixo, a lista "A fazer" separada por dia (a mais recente primeiro), cada item
 * com o emblema (a bola da modalidade nas apostas, o cofrinho no aporte...), a tarja com o tipo,
 * o que fazer e o detalhe. Tocar numa pendência abre a tela que resolve.
 */
@Composable
fun NotificacoesTela(pendencias: List<Pendencia>, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    // Com a lista na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(Unit) { Notificador.limparAcoes(ctx) }
    val hoje = LocalDate.now()
    val deHoje = pendencias.count { !it.desde.toLocalDate().isBefore(hoje) }

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Resumo(pendencias.size, deHoje, Modifier.padding(top = 8.dp)) }
        item {
            Secao("A fazer") {
                if (pendencias.isEmpty()) {
                    TextoVazio(
                        "Quando houver algo a fazer no bolão (aporte, aposta, envio de comprovante, extrato), aparece aqui.",
                        Modifier.padding(vertical = 10.dp),
                    )
                } else {
                    var dia: LocalDate? = null
                    pendencias.forEach { p ->
                        val d = p.desde.toLocalDate()
                        if (d != dia) {
                            // Divisória inteira entre um dia e outro; dentro do mesmo dia, uma mais curta.
                            if (dia != null) HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            dia = d
                            CabecalhoDia(d, hoje)
                        } else {
                            HorizontalDivider(Modifier.padding(start = 66.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        ItemPendencia(p) { abrir(p.destino) }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/** Quadro do alto (como o do saldo): "Tudo em dia" ou quantas pendências há (as de hoje e as de antes). */
@Composable
private fun Resumo(total: Int, deHoje: Int, modifier: Modifier = Modifier) {
    Quadro(modifier) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
            Text("Situação do bolão", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (total == 0) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = corEntrada(), modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Tudo em dia", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                }
            } else {
                TextoQueCabe(
                    textoQuantas(total),
                    MaterialTheme.typography.displaySmall,
                    peso = FontWeight.Bold,
                    menor = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Indicador("De hoje", deHoje.toString(), Modifier.weight(1f))
                    Indicador("Anteriores", (total - deHoje).toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

/** Emblema da pendência: a bola da modalidade nas apostas e um desenho colorido nas outras. */
@Composable
private fun EmblemaPendencia(p: Pendencia) {
    val tamanho = 52.dp
    val m = p.modalidade
    when {
        p.tipo == Pendencia.Tipo.APORTE || p.comprovante == TipoComprovante.APORTE ->
            EmblemaDesenho(Desenhos.Cofrinho, CoresEmblema.APORTE, tamanho)
        p.tipo == Pendencia.Tipo.EXTRATO || p.comprovante == TipoComprovante.EXTRATO ->
            EmblemaDesenho(Desenhos.Extrato, CoresEmblema.EXTRATO, tamanho)
        p.comprovante == TipoComprovante.RATEIO -> EmblemaDesenho(Desenhos.Moedas, CoresEmblema.RATEIO, tamanho)
        m != null -> EmblemaModalidade(m, p.concurso.takeIf { it > 0 && p.tipo != Pendencia.Tipo.INDICES }, tamanho)
        p.tipo == Pendencia.Tipo.APOSTA_DO_DIA -> EmblemaIcone(Icones.TrevoCheio, AzulMarinho, tamanho, corIcone = Dourado)
        p.tipo == Pendencia.Tipo.RASCUNHO -> EmblemaIcone(Icons.Outlined.AttachFile, AzulMarinho, tamanho)
        p.tipo == Pendencia.Tipo.INDICES -> EmblemaIcone(Icons.Outlined.QueryStats, CoresEmblema.RATEIO, tamanho)
        else -> EmblemaIcone(Icons.AutoMirrored.Outlined.Send, CoresEmblema.APORTE, tamanho)
    }
}

/** Uma pendência da lista: emblema, tarja com o tipo, o que fazer e o detalhe. */
@Composable
private fun ItemPendencia(p: Pendencia, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(vertical = 10.dp),
    ) {
        EmblemaPendencia(p)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            TarjaTitulo(tarjaPendencia(p))
            Text(p.titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                p.texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

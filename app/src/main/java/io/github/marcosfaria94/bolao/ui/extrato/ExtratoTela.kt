package io.github.marcosfaria94.bolao.ui.extrato

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.pdf.AuditoriaPdf
import io.github.marcosfaria94.bolao.pdf.PdfsDoApp
import io.github.marcosfaria94.bolao.ui.comum.AVISO_DEFINITIVO
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BotoesDeRegistro
import io.github.marcosfaria94.bolao.ui.comum.CampoData
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.SeletorMes
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Meses que podem ter extrato: do cadastro até o atual (o mais recente primeiro). */
private fun mesesDisponiveis(e: Estado, hoje: LocalDate): List<YearMonth> {
    val atual = YearMonth.from(hoje)
    var m = YearMonth.from(PdfsDoApp.inicio(e))
    val lista = mutableListOf<YearMonth>()
    while (!m.isAfter(atual)) {
        lista += m
        m = m.plusMonths(1)
    }
    return lista.reversed().ifEmpty { listOf(atual) }
}

@Composable
fun ExtratoTela(estado: Estado, mesInicial: YearMonth? = null) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()

    var mes by remember { mutableStateOf(mesInicial ?: YearMonth.from(hoje)) }
    var informado by remember { mutableLongStateOf(Operacoes.saldo(estado)) }
    var confirmar by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }

    val existente = estado.extratos.firstOrNull { it.mes == mes.toString() }
    val comprovante = estado.comprovantes.lastOrNull { it.tipo == TipoComprovante.EXTRATO && it.mes == mes.toString() }
    val impedimento = Operacoes.impedimentoDoExtrato(estado, mes, hoje)
    val conferido = Operacoes.conferirSaldo(estado, mes, informado)

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mês/ano (abre no mês atual).
                SeletorMes(
                    meses = mesesDisponiveis(estado, hoje),
                    escolhido = mes,
                    aoEscolher = { mes = it },
                    detalhe = { m -> if (estado.extratos.any { it.mes == m.toString() }) "registrado" else null },
                )

                if (existente != null) {
                    val quando = Instant.ofEpochMilli(existente.criadoEm).atZone(ZoneId.systemDefault()).toLocalDate()
                    Aviso("O extrato de ${Datas.mesAno(mes)} foi registrado em ${Datas.longa(quando)}: só dá para ver o comprovante.")
                    LinhaValor("Saldo da caixinha informado", Dinheiro.rs(existente.saldoInformado))
                    LinhaValor(
                        "Rendimento líquido do mês",
                        (if (existente.rendimento < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(existente.rendimento)),
                    )
                    if (comprovante != null) {
                        OutlinedButton(onClick = { vendo = true }, modifier = Modifier.fillMaxWidth()) { Text("Visualizar comprovante") }
                        if (!comprovante.envioDispensado) {
                            val enviado = estado.enviado(comprovante)
                            Button(
                                onClick = { escopo.launch { Compartilhar.comprovante(ctx, estado, comprovante) } },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (enviado) "Enviar de novo" else "Enviar ao grupo") }
                        }
                    }
                } else {
                    Text(
                        "Abra a caixinha do bolão no Nubank e digite o saldo líquido que aparece. O rendimento do mês é a diferença para o saldo do app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CampoDinheiro(
                        centavos = informado,
                        aoMudar = { informado = it },
                        rotulo = "Saldo líquido da caixinha",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LinhaValor("Saldo calculado pelo app", Dinheiro.rs(conferido.calculado))
                    LinhaValor(
                        "Rendimento líquido de ${Datas.mesAno(mes)}",
                        (if (conferido.rendimento < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(conferido.rendimento)),
                        destaque = true,
                    )
                    conferido.alerta?.let { Aviso("Possível inconsistência: $it", alerta = true) }
                    BotoesDeRegistro(
                        habilitado = impedimento == null,
                        trabalhando = trabalhando,
                        aoVisualizar = { vendo = true },
                        aoRegistrar = { confirmar = true },
                    )
                    impedimento?.let { Aviso(it, alerta = true) }
                }
            }
        }

        Auditoria(estado)
    }

    if (vendo) {
        if (existente != null && comprovante != null) {
            VisualizadorComprovante(estado, comprovante.titulo, aoFechar = { vendo = false }, comprovante = comprovante)
        } else {
            VisualizadorComprovante(
                estado,
                Mensagens.tituloExtrato(hoje),
                aoFechar = { vendo = false },
                previa = Recibos.previa(estado, TipoComprovante.EXTRATO, Operacoes.previaExtrato(estado, mes, informado, hoje), 0),
            )
        }
    }

    if (confirmar) {
        DialogoConfirmar(
            titulo = "Registrar o extrato de ${Datas.mesAno(mes)}?",
            texto = "Lança o rendimento de ${Dinheiro.rs(conferido.rendimento)} e fecha o mês. Em seguida o WhatsApp abre com o comprovante." +
                (conferido.alerta?.let { "\n\nAtenção: $it" } ?: "") + "\n\n" + AVISO_DEFINITIVO,
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmar = false
                trabalhando = true
                escopo.launch {
                    try {
                        val m = mes
                        val v = informado
                        registrarComprovante(ctx, { Operacoes.registrarExtrato(it, m, v, hoje, System.currentTimeMillis()) })
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível registrar: ${t.message}")
                    } finally {
                        trabalhando = false
                    }
                }
            },
            aoFechar = { confirmar = false },
        )
    }
}

/** PDF com todos os comprovantes do período e os anexos originais, em ordem cronológica. */
@Composable
private fun Auditoria(estado: Estado) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var de by remember { mutableStateOf(PdfsDoApp.inicio(estado)) }
    var ate by remember { mutableStateOf(LocalDate.now()) }
    var gerando by remember { mutableStateOf(false) }
    var progresso by remember { mutableFloatStateOf(0f) }
    var pronto by remember { mutableStateOf<Pair<Uri, AuditoriaPdf.Resultado>?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Auditoria", style = MaterialTheme.typography.titleMedium)
            Text(
                "PDF com todos os comprovantes (aporte, aposta, apuração, rateio e extrato) e os anexos originais, em ordem cronológica.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoData(data = de, aoMudar = { de = it }, rotulo = "De", modifier = Modifier.weight(1f))
                CampoData(data = ate, aoMudar = { ate = it }, rotulo = "Até", modifier = Modifier.weight(1f))
            }
            if (gerando) LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth())
            Button(
                enabled = !gerando && !de.isAfter(ate),
                onClick = {
                    gerando = true
                    progresso = 0f
                    escopo.launch {
                        try {
                            pronto = PdfsDoApp.auditoriaEmDownloads(ctx, estado, de, ate) { feitos, total ->
                                progresso = if (total == 0) 1f else feitos / total.toFloat()
                            }
                        } catch (t: Throwable) {
                            avisos.showSnackbar("Não foi possível gerar o PDF: ${t.message}")
                        } finally {
                            gerando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (gerando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Gerar PDF")
            }
        }
    }

    pronto?.let { (uri, r) ->
        AlertDialog(
            onDismissRequest = { pronto = null },
            title = { Text("Auditoria pronta") },
            text = {
                Text(
                    "Salva em Downloads/Bolao: ${r.comprovantes} comprovante(s), ${r.anexos} anexo(s), ${r.paginas} página(s)." +
                        if (r.problemas.isEmpty()) "\n\nTudo confere." else "\n\nAtenção:\n" + r.problemas.joinToString("\n") { "• $it" },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pronto = null
                    Compartilhar.enviarArquivo(ctx, uri, "application/pdf", "Enviar a auditoria")
                }) { Text("Enviar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pronto = null }) { Text("Fechar") }
                    TextButton(onClick = {
                        pronto = null
                        Compartilhar.abrir(ctx, uri, "application/pdf")
                    }) { Text("Abrir") }
                }
            },
        )
    }
}

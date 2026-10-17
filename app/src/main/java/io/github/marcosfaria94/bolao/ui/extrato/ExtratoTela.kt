package io.github.marcosfaria94.bolao.ui.extrato

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PreviaMensagem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Mês que a tela abre: o atual no último dia; antes disso, o anterior se ainda não teve extrato. */
fun mesPadraoDoExtrato(estado: Estado, hoje: LocalDate): YearMonth {
    val atual = YearMonth.from(hoje)
    val anterior = atual.minusMonths(1)
    return when {
        Calendario.ehUltimoDiaDoMes(hoje) -> atual
        estado.extratos.none { it.mes == anterior.toString() } && estado.lancamentos.any { YearMonth.from(it.data) == anterior } -> anterior
        else -> atual
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtratoTela(estado: Estado, aoConcluir: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()

    var mes by remember { mutableStateOf(mesPadraoDoExtrato(estado, hoje)) }
    var informado by remember { mutableLongStateOf(Operacoes.saldo(estado)) }
    var confirmar by remember { mutableStateOf(false) }
    var desfazer by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }

    val conferido = Operacoes.conferirSaldo(estado, mes, informado)
    val previa = remember(estado, mes, informado) {
        Operacoes.textoExtrato(Operacoes.registrarExtrato(estado, mes, informado, hoje, System.currentTimeMillis()), mes)
    }
    val existente = estado.extratos.firstOrNull { it.mes == mes.toString() }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(YearMonth.from(hoje).minusMonths(1), YearMonth.from(hoje)).forEach { opcao ->
                FilterChip(
                    selected = mes == opcao,
                    onClick = { mes = opcao },
                    label = { Text(Datas.mesAno(opcao)) },
                )
            }
        }
        if (!Calendario.ehUltimoDiaDoMes(hoje) && mes == YearMonth.from(hoje)) {
            Aviso("O extrato é feito no último dia do mês (${Datas.longa(mes.atEndOfMonth())}). Dá para adiantar, mas o rendimento pode mudar até lá.")
        }
        existente?.let {
            val quando = Instant.ofEpochMilli(it.criadoEm).atZone(ZoneId.systemDefault()).toLocalDate()
            Aviso("O extrato de ${Datas.mesAno(mes)} já foi registrado em ${Datas.longa(quando)}. Registrar de novo substitui o rendimento lançado.")
        }

        Text(
            "Abra a caixinha do bolão no Nubank e digite o saldo que aparece agora. O app calcula o rendimento pela diferença.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CampoDinheiro(
            centavos = informado,
            aoMudar = { informado = it },
            rotulo = "Saldo líquido da caixinha agora",
            modifier = Modifier.fillMaxWidth(),
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp)) {
                LinhaValor("Saldo calculado pelo app", Dinheiro.rs(conferido.calculado))
                LinhaValor("Saldo da caixinha", Dinheiro.rs(conferido.informado))
                LinhaValor(
                    "Rendimento líquido de ${Datas.mesAno(mes)}",
                    (if (conferido.rendimento < 0) "− " else "") + Dinheiro.rs(kotlin.math.abs(conferido.rendimento)),
                    destaque = true,
                )
            }
        }
        conferido.alerta?.let { Aviso("Possível inconsistência: $it", alerta = true) }

        PreviaMensagem(previa)

        Button(
            enabled = !enviando,
            onClick = { confirmar = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Registrar e enviar no WhatsApp") }
        if (existente != null) {
            TextButton(onClick = { desfazer = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Desfazer o extrato de ${Datas.mesAno(mes)}", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmar) {
        DialogoConfirmar(
            titulo = "Registrar o extrato de ${Datas.mesAno(mes)}?",
            texto = "Lança o rendimento de ${Dinheiro.rs(conferido.rendimento)} e abre o WhatsApp com o extrato." +
                (conferido.alerta?.let { "\n\nAtenção: $it" } ?: ""),
            confirmar = "Registrar",
            aoConfirmar = {
                confirmar = false
                enviando = true
                escopo.launch {
                    try {
                        val novo = repo.atualizar { Operacoes.registrarExtrato(it, mes, informado, hoje, System.currentTimeMillis()) }
                        Compartilhar.whatsapp(ctx, Operacoes.textoExtrato(novo, mes))
                        aoConcluir()
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível registrar: ${t.message}")
                    } finally {
                        enviando = false
                    }
                }
            },
            aoFechar = { confirmar = false },
        )
    }
    if (desfazer) {
        DialogoConfirmar(
            titulo = "Desfazer o extrato de ${Datas.mesAno(mes)}?",
            texto = "O rendimento lançado para o mês sai do saldo.",
            confirmar = "Desfazer",
            aoConfirmar = {
                desfazer = false
                escopo.launch { repo.atualizar { Operacoes.desfazerExtrato(it, mes) } }
            },
            aoFechar = { desfazer = false },
        )
    }
}

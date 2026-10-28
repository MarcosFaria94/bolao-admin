package io.github.marcosfaria94.bolao.ui.rateio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.data.Rateio
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BotoesDeRegistro
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.copiar
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Rateio: o Extra (o que pode ser rateado), o valor total e a chave Pix de cada integrante para
 * fazer os Pix. Desde a 0.10 o rateio é registrado antes dos Pix: os comprovantes dos Pix são
 * anexados depois, na aba Comprovantes (o app confere o valor de cada um com a cota), e a
 * conferência cria o comprovante do bolão. Até lá, o rateio pode ser descartado.
 */
@Composable
fun RateioTela(estado: Estado, abrir: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()

    val saldo = Operacoes.saldo(estado)
    val provisao = Operacoes.provisao(estado, hoje)
    val extra = Operacoes.extra(estado, hoje)
    // Começa no Extra (e volta a ele quando o saldo muda, ex.: depois de registrar).
    var pedido by remember(extra) { mutableLongStateOf(extra.coerceAtLeast(0)) }
    var confirmar by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }
    var descartar by remember { mutableStateOf<Rateio?>(null) }

    val calculo = Operacoes.calcularRateio(estado, hoje, pedido)
    val acima = pedido > extra
    val esperando = estado.rateios.filter { it.pendenteDeAnexo }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp)) {
                LinhaValor("Saldo líquido", Dinheiro.rs(saldo))
                LinhaValor("Provisionado", Dinheiro.rs(provisao.provisionado))
                LinhaValor("Extra", (if (extra < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(extra)))
            }
        }

        // Rateios registrados que ainda esperam os Pix (ou a conferência).
        esperando.forEach { r ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Rateio Nº${r.numeroNoMes} · ${Dinheiro.rs(r.cota)} para cada um", style = MaterialTheme.typography.titleMedium)
                    val faltam = estado.pessoas.filter { !r.pix.containsKey(it.id) }
                    Text(
                        if (faltam.isEmpty()) "Todos os Pix anexados: falta a conferência (aba Comprovantes)."
                        else "Esperando o Pix de " + faltam.joinToString(", ") { it.apelido } + " (aba Comprovantes).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { abrir(Pendencias.ABA_COMPROVANTES) }) { Text("Comprovantes") }
                        TextButton(onClick = { descartar = r }) { Text("Descartar rateio", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }

        if (extra <= 0) {
            Aviso("Não há valor a ser rateado: o saldo líquido não passa do provisionado para as apostas do mês.", alerta = true)
        } else {
            // Vermelho, com a explicação embaixo, só quando o valor passa do Extra.
            CampoDinheiro(
                centavos = pedido,
                aoMudar = { pedido = it },
                rotulo = "Valor total",
                erro = acima,
                ajuda = if (acima) "Valor acima de ${Dinheiro.rs(extra)} não pode ser rateado." else null,
                modifier = Modifier.fillMaxWidth(),
            )
            estado.pessoas.forEachIndexed { i, p ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pix ${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${p.apelido} · ${Dinheiro.rs(calculo.cota)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Chave: ${p.chavePix}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { copiar(ctx, "Chave Pix", p.chavePix) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar chave Pix")
                        }
                    }
                }
            }
            Aviso("Registre o rateio, faça os ${estado.pessoas.size} Pix e anexe os comprovantes na aba Comprovantes.")
            BotoesDeRegistro(
                habilitado = !acima && calculo.cota > 0,
                trabalhando = trabalhando,
                aoVisualizar = { vendo = true },
                aoRegistrar = { confirmar = true },
                visualizarHabilitado = !acima && calculo.cota > 0,
                textoRegistrar = "Registrar rateio",
            )
        }
    }

    if (vendo) {
        VisualizadorComprovante(
            estado,
            Operacoes.tituloRateio(estado, hoje),
            aoFechar = { vendo = false },
            previa = Recibos.previa(estado, TipoComprovante.RATEIO, Operacoes.textoRateio(estado, hoje, calculo.total), estado.pessoas.size),
            anexos = emptyList(),
        )
    }

    if (confirmar) {
        DialogoConfirmar(
            titulo = "Registrar o rateio?",
            texto = "Sai ${Dinheiro.rs(calculo.total)} do saldo (${Dinheiro.rs(calculo.cota)} para cada um). " +
                "Depois, anexe os comprovantes dos ${estado.pessoas.size} Pix na aba Comprovantes: até a conferência, " +
                "o rateio ainda pode ser descartado.",
            confirmar = "Registrar",
            aoConfirmar = {
                confirmar = false
                trabalhando = true
                escopo.launch {
                    try {
                        val agora = System.currentTimeMillis()
                        val valor = pedido
                        repo.atualizar { Operacoes.registrarRateio(it, hoje, valor, agora) }
                        avisos.showSnackbar("Rateio registrado. Faça os Pix e anexe os comprovantes na aba Comprovantes.")
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

    descartar?.let { r ->
        DialogoConfirmar(
            titulo = "Descartar o rateio Nº${r.numeroNoMes}?",
            texto = "O rateio ainda não foi conferido: ele sai do app e ${Dinheiro.rs(r.cota * estado.pessoas.size)} volta ao saldo." +
                (if (r.pix.isNotEmpty()) " Os comprovantes de Pix anexados também são apagados." else ""),
            confirmar = "Descartar",
            aoConfirmar = {
                descartar = null
                escopo.launch {
                    try {
                        repo.atualizar { Operacoes.descartarRateio(it, r.numero) }
                        r.pix.values.forEach { repo.apagarSeSolto(it) }
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível descartar: ${t.message}")
                    }
                }
            },
            aoFechar = { descartar = null },
        )
    }
}

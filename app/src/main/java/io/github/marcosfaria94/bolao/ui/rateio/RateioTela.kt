package io.github.marcosfaria94.bolao.ui.rateio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.ui.comum.AVISO_DEFINITIVO
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BotoesDeRegistro
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.Historico
import io.github.marcosfaria94.bolao.ui.comum.LinhaAnexo
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.copiar
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun RateioTela(estado: Estado, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val teste by repo.modoTeste.collectAsStateWithLifecycle()

    val saldo = Operacoes.saldo(estado)
    val provisao = Operacoes.provisao(estado, hoje)
    val extra = Operacoes.extra(estado, hoje)
    // Começa no Extra (e volta a ele quando o saldo muda, ex.: depois de registrar).
    var pedido by remember(extra) { mutableLongStateOf(extra.coerceAtLeast(0)) }
    val comprovantes = remember { mutableStateMapOf<String, Arquivo>() }
    var paraQuem by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }

    val calculo = Operacoes.calcularRateio(estado, hoje, pedido)
    val acima = pedido > extra
    val faltam = estado.pessoas.count { comprovantes[it.id] == null }

    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pessoa = paraQuem
        if (uri != null && pessoa != null) escopo.launch {
            try {
                comprovantes[pessoa] = repo.guardarArquivo(uri, "pix")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível anexar: ${t.message}")
            }
        }
    }

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
        }

        if (extra > 0) {
            if (teste) {
                TextButton(onClick = {
                    escopo.launch {
                        try {
                            estado.pessoas.forEach { p -> comprovantes[p.id] = ModoTeste.pix(ctx, p, calculo.cota) }
                        } catch (t: Throwable) {
                            avisos.showSnackbar("Não foi possível criar os comprovantes de exemplo: ${t.message}")
                        }
                    }
                }) { Text("Usar comprovantes de Pix de exemplo") }
            }
            estado.pessoas.forEachIndexed { i, p ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        val arq = comprovantes[p.id]
                        LinhaAnexo(
                            titulo = "Comprovante do Pix",
                            nomeArquivo = arq?.nomeOriginal,
                            aoAnexar = {
                                paraQuem = p.id
                                escolher.launch(arrayOf("application/pdf", "image/*"))
                            },
                            arquivo = arq?.let { repo.arquivo(it) },
                            mime = arq?.mime,
                        )
                    }
                }
            }
            if (faltam > 0) Aviso("Falta anexar $faltam comprovante(s) do Pix.", alerta = true)
            BotoesDeRegistro(
                habilitado = !acima && calculo.cota > 0 && faltam == 0,
                trabalhando = trabalhando,
                aoVisualizar = { vendo = true },
                aoRegistrar = { confirmar = true },
                visualizarHabilitado = !acima && calculo.cota > 0,
            )
        }

        Spacer(Modifier.height(8.dp))
        Historico(
            titulo = "Histórico de rateios",
            estado = estado,
            filtro = { it.tipo == TipoLancamento.RATEIO },
            abrir = abrir,
        )
    }

    if (vendo) {
        VisualizadorComprovante(
            estado,
            Operacoes.tituloRateio(estado, hoje),
            aoFechar = { vendo = false },
            previa = Recibos.previa(estado, TipoComprovante.RATEIO, Operacoes.textoRateio(estado, hoje, calculo.total), estado.pessoas.size),
            anexos = estado.pessoas.mapNotNull { comprovantes[it.id] },
        )
    }

    if (confirmar) {
        DialogoConfirmar(
            titulo = "Registrar o rateio?",
            texto = "Sai ${Dinheiro.rs(calculo.total)} do saldo (${Dinheiro.rs(calculo.cota)} para cada um). " +
                "Em seguida o WhatsApp abre com o comprovante e os Pix.\n\n$AVISO_DEFINITIVO",
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmar = false
                trabalhando = true
                escopo.launch {
                    try {
                        val lista = estado.pessoas.mapNotNull { comprovantes[it.id] }
                        val agora = System.currentTimeMillis()
                        val valor = pedido
                        registrarComprovante(ctx, { Operacoes.registrarRateio(it, hoje, valor, lista, agora) })
                        comprovantes.clear()
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

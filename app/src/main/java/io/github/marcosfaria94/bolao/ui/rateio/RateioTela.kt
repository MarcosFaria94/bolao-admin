package io.github.marcosfaria94.bolao.ui.rateio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.CalculoRateio
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.ui.comum.Anexo
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LinhaAnexo
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PreviaMensagem
import io.github.marcosfaria94.bolao.ui.comum.TituloSecao
import io.github.marcosfaria94.bolao.ui.comum.copiar
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun RateioTela(estado: Estado, aoConcluir: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val sugestao = remember(estado.lancamentos.size) { Operacoes.sugestaoProvisionado(estado, hoje) }
    val saldo = Operacoes.saldo(estado)

    var provisionado by remember { mutableLongStateOf(sugestao.provisionado) }
    val comprovantes = remember { mutableStateMapOf<String, Arquivo>() }
    var paraQuem by remember { mutableStateOf<String?>(null) }
    var confirmar by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var desfazer by remember { mutableStateOf(false) }

    val n = estado.pessoas.size.coerceAtLeast(1)
    val calculo = CalculoRateio.calcular(saldo, provisionado, n)

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
                Text("Rateio Nº${estado.proximoRateio}", style = MaterialTheme.typography.titleMedium)
                LinhaValor("Saldo líquido", Dinheiro.rs(saldo))
                LinhaValor(
                    "Apostas que faltam no mês",
                    "${sugestao.apostasRestantes} × ${Dinheiro.rs(sugestao.valorPorAposta)}",
                )
                LinhaValor("Aportes do mês (limite)", Dinheiro.rs(sugestao.aportesDoMes))
                LinhaValor("Provisionado sugerido", Dinheiro.rs(sugestao.provisionado))
            }
        }
        CampoDinheiro(
            centavos = provisionado,
            aoMudar = { provisionado = it },
            rotulo = "Provisionado",
            ajuda = "Valor guardado para as apostas que faltam no mês, nunca acima dos aportes do mês.",
            modifier = Modifier.fillMaxWidth(),
        )

        if (calculo == null) {
            Aviso(
                "Sem rateio agora: o saldo líquido (${Dinheiro.rs(saldo)}) precisa ser maior que o provisionado (${Dinheiro.rs(provisionado)}).",
                alerta = true,
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    LinhaValor("Saldo − Provisionado", Dinheiro.rs(calculo.disponivel))
                    LinhaValor("Cota de cada integrante (÷ $n, para baixo)", Dinheiro.rs(calculo.cota), destaque = true)
                    LinhaValor("Total do rateio", Dinheiro.rs(calculo.total))
                    if (calculo.sobra > 0) LinhaValor("Sobra que fica no saldo", Dinheiro.rs(calculo.sobra))
                    LinhaValor("Saldo final", Dinheiro.rs(calculo.saldoFinal))
                }
            }

            TituloSecao("Pix de cada integrante")
            estado.pessoas.forEach { p ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${p.apelido} · ${Dinheiro.rs(calculo.cota)}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Pix: ${p.chavePix}",
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

            val texto = Mensagens.rateio(estado.proximoRateio, hoje, saldo, calculo.total)
            PreviaMensagem(texto, rodape = "Os ${estado.pessoas.size} comprovantes do Pix vão juntos, como anexo.")
            val faltam = estado.pessoas.count { comprovantes[it.id] == null }
            if (faltam > 0) Aviso("Falta anexar $faltam comprovante(s) do Pix.", alerta = true)
            Button(
                enabled = faltam == 0 && !enviando,
                onClick = { confirmar = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Registrar e enviar no WhatsApp") }

            if (confirmar) {
                DialogoConfirmar(
                    titulo = "Registrar o rateio Nº${estado.proximoRateio}?",
                    texto = "Desconta ${Dinheiro.rs(calculo.total)} do saldo (${Dinheiro.rs(calculo.cota)} para cada um) e abre o WhatsApp com a mensagem e os comprovantes.",
                    confirmar = "Registrar",
                    aoConfirmar = {
                        confirmar = false
                        enviando = true
                        escopo.launch {
                            try {
                                val lista = estado.pessoas.mapNotNull { comprovantes[it.id] }
                                repo.atualizar {
                                    Operacoes.registrarRateio(it, hoje, provisionado, calculo.cota, lista, System.currentTimeMillis())
                                }
                                Compartilhar.whatsapp(ctx, texto, lista.map { Anexo(repo.arquivo(it), it.mime) })
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
        }

        estado.rateios.maxByOrNull { it.numero }?.let { ultimo ->
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "Último rateio: Nº${ultimo.numero} em ${Datas.longa(ultimo.data)} · ${Dinheiro.rs(ultimo.cota)} para cada um",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { desfazer = true }) { Text("Desfazer o rateio Nº${ultimo.numero}", color = MaterialTheme.colorScheme.error) }
            if (desfazer) {
                DialogoConfirmar(
                    titulo = "Desfazer o rateio Nº${ultimo.numero}?",
                    texto = "Os ${Dinheiro.rs(ultimo.cota * estado.pessoas.size)} voltam ao saldo. Use só se o registro foi um engano.",
                    confirmar = "Desfazer",
                    aoConfirmar = {
                        desfazer = false
                        escopo.launch { repo.atualizar { Operacoes.desfazerRateio(it, ultimo.numero) } }
                    },
                    aoFechar = { desfazer = false },
                )
            }
        }
    }
}

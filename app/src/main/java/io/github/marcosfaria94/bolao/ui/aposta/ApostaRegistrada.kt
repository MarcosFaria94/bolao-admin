package io.github.marcosfaria94.bolao.ui.aposta

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.JogoLoteca
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.imagem.ImagemConferencia
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.AVISO_DEFINITIVO
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LinhaAnexo
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import io.github.marcosfaria94.bolao.ui.tema.corConferir
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Resultado do sorteio para a apuração: da Caixa ou digitado pelo Administrador. */
private sealed interface Busca {
    data object Carregando : Busca
    data class Pronto(val concurso: Concurso, val manual: Boolean) : Busca
    data class Falhou(val motivo: String) : Busca
}

/** O que está aberto no visualizador de comprovantes. */
private sealed interface Vendo {
    data class Registrado(val comprovante: Comprovante) : Vendo
    data object PreviaApuracao : Vendo
}

/**
 * Aposta registrada, na página da modalidade (embaixo do quadro com o prêmio e o sorteio dela):
 * "Aposta Nº1", o registro, a situação ("Sorteio hoje"...) e, depois do sorteio, se acumulou; os
 * números (com os acertos, quando o resultado sai); a apuração quando ela ainda falta; e, embaixo,
 * os dois comprovantes (aposta e apuração) para ver e enviar. Vai dentro da coluna da página (não
 * tem rolagem própria). O valor do prêmio aparece aqui (o aviso do celular não traz o valor).
 */
@Composable
fun SecaoApostaRegistrada(estado: Estado, a: Aposta, abrir: (String) -> Unit = {}, aoSair: () -> Unit = {}) {
    // Registrada e ainda sem a conferência do comprovante da Caixa: espera o comprovante (e pode ser descartada).
    if (a.pendenteDeAnexo) {
        ApostaEsperandoComprovante(estado, a, abrir, aoSair)
        return
    }
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val m = a.modalidade
    val ap = a.apuracao
    val apurada = ap?.confirmadaEm != null
    val individual = estado.individual
    // Depois do sorteio (ou quando a conferência automática já chegou), a apuração entra aqui.
    val apurar = !apurada && (ap != null || (a.dataSorteio != null && !a.dataSorteio.isAfter(hoje)))
    var excluir by remember { mutableStateOf(false) }

    var busca by remember(a.id) { mutableStateOf<Busca>(Busca.Carregando) }
    var premio by remember(a.id) { mutableLongStateOf(0L) }
    var premioEditado by remember(a.id) { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf<Vendo?>(null) }

    suspend fun buscar() {
        busca = Busca.Carregando
        busca = try {
            val c = CaixaApi.concurso(m, a.concurso)
            if (c.temResultado) Busca.Pronto(c, manual = false) else Busca.Falhou("O resultado do concurso ${a.concurso} ainda não saiu.")
        } catch (t: Throwable) {
            Busca.Falhou("Não foi possível consultar a Caixa: ${t.message}")
        }
    }
    LaunchedEffect(a.id, apurar) { if (apurar) buscar() }

    val pronto = (busca as? Busca.Pronto)?.takeIf { apurar }
    val conferencia = pronto?.let { Conferencia.conferir(m, a.palpite, it.concurso, a.bilheteFederal, a.fracoesFederal) }
    // Mesmo instante enquanto o resultado não muda (evita refazer a imagem a cada toque).
    val instante = remember(conferencia) { System.currentTimeMillis() }
    LaunchedEffect(conferencia) {
        if (conferencia != null && !premioEditado) premio = conferencia.premio
        // Guarda a conferência (sem confirmar): é ela que muda a situação na lista de apostas.
        if (conferencia != null && pronto?.manual == false) {
            val nova = Operacoes.apuracaoDe(pronto.concurso, conferencia, conferencia.premio, System.currentTimeMillis())
            repo.enfileirar { Operacoes.guardarConferencia(it, a.id, nova) }
        }
    }
    // Apuração montada agora (ainda sem registrar). Resultado digitado não diz se acumulou.
    val apNova: Apuracao? = if (pronto != null && conferencia != null) {
        Operacoes.apuracaoDe(pronto.concurso, conferencia, premio, instante).let { if (pronto.manual) it.copy(acumulou = null) else it }
    } else {
        null
    }
    val apTela = if (apurada) ap else apNova ?: ap

    // O valor do prêmio aparece aqui (na lista e no aviso do celular, não).
    val s = situacaoDe(a, hoje)
    val premioNaTela = if (apurada) ap?.premio ?: 0 else (apNova ?: ap)?.premio ?: 0
    val situacao = when {
        s == Situacao.PREMIADA -> "Aposta premiada (${Dinheiro.rs(premioNaTela)})"
        s == Situacao.PREMIADA_A_APURAR && premioNaTela > 0 -> "Aposta premiada (${Dinheiro.rs(premioNaTela)}) · aguardando apuração"
        else -> textoSituacao(a, s, hoje)
    }
    // Depois do sorteio: se o prêmio principal acumulou (da Caixa; o resultado digitado não diz).
    val acumulou = apTela?.acumulou

    // A aposta: o número, o registro, a situação e os números (com os acertos, depois do sorteio).
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tituloDaAposta(a), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp))
            Column {
                LinhaInfo("Registro", Datas.relativa(a.dataAposta, hoje))
                LinhaInfo("Situação", situacao, destaque = true, cor = corDaSituacao(s))
                acumulou?.let { LinhaInfo("Acumulou", if (it) "Sim" else "Não") }
            }
            ImagemDaAposta(a, apTela)
        }
    }

    if (apurar) {
        when (val b = busca) {
            Busca.Carregando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("  Buscando o resultado na Caixa...")
            }
            is Busca.Falhou -> {
                Aviso(b.motivo, alerta = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { escopo.launch { buscar() } }) { Text("Tentar de novo") }
                    Button(onClick = { busca = Busca.Pronto(concursoVazio(a), manual = true) }) { Text("Informar à mão") }
                }
            }
            is Busca.Pronto -> Unit
        }
        if (pronto != null && conferencia != null) {
            if (pronto.manual) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resultado do sorteio", style = MaterialTheme.typography.titleMedium)
                        ResultadoManual(a, pronto.concurso) { busca = Busca.Pronto(it, manual = true) }
                    }
                }
            } else if (conferencia.observacoes.isNotEmpty()) {
                TextButton(onClick = { busca = Busca.Pronto(pronto.concurso, manual = true) }) { Text("Informar o resultado à mão") }
            }
            conferencia.observacoes.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            CampoDinheiro(
                centavos = premio,
                aoMudar = {
                    premio = it
                    premioEditado = true
                },
                rotulo = if (individual) "Prêmio neste jogo" else "Prêmio do bolão neste jogo",
                ajuda = if (conferencia.precisaConfirmarValor) "Confira o valor no app da Caixa." else "Valor calculado pela tabela de prêmios da Caixa.",
                modifier = Modifier.fillMaxWidth(),
            )
            if (conferencia.precisaConfirmarValor && conferencia.premiada) {
                Aviso("A Caixa não informou o valor desta faixa: digite o prêmio que aparece no app da Caixa.", alerta = true)
            }
        }
    }

    if (individual) {
        // Modo individual: sem comprovantes; a apuração é um toque e a aposta pode ser excluída.
        if (apNova != null) {
            Button(onClick = { confirmar = true }, enabled = !trabalhando, modifier = Modifier.fillMaxWidth()) {
                if (trabalhando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Apurar")
            }
        }
        TextButton(onClick = { excluir = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Excluir aposta", color = MaterialTheme.colorScheme.error)
        }
    }
    if (!individual) Spacer(Modifier.height(4.dp))
    if (!individual) Text("Comprovantes", style = MaterialTheme.typography.titleMedium)
    if (!individual) Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val cAposta = Operacoes.comprovanteDaAposta(estado, a)
            LinhaComprovante(
                titulo = "Comprovante da aposta",
                detalhe = detalheDoEnvio(estado, cAposta),
                enviado = cAposta != null && estado.enviado(cAposta),
                habilitado = cAposta != null,
                aoVisualizar = { cAposta?.let { vendo = Vendo.Registrado(it) } },
                aoEnviar = { cAposta?.let { c -> escopo.launch { Compartilhar.comprovante(ctx, estado, c) } } },
                textoEnviar = if (cAposta != null && estado.enviado(cAposta)) "Enviar de novo" else "Enviar",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val cApuracao = Operacoes.comprovanteDaApuracao(estado, a)
            when {
                apurada -> LinhaComprovante(
                    titulo = "Comprovante da apuração",
                    detalhe = detalheDoEnvio(estado, cApuracao),
                    enviado = cApuracao != null && estado.enviado(cApuracao),
                    habilitado = cApuracao != null,
                    aoVisualizar = { cApuracao?.let { vendo = Vendo.Registrado(it) } },
                    aoEnviar = { cApuracao?.let { c -> escopo.launch { Compartilhar.comprovante(ctx, estado, c) } } },
                    textoEnviar = if (cApuracao != null && estado.enviado(cApuracao)) "Enviar de novo" else "Enviar",
                )
                apNova != null -> LinhaComprovante(
                    titulo = "Comprovante da apuração",
                    detalhe = "Ainda não registrado",
                    enviado = false,
                    habilitado = true,
                    aoVisualizar = { vendo = Vendo.PreviaApuracao },
                    aoEnviar = { confirmar = true },
                    textoEnviar = "Apurar e enviar",
                    trabalhando = trabalhando,
                )
                else -> LinhaComprovante(
                    titulo = "Comprovante da apuração",
                    detalhe = if (apurar) "Esperando o resultado do sorteio" else "Sai depois do sorteio",
                    enviado = false,
                    habilitado = false,
                    aoVisualizar = {},
                    aoEnviar = {},
                    textoEnviar = "Enviar",
                )
            }
        }
    }

    when (val v = vendo) {
        is Vendo.Registrado -> VisualizadorComprovante(estado, v.comprovante.titulo, aoFechar = { vendo = null }, comprovante = v.comprovante)
        Vendo.PreviaApuracao -> apNova?.let { registro ->
            val acertos by produceState<Bitmap?>(null, registro) {
                value = withContext(Dispatchers.Default) { runCatching { ImagemConferencia.gerar(ctx, a, registro) }.getOrNull() }
            }
            VisualizadorComprovante(
                estado,
                Mensagens.tituloApuracao(a.numero, hoje),
                aoFechar = { vendo = null },
                previa = Recibos.previa(
                    estado, TipoComprovante.APURACAO, Operacoes.textoApuracao(estado, a, premio, hoje), 1,
                    resultado = Recibos.resultadoDe(a, registro),
                ),
                imagensExtras = listOfNotNull(acertos?.let { "Anexo: imagem dos acertos" to it }),
            )
        }
        null -> Unit
    }

    if (excluir) {
        DialogoConfirmar(
            titulo = "Excluir a aposta Nº${a.numero}?",
            texto = "A aposta sai do app" + (if ((ap?.premio ?: 0) > 0 && apurada) ", junto com o prêmio dela" else "") +
                ", e ${Dinheiro.rs(a.valor)} volta ao saldo.",
            confirmar = "Excluir",
            aoConfirmar = {
                excluir = false
                escopo.launch {
                    try {
                        repo.atualizar { Operacoes.descartarAposta(it, a.id) }
                        aoSair()
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível excluir: ${t.message}")
                    }
                }
            },
            aoFechar = { excluir = false },
        )
    }

    if (confirmar && individual) {
        val registro = apNova
        DialogoConfirmar(
            titulo = "Apurar a aposta Nº${a.numero}?",
            texto = if (premio > 0) "Soma ${Dinheiro.rs(premio)} ao saldo." else "Registra que não houve prêmio.",
            confirmar = "Apurar",
            aoConfirmar = {
                confirmar = false
                if (registro != null) {
                    trabalhando = true
                    escopo.launch {
                        try {
                            repo.atualizar { Operacoes.confirmarApuracao(it, a.id, registro, hoje, System.currentTimeMillis()) }
                        } catch (t: Throwable) {
                            avisos.showSnackbar("Não foi possível apurar: ${t.message}")
                        } finally {
                            trabalhando = false
                        }
                    }
                }
            },
            aoFechar = { confirmar = false },
        )
    }

    if (confirmar && !individual) {
        val registro = apNova
        DialogoConfirmar(
            titulo = "Confirmar a apuração da aposta Nº${a.numero}?",
            texto = (
                if (premio > 0) "Soma ${Dinheiro.rs(premio)} ao saldo. Em seguida o WhatsApp abre com o comprovante e a imagem dos acertos."
                else "Registra que não houve prêmio. Em seguida o WhatsApp abre com o comprovante e a imagem dos acertos."
                ) + "\n\n" + AVISO_DEFINITIVO,
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmar = false
                if (registro != null) {
                    trabalhando = true
                    escopo.launch {
                        try {
                            val agora = System.currentTimeMillis()
                            val imagem = repo.guardarGerado("acertos", "acertos-${m.sigla}-${a.concurso}.png", "image/png") { f ->
                                ImagemConferencia.salvar(ctx, a, registro, f)
                            }
                            registrarComprovante(ctx, { Operacoes.confirmarApuracao(it, a.id, registro, hoje, agora, imagem) })
                        } catch (t: Throwable) {
                            avisos.showSnackbar("Não foi possível confirmar: ${t.message}")
                        } finally {
                            trabalhando = false
                        }
                    }
                }
            },
            aoFechar = { confirmar = false },
        )
    }
}

/**
 * A aposta registrada que ainda espera o comprovante da Caixa (ou a conferência dele): o registro,
 * os números, o comprovante anexado (se já veio), o atalho para a aba Comprovantes e o descarte
 * (até a conferência, o valor volta ao saldo).
 */
@Composable
private fun ApostaEsperandoComprovante(estado: Estado, a: Aposta, abrir: (String) -> Unit, aoSair: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    var descartar by remember { mutableStateOf(false) }
    val anexado = a.comprovante
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tituloDaAposta(a), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp))
            Column {
                LinhaInfo("Registro", Datas.relativa(a.dataAposta, hoje))
                LinhaInfo(
                    "Situação",
                    if (anexado == null) "Esperando o comprovante da Caixa" else "Comprovante anexado: falta a conferência",
                    destaque = true,
                    cor = if (anexado == null) MaterialTheme.colorScheme.error else corConferir(),
                )
            }
            ImagemDaAposta(a, null)
        }
    }
    a.notasDoAnexo.forEach { Aviso(it) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (anexado != null) {
                LinhaAnexo(
                    titulo = "Comprovante da Caixa",
                    nomeArquivo = anexado.nomeOriginal,
                    aoAnexar = { abrir(Pendencias.ABA_COMPROVANTES) },
                    arquivo = repo.arquivo(anexado),
                    mime = anexado.mime,
                )
            } else {
                Text(
                    "Faça a aposta no app Loterias CAIXA com estes números e anexe o comprovante na aba Comprovantes " +
                        "(dá para escolher vários PDFs de uma vez). Sem o comprovante conferido, a aposta não vale para o bolão.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { abrir(Pendencias.ABA_COMPROVANTES) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (anexado == null) "Anexar na aba Comprovantes" else "Conferir na aba Comprovantes")
            }
        }
    }
    TextButton(onClick = { descartar = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Descartar aposta", color = MaterialTheme.colorScheme.error)
    }
    if (descartar) {
        DialogoConfirmar(
            titulo = "Descartar a aposta Nº${a.numero}?",
            texto = "A aposta ainda não foi conferida: ela sai do app e ${Dinheiro.rs(a.valor)} volta ao saldo." +
                (if (anexado != null) " O comprovante anexado também é apagado." else ""),
            confirmar = "Descartar",
            aoConfirmar = {
                descartar = false
                escopo.launch {
                    try {
                        repo.atualizar { Operacoes.descartarAposta(it, a.id) }
                        anexado?.let { repo.apagarSeSolto(it) }
                        avisos.showSnackbar("Aposta descartada: ${Dinheiro.rs(a.valor)} voltou ao saldo.")
                        aoSair()
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível descartar: ${t.message}")
                    }
                }
            },
            aoFechar = { descartar = false },
        )
    }
}

/** "Enviado ao grupo", "Ainda não enviado ao grupo" ou, nos registros antigos, "sem comprovante". */
private fun detalheDoEnvio(e: Estado, c: Comprovante?): String = when {
    c == null -> "Registro antigo, sem comprovante"
    e.enviado(c) -> "Enviado ao grupo"
    else -> "Ainda não enviado ao grupo"
}

/** Um comprovante da aposta registrada: nome, se já foi ao grupo, e os botões para ver e enviar. */
@Composable
private fun LinhaComprovante(
    titulo: String,
    detalhe: String,
    enviado: Boolean,
    habilitado: Boolean,
    aoVisualizar: () -> Unit,
    aoEnviar: () -> Unit,
    textoEnviar: String,
    trabalhando: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (enviado) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = if (enviado) corEntrada() else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.padding(start = 10.dp)) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = aoVisualizar,
                enabled = habilitado && !trabalhando,
                modifier = Modifier.weight(1f),
            ) { Text("Visualizar") }
            Button(
                onClick = aoEnviar,
                enabled = habilitado && !trabalhando,
                modifier = Modifier.weight(1f),
            ) {
                if (trabalhando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(textoEnviar)
            }
        }
    }
}

private fun concursoVazio(a: Aposta) = Concurso(
    modalidade = a.modalidade,
    numero = a.concurso,
    data = a.dataSorteio,
    dezenas = if (a.modalidade.formato == Formato.COLUNAS) List(Volante.COLUNAS_SUPER_SETE) { -1 } else emptyList(),
)

/** Digitação do resultado quando a Caixa não responde. */
@Composable
private fun ResultadoManual(a: Aposta, atual: Concurso, aoMudar: (Concurso) -> Unit) {
    val m = a.modalidade
    when (m.formato) {
        Formato.NUMEROS -> {
            val alvo = if (m == Modalidade.DUPLA_SENA) "1º sorteio" else "Números sorteados"
            Text("$alvo (${atual.dezenas.size} de ${m.qtdSorteada})", style = MaterialTheme.typography.labelLarge)
            GradeNumeros(
                m,
                situacao = { if (it in atual.dezenas) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                aoTocar = { n ->
                    val novo = if (n in atual.dezenas) atual.dezenas - n else if (atual.dezenas.size < m.qtdSorteada) (atual.dezenas + n).sorted() else atual.dezenas
                    aoMudar(atual.copy(dezenas = novo))
                },
            )
            if (m == Modalidade.DUPLA_SENA) {
                Text("2º sorteio (${atual.segundoSorteio.size} de 6)", style = MaterialTheme.typography.labelLarge)
                GradeNumeros(
                    m,
                    situacao = { if (it in atual.segundoSorteio) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                    aoTocar = { n ->
                        val s = atual.segundoSorteio
                        val novo = if (n in s) s - n else if (s.size < 6) (s + n).sorted() else s
                        aoMudar(atual.copy(segundoSorteio = novo))
                    },
                )
            }
            if (m.trevos > 0) {
                Text("Trevos sorteados (${atual.trevos.size} de ${m.trevos})", style = MaterialTheme.typography.labelLarge)
                GradeTrevos(
                    m,
                    situacao = { if (it in atual.trevos) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                    aoTocar = { t ->
                        val novo = if (t in atual.trevos) atual.trevos - t else if (atual.trevos.size < m.trevos) (atual.trevos + t).sorted() else atual.trevos
                        aoMudar(atual.copy(trevos = novo))
                    },
                )
            }
            if (m.extra != Extra.NENHUM) {
                OutlinedTextField(
                    value = atual.timeOuMes.orEmpty(),
                    onValueChange = { aoMudar(atual.copy(timeOuMes = it)) },
                    label = { Text(if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte sorteado" else "Time do Coração sorteado") },
                    supportingText = if (m.extra == Extra.MES_DE_SORTE) {
                        { Text(MESES_DE_SORTE.joinToString(", ")) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Formato.COLUNAS -> GradeColunas(
            m,
            situacao = { c, n -> if (atual.dezenas.getOrNull(c) == n) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
            aoTocar = { c, n ->
                val cols = MutableList(Volante.COLUNAS_SUPER_SETE) { i -> atual.dezenas.getOrNull(i) ?: -1 }
                cols[c] = n
                aoMudar(atual.copy(dezenas = cols))
            },
        )
        Formato.LOTECA -> {
            val resultados = (0 until Volante.JOGOS_LOTECA).map { atual.jogosLoteca.getOrNull(it)?.resultado.orEmpty() }
            GradeLoteca(
                m,
                rotulos = a.jogosLoteca,
                palpites = resultados,
                aoEscolher = { j, op ->
                    val jogos = (0 until Volante.JOGOS_LOTECA).map { i ->
                        val r = if (i == j) op else resultados[i]
                        val (g1, g2) = when (r) {
                            "1" -> 1 to 0
                            "2" -> 0 to 1
                            "X" -> 0 to 0
                            else -> null to null
                        }
                        JogoLoteca(i + 1, "", "", g1, g2)
                    }
                    aoMudar(atual.copy(jogosLoteca = jogos))
                },
            )
        }
        Formato.FEDERAL -> {
            (0 until 5).forEach { i ->
                OutlinedTextField(
                    value = atual.bilhetesFederal.getOrNull(i).orEmpty(),
                    onValueChange = { t ->
                        val lista = MutableList(5) { k -> atual.bilhetesFederal.getOrNull(k).orEmpty() }
                        lista[i] = t.filter(Char::isDigit).take(6)
                        aoMudar(atual.copy(bilhetesFederal = lista.takeIf { l -> l.any { it.isNotEmpty() } } ?: emptyList()))
                    },
                    label = { Text("${i + 1}º prêmio") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

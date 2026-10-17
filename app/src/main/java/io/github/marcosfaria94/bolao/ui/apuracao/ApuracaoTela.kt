package io.github.marcosfaria94.bolao.ui.apuracao

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.JogoLoteca
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.aposta.CabecalhoAposta
import io.github.marcosfaria94.bolao.ui.aposta.GradeColunas
import io.github.marcosfaria94.bolao.ui.aposta.GradeLoteca
import io.github.marcosfaria94.bolao.ui.aposta.GradeNumeros
import io.github.marcosfaria94.bolao.ui.aposta.GradeTrevos
import io.github.marcosfaria94.bolao.ui.aposta.SituacaoNumero
import io.github.marcosfaria94.bolao.ui.aposta.VolanteDaApuracao
import io.github.marcosfaria94.bolao.ui.comum.Anexo
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PreviaMensagem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private sealed interface Busca {
    data object Carregando : Busca
    data class Pronto(val concurso: Concurso, val manual: Boolean) : Busca
    data class Falhou(val motivo: String) : Busca
}

/** Monta o registro da apuração a partir do resultado e da conferência. */
private fun apuracaoDe(c: Concurso, r: Conferencia.Resultado, premio: Long, agora: Long) =
    Operacoes.apuracaoDe(c, r, premio, agora)

@Composable
fun ApuracaoTela(estado: Estado, id: String) {
    val a = estado.apostas.firstOrNull { it.id == id }
    if (a == null || !a.registrada) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não está registrada.") }
        return
    }
    if (a.apuracao?.confirmadaEm != null) {
        ApuracaoConfirmada(estado, a)
    } else {
        ApuracaoPendente(estado, a)
    }
}

@Composable
private fun ApuracaoPendente(estado: Estado, a: Aposta) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val m = a.modalidade

    var busca by remember { mutableStateOf<Busca>(Busca.Carregando) }
    var premio by remember { mutableLongStateOf(0L) }
    var premioEditado by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }

    suspend fun buscar() {
        busca = Busca.Carregando
        busca = try {
            val c = CaixaApi.concurso(m, a.concurso)
            if (c.temResultado) Busca.Pronto(c, manual = false) else Busca.Falhou("O resultado do concurso ${a.concurso} ainda não saiu.")
        } catch (t: Throwable) {
            Busca.Falhou("Não foi possível consultar a Caixa: ${t.message}")
        }
    }
    LaunchedEffect(a.id) { buscar() }

    val pronto = busca as? Busca.Pronto
    val conferencia = pronto?.let { Conferencia.conferir(m, a.palpite, it.concurso, a.bilheteFederal, a.fracoesFederal) }
    // Mesmo instante enquanto o resultado não muda (evita refazer a imagem a cada toque).
    val instante = remember(conferencia) { System.currentTimeMillis() }
    LaunchedEffect(conferencia) {
        if (conferencia != null && !premioEditado) premio = conferencia.premio
        // Guarda a conferência (sem confirmar) para o Painel já mostrar o resultado.
        if (conferencia != null && pronto?.manual == false) {
            val ap = apuracaoDe(pronto.concurso, conferencia, conferencia.premio, System.currentTimeMillis())
            repo.enfileirar { Operacoes.guardarConferencia(it, a.id, ap) }
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
        CabecalhoAposta(a, "#${a.numero} · ${m.nome} ${a.concurso}", "Apuração")

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
            }

            val ap = apuracaoDe(pronto.concurso, conferencia, premio, instante)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Contagem de acertos: ${conferencia.acertos}", style = MaterialTheme.typography.titleMedium)
                    if (m.formato != Formato.FEDERAL) VolanteDaApuracao(a.copy(apuracao = ap))
                    conferencia.faixas.forEach { f ->
                        Text(
                            "Faixa premiada: ${f.descricao}" + if (f.valor > 0) " · ${Dinheiro.rs(f.valor)}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    conferencia.observacoes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }

            CampoDinheiro(
                centavos = premio,
                aoMudar = {
                    premio = it
                    premioEditado = true
                },
                rotulo = "Prêmio do bolão neste jogo",
                ajuda = if (conferencia.precisaConfirmarValor) "Confira o valor no app da Caixa." else "Valor calculado pela tabela de prêmios da Caixa.",
                modifier = Modifier.fillMaxWidth(),
            )
            if (conferencia.precisaConfirmarValor && conferencia.premiada) {
                Aviso("A Caixa não informou o valor desta faixa: digite o prêmio que aparece no app da Caixa.", alerta = true)
            }

            val texto = Mensagens.apuracao(Operacoes.identificacao(estado, a), conferencia.acertos, Operacoes.saldoParaApuracao(estado, a), premio)
            PreviaMensagem(texto, rodape = "A imagem abaixo vai junto, como anexo.")
            PreviaImagem(a, ap)

            Button(
                enabled = !enviando,
                onClick = { confirmar = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Confirmar e enviar no WhatsApp") }

            if (confirmar) {
                DialogoConfirmar(
                    titulo = "Confirmar a apuração do jogo #${a.numero}?",
                    texto = if (premio > 0) "Soma ${Dinheiro.rs(premio)} ao saldo e abre o WhatsApp com a mensagem e a imagem."
                    else "Registra que não houve prêmio e abre o WhatsApp com a mensagem e a imagem.",
                    confirmar = "Confirmar",
                    aoConfirmar = {
                        confirmar = false
                        enviando = true
                        escopo.launch {
                            try {
                                val agora = System.currentTimeMillis()
                                repo.atualizar { Operacoes.confirmarApuracao(it, a.id, ap, LocalDate.now(), agora) }
                                val arq = withContext(Dispatchers.Default) { ImagemApuracao.salvar(ctx, a, ImagemApuracao.gerar(a, ap)) }
                                Compartilhar.whatsapp(ctx, texto, listOf(Anexo(arq, "image/png")))
                            } catch (t: Throwable) {
                                avisos.showSnackbar("Não foi possível confirmar: ${t.message}")
                            } finally {
                                enviando = false
                            }
                        }
                    },
                    aoFechar = { confirmar = false },
                )
            }
        }
    }
}

@Composable
private fun ApuracaoConfirmada(estado: Estado, a: Aposta) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val escopo = rememberCoroutineScope()
    val avisos = LocalAvisos.current
    val ap = a.apuracao ?: return
    var desfazer by remember { mutableStateOf(false) }
    val texto = Mensagens.apuracao(Operacoes.identificacao(estado, a), ap.acertos, Operacoes.saldoParaApuracao(estado, a), ap.premio)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CabecalhoAposta(
            a,
            "#${a.numero} · ${a.modalidade.nome} ${a.concurso}",
            if (ap.premio > 0) "Apurado · prêmio ${Dinheiro.rs(ap.premio)}" else "Apurado · sem prêmio",
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Contagem de acertos: ${ap.acertos}", style = MaterialTheme.typography.titleMedium)
                if (a.modalidade.formato != Formato.FEDERAL) VolanteDaApuracao(a)
            }
        }
        PreviaMensagem(texto)
        PreviaImagem(a, ap)
        OutlinedButton(
            onClick = {
                escopo.launch {
                    val arq = withContext(Dispatchers.Default) { ImagemApuracao.salvar(ctx, a, ImagemApuracao.gerar(a, ap)) }
                    Compartilhar.whatsapp(ctx, texto, listOf(Anexo(arq, "image/png")))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Enviar a mensagem de novo") }
        TextButton(onClick = { desfazer = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Desfazer a apuração", color = MaterialTheme.colorScheme.error)
        }
    }

    if (desfazer) {
        DialogoConfirmar(
            titulo = "Desfazer a apuração?",
            texto = if (ap.premio > 0) "O prêmio de ${Dinheiro.rs(ap.premio)} sai do saldo e o jogo volta para \"sorteados\"."
            else "O jogo volta para \"sorteados\", para apurar de novo.",
            confirmar = "Desfazer",
            aoConfirmar = {
                desfazer = false
                escopo.launch {
                    try {
                        repo.atualizar { Operacoes.desfazerApuracao(it, a.id) }
                    } catch (t: Throwable) {
                        avisos.showSnackbar(t.message ?: "Não foi possível desfazer.")
                    }
                }
            },
            aoFechar = { desfazer = false },
        )
    }
}

@Composable
private fun PreviaImagem(a: Aposta, ap: Apuracao) {
    val imagem by produceState<android.graphics.Bitmap?>(null, a, ap) {
        value = withContext(Dispatchers.Default) { runCatching { ImagemApuracao.gerar(a, ap) }.getOrNull() }
    }
    imagem?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Imagem da apuração",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
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

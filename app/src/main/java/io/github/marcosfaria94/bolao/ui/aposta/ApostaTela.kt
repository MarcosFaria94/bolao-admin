package io.github.marcosfaria94.bolao.ui.aposta

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.Anexo
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoData
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.CampoInteiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LinhaAnexo
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PreviaMensagem
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Tela de uma aposta: rascunho (editável) ou já registrada. */
@Composable
fun ApostaTela(estado: Estado, id: String, abrirApuracao: (String) -> Unit, aoSair: () -> Unit) {
    val a = estado.apostas.firstOrNull { it.id == id }
    if (a == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não existe mais.") }
        return
    }
    if (a.registrada) {
        DetalheAposta(estado, a, abrirApuracao)
    } else {
        EditorAposta(estado, a, aoSair)
    }
}

@Composable
fun CabecalhoAposta(a: Aposta, titulo: String, subtitulo: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SeloModalidade(a.modalidade, tamanho = 48)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private sealed interface Consulta {
    data object Nada : Consulta
    data object Carregando : Consulta
    data class Ok(val texto: String) : Consulta
    data class Erro(val texto: String) : Consulta
}

@Composable
private fun EditorAposta(estado: Estado, inicial: Aposta, aoSair: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val m = inicial.modalidade
    val teste by repo.modoTeste.collectAsStateWithLifecycle()

    // O rascunho na tela é a fonte da verdade; cada mudança vai para a fila de gravação.
    var r by remember(inicial.id) { mutableStateOf(inicial) }
    fun mudar(novo: Aposta) {
        r = novo
        repo.enfileirar { e -> if (e.apostas.any { it.id == novo.id && !it.registrada }) Operacoes.salvarRascunho(e, novo) else e }
    }
    fun mudarPalpite(p: Palpite) = mudar(r.copy(numeros = p.numeros, trevos = p.trevos, extra = p.extra, palpitesLoteca = p.loteca))

    var consulta by remember { mutableStateOf<Consulta>(Consulta.Nada) }
    var confirmarRegistro by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var registrando by remember { mutableStateOf(false) }

    suspend fun consultarCaixa() {
        consulta = Consulta.Carregando
        try {
            val c = CaixaApi.ultimo(m, usarCache = false)
            var novo = r.copy(
                concurso = c.proximoNumero ?: (c.numero + 1),
                dataSorteio = c.proximaData ?: r.dataSorteio,
                premioEstimado = c.premioEstimadoProximo ?: r.premioEstimado,
            )
            if (m == Modalidade.LOTECA) {
                runCatching { CaixaApi.concurso(m, novo.concurso) }.getOrNull()
                    ?.jogosLoteca?.takeIf { it.isNotEmpty() }
                    ?.let { jogos -> novo = novo.copy(jogosLoteca = jogos.map { "${it.time1} x ${it.time2}" }) }
            }
            mudar(novo)
            consulta = Consulta.Ok("Dados do concurso ${novo.concurso} conferidos na Caixa.")
        } catch (t: Throwable) {
            consulta = Consulta.Erro("Não foi possível consultar a Caixa (${t.message}). Preencha os dados do concurso à mão.")
        }
    }

    LaunchedEffect(inicial.id) {
        if (inicial.concurso == 0) consultarCaixa()
    }

    val escolherComprovante = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            try {
                val arq = repo.guardarArquivo(uri, "comprovante")
                mudar(r.copy(comprovante = arq))
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível anexar: ${t.message}")
            }
        }
    }

    val pendencias = buildList {
        if (m.formato == Formato.FEDERAL) {
            if (r.bilheteFederal.isNullOrBlank()) add("Informe o número do bilhete.")
            if ((r.fracoesFederal ?: 0) !in 1..10) add("Informe de 1 a 10 frações.")
        } else {
            addAll(Volante.pendencias(m, r.palpite))
        }
        if (r.concurso <= 0) add("Informe o número do concurso.")
        if (r.valor <= 0) add("Informe o valor da aposta.")
    }
    val saldo = Operacoes.saldo(estado)
    val eventual = Operacoes.ehEventual(estado, r.dataAposta, r.id)

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CabecalhoAposta(
            r,
            "${m.nome}${if (r.concurso > 0) " ${r.concurso}" else ""}",
            "Jogo #${estado.proximoJogo} · ${if (eventual) "APOSTA EVENTUAL" else "aposta do dia"} · rascunho",
        )

        // Concurso
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Concurso", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    when (consulta) {
                        Consulta.Carregando -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else -> TextButton(onClick = { escopo.launch { consultarCaixa() } }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Consultar a Caixa")
                        }
                    }
                }
                when (val c = consulta) {
                    is Consulta.Erro -> Aviso(c.texto, alerta = true)
                    is Consulta.Ok -> Text(c.texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    else -> Unit
                }
                CampoInteiro(
                    valor = r.concurso.takeIf { it > 0 },
                    aoMudar = { mudar(r.copy(concurso = it ?: 0)) },
                    rotulo = "Número do concurso",
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoData(
                    data = r.dataSorteio,
                    aoMudar = { mudar(r.copy(dataSorteio = it)) },
                    rotulo = "Data do sorteio",
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoDinheiro(
                    centavos = r.premioEstimado ?: 0L,
                    aoMudar = { mudar(r.copy(premioEstimado = it.takeIf { v -> v > 0 })) },
                    rotulo = if (m == Modalidade.FEDERAL) "1º prêmio" else "Prêmio estimado",
                    ajuda = r.premioEstimado?.let { "Na mensagem: ${Dinheiro.abreviar(it)}" },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Volante
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VolanteEditavel(m, r, ::mudar, ::mudarPalpite)
            }
        }

        // Valor
        if (m.formato != Formato.FEDERAL) {
            CampoDinheiro(
                centavos = r.valor,
                aoMudar = { mudar(r.copy(valor = it)) },
                rotulo = "Valor da aposta",
                ajuda = "Preço da aposta simples: ${Dinheiro.rs(estado.config.preco(m))}",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (r.valor > saldo) {
            Aviso("O valor passa do saldo do bolão (${Dinheiro.rs(saldo)}).", alerta = true)
        }

        // Comprovante
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Faça a aposta no app Loterias CAIXA com estes números e anexe o comprovante aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinhaAnexo(
                    titulo = "Comprovante da Caixa",
                    nomeArquivo = r.comprovante?.nomeOriginal,
                    aoAnexar = { escolherComprovante.launch(arrayOf("application/pdf", "image/*")) },
                    arquivo = r.comprovante?.let { repo.arquivo(it) },
                    mime = r.comprovante?.mime,
                )
                if (teste) {
                    TextButton(onClick = {
                        escopo.launch {
                            try {
                                val arq = ModoTeste.comprovante(ctx, r)
                                mudar(r.copy(comprovante = arq))
                            } catch (t: Throwable) {
                                avisos.showSnackbar("Não foi possível criar o comprovante de exemplo: ${t.message}")
                            }
                        }
                    }) { Text("Usar um comprovante de exemplo") }
                }
            }
        }

        PreviaMensagem(Operacoes.textoAposta(estado, r), rodape = "O comprovante vai junto, como anexo.")

        pendencias.firstOrNull()?.let { Aviso(it, alerta = true) }
        if (pendencias.isEmpty() && r.comprovante == null) {
            Aviso("Falta anexar o comprovante da Caixa: sem ele publicado no grupo, a aposta não vale para o bolão.", alerta = true)
        }

        Button(
            enabled = pendencias.isEmpty() && r.comprovante != null && !registrando,
            onClick = { confirmarRegistro = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Registrar e enviar no WhatsApp") }
        TextButton(onClick = { confirmarExclusao = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Descartar rascunho", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmarRegistro) {
        DialogoConfirmar(
            titulo = "Registrar o jogo #${estado.proximoJogo}?",
            texto = "Desconta ${Dinheiro.rs(r.valor)} do saldo e abre o WhatsApp com a mensagem e o comprovante.",
            confirmar = "Registrar",
            aoConfirmar = {
                confirmarRegistro = false
                registrando = true
                escopo.launch {
                    try {
                        val agora = System.currentTimeMillis()
                        val pronta = r
                        val novo = repo.atualizar { e -> Operacoes.registrarAposta(Operacoes.salvarRascunho(e, pronta), pronta.id, agora) }
                        val reg = novo.apostas.first { it.id == pronta.id }
                        val anexos = listOfNotNull(reg.comprovante?.let { Anexo(repo.arquivo(it), it.mime) })
                        Compartilhar.whatsapp(ctx, Operacoes.textoAposta(novo, reg), anexos)
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível registrar: ${t.message}")
                    } finally {
                        registrando = false
                    }
                }
            },
            aoFechar = { confirmarRegistro = false },
        )
    }
    if (confirmarExclusao) {
        DialogoConfirmar(
            titulo = "Descartar este rascunho?",
            texto = "Os números e o comprovante anexado desta aposta serão apagados do app.",
            confirmar = "Descartar",
            aoConfirmar = {
                confirmarExclusao = false
                escopo.launch {
                    repo.atualizar { Operacoes.excluirRascunho(it, r.id) }
                    aoSair()
                }
            },
            aoFechar = { confirmarExclusao = false },
        )
    }
}

@Composable
private fun VolanteEditavel(m: Modalidade, r: Aposta, mudar: (Aposta) -> Unit, mudarPalpite: (Palpite) -> Unit) {
    when (m.formato) {
        Formato.NUMEROS -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Números", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${r.numeros.size} de ${m.qtdAposta}", style = MaterialTheme.typography.labelLarge)
            }
            GradeNumeros(
                m,
                situacao = { if (it in r.numeros) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                aoTocar = { n -> mudarPalpite(Volante.alternar(m, r.palpite, n)) },
            )
            if (m.trevos > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trevos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${r.trevos.size} de ${m.trevos}", style = MaterialTheme.typography.labelLarge)
                }
                GradeTrevos(
                    m,
                    situacao = { if (it in r.trevos) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                    aoTocar = { t -> mudarPalpite(Volante.alternarTrevo(m, r.palpite, t)) },
                )
            }
            when (m.extra) {
                Extra.TIME_DO_CORACAO -> OutlinedTextField(
                    value = r.extra.orEmpty(),
                    onValueChange = { mudar(r.copy(extra = it)) },
                    label = { Text("Time do Coração") },
                    supportingText = { Text("O mesmo escolhido no app da Caixa.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Extra.MES_DE_SORTE -> EscolhaMes(r.extra) { mudar(r.copy(extra = it)) }
                Extra.NENHUM -> Unit
            }
        }
        Formato.COLUNAS -> {
            Text("Um número em cada coluna", style = MaterialTheme.typography.titleMedium)
            GradeColunas(
                m,
                situacao = { c, n -> if (r.numeros.getOrNull(c) == n) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                aoTocar = { c, n ->
                    val cols = MutableList(Volante.COLUNAS_SUPER_SETE) { i -> r.numeros.getOrNull(i) ?: -1 }
                    cols[c] = n
                    mudar(r.copy(numeros = cols))
                },
            )
        }
        Formato.LOTECA -> {
            Text("Palpites: 1 (time da casa), X (empate) ou 2", style = MaterialTheme.typography.titleMedium)
            GradeLoteca(
                m,
                rotulos = r.jogosLoteca,
                palpites = r.palpitesLoteca,
                aoEscolher = { j, op ->
                    val lista = MutableList(Volante.JOGOS_LOTECA) { i -> r.palpitesLoteca.getOrNull(i) ?: "" }
                    lista[j] = op
                    mudar(r.copy(palpitesLoteca = lista))
                },
            )
        }
        Formato.FEDERAL -> {
            Text("Bilhete da Federal", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = r.bilheteFederal.orEmpty(),
                onValueChange = { t -> mudar(r.copy(bilheteFederal = t.filter(Char::isDigit).take(6))) },
                label = { Text("Número do bilhete") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            CampoInteiro(
                valor = r.fracoesFederal,
                aoMudar = { mudar(r.copy(fracoesFederal = it?.coerceAtMost(10))) },
                rotulo = "Frações (1 a 10)",
                ajuda = "10 frações = bilhete inteiro.",
                modifier = Modifier.fillMaxWidth(),
            )
            CampoDinheiro(
                centavos = r.valor,
                aoMudar = { mudar(r.copy(valor = it)) },
                rotulo = "Valor pago",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (m.formato != Formato.FEDERAL) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { mudarPalpite(Volante.completar(m, r.palpite)) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Completar com aleatórios")
            }
            OutlinedButton(onClick = {
                val vazio = Palpite(
                    numeros = if (m.formato == Formato.COLUNAS) List(Volante.COLUNAS_SUPER_SETE) { -1 } else emptyList(),
                    extra = if (m.extra == Extra.TIME_DO_CORACAO) r.extra else null,
                )
                mudarPalpite(vazio)
            }) { Text("Limpar") }
        }
    }
}

@Composable
private fun EscolhaMes(atual: String?, aoEscolher: (String) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Mês de Sorte: ${atual ?: "escolher"}")
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            MESES_DE_SORTE.forEach { mes ->
                DropdownMenuItem(text = { Text(mes) }, onClick = {
                    aoEscolher(mes)
                    aberto = false
                })
            }
        }
    }
}

@Composable
private fun DetalheAposta(estado: Estado, a: Aposta, abrirApuracao: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val escopo = rememberCoroutineScope()
    val avisos = LocalAvisos.current
    var confirmarDesfazer by remember { mutableStateOf(false) }
    val hoje = LocalDate.now()
    val m = a.modalidade
    val ap = a.apuracao

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val situacao = when {
            ap?.confirmadaEm != null -> if (ap.premio > 0) "Apurado · prêmio ${Dinheiro.rs(ap.premio)}" else "Apurado · sem prêmio"
            a.dataSorteio != null && a.dataSorteio.isBefore(hoje) -> "Sorteado · aguardando apuração"
            a.dataSorteio == hoje -> "Sorteio hoje"
            else -> "Aguardando sorteio"
        }
        CabecalhoAposta(a, "#${a.numero} · ${m.nome} ${a.concurso}", situacao)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp)) {
                LinhaValor("Data da aposta", Datas.longa(a.dataAposta))
                LinhaValor("Sorteio", a.dataSorteio?.let { "${Datas.diaSemana(it)}, ${Datas.longa(it)}" } ?: "a confirmar")
                LinhaValor("Valor", Dinheiro.rs(a.valor))
                a.premioEstimado?.let { LinhaValor(if (m == Modalidade.FEDERAL) "1º prêmio" else "Prêmio estimado", Dinheiro.rs(it)) }
                if (a.eventual) LinhaValor("Tipo", "Aposta eventual")
                if (m == Modalidade.FEDERAL) {
                    LinhaValor("Bilhete", a.bilheteFederal.orEmpty())
                    LinhaValor("Frações", "${a.fracoesFederal ?: 10} de 10")
                }
            }
        }

        if (m.formato != Formato.FEDERAL) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VolanteDaApuracao(a)
                }
            }
        }

        a.comprovante?.let { c ->
            LinhaAnexo(
                titulo = "Comprovante da Caixa",
                nomeArquivo = c.nomeOriginal,
                aoAnexar = {},
                arquivo = repo.arquivo(c),
                mime = c.mime,
            )
        }

        PreviaMensagem(Operacoes.textoAposta(estado, a))
        OutlinedButton(
            onClick = {
                val anexos = listOfNotNull(a.comprovante?.let { Anexo(repo.arquivo(it), it.mime) })
                Compartilhar.whatsapp(ctx, Operacoes.textoAposta(estado, a), anexos)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Enviar a mensagem de novo") }

        val podeApurar = ap?.confirmadaEm != null || (a.dataSorteio != null && !a.dataSorteio.isAfter(hoje))
        if (podeApurar) {
            Button(onClick = { abrirApuracao(a.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (ap?.confirmadaEm != null) "Ver apuração" else "Apurar")
            }
        }
        if (ap?.confirmadaEm == null) {
            TextButton(onClick = { confirmarDesfazer = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Desfazer o registro", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmarDesfazer) {
        DialogoConfirmar(
            titulo = "Desfazer o registro do jogo #${a.numero}?",
            texto = "O valor de ${Dinheiro.rs(a.valor)} volta ao saldo e a aposta volta a ser rascunho. " +
                "Use só se o registro foi um engano.",
            confirmar = "Desfazer",
            aoConfirmar = {
                confirmarDesfazer = false
                escopo.launch {
                    try {
                        repo.atualizar { Operacoes.desfazerAposta(it, a.id) }
                    } catch (t: Throwable) {
                        avisos.showSnackbar(t.message ?: "Não foi possível desfazer.")
                    }
                }
            },
            aoFechar = { confirmarDesfazer = false },
        )
    }
}

/** Volante somente leitura, com acertos marcados quando já há apuração. */
@Composable
fun VolanteDaApuracao(a: Aposta) {
    val m = a.modalidade
    val ap = a.apuracao
    val sorteados = ap?.dezenasSorteadas?.toSet().orEmpty()
    val temResultado = ap != null
    fun sit(apostado: Boolean, sorteado: Boolean): SituacaoNumero = when {
        !temResultado -> if (apostado) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE
        apostado && sorteado -> SituacaoNumero.ACERTO
        apostado -> SituacaoNumero.ERRO
        sorteado -> SituacaoNumero.SORTEADO
        else -> SituacaoNumero.LIVRE
    }
    when (m.formato) {
        Formato.NUMEROS -> {
            Text(if (m == Modalidade.DUPLA_SENA && temResultado) "1º sorteio" else "Números", style = MaterialTheme.typography.titleSmall)
            GradeNumeros(m, situacao = { sit(it in a.numeros, it in sorteados) }, aoTocar = null)
            if (m == Modalidade.DUPLA_SENA && temResultado) {
                val segundo = ap?.segundoSorteio?.toSet().orEmpty()
                Text("2º sorteio", style = MaterialTheme.typography.titleSmall)
                GradeNumeros(m, situacao = { sit(it in a.numeros, it in segundo) }, aoTocar = null)
            }
            if (m.trevos > 0) {
                val trevos = ap?.trevosSorteados?.toSet().orEmpty()
                Text("Trevos", style = MaterialTheme.typography.titleSmall)
                GradeTrevos(m, situacao = { sit(it in a.trevos, it in trevos) }, aoTocar = null)
            }
            a.extra?.let {
                val nome = if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte" else "Time do Coração"
                Text("$nome: $it${ap?.extraSorteado?.let { s -> " (sorteado: $s)" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Formato.COLUNAS -> GradeColunas(
            m,
            situacao = { c, n -> sit(a.numeros.getOrNull(c) == n, sorteados.isNotEmpty() && ap?.dezenasSorteadas?.getOrNull(c) == n) },
            aoTocar = null,
        )
        Formato.LOTECA -> GradeLoteca(
            m,
            rotulos = a.jogosLoteca,
            palpites = a.palpitesLoteca,
            resultados = ap?.resultadosLoteca.orEmpty().map { it.ifBlank { null } },
            aoEscolher = null,
        )
        Formato.FEDERAL -> Unit
    }
    if (temResultado) LegendaApuracao(m)
}

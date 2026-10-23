package io.github.marcosfaria94.bolao.ui.aposta

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.LeituraComprovante
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.imagem.LeitorComprovante
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.Animacoes
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BotoesDeRegistro
import io.github.marcosfaria94.bolao.ui.comum.CampoData
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.CampoInteiro
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.DialogoSimNao
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.LinhaAnexo
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.TarjaTitulo
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Tela de uma aposta já gravada: rascunho em andamento (editável) ou registrada, com a
 * apuração na mesma tela (DetalheAposta).
 */
@Composable
fun ApostaTela(estado: Estado, id: String, aoSair: () -> Unit) {
    val a = estado.apostas.firstOrNull { it.id == id }
    if (a == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não existe mais.") }
        return
    }
    if (a.registrada) {
        DetalheAposta(estado, a)
        return
    }
    // Rascunho em andamento: a tela é a fonte da verdade; ao sair, vale a regra do rascunho.
    val app = LocalContext.current.bolao
    val vm = viewModel(key = "aposta-${a.id}") { ApostaEmEdicao(app.repo, app.escopo, a, nova = false) }
    EditorAposta(
        estado = estado,
        r = vm.aposta,
        mudar = vm::mudar,
        aoDescartar = {
            vm.descartar()
            aoSair()
        },
    )
}

/**
 * Aposta nova ("nova/{modalidade}"), com o volante vazio. Ao sair da tela, fica em "Em andamento"
 * se o volante estiver completo ou se o comprovante da Caixa estiver anexado; senão, é descartada.
 */
@Composable
fun ApostaNovaTela(estado: Estado, m: Modalidade, aoSair: () -> Unit) {
    val app = LocalContext.current.bolao
    val vm = viewModel {
        ApostaEmEdicao(
            app.repo,
            app.escopo,
            novaAposta(estado, m, app.indices.itens.value[m]?.ultimo, LocalDate.now(), System.currentTimeMillis()),
            nova = true,
        )
    }
    // Registrada direto daqui: a tela vira a da aposta registrada.
    val gravada = estado.apostas.firstOrNull { it.id == vm.aposta.id }
    if (gravada?.registrada == true) {
        DetalheAposta(estado, gravada)
        return
    }
    EditorAposta(
        estado = estado,
        r = vm.aposta,
        mudar = vm::mudar,
        aoDescartar = {
            vm.descartar()
            aoSair()
        },
    )
}

/** "Prêmio" (ou "1º prêmio", na Federal) e o valor por extenso: "R$ 200 Milhões". null = sem estimativa. */
fun textoPremio(a: Aposta): Pair<String, String>? = a.premioEstimado?.takeIf { it > 0 }?.let {
    (if (a.modalidade == Modalidade.FEDERAL) "1º prêmio" else "Prêmio") to Dinheiro.porExtenso(it)
}

/**
 * Cabeçalho da tela da aposta: o emblema da modalidade (sigla e concurso), a tarja "Aposta Nº1"
 * e o nome da modalidade bem grande. No rascunho vem também o lápis discreto ([aoEditar]) que
 * abre os dados do concurso.
 */
@Composable
fun CabecalhoAposta(
    a: Aposta,
    numero: Int,
    aoEditar: (() -> Unit)? = null,
    carregando: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmblemaModalidade(a.modalidade, a.concurso.takeIf { it > 0 }, tamanho = 66.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TarjaTitulo("Aposta Nº$numero")
            TextoQueCabe(
                a.modalidade.nome,
                MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp, lineHeight = 42.sp),
                peso = FontWeight.Bold,
                menor = 22.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (carregando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        if (aoEditar != null) {
            IconButton(onClick = aoEditar, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Alterar os dados do concurso",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Linha "Rótulo: valor" alinhada, como no esboço da tela. */
@Composable
fun LinhaInfo(rotulo: String, valor: String, destaque: Boolean = false, cor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("$rotulo: ", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.SemiBold,
            color = cor,
        )
    }
}

private sealed interface Consulta {
    data object Nada : Consulta
    data object Carregando : Consulta
    data class Ok(val texto: String) : Consulta
    data class Erro(val texto: String) : Consulta
}

/** O próximo concurso da modalidade, como a Caixa informa (na Loteca, com os 14 jogos). */
private data class DadosConcurso(val numero: Int, val sorteio: LocalDate?, val premio: Long?, val jogosLoteca: List<String>?)

private suspend fun dadosNaCaixa(m: Modalidade): DadosConcurso {
    val c = CaixaApi.ultimo(m, usarCache = false)
    val numero = c.proximoNumero ?: (c.numero + 1)
    val jogos = if (m == Modalidade.LOTECA) {
        runCatching { CaixaApi.concurso(m, numero) }.getOrNull()
            ?.jogosLoteca?.takeIf { it.isNotEmpty() }
            ?.map { "${it.time1} x ${it.time2}" }
    } else {
        null
    }
    return DadosConcurso(numero, c.proximaData, c.premioEstimadoProximo, jogos)
}

private fun Aposta.com(d: DadosConcurso): Aposta = copy(
    concurso = d.numero,
    dataSorteio = d.sorteio ?: dataSorteio,
    premioEstimado = d.premio ?: premioEstimado,
    jogosLoteca = d.jogosLoteca ?: jogosLoteca,
)

/**
 * Editor do rascunho: o cabeçalho (com o lápis dos dados do concurso), o volante com
 * "Completar" e "Limpar", o comprovante da Caixa e o registro. Ao sair, a aposta só fica em
 * andamento com o volante completo ou com o comprovante anexado ([ficaEmAndamento]).
 */
@Composable
private fun EditorAposta(
    estado: Estado,
    r: Aposta,
    mudar: (Aposta) -> Unit,
    aoDescartar: () -> Unit,
) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val m = r.modalidade
    val federal = m.formato == Formato.FEDERAL
    val teste by repo.modoTeste.collectAsStateWithLifecycle()
    val hoje = LocalDate.now()
    // Nas respostas que chegam depois (Caixa, arquivo escolhido), vale a aposta como está agora.
    val atual by rememberUpdatedState(r)
    val mudarAtual by rememberUpdatedState(mudar)
    fun mudarPalpite(p: Palpite) = mudarAtual(atual.copy(numeros = p.numeros, trevos = p.trevos, extra = p.extra, palpitesLoteca = p.loteca))

    var consulta by remember { mutableStateOf<Consulta>(Consulta.Nada) }
    var editando by remember { mutableStateOf(false) }
    var conferencia by remember { mutableStateOf<ConferenciaCaixa?>(null) }
    var lendo by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var registrando by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }

    // Sem o número do concurso: busca o próximo na Caixa assim que a tela abre.
    LaunchedEffect(r.id) {
        if (atual.concurso == 0) {
            consulta = Consulta.Carregando
            consulta = try {
                val d = dadosNaCaixa(m)
                mudarAtual(atual.com(d))
                Consulta.Ok("Dados do concurso ${d.numero} conferidos na Caixa.")
            } catch (t: Throwable) {
                Consulta.Erro("Não foi possível consultar a Caixa (${t.message}). Toque no lápis para preencher os dados do concurso.")
            }
        }
    }

    fun trocarComprovante(novo: Arquivo) {
        val anterior = atual.comprovante
        mudarAtual(atual.copy(comprovante = novo))
        // O arquivo trocado não serve mais (só é apagado se nenhum registro o usa).
        anterior?.takeIf { it.nome != novo.nome }?.let { repo.apagarSeSolto(it) }
    }

    val escolherComprovante = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            try {
                trocarComprovante(repo.guardarArquivo(uri, "comprovante"))
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível anexar: ${t.message}")
            }
        }
    }

    val pendencias = buildList {
        if (federal) {
            if (r.bilheteFederal.isNullOrBlank()) add("Informe o número do bilhete.")
            if ((r.fracoesFederal ?: 0) !in 1..10) add("Informe de 1 a 10 frações.")
        } else {
            addAll(Volante.pendencias(m, r.palpite))
        }
        if (r.concurso <= 0) add("Informe o número do concurso (no lápis, no alto da tela).")
        if (r.valor <= 0) add(if (federal) "Informe o valor pago." else "Informe o preço da aposta (no lápis, no alto da tela).")
    }
    val numero = Operacoes.proximoNumero(estado, hoje)
    val titulo = Operacoes.tituloAposta(estado, r, hoje)

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
            numero,
            aoEditar = { editando = true },
            carregando = consulta == Consulta.Carregando,
        )
        Column {
            textoPremio(r)?.let { (rotulo, valor) -> LinhaInfo(rotulo, valor) }
            LinhaInfo("Sorteio", r.dataSorteio?.let { Datas.relativa(it, hoje) } ?: "a confirmar")
        }
        (consulta as? Consulta.Erro)?.let { Aviso(it.texto, alerta = true) }

        // Volante
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VolanteEditavel(m, r, { mudarAtual(it) }, ::mudarPalpite)
            }
        }

        // Comprovante da Caixa
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
                                trocarComprovante(ModoTeste.comprovante(ctx, atual))
                            } catch (t: Throwable) {
                                avisos.showSnackbar("Não foi possível criar o comprovante de exemplo: ${t.message}")
                            }
                        }
                    }) { Text("Usar um comprovante de exemplo") }
                }
            }
        }

        pendencias.firstOrNull()?.let { Aviso(it, alerta = true) }
        if (pendencias.isEmpty() && r.comprovante == null) {
            Aviso("Falta anexar o comprovante da Caixa: sem ele publicado no grupo, a aposta não vale para o bolão.", alerta = true)
        }
        if (!ficaEmAndamento(r)) {
            Aviso(
                "Se sair agora, a aposta é descartada: complete o " + (if (federal) "bilhete" else "volante") +
                    " ou anexe o comprovante da Caixa.",
            )
        }

        BotoesDeRegistro(
            habilitado = pendencias.isEmpty() && r.comprovante != null,
            trabalhando = registrando || lendo,
            aoVisualizar = { vendo = true },
            aoRegistrar = {
                // Antes de registrar, confere a aposta com o comprovante da Caixa.
                val arq = atual.comprovante ?: return@BotoesDeRegistro
                lendo = true
                escopo.launch {
                    conferencia = conferirComCaixa(atual, repo.arquivo(arq), arq.mime)
                    lendo = false
                }
            },
        )
        if (lendo) {
            Text(
                "Conferindo o comprovante da Caixa...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { confirmarExclusao = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Descartar rascunho", color = MaterialTheme.colorScheme.error)
        }
    }

    if (editando) {
        DialogoConcurso(
            a = r,
            precoAjustes = estado.config.preco(m),
            aoSalvar = { novo ->
                editando = false
                mudarAtual(novo)
                if (consulta is Consulta.Erro && novo.concurso > 0) consulta = Consulta.Nada
            },
            aoFechar = { editando = false },
        )
    }

    if (vendo) {
        VisualizadorComprovante(
            estado,
            titulo,
            aoFechar = { vendo = false },
            previa = Recibos.previa(estado, TipoComprovante.APOSTA, Operacoes.textoAposta(estado, r, hoje), if (r.comprovante != null) 1 else 0),
            anexos = listOfNotNull(r.comprovante),
        )
    }

    conferencia?.let { conf ->
        val pronta = (conf as? ConferenciaCaixa.Diverge)?.ajustada ?: r
        // Pergunta curta; só o que foge do normal na conferência com a Caixa entra antes.
        val antes = when (conf) {
            is ConferenciaCaixa.Confere -> conf.faltou?.let { "Não deu para ler $it no comprovante da Caixa: confira antes." }
            is ConferenciaCaixa.Diverge ->
                "O comprovante da Caixa tem outra aposta:\n" +
                    conf.diferencas.joinToString("\n") { d -> "• ${d.campo}: no app ${d.noApp}; na Caixa ${d.naCaixa}." } +
                    "\nA aposta vai ficar como na Caixa."
            is ConferenciaCaixa.NaoLida -> "Não deu para conferir o comprovante da Caixa (${conf.motivo}): confira os números antes."
        }
        DialogoSimNao(
            titulo = "Deseja registrar aposta?",
            texto = listOfNotNull(antes, "A ação é irreversível.").joinToString("\n\n"),
            aoNao = { conferencia = null },
            aoSim = {
                conferencia = null
                registrando = true
                escopo.launch {
                    try {
                        val agora = System.currentTimeMillis()
                        val final = comDadosDoConcurso(pronta, r)
                        mudarAtual(final)
                        registrarComprovante(ctx, { e -> Operacoes.registrarAposta(Operacoes.salvarRascunho(e, final), final.id, hoje, agora) })
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível registrar: ${t.message}")
                    } finally {
                        registrando = false
                    }
                }
            },
        )
    }
    if (confirmarExclusao) {
        DialogoConfirmar(
            titulo = "Descartar este rascunho?",
            texto = "Os números e o comprovante anexado desta aposta serão apagados do app. A aposta ainda não foi registrada.",
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmarExclusao = false
                aoDescartar()
            },
            aoFechar = { confirmarExclusao = false },
        )
    }
}

/**
 * Janela do lápis: número do concurso, dia do sorteio, prêmio e o preço da aposta (a Federal
 * tem o valor pago no próprio formulário). "Consultar a Caixa" preenche com o próximo concurso.
 */
@Composable
private fun DialogoConcurso(a: Aposta, precoAjustes: Long, aoSalvar: (Aposta) -> Unit, aoFechar: () -> Unit) {
    val m = a.modalidade
    val federal = m.formato == Formato.FEDERAL
    val escopo = rememberCoroutineScope()
    var concurso by remember { mutableStateOf(a.concurso.takeIf { it > 0 }) }
    var sorteio by remember { mutableStateOf(a.dataSorteio) }
    var premio by remember { mutableLongStateOf(a.premioEstimado ?: 0L) }
    var preco by remember { mutableLongStateOf(a.valor) }
    var jogos by remember { mutableStateOf(a.jogosLoteca) }
    var consulta by remember { mutableStateOf<Consulta>(Consulta.Nada) }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Dados do concurso") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val c = consulta) {
                    Consulta.Carregando -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Consultando a Caixa...", style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {
                        TextButton(onClick = {
                            consulta = Consulta.Carregando
                            escopo.launch {
                                consulta = try {
                                    val d = dadosNaCaixa(m)
                                    concurso = d.numero
                                    d.sorteio?.let { sorteio = it }
                                    d.premio?.let { premio = it }
                                    d.jogosLoteca?.let { jogos = it }
                                    Consulta.Ok("Dados do concurso ${d.numero} conferidos na Caixa.")
                                } catch (t: Throwable) {
                                    Consulta.Erro("Não foi possível consultar a Caixa (${t.message}).")
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Consultar a Caixa")
                        }
                        when (c) {
                            is Consulta.Ok -> Text(c.texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            is Consulta.Erro -> Text(c.texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            else -> Unit
                        }
                    }
                }
                CampoInteiro(
                    valor = concurso,
                    aoMudar = { concurso = it },
                    rotulo = "Número do concurso",
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoData(
                    data = sorteio,
                    aoMudar = { sorteio = it },
                    rotulo = "Sorteio",
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoDinheiro(
                    centavos = premio,
                    aoMudar = { premio = it },
                    rotulo = if (federal) "1º prêmio" else "Prêmio estimado",
                    ajuda = premio.takeIf { it > 0 }?.let { "No comprovante: ${Dinheiro.abreviar(it)}" },
                    imeAction = if (federal) ImeAction.Done else ImeAction.Next,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!federal) {
                    CampoDinheiro(
                        centavos = preco,
                        aoMudar = { preco = it },
                        rotulo = "Preço da aposta",
                        ajuda = "Aposta simples (Ajustes): ${Dinheiro.rs(precoAjustes)}",
                        imeAction = ImeAction.Done,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                aoSalvar(
                    a.copy(
                        concurso = concurso ?: 0,
                        dataSorteio = sorteio,
                        premioEstimado = premio.takeIf { it > 0 },
                        valor = if (federal) a.valor else preco,
                        jogosLoteca = jogos,
                    ),
                )
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

/**
 * "Completar" com um efeito de pulsar enquanto não foi tocado ([destacar]): o botão respira e
 * solta um anel em volta, para chamar a atenção para o próximo passo.
 */
@Composable
private fun BotaoCompletar(destacar: Boolean, modifier: Modifier, aoTocar: () -> Unit) {
    val conteudo: @Composable () -> Unit = {
        Icon(Icons.Outlined.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Completar")
    }
    if (!destacar || !Animacoes.ligadas) {
        FilledTonalButton(onClick = aoTocar, modifier = modifier) { conteudo() }
        return
    }
    val pulso = rememberInfiniteTransition(label = "pulso")
    val escala by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escala",
    )
    val anel by pulso.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "anel",
    )
    val cor = MaterialTheme.colorScheme.primary
    Button(
        onClick = aoTocar,
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .drawBehind {
                val folga = 7.dp.toPx() * anel
                drawRoundRect(
                    color = cor.copy(alpha = 0.45f * (1f - anel)),
                    topLeft = Offset(-folga, -folga),
                    size = Size(size.width + 2 * folga, size.height + 2 * folga),
                    cornerRadius = CornerRadius(size.height / 2 + folga),
                )
            },
    ) { conteudo() }
}

/** Volante completo: no lugar do "Completar", a marca verde. */
@Composable
private fun MarcaCompleto(modifier: Modifier) {
    val verde = corEntrada()
    Row(
        modifier = modifier
            .height(40.dp)
            .background(verde.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = verde, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Volante completo", style = MaterialTheme.typography.labelLarge, color = verde, maxLines = 1)
    }
}

@Composable
private fun VolanteEditavel(
    m: Modalidade,
    r: Aposta,
    mudar: (Aposta) -> Unit,
    mudarPalpite: (Palpite) -> Unit,
) {
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
                Extra.TIME_DO_CORACAO -> BotaoTimeDoCoracao(r.extra) { mudar(r.copy(extra = it)) }
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
        // "Completar" enquanto faltam números; com o volante cheio, a marca "Volante completo".
        val completo = Volante.pendencias(m, r.palpite).isEmpty()
        var usouCompletar by rememberSaveable(r.id) { mutableStateOf(false) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!completo) {
                BotaoCompletar(destacar = !usouCompletar, modifier = Modifier.weight(1f)) {
                    usouCompletar = true
                    mudarPalpite(Volante.completar(m, r.palpite))
                }
            } else {
                MarcaCompleto(Modifier.weight(1f))
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

/** Resultado da conferência da aposta com o comprovante da Caixa, antes do registro. */
sealed interface ConferenciaCaixa {
    /** Confere ([conferidos]: o que foi lido); [faltou]: a parte principal que não deu para ler. */
    data class Confere(val conferidos: List<String>, val faltou: String?) : ConferenciaCaixa
    data class Diverge(val diferencas: List<Operacoes.Divergencia>, val ajustada: Aposta) : ConferenciaCaixa
    data class NaoLida(val motivo: String) : ConferenciaCaixa
}

/** Lê o comprovante da Caixa (reconhecimento de texto no celular) e compara com a aposta. */
suspend fun conferirComCaixa(a: Aposta, arquivo: java.io.File, mime: String): ConferenciaCaixa = try {
    val m = a.modalidade
    val lida = LeituraComprovante.ler(m, LeitorComprovante.texto(arquivo, mime))
    if (lida.vazia) {
        ConferenciaCaixa.NaoLida("o app não achou o concurso nem os números no comprovante")
    } else {
        val diferencas = Operacoes.divergencias(a, lida)
        if (diferencas.isNotEmpty()) {
            ConferenciaCaixa.Diverge(diferencas, Operacoes.ajustarPeloComprovante(a, lida))
        } else {
            val principal = when (m.formato) {
                Formato.NUMEROS, Formato.COLUNAS -> "números" to lida.numeros
                Formato.LOTECA -> "palpites" to lida.palpitesLoteca
                Formato.FEDERAL -> "bilhete" to lida.bilhete
            }
            val conferidos = listOfNotNull(
                lida.concurso?.let { "concurso" },
                principal.second?.let { principal.first },
                lida.trevos?.let { "trevos" },
                lida.extra?.let { if (m.extra == Extra.MES_DE_SORTE) "mês de sorte" else "time do coração" },
            )
            ConferenciaCaixa.Confere(conferidos, if (principal.second == null) "os ${principal.first}" else null)
        }
    }
} catch (t: Throwable) {
    ConferenciaCaixa.NaoLida(t.message?.takeIf { it.isNotBlank() } ?: "erro na leitura")
}

/**
 * Se a Caixa tem outro concurso no comprovante, atualiza a data do sorteio e o prêmio
 * quando o concurso é o próximo da modalidade (o que a Caixa informa).
 */
private suspend fun comDadosDoConcurso(pronta: Aposta, antes: Aposta): Aposta {
    if (pronta.concurso == antes.concurso) return pronta
    val u = runCatching { CaixaApi.ultimo(pronta.modalidade) }.getOrNull() ?: return pronta
    return if (u.proximoNumero == pronta.concurso) {
        pronta.copy(dataSorteio = u.proximaData ?: pronta.dataSorteio, premioEstimado = u.premioEstimadoProximo ?: pronta.premioEstimado)
    } else {
        pronta
    }
}

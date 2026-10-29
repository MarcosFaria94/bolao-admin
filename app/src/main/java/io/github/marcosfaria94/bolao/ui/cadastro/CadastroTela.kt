package io.github.marcosfaria94.bolao.ui.cadastro

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.MAXIMO_DE_INTEGRANTES
import io.github.marcosfaria94.bolao.data.MINIMO_DE_INTEGRANTES
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BarraTitulo
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.CampoInteiro
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.teste.BotaoExperimentar
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class FormPessoa {
    var nome by mutableStateOf("")
    var apelido by mutableStateOf("")
    var pix by mutableStateOf("")
}

class CadastroViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repositorio = app.bolao.repo

    var passo by mutableIntStateOf(0)
    /** Bolão (o grupo) ou Individual: escolhido no passo 1. */
    var modo by mutableStateOf(Modo.BOLAO)
    val pessoas = mutableStateListOf(FormPessoa(), FormPessoa(), FormPessoa())
    var administrador by mutableIntStateOf(0)
    /** Modo individual: o nome de quem usa (opcional). */
    var nomeIndividual by mutableStateOf("")

    val individual: Boolean get() = modo == Modo.INDIVIDUAL

    /** Passos do cadastro: 1 = modo; no bolão, integrantes, saldo, aporte e termo; no individual, o nome. */
    val totalPassos: Int get() = if (individual) 2 else 5

    /** Muda a quantidade de integrantes (no mínimo 2), mantendo o que já foi digitado. */
    fun mudarQuantidade(n: Int) {
        val alvo = n.coerceIn(MINIMO_DE_INTEGRANTES, MAXIMO_DE_INTEGRANTES)
        while (pessoas.size < alvo) pessoas += FormPessoa()
        while (pessoas.size > alvo) pessoas.removeAt(pessoas.lastIndex)
        if (administrador >= pessoas.size) administrador = 0
    }
    var saldoInicial by mutableLongStateOf(0L)
    /** Mês do primeiro aporte mensal (null = o padrão, ver [inicioAportes]). */
    var inicioEscolhido by mutableStateOf<YearMonth?>(null)
    var aporteMensal by mutableLongStateOf(Config().aporteMensal)

    /**
     * Padrão: o aporte deste mês só é cobrado se o cadastro é feito no dia 1º e o bolão
     * começa sem caixa; com saldo inicial, os aportes começam no mês seguinte.
     */
    fun inicioAportes(hoje: LocalDate): YearMonth = inicioEscolhido
        ?: if (hoje.dayOfMonth == 1 && saldoInicial == 0L) YearMonth.from(hoje) else YearMonth.from(hoje).plusMonths(1)
    /** Termo assinado e termo sem assinaturas (anexos opcionais, também em Ajustes). */
    var termo by mutableStateOf<Arquivo?>(null)
    var termoSemAssinatura by mutableStateOf<Arquivo?>(null)
    var salvando by mutableStateOf(false)
    var tentouAvancar by mutableStateOf(false)

    fun problemasPessoas(): List<String> = buildList {
        pessoas.forEachIndexed { i, p ->
            val n = i + 1
            if (p.nome.isBlank()) add("Falta o nome completo do integrante $n.")
            if (p.apelido.isBlank()) add("Falta o nome curto do integrante $n.")
            if (p.pix.isBlank()) add("Falta a chave Pix do integrante $n.")
        }
        val apelidos = pessoas.map { it.apelido.trim().lowercase() }.filter { it.isNotEmpty() }
        if (apelidos.size != apelidos.distinct().size) add("Os nomes curtos precisam ser diferentes entre si.")
    }

    fun problemasValores(): List<String> = if (aporteMensal <= 0) listOf("Informe o aporte mensal.") else emptyList()

    fun problemasDoPasso(): List<String> = when {
        individual -> emptyList()
        passo == 2 -> problemasPessoas()
        passo == 4 -> problemasValores()
        else -> emptyList()
    }

    fun avancar() {
        tentouAvancar = true
        if (problemasDoPasso().isEmpty()) {
            passo++
            tentouAvancar = false
        }
    }

    fun voltar() {
        if (passo > 0) passo--
        tentouAvancar = false
    }

    fun guardarTermo(uri: android.net.Uri, assinado: Boolean, aoErro: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val arq = repo.guardarArquivo(uri, if (assinado) "termo-assinado" else "termo-sem-assinatura")
                if (assinado) termo = arq else termoSemAssinatura = arq
            } catch (t: Throwable) {
                aoErro("Não foi possível guardar o arquivo: ${t.message}")
            }
        }
    }

    fun concluir(aoErro: (String) -> Unit) {
        if (salvando) return
        if (individual) {
            concluirIndividual(aoErro)
            return
        }
        val problemas = problemasPessoas() + problemasValores()
        if (problemas.isNotEmpty()) {
            aoErro(problemas.first())
            return
        }
        salvando = true
        viewModelScope.launch {
            try {
                val hoje = LocalDate.now()
                val agora = System.currentTimeMillis()
                val lista = pessoas.mapIndexed { i, f ->
                    Pessoa(
                        id = Repositorio.novoId(),
                        nome = f.nome.trim(),
                        apelido = f.apelido.trim(),
                        chavePix = f.pix.trim(),
                        administrador = i == administrador,
                    )
                }
                val lancamentos = if (saldoInicial != 0L) {
                    listOf(
                        Lancamento(
                            id = Repositorio.novoId(),
                            data = hoje,
                            criadoEm = agora,
                            tipo = TipoLancamento.SALDO_INICIAL,
                            valor = saldoInicial,
                            obs = "Saldo da caixinha no cadastro inicial",
                        ),
                    )
                } else {
                    emptyList()
                }
                repo.atualizar {
                    it.copy(
                        modo = Modo.BOLAO,
                        cadastroConcluido = true,
                        cadastradoEm = hoje,
                        inicioAportes = inicioAportes(hoje).toString(),
                        pessoas = lista,
                        config = it.config.copy(aporteMensal = aporteMensal),
                        lancamentos = it.lancamentos + lancamentos,
                        termo = termo ?: it.termo,
                        termoSemAssinatura = termoSemAssinatura ?: it.termoSemAssinatura,
                    )
                }
            } catch (t: Throwable) {
                aoErro("Não foi possível salvar: ${t.message}")
            } finally {
                salvando = false
            }
        }
    }

    /** Modo individual: só o nome (opcional); o saldo começa em zero, sem aporte, sem termo. */
    private fun concluirIndividual(aoErro: (String) -> Unit) {
        salvando = true
        viewModelScope.launch {
            try {
                val nome = nomeIndividual.trim().ifBlank { "Eu" }
                val hoje = LocalDate.now()
                repo.atualizar {
                    it.copy(
                        modo = Modo.INDIVIDUAL,
                        cadastroConcluido = true,
                        cadastradoEm = hoje,
                        pessoas = listOf(
                            Pessoa(
                                id = Repositorio.novoId(),
                                nome = nome,
                                apelido = nome.substringBefore(' '),
                                chavePix = "",
                                administrador = true,
                            ),
                        ),
                    )
                }
            } catch (t: Throwable) {
                aoErro("Não foi possível salvar: ${t.message}")
            } finally {
                salvando = false
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CadastroTela(recomeco: Int = 0) {
    // Cada volta ao cadastro (depois de apagar tudo) tem um cadastro novo, sem nada do anterior,
    // e começa no "Como vai usar o app?" (as boas-vindas ficam para o app recém-instalado).
    val vm: CadastroViewModel = viewModel(key = "cadastro-$recomeco")
    remember(vm) { if (recomeco > 0 && vm.passo == 0) vm.passo = 1 }
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val avisar: (String) -> Unit = { msg -> escopo.launch { avisos.showSnackbar(msg) } }
    var restaurando by remember { mutableStateOf(false) }
    var confirmarRestaurar by remember { mutableStateOf(false) }
    val preparandoExemplo by ModoTeste.trabalhando.collectAsStateWithLifecycle()

    val escolherBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            restaurando = true
            try {
                val entrada = ctx.contentResolver.openInputStream(uri) ?: error("Não foi possível abrir o arquivo.")
                entrada.use { ctx.bolao.backup.restaurar(it) }
                avisos.showSnackbar("Backup restaurado.")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível restaurar: ${t.message}")
            } finally {
                restaurando = false
            }
        }
    }
    val escolherTermoAssinado = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.guardarTermo(uri, assinado = true, aoErro = avisar)
    }
    val escolherTermoSemAssinatura = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.guardarTermo(uri, assinado = false, aoErro = avisar)
    }

    Scaffold(
        topBar = {
            BarraTitulo("Cadastro inicial", subtitulo = if (vm.passo > 0) "Passo ${vm.passo} de ${vm.totalPassos}" else null)
        },
        snackbarHost = { SnackbarHost(avisos) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            if (vm.passo > 0) {
                LinearProgressIndicator(
                    progress = { vm.passo / vm.totalPassos.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    vm.passo == 0 -> PassoBoasVindas(
                        restaurando = restaurando,
                        aoComecar = { vm.passo = 1 },
                        aoRestaurar = { confirmarRestaurar = true },
                        preparandoExemplo = preparandoExemplo,
                        aoExperimentar = { ModoTeste.entrar(ctx) },
                    )
                    vm.passo == 1 -> PassoModo(vm)
                    vm.individual -> PassoIndividual(vm)
                    vm.passo == 2 -> PassoIntegrantes(vm)
                    vm.passo == 3 -> PassoSaldo(vm)
                    vm.passo == 4 -> PassoValores(vm)
                    else -> PassoTermo(
                        vm,
                        aoEscolherSemAssinatura = { escolherTermoSemAssinatura.launch(arrayOf("application/pdf", "image/*")) },
                        aoEscolherAssinado = { escolherTermoAssinado.launch(arrayOf("application/pdf", "image/*")) },
                    )
                }
                if (vm.tentouAvancar) {
                    vm.problemasDoPasso().firstOrNull()?.let { Aviso(it, alerta = true) }
                }
            }
            if (vm.passo > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = vm::voltar, modifier = Modifier.weight(1f)) { Text("Voltar") }
                    if (vm.passo < vm.totalPassos) {
                        Button(onClick = vm::avancar, modifier = Modifier.weight(1f)) { Text("Continuar") }
                    } else {
                        Button(
                            onClick = { vm.concluir(avisar) },
                            enabled = !vm.salvando,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (vm.salvando) CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                            else Text("Concluir")
                        }
                    }
                }
            }
        }
    }

    if (confirmarRestaurar) {
        DialogoConfirmar(
            titulo = "Restaurar backup",
            texto = "Escolha o arquivo .zip de backup do Bolão dos Amigos (por exemplo, em Downloads/Bolao).",
            confirmar = "Escolher arquivo",
            aoConfirmar = {
                confirmarRestaurar = false
                escolherBackup.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            },
            aoFechar = { confirmarRestaurar = false },
        )
    }
}

@Composable
private fun PassoBoasVindas(
    restaurando: Boolean,
    aoComecar: () -> Unit,
    aoRestaurar: () -> Unit,
    preparandoExemplo: Boolean,
    aoExperimentar: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text("Bem-vindo ao Bolão dos Amigos", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Este cadastro é feito uma vez só. Os dados ficam apenas neste celular: " +
            "integrantes, chaves Pix, saldo inicial e aporte mensal.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = aoComecar, modifier = Modifier.fillMaxWidth()) { Text("Começar o cadastro") }
    Spacer(Modifier.height(16.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Já usava o app?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Se você tem um backup (.zip), restaure e tudo volta como estava, sem precisar cadastrar de novo.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = aoRestaurar, enabled = !restaurando) {
                Icon(Icons.Outlined.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (restaurando) "Restaurando..." else "Restaurar backup")
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Só quer conhecer o app?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Abre com um mês de exemplo já preenchido (integrantes, aportes, apostas com comprovante, " +
                    "um prêmio, rateio e extrato), sem digitar nada. Fica separado dos dados reais e sai com um toque.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            BotaoExperimentar(preparando = preparandoExemplo, aoTocar = aoExperimentar)
        }
    }
}

/** Passo 1: Bolão (o grupo) ou Individual. */
@Composable
private fun PassoModo(vm: CadastroViewModel) {
    Text("Como vai usar o app?", style = MaterialTheme.typography.titleLarge)
    OpcaoDeModo(
        titulo = "Bolão",
        texto = "Para um grupo de 2 ou mais pessoas, com o termo, os aportes, o rateio, o extrato e os comprovantes " +
            "enviados ao grupo do WhatsApp.",
        escolhida = !vm.individual,
        aoEscolher = { vm.modo = Modo.BOLAO },
    )
    OpcaoDeModo(
        titulo = "Individual",
        texto = "Só para você, bem enxuto: registra as apostas e os prêmios. O saldo começa em zero e cai a cada aposta " +
            "(mostra quanto você gastou) e sobe com os prêmios. Sem comprovantes, sem termo; tudo pode ser excluído e o " +
            "app pode ser apagado a qualquer momento.",
        escolhida = vm.individual,
        aoEscolher = { vm.modo = Modo.INDIVIDUAL },
    )
}

@Composable
private fun OpcaoDeModo(titulo: String, texto: String, escolhida: Boolean, aoEscolher: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (escolhida) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = escolhida, onClick = aoEscolher, role = Role.RadioButton),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected = escolhida, onClick = null)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Modo individual: o nome (opcional) e o que muda. */
@Composable
private fun PassoIndividual(vm: CadastroViewModel) {
    Text("Modo individual", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = vm.nomeIndividual,
        onValueChange = { vm.nomeIndividual = it },
        label = { Text("Seu nome (opcional)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    Aviso(
        "O saldo começa em R$ 0,00 e mostra o resultado das suas apostas: cada aposta desconta o preço e cada prêmio soma. " +
            "Os preços da aposta simples já vêm preenchidos e podem ser mudados em Ajustes.",
    )
}

@Composable
private fun PassoIntegrantes(vm: CadastroViewModel) {
    Text("Integrantes", style = MaterialTheme.typography.titleLarge)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Quantos integrantes?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        OutlinedIconButton(onClick = { vm.mudarQuantidade(vm.pessoas.size - 1) }, enabled = vm.pessoas.size > MINIMO_DE_INTEGRANTES) {
            Icon(Icons.Outlined.Remove, contentDescription = "Menos um integrante")
        }
        Text(
            vm.pessoas.size.toString(),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        OutlinedIconButton(onClick = { vm.mudarQuantidade(vm.pessoas.size + 1) }, enabled = vm.pessoas.size < MAXIMO_DE_INTEGRANTES) {
            Icon(Icons.Outlined.Add, contentDescription = "Mais um integrante")
        }
    }
    Text(
        "No mínimo $MINIMO_DE_INTEGRANTES. Cada um fica com 1/${vm.pessoas.size} de qualquer prêmio e obrigação. " +
            "O nome curto aparece nas mensagens, ex.: \"APORTE (Fulano)\". Marque quem é o Administrador.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    vm.pessoas.forEachIndexed { i, p ->
        val marcarErro = vm.tentouAvancar
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = vm.administrador == i, onClick = { vm.administrador = i }, role = Role.RadioButton),
                ) {
                    Text("Integrante ${i + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("Administrador", style = MaterialTheme.typography.bodyMedium)
                    RadioButton(selected = vm.administrador == i, onClick = null)
                }
                OutlinedTextField(
                    value = p.nome,
                    onValueChange = { p.nome = it },
                    label = { Text("Nome completo") },
                    isError = marcarErro && p.nome.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = p.apelido,
                    onValueChange = { p.apelido = it },
                    label = { Text("Nome curto (para as mensagens)") },
                    isError = marcarErro && p.apelido.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = p.pix,
                    onValueChange = { p.pix = it },
                    label = { Text("Chave Pix") },
                    isError = marcarErro && p.pix.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PassoSaldo(vm: CadastroViewModel) {
    val hoje = LocalDate.now()
    val atual = YearMonth.from(hoje)
    Text("Saldo inicial", style = MaterialTheme.typography.titleLarge)
    Text(
        "Quanto o bolão já tem na caixinha hoje? Se ele já começa com caixa, não precisa de aporte para começar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CampoDinheiro(
        centavos = vm.saldoInicial,
        aoMudar = { vm.saldoInicial = it },
        rotulo = "Saldo inicial da caixinha",
        ajuda = "Pode ficar R$ 0,00 se o bolão começa sem caixa.",
        imeAction = ImeAction.Done,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    Text("O aporte mensal começa em", style = MaterialTheme.typography.titleMedium)
    val inicio = vm.inicioAportes(hoje)
    listOf(atual, atual.plusMonths(1)).forEach { m ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = inicio == m, onClick = { vm.inicioEscolhido = m }, role = Role.RadioButton),
        ) {
            RadioButton(selected = inicio == m, onClick = null)
            Column(Modifier.padding(start = 8.dp)) {
                Text(Datas.mesAnoTitulo(m), style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (m == atual) "O aporte deste mês será cobrado (data fixa no dia 1º)."
                    else "Este mês fica coberto pelo saldo inicial; o primeiro aporte é ${Datas.quando(m.atDay(1), hoje)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PassoValores(vm: CadastroViewModel) {
    Text("Aporte mensal", style = MaterialTheme.typography.titleLarge)
    CampoDinheiro(
        centavos = vm.aporteMensal,
        aoMudar = { vm.aporteMensal = it },
        rotulo = "Aporte de cada integrante no dia 1º",
        ajuda = "Total do mês: ${Dinheiro.rs(vm.aporteMensal * vm.pessoas.size)} (${vm.pessoas.size} integrantes)",
        erro = vm.tentouAvancar && vm.aporteMensal <= 0,
        imeAction = ImeAction.Done,
        modifier = Modifier.fillMaxWidth(),
    )
    Aviso(
        "Os preços da aposta simples já vêm preenchidos (valores de out/2026) e podem ser mudados depois em Ajustes.",
    )
}

@Composable
private fun PassoTermo(vm: CadastroViewModel, aoEscolherSemAssinatura: () -> Unit, aoEscolherAssinado: () -> Unit) {
    Text("Termo", style = MaterialTheme.typography.titleLarge)
    Text(
        "Anexe o termo sem assinaturas e o termo assinado pelos ${vm.pessoas.size}. Se ainda não tiver, pule e anexe depois em Ajustes.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = aoEscolherSemAssinatura, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(vm.termoSemAssinatura?.let { "Sem assinaturas: ${it.nomeOriginal}" } ?: "Anexar o termo sem assinaturas")
    }
    OutlinedButton(onClick = aoEscolherAssinado, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(vm.termo?.let { "Assinado: ${it.nomeOriginal}" } ?: "Anexar o termo assinado")
    }
    Spacer(Modifier.height(8.dp))
    Text("Confira", style = MaterialTheme.typography.titleMedium)
    vm.pessoas.forEachIndexed { i, p ->
        Text(
            "• ${p.apelido.trim()}${if (i == vm.administrador) " (Administrador)" else ""} — Pix: ${p.pix.trim()}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Text("• Saldo inicial: ${Dinheiro.rs(vm.saldoInicial)}", style = MaterialTheme.typography.bodyMedium)
    Text("• Primeiro aporte mensal: ${Datas.mesAno(vm.inicioAportes(LocalDate.now()))}", style = MaterialTheme.typography.bodyMedium)
    Text("• Aporte mensal: ${Dinheiro.rs(vm.aporteMensal)} por integrante", style = MaterialTheme.typography.bodyMedium)
    TextButton(onClick = { vm.passo = 2 }) { Text("Corrigir algo") }
}

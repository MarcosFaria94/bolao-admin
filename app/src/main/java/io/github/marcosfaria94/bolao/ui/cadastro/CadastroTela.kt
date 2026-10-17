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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.CampoInteiro
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.teste.BotaoExperimentar
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.launch
import java.time.LocalDate

class FormPessoa {
    var nome by mutableStateOf("")
    var apelido by mutableStateOf("")
    var pix by mutableStateOf("")
}

class CadastroViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repositorio = app.bolao.repo

    var passo by mutableIntStateOf(0)
    val pessoas = mutableStateListOf(FormPessoa(), FormPessoa(), FormPessoa())
    var administrador by mutableIntStateOf(0)
    var nomeGrupo by mutableStateOf("")
    var saldoInicial by mutableLongStateOf(0L)
    var proximoJogo by mutableStateOf<Int?>(1)
    var proximoRateio by mutableStateOf<Int?>(1)
    var aporteMensal by mutableLongStateOf(Config().aporteMensal)
    var termo by mutableStateOf<Arquivo?>(null)
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

    fun problemasNumeros(): List<String> = buildList {
        if ((proximoJogo ?: 0) < 1) add("Informe o próximo número de jogo.")
        if ((proximoRateio ?: 0) < 1) add("Informe o próximo número de rateio.")
    }

    fun problemasValores(): List<String> = if (aporteMensal <= 0) listOf("Informe o aporte mensal.") else emptyList()

    fun problemasDoPasso(): List<String> = when (passo) {
        1 -> problemasPessoas()
        2 -> problemasNumeros()
        3 -> problemasValores()
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

    fun guardarTermo(uri: android.net.Uri, aoErro: (String) -> Unit) {
        viewModelScope.launch {
            try {
                termo = repo.guardarArquivo(uri, "termo")
            } catch (t: Throwable) {
                aoErro("Não foi possível guardar o PDF: ${t.message}")
            }
        }
    }

    fun concluir(aoErro: (String) -> Unit) {
        if (salvando) return
        val problemas = problemasPessoas() + problemasNumeros() + problemasValores()
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
                        cadastroConcluido = true,
                        nomeGrupo = nomeGrupo.trim(),
                        pessoas = lista,
                        config = it.config.copy(aporteMensal = aporteMensal),
                        proximoJogo = proximoJogo ?: 1,
                        proximoRateio = proximoRateio ?: 1,
                        lancamentos = it.lancamentos + lancamentos,
                        termo = termo ?: it.termo,
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

private const val TOTAL_PASSOS = 4

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CadastroTela(vm: CadastroViewModel = viewModel()) {
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
    val escolherTermo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.guardarTermo(uri, avisar)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Cadastro inicial")
                    if (vm.passo > 0) {
                        Text(
                            "Passo ${vm.passo} de $TOTAL_PASSOS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            })
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
                    progress = { vm.passo / TOTAL_PASSOS.toFloat() },
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
                when (vm.passo) {
                    0 -> PassoBoasVindas(
                        restaurando = restaurando,
                        aoComecar = { vm.passo = 1 },
                        aoRestaurar = { confirmarRestaurar = true },
                        preparandoExemplo = preparandoExemplo,
                        aoExperimentar = { ModoTeste.entrar(ctx) },
                    )
                    1 -> PassoIntegrantes(vm)
                    2 -> PassoSaldo(vm)
                    3 -> PassoValores(vm)
                    else -> PassoTermo(vm, aoEscolher = { escolherTermo.launch(arrayOf("application/pdf")) })
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
                    if (vm.passo < TOTAL_PASSOS) {
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
            texto = "Escolha o arquivo .zip de backup do Bolão Admin (por exemplo, em Downloads/Bolao).",
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
    Text("Bem-vindo ao Bolão Admin", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Este cadastro é feito uma vez só. Os dados ficam apenas neste celular: " +
            "integrantes, chaves Pix, saldo e numeração dos jogos.",
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

@Composable
private fun PassoIntegrantes(vm: CadastroViewModel) {
    Text("Integrantes", style = MaterialTheme.typography.titleLarge)
    Text(
        "O nome curto aparece nas mensagens, ex.: \"APORTE (Fulano)\". Marque quem é o Administrador.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = vm.nomeGrupo,
        onValueChange = { vm.nomeGrupo = it },
        label = { Text("Nome do grupo (opcional)") },
        placeholder = { Text("Ex.: Bolão dos Amigos") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
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
    Text("Saldo e numeração", style = MaterialTheme.typography.titleLarge)
    Text(
        "Use o saldo de hoje da caixinha do bolão. Se o bolão já vinha funcionando, " +
            "informe o próximo número de jogo e de rateio para continuar a sequência.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CampoDinheiro(
        centavos = vm.saldoInicial,
        aoMudar = { vm.saldoInicial = it },
        rotulo = "Saldo inicial da caixinha",
        ajuda = "Pode ficar R$ 0,00 se o bolão está começando agora.",
        modifier = Modifier.fillMaxWidth(),
    )
    CampoInteiro(
        valor = vm.proximoJogo,
        aoMudar = { vm.proximoJogo = it },
        rotulo = "Próximo número de jogo",
        prefixo = "#",
        ajuda = "O próximo jogo será o #${vm.proximoJogo ?: 1}.",
        erro = vm.tentouAvancar && (vm.proximoJogo ?: 0) < 1,
        modifier = Modifier.fillMaxWidth(),
    )
    CampoInteiro(
        valor = vm.proximoRateio,
        aoMudar = { vm.proximoRateio = it },
        rotulo = "Próximo número de rateio",
        prefixo = "Nº",
        ajuda = "O próximo rateio será o Nº${vm.proximoRateio ?: 1}.",
        erro = vm.tentouAvancar && (vm.proximoRateio ?: 0) < 1,
        imeAction = ImeAction.Done,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PassoValores(vm: CadastroViewModel) {
    Text("Aporte mensal", style = MaterialTheme.typography.titleLarge)
    CampoDinheiro(
        centavos = vm.aporteMensal,
        aoMudar = { vm.aporteMensal = it },
        rotulo = "Aporte de cada integrante no dia 1º",
        ajuda = "Total do mês: ${Dinheiro.rs(vm.aporteMensal * 3)}",
        erro = vm.tentouAvancar && vm.aporteMensal <= 0,
        imeAction = ImeAction.Done,
        modifier = Modifier.fillMaxWidth(),
    )
    Aviso(
        "Os preços da aposta simples já vêm preenchidos (valores de out/2026) e podem ser mudados depois em Ajustes.",
    )
}

@Composable
private fun PassoTermo(vm: CadastroViewModel, aoEscolher: () -> Unit) {
    Text("Termo assinado", style = MaterialTheme.typography.titleLarge)
    Text(
        "Guarde o PDF do termo com as 3 assinaturas. Se ainda não estiver assinado, pule e guarde depois em Ajustes.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = aoEscolher, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(vm.termo?.let { "Trocar: ${it.nomeOriginal}" } ?: "Escolher o PDF")
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
    Text("• Próximo jogo: #${vm.proximoJogo ?: 1} · próximo rateio: Nº${vm.proximoRateio ?: 1}", style = MaterialTheme.typography.bodyMedium)
    Text("• Aporte mensal: ${Dinheiro.rs(vm.aporteMensal)} por integrante", style = MaterialTheme.typography.bodyMedium)
    TextButton(onClick = { vm.passo = 1 }) { Text("Corrigir algo") }
}

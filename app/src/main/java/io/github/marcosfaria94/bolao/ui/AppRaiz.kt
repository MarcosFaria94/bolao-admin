package io.github.marcosfaria94.bolao.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Selos
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import io.github.marcosfaria94.bolao.ui.ajustes.AjustesTela
import io.github.marcosfaria94.bolao.ui.aporte.AporteTela
import io.github.marcosfaria94.bolao.ui.aposta.ApostaTela
import io.github.marcosfaria94.bolao.ui.aposta.ApostasTela
import io.github.marcosfaria94.bolao.ui.aposta.NovaApostaTela
import io.github.marcosfaria94.bolao.ui.apuracao.ApuracaoTela
import io.github.marcosfaria94.bolao.ui.atualizacao.AtualizacaoViewModel
import io.github.marcosfaria94.bolao.ui.atualizacao.DialogosAtualizacao
import io.github.marcosfaria94.bolao.ui.cadastro.CadastroTela
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.extincao.ExtincaoTela
import io.github.marcosfaria94.bolao.ui.extrato.ExtratoTela
import io.github.marcosfaria94.bolao.ui.financeiro.FinanceiroTela
import io.github.marcosfaria94.bolao.ui.financeiro.SaldoTela
import io.github.marcosfaria94.bolao.ui.ajustes.IntegridadeTela
import io.github.marcosfaria94.bolao.ui.jogos.JogosTela
import io.github.marcosfaria94.bolao.ui.jogos.ModalidadeTela
import io.github.marcosfaria94.bolao.ui.rateio.RateioTela
import io.github.marcosfaria94.bolao.ui.teste.DialogoSairDoTeste
import io.github.marcosfaria94.bolao.ui.teste.FaixaModoTeste
import java.time.LocalDate

private enum class Aba(val rota: String, val rotulo: String, val icone: ImageVector) {
    FINANCEIRO("financeiro", "Financeiro", Icons.Outlined.AccountBalanceWallet),
    APOSTAS("apostas", "Apostas", Icons.Outlined.ConfirmationNumber),
    JOGOS("jogos", "Jogos", Icons.Outlined.Leaderboard),
    AJUSTES("ajustes", "Ajustes", Icons.Outlined.Settings),
}

@Composable
fun AppRaiz() {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val estado by repo.estado.collectAsStateWithLifecycle()
    val aviso by repo.aviso.collectAsStateWithLifecycle()
    val teste by repo.modoTeste.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val atualizacao: AtualizacaoViewModel = viewModel()

    LaunchedEffect(aviso) {
        aviso?.let {
            avisos.showSnackbar(it)
            repo.limparAviso()
        }
    }

    CompositionLocalProvider(LocalAvisos provides avisos) {
        val e = estado
        when {
            e == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            // Extinção em andamento: só a tela da extinção, até o fim.
            e.extincao != null -> ExtincaoTela(e)
            !e.cadastroConcluido -> CadastroTela()
            // Ao entrar ou sair do modo de teste, a navegação recomeça do Financeiro.
            else -> key(teste) { Principal(e, atualizacao, teste) }
        }
        DialogosAtualizacao(atualizacao)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Principal(estado: Estado, atualizacao: AtualizacaoViewModel, teste: Boolean) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    var sairDoTeste by remember { mutableStateOf(false) }
    val hoje = LocalDate.now()
    val selos = Selos.de(estado, hoje)

    // Ao abrir: confere os sorteios e os índices na hora (sem esperar a próxima hora).
    LaunchedEffect(Unit) {
        Vigia.rodarAgora(ctx)
        ctx.bolao.indices.atualizar()
    }

    // Notificação tocada: abre a tela correspondente.
    val destino by ctx.bolao.destinoPendente.collectAsStateWithLifecycle()
    LaunchedEffect(destino) {
        destino?.let { d ->
            ctx.bolao.destinoPendente.value = null
            val rota = if (d == "painel") Aba.FINANCEIRO.rota else d
            if (rota != Aba.FINANCEIRO.rota) runCatching { nav.navigate(rota) { launchSingleTop = true } }
        }
    }

    // Android 13+: pede a permissão de notificação uma vez, logo depois do cadastro.
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) Vigia.rodarAgora(ctx)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notificador.permitido(ctx)) {
            val prefs = ctx.getSharedPreferences("app", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("pediu_notificacao", false)) {
                prefs.edit().putBoolean("pediu_notificacao", true).apply()
                pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val entrada by nav.currentBackStackEntryAsState()
    val rota = entrada?.destination?.route ?: Aba.FINANCEIRO.rota
    val ehAba = Aba.entries.any { it.rota == rota }
    val titulo = when (rota) {
        Aba.FINANCEIRO.rota -> estado.nomeExibicao
        Aba.APOSTAS.rota -> "Apostas"
        Aba.JOGOS.rota -> "Jogos"
        Aba.AJUSTES.rota -> "Ajustes"
        "aporte" -> "Aporte"
        "rateio" -> "Rateio"
        "extrato" -> "Extrato"
        "apostar" -> "Nova aposta"
        "aposta/{id}" -> "Aposta"
        "apuracao/{id}" -> "Apuração"
        "modalidade/{nome}" -> "Jogos"
        "saldo" -> "Saldo"
        "integridade" -> "Verificar integridade"
        else -> ""
    }
    fun abrir(r: String) = nav.navigate(r) { launchSingleTop = true }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(titulo) },
                    navigationIcon = {
                        if (!ehAba) {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                            }
                        }
                    },
                )
                if (teste) FaixaModoTeste(aoSair = { sairDoTeste = true })
            }
        },
        bottomBar = {
            if (ehAba) {
                NavigationBar {
                    Aba.entries.forEach { aba ->
                        val n = when (aba) {
                            Aba.FINANCEIRO -> selos.financeiro
                            Aba.APOSTAS -> selos.apostas
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = rota == aba.rota,
                            onClick = {
                                nav.navigate(aba.rota) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                BadgedBox(badge = { if (n > 0) Badge { Text(n.toString()) } }) {
                                    Icon(aba.icone, contentDescription = null)
                                }
                            },
                            label = { Text(aba.rotulo) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(LocalAvisos.current) },
        // Nas abas a barra de baixo já cuida da margem do sistema; nas outras telas, o conteúdo cuida.
        contentWindowInsets = if (ehAba) WindowInsets(0, 0, 0, 0) else WindowInsets.navigationBars,
    ) { padding ->
        NavHost(nav, startDestination = Aba.FINANCEIRO.rota, modifier = Modifier.padding(padding)) {
            composable(Aba.FINANCEIRO.rota) { FinanceiroTela(estado, selos, abrir = ::abrir, teste = teste) }
            composable(Aba.APOSTAS.rota) { ApostasTela(estado, abrir = ::abrir) }
            composable(Aba.JOGOS.rota) { JogosTela(estado, abrirModalidade = { abrir("modalidade/${it.name}") }) }
            composable(Aba.AJUSTES.rota) { AjustesTela(estado, atualizacao, abrir = ::abrir) }
            composable("saldo") { SaldoTela(estado) }
            composable("integridade") { IntegridadeTela(estado) }
            composable("aporte") { AporteTela(estado) }
            composable("rateio") { RateioTela(estado) }
            composable("extrato") { ExtratoTela(estado) }
            composable("apostar") {
                NovaApostaTela(estado, abrirAposta = { id ->
                    nav.navigate("aposta/$id") { popUpTo("apostar") { inclusive = true } }
                })
            }
            composable("aposta/{id}") { e ->
                ApostaTela(
                    estado,
                    id = e.arguments?.getString("id").orEmpty(),
                    abrirApuracao = { abrir("apuracao/$it") },
                    aoSair = { nav.popBackStack() },
                )
            }
            composable("apuracao/{id}") { e -> ApuracaoTela(estado, e.arguments?.getString("id").orEmpty()) }
            composable("modalidade/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                if (m != null) {
                    ModalidadeTela(estado, m, abrirAposta = { id -> nav.navigate("aposta/$id") })
                }
            }
        }
    }

    if (sairDoTeste) DialogoSairDoTeste(aoFechar = { sairDoTeste = false })
}

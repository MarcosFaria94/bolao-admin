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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import io.github.marcosfaria94.bolao.data.Pendencias
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
import io.github.marcosfaria94.bolao.ui.comum.ComprovanteTela
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.extincao.ExtincaoTela
import io.github.marcosfaria94.bolao.ui.extrato.ExtratoTela
import io.github.marcosfaria94.bolao.ui.financeiro.FinanceiroTela
import io.github.marcosfaria94.bolao.ui.financeiro.SaldoTela
import io.github.marcosfaria94.bolao.ui.ajustes.IntegridadeTela
import io.github.marcosfaria94.bolao.ui.jogos.JogosTela
import io.github.marcosfaria94.bolao.ui.jogos.ModalidadeTela
import io.github.marcosfaria94.bolao.ui.notificacoes.NotificacoesTela
import io.github.marcosfaria94.bolao.ui.rateio.RateioTela
import io.github.marcosfaria94.bolao.ui.teste.DialogoSairDoTeste
import io.github.marcosfaria94.bolao.ui.teste.FaixaModoTeste
import io.github.marcosfaria94.bolao.ui.tema.Barra
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.YearMonth

private enum class Aba(val rota: String, val rotulo: String, val icone: ImageVector, val iconeEscolhido: ImageVector) {
    FINANCEIRO("financeiro", "Financeiro", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    APOSTAS("apostas", "Apostas", Icones.Trevo, Icones.TrevoCheio),
    JOGOS("jogos", "Jogos", Icons.Outlined.Leaderboard, Icons.Filled.Leaderboard),
    NOTIFICACOES("notificacoes", "Notificações", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    AJUSTES("ajustes", "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
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
    // Contador do sino: todas as pendências de agora (refeitas a cada minuto, ex.: virada do dia).
    val agora by produceState(LocalDateTime.now()) {
        while (true) {
            delay(60_000)
            value = LocalDateTime.now()
        }
    }
    val pendencias = Pendencias.de(estado, agora)

    // Ao abrir: confere os sorteios e os índices na hora (sem esperar a próxima hora).
    LaunchedEffect(Unit) {
        Vigia.rodarAgora(ctx)
        ctx.bolao.indices.atualizar()
    }

    fun abrirAba(rota: String) = nav.navigate(rota) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    // Notificação tocada: abre a tela correspondente.
    val destino by ctx.bolao.destinoPendente.collectAsStateWithLifecycle()
    LaunchedEffect(destino) {
        destino?.let { d ->
            // O NavHost fica dentro do Scaffold, que só o compõe na hora do layout: ao abrir o app
            // por uma notificação, espera a navegação existir (antes disso o pedido se perdia).
            nav.currentBackStackEntryFlow.first()
            ctx.bolao.destinoPendente.value = null
            runCatching {
                when {
                    d == "painel" || d == Aba.FINANCEIRO.rota -> Unit
                    Aba.entries.any { it.rota == d } -> abrirAba(d)
                    else -> nav.navigate(d) { launchSingleTop = true }
                }
            }
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
        Aba.NOTIFICACOES.rota -> "Notificações"
        Aba.AJUSTES.rota -> "Ajustes"
        "aporte" -> "Aporte"
        "rateio" -> "Rateio"
        "extrato", "extrato/{mes}" -> "Extrato"
        "comprovante/{id}" -> "Comprovante"
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
                // Azul-noite do ícone, com a aba escolhida em dourado.
                NavigationBar(containerColor = Barra.fundo(), contentColor = Barra.outras()) {
                    Aba.entries.forEach { aba ->
                        val escolhida = rota == aba.rota
                        NavigationBarItem(
                            selected = escolhida,
                            onClick = { abrirAba(aba.rota) },
                            icon = {
                                val n = if (aba == Aba.NOTIFICACOES) pendencias.size else 0
                                BadgedBox(badge = {
                                    if (n > 0) {
                                        Badge(containerColor = Barra.contador, contentColor = Color.White) {
                                            Text(if (n > 99) "99+" else n.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }) {
                                    Icon(if (escolhida) aba.iconeEscolhido else aba.icone, contentDescription = null)
                                }
                            },
                            label = {
                                Text(
                                    aba.rotulo,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (escolhida) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Barra.escolhida,
                                selectedTextColor = Barra.escolhida,
                                unselectedIconColor = Barra.outras(),
                                unselectedTextColor = Barra.outras(),
                                indicatorColor = Color.Transparent,
                            ),
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
            composable(Aba.FINANCEIRO.rota) { FinanceiroTela(estado, abrir = ::abrir) }
            composable(Aba.APOSTAS.rota) { ApostasTela(estado, abrir = ::abrir) }
            composable(Aba.JOGOS.rota) { JogosTela(estado, abrirModalidade = { abrir("modalidade/${it.name}") }) }
            composable(Aba.NOTIFICACOES.rota) { NotificacoesTela(pendencias, abrir = ::abrir) }
            composable(Aba.AJUSTES.rota) { AjustesTela(estado, atualizacao, abrir = ::abrir) }
            composable("saldo") { SaldoTela(estado) }
            composable("integridade") { IntegridadeTela(estado) }
            composable("aporte") { AporteTela(estado, abrir = ::abrir) }
            composable("rateio") { RateioTela(estado, abrir = ::abrir) }
            composable("extrato") { ExtratoTela(estado) }
            composable("extrato/{mes}") { e ->
                val mes = runCatching { YearMonth.parse(e.arguments?.getString("mes").orEmpty()) }.getOrNull()
                ExtratoTela(estado, mesInicial = mes)
            }
            composable("comprovante/{id}") { e -> ComprovanteTela(estado, e.arguments?.getString("id").orEmpty()) }
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
            composable("apuracao/{id}") { e -> ApuracaoTela(estado, e.arguments?.getString("id").orEmpty(), abrir = ::abrir) }
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

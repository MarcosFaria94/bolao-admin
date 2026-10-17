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
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
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
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import io.github.marcosfaria94.bolao.ui.ajustes.AjustesTela
import io.github.marcosfaria94.bolao.ui.aporte.AporteTela
import io.github.marcosfaria94.bolao.ui.aposta.ApostaTela
import io.github.marcosfaria94.bolao.ui.aposta.NovaApostaTela
import io.github.marcosfaria94.bolao.ui.apuracao.ApuracaoTela
import io.github.marcosfaria94.bolao.ui.extrato.ExtratoTela
import io.github.marcosfaria94.bolao.ui.rateio.RateioTela
import io.github.marcosfaria94.bolao.ui.atualizacao.AtualizacaoViewModel
import io.github.marcosfaria94.bolao.ui.atualizacao.DialogosAtualizacao
import io.github.marcosfaria94.bolao.ui.cadastro.CadastroTela
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.jogos.JogosTela
import io.github.marcosfaria94.bolao.ui.painel.PainelTela
import io.github.marcosfaria94.bolao.ui.teste.DialogoSairDoTeste
import io.github.marcosfaria94.bolao.ui.teste.FaixaModoTeste

private data class Aba(val rota: String, val rotulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba("painel", "Painel", Icons.Outlined.Home),
    Aba("jogos", "Jogos", Icons.Outlined.Casino),
    Aba("ajustes", "Ajustes", Icons.Outlined.Settings),
)

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
            !e.cadastroConcluido -> CadastroTela()
            // Ao entrar ou sair do modo de teste, a navegação recomeça do Painel.
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

    // Notificação tocada: abre a tela correspondente.
    val destino by ctx.bolao.destinoPendente.collectAsStateWithLifecycle()
    LaunchedEffect(destino) {
        destino?.let { d ->
            ctx.bolao.destinoPendente.value = null
            if (d != "painel") runCatching { nav.navigate(d) { launchSingleTop = true } }
        }
    }

    // Android 13+: pede a permissão de notificação uma vez, logo depois do cadastro.
    // Com a permissão dada, os avisos pendentes já aparecem (sem esperar a próxima hora).
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
    val rota = entrada?.destination?.route ?: "painel"
    val ehAba = ABAS.any { it.rota == rota }
    val titulo = when (rota) {
        "painel" -> estado.nomeGrupo.ifBlank { "Bolão" }
        "jogos" -> "Jogos"
        "ajustes" -> "Ajustes"
        "aporte" -> "Aporte"
        "apostar" -> "Nova aposta"
        "aposta/{id}" -> "Aposta"
        "apuracao/{id}" -> "Apuração"
        "rateio" -> "Rateio"
        "extrato" -> "Extrato do mês"
        else -> ""
    }

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
                    ABAS.forEach { aba ->
                        NavigationBarItem(
                            selected = rota == aba.rota,
                            onClick = {
                                nav.navigate(aba.rota) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(aba.icone, contentDescription = null) },
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
        NavHost(nav, startDestination = "painel", modifier = Modifier.padding(padding)) {
            composable("painel") { PainelTela(estado, abrir = { nav.navigate(it) }, teste = teste) }
            composable("jogos") { JogosTela(estado) }
            composable("ajustes") { AjustesTela(estado, atualizacao) }
            composable("aporte") { AporteTela(estado, aoConcluir = { nav.popBackStack() }) }
            composable("apostar") {
                NovaApostaTela(estado, abrirAposta = { id ->
                    nav.navigate("aposta/$id") { popUpTo("apostar") { inclusive = true } }
                })
            }
            composable("aposta/{id}") { e ->
                ApostaTela(
                    estado,
                    id = e.arguments?.getString("id").orEmpty(),
                    abrirApuracao = { nav.navigate("apuracao/$it") },
                    aoSair = { nav.popBackStack() },
                )
            }
            composable("apuracao/{id}") { e -> ApuracaoTela(estado, e.arguments?.getString("id").orEmpty()) }
            composable("rateio") { RateioTela(estado, aoConcluir = { nav.popBackStack() }) }
            composable("extrato") { ExtratoTela(estado, aoConcluir = { nav.popBackStack() }) }
        }
    }

    if (sairDoTeste) DialogoSairDoTeste(aoFechar = { sairDoTeste = false })
}

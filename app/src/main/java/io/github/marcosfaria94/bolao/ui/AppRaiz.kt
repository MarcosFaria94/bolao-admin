package io.github.marcosfaria94.bolao.ui

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
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
import io.github.marcosfaria94.bolao.ui.ajustes.AjustesTela
import io.github.marcosfaria94.bolao.ui.atualizacao.AtualizacaoViewModel
import io.github.marcosfaria94.bolao.ui.atualizacao.DialogosAtualizacao
import io.github.marcosfaria94.bolao.ui.cadastro.CadastroTela
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.jogos.JogosTela
import io.github.marcosfaria94.bolao.ui.painel.PainelTela

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
            else -> Principal(e, atualizacao)
        }
        DialogosAtualizacao(atualizacao)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Principal(estado: Estado, atualizacao: AtualizacaoViewModel) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rota = entrada?.destination?.route ?: "painel"
    val ehAba = ABAS.any { it.rota == rota }
    val titulo = when (rota) {
        "painel" -> estado.nomeGrupo.ifBlank { "Bolão" }
        "jogos" -> "Jogos"
        "ajustes" -> "Ajustes"
        else -> ""
    }

    Scaffold(
        topBar = {
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
            composable("painel") { PainelTela(estado, abrir = { nav.navigate(it) }) }
            composable("jogos") { JogosTela(estado) }
            composable("ajustes") { AjustesTela(estado, atualizacao) }
        }
    }
}

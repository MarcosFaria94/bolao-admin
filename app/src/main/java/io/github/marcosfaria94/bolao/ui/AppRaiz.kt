package io.github.marcosfaria94.bolao.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import io.github.marcosfaria94.bolao.ui.ajustes.AjustesTela
import io.github.marcosfaria94.bolao.ui.aporte.AporteTela
import io.github.marcosfaria94.bolao.ui.atualizacao.AtualizacaoViewModel
import io.github.marcosfaria94.bolao.ui.atualizacao.DialogosAtualizacao
import io.github.marcosfaria94.bolao.ui.cadastro.CadastroTela
import io.github.marcosfaria94.bolao.ui.comprovantes.ComprovantesTela
import io.github.marcosfaria94.bolao.ui.comum.BarraTitulo
import io.github.marcosfaria94.bolao.ui.comum.ComprovanteTela
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.extincao.ExtincaoTela
import io.github.marcosfaria94.bolao.ui.extrato.ExtratoTela
import io.github.marcosfaria94.bolao.ui.home.HomeTela
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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth

private enum class Aba(val rota: String, val rotulo: String, val icone: ImageVector, val iconeEscolhido: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    JOGOS("jogos", "Jogos", Icones.Trevo, Icones.TrevoCheio),
    COMPROVANTES(Pendencias.ABA_COMPROVANTES, "Comprovantes", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    AJUSTES("ajustes", "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** As abas do modo: o individual não tem comprovantes. */
private fun abasDe(e: Estado): List<Aba> = if (e.individual) listOf(Aba.HOME, Aba.JOGOS, Aba.AJUSTES) else Aba.entries

/** A tela das abas (as páginas que se trocam deslizando o dedo para os lados). */
private const val ROTA_ABAS = "abas"

/** Endereços antigos (avisos de versões anteriores): "financeiro" e "apostas" eram abas; "apostar", a Nova aposta. */
private val PARA_A_HOME = setOf("painel", "financeiro", "apostas")

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
            // Ao entrar ou sair do modo de teste (ou trocar de modo), a navegação recomeça da Home.
            else -> key(teste, e.modo) { Principal(e, atualizacao, teste) }
        }
        DialogosAtualizacao(atualizacao)
    }
}

@Composable
private fun Principal(estado: Estado, atualizacao: AtualizacaoViewModel, teste: Boolean) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var sairDoTeste by remember { mutableStateOf(false) }
    // Contador do sino (no Histórico da Home): todas as pendências de agora (refeitas a cada minuto, ex.: virada do dia).
    val agora by produceState(LocalDateTime.now()) {
        while (true) {
            delay(60_000)
            value = LocalDateTime.now()
        }
    }
    // Pendências do bolão + avisos dos índices (preço diferente, regras da Caixa que mudaram).
    val avisosIndices by ctx.bolao.indices.avisos.collectAsStateWithLifecycle()
    val pendencias = Pendencias.comAvisosDosIndices(Pendencias.de(estado, agora), avisosIndices)

    // Ao abrir: confere os sorteios e os índices na hora (sem esperar a próxima hora).
    LaunchedEffect(Unit) {
        Vigia.rodarAgora(ctx)
        ctx.bolao.indices.atualizar()
    }
    // O concurso das apostas que ainda não valem (em andamento ou esperando o comprovante) acompanha a Caixa.
    val itens by ctx.bolao.indices.itens.collectAsStateWithLifecycle()
    LaunchedEffect(itens) { atualizarConcursos(ctx, itens) }

    val abas = abasDe(estado)
    val paginas = rememberPagerState { abas.size }

    /** Vai para a aba [aba], fechando o que estiver aberto por cima. */
    fun irParaAba(aba: Aba) {
        val i = abas.indexOf(aba).takeIf { it >= 0 } ?: 0
        if (nav.currentBackStackEntry?.destination?.route != ROTA_ABAS) nav.popBackStack(ROTA_ABAS, inclusive = false)
        escopo.launch { paginas.scrollToPage(i) }
    }

    // Uma aba (ex.: a pendência "Fazer a aposta de hoje" abre os Jogos) troca de aba; o resto abre por cima.
    fun abrir(r: String) {
        val aba = Aba.entries.firstOrNull { it.rota == r }
        when {
            aba != null -> irParaAba(aba)
            r in PARA_A_HOME -> irParaAba(Aba.HOME)
            r == "apostar" -> irParaAba(Aba.JOGOS)
            else -> nav.navigate(r) { launchSingleTop = true }
        }
    }

    // Notificação tocada: abre a tela correspondente.
    val destino by ctx.bolao.destinoPendente.collectAsStateWithLifecycle()
    LaunchedEffect(destino) {
        destino?.let { d ->
            // O NavHost fica dentro do Scaffold, que só o compõe na hora do layout: ao abrir o app
            // por uma notificação, espera a navegação existir (antes disso o pedido se perdia).
            nav.currentBackStackEntryFlow.first()
            ctx.bolao.destinoPendente.value = null
            runCatching { abrir(d) }
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
    val rota = entrada?.destination?.route ?: ROTA_ABAS
    val ehAba = rota == ROTA_ABAS
    val abaAtual = abas.getOrElse(paginas.currentPage) { Aba.HOME }
    val titulo = when (rota) {
        ROTA_ABAS -> abaAtual.rotulo
        "notificacoes" -> "Notificações"
        "aporte" -> "Aporte"
        "rateio" -> "Rateio"
        "extrato", "extrato/{mes}" -> "Extrato"
        "comprovante/{id}" -> "Comprovante"
        // A página do jogo, com o volante ou a aposta aberta.
        "modalidade/{nome}", "nova/{nome}", "aposta/{id}", "apuracao/{id}" -> "Jogos"
        "saldo" -> "Saldo"
        "integridade" -> "Verificar integridade"
        else -> ""
    }

    // Os parâmetros do Histórico da Home recomeçam quando se vai para outra aba; ao voltar de uma
    // tela aberta a partir da Home (uma aposta, um comprovante, as notificações), ficam como estavam.
    var visitaDaHome by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(paginas.currentPage) { if (abaAtual != Aba.HOME) visitaDaHome++ }

    // Na aba Comprovantes, o número do que espera: comprovante, Pix ou a conferência.
    val paraAnexar = if (estado.individual) 0 else Operacoes.apostasSemComprovante(estado).size + Operacoes.rateiosSemPix(estado).size +
        Operacoes.apostasParaConferir(estado).size + Operacoes.rateiosParaConferir(estado).size

    Scaffold(
        topBar = {
            Column {
                BarraTitulo(titulo, aoVoltar = if (ehAba) null else ({ nav.popBackStack() }))
                if (teste) FaixaModoTeste(aoSair = { sairDoTeste = true })
            }
        },
        bottomBar = {
            if (ehAba) {
                // Azul-noite do ícone, com a aba escolhida em dourado.
                NavigationBar(containerColor = Barra.fundo(), contentColor = Barra.outras()) {
                    abas.forEachIndexed { i, aba ->
                        val escolhida = paginas.currentPage == i
                        NavigationBarItem(
                            selected = escolhida,
                            onClick = { escopo.launch { paginas.scrollToPage(i) } },
                            icon = {
                                BadgedBox(badge = {
                                    if (aba == Aba.COMPROVANTES && paraAnexar > 0) {
                                        Badge(containerColor = Barra.contador, contentColor = Color.White) { Text(paraAnexar.toString()) }
                                    }
                                }) { Icon(if (escolhida) aba.iconeEscolhido else aba.icone, contentDescription = null) }
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
        // Troca de tela instantânea, sem o esmaecer (fade) padrão da navegação.
        NavHost(
            nav,
            startDestination = ROTA_ABAS,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            // As abas: Home, Jogos, Comprovantes e Ajustes, uma do lado da outra (deslizar o dedo troca de aba).
            composable(ROTA_ABAS) {
                // O voltar do celular numa aba que não é a Home volta para a Home.
                BackHandler(enabled = paginas.currentPage != 0) { escopo.launch { paginas.scrollToPage(0) } }
                HorizontalPager(state = paginas, key = { abas[it].rota }, modifier = Modifier.fillMaxSize()) { i ->
                    when (abas[i]) {
                        Aba.HOME -> HomeTela(
                            estado, visitaDaHome, pendencias = pendencias.size,
                            abrir = ::abrir, apostar = { irParaAba(Aba.JOGOS) },
                        )
                        Aba.JOGOS -> JogosTela(estado, abrirModalidade = { abrir("modalidade/${it.name}") })
                        Aba.COMPROVANTES -> ComprovantesTela(estado, abrir = ::abrir)
                        Aba.AJUSTES -> AjustesTela(estado, atualizacao, abrir = ::abrir)
                    }
                }
            }
            composable("notificacoes") { NotificacoesTela(pendencias, abrir = ::abrir) }
            composable("saldo") { SaldoTela(estado) }
            composable("integridade") { IntegridadeTela(estado) }
            composable("aporte") { AporteTela(estado) }
            composable("rateio") { RateioTela(estado, abrir = ::abrir) }
            composable("extrato") { ExtratoTela(estado) }
            composable("extrato/{mes}") { e ->
                val mes = runCatching { YearMonth.parse(e.arguments?.getString("mes").orEmpty()) }.getOrNull()
                ExtratoTela(estado, mesInicial = mes)
            }
            composable("comprovante/{id}") { e -> ComprovanteTela(estado, e.arguments?.getString("id").orEmpty()) }
            // A página do jogo: o volante (a aposta em andamento da modalidade ou uma nova).
            composable("modalidade/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                if (m != null) ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir)
            }
            // Endereço antigo da aposta nova: a página do jogo.
            composable("nova/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                if (m != null) ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir)
            }
            // Uma aposta (do Histórico ou de um aviso): a página do jogo dela, com a aposta no lugar do volante.
            composable("aposta/{id}") { e -> PaginaDaAposta(estado, e.arguments?.getString("id").orEmpty(), ::abrir) { fechar(nav, e) } }
            // A apuração fica na aposta (avisos antigos ainda podem abrir este endereço).
            composable("apuracao/{id}") { e -> PaginaDaAposta(estado, e.arguments?.getString("id").orEmpty(), ::abrir) { fechar(nav, e) } }
        }
    }

    if (sairDoTeste) DialogoSairDoTeste(aoFechar = { sairDoTeste = false })
}

/**
 * As apostas que ainda não valem (em andamento, ou registradas esperando o comprovante depois do
 * sorteio do concurso delas) passam para o concurso que a Caixa está recebendo agora.
 */
private suspend fun atualizarConcursos(ctx: Context, itens: Map<Modalidade, Indices.Item>) {
    val proximos = itens.mapNotNull { (m, item) ->
        val numero = item.proximoNumero ?: return@mapNotNull null
        val u = item.ultimo
        m to Operacoes.ProximoConcurso(numero, u?.proximaData?.takeIf { u.proximoNumero == numero }, u?.premioEstimadoProximo?.takeIf { u.proximoNumero == numero })
    }.toMap()
    if (proximos.isEmpty()) return
    val repo = ctx.bolao.repo
    var mudadas: List<Pair<Aposta, Aposta>> = emptyList()
    runCatching {
        repo.atualizar { e ->
            val (novo, lista) = Operacoes.atualizarConcursos(e, proximos)
            mudadas = lista
            novo
        }
    }
    mudadas.firstOrNull()?.let { (antes, depois) ->
        val outras = if (mudadas.size > 1) " (e mais ${mudadas.size - 1})" else ""
        repo.avisar("O concurso ${antes.concurso} da ${antes.modalidade.nome} já foi sorteado: a aposta passou para o ${depois.concurso}$outras.")
    }
}

/** Fecha a tela [entrada], só se ela ainda for a de cima (nunca fecha outra por engano). */
private fun fechar(nav: NavHostController, entrada: NavBackStackEntry) {
    if (nav.currentBackStackEntry?.id == entrada.id) nav.popBackStack()
}

/** A página do jogo de uma aposta (registrada ou em andamento). */
@Composable
private fun PaginaDaAposta(estado: Estado, id: String, abrir: (String) -> Unit, aoSair: () -> Unit) {
    val a = estado.apostas.firstOrNull { it.id == id }
    if (a == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não existe mais.") }
    } else {
        ModalidadeTela(estado, a.modalidade, aoSair = aoSair, apostaId = id, abrir = abrir)
    }
}

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import io.github.marcosfaria94.bolao.notificacoes.Despertador
import io.github.marcosfaria94.bolao.notificacoes.SegundoPlano
import io.github.marcosfaria94.bolao.ui.comum.BarraDaTela
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import io.github.marcosfaria94.bolao.ui.comum.ArrasteParaAbrir
import io.github.marcosfaria94.bolao.ui.comum.PainelLateral
import io.github.marcosfaria94.bolao.ui.comum.arrastarPainel
import io.github.marcosfaria94.bolao.ui.comum.FiltroHome
import io.github.marcosfaria94.bolao.ui.comum.LocalBarraDaTela
import io.github.marcosfaria94.bolao.ui.home.FiltroTela
import io.github.marcosfaria94.bolao.ui.comum.IndicadorDoPuxao
import io.github.marcosfaria94.bolao.ui.comum.acompanharPuxao
import io.github.marcosfaria94.bolao.ui.comum.deslizarParaFechar
import io.github.marcosfaria94.bolao.ui.comum.rememberPuxaoAlemDaBorda
import io.github.marcosfaria94.bolao.ui.tema.AmareloSino
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.time.LocalDate
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import io.github.marcosfaria94.bolao.ui.ajustes.AjustesSecaoTela
import io.github.marcosfaria94.bolao.ui.ajustes.ROTA_SECAO_AJUSTES
import io.github.marcosfaria94.bolao.ui.ajustes.SecaoAjuste
import io.github.marcosfaria94.bolao.notificacoes.Toque
import androidx.compose.material3.AlertDialog
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

/** As páginas que se trocam deslizando o dedo (as abas da barra de baixo): as mesmas nos dois modos. */
private enum class Aba(val rota: String, val rotulo: String, val icone: ImageVector, val iconeEscolhido: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    JOGOS("jogos", "Jogos", Icones.Trevo, Icones.TrevoCheio),
    AJUSTES("ajustes", "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** Telas que não limpam o filtro do Histórico: o próprio "Filtrar" e os Comprovantes (as Notificações ficam nas abas). */
private val MANTEM_O_FILTRO = setOf(ROTA_FILTRO, ROTA_COMPROVANTES)

/** Telas sem a seta de voltar na barra de cima (fecham deslizando o dedo, ou pelo voltar do celular). */
private val SEM_SETA = setOf("aporte", "rateio", "extrato", "extrato/{mes}", ROTA_FILTRO, "saldo", ROTA_COMPROVANTES)

/** A tela das abas (as páginas que se trocam deslizando o dedo para os lados). */
private const val ROTA_ABAS = "abas"
private const val ROTA_FILTRO = "filtro"

/** As Notificações: o painel à esquerda das abas (não é uma tela da navegação; abrir() cuida). */
private const val ROTA_NOTIFICACOES = "notificacoes"

/** Comprovantes: até a 0.12 era uma aba; agora abre pelo botão da barra de cima (ou puxando as Notificações). */
private const val ROTA_COMPROVANTES = Pendencias.ABA_COMPROVANTES

/** As Notificações e os endereços antigos dos jogos em andamento (hoje a seção "Meus jogos" delas). */
private val PARA_AS_NOTIFICACOES = setOf(ROTA_NOTIFICACOES, "em-andamento", "nao-sorteados")

/** Endereços antigos (avisos de versões anteriores): "financeiro" e "apostas" eram abas; "apostar", a Nova aposta. */
private val PARA_A_HOME = setOf("painel", "financeiro", "apostas")

@Composable
fun AppRaiz() {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val estado by repo.estado.collectAsStateWithLifecycle()
    val aviso by repo.aviso.collectAsStateWithLifecycle()
    val teste by repo.modoTeste.collectAsStateWithLifecycle()
    val recomeco by repo.recomecos.collectAsStateWithLifecycle()
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
            !e.cadastroConcluido -> CadastroTela(recomeco)
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
    // Contadores da barra de cima das abas: as pendências de agora (refeitas a cada minuto, ex.: virada do dia).
    val agora by produceState(LocalDateTime.now()) {
        while (true) {
            delay(60_000)
            value = LocalDateTime.now()
        }
    }
    // Pendências do bolão + avisos dos índices (preço diferente, regras da Caixa que mudaram).
    val avisosIndices by ctx.bolao.indices.avisos.collectAsStateWithLifecycle()
    val pendencias = Pendencias.comAvisosDosIndices(Pendencias.de(estado, agora), avisosIndices)
    // O sino fica com os avisos; a falta de comprovante, com o botão Comprovantes.
    val doSino = Pendencias.doSino(pendencias)

    // Ao abrir: confere os sorteios e os índices na hora (sem esperar a próxima hora).
    LaunchedEffect(Unit) {
        Vigia.rodarAgora(ctx)
        ctx.bolao.indices.atualizar()
    }
    // O despertador da apuração acompanha as apostas (registrar, apurar e descartar mudam a hora de acordar).
    LaunchedEffect(estado.apostas) { runCatching { Despertador.agendar(ctx, estado) } }
    // O concurso das apostas que ainda não valem (em andamento ou esperando o comprovante) acompanha a Caixa.
    val itens by ctx.bolao.indices.itens.collectAsStateWithLifecycle()
    LaunchedEffect(itens) { atualizarConcursos(ctx, itens) }

    val abas = Aba.entries
    val paginaDaHome = abas.indexOf(Aba.HOME)
    val paginas = rememberPagerState(initialPage = paginaDaHome) { abas.size }
    val barra = remember { BarraDaTela() }
    // As Notificações: o painel à esquerda das abas, que entra e sai acompanhando o dedo, como as abas.
    val painel = remember { PainelLateral() }
    // Tocar na aba em que já se está: a lista dela volta ao começo (um contador por aba).
    val toques = remember { mutableStateListOf(0, 0, 0) }

    /** Vai para a aba [aba], fechando o que estiver aberto por cima (e as Notificações). */
    fun irParaAba(aba: Aba) {
        val i = abas.indexOf(aba).takeIf { it >= 0 } ?: paginaDaHome
        if (nav.currentBackStackEntry?.destination?.route != ROTA_ABAS) nav.popBackStack(ROTA_ABAS, inclusive = false)
        painel.fecharJa()
        escopo.launch { paginas.scrollToPage(i) }
    }

    /** Abre as Notificações por cima da aba de agora (fechando o que estiver aberto por cima das abas). */
    fun abrirNotificacoes() {
        if (nav.currentBackStackEntry?.destination?.route != ROTA_ABAS) nav.popBackStack(ROTA_ABAS, inclusive = false)
        painel.abrirJa()
    }

    /** Os Comprovantes, a partir das Notificações: elas fecham, e os Comprovantes voltam para a aba de antes delas. */
    fun abrirComprovantesDasNotificacoes() {
        painel.fecharJa()
        nav.navigate(ROTA_COMPROVANTES) { launchSingleTop = true }
    }

    // Uma aba (ex.: a pendência "Fazer a aposta de hoje" abre os Jogos) troca de aba; o resto abre por cima.
    fun abrir(r: String) {
        val aba = Aba.entries.firstOrNull { it.rota == r }
        when {
            aba != null -> irParaAba(aba)
            r in PARA_A_HOME -> irParaAba(Aba.HOME)
            r == "apostar" -> irParaAba(Aba.JOGOS)
            r in PARA_AS_NOTIFICACOES -> abrirNotificacoes()
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

    // Uma vez, logo depois do cadastro: a permissão de notificação (Android 13+) e, em seguida, o
    // pedido para a economia de bateria não pausar o app (a apuração e o aviso do prêmio com o app fechado).
    val prefs = remember { ctx.getSharedPreferences("app", Context.MODE_PRIVATE) }
    fun pedirBateria() {
        if (!prefs.getBoolean("pediu_bateria", false) && !SegundoPlano.bateriaLivre(ctx)) {
            prefs.edit().putBoolean("pediu_bateria", true).apply()
            SegundoPlano.pedirBateriaLivre(ctx)
        }
    }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) Vigia.rodarAgora(ctx)
        pedirBateria()
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notificador.permitido(ctx) && !prefs.getBoolean("pediu_notificacao", false)) {
            prefs.edit().putBoolean("pediu_notificacao", true).apply()
            pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            pedirBateria()
        }
    }

    val entrada by nav.currentBackStackEntryAsState()
    val rota = entrada?.destination?.route ?: ROTA_ABAS
    val ehAba = rota == ROTA_ABAS
    val abaAtual = abas.getOrElse(paginas.currentPage) { Aba.HOME }
    // A ação que a tela de cima pôs na barra (o "Detalhes →" da página do jogo).
    val acao = barra.acao?.takeIf { !ehAba && it.dono === entrada }
    // As Notificações abertas (mais da metade na tela, como a página do pager): a barra é delas.
    val nasNotificacoes = ehAba && painel.aberto
    val titulo = acao?.titulo ?: when (rota) {
        ROTA_ABAS -> if (nasNotificacoes) "Notificações" else abaAtual.rotulo
        ROTA_FILTRO -> "Filtrar"
        ROTA_COMPROVANTES -> "Comprovantes"
        ROTA_SECAO_AJUSTES -> SecaoAjuste.de(entrada?.arguments?.getString("secao"))?.titulo(estado.individual) ?: "Ajustes"
        "aporte" -> "Aporte"
        "rateio" -> "Rateio"
        "extrato", "extrato/{mes}" -> "Extrato"
        "comprovante/{id}" -> "Comprovante"
        // A página do jogo, com o volante ou a aposta aberta.
        "modalidade/{nome}", "nova/{nome}", "detalhes/{nome}", "aposta/{id}", "apuracao/{id}" -> "Jogos"
        "saldo" -> "Saldo"
        "integridade" -> "Verificar integridade"
        else -> ""
    }

    // A rolagem da Home recomeça quando se vai para outra aba.
    var visitaDaHome by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(paginas.currentPage) { if (abaAtual != Aba.HOME) visitaDaHome++ }

    // Toda vez que se entra nos Jogos (pela barra, deslizando ou voltando de um jogo): a Caixa de novo.
    val abaAssentada = abas.getOrNull(paginas.settledPage)
    LaunchedEffect(ehAba, abaAssentada) { if (ehAba && abaAssentada == Aba.JOGOS) ctx.bolao.indices.atualizarAoEntrar() }

    // O filtro do Histórico (tela "Filtrar"): começa no padrão (todos os tipos no mês atual) e volta a
    // ele sozinho ao trocar de tela; só o "Filtrar" e as telas que se abrem puxando para baixo o mantêm.
    var filtro by rememberSaveable(stateSaver = FiltroHome.Saver) { mutableStateOf(FiltroHome.padrao(LocalDate.now())) }
    val naHome = ehAba && abas.getOrNull(paginas.settledPage) == Aba.HOME
    LaunchedEffect(naHome, rota) {
        if (!naHome && rota !in MANTEM_O_FILTRO) filtro = FiltroHome.padrao(LocalDate.now())
    }

    // Nas Notificações, só o número do que espera comprovante (da Caixa ou dos Pix) e do que espera a conferência.
    val semComprovante = if (estado.individual) 0 else Operacoes.apostasSemComprovante(estado).size + Operacoes.rateiosSemPix(estado).size
    val aConferir = if (estado.individual) 0 else Operacoes.apostasParaConferir(estado).size + Operacoes.rateiosParaConferir(estado).size
    // O número do sino: as linhas das Pendências (os comprovantes contam uma linha cada: os que faltam e os a conferir).
    val noSino = doSino.size + listOf(semComprovante, aConferir).count { it > 0 }

    CompositionLocalProvider(LocalBarraDaTela provides barra) {
    Scaffold(
        topBar = {
            Column {
                BarraTitulo(
                    titulo,
                    aoVoltar = when {
                        ehAba || rota in SEM_SETA -> null
                        acao?.aoVoltar != null -> acao.aoVoltar
                        else -> {
                            { nav.popBackStack(); Unit }
                        }
                    },
                    acoes = {
                        // Nas abas: o sino das notificações, à direita do título (nas Notificações, nada).
                        if (ehAba && !nasNotificacoes) Sino(noSino) { abrirNotificacoes() }
                        // O "Detalhes →" da página do jogo (nos Detalhes, o "Jogos →"), na mesma cor e letra do título.
                        acao?.botao?.let { rotulo ->
                            val cor = MaterialTheme.colorScheme.onSurface
                            TextButton(onClick = acao.aoTocarBotao, colors = ButtonDefaults.textButtonColors(contentColor = cor)) {
                                Text(rotulo, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = cor, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = cor, modifier = Modifier.size(28.dp))
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
                    abas.forEachIndexed { i, aba ->
                        val escolhida = abaAtual == aba
                        NavigationBarItem(
                            selected = escolhida,
                            onClick = {
                                when {
                                    // Com as Notificações abertas: fecham, e vai para a aba tocada.
                                    painel.visivel -> {
                                        painel.fecharJa()
                                        escopo.launch { paginas.scrollToPage(i) }
                                    }
                                    // Já nesta aba: a lista volta ao começo.
                                    abaAtual == aba -> toques[i]++
                                    else -> escopo.launch { paginas.scrollToPage(i) }
                                }
                            },
                            // Só 3 abas: os ícones maiores, para ficarem mais visíveis.
                            icon = { Icon(if (escolhida) aba.iconeEscolhido else aba.icone, contentDescription = null, modifier = Modifier.size(ICONE_DA_ABA)) },
                            label = {
                                Text(
                                    aba.rotulo,
                                    fontSize = 12.5.sp,
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
            // As abas: Home, Jogos e Ajustes, uma do lado da outra (deslizar o dedo troca de aba).
            composable(ROTA_ABAS) {
                // O voltar do celular numa página que não é a Home volta para a Home; com as Notificações abertas, fecha elas.
                BackHandler(enabled = paginas.currentPage != paginaDaHome && !painel.visivel) { escopo.launch { paginas.scrollToPage(paginaDaHome) } }
                BackHandler(enabled = painel.visivel) { painel.fecharJa() }
                // Puxar qualquer aba para baixo (do alto da lista) abre as Notificações. Para os lados,
                // além das bordas: para a direita na Home, as Notificações entram como uma página (com o
                // dedo, como as abas); para a esquerda nos Ajustes (a última página), de volta à Home.
                val puxaoParaBaixo = rememberPuxaoAlemDaBorda(vertical = true, noInicio = { abrirNotificacoes() })
                val puxaoDoFim = rememberPuxaoAlemDaBorda(
                    vertical = false,
                    noFim = { if (paginas.currentPage == abas.lastIndex) escopo.launch { paginas.scrollToPage(paginaDaHome) } },
                )
                val arraste = remember(painel) { ArrasteParaAbrir(painel) }
                SideEffect { arraste.ativo = paginas.currentPage == paginaDaHome }
                Box(Modifier.fillMaxSize().onSizeChanged { painel.largura = it.width.toFloat().coerceAtLeast(1f) }) {
                    Box(Modifier.fillMaxSize().nestedScroll(puxaoParaBaixo).nestedScroll(puxaoDoFim).nestedScroll(arraste)) {
                        HorizontalPager(
                            state = paginas,
                            key = { abas[it].rota },
                            modifier = Modifier
                                .fillMaxSize()
                                .acompanharPuxao(puxaoParaBaixo)
                                .acompanharPuxao(puxaoDoFim)
                                .graphicsLayer { translationX = painel.deslocamentoDaAba },
                        ) { i ->
                            when (abas[i]) {
                                Aba.HOME -> HomeTela(
                                    estado, visitaDaHome, filtro = filtro, abrir = ::abrir, apostar = { irParaAba(Aba.JOGOS) },
                                    paraOTopo = toques[i],
                                )
                                Aba.JOGOS -> JogosTela(
                                    estado,
                                    abrirModalidade = { abrir("modalidade/${it.name}") },
                                    abrirDetalhes = { abrir("detalhes/${it.name}") },
                                    paraOTopo = toques[i],
                                )
                                Aba.AJUSTES -> AjustesTela(estado, atualizacao, abrir = ::abrir, paraOTopo = toques[i])
                            }
                        }
                        IndicadorDoPuxao(
                            puxaoParaBaixo, puxando = "Notificações", pronto = "Solte para abrir", icone = Icons.Filled.Notifications,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                        )
                    }
                    // As Notificações: as pendências e os meus jogos, à esquerda da aba. Fecham deslizando o
                    // dedo para os lados; puxar para baixo de novo troca pelos Comprovantes.
                    if (painel.visivel) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = painel.deslocamentoDoPainel
                                    alpha = 1f - (painel.saida / painel.largura).coerceIn(0f, 1f) * 0.6f
                                }
                                .background(MaterialTheme.colorScheme.background),
                        ) {
                            NotificacoesTela(
                                doSino,
                                estado,
                                abrir = ::abrir,
                                abrirComprovantes = if (estado.individual) null else ({ abrirComprovantesDasNotificacoes() }),
                                modifier = Modifier.arrastarPainel(painel),
                                semComprovante = semComprovante,
                                aConferir = aConferir,
                                ativa = painel.aberto,
                            )
                        }
                    }
                }
            }
            // Telas que fecham deslizando o dedo para qualquer lado.
            composable(ROTA_FILTRO) { e -> FiltroTela(estado, filtro, mudar = { filtro = it }, aoFechar = { fechar(nav, e) }) }
            // Comprovantes: fecham deslizando o dedo para os lados ou puxando para baixo (e voltam para a
            // tela de antes; abertos pelas Notificações, a de antes delas).
            composable(ROTA_COMPROVANTES) { e ->
                // A leitura dos comprovantes continua mesmo fechando a tela: o modelo fica com as abas.
                val dono = remember(e) { nav.getBackStackEntry(ROTA_ABAS) }
                val puxao = rememberPuxaoAlemDaBorda(vertical = true, noInicio = { fechar(nav, e) })
                Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }.nestedScroll(puxao)) {
                    Box(Modifier.fillMaxSize().acompanharPuxao(puxao)) {
                        ComprovantesTela(estado, abrir = ::abrir, vm = viewModel(dono))
                    }
                    IndicadorDoPuxao(
                        puxao, puxando = "Voltar", pronto = "Solte para voltar", icone = Icons.Outlined.KeyboardArrowDown,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                    )
                }
            }
            // O Saldo atual (tocar no quadro do saldo): deslizar o dedo para qualquer lado volta para a Home.
            composable("saldo") { Box(Modifier.fillMaxSize().deslizarParaFechar { irParaAba(Aba.HOME) }) { SaldoTela(estado) } }
            composable("integridade") { e -> Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { IntegridadeTela(estado) } }
            // Cada item do menu dos Ajustes, numa tela própria.
            composable(ROTA_SECAO_AJUSTES) { e ->
                val secao = SecaoAjuste.de(e.arguments?.getString("secao"))
                Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) {
                    if (secao != null) AjustesSecaoTela(estado, secao, atualizacao, abrir = ::abrir)
                }
            }
            composable("aporte") { e -> Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { AporteTela(estado) } }
            composable("rateio") { e -> Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { RateioTela(estado, abrir = ::abrir) } }
            composable("extrato") { e -> Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { ExtratoTela(estado) } }
            composable("extrato/{mes}") { e ->
                val mes = runCatching { YearMonth.parse(e.arguments?.getString("mes").orEmpty()) }.getOrNull()
                Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { ExtratoTela(estado, mesInicial = mes) }
            }
            composable("comprovante/{id}") { e -> ComprovanteTela(estado, e.arguments?.getString("id").orEmpty()) }
            // A página do jogo: o volante (a aposta em andamento da modalidade ou uma nova).
            composable("modalidade/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                if (m != null) ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir, irParaJogos = { irParaAba(Aba.JOGOS) })
            }
            // Segurar a modalidade em Jogos: a página do jogo já nos Detalhes.
            composable("detalhes/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                if (m != null) {
                    ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir, detalhes = true, irParaJogos = { irParaAba(Aba.JOGOS) })
                }
            }
            // Endereço antigo da aposta nova: a página do jogo.
            composable("nova/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                if (m != null) ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir, irParaJogos = { irParaAba(Aba.JOGOS) })
            }
            // Uma aposta (do Histórico ou de um aviso): a página do jogo dela, com a aposta no lugar do volante.
            composable("aposta/{id}") { e ->
                PaginaDaAposta(estado, e.arguments?.getString("id").orEmpty(), ::abrir, irParaJogos = { irParaAba(Aba.JOGOS) }) { fechar(nav, e) }
            }
            // A apuração fica na aposta (avisos antigos ainda podem abrir este endereço).
            composable("apuracao/{id}") { e ->
                PaginaDaAposta(estado, e.arguments?.getString("id").orEmpty(), ::abrir, irParaJogos = { irParaAba(Aba.JOGOS) }) { fechar(nav, e) }
            }
        }
    }
    }

    if (sairDoTeste) DialogoSairDoTeste(aoFechar = { sairDoTeste = false })

    // O prêmio principal tocando com o app aberto: o "Parar o toque" também aqui.
    val tocando by Toque.tocando.collectAsStateWithLifecycle()
    if (tocando == Toque.Origem.PREMIO) {
        AlertDialog(
            onDismissRequest = { Toque.parar() },
            icon = { Icon(Icons.Filled.Notifications, contentDescription = null, tint = AmareloSino) },
            title = { Text("Prêmio principal!") },
            text = { Text("Uma aposta acertou o prêmio principal. Veja em Notificações ou no Histórico.") },
            confirmButton = { TextButton(onClick = { Toque.parar() }) { Text("Parar o toque") } },
        )
    }
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
private fun PaginaDaAposta(estado: Estado, id: String, abrir: (String) -> Unit, irParaJogos: () -> Unit, aoSair: () -> Unit) {
    val a = estado.apostas.firstOrNull { it.id == id }
    if (a == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não existe mais.") }
    } else {
        ModalidadeTela(estado, a.modalidade, aoSair = aoSair, apostaId = id, abrir = abrir, irParaJogos = irParaJogos)
    }
}

/** Tamanho dos ícones das 3 abas da barra de baixo (o padrão é 24dp). */
private val ICONE_DA_ABA = 31.dp

/** O sino das notificações (na barra de cima das abas): todo amarelo, com o contador vermelho de pendências. */
@Composable
private fun Sino(pendencias: Int, aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar, modifier = Modifier.size(48.dp)) {
        BadgedBox(badge = {
            if (pendencias > 0) {
                Badge(containerColor = Barra.contador, contentColor = Color.White) {
                    Text(if (pendencias > 99) "99+" else pendencias.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notificações", tint = AmareloSino, modifier = Modifier.size(30.dp))
        }
    }
}

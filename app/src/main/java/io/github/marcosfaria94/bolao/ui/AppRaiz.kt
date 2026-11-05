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
import androidx.compose.runtime.mutableStateListOf
import io.github.marcosfaria94.bolao.ui.comum.FiltroHome
import io.github.marcosfaria94.bolao.ui.comum.LocalBarraDaTela
import io.github.marcosfaria94.bolao.ui.home.FiltroTela
import io.github.marcosfaria94.bolao.ui.comum.IndicadorDoPuxao
import io.github.marcosfaria94.bolao.ui.comum.acompanharPuxao
import io.github.marcosfaria94.bolao.ui.comum.deslizarParaFechar
import io.github.marcosfaria94.bolao.ui.comum.rememberPuxaoAlemDaBorda
import io.github.marcosfaria94.bolao.ui.tema.AmareloSino
import io.github.marcosfaria94.bolao.ui.comum.PuxarParaVoltar
import io.github.marcosfaria94.bolao.ui.comum.soltarDasPaginas
import io.github.marcosfaria94.bolao.ui.painel.PainelDeJogosTela
import io.github.marcosfaria94.bolao.ui.painel.ROTA_PAINEL
import io.github.marcosfaria94.bolao.ui.painel.IconeDoPainel
import io.github.marcosfaria94.bolao.ui.painel.quantasPendencias
import io.github.marcosfaria94.bolao.ui.painel.resumoDasPendencias
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
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

/**
 * As abas giram em círculo: da Home, deslizando para a direita, os Ajustes; dos Ajustes, para a
 * esquerda, a Home, com a mesma animação das outras trocas (até a 0.15 era um puxão além da borda e a
 * troca acontecia de uma vez). O pager tem muitas páginas e a aba de cada uma é o resto da divisão.
 */
private const val VOLTAS_DAS_ABAS = 1000

private fun abaDa(pagina: Int): Aba = Aba.entries[pagina.mod(Aba.entries.size)]

/** A página da aba [aba] mais perto da página [atual] (sem dar a volta inteira). */
private fun paginaDa(aba: Aba, atual: Int): Int {
    val n = Aba.entries.size
    val d = (aba.ordinal - atual.mod(n)).mod(n)
    return atual + if (d > n / 2) d - n else d
}

/** Telas que não limpam o filtro do Histórico: o próprio "Filtrar", os Comprovantes e o Painel de Jogos. */
private val MANTEM_O_FILTRO = setOf(ROTA_FILTRO, ROTA_COMPROVANTES, ROTA_PAINEL)

/** Telas sem a seta de voltar na barra de cima (fecham deslizando o dedo, puxando para baixo ou pelo voltar do celular). */
private val SEM_SETA = setOf(
    "aporte", "rateio", "extrato", "extrato/{mes}", ROTA_FILTRO, "saldo", ROTA_COMPROVANTES, ROTA_PAINEL, ROTA_SECAO_AJUSTES, "integridade",
)

/** A página do jogo (o volante, a aposta aberta ou os Detalhes): o título "← Voltar" também volta. */
private val ROTAS_DO_JOGO = setOf("modalidade/{nome}", "nova/{nome}", "detalhes/{nome}", "aposta/{id}", "apuracao/{id}")

/** A tela das abas (as páginas que se trocam deslizando o dedo para os lados). */
private const val ROTA_ABAS = "abas"
private const val ROTA_FILTRO = "filtro"

/** Comprovantes: até a 0.12 era uma aba; agora abre pelas pendências do resumo do Painel de Jogos. */
private const val ROTA_COMPROVANTES = Pendencias.ABA_COMPROVANTES

/**
 * O Painel de Jogos e os endereços antigos: os jogos em andamento e as Notificações (saíram na 0.17; as
 * pendências ficaram no resumo do Painel de Jogos e os avisos antigos do celular ainda abrem "notificacoes").
 */
private val PARA_O_PAINEL = setOf(ROTA_PAINEL, "em-andamento", "nao-sorteados", "notificacoes")

/** Itens dos Ajustes que até a 0.15 tinham tela própria (hoje ficam direto na lista). */
private val PARA_OS_AJUSTES = setOf("ajustes/teste", "ajustes/atualizacao", "ajustes/creditos")

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
    // A aba de agora: ao ligar ou desligar o modo de teste (no próprio Ajustes) a navegação recomeça, mas na mesma aba.
    var abaGuardada by rememberSaveable { mutableStateOf(Aba.HOME.name) }

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
            !e.cadastroConcluido -> {
                LaunchedEffect(Unit) { abaGuardada = Aba.HOME.name }
                CadastroTela(recomeco)
            }
            // Ao entrar ou sair do modo de teste (ou trocar de modo), a navegação recomeça (na aba em que estava).
            else -> key(teste, e.modo) {
                val aba = Aba.entries.firstOrNull { it.name == abaGuardada } ?: Aba.HOME
                Principal(e, atualizacao, teste, aba) { abaGuardada = it.name }
            }
        }
        DialogosAtualizacao(atualizacao)
    }
}

@Composable
private fun Principal(estado: Estado, atualizacao: AtualizacaoViewModel, teste: Boolean, abaInicial: Aba, aoMudarDeAba: (Aba) -> Unit) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var sairDoTeste by remember { mutableStateOf(false) }
    // O contador da barra de cima das abas: as pendências de agora (refeitas a cada minuto, ex.: virada do dia).
    val agora by produceState(LocalDateTime.now()) {
        while (true) {
            delay(60_000)
            value = LocalDateTime.now()
        }
    }
    // Pendências do bolão + avisos dos índices (preço diferente, regras da Caixa que mudaram).
    val avisosIndices by ctx.bolao.indices.avisos.collectAsStateWithLifecycle()
    val pendencias = Pendencias.comAvisosDosIndices(Pendencias.de(estado, agora), avisosIndices)
    // O resumo do Painel de Jogos: cada tipo de pendência com a quantidade.
    val resumo = resumoDasPendencias(estado, pendencias)

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
    val paginas = rememberPagerState(initialPage = abas.size * VOLTAS_DAS_ABAS + abaInicial.ordinal) { abas.size * VOLTAS_DAS_ABAS * 2 }
    /** Vai para a aba [aba] na hora (sem animação), pelo caminho mais curto. */
    suspend fun mostrar(aba: Aba) = paginas.scrollToPage(paginaDa(aba, paginas.currentPage))
    val barra = remember { BarraDaTela() }
    // Tocar na aba em que já se está: a lista dela volta ao começo (um contador por aba).
    val toques = remember { mutableStateListOf(0, 0, 0) }

    /** Vai para a aba [aba], fechando o que estiver aberto por cima. */
    fun irParaAba(aba: Aba) {
        if (nav.currentBackStackEntry?.destination?.route != ROTA_ABAS) nav.popBackStack(ROTA_ABAS, inclusive = false)
        escopo.launch { mostrar(aba) }
    }

    /** O Painel de Jogos (o ícone da barra de cima ou puxando qualquer aba para baixo): volta para a tela de antes. */
    fun abrirPainel() {
        nav.navigate(ROTA_PAINEL) { launchSingleTop = true }
    }

    // Uma aba (ex.: a pendência "Fazer a aposta de hoje" abre os Jogos) troca de aba; o resto abre por cima.
    fun abrir(r: String) {
        val aba = Aba.entries.firstOrNull { it.rota == r }
        when {
            aba != null -> irParaAba(aba)
            r in PARA_A_HOME -> irParaAba(Aba.HOME)
            r == "apostar" -> irParaAba(Aba.JOGOS)
            r in PARA_O_PAINEL -> abrirPainel()
            r in PARA_OS_AJUSTES -> irParaAba(Aba.AJUSTES)
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
    val abaAtual = abaDa(paginas.currentPage)
    // A ação que a tela de cima pôs na barra (o "Detalhes →" da página do jogo).
    val acao = barra.acao?.takeIf { !ehAba && it.dono === entrada }
    /** O título da tela [r] (na barra de cima). */
    fun tituloDa(r: String, e: NavBackStackEntry?): String = when (r) {
        ROTA_ABAS -> abaAtual.rotulo
        ROTA_FILTRO -> "Filtrar"
        ROTA_COMPROVANTES -> "Comprovantes"
        ROTA_PAINEL -> "Painel de Jogos"
        ROTA_SECAO_AJUSTES -> SecaoAjuste.de(e?.arguments?.getString("secao"))?.titulo(estado.individual) ?: "Ajustes"
        "aporte" -> "Aporte"
        "rateio" -> "Rateio"
        "extrato", "extrato/{mes}" -> "Extrato"
        "comprovante/{id}" -> "Comprovante"
        // A página do jogo, com o volante ou a aposta aberta: "← Voltar" (volta para onde ela foi aberta).
        in ROTAS_DO_JOGO -> "Voltar"
        "saldo" -> "Saldo"
        "integridade" -> "Verificar integridade"
        else -> ""
    }

    /** O nome da tela [e] para o aviso do puxão ("Home", "Jogos", "Quina"...). */
    fun nomeDaTela(e: NavBackStackEntry?): String {
        val r = e?.destination?.route ?: return abaAtual.rotulo
        if (r == ROTA_ABAS) return abaAtual.rotulo
        if (r in ROTAS_DO_JOGO) {
            val a = e.arguments
            val m = runCatching { Modalidade.valueOf(a?.getString("nome").orEmpty()) }.getOrNull()
                ?: estado.apostas.firstOrNull { it.id == a?.getString("id") }?.modalidade
            return m?.nome ?: "Voltar"
        }
        return tituloDa(r, e).ifEmpty { "Voltar" }
    }

    val titulo = acao?.titulo ?: tituloDa(rota, entrada)

    // A rolagem da Home recomeça quando se vai para outra aba.
    var visitaDaHome by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(paginas.currentPage) { if (abaAtual != Aba.HOME) visitaDaHome++ }

    // Toda vez que se entra nos Jogos (pela barra, deslizando ou voltando de um jogo): a Caixa de novo.
    val abaAssentada = abaDa(paginas.settledPage)
    LaunchedEffect(ehAba, abaAssentada) { if (ehAba && abaAssentada == Aba.JOGOS) ctx.bolao.indices.atualizarAoEntrar() }
    LaunchedEffect(abaAssentada) { aoMudarDeAba(abaAssentada) }

    // O filtro do Histórico (tela "Filtrar"): começa no padrão (todos os tipos no mês atual) e volta a
    // ele sozinho ao trocar de tela; só o "Filtrar" e as telas que se abrem puxando para baixo o mantêm.
    var filtro by rememberSaveable(stateSaver = FiltroHome.Saver) { mutableStateOf(FiltroHome.padrao(LocalDate.now())) }
    val naHome = ehAba && abaAssentada == Aba.HOME
    LaunchedEffect(naHome, rota) {
        if (!naHome && rota !in MANTEM_O_FILTRO) filtro = FiltroHome.padrao(LocalDate.now())
    }

    // O contador vermelho do ícone do Painel de Jogos: as pendências.
    val noContador = quantasPendencias(resumo)

    CompositionLocalProvider(LocalBarraDaTela provides barra) {
    Scaffold(
        topBar = {
            Column {
                BarraTitulo(
                    titulo,
                    tituloVolta = rota in ROTAS_DO_JOGO,
                    aoVoltar = when {
                        ehAba || rota in SEM_SETA -> null
                        acao?.aoVoltar != null -> acao.aoVoltar
                        else -> {
                            { nav.popBackStack(); Unit }
                        }
                    },
                    acoes = {
                        // Nas abas: o ícone do Painel de Jogos (com as pendências), à direita do título.
                        if (ehAba) BotaoDoPainel(noContador) { abrirPainel() }
                        // O "Detalhes →" da página do jogo (nos Detalhes, o "Voltar →"), na mesma cor e letra do título.
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
                                    // Já nesta aba: a lista volta ao começo.
                                    abaAtual == aba -> toques[i]++
                                    else -> escopo.launch { mostrar(aba) }
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
                // O voltar do celular numa página que não é a Home volta para a Home.
                BackHandler(enabled = abaAtual != Aba.HOME) { escopo.launch { mostrar(Aba.HOME) } }
                // Puxar qualquer aba para baixo (do alto da lista) abre o Painel de Jogos (até a 0.16, as
                // Notificações). Para os lados, as abas giram (da Home para a direita, os Ajustes).
                val puxaoVertical = rememberPuxaoAlemDaBorda(vertical = true, noInicio = { abrirPainel() })
                Box(Modifier.fillMaxSize().nestedScroll(puxaoVertical)) {
                    HorizontalPager(
                        state = paginas,
                        flingBehavior = soltarDasPaginas(paginas),
                        modifier = Modifier
                            .fillMaxSize()
                            .acompanharPuxao(puxaoVertical),
                    ) { pagina ->
                        val aba = abaDa(pagina)
                        when (aba) {
                            Aba.HOME -> HomeTela(
                                estado, visitaDaHome, filtro = filtro, abrir = ::abrir, apostar = { irParaAba(Aba.JOGOS) },
                                paraOTopo = toques[aba.ordinal],
                            )
                            Aba.JOGOS -> JogosTela(
                                estado,
                                abrirModalidade = { abrir("modalidade/${it.name}") },
                                abrirDetalhes = { abrir("detalhes/${it.name}") },
                                paraOTopo = toques[aba.ordinal],
                            )
                            Aba.AJUSTES -> AjustesTela(estado, atualizacao, abrir = ::abrir, paraOTopo = toques[aba.ordinal])
                        }
                    }
                    IndicadorDoPuxao(
                        puxaoVertical, puxando = "Painel de Jogos", pronto = "Painel de Jogos", icone = Icones.Painel,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                    )
                }
            }
            // Todas as telas por cima das abas voltam puxando para baixo (do alto da lista): o aviso diz
            // "Voltar" e, quando soltar já volta, o nome da tela de baixo. Muitas também fecham deslizando
            // o dedo para os lados.
            composable(ROTA_FILTRO) { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { FiltroTela(estado, filtro, mudar = { filtro = it }, aoFechar = { fechar(nav, e) }) }
            }
            // Comprovantes (as pendências do resumo do Painel de Jogos): voltam para a tela de antes.
            composable(ROTA_COMPROVANTES) { e ->
                // A leitura dos comprovantes continua mesmo fechando a tela: o modelo fica com as abas.
                val dono = remember(e) { nav.getBackStackEntry(ROTA_ABAS) }
                TelaDeCima(nav, e, ::nomeDaTela) {
                    Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) {
                        ComprovantesTela(estado, abrir = ::abrir, vm = viewModel(dono))
                    }
                }
            }
            // Painel de Jogos, com o resumo das pendências (o ícone da barra de cima, ou puxando qualquer aba para baixo).
            composable(ROTA_PAINEL) { e ->
                TelaDeCima(nav, e, ::nomeDaTela) {
                    Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) {
                        PainelDeJogosTela(estado, resumo, abrir = ::abrir, apostar = { irParaAba(Aba.JOGOS) })
                    }
                }
            }
            // O Saldo atual (tocar no quadro do saldo): deslizar o dedo para qualquer lado volta para a Home.
            composable("saldo") { e ->
                TelaDeCima(nav, e, ::nomeDaTela, aoVoltar = { irParaAba(Aba.HOME) }) {
                    Box(Modifier.fillMaxSize().deslizarParaFechar { irParaAba(Aba.HOME) }) { SaldoTela(estado) }
                }
            }
            composable("integridade") { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { IntegridadeTela(estado) } }
            }
            // Cada item do menu dos Ajustes que tem tela própria.
            composable(ROTA_SECAO_AJUSTES) { e ->
                val secao = SecaoAjuste.de(e.arguments?.getString("secao"))
                TelaDeCima(nav, e, ::nomeDaTela) {
                    Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) {
                        if (secao != null) AjustesSecaoTela(estado, secao, atualizacao, abrir = ::abrir)
                    }
                }
            }
            composable("aporte") { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { AporteTela(estado) } }
            }
            composable("rateio") { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { RateioTela(estado, abrir = ::abrir) } }
            }
            composable("extrato") { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { ExtratoTela(estado) } }
            }
            composable("extrato/{mes}") { e ->
                val mes = runCatching { YearMonth.parse(e.arguments?.getString("mes").orEmpty()) }.getOrNull()
                TelaDeCima(nav, e, ::nomeDaTela) {
                    Box(Modifier.fillMaxSize().deslizarParaFechar { fechar(nav, e) }) { ExtratoTela(estado, mesInicial = mes) }
                }
            }
            composable("comprovante/{id}") { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { ComprovanteTela(estado, e.arguments?.getString("id").orEmpty()) }
            }
            // A página do jogo: o volante (a aposta em andamento da modalidade ou uma nova).
            composable("modalidade/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                TelaDeCima(nav, e, ::nomeDaTela) {
                    if (m != null) ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir)
                }
            }
            // Segurar a modalidade em Jogos: a página do jogo já nos Detalhes.
            composable("detalhes/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                TelaDeCima(nav, e, ::nomeDaTela) {
                    if (m != null) ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir, detalhes = true)
                }
            }
            // Endereço antigo da aposta nova: a página do jogo.
            composable("nova/{nome}") { e ->
                val m = runCatching { Modalidade.valueOf(e.arguments?.getString("nome").orEmpty()) }.getOrNull()
                TelaDeCima(nav, e, ::nomeDaTela) {
                    if (m != null) ModalidadeTela(estado, m, aoSair = { fechar(nav, e) }, abrir = ::abrir)
                }
            }
            // Uma aposta (do Histórico ou de um aviso): a página do jogo dela, com a aposta no lugar do volante.
            composable("aposta/{id}") { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { PaginaDaAposta(estado, e.arguments?.getString("id").orEmpty(), ::abrir) { fechar(nav, e) } }
            }
            // A apuração fica na aposta (avisos antigos ainda podem abrir este endereço).
            composable("apuracao/{id}") { e ->
                TelaDeCima(nav, e, ::nomeDaTela) { PaginaDaAposta(estado, e.arguments?.getString("id").orEmpty(), ::abrir) { fechar(nav, e) } }
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
            text = { Text("Uma aposta acertou o prêmio principal. Veja no Histórico.") },
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

/**
 * Uma tela por cima das abas: puxar para baixo (do alto da lista) volta ([aoVoltar]; sem ele, fecha a
 * tela), com o nome da tela de baixo no aviso ([nome] da entrada de baixo).
 */
@Composable
private fun TelaDeCima(
    nav: NavHostController,
    entrada: NavBackStackEntry,
    nome: (NavBackStackEntry?) -> String,
    aoVoltar: (() -> Unit)? = null,
    conteudo: @Composable () -> Unit,
) {
    val anterior = remember(entrada) { nav.previousBackStackEntry }
    PuxarParaVoltar(nome(anterior), aoVoltar ?: { fechar(nav, entrada) }, conteudo = conteudo)
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

/** Tamanho dos ícones das 3 abas da barra de baixo (o padrão é 24dp). */
private val ICONE_DA_ABA = 31.dp

/** Tamanho do ícone do Painel de Jogos na barra de cima (até a 0.17, a bola de 38dp; até a 0.16, um bilhete e o sino). */
private val TAMANHO_DO_ICONE = 38.dp

/**
 * O ícone do Painel de Jogos (na barra de cima das abas): o quadrado azul-marinho com o painel dourado
 * e, no canto, o contador vermelho das pendências.
 */
@Composable
private fun BotaoDoPainel(pendencias: Int, aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar, modifier = Modifier.size(58.dp).semantics(mergeDescendants = true) { contentDescription = "Painel de Jogos" }) {
        BadgedBox(badge = {
            if (pendencias > 0) {
                Badge(
                    containerColor = Barra.contador,
                    contentColor = Color.White,
                    modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                ) {
                    Text(if (pendencias > 99) "99+" else pendencias.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }) {
            IconeDoPainel(TAMANHO_DO_ICONE)
        }
    }
}

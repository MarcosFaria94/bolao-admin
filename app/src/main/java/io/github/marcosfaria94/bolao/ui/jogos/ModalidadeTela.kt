package io.github.marcosfaria94.bolao.ui.jogos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.marcosfaria94.bolao.ui.comum.AcaoDaBarra
import io.github.marcosfaria94.bolao.ui.comum.LocalBarraDaTela
import io.github.marcosfaria94.bolao.ui.comum.acompanharPuxao
import io.github.marcosfaria94.bolao.ui.comum.rememberPuxaoAlemDaBorda
import io.github.marcosfaria94.bolao.ui.comum.soltarDasPaginas
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import io.github.marcosfaria94.bolao.domain.Tarjas
import io.github.marcosfaria94.bolao.ui.aposta.coresDaBola
import io.github.marcosfaria94.bolao.ui.aposta.CoresDaBola
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import io.github.marcosfaria94.bolao.ui.comum.deslizarEntreBotoes
import io.github.marcosfaria94.bolao.domain.PremiosTipicos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.aposta.TENTAR_DE_NOVO
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.ui.aposta.ApostaEmEdicao
import io.github.marcosfaria94.bolao.ui.aposta.AvisoDoComprovante
import io.github.marcosfaria94.bolao.ui.aposta.SecaoApostaRegistrada
import io.github.marcosfaria94.bolao.ui.aposta.SecaoVolante
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.aposta.proximaAposta
import io.github.marcosfaria94.bolao.ui.aposta.rascunhoDa
import io.github.marcosfaria94.bolao.ui.comum.CartaoQueAbre
import io.github.marcosfaria94.bolao.ui.comum.TarjaModalidade
import io.github.marcosfaria94.bolao.ui.aposta.valorSemQuebra
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Volante
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import java.time.LocalTime
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.TarjaDoDestaque
import io.github.marcosfaria94.bolao.ui.comum.TituloDestaque
import io.github.marcosfaria94.bolao.ui.comum.bola3D
import io.github.marcosfaria94.bolao.ui.comum.estiloNaBola
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import java.time.Instant
import java.time.LocalDate

/**
 * Página do jogo (tocar numa modalidade em Jogos): o emblema, o nome e a chance do prêmio
 * principal; o prêmio e o dia do sorteio num quadro; e, embaixo, o volante da aposta (do "Aposta
 * Nº1" até "Descartar rascunho"), com a aposta em andamento da modalidade ou uma nova.
 * "Detalhes →" (na barra de cima, na linha do título) leva aos Detalhes (último resultado, regras e
 * probabilidades), que ficam à direita da página: deslizar o dedo para a esquerda também abre e,
 * para a direita (ou o voltar), volta à página como ela estava, sem mexer na aposta.
 * Na barra: "Detalhes →" na página do jogo e, nos Detalhes, "← Aposta" (volta para a página do
 * volante). Deslizar para a direita na página do jogo ou para a esquerda nos Detalhes (e puxar para
 * baixo, ou o voltar do celular) sai da página, de volta para a tela de onde ela foi aberta ([aoSair]:
 * a Home, os Jogos, o Painel de Jogos...).
 *
 * [apostaId]: a aposta aberta pelo Histórico ou por um aviso. Registrada, ela aparece no lugar do
 * volante (com a apuração e os comprovantes); em andamento, ela é a do volante.
 *
 * Desde a 0.25, ao registrar a aposta do volante a página não vai para a aposta registrada: continua no
 * volante, vazio e no mesmo concurso, para mais apostas, e no alto fica o aviso do comprovante da Caixa
 * que falta ([AvisoDoComprovante]), com o botão que abre os Comprovantes.
 * [detalhes]: abre direto nos Detalhes (segurar a modalidade em Jogos); aí o voltar sai da página.
 *
 * Desde a 0.27: no alto, a tarja da modalidade (como as da Caixa) com o concurso e a chance; no quadro do
 * prêmio, os valores acumulados que a Caixa informa (para o concurso, para o de final 0 ou 5 e para o
 * concurso especial); registrar não leva mais a página para o alto (o lugar da tela fica onde estava).
 * [especial]: a página de um concurso especial (a opção à parte nos Jogos, [Especiais]), com o volante
 * e a aposta em andamento só dele.
 */
@Composable
fun ModalidadeTela(
    estado: Estado,
    m: Modalidade,
    aoSair: () -> Unit,
    apostaId: String? = null,
    abrir: (String) -> Unit = {},
    detalhes: Boolean = false,
    especial: Int? = null,
) {
    val app = LocalContext.current.bolao
    val indices = remember { app.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    val consultas by indices.consultas.collectAsStateWithLifecycle()
    // Ao entrar: a agenda dos sorteios decide se consulta a Caixa (o concurso do volante não pode ser um que já foi sorteado).
    LaunchedEffect(Unit) { indices.atualizarAoEntrar() }
    val item: Indices.Item? = itens[m]
    val u = item?.ultimo
    // A página de um concurso especial (desde a 0.27); a página normal deixa de fora as apostas em andamento
    // dos especiais que aparecem à parte nos Jogos.
    val esp: ConcursoEspecial? = especial?.let { Especiais.doConcurso(m, it) }
    val separados = remember(itens) {
        Especiais.aParte(Instant.now(), { itens[it]?.proximoNumero }).filter { it.modalidade == m }.map { it.concurso }.toSet()
    }
    val hoje = LocalDate.now()
    val escopo = rememberCoroutineScope()
    // Duas páginas lado a lado: a do jogo (0) e os Detalhes (1).
    val inicial = if (detalhes) 1 else 0
    val paginas = rememberPagerState(initialPage = inicial) { 2 }
    val nosDetalhes = paginas.currentPage == 1
    // O "← Aposta" da barra volta à página do volante. O voltar do celular também, menos quando a página
    // foi aberta direto nos Detalhes (segurar a modalidade): aí ele sai da página (volta para os Jogos).
    val voltarDosDetalhes: () -> Unit = { escopo.launch { paginas.animateScrollToPage(0) } }
    BackHandler(enabled = nosDetalhes && !detalhes) { escopo.launch { paginas.animateScrollToPage(0) } }
    // Na barra de cima: só o "Detalhes →"; nos Detalhes, só o "← Aposta" (volta à página do volante). Desde a
    // 0.22 não há mais o "← Voltar" nem o "Voltar →": sai-se da página deslizando o dedo ou pelo voltar do celular.
    val barra = LocalBarraDaTela.current
    val dono = LocalViewModelStoreOwner.current
    DisposableEffect(nosDetalhes, dono) {
        val acao = if (nosDetalhes) {
            AcaoDaBarra(dono, titulo = "Aposta", aoVoltar = voltarDosDetalhes)
        } else {
            AcaoDaBarra(dono, botao = "Detalhes", aoTocarBotao = { escopo.launch { paginas.animateScrollToPage(1) } })
        }
        barra.acao = acao
        onDispose { if (barra.acao === acao) barra.acao = null }
    }
    // Além das bordas: para a direita na página do jogo ou para a esquerda nos Detalhes, sai da página.
    val puxao = rememberPuxaoAlemDaBorda(
        vertical = false,
        noInicio = { if (paginas.currentPage == 0) aoSair() },
        noFim = { if (paginas.currentPage == 1) aoSair() },
    )

    val aberta = apostaId?.let { id -> estado.apostas.firstOrNull { it.id == id } }
    // A página é de uma aposta já registrada (aberta pelo Histórico, por um aviso...) ou do volante; decidido ao
    // abrir: a aposta do volante registrada aqui não troca a página (desde a 0.25, o volante continua, vazio).
    val existiaAoAbrir = remember(apostaId) { apostaId == null || aberta != null }
    val daRegistrada = remember(apostaId) { aberta?.registrada == true }
    if (!existiaAoAbrir || (daRegistrada && aberta == null)) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não existe mais.") }
        return
    }
    // O volante: a aposta em andamento (a aberta ou a da modalidade) ou uma nova, com o volante vazio.
    // Fica no modelo da página: ver os Detalhes não descarta nada.
    val vm: ApostaEmEdicao? = if (daRegistrada) {
        null
    } else {
        viewModel(key = "volante") {
            val emAndamento = aberta ?: rascunhoDa(estado, m, esp?.concurso, if (esp == null) separados else emptySet())
            ApostaEmEdicao(
                app.repo,
                app.escopo,
                emAndamento ?: novaAposta(estado, m, app.indices.itens.value[m]?.ultimo, hoje, System.currentTimeMillis(), especial = esp),
                nova = emAndamento == null,
            )
        }
    }
    // Registrada (a aberta): a página mostra a aposta registrada.
    val registrada: Aposta? = aberta?.takeIf { daRegistrada }
    val aposta: Aposta = registrada ?: vm!!.aposta
    // As apostas registradas nesta página que ainda esperam o comprovante da Caixa (o aviso do alto do volante).
    val semComprovante = vm?.let { v ->
        val ids = v.registradasAqui.toSet()
        Operacoes.apostasSemComprovante(estado).filter { it.id in ids }
    }.orEmpty()
    val rolagem = rememberScrollState()

    // O volante acompanha a Caixa: a consulta que chega com a página aberta pode trazer outro concurso.
    if (vm != null) {
        LaunchedEffect(vm, item?.proximoNumero, u?.proximaData) {
            vm.acompanharConcurso(u, item?.proximoNumero, hoje)?.let { (antes, depois) ->
                // A gravada (em andamento) a raiz do app já avisa; aqui, só a que ainda não foi gravada.
                if (antes.concurso > 0 && estado.apostas.none { it.id == antes.id }) {
                    app.repo.avisar("O concurso ${antes.concurso} da ${m.nome} já foi sorteado: a aposta passou para o ${depois.concurso}.")
                }
            }
        }
    }

    // O quadro mostra o concurso da aposta; sem os dados dela, os do próximo concurso (da Caixa).
    val doProximo = registrada == null && (aposta.concurso == 0 || aposta.concurso == item?.proximoNumero)
    // O concurso especial da aposta (o da página especial ou, quando ele já é o próximo, o da página normal).
    val especialDaAposta = esp ?: Especiais.doConcurso(m, aposta.concurso)
    val premio = aposta.premioEstimado?.takeIf { it > 0 } ?: u?.premioEstimadoProximo?.takeIf { doProximo && it > 0 }
        ?: especialDaAposta?.premioEstimado
    val sorteio = aposta.dataSorteio ?: u?.proximaData?.takeIf { doProximo } ?: especialDaAposta?.dia
    // O concurso em destaque (desde a 0.34 sempre o de maior k1, especial ou não; de 0.28 a 0.33, o especial quando
    // havia um): na página do especial (ou na da modalidade, quando o especial já é o próximo concurso) ou na do jogo.
    val estimativas by indices.especiais.collectAsStateWithLifecycle()
    val destaque = remember(itens, estimativas) { indices.destaque(itens) }
    val especialEmDestaque = destaque?.especial
    val emDestaque = destaque != null && destaque.modalidade == m && registrada == null &&
        if (especialEmDestaque != null) especialDaAposta == especialEmDestaque else doProximo && especialDaAposta == null
    // O próximo concurso (da Caixa, guardado no celular) já foi sorteado e o resultado ainda não chegou.
    val situacao = indices.situacao(m, Instant.now(), itens, consultas)
    val avisoDoQuadro = situacao.sorteio?.takeIf { doProximo && situacao.sorteado }?.let {
        "Sorteado ${AgendaDaCaixa.quando(it, Instant.now())}: aguardando o resultado da Caixa."
    }

    // As duas páginas ficam montadas: ir aos Detalhes e voltar não perde nada da página do jogo.
    Box(Modifier.fillMaxSize().nestedScroll(puxao)) {
    HorizontalPager(state = paginas, beyondViewportPageCount = 1, flingBehavior = soltarDasPaginas(paginas), modifier = Modifier.fillMaxSize().acompanharPuxao(puxao)) { pagina ->
        if (pagina == 1) {
            DetalhesDaModalidade(estado, m, u, atualizando, visivel = paginas.currentPage == 1)
            return@HorizontalPager
        }
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rolagem)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CabecalhoModalidade(m, aposta.concurso.takeIf { it > 0 } ?: item?.proximoNumero, especialDaAposta)
            QuadroPremio(
                premio = premio,
                sorteio = sorteio,
                hoje = hoje,
                federal = m == Modalidade.FEDERAL,
                destaque = emDestaque,
                especial = especialDaAposta != null,
                consultando = premio == null && sorteio == null && atualizando,
                aviso = avisoDoQuadro,
                hora = especialDaAposta?.takeIf { it.dia == sorteio }?.hora,
                acumulados = if (registrada == null) acumuladosDaPagina(m, u, doProximo, esp) else emptyList(),
            )
            if (registrada != null) {
                SecaoApostaRegistrada(
                    estado, registrada, abrir = abrir,
                    aoSair = {
                        // Descartada (ou excluída) aqui: o volante desta página não traz a aposta de volta como rascunho.
                        vm?.esquecer()
                        aoSair()
                    },
                )
            } else {
                val v = vm!!
                // A aposta mudada (ou descartada) numa tela aberta por cima desta: a página acompanha.
                val gravada = estado.apostas.firstOrNull { it.id == v.aposta.id }
                LaunchedEffect(gravada) { if (v.conferirGravada(gravada)) aoSair() }
                // O aviso do comprovante aparece no alto depois do registro: a rolagem desce o mesmo tanto, para o
                // lugar da tela (o volante, o botão) ficar onde estava (desde a 0.27; antes a página voltava ao alto).
                // A altura conta também o espaço entre os itens da página (o aviso é um item a mais).
                val espaco = with(LocalDensity.current) { 12.dp.roundToPx() }
                var alturaDoAviso by remember { mutableIntStateOf(0) }
                if (semComprovante.isNotEmpty()) {
                    Box(
                        Modifier.onSizeChanged { novo ->
                            val total = novo.height + espaco
                            val diferenca = total - alturaDoAviso
                            alturaDoAviso = total
                            if (diferenca != 0) escopo.launch { rolagem.scrollBy(diferenca.toFloat()) }
                        },
                    ) {
                        AvisoDoComprovante(semComprovante, aoAnexar = { abrir(Pendencias.ABA_COMPROVANTES) })
                    }
                } else if (alturaDoAviso != 0) {
                    LaunchedEffect(Unit) {
                        val diferenca = -alturaDoAviso
                        alturaDoAviso = 0
                        rolagem.scrollBy(diferenca.toFloat())
                    }
                }
                SecaoVolante(
                    estado = estado,
                    r = v.aposta,
                    mudar = v::mudar,
                    // O concurso do volante já foi sorteado e o resultado ainda não chegou: sem registro até lá.
                    bloqueio = avisoDoQuadro?.let { bloqueioDaAposta(v.aposta.concurso.takeIf { it > 0 } ?: item?.proximoNumero) },
                    atualizando = atualizando,
                    aoAtualizar = { indices.atualizar(forcar = true) },
                    aoRegistrar = { feita, feitas ->
                        // Desde a 0.25: o volante continua na página, vazio e no mesmo concurso, para a próxima aposta.
                        // Desde a 0.27 a página não sobe mais para o alto (pedido do usuário).
                        v.recomecar(proximaAposta(estado, feita, LocalDate.now(), System.currentTimeMillis()), feitas)
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    }
}

/**
 * O aviso do volante bloqueado (desde a 0.22): os dados guardados são de um concurso que já foi (ou está
 * sendo) sorteado e o resultado ainda não chegou; a aposta só é liberada com os dados atualizados.
 */
fun bloqueioDaAposta(concurso: Int?): String =
    "Aposta bloqueada: o concurso" + (concurso?.let { " $it" } ?: "") + " já foi sorteado e o resultado ainda não chegou. " +
        "O registro é liberado quando os dados forem atualizados."

/**
 * O alto da página do jogo (desde a 0.27): a tarja da modalidade (ou a do concurso especial), como as da
 * Caixa, e embaixo o concurso e a chance do prêmio principal ("1 em 50.063.860"). Até a 0.26, a bola com a
 * sigla e o concurso e o nome bem grande.
 */
@Composable
private fun CabecalhoModalidade(m: Modalidade, concurso: Int?, especial: ConcursoEspecial?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TarjaModalidade(m, Modifier.fillMaxWidth(), altura = 58.dp, especial = especial)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                concurso?.let { "Concurso $it" } ?: "Concurso",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Probabilidades.faixas(m).firstOrNull()?.let {
                Text(Probabilidades.texto(it.umEm), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Os valores acumulados que a Caixa informa (desde a 0.27, no quadro do prêmio da página do jogo), como no
 * site dela: o acumulado para o concurso da aposta, o do próximo concurso de final 0 ou 5 e o do concurso
 * especial (Mega da Virada, Quina de São João...). Só os que a Caixa informa (maiores que zero) e só para o
 * próximo concurso ([doProximo]); na página de um concurso especial ([esp]), o acumulado para ele.
 */
fun acumuladosDaPagina(m: Modalidade, u: Concurso?, doProximo: Boolean, esp: ConcursoEspecial?): List<Pair<String, Long>> {
    if (u == null) return emptyList()
    val nome = esp?.nome ?: Especiais.nomeDoEspecial(m)
    val doEspecial = u.acumuladoEspecial?.let { v -> (if (nome != null) "Acumulado para a $nome" else "Acumulado para o concurso especial") to v }
    if (esp != null) return listOfNotNull(doEspecial)
    if (!doProximo) return emptyList()
    return listOfNotNull(
        u.acumuladoProximo?.let { v -> "Acumulado para o concurso ${u.proximoNumero ?: (u.numero + 1)}" to v },
        u.acumuladoFinal05?.let { v -> u.numeroConcursoEspecial?.let { n -> "Acumulado para o concurso de final ${n % 10} ($n)" to v } },
        doEspecial,
    )
}

/**
 * O prêmio (de ponta a ponta) e o dia do sorteio, juntos num quadro. Desde a 0.34 o concurso em destaque (o de maior
 * k1) tem, no alto do quadro, a tarja dourada "CONCURSO EM DESTAQUE" e o concurso especial ([especial]), o título que
 * brilha "CONCURSO ESPECIAL" (até a 0.33 só o destaque tinha um título, e o especial era sempre o destaque).
 */
@Composable
private fun QuadroPremio(
    premio: Long?,
    sorteio: LocalDate?,
    hoje: LocalDate,
    federal: Boolean,
    destaque: Boolean,
    consultando: Boolean,
    especial: Boolean = false,
    aviso: String? = null,
    hora: LocalTime? = null,
    acumulados: List<Pair<String, Long>> = emptyList(),
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (destaque) TarjaDoDestaque(Modifier.fillMaxWidth())
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (especial) {
                TituloDestaque(especial = true)
                Spacer(Modifier.height(4.dp))
            }
            if (consultando) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("  Consultando a Caixa...", style = MaterialTheme.typography.bodyLarge)
                }
                return@Column
            }
            if (federal && premio != null) {
                Text("1º prêmio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (premio != null) {
                TextoQueCabe(
                    Dinheiro.porExtenso(premio),
                    MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 70.sp),
                    peso = FontWeight.Bold,
                    menor = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // A Caixa ainda não divulgou a estimativa: no lugar do valor grande, uma linha como a do sorteio.
                RotuloValor("Prêmio", NAO_DIVULGADO)
            }
            RotuloValor("Sorteio", (sorteio?.let { Datas.sorteio(it, hoje) } ?: NAO_DIVULGADO) + (hora?.let { " às ${AgendaDaCaixa.hora(it)}" } ?: ""))
            // Desde a 0.27: os acumulados que a Caixa informa (o do concurso, o de final 0 ou 5 e o do especial).
            if (acumulados.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    acumulados.forEach { (rotulo, valor) ->
                        Column {
                            Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(valorSemQuebra(Dinheiro.rs(valor)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            // Desde a 0.20: o concurso já foi sorteado e o resultado ainda não chegou ao app.
            if (aviso != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.HourglassTop, contentDescription = null, tint = corDestaque(), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(aviso, style = MaterialTheme.typography.bodyMedium, color = corDestaque())
                }
            }
        }
    }
}

/**
 * "Detalhes" do jogo, à direita da página: o último resultado (se acumulou, os números sorteados e a
 * premiação; na Dupla Sena, cada sorteio com a sua premiação, desde a 0.27) e, desde a 0.27, as Regras, as
 * Probabilidades (em tabela, como a do site da Caixa) e a Quantidade de prêmios, cada uma num cartão que
 * abre e fecha tocando no título. Com as apostas com mais números ligadas (o parâmetro), a tabela de
 * probabilidades tem uma linha para cada quantidade de números e aparece a da quantidade de prêmios; sem
 * ele, só a linha da aposta simples. Voltar leva à página do jogo como ela estava (o volante não muda).
 *
 * Desde a 0.28, logo acima do resultado, os botões para ir e voltar nos concursos anteriores e o campo para
 * digitar um concurso ([NavegacaoDoConcurso]). Desde a 0.33 o resultado vem do histórico do app ([Historico]: todos
 * os concursos já sorteados vêm embutidos e os novos se acrescentam a cada consulta); só o que não está nele vai à
 * Caixa. Saindo dos Detalhes ([visivel] falso), volta para o último concurso.
 */
@Composable
private fun DetalhesDaModalidade(estado: Estado, m: Modalidade, u: Concurso?, atualizando: Boolean, visivel: Boolean = true) {
    val mais = estado.parametros.maisNumeros && Volante.aceitaMais(m)
    // O concurso escolhido nos botões ou no campo (null = o último, o que os Jogos guardam).
    var escolhido by remember(m) { mutableStateOf<Int?>(null) }
    var outro by remember(m) { mutableStateOf<Concurso?>(null) }
    var buscando by remember(m) { mutableStateOf(false) }
    var erro by remember(m) { mutableStateOf<String?>(null) }
    var tentativa by remember(m) { mutableIntStateOf(0) }
    val historico = LocalContext.current.bolao.historico
    LaunchedEffect(visivel) { if (!visivel) escolhido = null }
    // O histórico da modalidade já fica lido na memória ao abrir os Detalhes (os botões e o campo respondem na hora).
    LaunchedEffect(m) { runCatching { historico.quantos(m) } }
    val ultimo = u?.numero
    val pedido = escolhido?.takeIf { it != ultimo }
    LaunchedEffect(m, pedido, tentativa) {
        erro = null
        if (pedido == null) {
            outro = null
            buscando = false
            return@LaunchedEffect
        }
        if (outro?.numero == pedido) return@LaunchedEffect
        buscando = true
        outro = try {
            historico.concurso(m, pedido) ?: CaixaApi.guardado(m, pedido) ?: CaixaApi.concurso(m, pedido)
        } catch (t: Throwable) {
            erro = erroDoConcurso(pedido)
            null
        }
        buscando = false
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (ultimo != null) {
            NavegacaoDoConcurso(numero = pedido ?: ultimo, ultimo = ultimo) { n -> escolhido = n.coerceIn(1, ultimo) }
        }
        when {
            pedido == null -> UltimoResultado(m, u, atualizando)
            buscando -> CartaoConsultando()
            erro != null -> CartaoErroDoConcurso(erro!!) { tentativa++ }
            else -> UltimoResultado(m, outro?.takeIf { it.numero == pedido }, atualizando = true, oUltimo = false)
        }
        CartaoQueAbre(TITULO_REGRAS) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                regras(m, estado, mais).forEach { (r, v) -> LinhaDaRegra(r, v) }
            }
        }
        // Desde a 0.29: o preço de cada aposta num cartão só dele (era a primeira tabela das Probabilidades).
        CartaoQueAbre(TITULO_PRECO) { TabelaDePreco(m, estado, mais) }
        CartaoQueAbre(TITULO_PROBABILIDADES) { TabelaDeProbabilidades(m, mais) }
        if (mais && Probabilidades.gruposDePremios(m) != null) {
            CartaoQueAbre(TITULO_QUANTIDADE) { TabelaDeQuantidade(m) }
        }
        // Desde a 0.32, em todas as modalidades: quanto cada faixa costuma pagar (a estimativa e o último concurso);
        // desde a 0.34, a coluna do concurso que está na tela (o escolhido nos botões ou no campo, do histórico).
        val naTela = if (pedido == null) u else outro?.takeIf { it.numero == pedido }
        if (m != Modalidade.FEDERAL) CartaoQueAbre(TITULO_PREMIOS_TIPICOS) { TabelaDePremiosTipicos(m, u, naTela) }
        Spacer(Modifier.height(8.dp))
    }
}

const val TITULO_REGRAS = "Regras"
const val TITULO_PRECO = "Preço"
const val TITULO_PROBABILIDADES = "Probabilidades"
const val TITULO_QUANTIDADE = "Quantidade de prêmios"
const val TITULO_PREMIOS_TIPICOS = "Prêmios típicos"

/** As colunas da tabela dos prêmios típicos (desde a 0.32). */
const val COLUNA_FAIXA = "Faixa"
const val COLUNA_ESTIMATIVA = "Estimativa"
const val COLUNA_ULTIMO_CONCURSO = "Último concurso"

/** A coluna do concurso escolhido nos Detalhes (desde a 0.34): "Concurso 7000" (o último continua "Último concurso"). */
fun colunaDoConcurso(numero: Int, ultimo: Int?): String = if (numero == ultimo) COLUNA_ULTIMO_CONCURSO else "Concurso $numero"

/**
 * Os prêmios típicos (desde a 0.32, em todas as modalidades, no cartão que começa fechado): uma linha por faixa de
 * premiação, com a estimativa do que cada aposta ganhadora recebe no próximo concurso ([PremiosTipicos.estimar], o
 * mesmo cálculo do k1, sem o IR) e o que o concurso pagou; embaixo, a nota que explica que é só uma estimativa
 * e como ela é calculada. Sem a estimativa (Loteca, concurso especial), só o concurso. Desde a 0.34 a coluna é a do
 * concurso que está na tela ([naTela]: o escolhido nos botões ou no campo; sem escolher, o último, [u]).
 */
@Composable
private fun TabelaDePremiosTipicos(m: Modalidade, u: Concurso?, naTela: Concurso? = u) {
    val indices = LocalContext.current.bolao.indices
    val itens by indices.itens.collectAsStateWithLifecycle()
    val estimativa = remember(itens[m]) { indices.premiosTipicos(m) }
    val concurso = naTela ?: u
    if (concurso == null && estimativa == null) {
        Text("Sem dados da Caixa agora.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val linhas = PremiosTipicos.linhas(m, estimativa, concurso)
    val coluna = concurso?.let { colunaDoConcurso(it.numero, u?.numero) } ?: COLUNA_ULTIMO_CONCURSO
    fun texto(v: Long?): String = v?.let { valorSemQuebra(PremiosTipicos.valor(it)) } ?: "—"
    TabelaRolavel(
        primeira = COLUNA_FAIXA,
        grupos = emptyList(),
        colunas = if (estimativa != null) listOf(COLUNA_ESTIMATIVA, coluna) else listOf(coluna),
        linhas = linhas.map { l -> l.nome to (if (estimativa != null) listOf(texto(l.estimativa), texto(l.ultimo)) else listOf(texto(l.ultimo))) },
    )
    val notas = listOfNotNull(
        if (estimativa != null) PremiosTipicos.NOTA_DA_ESTIMATIVA else PremiosTipicos.SEM_ESTIMATIVA,
        concurso?.let { PremiosTipicos.notaDoConcurso(it.numero, ultimo = it.numero == u?.numero) },
        Probabilidades.LEGENDA_DA_MAIS_MILIONARIA.takeIf { m == Modalidade.MAIS_MILIONARIA },
    )
    notas.forEach { n ->
        Text("* $n", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

/**
 * Uma linha das Regras (desde a 0.29): o rótulo à esquerda, inteiro, e o valor à direita, que quebra a linha
 * quando não cabe sem empurrar o rótulo (na 0.28, "Premiação" se partia junto com o valor da Timemania).
 */
@Composable
private fun LinhaDaRegra(rotulo: String, valor: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.width(16.dp))
        // Os nomes e os valores não se partem no fim da linha ("Time do / Coração", "R$ / 6,00").
        Text(semQuebrarNomes(valor), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/** Os valores das Regras sem quebra dentro dos nomes ("Time do Coração", "Mês de Sorte") e do dinheiro (desde a 0.29). */
fun semQuebrarNomes(texto: String): String = valorSemQuebra(texto)
    .replace("Time do Coração", "Time\u00A0do\u00A0Coração")
    .replace("Mês de Sorte", "Mês\u00A0de\u00A0Sorte")

/** O rótulo da primeira coluna das tabelas: os números jogados (e os trevos, na +Milionária). */
private fun primeiraColuna(m: Modalidade): String = if (m.trevos > 0) "Números + trevos" else "Números"

/** O rótulo de uma linha das tabelas: "6", "6 + 2" (+Milionária) ou "7" (as 7 colunas da Super Sete). */
private fun rotuloDaLinha(m: Modalidade, l: Probabilidades.LinhaDaTabela): String =
    if (m.trevos > 0) "${l.numeros} + ${l.trevos}" else if (m.formato == Formato.COLUNAS) "7" else "${l.numeros}"

/**
 * O preço de cada aposta (desde a 0.29, no cartão "Preço", entre as Regras e as Probabilidades; na 0.28 era a
 * primeira tabela das Probabilidades): os números jogados, quantas apostas simples cabem neles e o preço.
 */
@Composable
private fun TabelaDePreco(m: Modalidade, estado: Estado, mais: Boolean) {
    val preco = estado.config.preco(m)
    val linhas = remember(m, mais) { Probabilidades.tabela(m, mais) }
    TabelaRolavel(
        primeira = primeiraColuna(m),
        grupos = emptyList(),
        colunas = listOf(COLUNA_QTD_DE_JOGOS, COLUNA_PRECO),
        linhas = linhas.map { l -> rotuloDaLinha(m, l) to listOf(Probabilidades.milhares(l.apostasSimples), valorSemQuebra(Dinheiro.rs(preco * l.apostasSimples))) },
        // Desde a 0.32, a +Milionária (números e trevos, 35 linhas) se ordena tocando no título da coluna.
        ordem = if (ordenaAsTabelas(m, linhas)) linhas.map { l -> listOf(l.apostasSimples, preco * l.apostasSimples) } else null,
    )
    if (!mais && Volante.aceitaMais(m)) {
        Text("* $NOTA_DAS_APOSTAS_COM_MAIS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

private const val NOTA_DAS_APOSTAS_COM_MAIS =
    "Com as apostas com mais números ligadas (Ajustes > Parâmetros), a tabela mostra também as apostas com mais números."

/**
 * As probabilidades, como na tabela da Caixa (desde a 0.29, uma tabela só, que rola para os lados quando não
 * cabe; na 0.28, dividida em tabelas menores): os números jogados e, embaixo do título "Probabilidade de
 * acerto (1 em)", o "1 em X" de cada faixa. Com uma linha só (a aposta simples: sem as apostas com mais
 * números, ou na Timemania, que só tem ela), a tabela vira de lado ([linhasInvertidas]): "Acertos" e
 * "Probabilidade", uma linha por faixa.
 */
@Composable
private fun TabelaDeProbabilidades(m: Modalidade, mais: Boolean) {
    val linhas = remember(m, mais) { Probabilidades.tabela(m, mais) }
    val colunas = Probabilidades.colunas(m)
    if (linhas.size == 1) {
        TabelaRolavel(
            primeira = if (m.trevos > 0) primeiraColuna(m) else COLUNA_ACERTOS,
            grupos = emptyList(),
            colunas = listOf(COLUNA_PROBABILIDADE),
            linhas = linhasInvertidas(m, linhas[0]),
        )
    } else if (m == Modalidade.MAIS_MILIONARIA) {
        ProbabilidadesEmPares(m, linhas, colunas)
    } else {
        TabelaRolavel(
            primeira = primeiraColuna(m),
            grupos = listOf(PROBABILIDADE_DE_ACERTO to colunas.size),
            colunas = colunas,
            linhas = linhas.map { l -> rotuloDaLinha(m, l) to colunas.indices.map { j -> l.umEm.getOrNull(j)?.let { Probabilidades.milhares(it) } ?: "—" } },
        )
    }
    // Desde a 0.32, a legenda do traço quando a tabela tem algum (a +Milionária com os 6 trevos).
    val notas = Probabilidades.observacoesDaTabela(m) +
        (if (Probabilidades.temTraco(linhas)) listOf(Probabilidades.legendaDoTraco(m)) else emptyList()) +
        if (!mais && Volante.aceitaMais(m)) listOf(NOTA_DAS_APOSTAS_COM_MAIS) else emptyList()
    notas.forEach { obs ->
        Text("* $obs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

/** As tabelas da modalidade se ordenam tocando no título da coluna (desde a 0.32: a +Milionária, com mais de uma linha). */
fun ordenaAsTabelas(m: Modalidade, linhas: List<Probabilidades.LinhaDaTabela>): Boolean = m == Modalidade.MAIS_MILIONARIA && linhas.size > 1

/**
 * As faixas da +Milionária em pares (desde a 0.32, pedido do usuário: a tabela das 10 faixas era grande demais): os
 * botões "6", "5", "4", "3" e "2" acertos mostram a tabela das duas faixas daquela quantidade de números ("6 + 2" e
 * "6 + 0/1"), com 3 colunas, que cabe na tela (e deixa o dedo passar a página). Deslizar o dedo para os lados em
 * cima dos botões ou da tabela troca o botão ([deslizarEntreBotoes]); tocar no título de uma coluna ordena.
 */
@Composable
private fun ProbabilidadesEmPares(m: Modalidade, linhas: List<Probabilidades.LinhaDaTabela>, colunas: List<String>) {
    val pares = remember(colunas) { colunas.indices.chunked(2) }
    val acertos = remember(pares) { pares.map { par -> colunas[par[0]].substringBefore(" ").toIntOrNull() ?: 0 } }
    var par by rememberSaveable(m) { mutableIntStateOf(0) }
    val g = par.coerceIn(pares.indices)
    val faixas = pares[g]
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.deslizarEntreBotoes(g, pares.size) { par = it },
    ) {
        SeletorDeAcertos(acertos, g) { par = it }
        TabelaRolavel(
            primeira = primeiraColuna(m),
            grupos = listOf(PROBABILIDADE_DE_ACERTO to faixas.size),
            colunas = faixas.map { colunas[it] },
            linhas = linhas.map { l -> rotuloDaLinha(m, l) to faixas.map { j -> l.umEm.getOrNull(j)?.let { Probabilidades.milhares(it) } ?: "—" } },
            ordem = linhas.map { l -> faixas.map { j -> l.umEm.getOrNull(j) } },
        )
    }
}

/**
 * A tabela de probabilidades de lado (desde a 0.29, pedido para a Timemania; vale para toda tabela de uma linha
 * só): uma linha por faixa, com os acertos ([rotuloDaFaixa]) e "1 em X".
 */
fun linhasInvertidas(m: Modalidade, l: Probabilidades.LinhaDaTabela): List<Pair<String, List<String>>> =
    Probabilidades.colunas(m).mapIndexed { j, c ->
        rotuloDaFaixa(m, c) to listOf(l.umEm.getOrNull(j)?.let { Probabilidades.texto(it) } ?: "—")
    }

/**
 * O nome da faixa na tabela de lado: os acertos ("7" para "7 acertos", "7" para "7 colunas"), com o nome da
 * faixa quando ela tem um ("6 (Sena)"); o Time do Coração, o Mês de Sorte e as faixas da +Milionária ("6 + 2")
 * ficam como estão.
 */
fun rotuloDaFaixa(m: Modalidade, coluna: String): String {
    val numero = coluna.substringBefore(" ")
    return when {
        coluna.endsWith(" acertos") || coluna.endsWith(" colunas") -> numero
        m == Modalidade.MEGA_SENA || m == Modalidade.DUPLA_SENA || m == Modalidade.QUINA ->
            mapOf("Sena" to 6, "Quina" to 5, "Quadra" to 4, "Terno" to 3, "Duque" to 2)[coluna]?.let { "$it ($coluna)" } ?: coluna
        else -> coluna
    }
}

/** As colunas das tabelas (desde a 0.28/0.29). */
const val COLUNA_QTD_DE_JOGOS = "Qtd de jogos"
const val COLUNA_PRECO = "Preço"
const val COLUNA_ACERTOS = "Acertos"
const val COLUNA_PROBABILIDADE = "Probabilidade"

/** O título das colunas das faixas nas tabelas de probabilidades. */
const val PROBABILIDADE_DE_ACERTO = "Probabilidade de acerto (1 em)"

/** "6 acertos": o botão de cada tabela da quantidade de prêmios (desde a 0.28, o título dela). */
fun tituloDaQuantidade(acertando: Int): String = "$acertando acertos"

/**
 * A "Quantidade de prêmios a receber acertando", como a da Caixa (só com as apostas com mais números). Desde a
 * 0.29, os botões "6 acertos", "5 acertos" e "4 acertos" (na Mega-Sena; nas outras modalidades, os acertos
 * delas) trocam a tabela, que é uma só: os números jogados e quantos prêmios de cada faixa a aposta recebe
 * acertando aquilo (rola para os lados quando não cabe).
 */
@Composable
private fun TabelaDeQuantidade(m: Modalidade) {
    val grupos = Probabilidades.gruposDePremios(m) ?: return
    val numeros = remember(m) { (m.qtdAposta..Volante.maximo(m)).toList() }
    val quantidades = remember(m) { numeros.map { n -> Probabilidades.quantidadeDePremios(m, n) } }
    var escolhido by rememberSaveable(m) { mutableIntStateOf(0) }
    val g = escolhido.coerceIn(grupos.indices)
    val grupo = grupos[g]
    // Desde a 0.32, deslizar o dedo para os lados troca o botão (nos extremos, a página sai dos Detalhes).
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.deslizarEntreBotoes(g, grupos.size) { escolhido = it }) {
        SeletorDeAcertos(grupos.map { it.acertando }, g) { escolhido = it }
        Text(
            "Quantos prêmios de cada faixa a aposta recebe acertando ${grupo.acertando} números:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TabelaRolavel(
            primeira = "Números",
            grupos = emptyList(),
            colunas = grupo.faixas.map { Probabilidades.nomeDaFaixa(m, it) },
            linhas = numeros.mapIndexed { i, n -> "$n" to grupo.faixas.indices.map { j -> Probabilidades.milhares(quantidades[i][g][j]) } },
        )
    }
    val nota = when (m) {
        Modalidade.DUPLA_SENA -> "Em cada um dos 2 sorteios."
        Modalidade.DIA_DE_SORTE -> "Sem contar o Mês de Sorte."
        else -> null
    }
    nota?.let {
        Text("* $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

/**
 * Os botões que trocam a tabela da quantidade de prêmios (desde a 0.29): um por quantidade de acertos, lado a
 * lado, o escolhido destacado. Com até 3, "6 acertos" numa linha; com mais (Lotofácil, Quina...), o número
 * grande e "acertos" embaixo, para caberem na largura da tela.
 */
@Composable
private fun SeletorDeAcertos(acertos: List<Int>, escolhido: Int, aoEscolher: (Int) -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    val umaLinha = acertos.size <= 3
    // Sem a medida intrínseca da altura (o TextoQueCabe não aceita): todos com a mesma altura mínima.
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        acertos.forEachIndexed { i, n ->
            val sim = i == escolhido
            val fundo = if (sim) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
            val texto = if (sim) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = if (umaLinha) 44.dp else 52.dp)
                    .clip(forma)
                    .background(fundo)
                    .clickable { aoEscolher(i) }
                    .clearAndSetSemantics {
                        contentDescription = tituloDaQuantidade(n)
                        role = Role.Tab
                        selected = sim
                        onClick { aoEscolher(i); true }
                    }
                    .padding(horizontal = 2.dp, vertical = 6.dp),
            ) {
                if (umaLinha) {
                    Text(tituloDaQuantidade(n), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = texto, maxLines = 1, softWrap = false)
                } else {
                    Text("$n", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = texto, maxLines = 1)
                    // Diminui para caber com a letra grande do celular (5 botões na Lotofácil).
                    TextoQueCabe("acertos", MaterialTheme.typography.labelSmall, cor = texto, menor = 8.sp)
                }
            }
        }
    }
}

/** "Não foi possível consultar o concurso 2949 na Caixa." */
fun erroDoConcurso(numero: Int): String = "Não foi possível consultar o concurso $numero na Caixa."

/**
 * Os botões para ir e voltar nos concursos (desde a 0.28, logo acima do resultado nos Detalhes): "‹" (o
 * anterior), o campo com o número e "›" (o seguinte, até o último). Desde a 0.33, o campo se chama só "Concurso"
 * (era "Concurso (o último)" no último) e o botão "Ir" fica embaixo (o Ir do teclado continua valendo); ele só
 * acende com um número diferente do que está na tela.
 */
@Composable
private fun NavegacaoDoConcurso(numero: Int, ultimo: Int, aoIr: (Int) -> Unit) {
    var texto by remember(numero) { mutableStateOf(numero.toString()) }
    val foco = LocalFocusManager.current
    val digitado = texto.toIntOrNull()?.takeIf { it > 0 }
    fun ir() {
        digitado?.let(aoIr) ?: run { texto = numero.toString() }
        foco.clearFocus()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalIconButton(onClick = { aoIr(numero - 1) }, enabled = numero > 1, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = CONCURSO_ANTERIOR)
        }
        OutlinedTextField(
            value = texto,
            onValueChange = { novo -> texto = novo.filter(Char::isDigit).take(6) },
            singleLine = true,
            label = { Text(ROTULO_DO_CONCURSO) },
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { ir() }, onDone = { ir() }),
            modifier = Modifier.weight(1f).semantics { contentDescription = CAMPO_DO_CONCURSO },
        )
        FilledTonalIconButton(onClick = { aoIr(numero + 1) }, enabled = numero < ultimo, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = PROXIMO_CONCURSO)
        }
    }
    Button(
        onClick = { ir() },
        enabled = digitado != null && digitado.coerceAtMost(ultimo) != numero,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
    ) {
        Text(IR_AO_CONCURSO, fontWeight = FontWeight.Bold)
    }
    }
}

/** O nome do campo do concurso nos Detalhes (desde a 0.33, sem o "(o último)"). */
const val ROTULO_DO_CONCURSO = "Concurso"

/** O botão embaixo do campo do concurso (desde a 0.33). */
const val IR_AO_CONCURSO = "Ir"

const val CONCURSO_ANTERIOR = "Concurso anterior"
const val PROXIMO_CONCURSO = "Próximo concurso"
const val CAMPO_DO_CONCURSO = "Número do concurso"

/** O quadro do resultado enquanto a Caixa responde (um concurso escolhido nos Detalhes). */
@Composable
private fun CartaoConsultando() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("  Consultando a Caixa...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** O quadro do resultado quando a Caixa não respondeu (um concurso escolhido nos Detalhes), com o "Tentar de novo". */
@Composable
private fun CartaoErroDoConcurso(texto: String, aoTentar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = aoTentar) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(TENTAR_DE_NOVO)
            }
        }
    }
}

/**
 * O resultado de um concurso (o último, ou o escolhido nos Detalhes, desde a 0.28, com [oUltimo] falso): se
 * acumulou (ou se saiu o prêmio principal), os números sorteados e a premiação.
 */
@Composable
private fun UltimoResultado(m: Modalidade, u: Concurso?, atualizando: Boolean, oUltimo: Boolean = true) {
    val hoje = LocalDate.now()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (u == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (atualizando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        if (atualizando) "  Consultando a Caixa..." else "Sem dados da Caixa agora.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                return@Column
            }
            val principal = u.faixas.minByOrNull { it.faixa }
            when {
                u.acumulado -> Text("Acumulou!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                principal != null && principal.ganhadores > 0 -> Text("Saiu o prêmio principal!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                else -> Text(if (oUltimo) "Último resultado" else "Resultado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "Concurso ${u.numero}" + (u.data?.let { " · ${Datas.relativa(it, hoje)}" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
            )
            // As cores oficiais das bolas (desde a 0.29: a Timemania amarela com o número verde).
            val cor = coresDaBola(m)
            if (m == Modalidade.DUPLA_SENA && u.segundoSorteio.isNotEmpty()) {
                // Desde a 0.27: cada sorteio com os números e a premiação dele (as faixas 1 a 4 são do 1º; 5 a 8, do 2º).
                SorteioDaDupla(m, "1º sorteio", u.dezenas.map(m::rotulo), u.faixas.filter { it.faixa <= 4 }, cor)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SorteioDaDupla(m, "2º sorteio", u.segundoSorteio.map(m::rotulo), u.faixas.filter { it.faixa > 4 }, cor)
            } else {
                Resultado(m, u, cor)
                if (u.faixas.isNotEmpty()) {
                    HorizontalDivider()
                    Premiacao(m, u.faixas)
                }
            }
        }
    }
}

/** Um sorteio da Dupla Sena nos Detalhes: o título, os números e a premiação dele. */
@Composable
private fun SorteioDaDupla(m: Modalidade, titulo: String, numeros: List<String>, faixas: List<FaixaPremio>, cor: CoresDaBola) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Bolas(numeros, cor)
        if (faixas.isNotEmpty()) Premiacao(m, faixas)
    }
}

/**
 * O nome da faixa na premiação: o da Caixa ("5 acertos") e, na +Milionária (desde a 0.32), a nomenclatura curta
 * ("6 + 2", "6 + 0/1"...), pela posição da faixa.
 */
fun nomeNaPremiacao(m: Modalidade, f: FaixaPremio): String =
    (if (m == Modalidade.MAIS_MILIONARIA) Probabilidades.FAIXAS_DA_MAIS_MILIONARIA.getOrNull(f.faixa - 1) else null)
        ?: f.descricao.ifBlank { "${f.faixa}ª faixa" }

/** "Não houve ganhadores", "1 aposta ganhadora", "1.234 apostas ganhadoras". */
fun textoDosGanhadores(ganhadores: Int): String = when (ganhadores) {
    0 -> "Não houve ganhadores"
    1 -> "1 aposta ganhadora"
    else -> "${Dinheiro.formatar(ganhadores * 100L).substringBefore(",")} apostas ganhadoras"
}

/**
 * "Premiação" e cada faixa: o nome, o valor e quantas apostas ganharam. Desde a 0.32 cada faixa de premiação é
 * literalmente uma faixa (pedido do usuário): uma tira na cor da modalidade, mais forte na primeira e mais clara nas
 * de baixo, com a ponta da esquerda cheia. Na +Milionária, os nomes curtos ("6 + 0/1") e a legenda deles no fim.
 */
@Composable
private fun Premiacao(m: Modalidade, faixas: List<FaixaPremio>) {
    val cor = Color(Tarjas.destaque(m))
    val ordenadas = faixas.sortedBy { it.faixa }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Premiação", style = MaterialTheme.typography.titleMedium)
        ordenadas.forEachIndexed { i, f ->
            // Da primeira faixa (mais forte) para a última (mais clara).
            val forca = if (ordenadas.size <= 1) 1f else 1f - i.toFloat() / (ordenadas.size - 1)
            FaixaDaPremiacao(nomeNaPremiacao(m, f), f, cor, forca)
        }
        if (m == Modalidade.MAIS_MILIONARIA) {
            Text(
                "* ${Probabilidades.LEGENDA_DA_MAIS_MILIONARIA}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Uma faixa da premiação (desde a 0.32): a tira colorida com o nome, o valor e os ganhadores. [forca] de 0 a 1. */
@Composable
private fun FaixaDaPremiacao(nome: String, f: FaixaPremio, cor: Color, forca: Float) {
    val ponta = with(LocalDensity.current) { PONTA_DA_FAIXA.toPx() }
    // No tema escuro, a cor clareada (o azul da Quina e da +Milionária quase sumia no fundo escuro).
    val escuro = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tom = if (escuro) lerp(cor, Color.White, 0.45f) else cor
    val fundo = tom.copy(alpha = (if (escuro) 0.12f else 0.10f) + 0.16f * forca)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(fundo)
            .drawBehind { drawRect(tom, size = Size(ponta, size.height)) }
            .padding(start = PONTA_DA_FAIXA + 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                nome + if (f.ganhadores > 0 && f.valor > 0) " · ${valorSemQuebra(Dinheiro.rs(f.valor))}" else "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(textoDosGanhadores(f.ganhadores), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A ponta cheia (na cor da modalidade) de cada faixa da premiação. */
private val PONTA_DA_FAIXA = 6.dp

/**
 * As regras resumidas (desde a 0.27, no formato pedido pelo usuário): Volante ("De 01 a 60"), Aposta ("De 6 a 20
 * números"), Sorteio (quantos números) e Premiação (a partir de quantos acertos). Com as apostas com mais números
 * ([mais]), a aposta vai até o máximo da modalidade. Desde a 0.29 as faixas começam com "De" (até a 0.28, "01 a
 * 60", "6 a 20 números"). Desde a 0.32 sem o preço (pedido do usuário: ele fica no cartão "Preço"); [estado]
 * ficou para quem já chamava assim.
 */
@Suppress("UNUSED_PARAMETER")
fun regras(m: Modalidade, estado: Estado, mais: Boolean): List<Pair<String, String>> {
    val comMais = mais && Volante.aceitaMais(m)
    // Desde a 0.29: "De 6 a 20 números" (até a 0.28, "6 a 20 números").
    fun faixa(de: Int, ate: Int, nome: String) = if (ate > de) "De $de a $ate $nome" else "$de $nome"
    return when (m.formato) {
        Formato.NUMEROS -> {
            val numeros = faixa(m.qtdAposta, if (comMais) Volante.maximo(m) else m.qtdAposta, "números")
            listOf(
                "Volante" to "De ${m.rotulo(m.menor)} a ${m.rotulo(m.maior)}" + if (m.trevos > 0) " e trevos de 1 a 6" else "",
                "Aposta" to numeros + when {
                    m.trevos > 0 -> " e " + faixa(m.trevos, if (comMais) Volante.maximoTrevos(m) else m.trevos, "trevos").replaceFirst("De ", "de ")
                    m.extra == Extra.TIME_DO_CORACAO -> " e o Time do Coração"
                    m.extra == Extra.MES_DE_SORTE -> " e o Mês de Sorte"
                    else -> ""
                },
                "Sorteio" to (if (m.sorteios > 1) "${m.sorteios} sorteios de ${m.qtdSorteada} números" else "${m.qtdSorteada} números") + when {
                    m.trevos > 0 -> " e ${m.trevos} trevos"
                    m.extra == Extra.TIME_DO_CORACAO -> " e o Time do Coração"
                    m.extra == Extra.MES_DE_SORTE -> " e o Mês de Sorte"
                    else -> ""
                },
                "Premiação" to premiacao(m),
            )
        }
        Formato.COLUNAS -> listOf(
            "Volante" to "7 colunas, de 0 a 9",
            "Aposta" to "7 números (1 por coluna)",
            "Sorteio" to "7 números (1 por coluna)",
            "Premiação" to premiacao(m),
        )
        Formato.LOTECA -> listOf(
            "Volante" to "14 jogos de futebol",
            "Aposta" to "14 palpites (1, X ou 2)",
            "Sorteio" to "Os resultados dos 14 jogos",
            "Premiação" to premiacao(m),
        )
        Formato.FEDERAL -> listOf("Bilhete" to "5 algarismos, inteiro ou em frações", "Premiação" to premiacao(m))
    }
}

/** A partir de quantos acertos a aposta é premiada. */
private fun premiacao(m: Modalidade): String = when (m) {
    Modalidade.MEGA_SENA -> "A partir de 4 números"
    Modalidade.LOTOFACIL -> "A partir de 11 números"
    Modalidade.QUINA -> "A partir de 2 números"
    Modalidade.LOTOMANIA -> "A partir de 15 números, ou nenhum"
    Modalidade.TIMEMANIA -> "A partir de 3 números, ou o Time do Coração"
    Modalidade.DUPLA_SENA -> "A partir de 3 números"
    Modalidade.DIA_DE_SORTE -> "A partir de 4 números, ou o Mês de Sorte"
    Modalidade.SUPER_SETE -> "A partir de 3 colunas"
    Modalidade.MAIS_MILIONARIA -> "A partir de 2 números e 1 trevo"
    Modalidade.LOTECA -> "A partir de 13 jogos"
    Modalidade.FEDERAL -> "5 prêmios, aproximações e finais"
}

/** "Sorteio: Hoje": o rótulo e o valor na mesma cor (o valor em negrito). */
@Composable
private fun RotuloValor(rotulo: String, valor: String) {
    Text(
        buildAnnotatedString {
            append("$rotulo: ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(valor) }
        },
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bolas(numeros: List<String>, cor: CoresDaBola) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        numeros.forEach { n ->
            // Os números sorteados em bolas 3D (só os números: as modalidades usam as tarjas desde a 0.27).
            Box(
                Modifier.size(38.dp).bola3D(cor.fundo).let { if (cor.comContorno) it.border(1.5.dp, cor.contorno, CircleShape) else it },
                contentAlignment = Alignment.Center,
            ) {
                Text(n, style = estiloNaBola(38.dp, cor.numero), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Números sorteados do último concurso, no formato de cada modalidade. */
@Composable
private fun Resultado(m: Modalidade, c: Concurso, cor: CoresDaBola) {
    when (m.formato) {
        Formato.NUMEROS -> {
            Bolas(c.dezenas.map(m::rotulo), cor)
            if (c.trevos.isNotEmpty()) {
                Text("Trevos", style = MaterialTheme.typography.labelLarge)
                Bolas(c.trevos.map { it.toString() }, coresDaBola(Tarjas.trevo(m)))
            }
            c.timeOuMes?.let {
                Text((if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte: " else "Time do Coração: ") + it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Formato.COLUNAS -> Bolas(c.dezenas.map { it.toString() }, cor)
        Formato.LOTECA -> c.jogosLoteca.forEach { j ->
            Text(
                "${j.ordem}. ${j.time1} ${j.gols1 ?: "-"} x ${j.gols2 ?: "-"} ${j.time2}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Formato.FEDERAL -> c.bilhetesFederal.forEachIndexed { i, b ->
            Text("${i + 1}º prêmio: $b", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

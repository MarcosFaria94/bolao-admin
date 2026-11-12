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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
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
import io.github.marcosfaria94.bolao.ui.comum.TituloDestaque
import io.github.marcosfaria94.bolao.ui.comum.bola3D
import io.github.marcosfaria94.bolao.ui.comum.estiloNaBola
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
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
    val emDestaque = doProximo && indices.destaque(itens)?.modalidade == m
    // O próximo concurso (da Caixa, guardado no celular) já foi sorteado e o resultado ainda não chegou.
    val situacao = indices.situacao(m, Instant.now(), itens, consultas)
    val avisoDoQuadro = situacao.sorteio?.takeIf { doProximo && situacao.sorteado }?.let {
        "Sorteado ${AgendaDaCaixa.quando(it, Instant.now())}: aguardando o resultado da Caixa."
    }

    // As duas páginas ficam montadas: ir aos Detalhes e voltar não perde nada da página do jogo.
    Box(Modifier.fillMaxSize().nestedScroll(puxao)) {
    HorizontalPager(state = paginas, beyondViewportPageCount = 1, flingBehavior = soltarDasPaginas(paginas), modifier = Modifier.fillMaxSize().acompanharPuxao(puxao)) { pagina ->
        if (pagina == 1) {
            DetalhesDaModalidade(estado, m, u, atualizando)
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
 * O prêmio (de ponta a ponta) e o dia do sorteio, juntos num quadro; o concurso em destaque tem o
 * contorno dourado e a estrelinha.
 */
@Composable
private fun QuadroPremio(
    premio: Long?,
    sorteio: LocalDate?,
    hoje: LocalDate,
    federal: Boolean,
    destaque: Boolean,
    consultando: Boolean,
    aviso: String? = null,
    hora: LocalTime? = null,
    acumulados: List<Pair<String, Long>> = emptyList(),
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = if (destaque) BorderStroke(2.dp, Dourado) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (destaque) {
                TituloDestaque()
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
 */
@Composable
private fun DetalhesDaModalidade(estado: Estado, m: Modalidade, u: Concurso?, atualizando: Boolean) {
    val mais = estado.parametros.maisNumeros && Volante.aceitaMais(m)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UltimoResultado(m, u, atualizando)
        CartaoQueAbre(TITULO_REGRAS) {
            regras(m, estado, mais).forEach { (r, v) -> LinhaValor(r, v) }
        }
        CartaoQueAbre(TITULO_PROBABILIDADES) { TabelaDeProbabilidades(m, estado, mais) }
        if (mais && Probabilidades.gruposDePremios(m) != null) {
            CartaoQueAbre(TITULO_QUANTIDADE) { TabelaDeQuantidade(m) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

const val TITULO_REGRAS = "Regras"
const val TITULO_PROBABILIDADES = "Probabilidades"
const val TITULO_QUANTIDADE = "Quantidade de prêmios"

/** A tabela de probabilidades, como a da Caixa: números jogados, valor da aposta e "1 em X" de cada faixa. */
@Composable
private fun TabelaDeProbabilidades(m: Modalidade, estado: Estado, mais: Boolean) {
    val preco = estado.config.preco(m)
    val linhas = remember(m, mais) { Probabilidades.tabela(m, mais) }
    val colunas = Probabilidades.colunas(m)
    TabelaRolavel(
        primeira = if (m.trevos > 0) "Números + trevos" else "Números",
        grupos = listOf("" to 1, "Probabilidade de acerto (1 em)" to colunas.size),
        colunas = listOf("Valor da aposta") + colunas,
        linhas = linhas.map { l ->
            val rotulo = if (m.trevos > 0) "${l.numeros} + ${l.trevos}" else if (m.formato == Formato.COLUNAS) "7" else "${l.numeros}"
            rotulo to (listOf(valorSemQuebra(Dinheiro.rs(preco * l.apostasSimples))) + l.umEm.map { v -> v?.let { Probabilidades.milhares(it) } ?: "—" })
        },
    )
    val notas = Probabilidades.observacoesDaTabela(m) + if (!mais && Volante.aceitaMais(m)) {
        listOf("Com as apostas com mais números ligadas (Ajustes > Parâmetros), a tabela mostra também as apostas com mais números.")
    } else {
        emptyList()
    }
    notas.forEach { obs ->
        Text("* $obs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

/**
 * A tabela "Quantidade de prêmios a receber acertando", como a da Caixa (só com as apostas com mais números):
 * para cada quantidade de números jogados, quantos prêmios de cada faixa a aposta recebe acertando 6, 5 ou 4
 * dos números sorteados (na Mega-Sena).
 */
@Composable
private fun TabelaDeQuantidade(m: Modalidade) {
    val grupos = Probabilidades.gruposDePremios(m) ?: return
    val linhas = remember(m) {
        (m.qtdAposta..Volante.maximo(m)).map { n -> "$n" to Probabilidades.quantidadeDePremios(m, n).flatten().map { Probabilidades.milhares(it) } }
    }
    TabelaRolavel(
        primeira = "Números",
        grupos = grupos.map { g -> "Acertando ${g.acertando}" to g.faixas.size },
        colunas = grupos.flatMap { g -> g.faixas.map { Probabilidades.nomeDaFaixa(m, it) } },
        linhas = linhas,
    )
    val nota = when (m) {
        Modalidade.DUPLA_SENA -> "Em cada um dos 2 sorteios."
        Modalidade.DIA_DE_SORTE -> "Sem contar o Mês de Sorte."
        else -> null
    }
    nota?.let {
        Text("* $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

/** O último concurso: se acumulou (ou se saiu o prêmio principal), os números sorteados e a premiação. */
@Composable
private fun UltimoResultado(m: Modalidade, u: Concurso?, atualizando: Boolean) {
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
                else -> Text("Último resultado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "Concurso ${u.numero}" + (u.data?.let { " · ${Datas.relativa(it, hoje)}" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
            )
            val cor = corModalidade(m.cor)
            if (m == Modalidade.DUPLA_SENA && u.segundoSorteio.isNotEmpty()) {
                // Desde a 0.27: cada sorteio com os números e a premiação dele (as faixas 1 a 4 são do 1º; 5 a 8, do 2º).
                SorteioDaDupla("1º sorteio", u.dezenas.map(m::rotulo), u.faixas.filter { it.faixa <= 4 }, cor)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SorteioDaDupla("2º sorteio", u.segundoSorteio.map(m::rotulo), u.faixas.filter { it.faixa > 4 }, cor)
            } else {
                Resultado(m, u, cor)
                if (u.faixas.isNotEmpty()) {
                    HorizontalDivider()
                    Premiacao(u.faixas)
                }
            }
        }
    }
}

/** Um sorteio da Dupla Sena nos Detalhes: o título, os números e a premiação dele. */
@Composable
private fun SorteioDaDupla(titulo: String, numeros: List<String>, faixas: List<FaixaPremio>, cor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Bolas(numeros, cor)
        if (faixas.isNotEmpty()) Premiacao(faixas)
    }
}

/** "Premiação" e cada faixa: o nome, o valor e quantas apostas ganharam. */
@Composable
private fun Premiacao(faixas: List<FaixaPremio>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Premiação", style = MaterialTheme.typography.titleMedium)
        faixas.sortedBy { it.faixa }.forEach { f ->
            Column {
                Text(
                    f.descricao.ifBlank { "${f.faixa}ª faixa" } + if (f.ganhadores > 0 && f.valor > 0) " · ${Dinheiro.rs(f.valor)}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when (f.ganhadores) {
                        0 -> "Não houve ganhadores"
                        1 -> "1 aposta ganhadora"
                        else -> "${Dinheiro.formatar(f.ganhadores * 100L).substringBefore(",")} apostas ganhadoras"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * As regras resumidas (desde a 0.27, no formato pedido pelo usuário): Volante, Aposta (de quantos a quantos
 * números), Preço (de quanto a quanto), Sorteio (quantos números) e Premiação (a partir de quantos acertos).
 * Com as apostas com mais números ([mais]), a aposta e o preço vão até o máximo da modalidade.
 */
fun regras(m: Modalidade, estado: Estado, mais: Boolean): List<Pair<String, String>> {
    val simples = estado.config.preco(m)
    val comMais = mais && Volante.aceitaMais(m)
    val maior = if (comMais) simples * Volante.apostasSimples(m, Volante.maximo(m), Volante.maximoTrevos(m)) else simples
    val preco = when {
        m.precoVariavel -> "Variável"
        maior > simples -> "${Dinheiro.rs(simples)} a ${Dinheiro.rs(maior)}"
        else -> Dinheiro.rs(simples)
    }
    fun faixa(de: Int, ate: Int, nome: String) = if (ate > de) "$de a $ate $nome" else "$de $nome"
    return when (m.formato) {
        Formato.NUMEROS -> {
            val numeros = faixa(m.qtdAposta, if (comMais) Volante.maximo(m) else m.qtdAposta, "números")
            listOf(
                "Volante" to "${m.rotulo(m.menor)} a ${m.rotulo(m.maior)}" + if (m.trevos > 0) " e trevos de 1 a 6" else "",
                "Aposta" to numeros + when {
                    m.trevos > 0 -> " e " + faixa(m.trevos, if (comMais) Volante.maximoTrevos(m) else m.trevos, "trevos")
                    m.extra == Extra.TIME_DO_CORACAO -> " e o Time do Coração"
                    m.extra == Extra.MES_DE_SORTE -> " e o Mês de Sorte"
                    else -> ""
                },
                "Preço" to preco,
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
            "Volante" to "7 colunas de 0 a 9",
            "Aposta" to "7 números (1 por coluna)",
            "Preço" to preco,
            "Sorteio" to "7 números (1 por coluna)",
            "Premiação" to premiacao(m),
        )
        Formato.LOTECA -> listOf(
            "Volante" to "14 jogos de futebol",
            "Aposta" to "14 palpites (1, X ou 2)",
            "Preço" to preco,
            "Sorteio" to "Os resultados dos 14 jogos",
            "Premiação" to premiacao(m),
        )
        Formato.FEDERAL -> listOf("Bilhete" to "5 algarismos, inteiro ou em frações", "Preço" to preco, "Premiação" to premiacao(m))
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
private fun Bolas(numeros: List<String>, cor: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        numeros.forEach { n ->
            // Os números sorteados em bolas 3D (só os números: as modalidades usam as tarjas desde a 0.27).
            Box(
                Modifier.size(38.dp).bola3D(cor),
                contentAlignment = Alignment.Center,
            ) {
                Text(n, style = estiloNaBola(38.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Números sorteados do último concurso, no formato de cada modalidade. */
@Composable
private fun Resultado(m: Modalidade, c: Concurso, cor: Color) {
    when (m.formato) {
        Formato.NUMEROS -> {
            Bolas(c.dezenas.map(m::rotulo), cor)
            if (c.trevos.isNotEmpty()) {
                Text("Trevos", style = MaterialTheme.typography.labelLarge)
                Bolas(c.trevos.map { it.toString() }, cor)
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

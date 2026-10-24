package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.ui.aposta.ApostaEmEdicao
import io.github.marcosfaria94.bolao.ui.aposta.SecaoApostaRegistrada
import io.github.marcosfaria94.bolao.ui.aposta.SecaoVolante
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.aposta.rascunhoDa
import io.github.marcosfaria94.bolao.ui.comum.BarraTitulo
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.LinhaValor
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.TituloDestaque
import io.github.marcosfaria94.bolao.ui.comum.bola3D
import io.github.marcosfaria94.bolao.ui.comum.estiloNaBola
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import java.time.LocalDate

/**
 * Página do jogo (tocar numa modalidade em Jogos): o emblema, o nome e a chance do prêmio
 * principal com o botão "Detalhes" ao lado; o prêmio e o dia do sorteio num quadro; e, embaixo, o
 * volante da aposta (do "Aposta Nº1" até "Descartar rascunho"), com a aposta em andamento da
 * modalidade ou uma nova. "Detalhes" (último resultado, regras e probabilidades) abre por cima e,
 * ao fechar, a página volta como estava, sem mexer na aposta.
 *
 * [apostaId]: a aposta aberta pelo Histórico ou por um aviso. Registrada, ela aparece no lugar do
 * volante (com a apuração e os comprovantes); em andamento, ela é a do volante.
 */
@Composable
fun ModalidadeTela(estado: Estado, m: Modalidade, aoSair: () -> Unit, apostaId: String? = null) {
    val app = LocalContext.current.bolao
    val indices = remember { app.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { indices.atualizar() }
    val item: Indices.Item? = itens[m]
    val u = item?.ultimo
    val hoje = LocalDate.now()
    var detalhes by rememberSaveable { mutableStateOf(false) }

    val aberta = apostaId?.let { id -> estado.apostas.firstOrNull { it.id == id } }
    if (apostaId != null && aberta == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não existe mais.") }
        return
    }
    // O volante: a aposta em andamento (a aberta ou a da modalidade) ou uma nova, com o volante vazio.
    // Fica no modelo da página: ver os Detalhes não descarta nada.
    val vm: ApostaEmEdicao? = if (aberta?.registrada == true) {
        null
    } else {
        viewModel(key = "volante") {
            val emAndamento = aberta ?: rascunhoDa(estado, m)
            ApostaEmEdicao(
                app.repo,
                app.escopo,
                emAndamento ?: novaAposta(estado, m, app.indices.itens.value[m]?.ultimo, hoje, System.currentTimeMillis()),
                nova = emAndamento == null,
            )
        }
    }
    // Registrada (a aberta, ou a do volante, logo depois do registro): a página mostra a aposta registrada.
    val registrada: Aposta? = aberta?.takeIf { it.registrada }
        ?: vm?.let { v -> estado.apostas.firstOrNull { it.id == v.aposta.id && it.registrada } }
    val aposta: Aposta = registrada ?: vm!!.aposta

    // O quadro mostra o concurso da aposta; sem os dados dela, os do próximo concurso (da Caixa).
    val doProximo = registrada == null && (aposta.concurso == 0 || aposta.concurso == item?.proximoNumero)
    val premio = aposta.premioEstimado?.takeIf { it > 0 } ?: u?.premioEstimadoProximo?.takeIf { doProximo && it > 0 }
    val sorteio = aposta.dataSorteio ?: u?.proximaData?.takeIf { doProximo }
    val emDestaque = doProximo && indices.destaque(itens)?.modalidade == m

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CabecalhoModalidade(m, aposta.concurso.takeIf { it > 0 } ?: item?.proximoNumero, aoDetalhes = { detalhes = true })
        QuadroPremio(
            premio = premio,
            sorteio = sorteio,
            hoje = hoje,
            federal = m == Modalidade.FEDERAL,
            destaque = emDestaque,
            consultando = premio == null && sorteio == null && atualizando,
        )
        if (registrada != null) {
            SecaoApostaRegistrada(estado, registrada)
        } else {
            val v = vm!!
            // A aposta mudada (ou descartada) numa tela aberta por cima desta: a página acompanha.
            val gravada = estado.apostas.firstOrNull { it.id == v.aposta.id }
            LaunchedEffect(gravada) { if (v.conferirGravada(gravada)) aoSair() }
            SecaoVolante(
                estado = estado,
                r = v.aposta,
                mudar = v::mudar,
                aoDescartar = {
                    v.descartar()
                    aoSair()
                },
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    if (detalhes) DetalhesDaModalidade(estado, m, u, atualizando, aoFechar = { detalhes = false })
}

/** Emblema (sigla e concurso), o nome bem grande e a chance do prêmio principal, com o "Detalhes" ao lado. */
@Composable
private fun CabecalhoModalidade(m: Modalidade, concurso: Int?, aoDetalhes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmblemaModalidade(m, concurso, tamanho = 68.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            TextoQueCabe(
                m.nome,
                MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 40.sp),
                peso = FontWeight.Bold,
                menor = 16.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Probabilidades.faixas(m).firstOrNull()?.let {
                Text(Probabilidades.texto(it.umEm), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = aoDetalhes,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        ) { Text("Detalhes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    }
}

/**
 * O prêmio (de ponta a ponta) e o dia do sorteio, juntos num quadro; o concurso em destaque tem o
 * contorno dourado e a estrelinha.
 */
@Composable
private fun QuadroPremio(premio: Long?, sorteio: LocalDate?, hoje: LocalDate, federal: Boolean, destaque: Boolean, consultando: Boolean) {
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
            TextoQueCabe(
                premio?.let { Dinheiro.porExtenso(it) } ?: "Prêmio a confirmar",
                MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 70.sp),
                peso = FontWeight.Bold,
                menor = 24.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            RotuloValor("Sorteio", sorteio?.let { Datas.relativa(it, hoje) } ?: "a confirmar")
        }
    }
}

/**
 * "Detalhes" do jogo, numa tela que abre por cima da página: o último resultado (se acumulou, os
 * números sorteados e a premiação), as regras e as probabilidades. Fechar volta para a página do
 * jogo como ela estava (o volante não muda).
 */
@Composable
private fun DetalhesDaModalidade(estado: Estado, m: Modalidade, u: Concurso?, atualizando: Boolean, aoFechar: () -> Unit) {
    Dialog(
        onDismissRequest = aoFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                BarraTitulo("Detalhes", aoVoltar = aoFechar)
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    UltimoResultado(m, u, atualizando)

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Regras", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            regras(m, estado).forEach { (r, v) -> LinhaValor(r, v) }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Probabilidades", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Probabilidades.faixas(m).forEach { f -> LinhaValor(f.nome, Probabilidades.texto(f.umEm)) }
                            Probabilidades.faixas(m).mapNotNull { it.observacao }.distinct().forEach { obs ->
                                Text("* $obs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
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
            Resultado(m, u, corModalidade(m.cor))
            if (u.faixas.isNotEmpty()) {
                HorizontalDivider()
                Text("Premiação", style = MaterialTheme.typography.titleMedium)
                u.faixas.sortedBy { it.faixa }.forEach { f ->
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
    }
}

/** Como funciona a aposta simples (regras resumidas). */
private fun regras(m: Modalidade, estado: Estado): List<Pair<String, String>> {
    val preco = "Preço" to if (m.precoVariavel) "variável" else Dinheiro.rs(estado.config.preco(m))
    return when (m.formato) {
        Formato.NUMEROS -> listOf(
            "Volante" to "${m.rotulo(m.menor)} a ${m.rotulo(m.maior)}" + if (m.trevos > 0) " + trevos de 1 a 6" else "",
            "Aposta simples" to "${m.qtdAposta} dezenas" + when {
                m.trevos > 0 -> " + ${m.trevos} trevos"
                m.extra == Extra.TIME_DO_CORACAO -> " + Time do Coração"
                m.extra == Extra.MES_DE_SORTE -> " + Mês de Sorte"
                else -> ""
            },
            "Sorteio" to if (m.sorteios > 1) "${m.sorteios} sorteios de ${m.qtdSorteada} dezenas" else "${m.qtdSorteada} dezenas",
            preco,
            "Premiação" to premiacao(m),
        )
        Formato.COLUNAS -> listOf("Volante" to "7 colunas de 0 a 9", "Aposta simples" to "1 número por coluna", preco, "Premiação" to premiacao(m))
        Formato.LOTECA -> listOf("Volante" to "14 jogos de futebol", "Aposta simples" to "1 palpite por jogo (1, X ou 2)", preco, "Premiação" to premiacao(m))
        Formato.FEDERAL -> listOf("Bilhete" to "5 algarismos, inteiro ou em frações", preco, "Premiação" to premiacao(m))
    }
}

private fun premiacao(m: Modalidade): String = when (m) {
    Modalidade.MEGA_SENA -> "Acertar 4, 5 ou 6"
    Modalidade.LOTOFACIL -> "Acertar 11 a 15"
    Modalidade.QUINA -> "Acertar 2 a 5"
    Modalidade.LOTOMANIA -> "Acertar 15 a 20, ou nenhum"
    Modalidade.TIMEMANIA -> "Acertar 3 a 7, ou o Time do Coração"
    Modalidade.DUPLA_SENA -> "Acertar 3 a 6 em qualquer sorteio"
    Modalidade.DIA_DE_SORTE -> "Acertar 4 a 7, ou o Mês de Sorte"
    Modalidade.SUPER_SETE -> "Acertar 3 a 7 colunas"
    Modalidade.MAIS_MILIONARIA -> "A partir de 2 números + 1 trevo"
    Modalidade.LOTECA -> "Acertar 13 ou 14 jogos"
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
            // As bolas sorteadas no mesmo estilo 3D dos emblemas.
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
            if (m == Modalidade.DUPLA_SENA && c.segundoSorteio.isNotEmpty()) {
                Text("1º sorteio", style = MaterialTheme.typography.labelLarge)
                Bolas(c.dezenas.map(m::rotulo), cor)
                Text("2º sorteio", style = MaterialTheme.typography.labelLarge)
                Bolas(c.segundoSorteio.map(m::rotulo), cor)
            } else {
                Bolas(c.dezenas.map(m::rotulo), cor)
            }
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

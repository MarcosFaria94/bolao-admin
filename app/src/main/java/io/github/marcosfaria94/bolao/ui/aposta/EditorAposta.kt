package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.Animacoes
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.CampoInteiro
import io.github.marcosfaria94.bolao.ui.comum.DialogoSimNao
import io.github.marcosfaria94.bolao.ui.comum.EtiquetaDePreco
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import io.github.marcosfaria94.bolao.data.Favorito
import io.github.marcosfaria94.bolao.data.Favoritos
import io.github.marcosfaria94.bolao.ui.painel.DezenasDoJogo
import io.github.marcosfaria94.bolao.ui.tema.Dourado

/**
 * Título do volante: "Aposta Nº1" (o número que a aposta vai receber no mês) e, embaixo (desde a 0.22), o
 * que falta marcar ([textoDoVolante]: "Marque 5 números", "Aposta completa"...), em verde quando o volante
 * está pronto. Desde a 0.27, na mesma linha do "Aposta Nº1", no canto direito, o preço da aposta ([preco],
 * que acompanha os números a mais); o lápis dos dados do concurso saiu (o app pega tudo da Caixa).
 */
@Composable
private fun TituloDoVolante(numero: Int, texto: String, pronto: Boolean, preco: String, carregando: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                "Aposta Nº$numero",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
            )
            if (texto.isNotEmpty()) {
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                    fontWeight = if (pronto) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (pronto) corEntrada() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (carregando) {
            CircularProgressIndicator(Modifier.padding(top = 4.dp).size(18.dp), strokeWidth = 2.dp)
        }
        EtiquetaDePreco(valorSemQuebra(preco), Modifier.padding(start = 8.dp))
    }
}

/** "1 número" ou "5 números". */
private fun quantos(n: Int, um: String, varios: String): String = if (n == 1) "1 $um" else "$n $varios"

/** O volante pronto, sem mais números. */
const val APOSTA_COMPLETA = "Aposta completa"

/**
 * O texto embaixo de "Aposta Nº X" (desde a 0.22): o que falta marcar e, com tudo marcado, que a
 * aposta está pronta. Faltando números: "Marque 2 números" (o mínimo da modalidade menos os já
 * marcados); com as apostas com mais números ligadas ([mais]), "Marque no mínimo 2 números". Depois
 * dos números, os trevos, o Time do Coração ou o Mês de Sorte. Pronta: "Aposta completa" ou, com
 * [mais], "Aposta com 7 números". A Federal não tem volante: vazio.
 */
fun textoDoVolante(m: Modalidade, p: Palpite, mais: Boolean): String {
    val marque = if (mais) "Marque no mínimo " else "Marque "
    val faltam = Volante.faltam(m, p)
    return when {
        m.formato == Formato.FEDERAL -> ""
        faltam > 0 && m.formato == Formato.LOTECA -> "Marque " + quantos(faltam, "palpite", "palpites")
        faltam > 0 -> marque + quantos(faltam, "número", "números")
        m.trevos > 0 && p.trevos.size < m.trevos -> marque + quantos(m.trevos - p.trevos.size, "trevo", "trevos")
        m.extra == Extra.TIME_DO_CORACAO && p.extra.isNullOrBlank() -> "Escolha o Time do Coração"
        m.extra == Extra.MES_DE_SORTE && p.extra.isNullOrBlank() -> "Escolha o Mês de Sorte"
        mais && m.formato == Formato.NUMEROS -> "Aposta com ${p.numeros.size} números" +
            if (m.trevos > 0 && p.trevos.size > m.trevos) " e ${p.trevos.size} trevos" else ""
        else -> APOSTA_COMPLETA
    }
}

/**
 * O valor em dinheiro não se parte no fim da linha: "(R$ 118,69)" vai inteiro para a linha de baixo
 * (até a 0.18 ficava "(R$" numa linha e "118,69)" na outra). Só na tela: as mensagens e os
 * comprovantes continuam com o espaço normal.
 */
fun valorSemQuebra(texto: String): String = texto.replace("R$ ", "R$\u00A0")

/** Linha "Rótulo: valor" alinhada, como no esboço da tela. */
@Composable
fun LinhaInfo(rotulo: String, valor: String, destaque: Boolean = false, cor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("$rotulo: ", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valorSemQuebra(valor),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.SemiBold,
            color = cor,
        )
    }
}

private sealed interface Consulta {
    data object Nada : Consulta
    data object Carregando : Consulta
    data class Ok(val texto: String) : Consulta
    data class Erro(val texto: String) : Consulta
}

/** O próximo concurso da modalidade, como a Caixa informa (na Loteca, com os 14 jogos). */
private data class DadosConcurso(val numero: Int, val sorteio: LocalDate?, val premio: Long?, val jogosLoteca: List<String>?)

private suspend fun dadosNaCaixa(m: Modalidade): DadosConcurso {
    val c = CaixaApi.ultimo(m, usarCache = false)
    val numero = c.proximoNumero ?: (c.numero + 1)
    val jogos = if (m == Modalidade.LOTECA) {
        runCatching { CaixaApi.concurso(m, numero) }.getOrNull()
            ?.jogosLoteca?.takeIf { it.isNotEmpty() }
            ?.map { "${it.time1} x ${it.time2}" }
    } else {
        null
    }
    return DadosConcurso(numero, c.proximaData, c.premioEstimadoProximo, jogos)
}

private fun Aposta.com(d: DadosConcurso): Aposta = copy(
    concurso = d.numero,
    dataSorteio = d.sorteio ?: dataSorteio,
    premioEstimado = d.premio ?: premioEstimado,
    jogosLoteca = d.jogosLoteca ?: jogosLoteca,
)

/**
 * O volante da página da modalidade, da aposta nova ou em andamento: "Aposta Nº1" com o lápis dos
 * dados do concurso e o que falta marcar, o volante e, no mesmo lugar, o "Completar" (enquanto falta
 * marcar) e o "Registrar aposta" (com o volante pronto), ao lado do "Limpar". Vai dentro da coluna da
 * página (não tem rolagem própria). Ao sair da página, a aposta só fica em andamento com o volante
 * completo ([ficaEmAndamento]); senão é descartada (o aviso fica logo abaixo do "Completar"). Desde a
 * 0.22 não há mais o "Descartar rascunho": para descartar, é limpar o volante e sair.
 *
 * Desde a 0.10 o registro não pede o comprovante da Caixa: ele é anexado depois, na aba
 * Comprovantes (até a conferência, a aposta ainda pode ser descartada). Antes de registrar, o app
 * confere na Caixa se o concurso da aposta ainda é o que está recebendo apostas; se já foi
 * sorteado, a aposta passa para o próximo.
 *
 * Os Parâmetros da sessão (desde a 0.22): sem a janela de confirmação, o registro sai direto; com as
 * apostas com mais números, o volante aceita até o máximo da modalidade e o preço acompanha; com a
 * teimosinha, a opção de repetir a aposta em vários concursos. [bloqueio]: o concurso do volante já foi
 * sorteado e o resultado ainda não chegou ao app; até os dados serem atualizados ([aoAtualizar]), não
 * dá para registrar.
 *
 * Desde a 0.25, registrada a aposta ([aoRegistrar]), a página não vai mais para a aposta registrada:
 * continua no volante, vazio, para fazer mais apostas, e o aviso do comprovante que falta fica no alto
 * ([AvisoDoComprovante]).
 */
@Composable
fun SecaoVolante(
    estado: Estado,
    r: Aposta,
    mudar: (Aposta) -> Unit,
    bloqueio: String? = null,
    atualizando: Boolean = false,
    aoAtualizar: () -> Unit = {},
    aoRegistrar: (registrada: Aposta, feitas: List<Aposta>) -> Unit = { _, _ -> },
) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val m = r.modalidade
    val federal = m.formato == Formato.FEDERAL
    val individual = estado.individual
    val parametros = estado.parametros
    val mais = parametros.maisNumeros && Volante.aceitaMais(m)
    // Sem a teimosinha no concurso especial (desde a 0.28).
    val comTeimosinha = parametros.teimosinha && Operacoes.aceitaTeimosinha(r)
    val hoje = LocalDate.now()
    // Nas respostas que chegam depois (Caixa, arquivo escolhido), vale a aposta como está agora.
    val atual by rememberUpdatedState(r)
    val mudarAtual by rememberUpdatedState(mudar)
    val precoSimples = estado.config.preco(m)
    fun mudarPalpite(p: Palpite) {
        // Com mais (ou menos) números, o preço acompanha as apostas simples que cabem no palpite.
        val antes = Volante.apostasSimples(m, atual.numeros.size, atual.trevos.size)
        val depois = Volante.apostasSimples(m, p.numeros.size, p.trevos.size)
        val valor = if (m.formato == Formato.NUMEROS && antes != depois) precoSimples * depois else atual.valor
        mudarAtual(atual.copy(numeros = p.numeros, trevos = p.trevos, extra = p.extra, palpitesLoteca = p.loteca, valor = valor))
    }

    var consulta by remember { mutableStateOf<Consulta>(Consulta.Nada) }
    var confirmar by remember { mutableStateOf<String?>(null) }
    var conferindo by remember { mutableStateOf(false) }
    var registrando by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }
    // Os jogos favoritos da modalidade que cabem no volante (desde a 0.28).
    val favoritos = Favoritos.paraOVolante(estado, m)
    var escolhendoFavorito by remember { mutableStateOf(false) }
    if (escolhendoFavorito) {
        EscolhaDoFavorito(estado, favoritos, aoEscolher = { f ->
            escolhendoFavorito = false
            mudarPalpite(f.palpite)
        }, aoFechar = { escolhendoFavorito = false })
    }

    // Sem o número do concurso (ou com um que já foi sorteado): busca o próximo na Caixa assim que a tela abre.
    // Desde a 0.27 não há mais o lápis para preencher à mão: sem resposta da Caixa, o "Tentar de novo".
    val proximoNosIndices = ctx.bolao.indices.itens.value[m]?.proximoNumero
    suspend fun buscarConcurso() {
        consulta = Consulta.Carregando
        consulta = try {
            val d = dadosNaCaixa(m)
            if (atual.concurso == 0 || d.numero > atual.concurso) mudarAtual(atual.com(d))
            Consulta.Ok("Dados do concurso ${d.numero} conferidos na Caixa.")
        } catch (t: Throwable) {
            if (atual.concurso == 0) Consulta.Erro("Não foi possível consultar a Caixa (${t.message}).") else Consulta.Nada
        }
    }
    LaunchedEffect(r.id) {
        val desatualizado = proximoNosIndices != null && Operacoes.concursoDesatualizado(atual, proximoNosIndices)
        if (atual.concurso == 0 || desatualizado) buscarConcurso()
    }

    // O volante (os números, os trevos, o time ou o mês): o que falta aparece embaixo do título.
    val doVolante = if (federal) emptyList() else Volante.pendencias(m, r.palpite)
    val completo = doVolante.isEmpty()
    // O resto: o que o volante não mostra sozinho.
    val pendencias = buildList {
        if (federal) {
            if (r.bilheteFederal.isNullOrBlank()) add("Informe o número do bilhete.")
            if ((r.fracoesFederal ?: 0) !in 1..10) add("Informe de 1 a 10 frações.")
        }
        if (r.concurso <= 0) add("Falta o número do concurso: o app ainda não conseguiu consultar a Caixa.")
        if (r.valor <= 0) add(if (federal) "Informe o valor pago." else "Informe o preço da aposta simples em Ajustes > Parâmetros.")
        if (comTeimosinha && r.teimosinha != 0 && r.teimosinha !in 2..Operacoes.MAXIMO_DA_TEIMOSINHA) {
            add("Teimosinha: informe de 2 a ${Operacoes.MAXIMO_DA_TEIMOSINHA} concursos.")
        }
    }
    val pronta = completo && pendencias.isEmpty()
    val numero = Operacoes.proximoNumero(estado, hoje)
    val titulo = Operacoes.tituloAposta(estado, r, hoje)
    val comEspelho = r.espelho && Volante.temEspelho(m)
    // Quantas apostas o registro faz: uma por concurso da teimosinha (vezes 2 com a aposta espelho).
    val concursos = if (comTeimosinha) Operacoes.concursosDaTeimosinha(r) else 1
    val apostas = concursos * (if (comEspelho) 2 else 1)
    val total = r.valor * apostas
    // O que as outras apostas do mesmo concurso já têm: o "Completar" não repete o mês nem o time dentro do conjunto.
    val ja = remember(estado.apostas, r.id, r.concurso) {
        val outras = estado.apostas.filter {
            it.id != r.id && it.modalidade == m && it.apuracao?.confirmadaEm == null && (r.concurso == 0 || it.concurso == r.concurso)
        }
        Volante.JaApostado(extras = outras.mapNotNull { it.extra?.takeIf { x -> x.isNotBlank() } }, jogos = outras.map { it.numeros })
    }

    /** Registra (a aposta, a espelho e os concursos da teimosinha) e avisa. [nota]: o que mudou na conferência do concurso. */
    fun registrar(nota: String) {
        registrando = true
        escopo.launch {
            try {
                val agora = System.currentTimeMillis()
                // Com a teimosinha desligada nos Parâmetros, a aposta vale só no concurso dela.
                val final = if (comTeimosinha) atual else atual.copy(teimosinha = 0)
                var feitas: List<Aposta> = emptyList()
                repo.atualizar { e ->
                    val (x, lista) = Operacoes.registrarTudo(Operacoes.salvarRascunho(e, final), final.id, hoje, agora)
                    feitas = lista
                    // O k1, o k2 e as odds do concurso ficam guardados na aposta (desde a 0.21).
                    Operacoes.guardarIndices(x, ctx.bolao.indices.itens.value)
                }
                // A página continua no volante, vazio, para a próxima aposta (desde a 0.25), já liberado.
                aoRegistrar(final, feitas)
                registrando = false
                avisos.showSnackbar(listOf(nota, mensagemDoRegistro(feitas, individual)).filter { it.isNotBlank() }.joinToString(" "))
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível registrar: ${t.message}")
            } finally {
                registrando = false
            }
        }
    }

    /** Antes de registrar: o concurso ainda é o que está recebendo apostas? Depois, a confirmação (ou o registro direto). */
    fun conferirERegistrar() {
        conferindo = true
        escopo.launch {
            val antes = atual
            val d = runCatching { dadosNaCaixa(m) }.getOrNull()
            val mudou = d != null && m.formato != Formato.LOTECA && m.formato != Formato.FEDERAL && d.numero > antes.concurso
            if (mudou) mudarAtual(antes.com(d!!))
            conferindo = false
            val nota = when {
                mudou -> "O concurso ${antes.concurso} da ${m.nome} já foi sorteado: a aposta vai para o concurso ${d!!.numero}."
                d == null -> "Não deu para conferir o concurso na Caixa agora: confira se o ${antes.concurso} ainda recebe apostas."
                else -> ""
            }
            // Parâmetro "Registrar sem confirmação" (desde a 0.22): direto, sem a janela.
            if (parametros.semConfirmacao) registrar(nota) else confirmar = nota
        }
    }

    (consulta as? Consulta.Erro)?.let {
        Aviso(it.texto, alerta = true)
        OutlinedButton(onClick = { escopo.launch { buscarConcurso() } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(TENTAR_DE_NOVO)
        }
    }

    // Volante
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TituloDoVolante(
                numero,
                texto = textoDoVolante(m, r.palpite, mais),
                pronto = completo,
                preco = if (federal && r.valor <= 0) "—" else Dinheiro.rs(r.valor),
                carregando = consulta == Consulta.Carregando,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VolanteEditavel(m, r, mais, ja, { mudarAtual(it) }, ::mudarPalpite)
                if (comTeimosinha) OpcaoTeimosinha(r, total) { mudarAtual(it) }
                // "Completar" enquanto falta marcar; com o volante pronto, no mesmo lugar, o "Registrar aposta". Com as
                // apostas com mais números (desde a 0.27), o lugar se divide em "Registrar" e "+1" (mais um número sorteado).
                var usouCompletar by rememberSaveable(r.id) { mutableStateOf(false) }
                val comMaisUm = completo && mais && m.formato == Formato.NUMEROS
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (!completo) {
                        BotaoCompletar(destacar = !usouCompletar, modifier = Modifier.weight(1f)) {
                            usouCompletar = true
                            mudarPalpite(Volante.completar(m, r.palpite, ja = ja))
                        }
                    } else {
                        Button(
                            onClick = { conferirERegistrar() },
                            enabled = pronta && bloqueio == null && !registrando && !conferindo,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (registrando || conferindo) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (comMaisUm) textoRegistrarCurto(apostas) else textoRegistrar(apostas), maxLines = 1)
                            }
                        }
                        if (comMaisUm) {
                            FilledTonalButton(
                                onClick = { mudarPalpite(Volante.maisUm(m, atual.palpite)) },
                                enabled = Volante.cabeMaisUm(m, r.palpite) && !registrando && !conferindo,
                                modifier = Modifier.weight(1f).semantics { contentDescription = MAIS_UM_DESCRICAO },
                            ) {
                                Text(MAIS_UM, maxLines = 1, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (!federal) {
                        OutlinedButton(
                            enabled = !registrando && !conferindo,
                            onClick = {
                                // Desde a 0.27 o Time do Coração também sai (até a 0.26 ficava: o time sorteado pelo
                                // "Completar" não saía mais com o "Limpar").
                                val vazio = Palpite(
                                    numeros = if (m.formato == Formato.COLUNAS) List(Volante.COLUNAS_SUPER_SETE) { -1 } else emptyList(),
                                )
                                mudarPalpite(vazio)
                            },
                        ) { Text("Limpar") }
                    }
                }
                // Desde a 0.28: o jogo favorito (a estrelinha do Painel de Jogos) preenche o volante. Com um só, ele
                // vai direto; com mais, a lista para escolher.
                if (favoritos.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { if (favoritos.size == 1) mudarPalpite(favoritos[0].palpite) else escolhendoFavorito = true },
                        enabled = !registrando && !conferindo,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Dourado, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(textoDoBotaoFavorito(favoritos.size), maxLines = 1)
                    }
                }
            }
        }
    }

    // Logo abaixo do "Completar": o que acontece ao sair com o volante incompleto.
    if (!ficaEmAndamento(r)) {
        Aviso("Se sair agora, a aposta é descartada: complete o " + (if (federal) "bilhete." else "volante."))
    }
    // O concurso do volante já foi sorteado e o resultado ainda não chegou: sem registro até os dados chegarem.
    if (bloqueio != null) {
        Aviso(bloqueio, alerta = true)
        OutlinedButton(onClick = aoAtualizar, enabled = !atualizando, modifier = Modifier.fillMaxWidth()) {
            if (atualizando) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Consultando a Caixa...")
            } else {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(ATUALIZAR_AGORA)
            }
        }
    }
    if (completo) pendencias.firstOrNull()?.let { Aviso(it, alerta = true) }
    if (pronta && !individual && bloqueio == null) {
        Aviso(
            if (apostas > 1) {
                "Registre as apostas e faça o jogo no app Loterias CAIXA com estes números" +
                    (if (comEspelho) ", marcando a Aposta espelho" else "") +
                    (if (concursos > 1) ", com a teimosinha de $concursos concursos" else "") +
                    ". Depois, anexe " + (if (comEspelho) "os comprovantes" else "o comprovante") + " da Caixa na aba Comprovantes."
            } else {
                "Registre a aposta e faça o jogo no app Loterias CAIXA com estes números. Depois, anexe o comprovante da Caixa na aba Comprovantes."
            },
        )
    }
    if (conferindo) {
        Text(
            "Conferindo o concurso na Caixa...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (!individual && completo) {
        OutlinedButton(onClick = { vendo = true }, enabled = pronta && !registrando && !conferindo, modifier = Modifier.fillMaxWidth()) {
            Text("Visualizar comprovante")
        }
    }

    if (vendo) {
        VisualizadorComprovante(
            estado,
            titulo,
            aoFechar = { vendo = false },
            previa = Recibos.previa(estado, TipoComprovante.APOSTA, Operacoes.textoAposta(estado, r, hoje), 1),
            anexos = emptyList(),
        )
    }

    confirmar?.let { antes ->
        // Lotomania com a aposta espelho (desde a 0.20): duas apostas, a segunda com os 50 números que ficaram de fora.
        val espelho = if (comEspelho) {
            "Com a aposta espelho: são duas apostas de ${Dinheiro.rs(r.valor)}" + (if (concursos > 1) " em cada concurso" else "") +
                ", a segunda com os 50 números que ficaram de fora."
        } else {
            ""
        }
        // Teimosinha (desde a 0.22): uma aposta em cada concurso.
        val teimosinha = if (concursos > 1) {
            "Teimosinha: a mesma aposta em $concursos concursos seguidos (do ${r.concurso} ao ${r.concurso + concursos - 1}), " +
                "${Dinheiro.rs(r.valor)} em cada um."
        } else {
            ""
        }
        val varias = apostas > 1
        val explicacao = if (individual) {
            "Desconta ${Dinheiro.rs(total)} do saldo. Dá para excluir " + (if (varias) "as apostas" else "a aposta") + " depois."
        } else if (varias) {
            "Desconta ${Dinheiro.rs(total)} do saldo. Depois, anexe " + (if (comEspelho) "os comprovantes" else "o comprovante") +
                " da Caixa na aba Comprovantes: até a conferência, as apostas ainda podem ser descartadas."
        } else {
            "Desconta ${Dinheiro.rs(r.valor)} do saldo. Depois, anexe o comprovante da Caixa na aba Comprovantes: " +
                "até a conferência dele, a aposta ainda pode ser descartada."
        }
        DialogoSimNao(
            titulo = if (varias) "Deseja registrar as apostas?" else "Deseja registrar aposta?",
            texto = listOf(antes, espelho, teimosinha, explicacao).filter { it.isNotBlank() }.joinToString("\n\n"),
            aoNao = { confirmar = null },
            aoSim = {
                confirmar = null
                registrar("")
            },
        )
    }
}

/**
 * O aviso do alto do volante depois do registro (desde a 0.25): as apostas registradas na página que ainda
 * esperam o comprovante da Caixa e o botão que abre os Comprovantes.
 */
@Composable
fun AvisoDoComprovante(apostas: List<Aposta>, aoAnexar: () -> Unit) {
    if (apostas.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Aviso(textoFaltaComprovante(apostas.map { it.numero }), alerta = true)
        OutlinedButton(onClick = aoAnexar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(ANEXAR_COMPROVANTES)
        }
    }
}

/** O botão do aviso do comprovante que falta: abre os Comprovantes. */
const val ANEXAR_COMPROVANTES = "Anexar comprovantes"

/** "Falta o comprovante da Caixa da aposta Nº3." (ou "das apostas Nº3, Nº4 e Nº5"), com o que fazer. */
fun textoFaltaComprovante(numeros: List<Int>): String {
    val lista = numeros.sorted().map { "Nº$it" }
    val quais = if (lista.size == 1) "da aposta ${lista[0]}" else "das apostas " + lista.dropLast(1).joinToString(", ") + " e " + lista.last()
    val valer = if (lista.size == 1) "ela valer" else "elas valerem"
    return "Falta o comprovante da Caixa $quais. Anexe em Comprovantes para $valer no bolão."
}

/** O botão do volante pronto: "Registrar aposta" ou, com a espelho ou a teimosinha, "Registrar 4 apostas". */
fun textoRegistrar(apostas: Int): String = if (apostas > 1) "Registrar $apostas apostas" else "Registrar aposta"

/** O mesmo, mais curto, dividindo o lugar com o "+1" (desde a 0.27): "Registrar" ou "Registrar (4)". */
fun textoRegistrarCurto(apostas: Int): String = if (apostas > 1) "Registrar ($apostas)" else "Registrar"

/** O botão do volante que preenche um jogo favorito (desde a 0.28): "Jogo favorito" ou "Jogos favoritos (3)". */
fun textoDoBotaoFavorito(quantos: Int): String = if (quantos == 1) "Jogo favorito" else "Jogos favoritos ($quantos)"

/** O título da janela que escolhe o jogo favorito do volante. */
const val ESCOLHA_O_FAVORITO = "Escolha o jogo favorito"

/**
 * A janela do volante com os jogos favoritos da modalidade (desde a 0.28, com mais de um): cada um com quantos
 * números, o preço e as dezenas em bolas; tocar preenche o volante.
 */
@Composable
private fun EscolhaDoFavorito(estado: Estado, favoritos: List<Favorito>, aoEscolher: (Favorito) -> Unit, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(ESCOLHA_O_FAVORITO) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                favoritos.forEach { f ->
                    Card(
                        onClick = { aoEscolher(f) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                Favoritos.descricao(f) + " · " + valorSemQuebra(Dinheiro.rs(Favoritos.preco(estado, f))),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            DezenasDoJogo(f.modalidade, f.numeros, f.trevos, f.extra)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

/** O botão que marca mais um número sorteado (com as apostas com mais números, desde a 0.27). */
const val MAIS_UM = "+1"
const val MAIS_UM_DESCRICAO = "Mais um número"

/** O botão que consulta a Caixa de novo quando ela não respondeu (desde a 0.27, no lugar do lápis). */
const val TENTAR_DE_NOVO = "Tentar de novo"

/** O botão do volante bloqueado (o concurso já foi sorteado e o resultado não chegou): consulta a Caixa na hora. */
const val ATUALIZAR_AGORA = "Atualizar agora"

/**
 * O aviso depois do registro: "Aposta Nº3 registrada.", "Apostas Nº3 e Nº4 (espelho) registradas.",
 * "Apostas Nº3 a Nº6 registradas (teimosinha de 4 concursos)."; no bolão, com o lembrete do comprovante.
 */
fun mensagemDoRegistro(feitas: List<Aposta>, individual: Boolean): String {
    val primeira = feitas.firstOrNull() ?: return "Aposta registrada."
    val teimosinha = primeira.ehTeimosinha
    val espelho = feitas.size == 2 && !teimosinha
    val texto = when {
        feitas.size == 1 -> "Aposta Nº${primeira.numero} registrada."
        espelho -> "Apostas Nº${primeira.numero} e Nº${feitas[1].numero} (espelho) registradas."
        else -> "Apostas Nº${feitas.minOf { it.numero }} a Nº${feitas.maxOf { it.numero }} registradas" +
            (if (teimosinha) " (teimosinha de ${primeira.teimosinha} concursos)" else "") + "."
    }
    val lembrete = when {
        individual -> ""
        feitas.size == 2 && espelho -> " Anexe os comprovantes da Caixa na aba Comprovantes."
        else -> " Anexe o comprovante da Caixa na aba Comprovantes."
    }
    return texto + lembrete
}

/**
 * "Completar" com um efeito de pulsar enquanto não foi tocado ([destacar]): o botão respira e
 * solta um anel em volta, para chamar a atenção para o próximo passo.
 */
@Composable
private fun BotaoCompletar(destacar: Boolean, modifier: Modifier, aoTocar: () -> Unit) {
    val conteudo: @Composable () -> Unit = {
        Icon(Icons.Outlined.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Completar")
    }
    if (!destacar || !Animacoes.ligadas) {
        FilledTonalButton(onClick = aoTocar, modifier = modifier) { conteudo() }
        return
    }
    val pulso = rememberInfiniteTransition(label = "pulso")
    val escala by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escala",
    )
    val anel by pulso.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "anel",
    )
    val cor = MaterialTheme.colorScheme.primary
    Button(
        onClick = aoTocar,
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .drawBehind {
                val folga = 7.dp.toPx() * anel
                drawRoundRect(
                    color = cor.copy(alpha = 0.45f * (1f - anel)),
                    topLeft = Offset(-folga, -folga),
                    size = Size(size.width + 2 * folga, size.height + 2 * folga),
                    cornerRadius = CornerRadius(size.height / 2 + folga),
                )
            },
    ) { conteudo() }
}

@Composable
private fun VolanteEditavel(
    m: Modalidade,
    r: Aposta,
    mais: Boolean,
    ja: Volante.JaApostado,
    mudar: (Aposta) -> Unit,
    mudarPalpite: (Palpite) -> Unit,
) {
    when (m.formato) {
        Formato.NUMEROS -> {
            GradeNumeros(
                m,
                situacao = { if (it in r.numeros) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                aoTocar = { n -> mudarPalpite(Volante.alternar(m, r.palpite, n, mais)) },
            )
            if (m.trevos > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trevos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (mais) "${r.trevos.size} (de ${m.trevos} a ${Volante.maximoTrevos(m)})" else "${r.trevos.size} de ${m.trevos}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                GradeTrevos(
                    m,
                    situacao = { if (it in r.trevos) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                    aoTocar = { t -> mudarPalpite(Volante.alternarTrevo(m, r.palpite, t, mais)) },
                )
            }
            when (m.extra) {
                Extra.TIME_DO_CORACAO -> BotaoTimeDoCoracao(r.extra) { mudar(r.copy(extra = it)) }
                // O "Sortear" do mês prefere os meses que as outras apostas do concurso ainda não têm.
                Extra.MES_DE_SORTE -> BotaoMesDeSorte(r.extra, ja.extras) { mudar(r.copy(extra = it)) }
                Extra.NENHUM -> Unit
            }
            if (Volante.temEspelho(m)) OpcaoEspelho(r.espelho, r.valor) { mudar(r.copy(espelho = it)) }
        }
        Formato.COLUNAS -> {
            Text("Um número em cada coluna", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GradeColunas(
                m,
                situacao = { c, n -> if (r.numeros.getOrNull(c) == n) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                aoTocar = { c, n ->
                    val cols = MutableList(Volante.COLUNAS_SUPER_SETE) { i -> r.numeros.getOrNull(i) ?: -1 }
                    cols[c] = n
                    mudar(r.copy(numeros = cols))
                },
            )
        }
        Formato.LOTECA -> {
            Text("Palpites: 1 (time da casa), X (empate) ou 2", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GradeLoteca(
                m,
                rotulos = r.jogosLoteca,
                palpites = r.palpitesLoteca,
                aoEscolher = { j, op ->
                    val lista = MutableList(Volante.JOGOS_LOTECA) { i -> r.palpitesLoteca.getOrNull(i) ?: "" }
                    lista[j] = op
                    mudar(r.copy(palpitesLoteca = lista))
                },
            )
        }
        Formato.FEDERAL -> {
            OutlinedTextField(
                value = r.bilheteFederal.orEmpty(),
                onValueChange = { t -> mudar(r.copy(bilheteFederal = t.filter(Char::isDigit).take(6))) },
                label = { Text("Número do bilhete") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            CampoInteiro(
                valor = r.fracoesFederal,
                aoMudar = { mudar(r.copy(fracoesFederal = it?.coerceAtMost(10))) },
                rotulo = "Frações (1 a 10)",
                ajuda = "10 frações = bilhete inteiro.",
                modifier = Modifier.fillMaxWidth(),
            )
            CampoDinheiro(
                centavos = r.valor,
                aoMudar = { mudar(r.copy(valor = it)) },
                rotulo = "Valor pago",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** O nome da opção da teimosinha no volante (com o parâmetro ligado). */
const val TITULO_TEIMOSINHA = "Teimosinha"

/** O campo da quantidade de concursos da teimosinha. */
const val CAMPO_DA_TEIMOSINHA = "Nº de concursos"

/**
 * A teimosinha no volante (desde a 0.22, com o parâmetro ligado): a chave e, ligada, o campo com o
 * número de concursos seguidos em que a aposta se repete (de 2 a 24). No registro vira uma aposta por
 * concurso. [total]: quanto o registro desconta (todas as apostas).
 */
@Composable
private fun OpcaoTeimosinha(r: Aposta, total: Long, aoMudar: (Aposta) -> Unit) {
    val ligada = r.teimosinha != 0
    // O que está digitado (a aposta guarda 1 enquanto o campo está vazio ou com um número que não vale).
    var texto by rememberSaveable(r.id) { mutableStateOf(if (r.teimosinha >= 2) r.teimosinha.toString() else "") }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .toggleable(value = ligada, role = Role.Switch) { v ->
                    texto = if (v) "2" else ""
                    aoMudar(r.copy(teimosinha = if (v) 2 else 0))
                }
                .padding(vertical = 6.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(TITULO_TEIMOSINHA, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (ligada && r.teimosinha in 2..Operacoes.MAXIMO_DA_TEIMOSINHA) {
                        "A mesma aposta em ${r.teimosinha} concursos seguidos: ${valorSemQuebra(Dinheiro.rs(total))}."
                    } else {
                        "Repete a mesma aposta nos concursos seguintes."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = ligada, onCheckedChange = null)
        }
        if (ligada) {
            OutlinedTextField(
                value = texto,
                onValueChange = { novo ->
                    texto = novo.filter(Char::isDigit).take(2)
                    val n = texto.toIntOrNull() ?: 0
                    aoMudar(r.copy(teimosinha = if (n >= 2) n else 1))
                },
                label = { Text(CAMPO_DA_TEIMOSINHA) },
                supportingText = { Text("De 2 a ${Operacoes.MAXIMO_DA_TEIMOSINHA} concursos seguidos.") },
                isError = r.teimosinha !in 2..Operacoes.MAXIMO_DA_TEIMOSINHA,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * A opção "Aposta espelho" do volante da Lotomania (desde a 0.20), como no volante da Caixa: ligada, o
 * registro faz também uma segunda aposta, com os 50 números que ficaram de fora (pelo mesmo [preco]).
 */
@Composable
private fun OpcaoEspelho(ligada: Boolean, preco: Long, aoMudar: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = ligada, role = Role.Switch, onValueChange = aoMudar)
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(TITULO_ESPELHO, style = MaterialTheme.typography.titleMedium)
            Text(
                "Faz outra aposta com os 50 números que ficaram de fora (mais ${Dinheiro.rs(preco)}).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = ligada, onCheckedChange = null)
    }
}

const val TITULO_ESPELHO = "Aposta espelho"

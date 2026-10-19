package io.github.marcosfaria94.bolao.ui.apuracao

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.JogoLoteca
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.imagem.ImagemConferencia
import io.github.marcosfaria94.bolao.imagem.LayoutTermico
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.aposta.CabecalhoAposta
import io.github.marcosfaria94.bolao.ui.aposta.GradeColunas
import io.github.marcosfaria94.bolao.ui.aposta.GradeLoteca
import io.github.marcosfaria94.bolao.ui.aposta.GradeNumeros
import io.github.marcosfaria94.bolao.ui.aposta.GradeTrevos
import io.github.marcosfaria94.bolao.ui.aposta.LinhaInfo
import io.github.marcosfaria94.bolao.ui.aposta.SituacaoNumero
import io.github.marcosfaria94.bolao.ui.aposta.VolanteDaApuracao
import io.github.marcosfaria94.bolao.ui.comum.AVISO_DEFINITIVO
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BotoesDeRegistro
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.TextoDoWhatsapp
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private sealed interface Busca {
    data object Carregando : Busca
    data class Pronto(val concurso: Concurso, val manual: Boolean) : Busca
    data class Falhou(val motivo: String) : Busca
}

@Composable
fun ApuracaoTela(estado: Estado, id: String) {
    val a = estado.apostas.firstOrNull { it.id == id }
    if (a == null || !a.registrada) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("Esta aposta não está registrada.") }
        return
    }
    if (a.apuracao?.confirmadaEm != null) {
        ApuracaoConfirmada(estado, a)
    } else {
        ApuracaoPendente(estado, a)
    }
}

/** "Contagem dos acertos", "Resultado" e "Acumulou", logo abaixo do cabeçalho. */
@Composable
private fun ResumoApuracao(acertos: String, premio: Long, acumulou: Boolean?) {
    Column {
        LinhaInfo("Contagem dos acertos", acertos)
        LinhaInfo("Resultado", if (premio > 0) "Prêmio de ${Dinheiro.rs(premio)}" else "Sem prêmio", destaque = premio > 0)
        LinhaInfo("Acumulou", when (acumulou) {
            true -> "SIM"
            false -> "NÃO"
            null -> "—"
        })
    }
}

/** Tabela do rateio oficial da Caixa que vai no comprovante da apuração. */
private fun rateioDe(a: Aposta, ap: Apuracao): LayoutTermico.RateioOficial? =
    ap.rateioOficial.takeIf { it.isNotEmpty() }?.let { LayoutTermico.RateioOficial(a.concurso, it, ap.faixasPremiadas.toSet(), ap.acumulou) }

@Composable
private fun ImagemDosAcertos(a: Aposta, ap: Apuracao) {
    val ctx = LocalContext.current
    val imagem by produceState<Bitmap?>(null, a, ap) {
        value = withContext(Dispatchers.Default) { runCatching { ImagemConferencia.gerar(ctx, a, ap) }.getOrNull() }
    }
    imagem?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Imagem dos acertos",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ApuracaoPendente(estado: Estado, a: Aposta) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val m = a.modalidade
    val hoje = LocalDate.now()

    var busca by remember { mutableStateOf<Busca>(Busca.Carregando) }
    var premio by remember { mutableLongStateOf(0L) }
    var premioEditado by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }

    suspend fun buscar() {
        busca = Busca.Carregando
        busca = try {
            val c = CaixaApi.concurso(m, a.concurso)
            if (c.temResultado) Busca.Pronto(c, manual = false) else Busca.Falhou("O resultado do concurso ${a.concurso} ainda não saiu.")
        } catch (t: Throwable) {
            Busca.Falhou("Não foi possível consultar a Caixa: ${t.message}")
        }
    }
    LaunchedEffect(a.id) { buscar() }

    val pronto = busca as? Busca.Pronto
    val conferencia = pronto?.let { Conferencia.conferir(m, a.palpite, it.concurso, a.bilheteFederal, a.fracoesFederal) }
    // Mesmo instante enquanto o resultado não muda (evita refazer a imagem a cada toque).
    val instante = remember(conferencia) { System.currentTimeMillis() }
    LaunchedEffect(conferencia) {
        if (conferencia != null && !premioEditado) premio = conferencia.premio
        // Guarda a conferência (sem confirmar): é ela que acende o aviso da aba Apostas.
        if (conferencia != null && pronto?.manual == false) {
            val ap = Operacoes.apuracaoDe(pronto.concurso, conferencia, conferencia.premio, System.currentTimeMillis())
            repo.enfileirar { Operacoes.guardarConferencia(it, a.id, ap) }
        }
    }
    val ap = if (pronto != null && conferencia != null) Operacoes.apuracaoDe(pronto.concurso, conferencia, premio, instante) else null
    val titulo = Mensagens.tituloApuracao(a.numero, hoje)

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CabecalhoAposta(a, a.numero)

        when (val b = busca) {
            Busca.Carregando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Text("  Buscando o resultado na Caixa...")
            }
            is Busca.Falhou -> {
                Aviso(b.motivo, alerta = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { escopo.launch { buscar() } }) { Text("Tentar de novo") }
                    Button(onClick = { busca = Busca.Pronto(concursoVazio(a), manual = true) }) { Text("Informar à mão") }
                }
            }
            is Busca.Pronto -> Unit
        }

        if (pronto != null && conferencia != null && ap != null) {
            ResumoApuracao(conferencia.acertos, premio, ap.acumulou)
            if (pronto.manual) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resultado do sorteio", style = MaterialTheme.typography.titleMedium)
                        ResultadoManual(a, pronto.concurso) { busca = Busca.Pronto(it, manual = true) }
                    }
                }
            } else {
                Text(
                    "Resultado oficial da Caixa, concurso ${pronto.concurso.numero}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (conferencia.observacoes.isNotEmpty()) {
                    TextButton(onClick = { busca = Busca.Pronto(pronto.concurso, manual = true) }) { Text("Informar o resultado à mão") }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (m.formato != Formato.FEDERAL) VolanteDaApuracao(a.copy(apuracao = ap))
                    conferencia.faixas.forEach { f ->
                        Text(
                            "Faixa premiada: ${f.descricao}" + if (f.valor > 0) " · ${Dinheiro.rs(f.valor)}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    conferencia.observacoes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }

            CampoDinheiro(
                centavos = premio,
                aoMudar = {
                    premio = it
                    premioEditado = true
                },
                rotulo = "Prêmio do bolão neste jogo",
                ajuda = if (conferencia.precisaConfirmarValor) "Confira o valor no app da Caixa." else "Valor calculado pela tabela de prêmios da Caixa.",
                modifier = Modifier.fillMaxWidth(),
            )
            if (conferencia.precisaConfirmarValor && conferencia.premiada) {
                Aviso("A Caixa não informou o valor desta faixa: digite o prêmio que aparece no app da Caixa.", alerta = true)
            }

            ImagemDosAcertos(a, ap)
            TextoDoWhatsapp(titulo, rodape = "A imagem do comprovante (com o rateio oficial) e a imagem dos acertos vão juntas.")
            BotoesDeRegistro(
                habilitado = true,
                trabalhando = trabalhando,
                aoVisualizar = { vendo = true },
                aoRegistrar = { confirmar = true },
                textoRegistrar = "Confirmar e enviar",
            )

            if (vendo) {
                val acertos by produceState<Bitmap?>(null, ap) {
                    value = withContext(Dispatchers.Default) { runCatching { ImagemConferencia.gerar(ctx, a, ap) }.getOrNull() }
                }
                VisualizadorComprovante(
                    estado,
                    titulo,
                    aoFechar = { vendo = false },
                    previa = Recibos.previa(
                        estado, TipoComprovante.APURACAO,
                        Operacoes.textoApuracao(estado, a, conferencia.acertos, premio, hoje), 1, rateioDe(a, ap),
                    ),
                    imagensExtras = listOfNotNull(acertos?.let { "Anexo: imagem dos acertos" to it }),
                )
            }

            if (confirmar) {
                DialogoConfirmar(
                    titulo = "Confirmar a apuração da aposta Nº${a.numero}?",
                    texto = (
                        if (premio > 0) "Soma ${Dinheiro.rs(premio)} ao saldo. Em seguida o WhatsApp abre com o comprovante e a imagem dos acertos."
                        else "Registra que não houve prêmio. Em seguida o WhatsApp abre com o comprovante e a imagem dos acertos."
                        ) + "\n\n" + AVISO_DEFINITIVO,
                    confirmar = "Confirmar",
                    aoConfirmar = {
                        confirmar = false
                        trabalhando = true
                        escopo.launch {
                            try {
                                val agora = System.currentTimeMillis()
                                val registro = ap
                                val imagem = repo.guardarGerado("acertos", "acertos-${m.sigla}-${a.concurso}.png", "image/png") { f ->
                                    ImagemConferencia.salvar(ctx, a, registro, f)
                                }
                                registrarComprovante(ctx, { Operacoes.confirmarApuracao(it, a.id, registro, hoje, agora, imagem) })
                            } catch (t: Throwable) {
                                avisos.showSnackbar("Não foi possível confirmar: ${t.message}")
                            } finally {
                                trabalhando = false
                            }
                        }
                    },
                    aoFechar = { confirmar = false },
                )
            }
        }
    }
}

@Composable
private fun ApuracaoConfirmada(estado: Estado, a: Aposta) {
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    val ap = a.apuracao ?: return
    val comprovante = Operacoes.comprovanteDaApuracao(estado, a)
    var vendo by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CabecalhoAposta(a, a.numero)
        ResumoApuracao(ap.acertos, ap.premio, ap.acumulou)
        if (a.modalidade.formato != Formato.FEDERAL) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VolanteDaApuracao(a)
                }
            }
        }
        ap.faixa?.let { Text("Faixa: $it", style = MaterialTheme.typography.bodyMedium) }
        if (comprovante != null) {
            TextoDoWhatsapp(comprovante.titulo)
            OutlinedButton(onClick = { vendo = true }, modifier = Modifier.fillMaxWidth()) { Text("Visualizar comprovante") }
            val enviado = estado.enviado(comprovante)
            OutlinedButton(
                onClick = { escopo.launch { Compartilhar.comprovante(ctx, estado, comprovante) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (enviado) "Enviar de novo" else "Enviar ao grupo") }
        }
    }

    if (vendo && comprovante != null) {
        VisualizadorComprovante(estado, comprovante.titulo, aoFechar = { vendo = false }, comprovante = comprovante)
    }
}

private fun concursoVazio(a: Aposta) = Concurso(
    modalidade = a.modalidade,
    numero = a.concurso,
    data = a.dataSorteio,
    dezenas = if (a.modalidade.formato == Formato.COLUNAS) List(Volante.COLUNAS_SUPER_SETE) { -1 } else emptyList(),
)

/** Digitação do resultado quando a Caixa não responde. */
@Composable
private fun ResultadoManual(a: Aposta, atual: Concurso, aoMudar: (Concurso) -> Unit) {
    val m = a.modalidade
    when (m.formato) {
        Formato.NUMEROS -> {
            val alvo = if (m == Modalidade.DUPLA_SENA) "1º sorteio" else "Números sorteados"
            Text("$alvo (${atual.dezenas.size} de ${m.qtdSorteada})", style = MaterialTheme.typography.labelLarge)
            GradeNumeros(
                m,
                situacao = { if (it in atual.dezenas) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                aoTocar = { n ->
                    val novo = if (n in atual.dezenas) atual.dezenas - n else if (atual.dezenas.size < m.qtdSorteada) (atual.dezenas + n).sorted() else atual.dezenas
                    aoMudar(atual.copy(dezenas = novo))
                },
            )
            if (m == Modalidade.DUPLA_SENA) {
                Text("2º sorteio (${atual.segundoSorteio.size} de 6)", style = MaterialTheme.typography.labelLarge)
                GradeNumeros(
                    m,
                    situacao = { if (it in atual.segundoSorteio) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                    aoTocar = { n ->
                        val s = atual.segundoSorteio
                        val novo = if (n in s) s - n else if (s.size < 6) (s + n).sorted() else s
                        aoMudar(atual.copy(segundoSorteio = novo))
                    },
                )
            }
            if (m.trevos > 0) {
                Text("Trevos sorteados (${atual.trevos.size} de ${m.trevos})", style = MaterialTheme.typography.labelLarge)
                GradeTrevos(
                    m,
                    situacao = { if (it in atual.trevos) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
                    aoTocar = { t ->
                        val novo = if (t in atual.trevos) atual.trevos - t else if (atual.trevos.size < m.trevos) (atual.trevos + t).sorted() else atual.trevos
                        aoMudar(atual.copy(trevos = novo))
                    },
                )
            }
            if (m.extra != Extra.NENHUM) {
                OutlinedTextField(
                    value = atual.timeOuMes.orEmpty(),
                    onValueChange = { aoMudar(atual.copy(timeOuMes = it)) },
                    label = { Text(if (m.extra == Extra.MES_DE_SORTE) "Mês de Sorte sorteado" else "Time do Coração sorteado") },
                    supportingText = if (m.extra == Extra.MES_DE_SORTE) {
                        { Text(MESES_DE_SORTE.joinToString(", ")) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Formato.COLUNAS -> GradeColunas(
            m,
            situacao = { c, n -> if (atual.dezenas.getOrNull(c) == n) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE },
            aoTocar = { c, n ->
                val cols = MutableList(Volante.COLUNAS_SUPER_SETE) { i -> atual.dezenas.getOrNull(i) ?: -1 }
                cols[c] = n
                aoMudar(atual.copy(dezenas = cols))
            },
        )
        Formato.LOTECA -> {
            val resultados = (0 until Volante.JOGOS_LOTECA).map { atual.jogosLoteca.getOrNull(it)?.resultado.orEmpty() }
            GradeLoteca(
                m,
                rotulos = a.jogosLoteca,
                palpites = resultados,
                aoEscolher = { j, op ->
                    val jogos = (0 until Volante.JOGOS_LOTECA).map { i ->
                        val r = if (i == j) op else resultados[i]
                        val (g1, g2) = when (r) {
                            "1" -> 1 to 0
                            "2" -> 0 to 1
                            "X" -> 0 to 0
                            else -> null to null
                        }
                        JogoLoteca(i + 1, "", "", g1, g2)
                    }
                    aoMudar(atual.copy(jogosLoteca = jogos))
                },
            )
        }
        Formato.FEDERAL -> {
            (0 until 5).forEach { i ->
                OutlinedTextField(
                    value = atual.bilhetesFederal.getOrNull(i).orEmpty(),
                    onValueChange = { t ->
                        val lista = MutableList(5) { k -> atual.bilhetesFederal.getOrNull(k).orEmpty() }
                        lista[i] = t.filter(Char::isDigit).take(6)
                        aoMudar(atual.copy(bilhetesFederal = lista.takeIf { l -> l.any { it.isNotEmpty() } } ?: emptyList()))
                    },
                    label = { Text("${i + 1}º prêmio") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

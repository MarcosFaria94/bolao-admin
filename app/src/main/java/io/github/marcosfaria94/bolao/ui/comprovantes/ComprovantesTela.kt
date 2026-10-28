package io.github.marcosfaria94.bolao.ui.comprovantes

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Associacao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Rateio
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.imagem.LeitorComprovante
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.Secao
import io.github.marcosfaria94.bolao.ui.comum.TextoVazio
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import io.github.marcosfaria94.bolao.ui.tema.corApuracao
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * O que a aba Comprovantes está fazendo: os arquivos escolhidos (já copiados para o app), a leitura,
 * o relatório da última leitura e a conferência. Fica no modelo da tela das abas, para não parar
 * quando se desliza para outra aba no meio da leitura.
 */
class ComprovantesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.bolao.repo

    /** Arquivos escolhidos, já guardados no app, esperando o "Registrar comprovantes". */
    val escolhidos = mutableStateListOf<Arquivo>()

    /** Texto já conhecido de um arquivo (os de exemplo do modo de teste, que o próprio app desenhou). */
    private val textos = mutableMapOf<String, String>()

    /** "Lendo 2 de 5..." enquanto lê; null parado. */
    var lendo by mutableStateOf<String?>(null)
        private set

    /** Avisos da última leitura (a janela do relatório); null = fechada. */
    var relatorio by mutableStateOf<List<Associacao.Aviso>?>(null)

    var aprovando by mutableStateOf(false)
        private set

    fun adicionar(uris: List<Uri>, aoErro: (String) -> Unit) {
        viewModelScope.launch {
            for (u in uris) {
                try {
                    escolhidos += repo.guardarArquivo(u, "anexo")
                } catch (t: Throwable) {
                    aoErro("Não foi possível abrir um dos arquivos: ${t.message}")
                }
            }
        }
    }

    fun remover(a: Arquivo) {
        escolhidos.remove(a)
        textos.remove(a.nome)
        repo.apagarSeSolto(a)
    }

    /** Modo de teste: os comprovantes de exemplo de tudo o que espera (apostas e Pix), já escolhidos. */
    fun usarExemplos(context: Context, aoErro: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ModoTeste.comprovantesDeExemplo(context, repo.atual()).forEach { (arquivo, texto) ->
                    escolhidos += arquivo
                    textos[arquivo.nome] = texto
                }
            } catch (t: Throwable) {
                aoErro("Não foi possível criar os comprovantes de exemplo: ${t.message}")
            }
        }
    }

    /** Lê os arquivos escolhidos e põe cada um no seu lugar (Associacao); no fim, abre o relatório. */
    fun registrar(aoErro: (String) -> Unit) {
        if (lendo != null || escolhidos.isEmpty()) return
        val lista = escolhidos.toList()
        viewModelScope.launch {
            try {
                val entradas = lista.mapIndexed { i, a ->
                    lendo = "Lendo ${i + 1} de ${lista.size}..."
                    val texto = textos[a.nome] ?: runCatching { LeitorComprovante.texto(repo.arquivo(a), a.mime) }.getOrDefault("")
                    Associacao.Entrada(a, texto)
                }
                lendo = "Guardando..."
                var resultado: Associacao.Resultado? = null
                repo.atualizar { e -> Associacao.associar(e, entradas).also { resultado = it }.estado }
                val r = resultado ?: return@launch
                escolhidos.clear()
                textos.clear()
                r.descartados.forEach { repo.apagarSeSolto(it) }
                // A aposta mudou de concurso pelo comprovante: o dia do sorteio e o prêmio vêm da Caixa.
                for (id in r.concursoMudado) atualizarSorteio(id)
                relatorio = r.avisos
            } catch (t: Throwable) {
                aoErro("Não foi possível registrar os comprovantes: ${t.message}")
            } finally {
                lendo = null
            }
        }
    }

    private suspend fun atualizarSorteio(id: String) {
        val a = repo.atual().apostas.firstOrNull { it.id == id } ?: return
        val m = a.modalidade
        val (data, premio) = runCatching {
            val u = CaixaApi.ultimo(m)
            if (u.proximoNumero == a.concurso) {
                u.proximaData to u.premioEstimadoProximo
            } else {
                val c = CaixaApi.concurso(m, a.concurso)
                c.data to null
            }
        }.getOrNull() ?: return
        runCatching { repo.atualizar { Operacoes.ajustarSorteio(it, id, data, premio) } }
    }

    /**
     * A conferência: aprova tudo o que tem os comprovantes anexados (na ordem do registro), criando os
     * comprovantes do bolão. Eles ficam prontos para enviar ao grupo (logo abaixo, na aba).
     */
    fun aprovarTodos(context: Context, aoFim: (String) -> Unit) {
        if (aprovando) return
        aprovando = true
        viewModelScope.launch {
            var feitos = 0
            try {
                val e = repo.atual()
                val agora = System.currentTimeMillis()
                data class Item(val ordem: Long, val aprovar: (Estado, Long) -> Estado)
                val itens = Operacoes.apostasParaConferir(e).map { a ->
                    val ordem = e.lancamentos.firstOrNull { it.apostaId == a.id && it.tipo == TipoLancamento.APOSTA }?.criadoEm ?: a.criadaEm
                    Item(ordem) { est, t -> Operacoes.aprovarAposta(est, a.id, t) }
                } + Operacoes.rateiosParaConferir(e).map { r ->
                    Item(r.criadoEm) { est, t -> Operacoes.aprovarRateio(est, r.numero, t) }
                }
                itens.sortedBy { it.ordem }.forEachIndexed { i, item ->
                    registrarComprovante(context, { est -> item.aprovar(est, agora + i) }, enviar = false)
                    feitos++
                }
                aoFim(if (feitos == 1) "Conferência aprovada: 1 comprovante pronto para enviar ao grupo." else "Conferência aprovada: $feitos comprovantes prontos para enviar ao grupo.")
            } catch (t: Throwable) {
                aoFim("Não foi possível aprovar tudo ($feitos aprovados): ${t.message}")
            } finally {
                aprovando = false
            }
        }
    }

    override fun onCleared() {
        // Escolhidos e não registrados: os arquivos copiados não servem para nada.
        escolhidos.forEach { repo.apagarSeSolto(it) }
    }
}

/**
 * Aba Comprovantes (desde a 0.10): as apostas e os rateios são registrados antes dos comprovantes;
 * aqui se escolhem de uma vez os PDFs da Caixa e dos Pix, o app lê cada um e põe no lugar certo
 * ("Registrar comprovantes"), e depois vem a conferência ("Aprovar todos"), que cria os
 * comprovantes do bolão para enviar ao grupo.
 */
@Composable
fun ComprovantesTela(estado: Estado, abrir: (String) -> Unit, vm: ComprovantesViewModel = viewModel()) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val teste by repo.modoTeste.collectAsStateWithLifecycle()
    val avisar: (String) -> Unit = { escopo.launch { avisos.showSnackbar(it) } }
    var confirmarAprovacao by remember { mutableStateOf(false) }

    val semComprovante = Operacoes.apostasSemComprovante(estado)
    val rateiosSemPix = Operacoes.rateiosSemPix(estado)
    val apostasConferir = Operacoes.apostasParaConferir(estado)
    val rateiosConferir = Operacoes.rateiosParaConferir(estado)
    val aEnviar = Operacoes.comprovantesPendentesDeEnvio(estado).sortedByDescending { it.criadoEm }
    val nada = semComprovante.isEmpty() && rateiosSemPix.isEmpty() && apostasConferir.isEmpty() && rateiosConferir.isEmpty() && aEnviar.isEmpty()

    val escolher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.adicionar(uris, avisar)
    }

    LazyColumn(contentPadding = PaddingTela, verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize()) {
        item(key = "anexar") {
            Secao("Anexar comprovantes", Modifier.padding(top = 8.dp)) {
                Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Escolha de uma vez os PDFs da Caixa (das apostas) e dos Pix (do rateio). O app lê cada um e " +
                            "põe no lugar certo; depois, é só conferir e aprovar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { escolher.launch(arrayOf("application/pdf", "image/*")) },
                        enabled = vm.lendo == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Escolher os PDFs")
                    }
                    if (teste && (semComprovante.isNotEmpty() || rateiosSemPix.isNotEmpty())) {
                        TextButton(onClick = { vm.usarExemplos(ctx, avisar) }, enabled = vm.lendo == null) {
                            Text("Usar comprovantes de exemplo")
                        }
                    }
                    vm.escolhidos.forEach { a ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(a.nomeOriginal, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.remover(a) }, enabled = vm.lendo == null) {
                                Icon(Icons.Outlined.Close, contentDescription = "Tirar ${a.nomeOriginal}")
                            }
                        }
                    }
                    Button(
                        onClick = { vm.registrar(avisar) },
                        enabled = vm.escolhidos.isNotEmpty() && vm.lendo == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (vm.lendo != null) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(vm.lendo.orEmpty())
                        } else {
                            Text(if (vm.escolhidos.size > 1) "Registrar ${vm.escolhidos.size} comprovantes" else "Registrar comprovantes")
                        }
                    }
                }
            }
        }

        if (apostasConferir.isNotEmpty() || rateiosConferir.isNotEmpty()) {
            item(key = "conferencia") {
                Secao("Conferência") {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        apostasConferir.forEachIndexed { i, a ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ConferirAposta(estado, a, aoAbrir = { abrir("aposta/${a.id}") })
                        }
                        rateiosConferir.forEachIndexed { i, r ->
                            if (i > 0 || apostasConferir.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ConferirRateio(estado, r)
                        }
                        Spacer(Modifier.padding(top = 6.dp))
                        Button(
                            onClick = { confirmarAprovacao = true },
                            enabled = !vm.aprovando,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        ) {
                            if (vm.aprovando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Aprovar todos (${apostasConferir.size + rateiosConferir.size})")
                        }
                    }
                }
            }
        }

        if (semComprovante.isNotEmpty() || rateiosSemPix.isNotEmpty()) {
            item(key = "esperando") {
                Secao("Esperando comprovante") {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        semComprovante.forEachIndexed { i, a ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            LinhaEsperando(
                                emblema = { EmblemaModalidade(a.modalidade, a.concurso, 40.dp) },
                                titulo = "Aposta Nº${a.numero} · ${a.modalidade.nome}",
                                texto = "Registrada ${Datas.relativa(a.dataAposta, LocalDate.now()).lowercase()} · ${Dinheiro.rs(a.valor)}",
                                aoTocar = { abrir("aposta/${a.id}") },
                            )
                        }
                        rateiosSemPix.forEachIndexed { i, r ->
                            if (i > 0 || semComprovante.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            val faltam = estado.pessoas.filter { !r.pix.containsKey(it.id) }
                            LinhaEsperando(
                                emblema = { EmblemaDesenho(Desenhos.Moedas, 40.dp) },
                                titulo = "Rateio Nº${r.numeroNoMes} · ${Dinheiro.rs(r.cota)} para cada um",
                                texto = "Falta o Pix de " + faltam.joinToString(", ") { it.apelido },
                                aoTocar = null,
                            )
                        }
                    }
                }
            }
        }

        if (aEnviar.isNotEmpty()) {
            item(key = "enviar") {
                Secao("Enviar ao grupo") {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        aEnviar.forEachIndexed { i, c ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            LinhaEnviar(c, aoVer = { abrir("comprovante/${c.id}") }) {
                                escopo.launch { Compartilhar.comprovante(ctx, estado, c) }
                            }
                        }
                    }
                }
            }
        }

        if (nada && vm.escolhidos.isEmpty()) {
            item(key = "vazio") {
                TextoVazio("Tudo em dia: nenhum registro esperando comprovante, conferência ou envio.")
            }
        }
    }

    if (confirmarAprovacao) {
        val n = apostasConferir.size + rateiosConferir.size
        DialogoConfirmar(
            titulo = "Aprovar a conferência?",
            texto = "Os comprovantes anexados de $n registro(s) conferem com o app. Os comprovantes do bolão serão criados " +
                "e ficam prontos para enviar ao grupo. Depois disso não dá mais para descartar esses registros.",
            confirmar = "Aprovar",
            aoConfirmar = {
                confirmarAprovacao = false
                vm.aprovarTodos(ctx, avisar)
            },
            aoFechar = { confirmarAprovacao = false },
        )
    }

    vm.relatorio?.let { lista -> RelatorioDaLeitura(lista, aoFechar = { vm.relatorio = null }) }
}

/** Uma aposta com o comprovante da Caixa anexado: o que mudou, o anexo (ver ou tirar) e abrir a aposta. */
@Composable
private fun ConferirAposta(estado: Estado, a: Aposta, aoAbrir: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val escopo = rememberCoroutineScope()
    val arq = a.comprovante ?: return
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = aoAbrir)) {
            EmblemaModalidade(a.modalidade, a.concurso, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Aposta Nº${a.numero} · ${a.modalidade.nome}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    Operacoes.jogoDe(a).let { io.github.marcosfaria94.bolao.domain.Mensagens.linhasDoJogo(a.modalidade, it) }.replace("\n", " · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        a.notasDoAnexo.forEach { n ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = corApuracao(), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Spacer(Modifier.width(6.dp))
                Text(n, style = MaterialTheme.typography.bodySmall, color = corApuracao())
            }
        }
        LinhaDoAnexo(arq, aoVer = { Compartilhar.abrir(ctx, repo.arquivo(arq), arq.mime) }) {
            escopo.launch {
                runCatching { repo.atualizar { Operacoes.desanexarDaAposta(it, a.id) } }
                repo.apagarSeSolto(arq)
            }
        }
    }
}

/** Um rateio com todos os Pix anexados: o Pix de cada integrante (ver ou tirar). */
@Composable
private fun ConferirRateio(estado: Estado, r: Rateio) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val escopo = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemaDesenho(Desenhos.Moedas, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Rateio Nº${r.numeroNoMes} · ${Datas.relativa(r.data, LocalDate.now())}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${Dinheiro.rs(r.cota)} para cada um", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        estado.pessoas.forEach { p ->
            val arq = r.pix[p.id] ?: return@forEach
            LinhaDoAnexo(arq, rotulo = "Pix de ${p.apelido}", aoVer = { Compartilhar.abrir(ctx, repo.arquivo(arq), arq.mime) }) {
                escopo.launch {
                    runCatching { repo.atualizar { Operacoes.desanexarPix(it, r.numero, p.id) } }
                    repo.apagarSeSolto(arq)
                }
            }
        }
    }
}

/** Um anexo na conferência: o nome, "ver" e "tirar" (volta a esperar o comprovante). */
@Composable
private fun LinhaDoAnexo(arq: Arquivo, rotulo: String? = null, aoVer: () -> Unit, aoTirar: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = corEntrada(), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            if (rotulo != null) Text(rotulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(arq.nomeOriginal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = aoVer) { Icon(Icons.Outlined.Visibility, contentDescription = "Ver ${arq.nomeOriginal}") }
        IconButton(onClick = { confirmar = true }) { Icon(Icons.Outlined.Close, contentDescription = "Tirar ${arq.nomeOriginal}") }
    }
    if (confirmar) {
        DialogoConfirmar(
            titulo = "Tirar este comprovante?",
            texto = "O arquivo sai do app e o registro volta a esperar o comprovante.",
            confirmar = "Tirar",
            aoConfirmar = {
                confirmar = false
                aoTirar()
            },
            aoFechar = { confirmar = false },
        )
    }
}

@Composable
private fun LinhaEsperando(emblema: @Composable () -> Unit, titulo: String, texto: String, aoTocar: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier)
            .padding(vertical = 10.dp),
    ) {
        emblema()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Um comprovante do bolão pronto para ir ao grupo. */
@Composable
private fun LinhaEnviar(c: Comprovante, aoVer: () -> Unit, aoEnviar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.weight(1f).clickable(onClick = aoVer)) {
            Text(c.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Ainda não enviado ao grupo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = aoEnviar) {
            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Enviar")
        }
    }
}

/** O relatório do "Registrar comprovantes": o que foi anexado, alterado, descartado e o que ainda falta. */
@Composable
private fun RelatorioDaLeitura(avisos: List<Associacao.Aviso>, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Comprovantes registrados") },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (avisos.isEmpty()) Text("Nenhum arquivo foi lido.")
                avisos.forEach { a ->
                    val (icone, cor) = when {
                        a.problema -> Icons.Outlined.ErrorOutline to MaterialTheme.colorScheme.error
                        a.tipo == Associacao.Tipo.ALTERADA -> Icons.Outlined.Info to corApuracao()
                        a.tipo == Associacao.Tipo.AINDA_FALTA -> Icons.Outlined.Info to MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Icons.Outlined.CheckCircle to corEntrada()
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(a.texto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (avisos.any { it.tipo == Associacao.Tipo.ANEXADO || it.tipo == Associacao.Tipo.ALTERADA || it.tipo == Associacao.Tipo.PIX_ANEXADO }) {
                    Aviso("Confira os anexos na Conferência e toque em \"Aprovar todos\".")
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("OK") } },
    )
}

package io.github.marcosfaria94.bolao.ui.sessoes

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Sessao
import io.github.marcosfaria94.bolao.data.Sessoes
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import io.github.marcosfaria94.bolao.ui.comum.CoresEmblema
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.tema.AzulMarinho
import io.github.marcosfaria94.bolao.ui.tema.CoresAlerta
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import io.github.marcosfaria94.bolao.ui.tema.VermelhoTeste
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A tela das sessões (o botão "Alternar sessão", ao lado do Painel de Jogos; desde a 0.21). */
const val ROTA_SESSOES = "sessoes"

/** O título da tela das sessões. */
const val TITULO_SESSOES = "Sessões"

/** A tela da extinção da sessão (o "Excluir" da tela das sessões é um atalho para ela). */
private const val ROTA_EXTINCAO = "ajustes/extincao"

/** "1 sessão criada", "3 sessões criadas". */
fun sessoesCriadas(n: Int): String = if (n == 1) "1 sessão criada" else "$n sessões criadas"

/**
 * As sessões (desde a 0.21): quantas foram criadas e cada uma (o nome, os integrantes e o saldo, a
 * que está em uso), o modo de teste à parte e o "Criar sessão". Tocar numa sessão passa a usá-la; o
 * menu de cada uma renomeia (só os bolões) e exclui: o "Excluir" é um atalho para a extinção dela (o
 * "Apagar tudo" no Individual; uma sessão sem o cadastro concluído é apagada na hora). A raiz cuida de
 * sair (puxando para baixo ou deslizando para os lados).
 */
@Composable
fun SessoesTela(abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val sessoes = ctx.bolao.sessoes
    val lista by sessoes.lista.collectAsStateWithLifecycle()
    val ativa by sessoes.ativa.collectAsStateWithLifecycle()
    val preparandoTeste by ModoTeste.trabalhando.collectAsStateWithLifecycle()
    var renomear by remember { mutableStateOf<Sessao?>(null) }
    var apagarVazia by remember { mutableStateOf<Sessao?>(null) }

    LazyColumn(contentPadding = PaddingTela, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item(key = "contagem") {
            Column(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)) {
                Text(sessoesCriadas(lista.size), style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp), fontWeight = FontWeight.Bold)
                Text(
                    "Cada sessão é um bolão com os seus dados, ajustes e comprovantes. Toque numa sessão para usá-la.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "lista") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                lista.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    val estado by sessoes.repo(s.id).estado.collectAsStateWithLifecycle()
                    LinhaDaSessao(
                        s, estado, emUso = s.id == ativa,
                        aoTocar = { if (s.id != ativa) usarSessao(ctx, s.id) },
                        aoRenomear = { renomear = s }.takeIf { !s.individual && s.nome.isNotBlank() },
                        aoExcluir = {
                            when {
                                // Sem cadastro: nada foi registrado, apaga na hora (com a confirmação).
                                estado?.cadastroConcluido != true -> apagarVazia = s
                                s.id == ativa -> abrir(ROTA_EXTINCAO)
                                else -> usarSessao(ctx, s.id, destino = ROTA_EXTINCAO)
                            }
                        },
                    )
                }
            }
        }
        item(key = "titulo-teste") {
            Text(
                "TESTE",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 1.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp),
            )
        }
        item(key = "teste") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val emTeste = ativa == Sessoes.TESTE
                Linha(
                    emblema = { EmblemaIcone(Icons.Outlined.Science, VermelhoTeste, TAMANHO_DO_EMBLEMA) },
                    titulo = Sessoes.NOME_DO_TESTE,
                    subtitulo = when {
                        preparandoTeste -> "Preparando o exemplo..."
                        emTeste -> "Dados de exemplo. Trocar de sessão apaga o exemplo."
                        else -> "Um mês de exemplo, separado das sessões. Ao sair, o exemplo é apagado."
                    },
                    emUso = emTeste,
                    aoTocar = { if (!emTeste && !preparandoTeste) ModoTeste.entrar(ctx) },
                    final = if (preparandoTeste) {
                        { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
                    } else {
                        null
                    },
                )
            }
        }
        item(key = "criar") {
            Button(
                onClick = { criarSessao(ctx) },
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Criar sessão", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    renomear?.let { s ->
        DialogoRenomear(s, aoFechar = { renomear = null })
    }
    apagarVazia?.let { s ->
        DialogoConfirmar(
            titulo = "Excluir esta sessão?",
            texto = "O cadastro de \"${sessoes.nome(s.id)}\" não foi concluído: nada foi registrado nela.",
            confirmar = "Excluir",
            aoConfirmar = {
                apagarVazia = null
                ctx.bolao.escopo.launch { runCatching { sessoes.excluir(s.id) } }
            },
            aoFechar = { apagarVazia = null },
        )
    }
}

/** Uma sessão da lista: o emblema, o nome, os integrantes e o saldo, "Em uso" e o menu (renomear, excluir). */
@Composable
private fun LinhaDaSessao(
    s: Sessao,
    estado: Estado?,
    emUso: Boolean,
    aoTocar: () -> Unit,
    aoRenomear: (() -> Unit)?,
    aoExcluir: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    // O que foi escolhido no menu acontece depois que ele fecha (o menu e a janela seguinte não se sobrepõem).
    var depois by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(depois) {
        val acao = depois ?: return@LaunchedEffect
        delay(DEPOIS_DO_MENU_MS)
        depois = null
        acao()
    }
    Linha(
        emblema = { EmblemaIcone(if (s.individual) Icons.Outlined.Person else Icons.Outlined.Groups, corDaSessao(s), TAMANHO_DO_EMBLEMA) },
        titulo = s.nome.ifBlank { Sessoes.NOME_SEM_CADASTRO },
        subtitulo = resumoDaSessao(s, estado),
        emUso = emUso,
        aoTocar = aoTocar,
        final = {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.semantics { contentDescription = "Opções de ${s.nome.ifBlank { Sessoes.NOME_SEM_CADASTRO }}" }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (aoRenomear != null) {
                        DropdownMenuItem(
                            text = { Text("Renomear") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menu = false
                                depois = aoRenomear
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Excluir", color = CoresAlerta.texto()) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = CoresAlerta.texto()) },
                        onClick = {
                            menu = false
                            depois = aoExcluir
                        },
                    )
                }
            }
        },
    )
}

/** A cor do emblema: o verde dos bolões e o dourado do Individual; cinza antes do cadastro. */
private fun corDaSessao(s: Sessao): Color = when {
    s.modo == null -> Color(0xFF7A8696)
    s.individual -> CoresEmblema.PREMIO
    else -> CoresEmblema.APORTE
}

/** "3 integrantes · saldo R$ 12,30", "Individual · saldo -R$ 9,00", "Cadastro não concluído"... */
fun resumoDaSessao(s: Sessao, e: Estado?): String = when {
    e == null -> "Carregando..."
    !e.cadastroConcluido -> "Cadastro não concluído"
    e.extincao != null -> "Extinção em andamento"
    s.individual || e.individual -> "Individual · saldo ${Dinheiro.rs(Operacoes.saldo(e))}"
    else -> "${e.pessoas.size} integrantes · saldo ${Dinheiro.rs(Operacoes.saldo(e))}"
}

private val TAMANHO_DO_EMBLEMA = 44.dp

/** Quanto esperar o menu da sessão fechar antes de abrir o que foi escolhido nele. */
private const val DEPOIS_DO_MENU_MS = 220L

/** Uma linha das listas das sessões. */
@Composable
private fun Linha(
    emblema: @Composable () -> Unit,
    titulo: String,
    subtitulo: String,
    emUso: Boolean,
    aoTocar: () -> Unit,
    final: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        emblema()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (emUso) {
                    Spacer(Modifier.width(8.dp))
                    EtiquetaEmUso()
                }
            }
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (final != null) final() else Spacer(Modifier.width(10.dp))
    }
}

/** "Em uso", em dourado. */
@Composable
private fun EtiquetaEmUso() {
    Text(
        "Em uso",
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        fontWeight = FontWeight.Bold,
        color = AzulMarinho,
        maxLines = 1,
        modifier = Modifier.background(Dourado, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** Renomear um bolão: vazio volta ao nome genérico ("Bolão 2"). */
@Composable
private fun DialogoRenomear(s: Sessao, aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    val sessoes = ctx.bolao.sessoes
    var nome by remember { mutableStateOf(s.nome) }
    val generico = remember(s) { sessoes.nomeGenerico(exceto = s.id) }
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Renomear") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(Sessoes.TAMANHO_DO_NOME) },
                    label = { Text("Nome do bolão") },
                    placeholder = { Text(generico) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Vazio: \"$generico\".", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching { sessoes.renomear(s.id, nome) }
                aoFechar()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
    )
}

/**
 * Passa a usar a sessão [id] (saindo do modo de teste, o exemplo é apagado) e, se houver, abre o
 * [destino] nela (o atalho para a extinção).
 */
fun usarSessao(ctx: Context, id: String, destino: String? = null) {
    val app = ctx.bolao
    app.escopo.launch {
        try {
            if (app.sessoes.emTeste) {
                app.sessoes.sairDoTeste(id)
                Vigia.esquecerAvisosDeTeste(app)
            } else {
                app.sessoes.trocarPara(id)
            }
            destino?.let { app.destinoPendente.value = it }
        } catch (t: Throwable) {
            app.repo.avisar("Não foi possível trocar de sessão: ${t.message}")
        }
    }
}

/** Cria uma sessão (vai para o cadastro); no modo de teste, o exemplo é apagado antes. */
fun criarSessao(ctx: Context) {
    val app = ctx.bolao
    app.escopo.launch {
        try {
            if (app.sessoes.emTeste) {
                app.sessoes.sairDoTeste()
                Vigia.esquecerAvisosDeTeste(app)
            }
            app.sessoes.criar()
        } catch (t: Throwable) {
            app.repo.avisar("Não foi possível criar a sessão: ${t.message}")
        }
    }
}

/**
 * Apaga a sessão [id] (fim da extinção, "Apagar tudo", extinção forçada) e mostra o aviso na sessão
 * que fica em uso: [aviso] recebe se ainda há outra sessão (com o nome dela) ou se o app foi para um
 * cadastro novo (null).
 */
suspend fun apagarSessao(ctx: Context, id: String, aviso: (outra: String?) -> String) = withContext(NonCancellable) {
    // A tela de quem pediu sai junto com a sessão: o resto não pode parar no meio.
    val app = ctx.bolao
    app.sessoes.excluir(id)
    Vigia.esquecerAvisosDaSessao(app, id)
    val outra = app.sessoes.sessao(app.sessoes.ativa.value)?.takeIf { it.nome.isNotBlank() }?.nome
    app.repo.avisar(aviso(outra))
}

/** O botão "Alternar sessão" na barra de cima das abas, ao lado do Painel de Jogos (desde a 0.21). */
@Composable
fun BotaoDaSessao(aoTocar: () -> Unit) {
    IconButton(onClick = aoTocar, modifier = Modifier.size(58.dp).semantics(mergeDescendants = true) { contentDescription = "Alternar sessão" }) {
        IconeDaSessao(38.dp)
    }
}

/** O ícone das sessões: o quadrado azul-marinho com as setas douradas, como o do Painel de Jogos. */
@Composable
fun IconeDaSessao(tamanho: Dp = 38.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(tamanho).background(AzulMarinho, RoundedCornerShape(tamanho * 0.29f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icones.Sessoes, contentDescription = null, tint = Dourado, modifier = Modifier.size(tamanho * 0.70f))
    }
}

/**
 * Trocar de sessão sem sair da tela em que se está presa (a extinção em andamento de outra sessão):
 * a lista das sessões, para usar outra.
 */
@Composable
fun DialogoTrocarSessao(aoFechar: () -> Unit) {
    val ctx = LocalContext.current
    val sessoes = ctx.bolao.sessoes
    val lista by sessoes.lista.collectAsStateWithLifecycle()
    val ativa by sessoes.ativa.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = aoFechar,
        icon = { IconeDaSessao(40.dp) },
        title = { Text("Alternar sessão") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                lista.filter { it.id != ativa }.forEach { s ->
                    val estado by sessoes.repo(s.id).estado.collectAsStateWithLifecycle()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                aoFechar()
                                usarSessao(ctx, s.id)
                            }
                            .padding(vertical = 10.dp),
                    ) {
                        EmblemaIcone(if (s.individual) Icons.Outlined.Person else Icons.Outlined.Groups, corDaSessao(s), 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(s.nome.ifBlank { Sessoes.NOME_SEM_CADASTRO }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(resumoDaSessao(s, estado), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = {
                    aoFechar()
                    criarSessao(ctx)
                }) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Criar sessão")
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
    )
}

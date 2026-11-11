package io.github.marcosfaria94.bolao.ui.sessoes

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
 * As sessões (desde a 0.21): quantas foram criadas e cada uma no seu cartão (desde a 0.24: o nome numa
 * linha só para ele; o tipo, formal, informal ou Individual; os integrantes e o saldo; o "Em uso"; e os
 * ícones de renomear e excluir, no lugar do menu de três pontinhos), o modo de teste à parte e o
 * "Criar sessão". Tocar numa sessão passa a usá-la; tocar na que já está em uso (desde a 0.23) volta
 * para a tela de antes ([aoVoltar]). Renomear: só os bolões. Excluir é um atalho para a extinção da
 * sessão (o "Apagar tudo" no Individual; uma sessão sem o cadastro concluído é apagada na hora). A raiz
 * cuida de sair (puxando para baixo ou deslizando para os lados).
 */
@Composable
fun SessoesTela(abrir: (String) -> Unit, aoVoltar: () -> Unit = {}) {
    val ctx = LocalContext.current
    val sessoes = ctx.bolao.sessoes
    val lista by sessoes.lista.collectAsStateWithLifecycle()
    val ativa by sessoes.ativa.collectAsStateWithLifecycle()
    val preparandoTeste by ModoTeste.trabalhando.collectAsStateWithLifecycle()
    var renomear by remember { mutableStateOf<Sessao?>(null) }
    var apagarVazia by remember { mutableStateOf<Sessao?>(null) }

    LazyColumn(contentPadding = PaddingTela, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item(key = "contagem") {
            Column(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)) {
                Text(sessoesCriadas(lista.size), style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp), fontWeight = FontWeight.Bold)
                Text(
                    "Cada sessão é um bolão com os seus dados, ajustes e comprovantes. Toque numa sessão para usá-la.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Cada sessão no seu cartão (desde a 0.24): o nome numa linha só, o tipo, os integrantes e o saldo,
        // e embaixo o "Em uso" e os ícones de renomear e excluir.
        items(lista, key = { "sessao-${it.id}" }) { s ->
            val estado by sessoes.repo(s.id).estado.collectAsStateWithLifecycle()
            val nome = s.nome.ifBlank { Sessoes.NOME_SEM_CADASTRO }
            CartaoDeSessao(
                titulo = nome,
                emblema = { EmblemaIcone(if (s.individual) Icons.Outlined.Person else Icons.Outlined.Groups, corDaSessao(s), TAMANHO_DO_EMBLEMA) },
                linhas = linhasDaSessao(s, estado),
                emUso = s.id == ativa,
                aoTocar = { if (s.id != ativa) usarSessao(ctx, s.id) else aoVoltar() },
                acoes = {
                    // Renomear: só os bolões (o Individual tem o nome fixo; a sessão sem cadastro ainda não tem nome).
                    if (!s.individual && s.nome.isNotBlank()) {
                        IconButton(onClick = { renomear = s }, modifier = Modifier.semantics { contentDescription = "Renomear $nome" }) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(
                        onClick = {
                            when {
                                // Sem cadastro: nada foi registrado, apaga na hora (com a confirmação).
                                estado?.cadastroConcluido != true -> apagarVazia = s
                                s.id == ativa -> abrir(ROTA_EXTINCAO)
                                else -> usarSessao(ctx, s.id, destino = ROTA_EXTINCAO)
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Excluir $nome" },
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = CoresAlerta.texto())
                    }
                },
            )
        }
        item(key = "titulo-teste") {
            Text(
                "TESTE",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 1.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
        }
        item(key = "teste") {
            val emTeste = ativa == Sessoes.TESTE
            CartaoDeSessao(
                titulo = Sessoes.NOME_DO_TESTE,
                emblema = { EmblemaIcone(Icons.Outlined.Science, VermelhoTeste, TAMANHO_DO_EMBLEMA) },
                linhas = listOf(
                    LinhaDoCartao(
                        when {
                            preparandoTeste -> "Preparando o exemplo..."
                            emTeste -> "Dados de exemplo. Trocar de sessão apaga o exemplo."
                            else -> "Um mês de exemplo, separado das sessões. Ao sair, o exemplo é apagado."
                        },
                    ),
                ),
                emUso = emTeste,
                // O modo de teste já em uso: volta, como as sessões.
                aoTocar = {
                    when {
                        emTeste -> aoVoltar()
                        !preparandoTeste -> ModoTeste.entrar(ctx)
                    }
                },
                final = if (preparandoTeste) {
                    { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
                } else {
                    null
                },
            )
        }
        item(key = "criar") {
            Button(
                onClick = { criarSessao(ctx) },
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
    s.individual || e.individual -> "Individual · saldo ${semQuebra(Dinheiro.rs(Operacoes.saldo(e)))}"
    else -> "${e.pessoas.size} integrantes · saldo ${semQuebra(Dinheiro.rs(Operacoes.saldo(e)))}"
}

/** O valor não quebra entre o "R$" e o número (com a letra grande do celular). */
private fun semQuebra(valor: String): String = valor.replace(' ', '\u00A0')

private val TAMANHO_DO_EMBLEMA = 44.dp

/** O tipo da sessão Individual no cartão. */
private const val TIPO_INDIVIDUAL = "Modo individual"

/** Uma linha de texto do cartão da sessão: [principal] em negrito (o tipo); [alerta] em vermelho. */
data class LinhaDoCartao(val texto: String, val principal: Boolean = false, val alerta: Boolean = false)

/**
 * "Bolão formal", "Bolão informal" ou "Modo individual" (null antes de carregar ou sem o cadastro
 * concluído). O Individual diz "Modo individual" para não repetir o nome da sessão, que é "Individual".
 */
fun tipoDaSessao(s: Sessao, e: Estado?): String? = when {
    e == null || !e.cadastroConcluido -> null
    s.individual || e.individual -> TIPO_INDIVIDUAL
    e.informal -> "Bolão informal"
    else -> "Bolão formal"
}

/**
 * As linhas do cartão da sessão (desde a 0.24), uma informação por linha para a leitura ficar fácil:
 * o tipo (formal, informal ou Individual), os integrantes (só no bolão), o saldo e, se houver, a
 * extinção em andamento.
 */
fun linhasDaSessao(s: Sessao, e: Estado?): List<LinhaDoCartao> {
    if (e == null) return listOf(LinhaDoCartao("Carregando..."))
    if (!e.cadastroConcluido) return listOf(LinhaDoCartao("Cadastro não concluído"))
    val tipo = tipoDaSessao(s, e) ?: return emptyList()
    return listOfNotNull(
        LinhaDoCartao(tipo, principal = true),
        if (tipo == TIPO_INDIVIDUAL) null else LinhaDoCartao(if (e.pessoas.size == 1) "1 integrante" else "${e.pessoas.size} integrantes"),
        LinhaDoCartao("Saldo: ${semQuebra(Dinheiro.rs(Operacoes.saldo(e)))}"),
        if (e.extincao != null) LinhaDoCartao("Extinção em andamento", alerta = true) else null,
    )
}

/**
 * O cartão de uma sessão (e do modo de teste), desde a 0.24: o nome numa linha inteira só para ele
 * (sem quebrar: o que não cabe termina em "..."); embaixo, o emblema e as [linhas]; no rodapé, o "Em
 * uso" (o cartão em uso também tem o contorno dourado) e as [acoes] (os ícones de renomear e excluir).
 * Tocar no cartão passa a usar a sessão ([aoTocar]).
 */
@Composable
private fun CartaoDeSessao(
    titulo: String,
    emblema: @Composable () -> Unit,
    linhas: List<LinhaDoCartao>,
    emUso: Boolean,
    aoTocar: () -> Unit,
    acoes: (@Composable RowScope.() -> Unit)? = null,
    final: (@Composable () -> Unit)? = null,
) {
    val rodape = emUso || acoes != null
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(18.dp),
        border = if (emUso) BorderStroke(2.dp, Dourado) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = aoTocar)
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = if (rodape) 4.dp else 14.dp),
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                emblema()
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    linhas.forEach { l ->
                        Text(
                            l.texto,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
                            fontWeight = if (l.principal || l.alerta) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                l.alerta -> CoresAlerta.texto()
                                l.principal -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                if (final != null) {
                    Spacer(Modifier.width(8.dp))
                    final()
                    Spacer(Modifier.width(8.dp))
                }
            }
            if (rodape) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                    if (emUso) EtiquetaEmUso()
                    Spacer(Modifier.weight(1f))
                    acoes?.invoke(this)
                }
            }
        }
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

package io.github.marcosfaria94.bolao.ui.ajustes

import io.github.marcosfaria94.bolao.ui.comum.VoltarAoTopo
import androidx.compose.foundation.lazy.rememberLazyListState
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import io.github.marcosfaria94.bolao.notificacoes.Toque
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Fingerprint
import io.github.marcosfaria94.bolao.seguranca.Biometria
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.marcosfaria94.bolao.notificacoes.SegundoPlano
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.net.Atualizador
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.pdf.AuditoriaPdf
import io.github.marcosfaria94.bolao.pdf.PdfsDoApp
import io.github.marcosfaria94.bolao.ui.atualizacao.AtualizacaoViewModel
import io.github.marcosfaria94.bolao.ui.comum.CampoData
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.CoresEmblema
import io.github.marcosfaria94.bolao.ui.comum.Desenhos
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.DialogoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.EmblemaDesenho
import io.github.marcosfaria94.bolao.ui.comum.EmblemaIcone
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.Secao
import io.github.marcosfaria94.bolao.ui.comum.TarjaTitulo
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.comum.copiar
import io.github.marcosfaria94.bolao.ui.tema.CoresAlerta
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import io.github.marcosfaria94.bolao.ui.teste.ModoTeste
import io.github.marcosfaria94.bolao.ui.atualizacao.EtapaAtualizacao
import androidx.compose.material3.Switch
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Endereço das telas dos itens do menu dos Ajustes ("ajustes/notificacoes", "ajustes/termo"...). */
const val ROTA_SECAO_AJUSTES = "ajustes/{secao}"

/**
 * Os itens da lista dos Ajustes: quase todos abrem a sua tela (que fecha deslizando o dedo para os
 * lados ou puxando para baixo). A Integridade abre direto a "Verificar integridade". Desde a 0.16 a
 * Atualização (tocar já verifica) e o Modo de teste (liga e desliga na chave) ficam direto na lista,
 * e os Créditos viraram o rodapé da lista. Desde a 0.23 os Integrantes abrem e fecham ali mesmo, na
 * lista (começam abertos), e o "Aporte e preços" está dentro dos Parâmetros. Desde a 0.24 a extinção
 * do bolão fica no fim dos Integrantes abertos (fora dos grupos).
 */
enum class SecaoAjuste(val id: String) {
    MODO("modo"),
    INTEGRANTES("integrantes"),
    /**
     * Os parâmetros da sessão (desde a 0.22): confirmação, mais números, teimosinha e o calendário de
     * apostas; desde a 0.23, também o aporte mensal e os preços (até a 0.22, a tela "Aporte e preços").
     */
    PARAMETROS("parametros"),
    TERMO("termo"),
    INTEGRIDADE("integridade"),
    AUDITORIA("auditoria"),
    BACKUP("backup"),
    /** A apuração automática (avisos, bateria, inicialização) e o toque do prêmio, numa tela só. */
    NOTIFICACOES("notificacoes"),
    /** O bloqueio do app com a biometria (desde a 0.30, no grupo Celular): pedir ou não, e depois de quanto tempo fora. */
    BIOMETRIA("biometria"),
    /** Fora da lista desde a 0.26: o app confere sozinho, ao abrir, se há versão nova ([AtualizacaoViewModel]). */
    ATUALIZACAO("atualizacao"),
    TESTE("teste"),
    EXTINCAO("extincao"),
    ;

    /** Abre uma tela própria (a Atualização e o Modo de teste agem ali mesmo, na lista). */
    val temTela: Boolean get() = this != ATUALIZACAO && this != TESTE

    /** Título da tela (na barra de cima) e da linha da lista. */
    fun titulo(individual: Boolean): String = when (this) {
        MODO -> "Modo"
        INTEGRANTES -> "Integrantes"
        PARAMETROS -> "Parâmetros"
        TERMO -> "Termo"
        INTEGRIDADE -> "Integridade"
        AUDITORIA -> "Auditoria"
        BACKUP -> "Backup"
        NOTIFICACOES -> "Notificações"
        BIOMETRIA -> "Biometria"
        ATUALIZACAO -> "Atualização"
        TESTE -> "Modo de teste"
        EXTINCAO -> if (individual) "Apagar tudo" else "Extinção do Bolão"
    }

    /** Para onde a linha leva. */
    val rota: String get() = if (this == INTEGRIDADE) "integridade" else "ajustes/$id"

    companion object {
        fun de(id: String?): SecaoAjuste? = when (id) {
            // Até a 0.14, a apuração automática e o toque do prêmio eram duas telas.
            "apuracao", "toque" -> NOTIFICACOES
            // Até a 0.22, o "Aporte e preços" era uma tela (agora fica nos Parâmetros).
            "valores" -> PARAMETROS
            else -> entries.firstOrNull { it.id == id && it.temTela }
        }

        /**
         * Os grupos da lista, em ordem (o individual não tem integrantes, termo, integridade nem
         * auditoria). Desde a 0.24: a extinção do bolão fica no fim dos Integrantes abertos (não está em
         * grupo nenhum: a [AjustesTela] desenha ela lá); no individual, o "Apagar tudo" fica no fim do
         * "Apostas" (e ele não tem o grupo Segurança); o Backup fica no Celular. Desde a 0.23 o termo
         * fica na Segurança e o aporte e os preços, nos Parâmetros. Os Créditos ficam no rodapé da lista.
         * Desde a 0.21 o Modo de teste saiu daqui: é uma sessão à parte, na tela das sessões.
         */
        fun grupos(individual: Boolean, informal: Boolean = false): List<Pair<String, List<SecaoAjuste>>> = listOfNotNull(
            if (individual) "APOSTAS" to listOf(MODO, PARAMETROS, EXTINCAO) else "BOLÃO" to listOf(INTEGRANTES, PARAMETROS),
            when {
                individual -> null
                // O bolão informal (desde a 0.22) não tem termo.
                informal -> "SEGURANÇA" to listOf(INTEGRIDADE, AUDITORIA)
                else -> "SEGURANÇA" to listOf(TERMO, INTEGRIDADE, AUDITORIA)
            },
            // Sem a Atualização desde a 0.26 (pedido do usuário): o app confere sozinho ao abrir e mostra a janela da versão nova.
            // A Biometria desde a 0.30 (pedido do usuário; antes o app sempre pedia, sem ajuste).
            "CELULAR" to listOf(NOTIFICACOES, BIOMETRIA, BACKUP),
        )
    }
}

private fun iconeDoItem(s: SecaoAjuste): ImageVector = when (s) {
    SecaoAjuste.MODO -> Icons.Outlined.Person
    SecaoAjuste.INTEGRANTES -> Icons.Outlined.Groups
    SecaoAjuste.PARAMETROS -> Icons.Outlined.Tune
    SecaoAjuste.TERMO -> Icons.Outlined.HistoryEdu
    SecaoAjuste.INTEGRIDADE -> Icons.Outlined.VerifiedUser
    SecaoAjuste.AUDITORIA -> Icons.Outlined.PictureAsPdf
    SecaoAjuste.BACKUP -> Icons.Outlined.Backup
    SecaoAjuste.NOTIFICACOES -> Icons.Outlined.NotificationsActive
    SecaoAjuste.BIOMETRIA -> Icons.Outlined.Fingerprint
    SecaoAjuste.ATUALIZACAO -> Icons.Outlined.SystemUpdate
    SecaoAjuste.TESTE -> Icons.Outlined.Science
    SecaoAjuste.EXTINCAO -> Icons.Outlined.DeleteForever
}

/** Tom da situação, à direita do nome. */
private enum class TomItem { NORMAL, ALERTA, DESTAQUE }

/**
 * Ajustes: uma lista em grupos, como os Ajustes do Android (Bolão, Segurança e Celular), com os
 * Créditos no rodapé ("Criado com Claude", a versão e o "Tecnologia"). Cada linha tem o ícone,
 * o nome, a situação à direita (ex.: "3 pessoas", "Falta anexar" em vermelho) e a seta, e abre a sua
 * tela (desde a 0.26 sem a linha da Atualização: o app confere sozinho ao abrir); os Integrantes (desde a
 * 0.23) abrem e fecham embaixo da linha, com a seta para baixo, e começam abertos. Tocar em "Ajustes"
 * na barra de baixo, já nos Ajustes ([paraOTopo]), volta a lista ao começo.
 */
@Composable
fun AjustesTela(estado: Estado, atualizacao: AtualizacaoViewModel, abrir: (String) -> Unit, paraOTopo: Int = 0) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val ultima by atualizacao.ultima.collectAsStateWithLifecycle()
    val etapa by atualizacao.etapa.collectAsStateWithLifecycle()
    val teste by repo.modoTeste.collectAsStateWithLifecycle()
    val preparandoTeste by ModoTeste.trabalhando.collectAsStateWithLifecycle()
    // A situação dos avisos, da bateria e do toque é lida de novo ao voltar (dos ajustes do Android ou de uma tela dos Ajustes).
    var conferido by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        conferido++
        onPauseOrDispose {}
    }
    val liberada = remember(conferido) { Notificador.permitido(ctx) && SegundoPlano.bateriaLivre(ctx) }
    val toqueLigado = remember(conferido, estado) { Toque.config(ctx).ligado }
    val bloqueio = remember(conferido) { Biometria.bloqueio(ctx) }
    val celularComBloqueio = remember(conferido) { Biometria.disponivel(ctx) }
    val individual = estado.individual

    fun situacao(s: SecaoAjuste): Pair<String, TomItem> = when (s) {
        SecaoAjuste.MODO -> "Individual" to TomItem.NORMAL
        SecaoAjuste.INTEGRANTES -> "${estado.pessoas.size} pessoas" + (if (estado.informal) " · informal" else "") to TomItem.NORMAL
        SecaoAjuste.PARAMETROS -> resumoDosParametros(estado.parametros) to TomItem.NORMAL
        SecaoAjuste.TERMO ->
            if (estado.termo != null && estado.termoSemAssinatura != null) "Anexado" to TomItem.NORMAL else "Falta anexar" to TomItem.ALERTA
        SecaoAjuste.INTEGRIDADE -> "Verificar" to TomItem.NORMAL
        SecaoAjuste.AUDITORIA -> "Gerar PDF" to TomItem.NORMAL
        SecaoAjuste.BACKUP -> "Cópia de tudo" to TomItem.NORMAL
        SecaoAjuste.NOTIFICACOES -> when {
            !liberada -> "Falta liberar" to TomItem.ALERTA
            toqueLigado -> "Toque ligado" to TomItem.NORMAL
            else -> "Toque desligado" to TomItem.NORMAL
        }
        SecaoAjuste.BIOMETRIA -> when {
            !celularComBloqueio -> SEM_BLOQUEIO_NO_CELULAR to TomItem.ALERTA
            bloqueio.pedir -> "Ligada" to TomItem.NORMAL
            else -> "Desligada" to TomItem.NORMAL
        }
        SecaoAjuste.ATUALIZACAO -> when {
            etapa is EtapaAtualizacao.Verificando -> "Verificando..." to TomItem.NORMAL
            (ultima?.codigo ?: 0) > Atualizador.versaoInstalada -> "Nova versão" to TomItem.DESTAQUE
            ultima != null -> "Em dia" to TomItem.NORMAL
            else -> "Verificar" to TomItem.NORMAL
        }
        SecaoAjuste.TESTE -> (if (preparandoTeste) "Preparando..." else "") to TomItem.NORMAL
        SecaoAjuste.EXTINCAO -> "" to TomItem.NORMAL
    }

    // Os integrantes abrem e fecham ali mesmo, na lista (desde a 0.23); começam abertos.
    var integrantesAbertos by rememberSaveable { mutableStateOf(true) }

    /**
     * O que tocar na linha faz: abre a tela; os Integrantes abrem e fecham na lista; a Atualização
     * verifica na hora; o Modo de teste liga ou desliga.
     */
    fun tocar(s: SecaoAjuste) {
        when (s) {
            SecaoAjuste.INTEGRANTES -> integrantesAbertos = !integrantesAbertos
            SecaoAjuste.ATUALIZACAO -> atualizacao.verificar(manual = true)
            SecaoAjuste.TESTE -> if (!preparandoTeste) {
                if (teste) ModoTeste.sair(ctx) else ModoTeste.entrar(ctx)
            }
            else -> abrir(s.rota)
        }
    }
    val lista = rememberLazyListState()
    VoltarAoTopo(lista, paraOTopo)

    LazyColumn(state = lista, contentPadding = PaddingTela, modifier = Modifier.fillMaxSize()) {
        SecaoAjuste.grupos(individual, estado.informal).forEachIndexed { g, (nome, secoes) ->
            item(key = "grupo-$nome") {
                Text(
                    nome,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 1.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = if (g == 0) 8.dp else 20.dp, bottom = 6.dp),
                )
            }
            item(key = "linhas-$nome") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    secoes.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = RECUO_DA_LISTA), color = MaterialTheme.colorScheme.outlineVariant)
                        val (texto, tom) = situacao(s)
                        LinhaDaLista(
                            titulo = s.titulo(individual),
                            situacao = texto,
                            tom = tom,
                            icone = iconeDoItem(s),
                            vermelho = s == SecaoAjuste.EXTINCAO,
                            final = when (s) {
                                // A chave do modo de teste (tocar na linha também liga e desliga).
                                SecaoAjuste.TESTE -> ({
                                    // Sem a margem mínima de toque (a linha inteira já liga e desliga): a linha fica da altura das outras.
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                        Switch(
                                            checked = teste,
                                            onCheckedChange = { tocar(s) },
                                            enabled = !preparandoTeste,
                                            modifier = Modifier.height(ALTURA_DA_CHAVE).semantics { contentDescription = "Modo de teste" },
                                        )
                                    }
                                })
                                // A atualização não abre tela: verifica ali mesmo (sem a seta).
                                SecaoAjuste.ATUALIZACAO -> ({})
                                else -> null
                            },
                            // Os integrantes abrem embaixo da linha: a seta para baixo (aberta, para cima).
                            seta = when {
                                s != SecaoAjuste.INTEGRANTES -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
                                integrantesAbertos -> Icons.Outlined.ExpandLess
                                else -> Icons.Outlined.ExpandMore
                            },
                            descricaoDaSeta = when {
                                s != SecaoAjuste.INTEGRANTES -> null
                                integrantesAbertos -> "Fechar os integrantes"
                                else -> "Abrir os integrantes"
                            },
                        ) { tocar(s) }
                        if (s == SecaoAjuste.INTEGRANTES && integrantesAbertos) {
                            // Cada integrante embaixo da linha, alinhado com o nome dela, com o copiar da chave Pix.
                            estado.pessoas.forEach { p ->
                                HorizontalDivider(Modifier.padding(start = RECUO_DA_LISTA), color = MaterialTheme.colorScheme.outlineVariant)
                                Box(Modifier.padding(start = RECUO_DA_LISTA, end = 4.dp)) {
                                    ItemIntegrante(p) { copiar(ctx, "Chave Pix", p.chavePix) }
                                }
                            }
                            // No fim dos integrantes, a extinção do bolão (desde a 0.24), com o ícone alinhado com os nomes.
                            HorizontalDivider(Modifier.padding(start = RECUO_DA_LISTA), color = MaterialTheme.colorScheme.outlineVariant)
                            Box(Modifier.padding(start = RECUO_DA_LISTA - RECUO_DO_ICONE)) {
                                LinhaDaLista(
                                    titulo = SecaoAjuste.EXTINCAO.titulo(individual),
                                    situacao = "",
                                    tom = TomItem.NORMAL,
                                    icone = iconeDoItem(SecaoAjuste.EXTINCAO),
                                    vermelho = true,
                                ) { tocar(SecaoAjuste.EXTINCAO) }
                            }
                        }
                    }
                }
            }
        }
        // Os Créditos, no rodapé: "Criado usando o Claude" (a Play Store), a versão e o "Tecnologia".
        item(key = "creditos") { CreditosNoRodape() }
    }
}

/** Altura da chave do modo de teste na lista (a da linha, para ela não ficar mais alta que as outras). */
private val ALTURA_DA_CHAVE = 24.dp

/** Onde começa o nome nas linhas da lista dos Ajustes (depois do ícone): os integrantes abertos ficam alinhados com ele. */
private val RECUO_DA_LISTA = 54.dp

/** A margem antes do ícone nas linhas da lista dos Ajustes. */
private val RECUO_DO_ICONE = 16.dp

/**
 * Uma linha da lista dos Ajustes: o ícone, o nome, a situação à direita e a [seta] (a de abrir a tela;
 * nos Integrantes, a de abrir e fechar embaixo da linha), ou o que vier em [final].
 */
@Composable
private fun LinhaDaLista(
    titulo: String,
    situacao: String,
    tom: TomItem,
    icone: ImageVector,
    vermelho: Boolean,
    final: (@Composable () -> Unit)? = null,
    seta: ImageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
    descricaoDaSeta: String? = null,
    aoTocar: () -> Unit,
) {
    val corSituacao = when (tom) {
        TomItem.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
        TomItem.ALERTA -> CoresAlerta.texto()
        TomItem.DESTAQUE -> corDestaque()
    }
    val corDoNome = if (vermelho) CoresAlerta.texto() else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(start = RECUO_DO_ICONE, end = 10.dp, top = 13.dp, bottom = 13.dp),
    ) {
        Icon(icone, contentDescription = null, tint = if (vermelho) corDoNome else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            titulo,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = corDoNome,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (situacao.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                situacao,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (tom == TomItem.NORMAL) FontWeight.Normal else FontWeight.SemiBold,
                color = corSituacao,
                maxLines = 1,
            )
        }
        if (final != null) {
            Spacer(Modifier.width(8.dp))
            final()
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.width(2.dp))
            Icon(
                seta,
                contentDescription = descricaoDaSeta,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** O quadro das telas dos Ajustes (sem título: o título fica na barra de cima). */
@Composable
internal fun QuadroAjuste(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), content = conteudo)
    }
}

/** Texto de apoio no alto das telas dos Ajustes. */
@Composable
internal fun ExplicacaoAjuste(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/** A tela de um item do menu dos Ajustes. */
@Composable
fun AjustesSecaoTela(estado: Estado, secao: SecaoAjuste, atualizacao: AtualizacaoViewModel, abrir: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = secao.id) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                when (secao) {
                    SecaoAjuste.MODO -> QuadroAjuste {
                        LinhaAjuste(
                            emblema = { EmblemaIcone(Icons.Outlined.Person, CoresEmblema.APORTE, TAMANHO_EMBLEMA) },
                            titulo = "Individual",
                            subtitulo = "Só as apostas e os prêmios, sem comprovantes. O saldo começa em zero e mostra quanto se gastou ou ganhou.",
                        )
                    }
                    SecaoAjuste.INTEGRANTES -> Integrantes(estado)
                    SecaoAjuste.PARAMETROS -> ParametrosDaSessao(estado)
                    SecaoAjuste.TERMO -> Termo(estado)
                    SecaoAjuste.INTEGRIDADE -> QuadroAjuste {
                        LinhaAjuste(
                            emblema = { EmblemaIcone(Icons.Outlined.VerifiedUser, CoresEmblema.APORTE, TAMANHO_EMBLEMA) },
                            titulo = "Verificar integridade",
                            subtitulo = "Confere a autenticação de todos os comprovantes, os anexos originais e um código de comprovante.",
                            aoTocar = { abrir("integridade") },
                            final = { Seta() },
                        )
                    }
                    SecaoAjuste.AUDITORIA -> QuadroAjuste { Auditoria(estado) }
                    SecaoAjuste.BACKUP -> Backup()
                    SecaoAjuste.NOTIFICACOES -> {
                        Secao("Apuração automática") { ApuracaoComOAppFechado() }
                        Secao("Toque do prêmio") { ToqueDoPremio() }
                    }
                    SecaoAjuste.BIOMETRIA -> BiometriaDoCelular()
                    // Ficam direto na lista (não têm tela).
                    SecaoAjuste.ATUALIZACAO, SecaoAjuste.TESTE -> Unit
                    SecaoAjuste.EXTINCAO -> Extincao(estado)
                }
            }
        }
    }
}

/** Os integrantes, com a chave Pix (o copiar no fim de cada um). */
@Composable
private fun Integrantes(estado: Estado) {
    val ctx = LocalContext.current
    QuadroAjuste {
        estado.pessoas.forEachIndexed { i, p ->
            // Sem as bolas das iniciais: a linha entre um e outro vai de ponta a ponta.
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ItemIntegrante(p) { copiar(ctx, "Chave Pix", p.chavePix) }
        }
    }
}

/**
 * O aporte mensal (só no bolão) e o preço da aposta simples de cada modalidade: desde a 0.23, no alto
 * dos Parâmetros (até a 0.22, a tela "Aporte e preços").
 */
@Composable
internal fun Valores(estado: Estado) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val escopo = rememberCoroutineScope()
    var editandoAporte by remember { mutableStateOf(false) }
    var editandoPreco by remember { mutableStateOf<Modalidade?>(null) }

    if (!estado.individual) {
        Secao("Aporte mensal") {
            LinhaAjuste(
                emblema = { EmblemaDesenho(Desenhos.Cofrinho, TAMANHO_EMBLEMA) },
                titulo = "Por integrante",
                subtitulo = "Total do mês: ${Dinheiro.rs(estado.config.aporteMensal * estado.pessoas.size.coerceAtLeast(1))}",
                aoTocar = { editandoAporte = true },
                final = {
                    Text(Dinheiro.rs(estado.config.aporteMensal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
            )
        }
    }
    Secao(
        "Preço da aposta simples",
        acao = { Text("Toque para mudar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
    ) {
        GradePrecos(estado) { editandoPreco = it }
    }

    if (editandoAporte) {
        DialogoDinheiro(
            titulo = "Aporte mensal",
            inicial = estado.config.aporteMensal,
            rotulo = "Valor por integrante",
            explicacao = "Mudanças no aporte precisam da concordância dos 3 integrantes, registrada no grupo.",
            aoConfirmar = { v ->
                editandoAporte = false
                if (v > 0) escopo.launch { repo.atualizar { it.copy(config = it.config.copy(aporteMensal = v)) } }
            },
            aoFechar = { editandoAporte = false },
        )
    }
    editandoPreco?.let { m ->
        DialogoDinheiro(
            titulo = m.nome,
            inicial = estado.config.preco(m),
            rotulo = "Preço da aposta simples",
            aoConfirmar = { v ->
                editandoPreco = null
                if (v > 0) escopo.launch {
                    repo.atualizar { it.copy(config = it.config.copy(precos = it.config.precos + (m.name to v))) }
                }
            },
            aoFechar = { editandoPreco = null },
        )
    }
}

/** Os dois anexos do termo: o sem assinaturas e o assinado pelos 3. */
@Composable
private fun Termo(estado: Estado) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val escolherTermoAssinado = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            try {
                val arq = repo.guardarArquivo(uri, "termo-assinado")
                repo.atualizar { it.copy(termo = arq) }
                repo.limparArquivosSoltos()
                avisos.showSnackbar("Termo assinado guardado.")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível guardar o arquivo: ${t.message}")
            }
        }
    }
    val escolherTermoSemAssinatura = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            try {
                val arq = repo.guardarArquivo(uri, "termo-sem-assinatura")
                repo.atualizar { it.copy(termoSemAssinatura = arq) }
                repo.limparArquivosSoltos()
                avisos.showSnackbar("Termo sem assinaturas guardado.")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível guardar o arquivo: ${t.message}")
            }
        }
    }
    QuadroAjuste {
        AnexoDoTermo(
            titulo = "Termo sem assinaturas",
            icone = Icons.Outlined.Description,
            cor = CoresEmblema.RATEIO,
            arquivo = estado.termoSemAssinatura,
            aoAnexar = { escolherTermoSemAssinatura.launch(arrayOf("application/pdf", "image/*")) },
        )
        Divisoria()
        AnexoDoTermo(
            titulo = "Termo assinado",
            icone = Icons.Outlined.HistoryEdu,
            cor = CoresEmblema.EXTRATO,
            arquivo = estado.termo,
            aoAnexar = { escolherTermoAssinado.launch(arrayOf("application/pdf", "image/*")) },
        )
    }
}

/** A cópia de tudo (fazer e restaurar). */
@Composable
private fun Backup() {
    val ctx = LocalContext.current
    val backup = remember { ctx.bolao.backup }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var trabalhando by remember { mutableStateOf(false) }
    var confirmandoRestauracao by remember { mutableStateOf(false) }
    val escolherBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) escopo.launch {
            trabalhando = true
            try {
                val entrada = ctx.contentResolver.openInputStream(uri) ?: error("Não foi possível abrir o arquivo.")
                val r = entrada.use { backup.restaurar(it) }
                avisos.showSnackbar("Backup restaurado: ${r.estado.apostas.size} jogos e ${r.anexos} anexos.")
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível restaurar: ${t.message}")
            } finally {
                trabalhando = false
            }
        }
    }
    QuadroAjuste {
        LinhaAjuste(
            emblema = { EmblemaIcone(Icons.Outlined.Backup, CoresEmblema.RATEIO, TAMANHO_EMBLEMA) },
            titulo = "Cópia de tudo",
            subtitulo = "Jogos, lançamentos, comprovantes e o termo. Fica em Downloads/Bolao e dá para enviar ao Drive ou por e-mail.",
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            OutlinedButton(
                enabled = !trabalhando,
                onClick = {
                    escopo.launch {
                        trabalhando = true
                        try {
                            val uri = backup.salvarEmDownloads()
                            val r = avisos.showSnackbar("Backup salvo em Downloads/Bolao.", actionLabel = "Enviar")
                            if (r == SnackbarResult.ActionPerformed) {
                                val enviar = Intent(Intent.ACTION_SEND)
                                    .setType("application/zip")
                                    .putExtra(Intent.EXTRA_STREAM, uri)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                ctx.startActivity(Intent.createChooser(enviar, "Enviar backup"))
                            }
                        } catch (t: Throwable) {
                            avisos.showSnackbar("Não foi possível fazer o backup: ${t.message}")
                        } finally {
                            trabalhando = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Fazer backup", maxLines = 1)
            }
            OutlinedButton(
                enabled = !trabalhando,
                onClick = { confirmandoRestauracao = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restaurar", maxLines = 1)
            }
        }
    }
    if (confirmandoRestauracao) {
        DialogoConfirmar(
            titulo = "Restaurar backup?",
            texto = "Todos os dados atuais serão trocados pelos do backup escolhido. Se quiser guardar os atuais, faça um backup antes.",
            confirmar = "Escolher arquivo",
            aoConfirmar = {
                confirmandoRestauracao = false
                escolherBackup.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            },
            aoFechar = { confirmandoRestauracao = false },
        )
    }
}

/**
 * A apuração automática e o aviso do prêmio com o app fechado: o que o celular precisa liberar.
 * A situação é lida de novo ao voltar dos ajustes do Android.
 */
@Composable
private fun ApuracaoComOAppFechado() {
    val ctx = LocalContext.current
    var conferido by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        conferido++
        onPauseOrDispose {}
    }
    val avisosPermitidos = remember(conferido) { Notificador.permitido(ctx) }
    val bateriaLivre = remember(conferido) { SegundoPlano.bateriaLivre(ctx) }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) Notificador.teste(ctx)
        conferido++
    }
    ExplicacaoAjuste(
        "O app confere o resultado, apura e avisa do prêmio sozinho, na hora do sorteio, mesmo com o app fechado. " +
            "Para isso o celular não pode pausar o app:",
    )
    QuadroAjuste {
        LinhaAjuste(
            emblema = { EmblemaIcone(Icons.Outlined.NotificationsActive, CoresEmblema.PREMIO, TAMANHO_EMBLEMA) },
            titulo = "Avisos",
            subtitulo = if (avisosPermitidos) "Permitidos" else "Desligados: o aviso do prêmio não chega",
            final = {
                if (avisosPermitidos) {
                    OutlinedButton(onClick = { Notificador.teste(ctx) }) { Text("Testar") }
                } else {
                    Button(onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            pedirPermissao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            SegundoPlano.abrirAjustesDoApp(ctx)
                        }
                    }) { Text("Permitir") }
                }
            },
        )
        Divisoria()
        LinhaAjuste(
            emblema = { EmblemaIcone(Icons.Outlined.BatteryFull, CoresEmblema.APORTE, TAMANHO_EMBLEMA) },
            titulo = "Bateria",
            subtitulo = if (bateriaLivre) "Sem restrições" else "Com restrição: a apuração pode atrasar",
            final = if (bateriaLivre) null else ({ Button(onClick = { SegundoPlano.pedirBateriaLivre(ctx) }) { Text("Liberar") } }),
        )
        Divisoria()
        LinhaAjuste(
            emblema = { EmblemaIcone(Icons.Outlined.PowerSettingsNew, AzulMarinhoEmblema, TAMANHO_EMBLEMA) },
            titulo = "Inicialização automática",
            subtitulo = "No HyperOS: ative para o app acordar sozinho na hora do resultado",
            final = { OutlinedButton(onClick = { SegundoPlano.abrirInicioAutomatico(ctx) }) { Text("Abrir") } },
        )
    }
}

/** A extinção do bolão (no individual, "Apagar tudo"), com o quadro vermelho de sempre. */
@Composable
private fun Extincao(estado: Estado) {
    var pedindoExtincao by remember { mutableStateOf(false) }
    // Extinção forçada: destravada pelos toques no quadro da extinção (vale enquanto a tela está aberta).
    var forcadaLiberada by remember { mutableStateOf(false) }
    CartaoExtincao(
        estado,
        forcadaLiberada = forcadaLiberada || estado.individual,
        aoLiberarForcada = { forcadaLiberada = true },
        aoPedir = { pedindoExtincao = true },
    )
    if (pedindoExtincao) {
        DialogoExtincao(
            estado,
            forcada = forcadaLiberada || estado.individual,
            aoFechar = { pedindoExtincao = false },
        )
    }
}

/** Tamanho dos emblemas das linhas dos Ajustes. */
internal val TAMANHO_EMBLEMA = 44.dp

/** Onde começa o texto das linhas (depois do emblema): botões e dicas ficam alinhados com ele. */
internal val RECUO_TEXTO = 58.dp

/** Azul do emblema da atualização (o mesmo nos dois temas, legível no escuro). */
internal val AzulMarinhoEmblema = Color(0xFF2F4A7A)

@Composable
internal fun Divisoria() {
    HorizontalDivider(Modifier.padding(start = RECUO_TEXTO), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun Seta() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Uma linha dos Ajustes: o emblema colorido, o título e o texto de apoio e, no fim, o valor ou o
 * botão ([final]). Com [aoTocar], a linha inteira é um botão.
 */
@Composable
internal fun LinhaAjuste(
    emblema: @Composable () -> Unit,
    titulo: String,
    subtitulo: String? = null,
    aoTocar: (() -> Unit)? = null,
    final: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (aoTocar != null) Modifier.clickable(onClick = aoTocar) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        emblema()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (final != null) {
            Spacer(Modifier.width(8.dp))
            final()
        }
    }
}

/** Um integrante: o apelido (com a marca do Administrador), o nome e a chave Pix, com o copiar no fim. */
@Composable
private fun ItemIntegrante(p: Pessoa, aoCopiar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.apelido,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (p.administrador) {
                    Spacer(Modifier.width(8.dp))
                    TarjaTitulo("Administrador", fundo = corDestaque().copy(alpha = 0.16f), cor = corDestaque())
                }
            }
            Text(
                p.nome,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Pix: ${p.chavePix}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = aoCopiar) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar chave Pix", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Auditoria (antes ficava no Extrato): o PDF com todos os comprovantes do período e os anexos
 * originais, em ordem cronológica, salvo em Downloads/Bolao.
 */
@Composable
private fun Auditoria(estado: Estado) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    var de by remember { mutableStateOf(PdfsDoApp.inicio(estado)) }
    var ate by remember { mutableStateOf(LocalDate.now()) }
    var gerando by remember { mutableStateOf(false) }
    var progresso by remember { mutableFloatStateOf(0f) }
    var pronto by remember { mutableStateOf<Pair<Uri, AuditoriaPdf.Resultado>?>(null) }

    LinhaAjuste(
        emblema = { EmblemaIcone(Icons.Outlined.PictureAsPdf, CoresEmblema.EXTRATO, TAMANHO_EMBLEMA) },
        titulo = "PDF da auditoria",
        subtitulo = "Todos os comprovantes (aporte, aposta, apuração, rateio e extrato) e os anexos originais, em ordem cronológica.",
    )
    Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Um embaixo do outro: lado a lado, a data inteira não cabe no cartão.
        CampoData(data = de, aoMudar = { de = it }, rotulo = "De", modifier = Modifier.fillMaxWidth())
        CampoData(data = ate, aoMudar = { ate = it }, rotulo = "Até", modifier = Modifier.fillMaxWidth())
        if (gerando) LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth())
        Button(
            enabled = !gerando && !de.isAfter(ate),
            onClick = {
                gerando = true
                progresso = 0f
                escopo.launch {
                    try {
                        pronto = PdfsDoApp.auditoriaEmDownloads(ctx, estado, de, ate) { feitos, total ->
                            progresso = if (total == 0) 1f else feitos / total.toFloat()
                        }
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível gerar o PDF: ${t.message}")
                    } finally {
                        gerando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (gerando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Gerar PDF")
        }
    }

    pronto?.let { (uri, r) ->
        AlertDialog(
            onDismissRequest = { pronto = null },
            title = { Text("Auditoria pronta") },
            text = {
                Text(
                    "Salva em Downloads/Bolao: ${r.comprovantes} comprovante(s), ${r.anexos} anexo(s), ${r.paginas} página(s)." +
                        if (r.problemas.isEmpty()) "\n\nTudo confere." else "\n\nAtenção:\n" + r.problemas.joinToString("\n") { "• $it" },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pronto = null
                    Compartilhar.enviarArquivo(ctx, uri, "application/pdf", "Enviar a auditoria")
                }) { Text("Enviar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pronto = null }) { Text("Fechar") }
                    TextButton(onClick = {
                        pronto = null
                        Compartilhar.abrir(ctx, uri, "application/pdf")
                    }) { Text("Abrir") }
                }
            },
        )
    }
}

/** Colunas da tabela de preços (10 modalidades = 4 + 4 + 2, a última linha no meio). */
private const val COLUNAS_PRECOS = 4

/** Tabela de preços: o selo de cada modalidade (desde a 0.27, no lugar da bola), o nome e o preço embaixo; tocar muda o preço. */
@Composable
private fun GradePrecos(estado: Estado, aoEscolher: (Modalidade) -> Unit) {
    val lista = Modalidade.ATIVAS.filter { !it.precoVariavel }
    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lista.chunked(COLUNAS_PRECOS).forEach { linha ->
            Row(Modifier.fillMaxWidth()) {
                // Linha incompleta: as bolas ficam no meio, alinhadas com as colunas de cima.
                val sobra = (COLUNAS_PRECOS - linha.size) / 2f
                if (sobra > 0f) Spacer(Modifier.weight(sobra))
                linha.forEach { m ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { aoEscolher(m) }
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                    ) {
                        SeloModalidade(m, 48.dp)
                        Spacer(Modifier.height(6.dp))
                        TextoQueCabe(
                            m.nome,
                            MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                            cor = MaterialTheme.colorScheme.onSurfaceVariant,
                            menor = 9.sp,
                        )
                        TextoQueCabe(
                            Dinheiro.rs(estado.config.preco(m)),
                            MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            peso = FontWeight.Bold,
                            menor = 11.sp,
                        )
                    }
                }
                if (sobra > 0f) Spacer(Modifier.weight(sobra))
            }
        }
    }
}

/** Um dos dois anexos do termo: o arquivo anexado (ou a falta dele) e, embaixo, ver e anexar (ou trocar). */
@Composable
private fun AnexoDoTermo(titulo: String, icone: ImageVector, cor: Color, arquivo: Arquivo?, aoAnexar: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemaIcone(icone, cor, TAMANHO_EMBLEMA)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                if (arquivo != null) {
                    Text(
                        "${arquivo.nomeOriginal} · anexado " +
                            Datas.quando(Instant.ofEpochMilli(arquivo.guardadoEm).atZone(ZoneId.systemDefault()).toLocalDate(), java.time.LocalDate.now()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        "Nenhum arquivo anexado.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CoresAlerta.texto(),
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = RECUO_TEXTO, top = 8.dp),
        ) {
            if (arquivo != null) {
                OutlinedButton(
                    onClick = { Compartilhar.abrir(ctx, repo.arquivo(arquivo), arquivo.mime) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver")
                }
            }
            OutlinedButton(onClick = aoAnexar, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (arquivo == null) "Anexar" else "Trocar")
            }
        }
    }
}

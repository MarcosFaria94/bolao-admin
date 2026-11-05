package io.github.marcosfaria94.bolao.ui.notificacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Pendencia
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.ui.aposta.tituloDaAposta
import io.github.marcosfaria94.bolao.ui.comum.CoresEmblema
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.EtiquetaSituacao
import io.github.marcosfaria94.bolao.ui.comum.Icones
import io.github.marcosfaria94.bolao.ui.comum.IndicadorDoPuxao
import io.github.marcosfaria94.bolao.ui.comum.acompanharPuxao
import io.github.marcosfaria94.bolao.ui.comum.corCirculo
import io.github.marcosfaria94.bolao.ui.comum.rememberPuxaoAlemDaBorda
import io.github.marcosfaria94.bolao.ui.home.etiquetaDoJogo
import io.github.marcosfaria94.bolao.ui.home.jogosEmAndamento
import io.github.marcosfaria94.bolao.ui.jogos.NAO_DIVULGADO
import io.github.marcosfaria94.bolao.ui.tema.Barra
import io.github.marcosfaria94.bolao.ui.tema.Dourado
import java.time.LocalDate

/**
 * Notificações (o painel à esquerda das abas: o sino da barra de cima ou puxar qualquer aba para
 * baixo), em dois grupos, como os Ajustes:
 * 1. PENDÊNCIAS: primeiro, se houver, os comprovantes que faltam ([semComprovante]) e os que
 *    esperam a conferência ([aConferir]), só com o número (o atalho abre os Comprovantes); depois,
 *    o que falta fazer, o mais recente primeiro: o ícone, o título e uma linha de texto.
 * 2. MEUS JOGOS: cada jogo em andamento (registrado esperando o sorteio, ou que falta registrar)
 *    numa linha: a bola da modalidade com o concurso, o nome, a aposta e o sorteio, e a etiqueta
 *    ("Sem comprovante", "Falta registrar"...). As dezenas ficam na página da aposta.
 * Tocar numa linha abre a tela que resolve. A raiz cuida de fechar deslizando o dedo para os lados
 * ([modifier]). Puxar para baixo de novo (do alto da lista) abre os Comprovantes ([abrirComprovantes];
 * null no modo individual). Empurrar para cima não faz nada. [ativa]: a lista está na tela.
 */
@Composable
fun NotificacoesTela(
    pendencias: List<Pendencia>,
    estado: Estado,
    abrir: (String) -> Unit,
    abrirComprovantes: (() -> Unit)?,
    modifier: Modifier = Modifier,
    semComprovante: Int = 0,
    aConferir: Int = 0,
    ativa: Boolean = true,
) {
    val ctx = LocalContext.current
    // Com a lista na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(ativa) { if (ativa) Notificador.limparAcoes(ctx) }
    val hoje = LocalDate.now()
    val jogos = jogosEmAndamento(estado, hoje).flatMap { it.apostas }
    val puxao = rememberPuxaoAlemDaBorda(vertical = true, noInicio = abrirComprovantes)
    val faltam = if (abrirComprovantes != null) semComprovante else 0
    val conferir = if (abrirComprovantes != null) aConferir else 0
    val total = pendencias.size + listOf(faltam, conferir).count { it > 0 }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize().nestedScroll(puxao).acompanharPuxao(puxao),
        ) {
            item(key = "titulo-pendencias") { TituloDoGrupo("PENDÊNCIAS", total, primeiro = true) }
            item(key = "pendencias") {
                Grupo {
                    // Quantas linhas já foram (a linha fina vai entre uma e outra).
                    var linha = 0
                    if (faltam > 0) {
                        LinhaDaLista(
                            Icons.AutoMirrored.Outlined.ReceiptLong, Barra.contador, "Esperando comprovante",
                            contador = faltam, aoTocar = abrirComprovantes!!,
                        )
                        linha++
                    }
                    if (conferir > 0) {
                        if (linha > 0) Divisoria()
                        LinhaDaLista(
                            Icons.Outlined.TaskAlt, CoresEmblema.EXTRATO, "Comprovantes a conferir",
                            contador = conferir, aoTocar = abrirComprovantes!!,
                        )
                        linha++
                    }
                    pendencias.forEach { p ->
                        if (linha > 0) Divisoria()
                        val (icone, cor) = iconeDa(p.tipo)
                        LinhaDaLista(icone, cor, p.titulo, p.texto) { abrir(p.destino) }
                        linha++
                    }
                    if (linha == 0) Vazio("Tudo em dia. Quando houver algo a fazer, aparece aqui.")
                }
            }
            item(key = "titulo-jogos") { TituloDoGrupo("MEUS JOGOS", jogos.size) }
            item(key = "jogos") {
                Grupo {
                    jogos.forEachIndexed { i, a ->
                        if (i > 0) Divisoria(recuo = 72.dp)
                        LinhaDoJogo(a, hoje) { abrir("aposta/${a.id}") }
                    }
                    if (jogos.isEmpty()) Vazio("Nenhum jogo em andamento.")
                }
            }
        }
        IndicadorDoPuxao(
            puxao, puxando = "Comprovantes", pronto = "Comprovantes", icone = Icons.AutoMirrored.Outlined.ReceiptLong,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
        )
    }
}

/** O ícone e a cor de cada tipo de pendência. */
private fun iconeDa(tipo: Pendencia.Tipo): Pair<ImageVector, Color> = when (tipo) {
    Pendencia.Tipo.APORTE -> Icons.Outlined.Payments to CoresEmblema.APORTE
    Pendencia.Tipo.APOSTA_DO_DIA -> Icones.Trevo to Dourado
    Pendencia.Tipo.RASCUNHO -> Icons.Outlined.EditNote to Color(0xFFE07B1F)
    Pendencia.Tipo.ANEXO -> Icons.AutoMirrored.Outlined.ReceiptLong to Barra.contador
    Pendencia.Tipo.CONFERENCIA -> Icons.Outlined.TaskAlt to CoresEmblema.EXTRATO
    Pendencia.Tipo.ENVIO -> Icons.AutoMirrored.Outlined.Send to CoresEmblema.RATEIO
    Pendencia.Tipo.APURACAO -> Icons.Outlined.CheckCircle to CoresEmblema.PREMIO
    Pendencia.Tipo.EXTRATO -> Icons.Outlined.Description to CoresEmblema.EXTRATO
    Pendencia.Tipo.INDICES -> Icons.Outlined.WarningAmber to Color(0xFFD97706)
}

/** "PENDÊNCIAS 2", como os títulos dos grupos dos Ajustes. */
@Composable
private fun TituloDoGrupo(nome: String, quantos: Int, primeiro: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = if (primeiro) 8.dp else 22.dp, bottom = 6.dp),
    ) {
        Text(
            nome,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 1.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (quantos > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                quantos.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Um grupo: as linhas num cartão, com a linha fina entre elas. */
@Composable
private fun Grupo(conteudo: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = conteudo)
    }
}

@Composable
private fun Divisoria(recuo: Dp = 64.dp) {
    HorizontalDivider(Modifier.padding(start = recuo), color = MaterialTheme.colorScheme.outlineVariant)
}

/** Uma pendência: o ícone num círculo, o título e uma linha de texto; à direita, o número (dos comprovantes). */
@Composable
private fun LinhaDaLista(icone: ImageVector, cor: Color, titulo: String, texto: String? = null, contador: Int? = null, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Box(Modifier.size(36.dp).background(corCirculo(), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!texto.isNullOrBlank()) {
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (contador != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.sizeIn(minWidth = 26.dp, minHeight = 26.dp).background(Barra.contador, CircleShape).padding(horizontal = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (contador > 99) "99+" else contador.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

/** Um jogo em andamento: a bola (sigla e concurso), o nome, a aposta e o sorteio, e a etiqueta à direita. */
@Composable
private fun LinhaDoJogo(a: Aposta, hoje: LocalDate, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
    ) {
        EmblemaModalidade(a.modalidade, a.concurso, tamanho = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                a.modalidade.nome,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val aposta = if (a.registrada) tituloDaAposta(a) else "Nova aposta"
            val sorteio = a.dataSorteio?.let { Datas.relativa(it, hoje) } ?: NAO_DIVULGADO
            Text(
                "$aposta · Sorteio: $sorteio",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        etiquetaDoJogo(a)?.let {
            Spacer(Modifier.width(8.dp))
            EtiquetaSituacao(it)
        }
    }
}

/** O grupo sem nada: o texto com o "certo" na frente. */
@Composable
private fun Vazio(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp)) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = CoresEmblema.APORTE, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

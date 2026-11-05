package io.github.marcosfaria94.bolao.ui.notificacoes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Pendencia
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.ui.comum.IndicadorDoPuxao
import io.github.marcosfaria94.bolao.ui.comum.acompanharPuxao
import io.github.marcosfaria94.bolao.ui.comum.deslizarParaFechar
import io.github.marcosfaria94.bolao.ui.comum.rememberPuxaoAlemDaBorda
import io.github.marcosfaria94.bolao.ui.home.CartaoDoConcurso
import io.github.marcosfaria94.bolao.ui.home.jogosEmAndamento
import java.time.LocalDate

/**
 * Notificações (puxar qualquer aba para baixo, o sino da barra de cima ou deslizar o dedo para a
 * direita a partir da Home): uma tela, duas seções.
 * 1. Pendências: o que falta fazer (menos os comprovantes, que ficam no botão Comprovantes), em
 *    texto corrido, o mais recente primeiro. Tocar num item abre a tela que resolve.
 * 2. Meus jogos: os jogos em andamento (registrados esperando o sorteio e os que falta registrar),
 *    cada concurso num cartão.
 * Deslizar o dedo para qualquer lado fecha (volta para a tela de antes). Puxar para baixo de novo
 * (do alto da lista) abre os Comprovantes ([abrirComprovantes]; null no modo individual). Empurrar
 * para cima não faz nada. [ativa]: a lista está na tela.
 */
@Composable
fun NotificacoesTela(
    pendencias: List<Pendencia>,
    estado: Estado,
    abrir: (String) -> Unit,
    aoFechar: () -> Unit,
    abrirComprovantes: (() -> Unit)?,
    ativa: Boolean = true,
) {
    val ctx = LocalContext.current
    // Com a lista na tela, o aviso "Ação necessária" do celular já cumpriu o papel.
    LaunchedEffect(ativa) { if (ativa) Notificador.limparAcoes(ctx) }
    val hoje = LocalDate.now()
    val grupos = jogosEmAndamento(estado, hoje)
    val jogos = grupos.sumOf { it.apostas.size }
    val puxao = rememberPuxaoAlemDaBorda(vertical = true, noInicio = abrirComprovantes)

    Box(Modifier.fillMaxSize().deslizarParaFechar(aoFechar)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize().nestedScroll(puxao).acompanharPuxao(puxao),
        ) {
            item(key = "titulo-pendencias") { TituloDaSecao("Pendências", pendencias.size, Modifier.padding(top = 4.dp)) }
            if (pendencias.isEmpty()) {
                item(key = "sem-pendencias") { TextoDaSecao("Tudo em dia. Quando houver algo a fazer no bolão, aparece aqui.") }
            }
            items(pendencias, key = { "p-" + it.chave }) { p ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { abrir(p.destino) }
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                ) {
                    Text(
                        p.titulo,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        p.texto,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "titulo-jogos") { TituloDaSecao("Meus jogos", jogos, Modifier.padding(top = 20.dp, bottom = 8.dp)) }
            if (grupos.isEmpty()) {
                item(key = "sem-jogos") { TextoDaSecao("Nenhum jogo em andamento.") }
            }
            items(grupos, key = { "j-${it.modalidade}-${it.concurso}-${it.sorteio}" }) { g ->
                Box(Modifier.padding(bottom = 12.dp)) { CartaoDoConcurso(estado, g, hoje, abrir) }
            }
        }
        IndicadorDoPuxao(
            puxao, puxando = "Comprovantes", pronto = "Solte para abrir", icone = Icons.AutoMirrored.Outlined.ReceiptLong,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
        )
    }
}

/** "Pendências" ou "Meus jogos", com quantos são ao lado. */
@Composable
private fun TituloDaSecao(titulo: String, quantos: Int, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp), fontWeight = FontWeight.Bold)
        if (quantos > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                quantos.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TextoDaSecao(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
    )
}

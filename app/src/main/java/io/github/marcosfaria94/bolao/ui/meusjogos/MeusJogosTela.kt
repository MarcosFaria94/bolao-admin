package io.github.marcosfaria94.bolao.ui.meusjogos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.ui.aposta.tituloDaAposta
import io.github.marcosfaria94.bolao.ui.comum.EmblemaModalidade
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.TextoQueCabe
import io.github.marcosfaria94.bolao.ui.home.GrupoEmAndamento
import io.github.marcosfaria94.bolao.ui.home.jogosEmAndamento
import io.github.marcosfaria94.bolao.ui.home.pendenciaDoJogo
import io.github.marcosfaria94.bolao.ui.home.resumoDoJogo
import io.github.marcosfaria94.bolao.ui.jogos.NAO_DIVULGADO
import io.github.marcosfaria94.bolao.ui.tema.corApuracao
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import java.time.LocalDate

/** Endereço da tela Meus jogos (puxar as Notificações para baixo, ou o bilhete ao lado do sino). */
const val ROTA_MEUS_JOGOS = "meus-jogos"

/**
 * Meus jogos: os jogos em andamento (registrados esperando o sorteio e os que falta registrar),
 * um cartão por modalidade e concurso, o sorteio mais próximo primeiro. No alto do cartão, a bola,
 * o nome e o concurso; embaixo, o sorteio e o prêmio estimado; depois, cada aposta (o número, as
 * dezenas e, à direita, OK ou Pendente, com o que falta). No alto da tela, quantos são, quantos
 * estão OK e quantos pendentes. Tocar numa aposta abre a página dela; sem jogos, o "Apostar" vai
 * para os Jogos ([apostar]). A raiz cuida de voltar (puxando para baixo ou deslizando para o lado).
 */
@Composable
fun MeusJogosTela(estado: Estado, abrir: (String) -> Unit, apostar: () -> Unit) {
    val ctx = LocalContext.current
    val itens by ctx.bolao.indices.itens.collectAsStateWithLifecycle()
    val hoje = LocalDate.now()
    val grupos = jogosEmAndamento(estado, hoje)
    val todas = grupos.flatMap { it.apostas }
    val pendentes = todas.count { pendenciaDoJogo(estado, it) != null }

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (grupos.isEmpty()) {
            item(key = "vazio") { Vazio(apostar) }
        } else {
            item(key = "resumo") { Resumo(todas.size, todas.size - pendentes, pendentes) }
            items(grupos, key = { "${it.modalidade.name}-${it.concurso}-${it.sorteio}" }) { g ->
                CartaoDoConcurso(estado, g, itens[g.modalidade], hoje, abrir)
            }
        }
    }
}

/** Verde do OK e âmbar do Pendente. */
@Composable
private fun corOk(): Color = corEntrada()

@Composable
private fun corPendente(): Color = corApuracao()

/** Quantos jogos, quantos OK e quantos pendentes, lado a lado. */
@Composable
private fun Resumo(jogos: Int, ok: Int, pendentes: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Numero(jogos, if (jogos == 1) "Jogo" else "Jogos", MaterialTheme.colorScheme.onSurface)
            Numero(ok, "OK", if (ok > 0) corOk() else MaterialTheme.colorScheme.onSurfaceVariant)
            Numero(pendentes, if (pendentes == 1) "Pendente" else "Pendentes", if (pendentes > 0) corPendente() else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowScope.Numero(valor: Int, rotulo: String, cor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(valor.toString(), fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = cor)
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * O prêmio estimado do concurso: o da Caixa agora (se a Caixa está recebendo apostas para ele) ou o
 * que estava valendo quando a aposta foi feita; sem nenhum, "Não divulgado".
 */
private fun premioDoConcurso(g: GrupoEmAndamento, item: Indices.Item?): String {
    val daCaixa = item?.ultimo?.takeIf { it.proximoNumero == g.concurso }?.premioEstimadoProximo
    val valor = daCaixa?.takeIf { it > 0 } ?: g.apostas.mapNotNull { it.premioEstimado }.filter { it > 0 }.maxOrNull()
    return valor?.let { Dinheiro.porExtenso(it) } ?: NAO_DIVULGADO
}

/** Um concurso: a bola, o nome e o número; o sorteio e o prêmio; e as apostas, cada uma com OK ou Pendente. */
@Composable
private fun CartaoDoConcurso(estado: Estado, g: GrupoEmAndamento, item: Indices.Item?, hoje: LocalDate, abrir: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) {
            EmblemaModalidade(g.modalidade, concurso = null, tamanho = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    g.modalidade.nome,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 23.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (g.concurso > 0) "Concurso ${g.concurso}" else "Concurso a definir",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Informacao(Icons.Outlined.Event, "Sorteio", g.sorteio?.let { Datas.relativa(it, hoje) } ?: NAO_DIVULGADO)
            Informacao(Icons.Outlined.EmojiEvents, "Prêmio estimado", premioDoConcurso(g, item))
        }
        g.apostas.forEach { a ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LinhaDaAposta(a, pendenciaDoJogo(estado, a)) { abrir("aposta/${a.id}") }
        }
    }
}

/** O sorteio ou o prêmio: o ícone, o rótulo pequeno e o valor, num quadrinho. */
@Composable
private fun RowScope.Informacao(icone: ImageVector, rotulo: String, valor: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(rotulo, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            // O valor diminui um pouco para caber inteiro ("R$ 120 Milhões", "07/11/2026").
            TextoQueCabe(
                valor,
                MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 19.sp),
                peso = FontWeight.SemiBold,
                menor = 11.sp,
            )
        }
    }
}

/** Uma aposta do concurso: o número, as dezenas e o que falta; à direita, OK ou Pendente. */
@Composable
private fun LinhaDaAposta(a: Aposta, pendencia: String?, aoTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (a.registrada) tituloDaAposta(a) else "Nova aposta",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                resumoDoJogo(a),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (pendencia != null) {
                Text(
                    pendencia,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = corPendente(),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        IndicadorOk(pendencia == null)
    }
}

/** "OK" (verde, com o certo) ou "Pendente" (âmbar, com o relógio). */
@Composable
private fun IndicadorOk(ok: Boolean) {
    val cor = if (ok) corOk() else corPendente()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(cor.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(start = 8.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
    ) {
        Icon(if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule, contentDescription = null, tint = cor, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            if (ok) "OK" else "Pendente",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
            fontWeight = FontWeight.Bold,
            color = cor,
            maxLines = 1,
        )
    }
}

/** Sem jogos em andamento: o bilhete, o texto e o "Apostar". */
@Composable
private fun Vazio(apostar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Nenhum jogo em andamento.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Os jogos registrados aparecem aqui até o sorteio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = apostar) { Text("Apostar") }
        }
    }
}

package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.domain.Dinheiro

/**
 * Quadro no estilo do saldo da Home: fundo um pouco mais escuro que o dos cartões e um
 * contorno fino. Com [aoTocar], o quadro inteiro é um botão.
 */
@Composable
fun Quadro(
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    val cores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val borda = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (aoTocar != null) {
        Card(onClick = aoTocar, colors = cores, border = borda, modifier = modifier.fillMaxWidth(), content = conteudo)
    } else {
        Card(colors = cores, border = borda, modifier = modifier.fillMaxWidth(), content = conteudo)
    }
}

/**
 * Saldo no estilo dos apps de banco: "Saldo líquido" e o valor grande à esquerda e, à direita
 * dele, o provisionado e o extra, um embaixo do outro. Com [aoTocar], o quadro inteiro é um botão.
 */
@Composable
fun QuadroSaldo(
    saldo: Long,
    provisionado: Long,
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
) {
    val extra = saldo - provisionado
    Quadro(modifier, aoTocar) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Saldo líquido",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                TextoQueCabe(
                    Dinheiro.rs(saldo),
                    MaterialTheme.typography.displayMedium,
                    peso = FontWeight.Bold,
                    menor = 22.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Indicador("Provisionado", Dinheiro.rs(provisionado))
                Indicador("Extra", (if (extra < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(extra)))
            }
        }
    }
}

/** Um número do lado direito do quadro do saldo, com o nome em cima: "Provisionado / R$ 30,00". */
@Composable
fun Indicador(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            rotulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            valor,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 24.sp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Seção das listas: o título (com uma [acao] opcional ao lado, como o seletor do mês) e, embaixo,
 * um cartão com o conteúdo (Ajustes).
 */
@Composable
fun Secao(
    titulo: String,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            acao?.invoke()
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), content = conteudo)
        }
    }
}

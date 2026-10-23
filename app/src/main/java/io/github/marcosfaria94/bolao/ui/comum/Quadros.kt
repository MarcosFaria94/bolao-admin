package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * Quadro no estilo do saldo do Financeiro: fundo um pouco mais escuro que o dos cartões e um
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
 * Saldo no estilo dos apps de banco: "Saldo líquido", o valor grande e, embaixo, o provisionado
 * e o extra (sem fundo próprio). [compacto]: a versão menor, no alto da Nova aposta.
 */
@Composable
fun QuadroSaldo(
    saldo: Long,
    provisionado: Long,
    modifier: Modifier = Modifier,
    compacto: Boolean = false,
    aoTocar: (() -> Unit)? = null,
) {
    val extra = saldo - provisionado
    val margens = if (compacto) PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
    else PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)
    Quadro(modifier, aoTocar) {
        Column(Modifier.padding(margens)) {
            Text(
                "Saldo líquido",
                style = if (compacto) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextoQueCabe(
                Dinheiro.rs(saldo),
                if (compacto) MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 38.sp)
                else MaterialTheme.typography.displayMedium,
                peso = FontWeight.Bold,
                menor = if (compacto) 20.sp else 28.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(if (compacto) 8.dp else 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Indicador("Provisionado", Dinheiro.rs(provisionado), Modifier.weight(1f), compacto)
                Indicador("Extra", (if (extra < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(extra)), Modifier.weight(1f), compacto)
            }
        }
    }
}

/** Um número do quadro, com o nome em cima: "Provisionado / R$ 30,00". */
@Composable
fun Indicador(rotulo: String, valor: String, modifier: Modifier = Modifier, compacto: Boolean = false) {
    Column(modifier) {
        Text(
            rotulo,
            style = if (compacto) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        TextoQueCabe(
            valor,
            MaterialTheme.typography.titleLarge.copy(fontSize = if (compacto) 17.sp else 20.sp),
            menor = 13.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Seção das listas: o título (com uma [acao] opcional ao lado, como o seletor do mês) e, embaixo,
 * um cartão com o conteúdo, como o Histórico do Financeiro.
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

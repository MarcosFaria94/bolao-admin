package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.ui.tema.corModalidade

/** Quantas colunas o volante tem na tela (parecido com o volante da Caixa). */
private fun colunasDoVolante(m: Modalidade): Int = when (m) {
    Modalidade.LOTOFACIL -> 5
    Modalidade.DIA_DE_SORTE -> 7
    else -> 10
}

/**
 * Situação de cada número na grade.
 * Na aposta só existe MARCADO/LIVRE; na apuração aparecem acerto, erro e sorteado.
 */
enum class SituacaoNumero { LIVRE, MARCADO, ACERTO, ERRO, SORTEADO }

@Composable
private fun Celula(
    texto: String,
    situacao: SituacaoNumero,
    cor: Color,
    forma: Shape,
    modifier: Modifier = Modifier,
    aoTocar: (() -> Unit)? = null,
) {
    val esquema = MaterialTheme.colorScheme
    val (fundo, textoCor, borda) = when (situacao) {
        SituacaoNumero.LIVRE -> Triple(esquema.surfaceContainerHigh, esquema.onSurface, null)
        SituacaoNumero.MARCADO, SituacaoNumero.ACERTO -> Triple(cor, Color.White, null)
        SituacaoNumero.ERRO -> Triple(esquema.surfaceContainerHigh, esquema.onSurfaceVariant, BorderStroke(2.dp, cor.copy(alpha = 0.6f)))
        SituacaoNumero.SORTEADO -> Triple(esquema.surfaceContainerHigh, cor, BorderStroke(2.dp, cor))
    }
    var mod = modifier
        .aspectRatio(1f)
        .clip(forma)
        .background(fundo, forma)
    if (borda != null) mod = mod.border(borda, forma)
    if (aoTocar != null) mod = mod.clickable(onClick = aoTocar)
    Box(mod, contentAlignment = Alignment.Center) {
        Text(
            texto,
            color = textoCor,
            fontSize = 14.sp,
            fontWeight = if (situacao == SituacaoNumero.LIVRE || situacao == SituacaoNumero.ERRO) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** Grade de números no estilo do volante (tocar marca e desmarca). */
@Composable
fun GradeNumeros(
    m: Modalidade,
    situacao: (Int) -> SituacaoNumero,
    aoTocar: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val cor = corModalidade(m.cor)
    val colunas = colunasDoVolante(m)
    val numeros = Volante.universo(m).toList()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        numeros.chunked(colunas).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                linha.forEach { n ->
                    Celula(
                        texto = m.rotulo(n),
                        situacao = situacao(n),
                        cor = cor,
                        forma = CircleShape,
                        modifier = Modifier.weight(1f),
                        aoTocar = aoTocar?.let { { it(n) } },
                    )
                }
                repeat(colunas - linha.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Trevos da +Milionária (1 a 6). */
@Composable
fun GradeTrevos(
    m: Modalidade,
    situacao: (Int) -> SituacaoNumero,
    aoTocar: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val cor = corModalidade(m.cor)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        Volante.TREVOS.forEach { t ->
            Celula(
                texto = t.toString(),
                situacao = situacao(t),
                cor = cor,
                forma = RoundedCornerShape(30),
                modifier = Modifier.weight(1f),
                aoTocar = aoTocar?.let { { it(t) } },
            )
        }
    }
}

/** Super Sete: 7 colunas com os números de 0 a 9; um por coluna. */
@Composable
fun GradeColunas(
    m: Modalidade,
    situacao: (coluna: Int, numero: Int) -> SituacaoNumero,
    aoTocar: ((coluna: Int, numero: Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val cor = corModalidade(m.cor)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            (1..Volante.COLUNAS_SUPER_SETE).forEach { c ->
                Text(
                    "C$c",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        (0..9).forEach { n ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                (0 until Volante.COLUNAS_SUPER_SETE).forEach { c ->
                    Celula(
                        texto = n.toString(),
                        situacao = situacao(c, n),
                        cor = cor,
                        forma = RoundedCornerShape(30),
                        modifier = Modifier.weight(1f),
                        aoTocar = aoTocar?.let { { it(c, n) } },
                    )
                }
            }
        }
    }
}

/** Loteca: palpite 1, X ou 2 em cada um dos 14 jogos. */
@Composable
fun GradeLoteca(
    m: Modalidade,
    rotulos: List<String>,
    palpites: List<String>,
    resultados: List<String?> = emptyList(),
    aoEscolher: ((jogo: Int, opcao: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val cor = corModalidade(m.cor)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until Volante.JOGOS_LOTECA).forEach { j ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${j + 1}. ${rotulos.getOrNull(j)?.takeIf { it.isNotBlank() } ?: "Jogo ${j + 1}"}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Volante.OPCOES_LOTECA.forEach { op ->
                    val marcado = palpites.getOrNull(j) == op
                    val resultado = resultados.getOrNull(j)
                    val sit = when {
                        resultado == null -> if (marcado) SituacaoNumero.MARCADO else SituacaoNumero.LIVRE
                        marcado && resultado == op -> SituacaoNumero.ACERTO
                        marcado -> SituacaoNumero.ERRO
                        resultado == op -> SituacaoNumero.SORTEADO
                        else -> SituacaoNumero.LIVRE
                    }
                    Celula(
                        texto = op,
                        situacao = sit,
                        cor = cor,
                        forma = RoundedCornerShape(30),
                        modifier = Modifier.width(44.dp).padding(start = 4.dp),
                        aoTocar = aoEscolher?.let { { it(j, op) } },
                    )
                }
            }
        }
    }
}

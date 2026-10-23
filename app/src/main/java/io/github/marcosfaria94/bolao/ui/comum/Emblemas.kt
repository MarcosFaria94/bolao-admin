package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.tema.corModalidade

/** Cores dos círculos dos emblemas dos lançamentos (iguais nos dois temas). */
object CoresEmblema {
    val APORTE = Color(0xFF1F8F5F)
    val RATEIO = Color(0xFF2557D6)
    val EXTRATO = Color(0xFF7C3AED)
    val SALDO_INICIAL = Color(0xFF0E9F8A)
    val RENDIMENTO = Color(0xFF15803D)
    val COMPLEMENTO = Color(0xFF9333EA)
    val PREMIO = Color(0xFFB7791F)
}

private fun Color.clarear(f: Float): Color = lerp(this, Color.White, f)
private fun Color.escurecer(f: Float): Color = lerp(this, Color.Black, f)

/** Fundo dos círculos: a cor com um leve degradê de cima para baixo. */
private fun fundoCirculo(cor: Color): Brush = Brush.verticalGradient(listOf(cor.clarear(0.12f), cor.escurecer(0.10f)))

/**
 * Tamanho de letra preso ao tamanho do círculo: não cresce com a letra maior do sistema
 * (o texto tem de caber dentro do emblema).
 */
@Composable
private fun Dp.emSp(): TextUnit = with(LocalDensity.current) { this@emSp.toSp() }

/**
 * Emblema de uma modalidade: círculo na cor dela com a sigla (ou um [desenho], como o troféu do
 * prêmio) e, embaixo, o número do concurso. Sem concurso, só a sigla, um pouco maior.
 * [contorno]: anel claro em volta, para destacar o círculo num fundo escuro.
 */
@Composable
fun EmblemaModalidade(
    m: Modalidade,
    concurso: Int?,
    tamanho: Dp = 56.dp,
    modifier: Modifier = Modifier,
    desenho: ImageVector? = null,
    contorno: Color? = null,
) {
    val numero = concurso?.takeIf { it > 0 }?.toString()
    val letraSigla = (tamanho * if (numero != null) 0.33f else 0.40f).emSp()
    val letraNumero = (tamanho * 0.20f).emSp()
    Box(
        modifier
            .size(tamanho)
            .then(if (contorno != null) Modifier.border(2.dp, contorno, CircleShape) else Modifier)
            .background(fundoCirculo(corModalidade(m.cor)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (desenho != null) {
                Image(desenho, contentDescription = null, modifier = Modifier.size(tamanho * if (numero != null) 0.50f else 0.66f))
            } else {
                Text(
                    m.sigla,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = letraSigla,
                    lineHeight = letraSigla * 1.05f,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
            if (numero != null) {
                Text(
                    numero,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = letraNumero,
                    lineHeight = letraNumero * 1.1f,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Emblema de um tipo de lançamento: círculo colorido com o desenho (cofrinho, moedas...). */
@Composable
fun EmblemaDesenho(desenho: ImageVector, cor: Color, tamanho: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(tamanho).background(fundoCirculo(cor), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(desenho, contentDescription = null, modifier = Modifier.size(tamanho * 0.75f))
    }
}

/** Emblema do saldo inicial: o cifrão. */
@Composable
fun EmblemaCifrao(tamanho: Dp = 56.dp, modifier: Modifier = Modifier) {
    val letra = (tamanho * 0.52f).emSp()
    Box(
        modifier.size(tamanho).background(fundoCirculo(CoresEmblema.SALDO_INICIAL), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$", color = Color.White, fontWeight = FontWeight.Black, fontSize = letra, lineHeight = letra, maxLines = 1)
    }
}

/**
 * Emblema de um lançamento no Histórico do Financeiro: a modalidade (sigla e concurso) nas
 * apostas, o troféu com o concurso nos prêmios, e um desenho colorido nos outros.
 */
@Composable
fun EmblemaLancamento(e: Estado, l: Lancamento, tamanho: Dp = 56.dp) {
    val a = l.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
    when (l.tipo) {
        TipoLancamento.APOSTA ->
            if (a != null) EmblemaModalidade(a.modalidade, a.concurso, tamanho)
            else EmblemaDesenho(Desenhos.Moedas, CoresEmblema.RATEIO, tamanho)
        TipoLancamento.PREMIO ->
            if (a != null) EmblemaModalidade(a.modalidade, a.concurso, tamanho, desenho = Desenhos.Trofeu)
            else EmblemaDesenho(Desenhos.Trofeu, CoresEmblema.PREMIO, tamanho)
        TipoLancamento.APORTE -> EmblemaDesenho(Desenhos.Cofrinho, CoresEmblema.APORTE, tamanho)
        TipoLancamento.RATEIO, TipoLancamento.DIVISAO_FINAL -> EmblemaDesenho(Desenhos.Moedas, CoresEmblema.RATEIO, tamanho)
        TipoLancamento.SALDO_INICIAL -> EmblemaCifrao(tamanho)
        TipoLancamento.RENDIMENTO -> EmblemaDesenho(Desenhos.Rendimento, CoresEmblema.RENDIMENTO, tamanho)
        TipoLancamento.COMPLEMENTO -> EmblemaDesenho(Desenhos.Complemento, CoresEmblema.COMPLEMENTO, tamanho)
    }
}

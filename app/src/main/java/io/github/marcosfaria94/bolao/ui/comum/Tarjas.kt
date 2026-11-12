package io.github.marcosfaria94.bolao.ui.comum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Tarjas
import io.github.marcosfaria94.bolao.imagem.Fontes
import io.github.marcosfaria94.bolao.ui.tema.Tarja
import kotlin.math.cos
import kotlin.math.sin

/** A letra das tarjas (Jost ExtraBold, geométrica como a do site das Loterias Caixa). */
@Composable
private fun letraDaTarja(): FontFamily {
    val ctx = LocalContext.current
    return remember { FontFamily(androidx.compose.ui.text.font.Typeface(Fontes.jostExtra(ctx.applicationContext))) }
}

/** O trevo de quatro folhas em forma de coração, centrado em ([cx], [cy]). */
private fun DrawScope.trevo(cx: Float, cy: Float, diametro: Float, clara: Color, cheia: Color) {
    for ((angulo, ehCheia) in Tarjas.FOLHAS) {
        val (inicio, curvas) = Tarjas.folha(angulo, cx, cy, diametro)
        val p = Path().apply {
            moveTo(inicio[0], inicio[1])
            curvas.forEach { k -> cubicTo(k[0], k[1], k[2], k[3], k[4], k[5]) }
            close()
        }
        drawPath(p, if (ehCheia) cheia else clara)
    }
}

/** Os fogos de artifício da tarja da Mega da Virada (como os da Caixa), atrás do nome. */
private fun DrawScope.fogos() {
    val h = size.height
    val w = size.width
    // Perto das bordas de cima e de baixo, para não atrapalhar a leitura do nome.
    val explosoes = listOf(
        Triple(Offset(h * 2.05f, h * 0.30f), h * 0.24f, Color(0xFFFFE082)),
        Triple(Offset(w * 0.52f, h * 0.06f), h * 0.20f, Color(0xFFF8BBD0)),
        Triple(Offset(w * 0.70f, h * 0.96f), h * 0.20f, Color(0xFFFFF59D)),
        Triple(Offset(w * 0.90f, h * 0.06f), h * 0.18f, Color(0xFFFFFFFF)),
    )
    for ((c, r, cor) in explosoes) {
        for (i in 0 until 14) {
            val a = Math.toRadians(i * 360.0 / 14 + 7).toFloat()
            val de = Offset(c.x + cos(a) * r * 0.35f, c.y + sin(a) * r * 0.35f)
            val ate = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
            drawLine(cor.copy(alpha = 0.5f), de, ate, strokeWidth = h * 0.022f, cap = StrokeCap.Round)
        }
    }
}

/** O fundo da tarja: a cor, o canto da esquerda em diagonal e o trevo (e os fogos da Mega da Virada). */
private fun DrawScope.fundoDaTarja(c: Tarjas.Cores, comFogos: Boolean) {
    val h = size.height
    drawRect(Color(c.fundo))
    val faixa = Path().apply {
        moveTo(0f, 0f)
        lineTo(Tarjas.FAIXA_EM_CIMA * h, 0f)
        lineTo(Tarjas.FAIXA_EMBAIXO * h, h)
        lineTo(0f, h)
        close()
    }
    drawPath(faixa, Color(c.faixa))
    if (comFogos) fogos()
    trevo(Tarjas.CENTRO_DO_TREVO * h, h / 2f, Tarjas.TAMANHO_DO_TREVO * h, Color(c.folhaClara), Color(c.folhaCheia))
}

/** A Mega da Virada tem os fogos de artifício na tarja. */
private fun comFogos(especial: ConcursoEspecial?): Boolean = especial?.tarja?.contains("virada") == true

/**
 * A tarja da modalidade, como as das Loterias Caixa (desde a 0.27, no lugar da bola com a sigla): a cor,
 * o canto da esquerda em diagonal, o trevo e o nome em minúsculas, à direita. Num concurso especial, o
 * nome dele ("mega da virada"). Ocupa a largura que receber; a altura é [altura] e a letra acompanha a
 * altura (não cresce com a letra grande do celular). [cantos]: o arredondado (0 dentro de um cartão).
 */
@Composable
fun TarjaModalidade(
    m: Modalidade,
    modifier: Modifier = Modifier,
    altura: Dp = 44.dp,
    especial: ConcursoEspecial? = null,
    cantos: Dp = altura * Tarjas.CANTO,
) {
    val c = Tarjas.cores(m)
    val nome = Tarjas.nome(m, especial)
    val fogos = comFogos(especial)
    val letra = with(LocalDensity.current) { (altura * Tarjas.LETRA).toSp() }
    val estilo = TextStyle(
        fontFamily = letraDaTarja(),
        fontWeight = FontWeight.ExtraBold,
        fontSize = letra,
        lineHeight = letra * 1.1f,
        letterSpacing = 0.01.em,
    )
    Box(
        modifier
            .height(altura)
            .clip(RoundedCornerShape(cantos))
            .drawBehind { fundoDaTarja(c, fogos) }
            .semantics { contentDescription = "Tarja $nome" },
        contentAlignment = Alignment.CenterEnd,
    ) {
        TextoQueCabe(
            nome,
            estilo,
            cor = Color(c.texto),
            menor = (letra.value * 0.55f).sp,
            modifier = Modifier.padding(start = altura * Tarjas.INICIO_DO_NOME, end = altura * Tarjas.MARGEM_DO_NOME),
        )
    }
}

/** A tarja do concurso: a do concurso especial, quando o [concurso] é um ([Especiais]), ou a da modalidade. */
@Composable
fun TarjaDoConcurso(m: Modalidade, concurso: Int?, modifier: Modifier = Modifier, altura: Dp = 44.dp, cantos: Dp = altura * Tarjas.CANTO) =
    TarjaModalidade(m, modifier, altura, Especiais.doConcurso(m, concurso), cantos)

/**
 * O selo da modalidade (desde a 0.27, no lugar da bola nas listas: Histórico, Comprovantes, Indicadores e
 * os preços dos Parâmetros): a tarja em miniatura, um quadrado com a cor, o canto escuro em diagonal e o
 * trevo. O nome da modalidade fica no texto ao lado.
 */
@Composable
fun SeloModalidade(m: Modalidade, tamanho: Dp = 44.dp, modifier: Modifier = Modifier) {
    val c = Tarjas.cores(m)
    Box(
        modifier
            .size(tamanho)
            .clip(RoundedCornerShape(tamanho * 0.22f))
            .drawBehind {
                val s = size.height
                drawRect(Color(c.fundo))
                val faixa = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0.66f * s, 0f)
                    lineTo(0.30f * s, s)
                    lineTo(0f, s)
                    close()
                }
                drawPath(faixa, Color(c.faixa))
                trevo(s / 2f, s / 2f, 0.64f * s, Color(c.folhaClara), Color(c.folhaCheia))
            }
            .semantics { contentDescription = Tarjas.nome(m) },
    )
}

/** O preço numa etiqueta (letra grande, com fundo próprio): no canto dos cartões dos Jogos e do volante. */
@Composable
fun EtiquetaDePreco(preco: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Tarja.fundo(), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            preco,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
            color = Tarja.texto(),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Um cartão que abre e fecha tocando no título (drill-down; desde a 0.27, nos Detalhes do jogo: Regras,
 * Probabilidades e Quantidade de prêmios). Começa fechado ([inicial]); a seta diz se abre ou fecha.
 */
@Composable
fun CartaoQueAbre(
    titulo: String,
    modifier: Modifier = Modifier,
    inicial: Boolean = false,
    conteudo: @Composable () -> Unit,
) {
    var aberto by rememberSaveable(titulo) { mutableStateOf(inicial) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = if (aberto) "Fechar" else "Abrir") { aberto = !aberto }
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                if (aberto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = (if (aberto) "Fechar " else "Abrir ") + titulo,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (aberto) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { conteudo() }
        }
    }
}

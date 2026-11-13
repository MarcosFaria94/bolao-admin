package io.github.marcosfaria94.bolao.ui.jogos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.IndiceEspecial
import io.github.marcosfaria94.bolao.domain.Especiais as EspeciaisDaCaixa
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.tema.corDestaque
import io.github.marcosfaria94.bolao.ui.tema.corEntrada
import kotlin.math.roundToLong

/** Endereço da página que explica o k1 e o k2 (tocar em "Indicadores", nos Jogos). */
const val ROTA_INDICES = "indices"

/*
 * Os textos desta página usam o espaço que não quebra depois do "R$" (\u00A0): o valor nunca fica
 * partido no fim da linha.
 */

/** Os 5 passos da conta do k1, em linhas gerais (do guia "k1 e k2"). */
private val PASSOS = listOf(
    "Prever as apostas" to "Estima quantas apostas o próximo concurso vai receber, pelo prêmio estimado, pelas apostas do último concurso e pelo dia do sorteio.",
    "Montar os potes" to "Calcula quanto do dinheiro das apostas vai para cada faixa de prêmio, com o acumulado na principal.",
    "Dividir entre os ganhadores" to "Considera a chance de levar sozinho ou de dividir o prêmio com outros ganhadores.",
    "Descontar o IR" to "Tira os 30% do imposto de renda dos prêmios acima de R\$\u00A02.428,80.",
    "Somar e dividir pelo preço" to "Em cada faixa, chance × prêmio médio; soma tudo e divide pelo preço da aposta simples.",
)

/** Como ler os índices. */
private val COMO_LER = listOf(
    "Abaixo de 1 é perda média" to "Em média, toda aposta perde dinheiro: é assim que a loteria funciona. O k1 aponta o jogo que perde menos.",
    "k1 alto com k2 baixo" to "O valor está no prêmio grande, que quase nunca sai (como na Mega-Sena, que costuma ter k2 zero).",
    "Na aba Jogos" to "A lista vem em ordem de k1, e o concurso em destaque é o de maior k1.",
    "Sem k1 e k2" to "Loteca e Federal não têm os índices.",
    "Concursos especiais" to "Na Mega da Virada, na Quina de São João e nos outros especiais, o k1 e o k2 vêm dos últimos especiais já sorteados (veja abaixo).",
)

/** Quantos especiais entram na média, por extenso (o texto da explicação). */
private val QUANTOS_ESPECIAIS = IndiceEspecial.QUANTOS

/** A explicação do k1 e do k2 dos concursos especiais (desde a 0.33). */
val TEXTO_DOS_ESPECIAIS = listOf(
    "No concurso especial o prêmio não acumula: tudo o que vai para os prêmios, com o acumulado do ano todo, sai no próprio sorteio (sem ganhador numa faixa, o prêmio desce para a de baixo).",
    "Por isso o retorno de um especial já sorteado é exato: o que ele pagou, já sem o IR, dividido pelo que arrecadou. O k2 é a mesma conta, só com os prêmios frequentes.",
    "O k1 e o k2 do próximo especial são a média ponderada desse retorno nos últimos $QUANTOS_ESPECIAIS especiais do mesmo tipo (a Mega da Virada só com as outras Mega da Virada): o mais recente pesa mais (5, depois 4, 3, 2 e 1).",
)

/**
 * Como é o cálculo do k1 e do k2 (desde a 0.19; tocar na Legenda dos Jogos), em linhas gerais: o que
 * cada índice quer dizer, a barra "de cada R$ 1,00" com o jogo em destaque de agora (os índices da
 * Caixa do momento), a receita em 5 passos, as fórmulas, os concursos especiais (desde a 0.33) e como ler. A raiz cuida de voltar (puxando
 * para baixo ou deslizando para o lado).
 */
@Composable
fun IndicesTela() {
    val indices = LocalContext.current.bolao.indices
    val itens by indices.itens.collectAsStateWithLifecycle()
    val exemplo = remember(itens) { indices.maiorK1(itens) }
    val estimativas by indices.especiais.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "intro") {
            Text(
                "Os dois números que o app usa para comparar os jogos: quanto cada aposta devolve, em média, do que a gente aposta.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        item(key = "k1") {
            CartaoDoIndice(
                "k1", "Retorno médio", corDestaque(),
                "Quanto volta, em média, de cada R\$\u00A01,00 apostado no próximo concurso. Soma todos os prêmios, do menor ao principal, já sem o imposto de renda.",
            )
        }
        item(key = "k2") {
            CartaoDoIndice(
                "k2", "Retorno médio frequente", corEntrada(),
                "A parte do k1 que vem dos prêmios mais fáceis (chance de pelo menos 1 em 1.000). É o retorno do dia a dia; o resto depende de prêmios raros.",
            )
        }
        item(key = "odds") {
            CartaoDoIndice(
                "odds", "Chance de prêmio", MaterialTheme.colorScheme.onSurfaceVariant,
                "De quantas apostas, em média, uma ganha algum prêmio: 1:34 quer dizer 1 em 34.",
            )
        }
        if (exemplo?.k1 != null) item(key = "exemplo") { Exemplo(exemplo) }
        item(key = "titulo-passos") { Titulo("A RECEITA DO k1 EM 5 PASSOS") }
        item(key = "passos") {
            Bloco {
                PASSOS.forEachIndexed { i, (titulo, texto) ->
                    Row(Modifier.padding(vertical = 7.dp)) {
                        Box(
                            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(titulo, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 21.sp))
                            Text(texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item(key = "formulas") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Formula("k1 = Σ (chance × prêmio líquido médio) ÷ preço")
                    Formula("k2 = a mesma soma, só com as faixas de chance de pelo menos 1 em 1.000")
                    Text(
                        "O Σ quer dizer \"some todas as faixas\" (sena, quina, quadra...). Os índices são da aposta simples e mudam a cada concurso.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item(key = "titulo-especiais") { Titulo("CONCURSOS ESPECIAIS") }
        item(key = "especiais") { Especiais(estimativas) }
        item(key = "titulo-ler") { Titulo("COMO LER") }
        item(key = "ler") {
            Bloco {
                COMO_LER.forEach { (titulo, texto) ->
                    Column(Modifier.padding(vertical = 7.dp)) {
                        Text(titulo, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 21.sp))
                        Text(texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "rodape") {
            Text(
                "Os dados de cada concurso vêm da API oficial das Loterias Caixa. O k1 é uma média matemática de longo prazo: não muda a chance de ganhar e não é promessa de retorno.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * O k1 e o k2 dos concursos especiais (desde a 0.33): a explicação e, para cada modalidade com especial, a
 * estimativa e os especiais que entraram na média (o concurso, o retorno dele e o peso).
 */
@Composable
private fun Especiais(estimativas: Map<Modalidade, IndiceEspecial.Estimativa>) {
    val suave = MaterialTheme.colorScheme.onSurfaceVariant
    Bloco {
        Column(Modifier.padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TEXTO_DOS_ESPECIAIS.forEach { t ->
                Text(t, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp), color = suave)
            }
            Modalidade.ATIVAS.forEach { m ->
                val e = estimativas[m] ?: return@forEach
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeloModalidade(m, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(EspeciaisDaCaixa.nomeDoEspecial(m) ?: m.nome, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 21.sp))
                        Text("k1 ${textoIndice(e.k1)} · k2 ${textoIndice(e.k2)}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = suave)
                    }
                }
                e.amostras.forEach { a ->
                    Row(Modifier.fillMaxWidth().padding(start = 52.dp)) {
                        // O dia do sorteio (sem ele, o concurso), numa linha só.
                        Text(a.data ?: "Concurso ${a.numero}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp), color = suave, maxLines = 1, modifier = Modifier.weight(1f))
                        Text("k1 ${textoIndice(a.k1)} · peso ${a.peso.toInt()}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp), color = suave, maxLines = 1)
                    }
                }
            }
            if (estimativas.isEmpty()) {
                Text("Sem os especiais já sorteados no celular agora.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp), color = suave)
            }
        }
    }
}

/** Título das partes ("COMO LER"), como os grupos dos Ajustes. */
@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 0.8.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp),
    )
}

@Composable
private fun Bloco(conteudo: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) { conteudo() }
    }
}

/** Um índice: a sigla grande na [cor], o nome e a explicação. */
@Composable
private fun CartaoDoIndice(sigla: String, nome: String, cor: Color, texto: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(width = 58.dp, height = 44.dp).background(cor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(sigla, color = cor, fontWeight = FontWeight.Bold, fontSize = if (sigla.length > 2) 17.sp else 22.sp, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(nome, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp))
                Text(texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Formula(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp, lineHeight = 21.sp),
        fontWeight = FontWeight.SemiBold,
    )
}

/** "R$ 0,76" (um valor por real apostado). */
private fun porReal(v: Double): String = Dinheiro.rs((v * 100).roundToLong().coerceAtLeast(0))

/**
 * O exemplo de agora: o jogo em destaque (o de maior k1) e a barra de R$ 1,00 dividida em prêmios
 * frequentes (k2, verde), prêmios raros (k1 − k2, dourado) e o que não volta, em média (cinza).
 */
@Composable
private fun Exemplo(item: Indices.Item) {
    val k1 = item.k1 ?: return
    val k2 = (item.k2 ?: 0.0).coerceIn(0.0, k1)
    val verde = corEntrada()
    val dourado = corDestaque()
    val cinza = MaterialTheme.colorScheme.surfaceContainerHighest
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "DE CADA R\$\u00A01,00 APOSTADO, AGORA",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, letterSpacing = 0.8.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeloModalidade(item.modalidade, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.modalidade.nome, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp))
                    Text(
                        "k1 ${textoIndice(k1)} · k2 ${textoIndice(k2)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(porReal(k1), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), fontWeight = FontWeight.Bold)
            }
            // A barra inteira é R$ 1,00 (devolver tudo o que se aposta).
            Row(
                Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(cinza),
            ) {
                if (k2 > 0.0) Box(Modifier.weight(k2.toFloat().coerceAtLeast(0.001f)).fillMaxHeight().background(verde))
                if (k1 - k2 > 0.0) Box(Modifier.weight((k1 - k2).toFloat().coerceAtLeast(0.001f)).fillMaxHeight().background(dourado))
                if (k1 < 1.0) Box(Modifier.weight((1.0 - k1).toFloat().coerceAtLeast(0.001f)).fillMaxHeight())
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinhaDaLegenda(verde, "Prêmios frequentes (k2)", porReal(k2))
                LinhaDaLegenda(dourado, "Prêmios raros (k1 − k2)", porReal(k1 - k2))
                LinhaDaLegenda(cinza, "Não volta, em média", porReal((1.0 - k1).coerceAtLeast(0.0)))
            }
        }
    }
}

@Composable
private fun LinhaDaLegenda(cor: Color, texto: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(cor, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp), modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp), fontWeight = FontWeight.SemiBold)
    }
}

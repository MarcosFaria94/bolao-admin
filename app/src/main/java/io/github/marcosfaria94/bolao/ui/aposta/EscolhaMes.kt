package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.tema.corModalidade
import kotlin.random.Random

/** O mês da lista com a mesma grafia de [texto] (sem diferença de acento ou de maiúscula), ou null. */
fun mesDaLista(texto: String?): String? {
    if (texto.isNullOrBlank()) return null
    val t = Conferencia.normalizar(texto)
    return MESES_DE_SORTE.firstOrNull { Conferencia.normalizar(it) == t }
}

/** As três letras do mês, em maiúsculas: "JAN", "FEV", "MAR"... */
fun siglaDoMes(mes: String): String = mes.take(3).uppercase()

/**
 * Um mês sorteado para o "Sortear" do Mês de Sorte (Dia de Sorte): um dos 12, sem repetir o [atual] e,
 * desde a 0.22, de preferência um que as outras apostas do mesmo concurso ainda não têm ([usados]): com
 * 12 meses, repetir por acaso dentro de um conjunto de apostas é comum.
 */
fun mesAleatorio(atual: String?, sorteio: Random = Random.Default, usados: List<String> = emptyList()): String {
    val escolhido = mesDaLista(atual)
    val opcoes = MESES_DE_SORTE.filter { it != escolhido }
    val vezes = usados.mapNotNull { mesDaLista(it) }.groupingBy { it }.eachCount()
    val menor = opcoes.minOf { vezes[it] ?: 0 }
    return opcoes.filter { (vezes[it] ?: 0) == menor }.random(sorteio)
}

/**
 * Botão do Mês de Sorte (Dia de Sorte; desde a 0.19, no lugar do menu que descia): as três letras do
 * mês e o nome; abre a lista dos meses, que funciona como a dos times da Timemania.
 */
@Composable
fun BotaoMesDeSorte(atual: String?, usados: List<String> = emptyList(), aoEscolher: (String) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    val mes = mesDaLista(atual)
    val cor = corModalidade(Modalidade.DIA_DE_SORTE.cor)
    OutlinedButton(
        onClick = { aberto = true },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (mes != null) {
            SeloDoMes(mes, cor, 34.dp)
        } else {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = cor, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Mês de Sorte", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(mes ?: atual?.takeIf { it.isNotBlank() } ?: "Escolher o mês", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Text("Trocar", style = MaterialTheme.typography.labelLarge)
    }
    if (aberto) {
        DialogoMeses(
            atual = atual,
            aoEscolher = {
                aoEscolher(it)
                aberto = false
            },
            aoFechar = { aberto = false },
            usados = usados,
        )
    }
}

/** As três letras do mês num quadradinho na cor do Dia de Sorte. */
@Composable
private fun SeloDoMes(mes: String, cor: Color, tamanho: Dp) {
    Box(
        Modifier.size(tamanho).background(cor.copy(alpha = 0.16f), RoundedCornerShape(tamanho * 0.26f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            siglaDoMes(mes),
            color = cor,
            fontSize = (tamanho.value * 0.33f).sp,
            lineHeight = (tamanho.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Tamanho do quadradinho do mês na lista. */
private val SELO = 40.dp

/**
 * A lista dos meses do Dia de Sorte (desde a 0.19), como a dos times: um mês por linha, de janeiro a
 * dezembro. Tocar num mês escolhe e fecha; o "Sortear" do alto (ou puxar a lista para baixo) escolhe
 * um mês ao acaso, fecha e avisa numa mensagem rápida qual saiu; deslizar o dedo para os lados (ou o
 * voltar do celular) fecha, com o mês que já estava.
 */
@Composable
fun DialogoMeses(atual: String?, aoEscolher: (String) -> Unit, aoFechar: () -> Unit, usados: List<String> = emptyList()) {
    val repo = LocalContext.current.bolao.repo
    val cor = corModalidade(Modalidade.DIA_DE_SORTE.cor)
    val escolhido = mesDaLista(atual)

    /** Um mês ao acaso: escolhe, fecha e avisa qual saiu. */
    fun sortear() {
        val m = mesAleatorio(atual, usados = usados)
        aoEscolher(m)
        repo.avisar("Mês sorteado: $m")
    }

    DialogoComSorteio(titulo = "Mês de Sorte", sortear = ::sortear, aoFechar = aoFechar) {
        items(MESES_DE_SORTE, key = { it }) { mes ->
            val este = mes == escolhido
            LinhaDaEscolha(este, cor, aoTocar = { aoEscolher(mes) }, recuo = SELO + 14.dp) {
                SeloDoMes(mes, cor, SELO)
                Spacer(Modifier.width(14.dp))
                Text(
                    mes,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                    fontWeight = if (este) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                if (este) Icon(Icons.Outlined.Check, contentDescription = "Escolhido", tint = cor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

package io.github.marcosfaria94.bolao.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.ui.comum.Secao
import io.github.marcosfaria94.bolao.ui.home.SEM_DIA_DEFINIDO
import kotlinx.coroutines.launch

/** Os nomes dos parâmetros (as chaves da tela; os testes procuram por eles). */
const val PARAMETRO_SEM_CONFIRMACAO = "Registrar sem confirmação"
const val PARAMETRO_MAIS_NUMEROS = "Apostas com mais números"
const val PARAMETRO_TEIMOSINHA = "Teimosinha"

/** O que a linha "Parâmetros" da lista dos Ajustes mostra à direita: o calendário. */
fun resumoDosParametros(p: Parametros): String = when {
    p.semDiaDefinido -> SEM_DIA_DEFINIDO
    p.dias.size == 1 -> "1 dia por mês"
    else -> "${p.dias.size} dias por mês"
}

/** "10 apostas por mês: dias 1, 4, 7... e 28." */
fun textoDoCalendario(dias: List<Int>): String = when (dias.size) {
    0 -> "Nenhum dia marcado."
    1 -> "1 aposta por mês: dia ${dias[0]}."
    else -> "${dias.size} apostas por mês: dias ${Calendario.texto(dias)}."
}

/**
 * Ajustes > Parâmetros (desde a 0.22): as escolhas de como a sessão funciona. Em "Apostas": registrar
 * sem a janela de confirmação, apostas com mais números e a teimosinha. Em "Calendário de apostas": os
 * dias do mês com aposta (tocar marca e desmarca) ou "Sem dia definido". Cada toque já vale.
 */
@Composable
internal fun ParametrosDaSessao(estado: Estado) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val escopo = rememberCoroutineScope()
    val p = estado.parametros
    fun mudar(f: (Parametros) -> Parametros) {
        escopo.launch { runCatching { repo.atualizar { it.copy(config = it.config.copy(parametros = f(it.config.parametros))) } } }
    }

    Secao("Apostas") {
        Chave(
            PARAMETRO_SEM_CONFIRMACAO,
            "Registra a aposta direto, sem a mensagem \"Deseja registrar aposta?\".",
            p.semConfirmacao,
        ) { v -> mudar { it.copy(semConfirmacao = v) } }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Chave(
            PARAMETRO_MAIS_NUMEROS,
            "Deixa marcar mais números que a aposta simples. O preço acompanha a quantidade.",
            p.maisNumeros,
        ) { v -> mudar { it.copy(maisNumeros = v) } }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Chave(
            PARAMETRO_TEIMOSINHA,
            "Mostra no volante a opção de repetir a mesma aposta em vários concursos seguidos.",
            p.teimosinha,
        ) { v -> mudar { it.copy(teimosinha = v) } }
    }

    Secao("Calendário de apostas") {
        Chave(
            SEM_DIA_DEFINIDO,
            "Sem dias marcados: a Home mostra \"Próxima aposta: sem dia definido\" e não há aviso de dia de aposta.",
            p.semDiaDefinido,
        ) { v -> mudar { it.copy(diasDeAposta = if (v) emptyList() else null) } }
        if (!p.semDiaDefinido) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Toque nos dias do mês com aposta:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GradeDeDias(p.dias) { dia ->
                    mudar { atual ->
                        val novos = if (dia in atual.dias) atual.dias - dia else (atual.dias + dia).sorted()
                        // Os mesmos dias do termo voltam a ser "os do termo" (null).
                        atual.copy(diasDeAposta = novos.takeIf { it != Calendario.DIAS_PROGRAMADOS })
                    }
                }
                Text(textoDoCalendario(p.dias), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (p.dias.any { it > 28 }) {
                    Text(
                        "Num mês que não tem o dia marcado (29, 30 ou 31), esse dia fica sem aposta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!estado.individual) {
                    Text(
                        "O provisionado do rateio acompanha o calendário: o aporte do mês dividido pelas apostas do mês.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (p.diasDeAposta != null) {
                    TextButton(onClick = { mudar { it.copy(diasDeAposta = null) } }) {
                        Text(if (estado.enviaAoGrupo) "Usar os dias do termo" else "Usar os dias padrão")
                    }
                }
            }
        }
    }
}

/** Uma chave de ligar e desligar: o nome, a explicação e a chave (a linha inteira liga e desliga). */
@Composable
private fun Chave(titulo: String, texto: String, ligada: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = ligada, role = Role.Switch, onValueChange = aoMudar)
            .padding(vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = ligada, onCheckedChange = null)
    }
}

/** Os dias do mês (1 a 31) em 7 colunas: os marcados na cor do app; tocar marca e desmarca. */
@Composable
private fun GradeDeDias(marcados: List<Int>, aoTocar: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..31).chunked(7).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                linha.forEach { dia ->
                    val marcado = dia in marcados
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (marcado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { aoTocar(dia) }
                            .semantics {
                                contentDescription = "Dia $dia"
                                selected = marcado
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            dia.toString(),
                            fontSize = 15.sp,
                            fontWeight = if (marcado) FontWeight.Bold else FontWeight.Normal,
                            color = if (marcado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(7 - linha.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

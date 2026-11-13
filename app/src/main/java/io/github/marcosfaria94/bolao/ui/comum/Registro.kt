package io.github.marcosfaria94.bolao.ui.comum

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.notificacoes.Vigia

/** Aviso que vai em toda confirmação de registro (o registro só cresce). */
const val AVISO_DEFINITIVO = "Depois de registrado, nada pode ser alterado nem desfeito."

/** O botão de registrar: "Registrar e enviar" (o WhatsApp abre em seguida) ou, no bolão informal, só "Registrar". */
fun textoDeRegistrar(e: Estado): String = if (e.enviaAoGrupo) "Registrar e enviar" else "Registrar"

/** O fim das confirmações de registro: o WhatsApp abre com o comprovante (no bolão informal, ele fica só no app). */
fun depoisDoRegistro(e: Estado, comOQue: String = "o comprovante"): String =
    if (e.enviaAoGrupo) "Em seguida o WhatsApp abre com $comOQue." else "O comprovante fica guardado no app."

/** Pé das telas de registro: "Visualizar comprovante" e "Registrar e enviar". */
@Composable
fun BotoesDeRegistro(
    habilitado: Boolean,
    trabalhando: Boolean,
    aoVisualizar: () -> Unit,
    aoRegistrar: () -> Unit,
    modifier: Modifier = Modifier,
    textoVisualizar: String = "Visualizar comprovante",
    textoRegistrar: String = "Registrar e enviar",
    visualizarHabilitado: Boolean = true,
    mostrarVisualizar: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        if (mostrarVisualizar) {
            OutlinedButton(
                onClick = aoVisualizar,
                enabled = visualizarHabilitado && !trabalhando,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(textoVisualizar) }
        }
        Button(
            onClick = aoRegistrar,
            enabled = habilitado && !trabalhando,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (trabalhando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(textoRegistrar)
        }
    }
}

/**
 * Grava um registro que cria um comprovante, desenha a imagem dele e (se [enviar])
 * abre o WhatsApp com o título, a imagem e os anexos. Devolve o comprovante criado.
 */
suspend fun registrarComprovante(
    context: Context,
    mudanca: (Estado) -> Estado,
    enviar: Boolean = true,
    repo: Repositorio = context.bolao.repo,
): Comprovante {
    val antes = repo.atual().comprovantes.size
    val novo = repo.atualizar(mudanca)
    check(novo.comprovantes.size > antes) { "O registro não criou o comprovante." }
    val c = novo.comprovantes.last()
    Recibos.arquivo(context, novo, c, repo)
    // No bolão informal (desde a 0.22) nada vai ao grupo: o comprovante fica só no app.
    if (enviar && novo.enviaAoGrupo) Compartilhar.comprovante(context, novo, c)
    return c
}

/** O botão que confirma as apostas (desde a 0.36: sem o comprovante da Caixa). */
const val CONFIRMAR = "Confirmar"

/**
 * Confirma as apostas [ids] (desde a 0.36: feitas no site da Caixa, sem cobrar o comprovante da Caixa; anexado, ele vai
 * junto): cria o comprovante do bolão de cada uma, na ordem do registro, sem abrir o WhatsApp (ficam prontos para enviar
 * ao grupo, nos Comprovantes). As que já não esperam a confirmação ficam de fora. Devolve quantas foram confirmadas.
 */
suspend fun confirmarApostas(context: Context, ids: Collection<String>, repo: Repositorio = context.bolao.repo): Int {
    val e = repo.atual()
    val agora = System.currentTimeMillis()
    fun registro(a: Aposta) = e.lancamentos.firstOrNull { it.apostaId == a.id && it.tipo == TipoLancamento.APOSTA }?.criadoEm ?: a.criadaEm
    val fila = e.apostas.filter { it.id in ids && it.registrada && it.pendenteDeAnexo }.sortedBy { registro(it) }
    var feitas = 0
    fila.forEachIndexed { i, a ->
        registrarComprovante(context, { est -> Operacoes.aprovarAposta(est, a.id, agora + i) }, enviar = false, repo = repo)
        feitas++
    }
    // Confirmada depois do sorteio, a aposta já pode ser conferida e apurada: o Vigia vai agora.
    if (feitas > 0) Vigia.rodarAgora(context)
    return feitas
}

/** O título da janela da confirmação: "Confirmar a aposta Nº3?" ou "Confirmar as 3 apostas?". */
fun tituloDaConfirmacao(numeros: List<Int>): String =
    if (numeros.size == 1) "Confirmar a aposta Nº${numeros[0]}?" else "Confirmar as ${numeros.size} apostas?"

/** O texto da janela da confirmação: só depois de pagar no site da Caixa; o comprovante do bolão; sem volta. */
fun textoDaConfirmacao(apostas: Int, e: Estado): String {
    val uma = apostas == 1
    val comprovante = when {
        !e.enviaAoGrupo && uma -> "o comprovante do bolão é criado e fica guardado no app"
        !e.enviaAoGrupo -> "os comprovantes do bolão são criados e ficam guardados no app"
        uma -> "o comprovante do bolão é criado e fica pronto para enviar ao grupo"
        else -> "os comprovantes do bolão são criados e ficam prontos para enviar ao grupo"
    }
    return "Confirme só depois de pagar no site da Caixa. " +
        (if (uma) "A aposta passa a valer no bolão: " else "As apostas passam a valer no bolão: ") + comprovante + ". " +
        (if (uma) "Depois disso ela não pode mais ser descartada." else "Depois disso elas não podem mais ser descartadas.")
}

/** O aviso depois da confirmação: "Aposta confirmada: o comprovante está pronto para enviar ao grupo." */
fun mensagemDaConfirmacao(feitas: Int, e: Estado): String = when {
    feitas == 0 -> "Nenhuma aposta para confirmar."
    !e.enviaAoGrupo && feitas == 1 -> "Aposta confirmada: o comprovante do bolão ficou guardado no app."
    !e.enviaAoGrupo -> "$feitas apostas confirmadas: os comprovantes do bolão ficaram guardados no app."
    feitas == 1 -> "Aposta confirmada: o comprovante está pronto para enviar ao grupo."
    else -> "$feitas apostas confirmadas: os comprovantes estão prontos para enviar ao grupo."
}

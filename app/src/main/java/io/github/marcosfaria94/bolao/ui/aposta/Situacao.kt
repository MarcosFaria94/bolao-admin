package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.ui.tema.corAguardando
import io.github.marcosfaria94.bolao.ui.tema.corApuracao
import io.github.marcosfaria94.bolao.ui.tema.corPremiada
import java.time.LocalDate

/** Situação de uma aposta registrada. */
enum class Situacao(val texto: String) {
    AGUARDANDO("Aguardando sorteio"),
    APURACAO("Aguardando apuração"),
    PREMIADA_A_APURAR("Aposta premiada · aguardando apuração"),
    NAO_PREMIADA("Aposta não premiada"),
    PREMIADA("Aposta premiada"),
}

fun situacaoDe(a: Aposta, hoje: LocalDate): Situacao {
    val ap = a.apuracao
    return when {
        ap?.confirmadaEm != null -> if (ap.premio > 0) Situacao.PREMIADA else Situacao.NAO_PREMIADA
        ap != null && (ap.premio > 0 || ap.faixasPremiadas.isNotEmpty()) -> Situacao.PREMIADA_A_APURAR
        ap != null -> Situacao.APURACAO
        a.dataSorteio != null && a.dataSorteio.isBefore(hoje) -> Situacao.APURACAO
        else -> Situacao.AGUARDANDO
    }
}

/** A aposta ganhou algum prêmio (apurada ou ainda esperando a apuração). */
fun premiada(s: Situacao): Boolean = s == Situacao.PREMIADA || s == Situacao.PREMIADA_A_APURAR

/**
 * Texto da situação: antes do sorteio, quando ele acontece ("Sorteio hoje", "Sorteio amanhã",
 * "Sorteio daqui 3 dias"); depois, o resultado.
 */
fun textoSituacao(a: Aposta, s: Situacao, hoje: LocalDate): String =
    if (s == Situacao.AGUARDANDO && a.dataSorteio != null) Datas.sorteioEm(a.dataSorteio, hoje) else s.texto

/**
 * Situação curta, na tarja do Histórico ("Aposta Nº8 · Sorteio hoje"): antes do sorteio, quando ele
 * acontece; depois, "Premiada" ou "Não premiada" ("Em apuração" enquanto o resultado não chega).
 * O valor do prêmio não aparece aqui (é surpresa na página da aposta).
 */
fun situacaoCurta(a: Aposta, s: Situacao, hoje: LocalDate): String = when (s) {
    Situacao.AGUARDANDO -> a.dataSorteio?.let { Datas.sorteioEm(it, hoje) } ?: "Aguardando sorteio"
    Situacao.APURACAO -> "Em apuração"
    Situacao.PREMIADA, Situacao.PREMIADA_A_APURAR -> "Premiada"
    Situacao.NAO_PREMIADA -> "Não premiada"
}

@Composable
fun corDaSituacao(s: Situacao): Color = when (s) {
    Situacao.AGUARDANDO -> corAguardando()
    Situacao.APURACAO -> corApuracao()
    Situacao.PREMIADA, Situacao.PREMIADA_A_APURAR -> corPremiada()
    Situacao.NAO_PREMIADA -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** "Aposta Nº8": o título da aposta. O Nº recomeça a cada mês. */
fun tituloDaAposta(a: Aposta): String = "Aposta Nº${a.numero}"

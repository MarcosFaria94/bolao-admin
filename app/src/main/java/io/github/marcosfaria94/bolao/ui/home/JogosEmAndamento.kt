package io.github.marcosfaria94.bolao.ui.home

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.aposta.Situacao
import io.github.marcosfaria94.bolao.ui.aposta.nomeDoTime
import io.github.marcosfaria94.bolao.ui.aposta.situacaoDe
import java.time.LocalDate

/** Os jogos de um mesmo concurso que ainda estão em andamento (antes do sorteio). */
data class GrupoEmAndamento(val modalidade: Modalidade, val concurso: Int, val sorteio: LocalDate?, val apostas: List<Aposta>)

/**
 * Os jogos em andamento: as apostas registradas que ainda não foram sorteadas (inclusive as que
 * esperam o comprovante) e as que ainda falta registrar (volante pronto), agrupadas por modalidade,
 * concurso e data do sorteio, o sorteio mais próximo primeiro (sem data no fim). Dentro do grupo, as
 * registradas na ordem de registro e, depois, as que falta registrar.
 */
fun jogosEmAndamento(e: Estado, hoje: LocalDate): List<GrupoEmAndamento> =
    e.apostas
        .filter { !it.registrada || situacaoDe(it, hoje) == Situacao.AGUARDANDO }
        .groupBy { Triple(it.modalidade, it.concurso, it.dataSorteio) }
        .map { (k, lista) ->
            GrupoEmAndamento(
                k.first, k.second, k.third,
                lista.sortedWith(compareBy<Aposta>({ !it.registrada }, { it.dataAposta }, { it.criadaEm }, { it.numero })),
            )
        }
        .sortedWith(
            compareBy<GrupoEmAndamento> { it.sorteio == null }
                .thenBy { it.sorteio }
                .thenBy { it.modalidade.ordinal }
                .thenBy { it.concurso },
        )

/**
 * Os grupos na ordem dos Meus jogos: o sorteio mais próximo primeiro (sem data no fim) e, no mesmo dia,
 * o maior k1 do concurso ([k1]; sem k1, depois).
 */
fun emOrdemDeSorteioEK1(grupos: List<GrupoEmAndamento>, k1: (GrupoEmAndamento) -> Double?): List<GrupoEmAndamento> =
    grupos.sortedWith(
        compareBy<GrupoEmAndamento> { it.sorteio == null }
            .thenBy { it.sorteio }
            .thenBy { -(k1(it) ?: Double.NEGATIVE_INFINITY) }
            .thenBy { it.modalidade.ordinal }
            .thenBy { it.concurso },
    )

/** Quantos jogos estão em andamento. */
fun quantosEmAndamento(e: Estado, hoje: LocalDate): Int = jogosEmAndamento(e, hoje).sumOf { it.apostas.size }

/**
 * O que ainda falta numa aposta em andamento (o "Pendente" dos Meus jogos), ou null quando ela está
 * OK: registrada, com o comprovante da Caixa conferido e (no bolão) o comprovante enviado ao grupo.
 */
fun pendenciaDoJogo(e: Estado, a: Aposta): String? = when {
    !a.registrada -> "Falta registrar"
    Operacoes.aguardandoComprovante(a) -> "Sem comprovante"
    Operacoes.aguardandoConferencia(a) -> "A conferir"
    !e.individual && Operacoes.comprovanteDaAposta(e, a)?.let { !it.envioDispensado && !e.enviado(it) } == true -> "Enviar ao grupo"
    else -> null
}

/** O jogo numa linha, para os Meus jogos: as dezenas (ou quantas são), os trevos, o time, o mês, o bilhete. */
fun resumoDoJogo(a: Aposta): String {
    val m = a.modalidade
    return when (m.formato) {
        Formato.NUMEROS -> buildList {
            add(
                when {
                    a.numeros.isEmpty() -> "Sem números"
                    a.numeros.size > MAXIMO_DE_DEZENAS_NA_LINHA -> "${a.numeros.size} números"
                    else -> a.numeros.sorted().joinToString(" ") { m.rotulo(it) }
                },
            )
            if (a.trevos.isNotEmpty()) add("Trevos " + a.trevos.sorted().joinToString(" "))
            when (m.extra) {
                Extra.TIME_DO_CORACAO -> a.extra?.let { add(nomeDoTime(it) ?: it) }
                Extra.MES_DE_SORTE -> a.extra?.let { add(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                Extra.NENHUM -> Unit
            }
        }.joinToString(" · ")
        Formato.COLUNAS -> a.numeros.joinToString(" ") { if (it in 0..9) it.toString() else "-" }.ifEmpty { "Sem números" }
        Formato.LOTECA -> "${a.palpitesLoteca.count { it.isNotBlank() }} palpites"
        Formato.FEDERAL -> a.bilheteFederal?.let { "Bilhete $it" } ?: "Bilhete"
    }
}

/** Mais dezenas do que isto (Lotomania): só quantas são. */
private const val MAXIMO_DE_DEZENAS_NA_LINHA = 20

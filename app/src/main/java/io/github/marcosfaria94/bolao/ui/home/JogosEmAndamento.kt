package io.github.marcosfaria94.bolao.ui.home

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.aposta.Situacao
import io.github.marcosfaria94.bolao.ui.aposta.situacaoDe
import io.github.marcosfaria94.bolao.ui.comum.Etiqueta
import io.github.marcosfaria94.bolao.ui.comum.TomEtiqueta
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

/** Quantos jogos estão em andamento. */
fun quantosEmAndamento(e: Estado, hoje: LocalDate): Int = jogosEmAndamento(e, hoje).sumOf { it.apostas.size }

/** A aposta que ainda falta registrar, ou que espera o comprovante da Caixa (ou a conferência dele). */
fun etiquetaDoJogo(a: Aposta): Etiqueta? = when {
    !a.registrada -> Etiqueta("Falta registrar", TomEtiqueta.LARANJA)
    Operacoes.aguardandoComprovante(a) -> Etiqueta("Sem comprovante", TomEtiqueta.VERMELHO)
    Operacoes.aguardandoConferencia(a) -> Etiqueta("A conferir", TomEtiqueta.ROXO)
    else -> null
}

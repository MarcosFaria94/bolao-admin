package io.github.marcosfaria94.bolao.data

import java.time.LocalDate
import java.time.YearMonth

/** Atualiza dados gravados por versões anteriores do app. */
object Migracao {

    const val VERSAO = 2

    fun migrar(e: Estado, hoje: LocalDate): Estado {
        if (e.versao >= VERSAO) return e
        // Até a 0.2.6 (versão 1) o número do jogo era corrido (#121, #122...). Agora é por mês,
        // como no termo: Nº1 = primeira aposta do mês. Rascunhos ficam sem número até o registro.
        val novos = e.apostas.filter { it.registrada }
            .groupBy { YearMonth.from(it.dataAposta) }
            .values
            .flatMap { doMes -> doMes.sortedWith(compareBy({ it.dataAposta }, { it.criadaEm }, { it.numero })).mapIndexed { i, a -> a.id to i + 1 } }
            .toMap()
        val cadastro = e.cadastradoEm
            ?: e.lancamentos.minOfOrNull { it.data }
            ?: if (e.cadastroConcluido) hoje else null
        // Os registros antigos não têm comprovante (o comprovante único chegou na 0.3).
        val legado = listOfNotNull(
            e.lancamentos.maxOfOrNull { it.criadoEm },
            e.apostas.filter { it.registrada }.mapNotNull { it.apuracao?.confirmadaEm }.maxOrNull(),
            e.rateios.maxOfOrNull { it.criadoEm },
            e.extratos.maxOfOrNull { it.criadoEm },
        ).maxOrNull()
        return e.copy(
            versao = VERSAO,
            cadastradoEm = cadastro,
            apostas = e.apostas.map { a -> a.copy(numero = novos[a.id] ?: 0) },
            legadoAte = e.legadoAte ?: legado,
        )
    }
}

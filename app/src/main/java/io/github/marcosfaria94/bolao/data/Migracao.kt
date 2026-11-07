package io.github.marcosfaria94.bolao.data

import java.time.LocalDate
import java.time.YearMonth

/** Atualiza dados gravados por versões anteriores do app. */
object Migracao {

    const val VERSAO = 3

    fun migrar(e: Estado, hoje: LocalDate): Estado {
        var x = e
        if (x.versao < 2) x = paraVersao2(x, hoje)
        if (x.versao < 3) x = paraVersao3(x)
        return x
    }

    private fun paraVersao2(e: Estado, hoje: LocalDate): Estado {
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
            versao = 2,
            cadastradoEm = cadastro,
            apostas = e.apostas.map { a -> a.copy(numero = novos[a.id] ?: 0) },
            legadoAte = e.legadoAte ?: legado,
        )
    }

    private val NOTA_DO_CONCURSO = Regex("^Concurso: no app (\\d+); no comprovante (\\d+)\\.$")

    /**
     * Versão 3 (0.21): até a 0.20 o telefone do rodapé do comprovante ("4004 0104") podia virar o
     * concurso da aposta. As apostas que esperam a conferência com um concurso lido longe demais do
     * que estava no app voltam ao concurso do app, com a nota de que o número lido não parece certo.
     */
    private fun paraVersao3(e: Estado): Estado = e.copy(
        versao = 3,
        apostas = e.apostas.map { a ->
            if (!a.registrada || !a.pendenteDeAnexo || a.comprovante == null) return@map a
            val nota = a.notasDoAnexo.firstNotNullOfOrNull { n -> NOTA_DO_CONCURSO.find(n)?.let { n to it } } ?: return@map a
            val noApp = nota.second.groupValues[1].toInt()
            val lido = nota.second.groupValues[2].toInt()
            if (a.concurso != lido || kotlin.math.abs(lido - noApp) <= io.github.marcosfaria94.bolao.domain.LeituraComprovante.DIFERENCA_MAXIMA_DO_CONCURSO) return@map a
            a.copy(
                concurso = noApp,
                notasDoAnexo = a.notasDoAnexo.map { if (it == nota.first) Associacao.notaConcursoDuvidoso(lido, noApp) else it },
            )
        },
    )
}

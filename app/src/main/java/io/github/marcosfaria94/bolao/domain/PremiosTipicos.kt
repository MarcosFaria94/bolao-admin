package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.domain.indices.RegistroK
import kotlin.math.roundToLong

/**
 * Os prêmios típicos de cada faixa (desde a 0.32, o cartão "Prêmios típicos" dos Detalhes, em todas as
 * modalidades): quanto cada aposta ganhadora deve receber no próximo concurso e quanto o último concurso pagou.
 *
 * A estimativa é a do mesmo cálculo do k1 ([IndiceK]), sem o Imposto de Renda: a venda prevista de apostas (pelo
 * prêmio estimado da Caixa, o dia da semana e as vendas do concurso anterior), a parte da arrecadação que vai
 * para cada faixa, o acumulado (na faixa principal) e a chance de dividir com outros ganhadores; dela sai o valor
 * médio que uma aposta ganhadora recebe em cada faixa. As faixas de valor fixo pagam o valor da Caixa. A Loteca
 * não tem os parâmetros do k1: só o último concurso.
 */
object PremiosTipicos {

    /**
     * Uma linha da tabela: a [faixa] (o número da Caixa), o [nome], a [estimativa] para o próximo concurso e o que o
     * [ultimo] concurso pagou a cada ganhador (centavos; null = sem estimativa / ninguém acertou).
     */
    data class Linha(val faixa: Int, val nome: String, val estimativa: Long?, val ultimo: Long?)

    /**
     * A estimativa de cada faixa (pelo número da Caixa), em centavos, antes do IR: o valor médio que uma aposta
     * ganhadora recebe, E[prêmio | ganhou] = (parte do k1 da faixa) × preço ÷ chance da faixa. null quando o k1 não
     * sai (concurso especial, dados incompletos, modalidade sem parâmetros).
     */
    fun estimar(p: ParametrosK, chave: String, u: RegistroK, a: RegistroK, preco: Double? = null): Map<Int, Long>? {
        val m = p.modalidades[chave] ?: return null
        // O mesmo cálculo do k1, sem o Imposto de Renda (o prêmio como a Caixa divulga).
        val r = runCatching { IndiceK.calcular(p.copy(irAliquota = 0.0), chave, u, a, preco) }.getOrNull() ?: return null
        if (r.k1 == null) return null
        val usado = preco?.takeIf { it > 0 } ?: m.preco
        return m.faixas.filter { it.p > 0 }.associate { f ->
            f.faixa to ((r.k1PorFaixa[f.faixa] ?: 0.0) * usado / f.p * 100).roundToLong()
        }
    }

    /** O nome de cada faixa, na ordem da Caixa (a faixa 1 é a primeira). A Dupla Sena tem as 4 de cada sorteio. */
    fun nomes(m: Modalidade): List<String> = when (m) {
        Modalidade.DUPLA_SENA -> Probabilidades.colunas(m).map { "$it (1º)" } + Probabilidades.colunas(m).map { "$it (2º)" }
        Modalidade.FEDERAL -> emptyList()
        else -> Probabilidades.colunas(m)
    }

    /** As linhas da tabela: uma por faixa, com a [estimativa] (de [estimar]) e o pago no [ultimo] concurso. */
    fun linhas(m: Modalidade, estimativa: Map<Int, Long>?, ultimo: Concurso?): List<Linha> =
        nomes(m).mapIndexed { i, nome ->
            val faixa = i + 1
            val paga = ultimo?.faixas?.firstOrNull { it.faixa == faixa }?.takeIf { it.ganhadores > 0 && it.valor > 0 }?.valor
            Linha(faixa, nome, estimativa?.get(faixa)?.takeIf { it > 0 }, paga)
        }

    /** O valor na tabela: por extenso de R$ 100 mil para cima ("R$ 58,5 Milhões"), senão com os centavos. */
    fun valor(centavos: Long): String = if (centavos >= 10_000_000L) Dinheiro.porExtenso(centavos) else Dinheiro.rs(centavos)

    /** A nota da estimativa (embaixo da tabela). */
    const val NOTA_DA_ESTIMATIVA =
        "Estimativa: é só uma previsão do valor médio que cada aposta ganhadora recebe no próximo concurso, antes do " +
            "Imposto de Renda. O cálculo é o mesmo do k1: a venda de apostas prevista (pelo prêmio estimado da Caixa, " +
            "o dia da semana e as vendas do concurso anterior), a parte da arrecadação que vai para cada faixa, o " +
            "acumulado na faixa principal e a chance de dividir o prêmio com outros ganhadores. As faixas de valor " +
            "fixo pagam o valor da Caixa. O prêmio de verdade depende de quantas apostas forem feitas e de quantas acertarem."

    /** A nota da coluna do último concurso. */
    fun notaDoUltimo(concurso: Int): String = "Último concurso: quanto a Caixa pagou a cada ganhador no concurso $concurso (— = ninguém acertou)."

    /** Sem a estimativa (Loteca, concurso especial, dados que não chegaram). */
    const val SEM_ESTIMATIVA =
        "Sem a estimativa agora (a Loteca não tem os parâmetros do cálculo; no concurso especial e sem os dados da " +
            "Caixa, ela não sai): a tabela mostra só o último concurso."
}

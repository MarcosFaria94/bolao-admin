package io.github.marcosfaria94.bolao.domain

/**
 * O prêmio de um concurso, no alto do resultado nos Detalhes (desde a 0.35, acima do "Acumulou!"):
 * - o **prêmio estimado** que a Caixa divulgou para ele (o "prêmio estimado do próximo concurso" que veio com o
 *   concurso anterior, [anterior]: o mesmo valor que a página do jogo mostrava antes do sorteio; na Mega da Virada,
 *   o R$ 1 Bilhão);
 * - sem a estimativa (os primeiros concursos de algumas modalidades), o que a faixa principal **pagou**, se saiu;
 * - na Federal, o **1º prêmio**.
 * Sem nada disso (acumulou e não há estimativa), nenhum.
 */
object PremioDoConcurso {

    const val ROTULO_ESTIMADO = "Prêmio estimado"
    const val ROTULO_PAGO = "Prêmio pago"
    const val ROTULO_DA_FEDERAL = "1º prêmio"

    /** [valor] em centavos. */
    data class Premio(val rotulo: String, val valor: Long)

    fun de(c: Concurso, anterior: Concurso?): Premio? {
        val principal = c.faixas.minByOrNull { it.faixa }
        if (c.modalidade == Modalidade.FEDERAL) {
            return principal?.valor?.takeIf { it > 0 }?.let { Premio(ROTULO_DA_FEDERAL, it) }
        }
        anterior?.takeIf { it.modalidade == c.modalidade && it.numero == c.numero - 1 }
            ?.premioEstimadoProximo?.takeIf { it > 0 }
            ?.let { return Premio(ROTULO_ESTIMADO, it) }
        val pago = principal?.takeIf { it.ganhadores > 0 }?.let { it.ganhadores * it.valor } ?: 0L
        return if (pago > 0) Premio(ROTULO_PAGO, pago) else null
    }
}

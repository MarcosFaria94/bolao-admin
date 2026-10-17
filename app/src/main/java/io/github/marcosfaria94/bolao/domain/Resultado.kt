package io.github.marcosfaria94.bolao.domain

import java.time.LocalDate

/** Uma faixa de premiação de um concurso, como a Caixa divulga. */
data class FaixaPremio(
    val faixa: Int,
    val descricao: String,
    val ganhadores: Int,
    /** Valor por aposta ganhadora, em centavos. */
    val valor: Long,
)

/** Um jogo da Loteca (resultado ou programação). */
data class JogoLoteca(
    val ordem: Int,
    val time1: String,
    val time2: String,
    val gols1: Int?,
    val gols2: Int?,
) {
    /** "1" (vitória do time 1), "X" (empate) ou "2"; null se ainda não houver placar. */
    val resultado: String?
        get() = if (gols1 == null || gols2 == null) null else when {
            gols1 > gols2 -> "1"
            gols1 < gols2 -> "2"
            else -> "X"
        }
}

/** Dados de um concurso devolvidos pela API da Caixa. */
data class Concurso(
    val modalidade: Modalidade,
    val numero: Int,
    val data: LocalDate?,
    /** Dezenas em ordem crescente (Super Sete: na ordem das colunas). */
    val dezenas: List<Int>,
    val segundoSorteio: List<Int> = emptyList(),
    val trevos: List<Int> = emptyList(),
    /** Time do Coração (Timemania) ou Mês de Sorte (Dia de Sorte). */
    val timeOuMes: String? = null,
    val faixas: List<FaixaPremio> = emptyList(),
    val acumulado: Boolean = false,
    val jogosLoteca: List<JogoLoteca> = emptyList(),
    /** Federal: os 5 bilhetes premiados, do 1º ao 5º prêmio (6 dígitos). */
    val bilhetesFederal: List<String> = emptyList(),
    val proximoNumero: Int? = null,
    val proximaData: LocalDate? = null,
    /** Prêmio estimado do próximo concurso, em centavos. */
    val premioEstimadoProximo: Long? = null,
    /** Número do próximo concurso especial (final 0 ou 5 na Mega, etc.), quando a Caixa informa. */
    val numeroConcursoEspecial: Int? = null,
    val acumuladoEspecial: Long? = null,
) {
    /** O sorteio já aconteceu e os números saíram. */
    val temResultado: Boolean
        get() = dezenas.isNotEmpty() || bilhetesFederal.isNotEmpty() || jogosLoteca.any { it.resultado != null }
}

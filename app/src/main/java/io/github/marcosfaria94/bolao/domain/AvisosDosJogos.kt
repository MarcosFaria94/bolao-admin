package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Os avisos dos Parâmetros (desde a 0.38, Ajustes > Parâmetros > Avisos, todos desligados por padrão): o concurso que
 * acumulou, o k1 acima de x e o prêmio estimado acima de n. Cada concurso avisa uma vez só por sessão (as [chaves]); os
 * de um mesmo tipo vêm juntos, num aviso só ("Acumularam: Mega-Sena e Quina").
 */
object AvisosDosJogos {

    /** Até quantos dias depois do sorteio o "acumulou" ainda avisa (ligar o aviso não traz os concursos velhos). */
    const val DIAS_DO_ACUMULOU = 2L

    /**
     * Uma modalidade nos dados dos Jogos: o último concurso ([ultimo], o dia dele e se acumulou) e o próximo ([proximo],
     * o prêmio estimado e o k1). Os especiais anunciados entram só com o prêmio ([nome] = o nome do especial).
     */
    data class Alvo(
        val modalidade: Modalidade,
        val nome: String,
        val ultimo: Int?,
        val diaDoUltimo: LocalDate?,
        val acumulou: Boolean,
        val proximo: Int?,
        val premio: Long?,
        val k1: Double?,
        val destino: String,
    )

    data class Aviso(val chaves: List<String>, val titulo: String, val texto: String, val destino: String)

    fun chaveDoAcumulou(a: Alvo): String = "acumulou:${a.modalidade.name}:${a.ultimo}"
    fun chaveDoK1(a: Alvo): String = "k1:${a.modalidade.name}:${a.proximo}"
    fun chaveDoPremio(a: Alvo): String = "premio:${a.modalidade.name}:${a.proximo}"

    /** Os avisos que faltam dar agora com os parâmetros [p] ([avisados] = as chaves já avisadas nesta sessão). */
    fun devidos(p: Parametros, alvos: List<Alvo>, hoje: LocalDate, avisados: Set<String>): List<Aviso> = buildList {
        if (p.avisarAcumulado) {
            val novos = alvos.filter { a ->
                a.acumulou && a.ultimo != null && a.diaDoUltimo != null &&
                    ChronoUnit.DAYS.between(a.diaDoUltimo, hoje) in 0..DIAS_DO_ACUMULOU && chaveDoAcumulou(a) !in avisados
            }.distinctBy { it.modalidade to it.ultimo }
            if (novos.isNotEmpty()) {
                add(
                    Aviso(
                        chaves = novos.map(::chaveDoAcumulou),
                        titulo = (if (novos.size == 1) "Acumulou: " else "Acumularam: ") + nomes(novos),
                        texto = novos.joinToString(" ") { a ->
                            "${a.nome}: o concurso ${a.ultimo} não teve ganhador" +
                                (a.premio?.takeIf { it > 0 }?.let { v -> "; o ${a.proximo ?: "próximo"} tem prêmio estimado de ${Dinheiro.porExtenso(v)}" } ?: "") + "."
                        },
                        destino = destino(novos),
                    ),
                )
            }
        }
        if (p.avisarK1) {
            val novos = alvos.filter { a -> a.proximo != null && (a.k1 ?: 0.0) > p.k1Minimo && chaveDoK1(a) !in avisados }
                .distinctBy { it.modalidade to it.proximo }
            if (novos.isNotEmpty()) {
                add(
                    Aviso(
                        chaves = novos.map(::chaveDoK1),
                        titulo = "k1 acima de ${IndiceK.exibir(p.k1Minimo, 2)}: " + nomes(novos),
                        texto = novos.joinToString(" ") { a -> "${a.nome}: k1 ${IndiceK.exibir(a.k1!!, 3)} no concurso ${a.proximo}." },
                        destino = destino(novos),
                    ),
                )
            }
        }
        if (p.avisarPremio) {
            val novos = alvos.filter { a -> a.proximo != null && (a.premio ?: 0) > p.premioMinimo && chaveDoPremio(a) !in avisados }
                .distinctBy { it.modalidade to it.proximo }
            if (novos.isNotEmpty()) {
                add(
                    Aviso(
                        chaves = novos.map(::chaveDoPremio),
                        titulo = "Prêmio acima de ${Dinheiro.porExtenso(p.premioMinimo)}: " + nomes(novos),
                        texto = novos.joinToString(" ") { a -> "${a.nome}: prêmio estimado de ${Dinheiro.porExtenso(a.premio!!)} no concurso ${a.proximo}." },
                        destino = destino(novos),
                    ),
                )
            }
        }
    }

    /** "Mega-Sena", "Mega-Sena e Quina", "Mega-Sena, Quina e Lotofácil". */
    private fun nomes(alvos: List<Alvo>): String {
        val n = alvos.map { it.nome }.distinct()
        return if (n.size == 1) n[0] else n.dropLast(1).joinToString(", ") + " e " + n.last()
    }

    /** Um só: a página dele; mais de um: os Jogos. */
    private fun destino(alvos: List<Alvo>): String = alvos.singleOrNull()?.destino ?: "jogos"
}

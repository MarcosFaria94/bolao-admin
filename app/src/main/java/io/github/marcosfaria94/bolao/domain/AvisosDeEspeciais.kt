package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Os avisos dos concursos especiais (desde a 0.34, Ajustes > Notificações > "Concursos especiais", ligado por padrão):
 * dois por concurso especial, cada um uma vez só.
 * - **Apostas abertas**: assim que o app fica sabendo do especial (a lista dos anunciados, [Especiais.CONHECIDOS], ou
 *   o próximo concurso de uma modalidade que é especial), com o concurso, o dia e a hora do sorteio, o prêmio estimado
 *   e o k1 (a média dos últimos especiais).
 * - **Véspera**: no dia anterior ao sorteio, a partir das [HORA_DA_VESPERA] h (perdida a véspera, no próprio dia,
 *   antes do sorteio: "é hoje"). Quem recebe a véspera sem ter recebido o "apostas abertas" não recebe os dois.
 * Depois do sorteio, nada.
 */
object AvisosDeEspeciais {

    /** A partir de que hora da véspera o aviso sai. */
    const val HORA_DA_VESPERA = 9

    /** Um concurso especial que pode ser avisado. [destino] = a tela que o toque no aviso abre. */
    data class Alvo(
        val modalidade: Modalidade,
        val nome: String,
        val concurso: Int,
        val dia: LocalDate?,
        val hora: LocalTime?,
        /** Prêmio estimado, em centavos (null = não divulgado). */
        val premio: Long?,
        val k1: Double?,
        val destino: String,
    ) {
        val sorteio: LocalDateTime? get() = dia?.atTime(hora ?: LocalTime.of(20, 0))
    }

    /** Um aviso para o celular: as [chaves] que ele marca como avisadas, o título, o texto e a tela que abre. */
    data class Aviso(val chaves: List<String>, val titulo: String, val texto: String, val destino: String, val modalidade: Modalidade)

    fun chaveDasApostas(a: Alvo): String = "especial:${a.modalidade.name}:${a.concurso}:abertas"
    fun chaveDaVespera(a: Alvo): String = "especial:${a.modalidade.name}:${a.concurso}:vespera"

    private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** "Concurso 3100 · 01/01/2027 às 11h · prêmio estimado de R$ 1 Bilhão · k1 0,395" (sem o que não se sabe). */
    fun detalhe(a: Alvo, comK1: Boolean = true): String = listOfNotNull(
        "Concurso ${a.concurso}",
        a.dia?.let { d -> d.format(DATA) + (a.hora?.let { " às " + AgendaDaCaixa.hora(it) } ?: "") },
        a.premio?.takeIf { it > 0 }?.let { "prêmio estimado de " + Dinheiro.porExtenso(it) },
        a.k1?.takeIf { comK1 }?.let { "k1 " + IndiceK.exibir(it, 3) },
    ).joinToString(" · ")

    /** Os avisos que faltam dar agora ([avisados] = as chaves já avisadas). */
    fun devidos(alvos: List<Alvo>, agora: LocalDateTime, avisados: Set<String>): List<Aviso> =
        alvos.distinctBy { it.modalidade to it.concurso }.mapNotNull { a ->
            val sorteio = a.sorteio
            if (sorteio != null && !agora.isBefore(sorteio)) return@mapNotNull null
            val apostas = chaveDasApostas(a)
            val vespera = chaveDaVespera(a)
            val hoje = agora.toLocalDate()
            val ehHoje = a.dia == hoje
            val ehVespera = a.dia != null && hoje == a.dia.minusDays(1) && agora.hour >= HORA_DA_VESPERA
            when {
                (ehVespera || ehHoje) && vespera !in avisados -> Aviso(
                    chaves = listOf(apostas, vespera),
                    titulo = "${a.nome}: o sorteio é ${if (ehHoje) "hoje" else "amanhã"}" + (a.hora?.let { " às " + AgendaDaCaixa.hora(it) } ?: ""),
                    texto = detalhe(a) + ". Ainda dá para apostar.",
                    destino = a.destino,
                    modalidade = a.modalidade,
                )
                apostas !in avisados && vespera !in avisados -> Aviso(
                    chaves = listOf(apostas),
                    titulo = "${a.nome}: apostas abertas",
                    texto = detalhe(a) + ".",
                    destino = a.destino,
                    modalidade = a.modalidade,
                )
                else -> null
            }
        }
}

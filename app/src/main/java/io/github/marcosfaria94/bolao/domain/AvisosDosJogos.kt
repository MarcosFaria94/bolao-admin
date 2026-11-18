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

    /**
     * Um aviso: as [chaves] que ele marca, o [titulo], o [texto] (o aviso fechado), a tela que abre, as [linhas] (o aviso
     * aberto, uma por modalidade, quando são várias; desde a 0.41) e a [modalidade] (quando é uma só: o desenho do aviso).
     */
    data class Aviso(
        val chaves: List<String>,
        val titulo: String,
        val texto: String,
        val destino: String,
        val linhas: List<String> = emptyList(),
        val modalidade: Modalidade? = null,
    )

    fun chaveDoAcumulou(a: Alvo): String = "acumulou:${a.modalidade.name}:${a.ultimo}"
    fun chaveDoK1(a: Alvo): String = "k1:${a.modalidade.name}:${a.proximo}"
    fun chaveDoPremio(a: Alvo): String = "premio:${a.modalidade.name}:${a.proximo}"

    /**
     * Os avisos que faltam dar agora com os parâmetros [p] ([avisados] = as chaves já avisadas nesta sessão). Desde a 0.41
     * (pedido do usuário: "reformular os pushs") o título já diz o principal ("Mega-Sena acumulou", "Quina: k1 0,850",
     * "Mega-Sena: R$ 120 Milhões") e, com várias modalidades, o aviso aberto traz uma linha para cada uma.
     */
    fun devidos(p: Parametros, alvos: List<Alvo>, hoje: LocalDate, avisados: Set<String>): List<Aviso> = buildList {
        if (p.avisarAcumulado) {
            val novos = alvos.filter { a ->
                a.acumulou && a.ultimo != null && a.diaDoUltimo != null &&
                    ChronoUnit.DAYS.between(a.diaDoUltimo, hoje) in 0..DIAS_DO_ACUMULOU && chaveDoAcumulou(a) !in avisados
            }.distinctBy { it.modalidade to it.ultimo }
            if (novos.isNotEmpty()) {
                val um = novos.singleOrNull()
                add(
                    Aviso(
                        chaves = novos.map(::chaveDoAcumulou),
                        titulo = if (um != null) "${um.nome} acumulou" else "Acumularam: " + nomes(novos),
                        texto = if (um != null) {
                            "Ninguém acertou o concurso ${um.ultimo}." +
                                (um.premio?.takeIf { it > 0 }?.let { v -> " O ${um.proximo ?: "próximo"} vale ${Dinheiro.porExtenso(v)}." } ?: "")
                        } else {
                            novos.joinToString(" · ") { a -> a.nome + (a.premio?.takeIf { it > 0 }?.let { ": " + Dinheiro.porExtenso(it) } ?: "") }
                        },
                        destino = destino(novos),
                        linhas = if (um != null) emptyList() else novos.map { a ->
                            listOfNotNull(a.nome, a.proximo?.let { "concurso $it" }, a.premio?.takeIf { it > 0 }?.let { Dinheiro.porExtenso(it) }).joinToString(" · ")
                        },
                        modalidade = um?.modalidade,
                    ),
                )
            }
        }
        if (p.avisarK1) {
            val novos = alvos.filter { a -> a.proximo != null && (a.k1 ?: 0.0) > p.k1Minimo && chaveDoK1(a) !in avisados }
                .distinctBy { it.modalidade to it.proximo }
            if (novos.isNotEmpty()) {
                val um = novos.singleOrNull()
                val minimo = IndiceK.exibir(p.k1Minimo, 2)
                add(
                    Aviso(
                        chaves = novos.map(::chaveDoK1),
                        titulo = if (um != null) "${um.nome}: k1 ${IndiceK.exibir(um.k1!!, 3)}" else "k1 acima de $minimo: " + nomes(novos),
                        texto = if (um != null) {
                            "Concurso ${um.proximo}, acima do seu mínimo de $minimo."
                        } else {
                            novos.joinToString(" · ") { a -> "${a.nome} ${IndiceK.exibir(a.k1!!, 3)}" }
                        },
                        destino = destino(novos),
                        linhas = if (um != null) emptyList() else novos.map { a -> "${a.nome} · concurso ${a.proximo} · k1 ${IndiceK.exibir(a.k1!!, 3)}" },
                        modalidade = um?.modalidade,
                    ),
                )
            }
        }
        if (p.avisarPremio) {
            val novos = alvos.filter { a -> a.proximo != null && (a.premio ?: 0) > p.premioMinimo && chaveDoPremio(a) !in avisados }
                .distinctBy { it.modalidade to it.proximo }
            if (novos.isNotEmpty()) {
                val um = novos.singleOrNull()
                val minimo = Dinheiro.porExtenso(p.premioMinimo)
                add(
                    Aviso(
                        chaves = novos.map(::chaveDoPremio),
                        titulo = if (um != null) "${um.nome}: ${Dinheiro.porExtenso(um.premio!!)}" else "Prêmio acima de $minimo: " + nomes(novos),
                        texto = if (um != null) {
                            "Prêmio estimado do concurso ${um.proximo}, acima dos $minimo que você escolheu."
                        } else {
                            novos.joinToString(" · ") { a -> "${a.nome}: ${Dinheiro.porExtenso(a.premio!!)}" }
                        },
                        destino = destino(novos),
                        linhas = if (um != null) emptyList() else novos.map { a -> "${a.nome} · concurso ${a.proximo} · ${Dinheiro.porExtenso(a.premio!!)}" },
                        modalidade = um?.modalidade,
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

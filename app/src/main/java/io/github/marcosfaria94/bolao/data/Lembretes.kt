package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Especiais
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth

/** Uma notificação a mostrar. [chave] evita repetir o mesmo aviso. */
data class Lembrete(
    val chave: String,
    val canal: Canal,
    val titulo: String,
    val texto: String,
    /** Tela que abre ao tocar (rota da navegação). */
    val destino: String,
) {
    enum class Canal { LEMBRETES, RESULTADOS }
}

/** Contagens dos selos vermelhos da barra de baixo. */
data class Selos(
    /** Aba Financeiro: aporte mensal pendente e extratos pendentes. */
    val aportePendente: Boolean,
    val extratosPendentes: List<YearMonth>,
    /** Aba Apostas: apostas com resultado oficial esperando a apuração. */
    val apuracoes: List<Aposta>,
) {
    val financeiro: Int get() = (if (aportePendente) 1 else 0) + extratosPendentes.size
    val apostas: Int get() = apuracoes.size

    companion object {
        val NENHUM = Selos(false, emptyList(), emptyList())

        fun de(e: Estado, hoje: LocalDate): Selos =
            if (!e.cadastroConcluido || e.extincao != null) NENHUM
            else Selos(
                aportePendente = Operacoes.aporteMensalPendente(e, hoje),
                extratosPendentes = Operacoes.extratosPendentes(e, hoje),
                apuracoes = Operacoes.apuracoesDisponiveis(e),
            )
    }
}

/**
 * Decide quais lembretes estão na hora. Função pura: recebe o estado, o horário
 * e as chaves já avisadas; quem chama mostra as notificações e guarda as chaves.
 */
object Lembretes {
    const val HORA_APORTE = 9
    const val HORA_JOGO = 10
    const val HORA_EXTRATO = 18
    const val MINUTOS_COMPROVANTE = 60L

    fun pendentes(
        e: Estado,
        agora: LocalDateTime,
        avisados: Set<String>,
        fuso: ZoneId = ZoneId.systemDefault(),
    ): List<Lembrete> {
        if (!e.cadastroConcluido || e.extincao != null) return emptyList()
        val hoje = agora.toLocalDate()
        val mes = YearMonth.from(hoje)
        val lista = mutableListOf<Lembrete>()
        fun add(l: Lembrete) {
            if (l.chave !in avisados) lista += l
        }

        // 1. Aporte mensal (a partir do dia 1º, às 9h).
        if (agora.hour >= HORA_APORTE || hoje.dayOfMonth > 1) {
            if (Operacoes.aporteMensalPendente(e, hoje)) {
                add(
                    Lembrete(
                        "aporte-$mes", Lembrete.Canal.LEMBRETES,
                        "Dia de aporte",
                        "Envie o lembrete ao grupo (${Dinheiro.rs(e.config.aporteMensal)} por integrante) e registre o aporte mensal.",
                        "aporte",
                    ),
                )
            }
        }

        // 2. Dia de jogo.
        if (Calendario.ehDiaProgramado(hoje) && agora.hour >= HORA_JOGO && !Operacoes.apostaFeitaEm(e, hoje)) {
            add(
                Lembrete(
                    "jogo-$hoje", Lembrete.Canal.LEMBRETES,
                    "Dia de aposta",
                    "Hoje, ${Datas.diaMes(hoje)}, é dia de aposta do bolão. Saldo: ${Dinheiro.rs(Operacoes.saldo(e))}.",
                    "apostar",
                ),
            )
        }

        // 3. Comprovante da Caixa pendente.
        val limite = agora.minusMinutes(MINUTOS_COMPROVANTE)
        Operacoes.rascunhos(e).filter { it.comprovante == null }.forEach { r ->
            val criada = Instant.ofEpochMilli(r.criadaEm).atZone(fuso).toLocalDateTime()
            if (criada.isBefore(limite)) {
                add(
                    Lembrete(
                        "comprovante-${r.id}", Lembrete.Canal.LEMBRETES,
                        "Comprovante da Caixa pendente",
                        "A aposta de ${r.modalidade.nome} ainda não tem o comprovante. Sem ele publicado no grupo, a aposta não vale para o bolão.",
                        "aposta/${r.id}",
                    ),
                )
            }
        }

        // 4. Concurso especial próximo (avisa uma vez, até 10 dias antes).
        Especiais.proximos(hoje).forEach { s ->
            add(
                Lembrete(
                    "especial-${s.modalidade.name}-${s.data}", Lembrete.Canal.LEMBRETES,
                    "${s.nome} chegando",
                    "Sorteio previsto para ${Datas.diaSemana(s.data)}, ${Datas.longa(s.data)}. Pelo termo, concursos especiais usam os prêmios pequenos e as sobras.",
                    "jogos",
                ),
            )
        }

        // 5. Resultado oficial saiu: apurar e enviar o comprovante.
        Operacoes.apuracoesDisponiveis(e).forEach { a ->
            val ap = a.apuracao!!
            val premio = if (ap.premio > 0) "prêmio de ${Dinheiro.rs(ap.premio)}" else "sem prêmio"
            add(
                Lembrete(
                    "sorteio-${a.id}", Lembrete.Canal.RESULTADOS,
                    "Resultado da aposta Nº${a.numero}: ${ap.acertos}",
                    "${a.modalidade.nome} ${a.concurso}: $premio. Toque para apurar o resultado e enviar o comprovante.",
                    "apuracao/${a.id}",
                ),
            )
        }

        // 6. Extrato: no último dia do mês (18h) e depois, enquanto não for registrado.
        Operacoes.extratosPendentes(e, hoje).forEach { m ->
            if (m != mes || agora.hour >= HORA_EXTRATO) {
                add(
                    Lembrete(
                        "extrato-$m", Lembrete.Canal.LEMBRETES,
                        if (m == mes) "Último dia do mês: extrato" else "Extrato de ${Datas.mesAno(m)} pendente",
                        "Registre e envie o extrato de ${Datas.mesAno(m)}: digite o saldo da caixinha do Nubank.",
                        "extrato",
                    ),
                )
            }
        }
        return lista
    }

    /**
     * Apostas cujo resultado já deve ter saído e ainda não foram conferidas.
     * Sorteios de domingo são às 11h; nos outros dias, à noite (21h).
     */
    fun aguardandoResultado(e: Estado, agora: LocalDateTime): List<Aposta> = e.apostas.filter { a ->
        val sorteio = a.dataSorteio ?: return@filter false
        if (!a.registrada || a.apuracao != null) return@filter false
        val horaResultado = if (sorteio.dayOfWeek == DayOfWeek.SUNDAY) 12 else 22
        val saiu = sorteio.atTime(horaResultado, 0)
        !agora.isBefore(saiu)
    }
}

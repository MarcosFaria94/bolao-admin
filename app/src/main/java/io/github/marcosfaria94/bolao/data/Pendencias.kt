package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth

/**
 * Uma pendência: algo que o Administrador precisa fazer para o bolão ficar em dia.
 * Todas aparecem nas Notificações (o sino da Home), a mais recente primeiro; tocar leva à tela que resolve.
 */
data class Pendencia(
    /** Identifica a pendência (evita repetir o aviso no celular). */
    val chave: String,
    val tipo: Tipo,
    val titulo: String,
    val texto: String,
    /** Desde quando está pendente (ordena a lista). */
    val desde: LocalDateTime,
    /** A partir de quando pode ir o aviso "Ação necessária" no celular. */
    val avisarDesde: LocalDateTime,
    /** Tela que resolve a pendência (rota da navegação). */
    val destino: String,
    /** Modalidade da aposta (ou dos índices) da pendência. */
    val modalidade: Modalidade? = null,
    /** Concurso da aposta (0 = sem número). */
    val concurso: Int = 0,
    /** Tipo do comprovante a enviar (pendência de envio). */
    val comprovante: TipoComprovante? = null,
) {
    enum class Tipo { APORTE, APOSTA_DO_DIA, RASCUNHO, ENVIO, APURACAO, EXTRATO, INDICES }
}

/** Aviso de aposta premiada (o outro aviso que vai para o celular), sem o valor: ele é surpresa no app. */
data class Premiada(val chave: String, val aposta: Aposta, val titulo: String, val texto: String, val destino: String)

/**
 * Regras das pendências, da apuração automática e dos 2 avisos do celular ("Ação necessária"
 * e "Aposta premiada"). Funções puras: quem chama mostra os avisos e grava o que foi avisado.
 */
object Pendencias {
    const val HORA_APORTE = 9
    const val HORA_JOGO = 10
    const val HORA_EXTRATO = 18
    const val MINUTOS_RASCUNHO = 60L

    /** A apuração automática sai a partir das 8h do dia seguinte ao sorteio (como no termo). */
    const val HORA_APURACAO = 8

    /** Sem apuração dois dias depois do sorteio: a Caixa não ajudou, o Administrador apura à mão. */
    const val DIAS_PARA_APURAR_A_MAO = 2L

    /** Avisos de "Ação necessária" no celular só das 8h às 22h. */
    val HORARIO_DOS_AVISOS = 8 until 22

    /** Aviso de aposta premiada só para conferências recentes (dados antigos não avisam de novo). */
    const val DIAS_AVISO_PREMIO = 3L

    private fun instante(ms: Long, fuso: ZoneId): LocalDateTime = Instant.ofEpochMilli(ms).atZone(fuso).toLocalDateTime()

    private fun nome(a: Aposta): String = "Nº${a.numero} | ${a.modalidade.nome}" + if (a.concurso > 0) " · ${a.concurso}" else ""

    /** Todas as pendências agora, a mais recente primeiro. */
    fun de(e: Estado, agora: LocalDateTime, fuso: ZoneId = ZoneId.systemDefault()): List<Pendencia> {
        if (!e.cadastroConcluido || e.extincao != null) return emptyList()
        val hoje = agora.toLocalDate()
        val mes = YearMonth.from(hoje)
        val lista = mutableListOf<Pendencia>()

        // Aporte mensal (a partir do dia 1º).
        if (Operacoes.aporteMensalPendente(e, hoje)) {
            val dia1 = mes.atDay(1)
            lista += Pendencia(
                "aporte-$mes", Pendencia.Tipo.APORTE,
                "Registrar o aporte mensal",
                "Aporte de ${Datas.mesAno(mes)}: ${Dinheiro.rs(Operacoes.valorAporteMensal(e))} " +
                    "(${Dinheiro.rs(e.config.aporteMensal)} por integrante).",
                dia1.atStartOfDay(), dia1.atTime(HORA_APORTE, 0), "aporte",
            )
        }

        // Apostas em andamento (rascunhos): anexar o comprovante da Caixa ou registrar.
        val rascunhos = Operacoes.rascunhos(e)
        rascunhos.forEach { r ->
            val criada = instante(r.criadaEm, fuso)
            val jogo = r.modalidade.nome + if (r.concurso > 0) " · ${r.concurso}" else ""
            lista += if (r.comprovante == null) {
                Pendencia(
                    "rascunho-${r.id}", Pendencia.Tipo.RASCUNHO,
                    "Anexar o comprovante da Caixa",
                    "$jogo: sem o comprovante publicado no grupo, a aposta não vale para o bolão.",
                    criada, criada.plusMinutes(MINUTOS_RASCUNHO), "aposta/${r.id}",
                    modalidade = r.modalidade, concurso = r.concurso,
                )
            } else {
                Pendencia(
                    "rascunho-${r.id}", Pendencia.Tipo.RASCUNHO,
                    "Registrar a aposta",
                    "$jogo: o comprovante da Caixa já está anexado.",
                    criada, criada.plusMinutes(MINUTOS_RASCUNHO), "aposta/${r.id}",
                    modalidade = r.modalidade, concurso = r.concurso,
                )
            }
        }

        // Dia de aposta (se não houver uma aposta em andamento, que já é pendência).
        if (Calendario.ehDiaProgramado(hoje) && !Operacoes.apostaFeitaEm(e, hoje) && rascunhos.isEmpty()) {
            lista += Pendencia(
                "aposta-$hoje", Pendencia.Tipo.APOSTA_DO_DIA,
                "Fazer a aposta de hoje",
                "Hoje é dia de aposta do bolão. Saldo: ${Dinheiro.rs(Operacoes.saldo(e))}.",
                hoje.atStartOfDay(), hoje.atTime(HORA_JOGO, 0), "jogos",
            )
        }

        // Comprovantes registrados que ainda não foram enviados ao grupo.
        Operacoes.comprovantesPendentesDeEnvio(e).forEach { c ->
            val criado = instante(c.criadoEm, fuso)
            // A apuração é feita sozinha: o aviso vai na hora. Nos outros, o WhatsApp já abre no registro.
            val aviso = if (c.tipo == TipoComprovante.APURACAO) criado else criado.plusMinutes(10)
            val a = c.apostaId?.let { id -> e.apostas.firstOrNull { it.id == id } }
            lista += Pendencia(
                "envio-${c.id}", Pendencia.Tipo.ENVIO,
                "Enviar o comprovante ao grupo",
                c.titulo,
                criado, aviso, "comprovante/${c.id}",
                modalidade = a?.modalidade, concurso = a?.concurso ?: 0, comprovante = c.tipo,
            )
        }

        // Apurações que precisam do Administrador.
        e.apostas.filter { it.registrada && it.apuracao?.confirmadaEm == null && it.dataSorteio != null }.forEach { a ->
            val ap = a.apuracao
            val sorteio = a.dataSorteio!!
            if (ap != null && ap.precisaConfirmar) {
                val conferida = instante(ap.conferidaEm, fuso)
                lista += Pendencia(
                    "apuracao-${a.id}", Pendencia.Tipo.APURACAO,
                    "Confirmar a apuração",
                    "${nome(a)}: confira o resultado e o prêmio no app da Caixa.",
                    conferida, conferida, "aposta/${a.id}",
                    modalidade = a.modalidade, concurso = a.concurso,
                )
            } else if (!hoje.isBefore(sorteio.plusDays(DIAS_PARA_APURAR_A_MAO))) {
                val desde = sorteio.plusDays(DIAS_PARA_APURAR_A_MAO).atStartOfDay()
                lista += Pendencia(
                    "apuracao-${a.id}", Pendencia.Tipo.APURACAO,
                    "Apurar a aposta",
                    "${nome(a)}: a apuração automática não conseguiu o resultado da Caixa.",
                    desde, desde.withHour(HORA_APURACAO), "aposta/${a.id}",
                    modalidade = a.modalidade, concurso = a.concurso,
                )
            }
        }

        // Extrato: no último dia do mês e depois, enquanto não for registrado.
        Operacoes.extratosPendentes(e, hoje).forEach { m ->
            val fim = m.atEndOfMonth()
            lista += Pendencia(
                "extrato-$m", Pendencia.Tipo.EXTRATO,
                "Registrar o extrato de ${Datas.mesAno(m)}",
                "Digite o saldo da caixinha do Nubank e envie a prestação de contas ao grupo.",
                fim.atStartOfDay(), fim.atTime(HORA_EXTRATO, 0), "extrato/$m",
            )
        }
        return lista.sortedWith(compareByDescending<Pendencia> { it.desde }.thenBy { it.chave })
    }

    /**
     * As pendências do bolão mais os avisos dos índices (preço de Ajustes diferente do dos
     * parâmetros, regras da Caixa que mudaram), a mais recente primeiro. Os avisos dos índices
     * ficam só nas Notificações do app (não vão para o celular).
     */
    fun comAvisosDosIndices(pendencias: List<Pendencia>, avisos: List<Indices.Aviso>, fuso: ZoneId = ZoneId.systemDefault()): List<Pendencia> {
        if (avisos.isEmpty()) return pendencias
        val extras = avisos.map { a ->
            val desde = instante(a.desde, fuso)
            Pendencia(a.chave, Pendencia.Tipo.INDICES, a.titulo, a.texto, desde, desde, a.destino, modalidade = a.modalidade)
        }
        return (pendencias + extras).sortedWith(compareByDescending<Pendencia> { it.desde }.thenBy { it.chave })
    }

    /** Pendências que já podem ter o aviso "Ação necessária" (e ainda não tiveram). */
    fun paraAvisar(pendencias: List<Pendencia>, agora: LocalDateTime, avisados: Set<String>): List<Pendencia> =
        if (agora.hour !in HORARIO_DOS_AVISOS) emptyList()
        else pendencias.filter { it.chave !in avisados && !agora.isBefore(it.avisarDesde) }

    /**
     * Apostas premiadas conferidas há pouco que ainda não tiveram o aviso. O aviso não traz o
     * valor do prêmio: ele aparece ao abrir a aposta no app.
     */
    fun premiadas(e: Estado, agora: LocalDateTime, avisados: Set<String>, fuso: ZoneId = ZoneId.systemDefault()): List<Premiada> {
        if (!e.cadastroConcluido || e.extincao != null) return emptyList()
        val limite = agora.minusDays(DIAS_AVISO_PREMIO)
        return e.apostas.mapNotNull { a ->
            val ap = a.apuracao ?: return@mapNotNull null
            val premiada = ap.premio > 0 || ap.faixasPremiadas.isNotEmpty()
            val chave = "premiada-${a.id}"
            if (!premiada || chave in avisados || instante(ap.conferidaEm, fuso).isBefore(limite)) return@mapNotNull null
            Premiada(chave, a, "Aposta premiada!", "${nome(a)} · Toque para ver o prêmio.", "aposta/${a.id}")
        }
    }

    /**
     * Apostas cujo resultado já deve ter saído e que precisam de conferência: as que ainda não
     * foram conferidas e as conferidas sem o rateio oficial ou com algum dado faltando
     * (conferir de novo, até a Caixa completar). Sorteios de domingo são às 11h; nos outros dias, à noite.
     */
    fun aguardandoResultado(e: Estado, agora: LocalDateTime): List<Aposta> = e.apostas.filter { a ->
        val sorteio = a.dataSorteio ?: return@filter false
        if (!a.registrada || a.apuracao?.confirmadaEm != null) return@filter false
        val ap = a.apuracao
        if (ap != null && ap.rateioOficial.isNotEmpty() && !ap.precisaConfirmar) return@filter false
        val horaResultado = if (sorteio.dayOfWeek == DayOfWeek.SUNDAY) 12 else 22
        !agora.isBefore(sorteio.atTime(horaResultado, 0))
    }

    /**
     * Apostas prontas para a apuração automática: conferidas com o rateio oficial publicado,
     * sem nada a confirmar, a partir das 8h do dia seguinte ao sorteio.
     */
    fun prontasParaApurar(e: Estado, agora: LocalDateTime): List<Aposta> {
        if (!e.cadastroConcluido || e.extincao != null) return emptyList()
        return e.apostas.filter { a ->
            val ap = a.apuracao ?: return@filter false
            val sorteio = a.dataSorteio ?: return@filter false
            a.registrada && ap.confirmadaEm == null && !ap.precisaConfirmar && ap.rateioOficial.isNotEmpty() &&
                !agora.isBefore(sorteio.plusDays(1).atTime(HORA_APURACAO, 0))
        }
    }
}

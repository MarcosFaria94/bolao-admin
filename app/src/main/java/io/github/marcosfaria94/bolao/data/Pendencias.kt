package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth

/**
 * Uma pendência: algo que o Administrador precisa fazer para o bolão ficar em dia. Desde a 0.17
 * ficam no resumo do Painel de Jogos (as Notificações saíram), contadas por tipo. Tocar leva à tela que resolve.
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
    enum class Tipo { APORTE, APOSTA_DO_DIA, RASCUNHO, ANEXO, CONFERENCIA, ENVIO, APURACAO, EXTRATO, INDICES }

    /** Falta de comprovante (anexar ou conferir): resolve-se na tela dos Comprovantes. */
    val deComprovante: Boolean get() = tipo == Tipo.ANEXO || tipo == Tipo.CONFERENCIA
}

/**
 * Aviso de aposta premiada (o outro aviso que vai para o celular), sem o valor: ele é surpresa no app.
 * [principal]: acertou o prêmio principal (aí, se ligado em Ajustes, toca o toque do prêmio).
 */
data class Premiada(
    val chave: String,
    val aposta: Aposta,
    val titulo: String,
    val texto: String,
    val destino: String,
    val principal: Boolean = false,
)

/**
 * Regras das pendências, da apuração automática e dos 2 avisos do celular ("Ação necessária"
 * e "Aposta premiada"). Funções puras: quem chama mostra os avisos e grava o que foi avisado.
 */
object Pendencias {
    const val HORA_APORTE = 9
    const val HORA_JOGO = 10
    const val HORA_EXTRATO = 18
    const val MINUTOS_RASCUNHO = 60L

    /** Depois de registrar uma aposta (ou um rateio), quanto esperar o comprovante antes de avisar no celular. */
    const val MINUTOS_ANEXO = 60L

    /** Tela dos Comprovantes (o botão da barra de cima): recebe os da Caixa e dos Pix e faz a conferência. */
    const val ABA_COMPROVANTES = "comprovantes"

    /** Hora do aviso "Apurar a aposta" quando a apuração automática não conseguiu o resultado. */
    const val HORA_APURACAO = 8

    /** Hora em que o resultado costuma estar na Caixa (os sorteios são às 20h; aos domingos, às 11h). */
    const val HORA_RESULTADO = 21
    const val HORA_RESULTADO_DOMINGO = 12

    /** Enquanto o resultado (ou o rateio oficial) não sai, confere de novo a cada meia hora. */
    const val MINUTOS_NOVA_CONFERENCIA = 30L

    /**
     * Sem o resultado da Caixa no dia seguinte ao sorteio (D+1): aí, e só aí, é um erro — a pendência
     * pede para apurar à mão e a página da aposta mostra "Tentar de novo" e "Informar à mão". Até a
     * 0.16 eram dois dias.
     */
    const val DIAS_PARA_APURAR_A_MAO = 1L

    /** Até quando (dias depois do sorteio) o app ainda acorda sozinho para conferir o resultado. */
    const val DIAS_DE_CONFERENCIA_AUTOMATICA = 3L

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

        // Aporte mensal (a partir do dia 1º). O modo individual não tem aporte.
        if (!e.individual && Operacoes.aporteMensalPendente(e, hoje)) {
            val dia1 = mes.atDay(1)
            lista += Pendencia(
                "aporte-$mes", Pendencia.Tipo.APORTE,
                "Registrar o aporte mensal",
                "Aporte de ${Datas.mesAno(mes)}: ${Dinheiro.rs(Operacoes.valorAporteMensal(e))} " +
                    "(${Dinheiro.rs(e.config.aporteMensal)} por integrante).",
                dia1.atStartOfDay(), dia1.atTime(HORA_APORTE, 0), "aporte",
            )
        }

        // Apostas em andamento (volante completo): falta registrar.
        val rascunhos = Operacoes.rascunhos(e)
        rascunhos.forEach { r ->
            val criada = instante(r.criadaEm, fuso)
            val jogo = r.modalidade.nome + if (r.concurso > 0) " · ${r.concurso}" else ""
            lista += Pendencia(
                "rascunho-${r.id}", Pendencia.Tipo.RASCUNHO,
                "Registrar a aposta",
                "$jogo: o volante está pronto, falta registrar.",
                criada, criada.plusMinutes(MINUTOS_RASCUNHO), "aposta/${r.id}",
                modalidade = r.modalidade, concurso = r.concurso,
            )
        }

        // Apostas registradas esperando o comprovante da Caixa (aba Comprovantes).
        Operacoes.apostasSemComprovante(e).forEach { a ->
            val registro = e.lancamentos.firstOrNull { it.apostaId == a.id && it.tipo == TipoLancamento.APOSTA }?.criadoEm ?: a.criadaEm
            val desde = instante(registro, fuso)
            lista += Pendencia(
                "anexo-${a.id}", Pendencia.Tipo.ANEXO,
                "Anexar o comprovante da Caixa",
                "Aposta ${nome(a)}: sem o comprovante da Caixa conferido, a aposta não vale para o bolão.",
                desde, desde.plusMinutes(MINUTOS_ANEXO), ABA_COMPROVANTES,
                modalidade = a.modalidade, concurso = a.concurso,
            )
        }
        // Rateios esperando os comprovantes dos Pix.
        Operacoes.rateiosSemPix(e).forEach { r ->
            val desde = instante(r.criadoEm, fuso)
            val faltam = e.pessoas.count { !r.pix.containsKey(it.id) }
            lista += Pendencia(
                "pix-${r.numero}", Pendencia.Tipo.ANEXO,
                "Anexar os Pix do rateio Nº${r.numeroNoMes}",
                if (faltam == 1) "Falta 1 comprovante de Pix (${Dinheiro.rs(r.cota)} para cada um)."
                else "Faltam $faltam comprovantes de Pix (${Dinheiro.rs(r.cota)} para cada um).",
                desde, desde.plusMinutes(MINUTOS_ANEXO), ABA_COMPROVANTES,
            )
        }
        // Comprovantes anexados esperando a conferência (uma pendência só, para todos).
        val paraConferir = Operacoes.apostasParaConferir(e).mapNotNull { it.comprovante?.guardadoEm } +
            Operacoes.rateiosParaConferir(e).flatMap { r -> r.pix.values.map { it.guardadoEm } }
        if (paraConferir.isNotEmpty()) {
            val n = Operacoes.apostasParaConferir(e).size + Operacoes.rateiosParaConferir(e).size
            val desde = instante(paraConferir.max(), fuso)
            lista += Pendencia(
                "conferir-${paraConferir.min()}", Pendencia.Tipo.CONFERENCIA,
                "Conferir os comprovantes",
                if (n == 1) "1 registro com os comprovantes anexados espera a sua aprovação."
                else "$n registros com os comprovantes anexados esperam a sua aprovação.",
                desde, desde.plusMinutes(MINUTOS_ANEXO), ABA_COMPROVANTES,
            )
        }

        // Dia de aposta (se não houver uma aposta em andamento, que já é pendência). Não no modo individual.
        if (!e.individual && Calendario.ehDiaProgramado(hoje) && !Operacoes.apostaFeitaEm(e, hoje) && rascunhos.isEmpty()) {
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
                "Enviar ao grupo",
                c.titulo,
                criado, aviso, "comprovante/${c.id}",
                modalidade = a?.modalidade, concurso = a?.concurso ?: 0, comprovante = c.tipo,
            )
        }

        // Apurações que precisam do Administrador (as apostas esperando o comprovante ficam de fora).
        e.apostas.filter { it.registrada && !it.pendenteDeAnexo && it.apuracao?.confirmadaEm == null && it.dataSorteio != null }.forEach { a ->
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
            } else if (!conferenciaCompleta(a) && !hoje.isBefore(sorteio.plusDays(DIAS_PARA_APURAR_A_MAO))) {
                // Com o resultado e o rateio oficial já conferidos, a apuração automática cuida (não é pendência).
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

        // Extrato: no último dia do mês e depois, enquanto não for registrado. Não no modo individual.
        if (!e.individual) Operacoes.extratosPendentes(e, hoje).forEach { m ->
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
     * ficam só no app, no resumo do Painel de Jogos (não vão para o celular).
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
            Premiada(chave, a, "Aposta premiada!", "${nome(a)} · Toque para ver o prêmio.", "aposta/${a.id}", principal = premioPrincipal(a))
        }
    }

    /**
     * A aposta acertou o prêmio principal: a 1ª faixa (sena, quina, 15 acertos, 20 acertos, 7 colunas,
     * 14 jogos, 6 números + 2 trevos, 1º prêmio da Federal...). Na Dupla Sena vale a sena dos dois sorteios.
     */
    fun premioPrincipal(a: Aposta): Boolean {
        val faixas = a.apuracao?.faixasPremiadas.orEmpty()
        return 1 in faixas || (a.modalidade == Modalidade.DUPLA_SENA && 5 in faixas)
    }

    /**
     * Apostas cujo resultado já deve ter saído e que precisam de conferência: as que ainda não
     * foram conferidas e as conferidas sem o rateio oficial ou com algum dado faltando
     * (conferir de novo, até a Caixa completar). Sorteios de domingo são às 11h; nos outros dias, à noite.
     */
    fun aguardandoResultado(e: Estado, agora: LocalDateTime): List<Aposta> = e.apostas.filter { a ->
        val sorteio = a.dataSorteio ?: return@filter false
        if (!a.registrada || a.pendenteDeAnexo || a.apuracao?.confirmadaEm != null) return@filter false
        if (conferenciaCompleta(a)) return@filter false
        !agora.isBefore(horaDoResultado(sorteio))
    }

    /** Quando o resultado do sorteio do dia [sorteio] costuma estar na Caixa. */
    fun horaDoResultado(sorteio: LocalDate): LocalDateTime =
        sorteio.atTime(if (sorteio.dayOfWeek == DayOfWeek.SUNDAY) HORA_RESULTADO_DOMINGO else HORA_RESULTADO, 0)

    /** Conferida com o rateio oficial e sem nada a confirmar: a apuração automática pode sair. */
    private fun conferenciaCompleta(a: Aposta): Boolean {
        val ap = a.apuracao ?: return false
        return ap.rateioOficial.isNotEmpty() && !ap.precisaConfirmar
    }

    /**
     * A partir de quando a apuração automática da aposta [a] pode sair: no dia do sorteio, na hora
     * em que a conferência fica completa (o resultado e o rateio oficial publicados pela Caixa).
     * Desde a 0.12 vale também para o bolão (antes, às 8h do dia seguinte).
     */
    @Suppress("UNUSED_PARAMETER")
    fun apuracaoAutomaticaEm(e: Estado, a: Aposta): LocalDateTime? = a.dataSorteio?.atStartOfDay()

    /**
     * Apostas prontas para a apuração automática: conferidas com o rateio oficial publicado e sem
     * nada a confirmar (no mesmo dia do sorteio, assim que a Caixa publica).
     */
    fun prontasParaApurar(e: Estado, agora: LocalDateTime): List<Aposta> {
        if (!e.cadastroConcluido || e.extincao != null) return emptyList()
        return e.apostas.filter { a ->
            val quando = apuracaoAutomaticaEm(e, a) ?: return@filter false
            a.registrada && !a.pendenteDeAnexo && a.apuracao?.confirmadaEm == null && conferenciaCompleta(a) && !agora.isBefore(quando)
        }
    }

    /**
     * Quando o app precisa acordar (mesmo fechado) para conferir e apurar sozinho: na hora do
     * resultado de cada aposta que espera o sorteio e a cada meia hora enquanto o resultado (ou o
     * rateio oficial) não sai. null = nada a esperar.
     */
    fun proximoDespertar(e: Estado, agora: LocalDateTime): LocalDateTime? {
        if (!e.cadastroConcluido || e.extincao != null) return null
        val vezes = e.apostas.mapNotNull { a ->
            val sorteio = a.dataSorteio ?: return@mapNotNull null
            if (!a.registrada || a.pendenteDeAnexo || a.apuracao?.confirmadaEm != null) return@mapNotNull null
            val resultado = horaDoResultado(sorteio)
            // Alguns dias depois do sorteio sem apuração, quem resolve é o Administrador (a pendência pede desde D+1).
            if (!agora.isBefore(sorteio.plusDays(DIAS_DE_CONFERENCIA_AUTOMATICA).atStartOfDay())) return@mapNotNull null
            when {
                agora.isBefore(resultado) -> resultado
                !conferenciaCompleta(a) -> agora.plusMinutes(MINUTOS_NOVA_CONFERENCIA)
                else -> apuracaoAutomaticaEm(e, a)?.let { if (agora.isBefore(it)) it else agora.plusMinutes(MINUTOS_NOVA_CONFERENCIA) }
            }
        }
        return vezes.minOrNull()
    }
}

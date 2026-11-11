package io.github.marcosfaria94.bolao.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Quando consultar a Caixa (desde a 0.20). Os dados de cada jogo (o último resultado, o próximo
 * concurso, a data e o prêmio estimado) só mudam no sorteio: o app consulta a Caixa depois de cada
 * sorteio e segura os dados (guardados no celular) até o sorteio seguinte. Assim a lista não volta para
 * dados velhos ao reabrir o app e a Caixa recebe poucos pedidos.
 *
 * Desde a 0.25 (pedido do usuário) a primeira tentativa é 5 minutos depois do sorteio e, enquanto o
 * resultado completo (com o rateio) não chega, de novo a cada 5 minutos; chegou, para na hora (até o
 * sorteio seguinte). Até a 0.24 esperava 1 hora e tentava a cada 15 minutos.
 *
 * Horário dos sorteios (de Brasília): concursos regulares às 21h; a Federal às 20h; aos domingos, às
 * 11h. Os concursos especiais podem ser em outro horário: enquanto o resultado não chega, o app tenta
 * de novo a cada [INTERVALO_DAS_TENTATIVAS] (depois de [ESPERA_LONGA], a cada [INTERVALO_LONGO]).
 */
object AgendaDaCaixa {

    /** Os horários da Caixa são os de Brasília, qualquer que seja o fuso do celular. */
    val FUSO: ZoneId = ZoneId.of("America/Sao_Paulo")

    val HORA_REGULAR: LocalTime = LocalTime.of(21, 0)
    val HORA_FEDERAL: LocalTime = LocalTime.of(20, 0)
    val HORA_DOMINGO: LocalTime = LocalTime.of(11, 0)

    /** Espera depois do sorteio até a primeira consulta (desde a 0.25: 5 minutos; até a 0.24, 1 hora). */
    val ESPERA_DEPOIS_DO_SORTEIO: Duration = Duration.ofMinutes(5)

    /** Esperando o resultado (ou depois de uma falha): nova tentativa a cada 5 minutos (desde a 0.25; antes, 15)... */
    val INTERVALO_DAS_TENTATIVAS: Duration = Duration.ofMinutes(5)

    /**
     * ...até este tempo esperando (ex.: a Loteca, que depende dos jogos, ou a Caixa fora do ar). Depois, as
     * tentativas ficam mais espaçadas, para não gastar a bateria a noite inteira...
     */
    val ESPERA_LONGA: Duration = Duration.ofHours(3)

    /** ...de meia em meia hora. */
    val INTERVALO_LONGO: Duration = Duration.ofMinutes(30)

    /** Sem a data do próximo sorteio (a Caixa não divulgou): pergunta de novo a cada... */
    val INTERVALO_SEM_DATA: Duration = Duration.ofHours(2)

    /** Hora do sorteio da modalidade [m] no dia [dia]. */
    fun horaDoSorteio(m: Modalidade, dia: LocalDate): LocalTime = when {
        dia.dayOfWeek == DayOfWeek.SUNDAY -> HORA_DOMINGO
        m == Modalidade.FEDERAL -> HORA_FEDERAL
        else -> HORA_REGULAR
    }

    /** O instante do sorteio da modalidade [m] no dia [dia] (hora de Brasília). */
    fun momentoDoSorteio(m: Modalidade, dia: LocalDate): Instant = dia.atTime(horaDoSorteio(m, dia)).atZone(FUSO).toInstant()

    /** A partir de quando o app procura o resultado do sorteio do dia [dia] na Caixa (o sorteio + 5 minutos). */
    fun resultadoSai(m: Modalidade, dia: LocalDate): Instant = momentoDoSorteio(m, dia).plus(ESPERA_DEPOIS_DO_SORTEIO)

    enum class Estado {
        /** Nada guardado ainda: consulta já. */
        SEM_DADOS,

        /** Com o último resultado completo: os dados valem até o próximo sorteio (certeza). */
        EM_DIA,

        /** O sorteio do próximo concurso já começou; a consulta é 5 minutos depois. */
        SORTEANDO,

        /** Passaram 5 minutos do sorteio e o resultado ainda não chegou ao app. */
        AGUARDANDO,

        /** O último resultado chegou sem o rateio (a Caixa ainda está apurando). */
        SEM_RATEIO,

        /** Com o último resultado, mas sem a data do próximo sorteio (não divulgada). */
        SEM_DATA,
    }

    data class Situacao(
        val estado: Estado,
        /** O próximo sorteio (null = data não divulgada). */
        val sorteio: Instant?,
        /** A partir de quando consultar a Caixa (null = já). */
        val consultarEm: Instant?,
    ) {
        /** Os dados valem com certeza (até o próximo sorteio). */
        val certa: Boolean get() = estado == Estado.EM_DIA

        /** O concurso mostrado como "próximo" já foi (ou está sendo) sorteado. */
        val sorteado: Boolean get() = estado == Estado.SORTEANDO || estado == Estado.AGUARDANDO

        fun precisaConsultar(agora: Instant): Boolean = consultarEm == null || !agora.isBefore(consultarEm)
    }

    /**
     * O resultado chegou sem o rateio oficial: faixa com ganhadores e sem o valor, ou nenhuma faixa com
     * ganhador (sempre há ganhadores nas faixas de baixo, menos na Loteca).
     */
    fun semRateio(c: Concurso): Boolean {
        if (!c.temResultado) return false
        if (c.faixas.any { it.ganhadores > 0 && it.valor <= 0 }) return true
        if (c.modalidade == Modalidade.LOTECA) return false
        return c.faixas.isEmpty() || c.faixas.none { it.ganhadores > 0 }
    }

    /**
     * A situação dos dados da modalidade [m]: [ultimo] = o último resultado guardado; [tentativa] = a
     * última consulta à Caixa (deu certo ou não).
     */
    fun situacao(m: Modalidade, ultimo: Concurso?, agora: Instant, tentativa: Instant?): Situacao {
        fun depoisDe(intervalo: Duration): Instant? = tentativa?.plus(intervalo)?.takeIf { it.isAfter(agora) }
        /** O intervalo entre as tentativas, esperando desde [desde]. */
        fun intervalo(desde: Instant?): Duration =
            if (desde != null && Duration.between(desde, agora) > ESPERA_LONGA) INTERVALO_LONGO else INTERVALO_DAS_TENTATIVAS
        val u = ultimo ?: return Situacao(Estado.SEM_DADOS, null, depoisDe(INTERVALO_DAS_TENTATIVAS))
        val sorteio = u.proximaData?.let { momentoDoSorteio(m, it) }
        if (semRateio(u)) return Situacao(Estado.SEM_RATEIO, sorteio, depoisDe(intervalo(u.data?.let { resultadoSai(m, it) })))
        if (sorteio == null) return Situacao(Estado.SEM_DATA, null, depoisDe(INTERVALO_SEM_DATA))
        val liberado = sorteio.plus(ESPERA_DEPOIS_DO_SORTEIO)
        return when {
            agora.isBefore(sorteio) -> Situacao(Estado.EM_DIA, sorteio, liberado)
            agora.isBefore(liberado) -> Situacao(Estado.SORTEANDO, sorteio, liberado)
            else -> {
                // Já tentou depois da liberação e o resultado não veio: espera o intervalo.
                val proxima = tentativa?.takeIf { !it.isBefore(liberado) }?.plus(intervalo(liberado))
                Situacao(Estado.AGUARDANDO, sorteio, proxima?.takeIf { it.isAfter(agora) })
            }
        }
    }

    /**
     * A próxima consulta depois de um sorteio, entre os jogos de [situacoes] (desde a 0.25: o despertador
     * acorda o app para ela, mesmo fechado). Contam os jogos em dia (5 minutos depois do próximo sorteio),
     * sorteando, esperando o resultado e sem o rateio; os sem a data do próximo sorteio e os sem dados
     * esperam o app abrir (ou o despertar de outro jogo). null = nenhum. Já devida = [agora].
     */
    fun proximaConsulta(situacoes: Collection<Situacao>, agora: Instant): Instant? = situacoes
        .filter { it.estado == Estado.EM_DIA || it.estado == Estado.SORTEANDO || it.estado == Estado.AGUARDANDO || it.estado == Estado.SEM_RATEIO }
        .map { s -> s.consultarEm?.takeIf { it.isAfter(agora) } ?: agora }
        .minOrNull()

    /**
     * Fica o concurso [novo] (que a Caixa acabou de mandar) no lugar do [guardado]? Nunca volta para um
     * concurso mais velho (servidores da Caixa às vezes respondem com dados atrasados); no mesmo
     * concurso, não troca um resultado completo por um sem rateio nem perde a data do próximo sorteio.
     */
    fun aceitar(guardado: Concurso?, novo: Concurso): Boolean = when {
        guardado == null -> true
        novo.numero != guardado.numero -> novo.numero > guardado.numero
        semRateio(novo) && !semRateio(guardado) -> false
        else -> novo.proximaData != null || guardado.proximaData == null
    }

    /** "21h" ou "20h30". */
    fun hora(t: LocalTime): String = if (t.minute == 0) "${t.hour}h" else "${t.hour}h${t.minute.toString().padStart(2, '0')}"

    /** No meio da frase: "hoje às 21h", "amanhã às 11h", "em 3 dias às 21h" (até 7 dias) ou "em 22/11 às 21h". */
    fun quando(instante: Instant, agora: Instant): String {
        val t = instante.atZone(FUSO)
        val dias = ChronoUnit.DAYS.between(agora.atZone(FUSO).toLocalDate(), t.toLocalDate())
        val dia = when (dias) {
            0L -> "hoje"
            1L -> "amanhã"
            -1L -> "ontem"
            in 2L..7L -> "em $dias dias"
            else -> "em ${Datas.diaMes(t.toLocalDate())}"
        }
        return "$dia às ${hora(t.toLocalTime())}"
    }
}

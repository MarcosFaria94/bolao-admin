package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.MESES_DE_SORTE
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.domain.Volante
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Dados de exemplo do modo de teste, para experimentar todas as telas sem digitar nada. Desde a
 * 0.10 quase tudo é sorteado a cada exemplo novo: a quantidade de integrantes (de 2 a 5, quando
 * não há cadastro real), o saldo inicial, o aporte, a modalidade de cada dia, quais apostas
 * ganham (e quanto), o rateio, um aporte adicional, os horários e os valores do rateio oficial.
 *
 * - mês anterior completo: saldo inicial, aporte mensal, as apostas dos dias programados apuradas
 *   (uma ou duas premiadas), às vezes um aporte adicional, o rateio e o extrato do mês, cada um
 *   com o seu comprovante enviado;
 * - mês atual até ontem: aporte do dia 1º e as apostas dos dias programados, todas apuradas menos
 *   a última, que fica esperando a apuração. A aposta de hoje fica para quem testa;
 * - às vezes, uma aposta registrada ontem esperando o comprovante da Caixa e um rateio esperando os
 *   Pix, para experimentar a aba Comprovantes.
 *
 * No modo individual: só as apostas e os prêmios (sem aporte, rateio, extrato nem comprovante).
 *
 * Quando [gerar] recebe resultados reais da Caixa, a última aposta usa um concurso de verdade
 * (a apuração e a conferência automática consultam a Caixa como no uso normal).
 *
 * Função pura: os anexos e as imagens dos comprovantes são só referências ([Arquivo]);
 * quem chama desenha as imagens.
 */
object DadosDeExemplo {

    /** Começo do nome de todos os anexos fictícios. */
    const val PREFIXO = "exemplo-"
    const val RENDIMENTO_MAXIMO = 9L

    /** Integrantes fictícios, usados enquanto não houver cadastro real (os primeiros N entram no exemplo). */
    val NOMES = listOf(
        Pessoa("exemplo-1", "Fulano de Tal", "Fulano", "fulano@exemplo.com", administrador = true),
        Pessoa("exemplo-2", "Beto da Silva", "Beto", "beto@exemplo.com"),
        Pessoa("exemplo-3", "Beltrano Souza", "Beltrano", "beltrano@exemplo.com"),
        Pessoa("exemplo-4", "Ciclano Pereira", "Ciclano", "ciclano@exemplo.com"),
        Pessoa("exemplo-5", "Sicrano Lima", "Sicrano", "sicrano@exemplo.com"),
    )

    /** Os 3 primeiros (o grupo do termo). */
    val PESSOAS = NOMES.take(3)

    /** Modalidades sorteadas para os dias programados (as mais repetidas saem mais). */
    private val SORTEIO_DE_MODALIDADES = listOf(
        Modalidade.LOTOFACIL, Modalidade.LOTOFACIL, Modalidade.QUINA, Modalidade.QUINA, Modalidade.DIA_DE_SORTE,
        Modalidade.LOTOMANIA, Modalidade.SUPER_SETE, Modalidade.DUPLA_SENA, Modalidade.TIMEMANIA, Modalidade.MEGA_SENA,
    )

    /** Modalidades em que o exemplo sabe montar um jogo premiado (nas faixas de baixo). */
    private val PODEM_GANHAR = setOf(
        Modalidade.LOTOFACIL, Modalidade.QUINA, Modalidade.DIA_DE_SORTE, Modalidade.LOTOMANIA, Modalidade.SUPER_SETE, Modalidade.TIMEMANIA,
    )

    /** Para o último jogo: modalidades com sorteio quase todo dia, então quase sempre há resultado recente. */
    private val PREFERIDAS_PARA_APURAR = listOf(Modalidade.LOTOFACIL, Modalidade.QUINA)

    /** Modalidades que vale consultar na Caixa antes de gerar. */
    val MODALIDADES: List<Modalidade> = (PREFERIDAS_PARA_APURAR + SORTEIO_DE_MODALIDADES).distinct()

    /** Sorteios por semana (aproximado), para estimar os números de concurso. */
    private val POR_SEMANA = mapOf(
        Modalidade.MEGA_SENA to 3, Modalidade.LOTOFACIL to 6, Modalidade.QUINA to 6, Modalidade.LOTOMANIA to 3,
        Modalidade.TIMEMANIA to 3, Modalidade.DUPLA_SENA to 3, Modalidade.DIA_DE_SORTE to 3, Modalidade.SUPER_SETE to 3,
        Modalidade.MAIS_MILIONARIA to 2, Modalidade.LOTECA to 1, Modalidade.FEDERAL to 2,
    )

    /** Números fictícios de referência (sem a Caixa): concurso por volta de [DATA_REFERENCIA]. */
    private val REFERENCIA = mapOf(
        Modalidade.MEGA_SENA to 2925, Modalidade.LOTOFACIL to 3505, Modalidade.QUINA to 6845, Modalidade.LOTOMANIA to 2815,
        Modalidade.TIMEMANIA to 2285, Modalidade.DUPLA_SENA to 2855, Modalidade.DIA_DE_SORTE to 1125, Modalidade.SUPER_SETE to 745,
        Modalidade.MAIS_MILIONARIA to 290, Modalidade.LOTECA to 1215, Modalidade.FEDERAL to 6015,
    )
    private val DATA_REFERENCIA: LocalDate = LocalDate.of(2026, 10, 1)

    /** Prêmio estimado que aparece na identificação do jogo (ex.: "1,7M"). */
    private val PREMIO_ESTIMADO = mapOf(
        Modalidade.MEGA_SENA to 3_500_000_000L, Modalidade.LOTOFACIL to 170_000_000L, Modalidade.QUINA to 70_000_000L,
        Modalidade.LOTOMANIA to 250_000_000L, Modalidade.TIMEMANIA to 420_000_000L, Modalidade.DUPLA_SENA to 190_000_000L,
        Modalidade.DIA_DE_SORTE to 40_000_000L, Modalidade.SUPER_SETE to 280_000_000L,
    )

    /** Rateio oficial fictício de cada modalidade do exemplo (faixa, ganhadores, prêmio em centavos). */
    private val FAIXAS: Map<Modalidade, List<FaixaPremio>> = mapOf(
        Modalidade.MEGA_SENA to listOf(
            FaixaPremio(1, "6 acertos", 0, 0),
            FaixaPremio(2, "5 acertos", 98, 2_543_120),
            FaixaPremio(3, "4 acertos", 6_480, 62_870),
        ),
        Modalidade.LOTOFACIL to listOf(
            FaixaPremio(1, "15 acertos", 1, 150_000_000),
            FaixaPremio(2, "14 acertos", 230, 160_000),
            FaixaPremio(3, "13 acertos", 7_100, 3_500),
            FaixaPremio(4, "12 acertos", 90_000, 1_400),
            FaixaPremio(5, "11 acertos", 450_000, 700),
        ),
        Modalidade.QUINA to listOf(
            FaixaPremio(1, "5 acertos", 0, 0),
            FaixaPremio(2, "4 acertos", 58, 1_052_340),
            FaixaPremio(3, "3 acertos", 5_310, 11_215),
            FaixaPremio(4, "2 acertos", 139_870, 425),
        ),
        Modalidade.DIA_DE_SORTE to listOf(
            FaixaPremio(1, "7 acertos", 0, 0),
            FaixaPremio(2, "6 acertos", 21, 359_812),
            FaixaPremio(3, "5 acertos", 984, 2_500),
            FaixaPremio(4, "4 acertos", 11_870, 500),
            FaixaPremio(5, "Mês de Sorte", 35_120, 250),
        ),
        Modalidade.LOTOMANIA to listOf(
            FaixaPremio(1, "20 acertos", 0, 0),
            FaixaPremio(2, "19 acertos", 6, 4_312_377),
            FaixaPremio(3, "18 acertos", 71, 228_042),
            FaixaPremio(4, "17 acertos", 640, 15_810),
            FaixaPremio(5, "16 acertos", 3_920, 2_581),
            FaixaPremio(6, "15 acertos", 16_840, 600),
            FaixaPremio(7, "0 acertos", 0, 0),
        ),
        Modalidade.SUPER_SETE to listOf(
            FaixaPremio(1, "7 acertos", 0, 0),
            FaixaPremio(2, "6 acertos", 2, 3_125_400),
            FaixaPremio(3, "5 acertos", 61, 102_540),
            FaixaPremio(4, "4 acertos", 1_002, 6_242),
            FaixaPremio(5, "3 acertos", 8_840, 600),
        ),
        Modalidade.DUPLA_SENA to listOf(
            FaixaPremio(1, "1º sorteio - 6 acertos", 0, 0),
            FaixaPremio(2, "1º sorteio - 5 acertos", 11, 1_998_210),
            FaixaPremio(3, "1º sorteio - 4 acertos", 690, 15_874),
            FaixaPremio(4, "1º sorteio - 3 acertos", 13_220, 300),
            FaixaPremio(5, "2º sorteio - 6 acertos", 0, 0),
            FaixaPremio(6, "2º sorteio - 5 acertos", 15, 1_310_450),
            FaixaPremio(7, "2º sorteio - 4 acertos", 702, 15_102),
            FaixaPremio(8, "2º sorteio - 3 acertos", 12_904, 300),
        ),
        Modalidade.TIMEMANIA to listOf(
            FaixaPremio(1, "7 acertos", 0, 0),
            FaixaPremio(2, "6 acertos", 3, 2_801_220),
            FaixaPremio(3, "5 acertos", 140, 194_330),
            FaixaPremio(4, "4 acertos", 2_640, 1_050),
            FaixaPremio(5, "3 acertos", 25_310, 350),
            FaixaPremio(6, "Time do Coração", 9_120, 850),
        ),
    )

    /** Prêmios fixos (as regras da Caixa): não variam de um exemplo para outro. */
    private val FIXOS = setOf(
        Modalidade.LOTOFACIL to 3, Modalidade.LOTOFACIL to 4, Modalidade.LOTOFACIL to 5,
        Modalidade.DIA_DE_SORTE to 3, Modalidade.DIA_DE_SORTE to 4, Modalidade.DIA_DE_SORTE to 5,
        Modalidade.TIMEMANIA to 4, Modalidade.TIMEMANIA to 5, Modalidade.TIMEMANIA to 6, Modalidade.SUPER_SETE to 5,
    )

    /** O rateio oficial de um concurso do exemplo: os ganhadores e os prêmios de rateio variam um pouco. */
    private fun faixasDo(m: Modalidade, r: Random): List<FaixaPremio> = FAIXAS[m].orEmpty().map { f ->
        if (f.ganhadores == 0 || (m to f.faixa) in FIXOS) {
            f
        } else {
            val fator = 0.75 + r.nextDouble() * 0.5
            f.copy(ganhadores = (f.ganhadores * (0.7 + r.nextDouble() * 0.6)).toInt().coerceAtLeast(1), valor = (f.valor * fator).toLong())
        }
    }

    private val TIMES_APOSTADOS = listOf("SANTOS/SP", "CRUZEIRO/MG", "BAHIA/BA", "GREMIO/RS", "FORTALEZA/CE")
    private const val TIME_SORTEADO = "CORITIBA/PR"

    private data class Planejada(
        val dia: LocalDate,
        val modalidade: Modalidade,
        /** Acertos de um jogo premiado; null = jogo sem prêmio. */
        val acertosPremiados: Int? = null,
        /** Hora e minuto do registro. */
        val hora: Int = 10,
        val minuto: Int = 0,
    )

    private sealed class Evento(val instante: Long) {
        class Aportes(instante: Long, val dia: LocalDate) : Evento(instante)
        class Adicional(instante: Long, val dia: LocalDate, val valor: Long) : Evento(instante)
        class Apostar(instante: Long, val indice: Int) : Evento(instante)
        class Apurar(instante: Long, val indice: Int) : Evento(instante)
        class Ratear(instante: Long, val dia: LocalDate, val fracao: Double) : Evento(instante)
        class FecharMes(instante: Long, val mes: YearMonth, val rendimento: Long) : Evento(instante)
    }

    /** Acertos de um jogo premiado (as faixas de baixo, que quase sempre têm ganhador). */
    private fun acertosComPremio(m: Modalidade, r: Random): Int = when (m) {
        Modalidade.LOTOFACIL -> r.nextInt(11, 14)
        Modalidade.QUINA -> r.nextInt(2, 4)
        Modalidade.DIA_DE_SORTE -> r.nextInt(4, 6)
        Modalidade.LOTOMANIA -> r.nextInt(15, 17)
        Modalidade.SUPER_SETE -> r.nextInt(3, 5)
        Modalidade.TIMEMANIA -> r.nextInt(3, 5)
        else -> 0
    }

    /** Preço da aposta simples (o mesmo de Ajustes no exemplo). */
    private fun preco(m: Modalidade): Long = Config().preco(m)

    /**
     * Gera o estado de exemplo para o dia [hoje] ([agora] em milissegundos, no fuso [fuso]).
     * [reais]: últimos concursos consultados na Caixa (pode ser vazio).
     * [pessoas]: integrantes do cadastro real, se já existir (as mensagens saem com os nomes certos).
     * [modo]: o do cadastro real (o exemplo do modo individual não tem aporte, rateio nem comprovante).
     */
    fun gerar(
        hoje: LocalDate,
        agora: Long,
        fuso: ZoneId = ZoneId.systemDefault(),
        reais: Map<Modalidade, Concurso> = emptyMap(),
        pessoas: List<Pessoa>? = null,
        semente: Long = hoje.toEpochDay(),
        modo: Modo = Modo.BOLAO,
    ): Estado {
        if (modo == Modo.INDIVIDUAL) return gerarIndividual(hoje, agora, fuso, reais, pessoas, semente)
        val r = Random(semente)
        val integrantes = pessoas?.takeIf { it.size >= MINIMO_DE_INTEGRANTES } ?: NOMES.take(r.nextInt(2, 6))
        val n = integrantes.size
        val ultimoInstante = agora - 60_000
        fun t(d: LocalDate, hora: Int, minuto: Int = 0): Long =
            minOf(d.atTime(hora, minuto).atZone(fuso).toInstant().toEpochMilli(), ultimoInstante)

        val mes = YearMonth.from(hoje)
        val anterior = mes.minusMonths(1)
        val diasAnterior = Calendario.diasProgramados(anterior)
        val diasAtual = Calendario.diasProgramados(mes).filter { it.isBefore(hoje) }
        // Uma ou duas premiadas no mês anterior; às vezes uma no mês atual (nunca a última, que espera a apuração).
        val modalidadesAnterior = diasAnterior.map { SORTEIO_DE_MODALIDADES.random(r) }
        val modalidadesAtual = diasAtual.map { SORTEIO_DE_MODALIDADES.random(r) }
        val premiadasAnterior = modalidadesAnterior.indices.filter { modalidadesAnterior[it] in PODEM_GANHAR }.shuffled(r).take(r.nextInt(1, 3)).toSet()
        val premiadaAtual = modalidadesAtual.indices.toList().dropLast(1).filter { modalidadesAtual[it] in PODEM_GANHAR }
            .shuffled(r).take(if (r.nextInt(100) < 45) 1 else 0).toSet()
        val planejadas = buildList {
            diasAnterior.forEachIndexed { i, d ->
                val m = modalidadesAnterior[i]
                add(Planejada(d, m, if (i in premiadasAnterior) acertosComPremio(m, r) else null, r.nextInt(9, 22), r.nextInt(0, 60)))
            }
            diasAtual.forEachIndexed { i, d ->
                val m = modalidadesAtual[i]
                add(Planejada(d, m, if (i in premiadaAtual) acertosComPremio(m, r) else null, r.nextInt(9, 22), r.nextInt(0, 60)))
            }
        }
        val pendente = planejadas.lastIndex

        // Último jogo: um concurso real já sorteado (antes de hoje), se a Caixa respondeu.
        val diaPendente = planejadas[pendente].dia
        val realPendente: Concurso? = (PREFERIDAS_PARA_APURAR + planejadas[pendente].modalidade).distinct()
            .firstNotNullOfOrNull { m ->
                reais[m]?.takeIf { c ->
                    val data = c.data
                    c.temResultado && data != null && !data.isBefore(diaPendente) && data.isBefore(hoje)
                }
            }

        // O aporte cobre as apostas do mês mais caro com folga (em reais inteiros por integrante).
        fun custo(lista: List<Modalidade>) = lista.sumOf { preco(it) }
        val maiorMes = maxOf(custo(modalidadesAnterior), custo(modalidadesAtual), 3000L)
        val aporte = maxOf(1000L, ((maiorMes + 300 + n * 100 - 1) / (n * 100)) * 100)
        val saldoInicial = r.nextLong(0, 1500)

        var e = Estado(
            cadastroConcluido = true,
            cadastradoEm = anterior.atDay(1),
            pessoas = integrantes,
            config = Config(aporteMensal = aporte),
            proximoRateio = 1,
            lancamentos = listOfNotNull(
                Lancamento(
                    id = novoId(), data = anterior.atDay(1), criadoEm = t(anterior.atDay(1), 8, r.nextInt(0, 30)),
                    tipo = TipoLancamento.SALDO_INICIAL, valor = saldoInicial, obs = "Saldo inicial (exemplo)",
                ).takeIf { saldoInicial > 0 },
            ),
            exemplo = true,
        )

        val eventos = buildList {
            add(Evento.Aportes(t(anterior.atDay(1), 8, r.nextInt(30, 60)), anterior.atDay(1)))
            if (hoje.dayOfMonth > 1) add(Evento.Aportes(t(mes.atDay(1), 8, r.nextInt(30, 60)), mes.atDay(1)))
            if (r.nextInt(100) < 30) {
                val dia = anterior.atDay(r.nextInt(5, 26))
                add(Evento.Adicional(t(dia, r.nextInt(9, 20), r.nextInt(0, 60)), dia, r.nextLong(5, 21) * 100))
            }
            planejadas.forEachIndexed { i, p ->
                add(Evento.Apostar(t(p.dia, p.hora, p.minuto), i))
                if (i != pendente) add(Evento.Apurar(t(p.dia.plusDays(2), r.nextInt(8, 12), r.nextInt(0, 60)), i))
            }
            val diaRateio = anterior.atDay(r.nextInt(12, 21))
            add(Evento.Ratear(t(diaRateio, r.nextInt(12, 20), r.nextInt(0, 60)), diaRateio, 0.5 + r.nextDouble() * 0.5))
            add(Evento.FecharMes(t(anterior.atEndOfMonth(), r.nextInt(18, 23), r.nextInt(0, 60)), anterior, r.nextLong(1, RENDIMENTO_MAXIMO + 1)))
        }.sortedBy { it.instante }

        val ids = arrayOfNulls<String>(planejadas.size)
        val resultados = arrayOfNulls<Concurso>(planejadas.size)

        /** Custo das apostas planejadas depois do instante [depois] no mesmo mês de [dia]. */
        fun custoRestante(dia: LocalDate, depois: Long): Long = planejadas.withIndex()
            .filter { (_, p) -> YearMonth.from(p.dia) == YearMonth.from(dia) && t(p.dia, p.hora, p.minuto) > depois }
            .sumOf { (_, p) -> preco(p.modalidade) }

        for (ev in eventos) {
            e = when (ev) {
                is Evento.Aportes -> Operacoes.registrarAporteMensal(e, ev.dia, ev.instante)

                is Evento.Adicional -> enviar(Operacoes.registrarAporteAdicional(e, ev.dia, ev.valor, ev.instante), ev.instante)

                is Evento.Apostar -> {
                    val p = planejadas[ev.indice]
                    val (m, concurso, sorteio, palpite) = if (ev.indice == pendente) {
                        if (realPendente != null) {
                            Quatro(realPendente.modalidade, realPendente.numero, realPendente.data!!, palpiteLivre(realPendente.modalidade, r))
                        } else {
                            val sorteio = minOf(p.dia.plusDays(1), hoje.minusDays(1)).let { if (it.isBefore(p.dia)) p.dia else it }
                            Quatro(p.modalidade, numeroEstimado(p.modalidade, sorteio, reais), sorteio, palpiteLivre(p.modalidade, r))
                        }
                    } else {
                        val sorteio = p.dia.plusDays(1)
                        val numero = numeroEstimado(p.modalidade, sorteio, reais)
                        val (resultado, palpite) = sorteioFicticio(p.modalidade, numero, sorteio, p.acertosPremiados, r)
                        resultados[ev.indice] = resultado
                        Quatro(p.modalidade, numero, sorteio, palpite)
                    }
                    val id = novoId()
                    ids[ev.indice] = id
                    val a = Aposta(
                        id = id,
                        numero = 0,
                        modalidade = m,
                        concurso = concurso,
                        dataAposta = p.dia,
                        dataSorteio = sorteio,
                        premioEstimado = PREMIO_ESTIMADO[m],
                        valor = e.config.preco(m),
                        numeros = palpite.numeros,
                        trevos = palpite.trevos,
                        extra = palpite.extra,
                        palpitesLoteca = palpite.loteca,
                        comprovante = Arquivo(
                            nome = "${PREFIXO}comprovante-${id.take(8)}.png",
                            nomeOriginal = "comprovante-exemplo-${m.sigla}-$concurso.png",
                            mime = "image/png",
                            guardadoEm = ev.instante,
                        ),
                        criadaEm = ev.instante - r.nextLong(1, 20) * 60_000,
                    )
                    enviar(Operacoes.registrarApostaComComprovante(Operacoes.salvarRascunho(e, a), id, p.dia, ev.instante), ev.instante)
                }

                is Evento.Apurar -> {
                    val id = ids[ev.indice]!!
                    val c = resultados[ev.indice]!!
                    val a = e.apostas.first { it.id == id }
                    val conf = Conferencia.conferir(a.modalidade, a.palpite, c, a.bilheteFederal, a.fracoesFederal)
                    val ap = Operacoes.apuracaoDe(c, conf, conf.premio, ev.instante)
                    val imagem = Arquivo(
                        nome = "${PREFIXO}acertos-${id.take(8)}.png",
                        nomeOriginal = "acertos-${a.modalidade.sigla}-${a.concurso}.png",
                        mime = "image/png",
                        guardadoEm = ev.instante,
                    )
                    enviar(Operacoes.confirmarApuracao(e, id, ap, planejadas[ev.indice].dia.plusDays(2), ev.instante, imagem), ev.instante)
                }

                is Evento.Ratear -> {
                    // Uma parte do Extra, sem mexer no que falta para as apostas do mês.
                    val extra = Operacoes.extra(e, ev.dia)
                    val folga = Operacoes.saldo(e) - custoRestante(ev.dia, ev.instante) - 100
                    val pedido = minOf((extra * ev.fracao).toLong(), extra, folga)
                    if (pedido <= 0 || Operacoes.calcularRateio(e, ev.dia, pedido).cota <= 0) {
                        e
                    } else {
                        val numero = e.proximoRateio
                        val comprovantes = integrantes.mapIndexed { i, pessoa ->
                            Arquivo(
                                nome = "${PREFIXO}pix-$numero-${i + 1}.png",
                                nomeOriginal = "pix-exemplo-${pessoa.apelido}.png",
                                mime = "image/png",
                                guardadoEm = ev.instante,
                            )
                        }
                        enviar(Operacoes.registrarRateioComPix(e, ev.dia, pedido, comprovantes, ev.instante), ev.instante)
                    }
                }

                is Evento.FecharMes -> enviar(
                    Operacoes.registrarExtrato(
                        e, ev.mes, informado = Operacoes.saldo(e) + ev.rendimento,
                        hoje = ev.mes.atEndOfMonth(), agora = ev.instante,
                    ),
                    ev.instante,
                )
            }
        }

        // Para experimentar a aba Comprovantes: às vezes uma aposta de ontem esperando o comprovante
        // da Caixa e um rateio de ontem esperando os Pix (com o de um integrante já anexado).
        val ontem = hoje.minusDays(1)
        if (YearMonth.from(ontem) == mes && hoje.dayOfMonth > 1) {
            val instante = t(ontem, r.nextInt(18, 23), r.nextInt(0, 60))
            if (r.nextInt(100) < 55) {
                val m = listOf(Modalidade.LOTOFACIL, Modalidade.QUINA, Modalidade.MEGA_SENA).random(r)
                val custo = e.config.preco(m)
                if (Operacoes.saldo(e) - custo >= custoRestante(ontem, instante)) {
                    val real = reais[m]
                    val sorteio = real?.proximaData?.takeIf { !it.isBefore(hoje) } ?: hoje.plusDays(1)
                    val numero = real?.proximoNumero ?: numeroEstimado(m, sorteio, reais)
                    val palpite = palpiteLivre(m, r)
                    val a = Aposta(
                        id = novoId(), numero = 0, modalidade = m, concurso = numero, dataAposta = ontem, dataSorteio = sorteio,
                        premioEstimado = real?.premioEstimadoProximo ?: PREMIO_ESTIMADO[m], valor = custo,
                        numeros = palpite.numeros, trevos = palpite.trevos, extra = palpite.extra, criadaEm = instante - 120_000,
                    )
                    e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, a), a.id, ontem, instante)
                }
            }
            if (r.nextInt(100) < 35) {
                val depois = instante + 60_000
                val extra = Operacoes.extra(e, ontem)
                val folga = Operacoes.saldo(e) - custoRestante(ontem, depois) - 100
                val pedido = minOf(extra, folga) / 2
                if (pedido > 0 && Operacoes.calcularRateio(e, ontem, pedido).cota > 0) {
                    e = Operacoes.registrarRateio(e, ontem, pedido, depois)
                    val rt = e.rateios.last()
                    if (r.nextBoolean()) {
                        val p = integrantes.random(r)
                        e = Operacoes.anexarPix(
                            e, rt.numero, p.id,
                            Arquivo("${PREFIXO}pix-${rt.numero}-${p.id}.png", "pix-exemplo-${p.apelido}.png", "image/png", depois),
                        )
                    }
                }
            }
        }
        return e
    }

    /**
     * Exemplo do modo individual: apostas em dias sorteados dos dois meses (sem os dias programados),
     * uma ou duas premiadas, a última esperando a apuração; sem aporte, rateio, extrato e comprovante.
     */
    private fun gerarIndividual(
        hoje: LocalDate,
        agora: Long,
        fuso: ZoneId,
        reais: Map<Modalidade, Concurso>,
        pessoas: List<Pessoa>?,
        semente: Long,
    ): Estado {
        val r = Random(semente)
        val pessoa = pessoas?.firstOrNull() ?: NOMES.first()
        val ultimoInstante = agora - 60_000
        fun t(d: LocalDate, hora: Int, minuto: Int): Long =
            minOf(d.atTime(hora, minuto).atZone(fuso).toInstant().toEpochMilli(), ultimoInstante)
        val inicio = YearMonth.from(hoje).minusMonths(1).atDay(1)
        val todos = generateSequence(inicio) { it.plusDays(1) }.takeWhile { it.isBefore(hoje) }.toList()
        val dias = todos.shuffled(r).take(r.nextInt(8, 15).coerceAtMost(todos.size)).sorted()
        val premiadas = dias.indices.toList().dropLast(1).shuffled(r).take(r.nextInt(1, 3)).toSet()
        var e = Estado(
            cadastroConcluido = true, cadastradoEm = inicio, pessoas = listOf(pessoa.copy(administrador = true)),
            exemplo = true, modo = Modo.INDIVIDUAL,
        )
        dias.forEachIndexed { i, dia ->
            val ultima = i == dias.lastIndex
            val m = if (i in premiadas) PODEM_GANHAR.random(r) else SORTEIO_DE_MODALIDADES.random(r)
            val registro = t(dia, r.nextInt(8, 22), r.nextInt(0, 60))
            val real = if (ultima) (PREFERIDAS_PARA_APURAR + m).distinct().firstNotNullOfOrNull { k ->
                reais[k]?.takeIf { c -> c.temResultado && c.data != null && !c.data.isBefore(dia) && c.data.isBefore(hoje) }
            } else {
                null
            }
            val mod = real?.modalidade ?: m
            val sorteio = real?.data ?: minOf(dia.plusDays(1), hoje.minusDays(1)).let { if (it.isBefore(dia)) dia else it }
            val numero = real?.numero ?: numeroEstimado(mod, sorteio, reais)
            val (resultado, palpite) = if (ultima) {
                null to palpiteLivre(mod, r)
            } else {
                sorteioFicticio(mod, numero, sorteio, if (i in premiadas) acertosComPremio(mod, r) else null, r)
            }
            val a = Aposta(
                id = novoId(), numero = 0, modalidade = mod, concurso = numero, dataAposta = dia, dataSorteio = sorteio,
                premioEstimado = PREMIO_ESTIMADO[mod], valor = e.config.preco(mod), numeros = palpite.numeros,
                trevos = palpite.trevos, extra = palpite.extra, palpitesLoteca = palpite.loteca, criadaEm = registro - 60_000,
            )
            e = Operacoes.registrarAposta(Operacoes.salvarRascunho(e, a), a.id, dia, registro)
            if (resultado != null) {
                val conf = Conferencia.conferir(mod, a.palpite, resultado, null, null)
                val quando = t(sorteio.plusDays(1), r.nextInt(8, 12), r.nextInt(0, 60))
                val apurada = Operacoes.apuracaoDe(resultado, conf, conf.premio, quando)
                e = Operacoes.confirmarApuracao(e, a.id, apurada, sorteio.plusDays(1).coerceAtMost(hoje), quando)
            }
        }
        return e
    }

    /** O comprovante recém-criado foi enviado ao grupo um minuto depois. */
    private fun enviar(e: Estado, instante: Long): Estado {
        val c = e.comprovantes.lastOrNull() ?: return e
        return if (c.envioDispensado || e.enviado(c)) e else Operacoes.registrarEnvio(e, c.id, instante + 60_000)
    }

    private data class Quatro(val modalidade: Modalidade, val concurso: Int, val sorteio: LocalDate, val palpite: Palpite)

    /** Número de concurso plausível para um sorteio na data [sorteio]. */
    private fun numeroEstimado(m: Modalidade, sorteio: LocalDate, reais: Map<Modalidade, Concurso>): Int {
        val real = reais[m]
        val dataReal = real?.data
        val (numeroRef, dataRef) = if (real != null && dataReal != null) real.numero to dataReal else REFERENCIA.getValue(m) to DATA_REFERENCIA
        val dias = ChronoUnit.DAYS.between(sorteio, dataRef)
        val passos = ceil(abs(dias) * POR_SEMANA.getValue(m) / 7.0).toInt()
        return (if (dias >= 0) numeroRef - passos else numeroRef + passos).coerceAtLeast(1)
    }

    /** Palpite sorteado, como o do botão "Completar" (com Time do Coração). */
    private fun palpiteLivre(m: Modalidade, r: Random): Palpite {
        val p = Volante.sortear(m, r)
        return if (m.extra == Extra.TIME_DO_CORACAO) p.copy(extra = TIMES_APOSTADOS.random(r)) else p
    }

    /** Acertos de um jogo sem prêmio, por modalidade (abaixo da menor faixa premiada). */
    private fun acertosSemPremio(m: Modalidade, r: Random): Int = when (m) {
        Modalidade.LOTOFACIL -> r.nextInt(8, 11)
        Modalidade.LOTOMANIA -> r.nextInt(8, 12)
        Modalidade.QUINA -> r.nextInt(0, 2)
        Modalidade.TIMEMANIA -> r.nextInt(1, 3)
        Modalidade.DIA_DE_SORTE -> r.nextInt(1, 4)
        Modalidade.SUPER_SETE -> r.nextInt(0, 3)
        else -> r.nextInt(0, 3)
    }

    /**
     * Resultado fictício de um concurso e um palpite com o número de acertos desejado.
     * Sem [acertosPremiados], o jogo fica sem prêmio.
     */
    private fun sorteioFicticio(m: Modalidade, numero: Int, data: LocalDate, acertosPremiados: Int?, r: Random): Pair<Concurso, Palpite> {
        val acertos = acertosPremiados ?: acertosSemPremio(m, r)
        return when {
            m.formato == Formato.COLUNAS -> {
                val sorteado = List(Volante.COLUNAS_SUPER_SETE) { r.nextInt(0, 10) }
                val iguais = (0 until Volante.COLUNAS_SUPER_SETE).shuffled(r).take(acertos).toSet()
                val apostado = sorteado.mapIndexed { i, n -> if (i in iguais) n else (n + 1 + r.nextInt(9)) % 10 }
                Concurso(m, numero, data, dezenas = sorteado, faixas = faixasDo(m, r)) to Palpite(numeros = apostado)
            }
            m == Modalidade.DUPLA_SENA -> {
                val universo = Volante.universo(m).toList()
                val primeiro = universo.shuffled(r).take(m.qtdSorteada).sorted()
                val segundo = universo.shuffled(r).take(m.qtdSorteada).sorted()
                var apostado = universo.shuffled(r).take(m.qtdAposta)
                var tentativas = 0
                while ((apostado.count { it in primeiro } > 2 || apostado.count { it in segundo } > 2) && tentativas++ < 100) {
                    apostado = universo.shuffled(r).take(m.qtdAposta)
                }
                if (apostado.count { it in primeiro } > 2 || apostado.count { it in segundo } > 2) {
                    apostado = (universo - primeiro.toSet() - segundo.toSet()).shuffled(r).take(m.qtdAposta)
                }
                Concurso(m, numero, data, dezenas = primeiro, segundoSorteio = segundo, faixas = faixasDo(m, r)) to
                    Palpite(numeros = apostado.sorted())
            }
            else -> {
                val universo = Volante.universo(m).toList()
                val sorteados = universo.shuffled(r).take(m.qtdSorteada)
                val fora = (universo - sorteados.toSet()).shuffled(r)
                val apostado = (sorteados.shuffled(r).take(acertos) + fora.take(m.qtdAposta - acertos)).sorted()
                val (extraApostado, extraSorteado) = when (m.extra) {
                    Extra.TIME_DO_CORACAO -> TIMES_APOSTADOS.random(r) to TIME_SORTEADO
                    Extra.MES_DE_SORTE -> {
                        val meses = MESES_DE_SORTE.shuffled(r)
                        meses[0] to meses[1]
                    }
                    Extra.NENHUM -> null to null
                }
                Concurso(
                    modalidade = m,
                    numero = numero,
                    data = data,
                    dezenas = sorteados.sorted(),
                    timeOuMes = extraSorteado,
                    faixas = faixasDo(m, r),
                ) to Palpite(numeros = apostado, extra = extraApostado)
            }
        }
    }

    /** Anexos fictícios que o estado usa (comprovantes das apostas, imagens dos acertos e Pix do rateio). */
    fun anexos(e: Estado): List<Arquivo> =
        (e.comprovantes.flatMap { it.anexos } + e.rateios.flatMap { it.pix.values })
            .filter { it.nome.startsWith(PREFIXO) }.distinctBy { it.nome }

    /**
     * Os anexos de exemplo são desenhados no celular depois de gerar os dados: aqui entram as
     * impressões digitais deles ([sha256] de cada arquivo) e a corrente de autenticação é refeita.
     * Só para dados de exemplo; os reais nunca são reescritos.
     */
    fun comImpressoesDigitais(e: Estado, sha256: (Arquivo) -> String?): Estado {
        require(e.exemplo) { "Só dados de exemplo." }
        fun com(a: Arquivo) = if (a.nome.startsWith(PREFIXO)) a.copy(sha256 = sha256(a) ?: a.sha256) else a
        var anterior: String? = null
        val comprovantes = e.comprovantes.map { c ->
            val novo = c.copy(anexos = c.anexos.map(::com))
            val codigo = Autenticacao.calcular(anterior, Operacoes.partesAutenticacao(novo))
            anterior = codigo
            novo.copy(autenticacao = codigo)
        }
        return e.copy(
            comprovantes = comprovantes,
            apostas = e.apostas.map { a -> a.copy(comprovante = a.comprovante?.let(::com)) },
            rateios = e.rateios.map { r -> r.copy(comprovantes = r.comprovantes.map(::com), pix = r.pix.mapValues { com(it.value) }) },
        )
    }
}

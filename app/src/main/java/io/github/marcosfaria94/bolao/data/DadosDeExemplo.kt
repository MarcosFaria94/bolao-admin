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
 * Dados de exemplo do modo de teste, para experimentar todas as telas sem digitar nada:
 *
 * - mês anterior completo: saldo inicial, aporte mensal, 10 apostas apuradas (uma premiada),
 *   um rateio no dia 14 e o extrato do mês, cada um com o seu comprovante enviado;
 * - mês atual até ontem: aporte do dia 1º e as apostas dos dias programados, todas apuradas
 *   menos a última, que fica esperando a apuração. A aposta de hoje fica para quem testa.
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
    const val SALDO_INICIAL = 1002L
    const val RENDIMENTO_ANTERIOR = 3L

    /** Integrantes fictícios, usados enquanto não houver cadastro real. */
    val PESSOAS = listOf(
        Pessoa("exemplo-1", "Fulano de Tal", "Fulano", "fulano@exemplo.com", administrador = true),
        Pessoa("exemplo-2", "Beto da Silva", "Beto", "beto@exemplo.com"),
        Pessoa("exemplo-3", "Beltrano Souza", "Beltrano", "beltrano@exemplo.com"),
    )

    /** Modalidade de cada dia programado (1, 4, 7, ... 28). */
    // Depois do rateio do dia 14 sobram R$ 15,02 para as 5 apostas que faltam (R$ 15,00).
    private val PLANO_ANTERIOR = listOf(
        Modalidade.LOTOFACIL, Modalidade.QUINA, Modalidade.DIA_DE_SORTE, Modalidade.LOTOMANIA, Modalidade.SUPER_SETE,
        Modalidade.DUPLA_SENA, Modalidade.QUINA, Modalidade.DIA_DE_SORTE, Modalidade.LOTOMANIA, Modalidade.LOTOFACIL,
    )
    private val PLANO_ATUAL = listOf(
        Modalidade.LOTOFACIL, Modalidade.QUINA, Modalidade.TIMEMANIA, Modalidade.DIA_DE_SORTE, Modalidade.LOTOMANIA,
        Modalidade.LOTOFACIL, Modalidade.QUINA, Modalidade.SUPER_SETE, Modalidade.DUPLA_SENA, Modalidade.LOTOFACIL,
    )

    /** Para o último jogo: modalidades com sorteio quase todo dia, então quase sempre há resultado recente. */
    private val PREFERIDAS_PARA_APURAR = listOf(Modalidade.LOTOFACIL, Modalidade.QUINA)

    /** Modalidades que vale consultar na Caixa antes de gerar. */
    val MODALIDADES: List<Modalidade> = (PREFERIDAS_PARA_APURAR + PLANO_ANTERIOR + PLANO_ATUAL).distinct()

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

    private const val TIME_APOSTADO = "SANTOS/SP"
    private const val TIME_SORTEADO = "CRUZEIRO/MG"

    private data class Planejada(
        val dia: LocalDate,
        val modalidade: Modalidade,
        /** Acertos de um jogo premiado (Lotofácil); null = jogo sem prêmio. */
        val acertosPremiados: Int? = null,
    )

    private sealed class Evento(val instante: Long) {
        class Aportes(instante: Long, val dia: LocalDate) : Evento(instante)
        class Apostar(instante: Long, val indice: Int) : Evento(instante)
        class Apurar(instante: Long, val indice: Int) : Evento(instante)
        class Ratear(instante: Long, val dia: LocalDate) : Evento(instante)
        class FecharMes(instante: Long, val mes: YearMonth) : Evento(instante)
    }

    /**
     * Gera o estado de exemplo para o dia [hoje] ([agora] em milissegundos, no fuso [fuso]).
     * [reais]: últimos concursos consultados na Caixa (pode ser vazio).
     * [pessoas]: integrantes do cadastro real, se já existir (as mensagens saem com os nomes certos).
     */
    fun gerar(
        hoje: LocalDate,
        agora: Long,
        fuso: ZoneId = ZoneId.systemDefault(),
        reais: Map<Modalidade, Concurso> = emptyMap(),
        pessoas: List<Pessoa>? = null,
        semente: Long = hoje.toEpochDay(),
    ): Estado {
        val r = Random(semente)
        val integrantes = pessoas?.takeIf { it.size == 3 } ?: PESSOAS
        val ultimoInstante = agora - 60_000
        fun t(d: LocalDate, hora: Int, minuto: Int = 0): Long =
            minOf(d.atTime(hora, minuto).atZone(fuso).toInstant().toEpochMilli(), ultimoInstante)

        val mes = YearMonth.from(hoje)
        val anterior = mes.minusMonths(1)
        val planejadas = buildList {
            Calendario.diasProgramados(anterior).forEachIndexed { i, d ->
                add(Planejada(d, PLANO_ANTERIOR[i], acertosPremiados = if (i == 0) 12 else null))
            }
            Calendario.diasProgramados(mes).filter { it.isBefore(hoje) }.forEachIndexed { i, d ->
                add(Planejada(d, PLANO_ATUAL[i], acertosPremiados = if (i == 0) 11 else null))
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

        var e = Estado(
            cadastroConcluido = true,
            cadastradoEm = anterior.atDay(1),
            pessoas = integrantes,
            config = Config(),
            proximoRateio = 1,
            lancamentos = listOf(
                Lancamento(
                    id = novoId(), data = anterior.atDay(1), criadoEm = t(anterior.atDay(1), 8),
                    tipo = TipoLancamento.SALDO_INICIAL, valor = SALDO_INICIAL, obs = "Saldo inicial (exemplo)",
                ),
            ),
            exemplo = true,
        )

        val eventos = buildList {
            add(Evento.Aportes(t(anterior.atDay(1), 9), anterior.atDay(1)))
            if (hoje.dayOfMonth > 1) add(Evento.Aportes(t(mes.atDay(1), 9), mes.atDay(1)))
            planejadas.forEachIndexed { i, p ->
                add(Evento.Apostar(t(p.dia, 10), i))
                if (i != pendente) add(Evento.Apurar(t(p.dia.plusDays(2), 9), i))
            }
            add(Evento.Ratear(t(anterior.atDay(14), 12), anterior.atDay(14)))
            add(Evento.FecharMes(t(anterior.atEndOfMonth(), 20), anterior))
        }.sortedBy { it.instante }

        val ids = arrayOfNulls<String>(planejadas.size)
        val resultados = arrayOfNulls<Concurso>(planejadas.size)

        for (ev in eventos) {
            e = when (ev) {
                is Evento.Aportes -> Operacoes.registrarAporteMensal(e, ev.dia, ev.instante)

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
                        criadaEm = ev.instante - 5 * 60_000,
                    )
                    enviar(Operacoes.registrarAposta(Operacoes.salvarRascunho(e, a), id, p.dia, ev.instante), ev.instante)
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
                    val extra = Operacoes.extra(e, ev.dia)
                    if (Operacoes.calcularRateio(e, ev.dia, extra).cota <= 0) {
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
                        enviar(Operacoes.registrarRateio(e, ev.dia, extra, comprovantes, ev.instante), ev.instante)
                    }
                }

                is Evento.FecharMes -> enviar(
                    Operacoes.registrarExtrato(
                        e, ev.mes, informado = Operacoes.saldo(e) + RENDIMENTO_ANTERIOR,
                        hoje = ev.mes.atEndOfMonth(), agora = ev.instante,
                    ),
                    ev.instante,
                )
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
        return if (m.extra == Extra.TIME_DO_CORACAO) p.copy(extra = TIME_APOSTADO) else p
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
                Concurso(m, numero, data, dezenas = sorteado, faixas = FAIXAS[m].orEmpty()) to Palpite(numeros = apostado)
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
                Concurso(m, numero, data, dezenas = primeiro, segundoSorteio = segundo, faixas = FAIXAS[m].orEmpty()) to
                    Palpite(numeros = apostado.sorted())
            }
            else -> {
                val universo = Volante.universo(m).toList()
                val sorteados = universo.shuffled(r).take(m.qtdSorteada)
                val fora = (universo - sorteados.toSet()).shuffled(r)
                val apostado = (sorteados.shuffled(r).take(acertos) + fora.take(m.qtdAposta - acertos)).sorted()
                val (extraApostado, extraSorteado) = when (m.extra) {
                    Extra.TIME_DO_CORACAO -> TIME_APOSTADO to TIME_SORTEADO
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
                    faixas = FAIXAS[m].orEmpty(),
                ) to Palpite(numeros = apostado, extra = extraApostado)
            }
        }
    }

    /** Anexos fictícios que o estado usa (comprovantes das apostas, imagens dos acertos e Pix do rateio). */
    fun anexos(e: Estado): List<Arquivo> =
        e.comprovantes.flatMap { it.anexos }.filter { it.nome.startsWith(PREFIXO) }.distinctBy { it.nome }

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
            rateios = e.rateios.map { r -> r.copy(comprovantes = r.comprovantes.map(::com)) },
        )
    }
}

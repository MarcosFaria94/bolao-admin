package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.LimiteMensal
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.data.TipoDeLimite
import io.github.marcosfaria94.bolao.domain.AvisosDosJogos
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.ChavesPix
import io.github.marcosfaria94.bolao.domain.ConcursosNotaveis
import io.github.marcosfaria94.bolao.domain.ConfigDaCalculadora
import io.github.marcosfaria94.bolao.domain.Limites
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.NumerosNotaveis
import io.github.marcosfaria94.bolao.domain.ResumoDoConcurso
import io.github.marcosfaria94.bolao.domain.TipoDeChave
import io.github.marcosfaria94.bolao.domain.Transcricao
import io.github.marcosfaria94.bolao.domain.indices.IndiceEspecial
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.domain.indices.RegistroK
import io.github.marcosfaria94.bolao.ui.ajustes.SecaoAjuste
import io.github.marcosfaria94.bolao.ui.ajustes.lerK1
import io.github.marcosfaria94.bolao.ui.ajustes.PARAMETRO_K1
import io.github.marcosfaria94.bolao.ui.ajustes.resumoDosIntegrantes
import io.github.marcosfaria94.bolao.ui.ajustes.tituloDoGrupoDoBolao
import io.github.marcosfaria94.bolao.ui.ajustes.valorCurto
import io.github.marcosfaria94.bolao.ui.caixa.ehParaOsLados
import io.github.marcosfaria94.bolao.ui.caixa.lerCookie
import io.github.marcosfaria94.bolao.ui.caixa.textoDoLote
import io.github.marcosfaria94.bolao.ui.jogos.TECLAS_QUE_FECHAM
import io.github.marcosfaria94.bolao.ui.jogos.proximaOrdem
import io.github.marcosfaria94.bolao.ui.jogos.proximoIntervalo
import io.github.marcosfaria94.bolao.ui.jogos.OrdemDaTabela
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import kotlin.math.abs

/** Os pedidos da 0.38: chaves Pix, Ajustes, Parâmetros (stop loss, saldo, avisos), k1 com o IPCA, Estatísticas, calculadora, Caixa. */
class Novidades038Test {

    private fun arquivo(vararg caminhos: String): File = caminhos.map { File(it) }.first { it.exists() }

    @Test
    fun chavesPix() {
        assertEquals(TipoDeChave.CPF, ChavesPix.tipo("529.982.247-25"))
        assertEquals(TipoDeChave.CPF, ChavesPix.tipo("52998224725"))
        assertEquals("529.***.***-25", ChavesPix.exibir("52998224725"))
        assertEquals("529.***.***-25", ChavesPix.exibir(" 529.982.247-25 "))
        assertEquals(TipoDeChave.EMAIL, ChavesPix.tipo("pixfacil@aol.com"))
        assertEquals("pixfacil@aol.com", ChavesPix.exibir("pixfacil@aol.com"))
        // Telefone: com ou sem o +55, com ou sem a pontuação.
        assertEquals("(31) 99876-5432", ChavesPix.exibir("+5531998765432"))
        assertEquals("(31) 99876-5432", ChavesPix.exibir("31998765432"))
        assertEquals("(31) 99876-5432", ChavesPix.exibir("(31) 99876-5432"))
        assertEquals("(31) 3333-4444", ChavesPix.exibir("3133334444"))
        // Aleatória: ela mesma (a linha corta com reticências).
        val aleatoria = "123e4567-e89b-12d3-a456-426614174000"
        assertEquals(TipoDeChave.ALEATORIA, ChavesPix.tipo(aleatoria))
        assertEquals(aleatoria, ChavesPix.exibir(aleatoria))
        // CPF com o dígito errado não é CPF (e 11 algarismos sem o 9 do celular, nem telefone).
        assertEquals(TipoDeChave.OUTRA, ChavesPix.tipo("12345678900"))
        assertTrue(ChavesPix.cpfValido("52998224725"))
        assertFalse(ChavesPix.cpfValido("11111111111"))
        assertTrue(ChavesPix.avisoDoCpf(listOf(2)).startsWith("A chave Pix do integrante 2 é um CPF."))
        assertTrue(ChavesPix.avisoDoCpf(listOf(1, 3)).startsWith("As chaves Pix dos integrantes 1 e 3 são CPF."))
    }

    @Test
    fun ajustes() {
        assertEquals("BOLÃO (INFORMAL)", tituloDoGrupoDoBolao(true))
        assertEquals("BOLÃO (FORMAL)", tituloDoGrupoDoBolao(false))
        assertEquals("3 pessoas", resumoDosIntegrantes(3))
        for (individual in listOf(false, true)) {
            for (informal in listOf(false, true)) {
                assertTrue(SecaoAjuste.grupos(individual, informal).none { SecaoAjuste.EXTINCAO in it.second })
            }
        }
        // O endereço da extinção continua (o "Excluir" das Sessões).
        assertEquals(SecaoAjuste.EXTINCAO, SecaoAjuste.de("extincao"))
    }

    @Test
    fun parametrosNovos() {
        val p = Parametros()
        assertNull(p.limiteMensal)
        assertFalse(p.exigirSaldo || p.avisarAcumulado || p.avisarK1 || p.avisarPremio)
        assertEquals("Avisar se k1 >", PARAMETRO_K1)
        assertEquals("R$ 100 mi", valorCurto(100_000_000_00))
        assertEquals("R$ 1,5 bi", valorCurto(1_500_000_000_00))
        assertEquals("R$ 500 mil", valorCurto(500_000_00))
        assertEquals(0.85, lerK1("0,85")!!, 1e-12)
        assertNull(lerK1("abc"))
        assertEquals(Limites.SEM_LIMITE, Limites.resumo(null))
        assertEquals("20 apostas por mês", Limites.resumo(LimiteMensal(TipoDeLimite.APOSTAS, 20)))
    }

    @Test
    fun stopLossESaldo() {
        val hoje = LocalDate.of(2026, 11, 17)
        val e = io.github.marcosfaria94.bolao.data.DadosDeExemplo.gerar(hoje, java.time.ZonedDateTime.of(hoje.atTime(12, 0), java.time.ZoneId.of("America/Sao_Paulo")).toInstant().toEpochMilli(), java.time.ZoneId.of("America/Sao_Paulo"))
        val (feitas, gasto) = Limites.doMes(e, java.time.YearMonth.from(hoje))
        // Sem limite: registra.
        assertNull(Limites.bloqueio(e, 1, 600, hoje))
        val porApostas = e.copy(config = e.config.copy(parametros = e.parametros.copy(limiteMensal = LimiteMensal(TipoDeLimite.APOSTAS, feitas.toLong() + 1))))
        assertNull(Limites.bloqueio(porApostas, 1, 600, hoje))
        assertTrue(Limites.bloqueio(porApostas, 2, 1200, hoje)!!.startsWith("Stop loss"))
        val porDinheiro = e.copy(config = e.config.copy(parametros = e.parametros.copy(limiteMensal = LimiteMensal(TipoDeLimite.DINHEIRO, gasto + 500))))
        assertNull(Limites.bloqueio(porDinheiro, 1, 500, hoje))
        assertTrue(Limites.bloqueio(porDinheiro, 1, 501, hoje)!!.startsWith("Stop loss"))
        // Saldo insuficiente: só com o parâmetro ligado.
        val saldo = io.github.marcosfaria94.bolao.data.Operacoes.saldo(e)
        assertNull(Limites.bloqueio(e, 1, saldo + 1, hoje))
        val exige = e.copy(config = e.config.copy(parametros = e.parametros.copy(exigirSaldo = true)))
        assertTrue(Limites.bloqueio(exige, 1, saldo + 1, hoje)!!.startsWith("Saldo insuficiente"))
        assertNull(Limites.bloqueio(exige, 1, saldo, hoje))
    }

    @Test
    fun avisosDosJogos() {
        val hoje = LocalDate.of(2026, 11, 17)
        fun alvo(m: Modalidade, acumulou: Boolean, dia: LocalDate?, k1: Double?, premio: Long?) =
            AvisosDosJogos.Alvo(m, m.nome, 100, dia, acumulou, 101, premio, k1, "modalidade/${m.name}")
        val alvos = listOf(
            alvo(Modalidade.MEGA_SENA, true, hoje.minusDays(1), 0.70, 120_000_000_00),
            alvo(Modalidade.QUINA, true, hoje.minusDays(10), 0.85, 10_000_000_00),
            alvo(Modalidade.LOTOFACIL, false, hoje, 0.90, 2_000_000_00),
        )
        // Desligados: nada.
        assertTrue(AvisosDosJogos.devidos(Parametros(), alvos, hoje, emptySet()).isEmpty())
        val p = Parametros(avisarAcumulado = true, avisarK1 = true, k1Minimo = 0.8, avisarPremio = true, premioMinimo = 100_000_000_00)
        val avisos = AvisosDosJogos.devidos(p, alvos, hoje, emptySet())
        assertEquals(3, avisos.size)
        // Acumulou: só o recente (a Quina acumulou há 10 dias).
        assertEquals("Acumulou: Mega-Sena", avisos[0].titulo)
        assertEquals("modalidade/MEGA_SENA", avisos[0].destino)
        assertEquals("k1 acima de 0,80: Quina e Lotofácil", avisos[1].titulo)
        assertEquals("jogos", avisos[1].destino)
        assertTrue(avisos[2].titulo.startsWith("Prêmio acima de"))
        // Uma vez só.
        assertTrue(AvisosDosJogos.devidos(p, alvos, hoje, avisos.flatMap { it.chaves }.toSet()).isEmpty())
    }

    @Test
    fun k1ComOIpca() {
        val p = ParametrosK.ler(arquivo("src/main/assets/parametros_k.json", "app/src/main/assets/parametros_k.json").readText())
        assertEquals("1.4", p.versao)
        val c = p.correcao!!
        assertEquals(2026, c.anoBase)
        assertEquals(1.0, c.fator(2026), 0.0)
        assertEquals(1.0, c.fator(2020), 0.0)
        // 2027: os preços de 2026 ainda não estão na tabela, então nada muda; com o IPCA de 2026, os valores de 2027 voltam.
        assertEquals(1.0, c.fator(2027), 0.0)
        val com2026 = c.copy(ipca = c.ipca + (2026 to 4.0))
        assertEquals(1.04, com2026.fator(2027), 1e-12)
        // A previsão das vendas: a estimativa e as vendas do anterior 4% maiores, com o fator 1,04, dão as vendas 4% maiores.
        val v = p.modalidades.getValue("megasena").vendas
        val (n1, _) = IndiceK.preverN(v, 50_000_000.0, 6_000_000.0, 1, 0)
        val (n2, _) = IndiceK.preverN(v, 52_000_000.0, 6_240_000.0, 1, 0, 1.04)
        assertEquals(n1 * 1.04, n2, n1 * 1e-9)
        // Os 15 casos do manual continuam dando k1 (com os parâmetros novos, valores um pouco diferentes).
        val casos = Json.parseToJsonElement(arquivo("src/test/resources/casos_de_teste.json", "app/src/test/resources/casos_de_teste.json").readText())
            .jsonObject["casos"]!!.jsonArray.map { it.jsonObject }
        for (caso in casos) {
            val chave = (caso["chave"] as kotlinx.serialization.json.JsonPrimitive).content
            val r = IndiceK.calcular(p, chave, RegistroK.deJson(caso["ultimo"] as JsonObject), RegistroK.deJson(caso["anterior"] as JsonObject))
            r.k1?.let { assertTrue("$chave: $it", it in 0.05..5.0) }
        }
    }

    @Test
    fun medianaDosEspeciais() {
        assertEquals(2.0, IndiceEspecial.mediana(listOf(3.0, 1.0, 2.0)), 0.0)
        assertEquals(2.5, IndiceEspecial.mediana(listOf(4.0, 1.0, 2.0, 3.0)), 0.0)
    }

    private fun resumo(n: Int, numeros: List<Int>, ganhadores: Int = 0, premio: Long = 0, extra: String? = null, trevos: List<Int> = emptyList()) =
        ResumoDoConcurso(n, LocalDate.of(2026, 1, 1).plusDays(n.toLong()), ganhadores = ganhadores, porGanhador = premio, numeros = numeros, extra = extra, trevos = trevos)

    @Test
    fun concursosNotaveisOrdenados() {
        val r = listOf(resumo(1, emptyList(), 1, 500), resumo(2, emptyList(), 2, 100), resumo(3, emptyList(), 0, 0), resumo(4, emptyList(), 1, 300))
        // Começo: os maiores prêmios (o total da faixa 1).
        assertEquals(listOf(1, 4, 2), ConcursosNotaveis.ordenar(r, 0, false).map { it.numero })
        // Pelo concurso, do mais novo; e ao contrário.
        assertEquals(listOf(4, 2, 1), ConcursosNotaveis.ordenar(r, -1, false).map { it.numero })
        assertEquals(listOf(1, 2, 4), ConcursosNotaveis.ordenar(r, -1, true).map { it.numero })
        assertEquals(listOf(2, 4, 1), ConcursosNotaveis.ordenar(r, 1, false).map { it.numero })
        // Tocar na mesma coluna inverte; em outra, começa do maior.
        assertEquals(OrdemDaTabela(0, true), proximaOrdem(OrdemDaTabela(0, false), 0))
        assertEquals(OrdemDaTabela(-1, false), proximaOrdem(OrdemDaTabela(0, false), -1))
    }

    @Test
    fun numerosNotaveis() {
        val r = listOf(resumo(1, listOf(1, 2, 3, 4, 5, 6)), resumo(2, listOf(1, 2, 3, 7, 8, 9)), resumo(3, listOf(1, 10, 11, 12, 13, 14)))
        val g = NumerosNotaveis.grupos(Modalidade.MEGA_SENA, r).single()
        assertEquals(NumerosNotaveis.TITULO_NUMEROS, g.titulo)
        assertEquals(60, g.itens.size)
        assertEquals(listOf("01", "02", "03"), g.itens.take(3).map { it.rotulo })
        assertEquals(3, g.itens[0].vezes)
        assertEquals(0, g.itens[0].atraso)
        // O 02 saiu no penúltimo: atraso 1; o 60 nunca saiu: sem atraso.
        assertEquals(1, g.itens.first { it.rotulo == "02" }.atraso)
        assertNull(g.itens.first { it.rotulo == "60" }.atraso)
        assertEquals("33,3%", NumerosNotaveis.porcentagem(1, 3))
        // Timemania: Times notáveis (o mesmo time com espaços diferentes junta).
        val tm = listOf(resumo(1, listOf(1), extra = "FLAMENGO   /RJ"), resumo(2, listOf(2), extra = "FLAMENGO/RJ"), resumo(3, listOf(3), extra = "BAHIA/BA"))
        val times = NumerosNotaveis.grupos(Modalidade.TIMEMANIA, tm).first { it.titulo == NumerosNotaveis.TITULO_TIMES }
        assertEquals("FLAMENGO/RJ", times.itens[0].rotulo)
        assertEquals(2, times.itens[0].vezes)
        // Dia de Sorte: os 12 meses; +Milionária: os trevos.
        val ds = NumerosNotaveis.grupos(Modalidade.DIA_DE_SORTE, listOf(resumo(1, listOf(1), extra = "Março"), resumo(2, listOf(2), extra = "MARCO")))
        val meses = ds.first { it.titulo == NumerosNotaveis.TITULO_MESES }
        assertEquals(12, meses.itens.size)
        assertEquals("Março", meses.itens[0].rotulo)
        assertEquals(2, meses.itens[0].vezes)
        val mm = NumerosNotaveis.grupos(Modalidade.MAIS_MILIONARIA, listOf(resumo(1, listOf(1, 2, 3, 4, 5, 6), trevos = listOf(2, 5))))
        assertEquals(listOf(NumerosNotaveis.TITULO_NUMEROS, NumerosNotaveis.TITULO_TREVOS), mm.map { it.titulo })
        // A Lotomania tem o 00.
        assertTrue(NumerosNotaveis.grupos(Modalidade.LOTOMANIA, emptyList()).single().itens.any { it.rotulo == "00" })
    }

    @Test
    fun numerosNotaveisDoHistorico() {
        val pasta = arquivo("src/main/assets/historico", "app/src/main/assets/historico")
        for (m in listOf(Modalidade.MEGA_SENA, Modalidade.TIMEMANIA, Modalidade.SUPER_SETE, Modalidade.LOTECA, Modalidade.DUPLA_SENA)) {
            val linhas = GZIPInputStream(File(pasta, "${m.slugApi}.jsonl.gzip").inputStream()).bufferedReader().readLines()
            val resumos = linhas.mapNotNull { ResumoDoConcurso.daLinha(it) }
            val grupos = NumerosNotaveis.grupos(m, resumos)
            assertTrue("sem notáveis na ${m.nome}", grupos.isNotEmpty() && grupos.all { it.itens.isNotEmpty() })
            val g = grupos.first()
            assertTrue("${m.nome}: ${g.itens.first().vezes}", g.itens.first().vezes > 0)
        }
    }

    @Test
    fun calculadora() {
        val m = Modalidade.MEGA_SENA
        val uma = Calculadora.chances(m, ConfigDaCalculadora())
        assertEquals("1 em 50.063.860", Calculadora.umEm(uma[0].p))
        assertEquals(Calculadora.NOME_QUALQUER, uma.last().nome)
        // Dez apostas simples: a sena fica praticamente 10 vezes mais provável.
        val dez = Calculadora.chances(m, ConfigDaCalculadora(simples = 10))
        assertEquals(10 * uma[0].p, dez[0].p, uma[0].p * 1e-5)
        // Uma aposta de 7 números vale 7 simples na sena.
        val sete = Calculadora.adicionar(m, ConfigDaCalculadora(simples = 0), 7, 0, 1)
        assertEquals(7L, Calculadora.apostasSimples(m, sete))
        assertEquals(7 * uma[0].p, Calculadora.chances(m, sete)[0].p, uma[0].p * 1e-6)
        // Somar o mesmo tipo junta no mesmo controle; a amplitude cresce para caber.
        val mais = Calculadora.adicionar(m, Calculadora.adicionar(m, ConfigDaCalculadora(), 8, 0, 5), 8, 0, 30)
        assertEquals(1, mais.extras.size)
        assertEquals(35, mais.extras[0].apostas)
        assertEquals(50, mais.maximo)
        // O tipo da simples vai para o controle da simples.
        assertEquals(4, Calculadora.adicionar(m, ConfigDaCalculadora(simples = 1), 6, 0, 3).simples)
        // As faixas do painel escolhidas na engrenagem.
        assertEquals(listOf("Quadra", Calculadora.NOME_QUALQUER), Calculadora.doPainel(m, ConfigDaCalculadora(faixas = listOf(2, Calculadora.QUALQUER_PREMIO))).map { it.nome })
        // Dupla Sena: os 2 sorteios.
        val dupla = Calculadora.chances(Modalidade.DUPLA_SENA, ConfigDaCalculadora())
        assertTrue(abs(dupla[0].p * 15_890_700 - 2.0) < 0.01)
        assertEquals("12,5%", Calculadora.porcentagem(0.125))
        assertEquals("< 0,0001%", Calculadora.porcentagem(1e-9))
        assertFalse(Calculadora.tem(Modalidade.FEDERAL))
    }

    @Test
    fun seletorECaixa() {
        // Segurar o botão: o intervalo cai 12% a cada passo, até 250 ms (4 por segundo).
        var i = 650L
        repeat(30) { i = proximoIntervalo(i) }
        assertEquals(250L, i)
        assertEquals(setOf(',', '.', ' ', '-'), TECLAS_QUE_FECHAM)
        assertTrue(ehParaOsLados(30f, 10f))
        assertFalse(ehParaOsLados(20f, 20f))
        assertEquals(io.github.marcosfaria94.bolao.ui.caixa.PosicaoDoCookie(0.9f, 0.85f, 0.1f), lerCookie("{\"x\":0.9,\"topo\":0.85,\"largura\":0.1}"))
        assertNull(lerCookie("null"))
        assertEquals("/mega-sena", Transcricao.fragmento(Transcricao.Pagina(Modalidade.MEGA_SENA, false)))
        assertEquals("/mega-sena/especial", Transcricao.fragmento(Transcricao.Pagina(Modalidade.MEGA_SENA, true)))
        // O fragmento leva de volta à mesma página.
        for (m in Modalidade.ATIVAS.filter { it != Modalidade.FEDERAL }) {
            for (esp in listOf(false, true)) {
                val p = Transcricao.Pagina(m, esp)
                val volta = Transcricao.pagina(Transcricao.ENDERECO + "#" + Transcricao.fragmento(p))
                if (esp && m !in listOf(Modalidade.MEGA_SENA, Modalidade.QUINA, Modalidade.LOTOFACIL, Modalidade.DUPLA_SENA)) continue
                assertEquals("$p", p, volta)
            }
        }
        assertEquals("Nenhuma aposta para transcrever agora.", textoDoLote(emptyList()))
    }
}

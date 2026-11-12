package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.domain.Tarjas
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.net.CaixaParser
import io.github.marcosfaria94.bolao.pdf.JogosPdf
import io.github.marcosfaria94.bolao.ui.aposta.novaAposta
import io.github.marcosfaria94.bolao.ui.aposta.rascunhoDa
import io.github.marcosfaria94.bolao.ui.aposta.textoRegistrarCurto
import io.github.marcosfaria94.bolao.ui.jogos.acumuladosDaPagina
import io.github.marcosfaria94.bolao.ui.jogos.regras
import io.github.marcosfaria94.bolao.ui.painel.avisoDaExclusao
import io.github.marcosfaria94.bolao.ui.painel.textoDaExclusao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.zip.Inflater
import kotlin.random.Random

/** As novidades da 0.27: tarjas, Federal fora, concursos especiais, Detalhes em tabelas, +1, lixeiras e o PDF dos jogos. */
class Novidades027Test {

    private val hoje = LocalDate.of(2026, 11, 12)

    private fun aposta(m: Modalidade, numero: Int, concurso: Int, registrada: Boolean = true, valor: Long = 600) = Aposta(
        id = "a$numero", numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje,
        valor = valor, registrada = registrada, criadaEm = numero.toLong(),
    )

    @Test
    fun semALoteriaFederal() {
        assertTrue(Modalidade.FEDERAL !in Modalidade.ATIVAS)
        assertEquals(Modalidade.entries.size - 1, Modalidade.ATIVAS.size)
        // Continua no enum só para ler as apostas antigas.
        assertEquals(Modalidade.FEDERAL, Modalidade.porSigla("FE"))
    }

    @Test
    fun tarjasComoAsDaCaixa() {
        assertEquals("mega-sena", Tarjas.nome(Modalidade.MEGA_SENA))
        assertEquals("+milionária", Tarjas.nome(Modalidade.MAIS_MILIONARIA))
        assertEquals("dia de sorte", Tarjas.nome(Modalidade.DIA_DE_SORTE))
        val virada = Especiais.doConcurso(Modalidade.MEGA_SENA, 3100)
        assertEquals("mega da virada", Tarjas.nome(Modalidade.MEGA_SENA, virada))
        // A Timemania é a única com o nome verde (o fundo é amarelo).
        assertTrue(Modalidade.ATIVAS.filter { Tarjas.cores(it).texto != 0xFFFFFFFF }.single() == Modalidade.TIMEMANIA)
        // As 4 folhas do trevo cabem no tamanho pedido, em volta do centro.
        for ((angulo, _) in Tarjas.FOLHAS) {
            val (inicio, curvas) = Tarjas.folha(angulo, 50f, 50f, 40f)
            // Os pontos da curva (os de controle podem ficar um pouco para fora).
            val pontos = listOf(inicio.toList()) + curvas.map { it.slice(4..5) }
            pontos.forEach { (x, y) -> assertTrue("$angulo: $x, $y", x in 29f..71f && y in 29f..71f) }
        }
    }

    @Test
    fun concursosEspeciais() {
        val e = Especiais.doConcurso(Modalidade.MEGA_SENA, 3100)!!
        assertEquals("Mega da Virada", e.nome)
        assertEquals(LocalDateTime.of(2027, 1, 1, 11, 0), e.sorteio)
        assertNull(Especiais.doConcurso(Modalidade.MEGA_SENA, 3085))
        assertNull(Especiais.doConcurso(Modalidade.QUINA, 3100))
        // O sorteio especial tem a hora dele na agenda da Caixa (a consulta do resultado é 5 minutos depois).
        assertEquals(LocalTime.of(11, 0), AgendaDaCaixa.horaDoSorteio(Modalidade.MEGA_SENA, LocalDate.of(2027, 1, 1)))
        assertEquals(LocalTime.of(21, 0), AgendaDaCaixa.horaDoSorteio(Modalidade.QUINA, LocalDate.of(2027, 1, 1)))
        // À parte nos Jogos enquanto o sorteio não passou e o próximo concurso da Mega ainda é outro.
        val agora = LocalDate.of(2026, 11, 12).atTime(8, 0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant()
        assertEquals(listOf(e), Especiais.aParte(agora, { if (it == Modalidade.MEGA_SENA) 3085 else null }))
        // Quando vira o próximo concurso, é o cartão da Mega-Sena que tem a tarja dele.
        assertTrue(Especiais.aParte(agora, { if (it == Modalidade.MEGA_SENA) 3100 else null }).isEmpty())
        // Depois do sorteio, sai.
        assertTrue(Especiais.aParte(Instant.parse("2027-01-01T15:00:00Z"), { 3085 }).isEmpty())
        assertEquals("Lotofácil da Independência", Especiais.nomeDoEspecial(Modalidade.LOTOFACIL))
    }

    @Test
    fun apostaDoConcursoEspecial() {
        val virada = Especiais.doConcurso(Modalidade.MEGA_SENA, 3100)!!
        val e = Estado()
        val nova = novaAposta(e, Modalidade.MEGA_SENA, null, hoje, 5, especial = virada)
        assertEquals(3100, nova.concurso)
        assertEquals(LocalDate.of(2027, 1, 1), nova.dataSorteio)
        assertEquals(100_000_000_000L, nova.premioEstimado)
        assertEquals(600, nova.valor)
        // A página normal da Mega não abre a aposta em andamento da Virada (e vice-versa).
        val normal = aposta(Modalidade.MEGA_SENA, 1, 3085, registrada = false)
        val daVirada = aposta(Modalidade.MEGA_SENA, 2, 3100, registrada = false)
        val comAs2 = Estado(apostas = listOf(normal, daVirada))
        assertEquals(normal.id, rascunhoDa(comAs2, Modalidade.MEGA_SENA, separados = setOf(3100))?.id)
        assertEquals(daVirada.id, rascunhoDa(comAs2, Modalidade.MEGA_SENA, especial = 3100)?.id)
        assertNull(rascunhoDa(Estado(apostas = listOf(daVirada)), Modalidade.MEGA_SENA, separados = setOf(3100)))
    }

    @Test
    fun acumuladosDetalhadosDaCaixa() {
        // Os campos da API da Caixa (como no site: próximo concurso, final 0 ou 5 e o especial).
        val json = """{"numero":3084,"numeroConcursoProximo":3085,"dataProximoConcurso":"13/11/2026","acumulado":true,
            "valorAcumuladoProximoConcurso":171152252.94,"valorAcumuladoConcurso_0_5":18277211.13,"numeroConcursoFinal_0_5":3090,
            "valorAcumuladoConcursoEspecial":101994941.78,"listaDezenas":["06","14","22","30","31","37"]}"""
        val u = CaixaParser.ler(Modalidade.MEGA_SENA, json)
        assertEquals(17_115_225_294L, u.acumuladoProximo)
        assertEquals(1_827_721_113L, u.acumuladoFinal05)
        assertEquals(3090, u.numeroConcursoEspecial)
        assertEquals(
            listOf(
                "Acumulado para o concurso 3085" to 17_115_225_294L,
                "Acumulado para o concurso de final 0 (3090)" to 1_827_721_113L,
                "Acumulado para a Mega da Virada" to 10_199_494_178L,
            ),
            acumuladosDaPagina(Modalidade.MEGA_SENA, u, doProximo = true, esp = null),
        )
        // Na página da Virada, só o acumulado para ela; fora do próximo concurso, nada.
        val virada = Especiais.doConcurso(Modalidade.MEGA_SENA, 3100)
        assertEquals(listOf("Acumulado para a Mega da Virada" to 10_199_494_178L), acumuladosDaPagina(Modalidade.MEGA_SENA, u, false, virada))
        assertTrue(acumuladosDaPagina(Modalidade.MEGA_SENA, u, doProximo = false, esp = null).isEmpty())
        // Sem acumulado (alguém ganhou), a linha não aparece.
        val semAcumulado = u.copy(acumuladoProximo = null, acumuladoEspecial = null)
        assertEquals(listOf("Acumulado para o concurso de final 0 (3090)" to 1_827_721_113L), acumuladosDaPagina(Modalidade.MEGA_SENA, semAcumulado, true, null))
        // Modalidade sem nome de especial.
        val timemania = Concurso(Modalidade.TIMEMANIA, 10, null, emptyList(), acumuladoEspecial = 100)
        assertEquals(listOf("Acumulado para o concurso especial" to 100L), acumuladosDaPagina(Modalidade.TIMEMANIA, timemania, true, null))
    }

    @Test
    fun tabelaDeProbabilidadesComoADaCaixa() {
        val mega = Probabilidades.tabela(Modalidade.MEGA_SENA, mais = true)
        assertEquals((6..20).toList(), mega.map { it.numeros })
        // Os valores da tabela do site da Caixa.
        assertEquals(listOf<Long?>(50_063_860, 154_518, 2_332), mega[0].umEm)
        assertEquals(listOf<Long?>(7_151_980, 44_981, 1_038), mega[1].umEm)
        assertEquals(1_787_995L, mega[2].umEm[0])
        assertEquals(595_998L, mega[3].umEm[0])
        assertEquals(238_399L, mega[4].umEm[0])
        assertEquals(108_363L, mega[5].umEm[0])
        assertEquals(54_182L, mega[6].umEm[0])
        assertEquals(29_175L, mega[7].umEm[0])
        assertEquals(16_671L, mega[8].umEm[0])
        assertEquals(28L, mega[2].apostasSimples) // 8 números: R$ 168,00 na Caixa
        assertEquals(38_760L, mega.last().apostasSimples) // 20 números: R$ 232.560,00
        // Sem as apostas com mais números, só a aposta simples.
        assertEquals(1, Probabilidades.tabela(Modalidade.MEGA_SENA, mais = false).size)
        // As colunas acompanham as faixas (o time e o mês no fim).
        assertEquals(6, Probabilidades.colunas(Modalidade.TIMEMANIA).size)
        assertEquals(listOf<Long?>(26_472_637, 216_103, 5_220, 276, 29, 80), Probabilidades.tabela(Modalidade.TIMEMANIA, true).single().umEm)
        assertEquals(3_268_760L, Probabilidades.tabela(Modalidade.LOTOFACIL, false).single().umEm[0])
        // +Milionária: números e trevos.
        val mais = Probabilidades.tabela(Modalidade.MAIS_MILIONARIA, mais = true)
        assertEquals(7 * 5, mais.size)
        assertEquals(238_360_500L, mais.first().umEm[0])
        assertEquals(Probabilidades.colunas(Modalidade.MAIS_MILIONARIA).size, mais.first().umEm.size)
        assertEquals(Volante.apostasSimples(Modalidade.MAIS_MILIONARIA, 12, 6), mais.last().apostasSimples)
        assertEquals("50.063.860", Probabilidades.milhares(50_063_860))
    }

    @Test
    fun quantidadeDePremiosComoADaCaixa() {
        val grupos = Probabilidades.gruposDePremios(Modalidade.MEGA_SENA)!!
        assertEquals(listOf(6, 5, 4), grupos.map { it.acertando })
        assertEquals(listOf(listOf(6, 5, 4), listOf(5, 4), listOf(4)), grupos.map { it.faixas })
        // As linhas da tabela da Caixa: 6 números = 1 sena; 7 = 1 sena e 6 quinas; 8 = 1, 12, 15...
        assertEquals(listOf(listOf(1L, 0, 0), listOf(1L, 0), listOf(1L)), Probabilidades.quantidadeDePremios(Modalidade.MEGA_SENA, 6))
        assertEquals(listOf(listOf(1L, 6, 0), listOf(2L, 5), listOf(3L)), Probabilidades.quantidadeDePremios(Modalidade.MEGA_SENA, 7))
        assertEquals(listOf(listOf(1L, 12, 15), listOf(3L, 15), listOf(6L)), Probabilidades.quantidadeDePremios(Modalidade.MEGA_SENA, 8))
        assertEquals(listOf(1L, 24, 90), Probabilidades.quantidadeDePremios(Modalidade.MEGA_SENA, 10)[0])
        assertEquals("Quadra", Probabilidades.nomeDaFaixa(Modalidade.MEGA_SENA, 4))
        assertEquals("11 acertos", Probabilidades.nomeDaFaixa(Modalidade.LOTOFACIL, 11))
        // Só nas modalidades com mais números (sem a +Milionária, por causa dos trevos).
        assertNull(Probabilidades.gruposDePremios(Modalidade.LOTOMANIA))
        assertNull(Probabilidades.gruposDePremios(Modalidade.MAIS_MILIONARIA))
        assertNull(Probabilidades.gruposDePremios(Modalidade.LOTECA))
    }

    @Test
    fun regrasNoFormatoPedido() {
        val e = Estado()
        assertEquals(
            listOf(
                "Volante" to "01 a 60",
                "Aposta" to "6 a 20 números",
                "Preço" to "R$ 6,00 a R$ 232.560,00",
                "Sorteio" to "6 números",
                "Premiação" to "A partir de 4 números",
            ),
            regras(Modalidade.MEGA_SENA, e, mais = true),
        )
        assertEquals(listOf("6 números", "R$ 6,00"), regras(Modalidade.MEGA_SENA, e, mais = false).map { it.second }.slice(1..2))
        assertEquals("2 sorteios de 6 números", regras(Modalidade.DUPLA_SENA, e, false).first { it.first == "Sorteio" }.second)
        assertEquals("6 a 12 números e 2 a 6 trevos", regras(Modalidade.MAIS_MILIONARIA, e, true).first { it.first == "Aposta" }.second)
        assertEquals("10 números e o Time do Coração", regras(Modalidade.TIMEMANIA, e, true).first { it.first == "Aposta" }.second)
        assertEquals("A partir de 13 jogos", regras(Modalidade.LOTECA, e, true).last().second)
    }

    @Test
    fun maisUmNumero() {
        val m = Modalidade.MEGA_SENA
        val seis = Palpite(numeros = listOf(1, 2, 3, 4, 5, 6))
        val sete = Volante.maisUm(m, seis, Random(1))
        assertEquals(7, sete.numeros.size)
        assertTrue(sete.numeros.containsAll(seis.numeros))
        assertEquals(sete.numeros.sorted(), sete.numeros)
        val vinte = Palpite(numeros = (1..20).toList())
        assertEquals(vinte, Volante.maisUm(m, vinte))
        assertTrue(!Volante.cabeMaisUm(m, vinte))
        assertTrue(Volante.cabeMaisUm(m, seis))
        assertEquals("Registrar", textoRegistrarCurto(1))
        assertEquals("Registrar (4)", textoRegistrarCurto(4))
    }

    @Test
    fun lixeirasDoPainel() {
        val rascunho = aposta(Modalidade.QUINA, 0, 7149, registrada = false).copy(id = "r")
        val semComprovante = aposta(Modalidade.QUINA, 1, 7149).copy(pendenteDeAnexo = true)
        val aConferir = aposta(Modalidade.QUINA, 2, 7149).copy(pendenteDeAnexo = true, comprovante = Arquivo("c.pdf", "c.pdf", "application/pdf", 0))
        val conferida = aposta(Modalidade.QUINA, 3, 7149)
        val lancamentos = listOf(semComprovante, aConferir, conferida).map {
            Lancamento("l${it.numero}", hoje, it.numero.toLong(), TipoLancamento.APOSTA, -it.valor, apostaId = it.id)
        }
        val e = Estado(apostas = listOf(rascunho, semComprovante, aConferir, conferida), lancamentos = lancamentos)
        val depois = Operacoes.descartarApostas(e, e.apostas.map { it.id })
        // Saem as que ainda podem; a conferida fica (e os lançamentos das que saíram também saem).
        assertEquals(listOf(conferida.id), depois.apostas.map { it.id })
        assertEquals(listOf(conferida.id), depois.lancamentos.mapNotNull { it.apostaId })
        // No modo individual, qualquer uma.
        val individual = e.copy(modo = Modo.INDIVIDUAL)
        assertTrue(Operacoes.descartarApostas(individual, listOf(conferida.id)).apostas.none { it.id == conferida.id })
        // Os textos da confirmação e do aviso.
        assertEquals("A aposta Nº1 da Quina (concurso 7149) sai do app e R$ 6,00 volta ao saldo.", textoDaExclusao(listOf(semComprovante)))
        assertEquals("A nova aposta (ainda não registrada) da Quina (concurso 7149) sai do app.", textoDaExclusao(listOf(rascunho)))
        assertEquals(
            "As 3 apostas saem do app e R$ 12,00 voltam ao saldo. A aposta já conferida fica: ela não pode mais sair do app.",
            textoDaExclusao(listOf(rascunho, semComprovante, aConferir), ficam = 1),
        )
        assertEquals("Aposta excluída: R$ 6,00 voltou ao saldo.", avisoDaExclusao(listOf(semComprovante)))
        assertEquals("2 apostas excluídas.", avisoDaExclusao(listOf(rascunho, rascunho.copy(id = "r2"))))
    }

    /** Abre os conteúdos (comprimidos) das páginas do PDF, para conferir o texto. */
    private fun conteudos(pdf: ByteArray): String {
        val s = String(pdf, Charsets.ISO_8859_1)
        val sb = StringBuilder()
        var i = s.indexOf("/FlateDecode /Length ")
        while (i >= 0) {
            val inicio = s.indexOf("stream\n", i) + 7
            val fim = s.indexOf("\nendstream", inicio)
            val inf = Inflater()
            inf.setInput(pdf, inicio, fim - inicio)
            val buf = ByteArray(200_000)
            val n = inf.inflate(buf)
            inf.end()
            sb.append(String(buf, 0, n, Charsets.ISO_8859_1))
            i = s.indexOf("/FlateDecode /Length ", fim)
        }
        return sb.toString()
    }

    @Test
    fun pdfDosJogosLeveEAgrupado() {
        assertEquals("Sexta-feira, 13/11/2026 às 21h", JogosPdf.quando(Modalidade.QUINA, LocalDate.of(2026, 11, 13)))
        assertEquals("Domingo, 15/11/2026 às 11h", JogosPdf.quando(Modalidade.QUINA, LocalDate.of(2026, 11, 15)))
        assertEquals("Sexta-feira, 01/01/2027 às 11h", JogosPdf.quando(Modalidade.MEGA_SENA, LocalDate.of(2027, 1, 1)))
        val quina = (1..3).map { aposta(Modalidade.QUINA, it, 7149).copy(numeros = listOf(5, 12, 33, 47, 80)) }
        val time = aposta(Modalidade.TIMEMANIA, 4, 2300).copy(numeros = (1..10).toList(), extra = "FLAMENGO/RJ")
        val grupos = listOf(
            JogosPdf.Grupo(Modalidade.QUINA, 7149, LocalDate.of(2026, 11, 13), "R$ 7 Milhões", quina),
            JogosPdf.Grupo(Modalidade.TIMEMANIA, 2300, null, "Não divulgado", listOf(time)),
            JogosPdf.Grupo(Modalidade.MEGA_SENA, 3100, LocalDate.of(2027, 1, 1), "R$ 1 Bilhão", listOf(aposta(Modalidade.MEGA_SENA, 5, 3100).copy(numeros = (1..6).toList()))),
        )
        val pdf = JogosPdf.gerar(grupos, hoje, teste = true) { if (it == "FLAMENGO/RJ") "Flamengo/RJ" else null }
        assertTrue(String(pdf, 0, 8, Charsets.ISO_8859_1).startsWith("%PDF-1.4"))
        // Leve: só texto e formas.
        assertTrue("${pdf.size} bytes", pdf.size < 20_000)
        // Página do tamanho de uma tela de celular.
        assertTrue(String(pdf, Charsets.ISO_8859_1).contains("/MediaBox [0 0 360 640]"))
        val texto = conteudos(pdf)
        for (t in listOf("(Meus jogos)", "(quina)", "(timemania)", "(mega da virada)", "(Concurso 7149)", "(Aposta N\\272" + "1)", "(80)",
            "(Time do Cora\\347\\343o: Flamengo/RJ)", "(Sorteio: N\\343o divulgado)", "(Pr\\352mio estimado: R$ 7 Milh\\365es)", "(Sorteio: Sexta-feira, 01/01/2027 \\340s 11h)")) {
            assertTrue(t, texto.contains(t))
        }
        assertTrue(texto.contains("TESTE do app"))
        // Muitas apostas: mais de uma página.
        val muitas = (1..40).map { aposta(Modalidade.LOTOMANIA, it, 2800).copy(numeros = (0..49).toList()) }
        val grande = JogosPdf.gerar(listOf(JogosPdf.Grupo(Modalidade.LOTOMANIA, 2800, null, "R$ 2 Milhões", muitas)), hoje, teste = false)
        val paginas = Regex("/Count (\\d+)").find(String(grande, Charsets.ISO_8859_1))!!.groupValues[1].toInt()
        assertTrue("$paginas páginas", paginas > 5)
    }
}

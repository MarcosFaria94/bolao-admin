package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.LeituraComprovante
import io.github.marcosfaria94.bolao.domain.Modalidade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** "Registrar comprovantes" (aba Comprovantes): cada arquivo lido vai para a aposta ou o rateio certo. */
class AssociacaoTest {

    private val hoje = LocalDate.of(2026, 10, 27)
    private var relogio = 1_000L
    private fun agora() = relogio.also { relogio += 10 }

    private val pessoas = listOf(
        Pessoa("p1", "Fulano de Tal", "Fulano", "fulano@exemplo.com", administrador = true),
        Pessoa("p2", "Beto da Silva", "Beto", "beto@exemplo.com"),
        Pessoa("p3", "Beltrano Souza", "Beltrano", "beltrano@exemplo.com"),
    )

    private fun inicial() = Estado(
        cadastroConcluido = true, cadastradoEm = hoje.withDayOfMonth(1), pessoas = pessoas,
        lancamentos = listOf(Lancamento("ini", hoje.withDayOfMonth(1), 0, TipoLancamento.SALDO_INICIAL, 20_000)),
    )

    private fun registrar(e: Estado, id: String, m: Modalidade, concurso: Int, numeros: List<Int>): Estado {
        val a = Aposta(
            id = id, numero = 0, modalidade = m, concurso = concurso, dataAposta = hoje, dataSorteio = hoje,
            valor = m.precoPadrao, numeros = numeros, criadaEm = agora(),
        )
        return Operacoes.registrarAposta(Operacoes.salvarRascunho(e, a), id, hoje, agora())
    }

    private fun arquivo(nome: String, sha: String = nome) = Arquivo(nome, nome, "application/pdf", agora(), sha256 = sha)

    private fun caixa(m: String, concurso: Int, numeros: String) = """
        LOTERIAS CAIXA
        CAIXA ECONÔMICA FEDERAL
        COMPROVANTE DE APOSTA
        $m
        CONCURSO: $concurso
        Números: $numeros
        Valor da aposta R$ 6,00
    """.trimIndent()

    private fun pix(valor: String, para: String) = """
        Comprovante de transferência
        Pix enviado
        Valor
        R$ $valor
        Destino
        Nome $para
        Instituição NU PAGAMENTOS
        Origem
        Nome Fulano de Tal
        ID da transação E18236120202610271200s0123456789AB
    """.trimIndent()

    @Test
    fun cadaComprovanteVaiParaASuaAposta() {
        var e = inicial()
        e = registrar(e, "ms", Modalidade.MEGA_SENA, 3076, listOf(5, 12, 23, 34, 45, 56))
        e = registrar(e, "qn", Modalidade.QUINA, 7150, listOf(1, 2, 3, 4, 5))
        val r = Associacao.associar(
            e,
            listOf(
                Associacao.Entrada(arquivo("quina.pdf"), caixa("QUINA", 7150, "01 02 03 04 05")),
                Associacao.Entrada(arquivo("mega.pdf"), caixa("MEGA-SENA", 3076, "05 12 23 34 45 56")),
            ),
        )
        assertEquals("quina.pdf", r.estado.apostas.first { it.id == "qn" }.comprovante?.nome)
        assertEquals("mega.pdf", r.estado.apostas.first { it.id == "ms" }.comprovante?.nome)
        assertTrue(r.avisos.all { it.tipo == Associacao.Tipo.ANEXADO })
        assertTrue(r.descartados.isEmpty())
        assertEquals(2, Operacoes.apostasParaConferir(r.estado).size)
        // A conferência cria os comprovantes do bolão, com o da Caixa anexado.
        var aprovado = r.estado
        listOf("ms", "qn").forEach { aprovado = Operacoes.aprovarAposta(aprovado, it, agora()) }
        assertEquals(listOf("mega.pdf", "quina.pdf"), aprovado.comprovantes.map { it.anexos.single().nome })
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(aprovado))
    }

    @Test
    fun outrosNumerosNoComprovanteAlteramOJogo() {
        val e = registrar(inicial(), "ms", Modalidade.MEGA_SENA, 3075, listOf(1, 2, 3, 4, 5, 6))
        val r = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("mega.pdf"), caixa("Mega-Sena", 3076, "10 20 30 40 50 60"))))
        val a = r.estado.apostas.single()
        assertEquals(listOf(10, 20, 30, 40, 50, 60), a.numeros)
        assertEquals(3076, a.concurso)
        assertEquals(Associacao.Tipo.ALTERADA, r.avisos.first().tipo)
        assertTrue(r.avisos.first().texto, r.avisos.first().texto.contains("jogo alterado pelos números do comprovante"))
        assertTrue(a.notasDoAnexo.any { it.startsWith("Números:") })
        // O concurso mudou: o app busca o dia do sorteio na Caixa.
        assertEquals(listOf("ms"), r.concursoMudado)
    }

    @Test
    fun repeticoesSaoDescartadas() {
        var e = inicial()
        e = registrar(e, "a", Modalidade.QUINA, 7150, listOf(1, 2, 3, 4, 5))
        // A mesma aposta registrada duas vezes no app por engano.
        e = registrar(e, "b", Modalidade.QUINA, 7150, listOf(1, 2, 3, 4, 5))
        e = registrar(e, "c", Modalidade.QUINA, 7150, listOf(6, 7, 8, 9, 10))
        val saldoAntes = Operacoes.saldo(e)
        val r = Associacao.associar(
            e,
            listOf(
                Associacao.Entrada(arquivo("q1.pdf", "H1"), caixa("Quina", 7150, "01 02 03 04 05")),
                // O mesmo arquivo escolhido duas vezes.
                Associacao.Entrada(arquivo("q1-copia.pdf", "H1"), caixa("Quina", 7150, "01 02 03 04 05")),
            ),
        )
        val tipos = r.avisos.map { it.tipo }
        assertTrue(tipos.toString(), Associacao.Tipo.ARQUIVO_REPETIDO in tipos)
        assertEquals(listOf("q1-copia.pdf"), r.descartados.map { it.nome })
        // A "b" ficou sem comprovante e é igual à "a", que tem: descartada por repetição, o valor volta.
        assertTrue(r.estado.apostas.none { it.id == "b" })
        assertTrue(tipos.toString(), Associacao.Tipo.APOSTA_REPETIDA in tipos)
        assertEquals(saldoAntes + 300, Operacoes.saldo(r.estado))
        // A "c" continua esperando (e sobe para Nº2).
        assertEquals(listOf("a" to 1, "c" to 2), r.estado.apostas.map { it.id to it.numero })
        assertTrue(r.avisos.any { it.tipo == Associacao.Tipo.AINDA_FALTA && it.texto.contains("Nº2") })

        // Num lote seguinte, o comprovante de uma aposta que já tem o seu é repetido.
        val r2 = Associacao.associar(r.estado, listOf(Associacao.Entrada(arquivo("q1-de-novo.pdf", "H9"), caixa("Quina", 7150, "01 02 03 04 05"))))
        assertEquals(Associacao.Tipo.ARQUIVO_REPETIDO, r2.avisos.first().tipo)
        assertEquals(listOf("q1-de-novo.pdf"), r2.descartados.map { it.nome })
    }

    @Test
    fun comprovanteSemApostaEsperandoEDescartado() {
        val e = registrar(inicial(), "a", Modalidade.QUINA, 7150, listOf(1, 2, 3, 4, 5))
        val r = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("lf.pdf"), caixa("Lotofácil", 3600, "01 02 03 04 05 06 07 08 09 10 11 12 13 14 15"))))
        assertEquals(Associacao.Tipo.SEM_APOSTA, r.avisos.first().tipo)
        assertEquals(listOf("lf.pdf"), r.descartados.map { it.nome })
        val lixo = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("foto.pdf"), "uma foto qualquer")))
        assertEquals(Associacao.Tipo.NAO_LIDO, lixo.avisos.first().tipo)
    }

    @Test
    fun pixVaiParaOIntegranteCerto() {
        var e = Operacoes.registrarRateio(inicial(), hoje, 3000, agora())
        val numero = e.rateios.single().numero
        val r = Associacao.associar(
            e,
            listOf(
                Associacao.Entrada(arquivo("pix-beto.pdf"), pix("10,00", "Beto da Silva")),
                Associacao.Entrada(arquivo("pix-errado.pdf"), pix("9,99", "Beltrano Souza")),
                Associacao.Entrada(arquivo("pix-beltrano.pdf"), pix("10,00", "Beltrano Souza")),
            ),
        )
        e = r.estado
        val rt = e.rateios.single()
        assertEquals("pix-beto.pdf", rt.pix["p2"]?.nome)
        assertEquals("pix-beltrano.pdf", rt.pix["p3"]?.nome)
        assertNull(rt.pix["p1"])
        // Valor que não bate com a cota: comprovante descartado, com o aviso.
        val errado = r.avisos.single { it.tipo == Associacao.Tipo.PIX_VALOR }
        assertTrue(errado.texto, errado.texto.contains("R$ 9,99") && errado.texto.contains("R$ 10,00"))
        assertEquals(listOf("pix-errado.pdf"), r.descartados.map { it.nome })
        assertTrue(r.avisos.any { it.tipo == Associacao.Tipo.AINDA_FALTA && it.texto.contains("Fulano") })
        // O Pix do mesmo integrante de novo: repetido.
        val de = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("pix-beto-2.pdf"), pix("10,00", "Beto da Silva"))))
        assertEquals(Associacao.Tipo.ARQUIVO_REPETIDO, de.avisos.first().tipo)
        // O último Pix completa o rateio: vai para a conferência.
        e = Associacao.associar(e, listOf(Associacao.Entrada(arquivo("pix-fulano.pdf"), pix("10,00", "Fulano de Tal")))).estado
        assertEquals(listOf(numero), Operacoes.rateiosParaConferir(e).map { it.numero })
    }

    @Test
    fun leituraDeVariosJogosEDeModalidades() {
        assertEquals(listOf(Modalidade.MEGA_SENA), LeituraComprovante.modalidades(caixa("MEGA-SENA", 3076, "05 12 23 34 45 56")))
        // "Caixa Econômica Federal" não é a Loteria Federal.
        assertNull(LeituraComprovante.modalidadeDaLinha("CAIXA ECONÔMICA FEDERAL"))
        assertEquals(Modalidade.FEDERAL, LeituraComprovante.modalidadeDaLinha("Loteria Federal - extração 6012"))
        assertEquals(Modalidade.MAIS_MILIONARIA, LeituraComprovante.modalidadeDaLinha("+Milionária 300"))
        assertEquals(Modalidade.LOTOFACIL, LeituraComprovante.modalidadeDaLinha("LOTOFÁCIL"))
        // Um comprovante com dois jogos de modalidades diferentes.
        val carrinho = "Comprovante do pedido\nMega-Sena\nConcurso 3076\nNúmeros: 05 12 23 34 45 56\nQuina\nConcurso 7150\nNúmeros: 01 02 03 04 05"
        val secoes = LeituraComprovante.secoes(carrinho)
        assertEquals(listOf(Modalidade.MEGA_SENA, Modalidade.QUINA), secoes.map { it.first })
        assertEquals(3076, LeituraComprovante.ler(Modalidade.MEGA_SENA, secoes[0].second).concurso)
        assertEquals(7150, LeituraComprovante.ler(Modalidade.QUINA, secoes[1].second).concurso)
        // Dois jogos da mesma modalidade no mesmo comprovante.
        val dois = LeituraComprovante.lerApostas(Modalidade.QUINA, "Quina\nConcurso 7150\nNúmeros: 01 02 03 04 05\nNúmeros: 06 07 08 09 10")
        assertEquals(listOf(listOf(1, 2, 3, 4, 5), listOf(6, 7, 8, 9, 10)), dois.map { it.numeros })
        assertTrue(dois.all { it.concurso == 7150 })
    }

    @Test
    fun leituraDoPix() {
        val texto = pix("1.234,56", "Beto da Silva")
        assertTrue(LeituraComprovante.ehPix(texto))
        assertFalse(LeituraComprovante.ehPix(caixa("Quina", 7150, "01 02 03 04 05")))
        val p = LeituraComprovante.lerPix(texto)
        assertEquals(123_456L, p.valor)
        assertTrue(p.destino, p.destino.contains("BETO DA SILVA"))
        assertFalse(p.destino, p.destino.contains("FULANO"))
        assertTrue(p.codigo.orEmpty().startsWith("E18236120"))
        assertTrue(LeituraComprovante.pontosDoNome(p.destino, "Beto da Silva", "Beto", "beto@x") > 0)
        assertEquals(0, LeituraComprovante.pontosDoNome(p.destino, "Fulano de Tal", "Fulano", "fulano@x"))
        // O comprovante de exemplo do modo de teste.
        val exemplo = "EXEMPLO\nNão é um comprovante real\nPix de R$ 8,00\nComprovante de Pix\nValor: R$ 8,00\nPara: Beto\nChave: beto@exemplo.com"
        assertTrue(LeituraComprovante.ehPix(exemplo))
        assertEquals(800L, LeituraComprovante.lerPix(exemplo).valor)
    }
}

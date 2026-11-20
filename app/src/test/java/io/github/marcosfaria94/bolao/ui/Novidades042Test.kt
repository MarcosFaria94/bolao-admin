package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Base
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Etapa
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Formato
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Garantia
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Modo as ModoDeCalcular
import io.github.marcosfaria94.bolao.domain.CalculoEmEtapas.Pedido
import io.github.marcosfaria94.bolao.domain.Efetivacao
import io.github.marcosfaria94.bolao.domain.Efetivacao.Situacao
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Transcricao
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.ui.caixa.nomeDoBotaoDeEfetivar
import io.github.marcosfaria94.bolao.ui.jogos.nomeDaModalidadeNaCalculadora
import io.github.marcosfaria94.bolao.ui.jogos.nomeDoPasso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** As novidades da 0.42 que dão para testar sem as telas. */
class Novidades042Test {

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    private val mega = Modalidade.MEGA_SENA

    // ---------- a calculadora em etapas ----------

    @Test
    fun etapasDeCadaModo() {
        assertEquals(listOf(Etapa.FAIXA, Etapa.CHANCE), CalculoEmEtapas.etapas(ModoDeCalcular.PROBABILIDADE))
        assertEquals(listOf(Etapa.QUANTIDADE, Etapa.TIPO), CalculoEmEtapas.etapas(ModoDeCalcular.JOGOS))
        // Desde a 0.43 a quantidade de prêmios é o desdobramento pelos prêmios.
        assertEquals(listOf(Etapa.BASE, Etapa.FAIXA, Etapa.PREMIOS), CalculoEmEtapas.etapas(ModoDeCalcular.DESDOBRAMENTO, Base.PREMIOS))
        assertEquals(listOf(Etapa.VALOR, Etapa.TIPO), CalculoEmEtapas.etapas(ModoDeCalcular.ORCAMENTO))
        assertEquals(listOf(Etapa.NUMEROS, Etapa.GARANTIA), CalculoEmEtapas.etapas(ModoDeCalcular.FECHAMENTO))
        assertEquals(listOf(Etapa.BASE, Etapa.NUMEROS), CalculoEmEtapas.etapas(ModoDeCalcular.DESDOBRAMENTO))
        // Desde a 0.43 os botões com a palavra inteira (e na ordem do pedido do usuário).
        assertEquals(listOf("Apostas", "Chance", "Valor", "Fechamento", "Desdobramento", "Empacotamento"), ModoDeCalcular.entries.map { it.titulo })
        // A barra de cima: como calcular, as etapas e o resultado.
        assertEquals(4, CalculoEmEtapas.passos(Pedido(modo = ModoDeCalcular.PROBABILIDADE)))
        assertEquals(4, CalculoEmEtapas.passos(Pedido(modo = ModoDeCalcular.DESDOBRAMENTO)))
        assertEquals(3, CalculoEmEtapas.passos(Pedido()))
        assertEquals("Etapa 2: Faixa", nomeDoPasso(2, "Faixa"))
        assertEquals("Calcular na Quina", nomeDaModalidadeNaCalculadora(Modalidade.QUINA))
    }

    @Test
    fun andarEVoltarNasEtapas() {
        var p = Pedido()
        assertEquals(0, CalculoEmEtapas.etapaAtual(mega, p))
        p = CalculoEmEtapas.comModo(p, ModoDeCalcular.PROBABILIDADE)
        assertEquals(1, CalculoEmEtapas.etapaAtual(mega, p))
        p = CalculoEmEtapas.comFaixa(mega, p, 0)
        assertEquals(2, CalculoEmEtapas.depoisDe(mega, p, 1))
        p = p.copy(valor = "1000000")
        assertEquals(3, CalculoEmEtapas.etapaAtual(mega, p))
        // Voltar e trocar a faixa com a chance já digitada vai direto ao resultado.
        p = CalculoEmEtapas.comFaixa(mega, p, 1)
        assertEquals(3, CalculoEmEtapas.depoisDe(mega, p, 1))
        // O mesmo modo mantém as escolhas; outro começa do zero (com o jeito da chance).
        assertEquals(p, CalculoEmEtapas.comModo(p, ModoDeCalcular.PROBABILIDADE))
        val outro = CalculoEmEtapas.comModo(p.copy(formato = Formato.PORCENTO), ModoDeCalcular.JOGOS)
        assertEquals(Pedido(modo = ModoDeCalcular.JOGOS, formato = Formato.PORCENTO), outro)
    }

    @Test
    fun trocarDeModalidadeNoMeio() {
        val p = Pedido(modo = ModoDeCalcular.PROBABILIDADE, faixa = 0, valor = "1000000")
        // Na Quina a escolha continua valendo (a faixa principal e a chance).
        assertEquals(p, CalculoEmEtapas.naModalidade(p, mega, Modalidade.QUINA))
        // A faixa vai pelo nome: a quina da Mega é a principal da Quina; a quadra, a quadra.
        assertEquals(0, CalculoEmEtapas.naModalidade(p.copy(faixa = 1), mega, Modalidade.QUINA).faixa)
        assertEquals(1, CalculoEmEtapas.naModalidade(p.copy(faixa = 2), mega, Modalidade.QUINA).faixa)
        assertEquals(Calculadora.QUALQUER_PREMIO, CalculoEmEtapas.naModalidade(p.copy(faixa = Calculadora.QUALQUER_PREMIO), mega, Modalidade.LOTOFACIL).faixa)
        // Sem o mesmo nome e fora da principal: a etapa da faixa pergunta de novo.
        assertNull(CalculoEmEtapas.naModalidade(p.copy(faixa = 1), Modalidade.LOTOFACIL, mega).faixa)
        // A quantidade de quadras não existe na Lotomania: começa de novo.
        assertEquals(Pedido(), CalculoEmEtapas.naModalidade(Pedido(modo = ModoDeCalcular.DESDOBRAMENTO, base = Base.PREMIOS, faixa = 2, valor = "10"), mega, Modalidade.LOTOMANIA))
        // 6 números de jogo na Mega viram nada na Lotofácil (não existe aposta de 6): o tipo sai.
        val j = CalculoEmEtapas.naModalidade(Pedido(modo = ModoDeCalcular.JOGOS, valor = "20", numeros = 6, trevos = 0), mega, Modalidade.LOTOFACIL)
        assertNull(j.numeros)
        assertEquals("20", j.valor)
        // Os números do desdobramento fora do limite da outra modalidade saem.
        assertEquals("", CalculoEmEtapas.naModalidade(Pedido(modo = ModoDeCalcular.DESDOBRAMENTO, base = Base.NUMEROS, valor = "20"), mega, Modalidade.QUINA).valor)
        assertNotNull(CalculoEmEtapas.motivoSemModo(Modalidade.LOTOMANIA, ModoDeCalcular.DESDOBRAMENTO))
        assertNotNull(CalculoEmEtapas.motivoSemModo(Modalidade.SUPER_SETE, ModoDeCalcular.FECHAMENTO))
        assertNull(CalculoEmEtapas.motivoSemModo(mega, ModoDeCalcular.FECHAMENTO))
    }

    @Test
    fun errosDoNumeroDigitado() {
        fun erro(p: Pedido) = CalculoEmEtapas.erroDoValor(mega, p)
        val chance = Pedido(modo = ModoDeCalcular.PROBABILIDADE, faixa = 1)
        assertNull(erro(chance.copy(valor = "1.000.000")))
        assertEquals(1_000_000.0, CalculoEmEtapas.numero("1.000.000")!!, 0.0)
        assertEquals(12.5, CalculoEmEtapas.numero("12,5")!!, 0.0)
        assertNotNull(erro(chance.copy(valor = "1")))
        assertNotNull(erro(chance.copy(formato = Formato.PORCENTO, valor = "100")))
        assertNull(erro(chance.copy(faixa = 0, formato = Formato.PORCENTO, valor = "100")))
        assertNotNull(erro(Pedido(modo = ModoDeCalcular.JOGOS, valor = "2,5")))
        assertNotNull(erro(Pedido(modo = ModoDeCalcular.FECHAMENTO, valor = "15")))
        assertNull(erro(Pedido(modo = ModoDeCalcular.FECHAMENTO, valor = "14")))
        // Nem com 20 números há 1 milhão de quadras.
        assertTrue(erro(Pedido(modo = ModoDeCalcular.DESDOBRAMENTO, base = Base.PREMIOS, faixa = 2, valor = "1000000"))!!.startsWith("Nem com 20 números"))
    }

    @Test
    fun premiosDoMinimoAoMaximo() {
        // 7 números na Mega: acertando 4, 3 quadras; acertando 5, 5 quadras; acertando 6, nenhuma (são senas e quinas).
        assertEquals(3L..5L, CalculoEmEtapas.premiosDaFaixa(mega, 7, 2))
        assertEquals(3L..12L, CalculoEmEtapas.premiosDaFaixa(mega, 8, 1))
        assertEquals(1L..1L, CalculoEmEtapas.premiosDaFaixa(mega, 6, 2))
        assertNull(CalculoEmEtapas.premiosDaFaixa(Modalidade.LOTOMANIA, 50, 0))
        // 10 quadras pedem 9 números (com 8, só 6).
        assertEquals(9, CalculoEmEtapas.tipoParaPremios(mega, 2, 10))
        val r = CalculoEmEtapas.resultado(mega, Pedido(modo = ModoDeCalcular.DESDOBRAMENTO, base = Base.PREMIOS, faixa = 2, valor = "10"), 600)!!
        assertEquals(84 * 600L, r.custo)
        assertEquals("1 aposta de 9 números = 84 jogos simples", r.tipo)
        assertEquals(10L..45L, r.premios!![2])
        assertEquals(2, r.destaque)
    }

    @Test
    fun jogosOrcamentoEProbabilidade() {
        val jogos = CalculoEmEtapas.resultado(mega, Pedido(modo = ModoDeCalcular.JOGOS, valor = "20", numeros = 6, trevos = 0), 600)!!
        assertEquals(20 * 600L, jogos.custo)
        assertEquals(20L, jogos.jogos)
        // R$ 100 em apostas simples de R$ 6: 16 jogos e sobram R$ 4.
        val o = CalculoEmEtapas.opcoesDeTipo(mega, Pedido(modo = ModoDeCalcular.ORCAMENTO, valor = "100"), 600)
        assertEquals(16L, o.first().jogos)
        assertEquals(400L, o.first().sobra)
        assertEquals(0L, o.first { it.linha.numeros == 20 }.jogos)
        val orc = CalculoEmEtapas.resultado(mega, Pedido(modo = ModoDeCalcular.ORCAMENTO, valor = "100", numeros = 6, trevos = 0), 600)!!
        assertEquals(96 * 100L, orc.custo)
        assertEquals(400L, orc.sobra)
        // A chance pedida da sena: pelo menos 1 em 1.000.000.
        val p = CalculoEmEtapas.resultado(mega, Pedido(modo = ModoDeCalcular.PROBABILIDADE, faixa = 0, valor = "1000000"), 600)!!
        assertTrue(p.chances.first { it.indice == 0 }.p >= 1e-6)
        assertEquals(0, p.destaque)
        // Falta escolha: sem resultado.
        assertNull(CalculoEmEtapas.resultado(mega, Pedido(modo = ModoDeCalcular.JOGOS, valor = "20"), 600))
    }

    @Test
    fun fechamentoComGarantia() {
        assertEquals(7..14, CalculoEmEtapas.limitesDoFechamento(mega))
        assertEquals(16..19, CalculoEmEtapas.limitesDoFechamento(Modalidade.LOTOFACIL))
        assertNull(CalculoEmEtapas.limitesDoFechamento(Modalidade.LOTOMANIA))
        assertNull(CalculoEmEtapas.limitesDoFechamento(Modalidade.MAIS_MILIONARIA))
        // Quadra garantida se 5 dos sorteados estiverem entre os 10 números: todo grupo de 5 tem 4 num jogo.
        val g = Garantia(4, 5)
        val jogos = CalculoEmEtapas.fechamento(10, 6, g)!!
        assertTrue("${jogos.size} jogos", jogos.size in 5..12)
        assertTrue(jogos.all { it.size == 6 && it.toSet().size == 6 })
        fun grupos(n: Int, k: Int, ini: Int = 0): List<List<Int>> =
            if (k == 0) listOf(emptyList()) else (ini..n - k).flatMap { i -> grupos(n, k - 1, i + 1).map { listOf(i) + it } }
        for (gr in grupos(10, 5)) assertTrue("$gr", jogos.any { j -> j.count { it in gr } >= 4 })
        // A garantia de todos do jogo é o desdobramento.
        assertEquals(7, CalculoEmEtapas.fechamento(7, 6, Garantia(6, 6))!!.size)
        // A chance da condição: os 6 sorteados entre os 6 números = 1 aposta simples.
        assertEquals(1.0 / 50_063_860, CalculoEmEtapas.chanceDaCondicao(mega, 6, 6), 1e-15)
        val r = CalculoEmEtapas.resultado(mega, Pedido(modo = ModoDeCalcular.FECHAMENTO, valor = "10", garantia = g), 600, jogos.size)!!
        assertEquals(jogos.size * 600L, r.custo)
        assertEquals(g, r.garantia)
        assertEquals("Pelo menos 1 quadra se 5 dos 6 sorteados estiverem entre os seus 10 números.", CalculoEmEtapas.textoDaGarantia(mega, g, 10))
        assertEquals("Quadra · se sair 5", CalculoEmEtapas.nomeDaGarantia(mega, g))
        // As garantias com 10 números: quadra (se sair 4, 5 ou 6), quina (5 ou 6), sena (6).
        assertEquals(listOf(4 to 3, 5 to 2, 6 to 1), CalculoEmEtapas.garantias(mega, 10).map { it.first to it.second.size }.sortedBy { it.first })
    }

    @Test
    fun desdobramento() {
        val r = CalculoEmEtapas.resultado(mega, Pedido(modo = ModoDeCalcular.DESDOBRAMENTO, base = Base.NUMEROS, valor = "10"), 600)!!
        assertEquals(210L, r.jogos)
        assertEquals(210 * 600L, r.custo)
        assertEquals(Volante.combinacoes(10, 6), CalculoEmEtapas.desdobramento((0 until 10).toList(), 6, 1000).size.toLong())
        assertEquals(60, CalculoEmEtapas.desdobramento((0 until 10).toList(), 6, 60).size)
    }

    // ---------- o "Efetivar apostas" ----------

    private val hoje = LocalDate.of(2026, 11, 20)

    private fun aposta(m: Modalidade, numeros: List<Int>, numero: Int, concurso: Int, sorteio: LocalDate) = Aposta(
        id = "a$numero", numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje, dataSorteio = sorteio, valor = 600,
        numeros = numeros, criadaEm = numero.toLong(), registrada = true, pendenteDeAnexo = true,
    )

    private fun estado(vararg apostas: Aposta) =
        Estado(cadastroConcluido = true, config = Config(parametros = Parametros()), apostas = apostas.toList(), modo = Modo.BOLAO)

    private val e = estado(
        aposta(Modalidade.QUINA, listOf(5, 17, 33, 41, 70), 3, 6900, hoje.plusDays(1)),
        aposta(mega, listOf(1, 2, 3, 4, 5, 6), 1, 2950, hoje.plusDays(2)),
        aposta(mega, listOf(7, 8, 9, 10, 11, 12), 2, 2950, hoje.plusDays(2)),
    )

    @Test
    fun listaSeparadaPorModalidadeEConcurso() {
        val lista = Efetivacao.lista(e, hoje)
        assertEquals(3, lista.size)
        val g = Efetivacao.grupos(lista)
        assertEquals(listOf(mega to 2950, Modalidade.QUINA to 6900), g.map { it.modalidade to it.concurso })
        assertEquals(listOf("Aposta Nº1", "Aposta Nº2"), g[0].linhas.map { it.item.titulo })
        assertEquals("Dom 22/11", Efetivacao.dia(g[0].sorteio))
    }

    @Test
    fun preSelecionadasESituacoes() {
        val lista = Efetivacao.lista(e, hoje)
        // Tudo marcado, menos a que já está no carrinho.
        assertEquals(setOf("a1", "a2", "a3"), Efetivacao.preSelecionadas(lista, emptyList(), emptyMap()))
        assertEquals(setOf("a2", "a3"), Efetivacao.preSelecionadas(lista, listOf("a1"), emptyMap()))
        val a1 = lista.first { it.chave == "a1" }
        assertEquals(Situacao.NoCarrinho, Efetivacao.situacao(a1, listOf("a1"), emptyMap()))
        assertEquals(Situacao.NaoEfetivada, Efetivacao.situacao(a1, emptyList(), emptyMap()))
        // A situação do lote vale mais (a que não foi continua marcada).
        val falhou = Situacao.Falhou("A sessão expirou", sessao = true)
        assertEquals(falhou, Efetivacao.situacao(a1, listOf("a1"), mapOf("a1" to falhou)))
        assertEquals(setOf("a1", "a2", "a3"), Efetivacao.preSelecionadas(lista, listOf("a1"), mapOf("a1" to falhou)))
        assertEquals("Sessão expirada", Efetivacao.etiqueta(falhou))
        assertEquals("Não efetivada", Efetivacao.etiqueta(Situacao.NaoEfetivada))
        assertEquals("No carrinho", Efetivacao.etiqueta(Situacao.NoCarrinho))
        assertEquals(2, Efetivacao.pendentes(lista, listOf("a1")))
    }

    @Test
    fun loteEResumo() {
        val lista = Efetivacao.lista(e, hoje)
        val lote = Efetivacao.lote(lista, setOf("a2", "a3"))
        assertEquals(listOf(mega, Modalidade.QUINA), lote.map { it.first.modalidade })
        assertEquals(listOf("a2"), lote.first().second.flatMap { it.ids })
        assertEquals("2 de 3 marcadas · R$ 12,00", Efetivacao.resumo(lista, setOf("a2", "a3")))
        assertEquals("Efetivar 1 aposta", Efetivacao.textoDoBotao(1))
        assertEquals("Efetivar 3 apostas", Efetivacao.textoDoBotao(3))
        assertEquals("Efetivar apostas: 3 para efetivar", nomeDoBotaoDeEfetivar(3))
        assertEquals("Efetivar apostas: nenhuma para efetivar", nomeDoBotaoDeEfetivar(0))
    }

    @Test
    fun sessaoDoSite() {
        assertEquals(Efetivacao.Sessao.LOGADA, Efetivacao.sessao("logado"))
        assertEquals(Efetivacao.Sessao.DESLOGADA, Efetivacao.sessao("deslogado"))
        assertEquals(Efetivacao.Sessao.DESCONHECIDA, Efetivacao.sessao(null))
        val r = Transcricao.resposta("""{"ok":false,"noCarrinho":false,"sessao":true,"mensagem":"A sessão do site expirou"}""")!!
        assertTrue(r.sessao)
        assertFalse(r.noCarrinho)
        assertFalse(Transcricao.resposta("""{"ok":true,"noCarrinho":true,"mensagem":"x"}""")!!.sessao)
    }

    /** O script só conta a aposta no carrinho com o contador do site subindo (o volante limpo não basta). */
    @Test
    fun carrinhoSoComOContador() {
        val js = arquivo("src/main/assets/caixa/transcrever.js", "app/src/main/assets/caixa/transcrever.js").readText()
        assertFalse(js.contains("marcados().length === 0 || !!modalAberto() || entrou()"))
        assertTrue(js.contains("n > antes"))
        assertTrue(js.contains("sessao: !!e.sessao"))
        assertTrue(js.contains("function Sessao("))
        assertFalse(js.contains("Transcrever de novo"))
        val sessao = arquivo("src/main/assets/caixa/sessao.js", "app/src/main/assets/caixa/sessao.js").readText()
        assertTrue(sessao.contains("'logado'") && sessao.contains("'deslogado'") && sessao.contains("'desconhecido'"))
        val cookies = arquivo("src/main/assets/caixa/cookies.js", "app/src/main/assets/caixa/cookies.js").readText()
        assertTrue(cookies.contains("bolao-efetivar") && cookies.contains("BolaoCadeado.efetivar") && cookies.contains("__bolaoEfetivarEstado"))
    }

    /** A barra de cima da Caixa sem o recarregar e sem o "Transcrever em lote"; o puxão do Painel desligado na Caixa. */
    @Test
    fun barraDaCaixa() {
        val raiz = arquivo("src/main/java/io/github/marcosfaria94/bolao/ui/AppRaiz.kt", "app/src/main/java/io/github/marcosfaria94/bolao/ui/AppRaiz.kt").readText()
        assertFalse(raiz.contains("Recarregar a página"))
        assertFalse(raiz.contains("TRANSCREVER_EM_LOTE"))
        assertTrue(raiz.contains("abaDa(paginas.settledPage) != Aba.CAIXA"))
        assertEquals("Qualquer prêmio", Calculadora.NOME_QUALQUER)
    }
}

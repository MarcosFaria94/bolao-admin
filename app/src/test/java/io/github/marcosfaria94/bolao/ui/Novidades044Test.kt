package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.PrecosDosConcursos
import io.github.marcosfaria94.bolao.domain.Transcricao
import io.github.marcosfaria94.bolao.ui.caixa.FimDoLote
import io.github.marcosfaria94.bolao.ui.caixa.PosicaoDoCookie
import io.github.marcosfaria94.bolao.ui.caixa.ROTULO_CAIXA
import io.github.marcosfaria94.bolao.ui.caixa.TITULO_CAIXA
import io.github.marcosfaria94.bolao.ui.caixa.ehParaBaixo
import io.github.marcosfaria94.bolao.ui.caixa.lerCookie
import io.github.marcosfaria94.bolao.ui.caixa.lugarDoCadeado
import io.github.marcosfaria94.bolao.ui.caixa.mensagemDaPaginaQueNaoAbriu
import io.github.marcosfaria94.bolao.ui.caixa.textoDoFimDoLote
import io.github.marcosfaria94.bolao.ui.painel.ROTA_RASCUNHOS
import io.github.marcosfaria94.bolao.ui.painel.TipoDoResumo
import io.github.marcosfaria94.bolao.ui.painel.gruposDosRascunhos
import io.github.marcosfaria94.bolao.ui.painel.impedimentoDoRascunho
import io.github.marcosfaria94.bolao.ui.painel.mensagemDoLoteDeRascunhos
import io.github.marcosfaria94.bolao.ui.painel.resumoDasPendencias
import io.github.marcosfaria94.bolao.ui.painel.resumoDosRascunhos
import io.github.marcosfaria94.bolao.ui.painel.textoDoLoteDeRascunhos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * 0.44: o "Efetivar apostas" na Quina Friday (outra página e outro preço), os botões da página da Loteria (o cadeado na
 * mesma altura do ícone dos cookies, sempre na página, o puxão do Painel), os Rascunhos (vários por modalidade, o lote, a
 * exclusão quando o concurso vira) e as Apostas não efetivadas (o "Confirmar" só depois de uma efetivação que falhou).
 */
class Novidades044Test {

    private val hoje = LocalDate.of(2026, 11, 25)

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    private fun aposta(
        id: String,
        numero: Int = 0,
        m: Modalidade = Modalidade.QUINA,
        concurso: Int = 7173,
        valor: Long = 300,
        numeros: List<Int> = listOf(25, 41, 44, 63, 72),
        registrada: Boolean = false,
        pendente: Boolean = registrada,
        criadaEm: Long = 1,
    ) = Aposta(
        id = id, numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje, dataSorteio = LocalDate.of(2026, 11, 29),
        valor = valor, numeros = numeros, criadaEm = criadaEm, registrada = registrada, pendenteDeAnexo = pendente,
    )

    private fun estado(vararg apostas: Aposta, parametros: Parametros = Parametros()) = Estado(
        cadastroConcluido = true, cadastradoEm = hoje.withDayOfMonth(1), config = Config(parametros = parametros),
        pessoas = listOf(Pessoa("p1", "Fulano", "Fulano", "f@x"), Pessoa("p2", "Beltrano", "Beltrano", "b@x")),
        apostas = apostas.toList(),
        lancamentos = listOf(Lancamento("ap", hoje.withDayOfMonth(1), 1, TipoLancamento.APORTE, 20000)) +
            apostas.filter { it.registrada }.map { Lancamento("l-${it.id}", hoje, 10L + it.numero, TipoLancamento.APOSTA, -it.valor, apostaId = it.id) },
    )

    @Test
    fun quinaFridayComOutroPreco() {
        assertEquals(250L, PrecosDosConcursos.simples(Modalidade.QUINA, 7173))
        assertNull(PrecosDosConcursos.simples(Modalidade.QUINA, 7174))
        assertNull(PrecosDosConcursos.simples(Modalidade.MEGA_SENA, 7173))
        val e = estado()
        assertEquals(250L, Operacoes.precoSimples(e, Modalidade.QUINA, 7173))
        assertEquals(e.config.preco(Modalidade.QUINA), Operacoes.precoSimples(e, Modalidade.QUINA, 7174))
        // A aposta Nº26 da foto (R$ 3,00, registrada e ainda não efetivada) passa a R$ 2,50, com o lançamento dela.
        val n26 = aposta("n26", numero = 26, registrada = true)
        val seis = aposta("seis", numero = 27, registrada = true, numeros = listOf(1, 2, 3, 4, 5, 6), valor = 1800)
        val confirmada = aposta("ok", numero = 28, registrada = true, pendente = false)
        val rascunhoQueSaiu = aposta("r", concurso = 7174, valor = 250)
        val depois = Operacoes.corrigirPrecos(estado(n26, seis, confirmada, rascunhoQueSaiu))
        assertEquals(250L, depois.apostas.first { it.id == "n26" }.valor)
        assertEquals(-250L, depois.lancamentos.first { it.apostaId == "n26" }.valor)
        // 6 números: 6 apostas simples de R$ 2,50.
        assertEquals(1500L, depois.apostas.first { it.id == "seis" }.valor)
        // A já confirmada não muda; o rascunho que passou para o 7174 volta ao preço de Ajustes.
        assertEquals(300L, depois.apostas.first { it.id == "ok" }.valor)
        assertEquals(e.config.preco(Modalidade.QUINA), depois.apostas.first { it.id == "r" }.valor)
        // Nada a corrigir: o mesmo estado.
        val certo = estado(aposta("x", valor = 250))
        assertTrue(Operacoes.corrigirPrecos(certo) === certo)
    }

    @Test
    fun paginaDoEfetivar() {
        // O endereço da página e, se não abrir, o da outra página da modalidade.
        assertEquals(listOf("/quina", "/quina/especial"), Transcricao.fragmentos(Transcricao.Pagina(Modalidade.QUINA, false)))
        assertEquals(listOf("/mega-sena/especial", "/mega-sena"), Transcricao.fragmentos(Transcricao.Pagina(Modalidade.MEGA_SENA, true)))
        assertTrue(Transcricao.fragmentos(Transcricao.Pagina(Modalidade.FEDERAL, false)).isEmpty())
        // A Quina Friday com o nome no endereço é a página especial da Quina.
        assertEquals(Transcricao.Pagina(Modalidade.QUINA, true), Transcricao.pagina(Transcricao.ENDERECO + "#/quina-friday"))
        assertEquals(Transcricao.Pagina(Modalidade.QUINA, true), Transcricao.pagina(Transcricao.ENDERECO + "#/quina/especial"))
        // O link da modalidade na página (o último recurso).
        val script = Transcricao.scriptDoLink(Transcricao.Pagina(Modalidade.MAIS_MILIONARIA, false))
        assertTrue(script, script.contains("\"maismilionaria\"") && script.contains("\"milionaria\""))
        assertEquals(
            "Não consegui abrir a página da Quina no site (ele ficou em #/home). Tente de novo.",
            mensagemDaPaginaQueNaoAbriu("Quina", "#/home"),
        )
        assertEquals("Não consegui abrir a página da Quina no site. Tente de novo.", mensagemDaPaginaQueNaoAbriu("Quina", null))
        // O plano confere o concurso só quando o app chegou à outra página da modalidade.
        assertTrue(Transcricao.json(Transcricao.plano(aposta("a"))!!).contains("\"conferirConcurso\":false"))
        val js = arquivo("src/main/assets/caixa/transcrever.js", "app/src/main/assets/caixa/transcrever.js").readText()
        assertTrue(js.contains("plano.conferirConcurso") && js.contains("conferirConcurso();"))
    }

    @Test
    fun efetivacaoQueFalhou() {
        val a = aposta("a", numero = 1, registrada = true)
        val b = aposta("b", numero = 2, registrada = true)
        val e = estado(a, b)
        val depois = Operacoes.guardarEfetivacao(e, mapOf("a" to "Não consegui abrir a página da Quina no site. Tente de novo."), listOf("b"))
        assertEquals(listOf("a"), Operacoes.apostasComFalhaNaEfetivacao(depois).map { it.id })
        // Na volta (foi para o carrinho), a nota sai; a confirmação também tira.
        assertTrue(Operacoes.apostasComFalhaNaEfetivacao(Operacoes.guardarEfetivacao(depois, emptyMap(), listOf("a"))).isEmpty())
        val aprovada = Operacoes.aprovarAposta(depois, "a", 100)
        assertNull(aprovada.apostas.first { it.id == "a" }.falhaNaEfetivacao)
        // Sem nada a guardar: o mesmo estado.
        assertTrue(Operacoes.guardarEfetivacao(e, emptyMap(), listOf("b")) === e)
        assertEquals("2 no carrinho (2 apostas confirmadas no app) · 1 não foi: continua marcada para tentar de novo.", textoDoFimDoLote(FimDoLote(2, 2, 1)))
        assertEquals("1 no carrinho (1 aposta confirmada no app).", textoDoFimDoLote(FimDoLote(1, 1, 0)))
        assertEquals("2 não foram: continuam marcadas para tentar de novo.", textoDoFimDoLote(FimDoLote(0, 0, 2)))
        assertNull(textoDoFimDoLote(FimDoLote(0, 0, 0)))
    }

    @Test
    fun rascunhos() {
        // Vários rascunhos da mesma modalidade: o lote registra todos, na ordem dada.
        val r1 = aposta("r1", concurso = 7174, criadaEm = 1)
        val r2 = aposta("r2", concurso = 7174, numeros = listOf(1, 2, 3, 4, 5), criadaEm = 2)
        val mega = aposta("m", m = Modalidade.MEGA_SENA, concurso = 3091, numeros = listOf(1, 2, 3, 4, 5, 6), valor = 600, criadaEm = 3)
        val e = estado(r1, r2, mega)
        val (depois, feitas) = Operacoes.registrarEmLote(e, listOf("r1", "r2", "m"), hoje, 1000)
        assertEquals(listOf("r1", "r2", "m"), feitas.map { it.id })
        assertEquals(listOf(1, 2, 3), feitas.map { it.numero })
        assertTrue(feitas.all { it.registrada && it.pendenteDeAnexo })
        assertTrue(Operacoes.rascunhos(depois).isEmpty())
        // Com a teimosinha desligada nos Parâmetros, o rascunho vale só no concurso dele.
        val teimoso = aposta("t", concurso = 7174).copy(teimosinha = 3)
        val semTeimosinha = estado(teimoso, parametros = Parametros(teimosinha = false))
        assertEquals(1, Operacoes.registrarEmLote(semTeimosinha, listOf("t"), hoje, 1000).second.size)
        assertEquals(3, Operacoes.registrarEmLote(estado(teimoso), listOf("t"), hoje, 1000).second.size)
        // A lista: por modalidade (na ordem do app) e concurso; os mais novos primeiro.
        val grupos = gruposDosRascunhos(listOf(r1, mega, r2))
        assertEquals(listOf(Modalidade.MEGA_SENA, Modalidade.QUINA), grupos.map { it.modalidade })
        assertEquals(listOf("r2", "r1"), grupos[1].rascunhos.map { it.id })
        // O que impede o registro.
        assertNull(impedimentoDoRascunho(r1, sorteado = false))
        assertEquals("O concurso 7174 já foi sorteado e o resultado ainda não chegou.", impedimentoDoRascunho(r1, sorteado = true))
        assertEquals("Falta completar o volante.", impedimentoDoRascunho(r1.copy(numeros = listOf(1, 2)), sorteado = false))
        assertEquals("Registrar 3 apostas em lote", textoDoLoteDeRascunhos(3))
        assertEquals("Registrar 1 aposta", textoDoLoteDeRascunhos(1))
        assertEquals("2 de 3 marcados · R$ 9,00", resumoDosRascunhos(3, 2, 900))
        assertEquals("Apostas Nº1 a Nº3 registradas: efetive na aba Loteria.", mensagemDoLoteDeRascunhos(feitas, individual = false))
        assertEquals("Aposta Nº1 registrada.", mensagemDoLoteDeRascunhos(feitas.take(1), individual = true))
        // O resumo do Painel: "Rascunhos" abre a tela dos rascunhos; "Apostas não efetivadas", os Comprovantes.
        val resumo = resumoDasPendencias(e, Pendencias.de(e, hoje.atTime(12, 0)))
        assertEquals(ROTA_RASCUNHOS, resumo.first { it.tipo == TipoDoResumo.REGISTRAR }.destino)
        assertEquals("Rascunhos", TipoDoResumo.REGISTRAR.rotulo)
        assertEquals("Apostas não efetivadas", TipoDoResumo.CONFIRMAR.rotulo)
    }

    @Test
    fun excluirRascunhoQuandoOConcursoVira() {
        val vencido = aposta("v", concurso = 7172)
        val doProximo = aposta("p", concurso = 7173)
        val registrada = aposta("x", numero = 1, concurso = 7172, registrada = true, pendente = false)
        val proximos = mapOf(Modalidade.QUINA to Operacoes.ProximoConcurso(7173, null, null))
        // Desligado (o padrão): nada sai (o rascunho passa para o próximo concurso, no atualizarConcursos).
        val desligado = estado(vencido, doProximo, registrada)
        assertFalse(desligado.parametros.excluirRascunhosVencidos)
        assertTrue(Operacoes.excluirRascunhosVencidos(desligado, proximos).second.isEmpty())
        // Ligado: só o rascunho vencido sai.
        val ligado = estado(vencido, doProximo, registrada, parametros = Parametros(excluirRascunhosVencidos = true))
        val (depois, fora) = Operacoes.excluirRascunhosVencidos(ligado, proximos)
        assertEquals(listOf("v"), fora.map { it.id })
        assertEquals(listOf("p", "x"), depois.apostas.map { it.id })
        assertEquals("Rascunho da Quina excluído: o concurso 7172 já foi sorteado.", avisoDosRascunhosExcluidos(fora))
        assertEquals("2 rascunhos excluídos: os concursos deles já foram sorteados.", avisoDosRascunhosExcluidos(listOf(vencido, vencido)))
    }

    @Test
    fun botoesDaPaginaDaLoteria() {
        assertEquals("Loteria", ROTULO_CAIXA)
        assertEquals("Loteria", TITULO_CAIXA)
        val d = 3f
        val largura = 1140f
        val altura = 2000f
        // O ícone dos cookies à esquerda (81 px da borda e de baixo, 141 px): o cadeado espelhado, na mesma altura e tamanho.
        val esquerda = PosicaoDoCookie(x = (81f + 70.5f) / largura, topo = (2000f - 81f - 141f) / altura, largura = 141f / largura, altura = 141f / altura, direita = false)
        val l = lugarDoCadeado(esquerda, largura, altura, d)
        assertEquals(141f, l.tamanho, 0.5f)
        assertEquals(81f, l.direita, 0.5f)
        assertEquals(81f, l.baixo, 0.5f)
        // Sem o ícone (o JSON do script sem ele): o canto (47dp a 27dp).
        val semIcone = lerCookie("{\"x\":1,\"topo\":1,\"largura\":0,\"cadeado\":true,\"semCookie\":true}")!!
        assertTrue(semIcone.naPagina)
        assertEquals(47f * d, lugarDoCadeado(semIcone, largura, altura, d).tamanho, 0.01f)
        // Puxar para baixo: bem mais para baixo que para os lados.
        assertTrue(ehParaBaixo(5f, 40f))
        assertFalse(ehParaBaixo(40f, 30f))
        assertFalse(ehParaBaixo(0f, -40f))
        // O script da página: sem mover o ícone, com a prancheta, o toque no alto e os botões sempre na página.
        val js = arquivo("src/main/assets/caixa/cookies.js", "app/src/main/assets/caixa/cookies.js").readText()
        assertFalse(js.contains("passarParaADireita"))
        assertTrue(js.contains("BolaoCadeado.toque") && js.contains("PRANCHETA") && js.contains("semCookie") && js.contains("__bolaoDepois"))
        // A barra de cima: o recarregar na Loteria, a calculadora só nos Jogos, o cadeado que se abre ao sair pela barra.
        val raiz = arquivo("src/main/java/io/github/marcosfaria94/bolao/ui/AppRaiz.kt", "app/src/main/java/io/github/marcosfaria94/bolao/ui/AppRaiz.kt").readText()
        assertTrue(raiz.contains("RECARREGAR_A_PAGINA"))
        assertTrue(raiz.contains("if (abaAtual == Aba.JOGOS) BotaoDaCalculadora"))
        assertTrue(raiz.contains("caixa.abrirCadeado()"))
        assertTrue(raiz.contains("caixa.aoPuxar"))
    }
}

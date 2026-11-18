package io.github.marcosfaria94.bolao.ui

import androidx.compose.ui.graphics.Color
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.NumerosNotaveis
import io.github.marcosfaria94.bolao.domain.ResumoDoConcurso
import io.github.marcosfaria94.bolao.domain.TIMES
import io.github.marcosfaria94.bolao.domain.timeDoCoracao
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.ui.aposta.nomeDoTimeSorteado
import io.github.marcosfaria94.bolao.ui.caixa.COR_DO_COOKIE
import io.github.marcosfaria94.bolao.ui.caixa.PosicaoDoCookie
import io.github.marcosfaria94.bolao.ui.caixa.corDoCadeado
import io.github.marcosfaria94.bolao.ui.caixa.corDoCss
import io.github.marcosfaria94.bolao.ui.caixa.corDoIconeDoCadeado
import io.github.marcosfaria94.bolao.ui.caixa.lerCookie
import io.github.marcosfaria94.bolao.ui.caixa.lugarDoCadeado
import io.github.marcosfaria94.bolao.ui.caixa.textoDoLote
import io.github.marcosfaria94.bolao.ui.caixa.tituloDoCarrinho
import io.github.marcosfaria94.bolao.ui.jogos.TECLAS_INOCUAS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.math.abs

/** Os pedidos da 0.39: k1 sem o IPCA, o cadeado espelhado ao ícone dos cookies, só o lote na Caixa, Times notáveis, seletor, "~". */
class Novidades039Test {

    private fun arquivo(vararg caminhos: String): File = caminhos.map { File(it) }.first { it.exists() }

    @Test
    fun vendasDeNovoComoNa13() {
        val atual = ParametrosK.ler(arquivo("src/main/assets/parametros_k.json", "app/src/main/assets/parametros_k.json").readText())
        val v13 = ParametrosK.ler(arquivo("src/test/resources/parametros_k_v1_3.json", "app/src/test/resources/parametros_k_v1_3.json").readText())
        assertEquals("1.5", atual.versao)
        assertNull(atual.correcao)
        // O modelo de vendas e as regras: iguais aos da 1.3.
        assertEquals(v13.modalidades, atual.modalidades)
        assertEquals(v13.irAliquota, atual.irAliquota, 0.0)
        assertEquals(v13.limiteIsencao, atual.limiteIsencao, 0.0)
    }

    @Test
    fun cookieECadeado() {
        // O JSON novo do script (com a altura, o lado, a cor e a opacidade) e o antigo (só x, topo e largura).
        val c = lerCookie("{\"x\":0.13,\"topo\":0.9,\"largura\":0.12,\"altura\":0.06,\"direita\":false,\"cor\":\"rgba(21, 89, 163, 0.5)\",\"opacidade\":0.8}")!!
        assertEquals(false, c.direita)
        assertEquals(0x801559A3.toInt(), c.cor)
        assertEquals(0.8f, c.opacidade, 1e-6f)
        assertEquals(PosicaoDoCookie(0.9f, 0.85f, 0.1f), lerCookie("{\"x\":0.9,\"topo\":0.85,\"largura\":0.1}"))
        assertNull(lerCookie("{\"x\":0.9,\"topo\":0.85,\"largura\":0.1,\"cor\":null}")?.cor)
        assertEquals(0xFF0056A7.toInt(), corDoCss("rgb(0, 86, 167)"))
        assertNull(corDoCss("transparent"))
        // Sem achar o ícone: o espelho do que se viu no site (47dp a 27dp da borda e de baixo).
        val d = 3f
        val semCookie = lugarDoCadeado(null, 1140f, 2000f, d)
        assertEquals(47f * d, semCookie.tamanho, 0.01f)
        assertEquals(27f * d, semCookie.direita, 0.01f)
        assertEquals(27f * d, semCookie.baixo, 0.01f)
        // O ícone à esquerda (a 81px da borda, 141px de largura e de altura, 81px acima do fim da página): espelhado.
        val largura = 1140f
        val altura = 2000f
        val esquerda = PosicaoDoCookie(x = (81f + 70.5f) / largura, topo = (2000f - 81f - 141f) / altura, largura = 141f / largura, altura = 141f / altura, direita = false)
        val espelho = lugarDoCadeado(esquerda, largura, altura, d)
        assertEquals(141f, espelho.tamanho, 0.5f)
        assertEquals(81f, espelho.direita, 0.5f)
        assertEquals(81f, espelho.baixo, 0.5f)
        // O ícone passou para a direita: o cadeado logo acima dele, no mesmo eixo.
        val direita = esquerda.copy(x = 1f - (81f + 70.5f) / largura, direita = true)
        val acima = lugarDoCadeado(direita, largura, altura, d)
        assertEquals(81f, acima.direita, 0.5f)
        assertEquals(81f + 141f + 8f * d, acima.baixo, 0.5f)
        // A cor: a do ícone (com a opacidade); sem ela, o azul a 50% da foto; o ícone branco, num fundo claro azul-noite.
        assertEquals(Color(COR_DO_COOKIE), corDoCadeado(null))
        assertEquals(0.4f, corDoCadeado(c.copy(cor = 0xFF0056A7.toInt(), opacidade = 0.4f)).alpha, 0.01f)
        assertEquals(Color.White, corDoIconeDoCadeado(corDoCadeado(null)))
        assertEquals(Color(0xFF13294B), corDoIconeDoCadeado(Color(0xFFF5F5F5)))
    }

    @Test
    fun soOLote() {
        assertEquals("1 aposta no carrinho", tituloDoCarrinho(1))
        assertEquals("3 apostas no carrinho", tituloDoCarrinho(3))
        assertEquals("Nenhuma aposta para transcrever agora.", textoDoLote(emptyList()))
        assertEquals("A aposta já está no carrinho do site. Se alguma saiu de lá, transcreva de novo.", textoDoLote(emptyList(), jaNoCarrinho = 1))
        assertEquals("As 2 apostas já estão no carrinho do site. Se alguma saiu de lá, transcreva de novo.", textoDoLote(emptyList(), jaNoCarrinho = 2))
    }

    @Test
    fun porcentagemComTil() {
        assertEquals("~100%", Calculadora.porcentagem(0.999_999))
        assertEquals("~34,3%", Calculadora.porcentagem(0.343))
        assertEquals("~0,00296%", Calculadora.porcentagem(0.000_029_6))
        assertEquals("0%", Calculadora.porcentagem(0.0))
        // Sem o "Certeza": a chance nunca chega a 100%.
        assertEquals("1 em 1", Calculadora.umEm(0.999_999_9))
        assertEquals(setOf(',', '.', ' ', '-'), TECLAS_INOCUAS)
    }

    @Test
    fun timesNotaveis() {
        // A busca do time pelo nome (agora num mapa): cada time da lista se acha pelo próprio nome, com ou sem acento.
        for (t in TIMES) assertEquals(t, timeDoCoracao(t.caixa))
        assertEquals("Vasco da Gama / RJ", nomeDoTimeSorteado("VASCO DA GAMA/RJ"))
        assertNotNull(timeDoCoracao("vasco da gama / rj"))
        assertNull(timeDoCoracao("TIME QUE NÃO EXISTE/XX"))
        // Os Times notáveis com o histórico embutido da Timemania: a conta e os nomes dos 80 times em pouco tempo.
        val pasta = arquivo("src/main/assets/historico", "app/src/main/assets/historico")
        val linhas = GZIPInputStream(File(pasta, "${Modalidade.TIMEMANIA.slugApi}.jsonl.gzip").inputStream()).bufferedReader().readLines()
        val resumos = linhas.mapNotNull { ResumoDoConcurso.daLinha(it) }
        val inicio = System.nanoTime()
        val times = NumerosNotaveis.grupos(Modalidade.TIMEMANIA, resumos).first { it.titulo == NumerosNotaveis.TITULO_TIMES }
        val nomes = times.itens.map { nomeDoTimeSorteado(it.rotulo) }
        val ms = (System.nanoTime() - inicio) / 1_000_000
        assertTrue("os Times notáveis levaram $ms ms", ms < 2_000)
        assertTrue(times.itens.size >= 70)
        assertEquals(times.itens.size, nomes.toSet().size)
        // O atraso: o time do último concurso tem atraso 0.
        assertTrue(times.itens.any { it.atraso == 0 })
        assertTrue(abs(times.itens.sumOf { it.vezes } - times.concursos) == 0)
    }
}

package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.domain.ConcursosNotaveis
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.EspeciaisDoHistorico
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Pix
import io.github.marcosfaria94.bolao.domain.ResumoDoConcurso
import io.github.marcosfaria94.bolao.domain.Tarjas
import io.github.marcosfaria94.bolao.domain.Trilha
import io.github.marcosfaria94.bolao.ui.ajustes.APOIE_PIX
import io.github.marcosfaria94.bolao.ui.ajustes.APOIE_TEXTO
import io.github.marcosfaria94.bolao.ui.ajustes.PIX_DO_APOIE
import io.github.marcosfaria94.bolao.ui.ajustes.modulosDoQrCode
import io.github.marcosfaria94.bolao.ui.aposta.coresDaBola
import io.github.marcosfaria94.bolao.ui.aposta.mudaOPrecoNoVolante
import io.github.marcosfaria94.bolao.ui.aposta.nomeDoTimeSorteado
import io.github.marcosfaria94.bolao.ui.aposta.precoComOSimples
import io.github.marcosfaria94.bolao.ui.jogos.ROTULO_DO_CONCURSO
import io.github.marcosfaria94.bolao.ui.jogos.rotaDasEstatisticas
import io.github.marcosfaria94.bolao.ui.jogos.temEstatisticas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.util.zip.GZIPInputStream

/**
 * 0.37: a Timemania amarela sem contorno (e a imagem da aposta amarela com o verde); o preço do volante (atalho dos
 * Parâmetros); nos Detalhes, a trilha dos concursos (o especial só com os especiais dele, a modalidade só com os
 * regulares) e o nome do time sorteado; as Estatísticas (os Concursos notáveis); o Pix do Apoie (o código e o QR Code).
 */
class Novidades037Test {

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    private fun resumos(m: Modalidade): List<ResumoDoConcurso> {
        val pasta = arquivo("src/main/assets/historico", "app/src/main/assets/historico")
        return GZIPInputStream(File(pasta, "${m.slugApi}.jsonl.gzip").inputStream()).bufferedReader().readLines()
            .mapNotNull { ResumoDoConcurso.daLinha(it) }
    }

    @Test
    fun timemaniaAmarela() {
        val b = Tarjas.bola(Modalidade.TIMEMANIA)
        assertEquals(Tarjas.AMARELO_TIMEMANIA, b.fundo)
        assertEquals(Tarjas.VERDE_TIMEMANIA, b.numero)
        // Sem o contorno verde na bola (o verde continua nos traços do volante e da apuração).
        assertFalse(b.contornoNaBola)
        assertFalse(coresDaBola(Modalidade.TIMEMANIA).comContorno)
        assertTrue(Tarjas.bola(Modalidade.QUINA).let { !it.contornoNaBola && it.contorno == 0L })
        // A imagem da aposta: amarela com o verde por cima (até a 0.36, verde com o branco); as outras, como antes.
        assertEquals(Tarjas.AMARELO_TIMEMANIA, Tarjas.fundoDaImagem(Modalidade.TIMEMANIA))
        assertEquals(Tarjas.VERDE_TIMEMANIA, Tarjas.frenteDaImagem(Modalidade.TIMEMANIA))
        assertEquals(Modalidade.QUINA.cor, Tarjas.fundoDaImagem(Modalidade.QUINA))
        assertEquals(0xFFFFFFFF, Tarjas.frenteDaImagem(Modalidade.QUINA))
    }

    @Test
    fun precoDoVolante() {
        fun aposta(m: Modalidade, numeros: Int, trevos: Int = m.trevos) = Aposta(
            id = "a", numero = 1, modalidade = m, concurso = 1, dataAposta = LocalDate.of(2026, 11, 17), valor = 0,
            numeros = (1..numeros).toList(), trevos = (1..trevos).toList(), criadaEm = 1L,
        )
        assertEquals(600L, precoComOSimples(aposta(Modalidade.MEGA_SENA, 6), 600))
        // Mais números: múltiplos da aposta simples (7 números na Mega = 7 apostas; 8 = 28).
        assertEquals(7 * 600L, precoComOSimples(aposta(Modalidade.MEGA_SENA, 7), 600))
        assertEquals(28 * 650L, precoComOSimples(aposta(Modalidade.MEGA_SENA, 8), 650))
        // O volante ainda vazio vale uma aposta simples.
        assertEquals(300L, precoComOSimples(aposta(Modalidade.QUINA, 0), 300))
        // +Milionária: vezes as combinações dos trevos (6 números e 3 trevos = 3).
        assertEquals(3 * 600L, precoComOSimples(aposta(Modalidade.MAIS_MILIONARIA, 6, 3), 600))
        assertTrue(mudaOPrecoNoVolante(Modalidade.LOTOFACIL))
        assertFalse(mudaOPrecoNoVolante(Modalidade.FEDERAL))
    }

    @Test
    fun especiaisDoHistorico() {
        // A Mega da Virada: as marcadas pela Caixa (desde 2017) e as de antes (2009 a 2016, pela arrecadação); o especial
        // de maio de 2026 (3010) é especial, mas não da época da Virada.
        val mega = EspeciaisDoHistorico.classificar(resumos(Modalidade.MEGA_SENA), Especiais.epoca(Modalidade.MEGA_SENA))
        assertEquals(
            listOf(1140, 1245, 1350, 1455, 1560, 1665, 1775, 1890, 2000, 2110, 2220, 2330, 2440, 2550, 2670, 2810, 2955),
            mega.daEpoca.filter { it <= 2955 },
        )
        assertTrue(3010 in mega.todos && 3010 !in mega.daEpoca)
        // Uma Mega comum não é especial.
        assertFalse(2954 in mega.todos || 3000 in mega.todos)
        // A Quina de São João (desde 2011) e a Lotofácil da Independência (desde 2012).
        val quina = EspeciaisDoHistorico.classificar(resumos(Modalidade.QUINA), Especiais.epoca(Modalidade.QUINA))
        assertEquals(2627, quina.daEpoca.first())
        assertTrue(listOf(4412, 6760, 7051).all { it in quina.daEpoca })
        val loto = EspeciaisDoHistorico.classificar(resumos(Modalidade.LOTOFACIL), Especiais.epoca(Modalidade.LOTOFACIL))
        assertEquals(800, loto.daEpoca.first())
        assertTrue(1861 in loto.daEpoca)
        // Sem época (a Timemania): nenhum especial.
        val time = EspeciaisDoHistorico.classificar(resumos(Modalidade.TIMEMANIA), Especiais.epoca(Modalidade.TIMEMANIA))
        assertTrue(time.todos.isEmpty() && time.daEpoca.isEmpty())
    }

    @Test
    fun trilhaDosConcursos() {
        // Só os especiais: os botões pulam de um especial para o outro.
        val virada = Trilha.especiais(listOf(2955, 2670, 2810))!!
        assertEquals(2955, virada.ultimo)
        assertEquals(2670, virada.primeiro)
        assertEquals(2810, virada.anterior(2955))
        assertNull(virada.proximo(2955))
        assertNull(virada.anterior(2670))
        assertEquals(2955, virada.maisPerto(2900))
        assertEquals(2810, virada.maisPerto(2800))
        assertFalse(virada.contem(2954))
        assertNull(Trilha.especiais(emptyList()))
        // Os regulares: sem os especiais; o último especial (a Virada que acabou de sair) não é o último da trilha.
        val mega = Trilha.regulares(2990, setOf(2955, 2990))
        assertEquals(2989, mega.ultimo)
        assertEquals(2954, mega.anterior(2956))
        assertEquals(2956, mega.proximo(2954))
        assertNull(mega.proximo(2989))
        assertEquals(2954, mega.maisPerto(2955))
        assertEquals(2989, mega.maisPerto(5000))
        assertEquals(1, mega.maisPerto(0))
        assertFalse(mega.contem(2955))
        assertTrue(mega.contem(2954))
    }

    @Test
    fun concursosNotaveis() {
        val linha = """{"numero":2955,"dataApuracao":"01/01/2026","valorArrecadado":3052000000.5,"indicadorConcursoEspecial":2,""" +
            """"listaRateioPremio":[{"faixa":2,"descricaoFaixa":"5 acertos","numeroDeGanhadores":3921,"valorPremio":11931.0},""" +
            """{"faixa":1,"descricaoFaixa":"6 acertos","numeroDeGanhadores":6,"valorPremio":181902157.83}]}"""
        val r = ResumoDoConcurso.daLinha(linha)!!
        assertEquals(2955, r.numero)
        assertEquals(LocalDate.of(2026, 1, 1), r.dia)
        assertTrue(r.marcado && r.saiu)
        assertEquals(6, r.ganhadores)
        assertEquals(18_190_215_783L, r.porGanhador)
        assertEquals(6 * 18_190_215_783L, r.total)
        assertNull(ResumoDoConcurso.daLinha("""{"semNumero":1}"""))
        // Na Mega-Sena de verdade: os maiores são os da Virada; os mais recentes, em ordem do mais novo.
        val todos = resumos(Modalidade.MEGA_SENA)
        val maiores = ConcursosNotaveis.maiores(todos)
        assertEquals(ConcursosNotaveis.QUANTOS, maiores.size)
        assertTrue(maiores.zipWithNext().all { (a, b) -> a.total >= b.total })
        assertTrue(maiores.all { it.saiu })
        val recentes = ConcursosNotaveis.recentes(todos)
        assertTrue(recentes.zipWithNext().all { (a, b) -> a.numero > b.numero })
        assertTrue(ConcursosNotaveis.resumo(todos).startsWith("O prêmio principal saiu em "))
        assertEquals("O prêmio principal saiu em 1 dos 2 concursos.", ConcursosNotaveis.resumo(listOf(r, r.copy(numero = 1, ganhadores = 0))))
        assertEquals("3.085", ConcursosNotaveis.milhar(3085))
    }

    @Test
    fun timeSorteado() {
        // O time da lista, mesmo com os espaços que a Caixa manda antes da barra.
        assertEquals("Vasco da Gama / RJ", nomeDoTimeSorteado("VASCO DA GAMA    /RJ"))
        // De fora da lista (outro biênio): o texto da Caixa limpo.
        assertEquals("SÃO RAIMUNDO / AM", nomeDoTimeSorteado("SÃO RAIMUNDO/AM"))
        // Os caracteres vazios de alguns concursos antigos saem (o da lista se acha pelas letras).
        assertEquals("Bahia / BA", nomeDoTimeSorteado("BAHIA\u0000\u0000\u0000/BA"))
        assertEquals("CRBIAMUNDOE / AL", nomeDoTimeSorteado("CRBIAMUNDOE\u0000\u0000/AL"))
    }

    @Test
    fun pixDoApoie() {
        // O exemplo do manual do Banco Central (o CRC confere).
        val exemplo = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-4266554400005204000053039865802BR" +
            "5913Fulano de Tal6008BRASILIA62070503***6304"
        assertEquals("1D3D", Pix.crc16(exemplo))
        assertEquals("0116pixfacil@aol.com", Pix.campo("01", "pixfacil@aol.com"))
        assertEquals("JOAO DA SILVA", Pix.limpar("João da Silva", 25))
        val codigo = PIX_DO_APOIE
        assertTrue(codigo.startsWith("000201"))
        assertTrue(codigo.contains("0014br.gov.bcb.pix0116$APOIE_PIX"))
        assertTrue(codigo.contains("5912MARCOS FARIA"))
        assertEquals(Pix.crc16(codigo.dropLast(4)), codigo.takeLast(4))
        // O QR Code: quadrado, com 21 + 4k módulos de lado.
        val q = modulosDoQrCode(codigo)
        assertTrue(q.isNotEmpty() && q.all { it.size == q.size })
        assertEquals(1, (q.size - 21) % 4 + 1)
        // O texto novo do Apoie e o campo da navegação dos Detalhes.
        assertTrue(APOIE_TEXTO.startsWith("Este aplicativo é mantido por paixão."))
        assertTrue(APOIE_TEXTO.endsWith("considere deixar umas moedas para o café."))
        assertEquals("Nº Concurso", ROTULO_DO_CONCURSO)
    }

    @Test
    fun estatisticas() {
        assertEquals("estatisticas/MEGA_SENA?especial=3100&concurso=2955", rotaDasEstatisticas(Modalidade.MEGA_SENA, 3100, 2955))
        assertEquals("estatisticas/QUINA?especial=0&concurso=0", rotaDasEstatisticas(Modalidade.QUINA, null, null))
        assertTrue(temEstatisticas(Modalidade.LOTECA))
        assertFalse(temEstatisticas(Modalidade.FEDERAL))
    }
}

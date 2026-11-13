package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Favoritos
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Probabilidades
import io.github.marcosfaria94.bolao.domain.Tarjas
import io.github.marcosfaria94.bolao.ui.aposta.CONCURSOS_DA_TEIMOSINHA
import io.github.marcosfaria94.bolao.ui.aposta.avisoAoSair
import io.github.marcosfaria94.bolao.ui.aposta.temNumeroMarcado
import io.github.marcosfaria94.bolao.ui.jogos.linhasInvertidas
import io.github.marcosfaria94.bolao.ui.jogos.regras
import io.github.marcosfaria94.bolao.ui.jogos.rotuloDaFaixa
import io.github.marcosfaria94.bolao.ui.painel.avisoDeRepeticao
import io.github.marcosfaria94.bolao.ui.painel.textoDaExclusaoDoFavorito
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** As novidades da 0.29 que não dependem da tela. */
class Novidades029Test {

    private val hoje = LocalDate.of(2026, 11, 12)

    private fun aposta(
        m: Modalidade,
        numeros: List<Int>,
        id: String = "a${numeros.hashCode()}",
        concurso: Int = 100,
        trevos: List<Int> = emptyList(),
        extra: String? = null,
        registrada: Boolean = false,
        numero: Int = 0,
    ) = Aposta(
        id = id, numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje, valor = 600,
        numeros = numeros, trevos = trevos, extra = extra, criadaEm = 1, registrada = registrada,
    )

    private fun estado(vararg apostas: Aposta) = Estado(
        cadastroConcluido = true,
        config = Config(parametros = Parametros()),
        lancamentos = listOf(Lancamento(id = "l1", data = hoje, criadoEm = 1, tipo = TipoLancamento.APORTE, valor = 30_000)),
        apostas = apostas.toList(),
    )

    @Test
    fun regrasComDe() {
        val e = estado()
        val mega = regras(Modalidade.MEGA_SENA, e, mais = true).toMap()
        assertEquals("De 01 a 60", mega["Volante"])
        assertEquals("De 6 a 20 números", mega["Aposta"])
        // Desde a 0.32 as regras não têm o preço (ele fica no cartão "Preço").
        assertNull(mega["Preço"])
        // Sem faixa (a aposta simples), sem o "De".
        assertEquals("6 números", regras(Modalidade.MEGA_SENA, e, mais = false).toMap()["Aposta"])
        val time = regras(Modalidade.TIMEMANIA, e, mais = false).toMap()
        assertEquals("De 01 a 80", time["Volante"])
        assertEquals("10 números e o Time do Coração", time["Aposta"])
        assertEquals("A partir de 3 números, ou o Time do Coração", time["Premiação"])
        assertEquals("De 00 a 99", regras(Modalidade.LOTOMANIA, e, mais = false).toMap()["Volante"])
        assertEquals("De 01 a 50 e trevos de 1 a 6", regras(Modalidade.MAIS_MILIONARIA, e, mais = false).toMap()["Volante"])
    }

    @Test
    fun tabelaDeLadoDaTimemania() {
        val m = Modalidade.TIMEMANIA
        val linhas = Probabilidades.tabela(m, mais = true)
        // A Timemania só tem a aposta simples: a tabela fica de lado (Acertos | Probabilidade).
        assertEquals(1, linhas.size)
        val deLado = linhasInvertidas(m, linhas[0])
        assertEquals(listOf("7", "6", "5", "4", "3", "Time do Coração"), deLado.map { it.first })
        assertEquals("1 em 26.472.637", deLado[0].second.single())
        assertEquals("1 em 80", deLado.last().second.single())
        // Os nomes das faixas: com o número de acertos.
        assertEquals("6 (Sena)", rotuloDaFaixa(Modalidade.MEGA_SENA, "Sena"))
        assertEquals("2 (Duque)", rotuloDaFaixa(Modalidade.QUINA, "Duque"))
        assertEquals("15", rotuloDaFaixa(Modalidade.LOTOFACIL, "15 acertos"))
        assertEquals("7", rotuloDaFaixa(Modalidade.SUPER_SETE, "7 colunas"))
        assertEquals("Mês de Sorte", rotuloDaFaixa(Modalidade.DIA_DE_SORTE, "Mês de Sorte"))
        assertEquals("6 + 2", rotuloDaFaixa(Modalidade.MAIS_MILIONARIA, "6 + 2"))
        // A Mega-Sena sem as apostas com mais números: uma linha por faixa.
        val mega = linhasInvertidas(Modalidade.MEGA_SENA, Probabilidades.tabela(Modalidade.MEGA_SENA, mais = false)[0])
        assertEquals(listOf("6 (Sena)" to listOf("1 em 50.063.860"), "5 (Quina)" to listOf("1 em 154.518"), "4 (Quadra)" to listOf("1 em 2.332")), mega)
    }

    @Test
    fun apostaRepetida() {
        val feita = aposta(Modalidade.QUINA, listOf(5, 12, 33, 47, 71), id = "r1", concurso = 7149, registrada = true, numero = 3)
        val outroConcurso = aposta(Modalidade.QUINA, listOf(5, 12, 33, 47, 71), id = "r2", concurso = 7148, registrada = true, numero = 2)
        val emAndamento = aposta(Modalidade.QUINA, listOf(5, 12, 33, 47, 71), id = "r3", concurso = 7149)
        val e = estado(feita, outroConcurso, emAndamento)
        // O favorito na ordem errada é o mesmo jogo; só a registrada no mesmo concurso conta.
        val nova = aposta(Modalidade.QUINA, listOf(71, 47, 33, 12, 5), id = "n1", concurso = 7149)
        assertEquals(listOf(feita), Favoritos.repetidas(e, nova))
        assertTrue(Favoritos.repetidas(e, nova.copy(concurso = 7150)).isEmpty())
        assertTrue(Favoritos.repetidas(e, nova.copy(numeros = listOf(1, 2, 3, 4, 5))).isEmpty())
        assertTrue(Favoritos.repetidas(e, nova.copy(concurso = 0)).isEmpty())
        // O time do coração conta.
        val time = aposta(Modalidade.TIMEMANIA, (1..10).toList(), id = "t1", concurso = 2470, extra = "AMERICA/RN", registrada = true, numero = 4)
        val et = estado(time)
        assertTrue(Favoritos.repetidas(et, time.copy(id = "t2", extra = "AMERICA/MG")).isEmpty())
        assertEquals(1, Favoritos.repetidas(et, time.copy(id = "t2")).size)
        // Os avisos.
        assertNull(avisoDeRepeticao(listOf(nova to emptyList())))
        assertEquals("Esse jogo já foi apostado no concurso 7149 da Quina (aposta Nº3).", avisoDeRepeticao(listOf(nova to listOf(feita))))
        assertEquals(
            "2 desses jogos já foram apostados nos mesmos concursos (Quina 7149: Nº3; Timemania 2470: Nº4).",
            avisoDeRepeticao(listOf(nova to listOf(feita), time to listOf(time))),
        )
    }

    @Test
    fun volanteETextos() {
        assertTrue(temNumeroMarcado(aposta(Modalidade.QUINA, listOf(5))))
        assertTrue(!temNumeroMarcado(aposta(Modalidade.QUINA, emptyList())))
        assertTrue(!temNumeroMarcado(aposta(Modalidade.SUPER_SETE, List(7) { -1 })))
        assertTrue(temNumeroMarcado(aposta(Modalidade.SUPER_SETE, listOf(-1, 3, -1, -1, -1, -1, -1))))
        assertEquals(2..10, CONCURSOS_DA_TEIMOSINHA)
        assertEquals("Se sair agora, a aposta é descartada: complete o volante.", avisoAoSair(false))
        val e = Favoritos.favoritar(estado(), aposta(Modalidade.QUINA, listOf(5, 12, 33, 47, 71)), 1)
        assertEquals("O jogo de 5 números da Quina sai dos favoritos. As apostas já feitas com ele continuam.", textoDaExclusaoDoFavorito(e.favoritos.single()))
    }

    @Test
    fun coresOficiais() {
        // A Timemania: amarela com o número e o contorno verdes (a marca dela).
        val t = Tarjas.bola(Modalidade.TIMEMANIA)
        assertEquals(Tarjas.AMARELO_TIMEMANIA, t.fundo)
        assertEquals(Tarjas.VERDE_TIMEMANIA, t.numero)
        assertEquals(Tarjas.VERDE_TIMEMANIA, t.contorno)
        assertEquals(Tarjas.AMARELO_TIMEMANIA, Tarjas.destaque(Modalidade.TIMEMANIA))
        // As outras: a cor da modalidade com o número branco, sem contorno.
        for (m in Modalidade.ATIVAS - Modalidade.TIMEMANIA) {
            assertEquals(m.cor, Tarjas.bola(m).fundo)
            assertEquals(0xFFFFFFFF, Tarjas.bola(m).numero)
            assertEquals(0L, Tarjas.bola(m).contorno)
            assertEquals(m.cor, Tarjas.destaque(m))
        }
        // Os trevos da +Milionária: dourados.
        assertTrue(Tarjas.trevo(Modalidade.MAIS_MILIONARIA) != Tarjas.bola(Modalidade.MAIS_MILIONARIA))
        assertEquals(Tarjas.bola(Modalidade.QUINA), Tarjas.trevo(Modalidade.QUINA))
    }
}

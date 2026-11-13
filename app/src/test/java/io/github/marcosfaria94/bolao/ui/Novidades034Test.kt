package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.domain.AvisosDeEspeciais
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.PremiosTipicos
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.notificacoes.AvisoDeEspeciais
import io.github.marcosfaria94.bolao.ui.ajustes.resumoDosAvisos
import io.github.marcosfaria94.bolao.ui.jogos.COLUNA_ULTIMO_CONCURSO
import io.github.marcosfaria94.bolao.ui.jogos.colunaDoConcurso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** As novidades da 0.34 que dão para testar sem as telas. */
class Novidades034Test {

    private fun item(m: Modalidade, proximo: Int, k1: Double?, especial: Boolean = false) = Indices.Item(
        m, Concurso(m, proximo - 1, null, emptyList(), proximoNumero = proximo),
        k1?.let { ResultadoK(proximo, especial, 0.0, 0, emptyMap(), 0.0, k1 = it, k2 = 0.0) }, odds = 10.0,
    )

    private val virada = Especiais.CONHECIDOS.first { it.modalidade == Modalidade.MEGA_SENA }

    @Test
    fun jogosEmOrdemDeK1ComOsEspeciais() {
        val quina = Indices.Opcao.DoJogo(item(Modalidade.QUINA, 7167, 0.62))
        val mega = Indices.Opcao.DoJogo(item(Modalidade.MEGA_SENA, 3086, 0.31))
        val loteca = Indices.Opcao.DoJogo(item(Modalidade.LOTECA, 1282, null))
        val especial = Indices.Opcao.Especial(virada, item(Modalidade.MEGA_SENA, 3100, 0.395, especial = true))
        // O especial entra na ordem do k1 (não fica mais sempre no alto); sem k1 no fim.
        val ordem = Indices.emOrdem(listOf(loteca, especial, mega, quina))
        assertEquals(listOf(quina, especial, mega, loteca), ordem)
        // O destaque é o de maior k1: aqui a Quina (o especial continua especial, mas não é o destaque).
        assertEquals(Indices.Destaque(Modalidade.QUINA, null), Indices.destaqueDe(ordem.first()))
        // Com o especial no alto, ele é o destaque (com o concurso especial).
        val outra = Indices.emOrdem(listOf(mega, especial))
        assertEquals(Indices.Destaque(Modalidade.MEGA_SENA, virada), Indices.destaqueDe(outra.first()))
        // No empate, o especial primeiro.
        val empate = Indices.Opcao.DoJogo(item(Modalidade.QUINA, 7167, 0.395))
        assertEquals(especial, Indices.emOrdem(listOf(empate, especial)).first())
    }

    private val alvo = AvisosDeEspeciais.Alvo(
        Modalidade.MEGA_SENA, "Mega da Virada", 3100, LocalDate.of(2027, 1, 1), LocalTime.of(11, 0),
        premio = 100_000_000_000L, k1 = 0.395, destino = "especial/MEGA_SENA/3100?detalhes=false",
    )

    @Test
    fun avisosDosConcursosEspeciais() {
        val antes = LocalDateTime.of(2026, 11, 13, 14, 0)
        // Apostas abertas: o primeiro aviso, uma vez.
        val abertas = AvisosDeEspeciais.devidos(listOf(alvo), antes, emptySet()).single()
        assertEquals("Mega da Virada: apostas abertas", abertas.titulo)
        assertEquals("Concurso 3100 · 01/01/2027 às 11h · prêmio estimado de R$ 1 Bilhão · k1 0,395.", abertas.texto)
        assertEquals(alvo.destino, abertas.destino)
        assertTrue(AvisosDeEspeciais.devidos(listOf(alvo), antes, abertas.chaves.toSet()).isEmpty())
        // Na véspera, a partir das 9h: "o sorteio é amanhã".
        val cedo = LocalDateTime.of(2026, 12, 31, 8, 0)
        assertTrue(AvisosDeEspeciais.devidos(listOf(alvo), cedo, abertas.chaves.toSet()).isEmpty())
        val vespera = AvisosDeEspeciais.devidos(listOf(alvo), LocalDateTime.of(2026, 12, 31, 9, 30), abertas.chaves.toSet()).single()
        assertEquals("Mega da Virada: o sorteio é amanhã às 11h", vespera.titulo)
        assertTrue(vespera.texto.endsWith("Ainda dá para apostar."))
        // Sem o aviso das apostas (o app soube do especial na véspera): só o da véspera, que marca os dois.
        val soVespera = AvisosDeEspeciais.devidos(listOf(alvo), LocalDateTime.of(2026, 12, 31, 10, 0), emptySet()).single()
        assertEquals(2, soVespera.chaves.size)
        // Perdida a véspera: no dia, antes do sorteio, "é hoje".
        val hoje = AvisosDeEspeciais.devidos(listOf(alvo), LocalDateTime.of(2027, 1, 1, 8, 0), abertas.chaves.toSet()).single()
        assertEquals("Mega da Virada: o sorteio é hoje às 11h", hoje.titulo)
        // Depois do sorteio, nada.
        assertTrue(AvisosDeEspeciais.devidos(listOf(alvo), LocalDateTime.of(2027, 1, 1, 11, 0), emptySet()).isEmpty())
        // O mesmo especial duas vezes na lista (anunciado e próximo concurso): um aviso só.
        assertEquals(1, AvisosDeEspeciais.devidos(listOf(alvo, alvo.copy(destino = "modalidade/MEGA_SENA")), antes, emptySet()).size)
        // Sem a data, o prêmio e o k1: só o que se sabe.
        val semNada = alvo.copy(dia = null, hora = null, premio = null, k1 = null)
        assertEquals("Concurso 3100.", AvisosDeEspeciais.devidos(listOf(semNada), antes, emptySet()).single().texto)
        // O aviso vem ligado por padrão (pedido do usuário).
        assertTrue(AvisoDeEspeciais.LIGADO_PADRAO)
    }

    @Test
    fun detalhesEAjustes() {
        // A coluna dos Prêmios típicos é a do concurso que está na tela.
        assertEquals("Concurso 7000", colunaDoConcurso(7000, 7166))
        assertEquals(COLUNA_ULTIMO_CONCURSO, colunaDoConcurso(7166, 7166))
        assertTrue(PremiosTipicos.notaDoConcurso(7000, ultimo = false).startsWith("Concurso 7000:"))
        assertEquals(PremiosTipicos.notaDoUltimo(7166), PremiosTipicos.notaDoConcurso(7166, ultimo = true))
        // As linhas saem do concurso escolhido (o que ele pagou em cada faixa).
        val c = Concurso(
            Modalidade.QUINA, 7000, null, listOf(20, 30, 53, 56, 64),
            faixas = listOf(FaixaPremio(1, "5 acertos", 0, 0), FaixaPremio(2, "4 acertos", 32, 1_696_350), FaixaPremio(3, "3 acertos", 2_920, 16_010)),
        )
        val linhas = PremiosTipicos.linhas(Modalidade.QUINA, null, c)
        assertNull(linhas[0].ultimo)
        assertEquals(1_696_350L, linhas[1].ultimo)
        // O resumo das Notificações na lista dos Ajustes.
        assertEquals("3 avisos ligados", resumoDosAvisos(3))
        assertEquals("1 aviso ligado", resumoDosAvisos(1))
        assertEquals("Avisos desligados", resumoDosAvisos(0))
    }
}

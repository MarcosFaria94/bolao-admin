package io.github.marcosfaria94.bolao.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.seguranca.Biometria
import io.github.marcosfaria94.bolao.ui.ajustes.SecaoAjuste
import io.github.marcosfaria94.bolao.ui.ajustes.textoDoTempo
import io.github.marcosfaria94.bolao.ui.aposta.quantidadeEmDestaque
import io.github.marcosfaria94.bolao.ui.home.GrupoEmAndamento
import io.github.marcosfaria94.bolao.ui.jogos.TABELA_ROLANDO
import io.github.marcosfaria94.bolao.ui.jogos.TENTATIVAS_DA_TABELA
import io.github.marcosfaria94.bolao.ui.jogos.distribuirSobra
import io.github.marcosfaria94.bolao.ui.jogos.larguraEmDuasLinhas
import io.github.marcosfaria94.bolao.ui.jogos.primeiraQueCabe
import io.github.marcosfaria94.bolao.ui.painel.TipoDoResumo
import io.github.marcosfaria94.bolao.ui.painel.porModalidade
import io.github.marcosfaria94.bolao.ui.painel.textoDasApostas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** As novidades da 0.30 que não dependem da tela. */
class Novidades030Test {

    @Test
    fun biometriaNoCelular() {
        // A Biometria nos Ajustes, no grupo Celular (do app todo), entre as Notificações e o Backup; abre a tela dela.
        assertEquals(listOf(SecaoAjuste.NOTIFICACOES, SecaoAjuste.BIOMETRIA, SecaoAjuste.BACKUP), SecaoAjuste.grupos(false).first { it.first == "CELULAR" }.second)
        assertEquals(listOf(SecaoAjuste.NOTIFICACOES, SecaoAjuste.BIOMETRIA, SecaoAjuste.BACKUP), SecaoAjuste.grupos(true).first { it.first == "CELULAR" }.second)
        assertEquals(SecaoAjuste.BIOMETRIA, SecaoAjuste.de("biometria"))
        assertEquals("Biometria", SecaoAjuste.BIOMETRIA.titulo(false))
        assertEquals("ajustes/biometria", SecaoAjuste.BIOMETRIA.rota)
        // Os tempos: 3 minutos é o de sempre.
        assertEquals(listOf(1, 3, 10, 30), Biometria.TEMPOS)
        assertTrue(Biometria.MINUTOS_PADRAO in Biometria.TEMPOS)
        assertEquals(3, Biometria.MINUTOS_PADRAO)
        assertEquals("10 min", textoDoTempo(10))
    }

    @Test
    fun superSeteDesmarcaATocarDeNovo() {
        val vazio = List(Volante.COLUNAS_SUPER_SETE) { -1 }
        val um = Volante.marcarColuna(vazio, 2, 5)
        assertEquals(listOf(-1, -1, 5, -1, -1, -1, -1), um)
        // Outro número na mesma coluna troca.
        assertEquals(listOf(-1, -1, 8, -1, -1, -1, -1), Volante.marcarColuna(um, 2, 8))
        // O mesmo número desmarca a coluna.
        assertEquals(vazio, Volante.marcarColuna(um, 2, 5))
        // A lista curta (aposta antiga) vira as 7 colunas.
        assertEquals(listOf(0, -1, -1, -1, -1, -1, -1), Volante.marcarColuna(emptyList(), 0, 0))
        // A Loteca: o mesmo palpite tira o palpite do jogo.
        val p = Volante.marcarPalpite(emptyList(), 3, "X")
        assertEquals("X", p[3])
        assertEquals(Volante.JOGOS_LOTECA, p.size)
        assertEquals("1", Volante.marcarPalpite(p, 3, "1")[3])
        assertEquals("", Volante.marcarPalpite(p, 3, "X")[3])
    }

    @Test
    fun quantidadeDoVolanteEmDestaque() {
        val dourado = Color(0xFFF2B630)
        val t = quantidadeEmDestaque("Marque no mínimo 6 números", dourado)
        assertEquals("Marque no mínimo 6 números", t.text)
        val destaque = t.spanStyles.single()
        assertEquals("6 números", t.text.substring(destaque.start, destaque.end))
        assertEquals(dourado, destaque.item.color)
        assertEquals(FontWeight.Bold, destaque.item.fontWeight)
        assertEquals("1 trevo", quantidadeEmDestaque("Marque 1 trevo", dourado).let { it.text.substring(it.spanStyles[0].start, it.spanStyles[0].end) })
        // Sem número, sem destaque.
        assertTrue(quantidadeEmDestaque("Escolha o Time do Coração", dourado).spanStyles.isEmpty())
    }

    @Test
    fun tabelasCabemAntesDeRolar() {
        // Uma letra = 10 px.
        val largura = { t: String -> t.length * 10 }
        // "Qtd de jogos": a melhor quebra é "Qtd de" / "jogos" (60 px + 1).
        assertEquals(61, larguraEmDuasLinhas("Qtd de jogos", largura))
        assertEquals(81, larguraEmDuasLinhas("Números + trevos", largura))
        // Sem espaço, a palavra inteira.
        assertEquals(60, larguraEmDuasLinhas("Quadra", largura))
        // A primeira forma que cabe; nenhuma, rola.
        val totais = mapOf(TENTATIVAS_DA_TABELA[0] to 340, TENTATIVAS_DA_TABELA[1] to 330, TENTATIVAS_DA_TABELA[2] to 310)
        assertEquals(TENTATIVAS_DA_TABELA[2], primeiraQueCabe(TENTATIVAS_DA_TABELA, 316) { totais[it] ?: 300 })
        assertEquals(TENTATIVAS_DA_TABELA[0], primeiraQueCabe(TENTATIVAS_DA_TABELA, 400) { totais[it] ?: 300 })
        assertNull(primeiraQueCabe(TENTATIVAS_DA_TABELA, 100) { 200 })
        // A letra nunca fica menor que 85%; rolando, a letra de sempre.
        assertTrue(TENTATIVAS_DA_TABELA.all { it.escala >= 0.85f })
        assertEquals(1f, TABELA_ROLANDO.escala)
        assertEquals(8.dp, TABELA_ROLANDO.folga)
        // A sobra vai para as colunas por igual e o resto, para a última: a soma é a largura exata.
        assertEquals(listOf(104, 64, 64, 86), distribuirSobra(listOf(100, 60, 60, 80), 318))
        assertEquals(316, distribuirSobra(listOf(71, 101, 67, 56), 316).sum())
        // Sem sobra (rolando), como está.
        assertEquals(listOf(200, 200), distribuirSobra(listOf(200, 200), 316))
    }

    @Test
    fun painelPorModalidade() {
        val dia = LocalDate.of(2026, 12, 1)
        fun g(m: Modalidade, c: Int, d: Int?) = GrupoEmAndamento(m, c, d?.let { dia.plusDays(it.toLong()) }, emptyList())
        val mega = g(Modalidade.MEGA_SENA, 3015, 2)
        val virada = g(Modalidade.MEGA_SENA, 3100, 31)
        val quina = g(Modalidade.QUINA, 7200, 0)
        val loto = g(Modalidade.LOTOFACIL, 3800, 2)
        val dupla = g(Modalidade.DUPLA_SENA, 2900, null)
        val k1 = mapOf(Modalidade.MEGA_SENA to 0.4, Modalidade.LOTOFACIL to 0.9, Modalidade.QUINA to 0.1)
        val ordem = porModalidade(listOf(virada, dupla, loto, mega, quina)) { k1[it.modalidade] }
        // O sorteio mais próximo primeiro (a Quina, hoje); no mesmo dia, o maior k1 (a Lotofácil antes da Mega); sem data, no fim.
        assertEquals(listOf(Modalidade.QUINA, Modalidade.LOTOFACIL, Modalidade.MEGA_SENA, Modalidade.DUPLA_SENA), ordem.map { it.modalidade })
        // A Mega da Virada dentro da Mega-Sena, como outro concurso, depois do mais próximo.
        assertEquals(listOf(mega, virada), ordem.first { it.modalidade == Modalidade.MEGA_SENA }.concursos)
        // A modalidade vai pelo concurso mais próximo dela: com a Virada sozinha, a Mega vai para o dia da Virada.
        val soVirada = porModalidade(listOf(virada, quina, dupla)) { null }
        assertEquals(listOf(Modalidade.QUINA, Modalidade.MEGA_SENA, Modalidade.DUPLA_SENA), soVirada.map { it.modalidade })
        // Sem k1 no mesmo dia: depois das que têm.
        val semK1 = porModalidade(listOf(mega, loto)) { if (it.modalidade == Modalidade.MEGA_SENA) 0.2 else null }
        assertEquals(listOf(Modalidade.MEGA_SENA, Modalidade.LOTOFACIL), semK1.map { it.modalidade })
    }

    @Test
    fun textosDoPainel() {
        assertEquals("Apostas não registradas", TipoDoResumo.REGISTRAR.rotulo)
        assertEquals("1 aposta", textoDasApostas(1))
        assertEquals("3 apostas", textoDasApostas(3))
    }
}

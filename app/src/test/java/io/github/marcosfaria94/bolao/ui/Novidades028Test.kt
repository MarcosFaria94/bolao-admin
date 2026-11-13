package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.DadosDeExemplo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Favoritos
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.aposta.textoDoBotaoFavorito
import io.github.marcosfaria94.bolao.ui.jogos.erroDoConcurso
import io.github.marcosfaria94.bolao.ui.jogos.tituloDaQuantidade
import io.github.marcosfaria94.bolao.ui.painel.impedimentoDoFavorito
import io.github.marcosfaria94.bolao.ui.painel.mensagemDosFavoritos
import io.github.marcosfaria94.bolao.ui.painel.textoDosFavoritos
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** As novidades da 0.28: teimosinha fora do especial, o especial em destaque, as tabelas divididas e os jogos favoritos. */
class Novidades028Test {

    private val hoje = LocalDate.of(2026, 11, 12)
    private val fuso = ZoneId.of("America/Sao_Paulo")

    private fun aposta(
        m: Modalidade,
        numeros: List<Int>,
        id: String = "a${numeros.hashCode()}",
        concurso: Int = 100,
        trevos: List<Int> = emptyList(),
        extra: String? = null,
        valor: Long = 600,
    ) = Aposta(
        id = id, numero = 0, modalidade = m, concurso = concurso, dataAposta = hoje, valor = valor,
        numeros = numeros, trevos = trevos, extra = extra, criadaEm = 1,
    )

    /** Um bolão com saldo (o aporte do mês já entrou). */
    private fun comSaldo(parametros: Parametros = Parametros()) = Estado(
        cadastroConcluido = true,
        config = Config(parametros = parametros),
        lancamentos = listOf(Lancamento(id = "l1", data = hoje, criadoEm = 1, tipo = TipoLancamento.APORTE, valor = 30_000)),
    )

    @Test
    fun semTeimosinhaNoConcursoEspecial() {
        val virada = aposta(Modalidade.MEGA_SENA, listOf(1, 2, 3, 4, 5, 6), concurso = 3100).copy(teimosinha = 4)
        assertFalse(Operacoes.aceitaTeimosinha(virada))
        assertEquals(1, Operacoes.concursosDaTeimosinha(virada))
        assertEquals(1, Operacoes.apostasDoRegistro(virada))
        // No concurso normal continua.
        val normal = virada.copy(concurso = 3090)
        assertTrue(Operacoes.aceitaTeimosinha(normal))
        assertEquals(4, Operacoes.concursosDaTeimosinha(normal))
        // Registrar a do especial faz uma aposta só, mesmo com a teimosinha marcada no rascunho.
        val e = comSaldo(Parametros(teimosinha = true))
        val (depois, feitas) = Operacoes.registrarTudo(Operacoes.salvarRascunho(e, virada), virada.id, hoje, 10)
        assertEquals(1, feitas.size)
        assertEquals(3100, feitas[0].concurso)
        assertEquals(0, feitas[0].teimosinha)
        assertEquals(e.saldo - 600, depois.saldo)
    }

    @Test
    fun concursoEspecialSempreEmDestaque() {
        val virada = Especiais.doConcurso(Modalidade.MEGA_SENA, 3100)!!
        val antes = LocalDateTime.of(2026, 11, 12, 10, 0).atZone(fuso).toInstant()
        // À parte (o próximo da Mega ainda é um concurso normal) ou já como o próximo: é o destaque.
        assertEquals(virada, Especiais.emDestaque(antes, { if (it == Modalidade.MEGA_SENA) 3085 else null }))
        assertEquals(virada, Especiais.emDestaque(antes, { if (it == Modalidade.MEGA_SENA) 3100 else null }))
        // Sorteado (o próximo já passou dele, ou a hora passou): não há especial em destaque.
        assertNull(Especiais.emDestaque(antes, { if (it == Modalidade.MEGA_SENA) 3101 else null }))
        val depois = LocalDateTime.of(2027, 1, 1, 11, 1).atZone(fuso).toInstant()
        assertNull(Especiais.emDestaque(depois, { null }))
        // Com dois, o de sorteio mais próximo.
        val quina = ConcursoEspecial(Modalidade.QUINA, "Quina de São João", "quina de são joão", 7200, LocalDateTime.of(2026, 12, 20, 20, 0), null)
        val lista = Especiais.CONHECIDOS + quina
        assertEquals(quina, Especiais.emDestaque(antes, { null }, lista))
    }

    @Test
    fun tabelasDivididas() {
        // Desde a 0.29 as tabelas não se dividem mais (uma tabela só, que rola para os lados).
        assertEquals("6 acertos", tituloDaQuantidade(6))
        assertEquals("Não foi possível consultar o concurso 2949 na Caixa.", erroDoConcurso(2949))
    }

    @Test
    fun favoritarEDesfavoritar() {
        var e = comSaldo()
        val a = aposta(Modalidade.QUINA, listOf(71, 5, 33, 12, 47))
        assertTrue(Favoritos.podeFavoritar(a))
        e = Favoritos.alternar(e, a, 100)
        assertEquals(1, e.favoritos.size)
        val f = e.favoritos[0]
        // Guarda os números (em ordem), não o concurso.
        assertEquals(listOf(5, 12, 33, 47, 71), f.numeros)
        assertEquals(Modalidade.QUINA, f.modalidade)
        // A mesma aposta em outro concurso (e com os números em outra ordem) é o mesmo jogo.
        val outra = aposta(Modalidade.QUINA, listOf(5, 12, 33, 47, 71), id = "b", concurso = 200)
        assertEquals(f, Favoritos.doJogo(e, outra))
        // Favoritar de novo não duplica.
        assertEquals(e, Favoritos.favoritar(e, outra, 200))
        // Outra modalidade com os mesmos números não é o mesmo jogo.
        assertNull(Favoritos.doJogo(e, outra.copy(modalidade = Modalidade.MEGA_SENA)))
        // Tirar e o "Desfazer".
        val semEle = Favoritos.alternar(e, outra, 300)
        assertTrue(semEle.favoritos.isEmpty())
        assertEquals(e.favoritos, Favoritos.devolver(semEle, f).favoritos)
        // Incompleta, Loteca e Federal não viram favoritas.
        assertFalse(Favoritos.podeFavoritar(aposta(Modalidade.QUINA, listOf(1, 2, 3))))
        assertFalse(Favoritos.podeFavoritar(aposta(Modalidade.LOTECA, emptyList()).copy(palpitesLoteca = List(14) { "1" })))
        assertFalse(Favoritos.aceita(Modalidade.FEDERAL))
        // O time do coração e os trevos contam.
        val t = aposta(Modalidade.TIMEMANIA, (1..10).toList(), extra = "AMERICA/RN")
        val comTime = Favoritos.favoritar(comSaldo(), t, 1)
        assertNotNull(Favoritos.doJogo(comTime, t))
        assertNull(Favoritos.doJogo(comTime, t.copy(extra = "AMERICA/MG")))
        val mm = aposta(Modalidade.MAIS_MILIONARIA, listOf(3, 11, 19, 27, 38, 49), trevos = listOf(5, 2))
        val comTrevos = Favoritos.favoritar(comSaldo(), mm, 1)
        assertEquals(listOf(2, 5), comTrevos.favoritos[0].trevos)
        assertNull(Favoritos.doJogo(comTrevos, mm.copy(trevos = listOf(2, 6))))
        // Super Sete: as colunas na ordem delas.
        val s7 = aposta(Modalidade.SUPER_SETE, listOf(3, 0, 7, 1, 9, 4, 6))
        val comS7 = Favoritos.favoritar(comSaldo(), s7, 1)
        assertEquals(listOf(3, 0, 7, 1, 9, 4, 6), comS7.favoritos[0].numeros)
        assertNull(Favoritos.doJogo(comS7, s7.copy(numeros = listOf(0, 3, 7, 1, 9, 4, 6))))
        // Guardado no JSON da sessão (e o JSON antigo, sem favoritos, continua lendo).
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        assertEquals(comTime, json.decodeFromString(Estado.serializer(), json.encodeToString(Estado.serializer(), comTime)))
        assertTrue(json.decodeFromString(Estado.serializer(), "{\"cadastroConcluido\":true}").favoritos.isEmpty())
    }

    @Test
    fun favoritoComMaisNumerosSoComOParametro() {
        val sete = aposta(Modalidade.MEGA_SENA, listOf(1, 2, 3, 4, 5, 6, 7), valor = 4200)
        val e = Favoritos.favoritar(comSaldo(Parametros(maisNumeros = true)), sete, 1)
        val f = e.favoritos.single()
        assertTrue(Favoritos.cabe(e, f))
        assertEquals(7, Favoritos.apostasSimples(f))
        assertEquals(e.config.preco(Modalidade.MEGA_SENA) * 7, Favoritos.preco(e, f))
        assertEquals("7 números", Favoritos.descricao(f))
        // Sem o parâmetro, não cabe no volante (nem pode ser apostado).
        val sem = e.copy(config = e.config.copy(parametros = Parametros(maisNumeros = false)))
        assertFalse(Favoritos.cabe(sem, f))
        assertTrue(Favoritos.paraOVolante(sem, Modalidade.MEGA_SENA).isEmpty())
        assertEquals("Precisa das apostas com mais números (Ajustes > Parâmetros).", impedimentoDoFavorito(sem, f, 3090, false))
        assertNull(impedimentoDoFavorito(e, f, 3090, false))
        assertTrue(impedimentoDoFavorito(e, f, 0, false)!!.startsWith("Sem os dados do próximo concurso da Mega-Sena"))
        assertEquals("O concurso 3090 da Mega-Sena já foi sorteado: aguarde o resultado.", impedimentoDoFavorito(e, f, 3090, true))
        assertEquals("Fazer jogo favorito", textoDoBotaoFavorito(1))
        assertEquals("Fazer jogo favorito (3)", textoDoBotaoFavorito(3))
        // A +Milionária com trevos a mais.
        val mm = aposta(Modalidade.MAIS_MILIONARIA, listOf(3, 11, 19, 27, 38, 49), trevos = listOf(1, 2, 3))
        val comMm = Favoritos.favoritar(e, mm, 2)
        assertEquals("6 números e 3 trevos", Favoritos.descricao(comMm.favoritos.last()))
    }

    @Test
    fun apostarOsFavoritos() {
        var e = comSaldo()
        val quina = aposta(Modalidade.QUINA, listOf(5, 12, 33, 47, 71), concurso = 7140)
        val dia = aposta(Modalidade.DIA_DE_SORTE, listOf(2, 7, 11, 18, 22, 25, 30), concurso = 1100, extra = "MARÇO")
        e = Favoritos.favoritar(Favoritos.favoritar(e, quina, 1), dia, 2)
        // As apostas nos próximos concursos (a base vem da aposta nova da modalidade, já no concurso certo).
        val apostas = e.favoritos.mapIndexed { i, f ->
            val base = aposta(f.modalidade, emptyList(), id = "n$i", concurso = if (f.modalidade == Modalidade.QUINA) 7149 else 1126, valor = e.config.preco(f.modalidade))
            Favoritos.aposta(e, f, base)
        }
        assertEquals(listOf(5, 12, 33, 47, 71), apostas[0].numeros)
        assertEquals("MARÇO", apostas[1].extra)
        assertEquals(7149, apostas[0].concurso)
        val texto = textoDosFavoritos(apostas, individual = false)
        assertTrue(texto, texto.startsWith("As 2 apostas vão para os próximos concursos (Quina 7149 e Dia de Sorte 1126), e R$"))
        // Desde a 0.36: fazer os jogos no site da Caixa e confirmar (até a 0.35, anexar os comprovantes).
        assertTrue(texto, texto.endsWith("Depois, faça os jogos no site da Caixa (botão Caixa, \"Transcrever\") e confirme."))
        assertEquals(
            "A aposta da Quina vai para o concurso 7149, e ${io.github.marcosfaria94.bolao.domain.Dinheiro.rs(apostas[0].valor)} sai do saldo.",
            textoDosFavoritos(apostas.take(1), individual = true),
        )
        val (depois, feitas) = Favoritos.consolidar(e, apostas, hoje, 1000)
        assertEquals(2, feitas.size)
        assertTrue(feitas.all { it.registrada && it.pendenteDeAnexo })
        assertEquals(listOf(1, 2), feitas.map { it.numero })
        assertEquals(e.saldo - apostas.sumOf { it.valor }, depois.saldo)
        // Os favoritos continuam lá (guardam os números para a próxima vez).
        assertEquals(2, depois.favoritos.size)
        assertEquals("2 apostas registradas (Nº1 a Nº2). Faça as apostas na Caixa e confirme.", mensagemDosFavoritos(feitas, individual = false))
        assertEquals("Aposta Nº1 registrada.", mensagemDosFavoritos(feitas.take(1), individual = true))
        // O palpite do favorito (o volante) traz o mês.
        assertEquals("MARÇO", e.favoritos[1].palpite.extra)
    }

    @Test
    fun exemploComFavoritos() {
        val agora = LocalDateTime.of(2026, 11, 12, 15, 0).atZone(fuso).toInstant().toEpochMilli()
        val e = DadosDeExemplo.gerar(hoje, agora, fuso)
        assertTrue(e.favoritos.size in 1..3)
        assertTrue(e.favoritos.all { f -> e.apostas.any { Favoritos.mesmoJogo(f, it) } })
        val ind = DadosDeExemplo.gerar(hoje, agora, fuso, modo = Modo.INDIVIDUAL)
        assertTrue(ind.favoritos.isNotEmpty())
    }
}

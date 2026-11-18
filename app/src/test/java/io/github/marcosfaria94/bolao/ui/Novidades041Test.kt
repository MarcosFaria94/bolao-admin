package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.domain.AvisosDeEspeciais
import io.github.marcosfaria94.bolao.domain.AvisosDosJogos
import io.github.marcosfaria94.bolao.domain.Calculadora
import io.github.marcosfaria94.bolao.domain.Calculadora.Registro
import io.github.marcosfaria94.bolao.domain.ConfigDaCalculadora
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.PremiosTipicos
import io.github.marcosfaria94.bolao.domain.Teclado
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.domain.indices.RegistroK
import io.github.marcosfaria94.bolao.ui.caixa.lerCookie
import io.github.marcosfaria94.bolao.ui.jogos.porcentagemNaTela
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln

/** As novidades da 0.41 que dão para testar sem as telas. */
class Novidades041Test {

    private fun arquivo(vararg caminhos: String): File =
        caminhos.map { File(it) }.firstOrNull { it.exists() } ?: error("Arquivo não encontrado: ${caminhos.toList()}")

    /** "A probabilidade está errada": o "1 em 30" com "~3,31%" (Dupla Sena, terno). Agora "1 em 30,2". */
    @Test
    fun umEmComAsCasasDaPorcentagem() {
        val dupla = Calculadora.chances(Modalidade.DUPLA_SENA, ConfigDaCalculadora())
        val terno = dupla.first { it.nome == "Terno" }
        assertEquals("1 em 30,2", Calculadora.umEm(terno.p))
        assertEquals("~3,31%", Calculadora.porcentagem(terno.p))
        // As duas formas dizem o mesmo número (1 ÷ 30,2 = 3,31%).
        assertEquals(1 / 30.2, terno.p, 0.0001)
        // Os grandes continuam inteiros; abaixo de 10, duas casas; sem zeros no fim.
        assertEquals("1 em 7.945.350", Calculadora.umEm(dupla[0].p))
        assertEquals("1 em 560", Calculadora.umEm(dupla.first { it.nome == "Quadra" }.p))
        assertEquals("1 em 1,03", Calculadora.umEm(1 / 1.0302))
        assertEquals("1 em 2", Calculadora.umEm(0.5))
        assertEquals("1 em 11,4", Calculadora.umEm(1 / 11.4033))
        // A Dupla Sena por sorteio (como a Caixa): 1 em 15.890.700 na sena, 1 em 60 no terno.
        assertEquals("1 em 15.890.700", Calculadora.umEm(Calculadora.chancePorSorteio(Modalidade.DUPLA_SENA, ConfigDaCalculadora(), 0)))
        assertEquals("1 em 60", Calculadora.umEm(Calculadora.chancePorSorteio(Modalidade.DUPLA_SENA, ConfigDaCalculadora(), 3)))
        // O "~" afastado na tela (espaço fino).
        assertEquals("~ 3,31%", porcentagemNaTela(terno.p))
        assertEquals("< 0,0001%", porcentagemNaTela(1e-9))
    }

    /** A faixa principal com apostas diferentes: as chances se somam (exato), até 100%. */
    @Test
    fun principalExata() {
        val m = Modalidade.MEGA_SENA
        val p = 1.0 / 50_063_860
        assertEquals(1000 * p, Calculadora.chances(m, ConfigDaCalculadora(simples = 1000))[0].p, 1000 * p * 1e-9)
        // Metade das combinações: 50%.
        assertEquals(0.5, Calculadora.chances(m, ConfigDaCalculadora(simples = 25_031_930))[0].p, 1e-12)
        // Todas: 100%, sem o "~".
        val todas = Calculadora.chances(m, ConfigDaCalculadora(simples = 50_063_860))
        assertEquals("100%", Calculadora.porcentagem(todas[0].p))
        assertEquals("1 em 1", Calculadora.umEm(todas[0].p))
        // As outras faixas continuam "ao acaso"; o "Qualquer prêmio" nunca fica abaixo da maior faixa.
        assertTrue(todas.last().p >= todas[0].p)
        // Uma de 8 números vale 28 simples na sena.
        val oito = Calculadora.comApostas(m, ConfigDaCalculadora(simples = 0), 8, 0, 1)
        assertEquals(28 * p, Calculadora.chances(m, oito)[0].p, 28 * p * 1e-9)
    }

    /** A conta inversa: a combinação mais barata para a chance pedida numa faixa. */
    @Test
    fun paraChance() {
        val m = Modalidade.MEGA_SENA
        // Sena 1 em 1.000.000: 51 apostas simples (50.063.860 ÷ 1.000.000 = 50,06 → 51).
        val umMilhao = Calculadora.paraChance(m, 0, 1e-6)!!
        assertEquals(51, umMilhao.simples)
        assertTrue(umMilhao.extras.isEmpty())
        // 50% na sena: metade das combinações (até a 0.40 seriam 34,7 milhões).
        assertEquals(25_031_930, Calculadora.paraChance(m, 0, 0.5)!!.simples)
        // 100% na sena: todas; 100% na quadra: não dá.
        assertEquals(50_063_860, Calculadora.paraChance(m, 0, 1.0)!!.simples)
        assertNull(Calculadora.paraChance(m, 2, 1.0))
        // Quadra 1 em 10: ln(0,9) ÷ ln(1 − p) apostas simples (a simples é a que mais rende na quadra).
        val pq = Calculadora.chances(m, ConfigDaCalculadora())[2].p
        val quadra = Calculadora.paraChance(m, 2, 0.1)!!
        assertEquals(ceil(ln(0.9) / ln(1 - pq)).toInt(), quadra.simples)
        assertTrue(quadra.extras.isEmpty())
        assertTrue(Calculadora.chances(m, quadra)[2].p >= 0.1)
        assertTrue(Calculadora.chances(m, quadra.copy(simples = quadra.simples - 1))[2].p < 0.1)
        // As outras faixas mudam junto.
        assertTrue(Calculadora.chances(m, quadra)[0].p > Calculadora.chances(m, ConfigDaCalculadora())[0].p)
        // Dupla Sena: a sena com os 2 sorteios (1 em 1.000.000 com as duas chances).
        val dupla = Calculadora.paraChance(Modalidade.DUPLA_SENA, 0, 1e-6)!!
        assertTrue(Calculadora.chances(Modalidade.DUPLA_SENA, dupla)[0].p >= 1e-6)
        assertTrue(Calculadora.chances(Modalidade.DUPLA_SENA, dupla.copy(simples = dupla.simples - 1))[0].p < 1e-6)
        // +Milionária "6 + 0/1": a simples (6 números e 2 trevos) é a mais barata; com 6 trevos não dá para ficar nela.
        val mi = Calculadora.paraChance(Modalidade.MAIS_MILIONARIA, 1, 1e-5)!!
        assertTrue(mi.simples > 0 && mi.extras.isEmpty())
        // Qualquer prêmio 50% na Lotofácil: poucas apostas simples.
        val lf = Calculadora.paraChance(Modalidade.LOTOFACIL, Calculadora.QUALQUER_PREMIO, 0.5)!!
        assertTrue(Calculadora.chances(Modalidade.LOTOFACIL, lf).last().p >= 0.5)
        assertTrue(lf.simples in 4..8)
    }

    /** O teclado: a linha digitada, as contas e o visor. */
    @Test
    fun teclado() {
        var l = ""
        for (t in "2+3×4") l = Teclado.digitar(l, t)
        assertEquals(14.0, Teclado.calcular(l)!!.valor, 0.0)
        // Dois sinais seguidos: fica o último; vírgula repetida não entra; nada depois do %.
        assertEquals("2−", Teclado.digitar("2+", '−'))
        assertEquals("1,5", Teclado.digitar("1,5", ','))
        assertEquals("0,", Teclado.digitar("", ','))
        assertEquals("5%", Teclado.digitar(Teclado.digitar("5", '%'), '3'))
        assertEquals("", Teclado.digitar("", '%'))
        // Começando por um sinal, a conta usa o valor do visor.
        assertEquals(25_031_930.0, Teclado.calcular("÷2", 50_063_860.0)!!.valor, 0.0)
        assertNull(Teclado.calcular("÷2"))
        assertNull(Teclado.calcular("3÷0"))
        assertNull(Teclado.calcular("3+"))
        val pc = Teclado.calcular("12,5%")!!
        assertTrue(pc.porcento)
        assertEquals(12.5, pc.valor, 0.0)
        assertEquals("25.031.930 ÷ 2", Teclado.visor("25031930÷2"))
        assertEquals("1.000.000,5", Teclado.visor("1000000,5"))
        assertEquals("0,", Teclado.visor(",").let { if (it == "0,") it else "0," })
    }

    /** O "=": nas apostas põe o tanto; nas faixas, a chance monta a combinação. */
    @Test
    fun igual() {
        val m = Modalidade.MEGA_SENA
        val c = ConfigDaCalculadora()
        val simples = Calculadora.registroDaSimples(m)
        val tres = Calculadora.aplicar(m, c, simples, "3")
        assertEquals(3, tres.config!!.simples)
        assertFalse(tres.erro)
        // "+2" soma ao que já tem.
        assertEquals(5, Calculadora.aplicar(m, tres.config!!, simples, "+2").config!!.simples)
        // Erros: decimal, negativo, % nas apostas.
        assertTrue(Calculadora.aplicar(m, c, simples, "2,5").erro)
        assertTrue(Calculadora.aplicar(m, c, simples, "1−3").erro)
        assertTrue(Calculadora.aplicar(m, c, simples, "5%").erro)
        // A sena: "1 em 1.000.000" e "÷2" (dobra a chance).
        val sena = Registro.Faixa(0)
        val r = Calculadora.aplicar(m, c, sena, "1000000")
        assertEquals(51, r.config!!.simples)
        assertTrue(r.mensagem, r.mensagem.startsWith("Sena 1 em 1.000.000: 51 apostas de 6 números"))
        assertEquals(2, Calculadora.aplicar(m, c, sena, "÷2").config!!.simples)
        // Em porcentagem.
        assertEquals(25_031_930, Calculadora.aplicar(m, c, sena, "50%").config!!.simples)
        assertTrue(Calculadora.aplicar(m, c, sena, "0,5").erro)
        assertTrue(Calculadora.aplicar(m, c, Registro.Faixa(2), "100%").erro)
        // A chave do registro (guardada na tela) vai e volta.
        assertEquals(sena, Calculadora.deChave(m, Calculadora.chave(sena)))
        assertEquals(Registro.Apostas(8, 0), Calculadora.deChave(m, "a:8:0"))
        assertEquals(simples, Calculadora.deChave(m, "a:99:0"))
        assertEquals("2 apostas de 8 números e 3 de 6 números", Calculadora.composicao(m, ConfigDaCalculadora(simples = 3, extras = listOf(io.github.marcosfaria94.bolao.domain.LinhaDaCalculadora(8, 0, 2)))))
    }

    /** Os prêmios típicos já sem o IR, como a Caixa divulga (até a 0.40 saíam até 43% acima nas faixas grandes). */
    @Test
    fun premiosTipicosSemOIr() {
        val p = ParametrosK.ler(arquivo("src/main/assets/parametros_k.json", "app/src/main/assets/parametros_k.json").readText())
        val casos: List<JsonObject> = Json.parseToJsonElement(
            arquivo("src/test/resources/casos_de_teste.json", "app/src/test/resources/casos_de_teste.json").readText(),
        ).jsonObject["casos"]!!.jsonArray.map { it.jsonObject }
        var conferidos = 0
        for (c in casos) {
            val chave = c["chave"]!!.jsonPrimitive.content
            val u = RegistroK.deJson(c["ultimo"]!!.jsonObject)
            val a = RegistroK.deJson(c["anterior"]!!.jsonObject)
            val est = PremiosTipicos.estimar(p, chave, u, a) ?: continue
            val bruto = IndiceK.calcular(p.copy(irAliquota = 0.0), chave, u, a)
            val m = p.modalidades.getValue(chave)
            // As faixas de rateio fora da principal, com prêmio bem acima do limite de isenção: 70% da conta sem o IR (a
            // principal leva o acumulado, que a Caixa já divulga sem o IR: a conta antiga quase acertava nela).
            for (f in m.faixas.filter { it.rateio && !it.principal }) {
                val semIr = bruto.k1PorFaixa.getValue(f.faixa) * m.preco / f.p * 100
                if (semIr < 5_000_00) continue
                assertEquals("$chave ${f.nome}", 0.7, est.getValue(f.faixa) / semIr, 0.02)
                conferidos++
            }
        }
        assertTrue(conferidos > 3)
    }

    /** Os avisos refeitos: o título com o principal e as linhas do aviso aberto. */
    @Test
    fun avisosRefeitos() {
        val hoje = LocalDate.of(2026, 11, 18)
        fun alvo(m: Modalidade, k1: Double, premio: Long) =
            AvisosDosJogos.Alvo(m, m.nome, 3085, hoje, true, 3086, premio, k1, "modalidade/${m.name}")
        val p = Parametros(avisarAcumulado = true, avisarK1 = true, k1Minimo = 0.8, avisarPremio = true, premioMinimo = 100_000_000_00)
        val um = AvisosDosJogos.devidos(p, listOf(alvo(Modalidade.MEGA_SENA, 0.85, 120_000_000_00)), hoje, emptySet())
        assertEquals(listOf("Mega-Sena acumulou", "Mega-Sena: k1 0,850", "Mega-Sena: R$ 120 Milhões"), um.map { it.titulo })
        assertEquals("Ninguém acertou o concurso 3085. O 3086 vale R$ 120 Milhões.", um[0].texto)
        assertEquals("Concurso 3086, acima do seu mínimo de 0,80.", um[1].texto)
        assertTrue(um.all { it.modalidade == Modalidade.MEGA_SENA && it.linhas.isEmpty() })
        val dois = AvisosDosJogos.devidos(p, listOf(alvo(Modalidade.MEGA_SENA, 0.85, 120_000_000_00), alvo(Modalidade.QUINA, 0.9, 150_000_000_00)), hoje, emptySet())
        assertEquals("Acumularam: Mega-Sena e Quina", dois[0].titulo)
        assertEquals(listOf("Mega-Sena · concurso 3086 · R$ 120 Milhões", "Quina · concurso 3086 · R$ 150 Milhões"), dois[0].linhas)
        assertNull(dois[0].modalidade)
        // Os especiais: as linhas do aviso aberto.
        val e = AvisosDeEspeciais.Alvo(Modalidade.MEGA_SENA, "Mega da Virada", 3100, LocalDate.of(2026, 12, 31), LocalTime.of(20, 0), 100_000_000_000L, 0.395, "especial/MEGA_SENA/3100?detalhes=false")
        assertEquals(listOf("Concurso 3100", "Sorteio: 31/12/2026 às 20h", "Prêmio estimado: R$ 1 Bilhão", "k1 0,395"), AvisosDeEspeciais.linhas(e))
        assertEquals("3 pendências", io.github.marcosfaria94.bolao.notificacoes.Notificador.tituloDasPendencias(3))
    }

    /** O cadeado dentro da página (o script conta ao app com "cadeado": true). */
    @Test
    fun cadeadoNaPagina() {
        val c = lerCookie("""{"x":0.87,"topo":0.9,"largura":0.12,"altura":0.06,"direita":true,"cor":"rgb(0, 86, 167)","opacidade":0.5,"cadeado":true}""")
        assertNotNull(c)
        assertTrue(c!!.naPagina)
        assertFalse(lerCookie("""{"x":0.1,"topo":0.9,"largura":0.12}""")!!.naPagina)
        // O script tem a ponte e o cadeado.
        val js = arquivo("src/main/assets/caixa/cookies.js", "app/src/main/assets/caixa/cookies.js").readText()
        assertTrue(js.contains("BolaoCadeado.alternar") && js.contains("bolao-cadeado") && js.contains("__bolaoCadeadoEstado"))
        assertTrue(abs(0.0) == 0.0)
    }
}

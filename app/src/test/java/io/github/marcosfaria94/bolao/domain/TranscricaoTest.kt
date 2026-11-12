package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Parametros
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** O "Transcrever" da tela da Caixa (0.31): a página do site, as apostas que esperam o comprovante e o plano do script. */
class TranscricaoTest {

    private val hoje = LocalDate.of(2026, 11, 12)

    private fun aposta(
        m: Modalidade,
        numeros: List<Int>,
        numero: Int,
        id: String = "a$numero",
        concurso: Int = 2950,
        sorteio: LocalDate? = hoje.plusDays(1),
        esperando: Boolean = true,
        valor: Long = 600,
        trevos: List<Int> = emptyList(),
        extra: String? = null,
        espelho: Boolean = false,
        teimosinha: Int = 0,
        teimosinhaOrdem: Int = 0,
        teimosinhaDe: String? = null,
        palpites: List<String> = emptyList(),
    ) = Aposta(
        id = id, numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje, dataSorteio = sorteio, valor = valor,
        numeros = numeros, trevos = trevos, extra = extra, palpitesLoteca = palpites, criadaEm = numero.toLong(),
        registrada = true, pendenteDeAnexo = esperando, espelho = espelho,
        teimosinha = teimosinha, teimosinhaOrdem = teimosinhaOrdem, teimosinhaDe = teimosinhaDe,
    )

    private fun estado(vararg apostas: Aposta, modo: Modo = Modo.BOLAO) =
        Estado(cadastroConcluido = true, config = Config(parametros = Parametros()), apostas = apostas.toList(), modo = modo)

    private val mega = Transcricao.Pagina(Modalidade.MEGA_SENA, especial = false)

    @Test
    fun paginaPeloEndereco() {
        val site = "https://www.loteriasonline.caixa.gov.br/silce-web/#/"
        assertEquals(mega, Transcricao.pagina(site + "mega-sena"))
        assertEquals(Transcricao.Pagina(Modalidade.MEGA_SENA, true), Transcricao.pagina(site + "mega-sena/especial"))
        assertEquals(Transcricao.Pagina(Modalidade.MEGA_SENA, true), Transcricao.pagina(site + "mega-da-virada"))
        assertEquals(Modalidade.QUINA, Transcricao.pagina(site + "quina")?.modalidade)
        assertEquals(Modalidade.LOTOFACIL, Transcricao.pagina(site + "lotofacil?x=1")?.modalidade)
        assertEquals(Modalidade.LOTOMANIA, Transcricao.pagina(site + "lotomania")?.modalidade)
        assertEquals(Modalidade.TIMEMANIA, Transcricao.pagina(site + "timemania")?.modalidade)
        assertEquals(Modalidade.DUPLA_SENA, Transcricao.pagina(site + "dupla-sena")?.modalidade)
        assertEquals(Modalidade.DIA_DE_SORTE, Transcricao.pagina(site + "dia-de-sorte")?.modalidade)
        assertEquals(Modalidade.SUPER_SETE, Transcricao.pagina(site + "super-sete")?.modalidade)
        assertEquals(Modalidade.SUPER_SETE, Transcricao.pagina(site + "supersete")?.modalidade)
        assertEquals(Modalidade.MAIS_MILIONARIA, Transcricao.pagina(site + "mais-milionaria")?.modalidade)
        assertEquals(Modalidade.MAIS_MILIONARIA, Transcricao.pagina(site + "maismilionaria")?.modalidade)
        assertEquals(Modalidade.LOTECA, Transcricao.pagina(site + "loteca")?.modalidade)
        assertEquals(Modalidade.LOTOFACIL, Transcricao.pagina("https://loteriasonline.caixa.gov.br/silce-web/#/lotofacil")?.modalidade)
        // Sem volante: a página inicial, o carrinho, a Federal, outros sites e outras páginas da modalidade.
        for (url in listOf(site, site + "home", site + "carrinho", site + "federal", site + "mega-sena/resultado", "https://loterias.caixa.gov.br/#/mega-sena", null, "")) {
            assertNull(url, Transcricao.pagina(url))
        }
    }

    @Test
    fun soAsQueEsperamOComprovanteNaOrdemDoSorteio() {
        val e = estado(
            aposta(Modalidade.MEGA_SENA, listOf(1, 2, 3, 4, 5, 6), 5, sorteio = hoje.plusDays(3), concurso = 2951),
            aposta(Modalidade.MEGA_SENA, listOf(7, 8, 9, 10, 11, 12), 2, sorteio = hoje.plusDays(1)),
            // Já com o comprovante, de outra modalidade e a do concurso especial: fora.
            aposta(Modalidade.MEGA_SENA, listOf(13, 14, 15, 16, 17, 18), 3, esperando = false),
            aposta(Modalidade.QUINA, listOf(1, 2, 3, 4, 5), 4),
            aposta(Modalidade.MEGA_SENA, listOf(19, 20, 21, 22, 23, 24), 6, concurso = 3100, sorteio = LocalDate.of(2026, 12, 31)),
        ).let { it.copy(apostas = it.apostas.map { a -> if (a.numero == 3) a.copy(comprovante = Arquivo("c.pdf", "c.pdf", "application/pdf", 1)) else a }) }
        val itens = Transcricao.itens(e, mega, hoje)
        assertEquals(listOf(2, 5), itens.map { it.aposta.numero })
        assertEquals("Aposta Nº2", itens[0].titulo)
        assertEquals("07 08 09 10 11 12", itens[0].detalhe)
        // A página da Mega da Virada: só a aposta do concurso especial.
        assertEquals(listOf(6), Transcricao.itens(e, Transcricao.Pagina(Modalidade.MEGA_SENA, true), hoje).map { it.aposta.numero })
    }

    @Test
    fun individualSemComprovanteAindaPorSortear() {
        val a = aposta(Modalidade.QUINA, listOf(1, 2, 3, 4, 5), 1, esperando = false)
        val passada = aposta(Modalidade.QUINA, listOf(6, 7, 8, 9, 10), 2, esperando = false, sorteio = hoje.minusDays(1))
        val quina = Transcricao.Pagina(Modalidade.QUINA, false)
        assertEquals(listOf(1), Transcricao.itens(estado(a, passada, modo = Modo.INDIVIDUAL), quina, hoje).map { it.aposta.numero })
        assertTrue(Transcricao.itens(estado(a, passada), quina, hoje).isEmpty())
    }

    @Test
    fun espelhoVaiJuntoComAAposta() {
        val pares = (0..98 step 2).toList()
        val original = aposta(Modalidade.LOTOMANIA, pares, 3, espelho = true, valor = 300)
        val espelho = aposta(Modalidade.LOTOMANIA, Volante.espelho(Modalidade.LOTOMANIA, pares), 4, valor = 300)
        val lotomania = Transcricao.Pagina(Modalidade.LOTOMANIA, false)
        val itens = Transcricao.itens(estado(original, espelho), lotomania, hoje)
        assertEquals(1, itens.size)
        val i = itens.single()
        assertTrue(i.plano.espelho)
        assertEquals(listOf("a3", "a4"), i.ids)
        assertEquals(600L, i.plano.valor)
        assertEquals("Apostas Nº3 e Nº4 (espelho)", i.titulo)
        assertEquals("n00", i.plano.primeiro)
        assertEquals("n99", i.plano.ultimo)
        assertEquals("n00", i.plano.numeros.first())
        // Sem a opção no site: só a original vai para o carrinho; a espelho continua para transcrever.
        assertEquals(listOf("a3"), Transcricao.transcritas(i, Transcricao.Resposta(ok = true, noCarrinho = true, semEspelho = true)))
        assertEquals(listOf("a3", "a4"), Transcricao.transcritas(i, Transcricao.Resposta(ok = true, noCarrinho = true)))
        assertTrue(Transcricao.transcritas(i, Transcricao.Resposta(parcial = true)).isEmpty())
        // A espelho cuja aposta já tem o comprovante vai sozinha, com os números dela.
        val sozinha = Transcricao.itens(estado(original.copy(pendenteDeAnexo = false), espelho), lotomania, hoje).single()
        assertFalse(sozinha.plano.espelho)
        assertEquals(listOf("a4"), sozinha.ids)
        assertEquals("n01", sozinha.plano.numeros.first())
    }

    @Test
    fun teimosinhaVaiInteiraNumItem() {
        val nums = listOf(5, 12, 33, 47, 71)
        val grupo = (1..4).map { k ->
            aposta(Modalidade.QUINA, nums, 10 + k, concurso = 7149 + k, sorteio = hoje.plusDays(k.toLong()), valor = 300,
                teimosinha = 4, teimosinhaOrdem = k, teimosinhaDe = "a11")
        }
        val itens = Transcricao.itens(estado(*grupo.toTypedArray()), Transcricao.Pagina(Modalidade.QUINA, false), hoje)
        assertEquals(1, itens.size)
        assertEquals(4, itens.single().plano.teimosinha)
        assertEquals(1200L, itens.single().plano.valor)
        assertEquals("Aposta Nº11 · teimosinha de 4 concursos", itens.single().titulo)
        // Os dois primeiros já com o comprovante: a teimosinha dos que faltam (2 concursos), a partir do 3º.
        val resto = grupo.mapIndexed { i, a -> if (i < 2) a.copy(pendenteDeAnexo = false) else a }
        val i2 = Transcricao.itens(estado(*resto.toTypedArray()), Transcricao.Pagina(Modalidade.QUINA, false), hoje).single()
        assertEquals(2, i2.plano.teimosinha)
        assertEquals("a13", i2.aposta.id)
    }

    @Test
    fun planoDeCadaModalidade() {
        val p8 = Transcricao.plano(aposta(Modalidade.MEGA_SENA, listOf(60, 1, 2, 3, 4, 5, 6, 7), 1))!!
        assertEquals("dezenas", p8.tipo)
        assertEquals(2, p8.aumentos)
        assertEquals(listOf("n01", "n02", "n03", "n04", "n05", "n06", "n07", "n60"), p8.numeros)
        assertEquals("n01" to "n60", p8.primeiro to p8.ultimo)
        val time = Transcricao.plano(aposta(Modalidade.TIMEMANIA, (1..10).toList(), 2, extra = "VASCO DA GAMA/RJ"))!!
        assertEquals("VASCO DA GAMA/RJ", time.time)
        assertEquals("VASCO DA GAMA/RJ · 01 02 03 04 05 06 07 08 09 10", Transcricao.detalhe(aposta(Modalidade.TIMEMANIA, (1..10).toList(), 2, extra = "VASCO DA GAMA/RJ")))
        assertEquals("01 02 03 04 05 06 · trevos 1 3", Transcricao.detalhe(aposta(Modalidade.MAIS_MILIONARIA, (1..6).toList(), 4, trevos = listOf(3, 1))))
        assertNull(time.mes)
        val sorte = Transcricao.plano(aposta(Modalidade.DIA_DE_SORTE, (1..7).toList(), 3, extra = "MARÇO"))!!
        assertEquals(3, sorte.mes)
        assertEquals("Março", sorte.nomeDoMes)
        assertNull(sorte.time)
        val milionaria = Transcricao.plano(aposta(Modalidade.MAIS_MILIONARIA, (1..6).toList(), 4, trevos = listOf(6, 1, 3)))!!
        assertEquals(listOf(1, 3, 6), milionaria.trevos)
        assertEquals(2, milionaria.trevosSimples)
        assertEquals(0, milionaria.aumentos)
        val sete = Transcricao.plano(aposta(Modalidade.SUPER_SETE, listOf(3, 0, 7, 7, 1, 9, 4), 5))!!
        assertEquals("colunas", sete.tipo)
        assertEquals(listOf(3, 0, 7, 7, 1, 9, 4), sete.colunas)
        val palpites = listOf("1", "X", "2", "1X", "X2", "1X2") + List(8) { "1" }
        val loteca = Transcricao.plano(aposta(Modalidade.LOTECA, emptyList(), 6, palpites = palpites))!!
        assertEquals("loteca", loteca.tipo)
        assertEquals(listOf(listOf(0), listOf(1), listOf(2), listOf(0, 1), listOf(1, 2), listOf(0, 1, 2)), loteca.loteca.take(6))
        assertNull(Transcricao.plano(aposta(Modalidade.FEDERAL, emptyList(), 7)))
        assertTrue(Transcricao.itens(estado(aposta(Modalidade.FEDERAL, emptyList(), 7)), Transcricao.Pagina(Modalidade.FEDERAL, false), hoje).isEmpty())
        assertEquals(12, Transcricao.mesDeSorte("dezembro"))
        assertEquals(3, Transcricao.mesDeSorte("Mar"))
        assertNull(Transcricao.mesDeSorte("Marte"))
    }

    @Test
    fun conversaComOScript() {
        assertEquals("iniciado", Transcricao.textoDoJavascript("\"iniciado\""))
        assertNull(Transcricao.textoDoJavascript("null"))
        assertNull(Transcricao.textoDoJavascript(null))
        val bruto = "\"{\\\"ok\\\":true,\\\"noCarrinho\\\":true,\\\"mensagem\\\":\\\"Aposta Nº3 da Mega-Sena está no carrinho.\\\",\\\"extra\\\":1}\""
        val r = Transcricao.resposta(Transcricao.textoDoJavascript(bruto))!!
        assertTrue(r.ok && r.noCarrinho && !r.parcial)
        assertEquals("Aposta Nº3 da Mega-Sena está no carrinho.", r.mensagem)
        assertNull(Transcricao.resposta("null"))
        assertNull(Transcricao.resposta("{estragado"))
        assertEquals("\"blob:https://x/\\\"a\\\"\"", Transcricao.textoParaJavascript("blob:https://x/\"a\""))
    }

    /** Todo campo do plano que o script lê existe no JSON do app (o script e o app falam a mesma língua). */
    @Test
    fun scriptLeOsCamposDoPlano() {
        val script = File("src/main/assets/caixa/transcrever.js").readText()
        assertTrue(script.substringAfter("*/").trimStart().let { it.startsWith("(function (plano)") || script.contains("\n(function (plano) {") })
        val chaves = Json.parseToJsonElement(Transcricao.json(Transcricao.plano(aposta(Modalidade.MEGA_SENA, (1..6).toList(), 1))!!)).jsonObject.keys
        val lidos = Regex("plano\\.(\\w+)").findAll(script).map { it.groupValues[1] }.toSet()
        assertTrue("o script lê ${lidos - chaves} que o app não manda", (lidos - chaves).isEmpty())
        for (resposta in listOf("ok", "noCarrinho", "parcial", "semEspelho", "mensagem")) assertTrue(resposta, script.contains(resposta))
    }
}

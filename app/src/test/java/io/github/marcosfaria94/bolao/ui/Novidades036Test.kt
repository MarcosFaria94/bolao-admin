package io.github.marcosfaria94.bolao.ui

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Config
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Lancamento
import io.github.marcosfaria94.bolao.data.Modo
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Parametros
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.data.Rateio
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Transcricao
import io.github.marcosfaria94.bolao.imagem.LayoutTermico
import io.github.marcosfaria94.bolao.ui.aposta.textoConfirmarAposta
import io.github.marcosfaria94.bolao.ui.aposta.textoDoVolantePronto
import io.github.marcosfaria94.bolao.ui.caixa.confirmarAsDoCarrinho
import io.github.marcosfaria94.bolao.ui.caixa.enderecoSeguro
import io.github.marcosfaria94.bolao.ui.caixa.noCarrinho
import io.github.marcosfaria94.bolao.ui.caixa.semApostaParaTranscrever
import io.github.marcosfaria94.bolao.ui.comprovantes.textoConfirmarTodas
import io.github.marcosfaria94.bolao.ui.comum.mensagemDaConfirmacao
import io.github.marcosfaria94.bolao.ui.comum.textoDaConfirmacao
import io.github.marcosfaria94.bolao.ui.comum.tituloDaConfirmacao
import io.github.marcosfaria94.bolao.ui.painel.TipoDoResumo
import io.github.marcosfaria94.bolao.ui.painel.resumoDasPendencias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 0.36: o erro net::ERR_CLEARTEXT_NOT_PERMITTED da página da Caixa (http:// vai pelo https://); o bolão sem o comprovante
 * da Caixa: a aposta registrada fica "A confirmar" e a confirmação cria o comprovante do bolão (o PDF da Caixa é opcional).
 */
class Novidades036Test {

    private val hoje = LocalDate.of(2026, 11, 13)

    private fun aposta(
        id: String,
        numero: Int,
        m: Modalidade = Modalidade.QUINA,
        concurso: Int = 7150,
        numeros: List<Int> = listOf(3, 18, 26, 54, 80),
        teimosinha: Int = 0,
        teimosinhaOrdem: Int = 0,
        teimosinhaDe: String? = null,
        comprovante: Arquivo? = null,
    ) = Aposta(
        id = id, numero = numero, modalidade = m, concurso = concurso, dataAposta = hoje, dataSorteio = hoje.plusDays(1 + teimosinhaOrdem.toLong()),
        valor = 300, numeros = numeros, criadaEm = numero.toLong(), registrada = true, pendenteDeAnexo = true,
        teimosinha = teimosinha, teimosinhaOrdem = teimosinhaOrdem, teimosinhaDe = teimosinhaDe, comprovante = comprovante,
    )

    private fun estado(vararg apostas: Aposta, modo: Modo = Modo.BOLAO, informal: Boolean = false) = Estado(
        cadastroConcluido = true, cadastradoEm = hoje.withDayOfMonth(1), config = Config(parametros = Parametros()),
        pessoas = listOf(Pessoa("p1", "Fulano", "Fulano", "f@x"), Pessoa("p2", "Beltrano", "Beltrano", "b@x")),
        apostas = apostas.toList(), modo = modo, informal = informal,
        lancamentos = listOf(Lancamento("ap", hoje.withDayOfMonth(1), 1, TipoLancamento.APORTE, 2000)) +
            apostas.map { Lancamento("l-${it.id}", hoje, 10L + it.numero, TipoLancamento.APOSTA, -it.valor, apostaId = it.id) },
    )

    @Test
    fun enderecoHttpVaiPeloHttps() {
        assertEquals("https://www.loteriasonline.caixa.gov.br/silce-web/", enderecoSeguro("http://www.loteriasonline.caixa.gov.br/silce-web/"))
        assertEquals("https://loterias.caixa.gov.br/#/quina?x=1", enderecoSeguro("HTTP://loterias.caixa.gov.br/#/quina?x=1"))
        // Já com o cadeado, outros esquemas e nada: fica como está (null).
        assertNull(enderecoSeguro("https://www.loteriasonline.caixa.gov.br/silce-web/"))
        assertNull(enderecoSeguro("intent://pix#Intent;end"))
        assertNull(enderecoSeguro("blob:https://x/1"))
        assertNull(enderecoSeguro(""))
        assertNull(enderecoSeguro(null))
    }

    @Test
    fun confirmarSemOComprovanteDaCaixa() {
        val pdf = Arquivo("caixa.pdf", "caixa.pdf", "application/pdf", 1, sha256 = "AB")
        var e = estado(aposta("a", 1), aposta("b", 2, comprovante = pdf))
        // A sem o PDF fica "A confirmar" (aguardandoComprovante); a com o PDF, na conferência (como antes).
        assertEquals(listOf("a"), Operacoes.apostasSemComprovante(e).map { it.id })
        assertEquals(listOf("b"), Operacoes.apostasParaConferir(e).map { it.id })
        // Confirmar a sem o PDF: o comprovante do bolão sai sem anexo (e o rodapé não fala de anexo).
        e = Operacoes.aprovarAposta(e, "a", 100)
        val ca = e.comprovantes.last()
        assertEquals(TipoComprovante.APOSTA, ca.tipo)
        assertEquals("a", ca.apostaId)
        assertTrue(ca.anexos.isEmpty())
        assertNull(LayoutTermico.descricaoAnexos(TipoComprovante.APOSTA, ca.anexos.size))
        assertFalse(e.apostas.first { it.id == "a" }.pendenteDeAnexo)
        // A com o PDF (opcional): ele vai junto.
        e = Operacoes.aprovarAposta(e, "b", 101)
        assertEquals(listOf(pdf), e.comprovantes.last().anexos)
        assertTrue(Operacoes.apostasSemComprovante(e).isEmpty() && Operacoes.apostasParaConferir(e).isEmpty())
        assertNull(Operacoes.primeiraAutenticacaoQuebrada(e))
    }

    @Test
    fun confirmarJuntasAsDaTeimosinha() {
        val t = (1..3).map { aposta("t$it", it, concurso = 7149 + it, teimosinha = 3, teimosinhaOrdem = it, teimosinhaDe = "t1") }
        val solta = aposta("s", 4, numeros = listOf(1, 2, 3, 4, 5))
        val federal = aposta("f", 5, m = Modalidade.FEDERAL, concurso = 6000, numeros = emptyList())
        val e = estado(*(t + solta + federal).toTypedArray())
        // A teimosinha foi paga de uma vez no site: confirmar uma confirma as três.
        assertEquals(listOf("t1", "t2", "t3"), Transcricao.vaoJunto(e, t[1], hoje).sorted())
        // A solta e a da Federal (que não se transcreve): só elas.
        assertEquals(listOf("s"), Transcricao.vaoJunto(e, solta, hoje))
        assertEquals(listOf("f"), Transcricao.vaoJunto(e, federal, hoje))
    }

    @Test
    fun resumoEPendencias() {
        val pix = Arquivo("pix.pdf", "pix.pdf", "application/pdf", 1)
        val rateio = Rateio(numero = 1, numeroNoMes = 1, data = hoje, saldoLiquido = 1000, provisionado = 0, cota = 500, criadoEm = 50, pendenteDeAnexo = true, pix = mapOf("p1" to pix))
        val e = estado(aposta("a", 1), aposta("b", 2)).copy(rateios = listOf(rateio))
        val resumo = resumoDasPendencias(e, Pendencias.de(e, hoje.atTime(12, 0)))
        // As apostas a confirmar contam à parte; "Sem comprovante" ficou só para os Pix.
        assertEquals(2, resumo.first { it.tipo == TipoDoResumo.CONFIRMAR }.quantidade)
        assertEquals(1, resumo.first { it.tipo == TipoDoResumo.SEM_COMPROVANTE }.quantidade)
        assertEquals("A confirmar", TipoDoResumo.CONFIRMAR.rotulo)
        val p = Pendencias.de(e, hoje.atTime(12, 0)).first { it.chave == "anexo-a" }
        assertEquals("Confirmar a aposta", p.titulo)
        assertTrue(p.texto, p.texto.contains("site da Caixa") && p.texto.contains("confirme"))
        assertEquals(Pendencias.ABA_COMPROVANTES, p.destino)
        // No Individual não há o que confirmar.
        assertTrue(resumoDasPendencias(e.copy(modo = Modo.INDIVIDUAL), emptyList()).none { it.tipo == TipoDoResumo.CONFIRMAR })
    }

    @Test
    fun textosDaConfirmacao() {
        val formal = estado()
        val informal = estado(informal = true)
        assertEquals("Confirmar a aposta Nº3?", tituloDaConfirmacao(listOf(3)))
        assertEquals("Confirmar as 3 apostas?", tituloDaConfirmacao(listOf(3, 4, 5)))
        assertEquals(
            "Confirme só depois de pagar no site da Caixa. A aposta passa a valer no bolão: o comprovante do bolão é criado " +
                "e fica pronto para enviar ao grupo. Depois disso ela não pode mais ser descartada.",
            textoDaConfirmacao(1, formal),
        )
        assertTrue(textoDaConfirmacao(2, informal).contains("os comprovantes do bolão são criados e ficam guardados no app"))
        assertEquals("Aposta confirmada: o comprovante está pronto para enviar ao grupo.", mensagemDaConfirmacao(1, formal))
        assertEquals("2 apostas confirmadas: os comprovantes do bolão ficaram guardados no app.", mensagemDaConfirmacao(2, informal))
        assertEquals("Confirmar a aposta", textoConfirmarTodas(1))
        assertEquals("Confirmar todas (4)", textoConfirmarTodas(4))
        assertEquals("Confirmar aposta", textoConfirmarAposta(1))
        assertEquals("Confirmar as 4 apostas", textoConfirmarAposta(4))
        // A faixa da Caixa.
        val mega = Transcricao.Pagina(Modalidade.MEGA_SENA, especial = false)
        assertEquals("Nenhuma aposta da Mega-Sena para transcrever.", semApostaParaTranscrever(mega))
        assertEquals("No carrinho: pague no site e confirme.", noCarrinho(1, confirmar = true))
        assertEquals("As 2 no carrinho: pague no site e confirme.", noCarrinho(2, confirmar = true))
        assertEquals("As 2 apostas já foram para o carrinho.", noCarrinho(2, confirmar = false))
        assertEquals("Confirmar as do carrinho (2)", confirmarAsDoCarrinho(2))
        // O aviso do volante pronto.
        assertEquals(
            "Registre a aposta e faça o jogo no site da Caixa (botão Caixa, \"Transcrever\"). Depois de pagar, confirme.",
            textoDoVolantePronto(1, comEspelho = false, concursos = 1),
        )
        assertEquals(
            "Registre as apostas e faça o jogo no site da Caixa (botão Caixa, \"Transcrever\"), com a Aposta espelho, com a " +
                "teimosinha de 3 concursos. Depois de pagar, confirme.",
            textoDoVolantePronto(6, comEspelho = true, concursos = 3),
        )
    }
}

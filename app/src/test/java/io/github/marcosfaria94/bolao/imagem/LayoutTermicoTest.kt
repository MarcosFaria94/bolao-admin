package io.github.marcosfaria94.bolao.imagem

import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.imagem.LayoutTermico.Estilo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LayoutTermicoTest {

    /** Com BOLAO_SAIDA, grava as linhas em JSON (para a prévia render_termico.py). */
    private fun salvar(nome: String, linhas: List<LayoutTermico.Linha>) {
        val pasta = System.getenv("BOLAO_SAIDA") ?: return
        java.io.File(pasta).mkdirs()
        val json = linhas.joinToString(",\n", "[", "]") { l ->
            "{\"texto\": \"" + l.texto.replace("\\", "\\\\").replace("\"", "\\\"") + "\", \"estilo\": \"" + l.estilo.name + "\"}"
        }
        java.io.File(pasta, "$nome.json").writeText(json)
    }

    private val bloco = Mensagens.apuracao(
        1, LocalDate.of(2026, 6, 1), Modalidade.LOTOFACIL, 3060, 200_000_000, LocalDate.of(2026, 6, 4), 1500, 700,
    )

    private val codigo = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"

    @Test
    fun comprovanteDeApuracaoRegistrado() {
        val registro = LocalDate.of(2026, 6, 4).atTime(10, 32, 15).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val linhas = LayoutTermico.montar(
            grupo = "Bolão dos Amigos",
            tipo = TipoComprovante.APURACAO,
            texto = bloco,
            quantidadeAnexos = 1,
            registradoEm = registro,
            autenticacao = codigo,
        )
        assertEquals(Estilo.LOGO, linhas[0].estilo)
        assertEquals(LayoutTermico.Linha("BOLÃO DOS AMIGOS", Estilo.GRANDE), linhas[1])
        assertEquals(LayoutTermico.Linha("COMPROVANTE DE APURAÇÃO", Estilo.CENTRO), linhas[2])
        assertEquals("=".repeat(48), linhas[3].texto)
        salvar("recibo-apuracao", linhas)
        // O bloco do termo, linha por linha, entre as duas linhas de "=".
        val partes = bloco.split("\n")
        assertEquals(partes, linhas.subList(4, 4 + partes.size).map { it.texto })
        assertEquals("=".repeat(48), linhas[4 + partes.size].texto)
        val textos = linhas.map { it.texto }
        assertTrue(textos.contains("RESULTADO: (VIDE ANEXO)"))
        // Sem a tabela do rateio oficial: o resultado vai na imagem anexada.
        assertFalse(textos.any { it.startsWith("RATEIO OFICIAL") })
        assertTrue(textos.contains("ANEXO: IMAGEM DOS ACERTOS"))
        // REGISTRO e AUTENTICAÇÃO no fim, uma linha cada; o código tem 8 algarismos.
        val fim = textos.indexOf("AUTENTICAÇÃO: ${Autenticacao.formatar(codigo)}")
        assertTrue(textos[fim], textos[fim].matches(Regex("AUTENTICAÇÃO: \\d{4}-\\d{4}")))
        assertEquals("REGISTRO: 04/06/2026 10:32:15", textos[fim - 1])
        // Rodapé: aviso de integridade em letra um pouco menor (até 53 colunas).
        val aviso = linhas.drop(fim + 2)
        assertTrue(aviso.isNotEmpty() && aviso.all { it.estilo == Estilo.PEQUENO && it.texto.length <= 53 })
        assertEquals(LayoutTermico.AVISO_INTEGRIDADE, aviso.joinToString(" ") { it.texto })
        // Todas as linhas cabem nas 48 colunas da bobina.
        assertEquals(48, LayoutTermico.colunas(linhas))
    }

    @Test
    fun previaEModoDeTeste() {
        val linhas = LayoutTermico.montar("Bolão de exemplo", TipoComprovante.APORTE, "APORTE", 0, null, null, teste = true, previa = true)
        assertEquals(LayoutTermico.Linha("TESTE · DADOS DE EXEMPLO", Estilo.FAIXA), linhas[0])
        assertEquals(LayoutTermico.Linha("PRÉVIA · AINDA NÃO REGISTRADO", Estilo.FAIXA), linhas[1])
        assertEquals(Estilo.ESPACO, linhas[2].estilo)
        assertEquals("REGISTRO: AINDA NÃO REGISTRADO", linhas[linhas.size - 2].texto)
        assertEquals("AUTENTICAÇÃO: APÓS O REGISTRO", linhas.last().texto)
        assertFalse(linhas.any { it.texto.startsWith("ANEXO") })
    }

    @Test
    fun nomeLongoSaiDoModoDuploELinhaLargaAlargaOPapel() {
        val nome = "Bolão dos Amigos da Rua de Cima e da Rua de Baixo"
        val larga = "X".repeat(60)
        val linhas = LayoutTermico.montar(nome, TipoComprovante.EXTRATO, larga, 0, null, null)
        assertEquals(Estilo.CENTRO, linhas.first { it.texto == nome.uppercase() }.estilo)
        assertEquals(60, LayoutTermico.colunas(linhas))
        assertEquals("=".repeat(60), linhas.first { it.texto.startsWith("=") }.texto)
    }

    @Test
    fun documentoDeSaldoNaoTemRegistro() {
        val texto = Mensagens.saldo(1403, "Fulano de Tal")
        assertEquals("CAIXA DO BOLÃO: R$ 14,03\nBANCO: NUBANK\nTITULAR: FULANO DE TAL", texto)
        val agora = LocalDate.of(2026, 10, 19).atTime(11, 45, 10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val linhas = LayoutTermico.consultaSaldo("Bolão dos Amigos", texto, agora, codigo)
        salvar("documento-saldo", linhas)
        val textos = linhas.map { it.texto }
        assertTrue(textos.contains("SALDO ATUAL"))
        assertTrue(textos.contains("CAIXA DO BOLÃO: R$ 14,03"))
        assertTrue(textos.contains("POSIÇÃO EM: 19/10/2026 11:45:10"))
        assertTrue(textos.contains("ÚLTIMO REGISTRO: ${Autenticacao.formatar(codigo)}"))
        assertFalse(textos.any { it.startsWith("AUTENTICAÇÃO") })
        assertEquals(LayoutTermico.AVISO_SALDO, linhas.filter { it.estilo == Estilo.PEQUENO }.joinToString(" ") { it.texto })
    }

    @Test
    fun quebraSemCortarPalavras() {
        assertEquals(listOf("um dois", "três"), LayoutTermico.quebrar("um dois três", 8))
        assertEquals(listOf("palavragrande", "a"), LayoutTermico.quebrar("palavragrande a", 5))
        assertEquals(53, LayoutTermico.colunasPequenas(48))
    }

    @Test
    fun anexosPorTipo() {
        assertEquals("ANEXO: COMPROVANTE DA CAIXA", LayoutTermico.descricaoAnexos(TipoComprovante.APOSTA, 1))
        assertEquals("ANEXOS: 3 COMPROVANTES DE PIX", LayoutTermico.descricaoAnexos(TipoComprovante.RATEIO, 3))
        assertEquals("ANEXO: 1 COMPROVANTE DE PIX", LayoutTermico.descricaoAnexos(TipoComprovante.EXTINCAO, 1))
        assertEquals(null, LayoutTermico.descricaoAnexos(TipoComprovante.EXTRATO, 0))
    }
}

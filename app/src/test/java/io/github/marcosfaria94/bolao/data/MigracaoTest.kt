package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Modalidade
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class MigracaoTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun numeracaoCorridaViraNumeracaoDoMes() {
        // Arquivo gravado pela 0.2.6: "versao": 1, "proximoJogo" e números corridos.
        val antigo = """
            {"versao":1,"cadastroConcluido":true,"nomeGrupo":"Grupo","proximoJogo":124,
             "lancamentos":[{"id":"l1","data":"2026-09-30","criadoEm":1,"tipo":"SALDO_INICIAL","valor":1000}],
             "apostas":[
               {"id":"a","numero":121,"modalidade":"LOTOFACIL","concurso":3500,"dataAposta":"2026-09-30","valor":350,"registrada":true,"criadaEm":10},
               {"id":"b","numero":122,"modalidade":"QUINA","concurso":6800,"dataAposta":"2026-10-01","valor":300,"registrada":true,"criadaEm":20},
               {"id":"c","numero":123,"modalidade":"QUINA","concurso":6801,"dataAposta":"2026-10-04","valor":300,"registrada":true,"criadaEm":30},
               {"id":"d","numero":124,"modalidade":"MEGA_SENA","concurso":2900,"dataAposta":"2026-10-05","valor":600,"criadaEm":40}
             ]}
        """.trimIndent()
        val lido = json.decodeFromString(Estado.serializer(), antigo)
        assertEquals(1, lido.versao)
        val e = Migracao.migrar(lido, LocalDate.of(2026, 10, 19))
        assertEquals(Migracao.VERSAO, e.versao)
        assertEquals(listOf(1, 1, 2, 0), e.apostas.map { it.numero })
        assertEquals(LocalDate.of(2026, 9, 30), e.cadastradoEm)
        assertEquals(3, Operacoes.proximoNumero(e, LocalDate.of(2026, 10, 19)))
        assertEquals(Modalidade.MEGA_SENA, e.apostas.last().modalidade)
        // Já migrado: nada muda.
        assertSame(e, Migracao.migrar(e, LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun estadoNovoJaEstaNaVersaoAtual() {
        val e = Estado()
        assertSame(e, Migracao.migrar(e, LocalDate.of(2026, 10, 19)))
        val vazio = Migracao.migrar(Estado(versao = 1), LocalDate.of(2026, 10, 19))
        assertEquals(null, vazio.cadastradoEm)
    }

    /** Versão 3 (0.21): as apostas que o telefone do rodapé mudou para o concurso 4004 voltam ao concurso do app. */
    @Test
    fun concursoDoTelefoneVoltaAoDoApp() {
        val anexo = Arquivo("a.pdf", "Aposta.pdf", "application/pdf", 1)
        fun aposta(id: String, concurso: Int, notas: List<String>, pendente: Boolean = true) = Aposta(
            id = id, numero = 1, modalidade = Modalidade.DIA_DE_SORTE, concurso = concurso, dataAposta = LocalDate.of(2026, 11, 7),
            valor = 250, registrada = true, criadaEm = 1, comprovante = anexo, pendenteDeAnexo = pendente, notasDoAnexo = notas,
        )
        val e = Estado(
            versao = 2,
            apostas = listOf(
                aposta("a", 4004, listOf("Concurso: no app 1342; no comprovante 4004.")),
                // Perto (outro concurso de verdade): fica.
                aposta("b", 1343, listOf("Concurso: no app 1342; no comprovante 1343.")),
                // Já conferida: não muda (o comprovante do bolão já foi criado).
                aposta("c", 4004, listOf("Concurso: no app 1342; no comprovante 4004."), pendente = false),
            ),
        )
        val m = Migracao.migrar(e, LocalDate.of(2026, 11, 7))
        assertEquals(3, m.versao)
        assertEquals(listOf(1342, 1343, 4004), m.apostas.map { it.concurso })
        assertEquals(listOf(Associacao.notaConcursoDuvidoso(4004, 1342)), m.apostas[0].notasDoAnexo)
        assertEquals(e.apostas[1], m.apostas[1])
    }
}

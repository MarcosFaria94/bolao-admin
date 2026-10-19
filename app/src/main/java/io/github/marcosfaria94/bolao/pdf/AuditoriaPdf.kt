package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.imagem.PngIndexado
import java.io.OutputStream
import java.time.LocalDate

/**
 * PDF de auditoria: todos os comprovantes únicos do período (aporte, aposta, apuração, rateio,
 * extrato e extinção), em ordem cronológica, cada um seguido dos seus anexos originais; no fim,
 * todos os lançamentos. Na capa, o resumo do período e a conferência da corrente de autenticação.
 */
object AuditoriaPdf {

    /** De onde vêm as imagens (no celular: arquivos do app; nos testes: imagens prontas). */
    interface FonteDeImagens {
        /** PNG com paleta do comprovante (é refeito se o arquivo tiver sumido). */
        fun recibo(c: Comprovante): ByteArray?

        /** Páginas do anexo como JPEG (foto: 1 página; PDF: as páginas). Vazio se não deu para ler. */
        fun paginas(a: Arquivo): List<ByteArray>

        /** SHA-256 do arquivo guardado, ou null se ele não existir mais. */
        fun sha256(a: Arquivo): String?
    }

    data class Resultado(val paginas: Int, val comprovantes: Int, val anexos: Int, val problemas: List<String>)

    private fun resumo(e: Estado, de: LocalDate, ate: LocalDate): List<List<String>> {
        val antes = e.lancamentos.filter { it.data.isBefore(de) }.sumOf { it.valor }
        val no = e.lancamentos.filter { !it.data.isBefore(de) && !it.data.isAfter(ate) }
        fun soma(vararg t: TipoLancamento) = no.filter { it.tipo in t }.sumOf { it.valor }
        val linhas = mutableListOf(
            listOf("Saldo no início do período", Dinheiro.rs(antes)),
            listOf("Saldo inicial do cadastro", Dinheiro.rs(soma(TipoLancamento.SALDO_INICIAL))),
            listOf("Aportes", Dinheiro.rs(soma(TipoLancamento.APORTE))),
            listOf("Prêmios", Dinheiro.rs(soma(TipoLancamento.PREMIO))),
            listOf("Rendimentos da caixinha", Dinheiro.rs(soma(TipoLancamento.RENDIMENTO))),
            listOf("Apostas", Dinheiro.rs(soma(TipoLancamento.APOSTA))),
            listOf("Rateios", Dinheiro.rs(soma(TipoLancamento.RATEIO))),
        )
        if (no.any { it.tipo == TipoLancamento.DIVISAO_FINAL || it.tipo == TipoLancamento.COMPLEMENTO }) {
            linhas += listOf("Complemento do Administrador", Dinheiro.rs(soma(TipoLancamento.COMPLEMENTO)))
            linhas += listOf("Divisão final do saldo", Dinheiro.rs(soma(TipoLancamento.DIVISAO_FINAL)))
        }
        linhas += listOf("Saldo no fim do período", Dinheiro.rs(antes + no.sumOf { it.valor }))
        return linhas
    }

    fun gerar(
        e: Estado,
        de: LocalDate,
        ate: LocalDate,
        fonte: FonteDeImagens,
        saida: OutputStream,
        geradoEm: Long = System.currentTimeMillis(),
        progresso: (feitos: Int, total: Int) -> Unit = { _, _ -> },
    ): Resultado {
        val doc = Documento(rodape = "Auditoria · ${e.nomeExibicao}")
        doc.pdf.titulo = "Auditoria do ${e.nomeExibicao}"
        doc.pdf.assunto = "Comprovantes de ${Datas.longa(de)} a ${Datas.longa(ate)}"
        val problemas = mutableListOf<String>()

        val doPeriodo = e.comprovantes.withIndex()
            .filter { (_, c) -> !c.data.isBefore(de) && !c.data.isAfter(ate) }
            .sortedWith(compareBy({ it.value.data }, { it.value.criadoEm }))

        // Conferência da corrente de autenticação (todos os comprovantes, não só os do período).
        val quebrada = Operacoes.primeiraAutenticacaoQuebrada(e)
        if (quebrada != null) problemas += "A autenticação do comprovante \"${e.comprovantes[quebrada].titulo}\" não confere com o registro."
        val anexosConferidos = mutableMapOf<String, String>()
        for (c in e.comprovantes) for (a in c.anexos) {
            val atual = fonte.sha256(a)
            anexosConferidos[a.nome] = when {
                atual == null -> "arquivo não encontrado"
                a.sha256 == null -> "sem impressão digital guardada"
                atual == a.sha256 -> "confere"
                else -> "DIFERENTE do registrado"
            }
            if (atual == null && doPeriodo.any { it.value.id == c.id }) problemas += "Anexo \"${a.nomeOriginal}\" de \"${c.titulo}\" não encontrado."
            if (atual != null && a.sha256 != null && atual != a.sha256) problemas += "Anexo \"${a.nomeOriginal}\" de \"${c.titulo}\" foi modificado."
        }

        // ---------- capa ----------
        doc.titulo("Auditoria do bolão", 22f)
        doc.paragrafo("**${e.nomeExibicao}**", tamanho = 13f, depois = 2f)
        doc.paragrafo(
            "Período: **${Datas.longa(de)} a ${Datas.longa(ate)}**. Gerado em ${Datas.dataHora(geradoEm)}." +
                (if (e.exemplo) " **MODO DE TESTE: dados de exemplo, não é o bolão real.**" else ""),
        )
        doc.paragrafo(
            "Integrantes: " + e.pessoas.joinToString(", ") { it.nome + if (it.administrador) " (Administrador)" else "" } + ".",
            marcacao = false,
        )
        doc.secao("Resumo do período")
        doc.tabela(listOf(listOf("Movimento", "Valor")) + resumo(e, de, ate), listOf(3f, 1.4f), alinharDireita = setOf(1))
        doc.secao("Conferência")
        if (quebrada == null) {
            doc.paragrafo("Corrente de autenticação **íntegra**: os ${e.comprovantes.size} comprovantes conferem com os registros, em ordem.")
        } else {
            doc.paragrafo("Atenção: ${problemas.first()}", cor = 0xA01818, fonte = EscritorPdf.Fonte.HELVETICA_NEGRITO, marcacao = false)
        }
        val totalAnexos = doPeriodo.sumOf { it.value.anexos.size }
        doc.paragrafo(
            "${doPeriodo.size} comprovante(s) e $totalAnexos anexo(s) neste período. Cada comprovante traz o código de " +
                "autenticação (SHA-256 encadeado) e cada anexo a conferência da sua impressão digital.",
        )
        problemas.drop(if (quebrada == null) 0 else 1).forEach { doc.paragrafo("• $it", cor = 0xA01818, marcacao = false) }

        doc.secao("Comprovantes")
        val enviados = e.envios.groupBy { it.comprovanteId }
        doc.tabela(
            listOf(listOf("Data", "Comprovante", "Autenticação", "Envio")) + doPeriodo.map { (_, c) ->
                listOf(
                    Datas.curta(c.data),
                    c.titulo,
                    Autenticacao.formatar(c.autenticacao).take(14),
                    when {
                        c.envioDispensado -> "não se aplica"
                        enviados[c.id] != null -> Datas.curta(java.time.Instant.ofEpochMilli(enviados.getValue(c.id).first().em).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                        else -> "não enviado"
                    },
                )
            },
            listOf(1f, 3.6f, 1.9f, 1.3f),
            tamanho = 8.5f,
        )

        // ---------- cada comprovante e os seus anexos ----------
        var feitos = 0
        val total = doPeriodo.size
        for ((_, c) in doPeriodo) {
            doc.novaPagina()
            doc.titulo(c.titulo, 14f)
            val envio = enviados[c.id]?.first()?.let { "enviado ao grupo em ${Datas.dataHora(it.em)}" }
                ?: if (c.envioDispensado) "registro sem envio ao grupo" else "ainda não enviado ao grupo"
            doc.paragrafo(
                "Registrado em ${Datas.dataHora(c.criadoEm)} · $envio · autenticação ${Autenticacao.formatar(c.autenticacao)}",
                tamanho = 8.5f, cor = 0x555555,
            )
            val png = runCatching { fonte.recibo(c) }.getOrNull()
            val partes = png?.let { PngIndexado.lerParaPdf(it) }
            if (partes != null) {
                doc.imagem(doc.pdf.pngIndexado(partes), larguraMax = 330f)
            } else {
                doc.codigo(c.texto)
            }
            c.anexos.forEachIndexed { i, a ->
                val paginas = runCatching { fonte.paginas(a) }.getOrDefault(emptyList())
                val conferencia = anexosConferidos[a.nome] ?: "-"
                if (paginas.isEmpty()) {
                    doc.paragrafo("Anexo ${i + 1}: ${a.nomeOriginal} (não foi possível mostrar o arquivo; impressão digital: $conferencia)", tamanho = 9f, marcacao = false)
                }
                paginas.forEachIndexed { k, jpeg ->
                    val legenda = "Anexo ${i + 1}: ${a.nomeOriginal}" +
                        (if (paginas.size > 1) " (página ${k + 1} de ${paginas.size})" else "") + " · impressão digital: $conferencia"
                    val img = runCatching { doc.pdf.jpeg(jpeg) }.getOrNull()
                    if (img != null) doc.imagem(img, legenda = legenda) else doc.paragrafo(legenda, tamanho = 9f, marcacao = false)
                }
            }
            feitos++
            progresso(feitos, total)
        }

        // ---------- todos os lançamentos do período ----------
        doc.novaPagina()
        doc.titulo("Lançamentos do período", 14f)
        val lancs = Operacoes.ordenados(e.lancamentos.filter { !it.data.isBefore(de) && !it.data.isAfter(ate) })
        var saldo = e.lancamentos.filter { it.data.isBefore(de) }.sumOf { it.valor }
        doc.tabela(
            listOf(listOf("Data", "Lançamento", "Valor", "Saldo")) + lancs.map { l ->
                saldo += l.valor
                listOf(Datas.curta(l.data), Operacoes.descricao(e, l), (if (l.valor < 0) "- " else "+ ") + Dinheiro.rs(kotlin.math.abs(l.valor)), Dinheiro.rs(saldo))
            },
            listOf(1f, 3.2f, 1.5f, 1.5f),
            alinharDireita = setOf(2, 3),
            tamanho = 8.5f,
        )
        doc.gravar(saida)
        return Resultado(doc.pdf.quantidadePaginas, doPeriodo.size, totalAnexos, problemas)
    }
}

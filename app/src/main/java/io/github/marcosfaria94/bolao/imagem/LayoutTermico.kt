package io.github.marcosfaria94.bolao.imagem

import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.FaixaPremio
import io.github.marcosfaria94.bolao.domain.Mensagens

/**
 * Conteúdo do comprovante térmico, linha a linha (sem desenhar nada): cabeçalho com o nome
 * do grupo, o bloco do termo (Parte 4) e o rodapé com o anexo, o registro e a autenticação.
 * Largura mínima de 48 colunas, como a bobina de 80 mm; linhas mais largas alargam o papel.
 */
object LayoutTermico {

    const val COLUNAS = 48

    enum class Estilo {
        /** Texto alinhado à esquerda. */
        NORMAL,
        CENTRO,
        /** Altura e largura dobradas, centralizado (como o "modo duplo" das impressoras). */
        GRANDE,
        /** Linha inteira em negativo (fundo escuro, letra na cor do papel). */
        INVERTIDO,
        /** Faixa em negativo com o texto centralizado. */
        FAIXA,
        /** Trevo do bolão. */
        LOGO,
        /** Meia linha em branco. */
        ESPACO,
        /** Letra um pouco menor (9/10), para o aviso do rodapé. */
        PEQUENO,
    }

    /** Aviso do rodapé de todo comprovante registrado. */
    const val AVISO_INTEGRIDADE =
        "Comprovante único. O código de autenticação (tirado do hash SHA-256 encadeado com os registros " +
            "anteriores) comprova a integridade deste registro, que fica guardado no app do Administrador do bolão."

    /** Subtítulo do documento do saldo (o "Saldo atual" do termo). */
    const val SUBTITULO_SALDO = "SALDO ATUAL"

    /** Aviso do documento de saldo (que não é registrado). */
    const val AVISO_SALDO =
        "Documento informativo, gerado na hora: não é um comprovante registrado e não fica guardado no app. " +
            "O código acima é o do último comprovante registrado."

    /** Colunas da letra do aviso num papel de [colunas] colunas normais. */
    fun colunasPequenas(colunas: Int): Int = colunas * 10 / 9

    /** Quebra [texto] em linhas de até [largura] caracteres, sem cortar palavras. */
    fun quebrar(texto: String, largura: Int): List<String> {
        val linhas = mutableListOf<String>()
        var atual = StringBuilder()
        for (palavra in texto.split(' ').filter { it.isNotEmpty() }) {
            if (atual.isNotEmpty() && atual.length + 1 + palavra.length > largura) {
                linhas += atual.toString()
                atual = StringBuilder()
            }
            if (atual.isNotEmpty()) atual.append(' ')
            atual.append(palavra)
        }
        if (atual.isNotEmpty()) linhas += atual.toString()
        return linhas
    }

    private fun pequenas(texto: String, colunas: Int): List<Linha> =
        quebrar(texto, colunasPequenas(colunas)).map { Linha(it, Estilo.PEQUENO) }

    data class Linha(val texto: String = "", val estilo: Estilo = Estilo.NORMAL)

    /**
     * O resultado do concurso, no comprovante da apuração (depois do bloco do termo, que diz
     * "RESULTADO: (VIDE ANEXO)"): os números sorteados e o rateio oficial da Caixa, com a faixa
     * da aposta em negativo. [sorteados]: rótulo e itens ("SORTEADOS" e os números, "TREVOS"...).
     */
    data class ResultadoOficial(
        val concurso: Int,
        val sorteados: List<Pair<String, List<String>>>,
        val faixas: List<FaixaPremio>,
        val premiadas: Set<Int>,
        val acumulou: Boolean?,
    )

    /** Linhas do resultado: os sorteados, se acumulou e a tabela "FAIXA / GANHADORES / PRÊMIO". */
    fun linhasDoResultado(r: ResultadoOficial): List<Linha> {
        if (r.sorteados.isEmpty() && r.faixas.isEmpty()) return emptyList()
        val tabela = r.faixas.sortedBy { it.faixa }.map { f ->
            val desc = f.descricao.ifBlank { "${f.faixa}ª faixa" }
            val ganhadores = Dinheiro.formatar(f.ganhadores.toLong() * 100).substringBefore(",")
            val premio = if (f.ganhadores == 0 || f.valor <= 0) "-" else Dinheiro.rs(f.valor)
            Triple(desc, ganhadores, premio) to (f.faixa in r.premiadas)
        }
        val wDesc = maxOf(16, tabela.maxOfOrNull { it.first.first.length } ?: 0)
        val wGan = maxOf(13, (tabela.maxOfOrNull { it.first.second.length } ?: 0) + 3)
        val wPremio = maxOf(COLUNAS - wDesc - wGan, 9, (tabela.maxOfOrNull { it.first.third.length } ?: 0) + 3)
        return buildList {
            add(Linha("-".repeat(maxOf(COLUNAS, wDesc + wGan + wPremio))))
            add(Linha("RESULTADO OFICIAL - CONCURSO ${r.concurso}"))
            r.sorteados.forEach { (rotulo, itens) ->
                Mensagens.linhaQuebrada(rotulo, itens.ifEmpty { listOf("-") }).split("\n").forEach { add(Linha(it)) }
            }
            r.acumulou?.let { add(Linha("ACUMULOU: ${if (it) "SIM" else "NÃO"}")) }
            if (tabela.isNotEmpty()) {
                add(Linha(estilo = Estilo.ESPACO))
                add(Linha("RATEIO OFICIAL"))
                add(Linha("FAIXA".padEnd(wDesc) + "GANHADORES".padStart(wGan) + "PRÊMIO".padStart(wPremio)))
                tabela.forEach { (t, destaque) ->
                    val texto = t.first.padEnd(wDesc) + t.second.padStart(wGan) + t.third.padStart(wPremio)
                    add(Linha(texto, if (destaque) Estilo.INVERTIDO else Estilo.NORMAL))
                }
            }
        }
    }

    fun subtitulo(tipo: TipoComprovante): String = when (tipo) {
        TipoComprovante.APORTE -> "COMPROVANTE DE APORTE"
        TipoComprovante.APOSTA -> "COMPROVANTE DE APOSTA"
        TipoComprovante.APURACAO -> "COMPROVANTE DE APURAÇÃO"
        TipoComprovante.RATEIO -> "COMPROVANTE DE RATEIO"
        TipoComprovante.EXTRATO -> "PRESTAÇÃO DE CONTAS"
        TipoComprovante.EXTINCAO -> "DIVISÃO FINAL DO SALDO"
    }

    /** Descrição dos anexos que vão junto (para o rodapé). */
    fun descricaoAnexos(tipo: TipoComprovante, quantidade: Int): String? = when {
        quantidade == 0 -> null
        tipo == TipoComprovante.APOSTA -> "ANEXO: COMPROVANTE DA CAIXA"
        tipo == TipoComprovante.APURACAO -> "ANEXO: IMAGEM DOS ACERTOS"
        tipo == TipoComprovante.RATEIO || tipo == TipoComprovante.EXTINCAO ->
            if (quantidade == 1) "ANEXO: 1 COMPROVANTE DE PIX" else "ANEXOS: $quantidade COMPROVANTES DE PIX"
        quantidade == 1 -> "ANEXO: 1 ARQUIVO"
        else -> "ANEXOS: $quantidade ARQUIVOS"
    }

    /**
     * Linhas de um comprovante.
     * [previa] = ainda não registrado (sem autenticação, com aviso no topo).
     * [teste] = modo de teste (aviso no topo para ninguém confundir com o bolão real).
     */
    fun montar(
        grupo: String,
        tipo: TipoComprovante,
        texto: String,
        quantidadeAnexos: Int,
        registradoEm: Long?,
        autenticacao: String?,
        teste: Boolean = false,
        previa: Boolean = false,
        resultado: ResultadoOficial? = null,
    ): List<Linha> {
        val bloco = texto.split("\n").map { Linha(it) }
        val extras = resultado?.let { linhasDoResultado(it) }.orEmpty()
        val largura = maxOf(COLUNAS, (bloco + extras).maxOf { it.texto.length })
        val nome = grupo.ifBlank { "Bolão dos Amigos" }.uppercase()
        return buildList {
            if (teste) add(Linha("TESTE · DADOS DE EXEMPLO", Estilo.FAIXA))
            if (previa) add(Linha("PRÉVIA · AINDA NÃO REGISTRADO", Estilo.FAIXA))
            if (teste || previa) add(Linha(estilo = Estilo.ESPACO))
            add(Linha(estilo = Estilo.LOGO))
            // No modo duplo cabem metade das colunas.
            add(Linha(nome, if (nome.length <= largura / 2) Estilo.GRANDE else Estilo.CENTRO))
            add(Linha(subtitulo(tipo), Estilo.CENTRO))
            add(Linha("=".repeat(largura)))
            addAll(bloco)
            addAll(extras)
            add(Linha("=".repeat(largura)))
            descricaoAnexos(tipo, quantidadeAnexos)?.let { add(Linha(it)) }
            // REGISTRO e AUTENTICAÇÃO sempre no fim, uma linha cada.
            if (previa || registradoEm == null || autenticacao.isNullOrBlank()) {
                add(Linha("REGISTRO: AINDA NÃO REGISTRADO"))
                add(Linha("AUTENTICAÇÃO: APÓS O REGISTRO"))
            } else {
                add(Linha("REGISTRO: ${Datas.dataHora(registradoEm)}"))
                add(Linha("AUTENTICAÇÃO: ${Autenticacao.formatar(autenticacao)}"))
                add(Linha(estilo = Estilo.ESPACO))
                addAll(pequenas(AVISO_INTEGRIDADE, largura))
            }
        }
    }

    /**
     * Documento do saldo atual (tela "Saldo"): mesmo visual dos comprovantes, mas não é registro.
     * [ultimaAutenticacao] liga o documento ao último comprovante registrado.
     */
    fun consultaSaldo(grupo: String, texto: String, geradoEm: Long, ultimaAutenticacao: String?, teste: Boolean = false): List<Linha> {
        val bloco = texto.split("\n").map { Linha(it) }
        val largura = maxOf(COLUNAS, bloco.maxOf { it.texto.length })
        val nome = grupo.ifBlank { "Bolão dos Amigos" }.uppercase()
        return buildList {
            if (teste) {
                add(Linha("TESTE · DADOS DE EXEMPLO", Estilo.FAIXA))
                add(Linha(estilo = Estilo.ESPACO))
            }
            add(Linha(estilo = Estilo.LOGO))
            add(Linha(nome, if (nome.length <= largura / 2) Estilo.GRANDE else Estilo.CENTRO))
            add(Linha(SUBTITULO_SALDO, Estilo.CENTRO))
            add(Linha("=".repeat(largura)))
            addAll(bloco)
            add(Linha("=".repeat(largura)))
            add(Linha("POSIÇÃO EM: ${Datas.dataHora(geradoEm)}"))
            if (!ultimaAutenticacao.isNullOrBlank()) add(Linha("ÚLTIMO REGISTRO: ${Autenticacao.formatar(ultimaAutenticacao)}"))
            add(Linha(estilo = Estilo.ESPACO))
            addAll(pequenas(AVISO_SALDO, largura))
        }
    }

    /** Linhas de um comprovante já registrado. */
    fun deComprovante(grupo: String, c: Comprovante, teste: Boolean = false, resultado: ResultadoOficial? = null): List<Linha> =
        montar(grupo, c.tipo, c.texto, c.anexos.size, c.criadoEm, c.autenticacao, teste, resultado = resultado)

    /** Colunas que o papel precisa ter para estas linhas. */
    fun colunas(linhas: List<Linha>): Int =
        maxOf(
            COLUNAS,
            linhas.filter { it.estilo != Estilo.GRANDE && it.estilo != Estilo.LOGO && it.estilo != Estilo.PEQUENO }
                .maxOfOrNull { it.texto.length } ?: 0,
        )
}

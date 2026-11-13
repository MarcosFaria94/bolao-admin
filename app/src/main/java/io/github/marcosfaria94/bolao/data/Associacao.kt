package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.LeituraComprovante
import io.github.marcosfaria94.bolao.domain.Modalidade

/**
 * "Registrar comprovantes" (aba Comprovantes, desde a 0.10): os arquivos escolhidos de uma vez
 * (comprovantes da Caixa e dos Pix), já lidos pelo reconhecimento de texto, vão para os seus
 * lugares: cada comprovante da Caixa para a aposta registrada que espera por ele, e cada Pix para
 * o integrante do rateio que espera por ele. Depois disso vem a conferência (aprovação), que cria
 * os comprovantes do bolão. Função pura: quem chama guarda os arquivos, lê o texto e grava o estado.
 *
 * Regras:
 * - primeiro vão os comprovantes iguais a uma aposta (de todos os arquivos); os que não batem em cheio
 *   vão depois, para a aposta mais parecida das que sobraram (desde a 0.22);
 * - arquivo repetido (o mesmo conteúdo já anexado, ou duas vezes no mesmo lote): descartado;
 * - comprovante da Caixa com outros números (ou outro concurso) que os da aposta do app: a aposta
 *   fica como no comprovante (o jogo é alterado pelos números do comprovante), com o aviso;
 * - comprovante de uma aposta que já tem o comprovante: descartado por repetição;
 * - depois de anexar, uma aposta do app igual a outra que já tem comprovante (mesmo jogo, mesmo
 *   concurso) e que ficou sem o seu é uma repetição: é descartada (o valor volta ao saldo);
 * - Pix com valor diferente da cota de todos os rateios que esperam Pix: o comprovante é descartado.
 */
object Associacao {

    /**
     * Um arquivo escolhido, já guardado no app, com o texto lido nele. [alternativas] (desde a 0.22):
     * outras leituras do mesmo arquivo (o texto do próprio PDF, a leitura com a imagem maior), para
     * quando a principal não bate com nenhuma aposta.
     */
    data class Entrada(val arquivo: Arquivo, val texto: String, val alternativas: List<String> = emptyList())

    enum class Tipo {
        /** Comprovante da Caixa anexado à aposta, conferindo com ela. */
        ANEXADO,

        /** Anexado, mas a aposta mudou para ficar como no comprovante. */
        ALTERADA,

        /** Aposta do app repetida (igual a outra que já tem comprovante): descartada. */
        APOSTA_REPETIDA,

        /** Arquivo repetido ou comprovante de uma aposta que já tem o seu: descartado. */
        ARQUIVO_REPETIDO,

        /** Comprovante de uma aposta que não está esperando comprovante: descartado. */
        SEM_APOSTA,

        PIX_ANEXADO,

        /** Pix com valor que não bate com o rateio: descartado. */
        PIX_VALOR,

        /** Não deu para entender o arquivo: descartado. */
        NAO_LIDO,

        /** O que ainda espera comprovante depois deste lote. */
        AINDA_FALTA,
    }

    data class Aviso(val tipo: Tipo, val texto: String) {
        /** Algo deu errado ou foi descartado (o aviso aparece em vermelho). */
        val problema: Boolean
            get() = tipo in setOf(Tipo.APOSTA_REPETIDA, Tipo.ARQUIVO_REPETIDO, Tipo.SEM_APOSTA, Tipo.PIX_VALOR, Tipo.NAO_LIDO)
    }

    data class Resultado(
        val estado: Estado,
        val avisos: List<Aviso>,
        /** Arquivos que não foram para lugar nenhum (o app pode apagá-los). */
        val descartados: List<Arquivo>,
        /** Apostas que mudaram de concurso pelo comprovante (o dia do sorteio e o prêmio são buscados na Caixa). */
        val concursoMudado: List<String> = emptyList(),
    )

    /** Nota da conferência quando o número lido como concurso está longe demais do da aposta (desde a 0.21). */
    fun notaConcursoDuvidoso(lido: Int, daAposta: Int): String =
        "O concurso lido no comprovante ($lido) não parece certo: ficou o da aposta ($daAposta). Confira."

    /** "Aposta Nº6 · Mega-Sena 3076". */
    fun nomeDaAposta(a: Aposta): String = "Aposta Nº${a.numero} · ${a.modalidade.nome}" + if (a.concurso > 0) " ${a.concurso}" else ""

    /** O que foi apostado é o mesmo (os números, os trevos, os palpites ou o bilhete). */
    fun mesmoPalpite(a: Aposta, b: Aposta): Boolean = a.modalidade == b.modalidade && when (a.modalidade.formato) {
        Formato.NUMEROS -> a.numeros.sorted() == b.numeros.sorted() && a.trevos.sorted() == b.trevos.sorted() &&
            (a.extra ?: "").equals(b.extra ?: "", ignoreCase = true)
        Formato.COLUNAS -> a.numeros == b.numeros
        Formato.LOTECA -> a.palpitesLoteca == b.palpitesLoteca
        Formato.FEDERAL -> a.bilheteFederal.orEmpty().filter(Char::isDigit) == b.bilheteFederal.orEmpty().filter(Char::isDigit)
    }

    /** A leitura [l] tem o mesmo jogo da aposta [a] (só o que foi lido conta; sem a parte principal, não). */
    private fun mesmoJogo(a: Aposta, l: LeituraComprovante.Lida): Boolean = when (a.modalidade.formato) {
        Formato.NUMEROS -> l.numeros != null && l.numeros.sorted() == a.numeros.sorted() &&
            (l.trevos == null || l.trevos.sorted() == a.trevos.sorted())
        Formato.COLUNAS -> l.numeros != null && l.numeros == a.numeros
        Formato.LOTECA -> l.palpitesLoteca != null && l.palpitesLoteca == a.palpitesLoteca
        Formato.FEDERAL -> l.bilhete != null && l.bilhete.filter(Char::isDigit) == a.bilheteFederal.orEmpty().filter(Char::isDigit)
    }

    /** A leitura tem a parte principal do jogo (os números, os palpites ou o bilhete). */
    private fun temJogo(m: Modalidade, l: LeituraComprovante.Lida): Boolean = when (m.formato) {
        Formato.NUMEROS, Formato.COLUNAS -> l.numeros != null
        Formato.LOTECA -> l.palpitesLoteca != null
        Formato.FEDERAL -> l.bilhete != null
    }

    /** Quanto a leitura [l] se parece com a aposta [a]: quantos números (ou colunas, ou palpites) batem. */
    fun semelhanca(a: Aposta, l: LeituraComprovante.Lida): Int = when (a.modalidade.formato) {
        Formato.NUMEROS -> l.numeros?.let { lidos -> a.numeros.count { it in lidos } } ?: 0
        Formato.COLUNAS -> l.numeros?.let { lidos -> a.numeros.indices.count { lidos.getOrNull(it) == a.numeros[it] } } ?: 0
        Formato.LOTECA -> l.palpitesLoteca?.let { lidos -> a.palpitesLoteca.indices.count { lidos.getOrNull(it) == a.palpitesLoteca[it] } } ?: 0
        Formato.FEDERAL -> if (l.bilhete != null && l.bilhete.filter(Char::isDigit) == a.bilheteFederal.orEmpty().filter(Char::isDigit)) 1 else 0
    }

    /** Os números das apostas [livres] da modalidade [m], para a leitura (apostas com mais números, desempate). */
    private fun esperados(livres: List<Aposta>, m: Modalidade): List<List<Int>> =
        if (m.formato != Formato.NUMEROS) emptyList() else livres.filter { it.modalidade == m }.map { it.numeros.sorted() }

    /** As leituras de aposta de um [texto] (uma por jogo), com a modalidade de cada uma, sem repetir o mesmo jogo. */
    private fun leiturasDe(texto: String, livres: List<Aposta>, pix: Boolean): List<Pair<Modalidade, LeituraComprovante.Lida>> {
        var leituras = LeituraComprovante.secoes(texto).flatMap { (m, t) ->
            LeituraComprovante.lerApostas(m, t, esperados(livres, m)).filter { !it.vazia }.map { m to it }
        }
        // Sem o nome da modalidade no texto: tenta as modalidades das apostas que esperam comprovante.
        if (leituras.isEmpty() && !pix) {
            leituras = livres.map { it.modalidade }.distinct().flatMap { m ->
                LeituraComprovante.lerApostas(m, texto, esperados(livres, m)).filter { temJogo(m, it) }.map { m to it }
            }
        }
        return leituras.distinctBy { (m, l) -> listOf(m, l.numeros, l.palpitesLoteca, l.bilhete) }
    }

    /** Quantas das [leituras] batem em cheio com uma aposta [livres] (cada aposta conta uma vez). */
    private fun acertosEmCheio(leituras: List<Pair<Modalidade, LeituraComprovante.Lida>>, livres: List<Aposta>): Int {
        val usadas = mutableSetOf<String>()
        return leituras.count { (m, l) ->
            livres.firstOrNull { it.id !in usadas && it.modalidade == m && mesmoJogo(it, l) }?.also { usadas += it.id } != null
        }
    }

    /**
     * Um comprovante do [texto] confere em cheio com as apostas que esperam comprovante: todas as leituras
     * têm o jogo e cada uma é igual a uma aposta. Aí não é preciso uma segunda leitura do arquivo.
     */
    fun confereEmCheio(e: Estado, texto: String): Boolean {
        val livres = Operacoes.apostasSemComprovante(e)
        val leituras = leiturasDe(texto, livres, LeituraComprovante.ehPix(texto))
        return leituras.isNotEmpty() && leituras.all { (m, l) -> temJogo(m, l) } && acertosEmCheio(leituras, livres) == leituras.size
    }

    fun associar(e: Estado, entradas: List<Entrada>): Resultado {
        var est = e
        val avisos = mutableListOf<Aviso>()
        val descartados = mutableListOf<Arquivo>()
        val concursoMudado = mutableListOf<String>()
        val conhecidos = buildSet {
            e.apostas.forEach { a -> a.comprovante?.sha256?.let { add(it.uppercase()) } }
            e.rateios.forEach { r -> (r.pix.values + r.comprovantes).forEach { a -> a.sha256?.let { add(it.uppercase()) } } }
            e.comprovantes.forEach { c -> c.anexos.forEach { a -> a.sha256?.let { add(it.uppercase()) } } }
        }.toMutableSet()
        // Apostas esperando o comprovante, na ordem do registro (as mais antigas recebem primeiro).
        val livres = Operacoes.apostasSemComprovante(e).map { it.id }.toMutableList()

        fun aposta(id: String) = est.apostas.first { it.id == id }

        /** Anexa a leitura [l] à aposta [alvo] (e às outras da mesma teimosinha, que têm o mesmo comprovante). */
        fun anexarEm(en: Entrada, alvo: Aposta, l: LeituraComprovante.Lida, duvidoso: Int?) {
            val m = alvo.modalidade
            val nome = en.arquivo.nomeOriginal
            val diferencas = Operacoes.divergencias(alvo, l)
            val ajustada = if (diferencas.isEmpty()) null else Operacoes.ajustarPeloComprovante(alvo, l)
            val notas = buildList {
                diferencas.forEach { d -> add("${d.campo}: no app ${d.noApp}; no comprovante ${d.naCaixa}.") }
                if (!temJogo(m, l)) add("Não deu para ler o jogo no comprovante: confira os números.")
                when {
                    duvidoso != null -> add(notaConcursoDuvidoso(duvidoso, alvo.concurso))
                    l.concurso == null -> add("Não deu para ler o concurso no comprovante: confira.")
                }
            }
            est = Operacoes.anexarNaAposta(est, alvo.id, en.arquivo, ajustada, notas)
            livres.remove(alvo.id)
            if (ajustada != null && ajustada.concurso != alvo.concurso) concursoMudado += alvo.id
            avisos += if (diferencas.isEmpty()) {
                Aviso(Tipo.ANEXADO, "${nomeDaAposta(alvo)}: comprovante anexado ($nome).")
            } else {
                Aviso(
                    Tipo.ALTERADA,
                    "${nomeDaAposta(alvo)}: jogo alterado pelos números do comprovante ($nome). " +
                        diferencas.joinToString(" ") { d -> "${d.campo}: era ${d.noApp}, ficou ${d.naCaixa}." },
                )
            }
            // Teimosinha (desde a 0.22): o comprovante da Caixa é um só para todos os concursos dela.
            if (alvo.ehTeimosinha) {
                val irmas = livres.map { aposta(it) }.filter { it.teimosinhaDe == alvo.teimosinhaDe && mesmoPalpite(it, alvo) }
                irmas.forEach { irma ->
                    // O mesmo jogo do comprovante, cada uma no seu concurso.
                    val igual = ajustada?.let { irma.copy(numeros = it.numeros, trevos = it.trevos, extra = it.extra) }
                    est = Operacoes.anexarNaAposta(est, irma.id, en.arquivo, igual, notas.filter { !it.startsWith("Concurso:") })
                    livres.remove(irma.id)
                }
                if (irmas.isNotEmpty()) {
                    avisos += Aviso(
                        Tipo.ANEXADO,
                        "Teimosinha: o mesmo comprovante ($nome) ficou também " +
                            (if (irmas.size == 1) "na aposta" else "nas apostas") + " " + irmas.joinToString(", ") { "Nº${it.numero}" } + ".",
                    )
                }
            }
        }

        /** A leitura com o concurso escolhido diante das apostas da modalidade [m] que esperam comprovante. */
        fun comConcurso(m: Modalidade, lida: LeituraComprovante.Lida): LeituraComprovante.Escolha {
            val candidatas = livres.map { aposta(it) }.filter { it.modalidade == m }
            // O concurso que bate com uma aposta; um número muito longe do de todas (ex.: um telefone) não muda a aposta.
            val esperados = candidatas.map { it.concurso }.ifEmpty {
                est.apostas.filter { it.registrada && it.modalidade == m && it.comprovante != null }.map { it.concurso }
            }
            return LeituraComprovante.escolherConcurso(lida, esperados)
        }

        /** 1ª passada: a leitura igual a uma aposta que espera comprovante vai para ela. true = anexou. */
        fun anexarEmCheio(en: Entrada, m: Modalidade, lida: LeituraComprovante.Lida): Boolean {
            val candidatas = livres.map { aposta(it) }.filter { it.modalidade == m }
            val escolha = comConcurso(m, lida)
            val l = escolha.lida
            val exata = candidatas.firstOrNull { mesmoJogo(it, l) && (l.concurso == null || l.concurso == it.concurso) }
                ?: candidatas.firstOrNull { mesmoJogo(it, l) }
                ?: return false
            anexarEm(en, exata, l, escolha.duvidoso)
            return true
        }

        /**
         * 2ª passada: a leitura que não é igual a nenhuma aposta. Vai para a aposta mais parecida (a que tem
         * mais números em comum; no empate, a do mesmo concurso e a mais antiga), que fica como no
         * comprovante. Até a 0.21 ia para a primeira da fila, parecida ou não: um número mal lido num
         * comprovante trocava o jogo de outra aposta, e as seguintes iam trocando em cadeia.
         */
        fun anexarNaMaisParecida(en: Entrada, m: Modalidade, lida: LeituraComprovante.Lida): Boolean {
            val nome = en.arquivo.nomeOriginal
            val candidatas = livres.map { aposta(it) }.filter { it.modalidade == m }
            val escolha = comConcurso(m, lida)
            val l = escolha.lida
            // O jogo do comprovante já tem comprovante (de antes ou deste lote): é repetição.
            val jaTem = est.apostas.firstOrNull { a ->
                a.registrada && a.comprovante != null && a.modalidade == m && mesmoJogo(a, l) &&
                    (l.concurso == null || l.concurso == a.concurso || a.ehTeimosinha)
            }
            if (jaTem != null) {
                // O mesmo arquivo já está nessa aposta (o jogo aparece duas vezes no texto): nada a avisar.
                if (jaTem.comprovante?.nome != en.arquivo.nome) {
                    avisos += Aviso(Tipo.ARQUIVO_REPETIDO, "$nome: comprovante repetido (${nomeDaAposta(jaTem)} já tem o seu). Descartado.")
                }
                return false
            }
            val alvo = candidatas.withIndex().maxWithOrNull(
                compareBy<IndexedValue<Aposta>>({ semelhanca(it.value, l) }, { if (l.concurso != null && it.value.concurso == l.concurso) 1 else 0 }, { -it.index }),
            )?.value
            if (alvo == null) {
                avisos += Aviso(
                    Tipo.SEM_APOSTA,
                    "$nome: ${m.nome}${l.concurso?.let { " $it" } ?: ""} não corresponde a nenhuma aposta esperando comprovante. Descartado.",
                )
                return false
            }
            anexarEm(en, alvo, l, escolha.duvidoso)
            return true
        }

        fun anexarPix(en: Entrada) {
            val nome = en.arquivo.nomeOriginal
            val pix = LeituraComprovante.lerPix(en.texto)
            val valor = pix.valor
            if (valor == null) {
                avisos += Aviso(Tipo.NAO_LIDO, "$nome: não deu para ler o valor do Pix. Descartado.")
                descartados += en.arquivo
                return
            }
            val esperando = Operacoes.rateiosSemPix(est).sortedWith(compareBy({ it.data }, { it.numeroNoMes }, { it.numero }))
            val r = esperando.firstOrNull { it.cota == valor }
            if (r == null) {
                val cotas = esperando.joinToString("; ") { "rateio Nº${it.numeroNoMes}: ${Dinheiro.rs(it.cota)} para cada um" }
                avisos += Aviso(
                    Tipo.PIX_VALOR,
                    "$nome: Pix de ${Dinheiro.rs(valor)} não bate com " +
                        (if (esperando.isEmpty()) "nenhum rateio esperando Pix" else "o valor do rateio ($cotas)") +
                        ". Comprovante descartado.",
                )
                descartados += en.arquivo
                return
            }
            // Quem recebeu: o integrante cujo nome (ou a chave Pix) aparece no destino do Pix.
            val pontos = est.pessoas.associateWith { LeituraComprovante.pontosDoNome(pix.destino, it.nome, it.apelido, it.chavePix) }
            val melhor = pontos.filter { it.value > 0 }.maxByOrNull { it.value }?.key
            if (melhor != null && r.pix.containsKey(melhor.id)) {
                avisos += Aviso(Tipo.ARQUIVO_REPETIDO, "$nome: o Pix de ${melhor.apelido} no rateio Nº${r.numeroNoMes} já foi anexado. Descartado.")
                descartados += en.arquivo
                return
            }
            val pessoa = melhor ?: est.pessoas.first { !r.pix.containsKey(it.id) }
            est = Operacoes.anexarPix(est, r.numero, pessoa.id, en.arquivo)
            avisos += Aviso(
                Tipo.PIX_ANEXADO,
                "Rateio Nº${r.numeroNoMes}: Pix de ${pessoa.apelido} (${Dinheiro.rs(valor)}) anexado ($nome)" +
                    (if (melhor == null) ". O nome de quem recebeu não foi lido: confira." else "."),
            )
        }

        // O que ficou para a 2ª passada: as leituras que não são iguais a nenhuma aposta.
        val depois = mutableListOf<Triple<Entrada, Modalidade, LeituraComprovante.Lida>>()
        for (en in entradas) {
            val nome = en.arquivo.nomeOriginal
            val sha = en.arquivo.sha256?.uppercase()
            if (sha != null && !conhecidos.add(sha)) {
                avisos += Aviso(Tipo.ARQUIVO_REPETIDO, "$nome: arquivo repetido (já anexado). Descartado.")
                descartados += en.arquivo
                continue
            }
            val pix = LeituraComprovante.ehPix(en.texto)
            // Entre as leituras do arquivo (a principal e as alternativas, desde a 0.22), vale a que mais bate
            // em cheio com as apostas que esperam comprovante; no empate, a principal.
            val fila = livres.map { aposta(it) }
            val leituras = (listOf(en.texto) + en.alternativas).filter { it.isNotBlank() }.ifEmpty { listOf(en.texto) }
                .map { leiturasDe(it, fila, pix) }
                .withIndex()
                .maxWithOrNull(compareBy<IndexedValue<List<Pair<Modalidade, LeituraComprovante.Lida>>>>({ acertosEmCheio(it.value, fila) }, { -it.index }))
                ?.value.orEmpty()
            val ehAposta = leituras.any { (m, l) -> temJogo(m, l) } || (leituras.isNotEmpty() && !pix)
            when {
                ehAposta -> {
                    // Se nenhuma leitura deste arquivo for usada, ele sai no fim (os usados ficam de fora da lista).
                    descartados += en.arquivo
                    for ((m, l) in leituras) if (!anexarEmCheio(en, m, l)) depois += Triple(en, m, l)
                }
                pix -> anexarPix(en)
                else -> {
                    avisos += Aviso(Tipo.NAO_LIDO, "$nome: não parece um comprovante de aposta da Caixa nem de Pix. Descartado.")
                    descartados += en.arquivo
                }
            }
        }
        for ((en, m, l) in depois) anexarNaMaisParecida(en, m, l)

        // Aposta do app repetida: igual a outra que já tem comprovante e sem o comprovante dela.
        for (id in livres.toList()) {
            val a = est.apostas.firstOrNull { it.id == id } ?: continue
            val igual = est.apostas.firstOrNull { b ->
                b.id != id && b.registrada && b.comprovante != null && b.concurso == a.concurso && mesmoPalpite(a, b)
            } ?: continue
            est = Operacoes.descartarAposta(est, id)
            livres.remove(id)
            avisos += Aviso(
                Tipo.APOSTA_REPETIDA,
                "${nomeDaAposta(a)}: descartada por repetição (igual à Aposta Nº${igual.numero}). ${Dinheiro.rs(a.valor)} voltou ao saldo.",
            )
        }

        // Desde a 0.36 o comprovante da Caixa é opcional: as apostas sem ele não entram no relatório como "ainda falta".
        Operacoes.rateiosSemPix(est).forEach { r ->
            val faltam = est.pessoas.filter { !r.pix.containsKey(it.id) }.joinToString(", ") { it.apelido }
            avisos += Aviso(Tipo.AINDA_FALTA, "Rateio Nº${r.numeroNoMes}: ainda falta o Pix de $faltam.")
        }
        val usados = (est.apostas.mapNotNull { it.comprovante?.nome } + est.rateios.flatMap { r -> r.pix.values.map { it.nome } }).toSet()
        return Resultado(est, avisos, descartados.filter { it.nome !in usados }.distinctBy { it.nome }, concursoMudado)
    }
}

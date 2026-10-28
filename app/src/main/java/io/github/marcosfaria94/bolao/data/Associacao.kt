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
 * - arquivo repetido (o mesmo conteúdo já anexado, ou duas vezes no mesmo lote): descartado;
 * - comprovante da Caixa com outros números (ou outro concurso) que os da aposta do app: a aposta
 *   fica como no comprovante (o jogo é alterado pelos números do comprovante), com o aviso;
 * - comprovante de uma aposta que já tem o comprovante: descartado por repetição;
 * - depois de anexar, uma aposta do app igual a outra que já tem comprovante (mesmo jogo, mesmo
 *   concurso) e que ficou sem o seu é uma repetição: é descartada (o valor volta ao saldo);
 * - Pix com valor diferente da cota de todos os rateios que esperam Pix: o comprovante é descartado.
 */
object Associacao {

    /** Um arquivo escolhido, já guardado no app, com o texto lido nele. */
    data class Entrada(val arquivo: Arquivo, val texto: String)

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

        /** Tenta pôr a leitura [l] (da modalidade [m]) numa aposta. true = usou o arquivo. */
        fun anexarLeitura(en: Entrada, m: Modalidade, l: LeituraComprovante.Lida): Boolean {
            val nome = en.arquivo.nomeOriginal
            val candidatas = livres.map { aposta(it) }.filter { it.modalidade == m }
            val exata = candidatas.firstOrNull { mesmoJogo(it, l) && (l.concurso == null || l.concurso == it.concurso) }
                ?: candidatas.firstOrNull { mesmoJogo(it, l) }
            val alvo = exata ?: run {
                // O jogo do comprovante já tem comprovante (de antes ou deste lote): é repetição.
                val jaTem = est.apostas.firstOrNull { a ->
                    a.registrada && a.comprovante != null && a.modalidade == m && mesmoJogo(a, l) &&
                        (l.concurso == null || l.concurso == a.concurso)
                }
                if (jaTem != null) {
                    avisos += Aviso(Tipo.ARQUIVO_REPETIDO, "$nome: comprovante repetido (${nomeDaAposta(jaTem)} já tem o seu). Descartado.")
                    return false
                }
                candidatas.firstOrNull { l.concurso != null && it.concurso == l.concurso } ?: candidatas.firstOrNull()
            }
            if (alvo == null) {
                avisos += Aviso(
                    Tipo.SEM_APOSTA,
                    "$nome: ${m.nome}${l.concurso?.let { " $it" } ?: ""} não corresponde a nenhuma aposta esperando comprovante. Descartado.",
                )
                return false
            }
            val diferencas = Operacoes.divergencias(alvo, l)
            val ajustada = if (diferencas.isEmpty()) null else Operacoes.ajustarPeloComprovante(alvo, l)
            val notas = buildList {
                diferencas.forEach { d -> add("${d.campo}: no app ${d.noApp}; no comprovante ${d.naCaixa}.") }
                if (!temJogo(m, l)) add("Não deu para ler o jogo no comprovante: confira os números.")
                if (l.concurso == null) add("Não deu para ler o concurso no comprovante: confira.")
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

        for (en in entradas) {
            val nome = en.arquivo.nomeOriginal
            val sha = en.arquivo.sha256?.uppercase()
            if (sha != null && !conhecidos.add(sha)) {
                avisos += Aviso(Tipo.ARQUIVO_REPETIDO, "$nome: arquivo repetido (já anexado). Descartado.")
                descartados += en.arquivo
                continue
            }
            val pix = LeituraComprovante.ehPix(en.texto)
            var leituras = LeituraComprovante.secoes(en.texto).flatMap { (m, t) ->
                LeituraComprovante.lerApostas(m, t).filter { !it.vazia }.map { m to it }
            }
            // Sem o nome da modalidade no texto: tenta as modalidades das apostas que esperam comprovante.
            if (leituras.isEmpty() && !pix) {
                leituras = livres.map { aposta(it).modalidade }.distinct().flatMap { m ->
                    LeituraComprovante.lerApostas(m, en.texto).filter { temJogo(m, it) }.map { m to it }
                }
            }
            val ehAposta = leituras.any { (m, l) -> temJogo(m, l) } || (leituras.isNotEmpty() && !pix)
            when {
                ehAposta -> {
                    var usou = false
                    for ((m, l) in leituras) if (anexarLeitura(en, m, l)) usou = true
                    if (!usou) descartados += en.arquivo
                }
                pix -> anexarPix(en)
                else -> {
                    avisos += Aviso(Tipo.NAO_LIDO, "$nome: não parece um comprovante de aposta da Caixa nem de Pix. Descartado.")
                    descartados += en.arquivo
                }
            }
        }

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

        livres.mapNotNull { id -> est.apostas.firstOrNull { it.id == id } }.forEach { a ->
            avisos += Aviso(Tipo.AINDA_FALTA, "${nomeDaAposta(a)}: ainda sem o comprovante da Caixa.")
        }
        Operacoes.rateiosSemPix(est).forEach { r ->
            val faltam = est.pessoas.filter { !r.pix.containsKey(it.id) }.joinToString(", ") { it.apelido }
            avisos += Aviso(Tipo.AINDA_FALTA, "Rateio Nº${r.numeroNoMes}: ainda falta o Pix de $faltam.")
        }
        val usados = (est.apostas.mapNotNull { it.comprovante?.nome } + est.rateios.flatMap { r -> r.pix.values.map { it.nome } }).toSet()
        return Resultado(est, avisos, descartados.filter { it.nome !in usados }.distinctBy { it.nome }, concursoMudado)
    }
}

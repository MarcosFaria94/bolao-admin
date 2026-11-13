package io.github.marcosfaria94.bolao.domain

import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.time.LocalDate

/**
 * O "Transcrever" da tela da Caixa (desde a 0.31): numa página de modalidade do Loterias Online da Caixa, o app
 * pega uma aposta que ainda espera o comprovante, marca o volante do site igual a ela e põe no carrinho. O
 * pagamento continua por conta do usuário, no próprio site.
 *
 * Aqui fica só o que não depende da tela: qual modalidade a página do site é (pelo endereço), quais apostas
 * esperam o comprovante (e em que ordem) e o [Plano] que o script do site (assets/caixa/transcrever.js) segue.
 */
object Transcricao {

    /** O site de apostas da Caixa. */
    const val SITE = "loteriasonline.caixa.gov.br"

    /** A página de entrada (o site leva para a página inicial dele). */
    const val ENDERECO = "https://www.loteriasonline.caixa.gov.br/silce-web/"

    /** Uma página de modalidade do site: a modalidade e se é a de um concurso especial (Mega da Virada...). */
    data class Pagina(val modalidade: Modalidade, val especial: Boolean)

    /** Os nomes das modalidades nos endereços do site, sem traços ("mega-sena" -> "megasena"). */
    private val NOS_ENDERECOS: List<Pair<String, Modalidade>> = listOf(
        "megasena" to Modalidade.MEGA_SENA,
        "lotofacil" to Modalidade.LOTOFACIL,
        "quina" to Modalidade.QUINA,
        "lotomania" to Modalidade.LOTOMANIA,
        "timemania" to Modalidade.TIMEMANIA,
        "duplasena" to Modalidade.DUPLA_SENA,
        "diadesorte" to Modalidade.DIA_DE_SORTE,
        "supersete" to Modalidade.SUPER_SETE,
        "maismilionaria" to Modalidade.MAIS_MILIONARIA,
        "milionaria" to Modalidade.MAIS_MILIONARIA,
        "loteca" to Modalidade.LOTECA,
    )

    /** Os concursos especiais com página própria, pelo nome no endereço ("mega-da-virada"). */
    private val ESPECIAIS_NOS_ENDERECOS: List<Pair<String, Modalidade>> = listOf(
        "virada" to Modalidade.MEGA_SENA,
        "saojoao" to Modalidade.QUINA,
        "independencia" to Modalidade.LOTOFACIL,
        "pascoa" to Modalidade.DUPLA_SENA,
    )

    private fun soLetras(t: String): String =
        Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().filter { it.isLetterOrDigit() }

    /**
     * A página de modalidade do endereço [url] do site da Caixa, ou null (outro site, a página inicial, o carrinho,
     * a Federal...). O site é uma página só, com a parte depois do "#" mudando: "…/silce-web/#/mega-sena",
     * "#/quina", "#/mega-sena/especial" (a Mega da Virada).
     */
    fun pagina(url: String?): Pagina? {
        if (url.isNullOrBlank()) return null
        val semEsquema = url.substringAfter("://", url)
        val dominio = semEsquema.substringBefore('/').substringBefore('#').substringBefore('?').substringBefore(':').lowercase()
        if (dominio != SITE && !dominio.endsWith(".$SITE")) return null
        val fragmento = url.substringAfter('#', "").substringBefore('?')
        val partes = fragmento.split('/').map { soLetras(it) }.filter { it.isNotEmpty() }
        val primeira = partes.firstOrNull() ?: return null
        ESPECIAIS_NOS_ENDERECOS.firstOrNull { primeira.contains(it.first) }?.let { return Pagina(it.second, especial = true) }
        val (nome, m) = NOS_ENDERECOS.firstOrNull { primeira.startsWith(it.first) } ?: return null
        val resto = primeira.removePrefix(nome) + partes.drop(1).joinToString("")
        // Outras páginas da modalidade (resultados, regras...) não têm o volante.
        if (resto.isNotEmpty() && !resto.contains("especial") && !resto.contains("aposta") && !resto.contains("volante")) return null
        return Pagina(m, especial = resto.contains("especial"))
    }

    // ---------- as apostas que esperam o comprovante ----------

    /**
     * A aposta vai para o site da Caixa: no bolão, a registrada que ainda não foi confirmada (sem o comprovante da
     * Caixa; desde a 0.36 ela é confirmada depois de transcrita e paga). No modo Individual, que não cobra nada, as
     * registradas sem comprovante e ainda por sortear.
     */
    fun esperaComprovante(e: Estado, a: Aposta, hoje: LocalDate): Boolean =
        Operacoes.aguardandoComprovante(a) ||
            (e.individual && a.registrada && a.comprovante == null && a.apuracao == null && a.dataSorteio?.isBefore(hoje) != true)

    /**
     * O que um toque no "Transcrever" põe no carrinho do site: a [aposta] (a do primeiro concurso, numa
     * teimosinha) e todas as que vão junto ([cobertas]: as dos outros concursos da teimosinha e as apostas
     * espelho da Lotomania), com o [plano] para o script e os textos da faixa da tela.
     */
    data class Item(
        val aposta: Aposta,
        val cobertas: List<Aposta>,
        val plano: Plano,
        val titulo: String,
        val detalhe: String,
    ) {
        val ids: List<String> get() = cobertas.map { it.id }
    }

    /** A aposta [b] é a espelho da [a] (Lotomania: os 50 números que ficaram de fora, no mesmo concurso). */
    private fun ehEspelhoDe(b: Aposta, a: Aposta): Boolean =
        a.espelho && !b.espelho && b.id != a.id && b.modalidade == a.modalidade && b.concurso == a.concurso &&
            b.teimosinhaDe == a.teimosinhaDe && b.numeros.sorted() == Volante.espelho(a.modalidade, a.numeros)

    /**
     * Os itens a transcrever na página [p], na ordem: o sorteio mais próximo primeiro (depois o concurso e o número
     * da aposta). Só as apostas da modalidade da página; na página de um concurso especial, só as dele, e na
     * página comum, só as dos concursos comuns. A teimosinha vai inteira num item (a do primeiro concurso,
     * com o número de concursos) e a aposta espelho vai junto com a dela (a opção "Aposta espelho" do site).
     */
    fun itens(e: Estado, p: Pagina, hoje: LocalDate): List<Item> {
        val m = p.modalidade
        if (m.formato == Formato.FEDERAL) return emptyList()
        val esperando = e.apostas.filter {
            it.modalidade == m && esperaComprovante(e, it, hoje) && (Especiais.doConcurso(m, it.concurso) != null) == p.especial
        }
        // As espelho que vão junto com a aposta delas.
        val espelhoDe = mutableMapOf<String, Aposta>()
        for (a in esperando.filter { it.espelho }) {
            esperando.firstOrNull { b -> ehEspelhoDe(b, a) && espelhoDe.values.none { it.id == b.id } }?.let { espelhoDe[a.id] = it }
        }
        val presas = espelhoDe.values.map { it.id }.toSet()
        // As outras se juntam por teimosinha (cada aposta solta é um grupo só dela); as espelho que não vão junto
        // com a delas (a dela já tem o comprovante) fazem um grupo à parte, com os números delas.
        val daModalidade = e.apostas.filter { it.modalidade == m && it.espelho }
        fun espelhoSolta(b: Aposta) = daModalidade.any { o -> ehEspelhoDe(b, o) }
        val grupos = esperando.filter { it.id !in presas }.groupBy {
            (if (it.ehTeimosinha) "t:${it.teimosinhaDe}" else it.id) + (if (espelhoSolta(it)) ":espelho" else "")
        }
        val ordem = compareBy<Aposta>({ it.dataSorteio ?: LocalDate.MAX }, { it.concurso }, { it.numero }, { it.criadaEm })
        return grupos.values.mapNotNull { grupo ->
            val membros = grupo.sortedWith(compareBy<Aposta>({ it.teimosinhaOrdem }).then(ordem))
            val primeira = membros.first()
            // A aposta espelho pelo site só quando todos os concursos têm a espelho esperando (senão, cada uma vai sozinha).
            val comEspelho = membros.all { it.id in espelhoDe }
            val cobertas = membros + (if (comEspelho) membros.mapNotNull { espelhoDe[it.id] } else emptyList())
            val plano = plano(primeira, concursos = membros.map { it.concurso }.distinct().size, espelho = comEspelho, valor = cobertas.sumOf { it.valor })
                ?: return@mapNotNull null
            val t = titulo(membros, comEspelho, espelhoDe)
            Item(primeira, cobertas, plano.copy(titulo = t), t, detalhe(primeira))
        }.sortedWith(compareBy(ordem) { it.aposta })
    }

    /**
     * As apostas que vão junto com a [a] no site da Caixa (desde a 0.36, para confirmar de uma vez as que foram pagas
     * juntas): as do item dela (a teimosinha inteira, a espelho da Lotomania); fora de um item, só ela.
     */
    fun vaoJunto(e: Estado, a: Aposta, hoje: LocalDate): List<String> {
        val p = Pagina(a.modalidade, especial = Especiais.doConcurso(a.modalidade, a.concurso) != null)
        return itens(e, p, hoje).firstOrNull { a.id in it.ids }?.ids ?: listOf(a.id)
    }

    /** "Aposta Nº3"; "Apostas Nº3 e Nº4 (espelho)"; "Aposta Nº3 · teimosinha de 4 concursos". */
    private fun titulo(membros: List<Aposta>, comEspelho: Boolean, espelhoDe: Map<String, Aposta>): String {
        val a = membros.first()
        val base = if (comEspelho) "Apostas Nº${a.numero} e Nº${espelhoDe[a.id]?.numero} (espelho)" else "Aposta Nº${a.numero}"
        val concursos = membros.map { it.concurso }.distinct().size
        return if (concursos > 1) "$base · teimosinha de $concursos concursos" else base
    }

    /** O jogo em texto: o time ou o mês, os números e os trevos; as colunas do Super Sete; os palpites da Loteca. */
    fun detalhe(a: Aposta): String {
        val m = a.modalidade
        return when (m.formato) {
            // O time do coração e o mês de sorte vêm antes dos números (com a letra grande, o fim da linha pode não caber).
            Formato.NUMEROS -> buildString {
                a.extra?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                append(a.numeros.sorted().joinToString(" ") { m.rotulo(it) })
                if (a.trevos.isNotEmpty()) append(" · trevos ").append(a.trevos.sorted().joinToString(" "))
            }
            Formato.COLUNAS -> a.numeros.joinToString(" ")
            Formato.LOTECA -> a.palpitesLoteca.joinToString(" ")
            Formato.FEDERAL -> a.bilheteFederal.orEmpty()
        }
    }

    // ---------- o plano para o script do site ----------

    /**
     * O que o script do site (assets/caixa/transcrever.js) faz, em JSON. Os números do volante vão como os ids
     * do site ("n04"; na Lotomania, o 00 é "n00"); [aumentos] = cliques em "aumentar a quantidade de números"
     * (acima da aposta simples, o site só deixa marcar depois deles).
     */
    @Serializable
    data class Plano(
        val modalidade: String,
        /** "dezenas" (volante de números), "colunas" (Super Sete) ou "loteca". */
        val tipo: String,
        /** Os ids do primeiro e do último número do volante (para saber se a página já mostra o volante). */
        val primeiro: String = "",
        val ultimo: String = "",
        val numeros: List<String> = emptyList(),
        val aumentos: Int = 0,
        /** +Milionária: os trevos (de 1 a 6); a aposta simples tem [trevosSimples]. */
        val trevos: List<Int> = emptyList(),
        val trevosSimples: Int = 0,
        /** Timemania: o time como a Caixa escreve ("VASCO DA GAMA/RJ"). */
        val time: String? = null,
        /** Dia de Sorte: o mês (1 a 12) e o nome. */
        val mes: Int? = null,
        val nomeDoMes: String? = null,
        /** Super Sete: o número de cada uma das 7 colunas. */
        val colunas: List<Int> = emptyList(),
        /** Loteca: para cada um dos 14 jogos, as colunas marcadas (0 = coluna 1, 1 = meio, 2 = coluna 2). */
        val loteca: List<List<Int>> = emptyList(),
        /** Lotomania: marcar a "Aposta espelho" do site. */
        val espelho: Boolean = false,
        /** Teimosinha: em quantos concursos (1 = só no aberto). */
        val teimosinha: Int = 1,
        /** O valor que o site deve mostrar, em centavos (0 = não conferir). */
        val valor: Long = 0,
        val concurso: Int = 0,
        /** O texto da aposta nas mensagens ("Aposta Nº3"). */
        val titulo: String = "",
    )

    private val json = Json { encodeDefaults = true }

    fun json(p: Plano): String = json.encodeToString(p)

    /** O id do número [n] no volante do site: "n04"; na Lotomania, o 00 é "n00". */
    fun idDoNumero(n: Int): String = "n" + n.toString().padStart(2, '0')

    /**
     * O plano da aposta [a] em [concursos] concursos (teimosinha), com a aposta espelho ou não, valendo [valor]
     * centavos no total. Null na Federal (o site não vende o bilhete escolhido).
     */
    fun plano(a: Aposta, concursos: Int = 1, espelho: Boolean = false, valor: Long = a.valor): Plano? {
        val m = a.modalidade
        val titulo = "Aposta Nº${a.numero}"
        return when (m.formato) {
            Formato.NUMEROS -> {
                val numeros = a.numeros.sorted()
                Plano(
                    modalidade = m.nome,
                    tipo = "dezenas",
                    primeiro = idDoNumero(m.menor),
                    ultimo = idDoNumero(m.maior),
                    numeros = numeros.map { idDoNumero(it) },
                    aumentos = (numeros.size - m.qtdAposta).coerceAtLeast(0),
                    trevos = a.trevos.sorted(),
                    trevosSimples = m.trevos,
                    time = a.extra?.takeIf { m.extra == Extra.TIME_DO_CORACAO && it.isNotBlank() },
                    mes = mesDeSorte(a.extra)?.takeIf { m.extra == Extra.MES_DE_SORTE },
                    nomeDoMes = mesDeSorte(a.extra)?.let { MESES_DE_SORTE[it - 1] }?.takeIf { m.extra == Extra.MES_DE_SORTE },
                    espelho = espelho && Volante.temEspelho(m),
                    teimosinha = concursos.coerceAtLeast(1),
                    valor = valor,
                    concurso = a.concurso,
                    titulo = titulo,
                )
            }
            Formato.COLUNAS -> Plano(
                modalidade = m.nome, tipo = "colunas", colunas = a.numeros, teimosinha = concursos.coerceAtLeast(1),
                valor = valor, concurso = a.concurso, titulo = titulo,
            )
            Formato.LOTECA -> Plano(
                modalidade = m.nome,
                tipo = "loteca",
                loteca = a.palpitesLoteca.map { p ->
                    p.uppercase().mapNotNull { c -> when (c) { '1' -> 0; 'X' -> 1; '2' -> 2; else -> null } }.distinct().sorted()
                },
                valor = valor,
                concurso = a.concurso,
                titulo = titulo,
            )
            Formato.FEDERAL -> null
        }
    }

    /** O mês de sorte (1 a 12) do texto da aposta ("Março", "MARCO"...), ou null. */
    fun mesDeSorte(texto: String?): Int? {
        if (texto.isNullOrBlank()) return null
        val t = soLetras(texto)
        val i = MESES_DE_SORTE.indexOfFirst { soLetras(it) == t || soLetras(it).take(3) == t }
        return if (i < 0) null else i + 1
    }

    // ---------- a resposta do script ----------

    /**
     * O que o script respondeu: a aposta foi para o carrinho ([noCarrinho]; também quando o carrinho já tinha uma
     * igual), o volante ficou marcado mas o script parou antes do carrinho para o usuário conferir ([parcial]) ou
     * deu errado. [semEspelho]: a "Aposta espelho" não foi marcada (a espelho continua para transcrever).
     */
    @Serializable
    data class Resposta(
        val ok: Boolean = false,
        val noCarrinho: Boolean = false,
        val parcial: Boolean = false,
        val semEspelho: Boolean = false,
        val mensagem: String = "",
    )

    private val leitor = Json { ignoreUnknownKeys = true }

    /** Lê a resposta do script (o JSON que ele deixa pronto); null se ainda não terminou ou veio estragada. */
    fun resposta(texto: String?): Resposta? =
        texto?.takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { leitor.decodeFromString<Resposta>(it) }.getOrNull() }

    /**
     * O valor que o WebView devolve de um script (evaluateJavascript: o resultado em JSON, "\"iniciado\"" ou "null")
     * como texto; null quando o script devolveu null (ou nada).
     */
    fun textoDoJavascript(bruto: String?): String? {
        if (bruto.isNullOrBlank() || bruto == "null") return null
        return runCatching { leitor.decodeFromString<String>(bruto) }.getOrNull()
    }

    /** O texto [t] escrito como uma string do JavaScript (entre aspas, com os escapes). */
    fun textoParaJavascript(t: String): String = json.encodeToString(t)

    /** As apostas que a [resposta] do item [i] pôs no carrinho (para não transcrever de novo). */
    fun transcritas(i: Item, resposta: Resposta): List<String> = when {
        !resposta.noCarrinho -> emptyList()
        resposta.semEspelho && i.plano.espelho -> i.cobertas.filter { c -> i.cobertas.none { o -> ehEspelhoDe(c, o) } }.map { it.id }
        else -> i.ids
    }
}

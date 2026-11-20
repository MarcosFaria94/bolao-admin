package io.github.marcosfaria94.bolao.domain

import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * A calculadora em etapas (desde a 0.42, pedido do usuário: "trate como fluxo de etapas de cálculo (1, 2, 3) que ocupa a
 * tela inteira, dê sempre para voltar e mudar a escolha anterior; o teclado de calculadora é uma ideia ruim; dá para
 * trocar a modalidade a qualquer momento"). A etapa 1 é sempre "como calcular" ([Modo]); as outras dependem dele
 * ([etapas]); no fim, o [Resultado]: o preço, quantos jogos e de que tipo, a chance de cada faixa e, quando faz sentido,
 * quantos prêmios de cada faixa (do mínimo ao máximo) e a garantia do fechamento.
 *
 * As contas de chance são as da [Calculadora] (jogos diferentes entre si: exata na faixa principal, ao acaso nas outras).
 */
object CalculoEmEtapas {

    /** Como calcular (a etapa 1). */
    enum class Modo(val letra: String, val titulo: String, val descricao: String, val exemplo: String) {
        PROBABILIDADE("P", "Probabilidade", "Diga a chance que você quer numa faixa: a calculadora monta os jogos mais baratos que chegam lá.", "Sena 1 em 1.000.000"),
        JOGOS("J", "Jogos", "Diga quantos jogos vai fazer e de que tipo: veja o preço e a chance de cada faixa.", "20 jogos simples"),
        DESDOBRAMENTO("D", "Desdobramento", "Todos os jogos simples de um grupo de números. Diga quantos números, ou quantos prêmios quer numa faixa se acertar.", "10 números = 210 jogos"),
        ORCAMENTO("$", "Orçamento", "Diga quanto pode gastar: veja quantos jogos cabem, o troco e as chances.", "R$ 100,00"),
        FECHAMENTO("F", "Fechamento", "Menos jogos que o desdobramento, com uma garantia: um prêmio, se parte dos sorteados estiver entre os seus números.", "10 números, quadra se sair 5"),
        COBERTURA("C", "Cobertura", "O máximo de jogos simples sem repetir nenhum grupo da menor faixa (na Mega, nenhuma quadra em dois jogos).", "Mega: até 32.450 jogos"),
    }

    /**
     * Por onde começa o desdobramento (desde a 0.43, pedido do usuário: o "Quantidade de prêmios" virou o Desdobramento):
     * pelos números (todos os jogos de N números) ou pelos prêmios (a aposta com menos números que dá N prêmios na faixa).
     */
    enum class Base(val titulo: String, val curto: String, val descricao: String, val exemplo: String) {
        NUMEROS("Pelos números", "Números", "Diga com quantos números: todos os jogos simples deles.", "10 números = 210 jogos"),
        PREMIOS("Pelos prêmios", "Prêmios", "Diga quantos prêmios quer numa faixa, se acertar: a calculadora acha os números que garantem isso.", "10 quadras = 9 números"),
    }

    /** As etapas depois da 1, conforme o modo. */
    enum class Etapa(val titulo: String) {
        FAIXA("Faixa"),
        CHANCE("Chance"),
        QUANTIDADE("Jogos"),
        PREMIOS("Prêmios"),
        VALOR("Valor"),
        TIPO("Tipo de jogo"),
        NUMEROS("Números"),
        GARANTIA("Garantia"),
        BASE("Base"),
    }

    /** As etapas do modo (o desdobramento depende da [base]: sem ela, as dos números). */
    fun etapas(modo: Modo, base: Base? = null): List<Etapa> = when (modo) {
        Modo.PROBABILIDADE -> listOf(Etapa.FAIXA, Etapa.CHANCE)
        Modo.JOGOS -> listOf(Etapa.QUANTIDADE, Etapa.TIPO)
        Modo.DESDOBRAMENTO -> if (base == Base.PREMIOS) listOf(Etapa.BASE, Etapa.FAIXA, Etapa.PREMIOS) else listOf(Etapa.BASE, Etapa.NUMEROS)
        Modo.ORCAMENTO -> listOf(Etapa.VALOR, Etapa.TIPO)
        Modo.FECHAMENTO -> listOf(Etapa.NUMEROS, Etapa.GARANTIA)
        Modo.COBERTURA -> listOf(Etapa.NUMEROS)
    }

    /** As etapas do pedido. */
    fun etapas(p: Pedido): List<Etapa> = p.modo?.let { etapas(it, p.base) }.orEmpty()

    /** O desdobramento pelos prêmios (a quantidade de prêmios da 0.42). */
    fun porPremios(p: Pedido): Boolean = p.modo == Modo.DESDOBRAMENTO && p.base == Base.PREMIOS

    /** O modo vale na modalidade? (o porquê, quando não vale). */
    fun motivoSemModo(m: Modalidade, modo: Modo): String? = when (modo) {
        Modo.DESDOBRAMENTO ->
            if (Probabilidades.gruposDePremios(m) == null) "A ${m.nome} não tem apostas com mais números." else null
        Modo.FECHAMENTO -> if (limitesDoFechamento(m) == null) "Não há fechamento na ${m.nome} por aqui." else null
        Modo.COBERTURA -> if (limitesDaCobertura(m) == null) "Não há cobertura na ${m.nome} por aqui." else null
        else -> null
    }

    // ---------- o pedido (as escolhas de cada etapa) ----------

    /** Como se digita a chance: "1 em X" ou "X%". */
    enum class Formato { UM_EM, PORCENTO }

    /** Uma garantia do fechamento: [acertos] garantidos se [sorteados] dos números sorteados estiverem entre os seus. */
    data class Garantia(val acertos: Int, val sorteados: Int)

    /**
     * As escolhas até agora. [faixa]: o índice das colunas da tabela ([Calculadora.QUALQUER_PREMIO] = algum prêmio);
     * [valor]: o que foi digitado na etapa do número (a chance, os jogos, os prêmios, o dinheiro ou os números);
     * [numeros]/[trevos]: o tipo de jogo.
     */
    data class Pedido(
        val modo: Modo? = null,
        val faixa: Int? = null,
        val formato: Formato = Formato.UM_EM,
        val valor: String = "",
        val numeros: Int? = null,
        val trevos: Int? = null,
        val garantia: Garantia? = null,
        val base: Base? = null,
    )

    /**
     * O pedido feito na modalidade [de] levado para a modalidade [m] (desde a 0.42 dá para trocar a modalidade a qualquer
     * momento): o que não vale lá sai. A faixa vai pelo nome (a quadra da Mega é a quadra da Quina; a quina da Mega, a
     * principal da Quina); sem o mesmo nome, a principal continua a principal e o "Qualquer prêmio" continua; outra faixa
     * sai (a etapa pergunta de novo).
     */
    fun naModalidade(p: Pedido, de: Modalidade, m: Modalidade): Pedido {
        val modo = p.modo?.takeIf { motivoSemModo(m, it) == null } ?: return Pedido()
        val faixas = faixasDoModo(m, p).map { it.first }
        val faixa = p.faixa?.let { f ->
            if (f == Calculadora.QUALQUER_PREMIO) {
                f
            } else {
                val nome = Calculadora.faixas(de).getOrNull(f)
                Calculadora.faixas(m).indexOf(nome).takeIf { it >= 0 } ?: f.takeIf { it == 0 }
            }
        }?.takeIf { it in faixas }
        val tipo = Calculadora.linhas(m).firstOrNull { it.numeros == p.numeros && it.trevos == p.trevos }
        val valor = when {
            porPremios(p) -> if (faixa == null) "" else p.valor
            modo == Modo.FECHAMENTO || modo == Modo.DESDOBRAMENTO || modo == Modo.COBERTURA ->
                p.valor.toIntOrNull()?.takeIf { v -> faixaDeNumeros(m, modo)?.let { v in it } == true }?.toString() ?: ""
            else -> p.valor
        }
        return p.copy(modo = modo, faixa = faixa, valor = valor, numeros = tipo?.numeros, trevos = tipo?.trevos, garantia = null)
    }

    /** A etapa em que o pedido está: 0 = como calcular; 1, 2... = as de [etapas]; o tamanho delas + 1 = o resultado. */
    fun etapaAtual(m: Modalidade, p: Pedido): Int {
        if (p.modo == null) return 0
        val e = etapas(p)
        e.forEachIndexed { i, x -> if (!completa(m, p, x)) return i + 1 }
        return e.size + 1
    }

    /** A etapa [e] já tem a escolha (e ela vale)? */
    fun completa(m: Modalidade, p: Pedido, e: Etapa): Boolean = when (e) {
        Etapa.FAIXA -> p.faixa != null
        Etapa.TIPO -> p.numeros != null
        Etapa.GARANTIA -> p.garantia != null
        Etapa.BASE -> p.base != null
        else -> erroDoValor(m, p) == null && p.valor.isNotBlank()
    }

    /**
     * A etapa que vem depois de escolher na etapa [i]: a primeira depois dela ainda sem escolha (as escolhas de depois que
     * continuam valendo ficam: mudar a faixa com a chance já digitada vai direto ao resultado). Voltar a uma etapa não apaga
     * nada: a tela mostra a escolha dela marcada, para mudar.
     */
    fun depoisDe(m: Modalidade, p: Pedido, i: Int): Int {
        if (p.modo == null) return 0
        val e = etapas(p)
        for (j in i until e.size) if (!completa(m, p, e[j])) return j + 1
        return etapaAtual(m, p).coerceAtLeast(i + 1)
    }

    /** O total de passos na barra de cima: como calcular, as etapas do modo e o resultado. */
    fun passos(p: Pedido): Int = 1 + (if (p.modo == null) 1 else etapas(p).size) + 1

    /** Escolher a base do desdobramento: a outra base começa a etapa seguinte do zero. */
    fun comBase(p: Pedido, base: Base): Pedido = if (p.base == base) p else p.copy(base = base, faixa = null, valor = "")

    /** Escolher o modo [novo]: o mesmo modo mantém as escolhas; outro começa do zero (guardando o jeito da chance). */
    fun comModo(p: Pedido, novo: Modo): Pedido = if (p.modo == novo) p else Pedido(modo = novo, formato = p.formato)

    /** Escolher a faixa: nos prêmios, a quantidade digitada fica só se ainda der com a faixa nova. */
    fun comFaixa(m: Modalidade, p: Pedido, faixa: Int): Pedido {
        val q = p.copy(faixa = faixa)
        return if (erroDoValor(m, q) != null) q.copy(valor = "") else q
    }

    // ---------- as faixas ----------

    /** As faixas que o pedido oferece: (índice, nome). Na probabilidade, todas e o "Qualquer prêmio"; nos prêmios, as de baixo. */
    fun faixasDoModo(m: Modalidade, p: Pedido): List<Pair<Int, String>> {
        val nomes = Calculadora.faixas(m)
        return when {
            p.modo == Modo.PROBABILIDADE -> nomes.mapIndexed { i, n -> i to n } + (Calculadora.QUALQUER_PREMIO to Calculadora.NOME_QUALQUER)
            porPremios(p) -> acertosDasFaixas(m).withIndex().filter { (i, a) -> a != null && i > 0 }.map { (i, _) -> i to nomes[i] }
            else -> emptyList()
        }
    }

    /** Os acertos de cada faixa das colunas (null = faixa que não é de acertos: o time, o mês, os trevos...). */
    fun acertosDasFaixas(m: Modalidade): List<Int?> {
        val n = Calculadora.faixas(m).size
        return when (m) {
            Modalidade.MEGA_SENA, Modalidade.LOTOFACIL, Modalidade.QUINA, Modalidade.DUPLA_SENA -> (0 until n).map { m.qtdSorteada - it }
            Modalidade.DIA_DE_SORTE, Modalidade.TIMEMANIA -> (0 until n).map { if (it == n - 1) null else m.qtdSorteada - it }
            else -> List(n) { null }
        }
    }

    /** O nome no plural para a etapa dos prêmios: "quadras", "quinas", "prêmios de 13 acertos". */
    fun plural(m: Modalidade, faixa: Int): String = when (val n = Calculadora.faixas(m).getOrNull(faixa)) {
        "Sena" -> "senas"
        "Quina" -> "quinas"
        "Quadra" -> "quadras"
        "Terno" -> "ternos"
        "Duque" -> "duques"
        null -> "prêmios"
        else -> "prêmios de $n"
    }

    // ---------- o número digitado ----------

    /** O número digitado ("1.000.000", "12,5", "R$ 100,00" → 1000000, 12.5, 100); null se não for um número. */
    fun numero(texto: String): Double? {
        val limpo = texto.replace("R$", "").replace(" ", "").replace(".", "").replace(',', '.')
        return limpo.toDoubleOrNull()?.takeIf { !it.isNaN() && !it.isInfinite() }
    }

    /** As faixas de números do fechamento e do desdobramento (quantos números se pode escolher). */
    fun faixaDeNumeros(m: Modalidade, modo: Modo): IntRange? = when (modo) {
        Modo.DESDOBRAMENTO -> if (Probabilidades.gruposDePremios(m) == null) null else (m.qtdAposta + 1)..Volante.maximo(m)
        Modo.FECHAMENTO -> limitesDoFechamento(m)
        Modo.COBERTURA -> limitesDaCobertura(m)
        else -> null
    }

    /** O que está errado no número digitado (null = está certo, ou ainda não foi digitado). */
    fun erroDoValor(m: Modalidade, p: Pedido): String? {
        if (p.valor.isBlank()) return null
        val v = numero(p.valor) ?: return "Digite um número."
        return when (p.modo) {
            Modo.PROBABILIDADE -> when {
                p.formato == Formato.PORCENTO && (v <= 0 || v > 100) -> "A chance vai de 0 a 100%."
                p.formato == Formato.PORCENTO && v >= 100 && p.faixa != 0 -> "Só a faixa principal chega a 100%: as outras sempre podem falhar."
                p.formato == Formato.UM_EM && v < 1 -> "Em \"1 em X\", o X é 1 ou mais."
                p.formato == Formato.UM_EM && v <= 1 && p.faixa != 0 -> "Só a faixa principal chega a 1 em 1: as outras sempre podem falhar."
                else -> null
            }
            Modo.JOGOS -> if (v < 1 || v != floor(v) || v > Calculadora.MAXIMO_DE_APOSTAS) "Digite quantos jogos (de 1 em diante)." else null
            Modo.DESDOBRAMENTO, Modo.FECHAMENTO, Modo.COBERTURA -> if (porPremios(p)) {
                when {
                    v < 1 || v != floor(v) -> "Digite quantos prêmios (de 1 em diante)."
                    p.faixa != null && tipoParaPremios(m, p.faixa, v.toLong()) == null -> "Nem com ${Volante.maximo(m)} números dá ${Probabilidades.milhares(v.toLong())} ${plural(m, p.faixa)}."
                    else -> null
                }
            } else {
                val r = faixaDeNumeros(m, p.modo)
                if (r == null || v != floor(v) || v.toInt() !in r) "De ${r?.first} a ${r?.last} números." else null
            }
            Modo.ORCAMENTO -> if (v <= 0) "Digite quanto quer gastar." else null
            null -> null
        }
    }

    /** Sugestões de toque rápido na etapa do número. */
    fun sugestoes(m: Modalidade, p: Pedido): List<Pair<String, String>> = when (p.modo) {
        Modo.PROBABILIDADE -> if (p.formato == Formato.UM_EM) {
            listOf("1000000" to "1 em 1.000.000", "100000" to "1 em 100.000", "1000" to "1 em 1.000", "100" to "1 em 100", "10" to "1 em 10")
        } else {
            listOf("1" to "1%", "5" to "5%", "10" to "10%", "25" to "25%", "50" to "50%")
        }
        Modo.JOGOS -> listOf("1", "5", "10", "20", "50", "100").map { it to it }
        Modo.ORCAMENTO -> listOf("10", "30", "50", "100", "200", "500").map { it to "R$ $it" }
        Modo.DESDOBRAMENTO -> if (porPremios(p)) {
            listOf("2", "5", "10", "20", "50").map { it to it }
        } else {
            faixaDeNumeros(m, Modo.DESDOBRAMENTO)?.let { r -> r.take(6).map { "$it" to "$it" } }.orEmpty()
        }
        Modo.FECHAMENTO -> faixaDeNumeros(m, Modo.FECHAMENTO)?.let { r -> r.take(6).map { "$it" to "$it" } }.orEmpty()
        Modo.COBERTURA -> faixaDeNumeros(m, Modo.COBERTURA)?.let { r ->
            (listOf(r.first + 4, r.first + 9, r.first + 14, r.first + 24).filter { it < r.last } .map { "$it" to "$it" }) + ("${r.last}" to "Todos (${r.last})")
        }.orEmpty()
        null -> emptyList()
    }

    // ---------- o tipo de jogo ----------

    /** Uma opção da etapa do tipo de jogo: a linha, quantos jogos e o preço de tudo (em centavos). */
    data class OpcaoDeTipo(val linha: Probabilidades.LinhaDaTabela, val jogos: Long, val custo: Long, val sobra: Long = 0)

    /** As opções de tipo de jogo para o pedido (jogos ou orçamento), com o preço da aposta simples [preco]. */
    fun opcoesDeTipo(m: Modalidade, p: Pedido, preco: Long): List<OpcaoDeTipo> {
        val v = numero(p.valor) ?: return emptyList()
        return Calculadora.linhas(m).map { l ->
            val deUm = l.apostasSimples * preco
            when (p.modo) {
                Modo.ORCAMENTO -> {
                    val centavos = (v * 100).roundToLong()
                    val n = if (deUm > 0) centavos / deUm else 0
                    OpcaoDeTipo(l, n, n * deUm, centavos - n * deUm)
                }
                else -> OpcaoDeTipo(l, v.toLong(), v.toLong() * deUm)
            }
        }
    }

    // ---------- os prêmios de cada faixa ----------

    /**
     * Quantos prêmios da faixa [faixa] uma aposta de [numeros] números recebe: o mínimo (acertando só essa faixa) e o
     * máximo (acertando mais). null = a faixa não é de acertos ou a modalidade não tem a tabela.
     */
    fun premiosDaFaixa(m: Modalidade, numeros: Int, faixa: Int): LongRange? {
        val acertos = acertosDasFaixas(m).getOrNull(faixa) ?: return null
        val grupos = Probabilidades.gruposDePremios(m) ?: return null
        val tabela = Probabilidades.quantidadeDePremios(m, numeros)
        val valores = grupos.mapIndexedNotNull { gi, g ->
            val fi = g.faixas.indexOf(acertos)
            if (g.acertando >= acertos && fi >= 0) g.acertando to tabela[gi][fi] else null
        }
        val minimo = valores.firstOrNull { it.first == acertos }?.second ?: return null
        val maximo = valores.maxOf { it.second }
        return minimo..maximo
    }

    /** A aposta de menos números que garante [quantos] prêmios da faixa [faixa] ao acertar só ela (null = nenhuma). */
    fun tipoParaPremios(m: Modalidade, faixa: Int, quantos: Long): Int? =
        (m.qtdAposta..Volante.maximo(m)).firstOrNull { n -> (premiosDaFaixa(m, n, faixa)?.first ?: 0) >= quantos }

    // ---------- o resultado ----------

    /**
     * O resultado: a [config] (os jogos, para as chances), o [custo] (centavos), quantos [jogos] e de que [tipo] ("6
     * números"), as [chances], os [premios] de cada faixa (do mínimo ao máximo; null = não se aplica), a [linha] com o
     * principal do pedido e, no fechamento, a [garantia] e a chance da condição dela.
     */
    data class Resultado(
        val config: ConfigDaCalculadora,
        val custo: Long,
        val jogos: Long,
        val tipo: String,
        val chances: List<Calculadora.Chance>,
        val premios: Map<Int, LongRange>?,
        val linha: String,
        val destaque: Int? = null,
        val garantia: Garantia? = null,
        val chanceDaCondicao: Double? = null,
        val numerosEscolhidos: Int? = null,
        val sobra: Long = 0,
        val cobertura: InfoDaCobertura? = null,
    )

    /**
     * A cobertura (desde a 0.43): com [numeros] números, jogos de [tamanho] sem repetir nenhum grupo de [acertos] (a menor
     * faixa). O máximo exato (o número de empacotamento D(v, k, t)) só é conhecido em casos pequenos: [johnson] é o limite
     * de Johnson (nunca passa disso), [grupos] o limite contando só os grupos (C(v, t) ÷ C(k, t), mais folgado) e
     * [montados] os jogos que a calculadora montou (null = conta grande demais para o celular; o máximo fica entre eles e
     * o limite de Johnson).
     */
    data class InfoDaCobertura(val numeros: Int, val tamanho: Int, val acertos: Int, val johnson: Long, val grupos: Long, val montados: Int?)

    /**
     * O resultado do pedido completo, com o preço da aposta simples [preco]. No fechamento, [jogosDoFechamento] = quantos
     * jogos a garantia pede (a conta é demorada: a tela faz antes, fora da linha das telas). Na cobertura, [montadosDaCobertura]
     * = quantos jogos a calculadora montou (só quando [podeMontarCobertura]; a tela faz a conta antes). null = falta alguma escolha.
     */
    fun resultado(m: Modalidade, p: Pedido, preco: Long, jogosDoFechamento: Int? = null, montadosDaCobertura: Int? = null): Resultado? {
        val modo = p.modo ?: return null
        if (etapaAtual(m, p) <= etapas(p).size) return null
        val v = numero(p.valor) ?: return null
        val simples = Calculadora.simples(m)
        fun res(c: ConfigDaCalculadora, linha: String, tipo: String, premios: Boolean = false, destaque: Int? = null, sobra: Long = 0): Resultado {
            val tipos = Calculadora.apostas(m, c)
            val n = tipos.singleOrNull()?.first?.numeros
            val mapa = if (premios && n != null) Calculadora.faixas(m).indices.mapNotNull { j -> premiosDaFaixa(m, n, j)?.let { j to it } }.toMap() else null
            return Resultado(c, Calculadora.custo(m, c, preco), Calculadora.total(m, c), tipo, Calculadora.chances(m, c), mapa, linha, destaque, sobra = sobra)
        }
        return when (modo) {
            Modo.PROBABILIDADE -> {
                val alvo = if (p.formato == Formato.PORCENTO) v / 100 else 1 / v
                val c = Calculadora.paraChance(m, p.faixa!!, alvo) ?: return null
                val fica = Calculadora.chances(m, c).firstOrNull { it.indice == p.faixa }?.p ?: 0.0
                val pedido = if (p.formato == Formato.PORCENTO) Teclado.numeroDaConta(v) + "%" else "1 em " + Calculadora.numeroDoUmEm(v)
                val nome = if (p.faixa == Calculadora.QUALQUER_PREMIO) Calculadora.NOME_QUALQUER else Calculadora.faixas(m)[p.faixa]
                res(c, "$nome: fica ${Calculadora.umEm(fica)} (pedido: $pedido)", Calculadora.composicao(m, c), destaque = p.faixa)
            }
            Modo.JOGOS -> {
                val c = Calculadora.comApostas(m, ConfigDaCalculadora(simples = 0), p.numeros!!, p.trevos ?: 0, v.toInt())
                res(c, "${Calculadora.quantas(v.toLong())} × ${Dinheiro.rs(precoDoTipo(m, p, preco))}", Calculadora.composicao(m, c), premios = v.toLong() == 1L)
            }
            Modo.ORCAMENTO -> {
                val o = opcoesDeTipo(m, p, preco).firstOrNull { it.linha.numeros == p.numeros && it.linha.trevos == (p.trevos ?: it.linha.trevos) } ?: return null
                if (o.jogos < 1) return null
                val c = Calculadora.comApostas(m, ConfigDaCalculadora(simples = 0), o.linha.numeros, o.linha.trevos, o.jogos.toInt())
                val sobra = if (o.sobra > 0) " · sobra ${Dinheiro.rs(o.sobra)}" else " · sem sobra"
                res(c, "Orçamento ${Dinheiro.rs((v * 100).roundToLong())}$sobra", Calculadora.composicao(m, c), premios = o.jogos == 1L, sobra = o.sobra)
            }
            Modo.DESDOBRAMENTO -> {
                val porPremios = porPremios(p)
                val n = if (porPremios) tipoParaPremios(m, p.faixa!!, v.toLong()) ?: return null else v.toInt()
                val c = Calculadora.comApostas(m, ConfigDaCalculadora(simples = 0), n, simples.trevos, 1)
                val jogos = Volante.combinacoes(n, m.qtdAposta)
                val linha = if (porPremios) {
                    val minimo = premiosDaFaixa(m, n, p.faixa!!)!!.first
                    "Se acertar a ${Calculadora.faixas(m)[p.faixa].lowercase()}: pelo menos ${Probabilidades.milhares(minimo)} ${plural(m, p.faixa)}"
                } else {
                    "Todos os jogos de ${m.qtdAposta} dos seus $n números"
                }
                res(c, linha, "1 aposta de $n números = ${Probabilidades.milhares(jogos)} jogos simples", premios = true, destaque = p.faixa.takeIf { porPremios })
                    .copy(jogos = jogos, numerosEscolhidos = n)
            }
            Modo.COBERTURA -> {
                val n = v.toInt()
                val k = m.qtdAposta
                val t = acertosDaCobertura(m) ?: return null
                val info = InfoDaCobertura(n, k, t, limiteDeJohnson(n, k, t), limiteDosGrupos(n, k, t), montadosDaCobertura)
                if (podeMontarCobertura(m, n) && montadosDaCobertura == null) return null
                val jogos = (montadosDaCobertura?.toLong() ?: info.johnson).coerceIn(1L, Calculadora.MAXIMO_DE_APOSTAS.toLong())
                val c = ConfigDaCalculadora(simples = jogos.toInt())
                val linha = if (montadosDaCobertura != null) {
                    "Montados ${Probabilidades.milhares(jogos)} jogos sem repetir ${nenhumGrupo(m)} (o máximo possível é de ${Probabilidades.milhares(jogos)} a ${Probabilidades.milhares(info.johnson)})"
                } else {
                    "No máximo ${Probabilidades.milhares(info.johnson)} jogos sem repetir ${nenhumGrupo(m)} (limite de Johnson)"
                }
                val tipo = (if (montadosDaCobertura != null) "" else "até ") + "${Probabilidades.milhares(jogos)} jogos de $k números"
                res(c, linha, tipo, destaque = indiceDosAcertos(m, t)).copy(numerosEscolhidos = n, cobertura = info)
            }
            Modo.FECHAMENTO -> {
                val g = p.garantia ?: return null
                val jogos = jogosDoFechamento ?: return null
                val c = ConfigDaCalculadora(simples = jogos)
                val n = v.toInt()
                res(c, "$jogos jogos simples (o desdobramento seria ${Probabilidades.milhares(Volante.combinacoes(n, m.qtdAposta))})", Calculadora.composicao(m, c))
                    .copy(garantia = g, chanceDaCondicao = chanceDaCondicao(m, n, g.sorteados), numerosEscolhidos = n, destaque = indiceDosAcertos(m, g.acertos))
            }
        }
    }

    private fun precoDoTipo(m: Modalidade, p: Pedido, preco: Long): Long =
        (Calculadora.linha(m, p.numeros ?: Calculadora.simples(m).numeros, p.trevos ?: Calculadora.simples(m).trevos)?.apostasSimples ?: 1) * preco

    /** O índice da faixa de [acertos] acertos (nas colunas). */
    fun indiceDosAcertos(m: Modalidade, acertos: Int): Int? = acertosDasFaixas(m).indexOf(acertos).takeIf { it >= 0 }

    // ---------- a cobertura ----------

    /** Os acertos da menor faixa (o tamanho do grupo que não se repete na cobertura): na Mega, 4 (a quadra). */
    fun acertosDaCobertura(m: Modalidade): Int? = acertosDasFaixas(m).filterNotNull().minOrNull()

    /**
     * Quantos números a cobertura aceita: de um a mais que a aposta simples até todos os da modalidade. null = sem
     * cobertura (Lotomania, +Milionária, Super Sete, Loteca, Federal).
     */
    fun limitesDaCobertura(m: Modalidade): IntRange? {
        if (m.formato != io.github.marcosfaria94.bolao.domain.Formato.NUMEROS || m.trevos > 0 || m == Modalidade.LOTOMANIA) return null
        val t = acertosDaCobertura(m) ?: return null
        if (t >= m.qtdAposta) return null
        return (m.qtdAposta + 1)..(m.maior - m.menor + 1)
    }

    /**
     * O limite de Johnson do número de empacotamento D(v, k, t): ⌊v/k · ⌊(v−1)/(k−1) · … ⌊(v−t+1)/(k−t+1)⌋⌋⌋. Na Mega com os
     * 60 números e a quadra: 32.450 (nenhuma coleção de jogos sem repetir quadra passa disso).
     */
    fun limiteDeJohnson(v: Int, k: Int, t: Int): Long {
        if (t <= 1) return (v / k).toLong()
        return v.toLong() * limiteDeJohnson(v - 1, k - 1, t - 1) / k
    }

    /** O limite contando só os grupos: C(v, t) ÷ C(k, t) (cada jogo gasta C(k, t) grupos). Na Mega: 487.635 ÷ 15 = 32.509. */
    fun limiteDosGrupos(v: Int, k: Int, t: Int): Long = Volante.combinacoes(v, t) / Volante.combinacoes(k, t)

    /**
     * A calculadora consegue montar os jogos da cobertura de [v] números no celular? Até 60 milhões de jogos possíveis e
     * 1 bilhão de grupos para olhar (a maioria dos jogos sai no primeiro grupo repetido): a Mega com os 60 números (uns
     * segundos), a Quina com os 80, a Dupla com os 50, o Dia de Sorte com os 31; a Lotofácil até 23 e a Timemania até 26.
     */
    fun podeMontarCobertura(m: Modalidade, v: Int): Boolean {
        val k = m.qtdAposta
        val t = acertosDaCobertura(m) ?: return false
        val jogos = Volante.combinacoes(v, k)
        return jogos <= JOGOS_DA_COBERTURA && jogos.toDouble() * Volante.combinacoes(k, t) <= GRUPOS_DA_COBERTURA &&
            Volante.combinacoes(v, t) <= 20_000_000L
    }

    private const val JOGOS_DA_COBERTURA = 60_000_000L
    private const val GRUPOS_DA_COBERTURA = 1_000_000_000.0

    /** Quantos grupos as ordens sorteadas da cobertura olham, somadas (só nas contas pequenas). */
    private const val PASSOS_DA_COBERTURA = 40_000_000.0

    /** O máximo de números com que a calculadora monta a cobertura na modalidade (null = nenhum). */
    fun maximoMontavel(m: Modalidade): Int? = limitesDaCobertura(m)?.lastOrNull { podeMontarCobertura(m, it) }

    /** "nenhuma quadra", "nenhum duque", "nenhum grupo de 11 números". */
    fun nenhumGrupo(m: Modalidade): String {
        val t = acertosDaCobertura(m) ?: return "nenhum grupo"
        return when (indiceDosAcertos(m, t)?.let { Calculadora.faixas(m)[it] }) {
            "Quina" -> "nenhuma quina"
            "Quadra" -> "nenhuma quadra"
            "Terno" -> "nenhum terno"
            "Duque" -> "nenhum duque"
            else -> "nenhum grupo de $t números"
        }
    }

    /**
     * Os jogos de uma cobertura: [v] números (índices 0 a v − 1), jogos de [k], sem repetir nenhum grupo de [t] (cada grupo
     * pelo número dele na ordem "colex"). Escolha gulosa: na ordem dos jogos, fica cada um que não repete nenhum grupo dos
     * que já ficaram. Primeiro na ordem do dicionário; quando a conta é pequena, também em ordens sorteadas (com a mesma
     * semente: o resultado é sempre o mesmo), ficando a que deu mais jogos. Nem sempre chega ao máximo, mas nunca repete um
     * grupo. [ativo] = continuar (a tela para a conta quando sai). Os jogos voltam em ordem.
     */
    fun cobertura(v: Int, k: Int, t: Int, ativo: () -> Boolean = { true }): List<IntArray> {
        val bin = Array(v + 1) { LongArray(t + 1) }
        for (n in 0..v) {
            bin[n][0] = 1
            for (r in 1..minOf(n, t)) bin[n][r] = bin[n - 1][r - 1] + bin[n - 1][r]
        }
        val subs = ArrayList<IntArray>()
        val s = IntArray(t) { it }
        while (true) {
            subs += s.copyOf()
            var i = t - 1
            while (i >= 0 && s[i] == k - t + i) i--
            if (i < 0) break
            s[i]++
            for (j in i + 1 until t) s[j] = s[j - 1] + 1
        }
        fun grupo(a: IntArray, sub: IntArray): Int {
            var r = 0L
            for (j in 0 until t) r += bin[a[sub[j]]][j + 1]
            return r.toInt()
        }
        fun proximo(a: IntArray): Boolean {
            var i = k - 1
            while (i >= 0 && a[i] == v - k + i) i--
            if (i < 0) return false
            a[i]++
            for (j in i + 1 until k) a[j] = a[j - 1] + 1
            return true
        }
        val grupos = bin[v][t].toInt()
        val total = Volante.combinacoes(v, k)
        val porJogo = subs.size
        if (total * porJogo > GRUPOS_GUARDADOS) {
            // Grande: só a ordem do dicionário, sem guardar os jogos que não ficam.
            val usados = java.util.BitSet(grupos)
            val jogos = ArrayList<IntArray>()
            val a = IntArray(k) { it }
            var passos = 0
            do {
                if (++passos and 0xFFF == 0 && !ativo()) break
                if (subs.none { usados.get(grupo(a, it)) }) {
                    subs.forEach { usados.set(grupo(a, it)) }
                    jogos += a.copyOf()
                }
            } while (proximo(a))
            return jogos
        }
        // Pequeno: os grupos de cada jogo guardados, a ordem do dicionário e as sorteadas.
        val n = total.toInt()
        val jogos = ArrayList<IntArray>(n)
        val dele = IntArray(n * porJogo)
        val a = IntArray(k) { it }
        do {
            val x = jogos.size
            jogos += a.copyOf()
            for (u in 0 until porJogo) dele[x * porJogo + u] = grupo(a, subs[u])
        } while (proximo(a))
        val usados = java.util.BitSet(grupos)
        fun rodada(ordem: IntArray?): IntArray {
            usados.clear()
            val ficam = IntArray(n)
            var q = 0
            for (pos in 0 until n) {
                val x = ordem?.get(pos) ?: pos
                val base = x * porJogo
                var cabe = true
                for (u in 0 until porJogo) if (usados.get(dele[base + u])) { cabe = false; break }
                if (cabe) {
                    for (u in 0 until porJogo) usados.set(dele[base + u])
                    ficam[q++] = x
                }
            }
            return ficam.copyOf(q)
        }
        var melhor = rodada(null)
        val voltas = minOf(VOLTAS_SORTEADAS, (PASSOS_DA_COBERTURA / (total * porJogo)).toInt() - 1)
        val sorteio = java.util.Random(SEMENTE_DA_COBERTURA)
        val ordem = IntArray(n) { it }
        for (volta in 0 until voltas) {
            if (!ativo()) break
            for (x in n - 1 downTo 1) {
                val y = sorteio.nextInt(x + 1)
                val tmp = ordem[x]; ordem[x] = ordem[y]; ordem[y] = tmp
            }
            val r = rodada(ordem)
            if (r.size > melhor.size) melhor = r
        }
        melhor.sort()
        return melhor.map { jogos[it] }
    }

    /** Até quantos grupos (jogos × grupos de cada jogo) a cobertura guarda para as ordens sorteadas. */
    private const val GRUPOS_GUARDADOS = 4_000_000L
    private const val VOLTAS_SORTEADAS = 200
    private const val SEMENTE_DA_COBERTURA = 42L

    // ---------- o fechamento ----------

    /**
     * Quantos números o fechamento aceita na modalidade: de um a mais que a aposta simples até onde a conta cabe no
     * celular (no máximo uns 5.000 jogos possíveis). null = sem fechamento (Lotomania, +Milionária, Super Sete, Loteca).
     */
    fun limitesDoFechamento(m: Modalidade): IntRange? {
        if (m.formato != io.github.marcosfaria94.bolao.domain.Formato.NUMEROS || m.trevos > 0 || m == Modalidade.LOTOMANIA) return null
        val k = m.qtdAposta
        var v = k + 1
        while (v + 1 <= m.maior - m.menor + 1 && Volante.combinacoes(v + 1, k) <= LIMITE_DE_JOGOS_POSSIVEIS) v++
        return (k + 1)..v
    }

    private const val LIMITE_DE_JOGOS_POSSIVEIS = 5_000L

    /** As garantias que a modalidade oferece com [numeros] números: as faixas de acertos e, em cada uma, de quantos sorteados. */
    fun garantias(m: Modalidade, numeros: Int): List<Pair<Int, List<Garantia>>> =
        acertosDasFaixas(m).filterNotNull().filter { it <= m.qtdAposta }.map { t ->
            t to (t..minOf(m.qtdSorteada, numeros)).map { s -> Garantia(t, s) }
        }.filter { it.second.isNotEmpty() }

    /** A chance de pelo menos [sorteados] dos números sorteados estarem entre os seus [numeros] números. */
    fun chanceDaCondicao(m: Modalidade, numeros: Int, sorteados: Int): Double {
        val total = Volante.combinacoes(m.maior - m.menor + 1, m.qtdSorteada).toDouble()
        var soma = 0.0
        for (h in sorteados..minOf(m.qtdSorteada, numeros)) {
            soma += Volante.combinacoes(numeros, h).toDouble() * Volante.combinacoes(m.maior - m.menor + 1 - numeros, m.qtdSorteada - h)
        }
        return soma / total
    }

    /**
     * Os jogos de um fechamento: [numeros] números, jogos de [tamanho], garantindo [g] (cada grupo de [Garantia.sorteados]
     * dos seus números tem pelo menos [Garantia.acertos] deles num jogo). Pela escolha gulosa: a cada passo, o jogo que
     * cobre mais grupos ainda descobertos (o primeiro, no empate: o resultado é sempre o mesmo). Nem sempre é o menor
     * fechamento que existe, mas a garantia vale. Os jogos vêm como índices dos números (0 a [numeros] − 1). null = grande
     * demais para a conta.
     */
    fun fechamento(numeros: Int, tamanho: Int, g: Garantia): List<List<Int>>? {
        if (g.acertos > tamanho || g.sorteados > numeros || g.acertos > g.sorteados) return null
        if (Volante.combinacoes(numeros, tamanho) > LIMITE_DE_JOGOS_POSSIVEIS * 2 || Volante.combinacoes(numeros, g.sorteados) > 20_000) return null
        val jogos = mascaras(numeros, tamanho)
        // Garantir os acertos de todos do jogo (acertos = tamanho) é o desdobramento: todos os jogos.
        if (g.acertos == tamanho) return jogos.map(::indices)
        val grupos = mascaras(numeros, g.sorteados)
        val ganho = IntArray(jogos.size)
        for (i in jogos.indices) {
            val j = jogos[i]
            var n = 0
            for (gr in grupos) if (Integer.bitCount(j and gr) >= g.acertos) n++
            ganho[i] = n
        }
        val coberto = BooleanArray(grupos.size)
        var faltam = grupos.size
        val escolhidos = mutableListOf<Int>()
        while (faltam > 0) {
            var melhor = 0
            for (i in 1 until jogos.size) if (ganho[i] > ganho[melhor]) melhor = i
            if (ganho[melhor] <= 0) return null
            val j = jogos[melhor]
            escolhidos += j
            for (gi in grupos.indices) {
                if (coberto[gi]) continue
                val gr = grupos[gi]
                if (Integer.bitCount(j and gr) >= g.acertos) {
                    coberto[gi] = true
                    faltam--
                    for (i in jogos.indices) if (Integer.bitCount(jogos[i] and gr) >= g.acertos) ganho[i]--
                }
            }
        }
        return escolhidos.map(::indices)
    }

    /** Todos os subconjuntos de [k] dos [n] índices, como máscaras de bits (em ordem). */
    private fun mascaras(n: Int, k: Int): IntArray {
        val lista = ArrayList<Int>()
        fun gerar(inicio: Int, falta: Int, mascara: Int) {
            if (falta == 0) { lista += mascara; return }
            for (i in inicio..n - falta) gerar(i + 1, falta - 1, mascara or (1 shl i))
        }
        gerar(0, k, 0)
        return lista.toIntArray()
    }

    private fun indices(mascara: Int): List<Int> = (0 until 32).filter { mascara and (1 shl it) != 0 }

    /** Os jogos do desdobramento de [numeros] números (todos os de [tamanho]), até [limite] (a lista na tela). */
    fun desdobramento(numeros: List<Int>, tamanho: Int, limite: Int = 500): List<List<Int>> {
        val s = numeros.sorted()
        val r = mutableListOf<List<Int>>()
        fun gerar(inicio: Int, atual: List<Int>) {
            if (r.size >= limite) return
            if (atual.size == tamanho) { r += atual; return }
            for (i in inicio..s.size - (tamanho - atual.size)) gerar(i + 1, atual + s[i])
        }
        gerar(0, emptyList())
        return r
    }

    /** O texto da garantia: "Pelo menos 1 quadra se 5 dos 6 sorteados estiverem entre os seus 10 números." */
    fun textoDaGarantia(m: Modalidade, g: Garantia, numeros: Int): String {
        val faixa = indiceDosAcertos(m, g.acertos)?.let { Calculadora.faixas(m)[it] } ?: "${g.acertos} acertos"
        val umaFaixa = when (faixa) { "Sena", "Quina", "Quadra" -> "1 ${faixa.lowercase()}"; "Terno", "Duque" -> "1 ${faixa.lowercase()}"; else -> "1 prêmio de $faixa" }
        return "Pelo menos $umaFaixa se ${g.sorteados} dos ${m.qtdSorteada} sorteados estiverem entre os seus $numeros números."
    }

    /** O nome curto de uma garantia: "Quadra · se sair 5". */
    fun nomeDaGarantia(m: Modalidade, g: Garantia): String {
        val faixa = indiceDosAcertos(m, g.acertos)?.let { Calculadora.faixas(m)[it] } ?: "${g.acertos} acertos"
        return "$faixa · se sair ${g.sorteados}"
    }

    /** "04 09 13 21 27 33": um jogo com os números de verdade ([escolhidos] na ordem dos índices). */
    fun textoDoJogo(m: Modalidade, jogo: List<Int>): String = jogo.sorted().joinToString(" ") { m.rotulo(it) }
}

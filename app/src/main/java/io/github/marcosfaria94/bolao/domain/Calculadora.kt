package io.github.marcosfaria94.bolao.domain

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.expm1
import kotlin.math.floor
import kotlin.math.ln1p
import kotlin.math.log10
import kotlin.math.pow

/**
 * A calculadora de probabilidades (desde a 0.38, pedido do usuário; o ícone ao lado das Estatísticas, nos Detalhes).
 * Desde a 0.41 ela é uma calculadora de verdade (pedido do usuário: "use lógica de calculadora: botões visíveis, tela de
 * saída e o '=' chamando o resultado"), nos dois sentidos:
 * - **das apostas para as chances**: escolhe-se um tipo de aposta (6 números, 8 números...), digita-se quantas e o "="
 *   mostra a chance de cada faixa e o preço;
 * - **das chances para as apostas** ([paraChance]): escolhe-se uma faixa (uma só de cada vez), digita-se a chance que se
 *   quer ("1 em 1.000.000" ou "5%") e o "=" monta a combinação de apostas mais barata que chega lá; as outras faixas
 *   mudam junto.
 *
 * A conta (revista na 0.41): as apostas são diferentes umas das outras. Na faixa principal isso é exato: cada aposta
 * simples é um resultado do sorteio, e apostas diferentes não se repetem, então a chance é a soma das chances (com todas
 * as combinações, 100%). Nas outras faixas as apostas contam como feitas ao acaso: 1 − Π (1 − p)ⁿ. Até a 0.40 a faixa
 * principal também usava essa conta, o que pedia apostas a mais para a mesma chance (para 50% na sena da Mega, 34,7
 * milhões de apostas em vez de 25 milhões). Na Dupla Sena cada aposta concorre nos 2 sorteios: a chance é a de ganhar em
 * pelo menos um deles (a tabela da Caixa mostra a de cada sorteio).
 */
object Calculadora {

    /** O índice da linha "Qualquer prêmio" (as faixas são 0, 1, 2...). */
    const val QUALQUER_PREMIO = -1

    const val NOME_QUALQUER = "Qualquer prêmio"

    /** O máximo de apostas de um tipo (e o da combinação que a conta inversa monta). */
    const val MAXIMO_DE_APOSTAS = 999_999_999

    /** A calculadora existe em todas as modalidades menos a Federal (bilhetes, sem volante). */
    fun tem(m: Modalidade): Boolean = m != Modalidade.FEDERAL

    /** Os tipos de aposta da modalidade (números e trevos), da tabela de probabilidades com mais números. */
    fun linhas(m: Modalidade): List<Probabilidades.LinhaDaTabela> = Probabilidades.tabela(m, mais = true)

    /** A linha da aposta simples. */
    fun simples(m: Modalidade): Probabilidades.LinhaDaTabela = linhas(m).first()

    /** Os tipos de aposta com mais números; vazio = a modalidade só tem a simples. */
    fun comMaisNumeros(m: Modalidade): List<Probabilidades.LinhaDaTabela> = linhas(m).drop(1)

    /** A linha da tabela com [numeros] e [trevos]. */
    fun linha(m: Modalidade, numeros: Int, trevos: Int): Probabilidades.LinhaDaTabela? =
        linhas(m).firstOrNull { it.numeros == numeros && it.trevos == trevos }

    /** Os nomes das faixas (as colunas da tabela de probabilidades). */
    fun faixas(m: Modalidade): List<String> = Probabilidades.colunas(m)

    /** A coluna do time ou do mês (independente dos números) é a última. */
    private fun temExtra(m: Modalidade): Boolean = m.extra != Extra.NENHUM

    /** Quantos sorteios cada aposta disputa (2 na Dupla Sena). */
    private fun sorteios(m: Modalidade): Int = m.sorteios.coerceAtLeast(1)

    /**
     * A faixa [j] é a principal, em que cada aposta simples é um resultado do sorteio (desde a 0.41): aí apostas
     * diferentes não se repetem e a chance delas se soma.
     */
    private fun exata(m: Modalidade, j: Int): Boolean = j == 0 && m.formato != Formato.FEDERAL

    /** Uma linha do painel: a faixa ([indice]; [QUALQUER_PREMIO] = algum prêmio), o nome e a chance (0 a 1). */
    data class Chance(val indice: Int, val nome: String, val p: Double)

    /** Os tipos de aposta da configuração, com quantas apostas de cada (as que têm zero ficam de fora). */
    fun apostas(m: Modalidade, c: ConfigDaCalculadora): List<Pair<Probabilidades.LinhaDaTabela, Int>> {
        val lista = mutableListOf(simples(m) to c.simples)
        c.extras.forEach { e -> linha(m, e.numeros, e.trevos)?.let { lista += it to e.apostas } }
        return lista.filter { it.second > 0 }
    }

    /** A chance de uma aposta do tipo [l] na faixa [j] num sorteio ([QUALQUER_PREMIO] = de algum prêmio). */
    private fun chanceDaLinha(m: Modalidade, l: Probabilidades.LinhaDaTabela, j: Int): Double {
        if (j != QUALQUER_PREMIO) return (l.chances.getOrNull(j) ?: 0.0).coerceIn(0.0, 1.0)
        val ps = faixas(m).indices.map { l.chances.getOrNull(it) ?: 0.0 }
        // Os números: as faixas são de acertos exatos (uma exclui a outra); o time e o mês, à parte.
        val numeros = (if (temExtra(m)) ps.dropLast(1) else ps).sum().coerceIn(0.0, 1.0)
        val extra = if (temExtra(m)) ps.last().coerceIn(0.0, 1.0) else 0.0
        val nada = (1 - numeros).pow(sorteios(m)) * (1 - extra)
        // Por "sorteio" (para a conta de [chanceDaFaixa], que eleva aos sorteios): a raiz do "nada" de uma aposta.
        return 1 - nada.pow(1.0 / sorteios(m))
    }

    /** A chance da faixa [j] com as apostas [grupo] (desde a 0.41, exata na principal; ver [Calculadora]). */
    private fun chanceDaFaixa(m: Modalidade, grupo: List<Pair<Probabilidades.LinhaDaTabela, Int>>, j: Int): Double {
        val s = sorteios(m)
        if (exata(m, j)) {
            val umSorteio = grupo.sumOf { (l, n) -> n * chanceDaLinha(m, l, j) }.coerceIn(0.0, 1.0)
            return if (s == 1) umSorteio else 1 - (1 - umSorteio).pow(s)
        }
        var log = 0.0
        for ((l, n) in grupo) log += n.toDouble() * s * ln1p(-chanceDaLinha(m, l, j).coerceAtMost(1 - 1e-16))
        return -expm1(log)
    }

    /** As chances do painel: todas as faixas e, no fim, "Qualquer prêmio" (nunca abaixo da maior faixa). */
    fun chances(m: Modalidade, c: ConfigDaCalculadora): List<Chance> {
        val grupo = apostas(m, c)
        val faixas = faixas(m).mapIndexed { j, nome -> Chance(j, nome, chanceDaFaixa(m, grupo, j)) }
        val qualquer = maxOf(chanceDaFaixa(m, grupo, QUALQUER_PREMIO), faixas.maxOfOrNull { it.p } ?: 0.0)
        return faixas + Chance(QUALQUER_PREMIO, NOME_QUALQUER, qualquer)
    }

    /** A chance da faixa [j] numa só aposta simples em um sorteio (a da Caixa; na Dupla Sena, a "por sorteio"). */
    fun chancePorSorteio(m: Modalidade, c: ConfigDaCalculadora, j: Int): Double {
        val grupo = apostas(m, c)
        if (exata(m, j)) return grupo.sumOf { (l, n) -> n * chanceDaLinha(m, l, j) }.coerceIn(0.0, 1.0)
        var log = 0.0
        for ((l, n) in grupo) log += n.toDouble() * ln1p(-chanceDaLinha(m, l, j).coerceAtMost(1 - 1e-16))
        return -expm1(log)
    }

    /** As linhas do painel que a configuração mostra (todas, ou as escolhidas). */
    fun doPainel(m: Modalidade, c: ConfigDaCalculadora): List<Chance> {
        val todas = chances(m, c)
        val escolhidas = c.faixas ?: return todas
        return todas.filter { it.indice in escolhidas }
    }

    /** Quantas apostas simples tudo isso vale (o preço é esse tanto vezes o preço da simples). */
    fun apostasSimples(m: Modalidade, c: ConfigDaCalculadora): Long = apostas(m, c).sumOf { (l, n) -> l.apostasSimples * n }

    /** Quantas apostas ao todo. */
    fun total(m: Modalidade, c: ConfigDaCalculadora): Long = apostas(m, c).sumOf { it.second.toLong() }

    /** O preço de tudo, em centavos, com o preço da aposta simples [preco]. */
    fun custo(m: Modalidade, c: ConfigDaCalculadora, preco: Long): Long = apostasSimples(m, c) * preco

    // ---------- a conta inversa (desde a 0.41) ----------

    /**
     * A combinação de apostas mais barata em que a faixa [j] ([QUALQUER_PREMIO] = algum prêmio) chega pelo menos à
     * chance [alvo] (0 a 1); as outras faixas ficam com o que essa combinação der. null quando não dá (100% numa faixa
     * que não é a principal, ou apostas demais).
     *
     * Cada tipo de aposta rende um tanto da faixa por real (na principal, a chance; nas outras, −ln(1 − p) por sorteio, que
     * se soma entre as apostas); o mais rendoso leva quase tudo. Com o mesmo rendimento, o mais barato (a simples): na
     * principal todos os tipos rendem igual (uma de 8 números são 28 simples), e a simples dá mais chance nas outras faixas.
     */
    fun paraChance(m: Modalidade, j: Int, alvo: Double, base: ConfigDaCalculadora = ConfigDaCalculadora()): ConfigDaCalculadora? {
        if (alvo.isNaN() || alvo <= 0.0) return base.copy(simples = 0, extras = emptyList())
        val s = sorteios(m)
        val exata = exata(m, j)
        if (!exata && alvo >= 1.0) return null
        if (alvo > 1.0) return null
        // O que é preciso juntar ([meta]) e o que cada tipo rende ([rende]).
        // (Com expm1 e ln1p: 1 − (1 − alvo) perde as casas das chances pequenas.)
        val meta = if (!exata) -ln1p(-alvo) else if (alvo >= 1.0) 1.0 else -expm1(ln1p(-alvo) / s)
        data class Tipo(val l: Probabilidades.LinhaDaTabela, val rende: Double, val custo: Long)
        val tipos = linhas(m).mapNotNull { l ->
            val p = chanceDaLinha(m, l, j)
            if (p <= 0.0) return@mapNotNull null
            val rende = if (exata) p else -s * ln1p(-p.coerceAtMost(1 - 1e-16))
            Tipo(l, rende, l.apostasSimples.coerceAtLeast(1))
        }
        if (tipos.isEmpty()) return null
        // O melhor rendimento por real; no empate (até 1 em 1 bilhão), o mais barato.
        val melhor = tipos.reduce { a, b ->
            val ra = a.rende / a.custo
            val rb = b.rende / b.custo
            when {
                rb > ra * (1 + 1e-9) -> b
                ra > rb * (1 + 1e-9) -> a
                b.custo < a.custo -> b
                else -> a
            }
        }
        fun quantas(t: Tipo, falta: Double): Long = if (falta <= 0) 0 else ceil(falta / t.rende * (1 - 1e-9)).toLong().coerceAtLeast(1)
        var escolha: List<Pair<Tipo, Long>> = listOf(melhor to quantas(melhor, meta))
        var menor = melhor.custo * escolha[0].second
        // O melhor tipo custa mais de uma simples: talvez trocar as últimas dele por outro tipo saia mais barato.
        if (melhor.custo > 1) {
            val n = escolha[0].second
            for (k in 1..minOf(n, 60L)) {
                val falta = meta - (n - k) * melhor.rende
                for (t in tipos) {
                    if (t === melhor) continue
                    val q = quantas(t, falta)
                    val custo = (n - k) * melhor.custo + q * t.custo
                    if (custo < menor) {
                        menor = custo
                        escolha = listOf(melhor to (n - k), t to q)
                    }
                }
            }
        }
        if (escolha.any { it.second > MAXIMO_DE_APOSTAS }) return null
        var c = base.copy(simples = 0, extras = emptyList())
        for ((t, q) in escolha) if (q > 0) c = comApostas(m, c, t.l.numeros, t.l.trevos, q.toInt())
        return c
    }

    // ---------- o teclado: o que se escolhe e o "=" (desde a 0.41) ----------

    /** O que o visor está mudando: quantas apostas de um tipo ou a chance de uma faixa. */
    sealed interface Registro {
        data class Apostas(val numeros: Int, val trevos: Int) : Registro
        data class Faixa(val indice: Int) : Registro
    }

    /** O registro das apostas simples (o que vem escolhido). */
    fun registroDaSimples(m: Modalidade): Registro = simples(m).let { Registro.Apostas(it.numeros, it.trevos) }

    /** O registro como texto, para guardar na tela ("a:6:0", "f:0", "f:-1"); [deChave] volta. */
    fun chave(r: Registro): String = when (r) {
        is Registro.Apostas -> "a:${r.numeros}:${r.trevos}"
        is Registro.Faixa -> "f:${r.indice}"
    }

    fun deChave(m: Modalidade, chave: String?): Registro {
        val p = chave?.split(':').orEmpty()
        return when {
            p.size == 3 && p[0] == "a" -> {
                val n = p[1].toIntOrNull()
                val t = p[2].toIntOrNull()
                if (n != null && t != null && linha(m, n, t) != null) Registro.Apostas(n, t) else registroDaSimples(m)
            }
            p.size == 2 && p[0] == "f" -> p[1].toIntOrNull()?.takeIf { it == QUALQUER_PREMIO || it in faixas(m).indices }?.let { Registro.Faixa(it) }
                ?: registroDaSimples(m)
            else -> registroDaSimples(m)
        }
    }

    /** O nome do registro no visor: "6 números" ou "Sena" / "Qualquer prêmio". */
    fun nomeDoRegistro(m: Modalidade, r: Registro): String = when (r) {
        is Registro.Apostas -> nomeDaLinha(m, r.numeros, r.trevos)
        is Registro.Faixa -> if (r.indice == QUALQUER_PREMIO) NOME_QUALQUER else faixas(m).getOrElse(r.indice) { "" }
    }

    /** O que se digita no registro (embaixo do nome, no visor). */
    fun pedidoDoRegistro(r: Registro): String = when (r) {
        is Registro.Apostas -> "quantas apostas"
        is Registro.Faixa -> "a chance: 1 em X, ou X%"
    }

    /** O valor do registro agora (o começo de uma conta que começa por um sinal): as apostas, ou o X do "1 em X". */
    fun valorAtual(m: Modalidade, c: ConfigDaCalculadora, r: Registro): Double? = when (r) {
        is Registro.Apostas -> apostasDe(m, c, r.numeros, r.trevos).toDouble()
        is Registro.Faixa -> chances(m, c).firstOrNull { it.indice == r.indice }?.p?.takeIf { it > 0 }?.let { 1.0 / it }
    }

    /** O valor do registro como aparece no visor sem nada digitado: "3" ou "1 em 50.063.860". */
    fun textoAtual(m: Modalidade, c: ConfigDaCalculadora, r: Registro): String = when (r) {
        is Registro.Apostas -> Probabilidades.milhares(apostasDe(m, c, r.numeros, r.trevos).toLong())
        is Registro.Faixa -> umEm(chances(m, c).firstOrNull { it.indice == r.indice }?.p ?: 0.0)
    }

    /** O que o "=" deu: a configuração nova (null = nada muda) e a mensagem do visor ([erro] = em vermelho). */
    data class Resposta(val config: ConfigDaCalculadora?, val mensagem: String, val erro: Boolean = false)

    /** "51 apostas de 6 números", "2 de 20 números e 3 de 6 números". */
    fun composicao(m: Modalidade, c: ConfigDaCalculadora): String {
        val partes = apostas(m, c).sortedByDescending { it.first.apostasSimples }
        if (partes.isEmpty()) return "nenhuma aposta"
        return partes.mapIndexed { i, (l, n) ->
            val nome = nomeDaLinha(m, l.numeros, l.trevos)
            if (i == 0) "${quantas(n)} de $nome" else "${Probabilidades.milhares(n.toLong())} de $nome"
        }.let { if (it.size == 1) it[0] else it.dropLast(1).joinToString(", ") + " e " + it.last() }
    }

    /**
     * O "=": faz a conta da [linha] digitada e aplica no registro [r]. Nas apostas, o resultado é quantas (inteiro, 0 ou
     * mais). Nas faixas, a chance pedida ("1 em X" com o número; "X%" com o %) monta a combinação mais barata
     * ([paraChance]); as outras faixas mudam junto.
     */
    fun aplicar(m: Modalidade, c: ConfigDaCalculadora, r: Registro, linha: String): Resposta {
        val conta = Teclado.calcular(linha, valorAtual(m, c, r)) ?: return Resposta(null, "A conta não fecha: confira o que foi digitado.", erro = true)
        val v = conta.valor
        when (r) {
            is Registro.Apostas -> {
                if (conta.porcento) return Resposta(null, "O % é só para a chance de uma faixa.", erro = true)
                if (v < 0) return Resposta(null, "As apostas não ficam abaixo de zero.", erro = true)
                if (v != floor(v)) return Resposta(null, "Digite um número inteiro de apostas.", erro = true)
                if (v > MAXIMO_DE_APOSTAS) return Resposta(null, "Apostas demais: o máximo é ${Probabilidades.milhares(MAXIMO_DE_APOSTAS.toLong())}.", erro = true)
                val nova = comApostas(m, c, r.numeros, r.trevos, v.toInt())
                return Resposta(nova, "${nomeDaLinha(m, r.numeros, r.trevos)}: ${quantas(v.toLong())}.")
            }
            is Registro.Faixa -> {
                val nome = nomeDoRegistro(m, r)
                val alvo = if (conta.porcento) {
                    if (v <= 0 || v > 100) return Resposta(null, "A chance vai de 0 a 100%.", erro = true)
                    v / 100
                } else {
                    if (v < 1) return Resposta(null, "Em \"1 em X\", o X é 1 ou mais (ou use o %).", erro = true)
                    1 / v
                }
                val pedido = if (conta.porcento) Teclado.numeroDaConta(v) + "%" else "1 em " + numeroDoUmEm(v)
                val nova = paraChance(m, r.indice, alvo, c) ?: return Resposta(
                    null,
                    if (alvo >= 1) "Só a faixa principal chega a 100%: as outras sempre podem falhar." else "Não dá para chegar a $pedido em $nome.",
                    erro = true,
                )
                val fica = chances(m, nova).firstOrNull { it.indice == r.indice }?.p ?: 0.0
                return Resposta(nova, "$nome $pedido: ${composicao(m, nova)} (fica ${umEm(fica)}).")
            }
        }
    }

    // ---------- os textos ----------

    /**
     * "1 em 50.063.860" (a chance [p]); "—" sem chance. Desde a 0.41 com uma casa decimal abaixo de 100 e duas abaixo de
     * 10 ("1 em 30,2", "1 em 1,03"): o "1 em 30" com "~3,31%" embaixo parecia errado (1 ÷ 30 = 3,33%; a conta certa é
     * 1 ÷ 30,2).
     */
    fun umEm(p: Double): String = when {
        p <= 0.0 -> "—"
        else -> "1 em " + numeroDoUmEm(1.0 / p)
    }

    /** O X do "1 em X" (desde a 0.41): inteiro de 100 para cima; abaixo disso com 1 casa (2 abaixo de 10), sem zeros no fim. */
    fun numeroDoUmEm(x: Double): String {
        val v = x.coerceAtLeast(1.0)
        if (v >= 99.95) return Probabilidades.milhares(BigDecimal(v).setScale(0, RoundingMode.HALF_UP).toLong())
        val casas = if (v >= 9.995) 1 else 2
        return BigDecimal(v).setScale(casas, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')
    }

    /**
     * "~12,3%", "~0,0021%", "< 0,0001%" (a porcentagem com 3 algarismos que contam). Desde a 0.39 (pedido do usuário) com o
     * "~" na frente: é um valor arredondado, e "100%" sem ele parecia certeza ("~100%" é quase certo, não certo). Desde a
     * 0.41 a faixa principal pode chegar a 100% de verdade (todas as combinações): aí "100%", sem o "~".
     */
    fun porcentagem(p: Double): String {
        val v = p * 100
        if (v <= 0.0) return "0%"
        if (p >= 1.0) return "100%"
        if (v < 0.0001) return "< 0,0001%"
        if (v >= 99.95) return "${APROXIMADO}100%"
        val casas = when {
            v >= 10 -> 1
            v >= 1 -> 2
            else -> (2 - floor(log10(v)).toInt()).coerceAtMost(6)
        }
        return APROXIMADO + BigDecimal(v).setScale(casas, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',') + "%"
    }

    /** O sinal das porcentagens arredondadas (desde a 0.39). */
    const val APROXIMADO = "~"

    /**
     * O nome de um tipo de aposta: "6 números", "8 números", "7 números e 3 trevos", "7 colunas" (desde a 0.40, pedido do
     * usuário; era "Apostas simples (6 números)" e "Apostas de 8 números").
     */
    fun nomeDaLinha(m: Modalidade, numeros: Int, trevos: Int): String {
        val nums = when (m.formato) {
            Formato.COLUNAS -> "7 colunas"
            Formato.LOTECA -> "14 jogos"
            else -> "$numeros números"
        }
        val trv = if (m.trevos > 0) " e $trevos trevos" else ""
        return "$nums$trv"
    }

    /** O título dos tipos de aposta com mais números (desde a 0.40). */
    fun tituloDosMaisNumeros(m: Modalidade): String = if (m.trevos > 0) "Com mais números ou trevos" else "Com mais números"

    /** "1 aposta", "3 apostas" (com o ponto dos milhares). */
    fun quantas(n: Long): String = if (n == 1L) "1 aposta" else "${Probabilidades.milhares(n)} apostas"

    fun quantas(n: Int): String = quantas(n.toLong())

    /** Quantas apostas do tipo [numeros]/[trevos] a configuração tem (0 se não houver). */
    fun apostasDe(m: Modalidade, c: ConfigDaCalculadora, numeros: Int, trevos: Int): Int {
        val s = simples(m)
        if (numeros == s.numeros && trevos == s.trevos) return c.simples
        return c.extras.filter { it.numeros == numeros && it.trevos == trevos }.sumOf { it.apostas }
    }

    /**
     * A configuração com [apostas] do tipo [numeros]/[trevos] (os que ficam em 0 saem da lista guardada). Nunca abaixo
     * de 0 nem acima de [MAXIMO_DE_APOSTAS].
     */
    fun comApostas(m: Modalidade, c: ConfigDaCalculadora, numeros: Int, trevos: Int, apostas: Int): ConfigDaCalculadora {
        val q = apostas.coerceIn(0, MAXIMO_DE_APOSTAS)
        val s = simples(m)
        if (numeros == s.numeros && trevos == s.trevos) return c.copy(simples = q)
        val outras = c.extras.filterNot { it.numeros == numeros && it.trevos == trevos }
        val extras = if (q == 0) outras else (outras + LinhaDaCalculadora(numeros, trevos, q)).sortedWith(compareBy({ it.numeros }, { it.trevos }))
        return c.copy(extras = extras)
    }
}

/**
 * As contas do teclado da calculadora (desde a 0.41): a linha digitada, com números (a vírgula é a casa decimal), + − × ÷
 * e o % no fim. Começando por um sinal, a conta usa o valor que está no visor ([Teclado.calcular] com o "atual").
 */
object Teclado {

    const val MAIS = '+'
    const val MENOS = '−'
    const val VEZES = '×'
    const val DIVIDIDO = '÷'
    const val PORCENTO = '%'
    val SINAIS = setOf(MAIS, MENOS, VEZES, DIVIDIDO)

    /** O tanto de caracteres que a linha aceita. */
    const val TAMANHO = 24

    /**
     * A linha com a tecla [t] (um algarismo, a vírgula, um sinal ou o %). Dois sinais seguidos: fica o último; vírgula
     * repetida no mesmo número não entra; nada depois do %.
     */
    fun digitar(linha: String, t: Char): String {
        if (linha.length >= TAMANHO) return linha
        if (linha.endsWith(PORCENTO)) return linha
        val ultimo = linha.lastOrNull()
        return when {
            t.isDigit() -> linha + t
            t == ',' -> {
                val numero = linha.takeLastWhile { it.isDigit() || it == ',' }
                when {
                    ',' in numero -> linha
                    numero.isEmpty() -> "${linha}0,"
                    else -> "$linha,"
                }
            }
            t in SINAIS -> when {
                linha.isEmpty() -> t.toString()
                ultimo in SINAIS -> linha.dropLast(1) + t
                ultimo == ',' -> linha.dropLast(1) + t
                else -> linha + t
            }
            t == PORCENTO -> if (ultimo != null && (ultimo.isDigit())) linha + t else linha
            else -> linha
        }
    }

    /** A linha sem o último caractere. */
    fun apagar(linha: String): String = linha.dropLast(1)

    /** O resultado de [calcular]: o [valor] e se veio com o % no fim. */
    data class Conta(val valor: Double, val porcento: Boolean)

    /**
     * Faz a conta da linha (× e ÷ antes de + e −). Começando por um sinal, a conta começa pelo [atual] (o valor do visor).
     * null quando a linha não fecha (vazia, terminando num sinal, divisão por zero).
     */
    fun calcular(linha: String, atual: Double? = null): Conta? {
        var l = linha.trim()
        val porcento = l.endsWith(PORCENTO)
        if (porcento) l = l.dropLast(1)
        if (l.isEmpty()) return null
        if (l.first() in SINAIS) {
            atual ?: return null
            l = numeroDaConta(atual) + l
        }
        // Os pedaços: números e sinais, alternados.
        val numeros = mutableListOf<Double>()
        val sinais = mutableListOf<Char>()
        val atualNumero = StringBuilder()
        for (ch in l) {
            if (ch in SINAIS) {
                numeros += atualNumero.toString().replace(',', '.').toDoubleOrNull() ?: return null
                atualNumero.clear()
                sinais += ch
            } else if (ch.isDigit() || ch == ',') {
                atualNumero.append(ch)
            } else if (ch != '.' && ch != ' ') {
                return null
            }
        }
        numeros += atualNumero.toString().replace(',', '.').toDoubleOrNull() ?: return null
        // × e ÷ primeiro.
        val n2 = mutableListOf(numeros[0])
        val s2 = mutableListOf<Char>()
        for (i in sinais.indices) {
            val b = numeros[i + 1]
            when (sinais[i]) {
                VEZES -> n2[n2.lastIndex] = n2.last() * b
                DIVIDIDO -> {
                    if (b == 0.0) return null
                    n2[n2.lastIndex] = n2.last() / b
                }
                else -> {
                    s2 += sinais[i]
                    n2 += b
                }
            }
        }
        var v = n2[0]
        for (i in s2.indices) v = if (s2[i] == MAIS) v + n2[i + 1] else v - n2[i + 1]
        if (v.isNaN() || v.isInfinite()) return null
        return Conta(v, porcento)
    }

    /** O valor do visor como começo de conta ("25031930", "30,25"). */
    fun numeroDaConta(v: Double): String =
        BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',').removePrefix("-")

    /** "25031930" → "25.031.930" (só algarismos). */
    private fun agrupar(algarismos: String): String {
        val sb = StringBuilder()
        for ((i, ch) in algarismos.withIndex()) {
            if (i > 0 && (algarismos.length - i) % 3 == 0) sb.append('.')
            sb.append(ch)
        }
        return sb.toString()
    }

    /** A linha como aparece no visor: os números com o ponto dos milhares e os sinais com espaço ("25.031.930 ÷ 2"). */
    fun visor(linha: String): String {
        val sb = StringBuilder()
        val numero = StringBuilder()
        fun soltar() {
            if (numero.isEmpty()) return
            val inteiro = numero.takeWhile { it.isDigit() }.toString()
            val resto = numero.substring(inteiro.length)
            sb.append(if (inteiro.isEmpty()) "" else agrupar(inteiro.trimStart('0').ifEmpty { "0" }))
            if (inteiro.isEmpty() && resto.startsWith(",")) sb.append("0")
            sb.append(resto)
            numero.clear()
        }
        for (ch in linha) {
            if (ch.isDigit() || ch == ',') {
                numero.append(ch)
            } else {
                soltar()
                if (ch in SINAIS) sb.append(" $ch ") else sb.append(ch)
            }
        }
        soltar()
        return sb.toString().trim()
    }
}

/**
 * Um tipo de aposta com mais números na calculadora (desde a 0.38): apostas com [numeros] números (e [trevos]), [apostas]
 * delas. Só os que não estão em 0 ficam guardados (o padrão 1 continua, para ler o que já estava guardado).
 */
@Serializable
data class LinhaDaCalculadora(val numeros: Int, val trevos: Int = 0, val apostas: Int = 1)

/**
 * A configuração da calculadora de uma modalidade (desde a 0.38): quantas apostas simples (começa em 1) e as apostas com
 * mais números (as que não estão em 0). [maximo] (a amplitude dos controles deslizantes, até a 0.40) e [faixas] (as faixas
 * do painel, até a 0.40) ficaram só para ler o que já estava guardado. O AC volta a esta configuração padrão.
 */
@Serializable
data class ConfigDaCalculadora(
    val simples: Int = 1,
    val extras: List<LinhaDaCalculadora> = emptyList(),
    val maximo: Int = 20,
    val faixas: List<Int>? = null,
)

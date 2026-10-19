package io.github.marcosfaria94.bolao.domain

/** Todos os valores do app são guardados em centavos (Long), nunca em Double. */
object Dinheiro {

    /** 4002 -> "40,02"; 123456 -> "1.234,56"; -350 -> "-3,50". */
    fun formatar(centavos: Long): String {
        val negativo = centavos < 0
        val abs = if (negativo) -centavos else centavos
        val reais = (abs / 100).toString()
        val cent = (abs % 100).toString().padStart(2, '0')
        val sb = StringBuilder()
        for ((i, c) in reais.withIndex()) {
            if (i > 0 && (reais.length - i) % 3 == 0) sb.append('.')
            sb.append(c)
        }
        return (if (negativo) "-" else "") + sb + "," + cent
    }

    /** 4002 -> "R$ 40,02". */
    fun rs(centavos: Long): String = "R$ " + formatar(centavos)

    /**
     * Lê um valor digitado: "40,02", "1.234,56", "R$ 10", "3.5", "-2,00".
     * Devolve null se não for um valor válido.
     */
    fun ler(texto: String): Long? {
        var t = texto.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace(" ", "")
        if (t.isEmpty()) return null
        val negativo = t.startsWith("-")
        if (negativo) t = t.substring(1)
        val inteiro: String
        val fracao: String
        when {
            t.contains(',') -> {
                inteiro = t.substringBeforeLast(',').replace(".", "")
                fracao = t.substringAfterLast(',')
            }
            t.count { it == '.' } == 1 && t.substringAfter('.').length <= 2 -> {
                inteiro = t.substringBefore('.')
                fracao = t.substringAfter('.')
            }
            else -> {
                inteiro = t.replace(".", "")
                fracao = ""
            }
        }
        if (inteiro.isEmpty() && fracao.isEmpty()) return null
        if (!inteiro.all { it.isDigit() } || !fracao.all { it.isDigit() } || fracao.length > 2) return null
        if (inteiro.length > 15) return null
        val i = if (inteiro.isEmpty()) 0L else inteiro.toLong()
        val f = if (fracao.isEmpty()) 0L else fracao.padEnd(2, '0').toLong()
        val v = i * 100 + f
        return if (negativo) -v else v
    }

    /**
     * Abreviação do prêmio usada na identificação do jogo (termo: "2M = Prêmio de 2 milhões").
     * 200000000 (R$ 2 mi) -> "2M"; R$ 1,7 mi -> "1,7M"; R$ 600 mil -> "600K"; R$ 1 bi -> "1B".
     */
    fun abreviar(centavos: Long): String {
        val reais = centavos / 100
        if (reais < 1_000) return reais.toString()
        val (unidade, sufixo) = when {
            reais >= 1_000_000_000L -> 1_000_000_000L to "B"
            reais >= 1_000_000L -> 1_000_000L to "M"
            else -> 1_000L to "K"
        }
        val decimos = (reais * 10 + unidade / 2) / unidade
        return when {
            decimos >= 1000 -> "${(reais + unidade / 2) / unidade}$sufixo"
            decimos % 10 == 0L -> "${decimos / 10}$sufixo"
            else -> "${decimos / 10},${decimos % 10}$sufixo"
        }
    }

    /**
     * Prêmio por extenso, para as telas: "R$ 170 Milhões", "R$ 1,7 Milhão", "R$ 450 Mil", "R$ 1,2 Bilhão".
     * [maiuscula] = false dá "170 milhões" (lista das apostas do mês).
     */
    fun porExtenso(centavos: Long, comRs: Boolean = true, maiuscula: Boolean = true): String {
        val reais = centavos / 100
        val (unidade, singular, plural) = when {
            reais >= 1_000_000_000L -> Triple(1_000_000_000L, "Bilhão", "Bilhões")
            reais >= 1_000_000L -> Triple(1_000_000L, "Milhão", "Milhões")
            reais >= 1_000L -> Triple(1_000L, "Mil", "Mil")
            else -> return (if (comRs) "R$ " else "") + formatar(centavos)
        }
        val decimos = (reais * 10 + unidade / 2) / unidade
        val numero = when {
            decimos >= 1000 -> "${(reais + unidade / 2) / unidade}"
            decimos % 10 == 0L -> "${decimos / 10}"
            else -> "${decimos / 10},${decimos % 10}"
        }
        val palavra = if (decimos < 20) singular else plural
        val texto = "$numero ${if (maiuscula) palavra else palavra.lowercase()}"
        return if (comRs) "R$ $texto" else texto
    }
}

package io.github.marcosfaria94.bolao.domain

import kotlin.math.cos
import kotlin.math.sin

/**
 * As tarjas das modalidades, como as das Loterias Caixa (desde a 0.27, no lugar das bolas com a sigla):
 * a faixa na cor da modalidade, o canto esquerdo mais escuro (ou mais claro, na Timemania) cortado em
 * diagonal, o trevo de quatro folhas em forma de coração e o nome em letra minúscula ("mega-sena"),
 * encostado à direita. As cores foram tiradas da página de resultados do site da Caixa.
 *
 * Aqui ficam só os dados (cores, nomes e o desenho do trevo), para as telas (ui/comum/Tarjas.kt) e o PDF
 * do Compartilhar (pdf/JogosPdf.kt) desenharem igual. Medidas em frações da altura da tarja.
 */
object Tarjas {

    /** As cores de uma tarja (ARGB): o fundo, o canto da esquerda, as duas folhas claras, as duas cheias e o nome. */
    data class Cores(val fundo: Long, val faixa: Long, val folhaClara: Long, val folhaCheia: Long, val texto: Long)

    private const val BRANCO = 0xFFFFFFFF

    fun cores(m: Modalidade): Cores = when (m) {
        Modalidade.MEGA_SENA -> Cores(0xFF4DA455, 0xFF419750, 0xFF71C89E, BRANCO, BRANCO)
        Modalidade.LOTOFACIL -> Cores(0xFF9D2F90, 0xFF902783, 0xFFC490C0, BRANCO, BRANCO)
        Modalidade.QUINA -> Cores(0xFF292D90, 0xFF272684, 0xFF8A82BD, BRANCO, BRANCO)
        Modalidade.LOTOMANIA -> Cores(0xFFF29A38, 0xFFEC8E34, 0xFFF6C990, BRANCO, BRANCO)
        // A Timemania é a única com o fundo claro: o canto fica mais claro e o nome e as folhas, verdes.
        Modalidade.TIMEMANIA -> Cores(0xFFFDF451, 0xFFFDF7A1, 0xFF71C89E, 0xFF4CA555, 0xFF41A83B)
        Modalidade.DUPLA_SENA -> Cores(0xFFB23547, 0xFFA3303D, 0xFFD5928A, BRANCO, BRANCO)
        Modalidade.DIA_DE_SORTE -> Cores(0xFFE6C947, 0xFFD8BD3C, 0xFFF4E5A0, BRANCO, BRANCO)
        Modalidade.SUPER_SETE -> Cores(0xFFAECD55, 0xFF90C156, 0xFFDDE7A2, BRANCO, BRANCO)
        Modalidade.MAIS_MILIONARIA -> Cores(0xFF2C2F74, 0xFF342A5C, 0xFF5B5C95, BRANCO, BRANCO)
        Modalidade.LOTECA -> Cores(0xFFEA3931, 0xFFDC3029, 0xFFF19C7F, BRANCO, BRANCO)
        Modalidade.FEDERAL -> Cores(0xFF346F99, 0xFF295C79, 0xFF68B6E4, BRANCO, BRANCO)
    }

    /** O nome na tarja, como a Caixa escreve: em minúsculas. */
    fun nome(m: Modalidade): String = when (m) {
        Modalidade.MEGA_SENA -> "mega-sena"
        Modalidade.LOTOFACIL -> "lotofácil"
        Modalidade.QUINA -> "quina"
        Modalidade.LOTOMANIA -> "lotomania"
        Modalidade.TIMEMANIA -> "timemania"
        Modalidade.DUPLA_SENA -> "dupla sena"
        Modalidade.DIA_DE_SORTE -> "dia de sorte"
        Modalidade.SUPER_SETE -> "super sete"
        Modalidade.MAIS_MILIONARIA -> "+milionária"
        Modalidade.LOTECA -> "loteca"
        Modalidade.FEDERAL -> "federal"
    }

    /** O nome da tarja do concurso: o do concurso especial ("mega da virada") ou o da modalidade. */
    fun nome(m: Modalidade, especial: ConcursoEspecial?): String = especial?.tarja ?: nome(m)

    // ---------- medidas (em frações da altura da tarja) ----------

    /** O canto da esquerda vai, em diagonal, de 1,72 alturas (em cima) a 0,75 altura (embaixo). */
    const val FAIXA_EM_CIMA = 1.72f
    const val FAIXA_EMBAIXO = 0.75f

    /** O centro do trevo fica a meia altura da borda esquerda; o trevo ocupa 0,66 da altura. */
    const val CENTRO_DO_TREVO = 0.5f
    const val TAMANHO_DO_TREVO = 0.66f

    /** O nome: a letra com 0,56 da altura, a 0,22 altura da borda direita. */
    const val LETRA = 0.56f
    const val MARGEM_DO_NOME = 0.22f

    /** Os cantos arredondados. */
    const val CANTO = 0.06f

    /** Onde o nome começa a poder ser escrito (depois do trevo). */
    const val INICIO_DO_NOME = 1.0f

    // ---------- o trevo ----------

    /**
     * Uma folha do trevo (um coração, como o do ícone do app), com a ponta em (0, 0) e a folha para cima:
     * 4 curvas cúbicas, cada uma com os dois pontos de controle e o ponto final (x1, y1, x2, y2, x3, y3).
     */
    private val FOLHA: List<FloatArray> = listOf(
        floatArrayOf(-10f, -10f, -40f, -30f, -40f, -55f),
        floatArrayOf(-40f, -75f, -20f, -85f, 0f, -68f),
        floatArrayOf(20f, -85f, 40f, -75f, 40f, -55f),
        floatArrayOf(40f, -30f, 10f, -10f, 0f, 0f),
    )

    /** O vão entre as folhas (a cruz no meio do trevo). */
    private const val VAO = 5f

    /** O raio do trevo inteiro, nas mesmas unidades da [FOLHA] (com o [VAO]). */
    private const val RAIO = 77.9f

    /** As 4 folhas: o ângulo (de cima, no sentido do relógio) e se é cheia (branca) ou clara. */
    val FOLHAS: List<Pair<Float, Boolean>> = listOf(-45f to true, 45f to false, 135f to true, 225f to false)

    /**
     * A folha de [angulo] graus pronta para desenhar num trevo de centro ([cx], [cy]) e [diametro]: o ponto
     * de partida (x, y) e as curvas, como em [FOLHA].
     */
    fun folha(angulo: Float, cx: Float, cy: Float, diametro: Float): Pair<FloatArray, List<FloatArray>> {
        val escala = diametro / 2f / RAIO
        val r = Math.toRadians(angulo.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        fun ponto(x: Float, y: Float): FloatArray {
            val y2 = y - VAO
            return floatArrayOf(cx + escala * (x * c - y2 * s), cy + escala * (x * s + y2 * c))
        }
        val inicio = ponto(0f, 0f)
        val curvas = FOLHA.map { k -> ponto(k[0], k[1]) + ponto(k[2], k[3]) + ponto(k[4], k[5]) }
        return inicio to curvas
    }
}

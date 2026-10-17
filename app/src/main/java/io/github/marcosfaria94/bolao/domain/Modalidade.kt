package io.github.marcosfaria94.bolao.domain

/** Como é o volante da aposta simples de cada modalidade. */
enum class Formato { NUMEROS, COLUNAS, LOTECA, FEDERAL }

/** Escolha adicional da aposta (Timemania e Dia de Sorte). */
enum class Extra { NENHUM, TIME_DO_CORACAO, MES_DE_SORTE }

/**
 * Modalidades das Loterias Caixa, com a sigla do termo (Parte 4).
 * Os preços são os da aposta simples em out/2026 e podem ser editados nas Configurações.
 */
enum class Modalidade(
    val sigla: String,
    val nome: String,
    /** Nome usado na API da Caixa: .../portaldeloterias/api/{slug}/{concurso} */
    val slugApi: String,
    val precoPadrao: Long,
    /** Cor da modalidade (ARGB). */
    val cor: Long,
    val formato: Formato,
    /** Menor e maior número do volante. */
    val menor: Int = 0,
    val maior: Int = 0,
    /** Quantos números a aposta simples marca. */
    val qtdAposta: Int = 0,
    /** Quantos números são sorteados (em cada sorteio). */
    val qtdSorteada: Int = 0,
    /** Dupla Sena tem 2 sorteios por concurso. */
    val sorteios: Int = 1,
    /** +Milionária: trevos marcados e sorteados (de 1 a 6). */
    val trevos: Int = 0,
    val extra: Extra = Extra.NENHUM,
) {
    MEGA_SENA("MS", "Mega-Sena", "megasena", 600, 0xFF209869, Formato.NUMEROS, 1, 60, 6, 6),
    LOTOFACIL("LF", "Lotofácil", "lotofacil", 350, 0xFF930089, Formato.NUMEROS, 1, 25, 15, 15),
    QUINA("QN", "Quina", "quina", 300, 0xFF260085, Formato.NUMEROS, 1, 80, 5, 5),
    LOTOMANIA("LM", "Lotomania", "lotomania", 300, 0xFFF78100, Formato.NUMEROS, 0, 99, 50, 20),
    TIMEMANIA("TM", "Timemania", "timemania", 350, 0xFF00A33A, Formato.NUMEROS, 1, 80, 10, 7, extra = Extra.TIME_DO_CORACAO),
    DUPLA_SENA("DP", "Dupla Sena", "duplasena", 300, 0xFFA61324, Formato.NUMEROS, 1, 50, 6, 6, sorteios = 2),
    DIA_DE_SORTE("DS", "Dia de Sorte", "diadesorte", 250, 0xFFCB852B, Formato.NUMEROS, 1, 31, 7, 7, extra = Extra.MES_DE_SORTE),
    SUPER_SETE("SS", "Super Sete", "supersete", 300, 0xFF7FA329, Formato.COLUNAS, 0, 9, 7, 7),
    MAIS_MILIONARIA("MI", "+Milionária", "maismilionaria", 600, 0xFF2E3078, Formato.NUMEROS, 1, 50, 6, 6, trevos = 2),
    LOTECA("LO", "Loteca", "loteca", 400, 0xFFFB1F00, Formato.LOTECA, qtdAposta = 14),
    FEDERAL("FE", "Loteria Federal", "federal", 0, 0xFF103099, Formato.FEDERAL);

    /** Preço variável: o valor é digitado na hora (Federal). */
    val precoVariavel: Boolean get() = this == FEDERAL

    /** Lotomania mostra "00" a "99"; as outras, "01" em diante com dois dígitos. */
    fun rotulo(numero: Int): String = numero.toString().padStart(2, '0')

    companion object {
        fun porSigla(sigla: String): Modalidade? = entries.firstOrNull { it.sigla == sigla }
    }
}

/** Meses do Dia de Sorte, na grafia usada pela Caixa. */
val MESES_DE_SORTE = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
)

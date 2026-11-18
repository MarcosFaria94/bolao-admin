package io.github.marcosfaria94.bolao.domain

/**
 * As chaves Pix nas telas (desde a 0.38, pedido do usuário). A chave fica guardada inteira (o copiar e as mensagens do
 * aporte e do rateio usam ela toda, senão o Pix não vai); só o que aparece na tela muda:
 * - CPF: os 3 primeiros e os 2 últimos algarismos, "123.***.***-09";
 * - e-mail: como está;
 * - telefone: "(31) 99999-8888" (sem o +55);
 * - aleatória: ela mesma (a linha corta com reticências quando não cabe).
 * No cadastro, uma chave com CPF pede confirmação ([TipoDeChave.CPF]: o CPF é um dado pessoal e vai inteiro nos Pix).
 */
enum class TipoDeChave { CPF, CNPJ, EMAIL, TELEFONE, ALEATORIA, OUTRA }

object ChavesPix {

    private val ALEATORIA = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val ALEATORIA_SEM_TRACOS = Regex("^[0-9a-fA-F]{32}$")
    private val CPF_COM_PONTOS = Regex("^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$")
    private val CNPJ_COM_PONTOS = Regex("^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$")

    /** Só algarismos e os sinais de quem digita um número (espaço, ponto, traço, parênteses, barra, +). */
    private val SO_NUMERO = Regex("^[\\d\\s.\\-()/+]+$")

    /** O tipo da [chave] (do jeito que foi digitada). */
    fun tipo(chave: String): TipoDeChave {
        val s = chave.trim()
        if (s.isEmpty()) return TipoDeChave.OUTRA
        if ('@' in s) return TipoDeChave.EMAIL
        if (ALEATORIA.matches(s) || ALEATORIA_SEM_TRACOS.matches(s) && s.any { it.isLetter() }) return TipoDeChave.ALEATORIA
        if (!SO_NUMERO.matches(s)) return TipoDeChave.OUTRA
        val d = s.filter(Char::isDigit)
        return when {
            CPF_COM_PONTOS.matches(s) -> TipoDeChave.CPF
            CNPJ_COM_PONTOS.matches(s) -> TipoDeChave.CNPJ
            s.startsWith("+") || '(' in s -> if (telefone(d) != null) TipoDeChave.TELEFONE else TipoDeChave.OUTRA
            d.length == 11 && cpfValido(d) -> TipoDeChave.CPF
            d.length == 14 && cnpjValido(d) -> TipoDeChave.CNPJ
            telefone(d) != null -> TipoDeChave.TELEFONE
            else -> TipoDeChave.OUTRA
        }
    }

    /** A chave como aparece na tela (ver [ChavesPix]). */
    fun exibir(chave: String): String {
        val s = chave.trim()
        val d = s.filter(Char::isDigit)
        return when (tipo(s)) {
            TipoDeChave.CPF -> "${d.substring(0, 3)}.***.***-${d.substring(9, 11)}"
            TipoDeChave.TELEFONE -> telefone(d)?.let { n ->
                if (n.length == 11) "(${n.substring(0, 2)}) ${n.substring(2, 7)}-${n.substring(7)}"
                else "(${n.substring(0, 2)}) ${n.substring(2, 6)}-${n.substring(6)}"
            } ?: s
            else -> s
        }
    }

    /** O número nacional do telefone (DDD + número, 10 ou 11 algarismos) ou null se não parecer um telefone. */
    private fun telefone(digitos: String): String? {
        val n = when {
            digitos.length in 12..13 && digitos.startsWith("55") -> digitos.substring(2)
            else -> digitos
        }
        if (n.length !in 10..11) return null
        val ddd = n.substring(0, 2).toInt()
        if (ddd < 11 || n[2] == '0') return null
        // Celular: 11 algarismos com o 9 na frente do número.
        if (n.length == 11 && n[2] != '9') return null
        return n
    }

    /** Os dois dígitos verificadores do CPF conferem (e não são todos iguais, como 111.111.111-11). */
    fun cpfValido(d: String): Boolean {
        if (d.length != 11 || d.all { it == d[0] }) return false
        fun dv(ate: Int): Int {
            val soma = (0 until ate).sumOf { (d[it] - '0') * (ate + 1 - it) }
            val r = soma * 10 % 11
            return if (r == 10) 0 else r
        }
        return dv(9) == d[9] - '0' && dv(10) == d[10] - '0'
    }

    private fun cnpjValido(d: String): Boolean {
        if (d.length != 14 || d.all { it == d[0] }) return false
        fun dv(pesos: List<Int>): Int {
            val soma = pesos.indices.sumOf { (d[it] - '0') * pesos[it] }
            val r = soma % 11
            return if (r < 2) 0 else 11 - r
        }
        val p1 = listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        return dv(p1) == d[12] - '0' && dv(listOf(6) + p1) == d[13] - '0'
    }

    /** O aviso do cadastro para uma chave com CPF (a janela que pede a confirmação). */
    fun avisoDoCpf(integrantes: List<Int>): String {
        val quem = when (integrantes.size) {
            1 -> "A chave Pix do integrante ${integrantes[0]} é um CPF."
            else -> "As chaves Pix dos integrantes ${integrantes.dropLast(1).joinToString(", ")} e ${integrantes.last()} são CPF."
        }
        return "$quem O CPF é um dado pessoal: quem recebe a chave, na mensagem do aporte e nos rateios, vê o número inteiro " +
            "(nas telas do app ele aparece escondido, 123.***.***-09). Se der, prefira e-mail, telefone ou chave aleatória."
    }

    const val TITULO_DO_AVISO_DO_CPF = "Chave Pix com CPF"
    const val USAR_O_CPF = "Usar mesmo assim"
    const val TROCAR_A_CHAVE = "Trocar a chave"

    /** O lembrete embaixo do campo, enquanto a chave digitada é um CPF. */
    const val LEMBRETE_DO_CPF = "Chave com CPF: na tela ela aparece escondida, mas vai inteira nos Pix."
}

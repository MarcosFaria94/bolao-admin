package io.github.marcosfaria94.bolao.domain

/** CPF no formato do termo: só os 6 dígitos do meio aparecem ("***.099.906-**"). */
object Cpf {

    /**
     * Aceita o CPF completo (com ou sem pontos) ou já mascarado. O app guarda só a forma
     * mascarada. Devolve "" para vazio e null se não der para entender.
     */
    fun mascarar(texto: String): String? {
        val t = texto.trim()
        if (t.isEmpty()) return ""
        val digitos = t.filter { it.isDigit() }
        return when {
            digitos.length == 11 -> "***.${digitos.substring(3, 6)}.${digitos.substring(6, 9)}-**"
            t.contains('*') && digitos.length == 6 -> "***.${digitos.substring(0, 3)}.${digitos.substring(3, 6)}-**"
            else -> null
        }
    }
}

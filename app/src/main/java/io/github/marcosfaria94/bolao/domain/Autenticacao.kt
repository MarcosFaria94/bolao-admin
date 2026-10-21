package io.github.marcosfaria94.bolao.domain

import java.security.MessageDigest

/**
 * Código de autenticação dos comprovantes: SHA-256 do conteúdo do registro, encadeado com o
 * código do comprovante anterior. Mudar qualquer registro antigo (ou um anexo) quebra a corrente,
 * e a auditoria mostra onde.
 *
 * O app guarda o SHA-256 inteiro; no comprovante vai só um código curto, de 8 algarismos
 * (ex.: "4829-1736"), fácil de digitar na conferência.
 */
object Autenticacao {

    /** Algarismos do código impresso. */
    const val DIGITOS = 8

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }

    fun sha256(texto: String): String = sha256(texto.encodeToByteArray())

    /** Código de um comprovante: [anterior] (null no primeiro) + as [partes] do registro. */
    fun calcular(anterior: String?, partes: List<String>): String =
        sha256((listOf(anterior.orEmpty()) + partes).joinToString("\n"))

    /** Os 8 algarismos do código impresso, tirados do começo do SHA-256 ("48291736"). */
    fun curto(codigo: String): String {
        // 15 dígitos hexadecimais (60 bits) cabem num Long.
        val n = codigo.trim().take(15).toLongOrNull(16) ?: 0L
        var limite = 1L
        repeat(DIGITOS) { limite *= 10 }
        return (n % limite).toString().padStart(DIGITOS, '0')
    }

    /** Forma impressa no comprovante: "4829-1736". */
    fun formatar(codigo: String): String = curto(codigo).chunked(4).joinToString("-")
}

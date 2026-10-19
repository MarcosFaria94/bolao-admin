package io.github.marcosfaria94.bolao.domain

import java.security.MessageDigest

/**
 * Código de autenticação dos comprovantes: SHA-256 do conteúdo do registro, encadeado com o
 * código do comprovante anterior. Mudar qualquer registro antigo (ou um anexo) quebra a corrente,
 * e a auditoria mostra onde.
 */
object Autenticacao {

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }

    fun sha256(texto: String): String = sha256(texto.encodeToByteArray())

    /** Código de um comprovante: [anterior] (null no primeiro) + as [partes] do registro. */
    fun calcular(anterior: String?, partes: List<String>): String =
        sha256((listOf(anterior.orEmpty()) + partes).joinToString("\n"))

    /** Forma curta, para o comprovante: "3F2A-9C1E-5B7D-0A41-77C2-8B90". */
    fun formatar(codigo: String): String = codigo.take(24).chunked(4).joinToString("-")
}

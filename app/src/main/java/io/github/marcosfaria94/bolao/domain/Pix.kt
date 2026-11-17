package io.github.marcosfaria94.bolao.domain

import java.text.Normalizer

/**
 * O código do Pix (BR Code, o "Pix Copia e Cola" que vai no QR Code; desde a 0.37, na tela do Apoie): o padrão EMV do
 * Banco Central, com a chave, o nome e a cidade de quem recebe, sem valor (quem paga escolhe) e o CRC16 no fim. Cada
 * campo é "ID (2 algarismos) + tamanho (2 algarismos) + valor".
 */
object Pix {

    /** O código do Pix estático para a [chave], com o [nome] (até 25 letras) e a [cidade] (até 15) de quem recebe. */
    fun copiaECola(chave: String, nome: String, cidade: String, identificador: String = "***"): String {
        val conta = campo("00", "br.gov.bcb.pix") + campo("01", chave.trim())
        val semCrc = campo("00", "01") +
            campo("26", conta) +
            campo("52", "0000") +
            campo("53", "986") +
            campo("58", "BR") +
            campo("59", limpar(nome, 25)) +
            campo("60", limpar(cidade, 15)) +
            campo("62", campo("05", identificador)) +
            "6304"
        return semCrc + crc16(semCrc)
    }

    /** Um campo: o [id], o tamanho do [valor] com 2 algarismos e o valor. */
    fun campo(id: String, valor: String): String = id + valor.length.toString().padStart(2, '0') + valor

    /** O texto sem acentos, em maiúsculas e com no máximo [maximo] caracteres (o que os bancos aceitam no nome e na cidade). */
    fun limpar(texto: String, maximo: Int): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
            .uppercase().filter { it.isLetterOrDigit() || it == ' ' }.trim().take(maximo).trim()

    /** O CRC16-CCITT (polinômio 0x1021, começando em 0xFFFF) do [texto], em 4 algarismos hexadecimais maiúsculos. */
    fun crc16(texto: String): String {
        var crc = 0xFFFF
        for (b in texto.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc.toString(16).uppercase().padStart(4, '0')
    }
}

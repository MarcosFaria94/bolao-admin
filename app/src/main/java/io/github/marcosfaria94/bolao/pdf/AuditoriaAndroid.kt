package io.github.marcosfaria94.bolao.pdf

import android.content.Context
import android.net.Uri
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Downloads
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.imagem.Anexos
import io.github.marcosfaria94.bolao.imagem.Recibos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Geração dos PDFs no celular: auditoria (com as imagens guardadas) e termo sem assinaturas. */
object PdfsDoApp {

    private class Fonte(val context: Context, val e: Estado) : AuditoriaPdf.FonteDeImagens {
        val repo = context.bolao.repo
        override fun recibo(c: Comprovante): ByteArray = Recibos.arquivoAgora(context, e, c).readBytes()
        override fun paginas(a: Arquivo): List<ByteArray> = Anexos.jpegsParaPdf(repo.arquivo(a), a.mime)
        override fun sha256(a: Arquivo): String? = repo.sha256(a)
    }

    private fun data(d: LocalDate) = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)

    fun nomeAuditoria(e: Estado, de: LocalDate, ate: LocalDate): String =
        (if (e.exemplo) "bolao-teste-auditoria-" else "bolao-auditoria-") + data(de) + "-a-" + data(ate) + ".pdf"

    /** Gera a auditoria em Downloads/Bolao. */
    suspend fun auditoriaEmDownloads(
        context: Context,
        e: Estado,
        de: LocalDate,
        ate: LocalDate,
        progresso: (Int, Int) -> Unit = { _, _ -> },
    ): Pair<Uri, AuditoriaPdf.Resultado> {
        var resultado: AuditoriaPdf.Resultado? = null
        val uri = Downloads.salvar(context, nomeAuditoria(e, de, ate), "application/pdf") { saida ->
            withContext(Dispatchers.IO) {
                resultado = AuditoriaPdf.gerar(e, de, ate, Fonte(context, e), saida, progresso = progresso)
            }
        }
        return uri to resultado!!
    }

    /** Gera a auditoria num arquivo do app (para anexar no WhatsApp). */
    suspend fun auditoriaEmArquivo(context: Context, e: Estado, de: LocalDate, ate: LocalDate, destino: File): AuditoriaPdf.Resultado =
        withContext(Dispatchers.IO) {
            destino.parentFile?.mkdirs()
            val temp = File(destino.path + ".tmp")
            val r = temp.outputStream().buffered().use { AuditoriaPdf.gerar(e, de, ate, Fonte(context, e), it) }
            if (!temp.renameTo(destino)) {
                destino.delete()
                temp.renameTo(destino)
            }
            r
        }

    /** Termo sem assinaturas, com os dados do cadastro, em Downloads/Bolao. */
    suspend fun termoEmDownloads(context: Context, e: Estado): Uri =
        Downloads.salvar(context, (if (e.exemplo) "termo-bolao-teste-" else "termo-bolao-") + data(LocalDate.now()) + ".pdf", "application/pdf") { saida ->
            withContext(Dispatchers.Default) { TermoPdf.gerar(e, saida) }
        }

    /** Início do período da auditoria: o cadastro ou o primeiro registro. */
    fun inicio(e: Estado): LocalDate =
        listOfNotNull(e.cadastradoEm, e.lancamentos.minOfOrNull { it.data }, e.comprovantes.minOfOrNull { it.data }).minOrNull() ?: LocalDate.now()
}

package io.github.marcosfaria94.bolao.ui.comum

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.domain.Mensagens
import java.io.File

/** Anexo para mandar junto com a mensagem. */
data class Anexo(val arquivo: File, val mime: String)

object Compartilhar {

    private val PACOTES_WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** Primeira linha das mensagens enviadas no modo de teste, para ninguém confundir com o bolão real. */
    const val AVISO_TESTE = "*TESTE do app (dados de exemplo) – pode ignorar*"

    private fun instalado(context: Context, pacote: String): Boolean = try {
        context.packageManager.getPackageInfo(pacote, 0)
        true
    } catch (e: Exception) {
        false
    }

    fun uri(context: Context, arquivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.arquivos", arquivo)

    /**
     * Abre o WhatsApp com a mensagem (em bloco monoespaçado) e os anexos prontos.
     * O Administrador escolhe o grupo e toca em Enviar.
     */
    fun whatsapp(context: Context, texto: String, anexos: List<Anexo> = emptyList(), monoespacado: Boolean = true) {
        val bloco = if (monoespacado) Mensagens.monoespacado(texto) else texto
        val mensagem = if (context.bolao.repo.emTeste) "$AVISO_TESTE\n$bloco" else bloco
        val uris = anexos.map { uri(context, it.arquivo) }
        val intent = when (uris.size) {
            0 -> Intent(Intent.ACTION_SEND).setType("text/plain")
            1 -> Intent(Intent.ACTION_SEND).setType(anexos[0].mime).putExtra(Intent.EXTRA_STREAM, uris[0])
            else -> Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType(if (anexos.all { it.mime.startsWith("image/") }) "image/*" else "*/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        intent.putExtra(Intent.EXTRA_TEXT, mensagem)
        if (uris.isNotEmpty()) {
            val clip = ClipData.newRawUri("anexo", uris[0])
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            intent.clipData = clip
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Cópia de segurança: se o WhatsApp não puser o texto junto do anexo, é só colar.
        if (uris.isNotEmpty()) copiar(context, "Mensagem do bolão", mensagem)
        val pacote = PACOTES_WHATSAPP.firstOrNull { instalado(context, it) }
        try {
            if (pacote != null) {
                context.startActivity(intent.setPackage(pacote).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                context.startActivity(Intent.createChooser(intent, "Enviar mensagem").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Não encontrei o WhatsApp neste celular.", Toast.LENGTH_LONG).show()
        }
    }

    /** Abre um anexo guardado (comprovante, termo) no visualizador do celular. */
    fun abrir(context: Context, arquivo: File, mime: String) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri(context, arquivo), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app para abrir este arquivo.", Toast.LENGTH_LONG).show()
        }
    }

    /** Pasta para arquivos gerados na hora (ex.: imagem da apuração). */
    fun pastaTemporaria(context: Context): File = File(context.cacheDir, "compartilhar").apply { mkdirs() }

    fun textoParaCopiar(texto: String): String = Mensagens.monoespacado(texto)
}

package io.github.marcosfaria94.bolao.ui.comum

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.imagem.Recibos
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

    private fun comAvisoDeTeste(context: Context, texto: String): String = when {
        !context.bolao.repo.emTeste -> texto
        texto.isBlank() -> AVISO_TESTE
        else -> "$AVISO_TESTE\n$texto"
    }

    /**
     * Abre o WhatsApp com o texto e os anexos prontos (o Administrador escolhe o grupo e envia).
     * Com [texto] vazio vão só os anexos (o "Saldo atual" do termo não tem mensagem).
     * Devolve false se não deu para abrir.
     */
    fun whatsapp(context: Context, texto: String, anexos: List<Anexo> = emptyList()): Boolean {
        val mensagem = comAvisoDeTeste(context, texto)
        val uris = anexos.map { uri(context, it.arquivo) }
        val intent = when (uris.size) {
            0 -> Intent(Intent.ACTION_SEND).setType("text/plain")
            1 -> Intent(Intent.ACTION_SEND).setType(anexos[0].mime).putExtra(Intent.EXTRA_STREAM, uris[0])
            else -> Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType(if (anexos.all { it.mime.startsWith("image/") }) "image/*" else "*/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        if (mensagem.isNotBlank()) intent.putExtra(Intent.EXTRA_TEXT, mensagem)
        if (uris.isNotEmpty()) {
            val clip = ClipData.newRawUri("anexo", uris[0])
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            intent.clipData = clip
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Se o WhatsApp não puser o texto junto dos anexos, é só colar.
            if (mensagem.isNotBlank()) copiar(context, "Mensagem do bolão", mensagem)
        }
        val pacote = PACOTES_WHATSAPP.firstOrNull { instalado(context, it) }
        return try {
            if (pacote != null) {
                context.startActivity(intent.setPackage(pacote).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                context.startActivity(Intent.createChooser(intent, "Enviar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Não encontrei o WhatsApp neste celular.", Toast.LENGTH_LONG).show()
            false
        }
    }

    /** Anexos que vão ao grupo: a imagem do comprovante e os anexos originais, sem mudança. */
    suspend fun anexosDo(context: Context, e: Estado, c: Comprovante): List<Anexo> {
        val repo = context.bolao.repo
        val recibo = Recibos.arquivo(context, e, c)
        return listOf(Anexo(recibo, "image/png")) + c.anexos.map { Anexo(repo.arquivo(it), it.mime) }
    }

    /**
     * Envia um comprovante ao grupo: no texto só o título (como no termo); a imagem do
     * comprovante e os anexos vão juntos. Fica registrado que foi aberto no WhatsApp.
     */
    suspend fun comprovante(context: Context, e: Estado, c: Comprovante): Boolean {
        val ok = whatsapp(context, c.titulo, anexosDo(context, e, c))
        if (ok) {
            runCatching { context.bolao.repo.atualizar { Operacoes.registrarEnvio(it, c.id, System.currentTimeMillis()) } }
        }
        return ok
    }

    /** Abre um anexo guardado (comprovante, termo, PDF) no visualizador do celular. */
    fun abrir(context: Context, arquivo: File, mime: String) = abrir(context, uri(context, arquivo), mime)

    fun abrir(context: Context, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app para abrir este arquivo.", Toast.LENGTH_LONG).show()
        }
    }

    /** Menu do sistema para enviar um arquivo (Drive, e-mail, WhatsApp...). */
    fun enviarArquivo(context: Context, uri: Uri, mime: String, titulo: String) {
        val enviar = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(enviar, titulo).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app para enviar este arquivo.", Toast.LENGTH_LONG).show()
        }
    }

    /** Pasta para arquivos gerados na hora. */
    fun pastaTemporaria(context: Context): File = File(context.cacheDir, "compartilhar").apply { mkdirs() }
}

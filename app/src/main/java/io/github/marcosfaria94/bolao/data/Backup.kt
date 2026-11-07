package io.github.marcosfaria94.bolao.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup completo num .zip: estado.json + todos os anexos (comprovantes, termo).
 * O arquivo vai para Downloads/Bolao e pode ser enviado para o Drive, e-mail etc.
 * Desde a 0.21 o backup é da sessão em uso (e a restauração também vai para ela).
 */
class Backup(private val context: Context) {

    private val sessoes: Sessoes get() = (context.applicationContext as io.github.marcosfaria94.bolao.BolaoApp).sessoes
    private val repo: Repositorio get() = sessoes.repoAtivo

    private companion object {
        const val ESTADO = "estado.json"
        const val PASTA = "arquivos/"
    }

    suspend fun gerar(saida: OutputStream) = withContext(Dispatchers.IO) {
        // Dados e anexos do mesmo espaço (reais ou do modo de teste).
        val r = repo.retrato()
        ZipOutputStream(BufferedOutputStream(saida)).use { zip ->
            zip.putNextEntry(ZipEntry(ESTADO))
            zip.write(repo.json.encodeToString(Estado.serializer(), r.estado).encodeToByteArray())
            zip.closeEntry()
            r.pastaArquivos.listFiles()?.filter { it.isFile }?.forEach { f ->
                zip.putNextEntry(ZipEntry(PASTA + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("LEIA-ME.txt"))
            zip.write(
                ("Backup do app Bolão dos Amigos.\n" +
                    (if (r.teste) "Atenção: backup do MODO DE TESTE (dados de exemplo).\n" else "") +
                    "Para restaurar: abra o app > Ajustes > Restaurar backup e escolha este arquivo.\n")
                    .encodeToByteArray(),
            )
            zip.closeEntry()
        }
    }

    /** Grava o backup em Downloads/Bolao e devolve o endereço do arquivo. */
    suspend fun salvarEmDownloads(): Uri {
        val agora = LocalDateTime.now()
        // Com o nome da sessão (desde a 0.21): "bolao-backup-bolao-2-2026-11-07-1300.zip".
        val sessao = if (repo.emTeste) "teste-" else sessoes.nome(repo.sessao).let { n ->
            java.text.Normalizer.normalize(n, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
                .replace(Regex("[^a-z0-9]+"), "-").trim('-').take(30).let { if (it.isBlank()) "" else "$it-" }
        }
        val nome = "bolao-backup-$sessao" + "%04d-%02d-%02d-%02d%02d.zip".format(
            agora.year, agora.monthValue, agora.dayOfMonth, agora.hour, agora.minute,
        )
        return Downloads.salvar(context, nome, "application/zip") { gerar(it) }
    }

    data class Restaurado(val estado: Estado, val anexos: Int)

    /** Lê um backup e substitui todos os dados do app por ele. */
    suspend fun restaurar(entrada: InputStream): Restaurado = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "restauracao-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var estado: Estado? = null
            var anexos = 0
            ZipInputStream(BufferedInputStream(entrada)).use { zip ->
                while (true) {
                    val item = zip.nextEntry ?: break
                    val nome = item.name
                    if (item.isDirectory) continue
                    if (nome == ESTADO) {
                        estado = Migracao.migrar(repo.json.decodeFromString(Estado.serializer(), zip.readBytes().decodeToString()), java.time.LocalDate.now())
                    } else if (nome.startsWith(PASTA)) {
                        val simples = nome.removePrefix(PASTA)
                        if (simples.isBlank() || simples.contains('/') || simples.contains('\\') || simples.contains("..")) continue
                        File(temp, simples).outputStream().use { zip.copyTo(it) }
                        anexos++
                    }
                }
            }
            val e = estado ?: throw IllegalArgumentException("Este arquivo não é um backup do Bolão.")
            val r = repo
            // Só pode haver uma sessão Individual (desde a 0.21).
            require(!e.individual || r.emTeste || !sessoes.temIndividual(exceto = r.sessao)) {
                "Este backup é do modo Individual e já existe a sessão Individual. Restaure-o nela."
            }
            r.restaurar(e, temp)
            r.limparArquivosSoltos()
            if (e.cadastroConcluido) {
                val atual = sessoes.sessao(r.sessao)
                // O nome da sessão continua (ou, num cadastro novo, vira o genérico).
                sessoes.nomear(r.sessao, if (atual?.individual == false) atual.nome else "", e.modo)
            }
            Restaurado(e, anexos)
        } finally {
            temp.deleteRecursively()
        }
    }
}

package io.github.marcosfaria94.bolao.imagem

import android.content.Context
import android.graphics.Bitmap
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.TipoComprovante
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * As imagens dos comprovantes (estilo papel térmico). A imagem é feita a partir do registro,
 * então pode ser refeita a qualquer momento (backup restaurado, dados de exemplo...): se o
 * arquivo não existir, é desenhado na hora.
 */
object Recibos {

    private val trava = Any()

    /** Tabela do rateio oficial (só no comprovante de apuração). */
    fun rateioDe(e: Estado, c: Comprovante): LayoutTermico.RateioOficial? {
        if (c.tipo != TipoComprovante.APURACAO) return null
        val a = e.apostas.firstOrNull { it.id == c.apostaId } ?: return null
        val ap = a.apuracao ?: return null
        if (ap.rateioOficial.isEmpty()) return null
        return LayoutTermico.RateioOficial(a.concurso, ap.rateioOficial, ap.faixasPremiadas.toSet(), ap.acumulou)
    }

    fun linhas(e: Estado, c: Comprovante): List<LayoutTermico.Linha> =
        LayoutTermico.deComprovante(e.nomeExibicao, c, rateioDe(e, c), teste = e.exemplo)

    /** Arquivo PNG do comprovante, desenhado agora se ainda não existir (fora da tela principal). */
    fun arquivoAgora(context: Context, e: Estado, c: Comprovante): File {
        val destino = context.bolao.repo.arquivo(c.imagem)
        synchronized(trava) {
            if (!destino.isFile || destino.length() == 0L) RenderTermico.salvar(context, linhas(e, c), destino)
        }
        return destino
    }

    suspend fun arquivo(context: Context, e: Estado, c: Comprovante): File =
        withContext(Dispatchers.Default) { arquivoAgora(context, e, c) }

    /** Imagem para mostrar na tela. */
    suspend fun bitmap(context: Context, linhas: List<LayoutTermico.Linha>): Bitmap =
        withContext(Dispatchers.Default) { RenderTermico.desenhar(context, linhas) }

    /** Como o comprovante vai ficar, antes de registrar (sem autenticação e com a faixa "PRÉVIA"). */
    fun previa(
        e: Estado,
        tipo: TipoComprovante,
        texto: String,
        anexos: Int,
        rateio: LayoutTermico.RateioOficial? = null,
    ): List<LayoutTermico.Linha> =
        LayoutTermico.montar(e.nomeExibicao, tipo, texto, anexos, null, null, rateio, teste = e.exemplo, previa = true)
}

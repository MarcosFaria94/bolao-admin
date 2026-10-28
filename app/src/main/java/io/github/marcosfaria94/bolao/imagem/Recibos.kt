package io.github.marcosfaria94.bolao.imagem

import android.content.Context
import android.graphics.Bitmap
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Apuracao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.domain.Extra
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
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

    fun linhas(e: Estado, c: Comprovante): List<LayoutTermico.Linha> =
        LayoutTermico.deComprovante(e.nomeExibicao, c, teste = e.exemplo, resultado = resultadoDo(e, c))

    /** O resultado oficial que vai no comprovante da apuração (os números sorteados e o rateio da Caixa). */
    fun resultadoDo(e: Estado, c: Comprovante): LayoutTermico.ResultadoOficial? {
        if (c.tipo != TipoComprovante.APURACAO) return null
        val a = e.apostas.firstOrNull { it.id == c.apostaId } ?: return null
        return a.apuracao?.let { resultadoDe(a, it) }
    }

    /** Os números sorteados de cada modalidade, como a Caixa divulga, e o rateio oficial da apuração [ap]. */
    fun resultadoDe(a: Aposta, ap: Apuracao): LayoutTermico.ResultadoOficial? {
        val m = a.modalidade
        fun nums(l: List<Int>) = l.map { if (m.formato == Formato.COLUNAS) it.toString() else m.rotulo(it) }
        val sorteados = buildList {
            when (m.formato) {
                Formato.NUMEROS -> {
                    if (m == Modalidade.DUPLA_SENA) {
                        add("1º SORTEIO" to nums(ap.dezenasSorteadas))
                        add("2º SORTEIO" to nums(ap.segundoSorteio))
                    } else {
                        add("SORTEADOS" to nums(ap.dezenasSorteadas))
                    }
                    if (m.trevos > 0) add("TREVOS" to ap.trevosSorteados.map { it.toString() })
                    when (m.extra) {
                        Extra.TIME_DO_CORACAO -> add("TIME DO CORAÇÃO" to listOfNotNull(ap.extraSorteado?.uppercase()))
                        Extra.MES_DE_SORTE -> add("MÊS DE SORTE" to listOfNotNull(ap.extraSorteado?.uppercase()))
                        Extra.NENHUM -> Unit
                    }
                }
                Formato.COLUNAS -> add("SORTEADOS" to nums(ap.dezenasSorteadas))
                Formato.LOTECA -> add("RESULTADOS" to ap.resultadosLoteca.map { it.ifBlank { "-" } })
                Formato.FEDERAL -> ap.premiosFederal.forEachIndexed { i, b -> add("${i + 1}º PRÊMIO" to listOf(b)) }
            }
        }.filter { it.second.isNotEmpty() }
        if (sorteados.isEmpty() && ap.rateioOficial.isEmpty()) return null
        return LayoutTermico.ResultadoOficial(a.concurso, sorteados, ap.rateioOficial, ap.faixasPremiadas.toSet(), ap.acumulou)
    }

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
        resultado: LayoutTermico.ResultadoOficial? = null,
    ): List<LayoutTermico.Linha> =
        LayoutTermico.montar(e.nomeExibicao, tipo, texto, anexos, null, null, teste = e.exemplo, previa = true, resultado = resultado)
}

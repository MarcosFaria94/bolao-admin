package io.github.marcosfaria94.bolao.notificacoes

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Lembretes
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.net.CaixaApi
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Roda de hora em hora, mesmo com o app fechado: confere os jogos já sorteados
 * (deixando a apuração pronta para confirmar) e mostra os lembretes do dia.
 * No modo de teste, faz o mesmo com os dados de exemplo e marca os avisos com "Teste".
 */
class Vigia(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val repo = ctx.bolao.repo
        val teste = repo.emTeste
        var e = repo.atual()
        val agora = LocalDateTime.now()

        // Conferência automática depois do horário do sorteio.
        for (a in Lembretes.aguardandoResultado(e, agora)) {
            try {
                val c = CaixaApi.concurso(a.modalidade, a.concurso)
                if (c.temResultado) {
                    val r = Conferencia.conferir(a.modalidade, a.palpite, c, a.bilheteFederal, a.fracoesFederal)
                    val ap = Operacoes.apuracaoDe(c, r, r.premio, System.currentTimeMillis())
                    e = repo.atualizar { Operacoes.guardarConferencia(it, a.id, ap) }
                }
            } catch (t: Throwable) {
                // Sem internet ou Caixa fora do ar: tenta de novo na próxima hora.
            }
        }

        // Sem permissão para notificar, os avisos ficam para a próxima vez (não contam como avisados).
        if (!Notificador.permitido(ctx)) return Result.success()

        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val avisados = prefs.getString(CHAVES, "").orEmpty().split('\n').filter { it.isNotBlank() }
        // Os avisos do modo de teste são guardados à parte (com prefixo), para não calar os reais.
        val doEspaco = avisados.filter { it.startsWith(PREFIXO_TESTE) == teste }.map { it.removePrefix(PREFIXO_TESTE) }.toSet()
        val novos = Lembretes.pendentes(e, agora, doEspaco)
        novos.forEach { l ->
            Notificador.mostrar(ctx, if (teste) l.copy(chave = PREFIXO_TESTE + l.chave, titulo = "Teste · ${l.titulo}") else l)
        }
        if (novos.isNotEmpty()) {
            val todas = (avisados + novos.map { (if (teste) PREFIXO_TESTE else "") + it.chave }).takeLast(MAX_CHAVES)
            prefs.edit().putString(CHAVES, todas.joinToString("\n")).apply()
        }
        return Result.success()
    }

    companion object {
        private const val NOME = "vigia-do-bolao"
        private const val NOME_AGORA = "vigia-agora"
        private const val PREFS = "lembretes"
        private const val CHAVES = "avisados"
        private const val PREFIXO_TESTE = "teste:"
        private const val MAX_CHAVES = 400

        fun agendar(context: Context) {
            val pedido = PeriodicWorkRequestBuilder<Vigia>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }

        /** Confere os sorteios e os lembretes agora, sem esperar a próxima hora. */
        fun rodarAgora(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOME_AGORA, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<Vigia>().build())
        }

        /** Esquece os avisos já dados no modo de teste (um exemplo novo pode avisar de novo). */
        fun esquecerAvisosDeTeste(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val reais = prefs.getString(CHAVES, "").orEmpty().split('\n').filter { it.isNotBlank() && !it.startsWith(PREFIXO_TESTE) }
            prefs.edit().putString(CHAVES, reais.joinToString("\n")).apply()
        }
    }
}

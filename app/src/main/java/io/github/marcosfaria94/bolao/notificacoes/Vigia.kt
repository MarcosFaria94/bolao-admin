package io.github.marcosfaria94.bolao.notificacoes

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
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
 */
class Vigia(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val repo = ctx.bolao.repo
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

        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val avisados = prefs.getString(CHAVES, "").orEmpty().split('\n').filter { it.isNotBlank() }
        val novos = Lembretes.pendentes(e, agora, avisados.toSet())
        novos.forEach { Notificador.mostrar(ctx, it) }
        if (novos.isNotEmpty()) {
            val todas = (avisados + novos.map { it.chave }).takeLast(MAX_CHAVES)
            prefs.edit().putString(CHAVES, todas.joinToString("\n")).apply()
        }
        return Result.success()
    }

    companion object {
        private const val NOME = "vigia-do-bolao"
        private const val PREFS = "lembretes"
        private const val CHAVES = "avisados"
        private const val MAX_CHAVES = 400

        fun agendar(context: Context) {
            val pedido = PeriodicWorkRequestBuilder<Vigia>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }
    }
}

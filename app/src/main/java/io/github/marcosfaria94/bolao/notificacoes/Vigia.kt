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
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.imagem.ImagemConferencia
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Roda de hora em hora, mesmo com o app fechado:
 * 1. confere os jogos já sorteados na Caixa (e de novo, até o rateio oficial sair);
 * 2. faz a apuração sozinha no dia seguinte ao sorteio, quando não há nada a confirmar
 *    (o envio do comprovante ao grupo vira pendência);
 * 3. mostra os dois avisos do celular: "Aposta premiada" e "Ação necessária".
 * No modo de teste faz o mesmo com os dados de exemplo e marca os avisos com "Teste".
 */
class Vigia(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val repo = ctx.bolao.repo
        val teste = repo.emTeste
        var e = repo.atual()
        val agora = LocalDateTime.now()

        // 1. Conferência automática depois do horário do sorteio.
        for (a in Pendencias.aguardandoResultado(e, agora)) {
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

        // 2. Apuração automática.
        for (a in Pendencias.prontasParaApurar(e, agora)) {
            try {
                apurar(ctx, a.id)
            } catch (t: Throwable) {
                // Fica para a próxima vez (ou para o Administrador, depois de dois dias).
            }
        }
        e = repo.atual()

        // 3. Avisos. Sem permissão, ficam para a próxima vez (não contam como avisados).
        if (!Notificador.permitido(ctx)) return Result.success()
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val avisados = prefs.getString(CHAVES, "").orEmpty().split('\n').filter { it.isNotBlank() }
        // Os avisos do modo de teste são guardados à parte (com prefixo), para não calar os reais.
        val doEspaco = avisados.filter { it.startsWith(PREFIXO_TESTE) == teste }.map { it.removePrefix(PREFIXO_TESTE) }.toSet()
        val prefixo = if (teste) "Teste · " else ""
        val dados = if (teste) PREFIXO_TESTE else ""

        val pendencias = Pendencias.de(e, agora)
        if (pendencias.isEmpty()) Notificador.limparAcoes(ctx)
        // Com o app aberto, a lista já está na tela: o aviso fica para depois, se continuar pendente.
        val novas = if (ctx.bolao.visivel) emptyList() else Pendencias.paraAvisar(pendencias, agora, doEspaco)
        Notificador.acaoNecessaria(ctx, novas, pendencias, prefixo)

        val premiadas = Pendencias.premiadas(e, agora, doEspaco)
        premiadas.forEach { Notificador.apostaPremiada(ctx, it, prefixo) }

        val chaves = novas.map { dados + it.chave } + premiadas.map { dados + it.chave }
        if (chaves.isNotEmpty()) {
            val todas = (avisados + chaves).takeLast(MAX_CHAVES)
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
        private const val MAX_CHAVES = 600

        fun agendar(context: Context) {
            val pedido = PeriodicWorkRequestBuilder<Vigia>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }

        /** Confere os sorteios e as pendências agora, sem esperar a próxima hora. */
        fun rodarAgora(context: Context) {
            if (android.os.Build.FINGERPRINT == "robolectric") return
            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOME_AGORA, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<Vigia>().build())
        }

        /**
         * Apuração automática de uma aposta já conferida: guarda a imagem dos acertos, registra
         * a apuração (o prêmio entra no saldo) e desenha o comprovante, sem abrir o WhatsApp.
         */
        suspend fun apurar(context: Context, id: String): Comprovante? {
            val repo = context.bolao.repo
            val a = repo.atual().apostas.firstOrNull { it.id == id } ?: return null
            val ap = a.apuracao ?: return null
            if (ap.confirmadaEm != null || ap.precisaConfirmar) return null
            val agora = System.currentTimeMillis()
            val hoje = LocalDate.now()
            val imagem = repo.guardarGerado("acertos", "acertos-${a.modalidade.sigla}-${a.concurso}.png", "image/png") { f ->
                ImagemConferencia.salvar(context, a, ap, f)
            }
            return registrarComprovante(context, { Operacoes.confirmarApuracao(it, id, ap, hoje, agora, imagem) }, enviar = false)
        }

        /** Formatação do app: esquece todos os avisos já dados e tira os que estão na tela. */
        fun esquecerTodosOsAvisos(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
            Notificador.limparAcoes(context)
        }

        /** Esquece os avisos já dados no modo de teste (um exemplo novo pode avisar de novo). */
        fun esquecerAvisosDeTeste(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val reais = prefs.getString(CHAVES, "").orEmpty().split('\n').filter { it.isNotBlank() && !it.startsWith(PREFIXO_TESTE) }
            prefs.edit().putString(CHAVES, reais.joinToString("\n")).apply()
        }
    }
}

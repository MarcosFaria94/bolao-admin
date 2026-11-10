package io.github.marcosfaria94.bolao.notificacoes

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Comprovante
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Pendencias
import io.github.marcosfaria94.bolao.data.Premiada
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.data.Sessoes
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.imagem.ImagemConferencia
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Roda de hora em hora e na hora marcada pelo [Despertador] (resultado do sorteio, apuração), mesmo
 * com o app fechado:
 * 0. atualiza os dados dos Jogos guardados no celular, quando a agenda dos sorteios pede (desde a 0.20);
 * 1. confere os jogos já sorteados na Caixa (e de novo, até o rateio oficial sair);
 * 2. faz a apuração sozinha quando não há nada a confirmar, na hora em que o resultado sai (no
 *    bolão, o envio do comprovante ao grupo vira pendência);
 * 3. mostra os dois avisos do celular: "Aposta premiada" e "Ação necessária" (no prêmio principal,
 *    toca o toque dos Ajustes, como uma chamada);
 * 4. marca o próximo despertar.
 * Desde a 0.21 faz isso em todas as sessões (cada bolão, o Individual e o modo de teste, se existir),
 * e não só na que está em uso: os avisos levam o nome da sessão (com mais de uma) e abrem nela. Os do
 * modo de teste levam "Teste" (e não tocam o toque do prêmio).
 */
class Vigia(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val app = ctx.bolao
        val agora = LocalDateTime.now()

        // 0. Os dados dos Jogos (desde a 0.20): só os jogos cujo sorteio já passou e o resultado não chegou.
        runCatching { app.indices.consultar() }

        val sessoes = app.sessoes.todasComDados().map { it.first }
        val estados = mutableListOf<Estado>()
        val premiadasPorSessao = mutableListOf<Pair<String, List<Premiada>>>()
        val permitido = Notificador.permitido(ctx)
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val avisados = prefs.getString(CHAVES, "").orEmpty().split('\n').filter { it.isNotBlank() }
        val novasChaves = mutableListOf<String>()
        val varias = app.sessoes.varias
        var algumaPendencia = false

        for (id in sessoes) {
            val repo = app.sessoes.repo(id)
            val teste = repo.emTeste
            var e = runCatching { vigiar(ctx, repo, agora) }.getOrElse { runCatching { repo.atual() }.getOrNull() } ?: continue
            // O k1, o k2 e as odds do concurso das apostas registradas (desde a 0.21).
            runCatching { e = repo.atualizar { Operacoes.guardarIndices(it, app.indices.itens.value) } }
            estados += e

            // 3. Avisos. Sem permissão, ficam para a próxima vez (não contam como avisados).
            if (!permitido) continue
            val marca = marcaDaSessao(id)
            val doEspaco = avisados.filter { daSessao(it, id) }.map { it.removePrefix(marca) }.toSet()
            val prefixo = when {
                teste -> "Teste · "
                varias -> "${app.sessoes.nome(id)} · "
                else -> ""
            }
            val pendencias = Pendencias.de(e, agora)
            if (pendencias.isNotEmpty()) algumaPendencia = true
            if (pendencias.isEmpty()) Notificador.limparAcoes(ctx, id)
            // Com o app aberto na sessão, a lista já está na tela: o aviso fica para depois, se continuar pendente.
            val naTela = app.visivel && app.sessoes.ativa.value == id
            val novas = if (naTela) emptyList() else Pendencias.paraAvisar(pendencias, agora, doEspaco)
            Notificador.acaoNecessaria(ctx, novas, pendencias, prefixo, id)
            val premiadas = Pendencias.premiadas(e, agora, doEspaco)
            premiadasPorSessao += id to premiadas
            novasChaves += novas.map { marca + it.chave } + premiadas.map { marca + it.chave }
        }

        // 4. O próximo despertar (resultado que ainda não saiu, apuração da manhã seguinte), de todas as sessões.
        Despertador.agendar(ctx, estados)
        if (!permitido) return Result.success()

        // Prêmio principal (fora do modo de teste): o toque dos Ajustes, como uma chamada.
        val tocar = premiadasPorSessao.any { (id, p) -> id != Sessoes.TESTE && p.any { it.principal } } && Toque.config(ctx).ligado
        for ((id, premiadas) in premiadasPorSessao) {
            val prefixo = when {
                id == Sessoes.TESTE -> "Teste · "
                varias -> "${app.sessoes.nome(id)} · "
                else -> ""
            }
            premiadas.forEach { Notificador.apostaPremiada(ctx, it, prefixo, comToque = tocar && id != Sessoes.TESTE && it.principal, sessao = id) }
        }
        if (!algumaPendencia) Notificador.limparAcoes(ctx)
        if (novasChaves.isNotEmpty()) {
            val todas = (avisados + novasChaves).takeLast(MAX_CHAVES)
            prefs.edit().putString(CHAVES, todas.joinToString("\n")).apply()
        }
        // Já marcado como avisado (não toca de novo); o trabalho segue até o fim do toque.
        if (tocar) Toque.tocarEEsperar(ctx, Toque.Origem.PREMIO)
        return Result.success()
    }

    /** Os passos 1 e 2 numa sessão: a conferência na Caixa e a apuração automática. Devolve o estado depois deles. */
    private suspend fun vigiar(ctx: Context, repo: Repositorio, agora: LocalDateTime): Estado {
        var e = repo.atual()
        // 0. O dia do sorteio das apostas registradas sem ele (os concursos seguintes de uma teimosinha, desde
        // a 0.22): o que a Caixa já divulgou e, se o concurso já foi sorteado, o dia dele (sem o dia, não há apuração).
        runCatching {
            val itens = ctx.bolao.indices.itens.value
            e = repo.atualizar { Operacoes.completarDatas(it, Operacoes.proximosConcursos(itens)) }
            val ultimos = itens.mapNotNull { (m, item) -> item.ultimo?.numero?.let { m to it } }.toMap()
            val datas = mutableMapOf<String, java.time.LocalDate>()
            for (a in Operacoes.semDataJaSorteadas(e, ultimos)) {
                runCatching { CaixaApi.concurso(a.modalidade, a.concurso).data }.getOrNull()?.let { datas[a.id] = it }
            }
            if (datas.isNotEmpty()) e = repo.atualizar { Operacoes.comDataDoSorteio(it, datas) }
        }
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
                apurar(ctx, a.id, repo)
            } catch (t: Throwable) {
                // Fica para a próxima vez (ou para o Administrador, depois de dois dias).
            }
        }
        return repo.atual()
    }

    companion object {
        private const val NOME = "vigia-do-bolao"
        private const val NOME_AGORA = "vigia-agora"
        private const val PREFS = "lembretes"
        private const val CHAVES = "avisados"
        private const val PREFIXO_TESTE = "teste:"

        /** As chaves dos avisos das sessões novas (desde a 0.21): "s:<id>:". A principal fica sem marca, como antes. */
        private const val PREFIXO_SESSAO = "s:"
        private const val MAX_CHAVES = 600

        /** A marca das chaves dos avisos da sessão [id]. */
        private fun marcaDaSessao(id: String): String = when (id) {
            Sessoes.PRINCIPAL -> ""
            Sessoes.TESTE -> PREFIXO_TESTE
            else -> "$PREFIXO_SESSAO$id:"
        }

        /** A chave [chave] (com a marca) é de um aviso da sessão [id]? */
        private fun daSessao(chave: String, id: String): Boolean = when (id) {
            Sessoes.PRINCIPAL -> !chave.startsWith(PREFIXO_TESTE) && !chave.startsWith(PREFIXO_SESSAO)
            else -> chave.startsWith(marcaDaSessao(id))
        }

        fun agendar(context: Context) {
            val pedido = PeriodicWorkRequestBuilder<Vigia>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NOME, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }

        /**
         * Confere os sorteios e as pendências agora, sem esperar a próxima hora. Do Android 12 em
         * diante vai como trabalho urgente (roda na hora, mesmo com o celular em repouso).
         */
        fun rodarAgora(context: Context) {
            val pedido = OneTimeWorkRequestBuilder<Vigia>().apply {
                if (Build.VERSION.SDK_INT >= 31) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }.build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOME_AGORA, ExistingWorkPolicy.REPLACE, pedido)
        }

        /**
         * Apuração automática de uma aposta já conferida: guarda a imagem dos acertos, registra
         * a apuração (o prêmio entra no saldo) e desenha o comprovante, sem abrir o WhatsApp.
         */
        suspend fun apurar(context: Context, id: String, repo: Repositorio = context.bolao.repo): Comprovante? {
            val a = repo.atual().apostas.firstOrNull { it.id == id } ?: return null
            val ap = a.apuracao ?: return null
            if (ap.confirmadaEm != null || ap.precisaConfirmar || a.pendenteDeAnexo) return null
            val agora = System.currentTimeMillis()
            val hoje = LocalDate.now()
            // Modo individual: sem comprovante e sem a imagem dos acertos; só o prêmio entra no saldo.
            if (repo.atual().individual) {
                repo.atualizar { Operacoes.confirmarApuracao(it, id, ap, hoje, agora) }
                return null
            }
            val imagem = repo.guardarGerado("acertos", "acertos-${a.modalidade.sigla}-${a.concurso}.png", "image/png") { f ->
                ImagemConferencia.salvar(context, a, ap, f)
            }
            return registrarComprovante(context, { Operacoes.confirmarApuracao(it, id, ap, hoje, agora, imagem) }, enviar = false, repo = repo)
        }

        /** Sessão apagada (extinção, formatação, sair do teste): esquece os avisos já dados nela e tira os que estão na tela. */
        fun esquecerAvisosDaSessao(context: Context, id: String) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val outros = prefs.getString(CHAVES, "").orEmpty().split('\n').filter { it.isNotBlank() && !daSessao(it, id) }
            prefs.edit().putString(CHAVES, outros.joinToString("\n")).apply()
            Notificador.limparAcoes(context, id)
        }

        /** Esquece os avisos já dados no modo de teste (um exemplo novo pode avisar de novo). */
        fun esquecerAvisosDeTeste(context: Context) = esquecerAvisosDaSessao(context, Sessoes.TESTE)
    }
}

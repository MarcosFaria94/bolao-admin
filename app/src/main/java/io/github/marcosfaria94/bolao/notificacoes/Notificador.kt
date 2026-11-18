package io.github.marcosfaria94.bolao.notificacoes

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.marcosfaria94.bolao.MainActivity
import io.github.marcosfaria94.bolao.R
import io.github.marcosfaria94.bolao.data.Pendencia
import io.github.marcosfaria94.bolao.data.Premiada
import io.github.marcosfaria94.bolao.data.Sessoes
import io.github.marcosfaria94.bolao.domain.Modalidade

/**
 * Os avisos que vão para o celular: as pendências do bolão, a aposta premiada (sem o valor: o prêmio é surpresa ao abrir
 * o app), os concursos especiais e os avisos dos jogos (acumulou, k1, prêmio). Todo o resto fica no resumo do Painel de
 * Jogos.
 *
 * Refeitos na 0.41 (pedido do usuário: "reformular os pushs, não gostei do atual"): o desenho grande da modalidade (o
 * trevo de corações na cor dela, [Emblema]) ou o do app; o título com o principal ("Mega-Sena acumulou", "Quina: k1
 * 0,850", "3 pendências") sem o nome da sessão na frente (ele vai no cabeçalho do aviso); o aviso aberto em linhas; o
 * dourado do app no lugar do azul-noite (que sumia no tema escuro); e um botão para os Detalhes quando é de uma
 * modalidade só.
 */
object Notificador {
    private const val CANAL_ACOES = "acoes"
    private const val CANAL_PREMIOS = "premios"

    /** Os avisos dos concursos especiais (desde a 0.34; um canal novo, com nome próprio nos ajustes do Android). */
    private const val CANAL_ESPECIAIS = "concursos_especiais"
    private const val ID_ESPECIAIS = 30_000
    private const val CANAL_JOGOS = "avisos_dos_jogos"
    private const val ID_JOGOS = 34_000
    const val EXTRA_DESTINO = "destino"

    /** A sessão do aviso (desde a 0.21): tocar abre o app nela. */
    const val EXTRA_SESSAO = "sessao"

    /** Um aviso só de "Ação necessária", atualizado com a lista de pendências. */
    private const val ID_ACOES = 1001

    /** O dourado do app (desde a 0.41; o azul-noite de antes não aparecia no tema escuro). */
    private const val COR = 0xFFF2B630.toInt()

    /** O "Ação necessária" do modo de teste e o começo dos números das outras sessões (desde a 0.21). */
    private const val ID_ACOES_TESTE = 1003
    private const val ID_ACOES_SESSOES = 20_000

    /** Código do "Parar o toque" (aviso do prêmio principal). */
    private const val ID_PARAR_TOQUE = 1002

    fun criarCanais(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        // Canais das versões anteriores (lembretes de aporte, jogo, resultado, especiais...).
        listOf("lembretes", "resultados").forEach { runCatching { nm.deleteNotificationChannel(it) } }
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ACOES, "Ação necessária", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Quando há alguma pendência para deixar o bolão em dia."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_PREMIOS, "Aposta premiada", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Quando uma aposta do bolão é premiada (o valor aparece ao abrir o app)."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ESPECIAIS, "Concursos especiais", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Mega da Virada, Quina de São João...: quando as apostas abrem e na véspera do sorteio."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_JOGOS, "Avisos dos jogos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Os avisos de Ajustes > Notificações: concurso que acumulou, k1 acima do escolhido, prêmio acima do escolhido."
            },
        )
    }

    /**
     * O começo de todos os avisos (desde a 0.41): o trevo pequeno, o dourado, o desenho grande da modalidade [m] (ou o do
     * app), o título, o texto e, com mais de uma sessão (ou no teste), o nome dela no cabeçalho ([prefixo] "Nome · ").
     */
    private fun aviso(context: Context, canal: String, titulo: String, texto: String, prefixo: String, m: Modalidade?): NotificationCompat.Builder =
        NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(COR)
            .setLargeIcon(runCatching { Emblema.de(m) }.getOrNull())
            .setContentTitle(titulo)
            .setContentText(texto)
            .setShowWhen(true)
            .setAutoCancel(true)
            .apply { prefixo.removeSuffix(" · ").takeIf { it.isNotBlank() }?.let { setSubText(it) } }

    /** O aviso aberto: as [linhas], uma embaixo da outra (até 6); sem elas, o texto inteiro. */
    private fun aberto(texto: String, linhas: List<String>, resumo: String? = null): NotificationCompat.Style =
        if (linhas.isEmpty()) {
            NotificationCompat.BigTextStyle().bigText(texto)
        } else {
            NotificationCompat.InboxStyle().also { i ->
                linhas.take(6).forEach { i.addLine(it) }
                (resumo ?: if (linhas.size > 6) "+${linhas.size - 6}" else null)?.let { i.setSummaryText(it) }
            }
        }

    /** O botão "Detalhes" de um aviso que abre a página de uma modalidade ("modalidade/X" → "detalhes/X"). */
    private fun comDetalhes(b: NotificationCompat.Builder, context: Context, destino: String, codigo: Int, sessao: String?) {
        val detalhes = when {
            destino.startsWith("modalidade/") -> "detalhes/" + destino.removePrefix("modalidade/")
            destino.startsWith("especial/") && destino.endsWith("?detalhes=false") -> destino.removeSuffix("?detalhes=false") + "?detalhes=true"
            else -> return
        }
        b.addAction(0, "Detalhes", abrir(context, detalhes, codigo + 1, sessao))
    }

    /**
     * Um aviso dos Parâmetros (desde a 0.38; desde a 0.41 nas Notificações): acumulou, k1 acima de x ou prêmio acima de
     * n. [prefixo]: o nome da sessão (com mais de uma). Tocar abre a página da modalidade (ou os Jogos, com mais de uma),
     * na sessão.
     */
    @SuppressLint("MissingPermission")
    fun avisoDosJogos(context: Context, a: io.github.marcosfaria94.bolao.domain.AvisosDosJogos.Aviso, prefixo: String, sessao: String) {
        if (!permitido(context)) return
        val id = ID_JOGOS + ((sessao + a.chaves.first()).hashCode() and 0xFFF)
        val b = aviso(context, CANAL_JOGOS, a.titulo, a.texto, prefixo, a.modalidade)
            .setStyle(aberto(a.texto, a.linhas))
            .setContentIntent(abrir(context, a.destino, id, sessao))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
        comDetalhes(b, context, a.destino, id, sessao)
        NotificationManagerCompat.from(context).notify(id, b.build())
    }

    /**
     * Um aviso de concurso especial (desde a 0.34): "Mega da Virada: apostas abertas" ou "...: o sorteio é amanhã".
     * Tocar abre a página do especial (ou a da modalidade).
     */
    @SuppressLint("MissingPermission")
    fun concursoEspecial(context: Context, a: io.github.marcosfaria94.bolao.domain.AvisosDeEspeciais.Aviso) {
        if (!permitido(context)) return
        val id = ID_ESPECIAIS + (a.chaves.last().hashCode() and 0xFFF)
        val b = aviso(context, CANAL_ESPECIAIS, a.titulo, a.texto, "", a.modalidade)
            .setStyle(aberto(a.texto, a.linhas))
            .setContentIntent(abrir(context, a.destino, id))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
        comDetalhes(b, context, a.destino, id, null)
        NotificationManagerCompat.from(context).notify(id, b.build())
    }

    fun permitido(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun abrir(context: Context, destino: String, codigo: Int, sessao: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_DESTINO, destino)
            .apply { if (sessao != null) putExtra(EXTRA_SESSAO, sessao) }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, codigo, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * O aviso "Ação necessária" de cada sessão (desde a 0.21): a principal com o número de sempre; as
     * outras, um número próprio.
     */
    private fun idDasAcoes(sessao: String): Int = when (sessao) {
        Sessoes.PRINCIPAL -> ID_ACOES
        Sessoes.TESTE -> ID_ACOES_TESTE
        else -> ID_ACOES_SESSOES + (sessao.hashCode() and 0xFFF)
    }

    /**
     * "Ação necessária" da sessão [sessao]: [novas] são as pendências que acabaram de ficar na hora do
     * aviso; [todas] entram na lista do aviso. Tocar abre o Painel de Jogos, na sessão.
     */
    @SuppressLint("MissingPermission")
    fun acaoNecessaria(context: Context, novas: List<Pendencia>, todas: List<Pendencia>, prefixo: String = "", sessao: String = Sessoes.PRINCIPAL) {
        if (!permitido(context) || novas.isEmpty()) return
        val principal = novas.first()
        // Desde a 0.41: com uma pendência, ela no título; com várias, "3 pendências" e a lista no aviso aberto.
        val uma = todas.size <= 1
        val titulo = if (uma) principal.titulo else tituloDasPendencias(todas.size)
        val texto = if (uma) principal.texto else todas.take(3).joinToString(" · ") { it.titulo } + if (todas.size > 3) " · …" else ""
        val modalidade = todas.mapNotNull { it.modalidade }.distinct().singleOrNull()?.takeIf { uma }
        val b = aviso(context, CANAL_ACOES, titulo, texto, prefixo, modalidade)
            .setStyle(aberto(texto, if (uma) emptyList() else todas.map { it.titulo }, "Toque para abrir o Painel de Jogos"))
            .setNumber(todas.size)
            .setContentIntent(abrir(context, "meus-jogos", idDasAcoes(sessao), sessao))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        NotificationManagerCompat.from(context).notify(idDasAcoes(sessao), b.build())
    }

    /** O título do aviso com várias pendências (desde a 0.41): "2 pendências". */
    fun tituloDasPendencias(n: Int): String = if (n == 1) "1 pendência" else "$n pendências"

    /**
     * Tira o aviso "Ação necessária" da sessão [sessao] (sem pendências, ou o Administrador já está
     * vendo a lista); sem a sessão, os de todas.
     */
    fun limparAcoes(context: Context, sessao: String? = null) {
        val nm = NotificationManagerCompat.from(context)
        if (sessao != null) {
            nm.cancel(idDasAcoes(sessao))
            return
        }
        runCatching {
            context.getSystemService(NotificationManager::class.java)?.activeNotifications
                ?.filter { it.notification.channelId == CANAL_ACOES && it.id != 7 }
                ?.forEach { nm.cancel(it.id) }
        }
        nm.cancel(ID_ACOES)
    }

    /**
     * "Aposta premiada!", sem o valor. Tocar abre a aposta, onde o prêmio aparece. [comToque]: o
     * prêmio principal está tocando (o aviso traz o "Parar o toque"; arrastar o aviso também para).
     * [quieto] (desde a 0.32): o aviso não toca o som dele (o som próprio do prêmio secundário toca no lugar).
     */
    @SuppressLint("MissingPermission")
    fun apostaPremiada(
        context: Context,
        p: Premiada,
        prefixo: String = "",
        comToque: Boolean = false,
        sessao: String? = null,
        quieto: Boolean = false,
    ) {
        if (!permitido(context)) return
        val id = (sessao.orEmpty() + p.chave).hashCode()
        val b = aviso(context, CANAL_PREMIOS, if (comToque) "Prêmio principal!" else p.titulo, p.texto, prefixo, p.aposta.modalidade)
            .setStyle(NotificationCompat.BigTextStyle().bigText(p.texto))
            .setContentIntent(abrir(context, p.destino, id, sessao))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(quieto)
        if (comToque) {
            val parar = PendingIntent.getBroadcast(
                context, ID_PARAR_TOQUE, Intent(context, PararToque::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            b.addAction(0, "Parar o toque", parar).setDeleteIntent(parar).setCategory(NotificationCompat.CATEGORY_ALARM)
        }
        NotificationManagerCompat.from(context).notify(id, b.build())
    }

    /** Aviso de teste (Ajustes), para conferir se o HyperOS está deixando passar. */
    @SuppressLint("MissingPermission")
    fun teste(context: Context) {
        if (!permitido(context)) return
        val texto = "É assim que chegam os avisos: as pendências do bolão, as apostas premiadas, os concursos especiais e os avisos dos jogos."
        val n = aviso(context, CANAL_ACOES, "Notificações funcionando", texto, "", null)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrir(context, "meus-jogos", 7))
            .build()
        NotificationManagerCompat.from(context).notify(7, n)
    }
}

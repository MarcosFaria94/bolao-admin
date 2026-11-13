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

/**
 * Os dois avisos que vão para o celular: "Ação necessária" (há pendência no bolão) e
 * "Aposta premiada" (sem o valor: o prêmio é surpresa ao abrir o app). Todo o resto fica
 * no resumo do Painel de Jogos (o ícone da barra de cima; até a 0.16, nas Notificações do app).
 */
object Notificador {
    private const val CANAL_ACOES = "acoes"
    private const val CANAL_PREMIOS = "premios"
    const val EXTRA_DESTINO = "destino"

    /** A sessão do aviso (desde a 0.21): tocar abre o app nela. */
    const val EXTRA_SESSAO = "sessao"

    /** Um aviso só de "Ação necessária", atualizado com a lista de pendências. */
    private const val ID_ACOES = 1001

    private const val COR = 0xFF13294B.toInt()

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
        val texto = if (todas.size <= 1) "${principal.titulo}: ${principal.texto}" else "${principal.titulo} e mais ${todas.size - 1} pendência(s)."
        val lista = NotificationCompat.InboxStyle()
        todas.take(6).forEach { lista.addLine(it.titulo) }
        if (todas.size > 6) lista.setSummaryText("+${todas.size - 6}")
        val n = NotificationCompat.Builder(context, CANAL_ACOES)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(COR)
            .setContentTitle(prefixo + "Ação necessária")
            .setContentText(texto)
            .setStyle(if (todas.size <= 1) NotificationCompat.BigTextStyle().bigText(texto) else lista)
            .setNumber(todas.size)
            .setContentIntent(abrir(context, "meus-jogos", idDasAcoes(sessao), sessao))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(idDasAcoes(sessao), n)
    }

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
        val b = NotificationCompat.Builder(context, CANAL_PREMIOS)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(COR)
            .setContentTitle(prefixo + if (comToque) "Prêmio principal!" else p.titulo)
            .setContentText(p.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(p.texto))
            .setContentIntent(abrir(context, p.destino, id, sessao))
            .setAutoCancel(true)
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
        val n = NotificationCompat.Builder(context, CANAL_ACOES)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(COR)
            .setContentTitle("Notificações funcionando")
            .setContentText("É assim que chegam os avisos de ação necessária e de aposta premiada.")
            .setContentIntent(abrir(context, "meus-jogos", 7))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(7, n)
    }
}

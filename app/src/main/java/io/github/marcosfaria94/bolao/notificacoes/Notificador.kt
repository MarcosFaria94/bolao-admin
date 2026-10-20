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

/**
 * Os dois avisos que vão para o celular: "Ação necessária" (há pendência no bolão) e
 * "Aposta premiada" (com o valor do prêmio). Todo o resto fica na aba Notificações do app.
 */
object Notificador {
    private const val CANAL_ACOES = "acoes"
    private const val CANAL_PREMIOS = "premios"
    const val EXTRA_DESTINO = "destino"

    /** Um aviso só de "Ação necessária", atualizado com a lista de pendências. */
    private const val ID_ACOES = 1001

    private const val COR = 0xFF13294B.toInt()

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
                description = "Quando uma aposta do bolão é premiada, com o valor do prêmio."
            },
        )
    }

    fun permitido(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun abrir(context: Context, destino: String, codigo: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_DESTINO, destino)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, codigo, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * "Ação necessária": [novas] são as pendências que acabaram de ficar na hora do aviso;
     * [todas] entram na lista do aviso. Tocar abre a aba Notificações.
     */
    @SuppressLint("MissingPermission")
    fun acaoNecessaria(context: Context, novas: List<Pendencia>, todas: List<Pendencia>, prefixo: String = "") {
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
            .setContentIntent(abrir(context, "notificacoes", ID_ACOES))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_ACOES, n)
    }

    /** Tira o aviso "Ação necessária" (sem pendências, ou o Administrador já está vendo a lista). */
    fun limparAcoes(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_ACOES)
    }

    /** "Aposta premiada!" com o valor do prêmio. Tocar abre a apuração da aposta. */
    @SuppressLint("MissingPermission")
    fun apostaPremiada(context: Context, p: Premiada, prefixo: String = "") {
        if (!permitido(context)) return
        val id = p.chave.hashCode()
        val n = NotificationCompat.Builder(context, CANAL_PREMIOS)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(COR)
            .setContentTitle(prefixo + p.titulo)
            .setContentText(p.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(p.texto))
            .setContentIntent(abrir(context, p.destino, id))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
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
            .setContentIntent(abrir(context, "notificacoes", 7))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(7, n)
    }
}

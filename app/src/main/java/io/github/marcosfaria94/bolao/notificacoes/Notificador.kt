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
import io.github.marcosfaria94.bolao.data.Lembrete

object Notificador {
    private const val CANAL_LEMBRETES = "lembretes"
    private const val CANAL_RESULTADOS = "resultados"
    const val EXTRA_DESTINO = "destino"

    fun criarCanais(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CANAL_LEMBRETES, "Lembretes do bolão", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Aporte, dia de aposta, comprovante pendente, concursos especiais e extrato."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_RESULTADOS, "Resultados dos jogos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso quando um jogo do bolão é sorteado, com o valor do prêmio."
            },
        )
    }

    fun permitido(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun mostrar(context: Context, l: Lembrete) {
        if (!permitido(context)) return
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_DESTINO, l.destino)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val abrir = PendingIntent.getActivity(
            context,
            l.chave.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canal = if (l.canal == Lembrete.Canal.RESULTADOS) CANAL_RESULTADOS else CANAL_LEMBRETES
        val n = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(0xFF0B6E4F.toInt())
            .setContentTitle(l.titulo)
            .setContentText(l.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(l.texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .setPriority(if (canal == CANAL_RESULTADOS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(l.chave.hashCode(), n)
    }

    /** Notificação de teste (Ajustes), para conferir se o HyperOS está deixando passar. */
    fun teste(context: Context) = mostrar(
        context,
        Lembrete(
            "teste-${System.currentTimeMillis()}", Lembrete.Canal.RESULTADOS,
            "Notificações funcionando",
            "É assim que chegam os avisos de aporte, dia de aposta, sorteios e extrato.",
            "painel",
        ),
    )
}

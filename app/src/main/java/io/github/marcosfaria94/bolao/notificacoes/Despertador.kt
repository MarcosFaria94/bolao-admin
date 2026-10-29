package io.github.marcosfaria94.bolao.notificacoes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Pendencias
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Despertador do Vigia. O WorkManager sozinho (de hora em hora) atrasa muito, ou nem roda, em
 * celulares que economizam bateria (HyperOS/MIUI): a apuração e o aviso do prêmio ficavam para
 * quando o app fosse aberto. O despertador acorda o app na hora em que o resultado deve sair, de
 * meia em meia hora enquanto ele não sai e na hora da apuração automática ([Pendencias.proximoDespertar]).
 * Um despertador só: cada vez que o Vigia roda (ou os dados mudam), ele é remarcado.
 */
object Despertador {
    private const val ACAO = "io.github.marcosfaria94.bolao.DESPERTAR"
    private const val CODIGO = 4242

    private fun pedido(context: Context, criar: Boolean): PendingIntent? {
        val intent = Intent(context, Despertado::class.java).setAction(ACAO)
        val flags = PendingIntent.FLAG_IMMUTABLE or if (criar) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, CODIGO, intent, flags)
    }

    /** Marca (ou desmarca) o próximo despertar a partir dos dados [e]. */
    fun agendar(context: Context, e: Estado, agora: LocalDateTime = LocalDateTime.now(), fuso: ZoneId = ZoneId.systemDefault()) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val quando = Pendencias.proximoDespertar(e, agora)
        if (quando == null) {
            pedido(context, criar = false)?.let { am.cancel(it) }
            return
        }
        val ms = maxOf(quando.atZone(fuso).toInstant().toEpochMilli(), System.currentTimeMillis() + 60_000)
        val pi = pedido(context, criar = true) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            }
        }.onFailure {
            // Sem permissão para a hora exata: vale a hora aproximada.
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi) }
        }
    }
}

/**
 * Recebe o despertador, a volta do celular depois de ligado e a atualização do app: confere os
 * sorteios agora (o Vigia remarca o próximo despertar no fim).
 */
class Despertado : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Vigia.agendar(context)
        }
        Vigia.rodarAgora(context)
    }
}

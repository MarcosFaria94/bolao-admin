package io.github.marcosfaria94.bolao.notificacoes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.marcosfaria94.bolao.BolaoApp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Pendencias
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Despertador do Vigia. O WorkManager sozinho (de hora em hora) atrasa muito, ou nem roda, em
 * celulares que economizam bateria (HyperOS/MIUI): a apuração e o aviso do prêmio ficavam para
 * quando o app fosse aberto. O despertador acorda o app 5 minutos depois do sorteio, de 5 em 5 minutos
 * enquanto o resultado (ou a apuração) não sai e para quando consegue ([Pendencias.proximoDespertar]).
 * Desde a 0.25 acorda também para os dados dos Jogos ([Indices.proximaConsulta]), mesmo sem apostas.
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
    fun agendar(context: Context, e: Estado, agora: LocalDateTime = LocalDateTime.now(), fuso: ZoneId = ZoneId.systemDefault()) =
        agendar(context, listOf(e), agora, fuso)

    /** O próximo despertar, lendo os dados de todas as sessões. */
    suspend fun agendarTodas(context: Context) {
        val estados = (context.applicationContext as BolaoApp).sessoes.todasComDados().map { it.second }
        agendar(context, estados)
    }

    /** O próximo despertar entre todas as sessões ([estados]; desde a 0.21 o Vigia cuida de todas). */
    fun agendar(context: Context, estados: List<Estado>, agora: LocalDateTime = LocalDateTime.now(), fuso: ZoneId = ZoneId.systemDefault()) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val dasApostas = estados.mapNotNull { Pendencias.proximoDespertar(it, agora) }.minOrNull()?.atZone(fuso)?.toInstant()
        val dosJogos = runCatching { (context.applicationContext as BolaoApp).indices.proximaConsulta(agora.atZone(fuso).toInstant()) }.getOrNull()
        val quando = listOfNotNull(dasApostas, dosJogos).minOrNull()
        if (quando == null) {
            pedido(context, criar = false)?.let { am.cancel(it) }
            return
        }
        val ms = maxOf(quando.toEpochMilli(), System.currentTimeMillis() + 60_000)
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

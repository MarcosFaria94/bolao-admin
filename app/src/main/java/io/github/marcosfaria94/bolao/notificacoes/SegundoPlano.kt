package io.github.marcosfaria94.bolao.notificacoes

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * O que o celular precisa liberar para a apuração e o aviso do prêmio funcionarem com o app
 * fechado: a economia de bateria sem restrições e, nos Xiaomi (HyperOS/MIUI), a inicialização
 * automática (sem ela, o sistema não deixa o app acordar sozinho).
 */
object SegundoPlano {

    /** Xiaomi, Redmi e Poco: têm a "Inicialização automática" do HyperOS/MIUI. */
    val xiaomi: Boolean
        get() = listOf(Build.MANUFACTURER, Build.BRAND).any { m ->
            m.equals("xiaomi", ignoreCase = true) || m.equals("redmi", ignoreCase = true) || m.equals("poco", ignoreCase = true)
        }

    /** A economia de bateria não pausa o app. */
    fun bateriaLivre(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    private fun abrir(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

    /** Pede ao sistema "deixar o app sempre rodar em segundo plano" (uma pergunta do próprio Android). */
    @SuppressLint("BatteryLife")
    fun pedirBateriaLivre(context: Context) {
        val pedido = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        if (!abrir(context, pedido)) abrirAjustesDoApp(context)
    }

    /** A lista da "Inicialização automática" do HyperOS/MIUI (nos outros celulares, os ajustes do app). */
    fun abrirInicioAutomatico(context: Context) {
        val telas = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.miui.securitycenter", "com.miui.powercenter.autostart.AutoStartManagementActivity"),
        )
        if (xiaomi && telas.any { abrir(context, Intent().setComponent(it)) }) return
        abrirAjustesDoApp(context)
    }

    /** Os ajustes do app no Android (bateria, notificações, permissões). */
    fun abrirAjustesDoApp(context: Context) {
        abrir(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
}

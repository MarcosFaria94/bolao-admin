package io.github.marcosfaria94.bolao.notificacoes

import android.content.Context
import io.github.marcosfaria94.bolao.domain.AvisosDeEspeciais

/**
 * O ajuste dos avisos dos concursos especiais (desde a 0.34, Ajustes > Notificações): ligado por padrão (pedido do
 * usuário). Vale para o app todo (os especiais são da Caixa, não de uma sessão). As chaves já avisadas
 * ([AvisosDeEspeciais.chaveDasApostas], [AvisosDeEspeciais.chaveDaVespera]) ficam guardadas para não repetir.
 */
object AvisoDeEspeciais {
    private const val PREFS = "notificacoes"
    private const val LIGADO = "especiais"
    private const val AVISADOS = "especiais_avisados"

    /** Quantas chaves guardar (as mais novas). */
    private const val MAXIMO = 80

    const val LIGADO_PADRAO = true

    fun ligado(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(LIGADO, LIGADO_PADRAO)

    fun ligar(ctx: Context, ligado: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(LIGADO, ligado).apply()
    }

    fun avisados(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(AVISADOS, "").orEmpty().split('\n').filter { it.isNotBlank() }.toSet()

    fun marcar(ctx: Context, chaves: Collection<String>) {
        if (chaves.isEmpty()) return
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val todas = (p.getString(AVISADOS, "").orEmpty().split('\n').filter { it.isNotBlank() } + chaves).distinct().takeLast(MAXIMO)
        p.edit().putString(AVISADOS, todas.joinToString("\n")).apply()
    }
}

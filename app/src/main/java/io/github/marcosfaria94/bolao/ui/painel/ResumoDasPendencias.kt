package io.github.marcosfaria94.bolao.ui.painel

import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Pendencia
import io.github.marcosfaria94.bolao.data.Pendencias

/** Os tipos de pendência do resumo do Painel de Jogos, na ordem em que aparecem. */
enum class TipoDoResumo(val rotulo: String) {
    /** Desde a 0.30, "Apostas não registradas" (até a 0.29, "Falta registrar"). */
    REGISTRAR("Apostas não registradas"),
    /** Desde a 0.36: as apostas registradas que falta confirmar (até a 0.35 contavam em "Sem comprovante"). */
    CONFIRMAR("A confirmar"),
    /** Desde a 0.36, só os rateios esperando os Pix. */
    SEM_COMPROVANTE("Sem comprovante"),
    CONFERIR("A conferir"),
    ENVIAR("Enviar ao grupo"),
    APURAR("Apuração"),
    APOSTA_DO_DIA("Aposta de hoje"),
    APORTE("Aporte mensal"),
    EXTRATO("Extrato"),
    INDICES("Índices"),
}

/** Uma linha do resumo: o tipo, quantas são e a tela que resolve (a primeira, quando são várias). */
data class ItemDoResumo(val tipo: TipoDoResumo, val quantidade: Int, val destino: String)

/**
 * O resumo das pendências (o alto do Painel de Jogos, desde que as Notificações saíram, na 0.17): cada
 * tipo com a quantidade, só os que têm alguma. Os comprovantes que faltam e os que esperam a
 * conferência abrem os Comprovantes; os envios, o comprovante (um só) ou os Comprovantes (vários); o
 * resto, a tela da primeira pendência daquele tipo (a mais antiga).
 */
fun resumoDasPendencias(e: Estado, pendencias: List<Pendencia>): List<ItemDoResumo> {
    val porTipo = pendencias.groupBy { it.tipo }
    fun primeira(t: Pendencia.Tipo): String? = porTipo[t]?.minByOrNull { it.desde }?.destino
    fun quantas(t: Pendencia.Tipo): Int = porTipo[t]?.size ?: 0
    val comprovantes = Pendencias.ABA_COMPROVANTES
    val aConfirmar = if (e.individual) 0 else Operacoes.apostasSemComprovante(e).size
    val semComprovante = if (e.individual) 0 else Operacoes.rateiosSemPix(e).size
    val conferir = if (e.individual) 0 else Operacoes.apostasParaConferir(e).size + Operacoes.rateiosParaConferir(e).size
    val envios = porTipo[Pendencia.Tipo.ENVIO].orEmpty()
    return listOfNotNull(
        primeira(Pendencia.Tipo.RASCUNHO)?.let { ItemDoResumo(TipoDoResumo.REGISTRAR, quantas(Pendencia.Tipo.RASCUNHO), it) },
        ItemDoResumo(TipoDoResumo.CONFIRMAR, aConfirmar, comprovantes).takeIf { aConfirmar > 0 },
        ItemDoResumo(TipoDoResumo.SEM_COMPROVANTE, semComprovante, comprovantes).takeIf { semComprovante > 0 },
        ItemDoResumo(TipoDoResumo.CONFERIR, conferir, comprovantes).takeIf { conferir > 0 },
        envios.takeIf { it.isNotEmpty() }?.let { ItemDoResumo(TipoDoResumo.ENVIAR, it.size, if (it.size == 1) it.single().destino else comprovantes) },
        primeira(Pendencia.Tipo.APURACAO)?.let { ItemDoResumo(TipoDoResumo.APURAR, quantas(Pendencia.Tipo.APURACAO), it) },
        primeira(Pendencia.Tipo.APOSTA_DO_DIA)?.let { ItemDoResumo(TipoDoResumo.APOSTA_DO_DIA, quantas(Pendencia.Tipo.APOSTA_DO_DIA), it) },
        primeira(Pendencia.Tipo.APORTE)?.let { ItemDoResumo(TipoDoResumo.APORTE, quantas(Pendencia.Tipo.APORTE), it) },
        primeira(Pendencia.Tipo.EXTRATO)?.let { ItemDoResumo(TipoDoResumo.EXTRATO, quantas(Pendencia.Tipo.EXTRATO), it) },
        primeira(Pendencia.Tipo.INDICES)?.let { ItemDoResumo(TipoDoResumo.INDICES, quantas(Pendencia.Tipo.INDICES), it) },
    )
}

/** O número do botão do Painel de Jogos (na barra de cima): a soma das pendências do resumo. */
fun quantasPendencias(resumo: List<ItemDoResumo>): Int = resumo.sumOf { it.quantidade }

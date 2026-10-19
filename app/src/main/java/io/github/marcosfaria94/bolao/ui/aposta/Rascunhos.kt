package io.github.marcosfaria94.bolao.ui.aposta

import android.content.Context
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.novoId
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import java.time.LocalDate

/**
 * Abre a aposta em andamento da modalidade ou começa uma nova (números aleatórios, preço de
 * Ajustes). Se os índices já têm o próximo concurso (de hoje em diante), ele já vem preenchido.
 * Devolve o id da aposta.
 */
suspend fun abrirOuCriarAposta(context: Context, estado: Estado, m: Modalidade): String {
    Operacoes.rascunhos(estado).firstOrNull { it.modalidade == m }?.let { return it.id }
    val hoje = LocalDate.now()
    val u = context.bolao.indices.itens.value[m]?.ultimo
    // Na Loteca, o editor busca na Caixa também os jogos do concurso.
    val atual = m != Modalidade.LOTECA && u?.proximaData?.let { !it.isBefore(hoje) } == true
    val p = Volante.sortear(m)
    val a = Aposta(
        id = novoId(),
        numero = 0,
        modalidade = m,
        concurso = if (atual) u?.proximoNumero ?: 0 else 0,
        dataAposta = hoje,
        dataSorteio = if (atual) u?.proximaData else null,
        premioEstimado = if (atual) u?.premioEstimadoProximo else null,
        valor = estado.config.preco(m),
        numeros = p.numeros,
        trevos = p.trevos,
        extra = p.extra,
        palpitesLoteca = p.loteca,
        fracoesFederal = if (m == Modalidade.FEDERAL) 10 else null,
        criadaEm = System.currentTimeMillis(),
    )
    context.bolao.repo.atualizar { Operacoes.salvarRascunho(it, a) }
    return a.id
}

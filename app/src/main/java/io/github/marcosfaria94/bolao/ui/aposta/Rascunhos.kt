package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.data.novoId
import io.github.marcosfaria94.bolao.data.palpite
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A aposta em andamento da modalidade (a mais recente), que o volante da página do jogo abre.
 * Sem nenhuma, o volante começa vazio, numa aposta nova.
 */
fun rascunhoDa(estado: Estado, m: Modalidade): Aposta? = Operacoes.rascunhos(estado).firstOrNull { it.modalidade == m }

/**
 * Aposta nova da modalidade, com o volante vazio e o preço de Ajustes. Se o [ultimo] concurso
 * (dos índices) já diz qual é o próximo (de hoje em diante), ele já vem preenchido; na Loteca o
 * editor busca na Caixa também os jogos do concurso.
 */
fun novaAposta(estado: Estado, m: Modalidade, ultimo: Concurso?, hoje: LocalDate, agora: Long): Aposta {
    val atual = m != Modalidade.LOTECA && ultimo?.proximaData?.let { !it.isBefore(hoje) } == true
    return Aposta(
        id = novoId(),
        numero = 0,
        modalidade = m,
        concurso = if (atual) ultimo?.proximoNumero ?: 0 else 0,
        dataAposta = hoje,
        dataSorteio = if (atual) ultimo?.proximaData else null,
        premioEstimado = if (atual) ultimo?.premioEstimadoProximo else null,
        valor = estado.config.preco(m),
        // Super Sete: as 7 colunas sem número (-1), como depois do "Limpar".
        numeros = if (m.formato == Formato.COLUNAS) List(Volante.COLUNAS_SUPER_SETE) { -1 } else emptyList(),
        fracoesFederal = if (m == Modalidade.FEDERAL) 10 else null,
        criadaEm = agora,
    )
}

/** O volante está completo: todas as dezenas (e os trevos, o time ou o mês); na Federal, o bilhete e as frações. */
fun volanteCompleto(a: Aposta): Boolean =
    if (a.modalidade.formato == Formato.FEDERAL) !a.bilheteFederal.isNullOrBlank() && (a.fracoesFederal ?: 0) in 1..10
    else Volante.pendencias(a.modalidade, a.palpite).isEmpty()

/**
 * Regra do rascunho: ao sair da tela, a aposta só fica em "Em andamento" com o volante completo
 * ou com o comprovante da Caixa anexado. Faltando dezena e sem o comprovante, é descartada.
 */
fun ficaEmAndamento(a: Aposta): Boolean = a.comprovante != null || volanteCompleto(a)

/** Já há alguma escolha no volante (descartar uma aposta assim merece um aviso). */
fun algoEscolhido(a: Aposta): Boolean = when (a.modalidade.formato) {
    Formato.NUMEROS -> a.numeros.isNotEmpty() || a.trevos.isNotEmpty()
    Formato.COLUNAS -> a.numeros.any { it >= 0 }
    Formato.LOTECA -> a.palpitesLoteca.any { it.isNotBlank() }
    Formato.FEDERAL -> !a.bilheteFederal.isNullOrBlank()
}

/** Como fica a aposta ao sair da tela (e o aviso para o Administrador, quando há). */
sealed interface Saida {
    /** Registrada, ou já descartada pelo botão: nada a fazer. */
    data object Nada : Saida

    /** Fica (ou continua) em "Em andamento". */
    data class EmAndamento(val aviso: String?) : Saida

    /** Descartada (sai de "Em andamento", se estava lá). */
    data class Descartada(val aviso: String?) : Saida
}

/**
 * O que acontece com a aposta [a] ao sair da tela. [gravada] = como ela está no app agora (null =
 * ainda não gravada); [nova] = a tela foi aberta como aposta nova.
 */
fun saidaDaTela(a: Aposta, gravada: Aposta?, nova: Boolean): Saida = when {
    gravada?.registrada == true -> Saida.Nada
    ficaEmAndamento(a) -> Saida.EmAndamento(
        if (!nova) null
        else "Aposta em andamento: " + when {
            a.comprovante == null -> "falta anexar o comprovante da Caixa."
            volanteCompleto(a) -> "pronta para registrar."
            else -> "falta completar o volante."
        },
    )
    else -> Saida.Descartada(
        if (gravada != null || algoEscolhido(a)) {
            "Aposta descartada: " + (if (a.modalidade.formato == Formato.FEDERAL) "o bilhete" else "o volante") +
                " ficou incompleto e sem o comprovante da Caixa."
        } else {
            null
        },
    )
}

/**
 * A aposta do volante da página do jogo (nova ou em andamento). Enquanto a página está aberta (e
 * também com os Detalhes abertos por cima), a aposta fica aqui
 * (e sobrevive a girar o celular); cada mudança que a deixa valendo (volante completo ou
 * comprovante anexado) já é gravada em "Em andamento". Ao sair da tela vale a regra
 * ([saidaDaTela]): fica em andamento com o volante completo ou o comprovante; senão, é descartada.
 */
class ApostaEmEdicao(
    private val repo: Repositorio,
    private val escopo: CoroutineScope,
    inicial: Aposta,
    private val nova: Boolean,
) : ViewModel() {
    var aposta by mutableStateOf(inicial)
        private set

    /** Descartada pelo botão "Descartar rascunho" (ou em outra tela): ao sair, nada mais a fazer. */
    private var descartada = false

    /** As versões que esta página abriu ou gravou: outra versão gravada veio de outra tela. */
    private val conhecidas = mutableListOf(inicial)

    /** Já foi vista gravada em "Em andamento" (aberta de lá, ou gravada por esta página). */
    private var vistaGravada = !nova

    fun mudar(novo: Aposta) {
        aposta = novo
        if (ficaEmAndamento(novo)) {
            conhecidas += novo
            if (conhecidas.size > 50) conhecidas.removeAt(1)
            repo.enfileirar { e -> if (e.apostas.any { it.id == novo.id && it.registrada }) e else Operacoes.salvarRascunho(e, novo) }
        }
    }

    /**
     * A mesma aposta pode ser aberta por cima desta página (pelo aviso do celular, por exemplo) e
     * mudada lá. Quando a página volta, ela confere a aposta gravada ([gravada]): uma versão que não
     * veio daqui passa a ser a do volante (para não desfazer, ao sair, o que foi feito na outra tela).
     * Devolve true se a aposta foi descartada na outra tela: a página não tem mais o que mostrar.
     */
    fun conferirGravada(gravada: Aposta?): Boolean {
        // Descartada aqui mesmo (o botão já fecha a página) ou já avisada: nada a fazer.
        if (descartada) return false
        when {
            gravada == null -> if (vistaGravada) {
                descartada = true
                return true
            }
            gravada.registrada -> Unit
            else -> {
                vistaGravada = true
                if (conhecidas.none { it == gravada }) {
                    aposta = gravada
                    conhecidas += gravada
                }
            }
        }
        return false
    }

    /** "Descartar rascunho": apaga a aposta e o comprovante anexado (a não ser que já tenha sido registrada). */
    fun descartar() {
        descartada = true
        val a = aposta
        repo.enfileirar { e -> Operacoes.excluirRascunho(e, a.id) }
        a.comprovante?.let { repo.apagarSeSolto(it) }
    }

    override fun onCleared() {
        if (descartada) return
        val a = aposta
        escopo.launch {
            runCatching {
                var aviso: String? = null
                repo.atualizar { e ->
                    when (val s = saidaDaTela(a, e.apostas.firstOrNull { it.id == a.id }, nova)) {
                        Saida.Nada -> e
                        is Saida.EmAndamento -> {
                            aviso = s.aviso
                            Operacoes.salvarRascunho(e, a)
                        }
                        is Saida.Descartada -> {
                            aviso = s.aviso
                            Operacoes.excluirRascunho(e, a.id)
                        }
                    }
                }
                // Sem o comprovante não há arquivo a apagar; um comprovante trocado já foi apagado na troca.
                aviso?.let { repo.avisar(it) }
            }
        }
    }
}

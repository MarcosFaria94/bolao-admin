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
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import java.time.LocalDate

/**
 * Para onde vai o "Apostar" de uma modalidade: a aposta em andamento dela ("aposta/{id}")
 * ou uma aposta nova ("nova/{modalidade}"), que só é gravada quando confirmada.
 */
fun rotaDaAposta(estado: Estado, m: Modalidade): String =
    Operacoes.rascunhos(estado).firstOrNull { it.modalidade == m }?.let { "aposta/${it.id}" } ?: "nova/${m.name}"

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

/**
 * Aposta nova ainda não confirmada: só existe nesta tela (e sobrevive a girar o celular).
 * Confirmada, ela vai para "Em andamento" e cada mudança passa a ser gravada. Se a tela fecha
 * sem a confirmação, a aposta é descartada, junto com o comprovante da Caixa que tiver sido
 * anexado (a não ser que a aposta tenha sido registrada).
 */
class ApostaNovaModelo(private val repo: Repositorio, inicial: Aposta) : ViewModel() {
    var aposta by mutableStateOf(inicial)
    var confirmada by mutableStateOf(false)

    override fun onCleared() {
        if (!confirmada) aposta.comprovante?.let { repo.apagarSeSolto(it) }
    }
}

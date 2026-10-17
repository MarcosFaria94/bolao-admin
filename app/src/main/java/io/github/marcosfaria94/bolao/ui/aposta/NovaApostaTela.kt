package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.novoId
import io.github.marcosfaria94.bolao.domain.Calendario
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.TituloSecao
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun NovaApostaTela(estado: Estado, abrirAposta: (String) -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { ctx.bolao.repo }
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val eventual = Operacoes.ehEventual(estado, hoje)
    val rascunhos = Operacoes.rascunhos(estado)

    fun criar(m: Modalidade) {
        escopo.launch {
            try {
                val p = Volante.sortear(m)
                val a = Aposta(
                    id = novoId(),
                    numero = 0,
                    modalidade = m,
                    concurso = 0,
                    dataAposta = hoje,
                    valor = estado.config.preco(m),
                    numeros = p.numeros,
                    trevos = p.trevos,
                    extra = p.extra,
                    palpitesLoteca = p.loteca,
                    fracoesFederal = if (m == Modalidade.FEDERAL) 10 else null,
                    criadaEm = System.currentTimeMillis(),
                )
                repo.atualizar { Operacoes.salvarRascunho(it, a) }
                abrirAposta(a.id)
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível começar a aposta: ${t.message}")
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Saldo: ${Dinheiro.rs(Operacoes.saldo(estado))}", style = MaterialTheme.typography.titleMedium)
                val quando = "${Datas.diaSemana(hoje)}, ${Datas.diaMes(hoje)}"
                when {
                    !Calendario.ehDiaProgramado(hoje) -> Aviso(
                        "Hoje ($quando) não é dia programado: esta será uma APOSTA EVENTUAL, que depende de saldo extraordinário. " +
                            "Próximo dia programado: ${Datas.diaMes(Calendario.proximoDiaProgramado(hoje))}.",
                        alerta = true,
                    )
                    eventual -> Aviso("A aposta de hoje já foi feita: uma nova aposta será EVENTUAL.", alerta = true)
                    else -> Aviso("Aposta do dia programado $quando.")
                }
            }
        }
        if (rascunhos.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { TituloSecao("Em andamento") }
            items(rascunhos, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { r ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().clickable { abrirAposta(r.id) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SeloModalidade(r.modalidade, tamanho = 36)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${r.modalidade.nome}${if (r.concurso > 0) " ${r.concurso}" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                if (r.comprovante == null) "Falta anexar o comprovante" else "Pronta para registrar",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text("Continuar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { TituloSecao("Escolha a modalidade") }
        items(Modalidade.entries, key = { it.name }) { m ->
            val guardado = CaixaApi.ultimoGuardado(m)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth().clickable { criar(m) },
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeloModalidade(m, tamanho = 36)
                        Spacer(Modifier.width(10.dp))
                        Text(m.nome, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (m.precoVariavel) "Preço variável" else Dinheiro.rs(estado.config.preco(m)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    guardado?.premioEstimadoProximo?.let {
                        Text(
                            "Prêmio estimado ${Dinheiro.abreviar(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

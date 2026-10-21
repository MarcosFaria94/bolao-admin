package io.github.marcosfaria94.bolao.ui.aposta

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.PaddingTela
import io.github.marcosfaria94.bolao.ui.comum.SeloModalidade
import io.github.marcosfaria94.bolao.ui.comum.TituloSecao
import io.github.marcosfaria94.bolao.ui.jogos.CartaoIndice
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Saldo, provisionado e extra (topo da Nova aposta e do Financeiro). */
@Composable
fun ResumoSaldo(estado: Estado, hoje: LocalDate) {
    val saldo = Operacoes.saldo(estado)
    val provisionado = Operacoes.provisao(estado, hoje).provisionado
    val extra = saldo - provisionado
    Column {
        Text("Saldo líquido · ${Datas.relativa(hoje, LocalDate.now())}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Dinheiro.rs(saldo), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Provisionado: ${Dinheiro.rs(provisionado)}", style = MaterialTheme.typography.bodyMedium)
        Text("Extra: " + (if (extra < 0) "- " else "") + Dinheiro.rs(kotlin.math.abs(extra)), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun NovaApostaTela(estado: Estado, abrirAposta: (String) -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val indices = remember { ctx.bolao.indices }
    val itens by indices.itens.collectAsStateWithLifecycle()
    val atualizando by indices.atualizando.collectAsStateWithLifecycle()
    val falhou by indices.falhou.collectAsStateWithLifecycle()
    val rascunhos = Operacoes.rascunhos(estado)
    LaunchedEffect(Unit) { indices.atualizar() }

    fun abrir(m: Modalidade) {
        escopo.launch {
            try {
                abrirAposta(abrirOuCriarAposta(ctx, estado, m))
            } catch (t: Throwable) {
                avisos.showSnackbar("Não foi possível começar a aposta: ${t.message}")
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingTela,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { ResumoSaldo(estado, hoje) }
        if (rascunhos.isNotEmpty()) {
            item { TituloSecao("Em andamento") }
            items(rascunhos, key = { it.id }) { r ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth().clickable { abrirAposta(r.id) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SeloModalidade(r.modalidade, tamanho = 36)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${r.modalidade.nome}${if (r.concurso > 0) " · ${r.concurso}" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                if (r.comprovante == null) "Sem o comprovante da Caixa" else "Pronta para registrar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("Continuar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TituloSecao("Escolha a modalidade", Modifier.weight(1f))
                if (atualizando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
        if (falhou && itens.values.all { it.ultimo == null }) {
            item {
                Text(
                    "A Caixa não respondeu agora: a ordem pelo k1 aparece quando houver conexão.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        items(indices.ordenados(itens), key = { it.modalidade.name }) { item ->
            CartaoIndice(item, estado, aoTocar = { abrir(item.modalidade) })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

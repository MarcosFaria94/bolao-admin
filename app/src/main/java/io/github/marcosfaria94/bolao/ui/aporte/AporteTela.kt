package io.github.marcosfaria94.bolao.ui.aporte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.TipoComprovante
import io.github.marcosfaria94.bolao.data.TipoLancamento
import io.github.marcosfaria94.bolao.domain.Datas
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Mensagens
import io.github.marcosfaria94.bolao.imagem.Recibos
import io.github.marcosfaria94.bolao.ui.comum.AVISO_DEFINITIVO
import io.github.marcosfaria94.bolao.ui.comum.Aviso
import io.github.marcosfaria94.bolao.ui.comum.BotoesDeRegistro
import io.github.marcosfaria94.bolao.ui.comum.CampoData
import io.github.marcosfaria94.bolao.ui.comum.CampoDinheiro
import io.github.marcosfaria94.bolao.ui.comum.Compartilhar
import io.github.marcosfaria94.bolao.ui.comum.DialogoConfirmar
import io.github.marcosfaria94.bolao.ui.comum.Historico
import io.github.marcosfaria94.bolao.ui.comum.LocalAvisos
import io.github.marcosfaria94.bolao.ui.comum.VisualizadorComprovante
import io.github.marcosfaria94.bolao.ui.comum.registrarComprovante
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Aporte. Enquanto o aporte mensal do mês não é registrado, a tela trata dele: valor fixo
 * (o de Ajustes), data fixa no dia 1º, "Enviar lembrete" e "Registrar" (sem mensagem).
 * Depois disso, a tela registra aportes adicionais (data e valor livres) e os envia ao grupo.
 */
@Composable
fun AporteTela(estado: Estado, abrir: (String) -> Unit) {
    val ctx = LocalContext.current
    val avisos = LocalAvisos.current
    val escopo = rememberCoroutineScope()
    val hoje = LocalDate.now()
    val mensal = Operacoes.aporteMensalPendente(estado, hoje)

    var data by remember { mutableStateOf(hoje) }
    var valor by remember { mutableLongStateOf(0L) }
    var confirmar by remember { mutableStateOf(false) }
    var vendo by remember { mutableStateOf(false) }
    var trabalhando by remember { mutableStateOf(false) }

    val dataAporte = if (mensal) YearMonth.from(hoje).atDay(1) else data
    val valorAporte = if (mensal) Operacoes.valorAporteMensal(estado) else valor
    val texto = Operacoes.textoAporte(estado, dataAporte, valorAporte, adicional = !mensal)
    val mesFechado = Operacoes.mesFechado(estado, YearMonth.from(dataAporte))
    val problema = when {
        mensal -> null
        valor <= 0 -> "Informe o valor do aporte."
        data.isAfter(hoje) -> "A data do aporte não pode ser no futuro."
        mesFechado -> "O extrato de ${Datas.mesAno(YearMonth.from(data))} já foi registrado: escolha uma data depois dele."
        else -> null
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (mensal) {
            Text(
                "Aporte mensal · ${Datas.mesAnoTitulo(YearMonth.from(hoje))}",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = Datas.longa(dataAporte),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Data do aporte") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = Dinheiro.rs(valorAporte),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Valor total do aporte") },
                supportingText = {
                    Text("${Dinheiro.rs(estado.config.aporteMensal)} por integrante (valor padrão em Ajustes).")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Aviso(
                "Envie o lembrete ao grupo. Quando os Pix caírem na caixinha, registre o aporte: " +
                    "o aporte mensal é só registrado, sem mensagem para o grupo.",
            )
            BotoesDeRegistro(
                habilitado = true,
                trabalhando = trabalhando,
                textoVisualizar = "Enviar lembrete",
                textoRegistrar = "Registrar",
                aoVisualizar = {
                    val chave = estado.administrador?.chavePix.orEmpty()
                    Compartilhar.whatsapp(ctx, Mensagens.lembreteAporte(estado.nomeExibicao, estado.config.aporteMensal, chave))
                },
                aoRegistrar = { confirmar = true },
            )
        } else {
            Text("Aporte adicional", style = MaterialTheme.typography.titleMedium)
            CampoData(
                data = data,
                aoMudar = { data = it },
                rotulo = "Data do aporte",
                modifier = Modifier.fillMaxWidth(),
            )
            CampoDinheiro(
                centavos = valor,
                aoMudar = { valor = it },
                rotulo = "Valor total do aporte",
                modifier = Modifier.fillMaxWidth(),
            )
            Aviso("Aporte adicional só com a concordância dos 3 integrantes, registrada no grupo (Parte 2 do termo).")
            problema?.takeIf { valor > 0 }?.let { Aviso(it, alerta = true) }
            BotoesDeRegistro(
                habilitado = problema == null,
                trabalhando = trabalhando,
                aoVisualizar = { vendo = true },
                aoRegistrar = { confirmar = true },
                visualizarHabilitado = valor > 0,
            )
        }

        Spacer(Modifier.height(8.dp))
        Historico(
            titulo = "Histórico de aportes",
            estado = estado,
            filtro = { it.tipo == TipoLancamento.APORTE },
            abrir = abrir,
        )
    }

    if (vendo) {
        VisualizadorComprovante(
            estado,
            Mensagens.tituloAporte(dataAporte),
            aoFechar = { vendo = false },
            previa = Recibos.previa(estado, TipoComprovante.APORTE, texto, 0),
        )
    }

    if (confirmar) {
        DialogoConfirmar(
            titulo = if (mensal) "Registrar o aporte mensal?" else "Registrar o aporte adicional?",
            texto = (
                if (mensal) "Aporte de ${Dinheiro.rs(valorAporte)} em ${Datas.longa(dataAporte)}. Fica só registrado (sem mensagem para o grupo)."
                else "Aporte de ${Dinheiro.rs(valorAporte)} em ${Datas.longa(dataAporte)}. Em seguida o WhatsApp abre com o comprovante."
                ) + "\n\n" + AVISO_DEFINITIVO,
            confirmar = "Confirmar",
            aoConfirmar = {
                confirmar = false
                trabalhando = true
                escopo.launch {
                    try {
                        val agora = System.currentTimeMillis()
                        if (mensal) {
                            registrarComprovante(ctx, { Operacoes.registrarAporteMensal(it, hoje, agora) }, enviar = false)
                            avisos.showSnackbar("Aporte mensal registrado.")
                        } else {
                            val d = data
                            val v = valor
                            registrarComprovante(ctx, { Operacoes.registrarAporteAdicional(it, d, v, agora) })
                            valor = 0L
                        }
                    } catch (t: Throwable) {
                        avisos.showSnackbar("Não foi possível registrar: ${t.message}")
                    } finally {
                        trabalhando = false
                    }
                }
            },
            aoFechar = { confirmar = false },
        )
    }
}

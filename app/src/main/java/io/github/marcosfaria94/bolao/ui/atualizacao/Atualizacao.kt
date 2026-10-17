package io.github.marcosfaria94.bolao.ui.atualizacao

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.marcosfaria94.bolao.net.Atualizador
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface EtapaAtualizacao {
    data object Nada : EtapaAtualizacao
    data object Verificando : EtapaAtualizacao
    data object EmDia : EtapaAtualizacao
    data class Disponivel(val versao: Atualizador.Versao) : EtapaAtualizacao
    data class PrecisaPermissao(val versao: Atualizador.Versao) : EtapaAtualizacao
    data class Baixando(val versao: Atualizador.Versao, val progresso: Float) : EtapaAtualizacao
    data class Pronta(val versao: Atualizador.Versao, val apk: File) : EtapaAtualizacao
    data class Erro(val mensagem: String) : EtapaAtualizacao
}

class AtualizacaoViewModel(app: Application) : AndroidViewModel(app) {

    private val _etapa = MutableStateFlow<EtapaAtualizacao>(EtapaAtualizacao.Nada)
    val etapa = _etapa.asStateFlow()

    /** Última versão publicada conhecida (para mostrar nos Ajustes). */
    private val _ultima = MutableStateFlow<Atualizador.Versao?>(null)
    val ultima = _ultima.asStateFlow()

    init {
        verificar(manual = false)
    }

    /** Na abertura do app a conferência é silenciosa: só aparece algo se houver versão nova. */
    fun verificar(manual: Boolean) {
        if (_etapa.value is EtapaAtualizacao.Baixando) return
        if (manual) _etapa.value = EtapaAtualizacao.Verificando
        viewModelScope.launch {
            try {
                val v = Atualizador.ultimaPublicada()
                _ultima.value = v
                _etapa.value = when {
                    v.codigo > Atualizador.versaoInstalada -> EtapaAtualizacao.Disponivel(v)
                    manual -> EtapaAtualizacao.EmDia
                    else -> EtapaAtualizacao.Nada
                }
            } catch (t: Throwable) {
                _etapa.value = if (manual) {
                    EtapaAtualizacao.Erro("Não foi possível conferir agora: ${t.message ?: "sem conexão"}")
                } else {
                    EtapaAtualizacao.Nada
                }
            }
        }
    }

    fun atualizar(v: Atualizador.Versao) {
        if (_etapa.value is EtapaAtualizacao.Baixando) return
        val ctx = getApplication<Application>()
        if (!Atualizador.podeInstalar(ctx)) {
            _etapa.value = EtapaAtualizacao.PrecisaPermissao(v)
            return
        }
        _etapa.value = EtapaAtualizacao.Baixando(v, 0f)
        viewModelScope.launch {
            try {
                val apk = Atualizador.baixar(ctx, v) { p -> _etapa.value = EtapaAtualizacao.Baixando(v, p) }
                _etapa.value = EtapaAtualizacao.Pronta(v, apk)
                Atualizador.instalar(ctx, apk)
            } catch (t: Throwable) {
                _etapa.value = EtapaAtualizacao.Erro("Falha ao baixar a atualização: ${t.message ?: "erro desconhecido"}")
            }
        }
    }

    fun abrirPermissao() {
        Atualizador.abrirPermissaoInstalar(getApplication<Application>())
    }

    fun instalar(apk: File) {
        Atualizador.instalar(getApplication<Application>(), apk)
    }

    fun fechar() {
        if (_etapa.value !is EtapaAtualizacao.Baixando) _etapa.value = EtapaAtualizacao.Nada
    }
}

/** Diálogos do fluxo de atualização. */
@Composable
fun DialogosAtualizacao(vm: AtualizacaoViewModel) {
    val etapa by vm.etapa.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    when (val e = etapa) {
        EtapaAtualizacao.Nada, EtapaAtualizacao.Verificando -> Unit

        EtapaAtualizacao.EmDia -> AlertDialog(
            onDismissRequest = vm::fechar,
            confirmButton = { TextButton(onClick = vm::fechar) { Text("OK") } },
            title = { Text("Tudo em dia") },
            text = { Text("Você já está na versão mais recente (${Atualizador.nomeInstalado}).") },
        )

        is EtapaAtualizacao.Disponivel -> AlertDialog(
            onDismissRequest = vm::fechar,
            confirmButton = { Button(onClick = { vm.atualizar(e.versao) }) { Text("Atualizar") } },
            dismissButton = { TextButton(onClick = vm::fechar) { Text("Agora não") } },
            title = { Text("Nova versão ${e.versao.nome}") },
            text = {
                Column {
                    Text("Você está na ${Atualizador.nomeInstalado}. A atualização mantém todos os dados.")
                    if (e.versao.notas.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Novidades", style = MaterialTheme.typography.titleSmall)
                        Text(
                            e.versao.notas,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                }
            },
        )

        is EtapaAtualizacao.PrecisaPermissao -> {
            // Ao voltar dos ajustes com a permissão liberada, o download começa sozinho.
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                if (Atualizador.podeInstalar(ctx)) vm.atualizar(e.versao)
            }
            AlertDialog(
                onDismissRequest = vm::fechar,
                confirmButton = {
                    Button(onClick = {
                        if (Atualizador.podeInstalar(ctx)) vm.atualizar(e.versao) else vm.abrirPermissao()
                    }) { Text("Liberar") }
                },
                dismissButton = { TextButton(onClick = vm::fechar) { Text("Cancelar") } },
                title = { Text("Permitir instalar a atualização") },
                text = {
                    Text(
                        "Só na primeira vez: o Android pede para liberar \"Instalar apps desconhecidos\" " +
                            "para o Bolão Admin. Toque em Liberar, ative a opção e volte para cá.",
                    )
                },
            )
        }

        is EtapaAtualizacao.Baixando -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            confirmButton = {},
            title = { Text("Baixando ${e.versao.nome}") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { e.progresso }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("${(e.progresso * 100).toInt()}%")
                }
            },
        )

        is EtapaAtualizacao.Pronta -> AlertDialog(
            onDismissRequest = vm::fechar,
            confirmButton = { Button(onClick = { vm.instalar(e.apk) }) { Text("Instalar") } },
            dismissButton = { TextButton(onClick = vm::fechar) { Text("Depois") } },
            title = { Text("Pronto para instalar") },
            text = { Text("Se a tela de instalação não abriu, toque em Instalar.") },
        )

        is EtapaAtualizacao.Erro -> AlertDialog(
            onDismissRequest = vm::fechar,
            confirmButton = { TextButton(onClick = vm::fechar) { Text("OK") } },
            title = { Text("Atualização") },
            text = { Text(e.mensagem) },
        )
    }
}

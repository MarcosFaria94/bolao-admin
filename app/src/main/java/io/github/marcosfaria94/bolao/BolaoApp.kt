package io.github.marcosfaria94.bolao

import android.app.Application
import android.content.Context
import io.github.marcosfaria94.bolao.data.Backup
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import io.github.marcosfaria94.bolao.ui.financeiro.apagarTemporarios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

class BolaoApp : Application() {
    val repo: Repositorio by lazy { Repositorio(this) }
    val backup: Backup by lazy { Backup(this, repo) }
    /** Índices k1/k2 e odds das modalidades (Jogos, Nova aposta e concurso em destaque). */
    val indices: Indices by lazy { Indices(this) }

    /** O app está na tela (o aviso "Ação necessária" espera o Administrador sair). */
    @Volatile
    var visivel: Boolean = false

    /** Tela pedida por uma notificação tocada (rota da navegação). */
    val destinoPendente = MutableStateFlow<String?>(null)

    /** Para tarefas que não podem parar quando a tela muda (ligar e desligar o modo de teste). */
    val escopo: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        repo // começa a carregar os dados já na abertura
        indices // lê os índices guardados e configura o endereço da API da Caixa
        Notificador.criarCanais(this)
        Vigia.agendar(this)
        // Documentos de saldo não ficam guardados: apaga os que sobraram da última vez.
        runCatching { apagarTemporarios(this, idadeMinimaMs = 0) }
    }
}

val Context.bolao: BolaoApp get() = applicationContext as BolaoApp

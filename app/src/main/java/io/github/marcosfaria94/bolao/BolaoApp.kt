package io.github.marcosfaria94.bolao

import android.app.Application
import android.content.Context
import io.github.marcosfaria94.bolao.data.Backup
import io.github.marcosfaria94.bolao.data.Indices
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.data.Sessoes
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import io.github.marcosfaria94.bolao.ui.financeiro.apagarTemporarios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BolaoApp : Application() {
    /** As sessões (desde a 0.21): cada bolão com os seus dados; uma em uso de cada vez. */
    val sessoes: Sessoes by lazy { Sessoes(this) }

    /** Os dados da sessão em uso (a que aparece nas telas). */
    val repo: Repositorio get() = sessoes.repoAtivo
    val backup: Backup by lazy { Backup(this) }
    /** Índices k1/k2 e odds das modalidades (Jogos e concurso em destaque). */
    val indices: Indices by lazy { Indices(this) }

    /** O app está na tela (o aviso "Ação necessária" espera o Administrador sair). */
    @Volatile
    var visivel: Boolean = false

    /**
     * A digital já foi confirmada nesta abertura do app. Fica aqui (e não na tela) para que uma
     * tela recriada (tela dividida, mudança de tamanho, girar o celular) não peça a digital de novo.
     */
    @Volatile
    var desbloqueado: Boolean = false

    /** Quando o app saiu da tela pela última vez (relógio desde o boot), para bloquear depois de um tempo fora. */
    @Volatile
    var saiuEm: Long = 0L

    /** Tela pedida por uma notificação tocada (rota da navegação). */
    val destinoPendente = MutableStateFlow<String?>(null)

    /** Para tarefas que não podem parar quando a tela muda (ligar e desligar o modo de teste). */
    val escopo: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        repo // começa a carregar os dados da sessão em uso já na abertura
        indices // lê os índices guardados e configura o endereço da API da Caixa
        // O k1, o k2 e as odds do concurso ficam guardados nas apostas registradas (desde a 0.21).
        escopo.launch {
            combine(indices.itens, sessoes.estado) { itens, e -> itens to e }.collect { (itens, e) ->
                if (e != null && Operacoes.guardarIndices(e, itens) !== e) runCatching { repo.atualizar { Operacoes.guardarIndices(it, itens) } }
            }
        }
        Notificador.criarCanais(this)
        if (!(android.os.Build.FINGERPRINT == "robolectric")) Vigia.agendar(this)
        // Documentos de saldo não ficam guardados: apaga os que sobraram da última vez.
        runCatching { apagarTemporarios(this, idadeMinimaMs = 0) }
    }
}

val Context.bolao: BolaoApp get() = applicationContext as BolaoApp

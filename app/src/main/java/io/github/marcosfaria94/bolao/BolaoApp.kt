package io.github.marcosfaria94.bolao

import android.app.Application
import android.content.Context
import io.github.marcosfaria94.bolao.data.Backup
import io.github.marcosfaria94.bolao.data.Repositorio
import io.github.marcosfaria94.bolao.notificacoes.Notificador
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import kotlinx.coroutines.flow.MutableStateFlow

class BolaoApp : Application() {
    val repo: Repositorio by lazy { Repositorio(this) }
    val backup: Backup by lazy { Backup(this, repo) }

    /** Tela pedida por uma notificação tocada (rota da navegação). */
    val destinoPendente = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        repo // começa a carregar os dados já na abertura
        Notificador.criarCanais(this)
        Vigia.agendar(this)
    }
}

val Context.bolao: BolaoApp get() = applicationContext as BolaoApp

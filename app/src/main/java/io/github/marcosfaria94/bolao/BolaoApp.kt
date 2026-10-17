package io.github.marcosfaria94.bolao

import android.app.Application
import android.content.Context
import io.github.marcosfaria94.bolao.data.Backup
import io.github.marcosfaria94.bolao.data.Repositorio

class BolaoApp : Application() {
    val repo: Repositorio by lazy { Repositorio(this) }
    val backup: Backup by lazy { Backup(this, repo) }

    override fun onCreate() {
        super.onCreate()
        repo // começa a carregar os dados já na abertura
    }
}

val Context.bolao: BolaoApp get() = applicationContext as BolaoApp

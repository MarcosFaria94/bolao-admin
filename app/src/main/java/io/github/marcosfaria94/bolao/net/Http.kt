package io.github.marcosfaria94.bolao.net

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object Http {
    val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}

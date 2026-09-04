package com.ejemplo.nettycoon.data.remote

import com.ejemplo.nettycoon.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Construye la instancia de Retrofit para la API geo-IP. Singleton simple (sin DI nuevo).
 *
 * El logging de OkHttp se activa solo en debug ([BuildConfig.DEBUG]); en release queda en
 * `NONE` para no volcar tráfico. Todo el tráfico es HTTPS (`https://ipwho.is/`).
 */
object RetrofitProvider {

    private const val BASE_URL = "https://ipwho.is/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val geoIpApiService: GeoIpApiService by lazy {
        retrofit.create(GeoIpApiService::class.java)
    }
}

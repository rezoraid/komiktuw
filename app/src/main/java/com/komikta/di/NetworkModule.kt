package com.komikta.di

import com.komikta.core.network.ApiConfig
import com.komikta.core.network.RetryInterceptor
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(context.cacheDir.resolve("http_cache"), 50L * 1024 * 1024))
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .dispatcher(Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 12
        })
        .retryOnConnectionFailure(true)
        .addInterceptor(RetryInterceptor())
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(ApiConfig.SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(ApiConfig.SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()
}

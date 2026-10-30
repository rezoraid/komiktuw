package com.komikta.di

import com.komikta.BuildConfig
import com.komikta.core.network.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import timber.log.Timber
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
        encodeDefaults = false
        prettyPrint = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(ApiConfig.SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(ApiConfig.SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json, okHttpClient: OkHttpClient): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT_MS
                requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
                socketTimeoutMillis = ApiConfig.SOCKET_TIMEOUT_MS
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        // Avoid logging full image URLs or sensitive data in production
                        if (BuildConfig.DEBUG) {
                            Timber.tag("Ktor").d(message)
                        }
                    }
                }
                level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
            }

            defaultRequest {
                url(ApiConfig.BASE_URL)
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.UserAgent, "Komikta-Android/${BuildConfig.VERSION_NAME}")
            }

            expectSuccess = false // We handle HTTP errors manually via ApiResponse
        }
    }
}

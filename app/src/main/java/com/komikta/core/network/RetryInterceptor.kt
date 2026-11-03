package com.komikta.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val delayMs: Long = 250L) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)
        return try {
            val response = chain.proceed(request)
            if (response.code in RETRYABLE && !chain.call().isCanceled()) {
                response.close()
                Thread.sleep(delayMs)
                chain.proceed(request)
            } else {
                response
            }
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            Thread.sleep(delayMs)
            chain.proceed(request)
        }
    }

    private companion object {
        val RETRYABLE = setOf(502, 503, 504)
    }
}

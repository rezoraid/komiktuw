package com.komikta.di

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.komikta.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        // Deteksi perangkat low-RAM agar cache & concurrency lebih aman
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRam = if (Build.VERSION.SDK_INT >= 19) am.isLowRamDevice else false
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalMb = memInfo.totalMem / (1024 * 1024)
        val lowEnd = isLowRam || totalMb < 3072L // < ~3 GB dianggap low-end

        val maxRequests = if (lowEnd) 12 else 20
        val maxPerHost = if (lowEnd) 4 else 6
        // Memory cache lebih kecil di low-end agar tidak menekan heap untuk UI/reader
        val memPercent = when {
            lowEnd -> 0.12
            totalMb < 4096L -> 0.18
            else -> 0.22
        }
        val diskBytes = when {
            lowEnd -> 48L * 1024 * 1024
            totalMb < 4096L -> 80L * 1024 * 1024
            else -> 120L * 1024 * 1024
        }

        val imageClient = okHttpClient.newBuilder()
            .dispatcher(Dispatcher().apply {
                this.maxRequests = maxRequests
                maxRequestsPerHost = maxPerHost
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(imageClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(memPercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskBytes)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .allowRgb565(true)
            .respectCacheHeaders(false)
            // Hindari decode ke ARGB_8888 penuh bila tidak perlu; Coil + RGB565 hemat memori signifikan
            .apply {
                if (BuildConfig.DEBUG) logger(DebugLogger())
            }
            .build()
    }
}

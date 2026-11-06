package com.komikta.core.cache

import com.komikta.core.common.Result
import com.komikta.data.local.dao.CacheDao
import com.komikta.data.local.entity.CacheEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseCache @Inject constructor(
    private val dao: CacheDao,
    private val json: Json
) {
    class Hit<T>(val value: T, val storedAt: Long)

    private class Entry(val value: Any, val storedAt: Long)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val memory = object : LinkedHashMap<String, Entry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > MEMORY_ENTRIES
    }
    private val inflight = HashMap<String, CompletableDeferred<Result<Any?>>>()

    init {
        scope.launch {
            runCatching {
                dao.deleteOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_AGE_DAYS))
                dao.keepNewest(DISK_ENTRIES)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> peekMemory(key: String): Hit<T>? = synchronized(lock) {
        memory[key]?.let { Hit(it.value as T, it.storedAt) }
    }

    suspend fun <T> peek(key: String, serializer: KSerializer<T>): Hit<T>? {
        peekMemory<T>(key)?.let { return it }
        val row = runCatching { dao.get(key) }.getOrNull() ?: return null
        val value = runCatching { json.decodeFromString(serializer, row.payload) }.getOrNull() ?: return null
        synchronized(lock) { memory[key] = Entry(value as Any, row.storedAt) }
        return Hit(value, row.storedAt)
    }

    fun <T> put(key: String, serializer: KSerializer<T>, value: T) {
        val now = System.currentTimeMillis()
        synchronized(lock) { memory[key] = Entry(value as Any, now) }
        scope.launch {
            runCatching { dao.put(CacheEntity(key, json.encodeToString(serializer, value), now)) }
        }
    }

    private fun touch(key: String) {
        val now = System.currentTimeMillis()
        synchronized(lock) { memory[key]?.let { memory[key] = Entry(it.value, now) } }
    }

    fun <T> update(key: String, serializer: KSerializer<T>, transform: (T) -> T) {
        val current = peekMemory<T>(key) ?: return
        put(key, serializer, transform(current.value))
    }

    fun clearPrefix(prefix: String) {
        synchronized(lock) { memory.keys.removeAll { it.startsWith(prefix) } }
        scope.launch { runCatching { dao.deletePrefix(prefix) } }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> shared(key: String, fetch: suspend () -> Result<T>): Result<T> {
        val (deferred, owner) = synchronized(lock) {
            val existing = inflight[key]
            if (existing != null) existing to false
            else CompletableDeferred<Result<Any?>>().also { inflight[key] = it } to true
        }
        if (owner) {
            scope.launch {
                val result = try {
                    fetch()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Result.Error("Dibatalkan")
                } catch (e: Exception) {
                    Result.Error(e.message ?: "Terjadi kesalahan", throwable = e)
                }
                synchronized(lock) { inflight.remove(key) }
                deferred.complete(result as Result<Any?>)
            }
        }
        return deferred.await() as Result<T>
    }

    suspend fun <T> load(
        key: String,
        serializer: KSerializer<T>,
        freshMs: Long,
        fetch: suspend () -> Result<T>
    ): Result<T> {
        val hit = peek(key, serializer)
        if (hit != null) {
            // Data lama tetap ditampilkan segera. Jika sudah stale, refresh berjalan di background
            // dan hasil baru menggantikan cache untuk pembukaan berikutnya.
            if (System.currentTimeMillis() - hit.storedAt > freshMs) {
                scope.launch {
                    when (val refreshed = shared(key, fetch)) {
                        is Result.Success -> put(key, serializer, refreshed.data)
                        else -> Unit // cache lama dipertahankan ketika jaringan gagal
                    }
                }
            }
            return Result.Success(hit.value)
        }
        return when (val r = shared(key, fetch)) {
            is Result.Success -> r.also { put(key, serializer, it.data) }
            else -> r
        }
    }

    fun <T> stream(
        key: String,
        serializer: KSerializer<T>,
        freshMs: Long,
        refresh: Boolean = false,
        wrap: (T) -> Result<T> = { Result.Success(it) },
        fetch: suspend () -> Result<T>
    ): Flow<Result<T>> = flow {
        val hit = peek(key, serializer)
        if (hit != null) emit(wrap(hit.value))
        val stale = hit == null || refresh || System.currentTimeMillis() - hit.storedAt > freshMs
        if (!stale) return@flow
        val r = shared(key, fetch)
        if (r is Result.Success) {
            if (hit == null || hit.value != r.data) {
                put(key, serializer, r.data)
                emit(wrap(r.data))
            } else touch(key)
        } else if (hit == null) {
            emit(r)
        }
    }

    private companion object {
        const val MEMORY_ENTRIES = 160
        const val DISK_ENTRIES = 400
        const val MAX_AGE_DAYS = 14L
    }
}

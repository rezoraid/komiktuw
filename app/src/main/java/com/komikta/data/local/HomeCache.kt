package com.komikta.data.local

import android.content.Context
import com.komikta.domain.model.Comic
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class HomeSnapshot(
    val latest: List<Comic> = emptyList(),
    val popular: List<Comic> = emptyList()
)

@Singleton
class HomeCache @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val file = File(context.filesDir, "home_snapshot.json")

    suspend fun read(): HomeSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) null
            else json.decodeFromString(HomeSnapshot.serializer(), file.readText())
                .takeIf { it.latest.isNotEmpty() || it.popular.isNotEmpty() }
        }.getOrNull()
    }

    suspend fun write(snapshot: HomeSnapshot) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(file.parentFile, "home_snapshot.tmp")
            tmp.writeText(json.encodeToString(HomeSnapshot.serializer(), snapshot))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
        Unit
    }
}

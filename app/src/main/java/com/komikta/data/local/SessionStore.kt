package com.komikta.data.local

import android.content.Context
import com.komikta.core.common.AvatarBust
import com.komikta.data.remote.dto.CommunityUserDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val sp = context.getSharedPreferences("komikta_session", Context.MODE_PRIVATE)
    private val devicePrefs = context.getSharedPreferences("komikta_device", Context.MODE_PRIVATE)

    /** ID acak per instalasi (bukan data pribadi); dipakai server agar satu perangkat tidak menambah views berulang. Tidak ikut terhapus saat logout. */
    val deviceId: String by lazy {
        devicePrefs.getString("id", null) ?: java.util.UUID.randomUUID().toString().also { devicePrefs.edit().putString("id", it).apply() }
    }
    @Volatile
    private var avatarRev = sp.getLong("avatarRev", 0L)
    private val _user = MutableStateFlow(readUser())
    val user: StateFlow<CommunityUserDto?> = _user.asStateFlow()

    @Volatile
    var token: String? = sp.getString("token", null)
        private set

    init { AvatarBust.set(_user.value?.picture, avatarRev) }

    private fun readUser(): CommunityUserDto? {
        if (sp.getString("token", null).isNullOrBlank()) return null
        val raw = sp.getString("user", null) ?: return null
        return runCatching { json.decodeFromString(CommunityUserDto.serializer(), raw) }.getOrNull()
    }

    private fun busted(u: CommunityUserDto): CommunityUserDto {
        val p = u.picture
        if (avatarRev <= 0L || p.isBlank() || p.startsWith("data:") || p.startsWith("file:")) return u
        return u.copy(picture = p + (if ('?' in p) "&" else "?") + "rev=$avatarRev")
    }

    fun bustedUrl(picture: String, revision: Long): String =
        if (picture.isBlank() || picture.startsWith("data:") || picture.startsWith("file:")) picture
        else picture + (if ('?' in picture) "&" else "?") + "rev=$revision"

    fun bumpAvatar(revision: Long = System.currentTimeMillis()) {
        avatarRev = revision
        sp.edit().putLong("avatarRev", avatarRev).apply()
        AvatarBust.set(_user.value?.picture, avatarRev)
    }

    fun save(token: String, incoming: CommunityUserDto) {
        val user = busted(incoming)
        this.token = token
        sp.edit().putString("token", token).putString("user", json.encodeToString(CommunityUserDto.serializer(), user)).apply()
        _user.value = user
        AvatarBust.set(user.picture, avatarRev)
    }

    fun update(incoming: CommunityUserDto) {
        if (token == null) return
        val old = _user.value
        val keep = if (old != null && old.id == incoming.id) {
            incoming.copy(
                commentCount = if (incoming.commentCount < 0) old.commentCount else incoming.commentCount,
                chaptersRead = if (incoming.chaptersRead < 0) old.chaptersRead else incoming.chaptersRead
            )
        } else incoming
        val user = busted(keep)
        sp.edit().putString("user", json.encodeToString(CommunityUserDto.serializer(), user)).apply()
        _user.value = user
        AvatarBust.set(user.picture, avatarRev)
    }

    fun replaceLocal(user: CommunityUserDto) {
        if (token == null) return
        sp.edit().putString("user", json.encodeToString(CommunityUserDto.serializer(), user)).apply()
        _user.value = user
        AvatarBust.set(user.picture, avatarRev)
    }

    fun updateToken(value: String) {
        if (token == null) return
        token = value
        sp.edit().putString("token", value).apply()
    }

    fun clear() {
        token = null
        sp.edit().clear().apply()
        _user.value = null
        AvatarBust.set(null, 0L)
    }
}

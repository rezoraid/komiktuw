package com.komikta.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.komikta.core.common.Result
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.data.repository.CommunityRepository
import com.komikta.ui.components.agoId
import com.komikta.ui.components.VerifiedBadge
import com.komikta.ui.theme.Ui
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OwnerToolsState(
    val loading: Boolean = true,
    val working: Boolean = false,
    val user: CommunityUserDto? = null,
    val error: String? = null,
    val notice: String? = null
)

/** Alat khusus OWNER untuk satu akun. Server tetap memeriksa peran di setiap permintaan. */
@HiltViewModel
class OwnerToolsViewModel @Inject constructor(private val repo: CommunityRepository) : ViewModel() {
    private val _ui = MutableStateFlow(OwnerToolsState())
    val ui: StateFlow<OwnerToolsState> = _ui.asStateFlow()
    private var id = ""

    fun open(userId: String) {
        _ui.value = OwnerToolsState()
        load(userId)
    }

    fun load(userId: String) {
        id = userId
        _ui.update { it.copy(loading = it.user == null, error = null) }
        viewModelScope.launch {
            when (val r = repo.adminUser(userId)) {
                is Result.Success -> _ui.update { it.copy(loading = false, user = r.data, error = null) }
                is Result.Error -> _ui.update { it.copy(loading = false, error = r.message) }
                else -> _ui.update { it.copy(loading = false) }
            }
        }
    }

    private fun act(done: String, block: suspend () -> Result<*>) {
        if (_ui.value.working) return
        viewModelScope.launch {
            _ui.update { it.copy(working = true, notice = null) }
            when (val r = block()) {
                is Result.Success -> { _ui.update { it.copy(working = false, notice = done) }; load(id) }
                is Result.Error -> _ui.update { it.copy(working = false, notice = r.message) }
                else -> _ui.update { it.copy(working = false) }
            }
        }
    }

    fun grantXp(amount: Int) = act(if (amount > 0) "XP ditambahkan" else "XP dikurangi") { repo.adminGrantXp(id, amount) }
    fun setRole(role: String) = act("Role diubah") { repo.adminSetRole(id, role) }
    fun setVerified(verified: Boolean) = act(if (verified) "Akun diverifikasi" else "Verifikasi dicabut") { repo.adminSetVerified(id, verified) }
    fun setBan(banned: Boolean) = act(if (banned) "Akun diblokir" else "Blokir dibuka") { repo.adminSetBan(id, banned) }
    fun resetMedia(avatar: Boolean) = act("Foto dihapus") { repo.adminResetMedia(id, avatar) }
}

@Composable
internal fun OwnerToolsDialog(
    userId: String,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    onOpenProfile: (() -> Unit)? = null,
    vm: OwnerToolsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { vm.open(userId) }
    // Setiap aksi sukses mengubah data akun: segarkan halaman profil di belakang dialog.
    LaunchedEffect(ui.notice) { if (ui.notice != null && ui.error == null) onChanged() }
    val u = ui.user
    var amount by remember { mutableStateOf("") }
    var confirmBan by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ui.Card).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Kelola pengguna", style = MaterialTheme.typography.titleMedium)
            when {
                ui.loading -> Text("Memuat…", fontSize = 14.sp, color = Ui.Text2)
                u == null -> {
                    Text(ui.error ?: "Gagal memuat akun", fontSize = 14.sp, color = Ui.Text2)
                    TextButton(onClick = { vm.load(userId) }) { Text("Coba lagi", color = Color.White) }
                }
                else -> {
                    val isOwner = u.role == "OWNER"
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("@${u.username}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (!u.email.isNullOrBlank()) Text(u.email, fontSize = 13.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Level ${u.level} • ${if (isOwner) "XP tidak berlaku" else "${u.xp} XP"}",
                            fontSize = 13.sp, color = Ui.Text2
                        )
                        Text("${u.chaptersRead.coerceAtLeast(0)} chapter dibaca • ${u.commentCount.coerceAtLeast(0)} komentar", fontSize = 13.sp, color = Ui.Text2)
                        Text(
                            (if (u.banned) "Diblokir" else "Aktif") + if (u.lastLoginAt > 0) " • login ${agoId(u.lastLoginAt, System.currentTimeMillis())}" else "",
                            fontSize = 13.sp, color = if (u.banned) Color(0xFFE08F8F) else Ui.Text2
                        )
                    }

                    Section("Role")
                    if (isOwner) {
                        Text("Role OWNER tidak bisa diubah dari aplikasi.", fontSize = 13.sp, color = Ui.Text3)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("USER" to "Member", "PREMIUM" to "Premium", "MODERATOR" to "Moderator").forEach { (role, label) ->
                                Pill(label, Modifier.weight(1f), selected = u.role == role, enabled = !ui.working && u.role != role) { vm.setRole(role) }
                            }
                        }
                    }

                    Section("Verified")
                    if (isOwner) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VerifiedBadge(size = 18.dp)
                            Text("OWNER selalu terverifikasi.", fontSize = 13.sp, color = Ui.Text3)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("Verified", Modifier.weight(1f), selected = u.verified, enabled = !ui.working && !u.verified) { vm.setVerified(true) }
                            Pill("Belum", Modifier.weight(1f), selected = !u.verified, enabled = !ui.working && u.verified) { vm.setVerified(false) }
                        }
                    }

                    if (!isOwner) {
                        Section("XP (1 – 50.000)")
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ui.Chip).padding(12.dp)) {
                            BasicTextField(
                                value = amount,
                                onValueChange = { amount = it.filter(Char::isDigit).take(5) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        val n = amount.toIntOrNull()?.takeIf { it in 1..50_000 }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("Tambah", Modifier.weight(1f), enabled = n != null && !ui.working) { vm.grantXp(n!!); amount = "" }
                            Pill("Kurangi", Modifier.weight(1f), enabled = n != null && !ui.working) { vm.grantXp(-n!!); amount = "" }
                        }
                    }

                    Section("Konten")
                    Pill("Hapus foto", Modifier.fillMaxWidth(), enabled = u.hasAvatar && !ui.working) { vm.resetMedia(avatar = true) }

                    if (!isOwner) {
                        Section("Akun")
                        Pill(
                            when {
                                u.banned -> "Buka blokir"
                                confirmBan -> "Ketuk lagi untuk memblokir"
                                else -> "Blokir akun"
                            },
                            Modifier.fillMaxWidth(), danger = !u.banned, enabled = !ui.working
                        ) {
                            if (u.banned) vm.setBan(false)
                            else if (confirmBan) { confirmBan = false; vm.setBan(true) }
                            else confirmBan = true
                        }
                    }
                }
            }
            ui.notice?.let { Text(it, fontSize = 13.sp, color = Color.White) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onOpenProfile != null && u != null) TextButton(onClick = onOpenProfile) { Text("Lihat profil", color = Color.White) }
                TextButton(onClick = onDismiss) { Text("Tutup", color = Ui.Text2) }
            }
        }
    }
}

@Composable
private fun Section(label: String) {
    Text(label.uppercase(), fontSize = 11.sp, letterSpacing = 1.sp, color = Ui.Text3)
}

@Composable
private fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val fg = when {
        selected -> Ui.Bg
        danger -> Color(0xFFE08F8F)
        else -> Color.White
    }
    Box(
        modifier.height(36.dp).clip(CircleShape)
            .background(if (selected) Ui.Pill else Color.Transparent)
            .border(1.dp, if (selected) Ui.Pill else Ui.Outline, CircleShape)
            .alpha(if (enabled || selected) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

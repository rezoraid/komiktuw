package com.komikta.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.BuildConfig
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.feature.auth.rememberGoogleSignIn
import com.komikta.ui.components.AppIcons
import com.komikta.ui.navigation.BottomNavItem
import com.komikta.ui.theme.Ui
import kotlinx.coroutines.delay

/** Profil sendiri. Tab Comments/Likes/All memakai data backend; pengaturan akun ada di menu ikon gerigi. */
@Composable
fun ProfileScreen(
    onOpen: (source: String, slug: String, chapter: String?) -> Unit = { _, _, _ -> },
    onModeration: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    pageModel: ProfilePageViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val page by pageModel.state.collectAsStateWithLifecycle()
    val signIn = rememberGoogleSignIn(onToken = viewModel::signIn, onError = viewModel::message, onStart = { viewModel.busy(true) }, onDone = { viewModel.busy(false) })
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var showXp by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.changeAvatar(uri)
    }
    LaunchedEffect(user?.id) {
        viewModel.refresh()
        user?.id?.let { pageModel.load(it) }
    }
    LaunchedEffect(ui.message) {
        if (ui.message != null) {
            delay(3000)
            viewModel.message(null)
        }
    }

    val u = user
    if (u == null) {
        Column(
            Modifier.fillMaxSize().background(Ui.Bg).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            GuestContent(
                busy = ui.busy,
                message = ui.message,
                onSignIn = signIn,
                onTerms = { uriHandler.openUri("${BuildConfig.BASE_URL}/terms") },
                onPrivacy = { uriHandler.openUri("${BuildConfig.BASE_URL}/privacy") }
            )
        }
    } else {
        // Identitas (nama, foto, peran, level) selalu dari sesi terbaru; angka statistik dari backend.
        val shown = remember(page, u) {
            page.copy(stats = page.stats?.copy(name = u.name, username = u.username, picture = u.picture, role = u.role, level = u.level))
        }
        Box(Modifier.fillMaxSize().background(P.Content)) {
            ProfilePage(
                state = shown,
                fallbackUser = u,
                busy = ui.busy,
                onTab = pageModel::selectTab,
                onLoadMore = pageModel::loadMore,
                onRetry = pageModel::retry,
                onItemClick = { onOpen(it.source, it.slug, it.chapter) },
                onAvatarClick = { avatarPicker.launch("image/*") },
                onLevelClick = { showXp = true },
                topLeft = {
                    SettingsMenu(
                        u,
                        onPhoto = { avatarPicker.launch("image/*") },
                        onEdit = { editing = true },
                        onVisibility = { viewModel.setHistoryVisibility(!u.publicHistory) },
                        onModeration = onModeration,
                        onTerms = { uriHandler.openUri("${BuildConfig.BASE_URL}/terms") },
                        onPrivacy = { uriHandler.openUri("${BuildConfig.BASE_URL}/privacy") },
                        onDelete = { confirmDelete = true }
                    )
                },
                topRight = {
                    Text(
                        "Logout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { confirmLogout = true }.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            )
            ui.message?.let {
                Text(
                    it, fontSize = 14.sp, color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF2B2C31)).padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }

    if (showXp && u != null) {
        val span = (u.xpNextLevel - u.xpCurrentLevel).coerceAtLeast(1)
        val into = (u.xp - u.xpCurrentLevel).coerceIn(0, span)
        AlertDialog(
            onDismissRequest = { showXp = false },
            containerColor = Ui.Card,
            title = { Text("Level ${u.level}", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Menuju level ${u.level + 1}", fontSize = 14.sp, color = Ui.Text2)
                        Text("${u.xp} / ${u.xpNextLevel} XP", fontSize = 14.sp, color = Ui.Text2)
                    }
                    Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Ui.Line)) {
                        Box(Modifier.fillMaxWidth(into.toFloat() / span).height(6.dp).background(P.Blue))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showXp = false }) { Text("Tutup", color = Ui.Accent) } }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = Ui.Card,
            title = { Text("Keluar dari akun?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Kamu perlu masuk lagi dengan Google untuk berkomentar dan mengumpulkan XP.", color = Ui.Text2, fontSize = 15.sp) },
            confirmButton = { TextButton(onClick = { viewModel.signOut(); confirmLogout = false }) { Text("Keluar", color = Color(0xFFE08F8F)) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Batal", color = Ui.Text2) } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Ui.Card,
            title = { Text("Hapus akun?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Akun dan semua komentarmu akan dihapus permanen. XP dan level hilang. Tindakan ini tidak bisa dibatalkan.", color = Ui.Text2, fontSize = 15.sp) },
            confirmButton = { TextButton(onClick = { viewModel.deleteAccount(); confirmDelete = false }) { Text("Hapus", color = Ui.Accent) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal", color = Ui.Text2) } }
        )
    }

    if (editing) {
        var value by remember { mutableStateOf(user?.username.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            containerColor = Ui.Card,
            title = { Text("Ganti username", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ui.Chip).padding(12.dp)) {
                        BasicTextField(
                            value = value,
                            onValueChange = { value = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            cursorBrush = SolidColor(Ui.Accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("3-20 karakter: huruf kecil, angka, underscore. Bisa diganti sekali per 7 hari.", fontSize = 13.sp, color = Ui.Text3, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { val name = value; editing = false; viewModel.changeUsername(name) }, enabled = value.length >= 3) { Text("Simpan", color = Ui.Accent) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Batal", color = Ui.Text2) } }
        )
    }
}

@Composable
private fun SettingsMenu(
    u: CommunityUserDto,
    onPhoto: () -> Unit,
    onEdit: () -> Unit,
    onVisibility: () -> Unit,
    onModeration: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val item: @Composable (String, Color, () -> Unit) -> Unit = { label, color, action ->
        DropdownMenuItem(text = { Text(label, fontSize = 15.sp, color = color) }, onClick = { open = false; action() })
    }
    Box {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { open = true }, contentAlignment = Alignment.Center) {
            Icon(AppIcons.settings, "Pengaturan", tint = Color.White, modifier = Modifier.size(24.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Ui.Card)) {
            item("Ganti foto profil", Color.White, onPhoto)
            item("Ganti username", Color.White, onEdit)
            item(if (u.publicHistory) "Riwayat & Likes: publik" else "Riwayat & Likes: privat", Color.White, onVisibility)
            if (u.role == "MODERATOR" || u.role == "OWNER") item("Moderasi", Color.White, onModeration)
            item("Ketentuan", Color.White, onTerms)
            item("Kebijakan Privasi", Color.White, onPrivacy)
            item("Hapus akun", Color(0xFFE08F8F), onDelete)
            DropdownMenuItem(text = { Text("Komikta ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = Ui.Text3) }, onClick = {}, enabled = false)
        }
    }
}

@Composable
private fun GuestContent(busy: Boolean, message: String?, onSignIn: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Ui.Chip), contentAlignment = Alignment.Center) {
            Icon(BottomNavItem.Profile.unselectedIcon, null, tint = Ui.Text2, modifier = Modifier.size(38.dp))
        }
        Text("Pembaca tamu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text("Bookmark dan riwayat baca tersimpan di perangkat ini", fontSize = 14.sp, color = Ui.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
    Column(Modifier.padding(top = 28.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ui.Card).padding(16.dp)) {
        Text("Masuk untuk ikut komunitas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Tulis komentar, balas, beri like, dan kumpulkan XP di setiap chapter.", fontSize = 14.sp, lineHeight = 20.sp, color = Ui.Text2, modifier = Modifier.padding(top = 4.dp))
        Text(
            if (busy) "Memproses…" else "Masuk dengan Google",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ui.OnAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Ui.AccentBrush)
                .clickable(enabled = !busy, onClick = onSignIn)
                .padding(vertical = 13.dp)
        )
        message?.let { Text(it, fontSize = 13.sp, color = Ui.Text2, modifier = Modifier.padding(top = 10.dp)) }
    }
    Text("TENTANG APLIKASI", fontSize = 12.sp, letterSpacing = 1.sp, color = Ui.Text3, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp, start = 2.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ui.Card)) {
        InfoLine("Ketentuan", null, onTerms)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
        InfoLine("Kebijakan Privasi", null, onPrivacy)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
        InfoLine("Versi", BuildConfig.VERSION_NAME, null)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
        InfoLine("Sumber", "Lumos · Manta · Shinigami", null)
    }
}

@Composable
private fun InfoLine(label: String, value: String?, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, fontSize = 14.sp, color = Ui.Text2)
        if (onClick != null) Icon(AppIcons.chevronRight, null, tint = Ui.Text3, modifier = Modifier.size(18.dp))
    }
}


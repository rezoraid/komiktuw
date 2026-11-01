package com.komikta.feature.profile

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Icon
import com.komikta.ui.components.AppIcons
import androidx.compose.foundation.shape.CircleShape
import com.komikta.ui.navigation.BottomNavItem
import com.komikta.data.local.entity.BookmarkEntity
import com.komikta.data.local.entity.ReadingHistoryEntity
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.historyChapterLabel
import com.komikta.ui.components.relativeTime
import com.komikta.BuildConfig
import com.komikta.R
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.feature.auth.rememberGoogleSignIn
import com.komikta.ui.components.Avatar
import com.komikta.ui.components.Chip
import com.komikta.ui.components.LevelChip
import com.komikta.ui.components.RoleBadge
import com.komikta.ui.theme.Ui

@Composable
fun ProfileScreen(onComicClick: (String, String) -> Unit = { _, _ -> }, onModeration: () -> Unit = {}, viewModel: ProfileViewModel = hiltViewModel()) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val signIn = rememberGoogleSignIn(onToken = viewModel::signIn, onError = viewModel::message, onStart = { viewModel.busy(true) }, onDone = { viewModel.busy(false) })
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        val u = user
        if (u == null) {
            GuestContent(
                busy = ui.busy,
                message = ui.message,
                onSignIn = signIn,
                onTerms = { uriHandler.openUri("${BuildConfig.BASE_URL}/terms") },
                onPrivacy = { uriHandler.openUri("${BuildConfig.BASE_URL}/privacy") }
            )
        } else {
            SignedIn(
                u,
                bookmarks = bookmarks,
                history = history,
                onComicClick = onComicClick,
                onEdit = { editing = true },
                onSignOut = viewModel::signOut,
                onDelete = { confirmDelete = true },
                onModeration = onModeration
            )
            ui.message?.let { Text(it, fontSize = 12.sp, color = Ui.Text2, modifier = Modifier.padding(top = 14.dp)) }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ketentuan", fontSize = 12.sp, color = Ui.Accent, modifier = Modifier.clickable { uriHandler.openUri("${BuildConfig.BASE_URL}/terms") }.padding(vertical = 6.dp))
                Text("Kebijakan Privasi", fontSize = 12.sp, color = Ui.Accent, modifier = Modifier.clickable { uriHandler.openUri("${BuildConfig.BASE_URL}/privacy") }.padding(vertical = 6.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Komikta ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = Ui.Text3)
            Text("Sumber: Lumos · Manta · Shinigami", fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(top = 2.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Ui.Card,
            title = { Text("Hapus akun?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Akun dan semua komentarmu akan dihapus permanen. XP dan level hilang. Tindakan ini tidak bisa dibatalkan.", color = Ui.Text2, fontSize = 14.sp) },
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
                    Text("3-20 karakter: huruf kecil, angka, underscore. Bisa diganti sekali per 7 hari.", fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.changeUsername(value) { ok -> if (ok) editing = false } }, enabled = value.length >= 3 && !ui.busy) { Text("Simpan", color = Ui.Accent) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Batal", color = Ui.Text2) } }
        )
    }
}

@Composable
private fun GuestContent(busy: Boolean, message: String?, onSignIn: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Ui.Chip), contentAlignment = Alignment.Center) {
            Icon(BottomNavItem.Profile.unselectedIcon, null, tint = Ui.Text2, modifier = Modifier.size(38.dp))
        }
        Text("Pembaca tamu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text("Bookmark dan riwayat baca tersimpan di perangkat ini", fontSize = 13.sp, color = Ui.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
    Column(Modifier.padding(top = 28.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Ui.Card).padding(16.dp)) {
        Text("Masuk untuk ikut komunitas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("Tulis komentar, balas, beri like, dan kumpulkan XP di setiap chapter.", fontSize = 13.sp, lineHeight = 19.sp, color = Ui.Text2, modifier = Modifier.padding(top = 4.dp))
        Text(
            if (busy) "Memproses…" else "Masuk dengan Google",
            fontSize = 14.sp,
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
        message?.let { Text(it, fontSize = 12.sp, color = Ui.Text2, modifier = Modifier.padding(top = 10.dp)) }
    }
    Text("TENTANG APLIKASI", fontSize = 11.sp, letterSpacing = 1.sp, color = Ui.Text3, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp, start = 2.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Ui.Card)) {
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
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, fontSize = 13.sp, color = Ui.Text2)
        if (onClick != null) Icon(AppIcons.chevronRight, null, tint = Ui.Text3, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SignedIn(
    u: CommunityUserDto,
    bookmarks: List<BookmarkEntity>,
    history: List<ReadingHistoryEntity>,
    onComicClick: (String, String) -> Unit,
    onEdit: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
    onModeration: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val now = remember { System.currentTimeMillis() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text("Keluar", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ui.Text2, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onSignOut).padding(horizontal = 8.dp, vertical = 6.dp))
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(u.picture, u.name.ifBlank { u.username }, 96.dp)
        Text(u.name.ifBlank { u.username }, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 14.dp))
        Text("@${u.username}", fontSize = 13.sp, color = Ui.Text2, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val staff = u.role == "OWNER" || u.role == "MODERATOR" || u.role == "PREMIUM"
            Pill(
                when (u.role) { "OWNER" -> "Owner"; "MODERATOR" -> "Moderator"; "PREMIUM" -> "Premium"; else -> "Member" },
                if (staff) Ui.Accent else Ui.Chip,
                if (staff) Ui.OnAccent else Color.White
            )
            Pill("Lv ${u.level}", Ui.Chip, Color.White)
            Pill("${u.xp} XP", Ui.Chip, Color.White)
        }
    }
    val span = (u.xpNextLevel - u.xpCurrentLevel).coerceAtLeast(1)
    val into = (u.xp - u.xpCurrentLevel).coerceIn(0, span)
    Row(Modifier.padding(top = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Menuju level ${u.level + 1}", fontSize = 11.sp, color = Ui.Text3)
        Text("${u.xp} / ${u.xpNextLevel}", fontSize = 11.sp, color = Ui.Text3)
    }
    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Ui.Line)) {
        Box(Modifier.fillMaxWidth(into.toFloat() / span).height(4.dp).background(Ui.Accent))
    }
    val joined = remember(u.joinedAt) {
        if (u.joinedAt <= 0L) "-" else {
            val days = ((now - u.joinedAt) / 86_400_000L).coerceAtLeast(0)
            if (days < 30) "$days" else "${days / 30}"
        }
    }
    val joinedLabel = if (u.joinedAt > 0L && (now - u.joinedAt) / 86_400_000L >= 30) "bulan\nbergabung" else "hari\nbergabung"
    Row(Modifier.padding(top = 24.dp).fillMaxWidth()) {
        Stat(if (u.commentCount >= 0) "${u.commentCount}" else "-", "komentar", Modifier.weight(1f))
        Stat("${bookmarks.size}", "bookmark", Modifier.weight(1f))
        Stat(joined, joinedLabel, Modifier.weight(1f))
    }
    Row(Modifier.padding(top = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf("Riwayat", "Bookmark").forEachIndexed { i, label ->
            Column(Modifier.clickable { tab = i }.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontSize = 14.sp, fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal, color = if (tab == i) Color.White else Ui.Text2)
                Box(Modifier.padding(top = 8.dp).size(width = 24.dp, height = 2.dp).background(if (tab == i) Ui.Accent else Color.Transparent))
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
    if (tab == 0) {
        if (history.isEmpty()) EmptyLine("Belum ada riwayat baca")
        history.take(20).forEach { h ->
            ComicLine(h.comicTitle, h.coverUrl, "${historyChapterLabel(h.lastChapterNumber)} · ${relativeTime(h.lastReadAt, now)}") { onComicClick(h.source, h.comicSlug) }
        }
    } else {
        if (bookmarks.isEmpty()) EmptyLine("Belum ada bookmark")
        bookmarks.take(30).forEach { b ->
            ComicLine(b.comicTitle, b.coverUrl, listOfNotNull(b.type?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }, b.status?.takeIf { it.isNotBlank() }).joinToString(" · ")) { onComicClick(b.source, b.comicSlug) }
        }
    }
    Spacer(Modifier.height(20.dp))
    Text("AKUN", fontSize = 11.sp, letterSpacing = 1.sp, color = Ui.Text3, modifier = Modifier.padding(bottom = 4.dp))
    MenuRow("Ganti username", onEdit)
    if (u.role == "MODERATOR" || u.role == "OWNER") MenuRow("Moderasi", onModeration)
    MenuRow("Hapus akun", onDelete, Color(0xFFE08F8F), last = true)
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, lineHeight = 16.sp, color = Ui.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, fontSize = 13.sp, color = Ui.Text3, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), textAlign = TextAlign.Center)
}

@Composable
private fun ComicLine(title: String, coverUrl: String?, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        CoverImage(
            url = coverUrl,
            contentDescription = title,
            widthPx = 140,
            heightPx = 196,
            modifier = Modifier.size(width = 48.dp, height = 66.dp).clip(RoundedCornerShape(4.dp)).background(Ui.Card)
        )
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = Ui.Text2, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit, color: Color = Color.White, last: Boolean = false) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = color, modifier = Modifier.weight(1f))
        Icon(AppIcons.chevronRight, null, tint = Ui.Text3, modifier = Modifier.size(18.dp))
    }
    if (last) Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
}

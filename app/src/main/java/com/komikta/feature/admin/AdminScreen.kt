package com.komikta.feature.admin

import com.komikta.ui.system.stableStatusBarsPadding
import com.komikta.ui.system.stableNavigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.data.remote.dto.AdminCommentDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.feature.community.ModerationViewModel
import com.komikta.feature.community.ReportsPane
import com.komikta.feature.profile.OwnerToolsDialog
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Avatar
import com.komikta.ui.components.Chip
import com.komikta.ui.components.EmptyState
import com.komikta.ui.components.ErrorState
import com.komikta.ui.components.relativeTime
import com.komikta.ui.components.roleName
import com.komikta.ui.components.roleTint
import com.komikta.ui.theme.Ui
import kotlinx.coroutines.delay
import java.util.Locale

private val Danger = Color(0xFFE08F8F)
private val Soft = Color(0xFFC3CBCE)
private val Tabs: List<Pair<String, ImageVector>> by lazy { listOf("Ringkasan" to AppIcons.dashboard, "Pengguna" to AppIcons.users, "Komentar" to AppIcons.comment, "Laporan" to AppIcons.flag) }
private val RoleFilters = listOf("OWNER" to "Owner", "MODERATOR" to "Moderator", "PREMIUM" to "Premium", "USER" to "Member")

private fun num(n: Number) = "%,d".format(Locale.US, n.toLong()).replace(',', '.')
private fun adminError(m: String?) = if (m?.contains("404") == true) "Fitur admin belum tersedia di server. Perbarui server terlebih dahulu." else m ?: "Terjadi kesalahan"

@Composable
fun AdminScreen(onBack: () -> Unit, onOpenProfile: (String) -> Unit, stats: AdminStatsViewModel = hiltViewModel()) {
    val viewer by stats.viewer.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Ui.Bg).stableStatusBarsPadding().stableNavigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(AppIcons.chevronLeft, "Back", tint = Color.White, modifier = Modifier.size(24.dp)) }
            Text("Panel Admin", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
        if (viewer?.role != "OWNER") {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyState(AppIcons.shield, "Khusus Owner", "Panel ini hanya bisa dibuka oleh akun Owner.") }
        } else {
            TabBar(tab) { tab = it }
            when (tab) {
                0 -> DashboardPane(stats, { tab = it }, Modifier.weight(1f))
                1 -> UsersPane(onOpenProfile, Modifier.weight(1f))
                2 -> CommentsPane(Modifier.weight(1f))
                else -> ReportsPane(hiltViewModel<ModerationViewModel>(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TabBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Tabs.forEachIndexed { i, (label, icon) ->
                val on = i == selected
                Column(Modifier.weight(1f).clickable { onSelect(i) }.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = if (on) Color.White else Ui.Text3, modifier = Modifier.size(21.dp))
                    Text(label, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Color.White else Ui.Text3, maxLines = 1, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
                    Box(Modifier.fillMaxWidth(0.6f).height(2.dp).background(if (on) Ui.Accent else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.Line))
    }
}

private data class Stat(val label: String, val value: String, val onClick: (() -> Unit)? = null)

@Composable
private fun Label(text: String) = Text(text.uppercase(), fontSize = 11.sp, letterSpacing = 1.sp, color = Ui.Text3, modifier = Modifier.padding(top = 8.dp, start = 2.dp))

@Composable
private fun Tiles(vararg items: Stat) {
    items.toList().chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { s ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Ui.Card).then(if (s.onClick != null) Modifier.clickable(onClick = s.onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(s.label, fontSize = 12.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardPane(vm: AdminStatsViewModel, onGo: (Int) -> Unit, modifier: Modifier) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui.stats
    Box(modifier.fillMaxWidth()) {
        when {
            s == null && ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Memuat…", color = Ui.Text2) }
            s == null -> ErrorState(adminError(ui.error), { vm.load() }, Modifier.align(Alignment.Center))
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ui.Card).clickable { onGo(3) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(AppIcons.flag, null, tint = if (s.openReports > 0) Ui.Accent else Ui.Text2, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (s.openReports > 0) "${num(s.openReports)} laporan menunggu" else "Tidak ada laporan terbuka", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ketuk untuk meninjau", fontSize = 12.sp, color = Ui.Text2)
                    }
                    Icon(AppIcons.chevronRight, null, tint = Ui.Text2, modifier = Modifier.size(18.dp))
                }
                Label("Pengguna")
                Tiles(Stat("Total", num(s.users)) { onGo(1) }, Stat("Baru 7 hari", num(s.newUsers7d)), Stat("Aktif 24 jam", num(s.active24h)), Stat("Diblokir", num(s.banned)) { onGo(1) })
                Label("Role")
                Tiles(Stat("Owner", num(s.owners)), Stat("Moderator", num(s.moderators)), Stat("Premium", num(s.premiums)))
                Label("Komunitas")
                Tiles(Stat("Komentar", num(s.comments)) { onGo(2) }, Stat("Komentar 24 jam", num(s.comments24h)), Stat("Chapter dibaca", num(s.chaptersRead)))
                Text(
                    if (ui.loading) "Memuat…" else "Muat ulang", fontSize = 14.sp, color = Ui.Text2,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp)).clickable(enabled = !ui.loading) { vm.load() }.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, hint: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).background(Ui.Card).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(AppIcons.search, null, tint = Ui.Text2, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = Ui.Text3, maxLines = 1)
            BasicTextField(value, onChange, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), cursorBrush = SolidColor(Color.White), modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Icon(AppIcons.close, "Hapus", tint = Ui.Text2, modifier = Modifier.size(18.dp).clickable { onChange("") })
    }
}

@Composable
private fun Hint(text: String) = Text(text, fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

@Composable
private fun UsersPane(onOpenProfile: (String) -> Unit, modifier: Modifier, vm: AdminUsersViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxWidth()) {
        SearchField(ui.query, "Cari username, nama, atau email", vm::setQuery)
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip("Semua", ui.role.isEmpty() && !ui.banned, { vm.setRole("") }) }
            items(RoleFilters) { (role, label) -> Chip(label, ui.role == role, { vm.setRole(role) }) }
            item { Chip("Diblokir", ui.banned, vm::toggleBanned) }
        }
        if (ui.items.isNotEmpty()) Hint("${num(ui.total.coerceAtLeast(ui.items.size))} pengguna")
        when {
            ui.loading && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Memuat…", color = Ui.Text2) }
            ui.error != null && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { ErrorState(adminError(ui.error), { vm.load() }) }
            ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyState(AppIcons.users, "Tidak ada pengguna", "Coba kata kunci atau filter lain.") }
            else -> LazyColumn(Modifier.weight(1f).padding(top = 4.dp)) {
                items(ui.items, key = { it.id }, contentType = { "user" }) { u -> UserRow(u) { selected = u.id } }
                if (ui.hasMore) item(key = "more") {
                    LaunchedEffect(ui.page) { vm.loadMore() }
                    Text(if (ui.loadingMore) "Memuat…" else "Muat lagi", fontSize = 14.sp, color = Ui.Text2, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
    selected?.let { id ->
        OwnerToolsDialog(id, onDismiss = { selected = null }, onChanged = { vm.load(silent = true) }, onOpenProfile = { selected = null; onOpenProfile(id) })
    }
}

@Composable
private fun UserRow(u: CommunityUserDto, onClick: () -> Unit) {
    val shown = u.name.ifBlank { u.username }
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(u.picture, shown, 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(shown, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${u.username}", fontSize = 13.sp, color = Ui.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(roleName(u.role), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = roleTint(u.role) ?: Ui.Text2)
                Text(if (u.banned) "Diblokir" else "Lv ${u.level}", fontSize = 12.sp, color = if (u.banned) Danger else Ui.Text3)
            }
        }
        Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(Ui.Line))
    }
}

@Composable
private fun CommentsPane(modifier: Modifier, vm: AdminCommentsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<AdminCommentDto?>(null) }
    LaunchedEffect(ui.notice) { if (ui.notice != null) { delay(2600); vm.clearNotice() } }
    Column(modifier.fillMaxWidth()) {
        SearchField(ui.query, "Cari isi komentar atau username", vm::setQuery)
        Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Semua", !ui.reported, { vm.setReported(false) })
            Chip("Dilaporkan", ui.reported, { vm.setReported(true) })
        }
        ui.notice?.let { Text(it, fontSize = 13.sp, color = Soft, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        if (ui.notice == null && ui.items.isNotEmpty()) Hint("${num(ui.total.coerceAtLeast(ui.items.size))} komentar")
        when {
            ui.loading && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Memuat…", color = Ui.Text2) }
            ui.error != null && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { ErrorState(adminError(ui.error), { vm.load() }) }
            ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyState(AppIcons.comment, "Tidak ada komentar", "Coba kata kunci atau filter lain.") }
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ui.items, key = { it.id }, contentType = { "comment" }) { c -> CommentCard(c) { pending = c } }
                if (ui.hasMore) item(key = "more") {
                    LaunchedEffect(ui.page) { vm.loadMore() }
                    Text(if (ui.loadingMore) "Memuat…" else "Muat lagi", fontSize = 14.sp, color = Ui.Text2, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
    pending?.let { c ->
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = Ui.Card,
            title = { Text("Hapus komentar?", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = { Text("Komentar @${c.user?.username ?: "?"} akan dihapus dan tidak bisa dikembalikan.", fontSize = 14.sp, color = Ui.Text2) },
            confirmButton = { TextButton(onClick = { pending = null; vm.remove(c) }) { Text("Hapus", color = Danger) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Batal", color = Ui.Text2) } }
        )
    }
}

@Composable
private fun CommentCard(c: AdminCommentDto, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ui.Card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("@${c.user?.username ?: "?"}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            c.user?.let { if (it.role != "USER") Text(roleName(it.role), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = roleTint(it.role) ?: Ui.Text2) }
            Box(Modifier.weight(1f))
            Text(relativeTime(c.createdAt, System.currentTimeMillis()), fontSize = 12.sp, color = Ui.Text3, maxLines = 1)
        }
        if (c.deleted) Text("Komentar sudah dihapus", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Ui.Text3)
        else Text(c.body, fontSize = 15.sp, lineHeight = 21.sp, color = Soft, maxLines = 6, overflow = TextOverflow.Ellipsis)
        Text(c.targetKey, fontSize = 12.sp, color = Ui.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString { append("${c.likeCount} suka · ${c.replyCount} balasan"); if (c.reportCount > 0) append(" · ${c.reportCount} laporan") },
                fontSize = 12.sp, color = if (c.reportCount > 0) Danger else Ui.Text3, modifier = Modifier.weight(1f)
            )
            if (!c.deleted) Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(AppIcons.trash, null, tint = Danger, modifier = Modifier.size(16.dp))
                Text("Hapus", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Danger)
            }
        }
    }
}

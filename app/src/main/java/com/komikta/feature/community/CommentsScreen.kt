package com.komikta.feature.community

import com.komikta.ui.components.ErrorState
import com.komikta.ui.components.EmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.data.remote.dto.CommentDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.feature.auth.rememberGoogleSignIn
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Avatar
import com.komikta.ui.components.Chip
import com.komikta.ui.components.LevelChip
import com.komikta.ui.components.RoleBadge
import com.komikta.ui.components.SkeletonBlock
import com.komikta.ui.components.relativeTime
import com.komikta.ui.theme.Mono
import kotlinx.coroutines.delay

private val ReportReasons = listOf("spam" to "Spam", "abuse" to "Kasar / pelecehan", "hate" to "SARA / kebencian", "spoiler" to "Spoiler", "other" to "Lainnya")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommentsScreen(onBack: () -> Unit, onUserClick: (String) -> Unit = {}, viewModel: CommentsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val me by viewModel.user.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<CommentDto?>(null) }
    var reportTarget by remember { mutableStateOf<CommentDto?>(null) }
    val signIn = rememberGoogleSignIn(onToken = viewModel::signIn, onError = viewModel::notice)

    LaunchedEffect(ui.notice) {
        if (ui.notice != null) {
            delay(2600)
            viewModel.notice(null)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Mono.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(AppIcons.chevronLeft, "Back", tint = Color.White, modifier = Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) {
                Text("Komentar", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (viewModel.chapterLabel != null) "Chapter ${viewModel.chapterLabel}" else "Halaman komik",
                    fontSize = 13.sp,
                    color = Mono.TextFaint,
                    maxLines = 1
                )
            }
            if (!ui.loading && ui.error == null) Row(
                Modifier.padding(end = 14.dp).clip(CircleShape).background(Mono.CardHigh).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(AppIcons.comment, null, tint = Mono.Icon, modifier = Modifier.size(15.dp))
                Text("${ui.total}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text)
            }
        }
        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Terbaru", selected = ui.sort == "newest", onClick = { viewModel.setSort("newest") })
            Chip("Teratas", selected = ui.sort == "top", onClick = { viewModel.setSort("top") })
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))

        when {
            ui.loading && ui.items.isEmpty() -> Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(5) { SkeletonBlock(Modifier.fillMaxWidth().height(60.dp)) }
            }
            ui.error != null && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ErrorState(ui.error ?: "Terjadi kesalahan", { viewModel.load() })
            }
            ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(AppIcons.comment, "Belum ada komentar", "Jadilah yang pertama menanggapi. Tulis komentarmu di bawah.")
            }
            else -> LazyColumn(Modifier.weight(1f)) {
                ui.items.forEach { root ->
                    item(key = root.id, contentType = "comment") {
                        CommentRow(root, false, me, viewModel, { deleteTarget = it }, { reportTarget = it }, onUserClick)
                    }
                    val rs = ui.replies[root.id]
                    if (root.replyCount > 0) {
                        item(key = "toggle-${root.id}", contentType = "toggle") {
                            val open = rs?.expanded == true
                            Row(
                                Modifier.padding(start = 56.dp, top = 2.dp, bottom = 6.dp).clip(CircleShape)
                                    .clickable { viewModel.toggleReplies(root) }.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(AppIcons.chevronDown, null, tint = Mono.Icon, modifier = Modifier.size(16.dp).rotate(if (open) 180f else 0f))
                                Text(if (open) "Sembunyikan balasan" else "Lihat ${root.replyCount} balasan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Mono.Text)
                            }
                        }
                    }
                    if (rs?.expanded == true) {
                        items(rs.items, key = { it.id }, contentType = { "reply" }) { reply ->
                            CommentRow(reply, true, me, viewModel, { deleteTarget = it }, { reportTarget = it }, onUserClick)
                        }
                        if (rs.loading) item(key = "rl-${root.id}", contentType = "more") { Text("Memuat balasan…", fontSize = 13.sp, color = Mono.TextFaint, modifier = Modifier.padding(start = 62.dp, bottom = 8.dp)) }
                        else if (rs.hasMore && rs.page > 0) item(key = "rm-${root.id}", contentType = "more") {
                            Text("Balasan lainnya", fontSize = 13.sp, color = Mono.TextDim, modifier = Modifier.padding(start = 62.dp, bottom = 10.dp).clickable { viewModel.loadReplies(root.id) })
                        }
                    }
                }
                if (ui.hasMore) item(key = "more", contentType = "more") {
                    LaunchedEffect(ui.page) { viewModel.loadMore() }
                    Text(
                        if (ui.loadingMore) "Memuat…" else "Muat komentar lainnya",
                        fontSize = 14.sp,
                        color = Mono.TextDim,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.loadMore() }.padding(16.dp)
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(Mono.Bar)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
            ui.notice?.let { Text(it, fontSize = 13.sp, color = Mono.TextDim, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) }
            val target = ui.replyingTo
            if (target != null || ui.editing != null) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (ui.editing != null) "Mengedit komentar" else "Membalas @${target?.user?.username}",
                        fontSize = 13.sp,
                        color = Mono.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::cancelCompose) { Icon(AppIcons.close, "Batal", tint = Mono.TextDim, modifier = Modifier.size(18.dp)) }
                }
            }
            if (me == null) {
                Text(
                    "Masuk dengan Google untuk berkomentar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable(onClick = signIn)
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Mono.CardHigh).padding(horizontal = 16.dp, vertical = 11.dp)) {
                        if (draft.isEmpty()) Text("Tulis komentar…", fontSize = 15.sp, color = Mono.TextFaint)
                        BasicTextField(
                            value = draft,
                            onValueChange = viewModel::setDraft,
                            maxLines = 5,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    val canSend = draft.isNotBlank() && !ui.sending
                    IconButton(onClick = viewModel::submit, enabled = canSend) {
                        Icon(AppIcons.send, "Kirim", tint = if (canSend) Mono.Icon else Mono.TextFaint, modifier = Modifier.size(26.dp))
                    }
                }
                if (draft.length > 800) Text("${draft.length}/${CommentsViewModel.MAX_LEN}", fontSize = 12.sp, color = Mono.TextFaint, modifier = Modifier.padding(start = 14.dp, bottom = 6.dp))
            }
        }
    }

    deleteTarget?.let { c ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = Mono.Card,
            title = { Text("Hapus komentar?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Komentar yang dihapus tidak bisa dikembalikan.", color = Mono.TextDim, fontSize = 15.sp) },
            confirmButton = { TextButton(onClick = { viewModel.delete(c); deleteTarget = null }) { Text("Hapus", color = Color(0xFFE08F8F)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal", color = Mono.TextDim) } }
        )
    }

    reportTarget?.let { c ->
        var reason by remember(c.id) { mutableStateOf("spam") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            containerColor = Mono.Card,
            title = { Text("Laporkan komentar", style = MaterialTheme.typography.titleMedium) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportReasons.forEach { (key, label) -> Chip(label, selected = reason == key, onClick = { reason = key }) }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.report(c, reason); reportTarget = null }) { Text("Kirim", color = Color.White) } },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text("Batal", color = Mono.TextDim) } }
        )
    }
}

@Composable
private fun CommentRow(
    c: CommentDto,
    isReply: Boolean,
    me: CommunityUserDto?,
    vm: CommentsViewModel,
    onDelete: (CommentDto) -> Unit,
    onReport: (CommentDto) -> Unit,
    onUserClick: (String) -> Unit
) {
    val meId = me?.id
    // Komentar milik sendiri: foto, nama, role, dan level selalu dari sesi terbaru, bukan dari daftar lama (cache).
    val author = if (me != null && c.user.id == me.id) c.user.copy(name = me.name, username = me.username, picture = me.picture, role = me.role, level = me.level) else c.user
    val avatar: Dp = if (isReply) 36.dp else 46.dp
    var menu by remember { mutableStateOf(false) }
    val canReport = !c.deleted && c.user.id != meId
    val openProfile = { if (c.user.id.isNotBlank()) onUserClick(c.user.id) }
    val time = relativeTime(c.createdAt, System.currentTimeMillis()) + if (c.edited && !c.deleted) " · diedit" else ""

    // Setiap komentar berupa kartu abu gelap; balasan menjorok ke kanan dengan foto lebih kecil.
    Row(
        Modifier.fillMaxWidth()
            .padding(start = if (isReply) 50.dp else 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(14.dp)).background(Mono.Card).padding(12.dp)
    ) {
        Box(Modifier.clip(CircleShape).clickable(onClick = openProfile)) { Avatar(author.picture, author.name.ifBlank { author.username }, avatar) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // Baris 1: username besar + waktu. Baris 2: role dan level.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    author.username, fontSize = if (isReply) 15.sp else 17.sp, fontWeight = FontWeight.Bold, color = Mono.Text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).clickable(onClick = openProfile)
                )
                Text(time, fontSize = 12.sp, color = Mono.TextFaint, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
            Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RoleBadge(author.role)
                LevelChip(author.level)
            }
            if (c.deleted) {
                Text("Komentar telah dihapus", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Mono.TextFaint, modifier = Modifier.padding(top = 8.dp))
            } else {
                val body = remember(c.replyTo, c.body) {
                    buildAnnotatedString {
                        if (c.replyTo != null) {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Mono.TextDim)) { append("@${c.replyTo} ") }
                        }
                        append(c.body)
                    }
                }
                Text(body, fontSize = 15.sp, lineHeight = 22.sp, color = Mono.Text, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.clip(CircleShape).clickable { vm.toggleLike(c) }.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(AppIcons.heart(c.liked), "Suka", tint = Mono.Icon, modifier = Modifier.size(20.dp))
                        if (c.likeCount > 0) Text("${c.likeCount}", fontSize = 13.sp, fontWeight = if (c.liked) FontWeight.Bold else FontWeight.Normal, color = Mono.Text)
                    }
                    Row(
                        Modifier.clip(CircleShape)
                            .clickable { if (meId == null) vm.notice("Masuk dulu untuk membalas") else vm.startReply(c) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(AppIcons.comment, "Balas", tint = Mono.Icon, modifier = Modifier.size(19.dp))
                        Text("Balas", fontSize = 13.sp, color = Mono.Text)
                    }
                    Spacer(Modifier.weight(1f))
                    if (c.canEdit || c.canDelete || canReport) Box {
                        Box(Modifier.size(34.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                            Icon(AppIcons.more, "Opsi", tint = Mono.Icon, modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(Mono.Card)) {
                            if (c.canEdit) DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; vm.startEdit(c) })
                            if (c.canDelete) DropdownMenuItem(text = { Text("Hapus") }, onClick = { menu = false; onDelete(c) })
                            if (canReport) DropdownMenuItem(
                                text = { Text("Laporkan") },
                                onClick = { menu = false; if (meId == null) vm.notice("Masuk dulu untuk melaporkan") else onReport(c) }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.komikta.feature.community

import com.komikta.ui.system.ImmersiveDialogEffect
import com.komikta.ui.system.stableStatusBarsPadding
import com.komikta.ui.system.stableNavigationBarsPadding
import com.komikta.ui.components.ErrorState
import com.komikta.ui.components.EmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.komikta.ui.components.RoleAvatar
import com.komikta.ui.components.RoleBadge
import com.komikta.ui.components.roleCard
import com.komikta.ui.components.NameWithBadge
import com.komikta.ui.components.SkeletonBlock
import com.komikta.ui.components.relativeTime
import com.komikta.ui.theme.Mono
import kotlinx.coroutines.delay

internal val ReportReasons = listOf("spam" to "Spam", "abuse" to "Kasar / pelecehan", "hate" to "SARA / kebencian", "spoiler" to "Spoiler", "other" to "Lainnya")

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
            .stableStatusBarsPadding()
            .stableNavigationBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(AppIcons.chevronLeft, "Back", tint = Color.White, modifier = Modifier.size(26.dp)) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Komentar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(viewModel.chapterLabel?.let { "Chapter $it" }, if (!ui.loading && ui.error == null) "${ui.total} komentar" else null)
                        .joinToString(" · ").ifEmpty { "Halaman komik" },
                    fontSize = 14.sp,
                    color = Mono.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
        CommentsBody(ui, me, viewModel, { deleteTarget = it }, { reportTarget = it }, onUserClick)

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
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoleAvatar(me?.picture, me?.let { it.name.ifBlank { it.username } }.orEmpty(), 34.dp, me?.role ?: "USER")
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Mono.CardHigh).padding(horizontal = 18.dp, vertical = 12.dp)) {
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
                    if (draft.isNotBlank() || ui.sending) IconButton(onClick = viewModel::submit, enabled = canSend) {
                        Icon(AppIcons.send, "Kirim", tint = if (canSend) Mono.Icon else Mono.TextFaint, modifier = Modifier.size(26.dp))
                    }
                }
                if (draft.length > 800) Text("${draft.length}/${CommentsViewModel.MAX_LEN}", fontSize = 12.sp, color = Mono.TextFaint, modifier = Modifier.padding(start = 14.dp, bottom = 6.dp))
            }
        }
    }

    CommentDialogs(viewModel, deleteTarget, { deleteTarget = null }, reportTarget, { reportTarget = null })
}

/** Isi daftar komentar (loading / error / kosong / list). Dipakai layar penuh dan sheet reader. */
@Composable
internal fun ColumnScope.CommentsBody(
    ui: CommentsUiState,
    me: CommunityUserDto?,
    viewModel: CommentsViewModel,
    onDelete: (CommentDto) -> Unit,
    onReport: (CommentDto) -> Unit,
    onUserClick: (String) -> Unit
) {
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
            else -> {
                val roots = remember(ui.items) { ui.items.distinctBy { it.id } }
                LazyColumn(Modifier.weight(1f)) {
                    roots.forEach { root ->
                        item(key = "c-${root.id}", contentType = "comment") {
                            CommentRow(root, false, me, viewModel, onDelete, onReport, onUserClick, repliesOpen = ui.replies[root.id]?.expanded == true)
                        }
                        val rs = ui.replies[root.id]
                        if (rs?.expanded == true) {
                            items(rs.items.distinctBy { it.id }, key = { "r-${it.id}" }, contentType = { "reply" }) { reply ->
                                CommentRow(reply, true, me, viewModel, onDelete, onReport, onUserClick)
                            }
                            if (rs.loading) item(key = "rl-${root.id}", contentType = "more") { Text("Memuat balasan…", fontSize = 13.sp, color = Mono.TextFaint, modifier = Modifier.padding(start = 60.dp, bottom = 8.dp)) }
                            else if (rs.hasMore && rs.page > 0) item(key = "rm-${root.id}", contentType = "more") {
                                Text("Balasan lainnya", fontSize = 13.sp, color = Mono.TextDim, modifier = Modifier.padding(start = 60.dp, bottom = 10.dp).clickable { viewModel.loadReplies(root.id) })
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
        }
}

/** Dialog hapus + laporkan, dipakai layar komentar penuh dan sheet komentar di reader. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommentDialogs(
    viewModel: CommentsViewModel,
    deleteTarget: CommentDto?,
    onDeleteDismiss: () -> Unit,
    reportTarget: CommentDto?,
    onReportDismiss: () -> Unit
) {
    deleteTarget?.let { c ->
        AlertDialog(
            onDismissRequest = { onDeleteDismiss() },
            containerColor = Mono.Card,
            title = { ImmersiveDialogEffect(); Text("Hapus komentar?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("Komentar yang dihapus tidak bisa dikembalikan.", color = Mono.TextDim, fontSize = 15.sp) },
            confirmButton = { TextButton(onClick = { viewModel.delete(c); onDeleteDismiss() }) { Text("Hapus", color = Color(0xFFE08F8F)) } },
            dismissButton = { TextButton(onClick = { onDeleteDismiss() }) { Text("Batal", color = Mono.TextDim) } }
        )
    }

    reportTarget?.let { c ->
        var reason by remember(c.id) { mutableStateOf("spam") }
        AlertDialog(
            onDismissRequest = { onReportDismiss() },
            containerColor = Mono.Card,
            title = { ImmersiveDialogEffect(); Text("Laporkan komentar", style = MaterialTheme.typography.titleMedium) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportReasons.forEach { (key, label) -> Chip(label, selected = reason == key, onClick = { reason = key }) }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.report(c, reason); onReportDismiss() }) { Text("Kirim", color = Color.White) } },
            dismissButton = { TextButton(onClick = { onReportDismiss() }) { Text("Batal", color = Mono.TextDim) } }
        )
    }
}

@Composable
internal fun CommentRow(
    c: CommentDto,
    isReply: Boolean,
    me: CommunityUserDto?,
    vm: CommentsViewModel,
    onDelete: (CommentDto) -> Unit,
    onReport: (CommentDto) -> Unit,
    onUserClick: (String) -> Unit,
    repliesOpen: Boolean = false
) {
    val meId = me?.id
    // Komentar milik sendiri: foto, nama, role, dan level selalu dari sesi terbaru, bukan dari daftar lama (cache).
    val author = if (me != null && c.user.id == me.id) c.user.copy(name = me.name, username = me.username, picture = me.picture, role = me.role, verified = me.verified, level = me.level) else c.user
    val shownName = author.name.ifBlank { author.username }
    val avatar: Dp = if (isReply) 28.dp else 34.dp
    var menu by remember { mutableStateOf(false) }
    val canReport = !c.deleted && c.user.id != meId
    val openProfile = { if (c.user.id.isNotBlank()) onUserClick(c.user.id) }
    val time = relativeTime(c.createdAt, System.currentTimeMillis()) + if (c.edited && !c.deleted) " · diedit" else ""

    // Datar tanpa kartu: foto di kiri; nama + role + waktu, isi, lalu suka / balas / jumlah balasan. Balasan menjorok.
    Row(
        Modifier.fillMaxWidth()
            .padding(start = if (isReply) 44.dp else 8.dp, end = 2.dp, top = 4.dp, bottom = 0.dp)
            .roleCard(author.role)
            .padding(start = 6.dp, end = 2.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Box(Modifier.clip(CircleShape).clickable(onClick = openProfile)) { RoleAvatar(author.picture, shownName, avatar, author.role) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NameWithBadge(
                    shownName, author.role, author.verified, 14.sp, badgeSize = 14.dp,
                    modifier = Modifier.weight(1f, fill = false).clickable(onClick = openProfile)
                )
                RoleBadge(author.role)
                Text("· $time", fontSize = 12.sp, color = Mono.TextFaint, maxLines = 1)
                Spacer(Modifier.weight(1f))
                if (!c.deleted && (c.canEdit || c.canDelete || canReport)) Box {
                    Box(Modifier.size(30.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                        Icon(AppIcons.moreVertical, "Opsi", tint = Mono.TextDim, modifier = Modifier.size(18.dp))
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
            if (c.deleted) {
                Text("Komentar telah dihapus", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = Mono.TextFaint, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            } else {
                val body = remember(c.replyTo, c.body) {
                    buildAnnotatedString {
                        if (c.replyTo != null) {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Mono.TextDim)) { append("@${c.replyTo} ") }
                        }
                        append(c.body)
                    }
                }
                Text(body, fontSize = 14.sp, lineHeight = 20.sp, color = Mono.Text, modifier = Modifier.padding(top = 2.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!c.deleted) {
                    Row(
                        Modifier.clip(CircleShape).clickable { vm.toggleLike(c) }.padding(end = 16.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(AppIcons.heart(c.liked), "Suka", tint = Mono.TextDim, modifier = Modifier.size(18.dp))
                        Text("${c.likeCount}", fontSize = 13.sp, color = Mono.TextDim)
                    }
                    Row(
                        Modifier.clip(CircleShape)
                            .clickable { if (meId == null) vm.notice("Masuk dulu untuk membalas") else vm.startReply(c) }
                            .padding(end = 16.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(AppIcons.reply, "Balas", tint = Mono.TextDim, modifier = Modifier.size(18.dp))
                        Text("Balas", fontSize = 13.sp, color = Mono.TextDim)
                    }
                }
                if (!isReply && c.replyCount > 0) {
                    Text(
                        if (repliesOpen) "Sembunyikan" else "${c.replyCount} balasan",
                        fontSize = 13.sp, color = Mono.TextDim,
                        modifier = Modifier.clip(CircleShape).clickable { vm.toggleReplies(c) }.padding(vertical = 5.dp)
                    )
                }
            }
        }
    }
}

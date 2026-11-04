package com.komikta.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.data.remote.dto.CommentDto
import com.komikta.feature.auth.rememberGoogleSignIn
import com.komikta.feature.community.CommentDialogs
import com.komikta.feature.community.CommentsBody
import com.komikta.feature.community.CommentsViewModel
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.RoleAvatar
import com.komikta.ui.theme.Mono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Komentar untuk SATU chapter, tampil sebagai bottom sheet di atas reader.
 * Awalnya setengah layar; geser/scroll ke atas -> penuh layar. Reader di belakangnya
 * tidak ikut berpindah halaman, jadi posisi baca tetap.
 *
 * Kotak tulis komentar ada di atas daftar (bukan di bawah) supaya selalu terlihat
 * saat sheet masih setengah layar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterCommentsSheet(
    source: String,
    comicSlug: String,
    chapterSlug: String,
    viewModel: CommentsViewModel,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    // Sinkron sebelum state dibaca, supaya tidak sempat tampil komentar chapter sebelumnya.
    remember(source, comicSlug, chapterSlug) { viewModel.bind(source, comicSlug, chapterSlug) }

    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val me by viewModel.user.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var deleteTarget by remember { mutableStateOf<CommentDto?>(null) }
    var reportTarget by remember { mutableStateOf<CommentDto?>(null) }
    val signIn = rememberGoogleSignIn(onToken = viewModel::signIn, onError = viewModel::notice)

    LaunchedEffect(ui.notice) {
        if (ui.notice != null) {
            delay(2600)
            viewModel.notice(null)
        }
    }

    // Balas / edit: buka penuh dan arahkan kursor ke kotak tulis.
    LaunchedEffect(ui.replyingTo?.id, ui.editing?.id) {
        if (ui.replyingTo != null || ui.editing != null) {
            sheetState.expand()
            runCatching { focus.requestFocus() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Mono.Bg,
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Mono.TextFaint) }
    ) {
        // fillMaxHeight: tinggi sheet = layar penuh, posisi awalnya otomatis setengah layar.
        // Saat sheet setengah layar, bagian bawah kolom ini berada di luar layar. Ukur seberapa banyak yang
        // keluar layar, lalu naikkan isi (termasuk kotak tulis) sebanyak itu supaya kotak tulis selalu terlihat.
        val rootHeight = LocalView.current.rootView.height
        var hiddenPx by remember { mutableIntStateOf(0) }
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onGloballyPositioned { c ->
                    val bottom = c.positionInWindow().y + c.size.height
                    val hidden = (bottom - rootHeight).toInt().coerceAtLeast(0)
                    if (kotlin.math.abs(hidden - hiddenPx) > 1) hiddenPx = hidden
                }
                .padding(bottom = with(LocalDensity.current) { hiddenPx.toDp() })
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Komentar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        viewModel.chapterLabel?.let { "Chapter $it" } ?: "Chapter ini",
                        fontSize = 13.sp,
                        color = Mono.TextFaint,
                        maxLines = 1
                    )
                }
                if (!ui.loading && ui.error == null) Row(
                    Modifier.clip(CircleShape).background(Mono.CardHigh).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(AppIcons.comment, null, tint = Mono.Icon, modifier = Modifier.size(15.dp))
                    Text("${ui.total}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text)
                }
                IconButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
                    Icon(AppIcons.close, "Tutup", tint = Mono.TextDim, modifier = Modifier.size(22.dp))
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))

            // Daftar komentar (populer di atas)
            CommentsBody(ui, me, viewModel, { deleteTarget = it }, { reportTarget = it }, onUserClick)

            // Kotak tulis komentar di bawah
            Box(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
            // Kotak tulis komentar (di atas daftar)
            Column(Modifier.fillMaxWidth().background(Mono.Bar).padding(top = 4.dp)) {
                ui.notice?.let {
                    Text(it, fontSize = 13.sp, color = Mono.TextDim, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                val target = ui.replyingTo
                if (target != null || ui.editing != null) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (ui.editing != null) "Mengedit komentar" else "Membalas @${target?.user?.username}",
                            fontSize = 13.sp,
                            color = Mono.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::cancelCompose) {
                            Icon(AppIcons.close, "Batal", tint = Mono.TextDim, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (me == null) {
                    Text(
                        "Masuk dengan Google untuk berkomentar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable(onClick = signIn)
                            .padding(vertical = 12.dp)
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RoleAvatar(me?.picture, me?.let { it.name.ifBlank { it.username } }.orEmpty(), 34.dp, me?.role ?: "USER")
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Mono.CardHigh)
                                .padding(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            if (draft.isEmpty()) Text("Tulis komentar…", fontSize = 15.sp, color = Mono.TextFaint)
                            BasicTextField(
                                value = draft,
                                onValueChange = viewModel::setDraft,
                                maxLines = 5,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 15.sp),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focus)
                                    // Mulai mengetik -> sheet penuh supaya keyboard tidak menutupi daftar.
                                    .onFocusChanged { if (it.isFocused) scope.launch { sheetState.expand() } }
                            )
                        }
                        val canSend = draft.isNotBlank() && !ui.sending
                        IconButton(
                            onClick = viewModel::submit,
                            enabled = canSend,
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(if (canSend) Color.White else Mono.CardHigh)
                        ) {
                            Icon(AppIcons.send, "Kirim", tint = if (canSend) Color.Black else Mono.TextFaint, modifier = Modifier.size(22.dp))
                        }
                    }
                    if (draft.length > 800) {
                        Text(
                            "${draft.length}/${CommentsViewModel.MAX_LEN}",
                            fontSize = 12.sp,
                            color = Mono.TextFaint,
                            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }

    CommentDialogs(viewModel, deleteTarget, { deleteTarget = null }, reportTarget, { reportTarget = null })
}

package com.komikta.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.data.remote.dto.CommentDto
import com.komikta.data.remote.dto.CommentPageDto
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Avatar
import com.komikta.ui.components.LevelChip
import com.komikta.ui.components.RoleBadge
import com.komikta.ui.components.relativeTime
import com.komikta.ui.theme.Mono

@Composable
fun ChapterCommentsSection(
    chapterSlug: String,
    load: suspend (String) -> CommentPageDto?,
    peek: (String) -> CommentPageDto?,
    onOpen: () -> Unit
) {
    var page by remember(chapterSlug) { mutableStateOf(peek(chapterSlug)) }
    var done by remember(chapterSlug) { mutableStateOf(page != null) }
    LaunchedEffect(chapterSlug) {
        load(chapterSlug)?.let { page = it }
        done = true
    }
    val items = remember(page) { page?.items.orEmpty().filter { !it.deleted }.take(3) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
    ) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Mono.Card).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.comment, null, tint = Mono.Icon, modifier = Modifier.size(20.dp))
                Text("Komentar chapter", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(start = 8.dp).weight(1f))
                val total = page?.total ?: 0
                if (total > 0) Text("$total", fontSize = 14.sp, color = Mono.TextDim)
            }
            when {
                !done -> Text("Memuat komentar…", fontSize = 14.sp, color = Mono.TextFaint, modifier = Modifier.padding(top = 14.dp))
                page == null -> Text("Komentar belum bisa dimuat", fontSize = 14.sp, color = Mono.TextFaint, modifier = Modifier.padding(top = 14.dp))
                items.isEmpty() -> Text("Belum ada komentar. Jadilah yang pertama menanggapi chapter ini.", fontSize = 14.sp, lineHeight = 20.sp, color = Mono.TextDim, modifier = Modifier.padding(top = 14.dp))
                else -> Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items.forEach { CommentLine(it, onOpen) }
                }
            }
            Text(
                if ((page?.total ?: 0) > items.size) "Lihat semua komentar" else "Tulis komentar",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable(onClick = onOpen)
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun CommentLine(c: CommentDto, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onOpen).padding(vertical = 10.dp)) {
        Avatar(c.user.picture, c.user.name.ifBlank { c.user.username }, 40.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.user.username, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Mono.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Text(relativeTime(c.createdAt, System.currentTimeMillis()), fontSize = 12.sp, color = Mono.TextFaint, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                RoleBadge(c.user.role)
                LevelChip(c.user.level)
            }
            Text(c.body, fontSize = 14.sp, lineHeight = 20.sp, color = Mono.Text, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            if (c.likeCount > 0 || c.replyCount > 0) {
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (c.likeCount > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(AppIcons.heart(true), null, tint = Mono.Icon, modifier = Modifier.size(15.dp))
                        Text("${c.likeCount}", fontSize = 12.sp, color = Mono.TextDim)
                    }
                    if (c.replyCount > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(AppIcons.comment, null, tint = Mono.Icon, modifier = Modifier.size(15.dp))
                        Text("${c.replyCount} balasan", fontSize = 12.sp, color = Mono.TextDim)
                    }
                }
            }
        }
    }
}

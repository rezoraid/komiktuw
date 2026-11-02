package com.komikta.feature.profile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.data.remote.dto.ActivityItemDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Avatar
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.agoId

/** Warna & ukuran diukur dari screenshot referensi (1080 px lebar ≈ 392 dp). */
internal object P {
    val Header = Color(0xFF242529)
    val Content = Color(0xFF16171C)
    val Shadow = Color(0xFF0D0E12)
    val Capsule = Color(0xFF3D3E40)
    val Blue = Color(0xFF2398F7)
    val Name = Color(0xFFECEBF0)
    val Label = Color(0xFF807F84)
    val TabOn = Color(0xFFFCFDFF)
    val TabOff = Color(0xFFB8BAB9)
    val Title = Color(0xFFEDEDF0)
    val Strong = Color(0xFFD9D9DD)
    val Sub = Color(0xFF9A9BA0)
    val Heart = Color(0xFFE5484D)
}

private val TabLabels = listOf("All", "Comments", "Likes")

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProfilePage(
    state: ProfilePageState,
    fallbackUser: CommunityUserDto?,
    busy: Boolean,
    onTab: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (ActivityItemDto) -> Unit,
    onAvatarClick: (() -> Unit)?,
    onLevelClick: (() -> Unit)?,
    topLeft: @Composable () -> Unit,
    topRight: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val now = remember { System.currentTimeMillis() }
    val tab = state.tabs[state.tab]
    val user = state.stats ?: fallbackUser

    // Pindah tab: jika header sudah tergulung, tahan posisi tepat di bawah tab yang menempel.
    LaunchedEffect(state.tab) { if (listState.firstVisibleItemIndex > 1) listState.scrollToItem(1) }

    // Item: 0 header, 1 tab, 2.. daftar. Muat halaman berikut saat tinggal ±4 baris dari ujung.
    val nearEnd by remember(tab.items.size) {
        derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= tab.items.size - 2 }
    }
    LaunchedEffect(nearEnd, tab.items.size, tab.hasMore, tab.loading, tab.error, state.tab) {
        if (nearEnd && tab.hasMore && !tab.loading && tab.error == null) onLoadMore()
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize().background(P.Content)) {
        item(key = "header", contentType = "header") {
            ProfileHeader(user, state.stats, busy, onAvatarClick, onLevelClick, topLeft, topRight)
        }
        stickyHeader(key = "tabs", contentType = "tabs") { ProfileTabs(state.tab, onTab) }
        item(key = "gap", contentType = "gap") { Spacer(Modifier.height(10.dp)) }
        items(tab.items, key = { it.listKey() }, contentType = { it.kind }) { item ->
            ActivityRow(item, user, now) { onItemClick(item) }
        }
        item(key = "footer", contentType = "footer") {
            Box(Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp), contentAlignment = Alignment.Center) {
                when {
                    state.locked(state.tab) -> FooterText("Aktivitas pengguna ini bersifat privat")
                    tab.loading -> CircularProgressIndicator(Modifier.size(22.dp), color = P.Label, strokeWidth = 2.dp)
                    tab.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FooterText(tab.error)
                        Text("Coba lagi", fontSize = 13.sp, color = P.TabOn, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onRetry).padding(12.dp))
                    }
                    tab.items.isEmpty() && !tab.hasMore -> FooterText(
                        when (state.tab) { 0 -> "Belum ada riwayat baca"; 1 -> "Belum ada komentar"; else -> "Belum ada yang disukai" }
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterText(text: String) {
    Text(text, fontSize = 13.sp, color = P.Label, textAlign = TextAlign.Center)
}

@Composable
private fun ProfileHeader(
    user: CommunityUserDto?,
    stats: CommunityUserDto?,
    busy: Boolean,
    onAvatarClick: (() -> Unit)?,
    onLevelClick: (() -> Unit)?,
    topLeft: @Composable () -> Unit,
    topRight: @Composable () -> Unit
) {
    val name = user?.let { it.name.ifBlank { it.username } }.orEmpty()
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(P.Header)) {
            Column(Modifier.fillMaxWidth().padding(top = 43.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Avatar(user?.picture, name, 116.dp, if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier)
                    if (busy) Box(Modifier.size(116.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
                Text(
                    name, fontSize = 17.sp, color = P.Name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoleCapsule(user?.role ?: "USER")
                    Capsule("Lvl. ${user?.level ?: 1}", onLevelClick)
                    if ((user?.publicNo ?: 0L) > 0L) Capsule("#${user?.publicNo}")
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp, start = 8.dp, end = 8.dp)) {
                    Stat(stats?.readMinutes?.takeIf { it >= 0 }?.toString(), "menit\nmembaca", Modifier.weight(1f))
                    Stat(stats?.commentCount?.takeIf { it >= 0 }?.toString(), "jumlah\nkomentar", Modifier.weight(1f))
                    Stat(stats?.monthsJoined?.takeIf { it >= 0 }?.toString(), "bulan\nbergabung", Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box { topLeft() }
                Box { topRight() }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(P.Shadow))
    }
}

@Composable
private fun Stat(value: String?, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value ?: "—", fontSize = 18.sp, color = P.Name, maxLines = 1)
        Text(label, fontSize = 11.sp, lineHeight = 13.sp, color = P.Label, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Capsule(text: String, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.height(28.dp).clip(CircleShape).background(P.Capsule)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 12.sp, color = Color.White, maxLines = 1) }
}

/** Kapsul hitam "Komikta" + kata peran di atas bidang biru dengan potongan diagonal, seperti referensi. */
@Composable
private fun RoleCapsule(role: String) {
    val word = when (role) { "OWNER" -> "Owner"; "MODERATOR" -> "Moderator"; "PREMIUM" -> "Premium"; else -> "Member" }
    val slant = with(LocalDensity.current) { 8.dp.toPx() }
    Row(Modifier.height(28.dp).clip(CircleShape).background(Color.Black), verticalAlignment = Alignment.CenterVertically) {
        Text("Komikta", fontSize = 12.sp, color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
        Box(
            Modifier.fillMaxHeight().drawBehind {
                val path = Path().apply {
                    moveTo(slant, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                }
                drawPath(path, P.Blue)
            }.padding(start = 14.dp, end = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text(word, fontSize = 12.sp, color = Color.White, maxLines = 1) }
    }
}

@Composable
private fun ProfileTabs(selected: Int, onTab: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).background(P.Content)) {
        TabLabels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) P.TabOn else P.TabOff)
                if (on) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(width = 19.dp, height = 2.dp).clip(CircleShape).background(P.TabOn))
            }
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItemDto, owner: CommunityUserDto?, now: Long, onClick: () -> Unit) {
    val chapter = remember(item.chapter, item.chapterTitle) { chapterLabel(item) }
    val ago = remember(item.at, now) { agoId(item.at, now) }
    val sub = remember(chapter, ago, item.likeCount, item.kind) {
        buildAnnotatedString {
            if (chapter != null) {
                withStyle(SpanStyle(color = P.Strong)) { append(chapter) }
                withStyle(SpanStyle(color = P.Sub)) { append("  •  ") }
            }
            withStyle(SpanStyle(color = P.Sub)) { append(if (item.kind == "like") "disukai $ago" else ago) }
            if (item.kind == "comment" && item.likeCount > 0) withStyle(SpanStyle(color = P.Sub)) { append("  •  ${item.likeCount} suka") }
        }
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Box(Modifier.size(46.dp)) {
            CoverImage(
                url = item.coverUrl.ifBlank { null }, contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)).background(P.Header),
                widthPx = 140, heightPx = 140, alignment = Alignment.TopCenter
            )
            if (owner != null) Avatar(
                owner.picture, owner.name.ifBlank { owner.username }, 22.dp,
                Modifier.align(Alignment.BottomEnd).offset(x = 7.dp, y = 5.dp).border(1.dp, Color(0xFFCFCFD2), CircleShape)
            )
        }
        Column(Modifier.weight(1f).padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.comicTitle, fontSize = 13.sp, color = P.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when (item.kind) {
                "comment" -> Text(item.body, fontSize = 12.sp, lineHeight = 17.sp, color = P.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                "like" -> Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.heart(true), null, tint = P.Heart, modifier = Modifier.size(14.dp))
                    Text(if (item.type == "chapter") "Chapter" else "Komik", fontSize = 11.sp, color = P.Sub, modifier = Modifier.padding(start = 6.dp))
                }
                else -> Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayDot()
                    Text(
                        if (item.readMinutes > 0) "${item.readMinutes} menit membaca" else "Lanjut baca",
                        fontSize = 11.sp, color = P.Sub, modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayDot() {
    Box(
        Modifier.size(16.dp).clip(CircleShape).background(Color.White).drawBehind {
            val w = size.width
            val tri = Path().apply { moveTo(w * 0.40f, w * 0.30f); lineTo(w * 0.72f, w * 0.50f); lineTo(w * 0.40f, w * 0.70f); close() }
            drawPath(tri, Color.Black)
        }
    )
}

private val NumberInSlug = Regex("""\d+(?:[.-]\d+)?""")

/** Label chapter untuk baris aktivitas; null untuk aktivitas tingkat komik. */
internal fun chapterLabel(item: ActivityItemDto): String? {
    val slug = item.chapter ?: return null
    val title = item.chapterTitle.trim()
    if (title.isNotEmpty()) return if (title.all { it.isDigit() || it == '.' }) "Chapter $title" else title
    val n = NumberInSlug.find(slug)?.value
    return if (n != null) "Chapter ${n.replace('-', '.')}" else slug.replace('-', ' ')
}

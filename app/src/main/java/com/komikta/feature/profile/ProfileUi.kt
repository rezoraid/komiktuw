package com.komikta.feature.profile

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.komikta.ui.components.roleName
import com.komikta.ui.components.roleTint
import com.komikta.ui.theme.Mono

/** Warna profil: teks putih, latar abu-hitam, ikon putih (lihat [Mono]). */
internal object P {
    val Content = Mono.Bg
    val Card = Mono.Card
    val CardHigh = Mono.CardHigh
    val Line = Mono.Line
    val Name = Mono.Text
    val Strong = Mono.Text
    val Sub = Mono.TextDim
    val Label = Mono.TextFaint
}

private val TabLabels = listOf("All", "Comments", "Likes")

private fun tabIcon(index: Int): ImageVector = when (index) {
    0 -> AppIcons.eye
    1 -> AppIcons.comment
    else -> AppIcons.heart(false)
}

/**
 * Halaman profil. Dipakai sama persis untuk profil sendiri dan profil orang lain; bedanya hanya
 * aksi di bar atas (pengaturan/logout vs kembali/kelola) dan ketukan pada foto/level.
 */
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

    // Item: 0 header, 1 tab, 2 jarak, 3.. daftar. Muat halaman berikut saat tinggal ±2 baris dari ujung.
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
        item(key = "gap", contentType = "gap") { Spacer(Modifier.height(8.dp)) }
        items(tab.items, key = { it.listKey() }, contentType = { it.kind }) { item ->
            ActivityRow(item, now) { onItemClick(item) }
        }
        item(key = "footer", contentType = "footer") {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), contentAlignment = Alignment.Center) {
                when {
                    tab.loading && !(tab.page == 0 && tab.items.isNotEmpty()) -> CircularProgressIndicator(Modifier.size(22.dp), color = Mono.Icon, strokeWidth = 2.dp)
                    tab.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tab.error, fontSize = 14.sp, color = P.Sub, textAlign = TextAlign.Center)
                        Text(
                            "Coba lagi", fontSize = 14.sp, color = Mono.Text, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp).clip(CircleShape).border(1.dp, P.Line, CircleShape).clickable(onClick = onRetry).padding(horizontal = 18.dp, vertical = 9.dp)
                        )
                    }
                    tab.items.isEmpty() && !tab.hasMore -> when (state.tab) {
                        0 -> ProfileEmpty(
                            AppIcons.eye, "Belum ada riwayat baca",
                            if (state.self) "Komik yang kamu baca akan muncul di sini." else "Pengguna ini belum membaca komik apa pun."
                        )
                        1 -> ProfileEmpty(
                            AppIcons.comment, "Belum ada komentar",
                            if (state.self) "Komentarmu di chapter dan komik akan muncul di sini." else "Pengguna ini belum menulis komentar."
                        )
                        else -> ProfileEmpty(
                            AppIcons.heart(false), "Belum ada yang disukai",
                            if (state.self) "Komik dan chapter yang kamu sukai akan muncul di sini." else "Pengguna ini belum menyukai komik atau chapter."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEmpty(icon: ImageVector, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(P.CardHigh), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mono.Icon, modifier = Modifier.size(28.dp))
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = P.Sub, textAlign = TextAlign.Center)
    }
}

private val AvatarSize = 94.dp
private val AvatarRing = 7.dp

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
    val handle = user?.username.orEmpty()
    Column(Modifier.fillMaxWidth().background(P.Content), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box { topLeft() }
            Box { topRight() }
        }

        // Foto profil dengan cincin putih tipis; ikon kamera hanya di profil sendiri.
        Box(Modifier.size(AvatarSize + AvatarRing * 2)) {
            Box(
                Modifier.fillMaxSize().border(2.dp, Mono.Icon, CircleShape).padding(AvatarRing - 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Avatar(user?.picture, name, AvatarSize, if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier)
                if (busy) Box(Modifier.size(AvatarSize).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
            if (onAvatarClick != null) Box(
                Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(P.CardHigh)
                    .border(2.dp, P.Content, CircleShape).clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) { Icon(AppIcons.camera, "Ganti foto profil", tint = Mono.Icon, modifier = Modifier.size(16.dp)) }
        }

        Text(
            name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = P.Name, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 14.dp, start = 24.dp, end = 24.dp)
        )
        if (handle.isNotBlank()) Text("@$handle", fontSize = 14.sp, color = P.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, start = 24.dp, end = 24.dp))

        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoleCapsule(user?.role ?: "USER")
            Capsule("Lv. ${user?.level ?: 1}", onLevelClick, AppIcons.star)
            if ((user?.publicNo ?: 0L) > 0L) Capsule("#${user?.publicNo}")
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(16.dp)).background(P.Card).padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Stat(stats?.chaptersRead?.takeIf { it >= 0 }?.toString(), "chapter\ndibaca", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(P.Line))
            Stat(stats?.commentCount?.takeIf { it >= 0 }?.toString(), "total\nkomentar", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(32.dp).background(P.Line))
            Stat(stats?.monthsJoined?.takeIf { it >= 0 }?.toString(), "bulan\nbergabung", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(value: String?, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value ?: "—", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Mono.Text, maxLines = 1)
        Text(label, fontSize = 12.sp, lineHeight = 15.sp, color = P.Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Capsule(text: String, onClick: (() -> Unit)? = null, icon: ImageVector? = null) {
    Row(
        Modifier.height(30.dp).clip(CircleShape).background(P.CardHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = Mono.Icon, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, maxLines = 1)
    }
}

/**
 * Kapsul peran. Peran khusus (Owner/Moderator/Premium) berwarna dan punya pendar miring yang lewat
 * dari kiri ke kanan tiap beberapa detik; Member abu netral tanpa efek.
 */
@Composable
private fun RoleCapsule(role: String) {
    val tint = roleTint(role)
    if (tint == null) { Capsule(roleName(role)); return }
    val sweep by rememberInfiniteTransition(label = "roleSweep").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(animation = keyframes { durationMillis = 3600; 1f at 1500 }, repeatMode = RepeatMode.Restart),
        label = "roleSweepValue"
    )
    val slant = with(LocalDensity.current) { 8.dp.toPx() }
    val base = remember(tint) { lerp(Color(0xFF0B0C0F), tint, 0.2f) }
    Box(
        Modifier.height(30.dp).clip(CircleShape).background(base).border(1.dp, tint.copy(alpha = 0.6f), CircleShape)
            .drawBehind {
                val band = size.width * 0.55f
                val cx = -band + (size.width + band * 2f) * sweep
                val path = Path().apply {
                    moveTo(cx - band / 2f + slant, 0f); lineTo(cx + band / 2f + slant, 0f)
                    lineTo(cx + band / 2f - slant, size.height); lineTo(cx - band / 2f - slant, size.height); close()
                }
                drawPath(path, Brush.horizontalGradient(listOf(Color.Transparent, tint.copy(alpha = 0.9f), Color.Transparent), startX = cx - band / 2f, endX = cx + band / 2f))
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(roleName(role), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, maxLines = 1) }
}

@Composable
private fun ProfileTabs(selected: Int, onTab: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(P.Content)) {
        Row(Modifier.fillMaxWidth().height(46.dp)) {
            TabLabels.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(tabIcon(i), null, tint = if (on) Mono.Icon else Mono.TextFaint, modifier = Modifier.size(18.dp))
                        Text(label, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Mono.Text else Mono.TextFaint)
                    }
                    if (on) Box(Modifier.align(Alignment.BottomCenter).size(width = 36.dp, height = 3.dp).clip(CircleShape).background(Mono.Text))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(P.Line))
    }
}

private val CoverW = 60.dp
private val CoverH = 84.dp

/** Satu baris aktivitas sebagai kartu: sampul, judul, chapter, waktu, lalu detail sesuai jenis. */
@Composable
private fun ActivityRow(item: ActivityItemDto, now: Long, onClick: () -> Unit) {
    val chapter = remember(item.chapter, item.chapterTitle) { chapterLabel(item) }
    val ago = remember(item.at, now) { agoId(item.at, now) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp)).background(P.Card).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        CoverImage(
            url = item.coverUrl.ifBlank { null }, contentDescription = null,
            modifier = Modifier.size(CoverW, CoverH).clip(RoundedCornerShape(8.dp)).background(P.CardHigh),
            widthPx = 168, heightPx = 234, alignment = Alignment.TopCenter
        )
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.comicTitle, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            when (item.kind) {
                "comment" -> {
                    Meta(chapter, ago)
                    Text(
                        item.body, fontSize = 13.sp, lineHeight = 18.sp, color = Mono.Text, maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(P.CardHigh).padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    if (item.likeCount > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(AppIcons.heart(true), null, tint = Mono.Icon, modifier = Modifier.size(14.dp))
                        Text("${item.likeCount} suka", fontSize = 12.sp, color = P.Sub)
                    }
                }
                "like" -> {
                    Meta(chapter, "disukai $ago")
                    Row(
                        Modifier.padding(top = 2.dp).clip(CircleShape).background(P.CardHigh).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(AppIcons.heart(true), null, tint = Mono.Icon, modifier = Modifier.size(13.dp))
                        Text(if (item.type == "chapter") "Chapter" else "Komik", fontSize = 12.sp, color = Mono.Text)
                    }
                }
                else -> {
                    Meta(chapter, ago)
                    if (item.chaptersRead > 0) Text("${item.chaptersRead} chapter dibaca", fontSize = 12.sp, color = P.Sub)
                }
            }
        }
        if (item.kind == "history") ContinuePill(Modifier.padding(start = 8.dp).align(Alignment.CenterVertically))
    }
}

/** Baris kecil di bawah judul: chapter (putih) lalu keterangan waktu (abu muda). */
@Composable
private fun Meta(chapter: String?, tail: String) {
    val text = remember(chapter, tail) {
        buildAnnotatedString {
            if (chapter != null) {
                withStyle(SpanStyle(color = Mono.Text, fontWeight = FontWeight.Medium)) { append(chapter) }
                withStyle(SpanStyle(color = Mono.TextFaint)) { append("  •  ") }
            }
            withStyle(SpanStyle(color = Mono.TextDim)) { append(tail) }
        }
    }
    Text(text, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ContinuePill(modifier: Modifier = Modifier) {
    Row(
        modifier.height(32.dp).clip(CircleShape).border(1.dp, Color(0xFF4A4A50), CircleShape).padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayDot()
        Text("Lanjut", fontSize = 12.sp, color = Mono.Text, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
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

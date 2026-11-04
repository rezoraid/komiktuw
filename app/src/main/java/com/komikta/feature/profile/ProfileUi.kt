package com.komikta.feature.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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
import com.komikta.ui.components.RoleAvatar
import com.komikta.ui.components.RoleName
import com.komikta.ui.components.RoleAura
import com.komikta.ui.components.FxRoleBadge
import com.komikta.ui.components.roleName
import com.komikta.ui.components.roleTint
import com.komikta.ui.theme.Mono

/** Warna profil: teks putih, latar abu-hitam, ikon putih (lihat [Mono]). */
internal object P {
    val Header = Mono.Header
    val Content = Mono.Content
    val Shadow = Color(0xFF0D0E12)
    val Capsule = Color(0xFF3D3E40)
    val Name = Mono.Text
    val Strong = Mono.Text
    val Sub = Mono.TextDim
    val Label = Color(0xFF8C8C93)
    val TabOff = Color(0xFFB8BAB9)
}

private val ShimmerColors = listOf(Color(0xFF2C2D31), Color(0xFF4A4B50), Color(0xFF2C2D31))

@Composable
private fun shimmerX(): State<Float> = rememberInfiniteTransition(label = "profileShimmer").animateFloat(
    initialValue = -500f, targetValue = 1300f,
    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
    label = "profileShimmerX"
)

@Composable
private fun Bone(modifier: Modifier, x: State<Float>, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(6.dp)) {
    Box(modifier.clip(shape).drawBehind { drawRect(Brush.linearGradient(ShimmerColors, Offset(x.value, 0f), Offset(x.value + 500f, 220f))) })
}

/** Baris aktivitas palsu (sampul + dua baris teks) yang tampil selama daftar dimuat. */
@Composable
private fun ActivitySkeleton(rows: Int) {
    val x = shimmerX()
    Column(Modifier.fillMaxWidth()) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Bone(Modifier.size(CoverW, CoverH), x)
                Column(Modifier.weight(1f).padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Bone(Modifier.fillMaxWidth(0.8f).height(16.dp), x)
                    Bone(Modifier.fillMaxWidth(0.5f).height(13.dp), x)
                    Bone(Modifier.fillMaxWidth(0.35f).height(13.dp), x)
                }
            }
        }
    }
}

private val TabLabels = listOf("History", "Comments", "Likes")

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
                    tab.loading -> ActivitySkeleton(if (tab.items.isEmpty()) 4 else 1)
                    tab.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tab.error, fontSize = 14.sp, color = P.Sub, textAlign = TextAlign.Center)
                        Text(
                            "Coba lagi", fontSize = 14.sp, color = Mono.Text, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp).clip(CircleShape).border(1.dp, P.Capsule, CircleShape).clickable(onClick = onRetry).padding(horizontal = 18.dp, vertical = 9.dp)
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
        Box(Modifier.size(64.dp).clip(CircleShape).background(P.Capsule), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mono.Icon, modifier = Modifier.size(28.dp))
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = P.Sub, textAlign = TextAlign.Center)
    }
}

private val AvatarSize = 120.dp

/** Header datar seperti sebelumnya (tanpa banner): bar atas, foto besar, nama, kapsul, statistik. */
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
    val shimmer = if (user == null || busy) shimmerX() else null
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(P.Header)) {
            RoleAura(user?.role, Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box { topLeft() }
                    Box { topRight() }
                }
                Box(contentAlignment = Alignment.Center) {
                    if (user == null) Bone(Modifier.size(AvatarSize), shimmer!!, CircleShape)
                    else RoleAvatar(user.picture, name, AvatarSize, user.role, if (onAvatarClick != null) Modifier.clip(CircleShape).clickable(onClick = onAvatarClick) else Modifier)
                    if (busy) Bone(Modifier.size(AvatarSize), shimmer!!, CircleShape)
                }
                if (user == null) Bone(Modifier.padding(top = 18.dp).size(width = 150.dp, height = 22.dp), shimmer!!)
                else RoleName(
                    name, user.role, 22.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp)
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (user == null) {
                        Bone(Modifier.size(width = 190.dp, height = 34.dp), shimmer!!, CircleShape)
                        Bone(Modifier.size(width = 70.dp, height = 34.dp), shimmer, CircleShape)
                    } else {
                        RoleCapsule(user.role)
                        Capsule("Lvl. ${user.level}", onLevelClick)
                        if (user.publicNo > 0L) Capsule("#${user.publicNo}")
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 20.dp, start = 8.dp, end = 8.dp)) {
                    Stat(stats?.chaptersRead?.takeIf { it >= 0 }?.toString(), "chapter\ndibaca", Modifier.weight(1f))
                    Stat(stats?.commentCount?.takeIf { it >= 0 }?.toString(), "jumlah\nkomentar", Modifier.weight(1f))
                    Stat(stats?.monthsJoined?.takeIf { it >= 0 }?.toString(), "bulan\nbergabung", Modifier.weight(1f))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(P.Shadow))
    }
}

@Composable
private fun Stat(value: String?, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (value == null) Bone(Modifier.size(width = 34.dp, height = 24.dp), shimmerX())
        else Text(value, fontSize = 22.sp, color = P.Name, maxLines = 1)
        Text(label, fontSize = 13.sp, lineHeight = 16.sp, color = P.Label, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Capsule(text: String, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.height(34.dp).clip(CircleShape).background(P.Capsule)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 14.sp, color = Color.White, maxLines = 1) }
}

/** Kapsul role: langsung nama role. Role khusus berkilau dan bercahaya, Member abu netral. */
@Composable
private fun RoleCapsule(role: String) {
    val tint = roleTint(role)
    if (tint == null) Capsule(roleName(role)) else FxRoleBadge(role, tint, large = true)
}

@Composable
private fun ProfileTabs(selected: Int, onTab: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).background(P.Content)) {
        TabLabels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Color.White else P.TabOff)
                if (on) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(width = 26.dp, height = 2.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

private val CoverW = 64.dp
private val CoverH = 90.dp

/** Baris aktivitas datar: sampul, judul, chapter • waktu, lalu detail. Tanpa foto profil di sampul. */
@Composable
private fun ActivityRow(item: ActivityItemDto, now: Long, onClick: () -> Unit) {
    val chapter = remember(item.chapter, item.chapterTitle) { chapterLabel(item) }
    val ago = remember(item.at, now) { agoId(item.at, now) }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverImage(
                url = item.coverUrl.ifBlank { null }, contentDescription = null,
                modifier = Modifier.size(CoverW, CoverH).clip(RoundedCornerShape(8.dp)).background(P.Header),
                widthPx = 192, heightPx = 270, alignment = Alignment.TopCenter
            )
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.comicTitle, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when (item.kind) {
                    "comment" -> {
                        Meta(chapter, ago + if (item.likeCount > 0) "  •  ${item.likeCount} suka" else "")
                        Text(item.body, fontSize = 14.sp, lineHeight = 19.sp, color = P.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    "like" -> {
                        Meta(chapter, "disukai $ago")
                        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.heart(true), null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(if (item.type == "chapter") "Chapter" else "Komik", fontSize = 13.sp, color = P.Sub, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    else -> Meta(chapter, ago + if (item.chaptersRead > 0) "  •  ${item.chaptersRead} chapter dibaca" else "")
                }
            }
        }
        Box(Modifier.padding(start = 16.dp + CoverW + 14.dp, end = 16.dp).fillMaxWidth().height(1.dp).background(P.Shadow))
    }
}

/** Baris di bawah judul: chapter (putih) lalu keterangan waktu (abu muda). */
@Composable
private fun Meta(chapter: String?, tail: String) {
    val text = remember(chapter, tail) {
        buildAnnotatedString {
            if (chapter != null) {
                withStyle(SpanStyle(color = Color.White)) { append(chapter) }
                withStyle(SpanStyle(color = P.Label)) { append("  •  ") }
            }
            withStyle(SpanStyle(color = P.Sub)) { append(tail) }
        }
    }
    Text(text, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

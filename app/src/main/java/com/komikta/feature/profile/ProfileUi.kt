package com.komikta.feature.profile

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import com.komikta.BuildConfig
import com.komikta.ui.components.EmptyState
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
    val Chrome = Color(0x59000000)
    val Hairline = Color(0xFF4A4B50)
}

private val TabLabels = listOf("All", "Comments", "Likes")

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProfilePage(
    state: ProfilePageState,
    fallbackUser: CommunityUserDto?,
    busy: Boolean,
    bannerBusy: Boolean,
    onTab: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (ActivityItemDto) -> Unit,
    onAvatarClick: (() -> Unit)?,
    onBannerClick: (() -> Unit)?,
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
            ProfileHeader(user, state.stats, busy, bannerBusy, onAvatarClick, onBannerClick, onLevelClick, topLeft, topRight)
        }
        stickyHeader(key = "tabs", contentType = "tabs") { ProfileTabs(state.tab, onTab) }
        item(key = "gap", contentType = "gap") { Spacer(Modifier.height(10.dp)) }
        items(tab.items, key = { it.listKey() }, contentType = { it.kind }) { item ->
            ActivityRow(item, now) { onItemClick(item) }
        }
        item(key = "footer", contentType = "footer") {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), contentAlignment = Alignment.Center) {
                when {
                    state.locked(state.tab) -> EmptyState(AppIcons.eye, "Aktivitas bersifat privat", "Pengguna ini menyembunyikan aktivitasnya.", compact = true)
                    tab.loading && !(tab.page == 0 && tab.items.isNotEmpty()) -> CircularProgressIndicator(Modifier.size(22.dp), color = P.Label, strokeWidth = 2.dp)
                    tab.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FooterText(tab.error)
                        Text("Coba lagi", fontSize = 14.sp, color = P.TabOn, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onRetry).padding(12.dp))
                    }
                    tab.items.isEmpty() && !tab.hasMore -> when (state.tab) {
                        0 -> EmptyState(AppIcons.eye, "Belum ada riwayat baca", "Komik yang dibaca akan muncul di sini.", compact = true)
                        1 -> EmptyState(AppIcons.comment, "Belum ada komentar", "Komentar di chapter dan komik akan muncul di sini.", compact = true)
                        else -> EmptyState(AppIcons.heart(false), "Belum ada yang disukai", "Komik dan chapter yang disukai akan muncul di sini.", compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterText(text: String) {
    Text(text, fontSize = 14.sp, color = P.Label, textAlign = TextAlign.Center)
}

private val BannerHeight = 150.dp
private val AvatarSize = 104.dp
private val AvatarRing = 3.dp

@Composable
private fun ProfileHeader(
    user: CommunityUserDto?,
    stats: CommunityUserDto?,
    busy: Boolean,
    bannerBusy: Boolean,
    onAvatarClick: (() -> Unit)?,
    onBannerClick: (() -> Unit)?,
    onLevelClick: (() -> Unit)?,
    topLeft: @Composable () -> Unit,
    topRight: @Composable () -> Unit
) {
    val name = user?.let { it.name.ifBlank { it.username } }.orEmpty()
    val bannerUrl = remember(user?.banner) {
        user?.banner?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("/")) BuildConfig.BASE_URL.trimEnd('/') + it else it }
    }
    val ring = AvatarSize + AvatarRing * 2
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(P.Header)) {
            Column(Modifier.fillMaxWidth()) {
                ProfileBanner(bannerUrl, bannerBusy, onBannerClick, topLeft, topRight)
                Column(Modifier.fillMaxWidth().padding(top = ring / 2 + 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        name, fontSize = 18.sp, color = P.Name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoleCapsule(user?.role ?: "USER")
                        Capsule("Lvl. ${user?.level ?: 1}", onLevelClick)
                        if ((user?.publicNo ?: 0L) > 0L) Capsule("#${user?.publicNo}")
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp, start = 8.dp, end = 8.dp)) {
                        Stat(stats?.chaptersRead?.takeIf { it >= 0 }?.toString(), "chapter\ndibaca", Modifier.weight(1f))
                        Stat(stats?.commentCount?.takeIf { it >= 0 }?.toString(), "jumlah\nkomentar", Modifier.weight(1f))
                        Stat(stats?.monthsJoined?.takeIf { it >= 0 }?.toString(), "bulan\nbergabung", Modifier.weight(1f))
                    }
                }
            }
            // Foto profil menimpa tepi bawah banner, dengan cincin warna latar agar terpisah dari gambar.
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = BannerHeight - ring / 2).size(ring)
                    .clip(CircleShape).background(P.Header).padding(AvatarRing),
                contentAlignment = Alignment.Center
            ) {
                Avatar(user?.picture, name, AvatarSize, if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier)
                if (busy) Box(Modifier.size(AvatarSize).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(P.Shadow))
    }
}

@Composable
private fun ProfileBanner(
    url: String?,
    busy: Boolean,
    onClick: (() -> Unit)?,
    topLeft: @Composable () -> Unit,
    topRight: @Composable () -> Unit
) {
    val hasImage = url != null
    Box(
        Modifier.fillMaxWidth().height(BannerHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (url != null) {
            CoverImage(url = url, contentDescription = null, modifier = Modifier.fillMaxSize(), widthPx = 1080, heightPx = 540)
            // Gelapkan sisi atas (tombol) dan lebur sisi bawah ke warna header.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x80000000), 0.5f to Color(0x0D000000), 1f to Color(0x99242529))))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF35373E), P.Header))))
        }
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.clip(CircleShape).then(if (hasImage) Modifier.background(P.Chrome) else Modifier)) { topLeft() }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).then(if (hasImage) Modifier.background(P.Chrome) else Modifier)) { topRight() }
        }
        if (onClick != null) Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 10.dp).size(34.dp).clip(CircleShape).background(P.Chrome),
            contentAlignment = Alignment.Center
        ) { Icon(AppIcons.camera, "Ganti banner", tint = Color.White, modifier = Modifier.size(18.dp)) }
        if (busy) Box(Modifier.fillMaxSize().background(Color(0x80000000)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun Stat(value: String?, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value ?: "—", fontSize = 19.sp, color = P.Name, maxLines = 1)
        Text(label, fontSize = 12.sp, lineHeight = 14.sp, color = P.Label, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Capsule(text: String, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.height(30.dp).clip(CircleShape).background(P.Capsule)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 13.sp, color = Color.White, maxLines = 1) }
}

/**
 * Kapsul peran. Peran khusus (Owner/Moderator/Premium) punya pendar warna miring yang lewat
 * dari kiri ke kanan tiap beberapa detik; Member polos tanpa efek.
 */
@Composable
private fun RoleCapsule(role: String) {
    val tint = when (role) {
        "OWNER" -> P.Blue
        "MODERATOR" -> Color(0xFF2FBF9B)
        "PREMIUM" -> Color(0xFFE0A526)
        else -> null
    }
    if (tint == null) { Capsule("Member"); return }
    val word = when (role) { "OWNER" -> "Owner"; "MODERATOR" -> "Moderator"; else -> "Premium" }
    val sweep by rememberInfiniteTransition(label = "roleSweep").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(animation = keyframes { durationMillis = 3600; 1f at 1500 }, repeatMode = RepeatMode.Restart),
        label = "roleSweepValue"
    )
    val slant = with(LocalDensity.current) { 8.dp.toPx() }
    val base = remember(tint) { lerp(Color(0xFF0B0C0F), tint, 0.2f) }
    Box(
        Modifier.height(30.dp).clip(CircleShape).background(base).border(1.dp, tint.copy(alpha = 0.55f), CircleShape)
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
    ) { Text(word, fontSize = 13.sp, color = Color.White, maxLines = 1) }
}

@Composable
private fun ProfileTabs(selected: Int, onTab: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).background(P.Content)) {
        TabLabels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) P.TabOn else P.TabOff)
                if (on) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).size(width = 22.dp, height = 2.dp).clip(CircleShape).background(P.TabOn))
            }
        }
    }
}

private val CoverW = 56.dp
private val CoverH = 78.dp

@Composable
private fun ActivityRow(item: ActivityItemDto, now: Long, onClick: () -> Unit) {
    val chapter = remember(item.chapter, item.chapterTitle) { chapterLabel(item) }
    val ago = remember(item.at, now) { agoId(item.at, now) }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverImage(
                url = item.coverUrl.ifBlank { null }, contentDescription = null,
                modifier = Modifier.size(CoverW, CoverH).clip(RoundedCornerShape(6.dp)).background(P.Header),
                widthPx = 168, heightPx = 234, alignment = Alignment.TopCenter
            )
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.comicTitle, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, color = P.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when (item.kind) {
                    "comment" -> {
                        Meta(chapter, ago + if (item.likeCount > 0) "  •  ${item.likeCount} suka" else "")
                        Text(item.body, fontSize = 13.sp, lineHeight = 18.sp, color = P.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    "like" -> {
                        Meta(chapter, "disukai $ago")
                        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.heart(true), null, tint = P.Heart, modifier = Modifier.size(15.dp))
                            Text(if (item.type == "chapter") "Chapter" else "Komik", fontSize = 12.sp, color = P.Sub, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    else -> Meta(chapter, ago + if (item.chaptersRead > 0) "  •  ${item.chaptersRead} chapter dibaca" else "")
                }
            }
            if (item.kind == "history") {
                ContinuePill(Modifier.padding(start = 10.dp))
            }
        }
        Box(Modifier.padding(start = 16.dp + CoverW + 14.dp, end = 16.dp).fillMaxWidth().height(1.dp).background(P.Shadow))
    }
}

/** Baris kecil di bawah judul: chapter (lebih terang) lalu keterangan waktu. */
@Composable
private fun Meta(chapter: String?, tail: String) {
    val text = remember(chapter, tail) {
        buildAnnotatedString {
            if (chapter != null) {
                withStyle(SpanStyle(color = P.Strong)) { append(chapter) }
                withStyle(SpanStyle(color = P.Sub)) { append("  •  ") }
            }
            withStyle(SpanStyle(color = P.Sub)) { append(tail) }
        }
    }
    Text(text, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ContinuePill(modifier: Modifier = Modifier) {
    Row(
        modifier.height(32.dp).clip(CircleShape).border(1.dp, P.Hairline, CircleShape).padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayDot()
        Text("Lanjut", fontSize = 12.sp, color = P.Name, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
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

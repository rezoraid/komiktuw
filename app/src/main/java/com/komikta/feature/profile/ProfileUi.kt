package com.komikta.feature.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.data.remote.dto.ActivityItemDto
import com.komikta.data.remote.dto.CommunityUserDto
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Avatar
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.FxRoleBadge
import com.komikta.ui.components.NameWithBadge
import com.komikta.ui.components.RoleAura
import com.komikta.ui.components.RoleAvatar
import com.komikta.ui.components.agoId
import com.komikta.ui.components.roleName
import com.komikta.ui.components.roleTint
import com.komikta.ui.theme.Mono

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
private val BubbleBg = Color(0xFF2A2B33)
private val TabTrack = Color(0xFF212228)

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

@Composable
private fun ActivitySkeleton(rows: Int) {
    val x = shimmerX()
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth()) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(shape).background(CardBg).border(1.dp, CardLine, shape).padding(10.dp)) {
                Bone(Modifier.size(CoverW, CoverH), x, RoundedCornerShape(11.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Bone(Modifier.fillMaxWidth(0.8f).height(15.dp), x)
                    Bone(Modifier.fillMaxWidth(0.45f).height(20.dp), x, CircleShape)
                    Bone(Modifier.fillMaxWidth(0.6f).height(13.dp), x)
                }
            }
        }
    }
}

private val TabLabels = listOf("History", "Comments", "Likes")

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
    val seen = remember { HashSet<String>() }
    val tab = state.tabs[state.tab]
    val user = state.stats ?: fallbackUser
    val scroll: () -> Float = remember(listState) { { if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 100000f } }

    LaunchedEffect(state.tab) { if (listState.firstVisibleItemIndex > 1) listState.scrollToItem(1) }

    val nearEnd by remember(tab.items.size) {
        derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= tab.items.size - 2 }
    }
    LaunchedEffect(nearEnd, tab.items.size, tab.hasMore, tab.loading, tab.error, state.tab) {
        if (nearEnd && tab.hasMore && !tab.loading && tab.error == null) onLoadMore()
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize().background(P.Content)) {
        item(key = "header", contentType = "header") {
            ProfileHeader(user, state.stats, busy, scroll, onAvatarClick, onLevelClick, topLeft, topRight)
        }
        stickyHeader(key = "tabs", contentType = "tabs") { ProfileTabs(state.tab, onTab) }
        item(key = "gap", contentType = "gap") { Spacer(Modifier.height(4.dp)) }
        itemsIndexed(tab.items, key = { _, item -> item.listKey() }, contentType = { _, item -> item.kind }) { index, item ->
            val key = item.listKey()
            val fresh = remember(key) { seen.add(key) }
            ActivityRow(item, now, fresh, index) { onItemClick(item) }
        }
        item(key = "footer", contentType = "footer") {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp), contentAlignment = Alignment.Center) {
                when {
                    tab.loading -> ActivitySkeleton(if (tab.items.isEmpty()) 4 else 1)
                    tab.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tab.error, fontSize = 14.sp, color = P.Sub, textAlign = TextAlign.Center)
                        Text(
                            "Coba lagi", fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp).clip(CircleShape).background(Color.White).clickable(onClick = onRetry).padding(horizontal = 22.dp, vertical = 9.dp)
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
        Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(84.dp).drawBehind {
                drawCircle(Color.White.copy(alpha = 0.28f), size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 7.dp.toPx()))))
            }.padding(12.dp).clip(CircleShape).background(Glass),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Text(subtitle, fontSize = 13.sp, lineHeight = 19.sp, color = P.Sub, textAlign = TextAlign.Center)
    }
}

private val AvatarSize = 120.dp

@Composable
private fun ProfileHeader(
    user: CommunityUserDto?,
    stats: CommunityUserDto?,
    busy: Boolean,
    scroll: () -> Float,
    onAvatarClick: (() -> Unit)?,
    onLevelClick: (() -> Unit)?,
    topLeft: @Composable () -> Unit,
    topRight: @Composable () -> Unit
) {
    val name = user?.let { it.name.ifBlank { it.username } }.orEmpty()
    val shimmer = if (user == null || busy) shimmerX() else null
    val tint = user?.role?.let { roleTint(it) }
    val accent = tint ?: Color.White
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().clipToBounds().background(P.Header)) {
            Box(Modifier.matchParentSize().graphicsLayer { translationY = scroll() * 0.45f }) {
                HalftoneBackdrop(accent, Modifier.matchParentSize(), 0.32f)
                FocusLines(accent, Modifier.matchParentSize(), 0.32f)
            }
            RoleAura(user?.role, Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box { topLeft() }
                    Box { topRight() }
                }
                Column(
                    Modifier.fillMaxWidth().graphicsLayer {
                        val p = (scroll() / (size.height * 0.85f)).coerceIn(0f, 1f)
                        alpha = 1f - p
                        scaleX = 1f - 0.07f * p
                        scaleY = 1f - 0.07f * p
                        translationY = scroll() * 0.2f
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                        if (user == null) Bone(Modifier.size(AvatarSize), shimmer!!, CircleShape)
                        else {
                            val click = if (onAvatarClick != null) Modifier.clip(CircleShape).clickable(onClick = onAvatarClick) else Modifier
                            if (tint == null) InkFrame(AvatarSize, click) { Avatar(user.picture, name, AvatarSize) }
                            else RoleAvatar(user.picture, name, AvatarSize, user.role, click)
                            if (busy) Bone(Modifier.size(AvatarSize), shimmer!!, CircleShape)
                            LevelPill(user.level, onLevelClick, Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                        }
                    }
                    if (user == null) Bone(Modifier.padding(top = 14.dp).size(width = 150.dp, height = 24.dp), shimmer!!)
                    else {
                        NameWithBadge(
                            name, user.role, user.verified, 24.sp, fontWeight = FontWeight.SemiBold, badgeSize = 21.dp,
                            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
                        )
                        if (user.username.isNotBlank()) Text("@${user.username}", fontSize = 13.sp, color = P.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp, start = 24.dp, end = 24.dp))
                    }
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (user == null) {
                            Bone(Modifier.size(width = 110.dp, height = 34.dp), shimmer!!, CircleShape)
                            Bone(Modifier.size(width = 70.dp, height = 34.dp), shimmer, CircleShape)
                        } else {
                            RoleCapsule(user.role)
                            if (user.publicNo > 0L) Capsule("#${user.publicNo}")
                        }
                    }
                    if (user != null && user.role != "OWNER" && user.xpNextLevel > user.xpCurrentLevel) XpBar(user, accent, onLevelClick)
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp, start = 14.dp, end = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(stats?.chaptersRead?.takeIf { it >= 0 }, "chapter\ndibaca", Modifier.weight(1f))
                        StatTile(stats?.commentCount?.takeIf { it >= 0 }, "jumlah\nkomentar", Modifier.weight(1f))
                        StatTile(stats?.monthsJoined?.takeIf { it >= 0 }, "bulan\nbergabung", Modifier.weight(1f))
                    }
                }
            }
        }
        HalftoneEdge(P.Header, P.Content)
    }
}

@Composable
private fun XpBar(user: CommunityUserDto, accent: Color, onClick: (() -> Unit)?) {
    val span = (user.xpNextLevel - user.xpCurrentLevel).coerceAtLeast(1)
    val into = (user.xp - user.xpCurrentLevel).coerceIn(0, span)
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(into, span) { go = true }
    val fill = animateFloatAsState(if (go) into.toFloat() / span else 0f, tween(1100, 150, FastOutSlowInEasing), label = "xpFill")
    val shine = rememberInfiniteTransition(label = "xpShine").animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "xpShineX")
    val brush = remember(accent) { Brush.horizontalGradient(listOf(lerp(accent, Color.Black, 0.12f), lerp(accent, Color.White, 0.55f))) }
    Column(
        Modifier.padding(top = 18.dp, start = 36.dp, end = 36.dp).widthIn(max = 340.dp).fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick) else Modifier)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Menuju Lv ${user.level + 1}", fontSize = 12.sp, color = P.Sub)
            Text("${user.xp} / ${user.xpNextLevel} XP", fontSize = 12.sp, color = P.Sub)
        }
        Box(Modifier.padding(top = 7.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0x2EFFFFFF))) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(fill.value.coerceIn(0f, 1f)).clip(CircleShape).background(brush).drawWithContent {
                    drawContent()
                    val x = shine.value * size.width
                    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent), x - 36.dp.toPx(), x + 36.dp.toPx()))
                }
            )
        }
    }
}

@Composable
private fun StatTile(value: Int?, label: String, modifier: Modifier) {
    var go by remember { mutableStateOf(false) }
    LaunchedEffect(value) { go = value != null }
    val n by animateIntAsState(if (go) value ?: 0 else 0, tween(950, easing = FastOutSlowInEasing), label = "statN")
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(Glass).border(1.dp, GlassLine, shape)
            .drawBehind {
                for (r in 0..2) for (c in 0..(2 - r)) drawCircle(Color.White.copy(alpha = 0.26f), (2.4f - (r + c) * 0.75f).dp.toPx(), Offset(size.width - (10 + c * 9).dp.toPx(), (10 + r * 9).dp.toPx()))
            }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (value == null) Bone(Modifier.size(width = 34.dp, height = 26.dp), shimmerX())
        else Text(n.toString(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
        Text(label, fontSize = 12.sp, lineHeight = 15.sp, color = P.Label, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Capsule(text: String) {
    Box(Modifier.height(34.dp).clip(CircleShape).background(Glass).border(1.dp, GlassLine, CircleShape).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun RoleCapsule(role: String) {
    val tint = roleTint(role)
    if (tint == null) Capsule(roleName(role)) else FxRoleBadge(role, tint, large = true)
}

private val TabIcons by lazy { listOf(AppIcons.eye, AppIcons.comment, AppIcons.heart(false)) }

@Composable
private fun ProfileTabs(selected: Int, onTab: (Int) -> Unit) {
    val trackShape = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().background(P.Content).padding(horizontal = 14.dp, vertical = 10.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(46.dp).clip(trackShape).background(TabTrack).border(1.dp, CardLine, trackShape).padding(4.dp)) {
            val w = maxWidth / 3
            val x = animateDpAsState(w * selected, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow), label = "tabX")
            Box(Modifier.offset { IntOffset(x.value.roundToPx(), 0) }.width(w).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color.White))
            Row(Modifier.fillMaxSize()) {
                TabLabels.forEachIndexed { i, label ->
                    val c by animateColorAsState(if (i == selected) Ink else P.TabOff, tween(180), label = "tabC")
                    Row(
                        Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(TabIcons[i], null, tint = c, modifier = Modifier.size(16.dp))
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

private val CoverW = 64.dp
private val CoverH = 90.dp

@Composable
private fun ActivityRow(item: ActivityItemDto, now: Long, fresh: Boolean, index: Int, onClick: () -> Unit) {
    val chapter = remember(item.chapter, item.chapterTitle) { chapterLabel(item) }
    val ago = remember(item.at, now) { agoId(item.at, now) }
    var shown by remember { mutableStateOf(!fresh) }
    LaunchedEffect(Unit) { shown = true }
    val enter = animateFloatAsState(if (shown) 1f else 0f, tween(420, delayMillis = (index % 6) * 45, easing = FastOutSlowInEasing), label = "rowIn")
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press = animateFloatAsState(if (pressed) 0.975f else 1f, tween(110), label = "rowPress")
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 26.dp.toPx(); scaleX = press.value; scaleY = press.value }
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Row(Modifier.fillMaxWidth().clip(shape).background(CardBg).border(1.dp, CardLine, shape).clickable(interactionSource = src, indication = null, onClick = onClick).padding(10.dp)) {
            val coverShape = RoundedCornerShape(11.dp)
            CoverImage(
                url = item.coverUrl.ifBlank { null }, contentDescription = null,
                modifier = Modifier.size(CoverW, CoverH).clip(coverShape).border(1.5.dp, Color(0x33FFFFFF), coverShape).background(P.Header),
                widthPx = 192, heightPx = 270, alignment = Alignment.TopCenter
            )
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.comicTitle, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when (item.kind) {
                    "comment" -> {
                        MetaRow(chapter, ago)
                        Box(
                            Modifier.padding(top = 5.dp).fillMaxWidth()
                                .drawBehind {
                                    val tail = Path().apply { moveTo(16.dp.toPx(), 0f); lineTo(23.dp.toPx(), -6.dp.toPx()); lineTo(30.dp.toPx(), 0f); close() }
                                    drawPath(tail, BubbleBg)
                                }
                                .clip(RoundedCornerShape(12.dp)).background(BubbleBg).padding(horizontal = 11.dp, vertical = 8.dp)
                        ) { Text(item.body, fontSize = 13.sp, lineHeight = 18.sp, color = P.Strong, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                        if (item.likeCount > 0) IconNote(AppIcons.heart(true), "${item.likeCount} suka")
                    }
                    "like" -> {
                        MetaRow(chapter, "disukai $ago")
                        IconNote(AppIcons.heart(true), if (item.type == "chapter") "Chapter" else "Komik")
                    }
                    else -> {
                        MetaRow(chapter, ago)
                        if (item.chaptersRead > 0) IconNote(AppIcons.eye, "${item.chaptersRead} chapter dibaca")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(chapter: String?, tail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (chapter != null) Text(
            chapter, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).clip(CircleShape).background(Glass).padding(horizontal = 9.dp, vertical = 3.dp)
        )
        Text(tail, fontSize = 12.sp, color = P.Label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = if (chapter != null) 8.dp else 0.dp))
    }
}

@Composable
private fun IconNote(icon: ImageVector, text: String) {
    Row(Modifier.padding(top = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 12.sp, color = P.Sub, modifier = Modifier.padding(start = 6.dp))
    }
}

private val NumberInSlug = Regex("""\d+(?:[.-]\d+)?""")

internal fun chapterLabel(item: ActivityItemDto): String? {
    val slug = item.chapter ?: return null
    val title = item.chapterTitle.trim()
    if (title.isNotEmpty()) return if (title.all { it.isDigit() || it == '.' }) "Chapter $title" else title
    val n = NumberInSlug.find(slug)?.value
    return if (n != null) "Chapter ${n.replace('-', '.')}" else slug.replace('-', ' ')
}

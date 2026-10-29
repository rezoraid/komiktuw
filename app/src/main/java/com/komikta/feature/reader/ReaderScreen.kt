package com.komikta.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.size.Dimension
import coil.size.Size
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.aspectRatio
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.komikta.domain.model.ChapterContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

enum class ScrollMode { Smooth, Swipe }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val autoNext by viewModel.autoNext.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableFloatStateOf(1f) }
    var scrollMode by remember { mutableStateOf(ScrollMode.Smooth) }

    // Auto hide controls
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    val density = LocalDensity.current

    val context = LocalContext.current

    // Fullscreen immersive: sembunyikan status bar + nav bar, konten sampai ke area poni/cutout
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (window != null && controller != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (Build.VERSION.SDK_INT >= 28) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null && controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars())
                if (Build.VERSION.SDK_INT >= 28) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    }
                }
            }
        }
    }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val readerItems = remember(uiState.loaded) { buildReaderItems(uiState.loaded) }
    val ratios = remember { HashMap<String, Float>() }
    val firstSlug = uiState.loaded.firstOrNull()?.chapterSlug

    LaunchedEffect(firstSlug) { if (firstSlug != null) listState.scrollToItem(0) }

    LaunchedEffect(readerItems) {
        snapshotFlow { readerItems.getOrNull(listState.firstVisibleItemIndex)?.chapterSlug }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { viewModel.setCurrent(it) }
    }

    LaunchedEffect(readerItems) {
        var done = -1
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { last ->
            if (last < 0) return@collect
            val end = (last + 4).coerceAtMost(readerItems.lastIndex)
            for (i in maxOf(last + 1, done + 1)..end) {
                (readerItems[i] as? ReaderItem.Page)?.let { context.imageLoader.enqueue(pageRequest(context, it.url, screenWidthPx)) }
            }
            done = maxOf(done, end)
        }
    }

    LaunchedEffect(readerItems, autoNext) {
        if (!autoNext) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { last ->
            if (last >= 0 && last >= readerItems.size - 6) viewModel.appendNext()
        }
    }

    LaunchedEffect(uiState.upcoming?.chapterSlug) {
        uiState.upcoming?.imageUrls?.take(3)?.forEach {
            context.imageLoader.enqueue(pageRequest(context, it, screenWidthPx, diskOnly = true))
        }
    }

    LaunchedEffect(autoScroll, scrollMode, scrollSpeed) {
        if (!autoScroll) return@LaunchedEffect
        if (scrollMode == ScrollMode.Smooth) {
            val pxPerSecond = with(density) { 70.dp.toPx() } * scrollSpeed
            var last = 0L
            while (autoScroll) {
                val dt = withFrameNanos { now ->
                    val d = if (last == 0L) 0L else now - last
                    last = now
                    d
                }
                listState.scrollBy(pxPerSecond * dt / 1_000_000_000f)
                if (!listState.canScrollForward) autoScroll = false
            }
        } else {
            while (autoScroll) {
                delay((2200 / scrollSpeed).toLong().coerceAtLeast(500))
                val distance = listState.layoutInfo.viewportSize.height * 0.7f
                listState.animateScrollBy(
                    distance,
                    tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
                if (!listState.canScrollForward) autoScroll = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            uiState.errorMessage != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage ?: "Error", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadChapter() }) { Text("Retry") }
                    }
                }
            }
            uiState.content != null -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(readerItems, key = { it.key }, contentType = { it::class }) { item ->
                        when (item) {
                            is ReaderItem.Page -> PageImage(item.url, screenWidthPx, ratios)
                            is ReaderItem.Divider -> {
                                val info = uiState.chapterInfo
                                val nextSlug = uiState.loaded.firstOrNull { it.chapterSlug == item.chapterSlug }?.nextChapterSlug
                                ChapterEnd(
                                    current = info[item.chapterSlug],
                                    fallbackTitle = uiState.loaded.firstOrNull { it.chapterSlug == item.chapterSlug }?.title,
                                    nextSlug = nextSlug,
                                    next = nextSlug?.let { info[it] },
                                    onNext = null,
                                    compact = true
                                )
                            }
                            is ReaderItem.End -> {
                                val chapter = uiState.loaded.firstOrNull { it.chapterSlug == item.chapterSlug }
                                val nextSlug = chapter?.nextChapterSlug
                                if (autoNext) {
                                    val info = uiState.chapterInfo
                                    ChapterEnd(
                                        current = info[item.chapterSlug],
                                        fallbackTitle = chapter?.title,
                                        nextSlug = nextSlug,
                                        next = nextSlug?.let { info[it] },
                                        onNext = { viewModel.appendNext() }
                                    )
                                } else {
                                    ChapterNav(
                                        hasPrev = !chapter?.prevChapterSlug.isNullOrBlank(),
                                        hasNext = !nextSlug.isNullOrBlank(),
                                        onPrev = { viewModel.goPrev() },
                                        onNext = { viewModel.goNext() },
                                        onDetail = onBack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top bar (floating)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xF2111111))
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text(
                    text = uiState.content?.title ?: "Reader",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom floating controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val navShape = RoundedCornerShape(30.dp)
            Row(
                Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
                    .clip(navShape)
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color(0xFF3A3A3C), navShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasPrev = !uiState.content?.prevChapterSlug.isNullOrBlank()
                val hasNext = !uiState.content?.nextChapterSlug.isNullOrBlank()
                IconButton(onClick = { viewModel.goPrev() }, enabled = hasPrev) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev", tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.3f))
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                }
                IconButton(
                    onClick = { autoScroll = !autoScroll },
                    modifier = Modifier.clip(CircleShape).background(if (autoScroll) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                ) {
                    Icon(Icons.Default.PlayArrow, if (autoScroll) "Pause" else "Auto scroll", tint = Color.White)
                }
                IconButton(onClick = { viewModel.goNext() }, enabled = hasNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f))
                }
            }
        }

        // Settings sheet
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF171717)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Reading Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(20.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto Scroll", color = Color.White)
                            Text(
                                "Scroll otomatis seperti tangan",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBDBDBD)
                            )
                        }
                        Switch(
                            checked = autoScroll,
                            onCheckedChange = { autoScroll = it }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Auto Next Chapter", color = Color.White)
                            Text(
                                "Lanjut otomatis di akhir chapter",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBDBDBD)
                            )
                        }
                        Switch(checked = autoNext, onCheckedChange = { viewModel.setAutoNext(it) })
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Mode", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScrollModeOption(
                            title = "Halus",
                            subtitle = "Mengalir pelan",
                            selected = scrollMode == ScrollMode.Smooth,
                            modifier = Modifier.weight(1f),
                            onClick = { scrollMode = ScrollMode.Smooth }
                        )
                        ScrollModeOption(
                            title = "Geser",
                            subtitle = "Seperti tangan",
                            selected = scrollMode == ScrollMode.Swipe,
                            modifier = Modifier.weight(1f),
                            onClick = { scrollMode = ScrollMode.Swipe }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Kecepatan: ${"%.1f".format(scrollSpeed)}x", color = Color.White)
                    Slider(
                        value = scrollSpeed,
                        onValueChange = { scrollSpeed = it },
                        valueRange = 0.5f..2.5f,
                        steps = 3
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

sealed interface ReaderItem {
    val key: String
    val chapterSlug: String
    data class Page(override val chapterSlug: String, val index: Int, val url: String) : ReaderItem {
        override val key get() = "$chapterSlug:$index:$url"
    }
    data class Divider(override val chapterSlug: String) : ReaderItem {
        override val key get() = "divider:$chapterSlug"
    }
    data class End(override val chapterSlug: String) : ReaderItem {
        override val key get() = "end:$chapterSlug"
    }
}

private fun buildReaderItems(chapters: List<ChapterContent>): List<ReaderItem> = buildList {
    chapters.forEachIndexed { i, c ->
        c.imageUrls.forEachIndexed { idx, url -> add(ReaderItem.Page(c.chapterSlug, idx, url)) }
        add(if (i < chapters.lastIndex) ReaderItem.Divider(c.chapterSlug) else ReaderItem.End(c.chapterSlug))
    }
}

private fun pageRequest(context: android.content.Context, url: String, widthPx: Int, diskOnly: Boolean = false) =
    ImageRequest.Builder(context)
        .data(url)
        .size(Size(widthPx, Dimension.Undefined))
        .crossfade(false)
        .apply { if (diskOnly) memoryCachePolicy(CachePolicy.DISABLED) }
        .build()

@Composable
private fun PageImage(url: String, widthPx: Int, ratios: HashMap<String, Float>) {
    val context = LocalContext.current
    var retry by remember(url) { mutableStateOf(0) }
    var failed by remember(url) { mutableStateOf(false) }
    var ratio by remember(url) { mutableStateOf(ratios[url]) }
    val request = remember(url, widthPx, retry) { pageRequest(context, url, widthPx) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio ?: 0.7f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { s ->
                val d = s.painter.intrinsicSize
                if (d.width > 0f && d.height > 0f) {
                    val r = d.width / d.height
                    ratios[url] = r
                    ratio = r
                }
                failed = false
            },
            onError = { failed = true }
        )
        if (failed) {
            Text(
                "Gagal memuat. Ketuk untuk ulangi",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8A8A8A),
                modifier = Modifier.clickable { failed = false; retry++ }.padding(16.dp)
            )
        } else if (ratio == null) {
            CircularProgressIndicator(color = Color.White.copy(0.5f), strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ScrollModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color.White else Color(0xFF242424))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(title, color = if (selected) Color.Black else Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(subtitle, color = if (selected) Color(0xFF444444) else Color(0xFF999999), style = MaterialTheme.typography.labelSmall)
    }
}

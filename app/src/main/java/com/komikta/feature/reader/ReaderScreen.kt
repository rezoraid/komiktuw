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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.size.Dimension
import coil.size.Size
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.aspectRatio
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScrollMode { Smooth, Swipe }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
                val images = uiState.content!!.imageUrls
                val context = LocalContext.current
                val screenWidthPx = with(LocalDensity.current) {
                    LocalConfiguration.current.screenWidthDp.dp.roundToPx()
                }
                fun request(url: String) = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size(screenWidthPx, Dimension.Undefined))
                    .crossfade(false)
                    .build()

                LaunchedEffect(images) {
                    snapshotFlow { listState.firstVisibleItemIndex }.collect { first ->
                        val end = (first + 8).coerceAtMost(images.lastIndex)
                        for (i in first..end) {
                            context.imageLoader.enqueue(request(images[i]))
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(images, key = { index, url -> "$index:$url" }) { index, url ->
                        SubcomposeAsyncImage(
                            model = request(url),
                            contentDescription = "Page ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                            loading = {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.7f)
                                        .background(Color(0xFF000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White.copy(0.5f),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        )
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
                    .background(Color.Black.copy(0.75f))
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
            Row(
                Modifier
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF171717).copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev chapter
                IconButton(
                    onClick = { viewModel.goPrev() },
                    enabled = !uiState.content?.prevChapterSlug.isNullOrBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "Prev",
                        tint = if (uiState.content?.prevChapterSlug != null) Color.White else Color(0xFF777777)
                    )
                }

                // Settings
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                }

                // Auto scroll play/pause
                IconButton(onClick = { autoScroll = !autoScroll }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        if (autoScroll) "Pause" else "Auto scroll",
                        tint = if (autoScroll) Color(0xFFFFFFFF) else Color.White
                    )
                }

                // Next chapter
                IconButton(
                    onClick = { viewModel.goNext() },
                    enabled = !uiState.content?.nextChapterSlug.isNullOrBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Next",
                        tint = if (uiState.content?.nextChapterSlug != null) Color.White else Color(0xFF777777)
                    )
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

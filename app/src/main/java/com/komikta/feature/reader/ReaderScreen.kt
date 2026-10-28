package com.komikta.feature.reader

import androidx.compose.animation.AnimatedVisibility
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
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var scrollSpeed by remember { mutableFloatStateOf(1.5f) } // pages-ish

    // Auto hide controls
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Auto scroll
    LaunchedEffect(autoScroll, scrollSpeed) {
        while (autoScroll) {
            delay((80 / scrollSpeed).toLong().coerceAtLeast(16))
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            if (last != null && last.index >= info.totalItemsCount - 1) {
                // reached end
                autoScroll = false
            } else {
                listState.animateScrollToItem(
                    index = (listState.firstVisibleItemIndex),
                    scrollOffset = listState.firstVisibleItemScrollOffset + (4 * scrollSpeed).toInt()
                )
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(images) { index, url ->
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(false)
                                .build(),
                            contentDescription = "Page ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                            loading = {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(400.dp)
                                        .background(Color(0xFF111111)),
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
                    .background(Color(0xFF1A1A1F).copy(alpha = 0.92f))
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
                        tint = if (uiState.content?.prevChapterSlug != null) Color.White else Color.Gray
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
                        tint = if (autoScroll) Color(0xFF8B5CF6) else Color.White
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
                        tint = if (uiState.content?.nextChapterSlug != null) Color.White else Color.Gray
                    )
                }
            }
        }

        // Settings sheet
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF1A1A1F)
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
                                color = Color(0xFFA1A1AA)
                            )
                        }
                        Switch(
                            checked = autoScroll,
                            onCheckedChange = { autoScroll = it }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Kecepatan: ${"%.1f".format(scrollSpeed)}x", color = Color.White)
                    Slider(
                        value = scrollSpeed,
                        onValueChange = { scrollSpeed = it },
                        valueRange = 0.5f..3f,
                        steps = 5
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

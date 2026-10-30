package com.komikta.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.komikta.ui.components.ComicCard
import com.komikta.ui.components.ShimmerBox
import com.komikta.ui.components.ShimmerHost

@Composable
fun ExploreScreen(
    onComicClick: (source: String, slug: String) -> Unit = { _, _ -> },
    focusSearch: Boolean = false,
    onSearchFocused: () -> Unit = {},
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            delay(150)
            runCatching { searchFocus.requestFocus() }
            keyboard?.show()
            onSearchFocused()
        }
    }
    var selectedType by remember { mutableStateOf("All") }
    var showFilter by remember { mutableStateOf(false) }
    val types = listOf("All", "Manhwa", "Manhua", "Manga")
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF000000))) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(FieldBg)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (uiState.query.isEmpty()) {
                    Text("Search manga, manhwa, manhua", color = Color(0xFF999999), fontSize = 15.sp, maxLines = 1)
                }
                BasicTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocus)
                )
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { type ->
                val selected = selectedType == type
                Chip(
                    text = type,
                    selected = selected,
                    onClick = {
                        selectedType = type
                        viewModel.setType(if (type == "All") null else type.lowercase())
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeCount = uiState.selectedGenres.size + listOf(uiState.sort, uiState.status).count { it != null }
            val active = activeCount > 0
            Row(
                Modifier
                    .clip(ChipShape)
                    .background(if (active) Color.White else FieldBg)
                    .clickable { showFilter = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Menu,
                    null,
                    tint = if (active) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (active) "Filters ($activeCount)" else "Filters",
                    color = if (active) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            val quick = (uiState.selectedGenres + uiState.genres.take(6)).toList()
            quick.forEach { genre ->
                val on = genre in uiState.selectedGenres
                Text(
                    genre,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(if (on) Color(0xFF3A3A3A) else FieldBg)
                        .clickable {
                            viewModel.applyFilters(
                                if (on) uiState.selectedGenres - genre else uiState.selectedGenres + genre,
                                uiState.sort,
                                uiState.status
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            uiState.isLoading && uiState.comics.isEmpty() -> ExploreGridLoading()
            uiState.errorMessage != null && uiState.comics.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage ?: "Error", color = Color.White)
                }
            }
            else -> {
                val filtered = uiState.comics

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { "${it.source}:${it.slug}" }, contentType = { "comic" }) { comic ->
                        ComicCard(
                            comic = comic,
                            onClick = { onComicClick(comic.source, comic.slug) }
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFFFFFF),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        GenreFilterSheet(
            genres = uiState.genres,
            selected = uiState.selectedGenres,
            sort = uiState.sort,
            status = uiState.status,
            onApply = { genres, sort, status ->
                viewModel.applyFilters(genres, sort, status)
                showFilter = false
            },
            onDismiss = { showFilter = false }
        )
    }
}


private val FieldBg = Color(0xFF141414)
private val ChipShape = RoundedCornerShape(20.dp)

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.Black else Color.White,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(ChipShape)
            .background(if (selected) Color.White else FieldBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ExploreGridLoading() = ShimmerHost {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(12) {
            Column {
                ShimmerBox(Modifier.fillMaxWidth().aspectRatio(0.68f))
                Spacer(Modifier.height(7.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.92f).height(12.dp))
                Spacer(Modifier.height(5.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.58f).height(9.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GenreFilterSheet(
    genres: List<String>,
    selected: Set<String>,
    sort: String?,
    status: String?,
    onApply: (Set<String>, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(selected) }
    var draftSort by remember { mutableStateOf(sort) }
    var draftStatus by remember { mutableStateOf(status) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF141414),
        contentColor = Color.White
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter komik", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }
            Spacer(Modifier.height(4.dp))

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                SheetLabel("Urutkan")
                OptionRow(
                    options = listOf("Terbaru" to "latest", "Populer" to "popular", "Rating" to "rating", "A-Z" to "az"),
                    selected = draftSort,
                    onSelect = { draftSort = if (draftSort == it) null else it }
                )
                SheetLabel("Status")
                OptionRow(
                    options = listOf("Ongoing" to "ongoing", "Completed" to "completed", "Hiatus" to "hiatus"),
                    selected = draftStatus,
                    onSelect = { draftStatus = if (draftStatus == it) null else it }
                )
                SheetLabel("Genre (pilih satu atau lebih)")
                if (genres.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genres.forEach { genre ->
                            val isSelected = genre in draft
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color.White else Color(0xFF242424))
                                    .clickable {
                                        draft = if (isSelected) draft - genre else draft + genre
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    genre,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        draft = emptySet()
                        draftSort = null
                        draftStatus = null
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF242424),
                        contentColor = Color.White
                    )
                ) { Text("Reset", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { onApply(draft, draftSort, draftStatus) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "Terapkan",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        color = Color(0xFF999999),
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionRow(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            val on = selected == value
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) Color.White else Color(0xFF242424))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    label,
                    color = if (on) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

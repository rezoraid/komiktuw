package com.komikta.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

@Composable
fun ExploreScreen(
    onComicClick: (source: String, slug: String) -> Unit = { _, _ -> },
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Search manga, manhwa, manhua", color = Color(0xFF999999)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFFBDBDBD)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(0xFF141414),
                unfocusedContainerColor = Color(0xFF141414),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFFFFFFF)
            )
        )

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { type ->
                val selected = selectedType == type
                FilterChip(
                    selected = selected,
                    onClick = { selectedType = type },
                    label = { Text(type) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFFFFF),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF141414),
                        labelColor = Color.White
                    ),
                    border = null
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val active = uiState.selectedGenres.isNotEmpty()
            Surface(
                onClick = { showFilter = true },
                shape = RoundedCornerShape(20.dp),
                color = if (active) Color.White else Color(0xFF141414)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                        if (active) "Filters (${uiState.selectedGenres.size})" else "Filters",
                        color = if (active) Color.Black else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            val quick = (uiState.selectedGenres + uiState.genres.take(6)).toList()
            quick.forEach { genre ->
                val on = genre in uiState.selectedGenres
                Surface(
                    onClick = {
                        viewModel.applyGenres(
                            if (on) uiState.selectedGenres - genre else uiState.selectedGenres + genre
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (on) Color(0xFF3A3A3A) else Color(0xFF141414)
                ) {
                    Text(
                        genre,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
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
                val filtered = if (selectedType == "All") uiState.comics
                else uiState.comics.filter { it.type?.contains(selectedType, true) == true }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.slug }) { comic ->
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
            onApply = {
                viewModel.applyGenres(it)
                showFilter = false
            },
            onDismiss = { showFilter = false }
        )
    }
}


@Composable
private fun ExploreGridLoading() {
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
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(selected) }
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
            Spacer(Modifier.height(8.dp))
            Text("Pilih satu atau lebih genre", color = Color(0xFF999999), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
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
                    onClick = { draft = emptySet() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF242424),
                        contentColor = Color.White
                    )
                ) { Text("Reset", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        if (draft.isEmpty()) "Terapkan" else "Terapkan (${draft.size})",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

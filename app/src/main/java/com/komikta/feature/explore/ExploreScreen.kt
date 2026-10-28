package com.komikta.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.komikta.ui.components.HomeShimmerLoading

@Composable
fun ExploreScreen(
    onComicClick: (source: String, slug: String) -> Unit = { _, _ -> },
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedType by remember { mutableStateOf("All") }
    val types = listOf("All", "Manhwa", "Manhua", "Manga")

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0F))
    ) {
        // Search
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Search manga, manhwa, manhua") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF71717A)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(0xFF1E1E24),
                unfocusedContainerColor = Color(0xFF1E1E24),
                cursorColor = Color(0xFF8B5CF6)
            )
        )

        // Type chips
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
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
                        selectedContainerColor = Color(0xFF8B5CF6),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E1E24),
                        labelColor = Color(0xFFA1A1AA)
                    ),
                    border = null
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            uiState.isLoading -> HomeShimmerLoading()
            uiState.errorMessage != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage ?: "Error", color = Color.White)
                }
            }
            uiState.comics.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results", color = Color(0xFF71717A))
                }
            }
            else -> {
                val filtered = if (selectedType == "All") {
                    uiState.comics
                } else {
                    uiState.comics.filter {
                        it.type?.contains(selectedType, ignoreCase = true) == true
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { comic ->
                        ComicCard(
                            comic = comic,
                            onClick = { onComicClick(comic.source, comic.slug) }
                        )
                    }
                }
            }
        }
    }
}

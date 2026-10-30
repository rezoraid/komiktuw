package com.komikta.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.komikta.domain.model.Comic
import com.komikta.ui.components.ComicCard

@Composable
fun LibraryScreen(
    onComicClick: (source: String, slug: String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Bookmarks", "History")

    Column(Modifier.fillMaxSize().background(Color(0xFF000000))) {
        Text(
            "Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
        )
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFFFFFFF),
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[tab]),
                    color = Color(0xFFFFFFFF)
                )
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(
                            title,
                            color = if (tab == i) Color(0xFFFFFFFF) else Color(0xFF999999)
                        )
                    }
                )
            }
        }

        val list = remember(tab, bookmarks, history) {
            if (tab == 0) {
                bookmarks.map {
                    Comic(it.comicTitle, it.comicSlug, it.coverUrl, it.type, it.status, source = it.source)
                }
            } else {
                history.map {
                    Comic(it.comicTitle, it.comicSlug, it.coverUrl, source = it.source)
                }
            }
        }

        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (tab == 0) "Belum ada bookmark" else "Belum ada history",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (tab == 0) "Ketuk ikon bookmark di halaman komik" else "Komik yang kamu baca muncul di sini",
                        color = Color(0xFF777777),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(list, key = { "${it.source}:${it.slug}" }, contentType = { "comic" }) { comic ->
                    ComicCard(comic = comic, onClick = { onComicClick(comic.source, comic.slug) })
                }
            }
        }
    }
}

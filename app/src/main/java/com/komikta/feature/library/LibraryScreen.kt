package com.komikta.feature.library

import com.komikta.ui.components.AppIcons
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.domain.model.Comic
import com.komikta.ui.components.SegmentedTabs
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.komikta.ui.components.ComicCard
import com.komikta.ui.theme.Ui
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun LibraryScreen(
    onComicClick: (source: String, slug: String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(Ui.Bg)) {
        Text("Library", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 14.dp, top = 18.dp, bottom = 14.dp))
        SegmentedTabs(listOf("Bookmarks (${bookmarks.size})", "History (${history.size})"), tab, { tab = it }, Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(14.dp))

        val list = remember(tab, bookmarks, history) {
            if (tab == 0) {
                bookmarks.map { Comic(it.comicTitle, it.comicSlug, it.coverUrl, it.type, it.status, source = it.source) }
            } else {
                history.map { Comic(it.comicTitle, it.comicSlug, it.coverUrl, source = it.source) }
            }
        }

        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        if (tab == 0) AppIcons.bookmark(false) else AppIcons.comment,
                        null,
                        tint = Ui.Accent,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(if (tab == 0) "Belum ada bookmark" else "Belum ada history", fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(if (tab == 0) "Simpan komik favoritmu dari halaman detail" else "Komik yang kamu baca akan muncul di sini", fontSize = 13.sp, color = Ui.Text2)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(list, key = { "${it.source}:${it.slug}" }, contentType = { "comic" }) { comic ->
                    ComicCard(comic, { onComicClick(comic.source, comic.slug) })
                }
            }
        }
    }
}

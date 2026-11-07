package com.komikta.feature.library

import com.komikta.ui.components.EmptyState
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.komikta.core.notify.UpdateNotifier
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
    onExplore: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val notifyOn by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Izin notifikasi ditolak. Aktifkan lewat Pengaturan HP.", Toast.LENGTH_LONG).show()
    }

    Column(Modifier.fillMaxSize().background(Ui.Bg)) {
        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Library", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${bookmarks.size} bookmark · ${history.size} riwayat baca", fontSize = 14.sp, color = Ui.Text2, modifier = Modifier.padding(top = 2.dp))
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable {
                    val next = !notifyOn
                    viewModel.setNotifications(next)
                    if (next && Build.VERSION.SDK_INT >= 33 && !UpdateNotifier.canPost(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    Toast.makeText(context, if (next) "Notifikasi aktif (chapter baru & pengumuman)" else "Notifikasi dimatikan", Toast.LENGTH_SHORT).show()
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.bell(notifyOn), "Notifikasi", tint = if (notifyOn) Ui.Accent else Ui.Text2, modifier = Modifier.size(24.dp))
            }
        }
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
                EmptyState(
                    icon = if (tab == 0) AppIcons.bookmark(false) else AppIcons.eye,
                    title = if (tab == 0) "Belum ada bookmark" else "Belum ada riwayat",
                    subtitle = if (tab == 0) "Simpan komik favoritmu dari halaman detail biar gampang dibaca lagi." else "Komik yang kamu baca akan muncul di sini.",
                    actionLabel = "Jelajahi komik",
                    onAction = onExplore
                )
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

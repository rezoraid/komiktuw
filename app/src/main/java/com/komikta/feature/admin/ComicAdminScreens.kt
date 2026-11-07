package com.komikta.feature.admin

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.data.remote.dto.AdminChapterDto
import com.komikta.data.remote.dto.absoluteUrl
import com.komikta.domain.model.Comic
import com.komikta.ui.components.AppIcons
import com.komikta.ui.components.Chip
import com.komikta.ui.components.CoverImage
import com.komikta.ui.components.EmptyState
import com.komikta.ui.components.ErrorState
import com.komikta.ui.components.fmtViews
import com.komikta.ui.components.statusLabel
import com.komikta.ui.components.typeLabel
import com.komikta.ui.system.stableNavigationBarsPadding
import com.komikta.ui.system.stableStatusBarsPadding
import com.komikta.ui.theme.Ui
import kotlinx.coroutines.delay

private val Dim = Color(0xFFC3CBCE)
private val Warn = Color(0xFFE08F8F)

private fun sourceName(id: String) = when (id) {
    "manta" -> "Manta"
    "shinigami" -> "Shinigami"
    "lumos" -> "Lumos"
    else -> id
}

// ---------------------------------------------------------------------------------------------
// Tab "Komik" di Panel Admin
// ---------------------------------------------------------------------------------------------

@Composable
internal fun ComicsPane(onOpen: (source: String, slug: String) -> Unit, modifier: Modifier, vm: AdminComicsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Column(modifier.fillMaxWidth()) {
        SearchField(ui.query, "Cari komik (judul)", vm::setQuery)
        when {
            ui.loading && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Memuat…", color = Ui.Text2) }
            ui.error != null && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { ErrorState(ui.error.orEmpty(), vm::reload) }
            ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyState(AppIcons.search, "Komik tidak ditemukan", "Coba judul lain.") }
            else -> LazyColumn(Modifier.weight(1f)) {
                items(ui.items, key = { "${it.source}:${it.slug}" }, contentType = { "comic" }) { c -> ComicPickRow(c) { onOpen(c.source, c.slug) } }
            }
        }
    }
}

@Composable
private fun ComicPickRow(c: Comic, onClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverImage(c.coverUrl, c.title, Modifier.size(width = 44.dp, height = 64.dp).clip(RoundedCornerShape(6.dp)).background(Ui.Card), 132, 192)
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val line = listOfNotNull(typeLabel(c.type), statusLabel(c.status), sourceName(c.source)).joinToString(" · ")
                Text(line, fontSize = 12.sp, color = Ui.Text2, maxLines = 1)
            }
            Icon(AppIcons.chevronRight, null, tint = Ui.Text3, modifier = Modifier.size(18.dp))
        }
        Box(Modifier.padding(start = 70.dp).fillMaxWidth().height(1.dp).background(Ui.Line))
    }
}

// ---------------------------------------------------------------------------------------------
// Komponen bersama
// ---------------------------------------------------------------------------------------------

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(AppIcons.chevronLeft, "Kembali", tint = Color.White, modifier = Modifier.size(24.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true, minHeight: Int = 44) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 12.sp, color = Ui.Text3)
        Box(Modifier.fillMaxWidth().heightMin(minHeight).clip(RoundedCornerShape(10.dp)).background(Ui.Card).padding(horizontal = 12.dp, vertical = 11.dp)) {
            BasicTextField(
                value, onChange, singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White), modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun Modifier.heightMin(dp: Int) = this.then(Modifier.defaultMinSize(minHeight = dp.dp))

@Composable
private fun SaveBar(label: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit, leading: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(Ui.Bg).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Text(
            if (busy) "Menyimpan…" else label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) Ui.OnAccent else Ui.Text3,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (enabled) Ui.Accent else Ui.Card)
                .clickable(enabled = enabled, onClick = onClick).padding(vertical = 13.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text, color = Dim) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm, color = Warn) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = Color.White) } },
        containerColor = Ui.Card
    )
}

// ---------------------------------------------------------------------------------------------
// Editor komik
// ---------------------------------------------------------------------------------------------

@Composable
fun ComicEditorScreen(
    onBack: () -> Unit,
    onEditPages: (source: String, slug: String, chapter: String) -> Unit,
    vm: ComicEditorViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var askLeave by remember { mutableStateOf(false) }
    var askReset by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.pickCover(uri) }
    val leave: () -> Unit = { if (ui.dirty) askLeave = true else onBack() }
    BackHandler(onBack = leave)
    LaunchedEffect(ui.notice) { if (ui.notice != null) { delay(3500); vm.clearNotice() } }

    Column(Modifier.fillMaxSize().background(Ui.Bg).stableStatusBarsPadding().stableNavigationBarsPadding()) {
        EditorTopBar("Edit komik", leave)
        val comic = ui.comic
        when {
            ui.loading && comic == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Memuat…", color = Ui.Text2) }
            comic == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ErrorState(if (ui.error?.contains("404") == true) "Fitur edit komik belum tersedia di server. Perbarui server terlebih dahulu." else ui.error ?: "Terjadi kesalahan", vm::load)
            }
            else -> {
                val chapters = remember(comic.chapters, filter) {
                    if (filter.isBlank()) comic.chapters else comic.chapters.filter { chapterLabel(it).contains(filter.trim(), ignoreCase = true) }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                    item(key = "cover") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(width = 96.dp, height = 134.dp).clip(RoundedCornerShape(8.dp)).background(Ui.Card), contentAlignment = Alignment.Center) {
                                CoverImage(absoluteUrl(ui.cover), ui.title, Modifier.fillMaxSize(), 288, 402)
                                if (ui.uploadingCover) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Pilih dari galeri", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ui.OnAccent,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Ui.Accent)
                                        .clickable(enabled = !ui.uploadingCover) { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                                Field("URL cover", ui.cover, vm::setCover)
                            }
                        }
                    }
                    item(key = "fields") {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Field("Judul", ui.title, vm::setTitle)
                            Field("Deskripsi", ui.synopsis, vm::setSynopsis, singleLine = false, minHeight = 110)
                            Field("Genre (pisahkan dengan koma)", ui.genres, vm::setGenres)
                            Field("Penulis", ui.author, vm::setAuthor)
                            Field("Artis", ui.artist, vm::setArtist)
                        }
                    }
                    item(key = "type") {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tipe", fontSize = 12.sp, color = Ui.Text3)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EditableTypes.forEach { (v, label) -> Chip(label, ui.type == v, { vm.setType(v) }) }
                            }
                            Text("Status", fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(top = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(EditableStatuses) { (v, label) -> Chip(label, ui.status == v, { vm.setStatus(v) }) }
                            }
                        }
                    }
                    if (comic.edited) item(key = "reset") {
                        Text(
                            "Kembalikan ke data asli", fontSize = 14.sp, color = Warn,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = !ui.saving) { askReset = true }.padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                    }
                    item(key = "chapters-head") {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Gambar chapter (${comic.chapters.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Pilih chapter untuk mengganti, menghapus, atau mengatur urutan gambar.", fontSize = 12.sp, color = Ui.Text3)
                            if (comic.chapters.size > 8) Field("Cari chapter", filter, { filter = it })
                        }
                    }
                    if (comic.chapters.isEmpty()) item(key = "no-chapters") {
                        Text("Belum ada chapter.", fontSize = 14.sp, color = Ui.Text2, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                    items(chapters, key = { it.slug }, contentType = { "chapter" }) { ch ->
                        Row(Modifier.fillMaxWidth().clickable { onEditPages(vm.source, vm.slug, ch.slug) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(chapterLabel(ch), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (ch.edited) Text("Diedit", fontSize = 12.sp, color = Dim, modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(5.dp)).background(Ui.Card).padding(horizontal = 6.dp, vertical = 2.dp))
                            ch.viewCount?.takeIf { it > 0L }?.let { Text(fmtViews(it.toDouble()), fontSize = 12.sp, color = Ui.Text2, modifier = Modifier.padding(start = 8.dp)) }
                            Icon(AppIcons.chevronRight, null, tint = Ui.Text3, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                        }
                    }
                }
                ui.notice?.let { Text(it, fontSize = 13.sp, color = Dim, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                SaveBar("Simpan perubahan", ui.canSave, ui.saving, vm::save)
            }
        }
    }
    if (askLeave) ConfirmDialog("Buang perubahan?", "Perubahan yang belum disimpan akan hilang.", "Buang", onConfirm = onBack, onDismiss = { askLeave = false })
    if (askReset) ConfirmDialog("Kembalikan ke data asli?", "Judul, cover, deskripsi, genre, dan data lain kembali seperti dari sumber.", "Kembalikan", onConfirm = vm::resetToSource, onDismiss = { askReset = false })
}

private fun chapterLabel(c: AdminChapterDto): String =
    c.title?.takeIf { it.isNotBlank() } ?: c.number?.takeIf { it.isNotBlank() }?.let { "Chapter $it" } ?: c.slug

// ---------------------------------------------------------------------------------------------
// Editor gambar chapter
// ---------------------------------------------------------------------------------------------

@Composable
fun PagesEditorScreen(onBack: () -> Unit, vm: PagesEditorViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var askLeave by remember { mutableStateOf(false) }
    var askReset by remember { mutableStateOf(false) }
    var deleteAt by remember { mutableIntStateOf(-1) }
    var replaceAt by remember { mutableIntStateOf(-1) }
    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && replaceAt >= 0) vm.replace(replaceAt, uri)
        replaceAt = -1
    }
    val addPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { uris -> vm.add(uris) }
    val leave: () -> Unit = { if (ui.dirty) askLeave = true else onBack() }
    BackHandler(onBack = leave)
    LaunchedEffect(ui.notice) { if (ui.notice != null) { delay(3500); vm.clearNotice() } }

    Column(Modifier.fillMaxSize().background(Ui.Bg).stableStatusBarsPadding().stableNavigationBarsPadding()) {
        EditorTopBar("Gambar chapter", leave) {
            if (ui.edited) Text(
                "Asli", fontSize = 14.sp, color = Warn,
                modifier = Modifier.padding(end = 10.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = !ui.saving) { askReset = true }.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        when {
            ui.loading && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Memuat…", color = Ui.Text2) }
            ui.error != null && ui.items.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ErrorState(if (ui.error?.contains("404") == true) "Fitur edit gambar belum tersedia di server. Perbarui server terlebih dahulu." else ui.error.orEmpty(), vm::load)
            }
            else -> {
                Text("${ui.items.size} gambar", fontSize = 12.sp, color = Ui.Text3, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                    itemsIndexed(ui.items, key = { _, page -> page.id }, contentType = { _, _ -> "page" }) { index, item ->
                        PageRow(
                            index = index, item = item, last = index == ui.items.lastIndex,
                            onUp = { vm.move(index, -1) }, onDown = { vm.move(index, +1) },
                            onReplace = { replaceAt = index; replacePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onDelete = { deleteAt = index }
                        )
                    }
                    if (ui.items.isEmpty()) item(key = "empty") { EmptyState(AppIcons.search, "Belum ada gambar", "Tambahkan gambar dengan tombol di bawah.", compact = true) }
                }
            }
        }
        ui.notice?.let { Text(it, fontSize = 13.sp, color = Dim, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        if (ui.items.isNotEmpty() || ui.error == null) SaveBar(
            "Simpan", ui.canSave, ui.saving, vm::save,
            leading = {
                Text(
                    "+ Tambah", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Ui.Card).clickable(enabled = !ui.saving) { addPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                )
            }
        )
    }
    if (deleteAt >= 0) ConfirmDialog("Hapus gambar ${deleteAt + 1}?", "Gambar dihapus dari chapter ini setelah kamu menekan Simpan.", "Hapus", onConfirm = { vm.delete(deleteAt) }, onDismiss = { deleteAt = -1 })
    if (askLeave) ConfirmDialog("Buang perubahan?", "Perubahan yang belum disimpan akan hilang.", "Buang", onConfirm = onBack, onDismiss = { askLeave = false })
    if (askReset) ConfirmDialog("Kembalikan ke gambar asli?", "Urutan dan gambar chapter kembali seperti dari sumber.", "Kembalikan", onConfirm = vm::resetToSource, onDismiss = { askReset = false })
}

@Composable
private fun PageRow(index: Int, item: PageItem, last: Boolean, onUp: () -> Unit, onDown: () -> Unit, onReplace: () -> Unit, onDelete: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ui.Text2, modifier = Modifier.width(30.dp))
            Box(Modifier.size(width = 64.dp, height = 96.dp).clip(RoundedCornerShape(6.dp)).background(Ui.Card), contentAlignment = Alignment.Center) {
                if (item.url.isNotBlank()) CoverImage(absoluteUrl(item.url), null, Modifier.fillMaxSize(), 192, 288, Alignment.TopCenter)
                if (item.busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onUp, enabled = index > 0 && !item.busy, modifier = Modifier.size(42.dp)) {
                    Icon(AppIcons.chevronDown, "Naik", tint = if (index > 0) Color.White else Ui.Text3, modifier = Modifier.size(22.dp).rotate(180f))
                }
                IconButton(onClick = onDown, enabled = !last && !item.busy, modifier = Modifier.size(42.dp)) {
                    Icon(AppIcons.chevronDown, "Turun", tint = if (!last) Color.White else Ui.Text3, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onReplace, enabled = !item.busy, modifier = Modifier.size(42.dp)) {
                    Icon(AppIcons.edit, "Ganti gambar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, enabled = !item.busy, modifier = Modifier.size(42.dp)) {
                    Icon(AppIcons.trash, "Hapus gambar", tint = Warn, modifier = Modifier.size(20.dp))
                }
            }
        }
        Box(Modifier.padding(start = 44.dp).fillMaxWidth().height(1.dp).background(Ui.Line))
    }
}

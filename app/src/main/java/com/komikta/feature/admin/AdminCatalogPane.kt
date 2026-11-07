package com.komikta.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.data.remote.dto.CatalogSourceDto
import com.komikta.ui.components.Chip
import com.komikta.ui.components.ErrorState
import com.komikta.ui.theme.Ui
import kotlinx.coroutines.delay

private val Sources = listOf("manta", "shinigami", "lumos")

private fun fieldLabel(f: String) = when (f) {
    "coverUrl", "cover" -> "cover"
    "rating" -> "rating"
    "viewCount", "views" -> "views"
    "genres" -> "genre"
    "author" -> "author"
    "artist" -> "artist"
    "synopsis" -> "sinopsis"
    "status" -> "status"
    "type" -> "jenis"
    "chapters", "latest" -> "chapter"
    else -> f
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), fontSize = 11.sp, letterSpacing = 1.sp, color = Ui.Text3, modifier = Modifier.padding(start = 2.dp, top = 6.dp))
        content()
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, primary: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1,
        color = if (!enabled) Ui.Text3 else if (primary) Ui.OnAccent else Color.White,
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(if (!enabled) Ui.Card else if (primary) Ui.Accent else Ui.NavPill)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun SourceCard(id: String, s: CatalogSourceDto, busy: Boolean, onSync: () -> Unit) {
    val pct = if (s.records == 0) 0 else (s.complete * 100 / s.records)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ui.Card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sourceLabel(id), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("$pct% lengkap", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (pct >= 90) Color(0xFF7DDBA3) else Ui.Text2)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Ui.Line)) {
            Box(Modifier.fillMaxWidth(pct / 100f).height(6.dp).background(Ui.Accent))
        }
        Text("${s.complete} dari ${s.records} komik sudah lengkap", fontSize = 12.sp, color = Ui.Text2)
        val missing = s.missing.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(4)
        if (missing.isNotEmpty()) Text(
            "Paling banyak kosong: " + missing.joinToString(", ") { "${fieldLabel(it.key)} (${it.value})" },
            fontSize = 12.sp, color = Ui.Text3
        )
        ActionButton("Sinkronkan ${sourceLabel(id)}", !busy, primary = false, modifier = Modifier.fillMaxWidth(), onClick = onSync)
    }
}

/** Panel Katalog: kelengkapan data tiap sumber, sinkron massal, dan sinkron satu komik. */
@Composable
internal fun CatalogPane(modifier: Modifier, vm: AdminCatalogViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { while (true) { delay(15_000); vm.load(silent = true) } }
    LaunchedEffect(ui.notice) { if (ui.notice != null) { delay(3500); vm.clearNotice() } }
    val st = ui.status
    Box(modifier.fillMaxWidth()) {
        when {
            st == null && ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Memuat…", color = Ui.Text2) }
            st == null -> ErrorState(adminError(ui.error), { vm.load() }, Modifier.align(Alignment.Center))
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Katalog data komik", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Server melengkapi cover, rating, genre, author, dan lainnya secara otomatis. Di sini kamu bisa memantau dan memicunya manual.", fontSize = 13.sp, color = Ui.Text2)
                ui.notice?.let { Text(it, fontSize = 13.sp, color = Color(0xFFC3CBCE)) }

                Section("Ringkasan") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Komik tersimpan", st.records.toString(), Modifier.weight(1f))
                        MiniStat("Antrean", st.queued.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Sedang sinkron", st.syncing.toString(), Modifier.weight(1f))
                        MiniStat("Gagal baru-baru ini", st.failedRecently.toString(), Modifier.weight(1f))
                    }
                    Text(
                        buildString {
                            append("Crawler: ").append(if (!st.crawl.enabled) "nonaktif" else if (st.crawl.running) "sedang berjalan" else "aktif, menunggu giliran")
                            append("  ·  AniList: ").append(if (st.external.enabled) "aktif" else "nonaktif")
                        },
                        fontSize = 12.sp, color = Ui.Text3
                    )
                }

                Section("Per sumber") {
                    Sources.forEach { id -> st.bySource[id]?.let { SourceCard(id, it, ui.busy) { vm.syncSource(id) } } }
                    st.bySource.keys.filter { it !in Sources }.forEach { id -> SourceCard(id, st.bySource.getValue(id), ui.busy) { vm.syncSource(id) } }
                    if (st.bySource.isEmpty()) Text("Belum ada data. Jalankan sinkronisasi di bawah.", fontSize = 13.sp, color = Ui.Text3)
                }

                Section("Sinkron massal") {
                    ActionButton(if (ui.busy) "Memproses…" else "Sinkronkan semua sumber", !ui.busy, modifier = Modifier.fillMaxWidth()) { vm.syncSource("") }
                    Text("Berjalan di latar belakang server; angka di atas diperbarui otomatis tiap 15 detik.", fontSize = 12.sp, color = Ui.Text3)
                }

                Section("Sinkron satu komik") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Sources.forEach { id -> Chip(sourceLabel(id), ui.source == id, { vm.setSource(id) }) }
                    }
                    SearchField(ui.slug, "Slug komik, contoh: solo-leveling", vm::setSlug)
                    ActionButton("Sinkronkan komik ini", !ui.busy && ui.slug.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { vm.syncOne() }
                }
                Box(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Ui.Card).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 12.sp, color = Ui.Text2, maxLines = 1)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
    }
}

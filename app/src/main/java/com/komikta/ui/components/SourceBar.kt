package com.komikta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.data.local.SourceChoice
import com.komikta.data.local.SourcePrefs
import com.komikta.ui.theme.Ui

/**
 * Pemilih sumber yang selalu terlihat (Explore & Home): tab sumber + tombol bahasa
 * yang muncul hanya untuk MANGA Plus. Pilihan langsung berlaku, tanpa dialog.
 */
@Composable
fun SourceBar(choice: SourceChoice, onSelect: (source: String, lang: String) -> Unit, modifier: Modifier = Modifier) {
    val sources = SourcePrefs.SOURCES
    var langMenu by remember { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedTabs(
            options = sources.map { it.label },
            selected = sources.indexOfFirst { it.id == choice.source }.coerceAtLeast(0),
            onSelect = { onSelect(sources[it].id, choice.lang) },
            modifier = Modifier.weight(1f)
        )
        if (choice.source == SourcePrefs.MANGAPLUS) {
            Box {
                Row(
                    Modifier.height(43.dp).clip(CircleShape).background(Ui.Card).clickable { langMenu = true }.padding(start = 16.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(SourcePrefs.languageLabel(choice.lang), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1)
                    Icon(AppIcons.chevronDown, "Pilih bahasa", tint = Ui.Text2, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }, modifier = Modifier.background(Ui.Card)) {
                    SourcePrefs.LANGUAGES.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.label, color = if (lang.code == choice.lang) Ui.Accent else Ui.Text2, fontSize = 15.sp, fontWeight = if (lang.code == choice.lang) FontWeight.SemiBold else FontWeight.Normal) },
                            onClick = { langMenu = false; onSelect(choice.source, lang.code) }
                        )
                    }
                }
            }
        }
    }
}

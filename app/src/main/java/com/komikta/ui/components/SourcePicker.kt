package com.komikta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.data.local.SourceChoice
import com.komikta.data.local.SourcePrefs
import com.komikta.ui.system.ImmersiveDialogEffect
import com.komikta.ui.theme.Mono

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourcePickerDialog(current: SourceChoice, onDismiss: () -> Unit, onSelect: (source: String, lang: String) -> Unit) {
    var source by remember { mutableStateOf(current.source) }
    var lang by remember { mutableStateOf(current.lang) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Mono.Card,
        title = { ImmersiveDialogEffect(); Text("Sumber bacaan", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourcePrefs.SOURCES.forEach { Chip(it.label, selected = source == it.id, onClick = { source = it.id }) }
                }
                if (source == SourcePrefs.MANGAPLUS) {
                    Text("Bahasa", color = Mono.TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SourcePrefs.LANGUAGES.forEach { Chip(it.label, selected = lang == it.code, onClick = { lang = it.code }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(source, lang); onDismiss() }) { Text("Terapkan", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = Mono.TextDim) } }
    )
}

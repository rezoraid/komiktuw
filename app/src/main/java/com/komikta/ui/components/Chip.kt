package com.komikta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.ui.theme.Ui

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) Ui.Pill else Color.Transparent, tween(140), label = "chipBg")
    val line by animateColorAsState(if (selected) Ui.Pill else Ui.Outline, tween(140), label = "chipLine")
    Row(
        modifier.height(37.dp).clip(CircleShape).background(bg).border(1.dp, line, CircleShape).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (selected) Icon(AppIcons.check, null, tint = Ui.Bg, modifier = Modifier.size(16.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected) Ui.Bg else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, fill: Boolean = true) {
    Row(modifier.clip(CircleShape).background(Ui.Card).padding(3.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Ui.Bg else Color.Transparent, tween(140), label = "segBg")
            Box(
                (if (fill) Modifier.weight(1f) else Modifier).height(37.dp).clip(CircleShape).background(bg).clickable { onSelect(i) }.padding(horizontal = if (fill) 0.dp else 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Color.White else Ui.Text2, maxLines = 1)
            }
        }
    }
}

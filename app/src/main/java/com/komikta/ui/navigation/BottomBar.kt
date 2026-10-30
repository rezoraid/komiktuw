package com.komikta.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BarBg = Color(0xFF0B0B0B)
private val BarLine = Color(0xFF1E1E1E)
private val Active = Color.White
private val Inactive = Color(0xFF8C8C8C)

@Composable
fun BottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().background(BarBg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(BarLine))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(56.dp)) {
            items.forEach { item ->
                val selected = item.route == currentRoute
                val tint = if (selected) Active else Inactive
                Column(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        item.title,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.title,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

package com.komikta.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.ui.theme.Ui

@Composable
fun BottomBar(items: List<BottomNavItem>, selectedRoute: String?, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Ui.Bar).navigationBarsPadding().height(68.dp)) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            val pill by animateColorAsState(if (selected) Ui.NavPill else Color.Transparent, tween(180), label = "navPill")
            val tint by animateColorAsState(if (selected) Ui.Accent else Ui.Text2, tween(150), label = "navTint")
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(Modifier.size(width = 58.dp, height = 30.dp).clip(CircleShape).background(pill), contentAlignment = Alignment.Center) {
                    Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.title, tint = tint, modifier = Modifier.size(22.dp))
                }
                Text(item.title, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Color.White else Ui.Text2, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

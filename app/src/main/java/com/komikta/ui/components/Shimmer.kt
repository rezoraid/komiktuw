package com.komikta.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1A1F),
            Color(0xFF2A2A32),
            Color(0xFF3A3A45),
            Color(0xFF2A2A32),
            Color(0xFF1A1A1F)
        ),
        start = Offset(translate - 250f, 0f),
        end = Offset(translate, 0f)
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
}

@Composable
fun HomeShimmerLoading() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ShimmerBox(Modifier.width(160.dp).height(28.dp))
        ShimmerBox(Modifier.width(90.dp).height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Column {
                    ShimmerBox(Modifier.width(140.dp).aspectRatio(0.68f))
                    Spacer(Modifier.height(8.dp))
                    ShimmerBox(Modifier.width(120.dp).height(14.dp))
                }
            }
        }
        ShimmerBox(Modifier.width(110.dp).height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(Modifier.weight(1f).aspectRatio(0.68f))
            ShimmerBox(Modifier.weight(1f).aspectRatio(0.68f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(Modifier.weight(1f).aspectRatio(0.68f))
            ShimmerBox(Modifier.weight(1f).aspectRatio(0.68f))
        }
    }
}

@Composable
fun DetailShimmerLoading() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row {
            ShimmerBox(Modifier.width(120.dp).height(170.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerBox(Modifier.fillMaxWidth().height(22.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.55f).height(14.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.4f).height(14.dp))
            }
        }
        ShimmerBox(Modifier.fillMaxWidth().height(70.dp))
        ShimmerBox(Modifier.width(100.dp).height(18.dp))
        repeat(6) { ShimmerBox(Modifier.fillMaxWidth().height(44.dp)) }
    }
}

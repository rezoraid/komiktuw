package com.komikta.ui.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerHost(content: @Composable () -> Unit) = content()

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF171717))
    )
}

@Composable
fun HomeShimmerLoading() = ShimmerHost {
    Column(
        Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(Modifier.fillMaxWidth().height(236.dp))
        HomeShimmerSection()
        HomeShimmerSection()
    }
}

@Composable
private fun HomeShimmerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ShimmerBox(Modifier.padding(horizontal = 16.dp).width(110.dp).height(18.dp))
        Row(
            Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Column(Modifier.weight(1f)) {
                    ShimmerBox(Modifier.fillMaxWidth().aspectRatio(0.75f))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.9f).height(12.dp))
                    Spacer(Modifier.height(4.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.55f).height(10.dp))
                }
            }
        }
    }
}

@Composable
fun DetailShimmerLoading() = ShimmerHost {
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

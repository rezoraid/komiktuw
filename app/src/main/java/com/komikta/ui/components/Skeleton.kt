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
import androidx.compose.ui.unit.dp
import com.komikta.ui.theme.Ui

private val BlockShape = RoundedCornerShape(8.dp)

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier) {
    Box(modifier.background(Ui.Card, BlockShape))
}

@Composable
fun PosterSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(0.72f))
        Spacer(Modifier.height(8.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.9f).height(12.dp))
        Spacer(Modifier.height(5.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.55f).height(10.dp))
    }
}

@Composable
fun PosterRowSkeleton(rows: Int = 2) {
    Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { PosterSkeleton(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun DetailSkeleton() {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row {
            SkeletonBlock(Modifier.width(100.dp).height(140.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBlock(Modifier.fillMaxWidth().height(22.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.55f).height(14.dp))
                SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(14.dp))
            }
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(70.dp))
        repeat(6) { SkeletonBlock(Modifier.fillMaxWidth().height(44.dp)) }
    }
}

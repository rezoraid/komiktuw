package com.komikta.feature.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal val Ink = Color(0xFF111111)
internal val Glass = Color(0x1FFFFFFF)
internal val GlassLine = Color(0x2BFFFFFF)
internal val CardBg = Color(0xFF1D1E24)
internal val CardLine = Color(0xFF2A2B32)

@Composable
internal fun HalftoneBackdrop(color: Color, modifier: Modifier = Modifier, focusY: Float = 0.5f) {
    val t = rememberInfiniteTransition(label = "halftone").animateFloat(0f, 6.2831855f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "halftoneT")
    Box(modifier.drawBehind {
        val step = 13.dp.toPx()
        val maxR = step * 0.47f
        val cx = size.width / 2f
        val cy = size.height * focusY
        val reach = hypot(size.width, size.height) * 0.6f
        val cols = (size.width / step).toInt() + 2
        val rows = (size.height / step).toInt() + 2
        val ph = t.value
        for (j in 0 until rows) for (i in 0 until cols) {
            val x = i * step + if (j % 2 == 0) 0f else step / 2f
            val y = j * step
            val d = (hypot(x - cx, y - cy) / reach).coerceIn(0f, 1f)
            val grow = d * d * (3f - 2f * d)
            val r = maxR * grow * (0.6f + 0.4f * (0.5f + 0.5f * sin(d * 9f - ph)))
            if (r > 0.7f) drawCircle(color.copy(alpha = 0.17f), r, Offset(x, y))
        }
    })
}

@Composable
internal fun FocusLines(color: Color, modifier: Modifier = Modifier, focusY: Float = 0.5f) {
    val rot = rememberInfiniteTransition(label = "focus").animateFloat(0f, 360f, infiniteRepeatable(tween(110000, easing = LinearEasing), RepeatMode.Restart), label = "focusR")
    Box(
        modifier.clipToBounds().drawWithCache {
            val c = Offset(size.width / 2f, size.height * focusY)
            val r0 = 96.dp.toPx()
            val r1 = hypot(size.width, size.height)
            val n = 46
            val brush = Brush.radialGradient(0f to Color.Transparent, (r0 / r1) to color.copy(alpha = 0.02f), 1f to color.copy(alpha = 0.2f), center = c, radius = r1)
            val paths = List(n) { k ->
                val a = k * 6.2831855f / n
                val w = 0.010f + 0.020f * ((k * 7) % 5) / 4f
                Path().apply {
                    moveTo(c.x + cos(a - w * 0.25f) * r0, c.y + sin(a - w * 0.25f) * r0)
                    lineTo(c.x + cos(a - w) * r1, c.y + sin(a - w) * r1)
                    lineTo(c.x + cos(a + w) * r1, c.y + sin(a + w) * r1)
                    lineTo(c.x + cos(a + w * 0.25f) * r0, c.y + sin(a + w * 0.25f) * r0)
                    close()
                }
            }
            onDrawBehind { rotate(rot.value, c) { paths.forEach { drawPath(it, brush) } } }
        }
    )
}

@Composable
internal fun HalftoneEdge(from: Color, to: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(21.dp).background(to).drawBehind {
            val step = 7.dp.toPx()
            val radii = floatArrayOf(3.55f, 2.6f, 1.5f)
            val cols = (size.width / step).toInt() + 2
            for (j in radii.indices) for (i in 0 until cols) drawCircle(from, radii[j].dp.toPx(), Offset(i * step + if (j % 2 == 0) 0f else step / 2f, j * step))
        }
    )
}

@Composable
internal fun InkFrame(size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val rot = rememberInfiniteTransition(label = "ink").animateFloat(0f, 360f, infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Restart), label = "inkR")
    val ring = 3.dp
    val gap = 4.dp
    Box(modifier.size(size + (ring + gap) * 2), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize().drawBehind {
                drawCircle(Color.White.copy(alpha = 0.22f), this.size.minDimension / 2f - ring.toPx() / 2f, center, style = Stroke(1.dp.toPx()))
            }
        )
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { rotationZ = rot.value }
                .drawWithCache {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val sweep = Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.White, Color.Transparent, Color.Transparent), c)
                    onDrawBehind { drawCircle(sweep, this.size.minDimension / 2f - ring.toPx() / 2f, c, style = Stroke(ring.toPx())) }
                }
        )
        content()
    }
}

@Composable
internal fun LevelPill(level: Int, onClick: (() -> Unit)?, modifier: Modifier = Modifier, edge: Color = P.Header) {
    Box(modifier.clip(CircleShape).background(edge).padding(2.5.dp)) {
        Box(
            Modifier.height(26.dp).clip(CircleShape).background(Color.White).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 11.dp),
            contentAlignment = Alignment.Center
        ) { Text("Lv $level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1) }
    }
}

@Composable
internal fun TopCircle(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0x33000000)).border(1.dp, GlassLine, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun TopPill(text: String, onClick: () -> Unit) {
    Box(Modifier.height(42.dp).clip(CircleShape).background(Color(0x33000000)).border(1.dp, GlassLine, CircleShape).clickable(onClick = onClick).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
    }
}

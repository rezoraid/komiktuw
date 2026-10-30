package com.komikta.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.domain.model.Comic

/**
 * Kartu komik ringan: tanpa animasi scale/spring agar scroll & press
 * tidak membebani main thread di perangkat low-end.
 */
@Composable
fun ComicCard(
    comic: Comic,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A))
                .indication(interaction, LocalIndication.current)
        ) {
            // Ukuran decode disesuaikan card (~120–160dp lebar di grid 3 kolom)
            CoverImage(
                url = comic.coverUrl,
                contentDescription = comic.title,
                modifier = Modifier.fillMaxSize(),
                targetWidth = 160.dp,
                targetHeight = 236.dp,
                crossfade = false
            )
            if (!comic.type.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(0.65f)
                ) {
                    Text(
                        text = comic.type!!,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = comic.title,
            color = Color(0xFFFFFFFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        if (!comic.status.isNullOrBlank() || comic.rating != null) {
            Text(
                text = listOfNotNull(
                    comic.rating?.let { "★ $it" },
                    comic.status
                ).joinToString(" · "),
                color = Color(0xFF999999),
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
            )
        }
    }
}

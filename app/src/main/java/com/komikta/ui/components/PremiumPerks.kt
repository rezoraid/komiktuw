package com.komikta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.ui.system.ImmersiveDialogEffect
import com.komikta.ui.system.noSheetInsets
import com.komikta.ui.system.stableNavigationBarsPadding
import com.komikta.ui.theme.Mono

/**
 * Angka keuntungan Premium. XP dihitung di SERVER, jadi angka di sini hanya tampilan dan HARUS sama dengan server
 * (pengali XP untuk role PREMIUM, batas XP harian, dan XP per chapter).
 */
object PremiumPerks {
    const val XP_MULTIPLIER = 2
    const val BASE_CHAPTER_XP = 8
    const val BASE_DAILY_XP = 80
}

/** [soon] = belum aktif, tampil dengan label "Segera". Hapus perk yang tidak jadi dibuat. */
private data class Perk(val icon: ImageVector, val title: String, val desc: String, val soon: Boolean = false)

private val Perks by lazy {
    listOf(
        Perk(
            AppIcons.star, "XP ×${PremiumPerks.XP_MULTIPLIER} lebih cepat",
            "Membaca, berkomentar, dan dapat like memberi XP ${PremiumPerks.XP_MULTIPLIER}× lipat, jadi levelmu naik jauh lebih cepat."
        ),
        Perk(
            AppIcons.chevronsDown, "Batas XP harian ${PremiumPerks.BASE_DAILY_XP * PremiumPerks.XP_MULTIPLIER}",
            "Batas XP per hari naik dari ${PremiumPerks.BASE_DAILY_XP} menjadi ${PremiumPerks.BASE_DAILY_XP * PremiumPerks.XP_MULTIPLIER}, jadi maraton baca tidak terpotong."
        ),
        Perk(
            AppIcons.diamond, "Tampilan eksklusif",
            "Badge Premium, cincin avatar bercahaya, nama bergradien, dan cahaya biru di setiap komentarmu."
        ),
        Perk(AppIcons.edit, "Ganti username lebih leluasa", "Jeda ganti username lebih singkat dibanding Member.", soon = true),
        Perk(AppIcons.eye, "Chapter lebih awal", "Baca chapter terbaru sebelum dirilis untuk semua orang.", soon = true)
    )
}

/** Lembar keuntungan Premium. Member melihat tombol "Jadi Premium"; pengguna Premium melihat status aktif. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSheet(isPremium: Boolean, onSubscribe: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Mono.Bg,
        contentColor = Color.White,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Mono.TextFaint))
            }
        },
        contentWindowInsets = { noSheetInsets() }
    ) {
        ImmersiveDialogEffect()
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).stableNavigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(64.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(RoleFx.PremDeep, RoleFx.PremCore, RoleFx.PremLight))),
                contentAlignment = Alignment.Center
            ) { Icon(AppIcons.diamond, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            Text(
                "Komikta Premium", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            Text(
                "Naik level lebih cepat dan tampil beda di komunitas.", fontSize = 14.sp, lineHeight = 20.sp,
                color = Mono.TextDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Perks.forEach { PerkRow(it) } }
            Spacer(Modifier.height(24.dp))
            if (isPremium) {
                Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.checkCircle, null, tint = RoleFx.PremLight, modifier = Modifier.size(20.dp))
                    Text("Kamu sudah Premium", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RoleFx.PremLight, modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    "Terima kasih sudah mendukung Komikta.", fontSize = 13.sp, color = Mono.TextFaint,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            } else {
                Text(
                    "Jadi Premium", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).clickable(onClick = onSubscribe).padding(vertical = 13.dp)
                )
            }
        }
    }
}

@Composable
private fun PerkRow(perk: Perk) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(RoleFx.PremCore.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(perk.icon, null, tint = RoleFx.PremLight, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(perk.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                if (perk.soon) Text(
                    "Segera", fontSize = 11.sp, color = Mono.TextDim,
                    modifier = Modifier.padding(start = 8.dp).clip(CircleShape).background(Mono.CardHigh).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(perk.desc, fontSize = 13.sp, lineHeight = 18.sp, color = Mono.TextDim, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

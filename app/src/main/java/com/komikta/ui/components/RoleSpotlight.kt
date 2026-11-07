package com.komikta.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp

/**
 * Latar role di belakang komentar, bertema Jepang: Owner matahari merah datar + sapuan enso, Moderator goresan katana,
 * Premium berlian kecil tersebar. Statis dan datar (tanpa blur), jadi tidak membebani render.
 * Member (role biasa) tidak mendapat efek.
 *
 * [centerX] dan [centerY] = pusat avatar, diukur dari pojok kiri-atas baris (termasuk padding).
 */
fun Modifier.roleSpotlight(role: String, centerX: Dp, centerY: Dp): Modifier {
    if (roleTint(role) == null) return this
    return drawBehind { drawRoleBackdrop(role, Offset(centerX.toPx(), centerY.toPx())) }
}

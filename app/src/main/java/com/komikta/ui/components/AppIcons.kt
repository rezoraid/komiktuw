package com.komikta.ui.components

import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.graphics.vector.ImageVector
import com.komikta.ui.navigation.circle
import com.komikta.ui.navigation.navIcon

object AppIcons {
    val chevronLeft: ImageVector by lazy {
        navIcon("ChevronLeft", false, strokeWidth = 2.4f) {
            moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f)
        }
    }

    val chevronRight: ImageVector by lazy {
        navIcon("ChevronRight", false, strokeWidth = 2.4f) {
            moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
        }
    }

    val chevronsDown: ImageVector by lazy {
        navIcon("ChevronsDown", false, strokeWidth = 2.2f) {
            moveTo(6.5f, 6.5f); lineTo(12f, 12f); lineTo(17.5f, 6.5f)
            moveTo(6.5f, 12.5f); lineTo(12f, 18f); lineTo(17.5f, 12.5f)
        }
    }

    val close: ImageVector by lazy {
        navIcon("Close", false, strokeWidth = 2.2f) {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }

    val sort: ImageVector by lazy {
        navIcon("Sort", false, strokeWidth = 2f) {
            moveTo(7f, 4f); lineTo(7f, 20f)
            moveTo(3.5f, 7.5f); lineTo(7f, 4f); lineTo(10.5f, 7.5f)
            moveTo(17f, 20f); lineTo(17f, 4f)
            moveTo(13.5f, 16.5f); lineTo(17f, 20f); lineTo(20.5f, 16.5f)
        }
    }

    val filter: ImageVector by lazy {
        navIcon("Filter", false, strokeWidth = 2.2f) {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(7f, 12f); lineTo(17f, 12f)
            moveTo(10f, 17f); lineTo(14f, 17f)
        }
    }

    val edit: ImageVector by lazy {
        navIcon("Edit", false, strokeWidth = 2f) {
            moveTo(4f, 16.5f); lineTo(3.5f, 20.5f); lineTo(7.5f, 20f)
            moveTo(5f, 19f); lineTo(16.8f, 7.2f)
            moveTo(14.8f, 5.2f); lineTo(16.8f, 3.2f); lineTo(20.8f, 7.2f); lineTo(18.8f, 9.2f)
        }
    }

    val search: ImageVector by lazy {
        navIcon("Search", false) {
            circle(10.5f, 10.5f, 6.5f)
            moveTo(15.4f, 15.4f); lineTo(20f, 20f)
        }
    }

    val checkCircle: ImageVector by lazy {
        navIcon("CheckCircle", false, strokeWidth = 1.6f) {
            circle(12f, 12f, 9f)
            moveTo(8f, 12.4f); lineTo(10.8f, 15.2f); lineTo(16.2f, 9.2f)
        }
    }

    val settings: ImageVector by lazy {
        navIcon("Settings", false, strokeWidth = 1.8f) {
            var first = true
            for (k in 0 until 8) for ((r, d) in listOf(7.3f to -14.0, 9.8f to -9.0, 9.8f to 9.0, 7.3f to 14.0)) {
                val a = Math.toRadians(k * 45.0 + d)
                val x = 12f + r * cos(a).toFloat()
                val y = 12f + r * sin(a).toFloat()
                if (first) { moveTo(x, y); first = false } else lineTo(x, y)
            }
            close()
            circle(12f, 12f, 3.2f)
        }
    }

    val camera: ImageVector by lazy {
        navIcon("Camera", false, strokeWidth = 1.8f) {
            moveTo(4f, 8f); lineTo(7.5f, 8f); lineTo(9f, 5.5f); lineTo(15f, 5.5f); lineTo(16.5f, 8f); lineTo(20f, 8f)
            lineTo(20f, 18.5f); lineTo(4f, 18.5f); close()
            circle(12f, 13f, 3.4f)
        }
    }

    val chevronDown: ImageVector by lazy {
        navIcon("ChevronDown", false, strokeWidth = 2.2f) {
            moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
        }
    }

    val check: ImageVector by lazy {
        navIcon("Check", false, strokeWidth = 2.4f) {
            moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f)
        }
    }

    val tune: ImageVector by lazy {
        navIcon("Tune", false, strokeWidth = 2f) {
            moveTo(4f, 8f); lineTo(9f, 8f); circle(12f, 8f, 2.6f); moveTo(14.6f, 8f); lineTo(20f, 8f)
            moveTo(4f, 16f); lineTo(9.4f, 16f); circle(12f, 16f, 2.6f); moveTo(15f, 16f); lineTo(20f, 16f)
        }
    }

    val star: ImageVector by lazy {
        navIcon("Star", true, strokeWidth = 1.4f) {
            moveTo(12f, 2.8f); lineTo(14.8f, 8.6f); lineTo(21.2f, 9.5f); lineTo(16.6f, 14f); lineTo(17.7f, 20.4f)
            lineTo(12f, 17.4f); lineTo(6.3f, 20.4f); lineTo(7.4f, 14f); lineTo(2.8f, 9.5f); lineTo(9.2f, 8.6f); close()
        }
    }

    val eye: ImageVector by lazy {
        navIcon("Eye", false, strokeWidth = 1.8f) {
            moveTo(2.5f, 12f)
            curveTo(5f, 7.5f, 8.3f, 5.5f, 12f, 5.5f)
            curveTo(15.7f, 5.5f, 19f, 7.5f, 21.5f, 12f)
            curveTo(19f, 16.5f, 15.7f, 18.5f, 12f, 18.5f)
            curveTo(8.3f, 18.5f, 5f, 16.5f, 2.5f, 12f)
            close()
            circle(12f, 12f, 3f)
        }
    }

    fun bell(filled: Boolean): ImageVector = if (filled) bellFilled else bellOutline

    private val bellOutline: ImageVector by lazy { buildBell(false) }
    private val bellFilled: ImageVector by lazy { buildBell(true) }

    private fun buildBell(filled: Boolean) = navIcon("Bell", filled) {
        moveTo(6f, 16.5f)
        curveTo(6.8f, 15.6f, 7.2f, 14.6f, 7.2f, 12.5f)
        lineTo(7.2f, 10f)
        curveTo(7.2f, 7.4f, 9.2f, 5.4f, 12f, 5.4f)
        curveTo(14.8f, 5.4f, 16.8f, 7.4f, 16.8f, 10f)
        lineTo(16.8f, 12.5f)
        curveTo(16.8f, 14.6f, 17.2f, 15.6f, 18f, 16.5f)
        close()
        moveTo(10.2f, 19.2f)
        curveTo(10.5f, 20.2f, 11.2f, 20.6f, 12f, 20.6f)
        curveTo(12.8f, 20.6f, 13.5f, 20.2f, 13.8f, 19.2f)
        moveTo(12f, 3.4f)
        lineTo(12f, 5.4f)
    }

    fun bookmark(filled: Boolean): ImageVector = if (filled) bookmarkFilled else bookmarkOutline

    private val bookmarkOutline: ImageVector by lazy { buildBookmark(false) }
    private val bookmarkFilled: ImageVector by lazy { buildBookmark(true) }

    private fun buildBookmark(filled: Boolean) = navIcon("Bookmark", filled) {
        moveTo(6.5f, 4.8f)
        curveTo(6.5f, 4.1f, 7f, 3.6f, 7.7f, 3.6f)
        lineTo(16.3f, 3.6f)
        curveTo(17f, 3.6f, 17.5f, 4.1f, 17.5f, 4.8f)
        lineTo(17.5f, 20.4f)
        lineTo(12f, 16.2f)
        lineTo(6.5f, 20.4f)
        close()
    }

    val comment: ImageVector by lazy {
        navIcon("Comment", false, strokeWidth = 1.8f) {
            moveTo(4f, 5.5f); lineTo(20f, 5.5f); lineTo(20f, 16.2f); lineTo(10.2f, 16.2f)
            lineTo(5.8f, 19.8f); lineTo(5.8f, 16.2f); lineTo(4f, 16.2f); close()
        }
    }

    val more: ImageVector by lazy {
        navIcon("More", true, stroked = false) {
            circle(5f, 12f, 1.7f); circle(12f, 12f, 1.7f); circle(19f, 12f, 1.7f)
        }
    }

    val moreVertical: ImageVector by lazy {
        navIcon("MoreVertical", true, stroked = false) {
            circle(12f, 5f, 1.7f); circle(12f, 12f, 1.7f); circle(12f, 19f, 1.7f)
        }
    }

    val reply: ImageVector by lazy {
        navIcon("Reply", false, strokeWidth = 1.8f) {
            moveTo(9.5f, 6f); lineTo(4f, 11.5f); lineTo(9.5f, 17f)
            moveTo(4f, 11.5f); lineTo(14f, 11.5f)
            curveTo(18f, 11.5f, 20.5f, 14f, 20.5f, 18.5f)
        }
    }

    val crown: ImageVector by lazy {
        navIcon("Crown", true, stroked = false) {
            moveTo(3f, 18f); lineTo(2.5f, 7f); lineTo(8f, 11.5f); lineTo(12f, 4.5f); lineTo(16f, 11.5f); lineTo(21.5f, 7f); lineTo(21f, 18f); close()
            moveTo(4f, 19.5f); lineTo(20f, 19.5f); lineTo(20f, 21.5f); lineTo(4f, 21.5f); close()
        }
    }

    val shield: ImageVector by lazy {
        navIcon("Shield", true, stroked = false) {
            moveTo(12f, 2.5f); lineTo(20f, 5.5f); lineTo(20f, 12f)
            curveTo(20f, 16.8f, 16.5f, 20.2f, 12f, 21.8f)
            curveTo(7.5f, 20.2f, 4f, 16.8f, 4f, 12f)
            lineTo(4f, 5.5f); close()
        }
    }

    val diamond: ImageVector by lazy {
        // Berlian bersegi: mahkota 3 potongan + dasar 3 potongan, dipisah celah tipis supaya bentuknya jelas.
        navIcon("Diamond", true, stroked = false) {
            // mahkota (atas)
            moveTo(2f, 9.4f); lineTo(6.6f, 4.2f); lineTo(8.3f, 9.4f); close()
            moveTo(7.7f, 4.2f); lineTo(16.3f, 4.2f); lineTo(15f, 9.4f); lineTo(9f, 9.4f); close()
            moveTo(17.4f, 4.2f); lineTo(22f, 9.4f); lineTo(15.7f, 9.4f); close()
            // dasar (bawah)
            moveTo(2.4f, 10.6f); lineTo(8.4f, 10.6f); lineTo(11.6f, 20.4f); close()
            moveTo(9.1f, 10.6f); lineTo(14.9f, 10.6f); lineTo(12f, 19.6f); close()
            moveTo(15.6f, 10.6f); lineTo(21.6f, 10.6f); lineTo(12.4f, 20.4f); close()
        }
    }

    val dashboard: ImageVector by lazy {
        navIcon("Dashboard", false, strokeWidth = 1.8f) {
            moveTo(4f, 4f); lineTo(10.5f, 4f); lineTo(10.5f, 12f); lineTo(4f, 12f); close()
            moveTo(13.5f, 4f); lineTo(20f, 4f); lineTo(20f, 8.5f); lineTo(13.5f, 8.5f); close()
            moveTo(13.5f, 12f); lineTo(20f, 12f); lineTo(20f, 20f); lineTo(13.5f, 20f); close()
            moveTo(4f, 15.5f); lineTo(10.5f, 15.5f); lineTo(10.5f, 20f); lineTo(4f, 20f); close()
        }
    }

    val users: ImageVector by lazy {
        navIcon("Users", false, strokeWidth = 1.8f) {
            circle(9f, 8.5f, 3.5f)
            moveTo(2.5f, 20f)
            curveTo(2.5f, 15.8f, 5.4f, 13.8f, 9f, 13.8f)
            curveTo(12.6f, 13.8f, 15.5f, 15.8f, 15.5f, 20f)
            moveTo(15.5f, 5.2f)
            curveTo(17.7f, 5.6f, 18.8f, 7f, 18.8f, 8.6f)
            curveTo(18.8f, 10.2f, 17.7f, 11.6f, 15.5f, 12f)
            moveTo(17f, 14.2f)
            curveTo(19.6f, 14.8f, 21.5f, 16.6f, 21.5f, 20f)
        }
    }

    val flag: ImageVector by lazy {
        navIcon("Flag", false, strokeWidth = 1.8f) {
            moveTo(5f, 21f); lineTo(5f, 3.5f)
            moveTo(5f, 4.5f); lineTo(18.5f, 4.5f); lineTo(15.5f, 9f); lineTo(18.5f, 13.5f); lineTo(5f, 13.5f)
        }
    }

    val trash: ImageVector by lazy {
        navIcon("Trash", false, strokeWidth = 1.8f) {
            moveTo(4f, 6.5f); lineTo(20f, 6.5f)
            moveTo(9f, 6.5f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 6.5f)
            moveTo(6f, 6.5f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 6.5f)
            moveTo(10f, 10.5f); lineTo(10f, 16f)
            moveTo(14f, 10.5f); lineTo(14f, 16f)
        }
    }

    val send: ImageVector by lazy {
        navIcon("Send", false, strokeWidth = 1.8f) {
            moveTo(4f, 11.6f); lineTo(20f, 4.4f); lineTo(14f, 20f); lineTo(11.4f, 13.2f); close()
            moveTo(11.4f, 13.2f); lineTo(20f, 4.4f)
        }
    }

    fun heart(filled: Boolean): ImageVector = if (filled) heartFilled else heartOutline

    private val heartOutline: ImageVector by lazy { buildHeart(false) }
    private val heartFilled: ImageVector by lazy { buildHeart(true) }

    private fun buildHeart(filled: Boolean) = navIcon("Heart", filled, strokeWidth = 1.8f) {
        moveTo(12f, 20.2f)
        curveTo(5.5f, 15.2f, 3.4f, 11.8f, 3.4f, 8.9f)
        curveTo(3.4f, 6.5f, 5.2f, 4.8f, 7.4f, 4.8f)
        curveTo(9.2f, 4.8f, 10.9f, 5.8f, 12f, 7.6f)
        curveTo(13.1f, 5.8f, 14.8f, 4.8f, 16.6f, 4.8f)
        curveTo(18.8f, 4.8f, 20.6f, 6.5f, 20.6f, 8.9f)
        curveTo(20.6f, 11.8f, 18.5f, 15.2f, 12f, 20.2f)
        close()
    }
}

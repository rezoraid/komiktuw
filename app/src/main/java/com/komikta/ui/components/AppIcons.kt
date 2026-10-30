package com.komikta.ui.components

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

    val chevronUp: ImageVector by lazy {
        navIcon("ChevronUp", false, strokeWidth = 2.4f) {
            moveTo(5f, 15f); lineTo(12f, 8f); lineTo(19f, 15f)
        }
    }

    val chevronDown: ImageVector by lazy {
        navIcon("ChevronDown", false, strokeWidth = 2.4f) {
            moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f)
        }
    }

    val chevronsDown: ImageVector by lazy {
        navIcon("ChevronsDown", false, strokeWidth = 2.2f) {
            moveTo(6.5f, 6.5f); lineTo(12f, 12f); lineTo(17.5f, 6.5f)
            moveTo(6.5f, 12.5f); lineTo(12f, 18f); lineTo(17.5f, 12.5f)
        }
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

    val download: ImageVector by lazy {
        navIcon("Download", false) {
            moveTo(12f, 4f); lineTo(12f, 15f)
            moveTo(7.5f, 10.8f); lineTo(12f, 15.2f); lineTo(16.5f, 10.8f)
            moveTo(5f, 19.5f); lineTo(19f, 19.5f)
        }
    }

    val search: ImageVector by lazy {
        navIcon("Search", false) {
            circle(10.5f, 10.5f, 6.5f)
            moveTo(15.4f, 15.4f); lineTo(20f, 20f)
        }
    }

    val bell: ImageVector by lazy {
        navIcon("Bell", false, strokeWidth = 1.8f) {
            moveTo(6f, 16.5f)
            lineTo(6f, 11f)
            curveTo(6f, 7.7f, 8.7f, 5f, 12f, 5f)
            curveTo(15.3f, 5f, 18f, 7.7f, 18f, 11f)
            lineTo(18f, 16.5f)
            lineTo(19.5f, 18f)
            lineTo(4.5f, 18f)
            close()
            moveTo(10f, 20.5f)
            curveTo(10.4f, 21.3f, 11.1f, 21.7f, 12f, 21.7f)
            curveTo(12.9f, 21.7f, 13.6f, 21.3f, 14f, 20.5f)
            moveTo(12f, 5f)
            lineTo(12f, 3.6f)
        }
    }

    val play: ImageVector by lazy {
        navIcon("Play", false) {
            moveTo(7.5f, 4.8f); lineTo(19f, 12f); lineTo(7.5f, 19.2f); close()
        }
    }

    val person: ImageVector by lazy {
        navIcon("Person", false, strokeWidth = 1.7f) {
            circle(12f, 8f, 3.6f)
            moveTo(5f, 20f)
            curveTo(5f, 16.2f, 8f, 14f, 12f, 14f)
            curveTo(16f, 14f, 19f, 16.2f, 19f, 20f)
        }
    }

    val palette: ImageVector by lazy {
        navIcon("Palette", false, strokeWidth = 1.7f) {
            circle(12f, 12f, 9f)
            circle(8f, 10.5f, 0.6f)
            circle(12f, 7.8f, 0.6f)
            circle(16f, 10.5f, 0.6f)
        }
    }

    val doc: ImageVector by lazy {
        navIcon("Doc", false, strokeWidth = 1.7f) {
            moveTo(6.5f, 3.5f); lineTo(17.5f, 3.5f); lineTo(17.5f, 20.5f); lineTo(6.5f, 20.5f); close()
            moveTo(9.5f, 8f); lineTo(14.5f, 8f)
            moveTo(9.5f, 12f); lineTo(14.5f, 12f)
            moveTo(9.5f, 16f); lineTo(12.5f, 16f)
        }
    }

    val eye: ImageVector by lazy {
        navIcon("Eye", false, strokeWidth = 1.7f) {
            moveTo(2.5f, 12f)
            curveTo(5f, 7.5f, 8.5f, 5.5f, 12f, 5.5f)
            curveTo(15.5f, 5.5f, 19f, 7.5f, 21.5f, 12f)
            curveTo(19f, 16.5f, 15.5f, 18.5f, 12f, 18.5f)
            curveTo(8.5f, 18.5f, 5f, 16.5f, 2.5f, 12f)
            close()
            circle(12f, 12f, 3f)
        }
    }

    val checkCircle: ImageVector by lazy {
        navIcon("CheckCircle", false, strokeWidth = 1.6f) {
            circle(12f, 12f, 9f)
            moveTo(8f, 12.4f); lineTo(10.8f, 15.2f); lineTo(16.2f, 9.2f)
        }
    }

    val arrowRight: ImageVector by lazy {
        navIcon("ArrowRight", false, strokeWidth = 2f) {
            moveTo(5f, 12f); lineTo(19f, 12f)
            moveTo(13f, 6f); lineTo(19f, 12f); lineTo(13f, 18f)
        }
    }
}

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
}

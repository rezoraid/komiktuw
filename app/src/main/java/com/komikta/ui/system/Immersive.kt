package com.komikta.ui.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** True selama user berada di reader atau komentar chapter (mode fullscreen). Diset sekali di MainNavGraph. */
val LocalImmersive = staticCompositionLocalOf { false }

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun applyBars(window: Window, view: View, hide: Boolean) {
    val controller = WindowCompat.getInsetsController(window, view)
    if (hide) {
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Satu-satunya tempat yang menyembunyikan / menampilkan system bar milik Activity.
 * Efek hanya jalan saat nilai [on] berubah, jadi pindah reader <-> komentar (true -> true)
 * tidak menyentuh status bar sama sekali. Keluar dari fullscreen (true -> false) mengembalikan bar.
 */
@Composable
fun ImmersiveEffect(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, on) {
        val window = view.context.findActivity()?.window
        if (window != null) applyBars(window, view, hide = on)
        // Keyboard, dialog, atau notifikasi bisa mengembalikan bar saat fokus pindah: sembunyikan lagi.
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (on && hasFocus && window != null) applyBars(window, view, hide = true)
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
    }
}

/**
 * Panggil di dalam isi ModalBottomSheet / AlertDialog. Dialog adalah window terpisah dan
 * secara default memunculkan status bar saat dibuka (penyebab kedip).
 */
@Composable
fun ImmersiveDialogEffect() {
    val on = LocalImmersive.current
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: (view as? DialogWindowProvider)?.window
    DisposableEffect(window, on) {
        if (on && window != null) applyBars(window, view, hide = true)
        onDispose { }
    }
}

/** Padding status bar yang tidak berubah walau bar disembunyikan, jadi konten tidak bergeser. */
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.stableStatusBarsPadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
}

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.stableNavigationBarsPadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
}

/** Inset sheet yang stabil: hanya navigation bar bawah, tanpa padding status bar di atas sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun stableSheetInsets(): WindowInsets =
    WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Bottom)

/** Sheet tidak menambah padding atas/bawah sendiri; konten mengatur inset-nya sendiri. */
fun noSheetInsets(): WindowInsets = WindowInsets(0, 0, 0, 0)

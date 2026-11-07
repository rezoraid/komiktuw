package com.komikta

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.komikta.core.notify.NotificationPrefs
import com.komikta.core.notify.UpdateNotifier
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.komikta.ui.navigation.MainNavGraph
import com.komikta.ui.theme.KomiktaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var notificationPrefs: NotificationPrefs

    /** Komik yang dibuka dari ketukan notifikasi: (source, slug). */
    private var openComic by mutableStateOf<Pair<String, String>?>(null)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private fun readTarget(intent: Intent?): Pair<String, String>? {
        val source = intent?.getStringExtra(UpdateNotifier.EXTRA_SOURCE)
        val slug = intent?.getStringExtra(UpdateNotifier.EXTRA_SLUG)
        return if (!source.isNullOrBlank() && !slug.isNullOrBlank()) source to slug else null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openComic = readTarget(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openComic = readTarget(intent)
        // Android 13+: minta izin notifikasi sekali. Selanjutnya bisa diatur lewat tombol lonceng di Library.
        if (Build.VERSION.SDK_INT >= 33 && notificationPrefs.enabled && !notificationPrefs.permissionAsked &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPrefs.permissionAsked = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // Diset sekali di sini (bukan per-layar) supaya tidak ada relayout saat masuk/keluar reader.
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        setContent {
            KomiktaTheme {
                MainNavGraph(
                    openComic = openComic,
                    onOpenHandled = {
                        intent?.removeExtra(UpdateNotifier.EXTRA_SOURCE)
                        intent?.removeExtra(UpdateNotifier.EXTRA_SLUG)
                        openComic = null
                    }
                )
            }
        }
    }
}

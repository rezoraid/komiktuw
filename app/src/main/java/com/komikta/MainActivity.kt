package com.komikta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.komikta.ui.navigation.MainNavGraph
import com.komikta.ui.theme.KomiktaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Memberi sinyal ke sistem bahwa first frame siap (startup tracing / baseline profile)
            ReportDrawn()
            KomiktaTheme {
                MainNavGraph()
            }
        }
    }
}

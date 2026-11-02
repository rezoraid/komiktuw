package com.komikta.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.komikta.ui.components.AppIcons

/** Profil pengguna lain: tampilan sama dengan profil sendiri, data diambil dari backend untuk userId tersebut. */
@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onOpen: (source: String, slug: String, chapter: String?) -> Unit,
    viewModel: ProfilePageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { viewModel.load(userId) }

    Box(Modifier.fillMaxSize().background(P.Content).statusBarsPadding()) {
        ProfilePage(
            state = state,
            fallbackUser = null,
            busy = false,
            onTab = viewModel::selectTab,
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            onItemClick = { onOpen(it.source, it.slug, it.chapter) },
            onAvatarClick = null,
            onLevelClick = null,
            topLeft = {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.chevronLeft, "Kembali", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            },
            topRight = {}
        )
        if (state.stats == null && state.statsError != null) {
            Text(state.statsError.orEmpty(), color = Color(0xFF807F84), fontSize = 14.sp, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        }
    }
}

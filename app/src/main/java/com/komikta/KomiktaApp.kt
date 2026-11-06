package com.komikta

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.komikta.data.local.SourcePrefs
import com.komikta.data.repository.CommunityRepository
import com.komikta.domain.repository.ComicRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KomiktaApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    @Inject
    lateinit var comics: dagger.Lazy<ComicRepository>

    @Inject
    lateinit var community: dagger.Lazy<CommunityRepository>

    @Inject
    lateinit var sourcePrefs: SourcePrefs

    private val warmupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        warmupScope.launch { warmUp() }
    }

    override fun newImageLoader(): ImageLoader = imageLoader.get()

    private suspend fun warmUp() = coroutineScope {
        val source = sourcePrefs.source
        val repo = comics.get()
        val session = community.get()
        launch { repo.observeLatest(source).collect {} }
        launch { repo.observePopular(source).collect {} }
        if (session.user.value != null) launch { session.refreshMe() }
    }
}

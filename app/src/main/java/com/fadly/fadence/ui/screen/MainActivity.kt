package com.fadly.fadence.ui.screen

import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.getSystemService
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.fadly.fadence.R
import com.fadly.fadence.core.model.CategoryInfo
import com.fadly.fadence.core.model.MediaEvent
import com.fadly.fadence.data.model.network.NetworkFeature
import com.fadly.fadence.extensions.currentFragment
import com.fadly.fadence.extensions.navigation.isValidCategory
import com.fadly.fadence.extensions.showToast
import com.fadly.fadence.extensions.whichFragment
import com.fadly.fadence.playback.Playback
import com.fadly.fadence.playback.library.MediaIDs
import com.fadly.fadence.core.model.shuffle.OpenShuffleMode
import com.fadly.fadence.ui.IScrollHelper
import com.fadly.fadence.ui.component.base.AbsSlidingMusicPanelActivity
import com.fadly.fadence.ui.screen.update.UpdateDialog
import com.fadly.fadence.ui.screen.update.UpdateSearchResult
import com.fadly.fadence.ui.screen.update.UpdateViewModel
import com.fadly.fadence.util.Preferences
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians M. A. (mardous)
 */
class MainActivity : AbsSlidingMusicPanelActivity(), MediaController.Listener {

    private val updateViewModel: UpdateViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = null

        updateTabs()
        setupNavigationController()

        val shortcutManager = getSystemService<ShortcutManager>()
        shortcutManager?.removeDynamicShortcuts(OLD_SHORTCUT_IDS)

        prepareUpdateViewModel()
    }

    override fun onConnected(controller: MediaController) {
        super.onConnected(controller)
        intent?.let { handlePlaybackIntent(it, true) }
    }

    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
        controller: MediaController,
        command: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        val sessionResult = when (command.customAction) {
            Playback.EVENT_MEDIA_CONTENT_CHANGED -> {
                playerViewModel.submitEvent(MediaEvent.MediaContentChanged)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            Playback.EVENT_FAVORITE_CONTENT_CHANGED -> {
                playerViewModel.submitEvent(MediaEvent.FavoriteContentChanged)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            Playback.EVENT_PLAYBACK_STARTED -> {
                playerViewModel.submitEvent(MediaEvent.PlaybackStarted)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            Playback.EVENT_PLAYBACK_RESTORED -> {
                playerViewModel.submitEvent(MediaEvent.PlaybackRestored)
                SessionResult(SessionResult.RESULT_SUCCESS)
            }

            else -> SessionResult(SessionError.ERROR_NOT_SUPPORTED)
        }
        return Futures.immediateFuture(sessionResult)
    }

    fun scanAllPaths() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.scan_media)
            .setMessage(R.string.scan_media_message)
            .setPositiveButton(R.string.scan_media_positive) { _, _ ->
                libraryViewModel.scanAllPaths(this).observe(this) {
                    // TODO show detailed info about scanned songs
                    showToast(R.string.scan_finished)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupNavigationController() {
        val navController = whichFragment<NavHostFragment>(R.id.fragment_container).navController
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.graph_main)

        val categoryInfo: CategoryInfo = Preferences.libraryCategories.first { it.visible }
        if (categoryInfo.visible) {
            val lastPage = Preferences.lastPage
            if (!navGraph.isValidCategory(lastPage)) {
                Preferences.lastPage = categoryInfo.category.id
                navGraph.setStartDestination(categoryInfo.category.id)
            } else {
                navGraph.setStartDestination(
                    if (Preferences.isRememberLastPage) {
                        lastPage.let {
                            if (it == 0) {
                                categoryInfo.category.id
                            } else {
                                it
                            }
                        }
                    } else categoryInfo.category.id
                )
            }
        }

        navController.graph = navGraph
        navigationView.setupWithNavController(navController)
        // Scroll Fragment to top
        navigationView.setOnItemReselectedListener {
            currentFragment(R.id.fragment_container).apply {
                if (this is IScrollHelper) {
                    scrollToTop()
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == navGraph.startDestinationId) {
                currentFragment(R.id.fragment_container)?.enterTransition = null
            }
            if (destination.navigatorName == "dialog") {
                return@addOnDestinationChangedListener
            }
            when (destination.id) {
                R.id.nav_home,
                R.id.nav_songs,
                R.id.nav_albums,
                R.id.nav_artists,
                R.id.nav_folders,
                R.id.nav_playlists,
                R.id.nav_genres,
                R.id.nav_years -> {
                    // Save the last tab
                    if (Preferences.isRememberLastPage) {
                        saveTab(destination.id)
                    }
                    // Show Bottom Navigation Bar
                    setBottomNavVisibility(visible = true, animate = true)
                }

                R.id.nav_queue,
                R.id.nav_lyrics_editor,
                R.id.nav_play_info,
                R.id.nav_about -> {
                    setBottomNavVisibility(visible = false, hideBottomSheet = true)
                }

                else -> setBottomNavVisibility(visible = false, animate = true) // Hide Bottom Navigation Bar
            }
        }
    }

    private fun saveTab(id: Int) {
        if (Preferences.libraryCategories.firstOrNull { it.category.id == id }?.visible == true) {
            Preferences.lastPage = id
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePlaybackIntent(intent, false)
    }

    private fun handlePlaybackIntent(intent: Intent, canRestorePlayback: Boolean) {
        when (intent.action) {
            APP_SHORTCUT_LAST_ADDED -> {
                playerViewModel.playMediaId(MediaIDs.LAST_ADDED)
                setIntent(Intent())
            }
            APP_SHORTCUT_TOP_TRACKS -> {
                playerViewModel.playMediaId(MediaIDs.TOP_TRACKS)
                setIntent(Intent())
            }
            APP_SHORTCUT_SHUFFLE -> {
                playerViewModel.playMediaId(MediaIDs.SONGS, true)
                setIntent(Intent())
            }
            APP_SHORTCUT_FAVORITES -> {
                playerViewModel.playMediaId(MediaIDs.FAVORITES, true)
                setIntent(Intent())
            }
            else -> {
                libraryViewModel.handleIntent(intent).observe(this) { result ->
                    if (result.handled) {
                        if (result.songs.isNotEmpty()) {
                            playerViewModel.openQueue(
                                queue = result.songs,
                                position = result.position,
                                shuffleMode = OpenShuffleMode.Off
                            )
                        }
                        setIntent(Intent())
                    } else if (canRestorePlayback) {
                        playerViewModel.restorePlayback()
                    }
                    if (result.failed) {
                        showToast(R.string.unplayable_file)
                    }
                }
            }
        }
    }

    private fun prepareUpdateViewModel() {
        updateViewModel.run {
            updateEventObservable.observe(this@MainActivity) { event ->
                event.getContentIfNotConsumed()?.let { result ->
                    when (result.state) {
                        UpdateSearchResult.State.Completed -> {
                            val release = result.data ?: return@let
                            if (result.wasFromUser || release.isDownloadable(this@MainActivity)) {
                                val existingDialog = supportFragmentManager.findFragmentByTag("UPDATE_FOUND")
                                if (existingDialog == null) {
                                    UpdateDialog().show(supportFragmentManager, "UPDATE_FOUND")
                                }
                            }
                        }
                        UpdateSearchResult.State.Failed -> {
                            if (result.wasFromUser) {
                                showToast(R.string.could_not_check_for_updates)
                            }
                        }
                        else -> {}
                    }
                }
            }
            updateEvent?.peekContent().let { updateState ->
                if (updateState == null || updateState.state == UpdateSearchResult.State.Idle) {
                    if (NetworkFeature.Updater.isAvailable(this@MainActivity)) {
                        searchForUpdate(false)
                    }
                }
            }
        }
    }

    companion object {
        private const val APP_SHORTCUT_LAST_ADDED = "com.fadly.fadence.shortcut.LAST_ADDED"
        private const val APP_SHORTCUT_TOP_TRACKS = "com.fadly.fadence.shortcut.TOP_TRACKS"
        private const val APP_SHORTCUT_SHUFFLE = "com.fadly.fadence.shortcut.SHUFFLE"
        private const val APP_SHORTCUT_FAVORITES = "com.fadly.fadence.shortcut.FAVORITES"

        private val OLD_SHORTCUT_IDS = listOf(
            "com.fadly.fadence.appshortcuts.id.last_added",
            "com.fadly.fadence.appshortcuts.id.top_tracks",
            "com.fadly.fadence.appshortcuts.id.shuffle_all",
        )
    }
}
package com.fadly.fadence.ui.screen.player.cover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.fadly.fadence.R
import com.fadly.fadence.extensions.currentFragment
import com.fadly.fadence.ui.component.base.goToDestination
import com.fadly.fadence.ui.screen.MainActivity
import com.fadly.fadence.ui.screen.lyrics.CoverLyricsScreen
import com.fadly.fadence.ui.screen.lyrics.LyricsFragment
import com.fadly.fadence.ui.screen.lyrics.LyricsViewModel
import com.fadly.fadence.ui.screen.player.PlayerViewModel
import com.fadly.fadence.ui.theme.FadenceMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class CoverLyricsFragment : Fragment() {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                FadenceMusicTheme {
                    CoverLyricsScreen(
                        lyricsViewModel,
                        playerViewModel,
                        onExpandClick = {
                            if (currentFragment(R.id.fragment_container) is LyricsFragment) {
                                (activity as? MainActivity)?.collapsePanel()
                            } else {
                                goToDestination(requireActivity(), R.id.nav_lyrics)
                            }
                        })
                }
            }
        }
    }
}
package com.fadly.fadence.ui.screen.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.fadly.fadence.ui.screen.library.LibraryViewModel
import com.fadly.fadence.ui.screen.player.PlayerViewModel
import com.fadly.fadence.ui.theme.FadenceMusicTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ShuffleModeFragment : BottomSheetDialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val playerViewModel: PlayerViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                FadenceMusicTheme {
                    ShuffleModeBottomSheet(
                        libraryViewModel = libraryViewModel,
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }
}
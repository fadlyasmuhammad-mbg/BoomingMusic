/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.fadly.fadence.ui.screen.library.years

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fadly.fadence.R
import com.fadly.fadence.core.sort.SongSortMode
import com.fadly.fadence.data.mapper.searchFilter
import com.fadly.fadence.data.model.ReleaseYear
import com.fadly.fadence.data.model.Song
import com.fadly.fadence.databinding.FragmentDetailListBinding
import com.fadly.fadence.extensions.applyHorizontalWindowInsets
import com.fadly.fadence.extensions.isNullOrEmpty
import com.fadly.fadence.extensions.materialSharedAxis
import com.fadly.fadence.extensions.media.songCountStr
import com.fadly.fadence.extensions.media.songsDurationStr
import com.fadly.fadence.extensions.navigation.searchArgs
import com.fadly.fadence.extensions.setSupportActionBar
import com.fadly.fadence.extensions.utilities.buildInfoString
import com.fadly.fadence.core.model.shuffle.OpenShuffleMode
import com.fadly.fadence.ui.ISongCallback
import com.fadly.fadence.ui.adapters.song.SongAdapter
import com.fadly.fadence.ui.component.base.AbsMainActivityFragment
import com.fadly.fadence.ui.component.menu.onSongMenu
import com.fadly.fadence.ui.component.menu.onSongsMenu
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class YearDetailFragment : AbsMainActivityFragment(R.layout.fragment_detail_list), ISongCallback {

    private val arguments by navArgs<YearDetailFragmentArgs>()
    private val detailViewModel: YearDetailViewModel by viewModel {
        parametersOf(arguments.extraYear)
    }

    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: SongAdapter

    private val year: ReleaseYear
        get() = detailViewModel.getYear().value ?: ReleaseYear.Empty

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailListBinding.bind(view)

        setSupportActionBar(binding.toolbar)
        materialSharedAxis(view)
        view.applyHorizontalWindowInsets()

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(bottom = it.getWithSpace())
        }

        setupButtons()
        setupRecyclerView()
        detailViewModel.getYear().observe(viewLifecycleOwner) { year ->
            binding.collapsingAppBarLayout.title = year.name
            binding.title.text = year.name
            songs(year.songs)
        }
    }

    private fun setupButtons() {
        binding.playAction.setOnClickListener {
            playerViewModel.openQueue(songAdapter.dataSet, shuffleMode = OpenShuffleMode.Off)
        }
        binding.shuffleAction.setOnClickListener {
            playerViewModel.openAndShuffleQueue(songAdapter.dataSet)
        }
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(requireActivity(), ArrayList(), R.layout.item_list, callback = this)
        binding.recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }
        songAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

    fun songs(songs: List<Song>) {
        binding.progressIndicator.hide()
        binding.subtitle.text = buildInfoString(songs.songCountStr(requireContext()), songs.songsDurationStr())
        songAdapter.dataSet = songs
    }

    private fun checkIsEmpty() {
        if (songAdapter.isNullOrEmpty) {
            findNavController().navigateUp()
        }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = song.onSongMenu(this, menuItem)

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_genre_detail, menu)
        SongSortMode.YearSongs.createMenu(menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return if (SongSortMode.YearSongs.sortItemSelected(item)) {
            detailViewModel.loadDetail()
            true
        } else when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            R.id.action_search -> {
                findNavController().navigate(R.id.nav_search, searchArgs(year.searchFilter(requireContext())))
                true
            }

            else -> songAdapter.dataSet.onSongsMenu(this, item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.fadly.fadence.data

import com.fadly.fadence.data.model.Song

interface SongProvider {
    val songs: List<Song>
}
package com.fadly.fadence.coil.model

import com.fadly.fadence.data.local.room.PlaylistEntity
import com.fadly.fadence.data.model.Song

class PlaylistImage(val playlistEntity: PlaylistEntity, val songs: List<Song>) {
    override fun toString(): String {
        return buildString {
            append("PlaylistImage{")
            append("playlistEntity=$playlistEntity,")
            append("songs=$songs")
            append("}")
        }
    }
}
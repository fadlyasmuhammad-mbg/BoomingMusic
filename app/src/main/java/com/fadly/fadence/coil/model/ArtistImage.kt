package com.fadly.fadence.coil.model

import android.net.Uri
import com.fadly.fadence.extensions.media.isArtistNameUnknown

class ArtistImage(val id: Long, val coverUri: Uri, val name: String) {
    val isNameUnknown = name.isArtistNameUnknown()

    override fun toString(): String {
        return buildString {
            append("ArtistImage{")
            append("id=$id,")
            append("coverUri=$coverUri,")
            append("name='$name'")
            append("}")
        }
    }
}
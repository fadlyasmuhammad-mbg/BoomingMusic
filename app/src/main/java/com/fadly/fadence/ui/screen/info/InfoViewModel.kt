package com.fadly.fadence.ui.screen.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.fadly.fadence.data.local.MetadataReader
import com.fadly.fadence.data.local.repository.Repository
import com.fadly.fadence.data.mapper.toPlayCount
import com.fadly.fadence.data.model.Album
import com.fadly.fadence.data.model.Artist
import com.fadly.fadence.data.model.Song
import com.fadly.fadence.extensions.files.asReadableFileSize
import com.fadly.fadence.extensions.files.formatFixed
import com.fadly.fadence.extensions.files.getHumanReadableSize
import com.fadly.fadence.extensions.files.getPrettyAbsolutePath
import com.fadly.fadence.extensions.files.toAudioFile
import com.fadly.fadence.extensions.media.asNumberOfTimes
import com.fadly.fadence.extensions.media.replayGainStr
import com.fadly.fadence.extensions.media.songDurationStr
import com.fadly.fadence.extensions.utilities.dateStr
import com.fadly.fadence.extensions.utilities.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioHeader
import java.io.File

class InfoViewModel(private val repository: Repository) : ViewModel() {

    private val _songInfoUiState = MutableStateFlow(
        SongInfoUiState(
            isLoading = true,
            isSuccess = false
        )
    )
    val songInfoUiState = _songInfoUiState.asStateFlow()

    fun loadAlbum(id: Long): LiveData<Album> = liveData(Dispatchers.IO) {
        if (id != -1L) {
            emit(repository.albumById(id))
        } else {
            emit(Album.empty)
        }
    }

    fun loadArtist(id: Long, name: String?): LiveData<Artist> = liveData(Dispatchers.IO) {
        if (name.isNullOrEmpty()) {
            emit(repository.artistById(id))
        } else if (id == -1L) {
            emit(repository.albumArtistByName(name))
        } else {
            emit(Artist.empty)
        }
    }

    fun playInfo(songs: List<Song>): LiveData<PlayInfoResult> = liveData(Dispatchers.IO) {
        val playCountEntities = repository.findSongsInPlayCount(songs).sortedByDescending { it.playCount }
        if (playCountEntities.isEmpty()) {
            emit(PlayInfoResult(-1, -1, -1, songs.map { it.toPlayCount() }))
        } else {
            val totalPlayCount = playCountEntities.sumOf { it.playCount }
            val totalSkipCount = playCountEntities.sumOf { it.skipCount }
            val lastPlayDate = playCountEntities.maxOf { it.timePlayed }
            emit(PlayInfoResult(totalPlayCount, totalSkipCount, lastPlayDate, playCountEntities))
        }
    }

    fun refreshSongInfo(context: Context, song: Song) = viewModelScope.launch(Dispatchers.IO) {
        val uiState = SongInfoUiState(isLoading = true, isSuccess = false)
        _songInfoUiState.value = uiState

        val songInfo = runCatching {
            val playCountEntity = repository.findSongInPlayCount(song.id) ?: song.toPlayCount()
            val playCount = playCountEntity.playCount.asNumberOfTimes(context)
            val skipCount = playCountEntity.skipCount.asNumberOfTimes(context)
            val lastPlayed = context.dateStr(playCountEntity.timePlayed)

            val dateModified = song.dateModified.format(context)
            val year = if (song.year > 0) song.year.toString() else null
            val trackLength = song.songDurationStr()
            val replayGain = song.replayGainStr(context)

            val metadataReader = MetadataReader(song.uri)
            if (!metadataReader.hasMetadata) {
                SongInfo(
                    playCount = playCount,
                    skipCount = skipCount,
                    lastPlayedDate = lastPlayed,
                    filePath = File(song.data).getPrettyAbsolutePath(),
                    fileSize = song.size.asReadableFileSize(),
                    trackLength = trackLength,
                    dateModified = dateModified,
                    title = song.title,
                    albumYear = year,
                    replayGain = replayGain
                )
            } else {
                val file = File(song.data)
                val filePath = file.getPrettyAbsolutePath()
                val fileSize = file.getHumanReadableSize()

                val audioHeaderInfo = getAudioHeader(file.toAudioFile()?.audioHeader, metadataReader)

                val title = metadataReader.first(MetadataReader.TITLE)
                val album = metadataReader.first(MetadataReader.ALBUM)
                val artist = metadataReader.merge(MetadataReader.ARTIST)
                val albumArtist = metadataReader.first(MetadataReader.ALBUM_ARTIST)

                val trackNumber = getNumberAndTotal(
                    metadataReader.value(MetadataReader.TRACK_NUMBER),
                    metadataReader.value(MetadataReader.TRACK_TOTAL)
                )
                val discNumber = getNumberAndTotal(
                    metadataReader.value(MetadataReader.DISC_NUMBER),
                    metadataReader.value(MetadataReader.DISC_TOTAL)
                )

                val composer = metadataReader.merge(MetadataReader.COMPOSER)
                val conductor = metadataReader.merge(MetadataReader.PRODUCER)
                val publisher = metadataReader.merge(MetadataReader.COPYRIGHT)
                val lyricist = metadataReader.merge(MetadataReader.LYRICIST)
                val arranger = metadataReader.merge(MetadataReader.ARRANGER)
                val genre = metadataReader.merge(MetadataReader.GENRE)
                val comment = metadataReader.value(MetadataReader.COMMENT)

                SongInfo(
                    playCount = playCount,
                    skipCount = skipCount,
                    lastPlayedDate = lastPlayed,
                    filePath = filePath,
                    fileSize = fileSize,
                    trackLength = trackLength,
                    dateModified = dateModified,
                    audioHeaderInfo = audioHeaderInfo,
                    title = title ?: song.title,
                    album = album,
                    artist = artist,
                    albumArtist = albumArtist,
                    albumYear = year,
                    trackNumber = trackNumber,
                    discNumber = discNumber,
                    composer = composer,
                    conductor = conductor,
                    publisher = publisher,
                    lyricist = lyricist,
                    arranger = arranger,
                    genre = genre,
                    replayGain = replayGain,
                    comment = comment
                )
            }
        }

        _songInfoUiState.value = uiState.copy(
            isLoading = false,
            isSuccess = songInfo.isSuccess,
            info = songInfo.getOrDefault(SongInfo.Empty)
        )
    }

    private fun getNumberAndTotal(number: String?, total: String?): String? {
        val numberInt = number?.toIntOrNull() ?: return null
        val totalInt = total?.toIntOrNull()
        return if (totalInt == null || totalInt == 0) {
            numberInt.toString().padStart(2, '0')
        } else {
            "%02d/%02d".format(numberInt, totalInt)
        }
    }

    private fun getAudioHeader(header: AudioHeader?, metadataReader: MetadataReader): AudioHeaderInfo {
        return AudioHeaderInfo(
            format = header?.formatFixed,
            bitrate = metadataReader.bitrate(),
            sampleRate = metadataReader.sampleRate(),
            channels = metadataReader.channelName(),
            variableBitrate = header?.isVariableBitRate == true,
            lossless = header?.isLossless == true
        )
    }
}
package com.fadly.fadence.playback

object Playback {
    // Custom commands
    const val TOGGLE_SHUFFLE = "com.fadly.fadence.command.shuffle.toggle"
    const val CYCLE_REPEAT = "com.fadly.fadence.command.repeat.cycle"
    const val TOGGLE_FAVORITE = "com.fadly.fadence.command.toggle_favorite"
    const val RESTORE_PLAYBACK = "com.fadly.fadence.command.restore_playback"

    const val SET_UNSHUFFLED_ORDER = "com.fadly.fadence.command.set.unshuffled_order"
    const val SET_STOP_POSITION = "com.fadly.fadence.command.set.stop_position"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "com.fadly.fadence.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "com.fadly.fadence.event.favorite_content_changed"
    const val EVENT_PLAYBACK_RESTORED = "com.fadly.fadence.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "com.fadly.fadence.event.playback_started"
}
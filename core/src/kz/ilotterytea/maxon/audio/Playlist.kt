package kz.ilotterytea.maxon.audio

import com.badlogic.gdx.audio.Music
import kz.ilotterytea.maxon.utils.math.Math

/**
 * Playlist.
 */
class Playlist(vararg musics: Music) {
    private val playlist: Array<out Music> = musics
    var playingNow: Music = playlist[0]
    private var index = 0

    var shuffleMode = false
    var volume = 1f
        set(value) {
            playingNow.volume = value
            field = value
        }

    /**
     * Play next music.
     */
    fun next() {
        if (playingNow.isPlaying) playingNow.stop()

        if (shuffleMode) {
            index = Math.getRandomNumber(0, playlist.size - 1)
            playingNow = playlist[index]
        } else {
            index++
            if (index > playlist.size - 1) index = 0

            playingNow = playlist[index]
        }

        playingNow.volume = volume
        playingNow.play()
    }
}
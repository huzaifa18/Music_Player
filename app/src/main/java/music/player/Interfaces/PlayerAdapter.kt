package music.player.Interfaces

import android.media.MediaPlayer
import android.support.v4.media.session.PlaybackStateCompat
import music.player.Models.Song
import music.player.Playback.PlayBackInfoListener


/**
 * Created by Huzaifa Asif on 6/3/2020.
 * Symtera Technologies pvt ltd
 * huzaifano1@hotmail.com
 **/

interface PlayerAdapter {

    fun isMediaPlayer(): Boolean

    fun isPlaying(): Boolean

    fun isReset(): Boolean

    fun getCurrentSong(): Song?

    @PlaybackStateCompat.State
    fun getState(): Int

    fun getPlayerPosition(): Int

    fun getMediaPlayer(): MediaPlayer?

    fun initMediaPlayer()

    fun release()

    fun resumeOrPause()

    fun reset()

    fun instantReset()

    fun skip(isNext: Boolean)

    fun seekTo(position: Int)

    fun setPlaybackInfoListener(playbackInfoListener: PlayBackInfoListener)

    fun registerNotificationActionsReceiver(isRegister: Boolean)

    fun setCurrentSong(song: Song, songs: List<Song>)

    fun onPauseActivity()

    fun onResumeActivity()
}
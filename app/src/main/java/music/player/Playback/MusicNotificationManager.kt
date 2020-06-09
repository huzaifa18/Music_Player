package music.player.Playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import music.player.Activities.MainActivity
import music.player.Models.Song
import music.player.R
import music.player.Services.MusicService
import music.player.Utilities.Utilities


/**
 * Created by Huzaifa Asif on 6/8/2020.
 * Symtera Technologies pvt ltd
 * huzaifano1@hotmail.com
 **/

class MusicNotificationManager internal constructor(private val mMusicService: MusicService) {
    private val CHANNEL_ID = "action.CHANNEL_ID"
    private val REQUEST_CODE = 100
    val notificationManager: NotificationManager
    var notificationBuilder: NotificationCompat.Builder? = null
        private set
    private var mediaSession: MediaSessionCompat? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null
    private val context: Context

    init {
        notificationManager = mMusicService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        context = mMusicService.baseContext
    }

    private fun playerAction(action: String): PendingIntent {

        val pauseIntent = Intent()
        pauseIntent.action = action

        return PendingIntent.getBroadcast(mMusicService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun createNotification(): Notification {

        val song = mMusicService.mediaPlayerHolder?.getCurrentSong()

        notificationBuilder = NotificationCompat.Builder(mMusicService, CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val openPlayerIntent = Intent(mMusicService, MainActivity::class.java)
        openPlayerIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(mMusicService, REQUEST_CODE,
            openPlayerIntent, 0)

        val artist = song!!.songArtist
        val songTitle = song.songTitle

        initMediaSession(song)

        notificationBuilder!!
            .setShowWhen(false)
            .setSmallIcon(R.drawable.ic_album_art_default)
            .setLargeIcon(Utilities().songArt(song.uri!!, mMusicService.baseContext))
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setContentTitle(songTitle)
            .setContentText(artist)
            .setContentIntent(contentIntent)
            .addAction(notificationAction(PREV_ACTION))
            .addAction(notificationAction(PLAY_PAUSE_ACTION))
            .addAction(notificationAction(NEXT_ACTION))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationBuilder!!.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession!!.sessionToken)
            .setShowActionsInCompactView(0, 1, 2))
        return notificationBuilder!!.build()
    }

    @SuppressLint("WrongConstant")
    private fun notificationAction(action: String): NotificationCompat.Action {

        val icon: Int

        when (action) {
            PREV_ACTION -> icon = R.drawable.ic_prev_white
            PLAY_PAUSE_ACTION ->

                icon = if (mMusicService.mediaPlayerHolder?.getState() != PlayBackInfoListener.State.PAUSED)
                    R.drawable.ic_pause_white
                else
                    R.drawable.ic_play_white
            NEXT_ACTION -> icon = R.drawable.ic_next_white
            else -> icon = R.drawable.ic_prev_white
        }
        return NotificationCompat.Action.Builder(icon, action, playerAction(action)).build()
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(CHANNEL_ID,
                mMusicService.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW)

            notificationChannel.description = mMusicService.getString(R.string.app_name)

            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.setShowBadge(false)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun initMediaSession(song: Song) {
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSession = MediaSessionCompat(context, "AudioPlayer")
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        updateMetaData(song)
    }

    private fun updateMetaData(song: Song) {
        mediaSession!!.setMetadata(
            MediaMetadataCompat.Builder()
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, Utilities().songArt(song.uri!!, context))
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.songArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.songTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.songTitle)
            .build())
    }

    companion object {
        val NOTIFICATION_ID = 101
        internal val PLAY_PAUSE_ACTION = "action.PLAYPAUSE"
        internal val NEXT_ACTION = "action.NEXT"
        internal val PREV_ACTION = "action.PREV"
    }

}
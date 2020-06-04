package music.player.Services

import android.app.*
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.RelativeLayout
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BlurTransformation
import music.player.Activities.MainActivity
import music.player.Activities.PlayScreen
import music.player.Interfaces.ACTIONS
import music.player.Models.Song
import music.player.R
import java.io.FileNotFoundException
import java.util.*

class MusicService : Service(), OnPreparedListener,
    OnErrorListener,
    OnCompletionListener {
    //media player
    lateinit var player: MediaPlayer

    //song list
    private var songs: ArrayList<Song>? = null

    //current position
    private var songPosn = 0

    //binder
    private val musicBind: IBinder = MusicBinder()

    //title of current song
    private var songTitle = ""

    //shuffle flag and random
    private var shuffle = false
    private var rand: Random? = null
    lateinit var notiView: RemoteViews
    lateinit var notiViewExpanded: RemoteViews

    lateinit var notificationBuilder: NotificationCompat.Builder
    lateinit var mediaSession: MediaSessionCompat
    var playOrPause = R.drawable.ic_pause_white

    override fun onCreate() {
        //create the service
        super.onCreate()
        //initialize position
        songPosn = 0
        //random
        rand = Random()
        //create player
        player = MediaPlayer()

        mediaSession = MediaSessionCompat(this, "Music Player")

        //initialize
        initMusicPlayer()

        initLayout()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //notification()

        if (ACTIONS.PLAY_ACTION.equals(intent!!.action)) {
            if (player.isPlaying) {
                pausePlayer()
            } else {
                resumePlayer()
            }
        }

        if (ACTIONS.NEXT_ACTION.equals(intent!!.action)) {
            playNext()
        }

        if (ACTIONS.PREV_ACTION.equals(intent!!.action)) {
            playPrev()
        }

        if (ACTIONS.STOPFOREGROUND_ACTION.equals(intent!!.action)) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    fun initLayout() {
        notiView = RemoteViews(packageName, R.layout.statusbar)
        notiViewExpanded = RemoteViews(packageName, R.layout.statusbarexpanded)
    }

    fun initMusicPlayer() {
        //set player properties
        player!!.setWakeMode(
            applicationContext,
            PowerManager.PARTIAL_WAKE_LOCK
        )
        player!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        //set listeners
        player!!.setOnPreparedListener(this)
        player!!.setOnCompletionListener(this)
        player!!.setOnErrorListener(this)
    }

    //pass song list
    fun setList(theSongs: ArrayList<Song>?) {
        songs = theSongs
    }

    //pass song list
    fun getSongsList(): ArrayList<Song>? {
        return songs
    }

    //binder
    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    //activity will bind to service
    override fun onBind(intent: Intent): IBinder? {
        return musicBind
    }

    //release resources when unbind
    override fun onUnbind(intent: Intent): Boolean {
        //player!!.stop()
        //player!!.release()
        return false
    }

    //play a song
    fun playSong() {
        //play
        player!!.reset()
        //get song
        val playSong: Song = songs!![songPosn]
        //get title
        songTitle = playSong.songTitle
        //get id
        val currSong: Long = playSong.songID
        //set uri
        val trackUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            currSong
        )
        //set the data source
        try {
            Log.e("TAG", "URI: " + Uri.parse(playSong.uri))
            Log.e("TAG", "Track URI: " + trackUri)
            //player = MediaPlayer.create(applicationContext, Uri.parse(playSong.uri))
            player.setDataSource(applicationContext, Uri.parse(playSong.uri))
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error setting data source", e)
        }
        player!!.prepareAsync()
    }

    //set the song
    fun setSong(songIndex: Int) {
        songPosn = songIndex
    }

    override fun onCompletion(mp: MediaPlayer) {
        //check if playback has reached the end of a track
        Log.e("TAG", "Song Completed!")
        if (player!!.currentPosition > 0) {
            mp.reset()
            playNext()
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.v("MUSIC PLAYER", "Playback Error")
        mp.reset()
        return false
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onPrepared(mp: MediaPlayer) {
        //start playback
        mp.start()
        //notification
        val notIntent = Intent(this, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendInt = PendingIntent.getActivity(
            this, 0,
            notIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        /*val builder = Notification.Builder(this)
        builder.setContentIntent(pendInt)
            .setSmallIcon(R.drawable.ic_play_white)
            .setTicker(songTitle)
            .setOngoing(true)
            .setCustomContentView(notiView)
            .setCustomBigContentView(notiViewExpanded)
            .setContentTitle("Playing")
            .setContentText(songTitle)*/


        notiView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_white)
        notiViewExpanded.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_white)

        notification()
    }

    private fun notification() {

        var notificationbg: RelativeLayout
        val strtitle: String = songs!!.get(songPosn).songTitle
        val strartist: String = songs!!.get(songPosn).songArtist

        notiView.setTextViewText(R.id.status_bar_track_name, strtitle)
        notiView.setTextViewText(R.id.status_bar_artist_name, strartist)
        val playIntent = Intent(this, MusicService::class.java)
        playIntent.action = ACTIONS.PLAY_ACTION
        val pplayIntent = PendingIntent.getService(this, 0, playIntent, 0)
        notiView.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent)
        notiViewExpanded.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent)
        val nextIntent = Intent(this, MusicService::class.java)
        nextIntent.action = ACTIONS.NEXT_ACTION
        val nnextIntent = PendingIntent.getService(this, 0, nextIntent, 0)
        notiView.setOnClickPendingIntent(R.id.status_bar_next, nnextIntent)
        notiViewExpanded.setOnClickPendingIntent(R.id.status_bar_next, nnextIntent)
        val prevIntent = Intent(this, MusicService::class.java)
        prevIntent.action = ACTIONS.PREV_ACTION
        val pprevIntent = PendingIntent.getService(this, 0, prevIntent, 0)
        notiView.setOnClickPendingIntent(R.id.status_bar_prev, pprevIntent)
        notiViewExpanded.setOnClickPendingIntent(R.id.status_bar_prev, pprevIntent)
        val closeIntent = Intent(this, MusicService::class.java)
        closeIntent.action = ACTIONS.STOPFOREGROUND_ACTION
        val ccloseIntent = PendingIntent.getService(this, 0, closeIntent, 0)
        notiView.setOnClickPendingIntent(R.id.status_bar_collapse, ccloseIntent)
        notiViewExpanded.setOnClickPendingIntent(R.id.status_bar_collapse, ccloseIntent)

        notiViewExpanded.setTextViewText(R.id.status_bar_track_name, strtitle)
        notiViewExpanded.setTextViewText(R.id.status_bar_artist_name, strartist)
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, songs!!.get(songPosn).songID)
        notiViewExpanded.setImageViewUri(R.id.status_bar_album_art, albumArtUri)
        Glide.with(applicationContext)
            .load(albumArtUri)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(90)))
            .into(object : SimpleTarget<Drawable?>() {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable?>?
                ) {
                    notiView.setImageViewUri(R.id.iv_bg, albumArtUri)
                    notiViewExpanded.setImageViewUri(R.id.iv_bg, albumArtUri)
                }
            })

        val notIntent = Intent(this, PlayScreen::class.java)
        notIntent.setAction(Intent.ACTION_MAIN)
        notIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendInt = PendingIntent.getActivity(
            this, 0,
            notIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        var bitmap: Bitmap?
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, albumArtUri)
        } catch (e: FileNotFoundException) {
            bitmap = getBitmapFromDrawable(getDrawable(R.drawable.ic_album_art_default))
        }
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("my_service", "My Background Service")
        } else {
            ""
        }
        notificationBuilder = NotificationCompat.Builder(this, channelId)
        /*val notification = notificationBuilder.setOngoing(true)
            .setContentIntent(pendInt)
            .setSmallIcon(R.drawable.ic_play_white)
            .setContentTitle("Playing")
            .setContentText(songTitle)
            .setTicker(songTitle)
            .setCustomBigContentView(notiViewExpanded)
            .setCustomContentView(notiView)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
            .setPriority(PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()*/

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    songs!!.get(songPosn).songArtist
                )
                //.putString(MediaMetadata.METADATA_KEY_ALBUM, "Dark Side of the Moon")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songTitle)
                .build()
        )
        mediaSession.isActive = true

        //MediaSessionCompat

        var notification = NotificationCompat.Builder(this, channelId)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_play_white)
            .addAction(R.drawable.ic_prev_white, "Previous", pprevIntent) // #0
            .addAction(playOrPause, "Pause", pplayIntent) // #1
            .addAction(R.drawable.ic_next_white, "Next", nnextIntent) // #2
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setContentTitle(songTitle)
            .setContentText(songs!!.get(songPosn).songArtist)
            .setLargeIcon(bitmap)
            .build()
        startForeground(NOTIFY_ID, notification)
    }

    fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        try {
            val bitmap: Bitmap
            bitmap = Bitmap.createBitmap(
                drawable!!.intrinsicWidth,
                drawable!!.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable!!.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
            drawable!!.draw(canvas)
            return bitmap
        } catch (e: OutOfMemoryError) {
            // Handle the error
            return null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    //playback methods
    val posn: Int
        get() = player!!.currentPosition

    val dur: Int
        get() = player!!.duration

    val isPlaying: Boolean
        get() = player!!.isPlaying

    val songId: Int
        get() = songPosn

    fun pausePlayer() {
        player!!.pause()
        notiView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play_white)
        notiViewExpanded.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play_white)
        playOrPause = R.drawable.ic_play_white
        notification()
        notificationBuilder.setOngoing(false)
        notificationBuilder.setContentTitle("Paused")
        notificationBuilder.setAutoCancel(true)
        stopForeground(false)
    }

    fun resumePlayer() {
        val seekTo = posn
        player!!.start()
        player!!.seekTo(seekTo)
        notiView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_white)
        notiViewExpanded.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_white)
        playOrPause = R.drawable.ic_pause_white
        notification()
    }

    fun seek(posn: Int) {
        player!!.seekTo(posn)
    }

    fun go() {
        player!!.start()
        notiView.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_white)
        notiViewExpanded.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_white)
        playOrPause = R.drawable.ic_pause_white
        notification()
    }

    //skip to previous track
    fun playPrev() {
        songPosn--
        if (songPosn < 0) songPosn = songs!!.size - 1
        playSong()
    }

    //skip to next
    fun playNext() {
        if (shuffle) {
            var newSong = songPosn
            while (newSong == songPosn) {
                newSong = rand!!.nextInt(songs!!.size)
            }
            songPosn = newSong
        } else {
            songPosn++
            if (songPosn >= songs!!.size) songPosn = 0
        }
        playSong()
    }

    override fun onDestroy() {
        //stopForeground(true)
    }

    //toggle shuffle
    fun setShuffle() {
        shuffle = if (shuffle) false else true
    }

    companion object {
        //notification id
        private const val NOTIFY_ID = 1
    }
}
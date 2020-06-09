package music.player.Activities

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.ImageView
import android.widget.MediaController.MediaPlayerControl
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.triggertrap.seekarc.SeekArc
import jp.wasabeef.glide.transformations.BlurTransformation
import music.player.Models.Song
import music.player.R
import music.player.Services.MusicService
import music.player.Utilities.Utilities
import java.util.*


class PlayScreen : Activity(), MediaPlayerControl {

    lateinit var imageViewBg: ImageView
    lateinit var album_art: ImageView
    lateinit var control: ImageView
    lateinit var next: ImageView
    lateinit var prev: ImageView
    lateinit var txtProgress: TextView
    lateinit var musicTitle: TextView
    lateinit var artistName: TextView
    lateinit var seekBar: SeekArc

    lateinit var musicSrv: MusicService
    private var playIntent: Intent? = null

    //binding
    private var musicBound = false

    lateinit var songs: ArrayList<Song>
    var currSong: Int = 0

    var mHandler = Handler()
    var utils = Utilities()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN.equals(intent!!.action)) {
            Toast.makeText(this, "Pending Intent Recieved!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_screen)

        playIntent = Intent(this, MusicService::class.java)
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
        startService(playIntent)
    }

    fun init() {

        imageViewBg = findViewById(R.id.imageView)
        album_art = findViewById(R.id.album_art)
        control = findViewById(R.id.control)
        next = findViewById(R.id.next)
        prev = findViewById(R.id.prev)
        txtProgress = findViewById(R.id.txtProgress)
        musicTitle = findViewById(R.id.musicTitle)
        artistName = findViewById(R.id.artistName)
        seekBar = findViewById(R.id.seekBar)
        seekBar.progress = 0

        currSong = intent.getIntExtra("id", -1)

        updateUI()

        listeners()
    }

    fun updateUI() {

        currSong = musicSrv.songId
        musicTitle.text = songs!!.get(currSong).songTitle
        artistName.text = songs!!.get(currSong).songArtist
        setBackGroundImage(songs!!.get(currSong).songID)

        Handler().postDelayed({
            prev.isEnabled = true
            control.isEnabled = true
            next.isEnabled = true
            seekBar.max = musicSrv.dur
            updateProgressBar()
        }, 200)
    }

    fun listeners() {
        seekBar.setOnSeekArcChangeListener(object : SeekArc.OnSeekArcChangeListener {
            override fun onProgressChanged(
                seekArc: SeekArc?,
                progress: Int,
                fromUser: Boolean
            ) {

            }

            override fun onStartTrackingTouch(seekArc: SeekArc?) {
                mHandler.removeCallbacks(mUpdateTimeTask)
            }

            override fun onStopTrackingTouch(seekArc: SeekArc?) {
                Log.e("TAG", "Progress: " + seekArc!!.progress)
                musicSrv.seek(seekArc!!.progress)
                updateProgressBar()
            }

        })

        control.setOnClickListener {
            control.isEnabled = false
            if (musicSrv.isPlaying) {
                musicSrv.pausePlayer()
                control.setImageResource(R.drawable.ic_play_white)
            } else {
                musicSrv.resumePlayer()
                control.setImageResource(R.drawable.ic_pause_white)
            }
            updateUI()
        }

        next.setOnClickListener {
            next.isEnabled = false
            musicSrv.playNext()
            //currSong++
            updateUI()
        }

        prev.setOnClickListener {
            prev.isEnabled = false
            musicSrv.playPrev()
            //currSong--
            updateUI()
        }

        musicSrv.player.setOnCompletionListener {
            if (musicSrv.player!!.currentPosition > 0) {
                musicSrv.player.reset()
                musicSrv.playNext()
                //currSong++
                updateUI()

            }
        }

    }

    fun updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 10)
    }

    var mUpdateTimeTask: Runnable = object : Runnable {
        override fun run() {
            //val currentDuration = musicSrv.dur
            val progress: Int = musicSrv.posn
            if (!musicSrv.isPlaying) {
                txtProgress.setText("" + utils.milliSecondsToTimer(progress.toLong()))
            } else {
                txtProgress.setText(
                    "" + utils.milliSecondsToTimer(progress.toLong())
                )
                seekBar.setProgress(progress)
            }
            mHandler.postDelayed(this, 10)
        }
    }

    private fun setBackGroundImage(song_id: Long) {
        val sArtworkUri: Uri = Uri
            .parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, song_id)

        Glide.with(applicationContext)
            .load(albumArtUri)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(80)))
            .into(imageViewBg)

        Glide.with(applicationContext)
            .load(albumArtUri)
            .apply(RequestOptions.circleCropTransform())
            .into(album_art)
    }

    //connect to the service
    private val musicConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: MusicService.MusicBinder = service as MusicService.MusicBinder
            //get service
            musicSrv = binder.service
            //get list
            songs = ArrayList()
            songs = musicSrv.getSongsList()!!
            init()
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun getCurrentPosition(): Int {
        updateProgressBar()
        return if (musicSrv != null && musicBound && musicSrv!!.isPlaying) musicSrv!!.posn else 0
    }

    override fun getDuration(): Int {
        return if (musicSrv != null && musicBound && musicSrv!!.isPlaying) musicSrv!!.dur else 0
    }

    override fun isPlaying(): Boolean {
        if (musicSrv != null && musicBound) {

            return musicSrv!!.isPlaying
        } else {
            return false
        }
    }

    override fun pause() {
        musicSrv!!.pausePlayer()
    }

    override fun seekTo(pos: Int) {
        musicSrv!!.seek(pos)
        updateProgressBar()
    }

    override fun start() {
        musicSrv!!.go()
    }

}
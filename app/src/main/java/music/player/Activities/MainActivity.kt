package music.player.Activities

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.MediaController.MediaPlayerControl
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.row_song.view.*
import music.player.Adapters.SongAdapter
import music.player.Controller.MusicController
import music.player.DataBase.SharedPrefManager
import music.player.Models.Song
import music.player.R
import music.player.Services.MusicService
import java.util.*


class MainActivity : Activity(), MediaPlayerControl {
    //song list variables
    private var songList: ArrayList<Song>? = null
    private var songView: ListView? = null

    //service
    private var musicSrv: MusicService? = null
    private var playIntent: Intent? = null

    //binding
    private var musicBound = false

    //controller
    private var controller: MusicController? = null

    //activity and playback pause flags
    private var paused = false
    private var playbackPaused = false

    lateinit var songAdt: SongAdapter

    lateinit var ll_main: LinearLayout
    lateinit var ll_secondary: LinearLayout

    var lastPlayed: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionBar?.setDisplayOptions(
            ActionBar.DISPLAY_SHOW_HOME or
                    ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_HOME_AS_UP or ActionBar.DISPLAY_USE_LOGO
        )
        actionBar?.setIcon(R.drawable.logo)

        var toolbar: Toolbar = findViewById(R.id.toolbar)
        //toolbar.inflateMenu(R.menu.main)

        init()
        checkPermission()
        Handler().postDelayed({
            lastPlayedSong()
        }, 1000)
    }

    private fun lastPlayedSong() {
        if (SharedPrefManager.getInstance(this).lastClicked != -1) {
            //songPicked(songView!!.getChildAt(SharedPrefManager.getInstance(this).lastClicked))
            val view = songView!!.getChildAt(SharedPrefManager.getInstance(this).lastClicked)
            if (view != null) {
                setBackGroundImage(songList!!.get(view.tag.toString().toInt()).songID)
                musicSrv!!.setSong(view.tag.toString().toInt())
                //musicSrv!!.playSong()
                if (playbackPaused) {
                    setController()
                    playbackPaused = true
                }
                controller!!.show(0)
            }
        }
    }

    fun init() {
        ll_main = findViewById(R.id.ll_main)
        ll_secondary = findViewById(R.id.ll_secondary)
        //retrieve list view
        songView = findViewById<View>(R.id.song_list) as ListView
        //instantiate list
        songList = ArrayList<Song>()
        //getSongList()
        //sort alphabetically by title
        Collections.sort(
            songList,
            Comparator<Song> { a: Song, b: Song -> a.songTitle.compareTo(b.songTitle) })
        //create and set adapter
        songAdt = SongAdapter(this, songList!!)
        songView!!.adapter = songAdt

        songView!!.setOnItemClickListener { adapterView, view, i, l ->
            if (lastPlayed != -1 && adapterView.getChildAt(lastPlayed) != null) {
                adapterView.getChildAt(lastPlayed).iv_playing.visibility = View.GONE
            }
            if (adapterView.getChildAt(i) != null) {
                adapterView.getChildAt(i).iv_playing.visibility = View.VISIBLE
            }
            songPicked(view)
            lastPlayed = i
            SharedPrefManager.getInstance(this).lastPlayedSong(lastPlayed)
        }

        //setup controller
        setController()
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 123
            )
            return
        } else {
            Log.e("TAG", "Has Permission!")
            getSongList()
        }

    }

    //connect to the service
    private val musicConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: MusicService.MusicBinder = service as MusicService.MusicBinder
            //get service
            musicSrv = binder.service
            //pass list
            musicSrv!!.setList(songList)
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    //start and bind the service when the activity starts
    override fun onStart() {
        super.onStart()
        if (playIntent == null) {
            playIntent = Intent(this, MusicService::class.java)
            bindService(
                Intent(this, MusicService::class.java),
                musicConnection,
                Context.BIND_AUTO_CREATE
            )
            startService(playIntent)
        }
    }

    //user song select
    fun songPicked(view: View) {
        Log.e("TAG", "View: " + view.tag.toString())
        setBackGroundImage(songList!!.get(view.tag.toString().toInt()).songID)
        musicSrv!!.setSong(view.tag.toString().toInt())
        musicSrv!!.playSong()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(0)

        val intent = Intent(this@MainActivity, PlayScreen::class.java)
        intent.putExtra("id", view.tag.toString().toInt())
        startActivity(intent)
        //finish()
    }

    private fun setBackGroundImage(song_id: Long) {
        val sArtworkUri: Uri = Uri
            .parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, song_id)
        Glide.with(this@MainActivity)
            .load(albumArtUri)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(90)))
            .into(object : SimpleTarget<Drawable?>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable?>?
                ) {
                    ll_main.setBackground(resource)
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //menu item selected
        when (item.itemId) {
            R.id.action_shuffle -> musicSrv!!.setShuffle()
            R.id.action_end -> {
                stopService(playIntent)
                musicSrv = null
                System.exit(0)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //method to retrieve song info from device
    fun getSongList() {
        val musicCursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            null,
            "is_music!=0",
            null,
            "_display_name ASC"
        )
        if (musicCursor != null) {
            if (musicCursor.moveToFirst()) {
                do {
                    val thisId =
                        musicCursor.getLong(musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))
                    val thisURI =
                        musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val thisTitle =
                        musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))
                    val thisArtist =
                        musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                    songList!!.add(Song(thisURI, thisId, thisTitle, thisArtist))
                } while (musicCursor.moveToNext())
                songAdt.notifyDataSetChanged()
            }
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
        playbackPaused = true
        musicSrv!!.pausePlayer()
    }

    override fun seekTo(pos: Int) {
        musicSrv!!.seek(pos)
    }

    override fun start() {
        musicSrv!!.go()
    }

    //set the controller up
    private fun setController() {
        controller = MusicController(this)
        //set previous and next button listeners
        controller!!.setPrevNextListeners(
            View.OnClickListener { playNext() },
            View.OnClickListener { playPrev() })
        //set and show
        controller!!.setMediaPlayer(this)
        controller!!.setAnchorView(findViewById(R.id.song_list))
        controller!!.setEnabled(true)
    }

    private fun playNext() {
        musicSrv!!.playNext()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(0)
        setBackGroundImage(songList!!.get(musicSrv!!.songId).songID)
    }

    private fun playPrev() {
        musicSrv!!.playPrev()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller!!.show(0)
        setBackGroundImage(songList!!.get(musicSrv!!.songId).songID)
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if (paused) {
            setController()
            paused = false
        }
    }

    override fun onStop() {
        controller!!.hide()
        super.onStop()
    }

    override fun onDestroy() {
        //stopService(playIntent)
        //musicSrv = null
        super.onDestroy()
    }

    @Override
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getSongList()
            } else {
                // User refused to grant permission.
                checkPermission()
            }
        }


    }
}
package music.player.Activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.MediaController
import com.triggertrap.seekarc.SeekArc
import music.player.Interfaces.PlayerAdapter
import music.player.R
import music.player.Services.MusicService
import android.widget.MediaController.MediaPlayerControl

class PlayScreen  : Activity(){

    lateinit var imageViewBg: ImageView
    lateinit var seekBar: SeekArc

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_screen)

        init()
    }

    fun init(){

        imageViewBg = findViewById(R.id.imageView)
        seekBar = findViewById(R.id.seekBar)

    }

}
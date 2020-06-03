package music.player.Adapters

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import music.player.Models.Song
import music.player.R
import java.util.*


/**
 * Created by Huzaifa Asif on 6/3/2020.
 * Symtera Technologies pvt ltd
 * huzaifano1@hotmail.com
 **/

class SongAdapter(c: Context?, theSongs: ArrayList<Song>) : BaseAdapter() {
    //song list and layout
    private val songs: ArrayList<Song>
    private val songInf: LayoutInflater
    private val context: Context
    override fun getCount(): Int {
        return songs.size
    }

    override fun getItem(arg0: Int): Any? {
        return null
    }

    override fun getItemId(arg0: Int): Long {
        return 0
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        //map to song layout
        val songLay = songInf.inflate(R.layout.row_song, parent, false)
        //get title and artist views
        val songView = songLay.findViewById<View>(R.id.tvSongName) as TextView
        val artistView = songLay.findViewById<View>(R.id.tvArtistName) as TextView
        val albumArt = songLay.findViewById<View>(R.id.album_art) as ImageView
        val currSong: Song = songs[position]
        songView.setText(currSong.songTitle)
        artistView.setText(currSong.songArtist)
        val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        val albumArtUri: Uri = ContentUris.withAppendedId(sArtworkUri, currSong.songID)
        Glide.with(context).load(albumArtUri)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(1))).into(albumArt)
        songLay.tag = position
        return songLay
    }

    //constructor
    init {
        songs = theSongs
        songInf = LayoutInflater.from(c)
        context = c!!
    }
}
package music.player.DataBase

import android.content.Context
import music.player.Models.Song


/**
 * Created by Huzaifa Asif on 6/5/2020.
 * Symtera Technologies pvt ltd
 * huzaifano1@hotmail.com
 **/

class SharedPrefManager private constructor(private val mCtx: Context) {

    fun saveSong(song: Song) {
        val sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("id", song.songID)
        editor.putString("title", song.songTitle)
        editor.putString("artist", song.songArtist)
        editor.putString("uri", song.uri)
        editor.apply()
    }

    fun lastPlayedSong(id: Int) {
        val sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("lastClicked", id)
        editor.apply()
    }

    val lastClicked: Int
        get() {
            val sharedPreferences =
                mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getInt("lastClicked", -1)
        }

    val songId: Long
        get() {
            val sharedPreferences =
                mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getLong("id", -1)
        }

    val songTitle: String
        get() {
            val sharedPreferences =
                mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString("title", "").toString()
        }

    val songArtist: String
        get() {
            val sharedPreferences =
                mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString("artist", "").toString()
        }

    val songUri: String
        get() {
            val sharedPreferences =
                mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString("uri", "").toString()
        }

    fun clear() {
        val sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        editor.commit()
    }

    companion object {
        private val SHARED_PREF_NAME = "music_prefs"
        private var mInstance: SharedPrefManager? = null

        @Synchronized
        fun getInstance(mCtx: Context): SharedPrefManager {
            if (mInstance == null) {
                mInstance = SharedPrefManager(mCtx)
            }
            return mInstance as SharedPrefManager
        }
    }
}
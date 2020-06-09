package music.player.Utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import music.player.R
import java.io.ByteArrayInputStream
import java.io.InputStream


/**
 * Created by Huzaifa Asif on 6/4/2020.
 * Symtera Technologies pvt ltd
 * huzaifano1@hotmail.com
 **/

class Utilities() {
    fun songArt(path: String, context: Context): Bitmap {
        val retriever = MediaMetadataRetriever()
        val inputStream: InputStream
        retriever.setDataSource(path)
        if (retriever.embeddedPicture != null) {
            inputStream = ByteArrayInputStream(retriever.embeddedPicture)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            retriever.release()
            return bitmap
        } else {
            return getLargeIcon(context)
        }
    }

    private fun getLargeIcon(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.ic_album_art_default)
    }

    fun milliSecondsToTimer(milliseconds: Long): String {
        val secondsString: String
        var finalTimerString = ""
        val str = ""
        val hours = (milliseconds / 3600000).toInt()
        val minutes = (milliseconds % 3600000).toInt() / 60000
        val seconds = (milliseconds % 3600000 % 60000 / 1000).toInt()
        if (hours > 0) {
            finalTimerString = "$hours:"
        }
        if (seconds < 10) {
            secondsString = "0$seconds"
        } else {
            secondsString = "" + seconds
        }
        return "$finalTimerString$minutes:$secondsString"
    }

    fun getProgressPercentage(currentDuration: Long, totalDuration: Long): Int {
        val valueOf = java.lang.Double.valueOf(0.0)
        return java.lang.Double.valueOf(
            ((currentDuration / 1000).toInt().toLong().toDouble() / ((totalDuration / 1000).toInt()
                .toLong().toDouble())) * 100.0
        )
            .toInt()
    }

    fun progressToTimer(progress: Int, totalDuration: Int): Int {
        return ((((progress.toDouble()) / 100.0) * ((totalDuration / 1000).toDouble())).toInt()) * 1000
    }
}
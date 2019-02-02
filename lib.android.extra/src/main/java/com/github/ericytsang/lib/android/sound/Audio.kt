package com.github.ericytsang.lib.android.sound

import android.content.Context
import android.media.MediaPlayer
import com.github.ericytsang.lib.android.resource.NormalizedUri
import com.github.ericytsang.lib.android.resource.RawResId
import com.github.ericytsang.lib.domainobjects.DataObject
import java.io.Closeable

object Audio
{
    class Player
    private constructor(
            val data:Data,
            private val mediaPlayer:MediaPlayer)
        :
            Closeable
    {
        companion object
        {
            fun create(data:Data,context:Context):Player?
            {
                return when(data)
                {
                    is Audio.Data.Uri -> Player(
                            data,
                            MediaPlayer.create(context,data.uri.uri) ?: return null)
                    is Audio.Data.ResId -> Player(
                            data,
                            MediaPlayer.create(context,data.rawResId.id) ?: return null)
                }
            }
        }

        fun play()
        {
            mediaPlayer.start()
        }

        override fun close()
        {
            mediaPlayer.release()
        }
    }

    sealed class Data:DataObject
    {

        abstract val name:String

        data class Uri(
                val uri:NormalizedUri)
            :Data()
        {
            override val name:String get()
            {
                return uri.uri.let {it.lastPathSegment ?: it.toString()}
            }
        }

        data class ResId(
                override val name:String,
                val rawResId:RawResId)
            :Data()
    }
}

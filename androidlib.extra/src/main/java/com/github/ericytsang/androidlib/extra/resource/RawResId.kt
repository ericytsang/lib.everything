package com.github.ericytsang.androidlib.extra.resource

import android.content.Context
import com.github.ericytsang.lib.domainobjects.Either

private interface IRawResId
{
    val id:Int
}

class RawResId
private constructor(
        data:Data)
    :IRawResId by data
{
    companion object
    {
        fun create(context:Context,rawResId:Int):Either<RawResId,Throwable>
        {
            try
            {
                context.resources.openRawResource(rawResId).close()
            }
            catch (e:Throwable)
            {
                return Either.B(e)
            }
            return Either.A(RawResId(Data(rawResId)))
        }
    }

    private data class Data(
            override val id:Int)
        :IRawResId
}

package com.github.ericytsang.lib.android.resource

import android.net.Uri
import com.github.ericytsang.lib.domainobjects.DataObject

private interface INormalizedUri:DataObject
{
    val uri:Uri
}

class NormalizedUri
private constructor(data:Data)
    :INormalizedUri by data
{
    companion object
    {
        fun fromAnyUri(uri:Uri) = NormalizedUri(Data(uri.normalizeScheme()))
    }

    private data class Data(
            override val uri:Uri)
        :INormalizedUri
}

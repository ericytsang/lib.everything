package com.github.ericytsang.lib.datastore

fun <E:Any> Iterator<E>.nextOrNull():E?
{
    return if (hasNext()) next() else null
}

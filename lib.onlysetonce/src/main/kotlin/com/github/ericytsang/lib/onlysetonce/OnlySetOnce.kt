package com.github.ericytsang.lib.onlysetonce

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class OnlySetOnce<in R,T:Any?>:ReadWriteProperty<R,T?>
{
    private var field:T? = null

    private val mutex = ReentrantLock()

    override fun getValue(thisRef:R,property:KProperty<*>):T?
    {
        return field
    }

    override fun setValue(thisRef:R,property:KProperty<*>,value:T?)
    {
        require(value != null)
        mutex.withLock()
        {
            if (field != null)
            {
                throw Exception("field has already been set")
            }
            field = value
        }
    }

    class Exception(reason:String):RuntimeException(reason)
}

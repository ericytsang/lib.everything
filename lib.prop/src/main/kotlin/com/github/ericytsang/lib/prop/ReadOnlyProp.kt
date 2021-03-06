package com.github.ericytsang.lib.prop

import java.io.Closeable
import java.io.Serializable

interface ReadOnlyProp<Context:Any,Value:Any>:Serializable
{
    /**
     * returns the value for this [ReadOnlyProp].
     */
    operator fun get(context:Context):Value

    /**
     * will call [onChanged] when [ReadOnlyProp]'s value has changed.
     * returns a [Closeable] that will unsubscribe [onChanged] from change
     * events so that it will not receive any more change events in the future.
     */
    fun listen(onChanged:(ReadOnlyProp<Context,Value>)->Unit):Closeable
}

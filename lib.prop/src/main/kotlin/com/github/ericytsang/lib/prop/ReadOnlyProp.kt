package com.github.ericytsang.lib.prop

import java.io.Closeable
import java.io.Serializable

interface ReadOnlyProp<Context:Any,Value:Any>:Serializable
{
    /**
     * returns the value for this [ReadOnlyProp].
     */
    fun get(context:Context):Value

    /**
     * returns the [Change] object that is being dispatched to all of its listeners for this property.
     * will throw an exception if called outside of the thread of a listener
     */
    fun getChange():ReadOnlyProp.Change<Context,Value>

    /**
     * will call [onChanged] before returning.
     * will call [onChanged] when [ReadOnlyProp]'s value has changed.
     * returns a [Closeable] that will unsubscribe [onChanged] from change
     * events so that it will not receive any more change events in the future.
     */
    fun listen(context:Context,onChanged:(Change<Context,Value>)->Unit):Closeable

    /**
     * will call [onChanged] when [ReadOnlyProp]'s value has changed.
     * returns a [Closeable] that will unsubscribe [onChanged] from change
     * events so that it will not receive any more change events in the future.
     */
    fun listen(onChanged:(Change<Context,Value>)->Unit):Closeable

    data class Change<Context:Any,Value:Any>(
            val source:ReadOnlyProp<Context,Value>,
            val context:Context,
            val oldValue:Value,
            val newValue:Value)
}

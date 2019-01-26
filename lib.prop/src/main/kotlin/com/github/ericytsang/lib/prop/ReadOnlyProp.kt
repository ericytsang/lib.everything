package com.github.ericytsang.lib.prop

import java.io.Closeable

interface ReadOnlyProp<Context:Any,Value:Any>
{
    /**
     * returns the value for this [ReadOnlyProp].
     */
    fun get(context:Context):Value

    /**
     * returns the value for this [ReadOnlyProp]; null if it is being unset.
     */
    fun getNullable(context:Context):Value?

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

    sealed class Change<Context:Any,Value:Any>
    {
        abstract val source:ReadOnlyProp<Context,Value>
        abstract val context:Context
        abstract val oldValue:Value
        abstract val newValue:Value
        abstract val nowValue:Value

        data class Before<Context:Any,Value:Any>(
                override val source:ReadOnlyProp<Context,Value>,
                override val context:Context,
                override val oldValue:Value,
                override val newValue:Value)
            :Change<Context,Value>()
        {
            override val nowValue:Value get() = oldValue
        }

        data class After<Context:Any,Value:Any>(
                override val source:ReadOnlyProp<Context,Value>,
                override val context:Context,
                override val oldValue:Value,
                override val newValue:Value)
            :Change<Context,Value>()
        {
            override val nowValue:Value get() = newValue
        }
    }
}
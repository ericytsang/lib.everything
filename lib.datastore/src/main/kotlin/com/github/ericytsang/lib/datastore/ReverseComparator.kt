package com.github.ericytsang.lib.datastore

import java.io.Serializable
import java.util.Comparator

/**
 * Created by surpl on 4/9/2018.
 */
private data class ReverseComparator2<T> internal constructor(
        /**
         * The comparator specified in the static factory.  This will never
         * be null, as the static factory returns a ReverseComparator
         * instance if its argument is null.
         *
         * @serial
         */
        internal val cmp:Comparator<T>)
    :Comparator<T>,Serializable
{
    override fun compare(t1:T,t2:T):Int
    {
        return cmp.compare(t2,t1)
    }
}

fun <T> Comparator<T>.reversedCompat():Comparator<T>
{
    return (this as? ReverseComparator2)?.cmp ?: ReverseComparator2(this)
}

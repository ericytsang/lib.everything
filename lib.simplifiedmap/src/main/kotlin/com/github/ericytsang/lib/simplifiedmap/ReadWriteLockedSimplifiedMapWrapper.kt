package com.github.ericytsang.lib.simplifiedmap

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class ReadWriteLockedSimplifiedMapWrapper<K,V:Any>(val underlying:SimplifiedMap<K,V>):ReadWriteLockedSimplifiedMap<K,V>
{
    override val readWriteLock = ReentrantReadWriteLock()

    override fun set(key:K,value:V?):V?
    {
        check(readWriteLock.isWriteLockedByCurrentThread)
        return underlying.set(key,value)
    }

    override fun clear()
    {
        check(readWriteLock.isWriteLockedByCurrentThread)
        super.clear()
    }

    override fun put(key:K,value:V):V?
    {
        check(readWriteLock.isWriteLockedByCurrentThread)
        return super.put(key,value)
    }

    override fun putAll(from:Map<out K,V>)
    {
        check(readWriteLock.isWriteLockedByCurrentThread)
        super.putAll(from)
    }

    override fun rm(key:K):V?
    {
        check(readWriteLock.isWriteLockedByCurrentThread)
        return super.rm(key)
    }

    override fun get(key:K):V? = read {underlying[key]}

    override val size:Int get() = read {super.size}

    override fun isEmpty():Boolean = read {super.isEmpty()}

    override fun containsKey(key:K):Boolean = read {super.containsKey(key)}

    override fun containsValue(value:V):Boolean = read {super.containsValue(value)}

    override val entries:Set<Map.Entry<K,V>> get() = read {super.entries}

    override val keys:Set<K> get() = read {underlying.keys}

    override val values:Collection<V> get() = read {super.values}

    override fun equals(other:Any?):Boolean = if (other is Map<*,*>)
    {
        read {
            other.entries == entries
        }
    }
    else
    {
        false
    }

    override fun hashCode():Int = read {underlying.hashCode()}

    override fun toString():String = read {underlying.toString()}

    private fun <R> read(block:()->R):R = if (readWriteLock.isWriteLockedByCurrentThread)
    {
        block()
    }
    else
    {
        readWriteLock.read(block)
    }
}

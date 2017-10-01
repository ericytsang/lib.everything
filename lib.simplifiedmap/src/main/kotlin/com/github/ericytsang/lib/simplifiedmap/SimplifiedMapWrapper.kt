package com.github.ericytsang.lib.simplifiedmap

class SimplifiedMapWrapper<K,V:Any>(val underlying:MutableMap<K,V>):SimplifiedMap<K,V>
{
    override val keys:Set<K> get() = underlying.keys

    override fun get(key:K):V? = underlying[key]

    override fun set(key:K,value:V?):V? = if (value == null)
    {
        underlying.remove(key)
    }
    else
    {
        underlying.put(key,value)
    }

    override fun equals(other:Any?):Boolean = if (other is Map<*,*>)
    {
        other.entries == entries
    }
    else
    {
        false
    }

    override fun hashCode():Int = underlying.hashCode()

    override fun toString():String = underlying.toString()
}

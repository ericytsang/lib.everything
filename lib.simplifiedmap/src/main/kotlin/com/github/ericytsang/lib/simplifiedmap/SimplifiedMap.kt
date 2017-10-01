package com.github.ericytsang.lib.simplifiedmap

import java.util.AbstractMap

interface SimplifiedMap<K,V:Any>:Map<K,V>
{
    /**
     * @param key key in the map to remap to [value].
     * @param value if null, remove the entry such that [get] ([key]) == null.
     * else, remap [key] to [value] such that [get] ([key]) == [value].
     * @return the value that was previously associated with [key] or null if
     * the entry did not previously exist.
     */
    operator fun set(key:K,value:V?):V?

    /**
     * @param key the key associated with the [V] to return.
     * @return the value that is associated with [key] or null if the entry
     * doesn't exist.
     */
    override operator fun get(key:K):V?

    /**
     * set of all [K]s that are associated with a [V].
     */
    override val keys:Set<K>

    override val size:Int get() = keys.size

    fun put(key:K,value:V):V? = set(key,value)

    fun putAll(from:Map<out K,V>) = from.entries.forEach {put(it.key,it.value)}

    fun rm(key:K):V? = set(key,null)

    override fun containsKey(key:K):Boolean = key in keys

    override fun containsValue(value:V):Boolean = value in values

    override fun isEmpty():Boolean = size == 0

    fun clear()
    {
        while (keys.isNotEmpty())
        {
            rm(keys.first())
        }
    }

    override val values:Collection<V> get() = keys.map {get(it)!!}

    override val entries:Set<Map.Entry<K,V>> get() = keys.map {AbstractMap.SimpleEntry(it,get(it)!!)}.toSet()
}

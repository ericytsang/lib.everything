package com.github.ericytsang.lib.datastore

sealed class Index<Indexer:Comparable<Indexer>>:Comparable<Index<Indexer>>
{
    class Min<Indexer:Comparable<Indexer>>:Index<Indexer>()
    {
        override fun compareTo(other:Index<Indexer>):Int = when(other)
        {
            is Min -> 0
            is Max,
            is Mid -> -1
        }
    }

    data class Mid<Indexer:Comparable<Indexer>>(val index:Indexer):Index<Indexer>()
    {
        override fun compareTo(other:Index<Indexer>):Int = when(other)
        {
            is Min -> 1
            is Max -> -1
            is Mid -> index.compareTo(other.index)
        }
    }

    class Max<Indexer:Comparable<Indexer>>:Index<Indexer>()
    {
        override fun compareTo(other:Index<Indexer>):Int = when(other)
        {
            is Max -> 0
            is Min,
            is Mid -> 1
        }
    }
}
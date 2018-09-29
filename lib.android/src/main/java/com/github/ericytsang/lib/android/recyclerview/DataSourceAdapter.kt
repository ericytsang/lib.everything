package com.github.ericytsang.lib.android.recyclerview

import android.arch.paging.ItemKeyedDataSource
import com.github.ericytsang.lib.awaitable.Awaitable
import com.github.ericytsang.lib.awaitable.SimpleAwaiter
import com.github.ericytsang.lib.datastore.Index
import com.github.ericytsang.lib.datastore.Pager
import com.github.ericytsang.lib.setofatleastone.SetOfAtLeastOne
import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DataSourceAdapter<I:Comparable<I>,Item:Any>(
        val pager:Pager<Index<I>,Item>,
        val signalledWhenDatasourceInvalidated:Awaitable,
        val toIndex:(Item)->Index<I>)
    :ItemKeyedDataSource<Index<I>,Item>()
{
    override fun loadInitial(
            params:LoadInitialParams<Index<I>>,
            callback:LoadInitialCallback<Item>)
    {
        val key = params.requestedInitialKey

        // load from beginning of the data source
        val items = if (key == null)
        {
            pager.page(Index.Min(),Pager.Order.ASC,params.requestedLoadSize)
        }

        // load around the provided key
        else
        {
            val pageUp = pager.page(key,Pager.Order.ASC,params.requestedLoadSize)
            val smallestKeyInPageUp = pageUp.map {getKey(it)}.min()
                    ?: Index.Max()
            val pageDn = pager.page(key,Pager.Order.DSC,params.requestedLoadSize)
                    .asReversed().filter {getKey(it) < smallestKeyInPageUp}
            pageDn+pageUp
        }

        // return items
        callback.onResult(items)
    }

    override fun loadAfter(
            params:LoadParams<Index<I>>,
            callback:LoadCallback<Item>)
    {
        pager
                .page(params.key,Pager.Order.ASC,params.requestedLoadSize+1)
                .filter {getKey(it) > params.key}
                .let {callback.onResult(it)}
    }

    override fun loadBefore(params:LoadParams<Index<I>>,callback:LoadCallback<Item>)
    {
        pager
                .page(params.key,Pager.Order.DSC,params.requestedLoadSize+1)
                .asReversed()
                .filter {getKey(it) < params.key}
                .let {callback.onResult(it)}
    }

    override fun getKey(item:Item):Index<I>
    {
        return toIndex(item)
    }

    private var listeningState:ListeningState = NoCallbackListeners(signalledWhenDatasourceInvalidated.updateStamp)
        set(value)
        {
            field.close()
            field = value
        }
    private val listeningStateLock = ReentrantLock()

    override fun invalidate() = listeningStateLock.withLock()
    {
        listeningState.invalidate()
        super.invalidate()
    }

    override fun removeInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback) = listeningStateLock.withLock()
    {
        listeningState.removeInvalidatedCallback(onInvalidatedCallback)
        super.removeInvalidatedCallback(onInvalidatedCallback)
    }

    override fun addInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback) = listeningStateLock.withLock()
    {
        listeningState.addInvalidatedCallback(onInvalidatedCallback)
        super.addInvalidatedCallback(onInvalidatedCallback)
    }

    private interface ListeningState:Closeable
    {
        fun invalidate()
        fun removeInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback)
        fun addInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback)
    }

    inner class NoCallbackListeners(
            val initialUpdateStamp:Long)
        :ListeningState
    {
        override fun invalidate()
        {
            listeningState = Invalidated()
        }

        override fun removeInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback) = Unit

        override fun addInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback)
        {
            listeningState = AtLeastOneCallbackListener(this,SetOfAtLeastOne.of(onInvalidatedCallback))
        }

        override fun close() = Unit
    }

    inner class AtLeastOneCallbackListener(
            val noCallbackListeners:NoCallbackListeners,
            callbackListeners:SetOfAtLeastOne<InvalidatedCallback>)
        :ListeningState
    {
        private val listeners = callbackListeners.toMutableSet()
        private val invalidater = SimpleAwaiter.fromLambda(signalledWhenDatasourceInvalidated,noCallbackListeners.initialUpdateStamp)
        {
            this@DataSourceAdapter.invalidate()
        }

        override fun invalidate()
        {
            listeningState = Invalidated()
        }

        override fun removeInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback)
        {
            listeners.remove(onInvalidatedCallback)
            if (listeners.isEmpty())
            {
                listeningState = NoCallbackListeners(noCallbackListeners.initialUpdateStamp)
            }
        }

        override fun addInvalidatedCallback(onInvalidatedCallback:InvalidatedCallback)
        {
            listeners.add(onInvalidatedCallback)
        }

        override fun close()
        {
            invalidater.close()
        }
    }

    inner class Invalidated:ListeningState
    {
        override fun invalidate() = Unit
        override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) = Unit
        override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) = Unit
        override fun close() = Unit
    }
}

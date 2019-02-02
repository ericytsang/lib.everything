package com.github.ericytsang.lib.android.extra

import android.os.Handler
import android.os.Looper
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object UiThreadExecutor:Executor
{
    private val mHandler = Handler(Looper.getMainLooper())

    override fun execute(command:Runnable)
    {
        mHandler.post(command)
    }

    fun <R> runIfIsUiThreadElsePost(block:()->R):Future<R>
    {
        val future = FutureTask(block)
        if (Thread.currentThread().isUiThread())
        {
            future.run()
        }
        else
        {
            mHandler.post()
            {
                future.run()
            }
        }
        return future
    }
}

// todo: move to another module
class PiggyBackExecutorService(
        private val executor:Executor)
    :AbstractExecutorService()
{
    private val lock = ReentrantLock()
    private val pendingCommands = LinkedBlockingQueue<Runnable>()
    private var shutdownCallSite:Throwable? = null
    private var isTerminated = false
    private val isTerminatedCondition = lock.newCondition()

    override fun execute(command:Runnable)
    {
        lock.withLock()
        {
            if (shutdownCallSite == null)
            {
                pendingCommands.put(command)
                executor.execute()
                {
                    pendingCommands.poll()?.run()
                    lock.withLock()
                    {
                        isTerminated = isTerminated || (isShutdown && pendingCommands.isEmpty())
                        isTerminatedCondition.signalAll()
                    }
                }
            }
            else
            {
                throw RejectedExecutionException("already shutdown",shutdownCallSite)
            }
        }
    }

    override fun isTerminated():Boolean
    {
        return isTerminated
    }

    override fun shutdown()
    {
        lock.withLock()
        {
            if (shutdownCallSite == null)
            {
                shutdownCallSite = Throwable("shutdown was called here")
            }
        }
    }

    override fun shutdownNow():MutableList<Runnable>
    {
        return lock.withLock()
        {
            if (shutdownCallSite == null)
            {
                shutdownCallSite = Throwable("shutdown was called here")
            }
            generateSequence {pendingCommands.poll()}.toMutableList()
        }
    }

    override fun isShutdown():Boolean
    {
        return shutdownCallSite != null
    }

    override fun awaitTermination(timeout:Long,unit:TimeUnit):Boolean
    {
        return lock.withLock()
        {
            if (!isTerminated)
            {
                isTerminatedCondition.await(timeout,unit)
            }
            else
            {
                true
            }
        }
    }
}

fun Thread.isUiThread():Boolean
{
    return this == Looper.getMainLooper().thread
}

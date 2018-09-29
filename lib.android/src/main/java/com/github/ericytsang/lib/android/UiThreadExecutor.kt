package com.github.ericytsang.lib.android

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

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

fun Thread.isUiThread():Boolean
{
    return this == Looper.getMainLooper().thread
}

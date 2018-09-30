package com.github.ericytsang.lib.concurrent

import java.io.Serializable
import java.util.concurrent.ExecutorService
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/**
 * returns a future task that has begun execution right away
 */
fun <V> future(
    isThreaded:Boolean = true,
    isDaemon:Boolean = false,
    contextClassLoader:ClassLoader? = null,
    name:String? = null,
    priority:Int = -1,
    block:()->V):FutureTask<V>
{
    val future = FutureTask(block)
    if (isThreaded)
    {
        thread(true,isDaemon,contextClassLoader,name,priority,{future.run()})
    }
    return future
}

/**
 * sleeps for the specified number of milliseconds. returns the number of
 * milliseconds that passed while sleeping. does not throw
 * [InterruptedException] is interrupted while sleeping.
 */
fun sleep(timeoutMillis:Long):SleepResult
{
    var wasInterrupted = false
    val sleepDuration = measureTimeMillis()
    {
        wasInterrupted = try
        {
            Thread.sleep(timeoutMillis)
            false
        }
        catch (ex:InterruptedException)
        {
            true
        }
    }
    return SleepResult(wasInterrupted,sleepDuration)
}

data class SleepResult(val wasInterrupted:Boolean,val sleepDuration:Long):Serializable

fun ExecutorService.awaitTermination()
{
    while (!isTerminated)
    {
        awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS)
    }
}

private val suspendedThreadStates = setOf(
    Thread.State.TIMED_WAITING,
    Thread.State.WAITING,
    Thread.State.BLOCKED)

val Thread.isSuspended:Boolean get()
{
    return state in suspendedThreadStates
}

fun Thread.awaitSuspended()
{
    while (!isSuspended)
    {
        Thread.yield()
    }
}

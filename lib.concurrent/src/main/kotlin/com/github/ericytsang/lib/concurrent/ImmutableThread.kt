package com.github.ericytsang.lib.concurrent

val thread = Thread()

fun Thread.mutableThread():MutableThread
{
    return ThreadToMutableThreadAdapter(this)
}

fun MutableThread.immutableThread():ImmutableThread
{
    return MutableThreadToImmutableThreadAdapter(this)
}

interface MutableThread:ImmutableThread
{
    override var contextClassLoader:ClassLoader
    fun start()
    fun interrupt()
    override var name:String
    override var priority:Int
    override var uncaughtExceptionHandler:Thread.UncaughtExceptionHandler
}

interface ImmutableThread
{
    fun join()
    fun join(millis:Long)
    fun join(millis:Long,nanos:Int)
    val state:Thread.State
    val threadGroup:ThreadGroup
    val stackTrace:Array<out StackTraceElement>
    val contextClassLoader:ClassLoader
    val id:Long
    val isAlive:Boolean
    val isDaemon:Boolean
    val isInterrupted:Boolean
    val name:String
    val priority:Int
    val uncaughtExceptionHandler:Thread.UncaughtExceptionHandler
    fun checkAccess()
    fun run()
    override fun toString():String
    override fun equals(other:Any?):Boolean
    override fun hashCode():Int
    fun awaitSuspended()
    val isSuspended:Boolean
}

class ThreadToMutableThreadAdapter(val delegate:Thread):MutableThread
{
    override fun join()
        = delegate.join()
    override fun join(millis:Long)
        = delegate.join(millis)
    override fun join(millis:Long,nanos:Int)
        = delegate.join(millis,nanos)
    override val state:Thread.State
        get() = delegate.state
    override val threadGroup:ThreadGroup
        get() = delegate.threadGroup
    override val stackTrace:Array<out StackTraceElement>
        get() = delegate.stackTrace
    override var contextClassLoader:ClassLoader
        get() = delegate.contextClassLoader
        set(value)
        {
            delegate.contextClassLoader = value
        }
    override val id:Long
        get() = delegate.id
    override val isAlive:Boolean
        get() = delegate.isAlive
    override val isDaemon:Boolean
        get() = delegate.isDaemon
    override fun start()
        = delegate.start()
    override fun interrupt()
        = delegate.interrupt()
    override val isInterrupted:Boolean
        get() = delegate.isInterrupted
    override var name:String
        get() = delegate.name
        set(value)
        {
            delegate.name = value
        }
    override var priority:Int
        get() = delegate.priority
        set(value)
        {
            delegate.priority = value
        }
    override var uncaughtExceptionHandler:Thread.UncaughtExceptionHandler
        get() = delegate.uncaughtExceptionHandler
        set(value)
        {
            delegate.uncaughtExceptionHandler = value
        }
    override fun checkAccess()
        = delegate.checkAccess()
    override fun run()
        = delegate.run()
    override fun toString():String
        = delegate.toString()
    override fun equals(other:Any?):Boolean
        = delegate.equals(other)
    override fun hashCode():Int
        = delegate.hashCode()
    override fun awaitSuspended()
        = delegate.awaitSuspended()
    override val isSuspended:Boolean
        get() = delegate.isSuspended
}

class MutableThreadToImmutableThreadAdapter(private val delegate:MutableThread):ImmutableThread by delegate

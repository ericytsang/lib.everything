package com.github.ericytsang.lib.testutils

import kotlin.system.measureTimeMillis

object TestUtils
{
    fun getAllChildrenOfThreadGroup(threadGroup:ThreadGroup,excludeCurrentThread:Boolean,exceptions:Set<String>):List<Thread>
    {
        var threads = arrayOfNulls<Thread>(threadGroup.activeCount())
        while (threadGroup.enumerate(threads,true) == threads.size)
        {
            threads = arrayOfNulls(threads.size*2)
        }
        return threads
            .filterNotNull()
            .filter {!excludeCurrentThread || it != Thread.currentThread()}
            .filter {it.name !in exceptions}
            .filter {!it.isDaemon}
    }

    fun assertAllWorkerThreadsDead(threadGroup:ThreadGroup = Thread.currentThread().threadGroup,exceptions:Set<String> = emptySet(),timeout:Long = 1)
    {
        // get worker threads
        val workerThreads = getAllChildrenOfThreadGroup(threadGroup,true,exceptions)

        // wait a maximum of timeout ms for threads to die
        if (timeout > 0)
        {
            var timeoutRemaining = timeout
            for (thread in workerThreads)
            {
                timeoutRemaining -= measureTimeMillis {
                    thread.join(timeoutRemaining)
                }
                if (timeoutRemaining <= 0) break
            }
        }

        // wait indefinitely for threads to die
        else
        {
            for (thread in workerThreads)
            {
                thread.join()
            }
        }

        // throw exceptions for threads that are still alive
        val stackTraces = workerThreads
            .filter {it.isAlive}
            .map {it.stackTrace.joinToString(prefix = "\n${it.name}:\n",separator = "\n    ")}
        if (stackTraces.isNotEmpty())
        {
            throw Exception("threads not dead: \n${stackTraces.joinToString(prefix = "\n\nvvvvvvvv\n\n",separator = "\n\n", postfix = "\n\n^^^^^^^^\n\n")}")
        }
    }

    class ExpectedExceptionNotThrownException internal constructor():Exception()

    fun exceptionExpected(block:()->Unit):Exception
    {
        try
        {
            block()
            throw ExpectedExceptionNotThrownException()
        }
        catch (ex:ExpectedExceptionNotThrownException)
        {
            throw ex
        }
        catch (ex:Exception)
        {
            ex.printStackTrace(System.out)
            return ex
        }
    }
}

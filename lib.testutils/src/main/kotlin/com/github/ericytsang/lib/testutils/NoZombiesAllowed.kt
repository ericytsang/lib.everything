package com.github.ericytsang.lib.testutils

import org.junit.rules.TestWatcher
import org.junit.runners.model.Statement
import java.util.concurrent.ArrayBlockingQueue
import kotlin.system.measureTimeMillis

/**
 * makes sure that there are no zombie threads kicking around by the end
 * of the unit test.
 */
class NoZombiesAllowed:TestWatcher()
{
    companion object
    {
        val consecutiveIntsFactory = generateSequence(0) {it+1}.iterator()
    }
    override fun apply(base:Statement,description:org.junit.runner.Description):Statement
    {
        return super.apply(
                object:Statement()
                {
                    override fun evaluate()
                    {
                        val threadGroup = ThreadGroup("${NoZombiesAllowed::class.simpleName!!}-${consecutiveIntsFactory.next()}")
                        serialWithThreadGroup(threadGroup)
                        {
                            base.evaluate()
                        }
                        assertAllWorkerThreadsDead(threadGroup)
                    }
                },
                description)
    }

    private fun <R> serialWithThreadGroup(threadGroup:ThreadGroup,block:()->R):R
    {
        val q = ArrayBlockingQueue<R>(1)
        val t = Thread(threadGroup)
        {
            q += block()
        }
        t.start()
        t.join()
        return q.take()
    }

    private fun getAllChildrenOfThreadGroup(threadGroup:ThreadGroup,excludeCurrentThread:Boolean,exceptions:Set<String>):List<Thread>
    {
        var threads = arrayOfNulls<Thread>(threadGroup.activeCount()+1)
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

    private fun assertAllWorkerThreadsDead(threadGroup:ThreadGroup = Thread.currentThread().threadGroup,exceptions:Set<String> = emptySet(),timeout:Long = 100)
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
}
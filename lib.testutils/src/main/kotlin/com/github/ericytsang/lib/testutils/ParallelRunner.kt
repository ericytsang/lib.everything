package com.github.ericytsang.lib.testutils

import com.github.ericytsang.lib.concurrent.awaitTermination
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.RunnerScheduler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class ParallelRunner(klass:Class<*>):BlockJUnit4ClassRunner(klass)
{
    init
    {
        setScheduler(ParallelRunnerScheduler())
    }

    private class ParallelRunnerScheduler:RunnerScheduler
    {
        private val threadPool = Executors.newCachedThreadPool()
        {
            Thread(ThreadGroup(toString()),it)
        }

        override fun schedule(childStatement:Runnable)
        {
            threadPool.execute(childStatement)
        }

        override fun finished()
        {
            threadPool.shutdown()
            threadPool.awaitTermination()
        }
    }
}

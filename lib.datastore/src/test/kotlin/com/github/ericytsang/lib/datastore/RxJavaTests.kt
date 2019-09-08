package com.github.ericytsang.lib.datastore

import com.github.ericytsang.lib.testutils.NoZombiesAllowed
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.internal.schedulers.ComputationScheduler
import io.reactivex.subjects.PublishSubject
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

class RxJavaTests
{
    @JvmField
    @Rule
    val noZombieThreads = NoZombiesAllowed()

    @Test
    fun what_thread_does_rxjava_Observable_onNext_get_called_on()
    {
        println("${Thread.currentThread()}: running the unit test")
        val observable = Observable.just(1,2,3,4,5)
        observable.subscribe {println("${Thread.currentThread()}: $it // subscribe")}.dispose()
    }

    @Test
    fun what_thread_does_rxjava_FlowableSubject_onNext_get_called_on()
    {
        println("${Thread.currentThread()}: running the unit test")
        var count = 0
        var shouldStop = false
        val isStopped = CountDownLatch(1)
        val scheduler = ComputationScheduler {Thread(it)}
        val observable = PublishSubject
                .create<Int>()
                {
                    while (!shouldStop)
                    {
                        println("${Thread.currentThread()}: $count // publish")
                        it.onNext(++count)
                    }
                    isStopped.countDown()
                }
                .subscribeOn(scheduler)
        val minDisposable1Iterations = CountDownLatch(100)
        val disposable1 = observable.forEach()
        {
            println("${Thread.currentThread()}: $it // subscribe1")
            minDisposable1Iterations.countDown()
        }
        val minDisposable2Iterations = CountDownLatch(100)
        val disposable2 = observable.forEach()
        {
            println("${Thread.currentThread()}: $it // subscribe2")
            minDisposable2Iterations.countDown()
        }
        minDisposable1Iterations.await()
        minDisposable2Iterations.await()
        disposable1.dispose()
        disposable2.dispose()
        shouldStop = true
        isStopped.await()
        scheduler.shutdown()
    }

    @Test
    fun what_thread_does_rxjava_FlowableSubject2_onNext_get_called_on()
    {
        println("${Thread.currentThread()}: running the unit test")
        val scheduler = ComputationScheduler {Thread(it)}
        var count = 0
        val observable = Flowable
                .generate<Int>()
                {
                    val thisCount = ++count
                    println("${Thread.currentThread()}: $thisCount // publish")
                    it.onNext(thisCount)
                }
                .onBackpressureLatest()
                .subscribeOn(scheduler) // what scheduler to call each subscribe() on (producer)
        //.observeOn(scheduler) // what scheduler to call onNext on for each subscribe() (consumer)
        val unblockAfterNIterations = CountDownLatch(5)
        val disposable1 = observable.forEach()
        {
            println("${Thread.currentThread()}: $it // subscribe1")
            unblockAfterNIterations.countDown()
        }
        unblockAfterNIterations.await()
        disposable1.dispose()
        println("disposed")
        scheduler.shutdown()
    }
}
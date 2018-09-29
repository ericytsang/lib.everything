package com.github.ericytsang.lib.datastore

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test

class RxJavaTests
{

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
        val scheduler = ExecutorScheduler(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()))
        val observable = PublishSubject
                .create<Int>()
                {
                    while (true)
                    {
                        println("${Thread.currentThread()}: $count // publish")
                        it.onNext(++count)
                    }
                }
                .subscribeOn(scheduler)
        val disposable1 = observable.forEach()
        {
            println("${Thread.currentThread()}: $it // subscribe1")
        }
        Thread.sleep(1000)
        val disposable2 = observable.forEach()
        {
            println("${Thread.currentThread()}: $it // subscribe2")
        }
        Thread.sleep(1000)
        disposable1.dispose()
        disposable2.dispose()
        scheduler.shutdown()
    }

    @Test
    fun what_thread_does_rxjava_FlowableSubject2_onNext_get_called_on()
    {
        println("${Thread.currentThread()}: running the unit test")
        val scheduler = ExecutorScheduler(Executors.newFixedThreadPool(100))
        var count = 0
        val observable = Flowable
                .generate<Int>()
                {
                    Thread.sleep(1000)
                    val thisCount = ++count
                    println("${Thread.currentThread()}: $thisCount // publish")
                    it.onNext(thisCount)
                }
                .onBackpressureLatest()
                .subscribeOn(scheduler) // what scheduler to call each subscribe() on (producer)
        //.observeOn(scheduler) // what scheduler to call onNext on for each subscribe() (consumer)
        val disposable1 = observable.forEach()
        {
            println("${Thread.currentThread()}: $it // subscribe1")
        }
        Thread.sleep(2100)
        disposable1.dispose()
        println("disposed")
        scheduler.shutdown()
        Thread.sleep(3000)
    }

    @Test
    fun backpressure_demo()
    {

        println("${Thread.currentThread()}: running the unit test")
        val scheduler = ExecutorScheduler(Executors.newFixedThreadPool(100))
        val latch = CountDownLatch(1)
        val observable = Flowable
                .generate<Int>()
                {
                    var count = 0
                    it.onNext(++count)
                    it.onNext(++count)
                    latch.await()
                    it.onNext(++count)
                    it.onComplete()
                }
                .onBackpressureLatest()
                .subscribeOn(scheduler) // what scheduler to call each subscribe() on (producer)
        //.observeOn(scheduler) // what scheduler to call onNext on for each subscribe() (consumer)
        val disposable1 = observable.forEach()
        {
            latch.countDown()
            println("${Thread.currentThread()}: $it // subscribe1")
        }
        Thread.sleep(3000)
        disposable1.dispose()
        scheduler.shutdown()
        Thread.sleep(3000)
    }
}
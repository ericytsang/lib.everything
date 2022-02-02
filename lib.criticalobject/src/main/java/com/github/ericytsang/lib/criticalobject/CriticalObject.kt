package com.github.ericytsang.lib.criticalobject

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

fun <T> criticalSetOf(vararg elements:T) = CriticalObject(mutableSetOf(*elements))
{
    it as Set<T>
}

class CriticalObject<Mutable,Immutable>(private val criticalObject:Mutable,val toReadOnly:(Mutable)->Immutable)
{
    private val reentrantLock = ReentrantReadWriteLock()
    fun <Result> lockToMutate(block:(Mutable)->Result):Result
    {
        return reentrantLock.write()
        {
            block(criticalObject)
        }
    }

    fun <Result> lockToRead(block:(Immutable)->Result):Result
    {
        return reentrantLock.read()
        {
            block(toReadOnly(criticalObject))
        }
    }
}
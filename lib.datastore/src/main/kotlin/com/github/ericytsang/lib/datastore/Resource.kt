package com.github.ericytsang.lib.datastore

import kotlin.concurrent.withLock

class Resource<Access:Any>(
        private val access:Access,
        private val lock:Lock)
{
    fun <Result> access(action:(Access)->Result):Result
    {
        return lock.withLock()
        {
            action(access)
        }
    }

    interface Lock
    {
        companion object
        {
            fun adapt(javaLock:java.util.concurrent.locks.Lock):Lock
            {
                return object:Lock
                {
                    override fun <R> withLock(actionDoneWithLock:()->R):R
                    {
                        return javaLock.withLock {actionDoneWithLock()}
                    }
                }
            }
        }
        fun <R> withLock(actionDoneWithLock:()->R):R
    }
}

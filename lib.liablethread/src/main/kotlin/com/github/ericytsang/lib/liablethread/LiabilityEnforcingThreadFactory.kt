package com.github.ericytsang.lib.liablethread

import java.util.concurrent.ThreadFactory

class LiabilityEnforcingThreadFactory(
        private val delegate:ThreadFactory)
    :ThreadFactory
{
    override fun newThread(r:Runnable):Thread
    {
        val stacktrace = Throwable("thread created at this stacktrace threw an exception")
        val runnable = Runnable()
        {
            try
            {
                r.run()
            }
            catch (e:Throwable)
            {
                stacktrace.initCause(e)
                throw stacktrace
            }
        }
        return delegate.newThread(runnable)
    }
}

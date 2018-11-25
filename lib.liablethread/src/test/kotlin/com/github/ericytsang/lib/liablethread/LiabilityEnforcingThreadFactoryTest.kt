package com.github.ericytsang.lib.liablethread

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import java.util.concurrent.Executors

@RunWith(MockitoJUnitRunner::class)
class LiabilityEnforcingThreadFactoryTest
{
    @Test
    fun exception_thrown_by_thread_should_contain_information_about_where_it_is_created()
    {
        val thread = LiabilityEnforcingThreadFactory(Executors.defaultThreadFactory()).newThread()
        {
            throw Exception()
        }
        thread.start()
        thread.join()
    }
}

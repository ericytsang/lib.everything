package com.github.ericytsang.lib.onlycallonce

import com.github.ericytsang.lib.testutils.TestUtils.exceptionExpected
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.runners.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class OneshotCallTest
{
    @Mock
    private val mockFunction = {}

    @Test
    fun subsequent_calls_are_ignored()
    {
        val fixture = OneshotCall
            .builder<Unit>()
            .ignoreSubsequent {mockFunction.invoke()}
        fixture.call(Unit)
        verify(mockFunction).invoke()
        repeat(5) {fixture.call(Unit)}
        verify(mockFunction).invoke()
    }

    @Test
    fun subsequent_calls_throw()
    {
        val fixture = OneshotCall
            .builder<Unit>()
            .throwSubsequent {mockFunction.invoke()}
        fixture.call(Unit)
        verify(mockFunction).invoke()
        repeat(5) {
            val ex = exceptionExpected()
            {
                fixture.call(Unit)
            }
            assert(ex is OneshotCall.ThrowOnSubsequentCalls.AlreadyCalledException)
        }
        verify(mockFunction).invoke()
    }
}

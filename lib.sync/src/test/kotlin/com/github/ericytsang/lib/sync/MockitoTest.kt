package com.github.ericytsang.lib.sync

import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class MockitoTest
{
    @Test
    fun can_use_multiple_thenAnswer_calls()
    {
        val mock = Mockito.mock(CharSequence::class.java)
        Mockito.`when`(mock.length)
                .thenAnswer {5}
                .thenAnswer {6}
        assertEquals(mock.length,5)
        assertEquals(mock.length,6)
        assertEquals(mock.length,6)
    }
}
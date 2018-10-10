package com.github.ericytsang.lib.typedstream

import org.junit.Test
import kotlin.test.assertEquals

class Tests
{
    @Test
    fun return_file_and_line_number_of_call_site()
    {
        assertEquals("${this::class.simpleName}.kt:11",getFileNameAndLine(StacktraceIndex()))
    }
}

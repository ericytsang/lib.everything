package com.github.ericytsang.lib.typedstream

import com.github.ericytsang.lib.getfileandline.StacktraceIndex
import com.github.ericytsang.lib.getfileandline.getFileNameAndLine
import org.junit.Test
import kotlin.test.assertEquals

class Tests
{
    @Test
    fun return_file_and_line_number_of_call_site()
    {
        assertEquals("${this::class.simpleName}.kt:13",getFileNameAndLine(StacktraceIndex()))
    }
}

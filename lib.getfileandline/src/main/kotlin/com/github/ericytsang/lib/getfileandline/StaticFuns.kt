package com.github.ericytsang.lib.getfileandline

import java.lang.Exception

data class StacktraceIndex(
        val indexOffset:Int = 0)
{
    val index = 1+indexOffset
}

/**
 * the file and line number, or null if index is out of bounds.
 */
fun getFileNameAndLine(stacktraceIndex:StacktraceIndex):String?
{
    val stacktrace = Exception().stackTrace
    val stacktraceTray = try
    {
        stacktrace[stacktraceIndex.index]
    }
    catch (e:ArrayIndexOutOfBoundsException)
    {
        null
    }
    return stacktraceTray?.let {"${it.fileName}:${it.lineNumber}"}
}

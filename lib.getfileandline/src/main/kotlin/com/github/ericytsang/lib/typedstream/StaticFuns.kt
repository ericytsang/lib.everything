package com.github.ericytsang.lib.typedstream

import java.lang.Exception

data class StacktraceIndex(
        val indexOffset:Int = 0)
{
    val index = 1+indexOffset
}

fun getFileNameAndLine(stacktraceIndex:StacktraceIndex):String
{
    val stacktrace = Exception().stackTrace[stacktraceIndex.index]
    return "${stacktrace.fileName}:${stacktrace.lineNumber}"
}

package com.github.ericytsang.lib.simplepipestream

object StaticFuns
{
    fun newSimplePipeStreamPair(bufferSize:Int?):Pair<SimplePipedInputStream,SimplePipedOutputStream>
    {
        val pipeO = bufferSize?.let {SimplePipedOutputStream(it)} ?: SimplePipedOutputStream()
        val pipeI = SimplePipedInputStream(pipeO)
        return pipeI to pipeO
    }
}

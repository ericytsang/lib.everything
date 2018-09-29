package com.github.ericytsang.lib.android.worker

import android.util.Base64
import androidx.work.Data
import androidx.work.WorkRequest
import androidx.work.Worker
import com.github.ericytsang.lib.domainobjects.serialize
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

private val INPUT_DATA_BASE64_ENCODED_DATA = WorkerWithInputData::class.qualifiedName+"INPUT_DATA_BASE64_ENCODED_DATA"

abstract class WorkerWithInputData<Input:Serializable>(
        val inputType:KClass<Input>)
    :Worker()
{
    final override fun doWork():Result
    {
        // parse input data
        val alarm = inputData
                .getString(INPUT_DATA_BASE64_ENCODED_DATA)!!
                .decodeFromString<Serializable>()
                .let {inputType.cast(it)}

        // do the work...
        return doWork(alarm)
    }

    abstract fun doWork(inputData:Input):Result
}

fun <B:WorkRequest.Builder<B,W>,W:WorkRequest,Worker:WorkerWithInputData<Input>,Input:Serializable>
        WorkRequest.Builder<B,W>.setInputData(workerClass:KClass<Worker>,inputData:Input):WorkRequest.Builder<B,W>
{
    return setInputData(Data.Builder()
            .putString(INPUT_DATA_BASE64_ENCODED_DATA,inputData.encodeToString())
            .build())
}

// todo: move to library
fun Serializable.encodeToString():String
{
    return Base64.encodeToString(serialize(),0)
}

// todo: move to library
inline fun <reified R> String.decodeFromString():R
{
    return Base64.decode(this,0)
            .let {ByteArrayInputStream(it)}
            .let {ObjectInputStream(it)}
            .readObject()
            .let {it as R}
}
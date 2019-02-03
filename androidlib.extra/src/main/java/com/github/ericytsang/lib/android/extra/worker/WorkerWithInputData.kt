package com.github.ericytsang.androidlib.extra.worker

import android.content.Context
import androidx.work.Data
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.ericytsang.androidlib.decodeFromString
import com.github.ericytsang.androidlib.encodeToString
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

private val INPUT_DATA_BASE64_ENCODED_DATA = WorkerWithInputData::class.qualifiedName+"INPUT_DATA_BASE64_ENCODED_DATA"

abstract class WorkerWithInputData<Input:Serializable>(
        val inputType:KClass<Input>,
        val context:Context,
        val workParams:WorkerParameters)
    :Worker(context,workParams)
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

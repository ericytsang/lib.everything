package com.github.ericytsang.androidlib.extra.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.ericytsang.androidlib.alarmManager
import com.github.ericytsang.androidlib.decodeFromString
import com.github.ericytsang.androidlib.encodeToString
import com.github.ericytsang.androidlib.forceExhaustiveWhen
import com.github.ericytsang.androidlib.setExactAndAllowWhileIdleCompat
import org.joda.time.DateTime
import java.io.Serializable
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// todo: move this and android manifest entry into its own module
class WorkScheduler(
        val strategy:Strategy)
{
    companion object
    {
        private val random = Random()
        val INPUT_DATA_BASE64_ENCODED_DATA = this::class.qualifiedName+"INPUT_DATA_BASE64_ENCODED_DATA"
        val FUNCTION_BASE64_ENCODED_DATA = this::class.qualifiedName+"FUNCTION_BASE64_ENCODED_DATA"
    }

    val instanceId = random.nextLong()

    sealed class Strategy
    {
        class AlarmManagerStrategy(context:Context):Strategy()
        {
            val appContext = context.applicationContext
            val alarmManager = context.alarmManager
        }
        class WorkManagerStrategy:Strategy()
        {
            val workManager = WorkManager.getInstance()
        }
    }

    /**
     * schedules work to be done in the future. previously scheduled work that
     * have not been executed yet will be cancelled, and will not be executed.
     */
    inline fun <reified Work:ScheduledWork<Input>,Input:Serializable> rescheduleWork(
            executionDateTime:DateTime,
            input:Input,
            scheduledWork:ScheduledWork<Input>)
    {
        Work::class.constructors.filter {it.parameters.isEmpty()}
        val now = DateTime.now()!!
        when (strategy)
        {
            is Strategy.AlarmManagerStrategy ->
            {
                val pendingIntent = PendingIntent.getBroadcast(
                        strategy.appContext,instanceId.hashCode(),
                        Intent(strategy.appContext,CustomBroadcastReceiver::class.java)
                                .putExtra(INPUT_DATA_BASE64_ENCODED_DATA,input.encodeToString())
                                .putExtra(FUNCTION_BASE64_ENCODED_DATA,scheduledWork.encodeToString()),
                        PendingIntent.FLAG_CANCEL_CURRENT)
                strategy.alarmManager.setExactAndAllowWhileIdleCompat(
                        AlarmManager.RTC_WAKEUP,
                        executionDateTime.millis,
                        pendingIntent)
            }
            is Strategy.WorkManagerStrategy ->
            {
                val NEXT_ALARM_WORK_REQUEST_TAG = this::class.qualifiedName+"NEXT_ALARM_WORK_REQUEST_TAG_"+instanceId
                strategy.workManager.cancelAllWorkByTag(NEXT_ALARM_WORK_REQUEST_TAG)
                strategy.workManager.enqueue(OneTimeWorkRequestBuilder<CustomWorker>()
                        .setInitialDelay(executionDateTime.minus(now.millis).millis,TimeUnit.MILLISECONDS)
                        .addTag(NEXT_ALARM_WORK_REQUEST_TAG)
                        .setInputData(Data.Builder()
                                .putString(INPUT_DATA_BASE64_ENCODED_DATA,input.encodeToString())
                                .putString(FUNCTION_BASE64_ENCODED_DATA,scheduledWork.encodeToString())
                                .build())
                        .build())
            }
        }.forceExhaustiveWhen
    }

    internal class CustomBroadcastReceiver:BroadcastReceiver()
    {
        override fun onReceive(context:Context,intent:Intent)
        {
            val input = intent
                    .getStringExtra(INPUT_DATA_BASE64_ENCODED_DATA)!!
                    .decodeFromString<Serializable>()
            val function = intent
                    .getStringExtra(FUNCTION_BASE64_ENCODED_DATA)!!
                    .decodeFromString<ScheduledWork<Serializable>>()
            thread {
                function.doWork(context.applicationContext,input)
            }
        }
    }

    private class CustomWorker(context:Context,workerParams:WorkerParameters):Worker(context,workerParams)
    {
        override fun doWork():Result
        {
            val input = inputData
                    .getString(INPUT_DATA_BASE64_ENCODED_DATA)!!
                    .decodeFromString<Serializable>()
            val function = inputData
                    .getString(FUNCTION_BASE64_ENCODED_DATA)!!
                    .decodeFromString<ScheduledWork<Serializable>>()
            function.doWork(applicationContext,input)
            return Result.success()
        }
    }

    interface ScheduledWork<in Input:Serializable>:Serializable
    {
        fun doWork(appContext:Context,input:Input)
    }
}
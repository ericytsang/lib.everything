package com.github.ericytsang.androidlib.shakelistener

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.github.ericytsang.androidlib.core.DoLog
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.math.abs
import kotlin.math.min

class ShakeListener(
        val shakeMagnitudeThreshold:Float,
        val minimumShakeDurationNanos:Long = NANOSECONDS.convert(300,MILLISECONDS),
        val onShake:(source:ShakeListener,avgAcceleration:Float)->Unit)
    :SensorEventListener,DoLog
{
    private val intervalNanosAverager = Averager(10)
    private var oldTimestampNanos:Long? = null

    private val accelerationAverager = Averager(10)
    private var oldAcceleration:Float? = null

    override fun onAccuracyChanged(sensor:Sensor,accuracy:Int) = Unit
    override fun onSensorChanged(event:SensorEvent)
    {
        val oldTimestampNanos = oldTimestampNanos
        val newTimestampNanos = event.timestamp
        this.oldTimestampNanos = newTimestampNanos

        if (oldTimestampNanos == null) return

        val averageIntervalNanos = intervalNanosAverager.addSample(newTimestampNanos.minus(oldTimestampNanos).toFloat())

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val oldAcceleration = oldAcceleration
        val newAcceleration = (x*x+y*y+z*z).toDouble().toFloat()
        this.oldAcceleration = newAcceleration

        accelerationAverager.numSamples = minimumShakeDurationNanos.div(averageIntervalNanos).toInt()
        val avgAcceleration = accelerationAverager.addSample(
                min(abs(newAcceleration-(oldAcceleration?:newAcceleration)),shakeMagnitudeThreshold*1.25f))

        if (avgAcceleration > shakeMagnitudeThreshold)
        {
            onShake(this,avgAcceleration)
        }
    }

    class Averager(_numSamples:Int)
    {
        var newSampleWeight = 0f
            private set
        var oldSampleWeight = 0f
            private set
        var numSamples = _numSamples
            set(value)
            {
                field = value
                newSampleWeight = 1f/numSamples
                oldSampleWeight = 1f-newSampleWeight
            }
        init
        {
            numSamples = _numSamples
            require(numSamples > 0)
        }
        var average:Float? = null
            private set
        fun addSample(newSample:Float):Float
        {
            val average = (average?:newSample)*oldSampleWeight+newSample*newSampleWeight
            this.average = average
            return average
        }
    }
}

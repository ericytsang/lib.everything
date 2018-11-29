package com.github.ericytsang.lib.android

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.Vibrator
import android.preference.Preference
import android.preference.PreferenceGroup
import android.preference.PreferenceManager
import android.support.v4.app.AlarmManagerCompat
import android.support.v4.content.ContextCompat
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

// Context

val Context.layoutInflater:LayoutInflater get()
{
    return LayoutInflater.from(this)
}

fun Context.getColorCompat(resId:Int):Int
{
    return ContextCompat.getColor(this,resId)
}

fun Context.getDrawableCompat(resId:Int):Drawable
{
    return ContextCompat.getDrawable(this,resId)!!
}

fun Context.startForegroundServiceCompat(intent:Intent)
{
    return ContextCompat.startForegroundService(this,intent)
}

val Context.activityManager:ActivityManager get()
{
    return getSystemServiceCompat(Context.ACTIVITY_SERVICE)
}

val Context.notificationManager:NotificationManager get()
{
    return getSystemServiceCompat(Context.NOTIFICATION_SERVICE)
}

val Context.keyguardManager:KeyguardManager get()
{
    return getSystemServiceCompat(Context.KEYGUARD_SERVICE)
}

val Context.powerManager:PowerManager get()
{
    return getSystemServiceCompat(Context.POWER_SERVICE)
}

val Context.alarmManager:AlarmManager get()
{
    return getSystemServiceCompat(Context.ALARM_SERVICE)
}

val Context.vibrator:Vibrator get()
{
    return getSystemServiceCompat(Context.VIBRATOR_SERVICE)
}

val Context.accessibilityManager:AccessibilityManager get()
{
    return getSystemServiceCompat(Context.ACCESSIBILITY_SERVICE)
}

val Context.windowManager:WindowManager get()
{
    return getSystemServiceCompat(Context.WINDOW_SERVICE)
}

inline fun <reified SystemService> Context.getSystemServiceCompat(key:String):SystemService
{
    return getSystemService(key) as SystemService
}

fun Context.getStringCompat(resId:Int,vararg formatArgs:Any):String
{
    return resources.getString(resId,*formatArgs)
}

val Context.defaultSharedPreferences:SharedPreferences get()
{
    return PreferenceManager.getDefaultSharedPreferences(this)
}

// SharedPreferences

fun <Result> SharedPreferences.editAndCommit(block:(SharedPreferences.Editor)->Result):Result
{
    val editor = edit()
    val result = block(editor)
    editor.commit()
    return result
}

// others

fun <T> Iterable<T>.sumBy(block:(T)->Long):Long
{
    return this.fold(0L) {acc, t -> acc+block(t)}
}

fun <Result> postOnUiThread(delay:Long = 0,block:()->Result):Future<Result>
{
    val result = FutureTask(block)
    val handler = Handler(Looper.getMainLooper())
    if (delay <= 0)
    {
        handler.post(result)
    }
    else
    {
        handler.postAtTime(result,SystemClock.uptimeMillis()+delay)
    }
    return result
}

fun <T> List<T>.subList(indices:IntRange):List<T>
{
    return subList(indices.first,indices.last)
}

fun fromHtml(html:String):Spanned
{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(html)
    }
}

fun IntRange.size():Int
{
    return Math.max(0,last-first+1)
}

fun <E> MutableList<E>.retainRange(retainedIndices:IntRange):MutableList<E>
{
    for (i in retainedIndices.last+1..lastIndex)
    {
        removeAt(lastIndex)
    }
    for (i in 1..retainedIndices.first)
    {
        removeAt(0)
    }
    return this
}

fun ExecutorService.executeAndAwait(countDownLatch:CountDownLatch = CountDownLatch(1),block:(CountDownLatch)->Unit)
{
    execute {
        block(countDownLatch)
        countDownLatch.await()
    }
}

val View.children:List<View> get()
{
    return if (this is ViewGroup)
    {
        (0 until childCount).map {getChildAt(it)}
    }
    else
    {
        listOf()
    }
}

val View.descendants:List<View> get()
{
    return children+children.flatMap {it.descendants}
}

val <R> R.forceExhaustiveWhen:R get() = this

fun <R> exhaustive(r:R):R = r

interface DoLog
interface NoLog

inline fun <reified R:DoLog> R.info(shouldLog:Boolean,log:()->Any?)
{
    if (shouldLog)
    {
        Log.i(R::class.simpleName,log()?.toString()?:"")
    }
}

inline fun <reified R:NoLog> R.info(shouldLog:Boolean,log:()->Any?)
{
    // no-op
}


fun AlarmManager.setExactAndAllowWhileIdleCompat(type:Int,triggerAtMillis:Long,operation:PendingIntent)
{
    AlarmManagerCompat.setExactAndAllowWhileIdle(this,type,triggerAtMillis,operation)
}

val PreferenceGroup.children:Sequence<Preference> get()
{
    var preferenceIndex = 0
    return generateSequence()
    {
        try
        {
            getPreference(preferenceIndex++)
        }
        catch (e:IndexOutOfBoundsException)
        {
            null
        }
    }
}

val PreferenceGroup.descendants:Sequence<Preference> get()
{
    return children.flatMap()
    {
        preference ->
        sequenceOf(preference)+when (preference)
        {
            is PreferenceGroup -> preference.descendants
            else -> sequenceOf()
        }
    }
}

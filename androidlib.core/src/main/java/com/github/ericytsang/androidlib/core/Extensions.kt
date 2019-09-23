package com.github.ericytsang.androidlib.core

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.content.res.TypedArray
import android.graphics.Point
import android.view.LayoutInflater
import android.graphics.drawable.Drawable
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.Vibrator
import android.preference.Preference
import android.preference.PreferenceGroup
import android.preference.PreferenceManager
import android.text.Html
import android.text.Spanned
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import com.github.ericytsang.androidlib.core.HvOrientation.*
import com.github.ericytsang.lib.domainobjects.serialize
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.Serializable
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

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

val Context.shortcutManager:ShortcutManager @TargetApi(Build.VERSION_CODES.N_MR1) get()
{
    return getSystemServiceCompat(Context.SHORTCUT_SERVICE)
}

val Context.activityManager:ActivityManager get()
{
    return getSystemServiceCompat(Context.ACTIVITY_SERVICE)
}

val Context.sensorManager:SensorManager get()
{
    return getSystemServiceCompat(Context.SENSOR_SERVICE)
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

val Context.clipboardManager:ClipboardManager get()
{
    return getSystemServiceCompat(Context.CLIPBOARD_SERVICE)
}

val Context.locationManager:LocationManager get()
{
    return getSystemServiceCompat(Context.LOCATION_SERVICE)
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

fun Context.checkSelfPermissionCompat(permission:String):Boolean
{
    return ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED
}

fun <R> Context.usingAttrs(attrs:AttributeSet,resId:IntArray,block:(TypedArray)->R):R
{
    val typedArray = theme.obtainStyledAttributes(attrs,resId,0,0)
    try
    {
        return block(typedArray)
    }
    finally
    {
        typedArray.recycle()
    }
}

val Context.manifestMetaData:Bundle get()
{
    return packageManager.getApplicationInfo(packageName,PackageManager.GET_META_DATA).metaData
}

// SharedPreferences

@SuppressLint("ApplySharedPref")
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

fun fromHtml(html:String,imageGetter:Html.ImageGetter? = null):Spanned
{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY,imageGetter,null)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html,imageGetter,null)
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

@Suppress("UNUSED_PARAMETER","unused")
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

val Display.screenOrientation:Orientation
    get()
    {
        return when (rotation)
        {
            Surface.ROTATION_0 -> Orientation.REGULAR_PORTRAIT
            Surface.ROTATION_180  -> Orientation.REVERSE_PORTRAIT
            Surface.ROTATION_90 -> Orientation.REGULAR_LANDSCAPE
            Surface.ROTATION_270  -> Orientation.REVERSE_LANDSCAPE
            else -> throw Exception()
        }
    }

enum class Orientation(val hvOrientation:HvOrientation)
{
    REGULAR_PORTRAIT(VERTICAL),
    REVERSE_PORTRAIT(VERTICAL),
    REGULAR_LANDSCAPE(HORIZONTAL),
    REVERSE_LANDSCAPE(HORIZONTAL),
}

enum class HvOrientation
{
    VERTICAL,
    HORIZONTAL,
}

/**
 * returns the screen dimensions of the phone as it would be when in portrait mode.
 */
fun Display.realScreenDimensionsForOrientation(hvOrientation:HvOrientation):RealScreenSize
{
    val screenDimensInPixels = Point().also {getRealSize(it)}
    return when (screenOrientation.hvOrientation)
    {
        VERTICAL -> when(hvOrientation)
        {
            VERTICAL -> RealScreenSize(screenDimensInPixels.x,screenDimensInPixels.y)
            HORIZONTAL -> RealScreenSize(screenDimensInPixels.y,screenDimensInPixels.x)
        }
        HORIZONTAL -> when(hvOrientation)
        {
            VERTICAL -> RealScreenSize(screenDimensInPixels.y,screenDimensInPixels.x)
            HORIZONTAL -> RealScreenSize(screenDimensInPixels.x,screenDimensInPixels.y)
        }
    }
}

data class RealScreenSize(
        val w:Int,
        val h:Int)
    :Serializable

// todo: move to library
fun Serializable.encodeToString():String
{
    return Base64.encodeToString(serialize(),0)
}

// todo: move to library
inline fun <reified R:Serializable> String.decodeFromString():R
{
    return Base64.decode(this,0)
            .let {ByteArrayInputStream(it)}
            .let {ObjectInputStream(it)}
            .readObject()
            .let {it as R}
}

// todo: move to library
inline fun <reified R:Any> kClass() = R::class

// todo: move to library
inline fun <reified C:Any> Any.cast():C = C::class.cast(this)

fun Random.randomString(length:Int):String
{
    return (1..length).map {nextInt(32,127)}.map {it.toChar()}.fold("") {a,e->a+e}
}

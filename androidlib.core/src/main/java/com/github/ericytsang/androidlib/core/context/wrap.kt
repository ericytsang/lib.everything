package com.github.ericytsang.androidlib.core.context

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import androidx.fragment.app.Fragment
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ApplicationContext
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ForegroundContext.ServiceContext
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ForegroundContext.ActivityContext
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ForegroundContext.ForResultContext.ActivityForResultContext
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ForegroundContext.ForResultContext.FragmentForResultContext
import com.github.ericytsang.androidlib.core.context.TypedContext.BackgroundContext.ForegroundContext.FragmentContext

fun Context.wrap() = ApplicationContext(applicationContext as Application)
fun Application.wrap() = ApplicationContext(this)
fun Service.wrap() = ServiceContext(this)

fun Activity.wrap() = ActivityContext(this)
fun Activity.wrap(requestCode:Int) = ActivityForResultContext(wrap(),requestCode)
fun ActivityContext.wrap(requestCode:Int) = ActivityForResultContext(this,requestCode)

fun Fragment.wrap() = FragmentContext(this)
fun Fragment.wrap(requestCode:Int) = FragmentForResultContext(wrap(),requestCode)
fun FragmentContext.wrap(requestCode:Int) = FragmentForResultContext(this,requestCode)

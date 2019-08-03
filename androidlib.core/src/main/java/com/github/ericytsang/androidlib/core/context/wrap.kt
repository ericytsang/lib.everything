package com.github.ericytsang.androidlib.core.context

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import androidx.fragment.app.Fragment
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ApplicationCtx
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext.ServiceCtx
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext.ActivityCtx
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext.ForResultCtx.ActivityForResultCtx
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext.ForResultCtx.FragmentForResultCtx
import com.github.ericytsang.androidlib.core.context.WrappedContext.BackgroundContext.ForegroundContext.FragmentCtx

fun Context.wrap() = ApplicationCtx(applicationContext as Application)
fun Application.wrap() = ApplicationCtx(this)
fun Service.wrap() = ServiceCtx(this)

fun Activity.wrap() = ActivityCtx(this)
fun Activity.wrap(requestCode:Int) = ActivityForResultCtx(wrap(),requestCode)
fun ActivityCtx.wrap(requestCode:Int) = ActivityForResultCtx(this,requestCode)

fun Fragment.wrap() = FragmentCtx(this)
fun Fragment.wrap(requestCode:Int) = FragmentForResultCtx(wrap(),requestCode)
fun FragmentCtx.wrap(requestCode:Int) = FragmentForResultCtx(this,requestCode)

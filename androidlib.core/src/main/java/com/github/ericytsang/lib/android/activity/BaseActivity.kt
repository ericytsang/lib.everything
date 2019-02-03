package com.github.ericytsang.androidlib.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import java.io.Closeable

/**
 * provides type safe classes for managing activity states.
 */
abstract class BaseActivity<Created:Closeable,Resumed:Closeable>:AppCompatActivity()
{
    // toolbar button actions

    final override fun onOptionsItemSelected(item:MenuItem):Boolean
    {
        return onOptionsItemSelected(MethodOverload,resumed!!,item)
    }

    protected open fun onOptionsItemSelected(methodOverload:MethodOverload,resumed:Resumed,item:MenuItem):Boolean
    {
        return when (item.itemId)
        {
            android.R.id.home ->
            {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // AppCompatActivity lifecycle

    /**
     * for making abstract methods not collide with other abstract methods with
     * subclass with similar signatures.
     */
    object MethodOverload
    private var created:Created? = null
    private var resumed:Resumed? = null

    override fun onCreate(savedInstanceState:Bundle?)
    {
        super.onCreate(savedInstanceState)
        created = makeCreated(MethodOverload,findViewById(android.R.id.content)!!)
    }

    final override fun onDestroy()
    {
        created!!.close()
        super.onDestroy()
    }

    final override fun onResume()
    {
        super.onResume()
        resumed = makeResumed(MethodOverload,created!!)
    }

    final override fun onPause()
    {
        resumed!!.close()
        super.onPause()
    }

    protected open fun beforeSuperOnCreate(methodOverload:MethodOverload) {}
    protected abstract fun makeCreated(methodOverload:MethodOverload,contentView:ViewGroup):Created
    protected abstract fun makeResumed(methodOverload:MethodOverload,created:Created):Resumed

    class NoOpState<out Context>(val context:Context):Closeable
    {
        override fun close() = Unit
    }

    // onActivityResult

    private val nullOnActivityResultHandler = {
        onActivityResult:OnActivityResultHandler ->
        super.onActivityResult(onActivityResult.requestCode,onActivityResult.resultCode,onActivityResult.data)
    }

    var onActivityResultHandler:(OnActivityResultHandler)->Unit = nullOnActivityResultHandler

    final override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?)
    {
        onActivityResultHandler(OnActivityResultHandler(requestCode,resultCode,data))
        onActivityResultHandler = nullOnActivityResultHandler
    }

    data class OnActivityResultHandler(
        val requestCode:Int,
        val resultCode:Int,
        val data:Intent?)
}

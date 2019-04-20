package com.github.ericytsang.androidlib.navbuttonlisteners

import android.view.KeyEvent
import android.view.ViewConfiguration
import com.github.ericytsang.androidlib.core.DoLog
import com.github.ericytsang.androidlib.core.postOnUiThread
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.value
import com.github.ericytsang.lib.simpletask.SimpleTask
import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class HardwareButtonLongPressListener(
        val keyCodes:Set<Int>,
        val action:(event:KeyEvent)->Unit)
    :Closeable,DoLog
{
    companion object
    {
        // interesting key codes
        val OVERVIEW_BUTTON_KEY_CODES = setOf(KeyEvent.KEYCODE_MENU,KeyEvent.KEYCODE_APP_SWITCH)
        val HOME_BUTTON_KEY_CODES = setOf(KeyEvent.KEYCODE_HOME)
        val BACK_BUTTON_KEY_CODES = setOf(KeyEvent.KEYCODE_BACK)
    }

    private val lock = ReentrantLock()

    private val toggleRaii = RaiiProp<SimpleTask<Unit,Unit>>(Opt.of())

    override fun close()
    {
        toggleRaii.close()
    }

    fun onKeyEvent(event:KeyEvent):Boolean = lock.withLock()
    {
        if (event.keyCode in keyCodes)
        {
            when (event.action)
            {
                // execute action after long press delay
                KeyEvent.ACTION_DOWN ->
                {
                    toggleRaii.value = run()
                    {
                        val actionToSchedule = SimpleTask<Unit,Unit> {action(event)}
                        postOnUiThread(ViewConfiguration.getLongPressTimeout().toLong()) {actionToSchedule(Unit)};
                        {Opt.of(actionToSchedule)}
                    }
                    false
                }

                // consume event if action is triggered; don't consume otherwise
                KeyEvent.ACTION_UP ->
                {
                    val shouldConsumeEvent = toggleRaii.value.invoke().opt?.wasInvokedOrClosed
                            ?: false
                    toggleRaii.close()
                    shouldConsumeEvent
                }

                // ignore & propagate unhandled events
                else -> false
            }
        }
        else
        {
            false
        }
    }
}

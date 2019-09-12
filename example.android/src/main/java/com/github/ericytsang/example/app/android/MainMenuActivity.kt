package com.github.ericytsang.example.app.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.ericytsang.androidlib.cannotopenlinkdialog.CannotOpenLinkActivity
import com.github.ericytsang.androidlib.confirmdialog.ConfirmDialogActivity
import com.github.ericytsang.androidlib.core.context.wrap
import com.github.ericytsang.androidlib.core.forceExhaustiveWhen
import com.github.ericytsang.app.example.android.databinding.ActivityMainMenuBinding
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.mutableNullableValue
import com.github.ericytsang.lib.prop.nullableValue
import java.io.Closeable

class MainMenuActivity:AppCompatActivity()
{
    private val confirmDialogTest = ConfirmDialogActivity.Companion<Int>()

    // created lifecycle
    private val created = RaiiProp(Opt.of<Created>())
    override fun onCreate(savedInstanceState:Bundle?)
    {
        super.onCreate(savedInstanceState)
        created.mutableNullableValue = {Created(this)}
    }
    override fun onDestroy()
    {
        created.close()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?)
    {
        val activityResult = OnActivityResultCode
                .values()
                .getOrNull(requestCode)
                ?.parseIntent(this,data!!)
        if (activityResult != null)
        {
            created.nullableValue?.onActivityResult(resultCode,activityResult)
        }
        else
        {
            super.onActivityResult(requestCode,resultCode,data)
        }
    }

    private class Created(
            val activity:MainMenuActivity)
        :Closeable
    {
        private val closeable = CloseableGroup()
        override fun close() = closeable.close()

        val contentView = ActivityMainMenuBinding
                .inflate(activity.layoutInflater,activity.findViewById(android.R.id.content),false)
                .apply {activity.setContentView(root)}

        // confirm dialog
        init
        {
            contentView.confirmDialogButton.setOnClickListener()
            {
                activity.confirmDialogTest.startActivityForResult(
                        activity,
                        OnActivityResultCode.ConfirmDialog.ordinal,
                        ConfirmDialogActivity.Params(
                                "title",
                                "prompt",
                                ConfirmDialogActivity.ButtonConfig(
                                        true,
                                        "yes",
                                        null,
                                        View.VISIBLE),
                                ConfirmDialogActivity.ButtonConfig(
                                        true,
                                        "no",
                                        null,
                                        View.VISIBLE),
                                0))
            }
        }

        // cannot open link activity
        init
        {
            contentView.openLinkButton.setOnClickListener()
            {
                CannotOpenLinkActivity.tryOpenLink(
                        activity.wrap(),
                        contentView.linkToOpenInput.text.toString())
            }
        }

        fun onActivityResult(resultCode:Int,activityResult:OnActivityResult)
        {
            when(activityResult)
            {
                is OnActivityResult.ConfirmDialog ->
                {
                    activityResult.result.extraUserData
                }
            }.forceExhaustiveWhen
        }
    }

    private sealed class OnActivityResult
    {
        data class ConfirmDialog(
                val result:ConfirmDialogActivity.Result<Int>
        ):OnActivityResult()
    }

    private enum class OnActivityResultCode
    {
        ConfirmDialog,
        ;

        companion object
        {
            fun fromResultCodeInt(requestCode:Int) = values().getOrNull(requestCode)
        }

        fun toResultCodeInt() = ordinal

        fun parseIntent(
                activity:MainMenuActivity,
                intent:Intent
        ):
                OnActivityResult = when(this)
        {
            ConfirmDialog -> OnActivityResult.ConfirmDialog(
                    activity.confirmDialogTest.parseOnActivityResult(intent)
            )
        }
    }
}

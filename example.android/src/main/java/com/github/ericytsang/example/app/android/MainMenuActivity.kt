package com.github.ericytsang.example.app.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.ericytsang.androidlib.alertdialog.AlertDialogActivity
import com.github.ericytsang.androidlib.cannotopenlinkdialog.CannotOpenLinkActivity
import com.github.ericytsang.androidlib.confirmdialog.ConfirmDialogActivity
import com.github.ericytsang.androidlib.core.context.wrap
import com.github.ericytsang.androidlib.core.forceExhaustiveWhen
import com.github.ericytsang.androidlib.core.getStringCompat
import com.github.ericytsang.app.example.android.R
import com.github.ericytsang.app.example.android.databinding.ActivityMainMenuBinding
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.mutableNullableValue
import com.github.ericytsang.lib.prop.nullableValue
import java.io.Closeable

// todo: fix all lint problems
// todo: make this project use all androidlib libraries
// todo: add UI unit tests
// todo: make the note-taking application

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

        val contentView:ActivityMainMenuBinding = ActivityMainMenuBinding
                .inflate(activity.layoutInflater,activity.findViewById(android.R.id.content),false)
                .apply {activity.setContentView(root)}

        // alert dialog
        init
        {
            contentView.alertDialogButton.setOnClickListener()
            {
                AlertDialogActivity.startActivityForResult(
                        activity,
                        OnActivityResultCode.AlertDialog.ordinal,
                        AlertDialogActivity.Params(
                                null,
                                contentView.alertTitleInput.text.toString().takeIf {it.isNotBlank()},
                                contentView.alertPromptInput.text.toString(),
                                activity.getStringCompat(android.R.string.ok)))
            }
        }

        // confirm dialog
        init
        {
            contentView.confirmDialogButton.setOnClickListener()
            {
                activity.confirmDialogTest.startActivityForResult(
                        activity,
                        OnActivityResultCode.ConfirmDialog.ordinal,
                        ConfirmDialogActivity.Params(
                                contentView.confirmTitleInput.text.toString().takeIf {it.isNotBlank()},
                                contentView.confirmPromptInput.text.toString(),
                                ConfirmDialogActivity.ButtonConfig(true,"yes",null,View.VISIBLE),
                                ConfirmDialogActivity.ButtonConfig(true,"no",null,View.VISIBLE),
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
                    contentView.confirmResultOutput.text = when(val result = activityResult.result)
                    {
                        is ConfirmDialogActivity.Result.ButtonPressed -> result.buttonId.name
                        is ConfirmDialogActivity.Result.Cancelled -> activity.getString(R.string.activity__main_menu__confirm__cancelled)
                    }
                }
                is OnActivityResult.AlertDialog ->
                {
                    contentView.alertResultOutput.text = activityResult.result.name
                }
            }.forceExhaustiveWhen
        }
    }

    private sealed class OnActivityResult
    {
        data class ConfirmDialog(
                val result:ConfirmDialogActivity.Result<Int>
        ):OnActivityResult()
        data class AlertDialog(
                val result:AlertDialogActivity.Result
        ):OnActivityResult()
    }

    private enum class OnActivityResultCode
    {
        ConfirmDialog,
        AlertDialog,
        ;

        fun parseIntent(
                activity:MainMenuActivity,
                intent:Intent
        ):
                OnActivityResult = when(this)
        {
            ConfirmDialog -> OnActivityResult.ConfirmDialog(
                    activity.confirmDialogTest.parseOnActivityResult(intent)
            )
            AlertDialog -> OnActivityResult.AlertDialog(
                    AlertDialogActivity.parseOnActivityResult(intent)
            )
        }
    }
}

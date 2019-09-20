package com.github.ericytsang.app.example.android

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
import com.github.ericytsang.androidlib.listitempickerdialog.ListItemPickerDialogActivity
import com.github.ericytsang.androidlib.texinputdialog.TextInputDialogActivity
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
    private val confirmDialogCompanion = ConfirmDialogActivity.Companion<Int>()
    private val listItemPickerDialogCompanion = ListItemPickerDialogActivity.Mediator<Int>()

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
            created.nullableValue?.onActivityResult(activityResult)
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
                activity.confirmDialogCompanion.startActivityForResult(
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

        // list item picker dialog
        var selectedInt:ListItemPickerDialogActivity.ListItem<Int>? = null
        init
        {
            contentView.listItemPickerButton.setOnClickListener()
            {
                activity.listItemPickerDialogCompanion.startActivityForResult(
                        activity,
                        OnActivityResultCode.ListItemPickerDialog.ordinal,
                        ListItemPickerDialogActivity.Params(
                                contentView.listItemPickerTitleInput.text.toString(),
                                contentView.listItemPickerPromptInput.text.toString(),
                                listOf(
                                        ListItemPickerDialogActivity.ListItem("Option 1","The number 1",1),
                                        ListItemPickerDialogActivity.ListItem("Option 2","The number 2",2),
                                        ListItemPickerDialogActivity.ListItem("Option 3","The number 3",3),
                                        ListItemPickerDialogActivity.ListItem("Option 4","The number 4",4)
                                ),
                                selectedInt
                        )
                )
            }
        }

        // text input dialog
        init
        {
            contentView.textInputButton.setOnClickListener()
            {
                TextInputDialogActivity.startActivityForResult(
                        activity,
                        OnActivityResultCode.TextInputDialog.ordinal,
                        TextInputDialogActivity.StartParams(
                                contentView.textInputDialogTitleInput.text.toString(),
                                contentView.textInputDialogPromptInput.text.toString(),
                                contentView.textInputDialogTextInput.text.toString()
                        )
                )
            }
        }

        // go to the preferences activity button
        init
        {
            contentView.gotoActivityPreferences.setOnClickListener()
            {
                SettingsActivity.start(activity.wrap(),SUnit())
            }
        }

        fun onActivityResult(activityResult:OnActivityResult)
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
                is OnActivityResult.ListItemPickerDialog ->
                {
                    selectedInt = activityResult.result.selected
                    contentView.listItemPickerResultOutput.text = selectedInt?.toString()?:"null"
                }
                is OnActivityResult.TextInputDialog ->
                {
                    contentView.textInputResultOutput.text = activityResult.result.toString()
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
        data class ListItemPickerDialog(
                val result:ListItemPickerDialogActivity.Result<Int>
        ):OnActivityResult()
        data class TextInputDialog(
                val result:TextInputDialogActivity.ResultParams
        ):OnActivityResult()
    }

    private enum class OnActivityResultCode
    {
        ConfirmDialog,
        AlertDialog,
        ListItemPickerDialog,
        TextInputDialog,
        ;

        fun parseIntent(
                activity:MainMenuActivity,
                intent:Intent
        ):
                OnActivityResult = when(this)
        {
            ConfirmDialog -> OnActivityResult.ConfirmDialog(
                    activity.confirmDialogCompanion.parseOnActivityResult(intent)
            )
            AlertDialog -> OnActivityResult.AlertDialog(
                    AlertDialogActivity.parseOnActivityResult(intent)
            )
            ListItemPickerDialog -> OnActivityResult.ListItemPickerDialog(
                    activity.listItemPickerDialogCompanion.parseOnActivityResult(intent)
            )
            TextInputDialog -> OnActivityResult.TextInputDialog(
                    TextInputDialogActivity.parseOnActivityResult(intent)
            )
        }
    }
}

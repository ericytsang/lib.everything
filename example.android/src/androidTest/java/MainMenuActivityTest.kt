package com.github.ericytsang.example.app.android

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import com.github.ericytsang.androidlib.confirmdialog.ConfirmDialogActivity
import com.github.ericytsang.androidlib.core.randomString
import com.github.ericytsang.app.example.android.MainMenuActivity
import com.github.ericytsang.androidlib.confirmdialog.R as ConfirmDialogR
import com.github.ericytsang.app.example.android.R
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class MainMenuActivityTest
{
    companion object
    {
        private const val randomInputStringLength = 5
    }

    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(MainMenuActivity::class.java)

    private val uiDevice = UiDevice.getInstance(getInstrumentation())

    private fun clickOutsideDialog()
    {
        uiDevice.click(5,uiDevice.displayHeight/2) // click outside dialog
    }



    /* confirm dialog tests */

    @Test
    fun confirmDialog_opens_on_button_clicked()
    {
        onView(withId(R.id.confirm_dialog_button)).perform(click())
        onView(withId(ConfirmDialogR.id.textview)).check(matches(isDisplayed()))
    }

    @Test
    fun confirmDialog_title_displays_title()
    {
        val titleText = Random.Default.randomString(randomInputStringLength)
        onView(withId(R.id.confirm_title_input)).perform(typeText(titleText))
        confirmDialog_opens_on_button_clicked()
        onView(withText(titleText)).check(matches(isDisplayed()))
    }

    @Test
    fun confirmDialog_summary_displays_summary()
    {
        val titleText = Random.Default.randomString(randomInputStringLength)
        onView(withId(R.id.confirm_prompt_input)).perform(typeText(titleText))
        confirmDialog_opens_on_button_clicked()
        onView(withId(ConfirmDialogR.id.textview)).check(matches(isDisplayed()))
    }

    @Test
    fun confirmDialog_OnActivityResult_negative_button()
    {
        confirmDialog_opens_on_button_clicked()
        onView(withId(ConfirmDialogR.id.button__cancel)).perform(click())
        onView(withId(R.id.confirm_result_output)).check(matches(withText(ConfirmDialogActivity.ButtonId.NO_BUTTON.name)))
    }

    @Test
    fun confirmDialog_OnActivityResult_positive_button()
    {
        confirmDialog_opens_on_button_clicked()
        onView(withId(ConfirmDialogR.id.button__ok)).perform(click())
        onView(withId(R.id.confirm_result_output)).check(matches(withText(ConfirmDialogActivity.ButtonId.YES_BUTTON.name)))
    }

    @Test
    fun confirmDialog_OnActivityResult_back_cancels()
    {
        confirmDialog_opens_on_button_clicked()
        pressBack()
        onView(withId(R.id.confirm_result_output)).check(matches(withText("cancelled")))
    }

    @Test
    fun confirmDialog_OnActivityResult_tap_outside_cancels()
    {
        confirmDialog_opens_on_button_clicked()
        clickOutsideDialog()
        onView(withId(R.id.confirm_result_output)).check(matches(withText("cancelled")))
    }
}

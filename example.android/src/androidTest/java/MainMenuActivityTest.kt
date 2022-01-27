package com.github.ericytsang.example.app.android


import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.ericytsang.androidlib.confirmdialog.ConfirmDialogActivity
import com.github.ericytsang.androidlib.core.randomString
import com.github.ericytsang.app.example.android.MainMenuActivity
import com.github.ericytsang.app.example.android.R
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random
import com.github.ericytsang.androidlib.alertdialog.R as AlertDialogR
import com.github.ericytsang.androidlib.confirmdialog.R as ConfirmDialogR
import com.github.ericytsang.androidlib.texinputdialog.R as TextInputDialogR


class MainMenuActivityTest
{
    companion object
    {
        private const val randomInputStringLength = 5
    }

    @get:Rule
    val activityTestRule = ActivityScenarioRule(MainMenuActivity::class.java)



    /* alert dialog tests */

    @Test
    fun alertDialog_opens_on_button_clicked()
    {
        closeSoftKeyboard()
        onView(withId(R.id.alert_dialog_button)).perform(betterScrollTo(),click())
        onView(withId(AlertDialogR.id.textview)).check(matches(isDisplayed()))
    }

    @Test
    fun alertDialog_title_displays_entered_title()
    {
        val titleText = Random.Default.randomString(randomInputStringLength)
        onView(withId(R.id.alert_title_input)).perform(betterScrollTo(),typeText(titleText))
        alertDialog_opens_on_button_clicked()
        onView(withText(titleText)).check(matches(isDisplayed()))
    }

    @Test
    fun alertDialog_summary_displays_entered_summary()
    {
        val promptText = Random.Default.randomString(randomInputStringLength)
        onView(withId(R.id.alert_prompt_input)).perform(betterScrollTo(),typeText(promptText))
        alertDialog_opens_on_button_clicked()
        onView(withId(AlertDialogR.id.textview)).check(matches(withText(promptText)))
    }

    @Test
    fun alertDialog_OnActivityResult_negative_button()
    {
        alertDialog_opens_on_button_clicked()
        onView(withId(AlertDialogR.id.button__dismiss)).perform(click())
        onView(withId(R.id.alert_result_output)).check(matches(withText("Ok")))
    }

    @Test
    fun alertDialog_OnActivityResult_back_cancels()
    {
        alertDialog_opens_on_button_clicked()
        pressBack()
        onView(withId(R.id.alert_result_output)).check(matches(withText("Cancelled")))
    }




    /* confirm dialog tests */

    @Test
    fun confirmDialog_opens_on_button_clicked()
    {
        closeSoftKeyboard()
        onView(withId(R.id.confirm_dialog_button)).perform(betterScrollTo(),click())
        onView(withId(ConfirmDialogR.id.textview)).check(matches(isDisplayed()))
    }

    @Test
    fun confirmDialog_title_displays_entered_title()
    {
        val titleText = Random.Default.randomString(randomInputStringLength)
        onView(withId(R.id.confirm_title_input)).perform(betterScrollTo(),typeText(titleText))
        confirmDialog_opens_on_button_clicked()
        onView(withText(titleText)).check(matches(isDisplayed()))
    }

    @Test
    fun confirmDialog_summary_displays_entered_summary()
    {
        val promptText = Random.Default.randomString(randomInputStringLength)
        onView(withId(R.id.confirm_prompt_input)).perform(betterScrollTo(),typeText(promptText))
        confirmDialog_opens_on_button_clicked()
        onView(withId(ConfirmDialogR.id.textview)).check(matches(withText(promptText)))
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




    /* text input dialog tests */

    @Test
    fun textInputDialog_opens_on_button_clicked()
    {
        onView(withId(R.id.text_input_button)).perform(betterScrollTo(),click())
        onView(withId(TextInputDialogR.id.edittext)).check(matches(isDisplayed()))
    }
}

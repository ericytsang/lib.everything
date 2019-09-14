package com.github.ericytsang.example.app.android

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import com.github.ericytsang.androidlib.confirmdialog.R as ConfirmDialogR
import com.github.ericytsang.app.example.android.R
import org.junit.Rule
import org.junit.Test

class MainMenuActivityTest
{
    @Rule
    @JvmField
    var activityTestRule = ActivityTestRule(MainMenuActivity::class.java)

    @Test
    fun confirm_dialog_opens_on_button_clicked()
    {
        onView(withId(R.id.confirm_dialog_button)).perform(click())
        onView(withId(ConfirmDialogR.id.textview)).check(matches(isDisplayed()))
    }
}

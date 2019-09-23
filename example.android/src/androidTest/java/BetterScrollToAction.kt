package com.github.ericytsang.example.app.android

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

class BetterScrollToAction:ViewAction by ScrollToAction()
{
    override fun getConstraints():Matcher<View>
    {
        return CoreMatchers.allOf(
                ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                ViewMatchers.isDescendantOfA(CoreMatchers.anyOf(
                        ViewMatchers.isAssignableFrom(ScrollView::class.java),
                        ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
                        ViewMatchers.isAssignableFrom(NestedScrollView::class.java))))
    }
}
package com.github.ericytsang.example.app.android

import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions

// convenience method
fun betterScrollTo():ViewAction
{
    return ViewActions.actionWithAssertions(BetterScrollToAction())
}
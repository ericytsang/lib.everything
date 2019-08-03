package com.github.ericytsang.androidlib.core.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.github.ericytsang.androidlib.core.context.wrap
import com.github.ericytsang.androidlib.core.intent.StartableIntent.StartableForResultIntent.ActivityForResultIntent
import java.io.Serializable

abstract class ActivityWithResultCompanion<Subclass:Activity,ActivityParams:Serializable,ActivityResult:Serializable>
    :ContextCompanion<Subclass,ActivityParams,ActivityForResultIntent>(ActivityForResultIntent)
{
    private val activityResultExtraKey = "${ActivityWithResultCompanion::class.qualifiedName}.activityResultExtraKey"
    fun startActivityForResult(context:Activity,requestCode:Int,params:ActivityParams)
    {
        toIntent(params).start(context.wrap(requestCode))
    }
    fun startActivityForResult(context:Fragment,requestCode:Int,params:ActivityParams)
    {
        toIntent(params).start(context.wrap(requestCode))
    }
    fun toIntent(context:Context,result:ActivityResult):Intent
    {
        val intent = Intent(context,contextClass)
        intent.putExtra(activityResultExtraKey,result)
        return intent
    }
    fun setOnActivityResult(activity:Subclass,result:ActivityResult)
    {
        val intent = toIntent(activity,result)
        activity.setResult(Activity.RESULT_OK,intent)
    }
    fun parseOnActivityResult(intent:Intent):ActivityResult
    {
        @Suppress("UNCHECKED_CAST")
        return intent.getSerializableExtra(activityResultExtraKey)!! as ActivityResult
    }
}

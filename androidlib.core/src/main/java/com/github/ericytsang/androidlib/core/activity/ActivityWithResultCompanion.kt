package com.github.ericytsang.androidlib.core.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import java.io.Serializable

abstract class ActivityWithResultCompanion<TActivity:Activity,ActivityParams:Serializable,ActivityResult:Serializable>
    :ContextCompanion<TActivity,ActivityParams>(ActivityIntent.FACTORY)
{
    private val activityResultExtraKey = "${ActivityWithResultCompanion::class.qualifiedName}.activityResultExtraKey"
    fun startActivityForResult(context:Activity,requestCode:Int,params:ActivityParams)
    {
        context.startActivityForResult(toIntent(params).intent(context),requestCode)
    }
    fun startActivityForResult(context:Fragment,requestCode:Int,params:ActivityParams)
    {
        context.startActivityForResult(toIntent(params).intent(context.activity!!),requestCode)
    }
    fun toIntent(context:Context,result:ActivityResult):Intent
    {
        val intent = Intent(context,contextClass)
        intent.putExtra(activityResultExtraKey,result)
        return intent
    }
    fun setOnActivityResult(activity:TActivity,result:ActivityResult)
    {
        val intent = toIntent(activity,result)
        activity.setResult(Activity.RESULT_OK,intent)
    }
    fun parseOnActivityResult(intent:Intent):ActivityResult
    {
        return intent.getSerializableExtra(activityResultExtraKey)!! as ActivityResult
    }
}

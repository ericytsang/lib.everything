package com.github.ericytsang.lib.android.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import java.io.Serializable

abstract class ActivityWithResultCompanion<Activity:AppCompatActivity,ActivityParams:Serializable,ActivityResult:Serializable>
    :ContextCompanion<Activity,ActivityParams>(ActivityIntent.FACTORY)
{
    private val activityResultExtraKey = "${ActivityWithResultCompanion::class.qualifiedName}.activityResultExtraKey"
    fun startActivityForResult(context:AppCompatActivity,requestCode:Int,params:ActivityParams)
    {
        context.startActivityForResult(toIntent(params).intent(context),requestCode)
    }
    protected fun setOnActivityResult(activity:Activity,result:ActivityResult)
    {
        val intent = Intent(activity,contextClass)
        intent.putExtra(activityResultExtraKey,result)
        activity.setResult(android.app.Activity.RESULT_OK,intent)
    }
    fun parseOnActivityResult(intent:Intent):ActivityResult
    {
        return intent.getSerializableExtra(activityResultExtraKey)!! as ActivityResult
    }
}

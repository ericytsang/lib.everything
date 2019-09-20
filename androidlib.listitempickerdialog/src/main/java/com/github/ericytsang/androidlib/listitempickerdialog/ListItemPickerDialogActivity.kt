package com.github.ericytsang.androidlib.listitempickerdialog

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ericytsang.androidlib.core.activity.ActivityWithResultCompanion
import com.github.ericytsang.androidlib.core.kClass
import com.github.ericytsang.androidlib.core.layoutInflater
import com.github.ericytsang.androidlib.core.viewholder.ViewHolder
import com.github.ericytsang.lib.closeablegroup.CloseableGroup
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.mutableNullableValue
import java.io.Closeable
import java.io.Serializable
import com.github.ericytsang.androidlib.listitempickerdialog.databinding.DialogListBinding
import com.github.ericytsang.androidlib.listitempickerdialog.databinding.ListItemBinding

class ListItemPickerDialogActivity:AppCompatActivity()
{
    // start & on-result

    class Mediator<UserData:Serializable>
        :ActivityWithResultCompanion<
            ListItemPickerDialogActivity,
            Params<UserData>,
            Result<UserData>>()
    {
        override val contextClass get() = kClass<ListItemPickerDialogActivity>()
        override val paramsClass get() = kClass<Params<UserData>>()
        override val resultClass get() = kClass<Result<UserData>>()
    }

    data class Params<UserData:Serializable>(
            val title:String,
            val leadInText:String?,
            val listItems:List<ListItem<UserData>>,
            val selected:ListItem<UserData>?)
        :Serializable

    data class Result<UserData:Serializable>(
            val params:Params<UserData>,
            val selected:ListItem<UserData>?)
        :Serializable

    data class ListItem<UserData:Serializable>(
            val title:String,
            val subTitle:String?,
            val userData:UserData)
        :Serializable

    // Created

    private val created = RaiiProp<Created>(Opt.of())

    private class Created(
            val mediator:Mediator<Serializable>,
            val activity:ListItemPickerDialogActivity,
            val params:Params<Serializable>)
        :Closeable
    {
        private val closeables = CloseableGroup()
        override fun close() = closeables.close()

        private val layout = DialogListBinding
                .inflate(activity.layoutInflater,activity.findViewById(android.R.id.content),false)
                .apply {activity.setContentView(root)}

        // default activity result value in case we are cancelled
        init
        {
            mediator.setOnActivityResult(activity,Result(params,params.selected))
        }

        // set title text
        init
        {
            activity.title = params.title
        }

        // set lead-in text
        init
        {
            if (params.leadInText != null)
            {
                layout.leadInText.text = params.leadInText
                layout.leadInText.visibility = View.VISIBLE
            }
            else
            {
                layout.leadInText.visibility = View.GONE
            }
        }

        // set up recycler view
        init
        {
            closeables.chainedAddCloseables()
            {
                closeables ->
                layout.recyclerView.layoutManager = LinearLayoutManager(activity)
                layout.recyclerView.adapter = Adapter(params,object:ListItemViewHolder.Listener
                {
                    override fun click(listItem:ListItem<Serializable>)
                    {
                        mediator.setOnActivityResult(activity,Result(params,listItem))
                        activity.finish()
                    }
                })
                closeables += Closeable()
                {
                    layout.recyclerView.layoutManager = null
                    layout.recyclerView.adapter = null
                }
            }
        }
    }

    override fun onCreate(savedInstanceState:Bundle?)
    {
        super.onCreate(savedInstanceState)
        val mediator = Mediator<Serializable>()
        val params = mediator.fromIntent(intent)
        created.mutableNullableValue = {Created(mediator,this,params)}
    }

    override fun onDestroy()
    {
        created.close()
        super.onDestroy()
    }

    // recycler view adapter

    private class ListItemViewHolder
    private constructor(
            val layout:ListItemBinding,
            val listener:Listener)
        :ViewHolder<ListItemViewHolder.Model>(
            layout.root)
    {
        companion object
        {
            fun from(parent:ViewGroup,onClickListener:Listener):ListItemViewHolder
            {
                return ListItemViewHolder(
                        ListItemBinding.inflate(parent.context.layoutInflater,parent,false),
                        onClickListener)
            }
        }

        init
        {
            layout.root.setOnClickListener()
            {
                listener.click(model!!.listItem)
            }
        }

        override fun onSetModel(oldModel:Model?,newModel:Model)
        {
            layout.radiobutton.isChecked = newModel.isSelected
            layout.text1.text = newModel.listItem.title
            if (newModel.listItem.subTitle?.isNotBlank() == true)
            {
                layout.text2.text = newModel.listItem.subTitle
                layout.text2.visibility = View.VISIBLE
            }
            else
            {
                layout.text2.visibility = View.GONE
            }
        }

        override fun haveTheSameId(oldModel:Model,newModel:Model):Boolean
        {
            return oldModel == newModel
        }

        data class Model(
                val listItem:ListItem<Serializable>,
                val isSelected:Boolean)

        interface Listener
        {
            fun click(listItem:ListItem<Serializable>)
        }
    }

    private class Adapter(
            val params:Params<Serializable>,
            val listener:ListItemViewHolder.Listener)
        :RecyclerView.Adapter<ListItemViewHolder>()
    {
        init
        {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent:ViewGroup,viewType:Int):ListItemViewHolder
        {
            return ListItemViewHolder.from(parent,listener)
        }

        override fun getItemCount():Int
        {
            return params.listItems.size
        }

        override fun onBindViewHolder(holder:ListItemViewHolder,position:Int)
        {
            val listItem = params.listItems[position]
            holder.set_model(ListItemViewHolder.Model(listItem,listItem == params.selected))
        }

        override fun getItemId(position:Int):Long
        {
            return params.listItems[position].hashCode().toLong()
        }
    }
}

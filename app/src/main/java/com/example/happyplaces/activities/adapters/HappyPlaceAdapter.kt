package com.example.happyplaces.activities.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.Models.HappyPlaceModel
import com.example.happyplaces.R
import com.example.happyplaces.activities.AddHappyPlaceActivity
import com.example.happyplaces.activities.HappyPlaceDetailActivity
import com.example.happyplaces.activities.MainActivity
import com.example.happyplaces.database.DatabaseHandler

open class HappyPlaceAdapter(private val context: Context,
                        private var list : ArrayList<HappyPlaceModel>) : RecyclerView.Adapter<HappyPlaceAdapter.ViewHolder>() {

    private var onClickListener : OnClickListener? = null
    class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_title)
        val description: TextView = view.findViewById(R.id.tv_description)
        val circularImage:ImageView = view.findViewById(R.id.iv_circular_place_image)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_happy_place,parent,false)
        return ViewHolder(view)
    }
    fun setOnClickListener(onClickListener : OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

            holder.title.text = item.title
            holder.description.text = item.description
            holder.circularImage.setImageURI(Uri.parse(item.image))

        holder.itemView.setOnClickListener {
            if (onClickListener != null){
                onClickListener!!.onClick(position,item)
            }
        }


    }
    fun notifyEditItem(activity: Activity,position: Int,requestCode: Int){
        val intent = Intent(context,AddHappyPlaceActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DETAILS,list[position])
        activity.startActivityForResult(intent,requestCode)
        //I need this activity so i know which activity is going to start this
//      this adapter is bind with the main Activity so this start activity
//      for result start from main activity and we can hear for result in main activity also.
//        request code is required to know whether i want to edit or do something else
        //Finally don't forget to call notify about data change
        notifyItemChanged(position) // so we don't need to restart activity
    }
    fun removeAt(position: Int){
        val dbHandler = DatabaseHandler(context,null)
        val isDeleted =  dbHandler.deleteHappyPlace(list[position])
        if (isDeleted > 0) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    interface OnClickListener{
        fun onClick(position: Int, item: HappyPlaceModel)
    }
}
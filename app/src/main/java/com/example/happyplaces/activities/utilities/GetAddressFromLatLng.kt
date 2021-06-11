package com.example.happyplaces.activities.utilities

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

class GetAddressFromLatLng(context: Context,private val latitude : Double,private val longitude: Double
) : AsyncTask<Void,String,String>() {
//geoCoder is taking care of geolocation . It convert location into human readable form
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    private lateinit var mAddressListener : AddressListener
    override fun doInBackground(vararg params: Void?): String {
        try {
            val addressList : List<Address>? =
                geocoder.getFromLocation(latitude,longitude,1)

            if (addressList != null && addressList.isNotEmpty()){
                val address : Address = addressList[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex){
                    sb.append(address.getAddressLine(i)).append(" ")
                }
                sb.deleteCharAt(sb.length - 1)
                return sb.toString()
        }
        }catch (e:Exception){
            e.printStackTrace()
        }
        return ""
    }

    override fun onPostExecute(result: String?) {
        if (result == null){
            mAddressListener.onError()
        }else {
            mAddressListener.onAddressFound(result)
        }
        super.onPostExecute(result)
    }
    fun setAddressListener(addressListener : AddressListener){
        mAddressListener = addressListener
    }
    fun getAddress(){
        execute()
    }
    interface AddressListener{
        fun onAddressFound(address:String?)
        fun onError()
    }
}
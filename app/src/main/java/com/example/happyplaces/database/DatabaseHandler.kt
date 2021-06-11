package com.example.happyplaces.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.example.happyplaces.Models.HappyPlaceModel

class DatabaseHandler(context: Context,factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context,DATABASE_NAME,factory,DATABASE_VERSION) {

    companion object{
        private const val DATABASE_VERSION = 1 // dataBAse version
        private const val DATABASE_NAME = "HappyPlacesDatabase" // database name
        private const val TABLE_HAPPY_PLACE = "HappyPlaceTable" // table name

        //All the columns name
        private const val KEY_ID = "_id"
        private const val KEY_TITLE = "title"
        private const val KEY_IMAGE = "image"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_DATE = "date"
        private const val KEY_LOCATION = "location"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_LATITUDE = "latitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_HAPPY_PLACE_TABLE = ("CREATE TABLE " + TABLE_HAPPY_PLACE  + "(" +
                KEY_ID + " INTEGER PRIMARY KEY," +
                KEY_TITLE + " TEXT," +
                KEY_IMAGE  + " TEXT," +
                KEY_DESCRIPTION + " TEXT," +
                KEY_DATE + " TEXT,"+
                KEY_LOCATION + " TEXT," +
                KEY_LONGITUDE + " TEXT," +
                KEY_LATITUDE + " TEXT" + ")" )

        db?.execSQL(CREATE_HAPPY_PLACE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_HAPPY_PLACE")
        onCreate(db)
    }
    fun addHappyPlace(happyPlace: HappyPlaceModel) : Long{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE,happyPlace.title)
        contentValues.put(KEY_DESCRIPTION,happyPlace.description)
        contentValues.put(KEY_DATE,happyPlace.date)
        contentValues.put(KEY_IMAGE,happyPlace.image)
        contentValues.put(KEY_LOCATION,happyPlace.location)
        contentValues.put(KEY_LONGITUDE,happyPlace.longitude)
        contentValues.put(KEY_LATITUDE,happyPlace.latitude)

        val result = db.insert(TABLE_HAPPY_PLACE,null,contentValues)
        db.close()
     return result
    }
    fun updateHappyPlace(happyPlace: HappyPlaceModel) : Int{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE,happyPlace.title)
        contentValues.put(KEY_DESCRIPTION,happyPlace.description)
        contentValues.put(KEY_DATE,happyPlace.date)
        contentValues.put(KEY_IMAGE,happyPlace.image)
        contentValues.put(KEY_LOCATION,happyPlace.location)
        contentValues.put(KEY_LONGITUDE,happyPlace.longitude)
        contentValues.put(KEY_LATITUDE,happyPlace.latitude)

        val success = db.update(TABLE_HAPPY_PLACE,contentValues, KEY_ID + "=" + happyPlace.id,null)
        db.close()
        return success
    }
    fun deleteHappyPlace(happyPlace: HappyPlaceModel) : Int{
        val db = this.writableDatabase

       val success =  db.delete(TABLE_HAPPY_PLACE, KEY_ID + "=" + happyPlace.id,null)
        db.close()

        return success
    }
    fun getHappyPlacesLists() : ArrayList<HappyPlaceModel>{

        val happyPlaceList = ArrayList<HappyPlaceModel>()

        val selectQuery = "SELECT * FROM $TABLE_HAPPY_PLACE"

        val db = this.readableDatabase

        try {
           val cursor = db.rawQuery(selectQuery,null)

            if (cursor.moveToFirst()){
                do {
                    val id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                    val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                    val description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                    val date = cursor.getString(cursor.getColumnIndex(KEY_DATE))
                    val image = cursor.getString(cursor.getColumnIndex(KEY_IMAGE))
                    val location = cursor.getString(cursor.getColumnIndex(KEY_LOCATION))
                    val longitude = cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE))
                    val latitude = cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE))

                    val place = HappyPlaceModel(id,title,image,description,date,location,latitude,longitude)
                    happyPlaceList.add(place)

                }while (cursor.moveToNext())
            }
            cursor.close()

        }catch (e : SQLiteException){
            db.execSQL(selectQuery)
            return ArrayList()
        }
        return happyPlaceList
    }
}
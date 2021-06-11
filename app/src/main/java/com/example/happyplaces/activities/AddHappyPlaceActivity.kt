package com.example.happyplaces.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.happyplaces.Models.HappyPlaceModel
import com.example.happyplaces.R
import com.example.happyplaces.activities.utilities.GetAddressFromLatLng
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val GALLERY_REQUESTED_CODE = 1
        private const val CAMERA_REQUESTED_CODE = 2
        private const val IMAGE_DIRECTORY = "HappyPlaceImages"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

    private val calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var binding: ActivityAddHappyPlaceBinding
    private var saveImageToInternalStorage: Uri? = null
    private var mLongitude: Double = 0.0
    private var mLatitude: Double = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null

    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAddPlace.setNavigationOnClickListener {
            onBackPressed()
        }
        mFusedLocationClient = FusedLocationProviderClient(this)
        //For places google map api and initialize it with key if it is not
        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity,resources.getString(R.string.google_maps_api_key))
        }
        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS)
        }
        dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }
        updateDateInView()

        if (mHappyPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            binding.etDate.setText(mHappyPlaceDetails!!.date)
            binding.etTitle.setText(mHappyPlaceDetails!!.title)
            binding.etDescription.setText(mHappyPlaceDetails!!.description)
            binding.etLocation.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            binding.ivPlaceImage.setImageURI(saveImageToInternalStorage)

            binding.btnSave.text = "UPDATE"
        }


        binding.etDate.setOnClickListener(this)
        binding.tvAddImage.setOnClickListener(this)
        binding.btnSave.setOnClickListener(this)
        binding.etLocation.setOnClickListener(this)
        binding.tvSelectCurrentLocation.setOnClickListener(this)
    }

    private fun isLocationEnabled() : Boolean{
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                val selectPickerDialog = AlertDialog.Builder(this)
                selectPickerDialog.setTitle("Choose action")
                val selectPickerDialogItems =
                    arrayOf("Select photo from gallery", "Capture photo from camera")
                selectPickerDialog.setItems(selectPickerDialogItems) { dialog, which ->
                    when (which) {
                        0 -> {
                            choosePhotoFromGallery()
                        }
                        1 -> {

                            capturePhotoFromCamera()
                        }
                    }
                }
                selectPickerDialog.show()

            }
            R.id.btn_save -> {
                when {
                    binding.etTitle.text.isNullOrEmpty() -> Toast.makeText(
                        this,
                        "Please enter title",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.etDescription.text.isNullOrEmpty() -> Toast.makeText(
                        this,
                        "Please enter a description",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.etLocation.text.isNullOrEmpty() -> Toast.makeText(
                        this,
                        "Please enter a location",
                        Toast.LENGTH_SHORT
                    ).show()
                    saveImageToInternalStorage == null -> Toast.makeText(
                        applicationContext,
                        "Please select an image",
                        Toast.LENGTH_SHORT
                    ).show()
                    else -> {
                        val title = binding.etTitle.text.toString()
                        val description = binding.etDescription.text.toString()
                        val date = binding.etDate.text.toString()
                        val location = binding.etLocation.text.toString()
                        val image = saveImageToInternalStorage.toString()

                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                            title, image, description, date, location, mLatitude, mLongitude
                        )

                        val databaseHandler = DatabaseHandler(this, null)
                        if (mHappyPlaceDetails == null) {
                            val addHappyPlace = databaseHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }

                        } else {
                            val updateHappyPlace = databaseHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }

                        }

                    }
                }

            }
            R.id.et_location ->{
                try {
                    val fields = listOf(Place.Field.ID,Place.Field.NAME,
                        Place.Field.LAT_LNG,Place.Field.ADDRESS)
                    //Start the autocomplete intent with the unique request code
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields)
                        .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)

                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
            R.id.tv_select_current_location ->{
                if(!isLocationEnabled()){
                    Toast.makeText(applicationContext, "Your location is turned off. Please turned it on", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withContext(this).withPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        .withListener(object :MultiplePermissionsListener{
                            override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                                if(p0!!.areAllPermissionsGranted()){
                                   requestNewLocationData()
                                }
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                p0: MutableList<PermissionRequest>?,
                                p1: PermissionToken?
                            ) {
                                showRationDialogForPermissions()
                            }

                        }).onSameThread().check()


                }
            }
        }
    }
    private fun requestNewLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.fastestInterval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallBack, Looper.myLooper())
    }
    private val mLocationCallBack = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation : Location = locationResult!!.lastLocation
            mLatitude = mLastLocation.latitude
            mLongitude = mLastLocation.longitude

            val addressTask  = GetAddressFromLatLng(this@AddHappyPlaceActivity,
                                        mLatitude,mLongitude)
            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    binding.etLocation.setText(address)
                }

                override fun onError() {
                    Log.e("Get Address ::","Something went wrong")
                }

            })
            addressTask.getAddress()

        }
    }

    private fun capturePhotoFromCamera() {
        Dexter.withContext(this).withPermissions(
            android.Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                if (p0!!.areAllPermissionsGranted()) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(cameraIntent, CAMERA_REQUESTED_CODE)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {
                showRationDialogForPermissions()
            }
        }
        ).onSameThread().check()

    }

    private fun choosePhotoFromGallery() {
        Dexter.withContext(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                if (p0!!.areAllPermissionsGranted()) {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, GALLERY_REQUESTED_CODE)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {
                p1!!.continuePermissionRequest()
                showRationDialogForPermissions()
            }
        }
        ).onSameThread().check()

    }

    private fun showRationDialogForPermissions() {
        AlertDialog.Builder(this).setMessage(
            "It looks like you have turned off permission" +
                    " required for this feature. It can enabled under " +
                    "the Application settings."
        )
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    settingIntent.data = uri
                    startActivity(settingIntent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView() {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        binding.etDate.setText(sdf.format(calendar.time).toString())

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUESTED_CODE) {
                if (data != null) {
                    try {
                        val bitmap =
                            MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)
                        //we can't test this because we have to reading it now, So we create a val and log it
                        saveImageToInternalStorage = saveImageToInternalStorage(bitmap)
                        binding.ivPlaceImage.setImageURI(data.data)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this,
                            "Failed to load image from gallery!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (requestCode == CAMERA_REQUESTED_CODE) {
                if (data != null) {
                    val capturedBitmap: Bitmap = data.extras!!.get("data") as Bitmap
                    saveImageToInternalStorage = saveImageToInternalStorage(capturedBitmap)
                    binding.ivPlaceImage.setImageBitmap(capturedBitmap)
                }
            }else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)

                binding.etLocation.setText(place.address)
                mLongitude = place.latLng!!.longitude
                mLatitude = place.latLng!!.latitude

            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
        stream.close()

        return Uri.parse(file.absolutePath)
    }
}


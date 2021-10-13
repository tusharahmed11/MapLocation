package com.example.maprnd

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.util.*
import android.location.Geocoder
import java.io.IOException


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private var currentMarker: Marker? = null
    private var currentLocation: Location? = null
    private var isPermission: Boolean = false
    private lateinit var googleMap: GoogleMap
    private lateinit var fabBtn: FloatingActionButton
    private var locationClient: FusedLocationProviderClient? = null
    private lateinit var locationManager: LocationManager
    private val GPS_REQUEST_CODE = 9001
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var bestProvider: String = ""
    private lateinit var criteria: Criteria
    private lateinit var addressTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabBtn = findViewById(R.id.fab)
        addressTv = findViewById(R.id.address_tv)
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        checkPermission()
        initMap()
        fabBtn.setOnClickListener {
            currentMarker?.remove()
            getCurrentLoc()

        }
        getCurrentLoc()

    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLoc() {
        locationClient?.lastLocation?.addOnSuccessListener {
            if (it != null){
                longitude = it.longitude
                latitude = it.latitude
                this.currentLocation = it
                initMap()
                gotoLocation(it.latitude,it.longitude)
            }else {
                if (isGPSenable()){
                    locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    criteria = Criteria()
                    bestProvider = (locationManager.getBestProvider(criteria,true)).toString()

                    val location = locationManager.getLastKnownLocation(bestProvider)
                    this.currentLocation = location
                    if (location != null){
                        latitude = location.latitude
                        longitude = location.longitude
                        this.currentLocation = location
                        initMap()
                        gotoLocation(latitude,longitude)
                    }else{
                        locationManager.requestLocationUpdates(bestProvider,1000,0f,this)
                    }

                }

            }
        }



    }

    private fun gotoLocation(latitude: Double, longitude: Double) {
        val latLang: LatLng = LatLng(latitude, longitude)
        val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngZoom(latLang,18f)
        googleMap.moveCamera(cameraUpdate)
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    private fun initMap() {
        if (isPermission){
            if (isGPSenable()){
                val mapSupportFragment : SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
                mapSupportFragment.getMapAsync(this)
            }
        }
    }

    private fun isGPSenable(): Boolean{
        val locationManager: LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val  providerEnable: Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (providerEnable){
            return true
        }else{
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("GPS Permission")
                .setMessage("GPS required for this app to work. Please enable GPS")
                .setPositiveButton("Yes", (DialogInterface.OnClickListener { dialog, which ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(intent,GPS_REQUEST_CODE)

                })).setCancelable(false)
            alertDialog.show()
        }

        return false
    }

    private fun checkPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(object : PermissionListener{
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
                isPermission = true
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {
               /* val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri: Uri = Uri.fromParts("package",packageName,"")
                intent.data = uri
                startActivity(intent)*/
            }

            override fun onPermissionRationaleShouldBeShown(
                request: PermissionRequest?,
                token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
            }

        }).check()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
       // googleMap.isMyLocationEnabled = true


        if(currentLocation != null){
            val latLong = LatLng(currentLocation?.latitude!!,currentLocation?.longitude!!)

            if (!(latitude == 0.0 && longitude == 0.0)){
                drawMarker(latLong)

                googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
                    override fun onMarkerDragStart(p0: Marker) {

                    }

                    override fun onMarkerDrag(p0: Marker) {
                    }

                    override fun onMarkerDragEnd(p0: Marker) {
                        if (currentMarker != null){
                            currentMarker?.remove()
                        }

                        val  newLatLng = LatLng(p0.position.latitude, p0.position.longitude)
                        drawMarker(newLatLng)
                    }

                })
            }



        }


    }

    private fun drawMarker(latLong: LatLng){
        val markerOption = MarkerOptions().position(latLong).title("I am here")
            .snippet(getAddress(latLong.latitude,latLong.longitude))
            .draggable(true)

        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLong))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong,15f))
        currentMarker = googleMap.addMarker(markerOption)
        currentMarker?.showInfoWindow()

    }

    private fun  getAddress(lat: Double, lon: Double): String {
        val geoCoder = Geocoder(this)
        val addresses = geoCoder.getFromLocation(lat,lon,1)

        return if (addresses.isNullOrEmpty()){
            "Empty"
        }else{
            addresses[0].getAddressLine(0).toString()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GPS_REQUEST_CODE){
            val  locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (providerEnable){
                Toast.makeText(this@MainActivity, "GPS is enabled", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this@MainActivity, "GPS is not enable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        locationManager.removeUpdates(this)
        latitude = location.latitude
        longitude = location.longitude
        gotoLocation(latitude, longitude)

        val addresses: List<Address>?
        val geocoder: Geocoder = Geocoder(this, Locale.getDefault())
        latitude = location.latitude
        longitude = location.longitude

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.size > 0) {
                val address = addresses[0].getAddressLine(0)
                val city = addresses[0].locality
                val state = addresses[0].adminArea
                val country = addresses[0].countryName
                val postalCode = addresses[0].postalCode
                val knownName = addresses[0].featureName
                addressTv.text = address
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}
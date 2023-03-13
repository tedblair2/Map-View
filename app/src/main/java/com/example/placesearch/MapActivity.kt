package com.example.placesearch

import android.Manifest.permission.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.placesearch.databinding.ActivityMapBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow

private const val REQUEST_CODE=21
class MapActivity : AppCompatActivity(),LocationListener {
    private val coarse=ACCESS_COARSE_LOCATION
    private val fine=ACCESS_FINE_LOCATION
    private val network=ACCESS_NETWORK_STATE
    private val storage=WRITE_EXTERNAL_STORAGE
    private lateinit var binding: ActivityMapBinding
    private lateinit var locationManager: LocationManager
    private lateinit var currentMarker:OverlayItem
    private lateinit var nextMarker: OverlayItem
    private var lat=0.0
    private var lon=0.0
    private var location: Location?=null
    private var currentLocation:GeoPoint?=null
    private lateinit var nextLocation:GeoPoint
    private lateinit var items:ArrayList<OverlayItem>
    private lateinit var mapController: MapController
    private lateinit var markerOverlay: ItemizedIconOverlay<OverlayItem>
    private var distance=""
//    private lateinit var roadOverlay:Polyline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMapBinding.inflate(layoutInflater)
        getInstance().load(this,PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(binding.root)
        supportActionBar?.hide()
        lat=intent.getStringExtra("lat")!!.toDouble()
        lon=intent.getStringExtra("lon")!!.toDouble()
        items= arrayListOf()
        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            requestPermission()
        }else{
            Toast.makeText(this, "Please enable you location", Toast.LENGTH_SHORT).show()
        }
    }
    private fun requestPermission(){
        if (ContextCompat.checkSelfPermission(this,coarse)!=PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,fine)!=PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,network) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,storage) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(coarse,fine,network,storage), REQUEST_CODE)
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,this)
            location= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            initMapView()
        }
    }

    private fun initMapView() {
        if (location != null){
            currentLocation=GeoPoint(location?.latitude!!, location?.longitude!!)
            Toast.makeText(this, "${location?.latitude} , ${location?.longitude}", Toast.LENGTH_SHORT).show()
        }
        nextLocation=GeoPoint(lat,lon)
        binding.mapview.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapview.setBuiltInZoomControls(true)
        binding.mapview.setMultiTouchControls(true)
        mapController= binding.mapview.controller as MapController
        mapController.setZoom(15.0)
        mapController.setCenter(currentLocation)
        currentMarker= OverlayItem("You","My Location",currentLocation)
        currentMarker.setMarker(ContextCompat.getDrawable(this,R.drawable.baseline_location_on_24))
        nextMarker= OverlayItem("Receiver","Location of Customer",nextLocation)
        nextMarker.setMarker(ContextCompat.getDrawable(this,R.drawable.baseline_location_on_24))
        items.add(currentMarker)
        items.add(nextMarker)
        markerOverlay=ItemizedIconOverlay(items,iconClick,applicationContext)
        binding.mapview.overlays.clear()
        binding.mapview.overlays.add(markerOverlay)
        try {
            val dist=(currentLocation!!.distanceToAsDouble(nextLocation))/1000
            distance= String.format("%.2f",dist)
            binding.distance.text="Distance: ${distance}Km"
            drawRoad(currentLocation!!,nextLocation)
        }catch (_:NullPointerException){}

    }
    private val iconClick=object :OnItemGestureListener<OverlayItem>{
        override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
            AlertDialog.Builder(this@MapActivity)
                .setTitle(item?.title)
                .setIcon(R.drawable.baseline_location_on_24)
                .setMessage(item?.snippet)
                .setPositiveButton("Ok"){dialog,_->
                    dialog.dismiss()
                }
                .show()
            return true
        }

        override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
            return false
        }
    }
    private fun drawRoad(start:GeoPoint,end:GeoPoint){
        CoroutineScope(Dispatchers.IO).launch {
            val roadManager=OSRMRoadManager(this@MapActivity,BuildConfig.APPLICATION_ID)
            val waypoints= arrayListOf<GeoPoint>()
            waypoints.add(start)
            waypoints.add(end)
            val road=roadManager.getRoad(waypoints)
            if (road.mStatus == Road.STATUS_OK) {
                val roadOverlay = RoadManager.buildRoadOverlay(road)
                binding.mapview.overlays.add(roadOverlay)
                withContext(Dispatchers.Main){
                    binding.mapview.invalidate()
                }
            } else {
                Toast.makeText(this@MapActivity, "Error when loading the road - status=${road.mStatus}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapview.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(this)
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE->{
                if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED &&
                    grantResults[1]==PackageManager.PERMISSION_GRANTED && grantResults[2]==PackageManager.PERMISSION_GRANTED
                    && grantResults[3]==PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,this)
                        location= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        initMapView()
                    }else{
                        Toast.makeText(this, "Please enable you location", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        requestPermission()
                    }else{
                        Toast.makeText(this, "Please enable you location", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            markerOverlay.removeItem(currentMarker)
            currentLocation= GeoPoint(location.latitude,location.longitude)
            mapController.animateTo(currentLocation)
            currentMarker= OverlayItem("You","My Location",currentLocation)
            currentMarker.setMarker(ContextCompat.getDrawable(this,R.drawable.baseline_location_on_24))
            markerOverlay.addItem(currentMarker)
            binding.mapview.overlays.clear()
            binding.mapview.overlays.add(markerOverlay)
            drawRoad(currentLocation!!,nextLocation)
            try {
                val dist=(currentLocation!!.distanceToAsDouble(nextLocation))/1000
                distance= String.format("%.2f",dist)
                binding.distance.text="Distance: ${distance}Km"
                if (dist<20){
                    locationManager.removeUpdates(this)
                }
            }catch (_:NullPointerException){}
        }else{
            Toast.makeText(this, "Please enable you location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
        super.onProviderEnabled(provider)
    }

    override fun onProviderDisabled(provider: String) {
        super.onProviderDisabled(provider)
    }
}
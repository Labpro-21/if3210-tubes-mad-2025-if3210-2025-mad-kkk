package com.example.purrytify

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.core.app.ActivityCompat


// OSMLocationPickerActivity (create this as a new file: OSMLocationPickerActivity.kt)
class OSMLocationPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var selectedMarker: Marker? = null
    private var selectedLocation: GeoPoint? = null
    private lateinit var btnConfirm: Button
    private lateinit var tvCoordinates: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_osm_location_picker)

        initializeViews()
        setupMap()
        setupClickListeners()
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.osmMapView)
        btnConfirm = findViewById(R.id.btnConfirmLocation)
        tvCoordinates = findViewById(R.id.tvCoordinates)

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(true)

        // Set default location (you can customize this based on user's country or current location)
        val startPoint = GeoPoint(-6.2088, 106.8456) // Jakarta, Indonesia
        val mapController = mapView.controller
        mapController.setZoom(10.0)
        mapController.setCenter(startPoint)

        // Add map click listener
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { point ->
                    selectLocation(point)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let { point ->
                    selectLocation(point)
                }
                return true
            }
        }

        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(0, mapEventsOverlay)
    }

    private fun selectLocation(point: GeoPoint) {
        selectedLocation = point

        // Remove previous marker
        selectedMarker?.let { marker ->
            mapView.overlays.remove(marker)
        }

        // Add new marker
        selectedMarker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected Location"
            snippet = "Lat: ${String.format("%.6f", point.latitude)}, Lng: ${String.format("%.6f", point.longitude)}"
            // You can set a custom icon here if you have one
            // icon = ContextCompat.getDrawable(this@OSMLocationPickerActivity, R.drawable.ic_location_pin)
        }

        mapView.overlays.add(selectedMarker)
        mapView.invalidate()

        // Update coordinates text
        tvCoordinates.text = "Selected: ${String.format("%.6f", point.latitude)}, ${String.format("%.6f", point.longitude)}"

        // Enable confirm button
        btnConfirm.isEnabled = true
        btnConfirm.alpha = 1.0f

        // Optionally, reverse geocode to get address
        reverseGeocode(point)
    }

    private fun reverseGeocode(point: GeoPoint) {
        // Simple reverse geocoding using Nominatim (OpenStreetMap's service)
        Thread {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?" +
                        "format=json&lat=${point.latitude}&lon=${point.longitude}&zoom=18&addressdetails=1"

                val response = java.net.URL(url).readText()

                runOnUiThread {
                    // You can parse the JSON response to get the full address
                    selectedMarker?.snippet = "Getting address..."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    selectedMarker?.snippet = "Address: Unable to get address"
                }
            }
        }.start()
    }

    private fun setupClickListeners() {
        btnConfirm.setOnClickListener {
            selectedLocation?.let { location ->
                val resultIntent = Intent().apply {
                    putExtra("latitude", location.latitude)
                    putExtra("longitude", location.longitude)
                    putExtra("address", selectedMarker?.snippet ?: "Selected Location")
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        // Add current location button (optional)
        findViewById<FloatingActionButton>(R.id.fabCurrentLocation)?.setOnClickListener {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        // Check for location permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            location?.let {
                val currentPoint = GeoPoint(it.latitude, it.longitude)
                mapView.controller.animateTo(currentPoint)
                selectLocation(currentPoint)
            }
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
package com.example.firebasegaonshield

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class GoogleMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fabCurrentLocation: FloatingActionButton
    private lateinit var firestore: FirebaseFirestore

    private val villages = listOf(
        Village("Leh Village, Ladakh", 34.1526, 77.5771, "Northern Region"),
        Village("Shimla Village, HP", 31.1048, 77.1734, "Northern Region"),
        Village("Rishikesh Village, Uttarakhand", 30.0869, 78.2676, "Northern Region"),
        Village("Jaipur Village, Rajasthan", 26.9124, 75.7873, "Western Region"),
        Village("Udaipur Village, Rajasthan", 24.5854, 73.7125, "Western Region"),
        Village("Ahmedabad Village, Gujarat", 23.0225, 72.5714, "Western Region"),
        Village("Varanasi Village, UP", 25.3176, 82.9739, "Central Region"),
        Village("Lucknow Village, UP", 26.8467, 80.9462, "Central Region"),
        Village("Patna Village, Bihar", 25.5941, 85.1376, "Eastern Region"),
        Village("Kolkata Village, WB", 22.5726, 88.3639, "Eastern Region"),
        Village("Guwahati Village, Assam", 26.1445, 91.7362, "North-East Region"),
        Village("Imphal Village, Manipur", 24.8170, 93.9368, "North-East Region"),
        Village("Bangalore Village, Karnataka", 12.9716, 77.5946, "Southern Region"),
        Village("Chennai Village, Tamil Nadu", 13.0827, 80.2707, "Southern Region"),
        Village("Kochi Village, Kerala", 9.9312, 76.2673, "Southern Region")
    )

    data class Village(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val region: String
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_google_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        fabCurrentLocation = view.findViewById(R.id.fabCurrentLocation)

        initializeMap()
        setupClickListeners()
    }

    private fun initializeMap() {
        // Get the SupportMapFragment and register for the map callback
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        if (mapFragment == null) {
            // If fragment doesn't exist, create it
            val newMapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map_container, newMapFragment)
                .commit()
            newMapFragment.getMapAsync(this)
        } else {
            mapFragment.getMapAsync(this)
        }
    }

    private fun setupClickListeners() {
        fabCurrentLocation.setOnClickListener {
            // Handle current location button click
            // You can implement location functionality here
            showToast("Current location feature coming soon")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable zoom controls
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = true

        // Add village markers and circles
        addVillageMarkersAndCircles()
        addHealthCenters()
        loadRiskZones()

        // Set initial camera position to show all India
        val indiaBounds = LatLngBounds(
            LatLng(8.0, 68.0), // Southwest corner
            LatLng(37.0, 97.0)  // Northeast corner
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(indiaBounds, 100))
    }

    private fun addVillageMarkersAndCircles() {
        villages.forEach { village ->
            val villageLatLng = LatLng(village.latitude, village.longitude)

            // Add marker for the village
            googleMap.addMarker(
                MarkerOptions()
                    .position(villageLatLng)
                    .title(village.name)
                    .snippet("Region: ${village.region}")
                    .icon(BitmapDescriptorFactory.defaultMarker(getColorForRegion(village.region)))
            )

            // Add circle around the village (5km radius)
            val circleOptions = CircleOptions()
                .center(villageLatLng)
                .radius(5000.0) // 5km in meters
                .strokeColor(getCircleColorForRegion(village.region))
                .fillColor(getCircleFillColorForRegion(village.region))
                .strokeWidth(2f)

            googleMap.addCircle(circleOptions)
        }
    }

    private fun addHealthCenters() {
        val healthCenters = listOf(
            Pair(LatLng(28.6139, 77.2090), "AIIMS Delhi"),
            Pair(LatLng(19.0760, 72.8777), "KEM Hospital Mumbai"),
            Pair(LatLng(13.0827, 80.2707), "Apollo Hospital Chennai"),
            Pair(LatLng(12.9716, 77.5946), "NIMHANS Bangalore"),
            Pair(LatLng(22.5726, 88.3639), "SSKM Hospital Kolkata")
        )

        healthCenters.forEach { (location, title) ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(title)
                    .snippet("Healthcare Center")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }
    }

    private fun loadRiskZones() {
        firestore.collection("risk_zones")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    val riskLevel = document.getString("riskLevel") ?: "LOW"

                    val riskZoneLatLng = LatLng(latitude, longitude)
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(riskZoneLatLng)
                            .title("Risk Zone")
                            .snippet("Risk Level: $riskLevel")
                            .icon(BitmapDescriptorFactory.defaultMarker(getRiskColor(riskLevel)))
                    )
                }
            }
    }

    private fun getRiskColor(riskLevel: String): Float {
        return when (riskLevel) {
            "HIGH" -> BitmapDescriptorFactory.HUE_RED
            "MEDIUM" -> BitmapDescriptorFactory.HUE_ORANGE
            else -> BitmapDescriptorFactory.HUE_YELLOW
        }
    }

    private fun getColorForRegion(region: String): Float {
        return when (region) {
            "Northern Region" -> BitmapDescriptorFactory.HUE_BLUE
            "Western Region" -> BitmapDescriptorFactory.HUE_ORANGE
            "Central Region" -> BitmapDescriptorFactory.HUE_RED
            "Eastern Region" -> BitmapDescriptorFactory.HUE_GREEN
            "North-East Region" -> BitmapDescriptorFactory.HUE_VIOLET
            "Southern Region" -> BitmapDescriptorFactory.HUE_YELLOW
            else -> BitmapDescriptorFactory.HUE_AZURE
        }
    }

    private fun getCircleColorForRegion(region: String): Int {
        return when (region) {
            "Northern Region" -> Color.BLUE
            "Western Region" -> Color.parseColor("#FF9800") // Orange
            "Central Region" -> Color.RED
            "Eastern Region" -> Color.GREEN
            "North-East Region" -> Color.MAGENTA
            "Southern Region" -> Color.YELLOW
            else -> Color.CYAN
        }
    }

    private fun getCircleFillColorForRegion(region: String): Int {
        return when (region) {
            "Northern Region" -> Color.argb(30, 0, 0, 255) // Light blue
            "Western Region" -> Color.argb(30, 255, 152, 0) // Light orange
            "Central Region" -> Color.argb(30, 255, 0, 0) // Light red
            "Eastern Region" -> Color.argb(30, 0, 255, 0) // Light green
            "North-East Region" -> Color.argb(30, 255, 0, 255) // Light magenta
            "Southern Region" -> Color.argb(30, 255, 255, 0) // Light yellow
            else -> Color.argb(30, 0, 255, 255) // Light cyan
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
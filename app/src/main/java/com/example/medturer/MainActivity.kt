package com.example.medturer

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val listaDeMarcadores = mutableListOf<Marker>()
    private lateinit var searchView: SearchView
    private lateinit var menuButton: ImageView
    private lateinit var rightCircle: View
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Initialize UI elements
        searchView = findViewById(R.id.searchView)
        menuButton = findViewById(R.id.menuButton)
        rightCircle = findViewById(R.id.rightCircle)
        map = findViewById(R.id.mapView)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Set the width of the navigation drawer to 75% of the screen
        val layoutParams = navigationView.layoutParams
        val displayMetrics = resources.displayMetrics
        layoutParams.width = (displayMetrics.widthPixels * 0.75).toInt()
        navigationView.layoutParams = layoutParams

        // Configure SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { buscarLugar(it) }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        // Configure menu button click listener to open the drawer
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Configure NavigationView item click listener (optional)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                    // Handle Home click
                }
                R.id.nav_places -> {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    // Handle Settings click
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                    // Crear un Intent para iniciar ProfileActivity
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Close the drawer after item selection
            true
        }

        // Configure right circle click listener (optional)
        rightCircle.setOnClickListener {
            Toast.makeText(this, "Black circle clicked", Toast.LENGTH_SHORT).show()
            // Implement any action for the black circle click here
        }

        // Configure the map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setScrollableAreaLimitLatitude(6.35, 6.12, 0)
        map.setScrollableAreaLimitLongitude(-75.69, -75.45, 0)
        map.minZoomLevel = 13.0
        val startPoint = GeoPoint(6.2518, -75.5636)
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)
        map.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                InfoWindow.closeAllInfoWindowsOn(map)
                map.performClick()
            }
            false
        }

        // Get tourist spots
        obtenerLugaresTuristicos()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun obtenerLugaresTuristicos() {
        val overpassQuery = """
            [out:json];
            (
                node["tourism"="museum"](6.12,-75.69,6.35,-75.45);
                node["tourism"="attraction"](6.12,-75.69,6.35,-75.45);
                node["tourism"="zoo"](6.12,-75.69,6.35,-75.45);
                node["tourism"="theme_park"](6.12,-75.69,6.35,-75.45);
                node["historic"](6.12,-75.69,6.35,-75.45);
            );
            out;
        """.trimIndent()

        val url = "https://overpass-api.de/api/interpreter?data=${overpassQuery.replace("\n", "")}"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    runOnUiThread { agregarMarcadores(JSONObject(jsonString)) }
                }
            }
        })
    }

    private fun agregarMarcadores(json: JSONObject) {
        val elements = json.getJSONArray("elements")

        for (i in 0 until elements.length()) {
            val lugar = elements.getJSONObject(i)
            val lat = lugar.optDouble("lat", 0.0)
            val lon = lugar.optDouble("lon", 0.0)
            val tags = lugar.optJSONObject("tags") ?: JSONObject()
            val nombre = tags.optString("name", "Lugar turístico")

            if (lat != 0.0 && lon != 0.0) {
                val marker = Marker(map)
                marker.position = GeoPoint(lat, lon)
                marker.title = nombre
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                listaDeMarcadores.add(marker)
                map.overlays.add(marker)
            }
        }

        map.addMapListener(object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                return false
            }

            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                val zoomLevel = map.zoomLevelDouble
                for (marker in listaDeMarcadores) {
                    marker.isEnabled = zoomLevel >= 15
                }
                map.invalidate()
                return true
            }
        })

        map.invalidate()
    }

    private fun buscarLugar(query: String) {
        listaDeMarcadores.forEach { marker ->
            if (marker.title?.contains(query, ignoreCase = true) == true) {
                map.controller.animateTo(marker.position)
                map.controller.setZoom(15.0)
                marker.showInfoWindow()
                return
            } else {
                InfoWindow.closeAllInfoWindowsOn(map)
            }
        }

        if (listaDeMarcadores.none { it.title?.contains(query, ignoreCase = true) == true }) {
            println("Buscando en la API: $query")
            buscarLugarEnApi(query)
        }
    }

    private fun buscarLugarEnApi(query: String) {
        val overpassQuery = """
            [out:json];
            node["name~"$query",i](6.12,-75.69,6.35,-75.45);
            out;
        """.trimIndent()

        val url = "https://overpass-api.de/api/interpreter?data=${overpassQuery.replace("\n", "")}"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    runOnUiThread { agregarMarcadorResultadoBusqueda(JSONObject(jsonString)) }
                }
            }
        })
    }

    private fun agregarMarcadorResultadoBusqueda(json: JSONObject) {
        val elements = json.getJSONArray("elements")

        if (elements.length() > 0) {
            val lugar = elements.getJSONObject(0)
            val lat = lugar.optDouble("lat", 0.0)
            val lon = lugar.optDouble("lon", 0.0)
            val nombre = lugar.optJSONObject("tags")?.optString("name", "Resultado de búsqueda") ?: "Resultado de búsqueda"

            if (lat != 0.0 && lon != 0.0) {
                val marker = Marker(map)
                marker.position = GeoPoint(lat, lon)
                marker.title = nombre
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(marker)
                map.controller.animateTo(marker.position)
                map.controller.setZoom(15.0)
                marker.showInfoWindow()
                map.invalidate()
            } else {
                println("No se encontraron coordenadas para la búsqueda.")
            }
        } else {
            println("No se encontraron resultados para la búsqueda.")
        }
    }
}
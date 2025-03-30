package com.example.medturer

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        setContentView(R.layout.activity_main)

        // Configurar el mapa
        map = findViewById(R.id.mapView)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Fijar mapa solo en Medellín
        map.setScrollableAreaLimitLatitude(6.35, 6.12, 0)
        map.setScrollableAreaLimitLongitude(-75.69, -75.45, 0)

        // Limitar el zoom mínimo a 13 (para evitar alejarse demasiado)
        map.minZoomLevel = 13.0

        // Centrar el mapa en Medellín
        val startPoint = GeoPoint(6.2518, -75.5636)
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)

        // Cerrar cuadro de información al tocar fuera - Marca peligro por ser una biblioteca de terceros
        map.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                InfoWindow.closeAllInfoWindowsOn(map)
                map.performClick() // Cumplir con las reglas de accesibilidad
            }
            false
        }

        // Obtener lugares turísticos
        obtenerLugaresTuristicos()
    }

    override fun onResume() {
        super.onResume()
        map.performClick() // Garantiza que performClick() esté definido
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

                // Agregar marcador a la lista
                listaDeMarcadores.add(marker)
                map.overlays.add(marker)
            }
        }

        // Ocultar los punteros si el zoom es menor a 15
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
}

package com.example.myagent.data.util

import android.util.Log
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

object PlaceNameResolver {

    private const val NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&zoom=18&addressdetails=1&accept-language=ru"
    private const val USER_AGENT = "MyAgentApp/1.0 (Android)"
    private const val TIMEOUT_MS = 10_000
    private const val REQUEST_DELAY_MS = 1_000L
    private const val CACHE_SIZE = 100

    private val roadCache = LruCache<String, String>(CACHE_SIZE)
    private const val NO_RESULT = ""

    private val ROAD_ABBREVIATIONS = listOf(
        "улица" to "ул.",
        "проспект" to "пр.",
        "переулок" to "пер.",
        "площадь" to "пл.",
        "шоссе" to "ш.",
        "набережная" to "наб.",
        "бульвар" to "бул.",
        "аллея" to "ал.",
        "проезд" to "пр-д",
        "линия" to "лн.",
        "тракт" to "тр."
    )

    suspend fun resolve(lat: Double, lon: Double): String {
        OverpassGeocoder.getNearestPlace(lat, lon)?.let { place ->
            Log.wtf("PlaceName", "Overpass: '${place.name}' (${place.category}, ${place.distanceMeters}м)")
            return place.name
        }
        val road = requestRoad(lat, lon)
        if (road != null) {
            Log.wtf("PlaceName", "Nominatim: '$road'")
            return road
        }
        return String.format(Locale.ROOT, "%.4f, %.4f", lat, lon)
    }

    private suspend fun requestRoad(lat: Double, lon: Double): String? {
        val key = String.format(Locale.ROOT, "%.5f,%.5f", lat, lon)
        synchronized(roadCache) {
            roadCache[key]?.let { return if (it == NO_RESULT) null else it }
        }

        val road = withContext(Dispatchers.IO) {
            try {
                delay(REQUEST_DELAY_MS)
                fetchRoad(lat, lon)
            } catch (e: Exception) {
                Log.wtf("PlaceName", "ошибка Nominatim: ${e.message}")
                null
            }
        }

        synchronized(roadCache) {
            roadCache.put(key, road ?: NO_RESULT)
        }
        return road
    }

    private fun fetchRoad(lat: Double, lon: Double): String? {
        val url = String.format(Locale.ROOT, NOMINATIM_URL, lat, lon)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val address = JSONObject(body).optJSONObject("address") ?: return null
            val road = address.optString("road").trim()
            if (road.isEmpty()) {
                return null
            }
            val houseNumber = address.optString("house_number").trim()
            val shortRoad = abbreviateRoad(road)
            return if (houseNumber.isEmpty()) shortRoad else "$shortRoad, $houseNumber"
        } finally {
            connection.disconnect()
        }
    }

    private fun abbreviateRoad(road: String): String {
        val lower = road.lowercase(Locale.ROOT)
        for ((full, short) in ROAD_ABBREVIATIONS) {
            if (lower.endsWith(" $full")) {
                val base = road.substring(0, road.length - full.length).trim()
                if (base.isNotEmpty()) {
                    return "$short $base"
                }
            }
        }
        return road
    }
}

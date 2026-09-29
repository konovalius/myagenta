package com.example.myagent.data.util

import android.location.Location
import android.util.Log
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class PlaceCategory {
    ATTRACTION,
    LARGE,
    TOPOGRAPHY,
    CACHED
}

data class PlaceResult(
    val name: String,
    val category: PlaceCategory,
    val distanceMeters: Double
)

object OverpassGeocoder {

    private val ENDPOINTS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.openstreetmap.ru/api/interpreter"
    )

    private const val USER_AGENT = "MyAgentApp/1.0 (Android)"
    private const val TIMEOUT_MS = 10_000
    private const val CACHE_SIZE = 100
    private const val NO_RESULT = ""

    private val ATTRACTION_AMENITIES = setOf("theatre", "museum", "arts_centre", "place_of_worship")
    private val LARGE_AMENITIES = setOf("marketplace", "bus_station")
    private val LARGE_BUILDINGS = setOf("commercial", "retail", "public")
    private val TOPO_LANDUSE = setOf("forest", "meadow")

    private val cache = LruCache<String, String>(CACHE_SIZE)

    suspend fun getNearestPlace(lat: Double, lon: Double): PlaceResult? {
        val key = String.format(Locale.ROOT, "%.5f,%.5f", lat, lon)
        synchronized(cache) {
            cache[key]?.let { cached ->
                return if (cached == NO_RESULT) null else PlaceResult(cached, PlaceCategory.CACHED, 0.0)
            }
        }

        val result = withContext(Dispatchers.IO) {
            try {
                requestNearestPlace(lat, lon)
            } catch (e: Exception) {
                Log.wtf("OverpassGeocoder", "ошибка запроса: ${e.message}")
                null
            }
        }

        synchronized(cache) {
            cache.put(key, result?.name ?: NO_RESULT)
        }
        return result
    }

    private fun requestNearestPlace(lat: Double, lon: Double): PlaceResult? {
        val body = "data=" + URLEncoder.encode(buildQuery(lat, lon), "UTF-8")
        for (endpoint in ENDPOINTS) {
            try {
                val result = request(endpoint, body, lat, lon)
                Log.wtf("OverpassGeocoder", "эндпоинт $endpoint ответил: ${result?.name ?: "объектов нет"}")
                return result
            } catch (e: Exception) {
                Log.wtf("OverpassGeocoder", "эндпоинт $endpoint недоступен: ${e.message}")
            }
        }
        Log.wtf("OverpassGeocoder", "ни один эндпоинт не ответил")
        return null
    }

    private fun buildQuery(lat: Double, lon: Double): String {
        val latText = String.format(Locale.ROOT, "%.7f", lat)
        val lonText = String.format(Locale.ROOT, "%.7f", lon)
        return """
            [out:json][timeout:10];
            (
              node(around:20,$latText,$lonText)[name][tourism];
              node(around:20,$latText,$lonText)[name][historic];
              node(around:20,$latText,$lonText)[name][amenity~"theatre|museum|arts_centre|place_of_worship"];
              way(around:20,$latText,$lonText)[name][tourism];
              way(around:20,$latText,$lonText)[name][historic];
              way(around:20,$latText,$lonText)[name][amenity~"theatre|museum|arts_centre|place_of_worship"];

              node(around:10,$latText,$lonText)[name][railway=station];
              node(around:10,$latText,$lonText)[name][amenity~"marketplace|bus_station"];
              way(around:10,$latText,$lonText)[name][building~"commercial|retail|public"];
              way(around:10,$latText,$lonText)[name][shop=mall];
              way(around:10,$latText,$lonText)[name][railway=station];

              node(around:50,$latText,$lonText)[name][natural];
              node(around:50,$latText,$lonText)[name][leisure=park];
              way(around:50,$latText,$lonText)[name][natural];
              way(around:50,$latText,$lonText)[name][leisure=park];
              way(around:50,$latText,$lonText)[name][landuse~"forest|meadow"];
            );
            out center 10;
        """.trimIndent()
    }

    private fun request(endpoint: String, body: String, lat: Double, lon: Double): PlaceResult? {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parse(response, lat, lon)
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(response: String, lat: Double, lon: Double): PlaceResult? {
        val elements = JSONObject(response).optJSONArray("elements") ?: return null
        val best = HashMap<PlaceCategory, PlaceResult>()
        for (index in 0 until elements.length()) {
            val element = elements.optJSONObject(index) ?: continue
            val tags = element.optJSONObject("tags") ?: continue
            val name = tags.optString("name").trim()
            if (name.isEmpty()) {
                continue
            }
            val category = categoryOf(tags) ?: continue
            val pointLat = if (element.has("lat")) {
                element.optDouble("lat", Double.NaN)
            } else {
                element.optJSONObject("center")?.optDouble("lat", Double.NaN) ?: Double.NaN
            }
            val pointLon = if (element.has("lon")) {
                element.optDouble("lon", Double.NaN)
            } else {
                element.optJSONObject("center")?.optDouble("lon", Double.NaN) ?: Double.NaN
            }
            if (pointLat.isNaN() || pointLon.isNaN()) {
                continue
            }
            val results = FloatArray(1)
            Location.distanceBetween(lat, lon, pointLat, pointLon, results)
            val candidate = PlaceResult(name, category, results[0].toDouble())
            val current = best[category]
            if (current == null || candidate.distanceMeters < current.distanceMeters) {
                best[category] = candidate
            }
        }
        val chosen = best[PlaceCategory.ATTRACTION]
            ?: best[PlaceCategory.LARGE]
            ?: best[PlaceCategory.TOPOGRAPHY]
        if (chosen != null) {
            Log.wtf("OverpassGeocoder", "выбрано '${chosen.name}' (${chosen.category}, ${chosen.distanceMeters}м)")
        } else {
            Log.wtf("OverpassGeocoder", "подходящих объектов нет")
        }
        return chosen
    }

    private fun categoryOf(tags: JSONObject): PlaceCategory? {
        val amenity = tags.optString("amenity")
        if (tags.has("tourism") || tags.has("historic") || amenity in ATTRACTION_AMENITIES) {
            return PlaceCategory.ATTRACTION
        }
        val building = tags.optString("building")
        if (tags.optString("railway") == "station" ||
            amenity in LARGE_AMENITIES ||
            building in LARGE_BUILDINGS ||
            tags.optString("shop") == "mall"
        ) {
            return PlaceCategory.LARGE
        }
        if (tags.has("natural") ||
            tags.optString("leisure") == "park" ||
            tags.optString("landuse") in TOPO_LANDUSE
        ) {
            return PlaceCategory.TOPOGRAPHY
        }
        return null
    }
}

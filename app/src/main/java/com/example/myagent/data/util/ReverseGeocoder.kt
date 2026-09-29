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

object ReverseGeocoder {

    private const val BASE_URL =
        "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&zoom=18&addressdetails=1&accept-language=ru"
    private const val USER_AGENT = "MyAgentApp/1.0 (Android)"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000
    private const val REQUEST_DELAY_MS = 1_000L
    private const val CACHE_SIZE = 100
    private const val NO_RESULT = ""

    private val cache = LruCache<String, String>(CACHE_SIZE)

    suspend fun getPlaceName(lat: Double, lon: Double): String? {
        val key = String.format(Locale.ROOT, "%.5f,%.5f", lat, lon)
        synchronized(cache) {
            val cached = cache[key]
            if (cached != null) {
                return if (cached == NO_RESULT) null else cached
            }
        }

        val placeName = withContext(Dispatchers.IO) {
            try {
                delay(REQUEST_DELAY_MS)
                requestPlaceName(lat, lon)
            } catch (e: Exception) {
                Log.wtf("ReverseGeocoder", "ошибка геокодирования: ${e.message}")
                null
            }
        }

        synchronized(cache) {
            cache.put(key, placeName ?: NO_RESULT)
        }
        return placeName
    }

    private fun requestPlaceName(lat: Double, lon: Double): String? {
        val connection = (URL(String.format(Locale.ROOT, BASE_URL, lat, lon)).openConnection()
            as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.wtf("ReverseGeocoder", "HTTP ${connection.responseCode}")
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parsePlaceName(JSONObject(body))
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePlaceName(json: JSONObject): String? {
        json.optJSONObject("address")?.let { address ->
            listOf("city", "town", "village", "suburb", "road").forEach { key ->
                val value = address.optString(key).trim()
                if (value.isNotEmpty()) {
                    return value
                }
            }
        }
        val displayName = json.optString("display_name").trim()
        if (displayName.isNotEmpty()) {
            return displayName.substringBefore(',').trim().ifEmpty { null }
        }
        return null
    }
}

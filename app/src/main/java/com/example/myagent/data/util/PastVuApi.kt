package com.example.myagent.data.util

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PastVuApi {

    private const val ENDPOINT = "https://api.pastvu.com/api2"
    private const val USER_AGENT = "MyAgentApp/1.0 (Android)"
    private const val TIMEOUT_MS = 20_000
    private const val TAG = "PastVu"
    private const val LOG_LIMIT = 500

    suspend fun getByBounds(z: Int, geometry: JSONObject): String = withContext(Dispatchers.IO) {
        val params = JSONObject()
            .put("z", z)
            .put("geometry", geometry)
        val body = JSONObject()
            .put("method", "photo.getByBounds")
            .put("params", params)
        post(body.toString())
    }

    suspend fun probe() = withContext(Dispatchers.IO) {
        val ring = JSONArray()
            .put(JSONArray().put(30.30).put(59.93))
            .put(JSONArray().put(30.50).put(59.93))
            .put(JSONArray().put(30.50).put(59.98))
            .put(JSONArray().put(30.30).put(59.98))
            .put(JSONArray().put(30.30).put(59.93))
        val geometry = JSONObject()
            .put("type", "Polygon")
            .put("coordinates", JSONArray().put(ring))
        val response = try {
            getByBounds(15, geometry)
        } catch (e: Exception) {
            Log.wtf(TAG, "request failed: ${e.javaClass.name}: ${e.message}")
            return@withContext
        }
        Log.wtf(TAG, "response: ${response.take(LOG_LIMIT)}")
    }

    private fun post(jsonBody: String): String {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Log.wtf(TAG, "http=$code len=${text.length}")
            text
        } finally {
            connection.disconnect()
        }
    }
}

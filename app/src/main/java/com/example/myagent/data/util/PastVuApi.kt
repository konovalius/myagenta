package com.example.myagent.data.util

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PastVuBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)

data class PastVuPhoto(
    val cid: Long,
    val file: String,
    val title: String,
    val lat: Double,
    val lon: Double,
    val year: Int,
    val year2: Int = 0
)

data class PastVuCluster(
    val lat: Double,
    val lon: Double,
    val count: Int,
    val previewFile: String?
)

data class PastVuSnapshot(
    val z: Int,
    val photos: List<PastVuPhoto>,
    val clusters: List<PastVuCluster>
)

fun formatYearLabel(year: Int, year2: Int): String? = when {
    year <= 0 -> null
    year2 > year -> "$year—$year2"
    else -> "$year"
}

fun PastVuPhoto.yearLabel(): String? = formatYearLabel(year, year2)

object PastVuApi {

    private const val ENDPOINT = "https://api.pastvu.com/api2"
    private const val USER_AGENT = "MyAgentApp/1.0 (Android)"
    private const val TIMEOUT_MS = 20_000
    private const val TAG = "PastVu"
    private const val LOG_LIMIT = 500
    private const val NEAREST_DEFAULT_LIMIT = 30

    suspend fun fetchPhotos(z: Int, bounds: PastVuBounds): PastVuSnapshot? = withContext(Dispatchers.IO) {
        if (bounds.west > bounds.east) {
            Log.wtf(TAG, "fetchPhotos: пересечение 180-го меридиана (west=${bounds.west}, east=${bounds.east})")
            return@withContext null
        }
        val startedAt = System.currentTimeMillis()
        val response = try {
            getByBounds(z, bounds.toPolygon())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.wtf(TAG, "fetchPhotos failed: ${e.javaClass.name}: ${e.message}")
            return@withContext null
        }
        val snapshot = try {
            parseSnapshot(z, response)
        } catch (e: Exception) {
            Log.wtf(TAG, "parse failed: ${e.javaClass.name}: ${e.message}")
            Log.wtf(TAG, "raw: ${response.take(LOG_LIMIT)}")
            return@withContext null
        }
        if (snapshot == null) {
            Log.wtf(TAG, "api error: ${response.take(LOG_LIMIT)}")
            return@withContext null
        }
        Log.wtf(
            TAG,
            "z=$z, photos=${snapshot.photos.size}, clusters=${snapshot.clusters.size}, ms=${System.currentTimeMillis() - startedAt}"
        )
        snapshot
    }

    suspend fun fetchNearestPhotos(
        lat: Double,
        lon: Double,
        limit: Int = NEAREST_DEFAULT_LIMIT
    ): List<PastVuPhoto>? = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val response = try {
            post(
                JSONObject()
                    .put("method", "photo.giveNearestPhotos")
                    .put(
                        "params",
                        JSONObject()
                            // в giveNearestPhotos geo = [lat, lon]
                            .put("geo", JSONArray().put(lat).put(lon))
                            .put("limit", limit)
                    )
                    .toString()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.wtf(TAG, "fetchNearestPhotos failed: ${e.javaClass.name}: ${e.message}")
            return@withContext null
        }
        val photos = try {
            parseNearestPhotos(response)
        } catch (e: Exception) {
            Log.wtf(TAG, "parse failed: ${e.javaClass.name}: ${e.message}")
            Log.wtf(TAG, "raw: ${response.take(LOG_LIMIT)}")
            return@withContext null
        }
        if (photos == null) {
            Log.wtf(TAG, "api error: ${response.take(LOG_LIMIT)}")
            return@withContext null
        }
        Log.wtf(
            TAG,
            "nearest lat=$lat, lon=$lon, photos=${photos.size}, ms=${System.currentTimeMillis() - startedAt}"
        )
        photos
    }

    private fun PastVuBounds.toPolygon(): JSONObject {
        val ring = JSONArray()
            .put(JSONArray().put(west).put(south))
            .put(JSONArray().put(east).put(south))
            .put(JSONArray().put(east).put(north))
            .put(JSONArray().put(west).put(north))
            .put(JSONArray().put(west).put(south))
        return JSONObject()
            .put("type", "Polygon")
            .put("coordinates", JSONArray().put(ring))
    }

    private fun parseSnapshot(z: Int, response: String): PastVuSnapshot? {
        val root = JSONObject(response)
        if (!root.has("result")) return null
        val result = root.getJSONObject("result")

        val photos = mutableListOf<PastVuPhoto>()
        result.optJSONArray("photos")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                photos += parsePhoto(item)
            }
        }

        val clusters = mutableListOf<PastVuCluster>()
        result.optJSONArray("clusters")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                // cluster.geo = [lat, lon]; внутри cluster.p.geo = [lon, lat]
                val geo = item.optJSONArray("geo")
                clusters += PastVuCluster(
                    lat = geo?.optDouble(0) ?: 0.0,
                    lon = geo?.optDouble(1) ?: 0.0,
                    count = item.optInt("c"),
                    previewFile = item.optJSONObject("p")?.optString("file")
                )
            }
        }

        return PastVuSnapshot(
            z = result.optInt("z", z),
            photos = photos,
            clusters = clusters
        )
    }

    private fun parsePhoto(item: JSONObject): PastVuPhoto {
        // geo = [lat, lon]
        val geo = item.optJSONArray("geo")
        return PastVuPhoto(
            cid = item.optLong("cid"),
            file = item.optString("file"),
            title = item.optString("title"),
            lat = geo?.optDouble(0) ?: 0.0,
            lon = geo?.optDouble(1) ?: 0.0,
            year = item.optInt("year"),
            year2 = item.optInt("year2")
        )
    }

    private fun parseNearestPhotos(response: String): List<PastVuPhoto>? {
        val root = JSONObject(response)
        if (!root.has("result")) return null
        val photos = mutableListOf<PastVuPhoto>()
        root.getJSONObject("result").optJSONArray("photos")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                photos += parsePhoto(item)
            }
        }
        return photos
    }

    suspend fun getByBounds(z: Int, geometry: JSONObject): String = withContext(Dispatchers.IO) {
        val params = JSONObject()
            .put("z", z)
            .put("geometry", geometry)
        val body = JSONObject()
            .put("method", "photo.getByBounds")
            .put("params", params)
        post(body.toString())
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

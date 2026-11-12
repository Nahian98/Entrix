package com.apexbytestudio.entrix.core.location

import android.content.Context
import android.content.pm.PackageManager
import com.apexbytestudio.entrix.core.model.Coordinates
import com.apexbytestudio.entrix.domain.location.LocationSearcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * [LocationSearcher] backed by the **Google Geocoding web service** — consistent, high-quality
 * results on every device (unlike the platform [android.location.Geocoder], whose backend quality
 * varies by manufacturer).
 *
 * It reuses the app's own Maps API key (read from the manifest meta-data). Because that key is
 * restricted to Android apps, each request is authorized the documented way — by sending the
 * `X-Android-Package` and `X-Android-Cert` headers — so no separate or unrestricted key is needed.
 * The cert SHA-1 is computed from the running app's signature at runtime, so it matches whether the
 * build is debug-signed or re-signed by Play App Signing.
 *
 * Results are biased to [regionBias]. Runs on [Dispatchers.IO]; uses only the platform
 * HttpURLConnection + org.json (no third-party networking dependency).
 *
 * Requires the **Geocoding API** to be enabled and added to the key's API restrictions in Google
 * Cloud; 10,000 requests/month are free.
 */
class GeocodingApiLocationSearcher(
    private val context: Context,
    private val regionBias: String = "bd",
) : LocationSearcher {

    private val apiKey: String by lazy {
        @Suppress("DEPRECATION")
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData
            ?.getString("com.google.android.geo.API_KEY")
            .orEmpty()
    }

    override suspend fun search(query: String): Result<Coordinates> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Maps API key is missing" }

            val url = URL(
                "https://maps.googleapis.com/maps/api/geocode/json" +
                    "?address=${URLEncoder.encode(query, "UTF-8")}" +
                    "&region=$regionBias" +
                    "&key=$apiKey",
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("X-Android-Package", context.packageName)
                signingSha1()?.let { setRequestProperty("X-Android-Cert", it) }
            }

            val body = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

            val json = JSONObject(body)
            when (val status = json.optString("status")) {
                "OK" -> Unit
                "ZERO_RESULTS" -> error("No match for \"$query\"")
                else -> error("Geocoding failed: $status")
            }

            val location = json.getJSONArray("results")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONObject("location")
            Coordinates(
                latitude = location.getDouble("lat"),
                longitude = location.getDouble("lng"),
            )
        }
    }

    /** SHA-1 of the app's signing certificate as uppercase hex (no colons) — matches the key restriction. */
    private fun signingSha1(): String? = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val cert = info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray() ?: return null
        MessageDigest.getInstance("SHA-1").digest(cert).joinToString("") { "%02X".format(it) }
    }.getOrNull()

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}

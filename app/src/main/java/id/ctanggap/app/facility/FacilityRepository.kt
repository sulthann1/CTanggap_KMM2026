package id.ctanggap.app.facility

import android.content.Context
import android.location.Location
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class Kind { FASKES, POLISI }

data class Facility(
    val name: String, val kind: Kind,
    val lat: Double, val lng: Double,
    val phone: String?, val distanceM: Float
)

/** Mencari fasilitas kesehatan dan kepolisian terdekat: Overpass API (OpenStreetMap), cadangan berkas lokal. */
class FacilityRepository(private val ctx: Context) {

    suspend fun nearby(lat: Double, lng: Double, radiusM: Int = 5_000): List<Facility> {
        val online = try { queryOverpass(lat, lng, radiusM) } catch (e: Exception) { emptyList() }
        val list = online.ifEmpty { loadFallback(lat, lng, radiusM) }
        return list.sortedBy { it.distanceM }
    }

    /** Pilih maks. 2 fasilitas kesehatan dan 2 kantor polisi yang punya nomor telepon. */
    fun pick(all: List<Facility>): List<Facility> =
        all.filter { !it.phone.isNullOrBlank() }.let { withPhone ->
            withPhone.filter { it.kind == Kind.FASKES }.take(2) +
                withPhone.filter { it.kind == Kind.POLISI }.take(2)
        }

    private fun queryOverpass(lat: Double, lng: Double, r: Int): List<Facility> {
        val q = """
            [out:json][timeout:10];
            (
              nwr["amenity"~"hospital|clinic|police"](around:$r,$lat,$lng);
            );
            out center tags;
        """.trimIndent()
        val conn = (URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true
            connectTimeout = 8_000; readTimeout = 12_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        OutputStreamWriter(conn.outputStream).use { it.write("data=" + URLEncoder.encode(q, "UTF-8")) }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val els = JSONObject(body).getJSONArray("elements")
        val out = mutableListOf<Facility>()
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val tags = e.optJSONObject("tags") ?: continue
            val (eLat, eLng) = if (e.has("lat")) e.getDouble("lat") to e.getDouble("lon")
            else e.optJSONObject("center")?.let { it.getDouble("lat") to it.getDouble("lon") } ?: continue
            val kind = if (tags.optString("amenity") == "police") Kind.POLISI else Kind.FASKES
            val phone = tags.optString("phone", tags.optString("contact:phone", "")).ifBlank { null }
            out += Facility(tags.optString("name", "(tanpa nama)"), kind, eLat, eLng, phone, dist(lat, lng, eLat, eLng))
        }
        return out
    }

    /** assets/facilities_fallback.json: isi manual untuk wilayah uji dan verifikasi nomornya lewat telepon. */
    private fun loadFallback(lat: Double, lng: Double, r: Int): List<Facility> = try {
        val arr = JSONArray(ctx.assets.open("facilities_fallback.json").bufferedReader().use { it.readText() })
        (0 until arr.length()).map { arr.getJSONObject(it) }.mapNotNull { o ->
            val fLat = o.getDouble("lat"); val fLng = o.getDouble("lng")
            val d = dist(lat, lng, fLat, fLng)
            if (d > r * 3) null else Facility(
                o.getString("name"), if (o.getString("kind") == "POLISI") Kind.POLISI else Kind.FASKES,
                fLat, fLng, o.optString("phone").ifBlank { null }, d
            )
        }
    } catch (e: Exception) { emptyList() }

    private fun dist(a: Double, b: Double, c: Double, d: Double): Float {
        val res = FloatArray(1); Location.distanceBetween(a, b, c, d, res); return res[0]
    }
}

package com.guesswhere.app.data

import com.guesswhere.app.domain.CommonsImage
import com.guesswhere.app.domain.Geo
import com.guesswhere.app.domain.Pack
import java.io.IOException
import kotlin.random.Random

/** Raised when the API answers with an error block instead of images. */
class MediaWikiApiError(code: String?, info: String?) :
    IOException("MediaWiki error ${code ?: "?"}: ${info ?: "unknown"}")

/**
 * Fetches candidate photographs for rounds from Wikimedia Commons.
 *
 * Strategy: over-request pages, then filter locally for what a guessing round
 * needs — geo-tagged bitmap files with a usable thumbnail. Neither the API nor
 * any server-side filter can express "random file WITH GPS", so we sample and
 * discard; typically half of landscape photos carry GPS metadata.
 */
class CommonsRepository(private val api: CommonsApi, private val local: LocalStore) {

    suspend fun fetchRounds(pack: Pack, wanted: Int, seed: Long?): List<CommonsImage> {
        require(wanted >= 1)
        val exclude = local.seenIds()
        val found = LinkedHashMap<Long, CommonsImage>()

        var attempt = 0
        var grnContinue: String? = null
        var gsrOffset: Int? = null

        while (found.size < wanted * OVERSAMPLE && attempt < MAX_ATTEMPTS) {
            attempt++
            val params = baseParams().toMutableMap()
            when (pack) {
                Pack.WORLD -> {
                    params["generator"] = "random"
                    params["grnnamespace"] = "6"
                    params["grnlimit"] = PAGE_LIMIT.toString()
                    grnContinue?.let { params["grncontinue"] = it }
                }
                else -> {
                    params["generator"] = "search"
                    params["gsrnamespace"] = "6"
                    params["gsrsearch"] = requireNotNull(pack.search)
                    params["gsrlimit"] = PAGE_LIMIT.toString()
                    gsrOffset?.let { params["gsroffset"] = it.toString() }
                }
            }
            val response = api.query(params)
            response.error?.let { throw MediaWikiApiError(it.code, it.info) }
            for (page in response.query?.pages.orEmpty()) {
                val image = page.toCommonsImage()
                if (image != null && page.pageid !in exclude && page.pageid !in found) {
                    found[page.pageid] = image
                }
            }
            grnContinue = response.continueKey?.grncontinue
            gsrOffset = response.continueKey?.gsroffset
            if (grnContinue == null && gsrOffset == null) break
        }

        val pool = found.values.toList()
        local.rememberSeen(pool.map { it.pageId })
        if (pool.isEmpty()) return emptyList()

        val ordered = if (seed != null) pool.shuffled(Random(seed)) else pool.shuffled()
        return ordered.take(wanted)
    }

    private fun baseParams(): Map<String, String> = mapOf(
        "action" to "query",
        "format" to "json",
        "formatversion" to "2",
        "prop" to "imageinfo",
        "iiprop" to "size|mediatype|url|extmetadata",
        // Viewer-quality preview; Commons serves the original when smaller.
        "iiurlwidth" to THUMB_WIDTH.toString(),
        "iiextmetadatafilter" to
            "Artist|Credit|LicenseShortName|LicenseUrl|UsageTerms|ImageDescription|" +
            "DateTimeOriginal|GPSLatitude|GPSLongitude|Categories",
    )

    private fun MwPage.toCommonsImage(): CommonsImage? {
        val info = imageinfo.firstOrNull() ?: return null
        if (!info.mediatype.equals("BITMAP", ignoreCase = true)) return null
        val fullUrl = info.url ?: return null
        // Commons appends ?utm_source=... tracking params; dropping them keeps
        // cache keys clean and avoids looking like referral traffic.
        val thumbUrl = info.thumburl?.substringBefore('?') ?: return null

        val meta = info.extmetadata.orEmpty()
        val lat = meta["GPSLatitude"]?.value?.trim()?.toDoubleOrNull() ?: return null
        val lon = meta["GPSLongitude"]?.value?.trim()?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        val geo = Geo(lat, lon)

        // Skip extreme panoramas and tiny files: terrible for guessing.
        val w = info.width
        val h = info.height
        if (w < MIN_WIDTH || h <= 0) return null
        val ratio = w.toDouble() / h
        if (ratio > MAX_ASPECT || ratio < 1.0 / MAX_ASPECT) return null

        val artist = (meta["Artist"]?.value?.takeIf { it.isNotBlank() }
            ?: meta["Credit"]?.value)
            ?.let(::stripHtml)?.take(ARTIST_MAX)?.trim()
            ?.ifBlank { null }
        val license = meta["LicenseShortName"]?.value?.trim()
            .takeUnless { it.isNullOrBlank() } ?: "See file page"
        val description = meta["ImageDescription"]?.value
            ?.let(::stripHtml)?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.take(DESCRIPTION_MAX)

        return CommonsImage(
            pageId = pageid,
            title = title.removePrefix("File:"),
            thumbUrl = thumbUrl,
            pageUrl = info.descriptionurl ?: "https://commons.wikimedia.org/",
            geo = geo,
            artist = artist ?: "Unknown photographer",
            license = license,
            description = description,
        )
    }

    /** Naive HTML-to-text: extmetadata fields carry small HTML fragments. */
    private fun stripHtml(raw: String): String =
        raw
            .replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private companion object {
        const val THUMB_WIDTH = 1600
        const val PAGE_LIMIT = 50
        const val OVERSAMPLE = 3
        const val MAX_ATTEMPTS = 8
        const val MIN_WIDTH = 500
        const val MAX_ASPECT = 3.0
        const val ARTIST_MAX = 64
        const val DESCRIPTION_MAX = 180
    }
}
package com.avonix.profitness.presentation.news

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import org.xmlpull.v1.XmlPullParser
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

// ── Data Model ───────────────────────────────────────────────────────────────

data class Article(
    val id: String,
    val title: String,
    val category: String,
    val readTime: String,
    val image: String,
    val summary: String = "",
    val content: String = "",
    val sourceUrl: String = "",
    val sourceName: String = "",
    val publishedAt: String = "",
    val publishedAtMs: Long = 0L,
    val isFeatured: Boolean = false
)

// ── RSS Feed Sources ─────────────────────────────────────────────────────────

private data class FeedSource(val category: String, val url: String, val sourceName: String)

// Reliable, up-to-date feeds. Google News RSS is always accessible and returns
// fresh content. ScienceDaily and BBC are proven stable sources.
private val RSS_FEEDS = listOf(
    // BİLİM – Science
    FeedSource("BİLİM", "https://rss.sciencedaily.com/news/health_medicine/sports_science.xml", "Science Daily"),
    FeedSource("BİLİM", "https://rss.sciencedaily.com/news/health_medicine/fitness.xml",        "Science Daily"),
    // BESLENME – Nutrition
    FeedSource("BESLENME", "https://news.google.com/rss/search?q=nutrition+diet+protein+health&hl=en-US&gl=US&ceid=US:en", "Google News"),
    FeedSource("BESLENME", "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml",            "NY Times"),
    // ANTRENMAN – Training
    FeedSource("ANTRENMAN", "https://news.google.com/rss/search?q=workout+training+gym+strength&hl=en-US&gl=US&ceid=US:en", "Google News"),
    FeedSource("ANTRENMAN", "https://news.google.com/rss/search?q=bodybuilding+muscle+hypertrophy&hl=en-US&gl=US&ceid=US:en", "Google News"),
    // SPOR – Sports
    FeedSource("SPOR", "https://feeds.bbci.co.uk/sport/rss.xml",                                 "BBC Sport"),
    FeedSource("SPOR", "https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml",                "NY Times"),
    // ZİHİN – Mind
    FeedSource("ZİHİN", "https://rss.sciencedaily.com/news/mind_brain/psychology.xml",            "Science Daily"),
    // YAŞAM – Lifestyle
    FeedSource("YAŞAM", "https://news.google.com/rss/search?q=wellness+lifestyle+healthy+living&hl=en-US&gl=US&ceid=US:en", "Google News"),
    // TOPARLANMA – Recovery
    FeedSource("TOPARLANMA", "https://news.google.com/rss/search?q=muscle+recovery+sleep+sports+medicine&hl=en-US&gl=US&ceid=US:en", "Google News"),
    // TEKNOLOJİ – Technology
    FeedSource("TEKNOLOJİ", "https://rss.sciencedaily.com/news/computers_math/wearable_technology.xml", "Science Daily"),
    FeedSource("TEKNOLOJİ", "https://news.google.com/rss/search?q=fitness+technology+wearable+smartwatch&hl=en-US&gl=US&ceid=US:en", "Google News"),
)

// ── Fallback Images per Category ─────────────────────────────────────────────

private val FALLBACK_IMAGES = mapOf(
    "BİLİM" to listOf(
        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
        "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=800",
        "https://images.unsplash.com/photo-1526506118085-60ce8714f8c5?w=800"
    ),
    "BESLENME" to listOf(
        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800",
        "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800",
        "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800"
    ),
    "ANTRENMAN" to listOf(
        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
        "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800",
        "https://images.unsplash.com/photo-1549060279-7e168fcee0c2?w=800"
    ),
    "SPOR" to listOf(
        "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800",
        "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800",
        "https://images.unsplash.com/photo-1540479859555-17af45c78602?w=800"
    ),
    "ZİHİN" to listOf(
        "https://images.unsplash.com/photo-1545389336-cf090694435e?w=800",
        "https://images.unsplash.com/photo-1516302752625-fcc3c50ae61f?w=800",
        "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800"
    ),
    "YAŞAM" to listOf(
        "https://images.unsplash.com/photo-1511275539165-cc46b1ee8960?w=800",
        "https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?w=800",
        "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800"
    ),
    "TOPARLANMA" to listOf(
        "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800",
        "https://images.unsplash.com/photo-1520334363584-7c70b7e8e2fc?w=800",
        "https://images.unsplash.com/photo-1515377905703-c4788e51af15?w=800"
    ),
    "TEKNOLOJİ" to listOf(
        "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800",
        "https://images.unsplash.com/photo-1575664210935-2b5f2e8bdf32?w=800",
        "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800"
    ),
)

// ── Demo Fallback (shown only when ALL real feeds fail) ──────────────────────

val DEMO_FALLBACK = listOf(
    Article(
        "d1", "Hypertrophy Science: Mechanical Tension & Muscle Growth",
        "BİLİM", "6 dk",
        "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=800",
        "Mechanical tension and metabolic stress form the two primary pillars of muscle mass growth.",
        "Hypertrophy — increased muscle mass — is built on mechanical tension and metabolic stress. Research shows mechanical load on muscle fibres activates the mTOR pathway, initiating protein synthesis.",
        sourceUrl = "https://examine.com/topics/muscle-hypertrophy/",
        sourceName = "Examine", isFeatured = true
    ),
    Article(
        "d2", "Creatine: The Real Performance Science",
        "BESLENME", "4 dk",
        "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800",
        "Creatine monohydrate is the most extensively researched sports supplement in the world.",
        "Creatine monohydrate is the most studied sports supplement globally. By accelerating ATP regeneration it enhances performance in short-duration, high-intensity exercise.",
        sourceUrl = "https://examine.com/supplements/creatine/",
        sourceName = "Examine", isFeatured = true
    ),
    Article(
        "d3", "Minimalist Recovery Protocols",
        "TOPARLANMA", "5 dk",
        "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800",
        "Growth hormone peaks during sleep. Insufficient sleep raises cortisol levels significantly.",
        "Parasympathetic nervous system activation is the most critical factor that accelerates recovery after training.",
        sourceUrl = "https://www.healthline.com/nutrition/10-benefits-of-exercise",
        sourceName = "Healthline"
    ),
    Article(
        "d4", "Mental Flow State & Athletic Performance",
        "ZİHİN", "8 dk",
        "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800",
        "Visualization and breathing protocols directly influence performance outcomes.",
        "Flow state unlocks optimal performance. Neuroscience research shows mental preparation can increase physical capacity by up to 15%.",
        sourceUrl = "https://www.sciencedaily.com/news/mind_brain/",
        sourceName = "Science Daily", isFeatured = true
    ),
    Article(
        "d5", "HIIT vs LISS: The Fat-Burning Battle",
        "ANTRENMAN", "5 dk",
        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
        "High-intensity interval training offers distinct advantages over prolonged steady-state cardio.",
        "Research shows HIIT's EPOC effect keeps calorie burning elevated long after the session ends.",
        sourceUrl = "https://www.healthline.com/health/fitness/hiit-vs-steady-state-cardio",
        sourceName = "Healthline", isFeatured = true
    ),
    Article(
        "d6", "The Future of Sports Technology",
        "TEKNOLOJİ", "4 dk",
        "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800",
        "Wearable technology and AI are revolutionising how athletes approach their training process.",
        "Biometric tracking technologies can now analyse heart-rate variability, sleep quality and stress levels in real time.",
        sourceUrl = "https://www.sciencedaily.com/news/computers_math/",
        sourceName = "Science Daily"
    )
)

// ── Repository ────────────────────────────────────────────────────────────────

object NewsRepository {

    suspend fun fetchAllNews(): List<Article> = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferreds = RSS_FEEDS.mapIndexed { idx, source ->
                async {
                    try { fetchFeed(source, idx * 15) } catch (_: Exception) { emptyList() }
                }
            }
            val results = deferreds.map { it.await() }.flatten()
            val unique = results.distinctBy { it.title.take(40) }
            if (unique.isEmpty()) return@coroutineScope DEMO_FALLBACK
            unique
                .sortedByDescending { it.publishedAtMs }
                .mapIndexed { i, art -> if (i < 6) art.copy(isFeatured = true) else art }
        }
    }

    private fun fetchFeed(source: FeedSource, idOffset: Int): List<Article> {
        val conn = URL(source.url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 ProfitnesApp/1.0")
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 12_000
        conn.readTimeout   = 15_000
        conn.connect()
        if (conn.responseCode !in 200..299) { conn.disconnect(); return emptyList() }
        return try {
            parseRss(conn.inputStream, source.category, source.sourceName, idOffset)
        } finally {
            conn.disconnect()
        }
    }

    // ── RSS / Atom parser ─────────────────────────────────────────────────────
    // Handles RSS 2.0, Atom 1.0 and Google News feeds.
    // Link extraction order: <link href>, <link> TEXT, <guid isPermaLink="true"> TEXT.
    // Image extraction order: media:content, media:thumbnail, enclosure, <img src> in description.

    private fun parseRss(
        stream: java.io.InputStream,
        category: String,
        sourceName: String,
        idOffset: Int
    ): List<Article> {
        val items   = mutableListOf<Article>()
        val parser  = Xml.newPullParser()
        parser.setInput(stream, null)

        var inItem   = false
        var curTag   = ""
        val title    = StringBuilder()
        val link     = StringBuilder()
        val guid     = StringBuilder()
        val desc     = StringBuilder()
        val pubDate  = StringBuilder()
        var imageUrl = ""
        var itemIdx  = 0
        var event    = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT && itemIdx < 20) {
            val rawTag   = parser.name?.lowercase() ?: ""
            val localTag = rawTag.substringAfterLast(":")

            when (event) {
                XmlPullParser.START_TAG -> {
                    curTag = rawTag
                    when {
                        localTag == "item" || localTag == "entry" -> {
                            inItem = true
                            title.clear(); link.clear(); guid.clear()
                            desc.clear(); pubDate.clear(); imageUrl = ""
                        }
                        inItem && (rawTag.contains("media:content") || rawTag.contains("media:thumbnail")) -> {
                            parser.getAttributeValue(null, "url")
                                ?.takeIf { it.isNotBlank() && imageUrl.isEmpty() }
                                ?.let { imageUrl = it }
                        }
                        inItem && localTag == "enclosure" -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            val url  = parser.getAttributeValue(null, "url")  ?: ""
                            if (type.startsWith("image") && imageUrl.isEmpty()) imageUrl = url
                        }
                        // Atom <link href="..."> — always prefer explicit href attribute
                        inItem && localTag == "link" -> {
                            parser.getAttributeValue(null, "href")
                                ?.takeIf { it.isNotBlank() && link.isEmpty() }
                                ?.let { link.append(it) }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (inItem && text.isNotBlank()) when {
                        localTag == "title" && title.isEmpty()  -> title.append(text)
                        // RSS 2.0 <link> text node
                        localTag == "link"  && link.isEmpty()   -> link.append(text)
                        // <guid isPermaLink="true"> — use as link fallback
                        localTag == "guid"  && guid.isEmpty()   -> guid.append(text)
                        (localTag == "description" || localTag == "summary" ||
                         localTag == "content" || rawTag == "content:encoded") &&
                                desc.isEmpty() -> desc.append(text)
                        (localTag == "pubdate" || localTag == "published" ||
                         localTag == "updated"  || rawTag == "dc:date") &&
                                pubDate.isEmpty() -> pubDate.append(text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    if ((localTag == "item" || localTag == "entry") && inItem) {
                        val t = title.toString().trim()
                        if (t.isNotBlank()) {
                            val rawDesc   = desc.toString()
                            val cleanDesc = stripHtml(rawDesc).trim()

                            // Image: from media tags first, then <img src="..."> in desc HTML
                            if (imageUrl.isBlank()) {
                                imageUrl = extractImageFromHtml(rawDesc)
                            }
                            val img = imageUrl.takeIf { it.isNotBlank() }
                                ?: getFallback(category, idOffset + itemIdx)

                            // Link: explicit link first, then guid (only if it looks like a URL)
                            val resolvedLink = link.toString().trim().ifBlank {
                                guid.toString().trim().takeIf { it.startsWith("http") } ?: ""
                            }

                            val wc          = cleanDesc.split("\\s+".toRegex()).size
                            val rawPubDate  = pubDate.toString().trim()

                            items.add(Article(
                                id          = "${idOffset + itemIdx}",
                                title       = t.take(140),
                                category    = category,
                                readTime    = "${maxOf(1, wc / 200)} dk",
                                image       = img,
                                summary     = cleanDesc.take(300),
                                content     = cleanDesc,
                                sourceUrl   = resolvedLink,
                                sourceName  = sourceName,
                                publishedAt = rawPubDate,
                                publishedAtMs = parseDateToMs(rawPubDate)
                            ))
                            itemIdx++
                        }
                        inItem = false; curTag = ""
                    }
                }
            }
            try { event = parser.next() } catch (_: Exception) { break }
        }
        return items
    }

    // ── Date parsing ─────────────────────────────────────────────────────────

    private fun parseDateToMs(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            when {
                dateStr.contains("T") -> {
                    val clean = dateStr.take(19).replace("T", " ")
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).also {
                        it.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(clean)?.time ?: 0L
                }
                else -> {
                    val normalized = dateStr.trim()
                        .replace("GMT", "+0000").replace("UTC", "+0000")
                    java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US)
                        .parse(normalized)?.time ?: 0L
                }
            }
        } catch (_: Exception) { 0L }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun getFallback(category: String, index: Int): String {
        val list = FALLBACK_IMAGES[category] ?: FALLBACK_IMAGES.values.first()
        return list[abs(index) % list.size]
    }

    /** Extract first <img src="..."> URL from an HTML snippet (used for description fields). */
    private fun extractImageFromHtml(html: String): String {
        if (html.isBlank()) return ""
        return Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1) ?: ""
    }

    private fun stripHtml(html: String): String = html
        .replace(Regex("<[^>]*>"), " ")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"")
        .replace(Regex("\\s{2,}"), " ").trim()

    // ── Translation (MyMemory – free, no API key required) ───────────────────

    suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank() || text.length < 8) return@withContext text
        try {
            val langPair = if (targetLang == "tr") "en|tr" else "tr|en"
            val encoded  = URLEncoder.encode(text.take(480), "UTF-8")
            val url      = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair")
            val conn     = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 8_000
            conn.readTimeout    = 10_000
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val translated = JSONObject(body)
                .getJSONObject("responseData")
                .getString("translatedText")
            if (translated.isNotBlank() && translated != "null") translated else text
        } catch (_: Exception) { text }
    }

    /** Translates article title and summary concurrently. */
    suspend fun translateArticle(article: Article, targetLang: String): Pair<String, String> =
        coroutineScope {
            val titleJob   = async { translateText(article.title,   targetLang) }
            val summaryJob = async { translateText(article.summary, targetLang) }
            Pair(titleJob.await(), summaryJob.await())
        }
}

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
    val isFeatured: Boolean = false
)

// ── RSS Feed Sources ─────────────────────────────────────────────────────────

private data class FeedSource(val category: String, val url: String, val sourceName: String)

private val RSS_FEEDS = listOf(
    FeedSource("BİLİM",       "https://feeds.sciencedaily.com/sciencedaily/health_medicine/fitness",   "Science Daily"),
    FeedSource("BİLİM",       "https://rss.sciencedaily.com/news/health_medicine/sports_science.xml",  "Science Daily"),
    FeedSource("BESLENME",    "https://www.healthline.com/rss/nutrition",                              "Healthline"),
    FeedSource("BESLENME",    "https://examine.com/feed/",                                             "Examine"),
    FeedSource("ANTRENMAN",   "https://www.self.com/feed/rss",                                         "SELF"),
    FeedSource("SPOR",        "https://feeds.bbci.co.uk/sport/rss.xml",                               "BBC Sport"),
    FeedSource("SPOR",        "https://www.menshealth.com/rss/all.xml",                               "Men's Health"),
    FeedSource("ZİHİN",       "https://rss.sciencedaily.com/news/mind_brain/psychology.xml",           "Science Daily"),
    FeedSource("YAŞAM",       "https://www.healthline.com/rss/wellness",                               "Healthline"),
    FeedSource("YAŞAM",       "https://www.medicalnewstoday.com/rss",                                  "Medical News Today"),
    FeedSource("TOPARLANMA",  "https://www.healthline.com/rss/health-news",                            "Healthline"),
    FeedSource("TEKNOLOJİ",   "https://rss.sciencedaily.com/news/computers_math/wearable_technology.xml", "Science Daily"),
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
        "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800",
        "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800"
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

val DEMO_FALLBACK = listOf(
    Article("d1", "Hipertrofi Bilimi: Mekanik Gerilim ve Kas Büyümesi",
        "BİLİM", "6 dk",
        "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=800",
        "Mekanik gerilim ve metabolik stres, kas kütlesi artışının temel iki direğini oluşturur.",
        "Hipertrofi — kas kütlesi artışı — mekanik gerilim ve metabolik stres üzerine inşa edilmiştir. Araştırmalar, kas liflerine uygulanan mekanik yükün, mTOR yolağını aktive ederek protein sentezini başlattığını göstermektedir.",
        isFeatured = true),
    Article("d2", "Kreatin: Gerçek Performans Bilimi",
        "BESLENME", "4 dk",
        "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800",
        "Kreatin monohidrat, dünya genelinde en kapsamlı araştırılan spor takviyesidir.",
        "Kreatin monohidrat, dünya genelinde en çok araştırılan spor takviyesidir. ATP yenilenmesini hızlandırarak kısa süreli yüksek yoğunluklu egzersizlerde performansı artırır.",
        isFeatured = true),
    Article("d3", "Minimalist Toparlanma Protokolleri",
        "TOPARLANMA", "5 dk",
        "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800",
        "Uyku sırasında büyüme hormonu zirvesine ulaşır. Yetersiz uyku kortizolü artırır.",
        "Parasempatik sinir sistemi aktivasyonu, antrenmandan sonra toparlanmayı hızlandıran en kritik faktördür."),
    Article("d4", "Zihinsel Akış ve Sporcu Performansı",
        "ZİHİN", "8 dk",
        "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800",
        "Vizualizasyon ve nefes protokolleri performansı doğrudan etkileyen unsurlardır.",
        "Flow state, optimal performansın kapısını açar. Nörobilim araştırmaları, zihinsel hazırlığın fiziksel kapasiteyi %15'e kadar artırabileceğini ortaya koymaktadır.",
        isFeatured = true),
    Article("d5", "HIIT vs LISS: Yağ Yakım Savaşı",
        "ANTRENMAN", "5 dk",
        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
        "Yüksek yoğunluklu interval antrenman, uzun süreli sabit tempo kardioya kıyasla avantajlar sunar.",
        "Araştırmalar, HIIT antrenmanının EPOC (egzersiz sonrası aşırı oksijen tüketimi) etkisiyle kalori yakmayı antrenman sonrasında da sürdürdüğünü göstermektedir.",
        isFeatured = true),
    Article("d6", "Spor Teknolojisinin Geleceği",
        "TEKNOLOJİ", "4 dk",
        "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800",
        "Giyilebilir teknoloji ve yapay zeka, sporcuların antrenman süreçlerini devrim niteliğinde değiştiriyor.",
        "Biyometrik izleme teknolojileri artık nabız değişkenliği, uyku kalitesi ve stres seviyelerini gerçek zamanlı analiz edebilmektedir.")
)

// ── Repository ────────────────────────────────────────────────────────────────

object NewsRepository {

    suspend fun fetchAllNews(): List<Article> = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferreds = RSS_FEEDS.mapIndexed { idx, source ->
                async {
                    try {
                        fetchFeed(source, idx * 5)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
            val results = deferreds.map { it.await() }.flatten()
            val unique = results.distinctBy { it.title.take(40) }
            if (unique.isEmpty()) return@coroutineScope DEMO_FALLBACK
            // Mark first 5 featured
            unique.mapIndexed { i, art -> if (i < 5) art.copy(isFeatured = true) else art }
        }
    }

    private fun fetchFeed(source: FeedSource, idOffset: Int): List<Article> {
        val conn = URL(source.url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 ProfitnesApp/1.0")
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
        conn.connectTimeout = 12_000
        conn.readTimeout = 15_000
        conn.connect()
        if (conn.responseCode != 200) { conn.disconnect(); return emptyList() }
        return try {
            parseRss(conn.inputStream, source.category, source.sourceName, idOffset)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRss(
        stream: java.io.InputStream,
        category: String,
        sourceName: String,
        idOffset: Int
    ): List<Article> {
        val items = mutableListOf<Article>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)

        var inItem = false
        var curTag = ""
        val title = StringBuilder()
        val link = StringBuilder()
        val desc = StringBuilder()
        val pubDate = StringBuilder()
        var imageUrl = ""
        var itemIdx = 0
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT && itemIdx < 5) {
            val rawTag = parser.name?.lowercase() ?: ""
            val localTag = rawTag.substringAfterLast(":")

            when (event) {
                XmlPullParser.START_TAG -> {
                    curTag = rawTag
                    when {
                        localTag == "item" || localTag == "entry" -> {
                            inItem = true
                            title.clear(); link.clear(); desc.clear(); pubDate.clear(); imageUrl = ""
                        }
                        inItem && (rawTag.contains("media:content") || rawTag.contains("media:thumbnail")) -> {
                            parser.getAttributeValue(null, "url")
                                ?.takeIf { it.isNotBlank() && imageUrl.isEmpty() }
                                ?.let { imageUrl = it }
                        }
                        inItem && localTag == "enclosure" -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            val url = parser.getAttributeValue(null, "url") ?: ""
                            if (type.startsWith("image") && imageUrl.isEmpty()) imageUrl = url
                        }
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
                        localTag == "link"  && link.isEmpty()   -> link.append(text)
                        (localTag == "description" || localTag == "summary" ||
                            localTag == "content" || rawTag == "content:encoded") &&
                                desc.isEmpty() -> desc.append(text)
                        (localTag == "pubdate" || localTag == "published" ||
                            localTag == "updated" || rawTag == "dc:date") &&
                                pubDate.isEmpty() -> pubDate.append(text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if ((localTag == "item" || localTag == "entry") && inItem) {
                        val t = title.toString().trim()
                        if (t.isNotBlank()) {
                            val cleanDesc = stripHtml(desc.toString()).trim()
                            val img = imageUrl.ifBlank { getFallback(category, idOffset + itemIdx) }
                            val wc = cleanDesc.split("\\s+".toRegex()).size
                            items.add(Article(
                                id = "${idOffset + itemIdx}",
                                title = t.take(140),
                                category = category,
                                readTime = "${maxOf(1, wc / 200)} dk",
                                image = img,
                                summary = cleanDesc.take(300),
                                content = cleanDesc,
                                sourceUrl = link.toString().trim(),
                                sourceName = sourceName,
                                publishedAt = pubDate.toString().trim()
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

    private fun getFallback(category: String, index: Int): String {
        val list = FALLBACK_IMAGES[category] ?: FALLBACK_IMAGES.values.first()
        return list[abs(index) % list.size]
    }

    private fun stripHtml(html: String): String = html
        .replace(Regex("<[^>]*>"), " ")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"")
        .replace(Regex("\\s{2,}"), " ").trim()

    // ── Translation (MyMemory – free, no key required) ────────────────────────

    suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank() || text.length < 8) return@withContext text
        try {
            val langPair = if (targetLang == "tr") "en|tr" else "tr|en"
            val encoded = URLEncoder.encode(text.take(480), "UTF-8")
            val url = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 8_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val translated = JSONObject(body)
                .getJSONObject("responseData")
                .getString("translatedText")
            if (translated.isNotBlank() && translated != "null") translated else text
        } catch (_: Exception) { text }
    }

    // ── Concurrent title + summary translation ────────────────────────────────

    suspend fun translateArticle(
        article: Article,
        targetLang: String
    ): Pair<String, String> = coroutineScope {
        val titleJob   = async { translateText(article.title,   targetLang) }
        val summaryJob = async { translateText(article.summary, targetLang) }
        Pair(titleJob.await(), summaryJob.await())
    }
}

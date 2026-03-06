package com.avonix.profitness.presentation.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs



// ── Data Model ───────────────────────────────────────────────────────────────

data class Article(
    val id: String,
    val title: String,
    val category: String,
    val readTime: String,
    val image: String,
    val summary: String = "",
    val content: String = "",        // plain-text version (used for translation)
    val contentHtml: String = "",    // raw HTML (used for rich rendering)
    val sourceUrl: String = "",
    val sourceName: String = "",
    val publishedAt: String = "",
    val publishedAtMs: Long = 0L,
    val isFeatured: Boolean = false
)

// ── RSS Feed Sources ─────────────────────────────────────────────────────────

private data class FeedSource(val category: String, val url: String, val sourceName: String)

// Feed list: spor, sağlık, beslenme, antrenman, yaşam, mental sağlık odaklı.
// Genel teknoloji/siyaset/ekonomi haberleri kapsam dışı.
// ScienceDaily health_medicine ve mind_brain feed'leri spor bilimiyle örtüşüyor.
private val RSS_FEEDS = listOf(

    // ── SPOR ─────────────────────────────────────────────────────────────────
    FeedSource("SPOR", "https://feeds.bbci.co.uk/sport/rss.xml",                  "BBC Sport"),
    FeedSource("SPOR", "https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml", "NY Times Sports"),

    // ── ANTRENMAN ────────────────────────────────────────────────────────────
    FeedSource("ANTRENMAN", "https://www.sciencedaily.com/rss/top/health.xml",    "Science Daily"),
    FeedSource("ANTRENMAN", "https://feeds.bbci.co.uk/news/health/rss.xml",       "BBC Health"),

    // ── BESLENME ─────────────────────────────────────────────────────────────
    FeedSource("BESLENME", "https://www.sciencedaily.com/rss/health_medicine.xml","Science Daily"),
    FeedSource("BESLENME", "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml", "NY Times Health"),
    FeedSource("BESLENME", "https://www.ntv.com.tr/saglik.rss",                   "NTV Sağlık"),

    // ── YAŞAM ────────────────────────────────────────────────────────────────
    FeedSource("YAŞAM", "https://feeds.bbci.co.uk/news/health/rss.xml",           "BBC Health"),
    FeedSource("YAŞAM", "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml","NY Times Health"),
    FeedSource("YAŞAM", "https://www.ntv.com.tr/saglik.rss",                      "NTV Sağlık"),

    // ── ZİHİN ────────────────────────────────────────────────────────────────
    FeedSource("ZİHİN", "https://www.sciencedaily.com/rss/mind_brain.xml",        "Science Daily"),

    // ── TOPARLANMA ───────────────────────────────────────────────────────────
    FeedSource("TOPARLANMA", "https://www.sciencedaily.com/rss/health_medicine.xml","Science Daily"),
    FeedSource("TOPARLANMA", "https://www.sciencedaily.com/rss/mind_brain.xml",    "Science Daily"),
)

// Konuyla ilgisiz başlıkları filtrele — tam kelime eşleşmesi kullanılır.
// "wolf" → "Wolves" gibi yanlış kesmeleri önlemek için \b word-boundary regex.
private val OFF_TOPIC_PATTERNS = listOf(
    "\\bufo\\b", "\\buzaylı\\b", "\\balien\\b", "\\bpentagon\\b",
    "\\bdrone\\b", "\\bmissile\\b",
    "\\btrump\\b", "\\bbiden\\b", "\\belection\\b", "\\bseçim\\b",
    "\\bpolitika\\b", "\\bpolitic\\b",
    "\\bbitcoin\\b", "\\bcrypto\\b", "\\bkripto\\b", "\\bborsa\\b",
    "\\bopenai\\b", "\\bchatgpt\\b", "\\banthropoc\\b",
    "\\biphone\\b", "\\bsamsung\\b", "\\bwindows\\b", "\\bmicrosoft\\b",
    "\\bearthquake\\b", "\\bdeprem\\b", "\\bflood\\b", "\\bsel\\b",
    "\\basteroid\\b", "\\bneutrino\\b", "\\bquantum\\b",
    "\\bdinosaur\\b", "\\bdinozor\\b", "\\barchaeology\\b", "\\barkeoloji\\b"
).map { it.toRegex(RegexOption.IGNORE_CASE) }

// ── Fallback Images per Category ─────────────────────────────────────────────

private val FALLBACK_IMAGES = mapOf(
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
)

// ── Demo Fallback (shown only when ALL real feeds fail) ──────────────────────
// Rich, science-backed articles presented in AI-editorial format.

val DEMO_FALLBACK = listOf(
    Article(
        id = "d1",
        title = "The Biomechanics of Hypertrophy: Why Mechanical Tension Drives Muscle Growth",
        category = "ANTRENMAN",
        readTime = "7",
        image = "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=800",
        summary = "New research confirms that mechanical tension — not metabolic fatigue — is the dominant signal for muscle protein synthesis. When muscle fibres experience sufficient load under controlled lengthening (eccentric phase), the mTOR signalling cascade activates within minutes, triggering anabolic gene expression that persists for up to 48 hours post-session.",
        content = "Skeletal muscle hypertrophy is governed by three primary mechanisms: mechanical tension, metabolic stress, and muscle damage. Of these, mechanical tension has emerged as the single most potent stimulus according to meta-analyses published in the Journal of Strength and Conditioning Research.\n\nThe mechanotransduction pathway begins at integrin receptors on the sarcolemma. Tensile force deforms these receptors, activating FAK (focal adhesion kinase) and downstream Akt/mTORC1 signalling — the master regulator of protein synthesis. Studies demonstrate that slow, controlled repetitions with a full range of motion generate greater intramuscular tension than partial-range, momentum-driven lifts.\n\nFor practical application: prioritise the eccentric phase (2–4 seconds lowering), select loads between 60–85% of 1RM, and ensure at least 10 hard sets per muscle group per week. Progressive overload — adding load or volume over time — remains the irreplaceable driver of long-term hypertrophic adaptation.",
        sourceUrl = "https://pubmed.ncbi.nlm.nih.gov/?term=mechanical+tension+hypertrophy",
        sourceName = "PubMed",
        publishedAt = "Mon, 03 Mar 2025 08:00:00 +0000",
        publishedAtMs = 1741_000_800_000L,
        isFeatured = true
    ),
    Article(
        id = "d2",
        title = "Creatine Monohydrate in 2025: What the Latest Meta-Analyses Confirm",
        category = "BESLENME",
        readTime = "5",
        image = "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800",
        summary = "A 2025 umbrella review of 47 randomised controlled trials reaffirms creatine monohydrate as the most evidence-backed ergogenic supplement available. Effects extend beyond explosive power — consistent creatine use improves working memory, reduces exercise-induced inflammation markers, and accelerates glycogen resynthesis between training sessions.",
        content = "Creatine phosphate serves as the primary phosphate donor during the first 8–10 seconds of maximal effort. By elevating intramuscular PCr stores by 20–40%, creatine supplementation directly extends the ATP-PCr energy system's output capacity before lactate accumulation begins.\n\nThe performance benefits are well-documented: 5–15% improvements in maximal strength, 1–5% improvements in short-duration endurance performance, and measurable lean mass accrual over 4–12 week loading periods. A 2024 meta-analysis in the British Journal of Sports Medicine found that the lean mass gains stem primarily from enhanced training volume capacity, not water retention alone.\n\nProtocol: 3–5 g/day of creatine monohydrate indefinitely. Loading phases (20 g/day for 5–7 days) saturate muscle stores faster but produce identical long-term outcomes. Timing is irrelevant; consistency matters. Co-ingestion with carbohydrates marginally improves cellular uptake via insulin signalling.",
        sourceUrl = "https://examine.com/supplements/creatine/",
        sourceName = "Examine",
        publishedAt = "Sat, 01 Mar 2025 10:00:00 +0000",
        publishedAtMs = 1740_826_800_000L,
        isFeatured = true
    ),
    Article(
        id = "d3",
        title = "Sleep Architecture and Athletic Recovery: The Circadian Performance Window",
        category = "TOPARLANMA",
        readTime = "6",
        image = "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800",
        summary = "Elite performance organisations now treat sleep as a primary training variable. Deep slow-wave sleep (SWS) drives 70% of daily growth hormone secretion, while REM sleep consolidates motor learning and technical skill. Athletes sleeping under 7 hours show a 1.7× higher injury risk and a 10–15% decrease in reaction time within 3 days.",
        content = "Sleep-deprived athletes operate with measurably compromised physiology: elevated cortisol, suppressed testosterone, impaired glycogen synthesis, and reduced neuromuscular coordination. These are not subjective complaints — they are measurable biomarker shifts detectable within 48 hours of sleep restriction to 6 hours or less.\n\nNASA, NFL, and NBA performance teams have embedded sleep specialists whose sole mandate is optimising player recovery windows. Key findings include: napping 20–30 minutes post-training accelerates muscle glycogen resynthesis; consistent sleep onset times (±30 min variance) significantly outperform equal total hours with irregular timing; blackout environments raise core body temperature drop required for sleep onset.\n\nPractical protocol: target 8–9 hours for athletes in high-volume training phases. Eliminate screens 45 minutes before bed (blue light suppresses melatonin by up to 50%). Keep bedroom temperature at 18–20°C. Magnesium glycinate (200–400 mg) 60 minutes before sleep has a Grade B evidence rating for improving sleep quality without dependency.",
        sourceUrl = "https://www.sleepfoundation.org/physical-activity/athletic-performance-and-sleep",
        sourceName = "Sleep Foundation",
        publishedAt = "Thu, 27 Feb 2025 12:00:00 +0000",
        publishedAtMs = 1740_657_600_000L,
        isFeatured = false
    ),
    Article(
        id = "d4",
        title = "Flow State Neuroscience: Engineering Peak Athletic Performance",
        category = "ZİHİN",
        readTime = "8",
        image = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800",
        summary = "Flow states — characterised by complete absorption, effortless action, and time distortion — are not mystical phenomena but measurable neurological events. During flow, the prefrontal cortex downregulates (transient hypofrontality), dopamine and norepinephrine surge, and default mode network activity drops sharply. Athletes in flow show up to 500% productivity increases and significantly lower perceived exertion at identical physical outputs.",
        content = "Mihaly Csikszentmihalyi's original flow research identified the challenge-skill balance as the primary precondition: a task must demand roughly 4% more than current ability to induce flow without triggering anxiety or boredom. This ratio has since been refined through neurofeedback research.\n\nModern sports neuroscience identifies four stages of the flow cycle: struggle (prefrontal cortex highly active, feels difficult), release (deliberate disengagement triggers subconscious processing), flow (transient hypofrontality, action feels automatic), and recovery (integration of new neural patterns). Elite performers learn to manage this cycle intentionally.\n\nPre-performance protocols that reliably induce flow preconditions: box breathing (4-4-4-4) to shift autonomic nervous system toward parasympathetic dominance; process-focused intention setting rather than outcome goals; progressive warm-up that mirrors competition intensity to prime motor patterns. Mental imagery at 98–100% vividness (not simply visualising but kinesthetically simulating) activates identical motor neurons as physical practice.",
        sourceUrl = "https://www.sciencedaily.com/news/mind_brain/psychology/",
        sourceName = "Science Daily",
        publishedAt = "Tue, 25 Feb 2025 09:00:00 +0000",
        publishedAtMs = 1740_473_200_000L,
        isFeatured = true
    ),
    Article(
        id = "d5",
        title = "HIIT Periodisation in 2025: Programming the Afterburn Effect",
        category = "ANTRENMAN",
        readTime = "6",
        image = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
        summary = "High-intensity interval training generates EPOC (excess post-exercise oxygen consumption) that can elevate metabolic rate for 14–38 hours post-session — a physiological response impossible to replicate with steady-state cardio alone. However, research now shows that HIIT effectiveness drops sharply after 3 consecutive sessions per week, making strategic periodisation essential for avoiding adrenal fatigue and performance plateaus.",
        content = "HIIT's metabolic superiority over LISS (low-intensity steady-state) stems from its ability to recruit fast-twitch type IIx fibres, deplete phosphocreatine stores, and cause significant mitochondrial biogenesis — all within sessions as short as 20 minutes. A landmark study in the Journal of Physiology found that 27 minutes of sprint interval training produced equivalent cardiovascular adaptations to 60 minutes of continuous moderate-intensity exercise.\n\nThe EPOC mechanism: high-intensity exercise creates oxygen debt, elevated lactate, hormonal cascade (adrenaline, growth hormone, testosterone), and elevated core temperature. Restoring homeostasis requires energy expenditure that continues hours after the session ends. The magnitude depends on intensity — only exercise above 70% VO2max generates meaningful EPOC.\n\nOptimal HIIT programming: 2–3 sessions per week maximum; 1:2 to 1:3 work-to-rest ratios for phosphocreatine-based intervals (10–15s sprints); 1:1 ratios for lactate-threshold work (30–60s intervals). Combine with 2–3 LISS sessions for aerobic base maintenance without CNS fatigue accumulation.",
        sourceUrl = "https://www.healthline.com/health/fitness/hiit-vs-steady-state-cardio",
        sourceName = "Healthline",
        publishedAt = "Sun, 23 Feb 2025 14:00:00 +0000",
        publishedAtMs = 1740_315_600_000L,
        isFeatured = true
    ),
    Article(
        id = "d6",
        title = "AI-Powered Biometrics: How Smart Wearables Are Redefining Training Load Management",
        category = "SPOR",
        readTime = "5",
        image = "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800",
        summary = "The latest generation of sports wearables integrates continuous HRV (heart rate variability), blood oxygen saturation, skin temperature, and movement velocity data into AI models that predict overtraining risk 48–72 hours before subjective symptoms appear. Professional teams report 23% reductions in soft-tissue injuries after implementing wearable-driven load management protocols.",
        content = "Heart rate variability has emerged as the most actionable real-time readiness metric. HRV reflects the balance between sympathetic and parasympathetic nervous system activity — high HRV indicates recovery and readiness; low HRV signals accumulated stress, whether from training, poor sleep, or psychological load.\n\nModern platforms such as WHOOP, Garmin Connect IQ, and Polar Vantage use 7–21 day rolling baselines to personalise readiness scores. A single absolute HRV value is meaningless; what matters is deviation from personal baseline. Research from the Finnish Institute of Sport shows that training prescribed by HRV-guided protocols resulted in 10% greater VO2max improvements over 8 weeks compared to traditional periodised plans.\n\nEmerging technologies: lactate threshold estimation via optical sensors (avoiding blood prick tests); continuous glucose monitoring integration for real-time fuel status; AI coaching systems that synthesise sleep, HRV, training load, and nutrition data to generate daily readiness recommendations. The convergence of these modalities marks the beginning of truly individualised, data-driven athletic development.",
        sourceUrl = "https://www.sciencedaily.com/news/computers_math/wearable_technology/",
        sourceName = "Science Daily",
        publishedAt = "Fri, 21 Feb 2025 11:00:00 +0000",
        publishedAtMs = 1740_132_000_000L,
        isFeatured = true
    ),
    Article(
        id = "d7",
        title = "Protein Distribution Across Meals: Why Timing Still Matters",
        category = "BESLENME",
        readTime = "4",
        image = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800",
        summary = "Consuming 0.4 g/kg of protein per meal (4–5 meals/day) maximises leucine-triggered muscle protein synthesis more effectively than front-loading the same daily total. The leucine threshold — approximately 2–3 g per serving — must be met at each meal to fully activate ribosomal protein synthesis machinery.",
        content = "The anabolic ceiling hypothesis states that muscle protein synthesis (MPS) reaches a maximal rate at approximately 20–40 g of high-quality protein per meal, with excess amino acids oxidised for energy rather than incorporated into muscle tissue. While this threshold varies with body mass and training status, the principle of distributed protein intake holds across populations.\n\nPractical meal structure for a 80 kg athlete targeting 1.8 g/kg/day (144 g total): Meal 1 — 35 g (eggs + Greek yoghurt); Meal 2 post-training — 40 g (whey + milk); Meal 3 — 35 g (chicken/fish); Meal 4 — 34 g (cottage cheese before sleep, for overnight casein slow-release). Pre-sleep casein deserves special attention: a 2024 study in the American Journal of Clinical Nutrition confirmed 40 g casein before sleep increases overnight MPS by 22%.",
        sourceUrl = "https://examine.com/nutrition/protein-timing/",
        sourceName = "Examine",
        publishedAt = "Wed, 19 Feb 2025 08:00:00 +0000",
        publishedAtMs = 1739_955_600_000L,
        isFeatured = false
    ),
    Article(
        id = "d8",
        title = "Zone 2 Training: The Aerobic Base Every Athlete Underestimates",
        category = "ANTRENMAN",
        readTime = "5",
        image = "https://images.unsplash.com/photo-1549060279-7e168fcee0c2?w=800",
        summary = "Zone 2 training — sustained effort at 60–70% of maximum heart rate — drives mitochondrial biogenesis more effectively than higher-intensity work. Elite endurance athletes spend 70–80% of training volume in this zone. For strength athletes, 3 hours of weekly Zone 2 has been shown to improve recovery between sessions by enhancing parasympathetic tone.",
        content = "Mitochondrial density is a primary determinant of oxidative capacity and long-term athletic health. Zone 2 exercise uniquely stimulates PGC-1α, the master regulator of mitochondrial biogenesis, without generating the oxidative stress and cortisol burden of higher-intensity sessions.\n\nDr. Iñigo San Millán's research with WorldTour cyclists demonstrates that lactate clearance ability — the capacity to recycle lactate as fuel — is the key differentiator between elite and sub-elite performers. This ability is built almost exclusively through Zone 2 volume.\n\nFor non-endurance athletes: 2–3 hours of Zone 2 per week (distributed across 2–3 sessions) measurably improves heart rate recovery between strength sets, reduces DOMS duration, and lowers resting heart rate over 8–12 weeks. Assessment: you should be able to hold a full conversation at Zone 2 effort. If breathing prevents speech, the intensity is too high.",
        sourceUrl = "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7739769/",
        sourceName = "NIH / PubMed",
        publishedAt = "Mon, 17 Feb 2025 10:00:00 +0000",
        publishedAtMs = 1739_783_400_000L,
        isFeatured = false
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

            val filtered = results.filter { article ->
                OFF_TOPIC_PATTERNS.none { pattern -> pattern.containsMatchIn(article.title) }
            }

            val rssArticles = filtered.distinctBy { it.title.take(40) }

            if (rssArticles.isEmpty()) return@coroutineScope DEMO_FALLBACK

            rssArticles
                .sortedByDescending { it.publishedAtMs }
                .mapIndexed { i, art -> if (i < 6) art.copy(isFeatured = true) else art }
        }
    }

    private fun fetchFeed(source: FeedSource, idOffset: Int): List<Article> {
        // Cache-busting: her istek benzersiz URL ile yapılır, hiçbir katman cache'leyemez
        val cacheBuster = System.currentTimeMillis() / 60_000  // her dakika değişir
        var url = if (source.url.contains("?")) "${source.url}&_t=$cacheBuster"
                  else "${source.url}?_t=$cacheBuster"
        for (attempt in 0..4) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
            conn.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,tr;q=0.8")
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.setRequestProperty("Expires", "0")
            conn.useCaches = false
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15_000
            conn.readTimeout    = 20_000
            val code = conn.responseCode
            when {
                code in 200..299 -> return try {
                    val encoding = conn.getHeaderField("Content-Encoding") ?: ""
                    val stream: InputStream = if (encoding.equals("gzip", ignoreCase = true))
                        GZIPInputStream(conn.inputStream)
                    else
                        conn.inputStream
                    parseRss(stream, source.category, source.sourceName, idOffset)
                } finally { conn.disconnect() }
                code in 300..399 -> {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) return emptyList()
                    url = if (location.startsWith("http")) location
                          else URL(URL(url), location).toString()
                }
                else -> { conn.disconnect(); return emptyList() }
            }
        }
        return emptyList()
    }

    // ── RSS / Atom parser (DOM-based) ────────────────────────────────────────
    // Uses DocumentBuilder so namespace handling is never an issue.
    // Supports RSS 2.0 (<item>) and Atom 1.0 (<entry>).

    private fun parseRss(
        stream: InputStream,
        category: String,
        sourceName: String,
        idOffset: Int
    ): List<Article> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            try { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) } catch (_: Exception) {}
            try { setFeature("http://xml.org/sax/features/external-general-entities", false) } catch (_: Exception) {}
        }
        val doc = factory.newDocumentBuilder().parse(stream)
        doc.documentElement.normalize()

        // Collect all <item> (RSS) and <entry> (Atom) nodes
        val nodes: NodeList = doc.getElementsByTagName("item").let {
            if (it.length > 0) it else doc.getElementsByTagName("entry")
        }

        val articles = mutableListOf<Article>()
        val max = minOf(nodes.length, 20)

        for (i in 0 until max) {
            val el = nodes.item(i) as? Element ?: continue

            val title   = el.firstText("title").trim().take(140)
            if (title.isBlank()) continue
            val pubDate = el.firstText("pubDate")
                .ifBlank { el.firstText("published") }
                .ifBlank { el.firstText("updated") }

            // Link: <link href="..."> (Atom) or <link> text (RSS) or <guid>
            val link = el.atomLinkHref()
                .ifBlank { el.firstText("link") }
                .ifBlank { el.firstText("guid").takeIf { it.startsWith("http") } ?: "" }

            // Full content: content:encoded (CDATA) > content > description > summary
            // content:encoded often contains the full article text; description is the teaser.
            val rawFull = el.cdataText("encoded")       // content:encoded — richest source
                .ifBlank { el.firstText("content") }    // Atom <content>
            val rawTeaser = el.firstText("description")
                .ifBlank { el.firstText("summary") }

            // Use whichever is longer as the definitive raw source for images
            val rawForImage = if (rawFull.length > rawTeaser.length) rawFull else rawTeaser

            val cleanFull   = stripHtml(rawFull).trim()
            val cleanTeaser = stripHtml(rawTeaser).trim()

            // summary = a concise teaser (first meaningful paragraph, up to 400 chars).
            // content = full structured plain-text (up to 8 000 chars), formatted with
            //           blank-line paragraph breaks so the reader UI renders it cleanly.
            val summaryText = buildSummary(cleanTeaser, cleanFull)
            val contentText = buildStructuredContent(cleanFull, cleanTeaser)

            // Image: media:content url > media:thumbnail url > enclosure url > <img> in html
            val imageUrl = el.mediaUrl()
                .ifBlank { el.enclosureUrl() }
                .ifBlank { extractImageFromHtml(rawForImage) }
                .ifBlank { getFallback(category, idOffset + i) }

            val wc = contentText.split("\\s+".toRegex()).size

            // Estimate read time in minutes.
            // When RSS only provides a short teaser, wc is very low (50–100 words)
            // but the real article is much longer. Use the fuller of full/teaser and
            // apply a floor of 3 min for feeds known to have truncated descriptions.
            val fullWc = maxOf(wc,
                cleanFull.split("\\s+".toRegex()).size,
                cleanTeaser.split("\\s+".toRegex()).size)
            val estimatedMin = if (fullWc < 150) {
                // Very short text — likely a truncated RSS teaser. Estimate 3–5 min
                // based on description length heuristic.
                maxOf(3, (summaryText.length / 80).coerceIn(3, 6))
            } else {
                maxOf(1, fullWc / 200)
            }

            // Stable ID: hash of title prefix + sourceName so the same article keeps
            // the same ID across fetches regardless of its position in the RSS feed.
            // This ensures clickCounts (keyed by articleKey) and trendingRanks (keyed
            // by article.id) always refer to the same underlying story.
            val stableId = "${title.take(60)}|${sourceName}".hashCode().toString()

            articles.add(Article(
                id            = stableId,
                title         = title,
                category      = category,
                readTime      = "$estimatedMin",
                image         = imageUrl,
                summary       = summaryText,
                content       = contentText,
                contentHtml   = rawFull.take(40_000).ifBlank { rawTeaser },
                sourceUrl     = link,
                sourceName    = sourceName,
                publishedAt   = pubDate,
                publishedAtMs = parseDateToMs(pubDate)
            ))
        }
        return articles
    }

    // ── DOM helper extensions ─────────────────────────────────────────────────

    /** Text content of the first matching tag (any namespace). */
    private fun Element.firstText(localName: String): String {
        val nl = getElementsByTagNameNS("*", localName)
        if (nl.length > 0) {
            val text = nl.item(0).textContent?.trim() ?: ""
            if (text.isNotBlank()) return text
        }
        // fallback: no-namespace
        val nl2 = getElementsByTagName(localName)
        return nl2.item(0)?.textContent?.trim() ?: ""
    }

    /** CDATA text — used for content:encoded */
    private fun Element.cdataText(localName: String): String {
        val nl = getElementsByTagNameNS("*", localName)
        return nl.item(0)?.textContent?.trim() ?: ""
    }

    /** Atom <link rel="alternate" href="..."> */
    private fun Element.atomLinkHref(): String {
        val links = getElementsByTagNameNS("*", "link")
        for (j in 0 until links.length) {
            val e = links.item(j) as? Element ?: continue
            val rel = e.getAttribute("rel")
            val href = e.getAttribute("href")
            if (href.isNotBlank() && (rel.isBlank() || rel == "alternate")) return href
        }
        // also try no-namespace link with href attr
        val links2 = getElementsByTagName("link")
        for (j in 0 until links2.length) {
            val e = links2.item(j) as? Element ?: continue
            val href = e.getAttribute("href")
            if (href.isNotBlank()) return href
        }
        return ""
    }

    /** media:content or media:thumbnail url attribute */
    private fun Element.mediaUrl(): String {
        for (tag in listOf("content", "thumbnail")) {
            val nl = getElementsByTagNameNS("http://search.yahoo.com/mrss/", tag)
            if (nl.length > 0) {
                val url = (nl.item(0) as? Element)?.getAttribute("url") ?: ""
                if (url.isNotBlank()) return url
            }
        }
        return ""
    }

    /** <enclosure type="image/..." url="..."> */
    private fun Element.enclosureUrl(): String {
        val nl = getElementsByTagName("enclosure")
        for (j in 0 until nl.length) {
            val e = nl.item(j) as? Element ?: continue
            val type = e.getAttribute("type")
            val url  = e.getAttribute("url")
            if (url.isNotBlank() && type.startsWith("image")) return url
        }
        return ""
    }

    // ── Date parsing ─────────────────────────────────────────────────────────

    private fun parseDateToMs(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val s = dateStr.trim()

        // 1) ISO-8601 with offset: "2026-03-04T12:18:34+03:00" veya "2026-03-04T12:18:34Z"
        if (s.contains("T")) {
            // offset'i SimpleDateFormat'ın anlayacağı şekle getir (+03:00 → +0300)
            val normalized = s.replace(Regex("([+-])(\\d{2}):(\\d{2})$")) { m ->
                "${m.groupValues[1]}${m.groupValues[2]}${m.groupValues[3]}"
            }.replace("Z", "+0000").take(24)
            for (fmt in listOf("yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss")) {
                try {
                    return java.text.SimpleDateFormat(fmt, java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(normalized)?.time ?: continue
                } catch (_: Exception) {}
            }
        }

        // 2) RFC 822: "Wed, 04 Mar 2026 22:06:35 GMT" veya "Fri, 06 Mar 2026 01:57:35 EST"
        // Timezone kısaltmalarını offset'e çevir (uzun olanlar önce)
        val normalized = s
            .replace("CEST", "+0200").replace("CET",  "+0100")
            .replace("EEST", "+0300").replace("EET",  "+0200")
            .replace("BST",  "+0100")
            .replace("EDT",  "-0400").replace("EST",  "-0500")
            .replace("CDT",  "-0500").replace("CST",  "-0600")
            .replace("MDT",  "-0600").replace("MST",  "-0700")
            .replace("PDT",  "-0700").replace("PST",  "-0800")
            .replace("GMT",  "+0000").replace("UTC",  "+0000")
            .replace("UT",   "+0000")

        for (fmt in listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z"
        )) {
            try {
                return java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                    .parse(normalized)?.time ?: continue
            } catch (_: Exception) {}
        }

        return 0L
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

    private fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        return html
            // Block-level tags → paragraph break
            .replace(Regex("</?(p|div|h[1-6]|li|blockquote|section|article|header|footer)[^>]*>",
                RegexOption.IGNORE_CASE), "\n\n")
            // Line breaks → single newline
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            // Strip remaining tags
            .replace(Regex("<[^>]*>"), "")
            // HTML entities
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"")
            .replace("&apos;", "'").replace("&hellip;", "…").replace("&mdash;", "—")
            .replace("&ndash;", "–").replace("&laquo;", "«").replace("&raquo;", "»")
            // Collapse multiple blank lines to at most one blank line
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n[ \\t]+"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    // ── Article full-content fetcher ─────────────────────────────────────────

    /**
     * Fetches the full article text from [url] by downloading the HTML page and
     * extracting the main body. Only used when the RSS feed provides no full content
     * (most English sources like BBC, NYT, ScienceDaily only put a teaser in RSS).
     *
     * Strategy: look for <article>, <main>, or the largest <div> with meaningful text.
     * Returns blank string on any failure so callers can fall back gracefully.
     */
    suspend fun fetchArticleContent(url: String): String = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext ""
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 12_000
            conn.readTimeout    = 15_000
            if (conn.responseCode !in 200..299) { conn.disconnect(); return@withContext "" }

            val encoding = conn.getHeaderField("Content-Encoding") ?: ""
            val html = (if (encoding.equals("gzip", ignoreCase = true))
                GZIPInputStream(conn.inputStream)
            else
                conn.inputStream).bufferedReader(Charsets.UTF_8).readText()
            conn.disconnect()

            extractMainContent(html)
        } catch (_: Exception) { "" }
    }

    /**
     * Extracts the main readable text from a full HTML page.
     *
     * Approach: collect all <p> tag contents after stripping known non-content
     * blocks (script, style, nav, header, footer, aside). This is robust across
     * all sites because paragraphs are universally marked with <p>.
     *
     * Additionally tries site-specific containers before the generic <p> sweep:
     *  - <div id="story_text">  → ScienceDaily
     *  - <div id="text">        → ScienceDaily inner body
     *  - <article>              → BBC, NYT and most modern news sites
     *
     * Falls back to the full <p> sweep if containers don't yield enough text.
     */
    private fun extractMainContent(html: String): String {
        // ── Step 1: strip known noise blocks ────────────────────────────────────
        // Use simple indexOf-based removal to avoid regex catastrophic backtracking
        // on large HTML pages (BBC page is ~370 KB).
        val noiseBlocks = listOf("script", "style", "nav", "footer", "aside", "form",
                                 "iframe", "noscript", "figcaption")
        var cleaned = html
        for (tag in noiseBlocks) {
            cleaned = removeHtmlBlock(cleaned, tag)
        }
        // Strip HTML comments
        cleaned = cleaned.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

        // ── Step 2: try site-specific containers first ───────────────────────────

        // ScienceDaily: <div id="story_text"> or <div id="text">
        for (divId in listOf("story_text", "text", "contents")) {
            val extracted = extractById(cleaned, divId)
            if (extracted.length > 400) return extracted
        }

        // Generic: <article> — BBC, NYT, most news sites
        val articleText = extractByTag(cleaned, "article")
        if (articleText.length > 400) return articleText

        // ── Step 3: collect all <p> tags as universal fallback ───────────────────
        return collectParagraphs(cleaned)
    }

    /**
     * Removes all occurrences of <tag ...>...</tag> using simple string scanning
     * (no regex) to avoid catastrophic backtracking on large HTML.
     */
    private fun removeHtmlBlock(html: String, tag: String): String {
        val open  = "<$tag"
        val close = "</$tag>"
        val sb = StringBuilder(html.length)
        var i = 0
        while (i < html.length) {
            val start = html.indexOf(open, i, ignoreCase = true)
            if (start == -1) { sb.append(html.substring(i)); break }
            sb.append(html.substring(i, start))
            val end = html.indexOf(close, start, ignoreCase = true)
            i = if (end == -1) html.length else end + close.length
        }
        return sb.toString()
    }

    /** Extracts the inner HTML of the first <tag> ... </tag> and strips to plain text. */
    private fun extractByTag(html: String, tag: String): String {
        val open  = "<$tag"
        val close = "</$tag>"
        val start = html.indexOf(open, ignoreCase = true).takeIf { it >= 0 } ?: return ""
        val bodyStart = html.indexOf('>', start).takeIf { it >= 0 }?.plus(1) ?: return ""
        val end = html.indexOf(close, bodyStart, ignoreCase = true).takeIf { it >= 0 } ?: return ""
        val inner = html.substring(bodyStart, end)
        return buildStructuredContent(stripHtml(inner), "")
    }

    /** Extracts the inner HTML of the first <div id="[id]"> ... </div> element. */
    private fun extractById(html: String, id: String): String {
        val marker = "id=\"$id\""
        val divStart = html.indexOf(marker, ignoreCase = true).takeIf { it >= 0 } ?: return ""
        // Walk back to find the opening < of this div
        val tagStart = html.lastIndexOf('<', divStart).takeIf { it >= 0 } ?: return ""
        val bodyStart = html.indexOf('>', tagStart).takeIf { it >= 0 }?.plus(1) ?: return ""
        // Find the matching closing </div> by counting nesting
        var depth = 1
        var i = bodyStart
        while (i < html.length && depth > 0) {
            val nextOpen  = html.indexOf("<div", i, ignoreCase = true)
            val nextClose = html.indexOf("</div>", i, ignoreCase = true)
            when {
                nextOpen != -1 && (nextClose == -1 || nextOpen < nextClose) -> {
                    depth++; i = nextOpen + 4
                }
                nextClose != -1 -> {
                    depth--; i = nextClose + 6
                    if (depth == 0) {
                        val inner = html.substring(bodyStart, nextClose)
                        return buildStructuredContent(stripHtml(inner), "")
                    }
                }
                else -> break
            }
        }
        return ""
    }

    /**
     * Collects all <p>...</p> text blocks, filters out very short ones
     * (likely nav labels, captions), and joins into structured paragraphs.
     * Universal fallback — works on any news site.
     */
    private fun collectParagraphs(html: String): String {
        val paragraphs = mutableListOf<String>()
        val pOpen  = "<p"
        val pClose = "</p>"
        var i = 0
        while (i < html.length) {
            val start = html.indexOf(pOpen, i, ignoreCase = true).takeIf { it >= 0 } ?: break
            val bodyStart = html.indexOf('>', start).takeIf { it >= 0 }?.plus(1) ?: break
            val end = html.indexOf(pClose, bodyStart, ignoreCase = true).takeIf { it >= 0 } ?: break
            val inner = html.substring(bodyStart, end)
            val text = stripHtml(inner).replace("\n", " ").replace(Regex("\\s{2,}"), " ").trim()
            // Only keep paragraphs that look like real prose (>= 60 chars)
            if (text.length >= 60) paragraphs.add(text)
            i = end + pClose.length
        }
        if (paragraphs.isEmpty()) return ""
        return paragraphs
            .distinct()
            .take(20)
            .joinToString("\n\n")
            .take(8_000)
    }

    // ── Content structuring ───────────────────────────────────────────────────

    /**
     * Builds a concise summary from the RSS teaser and/or full text.
     * Takes the first meaningful sentence(s) up to ~400 characters so the
     * summary card in the reader shows a proper intro, not the full body.
     */
    private fun buildSummary(cleanTeaser: String, cleanFull: String): String {
        val source = cleanTeaser.ifBlank { cleanFull }
        if (source.isBlank()) return ""
        // Take the first paragraph (split by double newline)
        val firstPara = source.split("\n\n").firstOrNull { it.trim().length > 40 }?.trim()
            ?: source.trim()
        return if (firstPara.length <= 420) firstPara
        else {
            // Trim at last sentence boundary within 420 chars
            val truncated = firstPara.take(420)
            val lastDot = truncated.lastIndexOfAny(charArrayOf('.', '!', '?'))
            if (lastDot > 80) truncated.substring(0, lastDot + 1) else truncated + "…"
        }
    }

    /**
     * Builds a clean, structured plain-text body from the full article content.
     * Paragraphs are separated by blank lines. Each paragraph is trimmed and
     * run-on whitespace is collapsed. Empty or duplicate paragraphs are removed.
     * The result is suitable both for direct display and for translation chunking.
     */
    private fun buildStructuredContent(cleanFull: String, cleanTeaser: String): String {
        val source = if (cleanFull.length > cleanTeaser.length + 60) cleanFull else cleanTeaser
        if (source.isBlank()) return ""

        val paragraphs = source
            .split("\n\n")
            .map { it.replace("\n", " ").replace(Regex("\\s{2,}"), " ").trim() }
            .filter { it.length > 30 }          // drop noise / image captions
            .distinct()                           // drop exact duplicates
            .take(20)                             // cap at 20 paragraphs

        return paragraphs.joinToString("\n\n").take(8_000)
    }

    // ── Translation ───────────────────────────────────────────────────────────
    // Primary  : Google Translate (unofficial endpoint — no API key, high quota)
    // Fallback : MyMemory (free, no API key)
    // If both fail the original text is returned so the UI never breaks.

    /**
     * Translates [text] to [targetLang] ("tr" or "en").
     * Tries Google Translate first; falls back to MyMemory on any error.
     * Chunk limit: 4 800 chars per call (Google's practical safe limit).
     */
    suspend fun translateText(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank() || text.length < 4) return@withContext text
        translateWithGoogle(text, targetLang)
            .takeIf { it.isNotBlank() && it != text }
            ?: translateWithMyMemory(text, targetLang)
                .takeIf { it.isNotBlank() && it != text }
            ?: text
    }

    /** Google Translate unofficial endpoint — returns blank string on failure. */
    private fun translateWithGoogle(text: String, targetLang: String): String {
        return try {
            // sl=auto: Google detects the source language automatically.
            // This correctly handles both English→Turkish and Turkish→English
            // without hard-coding assumptions about the source language.
            val encoded = URLEncoder.encode(text.take(4_800), "UTF-8")
            val urlStr  = "https://translate.googleapis.com/translate_a/single" +
                          "?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout    = 15_000
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            conn.disconnect()
            // Response format: [[["translated","original",null,null,1],...],null,"en",...]
            // Parse the nested JSON array manually to avoid a full JSON library dependency.
            parseGoogleTranslateResponse(body)
        } catch (_: Exception) { "" }
    }

    /**
     * Parses the Google Translate JSON array response.
     * Format: [ [ ["chunk_translated", "chunk_original", ...], ... ], null, "srcLang", ... ]
     */
    private fun parseGoogleTranslateResponse(body: String): String {
        return try {
            val outer = org.json.JSONArray(body)
            val segments = outer.getJSONArray(0)
            val sb = StringBuilder()
            for (i in 0 until segments.length()) {
                val segment = segments.optJSONArray(i) ?: continue
                val part = segment.optString(0, "")
                if (part.isNotBlank()) sb.append(part)
            }
            sb.toString().trim()
        } catch (_: Exception) { "" }
    }

    /** MyMemory fallback — returns blank string on failure. */
    private fun translateWithMyMemory(text: String, targetLang: String): String {
        return try {
            // Detect source language by a quick heuristic: if majority of alpha chars
            // are ASCII, treat as English; otherwise treat as Turkish.
            val sourceLang = detectLang(text)
            if (sourceLang == targetLang) return text   // already in target language
            val langPair = "$sourceLang|$targetLang"
            val encoded  = URLEncoder.encode(text.take(480), "UTF-8")
            val url      = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair")
            val conn     = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 10_000
            conn.readTimeout    = 12_000
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val translated = JSONObject(body)
                .getJSONObject("responseData")
                .getString("translatedText")
            if (translated.isNotBlank() && translated != "null") translated else ""
        } catch (_: Exception) { "" }
    }

    /**
     * Splits [text] into word-boundary chunks of at most [CHUNK_SIZE] chars and
     * translates each via [translateText], then rejoins. Up to [MAX_CHUNKS] chunks
     * so very long articles don't hammer the API excessively.
     */
    private const val CHUNK_SIZE  = 4_800
    private const val MAX_CHUNKS  = 6   // ≈ 28 800 chars — covers full articles

    suspend fun translateLongText(text: String, targetLang: String): String {
        if (text.isBlank()) return text
        if (text.length <= CHUNK_SIZE) return translateText(text, targetLang)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length && chunks.size < MAX_CHUNKS) {
            val end = minOf(start + CHUNK_SIZE, text.length)
            val chunk = if (end >= text.length) {
                text.substring(start)
            } else {
                val lastSpace = text.lastIndexOf(' ', end)
                if (lastSpace > start) text.substring(start, lastSpace)
                else text.substring(start, end)
            }
            chunks.add(chunk.trim())
            start += chunk.length + 1
        }
        return chunks.map { translateText(it, targetLang) }.joinToString(" ")
    }

    /**
     * Heuristic language detector: checks for Turkish-specific characters.
     * Returns "tr" if Turkish chars (ğ, ş, ı, ö, ü, ç, İ, Ğ, Ş, Ö, Ü, Ç) are present
     * in a meaningful ratio, otherwise returns "en".
     */
    private fun detectLang(text: String): String {
        val sample = text.take(300)
        val turkishChars = setOf('ğ','Ğ','ş','Ş','ı','İ','ö','Ö','ü','Ü','ç','Ç')
        val turkishCount = sample.count { it in turkishChars }
        val alphaCount   = sample.count { it.isLetter() }
        return if (alphaCount > 0 && turkishCount.toDouble() / alphaCount > 0.01) "tr" else "en"
    }

    /**
     * Detects the language of an article by sampling its title and summary.
     * Public — used by the ViewModel to decide whether translation is needed.
     * Returns "tr" or "en".
     */
    fun detectArticleLanguage(article: Article): String =
        detectLang(article.title + " " + article.summary.take(200))

    /** Translates article title and summary concurrently. */
    suspend fun translateArticle(article: Article, targetLang: String): Pair<String, String> =
        coroutineScope {
            val titleJob   = async { translateText(article.title,          targetLang) }
            val summaryJob = async { translateLongText(article.summary,    targetLang) }
            Pair(titleJob.await(), summaryJob.await())
        }

    /**
     * Translates title, summary and full body content concurrently.
     */
    suspend fun translateArticleFull(article: Article, targetLang: String): Triple<String, String, String> =
        coroutineScope {
            val titleJob   = async { translateText(article.title,          targetLang) }
            val summaryJob = async { translateLongText(article.summary,    targetLang) }
            val contentJob = async { translateLongText(article.content,    targetLang) }
            Triple(titleJob.await(), summaryJob.await(), contentJob.await())
        }
}

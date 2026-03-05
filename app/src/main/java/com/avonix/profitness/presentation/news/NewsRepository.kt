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

// Reliable, up-to-date feeds. ScienceDaily, BBC, NY Times and Healthline
// are proven stable sources with consistent XML schemas that do not require auth or redirects.
// Google News RSS feeds have been replaced with more reliable alternatives.
private val RSS_FEEDS = listOf(
    // BİLİM – Science
    FeedSource("BİLİM", "https://rss.sciencedaily.com/news/health_medicine/sports_science.xml",  "Science Daily"),
    FeedSource("BİLİM", "https://rss.sciencedaily.com/news/health_medicine/fitness.xml",         "Science Daily"),
    FeedSource("BİLİM", "https://rss.sciencedaily.com/news/health_medicine/sports_medicine.xml", "Science Daily"),
    // BESLENME – Nutrition
    FeedSource("BESLENME", "https://rss.sciencedaily.com/news/health_medicine/nutrition.xml",          "Science Daily"),
    FeedSource("BESLENME", "https://rss.sciencedaily.com/news/health_medicine/diet_and_weight_loss.xml","Science Daily"),
    FeedSource("BESLENME", "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml",                  "NY Times"),
    // ANTRENMAN – Training
    FeedSource("ANTRENMAN", "https://rss.sciencedaily.com/news/health_medicine/fitness.xml",          "Science Daily"),
    FeedSource("ANTRENMAN", "https://rss.sciencedaily.com/news/health_medicine/sports_medicine.xml",  "Science Daily"),
    FeedSource("ANTRENMAN", "https://www.menshealth.com/rss/all.xml",                                 "Men's Health"),
    // SPOR – Sports
    FeedSource("SPOR", "https://feeds.bbci.co.uk/sport/rss.xml",                   "BBC Sport"),
    FeedSource("SPOR", "https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml",  "NY Times"),
    FeedSource("SPOR", "https://www.runnersworld.com/rss/all.xml",                 "Runner's World"),
    // ZİHİN – Mind
    FeedSource("ZİHİN", "https://rss.sciencedaily.com/news/mind_brain/psychology.xml",           "Science Daily"),
    FeedSource("ZİHİN", "https://rss.sciencedaily.com/news/mind_brain/sport_psychology.xml",     "Science Daily"),
    // YAŞAM – Lifestyle
    FeedSource("YAŞAM", "https://rss.sciencedaily.com/news/health_medicine/healthy_aging.xml",   "Science Daily"),
    FeedSource("YAŞAM", "https://rss.nytimes.com/services/xml/rss/nyt/Well.xml",                 "NY Times Well"),
    FeedSource("YAŞAM", "https://www.womenshealthmag.com/rss/all.xml",                           "Women's Health"),
    // TOPARLANMA – Recovery
    FeedSource("TOPARLANMA", "https://rss.sciencedaily.com/news/health_medicine/sleep.xml",      "Science Daily"),
    FeedSource("TOPARLANMA", "https://rss.sciencedaily.com/news/health_medicine/pain.xml",       "Science Daily"),
    // TEKNOLOJİ – Technology
    FeedSource("TEKNOLOJİ", "https://rss.sciencedaily.com/news/computers_math/wearable_technology.xml", "Science Daily"),
    FeedSource("TEKNOLOJİ", "https://rss.sciencedaily.com/news/computers_math/artificial_intelligence.xml", "Science Daily"),
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
// Rich, science-backed articles presented in AI-editorial format.

val DEMO_FALLBACK = listOf(
    Article(
        id = "d1",
        title = "The Biomechanics of Hypertrophy: Why Mechanical Tension Drives Muscle Growth",
        category = "BİLİM",
        readTime = "7 dk",
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
        readTime = "5 dk",
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
        readTime = "6 dk",
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
        readTime = "8 dk",
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
        readTime = "6 dk",
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
        category = "TEKNOLOJİ",
        readTime = "5 dk",
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
        readTime = "4 dk",
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
        readTime = "5 dk",
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
            val unique = results.distinctBy { it.title.take(40) }
            if (unique.isEmpty()) return@coroutineScope DEMO_FALLBACK
            unique
                .sortedByDescending { it.publishedAtMs }
                .mapIndexed { i, art -> if (i < 6) art.copy(isFeatured = true) else art }
        }
    }

    private fun fetchFeed(source: FeedSource, idOffset: Int): List<Article> {
        var url = source.url
        // Follow up to 5 redirects manually to handle HTTP→HTTPS cross-protocol redirects
        for (attempt in 0..4) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Safari/537.36")
            conn.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            conn.setRequestProperty("Cache-Control", "no-cache")
            conn.setRequestProperty("Pragma", "no-cache")
            conn.instanceFollowRedirects = false   // handle manually for cross-protocol support
            conn.connectTimeout = 12_000
            conn.readTimeout    = 15_000
            conn.connect()
            val code = conn.responseCode
            when {
                code in 200..299 -> return try {
                    parseRss(conn.inputStream, source.category, source.sourceName, idOffset)
                } finally { conn.disconnect() }
                code in 300..399 -> {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) return emptyList()
                    url = location
                }
                else -> { conn.disconnect(); return emptyList() }
            }
        }
        return emptyList()
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
                                summary     = cleanDesc.take(600),
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

    /**
     * Splits text into chunks of at most 480 chars (breaking at word boundaries) and
     * translates each chunk sequentially, then joins them. Max 4 chunks (~1920 chars)
     * to avoid excessive API calls while covering most article bodies.
     */
    suspend fun translateLongText(text: String, targetLang: String): String {
        if (text.isBlank()) return text
        if (text.length <= 480) return translateText(text, targetLang)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length && chunks.size < 4) {
            val end = minOf(start + 480, text.length)
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

    /** Translates article title and summary concurrently. */
    suspend fun translateArticle(article: Article, targetLang: String): Pair<String, String> =
        coroutineScope {
            val titleJob   = async { translateText(article.title,   targetLang) }
            val summaryJob = async { translateLongText(article.summary, targetLang) }
            Pair(titleJob.await(), summaryJob.await())
        }

    /**
     * Translates title, summary and body content concurrently.
     * Content is chunked into up to 4×480-char segments for full coverage.
     */
    suspend fun translateArticleFull(article: Article, targetLang: String): Triple<String, String, String> =
        coroutineScope {
            val titleJob   = async { translateText(article.title,          targetLang) }
            val summaryJob = async { translateLongText(article.summary,    targetLang) }
            val contentJob = async { translateLongText(article.content,    targetLang) }
            Triple(titleJob.await(), summaryJob.await(), contentJob.await())
        }
}

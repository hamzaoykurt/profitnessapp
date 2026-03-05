package com.avonix.profitness.core.theme

/** All user-visible UI strings, available in Turkish and English. */
data class AppStrings(

    // ── Profile – metrics section ────────────────────────────────────────────
    val performanceMetrics  : String,
    val seeAll              : String,

    // ── Profile – weekly activity ────────────────────────────────────────────
    val weeklyActivity      : String,
    val thisWeekSummary     : String,
    val dayAbbreviations    : List<String>,
    val unitDays            : String,
    val unitStreak          : String,

    // ── Profile – metric card labels ─────────────────────────────────────────
    val fatRatioLabel       : String,
    val muscleMassLabel     : String,
    val activeDaysLabel     : String,
    val dailyStreakLabel     : String,
    val bmiLabel            : String,
    val weeklyCaloriesLabel : String,

    // ── Profile – trophy subtitles ───────────────────────────────────────────
    val trophyFirstWin      : String,
    val trophySevenDay      : String,
    val trophyFiftyWorkouts : String,
    val trophySuperMember   : String,
    val trophyExcellence    : String,
    val trophyEliteLabel    : String,

    // ── Profile – trophies section header ────────────────────────────────────
    val achievements        : String,

    // ── Settings section ─────────────────────────────────────────────────────
    val accountSettings     : String,
    val editProfileHint     : String,
    val notificationsLabel  : String,
    val notificationsActive : String,
    val notificationsOff    : String,
    val languageLabel       : String,
    val currentLanguageName : String,
    val securityLabel       : String,
    val securityValue       : String,
    val logoutLabel         : String,

    // ── Appearance sheet ─────────────────────────────────────────────────────
    val appearanceTitle     : String,
    val modeLabel           : String,
    val darkLabel           : String,
    val lightLabel          : String,
    val accentColorLabel    : String,
    val applyLabel          : String,

    // ── Notifications sheet ──────────────────────────────────────────────────
    val notifSheetTitle     : String,
    val workoutReminders    : String,
    val progressUpdates     : String,
    val newsAlerts          : String,

    // ── Language sheet ───────────────────────────────────────────────────────
    val langSheetTitle      : String,
    val turkishLabel        : String,
    val englishLabel        : String,

    // ── Workout screen ───────────────────────────────────────────────────────
    val todayProgram        : String,   // "BUGÜNKÜ PROGRAM" / "TODAY'S PROGRAM"
    val completedLabel      : String,   // "tamamlandı" / "completed"
    val helloAthlete        : String,   // "Merhaba, Atlet" / "Hello, Athlete"
    val unitMin             : String,   // "dk" / "min"
    val unitDone            : String,   // "bitti" / "done"  (progress ring)
    val streakTitle         : String,   // "%d Günlük Seri!" / "%d-Day Streak!"  (%d replaced at runtime)
    val streakStart         : String,   // "Seri başlat!" / "Start a Streak!"
    val streakMotivate      : String,   // "Devam et, yavaşlama." / "Keep going, don't slow down."
    val streakBegin         : String,   // "Bugün bir hareket yap…" / "Move today to start your streak."
    val restDayTitle        : String,   // "DİNLENME" / "RECOVERY"
    val restDaySubtitle     : String,   // "Kasların büyüyor. Bugün dinlen." / "Muscles growing. Rest today."
    val recoveryTip1        : String,
    val recoveryTip2        : String,
    val recoveryTip3        : String,

    // ── AI Coach screen ──────────────────────────────────────────────────────
    val oracleWelcome       : String,
    val chipNutrition       : String,
    val chipMotivation      : String,
    val chipProgram         : String,
    val chipRecovery        : String,
    val chipHiitVsLiss      : String,

    // ── News screen – categories ─────────────────────────────────────────────
    val newsCatAll          : String,   // "TÜMÜ" / "ALL"
    val newsCatSaved        : String,   // "KAYDEDILENLER" / "SAVED"
    val newsCatScience      : String,   // "BİLİM" / "SCIENCE"
    val newsCatNutrition    : String,   // "BESLENME" / "NUTRITION"
    val newsCatTraining     : String,   // "ANTRENMAN" / "TRAINING"
    val newsCatSports       : String,   // "SPOR" / "SPORTS"
    val newsCatMind         : String,   // "ZİHİN" / "MIND"
    val newsCatLifestyle    : String,   // "YAŞAM" / "LIFESTYLE"
    val newsCatRecovery     : String,   // "TOPARLANMA" / "RECOVERY"
    val newsCatTech         : String,   // "TEKNOLOJİ" / "TECHNOLOGY"

    // ── News screen – UI labels ──────────────────────────────────────────────
    val liveLabel           : String,   // "CANLI" / "LIVE"
    val newsCountLabel      : String,   // "HABER" / "NEWS"
    val allNewsLabel        : String,   // "TÜM HABERLER" / "ALL NEWS"
    val noSavedNews         : String,
    val noCategoryNews      : String,
    val readingLabel        : String,   // "OKUMA" / "READ"
    val aiSummaryLabel      : String,   // "AI ÖZETİ" / "AI SUMMARY"
    val contentLabel        : String,   // "İÇERİK" / "CONTENT"
    val goToOriginal        : String,   // "HABERİN ORİJİNALİNE GİT" / "READ ORIGINAL ARTICLE"
    val translatingLabel    : String,   // "Türkçeye çevriliyor…" / "Translating…"
    val translatedLabel     : String,   // "Yapay zeka ile çevrildi" / "AI Translated"
    val sourceLabel         : String,   // "Kaynak" / "Source"
    val noSummaryLabel      : String,   // "Özet mevcut değil." / "No summary available."
    val saveArticle         : String,   // "Kaydet" / "Save"
    val unsaveArticle       : String,   // "Kaydı kaldır" / "Remove"

    // ── Program Builder screen ───────────────────────────────────────────────
    val programStudioSub    : String,   // "Hazır programlardan seç veya kendin tasarla."
    val createWithAI        : String,   // "AI ile Oluştur" / "Create with AI"
    val createManually      : String,   // "Manuel Oluştur" / "Create Manually"
    val activeProtocols     : String,   // "AKTİF PROTOKOLLER" / "ACTIVE PROTOCOLS"
    val readyPrograms       : String,   // "HAZIR PROGRAMLAR" / "READY PROGRAMS"
    val programSavedMsg     : String,   // "kaydedildi ✓" / "saved ✓"
    val programSavedDefault : String,   // "Program kaydedildi ✓" / "Program saved ✓"
    val applyProtocol       : String,   // "PROTOKOLÜ UYGULA" / "APPLY PROTOCOL"
    val saveProtocol        : String,   // "PROTOKOLÜ KAYDET" / "SAVE PROTOCOL"
    val activeLabel         : String,   // "AKTİF" / "ACTIVE"
    val analysisResult      : String,   // "ANALİZ SONUCU" / "ANALYSIS RESULT"
    val dayLabel            : String,   // "GÜN" / "DAY"
    val weekLabel           : String,   // "HAFTA" / "WEEK"
    val levelLabel          : String,   // "SEVİYE" / "LEVEL"
    val muscleDistLabel     : String,   // "KAS YÜKÜ DAĞILIMI" / "MUSCLE LOAD DISTRIBUTION"
    val scheduleLabel       : String,   // "ANTRENMAN PROGRAMI" / "TRAINING SCHEDULE"
    val aiProtocolSub       : String,   // "PROTOKOL ÜRETİMİ" / "PROTOCOL GENERATION"
    val manualBuilderSub    : String,   // "PROTOKOL TASARIMI" / "PROTOCOL DESIGN"

    // ── Program category chip labels ─────────────────────────────────────────
    val progCatAll          : String,   // "TÜMÜ" / "ALL"
    val progCatMuscle       : String,   // "KAS" / "MUSCLE"
    val progCatFatLoss      : String,   // "YAĞ YAKIMI" / "FAT LOSS"
    val progCatStrength     : String,   // "GÜÇ" / "STRENGTH"
    val progCatEndurance    : String,   // "DAYANIKLILIK" / "ENDURANCE"
    val progCatBeginner     : String,   // "BAŞLANGIÇ" / "BEGINNER"
)

// ── Turkish ───────────────────────────────────────────────────────────────────

val TurkishStrings = AppStrings(
    performanceMetrics  = "PERFORMANS ÖLÇÜTLERİ",
    seeAll              = "Tümünü Gör",
    weeklyActivity      = "HAFTALIK AKTİVİTE",
    thisWeekSummary     = "Bu hafta 5 antrenman",
    dayAbbreviations    = listOf("Pzt", "Sal", "Çrş", "Per", "Cum", "Cmt", "Paz"),
    unitDays            = "gün",
    unitStreak          = "seri",
    fatRatioLabel       = "YAĞ ORANI",
    muscleMassLabel     = "KAS KÜTLESİ",
    activeDaysLabel     = "AKTİF GÜN",
    dailyStreakLabel     = "GÜNLÜK SERİ",
    bmiLabel            = "VÜCUT KİTLE İND.",
    weeklyCaloriesLabel = "HAFTALIK KALORİ",
    trophyFirstWin      = "İlk Zafer",
    trophySevenDay      = "7 Günlük",
    trophyFiftyWorkouts = "50 Antrenman",
    trophySuperMember   = "Süper Üye",
    trophyExcellence    = "Mükemmellik",
    trophyEliteLabel    = "ELİTE",
    achievements        = "BAŞARILAR",
    accountSettings     = "HESAP VE AYARLAR",
    editProfileHint     = "Profili düzenlemek için dokun",
    notificationsLabel  = "Bildirimler",
    notificationsActive = "Aktif",
    notificationsOff    = "Kapalı",
    languageLabel       = "Dil",
    currentLanguageName = "Türkçe",
    securityLabel       = "Güvenlik",
    securityValue       = "Yüksek",
    logoutLabel         = "Çıkış Yap",
    appearanceTitle     = "GÖRÜNÜM AYARLARI",
    modeLabel           = "MOD",
    darkLabel           = "KARANLIK",
    lightLabel          = "AYDINLIK",
    accentColorLabel    = "VURGU RENGİ",
    applyLabel          = "UYGULA",
    notifSheetTitle     = "BİLDİRİM AYARLARI",
    workoutReminders    = "Antrenman Hatırlatmaları",
    progressUpdates     = "İlerleme Güncellemeleri",
    newsAlerts          = "Haber Bildirimleri",
    langSheetTitle      = "DİL SEÇİMİ",
    turkishLabel        = "Türkçe",
    englishLabel        = "English",

    // Workout
    todayProgram        = "BUGÜNKÜ PROGRAM",
    completedLabel      = "tamamlandı",
    helloAthlete        = "Merhaba, Atlet 👋",
    unitMin             = "dk",
    unitDone            = "bitti",
    streakTitle         = "%d Günlük Seri!",
    streakStart         = "Seri başlat!",
    streakMotivate      = "Devam et, yavaşlama.",
    streakBegin         = "Bugün bir hareket yap, serin başlasın.",
    restDayTitle        = "DİNLENME",
    restDaySubtitle     = "Kasların büyüyor. Bugün dinlen.",
    recoveryTip1        = "💧 Bol su için",
    recoveryTip2        = "😴 8 saat uyu",
    recoveryTip3        = "🥗 Protein al",

    // AI Coach
    oracleWelcome       = "Oracle Sanctuary'ye Hoş Geldiniz. Performans hedeflerini analiz etmeye hazırım.",
    chipNutrition       = "Beslenme Tavsiyesi",
    chipMotivation      = "Motivasyon",
    chipProgram         = "Program Önerisi",
    chipRecovery        = "Toparlanma",
    chipHiitVsLiss      = "HIIT vs LISS",

    // News – categories
    newsCatAll          = "TÜMÜ",
    newsCatSaved        = "KAYDEDILENLER",
    newsCatScience      = "BİLİM",
    newsCatNutrition    = "BESLENME",
    newsCatTraining     = "ANTRENMAN",
    newsCatSports       = "SPOR",
    newsCatMind         = "ZİHİN",
    newsCatLifestyle    = "YAŞAM",
    newsCatRecovery     = "TOPARLANMA",
    newsCatTech         = "TEKNOLOJİ",

    // News – UI labels
    liveLabel           = "CANLI",
    newsCountLabel      = "HABER",
    allNewsLabel        = "TÜM HABERLER",
    noSavedNews         = "Henüz kaydedilen haber yok",
    noCategoryNews      = "Bu kategoride haber bulunamadı",
    readingLabel        = "OKUMA",
    aiSummaryLabel      = "AI ÖZETİ",
    contentLabel        = "İÇERİK",
    goToOriginal        = "HABERİN ORİJİNALİNE GİT",
    translatingLabel    = "Türkçeye çevriliyor…",
    translatedLabel     = "Yapay zeka ile Türkçeye çevrildi",
    sourceLabel         = "Kaynak",
    noSummaryLabel      = "Bu haber için özet mevcut değil.",
    saveArticle         = "Kaydet",
    unsaveArticle       = "Kaydı kaldır",

    // Program Builder
    programStudioSub    = "Hazır programlardan seç veya kendin tasarla.",
    createWithAI        = "AI ile Oluştur",
    createManually      = "Manuel Oluştur",
    activeProtocols     = "AKTİF PROTOKOLLER",
    readyPrograms       = "HAZIR PROGRAMLAR",
    programSavedMsg     = "kaydedildi ✓",
    programSavedDefault = "Program kaydedildi ✓",
    applyProtocol       = "PROTOKOLÜ UYGULA",
    saveProtocol        = "PROTOKOLÜ KAYDET",
    activeLabel         = "AKTİF",
    analysisResult      = "ANALİZ SONUCU",
    dayLabel            = "GÜN",
    weekLabel           = "HAFTA",
    levelLabel          = "SEVİYE",
    muscleDistLabel     = "KAS YÜKÜ DAĞILIMI",
    scheduleLabel       = "ANTRENMAN PROGRAMI",
    aiProtocolSub       = "PROTOKOL ÜRETİMİ",
    manualBuilderSub    = "PROTOKOL TASARIMI",

    // Program categories
    progCatAll          = "TÜMÜ",
    progCatMuscle       = "KAS",
    progCatFatLoss      = "YAĞ YAKIMI",
    progCatStrength     = "GÜÇ",
    progCatEndurance    = "DAYANIKLILIK",
    progCatBeginner     = "BAŞLANGIÇ",
)

// ── English ───────────────────────────────────────────────────────────────────

val EnglishStrings = AppStrings(
    performanceMetrics  = "PERFORMANCE METRICS",
    seeAll              = "See All",
    weeklyActivity      = "WEEKLY ACTIVITY",
    thisWeekSummary     = "5 workouts this week",
    dayAbbreviations    = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    unitDays            = "days",
    unitStreak          = "streak",
    fatRatioLabel       = "BODY FAT",
    muscleMassLabel     = "MUSCLE MASS",
    activeDaysLabel     = "ACTIVE DAYS",
    dailyStreakLabel     = "DAILY STREAK",
    bmiLabel            = "BODY MASS IDX",
    weeklyCaloriesLabel = "WEEKLY CALORIES",
    trophyFirstWin      = "First Victory",
    trophySevenDay      = "7-Day Streak",
    trophyFiftyWorkouts = "50 Workouts",
    trophySuperMember   = "Super Member",
    trophyExcellence    = "Excellence",
    trophyEliteLabel    = "ELITE",
    achievements        = "ACHIEVEMENTS",
    accountSettings     = "ACCOUNT & SETTINGS",
    editProfileHint     = "Tap to edit profile",
    notificationsLabel  = "Notifications",
    notificationsActive = "Active",
    notificationsOff    = "Off",
    languageLabel       = "Language",
    currentLanguageName = "English",
    securityLabel       = "Security",
    securityValue       = "High",
    logoutLabel         = "Log Out",
    appearanceTitle     = "APPEARANCE SETTINGS",
    modeLabel           = "MODE",
    darkLabel           = "DARK",
    lightLabel          = "LIGHT",
    accentColorLabel    = "ACCENT COLOR",
    applyLabel          = "APPLY",
    notifSheetTitle     = "NOTIFICATION SETTINGS",
    workoutReminders    = "Workout Reminders",
    progressUpdates     = "Progress Updates",
    newsAlerts          = "News Alerts",
    langSheetTitle      = "LANGUAGE",
    turkishLabel        = "Türkçe",
    englishLabel        = "English",

    // Workout
    todayProgram        = "TODAY'S PROGRAM",
    completedLabel      = "completed",
    helloAthlete        = "Hello, Athlete 👋",
    unitMin             = "min",
    unitDone            = "done",
    streakTitle         = "%d-Day Streak!",
    streakStart         = "Start a Streak!",
    streakMotivate      = "Keep going, don't slow down.",
    streakBegin         = "Move today to start your streak.",
    restDayTitle        = "RECOVERY",
    restDaySubtitle     = "Muscles are growing. Rest today.",
    recoveryTip1        = "💧 Stay hydrated",
    recoveryTip2        = "😴 Sleep 8 hours",
    recoveryTip3        = "🥗 Eat enough protein",

    // AI Coach
    oracleWelcome       = "Welcome to Oracle Sanctuary. Ready to analyse your performance goals.",
    chipNutrition       = "Nutrition Advice",
    chipMotivation      = "Motivation",
    chipProgram         = "Program Suggestion",
    chipRecovery        = "Recovery",
    chipHiitVsLiss      = "HIIT vs LISS",

    // News – categories
    newsCatAll          = "ALL",
    newsCatSaved        = "SAVED",
    newsCatScience      = "SCIENCE",
    newsCatNutrition    = "NUTRITION",
    newsCatTraining     = "TRAINING",
    newsCatSports       = "SPORTS",
    newsCatMind         = "MIND",
    newsCatLifestyle    = "LIFESTYLE",
    newsCatRecovery     = "RECOVERY",
    newsCatTech         = "TECHNOLOGY",

    // News – UI labels
    liveLabel           = "LIVE",
    newsCountLabel      = "NEWS",
    allNewsLabel        = "ALL NEWS",
    noSavedNews         = "No saved articles yet",
    noCategoryNews      = "No articles found in this category",
    readingLabel        = "READ",
    aiSummaryLabel      = "AI SUMMARY",
    contentLabel        = "CONTENT",
    goToOriginal        = "READ ORIGINAL ARTICLE",
    translatingLabel    = "Translating to Turkish…",
    translatedLabel     = "AI Translated to Turkish",
    sourceLabel         = "Source",
    noSummaryLabel      = "No summary available for this article.",
    saveArticle         = "Save",
    unsaveArticle       = "Remove",

    // Program Builder
    programStudioSub    = "Choose from ready programs or design your own.",
    createWithAI        = "Create with AI",
    createManually      = "Create Manually",
    activeProtocols     = "ACTIVE PROTOCOLS",
    readyPrograms       = "READY PROGRAMS",
    programSavedMsg     = "saved ✓",
    programSavedDefault = "Program saved ✓",
    applyProtocol       = "APPLY PROTOCOL",
    saveProtocol        = "SAVE PROTOCOL",
    activeLabel         = "ACTIVE",
    analysisResult      = "ANALYSIS RESULT",
    dayLabel            = "DAY",
    weekLabel           = "WEEK",
    levelLabel          = "LEVEL",
    muscleDistLabel     = "MUSCLE LOAD DISTRIBUTION",
    scheduleLabel       = "TRAINING SCHEDULE",
    aiProtocolSub       = "PROTOCOL GENERATION",
    manualBuilderSub    = "PROTOCOL DESIGN",

    // Program categories
    progCatAll          = "ALL",
    progCatMuscle       = "MUSCLE",
    progCatFatLoss      = "FAT LOSS",
    progCatStrength     = "STRENGTH",
    progCatEndurance    = "ENDURANCE",
    progCatBeginner     = "BEGINNER",
)

// ── Convenience extensions ────────────────────────────────────────────────────

val AppThemeState.strings: AppStrings
    get() = if (language == AppLanguage.ENGLISH) EnglishStrings else TurkishStrings

/** Maps an internal category key (always Turkish) to the localised display label. */
fun AppStrings.localizedNewsCategory(key: String): String = when (key) {
    "TÜMÜ"          -> newsCatAll
    "KAYDEDILENLER" -> newsCatSaved
    "BİLİM"         -> newsCatScience
    "BESLENME"      -> newsCatNutrition
    "ANTRENMAN"     -> newsCatTraining
    "SPOR"          -> newsCatSports
    "ZİHİN"         -> newsCatMind
    "YAŞAM"         -> newsCatLifestyle
    "TOPARLANMA"    -> newsCatRecovery
    "TEKNOLOJİ"     -> newsCatTech
    else            -> key
}

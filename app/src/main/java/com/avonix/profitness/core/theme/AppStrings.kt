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
    val backgroundToneLabel : String,
    val surfaceClassicLabel : String,
    val surfaceOledLabel    : String,
    val surfaceGraphiteLabel: String,
    val intensityLabel      : String,
    val intensityNeonLabel  : String,
    val intensityPastelLabel: String,
    val previewLabel        : String,

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
    val newsCatNutrition    : String,   // "BESLENME" / "NUTRITION"
    val newsCatTraining     : String,   // "ANTRENMAN" / "TRAINING"
    val newsCatSports       : String,   // "SPOR" / "SPORTS"
    val newsCatMind         : String,   // "ZİHİN" / "MIND"
    val newsCatLifestyle    : String,   // "YAŞAM" / "LIFESTYLE"
    val newsCatRecovery     : String,   // "TOPARLANMA" / "RECOVERY"

    // ── News screen – UI labels ──────────────────────────────────────────────
    val liveLabel           : String,   // "CANLI" / "LIVE"
    val newsCountLabel      : String,   // "HABER" / "NEWS"
    val allNewsLabel        : String,   // "TÜM HABERLER" / "ALL NEWS"
    val noSavedNews         : String,
    val noCategoryNews      : String,
    val readingLabel        : String,   // "OKUMA" / "READ"
    val readTimeUnit        : String,   // "dk" / "min"
    val aiSummaryLabel      : String,   // "AI ÖZETİ" / "AI SUMMARY"
    val contentLabel        : String,   // "İÇERİK" / "CONTENT"
    val goToOriginal        : String,   // "HABERİN ORİJİNALİNE GİT" / "READ ORIGINAL ARTICLE"
    val translatingLabel    : String,   // "Türkçeye çevriliyor…" / "Translating…"
    val loadingContentLabel : String,   // "İçerik yükleniyor…"  / "Loading content…"
    val translatedLabel     : String,   // "Yapay zeka ile çevrildi" / "AI Translated"
    val sourceLabel         : String,   // "Kaynak" / "Source"
    val noSummaryLabel      : String,   // "Özet mevcut değil." / "No summary available."
    val saveArticle              : String,   // "Kaydet" / "Save"
    val unsaveArticle            : String,   // "Kaydı kaldır" / "Remove"
    val reportArticle            : String,   // "Bildir" / "Report"
    val reportDialogTitle        : String,   // "Haberi Bildir" / "Report Article"
    val reportReasonMisinfo      : String,   // "Yanlış Bilgi" / "Misinformation"
    val reportReasonSpam         : String,   // "Spam" / "Spam"
    val reportReasonInappropriate: String,   // "Uygunsuz İçerik" / "Inappropriate Content"
    val reportConfirm            : String,   // "Bildir" / "Report"
    val reportCancel             : String,   // "İptal" / "Cancel"
    val reportSuccessMsg         : String,   // "Haber bildirildi ve kaldırıldı." / "Article reported and removed."
    val newArticlesMsg           : String,   // "yeni haber" / "new articles"
    val noNewArticlesMsg         : String,   // "Haberler güncel." / "Already up to date."

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

    // ── Challenge 2.0 (FAZ 7J) — kind & event modes ──────────────────────────
    val challengeKindMetric    : String,   // "METRİK" / "METRIC"
    val challengeKindEvent     : String,   // "ETKİNLİK" / "EVENT"
    val eventModePhysical      : String,   // "FİZİKSEL" / "PHYSICAL"
    val eventModeOnline        : String,   // "ONLİNE" / "ONLINE"
    val eventModeMovementList  : String,   // "HAREKET" / "MOVEMENTS"

    // ── Challenge 2.0 — create overlay ───────────────────────────────────────
    val newChallengeTitle      : String,   // "YENİ CHALLENGE" / "NEW CHALLENGE"
    val newEventTitle          : String,   // "YENİ ETKİNLİK" / "NEW EVENT"
    val challengeFieldTitle    : String,   // "BAŞLIK" / "TITLE"
    val challengeFieldDesc     : String,   // "AÇIKLAMA" / "DESCRIPTION"
    val challengeVisibility    : String,   // "GÖRÜNÜRLÜK" / "VISIBILITY"
    val challengeVisPublic     : String,   // "PUBLIC" / "PUBLIC"
    val challengeVisPrivate    : String,   // "ÖZEL" / "PRIVATE"
    val challengeCreateBtn     : String,   // "OLUŞTUR" / "CREATE"

    // ── Challenge 2.0 — detail overlay ───────────────────────────────────────
    val challengeLabel         : String,   // "CHALLENGE" / "CHALLENGE"
    val eventLabel             : String,   // "ETKİNLİK" / "EVENT"
    val openInMapsLabel        : String,   // "HARİTADA AÇ" / "OPEN IN MAPS"
    val openLinkLabel          : String,   // "BAĞLANTIYI AÇ" / "OPEN LINK"
    val completeAllLabel       : String,   // "TÜMÜNÜ TAMAMLA" / "COMPLETE ALL"
    val skipProgramTodayLabel  : String,   // "Bugün programı atla" / "Skip today's program"
    val joinLabel              : String,   // "KATIL" / "JOIN"
    val leaveLabel             : String,   // "AYRIL" / "LEAVE"

    // ── Challenge 2.0 — dashboard ────────────────────────────────────────────
    val todayEventsTitle       : String,   // "BUGÜNKÜ ETKİNLİKLERİN" / "YOUR EVENTS TODAY"
    val upcomingEventsTitle    : String,   // "YAKLAŞAN ETKİNLİKLER" / "UPCOMING EVENTS"
    val skippedProgramTitle    : String,   // "BUGÜN ETKİNLİK GÜNÜ" / "EVENT DAY"
    val skippedProgramBody     : String,   // long sentence

    // ── Challenge 2.0 — private join password dialog ─────────────────────────
    val privateJoinTitle       : String,   // "ŞİFRE GEREKLİ" / "PASSWORD REQUIRED"
    val privateJoinHint        : String,   // long sentence
    val privateJoinCancel      : String    // "İPTAL" / "CANCEL"
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
    backgroundToneLabel = "ARKA PLAN TONU",
    surfaceClassicLabel = "KLASİK",
    surfaceOledLabel    = "OLED",
    surfaceGraphiteLabel= "GRAFİT",
    intensityLabel      = "YOĞUNLUK",
    intensityNeonLabel  = "NEON",
    intensityPastelLabel= "PASTEL",
    previewLabel        = "ÖN İZLEME",
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
    newsCatNutrition    = "BESLENME",
    newsCatTraining     = "ANTRENMAN",
    newsCatSports       = "SPOR",
    newsCatMind         = "ZİHİN",
    newsCatLifestyle    = "YAŞAM",
    newsCatRecovery     = "TOPARLANMA",

    // News – UI labels
    liveLabel           = "CANLI",
    newsCountLabel      = "HABER",
    allNewsLabel        = "TÜM HABERLER",
    noSavedNews         = "Henüz kaydedilen haber yok",
    noCategoryNews      = "Bu kategoride haber bulunamadı",
    readingLabel        = "OKUMA",
    readTimeUnit        = "dk",
    aiSummaryLabel      = "AI ÖZETİ",
    contentLabel        = "İÇERİK",
    goToOriginal        = "HABERİN ORİJİNALİNE GİT",
    translatingLabel    = "Türkçeye çevriliyor…",
    loadingContentLabel = "İçerik yükleniyor…",
    translatedLabel     = "Yapay zeka ile Türkçeye çevrildi",
    sourceLabel         = "Kaynak",
    noSummaryLabel      = "Bu haber için özet mevcut değil.",
    saveArticle              = "Kaydet",
    unsaveArticle            = "Kaydı kaldır",
    reportArticle            = "Bildir",
    reportDialogTitle        = "Haberi Bildir",
    reportReasonMisinfo      = "Yanlış Bilgi",
    reportReasonSpam         = "Spam",
    reportReasonInappropriate= "Uygunsuz İçerik",
    reportConfirm            = "Bildir",
    reportCancel             = "İptal",
    reportSuccessMsg         = "Haber bildirildi ve listenizden kaldırıldı.",
    newArticlesMsg           = "yeni haber",
    noNewArticlesMsg         = "Haberler güncel.",

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

    // Challenge 2.0 — kind & event modes
    challengeKindMetric    = "METRİK",
    challengeKindEvent     = "ETKİNLİK",
    eventModePhysical      = "FİZİKSEL",
    eventModeOnline        = "ONLİNE",
    eventModeMovementList  = "HAREKET",

    // Challenge 2.0 — create overlay
    newChallengeTitle      = "YENİ CHALLENGE",
    newEventTitle          = "YENİ ETKİNLİK",
    challengeFieldTitle    = "BAŞLIK",
    challengeFieldDesc     = "AÇIKLAMA",
    challengeVisibility    = "GÖRÜNÜRLÜK",
    challengeVisPublic     = "PUBLIC",
    challengeVisPrivate    = "ÖZEL",
    challengeCreateBtn     = "OLUŞTUR",

    // Challenge 2.0 — detail overlay
    challengeLabel         = "CHALLENGE",
    eventLabel             = "ETKİNLİK",
    openInMapsLabel        = "HARİTADA AÇ",
    openLinkLabel          = "BAĞLANTIYI AÇ",
    completeAllLabel       = "TÜMÜNÜ TAMAMLA",
    skipProgramTodayLabel  = "Bugün programı atla",
    joinLabel              = "KATIL",
    leaveLabel             = "AYRIL",

    // Challenge 2.0 — dashboard
    todayEventsTitle       = "BUGÜNKÜ ETKİNLİKLERİN",
    upcomingEventsTitle    = "YAKLAŞAN ETKİNLİKLER",
    skippedProgramTitle    = "BUGÜN ETKİNLİK GÜNÜ",
    skippedProgramBody     = "Günlük programı atladın. Etkinlik detayından hareketlerini işaretleyebilirsin.",

    // Challenge 2.0 — private join dialog
    privateJoinTitle       = "ŞİFRE GEREKLİ",
    privateJoinHint        = "Bu challenge özel. Katılmak için şifreyi gir.",
    privateJoinCancel      = "İPTAL"
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
    backgroundToneLabel = "BACKGROUND TONE",
    surfaceClassicLabel = "CLASSIC",
    surfaceOledLabel    = "OLED",
    surfaceGraphiteLabel= "GRAPHITE",
    intensityLabel      = "INTENSITY",
    intensityNeonLabel  = "NEON",
    intensityPastelLabel= "PASTEL",
    previewLabel        = "PREVIEW",
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
    newsCatNutrition    = "NUTRITION",
    newsCatTraining     = "TRAINING",
    newsCatSports       = "SPORTS",
    newsCatMind         = "MIND",
    newsCatLifestyle    = "LIFESTYLE",
    newsCatRecovery     = "RECOVERY",

    // News – UI labels
    liveLabel           = "LIVE",
    newsCountLabel      = "NEWS",
    allNewsLabel        = "ALL NEWS",
    noSavedNews         = "No saved articles yet",
    noCategoryNews      = "No articles found in this category",
    readingLabel        = "READ",
    readTimeUnit        = "min",
    aiSummaryLabel      = "AI SUMMARY",
    contentLabel        = "CONTENT",
    goToOriginal        = "READ ORIGINAL ARTICLE",
    translatingLabel    = "Translating to English…",
    loadingContentLabel = "Loading content…",
    translatedLabel     = "AI Translated to English",
    sourceLabel         = "Source",
    noSummaryLabel      = "No summary available for this article.",
    saveArticle              = "Save",
    unsaveArticle            = "Remove",
    reportArticle            = "Report",
    reportDialogTitle        = "Report Article",
    reportReasonMisinfo      = "Misinformation",
    reportReasonSpam         = "Spam",
    reportReasonInappropriate= "Inappropriate Content",
    reportConfirm            = "Report",
    reportCancel             = "Cancel",
    reportSuccessMsg         = "Article reported and removed from your feed.",
    newArticlesMsg           = "new articles",
    noNewArticlesMsg         = "Already up to date.",

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

    // Challenge 2.0 — kind & event modes
    challengeKindMetric    = "METRIC",
    challengeKindEvent     = "EVENT",
    eventModePhysical      = "PHYSICAL",
    eventModeOnline        = "ONLINE",
    eventModeMovementList  = "MOVEMENTS",

    // Challenge 2.0 — create overlay
    newChallengeTitle      = "NEW CHALLENGE",
    newEventTitle          = "NEW EVENT",
    challengeFieldTitle    = "TITLE",
    challengeFieldDesc     = "DESCRIPTION",
    challengeVisibility    = "VISIBILITY",
    challengeVisPublic     = "PUBLIC",
    challengeVisPrivate    = "PRIVATE",
    challengeCreateBtn     = "CREATE",

    // Challenge 2.0 — detail overlay
    challengeLabel         = "CHALLENGE",
    eventLabel             = "EVENT",
    openInMapsLabel        = "OPEN IN MAPS",
    openLinkLabel          = "OPEN LINK",
    completeAllLabel       = "COMPLETE ALL",
    skipProgramTodayLabel  = "Skip today's program",
    joinLabel              = "JOIN",
    leaveLabel             = "LEAVE",

    // Challenge 2.0 — dashboard
    todayEventsTitle       = "YOUR EVENTS TODAY",
    upcomingEventsTitle    = "UPCOMING EVENTS",
    skippedProgramTitle    = "EVENT DAY",
    skippedProgramBody     = "You're skipping today's program. Mark your movements from the event detail.",

    // Challenge 2.0 — private join dialog
    privateJoinTitle       = "PASSWORD REQUIRED",
    privateJoinHint        = "This challenge is private. Enter the password to join.",
    privateJoinCancel      = "CANCEL"
)

// ── Convenience extensions ────────────────────────────────────────────────────

val AppThemeState.strings: AppStrings
    get() = if (language == AppLanguage.ENGLISH) EnglishStrings else TurkishStrings

/** Maps an internal category key (always Turkish) to the localised display label. */
fun AppStrings.localizedNewsCategory(key: String): String = when (key) {
    "TÜMÜ"          -> newsCatAll
    "KAYDEDILENLER" -> newsCatSaved
    "BESLENME"      -> newsCatNutrition
    "ANTRENMAN"     -> newsCatTraining
    "SPOR"          -> newsCatSports
    "ZİHİN"         -> newsCatMind
    "YAŞAM"         -> newsCatLifestyle
    "TOPARLANMA"    -> newsCatRecovery
    else            -> key
}

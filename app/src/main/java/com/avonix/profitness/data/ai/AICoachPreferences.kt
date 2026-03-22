package com.avonix.profitness.data.ai

import android.content.Context
import android.content.SharedPreferences

enum class ResponseLength(
    val label: String,
    val description: String,
    val systemHint: String
) {
    SHORT(
        label       = "Kısa & Net",
        description = "Özet, doğrudan cevaplar",
        systemHint  = "Cevaplarını kısa ve öz tut. Maksimum 2-3 cümle. Gereksiz açıklama, giriş veya kapanış yapma."
    ),
    MEDIUM(
        label       = "Dengeli",
        description = "Ne çok kısa ne çok uzun",
        systemHint  = "Cevaplarını dengeli uzunlukta ver. 1-3 kısa paragraf yeterli."
    ),
    LONG(
        label       = "Detaylı",
        description = "Kapsamlı açıklamalar",
        systemHint  = "Cevaplarını detaylı ve kapsamlı ver. Adım adım açıklamalar, örnekler ve pratik ipuçları ekle."
    )
}

enum class CommunicationStyle(
    val label: String,
    val description: String,
    val systemHint: String
) {
    FRIENDLY(
        label       = "Arkadaş Canlısı",
        description = "Sıcak, samimi, destekleyici",
        systemHint  = "Sıcak ve samimi bir arkadaş gibi konuş. Kullanıcıyı destekle, pozitif tut ama gerçekçi ol."
    ),
    COACH(
        label       = "Koç Tarzı",
        description = "Disiplinli, hedef odaklı, motive edici",
        systemHint  = "Bir spor koçu gibi konuş. Disiplinli, net ve hedef odaklı ol. Sonuçlara ve aksiyona odaklan."
    ),
    SCIENTIFIC(
        label       = "Bilimsel & Analitik",
        description = "Kanıta dayalı, teknik, açıklayıcı",
        systemHint  = "Bilimsel ve analitik bir ton kullan. Kanıta dayalı bilgi ver. Gerektiğinde mekanizmaları açıkla ama anlaşılır kal."
    ),
    BLUNT(
        label       = "Sert & Dürüst",
        description = "Direkt, şekersiz, gerçekçi",
        systemHint  = "Sert ve dürüst ol. Sugarcoating yapma, gerçeği direkt söyle. Övgü sadece gerçekten hak edildiğinde ver."
    )
}

data class AICoachPrefs(
    val responseLength         : ResponseLength    = ResponseLength.MEDIUM,
    val communicationStyle     : CommunicationStyle = CommunicationStyle.COACH,
    val allowProfileAccess     : Boolean            = true,
    val allowThirdPartyProcessing: Boolean          = false,
    val onboardingCompleted    : Boolean            = false
)

class AICoachPrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_coach_prefs", Context.MODE_PRIVATE)

    fun getPrefs(): AICoachPrefs = AICoachPrefs(
        responseLength = runCatching {
            ResponseLength.valueOf(prefs.getString("response_length", ResponseLength.MEDIUM.name)!!)
        }.getOrDefault(ResponseLength.MEDIUM),
        communicationStyle = runCatching {
            CommunicationStyle.valueOf(prefs.getString("communication_style", CommunicationStyle.COACH.name)!!)
        }.getOrDefault(CommunicationStyle.COACH),
        allowProfileAccess      = prefs.getBoolean("allow_profile_access", true),
        allowThirdPartyProcessing = prefs.getBoolean("allow_third_party", false),
        onboardingCompleted     = prefs.getBoolean("onboarding_completed", false)
    )

    fun savePrefs(aiPrefs: AICoachPrefs) {
        prefs.edit()
            .putString("response_length",      aiPrefs.responseLength.name)
            .putString("communication_style",  aiPrefs.communicationStyle.name)
            .putBoolean("allow_profile_access", aiPrefs.allowProfileAccess)
            .putBoolean("allow_third_party",   aiPrefs.allowThirdPartyProcessing)
            .putBoolean("onboarding_completed", true)
            .apply()
    }
}

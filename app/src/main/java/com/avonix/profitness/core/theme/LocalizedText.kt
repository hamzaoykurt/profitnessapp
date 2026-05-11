package com.avonix.profitness.core.theme

fun AppThemeState.t(tr: String, en: String): String =
    if (language == AppLanguage.ENGLISH) en else tr

fun AppThemeState.ui(tr: String): String =
    if (language == AppLanguage.ENGLISH) EnglishUiFallbacks[tr] ?: tr else tr

fun AppThemeState.ui(tr: String, en: String): String = t(tr, en)

private val EnglishUiFallbacks = mapOf(
    "Kredi bitti!" to "Out of credits!",
    "Kredi satın al veya plana yükselt" to "Buy credits or upgrade your plan",
    "Kalan bakiye: %d kredi" to "Remaining balance: %d credits",
    "0 kredi" to "0 credits",
    "%d kredi" to "%d credits",
    "1 kredi" to "1 credit",
    "1 kredi / mesaj" to "1 credit / message",
    "3 kredi" to "3 credits",
    "3 kredi / AI analiz" to "3 credits / AI analysis",
    "Profil güncellendi" to "Profile updated",
    "Güncelleme başarısız" to "Update failed",
    "Fotoğraf yüklendi" to "Photo uploaded",
    "Fotoğraf yüklenemedi" to "Photo could not be uploaded",
    "Geçerli bir ağırlık girin (örn. 75.5)" to "Enter a valid weight (e.g. 75.5)",
    "Ağırlık kaydedildi" to "Weight saved",
    "Kayıt güncellendi" to "Entry updated",
    "Kayıt başarısız. Lütfen tekrar deneyin." to "Save failed. Please try again.",
    "Kayıt silindi" to "Entry deleted",
    "Silme başarısız" to "Delete failed",
    "Trend analizi için en az iki geçerli kilo kaydı gerekiyor." to "At least two valid weight entries are needed for trend analysis.",
    "Trend analizi şu an mevcut değil." to "Trend analysis is not available right now.",
    "Bağlantı hatası. Lütfen tekrar dene." to "Connection error. Please try again.",
    "Oturum bulunamadı" to "Session not found",
    "Egzersizler yüklenemedi" to "Exercises could not be loaded",
    "Yapay zeka yanıt vermedi" to "AI did not respond",
    "JSON bulunamadı" to "JSON not found",
    "JSON parse hatası" to "JSON parse error",
    "Program günleri bulunamadı" to "Program days not found",
    "Bilinmeyen hata" to "Unknown error",
    "Şifreler eşleşmiyor." to "Passwords do not match.",
    "6 haneli kodu eksiksiz girin." to "Enter the full 6-digit code.",
    "Email adresinizi girin." to "Enter your email address.",
    "Geçerli bir email adresi girin." to "Enter a valid email address.",
    "Lütfen tüm alanları doldurun." to "Please fill in all fields.",
    "Şifre en az 6 karakter olmalı." to "Password must be at least 6 characters.",
    "Email adresiniz doğrulanmamış. Gelen kutunuzu kontrol edin." to "Your email is not verified. Check your inbox.",
    "Hatalı email veya şifre." to "Incorrect email or password.",
    "İnternet bağlantısını kontrol edin." to "Check your internet connection.",
    "Giriş yapılamadı. Tekrar dene." to "Could not sign in. Try again.",
    "Bu email adresi zaten kayıtlı." to "This email address is already registered.",
    "Kayıt olunamadı. Tekrar dene." to "Could not sign up. Try again.",
    "Doğrulama başarısız." to "Verification failed.",
    "Kodun süresi dolmuş. Yeni kod gönder." to "The code has expired. Send a new code.",
    "Kod hatalı. Tekrar dene." to "The code is incorrect. Try again.",
    "Kod doğrulanamadı. Tekrar dene." to "Could not verify the code. Try again.",
    "Bir hata oluştu." to "Something went wrong.",
    "İşlem gerçekleştirilemedi. Tekrar dene." to "The action could not be completed. Try again."
)

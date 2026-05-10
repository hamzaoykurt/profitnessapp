package com.avonix.profitness.core.theme

fun AppThemeState.t(tr: String, en: String): String =
    if (language == AppLanguage.ENGLISH) en else tr

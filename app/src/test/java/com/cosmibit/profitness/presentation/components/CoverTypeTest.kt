package com.cosmibit.profitness.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Kapak türü eşlemesi: spor adı > kategori > fitness varsayılanı (hash yok). */
class CoverTypeTest {
    private val table = listOf(
        "Tenis Atletik" to CoverType.Tennis,
        "Kürek Erg Temel" to CoverType.Rowing,
        "Bisiklet 4 Hafta" to CoverType.Bike,
        "7 Günlük Hibrit" to CoverType.Strength,
        "Hamza'nın Güncellenmiş Antrenman Programı" to CoverType.Strength,
        "Full Body Başlangıç" to CoverType.Strength,
        "Push Pull Legs" to CoverType.Strength,
        "Upper Lower Split" to CoverType.Strength,
        "Dönüşüm Programı" to CoverType.Strength,
        "Göğüs Sırt Kuvvet" to CoverType.Strength,
        "" to CoverType.Strength,
        "Yüzme Başlangıç" to CoverType.Swim,
        "Swimming Endurance" to CoverType.Swim,
        "Futbol Kondisyon" to CoverType.Football,
        "Halısa" to CoverType.Football,
        "Halı saha maçı" to CoverType.Football,
        "hali saha" to CoverType.Football,
        "Futsal Ligi" to CoverType.Football,
        "Basketbol Atletik" to CoverType.Basketball,
        "Voleybol Temel" to CoverType.Volleyball,
        "Boks Başlangıç" to CoverType.Combat,
        "MMA Conditioning" to CoverType.Combat,
        "Yoga Sabah" to CoverType.Yoga,
        "Esneklik ve Mobilite" to CoverType.Yoga,
        "Pilates Core" to CoverType.Pilates,
        "Koşu 5K" to CoverType.Run,
        "Running Base" to CoverType.Run,
        "Yürüyüş Planı" to CoverType.Walk,
        "Hiking Trekking" to CoverType.Walk,
        "HIIT Yağ Yakımı" to CoverType.Cardio,
        "KARDİYO DAYANIKLILIK" to CoverType.Cardio,
        "Core ve Karın" to CoverType.Core,
        "Kayak Hazırlık" to CoverType.Ski,
        "Golf Mobilite" to CoverType.Golf,
        "Rowing Machine Intro" to CoverType.Rowing,
        "KUVVET Tenis" to CoverType.Tennis,  // spor adı kategoriyi ezer
        "Herkese Açık Koşu Etkinliği running Koşu kardiyo" to CoverType.Run,
        "Hafta Sonu Challenge cycling Bisiklet kardiyo" to CoverType.Bike,
        "30 Gün Şınav strength Kuvvet kuvvet" to CoverType.Strength,
        "Havuz Günü swimming Yüzme kardiyo" to CoverType.Swim,
        "Mesafe Yarışı kardiyo" to CoverType.Cardio
    )

    @Test fun titleToType() {
        table.forEach { (title, expected) -> assertEquals(title, expected, coverTypeFor(title)) }
    }

    @Test fun unknownIsFitnessCoralNotRandomSport() {
        listOf("Atletik Temel", "Güncellenmiş", "Program 12", "xyz", null).forEach {
            assertEquals(CoverType.Strength, coverTypeFor(it))
        }
    }

    @Test fun hueDiffersBetweenSportsAndFitness() {
        assertNotEquals(CoverType.Strength.hue, CoverType.Run.hue)
        assertNotEquals(CoverType.Tennis.hue, CoverType.Rowing.hue)
        assertEquals(CoverType.Strength.hue, coverHueFor("Hibrit"))
    }
}

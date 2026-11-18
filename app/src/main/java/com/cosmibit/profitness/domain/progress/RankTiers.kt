package com.cosmibit.profitness.domain.progress

/**
 * TEK KAYNAK: Rank (uzun vadeli lig) eşikleri ve seviye adımı.
 *  - Seviye: her [LEVEL_XP] XP'de bir yükselir (kısa vadeli ilerleme).
 *  - Rank: toplam XP eşiğiyle lig (Bronze -> Diamond), uzun vadeli.
 * Hesap mantığı değişmedi; Profil, Başarımlar ve sıralama kartı bu tablodan okur.
 */
object RankTiers {
    const val LEVEL_XP = 500

    data class Tier(val name: String, val minXp: Int)

    val tiers: List<Tier> = listOf(
        Tier("Bronze", 0),
        Tier("Silver", 1000),
        Tier("Gold", 5000),
        Tier("Platinum", 15000),
        Tier("Diamond", 50000)
    )

    fun rankFor(xp: Int): String = tiers.last { xp >= it.minXp }.name

    /** Bir sonraki rank (en üstteyse null). */
    fun nextTier(xp: Int): Tier? = tiers.firstOrNull { it.minXp > xp }

    /** Sonraki rank'a ilerleme 0..1, toplam XP / sonraki eşik ("2100/5000"); en üstte 1. Başarımlar sayfasıyla aynı. */
    fun progressToNext(xp: Int): Float {
        val next = nextTier(xp) ?: return 1f
        return (xp.toFloat() / next.minXp).coerceIn(0f, 1f)
    }

    /** Sonraki seviyeye kalan XP. */
    fun xpToNextLevel(xp: Int): Int = LEVEL_XP - (xp.coerceAtLeast(0) % LEVEL_XP)
}

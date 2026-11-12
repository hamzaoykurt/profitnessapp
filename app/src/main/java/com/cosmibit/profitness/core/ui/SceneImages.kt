package com.cosmibit.profitness.core.ui

/** Kategori -> yedek fotoğraf URL'i (eskiden WorkoutViewModel.categoryImageFallback; URL'ler aynı). */
fun categoryImageUrl(category: String): String = when {
    category.contains("Göğüs", ignoreCase = true) ->
        "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800"
    category.contains("Sırt", ignoreCase = true) ->
        "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?w=800"
    category.contains("Omuz", ignoreCase = true) ->
        "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=800"
    category.contains("Bacak", ignoreCase = true) ||
    category.contains("Quad", ignoreCase = true) ->
        "https://images.unsplash.com/photo-1567013127542-490d757e51fc?w=800"
    category.contains("Core", ignoreCase = true) ->
        "https://images.unsplash.com/photo-1599058917212-d750089bc07e?w=800"
    category.contains("Kardiyo", ignoreCase = true) ->
        "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=800"
    else ->
        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800"
}

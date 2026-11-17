package com.cosmibit.profitness.presentation.dashboard

/**
 * Ana sekme geçmişi (geri tuşu = bir önceki sekme). Saf mantık; Compose'tan bağımsız test edilir.
 * - Aynı sekmeye geçiş yığına yazılmaz; ardışık tekrar kaydı yok; üst sınır [max] (en eski düşer).
 * - [pop] mevcut sekmeyle aynı kayıtları atlar, gidilecek sekmeyi döndürür (yığın boşsa null).
 */
class TabHistory<T>(private val max: Int = 10) {
    private val stack = ArrayDeque<T>()

    val size: Int get() = stack.size
    fun isEmpty(): Boolean = stack.isEmpty()
    fun snapshot(): List<T> = stack.toList()

    fun onNavigate(from: T, to: T) {
        if (from == to) return
        if (stack.lastOrNull() != from) stack.addLast(from)
        while (stack.size > max) stack.removeFirst()
    }

    fun pop(current: T): T? {
        while (stack.isNotEmpty()) {
            val t = stack.removeLast()
            if (t != current) return t
        }
        return null
    }
}

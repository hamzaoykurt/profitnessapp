package com.avonix.profitness.presentation.news

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Maximum articles kept in memory. LazyColumn only renders visible items (virtualised),
// but the backing list still lives in the heap. 300 articles × ~5 KB average ≈ 1.5 MB —
// well within budget. When loadMore appends new items, the oldest are dropped to stay
// under this cap, preventing unbounded memory growth.
private const val MAX_ARTICLES = 300

// ── UI State ──────────────────────────────────────────────────────────────────

data class NewsUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    // Set to false when a loadMore fetch returns no new articles, so we don't hammer
    // the network. Reset to true on manual refresh.
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val lastUpdated: Long = 0L
)

data class ArticleDetailState(
    val article: Article,
    val displayTitle: String = article.title,
    val displaySummary: String = article.summary,
    val displayContent: String = article.content,
    val isTranslating: Boolean = false,
    val wasTranslated: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("news_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<ArticleDetailState?>(null)
    val detailState: StateFlow<ArticleDetailState?> = _detailState.asStateFlow()

    private val _savedIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet("saved_ids", emptySet()) ?: emptySet()
    )
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val _reportedIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet("reported_ids", emptySet()) ?: emptySet()
    )
    val reportedIds: StateFlow<Set<String>> = _reportedIds.asStateFlow()

    /**
     * Card-level title translation cache. Populated eagerly in the background when Turkish
     * mode is active, so card titles appear localised without any visible loading state.
     * Key = Article.id, Value = translated title string.
     */
    private val _cardTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val cardTranslations: StateFlow<Map<String, String>> = _cardTranslations.asStateFlow()

    private var cardTranslationJob: Job? = null

    /**
     * Persistent click counts keyed by a stable article key (title prefix + sourceName).
     * Loaded from SharedPreferences so trending articles persist across sessions.
     * Used to promote most-read articles into the featured carousel.
     */
    private val clickCounts: MutableMap<String, Int> = run {
        val stored = prefs.all
        stored.entries
            .filter { it.key.startsWith("click_") && it.value is Int }
            .associate { it.key.removePrefix("click_") to (it.value as Int) }
            .toMutableMap()
    }

    init { loadNews() }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val articles = NewsRepository.fetchAllNews()
                _uiState.value = NewsUiState(
                    articles = applyClickRanking(articles),
                    isLoading = false,
                    canLoadMore = true,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = NewsUiState(
                    articles = DEMO_FALLBACK,
                    isLoading = false,
                    canLoadMore = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Fetches fresh RSS data and appends articles not already in the list.
     * Applies a hard cap of [MAX_ARTICLES] to prevent unbounded memory growth —
     * oldest articles (by publishedAtMs) are dropped when the cap is reached.
     * Sets canLoadMore = false if no new articles are available so callers
     * can stop triggering further requests until the next manual refresh.
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            try {
                val existing = _uiState.value.articles
                val existingKeys = existing.map { articleKey(it) }.toHashSet()

                val fresh = NewsRepository.fetchAllNews()
                val newArticles = fresh.filter { articleKey(it) !in existingKeys }

                if (newArticles.isEmpty()) {
                    // No new content — stop auto-fetching until next manual refresh
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        canLoadMore = false
                    )
                    return@launch
                }

                // Merge, deduplicate, re-rank by click count, then cap
                val merged = (existing + newArticles)
                    .distinctBy { articleKey(it) }
                    .let { applyClickRanking(it) }
                    // Hard memory cap: keep the MAX_ARTICLES most recent articles.
                    // Oldest are dropped — they had the most scroll distance anyway.
                    .sortedByDescending { it.publishedAtMs }
                    .take(MAX_ARTICLES)

                _uiState.value = _uiState.value.copy(
                    articles = merged,
                    isLoadingMore = false,
                    canLoadMore = true,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    /**
     * Eagerly translates article titles for feed cards in the background (Turkish only).
     * Featured carousel articles are prioritised so they localise first.
     * Each translated title is emitted individually to avoid blocking the UI.
     */
    fun startCardTranslations(articles: List<Article>, lang: String) {
        if (lang != "tr" || articles.isEmpty()) return
        cardTranslationJob?.cancel()
        cardTranslationJob = viewModelScope.launch(Dispatchers.IO) {
            val ordered = articles.sortedByDescending { it.isFeatured }
            for (article in ordered) {
                if (!isActive) break
                if (article.id in _cardTranslations.value) continue
                try {
                    val translated = NewsRepository.translateText(article.title, "tr")
                    if (translated.isNotBlank() && translated != article.title) {
                        _cardTranslations.value = _cardTranslations.value + (article.id to translated)
                    }
                } catch (_: Exception) { /* skip silently */ }
            }
        }
    }

    fun toggleSave(articleId: String) {
        val updated = _savedIds.value.toMutableSet()
        if (articleId in updated) updated.remove(articleId) else updated.add(articleId)
        _savedIds.value = updated.toSet()
        prefs.edit().putStringSet("saved_ids", updated).apply()
    }

    fun reportArticle(articleId: String) {
        val updatedReported = _reportedIds.value.toMutableSet().also { it.add(articleId) }
        _reportedIds.value = updatedReported.toSet()
        prefs.edit().putStringSet("reported_ids", updatedReported).apply()

        val updatedSaved = _savedIds.value.toMutableSet().also { it.remove(articleId) }
        _savedIds.value = updatedSaved.toSet()
        prefs.edit().putStringSet("saved_ids", updatedSaved).apply()

        _detailState.value = null
    }

    /**
     * Opens article detail and records a click for featured ranking.
     * - Non-Turkish: shown immediately in original language.
     * - Turkish: show loading (isTranslating=true) until translation completes,
     *   then reveal the fully translated content — no English flash.
     *   Cached card titles are reused to skip redundant title translation.
     */
    fun openArticle(article: Article, appLanguage: String) {
        recordClick(article)

        if (appLanguage != "tr") {
            _detailState.value = ArticleDetailState(article = article)
            return
        }

        val cachedTitle = _cardTranslations.value[article.id]

        // Show loading indicator; keep content blank until translation is done
        _detailState.value = ArticleDetailState(
            article = article,
            displayTitle = cachedTitle ?: article.title,
            displaySummary = "",
            displayContent = "",
            isTranslating = true
        )

        viewModelScope.launch {
            try {
                val resolvedTitle = cachedTitle
                    ?: NewsRepository.translateText(article.title, "tr").ifBlank { article.title }

                val resolvedSummary =
                    NewsRepository.translateLongText(article.summary, "tr").ifBlank { article.summary }

                val resolvedContent = if (article.content.isNotBlank() && article.content != article.summary) {
                    NewsRepository.translateLongText(article.content, "tr").ifBlank { article.content }
                } else resolvedSummary

                _detailState.value = ArticleDetailState(
                    article = article,
                    displayTitle = resolvedTitle,
                    displaySummary = resolvedSummary,
                    displayContent = resolvedContent,
                    isTranslating = false,
                    wasTranslated = true
                )

                if (resolvedTitle != article.title) {
                    _cardTranslations.value = _cardTranslations.value + (article.id to resolvedTitle)
                }
            } catch (_: Exception) {
                // Fallback to original content on error
                _detailState.value = ArticleDetailState(
                    article = article,
                    isTranslating = false
                )
            }
        }
    }

    fun closeArticle() { _detailState.value = null }

    fun refresh() {
        cardTranslationJob?.cancel()
        _cardTranslations.value = emptyMap()
        loadNews()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Stable key for an article across fetches (independent of sequential ID).
     * Uses first 60 chars of title + sourceName so the same article from the same
     * source is always recognised as a duplicate even after re-fetch.
     */
    private fun articleKey(a: Article) = "${a.title.take(60)}|${a.sourceName}"

    /**
     * Increments the click counter for an article and persists it.
     * Then re-ranks the current article list so the most-clicked appear in the
     * featured carousel (top cards).
     */
    private fun recordClick(article: Article) {
        val key = articleKey(article)
        clickCounts[key] = (clickCounts[key] ?: 0) + 1
        prefs.edit().putInt("click_$key", clickCounts[key]!!).apply()

        // Re-rank featured in the current list without a full re-fetch
        val current = _uiState.value.articles
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(articles = applyClickRanking(current))
        }
    }

    /**
     * Promotes the 6 most-clicked articles to isFeatured = true.
     * Falls back to the 6 most-recent articles when no clicks have been recorded yet
     * (first launch). Articles with clicks always outrank articles without.
     */
    private fun applyClickRanking(articles: List<Article>): List<Article> {
        if (articles.isEmpty()) return articles

        val scored = articles.map { it to (clickCounts[articleKey(it)] ?: 0) }
        val hasAnyClicks = scored.any { (_, clicks) -> clicks > 0 }

        val featuredIds: Set<String> = if (hasAnyClicks) {
            // Most-clicked first; ties broken by publish date (most recent wins)
            scored
                .sortedWith(compareByDescending<Pair<Article, Int>> { (_, c) -> c }
                    .thenByDescending { (a, _) -> a.publishedAtMs })
                .take(6)
                .map { (a, _) -> a.id }
                .toHashSet()
        } else {
            // No clicks yet — feature the 6 most recent articles
            articles
                .sortedByDescending { it.publishedAtMs }
                .take(6)
                .map { it.id }
                .toHashSet()
        }

        return articles.map { it.copy(isFeatured = it.id in featuredIds) }
    }
}

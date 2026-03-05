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

// ── UI State ──────────────────────────────────────────────────────────────────

data class NewsUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = true,
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

    init { loadNews() }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val articles = NewsRepository.fetchAllNews()
                _uiState.value = NewsUiState(
                    articles = articles,
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = NewsUiState(
                    articles = DEMO_FALLBACK,
                    isLoading = false,
                    error = e.message
                )
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
     * Opens article detail.
     * - Non-Turkish: shown immediately in original language.
     * - Turkish: show loading (isTranslating=true) until translation completes,
     *   then reveal the fully translated content — no English flash.
     *   Cached card titles are reused to skip redundant title translation.
     */
    fun openArticle(article: Article, appLanguage: String) {
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
}

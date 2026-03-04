package com.avonix.profitness.presentation.news

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun toggleSave(articleId: String) {
        val updated = _savedIds.value.toMutableSet()
        if (articleId in updated) updated.remove(articleId) else updated.add(articleId)
        _savedIds.value = updated.toSet()
        prefs.edit().putStringSet("saved_ids", updated).apply()
    }

    /** Open article and auto-translate if the app language is Turkish (articles fetched in EN). */
    fun openArticle(article: Article, appLanguage: String) {
        _detailState.value = ArticleDetailState(article = article, isTranslating = true)
        viewModelScope.launch {
            // Fetched RSS articles are primarily English – translate when app is Turkish
            val shouldTranslate = appLanguage == "tr" && article.sourceUrl.isNotBlank()
            if (shouldTranslate) {
                try {
                    val (translatedTitle, translatedSummary) =
                        NewsRepository.translateArticle(article, "tr")
                    _detailState.value = ArticleDetailState(
                        article = article,
                        displayTitle = translatedTitle.ifBlank { article.title },
                        displaySummary = translatedSummary.ifBlank { article.summary },
                        isTranslating = false,
                        wasTranslated = translatedTitle != article.title
                    )
                } catch (_: Exception) {
                    _detailState.value = ArticleDetailState(article = article, isTranslating = false)
                }
            } else {
                _detailState.value = ArticleDetailState(article = article, isTranslating = false)
            }
        }
    }

    fun closeArticle() { _detailState.value = null }

    fun refresh() { loadNews() }
}

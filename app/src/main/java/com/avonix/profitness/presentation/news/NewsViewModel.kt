package com.avonix.profitness.presentation.news

import androidx.lifecycle.ViewModel
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

class NewsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<ArticleDetailState?>(null)
    val detailState: StateFlow<ArticleDetailState?> = _detailState.asStateFlow()

    init { loadNews() }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = NewsUiState(isLoading = true)
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

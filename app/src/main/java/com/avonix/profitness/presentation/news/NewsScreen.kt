@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.avonix.profitness.presentation.news

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.avonix.profitness.core.theme.*
import kotlinx.coroutines.delay



// ── Category internal keys (language-invariant, must match Article.category values) ──

private val CATEGORY_KEYS = listOf(
    "TÜMÜ", "KAYDEDILENLER", "SPOR", "ANTRENMAN", "BESLENME",
    "YAŞAM", "ZİHİN", "TOPARLANMA"
)

private const val PAGE_SIZE = 20

// ── Screen Entry ─────────────────────────────────────────────────────────────

@Composable
fun NewsScreen(newsViewModel: NewsViewModel = viewModel()) {
    val uiState          by newsViewModel.uiState.collectAsState()
    val detailState      by newsViewModel.detailState.collectAsState()
    val savedIds         by newsViewModel.savedIds.collectAsState()
    val reportedIds      by newsViewModel.reportedIds.collectAsState()
    val cardTranslations by newsViewModel.cardTranslations.collectAsState()
    val trendingRanks    by newsViewModel.trendingRanks.collectAsState()
    val theme            = LocalAppTheme.current
    val strings          = theme.strings
    val appLang          = if (theme.language == AppLanguage.TURKISH) "tr" else "en"
    val snackbarHost     = remember { SnackbarHostState() }
    val initialReported  = remember { reportedIds.size }

    // Haberler yalnızca ilk açılışta (init) veya pull-to-refresh ile yüklenir.
    // ON_RESUME / tab geçişi tetiklemez — kullanıcı her sekmeye geçtiğinde beklemez.

    // When language changes: flush stale translated titles from the old language.
    LaunchedEffect(appLang) {
        newsViewModel.onLanguageChanged()
    }

    // When articles load (or language changes): start translating card titles.
    // onLanguageChanged above already cleared the cache, so this picks it up cleanly.
    LaunchedEffect(uiState.articles, appLang) {
        newsViewModel.startCardTranslations(uiState.articles, appLang)
    }



    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()

        // NewsFeed her zaman composition'da — listState hiç destroy olmaz
        NewsFeed(
            uiState          = uiState,
            savedIds         = savedIds,
            reportedIds      = reportedIds,
            cardTranslations = cardTranslations,
            trendingRanks    = trendingRanks,
            onArticleClick   = { newsViewModel.openArticle(it, appLang) },
            onRefresh        = { newsViewModel.refresh() },
            onLoadMore       = { newsViewModel.loadMore() },
            onSave           = { newsViewModel.toggleSave(it) }
        )

        // Detail ekranı üzerine slide-in olarak biniyor
        AnimatedVisibility(
            visible = detailState != null,
            enter = slideInHorizontally { it } + fadeIn(),
            exit  = slideOutHorizontally { it } + fadeOut()
        ) {
            detailState?.let { detail ->
                MuseReader(
                    detailState = detail,
                    isSaved     = detail.article.id in savedIds,
                    onBack      = { newsViewModel.closeArticle() },
                    onSave      = { newsViewModel.toggleSave(detail.article.id) },
                    onReport    = {
                        newsViewModel.reportArticle(detail.article.id)
                        snackbarHost.currentSnackbarData?.dismiss()
                    }
                )
            }
        }

        // Snackbar shown after a report action
        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp),
            snackbar  = { data ->
                    Snackbar(
                            snackbarData    = data,
                            containerColor  = theme.bg2,
                            contentColor    = theme.text0,
                            shape           = RoundedCornerShape(14.dp)
                        )
            }
        )
    }

    // Show snackbar only when a new report is added during this session
    LaunchedEffect(reportedIds.size) {
        if (reportedIds.size > initialReported) {
            snackbarHost.showSnackbar(
                message  = strings.reportSuccessMsg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // After a manual pull-to-refresh, show how many new articles arrived (or "up to date")
    LaunchedEffect(uiState.manualRefreshAt) {
        if (uiState.manualRefreshAt == 0L) return@LaunchedEffect
        val msg = if (uiState.newArticleCount > 0)
            "${uiState.newArticleCount} ${strings.newArticlesMsg}"
        else
            strings.noNewArticlesMsg
        snackbarHost.showSnackbar(message = msg, duration = SnackbarDuration.Short)
    }
}

// ── Feed ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsFeed(
    uiState: NewsUiState,
    savedIds: Set<String>,
    reportedIds: Set<String>,
    cardTranslations: Map<String, String>,
    trendingRanks: Map<String, Int>,
    onArticleClick: (Article) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSave: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("TÜMÜ") }
    val theme   = LocalAppTheme.current
    val strings = theme.strings
    val accent  = MaterialTheme.colorScheme.primary
    val listState = rememberLazyListState()

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) onRefresh()
    }
    // Stop the indicator when loading finishes.
    // uiState.lastUpdated is included so that if isLoading was already false
    // before the swipe (race condition), the indicator still stops on completion.
    val isLoading = uiState.isLoading
    LaunchedEffect(isLoading, uiState.lastUpdated) {
        if (!isLoading) isRefreshing = false
    }

    // Filtered article list: exclude reported, apply category filter, sort newest first
    val filteredArticles = remember(selectedCategory, uiState.articles, savedIds, reportedIds) {
        val nonReported = uiState.articles
            .filter { it.id !in reportedIds }
            .sortedByDescending { it.publishedAtMs }
        when (selectedCategory) {
            "TÜMÜ"          -> nonReported
            "KAYDEDILENLER" -> nonReported.filter { it.id in savedIds }
            else            -> nonReported.filter { it.category == selectedCategory }
        }
    }

    // Pagination: reset visible count when category or article list changes
    var visibleCount by remember(selectedCategory, uiState.lastUpdated) {
        mutableIntStateOf(PAGE_SIZE)
    }
    val displayArticles = remember(filteredArticles, visibleCount) {
        filteredArticles.take(visibleCount)
    }
    val hasMore = filteredArticles.size > visibleCount

    // Infinite scroll trigger: load more when near the bottom
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 4
        }
    }
    LaunchedEffect(isNearBottom) {
        if (!isNearBottom || uiState.isLoading) return@LaunchedEffect
        if (hasMore) {
            // More local articles available — reveal next page instantly (no network)
            visibleCount += PAGE_SIZE
        }
        // Network loadMore removed: haberleri otomatik çekmiyoruz.
        // Kullanıcı pull-to-refresh yapınca taze veri gelir.
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    "EDITORIAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.ExtraLight
                )
                Text(
                    "MUSE",
                    style = MaterialTheme.typography.displayLarge,
                    color = theme.text0,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // ── Hero Carousel ─────────────────────────────────────────────────────
        item {
            val featured = uiState.articles.filter { it.isFeatured }.take(6)
            if (uiState.isLoading) {
                CarouselSkeleton()
            } else if (featured.isNotEmpty()) {
                MuseAutoCarousel(
                    articles         = featured,
                    savedIds         = savedIds,
                    cardTranslations = cardTranslations,
                    trendingRanks    = trendingRanks,
                    onArticleClick   = onArticleClick,
                    onSave           = onSave
                )
            }
        }

        // ── Live indicator ─────────────────────────────────────────────────────
        if (!uiState.isLoading && uiState.articles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveDot()
                    Text(
                        "${strings.liveLabel} • ${uiState.articles.size} ${strings.newsCountLabel}",
                        color = theme.text2,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Categories ─────────────────────────────────────────────────────────
        item {
            MuseCategoryBar(selected = selectedCategory, onSelect = { selectedCategory = it }, strings = strings)
        }

        // ── Section label ──────────────────────────────────────────────────────
        item {
            Text(
                text = if (selectedCategory == "TÜMÜ") strings.allNewsLabel
                       else strings.localizedNewsCategory(selectedCategory),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                color = theme.text2,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ── Empty states ───────────────────────────────────────────────────────
        if (!uiState.isLoading && filteredArticles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (selectedCategory == "KAYDEDILENLER") Icons.Rounded.BookmarkBorder
                            else Icons.Rounded.SearchOff,
                            null,
                            tint = theme.text2,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (selectedCategory == "KAYDEDILENLER") strings.noSavedNews
                            else strings.noCategoryNews,
                            color = theme.text2,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ── Article feed ───────────────────────────────────────────────────────
        itemsIndexed(displayArticles) { index, article ->
            MuseArticleCard(
                article      = article,
                displayTitle = cardTranslations[article.id] ?: article.title,
                isReversed   = index % 2 != 0,
                isSaved      = article.id in savedIds,
                onClick      = { onArticleClick(article) },
                onSave       = { onSave(article.id) }
            )
        }

        // ── Load more indicator ────────────────────────────────────────────────
        // Show spinner when paging through local articles OR fetching from network
        if (!uiState.isLoading && (hasMore || uiState.isLoadingMore)) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = accent,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }

    } // end PullToRefreshBox
}

// ── Auto-Scrolling Carousel ───────────────────────────────────────────────────

@Composable
private fun MuseAutoCarousel(
    articles: List<Article>,
    savedIds: Set<String>,
    cardTranslations: Map<String, String>,
    trendingRanks: Map<String, Int>,
    onArticleClick: (Article) -> Unit,
    onSave: (String) -> Unit
) {
    val pagerState = rememberPagerState { articles.size }

    // Auto-scroll: stable loop — LaunchedEffect(Unit) prevents restart on each page change
    LaunchedEffect(Unit) {
        while (articles.size > 1) {
            delay(4_000L)
            val next = (pagerState.currentPage + 1) % articles.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(440.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 16.dp
        ) { page ->
            val art = articles[page]
            MuseHeroCard(
                article       = art,
                displayTitle  = cardTranslations[art.id] ?: art.title,
                isSaved       = art.id in savedIds,
                trendingRank  = trendingRanks[art.id],
                onClick       = { onArticleClick(art) },
                onSave        = { onSave(art.id) }
            )
        }
        Spacer(Modifier.height(14.dp))
        PagerDotIndicator(pagerState = pagerState, count = articles.size)
    }
}

@Composable
private fun MuseHeroCard(
    article: Article,
    displayTitle: String,
    isSaved: Boolean,
    trendingRank: Int?,        // null = no clicks yet (new session)
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    // Rank-based accent colours: gold → silver → bronze → accent for the rest
    val rankColor = when (trendingRank) {
        1    -> Color(0xFFFFD700)
        2    -> Color(0xFFB0BEC5)
        3    -> Color(0xFFCD7F32)
        else -> accent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = article.image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark gradient overlay
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.4f to Color.Transparent,
                    1f to Color.Black.copy(0.88f)
                )
            )
        )
        // Top-left: TRENDING badge (when rank exists) OR category badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        ) {
            if (trendingRank != null) {
                // TRENDING #N badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rankColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        "TRENDING #$trendingRank",
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }
            } else {
                // No clicks yet — show category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(article.category, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
            }
        }

        // Top-right: category chip (when trending badge is shown) + source
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (trendingRank != null) {
                // Category chip — distinct look: accent border + tag icon
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(0.60f))
                        .border(1.dp, rankColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Tag,
                        contentDescription = null,
                        tint = rankColor,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        article.category,
                        color = Snow,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            if (article.sourceName.isNotBlank()) {
                // Source chip — subdued, clearly different from category
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(0.40f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Newspaper,
                        contentDescription = null,
                        tint = Snow.copy(0.5f),
                        modifier = Modifier.size(8.dp)
                    )
                    Text(
                        article.sourceName.take(18),
                        color = Snow.copy(0.6f),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Normal,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
        // Bottom: title + meta row, full width with right padding for action buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 72.dp, bottom = 20.dp)
        ) {
            Text(
                displayTitle.uppercase(),
                color = Snow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 23.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Schedule, null, tint = Snow.copy(0.5f), modifier = Modifier.size(10.dp))
                val heroStrings = LocalAppTheme.current.strings
                Text("${article.readTime} ${heroStrings.readTimeUnit} ${heroStrings.readingLabel}", color = Snow.copy(0.5f), fontSize = 8.5.sp, letterSpacing = 1.sp)
                if (article.publishedAt.isNotBlank()) {
                    Text("•", color = Snow.copy(0.3f), fontSize = 8.5.sp)
                    Text(formatDate(article.publishedAt), color = Snow.copy(0.5f), fontSize = 8.5.sp, letterSpacing = 0.8.sp)
                }
            }
        }
        // Bottom-right action buttons — stacked vertically, compact
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Share button
            if (article.sourceUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.55f))
                        .clickable(onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.sourceUrl}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            } catch (_: Exception) {}
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Share, null, tint = Snow, modifier = Modifier.size(16.dp))
                }
            }
            // Bookmark button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSaved) accent.copy(0.25f) else Color.Black.copy(0.55f))
                    .clickable(onClick = onSave),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    null,
                    tint = if (isSaved) accent else Snow,
                    modifier = Modifier.size(16.dp)
                )
            }
            // Open source URL button
            if (article.sourceUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.55f))
                        .clickable(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl)))
                            } catch (_: Exception) {}
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, tint = Snow, modifier = Modifier.size(16.dp))
                }
            }
            // Read article button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Pager Dot Indicator ───────────────────────────────────────────────────────

@Composable
private fun PagerDotIndicator(pagerState: PagerState, count: Int) {
    val accent = MaterialTheme.colorScheme.primary
    val theme  = LocalAppTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val isSelected = pagerState.currentPage == i
            val width by animateDpAsState(if (isSelected) 20.dp else 6.dp, label = "dot_w")
            val color by animateColorAsState(
                if (isSelected) accent else theme.text2.copy(0.40f), label = "dot_c"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// ── Category Bar — Nav-bar pill style ─────────────────────────────────────────

@Composable
private fun MuseCategoryBar(
    selected: String,
    onSelect: (String) -> Unit,
    strings: AppStrings
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    LazyRow(
        modifier = Modifier.padding(vertical = 20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CATEGORY_KEYS) { cat ->
            val isSelected = cat == selected
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val chipScale by animateFloatAsState(
                targetValue = if (isPressed) 0.90f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "cat_scale"
            )
            val bgColor by animateColorAsState(
                if (isSelected) accent else theme.bg2.copy(alpha = 0.80f),
                label = "cat_bg"
            )
            val textColor by animateColorAsState(
                if (isSelected) Color.Black else theme.text2,
                label = "cat_text"
            )

            Row(
                modifier = Modifier
                    .scale(chipScale)
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .then(
                        if (!isSelected) Modifier.border(0.5.dp, theme.stroke.copy(alpha = 0.55f), RoundedCornerShape(50))
                        else Modifier
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (cat == "KAYDEDILENLER") {
                    Icon(
                        if (isSelected) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    strings.localizedNewsCategory(cat),
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ── Article Card — Glass effect matching nav-bar theme ────────────────────────

@Composable
private fun MuseArticleCard(
    article: Article,
    displayTitle: String,
    isReversed: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    val borderBrush = Brush.horizontalGradient(
        listOf(accent.copy(alpha = 0.28f), theme.stroke.copy(alpha = 0.06f), accent.copy(alpha = 0.12f))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                spotColor = accent.copy(alpha = 0.18f),
                ambientColor = Color.Black.copy(alpha = 0.40f)
            )
            .clip(RoundedCornerShape(20.dp))
            .drawWithCache {
                val bgBase = theme.bg1
                val accentBleed = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to accent.copy(alpha = 0.10f),
                        0.40f to accent.copy(alpha = 0.03f),
                        1.00f to Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.75f, size.height)
                )
                val depthShadow = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.55f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.20f)
                    )
                )
                onDrawBehind {
                    drawRect(bgBase)
                    drawRect(accentBleed)
                    drawRect(depthShadow)
                }
            }
            .border(0.5.dp, borderBrush, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.height(130.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isReversed) {
                AsyncImage(
                    model = article.image,
                    contentDescription = null,
                    modifier = Modifier.width(110.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
                    contentScale = ContentScale.Crop
                )
                ArticleCardText(article, displayTitle, accent, theme, Modifier.weight(1f).padding(start = 14.dp, end = 36.dp, top = 14.dp, bottom = 14.dp), TextAlign.Start)
            } else {
                ArticleCardText(article, displayTitle, accent, theme, Modifier.weight(1f).padding(start = 36.dp, end = 14.dp, top = 14.dp, bottom = 14.dp), TextAlign.End)
                AsyncImage(
                    model = article.image,
                    contentDescription = null,
                    modifier = Modifier.width(110.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Bookmark button overlaid top corner
        val saveInteractionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(if (!isReversed) Alignment.TopEnd else Alignment.TopStart)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(if (theme.isDark) Color.Black.copy(alpha = 0.48f) else theme.bg3.copy(alpha = 0.80f))
                .clickable(
                    interactionSource = saveInteractionSource,
                    indication = null,
                    onClick = onSave
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = if (isSaved) LocalAppTheme.current.strings.unsaveArticle else LocalAppTheme.current.strings.saveArticle,
                tint = if (isSaved) accent else theme.text1,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ArticleCardText(
    article: Article,
    displayTitle: String,
    accent: Color,
    theme: AppThemeState,
    modifier: Modifier,
    align: TextAlign
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        // Category + source
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (align == TextAlign.End) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(article.category, color = accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            if (article.sourceName.isNotBlank()) {
                Text("  ·  ${article.sourceName.take(12)}", color = theme.text2, fontSize = 8.sp, fontWeight = FontWeight.Normal)
            }
        }
        Spacer(Modifier.height(6.dp))
        // Title
        Text(
            displayTitle,
            color = theme.text0,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.height(8.dp))
        // Footer — okuma süresi + tarih
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (align == TextAlign.End) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Schedule, null, tint = theme.text2, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(4.dp))
            Text("${article.readTime} ${LocalAppTheme.current.strings.readTimeUnit}", color = theme.text2, fontSize = 9.sp)
            val dateStr = formatDate(article.publishedAt)
            if (dateStr.isNotBlank()) {
                Text("  ·  $dateStr", color = theme.text2, fontSize = 9.sp)
            }
        }
    }
}

// ── Loading Skeleton ──────────────────────────────────────────────────────────

@Composable
private fun CarouselSkeleton() {
    val theme = LocalAppTheme.current
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(440.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(theme.bg2.copy(shimmer))
    )
    Spacer(Modifier.height(20.dp))
}

// ── Live Dot ──────────────────────────────────────────────────────────────────

@Composable
private fun LiveDot() {
    val accent = MaterialTheme.colorScheme.primary
    val scale by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "live_scale"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(accent.copy(scale * 0.8f))
    )
}

// ── Article Reader ────────────────────────────────────────────────────────────

@Composable
private fun MuseReader(
    detailState: ArticleDetailState,
    isSaved: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onReport: () -> Unit
) {
    val article     = detailState.article
    val context     = LocalContext.current
    val scrollState = rememberScrollState()
    val theme       = LocalAppTheme.current
    val strings     = theme.strings
    val accent      = MaterialTheme.colorScheme.primary
    var showReportDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        Column(modifier = Modifier.verticalScroll(scrollState).navigationBarsPadding()) {

            // ── Hero image ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                AsyncImage(
                    model = article.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(0.35f),
                            0.5f to Color.Transparent,
                            1f to Color.Black
                        )
                    )
                )
                // Title + category + meta at bottom of hero
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    // Category + source row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accent)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                article.category,
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        if (article.sourceName.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Article,
                                    contentDescription = null,
                                    tint = Snow.copy(0.75f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    article.sourceName,
                                    color = Snow.copy(0.9f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        detailState.displayTitle.uppercase(),
                        color = Snow,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, null, tint = Snow.copy(0.5f), modifier = Modifier.size(12.dp))
                        Text("${article.readTime} ${strings.readTimeUnit} ${strings.readingLabel}", color = Snow.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                        if (article.publishedAt.isNotBlank()) {
                            Text("•", color = Snow.copy(0.3f), fontSize = 9.sp)
                            Text(formatDate(article.publishedAt), color = Snow.copy(0.5f), fontSize = 9.sp)
                        }
                    }
                }
            }

            // ── AI Summary box ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {

                if (detailState.isTranslating || detailState.isLoadingContent) {
                    // Translation or full-content fetch in progress — show centred spinner
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accent, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (detailState.isTranslating) strings.translatingLabel
                                else strings.loadingContentLabel,
                                color = theme.text2,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else {
                // Summary card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.bg2)
                        .border(1.dp, accent.copy(0.25f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.dp))
                            Text(strings.aiSummaryLabel, color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        val summaryText = detailState.displaySummary.ifBlank { strings.noSummaryLabel }
                        val summaryParagraphs = summaryText
                            .split("\n\n")
                            .map { it.replace("\n", " ").trim() }
                            .filter { it.isNotBlank() }
                         summaryParagraphs.forEachIndexed { idx, para ->
                                Text(
                                    para,
                                    color      = theme.text1,
                                    fontSize   = 15.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Light,
                                    fontStyle  = FontStyle.Italic
                                )
                            if (idx < summaryParagraphs.lastIndex) Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Full content — rich HTML render when available, plain-text fallback
                if (detailState.displayContent.isNotBlank()) {
                    Text(
                        strings.contentLabel,
                        color         = theme.text2,
                        fontSize      = 9.sp,
                        letterSpacing = 3.sp,
                        fontWeight    = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    if (detailState.displayContentHtml.isNotBlank()) {
                        // Rich render: parse HTML → structured blocks
                        val blocks = parseHtmlToBlocks(detailState.displayContentHtml)
                        blocks.forEachIndexed { idx, block ->
                            when (block) {
                                is HtmlBlock.Heading -> {
                                    if (idx > 0) Spacer(Modifier.height(24.dp))
                                    Text(
                                        block.text,
                                        color      = theme.text0,
                                        fontSize   = if (block.level <= 2) 19.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = if (block.level <= 2) 26.sp else 24.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                is HtmlBlock.Paragraph -> {
                                    if (block.annotated.isNotEmpty()) {
                                        Text(
                                            block.annotated,
                                            color      = theme.text1,
                                            fontSize   = 16.sp,
                                            lineHeight = 27.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                        if (idx < blocks.lastIndex) Spacer(Modifier.height(14.dp))
                                    }
                                }
                                is HtmlBlock.ListItem -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            "•",
                                            color      = theme.text2,
                                            fontSize   = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier   = Modifier.padding(top = 1.dp)
                                        )
                                        Text(
                                            block.annotated,
                                            color      = theme.text1,
                                            fontSize   = 16.sp,
                                            lineHeight = 26.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                    if (idx < blocks.lastIndex) Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    } else {
                        // Plain-text fallback (translated content)
                        val paragraphs = detailState.displayContent
                            .split("\n\n")
                            .map { it.replace("\n", " ").trim() }
                            .filter { it.isNotBlank() }
                        paragraphs.forEachIndexed { idx, para ->
                            Text(
                                para,
                                color      = theme.text1,
                                fontSize   = 16.sp,
                                lineHeight = 27.sp,
                                fontWeight = FontWeight.Light
                            )
                            if (idx < paragraphs.lastIndex) Spacer(Modifier.height(14.dp))
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
                } // end isTranslating else

                // ── Original article button ────────────────────────────────────
                if (article.sourceUrl.isNotBlank()) {
                    Button(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl))
                                )
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            strings.goToOriginal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Source info ────────────────────────────────────────────────
                if (article.sourceName.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Article, null, tint = theme.text2, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${strings.sourceLabel}: ${article.sourceName}",
                            color = theme.text2,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }

        // ── Top navigation bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navBtnBg = if (theme.isDark) Color.Black.copy(0.55f) else theme.bg2.copy(0.90f)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(navBtnBg)
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = theme.text0, modifier = Modifier.size(20.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bookmark button
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSaved) accent.copy(0.25f) else navBtnBg)
                ) {
                    Icon(
                        if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null,
                        tint = if (isSaved) accent else theme.text0,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Share button
                if (article.sourceUrl.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.sourceUrl}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(navBtnBg)
                    ) {
                        Icon(Icons.Rounded.Share, null, tint = theme.text0, modifier = Modifier.size(20.dp))
                    }
                }

                // Report button
                IconButton(
                    onClick = { showReportDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(navBtnBg)
                ) {
                    Icon(Icons.Rounded.Flag, null, tint = theme.text1, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Report dialog
        if (showReportDialog) {
            ReportDialog(
                strings  = strings,
                onDismiss = { showReportDialog = false },
                onConfirm = {
                    showReportDialog = false
                    onReport()
                }
            )
        }
    }
}

// ── Report Dialog ─────────────────────────────────────────────────────────────

@Composable
private fun ReportDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val theme  = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    var selectedReason by remember { mutableStateOf<String?>(null) }

    val reasons = listOf(
        strings.reportReasonMisinfo,
        strings.reportReasonSpam,
        strings.reportReasonInappropriate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor  = theme.bg1,
        shape           = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Flag, null, tint = accent, modifier = Modifier.size(18.dp))
                Text(strings.reportDialogTitle, color = theme.text0, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                reasons.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accent.copy(0.15f) else theme.bg2)
                            .border(1.dp, if (isSelected) accent.copy(0.5f) else theme.stroke.copy(0.15f), RoundedCornerShape(12.dp))
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accent else Color.Transparent)
                                .border(1.5.dp, if (isSelected) accent else theme.text2.copy(0.4f), CircleShape)
                        )
                         Text(reason, color = if (isSelected) theme.text0 else theme.text2, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = selectedReason != null,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = accent,
                    contentColor           = Color.Black,
                    disabledContainerColor = theme.bg2,
                    disabledContentColor   = theme.text2
                )
            ) {
                Text(strings.reportConfirm, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.reportCancel, color = theme.text2, fontSize = 13.sp)
            }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDate(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
        when {
            // ISO-8601: "2026-03-04T12:18:34+03:00" → "2026-03-04"
            raw.contains("T") -> raw.substringBefore("T")
            // RFC 822: "Wed, 04 Mar 2026 22:06:35 +0000" → "04 Mar 2026"
            raw.contains(",") -> raw.substringAfter(",").trim().take(11).trim()
            else -> raw.take(10)
        }
    } catch (_: Exception) { "" }
}

// ── Rich HTML renderer ────────────────────────────────────────────────────────

/** A parsed block from an HTML article body. */
private sealed interface HtmlBlock {
    data class Heading(val level: Int, val text: String) : HtmlBlock
    data class Paragraph(val annotated: AnnotatedString) : HtmlBlock
    data class ListItem(val annotated: AnnotatedString) : HtmlBlock
}

/**
 * Converts a raw HTML string into a flat list of [HtmlBlock]s.
 *
 * Supported tags:
 *  - <h1>…<h6>     → Heading block
 *  - <p>, <div>    → Paragraph block
 *  - <li>          → ListItem block
 *  - <strong>, <b> → bold span
 *  - <em>, <i>     → italic span
 *  - <br>          → newline inside span
 *  Everything else is stripped.
 */
private fun parseHtmlToBlocks(html: String): List<HtmlBlock> {
    if (html.isBlank()) return emptyList()

    val blocks = mutableListOf<HtmlBlock>()

    // ── 1. Split into raw block-level chunks ─────────────────────────────────
    // We tokenise by block-level open/close tags. Each token is either a tag
    // or the text between tags.
    val blockTagPattern = Regex(
        """<(/?)(?:p|div|h[1-6]|li|ul|ol|blockquote|section|article|header|footer|figure|figcaption)(?:\s[^>]*)?>""",
        RegexOption.IGNORE_CASE
    )

    data class Chunk(val tag: String, val level: Int, val isClose: Boolean, val raw: String)

    // Walk through splitting at block boundaries
    var cursor = 0
    val tagMatches = blockTagPattern.findAll(html).toList()

    // collect (segment_text, tag_that_follows) pairs
    data class Segment(val inner: String, val tagName: String, val tagLevel: Int, val isClose: Boolean)
    val segments = mutableListOf<Segment>()

    for (match in tagMatches) {
        val before = html.substring(cursor, match.range.first)
        if (before.isNotBlank()) {
            segments.add(Segment(before, "", 0, false))
        }
        val full = match.value
        val isClose = full.startsWith("</")
        val tagName = Regex("[a-zA-Z][a-zA-Z0-9]*").find(full.removePrefix("</").removePrefix("<"))
            ?.value?.lowercase() ?: ""
        val level = if (tagName.matches(Regex("h[1-6]"))) tagName[1].digitToInt() else 0
        segments.add(Segment("", tagName, level, isClose))
        cursor = match.range.last + 1
    }
    // Trailing text after last tag
    if (cursor < html.length) {
        val tail = html.substring(cursor)
        if (tail.isNotBlank()) segments.add(Segment(tail, "", 0, false))
    }

    // ── 2. Accumulate inner HTML per logical block ────────────────────────────
    var currentTag = "p"
    var currentLevel = 0
    val buffer = StringBuilder()

    fun flush() {
        val raw = buffer.toString().trim()
        buffer.clear()
        if (raw.isBlank()) return
        when {
            currentTag.matches(Regex("h[1-6]")) -> {
                val text = stripInlineTags(raw)
                if (text.isNotBlank()) blocks.add(HtmlBlock.Heading(currentLevel, text))
            }
            currentTag == "li" -> {
                val ann = inlineAnnotatedString(raw)
                if (ann.isNotEmpty()) blocks.add(HtmlBlock.ListItem(ann))
            }
            else -> {
                val ann = inlineAnnotatedString(raw)
                if (ann.isNotEmpty()) blocks.add(HtmlBlock.Paragraph(ann))
            }
        }
    }

    for (seg in segments) {
        if (seg.tagName.isEmpty()) {
            // Plain text segment
            buffer.append(seg.inner)
        } else if (seg.isClose) {
            // Closing tag — flush the accumulated buffer
            flush()
            currentTag = "p"
            currentLevel = 0
        } else {
            // Opening tag — flush previous block then start new one
            flush()
            currentTag = seg.tagName
            currentLevel = seg.tagLevel
        }
    }
    flush()

    return blocks
}

/** Strips all tags and entities from an inline HTML string (for headings). */
private fun stripInlineTags(html: String): String = html
    .replace(Regex("<[^>]*>"), "")
    .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    .replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"")
    .replace("&hellip;", "…").replace("&mdash;", "—").replace("&ndash;", "–")
    .replace(Regex("\\s+"), " ").trim()

/**
 * Converts inline HTML (bold, italic, br) into a Compose [AnnotatedString].
 * Supports <strong>, <b>, <em>, <i>, <br> and strips unknown tags.
 */
private fun inlineAnnotatedString(html: String): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")

    val cleaned = html
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"")
        .replace("&hellip;", "…").replace("&mdash;", "—").replace("&ndash;", "–")
        .replace("&apos;", "'")

    return buildAnnotatedString {
        val tokenPattern = Regex("""<(/?)(\w+)[^>]*>|([^<]+)""")
        val boldTags  = setOf("strong", "b")
        val italicTags = setOf("em", "i")
        var boldDepth   = 0
        var italicDepth = 0

        for (tok in tokenPattern.findAll(cleaned)) {
            val closingSlash = tok.groupValues[1]
            val tagName      = tok.groupValues[2].lowercase()
            val text         = tok.groupValues[3]

            when {
                text.isNotEmpty() -> append(text)
                tagName == "br"   -> append("\n")
                tagName in boldTags -> {
                    if (closingSlash.isEmpty()) boldDepth++ else boldDepth = maxOf(0, boldDepth - 1)
                }
                tagName in italicTags -> {
                    if (closingSlash.isEmpty()) italicDepth++ else italicDepth = maxOf(0, italicDepth - 1)
                }
                else -> { /* skip unknown tags */ }
            }

            // Re-apply span styles after every token so nesting works correctly
            if (boldDepth > 0 || italicDepth > 0) {
                val fw = if (boldDepth > 0) FontWeight.Bold else null
                val fs = if (italicDepth > 0) FontStyle.Italic else null
                // We can't retroactively apply spans; instead we push a zero-width span
                // so the *next* text segment gets the right style.
                // The approach: rebuild with a simpler two-pass tokeniser below.
            }
        }
    }.let {
        // Two-pass: simpler and more reliable than tracking depth above.
        buildInlineAnnotatedString(cleaned)
    }
}

/** Clean two-pass inline parser used by [inlineAnnotatedString]. */
private fun buildInlineAnnotatedString(html: String): AnnotatedString {
    data class Run(val text: String, val bold: Boolean, val italic: Boolean)

    val runs = mutableListOf<Run>()
    val tokenPattern = Regex("""<(/?)(\w+)[^>]*>|([^<]+)""")
    val boldTags   = setOf("strong", "b")
    val italicTags = setOf("em", "i")
    var bold   = false
    var italic = false

    for (tok in tokenPattern.findAll(html)) {
        val isClose = tok.groupValues[1] == "/"
        val tag     = tok.groupValues[2].lowercase()
        val text    = tok.groupValues[3]
        when {
            text.isNotEmpty() -> runs.add(Run(text, bold, italic))
            tag == "br"       -> runs.add(Run("\n", bold, italic))
            tag in boldTags   -> bold   = !isClose
            tag in italicTags -> italic = !isClose
        }
    }

    return buildAnnotatedString {
        for (run in runs) {
            val spanStyle = SpanStyle(
                fontWeight = if (run.bold) FontWeight.Bold else FontWeight.Light,
                fontStyle  = if (run.italic) FontStyle.Italic else FontStyle.Normal
            )
            withStyle(spanStyle) { append(run.text) }
        }
    }
}

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.avonix.profitness.core.theme.*
import kotlinx.coroutines.delay
import androidx.compose.ui.input.nestedscroll.nestedScroll

// ── Category internal keys (language-invariant, must match Article.category values) ──

private val CATEGORY_KEYS = listOf(
    "TÜMÜ", "KAYDEDILENLER", "BİLİM", "BESLENME", "ANTRENMAN",
    "SPOR", "ZİHİN", "YAŞAM", "TOPARLANMA", "TEKNOLOJİ"
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
    val theme            = LocalAppTheme.current
    val strings          = theme.strings
    val appLang          = if (theme.language == AppLanguage.TURKISH) "tr" else "en"
    val snackbarHost     = remember { SnackbarHostState() }
    val initialReported  = remember { reportedIds.size }

    // Trigger card-level title translations as soon as articles are ready.
    // Featured articles are prioritised inside startCardTranslations.
    LaunchedEffect(uiState.articles, appLang) {
        newsViewModel.startCardTranslations(uiState.articles, appLang)
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        AnimatedContent(
            targetState = detailState,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "news_detail"
        ) { detail ->
            if (detail != null) {
                MuseReader(
                    detailState = detail,
                    isSaved = detail.article.id in savedIds,
                    onBack = { newsViewModel.closeArticle() },
                    onSave = { newsViewModel.toggleSave(detail.article.id) },
                    onReport = {
                        newsViewModel.reportArticle(detail.article.id)
                        snackbarHost.currentSnackbarData?.dismiss()
                    }
                )
            } else {
                NewsFeed(
                    uiState          = uiState,
                    savedIds         = savedIds,
                    reportedIds      = reportedIds,
                    cardTranslations = cardTranslations,
                    onArticleClick   = { newsViewModel.openArticle(it, appLang) },
                    onRefresh        = { newsViewModel.refresh() },
                    onLoadMore       = { newsViewModel.loadMore() },
                    onSave           = { newsViewModel.toggleSave(it) }
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
                    contentColor    = Snow,
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
}

// ── Feed ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsFeed(
    uiState: NewsUiState,
    savedIds: Set<String>,
    reportedIds: Set<String>,
    cardTranslations: Map<String, String>,
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

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) { onRefresh() }
    }
    // Stop the indicator once loading finishes
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) pullRefreshState.endRefresh()
    }

    // Filtered article list: exclude reported, then apply category filter
    val filteredArticles = remember(selectedCategory, uiState.articles, savedIds, reportedIds) {
        val nonReported = uiState.articles.filter { it.id !in reportedIds }
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
            // More local articles available — just reveal next page instantly
            visibleCount += PAGE_SIZE
        } else if (uiState.canLoadMore && !uiState.isLoadingMore) {
            // Local list exhausted — fetch new articles from network
            onLoadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
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
                    color = Snow,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // ── Hero Carousel ──────────────────────────────────────────────────────
        item {
            val featured = uiState.articles.filter { it.isFeatured }.take(6)
            if (uiState.isLoading) {
                CarouselSkeleton()
            } else if (featured.isNotEmpty()) {
                MuseAutoCarousel(
                    articles         = featured,
                    savedIds         = savedIds,
                    cardTranslations = cardTranslations,
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
                            tint = Snow.copy(0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (selectedCategory == "KAYDEDILENLER") strings.noSavedNews
                            else strings.noCategoryNews,
                            color = Snow.copy(0.4f),
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

    PullToRefreshContainer(
        state = pullRefreshState,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding(),
        containerColor = theme.bg2,
        contentColor = accent
    )
    } // end Box
}

// ── Auto-Scrolling Carousel ───────────────────────────────────────────────────

@Composable
private fun MuseAutoCarousel(
    articles: List<Article>,
    savedIds: Set<String>,
    cardTranslations: Map<String, String>,
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
            MuseHeroCard(
                article      = articles[page],
                displayTitle = cardTranslations[articles[page].id] ?: articles[page].title,
                isSaved      = articles[page].id in savedIds,
                onClick      = { onArticleClick(articles[page]) },
                onSave       = { onSave(articles[page].id) }
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
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
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
        // Top category badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(article.category, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        }
        // Source badge top-right
        if (article.sourceName.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(article.sourceName.take(15), color = Snow.copy(0.8f), fontSize = 8.sp, fontWeight = FontWeight.Medium)
            }
        }
        // Bottom content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 100.dp, bottom = 24.dp)
        ) {
            Text(
                displayTitle.uppercase(),
                color = Snow,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 27.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Schedule, null, tint = Snow.copy(0.5f), modifier = Modifier.size(12.dp))
                val heroStrings = LocalAppTheme.current.strings
                Text("${article.readTime} ${heroStrings.readingLabel}", color = Snow.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                if (article.publishedAt.isNotBlank()) {
                    Text("•", color = Snow.copy(0.3f), fontSize = 9.sp)
                    Text(formatDate(article.publishedAt), color = Snow.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
        }
        // Bottom-right action buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val isSelected = pagerState.currentPage == i
            val width by animateDpAsState(if (isSelected) 20.dp else 6.dp, label = "dot_w")
            val color by animateColorAsState(
                if (isSelected) accent else Snow.copy(0.25f), label = "dot_c"
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
                .background(Color.Black.copy(alpha = 0.48f))
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
                tint = if (isSaved) accent else Snow.copy(0.70f),
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
            color = Snow,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.height(8.dp))
        // Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (align == TextAlign.End) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Schedule, null, tint = Snow.copy(0.35f), modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(4.dp))
            Text(article.readTime, color = Snow.copy(0.35f), fontSize = 9.sp)
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
                // Category + source badges at top
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(72.dp, 16.dp, 16.dp, 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(article.category, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    if (article.sourceName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(0.5f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(article.sourceName, color = Snow.copy(0.85f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                // Title at bottom of hero
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
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
                        Text("${article.readTime} ${strings.readingLabel}", color = Snow.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                        if (article.publishedAt.isNotBlank()) {
                            Text("•", color = Snow.copy(0.3f), fontSize = 9.sp)
                            Text(formatDate(article.publishedAt), color = Snow.copy(0.5f), fontSize = 9.sp)
                        }
                    }
                }
            }

            // ── AI Summary box ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {

                if (detailState.isTranslating) {
                    // Translation in progress — show centred spinner
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accent, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Türkçeye çevriliyor…",
                                color = Snow.copy(0.5f),
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
                        Text(
                            detailState.displaySummary.ifBlank { strings.noSummaryLabel },
                            color = Snow.copy(0.85f),
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Light,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Full content
                if (detailState.displayContent.isNotBlank() && detailState.displayContent != article.summary) {
                    Text(strings.contentLabel, color = theme.text2, fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        detailState.displayContent,
                        color = Snow.copy(0.75f),
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Light
                    )
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
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.55f))
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Snow, modifier = Modifier.size(20.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bookmark button
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSaved) accent.copy(0.25f) else Color.Black.copy(0.55f))
                ) {
                    Icon(
                        if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null,
                        tint = if (isSaved) accent else Snow,
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
                            .background(Color.Black.copy(0.55f))
                    ) {
                        Icon(Icons.Rounded.Share, null, tint = Snow, modifier = Modifier.size(20.dp))
                    }
                }

                // Report button
                IconButton(
                    onClick = { showReportDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.55f))
                ) {
                    Icon(Icons.Rounded.Flag, null, tint = Snow.copy(0.70f), modifier = Modifier.size(20.dp))
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
                Text(strings.reportDialogTitle, color = Snow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Text(reason, color = if (isSelected) Snow else theme.text2, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
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
    return try {
        when {
            raw.contains("T") -> raw.substringBefore("T")
            raw.length >= 16  -> raw.take(16).substringAfterLast(",").trim()
            else               -> raw.take(16)
        }
    } catch (_: Exception) { raw.take(16) }
}

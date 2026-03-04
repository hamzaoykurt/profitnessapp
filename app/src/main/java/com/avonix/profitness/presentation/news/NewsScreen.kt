@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.avonix.profitness.presentation.news

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

// ── Categories ────────────────────────────────────────────────────────────────

private val CATEGORIES = listOf(
    "TÜMÜ", "BİLİM", "BESLENME", "ANTRENMAN", "SPOR", "ZİHİN", "YAŞAM", "TOPARLANMA", "TEKNOLOJİ"
)

// ── Screen Entry ─────────────────────────────────────────────────────────────

@Composable
fun NewsScreen(newsViewModel: NewsViewModel = viewModel()) {
    val uiState by newsViewModel.uiState.collectAsState()
    val detailState by newsViewModel.detailState.collectAsState()
    val theme = LocalAppTheme.current
    val appLang = if (theme.language == AppLanguage.TURKISH) "tr" else "en"

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
                    onBack = { newsViewModel.closeArticle() }
                )
            } else {
                NewsFeed(
                    uiState = uiState,
                    onArticleClick = { newsViewModel.openArticle(it, appLang) },
                    onRefresh = { newsViewModel.refresh() }
                )
            }
        }
    }
}

// ── Feed ──────────────────────────────────────────────────────────────────────

@Composable
private fun NewsFeed(
    uiState: NewsUiState,
    onArticleClick: (Article) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("TÜMÜ") }
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
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
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(theme.bg2)
                ) {
                    Icon(
                        if (uiState.isLoading) Icons.Rounded.HourglassEmpty
                        else Icons.Rounded.Refresh,
                        contentDescription = "Yenile",
                        tint = if (uiState.isLoading) accent else Snow.copy(0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Hero Carousel ──────────────────────────────────────────────────────
        item {
            val featured = uiState.articles.filter { it.isFeatured }.take(6)
            if (uiState.isLoading) {
                CarouselSkeleton()
            } else if (featured.isNotEmpty()) {
                MuseAutoCarousel(articles = featured, onArticleClick = onArticleClick)
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
                        "CANLI • ${uiState.articles.size} HABER",
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
            MuseCategoryBar(selected = selectedCategory, onSelect = { selectedCategory = it })
        }

        // ── Section label ──────────────────────────────────────────────────────
        item {
            Text(
                text = if (selectedCategory == "TÜMÜ") "TÜM HABERLER" else selectedCategory,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                color = theme.text2,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ── Article feed ───────────────────────────────────────────────────────
        val feed = if (selectedCategory == "TÜMÜ") uiState.articles
                   else uiState.articles.filter { it.category == selectedCategory }

        if (!uiState.isLoading && feed.isEmpty() && uiState.articles.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SearchOff, null, tint = Snow.copy(0.3f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Bu kategoride haber bulunamadı", color = Snow.copy(0.4f), fontSize = 13.sp)
                    }
                }
            }
        }

        itemsIndexed(feed) { index, article ->
            MuseArticleCard(
                article = article,
                isReversed = index % 2 != 0,
                onClick = { onArticleClick(article) }
            )
        }
    }
}

// ── Auto-Scrolling Carousel ───────────────────────────────────────────────────

@Composable
private fun MuseAutoCarousel(articles: List<Article>, onArticleClick: (Article) -> Unit) {
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
            MuseHeroCard(article = articles[page], onClick = { onArticleClick(articles[page]) })
        }
        Spacer(Modifier.height(14.dp))
        PagerDotIndicator(pagerState = pagerState, count = articles.size)
    }
}

@Composable
private fun MuseHeroCard(article: Article, onClick: () -> Unit) {
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
                article.title.uppercase(),
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
                Text("${article.readTime} OKUMA", color = Snow.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.5.sp)
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
                // Open source URL button
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

// ── Category Bar ──────────────────────────────────────────────────────────────

@Composable
private fun MuseCategoryBar(selected: String, onSelect: (String) -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    LazyRow(
        modifier = Modifier.padding(vertical = 24.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CATEGORIES) { cat ->
            val isSelected = cat == selected
            val bgColor by animateColorAsState(
                if (isSelected) accent else theme.bg2,
                label = "cat_bg"
            )
            val textColor by animateColorAsState(
                if (isSelected) Color.Black else Snow.copy(0.6f),
                label = "cat_text"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    cat,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

// ── Article Card ──────────────────────────────────────────────────────────────

@Composable
private fun MuseArticleCard(article: Article, isReversed: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(theme.bg1)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.height(140.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isReversed) {
                AsyncImage(
                    model = article.image,
                    contentDescription = null,
                    modifier = Modifier.width(110.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
                    contentScale = ContentScale.Crop
                )
                ArticleCardText(article, accent, theme, Modifier.weight(1f).padding(16.dp), TextAlign.Start)
            } else {
                ArticleCardText(article, accent, theme, Modifier.weight(1f).padding(16.dp), TextAlign.End)
                AsyncImage(
                    model = article.image,
                    contentDescription = null,
                    modifier = Modifier.width(110.dp).fillMaxHeight().clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun ArticleCardText(
    article: Article,
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
            article.title,
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
private fun MuseReader(detailState: ArticleDetailState, onBack: () -> Unit) {
    val article = detailState.article
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary

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
                        Text("${article.readTime} OKUMA", color = Snow.copy(0.5f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                        if (article.publishedAt.isNotBlank()) {
                            Text("•", color = Snow.copy(0.3f), fontSize = 9.sp)
                            Text(formatDate(article.publishedAt), color = Snow.copy(0.5f), fontSize = 9.sp)
                        }
                    }
                }
            }

            // ── Translation badge ──────────────────────────────────────────────
            if (detailState.isTranslating) {
                TranslationBadge(isLoading = true)
            } else if (detailState.wasTranslated) {
                TranslationBadge(isLoading = false)
            }

            // ── AI Summary box ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
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
                            Text("AI ÖZETİ", color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            detailState.displaySummary.ifBlank { "Bu haber için özet mevcut değil." },
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
                if (article.content.isNotBlank() && article.content != article.summary) {
                    Text("İÇERİK", color = theme.text2, fontSize = 9.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        article.content,
                        color = Snow.copy(0.75f),
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(32.dp))
                }

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
                            "HABERİN ORİJİNALİNE GİT",
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
                            "Kaynak: ${article.sourceName}",
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
        }
    }
}

// ── Translation Badge ─────────────────────────────────────────────────────────

@Composable
private fun TranslationBadge(isLoading: Boolean) {
    val theme = LocalAppTheme.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLoading) theme.bg2 else accent.copy(0.1f))
            .border(1.dp, accent.copy(0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            val alpha by rememberInfiniteTransition(label = "tl").animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "tl_alpha"
            )
            Icon(Icons.Rounded.Translate, null, tint = accent.copy(alpha), modifier = Modifier.size(14.dp))
            Text("Türkçeye çevriliyor...", color = accent.copy(alpha), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        } else {
            Icon(Icons.Rounded.Translate, null, tint = accent, modifier = Modifier.size(14.dp))
            Text("Yapay zeka ile Türkçeye çevrildi", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
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

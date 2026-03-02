@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.avonix.profitness.presentation.news

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.avonix.profitness.core.theme.*
import kotlinx.coroutines.delay

data class Article(
    val id: String,
    val title: String,
    val category: String,
    val readTime: String,
    val image: String,
    val summary: String = "",
    val isFeatured: Boolean = false
)

val DEMO_NEWS = listOf(
    Article("1", "Hipertrofi Bilimi: Mekanik Gerilim", "BİLİM", "6 dk",
        "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=800",
        "Hipertrofi — kas kütlesi artışı — mekanik gerilim ve metabolik stres üzerine inşa edilmiştir.",
        isFeatured = true),
    Article("2", "Kreatin: Gerçek Performans", "BESLENME", "4 dk",
        "https://images.unsplash.com/photo-1593079831268-3381b0db4a77?w=800",
        "Kreatin monohidrat, dünya genelinde en çok araştırılan spor takviyesidir.",
        isFeatured = true),
    Article("3", "Minimalist Toparlanma", "YAŞAM", "5 dk",
        "https://images.unsplash.com/photo-1511275539165-cc46b1ee8960?w=800",
        "Uyku sırasında büyüme hormonu zirvesine ulaşır. Yetersiz uyku kortizolü artırır."),
    Article("4", "Zihinsel Akış", "ZİHİN", "8 dk",
        "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800",
        "Vizualizasyon ve nefes protokolleri performansı doğrudan etkileyen unsurlardır.")
)

private val CATEGORIES = listOf("TÜMÜ", "BİLİM", "BESLENME", "YAŞAM", "ZİHİN")

@Composable
fun NewsScreen() {
    var selectedCategory by remember { mutableStateOf("TÜMÜ") }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }

    val theme = LocalAppTheme.current
    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        selectedArticle?.let { art ->
            MuseReader(article = art, onBack = { selectedArticle = null })
        } ?: run {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp, 80.dp, 0.dp, 140.dp)
            ) {
                // ── Editorial Header ─────────────────────────────────────────
                item {
                    Column(Modifier.padding(24.dp, 0.dp, 24.dp, 32.dp)) {
                        Text("EDITORIAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 6.sp, fontWeight = FontWeight.ExtraLight)
                        Text("MUSE", style = MaterialTheme.typography.displayLarge, color = Snow, fontWeight = FontWeight.Black)
                    }
                }

                // ── Hero Section (Parallax) ───────────────────────────────────
                item {
                    MuseHeroSection(
                        articles = DEMO_NEWS.filter { it.isFeatured },
                        onArticleClick = { selectedArticle = it }
                    )
                }

                // ── Categories ────────────────────────────────────────────────
                item {
                    MuseCategorySection(selected = selectedCategory, onSelect = { selectedCategory = it })
                }

                // ── Zig-Zag Feed ──────────────────────────────────────────────
                val feed = if (selectedCategory == "TÜMÜ") DEMO_NEWS else DEMO_NEWS.filter { it.category == selectedCategory }
                itemsIndexed(feed) { index, article ->
                    MuseEditorialCard(
                        article = article,
                        isRightAligned = index % 2 != 0,
                        onClick = { selectedArticle = article }
                    )
                }
            }
        }
    }
}

@Composable
private fun MuseHeroSection(articles: List<Article>, onArticleClick: (Article) -> Unit) {
    val pagerState = rememberPagerState { articles.size }
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(420.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 16.dp
    ) { page ->
        val article = articles[page]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(32.dp))
                .clickable { onArticleClick(article) }
        ) {
            AsyncImage(
                model = article.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))))
            
            Column(Modifier.align(Alignment.BottomStart).padding(32.dp)) {
                Text(article.category, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(article.title.uppercase(), color = Snow, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text("${article.readTime} READ", color = Snow.copy(0.6f), fontSize = 10.sp, fontWeight = FontWeight.Light, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun MuseCategorySection(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(vertical = 32.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(CATEGORIES) { cat ->
            val isSelected = cat == selected
            Column(
                modifier = Modifier.clickable { onSelect(cat) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cat,
                    color = if (isSelected) Snow else Mist,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 2.sp
                )
                if (isSelected) {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

@Composable
private fun MuseEditorialCard(article: Article, isRightAligned: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp, 16.dp)
            .height(200.dp)
            .clickable { onClick() },
        horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start
    ) {
        if (!isRightAligned) {
            AsyncImage(
                model = article.image,
                contentDescription = null,
                modifier = Modifier.weight(0.45f).fillMaxHeight().clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(0.55f).padding(vertical = 8.dp)) {
                Text(article.category, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(article.title, color = Snow, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp, maxLines = 3)
                Spacer(Modifier.weight(1f))
                Text(article.readTime.uppercase(), color = Fog, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(Modifier.weight(0.55f).padding(vertical = 8.dp), horizontalAlignment = Alignment.End) {
                Text(article.category, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(article.title, color = Snow, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp, maxLines = 3, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Spacer(Modifier.weight(1f))
                Text(article.readTime.uppercase(), color = Fog, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(20.dp))
            AsyncImage(
                model = article.image,
                contentDescription = null,
                modifier = Modifier.weight(0.45f).fillMaxHeight().clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun MuseReader(article: Article, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    
    val theme = LocalAppTheme.current
    Box(modifier = Modifier.fillMaxSize().background(theme.bg0)) {
        PageAccentBloom()
        Column(modifier = Modifier.verticalScroll(scrollState).padding(0.dp, 0.dp, 0.dp, 120.dp)) {
            // Hero
            Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
                AsyncImage(
                    model = article.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent, Color.Black))))
                
                Column(Modifier.align(Alignment.BottomStart).padding(32.dp)) {
                    Text(article.category, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(article.title.uppercase(), color = Snow, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 46.sp)
                }
            }
            
            // Content
            Column(Modifier.padding(32.dp)) {
                Text(
                    "Tutarlılık, motivasyondan daha değerlidir. Performansın zirvesinde, biyolojik kapasitenizi zorlamak değil, onu sistemli bir şekilde yönetmek yatar.",
                    color = Mist, fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Light, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(Modifier.height(32.dp))
                repeat(4) {
                    Text(
                        "Profesyonel spor dünyasında adaptasyon döngüsü, sadece antrenmanın şiddetiyle değil, aynı zamanda toparlanma fazının derinliğiyle ölçülür. Bir atletin başarısı, dinlenme anındaki parasempatik aktivasyonu ne kadar hızlı tetikleyebildiğine bağlıdır. Bilimsel veriler gösteriyor ki, yüksek yoğunluklu antrenman sonrası ilk 2 saatlik pencere, metabolik homeostasın yeniden kurulması için hayati öneme sahiptir.",
                        color = Snow.copy(0.8f), fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        
        // Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(0.5f))) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Snow)
            }
            IconButton(onClick = {}, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(0.5f))) {
                Icon(Icons.Rounded.Share, null, tint = Snow)
            }
        }
    }
}

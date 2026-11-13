package com.cosmibit.profitness.presentation.components

import android.content.Context
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import com.cosmibit.profitness.core.theme.LocalAppTheme
import com.cosmibit.profitness.core.theme.rememberSinemaMotionOn
import com.cosmibit.profitness.core.theme.sinemaCapsuleGlass
import com.cosmibit.profitness.core.theme.t
import com.cosmibit.profitness.presentation.workout.SceneImage
import java.io.File

/** Tek paylaşılan disk önbelleği (100 MB, en az kullanılan silinir): döngü videoları her açılışta yeniden inmez. */
@OptIn(UnstableApi::class)
private object SinemaVideoCache {
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(context: Context): Cache = cache ?: run {
        val app = context.applicationContext
        SimpleCache(File(app.cacheDir, "sinema_video"), LeastRecentlyUsedCacheEvictor(100L * 1024 * 1024), StandaloneDatabaseProvider(app))
            .also { cache = it }
    }
}

/**
 * Tek aktif oynatıcı: oynamak isteyen her [SinemaVideo] kendini yığına ekler, yalnız en üsttekisi oynar.
 * Hareketi gör ekranı açılınca altındaki Antrenman videosu durur; kapanınca devam eder.
 */
private object SinemaVideoFocus {
    val stack = mutableStateListOf<Any>()
}

/**
 * Dikey (9:16) sessiz döngü hareket videosu. Kırpma: alanı kaplar (cover), ortalı.
 * - [posterUrl]: ilk kare gelene kadar ve hata olursa görünür (null ise altındaki içerik görünür).
 * - Oynatıcı yalnız oynarken yaşar: [playing] false, uygulama arka planda ya da başka bir SinemaVideo öndeyse serbest bırakılır.
 * - Sistem animasyonları kapalıysa (azaltılmış hareket) otomatik oynamaz; poster üstünde oynat düğmesi çıkar.
 * Ağ/çözme hatasında sessizce poster kalır.
 */
@OptIn(UnstableApi::class)
@Composable
fun SinemaVideo(
    url: String,
    posterUrl: String?,
    playing: Boolean,
    modifier: Modifier = Modifier,
    muted: Boolean = true,
    loop: Boolean = true
) {
    val context = LocalContext.current
    val motionOn = rememberSinemaMotionOn()
    val resumed = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.RESUMED)
    var userStarted by remember(url) { mutableStateOf(false) }
    val wantPlay = playing && resumed && url.isNotBlank() && (motionOn || userStarted)

    val token = remember { Any() }
    DisposableEffect(wantPlay) {
        if (wantPlay) SinemaVideoFocus.stack.add(token)
        onDispose { SinemaVideoFocus.stack.remove(token) }
    }
    val active = wantPlay && SinemaVideoFocus.stack.lastOrNull() === token

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var firstFrame by remember { mutableStateOf(false) }
    var aspect by remember { mutableFloatStateOf(9f / 16f) }

    DisposableEffect(url, active, loop) {
        if (!active) return@DisposableEffect onDispose { }
        val factory = CacheDataSource.Factory()
            .setCache(SinemaVideoCache.get(context))
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(factory))
            .build()
        p.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        p.volume = if (muted) 0f else 1f
        p.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }
            override fun onPlayerError(error: PlaybackException) { firstFrame = false }   // poster kalır
        })
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true
        player = p
        onDispose {
            firstFrame = false
            player = null
            p.release()
        }
    }
    LaunchedEffect(player, muted) { player?.volume = if (muted) 0f else 1f }

    val videoAlpha by animateFloatAsState(if (firstFrame) 1f else 0f, label = "sinemaVideoAlpha")
    Box(modifier.clipToBounds()) {
        if (!posterUrl.isNullOrBlank()) {
            SceneImage(
                url = posterUrl, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - videoAlpha },
                requestWidthPx = 1080, requestHeightPx = 1920,
                fallbackIconSize = 64.dp, fallbackIconBias = -0.2f, fallbackGradient = true
            )
        }
        player?.let { p ->
            AndroidView(
                factory = { TextureView(it) },
                update = { tv -> p.setVideoTextureView(tv) },
                modifier = Modifier
                    .layout { m, c ->
                        // cover: oran korunur, kısa kenar alanı doldurur, taşan kırpılır
                        val cw = c.maxWidth
                        val ch = c.maxHeight
                        val w: Int
                        val h: Int
                        if (cw.toFloat() / ch > aspect) { w = cw; h = (cw / aspect).toInt() } else { h = ch; w = (ch * aspect).toInt() }
                        val pl = m.measure(Constraints.fixed(w.coerceAtLeast(1), h.coerceAtLeast(1)))
                        layout(cw, ch) { pl.place((cw - w) / 2, (ch - h) / 2) }
                    }
                    .graphicsLayer { alpha = videoAlpha }
            )
        }
        if (playing && !motionOn && !userStarted) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .sinemaCapsuleGlass(CircleShape)
                    .clickable(role = Role.Button) { userStarted = true },
                contentAlignment = Alignment.Center
            ) {
                val theme = LocalAppTheme.current
                Icon(Icons.Rounded.PlayArrow, theme.t("Videoyu oynat", "Play video"), tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

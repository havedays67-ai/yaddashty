package com.yaddashty.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yaddashty.app.ads.AdManager
import com.yaddashty.app.ads.TapsellConfig
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class Star2D(var x: Float, var y: Float, val radius: Float, val speed: Float, val color: Color)
private data class Bullet2D(var x: Float, var y: Float, val vy: Float, val isEnemy: Boolean, val color: Color)
private data class Enemy2D(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var hp: Int,
    val maxHp: Int,
    val color: Color,
    val isBoss: Boolean = false
)
private data class Particle2D(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var alpha: Float,
    val color: Color
)

@Composable
fun NeonGameScreen(onOpenNotes: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adReady by AdManager.adReadyFlow.collectAsState()
    val adStatusMsg by AdManager.statusMessageFlow.collectAsState()

    var useUltraWebEngine by remember { mutableStateOf(false) }
    var testMode by remember { mutableStateOf(TapsellConfig.testMode) }

    // وضعیت‌های بازی دو بعدی نیتیو
    var gameState by remember { mutableStateOf("PLAYING") } // PLAYING | GAMEOVER
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var wave by remember { mutableIntStateOf(1) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    var playerX by remember { mutableFloatStateOf(500f) }
    var playerY by remember { mutableFloatStateOf(1350f) }
    var invulnerableTicks by remember { mutableIntStateOf(60) }

    val stars = remember {
        mutableStateListOf<Star2D>().apply {
            val palette = listOf(Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFFE0F2FE), Color(0xFFFACC15))
            repeat(85) {
                add(
                    Star2D(
                        x = Random.nextFloat() * 1080f,
                        y = Random.nextFloat() * 1800f,
                        radius = 1.5f + Random.nextFloat() * 3.5f,
                        speed = 2f + Random.nextFloat() * 7f,
                        color = palette[it % palette.size]
                    )
                )
            }
        }
    }
    val bullets = remember { mutableStateListOf<Bullet2D>() }
    val enemies = remember { mutableStateListOf<Enemy2D>() }
    val particles = remember { mutableStateListOf<Particle2D>() }

    // مقداردهی اولیه و پیش‌بارگذاری تبلیغ تپسل در بدو ورود به بازی (شرط ۳)
    LaunchedEffect(activity) {
        if (activity != null) {
            AdManager.initialize(activity)
        }
    }

    fun resetGame() {
        score = 0
        lives = 3
        wave = 1
        invulnerableTicks = 80
        bullets.clear()
        enemies.clear()
        particles.clear()
        feedbackMessage = null
        gameState = "PLAYING"
        if (activity != null) {
            AdManager.preload(activity)
        }
    }

    // حلقه ۶۰ فریم بر ثانیه موتور دو بعدی نیتیو
    LaunchedEffect(gameState, useUltraWebEngine) {
        if (useUltraWebEngine) return@LaunchedEffect
        var tick = 0
        while (true) {
            delay(16L)
            tick++

            // حرکت ستاره‌ها
            for (s in stars) {
                s.y += s.speed
                if (s.y > 1900f) {
                    s.y = -10f
                    s.x = Random.nextFloat() * 1080f
                }
            }

            if (gameState != "PLAYING") continue

            if (invulnerableTicks > 0) invulnerableTicks--

            // شلیک خودکار سفینه نئونی
            if (tick % 8 == 0) {
                bullets.add(Bullet2D(playerX - 22f, playerY - 36f, -24f, false, Color(0xFF38BDF8)))
                bullets.add(Bullet2D(playerX + 22f, playerY - 36f, -24f, false, Color(0xFFF472B6)))
            }

            // تولید دشمنان و غول‌آخر
            val spawnRate = (38 - wave * 2).coerceAtLeast(16)
            if (tick % spawnRate == 0) {
                val isBoss = (score > 0 && score % 1500 == 0 && enemies.none { it.isBoss })
                val ex = 90f + Random.nextFloat() * 900f
                enemies.add(
                    Enemy2D(
                        x = ex,
                        y = -60f,
                        vx = (Random.nextFloat() - 0.5f) * 5f,
                        vy = if (isBoss) 2.2f else (4.2f + Random.nextFloat() * 4.5f + wave * 0.4f),
                        radius = if (isBoss) 82f else 42f,
                        hp = if (isBoss) 18 + wave * 4 else 2 + (wave / 2),
                        maxHp = if (isBoss) 18 + wave * 4 else 2 + (wave / 2),
                        color = if (isBoss) Color(0xFFFB7185) else Color(0xFFA855F7),
                        isBoss = isBoss
                    )
                )
            }

            // بروزرسانی گلوله‌ها
            val bIter = bullets.iterator()
            while (bIter.hasNext()) {
                val b = bIter.next()
                b.y += b.vy
                if (b.y < -50f || b.y > 2000f) {
                    bIter.remove()
                }
            }

            // بروزرسانی دشمنان و برخوردها
            val eIter = enemies.iterator()
            while (eIter.hasNext()) {
                val e = eIter.next()
                e.x += e.vx
                e.y += e.vy
                if (e.x < e.radius || e.x > 1020f - e.radius) e.vx *= -1f

                if (e.y > 1950f) {
                    eIter.remove()
                    continue
                }

                // برخورد با سفینه بازیکن
                if (invulnerableTicks <= 0 && hypot(e.x - playerX, e.y - playerY) < e.radius + 36f) {
                    lives -= 1
                    invulnerableTicks = 90
                    repeat(22) {
                        val ang = Random.nextFloat() * 6.28f
                        val spd = 3f + Random.nextFloat() * 10f
                        particles.add(
                            Particle2D(
                                playerX,
                                playerY,
                                cos(ang) * spd,
                                sin(ang) * spd,
                                6f,
                                1f,
                                Color(0xFFF43F5E)
                            )
                        )
                    }
                    if (!e.isBoss) eIter.remove()
                    if (lives <= 0) {
                        lives = 0
                        gameState = "GAMEOVER"
                    }
                    continue
                }

                // برخورد گلوله‌ها با دشمن
                val bulletHit = bullets.firstOrNull { !it.isEnemy && hypot(it.x - e.x, it.y - e.y) < e.radius + 14f }
                if (bulletHit != null) {
                    bullets.remove(bulletHit)
                    e.hp -= 1
                    if (e.hp <= 0) {
                        score += if (e.isBoss) 500 else 100
                        if (score > highScore) highScore = score
                        if (score % 1000 == 0) wave++
                        repeat(18) {
                            val ang = Random.nextFloat() * 6.28f
                            val spd = 2f + Random.nextFloat() * 9f
                            particles.add(
                                Particle2D(
                                    e.x,
                                    e.y,
                                    cos(ang) * spd,
                                    sin(ang) * spd,
                                    5f,
                                    1f,
                                    e.color
                                )
                            )
                        }
                        eIter.remove()
                    }
                }
            }

            // بروزرسانی ذرات انفجار
            val pIter = particles.iterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.x += p.vx
                p.y += p.vy
                p.alpha -= 0.04f
                if (p.alpha <= 0f) pIter.remove()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040711))
            .systemBarsPadding()
    ) {
        // هدر بالایی بازی
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "❤️ $lives",
                    color = Color(0xFFFB7185),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = "امتیاز: $score",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "موج $wave",
                    color = Color(0xFFFACC15),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        testMode = !testMode
                        TapsellConfig.testMode = testMode
                        if (activity != null) AdManager.preload(activity)
                    }
                ) {
                    Text(
                        text = if (testMode) "تست تپسل: روشن" else "تست: خاموش",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp
                    )
                }
                TextButton(onClick = { useUltraWebEngine = !useUltraWebEngine }) {
                    Text(
                        text = if (useUltraWebEngine) "بوم نیتیو" else "موتور اولترا",
                        color = Color(0xFFC084FC),
                        fontSize = 11.sp
                    )
                }
                TextButton(onClick = onOpenNotes) {
                    Text(text = "یادداشت‌ها", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }

        if (useUltraWebEngine && activity != null) {
            UltraWebGameView(activity = activity)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                playerX = change.position.x.coerceIn(50f, size.width - 50f)
                                playerY = change.position.y.coerceIn(120f, size.height - 80f)
                            }
                        }
                ) {
                    // پس‌زمینه کهکشانی
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF040711), Color(0xFF0F172A), Color(0xFF1E1B4B))
                        )
                    )

                    // ستاره‌ها
                    for (s in stars) {
                        drawCircle(
                            color = s.color,
                            radius = s.radius,
                            center = Offset(s.x % size.width, s.y % size.height)
                        )
                    }

                    // گلوله‌ها
                    for (b in bullets) {
                        drawCircle(color = b.color, radius = 9f, center = Offset(b.x, b.y))
                    }

                    // دشمنان
                    for (e in enemies) {
                        drawCircle(
                            color = e.color.copy(alpha = 0.25f),
                            radius = e.radius + 10f,
                            center = Offset(e.x, e.y)
                        )
                        drawCircle(
                            color = e.color,
                            radius = e.radius,
                            center = Offset(e.x, e.y),
                            style = Stroke(width = 6f)
                        )
                    }

                    // ذرات انفجار
                    for (p in particles) {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                            radius = p.radius,
                            center = Offset(p.x, p.y)
                        )
                    }

                    // سفینه نئونی بازیکن
                    if (gameState == "PLAYING") {
                        val shipPath = Path().apply {
                            moveTo(playerX, playerY - 46f)
                            lineTo(playerX - 40f, playerY + 36f)
                            lineTo(playerX, playerY + 18f)
                            lineTo(playerX + 40f, playerY + 36f)
                            close()
                        }
                        drawPath(path = shipPath, color = Color(0xFF38BDF8))
                        if (invulnerableTicks > 0) {
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                                radius = 62f,
                                center = Offset(playerX, playerY),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }

                // ۸. صفحه Game Over همراه با دکمه «ببین تا یک جان اضافه بگیر» متصل به AdManager.showRewarded
                if (gameState == "GAMEOVER") {
                    val isOnlineNow = AdManager.isOnline(context)
                    val canWatchRewarded = isOnlineNow && adReady

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xE6040711))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "پایان بازی (Game Over)",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "امتیاز شما: $score  |  بهترین رکورد: $highScore",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // شرط ۸: دکمه «ببین تا یک جان اضافه بگیر» متصل به AdManager.showRewarded
                                Button(
                                    onClick = {
                                        if (activity != null) {
                                            AdManager.showRewarded(
                                                activity = activity,
                                                onReward = {
                                                    // شرط ۴: فقط وقتی ویدیو کامل دیده شد ۱ جان اضافه بده
                                                    lives = 1
                                                    invulnerableTicks = 150
                                                    enemies.clear()
                                                    bullets.clear()
                                                    feedbackMessage = null
                                                    gameState = "PLAYING"
                                                },
                                                onFail = { reason ->
                                                    feedbackMessage = reason
                                                }
                                            )
                                        }
                                    },
                                    enabled = canWatchRewarded,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        disabledContainerColor = Color(0xFF334155)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (canWatchRewarded) {
                                            "🎬 ببین تا یک جان اضافه بگیر"
                                        } else {
                                            "الان تبلیغی موجود نیست"
                                        },
                                        color = if (canWatchRewarded) Color(0xFF040711) else Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }

                                // شرط ۵: نمایش پیام «الان تبلیغی موجود نیست» در صورت عدم آمادگی یا نبود اینترنت
                                if (!canWatchRewarded || feedbackMessage != null) {
                                    Text(
                                        text = feedbackMessage ?: if (!isOnlineNow) {
                                            "الان تبلیغی موجود نیست (اینترنت قطع است)"
                                        } else {
                                            adStatusMsg.ifEmpty { "الان تبلیغی موجود نیست" }
                                        },
                                        color = Color(0xFFFB7185),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                OutlinedButton(
                                    onClick = { resetGame() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "شروع مجدد از اول", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun UltraWebGameView(activity: Activity) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                addJavascriptInterface(
                    AdManager.WebBridge(activity) { webViewRef },
                    "AndroidTapsellBridge"
                )
                webViewRef = this
                loadUrl("file:///android_asset/game/index.html")
            }
        }
    )
}

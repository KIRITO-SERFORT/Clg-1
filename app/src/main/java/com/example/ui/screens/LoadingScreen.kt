package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** How long the loading screen stays at least (milliseconds). */
const val SPLASH_MIN_MILLIS = 3200L

/** Background used only when you provide your own GIF (see LoadingScreen). */
val LoadingBackground = Color(0xFF0B0F1A)

private val GoldLight = Color(0xFFF9E7A6)
private val Gold = Color(0xFFE6B94A)
private val GoldDeep = Color(0xFFB8860B)
private val RoyalDeep = Color(0xFF0B0A1F)
private val RoyalMid = Color(0xFF1B1040)
private val RoyalPurple = Color(0xFF2E1065)
private val Lavender = Color(0xFFC4B5FD)

/**
 * Loading screen.
 * Default: a royal animation drawn in code (twinkling stars, orbiting gold rings,
 * logo reveal, letter-by-letter title, rotating status messages, shimmering progress bar).
 * Optional: if you add your own animated GIF at app/src/main/res/raw/loading_logo.gif,
 * that GIF is played instead.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val gifResId = remember {
        context.resources.getIdentifier("loading_logo", "raw", context.packageName)
    }
    if (gifResId != 0) {
        GifLoading(gifResId, modifier)
    } else {
        RoyalLoading(modifier)
    }
}

@Composable
private fun GifLoading(gifResId: Int, modifier: Modifier) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LoadingBackground),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(gifResId).build(),
            imageLoader = imageLoader,
            contentDescription = "Loading",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(240.dp)
        )
    }
}

private data class Star(val x: Float, val y: Float, val radiusDp: Float, val phase: Float)

@Composable
private fun RoyalLoading(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "royal_loading")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "spin"
    )
    val spinBack by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "spin_back"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle"
    )
    val barShift by infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearOutSlowInEasing)),
        label = "bar"
    )

    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(700)) }
        logoScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    val stars = remember {
        val random = Random(7)
        List(46) {
            Star(random.nextFloat(), random.nextFloat(), 0.8f + random.nextFloat() * 1.6f, random.nextFloat())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalDeep, RoyalMid, RoyalPurple))),
        contentAlignment = Alignment.Center
    ) {
        // Twinkling stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { star ->
                val alpha = 0.2f + 0.8f * abs(sin(2.0 * PI * (twinkle + star.phase))).toFloat()
                drawCircle(
                    color = GoldLight.copy(alpha = alpha * 0.8f),
                    radius = star.radiusDp.dp.toPx(),
                    center = Offset(star.x * size.width, star.y * size.height)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with orbiting rings
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // soft glow behind the logo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Gold.copy(alpha = 0.28f), Color.Transparent),
                            center = center,
                            radius = size.minDimension / 2f
                        ),
                        radius = size.minDimension / 2f,
                        center = center
                    )

                    // outer gold ring (clockwise)
                    val outerInset = 6.dp.toPx()
                    rotate(degrees = spin, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0f to Color.Transparent,
                                0.65f to GoldDeep.copy(alpha = 0.6f),
                                1f to GoldLight,
                                center = center
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(outerInset, outerInset),
                            size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // inner lavender ring (counter-clockwise)
                    val innerInset = 22.dp.toPx()
                    rotate(degrees = spinBack, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0f to Color.Transparent,
                                0.7f to Lavender.copy(alpha = 0.5f),
                                1f to Color.White,
                                center = center
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(innerInset, innerInset),
                            size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // small gold bead riding the outer ring
                    val orbitRadius = size.width / 2f - outerInset
                    val angle = Math.toRadians(spin.toDouble())
                    val bead = Offset(
                        center.x + orbitRadius * cos(angle).toFloat(),
                        center.y + orbitRadius * sin(angle).toFloat()
                    )
                    drawCircle(Gold.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = bead)
                    drawCircle(GoldLight, radius = 4.dp.toPx(), center = bead)
                }

                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Clg 1% logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                        .clip(CircleShape)
                        .border(2.dp, Brush.linearGradient(listOf(GoldLight, GoldDeep)), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title, one letter at a time
            val title = "Clg 1%"
            val titleStyle = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                letterSpacing = 2.sp,
                brush = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                title.forEachIndexed { index, letter ->
                    if (letter == ' ') {
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        val reveal = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(450L + index * 70L)
                            reveal.animateTo(1f, tween(450))
                        }
                        Text(
                            text = letter.toString(),
                            style = titleStyle,
                            modifier = Modifier.graphicsLayer {
                                alpha = reveal.value
                                translationY = (1f - reveal.value) * 18.dp.toPx()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rotating status messages
            val messages = remember {
                listOf("Syncing your attendance…", "Fetching your timetable…", "Preparing your widgets…")
            }
            var messageIndex by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1100)
                    messageIndex = (messageIndex + 1) % messages.size
                }
            }
            Crossfade(targetState = messageIndex, animationSpec = tween(400), label = "message") { index ->
                Text(
                    text = messages[index],
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = Lavender.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Shimmering progress bar
            Box(
                modifier = Modifier
                    .width(170.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = size.width * 0.4f
                    val x = barShift * size.width
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Gold, GoldLight, Gold, Color.Transparent),
                            startX = x,
                            endX = x + barWidth
                        ),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                }
            }
        }
    }
}

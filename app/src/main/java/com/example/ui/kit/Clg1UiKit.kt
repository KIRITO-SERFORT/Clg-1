package com.example.ui.kit

/*
 * CLG 1% UI KIT  (single file, Jetpack Compose + Material 3, no extra libraries)
 *
 * Contains:
 *   1. ClgColors + ClgTheme (dark-first, light supported)
 *   2. LoadingScreen   (logo animation: spring-in, glow rings, shine sweep, particles, progress bar, fade-out)
 *   3. Motion helpers  (bouncyClick, shimmer, staggeredEnter, rememberReduceMotion)
 *   4. Components      (GlassCard, GradientButton, ProgressRing, AnimatedNumber, StatCard,
 *                       SubjectRow, ClgBottomBar, ClgTabContent, SkeletonBox, ClgBackground)
 *   5. ClgDemoHome     (a full example screen that uses everything; replace the fake data)
 */

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

// ============================================================================================
// 1. COLORS + THEME
// ============================================================================================

object ClgColors {
    val Navy: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.background
    val NavyDeep: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerLowest
    val NavySurface: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
    val NavyCard: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainer
    val Cyan: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val CyanDeep: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
    val White: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val TextMuted: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val Outline: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
    val Success: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
    val Warning = Color(0xFFFFB74D)
    val Danger: Color @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = MaterialTheme.colorScheme.error
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF00F0FF), onPrimary = Color(0xFF010824),
    secondary = Color(0xFF00B8D4), onSecondary = Color(0xFF010824),
    tertiary = Color(0xFF4ADE80),
    background = Color(0xFF010824), onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A1433), onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF111C40), onSurfaceVariant = Color(0xFF9AA7C7),
    outline = Color(0xFF2A3969), error = Color(0xFFFF6B6B)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF006B7A), onPrimary = Color.White,
    secondary = Color(0xFF00899C),
    background = Color(0xFFF4F8FF), onBackground = Color(0xFF0B1330),
    surface = Color.White, onSurface = Color(0xFF0B1330),
    surfaceVariant = Color(0xFFE6EEFB), onSurfaceVariant = Color(0xFF50607F),
    outline = Color(0xFFB7C4DE), error = Color(0xFFB3261E)
)

private val ClgShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp)
)

private val Base = Typography()
private val ClgTypography = Typography(
    headlineLarge = Base.headlineLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = Base.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

/** Wrap your whole app in this. Dark is the brand look; it follows the system setting. */
@Composable
fun ClgTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ClgTypography, shapes = ClgShapes, content = content
    )
}

// ============================================================================================
// 3. MOTION HELPERS
// ============================================================================================

/** True when the user switched system animations off. Decorative motion should then be skipped. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Press feedback: springy shrink + light haptic. Replaces clickable() for cards and buttons. */
fun Modifier.bouncyClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f), label = "press"
    )
    val haptic = LocalHapticFeedback.current
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = source, indication = null, enabled = enabled, role = Role.Button,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
        )
}

/** Moving highlight for loading placeholders (skeletons). */
fun Modifier.shimmer(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "shimmerX"
    )
    this.drawBehind {
        val w = size.width
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.06f)),
                start = Offset(w * x, 0f), end = Offset(w * x + w * 0.6f, size.height)
            )
        )
    }
}

/** List items fade/slide up one after another. index = position in the list. */
fun Modifier.staggeredEnter(index: Int, reduceMotion: Boolean = false): Modifier = composed {
    var shown by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(Unit) { delay((index.coerceAtMost(10) * 60).toLong()); shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "enterAlpha")
    val y by animateFloatAsState(if (shown) 0f else 24f, tween(450, easing = FastOutSlowInEasing), label = "enterY")
    this.graphicsLayer { alpha = a; translationY = y * density }
}

// ============================================================================================
// 2. LOADING SCREEN
// ============================================================================================

/**
 * @param isReady       true when your real loading is done; the screen then fades out (after 1.6 s minimum)
 * @param onFinished    called after the fade-out; hide the screen here
 * @param progress      optional real progress 0..1. null = looping bar
 */
@Composable
fun LoadingScreen(
    isReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    reduceMotion: Boolean = rememberReduceMotion()
) {
    val logoScale = remember { Animatable(if (reduceMotion) 1f else 0.6f) }
    val logoAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val barAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduceMotion) logoAlpha.animateTo(1f, tween(500)) }
    LaunchedEffect(Unit) { if (!reduceMotion) logoScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f)) }
    LaunchedEffect(Unit) { delay(700); barAlpha.animateTo(1f, tween(500)) }

    val inf = rememberInfiniteTransition(label = "loading")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulse")
    val shine by inf.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(2200, delayMillis = 600, easing = LinearEasing), RepeatMode.Restart), label = "shine")
    val particles by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "particles")
    val barLoop by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "barLoop")

    var minTimeDone by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1600); minTimeDone = true }
    LaunchedEffect(isReady, minTimeDone) { if (isReady && minTimeDone) exiting = true }
    val screenAlpha by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f, animationSpec = tween(450), label = "exit",
        finishedListener = { if (exiting) onFinished() }
    )

    Box(
        modifier = modifier.fillMaxSize().background(ClgColors.Navy).alpha(screenAlpha)
            .semantics { contentDescription = "CLG 1 percent is loading" },
        contentAlignment = Alignment.Center
    ) {
        val particleColor = MaterialTheme.colorScheme.primary
        if (!reduceMotion) Canvas(Modifier.fillMaxSize()) {           // rising particles = growth
            for (i in 0 until 18) {
                val seed = (i * 0.61803f) % 1f
                val t = (particles + seed) % 1f
                val x = size.width * (0.12f + 0.76f * ((i * 37 % 100) / 100f)) + 14f * sin((t * 2 * PI + i).toFloat())
                val y = size.height * (1.05f - 1.1f * t)
                drawCircle(particleColor.copy(alpha = (1f - t) * 0.55f * (0.4f + 0.6f * seed)), 2.5f + 4f * seed, Offset(x, y))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                val pulseCyan = ClgColors.Cyan
                if (!reduceMotion) Canvas(Modifier.fillMaxSize()) {   // pulsing glow rings
                    for (k in 0..1) {
                        val t = (pulse + k * 0.5f) % 1f
                        drawCircle(
                            Brush.radialGradient(listOf(pulseCyan.copy(alpha = 0.28f * (1f - t)), Color.Transparent)),
                            radius = size.minDimension * (0.35f + 0.25f * t)
                        )
                    }
                }
                Image(                                                // logo + diagonal shine sweep
                    painter = painterResource(R.drawable.clg1_logo),
                    contentDescription = "Clg 1% Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(190.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            if (!reduceMotion) {
                                val w = size.width; val h = size.height; val x = w * shine
                                drawRect(
                                    brush = Brush.linearGradient(
                                        listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                        start = Offset(x - w * 0.18f, h * 0.9f), end = Offset(x + w * 0.18f, h * 0.1f)
                                    ),
                                    size = Size(w, h), blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Clg 1%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Loading academic workspace...",
                style = MaterialTheme.typography.bodySmall,
                color = ClgColors.TextMuted
            )
            Spacer(Modifier.height(16.dp))
            Box(Modifier.width(180.dp).height(4.dp).alpha(barAlpha.value)) {
                val barCyan = ClgColors.Cyan
                Canvas(Modifier.fillMaxSize()) {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(Color.White.copy(alpha = 0.12f), cornerRadius = r)
                    if (progress != null) {
                        drawRoundRect(barCyan, size = Size(size.width * progress.coerceIn(0f, 1f), size.height), cornerRadius = r)
                    } else {
                        val w = size.width * 0.35f; val x = (size.width + w) * barLoop - w
                        drawRoundRect(Brush.horizontalGradient(listOf(Color.Transparent, barCyan, Color.Transparent), startX = x, endX = x + w), cornerRadius = r)
                    }
                }
            }
        }
    }
}

// ============================================================================================
// 4. COMPONENTS
// ============================================================================================

/** Screen background: deep navy gradient with a soft cyan glow at the top. */
@Composable
fun ClgBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val bgGlow = ClgColors.Cyan
    Box(
        modifier = modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(ClgColors.Navy, ClgColors.NavyDeep)))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(bgGlow.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.85f, 0f), radius = size.width * 0.8f),
                    radius = size.width * 0.8f, center = Offset(size.width * 0.85f, 0f)
                )
            },
        content = content
    )
}

/** Translucent card with a cyan-to-white gradient border. Pass onClick to make it tappable. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val base = modifier.fillMaxWidth().clip(shape)
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))
        .border(1.dp, Brush.linearGradient(listOf(ClgColors.Cyan.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))), shape)
    Column(
        modifier = (if (onClick != null) base.bouncyClick(onClick = onClick) else base).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content
    )
}

/** Main call-to-action: cyan gradient pill. */
@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier.fillMaxWidth().height(52.dp).clip(CircleShape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(ClgColors.CyanDeep, ClgColors.Cyan))
                else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f)))
            )
            .bouncyClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = ClgColors.Navy, style = MaterialTheme.typography.labelLarge) }
}

/** Number that counts up/down smoothly when its value changes. */
@Composable
fun AnimatedNumber(value: Float, modifier: Modifier = Modifier, decimals: Int = 0, suffix: String = "", style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium) {
    val shown by animateFloatAsState(value, tween(900, easing = FastOutSlowInEasing), label = "number")
    Text("%.${decimals}f%s".format(shown, suffix), style = style, modifier = modifier, color = MaterialTheme.colorScheme.onBackground)
}

/** Circular gradient progress (0..1) with your own content in the middle. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, size: Dp = 140.dp, strokeWidth: Dp = 12.dp, content: @Composable BoxScope.() -> Unit = {}) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1100, easing = FastOutSlowInEasing), label = "ring")
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val context = LocalContext.current
    val palette = com.example.ui.theme.ClgThemeStore.load(context)
    val reduceMotion = com.example.ui.ux.rememberUxReduceMotion()
    val infinite = rememberInfiniteTransition(label = "ring_motion")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "ring_phase"
    )
    val activePhase = if (reduceMotion) 0f else phase

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val ringColors = listOf(secondary, primary, tertiary, primary)

        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val innerRadius = (this.size.minDimension / 2f) - strokeWidth.toPx()

            // Inner theme-matched motion graphics
            if (!reduceMotion && innerRadius > 0f) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                when (palette) {
                    com.example.ui.theme.ClgPalette.SUNSET,
                    com.example.ui.theme.ClgPalette.BLACK_RED,
                    com.example.ui.theme.ClgPalette.SAKURA -> {
                        // Plasma Shimmer + Fire embers
                        val sweepX = this.size.width * (-0.5f + 2f * activePhase)
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, primary.copy(alpha = 0.16f), secondary.copy(alpha = 0.12f), Color.Transparent),
                                start = Offset(sweepX, 0f),
                                end = Offset(sweepX + this.size.width * 0.7f, this.size.height)
                            ),
                            radius = innerRadius,
                            center = center
                        )
                        for (i in 0..5) {
                            val t = (activePhase + i * 0.17f) % 1f
                            val ex = center.x + sin((t * 4f + i) * Math.PI).toFloat() * innerRadius * 0.45f
                            val ey = center.y + innerRadius * (0.65f - 1.3f * t)
                            drawCircle(primary.copy(alpha = (1f - t) * 0.45f), 2.5f + (i % 3), Offset(ex, ey))
                        }
                    }
                    com.example.ui.theme.ClgPalette.FOREST,
                    com.example.ui.theme.ClgPalette.AMOLED,
                    com.example.ui.theme.ClgPalette.AURORA,
                    com.example.ui.theme.ClgPalette.PURPLE -> {
                        // Matrix / Waterfall vertical light streams
                        for (col in 0..4) {
                            val t = (activePhase + col * 0.21f) % 1f
                            val x = center.x + (col - 2) * (innerRadius * 0.34f)
                            val y = center.y - innerRadius * 0.7f + t * innerRadius * 1.4f
                            drawLine(
                                color = primary.copy(alpha = (1f - t) * 0.35f),
                                start = Offset(x, y - 14f),
                                end = Offset(x, y + 6f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    com.example.ui.theme.ClgPalette.GRAPHITE,
                    com.example.ui.theme.ClgPalette.PAPER -> {
                        // Liquid Ink Bleed pulsing aura
                        val pulse = 0.5f + 0.5f * sin(activePhase * 2f * Math.PI).toFloat()
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(primary.copy(alpha = 0.18f * pulse), Color.Transparent),
                                center = center,
                                radius = innerRadius * (0.55f + 0.35f * pulse)
                            ),
                            radius = innerRadius,
                            center = center
                        )
                    }
                    else -> {
                        // Water Flow / Mist dual-wave shimmer
                        val waveOffset = activePhase * 2f * Math.PI.toFloat()
                        for (step in -3..3) {
                            val x = center.x + step * (innerRadius * 0.24f)
                            val y = center.y + innerRadius * 0.35f + sin(waveOffset + step * 0.9f) * 6f
                            drawCircle(primary.copy(alpha = 0.18f), 8f, Offset(x, y))
                        }
                    }
                }
            }

            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            rotate(-90f + activePhase * 360f) {
                drawArc(
                    Brush.sweepGradient(ringColors),
                    startAngle = -activePhase * 360f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke
                )
            }
        }
        content()
    }
}

/** Small statistic card. `status` is shown as TEXT (never colour alone). */
@Composable
fun StatCard(title: String, value: Float, modifier: Modifier = Modifier, decimals: Int = 0, suffix: String = "", status: String? = null) {
    GlassCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedNumber(value, decimals = decimals, suffix = suffix)
        if (status != null) Text(status, style = MaterialTheme.typography.labelMedium, color = ClgColors.Cyan)
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, width: Dp? = null, height: Dp = 16.dp) {
    Box(
        modifier = modifier.then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height).clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).shimmer()
    )
}

/** One subject with a smooth progress bar and a TEXT status chip. */
@Composable
fun SubjectRow(name: String, percent: Float, statusText: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val p by animateFloatAsState((percent / 100f).coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "bar")
    GlassCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("%.1f%%".format(percent), style = MaterialTheme.typography.titleMedium, color = ClgColors.Cyan)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))) {
            Box(Modifier.fillMaxWidth(p).height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(ClgColors.CyanDeep, ClgColors.Cyan))))
        }
        Text(statusText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class NavItem(val label: String, val icon: ImageVector)

/** Floating pill bottom bar: the selected item grows and shows its label. */
@Composable
fun ClgBottomBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .border(1.dp, ClgColors.Cyan.copy(alpha = 0.25f), CircleShape)
                .padding(horizontal = 6.dp, vertical = 5.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, item ->
                val isSel = i == selected
                val bg by animateColorAsState(if (isSel) ClgColors.Cyan.copy(alpha = 0.18f) else Color.Transparent, tween(250), label = "navBg")
                val tint by animateColorAsState(if (isSel) ClgColors.Cyan else MaterialTheme.colorScheme.onSurfaceVariant, tween(250), label = "navTint")
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .semantics { contentDescription = item.label + if (isSel) ", selected" else "" }
                        .bouncyClick { onSelect(i) }
                        .padding(horizontal = if (isSel) 12.dp else 9.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(21.dp))
                    AnimatedVisibility(isSel, enter = fadeIn(tween(200)) + expandHorizontally(), exit = fadeOut(tween(120)) + shrinkHorizontally()) {
                        Text(
                            text = item.label,
                            color = tint,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Smooth fade + small slide when switching tabs. */
@Composable
fun <T> ClgTabContent(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = target, modifier = modifier,
        transitionSpec = { (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 20 }) togetherWith fadeOut(tween(120)) },
        label = "tabs"
    ) { content(it) }
}

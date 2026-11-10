package com.example.ui.ux

/*
 * CLG 1% UX KIT  (single file, Material 3 + Compose, no extra libraries)
 *
 * The "feel" layer: what the app does while loading, when empty, when something fails, when you swipe,
 * when you finish something. Self-contained and uses MaterialTheme colours, so it matches every theme in ClgThemes.kt.
 *
 *   UxState / UxStateHost     loading skeleton -> content, or error with Retry, with a smooth cross-fade
 *   UxSkeletonList            shimmering placeholder rows
 *   UxEmptyState              friendly empty screen with an optional action
 *   UxErrorState              plain-language error with Retry
 *   UxSuccessCheck            animated tick for "done" moments
 *   UxCountUp                 number that counts to its new value
 *   UxScreenSwitcher          directional slide between tabs/steps (reduced-motion aware)
 *   UxSwipeToDelete           swipe row to delete (pair with showUndo)
 *   SnackbarHostState.showUndo   "Deleted. Undo" snackbar
 *   UxPullToRefresh           pull down to refresh
 *   UxBottomSheet             modal sheet with a drag handle
 *   UxLargeScaffold           collapsing large title bar
 *   UxField                   text field with inline error, counter, clear button, IME actions
 *   UxFirstRunTip             one-time tip card, remembered on the phone
 *   rememberUxHaptics         tick / confirm feedback
 *   rememberUxReduceMotion    true when the user turned system animations off
 *
 * Written without being compiled. If the compiler complains, fix imports first.
 */

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

// ------------------------------------------------------------------------------------------------ basics

/** True when the user switched system animations off. Decorative motion should then be skipped. */
@Composable
fun rememberUxReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

class UxHaptics(private val tickAction: () -> Unit, private val confirmAction: () -> Unit) {
    /** Light tick: selection changed, toggle flipped. */
    fun tick() = tickAction()
    /** Stronger tap: something was saved or completed. */
    fun confirm() = confirmAction()
}

@Composable
fun rememberUxHaptics(): UxHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        UxHaptics({ haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, { haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
    }
}

/** Moving highlight for placeholders. */
fun Modifier.uxShimmer(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "uxShimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "uxShimmerX")
    val base = MaterialTheme.colorScheme.onSurface
    drawBehind {
        val w = size.width
        drawRect(Brush.linearGradient(listOf(base.copy(alpha = 0.06f), base.copy(alpha = 0.16f), base.copy(alpha = 0.06f)), Offset(w * x, 0f), Offset(w * x + w * 0.6f, size.height)))
    }
}

// ------------------------------------------------------------------------------------------------ loading / empty / error

/** Loading / error / content for any screen. Put it in your ViewModel and show it with [UxStateHost]. */
sealed interface UxState<out T> {
    object Loading : UxState<Nothing>
    data class Error(val message: String) : UxState<Nothing>
    data class Content<T>(val data: T) : UxState<T>
}

@Composable
fun UxSkeletonRow(modifier: Modifier = Modifier, height: Dp = 72.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).uxShimmer())
}

@Composable
fun UxSkeletonList(count: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp).semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { UxSkeletonRow() }
    }
}

/** Cross-fades between a skeleton, an error with Retry, and your content. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> UxStateHost(
    state: UxState<T>, onRetry: () -> Unit, modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit = { UxSkeletonList() },
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = state, modifier = modifier, contentKey = { it::class },
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "uxState"
    ) { s ->
        when (s) {
            UxState.Loading -> skeleton()
            is UxState.Error -> UxErrorState(s.message, onRetry)
            is UxState.Content -> content(s.data)
        }
    }
}

@Composable
fun UxEmptyState(
    title: String, body: String, icon: ImageVector = Icons.Default.Info, modifier: Modifier = Modifier,
    actionLabel: String? = null, onAction: (() -> Unit)? = null
) {
    val reduce = rememberUxReduceMotion()
    val t = rememberInfiniteTransition(label = "uxEmptyFloat")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "uxEmptyPhase")
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primaryContainer)
                .graphicsLayer { translationY = if (reduce) 0f else sin(phase * 2f * Math.PI.toFloat()) * 6f * density },
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun UxErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}

// ------------------------------------------------------------------------------------------------ feedback

/** Animated tick. Set [play] = true when the thing succeeded. */
@Composable
fun UxSuccessCheck(play: Boolean, modifier: Modifier = Modifier, size: Dp = 72.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val reduce = rememberUxReduceMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(play) {
        if (!play) progress.snapTo(0f) else if (reduce) progress.snapTo(1f) else progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    Canvas(modifier.size(size).semantics { contentDescription = if (play) "Done" else "" }) {
        val stroke = Stroke(width = this.size.minDimension * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val inset = stroke.width / 2
        drawArc(color, -90f, 360f * progress.value.coerceAtMost(1f), false, Offset(inset, inset), Size(this.size.width - stroke.width, this.size.height - stroke.width), style = stroke)
        val w = this.size.width; val h = this.size.height
        val tick = Path().apply { moveTo(w * 0.28f, h * 0.52f); lineTo(w * 0.44f, h * 0.67f); lineTo(w * 0.74f, h * 0.36f) }
        val pm = PathMeasure(); pm.setPath(tick, false)
        val seg = Path(); pm.getSegment(0f, pm.length * ((progress.value - 0.35f) / 0.65f).coerceIn(0f, 1f), seg, true)
        drawPath(seg, color, style = stroke)
    }
}

/** Number that counts up or down to [value]. */
@Composable
fun UxCountUp(value: Float, modifier: Modifier = Modifier, decimals: Int = 0, suffix: String = "", style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium) {
    val reduce = rememberUxReduceMotion()
    val shown by animateFloatAsState(value, tween(if (reduce) 0 else 900, easing = FastOutSlowInEasing), label = "uxCount")
    Text("%.${decimals}f%s".format(shown, suffix), style = style, modifier = modifier)
}

/** "Deleted. Undo": call from a coroutine; [onUndo] runs only if the user taps Undo. */
suspend fun SnackbarHostState.showUndo(message: String, actionLabel: String = "Undo", onUndo: () -> Unit) {
    val result = showSnackbar(message = message, actionLabel = actionLabel, withDismissAction = true, duration = SnackbarDuration.Long)
    if (result == SnackbarResult.ActionPerformed) onUndo()
}

// ------------------------------------------------------------------------------------------------ navigation & gestures

/**
 * Slides between tabs/steps in the direction of travel (right-to-left when the index grows).
 * With reduced motion it only cross-fades.
 */
@Composable
fun UxScreenSwitcher(index: Int, modifier: Modifier = Modifier, content: @Composable (Int) -> Unit) {
    val reduce = rememberUxReduceMotion()
    AnimatedContent(
        targetState = index, modifier = modifier,
        transitionSpec = {
            if (reduce) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
            else {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(280)) { w -> dir * w / 6 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(200)) { w -> -dir * w / 6 } + fadeOut(tween(120)))
            }
        }, label = "uxSwitch"
    ) { content(it) }
}

/**
 * Swipe a row to the left to delete. Swiping has no screen-reader equivalent, so ALSO offer a visible
 * delete action (menu or button) for the same row. Pair with [showUndo] so a slip is never costly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UxSwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val haptics = rememberUxHaptics()
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.EndToStart) { haptics.confirm(); onDelete(); true } else false
    })
    SwipeToDismissBox(
        state = state, modifier = modifier, enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.errorContainer).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        content = { content() }
    )
}

@Composable
fun UxPullToRefresh(isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        content()
        if (isRefreshing) {
            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UxBottomSheet(visible: Boolean, onDismiss: () -> Unit, title: String? = null, content: @Composable () -> Unit) {
    if (!visible) return
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** Screen with a large title that collapses as you scroll. Put a LazyColumn (or scrollable Column) inside. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UxLargeScaffold(
    title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null,
    snackbarHost: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { if (snackbarHost != null) SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = { Text(title) }, scrollBehavior = scroll,
                navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        content = content
    )
}

// ------------------------------------------------------------------------------------------------ forms & tips

/**
 * Text field with: inline error text (not colour only), an optional character counter, a clear button,
 * and keyboard actions (Next / Done). [error] = null means valid.
 */
@Composable
fun UxField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    error: String? = null, maxLength: Int? = null, keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next, onImeAction: (() -> Unit)? = null, singleLine: Boolean = true
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value, onValueChange = { v -> if (maxLength == null || v.length <= maxLength) onChange(v) },
        label = { Text(label) }, singleLine = singleLine, isError = error != null, modifier = modifier,
        supportingText = {
            Row(Modifier.fillMaxWidth()) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                if (maxLength != null) Text("${value.length}/$maxLength")
            }
        },
        trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Clear, contentDescription = "Clear $label") } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onImeAction?.invoke() }, onNext = { onImeAction?.invoke() }, onGo = { onImeAction?.invoke() })
    )
}

private const val TIPS_PREFS = "clg1_ux_tips"

/** One-time tip. Shows until the student taps "Got it"; remembered per [tipKey]. */
@Composable
fun UxFirstRunTip(tipKey: String, text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(TIPS_PREFS, Context.MODE_PRIVATE) }
    var visible by remember { mutableStateOf(!prefs.getBoolean(tipKey, false)) }
    AnimatedVisibility(visible, modifier = modifier, enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                TextButton(onClick = { prefs.edit().putBoolean(tipKey, true).apply(); visible = false }) { Text("Got it") }
            }
        }
    }
}

/** Forget all dismissed tips (for a "Show tips again" button in Settings). */
fun resetUxTips(context: Context) { context.getSharedPreferences(TIPS_PREFS, Context.MODE_PRIVATE).edit().clear().apply() }

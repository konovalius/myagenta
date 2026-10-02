package com.example.myagent.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.theme.GoshaSans
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

private val PillHeight = 32.dp
private val PillShape = RoundedCornerShape(percent = 50)

private val PillSidePadding = 4.dp
private val ValueCircleSize = 24.dp
private val ValueChipPadding = 2.dp

private val RulerInset = 8.dp
private val TickWidth = 2.dp
private val TickHeight = 10.dp
private val BaseTickHeight = 14.dp
private val IndicatorWidth = 2.dp
private val IndicatorHeight = 16.dp

private val PillBackgroundIdle = Color.Black.copy(alpha = 0.55f)
private val PillBackgroundHeld = Color.Black.copy(alpha = 0.72f)
private val ActiveRed = Color(0xFFFF3B30)
private val ActiveChipBackground = Color.White.copy(alpha = 0.85f)
private val ActiveChipText = Color(0xFF0B0B0B)
private val IdleValueColor = Color.White.copy(alpha = 0.85f)
private val TickColor = Color.White.copy(alpha = 0.35f)
private val BaseTickColor = Color.White.copy(alpha = 0.6f)

private const val ValueFontSize = 12

/** Насколько деление должно совпасть с базой, чтобы считаться базовым. */
private const val TickEpsilon = 0.01f

/** Сколько надо удерживать капсулу, чтобы включился перебор. */
private const val ScrubHoldMillis = 220L

/** Начиная с какого расхождения пресет считается тем, на котором мы стоим. */
private const val PresetSnapEpsilon = 0.005f

private const val PillExpandMillis = 200
private const val PillFadeMillis = 120

/**
 * Капсула зума.
 *
 * В покое это два значения: слева «1x», справа текущее — а если стоим
 * на базе, там «2x», чтобы не получилось «1x 1x». Короткий тап переключает
 * 1x ↔ 2x, активное значение при этом стоит в светлом кружке.
 *
 * Удержание раскрывает капсулу вширь на всю ширину превью и показывает
 * линейку без подписей: деления с шагом 0.5x и одна линия-указатель.
 * Указатель стоит ровно под пальцем, а значение зума считается от его
 * позиции, поэтому линейка и протяжка не расходятся. Шкала линейная: пока
 * палец не отпущен, идёт под ним, а после отпускания зум защёлкивается на
 * ближайшее деление.
 */
@Composable
fun ZoomPill(
    presets: List<ZoomPreset>,
    magnification: Float,
    range: ClosedFloatingPointRange<Float>,
    onMagnificationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (presets.isEmpty()) return

    var isScrubbing by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val currentChange by rememberUpdatedState(onMagnificationChange)
    val currentPresets by rememberUpdatedState(presets)
    val currentRange by rememberUpdatedState(range)
    // Состояние, а не снимок значения: линейка читает его прямо в offset, иначе
    // каждый кадр протяжки пересобирал бы дерево целиком.
    val magnificationState = rememberUpdatedState(magnification)
    // Позиция пальца на линейке — по ней же считается зум, поэтому линейке она
    // нужна как состояние по той же причине, что и увеличение выше.
    var pointerX by remember { mutableFloatStateOf(0f) }
    // Ширина капсулы прямо сейчас: в покое она узкая, а в развёрнутом виде
    // занимает всё превью. Нужна, чтобы привести координату нажатия к линейке.
    var pillWidthPx by remember { mutableIntStateOf(0) }
    var pendingTap by remember { mutableStateOf(false) }
    var pendingTapPos by remember { mutableStateOf(0f) }

    BoxWithConstraints(modifier = modifier) {
        val insetPx = with(density) { RulerInset.toPx() }
        // Ширина превью — это ширина развёрнутой капсулы и длина линейки.
        val fullWidthPx = constraints.maxWidth
        // Ход протяжки — длина шкалы между полями. Заодно это и длина самой
        // линейки, поэтому указатель едет ровно под пальцем. Ширину самой капсулы
        // брать нельзя: она анимируется, и первые кадры протяжки летели бы вразы
        // быстрее.
        val travelPx = (fullWidthPx - 2f * insetPx).coerceAtLeast(1f)

        // Зум по позиции пальца на линейке. Шкала линейная, поэтому это просто
        // доля пройденного хода в диапазоне.
        fun valueAtRuler(x: Float): Float {
            val range = currentRange
            val start = minOf(range.start, range.endInclusive)
            val end = maxOf(range.start, range.endInclusive)
            return start + (x - insetPx) / travelPx * (end - start)
        }

        val base = baseZoomPreset(currentPresets)
        val two = secondZoomPreset(currentPresets)
        val baseMagnification = base?.magnification ?: 1f
        val atBase = abs(magnificationState.value - baseMagnification) < PresetSnapEpsilon
        val ticks = remember(currentRange) { zoomTickValues(currentRange) }

        val zoomVal = magnificationState.value
        fun fmt(v: Float): String {
            val rounded = (v * 10f).roundToInt() / 10f
            // убрать .0 если целое
            return if (rounded % 1f == 0f) "${rounded.toInt()}x" else "${rounded}x"
        }
        val at1 = abs(zoomVal - 1f) < 0.001f
        val at2 = abs(zoomVal - 2f) < 0.001f
        val collapsedLabels: List<String> = when {
            at1 || at2 -> listOf("1x", "2x")
            zoomVal > 1f && zoomVal < 2f -> listOf("1x", fmt(zoomVal), "2x")
            zoomVal > 2f -> listOf("1x", "2x", fmt(zoomVal))
            else -> listOf("1x", if (two != null) two.label else fmt(zoomVal))
        }

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (isScrubbing) PillBackgroundHeld else PillBackgroundIdle)
            .height(PillHeight)
            .onSizeChanged { pillWidthPx = it.width }
            .pointerInput(travelPx, ticks, currentPresets) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Проверим, куда попал тап (по индексу в collapsedLabels)
                    // Но в collapsed состоянии ширина не известна просто — обработаем тап по жесту: если короткий, определим позже по координате
                    if (!awaitHoldOrRelease(down.id, viewConfiguration.touchSlop)) {
                        // короткий тап — обработаем ниже через Row с pointerInput? или проще через индексы
                        // но здесь координата down.position в пределах Box
                        // запомним позицию
                        pendingTapPos = down.position.x
                        pendingTap = true
                        return@awaitEachGesture
                    }
                    // Позицию пальца надо привести к линейке. Капсула в покое
                    // узкая и стоит по центру превью, а линейка занимает всю
                    // ширину, поэтому при раскрытии левый край капсулы уезжает
                    // влево и координата внутри капсули завышается. Приводим её
                    // один раз, при нажатии, когда ширина ещё не меняется, а
                    // дальше копим приращения: они от края не зависят.
                    val shift = (fullWidthPx - pillWidthPx) / 2f
                    var fingerX = (down.position.x + shift).coerceIn(insetPx, insetPx + travelPx)
                    pointerX = fingerX
                    isScrubbing = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var value = valueAtRuler(fingerX)
                    currentChange(value)
                    drag(down.id) { change ->
                        fingerX = (fingerX + change.positionChange().x)
                            .coerceIn(insetPx, insetPx + travelPx)
                        change.consume()
                        pointerX = fingerX
                        value = valueAtRuler(fingerX)
                        currentChange(value)
                    }
                    currentChange(nearestTickValue(ticks, value))
                    isScrubbing = false
                }
            },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isScrubbing,
                transitionSpec = {
                    fadeIn(tween(PillFadeMillis)) togetherWith
                        fadeOut(tween(PillFadeMillis)) using
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ -> tween(PillExpandMillis) }
                        )
                },
                label = "zoomPillContent"
            ) { scrubbing ->
                if (scrubbing) {
                    ZoomRuler(
                        ticks = ticks,
                        baseMagnification = baseMagnification,
                        range = currentRange,
                        insetPx = insetPx,
                        travelPx = travelPx,
                        pointerX = pointerX
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = PillSidePadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val zoomValNow = magnificationState.value
                        val at1n = abs(zoomValNow - 1f) < 0.001f
                        val at2n = abs(zoomValNow - 2f) < 0.001f
                        val count = collapsedLabels.size
                        collapsedLabels.forEachIndexed { index, label ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .pointerInput(index, count, zoomValNow) {
                                        detectTapGestures(
                                            onLongPress = {
                                                isScrubbing = true
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onTap = {
                                                when (count) {
                                                    2 -> {
                                                        if (index == 0) currentChange(1f)
                                                        else currentChange(2f)
                                                    }
                                                    3 -> {
                                                        if (zoomValNow > 1f && zoomValNow < 2f) {
                                                            when (index) {
                                                                0 -> currentChange(1f)
                                                                1 -> {
                                                                    isScrubbing = true
                                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                }
                                                                2 -> currentChange(2f)
                                                            }
                                                        } else {
                                                            when (index) {
                                                                0 -> currentChange(1f)
                                                                1 -> currentChange(2f)
                                                                2 -> {
                                                                    isScrubbing = true
                                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    else -> {
                                                        if (index == 0) currentChange(1f)
                                                        else {
                                                            isScrubbing = true
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                val isActive = when (count) {
                                    2 -> {
                                        if (index == 0) at1n else at2n
                                    }
                                    3 -> {
                                        if (zoomValNow > 1f && zoomValNow < 2f) {
                                            index == 1 // текущее активное
                                        } else { // >2f
                                            index == 2
                                        }
                                    }
                                    else -> index == 0
                                }
                                ZoomValueChip(
                                    label = label,
                                    isActive = isActive
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Переключение 1x ↔ 2x: с базы — на 2x, с любой другой точки — на базу.
 */
private fun toggleBaseAndTwo(
    presets: List<ZoomPreset>,
    magnification: Float,
    onMagnificationChange: (Float) -> Unit
) {
    val base = baseZoomPreset(presets) ?: return
    val atBase = abs(magnification - base.magnification) < PresetSnapEpsilon
    val two = secondZoomPreset(presets)
    onMagnificationChange(
        if (atBase) two?.magnification ?: base.magnification else base.magnification
    )
}

/**
 * Ждём, пока палец либо простоит [ScrubHoldMillis] — тогда это удержание, либо
 * сорвётся/отпустит раньше — тогда это тап или отмена жеста.
 */
private suspend fun AwaitPointerEventScope.awaitHoldOrRelease(
    pointerId: PointerId,
    touchSlop: Float
): Boolean {
    var alive = true
    return withTimeoutOrNull(ScrubHoldMillis) {
        while (alive) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId }
            val delta = change?.positionChange()
            val moved = delta == null || abs(delta.x) > touchSlop || abs(delta.y) > touchSlop
            if (change == null || !change.pressed || moved) {
                alive = false
            }
        }
        alive
    } ?: true
}

/**
 * Линейка зума без подписей: деления и одна линия-указатель.
 *
 * Деления на месте, едет указатель, поэтому текущее увеличение читается по его
 * положению. Шкала линейная: деления с шагом 0.5x стоят на равных расстояниях,
 * а указатель приходит из той же координаты, из которой считается зум, — он
 * стоит ровно под пальцем, без догоняющей анимации.
 */
@Composable
private fun ZoomRuler(
    ticks: List<Float>,
    baseMagnification: Float,
    range: ClosedFloatingPointRange<Float>,
    insetPx: Float,
    travelPx: Float,
    pointerX: Float
) {
    val start = maxOf(minOf(range.start, range.endInclusive), 0.01f)
    val end = maxOf(range.start, range.endInclusive)
    // Пикселей на единицу зума: так деления раскладываются по шкале, а ход
    // пальца ровно в тех же единицах ведёт увеличение.
    val perUnit = if (end > start) travelPx / (end - start) else 0f

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        ticks.forEach { tick ->
            val base = abs(tick - baseMagnification) < TickEpsilon
            Box(
                modifier = Modifier
                    .offset { IntOffset((insetPx + (tick - start) * perUnit).roundToInt(), 0) }
                    .width(TickWidth)
                    .height(if (base) BaseTickHeight else TickHeight)
                    .background(if (base) BaseTickColor else TickColor)
            )
        }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(pointerX.coerceIn(insetPx, insetPx + travelPx).roundToInt(), 0)
                }
                .width(IndicatorWidth)
                .height(IndicatorHeight)
                .background(ActiveRed)
        )
    }
}

/**
 * Значение зума. Кружок рисуется только у активного: он светлый и полупрозрачный,
 * поэтому под ним читается чёрная капсула, а текст в нём тёмный. Второе значение
 * остаётся просто текстом — так пара читается как переключатель, а не как две
 * равноправные кнопки.
 */
@Composable
private fun ZoomValueChip(label: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isActive) ActiveChipBackground else Color.Transparent)
            .defaultMinSize(minWidth = ValueCircleSize, minHeight = ValueCircleSize),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = ValueFontSize.sp,
            fontFamily = GoshaSans,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) ActiveChipText else IdleValueColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = ValueChipPadding)
        )
    }
}
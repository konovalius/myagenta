package com.example.myagent.ui.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt

/**
 * Описание одного физического объектива, посчитанное из характеристик камеры.
 *
 * Все поля выводятся из Camera2-характеристик в рантайме, поэтому одна и та же
 * разметка пресетов честно работает и на телефоне с настоящим ультра-широким
 * объективом, и на устройстве, где широкоугольной камеры фактически нет.
 */
data class LensInfo(
    val cameraId: String,
    val lensFacing: Int,
    /** Эквивалентное фокусное в мм (по ширине кадра 36 мм). */
    val eqFocal35mm: Float,
    /** Горизонтальный угол обзора в градусах при zoomRatio = 1. */
    val hfovDeg: Float,
    val minZoomRatio: Float,
    val maxZoomRatio: Float
)

/**
 * Один пресет зума: конкретный объектив плюс цифровой множитель на нём.
 *
 * [label] — честная подпись вида «0.9x» / «1x» / «5x», а не стоковые
 * захардкоженные 0.5x, которые на части устройств соответствуют несуществующему
 * объективу.
 *
 * [magnification] — увеличение относительно базового объектива. Именно по нему
 * пресеты сравниваются между собой, а не по [zoomRatio]: у разных объективов
 * коэффициент отсчитывается от собственного широкого положения, поэтому «1x»
 * широкой камеры и «1x» основной — это разные кадры.
 */
data class ZoomPreset(
    val lens: LensInfo,
    val zoomRatio: Float,
    val label: String,
    /**
     * Объектив, который CameraX выбирает по умолчанию; на нём 1x.
     *
     * Признак объективный, а не выведенный из [zoomRatio]: у физически
     * отдельной широкой камеры [LensInfo.minZoomRatio] обычно равен единице,
     * и проверка `zoomRatio < 1` назвала бы её базовой. Всё, что не базовое,
     * — широкее и включается перебиндовкой камеры.
     */
    val isBaseLens: Boolean,
    val magnification: Float
)

/** Непрерывное увеличение, разрешённое в конкретный объектив и коэффициент на нём. */
data class ResolvedZoom(
    val lens: LensInfo,
    val zoomRatio: Float,
    val magnification: Float
)

/** Ширина кадра 35 мм, относительно которой считается эквивалентное фокусное. */
private const val FullFrameWidthMm = 36f

/** Шаг дедупликации объективов по эквивалентному фокусному, мм. */
private const val DedupeStepMm = 0.05f

/**
 * Цифровые шаги зума внутри одного объектива.
 *
 * Подобраны с почти постоянным отношением соседних шагов, чтобы каждый тап
 * менял кадрирование на глаз одинаково. Шаги за пределами реального диапазона
 * объектива отсекаются при сборке пресетов.
 */
private val DigitalSteps = floatArrayOf(1f, 1.5f, 2f, 3f, 4f, 5f, 7f, 10f)

/**
 * Читает характеристики объектива из CameraInfo.
 *
 * Берётся самое широкое фокусное из [CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]:
 * у логических камер с переменным фокусом это и есть широкое состояние.
 */
fun CameraInfo.toLensInfo(): LensInfo? {
    val camera2Info = runCatching { Camera2CameraInfo.from(this) }.getOrNull() ?: return null
    val lensFacing = runCatching { camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING) }
        .getOrNull()
        ?: return null
    if (lensFacing == CameraSelector.LENS_FACING_UNKNOWN) return null

    val focalLength = runCatching {
        camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
    }.getOrNull()
        ?.minOrNull()
        ?: return null
    if (focalLength <= 0f) return null

    val physicalSize = runCatching {
        camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
    }.getOrNull()
        ?: return null
    val sensorWidth = physicalSize.width
    val sensorHeight = physicalSize.height
    if (sensorWidth <= 0f || sensorHeight <= 0f) return null

    val zoomState = runCatching { zoomState.value }.getOrNull()
    val minZoomRatio = zoomState?.minZoomRatio?.takeIf { it > 0f } ?: 1f
    val maxZoomRatio = zoomState?.maxZoomRatio?.takeIf { it >= minZoomRatio } ?: minZoomRatio

    return LensInfo(
        cameraId = camera2Info.getCameraId(),
        lensFacing = lensFacing,
        eqFocal35mm = focalLength * FullFrameWidthMm / sensorWidth,
        hfovDeg = horizontalFovDeg(sensorWidth, focalLength),
        minZoomRatio = minZoomRatio,
        maxZoomRatio = maxZoomRatio
    )
}

/** Горизонтальный угол обзора: 2·arctan(ширина сенсора / 2 / фокусное). */
fun horizontalFovDeg(sensorWidthMm: Float, focalLengthMm: Float): Float =
    Math.toDegrees((2.0 * atan(sensorWidthMm / (2.0 * focalLengthMm))).toDouble()).toFloat()

/**
 * Все объективы заданного направления, отсортированные от самого широкого к самому узкому.
 *
 * Перечисление идёт напрямую через [android.hardware.camera2.CameraManager], а не через
 * `ProcessCameraProvider.availableCameraInfos`: последний на части устройств (в частности на
 * TECNO CK7n) отдаёт только по одной камере на направление и прячет дополнительные
 * физические объективы. Характеристики при этом читаются те же самые, поэтому значения
 * совпадают с `CameraInfo.toLensInfo()`.
 */
fun CameraManager.lensesFor(lensFacing: Int): List<LensInfo> =
    runCatching { cameraIdList }
        .getOrNull()
        .orEmpty()
        .mapNotNull { id -> cameraIdToLensInfo(id) }
        .filter { it.lensFacing == lensFacing }
        .sortedBy { it.eqFocal35mm }

/** Читает характеристики одного объектива по его camera id. */
fun CameraManager.cameraIdToLensInfo(cameraId: String): LensInfo? {
    val characteristics = runCatching { getCameraCharacteristics(cameraId) }.getOrNull() ?: return null

    val facing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: return null
    if (facing == CameraSelector.LENS_FACING_UNKNOWN) return null

    val focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        ?.minOrNull()
        ?: return null
    if (focalLength <= 0f) return null

    val physicalSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
    val sensorWidth = physicalSize?.width ?: return null
    val sensorHeight = physicalSize.height
    if (sensorWidth <= 0f || sensorHeight <= 0f) return null

    val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
    val minZoomRatio = zoomRange?.lower?.takeIf { it > 0f } ?: 1f
    val maxZoomRatio = zoomRange?.upper?.takeIf { it >= minZoomRatio }
        ?: characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            ?.takeIf { it >= minZoomRatio }
        ?: minZoomRatio

    return LensInfo(
        cameraId = cameraId,
        lensFacing = facing,
        eqFocal35mm = focalLength * FullFrameWidthMm / sensorWidth,
        hfovDeg = horizontalFovDeg(sensorWidth, focalLength),
        minZoomRatio = minZoomRatio,
        maxZoomRatio = maxZoomRatio
    )
}

/**
 * Селектор, жёстко указывающий на конкретную камеру по её id.
 *
 * [baseLensFacing] берётся у [base], чтобы фильтр не мог выбрать камеру
 * противоположного направления.
 */
fun lensCameraSelector(base: CameraSelector, lensCameraId: String): CameraSelector =
    CameraSelector.Builder()
        .requireLensFacing(base.lensFacing ?: CameraSelector.LENS_FACING_BACK)
        .addCameraFilter { infos ->
            infos.filter { Camera2CameraInfo.from(it).getCameraId() == lensCameraId }
        }
        .build()

/**
 * Какая именно камера будет выбрана селектором — чтобы пресет «1x» совпадал
 * с тем, что CameraX выбирает по умолчанию, и не перебиндовывался вхолостую.
 */
fun CameraSelector.resolveCameraId(available: List<CameraInfo>): String? =
    runCatching { filter(available).firstOrNull() }
        .getOrNull()
        ?.let { runCatching { Camera2CameraInfo.from(it).getCameraId() }.getOrNull() }

/**
 * Собирает ряд пресетов для выбранного направления.
 *
 * [baseLens] — объектив, который CameraX выбрал по умолчанию; его эквивалентное
 * фокусное эталонно, 1x. Объективы шире базового дают пресеты меньше 1x,
 * остальные переиспользуются как цифровые шаги.
 *
 * Дублирующиеся по углу обзора объективы (например, логическая камера,
 * повторяющая основную) отбрасываются: ширина кадра от них не меняется,
 * а лишний перебиндинг виден пользователю как чёрный кадр.
 */
fun buildZoomPresets(lenses: List<LensInfo>, baseLens: LensInfo): List<ZoomPreset> {
    val widerLenses = lenses
        .filter { it.eqFocal35mm < baseLens.eqFocal35mm - DedupeStepMm }
        .distinctBy { (it.eqFocal35mm / DedupeStepMm).roundToInt() }
        .sortedBy { it.eqFocal35mm }

    val widePresets = widerLenses.map { lens ->
        presetOf(lens, lens.minZoomRatio, baseLens, isBaseLens = false)
    }

    val digitalPresets = DigitalSteps
        .filter { it >= baseLens.minZoomRatio && it <= baseLens.maxZoomRatio }
        .map { ratio -> presetOf(baseLens, ratio, baseLens, isBaseLens = true) }

    return (widePresets + digitalPresets)
        .distinctBy { it.label }
        .sortedBy { it.magnification }
}

/**
 * Пресет из объектива и коэффициента зума на нём.
 *
 * Цифровой зум в z раз сужает угол обзора, то есть увеличивает эквивалентное
 * фокусное в z раз. Поэтому увеличение = eq35(объектив) · z / eq35(база);
 * у более широкого объектива при z = 1 оно меньше единицы.
 */
private fun presetOf(
    lens: LensInfo,
    zoomRatio: Float,
    baseLens: LensInfo,
    isBaseLens: Boolean
): ZoomPreset {
    val magnification = lens.eqFocal35mm * zoomRatio / baseLens.eqFocal35mm
    return ZoomPreset(
        lens = lens,
        zoomRatio = zoomRatio,
        label = zoomLabelFor(magnification),
        isBaseLens = isBaseLens,
        magnification = magnification
    )
}

/**
 * Подпись увеличения: «1x», «0.6x», «3.4x», «10x».
 *
 * Публичная, потому что капсула зума показывает текущее значение, а оно при
 * пинче промежуточное и ни с одним пресетом не совпадает.
 */
fun zoomLabelFor(magnification: Float): String {
    val text = if (magnification >= 10f) {
        ((magnification / 5f).roundToInt() * 5).toString()
    } else {
        val tenths = (magnification * 10f).roundToInt()
        val whole = tenths / 10
        val fraction = tenths % 10
        if (fraction == 0) "$whole" else "$whole.$fraction"
    }
    return "${text}x"
}

/**
 * Пресет, который виден в капсуле слева: базовый объектив, то есть «1x».
 */
fun baseZoomPreset(presets: List<ZoomPreset>): ZoomPreset? =
    presets.firstOrNull { it.isBaseLens }

/**
 * Пресет «2x» основного объектива — второе место капсулы.
 *
 * null, если камера до двухкратного зума не дотягивает: тогда второе место
 * в капсуле показывает текущее значение, а на «1x» просто отсутствует, чтобы
 * не вышло «1x | 1x».
 */
fun secondZoomPreset(presets: List<ZoomPreset>): ZoomPreset? =
    presets.firstOrNull {
        it.isBaseLens && kotlin.math.abs(it.magnification - 2f) < SecondMagnificationEpsilon
    }

/** Допуск, в котором увеличение пресета считается ровно двукратным. */
private const val SecondMagnificationEpsilon = 0.01f

/**
 * Сдвиг зума при протяжке пальцем по капсуле.
 *
 * Пиксели переводятся в множитель линейно: деления линейки идут с шагом 0.5x на
 * равных расстояниях, и ход пальца должен совпадать с их шагом, иначе значение
 * и указатель разъезжаются. Вправо — увеличение.
 *
 * [travelPx] — весь доступный ход, за который зум проходит диапазон целиком.
 * Считать его надо по ширине превью, а не по ширине разросшейся капсулы:
 * та анимируется, и первые кадры протяжки летели бы вразы быстрее.
 */
fun scrubMagnification(
    magnification: Float,
    deltaX: Float,
    travelPx: Float,
    range: ClosedFloatingPointRange<Float>
): Float {
    if (travelPx <= 0f) return magnification
    val start = minOf(range.start, range.endInclusive)
    val end = maxOf(range.start, range.endInclusive)
    if (end <= start) return magnification.coerceIn(start, end)
    val perPx = (end - start) / travelPx
    return (magnification + deltaX * perPx).coerceIn(start, end)
}

/** Системный [CameraManager] или null, если служба камеры недоступна. */
fun cameraManagerFrom(context: Context): CameraManager? =
    runCatching { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
        .getOrNull()

/**
 * Кламп цифрового зума в диапазон, который реально поддерживает объектив.
 *
 * `minZoomRatio` у камер с ультра-широким объективом бывает меньше единицы,
 * поэтому снизу ограничиваться единицей нельзя.
 */
fun LensInfo.clampZoomRatio(ratio: Float): Float =
    ratio.coerceIn(minZoomRatio, maxZoomRatio)

/**
 * Деления линейки зума.
 *
 * Шкала линейная, поэтому расстояние между делениями должно быть постоянным, а
 * шаг по значению — нет: на всём диапазоне ровно [ZoomTickGaps] промежутков, то
 * есть линейка не зависит от того, 1x…10x это или 0.5x…2x. Шаг получается сам,
 * а значения округляются до сотых, чтобы накопление шага не давало 2.6000001.
 *
 * Начало диапазона берётся как есть: если оно нецелое (например, 0.6x от
 * ультра-широкого объектива), то первое деление стоит ровно на левом краю шкалы,
 * а не уезжает за него.
 */
fun zoomTickValues(range: ClosedFloatingPointRange<Float>): List<Float> {
    val start = minOf(range.start, range.endInclusive)
    val end = maxOf(range.start, range.endInclusive)
    if (end <= start) return listOf(start)
    val step = (end - start) / ZoomTickGaps
    val ticks = mutableListOf<Float>()
    for (i in 0..ZoomTickGaps) {
        // Умножаем на индекс, а не копим шаг: накопление уводило последние
        // деления от расчётных мест.
        val tick = ((start + i * step) * 100f).roundToInt() / 100f
        // На узком диапазоне шаг меньше сотой, и соседние деления слились бы в
        // одно — такое деление убираем, чтобы линейка не выглядела слипшейся.
        if (ticks.isEmpty() || tick > ticks.last() + ZoomTickEpsilon) ticks += tick
    }
    return ticks
}

/** Промежутков между делениями линейки: делений получается на одно больше. */
private const val ZoomTickGaps = 56

/** Минимальный разрыв между делениями — меньший сливается в одно деление. */
private const val ZoomTickEpsilon = 0.001f

/**
 * Деление линейки, ближайшее к [magnification].
 *
 * Шкала линейная, поэтому ближайшим считается деление с наименьшим расстоянием
 * по самому значению, а не по его логарифму.
 */
fun nearestTickValue(ticks: List<Float>, magnification: Float): Float {
    if (ticks.isEmpty()) return magnification
    return ticks.minByOrNull { abs(it - magnification) } ?: magnification
}

/**
 * Диапазон увеличений, который способен дать каждый объектив.
 *
 * Увеличение считается относительно базового объектива, поэтому у разных
 * объективов диапазоны перекрываются, а не следуют друг за другом.
 */
private fun zoomIntervals(
    lenses: List<LensInfo>,
    baseLens: LensInfo
): List<Pair<LensInfo, ClosedFloatingPointRange<Float>>> {
    if (baseLens.eqFocal35mm <= 0f) return emptyList()
    return lenses
        .distinctBy { it.cameraId }
        .filter { it.eqFocal35mm > 0f }
        .map { lens ->
            val low = lens.eqFocal35mm * lens.minZoomRatio / baseLens.eqFocal35mm
            val high = lens.eqFocal35mm * lens.maxZoomRatio / baseLens.eqFocal35mm
            lens to (minOf(low, high)..maxOf(low, high))
        }
        .filter { (_, range) -> !range.isEmpty() }
}

/**
 * Общий диапазон увеличений всех объективов.
 *
 * Им клампится непрерывное значение при пинче: без клампа увеличение уходит за
 * пределы, подсветка чипа залипает на краю ряда, и обратный пинч приходится
 * делать долго.
 */
fun zoomMagnificationRange(
    lenses: List<LensInfo>,
    baseLens: LensInfo
): ClosedFloatingPointRange<Float> {
    val intervals = zoomIntervals(lenses, baseLens)
    if (intervals.isEmpty()) return 1f..1f
    return intervals.minOf { (_, range) -> range.start }..
        intervals.maxOf { (_, range) -> range.endInclusive }
}

/**
 * Переводит непрерывное увеличение в объектив и коэффициент зума на нём.
 *
 * Если увеличение попадает в диапазон нескольких объективов, выбирается самый
 * узкий из них: кадрирование телеобъектива не домплифицируется, картинка
 * остаётся резче, чем цифровой кроп широкого объектива. Так «1x» — это всегда
 * основная камера, а уход ниже единицы плавно уводит на широкий объектив и
 * плавно же возвращает обратно.
 *
 * Увеличение, недостижимое ни на одном объективе — бывает, когда диапазоны
 * не смыкаются, — прижимается к краю ближайшего.
 */
fun resolveZoom(
    lenses: List<LensInfo>,
    baseLens: LensInfo,
    magnification: Float
): ResolvedZoom? {
    val intervals = zoomIntervals(lenses, baseLens)
    if (intervals.isEmpty()) return null

    val covering = intervals.maxByOrNull { (lens, _) -> lens.eqFocal35mm }
        ?.takeIf { (_, range) -> magnification in range }
    if (covering != null) return zoomIn(covering.first, baseLens, magnification)

    val nearest = intervals.minByOrNull { (_, range) ->
        if (magnification < range.start) range.start - magnification
        else magnification - range.endInclusive
    } ?: return null

    return zoomIn(nearest.first, baseLens, magnification.coerceIn(nearest.second))
}

/** Конкретный коэффициент зума, дающий [magnification] на [lens]. */
private fun zoomIn(
    lens: LensInfo,
    baseLens: LensInfo,
    magnification: Float
): ResolvedZoom {
    val zoomRatio = lens.clampZoomRatio(
        magnification * baseLens.eqFocal35mm / lens.eqFocal35mm
    )
    return ResolvedZoom(
        lens = lens,
        zoomRatio = zoomRatio,
        magnification = lens.eqFocal35mm * zoomRatio / baseLens.eqFocal35mm
    )
}

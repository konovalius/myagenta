package com.example.myagent.ui.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myagent.ui.common.pressScale
import com.example.myagent.ui.theme.GoshaSans
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class CameraMode(val label: String) {
    PHOTO("Photo"),
    SLOW_MO("Slow-mo"),
    TIMELAPSE("Timelapse"),
    VIDEO("Video"),
    FIRST_TIME("First time")
}

private val CarouselItemWidth = 84.dp
private val CarouselItemSpacing = 0.dp
private val NavIconSize = 35.2.dp
private val NavIconGlyph = 28.6.dp
private val NavIconSpacing = 12.dp

@Composable
fun CameraToolbar(
    centeredModeIndex: Int,
    onCenteredModeChange: (Int) -> Unit,
    onModeTapped: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = CameraMode.entries
    val listState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                0
            } else {
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo.minByOrNull {
                    abs(it.offset + it.size / 2 - viewportCenter)
                }?.index ?: 0
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { onCenteredModeChange(it) }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val sidePadding = (maxWidth - CarouselItemWidth) / 2
        val sidePaddingPx = with(density) { sidePadding.toPx() }
        val itemWidthPx = with(density) { CarouselItemWidth.toPx() }
        val spacingPx = with(density) { CarouselItemSpacing.toPx() }
        val stepPx = itemWidthPx + spacingPx
        val centerX = with(density) { maxWidth.toPx() } / 2f
        val maxDistance = centerX

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(CarouselItemSpacing)
        ) {
            items(modes.size) { index ->
                val mode = modes[index]
                val isCentered = index == centeredIndex
                val interactionSource = remember { MutableInteractionSource() }
                Text(
                    text = mode.label,
                    fontSize = 18.sp,
                    fontFamily = GoshaSans,
                    fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCentered) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(CarouselItemWidth)
                        .graphicsLayer {
                            val visible = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == index }
                            if (visible != null) {
                                val scrollPx = visible.index * stepPx + visible.offset
                                val itemCenterX =
                                    sidePaddingPx + index * stepPx + itemWidthPx / 2f - scrollPx
                                val fraction =
                                    (abs(itemCenterX - centerX) / maxDistance)
                                        .coerceIn(0f, 1f)
                                val depthScale = 1f - fraction * 0.4f
                                scaleX = depthScale
                                scaleY = depthScale
                                alpha = 1f - fraction * 0.5f
                            }
                        }
                        .pressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                scope.launch { listState.animateScrollToItem(index) }
                                onModeTapped(mode)
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun CameraNavIcons(
    onNavigateToMap: () -> Unit,
    onNavigateToMasterFolders: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NavIconSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraNavButton(Icons.Filled.Map, "Карта", onNavigateToMap)
        CameraNavButton(Icons.Filled.Folder, "Мастер-папки", onNavigateToMasterFolders)
        CameraNavButton(Icons.Filled.PhotoLibrary, "Галерея", onOpenGallery)
    }
}

@Composable
private fun CameraNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(NavIconSize)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(NavIconGlyph)
        )
    }
}

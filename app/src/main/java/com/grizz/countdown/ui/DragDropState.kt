package com.grizz.countdown.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Long-press drag reordering for a [androidx.compose.foundation.lazy.LazyColumn].
 *
 * The dragged row is drawn at an offset from where the list actually placed it,
 * while [onMove] keeps swapping the underlying data as the row's midpoint passes
 * a neighbour — so the other rows animate into their new slots live.
 */
class DragDropState internal constructor(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
    private val onDrop: () -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    /** How far to visually displace the dragged row from its laid-out position. */
    val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
            }
    }

    internal fun onDragInterrupted() {
        if (draggingItemIndex != null) onDrop()
        draggingItemIndex = null
        draggingItemDraggedDelta = 0f
        draggingItemInitialOffset = 0
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..(item.offset + item.size) &&
                draggingItem.index != item.index
        }

        if (targetItem != null) {
            // Moving the item that anchors the viewport would make the list jump;
            // re-anchor scroll to the item taking its place.
            val scrollToIndex = when (state.firstVisibleItemIndex) {
                targetItem.index -> draggingItem.index
                draggingItem.index -> targetItem.index
                else -> null
            }
            if (scrollToIndex != null) {
                scope.launch {
                    state.scrollToItem(scrollToIndex, state.firstVisibleItemScrollOffset)
                    onMove(draggingItem.index, targetItem.index)
                }
            } else {
                onMove(draggingItem.index, targetItem.index)
            }
            draggingItemIndex = targetItem.index
        } else {
            // Dragging past an edge: nudge the list so the user can reach off-screen rows.
            val overscroll = when {
                draggingItemDraggedDelta > 0 ->
                    (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 ->
                    (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) scrollChannel.trySend(overscroll)
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
    onDrop: () -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(state = lazyListState, scope = scope, onMove = onMove, onDrop = onDrop)
    }
    androidx.compose.runtime.LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}

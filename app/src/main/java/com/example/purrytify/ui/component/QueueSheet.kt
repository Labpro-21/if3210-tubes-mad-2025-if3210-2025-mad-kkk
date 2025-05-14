package com.example.purrytify.ui.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.data.model.Song
import com.example.purrytify.ui.theme.Poppins
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueSheet(
    queueList: List<Song>,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    sheetState: SheetState,
    currIdx: Int,
    onMove: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val state = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to -> onMove(from.index, to.index) }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins
                )

                Text(
                    text = "${queueList.size} songs",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontFamily = Poppins
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            if (queueList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = Color.Gray,
                        fontFamily = Poppins
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .reorderable(state)
                ) {
                    itemsIndexed(queueList) { idx, song ->
                        ReorderableItem(state, key = idx) { isDragging ->
                            val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "")
                            Modifier
                                .fillMaxWidth()
                            Box(
                                modifier = Modifier.shadow(elevation.value)
                            ) {
                                QueueItem(
                                    song = song,
                                    onClick = { onSongClick(idx) },
                                    currSong = idx == currIdx,
                                    modifier = Modifier.detectReorderAfterLongPress(state)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


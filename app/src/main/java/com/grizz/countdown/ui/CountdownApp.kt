package com.grizz.countdown.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.grizz.countdown.data.CountdownEvent
import com.grizz.countdown.ui.theme.onColorFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

@Composable
fun CountdownApp(viewModel: EventsViewModel) {
    val events by viewModel.events.collectAsState()
    val message by viewModel.message.collectAsState()
    var editing by remember { mutableStateOf<CountdownEvent?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val target = editing
    if (target == null) {
        EventListScreen(
            events = events,
            snackbarHostState = snackbarHostState,
            onAdd = { editing = CountdownEvent(colorArgb = EventColors[events.size % EventColors.size]) },
            onOpen = { editing = it },
            onMove = viewModel::move,
            onDrop = { viewModel.commitOrder() },
            onExport = viewModel::exportTo,
            onImport = viewModel::importFrom
        )
    } else {
        val isExisting = events.any { it.id == target.id }
        BackHandler { editing = null }
        EventEditor(
            event = target,
            isExisting = isExisting,
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(target.id)
                editing = null
            },
            onCancel = { editing = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventListScreen(
    events: List<CountdownEvent>,
    snackbarHostState: SnackbarHostState,
    onAdd: () -> Unit,
    onOpen: (CountdownEvent) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDrop: () -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit
) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState, onMove = onMove, onDrop = onDrop)
    var menuOpen by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExport) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImport) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Grizz", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export backup") },
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch("grizz-events.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import backup") },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add event")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing to count yet.\nTap + to add a date.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dragDropState) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = dragDropState::onDragStart,
                            onDragEnd = dragDropState::onDragInterrupted,
                            onDragCancel = dragDropState::onDragInterrupted,
                            onDrag = { change, offset ->
                                change.consume()
                                dragDropState.onDrag(offset)
                            }
                        )
                    }
            ) {
                itemsIndexed(events, key = { _, item -> item.id }) { index, event ->
                    val dragging = index == dragDropState.draggingItemIndex
                    val itemModifier = if (dragging) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer { translationY = dragDropState.draggingItemOffset }
                    } else {
                        Modifier.animateItem()
                    }
                    EventCard(
                        event = event,
                        today = today,
                        lifted = dragging,
                        modifier = itemModifier,
                        onClick = { onOpen(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: CountdownEvent,
    today: LocalDate,
    lifted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = event.colorArgb.toComposeColor()
    val onBackground = onColorFor(background)
    val days = event.daysFrom(today)

    Surface(
        color = background,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = if (lifted) 12.dp else 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onBackground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = event.localDate.format(DateFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = onBackground.copy(alpha = 0.75f)
                )
                if (event.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = onBackground.copy(alpha = 0.6f),
                        maxLines = 2
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            CountNumber(days = days, color = onBackground)
        }
    }
}

/**
 * The number carries the meaning on its own; the only qualifier shown is "ago",
 * because 12 behind and 12 ahead are otherwise identical on screen.
 */
@Composable
private fun CountNumber(days: Int, color: Color, big: Boolean = true) {
    Column(horizontalAlignment = Alignment.End) {
        if (days == 0) {
            Text(
                text = "today",
                fontSize = if (big) 30.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        } else {
            Text(
                text = abs(days).toString(),
                fontSize = if (big) 46.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (days < 0) {
                Text(
                    text = "ago",
                    fontSize = 12.sp,
                    color = color.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditor(
    event: CountdownEvent,
    isExisting: Boolean,
    onSave: (CountdownEvent) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var title by rememberSaveable(event.id) { mutableStateOf(event.title) }
    var note by rememberSaveable(event.id) { mutableStateOf(event.note) }
    var date by rememberSaveable(event.id) { mutableStateOf(event.date) }
    var color by rememberSaveable(event.id) { mutableStateOf(event.colorArgb) }
    var pickingDate by remember { mutableStateOf(false) }

    val parsedDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isExisting) "Edit" else "New") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isExisting) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(
                        onClick = {
                            onSave(
                                event.copy(
                                    title = title.trim(),
                                    note = note.trim(),
                                    date = parsedDate.toString(),
                                    colorArgb = color
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Live preview of the card being built.
            EventCard(
                event = event.copy(title = title, note = note, date = parsedDate.toString(), colorArgb = color),
                today = LocalDate.now(),
                lifted = false,
                onClick = {}
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = { pickingDate = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = parsedDate.format(DateFormat),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Colour", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EventColors.forEach { swatch ->
                    val selected = swatch == color
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(swatch.toComposeColor())
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { color = swatch }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = parsedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

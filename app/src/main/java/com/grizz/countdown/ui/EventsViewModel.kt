package com.grizz.countdown.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grizz.countdown.data.CountdownEvent
import com.grizz.countdown.data.EventStore
import com.grizz.countdown.widget.refreshCountdownWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = EventStore.get(app)

    val events: StateFlow<List<CountdownEvent>> = store.events

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { store.load() }
    }

    fun save(event: CountdownEvent) = viewModelScope.launch {
        store.upsert(event)
        refreshWidgets()
    }

    fun delete(id: String) = viewModelScope.launch {
        store.delete(id)
        refreshWidgets()
    }

    /** Called for every item crossed during a drag; cheap and in-memory. */
    fun move(from: Int, to: Int) = store.move(from, to)

    /** Called once the drag settles. */
    fun commitOrder() = viewModelScope.launch { store.persist() }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        val payload = store.exportJson()
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(payload.toByteArray())
                } ?: error("no stream")
            }.isSuccess
        }
        _message.value = if (ok) "Exported ${events.value.size}" else "Export failed"
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        val text = withContext(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
        }
        val parsed = text?.let { EventStore.parseImport(it) }
        if (parsed == null) {
            _message.value = "Could not read that file"
            return@launch
        }
        store.replaceAll(parsed)
        refreshWidgets()
        _message.value = "Imported ${parsed.size}"
    }

    fun messageShown() {
        _message.value = null
    }

    private suspend fun refreshWidgets() = refreshCountdownWidgets(getApplication())
}

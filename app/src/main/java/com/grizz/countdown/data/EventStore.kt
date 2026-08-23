package com.grizz.countdown.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Every event lives in a single JSON file in the app's private storage. The list
 * order *is* the user's manual order, so it is preserved verbatim on write.
 *
 * Small data set (tens of rows), read by both the app and the widget process, and
 * exported as-is — a flat file beats a database here.
 */
class EventStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)
    private val writeLock = Mutex()

    private val _events = MutableStateFlow<List<CountdownEvent>>(emptyList())
    val events: StateFlow<List<CountdownEvent>> = _events.asStateFlow()

    suspend fun load() {
        _events.value = withContext(Dispatchers.IO) { readFile(file) }
    }

    suspend fun upsert(event: CountdownEvent) {
        val current = _events.value
        val index = current.indexOfFirst { it.id == event.id }
        _events.value = if (index >= 0) {
            current.toMutableList().also { it[index] = event }
        } else {
            current + event
        }
        persist()
    }

    suspend fun delete(id: String) {
        _events.value = _events.value.filterNot { it.id == id }
        persist()
    }

    suspend fun replaceAll(events: List<CountdownEvent>) {
        _events.value = events
        persist()
    }

    /**
     * In-memory only: a drag fires this on every crossed item, so writing here
     * would hammer the disk. [persist] is called once the drag settles.
     */
    fun move(from: Int, to: Int) {
        val current = _events.value
        if (from !in current.indices || to !in current.indices || from == to) return
        _events.value = current.toMutableList().also { it.add(to, it.removeAt(from)) }
    }

    suspend fun persist() {
        val snapshot = _events.value
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                // Write-then-rename so a kill mid-write cannot truncate the real file.
                val tmp = File(file.parentFile, "$FILE_NAME.tmp")
                tmp.writeText(json.encodeToString(snapshot))
                if (!tmp.renameTo(file)) {
                    file.writeText(json.encodeToString(snapshot))
                    tmp.delete()
                }
            }
        }
    }

    fun exportJson(): String = json.encodeToString(_events.value)

    companion object {
        private const val FILE_NAME = "events.json"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

        @Volatile
        private var instance: EventStore? = null

        fun get(context: Context): EventStore =
            instance ?: synchronized(this) {
                instance ?: EventStore(context).also { instance = it }
            }

        private fun readFile(file: File): List<CountdownEvent> {
            if (!file.exists()) return emptyList()
            return runCatching {
                json.decodeFromString<List<CountdownEvent>>(file.readText())
            }.getOrDefault(emptyList())
        }

        /** Direct read for the widget, which has no ViewModel to observe. */
        suspend fun readDirect(context: Context): List<CountdownEvent> =
            withContext(Dispatchers.IO) {
                readFile(File(context.applicationContext.filesDir, FILE_NAME))
            }

        fun parseImport(text: String): List<CountdownEvent>? =
            runCatching { json.decodeFromString<List<CountdownEvent>>(text) }.getOrNull()
    }
}

package com.grizz.countdown.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Repaint the widget whenever the app's data or the calendar day changes. */
suspend fun refreshCountdownWidgets(context: Context) {
    runCatching { CountdownWidget().updateAll(context) }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in DAY_ROLLOVER_ACTIONS) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    refreshCountdownWidgets(context.applicationContext)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private companion object {
        val DAY_ROLLOVER_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}

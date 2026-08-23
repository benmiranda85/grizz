package com.grizz.countdown.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.grizz.countdown.MainActivity
import com.grizz.countdown.data.EventStore
import com.grizz.countdown.data.nextUp
import com.grizz.countdown.ui.theme.onColorFor
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.min

/**
 * Shows whichever event is next: the soonest one still ahead, or — if everything
 * has passed — the most recent one, counting up.
 */
class CountdownWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val events = EventStore.readDirect(context)
        provideContent {
            GlanceTheme {
                WidgetBody(events = events)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetBody(events: List<com.grizz.countdown.data.CountdownEvent>) {
        val today = LocalDate.now()
        val event = events.nextUp(today)
        val size = LocalSize.current

        val background = event?.let { Color(it.colorArgb) } ?: Color(0xFF1B1B1F)
        val foreground = ColorProvider(onColorFor(background))

        // Scale the number to whatever cell the user dropped the widget into.
        val shortest = min(size.width.value, size.height.value)
        val numberSize = (shortest * 0.36f).coerceIn(28f, 72f).sp
        val titleSize = (shortest * 0.10f).coerceIn(11f, 16f).sp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(background)
                .cornerRadius(20.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            if (event == null) {
                Text(
                    text = "No dates yet",
                    style = TextStyle(color = foreground, fontSize = titleSize, textAlign = TextAlign.Center)
                )
                return@Column
            }

            val days = event.daysFrom(today)
            Text(
                text = if (days == 0) "today" else abs(days).toString(),
                style = TextStyle(
                    color = foreground,
                    fontSize = if (days == 0) titleSize else numberSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
            if (days < 0) {
                Text(
                    text = "ago",
                    style = TextStyle(color = foreground, fontSize = 11.sp, textAlign = TextAlign.Center),
                    maxLines = 1
                )
            }
            Text(
                text = event.title.ifBlank { "Untitled" },
                style = TextStyle(color = foreground, fontSize = titleSize, textAlign = TextAlign.Center),
                maxLines = 2
            )
        }
    }
}

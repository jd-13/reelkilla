package me.jdevlin.reelkilla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.jdevlin.reelkilla.ui.theme.ReelkillaTheme
import me.jdevlin.reelkilla.widget.ReelkillaWidget
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReelkillaTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current
    var nextOpenAt by remember { mutableLongStateOf(NextOpenTimeStore.getNextOpenAt(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val remainingMs = max(0L, nextOpenAt - now)
    val onDurationSelected: (Long) -> Unit = { durationMs ->
        nextOpenAt = NextOpenTimeStore.advanceNextOpenAt(context, durationMs)
        now = System.currentTimeMillis()
        ReelkillaWidget.refreshAll(context)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Instagram available in",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formatRemainingTime(remainingMs),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Block for",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DurationButton(label = "30 min", durationMs = 30L * 60L * 1000L, onClick = onDurationSelected)
                DurationButton(label = "1 hr", durationMs = 60L * 60L * 1000L, onClick = onDurationSelected)
                DurationButton(label = "2 hr", durationMs = 2L * 60L * 60L * 1000L, onClick = onDurationSelected)
                DurationButton(label = "4 hr", durationMs = 4L * 60L * 60L * 1000L, onClick = onDurationSelected)
            }
        }
    }
}

@Composable
private fun DurationButton(
    label: String,
    durationMs: Long,
    onClick: (Long) -> Unit
) {
    Button(
        onClick = { onClick(durationMs) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label)
    }
}

private fun formatRemainingTime(ms: Long): String {
    if (ms <= 0L) {
        return "Available now"
    }

    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
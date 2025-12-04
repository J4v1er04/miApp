package com.example.myapplication.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val duration by homeViewModel.sessionDuration.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RehabProgressCard( 
            isActive = uiState.isActive,
            angle = uiState.currentAngle,
            progress = uiState.currentProgress / 100f, // Convertir a float 0.0-1.0
            duration = duration
        )

        Spacer(modifier = Modifier.height(16.dp))
        LiveEventsLog(events = uiState.liveEvents)
        Spacer(modifier = Modifier.height(16.dp))

        ManualControlsCard(
            ledOn = uiState.ledOn,
            buzzerOn = uiState.buzzerOn,
            onLedToggle = { homeViewModel.setLedState(it) },
            onBuzzerToggle = { homeViewModel.setBuzzerState(it) }
        )

        Spacer(modifier = Modifier.weight(1f))

        SessionControlButtons(
            isActive = uiState.isActive,
            onStartClick = { homeViewModel.startSession("brazo") },
            onStopClick = { homeViewModel.stopSession() },
            onCalibInitClick = { homeViewModel.calibrateStart() },
            onCalibFinalClick = { homeViewModel.calibrateEnd() }
        )
    }
}

@Composable
fun RehabProgressCard(isActive: Boolean, angle: Float, progress: Float, duration: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sesión Actual", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(String.format(Locale.US, "%.1f°", angle), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressAnimation")
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
            if (isActive) {
                Text(duration, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun SessionControlButtons(
    isActive: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onCalibInitClick: () -> Unit,
    onCalibFinalClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = if (isActive) onStopClick else onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (isActive) "Detener Sesión" else "Iniciar Sesión", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCalibInitClick, modifier = Modifier.weight(1f), enabled = !isActive) {
                Text("Calibrar Inicio")
            }
            Button(onClick = onCalibFinalClick, modifier = Modifier.weight(1f), enabled = !isActive) {
                Text("Calibrar Final")
            }
        }
    }
}

@Composable
fun ManualControlsCard(
    ledOn: Boolean, 
    buzzerOn: Boolean, 
    onLedToggle: (Boolean) -> Unit, 
    onBuzzerToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Controles Manuales", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlashOn, contentDescription = "LED")
                Text("Activar LED", modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
                Switch(checked = ledOn, onCheckedChange = onLedToggle)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = "Buzzer")
                Text("Activar Buzzer", modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
                Switch(checked = buzzerOn, onCheckedChange = onBuzzerToggle)
            }
        }
    }
}


@Composable
fun LiveEventsLog(events: List<LiveEvent>) {
    Column(modifier = Modifier.heightIn(max = 100.dp)) {
        Text("Registro de Eventos", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Esperando eventos...", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn {
                items(events.reversed()) { event ->
                    EventRow(eventType = event.eventType, timestamp = event.timestamp)
                }
            }
        }
    }
}

// --- EventRow AHORA ES MÁS COMPLETO ---
@Composable
fun EventRow(eventType: String, timestamp: com.google.firebase.Timestamp?) {
    val eventInfo = remember(eventType) {
        when (eventType) {
            "IMU_ALERTA_BRUSCO" -> Pair(Icons.Default.Warning, "Alerta Mov. Brusco")
            "PIR_MOVIMIENTO" -> Pair(Icons.Default.DirectionsRun, "Movimiento PIR")
            "IMU_VERTICAL" -> Pair(Icons.Default.StayCurrentPortrait, "IMU Vertical")
            "IMU_HORIZONTAL" -> Pair(Icons.Default.StayCurrentLandscape, "IMU Horizontal")
            "LED Activado (Manual)" -> Pair(Icons.Default.FlashOn, "LED Activado")
            "LED Desactivado (Manual)" -> Pair(Icons.Default.FlashOff, "LED Desactivado")
            "Buzzer Activado (Manual)" -> Pair(Icons.Default.NotificationsActive, "Buzzer Activado")
            "Buzzer Desactivado (Manual)" -> Pair(Icons.Default.NotificationsOff, "Buzzer Desactivado")
            else -> Pair(Icons.Default.Info, eventType) // Fallback
        }
    }

    val color = when(eventType) {
        "IMU_ALERTA_BRUSCO" -> MaterialTheme.colorScheme.error
        "PIR_MOVIMIENTO" -> MaterialTheme.colorScheme.onSurface
        "IMU_VERTICAL" -> Color.Gray
        "IMU_HORIZONTAL" -> Color.Gray
        "LED Activado (Manual)" -> Color.Blue
        "Buzzer Activado (Manual)" -> Color.Blue
        else -> Color.Gray
    }

    val icon = eventInfo.first
    val text = eventInfo.second

    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(timestamp?.toDate()?.toFormattedTimeString() ?: "--:--:--", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(70.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (eventType == "IMU_ALERTA_BRUSCO") FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

fun Date.toFormattedTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(this)
}

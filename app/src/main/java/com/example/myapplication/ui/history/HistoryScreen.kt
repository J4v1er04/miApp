package com.example.myapplication.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class para una sesión de historial, ahora con el ID del documento
data class HistorySession(
    @DocumentId val id: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val events: List<Map<String, Any>> = emptyList()
)

data class SessionEvent(
    val eventType: String,
    val timestamp: Timestamp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val db = Firebase.firestore
    val (sessions, setSessions) = remember { mutableStateOf<List<HistorySession>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("history")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val sessionList = snapshots?.toObjects(HistorySession::class.java) ?: emptyList()
                setSessions(sessionList)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Sesiones", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(session)
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: HistorySession) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- Diálogo de Confirmación para Eliminar ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta sesión del historial? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        val db = Firebase.firestore
                        db.collection("history").document(session.id).delete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sesión del ${session.startTime?.toDate()?.toFormattedDateString()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "De ${session.startTime?.toDate()?.toFormattedTimeString()} a ${session.endTime?.toDate()?.toFormattedTimeString()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${session.events.size} eventos registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (session.events.any { (it["eventType"] as? String) == "IMU_ALERTA_BRUSCO" }) FontWeight.Bold else FontWeight.Normal,
                        color = if (session.events.any { (it["eventType"] as? String) == "IMU_ALERTA_BRUSCO" }) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // --- Botones de acción ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("Eliminar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Ocultar" else "Ver Detalles")
                }
            }

            // Contenido expandible
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val parsedEvents = session.events.mapNotNull {
                        val type = it["eventType"] as? String
                        val ts = it["timestamp"] as? Timestamp
                        if (type != null && ts != null) SessionEvent(type, ts) else null
                    }

                    if (parsedEvents.isEmpty()) {
                        Text("No se registraron eventos en esta sesión.")
                    } else {
                        parsedEvents.sortedBy { it.timestamp.seconds }.forEach { event ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = event.timestamp.toDate().toFormattedTimeString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(80.dp)
                                )
                                Text(text = event.eventType, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Date.toFormattedDateString(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(this)
}

fun Date.toFormattedTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(this)
}

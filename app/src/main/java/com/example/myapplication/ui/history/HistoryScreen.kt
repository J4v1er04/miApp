package com.example.myapplication.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

fun Date.toFormattedTimeString(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(this)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val groupedSessions by historyViewModel.groupedSessions.collectAsState()

    if (groupedSessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay sesiones en el historial.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            groupedSessions.forEach { group ->
                stickyHeader {
                    DateHeader(date = group.date)
                }
                items(group.sessions) { session ->
                    SessionCard(session = session, onDelete = { historyViewModel.deleteSession(session.id) })
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SessionCard(session: HistorySession, onDelete: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta sesión del historial?") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false; }) {
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "De ${session.startTime?.toDate()?.toFormattedTimeString() ?: "--:--"} a ${session.endTime?.toDate()?.toFormattedTimeString() ?: "--:--"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expandir/Colapsar"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    if (session.events.isEmpty()) {
                        Text("No se registraron eventos.")
                    } else {
                        session.events.forEach { event ->
                            val eventType = event["eventType"] as? String ?: ""
                            val timestamp = event["timestamp"] as? Timestamp
                            EventRow(eventType = eventType, timestamp = timestamp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar Sesión")
                    }
                }
            }
        }
    }
}

@Composable
fun EventRow(eventType: String, timestamp: Timestamp?) {
    val isAlert = eventType == "IMU_ALERTA_BRUSCO"
    val color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isAlert) {
            Icon(Icons.Default.Warning, contentDescription = "Alerta", tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = timestamp?.toDate()?.toFormattedTimeString() ?: "--:--:--",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = eventType,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isAlert) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

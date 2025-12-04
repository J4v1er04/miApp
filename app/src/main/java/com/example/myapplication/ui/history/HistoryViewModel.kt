package com.example.myapplication.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// --- DATA CLASSES (como las teníamos en HistoryScreen.kt) ---
data class HistorySession(
    @DocumentId val id: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val events: List<Map<String, Any>> = emptyList()
)

data class GroupedSession(val date: String, val sessions: List<HistorySession>)

class HistoryViewModel : ViewModel() {

    private val db = Firebase.firestore

    private val _groupedSessions = MutableStateFlow<List<GroupedSession>>(emptyList())
    val groupedSessions: StateFlow<List<GroupedSession>> = _groupedSessions

    init {
        listenToHistory()
    }

    private fun listenToHistory() {
        viewModelScope.launch {
            db.collection("history")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w("HistoryViewModel", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    val sessions = snapshots?.toObjects(HistorySession::class.java) ?: emptyList()
                    // Agrupa las sesiones por día
                    val grouped = sessions.groupBy { 
                        it.startTime?.toDate()?.toFormattedDateString() ?: "Fecha desconocida"
                    }.map { (date, sessionList) ->
                        GroupedSession(date, sessionList)
                    }
                    _groupedSessions.value = grouped
                }
        }
    }

    fun deleteSession(sessionId: String) {
        db.collection("history").document(sessionId).delete()
            .addOnSuccessListener { Log.d("HistoryViewModel", "Session $sessionId deleted") }
            .addOnFailureListener { e -> Log.w("HistoryViewModel", "Error deleting session", e) }
    }
}

// Función de extensión para formatear la fecha
fun java.util.Date.toFormattedDateString(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(this)
}

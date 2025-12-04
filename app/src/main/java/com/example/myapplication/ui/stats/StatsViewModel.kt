package com.example.myapplication.ui.stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale // <-- IMPORTACIÓN AÑADIDA

data class BarChartData(val label: String, val value: Float)

data class StatsState(
    val last7DaysEvents: List<BarChartData> = emptyList()
)

class StatsViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow(StatsState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                db.collection("history").get().addOnSuccessListener { snapshot ->
                    if (snapshot.isEmpty) return@addOnSuccessListener

                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }.time

                    // Contar eventos por día
                    val dailyCounts = mutableMapOf<String, Int>()
                    val dateFormatter = java.text.SimpleDateFormat("EEE", Locale.getDefault()) // Formato "Lun", "Mar", etc.

                    for (i in 0..6) {
                        val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }.time
                        dailyCounts[dateFormatter.format(day)] = 0
                    }

                    snapshot.documents.forEach { doc ->
                        val events = doc.get("events") as? List<Map<String, Any>> ?: emptyList()
                        events.forEach { event ->
                            val timestamp = event["timestamp"] as? Timestamp
                            if (timestamp != null && timestamp.toDate().after(sevenDaysAgo)) {
                                val dayLabel = dateFormatter.format(timestamp.toDate())
                                dailyCounts[dayLabel] = (dailyCounts[dayLabel] ?: 0) + 1
                            }
                        }
                    }
                    
                    val chartData = dailyCounts.entries.map { BarChartData(it.key, it.value.toFloat()) }.reversed()
                    _uiState.value = _uiState.value.copy(last7DaysEvents = chartData)
                }
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Error loading stats", e)
            }
        }
    }
}

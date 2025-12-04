package com.example.myapplication.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class LiveEvent(val eventType: String = "", val timestamp: Timestamp = Timestamp.now(), val angle: Float = 0f, val progress: Int = 0, val limb: String? = null)

data class HomeState(
    val isActive: Boolean = false,
    val ledOn: Boolean = false,
    val buzzerOn: Boolean = false,
    val isBridgeOnline: Boolean = false,
    val liveEvents: List<LiveEvent> = emptyList(),
    val currentAngle: Float = 0f,
    val currentProgress: Int = 0
)

class HomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val statusRef = db.collection("system_status").document("status")
    private val heartbeatRef = db.collection("system_status").document("bridge_heartbeat")
    private val liveEventRef = db.collection("system_status").document("live_event")
    private val commandRef = db.collection("system_commands").document("command")

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState

    private val _sessionDuration = MutableStateFlow("00:00")
    val sessionDuration: StateFlow<String> = _sessionDuration
    private var timerJob: Job? = null

    init {
        listenToSystemStatus()
        listenToBridgeHeartbeat()
        listenToLiveEvents()
    }

    private fun listenToSystemStatus() {
        statusRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("HomeViewModel", "Status listen failed.", e)
                stopTimer()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val isActive = snapshot.getBoolean("is_active") ?: false
                val startTime = snapshot.getTimestamp("session_start_time")

                if (isActive && startTime != null) {
                    startTimer(startTime)
                } else {
                    stopTimer()
                }

                _uiState.value = _uiState.value.copy(
                    isActive = isActive,
                    ledOn = snapshot.getBoolean("led_on") ?: false,
                    buzzerOn = snapshot.getBoolean("buzzer_on") ?: false
                )
            } else {
                stopTimer()
            }
        }
    }

    private fun listenToLiveEvents() {
        liveEventRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("HomeViewModel", "Live event listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val event = snapshot.toObject(LiveEvent::class.java)
                if (event != null) {
                    _uiState.value = _uiState.value.copy(
                        currentAngle = event.angle,
                        currentProgress = event.progress
                    )
                    // Solo añade eventos con un tipo definido (ej: IMU_ALERTA_BRUSCO)
                    if(event.eventType.isNotEmpty()) addEventToList(event)
                }
            }
        }
    }
    
    fun startSession(limb: String) {
        val sessionId = db.collection("sessions").document().id
        val data = mapOf(
           "is_active" to true,
           "current_limb" to limb,
           "session_id" to sessionId,
           "session_start_time" to Timestamp.now()
        )
        statusRef.set(data)
        db.collection("sessions").document(sessionId).set(mapOf("limb" to limb, "startTime" to Timestamp.now()))
    }

    fun stopSession() {
        statusRef.update("is_active", false)
    }
    
    fun calibrateStart() {
        sendCalibrationCommand("CALIB_INIT")
    }

    fun calibrateEnd() {
        sendCalibrationCommand("CALIB_FINAL")
    }

    private fun sendCalibrationCommand(command: String) {
        commandRef.set(mapOf(
            "command" to command,
            "timestamp" to Timestamp.now()
        ))
    }
    
    private fun listenToBridgeHeartbeat() {
        heartbeatRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                _uiState.value = _uiState.value.copy(isBridgeOnline = false)
                return@addSnapshotListener
            }
            val lastSeen = snapshot?.getTimestamp("last_seen")
            val isOnline = if (lastSeen != null) {
                (System.currentTimeMillis() - lastSeen.toDate().time) < 30_000
            } else { false }
            _uiState.value = _uiState.value.copy(isBridgeOnline = isOnline)
        }
    }
    
    private fun startTimer(startTime: Timestamp) {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (isActive) {
                val durationInSeconds = Timestamp.now().seconds - startTime.seconds
                val minutes = durationInSeconds / 60
                val seconds = durationInSeconds % 60
                _sessionDuration.value = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _sessionDuration.value = "00:00"
    }

    private fun addEventToList(event: LiveEvent) {
        val updatedEvents = (_uiState.value.liveEvents + event).takeLast(5)
        _uiState.value = _uiState.value.copy(liveEvents = updatedEvents)
    }

    fun setLedState(isOn: Boolean) {
        _uiState.value = _uiState.value.copy(ledOn = isOn)
        statusRef.update("led_on", isOn)
        // --- AÑADIDO: Registrar evento manual en la UI ---
        val eventText = if (isOn) "LED Activado (Manual)" else "LED Desactivado (Manual)"
        addEventToList(LiveEvent(eventType = eventText, timestamp = Timestamp.now()))
    }

    fun setBuzzerState(isOn: Boolean) {
        _uiState.value = _uiState.value.copy(buzzerOn = isOn)
        statusRef.update("buzzer_on", isOn)
        // --- AÑADIDO: Registrar evento manual en la UI ---
        val eventText = if (isOn) "Buzzer Activado (Manual)" else "Buzzer Desactivado (Manual)"
        addEventToList(LiveEvent(eventType = eventText, timestamp = Timestamp.now()))
    }
}
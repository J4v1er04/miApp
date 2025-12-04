package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.auth.RegisterScreen
import com.example.myapplication.ui.history.HistoryScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // Inform user that your app will not show notifications.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()
        logFCMToken()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun logFCMToken() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d(TAG, "FCM Registration Token: $token")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                onNavigateToLogin = { navController.navigate("login") { popUpTo("login") { inclusive = true } } }
            )
        }
        composable("home") {
            HomeScreen(
                onLogout = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(navController)
        }
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit, onNavigateToHistory: () -> Unit) {
    val user = Firebase.auth.currentUser
    val displayName = user?.displayName

    var isArmed by remember { mutableStateOf(false) }
    val db = Firebase.firestore
    val statusRef = db.collection("system_status").document("status")

    LaunchedEffect(Unit) {
        statusRef.addSnapshotListener { snapshot, e ->
            if (snapshot != null && snapshot.exists()) {
                isArmed = snapshot.getBoolean("is_armed") ?: false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (displayName != null) "¡Bienvenido, $displayName!" else "¡Bienvenido!")
            Spacer(modifier = Modifier.height(16.dp))

            val buttonColors = if (isArmed) {
                ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)) // Red
            } else {
                ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Green
            }

            Button(
                onClick = { 
                    val newArmedState = !isArmed
                    if (newArmedState) {
                        // Cuando se ARMA: guardar la hora de inicio de la sesión
                        val statusUpdate = mapOf(
                            "is_armed" to true,
                            "session_start_time" to Timestamp.now()
                        )
                        statusRef.set(statusUpdate)
                    } else {
                        // Cuando se DESARMA: solo apagar el sistema.
                        // El script de Python se encargará de guardar el historial.
                        statusRef.update("is_armed", false)
                    }
                },
                colors = buttonColors
            ) {
                Text(if (isArmed) "Desarmar Sistema" else "Armar Sistema")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNavigateToHistory) {
                Text("Ver Historial")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onLogout) {
                Text("Cerrar sesión")
            }
        }
    }
}
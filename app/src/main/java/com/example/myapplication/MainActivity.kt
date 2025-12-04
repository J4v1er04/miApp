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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.* 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.auth.AppBackground
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.auth.RegisterScreen
import com.example.myapplication.ui.history.HistoryScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.stats.StatsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
        } else {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()
        logFCMToken()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
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
            val token = task.result
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

    // --- LISTENER DE ESTADO DE AUTENTICACIÓN ---
    DisposableEffect(auth) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // Si en cualquier momento el usuario es nulo, vamos a login
                navController.navigate("login") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
        auth.addAuthStateListener(authStateListener)
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem("home", "Actividad", Icons.Filled.Bolt, Icons.Outlined.Bolt),
        BottomNavItem("history", "Historial", Icons.Filled.Article, Icons.Outlined.Article),
        BottomNavItem("stats", "Progreso", Icons.Filled.BarChart, Icons.Outlined.BarChart),
        BottomNavItem("profile", "Perfil", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
    )

    val startDestination = if (auth.currentUser != null) "home" else "login"
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = currentRoute !in listOf("login", "register")

    AppBackground {
        Scaffold(
            bottomBar = {
                if (shouldShowBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                label = { Text(item.title) },
                                icon = {
                                    Icon(
                                        if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                }
                            )
                        }
                    }
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(paddingValues)
            ) {
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
                composable("home") { HomeScreen(navController) }
                composable("history") { HistoryScreen(navController) }
                composable("stats") { StatsScreen(navController) }
                composable("profile") { ProfileScreen(navController) }
            }
        }
    }
}
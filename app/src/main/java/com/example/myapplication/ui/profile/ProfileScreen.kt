package com.example.myapplication.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val user = profileViewModel.currentUser
    val uiState by profileViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val auth = Firebase.auth

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            profileViewModel.onSnackbarShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Gestión de Perfil", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Información de la Cuenta", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Email: ", fontWeight = FontWeight.Bold)
                        Text(user?.email ?: "No disponible")
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { profileViewModel.showChangeEmailDialog(true) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Cambiar Email")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text("Miembro desde: ", fontWeight = FontWeight.Bold)
                        val creationDate = user?.metadata?.creationTimestamp?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "No disponible"
                        Text(creationDate)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Seguridad", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { profileViewModel.showChangePasswordDialog(true) }) {
                        Text("Cambiar Contraseña")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // --- NUEVO BOTÓN DE CERRAR SESIÓN ---
            Button(onClick = { 
                auth.signOut()
                navController.navigate("login") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }) {
                Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { profileViewModel.showDeleteAccountDialog(true) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("Eliminar Cuenta")
            }
        }
    }

    // --- DIÁLOGOS ---
    if (uiState.showChangeEmailDialog) {
        ChangeEmailDialog(
            onDismiss = { profileViewModel.showChangeEmailDialog(false) },
            onConfirm = { newEmail, password -> profileViewModel.changeEmail(newEmail, password) }
        )
    }
    if (uiState.showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { profileViewModel.showChangePasswordDialog(false) },
            onConfirm = { newPassword, currentPassword -> profileViewModel.changePassword(newPassword, currentPassword) }
        )
    }
    if (uiState.showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { profileViewModel.showDeleteAccountDialog(false) },
            onConfirm = { password -> profileViewModel.deleteAccount(password) }
        )
    }
}

// ... (El resto de los Composables de diálogo se mantienen igual)

@Composable
fun ChangeEmailDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Email") },
        text = {
            Column {
                OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Nuevo Email") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Contraseña Actual") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(newEmail, password) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("Contraseña Actual") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it }, label = { Text("Nueva Contraseña") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(newPassword, currentPassword) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Cuenta") },
        text = {
            Column {
                Text("Esta acción es irreversible. Para confirmar, por favor, introduce tu contraseña.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Contraseña Actual") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        },
        confirmButton = {
            Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), onClick = { onConfirm(password) }) {
                Text("Eliminar Definitivamente")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

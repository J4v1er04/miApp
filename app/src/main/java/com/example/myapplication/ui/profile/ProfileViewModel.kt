package com.example.myapplication.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileState(
    val showChangeEmailDialog: Boolean = false,
    val showChangePasswordDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val snackbarMessage: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState = _uiState.asStateFlow()

    val currentUser = Firebase.auth.currentUser

    fun onSnackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun showChangeEmailDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showChangeEmailDialog = show)
    }

    fun showChangePasswordDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showChangePasswordDialog = show)
    }

    fun showDeleteAccountDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteAccountDialog = show)
    }

    fun changeEmail(newEmail: String, currentPassword: String) {
        viewModelScope.launch {
            try {
                val user = currentUser ?: return@launch
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                
                user.reauthenticate(credential).await()
                user.updateEmail(newEmail).await()
                
                _uiState.value = _uiState.value.copy(showChangeEmailDialog = false, snackbarMessage = "Email actualizado con éxito")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error changing email", e)
                _uiState.value = _uiState.value.copy(snackbarMessage = "Error: ${e.message}")
            }
        }
    }

    fun changePassword(newPassword: String, currentPassword: String) {
        viewModelScope.launch {
            try {
                val user = currentUser ?: return@launch
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()

                _uiState.value = _uiState.value.copy(showChangePasswordDialog = false, snackbarMessage = "Contraseña actualizada con éxito")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error changing password", e)
                _uiState.value = _uiState.value.copy(snackbarMessage = "Error: ${e.message}")
            }
        }
    }

    fun deleteAccount(currentPassword: String) {
        viewModelScope.launch {
            try {
                val user = currentUser ?: return@launch
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential).await()
                user.delete().await()
                // No se actualiza el snackbar aquí, porque la navegación se encargará de llevarlo al login.
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error deleting account", e)
                _uiState.value = _uiState.value.copy(snackbarMessage = "Error: ${e.message}")
            }
        }
    }
}

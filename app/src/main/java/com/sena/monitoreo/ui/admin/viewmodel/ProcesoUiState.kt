package com.sena.monitoreo.ui.admin.viewmodel

/**
 * Define los posibles estados de la interfaz de usuario para la gestión del proceso.
 */
sealed interface ProcesoUiState {
    val message: String
    val isSuccess: Boolean

    data object Idle : ProcesoUiState {
        override val message: String = ""
        override val isSuccess: Boolean = true
    }

    data object Loading : ProcesoUiState {
        override val message: String = "Cargando estado..."
        override val isSuccess: Boolean = true
    }

    data class Success(override val message: String = "") : ProcesoUiState {
        override val isSuccess: Boolean = true
    }

    data class Error(override val message: String) : ProcesoUiState {
        override val isSuccess: Boolean = false
    }
}
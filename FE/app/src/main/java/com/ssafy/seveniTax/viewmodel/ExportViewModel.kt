package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val successFileName: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun exportBookEntries(year: Int) {
        export { exportRepository.exportBookEntries(year) }
    }

    fun exportVat(year: Int, half: Int) {
        export { exportRepository.exportVat(year, half) }
    }

    fun exportIncomeTax(year: Int) {
        export { exportRepository.exportIncomeTax(year) }
    }

    private fun export(action: suspend () -> Result<String>) {
        viewModelScope.launch {
            _uiState.value = ExportUiState(isExporting = true)
            val result = action()
            _uiState.value = result.fold(
                onSuccess = { ExportUiState(successFileName = it) },
                onFailure = { ExportUiState(errorMessage = it.message ?: "내보내기에 실패했습니다") }
            )
        }
    }

    fun clearState() {
        _uiState.value = ExportUiState()
    }
}

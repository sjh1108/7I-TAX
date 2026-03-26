package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.tax.TaxDeadline
import com.ssafy.seveniTax.data.repository.TaxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class TaxFilter(val label: String) {
    ALL("전체"),
    VAT("부가세"),
    INCOME("소득세"),
    LOCAL("지방세")
}

enum class TaxType {
    VAT, INCOME, LOCAL
}

@HiltViewModel
class TaxCalendarViewModel @Inject constructor(
    private val taxRepository: TaxRepository
) : ViewModel() {

    private val _deadlines = MutableStateFlow<List<TaxDeadline>>(emptyList())
    val deadlines: StateFlow<List<TaxDeadline>> = _deadlines.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaxFilter.ALL)
    val selectedFilter: StateFlow<TaxFilter> = _selectedFilter.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadDeadlines()
    }

    fun loadDeadlines() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = taxRepository.getDeadlines()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _deadlines.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = response.body()?.message ?: "데이터를 불러올 수 없습니다"
                }
            } catch (e: Exception) {
                _error.value = "네트워크 오류가 발생했습니다"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectFilter(filter: TaxFilter) {
        _selectedFilter.value = filter
    }

    fun goToPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun getFilteredDeadlines(): List<TaxDeadline> {
        val all = _deadlines.value
        return when (_selectedFilter.value) {
            TaxFilter.ALL -> all
            TaxFilter.VAT -> all.filter { classifyTax(it) == TaxType.VAT }
            TaxFilter.INCOME -> all.filter { classifyTax(it) == TaxType.INCOME }
            TaxFilter.LOCAL -> all.filter { classifyTax(it) == TaxType.LOCAL }
        }
    }

    fun getDeadlinesForMonth(yearMonth: YearMonth): List<TaxDeadline> {
        return getFilteredDeadlines().filter { deadline ->
            try {
                val date = LocalDate.parse(deadline.deadline)
                YearMonth.from(date) == yearMonth
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getDeadlinesForDate(date: LocalDate): List<TaxDeadline> {
        return getFilteredDeadlines().filter { deadline ->
            try {
                LocalDate.parse(deadline.deadline) == date
            } catch (e: Exception) {
                false
            }
        }
    }

    fun getMostUrgentDeadline(): TaxDeadline? {
        return _deadlines.value
            .filter { it.dDay >= 0 }
            .minByOrNull { it.dDay }
    }

    companion object {
        fun classifyTax(deadline: TaxDeadline): TaxType {
            val name = deadline.taxName
            return when {
                name.contains("지방") -> TaxType.LOCAL
                name.contains("부가") -> TaxType.VAT
                name.contains("소득") -> TaxType.INCOME
                else -> TaxType.INCOME
            }
        }
    }
}

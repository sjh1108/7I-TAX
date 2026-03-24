package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.book.BookEntryResponse
import com.ssafy.seveniTax.data.repository.BookEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EntryFilter(val label: String) {
    ALL("전체"), INCOME("수입"), EXPENSE("비용"), ASSET("자산")
}

enum class BookTab(val label: String) {
    LIST("장부 목록"), CATEGORY("세목별")
}

@HiltViewModel
class BookEntryViewModel @Inject constructor(
    private val bookEntryRepository: BookEntryRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<BookEntryResponse>>(emptyList())
    val entries: StateFlow<List<BookEntryResponse>> = _entries.asStateFlow()

    private val _unconfirmedCount = MutableStateFlow(0)
    val unconfirmedCount: StateFlow<Int> = _unconfirmedCount.asStateFlow()

    private val _selectedFilter = MutableStateFlow(EntryFilter.ALL)
    val selectedFilter: StateFlow<EntryFilter> = _selectedFilter.asStateFlow()

    private val _selectedTab = MutableStateFlow(BookTab.LIST)
    val selectedTab: StateFlow<BookTab> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadEntries()
        loadUnconfirmedCount()
    }

    fun loadEntries() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = bookEntryRepository.getEntries(confirmed = null, page = 0, size = 50)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _entries.value = response.body()?.data?.content ?: emptyList()
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

    fun loadUnconfirmedCount() {
        viewModelScope.launch {
            try {
                val response = bookEntryRepository.getUnconfirmedCount()
                if (response.isSuccessful && response.body()?.status == "success") {
                    _unconfirmedCount.value = response.body()?.data ?: 0
                }
            } catch (_: Exception) { }
        }
    }

    fun selectFilter(filter: EntryFilter) {
        _selectedFilter.value = filter
    }

    fun selectTab(tab: BookTab) {
        _selectedTab.value = tab
    }

    fun getFilteredEntries(): List<BookEntryResponse> {
        val all = _entries.value
        return when (_selectedFilter.value) {
            EntryFilter.ALL -> all
            EntryFilter.INCOME -> all.filter { it.entryType == "INCOME" }
            EntryFilter.EXPENSE -> all.filter { it.entryType == "EXPENSE" }
            EntryFilter.ASSET -> all.filter { it.entryType == "ASSET" }
        }
    }

    fun getTotalIncome(): Long = _entries.value.sumOf { it.incomeAmount }
    fun getTotalExpense(): Long = _entries.value.sumOf { it.expenseAmount }
    fun getTotalAsset(): Long = _entries.value.sumOf { it.fixedAssetAmount }
    fun getNetProfit(): Long = getTotalIncome() - getTotalExpense()

    // 세목별 비용 집계
    fun getExpenseByCategory(): List<Pair<String, Long>> {
        return _entries.value
            .filter { it.entryType == "EXPENSE" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.expenseAmount } }
            .sortedByDescending { it.second }
    }

    // 세목별 수입 집계
    fun getIncomeByCategory(): List<Pair<String, Long>> {
        return _entries.value
            .filter { it.entryType == "INCOME" && it.categoryName != null }
            .groupBy { it.categoryName!! }
            .map { (name, entries) -> name to entries.sumOf { it.incomeAmount } }
            .sortedByDescending { it.second }
    }
}
